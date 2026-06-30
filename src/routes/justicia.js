import { Router } from 'express';
import { getDb } from '../config/db.js';
import { verificarSesion, verificarRol } from '../middleware/auth.js';
import crypto from 'crypto';

const router = Router();

// ── MÓDULO 6: JUSTICIA, EXPEDIENTES DISCIPLINARIOS Y SANCIONES ────────────

// POST /api/justicia/denunciar - Buzón de Denuncias
router.post('/denunciar', verificarSesion, (req, res) => {
  const db = getDb();
  const { denunciado_alias, tipo, descripcion } = req.body;
  const denuncianteId = req.session.usuario.id;

  if (!denunciado_alias || !tipo || !descripcion) {
    return res.status(400).json({ error: 'Denunciado, tipo y descripción requeridos' });
  }

  const tiposValidos = ['difamacion', 'acoso', 'injurias', 'fraude', 'suplantacion', 'otra'];
  if (!tiposValidos.includes(tipo)) {
    return res.status(400).json({ error: `Tipo de denuncia inválido. Válidos: ${tiposValidos.join(', ')}` });
  }

  const denunciado = db.prepare('SELECT id, alias, dip FROM solicitantes WHERE alias = ?').get(denunciado_alias);
  if (!denunciado) return res.status(404).json({ error: 'Usuario denunciado no encontrado' });
  if (denunciado.id === denuncianteId) return res.status(400).json({ error: 'No puedes denunciarte a ti mismo' });

  // Verificar que no haya una denuncia activa similar
  const existente = db.prepare(`
    SELECT id FROM denuncias 
    WHERE denunciante_id = ? AND denunciado_id = ? AND estado NOT IN ('archivada')
    AND creado_en > datetime('now', '-30 days')
  `).get(denuncianteId, denunciado.id);

  if (existente) {
    return res.status(400).json({ error: 'Ya existe una denuncia activa contra este usuario. Espera a que se resuelva.' });
  }

  db.prepare('INSERT INTO denuncias (denunciante_id, denunciado_id, tipo, descripcion) VALUES (?, ?, ?, ?)')
    .run(denuncianteId, denunciado.id, tipo, descripcion);

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(denuncianteId, 'denuncia', `Denuncia por ${tipo} contra ${denunciado.alias}`, ip);

  res.json({
    success: true,
    message: 'Denuncia registrada. El Departamento de Justicia la revisará en un plazo máximo de 7 días hábiles.',
    denuncia: { tipo, contra: denunciado.alias }
  });
});

// POST /api/justicia/iniciar-expediente/:denuncia_id - [Iniciar Expediente Disciplinario]
router.post('/iniciar-expediente/:denuncia_id', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  const db = getDb();
  const denuncia = db.prepare("SELECT * FROM denuncias WHERE id = ? AND estado = 'pendiente'").get(req.params.denuncia_id);
  if (!denuncia) return res.status(404).json({ error: 'Denuncia no encontrada o ya procesada' });

  const { tipo_infraccion, descripcion_hechos } = req.body;
  if (!tipo_infraccion || !['Leve', 'Grave', 'Muy_Grave'].includes(tipo_infraccion)) {
    return res.status(400).json({ error: 'Tipo de infracción requerido (Leve, Grave, Muy_Grave)' });
  }

  // Generar código de expediente único
  const codigo = `EXP-${new Date().getFullYear()}-${String(db.prepare('SELECT COUNT(*) as total FROM expedientes_disciplinarios').get().total + 1).padStart(4, '0')}`;

  // Actualizar denuncia
  db.prepare("UPDATE denuncias SET estado = 'derivada' WHERE id = ?").run(denuncia.id);

  // Crear expediente
  const result = db.prepare(`
    INSERT INTO expedientes_disciplinarios (codigo_expediente, infractor_id, denuncia_id, tipo_infraccion, descripcion_hechos, gravedad, estado, fecha_inicio)
    VALUES (?, ?, ?, ?, ?, ?, 'instruccion', datetime('now'))
  `).run(codigo, denuncia.denunciado_id, denuncia.id, tipo_infraccion, descripcion_hechos || denuncia.descripcion, tipo_infraccion);

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'iniciar_expediente', `Expediente ${codigo} iniciado. Infracción: ${tipo_infraccion}`, ip);

  res.json({
    success: true,
    message: `Expediente ${codigo} iniciado. Se debe notificar pliego de cargos.`,
    expediente: { codigo, id: result.lastInsertRowid, tipo_infraccion, estado: 'instruccion' }
  });
});

// POST /api/justicia/notificar-cargos/:expediente_id - [Notificar Pliego de Cargos]
router.post('/notificar-cargos/:expediente_id', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  const db = getDb();
  const expediente = db.prepare("SELECT * FROM expedientes_disciplinarios WHERE id = ? AND estado = 'instruccion'").get(req.params.expediente_id);
  if (!expediente) return res.status(404).json({ error: 'Expediente no encontrado o no está en fase de instrucción' });

  // Actualizar: pasar a fase de alegaciones (5 días hábiles)
  db.prepare(`
    UPDATE expedientes_disciplinarios 
    SET estado = 'alegaciones', fecha_notificacion_cargos = datetime('now'), fecha_limite_apelacion = datetime('now', '+5 days')
    WHERE id = ?
  `).run(expediente.id);

  // Generar documento GDLP-JUS-001
  const contenidoHash = crypto.createHash('sha256').update(`${expediente.codigo_expediente}-cargos-${Date.now()}`).digest('hex');
  db.prepare(`
    INSERT INTO documentos_firmados (codigo_modelo, titulo_documento, contenido_hash, estado, url_firma)
    VALUES (?, ?, ?, ?, ?)
  `).run(
    'GDLP-JUS-001',
    `Acuerdo de Inicio de Expediente Disciplinario y Pliego de Cargos - ${expediente.codigo_expediente}`,
    contenidoHash,
    'pendiente',
    crypto.randomBytes(16).toString('hex')
  );

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'notificar_cargos', `Pliego de cargos notificado para ${expediente.codigo_expediente}`, ip);

  res.json({
    success: true,
    message: `Pliego de cargos notificado. El infractor dispone de 5 días hábiles para presentar alegaciones.`,
    expediente: expediente.codigo_expediente,
    fecha_limite_alegaciones: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString()
  });
});

// POST /api/justicia/alegar/:expediente_id - [Adjuntar Alegaciones] (El acusado)
router.post('/alegar/:expediente_id', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;
  const expediente = db.prepare("SELECT * FROM expedientes_disciplinarios WHERE id = ? AND estado = 'alegaciones'").get(req.params.expediente_id);
  if (!expediente) return res.status(404).json({ error: 'Expediente no encontrado o no está en fase de alegaciones' });
  if (expediente.infractor_id !== usuarioId) return res.status(403).json({ error: 'Solo el infractor puede presentar alegaciones' });

  const { alegaciones } = req.body;
  if (!alegaciones) return res.status(400).json({ error: 'Texto de alegaciones requerido' });

  db.prepare('UPDATE expedientes_disciplinarios SET fecha_alegaciones = datetime(\'now\'), descripcion_hechos = descripcion_hechos || ? WHERE id = ?')
    .run(`\n\nALEGACIONES DEL INFRACTOR:\n${alegaciones}`, expediente.id);

  res.json({ success: true, message: 'Alegaciones registradas correctamente.' });
});

// POST /api/justicia/resolver/:expediente_id - [Dictaminar Resolución] (Juez, 10 días hábiles)
router.post('/resolver/:expediente_id', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  const db = getDb();
  const expediente = db.prepare("SELECT * FROM expedientes_disciplinarios WHERE id = ? AND estado = 'alegaciones'").get(req.params.expediente_id);
  if (!expediente) return res.status(404).json({ error: 'Expediente no encontrado o no está listo para resolución' });

  const { sancion_tipo, cuantia_multa, suspension_dias, hechos_probados, conducta_tipica } = req.body;
  if (!sancion_tipo) return res.status(400).json({ error: 'Tipo de sanción requerido' });

  // Determinar gravedad y efectos
  let gravedad = expediente.gravedad;
  let sancionExpulsion = 0;
  let sancionConfiscacion = 0;

  if (sancion_tipo === 'expulsion') {
    gravedad = 'Muy_Grave';
    sancionExpulsion = 1;
    sancionConfiscacion = 1;
  }

  // Pasar a fase de apelación (5 días hábiles)
  const fechaLimiteApelacion = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString();

  db.prepare(`
    UPDATE expedientes_disciplinarios 
    SET estado = 'apelacion', fecha_resolucion = datetime('now'), 
        sancion_tipo = ?, sancion_multa = ?, sancion_suspension_dias = ?,
        sancion_expulsion = ?, sancion_confiscacion = ?,
        resuelto_por = ?, gravedad = ?,
        fecha_limite_apelacion = ?,
        descripcion_hechos = descripcion_hechos || ?
    WHERE id = ?
  `).run(
    sancion_tipo, cuantia_multa || 0, suspension_dias || 0,
    sancionExpulsion, sancionConfiscacion,
    req.session.usuario.id, gravedad,
    fechaLimiteApelacion,
    `\n\nHECHOS PROBADOS:\n${hechos_probados || ''}\n\nCONDUCTA TÍPICA:\n${conducta_tipica || ''}`,
    expediente.id
  );

  // Generar documento GDLP-JUS-003
  const contenidoHash = crypto.createHash('sha256').update(`${expediente.codigo_expediente}-resolucion-${Date.now()}`).digest('hex');
  db.prepare(`
    INSERT INTO documentos_firmados (codigo_modelo, titulo_documento, contenido_hash, estado, url_firma)
    VALUES (?, ?, ?, ?, ?)
  `).run(
    'GDLP-JUS-003',
    `Resolución Sancionadora en Primera Instancia - ${expediente.codigo_expediente}`,
    contenidoHash,
    'pendiente',
    crypto.randomBytes(16).toString('hex')
  );

  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket.remoteAddress || 'unknown';
  db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
    .run(req.session.usuario.id, 'resolver_expediente', `Resolución emitida para ${expediente.codigo_expediente}. Sanción: ${sancion_tipo}`, ip);

  // Si la sanción es de suspensión, aplicar inmediatamente
  if (sancion_tipo === 'suspension' && suspension_dias > 0) {
    db.prepare('UPDATE solicitantes SET estado = ? WHERE id = ?').run('suspendido', expediente.infractor_id);
  }

  res.json({
    success: true,
    message: `Resolución dictaminada para ${expediente.codigo_expediente}. El infractor tiene 5 días hábiles para apelar.`,
    expediente: expediente.codigo_expediente,
    sancion: { tipo: sancion_tipo, multa: cuantia_multa, suspension: suspension_dias },
    fecha_limite_apelacion: fechaLimiteApelacion
  });
});

// POST /api/justicia/apelar/:expediente_id - [Interponer Recurso de Apelación]
router.post('/apelar/:expediente_id', verificarSesion, (req, res) => {
  const db = getDb();
  const usuarioId = req.session.usuario.id;
  const expediente = db.prepare("SELECT * FROM expedientes_disciplinarios WHERE id = ? AND estado = 'apelacion'").get(req.params.expediente_id);
  if (!expediente) return res.status(404).json({ error: 'Expediente no encontrado o no está en fase de apelación' });
  if (expediente.infractor_id !== usuarioId) return res.status(403).json({ error: 'Solo el infractor puede apelar' });

  // Verificar plazo
  if (new Date() > new Date(expediente.fecha_limite_apelacion)) {
    // Plazo vencido, la sanción queda firme
    return ejecutarSentenciaFirme(db, expediente, req);
  }

  const { motivo_apelacion } = req.body;
  db.prepare('UPDATE expedientes_disciplinarios SET fecha_apelacion = datetime(\'now\'), descripcion_hechos = descripcion_hechos || ? WHERE id = ?')
    .run(`\n\nRECURSO DE APELACIÓN:\n${motivo_apelacion || 'Sin motivo expresado'}`, expediente.id);

  res.json({ success: true, message: 'Recurso de apelación interpuesto. La Junta Directiva revisará el caso.' });
});

// POST /api/justicia/ejecutar-sentencia/:expediente_id - Ejecutar sentencia firme
router.post('/ejecutar-sentencia/:expediente_id', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  const db = getDb();
  const result = ejecutarSentenciaFirme(db, db.prepare('SELECT * FROM expedientes_disciplinarios WHERE id = ?').get(req.params.expediente_id), req);
  res.json(result);
});

// GET /api/justicia/expedientes - Listar expedientes
router.get('/expedientes', verificarSesion, (req, res) => {
  const db = getDb();
  const { estado } = req.query;
  let query = `
    SELECT e.*, s.alias as infractor_alias, s.dip as infractor_dip, j.alias as juez_alias
    FROM expedientes_disciplinarios e
    JOIN solicitantes s ON s.id = e.infractor_id
    LEFT JOIN solicitantes j ON j.id = e.resuelto_por
    WHERE 1=1
  `;
  const params = [];
  if (estado) { query += ' AND e.estado = ?'; params.push(estado); }
  query += ' ORDER BY e.creado_en DESC';

  res.json(db.prepare(query).all(...params));
});

// GET /api/justicia/expedientes/:id - Detalle de expediente
router.get('/expedientes/:id', verificarSesion, (req, res) => {
  const db = getDb();
  const expediente = db.prepare(`
    SELECT e.*, s.alias as infractor_alias, s.dip as infractor_dip, s.franja_edad,
           j.alias as juez_alias, d.tipo as denuncia_tipo, d.descripcion as denuncia_descripcion,
           d.denunciante_id
    FROM expedientes_disciplinarios e
    JOIN solicitantes s ON s.id = e.infractor_id
    LEFT JOIN solicitantes j ON j.id = e.resuelto_por
    LEFT JOIN denuncias d ON d.id = e.denuncia_id
    WHERE e.id = ?
  `).get(req.params.id);
  if (!expediente) return res.status(404).json({ error: 'Expediente no encontrado' });
  res.json(expediente);
});

// GET /api/justicia/denuncias - Listar denuncias
router.get('/denuncias', verificarSesion, verificarRol('administrador', 'junta', 'juez'), (req, res) => {
  const db = getDb();
  const denuncias = db.prepare(`
    SELECT d.*, denun.alias as denunciante_alias, denun.dip as denunciante_dip,
           denun2.alias as denunciado_alias, denun2.dip as denunciado_dip
    FROM denuncias d
    JOIN solicitantes denun ON denun.id = d.denunciante_id
    JOIN solicitantes denun2 ON denun2.id = d.denunciado_id
    ORDER BY d.creado_en DESC
  `).all();
  res.json(denuncias);
});

// ── Helper: Ejecutar sentencia firme ─────────────────────────────────────────
function ejecutarSentenciaFirme(db, expediente, req) {
  if (!expediente) return { error: 'Expediente no encontrado' };

  const ip = req?.headers?.['x-forwarded-for']?.split(',')[0]?.trim() || req?.socket?.remoteAddress || 'unknown';

  const ejecutar = db.transaction(() => {
    // Marcar expediente como firme
    db.prepare("UPDATE expedientes_disciplinarios SET estado = 'firme' WHERE id = ?").run(expediente.id);

    // Aplicar sanciones
    if (expediente.sancion_multa > 0) {
      const cuenta = db.prepare('SELECT id, saldo FROM cuentas_bancarias WHERE usuario_id = ?').get(expediente.infractor_id);
      if (cuenta) {
        const multaFinal = Math.min(expediente.sancion_multa, cuenta.saldo);
        db.prepare('UPDATE cuentas_bancarias SET saldo = saldo - ? WHERE id = ?').run(multaFinal, cuenta.id);
        db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
          .run(cuenta.id, 'multa', multaFinal, `Multa firme - ${expediente.codigo_expediente}`);
      }
    }

    // Expulsión definitiva
    if (expediente.sancion_expulsion) {
      db.prepare("UPDATE solicitantes SET estado = 'expulsado', lista_negra = 1, motivo_lista_negra = ? WHERE id = ?")
        .run(`Expulsión definitiva - ${expediente.codigo_expediente}`, expediente.infractor_id);

      // Confiscar fondos
      if (expediente.sancion_confiscacion) {
        const cuenta = db.prepare('SELECT id, saldo FROM cuentas_bancarias WHERE usuario_id = ?').get(expediente.infractor_id);
        if (cuenta && cuenta.saldo > 0) {
          db.prepare('INSERT INTO transacciones (cuenta_origen_id, tipo, cantidad, concepto) VALUES (?, ?, ?, ?)')
            .run(cuenta.id, 'quema', cuenta.saldo, `Confiscación por expulsión - ${expediente.codigo_expediente}`);
          db.prepare('UPDATE cuentas_bancarias SET saldo = 0, estado = ? WHERE id = ?').run('expulsada', cuenta.id);
        }
      }

      // Generar GDLP-JUS-004
      const hashQuema = crypto.createHash('sha256').update(`QUEMA-${expediente.codigo_expediente}-${Date.now()}`).digest('hex').substring(0, 16);
      const contenidoHash = crypto.createHash('sha256').update(`${expediente.codigo_expediente}-expulsion-${Date.now()}`).digest('hex');
      db.prepare(`
        INSERT INTO documentos_firmados (codigo_modelo, titulo_documento, contenido_hash, estado, url_firma)
        VALUES (?, ?, ?, ?, ?)
      `).run(
        'GDLP-JUS-004',
        `Decreto de Expulsión Definitiva y Confiscación Monetaria - ${expediente.codigo_expediente}`,
        contenidoHash,
        'pendiente',
        crypto.randomBytes(16).toString('hex')
      );
    }

    // Suspensión: restaurar estado si aplica
    if (expediente.sancion_suspension_dias > 0 && !expediente.sancion_expulsion) {
      db.prepare("UPDATE solicitantes SET estado = 'activo' WHERE id = ? AND estado = 'suspendido'").run(expediente.infractor_id);
    }

    db.prepare('INSERT INTO logs_auditoria (usuario_id, accion, detalle, ip) VALUES (?, ?, ?, ?)')
      .run(req?.session?.usuario?.id || null, 'sentencia_firme', `Sentencia firme: ${expediente.codigo_expediente}`, ip);
  });

  ejecutar();

  return {
    success: true,
    message: `Sentencia firme ejecutada para ${expediente.codigo_expediente}`,
    sanciones_aplicadas: {
      multa: expediente.sancion_multa,
      suspension: expediente.sancion_suspension_dias,
      expulsion: !!expediente.sancion_expulsion,
      confiscacion: !!expediente.sancion_confiscacion
    }
  };
}

export default router;
