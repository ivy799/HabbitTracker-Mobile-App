package com.example.habbittracker.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Models.HabitLog;
import com.example.habbittracker.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    private ArrayList<HabitLog> logs;
    private Context context;

    public RecentActivityAdapter(ArrayList<HabitLog> logs, Context context) {
        this.logs = logs;
        this.context = context;
    }

    public void updateLogs(ArrayList<HabitLog> newLogs) {
        this.logs.clear();
        this.logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        HabitLog log = logs.get(position);
        holder.bind(log, context);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvHabitName, tvDate, tvStatus;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivActivityIcon);
            tvHabitName = itemView.findViewById(R.id.tvActivityHabitName);
            tvDate = itemView.findViewById(R.id.tvActivityDate);
            tvStatus = itemView.findViewById(R.id.tvActivityStatus);
        }

        void bind(HabitLog log, Context context) {
            String habitName = getHabitNameById(log.getHabit_id(), context);
            tvHabitName.setText(habitName);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            try {
                String dateString = String.valueOf(log.getLog_date()); // misalnya "04-06-2025"
                Date date = formatter.parse(dateString); // ubah String jadi Date
                String formattedDate = formatter.format(date); // ubah Date jadi String lagi kalau mau tampilkan
                tvDate.setText(formattedDate); // tampilkan di TextView
            } catch (ParseException e) {
                e.printStackTrace();
                tvDate.setText("Format tanggal salah!");
            }

            System.out.println("Log Status: " + log.getStatus());


            if (log.getStatus() == 1) {
                tvStatus.setText("Completed");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success_color));
                ivIcon.setImageResource(android.R.drawable.ic_media_play); // Using built-in icon as fallback
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color));
            } else if (log.getStatus() == 2) {
                tvStatus.setText("Skipped");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_color));
                ivIcon.setImageResource(android.R.drawable.ic_media_pause); // Using built-in icon as fallback
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_color));
            } else {
                tvStatus.setText("Unknown");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                ivIcon.setImageResource(android.R.drawable.ic_dialog_info);
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        private String getHabitNameById(int habitId, Context context) {
            try {
                HabitHelper habitHelper = HabitHelper.getInstance(context);
                habitHelper.open();

                Cursor cursor = habitHelper.search(habitId);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex("name");
                    if (nameIndex != -1) {
                        String habitName = cursor.getString(nameIndex);
                        cursor.close();
                        habitHelper.close();
                        return habitName;
                    }
                    cursor.close();
                }
                habitHelper.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Unknown Habit";
        }

        private String formatDate(String dateString) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                return dateString; // Return original if parsing fails
            }
        }
    }
}