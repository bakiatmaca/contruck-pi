import os
import glob
import time
import RPi.GPIO as GPIO
from bluetooth import *

GPIO.cleanup()

#GPIO numbers
GPIO_FORWARD = 22
GPIO_BACK = 23
GPIO_LEFT = 25
GPIO_RIGHT = 24

print "contruck-pi server is starting..."

GPIO.setmode(GPIO.BCM)
GPIO.setup(GPIO_FORWARD, GPIO.OUT)
GPIO.setup(GPIO_BACK, GPIO.OUT)
GPIO.setup(GPIO_LEFT, GPIO.OUT)
GPIO.setup(GPIO_RIGHT, GPIO.OUT)

GPIO.output(GPIO_FORWARD,False)
GPIO.output(GPIO_BACK,False)
GPIO.output(GPIO_LEFT,False)
GPIO.output(GPIO_RIGHT,False)

server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service( server_sock, "contruck-pi-server",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
#                   protocols = [ OBEX_UUID ] 
                    )

while True:    
	try:      
		print "Waiting for connection on ConTruck-PI channel %d" % port
		client_sock, client_info = server_sock.accept()
		print "Accepted connection from ", client_info
		break

	except IOError:
		pass
	except KeyboardInterrupt:
		print "stoped."
		break
	
if ((client_sock is None) == False):
	while True:          
		try:
			data = client_sock.recv(1024)
			if len(data) == 0: pass
			print "received [%s]" % data
	
			if data == 'forward':
				GPIO.output(GPIO_BACK,False)
				GPIO.output(GPIO_FORWARD,True)
			elif data == 'back':
				GPIO.output(GPIO_FORWARD,False)
				GPIO.output(GPIO_BACK,True)
			elif data == 'left':
				GPIO.output(GPIO_RIGHT,False)
				GPIO.output(GPIO_LEFT,True)
			elif data == 'right':
				GPIO.output(GPIO_LEFT,False)
				GPIO.output(GPIO_RIGHT,True)


			if data == 'forward-clr':
				GPIO.output(GPIO_FORWARD,False)
			elif data == 'back-clr':
				GPIO.output(GPIO_BACK,False)
			elif data == 'left-clr':
				GPIO.output(GPIO_LEFT,False)
			elif data == 'right-clr':
				GPIO.output(GPIO_RIGHT,False)
			else:
				data = 'invalid command' 

			#client_sock.send(data)
			#print "sending [%s]" % data
	
		except IOError:
			GPIO.output(GPIO_FORWARD,False)
			GPIO.output(GPIO_BACK,False)
			GPIO.output(GPIO_LEFT,False)
			GPIO.output(GPIO_RIGHT,False)
			print "IOError occurred"
			pass
	
		except KeyboardInterrupt:
			print "disconnected"
	
			client_sock.close()
			server_sock.close()
			print "socket is closed"
			break

GPIO.cleanup()