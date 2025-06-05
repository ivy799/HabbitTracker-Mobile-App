package com.example.habbittracker;

import android.app.Application;

public class HabitTrackerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme saat aplikasi pertama kali dibuka
        ThemeManager.applyTheme(this);
    }
}