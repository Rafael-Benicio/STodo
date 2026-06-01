package com.example.stodo.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.stodo.Task;
import com.example.stodo.repository.TaskRepository;
import com.example.stodo.service.TaskService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * SyncManager coordinates the data synchronization between the Android app and the Desktop Hub.
 * It follows a Push/Pull batch protocol using HTTP POST requests.
 */
public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final int PROTOCOL_VERSION = 1;
    private static final String PREFS_NAME = "SyncPrefs";
    private static final String KEY_LAST_SYNC_SERVER = "lastServerTimestamp";
    private static final String KEY_LAST_SYNC_LOCAL = "lastLocalTimestamp";

    private final TaskRepository repository;
    private final RequestQueue requestQueue;
    private final Gson gson;
    private final SharedPreferences prefs;

    /**
     * Interface for receiving synchronization results.
     */
    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    public SyncManager(Context context, TaskService taskService, TaskRepository repository) {
        this.repository = repository;
        this.requestQueue = Volley.newRequestQueue(context);
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Initiates a full synchronization cycle with the specified Desktop Hub.
     * Example: syncManager.sync("192.168.1.5", 8080, callback);
     */
    public void sync(String host, int port, SyncCallback callback) {
        final long startTime = System.currentTimeMillis();
        final long lastServerSync = prefs.getLong(KEY_LAST_SYNC_SERVER, 0);
        final long lastLocalSync = prefs.getLong(KEY_LAST_SYNC_LOCAL, 0);
        
        List<Task> localChanges = getLocalChanges(lastLocalSync);
        String url = buildSyncUrl(host, port);

        try {
            org.json.JSONObject payload = createSyncPayload(lastServerSync, localChanges);
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> handleSyncSuccess(response, startTime, callback),
                error -> handleSyncError(error, callback)
            );

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Sync initiation failed", e);
            callback.onError("Internal error: " + e.getMessage());
        }
    }

    private String buildSyncUrl(String host, int port) {
        String formattedHost = host.contains(":") ? "[" + host + "]" : host;
        return String.format("http://%s:%d/api/v1/sync", formattedHost, port);
    }

    private org.json.JSONObject createSyncPayload(long lastServerSync, List<Task> changes) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("protocolVersion", PROTOCOL_VERSION);
        body.addProperty("lastSyncTimestamp", lastServerSync);
        
        JsonArray changesArray = new JsonArray();
        for (Task task : changes) {
            changesArray.add(gson.toJsonTree(task));
        }
        body.add("clientChanges", changesArray);
        
        return new org.json.JSONObject(body.toString());
    }

    private void handleSyncSuccess(org.json.JSONObject response, long startTime, SyncCallback callback) {
        try {
            processResponse(response);
            prefs.edit().putLong(KEY_LAST_SYNC_LOCAL, startTime).apply();
            callback.onSuccess();
        } catch (Exception e) {
            Log.e(TAG, "Response processing failed", e);
            callback.onError("Processing error: " + e.getMessage());
        }
    }

    private void handleSyncError(com.android.volley.VolleyError error, SyncCallback callback) {
        String msg = error.getMessage();
        if (error.networkResponse != null) msg = "HTTP " + error.networkResponse.statusCode;
        Log.e(TAG, "Network error: " + msg);
        callback.onError("Network error: " + msg);
    }

    private List<Task> getLocalChanges(long lastLocalSync) {
        List<Task> all = repository.getAll();
        List<Task> changes = new ArrayList<>();
        for (Task t : all) {
            if (lastLocalSync == 0 || t.getUpdatedAt() > lastLocalSync) {
                changes.add(t);
            }
        }
        return changes;
    }

    private void processResponse(org.json.JSONObject response) throws Exception {
        long serverTimestamp = response.getLong("serverTimestamp");
        org.json.JSONArray serverChanges = response.getJSONArray("serverChanges");

        for (int i = 0; i < serverChanges.length(); i++) {
            Task serverTask = gson.fromJson(serverChanges.getJSONObject(i).toString(), Task.class);
            applyServerTask(serverTask);
        }

        prefs.edit().putLong(KEY_LAST_SYNC_SERVER, serverTimestamp).apply();
    }

    private void applyServerTask(Task serverTask) {
        Task localTask = repository.getById(serverTask.getId());
        if (localTask == null) {
            repository.add(serverTask);
        } else if (serverTask.getUpdatedAt() > localTask.getUpdatedAt()) {
            repository.update(serverTask);
        }
    }
}
