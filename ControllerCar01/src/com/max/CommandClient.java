package com.max;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class CommandClient {
	
	private String serverIp = "";
	private int port = 9876;
	
	public CommandClient() {
	}
	
	public void sendCommand(String commandData) throws Exception {

		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName(serverIp);
		byte[] sendData = new byte[1024];

		sendData = commandData.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		clientSocket.send(sendPacket);
		
		clientSocket.close();
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
}
