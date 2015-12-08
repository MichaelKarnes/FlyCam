package com.example.flight.paracam.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.flight.paracam.RelativeRect;

/**
 * Created by cuong on 12/3/15.
 */
public class DrawingView extends View {

    public Paint mPaint = new Paint();
    public Paint bluePaint = new Paint();

    public float xRatio = 0.0f;
    public RelativeRect mRect;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);

        bluePaint.setColor(Color.parseColor("#95a5a6"));
        bluePaint.setStrokeWidth(3);

    }

    @Override
    public void onDraw(Canvas canvas) {
        // draw middle line
        canvas.drawLine(canvas.getWidth()/2, 0, canvas.getWidth()/2, canvas.getHeight(), bluePaint);

        canvas.drawLine(xRatio * canvas.getWidth(), 0, xRatio * canvas.getWidth(), canvas.getHeight(), mPaint);

        if(mRect != null) {
            float left = mRect.getLeftX() * canvas.getWidth();
            float top = mRect.getTopY() * canvas.getHeight();
            float right = mRect.getRightX() * canvas.getWidth();
            float bottom = mRect.getBottomY() * canvas.getHeight();

            canvas.drawRect(left, top, right, bottom, mPaint);

        }
    }

}
