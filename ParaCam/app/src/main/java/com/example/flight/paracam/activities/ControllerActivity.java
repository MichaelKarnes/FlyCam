package com.example.flight.paracam.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.flight.paracam.R;

public class ControllerActivity extends ControllerActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.ControllerActivityBase);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_controller, menu);
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
    protected void onTakeOff() {
        Log.d("ControllerActivity", "Takeoff button pressed");
    }

    @Override
    protected void onLeftJoystickMove(int angle, int power, int direction){
        Log.d("ControllerActivityBase", "Left stick moved");
    }

    @Override
    protected void onRightJoystickMove(int angle, int power, int direction){
        Log.d("ControllerActivityBase", "Right stick moved");
    }
}
