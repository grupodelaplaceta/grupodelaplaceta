import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';

const router = Router();

// ── MÓDULO 2: CORE BANCARIO Y GESTIÓN DE DIVISA ────────────────────────────

// POST /api/bancario/transferencia - Realizar transferencia entre cuentas
router.post('/transferencia', verificarSesion, (req, res) => {
  const db = getDb();
  const { destino_dip, cantidad, concepto } = req.body;

  if (!destino_dip || !cantidad || cantidad <= 0) {
    return res.status(400).json({ error: 'Destino y cantidad válida requeridos' });
  }

  const usuarioId = req.session.usuario.id;

  // Obtener cuenta origen
  const cuentaOrigen = db.prepare('SELECT * FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(usuarioId, 'activa');
  if (!cuentaOrigen) return res.status(400).json({ error: 'Cuenta origen no encontrada o no activa' });

  // Verificar si está en mora (infracción grave bloquea transferencias)
  const infractor = db.prepare("SELECT estado FROM expedientes_disciplinarios WHERE infractor_id = ? AND estado IN ('instruccion','alegaciones','resolucion','apelacion') AND sancion_expulsion = 1").get(usuarioId);
  if (infractor) return res.status(403).json({ error: 'Transferencias bloqueadas: tiene un expediente disciplinario activo por infracción grave.' });

  // Verificar límite diario de transferencias
  const hoy = new Date().toISOString().split('T')[0];
  const enviadoHoy = db.prepare(`
    SELECT COALESCE(SUM(cantidad), 0) as total FROM transacciones
    WHERE cuenta_origen_id = ? AND tipo = 'transferencia' AND date(creado_en) = ?
  `).get(cuentaOrigen.id, hoy);

  if (enviadoHoy.total + Number(cantidad) > cuentaOrigen.limite_transferencia_diario) {
    return res.status(400).json({ error: `Límite diario de transferencia excedido (${cuentaOrigen.limite_transferencia_diario} Pz)` });
  }

  // Verificar bono de bienvenida (bloqueo 30 días)
  if (cuentaOrigen.bono_bienvenida_activo) {
    const diasDesdeBono = Math.floor((Date.now() - new Date(cuentaOrigen.fecha_bono).getTime()) / (1000 * 60 * 60 * 24));
    if (diasDesdeBono < 30) {
      return res.status(400).json({ error: `Bono de bienvenida activo. Las transferencias estarán bloqueadas durante 30 días desde la activación. Quedan ${30 - diasDesdeBono} días.` });
    }
    // Desactivar bono después de 30 días
    db.prepare('UPDATE cuentas_bancarias SET bono_bienvenida_activo = 0 WHERE id = ?').run(cuentaOrigen.id);
  }

  // Obtener cuenta destino
  const usuarioDestino = db.prepare('SELECT id FROM solicitantes WHERE dip = ? AND estado = ?').get(destino_dip, 'activo');
  if (!usuarioDestino) return res.status(404).json({ error: 'Usuario destino no encontrado o inactivo' });

  const cuentaDestino = db.prepare('SELECT * FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(usuarioDestino.id, 'activa');
  if (!cuentaDestino) return res.status(400).json({ error: 'Cuenta destino no encontrada o no activa' });

  // Verificar saldo suficiente
  if (cuentaOrigen.saldo < Number(cantidad)) {
    return res.status(400).json({ error: 'Saldo insuficiente' });
  }

  // Aplicar retención máxima (hasta 12% según normativa)
  const retencion = Math.min(Number(cantidad) * 0.12, Number(cantidad) * 0.12);
  const cantidadNeta = Number(cantidad) - retencion;

  // Ejecutar transferencia
  const transfer = db.transaction(() => {
    // Debitar origen
    db.prepare('UPDATE cuentas_bancarias SET saldo = saldo - ?, actualizado_en = datetime(\'now\') WHERE id = ?')
      .run(Number(cantidad), cuentaOrigen.id);

    // Acreditar destino (neto)
    db.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ?, actualizado_en = datetime(\'now\') WHERE id = ?')
      .run(cantidadNeta, cuentaDestino.id);

    // Registrar transacción
    const result = db.prepare(`
      INSERT INTO transacciones (cuenta_origen_id, cuenta_destino_id, tipo, cantidad, concepto, retencion_aplicada, referencia_externa)
      VALUES (?, ?, 'transferencia', ?, ?, ?, ?)
    `).run(cuentaOrigen.id, cuentaDestino.id, Number(cantidad), concepto || 'Transferencia entre usuarios', retencion, `TXN-${Date.now()}`);

    // Registrar log
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle) VALUES (?, ?, ?)')
      .run(usuarioId, 'transferencia', `Transferencia de ${cantidad} Pz a DIP ${destino_dip}. Retención: ${retencion} Pz`);

    return result.lastInsertRowid;
  });

  const id = transfer();
  res.json({
    success: true,
    message: `Transferencia de ${cantidad} Pz realizada. Retención aplicada: ${retencion} Pz.`,
    id_transaccion: id,
    cantidad_enviada: Number(cantidad),
    cantidad_recibida: cantidadNeta,
    retencion
  });
});

// GET /api/bancario/cuenta - Obtener datos de la cuenta del usuario logueado
router.get('/cuenta', verificarSesion, (req, res) => {
  const db = getDb();
  const cuenta = db.prepare(`
    SELECT c.*, s.alias, s.dip, s.placeid, s.franja_edad
    FROM cuentas_bancarias c
    JOIN solicitantes s ON s.id = c.usuario_id
    WHERE c.usuario_id = ?
  `).get(req.session.usuario.id);

  if (!cuenta) return res.status(404).json({ error: 'Cuenta no encontrada' });
  res.json(cuenta);
});

// GET /api/bancario/movimientos - Historial de transacciones
router.get('/movimientos', verificarSesion, (req, res) => {
  const db = getDb();
  const cuenta = db.prepare('SELECT id FROM cuentas_bancarias WHERE usuario_id = ?').get(req.session.usuario.id);
  if (!cuenta) return res.status(404).json({ error: 'Cuenta no encontrada' });

  const movimientos = db.prepare(`
    SELECT t.*, 
      CASE WHEN t.cuenta_origen_id = ? THEN 'enviado' ELSE 'recibido' END as direccion,
      s_origen.alias as alias_origen,
      s_destino.alias as alias_destino
    FROM transacciones t
    LEFT JOIN cuentas_bancarias c_origen ON t.cuenta_origen_id = c_origen.id
    LEFT JOIN cuentas_bancarias c_destino ON t.cuenta_destino_id = c_destino.id
    LEFT JOIN solicitantes s_origen ON c_origen.usuario_id = s_origen.id
    LEFT JOIN solicitantes s_destino ON c_destino.usuario_id = s_destino.id
    WHERE t.cuenta_origen_id = ? OR t.cuenta_destino_id = ?
    ORDER BY t.creado_en DESC LIMIT 100
  `).all(cuenta.id, cuenta.id, cuenta.id);

  res.json(movimientos);
});

// POST /api/bancario/emision - [Emisión monetaria] (Exclusivo Junta/Departamento Económico)
router.post('/emision', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const { cantidad, destino_dip, motivo } = req.body;

  if (!cantidad || cantidad <= 0) return res.status(400).json({ error: 'Cantidad válida requerida' });

  const db2 = getDb();
  const destino = db2.prepare('SELECT id FROM solicitantes WHERE dip = ? AND estado = ?').get(destino_dip, 'activo');
  if (!destino) return res.status(404).json({ error: 'Destino no encontrado' });

  const cuentaDestino = db2.prepare('SELECT id FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(destino.id, 'activa');
  if (!cuentaDestino) return res.status(400).json({ error: 'Cuenta destino no activa' });

  // Split automático para emisiones > 7.500 Pz
  let cantidadReal = Number(cantidad);
  if (cantidadReal > 7500) {
    const tesoro = db2.prepare("SELECT id FROM cuentas_bancarias WHERE tipo_cuenta = 'Tesoro'").get();
    const admin = db2.prepare("SELECT id FROM cuentas_bancarias WHERE tipo_cuenta = 'Administracion'").get();

    const alTesoro = cantidadReal * 0.60;
    const aAdmin = cantidadReal * 0.20;
    const alDestino = cantidadReal * 0.20;

    const split = db.transaction(() => {
      if (tesoro) db2.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ? WHERE id = ?').run(alTesoro, tesoro.id);
      if (admin) db2.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ? WHERE id = ?').run(aAdmin, admin.id);
      db2.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ? WHERE id = ?').run(alDestino, cuentaDestino.id);

      db2.prepare('INSERT INTO transacciones (cuenta_destino_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)').run(cuentaDestino.id, 'split', alDestino, `Emisión excepcional (split): ${motivo || 'Sin motivo'}`);
    });
    split();

    return res.json({
      success: true,
      message: `Emisión de ${cantidadReal} Pz con split automático aplicado.`,
      detalle: {
        total: cantidadReal,
        al_tesoro: `${alTesoro} Pz (60%)`,
        a_administracion: `${aAdmin} Pz (20%)`,
        al_destino: `${alDestino} Pz (20%)`
      }
    });
  }

  // Emisión normal
  db.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ? WHERE id = ?').run(cantidadReal, cuentaDestino.id);
  db.prepare('INSERT INTO transacciones (cuenta_destino_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
    .run(cuentaDestino.id, 'split', cantidadReal, `Emisión monetaria: ${motivo || 'Sin motivo'}`);

  res.json({ success: true, message: `Emisión de ${cantidadReal} Pz realizada.` });
});

// POST /api/bancario/quema - [Quema monetaria] (Exclusivo Junta/Departamento Económico)
router.post('/quema', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const { cuenta_id, cantidad, motivo } = req.body;

  if (!cuenta_id || !cantidad || cantidad <= 0) return res.status(400).json({ error: 'Cuenta y cantidad válida requeridos' });

  const cuenta = db.prepare('SELECT * FROM cuentas_bancarias WHERE id = ?').get(cuenta_id);
  if (!cuenta) return res.status(404).json({ error: 'Cuenta no encontrada' });
  if (cuenta.saldo < cantidad) return res.status(400).json({ error: 'Saldo insuficiente para quema' });

  db.prepare('UPDATE cuentas_bancarias SET saldo = saldo - ? WHERE id = ?').run(cantidad, cuenta_id);
  db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
    .run(cuenta_id, 'quema', cantidad, `QUEMA: ${motivo || 'Quema monetaria programada'}`);

  res.json({
    success: true,
    message: `Quema de ${cantidad} Pz ejecutada. Dinero eliminado del suministro circulante.`,
    direccion_quema: '0x00000_QUEMA'
  });
});

export default router;
