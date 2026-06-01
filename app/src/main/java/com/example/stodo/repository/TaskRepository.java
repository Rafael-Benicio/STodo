package com.example.stodo.repository;

import com.example.stodo.Task;
import java.util.List;

public interface TaskRepository {
    List<Task> getAll();
    Task getById(String id);
    void add(Task task);
    void update(Task task);
    void delete(String id);
    int getMaxPosition();
}