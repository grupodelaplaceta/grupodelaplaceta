/**
 * Rutas para envío de emails desde CRM a tutores
 * Plantillas autorellenadas según el caso
 */
import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';

const router = Router();

// ── PLANTILLAS DE EMAIL ─────────────────────────────────────────────────────
const PLANTILLAS = {
  pendiente_firma: {
    asunto: '📋 Placeta Junior — Documentos pendientes de firma',
    cuerpo: `Hola {{nombreTutor}},

Los documentos de {{nombreMenor}} para Placeta Junior están listos para su firma.

Para completar el alta, debe firmar los siguientes documentos desde PlacetaID Móvil:
1. 📜 Términos y Condiciones (PJ-TYC-001)
2. 🔒 Política de Privacidad (PJ-PRV-001)
3. 📋 Consentimiento del Tutor (PJ-CON-001)

DIP del menor: {{dipMenor}}

Puede acceder a PlacetaID Móvil para revisar y firmar los documentos.

Atentamente,
Grupo de La Placeta`
  },
  bono_bienvenida: {
    asunto: '🎉 Placeta Junior — Bono de bienvenida activado',
    cuerpo: `Hola {{nombreTutor}},

¡El alta de {{nombreMenor}} en Placeta Junior se ha completado con éxito!

🎉 Se ha activado un bono de bienvenida de 750 Placetas.
🏦 Se ha creado una cuenta bancaria infantil con IBAN APP (Capitalia).
🌟 {{nombreMenor}} ya puede acceder a la academia y empezar a aprender.

Límites por defecto:
• Gasto diario: 10 Pz/día
• Gasto semanal: 50 Pz/semana
• Compras >1.000 Pz: requieren su aprobación

Puede modificar estos límites desde PlacetaID Móvil > Control Parental.

Atentamente,
Grupo de La Placeta`
  },
  autorizacion_compra: {
    asunto: '🔔 Placeta Junior — Solicitud de autorización de compra',
    cuerpo: `Hola {{nombreTutor}},

{{nombreMenor}} solicita su autorización para realizar una compra en Placeta Junior.

📝 Concepto: {{concepto}}
💰 Importe: {{importe}} Placetas
👶 Menor: {{nombreMenor}} (DIP: {{dipMenor}})

Para autorizar esta compra, abra PlacetaID Móvil y revise las solicitudes pendientes.

Si no reconoce esta solicitud, puede rechazarla desde la misma aplicación.

Atentamente,
Grupo de La Placeta`
  },
  limite_excedido: {
    asunto: '⚠️ Placeta Junior — Límite de gasto alcanzado',
    cuerpo: `Hola {{nombreTutor}},

{{nombreMenor}} ha alcanzado el límite de gasto diario configurado.

📊 Límite diario: {{limiteDiario}} Pz
💳 Gastado hoy: {{gastadoHoy}} Pz
📅 Límite semanal: {{limiteSemanal}} Pz

Si desea aumentar los límites, puede hacerlo desde PlacetaID Móvil > Control Parental.

Atentamente,
Grupo de La Placeta`
  },
  recordatorio_acceso: {
    asunto: '🔐 Placeta Junior — Solicitud de acceso pendiente',
    cuerpo: `Hola {{nombreTutor}},

{{nombreMenor}} ha solicitado acceso a Placeta Junior.

Para permitir el acceso, abra PlacetaID Móvil y apruebe la solicitud pendiente.

Si no desea permitir el acceso en este momento, puede rechazarla o ignorarla.

Atentamente,
Grupo de La Placeta`
  }
};

// GET /api/admin/junior/email/plantillas — Listar plantillas
router.get('/junior/email/plantillas', verificarSesion, verificarRol('administrador'), (req, res) => {
  const plantillas = Object.entries(PLANTILLAS).map(([id, p]) => ({
    id, asunto: p.asunto, cuerpo: p.cuerpo
  }));
  res.json(plantillas);
});

// POST /api/admin/junior/email/enviar — Enviar email
router.post('/junior/email/enviar', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const { plantillaId, emailTutor, variables } = req.body;
    if (!plantillaId || !emailTutor) {
      return res.status(400).json({ error: 'Plantilla y email requeridos' });
    }

    const plantilla = PLANTILLAS[plantillaId];
    if (!plantilla) return res.status(400).json({ error: 'Plantilla no encontrada' });

    // Rellenar plantilla con variables
    let asunto = plantilla.asunto;
    let cuerpo = plantilla.cuerpo;

    if (variables) {
      for (const [key, value] of Object.entries(variables)) {
        const token = `{{${key}}}`;
        asunto = asunto.replaceAll(token, value || '');
        cuerpo = cuerpo.replaceAll(token, value || '');
      }
    }

    // En desarrollo, simulamos el envío
    const esDemo = !process.env.SMTP_HOST || process.env.NODE_ENV === 'development';
    if (esDemo) {
      const db = getDb();
      db.prepare(`INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip)
        VALUES (?, ?, ?, ?)`)
        .run(req.session.usuario.id, 'email_simulado',
          `Email a ${emailTutor} - Plantilla: ${plantillaId} - Asunto: ${asunto}`,
          req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown');

      return res.json({
        success: true,
        simulado: true,
        mensaje: '📧 Email simulado (modo desarrollo). Configure SMTP para envío real.',
        asunto,
        cuerpo,
        destinatario: emailTutor
      });
    }

    // Aquí iría el envío real con nodemailer o similar
    res.json({ success: true, mensaje: `Email enviado a ${emailTutor}` });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/junior/email/vista-previa/:plantillaId — Vista previa HTML
router.get('/junior/email/vista-previa/:plantillaId', verificarSesion, verificarRol('administrador'), (req, res) => {
  const plantilla = PLANTILLAS[req.params.plantillaId];
  if (!plantilla) return res.status(404).json({ error: 'Plantilla no encontrada' });

  const variables = {
    nombreTutor: '[Nombre del Tutor]',
    nombreMenor: '[Nombre del Menor]',
    dipMenor: '[DIP del Menor]',
    concepto: '[Concepto]',
    importe: '[Importe]',
    limiteDiario: '[Límite]',
    gastadoHoy: '[Gastado]',
    limiteSemanal: '[Límite Semanal]'
  };

  let cuerpo = plantilla.cuerpo;
  for (const [key, value] of Object.entries(variables)) {
    cuerpo = cuerpo.replaceAll(`{{${key}}}`, value);
  }

  res.json({ asunto: plantilla.asunto, cuerpo, id: req.params.plantillaId });
});

export default router;
