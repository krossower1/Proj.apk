package com.example.spacer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

// ============ NOWA CZĘŚĆ — bottom menu ============
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ============ WSPÓLNY CUSTOM TOAST =============
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.settings));
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // pozostaje w SettingsActivity
                return true;

            } else if (id == R.id.nav_home) {
                text.setText("Ekran główny");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_account) {
                text.setText("Konto");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
                return true;
            }

            return false;
        });
// ============ KONIEC NOWEGO ============
    }
}
