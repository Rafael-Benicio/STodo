package com.example.stodo.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "stodo.db";
    private static final int DATABASE_VERSION = 7;

    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_COMPLETED = "completed";
    public static final String COLUMN_UNCHECK_TIMESTAMP = "uncheck_timestamp";
    public static final String COLUMN_AUTO_UNCHECK_MINUTES = "auto_uncheck_minutes";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_COMPLETION_COUNT = "completion_count";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_IS_DELETED = "is_deleted";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TASKS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_COMPLETED + " INTEGER, " +
                    COLUMN_UNCHECK_TIMESTAMP + " INTEGER DEFAULT 0, " +
                    COLUMN_AUTO_UNCHECK_MINUTES + " INTEGER DEFAULT 0, " +
                    COLUMN_POSITION + " INTEGER DEFAULT 0, " +
                    COLUMN_COMPLETION_COUNT + " INTEGER DEFAULT 0, " +
                    COLUMN_UPDATED_AT + " INTEGER DEFAULT 0, " +
                    COLUMN_IS_DELETED + " INTEGER DEFAULT 0);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Configures the SQLite database options, enabling write-ahead logging (WAL).
     * Example: onConfigure(db);
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_UNCHECK_TIMESTAMP + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_AUTO_UNCHECK_MINUTES + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_POSITION + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_COMPLETION_COUNT + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 6) {
            // Migration for UUID (int to TEXT)
            db.execSQL("CREATE TABLE tasks_new (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_COMPLETED + " INTEGER, " +
                    COLUMN_UNCHECK_TIMESTAMP + " INTEGER DEFAULT 0, " +
                    COLUMN_AUTO_UNCHECK_MINUTES + " INTEGER DEFAULT 0, " +
                    COLUMN_POSITION + " INTEGER DEFAULT 0, " +
                    COLUMN_COMPLETION_COUNT + " INTEGER DEFAULT 0);");

            db.execSQL("INSERT INTO tasks_new SELECT CAST(" + COLUMN_ID + " AS TEXT), " +
                    COLUMN_TITLE + ", " + COLUMN_COMPLETED + ", " + COLUMN_UNCHECK_TIMESTAMP + ", " +
                    COLUMN_AUTO_UNCHECK_MINUTES + ", " + COLUMN_POSITION + ", " + COLUMN_COMPLETION_COUNT +
                    " FROM " + TABLE_TASKS + ";");

            db.execSQL("DROP TABLE " + TABLE_TASKS + ";");
            db.execSQL("ALTER TABLE tasks_new RENAME TO " + TABLE_TASKS + ";");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_UPDATED_AT + " INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_IS_DELETED + " INTEGER DEFAULT 0;");
        }
    }
}