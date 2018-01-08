package xGUI.controls;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextHitInfo;
import java.awt.geom.Rectangle2D;

import kernel.MediaManager.WebImage;

import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import wrappers.Cursor;
import wrappers.XMLNode;
import xGUI.IStyleLoader;
import xGUI.KeyArgs;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.VisualComponent;
import xGUI.XColors;
import xGUI.XGUI;
import xGUI.XMLFontInfo;


public class EditBox extends VisualComponent
{
	/** this is the identifier of the style node, in the theme XML */
	public static final String styleID = "textBox";
	
	public static class editBoxStyle extends IStyleLoader
	{
		/** default metrics */
		private final int DEFAULT_MIN_WIDTH = 20;
		private final int DEFAULT_MIN_HEIGHT = 22;
		
		/** metrics (set by theme) */
		protected int MIN_WIDTH = DEFAULT_MIN_WIDTH;
		protected int MIN_HEIGHT = DEFAULT_MIN_HEIGHT;
		protected Font FONT = null;
		protected SkinState skinStateNormal = null;
		protected SkinState skinStateHover = null;
		protected SkinState skinStateFocused = null;
		protected SkinState skinStateFocusedHover = null;
		protected SkinState skinStateDisabled = null;
		
		private XGUI xgui;
		
		public editBoxStyle(XGUI owner)
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
				
				XMLNode eFont = eRoot.getSubnode( "font"); 
				XMLFontInfo xf = new XMLFontInfo(eFont, xgui.getDefaultFont(), xgui.getDefaultTextColor());
				FONT = xf.font;
			}
			
			XMLNode eFont = eRoot.getSubnode( "font"); 
			XMLFontInfo xf = new XMLFontInfo(eFont, xgui.getDefaultFont(), xgui.getDefaultTextColor());
			FONT = xf.font;
			
			XMLNode eSkin = eRoot.getSubnode("normal");
			if (eSkin != null)
				skinStateNormal = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("hover");
			if (eSkin != null)
				skinStateHover = new SkinState(eSkin, themeRootURL, xgui);
			else
				skinStateHover = skinStateNormal;
			eSkin = eRoot.getSubnode("focused");
			if (eSkin != null)
				skinStateFocused = new SkinState(eSkin, themeRootURL, xgui);
			eSkin = eRoot.getSubnode("focused_hover");
			if (eSkin != null)
				skinStateFocusedHover = new SkinState(eSkin, themeRootURL, xgui);
			else
				skinStateFocusedHover = skinStateFocused;
			eSkin = eRoot.getSubnode("disabled");
			if (eSkin != null)
				skinStateDisabled = new SkinState(eSkin, themeRootURL, xgui);
			
			notifyUpdate();
		}
	}
	
	private editBoxStyle style = null;
	
	private int iCaretPos = 0;
	private Shape caret = null;
	private boolean caretVisible = false;
	
	/** white pixels between border and text */
	private int whiteBorderX = 5;
	/** white pixels between border and text */
	private int whiteBorderY = 3;
	
	/** horizontal scroll in pixels (always 0 or negative) */
	private int iScrollX = 0;
	
	private int selStart = -1; //selection start (absolute offset)
	private int selEnd = -1; // selection end (absolute offset)
	private int selOrigin = -1; // selection origin (where it first started)
	Shape selection = null;
	
	protected boolean m_readonly = false;
	
	private boolean m_isMouseIn = false;
	
	@Override
	protected synchronized void paint(Graphics2D gfx)
	{
		boolean skin_success = false;
		SkinState ss = null;
		if (!isEnabled())
			ss = style.skinStateDisabled;
		else
			if (isFocused()) {
				if (m_isMouseIn)
					ss = style.skinStateFocusedHover;
				else
					ss = style.skinStateFocused;
			} else {
				if (m_isMouseIn)
					ss = style.skinStateHover;
				else
					ss = style.skinStateNormal;
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
			
			if (getMediaMan().checkAllImagesLoaded(this, bl, br, bt, bb, ctl, ctr, cbl, cbr)) {
				skin_success = true;
				
				int ctlw = ctl.getWidth(this);
				int mtw = m_loc.w - ctlw - ctr.getWidth(this);
				int ctlh = ctl.getHeight(this);
				int blh = m_loc.h - ctlh - cbl.getHeight(this);
				int blw = bl.getWidth(this);
				int brw = br.getWidth(this);
				int mw = m_loc.w - blw - brw;
				int bth = bt.getHeight(this);
				int bbh = bb.getHeight(this);
				int mh = m_loc.h - bth - bbh;
				int ctrh = ctr.getHeight(this);
				int brh = m_loc.h - ctrh - cbr.getHeight(this);
				int cblw = cbl.getWidth(this);
				int bbw = m_loc.w - cblw - cbr.getWidth(this);
				
				drawImage(ctl, 0, 0);
				drawImage(bt, ctlw, 0, mtw, bth);
				drawImage(ctr, ctlw+mtw, 0);
				drawImage(bl, 0, ctlh, blw, blh);
				drawImage(br, blw+mw, ctrh, brw, brh);
				drawImage(cbl, 0, ctlh+blh);
				drawImage(bb, cblw, bth+mh, bbw, bbh);
				drawImage(cbr, cblw+bbw, ctrh+brh);
				
				gfx.setColor(isEnabled() ? XColors.editBackground.awtClr : XColors.disabledLight.awtClr);
				gfx.fillRect(blw, bth, mw, mh);
				
			}
			
			gfx.setColor(ss.getTextColor().awtClr);
		}
		
		if (!skin_success) 
		{
			Location lFrame = new Location(1, 1, m_loc.w-2, m_loc.h-2);
			getGUI().draw3DFrame(gfx, lFrame, isEnabled() ? XColors.editBackground : XColors.disabledLight, false);
			if (isFocused()) {
				gfx.setColor(Color.gray75.awtClr);
				gfx.drawRect(0, 0, m_loc.w-1, m_loc.h-1);
			}
		}
		
		setClip(gfx, whiteBorderX, whiteBorderY, m_loc.w - 2*whiteBorderX, m_loc.h - 2*whiteBorderY);		
		// set scroll translation:
		gfx.translate(iScrollX, 0);
		
			if (selStart != selEnd) { // if selection is not empty
				gfx.setColor(isFocused() ? XColors.selection.awtClr : XColors.selectionUnfocused.awtClr);
				gfx.translate(whiteBorderX, whiteBorderY + fontInfo.ascent);
				gfx.fill(selection);
				gfx.translate(-whiteBorderX, -whiteBorderY - fontInfo.ascent);
			}
			
			if (isEnabled())
				gfx.setColor(XColors.text.awtClr);
			else
				gfx.setColor(XColors.disabledMed.awtClr);
			m_textLayout.draw(gfx, whiteBorderX, whiteBorderY + fontInfo.ascent);
			
			if (caretVisible && caret != null) {
				gfx.setColor(XColors.caret.awtClr);
				gfx.translate(whiteBorderX, whiteBorderY + fontInfo.ascent);
				gfx.draw(caret);
				gfx.translate(-whiteBorderX, -whiteBorderY - fontInfo.ascent);
			}
		gfx.translate(-iScrollX, 0);
		restoreClip(gfx);
	}
	
	@Override
	protected void init() 
	{
		style = (editBoxStyle)getGUI().getStyle(styleID, editBoxStyle.class);
		style.addSubscriber(this);
		
		setFocusable(true);
		setLocation(init_loc);
		updateFromStyle();		
		setText("EditBox");
		setCaretPos(text.length(), false);
	}
	
	@Override
	protected void updateFromStyle() { 
		setFont(style.FONT);
		int w = m_loc.w < style.MIN_WIDTH ? style.MIN_WIDTH : m_loc.w;
		int h = m_loc.h < style.MIN_HEIGHT ? style.MIN_HEIGHT : m_loc.h;
		setLocation(m_loc.x, m_loc.y, w, h);
	}
	
	public EditBox(Integer x, Integer y, Integer w) 
	{
		init_loc = new Location(x,y,Math.max(w, 40),22);
		m_visible = true;
		cursor = Cursor.TEXT;
	}
	
	@Override
	protected void handleMousePressed(MouseArgs arg0) {
		suspendPaint();
			Location lClick = screenToLocal(arg0.x, arg0.y);
			TextHitInfo h = m_textLayout.hitTestChar(lClick.x - whiteBorderX - iScrollX, lClick.y - whiteBorderY);
			setCaretPos(h.getInsertionIndex(), false);
		resumePaint(true);
		onMousePressed.fire(arg0);
	}
	
	@Override
	protected void handleMouseDragged(MouseArgs arg0) {
		suspendPaint();
			Location lClick = screenToLocal(arg0.x, arg0.y);
			TextHitInfo h = m_textLayout.hitTestChar(lClick.x - whiteBorderX - iScrollX, lClick.y - whiteBorderY);
			setCaretPos(h.getInsertionIndex(), true);
		resumePaint(true);
		onMouseDragged.fire(arg0);
	}
	
	@Override
	protected void handleMouseWheel(MouseArgs arg0) {
		iScrollX -= arg0.wheelSteps * 30;
		if (iScrollX > 0)
			iScrollX = 0;
		int maxScroll = (int)m_recText.getWidth() + 30 - (m_loc.w - 2*whiteBorderX);
		if (maxScroll > 0) {
			if (iScrollX < -maxScroll)
				iScrollX = -maxScroll;
		}
		else
			iScrollX = 0;
		
		repaint();
		onMouseWheel.fire(arg0);
	}
	
	@Override
	protected void handleGetFocus() {
		showCaret();
		onFocus.fire();
	}
	
	@Override
	protected void handleLostFocus() {
		hideCaret();
		onFocusLost.fire();
	}
	
	@Override
	protected boolean handleKeyTyped(KeyArgs arg) 
	{
		boolean used = false;
		if (!m_readonly && arg.isChar()) {
			suspendPaint();
				setText(
					text.substring(0,iCaretPos) +
					String.valueOf(arg.keyChar) +
					text.substring(iCaretPos, text.length())
				);
				moveCaret(+1, false);
			resumePaint(true);
			used = true;
		}
		
		onKeyTyped.fire(arg);
		return used;
	}
	
	@Override
	protected void handleMouseEntered(MouseArgs arg0) 
	{
		m_isMouseIn = true;
		onMouseEnter.fire(arg0);
	}
	
	@Override
	protected void handleMouseExit(MouseArgs arg0) 
	{
		m_isMouseIn = false;
		onMouseEnter.fire(arg0);
	}
	
	@Override
	protected boolean handleKeyPressed(KeyArgs arg) 
	{
		boolean used = false;
		switch (arg.keyCode) {
		
		case KeyArgs.KEY_BACKSPACE:
			if (!text.isEmpty() && iCaretPos > 0) {
				suspendPaint();
				setText(
					text.substring(0,iCaretPos-1) + 
					text.substring(iCaretPos, text.length())
				);
				moveCaret(-1, false);
				resumePaint(true);
			}
			used = true;
			break;
		case KeyArgs.KEY_DELETE:
			if (!text.isEmpty() && iCaretPos < text.length()) {
				suspendPaint();
				setText(
					text.substring(0,iCaretPos) + 
					text.substring(iCaretPos+1, text.length())
				);
				resumePaint(true);
			}
			used = true;
			break;
		case KeyArgs.KEY_LEFT:
			moveCaret(-1, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			used = true;
			break;
		case KeyArgs.KEY_RIGHT:
			moveCaret(+1, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			used = true;
			break;
		case KeyArgs.KEY_HOME:
			setCaretPos(0, arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			used = true;
			break;
		case KeyArgs.KEY_END:
			setCaretPos(text.length(), arg.checkModifier(KeyArgs.SHIFT_DOWN_MASK));
			used = true;
			break;
		default: ;
		}
		
		onKeyPressed.fire(arg);
		return used;
	}
	
	// === Public methods ==============================================================
	
	public Integer getCaretPos() { return iCaretPos; }
	public void setCaretPos(Integer pos, Boolean shiftDown)
	{
		if (pos == iCaretPos && caret != null) {
			if (!shiftDown && selStart != selEnd) {
				selOrigin = selStart = selEnd = iCaretPos;
				repaint(); //TODO add area to repaint ;)
			}
			
			return;
		}
		
		suspendPaint();
		
		hideCaret();
		iCaretPos = Math.max(0, Math.min(pos,text.length()));
		if (m_textLayout != null) {
			caret = m_textLayout.getCaretShapes(iCaretPos)[0];
			Rectangle2D rb = caret.getBounds2D();
			// check for caret outside of edit box area:
			boolean needRepaint = false;
			if (rb.getX() + iScrollX < 2*whiteBorderX) {
				iScrollX = Math.min(0, (int) -rb.getX() + 2*whiteBorderX);
				needRepaint = true;
			}
			if (rb.getMaxX() + iScrollX >= m_loc.w - 3*whiteBorderX) {
				iScrollX = m_loc.w - 3*whiteBorderX -(int)rb.getMaxX();
				needRepaint = true;
			}
			showCaret();
			if (needRepaint)
				repaint();
		}
		
		if (!shiftDown) {
			boolean repaint = selStart!=selEnd;
			selOrigin = selStart = selEnd = iCaretPos;
			if (repaint)
				repaint();
		} else {
			selStart = Math.min(selOrigin, iCaretPos);
			selEnd = Math.max(selOrigin, iCaretPos);
			selection = m_textLayout.getVisualHighlightShape(TextHitInfo.beforeOffset(selStart), TextHitInfo.afterOffset(selEnd));
			repaint();
		}
		
		resumePaint(true);
	}
	public void moveCaret(Integer delta, Boolean shiftDown) {
		setCaretPos(iCaretPos + delta, shiftDown);
	}
	
	/** hides the caret. */
	public void hideCaret()	{
		if (caretVisible) {
			caretVisible = false;
			repaint();	//TODO restrict area to caret
		}
	}	
	/** shows the caret */
	public void showCaret() {
		if (isFocused()) {
			caretVisible = true;
			repaint();	//TODO restrict area to caret
		}
	}
	
	public void setReadonly(Boolean ro) {
		m_readonly = ro;
	}
	public Boolean getReadonly() { return m_readonly; }
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

	@Override
	public void destroy() {
		
	}

}
