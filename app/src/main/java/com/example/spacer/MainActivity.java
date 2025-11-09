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
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.view.LayoutInflater;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;



import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MapView map;
    private TextView date;
    private TextView predkosc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicjalizacja sensor managera
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Pobranie akcelerometru
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (accelerometer == null) {
            Toast.makeText(this, "Brak akcelerometru w tym urządzeniu!", Toast.LENGTH_LONG).show();
        }

        // Konfiguracja OSMDroid
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        // Create date and time formatters with locale
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        String currentDateString = dateFormat.format(new Date());

        date = findViewById(R.id.date);

        date.setText(getString(R.string.dzien) + " " + currentDateString);

        map = findViewById(R.id.map);
        map.setMultiTouchControls(true); // umożliwia pinch-zoom

        // Ustawienie punktu startowego
        GeoPoint startPoint = new GeoPoint(52, 21.0122); // Warszawa
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
        // Rejestracja nasłuchiwania sensora
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause(); // ważne dla OSMDroid
        // Wyrejestrowanie nasłuchiwania (oszczędzanie baterii)
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.alerty) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Czy chcesz otrzymywać alerty o niestabilnym chodzie?");
            builder.setTitle("Alert");
            builder.setCancelable(false);
            builder.setPositiveButton("Tak", (DialogInterface.OnClickListener) (dialog, which) -> {
                dialog.cancel();
            });
            builder.setNegativeButton("Nie", (DialogInterface.OnClickListener) (dialog, which) -> {
                dialog.cancel();
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;

            // Toast.makeText(this, "Menu Item is Pressed", Toast.LENGTH_SHORT).show();
        }

        // ------------------------------------------
        // Usuwanie danych użytkownika z SQLite | CUSTOM TOAST
        // ------------------------------------------
        if (id == R.id.udane) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.clearUsers();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView text = layout.findViewById(R.id.text_toast);
            text.setText("Wszystkie dane zostały usunięte!");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
            // ----------------------------

            return true;
        }

        if (id == R.id.wyloguj) {

            // ----------------------------
            // CUSTOM TOAST
            // ----------------------------
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView text = layout.findViewById(R.id.text_toast);
            text.setText("Wylogowano pomyślnie!");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
            // ----------------------------

            // Wylogowanie i przejście do ekranu logowania
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // kończymy MainActivity, by nie wrócić po cofnięciu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        predkosc = findViewById(R.id.textView4);

        float x = event.values[0];
        float y = event.values[1];
        // float z = event.values[2];

        predkosc.setText(getString(R.string.preskosc) + " " + Math.ceil(Math.abs(x) + Math.abs(y)) + " m");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }
}