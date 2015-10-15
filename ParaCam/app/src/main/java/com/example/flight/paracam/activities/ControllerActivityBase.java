package com.example.flight.paracam.activities;

import android.app.Activity;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.view.ViewGroup.LayoutParams;

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
    private Button land_btn;
    private Button follow_btn;
    private Button emergency_btn;
    private Button capture_photo;

    private DroneVideoView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui_controller);

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
        land_btn = (Button) findViewById(R.id.land_button);
        capture_photo = (Button) findViewById(R.id.capture_photo);
    }

    private void initListeners() {
            takeoff_btn.setOnClickListener(this);
            follow_btn.setOnClickListener(this);
            emergency_btn.setOnClickListener(this);
            land_btn.setOnClickListener(this);
            capture_photo.setOnClickListener(this);
            left_stick.setOnJoystickMoveListener(this, (long) 100);
            right_stick.setOnJoystickMoveListener(this, (long) 100);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.takeoffbutton:
                onTakeOff();
            case R.id.emergency_btn:
                onEmergency();
            case R.id.land_button:
                onLand();
            case R.id.follow_button:
                onFollow();
            case R.id.capture_photo:
                onCapturePhoto();
        }
    }

    public void initVideoView() {
        view = new DroneVideoView(this, false);
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
        land_btn.setEnabled(b);
        capture_photo.setEnabled(b);
    }

    protected void onTakeOff(){

    }

    protected void onLeftJoystickMove(int angle, int power, int direction){

    }

    protected void onRightJoystickMove(int angle, int power, int direction){

    }

    protected void onEmergency(){

    }

    protected void onLand(){

    }

    protected void onFollow(){

    }

    protected void onCapturePhoto(){

    }

    public void setBatteryValue(int value){
        battery_status.setProgress(value);
        batteryPer.setText(value + " %");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }
}
