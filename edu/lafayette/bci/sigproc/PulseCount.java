package edu.lafayette.bci.sigproc;

/**
 * This algorithm counts the number of pulses (ones) that occur
 * within the given time threshold and outputs a one if the 
 * number threshold has been exceeded.
 *
 * @author Haley Garrison
 */
public class PulseCount extends Algorithm {

	// Threshold values
	private int numThres = 1;
	private double timeThres = 0;
	
	// Time and number counters
	private double startTime = 0.0;
	private int count = 0;
	
	/**
	 * Creates a new pulse count with the given number and
	 * time thresholds.  If the timeThres is set to 0, then
	 * time will not be considered and the algorithm will output
	 * a one whenever the numThres has been reached.
	 * 
	 * @param numThres Number of pulses to count to
	 * @param timeThres Time (in secs) within which numThres must be exceeded
	 */
	public PulseCount(int numThres, double timeThres) {
		this.numThres = numThres;
		this.timeThres = timeThres;
	}
	
	/* (non-Javadoc)
	 * @see Algorithm#process(Point)
	 */
	@Override
	public Point process(Point p) {
		// No detection, return zero
		if (p.getY() <= 0.0) {
			Point pt = new Point(p.getX(), 0.0);
			if (this.tap != null)
				this.tap.addPoint(pt);
			return pt;
		}

		// Check if the time threshold is zero, if so unconditionally increment
		// count
		if (timeThres == 0) {
			// Increment count
			count++;

			// Check if number has been exceeded
			if (count >= numThres) {
				count = 0;
				Point pt = new Point(p.getX(), 1.0);
				if (this.tap != null)
					this.tap.addPoint(pt);
				return pt;
			}
			
			Point pt = new Point(p.getX(), 0.0);
			if (this.tap != null)
				this.tap.addPoint(pt);
			return pt;
		}
		
		// Set the startTime, reset count
		if (startTime == 0.0 || p.getX() - startTime > timeThres) {
			startTime = p.getX();
			count = 0;
		}
		
		count++;
		
		// Check if number has been exceeded
		if (count >= numThres) {
			count = 0;
			startTime = 0.0;
			Point pt = new Point(p.getX(), 1.0);
			if (this.tap != null)
				this.tap.addPoint(pt);
			return pt;
		}
		
		Point pt = new Point(p.getX(), 0.0);
		if (this.tap != null)
			this.tap.addPoint(pt);
		return pt;
	}

}
