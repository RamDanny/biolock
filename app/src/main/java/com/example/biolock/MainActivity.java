package com.example.biolock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void trainButtonHome(View view) {
        Intent i = new Intent(MainActivity.this, TrainActivity.class);
        startActivity(i);
    }

    public void testButtonHome(View view) {
        Intent i = new Intent(MainActivity.this, TestActivity.class);
        startActivity(i);
    }
}