const { app, BrowserWindow, ipcMain, Menu, Tray, nativeImage } = require('electron');
const path = require('path');
const { startSyncServer, processClientPush } = require('./server/sync-server');
const { startMdnsService, getPeers } = require('./server/mdns-service');
const TaskRepository = require('./db/database');
const logger = require('./server/console-ansi-logger');


const APP_CONFIG = {
    WINDOW_WIDTH: 1000,
    WINDOW_HEIGHT: 700,
    SYNC_PORT: 8080
};

let mainWindow;
let tray;
let isQuitting = false;

/**
 * Creates the main application window.
 */
function createWindow() {
    Menu.setApplicationMenu(null);

    mainWindow = new BrowserWindow({
        width: APP_CONFIG.WINDOW_WIDTH,
        height: APP_CONFIG.WINDOW_HEIGHT,
        autoHideMenuBar: true,
        icon: path.join(__dirname, 'icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'src/index.html'));

    // Prevent window destruction on close, hide to tray instead
    mainWindow.on('close', (event) => {
        if (!isQuitting) {
            event.preventDefault();
            mainWindow.hide();
        }
        return false;
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

/**
 * Initializes the system tray icon and context menu.
 */
function createTray() {
    const iconPath = path.join(__dirname, 'icon.png');
    const icon = nativeImage.createFromPath(iconPath);
    tray = new Tray(icon.resize({ width: 16, height: 16 }));

    const contextMenu = Menu.buildFromTemplate([
        { 
            label: 'Open STodo', 
            click: () => {
                if (mainWindow) mainWindow.show();
                else createWindow();
            } 
        },
        { type: 'separator' },
        { 
            label: 'Quit', 
            click: () => {
                isQuitting = true;
                app.quit();
            } 
        }
    ]);

    tray.setToolTip('STodo Desktop Hub');
    tray.setContextMenu(contextMenu);

    // Restore window on single left click
    tray.on('click', () => {
        if (mainWindow) mainWindow.show();
        else createWindow();
    });

    // Support for platforms where double-click is preferred
    tray.on('double-click', () => {
        if (mainWindow) mainWindow.show();
        else createWindow();
    });
}

const lastSyncs = new Map();
let syncTimeout = null;

/**
 * Triggers a debounced sync call to all discovered peers.
 */
function triggerPeerSync() {
    if (syncTimeout) clearTimeout(syncTimeout);
    syncTimeout = setTimeout(pushChangesToPeers, 500);
}

/**
 * Sends local modifications to all discovered mobile peers.
 */
async function pushChangesToPeers() {
    const peersList = getPeers();
    if (peersList.length === 0) return;
    for (const peer of peersList) {
        try {
            await syncWithPeer(peer);
        } catch (err) {
            logger.error('Sync', `Failed to sync with peer ${peer.name}: ${err.message}`);
        }
    }
}

/**
 * Performs a Push/Pull sync request to a specific peer.
 */
async function syncWithPeer(peer) {
    const lastSync = lastSyncs.get(peer.name) || 0;
    const localChanges = await TaskRepository.getForSync(lastSync);
    const payload = {
        protocolVersion: 1,
        lastSyncTimestamp: lastSync,
        clientChanges: localChanges
    };
    const response = await fetch(`http://${peer.host}:${peer.port}/api/v1/sync`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const responseData = await response.json();
    await handlePeerSyncResponse(peer.name, responseData);
    logger.info('Sync', `Successfully synchronized with peer ${peer.name}: sent ${localChanges.length} local changes, received ${responseData.serverChanges ? responseData.serverChanges.length : 0} changes`);
}

/**
 * Processes incoming changes from a peer sync response.
 */
async function handlePeerSyncResponse(peerName, responseData) {
    const { serverTimestamp, serverChanges } = responseData;
    if (serverChanges && serverChanges.length > 0) {
        await processClientPush(serverChanges);
        if (mainWindow) mainWindow.webContents.send('refresh-tasks');
    }
    lastSyncs.set(peerName, serverTimestamp);
}

// IPC Handlers (UI -> DB)
ipcMain.handle('get-tasks', async () => {
    return await TaskRepository.getAll();
});

ipcMain.handle('add-task', async (event, task) => {
    const maxPos = await TaskRepository.getMaxPosition();
    task.position = maxPos + 1;
    task.updatedAt = Date.now();
    const result = await TaskRepository.insert(task);
    triggerPeerSync();
    return result;
});

ipcMain.handle('update-task', async (event, task) => {
    task.updatedAt = Date.now();
    const result = await TaskRepository.update(task);
    triggerPeerSync();
    return result;
});

ipcMain.handle('delete-task', async (event, id) => {
    const task = await TaskRepository.getById(id);
    if (task) {
        task.isDeleted = true;
        task.updatedAt = Date.now();
        const result = await TaskRepository.update(task);
        triggerPeerSync();
        return result;
    }
});

app.on('ready', () => {
    createWindow();
    createTray();
    
    startSyncServer(APP_CONFIG.SYNC_PORT, () => {
        if (mainWindow) {
            mainWindow.webContents.send('refresh-tasks');
        }
    });
    startMdnsService(APP_CONFIG.SYNC_PORT, (peers) => {
        triggerPeerSync();
    });
    
    logger.info('App', 'STodo Desktop Hub initialized in background.');
});

// Keep the process running when windows are hidden
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        // App stays in tray
    }
});

app.on('activate', () => {
    if (mainWindow === null) createWindow();
    else mainWindow.show();
});

// Ensure flag is set before quitting
app.on('before-quit', () => {
    isQuitting = true;
});
