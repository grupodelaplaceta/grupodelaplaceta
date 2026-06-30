import { Router } from 'express';
import crypto from 'crypto';
import {
  sbFindSolicitante, sbFindSolicitanteByDip, sbCreateSolicitante, sbUpdateSolicitante,
  sbCreateLog, sbCreatePlacetaidToken, sbFindPlacetaidToken,
  sbDeactivatePlacetaidToken
} from '../config/db-supabase.js';

const router = Router();

// ── Login FASE 1: alias + password ─────────────────────────────────────────

router.post('/login', async (req, res) => {
  try {
    const { alias, password } = req.body;
    if (!alias || !password) return res.status(400).json({ error: 'Alias/DIP y contraseña requeridos' });

    const usuario = await sbFindSolicitante(alias);
    if (!usuario) return res.status(401).json({ error: 'Credenciales inválidas' });
    if (usuario.estado === 'expulsado') return res.status(403).json({ error: 'Cuenta expulsada.' });
    if (usuario.estado === 'suspendido') return res.status(403).json({ error: 'Cuenta suspendida.' });
    if (usuario.estado === 'baja') return res.status(403).json({ error: 'Cuenta dada de baja.' });

    const bcrypt = await import('bcryptjs');
    const valid = await bcrypt.compare(password, usuario.password_hash);
    if (!valid) {
      const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
      await sbCreateLog({ usuario_id: usuario.id, accion: 'login_fallido', detalle: 'Intento de inicio de sesión fallido', ip });
      return res.status(401).json({ error: 'Credenciales inválidas' });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    await sbUpdateSolicitante(usuario.id, { ultimo_acceso: new Date().toISOString(), ip_ultimo_acceso: ip });
    await sbCreateLog({ usuario_id: usuario.id, accion: 'login', detalle: 'Inicio de sesión exitoso', ip });

    const sessionData = { id: usuario.id, alias: usuario.alias, dip: usuario.dip, placeid: usuario.placeid, rol: usuario.rol, franja_edad: usuario.franja_edad, cargo: usuario.cargo, estado: usuario.estado };

    const token = crypto.randomBytes(32).toString('hex');
    const expira = new Date(Date.now() + 86400000).toISOString();
    await sbCreatePlacetaidToken({ token, usuario_id: usuario.id, expira_en: expira });

    res.json({ token, usuario: sessionData });
  } catch (err) {
    console.error('Error PlacetaID login:', err);
    res.status(500).json({ error: 'Error interno' });
  }
});

// ── Establecer sesión desde token de PlacetaID real ───────────────────────

router.post('/set-session', async (req, res) => {
  const { token, usuario } = req.body;
  if (!token) return res.status(400).json({ error: 'Token requerido' });

  if (usuario) {
    try {
      let localUser = await sbFindSolicitanteByDip(usuario.dip);
      if (!localUser) {
        const placeid = usuario.placeid || `PLID-${usuario.dip}`;
        const alias = usuario.alias || usuario.nombre?.toLowerCase()?.replace(/\s/g, '') || `user${usuario.dip}`;
        const { sbFindSolicitanteByDip: _ } = await import('../config/db-supabase.js');
        const newUser = await sbCreateSolicitante({
          alias, nombre_real: usuario.nombreCompleto || usuario.nombre || alias,
          email: usuario.correo || `${alias}@laplaceta.org`, dip: usuario.dip,
          placeid, rol: 'miembro', estado: 'activo',
          franja_edad: usuario.edad >= 18 ? 'Alta_Plena' : 'Tutelada_Senior', password_hash: ''
        });
        localUser = newUser;
      }
      if (localUser) {
        req.session.usuario = { id: localUser.id, alias: localUser.alias, dip: localUser.dip, placeid: localUser.placeid, rol: localUser.rol, franja_edad: localUser.franja_edad, cargo: localUser.cargo, estado: localUser.estado };
        const esAdmin = localUser.rol === 'administrador' || localUser.rol === 'admin';
        return res.json({ success: true, redirect: esAdmin ? '/admin/dashboard' : '/ciudadano' });
      }
    } catch (err) {
      console.error('Error set-session:', err);
    }
  }
});

// ── Verificar token ────────────────────────────────────────────────────────

router.get('/verify', async (req, res) => {
  const auth = req.headers['authorization'];
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'Token requerido' });
  try {
    const token = auth.split(' ')[1];
    const tk = await sbFindPlacetaidToken(token);
    if (!tk) return res.status(401).json({ error: 'Token inválido o expirado' });
    res.json({ autenticado: true, usuario: tk.solicitantes });
  } catch (err) {
    res.status(500).json({ error: 'Error interno' });
  }
});

// ── Cerrar sesión ──────────────────────────────────────────────────────────

router.post('/logout', async (req, res) => {
  const auth = req.headers['authorization'];
  if (auth && auth.startsWith('Bearer ')) {
    const token = auth.split(' ')[1];
    try { await sbDeactivatePlacetaidToken(token); } catch (e) {}
  }
  const usuarioId = req.session.usuario?.id;
  if (usuarioId) {
    await sbCreateLog({ usuario_id: usuarioId, accion: 'logout', detalle: 'Cierre de sesión' });
  }
  req.session.destroy();
  res.json({ success: true });
});

export default router;
