package edu.lafayette.bci.sigproc;

/**
 * A class representing a single, two-dimensional
 * point.
 *
 * @author Haley Garrison
 */
public class Point {

	// Coordinates
	private double x = 0.0;
	private double y = 0.0;
	
	/**
	 * Creates a new point with the give coordinates
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * @return Point's x coordinate
	 */
	public double getX() {
		return x;
	}
	
	/**
	 * @return Point's y coordinate
	 */
	public double getY() {
		return y;
	}
	
	/**
	 * @param x The new x coordinate
	 */
	public void setX(double x) {
		this.x = x;
	}
	
	/**
	 * @param y The new y coordinate
	 */
	public void setY(double y) {
		this.y = y;
	}
}
