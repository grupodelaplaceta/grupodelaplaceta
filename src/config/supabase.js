import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.SUPABASE_URL || 'https://htikrqaywapshlkdonvs.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_PUBLISHABLE_KEY || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY || '';

// Validar que las claves no tengan caracteres inválidos (como los bullets ••••)
function isValidKey(key) {
  if (!key) return false;
  if (key.includes('•') || key.includes('…') || key.includes('···')) return false;
  return true;
}

const activeKey = isValidKey(supabaseServiceKey) ? supabaseServiceKey : 
                  isValidKey(supabaseAnonKey) ? supabaseAnonKey : null;

if (!activeKey) {
  console.warn('⚠️  Supabase: No hay clave API válida configurada. La funcionalidad Supabase estará desactivada.');
}

export const supabase = activeKey ? createClient(supabaseUrl, activeKey, {
  auth: { autoRefreshToken: false, persistSession: false }
}) : null;

export async function testConnection() {
  if (!supabase) {
    console.log('⚠️  Supabase: No configurado (clave API no disponible)');
    return false;
  }
  try {
    const { data, error } = await supabase.from('solicitantes').select('count').limit(1);
    if (error && error.code === '42P01') {
      console.log('📦 Supabase conectado. Tablas pendientes de migrar.');
      return true;
    }
    if (error) throw error;
    console.log('✅ Supabase conectado.');
    return true;
  } catch (err) {
    console.log('⚠️  Supabase: No disponible (' + err.message.substring(0, 60) + ')');
    return false;
  }
}
