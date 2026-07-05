import { Router } from 'express';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { getDb } from '../config/db.js';
import { generarToken } from '../middleware/auth.js';
import {
  sbFindSolicitante, sbFindSolicitanteByEmail, sbFindSolicitanteByDip,
  sbCreateSolicitante, sbUpdateSolicitante, sbCreateLog
} from '../config/db-supabase.js';

const router = Router();

// POST /api/auth/login - Inicio de sesión
router.post('/login', async (req, res) => {
  try {
    const { alias, password } = req.body;
    if (!alias || !password) return res.status(400).json({ error: 'Alias y contraseña requeridos' });

    // Buscar en Supabase (CRM) primero
    let usuario = null;
    try { usuario = await sbFindSolicitante(alias); } catch (e) {}

    // Si Supabase devolvió usuario pero sin password_hash, ignorar
    if (usuario && !usuario.password_hash) usuario = null;

    // Función helper para verificar password
    let validPassword = false;
    if (usuario) {
      try { validPassword = await bcrypt.compare(password, usuario.password_hash); } catch (e) { validPassword = false; }
    }

    // Fallback a SQLite (bancario legacy) si Supabase falló
    if (!validPassword || !usuario) {
      try {
        const db = getDb();
        let sqliteUser = db.prepare('SELECT * FROM solicitantes WHERE alias = ?').get(alias);
        if (!sqliteUser) sqliteUser = db.prepare('SELECT * FROM solicitantes WHERE dip = ?').get(alias);
        if (sqliteUser) {
          try { validPassword = await bcrypt.compare(password, sqliteUser.password_hash); } catch (e) { validPassword = false; }
          if (validPassword) usuario = sqliteUser;
        } else {
          console.warn(`  ⚠️  Login: Usuario "${alias}" no encontrado en SQLite`);
        }
      } catch (e) {
        console.error(`  ⚠️  Login: Error en fallback SQLite para "${alias}":`, e.message);
      }
    }

    if (!usuario || !validPassword) {
      console.warn(`  ⚠️  Login 401: alias="${alias}", encontrado=${!!usuario}, password_valido=${validPassword}`);
      return res.status(401).json({ error: 'Credenciales inválidas' });
    }

    // Verificar estado
    if (usuario.estado === 'expulsado') return res.status(403).json({ error: 'Cuenta expulsada. Contacte a la Junta Directiva.' });
    if (usuario.estado === 'suspendido') return res.status(403).json({ error: 'Cuenta suspendida temporalmente.' });
    if (usuario.estado === 'baja') return res.status(403).json({ error: 'Cuenta dada de baja.' });

    // Actualizar último acceso
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    try { await sbUpdateSolicitante(usuario.id, { ultimo_acceso: new Date().toISOString(), ip_ultimo_acceso: ip }); } catch (e) {}

    // Registrar log
    try { await sbCreateLog({ usuario_id: usuario.id, accion: 'login', detalle: 'Inicio de sesión exitoso', ip }); } catch (e) {}

    // Datos de sesión
    const sessionData = {
      id: usuario.id,
      alias: usuario.alias,
      dip: usuario.dip,
      placeid: usuario.placeid,
      rol: usuario.rol,
      franja_edad: usuario.franja_edad,
      cargo: usuario.cargo,
      estado: usuario.estado
    };

    // Si es API, devolver JWT
    if (req.headers['accept'] === 'application/json' || req.xhr) {
      const token = generarToken(sessionData);
      return res.json({ token, usuario: sessionData });
    }

    // Si es web, establecer sesión
    req.session.usuario = sessionData;
    const esAdmin = usuario.rol === 'administrador' || usuario.rol === 'admin' || usuario.cargo === 'junta';
    res.json({ success: true, redirect: esAdmin ? '/admin/dashboard' : '/ciudadano', usuario: sessionData });
  } catch (err) {
    console.error('Error en login:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// POST /api/auth/logout - también acepta GET para enlaces directos
router.post('/logout', (req, res) => {
  const usuarioId = req.session.usuario?.id;
  if (usuarioId) {
    sbCreateLog({ usuario_id: usuarioId, accion: 'logout', detalle: 'Cierre de sesión' }).catch(() => {});
  }
  req.session = null;
  res.redirect('/login');
});

router.get('/logout', (req, res) => {
  const usuarioId = req.session.usuario?.id;
  if (usuarioId) {
    sbCreateLog({ usuario_id: usuarioId, accion: 'logout', detalle: 'Cierre de sesión' }).catch(() => {});
  }
  req.session = null;
  res.redirect('/login');
});

// GET /api/auth/session - Obtener sesión actual
router.get('/session', (req, res) => {
  if (req.session.usuario) return res.json({ autenticado: true, usuario: req.session.usuario });
  res.json({ autenticado: false });
});

// POST /api/auth/register - Registro de nuevo solicitante
router.post('/register', async (req, res) => {
  try {
    const { alias, nombre_real, email, fecha_nacimiento, password, id_aval } = req.body;
    if (!alias || !nombre_real || !email || !fecha_nacimiento || !password) {
      return res.status(400).json({ error: 'Todos los campos son obligatorios' });
    }

    // Verificar si el alias ya existe en Supabase
    try {
      const existente = await sbFindSolicitante(alias);
      if (existente) return res.status(409).json({ error: 'El alias o email ya están registrados' });
    } catch (e) {}
    try {
      const existenteEmail = await sbFindSolicitanteByEmail(email);
      if (existenteEmail) return res.status(409).json({ error: 'El alias o email ya están registrados' });
    } catch (e) {}

    // Calcular edad
    const nacimiento = new Date(fecha_nacimiento);
    const hoy = new Date(2026, 5, 28);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mesDiff = hoy.getMonth() - nacimiento.getMonth();
    if (mesDiff < 0 || (mesDiff === 0 && hoy.getDate() < nacimiento.getDate())) edad--;

    // Determinar franja
    let franja = 'Alta_Plena';
    if (edad < 16) franja = 'Tutelada_Basica';
    else if (edad < 18) franja = 'Tutelada_Senior';

    // Verificar lista negra por IP (SQLite)
    const db = getDb();
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const ipRepetida = db.prepare('SELECT id FROM solicitantes WHERE ip_registro = ? AND lista_negra = 1').get(ip);
    if (ipRepetida) {
      const motivo = db.prepare('SELECT motivo_lista_negra FROM solicitantes WHERE ip_registro = ? AND lista_negra = 1 LIMIT 1').get(ip);
      return res.status(403).json({ error: `Coincidencia en Lista Negra: ${motivo?.motivo_lista_negra || 'Expulsión previa'}` });
    }

    // Generar DIP (verificar unicidad en ambos sistemas)
    let dip, placeid, hash, passwordHash;
    do {
      const num = Math.floor(10000000 + Math.random() * 90000000);
      const letras = 'TRWAGMYFPDXBNJZSQVHLCKE';
      dip = `${num}${letras[num % 23]}`;
    } while (db.prepare('SELECT id FROM solicitantes WHERE dip = ?').get(dip));

    placeid = `PLID-${dip}`;
    const timestamp = Date.now().toString(36);
    const random = Math.random().toString(36).substring(2, 10);
    hash = crypto.createHash('sha256').update(`${alias}-${timestamp}-${random}`).digest('hex');
    passwordHash = await bcrypt.hash(password, 10);

    // Insertar en Supabase
    try {
      await sbCreateSolicitante({
        alias, nombre_real, email, fecha_nacimiento, edad, dip, placeid,
        franja_edad: franja, hash_credencial: hash, estado: 'pendiente',
        password_hash: passwordHash, ip_registro: ip, rol: 'miembro'
      });
    } catch (sbErr) {
      // Fallback: insertar en SQLite
      const result = db.prepare(`
        INSERT INTO solicitantes (alias, nombre_real, email, fecha_nacimiento, edad, dip, placeid, franja_edad, id_aval, hash_credencial, estado, password_hash, ip_registro, tutor_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pendiente', ?, ?, ?)
      `).run(alias, nombre_real, email, fecha_nacimiento, edad, dip, placeid, franja, id_aval || null, hash, passwordHash, ip, null);
    }

    // Registrar log
    try { await sbCreateLog({ usuario_id: 0, accion: 'registro', detalle: `Solicitud de alta registrada. Franja: ${franja}`, ip }); } catch (e) {}
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)').run(0, 'registro', `Solicitud de alta registrada. Franja: ${franja}`, ip);

    res.json({
      success: true,
      message: 'Solicitud de alta registrada. Pendiente de aprobación por la Junta Directiva (plazo máximo: 7 días).',
      usuario: { alias, dip, placeid, franja_edad: franja, estado: 'pendiente' }
    });
  } catch (err) {
    console.error('Error en registro:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// ── Funciones auxiliares ─────────────────────────────────────────────────────

function generarDIP(edad, db) {
  // Generar DIP único: 8 dígitos + letra de control
  let dip;
  do {
    const num = Math.floor(10000000 + Math.random() * 90000000);
    const letras = 'TRWAGMYFPDXBNJZSQVHLCKE';
    const letra = letras[num % 23];
    dip = `${num}${letra}`;
  } while (db.prepare('SELECT id FROM solicitantes WHERE dip = ?').get(dip));
  return dip;
}

function generarHashCredencial(alias) {
  const timestamp = Date.now().toString(36);
  const random = Math.random().toString(36).substring(2, 10);
  return crypto.createHash('sha256').update(`${alias}-${timestamp}-${random}`).digest('hex');
}

export default router;
