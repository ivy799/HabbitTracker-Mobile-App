package com.example.habbittracker.Database_config.HabitLogs;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.habbittracker.Database_config.DatabaseHelper;

public class HabitLogHelper {
    public static final String TABLE_NAME = HabitLogsDatabaseContract.table_name;
    public static DatabaseHelper databaseHelper;
    public static SQLiteDatabase sqLiteDatabase;
    public static volatile HabitLogHelper instance;


    public HabitLogHelper(DatabaseHelper dbHelper) {
        databaseHelper = dbHelper;
    }

    public static HabitLogHelper getInstance(DatabaseHelper dbHelper) {
        if (instance == null) {
            synchronized (HabitLogHelper.class) {
                if (instance == null) {
                    instance = new HabitLogHelper(dbHelper);
                }
            }
        }
        return instance;
    }

    public void open() {
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
                HabitLogsDatabaseContract.habitLogsColumns._ID + " ASC"
        );
    }

    public Cursor search(String keyword) {
        return sqLiteDatabase.query(
                TABLE_NAME,
                null,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID + " LIKE ?",
                new String[]{"%" + keyword + "%"},
                null,
                null,
                HabitLogsDatabaseContract.habitLogsColumns._ID + " ASC"
        );
    }

    public static Cursor queryByHabitId(String habitId) {
        return sqLiteDatabase.query(
                TABLE_NAME,
                null,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID + " = ?",
                new String[]{habitId},
                null,
                null,
                HabitLogsDatabaseContract.habitLogsColumns._ID + " ASC"
        );
    }

    public long insert(ContentValues values) {
        return sqLiteDatabase.insert(TABLE_NAME, null, values);
    }

    public long update(String id, ContentValues values) {
        return sqLiteDatabase.update(TABLE_NAME, values,
                HabitLogsDatabaseContract.habitLogsColumns._ID + " = ?",
                new String[]{id});
    }

    public long deleteById(String id) {
        return sqLiteDatabase.delete(TABLE_NAME,
                HabitLogsDatabaseContract.habitLogsColumns._ID + " = ?",
                new String[]{id});
    }

    public long deleteByHabitId(String habitId) {
        return sqLiteDatabase.delete(
                TABLE_NAME,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID + " = ?",
                new String[]{habitId}
        );
    }

    public Cursor queryByHabitIdAndDate(int habitId, String date) {
        return sqLiteDatabase.query(
                TABLE_NAME,
                null,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID + " = ? AND " +
                        HabitLogsDatabaseContract.habitLogsColumns.LOG_DATE + " = ?",
                new String[]{String.valueOf(habitId), date},
                null,
                null,
                HabitLogsDatabaseContract.habitLogsColumns._ID + " ASC"
        );
    }
}
