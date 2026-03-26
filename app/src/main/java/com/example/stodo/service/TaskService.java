package com.example.stodo.service;

import com.example.stodo.Task;
import java.util.List;

public interface TaskService {
    List<Task> getActiveTasks();
    List<Task> getCompletedTasks();
    void addTask(String title);
    void updateTask(Task task);
    void deleteTask(int id);
    void deleteCompletedTasks();
}