package com.example.habbittracker.Fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Activities.HabitFormActivity;
import com.example.habbittracker.Adapters.HabitAdapter;
import com.example.habbittracker.Database_config.DatabaseHelper;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.Habit.HabitMappingHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_UPDATE = 200;
    private RecyclerView rvHabit;
    private ExtendedFloatingActionButton fabAdd;
    private HabitAdapter adapter;
    private HabitHelper habitHelper;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inisialisasi database
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        HabitLogHelper.getInstance(dbHelper).open();

        // Inisialisasi helper
        habitHelper = HabitHelper.getInstance(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        habitHelper.open();
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        habitHelper.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHabit = view.findViewById(R.id.rv_habits);
        fabAdd = view.findViewById(R.id.fab_add);

        rvHabit.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HabitAdapter(requireActivity());
        rvHabit.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), HabitFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD);
        });

        loadData();
    }

    private void loadData() {
        new LoadHabitsAsync(requireContext(), habits -> {
            if (!habits.isEmpty()) {
                adapter.setListHabits(habits);
            } else {
                adapter.setListHabits(new ArrayList<>());
            }
        }).execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
    public void onDestroy() {
        super.onDestroy();
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