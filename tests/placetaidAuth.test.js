import test from 'node:test';
import assert from 'node:assert/strict';
import { buildPlacetaidAuthUrl, buildPlacetaidSessionData } from '../src/services/placetaidAuth.js';

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

test('buildPlacetaidSessionData uses the PlacetaID payload when no local profile exists', () => {
  const sessionData = buildPlacetaidSessionData({
    dip: '12345678Z',
    placeid: 'PLID-12345678Z',
    nombre: 'Ana',
    apellidos: 'García',
    rol: 'miembro',
    edad: 35,
    correo: 'ana@example.com'
  });

  assert.equal(sessionData.dip, '12345678Z');
  assert.equal(sessionData.placeid, 'PLID-12345678Z');
  assert.equal(sessionData.rol, 'miembro');
  assert.equal(sessionData.alias, 'anagarca');
});
