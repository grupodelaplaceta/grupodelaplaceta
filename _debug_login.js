import initSqlJs from 'sql.js';
import fs from 'fs';
import bcrypt from 'bcryptjs';

const SQL = await initSqlJs();
const buf = fs.readFileSync('./data/gdlp-crm.db');
const db = new SQL.Database(buf);
const r = db.exec("SELECT alias, rol, password_hash FROM solicitantes WHERE alias = 'admin'");
console.log('Result:', JSON.stringify(r, null, 2));

if (r.length > 0 && r[0].values.length > 0) {
  const hash = r[0].values[0][2];
  console.log('Hash found:', hash ? hash.substring(0, 40) + '...' : 'EMPTY');
  const valid = await bcrypt.compare('admin123', hash);
  console.log('Password valid:', valid);
} else {
  console.log('No admin user found');
}
