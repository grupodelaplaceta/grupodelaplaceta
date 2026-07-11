import { Router } from 'express';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import {
  sbFindSolicitante, sbFindSolicitanteByEmail, sbFindSolicitanteByDip,
  sbCreateSolicitante, sbUpdateSolicitante,
  sbCreateSolicitudDip,
  sbFindControlParentalByCodigo, sbCreateControlParental,
  sbCreateJunior, sbFindJuniorByDip, sbFindJuniorByTutor,
  sbCreateLog, sbCreateJuniorLog, sbCreatePlacetaTransaction
} from '../config/db-supabase.js';
// Funciones bancarias se llaman via API del backend-banco
async function bonoBienvenida({ juniorAccountId, juniorDip, tutorDip }) {
  try {
    const BRIDGE = process.env.MONGO_BRIDGE_URL || 'http://localhost:8787';
    const r = await fetch(`${BRIDGE}/api/state`);
    if (!r.ok) throw new Error('Banco no disponible');
    const state = await r.json();
    const from = state.accounts.find(a => a.id === 'AGLDP');
    const to = state.accounts.find(a => a.id === juniorAccountId);
    if (!from || !to) throw new Error('Cuentas no encontradas');
    if (from.balancePz < 750) throw new Error('Saldo insuficiente en AGLDP');
    from.balancePz -= 750;
    to.balancePz += 750;
    state.transactions = [...(state.transactions || []), {
      id: `pj-bono-${Date.now()}`, kind: 'Gift', fromAccountId: 'AGLDP', toAccountId: juniorAccountId,
      amountPz: 750, ivaPz: 0, netAmount: 750, concept: 'Bono Bienvenida Placeta Junior' + (tutorDip === '11111111D' ? ' (Demo)' : ''),
      status: 'Settled', createdAt: new Date().toISOString()
    }];
    await fetch(`${BRIDGE}/api/state`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(state) });
    return { success: true };
  } catch (e) { return { success: false, error: e.message }; }
}
async function crearCuentaInfantil({ juniorDip, juniorNombre, tutorAccountId, sendLimitPz }) {
  return { accountId: `u-${juniorDip?.toLowerCase().replace(/-/g, '')}`, iban: `CAPI-${Date.now().toString(36).toUpperCase()}`, exists: false };
}

const router = Router();

// ═══════════════════════════════════════════════════════════════════════════
//  REGISTRO — Placeta Junior (<16) y Placeta Joven (16-17)
// ═══════════════════════════════════════════════════════════════════════════

router.post('/register', async (req, res) => {
  try {
    const { nombre, apellidos, fecha_nacimiento, nombre_tutor, apellidos_tutor, dni_tutor, email, password } = req.body;

    // ── Validaciones básicas ──────────────────────────────────────────────
    if (!nombre || !apellidos || !fecha_nacimiento || !email || !password) {
      return res.status(400).json({ error: 'Todos los campos obligatorios deben completarse.' });
    }
    if (!nombre_tutor || !apellidos_tutor || !dni_tutor) {
      return res.status(400).json({ error: 'Los datos del tutor legal son obligatorios.' });
    }

    const nacimiento = new Date(fecha_nacimiento);
    const hoy = new Date(2026, 6, 11);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mesDiff = hoy.getMonth() - nacimiento.getMonth();
    if (mesDiff < 0 || (mesDiff === 0 && hoy.getDate() < nacimiento.getDate())) edad--;

    // Solo menores de 16 años
    if (edad >= 16) {
      return res.status(400).json({ error: 'Placeta Junior es solo para menores de 16 años. Los mayores deben usar PlacetaID estándar.' });
    }

    // Validar email único
    const emailExistente = await sbFindSolicitanteByEmail(email);
    if (emailExistente) {
      return res.status(400).json({ error: 'El email ya está registrado en el sistema.' });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    // ── Generar DIP ───────────────────────────────────────────────────────
    const dipRaw = crypto.randomBytes(4).toString('hex').toUpperCase();
    const dip = `JUNIOR-${dipRaw}`;

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
          nombre_real: `${nombre_tutor} ${apellidos_tutor}`,
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
          nombre_real: `${nombre_tutor} ${apellidos_tutor}`,
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
      email,
      fecha_nacimiento,
      edad,
      dip,
      placeid: `PLID-J${dipRaw}`,
      franja_edad: 'Tutelada_Basica',
      password_hash: hashedPassword,
      rol: 'miembro',
      estado: 'pendiente',
      ip_registro: ip,
      tutor_id: tutorRecord.id
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
      tutor_nombre: `${nombre_tutor} ${apellidos_tutor}`,
      dni_tutor_hash: dniHash,
      dni_tutor_salt: salt,
      email_contacto: email,
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

    // ── Respuesta — siempre pendiente de firma del tutor ──────────────────
    return res.json({
      success: true,
      message: 'Registro completado. El tutor legal debe firmar el alta, los términos y condiciones, y la política de privacidad desde PlacetaID Móvil para generar el DIP Digital y activar la cuenta.',
      redirect: '/registro/pendiente-firma',
      dip,
      necesita_firma_tutor: true,
      junior_id: juniorRecord.id,
      tutor_dip: dni_tutor
    });
  } catch (err) {
    console.error('[Placeta Junior] Error en registro:', err);
    res.status(500).json({ error: 'Error interno del servidor. Inténtelo de nuevo más tarde.' });
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
