package com.example.habbittracker.Fragment;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GraphFragment extends Fragment {

    private TextView tvTotalHabits, tvActiveHabits, tvCompletionRate;
    private LineChart lineChart;
    private BarChart barChart;
    private PieChart pieChart;
    private MaterialButton btnToggleChart;
    private ImageView btnRefresh;
    private RecyclerView rvRecentActivity;
    private RecentActivityAdapter activityAdapter;

    private boolean isLineChartVisible = true;
    private ArrayList<Habit> habitList = new ArrayList<>();
    private ArrayList<HabitLog> recentLogs = new ArrayList<>();

    public GraphFragment() {
        // Required empty public constructor
    }

    public static GraphFragment newInstance() {
        return new GraphFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        initViews(view);
        setupClickListeners();
        loadData();

        return view;
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
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with context
        activityAdapter = new RecentActivityAdapter(recentLogs, getContext());
        rvRecentActivity.setAdapter(activityAdapter);
    }

    private void setupClickListeners() {
        btnToggleChart.setOnClickListener(v -> toggleChart());
        btnRefresh.setOnClickListener(v -> refreshData());
    }

    private void loadData() {
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
            }
            habitHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentActivity() {
        recentLogs.clear();
        try {
            // Get recent logs from last 7 days
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String weekAgo = sdf.format(cal.getTime());
            String today = sdf.format(new Date());

            Cursor cursor = HabitLogHelper.instance.queryByDateRange(weekAgo, today);
            if (cursor != null) {
                recentLogs = HabitLogsMappingHelper.mapCursorToArrayList(cursor);
                cursor.close();
            }
        } catch (Exception e) {
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

        tvTotalHabits.setText(String.valueOf(totalHabits));
        tvActiveHabits.setText(String.valueOf(activeHabits));
        tvCompletionRate.setText(String.format(Locale.getDefault(), "%.1f%%", completionRate));
    }

    private void setupCharts() {
        setupLineChart();
        setupBarChart();
        setupPieChart();
    }

    private void setupLineChart() {
        List<Entry> entries = new ArrayList<>();

        // Generate data for last 7 days
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            // Count completed habits for this date
            int completedCount = countCompletedHabitsForDate(date);
            entries.add(new Entry(6 - i, completedCount));

            cal.add(Calendar.DAY_OF_MONTH, i); // Reset
        }

        LineDataSet dataSet = new LineDataSet(entries, "Completed Habits");
        dataSet.setColor(Color.parseColor("#6C5CE7"));
        dataSet.setCircleColor(Color.parseColor("#6C5CE7"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#A29BFE"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize chart
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        // X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getLastSevenDays()));
        xAxis.setGranularity(1f);

        // Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void setupBarChart() {
        List<BarEntry> entries = new ArrayList<>();

        // Generate data for last 7 days
        for (int i = 0; i < 7; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -6 + i);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

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
        Map<String, Integer> categoryCount = new HashMap<>();

        // Count habits by category
        for (Habit habit : habitList) {
            String category = habit.getCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
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

        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void toggleChart() {
        if (isLineChartVisible) {
            lineChart.setVisibility(View.GONE);
            barChart.setVisibility(View.VISIBLE);
            btnToggleChart.setText("Line Chart");
            isLineChartVisible = false;
        } else {
            lineChart.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.GONE);
            btnToggleChart.setText("Bar Chart");
            isLineChartVisible = true;
        }
    }

    private void refreshData() {
        // Add refresh animation to button
        btnRefresh.animate().rotation(360f).setDuration(500).start();

        loadData();
    }

    private int countCompletedHabitsForDate(String date) {
        int count = 0;
        for (HabitLog log : recentLogs) {
            if (log.getLog_date().equals(date) && log.isStatus() == 1) { // 1 = completed
                count++;
            }
        }
        return count;
    }

    private String[] getLastSevenDays() {
        String[] days = new String[7];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            cal.add(Calendar.DAY_OF_MONTH, -i);
            days[6 - i] = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, i); // Reset
        }

        return days;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
}