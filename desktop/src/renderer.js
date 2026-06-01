const taskList = document.getElementById('tasks');
const taskInput = document.getElementById('taskTitle');
const addBtn = document.getElementById('addBtn');
const syncStatus = document.getElementById('sync-status');

// Carregar tarefas ao iniciar
async function loadTasks() {
    const tasks = await window.api.getTasks();
    renderTasks(tasks);
    syncStatus.innerText = 'Servidor rodando na rede local (Porta 8080)';
}

function renderTasks(tasks) {
    taskList.innerHTML = '';
    tasks.forEach(task => {
        const li = document.createElement('li');
        
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = task.completed;
        checkbox.onchange = async () => {
            task.completed = checkbox.checked;
            await window.api.updateTask(task);
            loadTasks();
        };

        const span = document.createElement('span');
        span.className = 'task-title' + (task.completed ? ' completed' : '');
        span.innerText = task.title;

        const deleteBtn = document.createElement('button');
        deleteBtn.innerText = 'Excluir';
        deleteBtn.className = 'delete-btn';
        deleteBtn.onclick = async () => {
            await window.api.deleteTask(task.id);
            loadTasks();
        };

        li.appendChild(checkbox);
        li.appendChild(span);
        li.appendChild(deleteBtn);
        taskList.appendChild(li);
    });
}

addBtn.onclick = async () => {
    const title = taskInput.value.trim();
    if (title) {
        await window.api.addTask({
            title: title,
            completed: false
        });
        taskInput.value = '';
        loadTasks();
    }
};

taskInput.onkeypress = (e) => {
    if (e.key === 'Enter') {
        addBtn.click();
    }
};

// Escutar notificações de sincronização em tempo real do Hub
if (window.api.onRefreshTasks) {
    window.api.onRefreshTasks(() => {
        console.log('[Sync] Dados atualizados via celular!');
        loadTasks();
    });
}

loadTasks();
