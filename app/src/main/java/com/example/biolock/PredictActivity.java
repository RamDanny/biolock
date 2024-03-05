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
import java.util.ArrayList;
import java.util.List;

import libsvm.*;

public class PredictActivity extends Activity {
    private TextView predictOutput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        predictOutput = findViewById(R.id.predictOutput);

        //// Generate datasets from raw data
        // Read raw dataset csv
        Context context = getApplicationContext();
        String exportFile = "";
        for (int i = 1; i <= 51; i++) {
            double sum_acc_x = 0, sum_acc_y = 0, sum_acc_z = 0;
            double sum_squared_acc_x = 0, sum_squared_acc_y = 0, sum_squared_acc_z = 0;
            double sum_abs_diff_acc_x = 0, sum_abs_diff_acc_y = 0, sum_abs_diff_acc_z = 0;
            double sum_resultant_acceleration = 0;
            double prev_x = 0, prev_y = 0, prev_z = 0;
            int rowCount = 0;
            try {
                String fileName = "accdata/accdata(" + i + ").csv";
                BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
                String nextLine;
                while ((nextLine = br.readLine()) != null) {
                    String[] parts = nextLine.split(",");
                    double acc_x = Double.parseDouble(parts[1]);
                    double acc_y = Double.parseDouble(parts[2]);
                    double acc_z = Double.parseDouble(parts[3]);

                    sum_acc_x += acc_x;
                    sum_acc_y += acc_y;
                    sum_acc_z += acc_z;

                    sum_squared_acc_x += Math.pow(acc_x, 2);
                    sum_squared_acc_y += Math.pow(acc_y, 2);
                    sum_squared_acc_z += Math.pow(acc_z, 2);

                    if (rowCount > 0) {
                        sum_abs_diff_acc_x += Math.abs(acc_x - prev_x);
                        sum_abs_diff_acc_y += Math.abs(acc_y - prev_y);
                        sum_abs_diff_acc_z += Math.abs(acc_z - prev_z);
                    } else {
                        prev_x = acc_x;
                        prev_y = acc_y;
                        prev_z = acc_z;
                    }

                    sum_resultant_acceleration += Math.sqrt(Math.pow(acc_x, 2) + Math.pow(acc_y, 2) + Math.pow(acc_z, 2));

                    rowCount++;
                }

                double avg_acc_x = sum_acc_x / rowCount;
                double avg_acc_y = sum_acc_y / rowCount;
                double avg_acc_z = sum_acc_z / rowCount;

                double std_dev_acc_x = Math.sqrt((sum_squared_acc_x / rowCount) - Math.pow(avg_acc_x, 2));
                double std_dev_acc_y = Math.sqrt((sum_squared_acc_y / rowCount) - Math.pow(avg_acc_y, 2));
                double std_dev_acc_z = Math.sqrt((sum_squared_acc_z / rowCount) - Math.pow(avg_acc_z, 2));

                double avg_abs_diff_acc_x = sum_abs_diff_acc_x / (rowCount - 1);
                double avg_abs_diff_acc_y = sum_abs_diff_acc_y / (rowCount - 1);
                double avg_abs_diff_acc_z = sum_abs_diff_acc_z / (rowCount - 1);

                double avg_resultant_acceleration = sum_resultant_acceleration / rowCount;

                // Append calculated values to a single CSV file
                String calculatedValues = avg_acc_x + "," + avg_acc_y + "," + avg_acc_z + "," +
                        std_dev_acc_x + "," + std_dev_acc_y + "," + std_dev_acc_z + "," +
                        avg_abs_diff_acc_x + "," + avg_abs_diff_acc_y + "," + avg_abs_diff_acc_z + "," +
                        avg_resultant_acceleration + "\n";
                //BufferedWriter writer;
                exportFile = "calculated_values.csv";
                if (i == 1) {
                    writeFileInternal(context, exportFile, calculatedValues, false);
                } else {
                    writeFileInternal(context, exportFile, calculatedValues, true);
                }
                System.out.println(calculatedValues);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //// Train and Test model from Dataset
        // Read dataset csv
        String csvFile = exportFile;
        // number of training instances, 2d array of svm nodes, array of labels
        int numTrainingInstances = 51;
        int numFeatures = 10;
        svm_node[][] trainingData = new svm_node[numTrainingInstances][numFeatures];
        double[] labelsArray = new double[numTrainingInstances];

        // Parse file to extract label, features
        String filedata = readFileInternal(context, exportFile);
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
        param.gamma = 0.000025;
        param.nu = 0.1;
        //param.degree = 3;
        //param.coef0 = 4.0;
        System.out.println("Gamma = " + String.valueOf(param.gamma));

        // Train the model
        svm_model model = svm.svm_train(prob, param);

        // Prepare test data
        int rows = 51;
        int cols = 10;
        svm_node[][] testdata = new svm_node[rows][cols];
        String csvFile2 = "calculated_values_5.csv";
        try {
            BufferedReader br2 = new BufferedReader(new InputStreamReader(context.getAssets().open(csvFile2)));
            String line2;
            int k = 0;
            while ((line2 = br2.readLine()) != null) {
                String[] parts2 = line2.split(",");
                for (int j = 0; j < parts2.length; j++) {
                    testdata[k][j] = new svm_node();
                    testdata[k][j].index = j + 1;
                    testdata[k][j].value = Double.parseDouble(parts2[j]);
                }
                k++;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
        if (accuracy > 70) {
            predictOutput.setText("VALID");
            predictOutput.setTextColor(Color.GREEN);
        }
        else {
            predictOutput.setText("NOT VALID");
            predictOutput.setTextColor(Color.RED);
        }
    }

    public void writeFileInternal(Context context, String fileName, String data, boolean append) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, append ? Context.MODE_APPEND : Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String readFileInternal(Context context, String fileName) {
        FileInputStream fis = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            fis = context.openFileInput(fileName);
            int character;
            while ((character = fis.read()) != -1) {
                stringBuilder.append((char) character);
            }
            fis.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
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

