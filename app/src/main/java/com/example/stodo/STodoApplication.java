package com.example.stodo;

import android.app.Application;
import android.util.Log;
import com.example.stodo.repository.SqliteTaskRepository;
import com.example.stodo.repository.TaskRepository;
import com.example.stodo.service.TaskService;
import com.example.stodo.service.TaskServiceImpl;
import com.example.stodo.sync.NetworkServiceDiscovery;
import com.example.stodo.sync.SyncManager;

public class STodoApplication extends Application {
    private static final String TAG = "STodoApp";
    private TaskService taskService;
    private TaskRepository taskRepository;
    
    private NetworkServiceDiscovery nsd;
    private SyncManager syncManager;
    private String serverHost;
    private int serverPort;

    @Override
    public void onCreate() {
        super.onCreate();
        taskRepository = new SqliteTaskRepository(this);
        taskService = new TaskServiceImpl(taskRepository);
        
        syncManager = new SyncManager(this, taskService, taskRepository);
        nsd = new NetworkServiceDiscovery(this, (host, port) -> {
            Log.d(TAG, "Desktop Hub descoberto globalmente: " + host + ":" + port);
            serverHost = host;
            serverPort = port;
        });
        
        // Iniciar descoberta global
        nsd.startDiscovery();
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public NetworkServiceDiscovery getNsd() {
        return nsd;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}
