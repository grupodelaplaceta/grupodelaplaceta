import { Router } from 'express';

const router = Router();
const API = 'https://id.laplaceta.org/api';
const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
let _token = null;
let _exp = 0;

async function getToken() {
  if (_token && Date.now() < _exp) return _token;

  // Autenticar via API Key (plid26 acepta ccb611... como admin)
  const r = await fetch(`${API}/admin/stats`, {
    headers: { 'X-API-Key': API_KEY }
  });
  if (r.ok) { _token = API_KEY; _exp = Date.now() + 3600000; return _token; }

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

// ── Solicitantes (apps integradas en PlacetaID) ─────────────────────────────
router.get('/solicitantes', async (req, res) => {
  try { send(res, await call('/admin/solicitantes')); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.get('/solicitantes/:id', async (req, res) => {
  try { send(res, await call(`/admin/solicitantes/${req.params.id}`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.post('/solicitantes', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/admin/solicitantes`;
    const result = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`, 'X-API-Key': API_KEY },
      body: JSON.stringify(req.body)
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

router.post('/solicitantes/upload-logo', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/admin/solicitantes/upload-logo`;
    const result = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`, 'X-API-Key': API_KEY },
      body: JSON.stringify(req.body)
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

router.patch('/solicitantes/:id/branding', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/admin/solicitantes/${req.params.id}/branding`;
    const result = await fetch(url, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`, 'X-API-Key': API_KEY },
      body: JSON.stringify(req.body)
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

router.get('/solicitantes/:id/instrucciones', async (req, res) => {
  try { send(res, await call(`/admin/solicitantes/${req.params.id}/instrucciones`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

router.delete('/solicitantes/:id', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/admin/solicitantes/${req.params.id}`;
    const result = await fetch(url, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}`, 'X-API-Key': API_KEY }
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

// ── Registro: crear en PlacetaID (genera 2FA) ──────────────────────────────
router.post('/registro/crear', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/registro/solicitante`;
    const result = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-API-Key': API_KEY },
      body: JSON.stringify(req.body)
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

// ── Generar token de registro para compartir ───────────────────────────────
router.post('/registro/generar-token', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/registro/generar-token`;
    const result = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-API-Key': API_KEY },
      body: JSON.stringify(req.body)
    });
    const txt = await result.text();
    try { res.status(result.status).json(JSON.parse(txt)); }
    catch { res.status(result.status).json({ raw: txt.substring(0,200) }); }
  } catch (e) { res.status(502).json({ error: e.message }); }
});

// ── Listar registros de PlacetaID (para importar) ──────────────────────────
router.get('/registros-placetaid', async (req, res) => {
  try { send(res, await call(`/admin/registros?limit=${req.query.limit || 200}`)); }
  catch (e) { res.status(502).json({ error: e.message }); }
});

export default router;
