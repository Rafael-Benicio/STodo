package com.example.stodo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

        // Check for auto-uncheck tasks
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
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTasks);
        activeTasks = taskService.getActiveTasks();
        adapter = new TaskAdapter(activeTasks, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
                        long uncheckTimestamp = 0;
                        String minsStr = editMinutes.getText().toString().trim();
                        if (!minsStr.isEmpty()) {
                            // If it's already checked (which it isn't here, but for consistency), 
                            // we would set the timestamp. For new tasks, we just store the duration 
                            // as negative or handle it separately.
                            // Let's assume the user wants it to uncheck after X minutes *once it is checked*.
                            // So we store the duration (minutes) in the uncheckTimestamp field as a negative value 
                            // to indicate it's a duration, OR we just use it when the checkbox is clicked.
                            // Better: Let's store the minutes in a simple way.
                            try {
                                uncheckTimestamp = -Long.parseLong(minsStr); // Negative indicates duration
                            } catch (NumberFormatException ignored) {}
                        }
                        taskService.addTask(title, uncheckTimestamp);
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
        if (task.getUncheckTimestamp() < 0) {
            editMinutes.setText(String.valueOf(-task.getUncheckTimestamp()));
        } else if (task.getUncheckTimestamp() > 0 && task.isCompleted()) {
            // It's already scheduled to uncheck, show remaining minutes approx
            long remainingMins = (task.getUncheckTimestamp() - System.currentTimeMillis()) / 60000;
            editMinutes.setText(String.valueOf(Math.max(1, remainingMins)));
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty()) {
                        long uncheckTimestamp = 0;
                        String minsStr = editMinutes.getText().toString().trim();
                        if (!minsStr.isEmpty()) {
                            try {
                                uncheckTimestamp = -Long.parseLong(minsStr);
                            } catch (NumberFormatException ignored) {}
                        }
                        Task updatedTask = new Task(task.getId(), title, task.isCompleted(), uncheckTimestamp);
                        taskService.updateTask(updatedTask);
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
        if (task.isCompleted() && task.getUncheckTimestamp() < 0) {
            // Set the actual timestamp
            long minutes = -task.getUncheckTimestamp();
            task.setUncheckTimestamp(System.currentTimeMillis() + (minutes * 60000));
        } else if (!task.isCompleted()) {
            // If unchecked manually, reset to duration if it was scheduled
            // Actually, keep it as negative so it can be used again.
            // But how do we know the original duration? 
            // Let's say we always store duration as negative in the DB when task is incomplete.
        }
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