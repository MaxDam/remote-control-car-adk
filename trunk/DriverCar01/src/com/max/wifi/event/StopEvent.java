package com.max.wifi.event;

public class StopEvent
{
	private boolean stop = false;
	
	public StopEvent()
	{
		stop = false;
	}
	
	public void stop()
	{
		stop = true;
	}
	
	public void reset()
	{
		stop = false;
	}
	
	public boolean isStopped()
	{
		return stop;
	}
}
