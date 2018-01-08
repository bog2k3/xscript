package xGUI;

import java.awt.Font;

import wrappers.Color;
import wrappers.XMLNode;

public class XMLFontInfo 
{
	public final Font font;
	public final Color color;
	
	public XMLFontInfo(XMLNode eFont, Font defFont, Color defTextColor)
	{
		if (eFont == null) {
			font = defFont;
			color = defTextColor;
			return;
		}
		
		color = eFont.getColorAttr("color", defTextColor);
		
		String family = eFont.getAttribute("family", defFont.getFamily());
		int size = eFont.getIntAttr("size", defFont.getSize());
		boolean bold = eFont.getBooleanAttr("bold", (defFont.getStyle() & Font.BOLD) != 0);
		boolean italic = eFont.getBooleanAttr("italic", (defFont.getStyle() & Font.ITALIC) != 0);

		int fontStyle = 0;
		if (bold) fontStyle |= Font.BOLD;
		if (italic) fontStyle |= Font.ITALIC;
		if (fontStyle != defFont.getStyle() 
			|| size != defFont.getSize() 
			|| !family.equals(defFont.getFamily()))
		{
			font = new Font(family, fontStyle, size);
		} else
			font = defFont;
	}
}
