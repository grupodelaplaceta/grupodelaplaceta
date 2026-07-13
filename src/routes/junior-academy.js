import { Router } from 'express';
import {
  sbFindJuniorByDip, sbUpdateJunior,
  sbGetAcademyProgress, sbUpsertAcademyProgress,
  sbUpdatePlacetaBalance,
  sbCreatePlacetaTransaction,
  sbCreateJuniorLog, sbGetParentalLimits
} from '../config/db-supabase.js';
import { supabase } from '../config/supabase.js';
import { generarCuestionarios, COSTO_DESBLOQUEO_POR_NIVEL, getRangoEdad } from '../data/cuestionarios.js';

const BANCO_API = (process.env.BANCO_API_URL || 'https://api.banco.laplaceta.org').replace(/\/+$/, '');
const CRM_KEY = process.env.CRM_READ_KEY || 'crm-gdlp-shared-key-2026';

async function apiBanco(action, data = {}) {
  try {
    const r = await fetch(`${BANCO_API}/api/crm-state`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CRM-Key': CRM_KEY },
      body: JSON.stringify({ action, ...data }),
      signal: AbortSignal.timeout(10000)
    });
    if (!r.ok) throw new Error('Banco no disponible');
    return r.json();
  } catch (e) { return { success: false, error: e.message }; }
}

// Pago bancario via API backend-banco (legacy, kept for reference)
async function desbloquearNivelBanco({ juniorAccountId, costoPlacetas, juniorDip, tutorDip }) {
  try {
    const BRIDGE = process.env.MONGO_BRIDGE_URL || 'http://localhost:8787';
    const r = await fetch(`${BRIDGE}/api/state`);
    if (!r.ok) throw new Error('Banco no disponible');
    const state = await r.json();
    const from = state.accounts.find(a => a.id === juniorAccountId);
    const capitalia = state.accounts.find(a => a.id === 'CAPITALIA_BANK');
    if (!from || !capitalia) throw new Error('Cuentas no encontradas');
    const esDemo = tutorDip === '11111111D' || (juniorDip || '').includes('DEMO');
    if (!esDemo && from.balancePz < costoPlacetas) throw new Error('Saldo insuficiente');
    if (!esDemo) { from.balancePz -= costoPlacetas; capitalia.balancePz += costoPlacetas; }
    const iva = Math.ceil(costoPlacetas * 12 / 100);
    if (iva > 0 && !esDemo) {
      const tglp = state.accounts.find(a => a.id === 'TGLP');
      if (tglp) { capitalia.balancePz -= iva; tglp.balancePz += iva; }
    }
    const suffix = esDemo ? ' (Demo)' : '';
    state.transactions = [...(state.transactions || []), {
      id: `pj-nivel-${Date.now()}`, kind: 'Transfer', fromAccountId: juniorAccountId,
      toAccountId: 'CAPITALIA_BANK', amountPz: costoPlacetas, ivaPz: 0,
      concept: `Desbloquear nivel${suffix}`, status: 'Settled', createdAt: new Date().toISOString()
    }];
    await fetch(`${BRIDGE}/api/state`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(state) });
    return { success: true, esDemo };
  } catch (e) { return { success: false, error: e.message }; }
}

const router = Router();

// ═══════════════════════════════════════════════════════════════════════════
//  CUESTIONARIOS DISPONIBLES (servidor — desde data/cuestionarios.js)
// ═══════════════════════════════════════════════════════════════════════════

router.get('/cuestionarios', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    const rangoEdad = getRangoEdad(junior.edad);
    const nivelActual = junior.nivel_academia || 1;

    // Generar cuestionarios solo para niveles desbloqueados (1 al nivel actual)
    const cuestionarios = {};
    const materias = ['matematicas', 'calculo_mental', 'lengua', 'medio', 'geografia'];
    const nombresMateria = {
      matematicas: 'Matemáticas',
      calculo_mental: 'Cálculo Mental',
      lengua: 'Lengua',
      medio: 'Medio',
      geografia: '🌍 Geografía'
    };

    const maxNivelVisible = Math.min(nivelActual, 35);

    for (const materia of materias) {
      cuestionarios[materia] = {
        nombre: nombresMateria[materia],
        niveles: {}
      };
      for (let n = 1; n <= maxNivelVisible; n++) {
        const preguntas = generarCuestionarios(junior.edad, materia, n);
        cuestionarios[materia].niveles[n] = preguntas.map((p, idx) => ({
          id: `${materia}-${n}-${idx}`,
          pregunta: p.pregunta,
          opciones: p.opciones,
          dificultad: p.dificultad,
          placetas_recompensa: p.placetas_recompensa
        }));
      }
    }

    // Costos de desbloqueo (niveles 2-10)
    const costos = {};
    for (let n = 2; n <= 35; n++) {
      costos[n] = COSTO_DESBLOQUEO_POR_NIVEL[n] || 999;
    }

    const progreso = await sbGetAcademyProgress(junior.id);

    res.json({
      rango_edad: rangoEdad,
      nivel_actual: nivelActual,
      nivel_maximo: 10,
      placetas_saldo: junior.placetas_saldo || 0,
      costos_desbloqueo: costos,
      cuestionarios,
      progreso: progreso || { completados: {}, puntuacion_total: 0, nivel_maximo: 1 },
      materias_disponibles: materias.map(m => ({
        id: m,
        nombre: nombresMateria[m],
        niveles_disponibles: nivelActual
      }))
    });
  } catch (err) {
    console.error('[Academy] Error cargando cuestionarios:', err);
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  ENVIAR RESPUESTAS (evaluación en servidor)
// ═══════════════════════════════════════════════════════════════════════════

router.post('/evaluar', verificarJunior, async (req, res) => {
  try {
    const { materia, nivel, respuestas } = req.body;
    if (!materia || !nivel || !respuestas || !Array.isArray(respuestas)) {
      return res.status(400).json({ error: 'Faltan datos: materia, nivel y respuestas.' });
    }

    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    // Verificar nivel desbloqueado
    if (nivel > (junior.nivel_academia || 1)) {
      return res.status(403).json({ error: `Nivel ${nivel} no desbloqueado.` });
    }

    // Generar preguntas y evaluar (TODO en servidor)
    const preguntas = generarCuestionarios(junior.edad, materia, nivel);

    let aciertos = 0;
    let totalPlacetasGanadas = 0;
    const resultados = [];

    for (const respuesta of respuestas) {
      const pregunta = preguntas[respuesta.idx];
      if (!pregunta) continue;

      const esCorrecta = respuesta.opcion === pregunta.correcta;
      if (esCorrecta) {
        aciertos++;
        totalPlacetasGanadas += pregunta.placetas_recompensa;
      }
      resultados.push({
        idx: respuesta.idx,
        correcta: esCorrecta,
        respuesta_correcta: pregunta.correcta,
        placetas_ganadas: esCorrecta ? pregunta.placetas_recompensa : 0
      });
    }

    const totalPreguntas = respuestas.length;
    const porcentaje = totalPreguntas > 0 ? Math.round((aciertos / totalPreguntas) * 100) : 0;

    // Actualizar progreso en Supabase
    const progresoActual = await sbGetAcademyProgress(junior.id) || {};
    const completados = progresoActual.completados || {};
    if (!completados[materia]) completados[materia] = {};
    if (!completados[materia][nivel]) completados[materia][nivel] = [];
    completados[materia][nivel].push({
      fecha: new Date().toISOString(),
      aciertos, total: totalPreguntas, porcentaje,
      placetas_ganadas: totalPlacetasGanadas
    });

    const nuevoSaldo = (junior.placetas_saldo || 0) + totalPlacetasGanadas;
    await sbUpdatePlacetaBalance(junior.id, nuevoSaldo);

    await sbUpsertAcademyProgress({
      junior_id: junior.id, completados,
      puntuacion_total: (progresoActual.puntuacion_total || 0) + totalPlacetasGanadas,
      nivel_maximo: junior.nivel_academia || 1,
      actualizado_en: new Date().toISOString()
    });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    if (totalPlacetasGanadas > 0) {
      await sbCreatePlacetaTransaction({
        junior_id: junior.id, tipo: 'ganar',
        concepto: `Nivel ${nivel} - ${materia}`,
        cantidad: totalPlacetasGanadas, saldo_resultante: nuevoSaldo, ip
      });
    }

    await sbCreateJuniorLog({
      junior_id: junior.id, accion: 'cuestionario_completado',
      detalle: `Materia: ${materia}, Nivel: ${nivel}, Aciertos: ${aciertos}/${totalPreguntas} (${porcentaje}%), Placetas: +${totalPlacetasGanadas}`, ip
    });

    if (req.session?.junior) req.session.junior.placetas_saldo = nuevoSaldo;

    res.json({
      success: true, aciertos, total: totalPreguntas, porcentaje,
      placetas_ganadas: totalPlacetasGanadas, saldo_actual: nuevoSaldo,
      resultados, aprobado: porcentaje >= 60
    });
  } catch (err) {
    console.error('[Academy] Error evaluando:', err);
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  DESBLOQUEAR NIVEL — con flujo bancario real + límites tutor
// ═══════════════════════════════════════════════════════════════════════════

router.post('/desbloquear-nivel', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    const nivelActual = junior.nivel_academia || 1;
    const siguienteNivel = nivelActual + 1;

    if (siguienteNivel > 10) {
      return res.status(400).json({ error: '¡Ya has alcanzado el nivel máximo! 🎉' });
    }

    const costo = COSTO_DESBLOQUEO_POR_NIVEL[siguienteNivel];
    if (!costo) return res.status(400).json({ error: 'Nivel no válido' });

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';

    // ── Verificar límites del tutor ────────────────────────────────────
    const limites = await sbGetParentalLimits(junior.id);
    const limiteDiario = limites?.limite_gasto_diario || 10;
    const limiteAprobacionTutor = limites?.limite_aprobacion_tutor || 1000; // Por defecto: 1000 Pz
    const requiereAprobacion = limites?.requiere_aprobacion_extra !== false;

    // Calcular gasto del día
    const { data: transaccionesHoy } = await supabase
      .from('junior_transacciones')
      .select('cantidad')
      .eq('junior_id', junior.id)
      .eq('tipo', 'gastar')
      .gte('creado_en', new Date().toISOString().slice(0, 10));

    const gastoHoy = (transaccionesHoy || []).reduce((s, t) => s + (t.cantidad || 0), 0);

    // ── Verificar límite diario ────────────────────────────────────────
    if (gastoHoy + costo > limiteDiario) {
      return res.status(403).json({
        error: `Límite diario de gasto (${limiteDiario} Pz) excedido. Has gastado ${gastoHoy} Pz hoy.`,
        limite_diario: limiteDiario, gasto_hoy: gastoHoy,
        disponible_hoy: Math.max(0, limiteDiario - gastoHoy),
        necesita_autorizacion_tutor: false
      });
    }

    // ── Verificar autorización del tutor para compras grandes ──────────
    // Si el costo supera el límite de aprobación, necesita firma del tutor vía PlacetaID
    if (costo > limiteAprobacionTutor && requiereAprobacion) {
      // Crear solicitud de autorización pendiente
      await sbCreateJuniorLog({
        junior_id: junior.id,
        accion: 'solicitar_autorizacion_tutor',
        detalle: `Solicita autorización para gastar ${costo} Pz (desbloquear nivel ${siguienteNivel}). Límite aprobación: ${limiteAprobacionTutor} Pz`,
        ip
      });

      return res.status(403).json({
        error: `Esta compra de ${costo} Pz supera el límite de autorización automática de ${limiteAprobacionTutor} Pz. El tutor debe aprobarla desde PlacetaID Móvil.`,
        necesita_autorizacion_tutor: true,
        costo,
        limite_aprobacion: limiteAprobacionTutor,
        mensaje: `Solicitud enviada al tutor. Debe aprobarla desde PlacetaID Móvil.`
      });
    }

    const saldoActual = junior.placetas_saldo || 0;
    if (saldoActual < costo) {
      return res.status(400).json({
        error: `No tienes suficientes placetas. Necesitas ${costo}, tienes ${saldoActual}.`,
        placetas_necesarias: costo, placetas_actuales: saldoActual
      });
    }

    // ── Pago bancario: Junior → Capitalia, Capitalia paga IVA a TGLP ──
    let pagoBancario = null;
    try {
      pagoBancario = await desbloquearNivelBanco({
        juniorAccountId: `u-${junior.dip?.toLowerCase().replace(/-/g, '')}`,
        costoPlacetas: costo,
        juniorDip: junior.dip,
        tutorDip: junior.tutor_dip
      });
    } catch (bankErr) {
      console.warn('[Academy] Pago bancario no disponible:', bankErr.message);
    }

    // ── Descontar placetas internas ────────────────────────────────────
    const nuevoSaldo = saldoActual - costo;
    await sbUpdatePlacetaBalance(junior.id, nuevoSaldo);
    await sbUpdateJunior(junior.id, { nivel_academia: siguienteNivel });
    await sbUpsertAcademyProgress({
      junior_id: junior.id, nivel_maximo: siguienteNivel,
      actualizado_en: new Date().toISOString()
    });

    await sbCreatePlacetaTransaction({
      junior_id: junior.id, tipo: 'gastar',
      concepto: `Desbloquear nivel ${siguienteNivel}`,
      cantidad: costo, saldo_resultante: nuevoSaldo, ip
    });

    await sbCreateJuniorLog({
      junior_id: junior.id, accion: 'nivel_desbloqueado',
      detalle: `Nivel ${siguienteNivel} desbloqueado por ${costo} Pz${pagoBancario ? '. Pago bancario OK + IVA Capitalia→TGLP' : ''}`, ip
    });

    if (req.session?.junior) {
      req.session.junior.placetas_saldo = nuevoSaldo;
      req.session.junior.nivel_academia = siguienteNivel;
    }

    res.json({
      success: true,
      message: `🌟 ¡Nivel ${siguienteNivel} desbloqueado! ${pagoBancario ? 'Pago bancario realizado. Capitalia pagará el IVA a Tributos.' : ''}`,
      nivel_actual: siguienteNivel, nivel_maximo: 10,
      saldo_actual: nuevoSaldo, costo,
      pago_bancario: !!pagoBancario,
      siguiente_nivel: siguienteNivel < 10 ? {
        nivel: siguienteNivel + 1,
        costo: COSTO_DESBLOQUEO_POR_NIVEL[siguienteNivel + 1] || '—'
      } : null
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  HISTORIAL DE TRANSACCIONES
// ═══════════════════════════════════════════════════════════════════════════

router.get('/historial', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });

    const { data, error } = await supabase
      .from('junior_transacciones')
      .select('*')
      .eq('junior_id', junior.id)
      .order('creado_en', { ascending: false })
      .limit(50);

    if (error) return res.json([]);
    res.json(data || []);
  } catch (err) {
    res.json([]);
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  MIDDLEWARE
// ═══════════════════════════════════════════════════════════════════════════

async function verificarJunior(req, res, next) {
  // From session (web) or from query/header (mobile app)
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

// ═══════════════════════════════════════════════════════════════════════════
//  RBU — Renta Básica Universal Junior (5 Pz diarios)
// ═══════════════════════════════════════════════════════════════════════════

const RBU_DIARIO = 5; // 5 Pz cada día
const RBU_FUNDACION = 'FUNDACION_BP';

router.get('/rbu', verificarJunior, async (req, res) => {
  try {
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });
    const esDemo = junior.tutor_dip === '11111111D';

    const hoy = new Date().toISOString().slice(0, 10);
    const { data: yaReclamado } = await supabase.from('junior_transacciones')
      .select('id').eq('junior_id', junior.id).eq('tipo', 'rbu')
      .gte('creado_en', hoy).limit(1);

    if (yaReclamado && yaReclamado.length > 0) {
      return res.json({ success: false, message: 'Ya has reclamado tu RBU hoy. ¡Vuelve mañana! 🌅' });
    }

    // Count streak days this week
    let streak = 1;
    for (let d = 1; d < 7; d++) {
      const fecha = new Date(Date.now() - d * 86400000).toISOString().slice(0, 10);
      const { data: dia } = await supabase.from('junior_transacciones')
        .select('id').eq('junior_id', junior.id).eq('tipo', 'rbu')
        .gte('creado_en', fecha).lt('creado_en', fecha + 'T23:59:59').limit(1);
      if (dia && dia.length > 0) streak++;
      else break;
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const cantidad = RBU_DIARIO;

    if (!esDemo) {
      try {
        await apiBanco('transferir', {
          from: RBU_FUNDACION, to: junior.cuenta_banco || junior.dip,
          cantidad, concepto: `RBU día ${streak} — Placeta Junior`, ip
        });
      } catch (e) { console.warn('[RBU] Banco:', e.message); }
    }

    const nuevoSaldo = (junior.placetas_saldo || 0) + cantidad;
    await sbUpdatePlacetaBalance(junior.id, nuevoSaldo);
    await sbCreatePlacetaTransaction({
      junior_id: junior.id, tipo: 'rbu',
      concepto: `RBU día ${streak}${esDemo ? ' (Demo)' : ''}`,
      cantidad, saldo_resultante: nuevoSaldo, ip
    });
    await sbCreateJuniorLog({
      junior_id: junior.id, accion: 'rbu_reclamado',
      detalle: `RBU +${cantidad} Pz (día ${streak}). Saldo: ${nuevoSaldo} Pz${esDemo ? ' (Demo)' : ''}`, ip
    });

    res.json({
      success: true, cantidad, streak, nuevo_saldo: nuevoSaldo,
      message: `¡RBU reclamada! +${cantidad} Pz. Día ${streak} de racha semanal.`,
      es_demo: esDemo
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
//  CONFIRMAR PAGO UPGRADE — Slideup de confirmación
// ═══════════════════════════════════════════════════════════════════════════

router.post('/confirmar-pago', verificarJunior, async (req, res) => {
  try {
    const { cantidad, concepto, nivel } = req.body;
    const junior = req.juniorData;
    if (!junior) return res.status(404).json({ error: 'Perfil no encontrado' });
    const esDemo = junior.tutor_dip === '11111111D';

    if (!esDemo && (junior.placetas_saldo || 0) < cantidad) {
      return res.status(400).json({ error: 'Saldo insuficiente', saldo_actual: junior.placetas_saldo, costo: cantidad });
    }

    const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
    const nuevoSaldo = (junior.placetas_saldo || 0) - cantidad;

    // Real bank operation → Capitalia
    if (!esDemo) {
      try {
        await apiBanco('transferir', {
          from: junior.cuenta_banco || junior.dip,
          to: 'CAPITALIA_BANK', cantidad,
          concepto: concepto || 'Upgrade academia', ip,
          iva: Math.ceil(cantidad * 12 / 100)
        });
      } catch (e) { console.warn('[Pago] Banco:', e.message); }
    }

    await sbUpdatePlacetaBalance(junior.id, nuevoSaldo);
    await sbCreatePlacetaTransaction({
      junior_id: junior.id, tipo: 'gastar',
      concepto: concepto || `Desbloquear nivel ${nivel || '?'}`,
      cantidad, saldo_resultante: nuevoSaldo, ip
    });
    await sbCreateJuniorLog({
      junior_id: junior.id, accion: 'pago_upgrade',
      detalle: `Pago ${cantidad} Pz → Capitalia. ${concepto || ''}. Saldo: ${nuevoSaldo} Pz${esDemo ? ' (Demo)' : ''}`, ip
    });

    res.json({
      success: true, pagado: cantidad, saldo_anterior: junior.placetas_saldo,
      nuevo_saldo: nuevoSaldo, es_demo: esDemo,
      message: `✅ Pago confirmado: -${cantidad} Pz. Te quedan ${nuevoSaldo} Pz.`
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
