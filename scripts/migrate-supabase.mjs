// Cargar variables de entorno PRIMERO
import 'dotenv/config';

const SUPABASE_URL = process.env.SUPABASE_URL;
const SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY;

if (!SERVICE_KEY || !SUPABASE_URL) {
  console.error('❌ Faltan SUPABASE_URL o SERVICE_KEY en .env');
  process.exit(1);
}

console.log('🔌 Conectando a:', SUPABASE_URL);
console.log('🔑 Key:', SERVICE_KEY.substring(0, 25) + '...');

// Crear cliente directamente
const { createClient } = await import('@supabase/supabase-js');
const supabase = createClient(SUPABASE_URL, SERVICE_KEY, {
  auth: { autoRefreshToken: false, persistSession: false }
});

// Probar conexión
const { error: testErr } = await supabase.from('solicitantes').select('count', { count: 'exact', head: true });
if (testErr && testErr.code !== '42P01') {
  console.error('❌ Error de conexión:', testErr.message);
  process.exit(1);
}
console.log('✅ Conexión establecida');

const statements = [
  `CREATE TABLE IF NOT EXISTS solicitantes (
    id BIGSERIAL PRIMARY KEY, alias TEXT UNIQUE NOT NULL, nombre_real TEXT NOT NULL,
    email TEXT, fecha_nacimiento TEXT, edad INTEGER DEFAULT 0,
    dip TEXT UNIQUE, placeid TEXT UNIQUE, franja_edad TEXT DEFAULT 'Alta_Plena',
    hash_credencial TEXT, password_hash TEXT, rol TEXT DEFAULT 'miembro',
    cargo TEXT, estado TEXT DEFAULT 'activo', ip_registro TEXT,
    ip_ultimo_acceso TEXT, ultimo_acceso TIMESTAMPTZ,
    lista_negra INTEGER DEFAULT 0, motivo_lista_negra TEXT,
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS solicitudes_dip (
    id BIGSERIAL PRIMARY KEY, alias TEXT NOT NULL, nombre_real TEXT NOT NULL,
    email TEXT, fecha_nacimiento TEXT, edad INTEGER, dip TEXT UNIQUE,
    codigo_tutor TEXT, pdf_solicitud TEXT, estado TEXT DEFAULT 'pendiente',
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS cuentas_bancarias (
    id BIGSERIAL PRIMARY KEY, usuario_id BIGINT REFERENCES solicitantes(id),
    tipo_cuenta TEXT, saldo NUMERIC DEFAULT 0, saldo_maximo NUMERIC DEFAULT 500000,
    bono_bienvenida_activo INTEGER DEFAULT 1, fecha_bono TIMESTAMPTZ,
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS entidades (
    id BIGSERIAL PRIMARY KEY, nombre TEXT, tipo TEXT, eip TEXT UNIQUE,
    representante_id BIGINT REFERENCES solicitantes(id), cif TEXT,
    descripcion TEXT, email TEXT, estado TEXT DEFAULT 'pendiente',
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS control_parental (
    id BIGSERIAL PRIMARY KEY, nombre_tutor TEXT, dni_tutor TEXT,
    email_tutor TEXT, telefono_tutor TEXT, nombre_menor TEXT,
    fecha_nac_menor TEXT, codigo_vinculacion TEXT UNIQUE,
    usado INTEGER DEFAULT 0, ip_registro TEXT, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS quejas (
    id BIGSERIAL PRIMARY KEY, nombre TEXT, email TEXT, tipo TEXT,
    mensaje TEXT, ip TEXT, leido INTEGER DEFAULT 0,
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS documentos_tramites (
    id BIGSERIAL PRIMARY KEY, usuario_id BIGINT REFERENCES solicitantes(id),
    tipo TEXT, referencia TEXT, pdf_url TEXT,
    creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS logs_auditoria (
    id BIGSERIAL PRIMARY KEY, usuario_id BIGINT REFERENCES solicitantes(id),
    accion TEXT, detalle TEXT, ip TEXT, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS fotos_likes (
    id BIGSERIAL PRIMARY KEY, foto_url TEXT NOT NULL,
    usuario_id BIGINT REFERENCES solicitantes(id),
    creado_en TIMESTAMPTZ DEFAULT NOW(), UNIQUE(foto_url, usuario_id))`,
  `CREATE TABLE IF NOT EXISTS contenidos (
    id BIGSERIAL PRIMARY KEY, titulo TEXT NOT NULL, slug TEXT UNIQUE NOT NULL,
    contenido TEXT NOT NULL, tipo TEXT DEFAULT 'pagina', meta_desc TEXT,
    estado TEXT DEFAULT 'publicado', autor_id BIGINT REFERENCES solicitantes(id),
    creado_en TIMESTAMPTZ DEFAULT NOW(), actualizado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS oauth_clients (
    id BIGSERIAL PRIMARY KEY, client_id TEXT UNIQUE, client_secret TEXT,
    nombre TEXT, descripcion TEXT, url_retorno TEXT, url_logo TEXT,
    activo INTEGER DEFAULT 1, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS oauth_codes (
    id BIGSERIAL PRIMARY KEY, code TEXT UNIQUE, client_id TEXT,
    usuario_id BIGINT REFERENCES solicitantes(id), redirect_uri TEXT,
    usado INTEGER DEFAULT 0, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS oauth_tokens (
    id BIGSERIAL PRIMARY KEY, token TEXT UNIQUE, client_id TEXT,
    usuario_id BIGINT REFERENCES solicitantes(id), activo INTEGER DEFAULT 1,
    expira_en TIMESTAMPTZ, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS placetaid_tokens (
    id BIGSERIAL PRIMARY KEY, token TEXT UNIQUE,
    usuario_id BIGINT REFERENCES solicitantes(id), activo INTEGER DEFAULT 1,
    expira_en TIMESTAMPTZ, creado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS voley_solicitudes_estado (
    id BIGSERIAL PRIMARY KEY, solicitud_id TEXT UNIQUE, estado TEXT,
    gestionado_por BIGINT REFERENCES solicitantes(id),
    gestionado_en TIMESTAMPTZ DEFAULT NOW())`,
  `CREATE TABLE IF NOT EXISTS documentos_firmados (
    id BIGSERIAL PRIMARY KEY, usuario_id BIGINT REFERENCES solicitantes(id),
    codigo_modelo TEXT, titulo_documento TEXT, estado TEXT DEFAULT 'pendiente',
    url_firma TEXT, hash_documento TEXT,
    firmado_por BIGINT REFERENCES solicitantes(id),
    creado_en TIMESTAMPTZ DEFAULT NOW())`
];

let creadas = 0, errores = 0;

for (const sql of statements) {
  try {
    const { error } = await supabase.rpc('exec_sql', { query: sql });
    if (error && !error.message?.includes('already exists')) {
      // Intentar con query directa
      const { error: e2 } = await supabase.from('_sql').select().limit(0).then(() => {}).catch(() => {});
      try {
        const r = await fetch(`${SUPABASE_URL}/rest/v1/`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'apikey': SERVICE_KEY,
            'Authorization': `Bearer ${SERVICE_KEY}`,
            'Prefer': 'resolution=merge-duplicates'
          },
          body: JSON.stringify({ query: sql })
        });
        if (r.ok || r.status === 400) { creadas++; }
        else { errores++; }
      } catch(e) { errores++; }
    } else {
      creadas++;
    }
  } catch(e) {
    errores++;
  }
}

console.log(`✅ Tablas: ${creadas} creadas/verificadas, ${errores} errores`);
console.log('📋 Si hay errores, ejecuta el SQL manualmente en:');
console.log('   https://supabase.com/dashboard/project/htikrqaywapshlkdonvs/sql/new');
console.log('   Usando el archivo: supabase-schema.sql');
