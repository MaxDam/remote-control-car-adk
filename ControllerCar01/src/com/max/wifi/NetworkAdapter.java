package com.max.wifi;

import java.lang.reflect.Method;

import com.max.wifi.event.CallbackEvent;
import com.max.wifi.event.ConditionalEvent;
import com.max.wifi.event.StopEvent;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;

public class NetworkAdapter
{
	private static String OtherName = "---";
	static String OtherClientIP = "0.0.0.0";
	
	final static String NetworkSSID = "NetworkCar01";
	final static String NetworkKey = "networkcar";

	final static int StartWifiTimeout = 10000;
	final static int StopWifiTimeout = 10000;
	final static int StartApTimeout = 10000;
	final static int StopApTimeout = 10000;
	final static int BeginScanTimeout = 5000;
	final static int BeginConnectionTimeout = 30000;
	
	final static int WaitTimeSpan = 250;
	
	static int ActionCounter = 0;

	private static boolean ConditionalWait(int timeout, CallbackEvent statusCallback, StopEvent stopNotifier, ConditionalEvent event)
	{
		int ActionId = ActionCounter ++;
		
		try
		{
			Log.d("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ precondition ]");
			
			if (event.checkCondition())
				return true;

			Log.d("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ start ]");
			
			if (!event.startEvent())
				return false;

			statusCallback.onCallback(event.getOperationName());
			
			boolean status = false;
			timeout /= WaitTimeSpan;
			
			Log.d("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + "[ loop ]");
			Log.v("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + "[ timeout " + (timeout * WaitTimeSpan) + " ]");
			
			while ((stopNotifier == null || !stopNotifier.isStopped()) && !(status = event.checkCondition()) && (timeout-- > 0))
			{
				try
				{
					Thread.sleep(WaitTimeSpan);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				Log.v("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + "[ timeout " + (timeout * WaitTimeSpan) + " ]");
			}
			
			if (stopNotifier != null && stopNotifier.isStopped())
			{
				Log.i("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ cancelled ]");
				
				event.onTimeout();
				return false;
			}
			
			if (status)
			{
				Log.i("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ success ]");
				
				return true;
			}
			else
			{
				Log.w("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ timeout ]");
				
				event.onTimeout();
				return false;
			}
		}
		catch (Exception e)
		{
			Log.e("Wifi action", "Action # " + ActionId + ": " + event.getOperationName() + " [ exception ]");
			
			try
			{
				event.onTimeout();
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
			}
			
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean StartWifi(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
    	final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	final Context contextHandler = context;
    	final CallbackEvent statusCallbackHandler = statusCallback;
    	final StopEvent stopNotifierHandler = stopNotifier;
    	
	    return ConditionalWait(StartWifiTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				if (!StopAccessPoint(contextHandler, statusCallbackHandler, stopNotifierHandler))
		        	return false;
				
				return wifiManager.setWifiEnabled(true);
			}

			public boolean checkCondition() throws Exception
			{
				return (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
			}

			public void onTimeout() throws Exception
			{
				StopWifi(contextHandler, statusCallbackHandler, null);
			}
			
			public String getOperationName()
			{
				return "Turning WiFi on...";
			}
	    });
	}

	public static boolean StopWifi(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
    	final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	
	    return ConditionalWait(StopWifiTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				return wifiManager.setWifiEnabled(false);
			}

			public boolean checkCondition() throws Exception
			{
				return (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED);
			}

			public void onTimeout() throws Exception
			{
			}
			
			public String getOperationName()
			{
				return "Turning WiFi off...";
			}
	    });
	}
	
	public static boolean StartAccessPoint(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
	    final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		final Context contextHandler = context;
		final CallbackEvent statusCallbackHandler = statusCallback;
		final StopEvent stopNotifierHandler = stopNotifier;
		
	    return ConditionalWait(StartApTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				if (!StopWifi(contextHandler, statusCallbackHandler, stopNotifierHandler))
		        	return false;
				
				Method SetWifiApEnabled = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
				
				WifiConfiguration netConfig = new WifiConfiguration();
				netConfig.SSID = NetworkSSID;
				netConfig.preSharedKey = NetworkKey;
				netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
				netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
				netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				
				return (Boolean)SetWifiApEnabled.invoke(wifiManager, netConfig, true);
			}
	
			public boolean checkCondition() throws Exception
			{
				Method IsWifiApEnabled = wifiManager.getClass().getMethod("isWifiApEnabled");
				
				return ((Boolean)IsWifiApEnabled.invoke(wifiManager));
			}
	
			public void onTimeout() throws Exception
			{
				StopAccessPoint(contextHandler, statusCallbackHandler, null);
			}
			
			public String getOperationName()
			{
				return "Turning AP on...";
			}
	    });
	}

	public static boolean StopAccessPoint(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
	    final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

	    return ConditionalWait(StopApTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				Method SetWifiApEnabled = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
				
		        return (Boolean)SetWifiApEnabled.invoke(wifiManager, null, false);
			}

			public boolean checkCondition() throws Exception
			{
				Method IsWifiApEnabled = wifiManager.getClass().getMethod("isWifiApEnabled");
				
				return (!(Boolean)IsWifiApEnabled.invoke(wifiManager));
			}

			public void onTimeout() throws Exception
			{
			}
			
			public String getOperationName()
			{
				return "Turning AP off...";
			}
	    });
	}

	public static boolean BeginNetworkScan(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
		final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		final Context contextHandler = context;
		final CallbackEvent statusCallbackHandler = statusCallback;
		final StopEvent stopNotifierHandler = stopNotifier;
		
	    return ConditionalWait(BeginScanTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				if (!StartWifi(contextHandler, statusCallbackHandler, stopNotifierHandler))
		        	return false;
				
		        return wifiManager.startScan();
			}
	
			public boolean checkCondition() throws Exception
			{
				if (wifiManager.getScanResults() != null)
				{
			    	for (ScanResult result : wifiManager.getScanResults())
			    	{
			    		if (result.SSID.equals(NetworkSSID) || result.SSID.equals('"' + NetworkSSID + '"'))
			    			return true;
			    	}
				}
		    	
		    	return false;
			}
	
			public void onTimeout() throws Exception
			{
				StopWifi(contextHandler, statusCallbackHandler, null);
			}
			
			public String getOperationName()
			{
				return "Scanning for network...";
			}
	    });
	}
	
	public static boolean BeginNetworkConnection(Context context, CallbackEvent statusCallback, StopEvent stopNotifier)
	{
    	final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	final Context contextHandler = context;
    	final CallbackEvent statusCallbackHandler = statusCallback;
    	final StopEvent stopNotifierHandler = stopNotifier;
    	
	    return ConditionalWait(BeginConnectionTimeout, statusCallback, stopNotifier, new ConditionalEvent()
	    {
	    	public boolean startEvent() throws Exception
			{
				if (!BeginNetworkScan(contextHandler, statusCallbackHandler, stopNotifierHandler))
		        	return false;
				
				if (wifiManager.getConfiguredNetworks() != null)
				{
					for (WifiConfiguration Network : wifiManager.getConfiguredNetworks())
					{
						if (Network.SSID.equals(NetworkSSID) || Network.SSID.equals('"' + NetworkSSID + '"'))
						{
							wifiManager.removeNetwork(Network.networkId);
							break;
						}
					}
				}
				
		        WifiConfiguration netConfig = new WifiConfiguration();
		        netConfig.SSID = '"' + NetworkSSID + '"';
		        netConfig.preSharedKey = '"' + NetworkKey + '"';
		        
		        int networkId = wifiManager.addNetwork(netConfig);
		        
		        if (networkId == -1)
		        	return false;
		        
		        return wifiManager.enableNetwork(networkId, true);
			}

			public boolean checkCondition() throws Exception
			{
				if (wifiManager.getConnectionInfo() == null)
					return false;
				
				if (wifiManager.getConnectionInfo().getSSID() == null)
					return false;
				
				if (!wifiManager.getConnectionInfo().getSSID().equals(NetworkSSID) &&
						!wifiManager.getConnectionInfo().getSSID().equals('"' + NetworkSSID + '"'))
					return false;
				
				if (connectivityManager.getActiveNetworkInfo() == null)
					return false;
				
				if (!connectivityManager.getActiveNetworkInfo().isConnected())
					return false;
				
				return true;
			}

			public void onTimeout() throws Exception
			{
				StopWifi(contextHandler, statusCallbackHandler, null);
			}
			
			public String getOperationName()
			{
				return "Connecting to network...";
			}
	    });
	}

	public static String GetOtherClientIP()
	{
		return OtherClientIP;
	}

	public static void SetOtherClientIP(String address)
	{
		OtherClientIP = address;
	}

	public static String GetUserName(Context context)
	{
		if (PreferenceManager.getDefaultSharedPreferences(context) == null)
			return "User";
		
		return PreferenceManager.getDefaultSharedPreferences(context).getString("nickNamePref", "User");
	}
	
	public static String GetOtherName()
	{
		return OtherName;
	}

	public static void SetOtherName(String name)
	{
		OtherName = name;
	}
	
	public static String GetServerIP(Context context)
	{
    	final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    	
    	if (wifiManager.getDhcpInfo() == null)
    		return "0.0.0.0";
    	
    	return Formatter.formatIpAddress(wifiManager.getDhcpInfo().gateway);
	}
}
