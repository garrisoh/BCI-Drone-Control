package edu.lafayette.bci.sigproc;

import java.util.LinkedList;

/**
 * A Butterworth filter algorithm.  Instances of this class can be 
 * a lowpass filter, a highpass filter, or a bandpass filter.
 * Code is a modified version of the code found here:
 * 
 * <a href=https://github.com/sikoried/jstk/blob/master/jstk/src/de/fau/cs/jstk/sampled/filters/Butterworth.java>
 * Butterworth.java</a>
 *
 * @author Haley Garrison
 */
public class Butterworth extends Algorithm {
	// Constants for defining the filter type
	/** Constant for the High-Pass Filter type. */
	public static int HPF = 0;
	/** Constant for the Low-Pass Filter type. */
	public static int LPF = 1;
	/** Constant for the Band-Pass Filter type. */
	public static int BPF = 2;
	
	// Transfer function numerator and denominator
	private double[] numB = null;
	private double[] denA = null;
	
	// Input buffer and size
	private LinkedList<Point> buffer = null;
	private int bufferSize = 0;
	
	// Output buffer
	private LinkedList<Point> prevFilt = null;
	
	/**
	 * Creates a new Butterworth filter of the order given with the frequency(ies)
	 * specified and the type given.  Order should not be too high to improve efficiency.
	 * 
	 * @param order The filter order (preferably less than 7)
	 * @param freqs The filter frequency(ies).  Can have up to two for BPF.
	 * @param samplingRate The sampling rate of the system in Hz
	 * @param type LPF=1, HPF=0, BPF=2
	 */
	public Butterworth(int order, double[] freqs, double samplingRate, int type) {
		// Normalize the frequencies
		double[] fnorms = null;
		if (freqs.length == 1) {
			fnorms = new double[1];
			fnorms[0] = 2 * freqs[0] / samplingRate;
		} else {
			fnorms = new double[2];
			fnorms[0] = 2 * freqs[0] / samplingRate;
			fnorms[1] = 2 * freqs[1] / samplingRate;
		}
		
		// Calculate transfer function
		numB = calcBCoeff(order, type);
		denA = calcACoeff(order, fnorms);
		
		double factor = calcBFactor(order, fnorms, type);
		
		// Multiply scale by B
		for(int i = 0; i < numB.length; i++) {
			numB[i] *= factor;
		}
		
		// Initialize the buffer
		buffer = new LinkedList<Point>();
		bufferSize = numB.length;
		
		prevFilt = new LinkedList<Point>();
	}
	
	/**
	 * Processes a single point and returns a single point.
	 * This method implements a butterworth filter at the given
	 * cutoff frequencies.
	 * 
	 * @param p The point to be processed
	 * @return The processed point
	 */
	public Point process(Point p) {
		if (prevFilt.size() == 0) {
			// Add the initial filtered point
			prevFilt.offer(new Point(p.getX(), numB[0] * p.getY()));
			
			// Add the point
			buffer.offer(p);
			
			if (this.tap != null) {
				this.tap.addPoint(prevFilt.getLast());
			}
			return prevFilt.getLast();
		}

		buffer.offer(p);

		// Calculate filtered output
	    double sum1 = 0.0;
	    for(int k = 0; k < buffer.size(); k++) {
	        sum1 += numB[k] * buffer.get(buffer.size()-k-1).getY();
	    }

	    double sum2 = 0.0;
	    for(int k = 1; k < prevFilt.size() + 1; k++) {
	        sum2 += denA[k] * prevFilt.get(prevFilt.size()-k).getY();
	    }

	    double y = sum1 - sum2;
		
	    // Wait for buffer to fill
 		if (buffer.size() < bufferSize) {
 			prevFilt.offer(new Point(p.getX(), y));
 			if (this.tap != null)
 				this.tap.addPoint(prevFilt.getLast());
 			return prevFilt.getLast();
 		}
 		
 		// Add to the previous
 		prevFilt.poll();
	    prevFilt.offer(new Point(p.getX(), y));
 		
	    // Remove the oldest point
		buffer.poll();
		if (this.tap != null) {
			this.tap.addPoint(prevFilt.getLast());
		}
		return prevFilt.getLast();
	}
	
	/*********** Coefficient Calculation ************/
	
	/**
	 * Calculates the factor by which B coefficients are multiplied.
	 * 
	 * @param order The order of the filter
	 * @param fnorm The normalized frequency(ies) of filtration (f / Nyquist), low first
	 * @param type The type of filter (HPF, LPF, or BPF)
	 * @return The value by which all B coefficients are multiplied
	 */
	private double calcBFactor(int order, double[] fnorm, int type) {
		if (type != BPF) {
			double omega = Math.PI * fnorm[0];
		    double sinOmega = Math.sin(omega);
		    
		    double piOverOrder = Math.PI / (2 * order);
		    
		    // Even order
		    double factor = 1.0;
		    for(int k = 0; k < order/2 ; k++) {
		        factor *= 1 + sinOmega * Math.sin((2*k+1) * piOverOrder);
		    }
		    
		    // Use cos instead of sin for highpass
		    if(type == LPF) {
		        sinOmega = Math.sin(omega/2.0);
		    } else {
		        sinOmega = Math.cos(omega/2.0);
		    }
		    
		    // Odd order
		    if(order % 2 == 1) {
		        factor *= sinOmega + ((type == LPF) ? Math.cos(omega / 2.0) : 
		        									  Math.sin(omega / 2.0));
		    }
		    
		    factor = Math.pow(sinOmega, order) / factor;
		    
		    return factor;
		} else {
		    double pang;      // pole angle
	        double spang;     // sine of pole angle
	        double cpang;     // cosine of pole angle
	        double a, b, c;   // workspace variables
	
	        double tt = Math.tan(Math.PI * (fnorm[1] - fnorm[0]) / 2.);
	        tt = 1. / tt;
	        
	        double sfr = 1.;
	        double sfi = 0.;
	
	        for (int k = 0; k < order; k++) {
	                pang = Math.PI * (double)(2*k+1)/(double)(2*order);
	                spang = tt + Math.sin(pang);
	                cpang = Math.cos(pang);
	                a = (sfr + sfi)*(spang - cpang);
	                b = sfr * spang;
	                c = -sfi * cpang;
	                sfr = b - c;
	                sfi = a - b - c;
	        }
	
	        return 1.0 / sfr;
		}
	}
	
	/**
	 * Calculates the coefficients of the numerator of the transfer
	 * function.
	 * 
	 * @param order The filter order
	 * @param type The type of filter (HPF, LPF, or BPF)
	 * @return The B coefficients from most to least significant
	 */
	private double[] calcBCoeff(int order, int type) {
		// Initialize coefficients vector
	    double[] bcoeff = new double[order + 1];
	    
	    // First order filter
	    if(order == 1) {
	        bcoeff[0] = 1;
	    	bcoeff[1] = 1;
	        return bcoeff;
	    }
	    
	    bcoeff[0] = 1;
	    bcoeff[1] = order;
	    
	    for(int i=2; i < order/2 + 1; i++) {
	        bcoeff[i] = (order - i + 1) * bcoeff[i-1] / i;
	        bcoeff[order-i] = bcoeff[i];
	    }
	    
	    bcoeff[order - 1] = order;
	    bcoeff[order] = 1;
	    
	    // Invert for highpass
	    if(type != LPF) {
	        for(int j=1; j < order + 1; j+=2) {
	            bcoeff[j] = -bcoeff[j];
	        }
	    }
	    
	    // Zero every other for bandpass
	    if(type == BPF) {
	    	double[] bcoeff2 = new double [2*order + 1];
	    	for (int i = 0; i < order; ++i) {
                bcoeff2[2*i] = bcoeff[i];
                bcoeff2[2*i + 1] = 0.0;
	        }
	
	        bcoeff2[2*order] = bcoeff[order];
	        return bcoeff2;
	    }
	    
		return bcoeff;
	}
	
	/**
	 * Calculates the coefficients of the denominator of the transfer
	 * function.
	 * 
	 * @param order The filter order
	 * @param fnorm The normalized frequency(ies) of filtration (f / Nyquist).  Low is first.
	 * @return The A coefficients from most to least significant
	 */
	private double[] calcACoeff(int order, double[] fnorm) {
		// LPF or HPF
		if (fnorm.length <= 1) {
			double theta = Math.PI * fnorm[0];
		    double stheta = Math.sin(theta);
		    double ctheta = Math.cos(theta);
		    
		    double[] coef1 = new double[2 * order];
		    
		    for(int k = 0; k < order; k++) {
		        double pang = Math.PI * (2*k + 1) / (2.0 * order);
		        double spang = Math.sin(pang);
		        double cpang = Math.cos(pang);
		        double a = 1.0 + stheta * spang;
		        
		        coef1[2 * k] = -ctheta / a;
		        coef1[2 * k + 1] = -stheta * cpang / a;
		    }
		    
		    double[] temp = binomialMult(coef1);
		    double[] acoeff = new double[order+1];
	
		    acoeff[0] = 1.0;
		    acoeff[1] = temp[0];
		    
		    // First order filter
		    if(order > 1) {
		        acoeff[2] = temp[2];
		    }
		        
		    for(int k = 3; k < order + 1; k++) {
		       acoeff[k] = temp[2*k - 2];
		    }
		    
			return acoeff;
		} else {
			// BPF
			double pang;    // pole angle
            double spang;   // sine of pole angle
            double cpang;   // cosine of pole angle
            double a;       // workspace variables

            double cp = Math.cos(Math.PI * (fnorm[1] + fnorm[0]) / 2.);
            double theta = Math.PI * (fnorm[1] - fnorm[0]) / 2.;
            double st = Math.sin(theta);
            double ct = Math.cos(theta);
            double s2t = 2. * st * ct; // sine of 2*theta
            double c2t = 2. * ct * ct - 1.0; // cosine of 2*theta

            double [] rcof = new double [2*order]; // z^-2 coefficients
            double [] tcof = new double [2*order]; // z^-1 coefficients

            for (int k = 0; k < order; ++k) {
                    pang = Math.PI * (double) (2 * k + 1) / (double) (2 * order);
                    spang = Math.sin(pang);
                    cpang = Math.cos(pang);
                    a = 1.0 + s2t * spang;
                    rcof[2*k] = c2t / a;
                    rcof[2*k + 1] = 1.0 * s2t * cpang / a;
                    tcof[2*k] = -2.0 * cp * (ct + st * spang) / a;
                    tcof[2*k + 1] = -2.0 * cp * st * cpang / a;
            }

            // compute trinomial
            double [] temp = trinomialMult(tcof, rcof);
            
            // we only need the 2n+1 coefficients
            double [] dcof = new double [2*order + 1];
            dcof[0] = 1.0;
            dcof[1] = temp[0];
            dcof[2] = temp[2];
            for (int k = 3; k < 2*order + 1; ++k)
                    dcof[k] = temp[2 * k - 2];
            
            return dcof;
		}
	}
	
	/**
	 * A helper function for calculating the A coefficients.  For
	 * more detail, see the link above.
	 */
	private double[] binomialMult(double[] p) {
		int n = p.length / 2;
	    double[] a = new double[2 * n];

	    for(int i = 0; i < n; i++) {
	            for(int j = i; j > 0; j--) {
	                    a[2 * j] += p[2 * i] * a[2 * (j - 1)] - 
	                    			p[2 * i + 1] * a[2 * (j - 1) + 1];
	                
	                    a[2 * j + 1] += p[2 * i] * a[2 * (j - 1) + 1] +
	                    				p[2 * i + 1] * a[2 * (j - 1)];
	            }

	            a[0] += p[2 * i];
	            a[1] += p[2 * i + 1];
	    }
	    
	    return a;
	}
	
	/**
	 * A helper function for calculating the A coefficients.  For
	 * more detail, see the link above.
	 */
	private double [] trinomialMult(double [] b, double [] c) {
        int n = b.length / 2;
        double [] a = new double [4 * n];

        a[0] = b[0];
        a[1] = b[1];
        a[2] = c[0];
        a[3] = c[1];

        for (int i = 1; i < n; ++i) {
                a[2 * (2 * i + 1)] += c[2 * i] * a[2 * (2 * i - 1)] - c[2 * i + 1]
                                * a[2 * (2 * i - 1) + 1];
                a[2 * (2 * i + 1) + 1] += c[2 * i] * a[2 * (2 * i - 1) + 1]
                                + c[2 * i + 1] * a[2 * (2 * i - 1)];

                for (int j = 2 * i; j > 1; --j) {
                        a[2 * j] += b[2 * i] * a[2 * (j - 1)] - b[2 * i + 1]
                                        * a[2 * (j - 1) + 1] + c[2 * i] * a[2 * (j - 2)]
                                        - c[2 * i + 1] * a[2 * (j - 2) + 1];
                        a[2 * j + 1] += b[2 * i] * a[2 * (j - 1) + 1] + b[2 * i + 1]
                                        * a[2 * (j - 1)] + c[2 * i] * a[2 * (j - 2) + 1]
                                        + c[2 * i + 1] * a[2 * (j - 2)];
                }

                a[2] += b[2 * i] * a[0] - b[2 * i + 1] * a[1] + c[2 * i];
                a[3] += b[2 * i] * a[1] + b[2 * i + 1] * a[0] + c[2 * i + 1];
                a[0] += b[2 * i];
                a[1] += b[2 * i + 1];
        }

        return a;
	}
}
