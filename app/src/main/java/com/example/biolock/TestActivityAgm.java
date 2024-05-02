package com.example.biolock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class TestActivityAgm extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private TextView userPrompt;
    public static boolean accmeter = false;
    public static boolean gyrmeter = false;
    public static boolean magmeter = false;

    private ArrayList accvals;
    private ArrayList gyrovals;
    private ArrayList magvals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_agm);

        userPrompt = findViewById(R.id.userPromptTest);

        accvals = new ArrayList(3);
        accvals.add(0.0);
        accvals.add(0.0);
        accvals.add(0.0);
        gyrovals = new ArrayList(3);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        magvals = new ArrayList(3);
        magvals.add(0.0);
        magvals.add(0.0);
        magvals.add(0.0);

        // Initialize sensor manager, accelerometer, and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }



        // Check if the sensors are available
        if (accelerometer == null) {
            //accelerometerValues.setText("Accelerometer not available on this device.");
        }

        if (gyroscope == null) {
            //gyroscopeValues.setText("Gyroscope not available on this device.");
        }

        if (magnetometer == null) {
            //
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listeners when the activity is resumed
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listeners when the activity is paused
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // When sensors detect change update the display
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && accmeter) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accvals.set(0, x);
            accvals.set(1, y);
            accvals.set(2, z);

            DatabaseManager db = new DatabaseManager(getApplicationContext());
            Boolean accInsert = db.insert_acc(String.valueOf(accvals.get(0)), String.valueOf(accvals.get(1)), String.valueOf(accvals.get(2)), false);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && gyrmeter) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            gyrovals.set(0, x);
            gyrovals.set(1, y);
            gyrovals.set(2, z);

            DatabaseManager db = new DatabaseManager(getApplicationContext());
            Boolean gyroInsert = db.insert_gyro(String.valueOf(gyrovals.get(0)), String.valueOf(gyrovals.get(1)), String.valueOf(gyrovals.get(2)), false);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && magmeter) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            magvals.set(0, x);
            magvals.set(1, y);
            magvals.set(2, z);

            DatabaseManager db = new DatabaseManager(getApplicationContext());
            Boolean magInsert = db.insert_mag(String.valueOf(magvals.get(0)), String.valueOf(magvals.get(1)), String.valueOf(magvals.get(2)), false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void startLogging(View view) {
        // Called when the "Start Logging" button is pressed
        accmeter = true;
        gyrmeter = true;
        magmeter = true;

        System.out.println("Logging started!");
    }

    public void nextPrompt(View view) {

    }

    public void stopLogging(View view) {
        // Called when the "Stop Logging" button is pressed
        accmeter = false;
        gyrmeter = false;
        magmeter = false;


        System.out.println("Logging stopped!");
    }

    public void viewDb(View view) {
        Intent i = new Intent(TestActivityAgm.this, ViewData.class);
        i.putExtra("train_mode", false);
        startActivity(i);
    }

    public void testButton(View view) {
        //Intent i = new Intent(TestActivityAgm.this, TestModel.class);
        //startActivity(i);
    }
}
