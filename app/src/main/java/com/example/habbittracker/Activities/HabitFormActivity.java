package com.example.habbittracker.Activities;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
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

    // Changed from Spinner to AutoCompleteTextView
    private AutoCompleteTextView spHabitFrequency, spHabitStatus, spHabitCategory;

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

        // Initialize views
        initializeViews();

        habitHelper = HabitHelper.getInstance(getApplicationContext());
        habitHelper.open();

        setupSpinners();
        etHabitStartDate.setOnClickListener(v -> showDatePickerDialog());

        habit = getIntent().getParcelableExtra(EXTRA_HABIT);

        // Jika tidak ada EXTRA_HABIT, cek habit_id
        if (habit == null) {
            int habitId = getIntent().getIntExtra("habit_id", -1);
            if (habitId != -1) {
                habit = loadHabitById(habitId);
            }
        }

        if (habit != null) {
            isEdit = true;
        } else {
            habit = new Habit();
        }

        setupUI();
        setupClickListeners();
    }

    private void initializeViews() {
        etHabitName = findViewById(R.id.etHabitName);
        etHabitDescription = findViewById(R.id.etHabitDescription);
        spHabitCategory = findViewById(R.id.spinnerHabitCategory);
        etHabitStartDate = findViewById(R.id.etHabitStartDate);
        etHabitTarget = findViewById(R.id.etHabitTarget);
        spHabitFrequency = findViewById(R.id.spinnerHabitFrequency);
        spHabitStatus = findViewById(R.id.spinnerHabitStatus);
        btnSave = findViewById(R.id.btnSaveHabit);
        btnDelete = findViewById(R.id.btnDeleteHabit);
    }

    private void setupSpinners() {
        // Setup frequency spinner
        String[] frequencyOptions = getResources().getStringArray(R.array.frequency_options);
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, frequencyOptions);
        spHabitFrequency.setAdapter(freqAdapter);

        // Setup status spinner
        String[] statusOptions = getResources().getStringArray(R.array.status_options);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, statusOptions);
        spHabitStatus.setAdapter(statusAdapter);

        // Setup category spinner
        String[] categoryOptions = getResources().getStringArray(R.array.category_options);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryOptions);
        spHabitCategory.setAdapter(categoryAdapter);
    }

    /**
     * Load habit data dari database berdasarkan ID
     */
    private Habit loadHabitById(int habitId) {
        try {
            Cursor cursor = habitHelper.search(habitId);
            if (cursor != null && cursor.moveToFirst()) {
                Habit loadedHabit = new Habit();

                // Extract data dari cursor
                int idIndex = cursor.getColumnIndex("id");
                int nameIndex = cursor.getColumnIndex("name");
                int descriptionIndex = cursor.getColumnIndex("description");
                int categoryIndex = cursor.getColumnIndex("category");
                int startDateIndex = cursor.getColumnIndex("start_date");
                int targetCountIndex = cursor.getColumnIndex("target_count");
                int currentCountIndex = cursor.getColumnIndex("current_count");
                int frequencyIndex = cursor.getColumnIndex("frequency");
                int isActiveIndex = cursor.getColumnIndex("is_active");

                if (idIndex >= 0) loadedHabit.setId(cursor.getInt(idIndex));
                if (nameIndex >= 0) loadedHabit.setName(cursor.getString(nameIndex));
                if (descriptionIndex >= 0) loadedHabit.setDescription(cursor.getString(descriptionIndex));
                if (categoryIndex >= 0) loadedHabit.setCategory(cursor.getString(categoryIndex));
                if (targetCountIndex >= 0) loadedHabit.setTarget_count(cursor.getInt(targetCountIndex));
                if (currentCountIndex >= 0) loadedHabit.setCurrent_count(cursor.getInt(currentCountIndex));
                if (frequencyIndex >= 0) loadedHabit.setFrequency(cursor.getString(frequencyIndex));
                if (isActiveIndex >= 0) loadedHabit.setIs_active(cursor.getInt(isActiveIndex) == 1);

                // Parse start date
                if (startDateIndex >= 0) {
                    String dateStr = cursor.getString(startDateIndex);
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date date = sdf.parse(dateStr);
                        loadedHabit.setStart_date(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        loadedHabit.setStart_date(new Date()); // Default ke hari ini
                    }
                }

                cursor.close();
                return loadedHabit;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading habit data", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void setupUI() {
        String actionBarTitle;
        String btnTitle;

        if (isEdit) {
            actionBarTitle = "Edit Habit";
            btnTitle = "Update";
            populateEditForm();
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            actionBarTitle = "Add Habit";
            btnTitle = "Save";
            btnDelete.setVisibility(View.GONE);
        }

        btnSave.setText(btnTitle);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(actionBarTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Populate form dengan data habit untuk edit
     */
    private void populateEditForm() {
        if (habit != null) {
            etHabitName.setText(habit.getName());
            etHabitDescription.setText(habit.getDescription());
            spHabitCategory.setText(habit.getCategory(), false);
            etHabitTarget.setText(String.valueOf(habit.getTarget_count()));
            spHabitFrequency.setText(habit.getFrequency(), false);
            spHabitStatus.setText(habit.getIs_active() ? "Active" : "Inactive", false);

            // Format tanggal untuk ditampilkan
            if (habit.getStart_date() != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                etHabitStartDate.setText(displayFormat.format(habit.getStart_date()));
            }
        }
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                try {
                    saveHabit();
                } catch (ParseException e) {
                    Toast.makeText(this, "Error parsing date", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Validasi input sebelum save
     */
    private boolean validateInput() {
        String name = etHabitName.getText().toString().trim();
        String description = etHabitDescription.getText().toString().trim();
        String category = spHabitCategory.getText().toString().trim();
        String startDate = etHabitStartDate.getText().toString().trim();
        String targetStr = etHabitTarget.getText().toString().trim();
        String frequency = spHabitFrequency.getText().toString().trim();
        String status = spHabitStatus.getText().toString().trim();

        if (name.isEmpty()) {
            etHabitName.setError("Habit name is required");
            etHabitName.requestFocus();
            return false;
        }

        if (description.isEmpty()) {
            etHabitDescription.setError("Description is required");
            etHabitDescription.requestFocus();
            return false;
        }

        if (category.isEmpty()) {
            spHabitCategory.setError("Please select a category");
            spHabitCategory.requestFocus();
            return false;
        }

        if (startDate.isEmpty()) {
            etHabitStartDate.setError("Start date is required");
            etHabitStartDate.requestFocus();
            return false;
        }

        if (targetStr.isEmpty()) {
            etHabitTarget.setError("Target count is required");
            etHabitTarget.requestFocus();
            return false;
        }

        try {
            int target = Integer.parseInt(targetStr);
            if (target <= 0) {
                etHabitTarget.setError("Target must be greater than 0");
                etHabitTarget.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etHabitTarget.setError("Please enter a valid number");
            etHabitTarget.requestFocus();
            return false;
        }

        if (frequency.isEmpty()) {
            spHabitFrequency.setError("Please select frequency");
            spHabitFrequency.requestFocus();
            return false;
        }

        if (status.isEmpty()) {
            spHabitStatus.setError("Please select status");
            spHabitStatus.requestFocus();
            return false;
        }

        return true;
    }

    private void saveHabit() throws ParseException {
        String name = etHabitName.getText().toString().trim();
        String description = etHabitDescription.getText().toString().trim();
        String category = spHabitCategory.getText().toString().trim();
        String startDate = etHabitStartDate.getText().toString().trim();
        int targetCount = Integer.parseInt(etHabitTarget.getText().toString().trim());
        String frequency = spHabitFrequency.getText().toString().trim();
        boolean isActive = spHabitStatus.getText().toString().equals("Active");

        // Update habit object
        habit.setName(name);
        habit.setDescription(description);
        habit.setCategory(category);
        habit.setTarget_count(targetCount);
        habit.setFrequency(frequency);
        habit.setIs_active(isActive);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = sdf.parse(startDate);
        habit.setStart_date(date);

        // Prepare ContentValues
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("category", category);
        values.put("start_date", sdf.format(date));
        values.put("target_count", targetCount);
        values.put("frequency", frequency);
        values.put("is_active", isActive ? 1 : 0);

        Intent intent = new Intent();
        intent.putExtra(EXTRA_HABIT, habit);

        if (isEdit) {
            // Update existing habit - jangan ubah current_count saat edit
            long result = habitHelper.update(String.valueOf(habit.getId()), values);
            if (result > 0) {
                setResult(RESULT_UPDATE, intent);
                Toast.makeText(this, "Habit updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update habit", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Add new habit - TAMBAH current_count untuk habit baru
            values.put("current_count", 0); // PENTING: Tambah ini!

            long result = habitHelper.insert(values);
            if (result > 0) {
                habit.setId((int) result);
                habit.setCurrent_count(0); // Set di object juga
                setResult(RESULT_ADD, intent);
                Toast.makeText(this, "Habit added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to add habit", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showDeleteConfirmation() {
        // Cek berapa banyak logs yang akan dihapus
        int logCount = getHabitLogCount(habit.getId());

        String message = "Are you sure you want to delete \"" + habit.getName() + "\"?\n\n" +
                "This will permanently delete:\n" +
                "• The habit\n";

        if (logCount > 0) {
            message += "• " + logCount + " progress logs\n";
        }

        message += "\nThis action cannot be undone.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Habit")
                .setMessage(message)
                .setIcon(R.drawable.ic_delete_24)
                .setPositiveButton("Delete", (dialog, which) -> deleteHabitWithLogs())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Hitung jumlah logs untuk habit ini
     */
    private int getHabitLogCount(int habitId) {
        try {
            Cursor cursor = HabitLogHelper.instance.queryByHabitId(String.valueOf(habitId));
            if (cursor != null) {
                int count = cursor.getCount();
                cursor.close();
                return count;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Menghapus habit beserta semua logs terkait
     */
    private void deleteHabitWithLogs() {
        if (habit == null || habit.getId() <= 0) {
            Toast.makeText(this, "Invalid habit data", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int logsDeleted = (int) HabitLogHelper.instance.deleteByHabitId(String.valueOf(habit.getId()));
            long habitDeleted = habitHelper.deleteById(String.valueOf(habit.getId()));

            if (habitDeleted > 0) {
                setResult(RESULT_DELETE);

                String message;
                if (logsDeleted > 0) {
                    message = "Habit and " + logsDeleted + " logs deleted successfully!";
                } else {
                    message = "Habit deleted successfully!";
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete habit", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error deleting habit: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
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
        if (habitHelper != null) {
            habitHelper.close();
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    etHabitStartDate.setText(dateStr);
                }, year, month, day);

        datePickerDialog.show();
    }
}