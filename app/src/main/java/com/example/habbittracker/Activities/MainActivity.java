package com.example.habbittracker.Activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Adapters.HabitAdapter;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.Habit.HabitMappingHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_UPDATE = 200;
    RecyclerView rvHabit;
    private ExtendedFloatingActionButton fabAdd;
    private HabitAdapter adapter;
    private HabitHelper habitHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("Habit Tracker");
        }

        rvHabit = findViewById(R.id.rv_habits);
        fabAdd = findViewById(R.id.fab_add);

        rvHabit.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(this);
        rvHabit.setAdapter(adapter);
        habitHelper = HabitHelper.getInstance(getApplicationContext());

        fabAdd.setOnClickListener(v ->{
            Intent intent = new Intent(MainActivity.this, HabitFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD);
        });

        habitHelper.open();
        loadData();
    }

    private void loadData(){
        new LoadHabitsAsync(this, habits -> {
            if (!habits.isEmpty()) {
                adapter.setListHabits(habits);
                adapter.notifyDataSetChanged();
            } else {
                adapter.setListHabits(new ArrayList<>());
                adapter.notifyDataSetChanged();
            }
        }).execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD) {
            if (resultCode == HabitFormActivity.RESULT_ADD) {
                loadData();
            }
        } else if (requestCode == REQUEST_UPDATE) {
            if (resultCode == HabitFormActivity.RESULT_UPDATE) {
                loadData();
            } else if (resultCode == HabitFormActivity.RESULT_DELETE) {
                loadData();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        habitHelper.close();
    }

    private static class LoadHabitsAsync {
        private final WeakReference<Context> weakContext;
        private final WeakReference<LoadHabitsCallback> weakCallback;

        private LoadHabitsAsync(Context context, LoadHabitsCallback callback) {
            weakContext = new WeakReference<>(context);
            weakCallback = new WeakReference<>(callback);
        }

        void execute() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                Context context = weakContext.get();
                if (context != null) {
                    HabitHelper studentHelper = HabitHelper.getInstance(context);
                    studentHelper.open();

                    Cursor habitCursor = studentHelper.queryAll();
                    ArrayList<Habit> habits = null;
                    try {
                        habits = HabitMappingHelper.mapCursorToArrayList(habitCursor);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    habitCursor.close();

                    ArrayList<Habit> finalHabits = habits;
                    handler.post(() -> {
                        LoadHabitsCallback callback = weakCallback.get();
                        if (callback != null) {
                            callback.postExecute(finalHabits);
                        }
                    });
                }
            });
        }

        interface LoadHabitsCallback {
            void postExecute(ArrayList<Habit> habits);
        }
    }
}