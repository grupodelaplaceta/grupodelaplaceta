import { MongoClient } from 'mongodb';

const MONGO_URI = process.env.PLACETAID_MONGO_URI || process.env.MONGO_URI || 'mongodb+srv://malegre_db_user:gKHctbCg9KcYUrO8@cluster0.m5bntoj.mongodb.net/';
const DB_NAME = process.env.PLACETAID_MONGO_DB || 'plid26';
const API_KEY = process.env.PLACETAID_API_KEY || 'ccb611655030bdadf7218418dc195dcb';

let client = null;
let db = null;

async function getDb() {
  if (db) return db;
  client = new MongoClient(MONGO_URI);
  await client.connect();
  db = client.db(DB_NAME);
  console.log('[PlacetaID-Mongo] Conectado a', DB_NAME);
  return db;
}

export async function getRegistros() {
  try {
    const d = await getDb();
    return await d.collection('registros').find({}).sort({ createdAt: -1 }).limit(200).toArray();
  } catch (err) {
    console.error('[PlacetaID-Mongo] Error registros:', err.message);
    throw err;
  }
}

export async function getLogs(limit = 50) {
  try {
    const d = await getDb();
    return await d.collection('logs').find({}).sort({ timestamp: -1 }).limit(limit).toArray();
  } catch (err) {
    console.error('[PlacetaID-Mongo] Error logs:', err.message);
    throw err;
  }
}

export async function getStats() {
  try {
    const d = await getDb();
    const total = await d.collection('registros').countDocuments();
    const activos = await d.collection('registros').countDocuments({ activo: true, bloqueado: false });
    const bloqueados = await d.collection('registros').countDocuments({ bloqueado: true });
    const logsCount = await d.collection('logs').countDocuments();
    return { total, activos, bloqueados, pendientes: total - activos - bloqueados, totalLogs: logsCount };
  } catch (err) {
    console.error('[PlacetaID-Mongo] Error stats:', err.message);
    throw err;
  }
}

export async function desbloquear(dip) {
  try {
    const d = await getDb();
    const r = await d.collection('registros').findOneAndUpdate(
      { dip },
      { $set: { bloqueado: false, intentosFallidos: 0 } },
      { returnDocument: 'after' }
    );
    return r;
  } catch (err) {
    console.error('[PlacetaID-Mongo] Error desbloquear:', err.message);
    throw err;
  }
}

export async function toggleActivo(dip) {
  try {
    const d = await getDb();
    const registro = await d.collection('registros').findOne({ dip });
    if (!registro) throw new Error('Registro no encontrado');
    const nuevoEstado = !registro.activo;
    await d.collection('registros').updateOne(
      { dip },
      { $set: { activo: nuevoEstado } }
    );
    return { activo: nuevoEstado };
  } catch (err) {
    console.error('[PlacetaID-Mongo] Error toggle:', err.message);
    throw err;
  }
}

export async function close() {
  if (client) await client.close();
}
