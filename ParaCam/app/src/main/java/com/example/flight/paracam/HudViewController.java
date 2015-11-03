/*
 * HudViewController
 *
 *  Created on: July 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.example.flight.paracam;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.gestures.EnhancedGestureDetector;
import com.parrot.freeflight.ui.hud.Sprite;
import com.parrot.freeflight.ui.hud.Sprite.Align;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.parrot.freeflight.video.VideoStageView;

public class HudViewController
{
    public enum JoystickType {
        NONE,
        ANALOGUE,
        ACCELERO,
        COMBINED,
        MAGNETO
    }

    private static final String TAG = "HudViewController";

    private static final int JOY_ID_LEFT = 1;
    private static final int JOY_ID_RIGHT = 2;
    private static final int ALERT_ID = 3;
    private static final int TAKE_OFF_ID = 4;
    private static final int TOP_BAR_ID = 5;
    private static final int BOTTOM_BAR_ID = 6;
    private static final int CAMERA_ID = 7;
    private static final int RECORD_ID = 8;
    private static final int PHOTO_ID = 9;
    private static final int SETTINGS_ID = 10;
    private static final int BATTERY_INDICATOR_ID = 11;
    private static final int WIFI_INDICATOR_ID = 12;
    private static final int EMERGENCY_LABEL_ID = 13;
    private static final int BATTERY_STATUS_LABEL_ID = 14;
    private static final int RECORD_LABEL_ID = 15;
    private static final int USB_INDICATOR_ID = 16;
    private static final int USB_INDICATOR_TEXT_ID = 17;
    private static final int BACK_BTN_ID = 18;
    private static final int LAND_ID = 19;


    private GLSurfaceView glView;
    private VideoStageView canvasView;


    private VideoStageRenderer renderer;
    private Activity context;

    private boolean useSoftwareRendering;
    private int prevRemainingTime;

    private SparseIntArray emergencyStringMap;

    public HudViewController(Activity context, boolean useSoftwareRendering)
    {
        this.context = context;
        this.useSoftwareRendering = useSoftwareRendering;

        canvasView = null;


        glView = new GLSurfaceView(context);
        glView.setEGLContextClientVersion(2);

        renderer = new VideoStageRenderer(context, null);

        if (useSoftwareRendering){
            // Replacing OpneGl based view with Canvas based one
//			RelativeLayout root = (RelativeLayout) context.findViewById(R.id.controllerRootLayout);
//			root.removeView(glView);
            glView = null;

            canvasView = new VideoStageView(context);
            canvasView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//			root.addView(canvasView, 0);
        }

        initCanvasSurfaceView();
        initGLSurfaceView();

        Resources res = context.getResources();


    }

    private void initCanvasSurfaceView()
    {
        if (canvasView != null) {
            canvasView.setRenderer(renderer);
        }
    }


    private void initGLSurfaceView() {
        if (glView != null) {
            glView.setRenderer(renderer);
        }
    }


    public void onPause()
    {
        if (glView != null) {
            glView.onPause();
        }

        if (canvasView != null) {
            canvasView.onStop();
        }
    }


    public void onResume()
    {
        if (glView != null) {
            glView.onResume();
        }

        if (canvasView != null) {
            canvasView.onStart();
        }
    }


    public void onDestroy()
    {
        renderer.clearSprites();

        if (canvasView != null) {
            canvasView.onStop();
        }
    }


    public boolean onDown(MotionEvent e)
    {
        return false;
    }


    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY)
    {
        return false;
    }


    public void onLongPress(MotionEvent e)
    {
        // Left unimplemented
    }


    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY)
    {
        return false;
    }


    public void onShowPress(MotionEvent e)
    {
        // Left unimplemented
    }


    public boolean onSingleTapUp(MotionEvent e)
    {
        return false;
    }


    public View getRootView()
    {
        if (glView != null) {
            return glView;
        } else if (canvasView != null) {
            return canvasView;
        }

        Log.w(TAG, "Can't find root view");
        return null;
    }
}
