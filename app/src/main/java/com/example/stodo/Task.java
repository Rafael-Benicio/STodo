package com.example.stodo;

public class Task {
    private int id;
    private String title;
    private boolean completed;
    private int autoUncheckMinutes; // 0 means disabled
    private long uncheckTimestamp; // Absolute epoch time when it should be unchecked
    private int position; // For manual ordering

    public Task(int id, String title, boolean completed) {
        this(id, title, completed, 0, 0, 0);
    }

    public Task(int id, String title, boolean completed, int autoUncheckMinutes, long uncheckTimestamp, int position) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.autoUncheckMinutes = autoUncheckMinutes;
        this.uncheckTimestamp = uncheckTimestamp;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getAutoUncheckMinutes() {
        return autoUncheckMinutes;
    }

    public void setAutoUncheckMinutes(int autoUncheckMinutes) {
        this.autoUncheckMinutes = autoUncheckMinutes;
    }

    public long getUncheckTimestamp() {
        return uncheckTimestamp;
    }

    public void setUncheckTimestamp(long uncheckTimestamp) {
        this.uncheckTimestamp = uncheckTimestamp;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}