package ij.measure;
import ij.plugin.filter.Analyzer;
import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.text.*;
import java.awt.*;
import java.awt.event.*;


/** This class implements the Apply Macro command in tables.
* @author Michael Schmid
*/
public class ResultsTableMacros implements Runnable, DialogListener, ActionListener, KeyListener {
	private static String NAME = "TableMacro.ijm";
	private String defaultMacro = "Sin=sin(rowIndex*0.1);\nCos=cos(rowIndex*0.1);\nSqr=Sin*Sin+Cos*Cos;";
	private GenericDialog gd;
	private ResultsTable rt, rt2;
	private Button runButton, undoButton, openButton, saveButton;
	private String title;
	private int runCount;
	private TextArea ta;
	
	public ResultsTableMacros(ResultsTable rt) {
		this.rt = rt;
		title = rt!=null?rt.getTitle():null;
		Thread thread = new Thread(this, "ResultTableMacros");
		thread.start();
	}
	
	private void showDialog() {
		if (rt==null)
			rt = Analyzer.getResultsTable();
		if (rt==null || rt.size()==0) {
			IJ.error("Results Table required");
			return;
		}
		ResultsTable rtBackup = (ResultsTable)rt.clone();
		String[] temp = rt.getHeadingsAsVariableNames();
		String[] variableNames = new String[temp.length+1];
		variableNames[0] = "";
		for (int i=1; i<variableNames.length; i++)
			variableNames[i] = temp[i-1];
		String dialogTitle = "Apply Macro to "+(title!=null?"\""+title+"\"":"Table");
		Frame parent = title!=null?WindowManager.getFrame(title):null;
		if (parent!=null)
			gd = new GenericDialog(dialogTitle, parent);
		else
			gd = new GenericDialog(dialogTitle);
		gd.addTextAreas(getMacro(), null, 12, 48);
		ta = gd.getTextArea1();
		ta.addKeyListener(this);
		gd.addChoice("Insert:", variableNames, variableNames[0]);
		
		Panel panel = new Panel();
		if (IJ.isMacOSX())
			panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		runButton = new Button("Run");
		runButton.addActionListener(this);
		panel.add(runButton);
		undoButton = new Button("Undo");
		undoButton.addActionListener(this);
		panel.add(undoButton);		
		openButton = new Button("Open");
		openButton.addActionListener(this);
		panel.add(openButton);
		saveButton = new Button("Save");
		saveButton.addActionListener(this);
		panel.add(saveButton);
		gd.addToSameRow();
		gd.addPanel(panel);

		gd.addHelp("<html><body><h1>Macro Equations for Results Tables</h1><ul>"+
				"<li>The macro, or a selection, is applied to each row of the table."+
				"<li>A new variable starting with an Uppercase character creates a new column."+
				"<li>A new variable starting with a lowercase character is temporary."+
				"<li>The variable <b>rowIndex</b> is pre-defined.\n"+
				"<li>String operations are supported for the 'Label' column only (if enabled<br>"+
				"with Analyze&gt;Set Measurements&gt;Display Label)."+				
				"<li>Click \"<b>Run</b>\" to apply the macro code to the table."+
				"<li>Select a line and press "+(IJ.isMacOSX()?"cmd":"ctrl") + "-r to apply a line of macro code."+
				"<li>Click \"<b>Undo</b>\", or press "+(IJ.isMacOSX()?"cmd":"ctrl")+"-z, to undo the table changes."+
				"<li>The code is saved at <b>macros/TableMacro.ijm</b> when you click \"<b>OK</b>\"."+
				"</ul></body></html>");

		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) {  // dialog cancelled?
			rt = rtBackup;
			updateDisplay();
			return;
		}
		if (runCount==0)
			applyMacro();
		IJ.saveString(ta.getText(), IJ.getDir("macros")+NAME);
	 }

	private void applyMacro() {
		int start = ta.getSelectionStart();
		int end = ta.getSelectionEnd();
		String macro  = start==end?ta.getText():ta.getSelectedText();
		rt.applyMacro(macro);
		updateDisplay();
		runCount++;
	}
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		String variableName = gd.getNextChoice();
		if (e!=null && (e.getSource() instanceof Choice) && variableName.length()>0) {
			int pos = ta.getCaretPosition();
			ta.insert(variableName, pos);
			//ta.setCaretPosition(pos+variableName.length());
		}
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==runButton) {
			rt2 = (ResultsTable)rt.clone();
			applyMacro();
		} else if (source==undoButton) {
			if (rt2!=null) {
				rt = rt2;
				updateDisplay();
				rt2 = null;
			}
		} else if (source==openButton) {
			String macro = IJ.openAsString(null);
			if (macro==null)
				return;
			if (macro.startsWith("Error: ")) {
				IJ.error(macro);
				return;
			} else
				ta.setText(macro);
		} else if (source==saveButton) {
			ta.selectAll();
			String macro = ta.getText();
			ta.select(0, 0);
			IJ.saveString(macro, null);
		}

	}
	 
	public void keyPressed(KeyEvent e) { 
		int flags = e.getModifiers();
		boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
		boolean meta = (flags & KeyEvent.META_MASK) != 0;
		int keyCode = e.getKeyCode();
		if (keyCode==KeyEvent.VK_R && (control||meta)) {
			rt2 = (ResultsTable)rt.clone();
			applyMacro();
		}
		if (keyCode==KeyEvent.VK_Z && (control||meta) && rt2!=null) {
			 rt = rt2;
			 updateDisplay();
			 rt2 = null;
		}
	} 
	
	public void keyReleased(KeyEvent e) {
	}
	
	public void keyTyped(KeyEvent e) {
	}
	 
	private String getMacro() {
		String macro = IJ.openAsString(IJ.getDir("macros")+NAME);
		if (macro==null || macro.startsWith("Error:"))
			return defaultMacro;
		else {
			macro =  macro.replaceAll("rowNumber", "rowIndex");
			return macro;
		}
	}
	 
	public void run() {
		showDialog();
 	}
 	
 	private void updateDisplay() {
		if (title!=null) rt.show(title);
 	}
	 
}
