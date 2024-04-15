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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class ViewData extends Activity {
    private TextView dataAcc, dataGyro, dataMag, dataSwipe, countDb, exportProgress;
    private boolean train_mode;
    private CountDownLatch latch;
    private ArrayList accmeta, gyrometa, magmeta, swipemeta;
    private ArrayList accmeta_test, gyrometa_test, magmeta_test, swipemeta_test;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewdata);

        Intent i = getIntent();
        train_mode = i.getBooleanExtra("train_mode", false);

        dataAcc = findViewById(R.id.dataAcc);
        dataGyro = findViewById(R.id.dataGyro);
        dataMag = findViewById(R.id.dataMag);
        dataSwipe = findViewById(R.id.dataSwipe);
        exportProgress = findViewById(R.id.exportProgress);
        countDb = findViewById(R.id.countDb);
        System.out.println("TRAIN MODE IS: "+train_mode);
        DatabaseManager db = new DatabaseManager(this);
        Cursor acc = db.get_acc(train_mode);
        Cursor gyro = db.get_gyro(train_mode);
        Cursor mag = db.get_mag(train_mode);
        Cursor swipe = db.get_swipe(train_mode);
        runOnUiThread(this::onViewData);
        while (acc.moveToNext()) {
            String temp = dataAcc.getText().toString();
            String id = String.valueOf(acc.getInt(0));
            String x = acc.getString(1);
            String y = acc.getString(2);
            String z = acc.getString(3);
            String dt1 = acc.getString(4);
            dataAcc.setText(temp + x + " " + y + " " + z + " " + dt1 + "\n");
        }
        runOnUiThread(this::onViewData);
        while (gyro.moveToNext()) {
            String temp = dataGyro.getText().toString();
            String id = String.valueOf(gyro.getInt(0));
            String gx = gyro.getString(1);
            String gy = gyro.getString(2);
            String gz = gyro.getString(3);
            String dt2 = gyro.getString(4);
            dataGyro.setText(temp + gx + " " + gy + " " + gz + " " + dt2 + "\n");
        }
        runOnUiThread(this::onViewData);
        while (mag.moveToNext()) {
            String temp = dataMag.getText().toString();
            String id = String.valueOf(mag.getInt(0));
            String mx = mag.getString(1);
            String my = mag.getString(2);
            String mz = mag.getString(3);
            String dt3 = mag.getString(4);
            dataMag.setText(temp + mx + " " + my + " " + mz + " " + dt3 + "\n");
        }
        runOnUiThread(this::onViewData);
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
        runOnUiThread(this::onViewData);
        countDb.setText(String.valueOf(db.getRowCount(train_mode)));
        latch = new CountDownLatch(4);
        accmeta = new ArrayList<>(3);
        gyrometa = new ArrayList<>(3);
        magmeta = new ArrayList<>(3);
        swipemeta = new ArrayList<>(3);
        accmeta_test = new ArrayList<>(3);
        gyrometa_test = new ArrayList<>(3);
        magmeta_test = new ArrayList<>(3);
        swipemeta_test = new ArrayList<>(3);
    }

    private void onViewData() {
        exportProgress.setText("");
    }

    public void backHome(View view) {
        Intent i = new Intent(ViewData.this, MainActivity.class);
        startActivity(i);
    }

    public void exportTableToCsv(SQLiteDatabase db, String tableName) {
        // Query the database to retrieve data from the table
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);


        System.out.println("Exporting "+tableName+ ".csv in exportTableToCsv function");


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
        System.out.println("Exporting "+tableName+ ".csv finished");
    }

    public void exportDb(View view) {
        String dbName = getResources().getString(R.string.db_name);
        String[] tables;
        if (train_mode) {
            tables = new String[]{"accdata", "gyrodata", "magdata", "swipedata"};
        }
        else {
            tables = new String[]{"accdata_test", "gyrodata_test", "magdata_test", "swipedata_test"};
        }

        SQLiteDatabase db = getApplicationContext().openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
        for (String table : tables) {
            exportTableToCsv(db, table);
            createDataset(getApplicationContext(), table);
        }
        db.close();
        if (train_mode) {
            Intent i = new Intent(ViewData.this, TrainActivitySwipe.class);
            // acc meta
            i.putExtra("accmeta_filecount", (int) accmeta.get(0));
            i.putExtra("accmeta_numfeatures", (int) accmeta.get(1));
            i.putExtra("accmeta_exportfile", (String) accmeta.get(2));
            // gyro meta
            i.putExtra("gyrometa_filecount", (int) gyrometa.get(0));
            i.putExtra("gyrometa_numfeatures", (int) gyrometa.get(1));
            i.putExtra("gyrometa_exportfile", (String) gyrometa.get(2));
            // mag meta
            i.putExtra("magmeta_filecount", (int) magmeta.get(0));
            i.putExtra("magmeta_numfeatures", (int) magmeta.get(1));
            i.putExtra("magmeta_exportfile", (String) magmeta.get(2));
            // swipe meta
            i.putExtra("swipemeta_filecount", (int) swipemeta.get(0));
            i.putExtra("swipemeta_numfeatures", (int) swipemeta.get(1));
            i.putExtra("swipemeta_exportfile", (String) swipemeta.get(2));
            startActivity(i);
        }
        else {
            Intent i = new Intent(ViewData.this, TestActivitySwipe.class);
            // acc meta
            i.putExtra("accmeta_test_filecount", (int) accmeta_test.get(0));
            i.putExtra("accmeta_test_numfeatures", (int) accmeta_test.get(1));
            i.putExtra("accmeta_test_exportfile", (String) accmeta_test.get(2));
            // gyro meta
            i.putExtra("gyrometa_test_filecount", (int) gyrometa_test.get(0));
            i.putExtra("gyrometa_test_numfeatures", (int) gyrometa_test.get(1));
            i.putExtra("gyrometa_test_exportfile", (String) gyrometa_test.get(2));
            // mag meta
            i.putExtra("magmeta_test_filecount", (int) magmeta_test.get(0));
            i.putExtra("magmeta_test_numfeatures", (int) magmeta_test.get(1));
            i.putExtra("magmeta_test_exportfile", (String) magmeta_test.get(2));
            // swipe meta
            i.putExtra("swipemeta_test_filecount", (int) swipemeta_test.get(0));
            i.putExtra("swipemeta_test_numfeatures", (int) swipemeta_test.get(1));
            i.putExtra("swipemeta_test_exportfile", (String) swipemeta_test.get(2));
            startActivity(i);
        }
    }

    public void createDataset(Context context, String sensor) {
        // Generate dataset
        // Parse data into files for each gesture
        if (sensor.equals("swipedata") || sensor.equals("swipedata_test")) {
            // Generate dataset
            // Parse data into files for each gesture
            int fileCount = 0;
            String data = readFileInternal(getApplicationContext(), sensor+".csv");
            String[] filerows = data.split("\n");
            for (int i = 1; i < filerows.length; i++) {
                String[] parts = filerows[i].split(",");

                if (i == 1) {
                    fileCount++;
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", false);
                }
                else if (parts[10].equals(filerows[i-1].split(",")[10])) {
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", true);
                }
                else {
                    fileCount++;
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", false);
                }
                //System.out.println(sensor+"("+fileCount+") = " + filerows[i]);
            }
            System.out.println(sensor+": "+fileCount+" files written");
            runOnUiThread(this::onExportProgress);

            // Generate feature dataset using gesture files
            String exportFile = "";
            int numFeatures = 0;

            for (int i = 1; i <= fileCount; i++) {
                ArrayList<Float> coord_x = new ArrayList<>();
                ArrayList<Float> coord_y = new ArrayList<>();
                float sum_vel_x = 0, sum_vel_y = 0;
                float sum_acc_x = 0, sum_acc_y = 0;
                float sum_pressure = 0;
                float sum_major = 0, sum_minor = 0;
                float dev_20_x = 0, dev_20_y = 0;
                float dev_50_x = 0, dev_50_y = 0;
                float dev_80_x = 0, dev_80_y = 0;
                int rowCount = 0;
                String letter = "";
                String fileName = sensor+"(" + i + ").csv";
                String data_i = readFileInternal(getApplicationContext(), fileName);
                String[] lines = data_i.split("\n");
                String[] prev = {};
                for (int j = 0; j < lines.length; j++) {
                    String nextLine = lines[j];
                    String[] parts = nextLine.split(",");
                    coord_x.add(Float.parseFloat(parts[3]));
                    coord_y.add(Float.parseFloat(parts[4]));
                    float vel_x = Float.parseFloat(parts[5]);
                    float vel_y = Float.parseFloat(parts[6]);
                    if (j == 0) {
                        coord_x.add(0, Float.parseFloat(parts[1]));
                        coord_y.add(0, Float.parseFloat(parts[2]));
                    }
                    else {
                        LocalDateTime ts = getDateTime(parts[12]);
                        LocalDateTime prev_ts = getDateTime(prev[12]);
                        sum_acc_x += Math.abs(vel_x - Float.parseFloat(prev[5])); // (diffInSecs(prev_ts, ts) * 100);
                        sum_acc_y += Math.abs(vel_y - Float.parseFloat(prev[6])); // (diffInSecs(prev_ts, ts) * 100);
                        coord_x.add(0, Float.parseFloat(parts[3]));
                        coord_y.add(0, Float.parseFloat(parts[4]));
                    }
                    float pressure = Float.parseFloat(parts[7]);
                    float major = Float.parseFloat(parts[8]);
                    float minor = Float.parseFloat(parts[9]);
                    letter = parts[10];

                    sum_vel_x += Math.abs(vel_x);
                    sum_vel_y += Math.abs(vel_y);
                    sum_pressure += pressure;
                    sum_major += major;
                    sum_minor += minor;

                    rowCount++;
                    prev = parts;
                }

                for (int k = 0; k < coord_x.size(); k++) {
                    int twenty = coord_x.size() / 5;
                    int fifty = coord_x.size() / 2;
                    int eighty = (coord_x.size() * 4) / 5;
                    dev_20_x += Math.abs(coord_x.get(k) - coord_x.get(twenty));
                    dev_20_y += Math.abs(coord_y.get(k) - coord_y.get(twenty));
                    dev_50_x += Math.abs(coord_x.get(k) - coord_x.get(fifty));
                    dev_50_y += Math.abs(coord_y.get(k) - coord_y.get(fifty));
                    dev_80_x += Math.abs(coord_x.get(k) - coord_x.get(eighty));
                    dev_80_y += Math.abs(coord_y.get(k) - coord_y.get(eighty));
                }

                float avg_vel_x = sum_vel_x / rowCount;
                float avg_vel_y = sum_vel_y / rowCount;
                float avg_acc_x = sum_acc_x / rowCount;
                float avg_acc_y = sum_acc_y / rowCount;
                float avg_pressure = sum_pressure / rowCount;
                float avg_major = sum_major / rowCount;
                float avg_minor = sum_minor / rowCount;
                float avg_dev_20_x = dev_20_x / rowCount, avg_dev_20_y = dev_20_y / rowCount;
                float avg_dev_50_x = dev_50_x / rowCount, avg_dev_50_y = dev_50_y / rowCount;
                float avg_dev_80_x = dev_80_x / rowCount, avg_dev_80_y = dev_80_y / rowCount;

                // Append calculated values to a single CSV file
                String calculatedValues = letter + "," +
                        avg_vel_x + "," + avg_vel_y + "," +
                        avg_acc_x + "," + avg_acc_y + "," +
                        avg_pressure + "," + avg_major + "," + avg_minor + "," +
                        avg_dev_20_x + "," + avg_dev_20_y + "," +
                        avg_dev_50_x + "," + avg_dev_50_y + "," +
                        avg_dev_80_x + "," + avg_dev_80_y + "\n";
                exportFile = sensor+"_calc.csv";
                numFeatures = calculatedValues.split(",").length - 1;
                if (i == 1) {
                    writeFileInternal(context, exportFile, calculatedValues, false);
                } else {
                    writeFileInternal(context, exportFile, calculatedValues, true);
                }
                System.out.println(calculatedValues);
            }

            System.out.println(sensor+"_calc dataset created");
            runOnUiThread(this::onExportProgress);

            // Export to external filesystem
            copyToDownloads(context, sensor+".csv");
            copyToDownloads(context, sensor+"_calc.csv");

            if (sensor.equals("swipedata")) {
                swipemeta.add(fileCount);
                swipemeta.add(numFeatures);
                swipemeta.add(exportFile);
            }
            else {
                swipemeta_test.add(fileCount);
                swipemeta_test.add(numFeatures);
                swipemeta_test.add(exportFile);
            }
        }
        else {
            int fileCount = 0;
            String data = readFileInternal(getApplicationContext(), sensor+".csv");
            String[] filerows = data.split("\n");
            for (int i = 1; i < filerows.length; i++) {
                String[] parts = filerows[i].split(",");

                if (i == 1) {
                    fileCount++;
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", false);
                }
                else if (diffInSecs(getDateTime(parts[4]), getDateTime(filerows[i-1].split(",")[4])) < 3) {
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", true);
                }
                else {
                    fileCount++;
                    writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", false);
                }
                //System.out.println(sensor+"("+fileCount+") = " + filerows[i]);
            }
            System.out.println(sensor+": "+fileCount+" files written");
            runOnUiThread(this::onExportProgress);

            // Generate feature dataset using gesture files
            String exportFile = "";
            int numFeatures = 0;
            for (int i = 1; i <= fileCount; i++) {
                double sum_x = 0, sum_y = 0, sum_z = 0;
                double sum_squared_x = 0, sum_squared_y = 0, sum_squared_z = 0;
                double sum_abs_diff_x = 0, sum_abs_diff_y = 0, sum_abs_diff_z = 0;
                double sum_resultant = 0;
                double prev_x = 0, prev_y = 0, prev_z = 0;
                int rowCount = 0;

                String fileName = sensor+"(" + i + ").csv";
                String data_i = readFileInternal(getApplicationContext(), fileName);
                String[] lines = data_i.split("\n");
                for (String nextLine: lines) {
                    String[] parts = nextLine.split(",");
                    double sensor_x = Double.parseDouble(parts[1]);
                    double sensor_y = Double.parseDouble(parts[2]);
                    double sensor_z = Double.parseDouble(parts[3]);

                    sum_x += sensor_x;
                    sum_y += sensor_y;
                    sum_z += sensor_z;

                    sum_squared_x += Math.pow(sensor_x, 2);
                    sum_squared_y += Math.pow(sensor_y, 2);
                    sum_squared_z += Math.pow(sensor_z, 2);

                    if (rowCount > 0) {
                        sum_abs_diff_x += Math.abs(sensor_x - prev_x);
                        sum_abs_diff_y += Math.abs(sensor_y - prev_y);
                        sum_abs_diff_z += Math.abs(sensor_z - prev_z);
                    } else {
                        prev_x = sensor_x;
                        prev_y = sensor_y;
                        prev_z = sensor_z;
                    }

                    sum_resultant += Math.sqrt(Math.pow(sensor_x, 2) + Math.pow(sensor_y, 2) + Math.pow(sensor_z, 2));

                    rowCount++;
                }

                double avg_x = sum_x / rowCount;
                double avg_y = sum_y / rowCount;
                double avg_z = sum_z / rowCount;

                double std_dev_x = Math.sqrt((sum_squared_x / rowCount) - Math.pow(avg_x, 2));
                double std_dev_y = Math.sqrt((sum_squared_y / rowCount) - Math.pow(avg_y, 2));
                double std_dev_z = Math.sqrt((sum_squared_z / rowCount) - Math.pow(avg_z, 2));

                double avg_abs_diff_x = sum_abs_diff_x / (rowCount - 1);
                double avg_abs_diff_y = sum_abs_diff_y / (rowCount - 1);
                double avg_abs_diff_z = sum_abs_diff_z / (rowCount - 1);

                double avg_resultant = sum_resultant / rowCount;

                // Append calculated values to a single CSV file
                String calculatedValues = avg_x + "," + avg_y + "," + avg_z + "," +
                        std_dev_x + "," + std_dev_y + "," + std_dev_z + "," +
                        avg_abs_diff_x + "," + avg_abs_diff_y + "," + avg_abs_diff_z + "," +
                        avg_resultant + "\n";
                exportFile = sensor+"_calc.csv";
                numFeatures = calculatedValues.split(",").length;
                if (i == 1) {
                    writeFileInternal(context, exportFile, calculatedValues, false);
                } else {
                    writeFileInternal(context, exportFile, calculatedValues, true);
                }
                //System.out.println(calculatedValues);
            }
            System.out.println(sensor+"_calc dataset created");
            runOnUiThread(this::onExportProgress);

            // Export to external filesystem
            copyToDownloads(context, sensor+".csv");
            copyToDownloads(context, sensor+"_calc.csv");

            if (sensor.equals("accdata")) {
                accmeta.add(fileCount);
                accmeta.add(numFeatures);
                accmeta.add(exportFile);
            }
            else if (sensor.equals("gyrodata")) {
                gyrometa.add(fileCount);
                gyrometa.add(numFeatures);
                gyrometa.add(exportFile);
            }
            else if (sensor.equals("magdata")) {
                magmeta.add(fileCount);
                magmeta.add(numFeatures);
                magmeta.add(exportFile);
            }
            else if (sensor.equals("accdata_test")) {
                accmeta_test.add(fileCount);
                accmeta_test.add(numFeatures);
                accmeta_test.add(exportFile);
            }
            else if (sensor.equals("gyrodata_test")) {
                gyrometa_test.add(fileCount);
                gyrometa_test.add(numFeatures);
                gyrometa_test.add(exportFile);
            }
            else if (sensor.equals("magdata_test")) {
                magmeta_test.add(fileCount);
                magmeta_test.add(numFeatures);
                magmeta_test.add(exportFile);
            }
        }
        latch.countDown();
        runOnUiThread(this::onExportProgress);
    }

    public void onExportProgress() {
        long val = latch.getCount();
        if (val == 0) exportProgress.setText("Export Complete!");
        else exportProgress.setText("Exporting DB...");
    }

    public void deleteRecords(View view) {
        DatabaseManager db = new DatabaseManager(getApplicationContext());
        Boolean truncate = db.truncateDb(train_mode);
        Intent i = new Intent(ViewData.this, MainActivity.class);
        startActivity(i);
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

    public LocalDateTime getDateTime(String str) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
            return dateTime;
        }
        catch (Exception e) {
            System.err.println("Error parsing the string: " + e.getMessage());
        }
        return null;
    }

    public long diffInSecs(LocalDateTime dt1, LocalDateTime dt2) {
        Duration duration = Duration.between(dt1, dt2);
        return Math.abs(duration.getSeconds());
    }

    public void copyToDownloads(Context context, String filePath) {
        String data = readFileInternal(context, filePath);
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File csvFile = new File(exportDir, filePath);

        try {
            while (csvFile.exists()) {
                if (csvFile.delete()) {
                    Log.d("InternalToDownload", "Deleted existing file: " + csvFile.getAbsolutePath());
                } else {
                    Log.e("InternalToDownload", "Failed to delete existing file: " + csvFile.getAbsolutePath());
                }
            }
            csvFile.createNewFile();
            FileWriter csvWriter = new FileWriter(csvFile);
            csvWriter.append(data);

            // Close the FileWriter and cursor
            csvWriter.flush();
            csvWriter.close();

            Log.d("InternalToDownload", "Exported file '" + csvFile.getAbsolutePath() + "' to Downloads");
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e("InternalToDownload", "Error exporting file '" + csvFile.getAbsolutePath() + "' to Downloads" + e.getMessage());
        }
    }
}
