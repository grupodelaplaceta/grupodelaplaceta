/**
 * Placeta Junior — Admin CRM (versión resiliente)
 * Funciona con Supabase (si disponible) o SQLite (fallback)
 * Nunca devuelve HTML — siempre JSON
 */
import crypto from 'crypto';
import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { supabase } from '../config/supabase.js';

const router = Router();

// Helper: query Supabase con protección null
async function s(table, select = '*', opts = {}) {
  if (!supabase) return null;
  try {
    let q = supabase.from(table).select(select);
    if (opts.eq) for (const [k, v] of Object.entries(opts.eq)) q = q.eq(k, v);
    if (opts.in) for (const [k, v] of Object.entries(opts.in)) q = q.in(k, v);
    if (opts.order) q = q.order(opts.order.field, { ascending: opts.order.asc ?? false });
    if (opts.limit) q = q.limit(opts.limit);
    const { data, error } = await q;
    return error ? null : (data || []);
  } catch { return null; }
}

// ── 1. STATS ────────────────────────────────────────────────────────────────
router.get('/junior/stats', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const todos = await s('junior_menores', 'id,estado,placetas_saldo');
    if (!todos) throw new Error('supabase_null');
    const trans = await s('junior_transacciones', 'cantidad,tipo');
    const gastos = (trans || []).filter(t => t.tipo === 'gastar').reduce((a, b) => a + (b.cantidad || 0), 0);
    return res.json({
      total: todos.length, activos: todos.filter(j => j.estado === 'activo').length,
      pendientes_firma: todos.filter(j => j.estado === 'pendiente_firma_tutor').length,
      total_placetas_circulacion: todos.reduce((s, j) => s + (j.placetas_saldo || 0), 0),
      impuestos: { total_iva_pagado: Math.ceil(gastos * 12 / 100), total_operaciones: (trans || []).length }
    });
  } catch {
    try {
      const db = getDb();
      const t = db.prepare("SELECT COUNT(*) as c FROM junior_menores").get()?.c || 0;
      const a = db.prepare("SELECT COUNT(*) as c FROM junior_menores WHERE estado='activo'").get()?.c || 0;
      const p = db.prepare("SELECT COUNT(*) as c FROM junior_menores WHERE estado='pendiente_firma_tutor'").get()?.c || 0;
      return res.json({ total: t, activos: a, pendientes_firma: p, total_placetas_circulacion: 0, impuestos: null });
    } catch { return res.json({ total: 0, activos: 0, pendientes_firma: 0 }); }
  }
});

// ── 2. JUNIORS ──────────────────────────────────────────────────────────────
router.get('/junior/list', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const d = await s('junior_menores', '*, tutor:solicitantes!junior_menores_tutor_dip_fkey(dip,alias,nombre_real,email)', { order: { field: 'creado_en', asc: false }, ...(req.query.estado ? { eq: { estado: req.query.estado } } : {}) });
    if (d) return res.json(d);
    throw new Error('supabase_null');
  } catch {
    try {
      const db = getDb();
      const rows = db.prepare(`SELECT j.*, s.alias as tutor_alias, s.dip as tutor_dip FROM junior_menores j LEFT JOIN solicitantes s ON j.tutor_dip = s.dip ORDER BY j.creado_en DESC`).all();
      return res.json(rows);
    } catch { return res.json([]); }
  }
});

router.get('/junior/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    if (!supabase) throw new Error('no_supabase');
    const { data: j } = await supabase.from('junior_menores').select('*, tutor:solicitantes!junior_menores_tutor_dip_fkey(dip,alias,nombre_real,email)').eq('dip', req.params.dip).limit(1).single();
    if (!j) return res.json({ junior: null, limites_parentales: null });
    const cp = await s('junior_control_parental', '*', { eq: { junior_id: j.id } });
    return res.json({ junior: j, limites_parentales: cp?.[0] || null });
  } catch { return res.json({ junior: null, limites_parentales: null }); }
});

router.post('/junior/:id/estado', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    if (supabase) await supabase.from('junior_menores').update({ estado: req.body.estado }).eq('id', req.params.id);
    const db = getDb();
    db.prepare("UPDATE junior_menores SET estado=? WHERE id=?").run(req.body.estado, req.params.id);
    return res.json({ success: true });
  } catch { return res.json({ success: true }); }
});

// ── 3. CONTROL PARENTAL ─────────────────────────────────────────────────────
router.get('/junior/control-parental', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const d = await s('junior_control_parental', '*, junior:junior_menores!junior_control_parental_junior_id_fkey(nombre,apellidos,dip)', { order: { field: 'actualizado_en', asc: false } });
    if (d) return res.json(d);
    throw new Error('null');
  } catch { return res.json([]); }
});

// ── 4. AUTORIZACIONES ───────────────────────────────────────────────────────
router.get('/junior/autorizaciones', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const d = await s('junior_logs', '*, junior:junior_menores!junior_logs_junior_id_fkey(nombre,apellidos,dip)', { in: { accion: ['solicitar_autorizacion_tutor','acceso_aprobado','vinculacion_tutor'] }, order: { field: 'creado_en', asc: false }, limit: 200 });
    return res.json(d || []);
  } catch { return res.json([]); }
});

// ── 5. VINCULACIONES ────────────────────────────────────────────────────────
router.get('/junior/vinculaciones', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const d = await s('junior_menores', 'dip,nombre,apellidos,tutor_dip,tutor_nombre,firmado_en,estado', { eq: { estado: 'activo' }, order: { field: 'creado_en', asc: false } });
    if (d) return res.json(d.map(j => ({ dip_menor: j.dip, nombre_menor: `${j.nombre} ${j.apellidos}`, dip_tutor: j.tutor_dip, nombre_tutor: j.tutor_nombre, firmado_en: j.firmado_en, estado: j.estado })));
    throw new Error('null');
  } catch { return res.json([]); }
});

// ── 6. IMPUESTOS ────────────────────────────────────────────────────────────
router.get('/junior/impuestos', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const all = await s('junior_transacciones', '*');
    if (!all) throw new Error('null');
    const gastos = all.filter(t => t.tipo === 'gastar').reduce((a, b) => a + (b.cantidad || 0), 0);
    const ganados = all.filter(t => t.tipo === 'ganar' || t.tipo === 'bonus').reduce((a, b) => a + (b.cantidad || 0), 0);
    return res.json({ total_iva_pagado: Math.ceil(gastos * 12 / 100), total_operaciones: all.length, total_gastado_menores: gastos, total_ganado_menores: ganados, iva_percent: 12, pagado_por: 'Capitalia', recibido_por: 'TGLP' });
  } catch { return res.json({ total_iva_pagado: 0, total_operaciones: 0, total_gastado_menores: 0, total_ganado_menores: 0 }); }
});

// ── 7. DOCUMENTOS ───────────────────────────────────────────────────────────
router.get('/junior/documentos', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const d = await s('documentos_firmados', '*, firmante:solicitantes!documentos_firmados_firmado_por_fkey(alias,dip)', { in: { codigo_modelo: ['PJ-TYC-001','PJ-PRV-001','PJ-CON-001'] }, order: { field: 'creado_en', asc: false }, limit: 100 });
    return res.json(d || []);
  } catch { return res.json([]); }
});

router.post('/junior/documentos/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const docs = [
      { codigo_modelo: 'PJ-TYC-001', titulo_documento: 'Términos y Condiciones Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() },
      { codigo_modelo: 'PJ-PRV-001', titulo_documento: 'Política de Privacidad Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() },
      { codigo_modelo: 'PJ-CON-001', titulo_documento: 'Consentimiento del Tutor Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() }
    ];
    const r = [];
    if (supabase) {
      for (const d of docs) { const { data } = await supabase.from('documentos_firmados').insert(d).select().single(); if (data) r.push(data); }
    }
    return res.json({ success: true, documentos: r });
  } catch (err) { return res.json({ success: false, error: err.message }); }
});

// ── 8. LOGS ─────────────────────────────────────────────────────────────────
router.get('/junior/logs', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const d = await s('junior_logs', '*, junior:junior_menores!junior_logs_junior_id_fkey(nombre,apellidos,dip)', { order: { field: 'creado_en', asc: false }, limit: 100 });
    return res.json(d || []);
  } catch { return res.json([]); }
});

// ── 9. PLACETAID ────────────────────────────────────────────────────────────
router.get('/junior/placetaid/:dip', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const URL = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const r = await fetch(`${URL}/api/admin/registros`, { headers: { 'X-API-Key': KEY } });
    if (!r.ok) return res.json(null);
    const regs = await r.json();
    return res.json(regs.find(r => r.dip === req.params.dip) || null);
  } catch { return res.json(null); }
});

router.get('/junior/placetaid', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const URL = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const r = await fetch(`${URL}/api/admin/registros`, { headers: { 'X-API-Key': KEY } });
    return res.json(r.ok ? await r.json() : []);
  } catch { return res.json([]); }
});

// ── 10. DEMO ────────────────────────────────────────────────────────────────
router.post('/junior/demo/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const db = getDb();
    let tutor = db.prepare("SELECT id FROM solicitantes WHERE dip='11111111D'").get();
    if (!tutor) { db.prepare("INSERT INTO solicitantes (alias,nombre_real,email,dip,placeid,rol,estado,franja_edad) VALUES ('tutor-demo','Tutor Demo','tutor@demo.com','11111111D','PLID-DEMO','tutor','activo','Alta_Plena')").run(); tutor = db.prepare("SELECT id FROM solicitantes WHERE dip='11111111D'").get(); }
    const dip = `JUNIOR-DEMO-${Date.now().toString(36).toUpperCase().slice(-4)}`;
    db.prepare("INSERT OR IGNORE INTO junior_menores (solicitante_id,dip,nombre,apellidos,edad,modalidad,tutor_dip,tutor_nombre,estado,placetas_saldo,nivel_academia,creado_en) VALUES(?,?,?,?,?,?,?,?,?,?,?,datetime('now'))").run(tutor.id, dip, 'Usuario', 'Demo', 10, 'Placeta Junior', '11111111D', 'Tutor Demo', 'activo', 5000, 5);
    return res.json({ success: true, message: `🧪 Demo: ${dip}`, dip });
  } catch (err) { return res.status(500).json({ error: err.message }); }
});

export default router;
