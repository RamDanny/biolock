package com.example.biolock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import java.util.concurrent.atomic.AtomicReference;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

public class
TestModel extends Activity {
    private TextView testOutput, testText;
    private CountDownLatch latch;
    private ArrayList accmeta_test, gyrometa_test, magmeta_test, swipemeta_test;
    private AtomicReference<Double> acc_accuracy, gyro_accuracy, mag_accuracy, swipe_accuracy;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testmodel);

        testText = findViewById(R.id.testText);
        testOutput = findViewById(R.id.testOutput);

        accmeta_test = new ArrayList<>(3);
        gyrometa_test = new ArrayList<>(3);
        magmeta_test = new ArrayList<>(3);
        swipemeta_test = new ArrayList<>(3);

        Intent i = getIntent();
        accmeta_test.add(i.getIntExtra("accmeta_test_filecount", 0));
        accmeta_test.add(i.getIntExtra("accmeta_test_numfeatures", 0));
        accmeta_test.add(i.getStringExtra("accmeta_test_exportfile"));
        gyrometa_test.add(i.getIntExtra("gyrometa_test_filecount", 0));
        gyrometa_test.add(i.getIntExtra("gyrometa_test_numfeatures", 0));
        gyrometa_test.add(i.getStringExtra("gyrometa_test_exportfile"));
        magmeta_test.add(i.getIntExtra("magmeta_test_filecount", 0));
        magmeta_test.add(i.getIntExtra("magmeta_test_numfeatures", 0));
        magmeta_test.add(i.getStringExtra("magmeta_test_exportfile"));
        swipemeta_test.add(i.getIntExtra("swipemeta_test_filecount", 0));
        swipemeta_test.add(i.getIntExtra("swipemeta_test_numfeatures", 0));
        swipemeta_test.add(i.getStringExtra("swipemeta_test_exportfile"));

        runTestingTasks();
    }

    public void runTestingTasks() {
        acc_accuracy = new AtomicReference<>((double) 0);
        gyro_accuracy = new AtomicReference<>((double) 0);
        mag_accuracy = new AtomicReference<>((double) 0);
        swipe_accuracy = new AtomicReference<>((double) 0);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        latch = new CountDownLatch(4);

        executorService.submit(() -> {
            acc_accuracy.set(testModel(getApplicationContext(), "accdata"));
            System.out.println("Acc = " + acc_accuracy.get());
            latch.countDown();
            runOnUiThread(this::onTestingTaskCompleted);
        });

        executorService.submit(() -> {
            gyro_accuracy.set(testModel(getApplicationContext(), "gyrodata"));
            System.out.println("Gyro = " + gyro_accuracy.get());
            latch.countDown();
            runOnUiThread(this::onTestingTaskCompleted);
        });

        executorService.submit(() -> {
            mag_accuracy.set(testModel(getApplicationContext(), "magdata"));
            System.out.println("Mag = " + mag_accuracy.get());
            latch.countDown();
            runOnUiThread(this::onTestingTaskCompleted);
        });

        executorService.submit(() -> {
            swipe_accuracy.set(testSwipe(getApplicationContext()));
            System.out.println("Swipe = " + swipe_accuracy.get());
            latch.countDown();
            runOnUiThread(this::onTestingTaskCompleted);
        });

        executorService.shutdown();
    }

    private void onTestingTaskCompleted() {
        long ct = latch.getCount();
        if (ct > 0) {
            testText.setText("The user is :");
        }
        else {
            Log.d("TestingResult", "Testing successful!");
            if (acc_accuracy.get() > 70 && gyro_accuracy.get() > 70 && mag_accuracy.get() > 70 && swipe_accuracy.get() > 70) {
                testOutput.setText("VALID");
                testOutput.setTextColor(Color.GREEN);
            }
            else {
                testOutput.setText("NOT VALID");
                testOutput.setTextColor(Color.RED);
            }
        }
    }

    public double testModel(Context context, String sensor) {
        int fileCount = 0, numFeatures = 0;
        String exportFile = "";
        if (sensor.equals("accdata_test")) {
            fileCount = (int) accmeta_test.get(0);
            numFeatures = (int) accmeta_test.get(1);
            exportFile = (String) accmeta_test.get(2);
        }
        else if (sensor.equals("gyrodata_test")) {
            fileCount = (int) gyrometa_test.get(0);
            numFeatures = (int) gyrometa_test.get(1);
            exportFile = (String) gyrometa_test.get(2);
        }
        else {
            fileCount = (int) magmeta_test.get(0);
            numFeatures = (int) magmeta_test.get(1);
            exportFile = (String) magmeta_test.get(2);
        }

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
        runOnUiThread(this::onTestingTaskCompleted);

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
        int fileCount = 0, numFeatures = 0;
        String exportFile = "";
        fileCount = (int) swipemeta_test.get(0);
        numFeatures = (int) swipemeta_test.get(1);
        exportFile = (String) swipemeta_test.get(2);

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
        runOnUiThread(this::onTestingTaskCompleted);

        // Testing
        // Prepare test data
        String testfile = readFileInternal(context, exportFile);
        String[] testrows = testfile.split("\n");
        int rows = testrows.length;
        int cols = testrows[0].split(",").length;
        svm_node[][] testdata = new svm_node[rows][cols-1];
        //String[] labelsarray = new String[rows];
        for (int x = 0; x < rows; x++) {
            String[] testline = testrows[x].split(",");
            //labelsarray[x] = testline[0];
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
            if (val == 1/*labelsarray[k].charAt(0)*/) correct++;
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
}

