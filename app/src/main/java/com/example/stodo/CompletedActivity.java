package com.example.stodo;

import android.content.Intent;
import android.os.Bundle;
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

        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
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

    private void refreshTasks() {
        completedTasks.clear();
        completedTasks.addAll(taskService.getCompletedTasks());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskClick(Task task) {
        // Handle task click
    }

    @Override
    public void onTaskStatusChanged(Task task) {
        taskService.updateTask(task);
        // If it was unchecked, it should disappear from this list
        if (!task.isCompleted()) {
            refreshTasks();
        }
    }
}