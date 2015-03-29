package edu.lafayette.bci.sigproc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * Represents a window of data (either raw or processed).
 * Graph is also capable of displaying itself in a GUI 
 * application based on the given scale.  The setWindowSize(int)
 * method must be called when the graph is created or no points
 * will be added to the Graph.  This class is thread safe.
 *
 * @author Haley Garrison
 */
public class Graph extends JComponent {
	
	// Silences warning from Eclipse
	private static final long serialVersionUID = 126;
	
	// Queue of data points with a given window
	private ConcurrentLinkedQueue<Point> data = null;
	
	// Max scale on x-axis (x) and max peak-to-peak distance (y)
	private double xScale = 0.0;
	private double yScale = 0.0;
	
	// Determines the size of the window of points kept
	private int windowSize = 0;
	
	// Axis labels for GUI applications
	private String xLabel = "X";
	private String yLabel = "Y";
	
	// Data pipeline
	private Pipeline pipe = null;
	
	// Indicates whether or not the graph is paused
	private boolean paused = false;
	
	// Keeps track of current "zero" value on the x-axis
	// This allows the graph to shift left when the window
	// size has been reached.
	private double xOffset = 0.0;
	
	// Used for un-pausing to indicate that the graph should reset
	// the offset
	private boolean needsOffset = false;
	
	// Indicates whether to repaint when a new point is added
	private boolean isGUI = false;
	
	// Indicates whether repainting occurs automatically
	private boolean autoRepaint = true;
	
	/**
	 * Creates a new graph with a data queue.
	 */
	public Graph() {
		data = new ConcurrentLinkedQueue<Point>();
	}
	
	/**
	 * Returns the data contained in this graph.
	 * 
	 * @return The queue containing this graph's processed data.
	 */
	public ConcurrentLinkedQueue<Point> getData() {
		return data;
	}
	
	/**
	 * Returns the pipeline contained in this graph.
	 * 
	 * @return This graph's pipeline.
	 */
	public Pipeline getPipeline() {
		return pipe;
	}
	
	/**
	 * This method must be called to set the scale of the
	 * graph for GUI applications or no graph will be shown.
	 * 
	 * @param x Max x-axis span of data points
	 * @param y Max peak-to-peak distance of data points
	 */
	public void setScale(double x, double y) {
		xScale = x;
		yScale = y;
	}
	
	/**
	 * Determines the maximum number of points kept at one time.
	 * This method must be called or no points will be kept.
	 * 
	 * @param size The number of samples kept for this graph.
	 */
	public void setWindowSize(int size) {
		windowSize = size;
	}
	
	/**
	 * Sets the labels for the x and y axes that will be displayed
	 * when this graph is added to a GUI application
	 * 
	 * @param xLabel The title of the x axis
	 * @param yLabel The title of the y axis
	 */
	public void setAxisLabels(String xLabel, String yLabel) {
		this.xLabel = xLabel;
		this.yLabel = yLabel;
	}
	
	/**
	 * Indicates whether the graph should automatically repaint
	 * when a new point is added.  This value is true by default.
	 * If auto repaint is disabled, the repaint() method must be
	 * called manually.
	 * 
	 * @param autoRepaint False to disable auto-repainting
	 */
	public void setAutoRepaint(boolean autoRepaint) {
		this.autoRepaint = autoRepaint;
	}
	
	/**
	 * Pause or unpause the graph.  Pausing prevents new data
	 * from being added and old data from being removed.
	 * 
	 * @param paused True to pause the graph
	 */
	public void setPaused(boolean paused) {
		this.paused = paused;
		
		// clear the data when unpaused
		if (!paused) {
			data.clear();
			needsOffset = true;
		}
	}
	
	/**
	 * Adds a signal processing pipeline to this graph.  Null 
	 * removes the pipeline and data will be left unprocessed
	 * 
	 * @param p The pipeline to add.
	 */
	public void addPipeline(Pipeline p) {
		pipe = p;
	}
	
	/**
	 * Adds a point to this graph, passing it through the
	 * pipeline if there is one.
	 * 
	 * @param p The new point to process and add to the graph.
	 */
	public void addPoint(Point p) {
		// don't add if the graph is paused or if p is null
		if (paused || p == null) {
			return;
		}
		
		// process point if a pipeline is being used
		Point toAdd = null;
		if (pipe != null) {
			toAdd = pipe.pushPoint(p);
		} else {
			toAdd = p;
		}
		
		// pop the oldest point if the windowSize has been reached
		// set the offset if unpausing or popping a point
		if (data.size() >= windowSize) {
			Point old = data.poll();
			xOffset = old.getX();
		} else if (needsOffset) {
			xOffset = p.getX();
			needsOffset = false;
		}
		
		// add to the end of the queue
		data.offer(toAdd);
		
		// repaint if this is being displayed in a GUI app
		if (isGUI && autoRepaint) {
			repaint();
		}
	}
	
	/**
	 * Returns this graph's data formatted as CSV data.  X values
	 * of the points are in the first column and Y values in the second
	 * 
	 * @return This graph's comma separated data
	 */
	public String toCSV() {
		// add all points to a CSV string
		String s = xLabel + "," + yLabel + System.getProperty("line.separator");
		for (Point p : data) {
			s += p.getX();
			s += ",";
			s += p.getY();
			s += System.getProperty("line.separator");
		}
		return s;
	}
	
	/**
	 * Sets this graph's data from a CSV string.  The csv string
	 * must have two columns, each labeled with the axis title.
	 * The first column will be treated as the x-axis values and
	 * the second column will be treated as the y-axis values.
	 * 
	 * @param csv The CSV string to parse
	 */
	public void fromCSV(String csv) {
		// get each row
		String[] lines = csv.split("\n");
		
		// get point data from the rows
		String[][] points = new String[lines.length][2];
		for (int i = 0; i < lines.length; i++) {
			points[i] = lines[i].split(",");
		}
		
		// set the axis labels
		xLabel = points[0][0];
		yLabel = points[0][1];
		
		// add all the points
		for (int i = 1; i < lines.length; i++) {
			double x = new Double(points[i][0]);
			double y = new Double(points[i][1]);
			addPoint(new Point(x, y));
		}
	}
	
	/**
	 * Draws a graphical representation of the graph.
	 */
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		
		// set isGUI to true when first added to a frame/component
		isGUI = true;
		
		// Check if background image is available and draw if so
		if (new File("FadedWhiteBg.png").exists()) {
			g2.drawImage(new ImageIcon("FadedWhiteBg.png").getImage(), 0, 0, 
						 this.getWidth(), this.getHeight(), null);
		}
		
		// set stroke and color
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(2));
		
		// draw axes
		g2.drawLine(20, 5, 20, this.getHeight() - 5);
		g2.drawLine(10, this.getHeight() / 2, this.getWidth() - 10, this.getHeight() / 2);
		g2.setFont(new Font("", Font.PLAIN, 10));
		g2.drawString(yLabel, 25, 10);
		g2.drawString(xLabel, this.getWidth() - 20, this.getHeight() / 2 - 5);
		
		// draw lines between each set of data points
		Object[] points = data.toArray();
		for (int i = 1; i < points.length; i++) {
			Point first = (Point)points[i - 1];
			Point second = (Point)points[i];
			
			// scale the points
			double x1 = ((first.getX() / xScale) * (this.getWidth() - 30)) + 40;
			double y1 = this.getHeight() / 2 - ((first.getY() / yScale) * (this.getHeight() - 10));
			double x2 = ((second.getX() / xScale) * (this.getWidth() - 30)) + 40;
			double y2 = this.getHeight() / 2 - ((second.getY() / yScale) * (this.getHeight() - 10));
			
			// adjust x values for offset
			x1 -= ((xOffset / xScale) * (this.getWidth() - 30)) + 20;
			x2 -= ((xOffset / xScale) * (this.getWidth() - 30)) + 20;
			
			// draw the line
			Line2D.Double line = new Line2D.Double(x1, y1, x2, y2);
			g2.draw(line);
		}
	}
}
