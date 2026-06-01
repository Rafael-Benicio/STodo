const taskList = document.getElementById('tasks');
const taskInput = document.getElementById('taskTitle');
const addBtn = document.getElementById('addBtn');
const syncStatus = document.getElementById('sync-status');
const taskInputSection = document.querySelector('.task-input');

const tabPending = document.getElementById('tabPending');
const tabCompleted = document.getElementById('tabCompleted');

let currentTab = 'pending'; // 'pending' ou 'completed'
let allTasks = [];

// Carregar tarefas ao iniciar
async function loadTasks() {
    allTasks = await window.api.getTasks();
    renderTasks();
    syncStatus.innerText = 'Servidor rodando na rede local (Porta 8080)';
}

function renderTasks() {
    taskList.innerHTML = '';
    
    // Filtrar tarefas baseada na aba ativa
    const filteredTasks = allTasks.filter(task => {
        if (currentTab === 'pending') return !task.completed;
        return task.completed;
    });

    // Mostrar ou esconder área de input
    if (currentTab === 'pending') {
        taskInputSection.style.display = 'flex';
    } else {
        taskInputSection.style.display = 'none';
    }

    filteredTasks.forEach(task => {
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

    if (filteredTasks.length === 0) {
        const emptyMsg = document.createElement('li');
        emptyMsg.style.justifyContent = 'center';
        emptyMsg.style.color = '#888';
        emptyMsg.innerText = currentTab === 'pending' ? 'Nenhuma tarefa pendente.' : 'Nenhuma tarefa concluída.';
        taskList.appendChild(emptyMsg);
    }
}

// Lógica de troca de abas
tabPending.onclick = () => {
    currentTab = 'pending';
    tabPending.classList.add('active');
    tabCompleted.classList.remove('active');
    renderTasks();
};

tabCompleted.onclick = () => {
    currentTab = 'completed';
    tabCompleted.classList.add('active');
    tabPending.classList.remove('active');
    renderTasks();
};

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
