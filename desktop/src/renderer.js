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
    
    const filteredTasks = allTasks.filter(task => {
        if (currentTab === 'pending') return !task.completed;
        return task.completed;
    });

    if (currentTab === 'pending') {
        taskInputSection.style.display = 'flex';
    } else {
        taskInputSection.style.display = 'none';
    }

    filteredTasks.forEach((task, index) => {
        const li = document.createElement('li');
        li.draggable = true;
        li.dataset.id = task.id;
        li.dataset.index = index;
        
        // Eventos de Arrastar
        li.addEventListener('dragstart', () => {
            li.classList.add('dragging');
        });

        li.addEventListener('dragend', async () => {
            li.classList.remove('dragging');
            await saveNewOrder();
        });

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
        emptyMsg.draggable = false;
        emptyMsg.style.justifyContent = 'center';
        emptyMsg.style.color = '#888';
        emptyMsg.innerText = currentTab === 'pending' ? 'Nenhuma tarefa pendente.' : 'Nenhuma tarefa concluída.';
        taskList.appendChild(emptyMsg);
    }
}

// Lógica de cálculo de posição durante o arraste
taskList.addEventListener('dragover', e => {
    e.preventDefault();
    const afterElement = getDragAfterElement(taskList, e.clientY);
    const dragging = document.querySelector('.dragging');
    if (afterElement == null) {
        taskList.appendChild(dragging);
    } else {
        taskList.insertBefore(dragging, afterElement);
    }
});

function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('li:not(.dragging)')];

    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

// Salvar a nova ordem no banco de dados
async function saveNewOrder() {
    const listItems = [...taskList.querySelectorAll('li[data-id]')];
    let changed = false;

    for (let i = 0; i < listItems.length; i++) {
        const id = listItems[i].dataset.id;
        const task = allTasks.find(t => t.id === id);
        
        // Se a posição mudou, atualizamos
        if (task && task.position !== i) {
            task.position = i;
            await window.api.updateTask(task);
            changed = true;
        }
    }

    if (changed) {
        console.log('[Order] Nova ordem salva e sincronizada.');
        // Não chamamos loadTasks() aqui para evitar flicker, 
        // já que o DOM já está na ordem certa.
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
