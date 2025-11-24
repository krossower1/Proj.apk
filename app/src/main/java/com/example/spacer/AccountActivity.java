package com.example.spacer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.text.InputType;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountActivity extends AppCompatActivity {
    private EditText etUsername, etPassword, etPhone, etAge;
    private ImageView imgAvatar;
    private Button btnSave;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_AGE = "age";
    private int avatarRes;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false; // stan widoczności hasła
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAge = findViewById(R.id.etAge);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnSave = findViewById(R.id.btnSave);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Wczytaj zapisane dane
        etUsername.setText(sharedPreferences.getString(KEY_USERNAME, ""));
        etPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
        etPhone.setText(sharedPreferences.getString(KEY_PHONE, ""));
        etAge.setText(sharedPreferences.getString(KEY_AGE, ""));
        avatarRes = sharedPreferences.getInt(KEY_AVATAR, R.drawable.avatar);
        imgAvatar.setImageResource(avatarRes);

        // Kliknięcie w avatar - początek
        imgAvatar.setOnClickListener(v -> { // <-- NOWE ZMIANY
            if (avatarRes == R.drawable.avatar) {
                avatarRes = R.drawable.avatar2;
            } else if (avatarRes == R.drawable.avatar2) {
                avatarRes = R.drawable.avatar3;
            } else if (avatarRes == R.drawable.avatar3) {
                avatarRes = R.drawable.avatar4;
            } else if (avatarRes == R.drawable.avatar4) {
                avatarRes = R.drawable.avatar5;
            } else if (avatarRes == R.drawable.avatar5) {
                avatarRes = R.drawable.avatar6;
            } else if (avatarRes == R.drawable.avatar6) {
                avatarRes = R.drawable.avatar7;
            } else {
                avatarRes = R.drawable.avatar; // powrót do pierwszego
            }
            imgAvatar.setImageResource(avatarRes);
        });
        //Kliknięcie w avatar - koniec

        // Pokaz/ukryj hasło
        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Obsługa zapisu zmian
        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_USERNAME, etUsername.getText().toString());
            editor.putString(KEY_PASSWORD, etPassword.getText().toString());
            editor.putString(KEY_PHONE, etPhone.getText().toString());
            editor.putInt(KEY_AVATAR, avatarRes);
            editor.putString(KEY_AGE, etAge.getText().toString());
            editor.apply();

            // CUSTOM TOAST
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);
            text.setText("Dane zapisane pomyślnie!");
            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        });



        // ============ bottom menu ============
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            if (id == R.id.nav_settings) {
                text.setText("Ustawienia");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new android.content.Intent(AccountActivity.this, SettingsActivity.class));

                return true;

            } else if (id == R.id.nav_home) {
                text.setText("Ekran główny");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new android.content.Intent(AccountActivity.this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_account) {
                text.setText("Konto");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // pozostaje w AccountActivity
                return true;
            }

            return false;
        });
        // ============ koniec bottom menu ============

    }
}
