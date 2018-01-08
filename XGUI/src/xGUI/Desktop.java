package xGUI;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import wrappers.Cursor;

import kernel.Message;

/**
 * This class acts as the bottom-most window, and contains all other windows as children
 * @author bog
 *
 */
public class Desktop extends Container implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener 
{
	// === private stuff ===============================================
	
	private Graphics2D screenGfx = null;
	private XGUI xgui = null;
	
	// === internal stuff ===============================================
	
	Window activeWindow = null;
	
	@Override
	public Location getScreenLocation() { return m_loc; }
	
	@Override
	protected void paintBackground(Graphics2D gfx) {
		gfx.setColor(XColors.desktop.awtClr);
		gfx.fillRect(m_loc.x+m_BL, m_loc.y+m_BT, m_loc.w-m_BL-m_BR, m_loc.h-m_BT-m_BB);
	}

	@Override
	protected synchronized void paint(Graphics2D gfx)
	{
		paintBackground(gfx);
		
		//TODO (low) check against clip area:
		paintBorder(gfx);
		
		gfx.translate(m_BL, m_BT);
		Rectangle r = gfx.getClipBounds();
		
		// limit clip region to client area:
		Location clientClip = (new Location(r.x, r.y, r.width, r.height)).limitToArea(getClientArea());
		gfx.setClip(clientClip.x, clientClip.y, clientClip.w, clientClip.h);
		
		// now blit windows:
		synchronized (m_Controls) {
			for (int i=0, n=getWindowCount(); i<n; i++)
			{
				Window w = getWindow(i);
				// verificat fiecare fereastra daca intersecteaza zona de clip
				if (w.isVisible() && w.m_loc.intersectRect(r))
					gfx.drawImage(w.frameBuffer, null,
						w.m_loc.x, w.m_loc.y);
			}
		}
		
		gfx.translate(-m_BL, -m_BT);
	}
	
	@Override
	protected void updateFromStyle() {
	}
	
	@Override
	protected Graphics2D getGfx() {
		return screenGfx;
	}
	
	@Override
	public Boolean isEnabled() { return true; }
	
	@Override
	public void invokePaint(Location loc, VisualComponent control, long threadID) 
	{
		super.invokePaint(loc, control, threadID);
		xgui.addDirtyRegion(loc);
	}
	
	@Override
	public void repaint() {
		suspendPaint();
		
		for (VisualComponent c : m_Controls)
			c.repaint();
		
		requestPaint(this, m_loc);
		resumePaint(true);
	}
	
	private VisualComponent m_MouseCaptured;
	private VisualComponent m_LastUnderMouse;
	
	private VisualComponent getControlAtPos(int x, int y)
	{
		Container c = this;
		boolean switched;
		do {
			switched = false;
			// get client coordinates:
			x -= c.m_loc.x + c.m_BL; 
			y -= c.m_loc.y + c.m_BT;
			
			Location client = c.getClientArea();
			if (x < 0 || x >= client.w || y < 0 || y >= client.h)
				break;
			
			x -=  c.m_clTranslateX;
			y -=  c.m_clTranslateY;
			
			// search for a child control below mouse :
			for (int i=c.m_Controls.size()-1; i>=0; i--) {
				VisualComponent cc = c.m_Controls.get(i);
				if (!cc.m_visible)
					continue;
				if (cc.m_loc.containsPoint(x, y)) {
					if (cc instanceof Container) {
						c = (Container)cc;
						switched = true;
						break;
					} else
						return cc;
				}
			}
		} while (switched);
		
		return c; // no children under mouse.
	}
	
	@Override
	public void mouseEntered(MouseEvent arg0) {
		// empty
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// empty
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) 
	{
		m_MouseCaptured = null;
		VisualComponent target = getControlAtPos(arg0.getX(), arg0.getY());
		if (target.isEnabled())
			target.handleMouseClicked(MouseArgs.fromMouseEvent(target, arg0));
	}
	
	@Override
	public void mousePressed(MouseEvent arg0) 
	{
		xgui.getApplet().requestFocus();
		
		VisualComponent target = getControlAtPos(arg0.getX(), arg0.getY());
		if (m_MouseCaptured != null && m_MouseCaptured.getLifeState() == RTEnvObj_LifeState.Available) {
			// mouse captured, just notify the capturer
			if (m_MouseCaptured.isEnabled())
				m_MouseCaptured.handleMousePressed(MouseArgs.fromMouseEvent(target, arg0));
		} else {
			// activate target's window:
			VisualComponent c = target;
			while (c != null && !(c instanceof Window))
				c = c.m_parent;
			boolean sameWindow = false;
			if (c != null) {
				sameWindow = ((Window)c).isActive();
				if (!sameWindow)
					((Window)c).activate();
			}
				
			m_MouseCaptured = target;
			if (target.isEnabled()) {
				VisualComponent toFocus = target;
				while (!(toFocus instanceof Window)) {
					if (toFocus.focusable) {
						toFocus.focus();
						break;
					} else {
						toFocus = toFocus.m_parent;
						if (toFocus == null)
							break;
					}
				}
				target.handleMousePressed(MouseArgs.fromMouseEvent(target, arg0));
			}
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0) {
		VisualComponent target = getControlAtPos(arg0.getX(), arg0.getY());
		MouseArgs marg = MouseArgs.fromMouseEvent(target, arg0);
		if (m_MouseCaptured != null && m_MouseCaptured.getLifeState() == RTEnvObj_LifeState.Available) {
			if (m_MouseCaptured.isEnabled())
				m_MouseCaptured.handleMouseReleased(marg);
		} else
			if (target.isEnabled())
				target.handleMouseReleased(marg);
		
		if (!marg.checkModifier(MouseArgs.BUTTON1_DOWN_MASK) 
			&& !marg.checkModifier(MouseArgs.BUTTON2_DOWN_MASK)
			&& !marg.checkModifier(MouseArgs.BUTTON3_DOWN_MASK))
			
			m_MouseCaptured = null;
	}
	
	@Override
	public void mouseDragged(MouseEvent arg0) 
	{
		if (m_MouseCaptured == null || m_MouseCaptured.getLifeState() != RTEnvObj_LifeState.Available || !m_MouseCaptured.isEnabled())
			return;
	
		VisualComponent target = getControlAtPos(arg0.getX(), arg0.getY());
		
		m_MouseCaptured.handleMouseDragged(MouseArgs.fromMouseEvent(m_MouseCaptured, arg0));
		
		if (target != m_LastUnderMouse) {
			if (m_LastUnderMouse == m_MouseCaptured)
				m_MouseCaptured.handleMouseExit(MouseArgs.fromMouseEvent(m_MouseCaptured, arg0));
			if (target == m_MouseCaptured)
				m_MouseCaptured.handleMouseEntered(MouseArgs.fromMouseEvent(m_MouseCaptured, arg0));
			
			m_LastUnderMouse = target;
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent arg0) 
	{
		VisualComponent target = (m_MouseCaptured != null) ? m_MouseCaptured : getControlAtPos(arg0.getX(), arg0.getY());  
		if (target != m_LastUnderMouse) {
			if (m_LastUnderMouse != null && m_LastUnderMouse.getLifeState() == RTEnvObj_LifeState.Available && m_LastUnderMouse.isEnabled())
				m_LastUnderMouse.handleMouseExit(MouseArgs.fromMouseEvent(m_LastUnderMouse, arg0));
			if (target.isEnabled())
				target.handleMouseEntered(MouseArgs.fromMouseEvent(target, arg0));
			
			m_LastUnderMouse = target;
		}
		else
			if (target.isEnabled())
				target.handleMouseMoved(MouseArgs.fromMouseEvent(target, arg0));
		
		// optimizat aici, doar cand se schimba targetul sau customArea din target;)
		VisualComponent curSrc = target;
		Cursor cur = curSrc.cursor;
		while (curSrc != null && (!curSrc.isEnabled() || (cur = curSrc.cursorForPoint(curSrc.screenToLocal(arg0.getX(), arg0.getY()))) == null))
			curSrc = curSrc.m_parent;
		setCursor(cur);
	}
	
	private void setCursor(Cursor cur) {
		getGUI().getApplet().setCursor(cur.awtCursor);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		VisualComponent target = getControlAtPos(arg0.getX(), arg0.getY()); 
		if (target.isEnabled())
			target.handleMouseWheel(MouseArgs.fromMouseWheelEvent(target, arg0));
	}
	
	protected void setMouseCaptureOverride(VisualComponent comp) {
		// TODO Auto-generated method stub
		
	}
	
	

	@Override
	public void keyPressed(KeyEvent arg0) 
	{
		KeyArgs a = KeyArgs.fromKeyEvent(arg0);
		arg0.consume();
		// check system keys... like alt-tab 'n' shit like that
		//....
		
		// now if we didn't previously used the key, forward it to the active window
		if (activeWindow != null)
			activeWindow.handleKeyPressed(a);
	}
	
	@Override
	public void keyReleased(KeyEvent arg0) 
	{
		KeyArgs a = KeyArgs.fromKeyEvent(arg0);
		arg0.consume();
		// check system keys... like alt-tab 'n' shit like that
		//....
		// no important shit usually happens on this function
		
		// now if we didn't previously used the key, forward it to the active window
		if (activeWindow != null)
			activeWindow.handleKeyReleased(a);
	}
	
	@Override
	public void keyTyped(KeyEvent arg0) 
	{
		KeyArgs a = KeyArgs.fromKeyEvent(arg0);
		arg0.consume();
		// check system keys... like alt-tab 'n' shit like that
		//....
		
		// now if we didn't previously used the key, forward it to the active window
		if (activeWindow != null)
			activeWindow.handleKeyTyped(a);
	}
	
	@Override
	protected XGUI getGUI() {
		return xgui;
	}
	
	@Override
	protected void init() 
	{
		suspendPaint();
		Rectangle rBnd = screenGfx.getDeviceConfiguration().getBounds();
		setLocation(new Location(0,0,rBnd.width, rBnd.height));
		resumePaint(false);
		
		screenGfx.setClip(0, 0, rBnd.width, rBnd.height);
		
		requestPaint(this, m_loc);
	}
	
	@Override
	protected void checkAttachedToParent() {
		if (xgui == null)
			throw new RuntimeException("Desktop has no GUI !");
	}
	
	@Override
	protected void handleGetFocus() {}
	@Override
	protected boolean handleKeyPressed(KeyArgs arg) {return false;}
	@Override
	protected boolean handleKeyReleased(KeyArgs arg) {return false;}
	@Override
	protected boolean handleKeyTyped(KeyArgs arg) {return false;}
	@Override
	protected void handleLostFocus() {}
	@Override
	protected void handleMouseClicked(MouseArgs arg0) {}
	@Override
	protected void handleMouseDragged(MouseArgs arg0) {}
	@Override
	protected void handleMouseEntered(MouseArgs arg0) {}
	@Override
	protected void handleMouseExit(MouseArgs arg0) {}
	@Override
	protected void handleMouseMoved(MouseArgs arg0) {}
	@Override
	protected void handleMousePressed(MouseArgs arg0) {}
	@Override
	protected void handleMouseReleased(MouseArgs arg0) {}
	@Override
	protected void handleMouseWheel(MouseArgs arg0) {}
	@Override
	protected void handleSizeChanged() {}
	@Override
	protected void handleTextChanged() {}
	
	// === Public methods ===============================================
	
	@Override
	public String toString() {
		return "Desktop" + Message.hashString(this);
	}
	
	public Desktop(XGUI xgui, Graphics2D screenGfx) 
	{
		this.xgui = xgui;
		
		m_BL = m_BT = m_BR = m_BB = 10;
		
		this.screenGfx = screenGfx;
		
		cursor = Cursor.DEFAULT;
		
		m_lifeState = RTEnvObj_LifeState.Available;
	}

	public Integer getWindowCount() {
		return m_Controls.size();
	}
	
	/**
	 * returns the window with the specified index. windows are sorted by their z-value, which means
	 * that index 0 is bottom-most window, index 1 the one above it, and so on.
	 * @param idx index of the window to retrieve
	 * @return returns null if the index is not a valid window index.
	 */
	Window getWindow(int idx)
	{
		synchronized (m_Controls) {
			if (idx < 0 || idx >= m_Controls.size())
				return null;
			return (Window) m_Controls.get(idx);
		}
	}
	
	/**
	 * searches for and retrieves the next window with the given title (titles may not be unique)
	 * @param title The title of the window to search for
	 * @param previous null to search for the first matching window, or the previous value
	 * returned by this method, to search for the next occurence 
	 * @return returns a handle to the next matching window or null if none is found
	 */
	public Window getWindowByName(String title, Window previous)
	{
		String s = null;
		for (VisualComponent c : m_Controls)
			if ((s = c.getText()) != null && s.equals(title))
				if (previous == null)
					return (Window)c;
				else
					if (c == previous)
						previous = null;
					else
						continue;
		return null;
	}
	
	public Window getActiveWindow() {
		if (activeWindow != null && activeWindow.getLifeState().equals(RTEnvObj_LifeState.Available))
			return activeWindow;
		else
			return null;
	}

	public void setWallpaper(String attribute) {
		// TODO Auto-generated method stub
		
	}

	public void setActiveWindow(Window window) {
		activeWindow = window;		
	}
	
}
