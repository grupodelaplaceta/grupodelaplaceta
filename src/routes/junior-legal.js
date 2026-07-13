import { Router } from 'express';
import crypto from 'crypto';
import { supabase } from '../config/supabase.js';
import { sbFindSolicitanteByDip } from '../config/db-supabase.js';

const router = Router();

// ── Documentos legales requeridos para Placeta Junior ────────────────────
const DOCUMENTOS_REQUERIDOS = [
  { codigo: 'PJ-TYC-001', nombre: 'Términos y Condiciones', orden: 1 },
  { codigo: 'PJ-PRV-001', nombre: 'Política de Privacidad', orden: 2 },
  { codigo: 'PJ-CON-001', nombre: 'Consentimiento Tutor Legal', orden: 3 }
];

// ═══════════════════════════════════════════════════════════════════════════
//  LISTAR documentos pendientes de firma para un junior
// ═══════════════════════════════════════════════════════════════════════════

router.get('/documentos-pendientes/:juniorId', async (req, res) => {
  try {
    const { juniorId } = req.params;

    // Get junior info
    const { data: junior, error: jErr } = await supabase
      .from('junior_menores')
      .select('id, dip, nombre, apellidos, tutor_dip, tutor_nombre, estado')
      .eq('id', juniorId)
      .single();

    if (jErr || !junior) return res.status(404).json({ error: 'Junior no encontrado' });

    // Get already signed documents (look up by tutor's solicitante ID)
    const tutorSol = await sbFindSolicitanteByDip(junior.tutor_dip);
    const tutorId = tutorSol?.id || null;

    const { data: firmados, error: fErr } = await supabase
      .from('documentos_firmados')
      .select('codigo_modelo')
      .eq('firmado_por', tutorId)
      .eq('estado', 'firmado')
      .ilike('codigo_modelo', `%::junior::${juniorId}`);

    const firmadosSet = new Set((firmados || []).map(f => f.codigo_modelo.split('::')[0]));

    // Build pending list
    const pendientes = DOCUMENTOS_REQUERIDOS
      .filter(d => !firmadosSet.has(d.codigo))
      .map(d => ({
        codigo: d.codigo,
        nombre: d.nombre,
        orden: d.orden,
        firmado: false
      }));

    const todosFirmados = pendientes.length === 0;

    res.json({
      success: true,
      junior: {
        id: junior.id,
        dip: junior.dip,
        nombre: junior.nombre,
        apellidos: junior.apellidos,
        tutor_dip: junior.tutor_dip,
        tutor_nombre: junior.tutor_nombre,
        estado: junior.estado
      },
      documentos: DOCUMENTOS_REQUERIDOS.map(d => ({
        codigo: d.codigo,
        nombre: d.nombre,
        orden: d.orden,
        firmado: firmadosSet.has(d.codigo)
      })),
      pendientes,
      todos_firmados: todosFirmados
    });
  } catch (err) {
    console.error('[Legal] Error listando documentos:', err);
    res.status(500).json({ error: 'Error interno' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  FIRMAR documento con firma manuscrita (base64 PNG)
// ═══════════════════════════════════════════════════════════════════════════

router.post('/firmar-documento', async (req, res) => {
  try {
    const { junior_id, codigo_documento, firma_base64, firmante_dip, firmante_nombre, ip } = req.body;

    if (!junior_id || !codigo_documento || !firma_base64 || !firmante_dip) {
      return res.status(400).json({ error: 'Faltan datos obligatorios: junior_id, codigo_documento, firma_base64, firmante_dip' });
    }

    // Validate document code
    const docDef = DOCUMENTOS_REQUERIDOS.find(d => d.codigo === codigo_documento);
    if (!docDef) return res.status(400).json({ error: 'Código de documento no válido' });

    // Get junior
    const { data: junior, error: jErr } = await supabase
      .from('junior_menores')
      .select('id, dip, nombre, apellidos, tutor_dip, estado')
      .eq('id', junior_id)
      .single();

    if (jErr || !junior) return res.status(404).json({ error: 'Junior no encontrado' });

    // Verify the signer is the tutor
    if (junior.tutor_dip !== firmante_dip) {
      return res.status(403).json({ error: 'Solo el tutor legal puede firmar estos documentos' });
    }

    // Find the tutor's solicitante record for FK
    const tutorSol = await sbFindSolicitanteByDip(firmante_dip);
    const tutorId = tutorSol?.id || null;
    const clientIp = ip || req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const ahora = new Date().toISOString();
    const firmaHash = crypto.createHash('sha256').update(firma_base64 + ahora).digest('hex');

    // Insert signed document using existing Supabase table columns
    const { data: doc, error: insErr } = await supabase
      .from('documentos_firmados')
      .insert({
        usuario_id: tutorId,
        codigo_modelo: `${codigo_documento}::junior::${junior.id}`,
        titulo_documento: docDef.nombre,
        url_firma: firma_base64,
        hash_documento: firmaHash,
        firmado_por: tutorId,
        estado: 'firmado'
      })
      .select()
      .single();

    if (insErr) {
      // If duplicate, it's already signed
      if (insErr.code === '23505') {
        return res.json({ success: true, ya_firmado: true, message: 'Documento ya firmado anteriormente' });
      }
      console.error('[Legal] Error insertando firma:', JSON.stringify(insErr));
      return res.status(500).json({ error: `Error al guardar la firma: ${insErr.message}` });
    }

    // Log
    await supabase.from('junior_logs').insert({
      junior_id: junior.id,
      accion: 'firma_documento',
      detalle: `Firma manuscrita: ${docDef.nombre} (${codigo_documento}) por ${firmante_dip}`,
      ip: clientIp
    });

    // Check if all documents are now signed (by codigo_modelo pattern)
    const { data: firmados } = await supabase
      .from('documentos_firmados')
      .select('codigo_modelo')
      .eq('firmado_por', tutorId)
      .eq('estado', 'firmado')
      .ilike('codigo_modelo', `%::junior::${junior.id}`);

    const firmadosSet = new Set((firmados || []).map(f => {
      // Extract just the doc code from "PJ-TYC-001::junior::7"
      return f.codigo_modelo.split('::')[0];
    }));
    const todosFirmados = DOCUMENTOS_REQUERIDOS.every(d => firmadosSet.has(d.codigo));

    // If all signed, activate the junior account
    if (todosFirmados && junior.estado === 'pendiente_firma_tutor') {
      const { error: updErr } = await supabase.from('junior_menores')
        .update({ estado: 'activo' })
        .eq('id', junior.id);

      if (!updErr) {
        // Activate solicitante too
        await supabase.from('solicitantes')
          .update({ estado: 'activo' })
          .eq('dip', junior.dip);

        await supabase.from('junior_logs').insert({
          junior_id: junior.id,
          accion: 'cuenta_activada',
          detalle: 'Todos los documentos firmados. Cuenta junior activada.',
          ip: clientIp
        });

        console.log(`[Legal] ✅ Cuenta junior ${junior.dip} ACTIVADA - todos los documentos firmados`);
      } else {
        console.error('[Legal] Error activando cuenta:', updErr);
      }
    }

    res.json({
      success: true,
      documento: docDef.nombre,
      todos_firmados: todosFirmados,
      cuenta_activada: todosFirmados && junior.estado === 'pendiente_firma_tutor',
      message: todosFirmados
        ? 'Documento firmado. ¡Todos los documentos completados! Cuenta activada.'
        : `"${docDef.nombre}" firmado correctamente. Quedan ${DOCUMENTOS_REQUERIDOS.length - firmadosSet.size} documento(s) pendiente(s).`
    });
  } catch (err) {
    console.error('[Legal] Error firmando documento:', err);
    res.status(500).json({ error: 'Error interno al procesar la firma' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  CONTENIDO de un documento legal
// ═══════════════════════════════════════════════════════════════════════════

router.get('/documento-contenido/:codigo', async (req, res) => {
  const contenidos = {
    'PJ-TYC-001': {
      titulo: 'Términos y Condiciones — Placeta Junior',
      contenido: `TÉRMINOS Y CONDICIONES DE PLACETA JUNIOR\n\n
1. PARTES\n
1.1. De una parte, el Grupo de La Placeta (en adelante, "GDLP"), con sede en La Placeta.\n
1.2. De otra parte, el tutor legal (en adelante, "el Tutor"), actuando en representación del menor (en adelante, "el Menor").\n\n
2. OBJETO\n
2.1. Placeta Junior es un servicio financiero-educativo para menores de 16 años.\n
2.2. Incluye monedero digital en Placeta (Pz), academia de educación financiera, y herramientas de ahorro.\n\n
3. RESPONSABILIDAD DEL TUTOR\n
3.1. El Tutor es el único responsable de las acciones del Menor en la plataforma.\n
3.2. El Tutor puede establecer límites de gasto y supervisar la actividad del Menor.\n\n
4. PROTECCIÓN DE DATOS\n
4.1. Los datos del Menor se tratan conforme al RGPD y la LOPDGDD.\n
4.2. El Tutor puede ejercer los derechos ARCO en cualquier momento.\n\n
5. VIGENCIA\n
5.1. Este acuerdo entra en vigor en la fecha de firma digital manuscrita.\n
5.2. Al cumplir 16 años, el Menor debe migrar a PlacetaID estándar.`,
      version: '1.0',
      fecha: '2026-07-13'
    },
    'PJ-PRV-001': {
      titulo: 'Política de Privacidad — Placeta Junior',
      contenido: `POLÍTICA DE PRIVACIDAD — PLACETA JUNIOR\n\n
1. RESPONSABLE\n
Grupo de La Placeta (GDLP)\n\n
2. DATOS RECOGIDOS\n
• Datos identificativos del Menor: nombre, apellidos, fecha de nacimiento.\n
• Datos del Tutor: DNI, nombre, email, firma manuscrita digitalizada.\n
• Datos de uso: transacciones, progreso en academia, conexiones.\n\n
3. FINALIDAD\n
• Gestión del monedero digital y cuenta bancaria infantil.\n
• Formación financiera en la Academia GDLP.\n
• Cumplimiento legal y prevención de fraude.\n\n
4. BASE LEGAL\n
• Consentimiento explícito del tutor legal (Art. 6.1.a y Art. 8 RGPD).\n
• Interés legítimo en la protección del menor.\n\n
5. CONSERVACIÓN\n
Los datos se conservan mientras la cuenta esté activa y hasta 5 años después de la baja.\n\n
6. DERECHOS\n
El Tutor puede ejercer los derechos de acceso, rectificación, supresión, oposición, limitación y portabilidad.\n\n
7. MEDIDAS DE SEGURIDAD\n
• Firma manuscrita digitalizada con hash SHA-256.\n
• Datos bancarios aislados en backend-banco.\n
• Comunicaciones cifradas TLS 1.3.`,
      version: '1.0',
      fecha: '2026-07-13'
    },
    'PJ-CON-001': {
      titulo: 'Consentimiento del Tutor Legal',
      contenido: `CONSENTIMIENTO DEL TUTOR LEGAL — PLACETA JUNIOR\n\n
Yo, el abajo firmante, en calidad de tutor legal del menor,\n\n
DECLARO Y CONSENTO:\n\n
1. Que autorizo al menor a utilizar Placeta Junior, incluyendo:\n
   a) Monedero digital en Placeta (Pz).\n
   b) Acceso a la Academia GDLP de educación financiera.\n
   c) Recepción de bonos y recompensas educativas.\n\n
2. Que he leído y acepto los Términos y Condiciones.\n
3. Que he leído y acepto la Política de Privacidad.\n
4. Que soy el tutor legal y tengo capacidad para otorgar este consentimiento.\n
5. Que entiendo que puedo revocar este consentimiento en cualquier momento.\n
6. Que me hago responsable de supervisar la actividad del menor.\n\n
La firma manuscrita digitalizada tiene la misma validez legal que la firma en papel,\n
conforme al Reglamento eIDAS (UE 910/2014) y la Ley 6/2020 de servicios electrónicos de confianza.`,
      version: '1.0',
      fecha: '2026-07-13'
    }
  };

  const doc = contenidos[req.params.codigo];
  if (!doc) return res.status(404).json({ error: 'Documento no encontrado' });

  res.json(doc);
});

export default router;
