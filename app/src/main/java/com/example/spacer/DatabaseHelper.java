package com.example.spacer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 4; // Incremented database version

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LOGIN = "login";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_WAGA = "waga";

    // Table for current day training data
    private static final String TABLE_TRAINING = "training_data";
    // New table for previous day training data
    private static final String TABLE_TRAINING_PREV = "training_data_prev";
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

        String CREATE_TRAINING_TABLE = "CREATE TABLE " + TABLE_TRAINING + " (" +
                COLUMN_TRAINING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DIST + " REAL, " +
                COLUMN_KRO + " INTEGER, " +
                COLUMN_KAL + " REAL, " +
                COLUMN_USER_ID + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_TRAINING_TABLE);

        String CREATE_TRAINING_PREV_TABLE = "CREATE TABLE " + TABLE_TRAINING_PREV + " (" +
            COLUMN_TRAINING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DIST + " REAL, " +
            COLUMN_KRO + " INTEGER, " +
            COLUMN_KAL + " REAL, " +
            COLUMN_USER_ID + " INTEGER, " +
            "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_TRAINING_PREV_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVesion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAINING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAINING_PREV);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
    
    public void shiftTrainingData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRAINING_PREV, null, null);
        db.execSQL("INSERT INTO " + TABLE_TRAINING_PREV + " SELECT * FROM " + TABLE_TRAINING);
        db.delete(TABLE_TRAINING, null, null);
        db.close();
    }

    public void clearUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRAINING, null, null);
        db.delete(TABLE_TRAINING_PREV, null, null);
        db.delete(TABLE_USERS, null, null); // usuwa wszystkie rekordy
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

        long result = db.insert(TABLE_TRAINING, null, values);
        db.close();
        return result != -1;
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

    public Cursor getAllTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING, null);
    }

    public Cursor getPreviousTrainingData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_TRAINING_PREV, null);
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
