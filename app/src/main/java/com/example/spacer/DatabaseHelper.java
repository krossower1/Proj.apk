package com.example.spacer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

/**
 * Helper class for managing database creation and version management.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 7; // Incremented database version for path table

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LOGIN = "login";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_WAGA = "waga";

    // New table structure for 14 days of training data
    public static final String[] TABLE_TRAINING_DAYS = new String[14];
    static {
        for (int i = 0; i < 14; i++) {
            TABLE_TRAINING_DAYS[i] = "training_data_" + i;
        }
    }

    // Markers table
    private static final String TABLE_MARKERS = "markers";
    private static final String COLUMN_MARKER_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";

    // Path points table
    private static final String TABLE_PATH_POINTS = "path_points";
    private static final String COLUMN_PATH_POINT_ID = "id";
    private static final String COLUMN_PATH_POINT_LATITUDE = "latitude";
    private static final String COLUMN_PATH_POINT_LONGITUDE = "longitude";


    // Columns for training data tables
    private static final String COLUMN_TRAINING_ID = "id";
    public static final String COLUMN_DIST = "dist";
    public static final String COLUMN_KRO = "kro";
    public static final String COLUMN_KAL = "kal";
    public static final String COLUMN_USER_ID = "user_id";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LOGIN + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_WAGA + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        // Create 14 tables for training data
        for (String tableName : TABLE_TRAINING_DAYS) {
            String CREATE_TRAINING_TABLE = "CREATE TABLE " + tableName + " (" +
                    COLUMN_TRAINING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DIST + " REAL, " +
                    COLUMN_KRO + " INTEGER, " +
                    COLUMN_KAL + " REAL, " +
                    COLUMN_USER_ID + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
            db.execSQL(CREATE_TRAINING_TABLE);
        }

        // Create markers table
        String CREATE_MARKERS_TABLE = "CREATE TABLE " + TABLE_MARKERS + " (" +
                COLUMN_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_MARKERS_TABLE);

        // Create path points table
        String CREATE_PATH_POINTS_TABLE = "CREATE TABLE " + TABLE_PATH_POINTS + " (" +
                COLUMN_PATH_POINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PATH_POINT_LATITUDE + " REAL, " +
                COLUMN_PATH_POINT_LONGITUDE + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_PATH_POINTS_TABLE);
    }

    /**
     * Called when the database needs to be upgraded.
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATH_POINTS);
        // Create tables again
        onCreate(db);
    }
    
    /**
     * Shifts the training data to the next day, deleting the oldest data.
     */
    public void shiftTrainingData() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Move data from day i to day i+1
        for (int i = 12; i >= 0; i--) {
            db.delete(TABLE_TRAINING_DAYS[i + 1], null, null);
            String cols = COLUMN_DIST + ", " + COLUMN_KRO + ", " + COLUMN_KAL + ", " + COLUMN_USER_ID;
            String insertQuery = "INSERT INTO " + TABLE_TRAINING_DAYS[i+1] + " (" + cols + ") SELECT " + cols + " FROM " + TABLE_TRAINING_DAYS[i];
            db.execSQL(insertQuery);
        }
        // Clear the first day's data
        db.delete(TABLE_TRAINING_DAYS[0], null, null);
        db.close();
    }

    /**
     * Deletes all data from all tables.
     */
    public void clearUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.delete(tableName, null, null);
        }
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_MARKERS, null, null);
        db.delete(TABLE_PATH_POINTS, null, null);
        db.close();
    }

    /**
     * Adds a new user to the database.
     * @param login User's login.
     * @param password User's password.
     * @param waga User's weight.
     * @return True if the user was added successfully, false otherwise.
     */
    public boolean addUser(String login, String password, String waga) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOGIN, login);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_WAGA, waga);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    /**
     * Adds a new training data entry for the current day.
     * @param dist Distance of the training.
     * @param kro Number of steps.
     * @param kal Calories burned.
     * @param userId ID of the user.
     * @return True if data was added successfully, false otherwise.
     */
    public boolean addTrainingData(double dist, int kro, double kal, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DIST, dist);
        values.put(COLUMN_KRO, kro);
        values.put(COLUMN_KAL, kal);
        values.put(COLUMN_USER_ID, userId);

        long result = db.insert(TABLE_TRAINING_DAYS[0], null, values);
        db.close();
        return result != -1;
    }

    /**
     * Adds a new marker to the database.
     * @param latitude Latitude of the marker.
     * @param longitude Longitude of the marker.
     * @param userId ID of the user.
     * @return True if the marker was added successfully, false otherwise.
     */
    public boolean addMarker(double latitude, double longitude, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_USER_ID, userId);
        long result = db.insert(TABLE_MARKERS, null, values);
        db.close();
        return result != -1;
    }
    
    /**
     * Adds a new path point to the database.
     * @param latitude Latitude of the point.
     * @param longitude Longitude of the point.
     * @param userId ID of the user.
     * @return True if the point was added successfully, false otherwise.
     */
    public boolean addPathPoint(double latitude, double longitude, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PATH_POINT_LATITUDE, latitude);
        values.put(COLUMN_PATH_POINT_LONGITUDE, longitude);
        values.put(COLUMN_USER_ID, userId);
        long result = db.insert(TABLE_PATH_POINTS, null, values);
        db.close();
        return result != -1;
    }

    /**
     * Retrieves all path points for a specific user.
     * @param userId ID of the user.
     * @return A cursor with all path points.
     */
    public Cursor getAllPathPoints(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PATH_POINTS + " WHERE " + COLUMN_USER_ID + " = " + userId + " ORDER BY " + COLUMN_PATH_POINT_ID + " ASC", null);
    }


    /**
     * Deletes the last added marker for a specific user.
     * @param userId ID of the user.
     */
    public void deleteLastMarker(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_MARKERS + " WHERE " + COLUMN_MARKER_ID + " = (SELECT MAX(" + COLUMN_MARKER_ID + ") FROM " + TABLE_MARKERS + " WHERE " + COLUMN_USER_ID + " = " + userId + ")";
        db.execSQL(query);
        db.close();
    }

    /**
     * Retrieves all markers for a specific user.
     * @param userId ID of the user.
     * @return A cursor with all markers.
     */
    public Cursor getAllMarkers(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_MARKERS + " WHERE " + COLUMN_USER_ID + " = " + userId, null);
    }

    /**
     * Gets the ID of the last user that was inserted into the database.
     * @return The user ID, or -1 if no user is found.
     */
    public int getLastUserId() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return userId;
    }

    /**
     * Retrieves training data for a specific day index.
     * @param dayIndex Index of the day (0-13).
     * @return A cursor with the training data, or null if the index is invalid.
     */
    public Cursor getTrainingDataForDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= 14) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[dayIndex], null);
    }

    /**
     * Retrieves all training data for the current day (day 0).
     * @return A cursor with the training data.
     */
    public Cursor getAllTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[0], null);
    }

    /**
     * Retrieves training data for the previous day (day 1).
     * @return A cursor with the training data.
     */
    public Cursor getPreviousTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[1], null);
    }

    /**
     * Checks if a user with the given credentials exists.
     * @param login User's login.
     * @param password User's password.
     * @return True if the user exists, false otherwise.
     */
    public boolean checkUser(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS +
                " WHERE " + COLUMN_LOGIN + "=? AND " + COLUMN_PASSWORD + "=? ";
        Cursor cursor = db.rawQuery(query, new String[]{login, password});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Retrieves the weight of a user with the given credentials.
     * @param login User's login.
     * @param password User's password.
     * @return The user's weight, or null if the user is not found.
     */
    public String getWaga(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WAGA + " FROM " + TABLE_USERS +
            " WHERE " + COLUMN_LOGIN + "=? AND " + COLUMN_PASSWORD + "=? ";
        Cursor cursor = db.rawQuery(query, new String[]{login, password});
        String waga = null;
        if (cursor.moveToFirst()) {
            waga = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return waga;
    }

    /**
     * Retrieves the user ID for a given login and password.
     * @param login User's login.
     * @param password User's password.
     * @return The user ID, or -1 if the user is not found.
     */
    public int getUserId(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_USERS +
                " WHERE " + COLUMN_LOGIN + "=? AND " + COLUMN_PASSWORD + "=? ";
        Cursor cursor = db.rawQuery(query, new String[]{login, password});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return userId;
    }
}
