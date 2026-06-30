import { Router } from 'express';
import { getDb } from '../config/db.js';

const VC_API = process.env.VOLEY_API || 'http://localhost:3000/api';

const router = Router();

// ── Helper: fetch al backend del Voley Club ─────────────────────────────────

async function vcFetch(endpoint, options = {}) {
  try {
    const url = `${VC_API}${endpoint}`;
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json', ...options.headers },
      ...options
    });
    if (!res.ok) return { error: `Error ${res.status}`, data: null };
    const data = await res.json();
    return { error: null, data };
  } catch (e) {
    return { error: e.message, data: null };
  }
}

// ── Obtener token de administración ──────────────────────────────────────────

function getAuthToken(req) {
  // El gestor se autentica con el backend del Voley Club
  // Token almacenado en sesión
  return req.session?.voleyToken || null;
}

// ── Dashboard del Voley Club ────────────────────────────────────────────────

router.get('/dashboard', async (req, res) => {
  const { data, error } = await vcFetch('/dashboard');
  if (error) return res.status(502).json({ error: 'Voley Club no disponible', detalle: error });
  res.json(data);
});

// ── Torneos ─────────────────────────────────────────────────────────────────

router.get('/torneos', async (req, res) => {
  const { data, error } = await vcFetch('/torneos');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Partidos ─────────────────────────────────────────────────────────────────

router.get('/partidos', async (req, res) => {
  const { data, error } = await vcFetch('/partidos');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Añadir partido (como gestor) ─────────────────────────────────────────────

router.post('/partidos', async (req, res) => {
  const token = getAuthToken(req);
  const { data, error } = await vcFetch('/partidos', {
    method: 'POST',
    headers: token ? { 'x-auth-token': token } : {},
    body: JSON.stringify(req.body)
  });
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Eliminar partido ─────────────────────────────────────────────────────────

router.delete('/partidos/:id', async (req, res) => {
  const token = getAuthToken(req);
  const { data, error } = await vcFetch(`/partidos/${req.params.id}`, {
    method: 'DELETE',
    headers: token ? { 'x-auth-token': token } : {}
  });
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Noticias ─────────────────────────────────────────────────────────────────

router.get('/noticias', async (req, res) => {
  const { data, error } = await vcFetch('/noticias');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

router.post('/noticias', async (req, res) => {
  const token = getAuthToken(req);
  const { data, error } = await vcFetch('/noticias', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { 'x-auth-token': token } : {}) },
    body: JSON.stringify(req.body)
  });
  if (error) return res.status(502).json({ error });
  res.json(data);
});

router.delete('/noticias/:id', async (req, res) => {
  const token = getAuthToken(req);
  const { data, error } = await vcFetch(`/noticias/${req.params.id}`, {
    method: 'DELETE',
    headers: token ? { 'x-auth-token': token } : {}
  });
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Miembros ─────────────────────────────────────────────────────────────────

router.get('/miembros', async (req, res) => {
  const { data, error } = await vcFetch('/miembros');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Solicitudes ──────────────────────────────────────────────────────────────

router.get('/solicitudes', async (req, res) => {
  const { data, error } = await vcFetch('/solicitudes');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

router.post('/solicitudes/estado', async (req, res) => {
  // Aprobar/rechazar solicitudes guardando estado localmente
  const { id, estado } = req.body;
  if (!id || !estado) return res.status(400).json({ error: 'id y estado requeridos' });

  const db = getDb();
  db.prepare(`CREATE TABLE IF NOT EXISTS voley_solicitudes_estado (
    id INTEGER PRIMARY KEY, solicitud_id TEXT UNIQUE, estado TEXT,
    gestionado_por INTEGER, gestionado_en TEXT DEFAULT (datetime('now'))
  )`).run();

  db.prepare('INSERT OR REPLACE INTO voley_solicitudes_estado (solicitud_id, estado, gestionado_por) VALUES (?, ?, ?)')
    .run(String(id), estado, req.session.usuario?.id || null);

  res.json({ success: true, estado });
});

// ── Estado completo del CRM ──────────────────────────────────────────────────

router.get('/crm-data', async (req, res) => {
  const { data, error } = await vcFetch('/crm-data');
  if (error) return res.status(502).json({ error });
  res.json(data);
});

// ── Login como gestor ────────────────────────────────────────────────────────

router.post('/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) return res.status(400).json({ error: 'Credenciales requeridas' });

  const { data, error } = await vcFetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  if (error) return res.status(502).json({ error: 'Voley Club no disponible' });
  if (!data?.token) return res.status(401).json({ error: 'Credenciales inválidas' });

  // Guardar token en sesión
  req.session.voleyToken = data.token;
  req.session.voleyUser = data.user || null;

  res.json({ success: true, user: data.user });
});

// ── Estado de la sesión de Voley ─────────────────────────────────────────────

router.get('/session', (req, res) => {
  if (req.session.voleyToken) {
    res.json({ autenticado: true, user: req.session.voleyUser });
  } else {
    res.json({ autenticado: false });
  }
});

// ── Logout de Voley ──────────────────────────────────────────────────────────

router.post('/logout', (req, res) => {
  req.session.voleyToken = null;
  req.session.voleyUser = null;
  res.json({ success: true });
});

export default router;
