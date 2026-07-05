import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';

const router = Router();
let raw = process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org';
if (!/^https?:\/\//i.test(raw)) raw = 'https://' + raw;
const BANCO_API = raw.replace(/\/+$/, '');

// ── Cache simple ────────────────────────────────────────────────────────────
let cache = { data: null, expiresAt: 0 };
const CACHE_TTL = 30_000; // 30 segundos

const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';

async function fetchBancoState(req) {
  if (cache.data && Date.now() < cache.expiresAt) return cache.data;
  const url = `${BANCO_API}/api/crm-state`;
  const headers = { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY };
  let res;
  try { res = await fetch(url, { headers }); }
  catch (e) { throw new Error(`No se puede conectar con ${BANCO_API} — ${e.message}`); }
  if (!res.ok) {
    let msg = `Banco API (${url}) respondió ${res.status}`;
    try { const err = await res.json(); if (err.error) msg += `: ${err.error}`; } catch {}
    throw new Error(msg);
  }
  const data = await res.json();
  cache = { data, expiresAt: Date.now() + CACHE_TTL };
  return data;
}

// ── Diagnóstico de conexión ────────────────────────────────────────────────
router.get('/status', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const url = `${BANCO_API}/api/crm-state`;
    const test = await fetch(url, { headers: { 'X-CRM-Key': CRM_KEY } });
    const info = { bancoApi: BANCO_API, crmKeySet: !!CRM_KEY, status: test.status, ok: test.ok };
    if (!test.ok) {
      let detail = '';
      try { const e = await test.json(); detail = e.error || ''; } catch {}
      info.error = detail;
    }
    res.json(info);
  } catch (e) {
    res.json({ bancoApi: BANCO_API, crmKeySet: !!CRM_KEY, status: 0, ok: false, error: e.message });
  }
});

// ── Resumen bancario ────────────────────────────────────────────────────────
router.get('/resumen', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    res.json({
      totalUsuarios: (state.users || []).length,
      totalCuentas: Object.keys(state.accounts || {}).length,
      totalTransacciones: (state.transactions || []).length,
      saldoTotal: Object.values(state.accounts || {}).reduce((sum, a) => sum + (a.balancePz || 0), 0),
      tesoro: state.accounts?.TGLP?.balancePz || 0,
      updatedAt: state.updatedAt
    });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Listar cuentas ──────────────────────────────────────────────────────────
router.get('/cuentas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const cuentas = Object.values(state.accounts || {});
    const { tipo, search } = req.query;
    let filtered = cuentas;
    if (tipo) filtered = filtered.filter(c => c.type === tipo);
    if (search) filtered = filtered.filter(c =>
      c.displayName?.toLowerCase().includes(search.toLowerCase()) ||
      c.iban?.toLowerCase().includes(search.toLowerCase()) ||
      c.placetaId?.toLowerCase().includes(search.toLowerCase())
    );
    res.json(filtered.sort((a, b) => (b.balancePz || 0) - (a.balancePz || 0)));
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Listar usuarios ─────────────────────────────────────────────────────────
router.get('/usuarios', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const { search } = req.query;
    let usuarios = state.users || [];
    if (search) usuarios = usuarios.filter(u =>
      u.displayName?.toLowerCase().includes(search.toLowerCase()) ||
      u.dip?.toLowerCase().includes(search.toLowerCase()) ||
      u.placetaId?.toLowerCase().includes(search.toLowerCase())
    );
    res.json(usuarios);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Últimos movimientos ─────────────────────────────────────────────────────
router.get('/movimientos', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const limit = Math.min(parseInt(req.query.limit) || 50, 200);
    const txs = (state.transactions || [])
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, limit);
    res.json(txs);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Buscar cuenta por DIP/PlacetaId ─────────────────────────────────────────
router.get('/buscar', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const q = (req.query.q || '').toUpperCase();
    if (!q) return res.json({ usuarios: [], cuentas: [] });

    const usuarios = (state.users || []).filter(u =>
      u.dip?.toUpperCase().includes(q) || u.placetaId?.toUpperCase().includes(q) || u.displayName?.toUpperCase().includes(q)
    );
    const placetaIds = new Set(usuarios.map(u => u.placetaId));
    const cuentas = Object.values(state.accounts || {}).filter(c =>
      c.placetaId && placetaIds.has(c.placetaId)
    );
    res.json({ usuarios, cuentas });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Resumen de cuentas por tipo ─────────────────────────────────────────────
router.get('/resumen-cuentas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const cuentas = Object.values(state.accounts || {});
    const porTipo = {};
    for (const c of cuentas) {
      const tipo = c.type || 'Unknown';
      if (!porTipo[tipo]) porTipo[tipo] = { tipo, count: 0, saldoTotal: 0, saldoMaximo: 0, sumaPromedio: 0 };
      porTipo[tipo].count++;
      porTipo[tipo].saldoTotal += c.balancePz || 0;
      if ((c.balancePz || 0) > porTipo[tipo].saldoMaximo) porTipo[tipo].saldoMaximo = c.balancePz || 0;
    }
    for (const t of Object.values(porTipo)) {
      t.promedio = t.count > 0 ? Math.round(t.saldoTotal / t.count) : 0;
    }
    res.json(Object.values(porTipo));
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Tarjetas emitidas ───────────────────────────────────────────────────────
router.get('/tarjetas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const cards = state.digitalCards || [];
    res.json({ total: cards.length, activas: cards.filter(c => c.status !== 'Frozen' && !c.frozen).length });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Emitir Placetas (solo administradores) ──────────────────────────────────
router.post('/emitir', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { cantidad, dip, motivo } = req.body;
    if (!cantidad || !dip) return res.status(400).json({ error: 'Faltan cantidad o DIP' });

    const r = await fetch(`${BANCO_API}/api/crm-state`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
      body: JSON.stringify({ action: 'emitir', cantidad, dip, motivo: motivo || 'Emisión administrativa' })
    });
    const data = await r.json();
    if (!r.ok) return res.status(r.status).json(data);
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Quemar Placetas (solo administradores) ──────────────────────────────────
router.post('/quemar', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { cantidad, cuentaId, motivo } = req.body;
    if (!cantidad || !cuentaId) return res.status(400).json({ error: 'Faltan cantidad o cuentaId' });

    const r = await fetch(`${BANCO_API}/api/crm-state`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
      body: JSON.stringify({ action: 'quemar', cantidad, cuentaId, motivo: motivo || 'Quema administrativa' })
    });
    const data = await r.json();
    if (!r.ok) return res.status(r.status).json(data);
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Cumplimiento / Sanciones ────────────────────────────────────────────────
router.get('/cumplimiento', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    res.json(state.complianceFlags || []);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Subsidios ───────────────────────────────────────────────────────────────
router.get('/subsidios', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    res.json(state.subsidyRequests || []);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Alertas combinadas ──────────────────────────────────────────────────────
router.get('/alertas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const cuentas = state.accounts || [];
    const cards = state.digitalCards || [];
    const txs = state.transactions || [];

    const alertas = [];
    // Cuentas en descubierto
    for (const c of cuentas) {
      if ((c.balancePz || 0) < 0) alertas.push({ tipo: 'descubierto', severity: 'alta', mensaje: `Cuenta ${c.displayName || c.id} con saldo negativo`, entidad: c });
    }
    // Tarjetas congeladas
    for (const c of cards) {
      if (c.status === 'Frozen' || c.frozen) alertas.push({ tipo: 'tarjeta_congelada', severity: 'media', mensaje: `Tarjeta ${c.id?.slice(0,12)} congelada`, entidad: c });
    }
    // Transacciones grandes (> 10000 Pz)
    for (const t of (txs || []).slice(-100)) {
      if ((t.amountPz || 0) > 10000) alertas.push({ tipo: 'tx_grande', severity: 'baja', mensaje: `Transferencia de ${t.amountPz} Pz`, entidad: t });
    }
    res.json(alertas.sort((a, b) => a.severidad?.localeCompare?.(b.severidad) || 0));
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Detalle de tarjetas ─────────────────────────────────────────────────────
router.get('/tarjetas-detalle', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    res.json(state.digitalCards || []);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Inversiones ─────────────────────────────────────────────────────────────
router.get('/inversiones', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    res.json({ holdings: state.investmentHoldings || [], operations: state.investmentOperations || [] });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Configuración del Tesoro ────────────────────────────────────────────────
router.get('/tesoro', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const tglp = (state.accounts || []).find(a => a.id === 'TGLP' || a.kind === 'TGLP');
    res.json({
      cuenta: tglp || null,
      config: state.treasuryConfig || null,
      totalUsuarios: (state.users || []).length,
      totalActivos: (state.accounts || []).length,
      masaMonetaria: (state.accounts || []).reduce((s, a) => s + (a.balancePz || 0), 0)
    });
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Gestión administrativa de cuentas (cambiar-tipo, asignar-eip, alta-tributos) ─
const ACCIONES_ADMIN = ['cambiar-tipo', 'asignar-eip', 'alta-tributos'];
router.post('/admin-cuentas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { action, ...params } = req.body;
    if (!ACCIONES_ADMIN.includes(action)) return res.status(400).json({ error: `Action debe ser: ${ACCIONES_ADMIN.join(', ')}` });
    const r = await fetch(`${BANCO_API}/api/crm-state`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
      body: JSON.stringify({ action, ...params })
    });
    const data = await r.json();
    if (!r.ok) return res.status(r.status).json(data);
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// ── Audit logs ─────────────────────────────────────────────────────────────
router.get('/audit-logs', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const state = await fetchBancoState(req);
    const logs = (state.auditLogs || []).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 100);
    res.json(logs);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

export default router;
