package com.example.stodo;

import android.app.Application;
import com.example.stodo.repository.SqliteTaskRepository;
import com.example.stodo.service.TaskService;
import com.example.stodo.service.TaskServiceImpl;

public class STodoApplication extends Application {
    private TaskService taskService;

    @Override
    public void onCreate() {
        super.onCreate();
        taskService = new TaskServiceImpl(new SqliteTaskRepository(this));
    }

    public TaskService getTaskService() {
        return taskService;
    }
}