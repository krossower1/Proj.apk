package com.example.spacer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;


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
    private static final double SUDDEN_MOVEMENT_THRESHOLD = 25.0; // Threshold for sudden movement detection
    private static final long SUDDEN_MOVEMENT_COOLDOWN_MS = 5000; // Cooldown for sudden movement snackbar
    private long lastSuddenMovementTime = 0;
    private boolean suddenMovementAlertsEnabled = true;
    private boolean weeklyReportEnabled = true;
    private boolean incidentMarkersVisible = true;

    // UI Elements
    private MapView map;
    private TextView dystans;
    private TextView kroki;
    private TextView kalorie;
    private TextView date;
    private Marker userMarker; // The marker on the map for the user's location.
    private final List<Marker> incidentMarkers = new ArrayList<>();
    private Button trackingButton;
    private ConstraintLayout mainLayout;

    // Sensor-related variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private final float[] gravity = new float[3]; // Stores the gravity components for filtering.
    double dist = 0; // Accumulated distance based on movement.
    double kal = 0;
    double waga = 0;
    private int kro = 0;
    private long lastStepTime = 0;
    private boolean isTracking = false;

    // Location-related variables
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper dbHelper;
    private int lastDay = -1;
    private int userId = -1;
    private Polyline userPath;
    private final List<GeoPoint> pathPoints = new ArrayList<>();

    // Swipe-related variables
    private GestureDetector gestureDetector;
    private int dayOffset = 0;


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
        checkDay();

        if (savedInstanceState != null) {
            userId = savedInstanceState.getInt("userId", -1);
            waga = savedInstanceState.getDouble("waga", 0);
        } else {
            // Get userId and waga from Intent
            Intent intent = getIntent();
            userId = intent.getIntExtra("userId", -1);
            String wagaString = intent.getStringExtra("waga");
            if (wagaString != null && !wagaString.isEmpty()) {
                waga = Double.parseDouble(wagaString);
            }
        }

        // --- Location Services Setup ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- osmdroid Map Configuration ---
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(this));
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);

        // --- Map Marker Setup ---
        userMarker = new Marker(map);
        userMarker.setTitle(getString(R.string.user_location_marker));
        userMarker.setEnabled(false); // Initially invisible until a location is found.
        map.getOverlays().add(userMarker);
        loadMarkersFromDatabase();

        userPath = new Polyline();
        userPath.getOutlinePaint().setColor(Color.BLUE);
        map.getOverlays().add(userPath);
        loadPathFromDatabase();

        // --- Permissions and Location Initiation ---
        checkAndRequestLocationPermission();

        // --- Sensor Setup ---
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if (accelerometer == null) {
            Toast.makeText(this, getString(R.string.noaccel), Toast.LENGTH_LONG).show();
        }
        if (gyroscope == null) {
            Toast.makeText(this, getString(R.string.no_gyroscope), Toast.LENGTH_LONG).show();
        }

        // --- UI Initialization ---
        date = findViewById(R.id.date);
        kroki = findViewById(R.id.kroki);
        dystans = findViewById(R.id.dystans);
        kalorie = findViewById(R.id.kalorie);
        updateDateUI();
        trackingButton = findViewById(R.id.button);
        mainLayout = findViewById(R.id.main);

        loadTrainingData(dayOffset);

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

        // --- Swipe Gesture Setup ---
        gestureDetector = new GestureDetector(this, new MyGestureListener());
        mainLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // ============ WSPÓLNY CUSTOM TOAST =============
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView text = layout.findViewById(R.id.text_toast);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.settings));
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            } else if (id == R.id.nav_home) {
                text.setText(getString(R.string.main_screen));
                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
                return true;

            } else if (id == R.id.nav_account) {
                text.setText(getString(R.string.account));
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
     * A gesture listener for detecting swipes on the main layout.
     */
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        /**
         * Called when a fling gesture is detected.
         *
         * @param e1        The first down motion event that started the fling.
         * @param e2        The move motion event that triggered the current onFling.
         * @param velocityX The velocity of this fling measured in pixels per second along the x-axis.
         * @param velocityY The velocity of this fling measured in pixels per second along the y-axis.
         * @return True if the event is consumed, false otherwise.
         */
        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (isTracking) return false; // Disable swipe while tracking

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe Right
                        if (dayOffset > 0) {
                            dayOffset--;
                            loadTrainingData(dayOffset);
                            updateDateUI();
                        }
                    } else {
                        // Swipe Left
                        if (dayOffset < 13) {
                            dayOffset++;
                            loadTrainingData(dayOffset);
                            updateDateUI();
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Saves the current state of the activity.
     * @param outState The bundle to save the state to.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("userId", userId);
        outState.putDouble("waga", waga);
    }
    /**
     * Loads training data from the database.
     */
    private void loadTrainingData(int dayIndex) {
        if (userId != -1) {
            Cursor cursor = dbHelper.getTrainingDataForDay(dayIndex, userId);
            if (cursor != null && cursor.moveToFirst()) {
                dist = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIST));
                kro = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KRO));
                kal = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KAL));
                cursor.close();
            } else {
                dist = 0;
                kro = 0;
                kal = 0;
            }
            updateTrainingUI();
            trackingButton.setEnabled(dayIndex == 0);
        }
    }

    /**
     * Updates the date UI with the current or selected date.
     */
    private void updateDateUI() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        String dateString = dateFormat.format(cal.getTime());
        date.setText(getString(R.string.dzien_formatted, dateString));
    }

    /**
     * Updates the training UI with the latest data.
     */
    private void updateTrainingUI() {
        dystans.setText(getString(R.string.dystans_formatted, Math.floor(dist) / 50, getString(R.string.meters_unit)));
        kroki.setText(getString(R.string.kroki_formatted, kro));
        kalorie.setText(getString(R.string.kalorie_formatted, (int) kal, getString(R.string.kcal_unit)));
    }

    /**
     * Checks if location permission has been granted. If not, requests it.
     * If it is granted, it proceeds to start the location tracking process.
     */
    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
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
                Toast.makeText(this, getString(R.string.nolok), Toast.LENGTH_LONG).show();
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
                .addOnSuccessListener(this, location -> {
                    if (location != null && !userMarker.isEnabled()) {
                        updateMapWithLocation(location);
                    }
                });
        // Start requesting continuous location updates.
        startLocationUpdates();
    }


    /**
     * Defines the parameters for location updates (priority and interval).
     */
    LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000) // High accuracy, 10-second interval.
            .build();

    /**
     * Callback object for receiving location updates.
     */
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            Location lastLocation = locationResult.getLastLocation();
            if (lastLocation != null) {
                updateMapWithLocation(lastLocation);
            }
        }
    };

    /**
     * Updates the map with a new location. It places the marker and centers the map.
     * Ignores invalid (0,0) coordinates.
     * @param location The new location to display.
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
        userMarker.setPosition(newPoint);

        if (isTracking) {
            pathPoints.add(newPoint);
            userPath.setPoints(pathPoints);
            dbHelper.addPathPoint(lat, lon, userId);
        }

        // If this is the first fix, enable the marker and jump to the location.
        if (!userMarker.isEnabled()) {
            userMarker.setEnabled(true);
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
        loadTrainingData(dayOffset);
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
        if (d != null) {
            d.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
            s.setSpan(span, s.length() - 1, s.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            item.setTitle(s);
        }

        // Ikona obok "Usuń dane"
        MenuItem deleteItem = menu.findItem(R.id.udane);
        SpannableString ss = new SpannableString("Usuń dane   "); // dodany odstęp
        Drawable dd = ContextCompat.getDrawable(this, R.drawable.delete);
        if (dd != null) {
            dd.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            dd.setBounds(0, 0, dd.getIntrinsicWidth(), dd.getIntrinsicHeight());
            ImageSpan spann = new ImageSpan(dd, ImageSpan.ALIGN_BOTTOM);
            ss.setSpan(spann, ss.length() - 1, ss.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            deleteItem.setTitle(ss);
        }
        //============== //================

        // Ikona obok "Alerty o niestabilnym chodzie"
        MenuItem alertItem = menu.findItem(R.id.alerty);
        SpannableString alertText = new SpannableString("Alerty o niestabilnym chodzie   "); // dodany odstęp
        Drawable alertIcon = ContextCompat.getDrawable(this, R.drawable.niest);
        if (alertIcon != null) {
            alertIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            alertIcon.setBounds(0, 0, alertIcon.getIntrinsicWidth(), alertIcon.getIntrinsicHeight());
            ImageSpan alertSpan = new ImageSpan(alertIcon, ImageSpan.ALIGN_BOTTOM);
            alertText.setSpan(alertSpan, alertText.length() - 1, alertText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            alertItem.setTitle(alertText);
        }

        // Ikona obok "Cotygodniowe raporty"
        MenuItem raportItem = menu.findItem(R.id.raporty);
        SpannableString raportText = new SpannableString("Cotygodniowe raporty   "); // dodany odstęp
        Drawable raportIcon = ContextCompat.getDrawable(this, R.drawable.raporty);
        if (raportIcon != null) {
            raportIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            raportIcon.setBounds(0, 0, raportIcon.getIntrinsicWidth(), raportIcon.getIntrinsicHeight());
            ImageSpan raportSpan = new ImageSpan(raportIcon, ImageSpan.ALIGN_BOTTOM);
            raportText.setSpan(raportSpan, raportText.length() - 1, raportText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            raportItem.setTitle(raportText);
        }
        //============== //================

        // Ikona obok "Pokazuj znaczniki na mapie"
        MenuItem znacznikiItem = menu.findItem(R.id.pznaczniki);
        SpannableString znacznikiText = new SpannableString("Pokazuj znaczniki na mapie   "); // dodany odstęp
        Drawable znacznikiIcon = ContextCompat.getDrawable(this, R.drawable.znaczniki);
        if (znacznikiIcon != null) {
            znacznikiIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            znacznikiIcon.setBounds(0, 0, znacznikiIcon.getIntrinsicWidth(), znacznikiIcon.getIntrinsicHeight());
            ImageSpan znacznikiSpan = new ImageSpan(znacznikiIcon, ImageSpan.ALIGN_BOTTOM);
            znacznikiText.setSpan(znacznikiSpan, znacznikiText.length() - 1, znacznikiText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            znacznikiItem.setTitle(znacznikiText);
        }
        //============== //================

        // Ikona obok "Usuń znacznik"
        MenuItem usunZnacznikItem = menu.findItem(R.id.uznacznik);
        SpannableString usunZnacznikText = new SpannableString("Usuń znacznik   "); // dodany odstęp
        Drawable usunZnacznikIcon = ContextCompat.getDrawable(this, R.drawable.delete);
        if (usunZnacznikIcon != null) {
            usunZnacznikIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            usunZnacznikIcon.setBounds(0, 0, usunZnacznikIcon.getIntrinsicWidth(), usunZnacznikIcon.getIntrinsicHeight());
            ImageSpan usunZnacznikSpan = new ImageSpan(usunZnacznikIcon, ImageSpan.ALIGN_BOTTOM);
            usunZnacznikText.setSpan(usunZnacznikSpan, usunZnacznikText.length() - 1, usunZnacznikText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            usunZnacznikItem.setTitle(usunZnacznikText);
        }
        //============== //================

        // Ikona obok "Porównaj ten dzień"
        MenuItem compareItem = menu.findItem(R.id.pdzien);
        SpannableString compareText = new SpannableString("Porównaj ten dzień   "); // dodany odstęp
        Drawable compareIcon = ContextCompat.getDrawable(this, R.drawable.compare);
        if (compareIcon != null) {
            compareIcon.setTint(Color.parseColor("#4CAF50")); // <-- kolor ikony
            compareIcon.setBounds(0, 0, compareIcon.getIntrinsicWidth(), compareIcon.getIntrinsicHeight());
            ImageSpan compareSpan = new ImageSpan(compareIcon, ImageSpan.ALIGN_BOTTOM);
            compareText.setSpan(compareSpan, compareText.length() - 1, compareText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            compareItem.setTitle(compareText);
        }
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
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.alerty) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.alert));
            if (suddenMovementAlertsEnabled) {
                builder.setMessage(getString(R.string.disable_sudden_movement_alerts_prompt));
            } else {
                builder.setMessage(getString(R.string.enable_sudden_movement_alerts_prompt));
            }
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.t), (dialog, which) -> {
                suddenMovementAlertsEnabled = !suddenMovementAlertsEnabled;
                String toastMessage = suddenMovementAlertsEnabled ? getString(R.string.sudden_movement_alerts_on_toast) : getString(R.string.sudden_movement_alerts_off_toast);
                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                dialog.cancel();
            });
            builder.setNegativeButton(getString(R.string.n), (dialog, which) -> dialog.cancel());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        } else if (id == R.id.raporty) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.weekly_report_title));
            if (weeklyReportEnabled) {
                builder.setMessage(getString(R.string.disable_weekly_report_alerts_prompt));
            } else {
                builder.setMessage(getString(R.string.enable_weekly_report_alerts_prompt));
            }
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.t), (dialog, which) -> {
                weeklyReportEnabled = !weeklyReportEnabled;
                String toastMessage = weeklyReportEnabled ? getString(R.string.weekly_report_alerts_on_toast) : getString(R.string.weekly_report_alerts_off_toast);
                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                dialog.cancel();
            });
            builder.setNegativeButton(getString(R.string.n), (dialog, which) -> dialog.cancel());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        } else if (id == R.id.pznaczniki) {
            incidentMarkersVisible = !incidentMarkersVisible;
            for (Marker m : incidentMarkers) {
                m.setEnabled(incidentMarkersVisible);
            }
            map.invalidate();
            return true;
        } else if (id == R.id.uznacznik) {
            if (!incidentMarkers.isEmpty()) {
                dbHelper.deleteLastMarker(userId);
                Marker lastMarker = incidentMarkers.remove(incidentMarkers.size() - 1);
                map.getOverlays().remove(lastMarker);
                map.invalidate();
            } else {
                Toast.makeText(this, getString(R.string.cannot_delete_first_marker), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.edane) {
            // Export user data and show a confirmation toast.
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(getString(R.string.export_data_prompt_csv));
            builder.setTitle(getString(R.string.export_data_title));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.t), (dialog, which) -> exportTrainingData());
            builder.setNegativeButton(getString(R.string.n), (dialog, which) -> dialog.cancel());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.udane) {
            // Clear user data and show a confirmation toast.
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(getString(R.string.clear_all_data_prompt));
            builder.setTitle(getString(R.string.alert));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.t), (dialog, which) -> {
                dbHelper.clearUsers();

                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));

                TextView text = layout.findViewById(R.id.text_toast);
                text.setText(R.string.cleardb);

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
            builder.setNegativeButton(getString(R.string.n), (dialog, which) -> dialog.cancel());
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }

        if (id == R.id.wyloguj) {
            // Log out the user and and return to the LoginActivity.
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));

            TextView text = layout.findViewById(R.id.text_toast);
            text.setText(R.string.logout);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        if (id == R.id.pdzien) {
            showComparisonDialog();
            return true;
        }

        if (id == R.id.debug_fill_data) {
            fillWithDummyData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * Fills the database with dummy data for testing purposes.
     */
    private void fillWithDummyData() {
        Random random = new Random();
        for (int i = 0; i < 14; i++) {
            double dummyDist = 1000 + random.nextDouble() * 9000; // Random distance between 1000 and 10000
            int dummyKro = 500 + random.nextInt(9500); // Random steps between 500 and 10000
            double dummyKal = 50 + random.nextDouble() * 450; // Random calories between 50 and 500
            dbHelper.saveTrainingData(i, dummyDist, dummyKro, dummyKal, userId);
        }
        Toast.makeText(this, "Filled database with dummy data", Toast.LENGTH_SHORT).show();
    }
    /**
     * Exports training data to a CSV file.
     */
    private void exportTrainingData() {
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e("exportTrainingData", "Failed to create directory");
            }
        }

        File file = new File(exportDir, "training_data.csv");
        try {
            if (!file.createNewFile()) {
                Log.e("exportTrainingData", "Failed to create file");
            }
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            osw.append(getString(R.string.csv_header));

            for (int i = 0; i < 14; i++) {
                Cursor cursor = dbHelper.getTrainingDataForDay(i, userId);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        osw.append(String.format("%s,%s,%s,%s,%s\n",
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4)));
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            osw.flush();
            osw.close();

            Toast.makeText(this, getString(R.string.data_exported_to_downloads), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("exportTrainingData", "Error writing to file", e);
            Toast.makeText(this, getString(R.string.data_export_error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when there is a new sensor event.
     * This method filters gravity from the accelerometer and calculates movement.
     * @param event The sensor event.
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

                long currentTime = System.currentTimeMillis();
                if (suddenMovementAlertsEnabled && magnitude > SUDDEN_MOVEMENT_THRESHOLD && (currentTime - lastSuddenMovementTime) > SUDDEN_MOVEMENT_COOLDOWN_MS) {
                    lastSuddenMovementTime = currentTime;
                    Snackbar.make(findViewById(R.id.main), getString(R.string.sudden_movement_detected), Snackbar.LENGTH_LONG).show();
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                            if (location != null) {
                                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                                Marker incidentMarker = new Marker(map);
                                incidentMarker.setPosition(point);
                                incidentMarker.setTitle(getString(R.string.sudden_movement_marker_title));
                                incidentMarker.setEnabled(incidentMarkersVisible);
                                map.getOverlays().add(incidentMarker);
                                incidentMarkers.add(incidentMarker);
                                dbHelper.addMarker(location.getLatitude(), location.getLongitude(), userId);
                                map.invalidate();
                            }
                        });
                    }
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

            if (waga > 0) { // Check if waga is greater than 0 to avoid division by zero
                kal = Math.floor(dist) / 50 * (3.5 / waga);
            } else {
                kal = 0; // Set calories to 0 if waga is not set
            }

            updateTrainingUI();
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * @param sensor The sensor being monitored.
     * @param accuracy The new accuracy of this sensor.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this application, but required to be implemented.
    }
    /**
     * Checks if the day has changed and shifts the training data accordingly.
     */
    private void checkDay() {
        Calendar c = Calendar.getInstance();
        int currentDay = c.get(Calendar.DAY_OF_YEAR);
        if (lastDay != -1 && lastDay != currentDay) {
            dbHelper.shiftTrainingData();
            if (weeklyReportEnabled && c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                showWeeklyReport();
            }
        }
        lastDay = currentDay;
    }
    /**
     * Shows a dialog comparing the current day's training data with the previous day's data.
     */
    private void showComparisonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.day_comparison_title));

        Cursor prevCursor = dbHelper.getTrainingDataForDay(dayOffset + 1, userId);
        Cursor currentCursor = dbHelper.getTrainingDataForDay(dayOffset, userId);

        if (prevCursor != null && prevCursor.moveToFirst()) {
            double prevDist = prevCursor.getDouble(prevCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIST));
            int prevKro = prevCursor.getInt(prevCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KRO));
            double prevKal = prevCursor.getDouble(prevCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KAL));

            String message = getString(R.string.previous_day) + "\n" +
                    getString(R.string.distance_label) + " " + String.format(Locale.getDefault(), "%.2f", prevDist / 50) + getString(R.string.meters_unit) + "\n" +
                    getString(R.string.steps_label) + " " + prevKro + "\n" +
                    getString(R.string.calories_label) + " " + String.format(Locale.getDefault(), "%.2f", prevKal) + getString(R.string.kcal_unit) + "\n\n";

            if (currentCursor != null && currentCursor.moveToFirst()) {
                double currentDist = currentCursor.getDouble(currentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIST));
                int currentKro = currentCursor.getInt(currentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KRO));
                double currentKal = currentCursor.getDouble(currentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KAL));
                message += getString(R.string.current_day) + "\n" +
                        getString(R.string.distance_label) + " " + String.format(Locale.getDefault(), "%.2f", currentDist / 50) + getString(R.string.meters_unit) + "\n" +
                        getString(R.string.steps_label) + " " + currentKro + "\n" +
                        getString(R.string.calories_label) + " " + String.format(Locale.getDefault(), "%.2f", currentKal) + getString(R.string.kcal_unit);
            }
            builder.setMessage(message);

        } else {
            builder.setMessage(getString(R.string.no_previous_data));
        }

        if (prevCursor != null) {
            prevCursor.close();
        }
        if (currentCursor != null) {
            currentCursor.close();
        }

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    /**
     * Shows a weekly report comparing the current week's training data with the previous week's data.
     */
    private void showWeeklyReport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.weekly_report_title));

        double prevWeekDist = 0, prevWeekKro = 0, prevWeekKal = 0;
        double currentWeekDist = 0, currentWeekKro = 0, currentWeekKal = 0;
        boolean dataFound = false;

        for (int i = 0; i < 14; i++) {
            Cursor cursor = dbHelper.getTrainingDataForDay(i, userId);
            if (cursor != null && cursor.moveToFirst()) {
                dataFound = true;
                do {
                    if (i < 7) {
                        currentWeekDist += cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIST));
                        currentWeekKro += cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KRO));
                        currentWeekKal += cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KAL));
                    } else {
                        prevWeekDist += cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DIST));
                        prevWeekKro += cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KRO));
                        prevWeekKal += cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_KAL));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        if (dataFound) {
            String message = getString(R.string.previous_week_report) + "\n" +
                    getString(R.string.distance_label) + " " + String.format(Locale.getDefault(), "%.2f", prevWeekDist / 50) + getString(R.string.meters_unit) + "\n" +
                    getString(R.string.steps_label) + " " + (int)prevWeekKro + "\n" +
                    getString(R.string.calories_label) + " " + String.format(Locale.getDefault(), "%.2f", prevWeekKal) + getString(R.string.kcal_unit) + "\n\n";

            message += getString(R.string.current_week_report) + "\n" +
                    getString(R.string.distance_label) + " " + String.format(Locale.getDefault(), "%.2f", currentWeekDist / 50) + getString(R.string.meters_unit) + "\n" +
                    getString(R.string.steps_label) + " " + (int)currentWeekKro + "\n" +
                    getString(R.string.calories_label) + " " + String.format(Locale.getDefault(), "%.2f", currentWeekKal) + getString(R.string.kcal_unit);
            builder.setMessage(message);
        } else {
            builder.setMessage(getString(R.string.not_enough_data_for_report));
        }

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    /**
     * Loads markers from the database and adds them to the map.
     */
    private void loadMarkersFromDatabase() {
        if (userId != -1) {
            Cursor cursor = dbHelper.getAllMarkers(userId);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                    GeoPoint point = new GeoPoint(lat, lon);
                    Marker incidentMarker = new Marker(map);
                    incidentMarker.setPosition(point);
                    incidentMarker.setTitle(getString(R.string.sudden_movement_marker_title));
                    incidentMarker.setEnabled(incidentMarkersVisible);
                    map.getOverlays().add(incidentMarker);
                    incidentMarkers.add(incidentMarker);
                } while (cursor.moveToNext());
                cursor.close();
                map.invalidate();
            }
        }
    }
    /**
     * Loads the user's path from the database and displays it on the map.
     */
    private void loadPathFromDatabase() {
        if (userId != -1) {
            Cursor cursor = dbHelper.getAllPathPoints(userId);
            if (cursor != null && cursor.moveToFirst()) {
                pathPoints.clear();
                do {
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                    pathPoints.add(new GeoPoint(lat, lon));
                } while (cursor.moveToNext());
                cursor.close();
                userPath.setPoints(pathPoints);
                map.invalidate();
            }
        }
    }


    /**
     * Called when the activity is being destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userId != -1) {
            dbHelper.saveTrainingData(0, dist, kro, kal, userId);
        }
    }
}
