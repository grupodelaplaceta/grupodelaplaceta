/* ═══════════════════════════════════════════════════════════════════════════
   PLACETA JUNIOR — JavaScript Frontend
   Solo llamadas a API — toda la lógica en servidor
   ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Llamada fetch estandarizada a la API
 */
async function apiFetch(url, opts = {}) {
  const r = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...opts
  });
  if (!r.ok) {
    let msg = `Error ${r.status}`;
    try { const e = await r.json(); if (e.error) msg = e.error; } catch {}
    throw new Error(msg);
  }
  return r.json();
}

/**
 * Mostrar toast notification
 */
function mostrarToast(mensaje, tipo = 'info') {
  const colors = {
    success: '#16a34a',
    error: '#dc2626',
    info: '#4f46e5',
    warning: '#d97706'
  };
  const existing = document.querySelector('.junior-toast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.className = 'junior-toast';
  toast.style.cssText = `
    position: fixed; bottom: 24px; right: 24px; z-index: 9999;
    background: ${colors[tipo] || colors.info}; color: #fff;
    padding: 14px 24px; border-radius: 12px; font-weight: 600;
    font-size: 0.9rem; box-shadow: 0 8px 32px rgba(0,0,0,0.2);
    animation: slideIn 0.3s ease-out;
    font-family: 'Plus Jakarta Sans', sans-serif;
  `;
  toast.textContent = mensaje;
  document.body.appendChild(toast);

  // Animación slideIn
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideIn { from { transform: translateX(100px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
  `;
  document.head.appendChild(style);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

/**
 * Mostrar modal
 */
function mostrarModal(titulo, html) {
  const existing = document.querySelector('.modal-overlay');
  if (existing) existing.remove();

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="modal-window">
      <div class="modal-header">
        <h3>${titulo}</h3>
        <button class="modal-close" onclick="this.closest('.modal-overlay').remove()">✕</button>
      </div>
      <div class="modal-body">${html}</div>
    </div>
  `;
  document.body.appendChild(overlay);
  overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.remove(); });
}

/**
 * Formatear fecha
 */
function formatFecha(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}
