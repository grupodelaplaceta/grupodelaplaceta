import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { supabase } from '../config/supabase.js';

const router = Router();

// ── PANEL DE ADMINISTRACIÓN ─────────────────────────────────────────────────

// GET /api/admin/dashboard - Estadísticas del dashboard
router.get('/dashboard', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();

  const stats = {
    usuarios: {
      total: db.prepare('SELECT COUNT(*) as total FROM solicitantes').get().total,
      activos: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='activo'").get().total,
      pendientes: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='pendiente'").get().total,
      suspendidos: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='suspendido'").get().total,
      expulsados: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='expulsado'").get().total,
      baja: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE estado='baja'").get().total,
      porFranja: {
        tuteladaBasica: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Tutelada_Basica'").get().total,
        tuteladaSenior: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Tutelada_Senior'").get().total,
        altaPlena: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Alta_Plena'").get().total,
        institucional: db.prepare("SELECT COUNT(*) as total FROM solicitantes WHERE franja_edad='Institucional'").get().total
      }
    },
    bancario: {
      totalCuentas: db.prepare('SELECT COUNT(*) as total FROM cuentas_bancarias').get().total,
      masaMonetaria: db.prepare("SELECT COALESCE(SUM(saldo),0) as total FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')").get().total,
      tesoro: db.prepare("SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Tesoro'").get().total,
      admin: db.prepare("SELECT COALESCE(saldo,0) as total FROM cuentas_bancarias WHERE tipo_cuenta='Administracion'").get().total,
      cuentasNegativas: db.prepare("SELECT COUNT(*) as total FROM cuentas_bancarias WHERE saldo < 0 AND estado='activa'").get().total
    },
    justicia: {
      expedientesAbiertos: db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')").get().total,
      expedientesInstruccion: db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='instruccion'").get().total,
      expedientesAlegaciones: db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='alegaciones'").get().total,
      expedientesResolucion: db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado='resolucion'").get().total,
      denunciasPendientes: db.prepare("SELECT COUNT(*) as total FROM denuncias WHERE estado='pendiente'").get().total,
      totalSancionesAplicadas: db.prepare("SELECT COUNT(*) as total FROM multas_automaticas WHERE estado='pagada'").get().total
    },
    fiscal: {
      ultimoIRM: db.prepare('SELECT periodo, COUNT(*) as usuarios, SUM(importe_irm) as total_irm FROM impuestos_irm GROUP BY periodo ORDER BY periodo DESC LIMIT 1').get(),
      multasPendientes: db.prepare("SELECT COUNT(*) as total FROM multas_automaticas WHERE estado='pendiente'").get().total,
      rbuReclamadas: db.prepare('SELECT COUNT(*) as total FROM renta_basica_universal WHERE pagado=1').get().total,
      rbuSemanaActual: db.prepare("SELECT COUNT(*) as total FROM renta_basica_universal WHERE semana = (SELECT semana FROM renta_basica_universal ORDER BY semana DESC LIMIT 1) AND pagado=1").get().total
    },
    rgpd: {
      solicitudesARCO: {
        total: db.prepare('SELECT COUNT(*) as total FROM solicitudes_arco').get().total,
        pendientes: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE estado='pendiente'").get().total,
        completadas: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE estado='completado'").get().total,
        porDerecho: {
          acceso: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='acceso'").get().total,
          rectificacion: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='rectificacion'").get().total,
          supresion: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='supresion'").get().total,
          oposicion: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='oposicion'").get().total,
          limitacion: db.prepare("SELECT COUNT(*) as total FROM solicitudes_arco WHERE derecho='limitacion'").get().total
        }
      },
      consentimientos: db.prepare('SELECT COUNT(*) as total FROM consentimientos').get().total
    },
    documentos: {
      total: db.prepare('SELECT COUNT(*) as total FROM documentos_firmados').get().total,
      pendientes: db.prepare("SELECT COUNT(*) as total FROM documentos_firmados WHERE estado='pendiente'").get().total,
      firmados: db.prepare("SELECT COUNT(*) as total FROM documentos_firmados WHERE estado='firmado'").get().total
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
  
  const reporte = {
    generado_en: new Date().toISOString(),
    resumen: {
      total_usuarios: db.prepare('SELECT COUNT(*) as t FROM solicitantes').get().t,
      usuarios_activos: db.prepare("SELECT COUNT(*) as t FROM solicitantes WHERE estado='activo'").get().t,
      masa_monetaria_circulante: db.prepare("SELECT COALESCE(SUM(saldo),0) as t FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')").get().t,
      expedientes_activos: db.prepare("SELECT COUNT(*) as t FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')").get().t,
      solicitudes_rgpd_pendientes: db.prepare("SELECT COUNT(*) as t FROM solicitudes_arco WHERE estado='pendiente'").get().t
    },
    usuarios_por_franja: db.prepare("SELECT franja_edad, COUNT(*) as total FROM solicitantes GROUP BY franja_edad").all(),
    cuentas_por_tipo: db.prepare("SELECT tipo_cuenta, COUNT(*) as total, COALESCE(SUM(saldo),0) as saldo_total FROM cuentas_bancarias GROUP BY tipo_cuenta").all(),
    expedientes_por_estado: db.prepare("SELECT estado, COUNT(*) as total FROM expedientes_disciplinarios GROUP BY estado").all(),
    ultimos_logs: db.prepare(`
      SELECT l.accion, l.detalle, l.creado_en, s.alias 
      FROM logs_auditoria l LEFT JOIN solicitantes s ON s.id=l.usuario_id 
      ORDER BY l.creado_en DESC LIMIT 50
    `).all()
  };

  res.json(reporte);
});

// GET /api/admin/ciudadanos - Lista completa de ciudadanos desde Supabase
router.get('/ciudadanos', verificarSesion, verificarRol('administrador', 'junta'), async (req, res) => {
  try {
    // Primero Supabase
    const { data: solicitantes, error } = await supabase
      .from('solicitantes')
      .select('*')
      .order('creado_en', { ascending: false })
      .limit(500);
    if (!error && solicitantes) return res.json(solicitantes);
  } catch (_) {}
  // Fallback SQLite
  try {
    const db = getDb();
    const ciudadanos = db.prepare('SELECT * FROM solicitantes ORDER BY creado_en DESC').all();
    return res.json(ciudadanos || []);
  } catch (_) {}
  res.json([]);
});

export default router;
