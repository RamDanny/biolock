package com.example.biolock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ViewData extends Activity {
    private TextView dataAcc, dataGyro, dataMag, dataSwipe, countDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewdata);

        dataAcc = findViewById(R.id.dataAcc);
        dataGyro = findViewById(R.id.dataGyro);
        dataMag = findViewById(R.id.dataMag);
        dataSwipe = findViewById(R.id.dataSwipe);
        countDb = findViewById(R.id.countDb);

        DatabaseManager db = new DatabaseManager(this);
        Cursor acc = db.get_acc();
        Cursor gyro = db.get_gyro();
        Cursor mag = db.get_mag();
        Cursor swipe = db.get_swipe();
        while (acc.moveToNext()) {
            String temp = dataAcc.getText().toString();
            String id = String.valueOf(acc.getInt(0));
            String x = acc.getString(1);
            String y = acc.getString(2);
            String z = acc.getString(3);
            String dt1 = acc.getString(4);
            dataAcc.setText(temp + x + " " + y + " " + z + " " + dt1 + "\n");
        }
        while (gyro.moveToNext()) {
            String temp = dataGyro.getText().toString();
            String id = String.valueOf(gyro.getInt(0));
            String gx = gyro.getString(1);
            String gy = gyro.getString(2);
            String gz = gyro.getString(3);
            String dt2 = gyro.getString(4);
            dataGyro.setText(temp + gx + " " + gy + " " + gz + " " + dt2 + "\n");
        }
        while (mag.moveToNext()) {
            String temp = dataMag.getText().toString();
            String id = String.valueOf(mag.getInt(0));
            String mx = mag.getString(1);
            String my = mag.getString(2);
            String mz = mag.getString(3);
            String dt3 = mag.getString(4);
            dataMag.setText(temp + mx + " " + my + " " + mz + " " + dt3 + "\n");
        }
        while (swipe.moveToNext()) {
            String temp = dataSwipe.getText().toString();
            String id = String.valueOf(swipe.getInt(0));
            String startx = swipe.getString(1);
            String starty = swipe.getString(2);
            String endx = swipe.getString(3);
            String endy = swipe.getString(4);
            String letter = swipe.getString(5);
            int gset = swipe.getInt(6);
            dataSwipe.setText(temp + letter + " | " + gset + " | " + startx + " " + starty + "\n");
        }
        countDb.setText(String.valueOf(db.getRowCount()));
    }

    public void backHome(View view) {
        Intent i = new Intent(ViewData.this, MainActivity.class);
        startActivity(i);
    }

    public void exportTableToCsv(SQLiteDatabase db, String tableName) {
        // Query the database to retrieve data from the table
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);

        // Create a CSV file to write the data
        String header = "";
        String[] columnNames = cursor.getColumnNames();
        for (String columnName : columnNames) {
            header += columnName + ",";
        }
        header += "\n";
        writeFileInternal(getApplicationContext(), tableName+".csv", header, false);

        // Write data rows
        String row = "";
        while (cursor.moveToNext()) {
            row = "";
            for (int i = 0; i < columnNames.length; i++) {
                row += cursor.getString(i) + ",";
            }
            row += "\n";
            writeFileInternal(getApplicationContext(), tableName+".csv", row, true);
        }
    }

    public void exportDb(View view) {
        String dbName = getResources().getString(R.string.db_name);
        String[] tables = {"accdata", "gyrodata", "magdata", "swipedata"};
        ArrayList<String> csvPaths = new ArrayList<>();

        SQLiteDatabase db = getApplicationContext().openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
        for (String table : tables) {
            exportTableToCsv(db, table);
            //csvPaths.add(csvPath);
            //System.out.println(csvPaths);
        }
        db.close();
    }

    public static void writeFileInternal(Context context, String filePath, String data, boolean append) {
        FileOutputStream fos = null;
        try {
            File file = new File(context.getFilesDir(), filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            fos = new FileOutputStream(file, append);
            fos.write(data.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String readFileInternal(Context context, String filePath) {
        FileInputStream fis = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            File file = new File(context.getFilesDir(), filePath);
            fis = new FileInputStream(file);
            int character;
            while ((character = fis.read()) != -1) {
                stringBuilder.append((char) character);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuilder.toString();
    }
}
