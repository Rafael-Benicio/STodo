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
    public void addTask(String title) {
        repository.add(new Task(nextId++, title, false));
    }

    @Override
    public void updateTask(Task task) {
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
}