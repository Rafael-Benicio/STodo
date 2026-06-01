const { app, BrowserWindow, ipcMain, Menu, Tray, nativeImage } = require('electron');
const path = require('path');
const startSyncServer = require('./server/sync-server');
const startMdnsService = require('./server/mdns-service');
const TaskRepository = require('./db/database');

let mainWindow;
let tray;
let isQuitting = false;

function createWindow() {
    Menu.setApplicationMenu(null);

    mainWindow = new BrowserWindow({
        width: 1000,
        height: 700,
        autoHideMenuBar: true,
        icon: path.join(__dirname, 'icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'src/index.html'));

    // Interceptar o evento de fechamento
    mainWindow.on('close', function (event) {
        if (!isQuitting) {
            event.preventDefault();
            mainWindow.hide(); // Apenas esconde a janela
        }
        return false;
    });

    mainWindow.on('closed', function () {
        mainWindow = null;
    });
}

function createTray() {
    const iconPath = path.join(__dirname, 'icon.png');
    const icon = nativeImage.createFromPath(iconPath);
    tray = new Tray(icon.resize({ width: 16, height: 16 }));

    const contextMenu = Menu.buildFromTemplate([
        { 
            label: 'Abrir STodo', 
            click: () => {
                if (mainWindow) mainWindow.show();
                else createWindow();
            } 
        },
        { type: 'separator' },
        { 
            label: 'Sair', 
            click: () => {
                isQuitting = true;
                app.quit();
            } 
        }
    ]);

    tray.setToolTip('STodo Desktop Hub');
    tray.setContextMenu(contextMenu);

    // Restaurar ao clicar no ícone
    tray.on('double-click', () => {
        if (mainWindow) mainWindow.show();
        else createWindow();
    });
}

// Handlers para IPC (UI -> DB)
ipcMain.handle('get-tasks', async () => {
    return await TaskRepository.getAll();
});

ipcMain.handle('add-task', async (event, task) => {
    const maxPos = await TaskRepository.getMaxPosition();
    task.position = maxPos + 1;
    task.updatedAt = Date.now();
    return await TaskRepository.insert(task);
});

ipcMain.handle('update-task', async (event, task) => {
    task.updatedAt = Date.now();
    return await TaskRepository.update(task);
});

ipcMain.handle('delete-task', async (event, id) => {
    const task = await TaskRepository.getById(id);
    if (task) {
        task.isDeleted = true;
        task.updatedAt = Date.now();
        return await TaskRepository.update(task);
    }
});

app.on('ready', () => {
    createWindow();
    createTray();
    
    const PORT = 8080;
    startSyncServer(PORT, () => {
        if (mainWindow) {
            mainWindow.webContents.send('refresh-tasks');
        }
    });
    startMdnsService(PORT);
    
    console.log('STodo Desktop Hub inicializado em segundo plano.');
});

// Impede que o app feche quando a janela for escondida
app.on('window-all-closed', function () {
    if (process.platform !== 'darwin') {
        // Não fazemos nada para manter o processo rodando no tray
    }
});

app.on('activate', function () {
    if (mainWindow === null) {
        createWindow();
    } else {
        mainWindow.show();
    }
});

// Antes de fechar realmente, garantir que tudo pare
app.on('before-quit', () => {
    isQuitting = true;
});
