package com.example.stodo.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.stodo.Task;
import java.util.ArrayList;
import java.util.List;

public class SqliteTaskRepository implements TaskRepository {
    private final DatabaseHelper dbHelper;

    public SqliteTaskRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    @Override
    public List<Task> getAll() {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Get all tasks ordered by position
        Cursor cursor = db.query(DatabaseHelper.TABLE_TASKS, null, null, null, null, null, DatabaseHelper.COLUMN_POSITION + " ASC");

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TITLE));
            boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMPLETED)) == 1;
            long uncheckTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UNCHECK_TIMESTAMP));
            int autoUncheckMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AUTO_UNCHECK_MINUTES));
            int position = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POSITION));
            tasks.add(new Task(id, title, completed, autoUncheckMinutes, uncheckTimestamp, position));
        }
        cursor.close();
        return tasks;
    }

    @Override
    public void add(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, task.getTitle());
        values.put(DatabaseHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_UNCHECK_TIMESTAMP, task.getUncheckTimestamp());
        values.put(DatabaseHelper.COLUMN_AUTO_UNCHECK_MINUTES, task.getAutoUncheckMinutes());
        values.put(DatabaseHelper.COLUMN_POSITION, task.getPosition());
        db.insert(DatabaseHelper.TABLE_TASKS, null, values);
    }

    @Override
    public void update(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, task.getTitle());
        values.put(DatabaseHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_UNCHECK_TIMESTAMP, task.getUncheckTimestamp());
        values.put(DatabaseHelper.COLUMN_AUTO_UNCHECK_MINUTES, task.getAutoUncheckMinutes());
        values.put(DatabaseHelper.COLUMN_POSITION, task.getPosition());
        db.update(DatabaseHelper.TABLE_TASKS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(task.getId())});
    }

    @Override
    public void delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_TASKS, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}