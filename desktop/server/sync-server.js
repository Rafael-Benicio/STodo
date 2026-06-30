const express = require('express');
const cors = require('cors');
const TaskRepository = require('../db/database');
const logger = require('./console-ansi-logger');

const CONFIG = {
    PROTOCOL_VERSION: 1,
    DEFAULT_PORT: 8080,
    HTTP_OK: 200,
    HTTP_UPGRADE_REQUIRED: 426,
    HTTP_SERVER_ERROR: 500
};

/**
 * Resolves conflicts and applies client-side changes to the server database.
 * @param {Array} clientChanges - List of modified tasks from the mobile client.
 */
async function processClientPush(clientChanges) {
    if (!clientChanges || !Array.isArray(clientChanges)) return;

    for (const clientTask of clientChanges) {
        const serverTask = await TaskRepository.getById(clientTask.id);

        if (!serverTask) {
            logger.info('Sync', `Inserting new client task: ${clientTask.title}`);
            await TaskRepository.insert(clientTask);
        } else if (clientTask.updatedAt > serverTask.updatedAt) {
            logger.info('Sync', `Updating server task with newer client data: ${clientTask.title}`);
            await TaskRepository.update(clientTask);
        } else {
            logger.info('Sync', `Client task ignored (older or equal): ${clientTask.title}`);
        }
    }
}

/**
 * Prepares the synchronization response body.
 * @param {number} lastSyncTimestamp - The threshold for changes to include.
 * @returns {Object} The formatted sync response.
 */
async function prepareSyncResponse(lastSyncTimestamp) {
    const serverChanges = await TaskRepository.getForSync(lastSyncTimestamp);
    logger.info('Sync', `Preparing to send ${serverChanges.length} server changes.`);

    return {
        protocolVersion: CONFIG.PROTOCOL_VERSION,
        serverTimestamp: Date.now(),
        serverChanges: serverChanges
    };
}

/**
 * Express router handler for processing peer sync requests.
 * @param {Object} req - Express request object.
 * @param {Object} res - Express response object.
 * @param {Function} onSyncComplete - Optional success callback.
 * @example handleSyncRequest(req, res, callback);
 */
async function handleSyncRequest(req, res, onSyncComplete) {
    const { protocolVersion, lastSyncTimestamp, clientChanges } = req.body;
    if (protocolVersion > CONFIG.PROTOCOL_VERSION) {
        logger.warn('Sync', `Upgrade required: client protocol version ${protocolVersion}`);
        return res.status(CONFIG.HTTP_UPGRADE_REQUIRED).json({ error: "Upgrade Required" });
    }
    try {
        await processClientPush(clientChanges);
        const response = await prepareSyncResponse(lastSyncTimestamp);
        res.status(CONFIG.HTTP_OK).json(response);
        logger.info('Sync', `Incoming sync completed: processed ${clientChanges ? clientChanges.length : 0} changes, sent ${response.serverChanges.length}`);
        if (onSyncComplete) onSyncComplete();
    } catch (err) {
        logger.error('Sync', `Processing Error: ${err.message}`);
        res.status(CONFIG.HTTP_SERVER_ERROR).json({ error: "Internal Server Error" });
    }
}

/**
 * Initializes and starts the HTTP Synchronization Server.
 * @param {number} port - The port to listen on.
 * @param {Function} onSyncComplete - Optional callback triggered after successful sync.
 * @example startSyncServer(8080, callback);
 */
function startSyncServer(port = CONFIG.DEFAULT_PORT, onSyncComplete) {
    const app = express();
    app.use(cors());
    app.use(express.json());

    app.post('/api/v1/sync', (req, res) => {
        handleSyncRequest(req, res, onSyncComplete);
    });

    app.get('/api/v1/ping', (req, res) => {
        res.status(CONFIG.HTTP_OK).json({ status: "ok" });
    });

    app.listen(port, () => {
        logger.info('Sync', `Sync Server running on port ${port}`);
    });
}

module.exports = {
    startSyncServer,
    processClientPush,
    prepareSyncResponse
};
