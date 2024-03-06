package com.example.biolock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

public class DatabaseManager extends SQLiteOpenHelper {

    public DatabaseManager(Context context){
        super(context,"BiolockTrials11.db",null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accdata(id INT PRIMARY KEY, acc_x TEXT, acc_y TEXT, acc_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE gyrodata(id INT PRIMARY KEY, gyro_x TEXT, gyro_y TEXT, gyro_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE swipedata(id INT PRIMARY KEY, start_x TEXT, start_y TEXT, end_x TEXT, end_y TEXT, letter TEXT, gset INT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE magdata(id INT PRIMARY KEY, mag_x TEXT, mag_y TEXT, mag_z TEXT, timestamp DATETIME)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insert_acc(String accx, String accy, String accz) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dt = dateTime.format(formatter);
        Random rand = new Random();
        int rand_int = rand.nextInt(64000);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", rand_int);
        cv.put("acc_x", accx);
        cv.put("acc_y", accy);
        cv.put("acc_z", accz);
        cv.put("timestamp", dt);
        long result = db.insert("accdata",null, cv);
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_gyro(String gyrox, String gyroy, String gyroz) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dt = dateTime.format(formatter);
        Random rand = new Random();
        int rand_int = rand.nextInt(64000);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", rand_int);
        cv.put("gyro_x", gyrox);
        cv.put("gyro_y", gyroy);
        cv.put("gyro_z", gyroz);
        cv.put("timestamp", dt);
        long result = db.insert("gyrodata",null, cv);
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_mag(String magx, String magy, String magz) {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dt = dateTime.format(formatter);
        Random rand = new Random();
        int rand_int = rand.nextInt(64000);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", rand_int);
        cv.put("mag_x", magx);
        cv.put("mag_y", magy);
        cv.put("mag_z", magz);
        cv.put("timestamp", dt);
        long result = db.insert("magdata",null, cv);
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_swipe(ArrayList<float[]> lines, String letter) {
        // get gesture id
        String countQuery = "SELECT COALESCE(MAX(gset), 0) FROM swipedata WHERE letter = '" + letter + "'";
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int gset = 0;
        long result = -1;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(countQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                gset = cursor.getInt(0);
            }

            gset++;
            // insert to swipedata
            ContentValues cv = new ContentValues();
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dt = dateTime.format(formatter);
            Random rand = new Random();
            for (float[] coords : lines) {
                cv.put("id", rand.nextInt(1000000000));
                cv.put("start_x", String.valueOf(coords[0]));
                cv.put("start_y", String.valueOf(coords[1]));
                cv.put("end_x", String.valueOf(coords[2]));
                cv.put("end_y", String.valueOf(coords[3]));
                cv.put("letter", letter);
                cv.put("gset", gset);
                cv.put("timestamp", dt);
                result = db.insert("swipedata",null, cv);
                if (result == -1)
                    return false;
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }

            if (db != null && db.isOpen()) {
                db.close();
            }
            if (result == -1)
                return false;
            else return true;
        }
    }

    public Cursor get_acc() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record = db.rawQuery("SELECT * FROM accdata",null);
        return record;
    }

    public Cursor get_gyro() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record = db.rawQuery("SELECT * FROM gyrodata",null);
        return record;
    }

    public Cursor get_mag() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record = db.rawQuery("SELECT * FROM magdata",null);
        return record;
    }

    public Cursor get_swipe() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record = db.rawQuery("SELECT * FROM swipedata",null);
        return record;
    }

    public int getRowCount() {
        String countQuery = "SELECT COUNT(*) FROM swipedata";
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int count = 0;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(countQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return count;
    }

}
