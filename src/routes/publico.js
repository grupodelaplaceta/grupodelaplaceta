import { Router } from 'express';
import { getDb } from '../config/db.js';
import crypto from 'crypto';
import path from 'path';
import fs from 'fs';
import PDFDocument from 'pdfkit';
import { generarPDFDIP, generarPDFPlacetaID, generarPDFQueja, generarPDFControlParental, generarPDFEntidad } from '../services/tramitePDFs.js';
import {
  sbFindSolicitante, sbFindSolicitanteByDip, sbFindSolicitanteById,
  sbCreateSolicitante, sbUpdateSolicitante,
  sbFindSolicitudDip, sbCreateSolicitudDip, sbUpdateSolicitudDip,
  sbFindControlParentalByCodigo, sbFindControlParentalByDni,
  sbCreateControlParental, sbUpdateControlParentalUsar,
  sbCreateQueja, sbFindQuejaById, sbListQuejas,
  sbFindEntidadByEip, sbCreateEntidad,
  sbCreateDocumentoTramite, sbFindDocumentosByUsuario,
  sbCreateLog, sbFindLogsByUsuario,
  sbListDocumentosFirmadosPendientes
} from '../config/db-supabase.js';

const router = Router();

// ── PÁGINAS PÚBLICAS ────────────────────────────────────────────────────────

router.get('/', (req, res) => {
  res.render('public/index', { titulo: 'Inicio', layout: 'layouts/publico', pathActual: '/' });
});

router.get('/contenidos', (req, res) => {
  res.render('public/contenidos', { titulo: 'Contenidos', layout: 'layouts/publico', pathActual: '/contenidos' });
});

router.get('/galeria', (req, res) => {
  res.render('public/galeria', { titulo: 'Galería', layout: 'layouts/publico', pathActual: '/galeria' });
});

router.get('/tramites', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites', { titulo: 'Trámites', layout: 'layouts/publico', pathActual: '/tramites', usuario });
});

// ── TRÁMITES LEGALES (Asociación) ───────────────────────────────────────────

router.get('/tramites/control-parental', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/control-parental', { titulo: 'Control Parental', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, usuario });
});

router.post('/tramites/control-parental', async (req, res) => {
  try {
    const { nombre_tutor, dni_tutor, email_tutor, telefono_tutor, nombre_menor, fecha_nac_menor } = req.body;
    if (!nombre_tutor || !dni_tutor || !email_tutor || !nombre_menor || !fecha_nac_menor) {
      return res.render('public/tramites/control-parental', {
        titulo: 'Control Parental', layout: 'layouts/publico', pathActual: '/tramites',
        error: 'Todos los campos obligatorios deben completarse.', resultado: null
      });
    }

    // Validar DNI
    if (!/^[0-9]{8}[A-Z]$/.test(dni_tutor)) {
      return res.render('public/tramites/control-parental', {
        titulo: 'Control Parental', layout: 'layouts/publico', pathActual: '/tramites',
        error: 'El formato del DNI no es válido. Debe ser 8 dígitos + letra mayúscula (ej: 12345678A).', resultado: null
      });
    }

    const codigo = crypto.randomBytes(4).toString('hex').toUpperCase();
    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    await sbCreateControlParental({
      nombre_tutor, dni_tutor, email_tutor,
      telefono_tutor: telefono_tutor || '',
      nombre_menor, fecha_nac_menor,
      codigo_vinculacion: codigo, ip_registro: ip
    });

    res.render('public/tramites/control-parental', {
      titulo: 'Control Parental', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: { codigo, nombre_menor }
    });
  } catch (err) {
    res.render('public/tramites/control-parental', {
      titulo: 'Control Parental', layout: 'layouts/publico', pathActual: '/tramites',
      error: 'Error al procesar la solicitud. Inténtelo de nuevo más tarde.', resultado: null
    });
  }
});

router.get('/tramites/recuperar-codigo', (req, res) => {
  res.render('public/tramites/recuperar-codigo', { titulo: 'Recuperar Código', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, dni: '' });
});

router.post('/tramites/recuperar-codigo', async (req, res) => {
  const { dni_tutor } = req.body;
  if (!dni_tutor) {
    return res.render('public/tramites/recuperar-codigo', {
      titulo: 'Recuperar Código', layout: 'layouts/publico', pathActual: '/tramites',
      error: 'Introduce tu DNI.', resultado: null
    });
  }
  const registros = await sbFindControlParentalByDni(dni_tutor);
  res.render('public/tramites/recuperar-codigo', {
    titulo: 'Recuperar Código', layout: 'layouts/publico', pathActual: '/tramites',
    resultado: registros.length > 0 ? registros : [],
    dni: dni_tutor
  });
});

router.get('/tramites/quejas', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/quejas', { titulo: 'Quejas y Sugerencias', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, usuario });
});

router.post('/tramites/quejas', async (req, res) => {
  const { nombre, email, tipo, mensaje } = req.body;
  if (!nombre || !mensaje) {
    return res.render('public/tramites/quejas', {
      titulo: 'Quejas y Sugerencias', layout: 'layouts/publico', pathActual: '/tramites',
      error: 'Nombre y mensaje son obligatorios.', resultado: null
    });
  }
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  const result = await sbCreateQueja({ nombre, email: email || '', tipo: tipo || 'sugerencia', mensaje, ip });
  const numReg = `Q-${String(result.id).padStart(4, '0')}-${new Date().getFullYear()}`;
  res.render('public/tramites/quejas', {
    titulo: 'Quejas y Sugerencias', layout: 'layouts/publico', pathActual: '/tramites',
    resultado: { message: 'Gracias por tu mensaje. Lo revisaremos en un plazo máximo de 30 días hábiles.', numero: numReg }
  });
});

// ── TRÁMITES ECOSISTEMA ─────────────────────────────────────────────────────

router.get('/tramites/alta-dip', (req, res) => {
  const usuario = req.session.usuario || null;
  // Si ya está logueado, ya tiene DIP
  if (usuario) {
    return res.render('public/tramites/alta-dip', {
      titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: null, yaTieneDIP: true, usuario
    });
  }
  res.render('public/tramites/alta-dip', { titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, yaTieneDIP: false, usuario: null });
});

router.post('/tramites/alta-dip', async (req, res) => {
  try {
    const usuarioSesion = req.session.usuario || null;

    // Si ya está logueado, redirigir
    if (usuarioSesion) {
      return res.render('public/tramites/alta-dip', {
        titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites',
        resultado: null, yaTieneDIP: true, usuario: usuarioSesion
      });
    }

    const { alias, nombre_real, email, fecha_nacimiento, codigo_tutor } = req.body;
    if (!alias || !nombre_real || !email || !fecha_nacimiento) {
      return res.render('public/tramites/alta-dip', { titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites', error: 'Campos obligatorios faltantes.', resultado: null, yaTieneDIP: false, usuario: null });
    }
    const nac = new Date(fecha_nacimiento);
    const hoy = new Date(2026, 5, 28);
    let edad = hoy.getFullYear() - nac.getFullYear();
    const md = hoy.getMonth() - nac.getMonth();
    if (md < 0 || (md === 0 && hoy.getDate() < nac.getDate())) edad--;

    if (edad < 16) {
      if (!codigo_tutor) {
        return res.render('public/tramites/alta-dip', { titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites', error: 'Menores de 16 necesitan código de vinculación de un tutor.', resultado: null, yaTieneDIP: false, usuario: null });
      }
      const cp = await sbFindControlParentalByCodigo(codigo_tutor);
      if (!cp || cp.usado) {
        return res.render('public/tramites/alta-dip', { titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites', error: 'Código de vinculación inválido o ya usado.', resultado: null, yaTieneDIP: false, usuario: null });
      }
      await sbUpdateControlParentalUsar(codigo_tutor);
    }

    const dip = generarDIP();
    await sbCreateSolicitudDip({ alias, nombre_real, email, fecha_nacimiento, edad, dip, codigo_tutor: codigo_tutor || null });

    res.render('public/tramites/alta-dip', {
      titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: { dip, message: 'Solicitud registrada. Recibirás el DIP provisional tras aprobación.' },
      yaTieneDIP: false, usuario: null
    });
  } catch (err) {
    res.render('public/tramites/alta-dip', { titulo: 'Alta DIP', layout: 'layouts/publico', pathActual: '/tramites', error: 'Error al procesar.', resultado: null, yaTieneDIP: false, usuario: null });
  }
});

router.get('/tramites/alta-placetid', (req, res) => {
  const usuario = req.session.usuario || null;
  if (usuario) {
    return res.render('public/tramites/alta-placetid', {
      titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: null, usuario, dipSugerido: usuario.dip
    });
  }
  res.render('public/tramites/alta-placetid', { titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, usuario: null, dipSugerido: '' });
});

router.post('/tramites/alta-placetid', async (req, res) => {
  try {
    const { dip, email, password } = req.body;
    if (!dip || !email || !password) {
      return res.render('public/tramites/alta-placetid', { titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites', error: 'Todos los campos son obligatorios.', resultado: null });
    }
    const solicitud = await sbFindSolicitudDip(dip);
    if (!solicitud || solicitud.estado !== 'pendiente') {
      return res.render('public/tramites/alta-placetid', { titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites', error: 'DIP no encontrado o ya procesado.', resultado: null });
    }

    const bcrypt = await import('bcryptjs');
    const { v4: uuidv4 } = await import('uuid');
    const ph = await bcrypt.hash(password, 10);
    const placeid = `PLID-${dip}`;
    const franja = solicitud.edad >= 18 ? 'Alta_Plena' : solicitud.edad >= 16 ? 'Tutelada_Senior' : 'Tutelada_Basica';

    await sbUpdateSolicitudDip(dip, { estado: 'completado' });
    const nuevoSolicitante = await sbCreateSolicitante({
      alias: solicitud.alias, nombre_real: solicitud.nombre_real, email,
      fecha_nacimiento: solicitud.fecha_nacimiento, edad: solicitud.edad,
      dip, placeid, franja_edad: franja, hash_credencial: uuidv4(),
      estado: 'activo', password_hash: ph, rol: 'miembro'
    });

    // Cuenta bancaria remains in SQLite (internal banking table)
    const db = getDb();
    const tipoCuenta = franja === 'Tutelada_Basica' ? 'Junior_Basica' : franja === 'Tutelada_Senior' ? 'Junior_Senior' : 'Ciudadana_Plena';
    db.prepare('INSERT INTO cuentas_bancarias (usuario_id, tipo_cuenta, saldo, saldo_maximo, bono_bienvenida_activo, fecha_bono) VALUES (?, ?, 500, ?, 1, datetime(\'now\'))')
      .run(nuevoSolicitante.id, tipoCuenta, franja === 'Alta_Plena' ? 500000 : franja === 'Tutelada_Senior' ? 250000 : 100000);

    res.render('public/tramites/alta-placetid', {
      titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: { placeid, message: `PlacetaID activado. Ya puedes acceder con tu alias: ${solicitud.alias}` }
    });
  } catch (err) {
    res.render('public/tramites/alta-placetid', { titulo: 'Alta PlacetaID', layout: 'layouts/publico', pathActual: '/tramites', error: 'Error al procesar.', resultado: null });
  }
});

router.get('/tramites/alta-entidad', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/alta-entidad', { titulo: 'Alta Entidad', layout: 'layouts/publico', pathActual: '/tramites', resultado: null, usuario });
});

router.post('/tramites/alta-entidad', async (req, res) => {
  try {
    const { nombre_entidad, tipo, representante_dip, email, cif, descripcion } = req.body;
    if (!nombre_entidad || !tipo || !representante_dip || !email) {
      return res.render('public/tramites/alta-entidad', { titulo: 'Alta Entidad', layout: 'layouts/publico', pathActual: '/tramites', error: 'Campos obligatorios faltantes.', resultado: null });
    }
    const rep = await sbFindSolicitanteByDip(representante_dip);
    if (!rep || rep.estado !== 'activo') {
      return res.render('public/tramites/alta-entidad', { titulo: 'Alta Entidad', layout: 'layouts/publico', pathActual: '/tramites', error: 'Representante no encontrado o inactivo.', resultado: null });
    }

    const eip = `EIP-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;

    // 1) Guardar en Supabase
    await sbCreateEntidad({ nombre: nombre_entidad, tipo, eip, representante_id: rep.id, cif: cif || '', descripcion: descripcion || '', email });

    // 2) Guardar en SQLite para PDF
    const db = getDb();
    const sqlResult = db.prepare(
      `INSERT INTO entidades (nombre, tipo, eip, representante_dip, email, cif, descripcion, creado_en)
       VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))`
    ).run(nombre_entidad, tipo, eip, representante_dip, email, cif || '', descripcion || '');
    const entidadId = sqlResult.lastInsertRowid;

    // 3) Registrar en tributos (backend-banco) para que el banco pueda validar el EIP
    const API_BANCO = process.env.BANCO_API_URL || 'http://localhost:3003';
    const placetaId = rep.placeid || `PLID-${rep.dip}`;
    try {
      await fetch(`${API_BANCO}/api/v1/tributos/alta`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          placeta_id: placetaId,
          dip: rep.dip,
          nombre: nombre_entidad,
          tipo_sujeto: 'Empresa',
          eip,
          iban: `GDLP-EIP-${eip.slice(-4)}`
        })
      });
    } catch (apiErr) {
      console.warn('[Tributos] No se pudo registrar en backend-banco:', apiErr.message);
    }

    // 4) Generar PDF certificado
    let pdfBuffer = null;
    try {
      const { generarPDFEntidad } = await import('../services/tramitePDFs.js');
      const datosEntidad = { id: entidadId, nombre: nombre_entidad, tipo, eip, representante_dip, email, cif: cif || '', descripcion: descripcion || '' };
      pdfBuffer = await generarPDFEntidad(datosEntidad);
    } catch (pdfErr) {
      console.warn('[Entidad] Error generando PDF:', pdfErr.message);
    }

    res.render('public/tramites/alta-entidad', {
      titulo: 'Alta Entidad', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: {
        eip,
        entidadId,
        message: `Solicitud de ${tipo} "${nombre_entidad}" registrada con EIP: ${eip}`
      }
    });
  } catch (err) {
    console.error('[Entidad] Error alta:', err);
    res.render('public/tramites/alta-entidad', { titulo: 'Alta Entidad', layout: 'layouts/publico', pathActual: '/tramites', error: 'Error al procesar.', resultado: null });
  }
});

// ── PROYECTOS ──────────────────────────────────────────────────────────────

router.get('/proyectos', (req, res) => {
  res.render('public/proyectos', { titulo: 'Proyectos', layout: 'layouts/publico', pathActual: '/proyectos', usuario: req.session.usuario || null });
});

router.get('/voley-club', (req, res) => { res.redirect('/proyectos'); });

// ── DESCARGA DE PDFs DE TRÁMITES ────────────────────────────────────────────

router.get('/tramites/pdf/:tipo', async (req, res) => {
  try {
    const { tipo } = req.params;
    let pdfBuffer, filename;

    switch (tipo) {
      case 'dip': {
        const id = req.query.id;
        const db = getDb();
        const datos = db.prepare('SELECT * FROM solicitudes_dip WHERE id = ?').get(id);
        if (!datos) return res.status(404).send('Solicitud no encontrada');
        pdfBuffer = await generarPDFDIP(datos);
        filename = `solicitud-dip-${datos.dip}.pdf`;
        break;
      }
      case 'placetid': {
        const id = req.query.id;
        const user = await sbFindSolicitanteById(Number(id));
        if (!user) return res.status(404).send('Usuario no encontrado');
        pdfBuffer = await generarPDFPlacetaID(user);
        filename = `activacion-placetid-${user.dip}.pdf`;
        break;
      }
      case 'queja': {
        const id = req.query.id;
        const datos = await sbFindQuejaById(Number(id));
        if (!datos) return res.status(404).send('Queja no encontrada');
        const quejaData = { ...datos, numero: `Q-${String(datos.id).padStart(4, '0')}-${new Date(datos.creado_en).getFullYear()}` };
        pdfBuffer = await generarPDFQueja(quejaData);
        filename = `queja-${quejaData.numero}.pdf`;
        break;
      }
      case 'control-parental': {
        const id = req.query.id;
        const db = getDb();
        const datos = db.prepare('SELECT * FROM control_parental WHERE id = ?').get(id);
        if (!datos) return res.status(404).send('Registro no encontrado');
        pdfBuffer = await generarPDFControlParental(datos);
        filename = `control-parental-${datos.codigo_vinculacion}.pdf`;
        break;
      }
      case 'entidad': {
        const id = req.query.id;
        const db = getDb();
        const datos = db.prepare('SELECT * FROM entidades WHERE id = ?').get(id);
        if (!datos) return res.status(404).send('Entidad no encontrada');
        pdfBuffer = await generarPDFEntidad(datos);
        filename = `alta-entidad-${datos.eip}.pdf`;
        break;
      }
      default:
        return res.status(400).send('Tipo de PDF no válido');
    }

    if (!pdfBuffer) return res.status(500).send('Error al generar PDF');
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    res.send(pdfBuffer);
  } catch (err) {
    console.error('Error generando PDF:', err);
    res.status(500).send('Error interno al generar el PDF');
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  TRÁMITES DE TRIBUTOS
// ═══════════════════════════════════════════════════════════════════════════

router.get('/tramites/alta-tributos', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/alta-tributos', {
    titulo: 'Alta en Tributos', layout: 'layouts/publico', pathActual: '/tramites',
    resultado: null, usuario, yaRegistrado: null
  });
});

router.post('/tramites/alta-tributos', async (req, res) => {
  const usuario = req.session.usuario || null;
  try {
    const { placeta_id, nombre, dip, tipo_sujeto, iban } = req.body;
    if (!placeta_id || !nombre || !dip) {
      return res.render('public/tramites/alta-tributos', {
        titulo: 'Alta en Tributos', layout: 'layouts/publico', pathActual: '/tramites',
        error: 'Faltan campos obligatorios: Placeta ID, nombre y DIP.', resultado: null, usuario, yaRegistrado: null
      });
    }

    // Verificar si ya existe
    const { sbGetTributosContributorByPlacetaId, sbCreateTributosContributor } = await import('../config/db-supabase.js');
    const existente = await sbGetTributosContributorByPlacetaId(placeta_id).catch(() => null);
    if (existente && existente.fecha_alta_tributos) {
      return res.render('public/tramites/alta-tributos', {
        titulo: 'Alta en Tributos', layout: 'layouts/publico', pathActual: '/tramites',
        resultado: null, usuario,
        yaRegistrado: new Date(existente.fecha_alta_tributos).toLocaleDateString('es-ES')
      });
    }

    const contributor = await sbCreateTributosContributor({
      id: crypto.randomUUID(),
      placeta_id,
      dip,
      nombre,
      tipo_sujeto: tipo_sujeto || 'Fisico',
      estado_fiscal: 'Al Dia',
      fecha_alta_tributos: new Date().toISOString(),
      roles_json: ['ciudadano'],
      iban: iban || null
    });

    return res.render('public/tramites/alta-tributos', {
      titulo: 'Alta en Tributos', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: contributor, usuario, yaRegistrado: null
    });
  } catch (err) {
    console.error('[Tributos] Error alta tramite:', err);
    return res.render('public/tramites/alta-tributos', {
      titulo: 'Alta en Tributos', layout: 'layouts/publico', pathActual: '/tramites',
      error: 'Error al procesar la solicitud. Inténtalo de nuevo.', resultado: null, usuario, yaRegistrado: null
    });
  }
});

// ── API Pública de Tributos (consulta para usuarios autenticados) ──────────
router.get('/api/tributos/mi-perfil', async (req, res) => {
  const usuario = req.session.usuario;
  if (!usuario) return res.status(401).json({ error: 'No autenticado' });
  try {
    const { sbGetTributosContributorByPlacetaId } = await import('../config/db-supabase.js');
    const placetaId = usuario.placeid || `PLID-${usuario.dip}`;
    const perfil = await sbGetTributosContributorByPlacetaId(placetaId).catch(() => null);
    res.json(perfil || null);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/api/tributos/mis-declaraciones', async (req, res) => {
  const usuario = req.session.usuario;
  if (!usuario) return res.status(401).json({ error: 'No autenticado' });
  try {
    const { sbListTributosDeclarations } = await import('../config/db-supabase.js');
    const todas = await sbListTributosDeclarations(100);
    const dip = usuario.dip;
    const alias = usuario.alias;
    const misDecls = todas.filter(d => d.dip === dip || d.placeta_id === `PLID-${dip}` || d.placeta_id === `PID-${alias?.toUpperCase()}`);
    res.json(misDecls);
  } catch (err) {
    res.json([]);
  }
});

router.get('/api/tributos/mis-facturas', async (req, res) => {
  const usuario = req.session.usuario;
  if (!usuario) return res.status(401).json({ error: 'No autenticado' });
  try {
    const { sbListTributosInvoices } = await import('../config/db-supabase.js');
    const todas = await sbListTributosInvoices(parseInt(req.query.limit) || 20);
    const placetaId = usuario.placeid || `PLID-${usuario.dip}`;
    const misFacts = todas.filter(f => f.emisor_placeta_id === placetaId || f.receptor_placeta_id === placetaId);
    res.json(misFacts);
  } catch (err) {
    res.json([]);
  }
});

router.get('/tramites/solicitar-factura', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/solicitar-factura', {
    titulo: 'Solicitar Factura', layout: 'layouts/publico', pathActual: '/tramites',
    resultado: null, usuario, error: null
  });
});

router.get('/tramites/consulta-tributos', (req, res) => {
  const usuario = req.session.usuario || null;
  res.render('public/tramites/consulta-tributos', {
    titulo: 'Consulta Tributaria', layout: 'layouts/publico', pathActual: '/tramites', usuario
  });
});

router.post('/tramites/solicitar-factura', async (req, res) => {
  const usuario = req.session.usuario || null;
  try {
    const { numero_factura, emisor_placeta_id, receptor_placeta_id, concepto_producto, cantidad, precio_unitario, iva_porcentaje, fecha_emision } = req.body;
    if (!numero_factura || !emisor_placeta_id || !receptor_placeta_id || !concepto_producto) {
      return res.render('public/tramites/solicitar-factura', {
        titulo: 'Solicitar Factura', layout: 'layouts/publico', pathActual: '/tramites',
        error: 'Faltan campos obligatorios.', resultado: null, usuario
      });
    }

    const { sbCreateTributosInvoice } = await import('../config/db-supabase.js');
    const invoice = await sbCreateTributosInvoice({
      id: crypto.randomUUID(),
      numero_factura,
      emisor_placeta_id,
      receptor_placeta_id,
      fecha_emision: fecha_emision ? new Date(fecha_emision).toISOString() : new Date().toISOString(),
      csv_verificacion: `CSV-${crypto.randomUUID().slice(0, 8).toUpperCase()}-${Date.now().toString(36).toUpperCase()}`,
      lineas: [{
        concepto_producto,
        cantidad: Number(cantidad) || 1,
        precio_unitario: Number(precio_unitario) || 0,
        iva_porcentaje: Number(iva_porcentaje) || 12
      }]
    });

    return res.render('public/tramites/solicitar-factura', {
      titulo: 'Solicitar Factura', layout: 'layouts/publico', pathActual: '/tramites',
      resultado: invoice, usuario, error: null
    });
  } catch (err) {
    console.error('[Tributos] Error factura tramite:', err);
    return res.render('public/tramites/solicitar-factura', {
      titulo: 'Solicitar Factura', layout: 'layouts/publico', pathActual: '/tramites',
      error: 'Error al emitir la factura. Inténtalo de nuevo.', resultado: null, usuario
    });
  }
});

// ── PÁGINAS LEGALES ─────────────────────────────────────────────────────────

router.get('/aviso-legal', (req, res) => {
  res.render('public/aviso-legal', { titulo: 'Aviso Legal', layout: 'layouts/publico', pathActual: '/aviso-legal', usuario: null });
});

router.get('/privacidad', (req, res) => {
  res.render('public/privacidad', { titulo: 'Política de Privacidad', layout: 'layouts/publico', pathActual: '/privacidad', usuario: null });
});

router.get('/cookies', (req, res) => {
  res.render('public/cookies', { titulo: 'Política de Cookies', layout: 'layouts/publico', pathActual: '/cookies', usuario: null });
});

// ── API INTERACTIVA (tramites en modal) ─────────────────────────────────────

router.get('/api/tramites/form/:tramite', (req, res) => {
  const tramites = {
    'control-parental': `
      <div class="aviso aviso-real" style="margin-top:4px"><span class="aviso-icon">⚖️</span><div><strong>Trámite legal real.</strong> Datos protegidos conforme RGPD.</div></div>
      <form onsubmit="return enviarForm('control-parental',this)">
        <h3 style="font-size:16px;font-weight:700;margin:12px 0 8px">👤 Datos del Tutor</h3>
        <div class="form-row"><div class="form-group"><label>Nombre Completo *</label><input type="text" name="nombre_tutor" required></div><div class="form-group"><label>DNI/NIE *</label><input type="text" name="dni_tutor" required pattern="^[0-9]{8}[A-Z]$" title="8 dígitos+letra"></div></div>
        <div class="form-group"><label>Email *</label><input type="email" name="email_tutor" required></div>
        <div class="s-div">Datos del menor</div>
        <div class="form-row"><div class="form-group"><label>Nombre del Menor *</label><input type="text" name="nombre_menor" required></div><div class="form-group"><label>Fecha Nacimiento *</label><input type="date" name="fecha_nac_menor" required max="2026-06-28"></div></div>
        <div class="s-div">Aceptación legal</div>
        <div class="cbox"><input type="checkbox" id="cp1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="cp1"><strong>Declaración responsable:</strong> Soy el tutor legal del menor.</label></div>
        <div class="cbox"><input type="checkbox" id="cp2" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="cp2">Acepto la <a href="/privacidad" target="_blank">Política de Privacidad</a> y el tratamiento de datos según RGPD.</label></div>
        <button type="submit" class="btn btn-p btn-bl">Generar Código de Vinculación</button>
      </form>`,
    'recuperar-codigo': `
      <form onsubmit="return enviarForm('recuperar-codigo',this)">
        <div class="form-group"><label>DNI del Tutor *</label><input type="text" name="dni_tutor" required pattern="^[0-9]{8}[A-Z]$" placeholder="12345678A"></div>
        <button type="submit" class="btn btn-p btn-bl">Buscar Códigos</button>
        <div id="resultRec" style="margin-top:16px"></div>
      </form>`,
    'quejas': `
      <div class="aviso aviso-real" style="margin-top:4px"><span class="aviso-icon">⚖️</span><div><strong>Canal oficial.</strong> Respondemos en máx. 30 días hábiles.</div></div>
      <form onsubmit="return enviarForm('quejas',this)">
        <div class="form-row"><div class="form-group"><label>Nombre *</label><input type="text" name="nombre" required></div><div class="form-group"><label>Email</label><input type="email" name="email"></div></div>
        <div class="form-group"><label>Tipo</label><select name="tipo"><option value="sugerencia">💡 Sugerencia</option><option value="queja">⚖️ Queja</option><option value="incidencia">🔧 Incidencia</option></select></div>
        <div class="form-group"><label>Mensaje *</label><textarea name="mensaje" rows="4" required style="min-height:100px"></textarea></div>
        <div class="cbox"><input type="checkbox" id="q1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="q1">Acepto la <a href="/privacidad" target="_blank">Política de Privacidad</a>.</label></div>
        <button type="submit" class="btn btn-p btn-bl">Enviar</button>
      </form>`,
    'alta-dip': `
      <div class="aviso aviso-sim" style="margin-top:4px"><span class="aviso-icon">🎭</span><div><strong>Ecosistema simulado.</strong> El DIP no tiene validez jurídica real.</div></div>
      <form onsubmit="return enviarForm('alta-dip',this)">
        <div class="form-group"><label>Alias *</label><input type="text" name="alias" required maxlength="30" placeholder="Nombre público"></div>
        <div class="form-group"><label>Nombre Real *</label><input type="text" name="nombre_real" required></div>
        <div class="form-row"><div class="form-group"><label>Email *</label><input type="email" name="email" required></div><div class="form-group"><label>Fecha Nac. *</label><input type="date" name="fecha_nacimiento" required max="2026-06-28"></div></div>
        <div class="form-group"><label>Código Tutor (si <16 años)</label><input type="text" name="codigo_tutor" placeholder="Código de vinculación"></div>
        <div class="s-div">Aceptación</div>
        <div class="cbox"><input type="checkbox" id="d1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="d1">Acepto la <a href="/privacidad" target="_blank">Política de Privacidad</a> y la normativa del ecosistema.</label></div>
        <button type="submit" class="btn btn-p btn-bl">Solicitar DIP</button>
      </form>`,
    'alta-placetid': `
      <form onsubmit="return enviarForm('alta-placetid',this)">
        <div class="form-group"><label>DIP Provisional *</label><input type="text" name="dip" required placeholder="Ej: 12345678A"></div>
        <div class="form-group"><label>Email *</label><input type="email" name="email" required></div>
        <div class="form-group"><label>Contraseña *</label><input type="password" name="password" required minlength="8"></div>
        <button type="submit" class="btn btn-p btn-bl">Activar PlacetaID</button>
      </form>`,
    'alta-entidad': `
      <div class="aviso aviso-sim" style="margin-top:4px"><span class="aviso-icon">🎭</span><div><strong>Ecosistema simulado.</strong> La entidad opera solo en La Placeta.</div></div>
      <form onsubmit="return enviarForm('alta-entidad',this)">
        <div class="form-group"><label>Nombre *</label><input type="text" name="nombre_entidad" required></div>
        <div class="form-row"><div class="form-group"><label>Tipo *</label><select name="tipo"><option value="empresa">Empresa</option><option value="asociacion">Asociación</option></select></div><div class="form-group"><label>CIF</label><input type="text" name="cif"></div></div>
        <div class="form-group"><label>DIP del Representante *</label><input type="text" name="representante_dip" required placeholder="DIP activo"></div>
        <div class="form-group"><label>Email *</label><input type="email" name="email" required></div>
        <div class="form-group"><label>Descripción</label><textarea name="descripcion" rows="2"></textarea></div>
        <div class="cbox"><input type="checkbox" id="e1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="e1">Acepto los términos del registro de entidades simuladas.</label></div>
        <button type="submit" class="btn btn-p btn-bl">Registrar Entidad</button>
      </form>`,
    'alta-tributos': `
      <div class="aviso aviso-sim"><span class="aviso-icon">🏛️</span><div><strong>Registro fiscal.</strong> Necesario para operar con tu cuenta bancaria.</div></div>
      <form onsubmit="return enviarForm('alta-tributos',this)">
        <div class="form-group"><label>Placeta ID *</label><input type="text" name="placeta_id" required placeholder="PID-MKL"></div>
        <div class="form-group"><label>Nombre *</label><input type="text" name="nombre" required></div>
        <div class="form-row"><div class="form-group"><label>DIP *</label><input type="text" name="dip" required></div><div class="form-group"><label>Tipo</label><select name="tipo_sujeto"><option value="Fisico">Físico</option><option value="Empresa">Empresa</option></select></div></div>
        <div class="form-group"><label>IBAN (opcional)</label><input type="text" name="iban" placeholder="GDLP-..."></div>
        <div class="s-div">Aceptación</div>
        <div class="cbox"><input type="checkbox" id="at1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="at1">Acepto el régimen fiscal y la <a href="/privacidad" target="_blank">Política de Privacidad</a>.</label></div>
        <button type="submit" class="btn btn-p btn-bl">📋 Darme de alta en Tributos</button>
      </form>`,
    'solicitar-factura': `
      <form onsubmit="return enviarForm('solicitar-factura',this)">
        <div class="form-row"><div class="form-group"><label>Nº Factura *</label><input type="text" name="numero_factura" required placeholder="F-2026-0001"></div><div class="form-group"><label>Fecha</label><input type="date" name="fecha_emision"></div></div>
        <div class="form-row"><div class="form-group"><label>Emisor *</label><input type="text" name="emisor_placeta_id" required placeholder="PID-MKL"></div><div class="form-group"><label>Receptor *</label><input type="text" name="receptor_placeta_id" required placeholder="PID-UNI"></div></div>
        <div class="form-group"><label>Concepto *</label><input type="text" name="concepto_producto" required></div>
        <div class="form-row"><div class="form-group"><label>Cantidad</label><input type="number" name="cantidad" value="1"></div><div class="form-group"><label>Precio (Pz)</label><input type="number" step="0.01" name="precio_unitario" required value="100"></div></div>
        <div class="form-group"><label>IVA %</label><input type="number" step="0.01" name="iva_porcentaje" value="12"></div>
        <div class="cbox"><input type="checkbox" id="sf1" required onchange="this.closest('.cbox').classList.toggle('checked',this.checked)"><label for="sf1">Confirmo los datos y acepto emitir esta factura.</label></div>
        <button type="submit" class="btn btn-p btn-bl">📄 Emitir factura</button>
      </form>`
  };
  const html = tramites[req.params.tramite];
  if (!html) return res.status(404).send('Trámite no encontrado');
  res.send(html);
});

router.post('/api/tramites/submit/:tramite', async (req, res) => {
  try {
    const data = req.body;
    let ok = true, msg = '', html = '';

    switch (req.params.tramite) {
      case 'control-parental': {
        if (!data.nombre_tutor || !data.dni_tutor || !data.email_tutor || !data.nombre_menor || !data.fecha_nac_menor)
          return res.json({ ok: false, error: 'Faltan campos obligatorios' });
        const codigo = crypto.randomBytes(4).toString('hex').toUpperCase();
        const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
        await sbCreateControlParental({
          nombre_tutor: data.nombre_tutor, dni_tutor: data.dni_tutor,
          email_tutor: data.email_tutor, nombre_menor: data.nombre_menor,
          fecha_nac_menor: data.fecha_nac_menor,
          codigo_vinculacion: codigo, ip_registro: ip
        });
        ok = true; msg = 'Código generado correctamente';
        html = `<div style="background:var(--p);color:var(--w);border-radius:12px;padding:20px;text-align:center;margin-top:12px"><div style="font-size:12px;opacity:.8">CÓDIGO DE VINCULACIÓN</div><div style="font-size:28px;font-weight:900;letter-spacing:4px;font-family:monospace;margin:8px 0">${codigo}</div><div style="font-size:12px;opacity:.8">Guárdalo. Es necesario para el alta del menor.</div></div>`;
        break;
      }
      case 'recuperar-codigo': {
        const registros = await sbFindControlParentalByDni(data.dni_tutor);
        if (registros.length === 0) return res.json({ ok: false, error: 'No se encontraron códigos para ese DNI.' });
        html = registros.map(r => `<div style="background:var(--bg);border-radius:10px;padding:14px;margin-bottom:6px"><strong>Menor:</strong> ${r.nombre_menor}<br><strong>Código:</strong> <span style="font-size:20px;font-weight:800;color:var(--p);letter-spacing:2px">${r.codigo_vinculacion}</span><br><span style="font-size:11px;color:var(--tm)">${new Date(r.creado_en+'Z').toLocaleDateString('es-ES')}</span></div>`).join('');
        ok = true; msg = 'Códigos encontrados';
        break;
      }
      case 'quejas': {
        if (!data.nombre || !data.mensaje) return res.json({ ok: false, error: 'Nombre y mensaje requeridos' });
        const ip2 = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
        const quejaResult = await sbCreateQueja({ nombre: data.nombre, email: data.email || '', tipo: data.tipo || 'sugerencia', mensaje: data.mensaje, ip: ip2 });
        ok = true; msg = 'Mensaje recibido. Te responderemos en máximo 30 días hábiles.';
        const ref = `Q-${String(quejaResult.id).padStart(4,'0')}-${new Date().getFullYear()}`;
        await sbCreateDocumentoTramite({ usuario_id: req.session.usuario?.id || 0, tipo: 'queja', referencia: ref });
        break;
      }
      case 'alta-dip': {
        if (!data.alias || !data.nombre_real || !data.email || !data.fecha_nacimiento) return res.json({ ok: false, error: 'Faltan campos' });
        const nac = new Date(data.fecha_nacimiento);
        const hoy = new Date(2026,5,28);
        let edad = hoy.getFullYear() - nac.getFullYear();
        const md = hoy.getMonth() - nac.getMonth();
        if (md < 0 || (md === 0 && hoy.getDate() < nac.getDate())) edad--;
        if (edad < 16 && data.codigo_tutor) {
          const cp = await sbFindControlParentalByCodigo(data.codigo_tutor);
          if (!cp || cp.usado) return res.json({ ok: false, error: 'Código de tutor inválido o ya usado.' });
          await sbUpdateControlParentalUsar(data.codigo_tutor);
        }
        const dip = generarDIP();
        await sbCreateSolicitudDip({ alias: data.alias, nombre_real: data.nombre_real, email: data.email, fecha_nacimiento: data.fecha_nacimiento, edad, dip, codigo_tutor: data.codigo_tutor || null });
        ok = true; msg = `DIP provisional: ${dip}. Pendiente de aprobación.`;
        html = `<div style="background:var(--p);color:var(--w);border-radius:12px;padding:16px;text-align:center;margin-top:12px"><div style="font-size:12px;opacity:.8">DIP PROVISIONAL</div><div style="font-size:24px;font-weight:900;letter-spacing:3px;font-family:monospace;margin:6px 0">${dip}</div></div><p style="font-size:13px;color:var(--tm);margin-top:12px">Guarda este DIP. Cuando sea aprobado podrás activar tu PlacetaID.</p>`;
        // Guardar documento
        await sbCreateDocumentoTramite({ usuario_id: req.session.usuario?.id || 0, tipo: 'dip', referencia: dip });
        break;
      }
      case 'alta-placetid': {
        if (!data.dip || !data.email || !data.password) return res.json({ ok: false, error: 'Faltan campos' });
        const sol = await sbFindSolicitudDip(data.dip);
        if (!sol || sol.estado !== 'pendiente') return res.json({ ok: false, error: 'DIP no encontrado o ya procesado.' });
        const bcrypt = await import('bcryptjs');
        const { v4: uuidv4 } = await import('uuid');
        const ph = await bcrypt.hash(data.password, 10);
        const placeid = `PLID-${data.dip}`;
        const franja = sol.edad >= 18 ? 'Alta_Plena' : sol.edad >= 16 ? 'Tutelada_Senior' : 'Tutelada_Basica';
        await sbUpdateSolicitudDip(data.dip, { estado: 'completado' });
        const nuevoUser = await sbCreateSolicitante({
          alias: sol.alias, nombre_real: sol.nombre_real, email: data.email,
          fecha_nacimiento: sol.fecha_nacimiento, edad: sol.edad,
          dip: data.dip, placeid, franja_edad: franja, hash_credencial: uuidv4(),
          estado: 'activo', password_hash: ph, rol: 'miembro'
        });
        ok = true; msg = `PlacetaID activado: ${placeid}. Bienvenido ${sol.alias}!`;
        await sbCreateDocumentoTramite({ usuario_id: nuevoUser.id, tipo: 'placetid', referencia: placeid });
        break;
      }
      case 'alta-entidad': {
        if (!data.nombre_entidad || !data.tipo || !data.representante_dip || !data.email) return res.json({ ok: false, error: 'Faltan campos' });
        const rep = await sbFindSolicitanteByDip(data.representante_dip);
        if (!rep || rep.estado !== 'activo') return res.json({ ok: false, error: 'Representante no encontrado.' });
        const eip = `EIP-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;
        const entidadData = { nombre: data.nombre_entidad, tipo: data.tipo, eip, representante_id: rep.id, cif: data.cif || '', descripcion: data.descripcion || '', email: data.email };
        await sbCreateEntidad(entidadData);
        // Guardar en SQLite para PDF y trazabilidad
        const db2 = getDb();
        const ins = db2.prepare(`INSERT INTO entidades (nombre, tipo, eip, representante_dip, email, cif, descripcion, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))`);
        const r2 = ins.run(data.nombre_entidad, data.tipo, eip, data.representante_dip, data.email, data.cif || '', data.descripcion || '');
        // Guardar como documento de trámite para que quede rastro
        const usuarioId = req.session.usuario?.id || rep.id || 0;
        try { await sbCreateDocumentoTramite({ usuario_id: usuarioId, tipo: 'entidad', referencia: eip }); } catch {}
        // Generar PDF
        let pdfUrl = '';
        try {
          const { generarPDFEntidad } = await import('../services/tramitePDFs.js');
          const pdfBuf = await generarPDFEntidad({ id: r2.lastInsertRowid, ...entidadData, representante_dip: data.representante_dip });
          if (pdfBuf && pdfBuf.length > 0) {
            const pdfDir = path.join(process.cwd(), 'public', 'pdfs');
            if (!fs.existsSync(pdfDir)) fs.mkdirSync(pdfDir, { recursive: true });
            const pdfName = `entidad-${eip}.pdf`;
            fs.writeFileSync(path.join(pdfDir, pdfName), pdfBuf);
            pdfUrl = `/pdfs/${pdfName}`;
          }
        } catch (pdfErr) { console.warn('[Entidad] PDF error:', pdfErr.message); }
        msg = `Entidad "${data.nombre_entidad}" registrada. EIP: ${eip}`;
        html = `<div style="background:var(--p);color:var(--w);border-radius:12px;padding:20px;text-align:center;margin-top:12px">
          <div style="font-size:12px;opacity:.8">EIP ASIGNADO</div>
          <div style="font-size:24px;font-weight:900;letter-spacing:3px;font-family:monospace;margin:8px 0">${eip}</div>
          <div style="font-size:13px;opacity:.9">${data.nombre_entidad} · ${data.tipo}</div>
        </div>
        <p style="font-size:13px;color:var(--tm);margin-top:12px">Registrado correctamente. Guarda tu EIP para vincularlo a tu cuenta bancaria.</p>
        ${pdfUrl ? `<div style="text-align:center;margin-top:10px"><a href="${pdfUrl}" target="_blank" class="btn btn-sm" style="background:var(--p);color:#fff;padding:8px 18px;border-radius:8px;text-decoration:none">📄 Descargar certificado PDF</a></div>` : ''}`;
        ok = true;
        break;
      }
      case 'alta-tributos': {
        if (!data.placeta_id || !data.nombre || !data.dip) return res.json({ ok: false, error: 'Faltan campos obligatorios.' });
        const { sbGetTributosContributorByPlacetaId, sbCreateTributosContributor } = await import('../config/db-supabase.js');
        const existente = await sbGetTributosContributorByPlacetaId(data.placeta_id).catch(() => null);
        if (existente && existente.fecha_alta_tributos) {
          return res.json({ ok: false, error: `Ya estás registrado como contribuyente (${new Date(existente.fecha_alta_tributos).toLocaleDateString('es-ES')}).` });
        }
        const contributor = await sbCreateTributosContributor({
          id: crypto.randomUUID(),
          placeta_id: data.placeta_id, dip: data.dip, nombre: data.nombre,
          tipo_sujeto: data.tipo_sujeto || 'Fisico', estado_fiscal: 'Al Dia',
          fecha_alta_tributos: new Date().toISOString(), roles_json: ['ciudadano'],
          iban: data.iban || null
        });
        ok = true; msg = `✅ Registrado en Tributos. Placeta ID: ${data.placeta_id}`;
        html = `<div style="background:var(--p);color:var(--w);border-radius:12px;padding:16px;text-align:center;margin-top:12px"><div style="font-size:12px;opacity:.8">PLACETA ID TRIBUTARIO</div><div style="font-size:22px;font-weight:900;letter-spacing:2px;font-family:monospace;margin:6px 0">${data.placeta_id}</div><div style="margin-top:6px"><span class="badge badge-activo">Al Día</span></div></div>`;
        break;
      }
      case 'solicitar-factura': {
        if (!data.numero_factura || !data.emisor_placeta_id || !data.receptor_placeta_id || !data.concepto_producto) return res.json({ ok: false, error: 'Faltan campos obligatorios.' });
        const { sbCreateTributosInvoice } = await import('../config/db-supabase.js');
        const invoice = await sbCreateTributosInvoice({
          id: crypto.randomUUID(), numero_factura: data.numero_factura,
          emisor_placeta_id: data.emisor_placeta_id, receptor_placeta_id: data.receptor_placeta_id,
          fecha_emision: data.fecha_emision ? new Date(data.fecha_emision).toISOString() : new Date().toISOString(),
          csv_verificacion: `CSV-${crypto.randomUUID().slice(0,8).toUpperCase()}-${Date.now().toString(36).toUpperCase()}`,
          lineas: [{ concepto_producto: data.concepto_producto, cantidad: Number(data.cantidad) || 1, precio_unitario: Number(data.precio_unitario) || 0, iva_porcentaje: Number(data.iva_porcentaje) || 12 }]
        });
        ok = true; msg = `✅ Factura ${data.numero_factura} emitida.`;
        html = `<div style="background:var(--p);color:var(--w);border-radius:12px;padding:16px;text-align:center;margin-top:12px"><div style="font-size:12px;opacity:.8">FACTURA EMITIDA</div><div style="font-size:18px;font-weight:800;margin:6px 0">${data.numero_factura}</div><div style="font-size:12px;opacity:.8">CSV: ${invoice.csv_verificacion}</div><div style="display:flex;justify-content:center;gap:16px;margin-top:8px;font-size:13px">Base: ${(invoice.base_imponible || 0).toFixed(2)} Pz · IVA: ${(invoice.total_iva || 0).toFixed(2)} Pz · Total: ${(invoice.total_factura || 0).toFixed(2)} Pz</div></div>`;
        break;
      }
      default: return res.json({ ok: false, error: 'Trámite no válido' });
    }
    res.json({ ok, msg, html: html || '' });
  } catch (err) {
    console.error('Error en trámite:', err);
    res.json({ ok: false, error: 'Error interno del servidor.' });
  }
});

// ── API: Documentos del usuario ──────────────────────────────────────────────

router.get('/api/tramites/mis-documentos', async (req, res) => {
  if (!req.session.usuario) return res.json([]);
  const docs = await sbFindDocumentosByUsuario(req.session.usuario.id, 20);
  res.json(docs.map(d => ({ tipo: d.tipo, referencia: d.referencia, creado_en: d.creado_en, pdf_url: d.pdf_url })));
});

// ── ÁREA DE CIUDADANO (requiere autenticación) ─────────────────────────────

function verificarAuth(req, res, next) {
  if (!req.session.usuario) return res.redirect('/login');
  next();
}

router.get('/ciudadano', verificarAuth, async (req, res) => {
  const usuario = await sbFindSolicitanteById(req.session.usuario.id);
  const docs = await sbFindDocumentosByUsuario(req.session.usuario.id, 10);
  const notifs = await sbFindLogsByUsuario(req.session.usuario.id, 15);
  res.render('public/ciudadano/dashboard', {
    titulo: 'Mi Área', layout: 'layouts/publico', pathActual: '/ciudadano',
    usuario, docs, notifs
  });
});

router.get('/ciudadano/documentos', verificarAuth, async (req, res) => {
  const docs = await sbFindDocumentosByUsuario(req.session.usuario.id, 100);
  res.render('public/ciudadano/documentos', { titulo: 'Mis Documentos', layout: 'layouts/publico', pathActual: '/ciudadano', docs });
});

router.get('/ciudadano/notificaciones', verificarAuth, async (req, res) => {
  const notifs = await sbFindLogsByUsuario(req.session.usuario.id, 50);
  res.render('public/ciudadano/notificaciones', { titulo: 'Notificaciones', layout: 'layouts/publico', pathActual: '/ciudadano', notifs });
});

router.get('/ciudadano/firmas', verificarAuth, async (req, res) => {
  const pendientes = await sbListDocumentosFirmadosPendientes(req.session.usuario.id);
  const db = getDb();
  const firmados = db.prepare("SELECT * FROM documentos_firmados WHERE firmado_por = ? AND estado = 'firmado' ORDER BY creado_en DESC").all(req.session.usuario.id);
  res.render('public/ciudadano/firmas', { titulo: 'Firmas', layout: 'layouts/publico', pathActual: '/ciudadano', pendientes, firmados });
});

// ── JUNTA DIRECTIVA ─────────────────────────────────────────────────────────

router.get('/junta', async (req, res) => {
  const db = getDb();
  const controles = db.prepare("SELECT * FROM config_control_parental ORDER BY creado_en DESC").all();
  const quejas = await sbListQuejas(20);
  res.render('public/junta', {
    titulo: 'Junta Directiva', layout: 'layouts/publico', pathActual: '/junta',
    controles, quejas
  });
});

// (Configuración de controles parentales movida al panel admin del CRM)

// ── Helpers ─────────────────────────────────────────────────────────────────
function generarDIP() {
  const num = Math.floor(10000000 + Math.random() * 90000000);
  const letras = 'TRWAGMYFPDXBNJZSQVHLCKE';
  return `${num}${letras[num % 23]}`;
}

export default router;
