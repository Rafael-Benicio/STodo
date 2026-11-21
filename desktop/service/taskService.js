const { TaskRepository } = require('../db/database');
const PriorityService = require('./priorityService');

const TIME_CONFIG = {
    MINUTES_PER_DAY: 1440,
    DEFAULT_TIMESTAMP: 0,
    DEFAULT_MINUTES: 0,
    MIDNIGHT_HOUR: 0,
    MIDNIGHT_MINUTE: 0,
    MIDNIGHT_SECOND: 0,
    MIDNIGHT_MS: 0
};

/**
 * Computes midnight timestamp of the target calendar day.
 * @param {number} autoUncheckMinutes - Duration in minutes.
 * @returns {number} Epoch timestamp at 00:00:00 on target day.
 * @example const ts = getMidnightTimestamp(1440);
 */
function getMidnightTimestamp(autoUncheckMinutes) {
    const days = Math.max(1, Math.round(autoUncheckMinutes / TIME_CONFIG.MINUTES_PER_DAY));
    const target = new Date();
    target.setDate(target.getDate() + days);
    target.setHours(
        TIME_CONFIG.MIDNIGHT_HOUR,
        TIME_CONFIG.MIDNIGHT_MINUTE,
        TIME_CONFIG.MIDNIGHT_SECOND,
        TIME_CONFIG.MIDNIGHT_MS
    );
    return target.getTime();
}

/**
 * Calculates and sets the target uncheck timestamp when a task is completed.
 * @param {Object} task - Target task object.
 * @returns {void}
 * @example calculateUncheckTimestamp(task);
 */
function calculateUncheckTimestamp(task) {
    const isCompleted = task.completed === true || task.completed === 1;
    const hasRenewal = (task.autoUncheckMinutes || 0) > TIME_CONFIG.DEFAULT_MINUTES;
    const isTimestampUnset = !task.uncheckTimestamp || task.uncheckTimestamp === TIME_CONFIG.DEFAULT_TIMESTAMP;

    if (isCompleted && hasRenewal && isTimestampUnset) {
        task.uncheckTimestamp = getMidnightTimestamp(task.autoUncheckMinutes);
        return;
    }
    if (!isCompleted) {
        task.uncheckTimestamp = TIME_CONFIG.DEFAULT_TIMESTAMP;
    }
}

/**
 * TaskService coordinates business rules and database operations for tasks.
 */
const TaskService = {
    /**
     * Retrieves all tasks after automatically resetting any expired completed tasks.
     * @returns {Promise<Array<Object>>} List of tasks.
     * @example const tasks = await TaskService.getAllTasks();
     */
    getAllTasks: async () => {
        await TaskRepository.resetExpiredTasks(Date.now());
        return await TaskRepository.getAll();
    },

    /**
     * Creates and persists a new task entity with calculated metadata.
     * @param {Object} task - Task data to insert.
     * @returns {Promise<void>}
     * @example await TaskService.addTask({ title: 'New Task' });
     */
    addTask: async (task) => {
        const maxPos = await TaskRepository.getMaxPosition();
        task.position = maxPos + 1;
        task.updatedAt = Date.now();
        task.autoUncheckMinutes = task.autoUncheckMinutes || TIME_CONFIG.DEFAULT_MINUTES;
        calculateUncheckTimestamp(task);
        return await TaskRepository.insert(task);
    },

    /**
     * Updates an existing task after computing renewal timestamp and color batch reset.
     * @param {Object} task - Task data to update.
     * @returns {Promise<Object>} Updated task and batch reset count.
     * @example await TaskService.updateTask(task);
     */
    updateTask: async (task) => {
        task.updatedAt = Math.max(Date.now(), (task.updatedAt || 0) + 1);
        calculateUncheckTimestamp(task);
        await TaskRepository.update(task);

        let batchResetCount = 0;
        const isCompleted = task.completed === true || task.completed === 1;
        if (isCompleted && task.priorityId) {
            const priority = await PriorityService.getPriorityById(task.priorityId);
            if (priority && priority.autoResetOnComplete) {
                batchResetCount = await TaskRepository.resetPriorityTasksIfAllCompleted(
                    task.priorityId, 
                    task.updatedAt
                );
            }
        }
        return { task, batchResetCount };
    },

    /**
     * Soft-deletes a task by marking it as deleted.
     * @param {string} id - Task identifier.
     * @returns {Promise<void|null>}
     * @example await TaskService.deleteTask('123');
     */
    deleteTask: async (id) => {
        const task = await TaskRepository.getById(id);
        if (!task) return null;
        task.isDeleted = true;
        task.updatedAt = Math.max(Date.now(), (task.updatedAt || 0) + 1);
        return await TaskRepository.update(task);
    },

    /**
     * Checks and resets any expired tasks whose uncheck time has arrived.
     * @returns {Promise<number>} Number of reset tasks.
     * @example const count = await TaskService.resetExpiredTasks();
     */
    resetExpiredTasks: async () => {
        return await TaskRepository.resetExpiredTasks(Date.now());
    }
};

module.exports = TaskService;
