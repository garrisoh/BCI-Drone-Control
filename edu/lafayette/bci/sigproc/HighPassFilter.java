package edu.lafayette.bci.sigproc;

/**
 * A simple high-pass filter algorithm.  Frequencies lower
 * than the cutoff frequency are filtered out.<br><br>
 * 
 * Algorithm pseudo-code source:<br>
 * <a href="http://en.wikipedia.org/wiki/High-pass_filter">
 * http://en.wikipedia.org/wiki/High-pass_filter</a>
 *
 * @author Haley Garrison
 */
public class HighPassFilter extends Algorithm {
	
	// Stores the previous points for use in this algorithm
	private Point previousFiltered = null;
	private Point previousUnfiltered = null;

	// The time constant of the filter in secs
	private double rc = 0.0;

	/**
	 * Constructor initializes the filter 3dB/cutoff frequency, 
	 * defined as 1/(2_PI * RC)
	 * 
	 * @param cutoff The cutoff frequency (in Hz)
	 */
	public HighPassFilter(double cutoff) {
		setCutoff(cutoff);
	}

	/**
	 * Processes a single point and returns a single point.
	 * This method implements a high-pass filter at the given
	 * cutoff frequency.
	 * 
	 * @param p The point to be processed
	 * @return The processed point
	 */
	@Override
	public Point process(Point p) {
		if (previousFiltered == null) {
			previousFiltered = p;
			previousUnfiltered = p;
			
			if (this.tap != null) {
				this.tap.addPoint(p);
			}
			return p;
		}

		// calculate new y value
		double dt = p.getX() - previousUnfiltered.getX();
		double alpha = rc / (rc + dt);
		double y = alpha * previousFiltered.getY() + 
				   alpha * (p.getY() - previousUnfiltered.getY());

		// get the new point
		Point filtered = new Point(p.getX(), y);

		// reset previous
		previousFiltered = filtered;
		previousUnfiltered = p;

		if (this.tap != null) {
			this.tap.addPoint(filtered);
		}
		return filtered;
	}

	/**
	 * Sets the RC time constant of the filter.  The cutoff
	 * frequency will be at 1/(2_PI * RC).
	 * 
	 * @param rc The rc time constant in seconds
	 */
	public void setRC(double rc) {
		this.rc = rc;
	}

	/**
	 * Sets the filter 3dB/cutoff frequency, defined as 1/(2_PI * RC).
	 * 
	 * @param cutoff The cutoff frequency (in Hz)
	 */
	public void setCutoff(double cutoff) {
		this.rc = 1.0 / (2 * Math.PI * cutoff);
	}
}
