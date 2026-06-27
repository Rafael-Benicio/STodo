package com.example.stodo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.stodo.repository.TaskRepository;
import com.example.stodo.sync.NetworkServiceDiscovery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, STodoApplication.OnIncomingSyncListener {

    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private static final long AUTO_SYNC_INTERVAL = 60000; // 60 segundos

    private TaskService taskService;
    private TaskRepository taskRepository;
    private TaskAdapter adapter;
    private List<Task> activeTasks;
    
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
        Log.d("MainActivity", "### STODO INICIALIZADO - VERSÃO SYNC ###");
        setContentView(R.layout.activity_main);

        // Carregar preferência salva
        isAutoSyncEnabled = getSharedPreferences("SyncPrefs", MODE_PRIVATE).getBoolean("auto_sync", false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        
        // Inicializar estado do toggle no menu da Toolbar
        MenuItem autoSyncItem = toolbar.getMenu().findItem(R.id.action_auto_sync);
        if (autoSyncItem != null) {
            autoSyncItem.setChecked(isAutoSyncEnabled);
        }

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sync) {
                performSync();
                return true;
            } else if (item.getItemId() == R.id.action_auto_sync) {
                toggleAutoSync(item);
                return true;
            }
            return false;
        });

        app = (STodoApplication) getApplication();
        taskService = app.getTaskService();
        taskRepository = app.getTaskRepository();

        taskService.checkAndUncheckTasks();
        
        setupRecyclerView();
        setupBottomNavigation();
        setupFab();

        checkPermissionsAndStartDiscovery();
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

    private void checkPermissionsAndStartDiscovery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
            Manifest.permission.NEARBY_WIFI_DEVICES : Manifest.permission.ACCESS_FINE_LOCATION;
            
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
        } else {
            app.getNsd().startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            app.getNsd().startDiscovery();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tasks_top_menu, menu);
        MenuItem autoSyncItem = menu.findItem(R.id.action_auto_sync);
        if (autoSyncItem != null) {
            autoSyncItem.setChecked(isAutoSyncEnabled);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            performSync();
            return true;
        } else if (item.getItemId() == R.id.action_auto_sync) {
            toggleAutoSync(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                Log.e("MainActivity", "Sync error with " + server.getName() + ": " + message);
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
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTasks);
        activeTasks = taskService.getActiveTasks();
        adapter = new TaskAdapter(activeTasks, this, taskService);
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
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_tasks);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_completed) {
                startActivity(new Intent(this, CompletedActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return item.getItemId() == R.id.navigation_tasks;
        });
    }

    private void setupFab() {
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText titleEt = view.findViewById(R.id.editTextTaskTitle);
        EditText minsEt = view.findViewById(R.id.editTextUncheckMinutes);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save", (d, w) -> {
                String title = titleEt.getText().toString().trim();
                if (!title.isEmpty()) {
                    int mins = 0;
                    try { mins = Integer.parseInt(minsEt.getText().toString()); } catch (Exception e) {}
                    taskService.addTask(title, mins);
                    refreshTasks();
                    triggerImmediateSync();
                }
            })
            .show();
    }

    private void showEditTaskDialog(Task task) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText titleEt = view.findViewById(R.id.editTextTaskTitle);
        EditText editMinutes = view.findViewById(R.id.editTextUncheckMinutes);
        
        titleEt.setText(task.getTitle());
        if (task.getAutoUncheckMinutes() > 0) editMinutes.setText(String.valueOf(task.getAutoUncheckMinutes()));

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Update", (d, w) -> {
                String title = titleEt.getText().toString().trim();
                if (!title.isEmpty()) {
                    int mins = 0;
                    try { mins = Integer.parseInt(editMinutes.getText().toString()); } catch (Exception e) {}
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
        activeTasks.clear();
        activeTasks.addAll(taskService.getActiveTasks());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskStatusChanged(Task task) {
        taskService.updateTask(task);
        if (task.isCompleted()) refreshTasks();
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

    @Override
    public void onTaskClick(Task task) {}
}
