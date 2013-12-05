package com.max;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

public class CommandServer extends AsyncTask <Integer, Integer, Void>{
	    
	private int port = 9876;
	
	private Context context = null;
	private Handler handler;
	
	public CommandServer(Context context, Handler handler){
		this.context = context;
		this.handler = handler;
    }

    protected void onPreExecute(){
        //Create the notification in the statusbar
    }

    @Override
    protected Void doInBackground(Integer... integers) {
        //This is where we would do the actual download stuff
        //for now I'm just going to loop for 10 seconds
        // publishing progress every second
    	
    	try {
	    	DatagramSocket serverSocket;
			serverSocket = new DatagramSocket(port);
	        byte[] receiveData = new byte[1024];
	        while(true)
	        {
	    		try {
	    			//riceve il packet
		        	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		        	serverSocket.receive(receivePacket);
		        	String payload = new String(receivePacket.getData());
		        	
		        	//invia il messaggio al gestore
		        	Message cmdMessage = handler.obtainMessage();
		        	cmdMessage.obj = payload;
		        	handler.sendMessage(cmdMessage);
		        	
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
	        }
    	} catch (Exception e) {
			e.printStackTrace();
		}
	

        return null;
    }

    protected void onProgressUpdate(Integer... progress) {
        //This method runs on the UI thread, it receives progress updates
        //from the background thread and publishes them to the status bar
    }

    protected void onPostExecute(Void result)    {
        //The task is complete, tell the status bar about it
    }
}