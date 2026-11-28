const crypto = require('crypto');
const { db } = require('./database');

const TRACKER_DB_CONFIG = {
    TABLE_NAME: 'episode_trackers',
    DEFAULT_EPISODE: 0,
    DEFAULT_POSITION: 0,
    EMPTY_LINKS_JSON: '[]'
};

/**
 * Safely parses stored JSON links into an array of link objects.
 * @param {string|null} rawLinks - Raw JSON string from SQLite column.
 * @returns {Array<Object>} Parsed link entries.
 * @example const links = parseLinksJson('[{"label":"Ep","url":"https://example.com"}]');
 */
function parseLinksJson(rawLinks) {
    if (!rawLinks) return [];
    try {
        const parsed = JSON.parse(rawLinks);
        return Array.isArray(parsed) ? parsed : [];
    } catch (_err) {
        return [];
    }
}

/**
 * Maps a raw database row into a structured Tracker entity.
 * @param {Object} row - Database row from episode_trackers table.
 * @returns {Object} Formatted tracker object.
 * @example const item = mapRowToTracker(row);
 */
function mapRowToTracker(row) {
    return {
        id: row.id,
        title: row.title,
        currentEpisode: row.current_episode || TRACKER_DB_CONFIG.DEFAULT_EPISODE,
        links: parseLinksJson(row.links),
        position: row.position || TRACKER_DB_CONFIG.DEFAULT_POSITION,
        createdAt: row.created_at,
        updatedAt: row.updated_at
    };
}

/**
 * TrackerRepository manages persistent CRUD operations for episode trackers.
 */
const TrackerRepository = {
    /**
     * Retrieves all episode trackers ordered by manual position and creation date.
     * @returns {Promise<Array<Object>>} List of episode tracker items.
     * @example const items = await TrackerRepository.getAll();
     */
    getAll: () => {
        const sql = `SELECT * FROM ${TRACKER_DB_CONFIG.TABLE_NAME} ORDER BY position ASC, created_at ASC`;
        return new Promise((resolve, reject) => {
            db.all(sql, [], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(mapRowToTracker));
            });
        });
    },

    /**
     * Finds a single tracker item by its unique identifier.
     * @param {string} id - Tracker UUID.
     * @returns {Promise<Object|null>} Found tracker or null.
     * @example const item = await TrackerRepository.getById('uuid-1');
     */
    getById: (id) => {
        const sql = `SELECT * FROM ${TRACKER_DB_CONFIG.TABLE_NAME} WHERE id = ?`;
        return new Promise((resolve, reject) => {
            db.get(sql, [id], (err, row) => {
                if (err) return reject(err);
                resolve(row ? mapRowToTracker(row) : null);
            });
        });
    },

    /**
     * Persists a new episode tracker record into SQLite.
     * @param {Object} tracker - Tracker object to insert.
     * @returns {Promise<void>}
     * @example await TrackerRepository.insert({ title: 'Show', currentEpisode: 1 });
     */
    insert: (tracker) => {
        const sql = `INSERT INTO ${TRACKER_DB_CONFIG.TABLE_NAME} 
            (id, title, current_episode, links, position, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)`;
        const params = [
            tracker.id || crypto.randomUUID(),
            tracker.title,
            tracker.currentEpisode ?? TRACKER_DB_CONFIG.DEFAULT_EPISODE,
            JSON.stringify(tracker.links || []),
            tracker.position ?? TRACKER_DB_CONFIG.DEFAULT_POSITION,
            tracker.createdAt || Date.now(),
            tracker.updatedAt || Date.now()
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Updates an existing episode tracker record.
     * @param {Object} tracker - Tracker object with updated fields.
     * @returns {Promise<void>}
     * @example await TrackerRepository.update(tracker);
     */
    update: (tracker) => {
        const sql = `UPDATE ${TRACKER_DB_CONFIG.TABLE_NAME} 
            SET title = ?, current_episode = ?, links = ?, position = ?, updated_at = ? 
            WHERE id = ?`;
        const params = [
            tracker.title,
            tracker.currentEpisode ?? TRACKER_DB_CONFIG.DEFAULT_EPISODE,
            JSON.stringify(tracker.links || []),
            tracker.position ?? TRACKER_DB_CONFIG.DEFAULT_POSITION,
            tracker.updatedAt || Date.now(),
            tracker.id
        ];
        return new Promise((resolve, reject) => {
            db.run(sql, params, (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Permanently removes an episode tracker record by ID.
     * @param {string} id - Tracker identifier.
     * @returns {Promise<void>}
     * @example await TrackerRepository.delete('uuid-1');
     */
    delete: (id) => {
        const sql = `DELETE FROM ${TRACKER_DB_CONFIG.TABLE_NAME} WHERE id = ?`;
        return new Promise((resolve, reject) => {
            db.run(sql, [id], (err) => (err ? reject(err) : resolve()));
        });
    },

    /**
     * Retrieves the highest position index currently stored in trackers.
     * @returns {Promise<number>} Maximum position value or -1 if empty.
     * @example const max = await TrackerRepository.getMaxPosition();
     */
    getMaxPosition: () => {
        const sql = `SELECT MAX(position) AS maxPos FROM ${TRACKER_DB_CONFIG.TABLE_NAME}`;
        return new Promise((resolve, reject) => {
            db.get(sql, [], (err, row) => {
                if (err) return reject(err);
                resolve(row && row.maxPos !== null ? row.maxPos : -1);
            });
        });
    }
};

module.exports = TrackerRepository;
