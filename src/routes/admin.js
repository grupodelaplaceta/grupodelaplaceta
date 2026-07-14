import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { supabase } from '../config/supabase.js';

const router = Router();

// Helper seguro para consultas SQLite que pueden fallar (tabla no existe)
function safeGet(db, sql, ...params) {
  try {
    const row = db.prepare(sql).get(...params);
    return row || {};
  } catch (e) {
    return {};
  }
}

function safeGetTotal(db, sql, ...params) {
  return safeGet(db, sql, ...params).total || 0;
}

// ── PANEL DE ADMINISTRACIÓN ─────────────────────────────────────────────────

// GET /api/admin/dashboard - Estadísticas del dashboard
router.get('/dashboard', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();

  const stats = {
    usuarios: {
      total: safeGetTotal(db, 'SELECT COUNT(*) as total FROM solicitantes'),
      activos: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE estado='activo'"),
      pendientes: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE estado='pendiente'"),
      suspendidos: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE estado='suspendido'"),
      expulsados: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE estado='expulsado'"),
      baja: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE estado='baja'"),
      porFranja: {
        tuteladaBasica: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Tutelada_Basica'"),
        tuteladaSenior: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Tutelada_Senior'"),
        altaPlena: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Alta_Plena'"),
        institucional: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Institucional'")
      }
    },
    bancario: {
      totalCuentas: safeGetTotal(db, 'SELECT COUNT(*) as total FROM cuentas_bancarias'),
      masaMonetaria: safeGetTotal(db, "SELECT COALESCE(SUM(saldo),0) as total FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')"),
      tesoro: safeGetTotal(db, "SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Tesoro'"),
      admin: safeGetTotal(db, "SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Administracion'"),
      cuentasNegativas: safeGetTotal(db, "SELECT COUNT(*) as total FROM cuentas_bancarias WHERE saldo < 0 AND estado='activa'")
    },
    justicia: {
      expedientesAbiertos: safeGetTotal(db, "SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')"),
      expedientesInstruccion: safeGetTotal(db, "SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='instruccion'"),
      expedientesAlegaciones: safeGetTotal(db, "SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='alegaciones'"),
      expedientesResolucion: safeGetTotal(db, "SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='resolucion'"),
      denunciasPendientes: safeGetTotal(db, "SELECT COUNT(*) as total FROM denuncias WHERE estado='pendiente'"),
      totalSancionesAplicadas: safeGetTotal(db, "SELECT COUNT(*) as total FROM multas_automaticas WHERE estado='pagada'")
    },
    fiscal: {
      ultimoIRM: safeGet(db, 'SELECT periodo, COUNT(*) as usuarios, SUM(importe_irm) as total_irm FROM impuestos_irm GROUP BY periodo ORDER BY periodo DESC LIMIT 1'),
      multasPendientes: safeGetTotal(db, "SELECT COUNT(*) as total FROM multas_automaticas WHERE estado='pendiente'"),
      rbuReclamadas: safeGetTotal(db, 'SELECT COUNT(*) as total FROM renta_basica_universal WHERE pagado=1'),
      rbuSemanaActual: safeGetTotal(db, "SELECT COUNT(*) as total FROM renta_basica_universal WHERE semana = (SELECT semana FROM renta_basica_universal ORDER BY semana DESC LIMIT 1) AND pagado=1")
    },
    rgpd: {
      solicitudesARCO: {
        total: safeGetTotal(db, 'SELECT COUNT(*) as total FROM solicitudes_arco'),
        pendientes: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE estado='pendiente'"),
        completadas: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE estado='completado'"),
        porDerecho: {
          acceso: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='acceso'"),
          rectificacion: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='rectificacion'"),
          supresion: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='supresion'"),
          oposicion: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='oposicion'"),
          limitacion: safeGetTotal(db, "SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='limitacion'")
        }
      },
      consentimientos: safeGetTotal(db, 'SELECT COUNT(*) as total FROM consentimientos')
    },
    documentos: {
      total: safeGetTotal(db, 'SELECT COUNT(*) as total FROM documentos_firmados'),
      pendientes: safeGetTotal(db, "SELECT COUNT(*) as total FROM documentos_firmados WHERE estado='pendiente'"),
      firmados: safeGetTotal(db, "SELECT COUNT(*) as total FROM documentos_firmados WHERE estado='firmado'")
    }
  };

  res.json(stats);
});

// GET /api/admin/logs - Logs de auditoría
router.get('/logs', verificarSesion, verificarRol('administrador'), (req, res) => {
  const db = getDb();
  const { accion, limite = 100 } = req.query;
  let query = `
    SELECT l.*, s.alias 
    FROM logs_auditoria l
    LEFT JOIN solicitantes s ON s.id = l.usuario_id
    WHERE 1=1
  `;
  const params = [];
  if (accion) { query += ' AND l.accion = ?'; params.push(accion); }
  query += ' ORDER BY l.creado_en DESC LIMIT ?';
  params.push(parseInt(limite));

  res.json(db.prepare(query).all(...params));
});

// GET /api/admin/reporte-completo - Reporte completo exportable
router.get('/reporte-completo', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  
  const sg = (sql) => { try { return db.prepare(sql).get() || {}; } catch { return {}; } };
  const sa = (sql) => { try { return db.prepare(sql).all(); } catch { return []; } };
  const sgt = (sql) => sg(sql).t || 0;

  const reporte = {
    generado_en: new Date().toISOString(),
    resumen: {
      total_usuarios: sgt('SELECT COUNT(*) as t FROM solicitantes'),
      usuarios_activos: sgt("SELECT COUNT(*) as t FROM solicitantes WHERE estado='activo'"),
      masa_monetaria_circulante: sgt("SELECT COALESCE(SUM(saldo),0) as t FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')"),
      expedientes_activos: sgt("SELECT COUNT(*) as t FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')"),
      solicitudes_rgpd_pendientes: sgt("SELECT COUNT(*) as t FROM solicitudes_arco WHERE estado='pendiente'")
    },
    usuarios_por_franja: sa("SELECT franja_edad, COUNT(*) as total FROM solicitantes GROUP BY franja_edad"),
    cuentas_por_tipo: sa("SELECT tipo_cuenta, COUNT(*) as total, COALESCE(SUM(saldo),0) as saldo_total FROM cuentas_bancarias GROUP BY tipo_cuenta"),
    expedientes_por_estado: sa("SELECT estado, COUNT(*) as total FROM expedientes_disciplinarios GROUP BY estado"),
    ultimos_logs: sa(`
      SELECT l.accion, l.detalle, l.creado_en, s.alias 
      FROM logs_auditoria l LEFT JOIN solicitantes s ON s.id=l.usuario_id 
      ORDER BY l.creado_en DESC LIMIT 50
    `)
  };

  res.json(reporte);
});

// GET /api/admin/ciudadanos - Lista completa de ciudadanos desde Supabase
router.get('/ciudadanos', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    const { data: solicitantes, error } = await supabase
      .from('solicitantes')
      .select('*')
      .order('creado_en', { ascending: false })
      .limit(500);
    if (!error && solicitantes) {
      // Filtrar DIPs demo y placeid demo
      const filtrados = solicitantes.filter(s =>
        !s.dip?.toLowerCase().includes('demo') &&
        !s.alias?.toLowerCase().includes('demo') &&
        !s.nombre_real?.toLowerCase().includes('demo') &&
        !s.email?.toLowerCase().includes('demo')
      );
      return res.json(filtrados);
    }
  } catch (_) {}
  try {
    const db = getDb();
    const ciudadanos = db.prepare('SELECT * FROM solicitantes ORDER BY creado_en DESC').all();
    const filtrados = (ciudadanos || []).filter(s =>
      !s.dip?.toLowerCase().includes('demo') &&
      !s.alias?.toLowerCase().includes('demo') &&
      !s.nombre_real?.toLowerCase().includes('demo')
    );
    return res.json(filtrados);
  } catch (_) {}
  res.json([]);
});

// DELETE /api/admin/ciudadanos/:dip (con purge completo)
router.delete('/ciudadanos/:dip', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const { dip } = req.params;
    const { error } = await supabase.from('solicitantes').delete().eq('dip', dip);
    if (error) throw new Error(error.message);
    await supabase.from('junior_menores').delete().eq('dip', dip);
    await supabase.from('tributos_contribuyentes').delete().eq('dip', dip);
    res.json({ success: true, message: `DIP ${dip} eliminado de Supabase` });
  } catch (err) {
    try {
      const db = getDb();
      db.prepare('DELETE FROM solicitantes WHERE dip = ?').run(req.params.dip);
      db.prepare('DELETE FROM junior_menores WHERE dip = ?').run(req.params.dip);
      db.prepare('DELETE FROM cuentas_bancarias WHERE placeta_id = ?').run(req.params.dip);
      res.json({ success: true, message: `DIP ${req.params.dip} eliminado de SQLite` });
    } catch (_) { res.status(500).json({ error: err.message }); }
  }
});

export default router;
