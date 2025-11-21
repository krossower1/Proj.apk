package com.example.spacer;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.os.Looper;
import androidx.constraintlayout.widget.ConstraintLayout;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
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
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;
import android.text.Spannable;
import android.graphics.Color;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;


/**
 * The main activity of the application, responsible for displaying the map,
 * tracking user location, and monitoring movement via the accelerometer.
 * Also handles the login screen logic.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // --- Constants and Class Variables ---

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100; // Request code for location permission.
    private static final float GYRO_THRESHOLD = 2.0f; // Threshold for step detection
    private static final int STEP_DELAY_MS = 500; // Minimum delay between steps

    // UI Elements
    private MapView map;
    private TextView date;
    private TextView dystans;
    private TextView kroki;
    private TextView kalorie;
    private Marker marker; // The marker on the map for the user's location.
    private Button trackingButton;
    private ConstraintLayout mainLayout;

    // Sensor-related variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private float[] gravity = new float[3]; // Stores the gravity components for filtering.
    double dist = 0; // Accumulated distance based on movement.
    double kal = 0;
    double waga = 0;
    private int kro = 0;
    private long lastStepTime = 0;
    private boolean isTracking = false;

    // Location-related variables
    private FusedLocationProviderClient fusedLocationClient;
    private BottomNavigationView bottomNav;
    private DatabaseHelper dbHelper;

    /**
     * Called when the activity is first created. Initializes the UI, map, sensors,
     * and location services.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);

        // --- Location Services Setup ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- osmdroid Map Configuration ---
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);

        // --- Map Marker Setup ---
        marker = new Marker(map);
        marker.setTitle("Jesteś tutaj");
        marker.setEnabled(false); // Initially invisible until a location is found.
        map.getOverlays().add(marker);

        // --- Permissions and Location Initiation ---
        checkAndRequestLocationPermission();

        // --- Sensor Setup ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if (accelerometer == null) {
            Toast.makeText(this, "@string/noaccel", Toast.LENGTH_LONG).show();
        }
        if (gyroscope == null) {
            Toast.makeText(this, "Gyroscope not available", Toast.LENGTH_LONG).show();
        }

        // --- UI Initialization ---
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        String currentDateString = dateFormat.format(new Date());
        date = findViewById(R.id.date);
        kroki = findViewById(R.id.kroki);
        dystans = findViewById(R.id.dystans);
        kalorie = findViewById(R.id.kalorie);
        date.setText(getString(R.string.dzien) + " " + currentDateString);
        trackingButton = findViewById(R.id.button);
        mainLayout = findViewById(R.id.main);

        trackingButton.setOnClickListener(v -> {
            isTracking = !isTracking;
            if (isTracking) {
                trackingButton.setText(R.string.zakoncz);
                mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green_background_dark));
            } else {
                trackingButton.setText(R.string.rozpocznij);
                mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green_background));
            }
        });

        // Get waga from Intent
        Intent intent = getIntent();
        String wagaString = intent.getStringExtra("waga");
        if (wagaString != null && !wagaString.isEmpty()) {
            waga = Double.parseDouble(wagaString);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ============ WSPÓLNY CUSTOM TOAST =============
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView text = layout.findViewById(R.id.text_toast);

            if (id == R.id.nav_settings) {
                text.setText("Ustawienia");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            } else if (id == R.id.nav_home) {
                text.setText("Ekran główny");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                // pozostaje w MainActivity
                return true;

            } else if (id == R.id.nav_account) {
                text.setText("Konto");
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                return true;
            }

            return false;
        });
    }

    /**
     * Checks if location permission has been granted. If not, requests it.
     * If it is granted, it proceeds to start the location tracking process.
     */
    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationProcess();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on requestPermissions().
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, start the location process.
                startLocationProcess();
            } else {
                // Permission denied, show a toast.
                Toast.makeText(this, "@string/nolok", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Initiates the process of getting the device's location. It first tries to get the
     * last known location and then requests continuous updates.
     */
    private void startLocationProcess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return; // Safeguard check.
        }

        // Attempt to get the last known location for a quick initial fix.
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && !marker.isEnabled()) {
                            updateMapWithLocation(location);
                        }
                    }
                });
        // Start requesting continuous location updates.
        startLocationUpdates();
    }


    /**
     * Defines the parameters for location updates (priority and interval).
     */
    LocationRequest locationRequest = new LocationRequest.Builder(
            LocationRequest.PRIORITY_HIGH_ACCURACY, 5000) // High accuracy, 5-second interval.
            .build();

    /**
     * Callback object for receiving location updates.
     */
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;
            Location lastLocation = locationResult.getLastLocation();
            if (lastLocation != null) {
                updateMapWithLocation(lastLocation);
            }
        }
    };

    /**
     * Updates the map with a new location. It places the marker and centers the map.
     * Ignores invalid (0,0) coordinates.
     * param location The new location to display.
     */
    private void updateMapWithLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // Ignore invalid (0,0) locations which can be returned on initial fix.
        if (lat == 0.0 && lon == 0.0) {
            Log.d("GPS", "Ignoring invalid (0,0) location update.");
            return;
        }

        Log.d("GPS", "Lat: " + lat + ", Lon: " + lon);
        GeoPoint newPoint = new GeoPoint(lat, lon);
        marker.setPosition(newPoint);

        // If this is the first fix, enable the marker and jump to the location.
        if (!marker.isEnabled()) {
            marker.setEnabled(true);
            map.getController().setZoom(18.0);
            map.getController().setCenter(newPoint);
        } else {
            // For subsequent updates, smoothly animate to the new location.
            map.getController().animateTo(newPoint);
        }
        map.invalidate(); // Redraw the map.
    }

    /**
     * Called when the activity will start interacting with the user.
     * Resumes map rendering, sensor listening, and location updates.
     */
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        startLocationUpdates();
    }

    /**
     * Starts requesting location updates from the FusedLocationProviderClient.
     * This is only done if permission has been granted.
     */
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    /**
     * Called when the activity is no longer in the foreground.
     * Pauses map rendering, sensor listening, and location updates to save battery.
     */
    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Called when the activity is no longer visible to the user.
     * Ensures location updates are stopped.
     */
    @Override
    public void onStop() {
        super.onStop();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu,menu);

        // Znajdź element menu "Wyloguj"
        MenuItem logoutItem = menu.findItem(R.id.wyloguj);
        if (logoutItem != null) {
            // Tworzymy SpannableString aby pogrubić tekst
            SpannableString spanString = new SpannableString(logoutItem.getTitle());
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            logoutItem.setTitle(spanString);
        }


        //Ikona obok "Eksport danych"
        MenuItem item = menu.findItem(R.id.edane); // "Eksport danych"
        SpannableString s = new SpannableString("Eksport danych   ");
        Drawable d = ContextCompat.getDrawable(this, R.drawable.archive);
        if (d != null) d.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
        s.setSpan(span, s.length() - 1, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        item.setTitle(s);

        // Ikona obok "Usuń dane"
        MenuItem deleteItem = menu.findItem(R.id.udane);
        SpannableString ss = new SpannableString("Usuń dane   "); // dodany odstęp
        Drawable dd = ContextCompat.getDrawable(this, R.drawable.delete);
        if (dd != null) dd.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        dd.setBounds(0, 0, dd.getIntrinsicWidth(), dd.getIntrinsicHeight());
        ImageSpan spann = new ImageSpan(dd, ImageSpan.ALIGN_BOTTOM);
        ss.setSpan(spann, ss.length() - 1, ss.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        deleteItem.setTitle(ss);
        //============== //================

        // Ikona obok "Alerty o niestabilnym chodzie"
        MenuItem alertItem = menu.findItem(R.id.alerty);
        SpannableString alertText = new SpannableString("Alerty o niestabilnym chodzie   "); // dodany odstęp
        Drawable alertIcon = ContextCompat.getDrawable(this, R.drawable.niest);
        if (alertIcon != null) alertIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        alertIcon.setBounds(0, 0, alertIcon.getIntrinsicWidth(), alertIcon.getIntrinsicHeight());
        ImageSpan alertSpan = new ImageSpan(alertIcon, ImageSpan.ALIGN_BOTTOM);
        alertText.setSpan(alertSpan, alertText.length() - 1, alertText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        alertItem.setTitle(alertText);

        // Ikona obok "Cotygodniowe raporty"
        MenuItem raportItem = menu.findItem(R.id.raporty);
        SpannableString raportText = new SpannableString("Cotygodniowe raporty   "); // dodany odstęp
        Drawable raportIcon = ContextCompat.getDrawable(this, R.drawable.raporty);
        if (raportIcon != null) raportIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        if (raportIcon != null) raportIcon.setBounds(0, 0, raportIcon.getIntrinsicWidth(), raportIcon.getIntrinsicHeight());
        ImageSpan raportSpan = new ImageSpan(raportIcon, ImageSpan.ALIGN_BOTTOM);
        raportText.setSpan(raportSpan, raportText.length() - 1, raportText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        raportItem.setTitle(raportText);
        //============== //================

        // Ikona obok "Pokazuj znaczniki na mapie"
        MenuItem znacznikiItem = menu.findItem(R.id.pznaczniki);
        SpannableString znacznikiText = new SpannableString("Pokazuj znaczniki na mapie   "); // dodany odstęp
        Drawable znacznikiIcon = ContextCompat.getDrawable(this, R.drawable.znaczniki);
        if (znacznikiIcon != null) znacznikiIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        if (znacznikiIcon != null) znacznikiIcon.setBounds(0, 0, znacznikiIcon.getIntrinsicWidth(), znacznikiIcon.getIntrinsicHeight());
        ImageSpan znacznikiSpan = new ImageSpan(znacznikiIcon, ImageSpan.ALIGN_BOTTOM);
        znacznikiText.setSpan(znacznikiSpan, znacznikiText.length() - 1, znacznikiText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        znacznikiItem.setTitle(znacznikiText);
        //============== //================

        // Ikona obok "Usuń znacznik"
        MenuItem usunZnacznikItem = menu.findItem(R.id.uznacznik);
        SpannableString usunZnacznikText = new SpannableString("Usuń znacznik   "); // dodany odstęp
        Drawable usunZnacznikIcon = ContextCompat.getDrawable(this, R.drawable.delete);
        if (usunZnacznikIcon != null) usunZnacznikIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        if (usunZnacznikIcon != null) usunZnacznikIcon.setBounds(0, 0, usunZnacznikIcon.getIntrinsicWidth(), usunZnacznikIcon.getIntrinsicHeight());
        ImageSpan usunZnacznikSpan = new ImageSpan(usunZnacznikIcon, ImageSpan.ALIGN_BOTTOM);
        usunZnacznikText.setSpan(usunZnacznikSpan, usunZnacznikText.length() - 1, usunZnacznikText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        usunZnacznikItem.setTitle(usunZnacznikText);
        //============== //================

        // Ikona obok "Porównaj ten dzień"
        MenuItem compareItem = menu.findItem(R.id.pdzien);
        SpannableString compareText = new SpannableString("Porównaj ten dzień   "); // dodany odstęp
        Drawable compareIcon = ContextCompat.getDrawable(this, R.drawable.compare);
        if (compareIcon != null) compareIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
        if (compareIcon != null) compareIcon.setBounds(0, 0, compareIcon.getIntrinsicWidth(), compareIcon.getIntrinsicHeight());
        ImageSpan compareSpan = new ImageSpan(compareIcon, ImageSpan.ALIGN_BOTTOM);
        compareText.setSpan(compareSpan, compareText.length() - 1, compareText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        compareItem.setTitle(compareText);
        //============== //================

        MenuItem alertItemm = menu.findItem(R.id.alerty); // np. "Alerty o niestabilnym chodzie"
        if (alertItemm != null) {
            SpannableString sss = new SpannableString(alertItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            alertItemm.setTitle(sss);
        }

        // COTYGODNIOWE RAPORTY
        MenuItem raportItemm = menu.findItem(R.id.raporty);
        if (raportItemm != null) {
            SpannableString sss = new SpannableString(raportItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            raportItemm.setTitle(sss);
        }

        // POKAZUJ ZNACZNIKI NA MAPIE
        MenuItem znacznikiItemm = menu.findItem(R.id.pznaczniki);
        if (znacznikiItemm != null) {
            SpannableString sss = new SpannableString(znacznikiItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            znacznikiItemm.setTitle(sss);
        }

        // USUŃ ZNACZNIK
        MenuItem usunZnacznikItemm = menu.findItem(R.id.uznacznik);
        if (usunZnacznikItemm != null) {
            SpannableString sss = new SpannableString(usunZnacznikItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            usunZnacznikItemm.setTitle(sss);
        }

        // PORÓWNAJ TEN DZIEŃ
        MenuItem pdzienItemm = menu.findItem(R.id.pdzien);
        if (pdzienItemm != null) {
            SpannableString sss = new SpannableString(pdzienItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            pdzienItemm.setTitle(sss);
        }

        // EKSPORT DANYCH
        MenuItem edaneItemm = menu.findItem(R.id.edane);
        if (edaneItemm != null) {
            SpannableString sss = new SpannableString(edaneItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            edaneItemm.setTitle(sss);
        }

        // USUŃ DANE
        MenuItem udaneItemm = menu.findItem(R.id.udane);
        if (udaneItemm != null) {
            SpannableString sss = new SpannableString(udaneItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            udaneItemm.setTitle(sss);
        }

        // WYLOGUJ
        MenuItem wylogujItemm = menu.findItem(R.id.wyloguj);
        if (wylogujItemm != null) {
            SpannableString sss = new SpannableString(wylogujItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            wylogujItemm.setTitle(sss);
        }


        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.alerty) {
            // Show alert dialog for unstable walking.
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Czy chcesz wyłączyć alerty o niestabilnym chodzie?");
            builder.setTitle("Alert");
            builder.setCancelable(false);
            builder.setPositiveButton("tak", (DialogInterface.OnClickListener) (dialog, which) -> {
                dialog.cancel();
            });
            builder.setNegativeButton("nie", (DialogInterface.OnClickListener) (dialog, which) -> {
                dialog.cancel();
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }



        if (id == R.id.udane) {
            // Clear user data and show a confirmation toast.
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.clearUsers();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView text = layout.findViewById(R.id.text_toast);
            text.setText("Wyczyszczono dane użytkowników.");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();

            return true;
        }

        if (id == R.id.wyloguj) {
            // Log out the user and return to the LoginActivity.
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, null);

            TextView text = layout.findViewById(R.id.text_toast);
            text.setText("Wylogowano pomyślnie.");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when there is a new sensor event.
     * This method filters gravity from the accelerometer and calculates movement.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isTracking) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // High-pass filter to isolate linear acceleration from gravity.
                final float alpha = 0.8f;

                // Isolate gravity contribution with a low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove gravity contribution to get linear acceleration.
                float x = event.values[0] - gravity[0];
                float y = event.values[1] - gravity[1];
                float z = event.values[2] - gravity[2];

                // Calculate the magnitude of the acceleration vector.
                double magnitude = Math.sqrt(x * x + y * y + z * z);

                // A threshold to filter out sensor noise when the device is mostly stationary.
                final double MOVEMENT_THRESHOLD = 0.2;

                if (magnitude > MOVEMENT_THRESHOLD) {
                    // Accumulate distance based on movement magnitude.
                    dist = dist + Math.floor(magnitude);
                }
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastStepTime) > STEP_DELAY_MS) {
                    float omegaMagnitude = (float) Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
                    if (omegaMagnitude > GYRO_THRESHOLD) {
                        lastStepTime = currentTime;
                        kro++;
                    }
                }
            }

            kal = Math.floor(dist) / 50 * (3.5 / waga);

            dystans.setText(getString(R.string.dystans) + " " + Math.floor(dist) / 50 + " m");

            kroki.setText(getString(R.string.kroki) + " " + kro);

            kalorie.setText(getString(R.string.kalorie) + " " + (int) kal + " kcal");
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this application, but required to be implemented.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        int userId = dbHelper.getLastUserId();
        if (userId != -1) {
            dbHelper.addTrainingData(Math.floor(dist)/50, kro, kal, userId);
        }
    }
}
