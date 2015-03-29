package edu.lafayette.bci.sigproc;

import java.util.LinkedList;

/**
 * This algorithm is a low-pass filter using the rolling
 * window method.  It averages points within a certain window
 * to create a smoothing effect.
 *
 * @author Haley Garrison
 */
public class MovingAverage extends Algorithm {

	// A Point buffer representing the rolling window
	private LinkedList<Point> buffer = null;
	
	// The algorithm window size
	private int windowSize = 0;
	
	/**
	 * Creates a new low-pass filter with the specified
	 * window size.  The window size represents the number
	 * of samples to average (the size of the rolling window).
	 * 
	 * @param windowSize
	 */
	public MovingAverage(int windowSize) {
		buffer = new LinkedList<Point>();
		this.windowSize = windowSize;
	}
	
	/**
	 * Processes a single point and returns a single point.
	 * This method implements an algorithm that manipulates
	 * the data.  Points can initially be buffered if more
	 * than one data point is needed for the algorithm.  If
	 * buffering occurs, a valid point must still be returned,
	 * even if it is (0, 0).
	 * 
	 * This method implements a low-pass filter by averaging values
	 * within a certain window size.
	 * 
	 * @param p The point to be processed
	 * @return The processed point
	 */
	@Override
	public Point process(Point p) {
		// buffer the point
		buffer.offer(p);
		
		// wait until buffer is full
		Point processed = new Point(0, 0);
		if (buffer.size() < windowSize - 1) {
			if (this.tap != null) {
				this.tap.addPoint(processed);
			}
			return processed;
		}
		
		// perform filter
		double filteredY = 0;
		for (Point point : buffer) {
			filteredY += point.getY();
		}
		filteredY /= windowSize;
		
		// pop off the oldest
		buffer.poll();
		
		processed = new Point(buffer.get(windowSize / 2).getX(), filteredY);
		
		if (this.tap != null) {
			this.tap.addPoint(processed);
		}
		return processed;
	}

	/**
	 * Sets the window size of the filter
	 * 
	 * @param windowSize The filter window size
	 */
	public void setwindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	
}
