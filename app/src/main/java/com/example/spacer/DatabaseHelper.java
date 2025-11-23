package com.example.spacer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 6; // Incremented database version

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

    private static final String TABLE_MARKERS = "markers";
    private static final String COLUMN_MARKER_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";

    private static final String COLUMN_TRAINING_ID = "id";
    public static final String COLUMN_DIST = "dist";
    public static final String COLUMN_KRO = "kro";
    public static final String COLUMN_KAL = "kal";
    public static final String COLUMN_USER_ID = "user_id";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LOGIN + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_WAGA + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

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

        String CREATE_MARKERS_TABLE = "CREATE TABLE " + TABLE_MARKERS + " (" +
                COLUMN_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_MARKERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKERS);
        onCreate(db);
    }
    
    public void shiftTrainingData() {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 12; i >= 0; i--) {
            db.delete(TABLE_TRAINING_DAYS[i + 1], null, null);
            String cols = COLUMN_DIST + ", " + COLUMN_KRO + ", " + COLUMN_KAL + ", " + COLUMN_USER_ID;
            String insertQuery = "INSERT INTO " + TABLE_TRAINING_DAYS[i+1] + " (" + cols + ") SELECT " + cols + " FROM " + TABLE_TRAINING_DAYS[i];
            db.execSQL(insertQuery);
        }
        db.delete(TABLE_TRAINING_DAYS[0], null, null);
        db.close();
    }

    public void clearUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        for (String tableName : TABLE_TRAINING_DAYS) {
            db.delete(tableName, null, null);
        }
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_MARKERS, null, null);
        db.close();
    }

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

    public void deleteLastMarker(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_MARKERS + " WHERE " + COLUMN_MARKER_ID + " = (SELECT MAX(" + COLUMN_MARKER_ID + ") FROM " + TABLE_MARKERS + " WHERE " + COLUMN_USER_ID + " = " + userId + ")";
        db.execSQL(query);
        db.close();
    }

    public Cursor getAllMarkers(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_MARKERS + " WHERE " + COLUMN_USER_ID + " = " + userId, null);
    }

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

    public Cursor getTrainingDataForDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= 14) {
            return null;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[dayIndex], null);
    }

    public Cursor getAllTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[0], null);
    }

    public Cursor getPreviousTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_DAYS[1], null);
    }

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
}
