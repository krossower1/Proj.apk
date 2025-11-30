package com.example.spacer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerTheme;
    private SharedPreferences prefs;
    private String theme; // aktualny motyw

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ======== SharedPreferences ========
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Bezpieczne odczytanie theme
        Object stored = prefs.getAll().get("theme");
        if (stored instanceof String) {
            theme = (String) stored;
        } else {
            theme = "default"; // fallback
        }

        // Załaduj odpowiedni layout w zależności od motywu
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

// ======== Bottom Menu Navigation ========
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

                // === opóźnienie 100 ms ===
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                }, 100);

                return true;
            }

            if (id == R.id.nav_account) {
                text.setText(R.string.account);
                toast.show();

                // === opóźnienie 100 ms ===
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
                }, 100);

                return true;
            }

            return false;
        });

        // ======== Spinner motywu ========
        spinnerTheme = findViewById(R.id.spinnerTheme);

        String[] themes = {"Domyślny", "Jasny", "Ciemny"};

        // Adapter z dynamicznym kolorem tekstu i tłem rozwijanego menu
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                // kolor tekstu dla wyświetlanego Spinnera
                if (theme.equals("dark")) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                } else if (theme.equals("default")) {
                    tv.setTextColor(getResources().getColor(R.color.green_text));
                } else { // jasny
                    tv.setTextColor(getResources().getColor(R.color.black));
                }
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);

                // kolor tekstu i tło rozwijanego menu
                if (theme.equals("dark")) {
                    tv.setTextColor(getResources().getColor(R.color.white));
                    tv.setBackgroundColor(getResources().getColor(R.color.black));
                } else if (theme.equals("default")) {
                    tv.setTextColor(getResources().getColor(R.color.green_text));
                    tv.setBackgroundColor(getResources().getColor(R.color.green_background));
                } else { // jasny
                    tv.setTextColor(getResources().getColor(R.color.black));
                    // tło domyślne pozostaje bez zmian
                }
                return tv;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);

        // ustaw spinner na aktualny motyw
        switch (theme) {
            case "light":
                spinnerTheme.setSelection(1);
                break;
            case "dark":
                spinnerTheme.setSelection(2);
                break;
            default:
                spinnerTheme.setSelection(0);
                break;
        }

        // listener zmiany motywu
        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme;
                switch (position) {
                    case 1:
                        selectedTheme = "light";
                        break;
                    case 2:
                        selectedTheme = "dark";
                        break;
                    default:
                        selectedTheme = "default";
                        break;
                }

                prefs.edit().putString("theme", selectedTheme).apply();

                if (!selectedTheme.equals(theme)) {
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
