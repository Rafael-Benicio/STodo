package com.example.stodo.sync;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * NetworkServiceDiscovery manages the discovery and registration of STodo services.
 * It allows mobile devices to act as both servers and clients on the local network.
 */
public class NetworkServiceDiscovery {
    private static final String TAG = "NSD";
    private static final String SERVICE_TYPE = "_stodo._tcp.";
    
    private final NsdManager nsdManager;
    private final DiscoveryCallback callback;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private String registeredServiceName;
    private boolean isDiscovering = false;

    /**
     * Callback interface for discovery events.
     * Example:
     * DiscoveryCallback cb = new DiscoveryCallback() { ... };
     */
    public interface DiscoveryCallback {
        /**
         * Triggered when a peer or hub service is found and resolved.
         * Example: onServerFound("STodo Hub", "192.168.1.5", 8080);
         */
        void onServerFound(String name, String host, int port);

        /**
         * Triggered when a previously resolved service is lost from the network.
         * Example: onServerLost("STodo Hub");
         */
        void onServerLost(String name);
    }

    /**
     * Constructor for NetworkServiceDiscovery.
     * Example: NetworkServiceDiscovery nsd = new NetworkServiceDiscovery(context, callback);
     */
    public NetworkServiceDiscovery(Context context, DiscoveryCallback callback) {
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.callback = callback;
    }

    /**
     * Starts scanning for the Desktop Hub or other Mobile peers on the local network.
     * Example: nsd.startDiscovery();
     */
    public void startDiscovery() {
        if (isDiscovering) return;
        initializeDiscoveryListener();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        isDiscovering = true;
        Log.d(TAG, "Starting network discovery...");
    }

    /**
     * Stops the active network discovery scan.
     * Example: nsd.stopDiscovery();
     */
    public void stopDiscovery() {
        if (!isDiscovering) return;
        nsdManager.stopServiceDiscovery(discoveryListener);
        isDiscovering = false;
        Log.d(TAG, "Discovery stopped.");
    }

    /**
     * Registers the local mobile service on the local network via mDNS.
     * Example: nsd.registerService("STodo Mobile (Pixel 6)", 8080);
     */
    public void registerService(String serviceName, int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);
        initializeRegistrationListener();
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    /**
     * Unregisters the local mobile service from the network.
     * Example: nsd.unregisterService();
     */
    public void unregisterService() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null;
        }
    }

    private void initializeRegistrationListener() {
        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Registration failed: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed: " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                registeredServiceName = serviceInfo.getServiceName();
                Log.i(TAG, "Service registered: " + registeredServiceName);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Service unregistered: " + serviceInfo.getServiceName());
            }
        };
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String type, int err) { handleDiscoveryError(err); }

            @Override
            public void onStopDiscoveryFailed(String type, int err) { handleDiscoveryError(err); }

            @Override
            public void onDiscoveryStarted(String type) { Log.d(TAG, "Discovery active"); }

            @Override
            public void onDiscoveryStopped(String type) { Log.i(TAG, "Discovery inactive"); }

            @Override
            public void onServiceFound(NsdServiceInfo info) { handleServiceFound(info); }

            @Override
            public void onServiceLost(NsdServiceInfo info) { handleServiceLost(info); }
        };
    }

    private void handleDiscoveryError(int err) {
        Log.e(TAG, "Discovery error: " + err);
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    private void handleServiceFound(NsdServiceInfo info) {
        if (info.getServiceType().startsWith(SERVICE_TYPE)) {
            resolveHubService(info);
        }
    }

    private void handleServiceLost(NsdServiceInfo info) {
        Log.i(TAG, "Service lost: " + info.getServiceName());
        callback.onServerLost(info.getServiceName());
    }

    private void resolveHubService(NsdServiceInfo info) {
        nsdManager.resolveService(info, new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                String name = serviceInfo.getServiceName();
                if (registeredServiceName != null && registeredServiceName.equals(name)) {
                    Log.d(TAG, "Ignoring self-discovered service: " + name);
                    return;
                }
                String host = serviceInfo.getHost().getHostAddress();
                int port = serviceInfo.getPort();
                Log.d(TAG, "Service Resolved: " + name + " at " + host + ":" + port);
                callback.onServerFound(name, host, port);
            }
        });
    }
}
