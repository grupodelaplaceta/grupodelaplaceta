import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import FirmaDigitalService from '../services/firmaDigital.js';

const router = Router();
const firmaService = new FirmaDigitalService();

// Códigos de modelo que SOLO puede firmar el tutor legal (nunca un admin)
const DOCUMENTOS_TUTOR_ONLY = ['PJ-TYC-001', 'PJ-PRV-001', 'PJ-CON-001'];

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

// POST /api/firma/firmar/:token - Firmar documento (SOLO el tutor legal, NUNCA admin)
router.post('/firmar/:token', verificarSesion, async (req, res) => {
  try {
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const firmante = await (await import('../config/db-supabase.js')).sbFindSolicitanteById(req.session.usuario.id);
    const documento = await (await import('../config/db-supabase.js')).sbFindDocumentoFirmadoByUrl(req.params.token);

    // Bloquear si es documento tutor-only y el firmante es admin
    if (documento && DOCUMENTOS_TUTOR_ONLY.includes(documento.codigo_modelo)) {
      const esAdmin = firmante?.rol === 'administrador' || firmante?.rol === 'admin' || req.session.usuario?.rol === 'administrador';
      if (esAdmin) {
        return res.status(403).json({
          error: '❌ Los administradores NO pueden firmar documentos legales de Placeta Junior. Solo el tutor legal puede hacerlo desde PlacetaID Móvil.',
          codigo: 'FIRMA_BLOQUEADA_ADMIN'
        });
      }
    }

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

// ═══════════════════════════════════════════════════════════════════════════
//  FIRMA GENERAL — PlacetaID Móvil (firma manuscrita base64)
// ═══════════════════════════════════════════════════════════════════════════

// POST /api/firma/firmar-manuscrito — Firmar cualquier documento con base64
router.post('/firmar-manuscrito', verificarSesion, async (req, res) => {
  try {
    const { codigo_modelo, titulo, contenido, firma_base64 } = req.body;
    if (!codigo_modelo || !titulo || !firma_base64) {
      return res.status(400).json({ error: 'codigo_modelo, titulo y firma_base64 requeridos' });
    }

    const { sbFindSolicitanteById } = await import('../config/db-supabase.js');
    const firmante = await sbFindSolicitanteById(req.session.usuario.id);
    if (!firmante) return res.status(404).json({ error: 'Usuario no encontrado' });

    // Block admin from signing tutor-only docs
    if (DOCUMENTOS_TUTOR_ONLY.includes(codigo_modelo)) {
      const esAdmin = firmante.rol === 'administrador' || firmante.rol === 'admin';
      if (esAdmin) {
        return res.status(403).json({
          error: '❌ Los administradores no pueden firmar documentos de Placeta Junior. Solo el tutor legal.',
          codigo: 'FIRMA_BLOQUEADA_ADMIN'
        });
      }
    }

    const crypto = await import('crypto');
    const ahora = new Date().toISOString();
    const firmaHash = crypto.createHash('sha256').update(firma_base64 + ahora).digest('hex');
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    const { supabase } = await import('../config/supabase.js');
    const { data: doc, error } = await supabase.from('documentos_firmados').insert({
      usuario_id: firmante.id,
      codigo_modelo,
      titulo_documento: titulo,
      url_firma: firma_base64,
      hash_documento: firmaHash,
      firmado_por: firmante.id,
      estado: 'firmado',
      creado_en: ahora
    }).select().single();

    if (error) {
      if (error.code === '23505') return res.json({ success: true, ya_firmado: true, message: 'Documento ya firmado' });
      return res.status(500).json({ error: error.message });
    }

    res.json({
      success: true, doc_id: doc.id, hash: firmaHash,
      message: 'Documento firmado correctamente',
      url_verificable: `/api/junior/documento-verificable/${doc.id}`
    });
  } catch (err) {
    console.error('[Firma] Error:', err);
    res.status(500).json({ error: 'Error al firmar documento' });
  }
});

export default router;
