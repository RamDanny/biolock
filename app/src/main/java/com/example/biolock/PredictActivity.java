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

public class PredictActivity extends Activity {
    private TextView predictOutput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        predictOutput = findViewById(R.id.predictOutput);

        double acc_accuracy = predictData(getApplicationContext(), "accdata");
        double gyro_accuracy = predictData(getApplicationContext(), "gyrodata");
        double mag_accuracy = predictData(getApplicationContext(), "magdata");
        System.out.println("Acc = " + String.valueOf(acc_accuracy));
        System.out.println("Gyro = " + String.valueOf(gyro_accuracy));
        System.out.println("Mag = " + String.valueOf(mag_accuracy));

        if (acc_accuracy > 70 && gyro_accuracy > 70 && mag_accuracy > 70) {
            predictOutput.setText("VALID");
            predictOutput.setTextColor(Color.GREEN);
        }
        else {
            predictOutput.setText("NOT VALID");
            predictOutput.setTextColor(Color.RED);
        }

    }

    public double predictData(Context context, String sensor) {
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
            else if (diffInSecs(getDateTime(parts[4]), getDateTime(filerows[i-1].split(",")[4])) < 6) {
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", true);
            }
            else {
                fileCount++;
                writeFileInternal(getApplicationContext(), sensor+"("+fileCount+").csv", filerows[i]+"\n", false);
            }
            //System.out.println(sensor+"("+fileCount+") = " + filerows[i]);
        }
        System.out.println(sensor+": "+fileCount+" files written");

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

        // Export to external filesystem
        copyToDownloads(context, sensor+".csv");
        copyToDownloads(context, sensor+"_calc.csv");


        // Train, test, predict
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

        /*ArrayList al = new ArrayList<>(3);
        al.add(0, exportFile);
        al.add(1, fileCount);
        al.add(2, numFeatures);
        return al;*/
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

