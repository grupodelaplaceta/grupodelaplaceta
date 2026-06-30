// Re-export desde db-supabase.js (mantiene compatibilidad con rutas admin)
// Las rutas admin (identidad, bancario, fiscal, etc.) importan { getDb } desde aqui
export { initializeDatabase, getDb } from './db-supabase.js';
