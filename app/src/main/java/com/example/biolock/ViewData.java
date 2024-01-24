package com.example.biolock;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ViewData extends Activity {
    private TextView dataAcc, dataGyro, dataSwipe, countDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewdata);

        dataAcc = findViewById(R.id.dataAcc);
        dataGyro = findViewById(R.id.dataGyro);
        dataSwipe = findViewById(R.id.dataSwipe);
        countDb = findViewById(R.id.countDb);

        DatabaseManager db = new DatabaseManager(this);
        Cursor acc = db.get_acc();
        Cursor gyro = db.get_gyro();
        Cursor swipe = db.get_swipe();
        while (acc.moveToNext()) {
            String temp = dataAcc.getText().toString();
            String id = String.valueOf(acc.getInt(0));
            String x = acc.getString(1);
            String y = acc.getString(2);
            String z = acc.getString(3);
            dataAcc.setText(temp + x + " " + y + " " + z + "\n");
        }
        while (gyro.moveToNext()) {
            String temp = dataGyro.getText().toString();
            String id = String.valueOf(gyro.getInt(0));
            String gx = gyro.getString(1);
            String gy = gyro.getString(2);
            String gz = gyro.getString(3);
            dataGyro.setText(temp + gx + " " + gy + " " + gz + "\n");
        }
        while (swipe.moveToNext()) {
            String temp = dataSwipe.getText().toString();
            String id = String.valueOf(swipe.getInt(0));
            String direction = swipe.getString(1);
            String sx = swipe.getString(2);
            String sy = swipe.getString(3);
            String ex = swipe.getString(4);
            String ey = swipe.getString(5);
            dataSwipe.setText(temp + direction + " " + sx + " " + sy + " " + ex + " " + ey+ "\n");
        }
        countDb.setText(String.valueOf(db.getRowCount()));
    }

    public void backHome(View view) {
        Intent i = new Intent(ViewData.this, MainActivity.class);
        startActivity(i);
    }
}
