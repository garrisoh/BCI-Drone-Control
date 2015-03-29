package edu.lafayette.bci.devices;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * This class provides a control mechanism for the Parrot
 * Ar.drone.  It communicates with a node.js server (or the simulator) running on
 * localhost port 5678.  This server uses the <a href="http://nodecopter.com">
 * nodecopter library</a> to send control commands to the drone.
 * The server must be running in order for this class to work.
 *
 * @author Haley Garrison
 */
public class Parrot {

	// The PrintWriter used to write control commands to the output
	// stream.
	private PrintWriter p = null;
	
	/**
	 * Creates a new instance of Parrot and initializes the data stream
	 * for communicating with the server.
	 */
	public Parrot() {
		try {
			p = new PrintWriter(
					new Socket("localhost", 5678).getOutputStream()
				);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Causes the drone to takeoff and hover a few feet above the ground
	 */
	public void takeoff() {
		p.print("takeoff");
		p.flush();
	}
	
	/**
	 * Causes the drone to land.
	 */
	public void land() {
		p.print("land");
		p.flush();
	}
	
	/**
	 * Stops all movement of the drone and makes it hover in place.
	 */
	public void hover() {
		p.print("hover");
		p.flush();
	}
	
	/**
	 * Moves the drone upward at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void up(double speed) {
		p.print("up " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Moves the drone downward at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void down(double speed) {
		p.print("down " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Rolls the drone left at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void left(double speed) {
		p.print("left " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Rolls the drone right at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void right(double speed) {
		p.print("right " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Pitches the drone forward at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void forward(double speed) {
		p.print("forward " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Pitches the drone backward at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void backward(double speed) {
		p.print("backward " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Yaws the drone clockwise at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void clockwise(double speed) {
		p.print("clockwise " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Yaws the drone counter-clockwise at the specified speed.
	 * 
	 * @param speed A value between 0 and 1 with 1 as max speed.
	 */
	public void counterClockwise(double speed) {
		p.print("counterClockwise " + clamp(speed, 0, 1));
		p.flush();
	}
	
	/**
	 * Closes the output stream, ending communication with the server.
	 */
	public void close() {
		p.close();
	}
	
	/**
	 * A helper function that clamps a value to the min and max values.
	 * 
	 * @param value The value to clamp
	 * @param min The maximum allowable value
	 * @param max The minimum allowable value
	 * @return The clamped value
	 */
	private double clamp(double value, double min, double max) {
		return (value < min) ? min : (value > max) ? max : value;
	}
}
