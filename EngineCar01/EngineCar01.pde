#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

//pin abilitazione
#define PIN_EN 2

//motore avanti/indietro
#define PIN_1A 3
#define PIN_2A 4

//motore sterzo
#define PIN_1B 5
#define PIN_2B 6

//fari
#define PIN_HL 7


//comandi
#define ACTION_DRIVE 0x1
#define ACTION_HEADLIGHTS 0x2
	
#define COMMAND_FORWARD 0x1
#define COMMAND_BACK 0x2
#define COMMAND_RIGHT 0x1
#define COMMAND_LEFT 0x2
#define COMMAND_STOP 0x0

#define COMMAND_ON 0x1
#define COMMAND_OFF 0x0

//connessione con Android USB
AndroidAccessory acc("Manufacturer",
		     "Project01",
		     "Description",
		     "Version",
		     "URI",
		     "Serial");
			
//receive message			
byte rcvmsg[3];

int LOOP_ON = 0;
byte pwmH;
byte pwmL;

//setup
void setup() {
  
  //init seriale e usb
  Serial.begin(19200);
  acc.powerOn();

  //enabler
  pinMode(PIN_EN, OUTPUT);
  
  //avanti/indietro
  pinMode(PIN_1A, OUTPUT);
  pinMode(PIN_2A, OUTPUT);
  
  //sterzo
  pinMode(PIN_1B, OUTPUT);
  pinMode(PIN_2B, OUTPUT);
  
  //fari
  pinMode(PIN_HL, OUTPUT);
}

//avanti
void forward(byte pwm, boolean analog) {
  
  digitalWrite(PIN_1A, HIGH); 
  
  if(analog) 
    analogWrite(PIN_2A, 255-pwm);  
  else
   digitalWrite(PIN_2A, LOW);
}

//indietro
void back(byte pwm, boolean analog) {
  if(analog) 
    analogWrite(PIN_1A, 255-pwm);    
  else 
    digitalWrite(PIN_1A, LOW);
  
  digitalWrite(PIN_2A, HIGH);   
}

//stop avanti-indietro
void stopForwardBack() {
  digitalWrite(PIN_1A, LOW);   
  digitalWrite(PIN_2A, LOW);   
}

//destra
void right(byte pwm, boolean analog) {
  digitalWrite(PIN_1B, HIGH);   
  
  if(analog)
    analogWrite(PIN_2B, 255-pwm);
  else
    digitalWrite(PIN_2B, LOW); 
}

//sinistra
void left(byte pwm, boolean analog) {
  
  if(analog)
    analogWrite(PIN_1B, 255-pwm);  
  else
    digitalWrite(PIN_1B, LOW);
  
  digitalWrite(PIN_2B, HIGH);   
}

//stop destra-sinistra
void stopRightLeft() {
  digitalWrite(PIN_1B, LOW);   
  digitalWrite(PIN_2B, LOW); 
}

//stop all
void stopAll() {
  digitalWrite(PIN_1A, LOW);   
  digitalWrite(PIN_2A, LOW);   
  digitalWrite(PIN_1B, LOW);   
  digitalWrite(PIN_2B, LOW); 
}

//accensione fari
void headlightOn() {
	digitalWrite(PIN_HL, HIGH); 
}

//spegnimento fari
void headlightOff() {
	digitalWrite(PIN_HL, LOW); 
}

//demo
void demo(boolean analog) {
  pwmH = 200;
  pwmL = 120;
  
  if(LOOP_ON > 0) return;
  
  //enabler motori
  digitalWrite(PIN_EN, HIGH);
  
  //avanti
  forward(pwmH, analog);

  //sterzo
  for(int i = 0; i < 10; i++) {
    right(pwmH, analog);
    delay(500);
    left(pwmH, analog);
    delay(500);
  }
  
  //indietro
  back(pwmH, analog);
  
  //sterzo
  for(int i = 0; i < 10; i++) {
    right(pwmH, analog);
    delay(500);
    left(pwmH, analog);
    delay(500);
  }
  
  stopAll();
  delay(10000);
  
  LOOP_ON = 1;
}

//loop
void loop() {
  
  //impostazioni iniziali
  int pwm = 200;
  boolean analog = false;

  //verifica se l'accessorio � connesso
  if (acc.isConnected()) {
	
    //se c'è connessione abilita i motori
    digitalWrite(PIN_EN, HIGH);
  
    //read the received data into the byte array 
    int len = acc.read(rcvmsg, sizeof(rcvmsg), 1);
    if (len > 0) {
      
	//azione di guida
	if (rcvmsg[0] == ACTION_DRIVE) {
        
          //destra o sinistra
          byte rightLeft = rcvmsg[1];
          if(rightLeft == COMMAND_RIGHT) {
            right(pwm, analog);
          } else if(rightLeft == COMMAND_LEFT) {
            left(pwm, analog);
          } else {
            stopRightLeft();
          }
          
          //avanti o indietro
          byte forwardBack = rcvmsg[2];
          if(forwardBack == COMMAND_FORWARD) {
            forward(pwm, analog);
          } else if(forwardBack == COMMAND_BACK) {
            back(pwm, analog);
          } else {
            stopForwardBack();
          }
        }
	  
	//azione di accensione/spegnimento fari
	if (rcvmsg[0] == ACTION_HEADLIGHTS) {
        
          byte state = rcvmsg[1];
          if(state == COMMAND_ON) {
            headlightOn();
          } else {
            headlightOff();
          }
      }
    }
    else {
  	//se non c'� connessione disabilita i motori
  	//digitalWrite(PIN_EN, LOW);
    }
  }
}
