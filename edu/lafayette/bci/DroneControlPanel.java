package edu.lafayette.bci;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import edu.lafayette.bci.sigproc.*;
import edu.lafayette.bci.devices.*;
import edu.lafayette.bci.utils.*;

/**
 * This is the main class for the DroneControlPanel.  It reads data
 * from the Emotiv headset on each sampling frame, pushes the data
 * through the signal processing pipelines, and updates the UI and
 * the drone commands.
 * 
 * If the drone is landed, blinking 5x within 2 seconds will command the
 * drone to takeoff.  If the drone is already flying, the same command will
 * land it.  This control scheme uses occipital alpha waves to toggle forward
 * movement.  If the drone has taken off and is hovering, upon closing and 
 * re-opening the eyes, the drone will move forward.  If the drone is already
 * moving forward, it will stop when the eyes are closed.  The yawrate of the
 * drone can be controlled using the horizontal (x-axis) gyroscope.  The further
 * the head is displaced from its initial (centered) position, the faster the
 * drone will rotate in that direction.
 *
 * @author Haley Garrison
 */
public class DroneControlPanel implements EmotivObserver, KeyListener, WatchdogObserver {
	
	// Threshold constants
	private static final double OCCIPITAL_THRES = 70.0; // Power threshold in (uV)^2
	private static final double DRONE_SPEED = 0.2; // Constant velocity of the drone in the given direction
	private static final double GYROX_POS_THRES = 2000.0; // Position threshold
	private static final double GYROX_POS_MAX = 10000.0; // Max value for normalizing gyro position
	private static final double BLINK_THRES = 65.0; // Blink detection power, in uV
	private static final int BLINK_NUM_THRES = 5; // Number of blinks to trigger a detection
	private static final double BLINK_TIME_THRES = 2.0; // Time within which blinks must occur (in secs)
	private static final long CONNECTION_TIMEOUT = 1000; // Timeout for detecting connection loss
	private static final int MOVING_AVG_WINDOW = 1500; // Number of milliseconds in filter window
	
	// Graph for occipital waves
	private Graph occipital = null;
	private Graph hpfO1 = null;
	private Graph hpfO2 = null;
	
	// Graph for gyros
	private Graph gyroX = null;
	
	// Graph for blinking
	private Graph frontal = null;
	private Graph hpfAF3 = null;
	private Graph hpfAF4 = null;
	
	// End devices
	private Emotiv emotiv = null;
	private Parrot drone = null;
	
	// UI class
	private DroneControlPanelUI ui = null;
	
	// Connection timer
	private Watchdog wd = null;
	
	// State variables
	private boolean isTurning = false; // Indicates if the drone is turning or stopped
	private boolean takeoff = false; // Indicates if the drone is flying or landed
	private boolean moving = false; // True if the drone is moving forward, false if stopped
	private volatile boolean estop = false; // Emergency stop
	private volatile boolean timedout = false;
	
	// Used to calculate elapsed time
	private long prevTime = 0;
	private long gyroPrevTime = 0;
	
	/**
	 * Main method, launches the drone control panel application
	 */
	public static void main(String[] args) {
		// Create an object of this class
		new DroneControlPanel();
	}
	
	/**
	 * Creates a new Drone Control Panel, initiates communication with
	 * the end devices.
	 */
	public DroneControlPanel() {
		// Create the ui
		ui = new DroneControlPanelUI();
		ui.setTakeoff(takeoff);
		ui.addKeyListener(this);
		
		// Setup the graphs and pipelines
		occipital = new Graph();
		hpfO1 = new Graph();
		hpfO2 = new Graph();
		gyroX = new Graph();
		frontal = new Graph();
		hpfAF3 = new Graph();
		hpfAF4 = new Graph();
		
		// Set graph window size (number of points kept)
		occipital.setWindowSize(5);
		hpfO1.setWindowSize(2);
		hpfO2.setWindowSize(2);
		gyroX.setWindowSize(2);
		frontal.setWindowSize(5);
		hpfAF3.setWindowSize(2);
		hpfAF4.setWindowSize(2);
		
		// Pipelines
		Pipeline pipeOcc = new Pipeline();
		Pipeline pipeHpf1 = new Pipeline();
		Pipeline pipeHpf2 = new Pipeline();
		Pipeline pipeGyroX = new Pipeline();
		Pipeline pipeFront = new Pipeline();
		Pipeline pipeHpf3 = new Pipeline();
		Pipeline pipeHpf4 = new Pipeline();
		
		// High pass filter to remove the drifting DC bias
		HighPassFilter hpf1 = new HighPassFilter(0.5);
		HighPassFilter hpf2 = new HighPassFilter(0.5);
		pipeHpf1.addAlgorithm(hpf1);
		pipeHpf2.addAlgorithm(hpf2);
		
		// Butterworth filter between 8-13Hz (alpha band)
		double[] freqs = { 8.0, 13.0 };
		Butterworth butter = new Butterworth(4, freqs, 1 / (Emotiv.SAMPLE_RATE_IN_MS / 1000.0), Butterworth.BPF);
		pipeOcc.addAlgorithm(butter);
		
		// Power calculation
		Power power = new Power(1 / 11.5, Emotiv.SAMPLE_RATE_IN_MS / 1000.0);
		pipeOcc.addAlgorithm(power);
		
		// Rolling average filter to smooth the power
		MovingAverage avg = new MovingAverage(MOVING_AVG_WINDOW / Emotiv.SAMPLE_RATE_IN_MS);
		pipeOcc.addAlgorithm(avg);
		
		// Convert to digital signal using level threshold
		Threshold thres = new Threshold(OCCIPITAL_THRES);
		pipeOcc.addAlgorithm(thres);
		
		// Perform rising and falling edge detection
		EdgeDetect edge = new EdgeDetect();
		pipeOcc.addAlgorithm(edge);
		
		// XGyro
		GyroDetect gd = new GyroDetect(GYROX_POS_THRES, false, true);
		pipeGyroX.addAlgorithm(gd);
		
		// Blinking HPF's
		HighPassFilter hpf3 = new HighPassFilter(0.5);
		HighPassFilter hpf4 = new HighPassFilter(0.5);
		pipeHpf3.addAlgorithm(hpf3);
		pipeHpf4.addAlgorithm(hpf4);
		
		// Blink detection threshold
		Threshold thres2 = new Threshold(BLINK_THRES);
		pipeFront.addAlgorithm(thres2);
		
		// Rising edge detection
		EdgeDetect edge2 = new EdgeDetect();
		pipeFront.addAlgorithm(edge2);
		
		// Pulse counter
		PulseCount cnt = new PulseCount(BLINK_NUM_THRES, BLINK_TIME_THRES);
		pipeFront.addAlgorithm(cnt);
		
		occipital.addPipeline(pipeOcc);
		hpfO1.addPipeline(pipeHpf1);
		hpfO2.addPipeline(pipeHpf2);
		gyroX.addPipeline(pipeGyroX);
		frontal.addPipeline(pipeFront);
		hpfAF3.addPipeline(pipeHpf3);
		hpfAF4.addPipeline(pipeHpf4);
		
		// Create the emotiv, watchdog, and the drone
		drone = new Parrot();
		wd = new Watchdog();
		wd.setTimeout(CONNECTION_TIMEOUT);
		wd.addObserver(this);
		emotiv = new Emotiv();
		emotiv.addObserver(this);
		wd.start();

		// Wait until window is closed
		while (!ui.isWindowClosed()) {
			try { Thread.sleep(250); } catch (Exception e) {}
		}
		
		// Close device communications
		wd.finish();
		emotiv.close();
		drone.hover();
		drone.land();
		drone.close();
	}
	
	/* (non-Javadoc)
	 * @see EmotivObserver#sensorsChanged(Emotiv)
	 */
	@Override
	public void sensorsChanged(Emotiv e) {
		// Initialize time
		if (prevTime == 0) {
			prevTime = System.currentTimeMillis();
		}
		
		// Get current time, calculate elapsed time
		long currTime = System.currentTimeMillis();
		
		// Reset watchdog timer
		wd.reset();
		
		// Pass data from each electrode through the HPF
		hpfO1.addPoint(new Point((currTime - prevTime) / 1000.0, e.getSensorValue("O1")));
		hpfO2.addPoint(new Point((currTime - prevTime) / 1000.0, e.getSensorValue("O2")));
		
		// Average signals and pass through occipital pipeline
		Point[] hpfO1data = new Point[hpfO1.getData().size()];
		Point[] hpfO2data = new Point[hpfO2.getData().size()];
		hpfO1.getData().toArray(hpfO1data);
		hpfO2.getData().toArray(hpfO2data);
		
		// Create a new point with the average of O1 and O2
		Point pt = new Point(hpfO1data[hpfO1data.length - 1].getX(), 
				(hpfO1data[hpfO1data.length - 1].getY() + hpfO2data[hpfO2data.length - 1].getY()) / 2.0);
		
		// Add to the occipital pipeline
		occipital.addPoint(pt);
		
		// Add AF3/4 data to high pass filters
		hpfAF3.addPoint(new Point((currTime - prevTime) / 1000.0, e.getSensorValue("AF3")));
		hpfAF4.addPoint(new Point((currTime - prevTime) / 1000.0, e.getSensorValue("AF4")));
		
		// Get high passed data
		Point[] hpfAF3data = new Point[hpfAF3.getData().size()];
		Point[] hpfAF4data = new Point[hpfAF3.getData().size()];
		hpfAF3.getData().toArray(hpfAF3data);
		hpfAF3.getData().toArray(hpfAF4data);
		
		// Average high passed data
		Point pt2 = new Point(hpfAF3data[hpfAF3data.length - 1].getX(), 
				(hpfAF3data[hpfAF3data.length - 1].getY() + hpfAF4data[hpfAF4data.length - 1].getY() / 2.0));
		
		// Add to frontal pipeline
		frontal.addPoint(pt2);
		
		// Estop logic
		if (estop) {
			drone.hover();
			drone.land();
			moving = false;
			takeoff = false;
			ui.setDirection(DroneControlPanelUI.HOVER);
			ui.setTakeoff(false);
			ui.setBottomLabel("Estop");
			return;
		}
		ui.setBottomLabel("Takeoff/Land: Blink x5, Toggle Forward/Stop: Close Eyes, Turn: Rotate Head (Reset Shift+C)");
		
		// Takeoff/landing logic
		Point[] frontalData = new Point[frontal.getData().size()];
		frontal.getData().toArray(frontalData);
		if (frontalData[frontalData.length - 1].getY() == 1.0) {
			// Toggle takeoff/landing
			takeoff = !takeoff;
			
			// Takeoff or land
			if (takeoff) {
				drone.takeoff();
			} else {
				drone.hover();
				drone.land();
				moving = false;
				ui.setDirection(DroneControlPanelUI.HOVER);
			}
			
			// Set UI and return
			ui.setTakeoff(takeoff);
			return;
		}
		
		// Look for alpha posedge/negedge
		Point[] occipitalData = new Point[occipital.getData().size()];
		occipital.getData().toArray(occipitalData);
		Point lastPoint = occipitalData[occipitalData.length - 1];
		
		if (lastPoint.getY() == 1 && moving) {
			// Rising edge detected while moving, stop drone
			drone.forward(0.0);
			ui.setDirection(ui.getDirection() & ~DroneControlPanelUI.FORWARD);
		} else if (lastPoint.getY() == -1 && moving) {
			// Wait for falling edge after stopping to prevent falling edge from
			// re-triggering movement
			moving = false;
		}
		else if (lastPoint.getY() == -1 && !moving) {
			// Falling edge detected while hovering, move drone forward
			drone.forward(DRONE_SPEED);
			moving = true;
			ui.setDirection(ui.getDirection() | DroneControlPanelUI.FORWARD);
		}
	}
	
	/* (non-Javadoc)
	 * @see EmotivObserver#gyrosChanged(Emotiv)
	 */
	@Override
	public void gyrosChanged(Emotiv e) {
		// Initialize time
		if (gyroPrevTime == 0) {
			gyroPrevTime = System.currentTimeMillis();
		}
		
		// Get current elapsed time
		long currTime = System.currentTimeMillis();
		
		// Add gyro data to graph
		gyroX.addPoint(new Point((currTime - gyroPrevTime) / 1000.0, e.getGyroValue("x")));
		
		// Get processed data
		Point[] gyroXData = new Point[gyroX.getData().size()];
		gyroX.getData().toArray(gyroXData);
		
		// Determine speed and direction of rotation
		double speed = gyroXData[gyroXData.length - 1].getY();
		if (speed < 0) {
			drone.counterClockwise(speed * -1 / GYROX_POS_MAX);
			ui.setDirection(ui.getDirection() | DroneControlPanelUI.CCW);
			isTurning = true;
		} else if (speed != 0) {
			drone.clockwise(speed / GYROX_POS_MAX);
			ui.setDirection(ui.getDirection() | DroneControlPanelUI.CW);
			isTurning = true;
		} else if (isTurning) {
			drone.clockwise(0.0);
			ui.setDirection(ui.getDirection() & ~DroneControlPanelUI.CCW & ~DroneControlPanelUI.CW);
			isTurning = false;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyReleased(KeyEvent e) {
		// Recalibrate if Shift+C is pressed
		if (e.getModifiers() == KeyEvent.SHIFT_DOWN_MASK && e.getKeyCode() == KeyEvent.VK_C) {
			GyroDetect det = (GyroDetect)gyroX.getPipeline().getAlgorithm(0);
			det.calibrateCenter();
			return;
		}
		
		// Implement emergency stop if any key is pressed
		estop = !estop;
		drone.hover();
		drone.land();
		moving = false;
		takeoff = false;
		ui.setDirection(DroneControlPanelUI.HOVER);
		ui.setTakeoff(false);
		
		// Set the label appropriately
		if (timedout) {
			ui.setBottomLabel("Connection Lost");
		} else if (estop) {
			ui.setBottomLabel("Estop");
		} else {
			ui.setBottomLabel("Takeoff/Land: Blink x5, Toggle Forward/Stop: Close Eyes, Turn: Rotate Head\nRecalibrate Gyros: Shift+C");
		}
	}
	
	/* (non-Javadoc)
	 * @see WatchdogObserver#timeout()
	 */
	public void timeout() {
		// Stop the drone
		timedout = true;
		drone.hover();
		
		// Set the UI to hover if we are flying
		if (takeoff)
			ui.setDirection(DroneControlPanelUI.HOVER);
		
		// Indicate connection lost on UI
		ui.setBottomLabel("Connection Lost");
	}
	
	/* ********** Unimplemented Methods ********** */
	// TODO: Add battery, quality indicators
	
	/* (non-Javadoc)
	 * @see EmotivObserver#batteryChanged(Emotiv)
	 */
	@Override
	public void batteryChanged(Emotiv e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see EmotivObserver#qualityChanged(Emotiv, java.lang.String, int)
	 */
	@Override
	public void qualityChanged(Emotiv e, String sensor, int quality) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
