import crypto from 'crypto';

export function buildPlacetaidSessionData(registroPlaceta = {}) {
  const dip = registroPlaceta.dip || registroPlaceta.DIP || '';
  const nombre = registroPlaceta.nombre || registroPlaceta.nombreCompleto || '';
  const apellidos = registroPlaceta.apellidos || '';
  const nombreReal = [nombre, apellidos].filter(Boolean).join(' ').trim();
  const aliasBase = (nombreReal || dip || 'usuario').toLowerCase().replace(/\s+/g, '').replace(/[^a-z0-9]/g, '');
  const alias = aliasBase || `user${dip}`;
  const rol = registroPlaceta.rol === 'administrador' || registroPlaceta.rol === 'admin' ? 'administrador' : 'miembro';
  return {
    id: registroPlaceta.id || `placetaid-${dip || crypto.randomBytes(4).toString('hex')}`,
    alias,
    dip,
    placeid: registroPlaceta.placeid || `PLID-${dip}`,
    rol,
    franja_edad: (registroPlaceta.edad || registroPlaceta.age || 0) >= 18 ? 'Alta_Plena' : 'Tutelada_Senior',
    cargo: registroPlaceta.cargo || null,
    estado: 'activo'
  };
}

export function buildPlacetaidAuthUrl(options = {}) {
  const {
    authBaseUrl = process.env.PLACETAID_AUTH_URL || process.env.PLACETAID_BASE_URL || 'https://id.laplaceta.org',
    clientId = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb',
    redirectUri = process.env.PLACETAID_REDIRECT_URI || 'https://www.laplaceta.org/placetid/callback',
    state,
    platform = 'web'
  } = options;

  if (!redirectUri) {
    throw new Error('redirectUri is required');
  }

  const normalizedBaseUrl = authBaseUrl.replace(/\/api$/, '');
  const url = new URL(normalizedBaseUrl);
  url.searchParams.set('client_id', clientId);
  url.searchParams.set('redirect_uri', redirectUri);
  url.searchParams.set('platform', platform);
  url.searchParams.set('state', state || crypto.randomUUID());
  return url.toString();
}
