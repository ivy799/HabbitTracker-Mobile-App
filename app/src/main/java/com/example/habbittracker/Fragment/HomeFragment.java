package com.example.habbittracker.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habbittracker.Activities.HabitFormActivity;
import com.example.habbittracker.Adapters.HabitAdapter;
import com.example.habbittracker.Api_config.ApiService;
import com.example.habbittracker.Api_config.RetrofitClient;
import com.example.habbittracker.Database_config.DatabaseHelper;
import com.example.habbittracker.Database_config.Habit.HabitHelper;
import com.example.habbittracker.Database_config.Habit.HabitMappingHelper;
import com.example.habbittracker.Database_config.HabitLogs.HabitLogHelper;
import com.example.habbittracker.Models.Habit;
import com.example.habbittracker.Models.Quotes;
import com.example.habbittracker.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_UPDATE = 200;
    private RecyclerView rvHabit;
    private FloatingActionButton fabAdd;
    private TextView tvQuote, tvAuthor;
    private ImageView btnRefreshQuote;
    private ProgressBar progressBarQuote;
    private HabitAdapter adapter;
    private HabitHelper habitHelper;
    private LinearLayout emptyStateLayout;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "QuotePrefs";
    private static final String KEY_LAST_QUOTE_TEXT = "last_quote_text";
    private static final String KEY_LAST_QUOTE_AUTHOR = "last_quote_author";
    private static final String KEY_LAST_FETCH_TIME = "last_fetch_time";

    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        HabitLogHelper.getInstance(dbHelper).open();

        habitHelper = HabitHelper.getInstance(requireContext());
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        habitHelper.open();
        loadData();

        if (shouldFetchNewQuote()) {
            fetchRandomQuote();
        } else {
            loadCachedQuote();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (habitHelper != null) {
            habitHelper.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadCachedQuote();
        loadData();
    }

    private void initViews(View view) {
        rvHabit = view.findViewById(R.id.rv_habits);
        fabAdd = view.findViewById(R.id.fab_add);
        tvQuote = view.findViewById(R.id.tv_quote);
        tvAuthor = view.findViewById(R.id.tv_quote_author);
        btnRefreshQuote = view.findViewById(R.id.btnRefreshQuote);
        progressBarQuote = view.findViewById(R.id.progressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
    }

    private void setupRecyclerView() {
        rvHabit.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HabitAdapter(requireActivity());
        rvHabit.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), HabitFormActivity.class);
            startActivityForResult(intent, REQUEST_ADD);
        });

        btnRefreshQuote.setOnClickListener(v -> {
            v.animate().rotation(360f).setDuration(500).start();
            fetchRandomQuote();
        });
    }

    private void loadData() {
        new LoadHabitsAsync(requireContext(), habits -> {
            if (isAdded() && getActivity() != null) {
                if (habits != null && !habits.isEmpty()) {
                    adapter.setListHabits(habits);
                    rvHabit.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                } else {
                    adapter.setListHabits(new ArrayList<>());
                    rvHabit.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                }
            }
        }).execute();
    }

    public void refreshHabits() {
        if (isAdded() && getActivity() != null) {
            loadData();
        }
    }

    private void loadCachedQuote() {
        String cachedText = sharedPreferences.getString(KEY_LAST_QUOTE_TEXT, null);
        String cachedAuthor = sharedPreferences.getString(KEY_LAST_QUOTE_AUTHOR, null);

        if (cachedText != null && cachedAuthor != null) {
            displayQuote(cachedText, cachedAuthor);
        } else {
            showDefaultQuote();
        }
    }
    private boolean shouldFetchNewQuote() {
        long lastFetchTime = sharedPreferences.getLong(KEY_LAST_FETCH_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastFetchTime;
        return timeDifference > (4 * 60 * 60 * 1000);
    }
    private void fetchRandomQuote() {
        if (!isNetworkAvailable()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            }
            loadCachedQuote();
            return;
        }

        showQuoteLoading(true);

        ApiService apiService = RetrofitClient.getQuoteApi();
        Call<List<Quotes>> call = apiService.getRandomQuote();

        call.enqueue(new Callback<List<Quotes>>() {
            @Override
            public void onResponse(Call<List<Quotes>> call, Response<List<Quotes>> response) {
                if (isAdded()) {
                    showQuoteLoading(false);

                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        Quotes quote = response.body().get(0);
                        String quoteText = quote.getQ();
                        String author = quote.getA();

                        displayQuote(quoteText, author);
                        saveQuoteToCache(quoteText, author);
                    } else {
                        Toast.makeText(requireContext(), "Failed to load quote", Toast.LENGTH_SHORT).show();
                        loadCachedQuote();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Quotes>> call, Throwable t) {
                if (isAdded()) {
                    showQuoteLoading(false);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    loadCachedQuote();
                }
            }
        });
    }
    private void displayQuote(String quoteText, String author) {
        if (tvQuote != null && tvAuthor != null && isAdded()) {
            tvQuote.setText("\"" + quoteText + "\"");
            tvAuthor.setText("— " + author);
        }
    }
    private void saveQuoteToCache(String quoteText, String author) {
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_LAST_QUOTE_TEXT, quoteText);
            editor.putString(KEY_LAST_QUOTE_AUTHOR, author);
            editor.putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis());
            editor.apply();
        }
    }
    private void showDefaultQuote() {
        displayQuote("The secret of getting ahead is getting started.", "Mark Twain");
    }
    private void showQuoteLoading(boolean show) {
        if (progressBarQuote != null && isAdded()) {
            progressBarQuote.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD) {
            if (resultCode == HabitFormActivity.RESULT_ADD) {
                loadData();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Habit added successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_UPDATE) {
            if (resultCode == HabitFormActivity.RESULT_UPDATE) {
                loadData();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Habit updated successfully!", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == HabitFormActivity.RESULT_DELETE) {
                loadData();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Habit deleted successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (habitHelper != null) {
            habitHelper.close();
        }
    }
    private static class LoadHabitsAsync {
        private final WeakReference<Context> weakContext;
        private final WeakReference<LoadHabitsCallback> weakCallback;

        private LoadHabitsAsync(Context context, LoadHabitsCallback callback) {
            weakContext = new WeakReference<>(context);
            weakCallback = new WeakReference<>(callback);
        }

        void execute() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                Context context = weakContext.get();
                ArrayList<Habit> habits = new ArrayList<>();

                if (context != null) {
                    try {
                        HabitHelper habitHelper = HabitHelper.getInstance(context);
                        habitHelper.open();

                        Cursor habitCursor = habitHelper.queryAll();
                        if (habitCursor != null) {
                            try {
                                habits = HabitMappingHelper.mapCursorToArrayList(habitCursor);
                            } catch (ParseException e) {
                                e.printStackTrace();
                                habits = new ArrayList<>();
                            } finally {
                                habitCursor.close();
                            }
                        }

                        habitHelper.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        habits = new ArrayList<>();
                    }
                }

                ArrayList<Habit> finalHabits = habits;
                handler.post(() -> {
                    LoadHabitsCallback callback = weakCallback.get();
                    if (callback != null) {
                        callback.postExecute(finalHabits);
                    }
                });
            });
        }

        interface LoadHabitsCallback {
            void postExecute(ArrayList<Habit> habits);
        }
    }
}
