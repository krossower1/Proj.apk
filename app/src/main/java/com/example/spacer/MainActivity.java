package com.example.spacer;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.os.Looper;
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

/**
 * The main activity of the application, responsible for displaying the map,
 * tracking user location, and monitoring movement via the accelerometer.
 * Also handles the login screen logic.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // --- Constants and Class Variables ---

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100; // Request code for location permission.

    // UI Elements
    private MapView map;
    private TextView date;
    private TextView dystans;
    // private TextView kroki;
    // private TextView kalorie;
    private Marker marker; // The marker on the map for the user's location.

    // Sensor-related variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] gravity = new float[3]; // Stores the gravity components for filtering.
    double dist = 0; // Accumulated distance based on movement.

    // Location-related variables
    private FusedLocationProviderClient fusedLocationClient;

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
        marker.setTitle("@string/start");
        marker.setEnabled(false); // Initially invisible until a location is found.
        map.getOverlays().add(marker);

        // --- Permissions and Location Initiation ---
        checkAndRequestLocationPermission();

        // --- Sensor Setup ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (accelerometer == null) {
            Toast.makeText(this, "@string/noaccel", Toast.LENGTH_LONG).show();
        }

        // --- UI Initialization ---
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        String currentDateString = dateFormat.format(new Date());
        date = findViewById(R.id.date);
        // kroki = findViewById(R.id.kroki);
        dystans = findViewById(R.id.dystans);
        date.setText(getString(R.string.dzien) + " " + currentDateString);
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
            builder.setMessage("@string/niestabil");
            builder.setTitle("Alert");
            builder.setCancelable(false);
            builder.setPositiveButton("@string/t", (DialogInterface.OnClickListener) (dialog, which) -> {
                dialog.cancel();
            });
            builder.setNegativeButton("@string/n", (DialogInterface.OnClickListener) (dialog, which) -> {
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
            text.setText("@string/cleardb");

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
            text.setText("@string/logout");

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

        if (dystans != null) {
            dystans.setText(getString(R.string.dystans) + " " + (int)dist + " m");
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this application, but required to be implemented.
    }
}
