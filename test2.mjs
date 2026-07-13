try {
  const m = await import('./src/routes/tributos.js');
  console.log('tributos OK');
} catch(e) {
  console.log('ERR:', e.message);
  if (e.stack) {
    const lines = e.stack.split('\n');
    for (const l of lines) {
      if (l.includes('file://') && !l.includes('node:') && !l.includes('[eval')) console.log(l.trim());
    }
  }
}
