package com.example.stodo;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.example.stodo.repository.SqliteTaskRepository;
import com.example.stodo.repository.TaskRepository;
import com.example.stodo.service.TaskService;
import com.example.stodo.service.TaskServiceImpl;
import com.example.stodo.sync.SyncManager;
import com.example.stodo.sync.SyncService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STodoApplication manages the lifecycle of local repositories, services,
 * and handles the lifecycle of the SyncService for P2P background sync.
 */
public class STodoApplication extends Application {
    private TaskService taskService;
    private TaskRepository taskRepository;
    private SyncManager syncManager;
    
    private final Set<DiscoveredServer> discoveredServers = Collections.synchronizedSet(new HashSet<>());
    private final List<OnIncomingSyncListener> incomingSyncListeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Interface to receive notifications for incoming sync events.
     */
    public interface OnIncomingSyncListener {
        /**
         * Triggered when an incoming sync finishes.
         * Example: listener.onIncomingSync();
         */
        void onIncomingSync();
    }

    /**
     * DiscoveredServer represents a peer or hub found on the local network.
     */
    public static class DiscoveredServer {
        private final String name;
        private final String host;
        private final int port;

        /**
         * Constructs a DiscoveredServer representation.
         * Example: DiscoveredServer server = new DiscoveredServer("Hub", "192.168.1.5", 8080);
         */
        public DiscoveredServer(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        /**
         * Returns the name of the discovered service.
         * Example: String name = server.getName();
         */
        public String getName() { return name; }

        /**
         * Returns the host address of the discovered service.
         * Example: String host = server.getHost();
         */
        public String getHost() { return host; }

        /**
         * Returns the port of the discovered service.
         * Example: int port = server.getPort();
         */
        public int getPort() { return port; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DiscoveredServer)) return false;
            DiscoveredServer that = (DiscoveredServer) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() { return name.hashCode(); }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        taskRepository = new SqliteTaskRepository(this);
        taskService = new TaskServiceImpl(taskRepository);
        syncManager = new SyncManager(this, taskService, taskRepository);
        startSyncService();
    }

    /**
     * Starts the background SyncService to enable persistent synchronization.
     * Example: app.startSyncService();
     */
    public void startSyncService() {
        Intent intent = new Intent(this, SyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * Stops the background SyncService.
     * Example: app.stopSyncService();
     */
    public void stopSyncService() {
        Intent intent = new Intent(this, SyncService.class);
        stopService(intent);
    }

    /**
     * Dispatches notifications to all registered UI sync listeners on the Main Thread.
     * Example: app.notifyIncomingSync();
     */
    public void notifyIncomingSync() {
        new Handler(Looper.getMainLooper()).post(() -> {
            synchronized (incomingSyncListeners) {
                for (OnIncomingSyncListener listener : incomingSyncListeners) {
                    listener.onIncomingSync();
                }
            }
        });
    }

    /**
     * Adds a newly discovered server to the list of active peers.
     * Example: app.addDiscoveredServer("Mobile", "192.168.1.10", 8080);
     */
    public void addDiscoveredServer(String name, String host, int port) {
        // Track the newly resolved peer
        discoveredServers.add(new DiscoveredServer(name, host, port));
    }

    /**
     * Removes a peer that is no longer available on the network.
     * Example: app.removeDiscoveredServer("Mobile");
     */
    public void removeDiscoveredServer(String name) {
        // Untrack the lost peer
        discoveredServers.remove(new DiscoveredServer(name, "", 0));
    }

    /**
     * Registers a listener to be notified when an incoming sync completes.
     * Example: app.registerIncomingSyncListener(listener);
     */
    public void registerIncomingSyncListener(OnIncomingSyncListener listener) {
        if (listener != null) {
            incomingSyncListeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered listener.
     * Example: app.unregisterIncomingSyncListener(listener);
     */
    public void unregisterIncomingSyncListener(OnIncomingSyncListener listener) {
        if (listener != null) {
            incomingSyncListeners.remove(listener);
        }
    }

    /**
     * Returns the active TaskService implementation instance.
     * Example: TaskService service = app.getTaskService();
     */
    public TaskService getTaskService() { return taskService; }

    /**
     * Returns the active TaskRepository implementation instance.
     * Example: TaskRepository repo = app.getTaskRepository();
     */
    public TaskRepository getTaskRepository() { return taskRepository; }

    /**
     * Returns the active SyncManager instance.
     * Example: SyncManager manager = app.getSyncManager();
     */
    public SyncManager getSyncManager() { return syncManager; }

    /**
     * Returns a thread-safe list copy of all active discovered servers.
     * Example: List<DiscoveredServer> list = app.getDiscoveredServers();
     */
    public List<DiscoveredServer> getDiscoveredServers() {
        return new ArrayList<>(discoveredServers);
    }
}
