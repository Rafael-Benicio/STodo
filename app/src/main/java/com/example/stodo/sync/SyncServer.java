package com.example.stodo.sync;

import android.util.Log;
import com.example.stodo.Task;
import com.example.stodo.repository.TaskRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SyncServer is an embedded HTTP server running on the Android device.
 * It listens for incoming POST /api/v1/sync requests and handles them using Last Write Wins logic.
 */
public class SyncServer {
    private static final String TAG = "SyncServer";
    private static final int PROTOCOL_VERSION = 1;

    // HTTP status codes as constants to avoid magic numbers
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UPGRADE_REQUIRED = 426;
    private static final int HTTP_INTERNAL_ERROR = 500;

    // Port search bounds as constants to avoid magic numbers
    private static final int PORT_RANGE_MIN = 8080;
    private static final int PORT_RANGE_MAX = 8089;

    // Header prefix constant to avoid magic substring index
    private static final String HEADER_CONTENT_LENGTH = "content-length:";
    
    private final TaskRepository repository;
    private final Gson gson;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private int port = -1;
    private volatile boolean running = false;

    /**
     * Constructs a SyncServer using the provided TaskRepository.
     * Example: SyncServer server = new SyncServer(taskRepository);
     */
    public SyncServer(TaskRepository repository) {
        this.repository = repository;
        this.gson = new Gson();
    }

    /**
     * Starts the synchronization server on an available port in the 8080-8089 range.
     * Returns the bound port number, or -1 if no port was available.
     * Example: int port = syncServer.start();
     */
    public int start() {
        for (int p = PORT_RANGE_MIN; p <= PORT_RANGE_MAX; p++) {
            try {
                serverSocket = new ServerSocket(p);
                this.port = p;
                this.running = true;
                startListeningThread();
                Log.i(TAG, "SyncServer started on port " + p);
                return p;
            } catch (Exception e) {
                Log.w(TAG, "Port " + p + " is busy, trying next...");
            }
        }
        return -1;
    }

    /**
     * Stops the running synchronization server and releases network resources.
     * Example: syncServer.stop();
     */
    public void stop() {
        this.running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing server socket", e);
        }
        if (serverThread != null) serverThread.interrupt();
    }

    /**
     * Returns the port the server is bound to, or -1 if not running.
     * Example: int currentPort = syncServer.getPort();
     */
    public int getPort() { return port; }

    private void startListeningThread() {
        serverThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleConnection(clientSocket)).start();
                } catch (Exception e) {
                    if (running) Log.e(TAG, "Accept error", e);
                }
            }
        });
        serverThread.start();
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream output = socket.getOutputStream()) {
            
            HttpRequest request = parseRequest(reader);
            if (request == null) {
                sendResponse(output, HTTP_BAD_REQUEST, "Bad Request");
                return;
            }
            
            if (!"POST".equalsIgnoreCase(request.method) || !"/api/v1/sync".equals(request.path)) {
                sendResponse(output, HTTP_NOT_FOUND, "Not Found");
                return;
            }

            processSyncRequest(request.body, output);
        } catch (Exception e) {
            Log.e(TAG, "Connection handler error", e);
        } finally {
            closeSocket(socket);
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    private static class HttpRequest {
        String method;
        String path;
        String body;
    }

    private HttpRequest parseRequest(BufferedReader reader) throws Exception {
        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) return null;

        String[] parts = line.split(" ");
        if (parts.length < 2) return null;

        HttpRequest req = new HttpRequest();
        req.method = parts[0];
        req.path = parts[1];

        int contentLength = readHeaders(reader);
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int read = reader.read(bodyChars, 0, contentLength);
            if (read == contentLength) req.body = new String(bodyChars);
        }
        return req;
    }

    private int readHeaders(BufferedReader reader) throws Exception {
        int contentLength = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            if (line.toLowerCase().startsWith(HEADER_CONTENT_LENGTH)) {
                contentLength = Integer.parseInt(line.substring(HEADER_CONTENT_LENGTH.length()).trim());
            }
        }
        return contentLength;
    }

    private void processSyncRequest(String requestBody, OutputStream output) throws Exception {
        if (requestBody == null) {
            sendResponse(output, HTTP_BAD_REQUEST, "Empty Body");
            return;
        }
        JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();
        int clientVersion = json.get("protocolVersion").getAsInt();
        if (clientVersion > PROTOCOL_VERSION) {
            sendResponse(output, HTTP_UPGRADE_REQUIRED, "Upgrade Required");
            return;
        }

        long lastSyncTimestamp = json.get("lastSyncTimestamp").getAsLong();
        JsonArray clientChanges = json.getAsJsonArray("clientChanges");
        
        synchronized (repository) {
            processClientChanges(clientChanges);
            String responseJson = prepareSyncResponse(lastSyncTimestamp);
            sendResponse(output, HTTP_OK, responseJson);
        }
    }

    private void processClientChanges(JsonArray clientChanges) {
        if (clientChanges == null) return;
        for (int i = 0; i < clientChanges.size(); i++) {
            Task clientTask = gson.fromJson(clientChanges.get(i), Task.class);
            applyTaskChange(clientTask);
        }
    }

    private void applyTaskChange(Task clientTask) {
        Task serverTask = repository.getById(clientTask.getId());
        if (serverTask == null) {
            repository.add(clientTask);
        } else if (clientTask.getUpdatedAt() > serverTask.getUpdatedAt()) {
            repository.update(clientTask);
        }
    }

    private String prepareSyncResponse(long lastSyncTimestamp) {
        List<Task> all = repository.getAll();
        List<Task> serverChanges = new ArrayList<>();
        for (Task task : all) {
            if (task.getUpdatedAt() > lastSyncTimestamp) {
                serverChanges.add(task);
            }
        }

        JsonObject response = new JsonObject();
        response.addProperty("protocolVersion", PROTOCOL_VERSION);
        response.addProperty("serverTimestamp", System.currentTimeMillis());
        
        JsonArray changesArray = new JsonArray();
        for (Task t : serverChanges) {
            changesArray.add(gson.toJsonTree(t));
        }
        response.add("serverChanges", changesArray);
        return response.toString();
    }

    private void sendResponse(OutputStream output, int statusCode, String responseText) throws Exception {
        String statusMessage = getStatusMessage(statusCode);
        byte[] bodyBytes = responseText.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: " + (statusCode == HTTP_OK ? "application/json" : "text/plain") + "\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(bodyBytes);
        output.flush();
    }

    private String getStatusMessage(int code) {
        if (code == HTTP_OK) return "OK";
        if (code == HTTP_BAD_REQUEST) return "Bad Request";
        if (code == HTTP_NOT_FOUND) return "Not Found";
        if (code == HTTP_UPGRADE_REQUIRED) return "Upgrade Required";
        return "Internal Server Error";
    }
}
