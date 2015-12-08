package com.example.flight.paracam;

import org.opencv.core.Rect;

/**
 * Created by cuong on 12/5/15.
 */
public class RelativeRect {
    public float left, top, width, height;

    public RelativeRect(float left, float top, float width, float height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public void configFromRect(Rect rect, int screenWidth, int screenHeight) {
        this.left = (float)rect.x / screenWidth;
        this.top = (float)rect.y / screenHeight;
        this.width = (float)rect.width / screenWidth;
        this.height = (float)rect.height / screenHeight;
    }

    public float getCenterX() { return left + width/2; }
    public float getCenterY() { return top + height/2; }

    public float getLeftX() { return left; }
    public float getRightX() { return left + width; }
    public float getTopY() { return top; }
    public float getBottomY() { return top + height; }
}
