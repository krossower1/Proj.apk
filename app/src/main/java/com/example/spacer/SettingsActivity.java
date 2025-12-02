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
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerTheme, spinnerLanguage;
    private Switch switchNotifications;
    private SharedPreferences prefs;
    private String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ======== SharedPreferences ========
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // ======== JĘZYK ========
        String savedLang = prefs.getString("appLanguage", "pl");
        setLocale(savedLang); // musi być przed setContentView()

        // ======== Motyw ========
        Object storedTheme = prefs.getAll().get("theme");
        theme = (storedTheme instanceof String) ? (String) storedTheme : "default";

        // Załaduj layout zależnie od motywu
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
        switchNotifications = findViewById(R.id.switchNotifications);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);
        switchNotifications.setChecked(notificationsEnabled);
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notificationsEnabled", isChecked).apply();
            String message = isChecked ? getString(R.string.notifications_on) : getString(R.string.notifications_off);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // ======== SPINNER JĘZYKA ========
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        String[] languages = {"Polski", "English"};
        ArrayAdapter<String> adapterLang = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                if (theme.equals("dark")) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                } else if (theme.equals("default")) {
                    tv.setTextColor(getResources().getColor(R.color.green_text));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                if (theme.equals("dark")) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                    tv.setBackgroundColor(getResources().getColor(R.color.black));
                } else if (theme.equals("default")) {
                    tv.setTextColor(getResources().getColor(R.color.green_text));
                    tv.setBackgroundColor(getResources().getColor(R.color.green_background));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return tv;
            }
        };
        adapterLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapterLang);
        spinnerLanguage.setSelection(savedLang.equals("pl") ? 0 : 1);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String langCode = (position == 0) ? "pl" : "en";
                String currentLang = prefs.getString("appLanguage", "pl");

                if (!langCode.equals(currentLang)) {
                    prefs.edit().putString("appLanguage", langCode).apply();
                    // restart aktywności, aby nowy język się załadował
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ======== SPINNER MOTYWU ========
        spinnerTheme = findViewById(R.id.spinnerTheme);
        String[] themes = {"Domyślny", "Jasny", "Ciemny"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                if (theme.equals("dark")) tv.setTextColor(getResources().getColor(R.color.white));
                else if (theme.equals("default")) tv.setTextColor(getResources().getColor(R.color.green_text));
                else tv.setTextColor(getResources().getColor(R.color.black));
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                if (theme.equals("dark")) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                    tv.setBackgroundColor(getResources().getColor(R.color.black));
                } else if (theme.equals("default")) {
                    tv.setTextColor(getResources().getColor(R.color.green_text));
                    tv.setBackgroundColor(getResources().getColor(R.color.green_background));
                } else {
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);

        // ustaw spinner na aktualny motyw
        switch (theme) {
            case "light": spinnerTheme.setSelection(1); break;
            case "dark": spinnerTheme.setSelection(2); break;
            default: spinnerTheme.setSelection(0); break;
        }

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme;
                switch (position) {
                    case 1: selectedTheme = "light"; break;
                    case 2: selectedTheme = "dark"; break;
                    default: selectedTheme = "default"; break;
                }

                if (!selectedTheme.equals(theme)) {
                    prefs.edit().putString("theme", selectedTheme).apply();
                    // restart aktywności po zmianie motywu
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
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.settings));
                toast.show();
                return true;
            }

            if (id == R.id.nav_home) {
                text.setText(R.string.main_screen);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }, 100);
                return true;
            }

            if (id == R.id.nav_account) {
                text.setText(R.string.account);
                toast.show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
                }, 100);
                return true;
            }
            return false;
        });
    }

    // ======== Metoda ustawiająca język ========
    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
