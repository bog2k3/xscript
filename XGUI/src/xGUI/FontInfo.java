package xGUI;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class FontInfo 
{
	public final Font font;
	public final FontMetrics fontMetrics;
	public final int ascent;
	
	public FontInfo(Font fnt, Graphics2D gfx) {
		font = fnt;
		if (gfx != null) {
			fontMetrics = gfx.getFontMetrics(); 
			ascent = fontMetrics.getAscent();
		} else {
			fontMetrics = null;
			ascent = 0;
		}
	}
	
}
