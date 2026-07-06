import { Router } from 'express';
import { verificarSesion } from '../middleware/auth.js';
import { getDb } from '../config/db-supabase.js';

const router = Router();

// ── Listar notificaciones ──────────────────────────────────────────────
router.get('/', verificarSesion, async (req, res) => {
  try {
    const db = getDb();
    const notis = db.prepare(`
      SELECT n.*, u.alias as autor_nombre
      FROM notificaciones n
      LEFT JOIN solicitantes u ON n.creado_por = u.id
      WHERE n.usuario_id IS NULL OR n.usuario_id = ?
      ORDER BY n.created_at DESC LIMIT 50
    `).all(req.session.usuario?.id || 0);
    res.json(notis);
  } catch (e) {
    res.json([]);
  }
});

// ── Crear notificación ─────────────────────────────────────────────────
router.post('/', verificarSesion, async (req, res) => {
  try {
    const { titulo, mensaje, tipo, usuario_id } = req.body;
    if (!titulo || !mensaje) return res.status(400).json({ error: 'Título y mensaje requeridos' });
    const db = getDb();
    const info = db.prepare(`
      INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, creado_por, created_at)
      VALUES (?, ?, ?, ?, ?, datetime('now'))
    `).run(usuario_id || null, titulo, mensaje, tipo || 'informativa', req.session.usuario?.id || null);
    res.json({ id: info.lastInsertRowid, ok: true, message: '✅ Notificación creada' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── Marcar como leída ──────────────────────────────────────────────────
router.post('/:id/leer', verificarSesion, async (req, res) => {
  try {
    const db = getDb();
    db.prepare(`UPDATE notificaciones SET leida = 1, leida_en = datetime('now') WHERE id = ?`).run(req.params.id);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── No leídas count ────────────────────────────────────────────────────
router.get('/no-leidas', verificarSesion, async (req, res) => {
  try {
    const db = getDb();
    const row = db.prepare(`
      SELECT COUNT(*) as total FROM notificaciones
      WHERE (usuario_id IS NULL OR usuario_id = ?) AND leida = 0
    `).get(req.session.usuario?.id || 0);
    res.json({ total: row?.total || 0 });
  } catch (e) {
    res.json({ total: 0 });
  }
});

// ── Crear notificación automática (desde otros módulos) ────────────────
export function crearNotificacion({ titulo, mensaje, tipo = 'informativa', usuario_id = null, creado_por = null }) {
  try {
    const db = getDb();
    db.prepare(`
      INSERT INTO notificaciones (usuario_id, titulo, mensaje, tipo, creado_por, created_at)
      VALUES (?, ?, ?, ?, ?, datetime('now'))
    `).run(usuario_id, titulo, mensaje, tipo, creado_por);
    return true;
  } catch (e) {
    console.error('[notifications] Error creating notification:', e.message);
    return false;
  }
}

export default router;
