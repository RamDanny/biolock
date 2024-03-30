package com.example.biolock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.*;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class TrainModel extends Activity {
    private TextView trainText;
    private CountDownLatch latch;
    private ArrayList accmeta, gyrometa, magmeta, swipemeta;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainmodel);

        trainText = findViewById(R.id.trainText);

        accmeta = new ArrayList<>(3);
        gyrometa = new ArrayList<>(3);
        magmeta = new ArrayList<>(3);
        swipemeta = new ArrayList<>(3);

        Intent i = getIntent();
        accmeta.add(i.getIntExtra("accmeta_filecount", 0));
        accmeta.add(i.getIntExtra("accmeta_numfeatures", 0));
        accmeta.add(i.getStringExtra("accmeta_exportfile"));
        gyrometa.add(i.getIntExtra("gyrometa_filecount", 0));
        gyrometa.add(i.getIntExtra("gyrometa_numfeatures", 0));
        gyrometa.add(i.getStringExtra("gyrometa_exportfile"));
        magmeta.add(i.getIntExtra("magmeta_filecount", 0));
        magmeta.add(i.getIntExtra("magmeta_numfeatures", 0));
        magmeta.add(i.getStringExtra("magmeta_exportfile"));
        swipemeta.add(i.getIntExtra("swipemeta_filecount", 0));
        swipemeta.add(i.getIntExtra("swipemeta_numfeatures", 0));
        swipemeta.add(i.getStringExtra("swipemeta_exportfile"));

        //overrideFromDownloads(getApplicationContext());
        runTrainingTasks();
    }

    private void runTrainingTasks() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        latch = new CountDownLatch(4);

        executorService.submit(() -> {
            trainModel(getApplicationContext(), "accdata");
            latch.countDown();
            runOnUiThread(this::onTrainingTaskCompleted);
        });

        executorService.submit(() -> {
            trainModel(getApplicationContext(), "gyrodata");
            latch.countDown();
            runOnUiThread(this::onTrainingTaskCompleted);
        });

        executorService.submit(() -> {
            trainModel(getApplicationContext(), "magdata");
            latch.countDown();
            runOnUiThread(this::onTrainingTaskCompleted);
        });

        executorService.submit(() -> {
            trainSwipe(getApplicationContext());
            latch.countDown();
            runOnUiThread(this::onTrainingTaskCompleted);
        });

        executorService.shutdown();
    }

    private void onTrainingTaskCompleted() {
        long ct = latch.getCount();
        if (ct > 0) {
            trainText.setText("Training...\nPlease wait...\n" + (4 - ct) + "/4");
        }
        else {
            trainText.setText("Training Completed!");
            Log.d("TrainingResult", "Training successful!");
        }
    }

    public void trainModel(Context context, String sensor) {
        // Training
        int fileCount = 0, numFeatures = 0;
        String exportFile = "";
        if (sensor.equals("accdata")) {
            fileCount = (int) accmeta.get(0);
            numFeatures = (int) accmeta.get(1);
            exportFile = (String) accmeta.get(2);
        }
        else if (sensor.equals("gyrodata")) {
            fileCount = (int) gyrometa.get(0);
            numFeatures = (int) gyrometa.get(1);
            exportFile = (String) gyrometa.get(2);
        }
        else {
            fileCount = (int) magmeta.get(0);
            numFeatures = (int) magmeta.get(1);
            exportFile = (String) magmeta.get(2);
        }
        // Read dataset csv
        // number of training instances, 2d array of svm nodes, array of labels
        int numTrainingInstances = fileCount;
        svm_node[][] trainingData = new svm_node[numTrainingInstances][numFeatures];
        double[] labelsArray = new double[numTrainingInstances];

        // Parse file to extract label, features
        String filedata = readFileInternal(context, exportFile);
        //System.out.println("Dataset File:::" + filedata);
        String[] lines = filedata.split("\n");
        int i = 0;
        for (String line : lines) {
            String[] parts = line.split(",");
            for (int j = 0; j < numFeatures; j++) {
                trainingData[i][j] = new svm_node();
                trainingData[i][j].index = j + 1;
                trainingData[i][j].value = Double.parseDouble(parts[j]);
            }
            System.out.println();
            labelsArray[i] = 1;
            i++;
        }

        runOnUiThread(this::onTrainingTaskCompleted);

        // Prepare training data
        svm_problem prob = new svm_problem();
        prob.l = numTrainingInstances; // Number of training instances
        prob.x = trainingData; // Array of svm_node arrays representing training instances
        prob.y = labelsArray; // Array of class labels for training instances

        // Set SVM parameters
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.RBF;
        param.C = 1.56; // Penalty parameter C of the error term
        param.gamma = 0.0000001;
        param.nu = 0.01;
        //param.degree = 3;
        //param.coef0 = 4.0;
        System.out.println("Gamma = " + param.gamma);

        // Train the model
        svm_model model = svm.svm_train(prob, param);

        // Export the model
        try {
            File file = new File(context.getFilesDir(), sensor+"_model.csv");
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            svm.svm_save_model(file.getAbsolutePath(), model);
            Log.d("ModelExport", "Exported model  " + sensor + "_model.csv  successfully!");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trainSwipe(Context context) {
        // Training
        String sensor = "swipedata";
        int fileCount = 0, numFeatures = 0;
        String exportFile = "";
        fileCount = (int) swipemeta.get(0);
        numFeatures = (int) swipemeta.get(1);
        exportFile = (String) swipemeta.get(2);
        // Read dataset csv
        // number of training instances, 2d array of svm nodes, array of labels
        int numTrainingInstances = fileCount;
        svm_node[][] trainingData = new svm_node[numTrainingInstances][numFeatures];
        double[] labelsArray = new double[numTrainingInstances];
        //exportFile = "swipedata_calc.csv";
        // Parse file to extract label, features
        String filedata = readFileInternal(context, exportFile);
        //System.out.println("Dataset File:::" + filedata);
        String[] lines = filedata.split("\n");
        int i = 0;
        for (String line : lines) {
            String[] parts = line.split(",");
            for (int j = 0; j < numFeatures; j++) {
                trainingData[i][j] = new svm_node();
                trainingData[i][j].index = j+1;
                trainingData[i][j].value = Double.parseDouble(parts[j+1]);
            }
            System.out.println();
            labelsArray[i] = 1;//parts[0].charAt(0);
            i++;
        }

        // Prepare training data
        svm_problem prob = new svm_problem();
        prob.l = numTrainingInstances; // Number of training instances
        prob.x = trainingData; // Array of svm_node arrays representing training instances
        prob.y = labelsArray; // Array of class labels for training instances

        // Set SVM parameters
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.RBF;
        //param.C = 1.56; // Penalty parameter C of the error term
        param.gamma = 0.0000001;
        //param.nr_weight = 0;
        param.nu = 0.01;
        //param.degree = 3;
        //param.coef0 = 4.0;
        System.out.println("Gamma = " + String.valueOf(param.gamma));

        // Train the model
        svm_model model = svm.svm_train(prob, param);

        // Export the model
        try {
            File file = new File(context.getFilesDir(), sensor+"_model.csv");
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            svm.svm_save_model(file.getAbsolutePath(), model);
            Log.d("ModelExport", "Exported model  " + sensor + "_model.csv  successfully!");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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

