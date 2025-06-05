package com.example.habbittracker.Adapters;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {
    private final ArrayList<Habit> listHabits = new ArrayList<>();
    private final Activity activity;

    public HabitAdapter(Activity activity) {
        this.activity = activity;
    }

    public void setListHabits(ArrayList<Habit> habits) {
        listHabits.clear();

        for (Habit habit : habits) {
            // Refresh dari database untuk memastikan data terbaru
            Cursor cursor = HabitHelper.instance.search(habit.getId());
            if (cursor != null && cursor.moveToFirst()) {
                int freqIndex = cursor.getColumnIndex("frequency");
                if (freqIndex >= 0) {
                    habit.setFrequency(cursor.getString(freqIndex));
                }
                cursor.close();
            }
            listHabits.add(habit);
        }

        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public HabitAdapter.HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.habit_item, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitAdapter.HabitViewHolder holder, int position) {
        holder.bind(listHabits.get(position));
    }

    @Override
    public int getItemCount() {
        return listHabits.size();
    }

    class HabitViewHolder extends RecyclerView.ViewHolder {
        final TextView tvHabitName;
        final TextView tvHabitDescription;
        final TextView tvHabitCategory;
        final TextView tvHabitFrequency;
        final TextView tvHabitStartDate;
        final TextView tvHabitTarget;
        final TextView tvHabitCurrent;
        final TextView tvHabitStatus;
        final Button btnFinishHabit, btnSkipHabit, btnDeactivateHabit;
        final ProgressBar progressBar;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvHabitName = itemView.findViewById(R.id.tvHabitName);
            this.tvHabitDescription = itemView.findViewById(R.id.tvHabitDescription);
            this.tvHabitCategory = itemView.findViewById(R.id.tvHabitCategory);
            this.tvHabitFrequency = itemView.findViewById(R.id.tvHabitFrequency);
            this.tvHabitStartDate = itemView.findViewById(R.id.tvHabitStartDate);
            this.tvHabitTarget = itemView.findViewById(R.id.tvHabitTarget);
            this.tvHabitCurrent = itemView.findViewById(R.id.tvHabitCurrent);
            this.tvHabitStatus = itemView.findViewById(R.id.tvHabitStatus);
            this.btnFinishHabit = itemView.findViewById(R.id.btnFinishHabit);
            this.btnSkipHabit = itemView.findViewById(R.id.btnSkipHabit);
            this.btnDeactivateHabit = itemView.findViewById(R.id.btnDeactivateHabit);
            this.progressBar = itemView.findViewById(R.id.progressBar);

        }

        void bind(Habit habit) {
            tvHabitName.setText(habit.getName());
            tvHabitDescription.setText(habit.getDescription());
            tvHabitCategory.setText(habit.getCategory());
            tvHabitFrequency.setText(habit.getFrequency());
            tvHabitStartDate.setText(habit.getStart_date().toString());
            tvHabitTarget.setText(String.valueOf(habit.getTarget_count()));
            tvHabitCurrent.setText(String.valueOf(habit.getCurrent_count()));
            tvHabitStatus.setText(habit.getIs_active() ? "Active" : "Inactive");

            // Hitung progress
            int progress = 0;
            if (habit.getTarget_count() > 0) {
                progress = (int) ((habit.getCurrent_count() * 100.0f) / habit.getTarget_count());
            }
            progressBar.setProgress(progress);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean sudahSelesaiHariIni = false;
            boolean weeklyCooldownPassed = true;


            Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
            if (habitLog != null && habitLog.moveToFirst()) {
                int statusColumnIndex = habitLog.getColumnIndex("status");
                if (statusColumnIndex != -1) {
                    int status = habitLog.getInt(statusColumnIndex);
                    sudahSelesaiHariIni = (status == 1 || status == 2);
                }
            }

            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                // Query log terakhir dengan status = 1 (complete)
                Cursor lastWeeklyLog = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());
                if (lastWeeklyLog != null && lastWeeklyLog.moveToFirst()) {
                    int logDateColIndex = lastWeeklyLog.getColumnIndex("log_date");
                    if (logDateColIndex >= 0) { // <-- CEK DI SINI
                        String lastLogDate = lastWeeklyLog.getString(logDateColIndex);
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date lastDate = sdf.parse(lastLogDate);
                            Date now = sdf.parse(today);
                            long diffMillis = now.getTime() - lastDate.getTime();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                            weeklyCooldownPassed = diffDays >= 7;
                        } catch (Exception e) {
                            weeklyCooldownPassed = true; // default allow jika error parsing date
                        }
                    }
                }
                if (lastWeeklyLog != null) lastWeeklyLog.close();
            }


            // Set background status badge
            if (habit.getIs_active()) {
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge);
            } else {
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
            }

            // Atur state tombol
            boolean canCompleteToday;
            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && weeklyCooldownPassed;
            } else {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni;
            }
            btnFinishHabit.setEnabled(canCompleteToday);
            btnSkipHabit.setEnabled(canCompleteToday);
            btnDeactivateHabit.setText(habit.getIs_active() ? "Disable" : "Activate");

            // Tombol Finish
            btnFinishHabit.setOnClickListener(v -> {
                try (Cursor c = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today)) {
                    boolean alreadyLogged = (c != null && c.moveToFirst());
                    if (!alreadyLogged) {
                        ContentValues values = new ContentValues();
                        values.put("habit_id", habit.getId());
                        values.put("log_date", today);
                        values.put("status", 1);
                        HabitLogHelper.instance.insert(values);

                        // Update model
                        habit.setCurrent_count(habit.getCurrent_count() + 1);
                        habit.setIs_active(habit.getCurrent_count() < habit.getTarget_count());

                        ContentValues habitValues = new ContentValues();
                        habitValues.put("current_count", habit.getCurrent_count());
                        habitValues.put("is_active", habit.getIs_active() ? 1 : 0);
                        HabitHelper.instance.update(String.valueOf(habit.getId()), habitValues);

                        // Update UI
                        tvHabitCurrent.setText(String.valueOf(habit.getCurrent_count()));
                        int newProgress = (int) ((habit.getCurrent_count() * 100.0f) / habit.getTarget_count());
                        progressBar.setProgress(newProgress);

                        if (!habit.getIs_active()) {
                            tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
                            tvHabitStatus.setText("Inactive");
                        }

                        btnFinishHabit.setEnabled(false);
                        btnSkipHabit.setEnabled(false);
                    }
                }
            });

            // Tombol Deactivate / Activate
            btnDeactivateHabit.setOnClickListener(v -> {
                ContentValues values = new ContentValues();
                boolean isActivating = !habit.getIs_active();

                values.put("is_active", isActivating ? 1 : 0);
                values.put("current_count", 0);
                habit.setIs_active(isActivating);
                habit.setCurrent_count(0);
                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                if (!isActivating) {
                    HabitLogHelper.instance.deleteByHabitId(String.valueOf(habit.getId()));
                }

                // Update UI
                tvHabitStatus.setBackgroundResource(isActivating ? R.drawable.habit_status_badge : R.drawable.habit_status_badge_inactive);
                tvHabitStatus.setText(isActivating ? "Active" : "Inactive");
                tvHabitCurrent.setText("0");
                progressBar.setProgress(0);
                btnFinishHabit.setEnabled(isActivating);
                btnSkipHabit.setEnabled(isActivating);
                btnDeactivateHabit.setText(isActivating ? "Disable" : "Activate");
            });

            // Tombol Skip
            btnSkipHabit.setOnClickListener(v -> {
                try (Cursor c = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today)) {
                    boolean alreadyLogged = (c != null && c.moveToFirst());
                    if (!alreadyLogged) {
                        ContentValues values = new ContentValues();
                        values.put("habit_id", habit.getId());
                        values.put("log_date", today);
                        values.put("status", 2);
                        HabitLogHelper.instance.insert(values);

                        btnFinishHabit.setEnabled(false);
                        btnSkipHabit.setEnabled(false);

                        Toast.makeText(itemView.getContext(),
                                "Habit skipped for today. Your streak is preserved!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        btnSkipHabit.setEnabled(false);
                        Toast.makeText(itemView.getContext(),
                                "You've already logged this habit today",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
