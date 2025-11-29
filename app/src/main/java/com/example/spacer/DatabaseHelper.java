package com.example.spacer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A helper class to manage database creation and version management for the Spacer application.
 *
 * <p>This class provides functionalities for creating, upgrading, and interacting with the
 * application's database, which stores user information, training data, markers, and path points.</p>
 *
 * <p>The database schema includes:
 * <ul>
 *     <li><b>{@value #TABLE_USERS}</b>: Stores user profiles (login, password, weight).</li>
 *     <li><b>training_data_N</b>: A set of 14 tables to store daily training data for each user.</li>
 *     <li><b>{@value #TABLE_MARKERS}</b>: Stores geographical markers placed by users.</li>
 *     <li><b>{@value #TABLE_PATH_POINTS}</b>: Stores points that form a user's path during a session.</li>
 * </ul>
 * </p>
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /** The name of the database file. */
    private static final String DATABASE_NAME = "users.db";
    /** The version of the database. Must be incremented when the schema changes. */
    private static final int DATABASE_VERSION = 7;

    // --- Table and Column Constants ---

    /** The name of the users table. */
    private static final String TABLE_USERS = "users";
    /** The unique ID for a user (primary key). */
    private static final String COLUMN_ID = "id";
    /** The user's login name. Must be unique. */
    private static final String COLUMN_LOGIN = "login";
    /** The user's password. */
    private static final String COLUMN_PASSWORD = "password";
    /** The user's weight. */
    private static final String COLUMN_WAGA = "waga";

    /** An array of table names for the 14 days of training data. */
    public static final String[] TABLE_TRAINING_DAYS = new String[14];
    static {
        for (int i = 0; i < 14; i++) {
            TABLE_TRAINING_DAYS[i] = "training_data_" + i;
        }
    }

    /** The name of the markers table. */
    private static final String TABLE_MARKERS = "markers";
    /** The unique ID for a marker (primary key). */
    private static final String COLUMN_MARKER_ID = "id";
    /** The latitude of a marker. */
    private static final String COLUMN_LATITUDE = "latitude";
    /** The longitude of a marker. */
    private static final String COLUMN_LONGITUDE = "longitude";

    /** The name of the path points table. */
    private static final String TABLE_PATH_POINTS = "path_points";
    /** The unique ID for a path point (primary key). */
    private static final String COLUMN_PATH_POINT_ID = "id";
    /** The latitude of a path point. */
    private static final String COLUMN_PATH_POINT_LATITUDE = "latitude";
    /** The longitude of a path point. */
    private static final String COLUMN_PATH_POINT_LONGITUDE = "longitude";

    // Common columns for training data tables
    /** The unique ID for a training entry (primary key). */
    private static final String COLUMN_TRAINING_ID = "id";
    /** The distance covered in the training session. */
    public static final String COLUMN_DIST = "dist";
    /** The number of steps taken in the training session. */
    public static final String COLUMN_KRO = "kro";
    /** The calories burned in the training session. */
    public static final String COLUMN_KAL = "kal";
    /** The foreign key referencing the user ID in the {@value #TABLE_USERS} table. */
    public static final String COLUMN_USER_ID = "user_id";

    /**
     * Constructs a new DatabaseHelper.
     *
     * @param context The context to use for locating paths to the database.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LOGIN + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_WAGA + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        // Create 14 tables for daily training data
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

        // Create the markers table
        String CREATE_MARKERS_TABLE = "CREATE TABLE " + TABLE_MARKERS + " (" +
                COLUMN_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_MARKERS_TABLE);

        // Create the path points table
        String CREATE_PATH_POINTS_TABLE = "CREATE TABLE " + TABLE_PATH_POINTS + " (" +
                COLUMN_PATH_POINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PATH_POINT_LATITUDE + " REAL, " +
                COLUMN_PATH_POINT_LONGITUDE + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_PATH_POINTS_TABLE);
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This simple upgrade policy is to drop all tables and recreate them.
        // A real-world application should implement a more robust migration strategy.
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATH_POINTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        // Recreate the database with the new schema
        onCreate(db);
    }

    /**
     * Shifts all training data forward by one day.
     * Data from day `i` is moved to day `i+1`, and the data from the last day is discarded.
     * The data for the current day (day 0) is cleared.
     */
    public void shiftTrainingData() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Start from the second to last day and move data to the next day.
        for (int i = 12; i >= 0; i--) {
            // Clear the destination day's table before copying.
            db.delete(TABLE_TRAINING_DAYS[i + 1], null, null);
            String cols = String.join(", ", new String[]{COLUMN_DIST, COLUMN_KRO, COLUMN_KAL, COLUMN_USER_ID});
            String insertQuery = "INSERT INTO " + TABLE_TRAINING_DAYS[i + 1] + " (" + cols + ") SELECT " + cols + " FROM " + TABLE_TRAINING_DAYS[i];
            db.execSQL(insertQuery);
        }
        // Clear the first day's data, as it has now been moved.
        db.delete(TABLE_TRAINING_DAYS[0], null, null);
        db.close();
    }

    /**
     * Deletes all data from all user-related tables in the database.
     * This is a destructive operation and should be used with caution.
     */
    public void clearUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Clear all training data tables
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.delete(tableName, null, null);
        }
        // Clear other tables
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_MARKERS, null, null);
        db.delete(TABLE_PATH_POINTS, null, null);
        db.close();
    }

    /**
     * Adds a new user to the users table.
     *
     * @param login    The user's desired login name (should be unique).
     * @param password The user's password.
     * @param waga     The user's weight as a string.
     * @return {@code true} if the user was successfully added, {@code false} otherwise.
     */
    public boolean addUser(String login, String password, String waga) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOGIN, login);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_WAGA, waga);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        // The insert method returns -1 if an error occurred.
        return result != -1;
    }

    /**
     * Saves or updates training data for a specific user and day.
     * If data for the given user and day already exists, it is updated. Otherwise, a new entry is inserted.
     *
     * @param dayIndex The index of the day to save data for (0-13, where 0 is today).
     * @param dist     The distance covered.
     * @param kro      The number of steps taken.
     * @param kal      The calories burned.
     * @param userId   The ID of the user.
     */
    public void saveTrainingData(int dayIndex, double dist, int kro, double kal, int userId) {
        if (dayIndex < 0 || dayIndex >= 14) {
            // Invalid day index, do nothing.
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DIST, dist);
        values.put(COLUMN_KRO, kro);
        values.put(COLUMN_KAL, kal);

        // Check if an entry for this user already exists for the given day.
        String selection = COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        Cursor cursor = db.query(TABLE_TRAINING_DAYS[dayIndex], null, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Data exists, so update the existing record.
            db.update(TABLE_TRAINING_DAYS[dayIndex], values, selection, selectionArgs);
        } else {
            // No data exists, so insert a new record.
            values.put(COLUMN_USER_ID, userId);
            db.insert(TABLE_TRAINING_DAYS[dayIndex], null, values);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    /**
     * Adds a new geographical marker to the database for a specific user.
     *
     * @param latitude  The latitude of the marker.
     * @param longitude The longitude of the marker.
     * @param userId    The ID of the user who owns the marker.
     */
    public void addMarker(double latitude, double longitude, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_USER_ID, userId);
        db.insert(TABLE_MARKERS, null, values);
        db.close();
    }

    /**
     * Adds a new point to the user's path history.
     *
     * @param latitude  The latitude of the path point.
     * @param longitude The longitude of the path point.
     * @param userId    The ID of the user this path point belongs to.
     */
    public void addPathPoint(double latitude, double longitude, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PATH_POINT_LATITUDE, latitude);
        values.put(COLUMN_PATH_POINT_LONGITUDE, longitude);
        values.put(COLUMN_USER_ID, userId);
        db.insert(TABLE_PATH_POINTS, null, values);
        db.close();
    }

    /**
     * Retrieves all path points for a specific user, ordered by their insertion time.
     *
     * @param userId The ID of the user.
     * @return A {@link Cursor} containing all path points for the user.
     */
    public Cursor getAllPathPoints(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Use query() with selection args to prevent SQL injection.
        return db.query(TABLE_PATH_POINTS, null, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)}, null, null, COLUMN_PATH_POINT_ID + " ASC");
    }

    /**
     * Deletes the most recently added marker for a specific user.
     *
     * @param userId The ID of the user whose last marker should be deleted.
     */
    public void deleteLastMarker(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Use execSQL with bind arguments to prevent SQL injection.
        String query = "DELETE FROM " + TABLE_MARKERS + " WHERE " + COLUMN_MARKER_ID + " = (SELECT MAX(" + COLUMN_MARKER_ID + ") FROM " + TABLE_MARKERS + " WHERE " + COLUMN_USER_ID + " = ?)";
        db.execSQL(query, new String[]{String.valueOf(userId)});
        db.close();
    }

    /**
     * Retrieves all markers for a specific user.
     *
     * @param userId The ID of the user.
     * @return A {@link Cursor} containing all markers for the user.
     */
    public Cursor getAllMarkers(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Use query() with selection args to prevent SQL injection.
        return db.query(TABLE_MARKERS, null, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    /**
     * Retrieves the training data for a specific day and user.
     *
     * @param dayIndex The index of the day (0-13).
     * @param userId   The ID of the user.
     * @return A {@link Cursor} containing the training data, or {@code null} if the day index is invalid.
     */
    public Cursor getTrainingDataForDay(int dayIndex, int userId) {
        if (dayIndex < 0 || dayIndex >= 14) {
            return null; // Invalid day index
        }
        SQLiteDatabase db = this.getReadableDatabase();
        // Use query() with selection args to prevent SQL injection.
        return db.query(TABLE_TRAINING_DAYS[dayIndex], null, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
    }

    /**
     * Checks if a user exists with the given login and password.
     *
     * @param login    The user's login name.
     * @param password The user's password.
     * @return {@code true} if a matching user is found, {@code false} otherwise.
     */
    public boolean checkUser(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_LOGIN + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {login, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Retrieves the weight of a user with the given credentials.
     *
     * @param login    The user's login name.
     * @param password The user's password.
     * @return The user's weight as a string, or {@code null} if the user is not found.
     */
    public String getWaga(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_WAGA};
        String selection = COLUMN_LOGIN + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {login, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        String waga = null;
        if (cursor.moveToFirst()) {
            // Using getColumnIndex is safer in case the column order changes.
            waga = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WAGA));
        }
        cursor.close();
        db.close();
        return waga;
    }

    /**
     * Retrieves the unique ID of a user with the given credentials.
     *
     * @param login    The user's login name.
     * @param password The user's password.
     * @return The user's ID, or -1 if the user is not found.
     */
    public int getUserId(String login, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_LOGIN + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {login, password};
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        }
        cursor.close();
        db.close();
        return userId;
    }
}
