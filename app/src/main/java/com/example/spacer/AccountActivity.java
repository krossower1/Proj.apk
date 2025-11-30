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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Handler;
import android.os.Looper;

public class AccountActivity extends AppCompatActivity {

    // UI elements
    private EditText etUsername, etPassword, etPhone, etAge, etWaga;
    private static final String PREFS_NAME = "UserPrefs";
    private boolean passwordVisible = false;

    // ======== NOWE: obsługa motywu ========
    private SharedPreferences prefs;
    private String theme; // aktualny motyw
    // ======== KONIEC NOWEGO ========

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ======== NOWE: SharedPreferences dla motywu ========
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Object stored = prefs.getAll().get("theme");
        if (stored instanceof String) {
            theme = (String) stored;
        } else {
            theme = "default";
        }

        // Wczytaj layout zależnie od motywu
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
        // ======== KONIEC NOWEGO ========


// ======================= Bottom menu initialization =======================
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView text = layout.findViewById(R.id.text_toast);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.settings));
                toast.show();

                Intent intent = new Intent(AccountActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;


            } else if (id == R.id.nav_home) {
                text.setText(getString(R.string.main_screen));
                toast.show();

                Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;

            } else if (id == R.id.nav_account) {
                text.setText(getString(R.string.account));
                toast.show();
                // Jesteśmy już w AccountActivity, nic nie robimy
                return true;
            }

            return false;
        });

        // ======================= END of bottom menu =======================

        // ======================= Field initialization =======================
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etWaga = findViewById(R.id.etWaga);
        Button btnSave = findViewById(R.id.btnSave);

        // ================================================================
        // WYBÓR IKON NA PODSTAWIE MOTYWU
        // ================================================================
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
        // ================================================================

        // ======================= Password visibility toggle =======================
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

        // ======================= "X" icons for other fields =======================
        setupClearIcon(etUsername, clearIcon);
        setupClearIcon(etPhone, clearIcon);
        setupClearIcon(etAge, clearIcon);
        setupClearIcon(etWaga, clearIcon);

        // Load saved data
        loadUserData();

        // Handle "Save changes" click
        btnSave.setOnClickListener(v -> saveUserData());
    }

    // ======== ZMIENIONA WERSJA — przyjmuje ikonę zależną od motywu ========
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
    // ========================================================================

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etUsername.setText(prefs.getString("username", ""));
        etPassword.setText(prefs.getString("password", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etAge.setText(prefs.getString("age", ""));
        etWaga.setText(prefs.getString("waga", ""));
    }

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
}