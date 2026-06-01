# Especificação de Sincronização Local (Protocolo STodo)
**Documento Oficial para Integração Multiplataforma (Desktop / Mobile)**

Este documento define o protocolo de comunicação, os modelos de dados e a arquitetura necessária para que o aplicativo Desktop (Hub) atue como o servidor central de sincronização para os aplicativos móveis na mesma rede local.

## 1. Visão Geral da Arquitetura
O sistema utiliza uma arquitetura assíncrona **Offline-First**. O Desktop atua como o servidor HTTP mestre na rede local (LAN). O aplicativo Mobile atua como cliente, descobrindo o Desktop e iniciando a sincronização via modelo **Push/Pull**.
- **Protocolo de Sincronização:** Baseado em "Last Modified" (Timestamp) e controle de exclusão lógica (Soft Delete).
- **Formato de Dados:** JSON sobre HTTP.

## 2. Descoberta de Rede (Network Discovery)
Para evitar que o usuário digite o IP do Desktop manualmente, o Desktop deve anunciar sua presença na rede local.
- **Protocolo Obrigatório no Desktop:** mDNS / DNS-SD.
- **Service Type:** `_stodo._tcp.`
- **Porta:** Sugerida `8080` (mas deve ser a porta real onde o servidor HTTP está rodando).
O Mobile fará o escaneamento desse serviço para encontrar o IP dinâmico do Hub.

## 3. Modelo de Dados de Intercâmbio (Entity `Task`)
O payload JSON que representa uma tarefa **deve** respeitar estritamente a seguinte estrutura.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000", // UUID v4
  "title": "Comprar Leite",
  "completed": false,
  "autoUncheckMinutes": 0, // 0 = desativado
  "uncheckTimestamp": 0, // Timestamp absoluto
  "position": 1, // Ordem manual na lista
  "completionCount": 0,
  "updatedAt": 1716654000000, // Essencial para resolução de conflitos
  "isDeleted": false // Soft delete
}
```

## 4. Versionamento da API (Evitando Quebras)
Para garantir que um aplicativo Mobile atualizado não quebre um Desktop desatualizado (e vice-versa), toda comunicação incluirá uma variável de versão da API.

- **`protocolVersion` atual:** `1`
- **Regra de Rejeição:** Se o servidor Desktop receber uma requisição com um `protocolVersion` maior do que ele suporta, ele **deve** retornar um erro HTTP `426 Upgrade Required`. Se o Mobile receber um `protocolVersion` menor do que o esperado (quebre compatibilidade), ele deve abortar a sincronização e avisar o usuário.

## 5. Endpoints da API (Hospedados no Desktop)

O servidor Desktop deve expor o seguinte endpoint REST.

### Endpoint de Sincronização em Lote (Sync)
- **Rota:** `POST /api/v1/sync`
- **Fluxo:** O Mobile envia as alterações locais desde o último sync e pede as novidades do servidor no mesmo período.

**Request do Mobile (Cliente) para o Desktop (Servidor):**
```json
{
  "protocolVersion": 1,
  "lastSyncTimestamp": 1716650000000,
  "clientChanges": [
     { "id": "uuid-1", "title": "Nova Tarefa", "updatedAt": 1716653000000, "isDeleted": false, ... }
  ]
}
```

**Response de Sucesso do Desktop para o Mobile (HTTP 200 OK):**
O Desktop deve resolver os conflitos (Lógica "Last Write Wins") antes de gerar a resposta. O `serverTimestamp` é o relógio do Desktop no momento da resposta.

```json
{
  "protocolVersion": 1,
  "serverTimestamp": 1716655000000,
  "serverChanges": [
     { "id": "uuid-3", "title": "Tarefa do PC", "updatedAt": 1716654500000, "isDeleted": false, ... }
  ]
}
```

## 6. Lógica Obrigatória no Servidor Desktop (Resolução de Conflitos)
Quando o endpoint `/api/v1/sync` for chamado, o Desktop deve executar os seguintes passos na mesma transação:

1. **Validação:** Checar se o `protocolVersion` do request é suportado. Se for maior, retornar `HTTP 426`.
2. **Processamento do Push (clientChanges):**
   - Para cada `clientTask` no request:
     - Buscar no banco local por `id`.
     - **Se não existir:** Inserir no banco do Desktop.
     - **Se existir (Conflito):** Comparar os timestamps.
       - `IF clientTask.updatedAt > serverTask.updatedAt`: Atualiza a versão do Desktop com os dados do cliente.
       - `ELSE`: Ignora a versão do cliente e mantém a do Desktop.
3. **Processamento do Pull (serverChanges):**
   - Buscar no banco do Desktop todas as tarefas onde `updatedAt > request.lastSyncTimestamp`.
   - **Cuidado:** Não enviar de volta as tarefas que acabaram de ser atualizadas pelo passo 2 no mesmo milissegundo, para economizar banda (opcional, mas recomendado).
4. **Retorno:** Devolver a lista do passo 3 e o `serverTimestamp` (o `System.currentTimeMillis()` atual do servidor).
