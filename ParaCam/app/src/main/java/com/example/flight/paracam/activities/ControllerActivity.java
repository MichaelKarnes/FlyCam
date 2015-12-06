package com.example.flight.paracam.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.flight.paracam.DroneService;
import com.example.flight.paracam.R;
import com.example.flight.paracam.RelativeRect;
import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.receivers.DroneAvailabilityDelegate;
import com.parrot.freeflight.receivers.DroneAvailabilityReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneVideoRecordStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneVideoRecordingStateReceiver;
import com.parrot.freeflight.receivers.MediaReadyDelegate;
import com.parrot.freeflight.receivers.MediaReadyReceiver;
import com.parrot.freeflight.receivers.MediaStorageReceiverDelegate;
import com.parrot.freeflight.receivers.NetworkChangeReceiver;
import com.parrot.freeflight.receivers.NetworkChangeReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.intents.DroneStateManager;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.parrot.freeflight.transcodeservice.TranscodingService;
import com.parrot.freeflight.video.VideoStageRenderer;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;

public class ControllerActivity extends ControllerActivityBase implements ServiceConnection,
        DroneAvailabilityDelegate,
        NetworkChangeReceiverDelegate,
        DroneConnectionChangeReceiverDelegate,
        DroneReadyReceiverDelegate, DroneBatteryChangedReceiverDelegate, DroneVideoRecordStateReceiverDelegate, DroneCameraReadyActionReceiverDelegate, DroneRecordReadyActionReceiverDelegate,
        MediaReadyDelegate,
        MediaStorageReceiverDelegate, VideoStageRenderer.BitmapReadyListener {

    public static final String TAG = ControllerActivity.class.getSimpleName();

    private DroneService mService;

    private BroadcastReceiver droneStateReceiver;
    private BroadcastReceiver networkChangeReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;
    private BroadcastReceiver droneReadyReceiver;
    private DroneVideoRecordingStateReceiver videoRecordingStateReceiver;
    private DroneCameraReadyChangeReceiver droneCameraReadyChangedReceiver;
    private DroneRecordReadyChangeReceiver droneRecordReadyChangeReceiver;
    private MediaReadyReceiver mediaReadyReceiver;

    private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    private boolean droneOnNetwork;
    private DroneBatteryChangedReceiver droneBatteryReceiver;
    private ControllerActivityBase ui;
    private boolean recording;
    private boolean prevRecording;
    private boolean running;

    //private HumanRecognitionThread human_recognition;

    private RelativeRect mRect;

    private int height = 240;
    private int width = 320;

    private Mat mRgba;
    private Mat mGray;
    private Mat mDetectionFrame;
    private Bitmap processedBitmap;
    private HOGDescriptor           descriptor;

    public boolean frameReady = false;

    private float detectionSizeRatio = 0.5f;

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swscale");
        System.loadLibrary("avcodec");
        System.loadLibrary("avfilter");
        System.loadLibrary("avformat");
        System.loadLibrary("avdevice");
        System.loadLibrary("glfix");
        System.loadLibrary("adfreeflight");
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        renderer.bitmap_listener = this;

        if (isFinishing()) {
            return;
        }

        ui = new ControllerActivityBase();
        running = false;

        initBroadcastReceivers();

        try {
            bindService(new Intent(this, DroneService.class), this, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRgba = new Mat(height, width, CvType.CV_8UC3);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mDetectionFrame = new Mat((int)(height * detectionSizeRatio), (int) (width * detectionSizeRatio), CvType.CV_8UC1);
        processedBitmap = Bitmap.createBitmap(1280, 960, Bitmap.Config.ARGB_8888);

        System.loadLibrary("opencv_java3");
        onNativeLibraryLoaded();

        mRect = new RelativeRect(0, 0, 0, 0);

        //human_recognition = new HumanRecognitionThread();
    }

    public void clickFunc(View view) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (mService != null) {
                    mService.triggerTakeOff();
                }
            }
        }, 1000);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            public void run() {
                if (mService != null) {
                    mService.triggerEmergency();
                }
            }
        }, 4000);
    }

    public void updateUI() {
        // TODO
        if(droneOnNetwork) {
            //setUIEnabled(true);
        }
        else {
            setUIEnabled(false);
        }
    }

    protected void initBroadcastReceivers()
    {
        droneStateReceiver = new DroneAvailabilityReceiver(this);
        networkChangeReceiver = new NetworkChangeReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);
        droneReadyReceiver = new DroneReadyReceiver(this);
        droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
        videoRecordingStateReceiver = new DroneVideoRecordingStateReceiver(this);
        droneCameraReadyChangedReceiver = new DroneCameraReadyChangeReceiver(this);
        droneRecordReadyChangeReceiver = new DroneRecordReadyChangeReceiver(this);
        mediaReadyReceiver = new MediaReadyReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(droneBatteryReceiver, new IntentFilter(DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(videoRecordingStateReceiver, new IntentFilter(DroneControlService.VIDEO_RECORDING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneCameraReadyChangedReceiver, new IntentFilter(DroneControlService.CAMERA_READY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneRecordReadyChangeReceiver, new IntentFilter(DroneControlService.RECORD_READY_CHANGED_ACTION));

        IntentFilter mediaReadyFilter = new IntentFilter();
        mediaReadyFilter.addAction(DroneControlService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        mediaReadyFilter.addAction(TranscodingService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        localBroadcastMgr.registerReceiver(mediaReadyReceiver, mediaReadyFilter);
    }

    private void registerBroadcastReceivers()
    {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastManager.registerReceiver(droneStateReceiver, new IntentFilter(
                DroneStateManager.ACTION_DRONE_STATE_CHANGED));

        broadcastManager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));
        broadcastManager.registerReceiver(droneReadyReceiver, new IntentFilter(DroneControlService.DRONE_STATE_READY_ACTION));

        registerReceiver(networkChangeReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }


    private void unregisterReceivers()
    {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastManager.unregisterReceiver(droneStateReceiver);
        broadcastManager.unregisterReceiver(droneConnectionChangeReceiver);
        broadcastManager.unregisterReceiver(droneReadyReceiver);
        unregisterReceiver(networkChangeReceiver);
    }


    @Override
    protected void onDestroy()
    {
        unbindService(this);
        super.onDestroy();
    }


    @Override
    protected void onPause()
    {
        if (mService != null) {
            mService.pause();
        }

        unregisterReceivers();
        stopTasks();

        super.onPause();

    }

    public void onDroneBatteryChanged(int value) {
        setBatteryValue(value);
    }

    @Override
    protected void onResume()
    {
        if (mService != null) {
            mService.resume();
        }

        registerBroadcastReceivers();

        disableAllButtons();

        checkDroneConnectivity();

        super.onResume();
    }


    private void disableAllButtons()
    {
        droneOnNetwork = false;
        updateUI();
    }

    public void onNetworkChanged(NetworkInfo info)
    {
        Log.d(TAG, "Network state has changed. State is: " + (info.isConnected() ? "CONNECTED" : "DISCONNECTED"));
        if (mService != null && info.isConnected()) {
            checkDroneConnectivity();
        } else {
            droneOnNetwork = false;
            updateUI();
        }
    }


    public void onDroneConnected()
    {
        if (mService != null) {
            mService.resume();
            mService.requestDroneStatus();
        }

        runTranscoding();
    }


    public void onDroneDisconnected()
    {
        // Left unimplemented
    }


    public void onDroneAvailabilityChanged(boolean droneOnNetwork)
    {
        if (droneOnNetwork) {
//            Log.d(TAG, "AR.Drone connection [CONNECTED]");
            Log.d(TAG, "AR.Drone connection [ON NETWORK]");
            this.droneOnNetwork = droneOnNetwork;

            updateUI();
        } else {
//            Log.d(TAG, "AR.Drone connection [DISCONNECTED]");
            Log.d(TAG, "AR.Drone connection [NOT ON NETWORK]");
        }
    }

    public void onDroneReady() {
        // TODO
        setUIEnabled(true);
    }


    @SuppressLint("NewApi")
    private void checkDroneConnectivity()
    {
        if (checkDroneConnectionTask != null && checkDroneConnectionTask.getStatus() != AsyncTask.Status.FINISHED) {
            checkDroneConnectionTask.cancel(true);
        }

        checkDroneConnectionTask = new CheckDroneNetworkAvailabilityTask() {

            @Override
            protected void onPostExecute(Boolean result) {
                onDroneAvailabilityChanged(result);
            }

        };

        if (Build.VERSION.SDK_INT >= 11) {
            checkDroneConnectionTask.executeOnExecutor(CheckDroneNetworkAvailabilityTask.THREAD_POOL_EXECUTOR, this);
        } else {
            checkDroneConnectionTask.execute(this);
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service){
        Log.d(TAG, "DroneService CONNECTED via CONTROLLER ACTIVITY!!!!!!!!");
        mService = (DroneService)((DroneControlService.LocalBinder) service).getService();
        mService.setMagnetoEnabled(true);

        if(mService != null) {
            mService.resume();
            mService.requestDroneStatus();

        }
        runTranscoding();
    }


    public void onServiceDisconnected(ComponentName name)
    {
        mService = null;
    }

    private boolean taskRunning(AsyncTask<?,?,?> checkMediaTask2)
    {
        if (checkMediaTask2 == null)
            return false;

        if (checkMediaTask2.getStatus() == AsyncTask.Status.FINISHED)
            return false;

        return true;
    }


    private void stopTasks()
    {
        if (taskRunning(checkDroneConnectionTask)) {
            checkDroneConnectionTask.cancelAnyFtpOperation();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    protected void onEmergency(){
        mService.triggerEmergency();
    }

    @Override
    protected void onRecord(){
        super.onRecord();
        if (isFinishing()) {
            return;
        }
        mService.record();
    }

    @Override
    protected void onTakeOff_or_Land() {
        super.onTakeOff_or_Land();
        if(isFinishing()){
            return;
        }
        if(mService!=null)
            mService.triggerTakeOff();
    }

    @Override
    protected void onLeftJoystickMove(int angle, int power, int direction){
        double radian_angle = (angle*Math.PI)/180;
        double vertical = (Math.cos(radian_angle)*power)/100;
        double horizontal = (Math.sin(radian_angle)*power)/100;

        //if(running){
            if (mService != null){
                mService.setGaz((float) vertical);
                mService.setYaw((float) horizontal);
                Log.d(TAG, "Vertical = " + vertical);
            }
        //}
    }

    @Override
    protected void onRightJoystickMove(int angle, int power, int direction){
        double radian_angle = (angle*Math.PI)/180;
        double vertical = (Math.cos(radian_angle)*power)/100;
        double horizontal = (Math.sin(radian_angle) * power)/ 100;

        //if(running){
            if (mService != null) {
                mService.setRoll((float) horizontal);
                mService.setPitch((float) -vertical);
            }
        //}
    }

    @Override
    public void onJoystickReleased(View v) {
        if(v.getId() == R.id.rightstick) {
            mService.setProgressiveCommandEnabled(false);
            running = false;
           // mService.setPitch(0);
           // mService.setRoll(0);
        }
        else if(v.getId() == R.id.leftstick){
            running = false;
            mService.setGaz(0);
            mService.setYaw(0);
        }
    }

    @Override
    public void onJoystickPressed(View v) {
        running = true;
        switch(v.getId()) {
            case R.id.rightstick:
                mService.setProgressiveCommandEnabled(true);
                break;
            case R.id.leftstick:
                //mService.setProgressiveCommandEnabled(false);
                break;
        }
    }

    @Override
    protected void onCapturePhoto(){
        mService.takePhoto();
    }

    @Override
    protected void onFollow(){
        super.onFollow();
        //human_recognition.start();
    }

    @Override
    public void onDroneRecordVideoStateChanged(boolean recording, boolean usbActive, int remainingTime) {
        if (mService == null)
             return;

        prevRecording = this.recording;
        this.recording = recording;

        if (!recording) {
            if (prevRecording != recording && mService != null) {
                runTranscoding();
            }
        }
    }

    @Override
    public void onCameraReadyChanged(boolean ready) {
        if (ready=true){
            //initVideoView();
        }
    }

    @Override
    public void onDroneRecordReadyChanged(boolean ready) {

    }

    private void runTranscoding(){
        if (mService != null) {
            DroneConfig droneConfig = mService.getDroneConfig();

            boolean sdCardMounted = mService.isMediaStorageAvailable();
            boolean recordingToUsb = droneConfig.isRecordOnUsb() && mService.isUSBInserted();

            if (recording)
                mService.record();
            else{
                // Start recording
                if (!sdCardMounted) {
                    if (recordingToUsb)
                        mService.record();
                    } else
                        mService.record();

            }
        }
    }


    @Override
    public void onMediaReady(File mediaFile) {
        Log.d(TAG, "New file available " + mediaFile.getAbsolutePath());
    }

    @Override
    public void onMediaStorageMounted() {

    }

    @Override
    public void onMediaStorageUnmounted() {

    }

    @Override
    public void onMediaEject() {

    }

    @Override
    protected void onCameraSwitch(){
        mService.switchCamera();
    }

    private long lastServiceCalledTime = 0;
    private long serviceCommandIntervalMillis = 750;

    ///////////////////////////////////////////////////////////
    // IMAGE PROCESSING ///////////////////////////////////////
    ///////////////////////////////////////////////////////////

    private void onNativeLibraryLoaded() {
        /*Size _winSize = new Size(64, 128);
        Size _blockSize = new Size(16, 16);
        Size _blockStride = new Size(8, 8);
        Size _cellSize = new Size(8, 8);
        int _nbins = 9;
        descriptor = new HOGDescriptor(_winSize, _blockSize, _blockStride, _cellSize, _nbins);*/
        descriptor = new HOGDescriptor();
        descriptor.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    @Override
    public void onBitmapReady(Bitmap bp) {
        processBitmap(bp);
        Log.d(TAG, "xDelta = " + xDelta);

        overlayView.xRatio = (float)(xDelta + 1) / 2;
        overlayView.postInvalidate();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                record_button.setText("" + xDelta);
            }
        });

        long now = System.currentTimeMillis();

        boolean canCallService = (now - lastServiceCalledTime) > serviceCommandIntervalMillis;

        if(humanDetected){
            //if(xDelta > 0.1 || xDelta < -0.1)

            //if (canCallService) {
                mService.setYaw((float) (xDelta * 0.5));

                lastServiceCalledTime = System.currentTimeMillis();
            //}
        }

        else //if (canCallService)
            mService.setYaw(0);
    }


    public void processBitmap(Bitmap inputBitmap){
        double startTime = System.currentTimeMillis();
        renderer.setBitmapCapture(false);
        Rect[] locationsArr = new Rect[0];

        Utils.bitmapToMat(inputBitmap, mRgba);

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY, 4);

        // Detection
        if (descriptor != null) {
            //Imgproc.resize(mGray, mGray, new Size(0, 0), detectionSizeRatio, detectionSizeRatio, Imgproc.INTER_LINEAR);
            MatOfRect locations = new MatOfRect();
            MatOfDouble weights = new MatOfDouble();
            double hitThreshold = 1;
            Size winStride = new Size();
            Size padding = new Size();
            double scale = 1.05;
            double finalThreshold = 0;
            boolean useMeanshiftGrouping = true;
            descriptor.detectMultiScale(mGray, locations, weights, hitThreshold, winStride, padding, scale, finalThreshold, useMeanshiftGrouping);
            //descriptor.detectMultiScale(mDetectionFrame, locations, weights);
            locationsArr = locations.toArray();
        }

        for (int j = 0; j < locationsArr.length; j++) {
            Rect rect = locationsArr[j];
            double p2x = rect.x + rect.width;
            double p2y = rect.y + rect.height;
            Log.d(TAG, "HUMAN DETECTED!!!!!!!");
//            Log.d(TAG, "Rect (" + rect.x + "," + rect.y + ") " + "(" + p2x + "," + p2y + ")");

            if(j==0){
                xDelta = (double) ((rect.x + rect.width/2) - mGray.width()/2)/(mGray.width()/2);
            }

//            Log.d(TAG, "Rectangle: Height " + rect.height + ", Width " + rect.width);
//            Log.d(TAG, "Matrix: Height " + mGray.height() + ", Width " + mGray.width());
        }

        if(locationsArr.length == 0){
            humanDetected = false;
            xDelta = 0;
        }
        else
            humanDetected = true;

        Log.d(TAG, "FRAME PROCESSED!!!!!!");
        frameReady = true;
        renderer.setBitmapCapture(true);
        double timeProcessed = System.currentTimeMillis() - startTime;
        Log.d(TAG, "TIME TAKEN FOR IMAGE PROCESS = " + timeProcessed);
    }


//    class HumanRecognitionThread extends Thread{
//        public void run(){
//            while(true){
//                if(frameReady){
//                    updateInfo();
//                    if(humanDetected){
//                        if(xDelta > 100){
//                            Log.d(TAG, "Moving Right: " + xDelta);
//                            mService.setYaw((float) 0.1);
//                        }
//
//                        if(xDelta < -100){
//                            Log.d(TAG, "Moving Left: " + xDelta);
//                            mService.setYaw((float) -0.1);
//                        }
////                        else if(rightDelta > 100){
////                            Log.d(TAG, "rightDelta: " + rightDelta);
////                            mService.setYaw((float) -0.1);
////                        }
//
//                        else
//                            mService.setYaw(0);
//                    }
//                    else
//                        mService.setYaw(0);
//
//                    frameReady = false;
//                }
//            }
//        }
//    }

}
