/**
 * Placeta Junior — Admin CRM
 * Consulta directa a Supabase (sin depender de API externa)
 */
import crypto from 'crypto';
import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { supabase } from '../config/supabase.js';

const router = Router();

async function sbQuery(table, select = '*', opts = {}) {
  try {
    let q = supabase.from(table).select(select);
    if (opts.eq) for (const [k, v] of Object.entries(opts.eq)) q = q.eq(k, v);
    if (opts.in) for (const [k, v] of Object.entries(opts.in)) q = q.in(k, v);
    if (opts.order) q = q.order(opts.order.field, { ascending: opts.order.asc ?? false });
    if (opts.limit) q = q.limit(opts.limit);
    const { data, error } = await q;
    if (error) throw error;
    return data || [];
  } catch (e) { console.error(`[Junior] Query ${table}:`, e.message); return null; }
}

// ── 1. STATS ────────────────────────────────────────────────────────────────
router.get('/junior/stats', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const todos = await sbQuery('junior_menores', 'id,estado,modalidad,placetas_saldo');
    if (!todos) return res.json({ total: 0, activos: 0, pendientes_firma: 0 });
    const trans = await sbQuery('junior_transacciones', 'cantidad,tipo');
    const gastos = (trans || []).filter(t => t.tipo === 'gastar').reduce((s, t) => s + (t.cantidad || 0), 0);
    res.json({
      total: todos.length, activos: todos.filter(j => j.estado === 'activo').length,
      pendientes_firma: todos.filter(j => j.estado === 'pendiente_firma_tutor').length,
      total_placetas_circulacion: todos.reduce((s, j) => s + (j.placetas_saldo || 0), 0),
      impuestos: { total_iva_pagado: Math.ceil(gastos * 12 / 100), total_operaciones: (trans || []).length }
    });
  } catch { res.json({ total: 0, activos: 0, pendientes_firma: 0 }); }
});

// ── 2. JUNIORS ──────────────────────────────────────────────────────────────
router.get('/junior/list', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const d = await sbQuery('junior_menores', '*, tutor:solicitantes!junior_menores_tutor_dip_fkey(dip,alias,nombre_real,email)', { order: { field: 'creado_en', asc: false }, ...(req.query.estado ? { eq: { estado: req.query.estado } } : {}) });
    res.json(d || []);
  } catch { res.json([]); }
});

router.get('/junior/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data: j, error: e } = await supabase.from('junior_menores').select('*, tutor:solicitantes!junior_menores_tutor_dip_fkey(dip,alias,nombre_real,email)').eq('dip', req.params.dip).limit(1).single();
    if (e && e.code !== 'PGRST116') throw e;
    const cp = j ? await sbQuery('junior_control_parental', '*', { eq: { junior_id: j.id } }) : null;
    res.json({ junior: j || null, limites_parentales: cp?.[0] || null });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

router.post('/junior/:id/estado', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  const { error } = await supabase.from('junior_menores').update({ estado: req.body.estado }).eq('id', req.params.id);
  if (error) return res.status(500).json({ error: error.message });
  res.json({ success: true });
});

// ── 3. CONTROL PARENTAL ─────────────────────────────────────────────────────
router.get('/junior/control-parental/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data: j } = await supabase.from('junior_menores').select('id').eq('dip', req.params.dip).limit(1).single();
    if (!j) return res.json({});
    const cp = await sbQuery('junior_control_parental', '*', { eq: { junior_id: j.id } });
    res.json(cp?.[0] || {});
  } catch { res.json({}); }
});

router.get('/junior/control-parental', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data } = await supabase.from('junior_control_parental').select('*, junior:junior_menores!junior_control_parental_junior_id_fkey(nombre,apellidos,dip)').order('actualizado_en', { ascending: false });
    res.json(data || []);
  } catch { res.json([]); }
});

// ── 4. AUTORIZACIONES ───────────────────────────────────────────────────────
router.get('/junior/autorizaciones', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const { data } = await supabase.from('junior_logs').select('*, junior:junior_menores!junior_logs_junior_id_fkey(nombre,apellidos,dip)').in('accion', ['solicitar_autorizacion_tutor','acceso_aprobado','vinculacion_tutor']).order('creado_en', { ascending: false }).limit(200);
    res.json(data || []);
  } catch { res.json([]); }
});

// ── 5. VINCULACIONES ────────────────────────────────────────────────────────
router.get('/junior/vinculaciones', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data } = await supabase.from('junior_menores').select('dip,nombre,apellidos,tutor_dip,tutor_nombre,firmado_en,estado').eq('estado','activo').order('creado_en', { ascending: false });
    res.json((data || []).map(j => ({ dip_menor: j.dip, nombre_menor: `${j.nombre} ${j.apellidos}`, dip_tutor: j.tutor_dip, nombre_tutor: j.tutor_nombre, firmado_en: j.firmado_en, estado: j.estado })));
  } catch { res.json([]); }
});

router.get('/junior/vinculaciones/:dipTutor', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try { const d = await sbQuery('junior_menores', '*', { eq: { tutor_dip: req.params.dipTutor }, order: { field: 'creado_en', asc: false } }); res.json(d || []); }
  catch { res.json([]); }
});

// ── 6. IMPUESTOS ────────────────────────────────────────────────────────────
router.get('/junior/impuestos', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const all = await sbQuery('junior_transacciones', '*');
    if (!all) return res.json({ total_iva_pagado: 0, total_operaciones: 0 });
    const gastos = all.filter(t => t.tipo === 'gastar').reduce((s, t) => s + (t.cantidad || 0), 0);
    const ganados = all.filter(t => t.tipo === 'ganar' || t.tipo === 'bonus').reduce((s, t) => s + (t.cantidad || 0), 0);
    res.json({ total_iva_pagado: Math.ceil(gastos * 12 / 100), total_operaciones: all.length, total_gastado_menores: gastos, total_ganado_menores: ganados, iva_percent: 12, pagado_por: 'Capitalia', recibido_por: 'TGLP' });
  } catch { res.json({ total_iva_pagado: 0, total_operaciones: 0 }); }
});

// ── 7. DOCUMENTOS ───────────────────────────────────────────────────────────
router.get('/junior/documentos', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const { data } = await supabase.from('documentos_firmados').select('*, firmante:solicitantes!documentos_firmados_firmado_por_fkey(alias,dip)').in('codigo_modelo', ['PJ-TYC-001','PJ-PRV-001','PJ-CON-001']).order('creado_en', { ascending: false }).limit(100);
    res.json(data || []);
  } catch { res.json([]); }
});

router.post('/junior/documentos/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const docs = [
      { codigo_modelo: 'PJ-TYC-001', titulo_documento: 'Términos y Condiciones Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() },
      { codigo_modelo: 'PJ-PRV-001', titulo_documento: 'Política de Privacidad Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() },
      { codigo_modelo: 'PJ-CON-001', titulo_documento: 'Consentimiento del Tutor Placeta Junior', estado: 'pendiente', url_firma: crypto.randomUUID() }
    ];
    const r = [];
    for (const d of docs) { const { data } = await supabase.from('documentos_firmados').insert(d).select().single(); if (data) r.push(data); }
    res.json({ success: true, documentos: r });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// ── 8. LOGS ─────────────────────────────────────────────────────────────────
router.get('/junior/logs', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try { const { data } = await supabase.from('junior_logs').select('*, junior:junior_menores!junior_logs_junior_id_fkey(nombre,apellidos,dip)').order('creado_en', { ascending: false }).limit(100); res.json(data || []); }
  catch { res.json([]); }
});

router.get('/junior/historial-placetas/:dip', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data: j } = await supabase.from('junior_menores').select('id').eq('dip', req.params.dip).limit(1).single();
    if (!j) return res.json([]);
    const { data } = await supabase.from('junior_transacciones').select('*').eq('junior_id', j.id).order('creado_en', { ascending: false }).limit(100);
    res.json(data || []);
  } catch { res.json([]); }
});

// ── 9. PLACETAID ────────────────────────────────────────────────────────────
router.get('/junior/placetaid/:dip', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const r = await fetch(`${process.env.PLACETAID_API_URL || 'https://id.laplaceta.org'}/api/admin/registros`, { headers: { 'X-API-Key': process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb' } });
    if (!r.ok) return res.json(null);
    const regs = await r.json();
    res.json(regs.find(reg => reg.dip === req.params.dip) || null);
  } catch { res.json(null); }
});

router.get('/junior/placetaid', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const r = await fetch(`${process.env.PLACETAID_API_URL || 'https://id.laplaceta.org'}/api/admin/registros`, { headers: { 'X-API-Key': process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb' } });
    res.json(r.ok ? await r.json() : []);
  } catch { res.json([]); }
});

// ── 10. DEMO ────────────────────────────────────────────────────────────────
router.post('/junior/demo/crear', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const db = getDb();
    let tutor = db.prepare("SELECT * FROM solicitantes WHERE dip='11111111D'").get();
    if (!tutor) { db.prepare("INSERT INTO solicitantes (alias,nombre_real,email,dip,placeid,rol,estado,franja_edad) VALUES ('tutor-demo','Tutor Demo','tutor@demo.com','11111111D','PLID-DEMO','tutor','activo','Alta_Plena')").run(); tutor = db.prepare("SELECT * FROM solicitantes WHERE dip='11111111D'").get(); }
    const dip = `JUNIOR-DEMO-${Date.now().toString(36).toUpperCase().slice(-4)}`;
    db.prepare(`INSERT OR IGNORE INTO junior_menores (solicitante_id,dip,nombre,apellidos,edad,modalidad,tutor_dip,tutor_nombre,estado,placetas_saldo,nivel_academia,creado_en) VALUES(?,?,?,?,?,?,?,?,?,?,?,datetime('now'))`).run(tutor.id, dip, 'Usuario', 'Demo', 10, 'Placeta Junior', '11111111D', 'Tutor Demo', 'activo', 5000, 5);
    res.json({ success: true, message: `🧪 Demo: ${dip}`, dip });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

export default router;
