import socket
import sys
import threading


GID = 20
MASTER_IP = sys.argv[1]
MASTER_PORT = int(sys.argv[2])
#MASTER_PORT = 10110
BUFFER_SIZE = 1024

my_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
my_socket.connect((MASTER_IP, MASTER_PORT))

request = bytearray()
request.append(20)
request.append(0x4A)
request.append(0x6F)
request.append(0x79)
request.append(0x21)

my_socket.send(request)

response_length = 10
response = my_socket.recv(response_length)
temp = bytearray()
temp.extend(response)
response = temp

print("GID: " + str(response[0]))

if (response[1] != 0x4A or response[2] != 0x6F or response[3] != 0x79 or response[4] != 0x21):
    raise Exception("Validation bits not correct!\n")

RING_ID = response[5]

NEXT_SLAVE_ADDR = str(response[6]) + "." + str(response[7]) + "."
NEXT_SLAVE_ADDR += str(response[8]) + "." + str(response[9])

print("Next Slave: " + NEXT_SLAVE_ADDR)

MY_ADDR = my_socket.getsockname()[0]
print("My Addr: " + MY_ADDR)

my_socket.close()

#Now Enter ring

MY_PORT = MASTER_PORT + RING_ID
NEXT_SLAVE_PORT = MY_PORT - 1


#Slave socket is set up and ready to send/recieve.
#For all intensive purposes, it is now part of the ring
#It knows the address of the next slave next_slave


class PortManager(threading.Thread):
    def __init__(self, MY_ADDR, MY_PORT, MY_RID, NEXT_SLAVE_ADDR):
        threading.Thread.__init__(self)
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.bind((MY_ADDR, MY_PORT))
        self.socket.settimeout(2.0)
        
        self.messageToSend = ""
        self.MY_ADDR = MY_ADDR
        self.NEXT_SLAVE_ADDR = NEXT_SLAVE_ADDR
        self.MY_PORT = MY_PORT
        self.MY_RID = MY_RID
        self.destRID = 0
        #print("PortManager Started!");
    
    def setMessageToSend(self, message):
        self.messageToSend = message
    
    def setDestRID(self, RID):
        self.destRID = RID
    
    def run(self):
        while True:
            if (self.messageToSend != ""):
                buf = bytearray(73)
                buf[0] = 20
                buf[1] = 0x4A
                buf[2] = 0x6F
                buf[3] = 0x79
                buf[4] = 0x21
                buf[5] = 127
                buf[6] = self.destRID
                buf[7] = self.MY_RID

                messageBytes = bytes(self.messageToSend, 'ascii')
                for i in range(len(self.messageToSend)) :
                    buf[i + 8] = messageBytes[i]
                checksum = 100
                buf[len(self.messageToSend) + 8] = checksum
                #print('Buf to send Prepared!')
                self.socket.sendto(buf, (self.NEXT_SLAVE_ADDR, self.MY_PORT - 1))
                self.messageToSend = ""
            #Process Incoming
            try:
                buf, sender = self.socket.recvfrom(73)
                buf = bytearray(buf)
                #print("Packet Recieved of length: " + str(len(buf)))
                #print(buf[0])
                checksumIndex = 0
                if (buf[6] == self.MY_RID):
                    for i in list(range(8, len(buf)))[::-1]:
                        if (buf[i] != 0):
                            checksumIndex = i
                            break
                    messageBytes = buf[8 : checksumIndex]
                    message = ""
                    for i in range(len(messageBytes)):
                        message += chr(messageBytes[i])
                    print("        --- New Message From Ring ID: " + chr(buf[7]) + " ---")
                    print("        | " + str(message))
                    print("        ----------------------------------")
                else:
                    #print("Packet Being Forwarded!")
                    #print(buf)
                    buf[5] = buf[5] - 1
                    checksum = 100
                    self.socket.sendto(buf, (self.NEXT_SLAVE_ADDR, self.MY_PORT - 1))

                    #print("Packet Forwarded")


            except Exception as e:
                #print(str(e))
                pass
                

pm = PortManager(MY_ADDR, MY_PORT, RING_ID, NEXT_SLAVE_ADDR)
pm.start()
while True:
    print("Destination Ring ID:")
    pm.setDestRID(int(input("")))
    print("Message To Send: ")
    pm.setMessageToSend(input(""))



