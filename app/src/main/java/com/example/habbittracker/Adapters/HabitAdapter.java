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
import java.util.Date;
import java.util.Locale;

public class HabitAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Activity activity;
    private final ArrayList<Object> displayList = new ArrayList<>();
    private ArrayList<Habit> originalHabits = new ArrayList<>();

    // Section titles
    private static final String TITLE_ACTIVE = "Active";
    private static final String TITLE_COMPLETED = "Completed Today";
    private static final String TITLE_INACTIVE = "Inactive";

    // Tambahkan listener untuk event semua habit aktif selesai hari ini
    public interface OnAllHabitsCompletedListener {
        void onAllHabitsCompleted();
    }
    private OnAllHabitsCompletedListener allHabitsCompletedListener;

    public void setOnAllHabitsCompletedListener(OnAllHabitsCompletedListener listener) {
        this.allHabitsCompletedListener = listener;
    }

    public HabitAdapter(Activity activity) {
        this.activity = activity;
    }

    public void setListHabits(ArrayList<Habit> habits) {
        originalHabits = new ArrayList<>(habits);

        ArrayList<Habit> active = new ArrayList<>();
        ArrayList<Habit> completed = new ArrayList<>();
        ArrayList<Habit> inactive = new ArrayList<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

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
            if (!habit.getIs_active()) {
                inactive.add(habit);
            } else if (isHabitCompletedForPeriod(habit, today)) {
                completed.add(habit);
            } else {
                active.add(habit);
            }
        }

        displayList.clear();
        if (!active.isEmpty()) {
            displayList.add(TITLE_ACTIVE);
            displayList.addAll(active);
        }
        if (!completed.isEmpty()) {
            displayList.add(TITLE_COMPLETED);
            displayList.addAll(completed);
        }
        if (!inactive.isEmpty()) {
            displayList.add(TITLE_INACTIVE);
            displayList.addAll(inactive);
        }
        notifyDataSetChanged();

        // Cek jika semua habit aktif sudah selesai
        boolean hasActiveHabit = false;
        for (Habit h : habits) {
            if (h.getIs_active()) {
                hasActiveHabit = true;
                break;
            }
        }
        if (hasActiveHabit && active.isEmpty() && allHabitsCompletedListener != null) {
            allHabitsCompletedListener.onAllHabitsCompleted();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.habit_item, parent, false);
            return new HabitViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((String) displayList.get(position));
        } else {
            ((HabitViewHolder) holder).bind((Habit) displayList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    private void sortHabitsByCompletionStatus() {
        // Sorting logic is now handled in setListHabits
    }

    /**
     * Cek apakah habit sudah completed/skipped untuk periode yang sesuai
     */
    private boolean isHabitCompletedForPeriod(Habit habit, String today) {
        boolean completedToday = false;
        boolean weeklyCooldownActive = false;
        boolean monthlyCooldownActive = false;

        Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
        if (habitLog != null && habitLog.moveToFirst()) {
            int statusColumnIndex = habitLog.getColumnIndex("status");
            if (statusColumnIndex != -1) {
                int status = habitLog.getInt(statusColumnIndex);
                completedToday = (status == 1 || status == 2); // completed or skipped
            }
            habitLog.close();
        }

        if (habit.getFrequency().equalsIgnoreCase("daily")) {
            return completedToday;
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
                        weeklyCooldownActive = diffDays < 7; // still in cooldown
                    } catch (Exception e) {
                        weeklyCooldownActive = false;
                    }
                }
                lastWeeklyLog.close();
            }
            return completedToday || weeklyCooldownActive;
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
        setListHabits(originalHabits);
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeader;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvSectionHeader);
        }
        public void bind(String header) {
            tvHeader.setText(header);
        }
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
                detailsSection.setVisibility(View.VISIBLE);
                ObjectAnimator rotation = ObjectAnimator.ofFloat(btnToggleDetails, "rotation", 0f, 180f);
                rotation.setDuration(200);
                rotation.start();
            } else {
                detailsSection.setVisibility(View.GONE);
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

            // --- Tambahan: Ubah warna tombol skip sesuai status selesai/skip ---
            if (sudahSelesaiHariIni) {
                if (isCompletedToday) {
                    // Habit selesai (completed)
                    btnFinishHabit.setIconResource(R.drawable.ic_check_circle_done_24);
                    btnSkipHabit.setIconResource(R.drawable.ic_skip_next_24);
                    // Skip button warna default
                    btnSkipHabit.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.primary_light));
                } else {
                    // Habit di-skip
                    btnFinishHabit.setIconResource(R.drawable.ic_check_24);
                    btnSkipHabit.setIconResource(R.drawable.ic_skip_next_24);
                    // Ganti warna tombol skip saat skip
                    btnSkipHabit.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.warning_color));
                }
            } else {
                // Belum selesai, icon dan warna default
                btnFinishHabit.setIconResource(R.drawable.ic_check_24);
                btnSkipHabit.setIconResource(R.drawable.ic_skip_next_24);
                btnSkipHabit.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.primary_light));
            }
            // --- End tambahan ---

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

            boolean canCompleteToday;
            if (habit.getFrequency().equalsIgnoreCase("weekly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && weeklyCooldownPassed;
            } else if (habit.getFrequency().equalsIgnoreCase("monthly")) {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni && monthlyCooldownPassed;
            } else {
                canCompleteToday = habit.getIs_active() && !sudahSelesaiHariIni;
            }

            if (isCompletedToday) {
                btnFinishHabit.setEnabled(true);
                btnFinishHabit.setOnClickListener(v -> {
                    undoHabitLog(habit, today);
                    refreshSorting();
                });
                btnSkipHabit.setEnabled(false);
            } else if (canCompleteToday) {
                btnFinishHabit.setEnabled(true);
                btnFinishHabit.setOnClickListener(v -> {
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
                        refreshSorting();
                    }
                });
                btnSkipHabit.setEnabled(true);
            } else {
                btnFinishHabit.setEnabled(false);
                btnFinishHabit.setOnClickListener(null);
                btnSkipHabit.setEnabled(false);
            }

            btnSkipHabit.setOnClickListener(v -> {
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

                            // Hanya ubah warna tombol skip, icon tetap
                            btnSkipHabit.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.warning_color));

                            Toast.makeText(itemView.getContext(),
                                    "Habit skipped for today. Your streak is preserved!",
                                    Toast.LENGTH_SHORT).show();

                            refreshSorting();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

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

            tvHabitStatus.setBackgroundResource(isActivating ? R.drawable.habit_status_badge : R.drawable.habit_status_badge_inactive);
            tvHabitStatus.setText(isActivating ? "Active" : "Inactive");
            tvHabitCurrent.setText("0");
            progressBar.setProgress(0);
            btnFinishHabit.setEnabled(isActivating);
            btnSkipHabit.setEnabled(isActivating);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean isCompletedForPeriod = isHabitCompletedForPeriod(habit, today);
            setCardBackground(habit, isCompletedForPeriod);

            refreshSorting();

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
                    .setIcon(R.drawable.ic_celebration_24)
                    .setCancelable(false)
                    .setPositiveButton("Continue Habit", (dialog, which) -> {
                        showContinueHabitDialog(habit);
                    })
                    .setNegativeButton("Stop & Rest", (dialog, which) -> {
                        completeAndDeactivateHabit(habit);
                        refreshSorting();
                    })
                    .show();
        }

        private void showContinueHabitDialog(Habit habit) {
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_continue_habit, null);

            EditText etNewTarget = dialogView.findViewById(R.id.etNewTarget);
            TextView tvCurrentStreak = dialogView.findViewById(R.id.tvCurrentStreak);
            TextView tvHabitName = dialogView.findViewById(R.id.tvHabitName);

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
                                    refreshSorting();
                                } else {
                                    Toast.makeText(itemView.getContext(), "Please enter a valid target greater than 0", Toast.LENGTH_SHORT).show();
                                    showContinueHabitDialog(habit);
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(itemView.getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                                showContinueHabitDialog(habit);
                            }
                        } else {
                            Toast.makeText(itemView.getContext(), "Please enter a target", Toast.LENGTH_SHORT).show();
                            showContinueHabitDialog(habit);
                        }
                    })
                    .setNegativeButton("Back", (dialog, which) -> {
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

                // --- Tambahan: kembalikan warna tombol skip ke semula saat undo ---
                btnSkipHabit.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.primary_light));
                // --- End tambahan ---

                Toast.makeText(itemView.getContext(), "Habit log berhasil di-undo!", Toast.LENGTH_SHORT).show();

                refreshSorting();
            } else {
                Toast.makeText(itemView.getContext(), "Tidak ada log untuk di-undo!", Toast.LENGTH_SHORT).show();
            }
            btnFinishHabit.setOnClickListener(v -> bind(habit));
            btnFinishHabit.setEnabled(true);
            btnSkipHabit.setEnabled(true);
        }

        private void continueHabitWithNewTarget(Habit habit, int newTarget) {
            try {
                ContentValues values = new ContentValues();
                values.put("target_count", newTarget);
                values.put("is_active", 1);

                habit.setTarget_count(newTarget);
                habit.setIs_active(true);

                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                tvHabitTarget.setText(String.valueOf(newTarget));
                int newProgress = (int) ((habit.getCurrent_count() * 100.0f) / newTarget);
                progressBar.setProgress(newProgress);

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
                ContentValues values = new ContentValues();
                values.put("is_active", 0);
                values.put("current_count", 0);

                habit.setIs_active(false);
                habit.setCurrent_count(0);

                HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                tvHabitCurrent.setText("0");
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_completed);
                tvHabitStatus.setText("Completed");
                progressBar.setProgress(100);

                btnFinishHabit.setEnabled(false);
                btnSkipHabit.setEnabled(false);

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
                habitValues.put("is_active", 1);

                HabitHelper.instance.update(String.valueOf(habit.getId()), habitValues);

                tvHabitCurrent.setText(String.valueOf(newCurrentCount));
                int newProgress = (int) ((newCurrentCount * 100.0f) / habit.getTarget_count());
                progressBar.setProgress(newProgress);

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
