const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('placetaidDesktop', {
  // Gestión de identidades
  identities: {
    list: () => ipcRenderer.invoke('identities:list'),
    add: (identity) => ipcRenderer.invoke('identities:add', identity),
    remove: (dip) => ipcRenderer.invoke('identities:remove', dip),
    clear: () => ipcRenderer.invoke('identities:clear')
  },

  // Autorización
  auth: {
    authorize: (params) => ipcRenderer.invoke('auth:authorize', params),
    deny: (params) => ipcRenderer.invoke('auth:deny', params)
  },

  // Ventana
  window: {
    minimize: () => ipcRenderer.invoke('window:minimize'),
    close: () => ipcRenderer.invoke('window:close')
  },

  // Eventos de solicitud de autenticación desde el protocolo
  onAuthRequest: (callback) => {
    ipcRenderer.on('auth-request', (_, params) => callback(params));
  }
});
