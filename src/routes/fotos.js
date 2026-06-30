import { Router } from 'express';
import { sbToggleLike, sbGetAllLikes, sbGetFotoLikes } from '../config/db-supabase.js';

const router = Router();

// ── Dar/quitar like a una foto ─────────────────────────────────────────────

router.post('/like', async (req, res) => {
  const { foto_url } = req.body;
  if (!foto_url) return res.status(400).json({ error: 'URL de foto requerida' });
  try {
    const result = await sbToggleLike(foto_url, req.session.usuario?.id || 0);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Obtener estado de likes ─────────────────────────────────────────────────

router.get('/likes', async (req, res) => {
  try {
    const result = await sbGetAllLikes();
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Obtener likes de una foto específica ────────────────────────────────────

router.get('/likes/:foto', async (req, res) => {
  try {
    const total = await sbGetFotoLikes(req.params.foto);
    res.json({ total, liked: false });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
