const { contextBridge, ipcRenderer } = require('electron');

/**
 * Exposes a secure, limited API surface to the renderer process.
 */
contextBridge.exposeInMainWorld('api', {
    getTasks: () => ipcRenderer.invoke('get-tasks'),
    addTask: (task) => ipcRenderer.invoke('add-task', task),
    updateTask: (task) => ipcRenderer.invoke('update-task', task),
    deleteTask: (id) => ipcRenderer.invoke('delete-task', id),
    getPriorities: () => ipcRenderer.invoke('get-priorities'),
    addPriority: (priority) => ipcRenderer.invoke('add-priority', priority),
    updatePriority: (priority) => ipcRenderer.invoke('update-priority', priority),
    deletePriority: (id) => ipcRenderer.invoke('delete-priority', id),
    getTrackers: () => ipcRenderer.invoke('get-trackers'),
    addTracker: (tracker) => ipcRenderer.invoke('add-tracker', tracker),
    updateTracker: (tracker) => ipcRenderer.invoke('update-tracker', tracker),
    deleteTracker: (id) => ipcRenderer.invoke('delete-tracker', id),
    openExternalLink: (url) => ipcRenderer.invoke('open-external-link', url),
    onTasksAutoReset: (callback) => {
        ipcRenderer.on('tasks-auto-reset', (event, count) => callback(count));
    }
});
