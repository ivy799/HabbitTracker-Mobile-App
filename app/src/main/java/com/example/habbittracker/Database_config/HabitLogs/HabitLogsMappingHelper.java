package com.example.habbittracker.Database_config.HabitLogs;

import android.database.Cursor;

import com.example.habbittracker.Models.HabitLog;

import java.util.ArrayList;

public class HabitLogsMappingHelper {
    public static ArrayList<HabitLog> mapCursorToArrayList(Cursor cursor) {
        ArrayList<HabitLog> habitLogs = new ArrayList<>();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(HabitLogsDatabaseContract.habitLogsColumns._ID));
            int habitId = cursor.getInt(cursor.getColumnIndexOrThrow(HabitLogsDatabaseContract.habitLogsColumns.HABIT_ID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(HabitLogsDatabaseContract.habitLogsColumns.LOG_DATE));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(HabitLogsDatabaseContract.habitLogsColumns.STATUS));

            habitLogs.add(new HabitLog(
                    id,
                    habitId,
                    date != null ? java.sql.Date.valueOf(date) : null, // Convert String to Date
                    status != null ? Integer.parseInt(status) : 0 // Convert String to int
            ));
        }
        return habitLogs;
    }
}
