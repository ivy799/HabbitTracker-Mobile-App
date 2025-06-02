package com.example.habbittracker.Activities;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HabitFormActivity extends AppCompatActivity {

    public static final String EXTRA_HABIT = "extra_habit";
    public static final int RESULT_ADD = 101;
    public static final int RESULT_UPDATE = 201;
    public static final int RESULT_DELETE = 301;
    public static final int REQUEST_UPDATE = 200;

    private EditText etHabitName,
            etHabitDescription,
            etHabitStartDate,
            etHabitTarget;

    private Spinner spHabitFrequency, spHabitStatus, spHabitCategory;

    private Habit habit;
    private HabitHelper habitHelper;
    private Button btnSave, btnDelete;

    private Boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etHabitName = findViewById(R.id.etHabitName);
        etHabitDescription = findViewById(R.id.etHabitDescription);
        spHabitCategory = findViewById(R.id.spinnerHabitCategory);
        etHabitStartDate = findViewById(R.id.etHabitStartDate);
        etHabitTarget = findViewById(R.id.etHabitTarget);
        spHabitFrequency = findViewById(R.id.spinnerHabitFrequency);
        spHabitStatus = findViewById(R.id.spinnerHabitStatus);
        btnSave = findViewById(R.id.btnSaveHabit);
        btnDelete = findViewById(R.id.btnDeleteHabit);

        habitHelper = HabitHelper.getInstance(getApplicationContext());
        habitHelper.open();

        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                this, R.array.frequency_options, android.R.layout.simple_spinner_item);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHabitFrequency.setAdapter(freqAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                this, R.array.status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHabitStatus.setAdapter(statusAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.category_options, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHabitCategory.setAdapter(categoryAdapter);

        etHabitStartDate.setOnClickListener(v -> showDatePickerDialog());

        habit = getIntent().getParcelableExtra(EXTRA_HABIT);
        if (habit != null){
            isEdit = true;
        } else{
            habit = new Habit();
        }

        String actionBarTitle;
        String btnTitle;

        if (isEdit){
            actionBarTitle = "Edit Habit";
            btnTitle = "Update";
            if (habit != null){
                etHabitName.setText(habit.getName());
                etHabitDescription.setText(habit.getDescription());
                spHabitCategory.setSelection(((ArrayAdapter<String>) spHabitCategory.getAdapter()).getPosition(habit.getCategory()));
                etHabitStartDate.setText(habit.getStart_date().toString());
                etHabitTarget.setText(String.valueOf(habit.getTarget_count()));
                spHabitFrequency.setSelection(((ArrayAdapter<String>) spHabitFrequency.getAdapter()).getPosition(habit.getFrequency()));
                spHabitStatus.setSelection(((ArrayAdapter<String>) spHabitStatus.getAdapter()).getPosition(habit.getIs_active() ? "Active" : "Inactive"));
            }
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            actionBarTitle = "Add Habit";
            btnTitle = "Save";
        }

        btnSave.setText(btnTitle);
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(actionBarTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnSave.setOnClickListener(v -> {
            try {
                saveHabit();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });

        btnDelete.setOnClickListener(v -> deleteHabit());
    }

    private void saveHabit() throws ParseException {
        String name = etHabitName.getText().toString().trim();
        String description = etHabitDescription.getText().toString().trim();
        String category = spHabitCategory.getSelectedItem().toString();
        String startDate = etHabitStartDate.getText().toString().trim();
        int targetCount = Integer.parseInt(etHabitTarget.getText().toString().trim());
        String frequency = spHabitFrequency.getSelectedItem().toString();
        boolean isActive = spHabitStatus.getSelectedItem().toString().equals("Active");

        habit.setName(name);
        habit.setDescription(description);
        habit.setCategory(category);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = sdf.parse(startDate);
        habit.setStart_date(date);
        habit.setTarget_count(targetCount);
        habit.setFrequency(frequency);
        habit.setIs_active(isActive);

        Intent intent = new Intent();
        intent.putExtra(EXTRA_HABIT, habit);

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("category", category);
        values.put("start_date", sdf.format(date));
        values.put("target_count", targetCount);
        values.put("current_count", 0);
        values.put("frequency", frequency);
        values.put("is_active", isActive ? 1 : 0);

        if (isEdit){
            long result = habitHelper.update(String.valueOf(habit.getId()), values);
            if (result > 0){
                setResult(RESULT_UPDATE, intent);
            } else {
                setResult(RESULT_CANCELED);
            }
        }else{
            long result = habitHelper.insert(values);
            if (result > 0){
                habit.setId((int) result);
                setResult(RESULT_ADD, intent);
            } else {
                setResult(RESULT_CANCELED);
                System.out.println("data tidak masuk");
            }
        }

    }

    private void deleteHabit(){
        if (habit != null && habit.getId() > 0){
            long result = habitHelper.deleteById(String.valueOf(habit.getId()));
            if (result > 0){
                setResult(RESULT_DELETE);
                finish();
            }else {
                Toast.makeText(this, "Failed to delete habit", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this, "Habit is empty", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        habitHelper.close();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format tanggal
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    etHabitStartDate.setText(dateStr);
                }, year, month, day);

        datePickerDialog.show();
    }
}