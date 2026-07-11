import 'dotenv/config';
import express from 'express';
import cookieSession from 'cookie-session';
import helmet from 'helmet';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import path from 'path';
import crypto from 'crypto';
import { fileURLToPath } from 'url';
import ejsLayouts from 'express-ejs-layouts';
import { initializeDatabase, getDb, sbFindSolicitante, sbListSolicitantes, sbFindDocumentoFirmadoByUrl } from './src/config/db-supabase.js';
import { supabase, testConnection } from './src/config/supabase.js';
import { buildPlacetaidAuthUrl } from './src/services/placetaidAuth.js';

// Importar rutas
import authRoutes from './src/routes/auth.js';
import identidadRoutes from './src/routes/identidad.js';
import bancarioRoutes from './src/routes/bancario.js';
import bancarioProxyRoutes from './src/routes/bancario-proxy.js';
import fiscalRoutes from './src/routes/fiscal.js';
import ocioRoutes from './src/routes/ocio.js';
import recursosRoutes from './src/routes/recursos.js';
import justiciaRoutes from './src/routes/justicia.js';
import rgpdRoutes from './src/routes/rgpd.js';
import pdfRoutes from './src/routes/pdf.js';
import adminRoutes from './src/routes/admin.js';
import firmaRoutes from './src/routes/firma.js';
import publicoRoutes from './src/routes/publico.js';
import placetidRoutes from './src/routes/placetid.js';
import contenidosRoutes from './src/routes/contenidos.js';
import voleybridgeRoutes from './src/routes/voleybridge.js';
import placetaidAuthRoutes from './src/routes/placetaid-auth.js';
import fotosRoutes from './src/routes/fotos.js';
import tributosRoutes from './src/routes/tributos.js';
import placetaidAdminRoutes from './src/routes/placetaid-admin.js';
import documentosRoutes from './src/routes/documentos.js';
import auditRoutes from './src/routes/audit.js';
import tramitacionRoutes from './src/routes/tramitacion.js';
import notificacionesRoutes from './src/routes/notifications.js';
import juniorAdminRoutes from './src/routes/junior-admin.js';
import juniorEmailRoutes from './src/routes/junior-email.js';
import juniorAuthRoutes from './src/routes/junior-auth.js';
import juniorMgmtRoutes from './src/routes/junior-mgmt.js';
import juniorAcademyRoutes from './src/routes/junior-academy.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();
const PORT = process.env.PORT || 3001;

// ── Inicializar BD (se hace en startServer) ─────────────────────────────────
// (la BD se inicializa al arrancar en la función startServer)

// ── Configuración de Sesión (cookie-session para Vercel serverless) ────────
const sessionSecret = process.env.SESSION_SECRET;
if (!sessionSecret || sessionSecret === 'cambiar_esto_por_un_secreto_seguro' || sessionSecret.length < 16) {
  console.warn('  ⚠️  SESSION_SECRET débil o no configurado. Usar secreto seguro en .env');
}
const sessionConfig = {
  name: 'gdlp-session',
  secret: sessionSecret || 'gdlp-default-secret-change-me',
  maxAge: 24 * 60 * 60 * 1000, // 24 horas
  httpOnly: true,
  sameSite: 'lax',
  secure: process.env.NODE_ENV === 'production',
  overwrite: true
};

// ── Middleware Global ─────────────────────────────────────────────────────────
app.use(helmet({
  contentSecurityPolicy: false, // Desactivado por las vistas EJS con inline scripts
  crossOriginEmbedderPolicy: false
}));

// CORS restrictivo en producción
const corsOrigins = process.env.CORS_ORIGINS ? process.env.CORS_ORIGINS.split(',') : ['http://localhost:3001', 'http://localhost:3000', 'https://id.laplaceta.org'];
app.use(cors({ origin: corsOrigins, credentials: true }));

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(cookieSession(sessionConfig));
app.use(express.static(path.join(__dirname, 'public')));

// ── Favicon ───────────────────────────────────────────────────────────────────
app.get('/favicon.ico', (req, res) => {
  res.redirect('/img/favicon.png');
});

// Rate limiting general API
const keyGenerator = (req) => {
  const fwd = req.headers['x-forwarded-for'];
  const ip = fwd ? fwd.split(',')[0].trim() : (req.ip || req.socket?.remoteAddress || 'unknown');
  return req.ip || ip;
};

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator
});
app.use('/api/', limiter);

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator,
  message: { error: 'Demasiados intentos. Intenta de nuevo más tarde.' }
});
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/register', authLimiter);

// ── Motor de Plantillas EJS con Layouts ─────────────────────────────────────
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'src/views'));
app.use(ejsLayouts);
app.set('layout', 'layouts/main');

// Middleware para disponibilizar variables globales en vistas
app.use((req, res, next) => {
  res.locals.usuario = req.session.usuario || null;
  res.locals.pathActual = req.path;
  res.locals.anoActual = 2026;
  next();
});

// Cache-busting: evitar caché del navegador en páginas HTML
app.use((req, res, next) => {
  if (req.accepts('html') && !req.path.startsWith('/api/') && !req.path.startsWith('/css/') && !req.path.startsWith('/js/') && !req.path.startsWith('/img/')) {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
  }
  next();
});

// ── Health Check ─────────────────────────────────────────────────────────────
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0', timestamp: new Date().toISOString() });
});

// ── Rutas API ────────────────────────────────────────────────────────────────
app.use('/api/auth', authRoutes);
app.use('/api/identidad', identidadRoutes);
app.use('/api/bancario', bancarioRoutes);
app.use('/api/bancario-proxy', bancarioProxyRoutes);
app.use('/api/fiscal', fiscalRoutes);
app.use('/api/ocio', ocioRoutes);
app.use('/api/recursos', recursosRoutes);
app.use('/api/justicia', justiciaRoutes);
app.use('/api/rgpd', rgpdRoutes);
app.use('/api/pdf', pdfRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/admin/tributos', tributosRoutes);
app.use('/api/admin/placetid', placetaidAdminRoutes);
app.use('/', documentosRoutes);
app.use('/api/admin/audit', auditRoutes);
app.use('/api/admin', tramitacionRoutes);
app.use('/api/notificaciones', notificacionesRoutes);
app.use('/api/admin', juniorAdminRoutes);
app.use('/api/admin', juniorEmailRoutes);
app.use('/api/junior', juniorAuthRoutes);
app.use('/api/junior', juniorMgmtRoutes);
app.use('/api/junior/academy', juniorAcademyRoutes);
app.use('/api/firma', firmaRoutes);
app.use('/api/placetid', placetidRoutes);
app.use('/api/contenidos', contenidosRoutes);
app.use('/api/voley', voleybridgeRoutes);
app.use('/api/placetid', placetaidAuthRoutes);
app.use('/api/fotos', fotosRoutes);

// ── PlacetaID Login (popup) ──────────────────────────────────────────────────
app.get('/placetid/login', (req, res) => {
  const placetaidConfig = getPlacetaidConfig(req);
  const state = req.query.state || crypto.randomUUID();
  const authUrl = buildPlacetaidAuthUrl({
    authBaseUrl: placetaidConfig.authBaseUrl,
    clientId: placetaidConfig.clientId,
    redirectUri: req.query.redirect_uri || placetaidConfig.redirectUri,
    state,
    platform: req.query.platform || 'web'
  });
  return res.redirect(authUrl);
});
app.get('/placetid/callback', (req, res) => {
  res.render('placetid/callback', { titulo: 'PlacetaID - Autenticación', layout: false });
});

// ── Rutas Públicas (Web) ─────────────────────────────────────────────────────
app.use('/', publicoRoutes);

function getPlacetaidConfig(req) {
  const protocol = req.headers['x-forwarded-proto'] || req.protocol || 'http';
  const host = req.get('host') || 'localhost:3001';
  const defaultApiUrl = process.env.VERCEL ? 'https://id.laplaceta.org/api' : 'http://localhost:3000/api';
  const apiUrl = process.env.PLACETAID_API_URL && process.env.PLACETAID_API_URL !== 'http://localhost:3000/api'
    ? process.env.PLACETAID_API_URL
    : defaultApiUrl;
  const authBaseUrl = process.env.PLACETAID_AUTH_URL || process.env.PLACETAID_BASE_URL || apiUrl.replace(/\/api$/, '');
  const clientId = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';
  const redirectUri = `${protocol}://${host}/placetid/callback`;

  return {
    authBaseUrl,
    clientId,
    redirectUri,
    authUrl: buildPlacetaidAuthUrl({
      authBaseUrl,
      clientId,
      redirectUri,
      state: req.query?.state || crypto.randomUUID(),
      platform: req.query?.platform || 'web'
    })
  };
}

// ── Rutas Admin Web ──────────────────────────────────────────────────────────

// Auth (sin layout - páginas independientes)
app.get('/login', (req, res) => {
  const placetaidConfig = getPlacetaidConfig(req);
  return res.render('auth/login', {
    titulo: 'PlacetaID - Iniciar Sesión',
    layout: false,
    placetaidAuthUrl: placetaidConfig.authUrl,
    placetaidClientId: placetaidConfig.clientId,
    placetaidRedirectUri: placetaidConfig.redirectUri
  });
});
app.get('/registro', (req, res) => res.render('auth/registro', { titulo: 'PlacetaID - Registro', layout: false }));
app.get('/oauth/authorize', (req, res) => res.render('public/oauth/login', { titulo: 'PlacetaID - Autorizar', client_id: req.query.client_id, redirect_uri: req.query.redirect_uri, state: req.query.state || '', client_nombre: '', client_logo: '', layout: false }));

// Admin panel
app.get('/admin', verificarAuth, (req, res) => {
  res.redirect('/admin/dashboard');
});
app.get('/admin/dashboard', verificarAuth, async (req, res) => {
  // Supabase: stats de usuarios (CRM)
  let totalUsuarios = 0, activos = 0, pendientes = 0;
  try {
    const todos = await sbListSolicitantes();
    totalUsuarios = todos.length;
    activos = todos.filter(u => u.estado === 'activo').length;
    pendientes = todos.filter(u => u.estado === 'pendiente').length;
  } catch (e) { /* fallback silencioso */ }

  // SQLite: stats bancarias/fiscal/justicia
  const db = getDb();
  const totalCuentas = db.prepare('SELECT COUNT(*) as total FROM cuentas_bancarias').get();
  const masaMonetaria = db.prepare("SELECT COALESCE(SUM(saldo),0) as total FROM cuentas_bancarias WHERE tipo_cuenta NOT IN ('Tesoro','Administracion')").get();
  const expedientesAbiertos = db.prepare("SELECT COUNT(*) as total FROM expedientes_disciplinarios WHERE estado NOT IN ('firme','archivado')").get();
  res.render('admin/dashboard', {
    titulo: 'Panel de Administración - GDLP CRM',
    totalUsuarios,
    activos,
    pendientes,
    totalCuentas: totalCuentas?.total || 0,
    masaMonetaria: masaMonetaria?.total || 0,
    expedientesAbiertos: expedientesAbiertos?.total || 0
  });
});

// Módulos
app.get('/admin/identidad', verificarAuth, (req, res) => res.render('identidad/gestion', { titulo: 'Gestión de Identidad' }));
app.get('/admin/bancario', verificarAuth, (req, res) => res.render('bancario/gestion', { titulo: 'Gestión Bancaria' }));
app.get('/admin/fiscal', verificarAuth, (req, res) => res.render('fiscal/gestion', { titulo: 'Gestión Fiscal' }));
app.get('/admin/tributos', verificarAuth, (req, res) => res.render('admin/tributos', { titulo: 'Gestión de Tributos' }));
app.get('/admin/placetid', verificarAuth, (req, res) => res.render('admin/placetid', { titulo: 'Administración PlacetaID' }));
app.get('/admin/ocio', verificarAuth, (req, res) => res.render('ocio/gestion', { titulo: 'Gestión de Ocio y Loterías' }));
app.get('/admin/recursos', verificarAuth, (req, res) => res.render('recursos/gestion', { titulo: 'Gestión de Recursos Digitales' }));
app.get('/admin/justicia', verificarAuth, (req, res) => res.render('justicia/gestion', { titulo: 'Gestión de Justicia' }));
app.get('/admin/rgpd', verificarAuth, (req, res) => res.render('rgpd/gestion', { titulo: 'Protección de Datos - RGPD' }));
app.get('/admin/pdf', verificarAuth, (req, res) => res.render('pdf/gestion', { titulo: 'Generación de Documentos' }));
app.get('/admin/firmas', verificarAuth, (req, res) => res.render('pdf/firmas', { titulo: 'Documentos para Firmar' }));
app.get('/admin/contenidos', verificarAuth, (req, res) => res.render('admin/contenidos', { titulo: 'Gestión de Contenidos' }));
app.get('/admin/voley-club', verificarAuth, (req, res) => res.render('admin/voley-club', { titulo: 'Voley Club - Gestión' }));
app.get('/admin/audit-bancario', verificarAuth, (req, res) => res.render('admin/audit-bancario', { titulo: 'Auditoría Bancaria' }));
app.get('/admin/tramitacion', verificarAuth, (req, res) => res.render('admin/tramitacion', { titulo: 'Tramitación' }));
app.get('/admin/ciudadanos', verificarAuth, (req, res) => res.render('admin/ciudadanos', { titulo: 'Ciudadanos' }));
app.get('/admin/empresas', verificarAuth, (req, res) => res.render('admin/empresas', { titulo: 'Empresas y EIP' }));
app.get('/admin/vigilancia', verificarAuth, (req, res) => res.render('admin/vigilancia', { titulo: 'Vigilancia y Monitoreo' }));
app.get('/admin/placeta-junior', verificarAuth, (req, res) => res.render('admin/placeta-junior', { titulo: 'Placeta Junior - Gestión' }));

// ── Rutas Web Placeta Junior ────────────────────────────────────────────────
app.get('/junior/login', (req, res) => res.render('junior/login', { titulo: 'Placeta Junior - Iniciar Sesión', error: null, layout: false }));
app.get('/junior/registro', (req, res) => res.render('junior/register', { titulo: 'Placeta Junior - Registro', error: null, success: null, layout: false }));
app.get('/junior/dashboard', verificarJuniorSession, (req, res) => res.render('junior/dashboard', { titulo: 'Mi Espacio - Placeta Junior', layout: false }));
app.get('/junior/academia', verificarJuniorSession, (req, res) => res.render('junior/academy', { titulo: 'Academia - Placeta Junior', layout: false }));
app.get('/junior/documentos/terminos', (req, res) => res.render('junior/terminos-condiciones', { titulo: 'Términos y Condiciones', layout: false }));
app.get('/junior/documentos/privacidad', (req, res) => res.render('junior/privacidad', { titulo: 'Política de Privacidad', layout: false }));
app.get('/junior/documentos/consentimiento', (req, res) => res.render('junior/consentimiento-tutor', {
  titulo: 'Consentimiento del Tutor', layout: false,
  nombreMenor: req.query.nombre || '', apellidosMenor: req.query.apellidos || '',
  fechaMenor: req.query.fecha || '', dipMenor: req.query.dip || '',
  nombreTutor: req.query.tutor_nombre || '', apellidosTutor: req.query.tutor_apellidos || '',
  emailTutor: req.query.tutor_email || '', firmaHash: req.query.hash || ''
}));

function verificarJuniorSession(req, res, next) {
  if (!req.session.junior) return res.redirect('/junior/login');
  next();
}

// URLs públicas de firma
app.get('/firmar/:token', async (req, res) => {
  let doc = null;
  try { doc = await sbFindDocumentoFirmadoByUrl(req.params.token); } catch (e) {}
  if (!doc || doc.estado !== 'pendiente') {
    return res.status(404).render('pdf/firma-error', { titulo: 'Documento no encontrado', error: 'El enlace de firma no es válido o ya fue procesado.' });
  }
  res.render('pdf/firma-publica', { titulo: 'Firma de Documento - GDLP', documento: doc });
});

// ── Helper de autenticación ──────────────────────────────────────────────────
function verificarAuth(req, res, next) {
  if (!req.session.usuario) return res.redirect('/login');
  next();
}

// ── Error 404 ────────────────────────────────────────────────────────────────
app.use((req, res) => {
  res.status(404).render('parciales/error', { titulo: '404 - No encontrado', error: 'La página solicitada no existe.' });
});

// ── Iniciar Servidor (asíncrono) ────────────────────────────────────────────
async function startServer() {
  try {
    await initializeDatabase();

    // Inicializar Supabase (no crítico si falla)
    try {
      const ok = await testConnection();
      if (ok) console.log('  📦 Supabase: Conectado');
      else console.log('  ⚠️  Supabase: No disponible');
    } catch(e) {
      console.log('  ⚠️  Supabase: No disponible');
    }

    app.listen(PORT, () => {
      console.log(`
╔══════════════════════════════════════════════════════╗
║         GDLP CRM - Sistema de Gestión               ║
║    Grupo de La Placeta - Normativa v5.0              ║
║                                                      ║
║  Servidor: http://localhost:${PORT}                    ║
║  Admin:   http://localhost:${PORT}/admin               ║
║  Firma:   http://localhost:${PORT}/firmar/<token>      ║
╚══════════════════════════════════════════════════════╝
      `);
    });
  } catch (err) {
    console.error('❌ Error al iniciar el servidor:', err);
    process.exit(1);
  }
}

// Solo iniciar servidor HTTP si no estamos en Vercel serverless
if (!process.env.VERCEL) {
  startServer();
}

// Export para Vercel Serverless
export default app;
