package com.max;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class DriverCar01Activity extends Activity {
	private static final String TAG = DriverCar01Activity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	//lock 
    private PowerManager.WakeLock lock;
    
    //accessory USB
	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private static final byte COMMAND_DRIVE = 0x1;
	
	private static final byte COMMAND_FORWARD = 0x1;
	private static final byte COMMAND_BACK = 0x2;
	private static final byte COMMAND_RIGHT = 0x1;
	private static final byte COMMAND_LEFT = 0x2;
	private static final byte COMMAND_STOP = 0x0;

	private int touch_width;
	private int touch_height;
    private final int reverse_percent=100;
    private final int dead_zone=30;
    
    private Handler serverHandler = null;
    private Vibrator vibrator;
    private CommandServer server = null;
    
    //console and ip
    private Handler UIHandler = new Handler();
    private TextView console;
    private ScrollView console_scroll;
    private TextView ipTxt;
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		//sattaggi view
		setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		//console and ip
		console = (TextView)findViewById(R.id.ConsoleText);
		console_scroll= (ScrollView)findViewById(R.id.ConsoleScroll);
		ipTxt = (TextView)findViewById(R.id.ip);
		
		//set del lock
		setWakeLock(this);
		
		//initialize usb
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);

		//servizio di vibrazione
		vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		
		//avvio dell'access point
		/*
		try {
			result = NetworkAdapter.StartAccessPoint(this, new CallbackEvent() {
				@Override
			    public void onCallback(Object arg) {
			    }
			}, new StopEvent());
			if(result) {
				Toast.makeText(this, "server avviato", Toast.LENGTH_SHORT).show();
				NetworkAdapter.StartWifi(this, new CallbackEvent() {
					@Override
				    public void onCallback(Object arg) {
				    }
				}, new StopEvent());
			}
			else {
				Toast.makeText(this, "server non avviato", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			Toast.makeText(this, "server non avviato", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		*/
		
		//bottone di test
		final Button buttonTestArduino = (Button) findViewById(R.id.testArduino);
		buttonTestArduino.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	for(int i = 0; i < 5; i++) {
            		sendDriverCommand(0, 1);
            		try {
            			Thread.sleep(300);
            		} catch (InterruptedException e) {
            			e.printStackTrace();
            		}
            		sendDriverCommand(1, 1);
            		try {
            			Thread.sleep(300);
            		} catch (InterruptedException e) {
            			e.printStackTrace();
            		}
            	}
        		sendDriverCommand(0, 0);
            }
        });
        
        final Button buttonReconnectUsbArduino = (Button) findViewById(R.id.reconnectUsbArduino);
        buttonReconnectUsbArduino.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	mUsbManager = UsbManager.getInstance(DriverCar01Activity.this);
        		mPermissionIntent = PendingIntent.getBroadcast(DriverCar01Activity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        		
        		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        		closeAccessory();
        		openAccessory(accessory);
            }
        });
		
        //set dell'ip address
		ipTxt.setText("server ip address: " + getLocalIpAddress());
	}

	/**
	 * Called when the activity is resumed from its paused state and immediately
	 * after onCreate().
	 */
	@Override
	public void onResume() {
		super.onResume();

		//set del lock
		setWakeLock(this);
		
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		//avvia l'accessorio USB
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
		
		//start del server
		startServer();
		
		
	}

	//avvia il server e gestisce l'handler dei comandi ricevuti
	private synchronized void startServer() {
		
		//gestore dei comandi remoti
		if(serverHandler == null) {
			serverHandler = new Handler() {
		    	@Override
		    	  public void handleMessage(Message msg) {
		    		String payload = (String) msg.obj;
		    		if(payload == null) return;
		    		
		    		//data la stringa del comando ottiene i dettagli
		    		String[] command = payload.split(";");
		        	String azione = command[0];
		        	int turn = Integer.parseInt(command[1]);
		        	int speed = Integer.parseInt(command[2]);
		        	Log.i("server", String.format("ricevuto azione:%s turn:%s speed:%s", azione, turn, speed));
		        	console(String.format("receive action: %s turn:%s speed:%s", azione, turn, speed));
		        	
		        	//vibra
		        	//vibrator.vibrate(100);
		        	
		        	//invia il comando (se è un'azione di guida)
		        	if(azione.equals("drive")) {
		        		sendDriverCommand(turn, speed);
		        	}
		    	  }
		    };
		    
			
		    //start del server udp 
		    server = new CommandServer(this, serverHandler);
			server.execute();
			console("server started..");
		}
	}

	/** Called when the activity is paused by the system. */
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
		releaseWakeLock();
		server.cancel(false);
	}

	/**
	 * Called when the activity is no longer needed prior to being removed from
	 * the activity stack.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		releaseWakeLock();
		server.cancel(false);
	}

	//apre la comunicazione tramite l'accessorio USB
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	//chiude la comunicazione con l'accessorio USB
	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	//invia un comando di pilotaggio
	public void sendDriverCommand(int turn, int speed) {
		
        byte turn_cmd = COMMAND_STOP;
        if(turn != 0) {
        	turn_cmd = (turn > 0 ? COMMAND_RIGHT : COMMAND_LEFT);
        }
        
        byte speed_cmd = COMMAND_STOP;
        if(speed != 0) {
        	speed_cmd = (speed > 0 ? COMMAND_FORWARD : COMMAND_BACK);
        }
        
        //invia il comando ad arduino
        sendCommandDriveToArduino(turn_cmd, speed_cmd);
	}
	
	//invia un comando di pilotaggio all'arduino
	private void sendCommandDriveToArduino(byte turn, byte speed) {
	
		byte[] buffer = new byte[3];
		buffer[0] = COMMAND_DRIVE;
		buffer[1] = turn;
		buffer[2] = speed;
		
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
				console("send data to arduino success");
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
				console("write failed to arduino");
			}
		}
		else {
			console("stream null to arduino");
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, 0, 0, "Preferences");
	    return true;
	}
 
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
	    	case 0: {
	             Intent intent = new Intent(DriverCar01Activity.this, PreferencesActivity.class);
	             startActivity(intent);
	             return true;
            }
	    }
	     
	    return false;
	}
	
	//acquisisce il lock
	public void setWakeLock(Context context){

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
		lock.acquire();
	}

	//rilascia il lock
	public void releaseWakeLock(){

		if(lock != null && lock.isHeld()){
			lock.release();			
		}
		lock = null;
	}

	public void console (final String msg){
		UIHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, msg);
                console.append(msg + "\n");
                console_scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
	}
	
	public String getLocalIpAddress() {
		WifiManager wim= (WifiManager) getSystemService(WIFI_SERVICE);
		List<WifiConfiguration> l =  wim.getConfiguredNetworks(); 
		WifiConfiguration wc = l.get(0); 
		return Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress());
	}
}