import PDFDocument from 'pdfkit';
import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FONTS_DIR = path.join(__dirname, '..', '..', 'public', 'fonts');
const IMG_DIR = path.join(__dirname, '..', '..', 'public', 'img');

/**
 * Motor de Documentos PDF — Estilo Corporativo GDLP
 * Paleta base: purple-900:#1c005f, purple-700:#341087, purple-500:#5a2fc2
 * Tributos: accent #4e396f
 * Fuente: Outfit con fallback Helvetica
 */
class PDFGenerator {
  constructor(opts = {}) {
    this.doc = null;
    this._fontsRegistered = false;
    this.accentColor = opts.accentColor || '#5a2fc2';
    this.logo = opts.logo || null;
    this.tipo = opts.tipo || 'general';
  }

  _registerFonts(doc) {
    if (this._fontsRegistered) return;
    try {
      doc.registerFont('Outfit', path.join(FONTS_DIR, 'outfit_regular.ttf'));
      doc.registerFont('Outfit-Bold', path.join(FONTS_DIR, 'outfit_bold.ttf'));
      doc.registerFont('Outfit-Light', path.join(FONTS_DIR, 'outfit_light.ttf'));
      doc.registerFont('Outfit-Black', path.join(FONTS_DIR, 'outfit_black.ttf'));
      this._fontsRegistered = true;
    } catch (e) {
      console.warn('[PDF] Outfit font not available:', e.message);
    }
  }

  _f(bold, light, black) {
    if (black && this._fontsRegistered) return 'Outfit-Black';
    if (bold && this._fontsRegistered) return 'Outfit-Bold';
    if (light && this._fontsRegistered) return 'Outfit-Light';
    if (this._fontsRegistered) return 'Outfit';
    if (bold) return 'Helvetica-Bold';
    if (light) return 'Helvetica-Oblique';
    return 'Helvetica';
  }

  _initDoc(options) {
    this.doc = new PDFDocument({
      size: 'A4',
      margins: { top: 55, bottom: 50, left: 50, right: 50 },
      bufferPages: true,
      info: { Author: 'Grupo de La Placeta', ...(options?.info || {}) }
    });
    this._registerFonts(this.doc);
    this.doc.on('pageAdded', () => this._addFooter());
    return this.doc;
  }

  _header(titulo, subtitulo, codigo) {
    const doc = this.doc;
    doc.save();
    if (this.logo) {
      try {
        const logoBuffer = fs.readFileSync(this.logo);
        doc.image(logoBuffer, 40, 28, { width: 80 });
        doc.font(this._f(true)).fontSize(14).fillColor('#1c005f');
        const logoLabel = this.tipo === 'tributos' ? '  TRIBUTOS GDLP' : '  GRUPO DE LA PLACETA';
        doc.text(logoLabel, 125, 42);
      } catch (e) {
        console.error('[PDF] Logo error:', e.message);
        doc.font(this._f(true)).fontSize(14).fillColor('#1c005f');
        const fallback = this.tipo === 'tributos' ? '🏛️  TRIBUTOS GDLP' : '🏛️  GRUPO DE LA PLACETA';
        doc.text(fallback, 40, 40);
      }
    } else {
      doc.font(this._f(true)).fontSize(14).fillColor('#1c005f');
      const fallback = this.tipo === 'tributos' ? '🏛️  TRIBUTOS GDLP' : '🏛️  GRUPO DE LA PLACETA';
      doc.text(fallback, 40, 40);
    }
    doc.font(this._f(false, true)).fontSize(7).fillColor('#5c5566');
    doc.text(codigo || 'Documento Oficial', 50, 56);
    if (titulo) {
      doc.font(this._f(false, false, true)).fontSize(16).fillColor('#1c005f');
      doc.text(titulo, 50, 78, { width: 500 });
    }
    if (subtitulo) {
      doc.font(this._f(false, true)).fontSize(8.5).fillColor(this.accentColor);
      doc.text(subtitulo, 50, doc.y + 2, { width: 500 });
    }
    const lineY = Math.max(doc.y + 10, 120);
    doc.rect(50, lineY, 500, 1.5).fill(this.accentColor);
    doc.y = lineY + 12;
    doc.restore();
  }

  _addFooter() {
    const doc = this.doc;
    const pg = doc.page;
    const w = pg.width;
    doc.save();
    doc.rect(50, pg.height - 38, w - 100, 0.5).fill(this.accentColor);
    doc.font(this._f(false, true)).fontSize(6).fillColor('#5c5566');
    const footerText = this.tipo === 'tributos' ? 'Tributos de La Placeta \u00B7 Documento oficial' : 'Grupo de La Placeta \u00B7 Documento oficial';
    doc.text(footerText, 50, pg.height - 33, { width: 350 });
    const pgN = doc.bufferedPageRange ? doc.bufferedPageRange().count : 1;
    doc.text(`P\u00E1g. ${pgN}`, w - 100, pg.height - 33, { width: 50, align: 'right' });
    doc.restore();
  }

  _tag(texto) {
    const doc = this.doc;
    doc.save();
    const tw = Math.max(texto.length * 7, 60);
    doc.roundedRect(50, doc.y - 2, tw + 20, 20, 4).fill(this.accentColor);
    doc.font(this._f(true)).fontSize(7.5).fillColor('#ffffff');
    doc.text(` ${texto} `, 50, doc.y, { width: tw + 10, align: 'center' });
    doc.y += 26;
    doc.restore();
  }

  _titulo1(t) {
    const doc = this.doc;
    doc.moveDown(0.3);
    doc.font(this._f(false, false, true)).fontSize(15).fillColor('#1c005f');
    doc.text(t, 50, doc.y, { width: 500 });
    doc.rect(50, doc.y + 2, 500, 2).fill('#1c005f');
    doc.y += 12;
  }

  _titulo2(t) {
    const doc = this.doc;
    doc.moveDown(0.2);
    doc.font(this._f(true)).fontSize(10.5).fillColor('#341087');
    doc.text(t, 50, doc.y, { width: 500 });
    doc.y += 6;
  }

  _titulo3(t) {
    const doc = this.doc;
    doc.moveDown(0.15);
    doc.font(this._f(true)).fontSize(9).fillColor('#1c005f');
    doc.text(t, 50, doc.y, { width: 500 });
    doc.y += 4;
  }

  _texto(t) {
    const doc = this.doc;
    doc.font(this._f()).fontSize(8.5).fillColor('#1c1226');
    doc.text(t, 50, doc.y, { width: 500, align: 'justify' });
    doc.y += 2;
  }

  _nota(t) {
    const doc = this.doc;
    const y = doc.y;
    doc.save();
    doc.rect(50, y, 3, 40).fill(this.accentColor);
    doc.font(this._f(false, true)).fontSize(8).fillColor('#1c1226');
    doc.text(t, 63, y + 3, { width: 478, align: 'justify' });
    doc.y = Math.max(doc.y, y + 18) + 5;
    doc.restore();
  }

  _linea() {
    const doc = this.doc;
    doc.moveTo(50, doc.y).lineTo(550, doc.y).lineWidth(1).strokeColor(this.accentColor).stroke();
    doc.y += 6;
  }

  _campo(n, v) {
    const doc = this.doc;
    doc.font(this._f(true)).fontSize(8).fillColor('#1c1226');
    doc.text(`${n}: `, 52, doc.y, { continued: true });
    doc.font(this._f()).fillColor('#5c5566').text(v || '\u2014');
    doc.y += 2.5;
  }

  _fila(n, v, b) {
    const doc = this.doc;
    const y = doc.y;
    doc.font(b ? this._f(false, false, true) : this._f()).fontSize(8).fillColor(b ? '#1c005f' : '#1c1226');
    doc.text(n, 52, y, { width: 280 });
    doc.font(b ? this._f(false, false, true) : this._f()).fillColor(b ? '#1c005f' : '#5c5566');
    doc.text(v, 340, y, { width: 190, align: 'right' });
    doc.y = y + 12;
  }

  _tabla(headers, rows, cw) {
    const doc = this.doc;
    const sx = 50;
    const tw = 500;
    const cw2 = cw || headers.map(() => tw / headers.length);
    const rh = 17;
    const fs = 7;

    doc.save();
    doc.rect(sx, doc.y, tw, rh).fill('#1c005f');
    doc.font(this._f(true)).fontSize(fs).fillColor('#ffffff');
    let hx = sx + 3;
    headers.forEach((h, i) => { doc.text(h, hx, doc.y + 4, { width: cw2[i] - 6 }); hx += cw2[i]; });
    doc.y += rh;

    rows.forEach((row, ri) => {
      if (ri % 2 === 0) doc.rect(sx, doc.y, tw, rh).fill('#f7f4fd');
      doc.font(this._f()).fontSize(fs).fillColor('#1c1226');
      let rx = sx + 3;
      row.forEach((cell, ci) => { doc.text(String(cell), rx, doc.y + 4, { width: cw2[ci] - 6 }); rx += cw2[ci]; });
      doc.rect(sx, doc.y + rh - 0.5, tw, 0.5).fillColor('#e3dbf5').fill();
      doc.y += rh;
    });
    doc.restore();
  }

  _firma(datos) {
    const doc = this.doc;
    const hash = crypto.createHash('sha256').update(JSON.stringify(datos) + Date.now()).digest('hex');
    this._linea();
    doc.moveDown(0.3);
    doc.font(this._f(true)).fontSize(9).fillColor('#1c005f');
    doc.text('C\u00DAMPLEASE Y NOTIF\u00CDQUESE.', 50, doc.y, { width: 500, align: 'center' });
    doc.moveDown(1.2);
    doc.font(this._f()).fontSize(7).fillColor('#5c5566');
    doc.text('DEPARTAMENTO DE JUSTICIA \u00B7 GRUPO DE LA PLACETA', 50, doc.y, { width: 500, align: 'center' });
    doc.moveDown(1);
    doc.text('Documento generado electr\u00F3nicamente', 50, doc.y, { width: 500, align: 'center' });
    doc.font(this._f(false, true)).fontSize(6).fillColor('#9c7ee6');
    doc.text(`SHA-256: ${hash.substring(0, 20)}...`, 50, doc.y + 10, { width: 500, align: 'center' });
  }

  _footerDoc(leyenda) {
    this._linea();
    const doc = this.doc;
    doc.font(this._f(false, true)).fontSize(6.5).fillColor('#5c5566');
    doc.text(leyenda || 'Documento oficial del Grupo de La Placeta \u00B7 C\u00F3digo Normativo Interno', 50, doc.y, { width: 500, align: 'center' });
    doc.moveDown(0.5);
    doc.text(`Generado el ${new Date().toLocaleString('es-ES')}`, 50, doc.y, { width: 500, align: 'center' });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  MODELOS DE DOCUMENTOS
  // ══════════════════════════════════════════════════════════════════════════

  generarInicioExpediente(d) {
    this._initDoc({ info: { Title: `Inicio Exp. ${d.ID_EXPEDIENTE}`, Subject: 'Inicio Expediente' } });
    this._header('ACUERDO DE INICIO DE EXPEDIENTE DISCIPLINARIO', 'Pliego de Cargos \u00B7 GDLP-JUS-001', 'GDLP-JUS-001');
    this._tag('GDLP-JUS-001');
    this._titulo2('Datos del expediente');
    this._campo('Ref. Expediente', d.ID_EXPEDIENTE);
    this._campo('Fecha', d.FECHA_SISTEMA || new Date().toLocaleDateString('es-ES'));
    this._campo('Infractor', `${d.ALIAS_INFRACTOR} \u00B7 ${d.DIP_N\u00DAMERO}`);
    this._campo('Infracci\u00F3n', d.TIPO_INFRACCION);
    this._titulo2('Descripci\u00F3n de los hechos');
    this._texto(d.DESCRIPCI\u00D3N_HECHOS || 'Seg\u00FAn consta en las actuaciones...');
    this._titulo2('Pliego de cargos');
    this._texto('Conforme al Cap\u00EDtulo X del C\u00F3digo Normativo Interno, se concede al presunto infractor un plazo de 5 d\u00EDas h\u00E1biles para presentar alegaciones.');
    this._nota('Plazo: 5 d\u00EDas h\u00E1biles desde la notificaci\u00F3n. Transcurrido el plazo sin alegaciones, se dictar\u00E1 resoluci\u00F3n.');
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap\u00EDtulo X \u2014 R\u00E9gimen Sancionador');
    return this.doc;
  }

  generarResolucionSancionadora(d) {
    this._initDoc({ info: { Title: `Resoluci\u00F3n ${d.ID_EXPEDIENTE}`, Subject: 'Resoluci\u00F3n Sancionadora' } });
    this._header('RESOLUCI\u00D3N SANCIONADORA', 'Primera Instancia \u00B7 GDLP-JUS-003', 'GDLP-JUS-003');
    this._tag('GDLP-JUS-003');
    this._titulo2('Referencia');
    this._campo('Expediente', d.ID_EXPEDIENTE);
    this._campo('Fecha', d.FECHA_SISTEMA || new Date().toLocaleDateString('es-ES'));
    this._campo('Infractor', `${d.ALIAS_INFRACTOR} \u00B7 ${d.DIP_N\u00DAMERO}`);
    this._campo('Gravedad', d.GRAVEDAD_INFRACCION);
    this._titulo2('Antecedentes');
    this._texto(`Iniciado expediente en fecha ${d.FECHA_INICIO_EXP || '\u2014'} por: ${d.DESCRIPCI\u00D3N_CORTA_HECHOS || 'conducta tipificada'}.`);
    this._texto(d.ALEGACIONES_PRESENTADAS !== false ? '\u2713 Present\u00F3 alegaciones en tiempo y forma.' : '\u2717 No present\u00F3 alegaciones.');
    this._titulo2('Fundamentos');
    this._texto('PRIMERO.- Competencia del Departamento de Justicia conforme al Cap\u00EDtulo X del C\u00F3digo Normativo Interno.');
    this._texto(`SEGUNDO.- Los hechos constituyen infracci\u00F3n de car\u00E1cter: ${d.GRAVEDAD_INFRACCION || 'Leve'}.`);
    this._texto(`TERCERO.- Conducta tipificada en: "${d.CONDUCTA_TIPICA_NORMATIVA || 'Art. 20 del C\u00F3digo Normativo'}".`);
    this._titulo2('Parte dispositiva');
    this._texto(`Se impone a ${d.ALIAS_INFRACTOR}: Sanci\u00F3n econ\u00F3mica: ${d.CUANT\u00CDA_MULTA_PZ || '0'} Pz. Suspensi\u00F3n: ${d.D\u00CDAS_SUSPENSI\u00D3N || '0'} d\u00EDas. Reincidencia: ${d.EFECTO_REINCIDENCIA || 'Muy Grave'}.`);
    this._titulo2('Recursos');
    this._nota(`Plazo de apelaci\u00F3n ante la Junta Directiva: ${d.FECHA_L\u00CDMITE_APELACI\u00D3N || '\u2014'}.`);
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap\u00EDtulo X \u2014 R\u00E9gimen Sancionador');
    return this.doc;
  }

  generarDecretoExpulsion(d) {
    this._initDoc({ info: { Title: `Expulsi\u00F3n ${d.ID_EXPEDIENTE}`, Subject: 'Decreto de Expulsi\u00F3n' } });
    this._header('DECRETO DE EXPULSI\u00D3N DEFINITIVA', 'Confiscaci\u00F3n Monetaria \u00B7 GDLP-JUS-004', 'GDLP-JUS-004');
    this._tag('GDLP-JUS-004');
    this._campo('Expediente', d.ID_EXPEDIENTE);
    this._campo('Infractor', d.ALIAS_INFRACTOR);
    this._campo('Saldo Confiscado', `${d.SALDO_CONFISCADO} Pz`);
    this._campo('Hash Quema', d.HASH_QUEMA_MONEDA);
    this._texto('Agotada la v\u00EDa interna sin apelaci\u00F3n, se procede a: expulsi\u00F3n definitiva, confiscaci\u00F3n total del saldo, quema irreversible y lista negra del sistema.');
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap\u00EDtulo X');
    return this.doc;
  }

  generarRequerimientoDescubierto(d) {
    this._initDoc({ info: { Title: `Requerimiento ${d.DIP_N\u00DAMERO}`, Subject: 'Requerimiento Descubierto' } });
    this._header('REQUERIMIENTO DE REGULARIZACI\u00D3N', 'Descubierto Bancario \u00B7 GDLP-HAC-010', 'GDLP-HAC-010');
    this._tag('GDLP-HAC-010');
    this._campo('Ciudadano', `${d.ALIAS_INFRACTOR} \u00B7 ${d.DIP_N\u00DAMERO}`);
    this._campo('Saldo Negativo', `${d.SALDO_NEGATIVO} Pz`);
    this._campo('Sanci\u00F3n', `${d.SANCI\u00D3N_APLICADA_PZ} Pz`);
    this._texto('Conforme al Cap\u00EDtulo IV del C\u00F3digo Normativo, se requiere regularizar la situaci\u00F3n financiera en 30 d\u00EDas. Transcurrido el plazo, se derivar\u00E1 al Departamento de Justicia.');
    this._nota('El incumplimiento dar\u00E1 lugar a expediente disciplinario por infracci\u00F3n econ\u00F3mica.');
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap\u00EDtulo IV \u2014 Banca, Capital e Impuestos');
    return this.doc;
  }

  generarCertificadoBaja(d) {
    this._initDoc({ info: { Title: `Baja ${d.DIP_REVOCADO}`, Subject: 'Certificado de Baja' } });
    this._header('CERTIFICADO DE BAJA VOLUNTARIA', 'Revocaci\u00F3n de Identidad \u00B7 GDLP-SEC-020', 'GDLP-SEC-020');
    this._tag('GDLP-SEC-020');
    this._campo('Ciudadano', d.ALIAS_CIUDADANO);
    this._campo('DIP Revocado', d.DIP_REVOCADO);
    this._campo('Saldo Liquidado', `${d.SALDO_LIQUIDADO_RETORNADO} Pz`);
    this._texto('Se certifica la baja voluntaria del ciudadano, revocando su PlacetaID, DIP y cuenta bancaria.');
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap\u00EDtulo II');
    return this.doc;
  }

  generarCertificadoARCO(d) {
    this._initDoc({ info: { Title: `ARCO ${(d.HASH_ANONIMIZACION||'').substring(0,12)}`, Subject: 'Certificado ARCO' } });
    this._header('CERTIFICADO DE DERECHOS ARCO', 'Anonimizaci\u00F3n de Datos \u00B7 GDLP-SEC-021', 'GDLP-SEC-021');
    this._tag('GDLP-SEC-021');
    this._campo('Titular', d.NOMBRE_REAL_AFECTADO || '[ANONIMIZADO]');
    this._campo('Hash', d.HASH_ANONIMIZACION);
    this._campo('Restricciones', d.LOG_RESTRICCION_DE_DATOS || 'Ninguna');
    this._nota('Anonimizaci\u00F3n irreversible conforme al RGPD (UE) 2016/679 y LOPDGDD 3/2018.');
    this._firma(d);
    this._footerDoc('C\u00F3digo Normativo Interno \u00B7 Cap. XI y XV');
    return this.doc;
  }

  // ── Tributos ──────────────────────────────────────────────────────────────

  generarDeclaracionTributaria(d) {
    this._initDoc({ info: { Title: `Declaraci\u00F3n ${d.ID_DECLARACION||d.mes_periodo}`, Subject: 'Declaraci\u00F3n Tributaria' } });
    const esBorrador = d.estado_pago === 'Borrador' || !d.estado_pago;
    this._header(
      esBorrador ? 'BORRADOR · DECLARACI\u00D3N TRIBUTARIA' : 'DECLARACI\u00D3N TRIBUTARIA MENSUAL',
      `Periodo: ${d.mes_periodo||'\u2014'} \u00B7 ${d.ID_DECLARACION||''}`,
      'TLP-DEC-001'
    );
    if (esBorrador) {
      this.doc.fontSize(48).font('Helvetica-Bold').fillColor('#ff000033')
        .text('BORRADOR', { align: 'center', underline: false });
      this.doc.fillColor('#000000');
    }
    this._tag('TLP-DEC-001');
    this._titulo2('Contribuyente');
    this._campo('Placeta ID', d.placeta_id); this._campo('Nombre', d.nombre);
    this._campo('DIP', d.dip); this._campo('Tipo', d.tipo_sujeto); this._campo('Cuenta BLP', d.cuenta_id_blp);

    // ── Patrimonio y Saldos Diarios ──
    this._titulo2('Patrimonio y Saldos');
    this._fila('Patrimonio Medio Mensual', `${Number(d.patrimonio_medio||0).toLocaleString()} Pz`);
    this._fila('Índice de Acumulación (IA)', `${Number(d.indice_acumulacion||0).toFixed(4)}`);
    this._fila('Días con datos del banco', `${d.dias_declarados_banco||0} de ${d.dias_activos_mes||30}`);
    this._fila('Días reconstruidos (CRM)', `${d.dias_reconstruidos_crm||0}`);

    // ── Desglose IRM ──
    const pm = Number(d.patrimonio_medio || 0);
    const SALDO_MAX = 500000;
    const exceso = Math.max(0, pm - SALDO_MAX);
    const ratio = SALDO_MAX > 0 ? exceso / SALDO_MAX : 0;
    let tasaIRM = 0, irmDesc = 'No aplica';
    if (pm <= SALDO_MAX) {
      irmDesc = 'Patrimonio dentro del límite legal (≤ 500.000 Pz)';
    } else {
      if (ratio <= 0.5) tasaIRM = 0.05;
      else if (ratio <= 1.0) tasaIRM = 0.10;
      else if (ratio <= 2.0) tasaIRM = 0.20;
      else tasaIRM = 0.35;
      irmDesc = `${(tasaIRM * 100).toFixed(0)}% sobre exceso de ${exceso.toLocaleString()} Pz`;
    }
    const irmCalc = Number(d.cuota_irm || 0);

    this._titulo2('IRM · Impuesto sobre la Renta de La Placeta');
    this._fila('Límite exento', '500.000 Pz');
    this._fila('Patrimonio Medio', `${pm.toLocaleString()} Pz`);
    this._fila('Exceso sobre límite', `${exceso.toLocaleString()} Pz`);
    this._fila('Ratio exceso/límite', `${ratio.toFixed(4)}`);
    this._fila('Tasa aplicable', `${(tasaIRM * 100).toFixed(2)}%`);
    this._fila('Cálculo', irmDesc);
    this._fila('Cuota IRM', `${irmCalc.toLocaleString()} Pz`);

    // ── Desglose IGF ──
    const igfCalc = Number(d.cuota_igf || 0);
    this._titulo2('IGF · Impuesto General sobre el Patrimonio');
    this._fila('Base imponible (Patrimonio Medio)', `${pm.toLocaleString()} Pz`);
    this._fila('Tipo impositivo', '1.5%');
    this._fila('Cálculo', `${pm.toLocaleString()} Pz × 1.5% = ${igfCalc.toLocaleString()} Pz`);
    this._fila('Cuota IGF', `${igfCalc.toLocaleString()} Pz`);

    // ── Totales ──
    this._titulo2('Resumen');
    this._fila('Total IRM + IGF', `${(irmCalc + igfCalc).toLocaleString()} Pz`);
    this._fila('Exención aplicada', d.exencion_aplicada ? 'Sí' : 'No');
    this._fila('Estado', esBorrador ? 'BORRADOR · Pendiente de aprobación' : (d.estado_pago || '—'));

    this._titulo2('Control');
    this._campo('ID Declaración', d.id || d.ID_DECLARACION || '—');
    this._campo('Bypass Junta', d.bypass_junta_directiva ? 'Sí' : 'No');
    if (d.id_permiso_junta) this._campo('ID Permiso', d.id_permiso_junta);
    const hash = d.pdf_hash || crypto.createHash('sha256').update(JSON.stringify(d)+Date.now()).digest('hex');
    this._footerDoc(`CSV: ${d.csv||hash.substring(0,16).toUpperCase()} \u00B7 Tributos de La Placeta (TLP) · ${esBorrador ? 'BORRADOR' : 'Documento Oficial'}`);
    return this.doc;
  }

  generarFacturaTributaria(d) {
    this._initDoc({ info: { Title: `Factura ${d.numero_factura}`, Subject: 'Factura TLP' } });
    this._header('FACTURA CON IVA', `N\u00BA ${d.numero_factura} \u00B7 ${d.csv_verificacion||''}`, 'TLP-FAC-001');
    this._tag('TLP-FAC-001');
    this._titulo2('Partes');
    this._campo('Emisor', d.emisor_placeta_id); this._campo('Receptor', d.receptor_placeta_id);
    this._campo('Fecha', d.fecha_emision ? new Date(d.fecha_emision).toLocaleString('es-ES') : '\u2014');
    if (d.transaction_id_blp) this._campo('ID BLP', d.transaction_id_blp);
    this._titulo2('Importes');
    this._fila('Base Imponible', `${Number(d.base_imponible||0).toLocaleString()} Pz`);
    this._fila('IVA 12%', `${Number(d.total_iva||0).toLocaleString()} Pz`);
    this._linea();
    this._fila('TOTAL', `${Number(d.total_factura||0).toLocaleString()} Pz`, true);
    if (d.lineas?.length) {
      this._titulo2('Detalle');
      this._tabla(['Concepto','Cant.','Unitario','Neto','IVA','Total'],
        d.lineas.map(l=>[l.concepto_producto||'\u2014',String(l.cantidad||0),Number(l.precio_unitario||0).toLocaleString(),Number(l.subtotal_neto||0).toLocaleString(),Number(l.subtotal_iva||0).toLocaleString(),Number((l.subtotal_neto||0)+(l.subtotal_iva||0)).toLocaleString()]),
        [130,35,55,55,55,60]);
    }
    this._footerDoc(`CSV: ${d.csv_verificacion||'\u2014'} \u00B7 Tributos de La Placeta \u00B7 IVA 12%`);
    return this.doc;
  }

  generarRectificacionTributaria(d) {
    this._initDoc({ info: { Title: `Rectif. ${d.ID_RECTIFICACION||d.id}`, Subject: 'Rectificaci\u00F3n Tributaria' } });
    this._header('RECTIFICACI\u00D3N Y REAJUSTE DE OFICIO', `Ref: ${d.ID_RECTIFICACION||d.id||'\u2014'}`, 'TLP-REC-001');
    this._tag('TLP-REC-001');
    this._campo('Decl. Original', d.declaracion_original_id); this._campo('Placeta ID', d.placeta_id);
    this._campo('Fecha', d.fecha_rectificacion ? new Date(d.fecha_rectificacion).toLocaleString('es-ES') : new Date().toLocaleString('es-ES'));
    this._titulo2('Delta (\u0394)');
    const delta = Number(d.diferencia_delta||0);
    this._fila('Cuota Provisional', `${Number(d.cuota_provisional_cobrada||0).toLocaleString()} Pz`);
    this._fila('Cuota Real', `${Number(d.cuota_real_calculada||0).toLocaleString()} Pz`);
    this._linea();
    this._fila(delta < 0 ? 'A Reembolsar' : 'A Cobrar', `${Math.abs(delta).toLocaleString()} Pz`, true);
    this._campo('Estado', d.estado_ajuste);
    if (d.signature_sha256) this._texto(`SHA-256: ${d.signature_sha256}`);
    this._footerDoc('Rectificaci\u00F3n Art. 4.16 ter \u00B7 Tributos de La Placeta');
    return this.doc;
  }

  // ── Manual / Documento genérico ────────────────────────────────────────────
  generarManual(titulo, contenido, meta) {
    meta = meta || {};
    this._initDoc({
      margins: { top: 55, bottom: 50, left: 50, right: 50 },
      info: { Title: titulo || 'Manual', Author: meta.autor || 'GDLP', Subject: meta.asunto || 'Manual', Keywords: meta.palabras_clave || 'GDLP' }
    });
    this._header(titulo, meta.subtitulo, `v${meta.version||'1.0'}`);

    if (typeof contenido === 'string') {
      this._renderHtml(contenido);
    } else if (Array.isArray(contenido)) {
      for (const s of contenido) {
        if (s.tipo === 'tag') this._tag(s.texto||s.etiqueta||'');
        else if (s.tipo === 'h1'||s.tipo === 'capitulo') this._titulo1(s.titulo||s.texto||'');
        else if (s.tipo === 'h2'||s.tipo === 'seccion') this._titulo2(s.titulo||s.texto||'');
        else if (s.tipo === 'h3'||s.tipo === 'articulo') this._titulo3(s.titulo||s.texto||'');
        else if (s.tipo === 'texto'||s.tipo === 'p') this._texto(s.contenido||s.texto||'');
        else if (s.tipo === 'nota') this._nota(s.contenido||s.texto||'');
        else if (s.tipo === 'campo') this._campo(s.nombre||s.label||'', s.valor||'');
        else if (s.tipo === 'fila') this._fila(s.nombre||s.label||'', s.valor||'', s.bold);
        else if (s.tipo === 'tabla') this._tabla(s.headers||[], s.rows||[], s.colWidths);
        else if (s.tipo === 'linea') this._linea();
        else if (s.tipo === 'firma') this._firma(s.datos||{});
        else if (s.contenido) this._texto(s.contenido);
      }
    }
    this._footerDoc(meta.footer || `${titulo} \u00B7 Grupo de La Placeta`);
    return this.doc;
  }

  _renderHtml(html) {
    // Extraer bloques con regex en lugar de split por líneas
    const blockRegex = /<(h[1-3]|p|tag|note)[^>]*>[\s\S]*?<\/\1>/gi;
    let lastIdx = 0;
    const blocks = [];
    let match;
    while ((match = blockRegex.exec(html)) !== null) {
      if (match.index > lastIdx) {
        const textBetween = html.slice(lastIdx, match.index).trim();
        if (textBetween) blocks.push({ type: 'text', content: textBetween });
      }
      const tagName = match[1];
      const inner = match[0].replace(/<[^>]*>/g, '').trim();
      if (inner) blocks.push({ type: tagName, content: inner });
      lastIdx = blockRegex.lastIndex;
    }
    if (lastIdx < html.length) {
      const rest = html.slice(lastIdx).trim();
      if (rest) blocks.push({ type: 'text', content: rest.replace(/<[^>]*>/g, '').trim() });
    }

    if (blocks.length === 0) {
      // Fallback: sin etiquetas HTML, tratar como texto plano
      const plain = html.replace(/<[^>]*>/g, '').trim();
      if (plain) this._texto(plain);
      return;
    }

    for (const b of blocks) {
      const content = b.content;
      if (!content) continue;
      if (b.type === 'h1') this._titulo1(content);
      else if (b.type === 'h2') this._titulo2(content);
      else if (b.type === 'h3') this._titulo3(content);
      else if (b.type === 'tag') this._tag(content);
      else if (b.type === 'note') this._nota(content);
      else this._texto(content);
    }
  }
}

export default PDFGenerator;
