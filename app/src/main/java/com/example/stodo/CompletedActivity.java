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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.List;

public class CompletedActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private TaskService taskService;
    private TaskAdapter adapter;
    private List<Task> completedTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        STodoApplication app = (STodoApplication) getApplication();
        taskService = app.getTaskService();

        taskService.checkAndUncheckTasks();

        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        taskService.checkAndUncheckTasks();
        refreshTasks();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                taskService.deleteCompletedTasks();
                refreshTasks();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewCompleted);
        completedTasks = taskService.getCompletedTasks();
        adapter = new TaskAdapter(completedTasks, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_completed);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_tasks) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.navigation_completed) {
                return true;
            }
            return false;
        });
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
        completedTasks.clear();
        completedTasks.addAll(taskService.getCompletedTasks());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskClick(Task task) {
    }

    @Override
    public void onTaskStatusChanged(Task task) {
        taskService.updateTask(task);
        if (!task.isCompleted()) {
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