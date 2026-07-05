import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import PDFGenerator from '../services/pdfGenerator.js';

const router = Router();

// ═══════════════════════════════════════════════════════════════════════════
//  TRAMITACIÓN — Workflow completo
// ═══════════════════════════════════════════════════════════════════════════

// GET /api/admin/tramites — Listar todos los trámites
router.get('/tramites', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const tramites = db.prepare(`
      SELECT d.*, s.alias, s.dip, s.nombre_real, s.email, s.telefono
      FROM documentos_tramites d
      JOIN solicitantes s ON d.usuario_id = s.id
      ORDER BY d.created_at DESC LIMIT 200
    `).all();
    res.json(tramites);
  } catch (err) {
    res.json([]);
  }
});

// GET /api/admin/tramites/pendientes
router.get('/tramites/pendientes', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  try {
    const db = getDb();
    const pendientes = db.prepare(`
      SELECT d.*, s.alias, s.dip
      FROM documentos_tramites d
      JOIN solicitantes s ON d.usuario_id = s.id
      WHERE d.estado = 'pendiente' OR d.estado IS NULL
      ORDER BY d.created_at ASC
    `).all();
    res.json(pendientes);
  } catch (err) {
    res.json([]);
  }
});

// POST /api/admin/tramites/aprobar/:id
router.post('/tramites/aprobar/:id', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  try {
    const db = getDb();
    const tramite = db.prepare('SELECT * FROM documentos_tramites WHERE id = ?').get(req.params.id);
    if (!tramite) return res.status(404).json({ error: 'Trámite no encontrado' });

    db.prepare(`UPDATE documentos_tramites SET estado='aprobado', resuelto_por=?, resuelto_en=datetime(), 
      observaciones=? WHERE id=?`)
      .run(req.session.usuario.id, req.body.observaciones || null, req.params.id);

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?,?,?,?)')
      .run(req.session.usuario.id, 'tramite_aprobar', `Trámite #${req.params.id} aprobado: ${tramite.tipo}`, ip);

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/tramites/rechazar/:id
router.post('/tramites/rechazar/:id', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), (req, res) => {
  try {
    const db = getDb();
    const tramite = db.prepare('SELECT * FROM documentos_tramites WHERE id = ?').get(req.params.id);
    if (!tramite) return res.status(404).json({ error: 'Trámite no encontrado' });

    db.prepare(`UPDATE documentos_tramites SET estado='rechazado', resuelto_por=?, resuelto_en=datetime(),
      observaciones=? WHERE id=?`)
      .run(req.session.usuario.id, req.body.observaciones || 'Sin especificar', req.params.id);

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?,?,?,?)')
      .run(req.session.usuario.id, 'tramite_rechazar', `Trámite #${req.params.id} rechazado: ${tramite.tipo}. Motivo: ${req.body.observaciones || 'No especificado'}`, ip);

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/tramites/generar-pdf/:id — Generar PDF resolutivo
router.post('/tramites/generar-pdf/:id', verificarSesion, verificarRol('administrador', 'junta', 'secretario'), async (req, res) => {
  try {
    const db = getDb();
    const tramite = db.prepare(`
      SELECT d.*, s.alias, s.dip, s.nombre_real
      FROM documentos_tramites d
      JOIN solicitantes s ON d.usuario_id = s.id
      WHERE d.id = ?
    `).get(req.params.id);
    if (!tramite) return res.status(404).json({ error: 'Trámite no encontrado' });

    const generator = new PDFGenerator();
    const doc = generator.generarManual(
      `RESOLUCIÓN DE TRÁMITE #${tramite.id}`,
      [
        { tipo: 'tag', texto: tramite.tipo?.toUpperCase() || 'TRÁMITE' },
        { tipo: 'h1', titulo: `Resolución de Trámite #${tramite.id}` },
        { tipo: 'h2', titulo: 'Datos del solicitante' },
        { tipo: 'campo', nombre: 'Alias', valor: tramite.alias },
        { tipo: 'campo', nombre: 'DIP', valor: tramite.dip },
        { tipo: 'campo', nombre: 'Tipo', valor: tramite.tipo },
        { tipo: 'campo', nombre: 'Estado', valor: tramite.estado || 'Pendiente' },
        { tipo: 'campo', nombre: 'Fecha', valor: tramite.created_at },
        { tipo: 'h2', titulo: 'Contenido' },
        { tipo: 'texto', contenido: tramite.contenido || 'Sin contenido' },
        ...(tramite.observaciones ? [
          { tipo: 'h2', titulo: 'Observaciones' },
          { tipo: 'nota', contenido: tramite.observaciones }
        ] : []),
        { tipo: 'linea' },
        { tipo: 'campo', nombre: 'Resuelto por', valor: req.session.usuario.alias },
        { tipo: 'campo', nombre: 'Fecha resolución', valor: new Date().toLocaleString('es-ES') }
      ],
      { version: '1.0', subtitulo: 'Documento oficial', autor: 'GDLP CRM' }
    );

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="resolucion-tramite-${tramite.id}.pdf"`);
    doc.pipe(res);
    doc.end();
  } catch (err) {
    console.error('[Tramites] Error PDF:', err.message);
    res.status(500).json({ error: 'Error generando PDF' });
  }
});

// GET /api/admin/tramites/tipos — Tipos de trámite disponibles
router.get('/tramites/tipos', verificarSesion, (req, res) => {
  res.json([
    { id: 'alta-tributos', nombre: 'Alta en Tributos', descripcion: 'Solicitud de alta en el sistema tributario TLP' },
    { id: 'solicitar-factura', nombre: 'Solicitud de Factura', descripcion: 'Solicitud de emisión de factura con IVA' },
    { id: 'consulta-tributos', nombre: 'Consulta Tributaria', descripcion: 'Consulta del estado tributario' },
    { id: 'solicitar-dip', nombre: 'Solicitud de DIP', descripcion: 'Solicitud de Documento de Identidad Placeta' },
    { id: 'baja-voluntaria', nombre: 'Baja Voluntaria', descripcion: 'Solicitud de baja del ecosistema' },
    { id: 'reclamacion', nombre: 'Reclamación', descripcion: 'Presentar una reclamación administrativa' },
    { id: 'sugerencia', nombre: 'Sugerencia', descripcion: 'Enviar una sugerencia a la administración' }
  ]);
});

// ═══════════════════════════════════════════════════════════════════════════
//  NOTIFICACIONES — Sistema de notificaciones internas
// ═══════════════════════════════════════════════════════════════════════════

// GET /api/admin/notificaciones
router.get('/notificaciones', verificarSesion, (req, res) => {
  try {
    const db = getDb();
    const notifs = db.prepare(`
      SELECT * FROM notificaciones 
      WHERE usuario_id = ? OR usuario_id IS NULL
      ORDER BY created_at DESC LIMIT 50
    `).all(req.session.usuario.id);
    const noLeidas = notifs.filter(n => !n.leida).length;
    res.json({ notificaciones: notifs, noLeidas });
  } catch (err) {
    res.json({ notificaciones: [], noLeidas: 0 });
  }
});

// POST /api/admin/notificaciones/crear
router.post('/notificaciones/crear', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const { titulo, mensaje, usuarioId, tipo } = req.body;
    if (!titulo || !mensaje) return res.status(400).json({ error: 'Título y mensaje requeridos' });

    db.prepare(`INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, creado_por, created_at)
      VALUES (?, ?, ?, ?, ?, datetime())`)
      .run(usuarioId || null, titulo, mensaje, tipo || 'informativa', req.session.usuario.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/notificaciones/leer/:id
router.post('/notificaciones/leer/:id', verificarSesion, (req, res) => {
  try {
    const db = getDb();
    db.prepare('UPDATE notificaciones SET leida=1, leida_en=datetime() WHERE id=?').run(req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  SANCIONES — Gestión administrativa
// ═══════════════════════════════════════════════════════════════════════════

// GET /api/admin/sanciones
router.get('/sanciones', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  try {
    const db = getDb();
    const sanciones = db.prepare(`
      SELECT m.*, s.alias, s.dip
      FROM multas_automaticas m
      JOIN solicitantes s ON m.usuario_id = s.id
      ORDER BY m.creado_en DESC LIMIT 200
    `).all();
    res.json(sanciones);
  } catch (err) {
    res.json([]);
  }
});

// POST /api/admin/sanciones/aplicar
router.post('/sanciones/aplicar', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  try {
    const db = getDb();
    const { usuarioId, tipo, importe, motivo } = req.body;
    if (!usuarioId || !importe || !motivo) return res.status(400).json({ error: 'Faltan datos' });

    db.prepare(`INSERT INTO multas_automaticas (usuario_id, tipo, importe, motivo, estado, creado_en)
      VALUES (?, ?, ?, ?, 'pendiente', datetime())`)
      .run(usuarioId, tipo || 'administrativa', importe, motivo);

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?,?,?,?)')
      .run(req.session.usuario.id, 'sancion_aplicar', `Sanción de ${importe} Pz a usuario #${usuarioId}: ${motivo}`, ip);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  VIGILANCIA — Monitoreo del sistema
// ═══════════════════════════════════════════════════════════════════════════

// GET /api/admin/vigilancia/logs — Logs de auditoría
router.get('/vigilancia/logs', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const limit = Math.min(parseInt(req.query.limit) || 100, 500);
    const logs = db.prepare(`
      SELECT l.*, s.alias 
      FROM logs_auditoria l
      LEFT JOIN solicitantes s ON l.usuario_id = s.id
      ORDER BY l.created_at DESC LIMIT ?
    `).all(limit);
    res.json(logs);
  } catch (err) {
    res.json([]);
  }
});

// GET /api/admin/vigilancia/alertas — Alertas del sistema
router.get('/vigilancia/alertas', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const alertas = [];
    const cuentasNegativas = db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE saldo < 0 AND estado='activa'").get().total;
    if (cuentasNegativas > 0) alertas.push({ tipo: 'warning', mensaje: `${cuentasNegativas} cuentas en descubierto`, fecha: new Date().toISOString() });

    const expedientesAbiertos = db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')").get().total;
    if (expedientesAbiertos > 0) alertas.push({ tipo: 'info', mensaje: `${expedientesAbiertos} expedientes abiertos`, fecha: new Date().toISOString() });

    const solicitudesPendientes = db.prepare("SELECT COUNT(*) as total FROM documentos_tramites WHERE estado='pendiente' OR estado IS NULL").get().total;
    if (solicitudesPendientes > 0) alertas.push({ tipo: 'info', mensaje: `${solicitudesPendientes} trámites pendientes`, fecha: new Date().toISOString() });

    const bonoPendientes = db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE bono_bienvenida_activo=1").get().total;
    if (bonoPendientes > 0) alertas.push({ tipo: 'success', mensaje: `${bonoPendientes} bonos de bienvenida pendientes`, fecha: new Date().toISOString() });

    const erroresRecientes = db.prepare("SELECT COUNT(*) as total FROM logs_auditoria WHERE accion LIKE '%error%' AND created_at >= datetime('now', '-1 day')").get().total;
    if (erroresRecientes > 0) alertas.push({ tipo: 'danger', mensaje: `${erroresRecientes} errores en las últimas 24h`, fecha: new Date().toISOString() });

    res.json(alertas);
  } catch (err) {
    res.json([]);
  }
});

// GET /api/admin/vigilancia/stats — Estadísticas del sistema
router.get('/vigilancia/stats', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const stats = {
      totalUsuarios: db.prepare('SELECT COUNT(*) as total FROM solicitantes').get().total,
      usuariosActivos: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='activo'").get().total,
      usuariosHoy: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE creado_en >= datetime('now', '-1 day')").get().total,
      transaccionesHoy: db.prepare("SELECT COUNT(*) as total FROM transacciones WHERE creado_en >= datetime('now', '-1 day')").get().total,
      logsHoy: db.prepare("SELECT COUNT(*) as total FROM logs_auditoria WHERE created_at >= datetime('now', '-1 day')").get().total,
      dbSize: 0
    };
    res.json(stats);
  } catch (err) {
    res.json({ error: err.message });
  }
});

export default router;
