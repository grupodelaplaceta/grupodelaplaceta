/**
 * Adaptador Híbrido: Supabase + SQLite
 * 
 * Proporciona:
 *   - Funciones CRUD para Supabase (tablas CRM públicas)
 *   - Wrapper SQLite para tablas internas (bancaria, fiscal, justicia)
 * 
 * Las rutas admin (identidad, bancario, fiscal, justicia, rgpd, ocio, recursos)
 * siguen usando getDb() de db.js (SQLite puro).
 * Las rutas públicas (auth, tramites, contenidos, fotos, placetaid, firma)
 * usan estas funciones Supabase.
 */

import initSqlJs from 'sql.js';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { supabase } from './supabase.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ── Estado SQLite (fallback para banking/fiscal/justicia) ───────────────────
let rawDb = null;
let sqliteWrapper = null;
let initPromise = null;

async function initSqlite() {
  if (sqliteWrapper) return sqliteWrapper;
  if (initPromise) return initPromise;

  initPromise = (async () => {
    const SQL = await initSqlJs();
    const dbPath = process.env.DB_PATH || path.join(__dirname, '../../data/gdlp-crm.db');
    const dbDir = path.dirname(dbPath);
    if (!fs.existsSync(dbDir)) fs.mkdirSync(dbDir, { recursive: true });

    if (fs.existsSync(dbPath)) {
      const buffer = fs.readFileSync(dbPath);
      rawDb = new SQL.Database(buffer);
    } else {
      rawDb = new SQL.Database();
    }

    rawDb.run('PRAGMA foreign_keys = ON');
    ejecutarMigracionesSqlite(rawDb);
    guardar();
    sqliteWrapper = crearWrapperSqlite(rawDb);
    console.log('  📦 SQLite: Inicializado (fallback para tablas bancarias/fiscal/justicia)');
    return sqliteWrapper;
  })();

  return initPromise;
}

function guardar() {
  if (!rawDb) return;
  const dbPath = process.env.DB_PATH || path.join(__dirname, '../../data/gdlp-crm.db');
  const data = rawDb.export();
  fs.writeFileSync(dbPath, Buffer.from(data));
}

function crearWrapperSqlite(raw) {
  return {
    prepare(sql) {
      let stmt;
      try { stmt = raw.prepare(sql); } catch (e) { stmt = null; }
      return {
        get(...params) {
          if (!stmt) return undefined;
          try {
            const arr = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            if (arr.length > 0) stmt.bind(arr);
            if (stmt.step()) { const row = stmt.getAsObject(); stmt.reset(); return row; }
            stmt.reset(); return undefined;
          } catch (e) { try { stmt.reset(); } catch (_) {} return undefined; }
        },
        all(...params) {
          if (!stmt) return [];
          const rows = [];
          try {
            const arr = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            if (arr.length > 0) stmt.bind(arr);
            while (stmt.step()) rows.push(stmt.getAsObject());
          } catch (e) { /* ignore */ }
          try { stmt.reset(); } catch (_) {}
          return rows;
        },
        run(...params) {
          try {
            const arr = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            raw.run(sql, arr);
            guardar();
          } catch (e) { /* ignore */ }
          return { lastInsertRowid: 0, changes: 1 };
        }
      };
    },
    exec(sql) {
      if (raw) return raw.exec(sql);
      return [];
    }
  };
}

// ── Inicialización principal ────────────────────────────────────────────────
let initialized = false;

export async function initializeDatabase() {
  if (initialized) return;
  initialized = true;
  await initSqlite();
}

export function getDb() {
  if (!sqliteWrapper) throw new Error('BD no inicializada. Llama a initializeDatabase() primero.');
  return sqliteWrapper;
}

// ═══════════════════════════════════════════════════════════════════════════
//  FUNCIONES SUPABASE - Operaciones CRUD públicas
// ═══════════════════════════════════════════════════════════════════════════

function checkSupabase() {
  if (!supabase) throw new Error('Supabase no está configurado');
  return supabase;
}

// ── SOLICITANTES ────────────────────────────────────────────────────────────

export async function sbFindSolicitante(aliasOrDip) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('solicitantes')
    .select('*')
    .or(`alias.eq.${aliasOrDip},dip.eq.${aliasOrDip}`)
    .limit(1)
    .single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbFindSolicitanteByEmail(email) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('solicitantes')
    .select('*').eq('email', email).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbFindSolicitanteByDip(dip) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('solicitantes')
    .select('*').eq('dip', dip).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbFindSolicitanteById(id) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('solicitantes')
    .select('*').eq('id', id).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbCreateSolicitante(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('solicitantes')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert solicitante: ${error.message}`);
  return result;
}

export async function sbUpdateSolicitante(id, data) {
  const sb = checkSupabase();
  const { error } = await sb.from('solicitantes').update(data).eq('id', id);
  if (error) throw new Error(`Supabase update solicitante: ${error.message}`);
  return true;
}

export async function sbListSolicitantes(filters = {}) {
  const sb = checkSupabase();
  let query = sb.from('solicitantes').select('*');
  if (filters.estado) query = query.eq('estado', filters.estado);
  if (filters.rol) query = query.eq('rol', filters.rol);
  query = query.order('creado_en', { ascending: false });
  const { data, error } = await query;
  if (error) throw new Error(`Supabase list solicitantes: ${error.message}`);
  return data || [];
}

// ── SOLICITUDES DIP ─────────────────────────────────────────────────────────

export async function sbFindSolicitudDip(dip) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('solicitudes_dip')
    .select('*').eq('dip', dip).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbCreateSolicitudDip(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('solicitudes_dip')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert solicitud_dip: ${error.message}`);
  return result;
}

export async function sbUpdateSolicitudDip(dip, data) {
  const sb = checkSupabase();
  const { error } = await sb.from('solicitudes_dip').update(data).eq('dip', dip);
  if (error) throw new Error(`Supabase update solicitud_dip: ${error.message}`);
  return true;
}

// ── CONTROL PARENTAL ────────────────────────────────────────────────────────

export async function sbFindControlParentalByCodigo(codigo) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('control_parental')
    .select('*').eq('codigo_vinculacion', codigo).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbFindControlParentalByDni(dni) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('control_parental')
    .select('*').eq('dni_tutor', dni).order('creado_en', { ascending: false });
  if (error) return [];
  return data || [];
}

export async function sbCreateControlParental(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('control_parental')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert control_parental: ${error.message}`);
  return result;
}

export async function sbUpdateControlParentalUsar(codigo) {
  const sb = checkSupabase();
  const { error } = await sb.from('control_parental')
    .update({ usado: 1 }).eq('codigo_vinculacion', codigo);
  if (error) throw new Error(`Supabase update control_parental: ${error.message}`);
  return true;
}

// ── QUEJAS ──────────────────────────────────────────────────────────────────

export async function sbCreateQueja(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('quejas')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert queja: ${error.message}`);
  return result;
}

export async function sbFindQuejaById(id) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('quejas')
    .select('*').eq('id', id).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbListQuejas(limit = 20) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('quejas')
    .select('*').order('creado_en', { ascending: false }).limit(limit);
  if (error) return [];
  return data || [];
}

// ── ENTIDADES ───────────────────────────────────────────────────────────────

export async function sbFindEntidadByEip(eip) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('entidades')
    .select('*').eq('eip', eip).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbCreateEntidad(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('entidades')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert entidad: ${error.message}`);
  return result;
}

// ── DOCUMENTOS TRÁMITES ─────────────────────────────────────────────────────

export async function sbCreateDocumentoTramite(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('documentos_tramites')
    .insert(data).select().single();
  if (error) throw new Error(`Supabase insert documento_tramite: ${error.message}`);
  return result;
}

export async function sbFindDocumentosByUsuario(usuarioId, limit = 20) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('documentos_tramites')
    .select('*').eq('usuario_id', usuarioId)
    .order('creado_en', { ascending: false }).limit(limit);
  if (error) return [];
  return data || [];
}

// ── LOGS AUDITORÍA ──────────────────────────────────────────────────────────

export async function sbCreateLog(data) {
  const sb = checkSupabase();
  const { error } = await sb.from('logs_auditoria').insert(data);
  if (error) console.error('[Supabase] Error insert log:', error.message);
}

export async function sbFindLogsByUsuario(usuarioId, limit = 50) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('logs_auditoria')
    .select('*').eq('usuario_id', usuarioId)
    .order('creado_en', { ascending: false }).limit(limit);
  if (error) return [];
  return data || [];
}

// ── FOTOS LIKES ─────────────────────────────────────────────────────────────

export async function sbToggleLike(fotoUrl, usuarioId) {
  const sb = checkSupabase();
  const { data: existente } = await sb.from('fotos_likes')
    .select('id').eq('foto_url', fotoUrl).eq('usuario_id', usuarioId).limit(1).single();

  if (existente) {
    await sb.from('fotos_likes').delete().eq('id', existente.id);
    const { count } = await sb.from('fotos_likes')
      .select('*', { count: 'exact', head: true }).eq('foto_url', fotoUrl);
    return { liked: false, total: count || 0 };
  }
  
  await sb.from('fotos_likes').insert({ foto_url: fotoUrl, usuario_id: usuarioId });
  const { count } = await sb.from('fotos_likes')
    .select('*', { count: 'exact', head: true }).eq('foto_url', fotoUrl);
  return { liked: true, total: count || 0 };
}

export async function sbGetAllLikes() {
  const sb = checkSupabase();
  const { data } = await sb.from('fotos_likes').select('foto_url');
  const result = {};
  (data || []).forEach(f => { result[f.foto_url] = (result[f.foto_url] || 0) + 1; });
  return result;
}

export async function sbGetFotoLikes(fotoUrl) {
  const sb = checkSupabase();
  const { count } = await sb.from('fotos_likes')
    .select('*', { count: 'exact', head: true }).eq('foto_url', fotoUrl);
  return count || 0;
}

// ── CONTENIDOS ──────────────────────────────────────────────────────────────

export async function sbListContenidos() {
  const sb = checkSupabase();
  const { data, error } = await sb.from('contenidos')
    .select('*').order('creado_en', { ascending: false });
  if (error) return [];
  return data || [];
}

export async function sbGetContenidoBySlug(slug) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('contenidos')
    .select('*').eq('slug', slug).eq('estado', 'publicado').limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbCreateContenido(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('contenidos')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbUpdateContenido(id, data) {
  const sb = checkSupabase();
  data.actualizado_en = new Date().toISOString();
  const { error } = await sb.from('contenidos').update(data).eq('id', id);
  if (error) throw new Error(error.message);
  return true;
}

export async function sbDeleteContenido(id) {
  const sb = checkSupabase();
  const { error } = await sb.from('contenidos').delete().eq('id', id);
  if (error) throw new Error(error.message);
  return true;
}

// ── DOCUMENTOS FIRMADOS ─────────────────────────────────────────────────────

export async function sbCreateDocumentoFirmado(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('documentos_firmados')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbFindDocumentoFirmadoByUrl(url) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('documentos_firmados')
    .select('*').eq('url_firma', url).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbUpdateDocumentoFirmado(id, data) {
  const sb = checkSupabase();
  const { error } = await sb.from('documentos_firmados').update(data).eq('id', id);
  if (error) throw new Error(error.message);
  return true;
}

export async function sbFindDocumentoFirmadoById(id) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('documentos_firmados')
    .select('*,solicitantes!documentos_firmados_firmado_por_fkey(alias,dip)')
    .eq('id', id).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbListDocumentosFirmadosPendientes(usuarioId) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('documentos_firmados')
    .select('*')
    .or(`firmado_por.eq.${usuarioId},firmado_por.is.null`)
    .eq('estado', 'pendiente')
    .order('creado_en', { ascending: false });
  if (error) return [];
  return data || [];
}

export async function sbListDocumentosFirmados(limit = 100) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('documentos_firmados')
    .select('*,solicitantes!documentos_firmados_firmado_por_fkey(alias)')
    .order('creado_en', { ascending: false }).limit(limit);
  if (error) return [];
  return data || [];
}

// ── OAUTH ───────────────────────────────────────────────────────────────────

export async function sbFindOAuthClient(clientId) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('oauth_clients')
    .select('*').eq('client_id', clientId).eq('activo', 1).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbFindOAuthClientSecure(clientId, clientSecret) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('oauth_clients')
    .select('*').eq('client_id', clientId).eq('client_secret', clientSecret).eq('activo', 1).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbCreateOAuthClient(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('oauth_clients')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbListOAuthClients() {
  const sb = checkSupabase();
  const { data, error } = await sb.from('oauth_clients')
    .select('client_id,nombre,descripcion,url_retorno,url_logo,activo,creado_en')
    .order('creado_en', { ascending: false });
  if (error) return [];
  return data || [];
}

export async function sbCreateOAuthCode(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('oauth_codes')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbFindOAuthCode(code, clientId) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('oauth_codes')
    .select('*').eq('code', code).eq('client_id', clientId).eq('usado', 0).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbMarkOAuthCodeUsado(id) {
  const sb = checkSupabase();
  const { error } = await sb.from('oauth_codes').update({ usado: 1 }).eq('id', id);
  if (error) throw new Error(error.message);
  return true;
}

export async function sbCreateOAuthToken(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('oauth_tokens')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

// ── PLACETAID TOKENS ────────────────────────────────────────────────────────

export async function sbCreatePlacetaidToken(data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('placetaid_tokens')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbFindPlacetaidToken(token) {
  const sb = checkSupabase();
  const { data, error } = await sb.from('placetaid_tokens')
    .select('*,solicitantes!placetaid_tokens_usuario_id_fkey(id,alias,dip,placeid,rol,franja_edad)')
    .eq('token', token).eq('activo', 1).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbDeactivatePlacetaidToken(token) {
  const sb = checkSupabase();
  const { error } = await sb.from('placetaid_tokens')
    .update({ activo: 0 }).eq('token', token);
  if (error) throw new Error(error.message);
  return true;
}

// ── Migraciones SQLite (solo tablas NO-Supabase) ──────────────────────────
function ejecutarMigracionesSqlite(database) {
  database.run(`CREATE TABLE IF NOT EXISTS cuentas_bancarias (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, tipo_cuenta TEXT, saldo REAL DEFAULT 0, saldo_maximo REAL DEFAULT 500000, limite_transferencia_diario REAL DEFAULT 50000, bono_bienvenida_activo INTEGER DEFAULT 1, fecha_bono TEXT, estado TEXT DEFAULT 'activa', creado_en TEXT DEFAULT (datetime('now')), actualizado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta_origen_id INTEGER, cuenta_destino_id INTEGER, tipo TEXT, cantidad REAL NOT NULL, concepto TEXT, iva_aplicado REAL DEFAULT 0, retencion_aplicada REAL DEFAULT 0, referencia_externa TEXT, estado TEXT DEFAULT 'completada', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS historial_saldos_diarios (id INTEGER PRIMARY KEY AUTOINCREMENT, cuenta_id INTEGER NOT NULL, fecha TEXT NOT NULL, saldo REAL NOT NULL, UNIQUE(cuenta_id, fecha))`);
  database.run(`CREATE TABLE IF NOT EXISTS impuestos_irm (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, periodo TEXT NOT NULL, patrimonio_medio REAL, indice_acumulacion REAL, tasa_aplicada REAL, importe_irm REAL, saldo_quemado REAL, estado TEXT DEFAULT 'calculado', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS multas_automaticas (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, tipo TEXT, dias_en_negativo INTEGER DEFAULT 0, importe REAL NOT NULL, motivo TEXT, estado TEXT DEFAULT 'pendiente', cuenta_id INTEGER, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS renta_basica_universal (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, semana TEXT NOT NULL, importe REAL DEFAULT 5, reclamado INTEGER DEFAULT 0, pagado INTEGER DEFAULT 0, creado_en TEXT DEFAULT (datetime('now')), UNIQUE(usuario_id, semana))`);
  database.run(`CREATE TABLE IF NOT EXISTS sorteos (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, descripcion TEXT, precio_boleto REAL DEFAULT 10, fecha_sorteo TEXT, estado TEXT DEFAULT 'activo', ganador_id INTEGER, premio REAL, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS boletos_comprados (id INTEGER PRIMARY KEY AUTOINCREMENT, sorteo_id INTEGER NOT NULL, usuario_id INTEGER NOT NULL, numero_boleto INTEGER, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS inversiones (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, fondo TEXT NOT NULL, cantidad_invertida REAL NOT NULL, rendimiento_esperado REAL, estado TEXT DEFAULT 'activa', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS recursos_digitales (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, tipo TEXT, identificador TEXT, estado TEXT DEFAULT 'activo', fecha_asignacion TEXT DEFAULT (datetime('now')), fecha_revocacion TEXT, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS denuncias (id INTEGER PRIMARY KEY AUTOINCREMENT, denunciante_id INTEGER NOT NULL, denunciado_id INTEGER NOT NULL, tipo TEXT, descripcion TEXT NOT NULL, estado TEXT DEFAULT 'pendiente', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS expedientes_disciplinarios (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo_expediente TEXT UNIQUE NOT NULL, infractor_id INTEGER NOT NULL, denuncia_id INTEGER, tipo_infraccion TEXT, descripcion_hechos TEXT, conducta_tipica TEXT, gravedad TEXT, fecha_inicio TEXT DEFAULT (datetime('now')), fecha_notificacion_cargos TEXT, fecha_alegaciones TEXT, fecha_resolucion TEXT, fecha_apelacion TEXT, fecha_limite_apelacion TEXT, estado TEXT DEFAULT 'instruccion', sancion_tipo TEXT, sancion_multa REAL, sancion_suspension_dias INTEGER, sancion_expulsion INTEGER DEFAULT 0, sancion_confiscacion INTEGER DEFAULT 0, resuelto_por INTEGER, hash_firma_documento TEXT, creado_en TEXT DEFAULT (datetime('now')), actualizado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS consentimientos (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, tipo TEXT, aceptado INTEGER DEFAULT 0, fecha_aceptacion TEXT, ip_aceptacion TEXT, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS solicitudes_arco (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, derecho TEXT, estado TEXT DEFAULT 'pendiente', detalle_solicitud TEXT, respuesta TEXT, fecha_respuesta TEXT, hash_anonimizacion TEXT, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS registro_tratamiento_datos (id INTEGER PRIMARY KEY AUTOINCREMENT, responsable TEXT NOT NULL, finalidad TEXT NOT NULL, datos_recogidos TEXT, periodo_conservacion TEXT, base_legal TEXT, transferencias_internacionales INTEGER DEFAULT 0, activo INTEGER DEFAULT 1, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS socios_oficiales (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL UNIQUE, categoria TEXT, numero_socio INTEGER UNIQUE, derecho_voto INTEGER DEFAULT 1, fecha_alta_asociacion TEXT DEFAULT (datetime('now')), activo INTEGER DEFAULT 1)`);
  database.run(`CREATE TABLE IF NOT EXISTS asambleas (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, tipo TEXT, fecha_convocatoria TEXT, fecha_celebracion TEXT, orden_dia TEXT, quorum_primera INTEGER DEFAULT 0, quorum_segunda INTEGER DEFAULT 0, estado TEXT DEFAULT 'convocada', acta_texto TEXT, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS votaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, asamblea_id INTEGER, titulo TEXT NOT NULL, descripcion TEXT, tipo_voto TEXT, votos_a_favor INTEGER DEFAULT 0, votos_en_contra INTEGER DEFAULT 0, abstenciones INTEGER DEFAULT 0, resultado TEXT, fecha_cierre TEXT, creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS config_control_parental (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, activo INTEGER DEFAULT 1, creado_en TEXT DEFAULT (datetime('now')))`);
  
  database.run('CREATE INDEX IF NOT EXISTS idx_cuentas_usuario ON cuentas_bancarias(usuario_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_transacciones_origen ON transacciones(cuenta_origen_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_transacciones_destino ON transacciones(cuenta_destino_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_expedientes_infractor ON expedientes_disciplinarios(infractor_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes_disciplinarios(estado)');
  database.run('CREATE INDEX IF NOT EXISTS idx_saldos_diarios_cuenta ON historial_saldos_diarios(cuenta_id, fecha)');
}
