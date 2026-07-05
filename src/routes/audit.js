import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { getBankState, getBankCollection } from '../services/bankApi.js';
import { generarToken } from '../middleware/auth.js';

const router = Router();

// Helper: intenta API banco, fallback a SQLite
async function conBanco(req, fnBanco, fnSqlite) {
  const token = req.session?.usuario?.placetaid_token || req.headers.authorization?.replace('Bearer ', '');
  if (token) {
    try {
      const res = await fnBanco(token);
      if (res.status === 200) return res.data;
    } catch {}
  }
  return fnSqlite();
}

// GET /api/admin/audit/bancario/resumen — Resumen bancario completo
router.get('/bancario/resumen', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    // Intentar desde API banco primero
    const token = req.session?.usuario?.placetaid_token;
    if (token) {
      try {
        const state = await getBankState(token);
        if (state.status === 200 && state.data) {
          const d = state.data;
          return res.json({
            cuentas: d.accounts?.length || 0,
            activas: d.accounts?.filter(a => a.estado === 'activa')?.length || 0,
            negativas: d.accounts?.filter(a => (a.saldo || 0) < 0)?.length || 0,
            masaMonetaria: d.accounts?.reduce((s, a) => s + (a.saldo || 0), 0) || 0,
            totalTransacciones: d.transactions?.length || 0,
            volumenTransacciones: d.transactions?.reduce((s, t) => s + Math.abs(t.cantidad || 0), 0) || 0,
            fuente: 'api_banco'
          });
        }
      } catch {}
    }
    // Fallback SQLite
    const db = getDb();
    const resumen = {
      cuentas: db.prepare('SELECT COUNT(*) as total FROM cuentas_bancarias').get().total,
      activas: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE estado='activa'").get().total,
      bloqueadas: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE estado='bloqueada'").get().total,
      negativas: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE saldo < 0 AND estado='activa'").get().total,
      masaMonetaria: db.prepare("SELECT COALESCE(SUM(saldo),0) as total FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')").get().total,
      tesoro: db.prepare("SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Tesoro'").get().total,
      adminFondo: db.prepare("SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Administracion'").get().total,
      totalTransacciones: db.prepare("SELECT COUNT(*) as total FROM transacciones WHERE creado_en >= datetime('now', '-30 days')").get().total,
      volumenTransacciones: db.prepare("SELECT COALESCE(SUM(cantidad),0) as total FROM transacciones WHERE creado_en >= datetime('now', '-30 days') AND estado='completada'").get().total,
      bonoPendientes: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE bono_bienvenida_activo=1").get().total,
      porTipo: {
        juniorBasica: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE tipo_cuenta='Junior_Basica'").get().total,
        juniorSenior: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE tipo_cuenta='Junior_Senior'").get().total,
        ciudadanaPlena: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE tipo_cuenta='Ciudadana_Plena'").get().total,
        institucional: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE tipo_cuenta='Institucional'").get().total
      }
    };
    res.json(resumen);
  } catch (err) {
    console.error('[Audit-Bancario] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/audit/bancario/transacciones — Últimas transacciones
router.get('/bancario/transacciones', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const limit = Math.min(parseInt(req.query.limit) || 100, 500);
    const transactions = db.prepare(`
      SELECT t.*, c1.alias as alias_origen, c2.alias as alias_destino
      FROM transacciones t
      LEFT JOIN cuentas_bancarias c1 ON t.cuenta_origen_id = c1.id
      LEFT JOIN cuentas_bancarias c2 ON t.cuenta_destino_id = c2.id
      ORDER BY t.creado_en DESC LIMIT ?
    `).all(limit);
    res.json(transactions);
  } catch (err) {
    console.error('[Audit-Bancario] Error transacciones:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/audit/bancario/cuentas-negativas — Cuentas en descubierto
router.get('/bancario/cuentas-negativas', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), (req, res) => {
  try {
    const db = getDb();
    const cuentas = db.prepare(`
      SELECT c.*, s.alias, s.dip, s.estado as estado_usuario
      FROM cuentas_bancarias c
      JOIN solicitantes s ON c.usuario_id = s.id
      WHERE c.saldo < 0 AND c.estado = 'activa'
      ORDER BY c.saldo ASC
    `).all();
    res.json(cuentas);
  } catch (err) {
    console.error('[Audit-Bancario] Error cuentas negativas:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/audit/bancario/saldos-diarios — Historial saldos
router.get('/bancario/saldos-diarios', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const days = Math.min(parseInt(req.query.days) || 31, 365);
    const saldos = db.prepare(`
      SELECT fecha, SUM(saldo) as saldo_total,
        SUM(CASE WHEN saldo < 0 THEN 1 ELSE 0 END) as cuentas_negativas
      FROM historial_saldos_diarios
      WHERE fecha >= datetime('now', ?)
      GROUP BY fecha ORDER BY fecha DESC
    `).all(`-${days} days`);
    res.json(saldos);
  } catch (err) {
    console.error('[Audit-Bancario] Error saldos diarios:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// GET /api/admin/audit/bancario/historial/:cuentaId — Historial de una cuenta
router.get('/bancario/historial/:cuentaId', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const cuenta = db.prepare(`
      SELECT c.*, s.alias, s.dip, s.nombre_real, s.rol
      FROM cuentas_bancarias c
      JOIN solicitantes s ON c.usuario_id = s.id
      WHERE c.id = ?
    `).get(req.params.cuentaId);
    if (!cuenta) return res.status(404).json({ error: 'Cuenta no encontrada' });

    const movimientos = db.prepare(`
      SELECT * FROM transacciones
      WHERE cuenta_origen_id = ? OR cuenta_destino_id = ?
      ORDER BY creado_en DESC LIMIT 200
    `).all(req.params.cuentaId, req.params.cuentaId);

    res.json({ cuenta, movimientos });
  } catch (err) {
    console.error('[Audit-Bancario] Error historial:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/audit/bancario/ajustar-saldo — Ajuste manual de saldo (admin)
router.post('/bancario/ajustar-saldo', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    const { cuentaId, cantidad, concepto } = req.body;
    if (!cuentaId || !cantidad) return res.status(400).json({ error: 'cuentaId y cantidad requeridos' });

    const cuenta = db.prepare('SELECT * FROM cuentas_bancarias WHERE id = ?').get(cuentaId);
    if (!cuenta) return res.status(404).json({ error: 'Cuenta no encontrada' });

    const nuevoSaldo = cuenta.saldo + cantidad;
    db.prepare('UPDATE cuentas_bancarias SET saldo = ?, actualizado_en = datetime() WHERE id = ?')
      .run(nuevoSaldo, cuentaId);

    db.prepare(`INSERT INTO transacciones (cuenta_origen_id, cuenta_destino_id, tipo, cantidad, concepto, estado, creado_en)
      VALUES (?, ?, ?, ?, ?, 'completada', datetime())`)
      .run(cantidad < 0 ? cuentaId : null, cantidad > 0 ? cuentaId : null,
        cantidad < 0 ? 'ajuste_debito' : 'ajuste_credito', Math.abs(cantidad),
        concepto || `Ajuste administrativo (${req.session.usuario.alias})`);

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
      .run(req.session.usuario.id, 'ajuste_saldo',
        `Ajuste de ${cantidad} Pz en cuenta #${cuentaId}. Concepto: ${concepto || 'Ajuste admin'}. Saldo anterior: ${cuenta.saldo}. Nuevo saldo: ${nuevoSaldo}`, ip);

    res.json({ success: true, nuevoSaldo });
  } catch (err) {
    console.error('[Audit-Bancario] Error ajuste:', err.message);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/audit/bancario/bloquear/:cuentaId
router.post('/bancario/bloquear/:cuentaId', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    db.prepare("UPDATE cuentas_bancarias SET estado='bloqueada', actualizado_en=datetime() WHERE id=?")
      .run(req.params.cuentaId);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/admin/audit/bancario/desbloquear/:cuentaId
router.post('/bancario/desbloquear/:cuentaId', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  try {
    const db = getDb();
    db.prepare("UPDATE cuentas_bancarias SET estado='activa', actualizado_en=datetime() WHERE id=?")
      .run(req.params.cuentaId);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
