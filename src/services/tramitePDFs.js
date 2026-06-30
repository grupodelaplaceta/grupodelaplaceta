import PDFDocument from 'pdfkit';
import https from 'https';
import crypto from 'crypto';

const LOGO_URL = 'https://i.postimg.cc/RZYKzdmX/Diseno-sin-titulo-76.png';

function fetchImage(url) {
  return new Promise((resolve, reject) => {
    https.get(url, res => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    }).on('error', reject);
  });
}

/**
 * Genera un PDF de solicitud de DIP
 */
export async function generarPDFDIP(datos) {
  const doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
  const buffers = [];
  doc.on('data', b => buffers.push(b));

  try {
    const img = await fetchImage(LOGO_URL);
    doc.image(img, 60, 50, { width: 50 });
  } catch {}

  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Solicitud de DIP', 120, 55);
  doc.fontSize(7).fillColor('#999').text(`Generado: ${new Date().toLocaleString('es-ES')}`, 120, 70);

  doc.moveDown(4);
  doc.fontSize(18).font('Helvetica-Bold').fillColor('#3f00d8').text('SOLICITUD DE DOCUMENTO DE IDENTIDAD DE LA PLACETA', { align: 'center' });
  doc.moveDown(0.5);
  const lineY = doc.y;
  doc.moveTo(60, lineY).lineTo(535, lineY).strokeColor('#3f00d8').stroke();
  doc.moveDown();

  const fields = [
    ['Nombre Real', datos.nombre_real],
    ['Alias en el Ecosistema', datos.alias],
    ['Email de Contacto', datos.email],
    ['Fecha de Nacimiento', datos.fecha_nacimiento],
    ['Edad', String(datos.edad)],
    ['DIP Provisional', datos.dip],
    ['Código de Tutor', datos.codigo_tutor || '—'],
  ];

  fields.forEach(([k, v]) => {
    doc.fontSize(10).font('Helvetica-Bold').fillColor('#333');
    doc.text(`${k}: `, { continued: true });
    doc.font('Helvetica').fillColor('#555').text(v);
    doc.moveDown(0.3);
  });

  doc.moveDown();
  doc.moveTo(60, doc.y).lineTo(535, doc.y).strokeColor('#ddd').stroke();
  doc.moveDown();

  doc.fontSize(8).font('Helvetica-Oblique').fillColor('#999')
    .text('Documento generado automáticamente por el sistema GDLP CRM. El DIP provisional será revisado por la administración.', { align: 'center' });

  doc.moveDown(0.5);
  const hash = crypto.createHash('sha256').update(JSON.stringify(datos) + Date.now()).digest('hex');
  doc.fontSize(7).font('Helvetica').fillColor('#bbb')
    .text(`[ Huella: ${hash.substring(0, 16)}... ]`, { align: 'center' });

  doc.end();
  return new Promise(resolve => doc.on('finish', () => resolve(Buffer.concat(buffers))));
}

/**
 * Genera un PDF de solicitud de PlacetaID
 */
export async function generarPDFPlacetaID(datos) {
  const doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
  const buffers = [];
  doc.on('data', b => buffers.push(b));

  try {
    const img = await fetchImage(LOGO_URL);
    doc.image(img, 60, 50, { width: 50 });
  } catch {}

  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Activación PlacetaID', 120, 55);
  doc.fontSize(7).fillColor('#999').text(`Generado: ${new Date().toLocaleString('es-ES')}`, 120, 70);

  doc.moveDown(4);
  doc.fontSize(18).font('Helvetica-Bold').fillColor('#3f00d8').text('CERTIFICADO DE ACTIVACIÓN DE PLACETAID', { align: 'center' });
  doc.moveDown(0.5);
  const lineY = doc.y;
  doc.moveTo(60, lineY).lineTo(535, lineY).strokeColor('#3f00d8').stroke();
  doc.moveDown();

  const fields = [
    ['PlacetaID', datos.placeid],
    ['Alias', datos.alias],
    ['DIP', datos.dip],
    ['Email', datos.email],
    ['Franja de Edad', datos.franja],
    ['Cuenta Bancaria', datos.tipo_cuenta],
    ['Saldo Inicial', `${datos.saldo_inicial} Pz`],
    ['Fecha de Alta', new Date().toLocaleDateString('es-ES')],
  ];

  fields.forEach(([k, v]) => {
    doc.fontSize(10).font('Helvetica-Bold').fillColor('#333');
    doc.text(`${k}: `, { continued: true });
    doc.font('Helvetica').fillColor('#555').text(v);
    doc.moveDown(0.3);
  });

  doc.moveDown();
  doc.moveTo(60, doc.y).lineTo(535, doc.y).strokeColor('#ddd').stroke();
  doc.moveDown();

  doc.fontSize(8).font('Helvetica-Oblique').fillColor('#999')
    .text('Ecosistema Virtual "La Placeta" · Asociación Grupo de La Placeta', { align: 'center' });

  doc.end();
  return new Promise(resolve => doc.on('finish', () => resolve(Buffer.concat(buffers))));
}

/**
 * Genera un PDF de queja o sugerencia
 */
export async function generarPDFQueja(datos) {
  const doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
  const buffers = [];
  doc.on('data', b => buffers.push(b));

  try {
    const img = await fetchImage(LOGO_URL);
    doc.image(img, 60, 50, { width: 50 });
  } catch {}

  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Registro de Queja / Sugerencia', 120, 55);
  doc.fontSize(7).fillColor('#999').text(`Generado: ${new Date().toLocaleString('es-ES')}`, 120, 70);

  doc.moveDown(4);
  doc.fontSize(18).font('Helvetica-Bold').fillColor('#3f00d8').text('QUEJA / SUGERENCIA', { align: 'center' });
  doc.moveDown(0.5);
  const lineY = doc.y;
  doc.moveTo(60, lineY).lineTo(535, lineY).strokeColor('#3f00d8').stroke();
  doc.moveDown();

  const fields = [
    ['Nº de Registro', datos.numero],
    ['Nombre', datos.nombre],
    ['Email', datos.email || '—'],
    ['Tipo', datos.tipo === 'queja' ? '🔴 Queja' : '💡 Sugerencia'],
    ['Fecha de Registro', new Date().toLocaleDateString('es-ES')],
  ];

  fields.forEach(([k, v]) => {
    doc.fontSize(10).font('Helvetica-Bold').fillColor('#333');
    doc.text(`${k}: `, { continued: true });
    doc.font('Helvetica').fillColor('#555').text(v);
    doc.moveDown(0.3);
  });

  doc.moveDown();
  doc.fontSize(11).font('Helvetica-Bold').fillColor('#333').text('Mensaje:', { underline: true });
  doc.moveDown(0.5);
  doc.fontSize(9).font('Helvetica').fillColor('#444').text(datos.mensaje, { align: 'justify' });

  doc.moveDown(2);
  doc.moveTo(60, doc.y).lineTo(535, doc.y).strokeColor('#ddd').stroke();
  doc.moveDown();
  doc.fontSize(8).font('Helvetica-Oblique').fillColor('#999')
    .text('Plazo estimado de respuesta: 30 días hábiles.', { align: 'center' });

  doc.end();
  return new Promise(resolve => doc.on('finish', () => resolve(Buffer.concat(buffers))));
}

/**
 * Genera un PDF de Control Parental
 */
export async function generarPDFControlParental(datos) {
  const doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
  const buffers = [];
  doc.on('data', b => buffers.push(b));

  try {
    const img = await fetchImage(LOGO_URL);
    doc.image(img, 60, 50, { width: 50 });
  } catch {}

  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Registro de Control Parental', 120, 55);
  doc.fontSize(7).fillColor('#999').text(`Generado: ${new Date().toLocaleString('es-ES')}`, 120, 70);

  doc.moveDown(4);
  doc.fontSize(18).font('Helvetica-Bold').fillColor('#3f00d8').text('REGISTRO DE CONTROL PARENTAL', { align: 'center' });
  doc.moveDown(0.3);
  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Vinculación Tutor - Menor · RGPD', { align: 'center' });
  doc.moveDown(0.5);
  const lineY = doc.y;
  doc.moveTo(60, lineY).lineTo(535, lineY).strokeColor('#3f00d8').stroke();
  doc.moveDown();

  const fields = [
    ['Nombre del Tutor', datos.nombre_tutor],
    ['DNI del Tutor', datos.dni_tutor],
    ['Email del Tutor', datos.email_tutor],
    ['Teléfono', datos.telefono_tutor || '—'],
    ['Nombre del Menor', datos.nombre_menor],
    ['Fecha de Nacimiento del Menor', datos.fecha_nac_menor],
    ['Código de Vinculación', datos.codigo],
  ];

  fields.forEach(([k, v]) => {
    doc.fontSize(10).font('Helvetica-Bold').fillColor('#333');
    doc.text(`${k}: `, { continued: true });
    doc.font('Helvetica').fillColor('#555').text(v);
    doc.moveDown(0.3);
  });

  doc.moveDown();
  doc.moveTo(60, doc.y).lineTo(535, doc.y).strokeColor('#ddd').stroke();
  doc.moveDown();
  doc.fontSize(8).font('Helvetica-Oblique').fillColor('#999')
    .text('Los datos facilitados quedan protegidos conforme al RGPD y la LOPDGDD.', { align: 'center' });

  doc.end();
  return new Promise(resolve => doc.on('finish', () => resolve(Buffer.concat(buffers))));
}

/**
 * Genera un PDF de Alta de Entidad
 */
export async function generarPDFEntidad(datos) {
  const doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
  const buffers = [];
  doc.on('data', b => buffers.push(b));

  try {
    const img = await fetchImage(LOGO_URL);
    doc.image(img, 60, 50, { width: 50 });
  } catch {}

  doc.fontSize(10).font('Helvetica').fillColor('#666').text('Solicitud de Alta de Entidad', 120, 55);
  doc.fontSize(7).fillColor('#999').text(`Generado: ${new Date().toLocaleString('es-ES')}`, 120, 70);

  doc.moveDown(4);
  doc.fontSize(18).font('Helvetica-Bold').fillColor('#3f00d8').text('SOLICITUD DE ALTA DE ENTIDAD', { align: 'center' });
  doc.moveDown(0.5);
  const lineY = doc.y;
  doc.moveTo(60, lineY).lineTo(535, lineY).strokeColor('#3f00d8').stroke();
  doc.moveDown();

  const fields = [
    ['Nombre de la Entidad', datos.nombre_entidad],
    ['Tipo', datos.tipo],
    ['EIP (Entidad ID Placeta)', datos.eip],
    ['Email de Contacto', datos.email],
    ['CIF / NIF', datos.cif || '—'],
    ['DIP del Representante', datos.representante_dip],
    ['Alias del Representante', datos.representante_alias],
  ];

  fields.forEach(([k, v]) => {
    doc.fontSize(10).font('Helvetica-Bold').fillColor('#333');
    doc.text(`${k}: `, { continued: true });
    doc.font('Helvetica').fillColor('#555').text(v);
    doc.moveDown(0.3);
  });

  if (datos.descripcion) {
    doc.moveDown();
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#333').text('Descripción:', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#444').text(datos.descripcion, { align: 'justify' });
  }

  doc.moveDown(2);
  doc.moveTo(60, doc.y).lineTo(535, doc.y).strokeColor('#ddd').stroke();
  doc.moveDown();
  doc.fontSize(8).font('Helvetica-Oblique').fillColor('#999')
    .text('Ecosistema Virtual "La Placeta" · Asociación Grupo de La Placeta', { align: 'center' });

  doc.end();
  return new Promise(resolve => doc.on('finish', () => resolve(Buffer.concat(buffers))));
}
