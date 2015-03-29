# BCI-Drone-Control
This repo contains signal processing libraries written in Java for the development of BCI systems.  It also supports communication with two end devices: the Emotiv EEG and the Parrot AR.Drone.  The DroneControlPanel.jar is an executable jar file that runs a program to control the Parrot AR.Drone using brainwave data from the Emotiv EEG.

## Installation:
1. Download and extract the zip file for this repository.
2. Copy the BCILib.jar file into your Java extensions folder (ex. /Library/Java/Extensions/ for Mac OS X).
3. Copy the Emotiv libraries into the same folder.  This step is not required if you are not using the Emotiv headset.  To use the Emotiv, you must have the research edition of the Emotiv SDK.  The required libraries are located under EmotivResearch/docs/Examples/Java/lib/.

## To Run the Drone Control Panel with the Simulator:
1. Complete the steps above to install the required libraries.
2. Launch DroneSim.  DroneSim must be running before the control panel program begins or the control panel will be unable to connect to the simulator.
3. Prepare and power on the Emotiv headset and dongle.  Wet each electrode with saline solution, place the headset on your head according to the instructions given by Emotiv, and ensure that the sensors have good quality using the Emotiv TestBench program from the SDK.  The O1, O2, AF3, and AF4 electrodes are the most critical.
4. Double-click the DroneControlPanel.jar file in the downloaded repo to launch the program.  Alternatively, you can import the project into Eclipse and run it from there.

##To Run the Drone Control Panel with the Parrot AR.Drone:
1. Complete the steps above to install the required libraries.
2. In a terminal, navigate to the resources folder: `cd path/to/downloaded/repo/edu/lafayette/bci/resources/`
3. Install the [nodecopter](http://www.nodecopter.com) library (requires node.js, see [nodejs.org](https://nodejs.org) for installation instructions): `npm install ar-drone`
4. Power on the drone.
5. Run the drone server (must be run before the control panel program is launched): `node ParrotServer.js`
6. Prepare and power on the Emotiv headset and dongle.  Wet each electrode with saline solution, place the headset on your head according to the instructions given by Emotiv, and ensure that the sensors have good quality using the Emotiv TestBench program from the SDK.  The O1, O2, AF3, and AF4 electrodes are the most critical.
7. Double-click the DroneControlPanel.jar file in the downloaded repo to launch the program.  Alternatively, you can import the project into Eclipse and run it from there.

## Controls for the Drone Control Panel:
Toggle Takeoff/Land - Blink 5x in 2 seconds.  Slower, more rhythmic blinks work better than fast blinks.
Toggle Forward/Hover - Close your eyes briefly (~1-2sec) and reopen them.  The drone will not move forward until you have re-opened your eyes and will stop as soon as it detects your eyes have closed.
Rotate CW/CCW - Rotate your head left or right.  The center position will be initialized to your head position when the program is launched, but it can be recalibrated by pressing Shift+C within the UI window.  The further you rotate your head, the faster the drone will rotate.
Estop - Press any key to enable.  The drone will land and stay landed until the Estop is released by pressing a key again.

Note: If the connection is lost with the Emotiv, the drone will hover in place until the connection is re-established.

## How it Works:
Takeoff/Land - This signal looks at high-amplitude muscular impulses on electrodes AF3 and AF4.  The raw data from the electrodes are high-passed at 0.5Hz to remove the DC bias, averaged to improve SNR, compared to a threshold, converted to a digital "1" at each rising edge of the threshold, and a pulse counter looks for 5 pulses in 2 seconds.
Forward/Hover - This signal looks at the alpha wave frequency (8-13Hz) in your EEG/brainwave data on sensors O1 and O2.  Increases in alpha waves are observed in the visual cortex of your brain when your eyes close.  To detect this signal, the raw O1 and O2 data are high-passed at 0.5Hz to remove the DC offset, averaged to improve SNR, filtered with an 8-13Hz Butterworth band-pass filter, computed as an average power over time, smoothed with a 1.5 second moving average filter, compared to a threshold, and finally converted to a "1" or "-1" value at the rising and falling edges of the pulse.
CW/CCW - The Emotiv headset contains a gyroscope that measures your head's rotational velocity.  Integrating this velocity over time gives a rough angular position for your head.  Recalibration clears the integrator, thereby resetting the inital position of the gyro position.

Note: The thresholds used for the DroneControlPanel may change from user to user.  These may need to be adjusted within the DroneControlPanel.java file.  The constants that may change are the occipital threshold and the blink threshold.  Increasing these thresholds will prevent undesired behavior, but will make the system less sensitive to your commands (or vice-versa for decreasing them).  Decreasing the moving average window size will reduce the time between eye opening/closure and the drone's response, but will make unwanted behavior more likely (opposite for increasing it).

## Library Structure:
The BCILib is divided into three packages: sigproc, devices, and utils.  Javadoc can be found for each of these packages and all of their classes in the doc folder or by clicking [here](./doc/index.html).

The sigproc package contains a Graph class that is responsible for storing data.  Each graph can contain a Pipeline of Algorithms.  Any points added to the graph will be pushed through each algorithm in its pipeline in the order that the algorithms were added to the pipeline.  The structure is shown below:

![Block Diagram](https://github.com/garrisoh/BCI-Drone-Control/BlockDiagram.png)

To create your own Algorithms, simply subclass the Algorithm abstract class and implement the process(Point) method as described in the Javadoc.

The devices package contains classes for interfacing with the Emotiv headset and the Parrot AR.Drone.  To receive raw data from the Emotiv, implement the EmotivObserver interface and add your class as an observer to an Emotiv object.  To control the Parrot AR.Drone from Java, run the Parrot.js server (instructions above) and use the methods in the Parrot class to send it commands.

The utils package contains a Watchdog timer.  This timer can be used to monitor the Emotiv's wireless connection.  Your class can create an instance of a Watchdog timer and implement the WatchdogObserver interface to handle connection timeouts for the Emotiv.

Additionally, the base package (edu.lafayette.bci) contains the java code for the DroneControlPanel and DroneControlPanelUI.  These classes can be used as examples or run by calling the main method in the DroneControlPanel class.
