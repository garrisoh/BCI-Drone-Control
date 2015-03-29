package edu.lafayette.bci.sigproc;

/**
 * An abstract class for an algorithm capable of manipulating
 * two-dimensional points.  A single point is taken in and a single
 * point is returned with each iteration through the algorithm.
 *
 * @author Haley Garrison
 */
public abstract class Algorithm {

	// Tap graph
	protected Graph tap = null;
	
	/**
	 * Processes a single point and returns a single point.
	 * This method implements an algorithm that manipulates
	 * the data.  Points can initially be buffered if more
	 * than one data point is needed for the algorithm.  If
	 * buffering occurs, a valid point must still be returned,
	 * even if it is (0, 0).  All points must be added to the
	 * tap if the tap is not null.
	 * 
	 * @param p The point to be processed
	 * @return The processed point
	 */
	public abstract Point process(Point p);
	
	/**
	 * Adds a tap to this graph.  All processed points should be
	 * added to the tap if there is one.
	 * 
	 * @param g The tap graph to add
	 */
	public void addTap(Graph g) {
		this.tap = g;
	}
}
