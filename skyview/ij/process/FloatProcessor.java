package ij.process;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import ij.gui.*;

/** This is an 32-bit floating-point image and methods that operate on that image. */
public class FloatProcessor extends ImageProcessor {

	private float min, max, snapshotMin, snapshotMax;
	private float[] pixels;
	private byte[] pixels8;
	private float[] snapshotPixels = null;
	private byte[] LUT = null;
	private float fillColor =  Float.MAX_VALUE;
	//private float bgColor = Float.MIN_VALUE;
	private boolean fixedScale = false;

	/** Creates a new FloatProcessor using the specified pixel array and ColorModel.
		Set 'cm' to null to use the default grayscale LUT. */
	public FloatProcessor(int width, int height, float[] pixels, ColorModel cm) {
		if (pixels!=null && width*height!=pixels.length)
			throw new IllegalArgumentException(WRONG_LENGTH);
		this.width = width;
		this.height = height;
		this.pixels = pixels;
		this.cm = cm;
		resetRoi();
		if (pixels!=null)
			findMinAndMax();
	}

	/** Creates a blank FloatProcessor using the default grayscale LUT that
		displays zero as black. Call invertLut() to display zero as white. */
	public FloatProcessor(int width, int height) {
		this(width, height, new float[width*height], null);
	}

	/** Creates a FloatProcessor from an int array using the default grayscale LUT. */
	public FloatProcessor(int width, int height, int[] pixels) {
		this(width, height);
		for (int i=0; i<pixels.length; i++)
			this.pixels[i] = (float)(pixels[i]);
		findMinAndMax();
	}
	
	/** Creates a FloatProcessor from a double array using the default grayscale LUT. */
	public FloatProcessor(int width, int height, double[] pixels) {
		this(width, height);
		for (int i=0; i<pixels.length; i++)
			this.pixels[i] = (float)pixels[i];
		findMinAndMax();
	}
	
	public FloatProcessor(int width, int height, double[] pixels, double fixedMin, double fixedMax) {
		this(width, height);
		for (int i=0; i<pixels.length; i++)
			this.pixels[i] = (float)pixels[i];
                //findMinAndMax();
		setMinAndMax(fixedMin, fixedMax);
		this.min = (float)fixedMin;
		this.max = (float)fixedMax;
	}
	
	/** Creates a FloatProcessor from a float[][] array using the default LUT. */
	public FloatProcessor(float[][] array) {
		width = array.length;
		height = array[0].length;
		pixels = new float[width*height];
		int i=0;
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				pixels[i++] = array[x][y];
			}
		}
		resetRoi();
		findMinAndMax();
	}

	/** Creates a FloatProcessor from an int[][] array. */
	public FloatProcessor(int[][] array) {
		this(array.length, array[0].length);
		setIntArray(array);
		findMinAndMax();
	}

	/**
	Calculates the minimum and maximum pixel value for the entire image. 
	Returns without doing anything if fixedScale has been set true as a result
	of calling setMinAndMax(). In this case, getMin() and getMax() return the
	fixed min and max defined by setMinAndMax(), rather than the calculated min
	and max.
	@see #getMin()
	@see #getMin()
	*/
	public void findMinAndMax() {
		if (fixedScale)
			return;
		min = Float.MAX_VALUE;
		max = -Float.MAX_VALUE;
		for (int i=0; i < width*height; i++) {
			float value = pixels[i];
			if (!Float.isInfinite(value)) {
				if (value<min)
					min = value;
				if (value>max)
					max = value;
			}
		}
		showProgress(1.0);
	}

	/**
	Sets the min and max variables that control how real
	pixel values are mapped to 0-255 screen values. Use
	resetMinAndMax() to enable auto-scaling;
	@see ij.plugin.frame.ContrastAdjuster 
	*/
	public void setMinAndMax(double min, double max) {
		if (min==0.0 && max==0.0)
			{resetMinAndMax(); return;}
		this.min = (float)min;
		this.max = (float)max;
		fixedScale = true;
		resetThreshold();
	}

	/** Recalculates the min and max values used to scale pixel
		values to 0-255 for display. This ensures that this 
		FloatProcessor is set up to correctly display the image. */
	public void resetMinAndMax() {
		fixedScale = false;
		findMinAndMax();
		resetThreshold();
	}

	/** Returns the smallest displayed pixel value. */
	public double getMin() {
		return min;
	}

	/** Returns the largest displayed pixel value. */
	public double getMax() {
		return max;
	}

	public Image createImage() {
		boolean firstTime = pixels8==null;
		if (firstTime || !lutAnimation) {
			// scale from float to 8-bits
			int size = width*height;
			if (pixels8==null)
				pixels8 = new byte[size];
			float value;
			int ivalue;
			float scale = 255f/(max-min);
			for (int i=0; i<size; i++) {
				value = pixels[i]-min;
				if (value<0f) value = 0f;
				ivalue = (int)(value*scale);
				if (ivalue>255) ivalue = 255;
				pixels8[i] = (byte)ivalue;
			}
		}
		if (cm==null)
			makeDefaultColorModel();
		if (source==null) {
			source = new MemoryImageSource(width, height, cm, pixels8, 0, width);
			source.setAnimated(true);
			source.setFullBufferUpdates(true);
			img = Toolkit.getDefaultToolkit().createImage(source);
		} else if (newPixels) {
			source.newPixels(pixels8, cm, 0, width);
			newPixels = false;
		} else
			source.newPixels();
		lutAnimation = false;
	    return img;
	}
	
	/** Returns a new, blank FloatProcessor with the specified width and height. */
	public ImageProcessor createProcessor(int width, int height) {
		ImageProcessor ip2 = new FloatProcessor(width, height, new float[width*height], getColorModel());
		ip2.setMinAndMax(getMin(), getMax());
		return ip2;
	}

	public void snapshot() {
		snapshotWidth=width;
		snapshotHeight=height;
		snapshotMin=min;
		snapshotMax=max;
		if (snapshotPixels==null || (snapshotPixels!=null && snapshotPixels.length!=pixels.length))
			snapshotPixels = new float[width * height];
        System.arraycopy(pixels, 0, snapshotPixels, 0, width*height);
	}
	
	public void reset() {
		if (snapshotPixels==null)
			return;
	    min=snapshotMin;
		max=snapshotMax;
        System.arraycopy(snapshotPixels,0,pixels,0,width*height);
	}
	
	public void reset(ImageProcessor mask) {
		if (mask==null || snapshotPixels==null)
			return;	
		if (mask.getWidth()!=roiWidth||mask.getHeight()!=roiHeight)
			throw new IllegalArgumentException(maskSizeError(mask));
		byte[] mpixels = (byte[])mask.getPixels();
		for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
			int i = y * width + roiX;
			int mi = my * roiWidth;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				if (mpixels[mi++]==0)
					pixels[i] = snapshotPixels[i];
				i++;
			}
		}
	}

	public void setSnapshotPixels(Object pixels) {
		snapshotPixels = (float[])pixels;
		snapshotWidth=width;
		snapshotHeight=height;
	}

	/** Returns a pixel value that must be converted using
		Float.intBitsToFloat(). */
	public int getPixel(int x, int y) {
		if (x>=0 && x<width && y>=0 && y<height)
			return Float.floatToIntBits(pixels[y*width+x]);
		else
			return 0;
	}

	public final int get(int x, int y) {
		return (int)pixels[y*width+x];
	}

	public final void set(int x, int y, int value) {
		pixels[y*width + x] = value;
	}

	public final float getf(int x, int y) {
		return pixels[y*width+x];
	}

	public final void setf(int x, int y, float value) {
		pixels[y*width + x] = value;
	}

    /** Returns the value of the pixel at (x,y) in a
    	one element int array. iArray is an optiona
		preallocated array. */
	public int[] getPixel(int x, int y, int[] iArray) {
		if (iArray==null) iArray = new int[1];
		iArray[0] = (int)getPixelValue(x, y);
		return iArray;
	}

	/** Sets a pixel in the image using a one element int array. */
	public void putPixel(int x, int y, int[] iArray) {
		putPixelValue(x, y, iArray[0]);
	}

	/** Uses bilinear interpolation to find the pixel value at real coordinates (x,y). */
	public double getInterpolatedPixel(double x, double y) {
		if (x<0.0) x = 0.0;
		if (x>=width-1.0) x = width-1.001;
		if (y<0.0) y = 0.0;
		if (y>=height-1.0) y = height-1.001;
		return getInterpolatedPixel(x, y, pixels);
	}
		
	/** Stores the specified value at (x,y). The value is expected to be a
		float that has been converted to an int using Float.floatToIntBits(). */
	public void putPixel(int x, int y, int value) {
		if (x>=0 && x<width && y>=0 && y<height)
			pixels[y*width + x] = Float.intBitsToFloat(value);
	}

	/** Stores the specified real value at (x,y). */
	public void putPixelValue(int x, int y, double value) {
		if (x>=0 && x<width && y>=0 && y<height)
			pixels[y*width + x] = (float)value;
	}

	/** Returns the value of the pixel at (x,y) as a float. */
	public float getPixelValue(int x, int y) {
		if (x>=0 && x<width && y>=0 && y<height)
			return pixels[y*width + x];
		else
			return 0f;
	}

	/** Draws a pixel in the current foreground color. */
	public void drawPixel(int x, int y) {
		if (x>=clipXMin && x<=clipXMax && y>=clipYMin && y<=clipYMax)
			putPixel(x, y, Float.floatToIntBits(fillColor));
	}

	/**	Returns a reference to the float array containing
		this image's pixel data. */
	public Object getPixels() {
		return (Object)pixels;
	}

	/** Returns a reference to this image's snapshot (undo) array
		if it is not null and 'snapshotCopyMode' is true. Otherwise,
		returns a copy of the pixel data. */
	public Object getPixelsCopy() {
		if (snapshotCopyMode && snapshotPixels!=null) {
			snapshotCopyMode = false;
			return snapshotPixels;
		} else {
			float[] pixels2 = new float[width*height];
        	System.arraycopy(pixels, 0, pixels2, 0, width*height);
			return pixels2;
		}
	}

	public void setPixels(Object pixels) {
		this.pixels = (float[])pixels;
		resetPixels(pixels);
		if (pixels==null) snapshotPixels = null;
		if (pixels==null) pixels8 = null;
	}

	/** Copies the image contained in 'ip' to (xloc, yloc) using one of
		the transfer modes defined in the Blitter interface. */
	public void copyBits(ImageProcessor ip, int xloc, int yloc, int mode) {
		//if (!(ip instanceof FloatProcessor))
		//	throw new IllegalArgumentException("32-bit (real) image required");
		new FloatBlitter(this).copyBits(ip, xloc, yloc, mode);
	}

	public void applyTable(int[] lut) {}

	private void process(int op, double value) {
		float c, v1, v2;
		boolean resetMinMax = roiWidth==width && roiHeight==height && !(op==FILL);
		c = (float)value;
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			int i = y * width + roiX;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				v1 = pixels[i];
				switch(op) {
					case INVERT:
						v2 = max - (v1 - min);
						break;
					case FILL:
						v2 = fillColor;
						break;
					case ADD:
						v2 = v1 + c;
						break;
					case MULT:
						v2 = v1 * c;
						break;
					case GAMMA:
						if (v1<=0f)
							v2 = 0f;
						else
							v2 = (float)Math.exp(c*Math.log(v1));
						break;
					case LOG:
						if (v1<=0f)
							v2 = 0f;
						else
							v2 = (float)Math.log(v1);
						break;
					case EXP:
						v2 = (float)Math.exp(v1);
						break;
					case SQR:
							v2 = v1*v1;
						break;
					case SQRT:
						if (v1<=0f)
							v2 = 0f;
						else
							v2 = (float)Math.sqrt(v1);
						break;
					case ABS:
							v2 = (float)Math.abs(v1);
						break;
					case MINIMUM:
						if (v1<value)
							v2 = (float)value;
						else
							v2 = v1;
						break;
					case MAXIMUM:
						if (v1>value)
							v2 = (float)value;
						else
							v2 = v1;
						break;
					 default:
					 	v2 = v1;
				}
				pixels[i++] = v2;
			}
			if (y%20==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		if (resetMinMax)
			findMinAndMax();
    }

	public void invert() {process(INVERT, 0.0);}
	public void add(int value) {process(ADD, value);}
	public void add(double value) {process(ADD, value);}
	public void multiply(double value) {process(MULT, value);}
	public void and(int value) {}
	public void or(int value) {}
	public void xor(int value) {}
	public void gamma(double value) {process(GAMMA, value);}
	public void log() {process(LOG, 0.0);}
	public void exp() {process(EXP, 0.0);}
	public void sqr() {process(SQR, 0.0);}
	public void sqrt() {process(SQRT, 0.0);}
	public void abs() {process(ABS, 0.0);}
	public void min(double value) {process(MINIMUM, value);}
	public void max(double value) {process(MAXIMUM, value);}



	/** Fills the current rectangular ROI. */
	public void fill() {process(FILL, 0.0);}

	/** Fills pixels that are within roi and part of the mask.
		Does nothing if the mask is not the same as the the ROI. */
	public void fill(ImageProcessor mask) {
		if (mask==null)
			{fill(); return;}
		int roiWidth=this.roiWidth, roiHeight=this.roiHeight;
		int roiX=this.roiX, roiY=this.roiY;
		if (mask.getWidth()!=roiWidth||mask.getHeight()!=roiHeight)
			return;
		byte[] mpixels = (byte[])mask.getPixels();
		for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
			int i = y * width + roiX;
			int mi = my * roiWidth;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				if (mpixels[mi++]!=0)
					pixels[i] = fillColor;
				i++;
			}
		}
	}

	/** 3x3 convolution contributed by Glynne Casteel. */
	public void convolve3x3(int[] kernel) {
		float p1, p2, p3, p4, p5, p6, p7, p8, p9;
		float k1=kernel[0], k2=kernel[1], k3=kernel[2],
		      k4=kernel[3], k5=kernel[4], k6=kernel[5],
		      k7=kernel[6], k8=kernel[7], k9=kernel[8];

		float scale = 0f;
		for (int i=0; i<kernel.length; i++)
			scale += kernel[i];
		if (scale==0) scale = 1f;
		int inc = roiHeight/25;
		if (inc<1) inc = 1;
		
		float[] pixels2 = (float[])getPixelsCopy();
		int offset;
		float sum;
        int rowOffset = width;
		for (int y=yMin; y<=yMax; y++) {
			offset = xMin + y * width;
			p1 = 0f;
			p2 = pixels2[offset-rowOffset-1];
			p3 = pixels2[offset-rowOffset];
			p4 = 0f;
			p5 = pixels2[offset-1];
			p6 = pixels2[offset];
			p7 = 0f;
			p8 = pixels2[offset+rowOffset-1];
			p9 = pixels2[offset+rowOffset];

			for (int x=xMin; x<=xMax; x++) {
				p1 = p2; p2 = p3;
				p3 = pixels2[offset-rowOffset+1];
				p4 = p5; p5 = p6;
				p6 = pixels2[offset+1];
				p7 = p8; p8 = p9;
				p9 = pixels2[offset+rowOffset+1];
				sum = k1*p1 + k2*p2 + k3*p3
				    + k4*p4 + k5*p5 + k6*p6
				    + k7*p7 + k8*p8 + k9*p9;
				sum /= scale;
				pixels[offset++] = sum;
			}
			if (y%inc==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		showProgress(1.0);
	}

	/** Filters using a 3x3 neighborhood. */
	public void filter(int type) {
		float p1, p2, p3, p4, p5, p6, p7, p8, p9;
		int inc = roiHeight/25;
		if (inc<1) inc = 1;
		
		float[] pixels2 = (float[])getPixelsCopy();
		int offset;
		float sum1, sum2;
        int rowOffset = width;
		for (int y=yMin; y<=yMax; y++) {
			offset = xMin + y * width;
			p1 = 0f;
			p2 = pixels2[offset-rowOffset-1];
			p3 = pixels2[offset-rowOffset];
			p4 = 0f;
			p5 = pixels2[offset-1];
			p6 = pixels2[offset];
			p7 = 0f;
			p8 = pixels2[offset+rowOffset-1];
			p9 = pixels2[offset+rowOffset];

			for (int x=xMin; x<=xMax; x++) {
				p1 = p2; p2 = p3;
				p3 = pixels2[offset-rowOffset+1];
				p4 = p5; p5 = p6;
				p6 = pixels2[offset+1];
				p7 = p8; p8 = p9;
				p9 = pixels2[offset+rowOffset+1];

				switch (type) {
					case BLUR_MORE:
						pixels[offset++] = (p1+p2+p3+p4+p5+p6+p7+p8+p9)/9f;
						break;
					case FIND_EDGES:
	        			sum1 = p1 + 2*p2 + p3 - p7 - 2*p8 - p9;
	        			sum2 = p1  + 2*p4 + p7 - p3 - 2*p6 - p9;
	        			pixels[offset++] = (float)Math.sqrt(sum1*sum1 + sum2*sum2);
	        			break;
				}
			}
			if (y%inc==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		if (type==BLUR_MORE)
			showProgress(1.0);
		else
			findMinAndMax();
	}

	/** Rotates the image or ROI 'angle' degrees clockwise.
		@see ImageProcessor#setInterpolate
	*/
	public void rotate(double angle) {
		float[] pixels2 = (float[])getPixelsCopy();
		double centerX = roiX + (roiWidth-1)/2.0;
		double centerY = roiY + (roiHeight-1)/2.0;
		int xMax = roiX + this.roiWidth - 1;
		
		double angleRadians = -angle/(180.0/Math.PI);
		double ca = Math.cos(angleRadians);
		double sa = Math.sin(angleRadians);
		double tmp1 = centerY*sa-centerX*ca;
		double tmp2 = -centerX*sa-centerY*ca;
		double tmp3, tmp4, xs, ys;
		int index, ixs, iys;
		double dwidth=width,dheight=height;
		double xlimit = width-1.0, xlimit2 = width-1.001;
		double ylimit = height-1.0, ylimit2 = height-1.001;
		
		for (int y=roiY; y<(roiY + roiHeight); y++) {
			index = y*width + roiX;
			tmp3 = tmp1 - y*sa + centerX;
			tmp4 = tmp2 + y*ca + centerY;
			for (int x=roiX; x<=xMax; x++) {
				xs = x*ca + tmp3;
				ys = x*sa + tmp4;
				if ((xs>=-0.01) && (xs<dwidth) && (ys>=-0.01) && (ys<dheight)) {
					if (interpolate) {
						if (xs<0.0) xs = 0.0;
						if (xs>=xlimit) xs = xlimit2;
						if (ys<0.0) ys = 0.0;			
						if (ys>=ylimit) ys = ylimit2;
				  		pixels[index++] = (float)getInterpolatedPixel(xs, ys, pixels2);
				  	} else {
				  		ixs = (int)(xs+0.5);
				  		iys = (int)(ys+0.5);
				  		if (ixs>=width) ixs = width - 1;
				  		if (iys>=height) iys = height -1;
						pixels[index++] = pixels2[width*iys+ixs];
					}
    			} else
					pixels[index++] = 0;
			}
			if (y%20==0)
			showProgress((double)(y-roiY)/roiHeight);
		}
		showProgress(1.0);
	}

	public void flipVertical() {
		int index1,index2;
		float tmp;
		for (int y=0; y<roiHeight/2; y++) {
			index1 = (roiY+y)*width+roiX;
			index2 = (roiY+roiHeight-1-y)*width+roiX;
			for (int i=0; i<roiWidth; i++) {
				tmp = pixels[index1];
				pixels[index1++] = pixels[index2];
				pixels[index2++] = tmp;
			}
		}
	}
	
    public void noise(double range) {
		Random rnd=new Random();
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			int i = y * width + roiX;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				float RandomBrightness = (float)(rnd.nextGaussian()*range);
				pixels[i] = pixels[i] + RandomBrightness;
				i++;
			}
		}
		resetMinAndMax();
    }

	public ImageProcessor crop() {
		ImageProcessor ip2 = createProcessor(roiWidth, roiHeight);
		float[] pixels2 = (float[])ip2.getPixels();
		for (int ys=roiY; ys<roiY+roiHeight; ys++) {
			int offset1 = (ys-roiY)*roiWidth;
			int offset2 = ys*width+roiX;
			for (int xs=0; xs<roiWidth; xs++)
				pixels2[offset1++] = pixels[offset2++];
		}
        return ip2;
	}
	
	/** Returns a duplicate of this image. */ 
	public synchronized ImageProcessor duplicate() { 
		ImageProcessor ip2 = createProcessor(width, height); 
		float[] pixels2 = (float[])ip2.getPixels(); 
		System.arraycopy(pixels, 0, pixels2, 0, width*height); 
		return ip2; 
	} 

	/** Scales the image or selection using the specified scale factors.
		@see ImageProcessor#setInterpolate
	*/
	public void scale(double xScale, double yScale) {
		double xCenter = roiX + roiWidth/2.0;
		double yCenter = roiY + roiHeight/2.0;
		int xmin, xmax, ymin, ymax;
		
		if ((xScale>1.0) && (yScale>1.0)) {
			//expand roi
			xmin = (int)(xCenter-(xCenter-roiX)*xScale);
			if (xmin<0) xmin = 0;
			xmax = xmin + (int)(roiWidth*xScale) - 1;
			if (xmax>=width) xmax = width - 1;
			ymin = (int)(yCenter-(yCenter-roiY)*yScale);
			if (ymin<0) ymin = 0;
			ymax = ymin + (int)(roiHeight*yScale) - 1;
			if (ymax>=height) ymax = height - 1;
		} else {
			xmin = roiX;
			xmax = roiX + roiWidth - 1;
			ymin = roiY;
			ymax = roiY + roiHeight - 1;
		}
		float[] pixels2 = (float[])getPixelsCopy();
		boolean checkCoordinates = (xScale < 1.0) || (yScale < 1.0);
		int index1, index2, xsi, ysi;
		double ys, xs;
		double xlimit = width-1.0, xlimit2 = width-1.001;
		double ylimit = height-1.0, ylimit2 = height-1.001;
		for (int y=ymin; y<=ymax; y++) {
			ys = (y-yCenter)/yScale + yCenter;
			ysi = (int)ys;
			if (ys<0.0) ys = 0.0;			
			if (ys>=ylimit) ys = ylimit2;
			index1 = y*width + xmin;
			index2 = width*(int)ys;
			for (int x=xmin; x<=xmax; x++) {
				xs = (x-xCenter)/xScale + xCenter;
				xsi = (int)xs;
				if (checkCoordinates && ((xsi<xmin) || (xsi>xmax) || (ysi<ymin) || (ysi>ymax)))
					pixels[index1++] = (float)min;
				else {
					if (interpolate) {
						if (xs<0.0) xs = 0.0;
						if (xs>=xlimit) xs = xlimit2;
						pixels[index1++] = (float)getInterpolatedPixel(xs, ys, pixels2);
					} else
						pixels[index1++] = pixels2[index2+xsi];
				}
			}
			if (y%20==0)
			showProgress((double)(y-ymin)/height);
		}
		showProgress(1.0);
	}

	/** Uses bilinear interpolation to find the pixel value at real coordinates (x,y). */
	private final double getInterpolatedPixel(double x, double y, float[] pixels) {
		int xbase = (int)x;
		int ybase = (int)y;
		double xFraction = x - xbase;
		double yFraction = y - ybase;
		int offset = ybase * width + xbase;
		double lowerLeft = pixels[offset];
		double lowerRight = pixels[offset + 1];
		double upperRight = pixels[offset + width + 1];
		double upperLeft = pixels[offset + width];
		double upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
		double lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);
		return lowerAverage + yFraction * (upperAverage - lowerAverage);
	}

	/** Creates a new FloatProcessor containing a scaled copy of this image or selection. */
	public ImageProcessor resize(int dstWidth, int dstHeight) {
		double srcCenterX = roiX + roiWidth/2.0;
		double srcCenterY = roiY + roiHeight/2.0;
		double dstCenterX = dstWidth/2.0;
		double dstCenterY = dstHeight/2.0;
		double xScale = (double)dstWidth/roiWidth;
		double yScale = (double)dstHeight/roiHeight;
		if (interpolate) {
			dstCenterX += xScale/2.0;
			dstCenterY += yScale/2.0;
		}
		ImageProcessor ip2 = createProcessor(dstWidth, dstHeight);
		float[] pixels2 = (float[])ip2.getPixels();
		double xs, ys;
		double xlimit = width-1.0, xlimit2 = width-1.001;
		double ylimit = height-1.0, ylimit2 = height-1.001;
		int index1, index2;
		for (int y=0; y<=dstHeight-1; y++) {
			ys = (y-dstCenterY)/yScale + srcCenterY;
			if (interpolate) {
				if (ys<0.0) ys = 0.0;
				if (ys>=ylimit) ys = ylimit2;
			}
			index1 = width*(int)ys;
			index2 = y*dstWidth;
			for (int x=0; x<=dstWidth-1; x++) {
				xs = (x-dstCenterX)/xScale + srcCenterX;
				if (interpolate) {
					if (xs<0.0) xs = 0.0;
					if (xs>=xlimit) xs = xlimit2;
					pixels2[index2++] = (float)getInterpolatedPixel(xs, ys, pixels);
				} else
		  			pixels2[index2++] = pixels[index1+(int)xs];
			}
			if (y%20==0)
			showProgress((double)y/dstHeight);
		}
		showProgress(1.0);
		return ip2;
	}

	/** Sets the foreground fill/draw color. */
	public void setColor(Color color) {
		int bestIndex = getBestIndex(color);
		if (bestIndex>0 && getMin()==0.0 && getMax()==0.0) {
			fillColor = bestIndex;
			setMinAndMax(0.0,255.0);
		} else if (bestIndex==0 && getMin()>0.0 && (color.getRGB()&0xffffff)==0)
			fillColor = 0f;
		else
			fillColor = (float)(min + (max-min)*(bestIndex/255.0));
	}
	
	/** Sets the default fill/draw value. */
	public void setValue(double value) {
		fillColor = (float)value;
	}

	/** Does nothing. The rotate() and scale() methods always zero fill. */
	public void setBackgroundValue(double value) {
	}

	public void setThreshold(double minThreshold, double maxThreshold, int lutUpdate) {
		if (minThreshold!=NO_THRESHOLD && max>min) {
			double minT = Math.round(((minThreshold-min)/(max-min))*255.0);
			double maxT = Math.round(((maxThreshold-min)/(max-min))*255.0);
			super.setThreshold(minT, maxT, lutUpdate);
			this.minThreshold = minThreshold;
			this.maxThreshold = maxThreshold;
		} else
			super.resetThreshold();
	}

	/** Performs a convolution operation using the specified kernel. */
	public void convolve(float[] kernel, int kernelWidth, int kernelHeight) {
		snapshot();
		new ij.plugin.filter.Convolver().convolve(this, kernel, kernelWidth, kernelHeight);
	}

	/** Not implemented. */
	public void threshold(int level) {}
	/** Not implemented. */
	public void autoThreshold() {}
	/** Not implemented. */
	public void medianFilter() {}
	/** Not implemented. */
	public int[] getHistogram() {return null;}
	/** Not implemented. */
	public void erode() {}
	/** Not implemented. */
	public void dilate() {}


}

