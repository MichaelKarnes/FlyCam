package com.example.flight.paracam.activities;

import android.app.Activity;
import android.graphics.Canvas;
import android.content.res.Resources;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.view.ViewGroup.LayoutParams;

import com.example.flight.paracam.HudViewController;
import com.example.flight.paracam.R;
import com.example.flight.paracam.ui.DroneVideoView;
import com.example.flight.paracam.ui.JoystickView;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.parrot.freeflight.video.VideoStageView;

public class ControllerActivityBase extends AppCompatActivity
implements View.OnClickListener, JoystickView.OnJoystickMoveListener, View.OnTouchListener {

    private Button takeoff_btn;
    private JoystickView left_stick;
    private JoystickView right_stick;
    private ProgressBar battery_status;
    private TextView batteryPer;
    private Button record_button;
    private Button follow_btn;
    private Button emergency_btn;
    private Button capture_photo;
    private Button switch_cam_btn;
//    private DrawThread drawThread;

    private GLSurfaceView glView;
    private VideoStageRenderer renderer;
    private Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui_controller);

        context = this;
        renderer = new VideoStageRenderer(context, null);
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
        glView = (GLSurfaceView) findViewById(R.id.video_feed);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
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
            case R.id.emergency_btn:
                onEmergency();
            case R.id.record_button:
                onRecord();
            case R.id.follow_button:
                onFollow();
            case R.id.capture_photo:
                onCapturePhoto();
            case R.id.switch_button:
                onCameraSwitch();
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
        //left unimplemented
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
}
