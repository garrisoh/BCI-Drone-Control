package edu.lafayette.bci.sigproc;

/**
 * This algorithm detects gyro movement.  When set to velocity
 * detect mode, the output will be a value between 1 and -1, 
 * where 1 indicates movement to the right, -1 to the left,
 * and zero means no movement.  All movement is compared to
 * a velocity threshold.  The position detect mode estimates
 * the head position relative to the initial position.  If return
 * to zero is enabled in velocity mode, the algorithm will output
 * a one or negative one only after the gyro has returned to the
 * initial position (ex. move head left then right).  For position
 * mode, return to zero will snap the position to zero after the
 * position falls below the given threshold.  Points within
 * -thres/2 and +thres/2 of the initial position will be considered 
 * at zero position.
 *
 * @author Haley Garrison
 */
public class GyroDetect extends Algorithm {

	// Mode and threshold parameters
	private boolean isVelocityMode = true;
	private boolean rtz = true;
	private double thres = 10.0;
	
	// RTZ detection for velocity mode
	private double prevDet = 0.0;
	private boolean triggered = false;
	
	// Integrator for position mode - Integral(v(t), t, 0, inf) = x(t)
	private double integrator = 0.0;
	
	/**
	 * Creates a new gyro detection algorithm with the given
	 * threshold and mode.  If return to zero is enabled, the
	 * algorithm will return a detection only after a combination
	 * left/right or right/left (velocity mode) or will return to
	 * zero position when it falls below the threshold (position
	 * mode).
	 * 
	 * @param thres The detection threshold.  Position thres ignored if rtz is not enabled.
	 * @param velocityMode True for velocity mode, false for position.
	 * @param rtz True if return to zero detection is enabled, false otherwise.
	 */
	public GyroDetect(double thres, boolean velocityMode, boolean rtz) {
		this.isVelocityMode = velocityMode;
		this.rtz = rtz;
		this.thres = thres;
	}
	
	/* (non-Javadoc)
	 * @see Algorithm#process(Point)
	 */
	@Override
	public Point process(Point p) {
		double y = 0.0;
		
		// determine y value based on the mode
		if (isVelocityMode && !rtz) {
			// Check if y is above or below the threshold
			y = (p.getY() >= thres) ? 1.0 : (p.getY() <= -thres) ? -1.0 : 0.0;
		} else if(isVelocityMode && rtz) {
			// Check if y is above or below the threshold
			double ynrz = (p.getY() >= thres) ? 1.0 : (p.getY() <= -thres) ? -1.0 : 0.0;
			
			// Trigger a detection only if the previous value and current value are opposite in sign
			y = (prevDet == -1.0 && ynrz == 1.0) ? -1.0 : (prevDet == 1.0 && ynrz == -1.0) ? 1.0 : 0.0;
			
			// Set triggered when y is not zero, reset when movement stops
			if (y != 0.0) 
				triggered = true;
			else if (ynrz == 0.0)
				triggered = false;
			
			// Set the previous detection to zero if we have returned to zero or the nrz value if
			// a threshold has been crossed, otherwise keep the previous detection
			prevDet = (triggered) ? 0.0 : (ynrz != 0.0) ? ynrz : prevDet;
		} else {
			// Integrate
			integrator += p.getY();
			
			// Return to zero if rtz is enabled and we are within the threshold of zero
			y = (integrator >= -thres/2 && integrator <= thres/2 && rtz) ? 0.0 : integrator;
		}
		
		Point pt = new Point(p.getX(), y);
		if (this.tap != null) {
			this.tap.addPoint(pt);
		}
		return pt;
	}

	/**
	 * In position mode, this method sets the center position to
	 * the current position.
	 */
	public void calibrateCenter() {
		integrator = 0.0;
	}
	
}
