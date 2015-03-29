package edu.lafayette.bci.utils;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class creates a watchdog timer that will notify all
 * observers when the timeout interval has been exceeded.
 *
 * @author Haley Garrison
 */
public class Watchdog extends Thread {
	// List of observers
	private ConcurrentLinkedQueue<WatchdogObserver> observers = null;
	
	// Timer variables
	private volatile long startTime = 0;
	private volatile long timeoutTime = 0;
	
	// Sychronized state variables
	private volatile boolean stop = false;
	private volatile boolean isFinished = false;
	
	/**
	 * Creates a new watchdog timer, initially with no observers.
	 */
	public Watchdog() {
		observers = new ConcurrentLinkedQueue<WatchdogObserver>();
	}
	
	/**
	 * Adds a WatchdogObserver to be notified on timeout.
	 * 
	 * @param o The observer to add to this timer.
	 */
	public void addObserver(WatchdogObserver o) {
		observers.add(o);
	}
	
	/**
	 * Removes a WatchdogObserver from the list of observers.
	 * 
	 * @param o The observer to remove.
	 */
	public void removeObserver(WatchdogObserver o) {
		observers.remove(o);
	}
	
	/**
	 * Sets the timeout duration after which observers will be notified.
	 * 
	 * @param millis The number of milliseconds before timeout.
	 */
	public void setTimeout(long millis) {
		timeoutTime = millis;
	}
	
	/**
	 * Starts the timer.
	 */
	@Override
	public void run() {
		isFinished = false;
		stop = false;
		startTime = System.currentTimeMillis();
		
		while (!stop) {
			if (System.currentTimeMillis() - startTime >= timeoutTime)
				notifyObservers();
			
			try { Thread.sleep(timeoutTime / 4); } catch (Exception e) {};
		}
		
		// Indicates that the thread has finished running
		isFinished = true;
	}
	
	/**
	 * Resets the timer.  If the timer has already finished, it will restart the timer,
	 * otherwise it will reset the start time to the current time.
	 */
	public void reset() {
		if (isFinished)
			run();
		else
			startTime = System.currentTimeMillis();
	}
	
	/**
	 * Stops the timer.  After the timer has been stopped, it must be restarted with the
	 * start() method.
	 */
	public void finish() {
		stop = true;
	}
	
	/**
	 * Notifies all observers that a timeout has occurred.
	 */
	private void notifyObservers() {
		for (WatchdogObserver o : observers)
			o.timeout();
	}
}
