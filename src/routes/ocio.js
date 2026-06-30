import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import crypto from 'crypto';

const router = Router();

// ── MÓDULO 4: OCIO, LOTERÍAS E INVERSIONES ─────────────────────────────────

// GET /api/ocio/sorteos - Listar sorteos activos
router.get('/sorteos', verificarSesion, (req, res) => {
  const db = getDb();
  const sorteos = db.prepare("SELECT * FROM sorteos WHERE estado = 'activo' ORDER BY fecha_sorteo ASC").all();
  res.json(sorteos);
});

// POST /api/ocio/comprar-boleto/:sorteo_id - [Adquirir Boleto]
router.post('/comprar-boleto/:sorteo_id', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;
  const sorteoId = req.params.sorteo_id;

  // Obtener datos del usuario (edad)
  const usuario = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(usuarioId);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  // FILTRO +12: Menores de 12 bloqueados
  if (usuario.edad < 12) {
    return res.status(403).json({ error: 'Restricción de edad: Mínimo 12 años' });
  }

  // Obtener sorteo
  const sorteo = db.prepare("SELECT * FROM sorteos WHERE id = ? AND estado = 'activo'").get(sorteoId);
  if (!sorteo) return res.status(404).json({ error: 'Sorteo no encontrado o ya celebrado' });

  // Verificar saldo
  const cuenta = db.prepare('SELECT * FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(usuarioId, 'activa');
  if (!cuenta) return res.status(400).json({ error: 'Cuenta bancaria activa requerida' });
  if (cuenta.saldo < sorteo.precio_boleto) {
    return res.status(400).json({ error: `Saldo insuficiente. Precio del boleto: ${sorteo.precio_boleto} Pz` });
  }

  // Generar número de boleto aleatorio (CSPRNG)
  const numeroBoleto = crypto.randomInt(100000, 999999);

  // Procesar compra
  db.transaction(() => {
    db.prepare('UPDATE cuentas_bancarias SET saldo = saldo - ? WHERE id = ?').run(sorteo.precio_boleto, cuenta.id);
    db.prepare('INSERT INTO boletos_comprados (sorteo_id, usuario_id, numero_boleto) VALUES (?, ?, ?)').run(sorteoId, usuarioId, numeroBoleto);
    db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
      .run(cuenta.id, 'compra_boleto', sorteo.precio_boleto, `Compra de boleto #${numeroBoleto} - ${sorteo.titulo}`);
  })();

  res.json({
    success: true,
    message: `Boleto #${numeroBoleto} adquirido para "${sorteo.titulo}"`,
    boleto: numeroBoleto,
    sorteo: sorteo.titulo,
    precio: sorteo.precio_boleto
  });
});

// POST /api/ocio/celebrar-sorteo/:id - Celebrar sorteo y determinar ganador (Admin)
router.post('/celebrar-sorteo/:id', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const sorteo = db.prepare("SELECT * FROM sorteos WHERE id = ? AND estado = 'activo'").get(req.params.id);
  if (!sorteo) return res.status(404).json({ error: 'Sorteo no encontrado o ya celebrado' });

  // Obtener todos los boletos comprados
  const boletos = db.prepare('SELECT * FROM boletos_comprados WHERE sorteo_id = ?').all(sorteo.id);
  if (boletos.length === 0) return res.status(400).json({ error: 'No hay boletos vendidos para este sorteo' });

  // CSPRNG para seleccionar ganador
  const indiceGanador = crypto.randomInt(0, boletos.length);
  const boletoGanador = boletos[indiceGanador];

  const premio = sorteo.premio || (boletos.length * sorteo.precio_boleto * 0.8); // 80% de la bolsa
  const cuentaGanador = db.prepare('SELECT id FROM cuentas_bancarias WHERE usuario_id = ?').get(boletoGanador.usuario_id);

  db.transaction(() => {
    // Actualizar sorteo
    db.prepare('UPDATE sorteos SET estado = ?, ganador_id = ?, premio = ? WHERE id = ?')
      .run('celebrado', boletoGanador.usuario_id, premio, sorteo.id);

    // Pagar premio
    if (cuentaGanador) {
      db.prepare('UPDATE cuentas_bancarias SET saldo = saldo + ? WHERE id = ?').run(premio, cuentaGanador.id);
      db.prepare('INSERT INTO transacciones (cuenta_destino_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
        .run(cuentaGanador.id, 'bono', premio, `Premio sorteo: ${sorteo.titulo}`);
    }
  })();

  res.json({
    success: true,
    message: `Sorteo "${sorteo.titulo}" celebrado. Ganador: boleto #${boletoGanador.numero_boleto}`,
    ganador: { usuario_id: boletoGanador.usuario_id, boleto: boletoGanador.numero_boleto },
    premio,
    total_boletos: boletos.length
  });
});

// POST /api/ocio/invertir - [Invertir Capital en Fondo] (Solo Alta Plena)
router.post('/invertir', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;
  const { fondo, cantidad } = req.body;

  if (!fondo || !cantidad || cantidad <= 0) {
    return res.status(400).json({ error: 'Fondo y cantidad válida requeridos' });
  }

  // Verificar perfil Alta Plena
  const usuario = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(usuarioId);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  if (usuario.franja_edad !== 'Alta_Plena') {
    return res.status(403).json({ error: 'Acceso denegado: Solo perfiles de Alta Plena (≥18 años) pueden invertir en fondos de riesgo.' });
  }

  // Verificar que no tenga sanciones activas
  const sancionActiva = db.prepare(`
    SELECT id FROM expedientes_disciplinarios 
    WHERE infractor_id = ? AND estado NOT IN ('firme', 'archivado') AND sancion_expulsion = 1
  `).get(usuarioId);
  if (sancionActiva) return res.status(403).json({ error: 'No puedes invertir: tienes sanciones disciplinarias activas.' });

  // Verificar saldo
  const cuenta = db.prepare('SELECT * FROM cuentas_bancarias WHERE usuario_id = ? AND estado = ?').get(usuarioId, 'activa');
  if (!cuenta) return res.status(400).json({ error: 'Cuenta bancaria activa requerida' });
  if (cuenta.saldo < cantidad) return res.status(400).json({ error: 'Saldo insuficiente' });

  // Simular rendimiento esperado
  const rendimiento = cantidad * (0.05 + Math.random() * 0.25); // 5% a 30%

  db.transaction(() => {
    db.prepare('UPDATE cuentas_bancarias SET saldo = saldo - ? WHERE id = ?').run(cantidad, cuenta.id);
    db.prepare('INSERT INTO inversiones (usuario_id, fondo, cantidad_invertida, rendimiento_esperado) VALUES (?, ?, ?, ?)')
      .run(usuarioId, fondo, cantidad, rendimiento);
    db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
      .run(cuenta.id, 'inversion', cantidad, `Inversión en fondo: ${fondo}`);
  })();

  res.json({
    success: true,
    message: `Inversión de ${cantidad} Pz en "${fondo}" realizada.`,
    fondo,
    cantidad,
    rendimiento_esperado: rendimiento.toFixed(2),
    tipo_perfil: usuario.franja_edad
  });
});

// POST /api/ocio/crear-sorteo - Crear nuevo sorteo (Admin)
router.post('/crear-sorteo', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const { titulo, descripcion, precio_boleto, fecha_sorteo, premio } = req.body;

  if (!titulo || !fecha_sorteo) return res.status(400).json({ error: 'Título y fecha requeridos' });

  db.prepare('INSERT INTO sorteos (titulo, descripcion, precio_boleto, fecha_sorteo, premio) VALUES (?, ?, ?, ?, ?)')
    .run(titulo, descripcion || '', precio_boleto || 10, fecha_sorteo, premio || null);

  res.json({ success: true, message: `Sorteo "${titulo}" creado` });
});

export default router;
