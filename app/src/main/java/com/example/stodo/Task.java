package com.example.stodo;

import com.google.gson.annotations.SerializedName;
import java.util.Comparator;

public class Task {
    public static final Comparator<Task> BY_PRIORITY = (t1, t2) -> {
        int countCompare = Integer.compare(t2.getCompletionCount(), t1.getCompletionCount());
        if (countCompare != 0) return countCompare;
        return Integer.compare(t1.getPosition(), t2.getPosition());
    };

    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("completed")
    private boolean completed;
    @SerializedName("autoUncheckMinutes")
    private int autoUncheckMinutes; // 0 means disabled
    @SerializedName("uncheckTimestamp")
    private long uncheckTimestamp; // Absolute epoch time when it should be unchecked
    @SerializedName("position")
    private int position; // For manual ordering
    @SerializedName("completionCount")
    private int completionCount;
    @SerializedName("updatedAt")
    private long updatedAt;
    @SerializedName("isDeleted")
    private boolean isDeleted;

    public Task(String id, String title, boolean completed) {
        this(id, title, completed, 0, 0, 0, 0, System.currentTimeMillis(), false);
    }

    public Task(String id, String title, boolean completed, int autoUncheckMinutes, long uncheckTimestamp, int position, int completionCount) {
        this(id, title, completed, autoUncheckMinutes, uncheckTimestamp, position, completionCount, System.currentTimeMillis(), false);
    }

    public Task(String id, String title, boolean completed, int autoUncheckMinutes, long uncheckTimestamp, int position, int completionCount, long updatedAt, boolean isDeleted) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.autoUncheckMinutes = autoUncheckMinutes;
        this.uncheckTimestamp = uncheckTimestamp;
        this.position = position;
        this.completionCount = completionCount;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public String getId() {
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
        if (!this.completed && completed) {
            this.completionCount++;
        }
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

    public int getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}