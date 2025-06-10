package com.example.habbittracker;

import android.app.Application;

public class HabitTrackerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ThemeManager.applyTheme(this);
    }
}