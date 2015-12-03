package com.example.flight.paracam.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by cuong on 12/3/15.
 */
public class DrawingView extends View {

    public Paint mPaint = new Paint();

    public float xRatio = 0.0f;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(5);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawLine(xRatio * canvas.getWidth(), 0, xRatio * canvas.getWidth(), canvas.getHeight(), mPaint);
    }

}
