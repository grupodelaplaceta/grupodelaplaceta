import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import { v4 as uuidv4 } from 'uuid';

const router = Router();

// ── MÓDULO 1: GESTIÓN DE IDENTIDAD Y CONTROL DE ACCESO ─────────────────────

// GET /api/identidad/pendientes - Lista de solicitudes pendientes (Junta Directiva)
router.get('/pendientes', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const pendientes = db.prepare(`
    SELECT s.*, t.alias as tutor_alias
    FROM solicitantes s
    LEFT JOIN solicitantes t ON s.tutor_id = t.id
    WHERE s.estado = 'pendiente'
    ORDER BY s.creado_en DESC
  `).all();
  res.json(pendientes);
});

// POST /api/identidad/aprobar/:id - [Aprobar Solicitud de Ingreso] (Exclusivo Junta Directiva)
router.post('/aprobar/:id', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const solicitante = db.prepare('SELECT * FROM solicitantes WHERE id = ? AND estado = ?').get(req.params.id, 'pendiente');
  if (!solicitante) return res.status(404).json({ error: 'Solicitante no encontrado o ya procesado' });

  // Verificar lista negra
  if (solicitante.lista_negra) {
    return res.status(403).json({ error: 'Coincidencia en Lista Negra. No se puede aprobar.' });
  }

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

  // Aprobar: cambiar estado a activo
  db.prepare('UPDATE solicitantes SET estado = ?, actualizado_en = datetime(\'now\') WHERE id = ?').run('activo', solicitante.id);

  // Crear cuenta bancaria según franja
  const tipoCuenta = getTipoCuenta(solicitante.franja_edad);
  const saldoMaximo = getSaldoMaximo(solicitante.franja_edad);

  db.prepare('INSERT INTO cuentas_bancarias (usuario_id, tipo_cuenta, saldo, saldo_maximo, bono_bienvenida_activo, fecha_bono) VALUES (?, ?, ?, ?, 1, datetime(\'now\'))')
    .run(solicitante.id, tipoCuenta, 500, saldoMaximo);

  // Registrar log
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'aprobar_alta', `Aprobado ingreso de ${solicitante.alias} (DIP: ${solicitante.dip})`, ip);

  res.json({
    success: true,
    message: `Solicitante ${solicitante.alias} aprobado. Cuenta ${tipoCuenta} creada con bono de bienvenida.`,
    usuario: { alias: solicitante.alias, dip: solicitante.dip, placeid: solicitante.placeid }
  });
});

// POST /api/identidad/rechazar/:id - Rechazar solicitud
router.post('/rechazar/:id', verificarSesion, verificarRol('administrador', 'junta'), (req, res) => {
  const db = getDb();
  const solicitante = db.prepare('SELECT * FROM solicitantes WHERE id = ? AND estado = ?').get(req.params.id, 'pendiente');
  if (!solicitante) return res.status(404).json({ error: 'Solicitante no encontrado o ya procesado' });

  const { motivo } = req.body;
  db.prepare('UPDATE solicitantes SET estado = ?, motivo_lista_negra = ?, actualizado_en = datetime(\'now\') WHERE id = ?')
    .run('baja', motivo || 'Solicitud rechazada', solicitante.id);

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'rechazar_alta', `Rechazado ingreso de ${solicitante.alias}: ${motivo || 'Sin motivo'}`, ip);

  res.json({ success: true, message: `Solicitud de ${solicitante.alias} rechazada.` });
});

// POST /api/identidad/generar-dip/:id - [Generar / Actualizar DIP]
router.post('/generar-dip/:id', verificarSesion, (req, res) => {
  const db = getDb();
  const usuario = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(req.params.id);
  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });

  // Recalcular edad y franja
  const nacimiento = new Date(usuario.fecha_nacimiento);
  const hoy = new Date(2026, 5, 28);
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const mesDiff = hoy.getMonth() - nacimiento.getMonth();
  if (mesDiff < 0 || (mesDiff === 0 && hoy.getDate() < nacimiento.getDate())) edad--;

  let franja = 'Alta_Plena';
  if (edad < 16) franja = 'Tutelada_Basica';
  else if (edad < 18) franja = 'Tutelada_Senior';

  const nuevoHash = uuidv4();

  db.prepare('UPDATE solicitantes SET edad = ?, franja_edad = ?, hash_credencial = ?, actualizado_en = datetime(\'now\') WHERE id = ?')
    .run(edad, franja, nuevoHash, usuario.id);

  // Actualizar permisos de cuenta según franja
  const db2 = getDb();
  const cuenta = db2.prepare('SELECT id, tipo_cuenta FROM cuentas_bancarias WHERE usuario_id = ?').get(usuario.id);
  if (cuenta) {
    const nuevoTipo = getTipoCuenta(franja);
    if (cuenta.tipo_cuenta !== nuevoTipo) {
      db2.prepare('UPDATE cuentas_bancarias SET tipo_cuenta = ? WHERE id = ?').run(nuevoTipo, cuenta.id);
    }
  }

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'generar_dip', `DIP actualizado para ${usuario.alias}. Franja: ${franja}`, ip);

  res.json({
    success: true,
    message: `DIP actualizado. Franja: ${franja}`,
    dip: usuario.dip,
    franja_edad: franja,
    hash_credencial: nuevoHash
  });
});

// GET /api/identidad/usuarios - Listado completo de usuarios
router.get('/usuarios', verificarSesion, (req, res) => {
  const db = getDb();
  const { estado, franja, rol } = req.query;
  let query = 'SELECT id, alias, dip, placeid, franja_edad, edad, estado, rol, cargo, email, creado_en, ultimo_acceso FROM solicitantes WHERE 1=1';
  const params = [];

  if (estado) { query += ' AND estado = ?'; params.push(estado); }
  if (franja) { query += ' AND franja_edad = ?'; params.push(franja); }
  if (rol) { query += ' AND rol = ?'; params.push(rol); }

  query += ' ORDER BY creado_en DESC';
  const usuarios = db.prepare(query).all(...params);
  res.json(usuarios);
});

// GET /api/identidad/usuarios/:id - Detalle de usuario
router.get('/usuarios/:id', verificarSesion, (req, res) => {
  const db = getDb();
  const usuario = db.prepare(`
    SELECT s.*, t.alias as tutor_alias, c.saldo, c.tipo_cuenta, c.estado as estado_cuenta
    FROM solicitantes s
    LEFT JOIN solicitantes t ON s.tutor_id = t.id
    LEFT JOIN cuentas_bancarias c ON s.id = c.usuario_id
    WHERE s.id = ?
  `).get(req.params.id);

  if (!usuario) return res.status(404).json({ error: 'Usuario no encontrado' });
  res.json(usuario);
});

// ── Helpers ──────────────────────────────────────────────────────────────────
function getTipoCuenta(franja) {
  const tipos = {
    'Tutelada_Basica': 'Junior_Basica',
    'Tutelada_Senior': 'Junior_Senior',
    'Alta_Plena': 'Ciudadana_Plena',
    'Institucional': 'Institucional'
  };
  return tipos[franja] || 'Ciudadana_Plena';
}

function getSaldoMaximo(franja) {
  const limites = {
    'Tutelada_Basica': 100000,
    'Tutelada_Senior': 250000,
    'Alta_Plena': 500000,
    'Institucional': 10000000
  };
  return limites[franja] || 500000;
}

// ── Importar desde PlacetaID ────────────────────────────────────────────────
router.post('/importar-placetid', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const PLID = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const r = await fetch(`${PLID}/api/admin/registros`, { headers: { 'X-API-Key': API_KEY } });
    if (!r.ok) return res.status(502).json({ error: `PlacetaID respondió ${r.status}`, imported: 0 });
    const registros = await r.json();
    const db = getDb();
    let imported = 0;
    const insert = db.prepare(`INSERT OR IGNORE INTO solicitantes (alias, nombre_real, email, telefono, dip, placeid, hash_credencial, rol, estado, franja_edad, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`);
    for (const reg of registros) {
      const alias = reg.nombre?.split(' ')[0] || reg.dip || 'importado';
      const nombre = reg.nombre || '';
      const email = reg.email || '';
      const telefono = reg.telefono || '';
      const placeid = reg.placeid || `PLID-${reg.dip}`;
      const hash = uuidv4();
      try {
        insert.run(alias, nombre, email, telefono, reg.dip, placeid, hash, reg.rol || 'miembro', reg.activo ? 'activo' : 'pendiente', reg.franjaEdad || 'Alta_Plena', reg.creadoEn || new Date().toISOString());
        imported++;
      } catch (e) { /* duplicado, ignorar */ }
    }
    res.json({ ok: true, imported, total: registros.length, message: `Importados ${imported} de ${registros.length} registros` });
  } catch (err) {
    res.status(500).json({ error: err.message, imported: 0 });
  }
});

// ── Enviar ciudadano a PlacetaID ────────────────────────────────────────────
router.post('/enviar-placetid/:id', verificarSesion, verificarRol('administrador'), async (req, res) => {
  try {
    const db = getDb();
    const user = db.prepare('SELECT * FROM solicitantes WHERE id = ?').get(req.params.id);
    if (!user) return res.status(404).json({ error: 'Ciudadano no encontrado' });
    const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
    const PLID = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org';
    const bcrypt = await import('bcryptjs');
    const body = {
      dip: user.dip,
      nombre: user.nombre_real || user.alias,
      email: user.email || '',
      telefono: user.telefono || '',
      password: `temp-${uuidv4().slice(0,8)}`,
      rol: user.rol || 'miembro',
      franjaEdad: user.franja_edad
    };
    body.passwordHash = await bcrypt.hash(body.password, 10);
    const r = await fetch(`${PLID}/api/admin/registros`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-API-Key': API_KEY },
      body: JSON.stringify(body)
    });
    const data = await r.json();
    if (!r.ok) return res.status(r.status).json({ error: data.error || 'Error en PlacetaID' });
    res.json({ ok: true, message: `Enviado ${user.alias} a PlacetaID`, placetaid: data });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
