package com.example.biolock;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends Activity implements SensorEventListener, View.OnTouchListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private TextView accelerometerValues;
    private TextView gyroscopeValues;
    private TextView swipeInfo;
    private Button startButton;
    public static boolean accmeter = false;
    public static boolean gyrmeter = false;
    public static boolean touched = false;
    private float xTouchStart;
    private float yTouchStart;
    private float xTouchEnd;
    private float yTouchEnd;
    public ArrayList accvals;
    public ArrayList gyrovals;
    public ArrayList swipevals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometerValues = findViewById(R.id.accelerometerValues);
        gyroscopeValues = findViewById(R.id.gyroscopeValues);
        swipeInfo = findViewById(R.id.swipeInfo);
        accvals = new ArrayList(3);
        accvals.add(0.0);
        accvals.add(0.0);
        accvals.add(0.0);
        gyrovals = new ArrayList(3);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        gyrovals.add(0.0);
        swipevals = new ArrayList(5);
        swipevals.add("None");
        swipevals.add(0.0);
        swipevals.add(0.0);
        swipevals.add(0.0);
        swipevals.add(0.0);

        // Initialize sensor manager, accelerometer, and gyroscope sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        findViewById(android.R.id.content).setOnTouchListener(this);

        // Check if the sensors are available
        if (accelerometer == null) {
            accelerometerValues.setText("Accelerometer not available on this device.");
        }

        if (gyroscope == null) {
            gyroscopeValues.setText("Gyroscope not available on this device.");
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
            String valuesText = "Accelerometer Values: \nX: " + x + "\nY: " + y + "\nZ: " + z;
            accelerometerValues.setText(valuesText);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && gyrmeter) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            gyrovals.set(0, x);
            gyrovals.set(1, y);
            gyrovals.set(2, z);
            String valuesText = "Gyroscope Values: \nX: " + x + "\nY: " + y + "\nZ: " + z;
            gyroscopeValues.setText(valuesText);
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
            System.out.println(String.valueOf(xTouchStart) + " " + yTouchStart);
        }
        if (event.getAction() == MotionEvent.ACTION_UP && touched) {
            xTouchEnd = event.getX();
            yTouchEnd = event.getY();
            detectSwipeDirection(xTouchStart, yTouchStart, xTouchEnd, yTouchEnd);
        }
        return true;
    }

    private void detectSwipeDirection(float xStart, float yStart, float xEnd, float yEnd) {
        float deltaX = xEnd - xStart;
        float deltaY = yEnd - yStart;
        float minSwipeDistance = 50;

        String direction = "";
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Horizontal swipe
            if (Math.abs(deltaX) > minSwipeDistance) {
                direction = (deltaX > 0) ? "Right" : "Left";
            }
        } else {
            // Vertical swipe
            if (Math.abs(deltaY) > minSwipeDistance) {
                direction = (deltaY > 0) ? "Down" : "Up";
            }
        }

        // Insert into database
        swipevals.set(0, direction);
        swipevals.set(1, xStart);
        swipevals.set(2, yStart);
        swipevals.set(3, xEnd);
        swipevals.set(4, yEnd);
        DatabaseManager db = new DatabaseManager(getApplicationContext());
        Boolean accInsert = db.insert_acc(String.valueOf(accvals.get(0)), String.valueOf(accvals.get(1)), String.valueOf(accvals.get(2)));
        Boolean gyroInsert = db.insert_gyro(String.valueOf(gyrovals.get(0)), String.valueOf(gyrovals.get(1)), String.valueOf(gyrovals.get(2)));
        Boolean swipeInsert = db.insert_swipe((String) swipevals.get(0), String.valueOf(swipevals.get(1)), String.valueOf(swipevals.get(2)), String.valueOf(swipevals.get(3)), String.valueOf(swipevals.get(4)));

        // Display detailed swipe information
        String swipeInfoText = "Swipe Info: \nDirection: " + direction +
                "\nStart X: " + xStart + "\nStart Y: " + yStart +
                "\nEnd X: " + xEnd + "\nEnd Y: " + yEnd;
        swipeInfo.setText(swipeInfoText);
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

    public void stopLogging(View view) {
        // Called when the "Stop Logging" button is pressed
        accmeter = false;
        gyrmeter = false;
        touched = false;

        System.out.println("Logging stopped!");
    }

    public void viewDb(View view) {
        Intent i = new Intent(MainActivity.this, ViewData.class);
        startActivity(i);
    }
}
