package com.example.stodo.service;

import com.example.stodo.Task;
import com.example.stodo.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;

public class TaskServiceImpl implements TaskService {
    private final TaskRepository repository;
    private int nextId = 100; // Just for new tasks in dummy impl

    public TaskServiceImpl(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Task> getActiveTasks() {
        List<Task> active = new ArrayList<>();
        for (Task task : repository.getAll()) {
            if (!task.isCompleted()) {
                active.add(task);
            }
        }
        return active;
    }

    @Override
    public List<Task> getCompletedTasks() {
        List<Task> completed = new ArrayList<>();
        for (Task task : repository.getAll()) {
            if (task.isCompleted()) {
                completed.add(task);
            }
        }
        return completed;
    }

    @Override
    public void addTask(String title, int autoUncheckMinutes) {
        List<Task> allTasks = repository.getAll();
        int maxPosition = -1;
        for (Task t : allTasks) {
            if (t.getPosition() > maxPosition) {
                maxPosition = t.getPosition();
            }
        }
        repository.add(new Task(nextId++, title, false, autoUncheckMinutes, 0, maxPosition + 1));
    }

    @Override
    public void updateTask(Task task) {
        if (task.isCompleted() && task.getAutoUncheckMinutes() > 0 && task.getUncheckTimestamp() == 0) {
            task.setUncheckTimestamp(System.currentTimeMillis() + (long) task.getAutoUncheckMinutes() * 60000);
        } else if (!task.isCompleted()) {
            task.setUncheckTimestamp(0);
        }
        repository.update(task);
    }

    @Override
    public void deleteTask(int id) {
        repository.delete(id);
    }

    @Override
    public void deleteCompletedTasks() {
        List<Task> all = repository.getAll();
        for (Task task : all) {
            if (task.isCompleted()) {
                repository.delete(task.getId());
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
                repository.update(task);
            }
        }
    }
}