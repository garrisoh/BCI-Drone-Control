package edu.lafayette.bci;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A UI class for displaying the current direction of travel of the drone and control instructions.
 *
 * @author Haley Garrison
 */
public class DroneControlPanelUI implements WindowListener {

	// Constants for the current direction of travel
	public static final int FORWARD = 1;
	public static final int BACKWARD = 1 << 1;
	public static final int LEFT = 1 << 2;
	public static final int RIGHT = 1 << 3;
	public static final int CCW = 1 << 4;
	public static final int CW = 1 << 5;
	public static final int HOVER = 0;
	
	// Variables to keep track of the state of the drone
	// and the mode/mode selection
	private boolean takeoff = false;
	private double vertSpeed = 0.0;
	
	// Window and window closed flags
	private JFrame window = null;
	private boolean closed = false;
	
	// UI components
	private JLabel image = null;
	private JLabel topLabel = null;
	private JLabel bottomLabel = null;
	
	// Keeps track of current state
	private int currDirection = HOVER;
	
	/**
	 * Creates a new DroneControlPanelUI and displays the window on the screen
	 */
	public DroneControlPanelUI() {
		// Create the window
		window = new JFrame("DroneControlPanel");
		window.setSize(650, 800);
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.addWindowListener(this);
		window.setContentPane(new VertSpeedPanel());
		
		// Set the background color and border
		window.getContentPane().setBackground(new Color(85, 85, 85));
		
		// Set layout manager
		BoxLayout b = new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS);
		window.setLayout(b);
		
		// Add space to top
		window.getContentPane().add(Box.createVerticalStrut(20));
		
		// Add the top label
		topLabel = new JLabel("Drone Control Panel");
		topLabel.setFont(new Font("Marker Felt", Font.TRUETYPE_FONT, 30));
		topLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		window.getContentPane().add(topLabel);
		
		// Add the landed image
		ImageIcon icon = new ImageIcon(this.getClass().getResource("resources/Drone.png"));
		image = new JLabel(icon);
		image.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		window.getContentPane().add(image);
		
		// Add the bottom label
		bottomLabel = new JLabel("Takeoff/Land: Blink x5, Toggle Forward/Stop: Close Eyes, Turn: Rotate Head (Reset Shift+C)");
		bottomLabel.setFont(new Font("Marker Felt", Font.TRUETYPE_FONT, 18));
		bottomLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		window.getContentPane().add(bottomLabel);
		
		// TODO: Info button for instructions
		
		window.setVisible(true);
	}
	
	/**
	 * Sets the text on the UI's top label.
	 * 
	 * @param text The new label text.
	 */
	public void setTopLabel(String text) {
		topLabel.setText(text);
	}
	
	/**
	 * Sets the text on the UI's bottom label.
	 * 
	 * @param text The new label text.
	 */
	public void setBottomLabel(String text) {
		bottomLabel.setText(text);
	}
	
	/**
	 * Gets the current state of the UI
	 * 
	 * @return A bitmask indicating the currently set direction
	 */
	public int getDirection() {
		return currDirection;
	}
	
	/**
	 * Sets the direction of travel to display on the UI.
	 * The direction must be one of the constants in this
	 * class.
	 * 
	 * @param direction Direction of travel.  Must be one of the constants or a bitmask
	 * of multiple directions
	 */
	public void setDirection(int direction) {
		// Don't set a direction if we haven't taken off
		if (!takeoff || direction == currDirection) {
			return;
		}
		
		// TODO: Add all directions
		// Set the direction
		switch(direction) {
			case FORWARD: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneF.png"))); break;
			case BACKWARD: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneB.png"))); break;
			case LEFT: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneL.png"))); break;
			case RIGHT: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneR.png"))); break;
			case CCW: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneCCW.png"))); break;
			case CW: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneCW.png"))); break;
			case FORWARD | CW: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneFCW.png"))); break;
			case FORWARD | CCW: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneFCCW.png"))); break;
			case HOVER:
			default: image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneHover.png")));
		}
		
		currDirection = direction;
	}
	
	/**
	 * Sets whether the drone is flying or has landed.
	 * 
	 * @param takeoff True if flying, false otherwise
	 */
	public void setTakeoff(boolean takeoff) {
		this.takeoff = takeoff;
		
		// Set the correct image
		if (takeoff) {
			image.setIcon(new ImageIcon(this.getClass().getResource("resources/DroneHover.png")));
		} else {
			image.setIcon(new ImageIcon(this.getClass().getResource("resources/Drone.png")));
		}
	}
	
	/**
	 * Sets the vertical speed indicator level on the UI.
	 * 
	 * @param speed Vertical speed, from -1.0 to 1.0, + = up
	 */
	public void setVertSpeed(double speed) {
		this.vertSpeed = speed;
		
		// Repaint the content pane to update the speed gauges
		window.getContentPane().repaint();
	}
	
	/**
	 * Adds a key listener to the window
	 * 
	 * @param k A class implementing the KeyListener interface
	 */
	public void addKeyListener(KeyListener k) {
		window.addKeyListener(k);
	}
	
	/**
	 * A JPanel Subclass for drawing a custom border indicating vertical velocity.
	 *
	 * @author Haley Garrison
	 */
	private class VertSpeedPanel extends JPanel {
		// Silences warnings from Eclipse
		private static final long serialVersionUID = 123;
		
		/**
		 * Creates a new VertSpeedPanel
		 */
		public VertSpeedPanel() {
			super();
		}
		
		/**
		 * Paints black borders on the sides of the window with a colored stripe
		 * indicating the speed.
		 */
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			// Draw black borders
			Graphics2D g2 = (Graphics2D)g;
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(4));
			g2.drawLine(2, 0, 2, window.getHeight());
			g2.drawLine(window.getWidth() - 2, 0, window.getWidth() - 2, window.getHeight());
			
			// Draw a line indicating velocity
			g2.setColor(new Color(159, 223, 111));
			g2.drawLine(0, (int)(window.getHeight() / 2 - vertSpeed * window.getHeight() / 2),
					    4, (int)(window.getHeight() / 2 - vertSpeed * window.getHeight() / 2));
			g2.drawLine(window.getWidth() - 4, (int)(window.getHeight() / 2 - vertSpeed * window.getHeight() / 2),
				    	window.getWidth(), (int)(window.getHeight() / 2 - vertSpeed * window.getHeight() / 2));
		}
	}
	
	/**
	 * Indicates whether the window has been closed or not
	 */
	public boolean isWindowClosed() {
		return closed;
	}
	
	/**************** WindowListener Methods *****************/
	
	/**
	 * Invoked when a window has been closed as the result of calling 
	 * dispose on the window.  This method sets the close flag to true.
	 */
	public void windowClosed(WindowEvent e) {
		closed = true;
	}
	
	/************* Unused Methods ***************/
	
	/**
	 * Unused
	 */
	public void windowActivated(WindowEvent e) {}
	
	/**
	 * Unused
	 */
	public void windowClosing(WindowEvent e) {}
	
	/**
	 * Unused
	 */
	public void windowDeactivated(WindowEvent e) {}
	
	/**
	 * Unused
	 */
	public void windowDeiconified(WindowEvent e) {}
	
	/**
	 * Unused
	 */
	public void windowIconified(WindowEvent e) {}
	
	/**
	 * Unused
	 */
	public void windowOpened(WindowEvent e) {}
}
