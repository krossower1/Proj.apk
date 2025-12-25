/**
 * Activity for user account management.
 * Allows users to view and update their profile information.
 */
package com.example.spacer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

/**
 * @brief Activity for user account management. Allows users to view and update their profile information, as well as handle theme changes.
 *
 */
public class AccountActivity extends AppCompatActivity {

    // UI elements
    private EditText etUsername, etPassword, etPhone, etAge, etWaga;
    private static final String PREFS_NAME = "UserPrefs";
    private boolean passwordVisible = false;

    private SharedPreferences prefs;
    private String theme;

    /**
     * @brief Initializes the activity, sets the content view based on the current theme, and sets up UI elements and listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Object stored = prefs.getAll().get("theme");
        if (stored instanceof String) {
            theme = (String) stored;
        } else {
            theme = "default";
        }

        switch (theme) {
            case "light":
                setContentView(R.layout.activity_account_light);
                break;
            case "dark":
                setContentView(R.layout.activity_account_dark);
                break;
            default:
                setContentView(R.layout.activity_account);
                break;
        }


        // Bottom menu initialization
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        switch (theme) {
            case "light":
                bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.white_bottom_menu));
                break;
            case "dark":
                bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.black_bottom_menu));
                break;
            default:
                bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.green_bottom_menu));
                break;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView text = layout.findViewById(R.id.text_toast);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.b_settings));
                toast.show();

                Intent intent = new Intent(AccountActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;


            } else if (id == R.id.nav_home) {
                text.setText(getString(R.string.b_home_screen));
                toast.show();

                Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;

            } else if (id == R.id.nav_account) {
                text.setText(getString(R.string.b_my_account));
                toast.show();
                return true;
            }

            return false;
        });

        // Field initialization
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etWaga = findViewById(R.id.etWaga);
        Button btnSave = findViewById(R.id.btnSave);

        int eyeOffIcon, eyeOnIcon, clearIcon;

        if (theme.equals("dark")) {
            eyeOffIcon = R.drawable.ic_eye_off_white;
            eyeOnIcon  = R.drawable.ic_eye_white;
            clearIcon  = R.drawable.ic_clear_white;
        } else {
            eyeOffIcon = R.drawable.ic_eye_off;
            eyeOnIcon  = R.drawable.ic_eye;
            clearIcon  = R.drawable.ic_clear;
        }
        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, eyeOffIcon, 0);

        final int finalEyeOffIcon = eyeOffIcon;
        final int finalEyeOnIcon = eyeOnIcon;

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if (passwordVisible) {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, finalEyeOffIcon, 0);
                        passwordVisible = false;
                    } else {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, finalEyeOnIcon, 0);
                        passwordVisible = true;
                    }
                    etPassword.setSelection(etPassword.getText().length());
                    v.performClick();
                    return true;
                }
            }
            return false;
        });

        // "X" icons for other fields
        setupClearIcon(etUsername, clearIcon);
        setupClearIcon(etPhone, clearIcon);
        setupClearIcon(etAge, clearIcon);
        setupClearIcon(etWaga, clearIcon);

        // Load saved data
        loadUserData();

        // Handle "Save changes" click
        btnSave.setOnClickListener(v -> saveUserData());
    }

    /**
     * @brief Sets up a clear icon for an EditText that clears the text when clicked. The icon is theme-dependent.
     * @param editText The EditText to set up the clear icon for.
     * @param clearIcon The drawable resource ID for the clear icon.
     *
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupClearIcon(EditText editText, int clearIcon) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, clearIcon, 0);

        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    editText.setText("");
                    v.performClick();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * @brief Loads user data from SharedPreferences and populates the EditText fields.
     *
     */
    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etUsername.setText(prefs.getString("username", ""));
        etPassword.setText(prefs.getString("password", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etAge.setText(prefs.getString("age", ""));
        etWaga.setText(prefs.getString("waga", ""));
    }

    /**
     * @brief Saves user data from the EditText fields to SharedPreferences.
     *
     */
    private void saveUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", etUsername.getText().toString());
        editor.putString("password", etPassword.getText().toString());
        editor.putString("phone", etPhone.getText().toString());
        editor.putString("age", etAge.getText().toString());
        editor.putString("waga", etWaga.getText().toString());
        editor.apply();
        Toast.makeText(this, "Zmiany zapisane!", Toast.LENGTH_SHORT).show();
    }
    /**
     * @brief Called when the activity will start interacting with the user. Checks if the theme has changed and restarts the activity if it has.
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkThemeChange();
    }
    /**
     * @brief Sets the application's locale.
     * @param langCode The language code to set (e.g., "en", "pl").
     *
     */
    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    /**
     * @brief Checks if the theme has changed since the activity was last created. If the theme has changed, the activity is restarted to apply the new theme.
     *
     */
    private void checkThemeChange() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Object stored = prefs.getAll().get("theme");

        String savedTheme;
        if (stored instanceof String) {
            savedTheme = (String) stored;
        } else {
            savedTheme = "default";
        }

        if (!savedTheme.equals(theme)) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
}