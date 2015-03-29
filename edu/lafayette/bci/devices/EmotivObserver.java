package edu.lafayette.bci.devices;

/**
 * An interface for observers of the Emotiv
 * sensor, quality, and battery data.  All methods
 * except qualityChanged require the implementing class
 * to pull the data from the Emotiv headset.  All methods 
 * are asynchronous and may be called from a thread other
 * than the main thread.
 *
 * @author Haley Garrison
 */
public interface EmotivObserver {

	/**
	 * Called when the battery value is updated.
	 * 
	 * @param e The calling Emotiv
	 */
	public void batteryChanged(Emotiv e);
	
	/**
	 * Called when the quality value of a sensor has changed.
	 * 
	 * @param e The calling Emotiv
	 * @param sensor The affected sensor
	 * @param quality The new quality value of the sensor
	 */
	public void qualityChanged(Emotiv e, String sensor, int quality);
	
	/**
	 * Called when the value of the sensors has changed.
	 * 
	 * @param e The calling Emotiv
	 */
	public void sensorsChanged(Emotiv e);
	
	/**
	 * Called when the gyroscope data has changed.
	 * 
	 * @param e The calling Emotiv
	 */
	public void gyrosChanged(Emotiv e);
	
}
