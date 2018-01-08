package xGUI.controls;

import java.awt.Font;
import java.awt.Graphics2D;


import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import kernel.Event;
import kernel.MediaManager.WebImage;

import wrappers.Color;
import wrappers.Cursor;
import wrappers.XMLNode;
import xGUI.IStyleLoader;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.TextAlign;
import xGUI.XColors;
import xGUI.VisualComponent;
import xGUI.XGUI;
import xGUI.XMLFontInfo;


public class Button extends VisualComponent 
{
	/** this is the identifier of the style node, in the theme XML */
	public static final String styleID = "button";
	
	public static class ButtonStyle extends IStyleLoader
	{
		/** default metrics */
		private final int DEFAULT_MIN_WIDTH = 40;
		private final int DEFAULT_MIN_HEIGHT = 20;
		private final Color DEFAULT_TEXT_COLOR = XColors.text;
		
		/** metrics (set by theme) */
		protected int MIN_WIDTH = DEFAULT_MIN_WIDTH;
		protected int MIN_HEIGHT = DEFAULT_MIN_HEIGHT;
		protected TextAlign TEXT_ALIGN_X = TextAlign.Center;
		protected TextAlign TEXT_ALIGN_Y = TextAlign.Center;
		protected int TEXT_OFFSET_X = 0;
		protected int TEXT_OFFSET_Y = 0;
		protected Color TEXT_COLOR = DEFAULT_TEXT_COLOR;
		protected Font FONT = null;
		protected SkinState skinStateNormal = null;
		protected SkinState skinStateHover = null;
		protected SkinState skinStatePressed = null;
		protected SkinState skinStateDisabled = null;
		
		private XGUI xgui;
		
		public ButtonStyle(XGUI owner)
		{
			this.xgui = owner;
		}
		
		@Override
		public void loadStyle(XMLNode eRoot, String themeRootURL) 
		{
			XMLNode eLayout = eRoot.getSubnode("layout");
			if (eLayout != null) {
				MIN_WIDTH = eLayout.getIntAttr("minWidth", DEFAULT_MIN_WIDTH);
				MIN_HEIGHT = eLayout.getIntAttr("minHeight", DEFAULT_MIN_HEIGHT);
				
				XMLNode eText = eLayout.getSubnode("text");
				if (eText != null) {
					TEXT_ALIGN_X = TextAlign.fromXMLAttrib(eText, "alignX", TextAlign.Center);
					TEXT_ALIGN_Y = TextAlign.fromXMLAttrib(eText, "alignY", TextAlign.Center);
					TEXT_OFFSET_X = eText.getIntAttr("offsetX", 0);
					TEXT_OFFSET_Y = eText.getIntAttr("offsetY", 0);
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
	
	private ButtonStyle style = null;
	
	/** 0 == normal, 1 == highlight, 2 == pressed */
	private int m_State = 0;
	/** 0 == normal, 1 == highlight, 2 == pressed */
	protected int getButtonState() { return m_State; }
	
	protected WebImage image = null;
	public enum ButtonLayout {
		textOnly,
		imageLeft,
		imageAbove,
		imageRight,
		imageBelow,
		imageUnder;
		
		static ButtonLayout[] valArray = values();
	}
	
	public static int BTNLAYOUT_TEXT = ButtonLayout.textOnly.ordinal();
	public static int BTNLAYOUT_IMAGELEFT = ButtonLayout.imageLeft.ordinal();
	public static int BTNLAYOUT_IMAGERIGHT = ButtonLayout.imageAbove.ordinal();
	public static int BTNLAYOUT_IMAGEABOVE = ButtonLayout.imageAbove.ordinal();
	public static int BTNLAYOUT_IMAGEBELOW = ButtonLayout.textOnly.ordinal();
	public static int BTNLAYOUT_IMAGEUNDER = ButtonLayout.textOnly.ordinal();
	
	protected ButtonLayout layout = ButtonLayout.imageLeft;
	public void setLayout(ButtonLayout layout) {
		checkAttachedToParent();
		this.layout = layout;
		requestPaint(this, m_loc);
	}
	
	public void setLayout(Integer enumIntValue) {
		setLayout(ButtonLayout.valArray[enumIntValue]);
	}
	
	protected class ButtonMetrics
	{
		float textOffX;
		float textOffY;
		float imgOffX;
		float imgOffY;
		ButtonMetrics(float tx, float ty, float ix, float iy) {
			textOffX = tx;
			textOffY = ty;
			imgOffX = ix;
			imgOffY = iy;
		}
	}
	
	public Button(Integer x, Integer y, Integer w, Integer h) {
		this();
		init_loc = new Location(x,y,w,h);
		text = "Button";
	}
	
	@Override
	protected void init() 
	{
		style = (ButtonStyle)getGUI().getStyle(styleID, ButtonStyle.class);
		style.addSubscriber(this);
		
		suspendPaint();
		setLocation(init_loc);
		updateFromStyle();
		resumePaint(false);
	}
	
	@Override
	protected void updateFromStyle() {
		setFont(style.FONT);
		int w = m_loc.w < style.MIN_WIDTH ? style.MIN_WIDTH : m_loc.w;
		int h = m_loc.h < style.MIN_HEIGHT ? style.MIN_HEIGHT : m_loc.h;
		setLocation(m_loc.x, m_loc.y, w, h);
	}
	
	protected Button() {
		m_Events.add(onClick = new Event(this));
		m_visible = true;
		cursor = Cursor.HAND;
	}
	
	public Button(Integer x, Integer y, Integer w, Integer h, String name) {
		this(x,y,w,h);
		text = name;
	}
	
	public Button setImage(String url) {
		checkAttachedToParent();
		image = getMediaMan().requestImage(url, this);
		return this;
	}

	/**
	 * onClick event receives one parameter : 
	 * @param Button sender
	 */
	public final Event onClick;

	@Override
	protected void paint(Graphics2D gfx) 
	{
		boolean skin_success = false;
		SkinState ss = null;
		if (!isEnabled())
			ss = style.skinStateDisabled;
		else
			switch (m_State) {
			case 0: ss = style.skinStateNormal; break;
			case 1: ss = style.skinStateHover; break;
			case 2: ss = style.skinStatePressed; break;
			}
		
		if (ss != null) {
			WebImage bl = ss.getElement("border_left");
			WebImage br = ss.getElement("border_right");
			WebImage bt = ss.getElement("border_top");
			WebImage bb = ss.getElement("border_bottom");
			WebImage ctl = ss.getElement("corner_topleft");
			WebImage ctr = ss.getElement("corner_topright");
			WebImage cbl = ss.getElement("corner_bottomleft");
			WebImage cbr = ss.getElement("corner_bottomright");
			WebImage ms = ss.getElement("stretch");
			
			skin_success = drawSkin(ctl, bt, ctr, br, cbr, bb, cbl, bl, ms);
			
			gfx.setColor(isEnabled() ? ss.getTextColor().awtClr : XColors.disabledDark.awtClr);
		}
		
		if (!skin_success) {
			// no skin available for current state, so use default drawing:
			Color fillClr = getButtonFillColor();
			getGUI().draw3DFrame(gfx, m_loc.untranslate(), fillClr, m_State!=2);
			
			gfx.setColor(isEnabled() ? (ss!=null ? ss.getTextColor().awtClr : XColors.text.awtClr) : XColors.disabledDark.awtClr);
		}

		setClip(gfx, 2, 2, m_loc.w-4, m_loc.h-4);
		
		ButtonMetrics met = getButtonMetrics();
		
		//TODO if disabled, gray out image using a filter
		if (image != null)
			drawImage(image, (int)met.imgOffX, (int)met.imgOffY);
		
		met.textOffX += (m_State==2 ? 1:0);
		met.textOffY += (m_State==2 ? 1:0);
		m_textLayout.draw(gfx, met.textOffX, met.textOffY);
		
		restoreClip(gfx);
	}

	protected ButtonMetrics getButtonMetrics() 
	{
		float textOffX = m_loc.w/2 - (float)m_recText.getCenterX(); 
		float textOffY = m_loc.h/2 - (float)m_recText.getCenterY();
		float imgOffX = 0, imgOffY = 0;
		if (image != null) {
			float tW = image.getWidth(this) + 10 + (float)m_recText.getWidth();
			float tH = image.getHeight(this) + 10 + (float)m_recText.getHeight();
			switch (layout) {
			case imageAbove:
				imgOffX = (m_loc.w - image.getWidth(this)) / 2;
				imgOffY = (m_loc.h - tH) / 2;
				textOffY = imgOffY + image.getHeight(this) + 10 - (float)m_recText.getY();
				break;
			case imageBelow:
				imgOffX = (m_loc.w - image.getWidth(this)) / 2;
				textOffY = (m_loc.h - tH) / 2 - (float)m_recText.getY();
				imgOffY = textOffY + (float)m_recText.getY() + (float)m_recText.getHeight() + 10;
				break;
			case imageLeft:
				imgOffY = (m_loc.h - image.getHeight(this))/2;
				imgOffX = (m_loc.w - tW) / 2;
				textOffX = imgOffX + image.getWidth(this) + 10 - (float)m_recText.getX();
				break;
			case imageRight:
				imgOffY = (m_loc.h - image.getHeight(this))/2;
				textOffX = (m_loc.w - tW) / 2 - (float)m_recText.getX();
				imgOffX = textOffX + (float)m_recText.getWidth() + 10;
				break;
			case imageUnder:
				imgOffX = (m_loc.w - image.getWidth(this)) / 2;
				imgOffY = (m_loc.h - image.getHeight(this)) / 2;
				break;
			}
			imgOffX += (m_State==2 ? 1:0);
			imgOffY += (m_State==2 ? 1:0);
		}
		return new ButtonMetrics(textOffX, textOffY, imgOffX, imgOffY);
	}

	protected Color getButtonFillColor() {
		if (!isEnabled())
			return XColors.disabledLight;
		else
		switch (m_State) {
		case 0: return XColors.windowBackground;
		case 1: return XColors.buttonHigh;
		case 2: return XColors.buttonPressed;
		default : return Color.magenta;
		}
	}

	@Override
	protected void handleMouseEntered(MouseArgs arg0) {
		if (arg0.checkModifier(MouseArgs.BUTTON1_DOWN_MASK))
			m_State = 2;
		else
			m_State = 1;
		requestPaint(this, m_loc);
		onMouseEnter.fire(this, arg0);
	}
	
	@Override
	protected void handleMouseExit(MouseArgs arg0) {
		if (arg0.checkModifier(MouseArgs.BUTTON1_DOWN_MASK))
			m_State = 1;
		else
			m_State = 0;
		requestPaint(this, m_loc);
		onMouseExit.fire(this, arg0);
	}
	
	@Override
	protected void handleMousePressed(MouseArgs arg0) {
		if (arg0.button == 1) {
			m_State = 2;
			requestPaint(this, m_loc);
		}
		onMousePressed.fire(this, arg0);
	}
	
	@Override 
	protected void handleMouseReleased(MouseArgs arg0) {
		onMouseReleased.fire(arg0);
		suspendPaint();
		if (arg0.button == 1 && arg0.target == this) {
			m_State = 1;
			//fire click event
			onClick.fire();
		} else
			m_State = 0;
		requestPaint(this, m_loc);
		resumePaint(true);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

	@Override
	public void destroy() {
		image = null;
	}

}
