import bcrypt from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';
import { initializeDatabase, getDb } from './db.js';

async function seed() {
  await initializeDatabase();
  const db = getDb();
  console.log('🌱 Sembrando datos iniciales...');

  const adminPass = await bcrypt.hash('admin123', 10);
  const adminExistente = db.prepare("SELECT id FROM solicitantes WHERE alias = ?").get('admin');
  if (!adminExistente) {
    const dip = '00000001A';
    db.prepare('INSERT INTO solicitantes (alias, nombre_real, email, fecha_nacimiento, edad, dip, placeid, franja_edad, hash_credencial, estado, password_hash, rol, cargo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)').run('admin', 'Administrador Sistema', 'admin@grupodelaplaceta.org', '1990-01-01', 36, dip, `PLID-${dip}`, 'Alta_Plena', uuidv4(), 'activo', adminPass, 'administrador', 'Administrador del CRM');
    console.log('✅ Admin creado: admin / admin123');
  } else {
    console.log('ℹ️ Admin ya existe');
  }

  const tratExistentes = db.prepare('SELECT COUNT(*) as count FROM registro_tratamiento_datos').get();
  if (!tratExistentes || tratExistentes.count === 0) {
    const insert = db.prepare('INSERT INTO registro_tratamiento_datos (responsable, finalidad, datos_recogidos, periodo_conservacion, base_legal) VALUES (?, ?, ?, ?, ?)');
    const ts = [['Asociación Grupo de La Placeta', 'Gestión de identidad', 'Nombre, email, fecha nacimiento, DIP', '5 años desde baja', 'Relación contractual'], ['Asociación Grupo de La Placeta', 'Gestión bancaria simulada', 'Saldos, transacciones', '5 años', 'Interés legítimo'], ['Asociación Grupo de La Placeta', 'Gestión de justicia', 'Denuncias, resoluciones', '10 años', 'Obligación legal'], ['Asociación Grupo de La Placeta', 'Notificaciones', 'Email, alias', '2 años', 'Interés legítimo']];
    for (const t of ts) insert.run(t[0], t[1], t[2], t[3], t[4]);
    console.log('✅ Tratamientos RGPD creados');
  }

  const admin = db.prepare("SELECT id FROM solicitantes WHERE alias = ?").get('admin');
  if (admin) {
    if (!db.prepare("SELECT id FROM cuentas_bancarias WHERE tipo_cuenta = ?").get('Tesoro')) {
      db.prepare('INSERT INTO cuentas_bancarias (usuario_id, tipo_cuenta, saldo, saldo_maximo, estado) VALUES (?, ?, ?, ?, ?)').run(admin.id, 'Tesoro', 10000000, 100000000, 'activa');
      console.log('✅ Cuenta del Tesoro creada');
    }
    if (!db.prepare("SELECT id FROM cuentas_bancarias WHERE tipo_cuenta = ?").get('Administracion')) {
      db.prepare('INSERT INTO cuentas_bancarias (usuario_id, tipo_cuenta, saldo, saldo_maximo, estado) VALUES (?, ?, ?, ?, ?)').run(admin.id, 'Administracion', 5000000, 50000000, 'activa');
      console.log('✅ Cuenta de Administración creada');
    }
  }
  console.log('🌱 Seed completado!');
}

seed().catch(console.error);
