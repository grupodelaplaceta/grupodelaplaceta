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

// ═══════════════════════════════════════════════════════════════════════════
//  PDF VERIFICABLE — Documento legal con logo, firma y hash
// ═══════════════════════════════════════════════════════════════════════════

router.get('/documento-verificable/:docId', async (req, res) => {
  try {
    const { data: doc, error } = await supabase
      .from('documentos_firmados')
      .select('*')
      .eq('id', req.params.docId)
      .single();

    if (error || !doc) return res.status(404).send('<h1>Documento no encontrado</h1>');

    // Get junior info from codigo_modelo (format: "PJ-TYC-001::junior::8")
    const parts = (doc.codigo_modelo || '').split('::');
    const juniorId = parts[2] || null;
    let junior = null;
    if (juniorId) {
      const { data: j } = await supabase.from('junior_menores')
        .select('dip,nombre,apellidos,tutor_dip,tutor_nombre')
        .eq('id', juniorId).single();
      junior = j;
    }

    // Get tutor info
    let tutorNombre = '—';
    if (doc.firmado_por) {
      const { data: sol } = await supabase.from('solicitantes')
        .select('nombre_real,dip')
        .eq('id', doc.firmado_por).single();
      if (sol) tutorNombre = sol.nombre_real || sol.dip || '—';
    }

    const docCode = parts[0] || doc.codigo_modelo;
    const fecha = doc.creado_en ? new Date(doc.creado_en).toLocaleDateString('es-ES', { day:'numeric', month:'long', year:'numeric', hour:'2-digit', minute:'2-digit' }) : '—';
    const firmaImg = doc.url_firma 
      ? (doc.url_firma.startsWith('data:') ? doc.url_firma : `data:image/png;base64,${doc.url_firma}`)
      : null;

    // Get full document content
    const contenidos = {
      'PJ-TYC-001': {
        titulo: 'Términos y Condiciones — Placeta Junior',
        contenido: '1. PARTES\n1.1. Grupo de La Placeta (GDLP).\n1.2. El tutor legal, en representación del menor.\n\n2. OBJETO\n2.1. Placeta Junior es un servicio financiero-educativo para menores de 16 años.\n2.2. Incluye monedero digital, academia, y herramientas de ahorro.\n\n3. RESPONSABILIDAD DEL TUTOR\n3.1. El Tutor es responsable de las acciones del Menor.\n3.2. Puede establecer límites y supervisar la actividad.\n\n4. PROTECCIÓN DE DATOS\n4.1. Conforme al RGPD y LOPDGDD.\n4.2. El Tutor puede ejercer derechos ARCO.\n\n5. VIGENCIA\n5.1. Desde la firma digital manuscrita.\n5.2. Al cumplir 16 años debe migrar a PlacetaID estándar.'
      },
      'PJ-PRV-001': {
        titulo: 'Política de Privacidad — Placeta Junior',
        contenido: '1. RESPONSABLE: Grupo de La Placeta (GDLP).\n\n2. DATOS RECOGIDOS:\n• Identificativos del Menor (nombre, fecha nacimiento).\n• Datos del Tutor (DNI, nombre, email, firma).\n• Datos de uso (transacciones, progreso academia).\n\n3. FINALIDAD:\n• Gestión del monedero digital y cuenta bancaria infantil.\n• Formación financiera en Academia GDLP.\n• Cumplimiento legal y prevención de fraude.\n\n4. BASE LEGAL:\n• Consentimiento explícito del tutor (Art. 6.1.a y Art. 8 RGPD).\n\n5. CONSERVACIÓN: Mientras la cuenta esté activa + 5 años.\n\n6. DERECHOS: Acceso, rectificación, supresión, oposición, limitación, portabilidad.\n\n7. SEGURIDAD: Firma SHA-256, datos bancarios aislados, TLS 1.3.'
      },
      'PJ-CON-001': {
        titulo: 'Consentimiento del Tutor Legal',
        contenido: 'El abajo firmante, como tutor legal del menor, DECLARA Y CONSENTE:\n\n1. Autoriza al menor a usar Placeta Junior:\n   a) Monedero digital en Placeta (Pz).\n   b) Academia GDLP de educación financiera.\n   c) Bonos y recompensas educativas.\n\n2. Ha leído y acepta los Términos y Condiciones.\n3. Ha leído y acepta la Política de Privacidad.\n4. Es el tutor legal con capacidad para consentir.\n5. Puede revocar este consentimiento en cualquier momento.\n6. Se responsabiliza de supervisar al menor.\n\nLa firma manuscrita digitalizada tiene validez legal conforme al Reglamento eIDAS (UE 910/2014) y Ley 6/2020.'
      }
    };
    const docInfo = contenidos[docCode] || { titulo: doc.titulo_documento, contenido: doc.titulo_documento };

    const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>${doc.titulo_documento || 'Documento Legal'} — Placeta Junior</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap');
    * { box-sizing:border-box; margin:0; padding:0; }
    body { font-family:'Outfit',system-ui,sans-serif; background:#f0edf7; color:#1a1a2e; line-height:1.6; }
    .page { max-width:800px; margin:40px auto; background:#fff; border-radius:16px; box-shadow:0 10px 40px rgba(0,0,0,.08); overflow:hidden; }
    .header { background:linear-gradient(135deg,#3f00d8,#1c005f); color:#fff; padding:40px 48px; text-align:center; position:relative; }
    .header .logo { font-size:48px; font-weight:900; letter-spacing:-1px; }
    .header .logo span { color:#FF6600; }
    .header .sub { font-size:14px; opacity:.8; margin-top:6px; }
    .header::after { content:''; position:absolute; bottom:-20px; left:50%; transform:translateX(-50%); width:60px; height:60px; background:#FF6600; border-radius:50%; border:4px solid #fff; display:flex; align-items:center; justify-content:center; font-size:24px; }
    .body { padding:56px 48px 40px; }
    .meta { background:#f8f7fc; border-radius:12px; padding:16px 20px; margin-bottom:24px; font-size:13px; display:grid; grid-template-columns:1fr 1fr; gap:8px; border-left:4px solid #3f00d8; }
    .meta strong { color:#3f00d8; }
    .content { white-space:pre-wrap; font-size:14px; line-height:1.8; color:#333; margin-bottom:32px; padding:20px; background:#fafafa; border-radius:10px; border:1px solid #eee; }
    .signature-box { text-align:center; padding:24px; background:#fff; border:2px dashed #ddd; border-radius:12px; margin-bottom:24px; }
    .signature-box img { max-width:400px; max-height:150px; }
    .signature-box .label { font-size:11px; color:#999; margin-top:8px; }
    .verify { background:#1c005f; color:#fff; padding:20px 24px; border-radius:12px; font-size:12px; word-break:break-all; }
    .verify h4 { color:#FF6600; margin-bottom:8px; font-size:14px; }
    .verify code { background:rgba(255,255,255,.1); padding:2px 6px; border-radius:4px; }
    .footer { text-align:center; padding:24px; color:#999; font-size:11px; border-top:1px solid #eee; }
    .footer strong { color:#3f00d8; }
    .stamp { position:absolute; top:30px; right:40px; border:4px solid rgba(255,255,255,.3); border-radius:50%; width:90px; height:90px; display:flex; align-items:center; justify-content:center; transform:rotate(-15deg); font-weight:900; font-size:11px; text-transform:uppercase; color:rgba(255,255,255,.5); text-align:center; line-height:1.2; }
    @media print {
      body { background:#fff; }
      .page { box-shadow:none; margin:0; max-width:100%; border-radius:0; }
      .header { background:#3f00d8 !important; -webkit-print-color-adjust:exact; print-color-adjust:exact; }
    }
  </style>
</head>
<body>
  <div class="page">
    <div class="header">
      <div class="stamp">FIRMADO<br>✓</div>
      <div class="logo">Placeta<span>Junior</span></div>
      <div class="sub">Grupo de La Placeta — Documento Legal Verificable</div>
    </div>

    <div class="body">
      <h2 style="color:#1c005f;margin-bottom:16px;font-size:22px">📜 ${docInfo.titulo || doc.titulo_documento}</h2>

      <div class="meta">
        <div><strong>Código:</strong> ${docCode}</div>
        <div><strong>Versión:</strong> 1.0</div>
        <div><strong>Firmado por:</strong> ${tutorNombre}</div>
        <div><strong>Fecha:</strong> ${fecha}</div>
        ${junior ? `<div><strong>Menor:</strong> ${junior.nombre} ${junior.apellidos} (${junior.dip})</div>` : ''}
        ${junior ? `<div><strong>Tutor:</strong> ${junior.tutor_nombre || '—'} (${junior.tutor_dip || '—'})</div>` : ''}
        <div><strong>ID Documento:</strong> ${doc.id}</div>
        <div><strong>Estado:</strong> ${doc.estado}</div>
      </div>

      <h3 style="color:#3f00d8;margin-bottom:8px;font-size:16px">Contenido del documento</h3>
      <div class="content">${docInfo.contenido.replace(/\n/g, '<br>')}</div>

      ${firmaImg ? `
      <h3 style="color:#3f00d8;margin-bottom:8px;font-size:16px">✍️ Firma manuscrita del tutor</h3>
      <div class="signature-box">
        <img src="${firmaImg}" alt="Firma manuscrita" />
        <div class="label">Firma digitalizada de ${tutorNombre} — Válida conforme a eIDAS (UE 910/2014)</div>
      </div>` : ''}

      <div class="verify">
        <h4>🔐 Verificación de autenticidad</h4>
        <p>Este documento ha sido firmado digitalmente con firma manuscrita digitalizada, con plena validez legal conforme al <strong>Reglamento eIDAS (UE 910/2014)</strong> y la <strong>Ley 6/2020</strong> de servicios electrónicos de confianza.</p>
        <p style="margin-top:8px"><strong>Hash SHA-256:</strong> <code>${doc.hash_documento || '—'}</code></p>
        <p style="margin-top:4px"><strong>ID de verificación:</strong> <code>PJ-${doc.id}-${docCode}</code></p>
        <p style="margin-top:8px;font-size:10px;opacity:.7">Para verificar la autenticidad de este documento, contacte con Grupo de La Placeta a través de grupodelaplaceta.vercel.app o la app PlacetaID Móvil.</p>
      </div>
    </div>

    <div class="footer">
      <strong>Placeta Junior</strong> · Grupo de La Placeta · Documento generado el ${fecha}<br>
      Este documento es legalmente vinculante. Cualquier modificación invalida su autenticidad.
    </div>
  </div>
</body>
</html>`;

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(html);
  } catch (err) {
    console.error('[Legal] Error generando documento PDF:', err);
    res.status(500).send('<h1>Error al generar el documento</h1>');
  }
});

export default router;
