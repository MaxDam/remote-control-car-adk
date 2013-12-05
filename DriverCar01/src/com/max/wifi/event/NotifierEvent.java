package com.max.wifi.event;

public class NotifierEvent
{
	public enum ThreadResult { Pending, Success, Failure }
	
	private ThreadResult result = null;
	
	private Exception exception = null;
	
	public NotifierEvent()
	{
		result = ThreadResult.Pending;
	}
	
	public void setResultSuccess()
	{
		if (result == ThreadResult.Pending)
			result = ThreadResult.Success;
	}
	
	public void setResultFailure(Exception e)
	{
		if (result == ThreadResult.Pending)
		{
			result = ThreadResult.Failure;
			exception = e;
		}
	}
	
	public ThreadResult getResult()
	{
		return result;
	}
	
	public Exception getException()
	{
		return exception;
	}
}
