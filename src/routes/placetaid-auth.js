/**
 * Integración con el PlacetaID real (plid26-main)
 * Proxy a la API de autenticación real en lugar de hacer auth local
 */

import { Router } from 'express';
import {
  sbFindSolicitanteByDip, sbCreateSolicitante,
  sbCreateLog
} from '../config/db-supabase.js';

const router = Router();

const PLACETAID_API = process.env.PLACETAID_API_URL || (process.env.VERCEL ? 'https://id.laplaceta.org/api' : 'http://localhost:3000/api');

// ── Login: delega al PlacetaID real ──────────────────────────────────────

router.post('/login', async (req, res) => {
  try {
    const { alias, password } = req.body;
    if (!alias || !password) return res.status(400).json({ error: 'DIP/Alias y contraseña requeridos' });

    // Llamar a FASE 1 del PlacetaID real
    const fase1Resp = await fetch(`${PLACETAID_API}/auth/fase1`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        dip: alias,
        password,
        servicio: 'GDLP CRM',
        platform: 'web'
      })
    });

    const fase1 = await fase1Resp.json();

    if (!fase1.ok) {
      if (fase1.bloqueado) return res.status(403).json({ error: fase1.error || 'Cuenta bloqueada' });
      if (fase1.intentosRestantes) return res.status(401).json({ error: `Credenciales incorrectas. Intentos restantes: ${fase1.intentosRestantes}` });
      return res.status(fase1Resp.status).json({ error: fase1.error || 'Error de autenticación' });
    }

    // Si tiene tokenFase2 → requiere 2FA
    if (fase1.tokenFase2) {
      return res.json({
        requiere2fa: true,
        tokenFase2: fase1.tokenFase2,
        mensaje: 'Introduce el código 2FA'
      });
    }

    // Login completo sin 2FA (demo o 2FA desactivado)
    if (fase1.tokenSesion) {
      return await completarSesion(res, fase1.tokenSesion, fase1.registro, req);
    }

    return res.status(500).json({ error: 'Respuesta inesperada del servidor PlacetaID' });

  } catch (err) {
    console.error('Error PlacetaID login:', err);
    if (err.cause?.code === 'ECONNREFUSED' || err.message?.includes('ECONNREFUSED')) {
      return res.status(503).json({ error: 'PlacetaID no disponible. Inténtalo más tarde.' });
    }
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// ── FASE 2: verificar código 2FA contra PlacetaID real ────────────────────

router.post('/fase2', async (req, res) => {
  try {
    const { tokenFase2, codigo2fa } = req.body;
    if (!tokenFase2 || !codigo2fa) return res.status(400).json({ error: 'Token y código 2FA requeridos' });

    const fase2Resp = await fetch(`${PLACETAID_API}/auth/fase2`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tokenFase2, codigo2fa })
    });

    const fase2 = await fase2Resp.json();

    if (!fase2.ok) {
      if (fase2.bloqueado) return res.status(403).json({ error: 'Cuenta bloqueada' });
      if (fase2.intentosRestantes) return res.status(401).json({ error: `Código incorrecto. Intentos restantes: ${fase2.intentosRestantes}` });
      return res.status(fase2Resp.status).json({ error: fase2.error || 'Error de verificación' });
    }

    if (!fase2.tokenSesion) return res.status(500).json({ error: 'Error al obtener token de sesión' });

    return await completarSesion(res, fase2.tokenSesion, fase2.registro, req);

  } catch (err) {
    console.error('Error PlacetaID fase2:', err);
    res.status(500).json({ error: 'Error de conexión con PlacetaID' });
  }
});

// ── Helper: crear sesión CRM desde token PlacetaID ────────────────────────

async function completarSesion(res, tokenSesion, registroPlaceta, req) {
  try {
    // Buscar o crear usuario local en Supabase
    let localUser = await sbFindSolicitanteByDip(registroPlaceta.dip);

    if (!localUser) {
      const alias = registroPlaceta.nombre?.toLowerCase()?.replace(/\s/g, '') ||
                    `user${registroPlaceta.dip}`;
      const nombreReal = registroPlaceta.nombreCompleto ||
                         `${registroPlaceta.nombre || ''} ${registroPlaceta.apellidos || ''}`.trim() ||
                         alias;
      const placeid = registroPlaceta.placeid || `PLID-${registroPlaceta.dip}`;
      const esAdmin = registroPlaceta.rol === 'administrador';

      localUser = await sbCreateSolicitante({
        alias,
        nombre_real: nombreReal,
        email: registroPlaceta.correo || `${alias}@laplaceta.org`,
        dip: registroPlaceta.dip,
        placeid,
        rol: esAdmin ? 'administrador' : 'miembro',
        estado: 'activo',
        franja_edad: (registroPlaceta.edad || 0) >= 18 ? 'Alta_Plena' : 'Tutelada_Senior',
        password_hash: ''
      });
    }

    const sessionData = {
      id: localUser.id,
      alias: localUser.alias,
      dip: localUser.dip,
      placeid: localUser.placeid,
      rol: localUser.rol,
      franja_edad: localUser.franja_edad,
      cargo: localUser.cargo,
      estado: localUser.estado
    };

    req.session.usuario = sessionData;
    req.session.placetaidToken = tokenSesion;

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    await sbCreateLog({ usuario_id: localUser.id, accion: 'login_placetaid', detalle: `Login via PlacetaID: ${registroPlaceta.dip}`, ip }).catch(() => {});

    const esAdmin = localUser.rol === 'administrador' || localUser.rol === 'admin';
    res.json({
      success: true,
      redirect: esAdmin ? '/admin/dashboard' : '/ciudadano',
      token: tokenSesion,
      usuario: sessionData
    });

  } catch (err) {
    console.error('Error completarSesion:', err);
    res.json({ success: true, token: tokenSesion, redirect: '/login', warning: 'Sesión obtenida pero no se pudo crear perfil local' });
  }
});

// ── set-session: Establecer sesión desde token PlacetaID (callback popup) ─

router.post('/set-session', async (req, res) => {
  const { token, usuario } = req.body;
  if (!token) return res.status(400).json({ error: 'Token requerido' });

  try {
    // Verificar token contra PlacetaID real
    const verifyResp = await fetch(`${PLACETAID_API}/auth/session`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!verifyResp.ok) return res.status(401).json({ error: 'Token inválido o expirado' });

    const verifyData = await verifyResp.json();
    if (!verifyData.ok || !verifyData.registro) return res.status(401).json({ error: 'Sesión PlacetaID no válida' });

    return await completarSesion(res, token, verifyData.registro, req);

  } catch (err) {
    console.error('Error set-session:', err);
    if (usuario) {
      try { return await completarSesion(res, token, usuario, req); } catch (e) { /* fallback falló */ }
    }
    res.status(502).json({ error: 'PlacetaID no disponible' });
  }
});

// ── Verificar token contra PlacetaID real ─────────────────────────────────

router.get('/verify', async (req, res) => {
  const auth = req.headers['authorization'];
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'Token requerido' });

  try {
    const token = auth.split(' ')[1];
    const verifyResp = await fetch(`${PLACETAID_API}/auth/session`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!verifyResp.ok) return res.status(401).json({ error: 'Token inválido o expirado' });

    const data = await verifyResp.json();
    if (!data.ok) return res.status(401).json({ error: 'Token inválido' });

    res.json({ autenticado: true, usuario: data.registro });
  } catch (err) {
    res.status(502).json({ error: 'PlacetaID no disponible' });
  }
});

// ── Cerrar sesión ────────────────────────────────────────────────────────

router.post('/logout', async (req, res) => {
  const usuarioId = req.session.usuario?.id;
  if (usuarioId) {
    await sbCreateLog({ usuario_id: usuarioId, accion: 'logout', detalle: 'Cierre de sesión' }).catch(() => {});
  }
  req.session.destroy();
  res.json({ success: true });
});

export default router;
