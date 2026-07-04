/**
 * Integración con el PlacetaID real (plid26-main)
 * Proxy a la API de autenticación real en lugar de hacer auth local
 */

import { Router } from 'express';
import bcrypt from 'bcryptjs';
import {
  sbFindSolicitanteByDip, sbFindSolicitante, sbCreateSolicitante,
  sbCreateLog
} from '../config/db-supabase.js';
import { buildPlacetaidSessionData } from '../services/placetaidAuth.js';
import { getDb } from '../config/db.js';

const router = Router();

const defaultPlacetaidApi = process.env.VERCEL ? 'https://id.laplaceta.org/api' : 'http://localhost:3000/api';
const PLACETAID_API = process.env.PLACETAID_API_URL && process.env.PLACETAID_API_URL !== 'http://localhost:3000/api'
  ? process.env.PLACETAID_API_URL
  : defaultPlacetaidApi;
const PLACETAID_CLIENT_ID = process.env.PLACETAID_CLIENT_ID || 'gdlp-crm';
const PLACETAID_CLIENT_SECRET = process.env.PLACETAID_CLIENT_SECRET || 'gdlp-crm-dev-secret';

function buildPlacetaidHeaders(extraHeaders = {}) {
  return {
    'Content-Type': 'application/json',
    'X-API-Key': PLACETAID_CLIENT_ID,
    ...extraHeaders
  };
}

// ── Login: delega al PlacetaID real ──────────────────────────────────────

router.post('/login', async (req, res) => {
  try {
    const { alias, password } = req.body;
    if (!alias || !password) return res.status(400).json({ error: 'DIP/Alias y contraseña requeridos' });

    // Intentar autenticación local primero (fallback si PlacetaID externo no responde)
    let localUser;
    try { localUser = await sbFindSolicitante(alias); } catch (e) { localUser = null; }

    // Si Supabase devolvió usuario pero sin password_hash, ignorar
    if (localUser && !localUser.password_hash) localUser = null;

    let validLocal = false;
    if (localUser) {
      try { validLocal = await bcrypt.compare(password, localUser.password_hash); } catch (e) { validLocal = false; }
    }

    // Fallback a SQLite si Supabase no tiene la password correcta
    if (!validLocal) {
      try {
        const db = getDb();
        const sqliteUser = db.prepare('SELECT * FROM solicitantes WHERE alias = ?').get(alias);
        if (sqliteUser) {
          try { validLocal = await bcrypt.compare(password, sqliteUser.password_hash); } catch (e) { validLocal = false; }
          if (validLocal) localUser = sqliteUser;
        }
      } catch (e) { localUser = null; }
    }

    if (localUser && validLocal) {
      const validLocal = await bcrypt.compare(password, localUser.password_hash);
      if (validLocal) {
        const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
        const sessionData = {
          id: localUser.id,
          alias: localUser.alias,
          dip: localUser.dip,
          placeid: localUser.placeid,
          rol: localUser.rol || 'miembro',
          franja_edad: localUser.franja_edad,
          cargo: localUser.cargo,
          estado: localUser.estado
        };
        req.session.usuario = sessionData;
        try { await sbCreateLog({ usuario_id: sessionData.id, accion: 'login_local', detalle: 'Login local CRM', ip }); } catch (e) {}
        const esAdmin = sessionData.rol === 'administrador' || sessionData.rol === 'admin' || sessionData.cargo === 'junta';
        return res.json({ success: true, redirect: esAdmin ? '/admin/dashboard' : '/ciudadano', usuario: sessionData });
      } else {
        return res.status(401).json({ error: 'Credenciales inválidas' });
      }
    }

    // Llamar a FASE 1 del PlacetaID real
    const fase1Resp = await fetch(`${PLACETAID_API}/auth/fase1`, {
      method: 'POST',
      headers: buildPlacetaidHeaders(),
      body: JSON.stringify({
        dip: alias,
        password,
        servicio: 'GDLP CRM',
        servicioUrl: `${req.protocol}://${req.get('host')}/placetid/callback`,
        clientId: PLACETAID_CLIENT_ID,
        platform: 'web',
        state: req.body.state || null
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
      headers: buildPlacetaidHeaders(),
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
    let localUser = null;
    let sessionData = buildPlacetaidSessionData(registroPlaceta);

    try {
      localUser = await sbFindSolicitanteByDip(registroPlaceta.dip);
      if (!localUser) {
        localUser = await sbCreateSolicitante({
          alias: sessionData.alias,
          nombre_real: registroPlaceta.nombreCompleto || `${registroPlaceta.nombre || ''} ${registroPlaceta.apellidos || ''}`.trim() || sessionData.alias,
          email: registroPlaceta.correo || `${sessionData.alias}@laplaceta.org`,
          dip: registroPlaceta.dip,
          placeid: sessionData.placeid,
          rol: sessionData.rol,
          estado: 'activo',
          franja_edad: sessionData.franja_edad,
          password_hash: ''
        });
      }

      sessionData = {
        id: localUser.id,
        alias: localUser.alias,
        dip: localUser.dip,
        placeid: localUser.placeid,
        rol: localUser.rol,
        franja_edad: localUser.franja_edad,
        cargo: localUser.cargo,
        estado: localUser.estado
      };
    } catch (dbErr) {
      console.warn('Supabase no disponible para perfil local, usando sesión mínima:', dbErr.message);
      sessionData.id = sessionData.id || `placetaid-${registroPlaceta.dip || 'local'}`;
    }

    req.session.usuario = sessionData;
    req.session.placetaidToken = tokenSesion;

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    try {
      await sbCreateLog({ usuario_id: sessionData.id, accion: 'login_placetaid', detalle: `Login via PlacetaID: ${registroPlaceta.dip}`, ip });
    } catch (logErr) {
      console.warn('No se pudo registrar el log:', logErr.message);
    }

    const esAdmin = sessionData.rol === 'administrador' || sessionData.rol === 'admin';
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
}

// ── set-session: Establecer sesión desde token PlacetaID (callback directo) ─

router.post('/set-session', async (req, res) => {
  const { token, usuario, state } = req.body;
  if (!token) return res.status(400).json({ error: 'Token requerido' });
  if (!usuario) return res.status(400).json({ error: 'Datos de usuario requeridos' });

  try {
    // Intentar verificar contra PlacetaID (con timeout 3s)
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);
    try {
      const verifyResp = await fetch(`${PLACETAID_API}/auth/session`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-API-Key': PLACETAID_CLIENT_ID
        },
        signal: controller.signal
      });
      clearTimeout(timeoutId);
      if (verifyResp.ok) {
        const verifyData = await verifyResp.json();
        if (verifyData.ok && verifyData.registro) {
          return await completarSesion(res, token, verifyData.registro, req);
        }
      }
    } catch (verifyErr) {
      // Fallback: continuar con datos del callback
    }

    // Usar datos del callback directamente
    if (state) {
      req.session.placetaidState = state;
    }
    return await completarSesion(res, token, usuario, req);

  } catch (err) {
    console.error('Error set-session:', err);
    res.status(502).json({ error: 'Error al crear sesión' });
  }
});

// ── Verificar token contra PlacetaID real ─────────────────────────────────

router.get('/verify', async (req, res) => {
  const auth = req.headers['authorization'];
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'Token requerido' });

  try {
    const token = auth.split(' ')[1];
    const verifyResp = await fetch(`${PLACETAID_API}/auth/session`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-API-Key': PLACETAID_CLIENT_ID
      }
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
