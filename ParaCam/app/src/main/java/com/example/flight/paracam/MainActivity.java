package com.example.flight.paracam;

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
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parrot.freeflight.receivers.DroneAvailabilityDelegate;
import com.parrot.freeflight.receivers.DroneAvailabilityReceiver;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.NetworkChangeReceiver;
import com.parrot.freeflight.receivers.NetworkChangeReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.intents.DroneStateManager;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.parrot.freeflight.transcodeservice.TranscodingService;
//import com.parrot.freeflight.utils.GPSHelper;

public class MainActivity extends AppCompatActivity
implements ServiceConnection,
        DroneAvailabilityDelegate,
        NetworkChangeReceiverDelegate,
        DroneConnectionChangeReceiverDelegate
{
    public static final String TAG = MainActivity.class.getSimpleName();

    private DroneControlService mService;

    private BroadcastReceiver droneStateReceiver;
    private BroadcastReceiver networkChangeReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;

    private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isFinishing()) {
            return;
        }

        initBroadcastReceivers();

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);

//        if (GPSHelper.deviceSupportGPS(this) && !GPSHelper.isGpsOn(this)) {
//            onNotifyAboutGPSDisabled();
//        }
    }

    public void updateButtonState() {
        // TODO
    }

    protected void initBroadcastReceivers()
    {
        droneStateReceiver = new DroneAvailabilityReceiver(this);
        networkChangeReceiver = new NetworkChangeReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);
    }

    private void registerBroadcastReceivers()
    {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastManager.registerReceiver(droneStateReceiver, new IntentFilter(
                DroneStateManager.ACTION_DRONE_STATE_CHANGED));

        IntentFilter mediaReadyFilter = new IntentFilter();
        mediaReadyFilter.addAction(DroneControlService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        mediaReadyFilter.addAction(TranscodingService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        broadcastManager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));

        registerReceiver(networkChangeReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }


    private void unregisterReceivers()
    {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastManager.unregisterReceiver(droneStateReceiver);
        broadcastManager.unregisterReceiver(droneConnectionChangeReceiver);
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

        disableAllButtons();

        checkDroneConnectivity();
    }


    private void disableAllButtons()
    {
        droneOnNetwork = false;

        updateButtonState();
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
        Log.d(TAG, "Network state has changed. State is: " + (info.isConnected()?"CONNECTED":"DISCONNECTED"));

        if (mService != null && info.isConnected()) {
            checkDroneConnectivity();
        } else {
            droneOnNetwork = false;
            updateButtonState();
        }
    }


    public void onDroneConnected()
    {
        if (mService != null) {
            mService.pause();
        }
    }


    public void onDroneDisconnected()
    {
        // Left unimplemented
    }


    public void onDroneAvailabilityChanged(boolean droneOnNetwork)
    {
        if (droneOnNetwork) {
            Log.d(TAG, "AR.Drone connection [CONNECTED]");
            this.droneOnNetwork = droneOnNetwork;

            updateButtonState();
        } else {
            Log.d(TAG, "AR.Drone connection [DISCONNECTED]");
        }
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
        mService = ((DroneControlService.LocalBinder) service).getService();
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
}
