package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
 
/**
 * 
* @author (C)Dimiter Prodanov
* 		  IMEC
* 
* @acknowledgments Many thanks to Jerome Mutterer for the code contributions and testing.
* 				   Thanks to Wayne Rasband for the code that properly handles the image magnification.
* 		
* @version 		1.2 28 April 2009
* 					- added support for arrow keys
* 					- fixed a bug in the cross position calculation
* 					- added FocusListener behavior
* 					- added support for magnification factors
* 				1.1.6 31 March 2009
* 					- added AdjustmentListener behavior thanks to Jerome Mutterer
* 					- improved pane visualization
* 					- added window rearrangement behavior. Initial code suggested by Jerome Mutterer
* 					- bug fixes by Wayne Raspband
* 				1.1 24 March 2009
* 					- improved projection image resizing
* 					- added ImageListener behaviors
* 					- added check-ups
* 					- improved pane updating
* 				1.0.5 23 March 2009
* 					- fixed pane updating issue
* 				1.0 21 March 2009
* 
* @contents This plugin projects dynamically orthogonal XZ and YZ views of a stack. 
* The output images are calibrated, which allows measurements to be performed more easily. 
*/

public class Orthogonal_Views implements PlugIn, 
									 MouseListener, 
									 MouseMotionListener, 
									 KeyListener, 
									 ActionListener,
									 ImageListener,
									 WindowListener,
									 AdjustmentListener,
									 MouseWheelListener,
									 FocusListener,
									 CommandListener
									 {

	 private boolean isProcessibleRoi=false;
	 private Roi roi;
	 private ImageWindow win;
	 private ImagePlus imp;
	 private ImageCanvas canvas;
	 private static final int H_ROI=0, H_ZOOM=1;
	 private ImagePlus xz_image=new ImagePlus(), yz_image=new ImagePlus(); 
	 private ImageProcessor fp1, fp2;
	 private static final String AX="AX", AY="AY", AZ="AZ", YROT="YROT", SPANELS="STICKY_PANELS"; 
	 private static float ax=(float)Prefs. getDouble(AX,1.0);
	 private static float ay=(float)Prefs. getDouble(AY,1.0);
	 private static float az=(float)Prefs. getDouble(AZ,1.0);
	 //private static boolean rotate=(boolean)Prefs.getBoolean(YROT,false);
	 //private static boolean sticky=(boolean)Prefs.getBoolean(SPANELS,false);
	 private static boolean rotate=false;
	 private static boolean sticky=true;

	 private int xyImX = 0;
	 private int xyImY = 0;
	 private Calibration cal=null, cal_xz=new Calibration(), cal_yz=new Calibration();
	 private double magnification=1.0;
	 private Color color = Roi.getColor();
	private static Orthogonal_Views instance;
	private Updater updater = new Updater();
	private double min, max;
	private Dimension screen = IJ.getScreenSize();
	private boolean flipXZ;
	private boolean syncZoom = true;

	 
	public void run(String arg) {
		imp = IJ.getImage();
		if (imp.getStackSize()==1) {
			IJ.error("Othogonal Views", "Stack Requires");
			return;
		}
		if (instance!=null && imp==instance.imp) {
			IJ.log("instance!=null: "+imp+"  "+instance.imp);
			return;
		}
		instance = this;
		ImageProcessor ip = imp.getProcessor();
		min = ip.getMin();
		max = ip.getMax();
		cal=this.imp.getCalibration();
		double calx=cal.pixelWidth;
		double caly=cal.pixelHeight;
		double calz=cal.pixelDepth;
		ax=1.0f;
		ay=(float)(caly/calx);
		az=(float)(calz/calx);
		
		try {
			win = imp.getWindow();
			//win.setResizable(false);
			win.running = true;
			isProcessibleRoi=processibleRoi(imp);
			canvas = win.getCanvas();
			addListeners(canvas);  
			magnification= canvas.getMagnification();
			if (!isProcessibleRoi) {
				//if (imp.getCurrentSlice()==1)
				//	imp.setSlice(imp.getStackSize()/2);
				imp.setRoi(new PointRoi(imp.getWidth()/2,imp.getHeight()/2));
				Toolbar.getInstance().setTool(Toolbar.POINT);
			}
		} catch (NullPointerException ex) { 
			return;
		}
		
		ImageStack is=imp.getStack();
		calibrate();
		if (createProcessors(is)) {
			if (ip.isColorLut() || ip.isInvertedLut()) {
				ColorModel cm = ip.getColorModel();
				fp1.setColorModel(cm);
				fp2.setColorModel(cm);
			}
			update();
		} else
			dispose();
	}
 
	private void addListeners(ImageCanvas canvass) {
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
		win.addWindowListener ((WindowListener) this);  
		win.addMouseWheelListener((MouseWheelListener) this);
		win.addFocusListener(this);
		Component[] c = win.getComponents();
		//IJ.log(c[1].toString());
		((java.awt.Scrollbar) c[1]).addAdjustmentListener ((AdjustmentListener) this);
		ImagePlus.addImageListener(this);
		Executer.addCommandListener(this);
	}
	 
	private void calibrate() {
		double arat=az/ax;
		double brat=az/ay;
		String unit=cal.getUnit();
		double o_depth=cal.pixelDepth;
		double o_height=cal.pixelHeight;
		double o_width=cal.pixelWidth;
		cal_xz.setUnit(unit);
		if (rotate) {
			cal_xz.pixelHeight=o_depth/arat;
			cal_xz.pixelWidth=o_width*ax;
		} else {
			cal_xz.pixelHeight=o_width*ax;//o_depth/arat;
			cal_xz.pixelWidth=o_depth/arat;
		}
		xz_image.setCalibration(cal_xz);
		cal_yz.setUnit(unit);
		cal_yz.pixelWidth=o_height*ay;
		cal_yz.pixelHeight=o_depth/brat;
		yz_image.setCalibration(cal_yz);
	}

	private void updateMagnification(int x, int y) {
        double magnification= win.getCanvas().getMagnification();
        int z = imp.getCurrentSlice()-1;
        ImageWindow win1 = xz_image.getWindow();
        if (win1==null) return;
        ImageCanvas ic1 = win1.getCanvas();
        double mag1 = ic1.getMagnification();
        double arat = az/ax;
		int zcoord=(int)(arat*z);
		if (flipXZ) zcoord=(int)(arat*(imp.getStackSize()-z));
        while (mag1<magnification) {
        	ic1.zoomIn(x, zcoord);
        	mag1 = ic1.getMagnification();
        }
        while (mag1>magnification) {
        	ic1.zoomOut(x, zcoord);
        	mag1 = ic1.getMagnification();
        }
        ImageWindow win2 = yz_image.getWindow();
        if (win2==null) return;
        ImageCanvas ic2 = win2.getCanvas();
        double mag2 = ic2.getMagnification();
		zcoord=(int)(arat*z);
        while (mag2<magnification) {
        	ic2.zoomIn(zcoord,y);
        	mag2 = ic2.getMagnification();
        }
        while (mag2>magnification) {
        	ic2.zoomOut(zcoord,y);
        	mag2 = ic2.getMagnification();
        }
	}
	
	void updateViews(Point p, ImageStack is) {
		if (fp1==null) return;
		updateXZView(p,is);
		
		float arat=az/ax;
		Calibration cal = imp.getCalibration();
		if (arat!=1.0f) {
			fp1.setInterpolate(true);
			ImageProcessor sfp1=fp1.resize((int)(fp1.getWidth()*ax), (int)(fp1.getHeight()*arat));
			sfp1.setMinAndMax(min, max);
			xz_image.setProcessor("XZ "+p.y, sfp1);
		} else {
			fp1.setMinAndMax(min, max);
	    	xz_image.setProcessor("XZ "+p.y, fp1);
		}
			
		if (rotate)
			updateYZView(p,is);
		else
			updateZYView(p,is);
				
		arat=az/ay;
		if (arat!=1.0f) {
			fp2.setInterpolate(true);
			if (rotate) {
				ImageProcessor sfp2=fp2.resize( (int)(fp2.getWidth()*ay), (int)(fp2.getHeight()*arat));
				sfp2.setMinAndMax(min, max);
				yz_image.setProcessor("ZY "+p.x, sfp2);
			}
			else {
				ImageProcessor sfp2=fp2.resize( (int)(fp2.getWidth()*arat), (int)(fp2.getHeight()*ay));
				sfp2.setMinAndMax(min, max);
				yz_image.setProcessor("YZ "+p.x, sfp2);
			}
		} else {
			fp2.setMinAndMax(min, max);
			if (rotate)
				yz_image.setProcessor("YZ "+p.x, fp2);
			else
				yz_image.setProcessor("ZY "+p.x, fp2);
		}
		
		calibrate();
		if (xz_image.getWindow()==null) {
			xz_image.show();
			yz_image.show();
		}
		 
	}
	
	void arrangeWindows(boolean sticky) {
		Point loc = imp.getWindow().getLocation();
		if ((xyImX!=loc.x)||(xyImY!=loc.y)) {
			xyImX =  imp.getWindow().getLocation().x;
			xyImY =  imp.getWindow().getLocation().y;
			ImageWindow win1 = xz_image.getWindow();
			if (win1!=null)
 				win1.setLocation(xyImX,xyImY +imp.getWindow().getHeight());
 			long start = System.currentTimeMillis();
 			ImageWindow win2 =null;
 			while (win2==null && (System.currentTimeMillis()-start)<=2500L)
				win2 = yz_image.getWindow();
			if (win2!=null)
 				win2.setLocation(xyImX+imp.getWindow().getWidth(),xyImY);
		}
	}
	
	/**
	 * @param is - used to get the dimensions of the new ImageProcessors
	 * @return
	 */
	boolean createProcessors(ImageStack is) {
		 //ImageStack is=imp.getStack();
		ImageProcessor ip=is.getProcessor(1);
		 int width= is.getWidth();
		 int height=is.getHeight();
		 int ds=is.getSize(); 
		 float arat=1.0f;//az/ax;
		 float brat=1.0f;//az/ay;
		// float arat=az/ax;
		// float brat=az/ay;
		 int za=(int)(ds*arat);
		 int zb=(int)(ds*brat);
		 //IJ.log("za: "+za +" zb: "+zb);
		  
		if (ip instanceof FloatProcessor) {
			fp1=new FloatProcessor(width,za);
			if (rotate)
				fp2=new FloatProcessor(height,zb);
			else
				fp2=new FloatProcessor(zb,height);
			return true;
		}
		
		if (ip instanceof ByteProcessor) {
			fp1=new ByteProcessor(width,za);
			if (rotate)
				fp2=new ByteProcessor(height,zb);
			else
				fp2=new ByteProcessor(zb,height);
			return true;
		}
		
		if (ip instanceof ShortProcessor) {
			fp1=new ShortProcessor(width,za);
			if (rotate)
				fp2=new ShortProcessor(height,zb);
			else
				fp2=new ShortProcessor(zb,height);
			return true;
		}
				
		if (ip instanceof ColorProcessor) {
			fp1=new ColorProcessor(width,za);
			if (rotate)
				fp2=new ColorProcessor(height,zb);
			else
				fp2=new ColorProcessor(zb,height);
			return true;
		}
		return false;
	}
	
	 void updateXZView(Point p, ImageStack is) {
		int width= is.getWidth();
		int size=is.getSize();
		ImageProcessor ip=is.getProcessor(1);
	
		int y=p.y;
		 try {
			 // XZ
				if (ip instanceof ShortProcessor) {
					short[] newpix=new short[width*size];
					 for (int i=0; i<size; i++) { 
						 Object pixels=is.getPixels(i+1);
						 if (flipXZ)
						     System.arraycopy(pixels, width*y, newpix, width*(size-i-1), width);
						 else
						     System.arraycopy(pixels, width*y, newpix, width*i, width);
					 }
					 fp1.setPixels(newpix);
					 return;
				}
								
				if (ip instanceof ByteProcessor) {
					byte[] newpix=new byte[width*size];
					 for (int i=0;i<size; i++) { 
						 Object pixels=is.getPixels(i+1);
						  if (flipXZ)
						     System.arraycopy(pixels, width*y, newpix, width*(size-i-1), width);
						  else
						     System.arraycopy(pixels, width*y, newpix, width*i, width);
					 }
					 fp1.setPixels(newpix);
					 return;
				}
				
			 	if (ip instanceof FloatProcessor) {
			 		 float[] newpix=new float[width*size];
					 for (int i=0; i<size; i++) { 
						 Object pixels=is.getPixels(i+1);
						  if (flipXZ)
						     System.arraycopy(pixels, width*y, newpix, width*(size-i-1), width);
						  else
						     System.arraycopy(pixels, width*y, newpix, width*i, width);
					 }
					 fp1.setPixels(newpix);
					 return;
				}
				
				if (ip instanceof ColorProcessor) {
					int[] newpix=new int[width*size];
					 for (int i=0;i<size; i++) { 
						 Object pixels=is.getPixels(i+1);
					  	  if (flipXZ)
						     System.arraycopy(pixels, width*y, newpix, width*(size-i-1), width);
						  else
						     System.arraycopy(pixels, width*y, newpix, width*i, width);
					 }
					 fp1.setPixels(newpix);
					 return;
				}
		} //end try
		catch (ArrayIndexOutOfBoundsException ex) {
			IJ.log("XZ: ArrayIndexOutOfBoundsException occured");
		}
			       		
	}
	
	void updateYZView(Point p, ImageStack is) {
		int width= is.getWidth();
		int height=is.getHeight();
		int ds=is.getSize();
		//IJ.log("image size: " +ds*height);
		ImageProcessor ip=is.getProcessor(1);

		int x=p.x;
		try {
			if (ip instanceof FloatProcessor) {
		 		 float[] newpix=new float[ds*height];
				 //IJ.log("ds " +ds); 
				 
				// for (int i=ds-1;i>=0; i--) { 
		 		 for (int i=0;i<ds; i++) { 
					 float[] pixels= (float[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int j=0;j<height;j++) {
					//	newpix[i*height + j] = pixels[x + j* width];
						 newpix[(ds-i-1)*height + j] = pixels[x + j* width];
		 			//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
				 fp2.setPixels(newpix);
			}
			
			if (ip instanceof ByteProcessor) {
			 
				byte[] newpix=new byte[ds*height];
				 
				// int c=0;
			
				// for (int i=ds-1;i>=0; i--){ 
				 for (int i=0;i<ds; i++) { 
					 byte[] pixels= (byte[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int j=0;j<height;j++) {
					//	newpix[i*height + j] = pixels[x + j* width];
						 newpix[(ds-i-1)*height + j] = pixels[x + j* width];
					//	c++;
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
			 
			}
			
			if (ip instanceof ShortProcessor) {
				short[] newpix=new short[ds*height];
				 
				 //int c=0;
				 //IJ.log("ds " +ds); 
				 //for (int i=ds-1;i>=0; i--) { 
				 for (int i=0;i<ds; i++) { 
					 short[] pixels= (short[]) is.getPixels(i+1);//toFloatPixels(pixels);
					 //IJ.log("i "+i);
					 for (int j=0;j<height;j++) {
						//newpix[i*height + j] = pixels[x + j* width];
						 newpix[(ds-i-1)*height + j] = pixels[x + j* width];
						//c++;
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
			 
			}
			
			if (ip instanceof ColorProcessor) {
				int[] newpix=new int[ds*height];
			
				// for (int i=ds-1;i>=0; i--) { 
				 for (int i=0;i<ds; i++) { 
					 int[] pixels= (int[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int j=0;j<height;j++) {
						//fp2.putPixelValue(j, ds-i-1, (double)pixels3[x + j* width]);
						//newpix[i*height + j] = pixels[x + j* width];
						 newpix[(ds-i-1)*height + j] = pixels[x + j* width];
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
					 
			}
		} //end try
		catch (ArrayIndexOutOfBoundsException ex) {
			IJ.log("YZ: ArrayIndexOutOfBoundsException occured");
		}

	}
	
	void updateZYView(Point p, ImageStack is) {
		int width= is.getWidth();
		int height=is.getHeight();
		int ds=is.getSize();
		ImageProcessor ip=is.getProcessor(1);

		int x=p.x;
		try {
			if (ip instanceof FloatProcessor) {
		 		 float[] newpix=new float[ds*height];
			 
				 for (int i=0;i<ds; i++) { 
					 float[] pixels= (float[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int y=0;y<height;y++) {
						newpix[i + y*ds] = pixels[x + y* width];
					
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
				 
			}
			
			if (ip instanceof ByteProcessor) {
			 
				byte[] newpix=new byte[ds*height];
				 
				 for (int i=0;i<ds; i++) { 
					 byte[] pixels= (byte[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int y=0;y<height;y++) {
						newpix[i + y*ds] = pixels[x + y* width];
					
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
			 
			}
			
			if (ip instanceof ShortProcessor) {
				short[] newpix=new short[ds*height];
				 
				 //IJ.log("ds " +ds); 
				 for (int i=0;i<ds; i++) { 
					 short[] pixels= (short[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int y=0;y<height;y++) {
						newpix[i + y*ds] = pixels[x + y* width];
					
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
		 			 
				 fp2.setPixels(newpix);
			 
			}
						
			if (ip instanceof ColorProcessor) {
				int[] newpix=new int[ds*height];
				 
				 for (int i=0;i<ds; i++) { 
					 int[] pixels= (int[]) is.getPixels(i+1);//toFloatPixels(pixels);
					// Log("i "+i);
					 for (int y=0;y<height;y++) {
						newpix[i + y*ds] = pixels[x + y* width];
				
						//Log("j "+j);
					}
					 
					//IJ.log("c" +c);	 
				 }
			 			 
				 fp2.setPixels(newpix);
				 
			}
		} //end try
		catch (ArrayIndexOutOfBoundsException ex) {
			IJ.log("ZY: ArrayIndexOutOfBoundsException occured");

		}

	}
	
 
	/** draws the crosses in the images */
	void drawCross(ImagePlus imp, Point p, GeneralPath path) {
		int width=imp.getWidth();
		int height=imp.getHeight();
		float x = p.x;
		float y = p.y;
		path.moveTo(0f, y);
		path.lineTo(width, y);
		path.moveTo(x, 0f);
		path.lineTo(x, height);	
	}
	 
	boolean showDialog(ImagePlus imp)   {
        if (imp==null) return true;
        GenericDialog gd=new GenericDialog("Parameters");
        gd.addMessage("This plugin projects orthogonal views\n");
        gd.addNumericField("aspect ratio X:", ax, 3);
        gd.addNumericField("aspect ratio Y:", ay, 3);
        gd.addNumericField("aspect ratio Z:", az, 3);
        gd.addCheckbox("rotate YZ", rotate);
        gd.addCheckbox("sticky panels", sticky);
        gd.showDialog();
        
        ax=(float)gd.getNextNumber();
        ay=(float)gd.getNextNumber();
        az=(float)gd.getNextNumber();
        rotate=gd.getNextBoolean();
        sticky=gd.getNextBoolean();
        if (sticky) rotate = false;
        if (gd.wasCanceled())
            return false;
        return true;
	 }
	     
    boolean processibleRoi(ImagePlus imp) {
    	// try {
    	   	roi = imp.getRoi();
    	       boolean ret=(roi!=null && ( //roi.getType()==Roi.LINE || 
    	       						 roi.getType()==Roi.POINT
    	       						 )
    	       		   );
    	       //Log("roi ret "+ ret);
    	       return ret;
    	 //} catch (NullPointerException ex) { 
    	//	 return false;
    	// }
    }
     
     /**
     * 
     */
    void showAbout() {
         IJ.showMessage("About StackSlicer...",
	         "This plugin projects dynamically orthogonal XZ and YZ views of a stack.\n" + 
	         "The user should provide a point selection in the active image window.\n" +
	         "The output images are calibrated, which allows measurements to be performed more easily.\n" +
	         "Optionally the YZ image can be rotated at 90 deg."
         );
     }
     
	void dispose(){
		updater.quit();
		updater = null;
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		canvas.removeKeyListener(this);
		canvas.setDisplayList(null);
       	ImageWindow win1 = xz_image.getWindow();
        	if (win1!=null) win1.getCanvas().setDisplayList(null);
       	ImageWindow win2 = yz_image.getWindow();
        	if (win2!=null) win2.getCanvas().setDisplayList(null);
		ImagePlus.removeImageListener(this);
		Executer.removeCommandListener(this);
		win.removeWindowListener(this);
		win.removeFocusListener(this);
		win.setResizable(true);
		instance = null;
	}
 	
    /* Saves the current settings of the plugin for further use
     *
    * @param prefs - the current preferences
    */
   static void savePreferences(Properties prefs) {
           prefs.put(AX, Double.toString(ax));
           prefs.put(AY, Double.toString(ay));
           prefs.put(AZ, Double.toString(az));
           prefs.put(YROT, Boolean.toString(rotate));
           prefs.put(SPANELS, Boolean.toString(sticky));
   }
        
    //@Override
	public void mouseClicked(MouseEvent e) {
	}

	//@Override
	public void mouseEntered(MouseEvent e) {
	}

	//@Override
	public void mouseExited(MouseEvent e) {
	}

	//@Override
	public void mousePressed(MouseEvent e) {
		update();
	}

	//@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	/**
	 * Refresh the output windows. This is done by sending a signal 
	 * to the Updater() thread. 
	 */
	void update() {
		if (updater!=null)
			updater.doUpdate();
	}
	
	private void exec() {
		roi=imp.getRoi();
		int x=0, y=0;
		int width=imp.getWidth();
		int height=imp.getHeight();
		ImageStack is=imp.getStack();
		double arat=az/ax;
		double brat=az/ay;
		if (roi !=null && roi.getType()==Roi.POINT) {
			Rectangle r=roi.getBounds();
			x=r.x;
			y=r.y;
			if (y>=height) y=height-1;
			if (x>=width) x=width-1;
			if (x<0) x=0;
			if (y<0) y=0;
			Point p=new Point (x,y);
			updateViews(p, is);
			if (canvas==null)
				return;
			else {
				GeneralPath path = new GeneralPath();
				drawCross(imp, p, path);
				canvas.setDisplayList(path, color, new BasicStroke(1));
			}
			updateCrosses(x, y, arat, brat);
		 }
		if (syncZoom) updateMagnification(x, y);
		arrangeWindows(sticky);
	}

	private void updateCrosses(int x, int y, double arat, double brat) {
		Point p;
		int z=imp.getNSlices();
		int zlice=imp.getCurrentSlice()-1;
		int zcoord=(int)(arat*zlice);
		if (flipXZ) zcoord=(int)(arat*(z-zlice));
		p=new Point (x, zcoord);
		ImageCanvas xz_canvas=xz_image.getCanvas();
		if (xz_canvas==null) 
			return;
		else {
			GeneralPath path = new GeneralPath();
			drawCross(xz_image, p, path);
			xz_canvas.setDisplayList(path, color, new BasicStroke(1));
		}
		zcoord=(int)(brat*(z-zlice));
		if (rotate) 
			p=new Point (y, zcoord);
		else {
			zcoord=(int)(arat*zlice);
			p=new Point (zcoord, y);
		}
		ImageCanvas yz_canvas=yz_image.getCanvas();
		if (yz_canvas==null) 
			return;
		else {
			GeneralPath path = new GeneralPath();
			drawCross(yz_image, p, path);
			yz_canvas.setDisplayList(path, color, new BasicStroke(1));
		}
	}

	//@Override
	public void mouseDragged(MouseEvent e) {
		e.consume();
		update();
	}

	//@Override
	public void mouseMoved(MouseEvent e) {
	}

	//@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		switch (key) {
			case KeyEvent.VK_LEFT: ;
			case KeyEvent.VK_RIGHT: ;
			case KeyEvent.VK_UP: ;
			case KeyEvent.VK_DOWN: {
				update();
			}
		}
		e.consume();
	}

	//@Override
	public void keyReleased(KeyEvent e) {
	}

	//@Override
	public void keyTyped(KeyEvent e) {
	}

	//@Override
	public void actionPerformed(ActionEvent ev) {
	}

	public void imageClosed(ImagePlus imp) {
		dispose();
	}

	public void imageOpened(ImagePlus imp) {
	}

	public void imageUpdated(ImagePlus imp) {
		if (imp==this.imp) {
			ImageProcessor ip = imp.getProcessor();
			min = ip.getMin();
			max = ip.getMax();
			update();
		}
	}

	public String commandExecuting(String command) {
		if (command.equals("In")||command.equals("Out")) {
			ImagePlus cimp = WindowManager.getCurrentImage();
			if (cimp==null) return command;
			if (cimp==imp) {
				IJ.runPlugIn("ij.plugin.Zoom", command.toLowerCase());
				xyImX=0; xyImY=0;
				update();
				return null;
			} else if (cimp==xz_image || cimp==yz_image) {
				syncZoom = false;
				return command;
			} else
				return command;
		} else if (command.equals("Flip Vertically")&& xz_image!=null) {
			if (xz_image==WindowManager.getCurrentImage()) {
				flipXZ = !flipXZ;
				update();
				return null;
			} else
				return command;
		} else
			return command;
	}

	//@Override
	public void windowActivated(WindowEvent e) {
		 arrangeWindows(sticky);
	}

	//@Override
	public void windowClosed(WindowEvent e) {
	}

	//@Override
	public void windowClosing(WindowEvent e) {
		dispose();		
	}

	//@Override
	public void windowDeactivated(WindowEvent e) {
	}

	//@Override
	public void windowDeiconified(WindowEvent e) {
		 arrangeWindows(sticky);
	}

	//@Override
	public void windowIconified(WindowEvent e) {
	}

	//@Override
	public void windowOpened(WindowEvent e) {
	}

	//@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		update();
	}
		
	//@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		e.consume();
		update();
	}

	//@Override
	public void focusGained(FocusEvent e) {
		arrangeWindows(sticky);
	}

	//@Override
	public void focusLost(FocusEvent e) {
		arrangeWindows(sticky);
	}
	
	/**
	 * This is a helper class for Othogonal_Views that delegates the
	 * repainting of the destination windows to another thread.
	 * 
	 * @author Albert Cardona
	 */
	private class Updater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		Updater() {
			super("Othogonal Views Updater");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted()) return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call update from this thread
					if (r>0)
						exec();
					synchronized (this) {
						if (r==request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) { }
			}
		}
		
	}  // Updater class

}
