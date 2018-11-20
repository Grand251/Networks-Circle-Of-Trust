# Networks-Circle-Of-Trust
Lab Project for Networks class

## Compile Commands:
  
  javac PortManager.java
  
  javac MessageHandler.java
  
  javac Master.java
  
## Run Commands:

  java Master <port_number>
  
  python Slave.py <master_ip> <master_port>
  
## Usage:
  
  While running, both the slave and master will continuosly request a destination Ring ID and a message to send.
  Recieved messages will be displayed in console.
  
## Working Details:
  
  Master consists of three threads. One handles adding new slaves to the ring. Another handles User Input. 
  The last is responsible for socket/port management.
  Slave consists of two threads. The first handles message management while the second manages the socket/port.
