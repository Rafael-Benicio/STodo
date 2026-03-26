package com.example.stodo;

public class Task {
    private int id;
    private String title;
    private boolean completed;
    private long uncheckTimestamp; // 0 means no auto-uncheck

    public Task(int id, String title, boolean completed) {
        this(id, title, completed, 0);
    }

    public Task(int id, String title, boolean completed, long uncheckTimestamp) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.uncheckTimestamp = uncheckTimestamp;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getUncheckTimestamp() {
        return uncheckTimestamp;
    }

    public void setUncheckTimestamp(long uncheckTimestamp) {
        this.uncheckTimestamp = uncheckTimestamp;
    }
}