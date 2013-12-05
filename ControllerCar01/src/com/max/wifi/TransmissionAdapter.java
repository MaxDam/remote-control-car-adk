package com.max.wifi;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.max.wifi.event.NotifierEvent;
import com.max.wifi.event.ObjectHolder;
import com.max.wifi.event.StopEvent;
import com.max.wifi.event.ThreadingEvent;

public class TransmissionAdapter
{
	public final static int MaxPacketSize = 2000;

	final static int SendPacketTimeout = 5000;
	final static int ReceivePacketTimeout = 5000;
	
	final static int WaitTimeSpan = 250;
	
	static int ActionCounter = 0;
	
	static ServerSocket server = null;
	static Socket socket = null;
	
	private static boolean ConditionalThreading(int timeout, int timeSpan, StopEvent stopNotifier, ThreadingEvent event)
	{
		int ActionId = ActionCounter ++;
		
		if (timeSpan < 0)
			timeSpan = WaitTimeSpan;
		
		final ThreadingEvent eventHandler = event;
		
		final NotifierEvent threadNotifier = new NotifierEvent();
		final Thread receiverThread = new Thread(new Runnable()
		{
            public void run()
            {
            	try
            	{
            		eventHandler.startThread();
            		threadNotifier.setResultSuccess();
            	}
            	catch (Exception e)
            	{
            		threadNotifier.setResultFailure(e);
            	}
            }
		});
		
		try
		{
			Log.d("Network action", "Action # " + ActionId + ": " + event.getOperationName() + " [ start ]");
			
			receiverThread.start();
			
			boolean status = false;
			timeout /= timeSpan;
			
			Log.d("Network action", "Action # " + ActionId + ": " + event.getOperationName() + "[ loop ]");
			Log.v("Network action", "Action # " + ActionId + ": " + event.getOperationName() + "[ timeout " + (timeout * timeSpan) + " ]");
			
			while ((stopNotifier == null || !stopNotifier.isStopped()) && !(status = !receiverThread.isAlive()) && (timeout-- > 0))
			{
				try
				{
					Thread.sleep(timeSpan);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				Log.v("Network action", "Action # " + ActionId + ": " + event.getOperationName() + "[ timeout " + (timeout * timeSpan) + " ]");
			}
			
			if (stopNotifier != null && stopNotifier.isStopped())
			{
				Log.i("Network action", "Action # " + ActionId + ": " + event.getOperationName() + " [ cancelled ]");
				
				receiverThread.interrupt();
				CloseConnections();
				
				return false;
			}
			
			if (threadNotifier.getResult() == NotifierEvent.ThreadResult.Failure)
				throw threadNotifier.getException();
			
			if (status)
			{
				Log.i("Network action", "Action # " + ActionId + ": " + event.getOperationName() + " [ success ]");
				
				return true;
			}
			else
			{
				Log.w("Network action", "Action # " + ActionId + ": " + event.getOperationName() + " [ timeout ]");
				
				receiverThread.interrupt();
				CloseConnections();
				
				return false;
			}
		}
		catch (Exception e)
		{
			Log.e("Network action", "Action # " + ActionId + ": " + event.getOperationName() + " [ exception ]");
			
			receiverThread.interrupt();
			CloseConnections();
			
			e.printStackTrace();
			return false;
		}
	}
	
	private static void CloseConnections()
	{
    	try
    	{
    		if (socket != null)
    			socket.close();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}

    	try
    	{
    		if (server != null)
    			server.close();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
	}
	
	//invia un comando
	public static boolean SendCommand(final String command, final String[] arguments, StopEvent stopNotifier, int timeout, int timeSpan)
	{
		final StopEvent stopNotifierHandler = stopNotifier;
    	
	    return ConditionalThreading(((timeout >= 0) ? timeout : SendPacketTimeout), timeSpan, stopNotifier, new ThreadingEvent()
	    {
	    	public void startThread() throws Exception
			{
	    		socket = null;
	            
				try
	    		{
					while (true)
					{
						try
						{
							//socket = new Socket(NetworkAdapter.GetOtherClientIP(), 2012);
							socket = new Socket("10.26.3.110", 2012);
							break;
						}
						catch (Exception e)
						{
		            		e.printStackTrace();
						}
					}
					
					if (stopNotifierHandler != null && stopNotifierHandler.isStopped())
	                	return;
					
					StringBuilder sb = new StringBuilder();
					sb.append(command).append(";");
					for (String s : arguments) 
					{
					    sb.append(s).append(";");
					}
					
					PrintStream outputStream = new PrintStream(socket.getOutputStream());
	            	outputStream.println(sb.toString());
	            }
	            catch (Exception e)
	            {
	            	throw e;
	            }
	            finally
	            {
	            	CloseConnections();
	            }
			}
			
			public String getOperationName()
			{
				return "Sending data...";
			}
	    });
	}

	//riceve un comando
	public static boolean ReceiveCommand(String command, String[] arguments, ObjectHolder<String> senderIPHolder, StopEvent stopNotifier, int timeout, int timeSpan)
	{
		final ObjectHolder<String> senderIPHolderHandler = senderIPHolder;
		final ObjectHolder<String> packetHolder = new ObjectHolder<String>();
		final StopEvent stopNotifierHandler = stopNotifier;
		
	    boolean result = ConditionalThreading(((timeout >= 0) ? timeout : ReceivePacketTimeout), timeSpan, stopNotifier, new ThreadingEvent()
	    {
	    	public void startThread() throws Exception
			{
                server = null;
                socket = null;

                try
                {
                	while (true)
                	{
                		try
                		{
                			server = new ServerSocket(2012);
                			break;
                		}
                		catch (Exception e)
                		{
		            		e.printStackTrace();
                		}
                	}
                	
                	if (stopNotifierHandler != null && stopNotifierHandler.isStopped())
	                	return;
                	
                	socket = server.accept();
                	
                	DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    
                    String inputBuffer = inputStream.readLine();
                    
                    if (senderIPHolderHandler != null)
                    	senderIPHolderHandler.setObject(socket.getInetAddress().getHostAddress());
                    
                    packetHolder.setObject(inputBuffer);
                }
                catch (Exception e)
                {
                	throw e;
                }
                finally
                {
                	CloseConnections();
                }
			}
			
			public String getOperationName()
			{
				return "Receiving data...";
			}
	    });
	    
	    if (result)
	    {
	    	try
	    	{
	    		String[] args = packetHolder.getObject().split(";");
	    		if(args.length > 0) {
	    			command = args[0];
	    			
	    			arguments = new String[args.length - 1];
	    			System.arraycopy(args, 1, arguments, 0, args.length - 1);
	    		}
	    	}
	    	catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    }
	    
	    return result;
	}
	
	public static boolean SendPackets(byte[] packet, StopEvent stopNotifier, int timeout, int timeSpan)
	{
    	final byte[] packetHandler = packet;
		final StopEvent stopNotifierHandler = stopNotifier;
    	
	    return ConditionalThreading(((timeout >= 0) ? timeout : SendPacketTimeout), timeSpan, stopNotifier, new ThreadingEvent()
	    {
	    	public void startThread() throws Exception
			{
	    		socket = null;
	            
				try
	    		{
					while (true)
					{
						try
						{
							socket = new Socket(NetworkAdapter.GetOtherClientIP(), 2012);
							break;
						}
						catch (Exception e)
						{
		            		// Temporary workaround
		            		//e.printStackTrace();
						}
					}
					
					if (stopNotifierHandler != null && stopNotifierHandler.isStopped())
	                	return;
					
	    			OutputStream outputStream = socket.getOutputStream();
	            	outputStream.write(packetHandler);
	            }
	            catch (Exception e)
	            {
	            	throw e;
	            }
	            finally
	            {
	            	CloseConnections();
	            }
			}
			
			public String getOperationName()
			{
				return "Sending data...";
			}
	    });
	}

	public static boolean ReceivePackets(byte[] packet, ObjectHolder<String> senderIPHolder, StopEvent stopNotifier, int timeout, int timeSpan)
	{
		final ObjectHolder<String> senderIPHolderHandler = senderIPHolder;
		final ObjectHolder<byte[]> packetHolder = new ObjectHolder<byte[]>();
		final StopEvent stopNotifierHandler = stopNotifier;
		
	    boolean result = ConditionalThreading(((timeout >= 0) ? timeout : ReceivePacketTimeout), timeSpan, stopNotifier, new ThreadingEvent()
	    {
	    	public void startThread() throws Exception
			{
                server = null;
                socket = null;

                try
                {
                	while (true)
                	{
                		try
                		{
                			server = new ServerSocket(2012);
                			break;
                		}
                		catch (Exception e)
                		{
		            		// Temporary workaround
		            		//e.printStackTrace();
                		}
                	}
                	
                	if (stopNotifierHandler != null && stopNotifierHandler.isStopped())
	                	return;
                	
                	socket = server.accept();
                	
                    InputStream inputStream = socket.getInputStream();
                    
                    byte[] inputBuffer = new byte[MaxPacketSize];
                    
                    inputStream.read(inputBuffer);
                    
                    if (senderIPHolderHandler != null)
                    	senderIPHolderHandler.setObject(socket.getInetAddress().getHostAddress());
                    
                    packetHolder.setObject(inputBuffer);
                }
                catch (Exception e)
                {
                	throw e;
                }
                finally
                {
                	CloseConnections();
                }
			}
			
			public String getOperationName()
			{
				return "Receiving data...";
			}
	    });
	    
	    if (result)
	    {
	    	try
	    	{
	    		for (int i = 0; i < packet.length; i ++)
	    			packet[i] = packetHolder.getObject()[i];
	    	}
	    	catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    }
	    
	    return result;
	}
}
