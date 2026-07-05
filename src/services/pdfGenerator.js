import PDFDocument from 'pdfkit';
import crypto from 'crypto';

/**
 * Motor de Generación de Documentos PDF
 * Basado en la Normativa Institucional Unificada v5.0
 * Catálogo de modelos: GDLP-JUS-001 a 004, GDLP-HAC-010 a 011, GDLP-SEC-020 a 021
 */

class PDFGenerator {
  constructor() {
    this.doc = null;
  }

  /**
   * Generar un PDF de resolución sancionadora (GDLP-JUS-003)
   */
  generarResolucionSancionadora(datos) {
    this.doc = new PDFDocument({
      size: 'A4',
      margins: { top: 50, bottom: 50, left: 60, right: 60 },
      info: {
        Title: `Resolución Sancionadora - ${datos.ID_EXPEDIENTE}`,
        Author: 'Departamento de Justicia - Grupo de La Placeta',
        Subject: 'Resolución Sancionadora en Primera Instancia',
        Keywords: 'GDLP-JUS-003, resolución, sanción, expediente'
      }
    });

    this._agregarEncabezadoOficial();
    this._agregarReferencia(datos);
    this._agregarAntecedentes(datos);
    this._agregarFundamentosDerecho(datos);
    this._agregarParteDispositiva(datos);
    this._agregarMedidasCautelares(datos);
    this._agregarRegimenRecursos(datos);
    this._agregarFirmaDigital(datos);

    return this.doc;
  }

  /**
   * Generar PDF de Acuerdo de Inicio de Expediente (GDLP-JUS-001)
   */
  generarInicioExpediente(datos) {
    this.doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
    this._agregarEncabezadoOficial();
    this.doc.fontSize(14).font('Helvetica-Bold').text('ACUERDO DE INICIO DE EXPEDIENTE DISCIPLINARIO Y PLIEGO DE CARGOS', { align: 'center' });
    this.doc.moveDown(2);
    this._agregarCampo('Ref. Expediente', datos.ID_EXPEDIENTE);
    this._agregarCampo('Fecha de Emisión', datos.FECHA_SISTEMA || new Date().toLocaleDateString('es-ES'));
    this._agregarCampo('Presunto Infractor', `${datos.ALIAS_INFRACTOR} · DIP: ${datos.DIP_NÚMERO}`);
    this._agregarCampo('Tipo de Infración', datos.TIPO_INFRACCION);
    this.doc.moveDown();
    this.doc.fontSize(10).font('Helvetica').text('DESCRIPCIÓN DE LOS HECHOS:', { underline: true });
    this.doc.moveDown(0.5);
    this.doc.text(datos.DESCRIPCIÓN_HECHOS || 'Según consta en las actuaciones...');
    this.doc.moveDown();
    this._agregarFirmaDigital(datos);
    return this.doc;
  }

  /**
   * Generar PDF de Decreto de Expulsión (GDLP-JUS-004)
   */
  generarDecretoExpulsion(datos) {
    this.doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
    this._agregarEncabezadoOficial();
    this.doc.fontSize(16).font('Helvetica-Bold').fillColor('#cc0000').text('DECRETO DE EXPULSIÓN DEFINITIVA Y CONFISCACIÓN MONETARIA', { align: 'center' });
    this.doc.fillColor('#000000');
    this.doc.moveDown(2);
    this._agregarCampo('Ref. Expediente', datos.ID_EXPEDIENTE);
    this._agregarCampo('Infractor', datos.ALIAS_INFRACTOR);
    this._agregarCampo('Saldo Confiscado', `${datos.SALDO_CONFISCADO} Pz`);
    this._agregarCampo('Hash de Quema', datos.HASH_QUEMA_MONEDA);
    this._agregarCampo('Estado de la Cuenta', datos.ESTADO_CUENTA || 'EXPULSADO_PERMANENTE');
    this.doc.moveDown();
    this.doc.fontSize(10).font('Helvetica');
    this.doc.text('Visto el expediente disciplinario de referencia, y habiendo agotado la vía interna sin que el infractor haya ejercido su derecho de apelación en el plazo establecido, se procede a:');
    this.doc.moveDown();
    this.doc.list([
      'La expulsión definitiva e irrevocable del ecosistema virtual "La Placeta".',
      'La confiscación total del saldo bancario del infractor.',
      `La quema irreversible de ${datos.SALDO_CONFISCADO} Pz del suministro monetario circulante.`,
      'La inclusión del infractor en la lista negra del sistema, impidiendo cualquier nuevo registro futuro.'
    ]);
    this.doc.moveDown();
    this._agregarFirmaDigital(datos);
    return this.doc;
  }

  /**
   * Generar PDF de Requerimiento de Regularización (GDLP-HAC-010)
   */
  generarRequerimientoDescubierto(datos) {
    this.doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
    this._agregarEncabezadoOficial();
    this.doc.fontSize(14).font('Helvetica-Bold').text('REQUERIMIENTO DE REGULARIZACIÓN POR DESCUBIERTO BANCARIO DE CUENTA', { align: 'center' });
    this.doc.moveDown(2);
    this._agregarCampo('Ciudadano', datos.ALIAS_INFRACTOR);
    this._agregarCampo('DIP', datos.DIP_NÚMERO);
    this._agregarCampo('Saldo Negativo Actual', `${datos.SALDO_NEGATIVO} Pz`);
    this._agregarCampo('Sanción Aplicada', `${datos.SANCIÓN_APLICADA_PZ} Pz`);
    this.doc.moveDown();
    this.doc.fontSize(10).font('Helvetica').text('De conformidad con el Capítulo IV de la Normativa Institucional Unificada v5.0, y habiendo transcurrido el período de gracia de 5 días, se requiere al ciudadano para que regularice su situación financiera en el plazo máximo de 30 días, transcurridos los cuales se derivará el caso al Departamento de Justicia.');
    this.doc.moveDown();
    this._agregarFirmaDigital(datos);
    return this.doc;
  }

  /**
   * Generar PDF de Certificado de Baja (GDLP-SEC-020)
   */
  generarCertificadoBaja(datos) {
    this.doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
    this._agregarEncabezadoOficial();
    this.doc.fontSize(14).font('Helvetica-Bold').text('RESOLUCIÓN DE CERTIFICACIÓN DE BAJA VOLUNTARIA Y REVOCACIÓN DE IDENTIDAD', { align: 'center' });
    this.doc.moveDown(2);
    this._agregarCampo('Ciudadano', datos.ALIAS_CIUDADANO);
    this._agregarCampo('DIP Revocado', datos.DIP_REVOCADO);
    this._agregarCampo('Saldo Liquidado', `${datos.SALDO_LIQUIDADO_RETORNADO} Pz`);
    this.doc.moveDown();
    this._agregarFirmaDigital(datos);
    return this.doc;
  }

  /**
   * Generar PDF de Certificado ARCO (GDLP-SEC-021)
   */
  generarCertificadoARCO(datos) {
    this.doc = new PDFDocument({ size: 'A4', margins: { top: 50, bottom: 50, left: 60, right: 60 } });
    this._agregarEncabezadoOficial();
    this.doc.fontSize(14).font('Helvetica-Bold').text('CERTIFICADO DE EJERCICIO DE DERECHOS ARCO Y ANONIMIZACIÓN DE DATOS REALES', { align: 'center' });
    this.doc.moveDown(2);
    this._agregarCampo('Titular', datos.NOMBRE_REAL_AFECTADO || '[DATOS ANONIMIZADOS]');
    this._agregarCampo('Hash de Anonimización', datos.HASH_ANONIMIZACION);
    this._agregarCampo('Restricciones Aplicadas', datos.LOG_RESTRICCION_DE_DATOS || 'Ninguna');
    this.doc.moveDown();
    this.doc.fontSize(9).font('Helvetica-Oblique').text('Este certificado acredita que se ha ejecutado el procedimiento de anonimización irreversible conforme al Reglamento General de Protección de Datos (RGPD) y la LOPDGDD.');
    this.doc.moveDown();
    this._agregarFirmaDigital(datos);
    return this.doc;
  }

  // ── Métodos Privados ─────────────────────────────────────────────────────

  _agregarEncabezadoOficial() {
    const doc = this.doc;
    doc.fontSize(8).font('Helvetica').fillColor('#666666');
    doc.text('CÓDIGO DE MODELO: GDLP-JUS-003', { align: 'right' });
    doc.moveDown(0.5);
    doc.fontSize(16).font('Helvetica-Bold').fillColor('#1a1a2e').text('DEPARTAMENTO DE JUSTICIA', { align: 'center' });
    doc.fontSize(10).font('Helvetica').fillColor('#555555').text('Ecosistema Virtual "La Placeta" · Asociación Grupo de La Placeta', { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(8).font('Helvetica').fillColor('#999999').text('NIF: G-12345678 · Normativa v5.0', { align: 'center' });
    doc.moveDown();
    this._dibujarLinea();
    doc.moveDown();
  }

  _dibujarLinea() {
    this.doc.moveTo(60, this.doc.y)
      .lineTo(535, this.doc.y)
      .strokeColor('#1a1a2e')
      .stroke();
  }

  _agregarReferencia(datos) {
    const doc = this.doc;
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text(`Ref. Expediente Disciplinario: ${datos.ID_EXPEDIENTE}`);
    doc.text(`Fecha de Emisión: ${datos.FECHA_SISTEMA || new Date().toLocaleDateString('es-ES')}`);
    doc.text(`Destinatario: ${datos.ALIAS_INFRACTOR} · DIP: ${datos.DIP_NÚMERO}`);
    doc.text(`Modalidad de Alta: ${datos.MODALIDAD_ALTA || 'Estándar'}`);
    doc.moveDown();
    this._dibujarLinea();
    doc.moveDown();
  }

  _agregarAntecedentes(datos) {
    const doc = this.doc;
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#1a1a2e').text('ANTECEDENTES DE HECHO', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text(`Que en fecha ${datos.FECHA_INICIO_EXP || '—'}, este órgano instructor procedió a la apertura de expediente de oficio debido a la detección en los logs del sistema de la siguiente conducta tipificada en la Normativa Unificada v5.0:`);
    doc.moveDown(0.5);
    doc.font('Helvetica-Oblique').text(`> El ciudadano incurrió en: ${datos.DESCRIPCIÓN_CORTA_HECHOS || 'conductas contrarias a la normativa'}.`);
    doc.moveDown(0.5);
    doc.font('Helvetica').text('Habiéndose notificado el preceptivo Pliego de Cargos, y habiendo transcurrido el plazo estatutario de cinco (5) días hábiles otorgados para la presentación de descargos:');
    doc.moveDown(0.3);
    doc.text(datos.ALEGACIONES_PRESENTADAS !== false ? '[X] El presunto infractor presentó escrito de alegaciones en tiempo y forma.' : '[ ] El presunto infractor NO presentó alegaciones, precluyendo su derecho.');
    doc.moveDown();
  }

  _agregarFundamentosDerecho(datos) {
    const doc = this.doc;
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#1a1a2e').text('FUNDAMENTOS DE DERECHO', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text('PRIMERO.- Compete al Departamento de Justicia del Grupo de La Placeta la instrucción y resolución de los expedientes incoados por infracciones a la convivencia, la economía del sistema o el uso ilegítimo de recursos digitales de la asociación, conforme al Capítulo X, Artículo 21 del texto legal vigente.');
    doc.moveDown(0.3);
    doc.text(`SEGUNDO.- Los hechos probados y analizados durante la fase instructora constituyen de manera inequívoca una INFRACCIÓN de carácter: ${datos.GRAVEDAD_INFRACCION || 'Leve'} (Leve / Grave / Muy Grave).`);
    doc.moveDown(0.3);
    doc.text(`TERCERO.- Se ha constatado la concurrencia de los preceptos tipificados en el Art. 20 de la Normativa Unificada, encuadrándose la conducta específicamente bajo el supuesto de: "${datos.CONDUCTA_TIPICA_NORMATIVA || 'conducta contraria a la normativa de convivencia'}".`);
    doc.moveDown();
  }

  _agregarParteDispositiva(datos) {
    const doc = this.doc;
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#1a1a2e').text('PARTE DISPOSITIVA / RESOLUCIÓN', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text(`Vistos los preceptos normativos aplicables, este Juez del Departamento de Justicia resuelve imponer a ${datos.ALIAS_INFRACTOR} las siguientes medidas disciplinarias y correctivas de aplicación directa en el backend del CRM:`);
    doc.moveDown(0.5);
    doc.list([
      `SANCIÓN ECONÓMICA: Se ordena un débito forzado inmediato en su cuenta bancaria personal por el importe de ${datos.CUANTÍA_MULTA_PZ || '0'} Placetas (Pz).`,
      `SUSPENSIÓN DE DERECHOS: Se inhabilita temporalmente el pasaporte digital PlacetaID del usuario, lo que implica la restricción absoluta de accesos a los sistemas, canales y cargos durante un período ininterrumpido de ${datos.DÍAS_SUSPENSIÓN || '0'} días naturales a contar desde la firma de este acto.`,
      `ADVERTENCIA DE REINCIDENCIA: Se notifica formalmente al sancionado que, en virtud del Art. 20, la reiteración en conductas tipificadas como ${datos.GRAVEDAD_INFRACCION || 'leves'} conllevará de forma automática la elevación de la sanción a la categoría de: ${datos.EFECTO_REINCIDENCIA || 'Muy Grave, pudiendo conllevar la expulsión definitiva'}.`
    ]);
    doc.moveDown();
  }

  _agregarMedidasCautelares(datos) {
    const doc = this.doc;
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#1a1a2e').text('MEDIDAS CAUTELARES', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text(datos.MEDIDAS_RATIFICADAS ? '[X] Se ratifican las medidas cautelares adoptadas en el modelo GDLP-JUS-002, procediéndose a su conversión en definitivas.' : '[ ] Se levantan las medidas cautelares previas con efectos desde el día de hoy.');
    doc.moveDown();
  }

  _agregarRegimenRecursos(datos) {
    const doc = this.doc;
    doc.fontSize(11).font('Helvetica-Bold').fillColor('#1a1a2e').text('RÉGIMEN DE RECURSOS', { underline: true });
    doc.moveDown(0.5);
    doc.fontSize(9).font('Helvetica').fillColor('#333333');
    doc.text('Contra la presente resolución en primera instancia, que no agota la vía asociativa interna, el integrante podrá interponer Recurso de Apelación ante la Junta Directiva del Grupo de La Placeta utilizando el botón del CRM habilitado al efecto.');
    doc.moveDown(0.3);
    doc.font('Helvetica-Bold').text(`El plazo inalterable para interponer dicho recurso expira el día: ${datos.FECHA_LÍMITE_APELACIÓN || '—'} (inclusive). Transcurrido dicho término sin que se haya registrado actividad recursiva, la sanción devendrá firme, inmutable e histórica en el DIP del ciudadano.`);
    doc.moveDown();
    this._dibujarLinea();
    doc.moveDown();
    doc.fontSize(11).font('Helvetica-Bold').text('CÚMPLASE Y NOTIFÍQUESE.', { align: 'center' });
    doc.moveDown();
  }

  _agregarFirmaDigital(datos) {
    const doc = this.doc;
    doc.moveDown();
    doc.fontSize(8).font('Helvetica').fillColor('#666666');
    const hash = crypto.createHash('sha256').update(JSON.stringify(datos) + Date.now()).digest('hex');
    doc.text(`[ Huella Digital Criptográfica: ${hash.substring(0, 20)}... ]`, { align: 'center' });
    doc.moveDown(0.3);
    doc.text('DEPARTAMENTO DE JUSTICIA · ASOCIACIÓN GRUPO DE LA PLACETA', { align: 'center' });
    doc.text('Documento generado electrónicamente · Código de verificación en CRM', { align: 'center', fontSize: 7 });
  }

  _agregarCampo(nombre, valor) {
    this.doc.fontSize(9).font('Helvetica-Bold').fillColor('#333333');
    this.doc.text(`${nombre}: `, { continued: true });
    this.doc.font('Helvetica').fillColor('#555555').text(valor || '—');
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  TRIBUTOS DE LA PLACETA — Documentos con estilo Código Normativo
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Generar PDF de Declaración Tributaria Mensual (TLP-DEC-001)
   * Estilo basado en Código Normativo Interno (codigo.html)
   */
  generarDeclaracionTributaria(datos) {
    this.doc = new PDFDocument({
      size: 'A4',
      margins: { top: 60, bottom: 50, left: 55, right: 55 },
      info: {
        Title: `Declaración Tributaria - ${datos.ID_DECLARACION || datos.mes_periodo}`,
        Author: 'Tributos de La Placeta (TLP)',
        Subject: 'Declaración Mensual IRM e IGF',
        Keywords: 'TLP, tributación, IRM, IGF, declaración'
      }
    });
    this._estiloCodigoNormativo();
    this.doc.moveDown(2);

    // Título
    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica-Bold');
    this.doc.text('TRIBUTOS DE LA PLACETA', { align: 'center' });
    this.doc.moveDown(0.3);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica');
    this.doc.text('Sistema de Control Fiscal · Acoplado al Banco de La Placeta (BLP)', { align: 'center' });
    this.doc.moveDown(0.5);
    this._tributosLinea('#5a2fc2');
    this.doc.moveDown(1.5);

    this.doc.fontSize(16).fillColor('#1c005f').font('Helvetica-Bold');
    this.doc.text('DECLARACIÓN TRIBUTARIA MENSUAL', { align: 'center' });
    this.doc.moveDown(0.5);
    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica');
    this.doc.text(`Periodo: ${datos.mes_periodo || '—'} · Ref: ${datos.ID_DECLARACION || 'N/D'}`, { align: 'center' });
    this.doc.moveDown(1.5);

    // Datos del contribuyente
    this._tributosRecuadroTitulo('DATOS DEL CONTRIBUYENTE');
    this._tributosCampo('Placeta ID', datos.placeta_id || '—');
    this._tributosCampo('Nombre / Razón Social', datos.nombre || '—');
    this._tributosCampo('DIP', datos.dip || '—');
    this._tributosCampo('Tipo de Sujeto', datos.tipo_sujeto || 'Físico');
    this._tributosCampo('Cuenta BLP', datos.cuenta_id_blp || '—');
    this.doc.moveDown(1);

    // Cálculos
    this._tributosRecuadroTitulo('CÁLCULO DE IMPUESTOS');
    this._tributosFila('Patrimonio Medio', `${Number(datos.patrimonio_medio || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosFila('Índice de Acumulación (IA)', `${Number(datos.indice_acumulacion || 0).toFixed(4)}%`);
    this._tributosFila('Cuota IRM', `${Number(datos.cuota_irm || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosFila('Cuota IGF', `${Number(datos.cuota_igf || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosFila('Exención Aplicada', datos.exencion_aplicada ? 'Sí' : 'No');
    this.doc.moveDown(1);

    // Estado
    this._tributosRecuadroTitulo('ESTADO Y CONTROL');
    this._tributosCampo('Estado de Pago', datos.estado_pago || 'Borrador');
    this._tributosCampo('Días Declarados (Banco)', String(datos.dias_declarados_banco || 0));
    this._tributosCampo('Días Reconstruidos (CRM)', String(datos.dias_reconstruidos_crm || 0));
    this._tributosCampo('Días Activos del Mes', String(datos.dias_activos_mes || 30));
    this._tributosCampo('Bypass Junta Directiva', datos.bypass_junta_directiva ? 'Sí' : 'No');
    if (datos.id_permiso_junta) this._tributosCampo('ID Permiso Junta', datos.id_permiso_junta);

    this.doc.moveDown(1.5);
    this._tributosLinea('#1c005f');
    this.doc.moveDown(1);

    // Firma y hash
    const hash = datos.pdf_hash || crypto.createHash('sha256').update(JSON.stringify(datos) + Date.now()).digest('hex');
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica');
    this.doc.text(`CSV: ${datos.csv || hash.substring(0, 16).toUpperCase()}`, { align: 'center' });
    this.doc.moveDown(0.3);
    this.doc.text(`SHA-256: ${hash}`, { align: 'center' });
    this.doc.moveDown(0.5);
    this._tributosLinea('#5a2fc2');
    this.doc.moveDown(0.5);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica-Oblique');
    this.doc.text('Tributos de La Placeta (TLP) · Documento generado electrónicamente con valor legal.', { align: 'center' });
    this.doc.text('Código Normativo Interno · Capítulo IV — Banca, Capital e Impuestos', { align: 'center' });
    this.doc.text(`Generado el ${new Date().toLocaleString('es-ES')}`, { align: 'center' });

    return this.doc;
  }

  /**
   * Generar PDF de Factura con IVA (TLP-FAC-001)
   * Estilo basado en Código Normativo Interno
   */
  generarFacturaTributaria(datos) {
    this.doc = new PDFDocument({
      size: 'A4',
      margins: { top: 60, bottom: 50, left: 55, right: 55 },
      info: {
        Title: `Factura TLP - ${datos.numero_factura}`,
        Author: 'Tributos de La Placeta (TLP)',
        Subject: 'Factura con IVA 12%',
        Keywords: 'TLP, factura, IVA, tributación'
      }
    });
    this._estiloCodigoNormativo();

    // ── Cabecera ──
    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica-Bold');
    this.doc.text('TRIBUTOS DE LA PLACETA', { align: 'center' });
    this.doc.moveDown(0.2);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica');
    this.doc.text('Sistema de Facturación · IVA 12% · Acoplado al Banco de La Placeta (BLP)', { align: 'center' });
    this.doc.moveDown(0.3);
    this._tributosLinea('#5a2fc2');
    this.doc.moveDown(1.2);

    this.doc.fontSize(16).fillColor('#1c005f').font('Helvetica-Bold');
    this.doc.text('FACTURA', { align: 'center' });
    this.doc.moveDown(0.3);
    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica');
    this.doc.text(`Nº ${datos.numero_factura} · CSV: ${datos.csv_verificacion || 'N/D'}`, { align: 'center' });
    this.doc.moveDown(1.2);

    this._tributosRecuadroTitulo('PARTES');
    this._tributosCampo('Emisor (Placeta ID)', datos.emisor_placeta_id || '—');
    this._tributosCampo('Receptor (Placeta ID)', datos.receptor_placeta_id || '—');
    this._tributosCampo('Fecha de Emisión', datos.fecha_emision ? new Date(datos.fecha_emision).toLocaleString('es-ES') : '—');
    if (datos.transaction_id_blp) this._tributosCampo('ID Transacción BLP', datos.transaction_id_blp);
    this.doc.moveDown(1);

    this._tributosRecuadroTitulo('IMPORTES');
    this._tributosFila('Base Imponible', `${Number(datos.base_imponible || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosFila('IVA (12%)', `${Number(datos.total_iva || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosLinea('#5a2fc2');
    this._tributosFila('TOTAL FACTURA', `${Number(datos.total_factura || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`, true);

    this.doc.moveDown(1.5);

    // Líneas si hay detalle
    if (datos.lineas && datos.lineas.length > 0) {
      this._tributosRecuadroTitulo('DETALLE DE PRODUCTOS / SERVICIOS');
      // Cabecera de tabla
      const colX = [55, 200, 280, 340, 400, 470];
      this.doc.fontSize(7.5).fillColor('#ffffff').font('Helvetica-Bold');
      this.doc.rect(55, this.doc.y, 490, 18).fill('#1c005f');
      const baseY = this.doc.y + 5;
      this.doc.text('Concepto', colX[0], baseY, { width: 130 });
      this.doc.text('Cant.', colX[1], baseY, { width: 40, align: 'center' });
      this.doc.text('P. Unit.', colX[2], baseY, { width: 55, align: 'right' });
      this.doc.text('Neto', colX[3], baseY, { width: 55, align: 'right' });
      this.doc.text('IVA', colX[4], baseY, { width: 55, align: 'right' });
      this.doc.text('Subtotal', colX[5], baseY, { width: 60, align: 'right' });
      this.doc.y += 22;

      datos.lineas.forEach((linea, index) => {
        if (index % 2 === 0) {
          this.doc.rect(55, this.doc.y - 4, 490, 20).fill('#f7f4fd');
        }
        this.doc.fontSize(7.5).fillColor('#1c1226').font('Helvetica');
        const y = this.doc.y;
        this.doc.text(linea.concepto_producto || '—', colX[0], y, { width: 130 });
        this.doc.text(String(linea.cantidad || 0), colX[1], y, { width: 40, align: 'center' });
        this.doc.text(Number(linea.precio_unitario || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 }), colX[2], y, { width: 55, align: 'right' });
        this.doc.text(Number(linea.subtotal_neto || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 }), colX[3], y, { width: 55, align: 'right' });
        this.doc.text(Number(linea.subtotal_iva || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 }), colX[4], y, { width: 55, align: 'right' });
        this.doc.text(Number((linea.subtotal_neto || 0) + (linea.subtotal_iva || 0)).toLocaleString('es-ES', { minimumFractionDigits: 2 }), colX[5], y, { width: 60, align: 'right' });
        this.doc.y += 14;
      });
    }

    this.doc.moveDown(1.5);
    this._tributosLinea('#1c005f');
    this.doc.moveDown(0.8);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica-Oblique');
    this.doc.text('Tributos de La Placeta (TLP) · Factura con IVA al 12% · Código Normativo Interno Cap. IV', { align: 'center' });
    this.doc.text(`CSV: ${datos.csv_verificacion || 'N/D'} · Generado el ${new Date().toLocaleString('es-ES')}`, { align: 'center' });

    return this.doc;
  }

  /**
   * Generar PDF de Rectificación / Reajuste Tributario (TLP-REC-001)
   */
  generarRectificacionTributaria(datos) {
    this.doc = new PDFDocument({
      size: 'A4',
      margins: { top: 60, bottom: 50, left: 55, right: 55 },
      info: {
        Title: `Rectificación Tributaria - ${datos.ID_RECTIFICACION || datos.id}`,
        Author: 'Tributos de La Placeta (TLP)',
        Subject: 'Rectificación Post-Bypass / Reajuste',
        Keywords: 'TLP, rectificación, bypass, reajuste'
      }
    });
    this._estiloCodigoNormativo();

    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica-Bold');
    this.doc.text('TRIBUTOS DE LA PLACETA', { align: 'center' });
    this.doc.moveDown(0.2);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica');
    this.doc.text('Sistema de Rectificación Impositiva · Art. 4.16 ter', { align: 'center' });
    this.doc.moveDown(0.3);
    this._tributosLinea('#5a2fc2');
    this.doc.moveDown(1.2);

    this.doc.fontSize(15).fillColor('#1c005f').font('Helvetica-Bold');
    this.doc.text('RECTIFICACIÓN Y REAJUSTE DE OFICIO', { align: 'center' });
    this.doc.moveDown(0.3);
    this.doc.fontSize(9).fillColor('#5a2fc2').font('Helvetica');
    this.doc.text(`Ref: ${datos.ID_RECTIFICACION || datos.id || 'N/D'} · ${datos.fecha_rectificacion ? new Date(datos.fecha_rectificacion).toLocaleDateString('es-ES') : new Date().toLocaleDateString('es-ES')}`, { align: 'center' });
    this.doc.moveDown(1.2);

    this._tributosRecuadroTitulo('DATOS DE LA RECTIFICACIÓN');
    this._tributosCampo('Declaración Original', datos.declaracion_original_id || '—');
    this._tributosCampo('Placeta ID', datos.placeta_id || '—');
    this._tributosCampo('Fecha Rectificación', datos.fecha_rectificacion ? new Date(datos.fecha_rectificacion).toLocaleString('es-ES') : new Date().toLocaleString('es-ES'));
    this.doc.moveDown(1);

    this._tributosRecuadroTitulo('DIFERENCIA DELTA (Δ)');
    const delta = Number(datos.diferencia_delta || 0);
    const esReembolso = delta < 0;
    this._tributosFila('Cuota Provisional Cobrada', `${Number(datos.cuota_provisional_cobrada || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosFila('Cuota Real Calculada', `${Number(datos.cuota_real_calculada || 0).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`);
    this._tributosLinea('#5a2fc2');
    this._tributosFila(
      esReembolso ? '🟢 Diferencia a Reembolsar' : '🔴 Diferencia a Cobrar',
      `${Math.abs(delta).toLocaleString('es-ES', { minimumFractionDigits: 2 })} Pz`,
      true
    );

    this.doc.moveDown(1);
    this._tributosCampo('Estado del Ajuste', datos.estado_ajuste || 'Pendiente_Procesamiento');

    if (datos.signature_sha256) {
      this.doc.moveDown(1);
      this.doc.fontSize(6.5).fillColor('#5c5566').font('Helvetica');
      this.doc.text(`Firma SHA-256: ${datos.signature_sha256}`, { align: 'center' });
    }

    this.doc.moveDown(1.5);
    this._tributosLinea('#1c005f');
    this.doc.moveDown(0.5);
    this.doc.fontSize(7).fillColor('#5c5566').font('Helvetica-Oblique');
    this.doc.text('Tributos de La Placeta (TLP) · Rectificación según Art. 4.16 ter del Código Normativo Interno', { align: 'center' });
    this.doc.text('Documento con validez ejecutiva · Compensación en caliente sobre el BLP', { align: 'center' });

    return this.doc;
  }

  // ── Estilo Código Normativo ─────────────────────────────────────────────

  _estiloCodigoNormativo() {
    // Colores de la paleta codigo.html:
    // purple-900:#1c005f, purple-700:#341087, purple-500:#5a2fc2,
    // purple-300:#9c7ee6, purple-100:#efe9fb, ink:#1c1226, gray:#5c5566
  }

  _tributosLinea(color = '#5a2fc2') {
    this.doc.moveTo(55, this.doc.y)
      .lineTo(545, this.doc.y)
      .strokeColor(color)
      .lineWidth(1.5)
      .stroke();
  }

  _tributosRecuadroTitulo(texto) {
    const doc = this.doc;
    doc.fontSize(8).fillColor('#ffffff').font('Helvetica-Bold');
    const y = doc.y;
    doc.rect(55, y, 490, 18).fill('#5a2fc2');
    doc.text(`  ${texto}`, 55, y + 4, { width: 480 });
    doc.y = y + 22;
  }

  _tributosCampo(nombre, valor) {
    const doc = this.doc;
    doc.fontSize(8).fillColor('#1c1226').font('Helvetica-Bold');
    const x = 60;
    doc.text(`${nombre}: `, x, doc.y, { continued: true });
    doc.font('Helvetica').fillColor('#5c5566').text(valor || '—');
    doc.y += 2;
  }

  _tributosFila(nombre, valor, bold = false) {
    const doc = this.doc;
    doc.fontSize(8.5).font(bold ? 'Helvetica-Bold' : 'Helvetica').fillColor('#1c1226');
    const y = doc.y;
    doc.text(nombre, 60, y, { width: 300 });
    doc.font(bold ? 'Helvetica-Bold' : 'Helvetica').fillColor(bold ? '#1c005f' : '#5c5566');
    doc.text(valor, 360, y, { width: 180, align: 'right' });
    doc.y = y + 14;
  }
}

export default PDFGenerator;
