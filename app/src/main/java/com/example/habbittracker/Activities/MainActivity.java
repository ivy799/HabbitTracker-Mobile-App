package com.example.habbittracker.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.habbittracker.Fragment.GeneralFragment;
import com.example.habbittracker.Fragment.GraphFragment;
import com.example.habbittracker.Fragment.HomeFragment;
import com.example.habbittracker.R;
import com.example.habbittracker.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.habbittracker.ThemeManager.applyTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Hanya handle top dan sides
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return WindowInsetsCompat.CONSUMED; // Consume insets
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Habit Tracker");
        }

        // Setup bottom navigation (jika diperlukan)
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    fragment = new HomeFragment();
                } else if (itemId == R.id.tiga) {
                    fragment = new GraphFragment();
                } else if (itemId == R.id.satu) {
                    fragment = new GeneralFragment();
                }

                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == HabitFormActivity.REQUEST_UPDATE) {
            if (resultCode == HabitFormActivity.RESULT_UPDATE) {
                // Refresh data setelah update
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).refreshHabits();
                }
                Toast.makeText(this, "Habit updated successfully!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == HabitFormActivity.RESULT_DELETE) {
                // Handle delete result
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).refreshHabits();
                }
                Toast.makeText(this, "Habit deleted successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}