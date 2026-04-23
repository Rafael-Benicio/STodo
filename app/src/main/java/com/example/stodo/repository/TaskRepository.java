package com.example.stodo.repository;

import com.example.stodo.Task;
import java.util.List;

public interface TaskRepository {
    List<Task> getAll();
    void add(Task task);
    void update(Task task);
    void delete(int id);
    int getMaxPosition();
}