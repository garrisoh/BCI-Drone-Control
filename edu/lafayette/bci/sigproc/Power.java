package edu.lafayette.bci.sigproc;

import java.util.LinkedList;

/**
 * Calculates an approximate value for the average power measured
 * over time of the incoming signal.  The signal must be periodic
 * with a period equal to that specified in the constructor.
 *
 * @author Haley Garrison
 */
public class Power extends Algorithm {
	
	// A buffer to store one period of data
	private LinkedList<Point> buffer = null;
	private int bufferSize = 0;
	
	// The period over which the power is calculated (in secs)
	private double period = 0;
	
	/**
	 * Creates a new power algorithm with the given period
	 * over which to average.  The sample rate determines
	 * how many samples exist per period.
	 * 
	 * @param period The period of the input signal in secs
	 * @param sampleRate The sample rate of the signal in secs
	 */
	public Power(double period, double sampleRate) {
		// calculate number of samples in a period
		bufferSize = (int)Math.ceil(period / sampleRate);
		
		buffer = new LinkedList<Point>();
		
		this.period = period;
	}
	
	/**
	 * Processes a single point and returns a single point.
	 * This method calculates the discrete power of the signal.
	 * 
	 * @param p The point to be processed
	 * @return The processed point
	 */
	@Override
	public Point process(Point p) {
		// wait for buffer to fill
		Point processed = new Point(0.0, 0.0);
		if (buffer.size() < bufferSize - 1) {
			buffer.offer(p);
			if (this.tap != null) {
				this.tap.addPoint(processed);
			}
			return processed;
		}
		
		// add the point
		buffer.offer(p);
		
		// add all magnitudes squared over the period
		double power = 0.0;
		double dt = period / bufferSize;
		for (Point pt : buffer) {
			power += pt.getY() * pt.getY() * dt;
		}
		
		// divide by the period
		power /= period;
		
		// remove the oldest point
		Point old = buffer.poll();
		processed = new Point(old.getX(), power);
		if (this.tap != null) {
			this.tap.addPoint(processed);
		}
		return processed;
	}
}
