package com.example.habbittracker.Database_config.Habit;

import android.provider.BaseColumns;

public class HabitDatabaseContract {
    public static String table_name = "habits";

    public static final class habitColumns implements BaseColumns {
        public static String TARGET_COUNT = "target_count";
        public static String CURRENT_COUNT = "current_count";
        public static String NAME = "name";
        public static String DESCRIPTION = "description";
        public static String CATEGORY = "category";
        public static String FREQUENCY = "frequency";
        public static String START_DATE = "start_date";
        public static String IS_ACTIVE = "is_active";
    }
}
