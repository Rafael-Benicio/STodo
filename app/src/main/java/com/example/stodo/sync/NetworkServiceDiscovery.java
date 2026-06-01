package com.example.stodo.sync;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NetworkServiceDiscovery {
    private static final String TAG = "NSD";
    private static final String SERVICE_TYPE = "_stodo._tcp";
    
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private OnServerFoundListener listener;
    private boolean isDiscovering = false;

    public interface OnServerFoundListener {
        void onServerFound(String host, int port);
    }

    public NetworkServiceDiscovery(Context context, OnServerFoundListener listener) {
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.listener = listener;
    }

    public void startDiscovery() {
        if (isDiscovering) return;
        
        initializeDiscoveryListener();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        isDiscovering = true;
    }

    public void stopDiscovery() {
        if (isDiscovering) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            isDiscovering = false;
        }
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo);
                // O Android às vezes retorna com ou sem o ponto final, vamos ser flexíveis
                String foundType = serviceInfo.getServiceType();
                if (foundType.startsWith(SERVICE_TYPE)) {
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed: " + errorCode);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                            String host = serviceInfo.getHost().getHostAddress();
                            int port = serviceInfo.getPort();
                            listener.onServerFound(host, port);
                        }
                    });
                } else {
                    Log.d(TAG, "Unknown Service Type: " + foundType);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "service lost" + serviceInfo);
            }
        };
    }
}
