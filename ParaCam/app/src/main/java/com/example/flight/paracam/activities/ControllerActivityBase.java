package com.example.flight.paracam.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.content.res.Resources;
import android.graphics.Color;
import android.nfc.Tag;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.view.ViewGroup.LayoutParams;

import com.example.flight.paracam.HudViewController;
import com.example.flight.paracam.R;
import com.example.flight.paracam.ui.DrawingView;
import com.example.flight.paracam.ui.DroneCameraView;
import com.example.flight.paracam.ui.DroneVideoView;
import com.example.flight.paracam.ui.JoystickView;
import com.parrot.freeflight.ui.gl.GLBGVideoSprite;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.parrot.freeflight.video.VideoStageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import javax.microedition.khronos.opengles.GL10;

public class ControllerActivityBase extends AppCompatActivity
implements View.OnClickListener, JoystickView.OnJoystickMoveListener, View.OnTouchListener, CvCameraViewListener2, GLBGVideoSprite.GLSpriteUpdateListener {
    public final String TAG = "ControllerActivityBase";

    private Button takeoff_btn;
    private JoystickView left_stick;
    private JoystickView right_stick;
    private ProgressBar battery_status;
    private TextView batteryPer;
    protected Button record_button;
    private Button follow_btn;
    private Button emergency_btn;
    private Button capture_photo;
    private Button switch_cam_btn;

    private CameraBridgeViewBase  mOpenCvCameraView;

    private ImageView openCV_video;

    private GLSurfaceView glView;
    protected DrawingView overlayView;
    protected VideoStageRenderer renderer;
    private Activity context;



    public double leftDelta = 0;
    public double rightDelta = 0;
    public double xDelta = 0;
    public boolean humanDetected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opencv_adapter_ui);

        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        mOpenCvCameraView.setCvCameraViewListener(this);

        context = this;
        renderer = new VideoStageRenderer(context, null);
        renderer.setGLSpriteListener(this);
        //update_thread = new UpdateVideoThread();
        initUI();
        initListeners();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ui_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initUI(){
        takeoff_btn = (Button) findViewById(R.id.takeoffbutton);
        left_stick = (JoystickView) findViewById(R.id.leftstick);
        right_stick = (JoystickView) findViewById(R.id.rightstick);
        battery_status = (ProgressBar) findViewById(R.id.batteryStat);
        batteryPer = (TextView) findViewById(R.id.batteryPercent);
        follow_btn = (Button) findViewById(R.id.follow_button);
        emergency_btn = (Button) findViewById(R.id.emergency_btn);
        record_button = (Button) findViewById(R.id.record_button);
        capture_photo = (Button) findViewById(R.id.capture_photo);
        switch_cam_btn = (Button) findViewById(R.id.switch_button);
        overlayView = (DrawingView) findViewById(R.id.overlay_view);
        glView = (GLSurfaceView) findViewById(R.id.video_feed);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        openCV_video = (ImageView) findViewById(R.id.openCV_video);
        openCV_video.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void initListeners() {
            takeoff_btn.setOnClickListener(this);
            follow_btn.setOnClickListener(this);
            emergency_btn.setOnClickListener(this);
            record_button.setOnClickListener(this);
            capture_photo.setOnClickListener(this);
            switch_cam_btn.setOnClickListener(this);
            left_stick.setOnJoystickMoveListener(this, (long) 100);
            right_stick.setOnJoystickMoveListener(this, (long) 100);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.takeoffbutton:
                onTakeOff_or_Land();
                break;
            case R.id.emergency_btn:
                onEmergency();
                break;
            case R.id.record_button:
                onRecord();
                break;
            case R.id.follow_button:
                onFollow();
                break;
            case R.id.capture_photo:
                onCapturePhoto();
                break;
            case R.id.switch_button:
                onCameraSwitch();
                break;
        }
    }

    public void onJoystickValueChanged(View v,int angle, int power, int direction){
        if (v.getId()==R.id.leftstick)
            onLeftJoystickMove(angle, power, direction);
        if (v.getId()==R.id.rightstick)
            onRightJoystickMove(angle, power, direction);
    }

    public void onJoystickReleased(View v){
        // left unimplemented
    }

    public void onJoystickPressed(View v) {
        // left unimplemented
    }

    public void setUIEnabled(Boolean b){
        takeoff_btn.setEnabled(b);
        left_stick.setEnabled(b);
        right_stick.setEnabled(b);
        follow_btn.setEnabled(b);
        emergency_btn.setEnabled(b);
        record_button.setEnabled(b);
        capture_photo.setEnabled(b);
        switch_cam_btn.setEnabled(b);
    }

    protected void onTakeOff_or_Land(){
        if(takeoff_btn.getText().equals("Take Off")){
            takeoff_btn.setText("Land");
        }

        else if(takeoff_btn.getText().equals("Land")){
            takeoff_btn.setText("Take Off");
        }
    }

    protected void onLeftJoystickMove(int angle, int power, int direction){
        //left unimplemented
    }

    protected void onRightJoystickMove(int angle, int power, int direction){
        //left unimplemented
    }

    protected void onEmergency(){
        //left unimplemented
    }

    protected void onRecord(){
        if(record_button.getText().equals("Record Video")){
            record_button.setText("Stop Recording");
        }
        else if(record_button.getText().equals("Stop Recording")){
            record_button.setText("Record Video");
        }
    }

    protected void onFollow(){
        renderer.setBitmapCapture(true);
        //overlayView.setLayoutParams(new RelativeLayout.LayoutParams(320, 240));
        //glView.setLayoutParams(new RelativeLayout.LayoutParams(320, 240));
//        LayoutParams lp = glView.getLayoutParams();
//        lp.width = 320;
//        lp.height = 240;
//        glView.setLayoutParams(lp);
//
//        renderer.onSurfaceChanged((GL10)null, 320, 240);
        //update_thread.start();
    }

    protected void onCapturePhoto(){
        //left unimplemented
    }

    protected void onCameraSwitch(){
        //left unimplemented
    }

    public void setBatteryValue(int value){
        battery_status.setProgress(value);
        batteryPer.setText(value + " %");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    protected void onResume(){
        if (glView != null) {
            glView.onResume();
        }
        super.onResume();
    }

    @Override
    protected void onPause(){
        if (glView != null) {
            glView.onPause();
        }
        super.onPause();
    }

    //new methods for opencv camera listener

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return null;
    }

    public void onFrameUpdated() {
//        Log.d("ControllerBase", "Listener called");
    }


//    class UpdateVideoThread extends Thread{
//
//        public void run(){
//            while (true) {
//
////                timeNow = System.currentTimeMillis();
////                timeDelta = timeNow - timePrev;
////                if (timeDelta >= 1000 / frameRate) {
////                    runOnUiThread(updateViewRunnable);
////                    timePrev = timeNow;
////                }
//                if(needToUpdate && nativeLibraryLoaded && renderer.getVideoObject()!=null){
//                    Log.d(TAG, "NUMBER BEING PROCESSED: " + renderer.getNum());
//                    processBitmap(renderer.getVideo());
//                    needToUpdate = false;
//                }
//
//            }
//        }
//
//        private void processBitmap(Bitmap inputBitmap){
//            renderer.setBitmapCapture(false);
//            Rect[] locationsArr = new Rect[0];
//
//            Utils.bitmapToMat(inputBitmap, mRgba);
//
//            Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY, 4);
//
//            // Detection
//            if (descriptor != null) {
//                MatOfRect locations = new MatOfRect();
//                MatOfDouble weights = new MatOfDouble();
//                double hitThreshold = 1;
//                Size winStride = new Size();
//                Size padding = new Size();
//                double scale = 1.05;
//                double finalThreshold = 0;
//                boolean useMeanshiftGrouping = true;
//                descriptor.detectMultiScale(mGray, locations, weights, hitThreshold, winStride, padding, scale, finalThreshold, useMeanshiftGrouping);
//                //descriptor.detectMultiScale(mDetectionFrame, locations, weights);
//                locationsArr = locations.toArray();
//            }
//
//            for (int j = 0; j < locationsArr.length; j++) {
//                Rect rect = locationsArr[j];
//                double p2x = rect.x + rect.width;
//                double p2y = rect.y + rect.height;
//                Log.d(TAG, "Rectangle Total: " + locationsArr.length);
//                Log.d(TAG, "Rect (" + rect.x + "," + rect.y + ") " + "(" + p2x + "," + p2y + ")");
//
//                if(j==0){
//                      xDelta = (rect.x + rect.width/2) - mGray.width()/2;
////                    leftDelta = (rect.x - (mGray.width() - p2x));
////                    rightDelta = ((mGray.width() - p2x) - rect.x);
//                }
//
//                Log.d(TAG, "Rectangle: Height " + rect.height + ", Width " + rect.width);
//                Log.d(TAG, "Matrix: Height " + mGray.height() + ", Wdidth " + mGray.width());
//            }
//
//            if(locationsArr.length > 0)
//                humanDetected = true;
//            else
//                humanDetected = false;
//
//            Log.d(TAG, "FRAME PROCESSED!!!!!!");
//            frameReady = true;
//            renderer.setBitmapCapture(true);
//        }
//
//    }

}
