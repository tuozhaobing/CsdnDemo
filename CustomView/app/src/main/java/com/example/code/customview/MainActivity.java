package com.example.code.customview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    RingCircleView arcPointLoadingView = findViewById(R.id.four_arc_view);
    arcPointLoadingView.startAnimation();
  }
}
