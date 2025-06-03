package com.example.habbittracker.Database_config;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.example.habbittracker.Database_config.Habit.HabitDatabaseContract;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogsDatabaseContract;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static String DATABASE_NAME = "habitTracker.db";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(android.content.Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

private static final String SQL_CREATE_TABLE_HABIT =
        String.format(
                "CREATE TABLE %s ("
                        + "%s INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s INTEGER NOT NULL, "
                        + "%s INTEGER NOT NULL)",
                HabitDatabaseContract.table_name,
                BaseColumns._ID,
                HabitDatabaseContract.habitColumns.NAME,
                HabitDatabaseContract.habitColumns.DESCRIPTION,
                HabitDatabaseContract.habitColumns.CATEGORY,
                HabitDatabaseContract.habitColumns.FREQUENCY,
                HabitDatabaseContract.habitColumns.START_DATE,
                HabitDatabaseContract.habitColumns.TARGET_COUNT,
                HabitDatabaseContract.habitColumns.CURRENT_COUNT,
                HabitDatabaseContract.habitColumns.IS_ACTIVE
        );

private static final String SQL_CREATE_TABLE_HABIT_LOGS =
        String.format(
                "CREATE TABLE %S ("
                        + "%S INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "%s INTEGER NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "FOREIGN KEY(%s) REFERENCES %s(%s))",
                HabitLogsDatabaseContract.table_name,
                HabitLogsDatabaseContract.habitLogsColumns._ID,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID,
                HabitLogsDatabaseContract.habitLogsColumns.LOG_DATE,
                HabitLogsDatabaseContract.habitLogsColumns.STATUS,
                HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID,
                HabitDatabaseContract.table_name,
                HabitDatabaseContract.habitColumns._ID
        );


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_HABIT);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_HABIT_LOGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HabitDatabaseContract.table_name);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + HabitLogsDatabaseContract.table_name);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
