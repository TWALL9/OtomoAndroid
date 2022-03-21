/* Serial Loop */

#include <SoftwareSerial.h>

#define rxPin 2
#define txPin 3

SoftwareSerial bluetoothSerial(rxPin, txPin); // RX, TX
char myChar;

void setup() {
  Serial.begin(9600);   
  Serial.println("AT");

  bluetoothSerial.begin(38400); // ZS-040 units start at 38400 baud
  bluetoothSerial.println("AT");
}

void loop() {
  while (bluetoothSerial.available()) {
    myChar = bluetoothSerial.read();
    Serial.print(myChar);
  }

  while (Serial.available()) {
    myChar = Serial.read();
    Serial.print(myChar); //echo
    bluetoothSerial.print(myChar);
  }
}
