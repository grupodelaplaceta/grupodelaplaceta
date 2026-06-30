import { Router } from 'express';
import { verificarSesion } from '../middleware/auth.js';
import FirmaDigitalService from '../services/firmaDigital.js';

const router = Router();
const firmaService = new FirmaDigitalService();

// ── SISTEMA DE FIRMA ONLINE VÍA URL PÚBLICA ────────────────────────────────

// POST /api/firma/crear - Crear documento para firma
router.post('/crear', verificarSesion, async (req, res) => {
  try {
    const { codigoModelo, titulo, contenido } = req.body;
    if (!codigoModelo || !titulo) return res.status(400).json({ error: 'Código de modelo y título requeridos' });

    const resultado = await firmaService.crearDocumentoParaFirma({
      codigoModelo, titulo, contenido: contenido || titulo, creadoPor: req.session.usuario.id
    });
    res.json({ success: true, message: 'Documento creado. Comparte la URL pública para la firma.', ...resultado });
  } catch (err) {
    console.error('Error creando documento:', err);
    res.status(500).json({ error: 'Error creando documento para firma' });
  }
});

// POST /api/firma/firmar/:token - Firmar documento mediante URL pública
router.post('/firmar/:token', verificarSesion, async (req, res) => {
  try {
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const resultado = await firmaService.firmarDocumento(req.params.token, req.session.usuario.id, ip);
    res.json(resultado);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// POST /api/firma/rechazar/:token - Rechazar documento
router.post('/rechazar/:token', verificarSesion, async (req, res) => {
  try {
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const resultado = await firmaService.rechazarDocumento(req.params.token, req.session.usuario.id, ip, req.body.motivo);
    res.json(resultado);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// GET /api/firma/verificar/:id - Verificar documento firmado
router.get('/verificar/:id', verificarSesion, async (req, res) => {
  try {
    const resultado = await firmaService.verificarDocumento(req.params.id);
    res.json(resultado);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// GET /api/firma/pendientes - Documentos pendientes del usuario
router.get('/pendientes', verificarSesion, async (req, res) => {
  try {
    const pendientes = await firmaService.obtenerPendientes(req.session.usuario.id);
    res.json(pendientes);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// GET /api/firma/documentos - Todos los documentos (admin)
router.get('/documentos', verificarSesion, async (req, res) => {
  try {
    const { sbListDocumentosFirmados } = await import('../config/db-supabase.js');
    const docs = await sbListDocumentosFirmados(100);
    res.json(docs);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
