import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import crypto from 'crypto';

const router = Router();

// ── MÓDULO 3: FISCAL, CONTABLE Y REGULATORIO ───────────────────────────────

// Demonio: Monitoreo de Descubiertos (se ejecuta cada 24h vía cron o endpoint manual)
router.post('/ejecutar-monitoreo', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), (req, res) => {
  const db = getDb();
  const resultados = [];
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

  // Buscar cuentas en negativo
  const cuentasNegativas = db.prepare(`
    SELECT c.*, s.alias, s.dip FROM cuentas_bancarias c
    JOIN solicitantes s ON s.id = c.usuario_id
    WHERE c.saldo < 0 AND c.estado = 'activa'
  `).all();

  for (const cuenta of cuentasNegativas) {
    // Calcular días en negativo
    const diasNegativos = db.prepare(`
      SELECT COUNT(DISTINCT fecha) as dias FROM historial_saldos_diarios
      WHERE cuenta_id = ? AND saldo < 0
    `).get(cuenta.id);

    const dias = diasNegativos?.dias || 1;

    if (dias >= 1 && dias <= 5) {
      // Periodo de gracia - solo marcar alerta
      db.prepare("UPDATE cuentas_bancarias SET estado = 'activa' WHERE id = ? AND estado = 'activa'").run(cuenta.id);
      // Registrar estado de alerta en notificación
      resultados.push({ alias: cuenta.alias, dip: cuenta.dip, dias_negativo: dias, accion: 'Periodo de gracia (días 1-5)' });

    } else if (dias >= 6 && dias < 30) {
      // Día 6+: Sanción de 25.000 Pz y estado Moroso_Nivel_1
      const sancion = 25000;
      
      const aplicarSancion = db.transaction(() => {
        // Aplicar multa
        const nuevoSaldo = cuenta.saldo - sancion;
        db.prepare('UPDATE cuentas_bancarias SET saldo = ?, estado = ? WHERE id = ?').run(nuevoSaldo, 'activa', cuenta.id);
        
        // Registrar multa
        db.prepare('INSERT INTO multas_automaticas (usuario_id, tipo, dias_en_negativo, importe, motivo, cuenta_id) VALUES (?, ?, ?, ?, ?, ?)')
          .run(cuenta.usuario_id, 'descubierto', dias, sancion, `Sanción por descubierto: día ${dias}`, cuenta.id);
        
        // Registrar transacción
        db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
          .run(cuenta.id, 'multa', sancion, `Sanción automática por descubierto (día ${dias}) - 25.000 Pz`);
      });
      aplicarSancion();
      
      // Generar PDF de requerimiento (GDLP-HAC-010)
      generarRequerimientoDescubierto(db, cuenta, sancion, dias);
      
      resultados.push({ alias: cuenta.alias, dip: cuenta.dip, dias_negativo: dias, accion: `Sanción de ${sancion} Pz aplicada`, sancion });
    }
  }

  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'monitoreo_descubiertos', `Monitoreo ejecutado. ${resultados.length} cuentas en negativo procesadas.`, ip);

  res.json({ success: true, message: 'Monitoreo completado', procesados: resultados.length, detalles: resultados });
});

// POST /api/fiscal/ejecutar-irm - [Ejecutar Cierre Fiscal Mensual] (IRM)
router.post('/ejecutar-irm', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), (req, res) => {
  const db = getDb();
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  const resultados = [];

  // Obtener todas las cuentas activas de tipo no institucional
  const cuentas = db.prepare(`
    SELECT c.*, s.alias, s.dip, s.franja_edad FROM cuentas_bancarias c
    JOIN solicitantes s ON s.id = c.usuario_id
    WHERE c.estado = 'activa' AND c.tipo_cuenta NOT IN ('Tesoro', 'Administracion')
  `).all();

  const mesAnterior = new Date();
  mesAnterior.setMonth(mesAnterior.getMonth() - 1);
  const periodo = `${mesAnterior.getFullYear()}-${String(mesAnterior.getMonth() + 1).padStart(2, '0')}`;
  const primerDia = `${periodo}-01`;
  const ultimoDia = new Date(mesAnterior.getFullYear(), mesAnterior.getMonth() + 1, 0).toISOString().split('T')[0];

  for (const cuenta of cuentas) {
    // Obtener historial de saldos diarios del mes anterior
    const saldos = db.prepare(`
      SELECT saldo FROM historial_saldos_diarios
      WHERE cuenta_id = ? AND fecha >= ? AND fecha <= ?
      ORDER BY fecha
    `).all(cuenta.id, primerDia, ultimoDia);

    if (saldos.length === 0) continue;

    // Calcular Patrimonio Medio (PM)
    const sumaSaldos = saldos.reduce((sum, s) => sum + s.saldo, 0);
    const pm = sumaSaldos / saldos.length;

    // Calcular Índice de Acumulación (IA)
    const saldoActual = cuenta.saldo;
    const ia = saldoActual / (pm || 1);

    // Determinar si aplica IRM
    const saldoMaximo = cuenta.saldo_maximo || 500000;
    if (pm <= saldoMaximo) {
      resultados.push({ alias: cuenta.alias, dip: cuenta.dip, pm, ia, irm: 0, accion: 'Dentro de límites' });
      continue;
    }

    // Calcular tasa IRM (progresiva)
    let tasa = 0;
    const exceso = pm - saldoMaximo;
    const ratio = exceso / saldoMaximo;

    if (ratio <= 0.5) tasa = 0.05;       // 5% si excede hasta 50%
    else if (ratio <= 1.0) tasa = 0.10;  // 10% si excede entre 50-100%
    else if (ratio <= 2.0) tasa = 0.20;  // 20% si excede entre 100-200%
    else tasa = 0.35;                     // 35% si excede más de 200%

    const importeIRM = exceso * tasa;

    const aplicarIRM = db.transaction(() => {
      // Aplicar descuento
      const nuevoSaldo = Math.max(0, cuenta.saldo - importeIRM);
      db.prepare('UPDATE cuentas_bancarias SET saldo = ? WHERE id = ?').run(nuevoSaldo, cuenta.id);

      // Quemar el IRM (enviar a dirección nula)
      db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
        .run(cuenta.id, 'quema', importeIRM, `IRM ${periodo} - Quema por exceso de capital. PM: ${pm.toFixed(2)} Pz`);

      // Registrar IRM
      db.prepare('INSERT INTO impuestos_irm (usuario_id, periodo, patrimonio_medio, indice_acumulacion, tasa_aplicada, importe_irm, saldo_quemado) VALUES (?, ?, ?, ?, ?, ?, ?)')
        .run(cuenta.usuario_id, periodo, pm, ia, tasa, importeIRM, importeIRM);

      // Registrar multa por exceso
      db.prepare('INSERT INTO multas_automaticas (usuario_id, tipo, importe, motivo, cuenta_id) VALUES (?, ?, ?, ?, ?)')
        .run(cuenta.usuario_id, 'irm', importeIRM, `IRM - Exceso de capital: ${exceso.toFixed(2)} Pz sobre límite de ${saldoMaximo} Pz`, cuenta.id);
    });
    aplicarIRM();

    resultados.push({
      alias: cuenta.alias, dip: cuenta.dip,
      patrimonio_medio: pm,
      indice_acumulacion: ia,
      tasa: `${(tasa * 100).toFixed(0)}%`,
      irm: importeIRM,
      exceso,
      accion: 'IRM aplicado y quemado'
    });
  }

  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'cierre_fiscal_irm', `IRM ejecutado para periodo ${periodo}. ${resultados.length} cuentas procesadas.`, ip);

  res.json({
    success: true,
    message: `Cierre fiscal IRM ejecutado para periodo ${periodo}`,
    periodo,
    cuentas_procesadas: resultados.length,
    afectados_por_irm: resultados.filter(r => r.irm > 0).length,
    detalles: resultados
  });
});

// POST /api/fiscal/reclamar-rbu - Reclamar Renta Básica Universal (semanal)
router.post('/reclamar-rbu', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;

  // Verificar mayoría de edad
  const usuario = db.prepare('SELECT franja_edad FROM solicitantes WHERE id = ?').get(usuarioId);
  if (!usuario || usuario.franja_edad !== 'Alta_Plena') {
    return res.status(403).json({ error: 'La RBU solo está disponible para usuarios con Alta Plena (≥18 años).' });
  }

  // Verificar que no haya reclamado esta semana
  const semana = getSemanaActual();
  const existente = db.prepare('SELECT id FROM renta_basica_universal WHERE usuario_id = ? AND semana = ?').get(usuarioId, semana);
  if (existente) return res.status(400).json({ error: 'Ya has reclamado la RBU esta semana.' });

  // Aplicar RBU (5 Pz no acumulable, inembargable)
  const cuenta = db.prepare('SELECT id FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(usuarioId, 'activa');
  if (!cuenta) return res.status(400).json({ error: 'Cuenta bancaria activa requerida' });

  db.transaction(() => {
    db.prepare('UPDATE cuentas_bancarias SET saldo = saldo + 5 WHERE id = ?').run(cuenta.id);
    db.prepare('INSERT INTO renta_basica_universal (usuario_id, semana, importe, reclamado, pagado) VALUES (?, ?, ?, 1, 1)').run(usuarioId, semana, 5);
    db.prepare('INSERT INTO transacciones (cuenta_destino_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)').run(cuenta.id, 'bono', 5, 'RBU - Renta Básica Universal');
  })();

  res.json({ success: true, message: 'RBU de 5 Pz reclamada con éxito.', semana, importe: 5 });
});

// POST /api/fiscal/registrar-saldo-diario - Registrar saldo diario (cronjob)
router.post('/registrar-saldo-diario', verificarSesion, verificarRol('administrador', 'junta', 'fiscal'), (req, res) => {
  const db = getDb();
  const hoy = new Date().toISOString().split('T')[0];
  const cuentas = db.prepare('SELECT id, saldo FROM cuentas_bancarias WHERE estado = ?').all('activa');

  const insert = db.prepare('INSERT OR IGNORE INTO historial_saldos_diarios (cuenta_id, fecha, saldo) VALUES (?, ?, ?)');
  for (const cuenta of cuentas) {
    insert.run(cuenta.id, hoy, cuenta.saldo);
  }

  res.json({ success: true, message: `Saldos registrados para ${cuentas.length} cuentas`, fecha: hoy });
});

// GET /api/fiscal/impuestos-irm/:usuario_id - Historial IRM de un usuario
router.get('/impuestos-irm/:usuario_id', verificarSesion, (req, res) => {
  const db = getDb();
  const irm = db.prepare('SELECT * FROM impuestos_irm WHERE usuario_id = ? ORDER BY periodo DESC').all(req.params.usuario_id);
  res.json(irm);
});

// ── Helpers ──────────────────────────────────────────────────────────────────
function getSemanaActual() {
  const ahora = new Date();
  const inicio = new Date(ahora.getFullYear(), 0, 1);
  const diff = (ahora - inicio + (inicio.getTimezoneOffset() - ahora.getTimezoneOffset()) * 60000) / 86400000;
  const semana = Math.ceil((diff + inicio.getDay() + 1) / 7);
  return `${ahora.getFullYear()}-W${String(semana).padStart(2, '0')}`;
}

function generarRequerimientoDescubierto(db, cuenta, sancion, dias) {
  try {
    const codigo = `GDLP-HAC-010-${cuenta.usuario_id}-${Date.now()}`;
    db.prepare(`
      INSERT INTO documentos_firmados (codigo_modelo, titulo_documento, contenido_hash, estado, url_firma)
      VALUES (?, ?, ?, ?, ?)
    `).run(
      'GDLP-HAC-010',
      'Requerimiento de Regularización por Descubierto Bancario de Cuenta',
      crypto.createHash('sha256').update(`${cuenta.alias}-${sancion}-${dias}`).digest('hex'),
      'pendiente',
      crypto.randomBytes(16).toString('hex')
    );
  } catch (e) {
    console.error('Error generando requerimiento:', e.message);
  }
}

export default router;
