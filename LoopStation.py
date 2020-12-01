import paho.mqtt.client as mqtt
import time

def on_connect(client,userdata,flags,rc):
    client.subscribe("SOURCE")
    
def on_message(client,userdata,msg):
    print("message arrive")
    mqttClient.publish("CTRL-SPEAKER",msg,payload)
    f=open('source.3gpp','w')
    #f.write(msg.payload)
    f.close()

mqttClient = mqtt.Client("LoopStation")
mqttClient.on_connect=on_connect
mqttClient.on_message = on_message
mqttClient.connect("localhost",1883)

mqttClient.loop_start()
while True:
    mqttClient.publish("CTRL-MIKE","MIKE ON")
    time.sleep(10)
