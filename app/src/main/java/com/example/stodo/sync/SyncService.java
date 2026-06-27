package com.example.stodo.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.stodo.R;
import com.example.stodo.STodoApplication;

/**
 * SyncService runs in the background as a Foreground Service.
 * It manages the HTTP SyncServer and NetworkServiceDiscovery to enable P2P sync.
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final String CHANNEL_ID = "sync_channel";
    private static final int NOTIFICATION_ID = 101;

    private STodoApplication app;
    private SyncServer syncServer;
    private NetworkServiceDiscovery nsd;

    @Override
    public void onCreate() {
        super.onCreate();
        app = (STodoApplication) getApplication();
        startForegroundNotification();
        initializeSyncServer();
        initializeNsd();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "SyncService started command.");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (syncServer != null) {
            syncServer.stop();
        }
        if (nsd != null) {
            nsd.unregisterService();
            nsd.stopDiscovery();
        }
        Log.i(TAG, "SyncService destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Binding is not supported
        return null;
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Sync Status", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("STodo Sync Active")
            .setContentText("Listening for peer updates on LAN")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        startForeground(NOTIFICATION_ID, n);
    }

    private void initializeSyncServer() {
        syncServer = new SyncServer(app.getTaskRepository());
        syncServer.setOnSyncCompleteListener(app::notifyIncomingSync);
        syncServer.start();
    }

    private void initializeNsd() {
        nsd = new NetworkServiceDiscovery(this, createDiscoveryCallback());
        int port = syncServer.getPort();
        if (port != -1) {
            String serviceName = "STodo Mobile (" + Build.MODEL + ")";
            nsd.registerService(serviceName, port);
        }
        nsd.startDiscovery();
    }

    private NetworkServiceDiscovery.DiscoveryCallback createDiscoveryCallback() {
        return new NetworkServiceDiscovery.DiscoveryCallback() {
            @Override
            public void onServerFound(String name, String host, int port) {
                Log.d(TAG, "Peer discovered: " + name + " -> " + host + ":" + port);
                app.addDiscoveredServer(name, host, port);
                triggerPeerSync(name, host, port);
            }

            @Override
            public void onServerLost(String name) {
                Log.d(TAG, "Peer lost: " + name);
                app.removeDiscoveredServer(name);
            }
        };
    }

    private void triggerPeerSync(String name, String host, int port) {
        app.getSyncManager().sync(host, port, new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                app.notifyIncomingSync();
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Initial sync failed with " + name + ": " + message);
            }
        });
    }
}
