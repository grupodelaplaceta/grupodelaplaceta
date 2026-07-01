/**
 * Integración con PlacetaID Gateway existente (plid26-main)
 * Permite validar usuarios contra el sistema PlacetaID centralizado
 */

const defaultPlacetaidApiUrl = process.env.VERCEL ? 'https://id.laplaceta.org/api' : 'http://localhost:3000/api';
const PLACETAID_API_URL = process.env.PLACETAID_API_URL && process.env.PLACETAID_API_URL !== 'http://localhost:3000/api'
  ? process.env.PLACETAID_API_URL
  : defaultPlacetaidApiUrl;
const PLACETAID_CLIENT_ID = process.env.PLACETAID_CLIENT_ID || 'gdlp-crm';

/**
 * Verificar un token PlacetaID contra el gateway
 */
export async function verificarPlacetaIDToken(token) {
  try {
    const response = await fetch(`${PLACETAID_API_URL}/auth/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    });
    if (!response.ok) return null;
    return await response.json();
  } catch (err) {
    console.error('Error verificando PlacetaID:', err.message);
    return null;
  }
}

/**
 * Obtener datos de usuario desde PlacetaID
 */
export async function obtenerUsuarioPlacetaID(placeid) {
  try {
    const response = await fetch(`${PLACETAID_API_URL}/usuarios/${placeid}`, {
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': PLACETAID_CLIENT_ID
      }
    });
    if (!response.ok) return null;
    return await response.json();
  } catch (err) {
    console.error('Error obteniendo usuario PlacetaID:', err.message);
    return null;
  }
}

/**
 * Generar código QR para vinculación PlacetaID
 */
export async function generarQRPlacetaID(data) {
  try {
    const QRCode = (await import('qrcode')).default;
    return await QRCode.toDataURL(JSON.stringify({
      type: 'placetaid_vinculacion',
      app: PLACETAID_CLIENT_ID,
      ...data
    }));
  } catch (err) {
    console.error('Error generando QR PlacetaID:', err.message);
    return null;
  }
}
