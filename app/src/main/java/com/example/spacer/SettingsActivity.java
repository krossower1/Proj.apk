package com.example.spacer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ======== SharedPreferences ========
        // Initialize SharedPreferences for storing settings
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // ======== JĘZYK ========
        // Get saved language from SharedPreferences, default to Polish ('pl')
        String savedLang = prefs.getString("appLanguage", "pl");
        // Set the app locale based on the saved language
        setLocale(savedLang); // This must be called before setContentView()

        // ======== Motyw ========
        // Get the saved theme from SharedPreferences, default to "default"
        Object storedTheme = prefs.getAll().get("theme");
        theme = (storedTheme instanceof String) ? (String) storedTheme : "default";

        // Load the layout based on the selected theme
        switch (theme) {
            case "light":
                setContentView(R.layout.activity_settings_light);
                break;
            case "dark":
                setContentView(R.layout.activity_settings_dark);
                break;
            default:
                setContentView(R.layout.activity_settings);
                break;
        }

        super.onCreate(savedInstanceState);

        // ======== POWIADOMIENIA ========
        // Initialize the notification switch
        SwitchCompat switchNotifications = findViewById(R.id.switchNotifications);
        // Get the current notification setting, default to enabled
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);
        // Set the switch to the saved state
        switchNotifications.setChecked(notificationsEnabled);
        // Add a listener to save the setting when the switch is toggled
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notificationsEnabled", isChecked).apply();
            String message = isChecked ? getString(R.string.notifications_on) : getString(R.string.notifications_off);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // ======== SPINNER JĘZYKA ========
        // Initialize the language spinner
        Spinner spinnerLanguage = findViewById(R.id.spinnerLanguage);
        String[] languages = {"Polski", "English"};
        // Create an ArrayAdapter for the language spinner
        ArrayAdapter<String> adapterLang = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                // Set text color based on the current theme
                if (theme.equals("dark")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.white));
                } else if (theme.equals("default")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_text));
                } else {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                }
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                // Set text and background color for dropdown items based on the current theme
                if (theme.equals("dark")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.white));
                    tv.setBackgroundColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                } else if (theme.equals("default")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_text));
                    tv.setBackgroundColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_background));
                } else {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                }
                return tv;
            }
        };
        adapterLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapterLang);
        // Set the spinner selection to the saved language
        spinnerLanguage.setSelection(savedLang.equals("pl") ? 0 : 1);

        // Add a listener to handle language selection changes
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String langCode = (position == 0) ? "pl" : "en";
                String currentLang = prefs.getString("appLanguage", "pl");

                // If the language is changed, save the new language and restart the activity
                if (!langCode.equals(currentLang)) {
                    prefs.edit().putString("appLanguage", langCode).apply();
                    // Restart the activity to apply the new language
                    finishAffinity(); // powoduje zamknięcie całej aplikacji
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ======== SPINNER MOTYWU ========
        // Initialize the theme spinner
        Spinner spinnerTheme = findViewById(R.id.spinnerTheme);
        String[] themes = {"Domyślny", "Jasny", "Ciemny"};
        // Create an ArrayAdapter for the theme spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, themes) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                // Set text color based on the current theme
                if (theme.equals("dark")) tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.white));
                else if (theme.equals("default")) tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_text));
                else tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                // Set text and background color for dropdown items based on the current theme
                if (theme.equals("dark")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.white));
                    tv.setBackgroundColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                } else if (theme.equals("default")) {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_text));
                    tv.setBackgroundColor(ContextCompat.getColor(SettingsActivity.this, R.color.green_background));
                } else {
                    tv.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.black));
                }
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);

        // Set the spinner selection to the current theme
        switch (theme) {
            case "light": spinnerTheme.setSelection(1); break;
            case "dark": spinnerTheme.setSelection(2); break;
            default: spinnerTheme.setSelection(0); break;
        }

        // Add a listener to handle theme selection changes
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme;
                switch (position) {
                    case 1: selectedTheme = "light"; break;
                    case 2: selectedTheme = "dark"; break;
                    default: selectedTheme = "default"; break;
                }

                // If the theme is changed, save the new theme and restart the activity
                if (!selectedTheme.equals(theme)) {
                    prefs.edit().putString("theme", selectedTheme).apply();
                    // Restart the activity to apply the new theme
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ======== MENU DOLNE ========
        // Initialize the bottom navigation view
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        // Set a listener for bottom navigation item selections
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            LayoutInflater inflater = getLayoutInflater();
            // Inflate the custom toast layout
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            // Create and configure a custom toast message
            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                // Show a toast for the settings screen
                text.setText(getString(R.string.b_settings));
                toast.show();
                return true;
            }

            if (id == R.id.nav_home) {
                // Show a toast and navigate to the main screen
                text.setText(R.string.b_home_screen);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }, 100);
                return true;
            }

            if (id == R.id.nav_account) {
                // Show a toast and navigate to the account screen
                text.setText(R.string.b_my_account);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
                }, 100);
                return true;
            }
            return false;
        });
    }

    /**
     * Sets the application's locale.
     * @param langCode The language code to set (e.g., "en", "pl").
     */
    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}