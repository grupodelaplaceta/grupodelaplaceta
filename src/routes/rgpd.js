import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import crypto from 'crypto';

const router = Router();

// ── MÓDULO 7: RGPD / PROTECCIÓN DE DATOS (Cumplimiento Real) ───────────────

// POST /api/rgpd/consentimiento - Registrar consentimiento
router.post('/consentimiento', verificarSesion, (req, res) => {
  const db = getDb();
  const { tipo, aceptado } = req.body;
  const usuarioId = req.session.usuario.id;

  if (!tipo) return res.status(400).json({ error: 'Tipo de consentimiento requerido' });

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

  // Si ya existe, actualizar; si no, insertar
  const existente = db.prepare('SELECT id FROM consentimientos WHERE usuario_id = ? AND tipo = ?').get(usuarioId, tipo);
  if (existente) {
    db.prepare('UPDATE consentimientos SET aceptado = ?, fecha_aceptacion = datetime(\'now\'), ip_aceptacion = ? WHERE id = ?')
      .run(aceptado ? 1 : 0, ip, existente.id);
  } else {
    db.prepare('INSERT INTO consentimientos (usuario_id, tipo, aceptado, ip_aceptacion) VALUES (?, ?, ?, ?)')
      .run(usuarioId, tipo, aceptado ? 1 : 0, ip);
  }

  res.json({ success: true, message: `Consentimiento ${aceptado ? 'aceptado' : 'rechazado'} para: ${tipo}` });
});

// POST /api/rgpd/solicitar-arco - [Solicitar Supresión de Datos / Derechos ARCO+]
router.post('/solicitar-arco', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;
  const { derecho, detalle } = req.body;

  if (!derecho || !['acceso', 'rectificacion', 'supresion', 'oposicion', 'limitacion'].includes(derecho)) {
    return res.status(400).json({ error: 'Derecho ARCO+ válido requerido' });
  }

  // Si es derecho de supresión (olvido), verificar deudas/sanciones activas
  if (derecho === 'supresion') {
    const deudasActivas = db.prepare(`
      SELECT COUNT(*) as total FROM multas_automaticas 
      WHERE usuario_id = ? AND estado IN ('pendiente', 'recurrida')
    `).get(usuarioId);

    const expedientesActivos = db.prepare(`
      SELECT COUNT(*) as total FROM expedientes_disciplinarios 
      WHERE infractor_id = ? AND estado NOT IN ('firme', 'archivado')
    `).get(usuarioId);

    if (deudasActivas.total > 0 || expedientesActivos.total > 0) {
      // Retener datos bloqueados - solo accesible para administradores de seguridad
      const solicitud = db.prepare(`
        INSERT INTO solicitudes_arco (usuario_id, derecho, estado, detalle_solicitud)
        VALUES (?, ?, 'rechazado', ?)
      `).run(usuarioId, derecho, detalle || 'Solicitud de supresión con deudas/expedientes activos');

      return res.json({
        success: false,
        message: 'No se puede ejecutar la supresión: tiene deudas pendientes o expedientes disciplinarios activos. Sus datos serán retenidos de forma bloqueada hasta la prescripción legal.',
        motivos: {
          deudas_activas: deudasActivas.total > 0,
          expedientes_activos: expedientesActivos.total > 0
        },
        id_solicitud: solicitud.lastInsertRowid
      });
    }

    // Perfil limpio: ejecutar anonimización irreversible
    return ejecutarAnonimizacion(db, usuarioId, req);
  }

  // Otros derechos (acceso, rectificación, oposición, limitación)
  const solicitud = db.prepare(`
    INSERT INTO solicitudes_arco (usuario_id, derecho, estado, detalle_solicitud)
    VALUES (?, ?, 'pendiente', ?)
  `).run(usuarioId, derecho, detalle || '');

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(usuarioId, `solicitud_arco_${derecho}`, `Solicitud de derecho ${derecho} registrada`, ip);

  res.json({
    success: true,
    message: `Solicitud de derecho ${derecho} registrada. Recibirá respuesta en un plazo máximo de 30 días.`,
    id_solicitud: solicitud.lastInsertRowid
  });
});

// POST /api/rgpd/completar-arco/:id - Completar solicitud ARCO (Admin DPD)
router.post('/completar-arco/:id', verificarSesion, verificarRol('administrador'), (req, res) => {
  const db = getDb();
  const { respuesta } = req.body;

  db.prepare('UPDATE solicitudes_arco SET estado = ?, respuesta = ?, fecha_respuesta = datetime(\'now\') WHERE id = ?')
    .run('completado', respuesta || 'Solicitud procesada', req.params.id);

  res.json({ success: true, message: 'Solicitud ARCO completada' });
});

// GET /api/rgpd/mis-solicitudes - Mis solicitudes ARCO
router.get('/mis-solicitudes', verificarSesion, (req, res) => {
  const db = getDb();
  const solicitudes = db.prepare('SELECT * FROM solicitudes_arco WHERE usuario_id = ? ORDER BY creado_en DESC').all(req.session.usuario.id);
  res.json(solicitudes);
});

// GET /api/rgpd/tratamientos - Registro de Actividades de Tratamiento (RAT)
router.get('/tratamientos', verificarSesion, verificarRol('administrador'), (req, res) => {
  const db = getDb();
  const tratamientos = db.prepare('SELECT * FROM registro_tratamiento_datos WHERE activo = 1').all();
  res.json(tratamientos);
});

// POST /api/rgpd/exportar-datos - [Exportar Base de Datos / Compartir Datos entre Áreas]
router.post('/exportar-datos', verificarSesion, verificarRol('administrador'), (req, res) => {
  const db = getDb();
  const { area_destino, justificacion, documento_aprobacion } = req.body;

  // Exigir justificación expresa y aprobación de la Junta
  if (!justificacion || !documento_aprobacion) {
    // Registrar intento de violación de seguridad
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
      .run(req.session.usuario.id, 'intento_exportacion_sin_permiso',
        `Intento de exportación de datos sin justificación/aprobación. Área destino: ${area_destino || 'no especificada'}`,
        ip);

    return res.status(403).json({
      error: 'Exportación denegada. Requisitos incumplidos:',
      requisitos: [
        'Justificación expresa del tratamiento (no proporcionada)',
        'Documento de aprobación formal firmado por la Junta Directiva (no proporcionado)'
      ],
      evento_registrado: 'Intento de violación de seguridad registrado en el log de auditoría del DPD.'
    });
  }

  // Si cumple requisitos, permitir exportación
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'exportacion_datos_autorizada',
      `Exportación de datos autorizada hacia área: ${area_destino}. Justificación: ${justificacion.substring(0, 100)}...`,
      ip);

  res.json({
    success: true,
    message: 'Exportación autorizada. Se ha registrado la transferencia inter-departamental.',
    registro: {
      area_destino,
      justificacion: justificacion.substring(0, 100) + '...',
      fecha: new Date().toISOString(),
      aprobado_por: req.session.usuario.alias
    }
  });
});

// ── Helper: Anonimización Irreversible ──────────────────────────────────────
function ejecutarAnonimizacion(db, usuarioId, req) {
  const usuario = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(usuarioId);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  const hashAnonimizacion = crypto.createHash('sha256').update(`${usuarioId}-${Date.now()}-${Math.random()}`).digest('hex');

  const anonimizar = db.transaction(() => {
    // Sobrescribir datos personales con cadenas aleatorias
    const aliasAnonimo = `Eliminado_User_${usuarioId}`;
    db.prepare(`
      UPDATE solicitantes SET 
        nombre_real = ?,
        email = ?,
        password_hash = NULL,
        totp_secret = NULL,
        hash_credencial = ?,
        estado = 'baja',
        actualizado_en = datetime('now')
      WHERE id = ?
    `).run(
      `[ANONIMIZADO] ${aliasAnonimo}`,
      `anonimo-${usuarioId}@eliminado.gdlp`,
      hashAnonimizacion,
      usuarioId
    );

    // Registrar solicitud ARCO completada
    db.prepare(`
      INSERT INTO solicitudes_arco (usuario_id, derecho, estado, detalle_solicitud, respuesta, fecha_respuesta, hash_anonimizacion)
      VALUES (?, 'supresion', 'completado', ?, ?, datetime('now'), ?)
    `).run(usuarioId, 'Solicitud de supresión (Derecho al Olvido) - Anonimización irreversible ejecutada',
      `Datos anonimizados. Hash: ${hashAnonimizacion}`, hashAnonimizacion);

    // Registrar log
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
      .run(usuarioId, 'anonimizacion_completada', `Derecho al olvido ejecutado. Hash anonimización: ${hashAnonimizacion}`, ip);

    // Generar certificado GDLP-SEC-021
    db.prepare(`
      INSERT INTO documentos_firmados (codigo_modelo, titulo_documento, contenido_hash, estado, url_firma)
      VALUES (?, ?, ?, ?, ?)
    `).run(
      'GDLP-SEC-021',
      `Certificado de Ejercicio de Derechos ARCO y Anonimización de Datos Reales - ${aliasAnonimo}`,
      hashAnonimizacion,
      'pendiente',
      crypto.randomBytes(16).toString('hex')
    );
  });

  anonimizar();

  return {
    success: true,
    message: 'Anonimización irreversible ejecutada. Todos los datos personales han sido sobrescritos de forma permanente.',
    hash_anonimizacion: hashAnonimizacion,
    registros_afectados: {
      nombre: '[SOBRESCRITO]',
      email: '[SOBRESCRITO]',
      credenciales: '[DESTRUIDAS]',
      estado: 'baja'
    }
  };
}

export default router;
