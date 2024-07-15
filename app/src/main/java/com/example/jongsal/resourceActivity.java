package com.example.jongsal;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class resourceActivity extends AppCompatActivity {

    private TextView tvHeartRate, tvElapsedTime, tvCompressionCount;
    private View statusIndicator;
    private GraphView heartRateGraph;
    private LineGraphSeries<DataPoint> ecgSeries;
    private int heartRate = 0;
    private int elapsedTime = 0;
    private int CompressionCount = 0;
    private int lastXValue = 0;
    private long startTime = 0;
    private boolean firstDataReceived = false;

    private Handler handler = new Handler();
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            handler.postDelayed(this, 500); // 주기를 500ms로 조정하여 더 자주 업데이트
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resource);

        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        statusIndicator = findViewById(R.id.statusIndicator);
        heartRateGraph = findViewById(R.id.heartRateGraph);

        ecgSeries = new LineGraphSeries<>();
        heartRateGraph.addSeries(ecgSeries);

        // GraphView 초기 설정
        heartRateGraph.getViewport().setXAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinX(0);
        heartRateGraph.getViewport().setMaxX(40); // X축 범위 설정
        heartRateGraph.getViewport().setYAxisBoundsManual(true);
        heartRateGraph.getViewport().setMinY(50);
        heartRateGraph.getViewport().setMaxY(200); // Y축 범위 설정 (ECG 데이터 범위에 맞게 설정)

        registerReceiver(mReceiver, new IntentFilter(BluetoothService.ACTION_DATA_AVAILABLE));
        Log.d("ResourceActivity", "BroadcastReceiver registered"); // 등록 확인 로그

        handler.post(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        handler.removeCallbacks(updateRunnable);
        Log.d("ResourceActivity", "BroadcastReceiver unregistered"); // 해제 확인 로그
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothService.EXTRA_DATA);
                Log.d("ResourceActivity", "Received data: " + data);
                String[] parts = data.split(",");
                if (parts.length == 3) {
                    try {
                        int newHeartRate = Integer.parseInt(parts[0].trim());
                        int newElapsedTime = Integer.parseInt(parts[1].trim());
                        int newCompressionCount = Integer.parseInt(parts[2].trim());
                        if (!firstDataReceived) {
                            startTime = System.currentTimeMillis();
                            firstDataReceived = true;
                        }

                        elapsedTime = newElapsedTime;
                        
                        // 숫자 애니메이션 추가
                        animateTextViewChange(tvHeartRate, heartRate, newHeartRate);
                        heartRate = newHeartRate;

                        animate2TextViewChange(tvCompressionCount, CompressionCount, newCompressionCount);
                        CompressionCount = newCompressionCount;


                        // 그래프 업데이트 애니메이션 추가
                        ecgSeries.appendData(new DataPoint(lastXValue++, newHeartRate), true, 40);

                        updateUI();
                    } catch (NumberFormatException e) {
                        Log.e("ResourceActivity", "Error parsing data: " + data, e);
                    }
                } else {
                    Log.e("ResourceActivity", "Incorrect data format: " + data);
                }
            } else {
                Log.e("ResourceActivity", "Received null data");
            }
        }
    };

    private void animateTextViewChange(final TextView textView, int oldValue, int newValue) {
        ValueAnimator animator = ValueAnimator.ofInt(oldValue, newValue);
        animator.setDuration(500); // 애니메이션 지속 시간 (500ms)
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                textView.setText(animation.getAnimatedValue().toString() + " bpm");
            }
        });
        animator.start();
    }
    private void animate2TextViewChange(final TextView textView, int oldValue, int newValue) {
        ValueAnimator animator = ValueAnimator.ofInt(oldValue, newValue);
        animator.setDuration(500); // 애니메이션 지속 시간 (500ms)
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                textView.setText(animation.getAnimatedValue().toString() + "회");
            }
        });
        animator.start();
    }

    private void updateUI() {
        // 받은 데이터를 로그로 출력하여 확인
        Log.d("ResourceActivity", "Updating UI - Heart Rate: " + heartRate + ", Elapsed Time: " + elapsedTime+",CompressionCount:" + CompressionCount);

        tvElapsedTime.setText(elapsedTime + " sec");
        tvCompressionCount.setText(String.valueOf(CompressionCount));

        if (heartRate >= 60 && heartRate <= 120) {
            statusIndicator.setBackground(getResources().getDrawable(R.drawable.green_circle));
        } else {
            statusIndicator.setBackground(getResources().getDrawable(R.drawable.red_circle));
        }

        // 그래프 애니메이션 설정
        heartRateGraph.getGridLabelRenderer().setHighlightZeroLines(false);
        heartRateGraph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        heartRateGraph.getViewport().setScalableY(true);
        heartRateGraph.getViewport().setScalable(true);
        heartRateGraph.getGridLabelRenderer().setHorizontalLabelsAngle(45);
        heartRateGraph.getViewport().setBackgroundColor(Color.argb(25, 255, 0, 0));
        heartRateGraph.getViewport().setDrawBorder(true);
    }
}