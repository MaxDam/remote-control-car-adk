package com.max.wifi.event;

public interface ThreadingEvent
{
	abstract void startThread() throws Exception;
	
	abstract String getOperationName();
}