package com.max.wifi.event;

public class ObjectHolder<Type>
{
    private Type value = null;
    
    public void setObject(Type newObject)
    {
    	value = newObject;
    }
    
    public Type getObject()
    {
    	return value;
    }
}