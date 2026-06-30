import { Router } from 'express';
import crypto from 'crypto';
import {
  sbFindOAuthClient, sbFindOAuthClientSecure, sbCreateOAuthClient,
  sbListOAuthClients, sbCreateOAuthCode, sbFindOAuthCode,
  sbMarkOAuthCodeUsado, sbCreateOAuthToken, sbFindSolicitanteById
} from '../config/db-supabase.js';

const router = Router();

// ── Registro de aplicaciones cliente ─────────────────────────────────────────

router.post('/apps/registro', async (req, res) => {
  const { nombre, descripcion, url_retorno, url_logo } = req.body;
  if (!nombre || !url_retorno) {
    return res.status(400).json({ error: 'Nombre y URL de retorno son obligatorios' });
  }
  try {
    const clientId = `app_${crypto.randomBytes(8).toString('hex')}`;
    const clientSecret = crypto.randomBytes(24).toString('hex');
    await sbCreateOAuthClient({ client_id: clientId, client_secret: clientSecret, nombre, descripcion: descripcion || '', url_retorno, url_logo: url_logo || '' });
    res.json({ client_id: clientId, client_secret: clientSecret, nombre });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Listar aplicaciones ─────────────────────────────────────────────────────

router.get('/apps', async (req, res) => {
  try {
    const apps = await sbListOAuthClients();
    res.json(apps);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Inicio de autorización OAuth ────────────────────────────────────────────

router.get('/authorize', async (req, res) => {
  const { client_id, redirect_uri, state } = req.query;
  if (!client_id || !redirect_uri) {
    return res.render('public/oauth/error', { titulo: 'Error de Autorización', error: 'Parámetros incompletos. Se requiere client_id y redirect_uri.' });
  }
  try {
    const client = await sbFindOAuthClient(client_id);
    if (!client) {
      return res.render('public/oauth/error', { titulo: 'Aplicación no encontrada', error: 'La aplicación solicitante no está registrada o ha sido desactivada.' });
    }
    if (req.session.usuario) {
      const code = crypto.randomBytes(16).toString('hex');
      await sbCreateOAuthCode({ code, client_id, usuario_id: req.session.usuario.id, redirect_uri });
      const sep = redirect_uri.includes('?') ? '&' : '?';
      return res.redirect(`${redirect_uri}${sep}code=${code}&state=${state || ''}`);
    }
    res.render('public/oauth/login', { titulo: 'Iniciar sesión con PlacetaID', client_id, redirect_uri, state: state || '', client_nombre: client.nombre, client_logo: client.url_logo, layout: false });
  } catch (err) {
    res.render('public/oauth/error', { titulo: 'Error', error: err.message });
  }
});

// ── Procesar login desde el popup OAuth ─────────────────────────────────────

router.post('/authorize', async (req, res) => {
  const { alias, password, client_id, redirect_uri, state } = req.body;
  if (!alias || !password || !client_id || !redirect_uri) {
    return res.render('public/oauth/login', { titulo: 'Error', error: 'Faltan datos requeridos', client_id, redirect_uri, state: state || '', client_nombre: 'Aplicación', client_logo: '', layout: false });
  }
  try {
    const client = await sbFindOAuthClient(client_id);
    if (!client) return res.render('public/oauth/error', { titulo: 'Error', error: 'Aplicación no válida.' });
    const bcrypt = await import('bcryptjs');
    const { sbFindSolicitante } = await import('../config/db-supabase.js');
    const usuario = await sbFindSolicitante(alias);
    if (!usuario) {
      return res.render('public/oauth/login', { titulo: 'Error', error: 'Credenciales inválidas', client_id, redirect_uri, state: state || '', client_nombre: client.nombre, client_logo: client.url_logo, layout: false });
    }
    const valid = await bcrypt.compare(password, usuario.password_hash);
    if (!valid) {
      return res.render('public/oauth/login', { titulo: 'Error', error: 'Credenciales inválidas', client_id, redirect_uri, state: state || '', client_nombre: client.nombre, client_logo: client.url_logo, layout: false });
    }
    const code = crypto.randomBytes(16).toString('hex');
    await sbCreateOAuthCode({ code, client_id, usuario_id: usuario.id, redirect_uri });
    const sep = redirect_uri.includes('?') ? '&' : '?';
    if (req.xhr || req.headers['accept'] === 'application/json') {
      return res.json({ success: true, redirect: `${redirect_uri}${sep}code=${code}&state=${state || ''}` });
    }
    res.redirect(`${redirect_uri}${sep}code=${code}&state=${state || ''}`);
  } catch (err) {
    res.render('public/oauth/error', { titulo: 'Error', error: err.message });
  }
});

// ── Canjear código por token ───────────────────────────────────────────────

router.post('/token', async (req, res) => {
  const { client_id, client_secret, code } = req.body;
  if (!client_id || !client_secret || !code) return res.status(400).json({ error: 'client_id, client_secret y code requeridos' });
  try {
    const client = await sbFindOAuthClientSecure(client_id, client_secret);
    if (!client) return res.status(401).json({ error: 'Credenciales de aplicación inválidas' });
    const authCode = await sbFindOAuthCode(code, client_id);
    if (!authCode) return res.status(400).json({ error: 'Código inválido o ya usado' });
    await sbMarkOAuthCodeUsado(authCode.id);
    const accessToken = crypto.randomBytes(32).toString('hex');
    const expira = new Date(Date.now() + 3600000).toISOString();
    await sbCreateOAuthToken({ token: accessToken, client_id, usuario_id: authCode.usuario_id, expira_en: expira });
    const usuario = await sbFindSolicitanteById(authCode.usuario_id);

    res.json({
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: 3600,
      usuario: {
        id: usuario.id,
        alias: usuario.alias,
        dip: usuario.dip,
        placeid: usuario.placeid,
        rol: usuario.rol,
        franja_edad: usuario.franja_edad
      }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Verificar token ─────────────────────────────────────────────────────────

router.get('/verify', async (req, res) => {
  const auth = req.headers['authorization'];
  if (!auth || !auth.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token requerido' });
  }
  try {
    const token = auth.split(' ')[1];
    // Buscar token en Supabase
    const { supabase } = await import('../config/supabase.js');
    const { data: tk, error } = await supabase.from('oauth_tokens')
      .select('*,solicitantes!oauth_tokens_usuario_id_fkey(id,alias,dip,placeid,rol,franja_edad),oauth_clients!oauth_tokens_client_id_fkey(nombre)')
      .eq('token', token).eq('activo', 1).limit(1).single();
    if (error || !tk) {
      return res.status(401).json({ error: 'Token inválido o expirado' });
    }
    res.json({
      autenticado: true,
      usuario: tk.solicitantes,
      aplicacion: tk.oauth_clients?.nombre || 'Desconocida'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
