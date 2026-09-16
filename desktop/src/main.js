const { app, BrowserWindow, Menu, Tray, shell, ipcMain, Notification } = require('electron');
const path = require('path');

// Single instance lock
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
    app.quit();
}

let mainWindow = null;
let tray = null;
const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged;
const DEFAULT_URL = isDev ? 'http://localhost:8080' : 'https://careerpulse-lms.onrender.com';
const APP_URL = process.env.APP_URL || DEFAULT_URL;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1360,
        height: 860,
        minWidth: 960,
        minHeight: 640,
        title: 'CareerPulse | Enterprise Learning Platform',
        backgroundColor: '#090B10',
        show: false,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: false,
            devTools: true
        }
    });

    // Graceful reveal once ready
    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
        if (isDev) {
            console.log(`[CareerPulse Desktop] Connected to: ${APP_URL}`);
        }
    });

    // Tag web document with desktop class on ready
    mainWindow.webContents.on('dom-ready', () => {
        mainWindow.webContents.executeJavaScript(`
            document.body.classList.add('is-electron-desktop');
            window.isElectronApp = true;
        `).catch(() => {});
    });

    // Load remote web application
    mainWindow.loadURL(APP_URL);

    // If initial load fails (e.g. server down or offline), render branded offline screen
    mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription, validatedURL) => {
        // Prevent infinite loop if offline.html itself triggers did-fail-load
        if (validatedURL && validatedURL.includes('offline.html')) return;
        
        console.warn(`[CareerPulse Desktop] Failed to load ${validatedURL}: ${errorDescription} (${errorCode})`);
        mainWindow.loadFile(path.join(__dirname, 'offline.html'), {
            query: { url: APP_URL }
        });
    });

    // Intercept navigation for external links (YouTube, docs, GitHub)
    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        if (url.includes('accounts.google.com')) {
            return {
                action: 'allow',
                overrideBrowserWindowOptions: {
                    width: 500,
                    height: 650,
                    autoHideMenuBar: true,
                    webPreferences: {
                        nodeIntegration: false,
                        contextIsolation: true
                    }
                }
            };
        }
        if (isExternalUrl(url)) {
            shell.openExternal(url);
            return { action: 'deny' };
        }
        return { action: 'allow' };
    });

    mainWindow.webContents.on('will-navigate', (event, url) => {
        if (isExternalUrl(url) && !url.includes('offline.html')) {
            event.preventDefault();
            shell.openExternal(url);
        }
    });

    // Build native application menu
    buildAppMenu();

    // Minimize to tray on close if preferred, or standard close
    mainWindow.on('close', (event) => {
        if (app.isQuitting) {
            mainWindow = null;
        } else {
            // Standard close
            mainWindow = null;
        }
    });
}

function isExternalUrl(url) {
    try {
        const parsed = new URL(url);
        const host = parsed.hostname.toLowerCase();
        return (
            host.includes('youtube.com') ||
            host.includes('youtu.be') ||
            host.includes('github.com') ||
            host.includes('google.com') ||
            host.includes('neon.tech')
        );
    } catch {
        return false;
    }
}

function buildAppMenu() {
    const isMac = process.platform === 'darwin';

    const template = [
        ...(isMac ? [{
            label: app.name,
            submenu: [
                { role: 'about' },
                { type: 'separator' },
                { role: 'services' },
                { type: 'separator' },
                { role: 'hide' },
                { role: 'hideOthers' },
                { role: 'unhide' },
                { type: 'separator' },
                { role: 'quit' }
            ]
        }] : []),
        {
            label: 'File',
            submenu: [
                {
                    label: 'Reconnect to Server',
                    accelerator: 'CmdOrCtrl+Shift+R',
                    click: () => {
                        if (mainWindow) mainWindow.loadURL(APP_URL);
                    }
                },
                { type: 'separator' },
                isMac ? { role: 'close' } : { role: 'quit' }
            ]
        },
        {
            label: 'Edit',
            submenu: [
                { role: 'undo' },
                { role: 'redo' },
                { type: 'separator' },
                { role: 'cut' },
                { role: 'copy' },
                { role: 'paste' },
                { role: 'selectAll' }
            ]
        },
        {
            label: 'View',
            submenu: [
                { role: 'reload' },
                { role: 'forceReload' },
                {
                    label: 'Toggle Developer Tools',
                    accelerator: isMac ? 'Alt+Command+I' : 'Ctrl+Shift+I',
                    click: () => {
                        if (mainWindow) {
                            if (mainWindow.webContents.isDevToolsOpened()) {
                                mainWindow.webContents.closeDevTools();
                            } else {
                                mainWindow.webContents.openDevTools({ mode: 'detach' });
                            }
                        }
                    }
                },
                { type: 'separator' },
                { role: 'resetZoom' },
                { role: 'zoomIn' },
                { role: 'zoomOut' },
                { type: 'separator' },
                { role: 'togglefullscreen' }
            ]
        },
        {
            label: 'Navigate',
            submenu: [
                {
                    label: 'Dashboard',
                    accelerator: 'CmdOrCtrl+1',
                    click: () => navigateTo('dashboard')
                },
                {
                    label: 'Course Catalog',
                    accelerator: 'CmdOrCtrl+2',
                    click: () => navigateTo('catalog')
                },
                {
                    label: 'My Learning',
                    accelerator: 'CmdOrCtrl+3',
                    click: () => navigateTo('my-learning')
                },
                {
                    label: 'Quiz Arena (AI)',
                    accelerator: 'CmdOrCtrl+4',
                    click: () => navigateTo('quiz-arena')
                },
                {
                    label: 'Discussions Forum',
                    accelerator: 'CmdOrCtrl+5',
                    click: () => navigateTo('discussions')
                },
                {
                    label: 'Leaderboard',
                    accelerator: 'CmdOrCtrl+6',
                    click: () => navigateTo('leaderboard')
                },
                {
                    label: 'Learner Profile',
                    accelerator: 'CmdOrCtrl+7',
                    click: () => navigateTo('profile')
                }
            ]
        },
        {
            label: 'Help',
            submenu: [
                {
                    label: 'Documentation & Architecture',
                    click: async () => {
                        await shell.openExternal('https://github.com/Mavihsg/learning-platform-CareerPulse');
                    }
                },
                {
                    label: 'About CareerPulse',
                    click: () => {
                        if (Notification.isSupported()) {
                            new Notification({
                                title: 'CareerPulse Desktop v1.0.0',
                                body: 'Enterprise Gamified Learning & Advancement Platform'
                            }).show();
                        }
                    }
                }
            ]
        }
    ];

    const menu = Menu.buildFromTemplate(template);
    Menu.setApplicationMenu(menu);
}

function navigateTo(viewName) {
    if (mainWindow && mainWindow.webContents) {
        mainWindow.webContents.executeJavaScript(`if (window.App && App.navigate) { App.navigate('${viewName}'); }`);
    }
}

// IPC event listeners
ipcMain.on('open-external', (event, url) => {
    shell.openExternal(url);
});

ipcMain.on('desktop-notify', (event, { title, body }) => {
    if (Notification.isSupported()) {
        new Notification({ title: title || 'CareerPulse', body: body || '' }).show();
    }
});

ipcMain.on('window-minimize', () => {
    if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
    if (mainWindow) {
        if (mainWindow.isMaximized()) {
            mainWindow.unmaximize();
        } else {
            mainWindow.maximize();
        }
    }
});

ipcMain.on('window-close', () => {
    if (mainWindow) mainWindow.close();
});

// Second instance focus
app.on('second-instance', () => {
    if (mainWindow) {
        if (mainWindow.isMinimized()) mainWindow.restore();
        mainWindow.focus();
    }
});

app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
