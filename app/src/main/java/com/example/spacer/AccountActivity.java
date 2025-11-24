package com.example.spacer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etPhone, etAge, etWaga;
    private Button btnSave;
    private static final String PREFS_NAME = "UserPrefs";
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // ======================= Bottom menu =======================
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ============ WSPÓLNY CUSTOM TOAST =============
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.settings));
                toast.show();

                startActivity(new Intent(AccountActivity.this, SettingsActivity.class));
                return true;

            } else if (id == R.id.nav_home) {
                text.setText("Ekran główny");
                toast.show();

                startActivity(new Intent(AccountActivity.this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_account) {
                text.setText("Konto");
                toast.show();

                // pozostaje w AccountActivity, więc nie otwieramy nowej
                return true;
            }

            return false;
        });
        // ======================= KONIEC bottom menu =======================

        // ======================= Inicjalizacja pól =======================
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        etWaga = findViewById(R.id.etWaga);
        btnSave = findViewById(R.id.btnSave);

        // ======================= Toggle widoczności hasła =======================
        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight()
                        - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if (passwordVisible) {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                        passwordVisible = false;
                    } else {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                        passwordVisible = true;
                    }
                    etPassword.setSelection(etPassword.getText().length());
                    return true;
                }
            }
            return false;
        });

        // ======================= Ikony „X” dla pozostałych pól =======================
        setupClearIcon(etUsername);
        setupClearIcon(etPhone);
        setupClearIcon(etAge);
        setupClearIcon(etWaga);

        // Wczytaj zapisane dane
        loadUserData();

        // Obsługa kliknięcia "Zapisz zmiany"
        btnSave.setOnClickListener(v -> saveUserData());
    }

    // Metoda dodająca ikonę „X” i czyszczącą pole
    private void setupClearIcon(EditText editText) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0);

        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                    editText.setText("");
                    return true;
                }
            }
            return false;
        });
    }

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