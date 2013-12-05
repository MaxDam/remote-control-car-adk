package com.max;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class StearingActivity extends Activity implements SensorEventListener {
    
	/* sensor data */
    SensorManager m_sensorManager;
    float []m_lastMagFields;
    float []m_lastAccels;
    private float[] m_rotationMatrix = new float[16];
    private float[] m_remappedR = new float[16];
    private float[] m_orientation = new float[4];
 
    /* fix random noise by averaging tilt values */
    final static int AVERAGE_BUFFER = 30;
    float []m_prevPitch = new float[AVERAGE_BUFFER];
    float m_lastPitch = 0.f;
    float m_lastYaw = 0.f;
    /* current index int m_prevEasts */
    int m_pitchIndex = 0;
 
    float []m_prevRoll = new float[AVERAGE_BUFFER];
    float m_lastRoll = 0.f;
    /* current index into m_prevTilts */
    int m_rollIndex = 0;
 
    /* center of the rotation */
    private float m_tiltCentreX = 0.f;
    private float m_tiltCentreY = 0.f;
    private float m_tiltCentreZ = 0.f;
 
    //pad
	private int touch_width;
	private int touch_height;
	private final int reverse_percent = 100;
	private final int speed_dead_zone = 30;
	private final int turn_dead_zone = 10;
	
	//commands
	int turn = 0;
	int speed = 0;
	
	private CommandClient commandClient = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.stearing);
        
        m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerListeners();
        
        //ottiene il touchpad e lo inizializza
		final ImageView touchpad = (ImageView)findViewById(R.id.steering);
        touchpad.post(new Runnable() {   //get size after touchpad drawn
            public void run() {
                touch_width = touchpad.getWidth();
                touch_height = touchpad.getHeight();
            }
        });

        //setta gli eventi del touchpad
        touchpad.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

	            Integer touchX = 0;
	            Integer touchY = 0;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    	
                    	//misurazione touch
                    	touchY = (touch_height/2-(int)event.getY());
                    	touchX = ((int)event.getX()-touch_width/2);
	                    
                        //se sta accelerando ..
                        
                        if (Math.abs(touchX) > speed_dead_zone) {
                            speed = (touchX > 0 ? 1 : -1);
                            sendCommandToServer();
                        }

	                    break;
                       
                    case MotionEvent.ACTION_MOVE:
                    	
                    	//misurazione touch
                    	touchY = (touch_height/2-(int)event.getY());
                    	touchX = ((int)event.getX()-touch_width/2);
	                    
                        //se sta accelerando ..
                        if (Math.abs(touchX) > speed_dead_zone) {
                            speed = (touchX > 0 ? 1 : -1);
                            sendCommandToServer();
                        }

	                    break;
	                    
                    case MotionEvent.ACTION_UP:
                		
                    	speed = 0;
                    	sendCommandToServer();
						
						break;
                    }
                    
                return true;
            }
        });  
    }

    //invia il comando al server
	public void sendCommandToServer() {
		try {
			commandClient.sendCommand(String.format("%s;%s;%s;", "drive", turn, speed));
			Log.i("controller", "inviato comando; " + turn + " " + speed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    private void registerListeners() {
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }
 
    private void unregisterListeners() {
        m_sensorManager.unregisterListener(this);
    }
 
    @Override
    public void onDestroy() {
        unregisterListeners();
        super.onDestroy();
    }
 
    @Override
    public void onPause() {
        unregisterListeners();
        super.onPause();
    }
 
    @Override
    public void onResume() {
        registerListeners();
        super.onResume();
        
        commandClient = new CommandClient();
        String serverIp = PreferenceManager.getDefaultSharedPreferences(this).getString("serverIPPref", "10.26.3.110");
        commandClient.setServerIp(serverIp);
    }
 
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
 
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accel(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag(event);
        }
    }
 
    private void accel(SensorEvent event) {
        if (m_lastAccels == null) {
            m_lastAccels = new float[3];
        }
 
        System.arraycopy(event.values, 0, m_lastAccels, 0, 3);
 
        /*if (m_lastMagFields != null) {
            computeOrientation();
        }*/
    }
 
    private void mag(SensorEvent event) {
        if (m_lastMagFields == null) {
            m_lastMagFields = new float[3];
        }
 
        System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);
 
        if (m_lastAccels != null) {
            computeOrientation();
        }
    }
 
    Filter [] m_filters = { new Filter(), new Filter(), new Filter() };
 
    private class Filter {
        static final int AVERAGE_BUFFER = 10;
        float []m_arr = new float[AVERAGE_BUFFER];
        int m_idx = 0;
 
        public float append(float val) {
            m_arr[m_idx] = val;
            m_idx++;
            if (m_idx == AVERAGE_BUFFER)
                m_idx = 0;
            return avg();
        }
        public float avg() {
            float sum = 0;
            for (float x: m_arr)
                sum += x;
            return sum / AVERAGE_BUFFER;
        }
 
    }
 
    private void computeOrientation() {
    	if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels, m_lastMagFields)) {
            SensorManager.getOrientation(m_rotationMatrix, m_orientation);
 
            /* 1 radian = 57.2957795 degrees */
            /* [0] : yaw, rotation around z axis
             * [1] : pitch, rotation around x axis
             * [2] : roll, rotation around y axis */
            float yaw = m_orientation[0] * 57.2957795f;
            float pitch = m_orientation[1] * 57.2957795f;
            float roll = m_orientation[2] * 57.2957795f;
 
            m_lastYaw = m_filters[0].append(yaw);
            m_lastPitch = m_filters[1].append(pitch);
            m_lastRoll = m_filters[2].append(roll);
            TextView rt = (TextView) findViewById(R.id.roll);
            TextView pt = (TextView) findViewById(R.id.pitch);
            TextView yt = (TextView) findViewById(R.id.yaw);
            yt.setText("azi z: " + m_lastYaw);
            pt.setText("pitch x: " + m_lastPitch);
            rt.setText("roll y: " + m_lastRoll);
            
            //aggiorna il turn
            if(Math.abs(m_lastPitch) > turn_dead_zone) {
            	turn = (m_lastPitch > 0 ? -1 : 1);
            	sendCommandToServer();
            }
            else {
            	turn = 0;
            	sendCommandToServer();
            }
        }
    }
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionmenu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	
        // Handle item selection
    	switch (item.getItemId()) {
            case R.id.pad:
            	intent = new Intent(StearingActivity.this, PadActivity.class);
        		startActivity(intent);
                return true;
            case R.id.stearing:
                return true;
            case R.id.preferences:
            		intent = new Intent(StearingActivity.this, PreferencesActivity.class);
            		startActivity(intent);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}