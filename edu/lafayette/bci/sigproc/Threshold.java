package edu.lafayette.bci.sigproc;

/**
 * This algorithm detects when the input signal exceeds a threshold
 * point and outputs a one when the threshold is exceeded. 
 *
 * @author Haley Garrison
 */
public class Threshold extends Algorithm {
	
	// User specified threshold
	private double thres = 0.0;
	
	/**
	 * Creates a new threshold algorithm.
	 * 
	 * @param thres The detection threshold
	 */
	public Threshold(double thres) {
		this.thres = thres;
	}

	/* (non-Javadoc)
	 * @see Algorithm#process(Point)
	 */
	@Override
	public Point process(Point p) {
		Point processed = (p.getY() > thres) ? new Point(p.getX(), 1.0) : new Point(p.getX(), 0.0);
		if (this.tap != null) {
			this.tap.addPoint(processed);
		}
		return processed;
	}

}
