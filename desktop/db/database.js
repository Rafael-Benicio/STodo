const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const crypto = require('crypto');

/**
 * Constants for database configuration and field states to avoid magic strings and numbers.
 */
const DB_CONFIG = {
    FILE_NAME: 'stodo_desktop.db',
    TABLE_TASKS: 'tasks',
    TABLE_PRIORITIES: 'priorities',
    STATUS_DELETED: 1,
    STATUS_ACTIVE: 0,
    DEFAULT_MINUTES: 0,
    DEFAULT_TIMESTAMP: 0,
    DEFAULT_POSITION: 0,
    DEFAULT_COMPLETIONS: 0,
    DEFAULT_PRIORITY_COLOR: '#6200ee'
};

const dbPath = path.join(__dirname, DB_CONFIG.FILE_NAME);
const db = new sqlite3.Database(dbPath);

/**
 * Initialize the database schema and columns if they do not exist.
 */
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS ${DB_CONFIG.TABLE_TASKS} (
        id TEXT PRIMARY KEY,
        title TEXT,
        completed INTEGER,
        auto_uncheck_minutes INTEGER DEFAULT 0,
        uncheck_timestamp INTEGER DEFAULT 0,
        position INTEGER DEFAULT 0,
        completion_count INTEGER DEFAULT 0,
        updated_at INTEGER DEFAULT 0,
        is_deleted INTEGER DEFAULT 0,
        color TEXT DEFAULT NULL,
        priority_id TEXT DEFAULT NULL
    )`);
    db.run(`ALTER TABLE ${DB_CONFIG.TABLE_TASKS} ADD COLUMN color TEXT DEFAULT NULL`, () => {});
    db.run(`ALTER TABLE ${DB_CONFIG.TABLE_TASKS} ADD COLUMN priority_id TEXT DEFAULT NULL`, () => {});

    db.run(`CREATE TABLE IF NOT EXISTS ${DB_CONFIG.TABLE_PRIORITIES} (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        color TEXT NOT NULL,
        position INTEGER DEFAULT 0,
        created_at INTEGER
    )`);
});

/**
 * Maps a raw database row to a structured Task model object.
 * @param {Object} row - The raw database row.
 * @returns {Object} The formatted Task entity.
 */
const mapRowToTask = (row) => ({
    id: row.id,
    title: row.title,
    completed: row.completed === 1,
    autoUncheckMinutes: row.auto_uncheck_minutes,
    uncheckTimestamp: row.uncheck_timestamp,
    position: row.position,
    completionCount: row.completion_count,
    updatedAt: row.updated_at,
    isDeleted: row.is_deleted === DB_CONFIG.STATUS_DELETED,
    color: row.priority_color || row.color || null,
    priorityId: row.priority_id || null,
    priorityName: row.priority_name || null
});

/**
 * Maps a raw database row to a structured Priority model object.
 * @param {Object} row - The raw database row.
 * @returns {Object} The formatted Priority entity.
 */
const mapRowToPriority = (row) => ({
    id: row.id,
    name: row.name,
    color: row.color,
    position: row.position,
    createdAt: row.created_at
});

/**
 * TaskRepository handles persistent CRUD database operations for the Task entity.
 */
const TaskRepository = {
    /**
     * Retrieves all active tasks joined with their linked priorities.
     * @returns {Promise<Array<Object>>} List of active tasks.
     * @example const tasks = await TaskRepository.getAll();
     */
    getAll: () => {
        const query = `SELECT t.*, p.name AS priority_name, p.color AS priority_color 
                       FROM ${DB_CONFIG.TABLE_TASKS} t
                       LEFT JOIN ${DB_CONFIG.TABLE_PRIORITIES} p ON t.priority_id = p.id
                       WHERE t.is_deleted = ${DB_CONFIG.STATUS_ACTIVE} 
                       ORDER BY t.position ASC`;
        return new Promise((resolve, reject) => {
            db.all(query, [], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(mapRowToTask));
            });
        });
    },

    /**
     * Finds a specific task by its unique identifier.
     * @param {string} id - The task UUID.
     * @returns {Promise<Object|null>} The found task or null.
     * @example const task = await TaskRepository.getById('123');
     */
    getById: (id) => {
        const query = `SELECT t.*, p.name AS priority_name, p.color AS priority_color 
                       FROM ${DB_CONFIG.TABLE_TASKS} t
                       LEFT JOIN ${DB_CONFIG.TABLE_PRIORITIES} p ON t.priority_id = p.id
                       WHERE t.id = ?`;
        return new Promise((resolve, reject) => {
            db.get(query, [id], (err, row) => {
                if (err) return reject(err);
                resolve(row ? mapRowToTask(row) : null);
            });
        });
    },

    /**
     * Persists a new task into the database.
     * @param {Object} task - The task data to insert.
     * @returns {Promise<void>}
     * @example await TaskRepository.insert({ title: 'Task 1' });
     */
    insert: (task) => {
        const sql = `INSERT INTO ${DB_CONFIG.TABLE_TASKS} (
            id, title, completed, auto_uncheck_minutes, uncheck_timestamp,
            position, completion_count, updated_at, is_deleted, color, priority_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`;
        const params = [
            task.id || crypto.randomUUID(),
            task.title,
            (task.completed === true || task.completed === 1) ? 1 : 0,
            task.autoUncheckMinutes || DB_CONFIG.DEFAULT_MINUTES,
            task.uncheckTimestamp || DB_CONFIG.DEFAULT_TIMESTAMP,
            task.position || DB_CONFIG.DEFAULT_POSITION,
            task.completionCount || DB_CONFIG.DEFAULT_COMPLETIONS,
            task.updatedAt || Date.now(),
            (task.isDeleted === true || task.isDeleted === 1) ? 1 : 0,
            task.color || null,
            task.priorityId || null
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Updates an existing task's fields in the database.
     * @param {Object} task - The task object with updated values.
     * @returns {Promise<void>}
     * @example await TaskRepository.update(task);
     */
    update: (task) => {
        const sql = `UPDATE ${DB_CONFIG.TABLE_TASKS} SET 
            title = ?, completed = ?, auto_uncheck_minutes = ?, uncheck_timestamp = ?,
            position = ?, completion_count = ?, updated_at = ?, is_deleted = ?, color = ?, priority_id = ?
            WHERE id = ?`;
        const params = [
            task.title,
            (task.completed === true || task.completed === 1) ? 1 : 0,
            task.autoUncheckMinutes || DB_CONFIG.DEFAULT_MINUTES,
            task.uncheckTimestamp || DB_CONFIG.DEFAULT_TIMESTAMP,
            task.position || DB_CONFIG.DEFAULT_POSITION,
            task.completionCount || DB_CONFIG.DEFAULT_COMPLETIONS,
            task.updatedAt || Date.now(),
            (task.isDeleted === true || task.isDeleted === 1) ? 1 : 0,
            task.color || null,
            task.priorityId || null,
            task.id
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Retrieves the highest position index in the tasks table.
     * @returns {Promise<number>} The maximum position value, or -1 if empty.
     * @example const max = await TaskRepository.getMaxPosition();
     */
    getMaxPosition: () => {
        const query = `SELECT MAX(position) as maxPos FROM ${DB_CONFIG.TABLE_TASKS}`;
        return new Promise((resolve, reject) => {
            db.get(query, [], (err, row) => {
                if (err) return reject(err);
                resolve(row && row.maxPos !== null ? row.maxPos : -1);
            });
        });
    }
};

/**
 * PriorityRepository handles database operations for priority categories.
 */
const PriorityRepository = {
    /**
     * Retrieves all configured priorities ordered by manual position.
     * @returns {Promise<Array<Object>>} List of priorities.
     * @example const list = await PriorityRepository.getAll();
     */
    getAll: () => {
        const query = `SELECT * FROM ${DB_CONFIG.TABLE_PRIORITIES} ORDER BY position ASC, created_at ASC`;
        return new Promise((resolve, reject) => {
            db.all(query, [], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(mapRowToPriority));
            });
        });
    },

    /**
     * Finds a priority by its unique identifier.
     * @param {string} id - Priority identifier.
     * @returns {Promise<Object|null>} Found priority or null.
     * @example const p = await PriorityRepository.getById('uuid');
     */
    getById: (id) => {
        const query = `SELECT * FROM ${DB_CONFIG.TABLE_PRIORITIES} WHERE id = ?`;
        return new Promise((resolve, reject) => {
            db.get(query, [id], (err, row) => {
                if (err) return reject(err);
                resolve(row ? mapRowToPriority(row) : null);
            });
        });
    },

    /**
     * Persists a new priority category.
     * @param {Object} priority - The priority to insert.
     * @returns {Promise<void>}
     * @example await PriorityRepository.insert({ name: 'High', color: '#ff0000' });
     */
    insert: (priority) => {
        const sql = `INSERT INTO ${DB_CONFIG.TABLE_PRIORITIES} (id, name, color, position, created_at) VALUES (?, ?, ?, ?, ?)`;
        const params = [
            priority.id || crypto.randomUUID(),
            priority.name,
            priority.color || DB_CONFIG.DEFAULT_PRIORITY_COLOR,
            priority.position || DB_CONFIG.DEFAULT_POSITION,
            priority.createdAt || Date.now()
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Updates an existing priority category's name, color, and position.
     * @param {Object} priority - The priority data with updated values.
     * @returns {Promise<void>}
     * @example await PriorityRepository.update(priority);
     */
    update: (priority) => {
        const sql = `UPDATE ${DB_CONFIG.TABLE_PRIORITIES} SET name = ?, color = ?, position = ? WHERE id = ?`;
        const params = [
            priority.name,
            priority.color,
            priority.position || DB_CONFIG.DEFAULT_POSITION,
            priority.id
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Deletes a priority category and disassociates linked tasks.
     * @param {string} id - Identifier of the priority to remove.
     * @returns {Promise<void>}
     * @example await PriorityRepository.delete('uuid');
     */
    delete: (id) => {
        return new Promise((resolve, reject) => {
            db.serialize(() => {
                db.run(`UPDATE ${DB_CONFIG.TABLE_TASKS} SET priority_id = NULL WHERE priority_id = ?`, [id]);
                db.run(`DELETE FROM ${DB_CONFIG.TABLE_PRIORITIES} WHERE id = ?`, [id], (err) => {
                    if (err) return reject(err);
                    resolve();
                });
            });
        });
    }
};

module.exports = {
    TaskRepository,
    PriorityRepository
};
