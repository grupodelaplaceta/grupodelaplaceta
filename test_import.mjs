try {
  const app = await import('./server.js');
  console.log('OK');
} catch(e) {
  console.error('ERR:', e.message);
  if (e.stack) {
    const lines = e.stack.split('\n');
    for (const l of lines) {
      if (l.includes('file://') && !l.includes('node:')) console.log(l.trim());
    }
  }
}
