import test from 'node:test';
import assert from 'node:assert/strict';
import { buildPlacetaidAuthUrl } from '../src/services/placetaidAuth.js';

test('buildPlacetaidAuthUrl generates the expected PlacetaID authorization URL', () => {
  const url = buildPlacetaidAuthUrl({
    authBaseUrl: 'https://id.laplaceta.org',
    clientId: 'ccb611655030bdadf7218418dc195dcb',
    redirectUri: 'https://grupodelaplaceta.vercel.app/placetid/callback',
    state: 'estado-seguro-aleatorio',
    platform: 'web'
  });

  const parsed = new URL(url);
  assert.equal(parsed.origin + parsed.pathname, 'https://id.laplaceta.org/');
  assert.equal(parsed.searchParams.get('client_id'), 'ccb611655030bdadf7218418dc195dcb');
  assert.equal(parsed.searchParams.get('redirect_uri'), 'https://grupodelaplaceta.vercel.app/placetid/callback');
  assert.equal(parsed.searchParams.get('platform'), 'web');
  assert.equal(parsed.searchParams.get('state'), 'estado-seguro-aleatorio');
});
