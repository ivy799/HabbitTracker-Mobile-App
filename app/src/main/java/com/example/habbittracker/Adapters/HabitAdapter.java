package com.example.habbittracker.Adapters;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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

            btnFinishHabit.setOnClickListener(v -> {
                Cursor c = HabitLogHelper.instance.queryByHabitIdAndDate(habit.getId(), today);
                boolean alreadyLogged = (c != null && c.moveToFirst());
                if (c != null) c.close();

                if (!alreadyLogged) {
                    // Insert log
                    ContentValues values = new ContentValues();
                    values.put("habit_id", habit.getId());
                    values.put("log_date", today); // hanya tanggal!
                    values.put("status", 1);
                    HabitLogHelper.instance.insert(values);

                    // Update habit
                    habit.setCurrent_count(habit.getCurrent_count() + 1);
                    habit.setIs_active(habit.getCurrent_count() < habit.getTarget_count());
                    ContentValues habitValues = new ContentValues();
                    habitValues.put("current_count", habit.getCurrent_count());
                    habitValues.put("is_active", habit.getIs_active() ? 1 : 0);

                    HabitHelper.instance.update(String.valueOf(habit.getId()), habitValues);

                    // Nonaktifkan tombol
                    btnFinishHabit.setEnabled(false);

                    notifyItemChanged(getAdapterPosition());
                } else {
                    btnFinishHabit.setEnabled(false); // Safety
                }
            });

        }
    }
}
