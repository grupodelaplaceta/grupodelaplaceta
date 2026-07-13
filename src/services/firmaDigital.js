import crypto from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import {
  sbCreateDocumentoFirmado, sbFindDocumentoFirmadoByUrl, sbFindDocumentoFirmadoById,
  sbUpdateDocumentoFirmado, sbFindSolicitanteById,
  sbCreateLog, sbListDocumentosFirmadosPendientes
} from '../config/db-supabase.js';

/**
 * Sistema de Firma Digital de Documentos
 * Permite la firma online de documentos mediante URL pública
 */

class FirmaDigitalService {
  /**
   * Crear un documento pendiente de firma con URL pública
   */
  async crearDocumentoParaFirma({ codigoModelo, titulo, contenido, creadoPor }) {
    const urlToken = uuidv4();
    const contenidoHash = crypto.createHash('sha256').update(contenido || titulo).digest('hex');

    const result = await sbCreateDocumentoFirmado({
      codigo_modelo: codigoModelo,
      titulo_documento: titulo,
      contenido_hash: contenidoHash,
      url_firma: urlToken,
      firmado_por: creadoPor || null,
      estado: 'pendiente'
    });

    return {
      id: result.id,
      url_firma: urlToken,
      url_completa: `${process.env.BASE_URL || 'http://localhost:3001'}/firmar/${urlToken}`,
      codigo_modelo: codigoModelo,
      titulo
    };
  }

  /**
   * Firmar un documento (verificar identidad y registrar firma)
   */
  async firmarDocumento(urlToken, firmanteId, ip) {
    const documento = await sbFindDocumentoFirmadoByUrl(urlToken);
    if (!documento || documento.estado !== 'pendiente') {
      throw new Error('Documento no encontrado o ya firmado');
    }

    const firmante = await sbFindSolicitanteById(firmanteId);
    if (!firmante) throw new Error('Firmante no encontrado');

    // Generar hash de firma
    const firmaHash = crypto.createHash('sha256')
      .update(`${documento.contenido_hash}-${firmanteId}-${Date.now()}`)
      .digest('hex');

    await sbUpdateDocumentoFirmado(documento.id, {
      estado: 'firmado', firma_hash: firmaHash,
      firmado_por: firmanteId, ip_firma: ip, fecha_firma: new Date().toISOString()
    });

    await sbCreateLog({
      usuario_id: firmanteId, accion: 'firmar_documento',
      detalle: `Documento ${documento.codigo_modelo} firmado: ${documento.titulo_documento}`, ip
    });

    return {
      success: true, documento: documento.titulo_documento,
      firmado_por: firmante.alias, hash_firma: firmaHash,
      fecha: new Date().toISOString()
    };
  }

  /**
   * Rechazar un documento pendiente de firma
   */
  async rechazarDocumento(urlToken, firmanteId, ip, motivo) {
    const documento = await sbFindDocumentoFirmadoByUrl(urlToken);
    if (!documento || documento.estado !== 'pendiente') {
      throw new Error('Documento no encontrado o ya procesado');
    }

    await sbUpdateDocumentoFirmado(documento.id, {
      estado: 'rechazado', firmado_por: firmanteId,
      ip_firma: ip, fecha_firma: new Date().toISOString()
    });

    await sbCreateLog({
      usuario_id: firmanteId, accion: 'rechazar_documento',
      detalle: `Documento ${documento.codigo_modelo} rechazado: ${motivo || 'Sin motivo'}`, ip
    });

    return { success: true, message: 'Documento rechazado' };
  }

  /**
   * Verificar la validez de un documento firmado
   */
  async verificarDocumento(documentoId) {
    let doc;
    try { doc = await sbFindDocumentoFirmadoById(documentoId); } catch (e) { doc = null; }
    if (!doc) return { valido: false, error: 'Documento no encontrado' };
    const esValido = doc.estado === 'firmado' && (doc.firma_hash !== null || doc.hash_documento !== null);
    const hash = doc.firma_hash || doc.hash_documento;

    return {
      valido: esValido,
      documento: { codigo: doc.codigo_modelo, titulo: doc.titulo_documento, estado: doc.estado },
      firma: esValido ? {
        hash, fecha: doc.fecha_firma || doc.creado_en, ip: doc.ip_firma
      } : null
    };
  }

  /**
   * Obtener documentos pendientes de firma de un usuario
   */
  async obtenerPendientes(usuarioId) {
    return await sbListDocumentosFirmadosPendientes(usuarioId);
  }

  /**
   * Generar par de claves RSA para firma digital (una vez)
   */
  static generarParClaves() {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
      modulusLength: 2048,
      publicKeyEncoding: { type: 'spki', format: 'pem' },
      privateKeyEncoding: { type: 'pkcs8', format: 'pem' }
    });
    return { publicKey, privateKey };
  }
}

export default FirmaDigitalService;
