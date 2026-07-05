const { app, BrowserWindow, ipcMain, dialog, shell, protocol } = require('electron');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const http = require('http');

const PLACETAID_API = process.env.PLACETAID_API_URL || 'https://id.laplaceta.org/api';
const PROTOCOL = 'placetaid-desktop://';
const CLIENT_ID = 'placetaid-desktop';

let mainWindow = null;
let pendingRequests = [];

// ── Almacenamiento de identidades ──────────────────────────────────────────
function identitiesFile() {
  return path.join(app.getPath('userData'), 'identities.enc');
}

function machineKey() {
  return crypto
    .createHash('sha256')
    .update(`${app.getPath('userData')}|${process.env.USERNAME || ''}|placetaid-desktop`)
    .digest();
}

function encrypt(data) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', machineKey(), iv);
  const enc = Buffer.concat([cipher.update(JSON.stringify(data), 'utf8'), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), enc]).toString('base64');
}

function decrypt(raw) {
  const buf = Buffer.from(raw, 'base64');
  const iv = buf.subarray(0, 12);
  const tag = buf.subarray(12, 28);
  const enc = buf.subarray(28);
  const decipher = crypto.createDecipheriv('aes-256-gcm', machineKey(), iv);
  decipher.setAuthTag(tag);
  return JSON.parse(Buffer.concat([decipher.update(enc), decipher.final()]).toString('utf8'));
}

function loadIdentities() {
  try {
    return decrypt(fs.readFileSync(identitiesFile(), 'utf8'));
  } catch {
    return [];
  }
}

function saveIdentities(list) {
  fs.writeFileSync(identitiesFile(), encrypt(list), 'utf8');
}

// ── Ventana principal ──────────────────────────────────────────────────────
function createWindow(params = {}) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.show();
    mainWindow.focus();
    if (params.action) mainWindow.webContents.send('auth-request', params);
    return;
  }

  mainWindow = new BrowserWindow({
    width: 480,
    height: 640,
    resizable: false,
    frame: false,
    transparent: false,
    backgroundColor: '#1c005f',
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  mainWindow.loadFile('index.html');
  mainWindow.setMenu(null);
  // Identificar la app para que plid26 detecte el origen desktop
  mainWindow.webContents.setUserAgent(`placetaid-desktop/${require('./package.json').version} Electron`);

  if (params.action) {
    mainWindow.webContents.once('did-finish-load', () => {
      mainWindow.webContents.send('auth-request', params);
    });
  }

  mainWindow.on('closed', () => { mainWindow = null; });
}

// ── Manejar protocolo placetaid-desktop:// ─────────────────────────────────
function handleProtocol(url) {
  try {
    const parsed = new URL(url);
    const action = parsed.hostname; // 'auth'
    const params = Object.fromEntries(parsed.searchParams.entries());
    params.action = action;

    if (action === 'auth') {
      createWindow(params);
    }
  } catch (e) {
    console.error('Error parsing protocol URL:', e.message);
  }
}

// ── Registrar protocolo en el SO ───────────────────────────────────────────
if (process.defaultApp) {
  if (process.argv.length >= 2) {
    app.setAsDefaultProtocolClient(PROTOCOL, process.execPath, [path.resolve(process.argv[1])]);
  }
} else {
  app.setAsDefaultProtocolClient(PROTOCOL);
}

// ── IPC: gestión de identidades ────────────────────────────────────────────
ipcMain.handle('identities:list', () => loadIdentities());

ipcMain.handle('identities:add', (_, identity) => {
  const list = loadIdentities();
  // Evitar duplicados por DIP
  const filtered = list.filter(i => i.dip !== identity.dip);
  filtered.push({ ...identity, addedAt: new Date().toISOString() });
  saveIdentities(filtered);
  return filtered;
});

ipcMain.handle('identities:remove', (_, dip) => {
  const list = loadIdentities().filter(i => i.dip !== dip);
  saveIdentities(list);
  return list;
});

ipcMain.handle('identities:clear', () => {
  saveIdentities([]);
  return [];
});

// ── IPC: autorizar solicitud ───────────────────────────────────────────────
ipcMain.handle('auth:authorize', async (_, { requestId, dip }) => {
  try {
    const identities = loadIdentities();
    const identity = identities.find(i => i.dip === dip);
    if (!identity) return { ok: false, error: 'Identidad no encontrada' };

    const res = await fetch(`${PLACETAID_API}/mobil/authorize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId, dip, authorized: true })
    });
    const data = await res.json();
    return data;
  } catch (err) {
    return { ok: false, error: err.message };
  }
});

ipcMain.handle('auth:deny', async (_, { requestId, dip }) => {
  try {
    const res = await fetch(`${PLACETAID_API}/mobil/authorize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId, dip, authorized: false })
    });
    return await res.json();
  } catch (err) {
    return { ok: false, error: err.message };
  }
});

// ── IPC: ventana ───────────────────────────────────────────────────────────
ipcMain.handle('window:minimize', () => mainWindow?.minimize());
ipcMain.handle('window:close', () => mainWindow?.close());

// ── Arranque ───────────────────────────────────────────────────────────────
const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', (_, argv) => {
    const url = argv.find(a => a.startsWith(PROTOCOL));
    if (url) handleProtocol(url);
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });

  app.on('open-url', (_, url) => {
    if (url.startsWith(PROTOCOL)) handleProtocol(url);
  });

  app.whenReady().then(() => {
    // Manejar URL de protocolo desde los argumentos de línea de comandos
    const url = process.argv.find(a => a.startsWith(PROTOCOL));
    if (url) {
      handleProtocol(url);
    } else {
      createWindow();
    }
  });

  app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
  });

  app.on('activate', () => {
    if (!mainWindow) createWindow();
  });
}
