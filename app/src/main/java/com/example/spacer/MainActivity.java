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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;



import androidx.core.content.ContextCompat;


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
 * @brief The main activity of the application. Handles user interaction, map display, sensor data, and training data management.
 *
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // --- Constants and Class Variables ---

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final float GYRO_THRESHOLD = 2.0f;
    private static final int STEP_DELAY_MS = 500;
    private static final double SUDDEN_MOVEMENT_THRESHOLD = 25.0;
    private static final long SUDDEN_MOVEMENT_COOLDOWN_MS = 5000;
    private long lastSuddenMovementTime = 0;
    private boolean suddenMovementAlertsEnabled = true;
    private boolean weeklyReportEnabled = true;
    private boolean incidentMarkersVisible = true;

    // UI Elements
    private MapView map;
    private TextView date;
    private Marker userMarker;
    private final List<Marker> incidentMarkers = new ArrayList<>();
    private ConstraintLayout mainLayout;
    private ViewSwitcher statsSwitcher;

    // Sensor-related variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private final float[] gravity = new float[3];
    double dist = 0;
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
    private SharedPreferences prefs;
    private String theme;

    /**
     * @brief Called when the activity is first created. Initializes UI elements, sensors, location services, and database helper.
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        String savedLang = prefs.getString("appLanguage", "pl");
        setLocale(savedLang);

        Object stored = prefs.getAll().get("theme");
        if (stored instanceof String) {
            theme = (String) stored;
        } else {
            theme = "default";
        }

        switch (theme) {
            case "light":
                setContentView(R.layout.activity_main_light);
                break;
            case "dark":
                setContentView(R.layout.activity_main_dark);
                break;
            default:
                setContentView(R.layout.activity_main);
                break;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);
        checkDay();

        if (savedInstanceState != null) {
            userId = savedInstanceState.getInt("userId", -1);
            waga = savedInstanceState.getDouble("waga", 0);
        } else {
            Intent intent = getIntent();
            userId = intent.getIntExtra("userId", -1);
            String wagaString = intent.getStringExtra("waga");
            if (wagaString != null && !wagaString.isEmpty()) {
                waga = Double.parseDouble(wagaString);
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(this));
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);

        userMarker = new Marker(map);
        userMarker.setTitle(getString(R.string.user_location_marker));
        userMarker.setEnabled(false);
        map.getOverlays().add(userMarker);
        loadMarkersFromDatabase();

        userPath = new Polyline();
        userPath.getOutlinePaint().setColor(Color.BLUE);
        map.getOverlays().add(userPath);
        loadPathFromDatabase();

        checkAndRequestLocationPermission();

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

        date = findViewById(R.id.date);
        updateDateUI();
        mainLayout = findViewById(R.id.main);
        statsSwitcher = findViewById(R.id.stats_switcher);

        statsSwitcher.setFactory(() -> {
            LayoutInflater inflater = getLayoutInflater();

            int statsLayoutRes;
            switch (theme) {
                case "light":
                    statsLayoutRes = R.layout.stats_view_light;
                    break;
                case "dark":
                    statsLayoutRes = R.layout.stats_view_dark;
                    break;
                default:
                    statsLayoutRes = R.layout.stats_view;
                    break;
            }

            return inflater.inflate(statsLayoutRes, statsSwitcher, false);
        });

        loadAndDisplayTrainingData(dayOffset);

        gestureDetector = new GestureDetector(this, new MyGestureListener());
        mainLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView text = layout.findViewById(R.id.text_toast);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);

            if (id == R.id.nav_settings) {
                text.setText(getString(R.string.b_settings));
                toast.show();

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;

            } else if (id == R.id.nav_home) {
                text.setText(getString(R.string.b_home_screen));
                toast.show();
                return true;

            } else if (id == R.id.nav_account) {
                text.setText(getString(R.string.b_my_account));
                toast.show();

                Intent intent = new Intent(MainActivity.this, AccountActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    /**
     * @brief A gesture listener to detect swipes for navigating between days.
     *
     */
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        /**
         * @brief Called when a fling gesture is detected.
         * @param e1        The first down motion event that started the fling.
         * @param e2        The move motion event that triggered the current onFling.
         * @param velocityX The velocity of this fling measured in pixels per second along the x axis.
         * @param velocityY The velocity of this fling measured in pixels per second along the y axis.
         * @return true if the event is consumed, else false.
         *
         */
        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            if (isTracking) return false;

            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // Swipe Right
                    if (dayOffset > 0) {
                        dayOffset--;
                        statsSwitcher.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_left));
                        statsSwitcher.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_right));
                        loadAndDisplayTrainingData(dayOffset, statsSwitcher.getNextView());
                        statsSwitcher.showPrevious();
                        updateDateUI();
                    }
                } else {
                    // Swipe Left
                    if (dayOffset < 13) {
                        dayOffset++;
                        statsSwitcher.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_right));
                        statsSwitcher.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_left));
                        loadAndDisplayTrainingData(dayOffset, statsSwitcher.getNextView());
                        statsSwitcher.showNext();
                        updateDateUI();
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * @brief Saves the instance state of the activity.
     * @param outState Bundle in which to place your saved state.
     *
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("userId", userId);
        outState.putDouble("waga", waga);
    }

    /**
     * @brief Loads and displays training data for a specific day index.
     * @param dayIndex The index of the day to load data for.
     *
     */
    private void loadAndDisplayTrainingData(int dayIndex) {
        loadAndDisplayTrainingData(dayIndex, statsSwitcher.getCurrentView());
    }

    /**
     * @brief Loads and displays training data for a specific day index into a specific view.
     * @param dayIndex      The index of the day to load data for.
     * @param viewToUpdate  The view to update with the loaded data.
     *
     */
    private void loadAndDisplayTrainingData(int dayIndex, View viewToUpdate) {
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
            updateStatsView(viewToUpdate, dayIndex);
        }
    }

    /**
     * @brief Updates the stats view with the current training data.
     * @param view      The view to update.
     * @param dayIndex  The index of the day being displayed.
     *
     */
    private void updateStatsView(View view, int dayIndex) {
        TextView dystans = view.findViewById(R.id.dystans);
        TextView kroki = view.findViewById(R.id.kroki);
        TextView kalorie = view.findViewById(R.id.kalorie);
        Button trackingButton = view.findViewById(R.id.button);

        dystans.setText(getString(R.string.dystans_formatted, Math.floor(dist) / 50, getString(R.string.meters_unit)));
        kroki.setText(getString(R.string.kroki_formatted, kro));
        kalorie.setText(getString(R.string.kalorie_formatted, (int) kal, getString(R.string.kcal_unit)));

        trackingButton.setEnabled(dayIndex == 0);
        trackingButton.setOnClickListener(v -> {
            isTracking = !isTracking;
            if (isTracking) {
                trackingButton.setText(R.string.zakoncz);
                switch (theme) {
                    case "light":
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.b_light_passive));
                        break;
                    case "dark":
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.b_dark_passive));
                        break;
                    default:
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green_background_dark));
                        break;
                }

                sendTrackingNotification(getString(R.string.start), getString(R.string.good_luck));
            } else {
                trackingButton.setText(R.string.rozpocznij);
                switch (theme) {
                    case "light":
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.b_light));
                        break;
                    case "dark":
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.b_dark));
                        break;
                    default:
                        mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green_background));
                        break;
                }
            }
        });
    }

    private void sendTrackingNotification(String title, String message) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true); // ta sama nazwa klucza!

        if (!notificationsEnabled) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "tracking_channel")
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify(1, builder.build());
    }


    /**
     * @brief Updates the date UI element with the currently displayed date.
     *
     */
    private void updateDateUI() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset);
        DateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        String dateString = dateFormat.format(cal.getTime());
        date.setText(getString(R.string.dzien_formatted, dateString));
    }

    /**
     * @brief Checks for location permissions and requests them if not granted.
     *
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
     * @brief Callback for the result from requesting permissions.
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationProcess();
            } else {
                Toast.makeText(this, getString(R.string.nolok), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * @brief Starts the location tracking process. Submethod handles the location updates.
     *
     */
    private void startLocationProcess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && !userMarker.isEnabled()) {
                        updateMapWithLocation(location);
                    }
                });
        startLocationUpdates();
    }


    LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .build();

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
     * @brief Updates the map with the user's current location.
     * @param location The user's current location.
     *
     */
    private void updateMapWithLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

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

        if (!userMarker.isEnabled()) {
            userMarker.setEnabled(true);
            map.getController().setZoom(18.0);
            map.getController().setCenter(newPoint);
        } else {
            map.getController().animateTo(newPoint);
        }
        map.invalidate();
    }

    /**
     * @brief Called when the activity will start interacting with the user.
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        suddenMovementAlertsEnabled = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("suddenMovementAlertsEnabled", true);
        checkThemeChange();
        map.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        startLocationUpdates();
        loadAndDisplayTrainingData(dayOffset);
    }

    /**
     * @brief Starts requesting location updates.
     *
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
     * @brief Called when the activity is no longer interacting with the user.
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * @brief Called when the activity is no longer visible to the user.
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * @brief Initialize the contents of the Activity's standard options menu.
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed- if you return false it will not be shown.
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu,menu);

        MenuItem logoutItem = menu.findItem(R.id.wyloguj);
        if (logoutItem != null) {
            SpannableString spanString = new SpannableString(logoutItem.getTitle());
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            logoutItem.setTitle(spanString);
        }


        MenuItem item = menu.findItem(R.id.edane);
        SpannableString s = new SpannableString(getString(R.string.data_export) + "  "); // dwie spacje
        Drawable d = ContextCompat.getDrawable(this, R.drawable.archive);
        if (d != null) {
            d.setTint(Color.parseColor("#4CAF50"));
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
            s.setSpan(span, s.length() - 2, s.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            item.setTitle(s);
        }

        MenuItem deleteItem = menu.findItem(R.id.udane);
        SpannableString ss = new SpannableString(getString(R.string.delete_data) + "  ");
        Drawable dd = ContextCompat.getDrawable(this, R.drawable.delete);
        if (dd != null) {
            dd.setTint(Color.parseColor("#4CAF50"));
            dd.setBounds(0, 0, dd.getIntrinsicWidth(), dd.getIntrinsicHeight());
            ImageSpan spann = new ImageSpan(dd, ImageSpan.ALIGN_BOTTOM);
            ss.setSpan(spann, ss.length() - 2, ss.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            deleteItem.setTitle(ss);
        }


        MenuItem raportItem = menu.findItem(R.id.raporty);
        SpannableString raportText = new SpannableString(getString(R.string.weekly_reports) + "  ");
        Drawable raportIcon = ContextCompat.getDrawable(this, R.drawable.raporty);
        if (raportIcon != null) {
            raportIcon.setTint(Color.parseColor("#4CAF50"));
            raportIcon.setBounds(0, 0, raportIcon.getIntrinsicWidth(), raportIcon.getIntrinsicHeight());
            ImageSpan raportSpan = new ImageSpan(raportIcon, ImageSpan.ALIGN_BOTTOM);
            raportText.setSpan(raportSpan, raportText.length() - 2, raportText.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            raportItem.setTitle(raportText);
        }

        MenuItem znacznikiItem = menu.findItem(R.id.pznaczniki);
        SpannableString znacznikiText = new SpannableString(getString(R.string.show_markers) + "  ");
        Drawable znacznikiIcon = ContextCompat.getDrawable(this, R.drawable.znaczniki);
        if (znacznikiIcon != null) {
            znacznikiIcon.setTint(Color.parseColor("#4CAF50"));
            znacznikiIcon.setBounds(0, 0, znacznikiIcon.getIntrinsicWidth(), znacznikiIcon.getIntrinsicHeight());
            ImageSpan znacznikiSpan = new ImageSpan(znacznikiIcon, ImageSpan.ALIGN_BOTTOM);
            znacznikiText.setSpan(znacznikiSpan, znacznikiText.length() - 2, znacznikiText.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            znacznikiItem.setTitle(znacznikiText);
        }

        MenuItem usunZnacznikItem = menu.findItem(R.id.uznacznik);
        SpannableString usunZnacznikText = new SpannableString(getString(R.string.delete_marker) + "  ");
        Drawable usunZnacznikIcon = ContextCompat.getDrawable(this, R.drawable.delete);
        if (usunZnacznikIcon != null) {
            usunZnacznikIcon.setTint(Color.parseColor("#4CAF50"));
            usunZnacznikIcon.setBounds(0, 0, usunZnacznikIcon.getIntrinsicWidth(), usunZnacznikIcon.getIntrinsicHeight());
            ImageSpan usunZnacznikSpan = new ImageSpan(usunZnacznikIcon, ImageSpan.ALIGN_BOTTOM);
            usunZnacznikText.setSpan(usunZnacznikSpan, usunZnacznikText.length() - 2, usunZnacznikText.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            usunZnacznikItem.setTitle(usunZnacznikText);
        }

        MenuItem compareItem = menu.findItem(R.id.pdzien);
        SpannableString compareText = new SpannableString(getString(R.string.compare_day) + "  ");
        Drawable compareIcon = ContextCompat.getDrawable(this, R.drawable.compare);
        if (compareIcon != null) {
            compareIcon.setTint(Color.parseColor("#4CAF50"));
            compareIcon.setBounds(0, 0, compareIcon.getIntrinsicWidth(), compareIcon.getIntrinsicHeight());
            ImageSpan compareSpan = new ImageSpan(compareIcon, ImageSpan.ALIGN_BOTTOM);
            compareText.setSpan(compareSpan, compareText.length() - 2, compareText.length() - 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            compareItem.setTitle(compareText);
        }

        MenuItem raportItemm = menu.findItem(R.id.raporty);
        if (raportItemm != null) {
            SpannableString sss = new SpannableString(raportItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            raportItemm.setTitle(sss);
        }

        MenuItem znacznikiItemm = menu.findItem(R.id.pznaczniki);
        if (znacznikiItemm != null) {
            SpannableString sss = new SpannableString(znacznikiItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            znacznikiItemm.setTitle(sss);
        }

        MenuItem usunZnacznikItemm = menu.findItem(R.id.uznacznik);
        if (usunZnacznikItemm != null) {
            SpannableString sss = new SpannableString(usunZnacznikItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            usunZnacznikItemm.setTitle(sss);
        }

        MenuItem pdzienItemm = menu.findItem(R.id.pdzien);
        if (pdzienItemm != null) {
            SpannableString sss = new SpannableString(pdzienItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            pdzienItemm.setTitle(sss);
        }

        MenuItem edaneItemm = menu.findItem(R.id.edane);
        if (edaneItemm != null) {
            SpannableString sss = new SpannableString(edaneItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            edaneItemm.setTitle(sss);
        }

        MenuItem udaneItemm = menu.findItem(R.id.udane);
        if (udaneItemm != null) {
            SpannableString sss = new SpannableString(udaneItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            udaneItemm.setTitle(sss);
        }

        MenuItem wylogujItemm = menu.findItem(R.id.wyloguj);
        if (wylogujItemm != null) {
            SpannableString sss = new SpannableString(wylogujItemm.getTitle());
            sss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")), 0, sss.length(), 0);
            wylogujItemm.setTitle(sss);
        }
        return true;
    }

    /**
     * @brief This hook is called whenever an item in your options menu is selected.
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.raporty) {
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
     * @brief Fills the database with dummy data for testing purposes.
     *
     */
    private void fillWithDummyData() {
        Random random = new Random();
        for (int i = 1; i < 14; i++) {
            double dummyDist = 1000 + random.nextDouble() * 9000;
            int dummyKro = 500 + random.nextInt(9500);
            double dummyKal = 50 + random.nextDouble() * 450;
            dbHelper.saveTrainingData(i, dummyDist, dummyKro, dummyKal, userId);
        }
        Toast.makeText(this, "Filled database with dummy data", Toast.LENGTH_SHORT).show();
    }

    /**
     * @brief Exports the training data to a CSV file in the Downloads directory.
     *
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
     * @brief Called when sensor values have changed.
     * @param event the {@link android.hardware.SensorEvent SensorEvent}.
     *
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isTracking) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                final float alpha = 0.8f;

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                float x = event.values[0] - gravity[0];
                float y = event.values[1] - gravity[1];
                float z = event.values[2] - gravity[2];

                double magnitude = Math.sqrt(x * x + y * y + z * z);

                final double MOVEMENT_THRESHOLD = 0.2;

                if (magnitude > MOVEMENT_THRESHOLD) {
                    dist = dist + Math.floor(magnitude);

                    if (waga > 0) {
                        kal = kal + (Math.floor(magnitude) / 50 * (3.5 / waga));
                    } else {
                        kal = 0;
                    }
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

            updateStatsView(statsSwitcher.getCurrentView(), dayOffset);
        }
    }

    /**
     * @brief Called when the accuracy of the registered sensor has changed.
     * @param sensor   The sensor being monitored.
     * @param accuracy The new accuracy of this sensor.
     *
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * @brief Checks if the day has changed and performs necessary actions.
     *
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
     * @brief Shows a dialog comparing the current day's data with the previous day's data.
     *
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
     * @brief Shows a weekly report comparing the current week's data with the previous week's data.
     *
     */
    private void showWeeklyReport() {
        double prevWeekDist = 0, prevWeekKro = 0, prevWeekKal = 0;
        double currentWeekDist = 0, currentWeekKro = 0, currentWeekKal = 0;
        boolean dataFound = false;
        String mess = "";

        // AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // builder.setTitle(getString(R.string.weekly_report_title));

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
            // builder.setMessage(message);
            mess = message;
        } else {
            sendTrackingNotification(getString(R.string.alert), getString(R.string.not_enough_data_for_report));
            // builder.setMessage(getString(R.string.not_enough_data_for_report));
        }
        sendTrackingNotification(getString(R.string.weekly_report_title), mess);

        // builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
        // builder.create().show();
    }

    /**
     * @brief Loads markers from the database and displays them on the map.
     *
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
     * @brief Loads the user's path from the database and displays it on the map.
     *
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
     * @brief Called when the activity is about to be destroyed.
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userId != -1) {
            dbHelper.saveTrainingData(0, dist, kro, kal, userId);
        }
    }
    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void checkThemeChange() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Object stored = prefs.getAll().get("theme");

        String savedTheme;
        if (stored instanceof String) {
            savedTheme = (String) stored;
        } else {
            savedTheme = "default";
        }

        if (!savedTheme.equals(theme)) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
}
