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

import java.util.ArrayList;
import java.util.Random;

public class TrainActivitySwipe extends Activity implements SensorEventListener, View.OnTouchListener {

    private SensorManager sensorManager;
    private TextView userPrompt;

    public static boolean touched = false;
    private float xTouchStart;
    private float yTouchStart;
    private float xTouchEnd;
    private float yTouchEnd;
    private ArrayList<float[]> lines;
    private ArrayList<float[]> swipepath;
    private String promptLetter;
    private VelocityTracker velocityTracker;
    private ArrayList<Float> pressures;
    private ArrayList<float[]> velocities;
    private ArrayList<Float> touchareas;
    private ArrayList accmeta, gyrometa, magmeta, swipemeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_swipe);

        userPrompt = findViewById(R.id.userPromptTrain);

        accmeta = new ArrayList<>(3);
        gyrometa = new ArrayList<>(3);
        magmeta = new ArrayList<>(3);
        swipemeta = new ArrayList<>(3);

        Intent i = getIntent();
        if (i != null) {
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
        }

        randPrompt();
        swipepath = new ArrayList<float[]>();
        lines = new ArrayList<float[]>();
        pressures = new ArrayList<>();
        velocities = new ArrayList<>();
        touchareas = new ArrayList<>();

        // Initialize sensor manager, accelerometer, and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        findViewById(android.R.id.content).setOnTouchListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listeners when the activity is resumed

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
            touchareas.clear();
            float toucharea = event.getSize();
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE && touched) {
            float[] point = {event.getX(), event.getY()};
            swipepath.add(point);
            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(1000); // Compute velocity in pixels per second
            float pressureMove = event.getPressure();
            pressures.add(pressureMove);
            float velocityX = velocityTracker.getXVelocity();
            float velocityY = velocityTracker.getYVelocity();
            float[] vels = {velocityX, velocityY};
            velocities.add(vels);
            float toucharea = event.getSize();
            touchareas.add(toucharea);

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
        Boolean swipeInsert = db.insert_swipe(lines, pressures, velocities, touchareas, promptLetter, true);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void startLogging(View view) {
        // Called when the "Start Logging" button is pressed

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
            recordSwipe(lines);
            lines.clear();
            swipepath.clear();
        }
        randPrompt();
    }

    public void stopLogging(View view) {
        // Called when the "Stop Logging" button is pressed

        touched = false;

        if (lines.size() > 0) {
            recordSwipe(lines);
            lines.clear();
            swipepath.clear();
        }

        System.out.println("Logging stopped!");
    }

    public void viewDb(View view) {
        Intent i = new Intent(TrainActivitySwipe.this, ViewData.class);
        i.putExtra("train_mode", true);
        startActivity(i);
    }

    public void trainButton(View view) {
        Intent i = new Intent(TrainActivitySwipe.this, TrainModel.class);
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
}
