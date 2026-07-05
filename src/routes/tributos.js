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
  sbGetTributosContributorByPlacetaId,
  sbGetDailyBalances,
  sbUpsertDailyBalance,
  sbClearDailyBalances,
  sbCalculateDeclarationFromDailyBalances,
  sbFindSolicitante
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
    return res.json({ contribuyentes: 0, declaraciones: 0, facturas: 0, importe_total: 0, iva_total: 0 });
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
    return res.json([]);
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
    const { placeta_id, dip, nombre, tipo_sujeto, roles_json, iban, eip } = req.body;
    if (!placeta_id || !dip || !nombre) {
      return res.status(400).json({ error: 'placeta_id_dip_nombre_requeridos' });
    }

    const tipo = tipo_sujeto || 'Fisico';
    const contributor = await sbCreateTributosContributor({
      id: crypto.randomUUID?.() || String(Date.now()),
      placeta_id,
      dip,
      nombre,
      tipo_sujeto: tipo,
      estado_fiscal: 'Al Dia',
      fecha_alta_tributos: new Date().toISOString(),
      roles_json: Array.isArray(roles_json) ? roles_json : String(roles_json || 'ciudadano').split(',').map((item) => item.trim()).filter(Boolean),
      iban: iban || null,
      eip: tipo === 'Empresa' ? (eip || null) : null
    });

    return res.json({ success: true, contributor });
  } catch (err) {
    console.error('[Tributos] Error crear contributor:', err.message);
    return res.status(500).json({ error: 'error_crear_contribuyente' });
  }
});

// ── Alta rápida desde PlacetaID (busca por DIP y crea contribuyente) ──────
router.post('/contributors/alta-rapida', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { dip, tipo_sujeto, eip } = req.body;
    if (!dip) return res.status(400).json({ error: 'DIP requerido' });

    // Buscar en Supabase
    const usuario = await sbFindSolicitante(dip);
    if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado en PlacetaID' });

    const placetaId = usuario.placeid || `PLID-${usuario.dip}`;
    const nombre = usuario.nombre_real || usuario.alias || `Usuario ${dip}`;
    const tipo = tipo_sujeto || 'Fisico';

    // Verificar si ya existe
    try {
      const existente = await sbGetTributosContributorByPlacetaId(placetaId);
      if (existente) return res.json({ success: true, yaExiste: true, contributor: existente });
    } catch {}

    const contributor = await sbCreateTributosContributor({
      id: crypto.randomUUID?.() || String(Date.now()),
      placeta_id: placetaId,
      dip,
      nombre,
      tipo_sujeto: tipo,
      estado_fiscal: 'Al Dia',
      fecha_alta_tributos: new Date().toISOString(),
      roles_json: ['ciudadano'],
      iban: null,
      eip: tipo === 'Empresa' ? (eip || null) : null
    });

    return res.json({ success: true, contributor });
  } catch (err) {
    console.error('[Tributos] Error alta rapida:', err.message);
    return res.status(500).json({ error: err.message });
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
    return res.json([]);
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
    return res.json([]);
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
    const now = new Date();
    return res.json({ mes_periodo: now.getFullYear()+'-'+String(now.getMonth()+1).padStart(2,'0'), estado_inhibicion_global: true });
  }
});

router.put('/inhibition', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { estado_inhibicion_global } = req.body;
    const estado = estado_inhibicion_global !== undefined ? estado_inhibicion_global : true;
    const result = await sbSetTributosInhibition(
      estado,
      req.session.usuario?.placeta_id || req.session.usuario?.id || 'admin'
    );
    return res.json({ success: true, inhibition: result });
  } catch (err) {
    console.error('[Tributos] Error set inhibition:', err.message);
    return res.json({ success: true, inhibition: { estado_inhibicion_global: req.body?.estado_inhibicion_global !== false } });
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
    return res.json([]);
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
    return res.json([]);
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  DAILY DECLARATIONS — Motor de Reconciliación Diaria
// ═══════════════════════════════════════════════════════════════════════════

/**
 * GET /api/admin/tributos/daily-balances/:placetaId
 * Obtiene los saldos diarios de un contribuyente para un mes.
 * Query: mes=2026-07 (por defecto mes actual)
 */
router.get('/daily-balances/:placetaId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { placetaId } = req.params;
    const ahora = new Date();
    const mesPeriodo = req.query.mes || `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
    const balances = await sbGetDailyBalances(placetaId, mesPeriodo);
    return res.json({ placeta_id: placetaId, mes_periodo: mesPeriodo, balances });
  } catch (err) {
    console.error('[Tributos] Error daily balances:', err.message);
    return res.json({ placeta_id: req.params.placetaId, mes_periodo: req.query.mes || '', balances: [] });
  }
});

/**
 * POST /api/admin/tributos/reconcile/:placetaId
 * Reconstruye los saldos diarios a partir del historial de transacciones del banco (BLP).
 * Body: { mes, transactions: [...] }
 * Si no se envían transacciones, genera datos simulados basados en el contribuyente.
 */
router.post('/reconcile/:placetaId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { placetaId } = req.params;
    const ahora = new Date();
    const mesPeriodo = req.body.mes || `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
    const transactions = Array.isArray(req.body.transactions) ? req.body.transactions : [];

    // Obtener info del contribuyente
    let contributor = null;
    try { contributor = await sbGetTributosContributorByPlacetaId(placetaId); } catch (e) {}
    const nombre = contributor?.nombre || placetaId;
    const saldoActual = Number(req.body.saldo_actual || contributor?.saldo || 0);

    // Calcular días del mes
    const [year, month] = mesPeriodo.split('-').map(Number);
    const diasEnMes = new Date(year, month, 0).getDate();

    // Construir mapa de saldos diarios desde transacciones
    const dailyMap = {};
    for (let d = 1; d <= diasEnMes; d++) {
      const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
      dailyMap[fechaStr] = { fecha: fechaStr, saldo: 0, transactions_count: 0, origen: 'reconstruido' };
    }

    // Procesar transacciones: asignar cada una a su día y acumular impacto
    let saldoCorriente = 0;
    const sortedTx = [...transactions].sort((a, b) => (a.createdAt || a.fecha || '').localeCompare(b.createdAt || b.fecha || ''));

    // Primera pasada: construir saldos secuenciales
    // Si no hay transacciones, crear datos simulados con el saldo actual
    if (sortedTx.length === 0) {
      // Sin transacciones: distribución uniforme del saldo actual
      for (let d = 1; d <= diasEnMes; d++) {
        const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
        const saldoDia = Math.max(0, saldoActual * (0.7 + Math.random() * 0.6));
        dailyMap[fechaStr] = {
          fecha: fechaStr,
          saldo: Math.round(saldoDia * 100) / 100,
          transactions_count: Math.floor(Math.random() * 3),
          origen: 'reconstruido'
        };
      }
      // Marcar el último día con el saldo actual exacto
      const ultimoDia = `${mesPeriodo}-${String(diasEnMes).padStart(2, '0')}`;
      if (dailyMap[ultimoDia]) {
        dailyMap[ultimoDia].saldo = saldoActual;
        dailyMap[ultimoDia].origen = 'banco';
      }
    } else {
      // Con transacciones reales: calcular saldo día a día
      let txIndex = 0;
      for (let d = 1; d <= diasEnMes; d++) {
        const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
        let txDelDia = 0;

        // Acumular transacciones de este día
        while (txIndex < sortedTx.length) {
          const txFecha = (sortedTx[txIndex].createdAt || sortedTx[txIndex].fecha || '').slice(0, 10);
          if (txFecha === fechaStr) {
            const tx = sortedTx[txIndex];
            const impacto = Math.abs(Number(tx.amountPz || tx.cantidad || 0));
            saldoCorriente += impacto * (tx.toAccountId === placetaId || tx.cuenta_destino === placetaId ? 1 : -1);
            txDelDia++;
            txIndex++;
          } else {
            break;
          }
        }

        dailyMap[fechaStr] = {
          fecha: fechaStr,
          saldo: Math.round(Math.max(0, saldoCorriente || saldoActual * (d / diasEnMes)) * 100) / 100,
          transactions_count: txDelDia,
          origen: txDelDia > 0 ? 'banco' : 'reconstruido'
        };
      }
      // Asegurar que el último día refleje el saldo actual
      const ultimoDia = `${mesPeriodo}-${String(diasEnMes).padStart(2, '0')}`;
      if (dailyMap[ultimoDia]) {
        dailyMap[ultimoDia].saldo = saldoActual;
        dailyMap[ultimoDia].origen = 'banco';
      }
    }

    // Guardar en base de datos
    await sbClearDailyBalances(placetaId, mesPeriodo);
    const balancesGuardados = [];
    for (const entry of Object.values(dailyMap)) {
      const saved = await sbUpsertDailyBalance(placetaId, mesPeriodo, entry.fecha, entry.saldo, entry.transactions_count);
      if (saved) balancesGuardados.push(saved);
    }

    // Calcular y actualizar la declaración
    const declaration = await sbCalculateDeclarationFromDailyBalances(placetaId, mesPeriodo, Object.values(dailyMap));

    return res.json({
      success: true,
      placeta_id: placetaId,
      mes_periodo: mesPeriodo,
      nombre,
      total_dias: diasEnMes,
      dias_con_datos: Object.values(dailyMap).filter(d => d.transactions_count > 0 || d.origen === 'banco').length,
      patrimonio_medio: declaration?.patrimonio_medio || 0,
      saldo_actual: saldoActual,
      balances: Object.values(dailyMap),
      declaration
    });
  } catch (err) {
    console.error('[Tributos] Error reconcile:', err.message);
    return res.status(500).json({ error: 'error_reconciliar', message: err.message });
  }
});

/**
 * POST /api/admin/tributos/recalculate/:declarationId
 * Recalcula una declaración existente a partir de sus saldos diarios almacenados.
 */
router.post('/recalculate/:declarationId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { declarationId } = req.params;
    const { placeta_id, mes_periodo } = req.body;
    if (!placeta_id || !mes_periodo) {
      return res.status(400).json({ error: 'placeta_id_y_mes_periodo_requeridos' });
    }

    const balances = await sbGetDailyBalances(placeta_id, mes_periodo);
    if (!balances || balances.length === 0) {
      return res.status(400).json({ error: 'sin_saldos_diarios', message: 'No hay saldos diarios. Ejecuta primero la reconciliación.' });
    }

    const declaration = await sbCalculateDeclarationFromDailyBalances(placeta_id, mes_periodo, balances);
    if (!declaration) {
      return res.status(500).json({ error: 'error_calcular_declaracion' });
    }

    return res.json({ success: true, declaration });
  } catch (err) {
    console.error('[Tributos] Error recalculate:', err.message);
    return res.status(500).json({ error: 'error_recalcular' });
  }
});

/**
 * GET /api/admin/tributos/reconciliation-status
 * Devuelve el estado de reconciliación para todos los contribuyentes activos.
 */
router.get('/reconciliation-status', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const contributors = await sbListTributosContributors();
    const ahora = new Date();
    const mesPeriodo = req.query.mes || `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;

    const statusList = await Promise.all(contributors.map(async (c) => {
      const balances = await sbGetDailyBalances(c.placeta_id, mesPeriodo);
      const diasConDatos = balances.filter(b => b.transactions_count > 0 || b.origen === 'banco').length;
      const [year, month] = mesPeriodo.split('-').map(Number);
      const totalDias = new Date(year, month, 0).getDate();
      return {
        placeta_id: c.placeta_id,
        nombre: c.nombre,
        tipo_sujeto: c.tipo_sujeto,
        estado_fiscal: c.estado_fiscal,
        total_dias_mes: totalDias,
        dias_conciliados: diasConDatos,
        dias_pendientes: Math.max(0, totalDias - diasConDatos),
        patrimonio_medio: balances.length > 0
          ? Number((balances.reduce((s, b) => s + Number(b.saldo || 0), 0) / balances.length).toFixed(2))
          : 0
      };
    }));

    return res.json({ mes_periodo: mesPeriodo, contributors: statusList });
  } catch (err) {
    console.error('[Tributos] Error reconciliation status:', err.message);
    return res.json({ mes_periodo: '', contributors: [] });
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
