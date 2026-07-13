import { Router } from 'express';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { supabase } from '../config/supabase.js';
import { getDb } from '../config/db.js';

const router = Router();

// ── KPIs ──────────────────────────────────────────────────────────────────

router.get('/kpi-ciudadanos', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try { const { count } = await supabase.from('solicitantes').select('*', { count: 'exact', head: true }); res.json({ count }); }
  catch { res.json({ count: 0 }); }
});

router.get('/kpi-juniors', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try { const { count } = await supabase.from('junior_menores').select('*', { count: 'exact', head: true }); res.json({ count }); }
  catch { res.json({ count: 0 }); }
});

router.get('/kpi-contribuyentes', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { data, count } = await supabase.from('tributos_contribuyentes').select('*', { count: 'exact', head: true });
    res.json({ count: count || (data||[]).length });
  } catch { res.json({ count: 0 }); }
});

router.get('/kpi-cuentas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const r = await supabase.from('cuentas_bancarias').select('*', { count: 'exact', head: true });
    res.json({ count: r.count || 0 });
  } catch {
    // Fallback SQLite
    try { const db = getDb(); const r = db.prepare('SELECT COUNT(*) as total FROM cuentas_bancarias').get(); res.json({ count: r?.total || 0 }); }
    catch { res.json({ count: 0 }); }
  }
});

// ── Contribuyentes ────────────────────────────────────────────────────────

router.get('/contribuyentes', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), async (req, res) => {
  try {
    const { data } = await supabase.from('tributos_contribuyentes').select('*').order('fecha_alta_tributos', { ascending: false }).limit(200);
    res.json(data || []);
  } catch {
    res.json([]);
  }
});

export default router;
