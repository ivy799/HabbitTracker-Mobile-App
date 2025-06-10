package com.example.habbittracker.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.habbittracker.Activities.MainActivity;
import com.example.habbittracker.R;
import com.example.habbittracker.ThemeManager;

public class GeneralFragment extends Fragment {

    private RadioGroup radioGroupTheme;
    private RadioButton radioSystem, radioLight, radioDark;
    private TextView textCurrentTheme;
    private Button btnApplyTheme;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String THEME_KEY = "selected_theme";

    // Theme constants
    private static final int THEME_SYSTEM = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;

    public GeneralFragment() {
        // Required empty public constructor
    }

    public static GeneralFragment newInstance() {
        return new GeneralFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general, container, false);

        initViews(view);
        setupListeners();
        loadSavedTheme();

        return view;
    }

    private void initViews(View view) {
        radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        radioSystem = view.findViewById(R.id.radioSystem);
        radioLight = view.findViewById(R.id.radioLight);
        radioDark = view.findViewById(R.id.radioDark);
        textCurrentTheme = view.findViewById(R.id.textCurrentTheme);
        btnApplyTheme = view.findViewById(R.id.btnApplyTheme);
    }

    private void setupListeners() {
        btnApplyTheme.setOnClickListener(v -> applySelectedTheme());

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            updateThemePreview();
        });
    }

    private void loadSavedTheme() {
        int savedTheme = ThemeManager.getSavedTheme(requireContext());

        switch (savedTheme) {
            case THEME_SYSTEM:
                radioSystem.setChecked(true);
                break;
            case THEME_LIGHT:
                radioLight.setChecked(true);
                break;
            case THEME_DARK:
                radioDark.setChecked(true);
                break;
        }

        updateThemePreview();
    }

    private void updateThemePreview() {
        String currentTheme = "System Default";

        if (radioLight.isChecked()) {
            currentTheme = "Light Theme";
        } else if (radioDark.isChecked()) {
            currentTheme = "Dark Theme";
        }

        textCurrentTheme.setText("Current: " + currentTheme);
    }

    private void applySelectedTheme() {
        int selectedTheme = THEME_SYSTEM;

        if (radioLight.isChecked()) {
            selectedTheme = THEME_LIGHT;
        } else if (radioDark.isChecked()) {
            selectedTheme = THEME_DARK;
        }

        // Save theme preference menggunakan ThemeManager
        ThemeManager.saveTheme(requireContext(), selectedTheme);

        // Apply theme
        ThemeManager.applyTheme(requireContext());

        Toast.makeText(getContext(), "Theme applied! Restarting app...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}