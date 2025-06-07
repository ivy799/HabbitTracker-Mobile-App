package com.example.habbittracker.Adapters;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Activities.HabitFormActivity;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

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

        // Sort habits dengan prioritas baru
        sortHabitsByCompletionStatus();
        notifyDataSetChanged();
    }

    /**
     * Sort habits dengan prioritas:
     * 1. Inactive habits selalu paling bawah
     * 2. Active & belum completed di atas
     * 3. Active & sudah completed di tengah
     */
    private void sortHabitsByCompletionStatus() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Collections.sort(listHabits, new Comparator<Habit>() {
            @Override
            public int compare(Habit habit1, Habit habit2) {
                // Prioritas 1: Inactive habits selalu paling bawah
                if (habit1.getIs_active() != habit2.getIs_active()) {
                    return habit1.getIs_active() ? -1 : 1; // inactive habits go to bottom
                }

                // Prioritas 2: Untuk active habits, yang belum completed di atas
                if (habit1.getIs_active() && habit2.getIs_active()) {
                    boolean habit1Completed = isHabitCompletedForPeriod(habit1, today);
                    boolean habit2Completed = isHabitCompletedForPeriod(habit2, today);

                    if (habit1Completed != habit2Completed) {
                        return habit1Completed ? 1 : -1; // completed habits go to bottom within active group
                    }
                }

                // Prioritas 3: Sort berdasarkan nama (alphabetical)
                return habit1.getName().compareToIgnoreCase(habit2.getName());
            }
        });
    }

    /**
     * Cek apakah habit sudah completed/skipped untuk periode yang sesuai
     */
    private boolean isHabitCompletedForPeriod(Habit habit, String today) {
        // Cek apakah sudah ada log untuk hari ini
        boolean completedToday = false;
        boolean weeklyCooldownActive = false;
        boolean monthlyCooldownActive = false;

        // Cek log hari ini
        Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
        if (habitLog != null && habitLog.moveToFirst()) {
            int statusColumnIndex = habitLog.getColumnIndex("status");
            if (statusColumnIndex != -1) {
                int status = habitLog.getInt(statusColumnIndex);
                completedToday = (status == 1 || status == 2); // completed or skipped
            }
            habitLog.close();
        }

        // Untuk daily habits, cukup cek hari ini
        if (habit.getFrequency().equalsIgnoreCase("daily")) {
            return completedToday;
        }

        // Untuk weekly habits, cek cooldown
        if (habit.getFrequency().equalsIgnoreCase("weekly")) {
            Cursor lastWeeklyLog = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());
            if (lastWeeklyLog != null && lastWeeklyLog.moveToFirst()) {
                int logDateColIndex = lastWeeklyLog.getColumnIndex("log_date");
                if (logDateColIndex >= 0) {
                    String lastLogDate = lastWeeklyLog.getString(logDateColIndex);
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date lastDate = sdf.parse(lastLogDate);
                        Date now = sdf.parse(today);
                        long diffMillis = now.getTime() - lastDate.getTime();
                        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                        weeklyCooldownActive = diffDays < 7; // still in cooldown
                    } catch (Exception e) {
                        weeklyCooldownActive = false;
                    }
                }
                lastWeeklyLog.close();
            }
            return completedToday || weeklyCooldownActive;
        }

        // Untuk monthly habits, cek cooldown
        if (habit.getFrequency().equalsIgnoreCase("monthly")) {
            Cursor lastMonthlyLog = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());
            if (lastMonthlyLog != null && lastMonthlyLog.moveToFirst()) {
                int logDateColIndex = lastMonthlyLog.getColumnIndex("log_date");
                if (logDateColIndex >= 0) {
                    String lastLogDate = lastMonthlyLog.getString(logDateColIndex);
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date lastDate = sdf.parse(lastLogDate);
                        Date now = sdf.parse(today);
                        long diffMillis = now.getTime() - lastDate.getTime();
                        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                        monthlyCooldownActive = diffDays < 30; // still in cooldown
                    } catch (Exception e) {
                        monthlyCooldownActive = false;
                    }
                }
                lastMonthlyLog.close();
            }
            return completedToday || monthlyCooldownActive;
        }

        return completedToday;
    }

    /**
     * Method untuk refresh sorting setelah ada perubahan status habit
     */
    public void refreshSorting() {
        sortHabitsByCompletionStatus();
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
        final MaterialButton btnFinishHabit, btnSkipHabit, btnDeactivateHabit, btnDeleteHabit, btnEditHabit;
        final ProgressBar progressBar;
        final ImageButton btnToggleDetails;
        final ConstraintLayout detailsSection;

        // State untuk dropdown
        private boolean isExpanded = false;

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
            this.btnToggleDetails = itemView.findViewById(R.id.btnToggleDetails);
            this.detailsSection = itemView.findViewById(R.id.detailsSection);
            this.btnDeleteHabit = itemView.findViewById(R.id.btnDeleteHabit);
            this.btnEditHabit = itemView.findViewById(R.id.btnEditHabit);


            // Setup dropdown toggle
            setupDropdownToggle();
        }

        private void setupDropdownToggle() {
            btnToggleDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleDetails();
                }
            });
        }

        private void toggleDetails() {
            isExpanded = !isExpanded;

            if (isExpanded) {
                // Tampilkan detail section
                detailsSection.setVisibility(View.VISIBLE);

                // Animasi rotasi tombol dropdown
                ObjectAnimator rotation = ObjectAnimator.ofFloat(btnToggleDetails, "rotation", 0f, 180f);
                rotation.setDuration(200);
                rotation.start();

            } else {
                // Sembunyikan detail section
                detailsSection.setVisibility(View.GONE);

                // Animasi rotasi tombol dropdown kembali
                ObjectAnimator rotation = ObjectAnimator.ofFloat(btnToggleDetails, "rotation", 180f, 0f);
                rotation.setDuration(200);
                rotation.start();
            }
        }

        private void resetDropdownState() {
            isExpanded = false;
            detailsSection.setVisibility(View.GONE);
            btnToggleDetails.setRotation(0f);
        }

        void bind(Habit habit) {
            resetDropdownState();

            tvHabitName.setText(habit.getName());
            tvHabitDescription.setText(habit.getDescription());
            tvHabitCategory.setText(habit.getCategory());
            tvHabitFrequency.setText(habit.getFrequency());
            SimpleDateFormat format1 = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String formattedDate = format1.format(habit.getStart_date());
            tvHabitStartDate.setText(formattedDate);
            tvHabitTarget.setText(String.valueOf(habit.getTarget_count()));
            tvHabitCurrent.setText(String.valueOf(habit.getCurrent_count()));

            btnEditHabit.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), com.example.habbittracker.Activities.HabitFormActivity.class);
                intent.putExtra(HabitFormActivity.EXTRA_HABIT, habit);
                activity.startActivityForResult(intent, HabitFormActivity.REQUEST_UPDATE);
            });

            int progress = 0;
            if (habit.getTarget_count() > 0) {
                progress = (int) ((habit.getCurrent_count() * 100.0f) / habit.getTarget_count());
            }
            progressBar.setProgress(progress);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean sudahSelesaiHariIni = false;
            boolean weeklyCooldownPassed = true;
            boolean monthlyCooldownPassed = true;

            boolean isCompletedForPeriod = isHabitCompletedForPeriod(habit, today);

            Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
            if (habitLog != null && habitLog.moveToFirst()) {
                int statusColumnIndex = habitLog.getColumnIndex("status");
                if (statusColumnIndex != -1) {
                    int status = habitLog.getInt(statusColumnIndex);
                    sudahSelesaiHariIni = (status == 1 || status == 2);
                }
                habitLog.close();
            }

            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                Cursor lastWeeklyLog = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());
                if (lastWeeklyLog != null && lastWeeklyLog.moveToFirst()) {
                    int logDateColIndex = lastWeeklyLog.getColumnIndex("log_date");
                    if (logDateColIndex >= 0) {
                        String lastLogDate = lastWeeklyLog.getString(logDateColIndex);
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date lastDate = sdf.parse(lastLogDate);
                            Date now = sdf.parse(today);
                            long diffMillis = now.getTime() - lastDate.getTime();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                            weeklyCooldownPassed = diffDays >= 7;
                        } catch (Exception e) {
                            weeklyCooldownPassed = true;
                        }
                    }
                }
                if (lastWeeklyLog != null) lastWeeklyLog.close();
            }
            if (habit.getFrequency().equalsIgnoreCase("monthly")) {
                Cursor lastMonthlyLog = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());
                if (lastMonthlyLog != null && lastMonthlyLog.moveToFirst()) {
                    int logDateColIndex = lastMonthlyLog.getColumnIndex("log_date");
                    if (logDateColIndex >= 0) {
                        String lastLogDate = lastMonthlyLog.getString(logDateColIndex);
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date lastDate = sdf.parse(lastLogDate);
                            Date now = sdf.parse(today);
                            long diffMillis = now.getTime() - lastDate.getTime();
                            long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                            monthlyCooldownPassed = diffDays >= 30;
                        } catch (Exception e) {
                            monthlyCooldownPassed = true;
                        }
                    }
                    lastMonthlyLog.close();
                }
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date todayDate = sdf.parse(today);

                Cursor lastLogCursor = HabitLogHelper.instance.queryLastCompletedLogByHabitId(habit.getId());

                if (lastLogCursor != null && lastLogCursor.moveToFirst()) {
                    int logDateColIndex = lastLogCursor.getColumnIndex("log_date");
                    if (logDateColIndex >= 0) {
                        String lastLogDate = lastLogCursor.getString(logDateColIndex);
                        Date lastDate = sdf.parse(lastLogDate);
                        long diffDays = (todayDate.getTime() - lastDate.getTime()) / (1000 * 60 * 60 * 24);

                        boolean missedStreak = false;

                        if (habit.getFrequency().equalsIgnoreCase("daily") && diffDays >= 2) {
                            missedStreak = true;
                        } else if (habit.getFrequency().equalsIgnoreCase("weekly") && diffDays >= 14) {
                            missedStreak = true;
                        } else if (habit.getFrequency().equalsIgnoreCase("monthly") && diffDays >= 60) {
                            missedStreak = true;
                        }

                        if (missedStreak) {
                            ContentValues resetValues = new ContentValues();
                            resetValues.put("current_count", 0);
                            habit.setCurrent_count(0);
                            HabitHelper.instance.update(String.valueOf(habit.getId()), resetValues);
                            tvHabitCurrent.setText("0");
                            progressBar.setProgress(0);
                            Toast.makeText(itemView.getContext(),
                                    "You missed your habit for too long. Streak reset!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    lastLogCursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            setCardBackground(habit, isCompletedForPeriod);

            if (habit.getIs_active()) {
                if (isCompletedForPeriod) {
                    tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_completed);
                    tvHabitStatus.setText("Completed");
                } else {
                    tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge);
                    tvHabitStatus.setText("Active");
                }
            } else {
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
                tvHabitStatus.setText("Inactive");
            }

            boolean canCompleteToday;
            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && weeklyCooldownPassed;
            } else if (habit.getFrequency().equalsIgnoreCase("monthly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && monthlyCooldownPassed;
            } else {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni;
            }

            btnFinishHabit.setEnabled(canCompleteToday);
            btnSkipHabit.setEnabled(canCompleteToday);
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

                        // Refresh sorting untuk memindahkan habit yang completed ke bawah
                        refreshSorting();
                    }
                }
            });
            btnDeactivateHabit.setOnClickListener(v -> {
                ContentValues values = new ContentValues();
                boolean isActivating = !habit.getIs_active();
                showMaterialConfirmationDialog(habit, isActivating);
            });
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

                        // Refresh sorting untuk memindahkan habit yang skipped ke bawah
                        refreshSorting();
                    } else {
                        btnSkipHabit.setEnabled(false);
                        Toast.makeText(itemView.getContext(),
                                "You've already logged this habit today",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void setCardBackground(Habit habit, boolean isCompletedForPeriod) {
            if (!habit.getIs_active()) {
                itemView.setAlpha(0.6f);
                itemView.setBackgroundResource(R.drawable.habit_card_inactive);
            } else if (isCompletedForPeriod) {
                itemView.setAlpha(1.0f);
                itemView.setBackgroundResource(R.drawable.habit_card_completed);
            } else {
                itemView.setAlpha(1.0f);
                itemView.setBackgroundResource(R.drawable.habit_card_active);
            }
        }

        private void performHabitToggle(Habit habit, boolean isActivating) {
            ContentValues values = new ContentValues();

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

            // Update button text
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean isCompletedForPeriod = isHabitCompletedForPeriod(habit, today);
            // Update card background
            setCardBackground(habit, isCompletedForPeriod);

            // Refresh sorting
            refreshSorting();

            // Show success message
            String message = isActivating ?
                    "Habit \"" + habit.getName() + "\" has been activated!" :
                    "Habit \"" + habit.getName() + "\" has been disabled.";
            Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
        }

        private void showMaterialConfirmationDialog(Habit habit, boolean isActivating) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(itemView.getContext());

            if (isActivating) {
                builder.setTitle("Activate Habit")
                        .setMessage("Are you sure you want to activate \"" + habit.getName() + "\"?\n\n" +
                                "This will reset your current progress to 0 and start tracking again.")
                        .setIcon(R.drawable.ic_play_arrow_24);
            } else {
                builder.setTitle("Disable Habit")
                        .setMessage("Are you sure you want to disable \"" + habit.getName() + "\"?\n\n" +
                                "This will:\n" +
                                "• Stop tracking this habit\n" +
                                "• Reset your current progress to 0\n" +
                                "• Delete all your habit logs\n\n" +
                                "This action cannot be undone.")
                        .setIcon(R.drawable.ic_pause_24);
            }

            builder.setPositiveButton(isActivating ? "Activate" : "Disable", (dialog, which) -> {
                        performHabitToggle(habit, isActivating);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();


        }
    }
}