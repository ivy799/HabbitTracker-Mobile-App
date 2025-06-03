package com.example.habbittracker.Database_config.Habit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.habbittracker.Database_config.DatabaseHelper;

public class HabitHelper {
    public static final String TABLE_NAME = HabitDatabaseContract.table_name;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase sqLiteDatabase;
    public static volatile HabitHelper instance;
    private boolean isOpen = false;

    private HabitHelper(Context context) {
        // Gunakan applicationContext untuk menghindari memory leak
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static HabitHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (SQLiteOpenHelper.class) {
                if (instance == null) {
                    instance = new HabitHelper(context);
                }
            }
        }
        return instance;
    }

    public void open() {
        if (!isOpen || sqLiteDatabase == null || !sqLiteDatabase.isOpen()) {
            sqLiteDatabase = databaseHelper.getWritableDatabase();
            isOpen = true;
        }
    }

    public void close() {
        if (isOpen && sqLiteDatabase != null && sqLiteDatabase.isOpen()) {
            sqLiteDatabase.close();
            isOpen = false;
        }
    }

    // Cek apakah database terbuka
    public boolean isOpen() {
        return isOpen && sqLiteDatabase != null && sqLiteDatabase.isOpen();
    }

    // Metode untuk memastikan database terbuka sebelum operasi database
    private void ensureOpen() {
        if (!isOpen()) {
            open();
        }
    }

    public Cursor queryAll() {
        ensureOpen();
        return sqLiteDatabase.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                HabitDatabaseContract.habitColumns._ID + " ASC"
        );
    }

    public Cursor search(String keyword) {
        ensureOpen();
        return sqLiteDatabase.query(
                TABLE_NAME,
                null,
                HabitDatabaseContract.habitColumns.NAME + " LIKE ?",
                new String[]{"%" + keyword + "%"},
                null,
                null,
                HabitDatabaseContract.habitColumns._ID + " ASC"
        );
    }

    public long insert(ContentValues values) {
        ensureOpen();
        return sqLiteDatabase.insert(TABLE_NAME, null, values);
    }

    public long update(String id, ContentValues values) {
        ensureOpen();
        return sqLiteDatabase.update(TABLE_NAME, values,
                HabitDatabaseContract.habitColumns._ID + " = ?",
                new String[]{id});
    }

    public long deleteById(String id) {
        ensureOpen();
        return sqLiteDatabase.delete(TABLE_NAME,
                HabitDatabaseContract.habitColumns._ID + " = ?",
                new String[]{id});
    }
}