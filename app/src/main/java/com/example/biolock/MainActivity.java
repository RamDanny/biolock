package com.example.biolock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView accelerometerData;
    private Button startButton, stopButton;
    private boolean isRecording = false;
    private File file;
    private FileWriter fileWriter;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometerData = findViewById(R.id.accelerometerData);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        // Check if the accelerometer sensor is available
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            accelerometerData.setText("Accelerometer not available on this device.");
        }
    }

    private void startRecording() {
        isRecording = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        // Create a file to store accelerometer data
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        file = new File(root, "accelerometer_data.txt");

        try {
            fileWriter = new FileWriter(file);
            fileWriter.append("Timestamp,X,Y,Z\n"); // Header
        } catch (IOException e) {
            Log.e("MainActivity", "Error opening file for writing", e);
        }
    }

    private void stopRecording() {
        isRecording = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        // Close the FileWriter
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Error closing file", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float timestamp = event.timestamp;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Append accelerometer data to the file
            try {
                if (fileWriter != null) {
                    fileWriter.append(timestamp + "," + x + "," + y + "," + z + "\n");
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Error writing to file", e);
            }

            // Display accelerometer data in TextView
            String data = "X: " + x + "\nY: " + y + "\nZ: " + z;
            accelerometerData.setText("Accelerometer Data: \n" + data);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener to save battery
        sensorManager.unregisterListener(this);
        stopRecording(); // Stop recording if the app goes to the background
    }


}
