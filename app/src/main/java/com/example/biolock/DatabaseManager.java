package com.example.biolock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

public class DatabaseManager extends SQLiteOpenHelper {

    public DatabaseManager(Context context){
        super(context,"BiolockReals22.db",null,1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accdata(id INTEGER PRIMARY KEY AUTOINCREMENT, acc_x TEXT, acc_y TEXT, acc_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE gyrodata(id INTEGER PRIMARY KEY AUTOINCREMENT, gyro_x TEXT, gyro_y TEXT, gyro_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE swipedata(id INTEGER PRIMARY KEY AUTOINCREMENT, start_x TEXT, start_y TEXT, end_x TEXT, end_y TEXT, vel_x TEXT, vel_y TEXT, pressure TEXT, majoraxis TEXT, minoraxis TEXT, letter TEXT, gset INT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE magdata(id INTEGER PRIMARY KEY AUTOINCREMENT, mag_x TEXT, mag_y TEXT, mag_z TEXT, timestamp DATETIME)");

        db.execSQL("CREATE TABLE accdata_test(id INTEGER PRIMARY KEY AUTOINCREMENT, acc_x TEXT, acc_y TEXT, acc_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE gyrodata_test(id INTEGER PRIMARY KEY AUTOINCREMENT, gyro_x TEXT, gyro_y TEXT, gyro_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE magdata_test(id INTEGER PRIMARY KEY AUTOINCREMENT, mag_x TEXT, mag_y TEXT, mag_z TEXT, timestamp DATETIME)");
        db.execSQL("CREATE TABLE swipedata_test(id INTEGER PRIMARY KEY AUTOINCREMENT, start_x TEXT, start_y TEXT, end_x TEXT, end_y TEXT, vel_x TEXT, vel_y TEXT, pressure TEXT, majoraxis TEXT, minoraxis TEXT, letter TEXT, gset INT, timestamp DATETIME)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean insert_acc(String accx, String accy, String accz, boolean train_mode) {
        Long timenow = Instant.now().toEpochMilli();
        String dt = String.valueOf(timenow);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("acc_x", accx);
        cv.put("acc_y", accy);
        cv.put("acc_z", accz);
        cv.put("timestamp", dt);
        long result;
        if (train_mode) {
            result = db.insert("accdata",null, cv);
        }
        else {
            result = db.insert("accdata_test",null, cv);
        }
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_gyro(String gyrox, String gyroy, String gyroz, boolean train_mode) {
        Long timenow = Instant.now().toEpochMilli();
        System.out.println("Time="+timenow);
        String dt = String.valueOf(timenow);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("gyro_x", gyrox);
        cv.put("gyro_y", gyroy);
        cv.put("gyro_z", gyroz);
        cv.put("timestamp", dt);
        long result;
        if (train_mode) {
            result = db.insert("gyrodata",null, cv);
        }
        else {
            result = db.insert("gyrodata_test",null, cv);
        }
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_mag(String magx, String magy, String magz, boolean train_mode) {
        Long timenow = Instant.now().toEpochMilli();
        String dt = String.valueOf(timenow);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mag_x", magx);
        cv.put("mag_y", magy);
        cv.put("mag_z", magz);
        cv.put("timestamp", dt);
        long result;
        if (train_mode) {
            result = db.insert("magdata", null, cv);
        }
        else {
            result = db.insert("magdata_test", null, cv);
        }
        if(result == -1)
            return false;
        return true;
    }

    public boolean insert_swipe(ArrayList<float[]> lines, ArrayList<Long> swipetimes, ArrayList<Float> pressures, ArrayList<float[]> velocities, ArrayList<float[]> axes, String letter, boolean train_mode) {
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
            //Long timenow = Instant.now().toEpochMilli();
            //String dt = String.valueOf(timenow);
            String dt = "";
            for (int i = 0; i < lines.size(); i++) {
                float[] coords = lines.get(i);
                dt = String.valueOf(swipetimes.get(i));
                float pressure = pressures.get(i);
                float[] velocity = velocities.get(i);
                float[] axis = axes.get(i);
                cv.put("start_x", String.valueOf(coords[0]));
                cv.put("start_y", String.valueOf(coords[1]));
                cv.put("end_x", String.valueOf(coords[2]));
                cv.put("end_y", String.valueOf(coords[3]));
                cv.put("vel_x", String.valueOf(velocity[0]));
                cv.put("vel_y", String.valueOf(velocity[1]));
                cv.put("pressure", String.valueOf(pressure));
                cv.put("majoraxis", String.valueOf(axis[0]));
                cv.put("minoraxis", String.valueOf(axis[1]));
                cv.put("letter", letter);
                cv.put("gset", gset);
                cv.put("timestamp", dt);
                if (train_mode) {
                    result = db.insert("swipedata", null, cv);
                }
                else {
                    result = db.insert("swipedata_test", null, cv);
                }
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

    public Cursor get_acc(boolean train_mode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record;
        if (train_mode) {
            record = db.rawQuery("SELECT * FROM accdata", null);
        }
        else {
            record = db.rawQuery("SELECT * FROM accdata_test", null);
        }
        return record;
    }

    public Cursor get_gyro(boolean train_mode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record;
        if (train_mode) {
            record = db.rawQuery("SELECT * FROM gyrodata", null);
        }
        else {
            record = db.rawQuery("SELECT * FROM gyrodata_test", null);
        }
        return record;
    }

    public Cursor get_mag(boolean train_mode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record;
        if (train_mode) {
            record = db.rawQuery("SELECT * FROM magdata", null);
        }
        else {
            record = db.rawQuery("SELECT * FROM magdata_test", null);
        }
        return record;
    }

    public Cursor get_swipe(boolean train_mode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor record;
        if (train_mode) {
            record = db.rawQuery("SELECT * FROM swipedata", null);
        }
        else {
            record = db.rawQuery("SELECT * FROM swipedata_test", null);
        }
        return record;
    }

    public int getRowCount(boolean train_mode) {
        String countQuery;
        if (train_mode) {
            countQuery = "SELECT COUNT(*) FROM swipedata";
        }
        else {
            countQuery = "SELECT COUNT(*) FROM swipedata_test";
        }
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

    public boolean truncateDb(boolean train_mode) {
        SQLiteDatabase db = getWritableDatabase();
        if (train_mode) {
            db.delete("accdata", null, null);
            db.delete("gyrodata", null, null);
            db.delete("magdata", null, null);
            db.delete("swipedata", null, null);
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'accdata'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'gyrodata'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'magdata'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'swipedata'");
            db.close();
        }
        else {
            db.delete("accdata_test", null, null);
            db.delete("gyrodata_test", null, null);
            db.delete("magdata_test", null, null);
            db.delete("swipedata_test", null, null);
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'accdata_test'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'gyrodata_test'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'magdata_test'");
            db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'swipedata_test'");
            db.close();
        }
        return true;
    }

}
