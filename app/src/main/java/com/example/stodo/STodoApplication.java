package com.example.stodo;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import com.example.stodo.repository.SqliteTaskRepository;
import com.example.stodo.repository.TaskRepository;
import com.example.stodo.service.TaskService;
import com.example.stodo.service.TaskServiceImpl;
import com.example.stodo.sync.NetworkServiceDiscovery;
import com.example.stodo.sync.SyncManager;
import com.example.stodo.sync.SyncServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STodoApplication manages the lifecycle of local repositories, services,
 * the local embedded sync server, and network discovery for P2P operations.
 */
public class STodoApplication extends Application {
    private static final String TAG = "STodoApp";
    private TaskService taskService;
    private TaskRepository taskRepository;
    
    private NetworkServiceDiscovery nsd;
    private SyncManager syncManager;
    private SyncServer syncServer;
    
    private final Set<DiscoveredServer> discoveredServers = Collections.synchronizedSet(new HashSet<>());

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
        
        startLocalServer();
        initializeNsd();
    }

    private void startLocalServer() {
        syncServer = new SyncServer(taskRepository);
        int localPort = syncServer.start();
        if (localPort != -1) {
            String serviceName = "STodo Mobile (" + Build.MODEL + ")";
            nsd = new NetworkServiceDiscovery(this, createDiscoveryCallback());
            nsd.registerService(serviceName, localPort);
        }
    }

    private void initializeNsd() {
        if (nsd == null) {
            nsd = new NetworkServiceDiscovery(this, createDiscoveryCallback());
        }
        nsd.startDiscovery();
    }

    private NetworkServiceDiscovery.DiscoveryCallback createDiscoveryCallback() {
        return new NetworkServiceDiscovery.DiscoveryCallback() {
            @Override
            public void onServerFound(String name, String host, int port) {
                Log.d(TAG, "Server discovered: " + name + " -> " + host + ":" + port);
                discoveredServers.add(new DiscoveredServer(name, host, port));
            }

            @Override
            public void onServerLost(String name) {
                Log.d(TAG, "Server lost: " + name);
                discoveredServers.remove(new DiscoveredServer(name, "", 0));
            }
        };
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (syncServer != null) {
            syncServer.stop();
        }
        if (nsd != null) {
            nsd.unregisterService();
            nsd.stopDiscovery();
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
     * Returns the active NetworkServiceDiscovery controller.
     * Example: NetworkServiceDiscovery nsd = app.getNsd();
     */
    public NetworkServiceDiscovery getNsd() { return nsd; }

    /**
     * Returns a thread-safe list copy of all active discovered servers.
     * Example: List<DiscoveredServer> list = app.getDiscoveredServers();
     */
    public List<DiscoveredServer> getDiscoveredServers() {
        return new ArrayList<>(discoveredServers);
    }
}
