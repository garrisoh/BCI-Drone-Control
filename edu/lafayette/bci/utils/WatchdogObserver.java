package edu.lafayette.bci.utils;

/**
 * An interface for Watchdog observers to be notified on timeout events.
 *
 * @author Haley Garrison
 */
public interface WatchdogObserver {
	/**
	 * Called when a Watchdog times out.
	 */
	public void timeout();
}
