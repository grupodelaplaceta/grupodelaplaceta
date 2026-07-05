import { Router } from 'express';

const router = Router();
const API = 'https://id.laplaceta.org/api';
const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
const DEMO_USER = { dip: '11111111D', password: 'Demo1234!' };

let _token = null;
let _exp = 0;

async function getToken() {
  if (_token && Date.now() < _exp) return _token;

  // Usar API Key directamente (plid26 acepta ccb611... como admin)
  try {
    const r = await fetch(`${API}/admin/stats`, {
      headers: { 'X-API-Key': API_KEY }
    });
    if (r.ok) { _token = API_KEY; _exp = Date.now() + 3600000; return _token; }
  } catch {}

  // Fallback: login OAuth demo
  try {
    const f1 = await fetch(`${API}/auth/fase1`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ dip: DEMO_USER.dip, password: DEMO_USER.password })
    });
    if (f1.ok) {
      const d = await f1.json();
      const t = d.tokenSesion || d.token || d.accessToken;
      if (t) {
        const test = await fetch(`${API}/admin/stats`, { headers: { 'Authorization': `Bearer ${t}`, 'X-API-Key': API_KEY } });
        if (test.ok || test.status === 403) { _token = t; _exp = Date.now() + 300000; return _token; }
      }
    }
  } catch {}

  throw new Error('No se pudo autenticar contra PlacetaID. Verifica PLACETAID_API_KEY.');
}

async function call(path) {
  const token = await getToken();
  const url = `${API}${path}`;
  const res = await fetch(url, { headers: { 'Authorization': `Bearer ${token}`, 'X-API-Key': API_KEY } });
  const txt = await res.text();
  try { return { status: res.status, data: JSON.parse(txt) }; }
  catch { return { status: res.status, data: { raw: txt.substring(0,200) } }; }
}

function send(res, result) {
  if (result.status === 403 && result.data?.error?.includes('administradores')) {
    return res.status(502).json({
      error: 'El token no tiene permisos de administrador.',
      solucion: 'Despliega verifyAdminApiKey en PLID26 (plid26-main/server.js) o asigna rol administrador al usuario demo.'
    });
  }
  res.status(result.status).json(result.data);
}

router.get('/stats', async (req, res) => {
  try { send(res, await call('/admin/stats')); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.get('/registros', async (req, res) => {
  try { send(res, await call('/admin/registros')); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.get('/logs', async (req, res) => {
  try { send(res, await call(`/admin/logs?limit=${req.query.limit || 50}`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.post('/desbloquear/:dip', async (req, res) => {
  try { send(res, await call(`/admin/desbloquear/${req.params.dip}`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.post('/toggle/:dip', async (req, res) => {
  try { send(res, await call(`/admin/toggle/${req.params.dip}`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

export default router;
