const express = require('express');
const cors = require('cors');
const TaskRepository = require('../db/database');

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
            console.log(`[Sync] Inserting new client task: ${clientTask.title}`);
            await TaskRepository.insert(clientTask);
        } else if (clientTask.updatedAt > serverTask.updatedAt) {
            console.log(`[Sync] Updating server task with newer client data: ${clientTask.title}`);
            await TaskRepository.update(clientTask);
        } else {
            console.log(`[Sync] Client task ignored (older or equal): ${clientTask.title}`);
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
    console.log(`[Sync] Sending ${serverChanges.length} server changes.`);

    return {
        protocolVersion: CONFIG.PROTOCOL_VERSION,
        serverTimestamp: Date.now(),
        serverChanges: serverChanges
    };
}

/**
 * Initializes and starts the HTTP Synchronization Server.
 * @param {number} port - The port to listen on.
 * @param {Function} onSyncComplete - Optional callback triggered after successful sync.
 */
function startSyncServer(port = CONFIG.DEFAULT_PORT, onSyncComplete) {
    const app = express();
    app.use(cors());
    app.use(express.json());

    app.post('/api/v1/sync', async (req, res) => {
        const { protocolVersion, lastSyncTimestamp, clientChanges } = req.body;
        
        if (protocolVersion > CONFIG.PROTOCOL_VERSION) {
            return res.status(CONFIG.HTTP_UPGRADE_REQUIRED).json({ error: "Upgrade Required" });
        }

        try {
            await processClientPush(clientChanges);
            const response = await prepareSyncResponse(lastSyncTimestamp);
            
            res.status(CONFIG.HTTP_OK).json(response);

            if (onSyncComplete) onSyncComplete();
        } catch (err) {
            console.error("[Sync] Processing Error:", err);
            res.status(CONFIG.HTTP_SERVER_ERROR).json({ error: "Internal Server Error" });
        }
    });

    app.listen(port, () => {
        console.log(`Sync Server running on port ${port}`);
    });
}

module.exports = startSyncServer;
