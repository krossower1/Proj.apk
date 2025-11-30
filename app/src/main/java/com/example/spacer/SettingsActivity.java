/**
 * This activity allows the user to configure the application's settings.
 * It also handles the bottom navigation menu to switch between different activities.
 */
package com.example.spacer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in onSaveInstanceState(Bundle).
     *     Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ======== BOTTOM MENU NAVIGATION ========
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ======== CUSTOM TOAST ========
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast,
                    findViewById(R.id.settingsOptions));
            TextView text = layout.findViewById(R.id.text_toast);

            if (id == R.id.nav_settings) {
                // User is already in the SettingsActivity, just show a toast.
                text.setText(getString(R.string.settings));
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                return true;

            } else if (id == R.id.nav_home) {
                // Navigate to MainActivity.
                text.setText(R.string.main_screen);
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                return true;

            } else if (id == R.id.nav_account) {
                // Navigate to AccountActivity.
                text.setText(R.string.account);
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(SettingsActivity.this, AccountActivity.class));
                return true;
            }

            return false;
        });
        // ======== END OF BOTTOM MENU ========
    }
}
