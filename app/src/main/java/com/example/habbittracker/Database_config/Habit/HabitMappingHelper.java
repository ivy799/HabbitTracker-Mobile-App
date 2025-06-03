package com.example.habbittracker.Database_config.Habit;

import android.database.Cursor;

import com.example.habbittracker.Models.Habit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HabitMappingHelper {

    public static ArrayList<Habit> mapCursorToArrayList(Cursor cursor) throws ParseException {
        ArrayList<Habit> habits = new ArrayList<>();

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns._ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.NAME));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.DESCRIPTION));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.CATEGORY));
            int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.FREQUENCY));
            String startDate = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.START_DATE));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(startDate);
            String targetCount = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.TARGET_COUNT));
            String currentCount = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.CURRENT_COUNT));
            String isActive = cursor.getString(cursor.getColumnIndexOrThrow(HabitDatabaseContract.habitColumns.IS_ACTIVE));

            habits.add(new Habit(
                    id,
                    Integer.parseInt(targetCount),
                    Integer.parseInt(currentCount),
                    name,
                    description,
                    category,
                    frequency == 0 ? "Daily" : frequency == 1 ? "Weekly" : "Monthly",
                    date != null ? new java.util.Date(String.valueOf(date)) : null,
                    isActive != null && isActive.equals("1")
            ));
        }
        return habits;
    }
}
