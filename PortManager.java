import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.Arrays;

public class PortManager extends Thread {
	private DatagramSocket socket;
	private InetAddress nextAddr;
	private int nextSlaveRID;
	private int portNumber;
	private String messageToSend;
	private byte destRID;

	public PortManager(int portNumber, byte[] nextAddr) {
		super();
		this.messageToSend = "";
		try{ 
			this.socket = new DatagramSocket(portNumber);
			//PortManager will listen to the socket for 1 sec before
			//moving onto other tasks
			this.socket.setSoTimeout(1000);
			this.nextAddr = InetAddress.getByAddress(nextAddr);
			this.portNumber = portNumber;
			this.destRID = 0;
			this.nextSlaveRID = 0;
		} catch(Exception e) {}
	}
	public void setNextSlaveRID(int RID) {
		this.nextSlaveRID = RID;
	}
	public void setNextAddr(byte[] addr) {
		try {
			this.nextAddr = InetAddress.getByAddress(addr);
		} catch (Exception e) {}
	}
	public void setMessageToSend(String message) {
		this.messageToSend = message;
	}
	public void setDestRID(int destRID) {
		this.destRID = (byte)destRID;
	}
	//Take in message and package it into a DatagramPacket
	private DatagramPacket preparePacket(String messageToSend) {
		int bufLen = 73;
		byte[] buf = new byte[bufLen];
		buf[0] = (byte) 20;
		buf[5] = (byte)127; //TTL
		byte[] messageBytes = messageToSend.getBytes();
		for (int i = 0; i < messageBytes.length && i < 64; i++ )
			buf[i + 8] = messageBytes[i];
		
		byte checksum = 100;//TODO
		buf[messageBytes.length + 8] = checksum;

		//DEBUG
		//System.out.print("PACKET Prepared with buf: ");
		//for (int i = 0; i < 25; i++) 
		//	System.out.print(" " + buf[i]);
		//System.out.println();
		buf[6] = this.destRID;

		return new DatagramPacket(buf, bufLen, this.nextAddr, this.portNumber + this.nextSlaveRID);
	}
	//Check if Ring ID is zero. If so print message contents.
	//Otherwise Decrement TTL, Recompute Checksum, Forward Packet
	private void processReceivedPacket(DatagramPacket packet) {
		byte[] buf = packet.getData();
		int bufLen = packet.getLength();
		//System.out.println("Just recived a packet of length: " + bufLen);
		
		//Find checksum index
		int checksumIndex = 0;
		for (int i = bufLen - 1; i >= 8; i--) {
			if (buf[i] != 0) {
				checksumIndex = i;
				break;
			}
		}
		//If ChecksumIndex is not found, packet is invalid.
		//if (checksumIndex == 0)
		//	return;
		byte RIDDest = buf[6];
		byte RIDSource = buf[7];
		if (RIDDest == 0) {
			String message = new String(Arrays.copyOfRange(buf, 8, checksumIndex));
			System.out.println("		---- New Message From Ring ID: " + (RIDSource & 0xFF) + " ----");
			System.out.println("		| " + message);
			System.out.println("		--------------------------------------");
			return;
		}
		
		//Prepare packet for forwarding
		byte TTL = buf[5];
		if (TTL == 0)
			return;
		
		TTL--;
		buf[5] = TTL;
		byte checksum = 0;//TODO
		packet = new DatagramPacket(buf, bufLen, this.nextAddr, this.portNumber + 1);
		try {this.socket.send(packet);} catch (Exception e) {}

	}
	public void run() { //Print and Send Messages
		while(true) {
			if (messageToSend != "") {

				//DEBUG
				//System.out.println("PortManager received message to send: " + messageToSend);

				DatagramPacket packet = preparePacket(messageToSend);
				//Reset Message Variable
				messageToSend = "";
				try {this.socket.send(packet);} catch (Exception e) {
					System.out.println("Packet Failed to send!");
				}
			}
			//Recive read/forward packets with timeout
			try {
				byte[] buf = new byte[73]; //9 + 64
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				this.socket.receive(packet);
				processReceivedPacket(packet);

			} catch (SocketTimeoutException e) {}
			catch (Exception e) {}

		}
	}
}
