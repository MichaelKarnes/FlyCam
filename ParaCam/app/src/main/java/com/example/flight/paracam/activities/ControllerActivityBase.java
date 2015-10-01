package com.example.flight.paracam.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.flight.paracam.R;
import com.example.flight.paracam.ui.JoystickView;

public class ControllerActivityBase extends AppCompatActivity
implements View.OnClickListener, JoystickView.OnJoystickMoveListener {

    private Button takeoff_btn;
    private JoystickView left_stick;
    private JoystickView right_stick;

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
    }

    private void initListeners() {
            takeoff_btn.setOnClickListener(this);
            left_stick.setOnJoystickMoveListener(this, (long) 1);
            right_stick.setOnJoystickMoveListener(this, (long) 1);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.takeoffbutton:
                onTakeOff();
        }
    }

    public void onJoystickValueChanged(View v,int angle, int power, int direction){
        if (v.getId()==R.id.leftstick)
            onLeftJoystickMove(angle, power, direction);
        if (v.getId()==R.id.rightstick)
            onRightJoystickMove(angle, power, direction);
    }

    protected void onTakeOff(){

    }

    protected void onLeftJoystickMove(int angle, int power, int direction){

    }

    protected void onRightJoystickMove(int angle, int power, int direction){

    }
}
