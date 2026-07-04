import bcrypt from 'bcryptjs';
import initSqlJs from 'sql.js';
import fs from 'fs';

const SQL = await initSqlJs();
const buf = fs.readFileSync('./data/gdlp-crm.db');
const db = new SQL.Database(buf);

const hash = await bcrypt.hash('Demo1234!', 10);

// Check if user exists
const existing = db.exec("SELECT alias FROM solicitantes WHERE alias = '11111111D' OR dip = '11111111D'");
if (existing.length > 0 && existing[0].values.length > 0) {
  console.log('User already exists, updating password...');
  db.run("UPDATE solicitantes SET password_hash = ?, estado = 'activo' WHERE alias = '11111111D' OR dip = '11111111D'", [hash]);
} else {
  console.log('Creating new ciudadano user...');
  db.run(
    "INSERT INTO solicitantes (alias, nombre_real, email, fecha_nacimiento, edad, dip, placeid, franja_edad, hash_credencial, estado, password_hash, rol) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
    ['11111111D', 'Ciudadano Demo', 'demo@laplaceta.org', '1990-01-01', 36, '11111111D', 'PLID-11111111D', 'Alta_Plena', 'x', 'activo', hash, 'miembro']
  );
}

// Verify
const r = db.exec("SELECT alias, rol, estado FROM solicitantes WHERE alias = '11111111D'");
console.log('User:', JSON.stringify(r[0]?.values[0]));

const data = db.export();
fs.writeFileSync('./data/gdlp-crm.db', Buffer.from(data));
console.log('DB saved successfully');
