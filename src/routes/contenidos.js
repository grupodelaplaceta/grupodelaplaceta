import { Router } from 'express';
import { sbListContenidos, sbGetContenidoBySlug, sbCreateContenido, sbUpdateContenido, sbDeleteContenido } from '../config/db-supabase.js';

const router = Router();

// ── Listar contenidos ────────────────────────────────────────────────────────

router.get('/', async (req, res) => {
  try {
    const contenidos = await sbListContenidos();
    res.json(contenidos);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Crear contenido ──────────────────────────────────────────────────────────

router.post('/', async (req, res) => {
  const { titulo, slug, contenido, tipo, estado, meta_desc } = req.body;
  if (!titulo || !slug || !contenido) {
    return res.status(400).json({ error: 'Título, slug y contenido son obligatorios' });
  }
  try {
    const result = await sbCreateContenido({
      titulo, slug, contenido, tipo: tipo || 'pagina',
      meta_desc: meta_desc || '', estado: estado || 'publicado',
      autor_id: req.session.usuario?.id || null
    });
    res.json({ success: true, message: 'Contenido creado', id: result.id, slug });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Obtener contenido por slug ───────────────────────────────────────────────

router.get('/:slug', async (req, res) => {
  try {
    const contenido = await sbGetContenidoBySlug(req.params.slug);
    if (!contenido) return res.status(404).json({ error: 'No encontrado' });
    res.json(contenido);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Actualizar contenido ─────────────────────────────────────────────────────

router.put('/:id', async (req, res) => {
  const { titulo, contenido, tipo, estado, meta_desc } = req.body;
  try {
    await sbUpdateContenido(req.params.id, {
      titulo, contenido, tipo: tipo || 'pagina',
      meta_desc: meta_desc || '', estado: estado || 'publicado'
    });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Eliminar contenido ───────────────────────────────────────────────────────

router.delete('/:id', async (req, res) => {
  try {
    await sbDeleteContenido(req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
