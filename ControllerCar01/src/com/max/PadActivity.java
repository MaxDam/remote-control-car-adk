package com.max;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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


public class PadActivity extends Activity {
	
	private static final String TAG = PadActivity.class.getSimpleName();
    
    //pad
	private int touch_width;
	private int touch_height;
	private final int reverse_percent=100;
	private final int dead_zone=30;
	
	//commands
	private int turn = 0;
	private int speed = 0;
	
	private CommandClient commandClient = null;
	
	//logs 
    private TextView xView;
    private TextView yView;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        setContentView(R.layout.main);
        
        //avvio del client
        /*
		CallbackEvent statusCallback = new CallbackEvent() {
	        public void onCallback(Object arg) {
	        }
		};
		StopEvent stopNotifier = new StopEvent ();
		if (NetworkAdapter.BeginNetworkScan(this, statusCallback, stopNotifier)) {
            if (NetworkAdapter.BeginNetworkConnection(this, statusCallback, stopNotifier)) {
            	NetworkAdapter.SetOtherClientIP(NetworkAdapter.GetServerIP(this));
            	Toast.makeText(this, "connesso al server", Toast.LENGTH_SHORT).show();
            }
        }
        */
        
        //ottiene il touchpad e lo inizializza
		final ImageView touchpad = (ImageView)findViewById(R.id.touchpad);
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
                
	            //init del log
	    		xView = (TextView) findViewById(R.id.xTxt);
	    	    yView = (TextView) findViewById(R.id.yTxt);
	    	    
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    	
                    	//misurazione touch
                    	touchY = (touch_height/2-(int)event.getY());
                    	touchX = ((int)event.getX()-touch_width/2);
	                    	         
                    	//log
                        xView.setText("X: " + touchX.toString());
                	    yView.setText("Y: " + touchY.toString());
                	    
                    	//se non sta accelerando ritorna
                        speed = 0;
                        if (Math.abs(touchY) < dead_zone) return true;
                        
                        //se sta accelerando..
                        speed = (touchY > 0 ? 1 : -1);
                        
                        //se sta sterzando..
                        turn = 0;
                        if(Math.abs(touchX) >= dead_zone) {
                        	turn = (touchX > 0 ? 1 : -1);
                        }
                        
                        //invia il comando al server
                        try {
							commandClient.sendCommand(String.format("%s;%s;%s;", "drive", turn, speed));
							Log.i("controller", "inviato comando; " + turn + " " + speed);
						} catch (Exception e) {
							e.printStackTrace();
						}

	                    break;
                            
                    case MotionEvent.ACTION_MOVE:
                    	 
                    	//misurazione touch
                    	touchY = (touch_height/2-(int)event.getY());
                    	touchX = ((int)event.getX()-touch_width/2);

                    	//log
                        xView.setText("X: " + touchX.toString());
                	    yView.setText("Y: " + touchY.toString());
                	    
                    	//se non sta accelerando ritorna
                        speed = 0;
                        if (Math.abs(touchY) < dead_zone) return true;
                        
                        //se sta accelerando..
                        speed = (touchY > 0 ? 1 : -1);
                        
                        //se sta sterzando..
                        turn = 0;
                        if(Math.abs(touchX) >= dead_zone) {
                        	turn = (touchX > 0 ? 1 : -1);
                        }
                        
                        //invia il comando al server
                        try {
							commandClient.sendCommand(String.format("%s;%s;%s;", "drive", turn, speed));
							Log.i("controller", "inviato comando; " + turn + " " + speed);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						break;
                            
                            
                    case MotionEvent.ACTION_UP:
                		
                    	//log
                        xView.setText("X: 0");
                	    yView.setText("Y: 0");

                	    //azzeramento del valori
                	    turn = 0;
                    	speed = 0;
                	    
                    	//invia il comando al server
                        try {
							commandClient.sendCommand(String.format("%s;%s;%s;", "drive", turn, speed));
							Log.i("controller", "inviato comando; " + turn + " " + speed);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						break;
                    }
                    
                return true;
            }
        }); 
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
 
    @Override
    public void onPause() {
        super.onPause();
    }
 
    @Override
    public void onResume() {
        super.onResume();

        commandClient = new CommandClient();
        String serverIp = PreferenceManager.getDefaultSharedPreferences(this).getString("serverIPPref", "10.26.3.110");
        commandClient.setServerIp(serverIp);
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
                return true;
            case R.id.stearing:
	            	intent = new Intent(PadActivity.this, StearingActivity.class);
	        		startActivity(intent);
                return true;
            case R.id.preferences:
            		intent = new Intent(PadActivity.this, PreferencesActivity.class);
            		startActivity(intent);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}