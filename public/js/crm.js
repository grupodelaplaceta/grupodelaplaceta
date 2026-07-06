// ═══════════════════════════════════════════════════════════════════════════
//  GDLP CRM - JavaScript del Sistema Premium v2.0
// ═══════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
  inicializarTooltips();
  inicializarFormularios();
  inicializarModales();
  inicializarSidebar();
  inicializarBusqueda();
  cargarContadorNotificaciones();
});

// ── Contador de notificaciones ─────────────────────────────────────────────
async function cargarContadorNotificaciones() {
  try {
    const r = await fetch('/api/notificaciones/no-leidas');
    const data = await r.json();
    const total = data.total || 0;
    ['notifBadge', 'notifBadgeMobile'].forEach(id => {
      const el = document.getElementById(id);
      if (el) {
        if (total > 0) { el.style.display = 'inline'; el.textContent = total; }
        else el.style.display = 'none';
      }
    });
  } catch (e) { /* silencioso */ }
}

// ── Sidebar ─────────────────────────────────────────────────────────────────
function inicializarSidebar() {
  const sidebar = document.getElementById('sidebar');
  if (!sidebar) return;
  // Record collapsed state
  const collapsed = localStorage.getItem('gdlp-sidebar-collapsed') === 'true';
  if (collapsed) sidebar.classList.add('collapsed');
}

function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  if (!sidebar) return;
  sidebar.classList.toggle('collapsed');
  localStorage.setItem('gdlp-sidebar-collapsed', sidebar.classList.contains('collapsed'));
}

// ── Tooltips ────────────────────────────────────────────────────────────────
function inicializarTooltips() {
  document.querySelectorAll('[data-tooltip]').forEach(el => {
    el.addEventListener('mouseenter', () => {
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

// ── API Formularios ─────────────────────────────────────────────────────────
function inicializarFormularios() {
  document.querySelectorAll('[data-api-form]').forEach(form => {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn = form.querySelector('button[type="submit"]');
      const url = form.dataset.apiForm;
      const method = form.dataset.apiMethod || 'POST';

      btn.disabled = true;
      const originalText = btn.innerHTML;
      btn.innerHTML = '<span class="spinner"></span> Procesando...';

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
          form.reset();
          mostrarToast('success', result.message || '✅ Operación exitosa');
          if (form.dataset.apiRedirect) {
            setTimeout(() => window.location.href = form.dataset.apiRedirect, 1000);
          }
          if (typeof form.onsuccess === 'function') form.onsuccess(result);
          // Auto-refresh: buscar funciones de recarga en el ámbito global
          if (typeof cargarTributosResumen === 'function') cargarTributosResumen();
          if (typeof cargarContribuyentes === 'function') cargarContribuyentes();
          if (typeof cargarFacturas === 'function') cargarFacturas();
          if (typeof cargarDeclaraciones === 'function') cargarDeclaraciones();
        } else {
          mostrarToast('error', result.error || '❌ Error en la operación');
        }
      } catch (err) {
        mostrarToast('error', '❌ Error de conexión con el servidor');
      } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
      }
    });
  });
}

// ── Toast System ────────────────────────────────────────────────────────────
function mostrarToast(tipo, texto) {
  const container = document.getElementById('mensajes') || crearContenedorMensajes();
  const icons = { success: '✅', error: '❌', info: 'ℹ️', warning: '⚠️' };
  const msg = document.createElement('div');
  msg.className = `alert alert-${tipo}`;
  msg.innerHTML = `<span>${icons[tipo] || ''}</span><span>${texto}</span>`;
  container.appendChild(msg);
  setTimeout(() => {
    msg.style.opacity = '0';
    msg.style.transform = 'translateX(30px)';
    msg.style.transition = 'all 0.3s';
    setTimeout(() => msg.remove(), 300);
  }, 4000);
}

function crearContenedorMensajes() {
  const container = document.createElement('div');
  container.id = 'mensajes';
  document.body.appendChild(container);
  return container;
}

// ── Modal System ────────────────────────────────────────────────────────────
function inicializarModales() {
  document.querySelectorAll('[data-modal-open]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.dataset.modalOpen;
      const modal = document.getElementById(modalId);
      if (modal) modal.classList.add('open');
    });
  });

  document.querySelectorAll('[data-modal-close]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modal = btn.closest('.modal-overlay, .confirm-overlay');
      if (modal) modal.classList.remove('open');
    });
  });

  // Close on overlay click
  document.querySelectorAll('.modal-overlay, .confirm-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) overlay.classList.remove('open');
    });
  });

  // Close on Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.open, .confirm-overlay.open').forEach(m => m.classList.remove('open'));
    }
  });
}

function abrirModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('open');
}

function cerrarModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('open');
}

function mostrarConfirmacion(titulo, mensaje, onConfirm, onCancel, tipo) {
  const overlay = document.createElement('div');
  overlay.className = 'confirm-overlay open';
  const icono = tipo === 'danger' ? '⚠️' : tipo === 'success' ? '✅' : 'ℹ️';
  overlay.innerHTML = `
    <div class="confirm-window">
      <div class="icon">${icono}</div>
      <h3>${titulo}</h3>
      <p>${mensaje}</p>
      <div class="confirm-actions">
        <button class="btn btn-outline" id="confirmCancel">Cancelar</button>
        <button class="btn ${tipo === 'danger' ? 'btn-danger' : tipo === 'success' ? 'btn-success' : 'btn-primary'}" id="confirmOk">Confirmar</button>
      </div>
    </div>`;
  document.body.appendChild(overlay);
  overlay.querySelector('#confirmCancel').addEventListener('click', () => {
    overlay.remove();
    if (onCancel) onCancel();
  });
  overlay.querySelector('#confirmOk').addEventListener('click', () => {
    overlay.remove();
    if (onConfirm) onConfirm();
  });
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) { overlay.remove(); if (onCancel) onCancel(); }
  });
}

// ── Búsqueda rápida en tablas ───────────────────────────────────────────────
function inicializarBusqueda() {
  document.querySelectorAll('[data-search]').forEach(input => {
    input.addEventListener('input', () => {
      const table = document.querySelector(input.dataset.search);
      if (!table) return;
      const q = input.value.toLowerCase();
      table.querySelectorAll('tbody tr').forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(q) ? '' : 'none';
      });
    });
  });
}

// ── Copy to clipboard ───────────────────────────────────────────────────────
function copiarAlPortapapeles(texto, mensaje) {
  navigator.clipboard.writeText(texto).then(() => {
    mostrarToast('success', mensaje || '📋 Copiado al portapapeles');
  }).catch(() => {
    mostrarToast('error', '❌ No se pudo copiar');
  });
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

function formatMoneda(cantidad) {
  return new Intl.NumberFormat('es-ES', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(cantidad || 0) + ' Pz';
}
