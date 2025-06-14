package com.example.habbittracker.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.example.habbittracker.Activities.MainActivity;
import com.example.habbittracker.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class LoadingActivity extends AppCompatActivity {

    private static final int LOADING_DURATION = 3000;

    private ImageView ivAppLogo;
    private TextView tvAppName;
    private TextView tvLoadingText;
    private CircularProgressIndicator progressIndicator;

    private String[] loadingTexts = {
            "Loading your habits...",
            "Preparing your journey...",
            "Setting up your goals...",
            "Almost ready..."
    };
    private int currentTextIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_loading);

        initViews();
        startAnimations();
        startLoadingTextAnimation();

        simulateLoadingProcess();
    }

    private void initViews() {
        ivAppLogo = findViewById(R.id.ivAppLogo);
        tvAppName = findViewById(R.id.tvAppName);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void startAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivAppLogo.startAnimation(fadeIn);

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        tvAppName.startAnimation(slideUp);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            Animation fadeInProgress = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            progressIndicator.startAnimation(fadeInProgress);
            tvLoadingText.startAnimation(fadeInProgress);
        }, 500);
    }

    private void startLoadingTextAnimation() {
        Handler textHandler = new Handler(Looper.getMainLooper());
        Runnable textChanger = new Runnable() {
            @Override
            public void run() {
                if (currentTextIndex < loadingTexts.length) {
                    tvLoadingText.setText(loadingTexts[currentTextIndex]);
                    currentTextIndex++;
                    textHandler.postDelayed(this, 700);
                }
            }
        };
        textHandler.postDelayed(textChanger, 800);
    }

    private void simulateLoadingProcess() {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {
            initializeApp();

            navigateToMainActivity();
        }, LOADING_DURATION);
    }

    private void initializeApp() {
        // TODO: Tambahkan inisialisasi yang diperlukan
        // Contoh:
        // - Inisialisasi database
        // - Load preferences
        // - Setup analytics
        // - Check updates

        // Contoh inisialisasi database
        try {
            // HabitHelper.instance.open(this);
            // Atau inisialisasi lain yang diperlukan
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}