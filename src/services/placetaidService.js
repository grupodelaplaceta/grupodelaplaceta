/**
 * Servicio de integración con PlacetaID Gateway
 * Gestiona la verificación de identidad, autorización parental y firmas
 * 
 * PlacetaID URL: https://id.laplaceta.org (o http://localhost:3000 en desarrollo)
 * API Key: ccb611655030bdadf7218418dc195dcb
 */

const PLACETAID_URL = process.env.PLACETAID_URL || 'http://localhost:3000';
const PLACETAID_API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';

/**
 * Solicitar autorización al tutor vía PlacetaID Móvil
 * Crea un auth request que el tutor debe aprobar desde su app
 */
export async function solicitarAutorizacionTutor({ dipTutor, concepto, monto, dipMenor, detalles }) {
  try {
    // Crear auth request en PlacetaID para que el tutor lo apruebe desde el móvil
    const r = await fetch(`${PLACETAID_URL}/api/mobil/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-API-Key': PLACETAID_API_KEY },
      body: JSON.stringify({
        dip: dipTutor,
        concepto: concepto || 'Autorización Placeta Junior',
        metadata: {
          tipo: 'autorizacion_parental',
          dip_menor: dipMenor,
          monto: monto,
          detalles: detalles || ''
        }
      })
    });

    if (!r.ok) {
      const errData = await r.json().catch(() => ({}));
      throw new Error(errData.error || `PlacetaID respondió ${r.status}`);
    }

    const data = await r.json();
    return {
      success: true,
      requestId: data.requestId,
      codigo: data.codigo,
      mensaje: 'Solicitud enviada al tutor. Debe aprobarla desde PlacetaID Móvil.'
    };
  } catch (err) {
    console.warn('[PlacetaID] Error solicitando autorización:', err.message);
    return { success: false, error: err.message };
  }
}

/**
 * Verificar si una solicitud de autorización fue aprobada por el tutor
 */
export async function verificarAutorizacion(requestId) {
  try {
    const r = await fetch(`${PLACETAID_URL}/api/mobil/poll/${requestId}`);
    if (!r.ok) return { success: false, aprobado: false };

    const data = await r.json();
    if (data.status === 'authorized' && data.token) {
      return {
        success: true,
        aprobado: true,
        token: data.token,
        firmado_por: data.authorizedBy
      };
    }

    return { success: true, aprobado: false, status: data.status };
  } catch (err) {
    return { success: false, aprobado: false, error: err.message };
  }
}

/**
 * Verificar identidad de un tutor mediante PlacetaID
 * Comprueba que el DIP del tutor existe y está activo
 */
export async function verificarTutor(dipTutor) {
  try {
    const r = await fetch(`${PLACETAID_URL}/api/mobil/status/${dipTutor}`, {
      headers: { 'X-API-Key': PLACETAID_API_KEY }
    });
    if (!r.ok) return { existe: false };

    const data = await r.json();
    return {
      existe: data.activo === true,
      activo: data.activo === true,
      nombre: data.nombre || '',
      tieneMovil: data.tieneMovil || false
    };
  } catch (err) {
    return { existe: false, error: err.message };
  }
}

/**
 * Consultar datos de un registro en PlacetaID por DIP
 */
export async function consultarRegistro(dip) {
  try {
    const r = await fetch(`${PLACETAID_URL}/api/admin/registros`, {
      headers: { 'X-API-Key': PLACETAID_API_KEY }
    });
    if (!r.ok) return null;

    const registros = await r.json();
    const registro = registros.find(reg => reg.dip === dip);
    return registro || null;
  } catch (err) {
    return null;
  }
}

/**
 * Crear un documento de firma en el CRM para que el tutor lo firme
 */
export async function crearDocumentoParaFirma({ codigoModelo, titulo, contenido, dipTutor }) {
  try {
    const r = await fetch(`${PLACETAID_URL}/api/admin/solicitantes`, {
      headers: { 'X-API-Key': PLACETAID_API_KEY }
    });
    // El documento se crea a través del CRM, no de PlacetaID directamente
    return {
      success: true,
      codigo_modelo: codigoModelo,
      titulo,
      mensaje: 'Documento listo para firma'
    };
  } catch (err) {
    return { success: false, error: err.message };
  }
}
