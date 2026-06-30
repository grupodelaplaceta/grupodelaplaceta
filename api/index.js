// Entry point for Vercel Serverless Functions
import 'dotenv/config';

// Import Express app (server.js detects VERCEL env and skips app.listen())
import app from '../server.js';

let initialized = false;

async function ensureDb() {
  if (initialized) return;
  const { initializeDatabase } = await import('../src/config/db-supabase.js');
  try {
    await initializeDatabase();
    initialized = true;
  } catch (err) {
    console.error('DB init error:', err);
  }
}

export default async function handler(req, res) {
  await ensureDb();
  return app(req, res);
}
