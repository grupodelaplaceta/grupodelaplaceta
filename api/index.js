// Entry point for Vercel Serverless Functions
import 'dotenv/config';

// Import Express app (server.js detects VERCEL env and skips app.listen())
import app from '../server.js';
import bcrypt from 'bcryptjs';

let initialized = false;

async function ensureDb() {
  if (initialized) return;
  const { initializeDatabase } = await import('../src/config/db-supabase.js');
  try {
    await initializeDatabase();
  } catch (err) {
    console.error('DB init error:', err);
  }

  // Seed demo users in Supabase (necesario en serverless, donde SQLite no persiste)
  try {
    const { sbFindSolicitante, sbCreateSolicitante } = await import('../src/config/db-supabase.js');
    const { supabase } = await import('../src/config/supabase.js');

    if (supabase) {
      // Verificar si el demo user ya existe en Supabase
      const demoUser = await sbFindSolicitante('11111111D').catch(() => null);
      if (!demoUser) {
        console.log('  🌱 Creando usuario demo en Supabase...');
        const demoHash = await bcrypt.hash('Demo1234!', 10);
        await sbCreateSolicitante({
          alias: '11111111D',
          nombre_real: 'Ciudadano Demo',
          email: 'demo@laplaceta.org',
          fecha_nacimiento: '1990-01-01',
          edad: 36,
          dip: '11111111D',
          placeid: 'PLID-11111111D',
          franja_edad: 'Alta_Plena',
          password_hash: demoHash,
          rol: 'administrador',
          cargo: 'Administrador del CRM',
          estado: 'activo'
        });
        console.log('  ✅ Usuario demo creado en Supabase (11111111D / Demo1234!)');
      }

      // Verificar si el admin existe en Supabase
      const adminUser = await sbFindSolicitante('admin').catch(() => null);
      if (!adminUser) {
        console.log('  🌱 Creando usuario admin en Supabase...');
        const adminHash = await bcrypt.hash('admin123', 10);
        await sbCreateSolicitante({
          alias: 'admin',
          nombre_real: 'Administrador Sistema',
          email: 'admin@grupodelaplaceta.org',
          fecha_nacimiento: '1990-01-01',
          edad: 36,
          dip: '00000001A',
          placeid: 'PLID-00000001A',
          franja_edad: 'Alta_Plena',
          password_hash: adminHash,
          rol: 'administrador',
          cargo: 'Administrador del CRM',
          estado: 'activo'
        });
        console.log('  ✅ Usuario admin creado en Supabase (admin / admin123)');
      }
    }
  } catch (err) {
    console.error('  ⚠️  Seed Supabase users error:', err.message);
  }

  initialized = true;
}

export default async function handler(req, res) {
  await ensureDb();
  return app(req, res);
}
