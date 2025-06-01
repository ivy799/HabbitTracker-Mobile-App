package com.example.habbittracker.Database_config.Habit;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.habbittracker.Database_config.DatabaseHelper;

public class HabitHelper {
    public static final String TABLE_NAME = HabitDatabaseContract.table_name;
    public static DatabaseHelper databaseHelper;
    public static SQLiteDatabase sqLiteDatabase;
    private static volatile HabitHelper INSTANCE;

    public HabitHelper(DatabaseHelper dbHelper) {
        databaseHelper = dbHelper;
    }

    public static HabitHelper getInstance(DatabaseHelper dbHelper) {
        if (INSTANCE == null) {
            synchronized (HabitHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HabitHelper(dbHelper);
                }
            }
        }
        return INSTANCE;
    }

    public void open(){
        sqLiteDatabase = databaseHelper.getWritableDatabase();
    }

    public void close() {
        databaseHelper.close();
        if (sqLiteDatabase.isOpen()) {
            sqLiteDatabase.close();
        }
    }

    public Cursor queryAll() {
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
        return sqLiteDatabase.insert(TABLE_NAME, null, values);
    }

    public long update(String id, ContentValues values) {
        return sqLiteDatabase.update(TABLE_NAME, values,
                HabitDatabaseContract.habitColumns._ID + " = ?",
                new String[]{id});
    }

    public long deleteById(String id) {
        return sqLiteDatabase.delete(TABLE_NAME,
                HabitDatabaseContract.habitColumns._ID + " = ?",
                new String[]{id});
    }
}
