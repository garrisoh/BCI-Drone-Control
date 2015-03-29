package edu.lafayette.bci.sigproc;

import java.util.ArrayList;

/**
 * This class represents a string of algorithms for processing
 * data points.  New data is pushed into the pipeline and the
 * next processed point pops out.  Algorithms can be added,
 * removed, or swapped.
 *
 * @author Haley Garrison
 */
public class Pipeline {

	// List of algorithms
	private ArrayList<Algorithm> algorithms = null;
	
	/**
	 * Creates a new empty pipeline to which algorithms can be
	 * added.
	 */
	public Pipeline() {
		algorithms = new ArrayList<Algorithm>();
	}
	
	/**
	 * The number of algorithms in the pipeline.
	 * 
	 * @return The size of the pipeline
	 */
	public int size() {
		return algorithms.size();
	}
	
	/**
	 * Pushes a new point into the pipeline and pops the next
	 * out.
	 * 
	 * @param p The point to push
	 * @return The next processed point
	 */
	public Point pushPoint(Point p) {
		for (int i = 0; i < algorithms.size(); i++) {
			p = algorithms.get(i).process(p);
		}
		return p;
	}
	
	/**
	 * Accessor for an algorithm within the pipeline.
	 * 
	 * @param index Index of the desired algorithm
	 * @return The corresponding algorithm
	 */
	public Algorithm getAlgorithm(int index) {
		return algorithms.get(index);
	}
	
	/**
	 * Adds an algorithm to the end of the pipeline
	 * 
	 * @param a The algorithm to add
	 */
	public void addAlgorithm(Algorithm a) {
		algorithms.add(a);
	}
	
	/**
	 * Removes an algorithm from the pipeline.
	 * 
	 * @param index The index of the algorithm to remove
	 */
	public void removeAlgorithm(int index) {
		algorithms.remove(index);
	}
	
	/**
	 * Swaps two existing algorithms.
	 * 
	 * @param index1 The index of the first algorithm
	 * @param index2 The index of the second algorithm
	 */
	public void swapAlgorithms(int index1, int index2) {
		Algorithm tmp = algorithms.get(index1);
		algorithms.set(index1, algorithms.get(index2));
		algorithms.set(index2, tmp);
	}
}
