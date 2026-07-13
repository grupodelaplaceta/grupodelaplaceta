import { Router } from 'express';
import { supabase } from '../config/supabase.js';
import { getDb } from '../config/db.js';

const router = Router();
const API = 'https://id.laplaceta.org/api';
const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
let _token = null;
let _exp = 0;

async function getToken() {
  if (_token && Date.now() < _exp) return _token;

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
      solucion: 'Despliega verifyAdminApiKey en PLID26 o asigna rol administrador al usuario demo.',
      fallback: true
    });
  }
  res.status(result.status).json(result.data);
}

// ── Fallback local ─────────────────────────────────────────────────────────
async function localRegistros() {
  try {
    if (supabase) {
      const { data } = await supabase.from('solicitantes').select('dip,nombre_real,alias,email,rol,estado,creado_en').limit(500);
      if (data) return data.map(r => ({
        dip: r.dip, nombre: r.nombre_real, alias: r.alias, email: r.email,
        rol: r.rol, activo: r.estado === 'activo', bloqueado: r.estado === 'suspendido',
        intentosFallidos: 0, totpVerified: false, createdAt: r.creado_en
      }));
    }
    const db = getDb();
    const rows = db.prepare("SELECT dip,nombre_real,alias,email,rol,estado,creado_en FROM solicitantes ORDER BY creado_en DESC LIMIT 500").all();
    return rows.map(r => ({
      dip: r.dip, nombre: r.nombre_real, alias: r.alias, email: r.email,
      rol: r.rol, activo: r.estado === 'activo', bloqueado: r.estado === 'suspendido',
      intentosFallidos: 0, totpVerified: false, createdAt: r.creado_en
    }));
  } catch { return []; }
}

async function localToggle(dip) {
  try {
    if (supabase) {
      const { data } = await supabase.from('solicitantes').select('estado').eq('dip', dip).single();
      const newState = data?.estado === 'suspendido' ? 'activo' : 'suspendido';
      await supabase.from('solicitantes').update({ estado: newState }).eq('dip', dip);
      return { ok: true, activo: newState === 'activo', mensaje: `Estado cambiado a: ${newState}` };
    }
    const db = getDb();
    const u = db.prepare("SELECT estado FROM solicitantes WHERE dip=?").get(dip);
    if (!u) return null;
    const newState = u.estado === 'suspendido' ? 'activo' : 'suspendido';
    db.prepare("UPDATE solicitantes SET estado=? WHERE dip=?").run(newState, dip);
    return { ok: true, activo: newState === 'activo', mensaje: `Estado cambiado a: ${newState}` };
  } catch { return null; }
}

async function localDesbloquear(dip) {
  try {
    if (supabase) {
      await supabase.from('solicitantes').update({ estado: 'activo' }).eq('dip', dip);
      return { ok: true, mensaje: `DIP ${dip} desbloqueado (local)` };
    }
    const db = getDb();
    db.prepare("UPDATE solicitantes SET estado='activo' WHERE dip=?").run(dip);
    return { ok: true, mensaje: `DIP ${dip} desbloqueado (local)` };
  } catch { return null; }
}

router.get('/stats', async (req, res) => {
  try { const r = await call('/admin/stats'); send(res, r); }
  catch {
    const regs = await localRegistros();
    res.json({ total: regs.length, activos: regs.filter(r => r.activo && !r.bloqueado).length, bloqueados: regs.filter(r => r.bloqueado).length, online: false });
  }
});

router.get('/registros', async (req, res) => {
  try { const r = await call('/admin/registros'); send(res, r); }
  catch {
    const regs = await localRegistros();
    const count = regs.length;
    const activos = regs.filter(r => r.activo && !r.bloqueado).length;
    const bloqueados = regs.filter(r => r.bloqueado).length;
    res.json({ registros: regs, total: count, activos, bloqueados, online: false });
  }
});

router.get('/logs', async (req, res) => {
  try { send(res, await call(`/admin/logs?limit=${req.query.limit || 50}`)); }
  catch { res.json({ logs: [], online: false, mensaje: 'PLID26 no disponible. Los logs requieren conexión directa.' }); }
});

router.post('/desbloquear/:dip', async (req, res) => {
  try { send(res, await call(`/admin/desbloquear/${req.params.dip}`)); }
  catch {
    const result = await localDesbloquear(req.params.dip);
    if (result) res.json(result);
    else res.status(502).json({ error: 'PLID26 no disponible y no hay fallback local', online: false });
  }
});

router.post('/toggle/:dip', async (req, res) => {
  try { send(res, await call(`/admin/toggle/${req.params.dip}`)); }
  catch {
    const result = await localToggle(req.params.dip);
    if (result) res.json(result);
    else res.status(502).json({ error: 'PLID26 no disponible y no hay fallback local', online: false });
  }
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

// ── Actualizar solicitante completo (PUT) ────────────────────────────────────
router.put('/solicitantes/:id', async (req, res) => {
  try {
    const token = await getToken();
    const url = `${API}/admin/solicitantes/${req.params.id}`;
    const result = await fetch(url, {
      method: 'PUT',
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
