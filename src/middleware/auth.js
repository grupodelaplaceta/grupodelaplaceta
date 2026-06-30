import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'gdlp-crm-jwt-secret-2026';

/**
 * Middleware de autenticación para API REST (JWT Bearer token)
 */
export function verificarTokenAPI(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token requerido' });
  }
  try {
    const token = auth.slice(7);
    const decoded = jwt.verify(token, JWT_SECRET);
    req.usuarioAPI = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Token inválido o expirado' });
  }
}

/**
 * Middleware de autenticación para Web (sesión)
 */
export function verificarSesion(req, res, next) {
  if (!req.session.usuario) {
    if (req.xhr || req.path.startsWith('/api/')) {
      return res.status(401).json({ error: 'Sesión requerida' });
    }
    return res.redirect('/login');
  }
  next();
}

/**
 * Middleware de verificación de rol
 */
export function verificarRol(...roles) {
  return (req, res, next) => {
    const usuario = req.session.usuario || req.usuarioAPI;
    if (!usuario) return res.status(401).json({ error: 'No autenticado' });
    if (!roles.includes(usuario.rol)) {
      return res.status(403).json({ error: 'Acceso denegado: permisos insuficientes' });
    }
    next();
  };
}

/**
 * Generar token JWT
 */
export function generarToken(payload) {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: process.env.JWT_EXPIRY || '24h' });
}
