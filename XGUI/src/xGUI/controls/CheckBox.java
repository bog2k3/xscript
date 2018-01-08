package xGUI.controls;

import java.awt.Font;
import java.awt.Graphics2D;

import kernel.MediaManager.WebImage;

import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import wrappers.XMLNode;
import xGUI.IStyleLoader;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.XColors;
import xGUI.XGUI;
import xGUI.XMLFontInfo;


public class CheckBox extends Button
{
	/** this is the identifier of the style node, in the theme XML */
	public static final String styleID = "checkBox";
	
	public static class CheckboxStyle extends IStyleLoader
	{
		/** default metrics */
		private final int DEFAULT_MIN_WIDTH = 20;
		private final int DEFAULT_MIN_HEIGHT = 15;
		private final int DEFAULT_TEXT_OFFSET_X = 5;
		private final int DEFAULT_TICK_OFFSET_X = 3;
		private final int DEFAULT_TICK_OFFSET_Y = 3;
		private final Color DEFAULT_TEXT_COLOR = XColors.text;
		
		/** metrics (set by theme) */
		protected int MIN_WIDTH = DEFAULT_MIN_WIDTH;
		protected int MIN_HEIGHT = DEFAULT_MIN_HEIGHT;
		protected int TEXT_OFFSET_X = DEFAULT_TEXT_OFFSET_X;
		protected int TICK_OFFSET_X = DEFAULT_TICK_OFFSET_X;
		protected int TICK_OFFSET_Y = DEFAULT_TICK_OFFSET_Y;
		protected Color TEXT_COLOR = DEFAULT_TEXT_COLOR;
		protected Font FONT = null;
		protected SkinState skinStateNormal = null;
		protected SkinState skinStateHover = null;
		protected SkinState skinStatePressed = null;
		protected SkinState skinStateDisabled = null;
		
		private XGUI xgui;
		
		public CheckboxStyle(XGUI owner)
		{
			this.xgui = owner;
		}
		
		@Override
		public void loadStyle(XMLNode eRoot, String themeRootURL) 
		{
			XMLNode eLayout = eRoot.getSubnode("layout");
			if (eLayout != null) {
				MIN_WIDTH = eLayout.getIntAttr( "minWidth", DEFAULT_MIN_WIDTH);
				MIN_HEIGHT = eLayout.getIntAttr( "minHeight", DEFAULT_MIN_HEIGHT);
				XMLNode eTick = eLayout.getSubnode("tick");
				if (eTick != null) {
					TICK_OFFSET_X = eTick.getIntAttr("offsetX", DEFAULT_TICK_OFFSET_X);
					TICK_OFFSET_Y = eTick.getIntAttr("offsetY", DEFAULT_TICK_OFFSET_Y);
				}
				XMLNode eText = eLayout.getSubnode("text");
				if (eText != null) {
					TEXT_OFFSET_X = eText.getIntAttr("offsetX", 0);
				}
			}
			
			XMLNode eFont = eRoot.getSubnode( "font"); 
			XMLFontInfo xf = new XMLFontInfo(eFont, xgui.getDefaultFont(), xgui.getDefaultTextColor());
			FONT = xf.font;
			TEXT_COLOR = xf.color;
			
			XMLNode eSkin = eRoot.getSubnode("normal");
			if (eSkin != null)
				skinStateNormal = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("hover");
			if (eSkin != null)
				skinStateHover = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("pressed");
			if (eSkin != null)
				skinStatePressed = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("disabled");
			if (eSkin != null)
				skinStateDisabled = new SkinState(eSkin, themeRootURL, xgui);
			
			notifyUpdate();
		}
		
	}
	
	private CheckboxStyle style = null;
	
	protected boolean bChecked = false;
	private int m_boxSize = 15;
	private int m_padding = 0;
	private Location init_loc;
	
	/** sets the padding (the space between the box and the text */
	public void setPadding(Integer padding) { 
		m_padding = padding; 
		setText(text); 
	}
	
	public Boolean isChecked() { return bChecked; }
	public void setChecked(Boolean value) {
		checkAttachedToParent();
		if (bChecked != value) {
			bChecked = value;
			repaint();
		}
	}
	
	@Override
	protected void init() 
	{
		style = (CheckboxStyle)getGUI().getStyle(styleID, CheckboxStyle.class);
		style.addSubscriber(this);
		
		suspendPaint();
		setLocation(init_loc);
		updateFromStyle();
		setFont(style.FONT);
		resumePaint(false);
	}
	
	@Override
	protected void updateFromStyle() 
	{
		m_padding = style.TEXT_OFFSET_X;
		handleTextChanged();
	}

	public CheckBox(Integer x, Integer y, String text) {
		init_loc = new Location(x, y, 0, 0);
		this.text = text;
	}
	
	@Override
	protected void handleTextChanged() {
		if (m_recText == null)
			return;
		int w = m_boxSize + m_padding + (int)m_recText.getWidth() + 5;
		int h = Math.max(m_boxSize, (int)m_recText.getHeight());
		setLocation(m_loc.x, m_loc.y, w, h);
	}
	
	@Override
	protected synchronized void paint(Graphics2D gfx) 
	{
		int boxYoffs = Math.max(0,(int)(m_recText.getHeight() - m_boxSize) / 2);
		
		boolean skin_success = false;
		SkinState ss = null;
		if (!isEnabled())
			ss = style.skinStateDisabled;
		else
			switch (getButtonState()) {
			case 0: ss = style.skinStateNormal; break;
			case 1: ss = style.skinStateHover; break;
			case 2: ss = style.skinStatePressed; break;
			}
		
		if (ss != null) {
			WebImage box = ss.getElement("box");
			WebImage tick = ss.getElement("tick");
			
			if (getMediaMan().checkAllImagesLoaded(this, box, tick))
			{
				drawImage(box, 0, boxYoffs);
				if (bChecked)
					drawImage(tick, style.TICK_OFFSET_X, boxYoffs + style.TICK_OFFSET_Y);
				
				skin_success = true;
			}
			
			gfx.setColor(ss.getTextColor().awtClr);
		}
		
		if (!skin_success) {
			// draw box:
			Color fillColor = null;
			if (!isEnabled())
				fillColor = XColors.disabledLight; //disabled paint different
			else
			switch (getButtonState()) {
			case 0: fillColor = XColors.checkBoxNorm; break;
			case 1: fillColor = XColors.checkBoxHigh; break;
			case 2: fillColor = XColors.checkBoxPressed; break;
			}
			getGUI().draw3DFrame(gfx, new Location(0, boxYoffs, m_boxSize, m_boxSize), fillColor, false);
			
			if (bChecked) {
				// draw check mark
				if (!isEnabled())
					gfx.setColor(XColors.disabledMed.awtClr);
				else
					gfx.setColor(XColors.checkBoxMark.awtClr);
				gfx.fillRect(4, boxYoffs+4, m_boxSize-8, m_boxSize-8);
			}
			
			if (!isEnabled())
				gfx.setColor(XColors.disabledMed.awtClr);
			else
				gfx.setColor(XColors.text.awtClr);
		}
		
		m_textLayout.draw(gfx, m_boxSize + m_padding, m_loc.h/2.0f - (float)m_recText.getCenterY());
	}
	
	@Override
	protected void handleMouseReleased(MouseArgs arg0) {
		suspendPaint();
		if (arg0.button == 1)
			setChecked(!bChecked);
		super.handleMouseReleased(arg0);
		resumePaint(true);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

}
