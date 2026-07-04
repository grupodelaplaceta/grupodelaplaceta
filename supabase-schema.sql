-- ============================================================
-- GDLP CRM - Esquema Completo para Supabase PostgreSQL
-- Ejecutar en el SQL Editor de Supabase
-- ============================================================
-- NOTA: Las tablas bancarias (cuentas_bancarias, transacciones)
-- se gestionan desde el backend de banco-app en Vercel.
-- Este esquema solo contiene las tablas del CRM.

-- 1. SOLICITANTES (usuarios)
CREATE TABLE IF NOT EXISTS solicitantes (
  id BIGSERIAL PRIMARY KEY,
  alias TEXT UNIQUE NOT NULL,
  nombre_real TEXT NOT NULL,
  email TEXT,
  fecha_nacimiento TEXT,
  edad INTEGER DEFAULT 0,
  dip TEXT UNIQUE,
  placeid TEXT UNIQUE,
  franja_edad TEXT DEFAULT 'Alta_Plena',
  hash_credencial TEXT,
  password_hash TEXT,
  rol TEXT DEFAULT 'miembro',
  cargo TEXT,
  estado TEXT DEFAULT 'activo',
  ip_registro TEXT,
  ip_ultimo_acceso TEXT,
  ultimo_acceso TIMESTAMPTZ,
  lista_negra INTEGER DEFAULT 0,
  motivo_lista_negra TEXT,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 2. SOLICITUDES DIP
CREATE TABLE IF NOT EXISTS solicitudes_dip (
  id BIGSERIAL PRIMARY KEY,
  alias TEXT NOT NULL,
  nombre_real TEXT NOT NULL,
  email TEXT,
  fecha_nacimiento TEXT,
  edad INTEGER,
  dip TEXT UNIQUE,
  codigo_tutor TEXT,
  pdf_solicitud TEXT,
  estado TEXT DEFAULT 'pendiente',
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 4. ENTIDADES
CREATE TABLE IF NOT EXISTS entidades (
  id BIGSERIAL PRIMARY KEY,
  nombre TEXT,
  tipo TEXT,
  eip TEXT UNIQUE,
  representante_id BIGINT REFERENCES solicitantes(id),
  cif TEXT,
  descripcion TEXT,
  email TEXT,
  estado TEXT DEFAULT 'pendiente',
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 5. CONTROL PARENTAL
CREATE TABLE IF NOT EXISTS control_parental (
  id BIGSERIAL PRIMARY KEY,
  nombre_tutor TEXT,
  dni_tutor TEXT,
  email_tutor TEXT,
  telefono_tutor TEXT,
  nombre_menor TEXT,
  fecha_nac_menor TEXT,
  codigo_vinculacion TEXT UNIQUE,
  usado INTEGER DEFAULT 0,
  ip_registro TEXT,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 6. QUEJAS
CREATE TABLE IF NOT EXISTS quejas (
  id BIGSERIAL PRIMARY KEY,
  nombre TEXT,
  email TEXT,
  tipo TEXT,
  mensaje TEXT,
  ip TEXT,
  leido INTEGER DEFAULT 0,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 7. DOCUMENTOS TRAMITES
CREATE TABLE IF NOT EXISTS documentos_tramites (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES solicitantes(id),
  tipo TEXT,
  referencia TEXT,
  pdf_url TEXT,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 8. LOGS AUDITORIA
CREATE TABLE IF NOT EXISTS logs_auditoria (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES solicitantes(id),
  accion TEXT,
  detalle TEXT,
  ip TEXT,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 9. FOTOS LIKES
CREATE TABLE IF NOT EXISTS fotos_likes (
  id BIGSERIAL PRIMARY KEY,
  foto_url TEXT NOT NULL,
  usuario_id BIGINT REFERENCES solicitantes(id),
  creado_en TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(foto_url, usuario_id)
);

-- 10. TRIBUTOS
CREATE TABLE IF NOT EXISTS tributos_contribuyentes (
  id TEXT PRIMARY KEY,
  placeta_id TEXT UNIQUE NOT NULL,
  dip TEXT UNIQUE,
  nombre TEXT NOT NULL,
  tipo_sujeto TEXT DEFAULT 'Fisico' CHECK (tipo_sujeto IN ('Fisico', 'Empresa')),
  iban TEXT,
  estado_fiscal TEXT DEFAULT 'Al Dia' CHECK (estado_fiscal IN ('Al Dia', 'Moroso', 'En Auditoria')),
  roles_json JSONB DEFAULT '["ciudadano"]',
  fecha_alta_tributos TIMESTAMPTZ, -- Si es NULL, el core BLP deniega transacciones
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tributos_contribuyentes_placeta ON tributos_contribuyentes(placeta_id);
CREATE INDEX IF NOT EXISTS idx_tributos_contribuyentes_dip ON tributos_contribuyentes(dip);

CREATE TABLE IF NOT EXISTS tributos_declaraciones (
  id TEXT PRIMARY KEY,
  contributor_id TEXT REFERENCES tributos_contribuyentes(id),
  placeta_id TEXT,
  mes_periodo TEXT NOT NULL,
  cuenta_id_blp TEXT NOT NULL,
  patrimonio_medio NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  indice_acumulacion NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
  cuota_irm NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  cuota_igf NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  exencion_aplicada BOOLEAN DEFAULT FALSE,
  dias_declarados_banco INTEGER NOT NULL DEFAULT 0,
  dias_reconstruidos_crm INTEGER NOT NULL DEFAULT 0,
  dias_activos_mes INTEGER NOT NULL DEFAULT 30,
  pdf_hash TEXT,
  estado_pago TEXT DEFAULT 'Borrador' CHECK (estado_pago IN ('Borrador', 'Emitido', 'Inhibido', 'Cobrado_Exito', 'Mora_Falta_Saldo', 'Rectificado_Completado')),
  bypass_junta_directiva BOOLEAN DEFAULT FALSE,
  id_permiso_junta TEXT,
  is_rectified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tributos_declaraciones_periodo ON tributos_declaraciones(mes_periodo);
CREATE INDEX IF NOT EXISTS idx_tributos_declaraciones_permiso ON tributos_declaraciones(id_permiso_junta);

-- Función trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_tributos_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tributos_contribuyentes_updated
  BEFORE UPDATE ON tributos_contribuyentes
  FOR EACH ROW EXECUTE FUNCTION update_tributos_modified_column();

CREATE TRIGGER trg_tributos_declaraciones_updated
  BEFORE UPDATE ON tributos_declaraciones
  FOR EACH ROW EXECUTE FUNCTION update_tributos_modified_column();

CREATE TRIGGER trg_tributos_facturas_updated
  BEFORE UPDATE ON tributos_facturas
  FOR EACH ROW EXECUTE FUNCTION update_tributos_modified_column();

CREATE TABLE IF NOT EXISTS tributos_facturas (
  id TEXT PRIMARY KEY,
  numero_factura TEXT UNIQUE NOT NULL,
  emisor_placeta_id TEXT NOT NULL REFERENCES tributos_contribuyentes(placeta_id),
  receptor_placeta_id TEXT NOT NULL REFERENCES tributos_contribuyentes(placeta_id),
  fecha_emision TIMESTAMPTZ DEFAULT NOW(),
  base_imponible NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  total_iva NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  total_factura NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
  transaction_id_blp TEXT UNIQUE,
  csv_verificacion TEXT UNIQUE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tributos_facturas_numero ON tributos_facturas(numero_factura);
CREATE INDEX IF NOT EXISTS idx_tributos_facturas_csv ON tributos_facturas(csv_verificacion);

CREATE TABLE IF NOT EXISTS tributos_lineas_factura (
  id TEXT PRIMARY KEY,
  factura_id TEXT NOT NULL REFERENCES tributos_facturas(id) ON DELETE CASCADE,
  concepto_producto VARCHAR(255) NOT NULL,
  cantidad INTEGER NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(15, 2) NOT NULL CHECK (precio_unitario >= 0),
  iva_porcentaje NUMERIC(5, 2) DEFAULT 12.00,
  subtotal_neto NUMERIC(15, 2) NOT NULL,
  subtotal_iva NUMERIC(15, 2) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tributos_rectificaciones (
  id TEXT PRIMARY KEY,
  declaracion_original_id TEXT NOT NULL REFERENCES tributos_declaraciones(id),
  fecha_rectificacion TIMESTAMPTZ DEFAULT NOW(),
  cuota_provisional_cobrada NUMERIC(15, 2) NOT NULL,
  cuota_real_calculada NUMERIC(15, 2) NOT NULL,
  diferencia_delta NUMERIC(15, 2) NOT NULL,
  estado_ajuste TEXT DEFAULT 'Pendiente_Procesamiento' CHECK (estado_ajuste IN ('Pendiente_Procesamiento', 'Diferencia_Cobrada', 'Diferencia_Devuelta')),
  signature_sha256 TEXT
);

CREATE TABLE IF NOT EXISTS tributos_control_recaudacion (
  id TEXT PRIMARY KEY,
  mes_periodo TEXT UNIQUE NOT NULL,
  estado_inhibicion_global BOOLEAN DEFAULT TRUE,
  fecha_ultima_modificacion TIMESTAMPTZ DEFAULT NOW(),
  operador_responsable_id TEXT NOT NULL,
  total_recaudado NUMERIC(15, 2) DEFAULT 0.00,
  comentario TEXT
);

CREATE TABLE IF NOT EXISTS tributos_audit_logs (
  id BIGSERIAL PRIMARY KEY,
  accion TEXT NOT NULL,
  entidad TEXT NOT NULL,
  entidad_id TEXT,
  usuario_id TEXT,
  detalle JSONB,
  ip TEXT,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tributos_audit_logs_accion ON tributos_audit_logs(accion, creado_en);

-- 11. CONTENIDOS
CREATE TABLE IF NOT EXISTS contenidos (
  id BIGSERIAL PRIMARY KEY,
  titulo TEXT NOT NULL,
  slug TEXT UNIQUE NOT NULL,
  contenido TEXT NOT NULL,
  tipo TEXT DEFAULT 'pagina',
  meta_desc TEXT,
  estado TEXT DEFAULT 'publicado',
  autor_id BIGINT REFERENCES solicitantes(id),
  creado_en TIMESTAMPTZ DEFAULT NOW(),
  actualizado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 11. DOCUMENTOS FIRMADOS
CREATE TABLE IF NOT EXISTS documentos_firmados (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES solicitantes(id),
  codigo_modelo TEXT,
  titulo_documento TEXT,
  estado TEXT DEFAULT 'pendiente',
  url_firma TEXT,
  hash_documento TEXT,
  firmado_por BIGINT REFERENCES solicitantes(id),
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 12. OAUTH
CREATE TABLE IF NOT EXISTS oauth_clients (
  id BIGSERIAL PRIMARY KEY,
  client_id TEXT UNIQUE,
  client_secret TEXT,
  nombre TEXT,
  descripcion TEXT,
  url_retorno TEXT,
  url_logo TEXT,
  activo INTEGER DEFAULT 1,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS oauth_codes (
  id BIGSERIAL PRIMARY KEY,
  code TEXT UNIQUE,
  client_id TEXT,
  usuario_id BIGINT REFERENCES solicitantes(id),
  redirect_uri TEXT,
  usado INTEGER DEFAULT 0,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS oauth_tokens (
  id BIGSERIAL PRIMARY KEY,
  token TEXT UNIQUE,
  client_id TEXT,
  usuario_id BIGINT REFERENCES solicitantes(id),
  activo INTEGER DEFAULT 1,
  expira_en TIMESTAMPTZ,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS placetaid_tokens (
  id BIGSERIAL PRIMARY KEY,
  token TEXT UNIQUE,
  usuario_id BIGINT REFERENCES solicitantes(id),
  activo INTEGER DEFAULT 1,
  expira_en TIMESTAMPTZ,
  creado_en TIMESTAMPTZ DEFAULT NOW()
);

-- 13. Voley Club
CREATE TABLE IF NOT EXISTS voley_solicitudes_estado (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id TEXT UNIQUE,
  estado TEXT,
  gestionado_por BIGINT REFERENCES solicitantes(id),
  gestionado_en TIMESTAMPTZ DEFAULT NOW()
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_solicitantes_alias ON solicitantes(alias);
CREATE INDEX IF NOT EXISTS idx_solicitantes_dip ON solicitantes(dip);
CREATE INDEX IF NOT EXISTS idx_logs_usuario ON logs_auditoria(usuario_id);
CREATE INDEX IF NOT EXISTS idx_logs_accion ON logs_auditoria(accion);
CREATE INDEX IF NOT EXISTS idx_docs_usuario ON documentos_tramites(usuario_id);
CREATE INDEX IF NOT EXISTS idx_fotos_url ON fotos_likes(foto_url);

-- ============================================================
-- Seed: Usuario admin por defecto
-- ============================================================
-- Password: admin123 (bcrypt hash)
INSERT INTO solicitantes (alias, nombre_real, email, dip, placeid, rol, cargo, estado, password_hash, franja_edad)
VALUES ('admin', 'Administrador CRM', 'admin@laplaceta.org', '00000001A', 'PLID-00000001A', 'administrador', 'Administrador del CRM', 'activo', '$2a$10$8KzQMGx5C5Kc5Qy5Q5z5Q.5z5Q5z5Q5z5Q5z5Q5z5Q5z5Q5z5Q5z', 'Alta_Plena')
ON CONFLICT (alias) DO NOTHING;
