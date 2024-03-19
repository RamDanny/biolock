package com.example.biolock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class TestActivitySwipe extends Activity implements View.OnTouchListener {


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        userPrompt = findViewById(R.id.userPromptTest);

        randPrompt();

        swipepath = new ArrayList<float[]>();
        lines = new ArrayList<float[]>();
        pressures = new ArrayList<>();
        velocities = new ArrayList<>();
        touchareas = new ArrayList<>();

        // Initialize sensor manager, accelerometer, and gyroscope sensors

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
        Boolean swipeInsert = db.insert_swipe(lines, pressures, velocities, touchareas, promptLetter, false);
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
        Intent i = new Intent(TestActivitySwipe.this, ViewData.class);
        i.putExtra("train_mode", false);
        startActivity(i);
    }

    public void testButton(View view) {
        Intent i = new Intent(TestActivitySwipe.this, TestModel.class);
        startActivity(i);
    }
}
