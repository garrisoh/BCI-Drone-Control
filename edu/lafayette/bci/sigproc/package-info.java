/**
 * This package implements a signal processing framework.  The Graph class
 * serves as a container for two-dimensional Points.  Graphs may contain
 * a Pipeline of Algorithms.  Any points added to the Graph will be pushed
 * through each Algorithm in the Pipeline in the order that the Algorithms
 * were added to the Pipeline.<br><br>
 * 
 * The Algorithms provided include a Butterworth filter (LPF, HPF, or BPF),
 * a simple 2nd order HighPassFilter, a MovingAverage filter, a Power
 * estimation algorithm, a Threshold detector, a PulseCount algorithm, an
 * EdgeDetect algorithm, and a GyroDetect algorithm (positional and velocity).
 * 
 * @author Haley Garrison
 */
package edu.lafayette.bci.sigproc;