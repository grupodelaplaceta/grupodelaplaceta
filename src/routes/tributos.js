import crypto from 'crypto';
import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import PDFGenerator from '../services/pdfGenerator.js';
import {
  sbGetTributosSummary,
  sbListTributosContributors,
  sbCreateTributosContributor,
  sbUpdateTributosContributor,
  sbListTributosDeclarations,
  sbCreateTributosDeclaration,
  sbListTributosInvoices,
  sbCreateTributosInvoice,
  sbGetTributosInhibition,
  sbSetTributosInhibition,
  sbListTributosRectifications,
  sbCreateTributosRectification,
  sbListTributosAuditLogs,
  sbGetTributosContributorByPlacetaId
} from '../config/db-supabase.js';

const router = Router();

// ═══════════════════════════════════════════════════════════════════════════
//  SUMMARY
// ═══════════════════════════════════════════════════════════════════════════

router.get('/summary', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const summary = await sbGetTributosSummary();
    return res.json(summary);
  } catch (err) {
    console.error('[Tributos] Error summary:', err.message);
    return res.status(500).json({ error: 'error_resumen_tributos' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  CONTRIBUYENTES
// ═══════════════════════════════════════════════════════════════════════════

router.get('/contributors', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const contributors = await sbListTributosContributors();
    return res.json(contributors);
  } catch (err) {
    console.error('[Tributos] Error contributors:', err.message);
    return res.status(500).json({ error: 'error_listar_contribuyentes' });
  }
});

router.get('/contributors/:placetaId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const contributor = await sbGetTributosContributorByPlacetaId(req.params.placetaId);
    if (!contributor) return res.status(404).json({ error: 'contribuyente_no_encontrado' });
    return res.json(contributor);
  } catch (err) {
    console.error('[Tributos] Error get contributor:', err.message);
    return res.status(500).json({ error: 'error_obtener_contribuyente' });
  }
});

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
      estado_fiscal: 'Al Dia',
      fecha_alta_tributos: new Date().toISOString(),
      roles_json: Array.isArray(roles_json) ? roles_json : String(roles_json || 'ciudadano').split(',').map((item) => item.trim()).filter(Boolean),
      iban: iban || null
    });

    return res.json({ success: true, contributor });
  } catch (err) {
    console.error('[Tributos] Error crear contributor:', err.message);
    return res.status(500).json({ error: 'error_crear_contribuyente' });
  }
});

router.put('/contributors/:placetaId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const updated = await sbUpdateTributosContributor(req.params.placetaId, req.body);
    return res.json({ success: true, contributor: updated });
  } catch (err) {
    console.error('[Tributos] Error update contributor:', err.message);
    return res.status(500).json({ error: 'error_actualizar_contribuyente' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  DECLARACIONES
// ═══════════════════════════════════════════════════════════════════════════

router.get('/declarations', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const declarations = await sbListTributosDeclarations();
    return res.json(declarations);
  } catch (err) {
    console.error('[Tributos] Error declarations:', err.message);
    return res.status(500).json({ error: 'error_listar_declaraciones' });
  }
});

router.post('/declarations', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { placeta_id, mes_periodo, patrimonio_medio, indice_acumulacion, cuota_irm, cuota_igf, cuenta_id_blp } = req.body;
    if (!placeta_id || !mes_periodo) {
      return res.status(400).json({ error: 'placeta_id_mes_periodo_requeridos' });
    }

    const declaration = await sbCreateTributosDeclaration({
      id: crypto.randomUUID?.() || String(Date.now()),
      mes_periodo,
      cuenta_id_blp: cuenta_id_blp || placeta_id,
      patrimonio_medio: Number(patrimonio_medio || 0),
      indice_acumulacion: Number(indice_acumulacion || 0),
      cuota_irm: Number(cuota_irm || 0),
      cuota_igf: Number(cuota_igf || 0),
      estado_pago: 'Borrador',
      dias_activos_mes: 30
    });

    return res.json({ success: true, declaration });
  } catch (err) {
    console.error('[Tributos] Error crear declaration:', err.message);
    return res.status(500).json({ error: 'error_crear_declaracion' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  FACTURAS
// ═══════════════════════════════════════════════════════════════════════════

router.get('/invoices', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const invoices = await sbListTributosInvoices({ limit: parseInt(req.query.limit || '20', 10) });
    return res.json(invoices);
  } catch (err) {
    console.error('[Tributos] Error invoices:', err.message);
    return res.status(500).json({ error: 'error_listar_facturas' });
  }
});

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
      csv_verificacion: crypto.randomUUID?.() ? `CSV-${crypto.randomUUID().slice(0, 8).toUpperCase()}-${Date.now().toString(36).toUpperCase()}` : `CSV-${Date.now().toString(36).toUpperCase()}`,
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

// ═══════════════════════════════════════════════════════════════════════════
//  INHIBICIÓN GLOBAL
// ═══════════════════════════════════════════════════════════════════════════

router.get('/inhibition', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const inhibition = await sbGetTributosInhibition();
    return res.json(inhibition);
  } catch (err) {
    console.error('[Tributos] Error inhibition:', err.message);
    return res.status(500).json({ error: 'error_obtener_inhibicion' });
  }
});

router.put('/inhibition', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { estado_inhibicion_global } = req.body;
    const result = await sbSetTributosInhibition(
      estado_inhibicion_global !== undefined ? estado_inhibicion_global : true,
      req.session.usuario?.placeta_id || req.session.usuario?.id || 'admin'
    );
    return res.json({ success: true, inhibition: result });
  } catch (err) {
    console.error('[Tributos] Error set inhibition:', err.message);
    return res.status(500).json({ error: 'error_actualizar_inhibicion' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  RECTIFICACIONES / REGULARIZACIONES
// ═══════════════════════════════════════════════════════════════════════════

router.get('/rectifications', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const rectifications = await sbListTributosRectifications();
    return res.json(rectifications);
  } catch (err) {
    console.error('[Tributos] Error rectifications:', err.message);
    return res.status(500).json({ error: 'error_listar_rectificaciones' });
  }
});

router.post('/rectifications', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { declaracion_original_id, cuota_provisional_cobrada, cuota_real_calculada } = req.body;
    if (!declaracion_original_id) {
      return res.status(400).json({ error: 'declaracion_original_id_requerido' });
    }

    const delta = Number((cuota_real_calculada || 0) - (cuota_provisional_cobrada || 0));
    const rectification = await sbCreateTributosRectification({
      id: crypto.randomUUID?.() || String(Date.now()),
      declaracion_original_id,
      cuota_provisional_cobrada: Number(cuota_provisional_cobrada || 0),
      cuota_real_calculada: Number(cuota_real_calculada || 0),
      diferencia_delta: Number(delta.toFixed(2)),
      estado_ajuste: delta <= 0 ? 'Pendiente_Procesamiento' : 'Diferencia_Cobrada',
      signature_sha256: crypto.createHash?.('sha256')?.update(JSON.stringify(req.body) + Date.now()).digest('hex') || null
    });

    return res.json({ success: true, rectification });
  } catch (err) {
    console.error('[Tributos] Error crear rectification:', err.message);
    return res.status(500).json({ error: 'error_crear_rectificacion' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  AUDIT LOGS
// ═══════════════════════════════════════════════════════════════════════════

router.get('/audit-logs', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const logs = await sbListTributosAuditLogs();
    return res.json(logs);
  } catch (err) {
    console.error('[Tributos] Error audit logs:', err.message);
    return res.status(500).json({ error: 'error_listar_audit_logs' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  PDF — GENERACIÓN DE DOCUMENTOS TRIBUTARIOS
// ═══════════════════════════════════════════════════════════════════════════

router.post('/pdf/:tipo', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { tipo } = req.params;
    const datos = req.body;
    const generator = new PDFGenerator();
    let doc;

    switch (tipo) {
      case 'declaracion':
        doc = generator.generarDeclaracionTributaria(datos);
        break;
      case 'factura':
        doc = generator.generarFacturaTributaria(datos);
        break;
      case 'rectificacion':
        doc = generator.generarRectificacionTributaria(datos);
        break;
      default:
        return res.status(400).json({ error: `Tipo de documento no soportado: ${tipo}. Tipos: declaracion, factura, rectificacion` });
    }

    const filename = `TLP-${tipo.toUpperCase()}-${datos.id || datos.numero_factura || datos.ID_DECLARACION || Date.now()}.pdf`;
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);

    doc.pipe(res);
    doc.end();

    // Log de auditoría
    try {
      const db = (await import('../config/db.js')).getDb();
      const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
      db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
        .run(req.session.usuario.id, 'generar_pdf', `PDF Tributos generado: ${tipo} - ${filename}`, ip);
    } catch (_) { /* fallback silencioso */ }

  } catch (err) {
    console.error('[Tributos] Error generando PDF:', err);
    res.status(500).json({ error: 'Error generando el documento PDF' });
  }
});

export default router;
