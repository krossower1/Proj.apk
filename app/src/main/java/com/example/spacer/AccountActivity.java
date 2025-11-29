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

public class AccountActivity extends AppCompatActivity {

    // UI elements
    private EditText etUsername, etPassword, etPhone, etAge, etWaga;
    // Name for shared preferences
    private static final String PREFS_NAME = "UserPrefs";
    // Flag for password visibility
    private boolean passwordVisible = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // ======================= Bottom menu initialization =======================
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ============ COMMON CUSTOM TOAST =============
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
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
                text.setText(getString(R.string.main_screen));
                toast.show();

                startActivity(new Intent(AccountActivity.this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_account) {
                text.setText(getString(R.string.account));
                toast.show();

                // stay in AccountActivity, so we don't open a new one
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

        // ======================= Password visibility toggle =======================
        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight()
                        - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if (passwordVisible) {
                        // Hide password
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                        passwordVisible = false;
                    } else {
                        // Show password
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
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
        setupClearIcon(etUsername);
        setupClearIcon(etPhone);
        setupClearIcon(etAge);
        setupClearIcon(etWaga);

        // Load saved data
        loadUserData();

        // Handle "Save changes" click
        btnSave.setOnClickListener(v -> saveUserData());
    }

    /**
     * Sets up a clear icon for an EditText that clears the text on click.
     * @param editText The EditText to set up the clear icon for.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupClearIcon(EditText editText) {
        // Set the clear icon
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0);

        // Set a touch listener to clear the text
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
     * Loads user data from SharedPreferences and populates the EditText fields.
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
     * Saves user data to SharedPreferences from the EditText fields.
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
}
