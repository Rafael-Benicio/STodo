/**
 * Priorities UI module: manages priority state and UI rendering.
 */
let allPriorities = [];

/**
 * Loads all priorities from the database.
 * @example await loadPriorities();
 */
async function loadPriorities() {
    const priorities = await window.api.getPriorities();
    allPriorities = priorities || [];
    return allPriorities;
}

/**
 * Fills a priority dropdown select element with existing priority choices.
 * @param {HTMLSelectElement} select - Target select element.
 * @param {string|null} currentPriorityId - Currently selected priority id.
 * @example populatePrioritySelectOptions(select, 'p-1');
 */
function populatePrioritySelectOptions(select, currentPriorityId) {
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.innerText = 'Sem prioridade';
    defaultOption.selected = !currentPriorityId;
    select.appendChild(defaultOption);

    for (const priority of allPriorities) {
        const option = document.createElement('option');
        option.value = priority.id;
        option.innerText = `● ${priority.name}`;
        option.selected = currentPriorityId === priority.id;
        select.appendChild(option);
    }
}

/**
 * Creates color input for a priority item, updating linked tasks reactively.
 * @param {Object} priority - The priority data.
 * @param {HTMLElement} li - The parent list item.
 * @returns {HTMLInputElement} The color input element.
 * @example const input = createPriorityColorInput(priority, li);
 */
function createPriorityColorInput(priority, li) {
    const input = document.createElement('input');
    input.type = 'color';
    input.className = 'priority-color-picker';
    input.value = priority.color;
    input.title = 'Alterar cor da prioridade';
    input.onchange = async (e) => {
        priority.color = e.target.value;
        li.style.borderLeft = `6px solid ${priority.color}`;
        await window.api.updatePriority(priority);
        await loadTasks();
        await loadPriorities();
    };
    return input;
}

/**
 * Creates delete button for a priority item in the list.
 * @param {string} priorityId - Identifier of priority to delete.
 * @returns {HTMLButtonElement} Delete button element.
 * @example const btn = createPriorityDeleteButton('p-1');
 */
function createPriorityDeleteButton(priorityId) {
    const btn = document.createElement('button');
    btn.className = 'delete-btn';
    btn.innerText = '🗑️';
    btn.title = 'Excluir prioridade';
    btn.onclick = async () => {
        await window.api.deletePriority(priorityId);
        await loadPriorities();
        await loadTasks();
    };
    return btn;
}

/**
 * Creates count badge for a given priority indicating associated tasks.
 * @param {string} priorityId - Target priority id.
 * @returns {HTMLElement} The created badge element.
 * @example const badge = createPriorityBadge('p-1');
 */
function createPriorityBadge(priorityId) {
    const count = allTasks.filter(t => t.priorityId === priorityId).length;
    const countBadge = document.createElement('span');
    countBadge.className = 'badge-count';
    countBadge.innerText = `${count} tarefa(s)`;
    return countBadge;
}

/**
 * Creates auto-reset checkbox enabling automatic uncheck when all tasks are complete.
 * @param {Object} priority - Target priority entity.
 * @returns {HTMLLabelElement} The created label containing checkbox.
 * @example const label = createPriorityResetCheckbox(priority);
 */
function createPriorityResetCheckbox(priority) {
    const label = document.createElement('label');
    label.className = 'priority-reset-label';
    label.title = 'Desmarcar todas automaticamente ao concluir todas as tarefas desta cor';

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'priority-reset-checkbox';
    checkbox.checked = Boolean(priority.autoResetOnComplete);
    checkbox.onchange = async () => {
        priority.autoResetOnComplete = checkbox.checked;
        await window.api.updatePriority(priority);
    };

    label.append(checkbox, document.createTextNode('Reset ao completar todas'));
    return label;
}

/**
 * Creates single priority list item for the priorities tab.
 * @param {Object} priority - Priority entity.
 * @returns {HTMLElement} The created list item.
 * @example const li = createPriorityElement(priority);
 */
function createPriorityElement(priority) {
    const li = document.createElement('li');
    li.className = 'priority-item';
    li.style.borderLeft = `6px solid ${priority.color}`;

    const colorInput = createPriorityColorInput(priority, li);
    const nameSpan = document.createElement('span');
    nameSpan.className = 'priority-name';
    nameSpan.innerText = priority.name;

    li.appendChild(colorInput);
    li.appendChild(nameSpan);
    li.appendChild(createPriorityResetCheckbox(priority));
    li.appendChild(createPriorityBadge(priority.id));
    li.appendChild(createPriorityDeleteButton(priority.id));
    return li;
}

/**
 * Renders the priorities list in the priorities tab.
 * @example renderPriorities();
 */
function renderPriorities() {
    const list = document.getElementById('priorities');
    list.innerHTML = '';
    if (allPriorities.length === 0) {
        list.innerHTML = '<li class="empty-state">Nenhuma prioridade cadastrada. Crie uma acima!</li>';
        return;
    }
    allPriorities.forEach(priority => {
        list.appendChild(createPriorityElement(priority));
    });
}

/**
 * Adds a new priority category from the priority input form.
 * @example await handleAddPriority();
 */
async function handleAddPriority() {
    const nameInput = document.getElementById('priorityName');
    const colorInput = document.getElementById('priorityColor');
    const name = nameInput.value.trim();
    if (!name) return;

    await window.api.addPriority({
        name,
        color: colorInput.value || COLOR_CONFIG.DEFAULT
    });
    nameInput.value = '';
    await loadPriorities();
    renderPriorities();
}
