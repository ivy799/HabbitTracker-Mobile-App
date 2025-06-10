package com.example.habbittracker.Fragment;

import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habbittracker.Adapters.RecentActivityAdapter;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
import com.example.habbittracker.Database_config.Habit.HabitMappingHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogsMappingHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.Models.HabitLog;
import com.example.habbittracker.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GraphFragment extends Fragment {

    private static final String TAG = "GraphFragment";

    private TextView tvTotalHabits, tvActiveHabits, tvCompletionRate;
    private LineChart lineChart;
    private BarChart barChart;
    private PieChart pieChart;
    private MaterialButton btnToggleChart;
    private MaterialButton btnRefresh;
    private RecyclerView rvRecentActivity;
    private RecentActivityAdapter activityAdapter;
    private boolean isLineChartVisible = true;
    private ArrayList<Habit> habitList = new ArrayList<>();
    private ArrayList<HabitLog> recentLogs = new ArrayList<>();

    public GraphFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        initViews(view);
        setupClickListeners();
        loadData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }


    private void initViews(View view) {
        tvTotalHabits = view.findViewById(R.id.tvTotalHabits);
        tvActiveHabits = view.findViewById(R.id.tvActiveHabits);
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate);

        lineChart = view.findViewById(R.id.lineChart);
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);

        btnToggleChart = view.findViewById(R.id.btnToggleChart);
        btnRefresh = view.findViewById(R.id.btnRefresh);

        rvRecentActivity = view.findViewById(R.id.rvRecentActivity);

        // Debug: Check if views are found
        Log.d(TAG, "LineChart found: " + (lineChart != null));
        Log.d(TAG, "BarChart found: " + (barChart != null));
        Log.d(TAG, "PieChart found: " + (pieChart != null));

        if (rvRecentActivity != null) {
            rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
            activityAdapter = new RecentActivityAdapter(recentLogs, getContext());
            rvRecentActivity.setAdapter(activityAdapter);
        }
    }
    private void setupClickListeners() {
        if (btnToggleChart != null) {
            btnToggleChart.setOnClickListener(v -> toggleChart());
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refreshData());
        }
    }
    private void loadData() {
        Log.d(TAG, "Loading data...");
        loadHabits();
        loadRecentActivity();
        updateStatistics();
        setupCharts();
    }
    private void loadHabits() {
        habitList.clear();
        try {
            HabitHelper habitHelper = HabitHelper.getInstance(getContext());
            habitHelper.open();

            Cursor cursor = habitHelper.queryAll();
            if (cursor != null) {
                habitList = HabitMappingHelper.mapCursorToArrayList(cursor);
                cursor.close();
                Log.d(TAG, "Loaded " + habitList.size() + " habits");
            } else {
                Log.w(TAG, "Habits cursor is null");
            }
            habitHelper.close();
        } catch (Exception e) {
            Log.e(TAG, "Error loading habits", e);
            e.printStackTrace();
        }
    }
    private void loadRecentActivity() {
        recentLogs.clear();
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String weekAgo = sdf.format(cal.getTime());
            String today = sdf.format(new Date());

            Log.d(TAG, "Querying logs from " + weekAgo + " to " + today);

            if (HabitLogHelper.instance != null) {
                Cursor cursor = HabitLogHelper.instance.queryByDateRange(weekAgo, today);
                if (cursor != null) {
                    recentLogs = HabitLogsMappingHelper.mapCursorToArrayList(cursor);
                    cursor.close();
                    Log.d(TAG, "Loaded " + recentLogs.size() + " recent logs");

                    for (HabitLog log : recentLogs) {
                        Log.d(TAG, "Log: Date=" + log.getLog_date() + ", Status=" + log.getStatus() + ", HabitId=" + log.getHabit_id());
                    }
                } else {
                    Log.w(TAG, "Recent logs cursor is null");
                }
            } else {
                Log.e(TAG, "HabitLogHelper.instance is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recent activity", e);
            e.printStackTrace();
        }

        if (activityAdapter != null) {
            activityAdapter.updateLogs(recentLogs);
        }
    }
    private void updateStatistics() {
        int totalHabits = habitList.size();
        int activeHabits = 0;
        int totalTargetDays = 0;
        int totalCompletedDays = 0;

        for (Habit habit : habitList) {
            if (habit.getIs_active()) {
                activeHabits++;
            }
            totalTargetDays += habit.getTarget_count();
            totalCompletedDays += habit.getCurrent_count();
        }

        float completionRate = totalTargetDays > 0 ?
                (totalCompletedDays * 100.0f) / totalTargetDays : 0;

        Log.d(TAG, "Statistics - Total: " + totalHabits + ", Active: " + activeHabits + ", Completion: " + completionRate + "%");

        if (tvTotalHabits != null) tvTotalHabits.setText(String.valueOf(totalHabits));
        if (tvActiveHabits != null) tvActiveHabits.setText(String.valueOf(activeHabits));
        if (tvCompletionRate != null) tvCompletionRate.setText(String.format(Locale.getDefault(), "%.1f%%", completionRate));
    }
    private void setupCharts() {
        Log.d(TAG, "Setting up charts...");
        setupLineChart();
        setupBarChart();
        setupPieChart();
    }
    private void setupLineChart() {
        if (lineChart == null) {
            Log.e(TAG, "LineChart is null");
            return;
        }

        Log.d(TAG, "Setting up LineChart...");

        List<Entry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Generate data for last 7 days
        for (int i = 6; i >= 0; i--) {
            Calendar tempCal = Calendar.getInstance();
            tempCal.add(Calendar.DAY_OF_MONTH, -i);
            String date = sdf.format(tempCal.getTime());

            int completedCount = countCompletedHabitsForDate(date);
            entries.add(new Entry(6 - i, completedCount));

            Log.d(TAG, "Day " + (6-i) + " - Date: " + date + ", Completed: " + completedCount);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Completed Habits");

        // Get theme colors
        int primaryColor = getThemeColor
                (com.google.android.material.R.attr.colorPrimary);
        int onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface);

        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(primaryColor);
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(onSurfaceColor);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(primaryColor);
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize chart with theme colors
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        // Set chart background color
        boolean isDarkMode = isDarkMode();
        lineChart.setBackgroundColor(Color.TRANSPARENT);

        // X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getLastSevenDays()));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setTextColor(onSurfaceColor);
        xAxis.setAxisLineColor(onSurfaceColor);

        // Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(onSurfaceColor);
        leftAxis.setAxisLineColor(onSurfaceColor);

        lineChart.getAxisRight().setEnabled(false);

        // Legend
        Legend legend = lineChart.getLegend();
        legend.setTextColor(onSurfaceColor);

        lineChart.animateX(1000);
        lineChart.invalidate();

        Log.d(TAG, "LineChart setup completed");
    }
    private void setupBarChart() {
        if (barChart == null) {
            Log.e(TAG, "BarChart is null");
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Generate data for last 7 days
        for (int i = 0; i < 7; i++) {
            Calendar tempCal = Calendar.getInstance();
            tempCal.add(Calendar.DAY_OF_MONTH, -6 + i);
            String date = sdf.format(tempCal.getTime());

            int completedCount = countCompletedHabitsForDate(date);
            entries.add(new BarEntry(i, completedCount));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Completed Habits");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Customize chart
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getLastSevenDays()));
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
    private void setupPieChart() {
        if (pieChart == null) {
            Log.e(TAG, "PieChart is null");
            return;
        }

        Map<String, Integer> categoryCount = new HashMap<>();

        for (Habit habit : habitList) {
            String category = habit.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            // Add dummy data if no habits exist
            entries.add(new PieEntry(1, "No Habits"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());

        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText("Habit Categories");
        pieChart.setCenterTextSize(16f);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        Legend legend = pieChart.getLegend();
        int onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface);
        legend.setTextColor(onSurfaceColor);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }
    private void toggleChart() {
        if (lineChart == null || barChart == null || btnToggleChart == null) {
            Log.e(TAG, "Chart views or button is null");
            return;
        }

        if (isLineChartVisible) {
            lineChart.setVisibility(View.GONE);
            barChart.setVisibility(View.VISIBLE);
            btnToggleChart.setText("Line Chart");
            isLineChartVisible = false;
            Log.d(TAG, "Switched to Bar Chart");
        } else {
            lineChart.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.GONE);
            btnToggleChart.setText("Bar Chart");
            isLineChartVisible = true;
            Log.d(TAG, "Switched to Line Chart");
        }
    }
    private void refreshData() {
        Log.d(TAG, "Refreshing data...");

        // Add refresh animation to button
        if (btnRefresh != null) {
            btnRefresh.animate().rotation(360f).setDuration(500).start();
        }

        loadData();
        Toast.makeText(getContext(), "Data refreshed", Toast.LENGTH_SHORT).show();
    }
    private int countCompletedHabitsForDate(String date) {
        int count = 0;
        Log.d(TAG, "Counting completed habits for date: " + date);

        for (HabitLog log : recentLogs) {
            try {
                String logDate = String.valueOf(log.getLog_date());
                int status = log.getStatus();

                Log.d(TAG, "Checking log - Date: " + logDate + ", Status: " + status);

                if (logDate != null && logDate.equals(date) && status == 1) {
                    count++;
                    Log.d(TAG, "Found completed habit for " + date);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking log", e);
            }
        }

        Log.d(TAG, "Total completed habits for " + date + ": " + count);
        return count;
    }
    private String[] getLastSevenDays() {
        String[] days = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            Calendar tempCal = Calendar.getInstance();
            tempCal.add(Calendar.DAY_OF_MONTH, -i);
            days[6 - i] = sdf.format(tempCal.getTime());
        }

        Log.d(TAG, "Last seven days: " + String.join(", ", days));
        return days;
    }
    private int getThemeColor(int colorAttr) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(colorAttr, typedValue, true)) {
            return ContextCompat.getColor(getContext(), typedValue.resourceId);
        }
        // Fallback colors
        return isDarkMode() ? Color.WHITE : Color.BLACK;
    }
    private boolean isDarkMode() {
        if (getContext() == null) return false;

        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}