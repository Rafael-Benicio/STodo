const { PriorityRepository } = require('../db/database');

/**
 * PriorityService coordinates business rules and repository operations for priorities.
 */
const PriorityService = {
    /**
     * Retrieves all configured priorities ordered by manual position.
     * @returns {Promise<Array<Object>>} List of priorities.
     * @example const list = await PriorityService.getAllPriorities();
     */
    getAllPriorities: async () => {
        return await PriorityRepository.getAll();
    },

    /**
     * Finds a priority by its unique identifier.
     * @param {string} id - Priority identifier.
     * @returns {Promise<Object|null>} Found priority or null.
     * @example const p = await PriorityService.getPriorityById('uuid');
     */
    getPriorityById: async (id) => {
        return await PriorityRepository.getById(id);
    },

    /**
     * Persists a new priority category.
     * @param {Object} priority - Priority entity data.
     * @returns {Promise<void>}
     * @example await PriorityService.addPriority({ name: 'High', color: '#ff0000' });
     */
    addPriority: async (priority) => {
        return await PriorityRepository.insert(priority);
    },

    /**
     * Updates an existing priority category.
     * @param {Object} priority - Priority entity data.
     * @returns {Promise<void>}
     * @example await PriorityService.updatePriority(priority);
     */
    updatePriority: async (priority) => {
        return await PriorityRepository.update(priority);
    },

    /**
     * Deletes a priority category and unlinks associated tasks.
     * @param {string} id - Priority identifier.
     * @returns {Promise<void>}
     * @example await PriorityService.deletePriority('uuid');
     */
    deletePriority: async (id) => {
        return await PriorityRepository.delete(id);
    }
};

module.exports = PriorityService;
