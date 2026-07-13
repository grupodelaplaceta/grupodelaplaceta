import { Router } from 'express';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import {
  sbFindSolicitante, sbFindSolicitanteByEmail, sbFindSolicitanteByDip,
  sbCreateSolicitante, sbUpdateSolicitante,
  sbCreateSolicitudDip,
  sbFindControlParentalByCodigo, sbFindControlParentalByDni, sbCreateControlParental,
  sbCreateJunior, sbFindJuniorByDip, sbFindJuniorByTutor,
  sbCreateLog, sbCreateJuniorLog, sbCreatePlacetaTransaction,
  sbCreateTributosContributor, sbGetTributosContributorByPlacetaId
} from '../config/db-supabase.js';

const BANCO_API = (process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org').replace(/\/+$/, '');
const PLACETAID_API = process.env.PLACETAID_API_URL || 'http://localhost:3000';
const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';

async function apiBanco(action, data = {}) {
  const r = await fetch(`${BANCO_API}/api/crm-state`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
    body: JSON.stringify({ action, ...data }),
    signal: AbortSignal.timeout(10000)
  });
  if (!r.ok) {
    const err = await r.json().catch(() => ({ error: r.statusText }));
    throw new Error(err.error || 'Error en API banco');
  }
  return r.json();
}

const router = Router();

// ═══════════════════════════════════════════════════════════════════════════
//  REGISTRO — Placeta Junior (<16) y Placeta Joven (16-17)
// ═══════════════════════════════════════════════════════════════════════════

router.post('/register', async (req, res) => {
  try {
    const { nombre, apellidos, fecha_nacimiento, nombre_tutor, apellidos_tutor, dni_tutor, email, password, tutor_ya_existe } = req.body;

    // ── Validaciones básicas ──────────────────────────────────────────────
    if (!nombre || !apellidos || !fecha_nacimiento || !email || !password) {
      return res.status(400).json({ error: 'Todos los campos obligatorios deben completarse.' });
    }
    if (!dni_tutor) {
      return res.status(400).json({ error: 'El DNI del tutor legal es obligatorio.' });
    }
    if (!tutor_ya_existe && (!nombre_tutor || !apellidos_tutor)) {
      return res.status(400).json({ error: 'Los datos del tutor legal son obligatorios.' });
    }
    // When tutor already exists, use placeholder names (will be filled from DB)
    const tutorFirstName = tutor_ya_existe ? (nombre_tutor || 'Tutor') : nombre_tutor;
    const tutorLastName = tutor_ya_existe ? (apellidos_tutor || '') : apellidos_tutor;

    const nacimiento = new Date(fecha_nacimiento);
    const hoy = new Date(2026, 6, 11);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mesDiff = hoy.getMonth() - nacimiento.getMonth();
    if (mesDiff < 0 || (mesDiff === 0 && hoy.getDate() < nacimiento.getDate())) edad--;

    if (edad >= 16) {
      return res.status(400).json({ error: 'Placeta Junior es solo para menores de 16 años. Los mayores deben usar PlacetaID estándar.' });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const dipRaw = crypto.randomBytes(4).toString('hex').toUpperCase();
    const dip = `JUNIOR-${dipRaw}`;

    // ── Generar email único para el menor ─────────────────────────────────
    let minorEmail = email;
    if (tutor_ya_existe) {
      minorEmail = `junior-${dipRaw.toLowerCase()}@laplaceta.org`;
      const exist = await sbFindSolicitanteByEmail(minorEmail).catch(() => null);
      if (exist) minorEmail = `junior-${dipRaw.toLowerCase()}-${Date.now()}@laplaceta.org`;
    } else {
      const emailExistente = await sbFindSolicitanteByEmail(email);
      if (emailExistente) {
        return res.status(400).json({ error: 'El email ya está registrado en el sistema.' });
      }
    }

    // ── Encriptar DNI del tutor ───────────────────────────────────────────
    const salt = crypto.randomBytes(16).toString('hex');
    const dniHash = crypto.createHash('sha256').update(dni_tutor + salt).digest('hex');

    // ── Buscar o crear tutor (puede estar ya registrado en PlacetaID) ──
    let tutorRecord = null;
    tutorRecord = await sbFindSolicitanteByDip(dni_tutor);
    if (!tutorRecord) {
      // Buscar por DNI en control_parental
      const cpRecords = await sbFindControlParentalByDni(dni_tutor);
      if (cpRecords && cpRecords.length > 0) {
        const cp = cpRecords[0];
        const tutorAlias = `tutor-${dipRaw.slice(0, 6)}`;
        tutorRecord = await sbCreateSolicitante({
          alias: tutorAlias,
          nombre_real: `${tutorFirstName} ${tutorLastName}`.trim(),
          email: cp.email_tutor || email,
          dip: dni_tutor,
          franja_edad: 'Alta_Plena',
          rol: 'tutor',
          estado: 'activo',
          ip_registro: ip
        });
      } else {
        // Crear registro del tutor si no existe
        const tutorAlias = `tutor-${dipRaw.slice(0, 6)}`;
        tutorRecord = await sbCreateSolicitante({
          alias: tutorAlias,
          nombre_real: `${tutorFirstName} ${tutorLastName}`.trim(),
          email: email,
          dip: dni_tutor,
          franja_edad: 'Alta_Plena',
          rol: 'tutor',
          estado: 'activo',
          ip_registro: ip
        });
      }
    }

    // ── Crear solicitante (menor) ────────────────────────────────────────
    const hashedPassword = await bcrypt.hash(password, 10);
    const alias = `${nombre.toLowerCase().replace(/\s/g, '')}.${dipRaw.slice(0, 4).toLowerCase()}`;

    const nuevoSolicitante = await sbCreateSolicitante({
      alias,
      nombre_real: `${nombre} ${apellidos}`,
      email: minorEmail,
      fecha_nacimiento,
      edad,
      dip,
      placeid: `PLID-J${dipRaw}`,
      franja_edad: 'Tutelada_Basica',
      password_hash: hashedPassword,
      rol: 'miembro',
      estado: 'pendiente',
      ip_registro: ip
    });

    // ── Crear registro en junior_menores ─────────────────────────────────
    const juniorRecord = await sbCreateJunior({
      solicitante_id: nuevoSolicitante.id,
      dip,
      nombre, apellidos,
      fecha_nacimiento,
      edad,
      modalidad: 'Placeta Junior',
      tutor_dip: dni_tutor,
      tutor_nombre: `${tutorFirstName} ${tutorLastName}`.trim(),
      dni_tutor_hash: dniHash,
      dni_tutor_salt: salt,
      email_contacto: minorEmail,
      estado: 'pendiente_firma_tutor',
      placetas_saldo: 0,
      nivel_academia: 1,
      ip_registro: ip
    });

    // ── Logs ──────────────────────────────────────────────────────────────
    await sbCreateLog({
      usuario_id: nuevoSolicitante.id,
      accion: 'registro_junior',
      detalle: `Registro Placeta Junior: ${nombre} ${apellidos} (DIP: ${dip}, Tutor: ${dni_tutor})`,
      ip
    });

    await sbCreateJuniorLog({
      junior_id: juniorRecord.id,
      accion: 'registro',
      detalle: `Registro completado. Edad: ${edad}. Pendiente firma del tutor vía PlacetaID Móvil.`,
      ip
    });

    // ── Crear solicitud de firma PlacetaID para el tutor ────────────────
    let placetaidRequest = null;
    try {
      const placetaIdResp = await fetch(`${PLACETAID_API}/api/mobil/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          dip: dni_tutor,
          servicio: 'Placeta Junior - Registro',
          servicioUrl: `${process.env.CRM_BASE_URL || 'http://localhost:3001'}/junior-auth/verify/${juniorRecord.id}`,
          plataforma: 'web'
        }),
        signal: AbortSignal.timeout(5000)
      });
      if (placetaIdResp.ok) {
        placetaidRequest = await placetaIdResp.json();
        await sbCreateJuniorLog({
          junior_id: juniorRecord.id,
          accion: 'placetaid_solicitud',
          detalle: `Solicitud de firma PlacetaID creada. Código: ${placetaidRequest.codigo}. RequestId: ${placetaidRequest.requestId}`,
          ip
        });
      }
    } catch (pidErr) {
      console.warn('[Placeta Junior] Error creando solicitud PlacetaID:', pidErr.message);
    }

    // ── Respuesta — siempre pendiente de firma del tutor ──────────────────
    const responseData = {
      success: true,
      message: 'Registro completado. El tutor legal debe firmar el alta, los términos y condiciones, y la política de privacidad desde PlacetaID Móvil para generar el DIP Digital y activar la cuenta.',
      redirect: '/registro/pendiente-firma',
      dip,
      necesita_firma_tutor: true,
      junior_id: juniorRecord.id,
      tutor_dip: dni_tutor,
      tutor_nombre: tutorRecord?.nombre_real || (tutorFirstName + ' ' + tutorLastName).trim(),
      tutor_email: tutorRecord?.email || email
    };

    if (placetaidRequest) {
      responseData.placetaid_codigo = placetaidRequest.codigo;
      responseData.placetaid_requestId = placetaidRequest.requestId;
    }

    return res.json(responseData);
  } catch (err) {
    console.error('[Placeta Junior] Error en registro:', err);
    res.status(500).json({ error: 'Error interno del servidor. Inténtelo de nuevo más tarde.' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  LOGIN — Por DIP del menor + verificación PlacetaID del tutor
// ═══════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════
//  TUTOR INFO — Obtener datos del tutor por DIP para pre-rellenar
// ═══════════════════════════════════════════════════════════════════════════

router.get('/tutor-info/:dip', async (req, res) => {
  try {
    const dip = req.params.dip;
    if (!dip) return res.status(400).json({ error: 'DIP requerido' });

    const tutor = await sbFindSolicitanteByDip(dip).catch(() => null);
    if (!tutor) return res.status(404).json({ error: 'Tutor no encontrado' });

    res.json({
      success: true,
      dip: tutor.dip,
      nombre: tutor.nombre_real || '',
      email: tutor.email || ''
    });
  } catch (err) {
    res.status(500).json({ error: 'Error interno' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  LOGIN — Por DIP del menor + verificación PlacetaID del tutor
// ═══════════════════════════════════════════════════════════════════════════

router.post('/login', async (req, res) => {
  try {
    const { dip, password } = req.body;
    if (!dip || !password) return res.status(400).json({ error: 'DIP y contraseña requeridos' });

    // Buscar por DIP
    const usuario = await sbFindSolicitanteByDip(dip);
    if (!usuario) return res.status(401).json({ error: 'Credenciales inválidas' });

    // Verificar que sea un junior
    const junior = await sbFindJuniorByDip(usuario.dip);
    if (!junior) return res.status(403).json({ error: 'Esta cuenta no tiene acceso a Placeta Junior.' });

    if (junior.estado === 'pendiente_firma_tutor') {
      return res.status(403).json({ error: 'El tutor legal debe firmar el alta desde PlacetaID Móvil antes de acceder.' });
    }

    if (junior.estado === 'suspendido' || junior.estado === 'baja') {
      return res.status(403).json({ error: 'Cuenta suspendida o dada de baja.' });
    }

    const validPassword = await bcrypt.compare(password, usuario.password_hash);
    if (!validPassword) return res.status(401).json({ error: 'Credenciales inválidas' });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    // ── Verificar acceso con el tutor vía PlacetaID ────────────────────
    const { solicitarAutorizacionTutor, verificarTutor } = await import('../services/placetaidService.js');

    // Verificar que el tutor esté registrado en PlacetaID
    if (junior.tutor_dip) {
      const tutorStatus = await verificarTutor(junior.tutor_dip);
      if (!tutorStatus.existe) {
        console.warn(`[Login] Tutor ${junior.tutor_dip} no encontrado en PlacetaID`);
      } else {
        // Solicitar autorización al tutor vía PlacetaID Móvil
        // El tutor recibe una notificación en su app y debe aprobar el acceso
        const authReq = await solicitarAutorizacionTutor({
          dipTutor: junior.tutor_dip,
          concepto: `Acceso de ${junior.nombre} ${junior.apellidos} a Placeta Junior`,
          monto: 0,
          dipMenor: junior.dip,
          detalles: `Inicio de sesión del menor ${junior.nombre} (DIP: ${junior.dip})`
        });

        if (authReq.success) {
          // Guardar requestId en sesión para polling
          req.session.authRequestId = authReq.requestId;
          req.session.authCodigo = authReq.codigo;

          return res.json({
            success: true,
            requiere_autorizacion_tutor: true,
            requestId: authReq.requestId,
            codigo: authReq.codigo,
            mensaje: 'Se ha enviado una solicitud al tutor. Debe aprobarla desde PlacetaID Móvil.',
            dip_menor: junior.dip,
            nombre_menor: `${junior.nombre} ${junior.apellidos}`
          });
        }
      }
    }

    // Si no hay tutor o falla la verificación, login directo
    await sbUpdateSolicitante(usuario.id, {
      ultimo_acceso: new Date().toISOString(),
      ip_ultimo_acceso: ip
    });

    await sbCreateLog({
      usuario_id: usuario.id,
      accion: 'login_junior',
      detalle: `Inicio de sesión en Placeta Junior`,
      ip
    });

    req.session.junior = {
      id: junior.id,
      solicitante_id: usuario.id,
      dip: usuario.dip,
      nombre: junior.nombre, apellidos: junior.apellidos,
      alias: usuario.alias, edad: junior.edad,
      modalidad: junior.modalidad,
      placetas_saldo: junior.placetas_saldo,
      nivel_academia: junior.nivel_academia, estado: junior.estado
    };

    res.json({ success: true, redirect: '/dashboard', junior: req.session.junior });
  } catch (err) {
    console.error('[Placeta Junior] Error en login:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  POLL — Verificar si el tutor autorizó el acceso
// ═══════════════════════════════════════════════════════════════════════════

router.get('/login/poll/:requestId', async (req, res) => {
  try {
    const { verificarAutorizacion } = await import('../services/placetaidService.js');
    const result = await verificarAutorizacion(req.params.requestId);

    if (result.aprobado) {
      // Autorizado — completar login
      const { requestId } = req.params;
      return res.json({ success: true, aprobado: true, redirect: '/dashboard' });
    }

    res.json({ success: true, aprobado: false, status: result.status || 'pending' });
  } catch (err) {
    res.json({ success: false, aprobado: false, error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  VERIFY — Webhook para PlacetaID: tutor firmó el registro
// ═══════════════════════════════════════════════════════════════════════════

router.get('/verify/:juniorId', async (req, res) => {
  try {
    const { juniorId } = req.params;
    if (!juniorId) return res.status(400).json({ error: 'ID de junior requerido' });

    // Buscar junior por ID en Supabase directamente
    const { supabase } = await import('../config/supabase.js');
    const { data: junior } = await supabase
      .from('junior_menores')
      .select('*, tutor:solicitantes!junior_menores_tutor_dip_fkey(dip, alias, nombre_real, email)')
      .eq('id', juniorId)
      .single()
      .catch(() => ({ data: null }));
    if (!junior) {
      return res.status(404).send(`
        <!DOCTYPE html><html><head><meta charset="utf-8"><title>Error</title>
        <style>body{font-family:sans-serif;text-align:center;padding:40px;color:#333}
        .error{color:#c33;font-size:24px;margin:20px 0}</style></head><body>
        <div class="error">❌</div>
        <h1>Menor no encontrado</h1>
        <p>El enlace de verificación no es válido o el registro ya fue procesado.</p>
        </body></html>
      `);
    }

    res.send(`
      <!DOCTYPE html><html><head><meta charset="utf-8">
      <title>Verificación Placeta Junior</title>
      <meta name="viewport" content="width=device-width,initial-scale=1">
      <link rel="preconnect" href="https://fonts.googleapis.com">
      <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
      <style>
        *{margin:0;padding:0;box-sizing:border-box}
        body{font-family:'Plus Jakarta Sans',sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
        .card{background:#fff;border-radius:24px;padding:40px;max-width:420px;width:100%;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,.15)}
        .icon{font-size:64px;margin-bottom:16px}
        h1{font-size:24px;font-weight:800;color:#1a1a2e;margin-bottom:8px}
        p{color:#666;font-size:14px;line-height:1.6;margin-bottom:24px}
        .code{background:#f0f0ff;border:2px dashed #667eea;border-radius:12px;padding:16px;font-size:32px;font-weight:800;letter-spacing:8px;color:#1a1a2e;margin:16px 0}
        .status{display:inline-block;padding:8px 20px;border-radius:20px;font-size:13px;font-weight:600;margin:8px 0}
        .status-pending{background:#fff3cd;color:#856404}
        .status-active{background:#d4edda;color:#155724}
        .btn{display:inline-block;padding:12px 32px;border-radius:12px;border:none;font-family:inherit;font-size:15px;font-weight:700;cursor:pointer;text-decoration:none;transition:transform .2s,box-shadow .2s}
        .btn-primary{background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;box-shadow:0 4px 15px rgba(102,126,234,.4)}
        .btn-primary:hover{transform:translateY(-2px);box-shadow:0 8px 25px rgba(102,126,234,.5)}
        .btn-secondary{background:#f0f0ff;color:#667eea;margin-top:12px}
        .detail{background:#f8f9fa;border-radius:12px;padding:16px;text-align:left;margin:16px 0}
        .detail-row{display:flex;justify-content:space-between;padding:6px 0;font-size:13px;border-bottom:1px solid #eee}
        .detail-row:last-child{border-bottom:none}
        .detail-label{color:#888}
        .detail-value{font-weight:600;color:#333}
        @media(max-width:480px){.card{padding:24px}.code{font-size:24px;letter-spacing:4px}}
      </style></head><body>
        <div class="card">
          <div class="icon">${junior.estado === 'activo' ? '✅' : '⏳'}</div>
          <h1>${junior.estado === 'activo' ? '¡Registro Verificado!' : 'Pendiente de Firma'}</h1>
          <p>${junior.estado === 'activo'
            ? 'El tutor legal ha firmado el alta. El menor ya puede acceder a Placeta Junior.'
            : 'El tutor legal debe abrir PlacetaID Móvil y aprobar la solicitud de firma para activar la cuenta.'}</p>
          <div class="detail">
            <div class="detail-row"><span class="detail-label">Menor</span><span class="detail-value">${junior.nombre || ''} ${junior.apellidos || ''}</span></div>
            <div class="detail-row"><span class="detail-label">DIP</span><span class="detail-value">${junior.dip || ''}</span></div>
            <div class="detail-row"><span class="detail-label">Tutor</span><span class="detail-value">${junior.tutor_nombre || ''}</span></div>
            <div class="detail-row"><span class="detail-label">Estado</span><span class="status ${junior.estado === 'activo' ? 'status-active' : 'status-pending'}">${junior.estado === 'activo' ? '✅ Activo' : '⏳ Pendiente firma'}</span></div>
          </div>
          ${junior.estado === 'activo'
            ? '<a href="/junior/login" class="btn btn-primary">Ir a Placeta Junior</a>'
            : '<div class="code">ABCD</div><p style="font-size:12px;color:#999">O introduce el código en PlacetaID Móvil</p>'
          }
          <a href="/junior/register" class="btn btn-secondary">← Volver</a>
        </div>
      </body></html>
    `);
  } catch (err) {
    console.error('[Verify] Error:', err.message);
    res.status(500).send('Error interno');
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  LOGOUT
// ═══════════════════════════════════════════════════════════════════════════

router.get('/logout', (req, res) => {
  req.session = null;
  res.redirect('/');
});

router.post('/logout', (req, res) => {
  req.session = null;
  res.json({ success: true, redirect: '/' });
});

export default router;
