// ── GDLP CRM - JavaScript del Sistema ───────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  inicializarTooltips();
  inicializarFormularios();
});

function inicializarTooltips() {
  document.querySelectorAll('[data-tooltip]').forEach(el => {
    el.addEventListener('mouseenter', (e) => {
      const tooltip = document.createElement('div');
      tooltip.className = 'tooltip';
      tooltip.textContent = el.dataset.tooltip;
      document.body.appendChild(tooltip);
      const rect = el.getBoundingClientRect();
      tooltip.style.top = `${rect.top - tooltip.offsetHeight - 8}px`;
      tooltip.style.left = `${rect.left + (rect.width - tooltip.offsetWidth) / 2}px`;
    });
    el.addEventListener('mouseleave', () => {
      document.querySelector('.tooltip')?.remove();
    });
  });
}

function inicializarFormularios() {
  // Fetch API helper para formularios
  document.querySelectorAll('[data-api-form]').forEach(form => {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn = form.querySelector('button[type="submit"]');
      const url = form.dataset.apiForm;
      const method = form.dataset.apiMethod || 'POST';
      
      btn.disabled = true;
      const originalText = btn.textContent;
      btn.textContent = 'Procesando...';

      try {
        const formData = new FormData(form);
        const data = Object.fromEntries(formData);
        
        const res = await fetch(url, {
          method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        
        const result = await res.json();
        
        if (result.success) {
          mostrarMensaje('success', result.message || 'Operación exitosa');
          if (form.dataset.apiRedirect) {
            setTimeout(() => window.location.href = form.dataset.apiRedirect, 1000);
          }
          if (typeof form.onsuccess === 'function') form.onsuccess(result);
        } else {
          mostrarMensaje('error', result.error || 'Error en la operación');
        }
      } catch (err) {
        mostrarMensaje('error', 'Error de conexión con el servidor');
      } finally {
        btn.disabled = false;
        btn.textContent = originalText;
      }
    });
  });
}

function mostrarMensaje(tipo, texto) {
  const container = document.getElementById('mensajes') || crearContenedorMensajes();
  const msg = document.createElement('div');
  msg.className = `alert alert-${tipo}`;
  msg.textContent = texto;
  container.appendChild(msg);
  setTimeout(() => msg.remove(), 5000);
}

function crearContenedorMensajes() {
  const container = document.createElement('div');
  container.id = 'mensajes';
  container.style.cssText = 'position:fixed;top:80px;right:20px;z-index:9999;max-width:400px;display:flex;flex-direction:column;gap:8px;';
  document.body.appendChild(container);
  return container;
}

// ── API Helper ──────────────────────────────────────────────────────────────
async function apiFetch(url, options = {}) {
  const defaultOptions = {
    headers: { 'Content-Type': 'application/json' }
  };
  const res = await fetch(url, { ...defaultOptions, ...options });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: 'Error del servidor' }));
    throw new Error(error.error || `HTTP ${res.status}`);
  }
  return res.json();
}

// ── Formatos ────────────────────────────────────────────────────────────────
function formatPz(cantidad) {
  return new Intl.NumberFormat('es-ES', {
    style: 'decimal',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(cantidad) + ' Pz';
}

function formatFecha(fecha) {
  if (!fecha) return '—';
  return new Date(fecha + 'Z').toLocaleDateString('es-ES', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}
