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

        setupRecyclerView();
        setupBottomNavigation();
        setupFab();
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

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty()) {
                        taskService.addTask(title);
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
        TextView titleView = dialogView.findViewById(android.R.id.title);
        // The dialog layout has a TextView with text "New Task", let's find it by index or ID if it had one
        // Better yet, let's find the first TextView which is our title
        if (dialogView instanceof android.view.ViewGroup) {
            View firstChild = ((android.view.ViewGroup) dialogView).getChildAt(0);
            if (firstChild instanceof TextView) {
                ((TextView) firstChild).setText("Edit Task");
            }
        }
        
        editText.setText(task.getTitle());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = editText.getText().toString().trim();
                    if (!title.isEmpty()) {
                        Task updatedTask = new Task(task.getId(), title, task.isCompleted());
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