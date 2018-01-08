package xGUI;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import kernel.MediaManager.WebImage;
import kernel.Message;
import wrappers.Color;
import wrappers.Cursor;
import wrappers.XMLNode;
import app.IApplication;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;


/**
 * Window class for X-GUI system
 * @author bog
 *
 */
public class Window extends Container
{
	/** this is the identifier of the style node, in the theme XML */
	public static final String styleID = "window";
	
	public static class WindowStyle extends IStyleLoader
	{
		/* default metrics and attributes :*/
		private final int DEFAULT_MIN_WIN_WIDTH = 120;
		private final int DEFAULT_MIN_WIN_HEIGHT = 120;
		private final int DEFAULT_ICON_X = 5;
		private final int DEFAULT_ICON_Y = 5;
		private final int DEFAULT_TITLE_OFFSET_X = 8;
		private final int DEFAULT_TITLE_OFFSET_Y = 2;
		private final Color DEFAULT_TEXT_COLOR = XColors.text;
		private final int DEFAULT_BORDER_TOP = 24;
		private final int DEFAULT_BORDER_LEFT = 3;
		private final int DEFAULT_BORDER_RIGHT = 3;
		private final int DEFAULT_BORDER_BOTTOM = 3;
		private final int DEFAULT_BORDER_RESIZE = 4;
		
		/* window metrics and attributes (can be modified by theme) */	
		protected int MIN_WINDOW_WIDTH = DEFAULT_MIN_WIN_WIDTH;
		protected int MIN_WINDOW_HEIGHT = DEFAULT_MIN_WIN_HEIGHT;
		protected int ICON_X = DEFAULT_ICON_X;
		protected int ICON_Y = DEFAULT_ICON_Y;
		protected TextAlign TITLE_ALIGN_X = TextAlign.Left;
		protected TextAlign TITLE_ALIGN_Y = TextAlign.Center;
		protected int TITLE_OFFSET_X = DEFAULT_TITLE_OFFSET_X; 
		protected int TITLE_OFFSET_Y = DEFAULT_TITLE_OFFSET_Y;
		protected Color TEXT_COLOR = DEFAULT_TEXT_COLOR;
		protected Font FONT = null;
		protected int BORDER_TOP = DEFAULT_BORDER_TOP;
		protected int BORDER_BOTTOM = DEFAULT_BORDER_BOTTOM;
		protected int BORDER_LEFT = DEFAULT_BORDER_LEFT;
		protected int BORDER_RIGHT = DEFAULT_BORDER_RIGHT;
		protected int BORDER_RESIZE = DEFAULT_BORDER_RESIZE;
		protected SkinState skinStateActive = null;
		protected SkinState skinStateInactive = null;
		
		private XGUI xgui;
		
		public WindowStyle(XGUI owner)
		{
			this.xgui = owner;
		}
		
		@Override
		public void loadStyle(XMLNode eRoot, String themeRootURL) 
		{
			XMLNode eLayout = eRoot.getSubnode("layout");
			if (eLayout != null) {
				MIN_WINDOW_WIDTH = eLayout.getIntAttr("minWidth", DEFAULT_MIN_WIN_WIDTH);
				MIN_WINDOW_HEIGHT = eLayout.getIntAttr("minHeight", DEFAULT_MIN_WIN_HEIGHT);
				
				XMLNode eIcon = eLayout.getSubnode("icon");
				if (eIcon != null) {
					ICON_X = eIcon.getIntAttr("X", DEFAULT_ICON_X);
					ICON_Y = eIcon.getIntAttr("Y", DEFAULT_ICON_Y);
				}
				
				XMLNode eBorder = eLayout.getSubnode("border");
				if (eBorder != null) {
					BORDER_TOP = eBorder.getIntAttr("top", DEFAULT_BORDER_TOP);
					BORDER_BOTTOM = eBorder.getIntAttr("bottom", DEFAULT_BORDER_BOTTOM);
					BORDER_LEFT = eBorder.getIntAttr("left", DEFAULT_BORDER_LEFT);
					BORDER_RIGHT = eBorder.getIntAttr("right", DEFAULT_BORDER_RIGHT);
					BORDER_RESIZE = eBorder.getIntAttr("resize_edge", DEFAULT_BORDER_RESIZE);
				}
				
				XMLNode eText = eLayout.getSubnode("text");
				if (eText != null) {
					TITLE_ALIGN_X = TextAlign.fromXMLAttrib(eText, "alignX", TextAlign.Left);
					TITLE_ALIGN_Y = TextAlign.fromXMLAttrib(eText, "alignY", TextAlign.Center);
					TITLE_OFFSET_X = eText.getIntAttr("offsetX", DEFAULT_TITLE_OFFSET_X);
					TITLE_OFFSET_Y = eText.getIntAttr("offsetY", DEFAULT_TITLE_OFFSET_Y);
				}
			}
				
			XMLNode eFont = eRoot.getSubnode("font");
			XMLFontInfo xf = new XMLFontInfo(eFont, xgui.getDefaultFont(), xgui.getDefaultTextColor());
			FONT = xf.font;
			TEXT_COLOR = xf.color;
			
			XMLNode eSkin = eRoot.getSubnode("active");
			if (eSkin != null)
				skinStateActive = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("inactive");
			if (eSkin != null)
				skinStateInactive = new SkinState(eSkin, themeRootURL, xgui);
			
			notifyUpdate();
		}
	}
	
	private WindowStyle style = null;

	// === internal stuff ==========================================
	
	BufferedImage frameBuffer = null;
	Graphics2D frameGfx = null;
	int m_mouseDX, m_mouseDY;
	
	private int m_rsBorderID = 0; // bit 0 -> left, bit 1-right, 2-up, 3-down
	
	private boolean m_Active = false;
	private boolean m_bMoving = false;
	private boolean m_bResizing = false;
	boolean m_bNeedRepaint = false;
	
	private int res_left_areaID;
	private int res_topleft_areaID;
	private int res_top_areaID;
	private int res_topright_areaID;
	private int res_right_areaID;
	private int res_bottomright_areaID;
	private int res_bottom_areaID;
	private int res_bottomleft_areaID;
	
	@Override
	protected Graphics2D getGfx() {
		return frameGfx;
	}
	
	@Override
	void invokePaint(Location loc, VisualComponent control, long threadID) 
	{
		if ((!m_visible && control!=m_parent) || m_parent == null)
			return;
		
		if (updateRgn.get(threadID) == null)
			updateRgn.put(threadID, new ArrayList<Rectangle>());
		ArrayList<Rectangle> rgn = updateRgn.get(threadID);
		
		if (control == this) {
			if (isPaintSuspended(threadID)) {
				rgn.add(new Rectangle(loc.x,loc.y,loc.w,loc.h));
				return;
			}
			if (loc == null) {
				if (rgn.isEmpty()) {
					loc = m_loc;
				} else {
					Rectangle2D r0 = rgn.get(0);
					for (int n = rgn.size(), i=1; i<n; i++)
						r0 = r0.createUnion(rgn.get(i));
					loc = new Location((int)r0.getX(), (int)r0.getY(), (int)r0.getWidth(), (int)r0.getHeight());
				}
			}
			
			if (frameBuffer == null) {
				recreateGraphics();
			}

			if (m_bNeedRepaint || loc == m_loc)
			{
				if (loc == m_loc)
					m_bNeedRepaint = false;
				
				setClip(frameGfx, loc.translate(-m_loc.x, -m_loc.y));
				paint(frameGfx);
				restoreClip(frameGfx);
			} // no need to repaint if only moving the window
		} else {
			// obtain window client area coordinates for clip area
			Container pp = control.m_parent;
			while (pp != null && pp != this) {
				loc = Location.clientToParent(loc, pp);
				pp = pp.m_parent;
			}
			if (pp == this) {
				// paint a specific control area:
				if (frameBuffer == null) {
					recreateGraphics();
				}
				loc = loc.translate(m_clTranslateX + m_BL, m_clTranslateY + m_BT);
				setClip(frameGfx, loc);
				paint(frameGfx);
				restoreClip(frameGfx);
				loc = Location.clientToParent(loc.translate(-m_clTranslateX-m_BL, -m_clTranslateY-m_BT), this);
			}
			else if (pp == null) {
				// a request arrived to repaint the desktop behind window
				if (m_visible) {
					paint(frameGfx);
				}
				m_parent.invokePaint(loc, m_parent, threadID);
				return;
			}
		}
		m_parent.invokePaint(Location.clientToParent(loc, m_parent), m_parent, threadID);
	}
	
	private void recreateGraphics() {
		frameBuffer = new BufferedImage(m_loc.w,m_loc.h,BufferedImage.TYPE_INT_ARGB);
		frameGfx = frameBuffer.createGraphics();
		frameGfx.setClip(0,0,m_loc.w,m_loc.h);
		frameGfx.setComposite(AlphaComposite.Src);
		frameGfx.setColor(Color.transparent.awtClr);
		frameGfx.fillRect(0,0,m_loc.w,m_loc.h);
		frameGfx.setComposite(AlphaComposite.SrcOver);
		updateGfx(frameGfx);
		m_bNeedRepaint = true;
	}
	
	@Override
	protected synchronized void handleSizeChanged() 
	{
		int w = m_loc.w, h = m_loc.h;
		if (w < style.MIN_WINDOW_WIDTH)
			w = style.MIN_WINDOW_WIDTH;
		if (h < style.MIN_WINDOW_HEIGHT)
			h = style.MIN_WINDOW_HEIGHT;
		if (w != m_loc.w || h != m_loc.h)
			m_loc = new Location(m_loc.x, m_loc.y, w, h);
		
		// update custom cursor areas
		int bord = style.BORDER_RESIZE;
		updateCursorArea(res_left_areaID, new Location(0, bord, bord, m_loc.h-2*bord), null);
		updateCursorArea(res_right_areaID, new Location(m_loc.w-bord, bord, bord, m_loc.h-2*bord), null);
		updateCursorArea(res_topleft_areaID, new Location(0, 0, bord, bord), null);
		updateCursorArea(res_topright_areaID, new Location(m_loc.w-bord, 0, bord, bord), null);
		updateCursorArea(res_top_areaID, new Location(bord, 0, m_loc.w - 2*bord, bord), null);
		updateCursorArea(res_bottom_areaID, new Location(bord, m_loc.h-bord, m_loc.w - 2*bord, bord), null);
		updateCursorArea(res_bottomleft_areaID, new Location(0, m_loc.h-bord, bord, bord), null);
		updateCursorArea(res_bottomright_areaID, new Location(m_loc.w-bord, m_loc.h-bord, bord, bord), null);
		// done with cursors
		
		onResize.fire();
		
		frameBuffer = null; // force recreate graphics
		frameGfx = null;
	}
	
	@Override
	protected void init() 
	{
		style = (WindowStyle)getGUI().getStyle(styleID, WindowStyle.class);
		style.addSubscriber(this);
		
		res_left_areaID = addCursorArea(init_loc, Cursor.RESIZE_W);
		res_topleft_areaID = addCursorArea(init_loc, Cursor.RESIZE_NW);
		res_top_areaID = addCursorArea(init_loc, Cursor.RESIZE_N);
		res_topright_areaID = addCursorArea(init_loc, Cursor.RESIZE_NE);
		res_right_areaID = addCursorArea(init_loc, Cursor.RESIZE_E);
		res_bottomright_areaID = addCursorArea(init_loc, Cursor.RESIZE_SE);
		res_bottom_areaID = addCursorArea(init_loc, Cursor.RESIZE_S);
		res_bottomleft_areaID = addCursorArea(init_loc, Cursor.RESIZE_SW);
		
		setLocation(init_loc);
		recreateGraphics();
		
		suspendPaint();
		
		updateFromStyle();
		
		resumePaint(false);
	}
	
	@Override
	protected void updateFromStyle()
	{
		m_BL = style.BORDER_LEFT;
		m_BR = style.BORDER_RIGHT;
		m_BB = style.BORDER_BOTTOM;
		m_BT = style.BORDER_TOP;
		
		setFont(style.FONT);
	}

	Window(String text, Location loc, IApplication ownerApp)
	{
		init_loc = loc;
		this.text = text;
		this.ownerApp = ownerApp;
		ownerApp.envObjects.add(this);
		
		setEventsToApp();
	}

	@Override
	protected void handleMouseDragged(MouseArgs arg0) 
	{
		if (m_bMoving)
			setPosition(screenToParent(arg0.x-m_mouseDX, arg0.y-m_mouseDY));
		
		if (m_bResizing) {
			int x = m_loc.x,
				y = m_loc.y,
				w = m_loc.w,
				h = m_loc.h;
			Location ml = screenToLocal(arg0.x, arg0.y);
			if ((m_rsBorderID & 1) != 0) { // left border
				int dx = ml.x - m_mouseDX;
				w -= dx;
				x += dx;
			}
			if ((m_rsBorderID & 2) != 0) { // right border
				w += ml.x - m_mouseDX;
				m_mouseDX = ml.x;
			}
			if ((m_rsBorderID & 4) != 0) { // top border
				int dy = ml.y - m_mouseDY;
				h -= dy;
				y += dy;
			}
			if ((m_rsBorderID & 8) != 0) { // bottom border
				h += ml.y - m_mouseDY;
				m_mouseDY = ml.y;
			}
				
			setLocation(x, y, w, h);
		}
		
		onMouseDragged.fire(arg0);
	}
	
	@Override
	protected void handleMousePressed(MouseArgs arg0) 
	{
		if (arg0.button == 1) {
			Location ml = screenToLocal(arg0.x, arg0.y);
			m_mouseDX = ml.x;
			m_mouseDY = ml.y;
			if (ml.x > style.BORDER_RESIZE && ml.x < m_loc.w-2*style.BORDER_RESIZE && ml.y > style.BORDER_RESIZE && ml.y < m_BT )
				m_bMoving = true;
			else {
				m_rsBorderID = 0;
				
				if (ml.x <= style.BORDER_RESIZE) {
					m_rsBorderID |= 1;
				} else
				if (ml.x >= m_loc.w - style.BORDER_RESIZE) {
					m_rsBorderID |= 2;
				}
				
				if (ml.y <= style.BORDER_RESIZE) {
					m_rsBorderID |= 4;
				} else
				if (ml.y >= m_loc.h - style.BORDER_RESIZE) {
					m_rsBorderID |= 8;
				}
				
				m_bResizing = m_rsBorderID != 0;
			}
		}
		onMousePressed.fire(arg0);
	}
	
	@Override
	protected void handleMouseReleased(MouseArgs arg0) 
	{
		if (arg0.button == 1)
			m_bMoving = false;
		onMouseReleased.fire(arg0);
	}

	@Override
	protected void paintBorder(Graphics2D gfx) 
	{
		boolean skin_success = false;
		SkinState ss = m_Active ? style.skinStateActive : style.skinStateInactive;
		
		if (ss != null) {
			WebImage tbl = ss.getElement("titlebar_left");
			WebImage tbm = ss.getElement("titlebar_stretch");
			WebImage tbr = ss.getElement("titlebar_right");
			WebImage bl = ss.getElement("border_left");
			WebImage br = ss.getElement("border_right");
			WebImage cl = ss.getElement("corner_left");
			WebImage cr = ss.getElement("corner_right");
			WebImage bb = ss.getElement("border_bottom");
			
			if (getKernel().getMediaManager().checkAllImagesLoaded(this, tbl, tbm, tbr, bl, br, cl, cr, bb)) {
				skin_success = true;
			
				int tblw = tbl.getWidth(this);
				int tbrw = tbr.getWidth(this);
				int tblh = tbl.getHeight(this);
				int tbrh = tbr.getHeight(this);
				int clh = cl.getHeight(this);
				int clw = cl.getWidth(this);
				int crh = cr.getHeight(this);
				int crw = cr.getWidth(this);
				int blh = m_loc.h - tblh - clh;
				int brh = m_loc.h - tbrh - crh;
				
				gfx.setComposite(AlphaComposite.Src);
	
				drawImage(tbm, tblw, 0, m_loc.w-tblw-tbrw, m_BT);
				drawImage(bl, 0, tblh, m_BL, blh);
				drawImage(br, m_loc.w - m_BR, tbrh, m_BR, brh);
				drawImage(bb, clw, m_loc.h-m_BB, m_loc.w-clw-crw, m_BB);
				drawImage(tbl, 0, 0);
				drawImage(tbr, m_loc.w - tbrw, 0);
				drawImage(cl, 0, m_loc.h-clh);
				drawImage(cr, m_loc.w - crw, m_loc.h-crh);
					
				gfx.setComposite(AlphaComposite.SrcOver);
			}
			else
				m_bNeedRepaint = true;
			
			gfx.setColor(ss.getTextColor().awtClr);
		}
		if (!skin_success)
		{
			// we have no skin for current state, so use default drawing:
			if (m_Active)
				gfx.setColor(XColors.windowBorderActive.awtClr);
			else
				gfx.setColor(XColors.windowBorderInactive.awtClr);
			
			gfx.fillRect(0, 0, m_loc.w, m_BT);
			gfx.fillRect(0, 0, m_BL, m_loc.h);
			gfx.fillRect(0, m_loc.h-m_BB, m_loc.w, m_BB);
			gfx.fillRect(m_loc.w-m_BR, 0, m_BR, m_loc.h);
			
			getGUI().draw3DFrame(gfx, m_loc.untranslate(), null, true);
			
			gfx.setColor(m_Active ? XColors.windowTitleActive.awtClr : XColors.windowTitleInactive.awtClr);
		}
		
		m_textLayout.draw(gfx, 20, m_BT/2 - (float)m_recText.getCenterY()); //TODO (low) modify to use align from xml
	}
	
	void repaintBorderRequest() {
		getKernel().postMessage(new PaintBorderMsg(this, frameGfx, this));
		m_parent.requestPaint(m_parent, Location.clientToParent(m_loc, m_parent));
	}
	
	/** this is called when the window is deactivated by selecting another window */
	void deactivated() 
	{
		if (!m_Active)
			return;
		
		m_Active = false;
		
		handleLostFocus();
		
		repaintBorderRequest();
		
		// TODO send deactivate event
	}
	
	// ==== Public methods ====================================================
	
	@Override
	public String toString() {
		return "Window" + Message.hashString(this);
	}
	
	int getResizeBorder() { return style.BORDER_RESIZE; }
	
	public void activate() 
	{
		if (m_Active)
			return;
		
		{
			Window wActive = ((Desktop)m_parent).getActiveWindow();
			((Desktop)m_parent).setActiveWindow(this);
			
			if (wActive != null)
				wActive.deactivated();
			
		
			m_Active = true;
		
			suspendPaint();
			handleGetFocus();
			repaintBorderRequest();
			bringToTop();
			resumePaint(true);
		}	
		
		// TODO send activate event
	}
	
	public Boolean isActive() { return m_Active; }
	
	public void bringToTop() {
		if (m_parent.m_Controls.get(m_parent.m_Controls.size()-1) == this)
			return;
		m_parent.bringControlToTop(this);
	}
	
	@Override
	public Boolean isFocused() {
		checkAttachedToParent();
		return ((Desktop)m_parent).getActiveWindow() == this;
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
