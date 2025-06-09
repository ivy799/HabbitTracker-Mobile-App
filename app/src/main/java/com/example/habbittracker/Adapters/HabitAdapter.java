package com.example.habbittracker.Adapters;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        final MaterialButton btnFinishHabit, btnSkipHabit, btnDeactivateHabit, btnEditHabit;
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
            boolean isCompletedToday = false;
            int logStatusToday = -1;

            Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
            if (habitLog != null && habitLog.moveToFirst()) {
                int statusColumnIndex = habitLog.getColumnIndex("status");
                if (statusColumnIndex != -1) {
                    logStatusToday = habitLog.getInt(statusColumnIndex);
                    sudahSelesaiHariIni = (logStatusToday == 1 || logStatusToday == 2);
                    isCompletedToday = (logStatusToday == 1);
                }
                habitLog.close();
            }

            boolean weeklyCooldownPassed = true;
            boolean monthlyCooldownPassed = true;

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

            // Tag/status & warna card
            setCardBackground(habit, sudahSelesaiHariIni);

            if (habit.getIs_active()) {
                if (sudahSelesaiHariIni) {
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

            // Tombol Complete dan Skip: enable jika belum "completed/skipped" hari ini, kecuali jika sudah completed, tombol Complete jadi "Undo Complete" dan tetap bisa diklik
            boolean canCompleteToday;
            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && weeklyCooldownPassed;
            } else if (habit.getFrequency().equalsIgnoreCase("monthly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && monthlyCooldownPassed;
            } else {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni;
            }

            // ---- TOMBOL COMPLETE ----
            if (isCompletedToday) {
                btnFinishHabit.setEnabled(true);
                btnFinishHabit.setOnClickListener(v -> {
                    undoHabitLog(habit, today);
                    bind(habit);
                });
                btnSkipHabit.setEnabled(false);
            } else if (canCompleteToday) {
                btnFinishHabit.setEnabled(true);
                btnFinishHabit.setOnClickListener(v -> {
                    // Insert completion log
                    ContentValues values = new ContentValues();
                    values.put("habit_id", habit.getId());
                    values.put("log_date", today);
                    values.put("status", 1);
                    HabitLogHelper.instance.insert(values);

                    int newCurrentCount = habit.getCurrent_count() + 1;
                    habit.setCurrent_count(newCurrentCount);

                    if (newCurrentCount >= habit.getTarget_count()) {
                        showStreakCompletionDialog(habit);
                    } else {
                        updateHabitProgress(habit, newCurrentCount);
                    }
                    // Setelah complete, bind ulang supaya tombol berubah menjadi "Undo Complete"
                    bind(habit);
                });
                btnSkipHabit.setEnabled(true);
            } else {
                // Sudah skip hari ini atau tidak bisa di-complete
                btnFinishHabit.setEnabled(false);
                btnFinishHabit.setOnClickListener(null);
                btnSkipHabit.setEnabled(false);
            }

            // ---- TOMBOL SKIP ----
            btnSkipHabit.setOnClickListener(v -> {
                // Tampilkan dialog konfirmasi sebelum skip
                new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                        .setTitle("Skip Habit")
                        .setMessage("Are you sure you want to skip this habit for today? Your streak will be preserved.")
                        .setPositiveButton("Skip", (dialog, which) -> {
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
                            // bind ulang agar update state UI
                            bind(habit);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // deactivate dan edit tetap sama
            btnDeactivateHabit.setOnClickListener(v -> {
                boolean isActivating = !habit.getIs_active();
                showMaterialConfirmationDialog(habit, isActivating);
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

        private void showStreakCompletionDialog(Habit habit) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(itemView.getContext())
                    .setTitle("🎉 Streak Completed!")
                    .setMessage("Congratulations! You've completed your habit streak for \"" + habit.getName() + "\"!\n\n" +
                            "Current streak: " + habit.getCurrent_count() + "/" + habit.getTarget_count() + " completed\n\n" +
                            "What would you like to do next?")
                    .setIcon(R.drawable.ic_celebration_24) // Tambah icon celebration
                    .setCancelable(false) // Tidak bisa dibatalkan dengan back button
                    .setPositiveButton("Continue Habit", (dialog, which) -> {
                        // User memilih untuk melanjutkan habit
                        showContinueHabitDialog(habit);
                    })
                    .setNegativeButton("Stop & Rest", (dialog, which) -> {
                        // User memilih untuk berhenti dan istirahat
                        completeAndDeactivateHabit(habit);
                    })
                    .show();
        }

        private void showContinueHabitDialog(Habit habit) {
            // Create custom layout for input
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_continue_habit, null);

            EditText etNewTarget = dialogView.findViewById(R.id.etNewTarget);
            TextView tvCurrentStreak = dialogView.findViewById(R.id.tvCurrentStreak);
            TextView tvHabitName = dialogView.findViewById(R.id.tvHabitName);

            // Set current information
            tvHabitName.setText(habit.getName());
            tvCurrentStreak.setText("Current streak: " + habit.getCurrent_count() + " days completed");
            etNewTarget.setHint("Enter new target (e.g., 30)");

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(itemView.getContext())
                    .setTitle("Set New Target")
                    .setView(dialogView)
                    .setIcon(R.drawable.ic_target_24)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        String newTargetStr = etNewTarget.getText().toString().trim();
                        if (!newTargetStr.isEmpty()) {
                            try {
                                int newTarget = Integer.parseInt(newTargetStr);
                                if (newTarget > 0) {
                                    continueHabitWithNewTarget(habit, newTarget);
                                } else {
                                    Toast.makeText(itemView.getContext(), "Please enter a valid target greater than 0", Toast.LENGTH_SHORT).show();
                                    showContinueHabitDialog(habit); // Show dialog again
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(itemView.getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                                showContinueHabitDialog(habit); // Show dialog again
                            }
                        } else {
                            Toast.makeText(itemView.getContext(), "Please enter a target", Toast.LENGTH_SHORT).show();
                            showContinueHabitDialog(habit); // Show dialog again
                        }
                    })
                    .setNegativeButton("Back", (dialog, which) -> {
                        // Kembali ke dialog sebelumnya
                        showStreakCompletionDialog(habit);
                    })
                    .setCancelable(false)
                    .show();
        }

        private void undoHabitLog(Habit habit, String today) {

            int deletedRows = HabitLogHelper.instance.deleteByHabitIdAndDate(habit.getId(), today);

            if (deletedRows > 0) {
                int newCurrentCount = habit.getCurrent_count() > 0 ? habit.getCurrent_count() - 1 : 0;
                habit.setCurrent_count(newCurrentCount);

                ContentValues values = new ContentValues();
                values.put("current_count", newCurrentCount);

                if (!habit.getIs_active() && newCurrentCount < habit.getTarget_count()) {
                    values.put("is_active", 1);
                    habit.setIs_active(true);
                }

                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                tvHabitCurrent.setText(String.valueOf(newCurrentCount));
                int newProgress = (int) ((newCurrentCount * 100.0f) / habit.getTarget_count());
                progressBar.setProgress(newProgress);

                btnFinishHabit.setEnabled(habit.getIs_active());
                btnSkipHabit.setEnabled(habit.getIs_active());

                setCardBackground(habit, false);

                Toast.makeText(itemView.getContext(), "Habit log berhasil di-undo!", Toast.LENGTH_SHORT).show();

                refreshSorting();
            } else {
                Toast.makeText(itemView.getContext(), "Tidak ada log untuk di-undo!", Toast.LENGTH_SHORT).show();
            }
            // Pada undoHabitLog, setelah sukses undo:
            btnFinishHabit.setOnClickListener(v -> bind(habit)); // reset ke listener awal (atau panggil bind(habit))
            btnFinishHabit.setEnabled(true);
            btnSkipHabit.setEnabled(true);
        }

        private void continueHabitWithNewTarget(Habit habit, int newTarget) {
            try {
                // Update target tanpa reset current_count
                ContentValues values = new ContentValues();
                values.put("target_count", newTarget);
                values.put("is_active", 1); // Pastikan tetap aktif

                habit.setTarget_count(newTarget);
                habit.setIs_active(true);

                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                // Update UI
                tvHabitTarget.setText(String.valueOf(newTarget));
                int newProgress = (int) ((habit.getCurrent_count() * 100.0f) / newTarget);
                progressBar.setProgress(newProgress);

                // Update status
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge);
                tvHabitStatus.setText("Active");

                Toast.makeText(itemView.getContext(),
                        "Great! Your new target is " + newTarget + " days. Keep going! 💪",
                        Toast.LENGTH_LONG).show();

                refreshSorting();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(itemView.getContext(),
                        "Error updating habit target",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void completeAndDeactivateHabit(Habit habit) {
            try {
                // Update habit menjadi tidak aktif dan reset current count
                ContentValues values = new ContentValues();
                values.put("is_active", 0);
                values.put("current_count", 0); // Reset streak

                habit.setIs_active(false);
                habit.setCurrent_count(0);

                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                // Update UI
                tvHabitCurrent.setText("0");
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_completed);
                tvHabitStatus.setText("Completed");
                progressBar.setProgress(100); // Show full progress as completed

                // Disable buttons
                btnFinishHabit.setEnabled(false);
                btnSkipHabit.setEnabled(false);

                // Update card background
                setCardBackground(habit, true);

                Toast.makeText(itemView.getContext(),
                        "🎉 Habit completed successfully! You can reactivate it anytime from the menu.",
                        Toast.LENGTH_LONG).show();

                refreshSorting();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(itemView.getContext(),
                        "Error completing habit",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void updateHabitProgress(Habit habit, int newCurrentCount) {
            try {
                ContentValues habitValues = new ContentValues();
                habitValues.put("current_count", newCurrentCount);
                habitValues.put("is_active", 1); // Tetap aktif

                HabitHelper.instance.update(String.valueOf(habit.getId()), habitValues);

                // Update UI
                tvHabitCurrent.setText(String.valueOf(newCurrentCount));
                int newProgress = (int) ((newCurrentCount * 100.0f) / habit.getTarget_count());
                progressBar.setProgress(newProgress);

                // Show encouraging message
                int remaining = habit.getTarget_count() - newCurrentCount;
                Toast.makeText(itemView.getContext(),
                        "Great job! Only " + remaining + " more days to complete your streak! 🔥",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(itemView.getContext(),
                        "Error updating habit progress",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}