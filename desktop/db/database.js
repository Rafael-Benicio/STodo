const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const crypto = require('crypto');

/**
 * Constants for database configuration and field names to avoid magic strings/numbers.
 */
const DB_CONFIG = {
    FILE_NAME: 'stodo_desktop.db',
    TABLE_NAME: 'tasks',
    STATUS_DELETED: 1,
    STATUS_ACTIVE: 0
};

const dbPath = path.join(__dirname, DB_CONFIG.FILE_NAME);
const db = new sqlite3.Database(dbPath);

/**
 * Initialize the database schema if it doesn't exist.
 */
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS ${DB_CONFIG.TABLE_NAME} (
        id TEXT PRIMARY KEY,
        title TEXT,
        completed INTEGER,
        auto_uncheck_minutes INTEGER DEFAULT 0,
        uncheck_timestamp INTEGER DEFAULT 0,
        position INTEGER DEFAULT 0,
        completion_count INTEGER DEFAULT 0,
        updated_at INTEGER DEFAULT 0,
        is_deleted INTEGER DEFAULT 0
    )`);
});

/**
 * Maps a raw database row to a Task model object.
 * @param {Object} row - The raw database row.
 * @returns {Object} The formatted Task object.
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
    isDeleted: row.is_deleted === 1
});

/**
 * TaskRepository handles all database operations for the Task entity.
 */
const TaskRepository = {
    /**
     * Retrieves all active tasks ordered by priority.
     * Example: const tasks = await TaskRepository.getAll();
     */
    getAll: () => {
        const query = `SELECT * FROM ${DB_CONFIG.TABLE_NAME} 
                       WHERE is_deleted = ${DB_CONFIG.STATUS_ACTIVE} 
                       ORDER BY completion_count DESC, position ASC`;
        return new Promise((resolve, reject) => {
            db.all(query, [], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(mapRowToTask));
            });
        });
    },

    /**
     * Retrieves tasks updated after a specific timestamp for synchronization.
     * @param {number} timestamp - The last sync timestamp.
     */
    getForSync: (timestamp) => {
        const query = `SELECT * FROM ${DB_CONFIG.TABLE_NAME} WHERE updated_at > ?`;
        return new Promise((resolve, reject) => {
            db.all(query, [timestamp], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(mapRowToTask));
            });
        });
    },

    /**
     * Finds a specific task by its unique identifier.
     * @param {string} id - The task UUID.
     */
    getById: (id) => {
        const query = `SELECT * FROM ${DB_CONFIG.TABLE_NAME} WHERE id = ?`;
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
     */
    insert: (task) => {
        const sql = `INSERT INTO ${DB_CONFIG.TABLE_NAME} (id, title, completed, auto_uncheck_minutes, uncheck_timestamp, position, completion_count, updated_at, is_deleted) 
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`;
        const params = [
            task.id || crypto.randomUUID(),
            task.title,
            (task.completed === true || task.completed === 1) ? 1 : 0,
            task.autoUncheckMinutes || 0,
            task.uncheckTimestamp || 0,
            task.position || 0,
            task.completionCount || 0,
            task.updatedAt || Date.now(),
            (task.isDeleted === true || task.isDeleted === 1) ? 1 : 0
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, function(err) {
                if (err) reject(err);
                else resolve();
            });
        });
    },

    /**
     * Updates an existing task's data.
     * @param {Object} task - The task object with updated fields.
     */
    update: (task) => {
        const sql = `UPDATE ${DB_CONFIG.TABLE_NAME} SET 
            title = ?, completed = ?, auto_uncheck_minutes = ?, uncheck_timestamp = ?, 
            position = ?, completion_count = ?, updated_at = ?, is_deleted = ? 
            WHERE id = ?`;
        const params = [
            task.title,
            (task.completed === true || task.completed === 1) ? 1 : 0,
            task.autoUncheckMinutes || 0,
            task.uncheckTimestamp || 0,
            task.position || 0,
            task.completionCount || 0,
            task.updatedAt || Date.now(),
            (task.isDeleted === true || task.isDeleted === 1) ? 1 : 0,
            task.id
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => {
                if (err) reject(err);
                else resolve();
            });
        });
    },

    /**
     * Gets the highest position value in the task list.
     */
    getMaxPosition: () => {
        const query = `SELECT MAX(position) as maxPos FROM ${DB_CONFIG.TABLE_NAME}`;
        return new Promise((resolve, reject) => {
            db.get(query, [], (err, row) => {
                if (err) reject(err);
                else resolve(row ? row.maxPos : -1);
            });
        });
    }
};

module.exports = TaskRepository;
