const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
    // Aqui adicionaremos as pontes para o banco de dados e sincronização
    getTasks: () => ipcRenderer.invoke('get-tasks'),
    addTask: (task) => ipcRenderer.invoke('add-task', task),
    updateTask: (task) => ipcRenderer.invoke('update-task', task),
    deleteTask: (id) => ipcRenderer.invoke('delete-task', id),
    onRefreshTasks: (callback) => ipcRenderer.on('refresh-tasks', callback)
});
