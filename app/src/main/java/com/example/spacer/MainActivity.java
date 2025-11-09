package com.example.spacer;

import android.util.Log;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import android.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private TextView date;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        double lat = 0;
                        double lon = 0;
                        if (location != null) {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            Log.d("GPS", "Lat: " + lat + ", Lon: " + lon);
                        }
                        // Ustawienie punktu startowego
                        GeoPoint startPoint = new GeoPoint(lat, lon); // Warszawa
                        map.getController().setZoom(12.0);
                        map.getController().setCenter(startPoint);

                        // Dodanie markera
                        Marker marker = new Marker(map);
                        marker.setPosition(startPoint);
                        marker.setTitle("START");
                        map.getOverlays().add(marker);
                    }
                });


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
    }

    LocationRequest locationRequest = new LocationRequest.Builder(
            LocationRequest.PRIORITY_HIGH_ACCURACY, 5000) // co 5 sekund
            .build();

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;
            for (Location location : locationResult.getLocations()) {
                Log.d("GPS", "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        fusedLocationClient.removeLocationUpdates(locationCallback);
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

        // Włącz aktualizacje:
        // fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        return super.onOptionsItemSelected(item);
    }
}