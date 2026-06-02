const UI_CONFIG = {
    PORT: 8080,
    TABS: { PENDING: 'pending', COMPLETED: 'completed' },
    CLASS_DRAGGING: 'dragging',
    SYNC_STATUS_TEXT: 'Sync Server running on LAN (Port 8080)',
    STORAGE_KEYS: { THEME: 'stodo_theme' },
    THEMES: { DARK: 'dark-mode', LIGHT: 'light-mode' }
};

const taskList = document.getElementById('tasks');
const taskInput = document.getElementById('taskTitle');
const addBtn = document.getElementById('addBtn');
const themeToggle = document.getElementById('themeToggle');
const syncStatus = document.getElementById('sync-status');
const taskInputSection = document.querySelector('.task-input');

const tabPending = document.getElementById('tabPending');
const tabCompleted = document.getElementById('tabCompleted');

let currentTab = UI_CONFIG.TABS.PENDING;
let allTasks = [];

/**
 * Loads and applies the saved theme preference.
 */
function initializeTheme() {
    const savedTheme = localStorage.getItem(UI_CONFIG.STORAGE_KEYS.THEME);
    if (savedTheme === UI_CONFIG.THEMES.DARK) {
        document.body.classList.add('dark-mode');
        themeToggle.innerText = '☀️';
    }
}

/**
 * Toggles the application theme between light and dark modes.
 */
function toggleTheme() {
    const isDarkMode = document.body.classList.toggle('dark-mode');
    themeToggle.innerText = isDarkMode ? '☀️' : '🌙';
    localStorage.setItem(
        UI_CONFIG.STORAGE_KEYS.THEME, 
        isDarkMode ? UI_CONFIG.THEMES.DARK : UI_CONFIG.THEMES.LIGHT
    );
}

/**
 * Loads all tasks from the local database and triggers a UI refresh.
 */
async function loadTasks() {
    allTasks = await window.api.getTasks();
    renderTasks();
    syncStatus.innerText = UI_CONFIG.SYNC_STATUS_TEXT;
}

/**
 * Creates an individual task list item (LI) with event listeners.
 * @param {Object} task - The task object to render.
 */
function createTaskElement(task) {
    const li = document.createElement('li');
    li.draggable = true;
    li.dataset.id = task.id;
    
    attachDragEvents(li);
    appendTaskContent(li, task);
    
    return li;
}

/**
 * Attaches drag and drop event listeners to a list item.
 * @param {HTMLElement} li - The list item element.
 */
function attachDragEvents(li) {
    li.addEventListener('dragstart', () => li.classList.add(UI_CONFIG.CLASS_DRAGGING));
    li.addEventListener('dragend', async () => {
        li.classList.remove(UI_CONFIG.CLASS_DRAGGING);
        await saveNewOrder();
    });
}

/**
 * Appends checkbox, title, and delete button to the task element.
 * @param {HTMLElement} li - The container element.
 * @param {Object} task - The task data.
 */
function appendTaskContent(li, task) {
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
}

/**
 * Renders the filtered task list based on the currently active tab.
 */
function renderTasks() {
    taskList.innerHTML = '';
    
    const filtered = allTasks.filter(t => 
        currentTab === UI_CONFIG.TABS.PENDING ? !t.completed : t.completed);

    taskInputSection.style.display = currentTab === UI_CONFIG.TABS.PENDING ? 'flex' : 'none';

    filtered.forEach(t => taskList.appendChild(createTaskElement(t)));

    if (filtered.length === 0) appendEmptyState();
}

/**
 * Appends a message when the current task list is empty.
 */
function appendEmptyState() {
    const emptyMsg = document.createElement('li');
    emptyMsg.style.justifyContent = 'center';
    emptyMsg.style.color = '#888';
    emptyMsg.innerText = currentTab === UI_CONFIG.TABS.PENDING ? 
        'No pending tasks.' : 'No completed tasks.';
    taskList.appendChild(emptyMsg);
}

/**
 * Calculates which element the dragged item should be inserted after.
 */
function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('li:not(.dragging)')];
    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) return { offset, element: child };
        return closest;
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

/**
 * Saves the new manual order of tasks to the database.
 */
async function saveNewOrder() {
    const listItems = [...taskList.querySelectorAll('li[data-id]')];
    for (let i = 0; i < listItems.length; i++) {
        const task = allTasks.find(t => t.id === listItems[i].dataset.id);
        if (task && task.position !== i) {
            task.position = i;
            await window.api.updateTask(task);
        }
    }
}

// Global UI Listeners
taskList.addEventListener('dragover', e => {
    e.preventDefault();
    const afterElement = getDragAfterElement(taskList, e.clientY);
    const dragging = document.querySelector('.' + UI_CONFIG.CLASS_DRAGGING);
    if (afterElement == null) taskList.appendChild(dragging);
    else taskList.insertBefore(dragging, afterElement);
});

tabPending.onclick = () => {
    currentTab = UI_CONFIG.TABS.PENDING;
    tabPending.classList.add('active');
    tabCompleted.classList.remove('active');
    renderTasks();
};

tabCompleted.onclick = () => {
    currentTab = UI_CONFIG.TABS.COMPLETED;
    tabCompleted.classList.add('active');
    tabPending.classList.remove('active');
    renderTasks();
};

themeToggle.onclick = () => toggleTheme();

addBtn.onclick = async () => {
    const title = taskInput.value.trim();
    if (title) {
        await window.api.addTask({ title, completed: false });
        taskInput.value = '';
        loadTasks();
    }
};

taskInput.onkeypress = (e) => { if (e.key === 'Enter') addBtn.click(); };

if (window.api.onRefreshTasks) {
    window.api.onRefreshTasks(() => loadTasks());
}

// Start application
initializeTheme();
loadTasks();
