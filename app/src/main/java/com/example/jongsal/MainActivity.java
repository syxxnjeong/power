package com.example.jongsal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ImageView에 애니메이션 적용
        ImageView heartImageView = findViewById(R.id.imageView2);
        Animation moveAnimation = AnimationUtils.loadAnimation(this, R.anim.move_heart);
        heartImageView.startAnimation(moveAnimation);

        // TextView에 동일한 애니메이션 적용
        TextView pulseCheckTextView = findViewById(R.id.pulse_check_textview);
        pulseCheckTextView.startAnimation(moveAnimation);
    }

    public void onImageClick(View view) {
        Intent intent = new Intent(this, Main2Activity .class);
        startActivity(intent);
    }
}

