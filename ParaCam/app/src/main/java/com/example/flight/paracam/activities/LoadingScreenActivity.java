package com.example.flight.paracam.activities;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.flight.paracam.DroneService;
import com.example.flight.paracam.R;
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


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.flight.paracam.DroneService;
import com.example.flight.paracam.R;
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


public class LoadingScreenActivity extends AppCompatActivity
        implements ServiceConnection,
        DroneAvailabilityDelegate,
        NetworkChangeReceiverDelegate,
        DroneConnectionChangeReceiverDelegate,
        DroneReadyReceiverDelegate {
    public static final String TAG = MainActivity.class.getSimpleName();

    private DroneService mService;

    private BroadcastReceiver droneStateReceiver;
    private BroadcastReceiver networkChangeReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;
    private BroadcastReceiver droneReadyReceiver;

    private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    Boolean droneConnected = false;

    private boolean droneOnNetwork;

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

        setContentView(R.layout.activity_loading);

        initBroadcastReceivers();

        Context freeflightContext;
        try {
            //freeflightContext = getApplicationContext().createPackageContext("com.parrot.freeflight", Context.CONTEXT_IGNORE_SECURITY);
            bindService(new Intent(this, DroneService.class), this, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void initBroadcastReceivers()
    {
        droneStateReceiver = new DroneAvailabilityReceiver(this);
        networkChangeReceiver = new NetworkChangeReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);
        droneReadyReceiver = new DroneReadyReceiver(this);
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


    @Override
    protected void onResume()
    {
        super.onResume();

        registerBroadcastReceivers();


        checkDroneConnectivity();
    }



    public void onNetworkChanged(NetworkInfo info)
    {
        Log.d(TAG, "Network state has changed. State is: " + (info.isConnected()?"CONNECTED":"DISCONNECTED"));

        if (mService != null && info.isConnected()) {
            checkDroneConnectivity();
        } else {
            droneOnNetwork = false;
        }
    }


    public void onDroneConnected()
    {
        droneConnected = true;
        if (mService != null) {
            mService.requestConfigUpdate();
        }
    }


    public void onDroneDisconnected()
    {
        droneConnected = false;
    }


    public void onDroneAvailabilityChanged(boolean droneOnNetwork)
    {
        if (droneOnNetwork) {
//            Log.d(TAG, "AR.Drone connection [CONNECTED]");
            Log.d(TAG, "AR.Drone connection [ON NETWORK]");
            this.droneOnNetwork = droneOnNetwork;

        } else {
//            Log.d(TAG, "AR.Drone connection [DISCONNECTED]");
            Log.d(TAG, "AR.Drone connection [NOT ON NETWORK]");
        }
    }

    public void onDroneReady() {
        droneConnected = true;
        Log.d(TAG, "Drone is READYYYYYYYYYYYYYYYYYYYYY");
        Intent intent = new Intent(this, ControllerActivity.class);
        startActivity(intent);
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

        if(mService != null)
            mService.requestDroneStatus();
    }


    public void onServiceDisconnected(ComponentName name)
    {
        // Left unimplemented
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
}

