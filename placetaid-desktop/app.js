// ── Estado ────────────────────────────────────────────────────────────────────
let identidades = [];
let authPending = null;
let selectedDip = null;

// ─── Inicializar ──────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  await cargarIdentidades();

  // Escuchar solicitudes de autenticación desde el protocolo
  window.placetaidDesktop.onAuthRequest((params) => {
    authPending = params;
    mostrarAuth(params);
  });
});

// ── Navegación ────────────────────────────────────────────────────────────────
function mostrarView(id) {
  document.querySelectorAll('.view').forEach(v => v.style.display = 'none');
  const view = document.getElementById('view-' + id);
  if (view) view.style.display = 'flex';
}

function volver() {
  mostrarView('identities');
}

// ── Identidades ───────────────────────────────────────────────────────────────
async function cargarIdentidades() {
  identidades = await window.placetaidDesktop.identities.list();
  renderIdentidades();
}

function renderIdentidades() {
  const list = document.getElementById('identitiesList');
  const empty = document.getElementById('emptyState');
  const addBtn = document.getElementById('btnAddIdentity');

  if (identidades.length === 0) {
    list.innerHTML = '';
    list.appendChild(empty);
    empty.style.display = '';
    addBtn.style.display = 'none';
    return;
  }

  empty.style.display = 'none';
  addBtn.style.display = '';
  list.innerHTML = identidades.map(id => `
    <div class="identity-card" onclick="seleccionarIdentidad('${id.dip}')">
      <div class="identity-avatar">${iniciales(id.nombre)}</div>
      <div class="identity-info">
        <div class="identity-name">${id.alias || id.nombre}</div>
        <div class="identity-dip">${id.dip} · ${id.nombre}</div>
      </div>
      <button class="identity-remove" onclick="event.stopPropagation();eliminarIdentidad('${id.dip}')">✕</button>
    </div>
  `).join('');
}

function iniciales(nombre) {
  return (nombre || '?').split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
}

async function eliminarIdentidad(dip) {
  if (!confirm(`¿Eliminar identidad ${dip}?`)) return;
  identidades = await window.placetaidDesktop.identities.remove(dip);
  renderIdentidades();
}

// ── Agregar identidad manual ──────────────────────────────────────────────────
function abrirAgregar() {
  document.getElementById('addDip').value = '';
  document.getElementById('addNombre').value = '';
  document.getElementById('addAlias').value = '';
  mostrarView('add');
}

async function guardarIdentidad() {
  const dip = document.getElementById('addDip').value.trim().toUpperCase();
  const nombre = document.getElementById('addNombre').value.trim();
  const alias = document.getElementById('addAlias').value.trim();

  if (!dip || !nombre) return alert('Completa al menos DIP y nombre');
  if (!/^\d{8}[A-Z]$/.test(dip)) return alert('Formato de DIP inválido (8 dígitos + letra)');

  identidades = await window.placetaidDesktop.identities.add({ dip, nombre, alias: alias || nombre });
  renderIdentidades();
  volver();
}

// ── Solicitud de autenticación ────────────────────────────────────────────────
function mostrarAuth(params) {
  document.getElementById('authServicio').textContent = params.servicio || 'Desconocido';
  document.getElementById('authCodigo').textContent = params.codigo || '—';
  document.getElementById('authServiceName').textContent =
    params.servicio ? `Solicitud de ${params.servicio}` : 'Solicitud de autenticación';

  // Renderizar selector de identidades
  const selector = document.getElementById('authIdentitySelector');
  selectedDip = null;
  document.getElementById('btnAuthorize').disabled = true;

  if (identidades.length === 0) {
    selector.innerHTML = '<p style="color:var(--tm);font-size:13px;text-align:center;padding:12px;">No hay identidades. Añade una primero.</p>';
  } else {
    selector.innerHTML = identidades.map(id => `
      <div class="identity-option" data-dip="${id.dip}" onclick="seleccionarAuthIdentity('${id.dip}')">
        <div class="radio"></div>
        <div>
          <div style="font-weight:600;font-size:14px;">${id.alias || id.nombre}</div>
          <div style="font-size:12px;color:var(--tm);">${id.dip}</div>
        </div>
      </div>
    `).join('');
  }

  mostrarView('auth');
}

function seleccionarAuthIdentity(dip) {
  selectedDip = dip;
  document.querySelectorAll('.identity-option').forEach(el => {
    el.classList.toggle('selected', el.dataset.dip === dip);
  });
  document.getElementById('btnAuthorize').disabled = false;
  const identidad = identidades.find(i => i.dip === dip);
  document.getElementById('authIdentidad').textContent = identidad ? (identidad.alias || identidad.nombre) : dip;
}

async function autorizar() {
  if (!authPending || !selectedDip) return;
  const btn = document.getElementById('btnAuthorize');
  btn.disabled = true;
  btn.textContent = '⏳ Autorizando...';

  const result = await window.placetaidDesktop.auth.authorize({
    requestId: authPending.requestId || authPending.id,
    dip: selectedDip
  });

  if (result.ok) {
    btn.textContent = '✅ Autorizado';
    setTimeout(() => window.placetaidDesktop.window.close(), 1000);
  } else {
    alert('Error: ' + (result.error || 'No se pudo autorizar'));
    btn.disabled = false;
    btn.textContent = '✓ Autorizar';
  }
}

async function denegar() {
  if (!authPending || !selectedDip) return;
  await window.placetaidDesktop.auth.deny({
    requestId: authPending.requestId || authPending.id,
    dip: selectedDip
  });
  window.placetaidDesktop.window.close();
}
