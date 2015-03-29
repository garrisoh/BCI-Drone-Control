package edu.lafayette.bci.devices;

import java.util.ArrayList;
import java.util.HashMap;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * A class representing the Emotiv headset.  This class is meant to
 * encapsulate the Emotiv SDK from the rest of the application, providing
 * only the necessary data to observers.  It handles all initialization
 * and data extraction from the emotiv engine.  Event handling is done
 * in an asynchronous, non-blocking manner, so updates are not guaranteed
 * to occur on the main thread.
 *
 * @author Haley Garrison
 */
public class Emotiv {

	/** The sampling period of the headset for all 14 electrodes. */
	public static final int SAMPLE_RATE_IN_MS = 10;
	
	/** Gyroscope sampling period for the Emotiv. */
	public static final int GYRO_SAMPLE_RATE_IN_MS = SAMPLE_RATE_IN_MS * 2;
	
	// Sensor and gyro data.  Access is by name.  For sensors, see diagram
	// for valid keys.  For gyro, keys are "x" or "y".  Values are either
	// a length 2 array with index 0 as quality and index 1 as value or
	// the value of the gyro sensor
	private volatile HashMap<String, Double[]> sensors = null;
	private volatile HashMap<String, Double> gyros = null;
	
	// Current battery percentage
	private volatile int battery = 0;

	// Observers that are notified when new data is available
	private volatile ArrayList<EmotivObserver> observers = null;
	
	// The current EmotivThread running updates
	private EmotivThread thread = null;
	
	// Flag for stopping the thread
	private volatile boolean stop = false;
	
	// Flag indicating that the thread stopped itself due to an error
	private volatile boolean error = false;

	/**
	 * Creates a new Emotiv instance and starts Emotiv updates  
	 * to observers.  A separate thread is used to poll for 
	 * updates in order to prevent blocking.
	 */
	public Emotiv() {
		observers = new ArrayList<EmotivObserver>();
		sensors = new HashMap<String, Double[]>();
		gyros = new HashMap<String, Double>();
		
		// start the thread
		thread = new EmotivThread(this);
		thread.start();
	}
	
	/**
	 * Cleans up the allocated resources and stops the observer
	 * updates.
	 */
	public void close() {
		// don't close if it has already been closed
		if (stop || error) {
			return;
		}
		
		// stop the thread (wait for it to finish)
		// thread will handle cleaning up resources
		stop = true;
		while (stop);
		stop = true;
	}
	
	/**
	 * Adds an EmotivObserver to the list and begins sending updates
	 * to the new observer.
	 * 
	 * @param o The new EmotivObserver
	 */
	public void addObserver(EmotivObserver o) {
		observers.add(o);
	}
	
	/**
	 * Removes an EmotivObserver from the list and stops sending updates
	 * to the observer.
	 * 
	 * @param o The EmotivObserver to remove
	 */
	public void removeObserver(EmotivObserver o) {
		observers.remove(o);
	}
	
	/**
	 * Returns the most recent reading from the specified sensor.
	 * 
	 * @param sensor The name of the desired sensor (ex. "AF3", "O2").
	 * @return The value of the most recent reading from sensor
	 */
	public double getSensorValue(String sensor) {
		return sensors.get(sensor)[1];
	}
	
	/**
	 * Returns the most recent quality value for the specified sensor.
	 * 
	 * @param sensor The name of the desired sensor (ex. "AF3", "O2").
	 * @return The quality of the sensor.  Max is 10 and min is 0
	 */
	public double getSensorQuality(String sensor) {
		return sensors.get(sensor)[0];
	}
	
	/**
	 * Returns the most recent reading from the specified gyro
	 * axis.  Values range from 0-255.
	 * 
	 * @param axis The name of the axis (either "x" or "y")
	 * @return The value of the gyroscope on the specified axis
	 */
	public double getGyroValue(String axis) {
		return gyros.get(axis);
	}
	
	/**
	 * Returns the percent of battery remaining.
	 * 
	 * @return Battery percentage (between 0-100)
	 */
	public int getBatteryPercent() {
		return battery;
	}

	/**
	 * A private Thread subclass that loops forever, notifying observers
	 * when new emotiv data becomes available (sensor, gyro, quality, or
	 * battery data).  This thread is used to make emotiv updates non-
	 * blocking.  The thread polls the Emotiv Engine every 32 ms and buffers
	 * 4 points each time.  This is because the engine buffers a minimum of
	 * 4 points, even though it is receiving data at a max rate of 128 Hz.  This
	 * higher sampling rate is achieved by sending out one of the buffered points
	 * every 8 ms and refreshing the buffers every 32 ms.  Gyro data appears at
	 * approximately half of the sensor data rate and battery data appears at
	 * approximately 1 Hz.
	 *
	 * @author Haley Garrison
	 */
	private class EmotivThread extends Thread {
		
		// An instance of the current Emotiv.  Used when notifying
		// observers
		private Emotiv e = null;
		
		// A list of the sensor names in the order of the channel number
		private final String[] sensorNames = { "AF3", "F7", "F3", "FC5", "T7", "P7",
											   "O1",  "O2", "P8", "T8",  "FC6","F4",
											   "F8", "AF4" };
		
		// Emotiv SDK variables - use Pointers and IntByReference because
		// the java library is a wrapper for the C library.
		
		// Size of data buffer - determines how often updates can occur
		private static final float BUFFER_SIZE_IN_SECS = (float)(4.0 / 128);
		
		// Rate at which to poll for new data
		private static final int POLLING_RATE = 8;
		
		// A handle to the last event that occurred
		private Pointer eEvent = null;
		
		// A buffer for holding emotiv state information
		private Pointer eState = null;
		
		// A buffer for holding raw sensor data
		private Pointer data = null;
		
		// The Id number of the current user profile
		private IntByReference userId = null;
		
		// The data buffer for receiving data and the current index of the point being
		// read from the buffers
		private double[][] dataBuffers = null;
		private int currIndex = 0;

		/**
		 * Constructor initializes the Emotiv instance and the emotiv
		 * engine
		 */
		public EmotivThread(Emotiv e) {
			this.e = e;
			dataBuffers = new double[14][4];
			
			// initialize handle/buffer
			eEvent = Edk.INSTANCE.EE_EmoEngineEventCreate();
			eState = Edk.INSTANCE.EE_EmoStateCreate();
			
			userId = new IntByReference();
			
			// initialize the emotiv engine
			if (Edk.INSTANCE.EE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {
				System.out.println("Emotiv Engine start up failed.");
				return;
			}
			
			// initialize the data buffer
			// this must happen after initialization or a seg-fault will occur
			data = Edk.INSTANCE.EE_DataCreate();
			Edk.INSTANCE.EE_DataSetBufferSizeInSec(BUFFER_SIZE_IN_SECS);
		}

		/**
		 * Loops, getting the state of the emotiv engine and
		 * notifying observers accordingly.
		 */
		@Override
		public void run() {
			// start acquiring raw data
			beginDataAcquisition();
			
			while (!stop) {
				// get new event if we have run out of data
				if (currIndex % dataBuffers[0].length == 0) {
					boolean status = handleEdkError(Edk.INSTANCE.EE_EngineGetNextEvent(eEvent));
					// stop if an error occurred
					if (status) {
						error = true;
						stop = true;
						break;
					}
					
					// update data and state
					Edk.INSTANCE.EE_DataUpdateHandle(userId.getValue(), data);
					Edk.INSTANCE.EE_EmoEngineEventGetEmoState(eEvent, eState);
					
					// check the number of samples taken
					IntByReference numSamples = new IntByReference(0);
					Edk.INSTANCE.EE_DataGetNumberOfSample(data, numSamples);
					
					if (numSamples == null || numSamples.getValue() == 0) {
						continue;
					}
				}
					
				// get sensor values and qualities
				int contactQuality = 0;
				for (int i = 0; i < 14; i++) {
					// get new data if we have run out
					if (currIndex % dataBuffers[0].length == 0) {
						// get data at ith channel (3 is offset in eeg channel enum)
						Edk.INSTANCE.EE_DataGet(data, i + 3, dataBuffers[i], 
												dataBuffers[i].length);
					}
					
					contactQuality = 
							EmoState.INSTANCE.ES_GetContactQuality(eState, i + 3);
					
					// set the appropriate sensor values
					Double[] sensorData = new Double[2];
					sensorData[0] = (double)contactQuality;
					sensorData[1] = dataBuffers[i][currIndex % dataBuffers[i].length];
					sensors.put(sensorNames[i], sensorData);
					
					// notify of quality change
					for (int j = 0; j < observers.size(); j++) {
						observers.get(j).qualityChanged(e, sensorNames[i], contactQuality);
					}
				}
				
				// get gyro data
				if (currIndex % 2 == 0) {
					IntByReference gyroX = new IntByReference();
					IntByReference gyroY = new IntByReference();
					Edk.INSTANCE.EE_HeadsetGetGyroDelta(userId.getValue(), gyroX, gyroY);
					gyros.put("x", (double)gyroX.getValue());
					gyros.put("y", (double)gyroY.getValue());
				}
				
				// get battery data
				if (currIndex % 128 == 0) {
					IntByReference batteryData = new IntByReference();
					IntByReference maxBattery = new IntByReference();
					EmoState.INSTANCE.ES_GetBatteryChargeLevel(eState, batteryData, maxBattery);
					battery = batteryData.getValue();
				}
				
				// notify observers
				for (int i = 0; i < observers.size(); i++) {
					observers.get(i).sensorsChanged(e);
					if (currIndex % 2 == 0)
						observers.get(i).gyrosChanged(e);
					if (currIndex % 128 == 0)
						observers.get(i).batteryChanged(e);
				}
				
				// increment the index
				currIndex++;
				
				// sleep at polling rate
				try { Thread.sleep(POLLING_RATE); } catch (Exception e) {};
			}
			
			// free emotiv engine resources
			Edk.INSTANCE.EE_EngineDisconnect();
			Edk.INSTANCE.EE_DataFree(data);
	    	Edk.INSTANCE.EE_EmoStateFree(eState);
	    	Edk.INSTANCE.EE_EmoEngineEventFree(eEvent);
	    	
	    	// alert the emotiv that the thread has finished
	    	stop = false;
		}
		
		/**
		 * Waits for a new user to connect and begins acquiring raw data
		 * from the new user.
		 */
		private void beginDataAcquisition() {
			while (!stop) {
				boolean status = handleEdkError(Edk.INSTANCE.EE_EngineGetNextEvent(eEvent));
				
				// stop if an error occurred
				if (status) {
					error = true;
					stop = true;
					break;
				}
				
				// get the event and user id
				int eventType = Edk.INSTANCE.EE_EmoEngineEventGetType(eEvent);
				Edk.INSTANCE.EE_EmoEngineEventGetUserId(eEvent, userId);
				
				// start getting raw data from the engine
				if (eventType == Edk.EE_Event_t.EE_UserAdded.ToInt() && userId != null) {
					Edk.INSTANCE.EE_DataAcquisitionEnable(userId.getValue(), true);
					break;
				}
				
				// sleep to prevent full CPU usage
				try { Thread.sleep(250); } catch (Exception e) {};
			}
		}
		
		/**
		 * Checks whether an EdkError has occurred and prints the error
		 * if it has occurred.
		 * 
		 * @param status The status code returned by EE_EngineGetNextEvent()
		 * @return True if an error has occurred, and false otherwise
		 */
		private boolean handleEdkError(int status) {
			if (!(status == EdkErrorCode.EDK_OK.ToInt() || status == EdkErrorCode.EDK_NO_EVENT.ToInt())) {
				// print error code in hexadecimal.  codes are found
				// in the EdkErrorCode.java file (also in hex).
				System.out.print("Emotiv engine error.  Code: ");
				System.out.println(Integer.toHexString(status));
				
				return true;
			}
			return false;
		}
	}
}
