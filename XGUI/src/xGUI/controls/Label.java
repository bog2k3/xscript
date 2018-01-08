package xGUI.controls;

import java.awt.Graphics2D;

import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import xGUI.Location;
import xGUI.VisualComponent;
import xGUI.XColors;

public class Label extends VisualComponent 
{
	protected Boolean autoSize = false;
	public Boolean getAutoSize() { return autoSize; }
	public void setAutoSize(Boolean sz) {
		autoSize = sz;
		if (autoSize) {
			handleTextChanged();
			repaint();
		}
	}
	
	public static final Integer BORDER_NONE = 0;
	public static final Integer BORDER_SIMPLE = 1;
	public static final Integer BORDER_3D_IN = 2;
	public static final Integer BORDER_3D_OUT = 3;
	
	protected Integer borderStyle = BORDER_NONE;
	public Integer getBorderStyle() { return borderStyle; }
	public void setBorderStyle(Integer style) {
		borderStyle = style;
		repaint();
	}
	
	protected Color clBorder = Color.black;
	
	public Label(String text, Integer x, Integer y, Integer w, Integer h) 
	{
		this.text = text;
		init_loc = new Location(x,y,w,h);
		m_visible = true;
	}
	
	@Override
	protected void handleTextChanged()
	{
		if (m_recText == null || !autoSize)
			return;
		
		int w = (int)m_recText.getWidth() + 6;
		int h = (int)m_recText.getHeight() + 8;
		setLocation(m_loc.x, m_loc.y, w, h);
	}

	@Override
	protected void paint(Graphics2D gfx) 
	{
		gfx.setColor(clBackground.awtClr);
		gfx.fillRect(0, 0, m_loc.w, m_loc.h);
		gfx.setColor(XColors.text.awtClr);
		
		m_textLayout.draw(gfx, 2+(borderStyle!=0?2:0), (float)(m_loc.h / 2.0 - m_recText.getCenterY()));
		
		switch (borderStyle) {
		case 1: /*simple*/
			gfx.setColor(clBorder.awtClr);
			gfx.drawRect(0, 0, m_loc.w-1, m_loc.h-1);
			break;
		case 2: /* 3D in */
			getGUI().draw3DFrame(gfx, m_loc.untranslate(), null, false);
			break;
		case 3: /* 3D out */
			getGUI().draw3DFrame(gfx, m_loc.untranslate(), null, true);
			break;
		case 0: /*none*/
		default:
		}
	}

	@Override
	protected void init() 
	{
		setLocation(init_loc);
		setText(text);
	}

	@Override
	protected void updateFromStyle() {
		// we lack style
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
	
	@Override
	public void destroy() {	}
	
	public void setBorderColor(Color color) {
		clBorder = color;
	}

}
