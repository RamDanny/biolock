package com.example.biolock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener, View.OnTouchListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private TextView accelerometerValues;
    private TextView gyroscopeValues;
    private TextView swipeInfo;
    private TextView userPrompt;
    public static boolean accmeter = false;
    public static boolean gyrmeter = false;
    public static boolean touched = false;
    private float xTouchStart;
    private float yTouchStart;
    private float xTouchEnd;
    private float yTouchEnd;
    private ArrayList accvals;
    private ArrayList gyrovals;
    private ArrayList<float[]> lines;
    private ArrayList<float[]> swipepath;
    private String promptLetter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*accelerometerValues = findViewById(R.id.accelerometerValues);
        gyroscopeValues = findViewById(R.id.gyroscopeValues);
        swipeInfo = findViewById(R.id.swipeInfo);*/
        userPrompt = findViewById(R.id.userPrompt);

        randPrompt();
        accvals = new ArrayList(3);
        accvals.add(0.0);
        accvals.add(0.0);
        accvals.add(0.0);
        gyrovals = new ArrayList(3);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        swipepath = new ArrayList<float[]>();
        lines = new ArrayList<float[]>();

        // Initialize sensor manager, accelerometer, and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        findViewById(android.R.id.content).setOnTouchListener(this);

        // Check if the sensors are available
        if (accelerometer == null) {
            //accelerometerValues.setText("Accelerometer not available on this device.");
        }

        if (gyroscope == null) {
            //gyroscopeValues.setText("Gyroscope not available on this device.");
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
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && gyrmeter) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            gyrovals.set(0, x);
            gyrovals.set(1, y);
            gyrovals.set(2, z);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Handle touch events
        if (event.getAction() == MotionEvent.ACTION_DOWN && touched) {
            xTouchStart = event.getX();
            yTouchStart = event.getY();
            System.out.println(xTouchStart + " " + yTouchStart);
            float[] point = {xTouchStart, yTouchStart};
            swipepath.add(point);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE && touched) {
            float[] point = {event.getX(), event.getY()};
            swipepath.add(point);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP && touched) {
            xTouchEnd = event.getX();
            yTouchEnd = event.getY();
            float[] point = {xTouchEnd, yTouchEnd};
            swipepath.add(point);

            for (int i = 0; i < swipepath.size()-1; i++) {
                float[] curr = swipepath.get(i);
                float[] next = swipepath.get(i+1);
                float[] segment = {curr[0], curr[1], next[0], next[1]};
                lines.add(segment);
            }
            swipepath.clear();
        }
        return true;
    }

    private void recordSwipe(ArrayList<float[]> lines) {
        DatabaseManager db = new DatabaseManager(getApplicationContext());
        Boolean accInsert = db.insert_acc(String.valueOf(accvals.get(0)), String.valueOf(accvals.get(1)), String.valueOf(accvals.get(2)));
        Boolean gyroInsert = db.insert_gyro(String.valueOf(gyrovals.get(0)), String.valueOf(gyrovals.get(1)), String.valueOf(gyrovals.get(2)));
        Boolean swipeInsert = db.insert_swipe(lines, promptLetter);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void startLogging(View view) {
        // Called when the "Start Logging" button is pressed
        accmeter = true;
        gyrmeter = true;
        touched = true;

        System.out.println("Logging started!");
    }

    public void randPrompt() {
        Random rand = new Random();
        int rand_int = rand.nextInt(35);
        if (rand_int < 26) {
            char c = (char)(rand_int + 65);
            userPrompt.setText("Draw Letter '" + ("" + c) + "'");
            promptLetter = "" + c;
        }
        else {
            userPrompt.setText("Draw Number '" + String.valueOf(rand_int-25) + "'");
            promptLetter = String.valueOf(rand_int-25);
        }
    }

    public void nextPrompt(View view) {
        recordSwipe(lines);
        lines.clear();
        swipepath.clear();
        randPrompt();
    }

    public void stopLogging(View view) {
        // Called when the "Stop Logging" button is pressed
        accmeter = false;
        gyrmeter = false;
        touched = false;

        recordSwipe(lines);
        lines.clear();
        swipepath.clear();

        System.out.println("Logging stopped!");
    }

    public void viewDb(View view) {
        Intent i = new Intent(MainActivity.this, ViewData.class);
        startActivity(i);
    }
}
