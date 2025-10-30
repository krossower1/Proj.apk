package com.example.spacer;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private TextView date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Konfiguracja OSMDroid
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        date = findViewById(R.id.date);

        // Create date and time formatters with locale
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        String currentDateString = dateFormat.format(new Date());

        date.setText(getString(R.string.dzien) + " " + currentDateString);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true); // umożliwia pinch-zoom

        // Ustawienie punktu startowego
        GeoPoint startPoint = new GeoPoint(52.2297, 21.0122); // Warszawa
        map.getController().setZoom(12.0);
        map.getController().setCenter(startPoint);

        // Dodanie markera
        Marker marker = new Marker(map);
        marker.setPosition(startPoint);
        marker.setTitle("Warszawa");
        map.getOverlays().add(marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume(); // ważne dla OSMDroid
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause(); // ważne dla OSMDroid
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu,menu);
        return true;
    }

}