/**
 * Selector Universal de Cuentas, Personas y Empresas
 * Reutilizable en todas las vistas del admin del CRM
 * 
 * Uso: 
 *   <div id="buscadorCuenta"></div>
 *   <script>crearBuscador('buscadorCuenta', { tipo: 'cuenta', onSelect: (item) => {...} })</script>
 */

// Cache de datos
let _cacheUsuarios = null;
let _cacheCuentas = null;
let _cacheEntidades = null;
let _cacheTimestamp = 0;
const CACHE_DURACION = 30000; // 30 segundos

async function obtenerUsuarios() {
  if (_cacheUsuarios && Date.now() - _cacheTimestamp < CACHE_DURACION) return _cacheUsuarios;
  try {
    const r = await fetch('/api/bancario-proxy/usuarios');
    if (r.ok) _cacheUsuarios = await r.json();
    else _cacheUsuarios = [];
  } catch { _cacheUsuarios = []; }
  _cacheTimestamp = Date.now();
  return _cacheUsuarios;
}

async function obtenerCuentas() {
  if (_cacheCuentas && Date.now() - _cacheTimestamp < CACHE_DURACION) return _cacheCuentas;
  try {
    const r = await fetch('/api/bancario-proxy/cuentas');
    if (r.ok) _cacheCuentas = await r.json();
    else _cacheCuentas = [];
  } catch { _cacheCuentas = []; }
  _cacheTimestamp = Date.now();
  return _cacheCuentas;
}

async function obtenerEntidades() {
  if (_cacheEntidades && Date.now() - _cacheTimestamp < CACHE_DURACION) return _cacheEntidades;
  try {
    const r = await fetch('/api/admin/entidades');
    if (r.ok) _cacheEntidades = await r.json();
    else _cacheEntidades = [];
  } catch { _cacheEntidades = []; }
  _cacheTimestamp = Date.now();
  return _cacheEntidades;
}

/**
 * Crea un buscador con autocompletado
 * @param {string} containerId - ID del contenedor donde se renderizará
 * @param {object} opts - Opciones
 * @param {'cuenta'|'persona'|'empresa'|'todos'} opts.tipo - Tipo de búsqueda
 * @param {function} opts.onSelect - Callback cuando se selecciona un item
 * @param {string} opts.placeholder - Placeholder del input
 * @param {string} opts.inputId - ID opcional para el input generado
 * @param {string} opts.selectedId - ID opcional del elemento seleccionado inicial
 */
function crearBuscador(containerId, opts = {}) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const tipo = opts.tipo || 'cuenta';
  const placeholder = opts.placeholder || 'Buscar...';
  const inputId = opts.inputId || 'buscadorInput_' + Date.now();
  const resultsId = 'buscadorResults_' + Date.now();
  const selectedId = 'buscadorSelected_' + Date.now();

  container.innerHTML = `
    <div style="position:relative">
      <input type="text" id="${inputId}" class="form-input" placeholder="${placeholder}" autocomplete="off"
        style="padding:10px 14px;width:100%;border:2px solid var(--line);border-radius:8px;font-size:13px">
      <div id="${resultsId}" style="display:none;position:absolute;top:100%;left:0;right:0;z-index:1000;background:#fff;border:2px solid #ddd6fe;border-radius:0 0 8px 8px;max-height:250px;overflow-y:auto;box-shadow:0 8px 24px rgba(0,0,0,0.12)"></div>
    </div>
    <div id="${selectedId}" style="margin-top:8px;display:none"></div>
  `;

  const input = document.getElementById(inputId);
  const results = document.getElementById(resultsId);
  const selected = document.getElementById(selectedId);
  let selectedItem = null;
  let timeoutId = null;

  if (opts.selectedId) {
    // Intentar precargar seleccionado
    setTimeout(async () => {
      const items = await obtenerTodos();
      const found = items.find(i => i.id === opts.selectedId || i.dip === opts.selectedId || i.placetaId === opts.selectedId);
      if (found) mostrarSeleccionado(found);
    }, 100);
  }

  async function obtenerTodos() {
    let items = [];
    if (tipo === 'cuenta' || tipo === 'todos') {
      const cuentas = await obtenerCuentas();
      items = items.concat(cuentas.map(c => ({
        ...c,
        _tipo: 'cuenta',
        _label: `🏦 ${c.displayName || 'Cuenta'} · ${c.iban || c.id?.slice(0,10) || ''} · ${c.type || ''}`,
        _search: `${c.displayName || ''} ${c.iban || ''} ${c.placetaId || ''} ${c.id || ''} ${c.eip || ''}`.toLowerCase()
      })));
    }
    if (tipo === 'persona' || tipo === 'todos') {
      const usuarios = await obtenerUsuarios();
      items = items.concat(usuarios.map(u => ({
        ...u,
        _tipo: 'persona',
        _label: `👤 ${u.displayName || u.nombre || ''} · DIP: ${u.dip || ''} · ${u.placetaId || ''}`,
        _search: `${u.displayName || ''} ${u.nombre || ''} ${u.dip || ''} ${u.placetaId || ''}`.toLowerCase()
      })));
    }
    if (tipo === 'empresa' || tipo === 'todos') {
      const entidades = await obtenerEntidades();
      items = items.concat(entidades.map(e => ({
        ...e,
        _tipo: 'empresa',
        _label: `🏢 ${e.nombre || 'Empresa'} · EIP: ${e.eip || ''}`,
        _search: `${e.nombre || ''} ${e.eip || ''} ${e.cif || ''}`.toLowerCase()
      })));
    }
    return items;
  }

  function mostrarSeleccionado(item) {
    selectedItem = item;
    selected.style.display = 'block';
    selected.innerHTML = `
      <div style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:#f5f3ff;border-radius:8px;border:1px solid #ddd6fe;font-size:13px">
        <span style="flex:1">${item._label || item._tipo + ': ' + (item.id || item.dip || '')}</span>
        <button class="btn btn-sm btn-ghost" onclick="this.closest('#${selectedId}').style.display='none';document.getElementById('${inputId}').value='';document.getElementById('${inputId}').focus();selectedItem=null" style="padding:4px 8px;font-size:12px">✕</button>
      </div>`;
    input.value = item._label || '';
    results.style.display = 'none';
    if (opts.onSelect) opts.onSelect(item);
  }

  input.addEventListener('input', async function() {
    const q = this.value.trim().toLowerCase();
    if (timeoutId) clearTimeout(timeoutId);
    if (!q || q.length < 2) { results.style.display = 'none'; return; }
    
    timeoutId = setTimeout(async () => {
      const items = await obtenerTodos();
      const filtrados = items.filter(i => i._search.includes(q)).slice(0, 15);
      
      if (!filtrados.length) {
        results.innerHTML = '<div style="padding:12px;color:#999;font-size:13px;text-align:center">Sin resultados</div>';
        results.style.display = 'block';
        return;
      }
      
      results.innerHTML = filtrados.map(item => `
        <div class="buscador-item" style="padding:10px 14px;cursor:pointer;border-bottom:1px solid #f0f0f0;font-size:13px;transition:background 0.15s"
          onmouseover="this.style.background='#f5f3ff'" onmouseout="this.style.background=''"
          onclick="(function(){document.getElementById('${inputId}').value='${item._label.replace(/'/g,"\\'")}';document.getElementById('${resultsId}').style.display='none';mostrarSeleccionado_${selectedId}(JSON.parse('${JSON.stringify(item).replace(/'/g,"\\'").replace(/"/g,'&quot;')}'))})()">
          ${item._label}
          <span style="font-size:11px;color:#999;display:block;margin-top:2px">
            ${item._tipo === 'cuenta' ? `Saldo: ${(item.balancePz || 0).toLocaleString()} Pz` : 
              item._tipo === 'persona' ? `DIP: ${item.dip || ''} · Rol: ${item.rol || ''}` :
              `EIP: ${item.eip || ''} · CIF: ${item.cif || ''}`}
          </span>
        </div>
      `).join('');
      results.style.display = 'block';
    }, 200);
  });

  // Hacer disponible la función de selección globalmente para este buscador
  window['mostrarSeleccionado_' + selectedId] = mostrarSeleccionado;

  input.addEventListener('blur', () => setTimeout(() => results.style.display = 'none', 200));
  input.addEventListener('focus', () => { if (results.innerHTML) results.style.display = 'block'; });

  return {
    getSelected: () => selectedItem,
    clear: () => { selectedItem = null; selected.style.display = 'none'; input.value = ''; },
    setValue: (item) => mostrarSeleccionado(item)
  };
}
