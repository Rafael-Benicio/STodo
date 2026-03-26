package com.example.stodo.repository;

import com.example.stodo.Task;
import java.util.ArrayList;
import java.util.List;

public class InMemoryTaskRepository implements TaskRepository {
    private List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    public InMemoryTaskRepository() {
        // Initial dummy data
        tasks.add(new Task(nextId++, "Fazer compras", false));
        tasks.add(new Task(nextId++, "Estudar Java", false));
        tasks.add(new Task(nextId++, "Lavar o carro", true));
    }

    @Override
    public List<Task> getAll() {
        return new ArrayList<>(tasks);
    }

    @Override
    public void add(Task task) {
        // In a real app, ID generation would be here
        tasks.add(task);
    }

    @Override
    public void update(Task task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == task.getId()) {
                tasks.set(i, task);
                return;
            }
        }
    }

    @Override
    public void delete(int id) {
        tasks.removeIf(task -> task.getId() == id);
    }
}