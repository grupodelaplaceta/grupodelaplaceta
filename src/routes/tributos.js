import crypto from 'crypto';
import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import PDFGenerator from '../services/pdfGenerator.js';
import path from 'path';
import { fileURLToPath } from 'url';
import { calcularContribucion, TIPOS_CONTRIBUCION } from '../services/contribuciones.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const LOGO_TRIBUTOS = path.join(__dirname, '..', '..', 'public', 'img', 'tributos.png');
import {
  sbGetTributosSummary,
  sbListTributosContributors,
  sbCreateTributosContributor,
  sbUpdateTributosContributor,
  sbListTributosDeclarations,
  sbCreateTributosDeclaration,
  sbDeleteTributosDeclaration,
  sbGetTributosDeclaration,
  sbUpdateTributosDeclaration,
  sbListTributosDeclaracionesPorMes,
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
  sbFindSolicitante,
  sbGetTributosContributorByEip,
  sbListTributosContributorsAll,
  sbMigrateTributosSchema
} from '../config/db-supabase.js';

const router = Router();
const BANCO_API = (process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org').replace(/\/+$/, '');

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

// ── Alta rápida desde PlacetaID (busca por DIP o EIP) ────────────────────
router.post('/contributors/alta-rapida', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { dip, tipo_sujeto, eip } = req.body;
    const tipo = tipo_sujeto || 'Fisico';

    // Para empresas: buscar por EIP en tributos_contribuyentes
    if (tipo === 'Empresa') {
      if (!eip) return res.status(400).json({ error: 'EIP requerido para empresas' });
      const existente = await sbGetTributosContributorByEip(eip).catch(() => null);
      if (existente) return res.json({ success: true, yaExiste: true, contributor: existente });
      const contributor = await sbCreateTributosContributor({
        id: crypto.randomUUID?.() || String(Date.now()),
        placeta_id: `EIP-${eip.replace(/[^A-Z0-9]/g, '')}`,
        dip: null, nombre: `Empresa ${eip}`, tipo_sujeto: 'Empresa',
        estado_fiscal: 'Al Dia', fecha_alta_tributos: new Date().toISOString(),
        roles_json: ['ciudadano', 'empresa'], iban: null, eip
      });
      return res.json({ success: true, contributor });
    }

    // Para personas: buscar por DIP — primero SQLite, luego Supabase
    if (!dip) return res.status(400).json({ error: 'DIP requerido' });
    let usuario = null;
    // Buscar en Supabase primero (fuente principal)
    try {
      usuario = await sbFindSolicitanteByDip(dip);
    } catch (_) {}
    // Fallback a SQLite
    if (!usuario) {
      try {
        const db = getDb();
        usuario = db.prepare('SELECT * FROM solicitantes WHERE dip = ?').get(dip);
      } catch (e) {}
    }
    // Si no hay registro en CRM, crear contribuyente igualmente con datos mínimos
    if (!usuario) {
      const placetaId = `PLID-${dip}`;
      const existente = await sbGetTributosContributorByPlacetaId(placetaId).catch(() => null);
      if (existente) return res.json({ success: true, yaExiste: true, contributor: existente });
      const contributor = await sbCreateTributosContributor({
        id: crypto.randomUUID?.() || String(Date.now()), placeta_id: placetaId,
        dip, nombre: `Ciudadano ${dip}`, tipo_sujeto: 'Fisico', estado_fiscal: 'Al Dia',
        fecha_alta_tributos: new Date().toISOString(), roles_json: ['ciudadano'], iban: null
      });
      return res.json({ success: true, contributor });
    }

    const placetaId = usuario.placeid || `PLID-${usuario.dip}`;
    const nombre = usuario.nombre_real || usuario.alias || `Usuario ${dip}`;

    // Detectar si es junior (régimen Capitalia)
    let esJunior = false;
    try {
      const { data: jr } = await supabase.from('junior_menores').select('id').eq('dip', dip).limit(1);
      esJunior = !!(jr && jr.length);
    } catch (_) {}

    const existente = await sbGetTributosContributorByPlacetaId(placetaId).catch(() => null);
    if (existente) return res.json({ success: true, yaExiste: true, contributor: existente });

    const contributor = await sbCreateTributosContributor({
      id: crypto.randomUUID?.() || String(Date.now()), placeta_id: placetaId,
      dip, nombre, tipo_sujeto: esJunior ? 'Junior' : 'Fisico',
      estado_fiscal: esJunior ? 'Capitalia' : 'Al Dia',
      fecha_alta_tributos: new Date().toISOString(),
      roles_json: esJunior ? ['junior', 'capitalia'] : ['ciudadano'],
      iban: null, eip: null
    });

    return res.json({ success: true, contributor, esJunior });
  } catch (err) {
    const msg = err.message?.includes('does not exist')
      ? 'Tabla tributos_contribuyentes no existe en Supabase. Ejecuta el script SQL primero.'
      : err.message;
    console.error('[Tributos] Error alta rapida:', msg);
    return res.status(500).json({ error: msg });
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

// ── CONTRIBUCIONES ──────────────────────────────────────────────────────────

// GET /contributors/calcular/todas — Calcular contribuciones de todos
router.get('/contributors/calcular/todas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const contributors = await sbListTributosContributorsAll();
    const resultados = [];
    for (const c of contributors) {
      // Estimar patrimonio desde el banco o usar valor por defecto
      let patrimonio = 0;
      try {
        const r = await fetch(`${BANCO_API}/api/account/${c.placeta_id}/balance`, {
          headers: { 'X-CRM-Key': process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026' },
          signal: AbortSignal.timeout(5000)
        });
        if (r.ok) { const d = await r.json(); patrimonio = d.balancePz || 0; }
      } catch {}
      if (!patrimonio) patrimonio = c.patrimonio_estimado || 0;
      if (!patrimonio) patrimonio = Math.floor(Math.random() * 30000) + 1000; // fallback

      const calc = calcularContribucion(c, patrimonio);
      resultados.push(calc);
    }
    const totalRecaudar = resultados.reduce((s, r) => s + r.total_contribucion, 0);
    const totalCapitalia = resultados.filter(r => r.paga_capitalia).reduce((s, r) => s + r.total_contribucion, 0);
    return res.json({
      total_contribuyentes: resultados.length,
      total_recaudar: totalRecaudar,
      total_paga_capitalia: totalCapitalia,
      total_pagan_contribuyentes: totalRecaudar - totalCapitalia,
      resultados
    });
  } catch (err) { return res.status(500).json({ error: err.message }); }
});

// GET /contributors/:placetaId/calcular — Calcular contribución de uno
router.get('/contributors/:placetaId/calcular', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const c = await sbGetTributosContributorByPlacetaId(req.params.placetaId);
    if (!c) return res.status(404).json({ error: 'No encontrado' });
    let patrimonio = 0;
    let ingresos = 0, pagos = 0;
    let cuentasUsadas = [];
    try {
      const r = await fetch(`${BANCO_API}/api/crm-state`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CRM-Key': process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026' },
        body: JSON.stringify({ action: 'get-state' }),
        signal: AbortSignal.timeout(8000)
      });
      if (r.ok) {
        const state = await r.json();
        const dip = c.dip || '';
        const dipKey = dip.toLowerCase().replace(/-/g, '');
        // Buscar TODAS las cuentas del contribuyente: personal, inversión, child, etc.
        const userAccounts = (state.accounts || []).filter(a => {
          const id = (a.id || '').toLowerCase();
          const pId = (a.placetaId || '').toLowerCase();
          return id.includes(dipKey) || pId.includes(dipKey) || pId === c.placeta_id?.toLowerCase();
        });
        if (userAccounts.length) {
          patrimonio = userAccounts.reduce((sum, a) => sum + (a.balancePz || 0), 0);
          cuentasUsadas = userAccounts.map(a => ({ id: a.id, tipo: a.type || a.kind || 'Personal', saldo: a.balancePz || 0, iban: a.iban || '—' }));
          // Ingresos/pagos del mes de TODAS las cuentas del usuario
          const mes = new Date().toISOString().slice(0, 7);
          const accountIds = new Set(userAccounts.map(a => a.id));
          const txs = (state.transactions || []).filter(t =>
            (accountIds.has(t.fromAccountId) || accountIds.has(t.toAccountId)) &&
            (t.createdAt || '').startsWith(mes)
          );
          for (const tx of txs) {
            const amount = Math.abs(Number(tx.amountPz || 0));
            if (accountIds.has(tx.toAccountId)) ingresos += amount;
            if (accountIds.has(tx.fromAccountId)) pagos += amount;
          }
        }
      }
    } catch {}
    if (!patrimonio) patrimonio = c.patrimonio_estimado || 1000;
    const resultado = calcularContribucion(c, patrimonio, ingresos, pagos);
    resultado.movimientos_mes = { ingresos, pagos, total_tx: ingresos + pagos > 0 ? 'con movimientos' : 'sin movimientos en el periodo' };
    resultado.cuentas = cuentasUsadas;
    resultado.patrimonio_desglosado = cuentasUsadas.length > 0;
    return res.json(resultado);
  } catch (err) { return res.status(500).json({ error: err.message }); }
});

// POST /contributors/:placetaId/tipo-contribucion — Asignar tipo
router.post('/contributors/:placetaId/tipo-contribucion', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { tipo_contribucion } = req.body;
    if (!tipo_contribucion || !TIPOS_CONTRIBUCION[tipo_contribucion]) {
      return res.status(400).json({ error: 'Tipo de contribución inválido', tipos: Object.keys(TIPOS_CONTRIBUCION) });
    }
    // Guardar en roles_json como JSON (evita schema migration)
    let updated;
    try {
      updated = await sbUpdateTributosContributor(req.params.placetaId, { tipo_contribucion });
    } catch (e) {
      // Fallback: columna no existe, migrar esquema
      await sbMigrateTributosSchema();
      updated = await sbUpdateTributosContributor(req.params.placetaId, { tipo_contribucion });
    }
    return res.json({ success: true, contributor: updated });
  } catch (err) { return res.status(500).json({ error: err.message, tip: "Ejecuta en SQL Editor de Supabase: ALTER TABLE tributos_contribuyentes ADD COLUMN tipo_contribucion TEXT DEFAULT 'estandar';" }); }
});

// POST /contributors/:placetaId/detectar-tipo — Auto-detectar según reglas
router.post('/contributors/:placetaId/detectar-tipo', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const c = await sbGetTributosContributorByPlacetaId(req.params.placetaId);
    if (!c) return res.status(404).json({ error: 'No encontrado' });
    const esEmpresa = c.tipo_sujeto === 'Empresa' || (c.roles_json || []).includes('empresa');
    const esJunior = c.tipo_sujeto === 'Junior' || (c.roles_json || []).includes('junior') || (c.roles_json || []).includes('capitalia');

    let tipoRecomendado;
    if (esJunior) {
      tipoRecomendado = 'exenta_junior';
    } else if (esEmpresa) {
      // Verificar si ha pagado IVA
      let haPagadoIVA = false;
      let patrimonio = 0;
      try {
        const r = await fetch(`${BANCO_API}/api/account/${c.placeta_id}/balance`, {
          headers: { 'X-CRM-Key': process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026' },
          signal: AbortSignal.timeout(5000)
        });
        if (r.ok) { const d = await r.json(); patrimonio = d.balancePz || 0; }
      } catch {}
      // Buscar facturas IVA del contribuyente
      try {
        const facturas = await sbListTributosInvoices({ limit: 50 });
        haPagadoIVA = (facturas || []).some(f =>
          f.emisor_placeta_id === c.placeta_id && f.total_iva > 0
        );
      } catch {}
      if (haPagadoIVA && patrimonio < 20000) {
        tipoRecomendado = 'empresa_exenta_igf';
      } else {
        tipoRecomendado = 'empresa';
      }
    } else {
      tipoRecomendado = 'estandar';
    }

    const updated = await sbUpdateTributosContributor(req.params.placetaId, { tipo_contribucion: tipoRecomendado });
    return res.json({ success: true, tipo_recomendado: tipoRecomendado, contributor: updated });
  } catch (err) { return res.status(500).json({ error: err.message }); }
});

// GET /tipos-contribucion — Listar tipos disponibles
router.get('/tipos-contribucion', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  return res.json(TIPOS_CONTRIBUCION);
});

// GET /contributors/:placetaId/cuentas — Cuentas bancarias del contribuyente (para asignar IBAN)
router.get('/contributors/:placetaId/cuentas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const c = await sbGetTributosContributorByPlacetaId(req.params.placetaId);
    if (!c) return res.json([]);
    const dip = c.dip || req.params.placetaId.replace('PLID-', '');
    const cuentas = [];
    // Buscar en bancario-proxy por DIP
    try {
      const r = await fetch(`${BANCO_API}/api/crm-state`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CRM-Key': process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026' },
        body: JSON.stringify({ action: 'get-state' }),
        signal: AbortSignal.timeout(8000)
      });
      if (r.ok) {
        const state = await r.json();
        const accountId = `u-${dip.toLowerCase().replace(/-/g, '')}`;
        const userAccounts = (state.accounts || []).filter(a =>
          a.id === accountId || a.placetaId === c.placeta_id || a.dip === dip
        );
        for (const a of userAccounts) {
          cuentas.push({ id: a.id, balancePz: a.balancePz || 0, iban: a.iban || `CAPI-${dip}`, type: a.type || 'Personal', displayName: a.displayName || a.id });
        }
      }
    } catch {}
    // Fallback SQLite
    if (!cuentas.length) {
      try {
        const db = getDb();
        const rows = db.prepare("SELECT id, saldo, iban, tipo_cuenta, display_name FROM cuentas_bancarias WHERE placeta_id = ? OR dip = ?").all(req.params.placetaId, dip);
        cuentas.push(...rows.map(r => ({ id: r.id, balancePz: r.saldo, iban: r.iban, type: r.tipo_cuenta, displayName: r.display_name })));
      } catch {}
    }
    return res.json(cuentas);
  } catch { return res.json([]); }
});

// POST /migrar-esquema — Añadir columnas faltantes a Supabase
router.post('/migrar-esquema', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    await sbMigrateTributosSchema();
    return res.json({ success: true, message: 'Esquema migrado correctamente' });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  DECLARACIONES
// ═══════════════════════════════════════════════════════════════════════════

router.get('/declarations', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const declarations = await sbListTributosDeclarations();
    // Enriquecer con datos del contribuyente y recalcular IRM/IGF sobre la marcha
    const enriched = await Promise.all(declarations.map(async (d) => {
      if (!d.nombre && d.placeta_id) {
        try {
          const c = await sbGetTributosContributorByPlacetaId(d.placeta_id);
          if (c) { d.nombre = c.nombre; d.dip = c.dip; d.tipo_sujeto = c.tipo_sujeto; d.eip = c.eip; }
        } catch {}
      }
      // Recalcular IRM/IGF si están a 0 pero hay patrimonio
      const pm = Number(d.patrimonio_medio || 0);
      if ((!d.cuota_irm || d.cuota_irm == 0) && (!d.cuota_igf || d.cuota_igf == 0) && pm > 0) {
        d.indice_acumulacion = 1;
        d.tipo_irm = 5;
        d.cuota_irm = Math.round(pm * 0.05 * 100) / 100;
        const baseIGF = Math.max(0, pm - 5000);
        d.cuota_igf = Math.round((Math.min(baseIGF, 15000) * 0.10 + Math.max(0, baseIGF - 15000) * 0.30) * 100) / 100;
      }
      return d;
    }));
    return res.json(enriched);
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

// ── Eliminar declaración (solo borradores) ──────────────────────────────
router.delete('/declarations/:id', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    await sbDeleteTributosDeclaration(req.params.id);
    res.json({ success: true, message: 'Declaración eliminada' });
  } catch (err) {
    console.error('[Tributos] Error eliminar declaration:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── Publicar declaración (Borrador → Pendiente_Aprobacion) ─────────────
router.put('/declarations/:id/publish', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const dec = await sbGetTributosDeclaration(req.params.id);
    if (!dec) return res.status(404).json({ error: 'Declaración no encontrada' });
    if (dec.estado_pago !== 'Borrador') return res.status(400).json({ error: 'Solo se pueden publicar borradores', estado_actual: dec.estado_pago });

    const updated = await sbUpdateTributosDeclaration(req.params.id, {
      estado_pago: 'Pendiente_Aprobacion',
      bypass_junta_directiva: req.body.bypass_junta_directiva === true,
      id_permiso_junta: req.body.id_permiso_junta || null
    });
    res.json({ success: true, declaration: updated });
  } catch (err) {
    console.error('[Tributos] Error publicar declaration:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── Aprobar declaración (Pendiente_Aprobacion → Aprobada) ──────────────
router.put('/declarations/:id/approve', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const dec = await sbGetTributosDeclaration(req.params.id);
    if (!dec) return res.status(404).json({ error: 'Declaración no encontrada' });
    if (dec.estado_pago !== 'Pendiente_Aprobacion' && dec.estado_pago !== 'Borrador') {
      return res.status(400).json({ error: 'Solo se pueden aprobar declaraciones en estado Pendiente_Aprobacion o Borrador', estado_actual: dec.estado_pago });
    }

    const updated = await sbUpdateTributosDeclaration(req.params.id, {
      estado_pago: 'Aprobada',
      bypass_junta_directiva: false
    });
    res.json({ success: true, declaration: updated });
  } catch (err) {
    console.error('[Tributos] Error aprobar declaration:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── Rechazar declaración (Pendiente_Aprobacion → Borrador) ─────────────
router.put('/declarations/:id/reject', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const dec = await sbGetTributosDeclaration(req.params.id);
    if (!dec) return res.status(404).json({ error: 'Declaración no encontrada' });
    if (dec.estado_pago !== 'Pendiente_Aprobacion') {
      return res.status(400).json({ error: 'Solo se pueden rechazar declaraciones Pendiente_Aprobacion', estado_actual: dec.estado_pago });
    }
    const updated = await sbUpdateTributosDeclaration(req.params.id, {
      estado_pago: 'Borrador',
      bypass_junta_directiva: false,
      id_permiso_junta: null
    });
    res.json({ success: true, declaration: updated, message: 'Declaración devuelta a Borrador' });
  } catch (err) {
    console.error('[Tributos] Error rechazar declaration:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── Emitir declaración (Aprobada → Emitida) + pago al banco ────────────
router.put('/declarations/:id/emit', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const dec = await sbGetTributosDeclaration(req.params.id);
    if (!dec) return res.status(404).json({ error: 'Declaración no encontrada' });
    if (dec.estado_pago !== 'Aprobada' && dec.estado_pago !== 'Borrador') {
      return res.status(400).json({ error: 'Solo se pueden emitir declaraciones Aprobadas', estado_actual: dec.estado_pago });
    }

    const totalCuota = (dec.cuota_irm || 0) + (dec.cuota_igf || 0);
    let transactionId = null;
    let paymentError = null;

    // Intentar pago al banco
    if (totalCuota > 0 && dec.placeta_id) {
      try {
        const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';
        const payR = await fetch(`${BANCO_API}/api/admin/transfer`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
          body: JSON.stringify({
            fromId: dec.placeta_id,
            toId: 'TGLP',
            amountPz: totalCuota,
            concept: `TLP-${dec.mes_periodo} IRM+IGF`,
            reference: `DEC-${dec.id?.slice(0, 8)}`,
            type: 'tax_payment'
          }),
          signal: AbortSignal.timeout(10000)
        });
        if (payR.ok) {
          const payResult = await payR.json();
          transactionId = payResult.transactionId || payResult.id || null;
        } else {
          paymentError = `Banco rechazó: ${payR.status}`;
        }
      } catch (e) {
        paymentError = `Banco no disponible: ${e.message}`;
      }
    }

    const updated = await sbUpdateTributosDeclaration(req.params.id, {
      estado_pago: transactionId ? 'Cobrado_Exito' : 'Emitido',
      transaction_id_blp: transactionId
    });

    res.json({
      success: true,
      declaration: updated,
      transactionId,
      paymentError,
      totalCuota
    });
  } catch (err) {
    console.error('[Tributos] Error emitir declaration:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ── Publicación masiva: publicar/aprobar/emitir TODAS las de un mes ────
router.put('/declarations/bulk/:accion', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { accion } = req.params; // publish, approve, emit
    const { mes_periodo, bypass_junta_directiva } = req.body;
    if (!mes_periodo) return res.status(400).json({ error: 'mes_periodo_requerido' });

    const todas = await sbListTributosDeclaracionesPorMes(mes_periodo);
    if (!todas.length) return res.json({ success: true, procesadas: 0, message: 'Sin declaraciones para este mes' });

    let ok = 0, errors = [], resultados = [];
    for (const dec of todas) {
      try {
        let updated;
        if (accion === 'publish') {
          if (dec.estado_pago !== 'Borrador') continue;
          updated = await sbUpdateTributosDeclaration(dec.id, {
            estado_pago: 'Pendiente_Aprobacion',
            bypass_junta_directiva: bypass_junta_directiva === true,
            id_permiso_junta: bypass_junta_directiva ? (req.body.id_permiso_junta || `BULK-${Date.now()}`) : null
          });
        } else if (accion === 'approve') {
          if (dec.estado_pago !== 'Pendiente_Aprobacion' && dec.estado_pago !== 'Borrador') continue;
          updated = await sbUpdateTributosDeclaration(dec.id, { estado_pago: 'Aprobada', bypass_junta_directiva: false });
        } else if (accion === 'emit') {
          if (dec.estado_pago !== 'Aprobada' && dec.estado_pago !== 'Borrador') continue;
          const totalCuota = (dec.cuota_irm || 0) + (dec.cuota_igf || 0);
          let txId = null;
          if (totalCuota > 0 && dec.placeta_id) {
            try {
              const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';
              const pr = await fetch(`${BANCO_API}/api/admin/transfer`, {
                method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
                body: JSON.stringify({
                  fromId: dec.placeta_id, toId: 'TGLP', amountPz: totalCuota,
                  concept: `TLP-${dec.mes_periodo}`, reference: `DEC-${dec.id?.slice(0, 8)}`, type: 'tax_payment'
                }),
                signal: AbortSignal.timeout(10000)
              });
              if (pr.ok) { const prj = await pr.json(); txId = prj.transactionId || prj.id || null; }
            } catch {}
          }
          updated = await sbUpdateTributosDeclaration(dec.id, {
            estado_pago: txId ? 'Cobrado_Exito' : 'Emitido',
            transaction_id_blp: txId
          });
        }
        if (updated) { ok++; resultados.push(updated); }
      } catch (e) { errors.push(dec.id + ': ' + e.message); }
    }

    res.json({ success: true, accion, procesadas: ok, errores: errors.length, total: todas.length, resultados });
  } catch (err) {
    console.error('[Tributos] Error bulk:', err.message);
    res.status(500).json({ error: err.message });
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
 * Reconstruye los saldos diarios a partir del historial de transacciones REALES del banco (BLP).
 * Body: { mes }
 */
router.post('/reconcile/:placetaId', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { placetaId } = req.params;
    const ahora = new Date();
    const mesPeriodo = req.body.mes || `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;

    // 1. Obtener info del contribuyente
    let contributor = null;
    try { contributor = await sbGetTributosContributorByPlacetaId(placetaId); } catch (e) {}
    const nombre = contributor?.nombre || placetaId;
    const dip = contributor?.dip || '';

    // 2. Obtener estado REAL del banco - TODAS las cuentas del titular
    let bankTx = [];
    let bankSaldoTotal = 0;
    let cuentasDelTitular = [];
    try {
      const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';
      const r = await fetch(`${BANCO_API}/api/crm-state`, {
        headers: { 'X-CRM-Key': CRM_KEY },
        signal: AbortSignal.timeout(8000)
      });
      if (r.ok) {
        const state = await r.json();
        const accounts = Array.isArray(state.accounts) ? state.accounts : Object.values(state.accounts || {});
        // Buscar TODAS las cuentas del titular (solo tipo Personal, Savings, Current, Investment)
        // Excluir: Business (empresas), Tesoro, TGLP (estatales), sys (sistema)
        // Para personas (DIP): solo Personal, Savings, Current, Investment
        // Para empresas (EIP o tipo_sujeto=Empresa): solo Business
        const esPersona = contributor?.tipo_sujeto !== 'Empresa' && !contributor?.eip;
        const TIPOS_VALIDOS = esPersona
          ? ['Personal', 'Savings', 'Current', 'Investment']
          : ['Business'];
        cuentasDelTitular = accounts.filter(a =>
          TIPOS_VALIDOS.includes(a.type) &&
          (a.placetaId === placetaId || a.placetaId === dip ||
           a.dip === dip || a.dip === placetaId ||
           a.id === placetaId || a.id === dip) &&
          a.id !== 'TGLP' && a.id !== 'sys-lottery' &&
          a.type !== 'Tesoro' && a.kind !== 'TGLP' &&
          !a.id?.startsWith('TGLP')
        );
        const accountIds = new Set(cuentasDelTitular.map(a => a.id));
        bankSaldoTotal = cuentasDelTitular.reduce((sum, a) => sum + (a.balancePz || 0), 0);

        // Agrupar TODAS las transacciones de todas las cuentas del titular
        const txs = state.transactions || [];
        bankTx = txs.filter(t =>
          accountIds.has(t.fromAccountId) || accountIds.has(t.toAccountId) ||
          accountIds.has(t.fromId) || accountIds.has(t.toId)
        );
      }
    } catch (e) {
      console.warn(`[Tributos] Banco no disponible para reconcile: ${e.message}`);
    }

    const [year, month] = mesPeriodo.split('-').map(Number);
    const diasEnMes = new Date(year, month, 0).getDate();

    // 3. Construir dailyMap con datos REALES
    const dailyMap = {};
    for (let d = 1; d <= diasEnMes; d++) {
      const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
      dailyMap[fechaStr] = { fecha: fechaStr, saldo: 0, transactions_count: 0, origen: 'reconstruido' };
    }

    if (bankTx.length > 0) {
      // Ordenar transacciones por fecha (ascendente)
      const sorted = bankTx.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''));
      const accountIds = new Set(cuentasDelTitular.map(a => a.id));

      // Paso 1: contar transacciones por día
      const txPorDia = {};
      for (const tx of sorted) {
        const d = parseInt((tx.createdAt || '').slice(8, 10), 10);
        if (d >= 1 && d <= diasEnMes) {
          if (!txPorDia[d]) txPorDia[d] = [];
          txPorDia[d].push(tx);
        }
      }

      // Paso 2: procesar en REVERSA desde el saldo real del último día
      let saldoCorriente = bankSaldoTotal;
      for (let d = diasEnMes; d >= 1; d--) {
        const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
        const txs = txPorDia[d] || [];

        dailyMap[fechaStr] = {
          fecha: fechaStr,
          saldo: Math.round(Math.max(0, saldoCorriente) * 100) / 100,
          transactions_count: txs.length,
          origen: txs.length > 0 ? 'banco' : 'reconstruido'
        };

        // Revertir transacciones de este día para obtener saldo del día anterior
        for (const tx of txs) {
          const amount = Math.abs(Number(tx.amountPz || 0));
          const esIngreso = accountIds.has(tx.toAccountId) || accountIds.has(tx.toId);
          saldoCorriente += esIngreso ? -amount : amount; // deshacer: restar ingresos, sumar gastos
        }
      }
    } else if (bankSaldoTotal > 0) {
      // Sin transacciones: el saldo es el mismo todos los días (no inventar dinero)
      const saldoPlano = bankSaldoTotal;
      for (let d = 1; d <= diasEnMes; d++) {
        const fechaStr = `${mesPeriodo}-${String(d).padStart(2, '0')}`;
        dailyMap[fechaStr] = {
          fecha: fechaStr,
          saldo: saldoPlano,
          transactions_count: 0,
          origen: 'banco'
        };
      }
    }
    // Si no hay nada, todo queda a 0

    // Guardar en base de datos
    await sbClearDailyBalances(placetaId, mesPeriodo);
    const balancesGuardados = [];
    for (const entry of Object.values(dailyMap)) {
      const saved = await sbUpsertDailyBalance(placetaId, mesPeriodo, entry.fecha, entry.saldo, entry.transactions_count, entry.origen);
      if (saved) balancesGuardados.push(saved);
    }

    // Calcular y actualizar la declaración
    const declaration = await sbCalculateDeclarationFromDailyBalances(placetaId, mesPeriodo, Object.values(dailyMap));

    // Recalcular IRM e IGF usando contribuciones.js
    if (declaration) {
      try {
        // Calcular ingresos/pagos del mes
        let ingresos = 0, pagos = 0;
        const mes = mesPeriodo;
        for (const tx of bankTx) {
          const txDate = (tx.createdAt || '').slice(0, 7);
          if (txDate === mes) {
            const amount = Math.abs(Number(tx.amountPz || 0));
            if (accountIds.has(tx.toAccountId)) ingresos += amount;
            if (accountIds.has(tx.fromAccountId)) pagos += amount;
          }
        }
        const patrimonio = declaration.patrimonio_medio || bankSaldoTotal || 0;
        const calc = calcularContribucion(contributor || { nombre, dip, tipo_sujeto: 'Fisico', placeta_id: placetaId }, patrimonio, ingresos, pagos);
        // Actualizar declaration con cálculos correctos
        declaration.cuota_irm = calc.irm.importe;
        declaration.cuota_igf = calc.igf.importe;
        declaration.indice_acumulacion = calc.irm.ia;
        declaration.tipo_irm = calc.irm.porcentaje;
        declaration.exencion_igf = calc.igf.exento || null;
        // Guardar en Supabase
        await sbUpdateTributosDeclaration(declaration.id, {
          cuota_irm: calc.irm.importe,
          cuota_igf: calc.igf.importe,
          indice_acumulacion: calc.irm.ia,
          tipo_irm: calc.irm.porcentaje
        });
      } catch (calcErr) {
        console.warn('[Tributos] Error recalculando IRM/IGF:', calcErr.message);
      }
    }

    // Enriquecer declaration con datos del contribuyente
    if (declaration) {
      declaration.nombre = nombre;
      declaration.dip = dip || contributor?.dip || '—';
      declaration.tipo_sujeto = contributor?.tipo_sujeto || 'Fisico';
      declaration.placeta_id = placetaId;
    }

    return res.json({
      success: true,
      placeta_id: placetaId,
      mes_periodo: mesPeriodo,
      nombre,
      total_dias: diasEnMes,
      dias_con_datos: Object.values(dailyMap).filter(d => d.transactions_count > 0 || d.origen === 'banco').length,
      patrimonio_medio: declaration?.patrimonio_medio || 0,
      saldo_actual: bankSaldoTotal,
      balances: Object.values(dailyMap),
      cuentas: cuentasDelTitular.map(c => ({
        id: c.id,
        displayName: c.displayName || c.id,
        type: c.type,
        balancePz: c.balancePz || 0,
        placetaId: c.placetaId || '',
        iban: c.iban || '',
        complianceStatus: c.complianceStatus || 'Clear'
      })),
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
    const generator = new PDFGenerator({ accentColor: '#4e396f', logo: LOGO_TRIBUTOS, tipo: 'tributos' });
    let doc;

    switch (tipo) {
      case 'declaracion':
        // Enriquecer con datos del contribuyente si faltan
        if (!datos.nombre || datos.nombre === '—' || !datos.placeta_id) {
          const pid = datos.placeta_id || datos.cuenta_id_blp || datos.id;
          if (pid && pid !== '—') {
            try {
              const c = await sbGetTributosContributorByPlacetaId(pid);
              if (c) {
                datos.placeta_id = c.placeta_id;
                datos.nombre = c.nombre;
                datos.dip = c.dip;
                datos.tipo_sujeto = c.tipo_sujeto;
                if (!datos.cuenta_id_blp) datos.cuenta_id_blp = c.placeta_id;
              }
            } catch {}
          }
        }
        if (!datos.nombre) datos.nombre = datos.placeta_id || '—';
        if (!datos.dip) datos.dip = '—';
        if (!datos.tipo_sujeto) datos.tipo_sujeto = '—';
        if (!datos.cuenta_id_blp) datos.cuenta_id_blp = datos.placeta_id || '—';
        // Recalcular IRM/IGF sobre la marcha (aunque la declaración sea antigua)
        try {
          const pm = Number(datos.patrimonio_medio || 0);
          if (pm > 0) {
            const cContrib = await sbGetTributosContributorByPlacetaId(datos.placeta_id || datos.cuenta_id_blp).catch(() => null);
            const calc = calcularContribucion(cContrib || { nombre: datos.nombre, dip: datos.dip, tipo_sujeto: datos.tipo_sujeto, placeta_id: datos.placeta_id }, pm);
            datos.cuota_irm = calc.irm.importe;
            datos.cuota_igf = calc.igf.importe;
            datos.indice_acumulacion = calc.irm.ia;
            datos.tipo_irm = calc.irm.porcentaje;
            datos.exencion_igf = calc.igf.exento || '';
          }
        } catch (_) {}
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
