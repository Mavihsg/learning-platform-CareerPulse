const { contextBridge, ipcRenderer } = require('electron');

/**
 * CareerPulse Desktop Secure Preload Script
 * Exposes a sandboxed IPC API to the client web application
 */
const desktopBridge = {
    isDesktop: true,
    platform: process.platform,
    version: '1.0.0',
    appVersion: '1.0.0',
    openExternal: (url) => ipcRenderer.send('open-external', url),
    showNotification: (title, body) => ipcRenderer.send('desktop-notify', { title, body }),
    checkOnlineStatus: () => navigator.onLine,
    minimize: () => ipcRenderer.send('window-minimize'),
    maximize: () => ipcRenderer.send('window-maximize'),
    close: () => ipcRenderer.send('window-close')
};

contextBridge.exposeInMainWorld('CareerPulseDesktop', desktopBridge);
contextBridge.exposeInMainWorld('electronAPI', desktopBridge);

