package com.example.habbittracker.Database_config.HabitLogs;

import android.provider.BaseColumns;

public class HabitLogsDatabaseContract {
    public static String table_name = "habit_logs";

    public static final class habitLogsColumns implements BaseColumns {
        public static String ID = "id";
        public static String HABIT_ID = "habit_id";
        public static String LOG_DATE = "log_date";
        public static String STATUS = "status";
    }
}
