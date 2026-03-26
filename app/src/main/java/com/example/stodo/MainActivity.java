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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private TaskService taskService;
    private TaskAdapter adapter;
    private List<Task> activeTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        STodoApplication app = (STodoApplication) getApplication();
        taskService = app.getTaskService();

        taskService.checkAndUncheckTasks();

        setupRecyclerView();
        setupBottomNavigation();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        taskService.checkAndUncheckTasks();
        refreshTasks();
        if (adapter != null) {
            adapter.startCountdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.stopCountdown();
        }
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
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_tasks);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_tasks) {
                return true;
            } else if (itemId == R.id.navigation_completed) {
                startActivity(new Intent(getApplicationContext(), CompletedActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText editText = dialogView.findViewById(R.id.editTextTaskTitle);
        EditText editMinutes = dialogView.findViewById(R.id.editTextUncheckMinutes);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty()) {
                        int autoUncheckMinutes = 0;
                        String minsStr = editMinutes.getText().toString().trim();
                        if (!minsStr.isEmpty()) {
                            try {
                                autoUncheckMinutes = Integer.parseInt(minsStr);
                            } catch (NumberFormatException ignored) {}
                        }
                        taskService.addTask(title, autoUncheckMinutes);
                        refreshTasks();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showEditTaskDialog(Task task) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText editText = dialogView.findViewById(R.id.editTextTaskTitle);
        EditText editMinutes = dialogView.findViewById(R.id.editTextUncheckMinutes);
        
        if (dialogView instanceof android.view.ViewGroup) {
            View firstChild = ((android.view.ViewGroup) dialogView).getChildAt(0);
            if (firstChild instanceof TextView) {
                ((TextView) firstChild).setText("Edit Task");
            }
        }
        
        editText.setText(task.getTitle());
        if (task.getAutoUncheckMinutes() > 0) {
            editMinutes.setText(String.valueOf(task.getAutoUncheckMinutes()));
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty()) {
                        int autoUncheckMinutes = 0;
                        String minsStr = editMinutes.getText().toString().trim();
                        if (!minsStr.isEmpty()) {
                            try {
                                autoUncheckMinutes = Integer.parseInt(minsStr);
                            } catch (NumberFormatException ignored) {}
                        }
                        task.setTitle(title);
                        task.setAutoUncheckMinutes(autoUncheckMinutes);
                        task.setUncheckTimestamp(0); 
                        taskService.updateTask(task);
                        refreshTasks();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void refreshTasks() {
        activeTasks.clear();
        activeTasks.addAll(taskService.getActiveTasks());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskClick(Task task) {
    }

    @Override
    public void onTaskStatusChanged(Task task) {
        taskService.updateTask(task);
        if (task.isCompleted()) {
            refreshTasks();
        }
    }

    @Override
    public void onTaskEdit(Task task) {
        showEditTaskDialog(task);
    }

    @Override
    public void onTaskDelete(Task task) {
        taskService.deleteTask(task.getId());
        refreshTasks();
    }
}