const UI_CONFIG = {
    TABS: { PENDING: 'pending', COMPLETED: 'completed', PRIORITIES: 'priorities' },
    CLASS_DRAGGING: 'dragging',
    STORAGE_KEYS: { THEME: 'stodo_theme', ACTIVE_TAB: 'stodo_active_tab' },
    THEMES: { DARK: 'dark-mode', LIGHT: 'light-mode' }
};

const COLOR_CONFIG = {
    DEFAULT: '#6200ee',
    OPACITY_HEX: '22',
    GRADIENT_STOP: '45%'
};

const taskList = document.getElementById('tasks');
const taskInput = document.getElementById('taskTitle');
const addBtn = document.getElementById('addBtn');
const themeToggle = document.getElementById('themeToggle');
const taskInputSection = document.getElementById('taskInputSection');
const taskListSection = document.getElementById('taskListSection');
const priorityInputSection = document.getElementById('priorityInputSection');
const priorityListSection = document.getElementById('priorityListSection');

const tabPending = document.getElementById('tabPending');
const tabCompleted = document.getElementById('tabCompleted');
const tabPriorities = document.getElementById('tabPriorities');
const sortActionsSelect = document.getElementById('sortActionsSelect');
const addPriorityBtn = document.getElementById('addPriorityBtn');

let currentTab = UI_CONFIG.TABS.PENDING;
let allTasks = [];

/**
 * Loads and applies the saved theme preference from storage.
 * @example initializeTheme();
 */
function initializeTheme() {
    const savedTheme = localStorage.getItem(UI_CONFIG.STORAGE_KEYS.THEME);
    if (savedTheme === UI_CONFIG.THEMES.DARK) {
        document.body.classList.add(UI_CONFIG.THEMES.DARK);
        themeToggle.innerText = '☀️';
    }
}

/**
 * Toggles the application theme between light and dark modes.
 * @example toggleTheme();
 */
function toggleTheme() {
    const isDarkMode = document.body.classList.toggle(UI_CONFIG.THEMES.DARK);
    themeToggle.innerText = isDarkMode ? '☀️' : '🌙';
    localStorage.setItem(
        UI_CONFIG.STORAGE_KEYS.THEME, 
        isDarkMode ? UI_CONFIG.THEMES.DARK : UI_CONFIG.THEMES.LIGHT
    );
}

/**
 * Loads all tasks from the database and triggers a UI re-render.
 * @example await loadTasks();
 */
async function loadTasks() {
    allTasks = await window.api.getTasks();
    if (currentTab === UI_CONFIG.TABS.PRIORITIES) {
        renderPriorities();
        return;
    }
    renderTasks();
}

/**
 * Creates an individual task list item (LI) with event listeners.
 * @param {Object} task - The task object to render.
 * @returns {HTMLElement} The created list item element.
 * @example const li = createTaskElement(task);
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
 * Attaches HTML5 drag and drop event listeners to a task item.
 * @param {HTMLElement} li - The list item element.
 * @example attachDragEvents(li);
 */
function attachDragEvents(li) {
    li.addEventListener('dragstart', () => li.classList.add(UI_CONFIG.CLASS_DRAGGING));
    li.addEventListener('dragend', async () => {
        li.classList.remove(UI_CONFIG.CLASS_DRAGGING);
        await saveNewOrder();
    });
}

/**
 * Applies or resets custom color accent border and gradient styling.
 * @param {HTMLElement} li - The task item element.
 * @param {string|null} color - The hex color code or null.
 * @example applyTaskColorStyle(li, '#ff0000');
 */
function applyTaskColorStyle(li, color) {
    if (!color) {
        li.style.borderLeft = '';
        li.style.background = '';
        return;
    }
    li.style.borderLeft = `6px solid ${color}`;
    li.style.background = `linear-gradient(90deg, ${color}${COLOR_CONFIG.OPACITY_HEX} 0%, var(--surface-color) ${COLOR_CONFIG.GRADIENT_STOP})`;
}

/**
 * Assigns a chosen priority to a task and updates UI styling.
 * @param {Object} task - Target task.
 * @param {string|null} priorityId - Selected priority ID.
 * @param {HTMLElement} li - Task list item element.
 * @example await assignPriorityToTask(task, 'p-1', li);
 */
async function assignPriorityToTask(task, priorityId, li) {
    task.priorityId = priorityId;
    const found = allPriorities.find(p => p.id === priorityId);
    task.color = found ? found.color : null;
    applyTaskColorStyle(li, task.color);
    await window.api.updateTask(task);
    await loadTasks();
}

/**
 * Attaches change listener to the task's priority select element.
 * @param {HTMLSelectElement} select - The select element.
 * @param {Object} task - The task data.
 * @param {HTMLElement} li - The list item.
 * @example attachPrioritySelectEvent(select, task, li);
 */
function attachPrioritySelectEvent(select, task, li) {
    select.onchange = async (e) => {
        const priorityId = e.target.value || null;
        await assignPriorityToTask(task, priorityId, li);
    };
}

/**
 * Creates reset button to clear priority and custom color from task.
 * @param {Object} task - Current task.
 * @param {HTMLElement} li - Task container element.
 * @param {HTMLSelectElement} select - Select element to reset.
 * @returns {HTMLButtonElement|null} Reset button or null.
 * @example const btn = createPriorityResetButton(task, li, select);
 */
function createPriorityResetButton(task, li, select) {
    if (!task.priorityId && !task.color) return null;
    const resetBtn = document.createElement('button');
    resetBtn.type = 'button';
    resetBtn.className = 'color-reset-btn';
    resetBtn.innerText = '✕';
    resetBtn.title = 'Remover prioridade';
    resetBtn.onclick = async (e) => {
        e.stopPropagation();
        select.value = '';
        await assignPriorityToTask(task, null, li);
    };
    return resetBtn;
}

/**
 * Builds priority controls for a task: priority select dropdown and reset button.
 * @param {Object} task - The task data.
 * @param {HTMLElement} li - The list item element.
 * @returns {HTMLElement} Container with priority selection controls.
 * @example const controls = createTaskPriorityControls(task, li);
 */
function createTaskPriorityControls(task, li) {
    const container = document.createElement('div');
    container.className = 'color-controls';

    const select = document.createElement('select');
    select.className = 'priority-select';
    select.title = 'Vincular prioridade à tarefa';

    populatePrioritySelectOptions(select, task.priorityId);
    attachPrioritySelectEvent(select, task, li);

    container.appendChild(select);
    const resetBtn = createPriorityResetButton(task, li, select);
    if (resetBtn) container.appendChild(resetBtn);

    return container;
}

/**
 * Creates checkbox input element for task completion state.
 * @param {Object} task - The task object.
 * @returns {HTMLInputElement} The checkbox element.
 * @example const checkbox = createTaskCheckbox(task);
 */
function createTaskCheckbox(task) {
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = task.completed;
    checkbox.addEventListener('change', async () => {
        task.completed = checkbox.checked;
        if (task.completed) task.completionCount = (task.completionCount || 0) + 1;
        await window.api.updateTask(task);
        loadTasks();
    });
    return checkbox;
}

/**
 * Creates the span element representing the task title.
 * @param {Object} task - The task object.
 * @returns {HTMLSpanElement} The span element.
 * @example const titleSpan = createTaskTitle(task);
 */
function createTaskTitle(task) {
    const span = document.createElement('span');
    span.className = 'task-title' + (task.completed ? ' completed' : '');
    span.innerText = task.title;
    return span;
}

/**
 * Creates the badge indicating task completion count.
 * @param {Object} task - The task object.
 * @returns {HTMLSpanElement} The badge element.
 * @example const badge = createCompletionBadge(task);
 */
function createCompletionBadge(task) {
    const countBadge = document.createElement('span');
    countBadge.className = 'badge-count';
    countBadge.innerText = `✓ ${task.completionCount || 0}`;
    return countBadge;
}

/**
 * Creates the options button that opens the task configuration modal.
 * @param {Object} task - The task object.
 * @param {HTMLElement} li - The parent list item element.
 * @returns {HTMLButtonElement} The created options button.
 * @example const btn = createTaskOptionsButton(task, li);
 */
function createTaskOptionsButton(task, li) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'options-btn';
    btn.innerText = '⚙️';
    btn.title = 'Opções da tarefa';
    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        openTaskOptionsModal(task, li);
    });
    return btn;
}

/**
 * Appends interactive task contents (checkbox, title, badges, options button).
 * @param {HTMLElement} li - The parent list item element.
 * @param {Object} task - The task object data.
 * @example appendTaskContent(li, task);
 */
function appendTaskContent(li, task) {
    applyTaskColorStyle(li, task.color);

    const actionsDiv = document.createElement('div');
    actionsDiv.className = 'task-actions';
    actionsDiv.appendChild(createTaskOptionsButton(task, li));

    li.appendChild(createTaskCheckbox(task));
    li.appendChild(createTaskTitle(task));
    const renewalBadge = createRenewalBadge(task);
    if (renewalBadge) li.appendChild(renewalBadge);
    li.appendChild(createCompletionBadge(task));
    li.appendChild(actionsDiv);
}

/**
 * Saves reordered task positions to the database following drag-and-drop.
 * @example await saveNewOrder();
 */
async function saveNewOrder() {
    const items = [...taskList.querySelectorAll('li')];
    for (let i = 0; i < items.length; i++) {
        const id = items[i].dataset.id;
        const task = allTasks.find(t => t.id === id);
        if (task) {
            task.position = i;
            await window.api.updateTask(task);
        }
    }
}

/**
 * Renders filtered tasks according to the active tab (Pending vs Completed).
 * @example renderTasks();
 */
function renderTasks() {
    taskList.innerHTML = '';
    const isCompletedTab = currentTab === UI_CONFIG.TABS.COMPLETED;
    const filteredTasks = allTasks.filter(t => t.completed === isCompletedTab);

    if (filteredTasks.length === 0) {
        taskList.innerHTML = `<li class="empty-state">${isCompletedTab ? 'Nenhuma tarefa concluída.' : 'Nenhuma tarefa pendente!'}</li>`;
        return;
    }

    filteredTasks.forEach(task => {
        taskList.appendChild(createTaskElement(task));
    });
}

/**
 * Adjusts container visibility based on the active tab.
 * @param {boolean} isPriorities - Whether the priorities tab is selected.
 * @param {boolean} isPending - Whether the pending tab is selected.
 * @example updateTabContainers(false, true);
 */
function updateTabContainers(isPriorities, isPending) {
    taskInputSection.style.display = isPending ? 'flex' : 'none';
    taskListSection.style.display = isPriorities ? 'none' : 'block';
    priorityInputSection.style.display = isPriorities ? 'flex' : 'none';
    priorityListSection.style.display = isPriorities ? 'block' : 'none';
    sortActionsSelect.style.display = isPriorities ? 'none' : 'inline-block';
}

/**
 * Shifts tabs between Pending, Completed, and Priorities views.
 * @param {string} tabName - Target tab ('pending', 'completed', or 'priorities').
 * @example switchTab('priorities');
 */
function switchTab(tabName) {
    currentTab = tabName;
    sessionStorage.setItem(UI_CONFIG.STORAGE_KEYS.ACTIVE_TAB, tabName);
    const isPending = tabName === UI_CONFIG.TABS.PENDING;
    const isCompleted = tabName === UI_CONFIG.TABS.COMPLETED;
    const isPriorities = tabName === UI_CONFIG.TABS.PRIORITIES;

    tabPending.classList.toggle('active', isPending);
    tabCompleted.classList.toggle('active', isCompleted);
    tabPriorities.classList.toggle('active', isPriorities);

    updateTabContainers(isPriorities, isPending);
    if (isPriorities) {
        renderPriorities();
        return;
    }
    renderTasks();
}

/**
 * Randomizes order of currently pending tasks.
 * @example await shufflePendingTasks();
 */
async function shufflePendingTasks() {
    const pendingTasks = allTasks.filter(t => !t.completed);
    for (let i = pendingTasks.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [pendingTasks[i], pendingTasks[j]] = [pendingTasks[j], pendingTasks[i]];
    }
    const updates = pendingTasks.map((task, index) => {
        task.position = index;
        return window.api.updateTask(task);
    });
    await Promise.all(updates);
}

/**
 * Sorts all tasks by their completion count in descending order.
 * @example await sortTasksByChecks();
 */
async function sortTasksByChecks() {
    allTasks.sort((a, b) => {
        if (b.completionCount !== a.completionCount) {
            return b.completionCount - a.completionCount;
        }
        return a.position - b.position;
    });
    const updates = allTasks.map((task, index) => {
        task.position = index;
        return window.api.updateTask(task);
    });
    await Promise.all(updates);
}

/**
 * Compares two tasks by their assigned color to group them together.
 * @param {Object} a - First task entity.
 * @param {Object} b - Second task entity.
 * @returns {number} Comparison sorting order.
 * @example const order = compareTasksByColor(task1, task2);
 */
function compareTasksByColor(a, b) {
    const colorA = a.color || '';
    const colorB = b.color || '';
    if (!colorA && colorB) return 1;
    if (colorA && !colorB) return -1;
    if (colorA !== colorB) return colorA.localeCompare(colorB);
    return a.position - b.position;
}

/**
 * Reorders tasks grouping them by their assigned color and persists order.
 * @example await groupTasksByColor();
 */
async function groupTasksByColor() {
    allTasks.sort(compareTasksByColor);
    const updates = allTasks.map((task, index) => {
        task.position = index;
        return window.api.updateTask(task);
    });
    await Promise.all(updates);
}

/**
 * Handles selection from the sort actions dropdown menu.
 * @param {Event} e - Change event.
 * @example await handleSortActionChange(event);
 */
async function handleSortActionChange(e) {
    const action = e.target.value;
    if (!action) return;
    if (action === 'shuffle') await shufflePendingTasks();
    if (action === 'checks') await sortTasksByChecks();
    if (action === 'color') await groupTasksByColor();
    window.location.reload();
}

/**
 * Adds a new task to the list from the input field.
 * @example await handleAddTask();
 */
async function handleAddTask() {
    const title = taskInput.value.trim();
    if (!title) return;
    const renewalSelect = document.getElementById('taskRenewalSelect');
    const autoUncheckMinutes = renewalSelect ? parseInt(renewalSelect.value, 10) || 0 : 0;
    await window.api.addTask({ title, completed: false, autoUncheckMinutes });
    taskInput.value = '';
    loadTasks();
}

// Event Listeners setup
themeToggle.onclick = toggleTheme;
tabPending.onclick = () => switchTab(UI_CONFIG.TABS.PENDING);
tabCompleted.onclick = () => switchTab(UI_CONFIG.TABS.COMPLETED);
tabPriorities.onclick = () => switchTab(UI_CONFIG.TABS.PRIORITIES);
sortActionsSelect.onchange = handleSortActionChange;
addBtn.onclick = handleAddTask;
addPriorityBtn.onclick = handleAddPriority;
taskInput.onkeypress = (e) => { if (e.key === 'Enter') handleAddTask(); };
document.getElementById('priorityName').onkeypress = (e) => {
    if (e.key === 'Enter') handleAddPriority();
};

// Drag over handling for reordering
taskList.addEventListener('dragover', (e) => {
    e.preventDefault();
    const draggingItem = document.querySelector(`.${UI_CONFIG.CLASS_DRAGGING}`);
    if (!draggingItem) return;
    const siblings = [...taskList.querySelectorAll(`li:not(.${UI_CONFIG.CLASS_DRAGGING})`)];
    const nextSibling = siblings.find(sibling => {
        return e.clientY <= sibling.offsetTop + sibling.offsetHeight / 2;
    });
    taskList.insertBefore(draggingItem, nextSibling);
});

/**
 * Bootstraps the application, restoring state and rendering initial views.
 * @example await initializeApp();
 */
async function initializeApp() {
    initializeTheme();
    initTaskModal();
    await loadPriorities();
    await loadTasks();
    if (window.api.onTasksAutoReset) {
        window.api.onTasksAutoReset(() => loadTasks());
    }
    const savedTab = sessionStorage.getItem(UI_CONFIG.STORAGE_KEYS.ACTIVE_TAB);
    if (savedTab) switchTab(savedTab);
}

initializeApp();
