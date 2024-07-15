package com.example.jongsal;


import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    private HeartRateGraphView heartRateGraphView;
    private Handler handler = new Handler();

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
        // HeartRateGraphView 설정
        
        heartRateGraphView = findViewById(R.id.heartRateGraph);

        startUpdatingGraph();
    }
    private void startUpdatingGraph() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                heartRateGraphView.updateGraph();
                handler.postDelayed(this, 100); // 0.1초 간격으로 업데이트
            }
        }, 100);
    }


    public void onImageClick(View view) {
        Intent intent = new Intent(this, Main2Activity .class);
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // 액티비티가 파괴될 때 핸들러 콜백 제거
    }
}

