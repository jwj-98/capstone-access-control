#include <LiquidCrystal_I2C.h>
//#include <LiquidCrystal.h>
#include <Wire.h>
LiquidCrystal_I2C lcd(0x27, 16, 2);
#include <Servo.h> 
Servo myservo; 
int cont=0;
int LED=12;
int LED2=13;
//int rele=A2;
int trig=0;
char sig='a';
#include <Adafruit_Fingerprint.h>

SoftwareSerial mySerial(2, 3);
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&mySerial);

void setup()  
{
  //lcd.begin(16, 2);   
  lcd.init();
  lcd.backlight();
  lcd.setCursor(0,0);
  lcd.clear();
  myservo.attach(A2);     
  pinMode(LED,OUTPUT);
  pinMode(LED2,OUTPUT);
  //pinMode(rele,OUTPUT);
  //digitalWrite(rele,0);
  Serial.begin(9600);
  
  while (!Serial);  // For Yun/Leo/Micro/Zero/...
  delay(100);
  
  Serial.println("\n\nfinger detecter");
  lcd.print("finger detecter");
   // set the data rate for the sensor serial port
  finger.begin(57600);
 
  if (finger.verifyPassword()) {
    Serial.println("Found fingerprint sensor!");
  } 
  else {
    Serial.println("Did not find fingerprint sensor :(");
    while (1) { delay(1); }
  }

  finger.getTemplateCount();
  Serial.print("Sensor contains "); 
  Serial.print(finger.templateCount); 
  Serial.println(" templates");
  Serial.println("Waiting for valid finger...");
  lcd.clear();

  myservo.write(90);
  }

void loop()                     // run over and over again
{
  //Serial.print("loop");
  if(getFingerprintIDez() == -2){
    Serial.println();
    Serial.println("fx");
  }

  /* 문자열로 저장*/
  while(Serial.available()){
     sig = Serial.read();
     Serial.println("new char");
     Serial.println(sig);
     if(sig == 'x'){
       Serial.println("de");
       openD();
       sig = 0;
     }
   }
   
   
  
  //digitalWrite(rele,cont);
  lcd.setCursor(0, 0);
  lcd.print("Found ID #"); 
  lcd.print(finger.fingerID);  
  
  if(cont == 1){
    if(trig){
      openD();
    }
  }
  else{
    if(trig){
      openD();
    }
  }
  //don't ned to run this at full speed.
}

void openD() {
      lcd.setCursor(0, 1);
      lcd.print("POWER = ON  ");
      myservo.write(0);
      digitalWrite(LED,HIGH);
      digitalWrite(LED2,LOW);
      delay(3000);
      lcd.setCursor(0, 1);
      lcd.print("POWER = OFF ");
      myservo.write(90);
      digitalWrite(LED,LOW);
      digitalWrite(LED2,HIGH);
      trig = 0;
}

uint8_t getFingerprintID() {
  uint8_t p = finger.getImage();
  switch (p) {
    case FINGERPRINT_OK:
      //Serial.println("Image taken");
      break;
    case FINGERPRINT_NOFINGER:
      //Serial.println("No finger detected");
      return p;
    case FINGERPRINT_PACKETRECIEVEERR:
      //Serial.println("Communication error");
      return p;
    case FINGERPRINT_IMAGEFAIL:
      //Serial.println("Imaging error");
      return p;
    default:
      //Serial.println("Unknown error");
      return p;
  }

  // OK success!

  p = finger.image2Tz();
  switch (p) {
    case FINGERPRINT_OK:
      //Serial.println("Image converted");
      break;
    case FINGERPRINT_IMAGEMESS:
      //Serial.println("Image too messy");
      return p;
    case FINGERPRINT_PACKETRECIEVEERR:
      //Serial.println("Communication error");
      return p;
    case FINGERPRINT_FEATUREFAIL:
      //Serial.println("Could not find fingerprint features");
      return p;
    case FINGERPRINT_INVALIDIMAGE:
      //Serial.println("Could not find fingerprint features");
      return p;
    default:
      //Serial.println("Unknown error");
      return p;
  }
  
  // OK converted!
  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    //Serial.println("Found a print match!");
  } else if (p == FINGERPRINT_PACKETRECIEVEERR) {
    //Serial.println("Communication error");
    return p;
  } else if (p == FINGERPRINT_NOTFOUND) {
    //Serial.println("Did not find a match");
    return p;
  } else {
    //Serial.println("Unknown error");
    return p;
  }   
  
  // found a match!
  Serial.print("Found ID #"); Serial.print(finger.fingerID); 
  Serial.print(" with confidence of "); Serial.println(finger.confidence); 

  return finger.fingerID;
}

// returns -1 if failed, otherwise returns ID #
int getFingerprintIDez() {
  delay(500);
  uint8_t p = finger.getImage();
  if (p != FINGERPRINT_OK)  return -1;
  
  p = finger.image2Tz();
  if (p != FINGERPRINT_OK)  return -1;
  
  p = finger.fingerFastSearch();
  if (p != FINGERPRINT_OK)  return -2;
  
  // found a match!
  
  Serial.print("Found ID #"); Serial.print(finger.fingerID); 
  Serial.print(" with confidence of "); Serial.println(finger.confidence);
  if(finger.confidence >= 100){
      Serial.println("Identical" );
      cont=!cont;
      trig = 1;
      Serial.print("fo#");
      Serial.println(finger.fingerID);
   }
  else{
    Serial.println("retouch");
  }
  
  Serial.println(cont);
  return finger.fingerID;
  
}
