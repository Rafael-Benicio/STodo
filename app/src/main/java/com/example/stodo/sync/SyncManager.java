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

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final int PROTOCOL_VERSION = 1;
    private static final String PREFS_NAME = "SyncPrefs";
    private static final String KEY_LAST_SYNC_SERVER = "lastServerTimestamp";
    private static final String KEY_LAST_SYNC_LOCAL = "lastLocalTimestamp";

    private final Context context;
    private final TaskService taskService;
    private final TaskRepository repository;
    private final RequestQueue requestQueue;
    private final Gson gson;
    private final SharedPreferences prefs;

    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    public SyncManager(Context context, TaskService taskService, TaskRepository repository) {
        this.context = context;
        this.taskService = taskService;
        this.repository = repository;
        this.requestQueue = Volley.newRequestQueue(context);
        this.gson = new Gson();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void sync(String host, int port, SyncCallback callback) {
        final long startTime = System.currentTimeMillis();
        final long lastServerSync = prefs.getLong(KEY_LAST_SYNC_SERVER, 0);
        final long lastLocalSync = prefs.getLong(KEY_LAST_SYNC_LOCAL, 0);
        
        List<Task> localChanges = getLocalChanges(lastLocalSync);

        Log.d(TAG, "Iniciando sync. LocalSync: " + lastLocalSync + " | ServerSync: " + lastServerSync);
        
        String formattedHost = host.contains(":") ? "[" + host + "]" : host;

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("protocolVersion", PROTOCOL_VERSION);
            requestBody.addProperty("lastSyncTimestamp", lastServerSync);
            
            JsonArray changesArray = new JsonArray();
            for (Task task : localChanges) {
                changesArray.add(gson.toJsonTree(task));
            }
            requestBody.add("clientChanges", changesArray);

            String url = String.format("http://%s:%d/api/v1/sync", formattedHost, port);
            Log.d(TAG, "Enviando " + localChanges.size() + " mudanças para: " + url);

            org.json.JSONObject jsonObject = new org.json.JSONObject(requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        processResponse(response);
                        // Atualizamos a âncora local com o tempo de início do sync
                        prefs.edit().putLong(KEY_LAST_SYNC_LOCAL, startTime).apply();
                        callback.onSuccess();
                    } catch (Exception e) {
                        Log.e(TAG, "Erro processando resposta", e);
                        callback.onError("Erro no processamento: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = error.getMessage();
                    if (error.networkResponse != null) {
                        errorMsg = "Status: " + error.networkResponse.statusCode;
                    }
                    Log.e(TAG, "Erro rede: " + errorMsg);
                    callback.onError("Erro na rede: " + errorMsg);
                }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            Log.e(TAG, "Erro fatal", e);
            callback.onError("Erro interno: " + e.getMessage());
        }
    }

    private List<Task> getLocalChanges(long lastLocalSync) {
        List<Task> all = repository.getAll();
        List<Task> changes = new ArrayList<>();
        for (Task t : all) {
            // Enviamos tudo que mudou localmente desde a última vez que enviamos algo com sucesso
            if (lastLocalSync == 0 || t.getUpdatedAt() > lastLocalSync) {
                changes.add(t);
            }
        }
        return changes;
    }

    private void processResponse(org.json.JSONObject response) throws Exception {
        long serverTimestamp = response.getLong("serverTimestamp");
        org.json.JSONArray serverChanges = response.getJSONArray("serverChanges");
        Log.d(TAG, "Recebidas " + serverChanges.length() + " mudanças do servidor");

        for (int i = 0; i < serverChanges.length(); i++) {
            org.json.JSONObject taskJson = serverChanges.getJSONObject(i);
            Task serverTask = gson.fromJson(taskJson.toString(), Task.class);
            
            Task localTask = repository.getById(serverTask.getId());
            if (localTask == null) {
                repository.add(serverTask);
            } else {
                if (serverTask.getUpdatedAt() > localTask.getUpdatedAt()) {
                    repository.update(serverTask);
                }
            }
        }

        prefs.edit().putLong(KEY_LAST_SYNC_SERVER, serverTimestamp).apply();
    }
}
