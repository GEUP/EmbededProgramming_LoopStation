import paho.mqtt.client as mqtt
import time

mqtt = mqtt.Client("LoopStation")
mqtt.connect("localhost",1883)

while True:
	mqtt.publish("CTRL-MIKE","MIKE ON")
	time.sleep(5)
	mqtt.publish("CTRL-MIKE","MIKE OFF")
	time.sleep(5)
