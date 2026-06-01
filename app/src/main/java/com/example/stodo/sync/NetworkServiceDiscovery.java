package com.example.stodo.sync;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * NetworkServiceDiscovery manages the discovery of the STodo Desktop Hub on the local network.
 * It uses the NsdManager to scan for services of type _stodo._tcp.
 */
public class NetworkServiceDiscovery {
    private static final String TAG = "NSD";
    private static final String SERVICE_TYPE = "_stodo._tcp";
    
    private final NsdManager nsdManager;
    private final OnServerFoundListener listener;
    private NsdManager.DiscoveryListener discoveryListener;
    private boolean isDiscovering = false;

    /**
     * Interface for notifying when a STodo Hub is discovered and resolved.
     */
    public interface OnServerFoundListener {
        void onServerFound(String host, int port);
    }

    public NetworkServiceDiscovery(Context context, OnServerFoundListener listener) {
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.listener = listener;
    }

    /**
     * Starts scanning for the Desktop Hub on the local network.
     */
    public void startDiscovery() {
        if (isDiscovering) return;
        
        initializeDiscoveryListener();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        isDiscovering = true;
        Log.d(TAG, "Starting network discovery...");
    }

    /**
     * Stops the active scanning process to save resources.
     */
    public void stopDiscovery() {
        if (isDiscovering) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            isDiscovering = false;
            Log.d(TAG, "Discovery stopped.");
        }
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String type, int err) {
                Log.e(TAG, "Start failed: " + err);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String type, int err) {
                Log.e(TAG, "Stop failed: " + err);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String type) { Log.d(TAG, "Discovery active"); }

            @Override
            public void onDiscoveryStopped(String type) { Log.i(TAG, "Discovery inactive"); }

            @Override
            public void onServiceFound(NsdServiceInfo info) {
                if (info.getServiceType().startsWith(SERVICE_TYPE)) resolveHubService(info);
            }

            @Override
            public void onServiceLost(NsdServiceInfo info) { Log.e(TAG, "Service lost: " + info); }
        };
    }

    private void resolveHubService(NsdServiceInfo info) {
        nsdManager.resolveService(info, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Hub Resolved: " + serviceInfo.getHost().getHostAddress());
                listener.onServerFound(serviceInfo.getHost().getHostAddress(), serviceInfo.getPort());
            }
        });
    }
}
