package com.example.jongsal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class HeartRateGraphView extends View {

    private Paint paint;
    private float[] heartRates;
    private int dataPoints = 50;  // 그래프에 표시할 데이터 포인트 수
    private float phase = 0;  // 그래프의 위아래 움직임을 조절하는 변수

    public HeartRateGraphView(Context context) {
        super(context);
        init();
    }

    public HeartRateGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeartRateGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        // 초기 심박수 데이터 설정
        heartRates = new float[dataPoints];
        generateHeartRates();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float spacing = width / (heartRates.length - 1);

        for (int i = 0; i < heartRates.length - 1; i++) {
            float startX = i * spacing;
            float startY = height / 2 - (heartRates[i] * height / 2) * (float) Math.sin(2 * Math.PI * (i + phase) / dataPoints);
            float stopX = (i + 1) * spacing;
            float stopY = height / 2 - (heartRates[i + 1] * height / 2) * (float) Math.sin(2 * Math.PI * (i + 1 + phase) / dataPoints);
            canvas.drawLine(startX, startY, stopX, stopY, paint);
        }
    }

    private void generateHeartRates() {
        for (int i = 0; i < heartRates.length; i++) {
            heartRates[i] = 0.5f + 0.4f * (float) Math.sin(2 * Math.PI * i / dataPoints);
        }
    }

    public void setHeartRates(float[] heartRates) {
        this.heartRates = heartRates;
        invalidate();
    }

    public void addHeartRate(float heartRate) {
        System.arraycopy(heartRates, 1, heartRates, 0, heartRates.length - 1);
        heartRates[heartRates.length - 1] = heartRate;
        invalidate();
    }

    public void updateGraph() {
        phase += 2.5;  // 그래프의 움직임 속도를 조절할 수 있습니다.
        invalidate();
    }
}
