import { Router } from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import PDFGenerator from '../services/pdfGenerator.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const LOGO_GDLP = path.join(__dirname, '..', '..', 'public', 'img', 'tributos.png');
const LOGO_TRIBUTOS = path.join(__dirname, '..', '..', 'public', 'img', 'tributos.png');

const router = Router();

// POST /api/pdf/generar/:codigo - Generar PDF según código de modelo
router.post('/generar/:codigo', verificarSesion, verificarRol('administrador', 'junta', 'juez'), async (req, res) => {
  try {
    const db = getDb();
    const { codigo } = req.params;
    const datos = req.body;

    const generator = new PDFGenerator({ logo: LOGO_GDLP });
    let doc;

    switch (codigo) {
      case 'GDLP-JUS-001':
        doc = generator.generarInicioExpediente(datos);
        break;
      case 'GDLP-JUS-003':
        doc = generator.generarResolucionSancionadora(datos);
        break;
      case 'GDLP-JUS-004':
        doc = generator.generarDecretoExpulsion(datos);
        break;
      case 'GDLP-HAC-010':
        doc = generator.generarRequerimientoDescubierto(datos);
        break;
      case 'GDLP-SEC-020':
        doc = generator.generarCertificadoBaja(datos);
        break;
      case 'GDLP-SEC-021':
        doc = generator.generarCertificadoARCO(datos);
        break;
      default:
        return res.status(400).json({ error: `Código de modelo no soportado: ${codigo}. Modelos: GDLP-JUS-001, GDLP-JUS-003, GDLP-JUS-004, GDLP-HAC-010, GDLP-SEC-020, GDLP-SEC-021` });
    }

    // Configurar respuesta como PDF
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${codigo}-${datos.ID_EXPEDIENTE || Date.now()}.pdf"`);

    doc.pipe(res);
    doc.end();

    // Registrar en logs
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
      .run(req.session.usuario.id, 'generar_pdf', `PDF generado: ${codigo}`, ip);

  } catch (err) {
    console.error('Error generando PDF:', err);
    res.status(500).json({ error: 'Error generando el documento PDF' });
  }
});

// GET /api/pdf/modelos - Listar modelos disponibles
router.get('/modelos', verificarSesion, (req, res) => {
  const modelos = [
    {
      codigo: 'GDLP-JUS-001',
      titulo: 'Acuerdo de Inicio de Expediente Disciplinario y Pliego de Cargos',
      modulo: 'Justicia',
      trigger: 'Apertura de oficio por el Departamento de Justicia o validación de denuncia',
      variables: ['ID_EXPEDIENTE', 'ALIAS_INFRACTOR', 'DIP_NÚMERO', 'TIPO_INFRACCION', 'DESCRIPCIÓN_HECHOS']
    },
    {
      codigo: 'GDLP-JUS-002',
      titulo: 'Notificación de Medidas Cautelares de Urgencia',
      modulo: 'Justicia',
      trigger: 'Clic en [Aplicar Medidas Cautelares] por el Juez',
      variables: ['ID_EXPEDIENTE', 'MEDIDAS_ADOPTADAS']
    },
    {
      codigo: 'GDLP-JUS-003',
      titulo: 'Resolución Sancionadora en Primera Instancia',
      modulo: 'Justicia',
      trigger: 'Emisión del fallo motivado por el Departamento de Justicia',
      variables: ['CUANTÍA_MULTA_PZ', 'DÍAS_SUSPENSIÓN', 'HECHOS_PROBADOS', 'FECHA_LÍMITE_APELACIÓN']
    },
    {
      codigo: 'GDLP-JUS-004',
      titulo: 'Decreto de Expulsión Definitiva y Confiscación Monetaria',
      modulo: 'Justicia',
      trigger: 'Sentencia firme por falta Muy Grave',
      variables: ['SALDO_CONFISCADO', 'HASH_QUEMA_MONEDA', 'ESTADO_CUENTA']
    },
    {
      codigo: 'GDLP-HAC-010',
      titulo: 'Requerimiento de Regularización por Descubierto Bancario de Cuenta',
      modulo: 'Hacienda',
      trigger: 'Script automatizado al día 6 de saldo negativo',
      variables: ['SALDO_NEGATIVO', 'SANCIÓN_APLICADA_PZ']
    },
    {
      codigo: 'GDLP-HAC-011',
      titulo: 'Providencia de Apremio y Multa Automática por Exceso de Capital',
      modulo: 'Hacienda',
      trigger: 'Auditoría automática mensual',
      variables: ['SALDO_AUDITADO', 'EXCESO_DETECTADO', 'MULTA_FIJA']
    },
    {
      codigo: 'GDLP-SEC-020',
      titulo: 'Resolución de Certificación de Baja Voluntaria y Revocación de Identidad',
      modulo: 'Secretaría',
      trigger: 'Solicitud aprobada de baja del ecosistema',
      variables: ['ALIAS_CIUDADANO', 'DIP_REVOCADO', 'SALDO_LIQUIDADO_RETORNADO']
    },
    {
      codigo: 'GDLP-SEC-021',
      titulo: 'Certificado de Ejercicio de Derechos ARCO y Anonimización de Datos Reales',
      modulo: 'Secretaría / RGPD',
      trigger: 'Ejecución del Derecho de Supresión ("Olvido")',
      variables: ['NOMBRE_REAL_AFECTADO', 'HASH_ANONIMIZACION', 'LOG_RESTRICCION_DE_DATOS']
    }
  ];

  res.json(modelos);
});

export default router;
