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

import crypto from 'crypto';
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
    try {
      // En Vercel serverless, sql.js WASM no está disponible
      if (process.env.VERCEL) {
        throw new Error('SQLite no disponible en Vercel serverless');
      }
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
    } catch (err) {
      console.log('  ⚠️  SQLite: No disponible -', err.message?.substring(0,80));
      sqliteWrapper = crearWrapperStub();
    }
    return sqliteWrapper;
  })();

  return initPromise;
}

function crearWrapperStub() {
  return {
    prepare(sql) {
      return {
        get() { return undefined; },
        all() { return []; },
        run() { return { lastInsertRowid: 0, changes: 0 }; }
      };
    },
    exec() { return []; }
  };
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
  await autoMigrateSupabase();
}

// ── Auto-migración Supabase (crea tablas si no existen) ────────────────────
async function autoMigrateSupabase() {
  const sb = safeSupabase();
  if (!sb) return;
  try {
    const sql = `
    CREATE TABLE IF NOT EXISTS tributos_contribuyentes (
      id TEXT PRIMARY KEY,
      placeta_id TEXT UNIQUE NOT NULL,
      dip TEXT UNIQUE,
      nombre TEXT NOT NULL,
      tipo_sujeto TEXT DEFAULT 'Fisico' CHECK (tipo_sujeto IN ('Fisico', 'Empresa')),
      iban TEXT,
      estado_fiscal TEXT DEFAULT 'Al Dia' CHECK (estado_fiscal IN ('Al Dia', 'Moroso', 'En Auditoria')),
      roles_json JSONB DEFAULT '["ciudadano"]',
      fecha_alta_tributos TIMESTAMPTZ,
      eip TEXT,
      created_at TIMESTAMPTZ DEFAULT NOW(),
      updated_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS tributos_declaraciones (
      id TEXT PRIMARY KEY,
      contributor_id TEXT REFERENCES tributos_contribuyentes(id),
      placeta_id TEXT,
      mes_periodo TEXT NOT NULL,
      cuenta_id_blp TEXT NOT NULL,
      patrimonio_medio NUMERIC(15,2) DEFAULT 0,
      indice_acumulacion NUMERIC(10,4) DEFAULT 0,
      cuota_irm NUMERIC(15,2) DEFAULT 0,
      cuota_igf NUMERIC(15,2) DEFAULT 0,
      exencion_aplicada BOOLEAN DEFAULT FALSE,
      dias_declarados_banco INTEGER DEFAULT 0,
      dias_reconstruidos_crm INTEGER DEFAULT 0,
      dias_activos_mes INTEGER DEFAULT 30,
      estado_pago TEXT DEFAULT 'Borrador',
      is_rectified BOOLEAN DEFAULT FALSE,
      created_at TIMESTAMPTZ DEFAULT NOW(),
      updated_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS tributos_facturas (
      id TEXT PRIMARY KEY,
      numero_factura TEXT UNIQUE NOT NULL,
      emisor_placeta_id TEXT NOT NULL,
      receptor_placeta_id TEXT NOT NULL,
      fecha_emision TIMESTAMPTZ DEFAULT NOW(),
      base_imponible NUMERIC(15,2) DEFAULT 0,
      total_iva NUMERIC(15,2) DEFAULT 0,
      total_factura NUMERIC(15,2) DEFAULT 0,
      created_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS tributos_control_recaudacion (
      id TEXT PRIMARY KEY,
      mes_periodo TEXT UNIQUE NOT NULL,
      estado_inhibicion_global BOOLEAN DEFAULT TRUE,
      operador_responsable_id TEXT NOT NULL DEFAULT 'system',
      created_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS tributos_saldos_diarios (
      id BIGSERIAL PRIMARY KEY,
      placeta_id TEXT NOT NULL,
      mes_periodo TEXT NOT NULL,
      fecha DATE NOT NULL,
      saldo NUMERIC(15,2) DEFAULT 0,
      transactions_count INTEGER DEFAULT 0,
      origen TEXT DEFAULT 'reconstruido',
      created_at TIMESTAMPTZ DEFAULT NOW(),
      UNIQUE(placeta_id, fecha)
    );
    CREATE TABLE IF NOT EXISTS tributos_audit_logs (
      id BIGSERIAL PRIMARY KEY,
      accion TEXT NOT NULL,
      entidad TEXT,
      entidad_id TEXT,
      usuario_id TEXT,
      detalle JSONB,
      ip TEXT,
      creado_en TIMESTAMPTZ DEFAULT NOW()
    );
    `;
    // Ejecutar cada CREATE TABLE por separado via .rpc() o query()
    const statements = sql.split(';').filter(s => s.trim().startsWith('CREATE'));
    for (const stmt of statements) {
      try { await sb.rpc('exec_sql', { sql: stmt + ';' }); } catch (e) {
        // Intentar via REST query directo si rpc falla
        try { await sb.query(stmt + ';'); } catch (e2) {
          console.warn('  ⚠️  No se pudo crear tabla via RPC ni query. Ejecuta el SQL manualmente en Supabase.');
        }
      }
    }
    console.log('  ✅ Supabase: Migración automática completada');
  } catch (err) {
    console.log('  ⚠️  Supabase: No se pudo auto-migrar -', err.message?.substring(0,80));
  }
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

function safeSupabase() {
  try { return checkSupabase(); } catch { return null; }
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

export async function sbListEntidades(estado) {
  const sb = checkSupabase();
  let q = sb.from('entidades').select('*, representante:solicitantes!entidades_representante_id_fkey(alias, dip, email)');
  if (estado) q = q.eq('estado', estado);
  const { data, error } = await q.order('creado_en', { ascending: false }).limit(200);
  if (error) return [];
  return data || [];
}

export async function sbUpdateEntidad(id, data) {
  const sb = checkSupabase();
  const { data: result, error } = await sb.from('entidades').update(data).eq('id', id).select().single();
  if (error) throw new Error(`Supabase update entidad: ${error.message}`);
  return result;
}

export async function sbDeleteEntidad(id) {
  const sb = checkSupabase();
  const { error } = await sb.from('entidades').delete().eq('id', id);
  if (error) throw new Error(`Supabase delete entidad: ${error.message}`);
  return true;
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
// ── TRIBUTOS ─────────────────────────────────────────────────────────────────

export async function sbGetTributosSummary() {
  const sb = safeSupabase();
  if (!sb) return { contribuyentes: 0, declaraciones: 0, facturas: 0, importe_total: 0, iva_total: 0 };
  try {
    const { count: totalContribuyentes, error: contribError } = await sb.from('tributos_contribuyentes')
      .select('id', { count: 'exact', head: true });
    if (contribError) throw contribError;

    const { count: totalDeclaraciones, error: declError } = await sb.from('tributos_declaraciones')
      .select('id', { count: 'exact', head: true });
    if (declError) throw declError;

    const { count: totalFacturas, error: factError } = await sb.from('tributos_facturas')
      .select('id', { count: 'exact', head: true });
    if (factError) throw factError;

    const { data: totalsFacturas, error: totalError } = await sb.from('tributos_facturas')
      .select('total_factura,total_iva');
    if (totalError) throw totalError;

    const totalImporte = (totalsFacturas || []).reduce((sum, row) => sum + Number(row.total_factura || 0), 0);
    const totalIva = (totalsFacturas || []).reduce((sum, row) => sum + Number(row.total_iva || 0), 0);

    return {
      contribuyentes: Number(totalContribuyentes || 0),
      declaraciones: Number(totalDeclaraciones || 0),
      facturas: Number(totalFacturas || 0),
      importe_total: Number(totalImporte.toFixed(2)),
      iva_total: Number(totalIva.toFixed(2))
    };
  } catch (err) {
    console.error('[Tributos] Supabase query error (summary):', err.message || err);
    return { contribuyentes: 0, declaraciones: 0, facturas: 0, importe_total: 0, iva_total: 0 };
  }
}

export async function sbListTributosContributors(limit = 50) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_contribuyentes')
      .select('*')
      .order('fecha_alta_tributos', { ascending: false }).limit(limit);
    if (error) throw error;
    return data || [];
  } catch (err) {
    console.error('[Tributos] Supabase query error (contributors):', err.message || err);
    return [];
  }
}

export async function sbCreateTributosContributor(data) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data: result, error } = await sb.from('tributos_contribuyentes')
    .insert(data).select().single();
  if (error) {
    // Intentar crear la tabla automáticamente si no existe
    if (error.message?.includes('does not exist') || error.code === '42P01') {
      try {
        const { supabase } = await import('./supabase.js');
        const createSQL = `CREATE TABLE IF NOT EXISTS tributos_contribuyentes (
          id TEXT PRIMARY KEY, placeta_id TEXT UNIQUE NOT NULL, dip TEXT UNIQUE,
          nombre TEXT NOT NULL, tipo_sujeto TEXT DEFAULT 'Fisico', iban TEXT,
          estado_fiscal TEXT DEFAULT 'Al Dia', roles_json JSONB DEFAULT '["ciudadano"]',
          fecha_alta_tributos TIMESTAMPTZ, eip TEXT,
          created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
        );`;
        await sb.rpc('exec_sql', { sql: createSQL }).catch(() => {});
        // Reintentar insert después de crear la tabla
        const { data: retry, error: retryErr } = await sb.from('tributos_contribuyentes')
          .insert(data).select().single();
        if (retryErr) throw new Error('Error al insertar después de crear tabla: ' + retryErr.message);
        return retry;
      } catch (createErr) {
        throw new Error('La tabla tributos_contribuyentes no existe y no se pudo crear automáticamente. Ejecuta el script SQL en Supabase.');
      }
    }
    throw new Error(error.message);
  }
  return result;
}

export async function sbListTributosDeclarations(limit = 50) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_declaraciones')
      .select('*')
      .order('mes_periodo', { ascending: false })
      .order('created_at', { ascending: false })
      .limit(limit);
    if (error) throw error;
    return data || [];
  } catch (err) {
    console.error('[Tributos] Supabase query error (declarations):', err.message || err);
    return [];
  }
}

export async function sbListTributosDeclaracionesPorMes(mesPeriodo) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_declaraciones')
      .select('*')
      .eq('mes_periodo', mesPeriodo)
      .order('created_at', { ascending: true });
    if (error) throw error;
    return data || [];
  } catch (err) {
    console.error('[Tributos] Error list por mes:', err.message || err);
    return [];
  }
}

export async function sbListTributosInvoices({ limit = 50 } = {}) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_facturas')
      .select('*')
      .order('fecha_emision', { ascending: false }).limit(limit);
    if (error) throw error;
    return data || [];
  } catch (err) {
    console.error('[Tributos] Supabase query error (invoices):', err.message || err);
    return [];
  }
}

export async function sbCreateTributosInvoice(payload) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const lines = Array.isArray(payload.lineas) ? payload.lineas : [];
  let baseImponible = Number(payload.base_imponible || 0);
  let totalIva = Number(payload.total_iva || 0);
  let totalFactura = Number(payload.total_factura || 0);

  if (lines.length > 0) {
    baseImponible = 0;
    totalIva = 0;
    totalFactura = 0;
    for (const line of lines) {
      const cantidad = Number(line.cantidad || 0);
      const precio = Number(line.precio_unitario || 0);
      const ivaPct = Number(line.iva_porcentaje || 0);
      const subtotalNeto = cantidad * precio;
      const subtotalIva = subtotalNeto * ivaPct / 100;
      baseImponible += subtotalNeto;
      totalIva += subtotalIva;
      totalFactura += subtotalNeto + subtotalIva;
      line.subtotal_neto = Number(subtotalNeto.toFixed(2));
      line.subtotal_iva = Number(subtotalIva.toFixed(2));
    }
    totalIva = Number(totalIva.toFixed(2));
    totalFactura = Number(totalFactura.toFixed(2));
    baseImponible = Number(baseImponible.toFixed(2));
  }

  const invoicePayload = {
    id: payload.id,
    numero_factura: payload.numero_factura,
    emisor_placeta_id: payload.emisor_placeta_id,
    receptor_placeta_id: payload.receptor_placeta_id,
    fecha_emision: payload.fecha_emision || new Date().toISOString(),
    base_imponible: baseImponible,
    total_iva: totalIva,
    total_factura: totalFactura,
    transaction_id_blp: payload.transaction_id_blp || null,
    csv_verificacion: payload.csv_verificacion || crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  };

  const { data: invoice, error: invoiceError } = await sb.from('tributos_facturas')
    .insert(invoicePayload).select().single();
  if (invoiceError) throw new Error(invoiceError.message);

  if (lines.length > 0) {
    const lineItems = lines.map((line) => ({
      id: line.id || `${invoice.id}-${String(Date.now())}`,
      factura_id: invoice.id,
      concepto_producto: line.concepto_producto,
      cantidad: Number(line.cantidad) || 0,
      precio_unitario: Number(line.precio_unitario) || 0,
      iva_porcentaje: Number(line.iva_porcentaje) || 0,
      subtotal_neto: Number(line.subtotal_neto) || 0,
      subtotal_iva: Number(line.subtotal_iva) || 0
    }));
    const { error: linesError } = await sb.from('tributos_lineas_factura').insert(lineItems);
    if (linesError) throw new Error(linesError.message);
  }

  const { data: lineData, error: lineFetchError } = await sb.from('tributos_lineas_factura')
    .select('*').eq('factura_id', invoice.id);
  if (lineFetchError) throw new Error(lineFetchError.message);

  return { ...invoice, lineas: lineData || [] };
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

// ═══════════════════════════════════════════════════════════════════════════
//  TRIBUTOS — Funciones adicionales
// ═══════════════════════════════════════════════════════════════════════════

export async function sbUpdateTributosContributor(placetaId, data) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data: result, error } = await sb.from('tributos_contribuyentes')
    .update(data).eq('placeta_id', placetaId).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbGetTributosContributorByPlacetaId(placetaId) {
  const sb = safeSupabase();
  if (!sb) return null;
  try {
    const { data, error } = await sb.from('tributos_contribuyentes')
      .select('*').eq('placeta_id', placetaId).limit(1).single();
    if (error && error.code !== 'PGRST116') return null;
    return data;
  } catch (err) {
    return null;
  }
}

export async function sbGetTributosContributorByEip(eip) {
  const sb = safeSupabase();
  if (!sb) return null;
  try {
    const cleanEip = String(eip || '').trim().toUpperCase();
    if (!cleanEip) return null;
    const { data, error } = await sb.from('tributos_contribuyentes')
      .select('*').eq('eip', cleanEip).limit(1).single();
    if (error && error.code !== 'PGRST116') return null;
    return data;
  } catch (err) {
    return null;
  }
}

export async function sbCreateTributosDeclaration(data) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data: result, error } = await sb.from('tributos_declaraciones')
    .insert({
      id: data.id,
      contributor_id: data.contributor_id || null,
      placeta_id: data.placeta_id || null,
      mes_periodo: data.mes_periodo,
      cuenta_id_blp: data.cuenta_id_blp || '',
      patrimonio_medio: data.patrimonio_medio || 0,
      indice_acumulacion: data.indice_acumulacion || 0,
      cuota_irm: data.cuota_irm || 0,
      cuota_igf: data.cuota_igf || 0,
      exencion_aplicada: data.exencion_aplicada || false,
      dias_declarados_banco: data.dias_declarados_banco || 0,
      dias_reconstruidos_crm: data.dias_reconstruidos_crm || 0,
      dias_activos_mes: data.dias_activos_mes || 30,
      estado_pago: data.estado_pago || 'Borrador',
      bypass_junta_directiva: data.bypass_junta_directiva || false,
      id_permiso_junta: data.id_permiso_junta || null,
      is_rectified: data.is_rectified || false
    }).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbGetTributosDeclaration(id) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data, error } = await sb.from('tributos_declaraciones').select('*').eq('id', id).limit(1).single();
  if (error && error.code !== 'PGRST116') throw new Error(error.message);
  return data || null;
}

export async function sbUpdateTributosDeclaration(id, data) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data: result, error } = await sb.from('tributos_declaraciones')
    .update({ ...data, updated_at: new Date().toISOString() })
    .eq('id', id)
    .select()
    .single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbDeleteTributosDeclaration(id) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { error } = await sb.from('tributos_declaraciones').delete().eq('id', id);
  if (error) throw new Error(error.message);
  return true;
}

export async function sbGetTributosInhibition() {
  const sb = safeSupabase();
  const now = new Date();
  const mesPeriodo = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  if (!sb) return { mes_periodo: mesPeriodo, estado_inhibicion_global: true };
  try {
    const { data, error } = await sb.from('tributos_control_recaudacion')
      .select('*').eq('mes_periodo', mesPeriodo).limit(1).single();
    if (error && error.code !== 'PGRST116') return { mes_periodo: mesPeriodo, estado_inhibicion_global: true };
    return data || { mes_periodo: mesPeriodo, estado_inhibicion_global: true };
  } catch (err) {
    return { mes_periodo: mesPeriodo, estado_inhibicion_global: true };
  }
}

export async function sbSetTributosInhibition(estado, operadorId) {
  const sb = safeSupabase();
  const now = new Date();
  const mesPeriodo = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  if (!sb) return { mes_periodo: mesPeriodo, estado_inhibicion_global: estado, operador_responsable_id: operadorId };
  try {
    const { data: existing } = await sb.from('tributos_control_recaudacion')
      .select('*').eq('mes_periodo', mesPeriodo).limit(1).single();

    if (existing) {
      const { data, error } = await sb.from('tributos_control_recaudacion')
        .update({
          estado_inhibicion_global: estado,
          fecha_ultima_modificacion: new Date().toISOString(),
          operador_responsable_id: operadorId
        }).eq('mes_periodo', mesPeriodo).select().single();
      if (error) return { mes_periodo: mesPeriodo, estado_inhibicion_global: estado };
      return data;
    }

    const { data, error } = await sb.from('tributos_control_recaudacion')
      .insert({
        id: crypto.randomUUID?.() || String(Date.now()),
        mes_periodo: mesPeriodo,
        estado_inhibicion_global: estado,
        operador_responsable_id: operadorId
      }).select().single();
    if (error) return { mes_periodo: mesPeriodo, estado_inhibicion_global: estado };
    return data;
  } catch (err) {
    return { mes_periodo: mesPeriodo, estado_inhibicion_global: estado };
  }
}

export async function sbListTributosRectifications(limit = 50) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_rectificaciones')
      .select('*')
      .order('fecha_rectificacion', { ascending: false }).limit(limit);
    if (error) return [];
    return data || [];
  } catch (err) {
    return [];
  }
}

export async function sbCreateTributosRectification(data) {
  const sb = safeSupabase();
  if (!sb) throw new Error('Supabase no configurado');
  const { data: result, error } = await sb.from('tributos_rectificaciones')
    .insert(data).select().single();
  if (error) throw new Error(error.message);
  return result;
}

export async function sbListTributosAuditLogs(limit = 50) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_audit_logs')
      .select('*')
      .order('creado_en', { ascending: false }).limit(limit);
    if (error) return [];
    return data || [];
  } catch (err) {
    return [];
  }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TRIBUTOS — Daily Balances & Reconciliation Engine
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Obtiene los saldos diarios de un contribuyente para un mes dado.
 * @param {string} placetaId
 * @param {string} mesPeriodo - "YYYY-MM"
 */
export async function sbGetDailyBalances(placetaId, mesPeriodo) {
  const sb = safeSupabase();
  if (!sb) return [];
  try {
    const { data, error } = await sb.from('tributos_saldos_diarios')
      .select('*')
      .eq('placeta_id', placetaId)
      .eq('mes_periodo', mesPeriodo)
      .order('fecha', { ascending: true });
    if (error) return [];
    return data || [];
  } catch (err) {
    return [];
  }
}

/**
 * Guarda o actualiza un saldo diario para un contribuyente.
 */
export async function sbUpsertDailyBalance(placetaId, mesPeriodo, fecha, saldo, transactionsCount) {
  const sb = safeSupabase();
  if (!sb) return null;
  try {
    const { data: existing } = await sb.from('tributos_saldos_diarios')
      .select('id')
      .eq('placeta_id', placetaId)
      .eq('fecha', fecha)
      .limit(1)
      .single();

    if (existing) {
      const { data, error } = await sb.from('tributos_saldos_diarios')
        .update({
          saldo: Number(saldo),
          transactions_count: Number(transactionsCount || 0),
          updated_at: new Date().toISOString()
        })
        .eq('id', existing.id)
        .select()
        .single();
      if (error) return null;
      return data;
    }

    const { data, error } = await sb.from('tributos_saldos_diarios')
      .insert({
        id: crypto.randomUUID?.() || String(Date.now()),
        placeta_id: placetaId,
        mes_periodo: mesPeriodo,
        fecha,
        saldo: Number(saldo),
        transactions_count: Number(transactionsCount || 0),
        origen: 'banco',
        created_at: new Date().toISOString()
      })
      .select()
      .single();
    if (error) return null;
    return data;
  } catch (err) {
    return null;
  }
}

/**
 * Elimina todos los saldos diarios de un contribuyente para un mes.
 */
export async function sbClearDailyBalances(placetaId, mesPeriodo) {
  const sb = safeSupabase();
  if (!sb) return false;
  try {
    await sb.from('tributos_saldos_diarios')
      .delete()
      .eq('placeta_id', placetaId)
      .eq('mes_periodo', mesPeriodo);
    return true;
  } catch (err) {
    return false;
  }
}

/**
 * Calcula y guarda/actualiza una declaración basada en los saldos diarios.
 * Devuelve la declaración actualizada con los campos calculados.
 */
export async function sbCalculateDeclarationFromDailyBalances(placetaId, mesPeriodo, dailyBalances) {
  const sb = safeSupabase();
  if (!sb) return null;

  const diasConSaldo = dailyBalances.filter(d => d.saldo !== undefined && d.saldo !== null);
  const totalDias = diasConSaldo.length;
  const sumaSaldos = diasConSaldo.reduce((sum, d) => sum + Number(d.saldo || 0), 0);
  const patrimonioMedio = totalDias > 0 ? Number((sumaSaldos / totalDias).toFixed(2)) : 0;
  const diasActivosMes = new Date(parseInt(mesPeriodo.slice(0,4)), parseInt(mesPeriodo.slice(5,7)), 0).getDate();
  const transactionCount = dailyBalances.reduce((sum, d) => sum + Number(d.transactions_count || 0), 0);

  try {
    // Buscar declaración existente
    const { data: existingDeclarations } = await sb.from('tributos_declaraciones')
      .select('*')
      .eq('placeta_id', placetaId)
      .eq('mes_periodo', mesPeriodo)
      .limit(1);

    const declarationData = {
      patrimonio_medio: patrimonioMedio,
      indice_acumulacion: totalDias > 0 ? Number((transactionCount / totalDias).toFixed(4)) : 0,
      dias_declarados_banco: totalDias,
      dias_reconstruidos_crm: Math.max(0, diasActivosMes - totalDias),
      dias_activos_mes: diasActivosMes,
      updated_at: new Date().toISOString()
    };

    // Calcular IRM (Impuesto sobre la Renta de La Placeta)
    // Tasa progresiva sobre el exceso de patrimonio sobre 500.000 Pz
    let cuotaIRM = 0;
    let cuotaIGF = 0;
    const SALDO_MAXIMO = 500000;
    if (patrimonioMedio > SALDO_MAXIMO) {
      const exceso = patrimonioMedio - SALDO_MAXIMO;
      const ratio = exceso / SALDO_MAXIMO;
      let tasa = 0;
      if (ratio <= 0.5) tasa = 0.05;
      else if (ratio <= 1.0) tasa = 0.10;
      else if (ratio <= 2.0) tasa = 0.20;
      else tasa = 0.35;
      cuotaIRM = Math.round(exceso * tasa * 100) / 100;
    }
    // Calcular IGF (Impuesto General sobre el Patrimonio)
    // Tasa fija del 1.5% sobre el patrimonio medio total
    cuotaIGF = Math.round(patrimonioMedio * 0.015 * 100) / 100;
    declarationData.cuota_irm = cuotaIRM;
    declarationData.cuota_igf = cuotaIGF;

    if (existingDeclarations && existingDeclarations.length > 0) {
      const { data, error } = await sb.from('tributos_declaraciones')
        .update(declarationData)
        .eq('id', existingDeclarations[0].id)
        .select()
        .single();
      if (error) return null;
      return { ...data, calculated: true, dailyBalances: dailyBalances };
    }

    // Crear nueva declaración
    const { data, error } = await sb.from('tributos_declaraciones')
      .insert({
        id: crypto.randomUUID?.() || String(Date.now()),
        mes_periodo: mesPeriodo,
        cuenta_id_blp: placetaId,
        patrimonio_medio: patrimonioMedio,
        indice_acumulacion: totalDias > 0 ? Number((transactionCount / totalDias).toFixed(4)) : 0,
        cuota_irm: cuotaIRM,
        cuota_igf: cuotaIGF,
        exencion_aplicada: false,
        dias_declarados_banco: totalDias,
        dias_reconstruidos_crm: Math.max(0, diasActivosMes - totalDias),
        dias_activos_mes: diasActivosMes,
        estado_pago: 'Borrador',
        bypass_junta_directiva: false,
        is_rectified: false
      })
      .select()
      .single();
    if (error) return null;
    return { ...data, calculated: true, dailyBalances: dailyBalances };
  } catch (err) {
    return null;
  }
}

// ── Migraciones SQLite (solo tablas NO-Supabase) ──────────────────────────
function ejecutarMigracionesSqlite(database) {
  database.run(`CREATE TABLE IF NOT EXISTS solicitantes (id INTEGER PRIMARY KEY AUTOINCREMENT, alias TEXT UNIQUE NOT NULL, nombre_real TEXT, email TEXT, fecha_nacimiento TEXT, edad INTEGER, dip TEXT UNIQUE, placeid TEXT, hash_credencial TEXT, password_hash TEXT, rol TEXT DEFAULT 'miembro', cargo TEXT, franja_edad TEXT, estado TEXT DEFAULT 'pendiente', ultimo_acceso TEXT, ip_ultimo_acceso TEXT, creado_en TEXT DEFAULT (datetime('now')), actualizado_en TEXT DEFAULT (datetime('now')))`);
  // Migración: añadir columna telefono si no existe
  try { database.run(`ALTER TABLE solicitantes ADD COLUMN telefono TEXT`); } catch(e) { /* ya existe */ }
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
  database.run(`CREATE TABLE IF NOT EXISTS documentos (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, subtitulo TEXT, contenido TEXT NOT NULL, tipo TEXT DEFAULT 'manual', autor TEXT, version TEXT DEFAULT '1.0', created_at TEXT DEFAULT (datetime('now')), updated_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS notificaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER, titulo TEXT NOT NULL, mensaje TEXT NOT NULL, tipo TEXT DEFAULT 'informativa', leida INTEGER DEFAULT 0, leida_en TEXT, creado_por INTEGER, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS documentos_tramites (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL, tipo TEXT NOT NULL, contenido TEXT, estado TEXT DEFAULT 'pendiente', resuelto_por INTEGER, resuelto_en TEXT, observaciones TEXT, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS logs_auditoria (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER, accion TEXT NOT NULL, detalle TEXT, ip TEXT, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS voley_torneos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, descripcion TEXT, fecha_inicio TEXT, fecha_fin TEXT, estado TEXT DEFAULT 'activo', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS voley_partidos (id INTEGER PRIMARY KEY AUTOINCREMENT, torneo_id INTEGER, equipo_local TEXT NOT NULL, equipo_visitante TEXT NOT NULL, fecha TEXT, resultado_local INTEGER, resultado_visitante INTEGER, estado TEXT DEFAULT 'pendiente', creado_en TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS voley_noticias (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, contenido TEXT, autor TEXT, imagen_url TEXT, destacado INTEGER DEFAULT 0, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS voley_miembros (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, rol TEXT, email TEXT, telefono TEXT, activo INTEGER DEFAULT 1, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS voley_solicitudes (id INTEGER PRIMARY KEY AUTOINCREMENT, solicitante_nombre TEXT NOT NULL, email TEXT, tipo TEXT, mensaje TEXT, estado TEXT DEFAULT 'pendiente', gestionado_por INTEGER, gestionado_en TEXT, created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS entidades (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, tipo TEXT NOT NULL, eip TEXT UNIQUE NOT NULL, representante_dip TEXT, email TEXT, cif TEXT, descripcion TEXT, creado_en TEXT DEFAULT (datetime('now')))`);

  // ── Tributos (SQLite fallback) ──────────────────────────────────────────
  database.run(`CREATE TABLE IF NOT EXISTS tributos_contribuyentes (id TEXT PRIMARY KEY, placeta_id TEXT UNIQUE NOT NULL, dip TEXT UNIQUE, nombre TEXT NOT NULL, tipo_sujeto TEXT DEFAULT 'Fisico', iban TEXT, estado_fiscal TEXT DEFAULT 'Al Dia', roles_json TEXT DEFAULT '["ciudadano"]', fecha_alta_tributos TEXT, eip TEXT, created_at TEXT DEFAULT (datetime('now')), updated_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS tributos_declaraciones (id TEXT PRIMARY KEY, contributor_id TEXT, placeta_id TEXT, mes_periodo TEXT NOT NULL, cuenta_id_blp TEXT NOT NULL DEFAULT '', patrimonio_medio REAL DEFAULT 0, indice_acumulacion REAL DEFAULT 0, cuota_irm REAL DEFAULT 0, cuota_igf REAL DEFAULT 0, estado_pago TEXT DEFAULT 'Borrador', created_at TEXT DEFAULT (datetime('now')))`);
  database.run(`CREATE TABLE IF NOT EXISTS tributos_saldos_diarios (id INTEGER PRIMARY KEY AUTOINCREMENT, placeta_id TEXT NOT NULL, mes_periodo TEXT NOT NULL, fecha TEXT NOT NULL, saldo REAL DEFAULT 0, transactions_count INTEGER DEFAULT 0, origen TEXT DEFAULT 'reconstruido', created_at TEXT DEFAULT (datetime('now')), UNIQUE(placeta_id, fecha))`);
  database.run(`CREATE TABLE IF NOT EXISTS tributos_control_recaudacion (id TEXT PRIMARY KEY, mes_periodo TEXT UNIQUE NOT NULL, estado_inhibicion_global INTEGER DEFAULT 1, created_at TEXT DEFAULT (datetime('now')))`);

  // ── Cuentas bancarias extendidas ────────────────────────────────────────
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN tipo TEXT DEFAULT 'Personal'`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN placeta_id TEXT`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN eip TEXT`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN display_name TEXT`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN iban TEXT`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN compliance_status TEXT DEFAULT 'Clear'`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN tributos_census_date TEXT`); } catch(e) {}
  try { database.run(`ALTER TABLE cuentas_bancarias ADD COLUMN closed_at TEXT`); } catch(e) {}
  database.run(`CREATE TABLE IF NOT EXISTS account_holders (id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER NOT NULL, placeta_id TEXT NOT NULL, dip TEXT, display_name TEXT, role TEXT DEFAULT 'Primary', ownership_percent REAL DEFAULT 100, added_at TEXT DEFAULT (datetime('now')), linked_at TEXT, valid_until TEXT, UNIQUE(account_id, placeta_id))`);
  database.run(`CREATE TABLE IF NOT EXISTS account_managers (id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER NOT NULL, placeta_id TEXT NOT NULL, dip TEXT, display_name TEXT, role TEXT DEFAULT 'Manager', permissions TEXT DEFAULT '["manage","view"]', added_at TEXT DEFAULT (datetime('now')), UNIQUE(account_id, placeta_id))`);
  database.run(`CREATE TABLE IF NOT EXISTS compliance_flags (id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER NOT NULL, tipo TEXT NOT NULL, severity TEXT DEFAULT 'warning', description TEXT, created_at TEXT DEFAULT (datetime('now')), resolved_at TEXT)`);

  database.run('CREATE INDEX IF NOT EXISTS idx_cuentas_usuario ON cuentas_bancarias(usuario_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_cuentas_placeta ON cuentas_bancarias(placeta_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_cuentas_eip ON cuentas_bancarias(eip)');
  database.run('CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario ON notificaciones(usuario_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_tramites_estado ON documentos_tramites(estado)');
  database.run('CREATE INDEX IF NOT EXISTS idx_logs_fecha ON logs_auditoria(created_at)');
  database.run('CREATE INDEX IF NOT EXISTS idx_transacciones_origen ON transacciones(cuenta_origen_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_transacciones_destino ON transacciones(cuenta_destino_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_expedientes_infractor ON expedientes_disciplinarios(infractor_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes_disciplinarios(estado)');
  database.run('CREATE INDEX IF NOT EXISTS idx_saldos_diarios_cuenta ON historial_saldos_diarios(cuenta_id, fecha)');
  database.run('CREATE INDEX IF NOT EXISTS idx_account_holders_account ON account_holders(account_id)');
  database.run('CREATE INDEX IF NOT EXISTS idx_account_managers_account ON account_managers(account_id)');
}

// ═══════════════════════════════════════════════════════════════════════════
//  PLACETA JUNIOR — Funciones específicas
// ═══════════════════════════════════════════════════════════════════════════

export async function sbCreateJunior(data) {
  const { data: result, error } = await supabase.from('junior_menores')
    .insert(data).select().single();
  if (error) throw new Error(`Error al crear registro junior: ${error.message}`);
  return result;
}

export async function sbFindJuniorByDip(dip) {
  const { data, error } = await supabase.from('junior_menores')
    .select('*')
    .eq('dip', dip).limit(1).single();
  if (error && error.code !== 'PGRST116') {
    // Try without FK join which may not exist
    const { data: d2, error: e2 } = await supabase.from('junior_menores')
      .select('*')
      .eq('dip', dip).limit(1).maybeSingle();
    if (e2) return null;
    return d2;
  }
  if (error) return null;
  return data;
}

export async function sbFindJuniorByTutor(tutorDip) {
  const { data, error } = await supabase.from('junior_menores')
    .select('*')
    .eq('tutor_dip', tutorDip)
    .order('creado_en', { ascending: false });
  if (error) return [];
  return data || [];
}

export async function sbListJuniors(filters = {}) {
  let query = supabase.from('junior_menores')
    .select('*');
  if (filters.estado) query = query.eq('estado', filters.estado);
  if (filters.modalidad) query = query.eq('modalidad', filters.modalidad);
  query = query.order('creado_en', { ascending: false });
  const { data, error } = await query;
  if (error) return [];
  return data || [];
}

export async function sbUpdateJunior(id, data) {
  const { error } = await supabase.from('junior_menores').update(data).eq('id', id);
  if (error) throw new Error(`Error al actualizar junior: ${error.message}`);
  return true;
}

// ═══════════════════════════════════════════════════════════════════════════
//  CONTROL PARENTAL JUNIOR — Límites y configuración
// ═══════════════════════════════════════════════════════════════════════════

export async function sbCreateParentalLimit(data) {
  const { data: result, error } = await supabase.from('junior_control_parental')
    .insert(data).select().single();
  if (error) throw new Error(`Error al crear límite parental: ${error.message}`);
  return result;
}

export async function sbGetParentalLimits(juniorId) {
  const { data, error } = await supabase.from('junior_control_parental')
    .select('*').eq('junior_id', juniorId).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbUpdateParentalLimits(juniorId, data) {
  const { error } = await supabase.from('junior_control_parental')
    .update(data).eq('junior_id', juniorId);
  if (error) throw new Error(`Error al actualizar límites parentales: ${error.message}`);
  return true;
}

// ═══════════════════════════════════════════════════════════════════════════
//  ACADEMIA — Progreso y cuestionarios
// ═══════════════════════════════════════════════════════════════════════════

export async function sbGetAcademyProgress(juniorId) {
  const { data, error } = await supabase.from('junior_academia')
    .select('*').eq('junior_id', juniorId).limit(1).single();
  if (error && error.code !== 'PGRST116') return null;
  return data;
}

export async function sbUpsertAcademyProgress(data) {
  const existing = await sbGetAcademyProgress(data.junior_id);
  if (existing) {
    const { error } = await supabase.from('junior_academia')
      .update(data).eq('junior_id', data.junior_id);
    if (error) throw new Error(`Error al actualizar progreso academia: ${error.message}`);
    return true;
  } else {
    const { error } = await supabase.from('junior_academia')
      .insert(data);
    if (error) throw new Error(`Error al crear progreso academia: ${error.message}`);
    return true;
  }
}

// ═══════════════════════════════════════════════════════════════════════════
//  TRANSACCIONES PLACETAS (gastos en academia)
// ═══════════════════════════════════════════════════════════════════════════

export async function sbCreatePlacetaTransaction(data) {
  const { data: result, error } = await supabase.from('junior_transacciones')
    .insert(data).select().single();
  if (error) throw new Error(`Error al crear transacción: ${error.message}`);
  return result;
}

export async function sbGetPlacetaBalance(juniorId) {
  const { data, error } = await supabase.from('junior_menores')
    .select('placetas_saldo').eq('id', juniorId).limit(1).single();
  if (error) return 0;
  return data?.placetas_saldo || 0;
}

export async function sbUpdatePlacetaBalance(juniorId, newSaldo) {
  const { error } = await supabase.from('junior_menores')
    .update({ placetas_saldo: newSaldo }).eq('id', juniorId);
  if (error) throw new Error(`Error al actualizar saldo: ${error.message}`);
  return true;
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOGS JUNIOR
// ═══════════════════════════════════════════════════════════════════════════

export async function sbCreateJuniorLog(data) {
  const { error } = await supabase.from('junior_logs').insert(data);
  if (error) console.error('[Placeta Junior] Error insert junior log:', error.message);
}
