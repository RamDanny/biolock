package com.example.biolock;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import libsvm.*;

public class TestModel extends Activity {
    private TextView testOutput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testmodel);

        testOutput = findViewById(R.id.testOutput);

        double acc_accuracy = testModel(getApplicationContext(), "accdata");
        double gyro_accuracy = testModel(getApplicationContext(), "gyrodata");
        double mag_accuracy = testModel(getApplicationContext(), "magdata");
        double swipe_accuracy = testSwipe(getApplicationContext());
        System.out.println("Acc = " + String.valueOf(acc_accuracy));
        System.out.println("Gyro = " + String.valueOf(gyro_accuracy));
        System.out.println("Mag = " + String.valueOf(mag_accuracy));
        System.out.println("Swipe = " + String.valueOf(swipe_accuracy));
        Log.d("TestingResult", "Testing successful!");

        if (acc_accuracy > 70 && gyro_accuracy > 70 && mag_accuracy > 70 && swipe_accuracy > 70) {
            testOutput.setText("VALID");
            testOutput.setTextColor(Color.GREEN);
        }
        else {
            testOutput.setText("NOT VALID");
            testOutput.setTextColor(Color.RED);
        }
    }

    public double testModel(Context context, String sensor) {
        // Generate dataset
        // Parse data into files for each gesture
        int fileCount = 0;
        String data = readFileInternal(getApplicationContext(), sensor+"_test.csv");
        String[] filerows = data.split("\n");
        for (int i = 1; i < filerows.length; i++) {
            String[] parts = filerows[i].split(",");

            if (i == 1) {
                fileCount++;
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", false);
            }
            else if (diffInSecs(getDateTime(parts[4]), getDateTime(filerows[i-1].split(",")[4])) < 6) {
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", true);
            }
            else {
                fileCount++;
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", false);
            }
            //System.out.println(sensor+"("+fileCount+") = " + filerows[i]);
        }
        System.out.println(sensor+"_test: "+fileCount+" files written");

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

            String fileName = sensor+"(" + i + ")_test.csv";
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
            exportFile = sensor+"_test_calc.csv";
            numFeatures = calculatedValues.split(",").length;
            if (i == 1) {
                writeFileInternal(context, exportFile, calculatedValues, false);
            } else {
                writeFileInternal(context, exportFile, calculatedValues, true);
            }
            //System.out.println(calculatedValues);
        }
        System.out.println(sensor+"_test_calc dataset created");

        // Export to external filesystem
        copyToDownloads(context, sensor+"_test.csv");
        copyToDownloads(context, sensor+"_test_calc.csv");

        // Load the model
        svm_model model = null;
        try {
            File file = new File(context.getFilesDir(), sensor+"_model.csv");
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            model = svm.svm_load_model(file.getAbsolutePath());
            Log.d("ModelLoad", "Loaded model  " + sensor + "_model.csv  successfully!");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Testing
        // Prepare test data
        String testfile = readFileInternal(context, exportFile);
        String[] testrows = testfile.split("\n");
        int rows = testrows.length;
        int cols = testrows[0].split(",").length;
        svm_node[][] testdata = new svm_node[rows][cols];
        for (int x = 0; x < rows; x++) {
            String[] testline = testrows[x].split(",");
            for (int y = 0; y < cols; y++) {
                testdata[x][y] = new svm_node();
                testdata[x][y].index = y + 1;
                testdata[x][y].value = Double.parseDouble(testline[y]);
            }
        }
        System.out.println("Test data ready!");

        // Make prediction
        double val = 0;
        int total = rows, correct = 0;
        for (int k = 0; k < rows; k++) {
            val = svm.svm_predict(model, testdata[k]);
            if (val == 1) correct++;
            System.out.println("Prediction " + k + " = " + String.valueOf(val));
        }
        double accuracy = (correct * 100.0) / total;
        System.out.println("Accuracy = " + correct + "/"+ total + " = " + String.valueOf(accuracy));
        return accuracy;
    }

    public double testSwipe(Context context) {
        String sensor = "swipedata";
        // Generate dataset
        // Parse data into files for each gesture
        int fileCount = 0;
        String data = readFileInternal(getApplicationContext(), sensor+"_test.csv");
        String[] filerows = data.split("\n");
        for (int i = 1; i < filerows.length; i++) {
            String[] parts = filerows[i].split(",");

            if (i == 1) {
                fileCount++;
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", false);
            }
            else if (parts[9].equals(filerows[i-1].split(",")[9])) {
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", true);
            }
            else {
                fileCount++;
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+")_test.csv", filerows[i]+"\n", false);
            }
            //System.out.println(sensor+"("+fileCount+") = " + filerows[i]);
        }
        System.out.println(sensor+"_test: "+fileCount+" files written");

        // Generate feature dataset using gesture files
        String exportFile = "";
        int numFeatures = 0;

        for (int i = 1; i <= fileCount; i++) {
            ArrayList<Float> coord_x = new ArrayList<>();
            ArrayList<Float> coord_y = new ArrayList<>();
            float sum_vel_x = 0, sum_vel_y = 0;
            float sum_acc_x = 0, sum_acc_y = 0;
            float sum_pressure = 0;
            float sum_area = 0;
            float dev_20_x = 0, dev_20_y = 0;
            float dev_50_x = 0, dev_50_y = 0;
            float dev_80_x = 0, dev_80_y = 0;
            int rowCount = 0;
            String letter = "";
            String fileName = sensor+"(" + i + ")_test.csv";
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
                    LocalDateTime ts = getDateTime(parts[11]);
                    LocalDateTime prev_ts = getDateTime(prev[11]);
                    sum_acc_x += Math.abs(vel_x - Float.parseFloat(prev[5])); // (diffInSecs(prev_ts, ts) * 100);
                    sum_acc_y += Math.abs(vel_y - Float.parseFloat(prev[6])); // (diffInSecs(prev_ts, ts) * 100);
                    coord_x.add(0, Float.parseFloat(parts[3]));
                    coord_y.add(0, Float.parseFloat(parts[4]));
                }
                float pressure = Float.parseFloat(parts[7]);
                float toucharea = Float.parseFloat(parts[8]);
                letter = parts[9];

                sum_vel_x += Math.abs(vel_x);
                sum_vel_y += Math.abs(vel_y);
                sum_pressure += pressure;
                sum_area += toucharea;

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
            float avg_area = sum_area / rowCount;
            float avg_dev_20_x = dev_20_x / rowCount, avg_dev_20_y = dev_20_y / rowCount;
            float avg_dev_50_x = dev_50_x / rowCount, avg_dev_50_y = dev_50_y / rowCount;
            float avg_dev_80_x = dev_80_x / rowCount, avg_dev_80_y = dev_80_y / rowCount;

            // Append calculated values to a single CSV file
            String calculatedValues = letter + "," +
                    avg_vel_x + "," + avg_vel_y + "," +
                    avg_acc_x + "," + avg_acc_y + "," +
                    avg_pressure + "," + avg_area + "," +
                    avg_dev_20_x + "," + avg_dev_20_y + "," +
                    avg_dev_50_x + "," + avg_dev_50_y + "," +
                    avg_dev_80_x + "," + avg_dev_80_y + "\n";
            exportFile = sensor+"_test_calc.csv";
            numFeatures = calculatedValues.split(",").length - 1;
            if (i == 1) {
                writeFileInternal(context, exportFile, calculatedValues, false);
            } else {
                writeFileInternal(context, exportFile, calculatedValues, true);
            }
            System.out.println(calculatedValues);
        }

        System.out.println(sensor+"_test_calc dataset created");

        // Export to external filesystem
        copyToDownloads(context, sensor+"_test.csv");
        copyToDownloads(context, sensor+"_test_calc.csv");

        // Load the model
        svm_model model = null;
        try {
            File file = new File(context.getFilesDir(), sensor+"_model.csv");
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            model = svm.svm_load_model(file.getAbsolutePath());
            Log.d("ModelLoad", "Loaded model  " + sensor + "_model.csv  successfully!");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Testing
        // Prepare test data
        String testfile = readFileInternal(context, exportFile);
        String[] testrows = testfile.split("\n");
        int rows = testrows.length;
        int cols = testrows[0].split(",").length;
        svm_node[][] testdata = new svm_node[rows][cols-1];
        String[] labelsarray = new String[rows];
        for (int x = 0; x < rows; x++) {
            String[] testline = testrows[x].split(",");
            labelsarray[x] = testline[0];
            for (int y = 0; y < cols-1; y++) {
                testdata[x][y] = new svm_node();
                testdata[x][y].index = y + 1;
                testdata[x][y].value = Double.parseDouble(testline[y+1]);
            }
        }
        System.out.println("Test data ready!");

        // Make prediction
        double val = 0;
        int total = rows, correct = 0;
        for (int k = 0; k < rows; k++) {
            val = svm.svm_predict(model, testdata[k]);
            if (val == labelsarray[k].charAt(0)) correct++;
            System.out.println("Prediction " + k + " = " + String.valueOf(val));
        }
        double accuracy = (correct * 100.0) / total;
        System.out.println("Accuracy = " + correct + "/"+ total + " = " + String.valueOf(accuracy));
        return accuracy;
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

