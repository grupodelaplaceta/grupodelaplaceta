import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';

const router = Router();
const BANCO_API = process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org';

// ── Cache simple ────────────────────────────────────────────────────────────
let cache = { data: null, expiresAt: 0 };
const CACHE_TTL = 30_000; // 30 segundos

async function fetchBancoState(req) {
  if (cache.data && Date.now() < cache.expiresAt) return cache.data;
  // Usar el token PlacetaID de la sesión para autenticar
  const token = req.session?.placetaidToken || req.session?.usuario?.placetaid_token;
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  headers['X-Placeta-App-ID'] = 'gdlp-crm';
  const res = await fetch(`${BANCO_API}/api/state`, { headers });
  if (!res.ok) throw new Error(`Banco API respondió ${res.status}`);
  const data = await res.json();
  cache = { data, expiresAt: Date.now() + CACHE_TTL };
  return data;
}

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

export default router;
