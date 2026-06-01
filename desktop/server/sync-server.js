const express = require('express');
const cors = require('cors');
const TaskRepository = require('../db/database');

const PROTOCOL_VERSION = 1;

function startSyncServer(port = 8080, onSyncComplete) {
    const app = express();
    app.use(cors());
    app.use(express.json());

    // Endpoint de Sincronização em Lote
    app.post('/api/v1/sync', async (req, res) => {
        console.log(`[Sync] Recebida requisição de sincronização.`);
        const { protocolVersion, lastSyncTimestamp, clientChanges } = req.body;
        console.log(`[Sync] Protocolo: ${protocolVersion}, LastSync: ${lastSyncTimestamp}, Mudanças do cliente: ${clientChanges ? clientChanges.length : 0}`);

        // 1. Validar Versão
        if (protocolVersion > PROTOCOL_VERSION) {
            return res.status(426).json({ error: "Upgrade Required", message: "Versão do protocolo não suportada." });
        }

        try {
            // 2. Processar Push (clientChanges)
            if (clientChanges && Array.isArray(clientChanges)) {
                for (const clientTask of clientChanges) {
                    const serverTask = await TaskRepository.getById(clientTask.id);

                    if (!serverTask) {
                        console.log(`[Sync] Inserindo nova tarefa do cliente: ${clientTask.title}`);
                        await TaskRepository.insert(clientTask);
                    } else {
                        // Existe: Resolver conflito (Last Write Wins)
                        if (clientTask.updatedAt > serverTask.updatedAt) {
                            console.log(`[Sync] Atualizando tarefa com dados do cliente: ${clientTask.title}`);
                            await TaskRepository.update(clientTask);
                        } else {
                            console.log(`[Sync] Tarefa do cliente ignorada (mais antiga ou igual): ${clientTask.title}`);
                        }
                    }
                }
            }

            // 3. Processar Pull (serverChanges)
            const serverChanges = await TaskRepository.getForSync(lastSyncTimestamp);
            console.log(`[Sync] Enviando ${serverChanges.length} mudanças do servidor.`);

            // 4. Responder
            const responseBody = {
                protocolVersion: PROTOCOL_VERSION,
                serverTimestamp: Date.now(),
                serverChanges: serverChanges
            };
            res.json(responseBody);

            // Notificar interface se houve mudanças
            if (onSyncComplete && ((clientChanges && clientChanges.length > 0) || serverChanges.length > 0)) {
                onSyncComplete();
            }

        } catch (err) {
            console.error("[Sync] Erro no processamento:", err);
            res.status(500).json({ error: "Internal Server Error" });
        }
    });

    app.listen(port, () => {
        console.log(`Servidor de Sincronização rodando na porta ${port}`);
    });
}

module.exports = startSyncServer;
