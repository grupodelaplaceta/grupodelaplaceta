import crypto from 'crypto';
import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import {
  sbGetTributosSummary,
  sbListTributosContributors,
  sbCreateTributosContributor,
  sbListTributosDeclarations,
  sbListTributosInvoices,
  sbCreateTributosInvoice
} from '../config/db-supabase.js';

const router = Router();

// GET /api/admin/tributos/summary
router.get('/summary', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const summary = await sbGetTributosSummary();
    return res.json(summary);
  } catch (err) {
    console.error('[Tributos] Error summary:', err.message);
    return res.status(500).json({ error: 'error_resumen_tributos' });
  }
});

// GET /api/admin/tributos/contributors
router.get('/contributors', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const contributors = await sbListTributosContributors();
    return res.json(contributors);
  } catch (err) {
    console.error('[Tributos] Error contributors:', err.message);
    return res.status(500).json({ error: 'error_listar_contribuyentes' });
  }
});

// POST /api/admin/tributos/contributors
router.post('/contributors', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { placeta_id, dip, nombre, tipo_sujeto, roles_json, iban } = req.body;
    if (!placeta_id || !dip || !nombre) {
      return res.status(400).json({ error: 'placeta_id_dip_nombre_requeridos' });
    }

    const contributor = await sbCreateTributosContributor({
      id: crypto.randomUUID?.() || String(Date.now()),
      placeta_id,
      dip,
      nombre,
      tipo_sujeto: tipo_sujeto || 'Fisico',
      roles_json: Array.isArray(roles_json) ? roles_json : String(roles_json || 'ciudadano').split(',').map((item) => item.trim()).filter(Boolean),
      iban: iban || null
    });

    return res.json({ success: true, contributor });
  } catch (err) {
    console.error('[Tributos] Error crear contributor:', err.message);
    return res.status(500).json({ error: 'error_crear_contribuyente' });
  }
});

// GET /api/admin/tributos/declarations
router.get('/declarations', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const declarations = await sbListTributosDeclarations();
    return res.json(declarations);
  } catch (err) {
    console.error('[Tributos] Error declarations:', err.message);
    return res.status(500).json({ error: 'error_listar_declaraciones' });
  }
});

// GET /api/admin/tributos/invoices
router.get('/invoices', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const invoices = await sbListTributosInvoices({ limit: parseInt(req.query.limit || '20', 10) });
    return res.json(invoices);
  } catch (err) {
    console.error('[Tributos] Error invoices:', err.message);
    return res.status(500).json({ error: 'error_listar_facturas' });
  }
});

// POST /api/admin/tributos/invoices
router.post('/invoices', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const {
      numero_factura,
      emisor_placeta_id,
      receptor_placeta_id,
      concepto_producto,
      cantidad,
      precio_unitario,
      iva_porcentaje
    } = req.body;

    if (!numero_factura || !emisor_placeta_id || !receptor_placeta_id || !concepto_producto) {
      return res.status(400).json({ error: 'campos_factura_obligatorios' });
    }

    const invoice = await sbCreateTributosInvoice({
      id: crypto.randomUUID?.() || String(Date.now()),
      numero_factura,
      emisor_placeta_id,
      receptor_placeta_id,
      lineas: [
        {
          concepto_producto,
          cantidad: Number(cantidad) || 1,
          precio_unitario: Number(precio_unitario) || 0,
          iva_porcentaje: Number(iva_porcentaje) || 12
        }
      ]
    });

    return res.json({ success: true, invoice });
  } catch (err) {
    console.error('[Tributos] Error crear invoice:', err.message);
    return res.status(500).json({ error: 'error_crear_factura' });
  }
});

export default router;
