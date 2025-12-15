package com.example.spacer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.media.MediaPlayer;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * @brief Activity for user registration. Allows new users to create an account.
 *
 */
public class RegisterActivity extends AppCompatActivity {

    // UI elements
    /** UI element for entering login. */
    private EditText etLogin;
    /** UI element for entering password. */
    private EditText etPassword;
    /** UI element for repeating password. */
    private EditText etRepeatPassword;
    /** UI element for entering weight. */
    private EditText etWaga;
    /** For storing application settings. */
    private SharedPreferences prefs;


    /** Database helper for database operations. */
    private DatabaseHelper dbHelper;

    /**
     * @brief Called when the activity is first created. Initializes the UI, database helper and sets up event listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedLang = prefs.getString("appLanguage", "pl");
        setLocale(savedLang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button btnRegister, btnGoToLogin;

        // Initialize UI elements
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        etWaga = findViewById(R.id.etWaga);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finishAffinity());

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Close the application when the close button is clicked
        btnClose.setOnClickListener(v -> finishAffinity());

        // Handle the "Register" button click
        btnRegister.setOnClickListener(v -> {
            // Get user input from EditText fields
            String login = etLogin.getText().toString().trim();
            String pass = etPassword.getText().toString();
            String repeat = etRepeatPassword.getText().toString();
            String waga = etWaga.getText().toString().trim();

            // Check if any of the fields are empty
            if (login.isEmpty() || pass.isEmpty() || repeat.isEmpty() || waga.isEmpty()) {
                // Show a custom toast message if any field is empty
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.no_field));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ====== Play a short sound ======
                MediaPlayer mp = MediaPlayer.create(this, R.raw.alert2); // plik w res/raw/alert.mp3
                mp.setOnCompletionListener(MediaPlayer::release);      // zwalnia zasoby po odtworzeniu
                mp.start();
                return;
            }

            // Check if the passwords match
            if (!pass.equals(repeat)) {
                // Show a custom toast message if the passwords don't match
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.passwords_not_matching));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // ====== Play a short sound ======
                MediaPlayer mp = MediaPlayer.create(this, R.raw.alert2); // plik w res/raw/alert.mp3
                mp.setOnCompletionListener(MediaPlayer::release);      // zwalnia zasoby po odtworzeniu
                mp.start();
                return;
            }

            // Add user to the database
            boolean inserted = dbHelper.addUser(login, pass, waga);
            if (inserted) {
                // Show a custom toast message if registration is successful
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.registration_successful));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // ====== Play a short sound ======
                MediaPlayer mp = MediaPlayer.create(this, R.raw.alert); // plik w res/raw/alert.mp3
                mp.setOnCompletionListener(MediaPlayer::release);      // zwalnia zasoby po odtworzeniu
                mp.start();

                // Go to the login screen
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Show a custom toast message if the user already exists
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, null);
                layout.setBackgroundResource(R.drawable.toast_error_background);

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(getString(R.string.user_already_exists));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                // ====== Play a short sound ======
                MediaPlayer mp = MediaPlayer.create(this, R.raw.alert2); // plik w res/raw/alert.mp3
                mp.setOnCompletionListener(MediaPlayer::release);      // zwalnia zasoby po odtworzeniu
                mp.start();
            }

        });

        // Handle the "Go to Login" button click
        btnGoToLogin.setOnClickListener(v -> {
            // Go to the login screen
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }

    /**
     * @brief Sets the locale of the application.
     * @param langCode The language code to set.
     *
     */
    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
