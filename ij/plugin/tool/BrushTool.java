package ij.plugin.tool;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.Colors;
import java.awt.*;
import java.awt.event.*;

	public class BrushTool extends PlugInTool implements Runnable {
	private static String BRUSH_WIDTH_KEY = "brush.width";
	private static String PENCIL_WIDTH_KEY = "pencil.width";
	private static String CIRCLE_NAME = "brush-tool-overlay";
	private int width = (int)Prefs.get(BRUSH_WIDTH_KEY, 5);
	private Thread thread;
	private boolean dialogShowing;
	private ImageProcessor ip;
	private int x2, x3;
	private boolean isPencil;
	private Overlay overlay;

	public void run(String arg) {
		isPencil = "pencil".equals(arg);
		if (isPencil)
			width = (int)Prefs.get(PENCIL_WIDTH_KEY, 1);
		Toolbar.addPlugInTool(this);
	}

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		ip = imp.getProcessor();
		ip.snapshot();
		Undo.setup(Undo.FILTER, imp);
		ip.setLineWidth(width);
		if (e.isAltDown())
			ip.setColor(Toolbar.getBackgroundColor());
		else
			ip.setColor(Toolbar.getForegroundColor());
		ip.moveTo(x, y);
		x2 = -1;
		x3 = x;
		if (overlay!=null && overlay.size()>0 && CIRCLE_NAME.equals(overlay.get(overlay.size()-1).getName())) {
			overlay.remove(overlay.size()-1);
			imp.setOverlay(overlay);
		} else if (overlay!=null)
			imp.setOverlay(null);
		overlay = null;
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		if (e.isShiftDown()) {
			if (x!=x2) {
				x2 = x;
				width = x-x3;
				if (width<1) width=1;
				Roi circle = new OvalRoi(x-width/2, y-width/2, width, width);
				circle.setName(CIRCLE_NAME);
				circle.setStrokeColor(Color.red);
				overlay = imp.getOverlay();
				if (overlay==null)
					overlay = new Overlay();
				else if (overlay.size()>0 && CIRCLE_NAME.equals(overlay.get(overlay.size()-1).getName()))
					overlay.remove(overlay.size()-1);
				overlay.add(circle);
				imp.setOverlay(overlay);
			}
			IJ.showStatus((isPencil?"Pencil":"Brush")+" size: "+ width);
		} else {
			ip.lineTo(x, y);
			imp.updateAndDraw();
		}
	}

	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		if (e.isShiftDown()) {
			if (isPencil)
				Prefs.set(PENCIL_WIDTH_KEY, width);
			else
				Prefs.set(BRUSH_WIDTH_KEY, width);
		}
	}

	public void showOptionsDialog() {
		thread = new Thread(this, "Brush Options");
		thread.setPriority(Thread.NORM_PRIORITY);
		thread.start();
	}

	public String getToolName() {
		if (isPencil)
			return "Pencil Tool";
		else
			return "Paintbrush Tool";
	}

	public String getToolIcon() {
		if (isPencil)
			return "C037L494fL4990L90b0Lc1c3L82a4Lb58bL7c4fDb4L5a5dL6b6cD7b";
		else
			return "C037La077Ld098L6859L4a2fL2f4fL3f99L5e9bL9b98L6888L5e8dL888c";
	}

	public void run() {
		new Options();
	}

	class Options implements DialogListener {

		Options() {
			if (dialogShowing)
				return;
			dialogShowing = true;
			showDialog();
		}

		public void showDialog() {
			Color color = Toolbar.getForegroundColor();
			String colorName = Colors.getColorName(color, "red");
			String name = isPencil?"Pencil":"Brush";
			GenericDialog gd = new NonBlockingGenericDialog(name+" Options");
			gd.addSlider(name+" width (pixels):", 1, 50, width);
			gd.addChoice("Color:", Colors.colors, colorName);
			gd.setInsets(10, 0, 0);
			gd.addMessage("You can also set the width by shift-dragging and\n"
									+"the color using the Color Picker (shift-k). Press\n"
									+"the alt key to draw in the background color.");
			gd.hideCancelButton();
			gd.addHelp("");
			gd.setHelpLabel("Undo");
			gd.setOKLabel("Close");
			gd.addDialogListener(this);
			gd.showDialog();
			dialogShowing = false;
		}

		public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
			if (e!=null && e.toString().contains("Undo")) {
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp!=null) IJ.run("Undo");
				return true;
			}
			width = (int)gd.getNextNumber();
			String colorName = gd.getNextChoice();
			Color color = Colors.getColor(colorName, Color.white);
			Toolbar.setForegroundColor(color);
			if (isPencil)
				Prefs.set(PENCIL_WIDTH_KEY, width);
			else
				Prefs.set(BRUSH_WIDTH_KEY, width);
			return true;
		}
	}

}
