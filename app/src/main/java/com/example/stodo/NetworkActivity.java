package com.example.stodo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;

/**
 * Activity for displaying discovered peers on the network, running scan,
 * and pinging peer devices to verify their reachability status.
 */
public class NetworkActivity extends AppCompatActivity implements STodoApplication.OnIncomingSyncListener {
    private STodoApplication app;
    private PeerAdapter adapter;
    private List<STodoApplication.DiscoveredServer> peers;

    /**
     * Initializes activity layouts and UI components on creation.
     * Example: onCreate(savedInstanceState);
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        app = (STodoApplication) getApplication();
        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_scan) {
                app.restartSyncService();
                Toast.makeText(this, "Scanning network...", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPeers);
        peers = app.getDiscoveredServers();
        adapter = new PeerAdapter(peers, app.getSyncManager());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_network);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_tasks) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.navigation_completed) {
                startActivity(new Intent(this, CompletedActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return item.getItemId() == R.id.navigation_network;
        });
    }

    /**
     * Refreshes the discovered server list upon incoming sync events.
     * Example: onIncomingSync();
     */
    @Override
    public void onIncomingSync() {
        runOnUiThread(() -> refreshPeers());
    }

    private void refreshPeers() {
        peers.clear();
        peers.addAll(app.getDiscoveredServers());
        adapter.notifyDataSetChanged();
    }

    /**
     * Registers incoming sync callbacks and refreshes peer lists on resume.
     * Example: onResume();
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshPeers();
        app.registerIncomingSyncListener(this);
    }

    /**
     * Unregisters listeners when activity goes out of scope.
     * Example: onPause();
     */
    @Override
    protected void onPause() {
        super.onPause();
        app.unregisterIncomingSyncListener(this);
    }
}
