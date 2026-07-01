import crypto from 'crypto';

export function buildPlacetaidAuthUrl(options = {}) {
  const {
    authBaseUrl = process.env.PLACETAID_AUTH_URL || process.env.PLACETAID_BASE_URL || 'https://id.laplaceta.org',
    clientId = process.env.PLACETAID_CLIENT_ID || 'gdlp-crm',
    redirectUri,
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
