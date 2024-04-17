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
import android.view.VelocityTracker;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

public class TestActivitySwipe extends Activity implements SensorEventListener, View.OnTouchListener {


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private TextView userPrompt;
    public static boolean accmeter = false;
    public static boolean gyrmeter = false;
    public static boolean magmeter = false;
    public static boolean touched = false;
    private float xTouchStart;
    private float yTouchStart;
    private float xTouchEnd;
    private float yTouchEnd;
    private ArrayList accvals;
    private ArrayList gyrovals;
    private ArrayList magvals;

    private ArrayList<float[]> lines;
    private ArrayList<float[]> swipepath;
    private ArrayList<Long> swipetimes;
    private String promptLetter;
    private VelocityTracker velocityTracker;
    private ArrayList<Float> pressures;
    private ArrayList<float[]> velocities;
    private ArrayList<float[]> axes;
    private ArrayList accmeta_test, gyrometa_test, magmeta_test, swipemeta_test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        userPrompt = findViewById(R.id.userPromptTest);
        accmeta_test = new ArrayList<>(3);
        gyrometa_test = new ArrayList<>(3);
        magmeta_test = new ArrayList<>(3);
        swipemeta_test = new ArrayList<>(3);

        Intent i = getIntent();
        if (i != null) {
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
        }

        randPrompt();

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

        swipepath = new ArrayList<float[]>();
        lines = new ArrayList<float[]>();
        swipetimes = new ArrayList<Long>();
        pressures = new ArrayList<>();
        velocities = new ArrayList<>();
        axes = new ArrayList<float[]>();

        // Initialize sensor manager, accelerometer, and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        findViewById(android.R.id.content).setOnTouchListener(this);


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
        // Update when sensors detect change
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


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Handle touch events
        if (event.getAction() == MotionEvent.ACTION_DOWN && touched) {
            xTouchStart = event.getX();
            yTouchStart = event.getY();
            System.out.println(xTouchStart + " " + yTouchStart);
            float[] point = {xTouchStart, yTouchStart};
            swipepath.add(point);
            swipetimes.add(Instant.now().toEpochMilli());
            velocities.clear();
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            else {
                velocityTracker.clear();
            }
            velocityTracker.addMovement(event);
            pressures.clear();
            float pressureDown = event.getPressure();
            axes.clear();
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE && touched) {
            float[] point = {event.getX(), event.getY()};
            swipepath.add(point);
            swipetimes.add(Instant.now().toEpochMilli());
            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(1000); // Compute velocity in pixels per second
            float pressureMove = event.getPressure();
            pressures.add(pressureMove);
            float velocityX = velocityTracker.getXVelocity();
            float velocityY = velocityTracker.getYVelocity();
            float[] vels = {velocityX, velocityY};
            velocities.add(vels);
            float major = event.getTouchMajor();
            float minor = event.getTouchMinor();
            float[] axis = {major, minor};
            axes.add(axis);

        }
        else if (event.getAction() == MotionEvent.ACTION_UP && touched) {
            xTouchEnd = event.getX();
            yTouchEnd = event.getY();
            float[] point = {xTouchEnd, yTouchEnd};
            swipepath.add(point);
            swipetimes.add(Instant.now().toEpochMilli());

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

    private void recordSwipe(ArrayList<float[]> lines, ArrayList<Long> swipetimes) {
        DatabaseManager db = new DatabaseManager(getApplicationContext());
        Boolean swipeInsert = db.insert_swipe(lines, swipetimes, pressures, velocities, axes, promptLetter, false);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void startLogging(View view) {
        // Called when the "Start Logging" button is pressed
        accmeter = true;
        gyrmeter = true;
        magmeter = true;
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
        if (lines.size() > 0) {
            recordSwipe(lines, swipetimes);
            lines.clear();
            swipepath.clear();
            swipetimes.clear();
        }
        randPrompt();
    }

    public void stopLogging(View view) {
        // Called when the "Stop Logging" button is pressed
        accmeter = false;
        gyrmeter = false;
        magmeter = false;
        touched = false;

        if (lines.size() > 0) {
            recordSwipe(lines, swipetimes);
            lines.clear();
            swipepath.clear();
            swipetimes.clear();
        }

        System.out.println("Logging stopped!");
    }

    public void viewDb(View view) {
        Intent i = new Intent(TestActivitySwipe.this, ViewData.class);
        i.putExtra("train_mode", false);
        startActivity(i);
    }

    public void testButton(View view) {
        Intent i = new Intent(TestActivitySwipe.this, TestModel.class);
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
