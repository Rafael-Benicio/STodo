const { app, BrowserWindow, ipcMain, Menu, Tray, nativeImage } = require('electron');
const path = require('path');
const TaskService = require('./service/taskService');
const { PriorityRepository } = require('./db/database');
const logger = require('./logger');

const APP_CONFIG = {
    WINDOW_WIDTH: 1000,
    WINDOW_HEIGHT: 700,
    TRAY_ICON_SIZE: 16
};

const TIMER_CONFIG = {
    EXPIRATION_CHECK_MS: 30000
};

let mainWindow = null;
let tray = null;
let isQuitting = false;
let expirationInterval = null;

/**
 * Creates the main application browser window.
 * @example createWindow();
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
    attachWindowEvents();
}

/**
 * Attaches lifecycle event listeners to the main window.
 * @example attachWindowEvents();
 */
function attachWindowEvents() {
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
 * Displays the main window or instantiates it if not present.
 * @example showMainWindow();
 */
function showMainWindow() {
    if (mainWindow) {
        mainWindow.show();
        return;
    }
    createWindow();
}

/**
 * Builds the context menu template for the system tray icon.
 * @returns {Menu} The constructed Electron menu instance.
 * @example const menu = buildTrayContextMenu();
 */
function buildTrayContextMenu() {
    return Menu.buildFromTemplate([
        {
            label: 'Open STodo',
            click: () => showMainWindow()
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
}

/**
 * Initializes the system tray icon and attaches user click listeners.
 * @example createTray();
 */
function createTray() {
    const iconPath = path.join(__dirname, 'icon.png');
    const icon = nativeImage.createFromPath(iconPath);
    tray = new Tray(icon.resize({ width: APP_CONFIG.TRAY_ICON_SIZE, height: APP_CONFIG.TRAY_ICON_SIZE }));

    tray.setToolTip('STodo Desktop');
    tray.setContextMenu(buildTrayContextMenu());
    tray.on('click', () => showMainWindow());
    tray.on('double-click', () => showMainWindow());
}

/**
 * Periodically checks for expired tasks and notifies the active window.
 * @example startExpirationChecker();
 */
function startExpirationChecker() {
    if (expirationInterval) return;
    expirationInterval = setInterval(async () => {
        const resetCount = await TaskService.resetExpiredTasks();
        if (resetCount > 0 && mainWindow && !mainWindow.isDestroyed()) {
            mainWindow.webContents.send('tasks-auto-reset', resetCount);
        }
    }, TIMER_CONFIG.EXPIRATION_CHECK_MS);
}

/**
 * Stops the periodic expiration checker timer.
 * @example stopExpirationChecker();
 */
function stopExpirationChecker() {
    if (expirationInterval) {
        clearInterval(expirationInterval);
        expirationInterval = null;
    }
}

/**
 * Registers IPC handlers for task entities.
 * @example registerTaskIpcHandlers();
 */
function registerTaskIpcHandlers() {
    ipcMain.handle('get-tasks', async () => await TaskService.getAllTasks());
    ipcMain.handle('add-task', async (event, task) => await TaskService.addTask(task));
    ipcMain.handle('update-task', async (event, task) => await TaskService.updateTask(task));
    ipcMain.handle('delete-task', async (event, id) => await TaskService.deleteTask(id));
}

/**
 * Registers IPC handlers for priority categories.
 * @example registerPriorityIpcHandlers();
 */
function registerPriorityIpcHandlers() {
    ipcMain.handle('get-priorities', async () => {
        return await PriorityRepository.getAll();
    });

    ipcMain.handle('add-priority', async (event, priority) => {
        return await PriorityRepository.insert(priority);
    });

    ipcMain.handle('update-priority', async (event, priority) => {
        return await PriorityRepository.update(priority);
    });

    ipcMain.handle('delete-priority', async (event, id) => {
        return await PriorityRepository.delete(id);
    });
}

app.on('ready', () => {
    createWindow();
    createTray();
    registerTaskIpcHandlers();
    registerPriorityIpcHandlers();
    startExpirationChecker();
    logger.info('App', 'STodo Desktop initialized successfully.');
});

app.on('window-all-closed', () => {
    // Process stays alive in tray until explicit quit
});

app.on('activate', () => {
    showMainWindow();
});

app.on('before-quit', () => {
    isQuitting = true;
    stopExpirationChecker();
});
