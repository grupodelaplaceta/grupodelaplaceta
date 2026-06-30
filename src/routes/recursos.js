import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import crypto from 'crypto';

const router = Router();

// ── MÓDULO 5: RECURSOS DIGITALES Y PERMISOS ────────────────────────────────

// GET /api/recursos/asignados - Listar recursos del usuario logueado
router.get('/asignados', verificarSesion, (req, res) => {
  const db = getDb();
  const recursos = db.prepare('SELECT * FROM recursos_digitales WHERE usuario_id = ? ORDER BY creado_en DESC').all(req.session.usuario.id);
  res.json(recursos);
});

// POST /api/recursos/asignar - Asignar recurso a usuario (Admin)
router.post('/asignar', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const { usuario_id, tipo, identificador } = req.body;

  if (!usuario_id || !tipo) return res.status(400).json({ error: 'Usuario, tipo requeridos' });

  // Verificar usuario existe
  const usuario = db.prepare('SELECT id, alias, rol, cargo FROM solicitantes WHERE id = ?').get(usuario_id);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  // Matriz de permisos por perfil operativo
  const permisosPorRol = {
    'administrador': ['email', 'herramienta', 'suscripcion', 'api_key', 'acceso_admin'],
    'junta': ['email', 'herramienta', 'suscripcion', 'acceso_admin'],
    'juez': ['email', 'herramienta', 'acceso_admin'],
    'fiscal': ['email', 'herramienta', 'acceso_admin'],
    'miembro': ['email', 'herramienta'],
    'entidad': ['herramienta', 'suscripcion'],
    'empresa': ['herramienta', 'suscripcion']
  };

  const permisos = permisosPorRol[usuario.rol] || ['herramienta'];
  if (!permisos.includes(tipo)) {
    return res.status(403).json({ error: `El rol "${usuario.rol}" no tiene permisos para asignar recursos de tipo "${tipo}"` });
  }

  db.prepare('INSERT INTO recursos_digitales (usuario_id, tipo, identificador) VALUES (?, ?, ?)')
    .run(usuario_id, tipo, identificador || `${tipo}_${usuario.alias}_${Date.now()}`);

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'asignar_recurso', `Recurso ${tipo} asignado a ${usuario.alias}`, ip);

  res.json({ success: true, message: `Recurso ${tipo} asignado a ${usuario.alias}` });
});

// POST /api/recursos/cesar/:id - [Registrar Cese de Cargo] (Junta Directiva)
router.post('/cesar/:id', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const usuarioId = req.params.id;
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

  const usuario = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(usuarioId);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  // Cambiar rango del usuario
  const cargoAnterior = usuario.cargo;
  db.prepare('UPDATE solicitantes SET rol = ?, cargo = ?, actualizado_en = datetime(\'now\') WHERE id = ?')
    .run('miembro', 'Ciudadano - Sin cargo', usuarioId);

  // Marcar recursos activos para revocación (cuenta atrás 48h)
  db.prepare(`
    UPDATE recursos_digitales SET estado = 'pendiente_revocacion', fecha_revocacion = datetime('now', '+48 hours')
    WHERE usuario_id = ? AND estado = 'activo'
  `).run(usuarioId);

  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'registrar_cese', `Cese de ${usuario.alias}. Cargo anterior: ${cargoAnterior}. Recursos marcados para revocación en 48h.`, ip);

  res.json({
    success: true,
    message: `Cese registrado para ${usuario.alias}. Los recursos serán revocados automáticamente en 48 horas.`,
    recursos_afectados: db.prepare('SELECT COUNT(*) as total FROM recursos_digitales WHERE usuario_id = ? AND estado = ?').get(usuarioId, 'pendiente_revocacion').total
  });
});

// POST /api/recursos/verificar-revocaciones - Script de verificación de revocaciones (cronjob)
router.post('/verificar-revocaciones', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const alertas = [];

  // Recursos pendientes de revocación cuya fecha haya pasado
  const pendientes = db.prepare(`
    SELECT r.*, s.alias, s.dip FROM recursos_digitales r
    JOIN solicitantes s ON s.id = r.usuario_id
    WHERE r.estado = 'pendiente_revocacion' AND r.fecha_revocacion <= datetime('now')
  `).all();

  for (const recurso of pendientes) {
    db.prepare('UPDATE recursos_digitales SET estado = ? WHERE id = ?').run('revocado', recurso.id);
    alertas.push({ alias: recurso.alias, recurso: recurso.tipo, identificador: recurso.identificador });
  }

  // Alertas rojas: recursos que deberían estar revocados pero siguen activos (más de 48h sin revocar)
  const incumplimientos = db.prepare(`
    SELECT r.*, s.alias, s.dip FROM recursos_digitales r
    JOIN solicitantes s ON s.id = r.usuario_id
    WHERE r.estado = 'pendiente_revocacion' AND r.fecha_revocacion < datetime('now', '-1 hour')
  `).all();

  for (const inc of incumplimientos) {
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle) VALUES (?, ?, ?)')
      .run(null, 'alerta_revocacion', `ALERTA: Recurso ${inc.tipo} de ${inc.alias} no revocado en plazo de 48h. ID recurso: ${inc.id}`);
  }

  res.json({
    success: true,
    message: `Verificación completada. ${alertas.length} recursos revocados. ${incumplimientos.length} alertas de incumplimiento.`,
    revocados: alertas,
    alertas_incumplimiento: incumplimientos.length
  });
});

export default router;
