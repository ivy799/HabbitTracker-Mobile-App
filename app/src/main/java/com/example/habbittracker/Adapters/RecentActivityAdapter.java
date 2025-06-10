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
import com.google.android.material.card.MaterialCardView;
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
        MaterialCardView statusBadgeContainer;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivActivityIcon);
            tvHabitName = itemView.findViewById(R.id.tvActivityHabitName);
            tvDate = itemView.findViewById(R.id.tvActivityDate);
            tvStatus = itemView.findViewById(R.id.tvActivityStatus);
            statusBadgeContainer = itemView.findViewById(R.id.statusBadgeContainer);
        }

        void bind(HabitLog log, Context context) {
            String habitName = getHabitNameById(log.getHabit_id(), context);
            tvHabitName.setText(habitName);
            String formattedDate = formatDate(String.valueOf(log.getLog_date()));
            tvDate.setText(formattedDate);

            setActivityStatusAndIcon(log.getStatus(), context);
        }

        private void setActivityStatusAndIcon(int status, Context context) {
            String statusText;
            int iconRes;
            int bgColorAttr;
            int textColorAttr;

            switch (status) {
                case 1:
                    statusText = "Completed";
                    iconRes = R.drawable.ic_check_circle_24;
                    bgColorAttr = com.google.android.material.R.attr.colorSecondary;
                    textColorAttr = com.google.android.material.R.attr.colorOnSecondary;
                    break;

                case 2:
                    statusText = "Skipped";
                    iconRes = R.drawable.ic_help_outline_24;
                    bgColorAttr = com.google.android.material.R.attr.colorTertiary;
                    textColorAttr = com.google.android.material.R.attr.colorOnTertiary;
                    break;

                case 3:
                    statusText = "Missed";
                    iconRes = R.drawable.ic_help_outline_24;
                    bgColorAttr = com.google.android.material.R.attr.colorError;
                    textColorAttr = com.google.android.material.R.attr.colorOnError;
                    break;

                default:
                    statusText = "Unknown";
                    iconRes = R.drawable.ic_help_outline_24;
                    bgColorAttr = com.google.android.material.R.attr.colorSurfaceVariant;
                    textColorAttr = com.google.android.material.R.attr.colorOnSurfaceVariant;
                    break;
            }

            tvStatus.setText(statusText);
            ivIcon.setImageResource(iconRes);

            int backgroundColor = getThemeColor(context, bgColorAttr);
            int textColor = getThemeColor(context, textColorAttr);
            int iconColor = getThemeColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer);

            statusBadgeContainer.setCardBackgroundColor(backgroundColor);
            tvStatus.setTextColor(textColor);
            ivIcon.setColorFilter(iconColor);
        }

        private int getThemeColor(Context context, int colorAttr) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            android.content.res.Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(colorAttr, typedValue, true);
            return ContextCompat.getColor(context, typedValue.resourceId);
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

                SimpleDateFormat todayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = todayFormat.format(new Date());

                if (dateString.equals(today)) {
                    return "Today";
                }

                Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
                String yesterdayStr = todayFormat.format(yesterday);

                if (dateString.equals(yesterdayStr)) {
                    return "Yesterday";
                }

                return outputFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
                return dateString;
            }
        }
    }
}