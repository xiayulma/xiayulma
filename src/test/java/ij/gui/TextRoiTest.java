//
// TextRoiTest.java
//

/*
ImageJ software for multidimensional image processing and analysis.

Copyright (c) 2010, ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the names of the ImageJDev.org developers nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package ij.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Font;
import java.awt.Rectangle;

import org.junit.Test;

/**
 * Unit tests for {@link TextRoi}.
 *
 * @author Barry DeZonia
 */
public class TextRoiTest {

	TextRoi t;
	
	@Test
	public void testTextRoiIntIntString() {
		
		t = new TextRoi(0,0,"");
		assertEquals(new Rectangle(0,0,1,1), t.getBounds());
		assertEquals("", t.getText());

		t = new TextRoi(1,3,"Super-sandwich-man's high, \"round\" shoes!");
		assertEquals(new Rectangle(1,3,1,1), t.getBounds());
		assertEquals("Super-sandwich-man's high, \"round\" shoes!\n", t.getText());
	}

	@Test
	public void testTextRoiIntIntStringFont() {
		Font font = new Font("SansBatootie",4,3);
		
		t = new TextRoi(0,0,"",font);
		assertEquals(new Rectangle(0,0,1,1), t.getBounds());
		assertEquals("", t.getText());

		t = new TextRoi(1,3,"Super-sandwich-man's high, \"round\" shoes!",font);
		assertEquals(new Rectangle(1,3,1,1), t.getBounds());
		assertEquals("Super-sandwich-man's high, \"round\" shoes!\n", t.getText());
	}

	@Test
	public void testTextRoiIntIntImagePlus() {
		// note - can't test - needs an active imagecanvas
	}

	@Test
	public void testClone() {
		t = new TextRoi(1,3,"Super-sandwich-man's high, \"round\" shoes!");
		TextRoi roi = (TextRoi) t.clone();
		assertEquals(t,roi);
	}

	@Test
	public void testDraw() {
		// note - can't test - needs a graphics context
	}

	/* Removed 7-20-10
	 * because it gives different results when running headless under hudson
	@Test
	public void testDrawPixelsImageProcessor() {
		t = new TextRoi(1,3,"Ab4");
		int size = 30;
		ImageProcessor proc = new ByteProcessor(size,size,new byte[size*size],null);
		proc.setColor(Color.red);
		t.drawPixels(proc);
		int[] expectedNonZeroes = new int[]{194,195,196,216,217,218,224,225,226,245,246,247,248,254,255,256,269,275,276,277,278,279,
											284,285,286,298,299,304,305,306,307,308,309,314,315,316,328,329,334,335,336,337,338,339,
											344,345,346,347,348,349,350,351,357,358,359,364,365,366,367,368,369,370,374,375,376,377,
											378,379,380,381,382,386,387,388,389,393,394,395,398,399,400,404,405,406,407,410,411,412,
											413,416,417,418,423,424,425,428,429,430,434,435,436,441,442,443,445,446,447,453,454,455,
											456,457,458,459,460,461,464,465,466,471,472,473,474,475,476,477,478,479,482,483,484,485,
											486,487,488,489,490,491,494,495,496,501,502,503,504,505,506,507,508,509,512,513,514,515,
											516,517,518,519,520,521,522,524,525,526,531,532,533,534,535,536,537,538,539,541,542,543,
											550,551,552,554,555,556,560,561,562,571,572,573,580,581,582,584,585,586,587,588,589,590,
											591,592,601,602,603,610,611,612,613,614,615,616,617,618,619,620,621,644,645,646,648,649,
											650};
		//RoiHelpers.printNonzeroIndices(proc);
		RoiHelpers.validateNonzeroResult(proc, expectedNonZeroes);
	}
	*/

	@Test
	public void testIsDrawingTool() {
		t = new TextRoi(1,3,"Ab4");
		assertTrue(t.isDrawingTool());  // true of all TextRois
	}

	@Test
	public void testAddChar() {
		
		// Wayne has said this should not be tested. He has updated documentation to tell people not to use it.
		
		/*
		// EMPTY string tests
		
		// backspace
		//validateAddChars("\b","");  //TODO - crash
		// newline
		validateAddChars("\n","");
		// try to add all other nonprinting chars
		for (int i = 0; i < 32; i++)
			if ( ((char)i != '\n') && ((char)i != '\b') )
				validateAddChars(""+(char)i,"");
		
		// ONE CHAR string tests
		
		// backspace
		//validateAddChars("X\b","");  // TODO - return val is "null\n" rather than empty string
		// newline
		validateAddChars("X\n","X\n");  // TODO: this is broken as well: "nullX\n\n" I think
		// try to add all other nonprinting chars
		for (int i = 0; i < 32; i++)
			if ( ((char)i != '\n') && ((char)i != '\b') )
				validateAddChars("X"+(char)i,"X");
		
		// Some general cases
		// TODO
		 */
	}

	@Test
	public void testSetFont1() {
		
		// test setFont(3 params)
		
		// save static values to avoid side effects later
		String savedFontName = TextRoi.getFont();
		int savedSize = TextRoi.getSize();
		int savedStyle = TextRoi.getStyle();
		
		assertEquals("SansSerif", savedFontName);
		assertEquals(18, savedSize);
		assertEquals(Font.PLAIN, savedStyle);

		TextRoi.setFont("Parp", 93, Font.BOLD);
		
		assertEquals("Parp",TextRoi.getFont());
		assertEquals(93,TextRoi.getSize());
		assertEquals(Font.BOLD,TextRoi.getStyle());
		
		// restore so we don't mess up other tests
		TextRoi.setFont(savedFontName,savedSize,savedStyle);
	}

	@Test
	public void testSetFont2() {

		// test setFont(4 params)
		
		// save static values to avoid side effects later
		String savedFontName = TextRoi.getFont();
		int savedSize = TextRoi.getSize();
		int savedStyle = TextRoi.getStyle();
		boolean saveAnti = TextRoi.isAntialiased();
		
		assertEquals("SansSerif", savedFontName);
		assertEquals(18, savedSize);
		assertEquals(Font.PLAIN, savedStyle);
		assertEquals(true, saveAnti);

		TextRoi.setFont("Parp", 93, Font.BOLD, !saveAnti);
		
		assertEquals("Parp",TextRoi.getFont());
		assertEquals(93,TextRoi.getSize());
		assertEquals(Font.BOLD,TextRoi.getStyle());
		assertEquals(!saveAnti, TextRoi.isAntialiased());
		
		// restore so we don't mess up other tests
		TextRoi.setFont(savedFontName,savedSize,savedStyle,saveAnti);
	}
	
	@Test
	public void testIsAntialiased() {
		// save static values to avoid side effects later
		String savedFontName = TextRoi.getFont();
		int savedSize = TextRoi.getSize();
		int savedStyle = TextRoi.getStyle();
		
		TextRoi.setFont("Parp", 93, Font.BOLD, true);
		assertTrue(TextRoi.isAntialiased());
		
		TextRoi.setFont("Parp", 93, Font.BOLD, false);
		assertFalse(TextRoi.isAntialiased());

		// restore so we don't mess up other tests
		TextRoi.setFont(savedFontName,savedSize,savedStyle);
	}

	@Test
	public void testSetAndGetCurrentFont() {
		Font font1 = new Font("Brietastic",14,5);
		Font font2 = new Font("Cheddarburg",14,5);
		t = new TextRoi(10000,0,"",font1);
		assertEquals(font1,t.getCurrentFont());
		t.setCurrentFont(font2);
		assertEquals(font2,t.getCurrentFont());
	}

    @Test
    public void testGetMacroCode() {
        // save static values to avoid side effects later
        String savedFontName = TextRoi.getFont();
        int savedSize = TextRoi.getSize();
        int savedStyle = TextRoi.getStyle();

        ImageProcessor proc = new ShortProcessor(104,77,new short[104*77],null);
        Font font = new Font("Goudaland",44,15);
        t = new TextRoi(0,1500,"Glaphound",font);
        TextRoi.setFont("Hork",64,Font.BOLD+Font.ITALIC,false);
        
        // this next test broken apart since font metrics change for diff platforms
        String tmp = t.getMacroCode(proc);
        assertTrue(tmp.indexOf("setFont(\"Hork\", 64, \"plain\");\nmakeText(\"Glaphound\", 0, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Glaphound\", 0, ") == 59);
        assertTrue(tmp.indexOf(");\n",60)==95);

        // this next test broken apart since font metrics change for diff platforms
        tmp = t.getMacroCode(proc);
        assertTrue(tmp.indexOf("makeText(\"Glaphound\", 0, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Glaphound\", 0, ") == 29);
        assertTrue(tmp.indexOf(");\n",30)==65);
        
        TextRoi.setFont("Glook",32,Font.PLAIN,true);

        // this next test broken apart since font metrics change for diff platforms
        tmp = t.getMacroCode(proc);
        assertTrue(tmp.indexOf("setFont(\"Glook\", 32, \" antialiased\");\nmakeText(\"Glaphound\", 0, ")==0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Glaphound\", 0, ")==67);
        assertTrue(tmp.indexOf(");\n",68) == 103);
        
        // this next test broken apart since font metrics change for diff platforms
        tmp = t.getMacroCode(proc);
        assertTrue(tmp.indexOf("makeText(\"Glaphound\", 0, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Glaphound\", 0, ") == 29);
        assertTrue(tmp.indexOf(");\n",30)==65);

        // restore so we don't mess up other tests
        TextRoi.setFont(savedFontName,savedSize,savedStyle);
    }
    
	@Test
	public void testGetText() {
		t = new TextRoi(10000,0,"");
		assertEquals("",t.getText());

		t = new TextRoi(10000,0,"Oooch");
		assertEquals("Oooch\n",t.getText());

		t = new TextRoi(10000,0,"Weekend\nweather\n\n");
		assertEquals("Weekend\nweather\n\n",t.getText());
	}

    @Test
    public void testRecordSetFont() {
        t = new TextRoi(-1,19,"Klezmer");
        
        String tmp = t.getMacroCode(new ByteProcessor(2,2,new byte[2*2],null));
        assertTrue(tmp.indexOf("setFont(\"SansSerif\", 18, \" antialiased\");\nmakeText(\"Klezmer\", -1, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Klezmer\", -1, ") == 68);
        assertTrue(tmp.indexOf(");\n",69) == 101);
        
        tmp = t.getMacroCode(new ByteProcessor(2,2,new byte[2*2],null));
        assertTrue(tmp.indexOf("makeText(\"Klezmer\", -1, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Klezmer\", -1, ") == 26);
        assertTrue(tmp.indexOf(");\n",27) == 59);
        
        TextRoi.recordSetFont();
        
        tmp = t.getMacroCode(new ByteProcessor(2,2,new byte[2*2],null));
        assertTrue(tmp.indexOf("setFont(\"SansSerif\", 18, \" antialiased\");\nmakeText(\"Klezmer\", -1, ") == 0);
        assertTrue(tmp.indexOf(");\n//drawString(\"Klezmer\", -1, ") == 68);
        assertTrue(tmp.indexOf(");\n",69) == 101);
    }
}
