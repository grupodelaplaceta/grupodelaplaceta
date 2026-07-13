import { Router } from 'express';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import {
  sbFindSolicitanteByDip, sbUpdateSolicitante,
  sbFindJuniorByDip, sbFindJuniorByTutor, sbListJuniors, sbUpdateJunior,
  sbGetParentalLimits, sbCreateParentalLimit, sbUpdateParentalLimits,
  sbCreateLog, sbCreateJuniorLog, sbCreatePlacetaTransaction,
  sbCreateTributosContributor, sbGetTributosContributorByPlacetaId
} from '../config/db-supabase.js';
import { supabase } from '../config/supabase.js';

const BANCO_API = (process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org').replace(/\/+$/, '');
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
//  PERFIL JUNIOR
// ═══════════════════════════════════════════════════════════════════════════

router.get('/perfil', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    const limites = await sbGetParentalLimits(junior.id);

    res.json({
      junior,
      limites_parentales: limites
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  VINCULACIÓN — Aceptar menor desde PlacetaID
// ═══════════════════════════════════════════════════════════════════════════

router.post('/vincular', async (req, res) => {
  try {
    const { dip_tutor, dip_menor, firma_hash } = req.body;
    if (!dip_tutor || !dip_menor || !firma_hash) {
      return res.status(400).json({ error: 'Faltan datos para la vinculación.' });
    }

    // Verificar tutor
    const tutor = await sbFindSolicitanteByDip(dip_tutor);
    if (!tutor) return res.status(404).json({ error: 'Tutor no encontrado' });

    // Verificar menor
    const junior = await sbFindJuniorByDip(dip_menor);
    if (!junior) return res.status(404).json({ error: 'Menor no encontrado' });

    if (junior.estado !== 'pendiente_firma_tutor') {
      return res.status(400).json({ error: 'El menor ya ha sido vinculado o no está en estado pendiente.' });
    }

    // Validar que el DIP del tutor coincide
    if (junior.tutor_dip !== dip_tutor) {
      return res.status(403).json({ error: 'Este tutor no está autorizado para vincular a este menor.' });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    // Actualizar estado del menor
    await sbUpdateJunior(junior.id, {
      estado: 'activo',
      firma_hash,
      firmado_en: new Date().toISOString(),
      ip_firma: ip
    });

    // Actualizar estado del solicitante
    await sbUpdateSolicitante(junior.solicitante_id, { estado: 'activo' });

    // ── Crear cuenta bancaria infantil con IBAN APP (Capitalia) ───────
    let cuentaInfo = null;
    try {
      const tutorAccountId = `u-${dip_tutor?.toLowerCase().replace(/-/g, '')}`;

      cuentaInfo = await apiBanco('crear-cuenta-infantil', {
        juniorDip: junior.dip,
        juniorNombre: `${junior.nombre} ${junior.apellidos}`,
        tutorAccountId,
        sendLimitPz: 50,
        tutorDip: dip_tutor
      });

      // ── Dar bono de bienvenida de 750 Pz (AGLDP → menor) ──────────
      try {
        await apiBanco('bono-bienvenida', {
          juniorAccountId: cuentaInfo.accountId,
          juniorDip: junior.dip,
          tutorDip: dip_tutor
        });
        // Registrar las 750 placetas internas también
        const nuevoSaldo = (junior.placetas_saldo || 0) + 750;
        await sbUpdateJunior(junior.id, { placetas_saldo: nuevoSaldo, limite_gasto_diario: 50, limite_gasto_semanal: 200 });
        await sbCreatePlacetaTransaction({
          junior_id: junior.id,
          tipo: 'bonus',
          concepto: '🎉 Bono de bienvenida Placeta Junior',
          cantidad: 750,
          saldo_resultante: nuevoSaldo,
          ip
        });
      } catch (bonusErr) {
        console.warn('[Junior] Error bono bienvenida:', bonusErr.message);
      }
    } catch (bankErr) {
      console.warn('[Junior] Error creando cuenta bancaria:', bankErr.message);
    }

    // ── Dar de alta al tutor y al menor en Tributos ──────────────────
    try {
      const juniorPlacetaId = `PLID-J${junior.dip?.split('-')[1] || '0000'}`;
      const tutorPlacetaId = `PLID-${dip_tutor}`;

      // Tutor como contribuyente (si no existe)
      const tutorExisting = await sbGetTributosContributorByPlacetaId(tutorPlacetaId).catch(() => null);
      if (!tutorExisting) {
        await sbCreateTributosContributor({
          id: crypto.randomUUID(),
          placeta_id: tutorPlacetaId,
          dip: dip_tutor,
          nombre: junior.tutor_nombre || `Tutor ${dip_tutor}`,
          tipo_sujeto: 'Fisico',
          estado_fiscal: 'Al Dia',
          fecha_alta_tributos: new Date().toISOString(),
          roles_json: ['ciudadano', 'tutor'],
          iban: null, eip: null
        });
      }

      // Menor como contribuyente
      const juniorExisting = await sbGetTributosContributorByPlacetaId(juniorPlacetaId).catch(() => null);
      if (!juniorExisting) {
        await sbCreateTributosContributor({
          id: crypto.randomUUID(),
          placeta_id: juniorPlacetaId,
          dip: junior.dip,
          nombre: `${junior.nombre} ${junior.apellidos}`,
          tipo_sujeto: 'Fisico',
          estado_fiscal: 'Al Dia',
          fecha_alta_tributos: new Date().toISOString(),
          roles_json: ['ciudadano', 'menor'],
          iban: cuentaInfo?.iban || null,
          eip: null
        });
      }

      await sbCreateJuniorLog({
        junior_id: junior.id,
        accion: 'alta_tributos',
        detalle: `Menor y tutor dados de alta en Tributos GDLP`,
        ip
      });
    } catch (tribErr) {
      console.warn('[Junior] Error alta tributos:', tribErr.message);
    }

    // Logs
    await sbCreateLog({
      usuario_id: tutor.id,
      accion: 'vincular_junior',
      detalle: `Tutor ${dip_tutor} vinculó a menor ${dip_menor}${cuentaInfo ? '. Cuenta bancaria creada + 750 Pz bono' : ''}`,
      ip
    });

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'vinculacion_tutor',
      detalle: `Vinculado al tutor ${dip_tutor}. Cuenta: ${cuentaInfo?.accountId || 'no creada'}. IBAN: ${cuentaInfo?.iban || '—'}`,
      ip
    });

    res.json({
      success: true,
      message: `Menor vinculado exitosamente.${cuentaInfo ? ' Cuenta bancaria infantil creada con 750 Pz de bienvenida.' : ''} Ya puede acceder a Placeta Junior.`,
      dip_menor,
      dip_tutor,
      cuenta_bancaria: cuentaInfo,
      tributos_dados_de_alta: true
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  MONEDERO — Saldo, límites e historial (con enforces del tutor)
// ═══════════════════════════════════════════════════════════════════════════

router.get('/monedero', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    // Límites del tutor (o valores por defecto)
    const limites = await sbGetParentalLimits(junior.id);
    const limitesEfectivos = limites ? {
      gasto_diario: limites.limite_gasto_diario || 10,
      gasto_semanal: limites.limite_gasto_semanal || 50,
      limite_aprobacion_tutor: limites.limite_aprobacion_tutor || 1000,
      tiempo_uso: limites.tiempo_uso_diario_minutos || 60,
      requiere_aprobacion: limites.requiere_aprobacion_extra !== false,
      categorias_bloqueadas: typeof limites.categorias_bloqueadas === 'string'
        ? JSON.parse(limites.categorias_bloqueadas || '[]')
        : (limites.categorias_bloqueadas || [])
    } : {
      gasto_diario: 10,
      gasto_semanal: 50,
      limite_aprobacion_tutor: 1000,
      tiempo_uso: 60,
      requiere_aprobacion: true,
      categorias_bloqueadas: []
    };

    // Historial de transacciones
    const { data: historial, error } = await supabase
      .from('junior_transacciones')
      .select('*')
      .eq('junior_id', junior.id)
      .order('creado_en', { ascending: false })
      .limit(30);

    // Calcular gasto del día y de la semana
    const hoy = new Date().toISOString().slice(0, 10);
    const inicioSemana = new Date();
    inicioSemana.setDate(inicioSemana.getDate() - inicioSemana.getDay());
    const semStr = inicioSemana.toISOString().slice(0, 10);

    const gastoHoy = (historial || [])
      .filter(t => t.tipo === 'gastar' && (t.creado_en || '').slice(0, 10) === hoy)
      .reduce((s, t) => s + (t.cantidad || 0), 0);

    const gastoSemana = (historial || [])
      .filter(t => t.tipo === 'gastar' && (t.creado_en || '') >= semStr)
      .reduce((s, t) => s + (t.cantidad || 0), 0);

    // Ingresos totales (ganar + bonus)
    const ingresos = (historial || [])
      .filter(t => t.tipo === 'ganar' || t.tipo === 'bonus')
      .reduce((s, t) => s + (t.cantidad || 0), 0);

    res.json({
      saldo_actual: junior.placetas_saldo || 0,
      ingresos_totales: ingresos,
      gasto_hoy: gastoHoy,
      gasto_semana: gastoSemana,
      limites: limitesEfectivos,
        saldo_disponible_hoy: Math.max(0, limitesEfectivos.gasto_diario - gastoHoy),
      saldo_disponible_semana: Math.max(0, limitesEfectivos.gasto_semanal - gastoSemana),
      historial: historial || [],
      nivel_academia: junior.nivel_academia || 1,
      cuenta_bancaria: {
        id: `u-${junior.dip?.toLowerCase().replace(/-/g, '')}`,
        tipo: 'Child',
        iban: `CAPI-${junior.dip?.split('-')[1] || '0000'}`,
        sendLimitPz: limitesEfectivos.gasto_diario
      }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  CONTROL PARENTAL — Configurar límites
// ═══════════════════════════════════════════════════════════════════════════

router.post('/control-parental', async (req, res) => {
  try {
    const { dip_tutor, dip_menor, limite_gasto_diario, limite_gasto_semanal, limite_aprobacion_tutor, tiempo_uso_diario, categorias_bloqueadas, requiere_aprobacion_extra } = req.body;

    if (!dip_tutor || !dip_menor) {
      return res.status(400).json({ error: 'Faltan datos del tutor o menor.' });
    }

    // Verificar tutor
    const tutor = await sbFindSolicitanteByDip(dip_tutor);
    if (!tutor) return res.status(404).json({ error: 'Tutor no encontrado' });

    // Verificar menor
    const junior = await sbFindJuniorByDip(dip_menor);
    if (!junior) return res.status(404).json({ error: 'Menor no encontrado' });

    if (junior.tutor_dip !== dip_tutor) {
      return res.status(403).json({ error: 'No eres el tutor de este menor.' });
    }

    // Verificar si ya existen límites
    const existentes = await sbGetParentalLimits(junior.id);

    const limitData = {
      junior_id: junior.id,
      dip_menor,
      dip_tutor,
      limite_gasto_diario: limite_gasto_diario || 10,
      limite_gasto_semanal: limite_gasto_semanal || 50,
      limite_aprobacion_tutor: limite_aprobacion_tutor || 1000, // Compras > esto requieren aprobación PlacetaID
      tiempo_uso_diario_minutos: tiempo_uso_diario || 60,
      categorias_bloqueadas: JSON.stringify(categorias_bloqueadas || []),
      requiere_aprobacion_extra: requiere_aprobacion_extra !== undefined ? requiere_aprobacion_extra : true,
      actualizado_en: new Date().toISOString()
    };

    if (existentes) {
      await sbUpdateParentalLimits(junior.id, limitData);
    } else {
      await sbCreateParentalLimit({ ...limitData, creado_en: new Date().toISOString() });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'control_parental_actualizado',
      detalle: `Tutor ${dip_tutor} actualizó límites parentales`,
      ip
    });

    res.json({
      success: true,
      message: 'Límites de control parental actualizados correctamente.'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  DIP DIGITAL — Generar DIP para el menor
// ═══════════════════════════════════════════════════════════════════════════

router.post('/generar-dip-digital', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    if (junior.estado !== 'activo') {
      return res.status(400).json({ error: 'El menor debe estar activo y vinculado al tutor para generar el DIP Digital.' });
    }

    // Generar DIP Digital (diferente del DIP de solicitante)
    const dipDigital = `DIP-DIGITAL-${junior.dip.split('-')[1]}-${crypto.randomBytes(2).toString('hex').toUpperCase()}`;

    await sbUpdateJunior(junior.id, {
      dip_digital: dipDigital,
      dip_digital_generado_en: new Date().toISOString()
    });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'dip_digital_generado',
      detalle: `DIP Digital generado: ${dipDigital}`,
      ip
    });

    res.json({
      success: true,
      message: 'DIP Digital generado correctamente.',
      dip_digital: dipDigital
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  LISTAR MENORES DE UN TUTOR
// ═══════════════════════════════════════════════════════════════════════════

router.get('/menores/:dipTutor', async (req, res) => {
  try {
    const menores = await sbFindJuniorByTutor(req.params.dipTutor);
    return res.json(Array.isArray(menores) ? menores : []);
  } catch (err) {
    return res.json([]);
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  SOLICITAR ACCESO — El menor pide permiso al tutor
// ═══════════════════════════════════════════════════════════════════════════

router.post('/solicitar-acceso', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'solicitar_acceso',
      detalle: `El menor solicita acceso a Placeta Junior. Pendiente de aprobación del tutor.`,
      ip
    });

    // Notificar al tutor vía PlacetaID (simulado)
    res.json({
      success: true,
      message: 'Solicitud enviada al tutor. Debe aceptarla desde PlacetaID Móvil.'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  APROBAR ACCESO — El tutor acepta desde PlacetaID
// ═══════════════════════════════════════════════════════════════════════════

router.post('/aprobar-acceso', async (req, res) => {
  try {
    const { dip_tutor, dip_menor } = req.body;
    if (!dip_tutor || !dip_menor) {
      return res.status(400).json({ error: 'Faltan datos.' });
    }

    const junior = await sbFindJuniorByDip(dip_menor);
    if (!junior) return res.status(404).json({ error: 'Menor no encontrado' });

    await sbUpdateJunior(junior.id, { ultimo_acceso_aprobado: new Date().toISOString() });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    await sbCreateJuniorLog({
      junior_id: junior.id,
      accion: 'acceso_aprobado',
      detalle: `Tutor ${dip_tutor} aprobó acceso del menor`,
      ip
    });

    res.json({
      success: true,
      message: 'Acceso aprobado. El menor ya puede usar Placeta Junior.'
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  MIDDLEWARE
// ═══════════════════════════════════════════════════════════════════════════


async function verificarJunior(req, res, next) {
  const dip = req.session?.junior?.dip || req.query.dip || req.body?.dip || req.headers['x-junior-dip'];
  if (!dip) return res.status(401).json({ error: 'No autorizado. Debes iniciar sesión.' });
  try {
    const junior = await sbFindJuniorByDip(dip);
    if (!junior) return res.status(401).json({ error: 'Perfil no encontrado.' });
    req.juniorDip = dip;
    req.juniorId = junior.id;
    req.juniorData = junior;
    next();
  } catch (e) {
    res.status(500).json({ error: 'Error verificando identidad.' });
  }
}

export default router;
