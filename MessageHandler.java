import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.util.Scanner;

public class MessageHandler extends Thread {
	private PortManager pm;

	public MessageHandler(int GID, int RID) {
		super();
		try{
			int portNumber = 10010 + ((GID % 30) * 5);
			byte[] nextAddr = InetAddress.getLocalHost().getAddress();
			this.pm = new PortManager(portNumber, nextAddr);
			this.pm.start();
		}
		catch (Exception e) {}
	}
	public void setNextSlaveRID(int RID) {
		this.pm.setNextSlaveRID(RID);
	}
	//For when the Master adds another Slave to the ring
	public void setNextAddr(byte[] addr) {
		this.pm.setNextAddr(addr);
	}
	
	public void run() {
		Scanner scanner = new Scanner(System.in);
		while(true) {
			System.out.println("Destination RID:");
			this.pm.setDestRID(Integer.parseInt(scanner.nextLine()));
			System.out.println("Message to Send:");
			//pm will send the message when it is ready
			this.pm.setMessageToSend(scanner.nextLine());
		}
	}
}
