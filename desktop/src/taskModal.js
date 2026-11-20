/**
 * Task modal module: manages the task configuration dialog.
 */
let currentModalTask = null;

/**
 * Builds the delete action button for the task modal.
 * @param {Object} task - Target task entity.
 * @returns {HTMLButtonElement} Configured delete button.
 * @example const btn = buildModalDeleteButton(task);
 */
function buildModalDeleteButton(task) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'modal-delete-btn';
    btn.innerText = '🗑️ Excluir Tarefa';
    btn.onclick = async () => {
        closeTaskOptionsModal();
        await window.api.deleteTask(task.id);
        loadTasks();
    };
    return btn;
}

/**
 * Closes the task configuration modal dialog.
 * @example closeTaskOptionsModal();
 */
function closeTaskOptionsModal() {
    const modal = document.getElementById('taskModal');
    if (!modal) return;
    modal.style.display = 'none';
    currentModalTask = null;
}

/**
 * Opens and populates the task options modal for a specific task.
 * @param {Object} task - Target task object.
 * @param {HTMLElement} li - The parent list item element.
 * @example openTaskOptionsModal(task, li);
 */
function openTaskOptionsModal(task, li) {
    currentModalTask = task;
    const modal = document.getElementById('taskModal');
    const titleHeader = document.getElementById('modalTaskTitle');
    const renewalBox = document.getElementById('modalRenewalContainer');
    const priorityBox = document.getElementById('modalPriorityContainer');
    const deleteBox = document.getElementById('modalDeleteContainer');

    titleHeader.innerText = task.title;
    renewalBox.innerHTML = '';
    renewalBox.appendChild(createTaskRenewalSelect(task));

    priorityBox.innerHTML = '';
    priorityBox.appendChild(createTaskPriorityControls(task, li));

    deleteBox.innerHTML = '';
    deleteBox.appendChild(buildModalDeleteButton(task));

    modal.style.display = 'flex';
}

/**
 * Initializes modal event listeners for closing upon overlay click or Esc key.
 * @example initTaskModal();
 */
function initTaskModal() {
    const modal = document.getElementById('taskModal');
    const closeBtn = document.getElementById('modalCloseBtn');
    if (!modal) return;

    closeBtn.onclick = closeTaskOptionsModal;
    modal.onclick = (e) => {
        if (e.target === modal) closeTaskOptionsModal();
    };
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeTaskOptionsModal();
    });
}
