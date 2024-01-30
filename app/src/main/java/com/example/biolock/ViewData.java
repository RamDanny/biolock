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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ViewData extends Activity {
    private TextView dataAcc, dataGyro, dataSwipe, countDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewdata);

        dataAcc = findViewById(R.id.dataAcc);
        dataGyro = findViewById(R.id.dataGyro);
        dataSwipe = findViewById(R.id.dataSwipe);
        countDb = findViewById(R.id.countDb);

        DatabaseManager db = new DatabaseManager(this);
        Cursor acc = db.get_acc();
        Cursor gyro = db.get_gyro();
        Cursor swipe = db.get_swipe();
        while (acc.moveToNext()) {
            String temp = dataAcc.getText().toString();
            String id = String.valueOf(acc.getInt(0));
            String x = acc.getString(1);
            String y = acc.getString(2);
            String z = acc.getString(3);
            dataAcc.setText(temp + x + " " + y + " " + z + "\n");
        }
        while (gyro.moveToNext()) {
            String temp = dataGyro.getText().toString();
            String id = String.valueOf(gyro.getInt(0));
            String gx = gyro.getString(1);
            String gy = gyro.getString(2);
            String gz = gyro.getString(3);
            dataGyro.setText(temp + gx + " " + gy + " " + gz + "\n");
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

    public String exportTableToCsv(SQLiteDatabase db, String tableName) {
        // Query the database to retrieve data from the table
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);

        // Create a CSV file to write the data
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File csvFile = new File(exportDir, tableName + ".csv");

        try {
            int count = 1;
            while (csvFile.exists()) {
                if (csvFile.delete()) {
                    Log.d("DatabaseExporter", "Deleted existing file: " + csvFile.getAbsolutePath());
                } else {
                    Log.e("DatabaseExporter", "Failed to delete existing file: " + csvFile.getAbsolutePath());
                }
            }
            csvFile.createNewFile();
            FileWriter csvWriter = new FileWriter(csvFile);

            String[] columnNames = cursor.getColumnNames();
            for (String columnName : columnNames) {
                csvWriter.append(columnName).append(",");
            }
            csvWriter.append("\n");

            // Write data rows
            while (cursor.moveToNext()) {
                for (int i = 0; i < columnNames.length; i++) {
                    csvWriter.append(cursor.getString(i)).append(",");
                }
                csvWriter.append("\n");
            }

            // Close the FileWriter and cursor
            csvWriter.flush();
            csvWriter.close();
            cursor.close();

            Log.d("DatabaseExporter", "Exported table '" + tableName + "' to CSV: " + csvFile.getAbsolutePath());
            return csvFile.getAbsolutePath();
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("DatabaseExporter", "Error exporting table '" + tableName + "' to CSV: " + e.getMessage());
            return null;
        }
    }

    public void exportDb(View view) {
        String dbName = getResources().getString(R.string.db_name);
        String[] tables = {"accdata", "gyrodata", "swipedata"};
        ArrayList<String> csvPaths = new ArrayList<>();

        SQLiteDatabase db = getApplicationContext().openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
        for (String table : tables) {
            String csvPath = exportTableToCsv(db, table);
            csvPaths.add(csvPath);
            System.out.println(csvPaths);
        }
        db.close();
    }
}
