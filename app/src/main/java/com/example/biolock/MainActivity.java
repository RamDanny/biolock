package com.example.biolock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void trainButtonAgm(View view) {
        Intent i = new Intent(MainActivity.this, TrainActivityAgm.class);
        startActivity(i);
    }

    public void trainButtonSwipe (View view) {
        Intent i = new Intent(MainActivity.this, TrainActivitySwipe.class);
        startActivity(i);
    }
    public void testbuttonagm(View view) {
        Intent i = new Intent(MainActivity.this, TestActivityAgm.class);
        startActivity(i);
    }

    public void testbuttonswipe (View view) {
        Intent i = new Intent(MainActivity.this, TestActivitySwipe.class);
        startActivity(i);
    }



}