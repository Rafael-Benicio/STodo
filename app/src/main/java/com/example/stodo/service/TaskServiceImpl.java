package com.example.stodo.service;

import com.example.stodo.Task;
import com.example.stodo.repository.TaskRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.util.UUID;

public class TaskServiceImpl implements TaskService {
    private final TaskRepository repository;

    public TaskServiceImpl(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Task> getActiveTasks() {
        return getFilteredAndSortedTasks(false);
    }

    @Override
    public List<Task> getCompletedTasks() {
        return getFilteredAndSortedTasks(true);
    }

    private List<Task> getFilteredAndSortedTasks(boolean completedStatus) {
        List<Task> filtered = new ArrayList<>();
        for (Task task : repository.getAll()) {
            if (task.isCompleted() == completedStatus && !task.isDeleted()) {
                filtered.add(task);
            }
        }
        Collections.sort(filtered, Task.BY_PRIORITY);
        return filtered;
    }

    @Override
    public void addTask(String title, int autoUncheckMinutes) {
        int nextPosition = repository.getMaxPosition() + 1;
        String uuid = UUID.randomUUID().toString();
        repository.add(new Task(uuid, title, false, autoUncheckMinutes, 0, nextPosition, 0));
    }

    @Override
    public void updateTask(Task task) {
        if (task.isCompleted() && task.getAutoUncheckMinutes() > 0 && task.getUncheckTimestamp() == 0) {
            task.setUncheckTimestamp(System.currentTimeMillis() + (long) task.getAutoUncheckMinutes() * 60000);
        } else if (!task.isCompleted()) {
            task.setUncheckTimestamp(0);
        }
        task.setUpdatedAt(System.currentTimeMillis());
        repository.update(task);
    }

    @Override
    public void deleteTask(String id) {
        Task task = repository.getById(id);
        if (task != null) {
            task.setDeleted(true);
            task.setUpdatedAt(System.currentTimeMillis());
            repository.update(task);
        }
    }

    @Override
    public void deleteCompletedTasks() {
        List<Task> all = repository.getAll();
        for (Task task : all) {
            if (task.isCompleted() && !task.isDeleted()) {
                task.setDeleted(true);
                task.setUpdatedAt(System.currentTimeMillis());
                repository.update(task);
            }
        }
    }

    @Override
    public void checkAndUncheckTasks() {
        long currentTime = System.currentTimeMillis();
        List<Task> all = repository.getAll();
        for (Task task : all) {
            if (task.isCompleted() && task.getUncheckTimestamp() > 0 && currentTime >= task.getUncheckTimestamp()) {
                task.setCompleted(false);
                task.setUncheckTimestamp(0);
                task.setUpdatedAt(System.currentTimeMillis());
                repository.update(task);
            }
        }
    }
}