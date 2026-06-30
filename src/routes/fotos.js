import { Router } from 'express';
import { sbToggleLike, sbGetAllLikes, sbGetFotoLikes, sbListContenidos } from '../config/db-supabase.js';

const router = Router();

const GALERIA_IMAGENES = [
  { url: 'https://i.postimg.cc/8kK3wPN6/uve.jpg', alt: 'UVE' },
  { url: 'https://i.postimg.cc/fWv4QVJx/vlcsnap-2026-02-06-00h47m04s050.png', alt: 'Universo' },
  { url: 'https://i.postimg.cc/Wp7Rcdhr/vlcsnap-2026-02-06-00h46m42s029.png', alt: 'Entorno' },
  { url: 'https://i.postimg.cc/Hndf7gP3/vlcsnap-2026-02-06-00h46m12s002.png', alt: 'Escenario' },
  { url: 'https://i.postimg.cc/264s66Tb/vlcsnap-2026-02-06-00h42m54s541.png', alt: 'Vista ecosistema' }
];

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

// ── Lista de imágenes de la galería ─────────────────────────────────────────

router.get('/galeria', async (req, res) => {
  try {
    const likes = await sbGetAllLikes();
    const imagenes = GALERIA_IMAGENES.map(img => ({
      ...img,
      likes: likes[img.url] || 0
    }));
    res.json(imagenes);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
