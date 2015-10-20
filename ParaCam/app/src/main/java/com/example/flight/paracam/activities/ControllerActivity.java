package com.example.flight.paracam.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.lang.Math;

import com.example.flight.paracam.DroneService;
import com.example.flight.paracam.MainUI;
import com.example.flight.paracam.R;
import com.parrot.freeflight.drone.DroneAcademyMediaListener;
import com.parrot.freeflight.drone.DroneProxy;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.receivers.DroneAvailabilityDelegate;
import com.parrot.freeflight.receivers.DroneAvailabilityReceiver;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.receivers.NetworkChangeReceiver;
import com.parrot.freeflight.receivers.NetworkChangeReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.intents.DroneStateManager;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;

import android.support.v4.content.LocalBroadcastManager;

public class ControllerActivity extends ControllerActivityBase implements ServiceConnection,
        DroneAvailabilityDelegate,
        NetworkChangeReceiverDelegate,
        DroneConnectionChangeReceiverDelegate,
        DroneReadyReceiverDelegate, DroneBatteryChangedReceiverDelegate {

    public static final String TAG = ControllerActivity.class.getSimpleName();

    private DroneService mService;

    private BroadcastReceiver droneStateReceiver;
    private BroadcastReceiver networkChangeReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;
    private BroadcastReceiver droneReadyReceiver;

    private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    private boolean droneOnNetwork;
    private DroneBatteryChangedReceiver droneBatteryReceiver;
    private ControllerActivityBase ui;


    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swscale");
        System.loadLibrary("avcodec");
        System.loadLibrary("avfilter");
        System.loadLibrary("avformat");
        System.loadLibrary("avdevice");
        System.loadLibrary("glfix");
        System.loadLibrary("adfreeflight");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
        ui = new ControllerActivityBase();
        Context con = getApplicationContext();

        initBroadcastReceivers();

        try {
            bindService(new Intent(this, DroneService.class), this, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (GPSHelper.deviceSupportGPS(this) && !GPSHelper.isGpsOn(this)) {
//            onNotifyAboutGPSDisabled();
//        }
        //droneProxy = DroneProxy.getInstance(getApplicationContext());
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
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(droneBatteryReceiver, new IntentFilter(DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
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
        super.onPause();

        unregisterReceivers();
        stopTasks();
    }

    public void onDroneBatteryChanged(int value) {
        setBatteryValue(value);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        registerBroadcastReceivers();

        disableAllButtons();

        checkDroneConnectivity();
    }


    private void disableAllButtons()
    {
        droneOnNetwork = false;
        updateUI();
    }

    protected boolean onStartFreeflight() {
        if (!droneOnNetwork) {
            return false;
        }

//        Intent connectActivity = new Intent(this, ConnectActivity.class);
//        startActivity(connectActivity);
        // TODO implement control

        return true;
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
//            mService.pause();

            mService.requestConfigUpdate();
        }
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
        Log.d(TAG, "Drone is READYYYYYYYYYYYYYYYYYYYYY");
        setUIEnabled(true);

        mService.record();

        //initCanvasView();
        //initGLView();
        initHudController();

//        try {
//            Thread.sleep(50, 0);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //startDrawThread();
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

    public void onServiceConnected(ComponentName name, IBinder service)
    {
        Log.d(TAG, "DroneService CONNECTED");
        mService = (DroneService)((DroneControlService.LocalBinder) service).getService();
        mService.setMagnetoEnabled(true);

        if(mService != null)
            mService.requestDroneStatus();
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

    protected boolean isFreeFlightEnabled()
    {
        return droneOnNetwork;
    }
//
//    private void onNotifyAboutGPSDisabled()
//    {
//        showAlertDialog(getString(R.string.Location_services_alert), getString(R.string.If_you_want_to_store_your_location_anc_access_your_media_enable_it),
//                null);
//    }

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

    public void beginUI(View view) {
        Intent intent = new Intent(this, MainUI.class);
        startActivity(intent);
    }

    @Override
    protected void onTakeOff() {
        mService.triggerTakeOff();
    }

    @Override
    protected void onLand() { mService.triggerTakeOff(); }

    @Override
    protected void onLeftJoystickMove(int angle, int power, int direction){
        double radian_angle = (angle*Math.PI)/180;
        double vertical = (Math.cos(radian_angle)*power)/100;
        double horizontal = (Math.sin(radian_angle)*power)/100;

        if (mService != null){
            mService.setGaz((float) vertical);
            mService.setYaw((float) horizontal);
        }

    }

    @Override
    protected void onRightJoystickMove(int angle, int power, int direction){
        double radian_angle = (angle*Math.PI)/180;
        double vertical = (Math.cos(radian_angle)*power)/100;
        double horizontal = (Math.sin(radian_angle) * power)/ 100;

        mService.setProgressiveCommandEnabled(true);

        if (mService != null) {
            mService.setRoll((float) horizontal);
            mService.setPitch((float) vertical);
        }

    }

    @Override
    public void onJoystickReleased(View v) {
        if(v.getId() == R.id.rightstick) {
            mService.setProgressiveCommandEnabled(false);
        }
    }

    @Override
    protected void onCapturePhoto(){
        mService.takePhoto();
    }

}
