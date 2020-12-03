# step-count-app

* Created an IOT project that uses the following three devices to send information about step counts 
  * BBC microbit microcontroller 
  * Raspberry Pi 
  * Android smartphone
* Created this project in a group with 3 people including myself

# Project Overview

* The BBC microbit microcontroller uses the accelerometer to mimic a pedometer and sends the information to the Raspberry Pi using the Eddystone BLE Beacon when the user presses a button.
* The Android smart phone displays an app as an user interface to send and receive information to the Raspberry Pi by communicating through a MQTT Broker
* The Raspberry Pi acts as the MQTT Broker for communication between the Raspberry Pi and the Android Device
* The Raspberry Pi receives the steps from the BBC microbit microcontroller by reading the information from the Eddystone BLE Beacon
* The Raspberry Pi takes the information received from the Android Device to perform calculations based on the information received and sends the Android Device a True or False statement based on the calculation
  * Android Device sends Weather Data
  * If the user sends this information, the Raspberry Pi calculates the predicted steps based on the weather and weights calculated from the user's typical steps during that weather and the poll of 20 students.
    * The Raspberry Pi compares the predicted steps and compares this to actual steps sent from the BBC microbit microcontroller
      * If Goal is reached then the Raspberry Pi sends Boolean Value of True which will display " You did it" on the app
      * If Goal is not reached then the Raspberry Pi send Boolean Value of False which will display " Keep Going" on the app
  * Android Device sends Height, Weight, Age, and Calorie Loss Goal
    * If the user sends this information, the Raspberry Pi calculates the calories lost based on the steps the user has sent from the BBC microbit microcontroller
    * The Raspberry Pi compares the calculated calorie loss and compares this to the goal
      * If Goal is reached then the Raspberry Pi sends Boolean Value of True which will display on the app as a successful gif and a text field saying " You did it"
      * If Goal is not reached then the Raspberry Pi send Boolean Value of False which will display on the app as a failure gif and a text field saying " Keep Going"

