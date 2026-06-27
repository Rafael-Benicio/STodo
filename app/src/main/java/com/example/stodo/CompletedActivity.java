package com.example.stodo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stodo.service.TaskService;
import com.example.stodo.sync.SyncManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

public class CompletedActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, STodoApplication.OnIncomingSyncListener {

    private static final long AUTO_SYNC_INTERVAL = 60000; // 60 segundos

    private TaskService taskService;
    private TaskAdapter adapter;
    private List<Task> completedTasks;
    private STodoApplication app;
    private boolean isAutoSyncEnabled = false;

    private final Handler autoSyncHandler = new Handler(Looper.getMainLooper());
    private final Handler syncDebounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncDebounceRunnable = new Runnable() {
        @Override
        public void run() {
            performSync();
        }
    };
    private final Runnable autoSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoSyncEnabled) {
                performSync();
            }
            autoSyncHandler.postDelayed(this, AUTO_SYNC_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        app = (STodoApplication) getApplication();
        taskService = app.getTaskService();
        isAutoSyncEnabled = getSharedPreferences("SyncPrefs", MODE_PRIVATE).getBoolean("auto_sync", false);

        taskService.checkAndUncheckTasks();

        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MenuItem autoSyncItem = toolbar.getMenu().findItem(R.id.action_auto_sync);
        if (autoSyncItem != null) autoSyncItem.setChecked(isAutoSyncEnabled);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                taskService.deleteCompletedTasks();
                refreshTasks();
                triggerImmediateSync();
                return true;
            } else if (item.getItemId() == R.id.action_sync) {
                performSync();
                return true;
            } else if (item.getItemId() == R.id.action_auto_sync) {
                toggleAutoSync(item);
                return true;
            }
            return false;
        });
    }

    private void toggleAutoSync(MenuItem item) {
        isAutoSyncEnabled = !isAutoSyncEnabled;
        item.setChecked(isAutoSyncEnabled);
        getSharedPreferences("SyncPrefs", MODE_PRIVATE).edit().putBoolean("auto_sync", isAutoSyncEnabled).apply();
        
        if (isAutoSyncEnabled) {
            Toast.makeText(this, "Auto-Sync ativado", Toast.LENGTH_SHORT).show();
            startAutoSync();
        } else {
            Toast.makeText(this, "Auto-Sync desativado", Toast.LENGTH_SHORT).show();
            stopAutoSync();
        }
    }

    private void startAutoSync() {
        autoSyncHandler.removeCallbacks(autoSyncRunnable);
        autoSyncHandler.postDelayed(autoSyncRunnable, AUTO_SYNC_INTERVAL);
    }

    private void stopAutoSync() {
        autoSyncHandler.removeCallbacks(autoSyncRunnable);
    }

    private void performSync() {
        List<STodoApplication.DiscoveredServer> servers = app.getDiscoveredServers();
        if (servers.isEmpty()) {
            return;
        }
        for (STodoApplication.DiscoveredServer server : servers) {
            syncWithServer(server);
        }
    }

    private void syncWithServer(STodoApplication.DiscoveredServer server) {
        app.getSyncManager().sync(server.getHost(), server.getPort(), new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> refreshTasks());
            }
            @Override
            public void onError(String message) {
                Log.e("CompletedActivity", "Sync error with " + server.getName() + ": " + message);
            }
        });
    }

    private void triggerImmediateSync() {
        if (isAutoSyncEnabled) {
            syncDebounceHandler.removeCallbacks(syncDebounceRunnable);
            syncDebounceHandler.postDelayed(syncDebounceRunnable, 500);
        }
    }

    @Override
    public void onIncomingSync() {
        runOnUiThread(() -> refreshTasks());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTasks();
        app.registerIncomingSyncListener(this);
        if (adapter != null) adapter.startCountdown();
        if (isAutoSyncEnabled) startAutoSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSync();
        app.unregisterIncomingSyncListener(this);
        if (adapter != null) adapter.stopCountdown();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewCompleted);
        completedTasks = taskService.getCompletedTasks();
        adapter = new TaskAdapter(completedTasks, this, taskService);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                triggerImmediateSync();
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_completed);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_tasks) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return item.getItemId() == R.id.navigation_completed;
        });
    }

    private void showEditTaskDialog(Task task) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText titleEt = view.findViewById(R.id.editTextTaskTitle);
        EditText minsEt = view.findViewById(R.id.editTextUncheckMinutes);
        
        titleEt.setText(task.getTitle());
        if (task.getAutoUncheckMinutes() > 0) minsEt.setText(String.valueOf(task.getAutoUncheckMinutes()));

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Update", (d, w) -> {
                String title = titleEt.getText().toString().trim();
                if (!title.isEmpty()) {
                    int mins = 0;
                    try { mins = Integer.parseInt(minsEt.getText().toString()); } catch (Exception e) {}
                    task.setTitle(title);
                    task.setAutoUncheckMinutes(mins);
                    task.setUncheckTimestamp(0);
                    taskService.updateTask(task);
                    refreshTasks();
                    triggerImmediateSync();
                }
            })
            .show();
    }

    private void refreshTasks() {
        completedTasks.clear();
        completedTasks.addAll(taskService.getCompletedTasks());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskClick(Task task) {}

    @Override
    public void onTaskStatusChanged(Task task) {
        taskService.updateTask(task);
        if (!task.isCompleted()) refreshTasks();
        triggerImmediateSync();
    }

    @Override
    public void onTaskEdit(Task task) { showEditTaskDialog(task); }

    @Override
    public void onTaskDelete(Task task) {
        taskService.deleteTask(task.getId());
        refreshTasks();
        triggerImmediateSync();
    }
}
