const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const crypto = require('crypto');

const dbPath = path.join(__dirname, 'stodo_desktop.db');
const db = new sqlite3.Database(dbPath);

// Inicializar banco de dados
db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS tasks (
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

const TaskRepository = {
    getAll: () => {
        return new Promise((resolve, reject) => {
            db.all("SELECT * FROM tasks WHERE is_deleted = 0 ORDER BY completion_count DESC, position ASC", [], (err, rows) => {
                if (err) reject(err);
                else {
                    resolve(rows.map(row => ({
                        id: row.id,
                        title: row.title,
                        completed: row.completed === 1,
                        autoUncheckMinutes: row.auto_uncheck_minutes,
                        uncheckTimestamp: row.uncheck_timestamp,
                        position: row.position,
                        completionCount: row.completion_count,
                        updatedAt: row.updated_at,
                        isDeleted: row.is_deleted === 1
                    })));
                }
            });
        });
    },

    getForSync: (lastSyncTimestamp) => {
        return new Promise((resolve, reject) => {
            db.all("SELECT * FROM tasks WHERE updated_at > ?", [lastSyncTimestamp], (err, rows) => {
                if (err) reject(err);
                else {
                    resolve(rows.map(row => ({
                        id: row.id,
                        title: row.title,
                        completed: row.completed === 1,
                        autoUncheckMinutes: row.auto_uncheck_minutes,
                        uncheckTimestamp: row.uncheck_timestamp,
                        position: row.position,
                        completionCount: row.completion_count,
                        updatedAt: row.updated_at,
                        isDeleted: row.is_deleted === 1
                    })));
                }
            });
        });
    },

    getById: (id) => {
        return new Promise((resolve, reject) => {
            db.get("SELECT * FROM tasks WHERE id = ?", [id], (err, row) => {
                if (err) reject(err);
                else if (!row) resolve(null);
                else {
                    resolve({
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
                }
            });
        });
    },

    insert: (task) => {
        return new Promise((resolve, reject) => {
            const sql = `INSERT INTO tasks (id, title, completed, auto_uncheck_minutes, uncheck_timestamp, position, completion_count, updated_at, is_deleted) 
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`;
            // Garantir mapeamento correto de nomes (JSON -> DB)
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
            db.run(sql, params, function(err) {
                if (err) reject(err);
                else resolve();
            });
        });
    },

    update: (task) => {
        return new Promise((resolve, reject) => {
            const sql = `UPDATE tasks SET 
                title = ?, 
                completed = ?, 
                auto_uncheck_minutes = ?, 
                uncheck_timestamp = ?, 
                position = ?, 
                completion_count = ?, 
                updated_at = ?, 
                is_deleted = ? 
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
            db.run(sql, params, (err) => {
                if (err) reject(err);
                else resolve();
            });
        });
    },

    getMaxPosition: () => {
        return new Promise((resolve, reject) => {
            db.get("SELECT MAX(position) as maxPos FROM tasks", [], (err, row) => {
                if (err) reject(err);
                else resolve(row ? row.maxPos : -1);
            });
        });
    }
};

module.exports = TaskRepository;
