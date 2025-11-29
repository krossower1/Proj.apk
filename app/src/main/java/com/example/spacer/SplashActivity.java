package com.example.spacer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2000; // 2 sekundy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Znajdź TextView
        TextView splashTitle = findViewById(R.id.splashTitle);

        // Wczytaj animację
        Animation splashAnim = AnimationUtils.loadAnimation(this, R.anim.splash_anim);
        splashTitle.startAnimation(splashAnim);

        // Po zakończeniu 2 sekund przejdź do LoginActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // usuwa SplashActivity z back stack
        }, SPLASH_DURATION);
    }
}
