import java.net.*; // for Socket, ServerSocket, and InetAddress
import java.io.*; // for IOException and Input/OutputStream
import java.nio.ByteBuffer;
import java.net.InetAddress;

public class Master {
	private static final int BUFSIZE = 32; // Size of receive buffer
	public static void main(String[] args) throws IOException {
		if (args.length != 1) // Test for correct # of args
			throw new IllegalArgumentException("Parameter(s): <Port>");
		
		int servPort = Integer.parseInt(args[0]); // Create a server socket to accept client connection requests
		ServerSocket servSock = new ServerSocket(servPort);
		int recvMsgSize; // Size of received message
		byte[] request; // Receive buffer
		byte ringID = 0;
		
		//Starts as Master addr, then will be previous slave
		//lastAddr will also be the next node in the ring
		byte[] lastAddr = InetAddress.getLocalHost().getAddress();
		
		//////////////////////////////////////////////////////////
		//Lab 3: msgHandler manages message input while
		//its subthread portManager manages socket operations
		MessageHandler msgHandler = new MessageHandler(20, 0);
		msgHandler.start();
		//////////////////////////////////////////////////////////
		
		for (;;) { // Run forever, accepting and servicing connections
			request = new byte[BUFSIZE]; // Receive buffer
			Socket clntSock = servSock.accept(); // Get client connection
			//System.out.println("Handling client at " + clntSock.getInetAddress().getHostAddress() + " on port " + clntSock.getPort() + "\n");
			
			byte[] clntAddr = clntSock.getInetAddress().getAddress();

			InputStream in = clntSock.getInputStream();
			OutputStream out = clntSock.getOutputStream();
			
			in.read(request);
			
			int expectedRequestLength = 5;
			
			//DEBUG
			//System.out.print("Request: ");
			//System.out.print(request[0] + " ");
			//for (int i = 1; i < 5; i++)
			//	System.out.print(Integer.toHexString((int)request[i]) + " ");
			//System.out.println();
			
			//Proceed if request looks good
			ringID++;
			byte GID = request[0];
			int responseLength = 10;
			byte[] response = new byte[responseLength];
			response[0] = GID;

			//TODO REVERSE to little endian
			//Validation bytes
			response[1] = 0x4A;
			response[2] = 0x6F;
			response[3] = 0x79;
			response[4] = 0x21;

			response[5] = ringID;
			
			//TODO Reverse
			response[6] = lastAddr[0];
			response[7] = lastAddr[1];
			response[8] = lastAddr[2];
			response[9] = lastAddr[3];
			
			lastAddr = clntAddr;
			
			///////////////////////////////////////////////////////
			//Lab 3: Notify portManager of new next address in ring
			msgHandler.setNextAddr(lastAddr);
			msgHandler.setNextSlaveRID(ringID);
			///////////////////////////////////////////////////////
			
			out.write(response, 0, responseLength);
			clntSock.close(); // Close the socket. We are done with this client!
			System.out.println("		New Slave! Ring ID: " + ringID);
		} /* NOT REACHED */
	}

}
