#include "DHT.h"
#include <Servo.h>
#include <SoftwareSerial.h>

SoftwareSerial mySerial(10, 11); // RX, TX
#define DHTPIN 2     // what digital pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
DHT dht(DHTPIN, DHTTYPE);
Servo myservo;  // create servo object to control a servo
int pos = 0;    // variable to store the servo position
int lightPin = 6;
int curtainPin = 5;
int doorPin = 4;
boolean doorPos = true;
 const int analogInPin0 = A0;// Analog input pins
void setup() {
  Serial.begin(9600);
  dht.begin();
  mySerial.begin(38400);
  myservo.attach(curtainPin);  // attaches the servo on pin 9 to the servo object
  pinMode(lightPin, OUTPUT);
  pinMode(doorPin, OUTPUT);
}

void loop() {
 myservo.detach();
 //send data every second
  dataSend();
 //if data available read
  if (mySerial.available() > 0)
  {
    String data = mySerial.readString();
    Serial.println(data);
    Serial.println(data.substring(0, data.indexOf("light")));
    if (data.endsWith("light")) {
      analogWrite(lightPin, data.substring(0, data.indexOf("light")).toInt());
    }
    if (data.endsWith("servo")) {
      myservo.attach(curtainPin);  // attaches the servo on pin 9 to the servo object
      myservo.write(data.substring(0, data.indexOf("servo")).toInt());
      Serial.println(myservo.read() );
    }
    if (data.equals("door")) {
      if (doorPos) {
        digitalWrite(doorPin, HIGH);
        doorPos = false;
      }
      else {
        digitalWrite(doorPin, LOW);
        doorPos = true;
      }
    }
  }
  delay(1000);
}
void dataSend() {
  float sensorValue = analogRead(analogInPin0);
  float voltage = ((sensorValue / 1023) * 5);
  Serial.print("Voltage : ");
  Serial.print(voltage);
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  // Read temperature as Fahrenheit (isFahrenheit = true)
  if (isnan(h) || isnan(t)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }
  Serial.print("Temperature: ");
  Serial.print(t);
  Serial.println(" *C ");
  String sendData="#"+String(t)+String(h)+String(voltage)+"55.5"+"~";
  mySerial.println(sendData);
  delay(20);
}

