const modules = [
  './src/services/contribuciones.js',
  './src/config/db-supabase.js',
  './src/services/pdfGenerator.js',
  './src/middleware/auth.js',
  './src/config/db.js'
];
for (const mod of modules) {
  try {
    const m = await import(mod);
    console.log('OK:', mod);
  } catch(e) {
    console.log('ERR:', mod, '-', e.message);
    if (e.stack) {
      const lines = e.stack.split('\n');
      for (const l of lines) {
        if (l.includes('file://') && !l.includes('node:') && !l.includes('test_modules')) console.log(l.trim());
      }
    }
  }
}
