package com.max.wifi.event;

public interface ConditionalEvent
{
	abstract boolean startEvent() throws Exception;
	
	abstract boolean checkCondition() throws Exception;
	
	abstract void onTimeout() throws Exception;
	
	abstract String getOperationName();
}