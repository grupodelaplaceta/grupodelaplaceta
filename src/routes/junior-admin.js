/**
 * Ruta de administración para Placeta Junior
 * Acceso completo desde CRM GDLP a:
 * - Documentos legales
 * - Autorizaciones PlacetaID
 * - Control parental
 * - Datos, vinculaciones, historial
 * - Impuestos (IVA pagado por Capitalia a Tributos)
 * - Demo mode (tutor 11111111D)
 */
import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';

const router = Router();
const JUNIOR_API_URL = process.env.JUNIOR_API_URL || 'http://localhost:3005/api';
const JUNIOR_API_KEY = process.env.JUNIOR_API_KEY || 'placeta-junior-crm-key-2026';

async function juniorFetch(endpoint, opts = {}) {
  const url = `${JUNIOR_API_URL}/crm${endpoint}`;
  const r = await fetch(url, {
    headers: { 'Content-Type': 'application/json', 'X-API-Key': JUNIOR_API_KEY }, ...opts
  });
  if (!r.ok) { let msg = `HTTP ${r.status}`; try { const e = await r.json(); if (e.error) msg = e.error; } catch {} throw new Error(msg); }
  return r.json();
}

// ── 1. STATS ───────────────────────────────────────────────────────────────
router.get('/junior/stats', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const stats = await juniorFetch('/juniors/stats');
    try { const imp = await juniorFetch('/impuestos'); stats.impuestos = imp; } catch {}
    res.json(stats);
  } catch (err) {
    try {
      const db = getDb();
      const total = db.prepare("SELECT COUNT(*) as t FROM junior_menores").get()?.t || 0;
      const activos = db.prepare("SELECT COUNT(*) as t FROM junior_menores WHERE estado='activo'").get()?.t || 0;
      const pendientes = db.prepare("SELECT COUNT(*) as t FROM junior_menores WHERE estado='pendiente_firma_tutor'").get()?.t || 0;
      res.json({ total, activos, pendientes_firma: pendientes, impuestos: null });
    } catch { res.json({ total: 0, activos: 0, pendientes_firma: 0 }); }
  }
});

// ── 2. JUNIORS ──────────────────────────────────────────────────────────────
router.get('/junior/list', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch(`/juniors${req.query.estado ? `?estado=${req.query.estado}` : ''}`)); }
  catch { res.json([]); }
});

router.get('/junior/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch(`/junior/${req.params.dip}`)); }
  catch (err) { res.status(500).json({ error: err.message }); }
});

router.post('/junior/:id/estado', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch(`/junior/${req.params.id}/estado`, { method: 'POST', body: JSON.stringify({ estado: req.body.estado }) })); }
  catch (err) { res.status(500).json({ error: err.message }); }
});

router.post('/junior/sincronizar', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const juniors = await juniorFetch('/juniors');
    let sinc = 0;
    const db = getDb();
    const ins = db.prepare(`INSERT OR IGNORE INTO junior_menores (solicitante_id,dip,nombre,apellidos,modalidad,tutor_dip,estado,placetas_saldo,nivel_academia,creado_en) VALUES(?,?,?,?,?,?,?,?,?,?)`);
    for (const j of juniors) { try { ins.run(j.solicitante_id,j.dip,j.nombre,j.apellidos,j.modalidad,j.tutor_dip,j.estado,j.placetas_saldo||0,j.nivel_academia||1,j.creado_en||new Date().toISOString()); sinc++; } catch {} }
    res.json({ success: true, sincronizados: sinc, total: juniors.length });
  } catch (err) { res.status(500).json({ error: err.message, sincronizados: 0 }); }
});

// ── 3. CONTROL PARENTAL ────────────────────────────────────────────────────
router.get('/junior/control-parental/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { const d = await juniorFetch(`/junior/${req.params.dip}`); res.json(d.limites_parentales || {}); }
  catch { res.json({}); }
});

router.get('/junior/control-parental', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch('/control-parental/todos')); }
  catch { res.json([]); }
});

// ── 4. AUTORIZACIONES (PlacetaID) ──────────────────────────────────────────
router.get('/junior/autorizaciones', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { res.json(await juniorFetch('/autorizaciones')); } catch { res.json([]); }
});

router.get('/junior/autorizaciones/:dip', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { res.json(await juniorFetch(`/autorizaciones/${req.params.dip}`)); } catch { res.json([]); }
});

// ── 5. VINCULACIONES ───────────────────────────────────────────────────────
router.get('/junior/vinculaciones', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch('/vinculaciones')); } catch { res.json([]); }
});

router.get('/junior/vinculaciones/:dipTutor', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch(`/tutor/${req.params.dipTutor}/menores`)); } catch { res.json([]); }
});

// ── 6. IMPUESTOS ───────────────────────────────────────────────────────────
router.get('/junior/impuestos', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch('/impuestos')); }
  catch {
    try {
      const db = getDb();
      const totalIva = db.prepare("SELECT COALESCE(SUM(cantidad),0) as total FROM junior_transacciones WHERE concepto LIKE '%IVA%'").get()?.total || 0;
      const totalOp = db.prepare("SELECT COUNT(*) as t FROM junior_transacciones WHERE tipo='gastar'").get()?.t || 0;
      res.json({ total_iva_pagado: totalIva, total_operaciones: totalOp, total_recaudado: totalIva });
    } catch { res.json({ total_iva_pagado: 0, total_operaciones: 0, total_recaudado: 0 }); }
  }
});

// ── 7. DOCUMENTOS LEGALES ──────────────────────────────────────────────────
router.get('/junior/documentos', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { res.json(await juniorFetch('/documentos')); } catch { res.json([]); }
});

router.post('/junior/documentos/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { res.json(await juniorFetch('/documentos/crear', { method: 'POST', body: JSON.stringify(req.body) })); }
  catch (err) { res.status(500).json({ error: err.message }); }
});

// ── 8. LOGS ────────────────────────────────────────────────────────────────
router.get('/junior/logs', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { res.json(await juniorFetch('/logs')); } catch { res.json([]); }
});

router.get('/junior/historial-placetas/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { res.json(await juniorFetch(`/historial/${req.params.dip}`)); } catch { res.json([]); }
});

// ── 9. PLACETAID ────────────────────────────────────────────────────────────
router.get('/junior/placetaid/:dip', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const PLID = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const r = await fetch(`${PLID}/api/admin/registros`, { headers: { 'X-API-Key': KEY } });
    if (!r.ok) return res.json(null);
    const registros = await r.json();
    res.json(registros.find(reg => reg.dip === req.params.dip) || null);
  } catch { res.json(null); }
});

router.get('/junior/placetaid', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const PLID = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const r = await fetch(`${PLID}/api/admin/registros`, { headers: { 'X-API-Key': KEY } });
    if (!r.ok) return res.json([]);
    res.json(await r.json());
  } catch { res.json([]); }
});

// ── 10. DEMO ────────────────────────────────────────────────────────────────
router.post('/junior/demo/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const db = getDb();
    let tutorDemo = db.prepare("SELECT * FROM solicitantes WHERE dip='11111111D'").get();
    if (!tutorDemo) {
      db.prepare("INSERT INTO solicitantes (alias,nombre_real,email,dip,placeid,rol,estado,franja_edad) VALUES ('tutor-demo','Tutor Demo','tutor@demo.com','11111111D','PLID-DEMO','tutor','activo','Alta_Plena')").run();
      tutorDemo = db.prepare("SELECT * FROM solicitantes WHERE dip='11111111D'").get();
    }
    const dip = `JUNIOR-DEMO-${Date.now().toString(36).toUpperCase().slice(-4)}`;
    const alias = `demo.${Date.now().toString(36).toLowerCase().slice(-4)}`;
    db.prepare(`INSERT OR IGNORE INTO junior_menores (solicitante_id,dip,nombre,apellidos,edad,modalidad,tutor_dip,tutor_nombre,estado,placetas_saldo,nivel_academia,creado_en) VALUES(?,?,?,?,?,?,?,?,?,?,?,datetime('now'))`)
      .run(tutorDemo.id, dip, 'Usuario', 'Demo', 10, 'Placeta Junior', '11111111D', 'Tutor Demo', 'activo', 5000, 5);
    res.json({ success: true, message: `🧪 Usuario demo creado. DIP: ${dip} | Tutor: 11111111D (demo)`, dip });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

export default router;
