package com.example.habbittracker.Adapters;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
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
import com.example.habbittracker.Models.HabitLog;
import com.example.habbittracker.R;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        if (habits.size() > 0) {
            this.listHabits.addAll(habits);
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

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean sudahSelesaiHariIni = false;
            Cursor habitLog = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
            if (habitLog != null && habitLog.moveToFirst()) {
                sudahSelesaiHariIni = true;
            }
            if (habitLog != null) habitLog.close();
            btnFinishHabit.setEnabled(!sudahSelesaiHariIni && habit.getIs_active());

            if (habit.getIs_active()) {
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge);
                tvHabitStatus.setText("Active");
            } else {
                tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
                tvHabitStatus.setText("Inactive");
            }

            btnFinishHabit.setOnClickListener(v -> {
                Cursor c = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
                boolean alreadyLogged = (c != null && c.moveToFirst());
                if (c != null) c.close();

                if (!alreadyLogged) {
                    ContentValues values = new ContentValues();
                    values.put("habit_id", habit.getId());
                    values.put("log_date", today);
                    values.put("status", 1);
                    HabitLogHelper.instance.insert(values);

                    // Update object model dulu
                    habit.setCurrent_count(habit.getCurrent_count() + 1);
                    habit.setIs_active(habit.getCurrent_count() < habit.getTarget_count());

                    // Buat ContentValues untuk update database
                    ContentValues habitValues = new ContentValues();
                    habitValues.put("current_count", habit.getCurrent_count());
                    habitValues.put("is_active", habit.getIs_active() ? 1 : 0);

                    // Update database
                    HabitHelper.instance.update(String.valueOf(habit.getId()), habitValues);

                    // LANGSUNG UPDATE UI DULU SEBELUM NOTIFY
                    // 1. Update text count
                    tvHabitCurrent.setText(String.valueOf(habit.getCurrent_count()));

                    // 2. Update progress bar
                    int progress = 0;
                    if (habit.getTarget_count() > 0) {
                        progress = (int) ((habit.getCurrent_count() * 100.0f) / habit.getTarget_count());
                    }
                    progressBar.setProgress(progress);

                    // 3. Update status badge jika completed
                    if (!habit.getIs_active()) {
                        tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
                        tvHabitStatus.setText("Inactive");
                    }

                    // 4. Disable button
                    btnFinishHabit.setEnabled(false);

                    // JANGAN NOTIFY ITEM CHANGED - ini akan me-reset UI
                    // notifyItemChanged(getAdapterPosition());
                } else {
                    btnFinishHabit.setEnabled(false);
                }
            });
            btnDeactivateHabit.setOnClickListener(v -> {
                ContentValues values = new ContentValues();

                if (habit.getIs_active()) {
                    // Nonaktifkan habit
                    values.put("is_active", 0);
                    values.put("current_count", 0);
                    habit.setIs_active(false);
                    habit.setCurrent_count(0);

                    // Hapus semua log habit ini
                    HabitLogHelper.instance.deleteByHabitId(String.valueOf(habit.getId()));

                    // Update database SEBELUM update UI
                    HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                    // LANGSUNG UPDATE UI
                    tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge_inactive);
                    tvHabitStatus.setText("Inactive");
                    tvHabitCurrent.setText("0");
                    progressBar.setProgress(0);

                    // Disable tombol
                    btnFinishHabit.setEnabled(false);
                    btnSkipHabit.setEnabled(false);
                } else {
                    // Aktifkan ulang
                    values.put("is_active", 1);
                    values.put("current_count", 0);
                    habit.setIs_active(true);
                    habit.setCurrent_count(0);

                    // Update database SEBELUM update UI
                    HabitHelper.instance.update(String.valueOf(habit.getId()), values);

                    // LANGSUNG UPDATE UI
                    tvHabitStatus.setBackgroundResource(R.drawable.habit_status_badge);
                    tvHabitStatus.setText("Active");
                    tvHabitCurrent.setText("0");
                    progressBar.setProgress(0);

                    // Enable tombol
                    btnFinishHabit.setEnabled(true);
                    btnSkipHabit.setEnabled(true);
                }

            });
            btnSkipHabit.setOnClickListener(v -> {

                Cursor c = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
                boolean alreadyLogged = (c != null && c.moveToFirst());
                if (c != null) c.close();

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
            });
        }
    }
}
