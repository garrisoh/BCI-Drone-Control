package edu.lafayette.bci.sigproc;

/**
 * The EdgeDetect algorithm outputs a one when a positive edge
 * is detected and a negative one when a negative edge is
 * detected.
 *
 * @author Haley Garrison
 */
public class EdgeDetect extends Algorithm {
	
	double prevVal = -1.0;
	
	/* (non-Javadoc)
	 * @see Algorithm#process(Point)
	 */
	@Override
	public Point process(Point p) {
		// Detect a rising or falling edge
		double y = (prevVal == 0.0 && p.getY() == 1.0) ? 1.0 : (prevVal == 1.0 && p.getY() == 0.0) ? -1.0 : 0.0;
		
		// Set the previous value to the current value of y
		prevVal = p.getY();
		
		// Return the point
		Point processed = new Point(p.getX(), y);
		if (this.tap != null) {
			this.tap.addPoint(processed);
		}
		return processed;
	}

}
