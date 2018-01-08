package xGUI;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

import wrappers.Color;

import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import kernel.Event;
import kernel.Message;


/**
 * 
 * @author bog
 *
 */
public abstract class Container extends VisualComponent 
{
	// ==== Private members ==========================================
	
	protected ArrayList<VisualComponent> m_Controls = new ArrayList<VisualComponent>();
	
	/** border width for top, bottom, left, right */
	protected int m_BT = 1, m_BB = 1, m_BL = 1, m_BR = 1;
	
	private VisualComponent m_focused = null;
	
	protected Container() {
		clBackground = XColors.windowBackground;
		m_Events.add(onClientScroll = new Event(this));
	}

	// ==== internal stuff ===========================================
	
	/** paints the border of a container. coordinates are in parent's client space */
	protected void paintBorder(Graphics2D gfx)
	{
		gfx.setColor(clBorder.awtClr);
		gfx.fillRect(0, 0, m_loc.w, m_BT);
		gfx.fillRect(0, 0, m_BL, m_loc.h);
		gfx.fillRect(0, m_loc.h-m_BB,m_loc.w, m_BB);
		gfx.fillRect(m_loc.w-m_BR, 0, m_BR, m_loc.h);
	}
	
	protected Color clBorder = Color.gray75;
	public Color getBorderColor() { return clBorder; }
	public void setBorderColor(Color cl) { 
		clBorder = cl;
		repaint();
	}
	
	/** paints the background of the container. coordinate system is set in parent's client space */
	protected void paintBackground(Graphics2D gfx)
	{
		gfx.setColor(clBackground.awtClr);
		gfx.fillRect(m_BL, m_BT, m_loc.w-m_BL-m_BR, m_loc.h-m_BT-m_BB);
	}
	
	@Override
	protected void paint(Graphics2D gfx) 
	{
		// if this method was called, then the clip rectangle intersects the location of the 
		// current object
		
		Rectangle r = gfx.getClipBounds();
		
		// if clip area intersects border, paint border
		if (r.x < m_BL || r.y < m_BT ||
				r.x+r.width > m_loc.w - m_BR ||
				r.y+r.height > m_loc.h - m_BB)
		{
			paintBorder(gfx);
		}
		
		// if clip area intersects client area paint container background and children
		if (	r.x + r.width > 0 + m_BL &&
				r.y + r.height > 0 + m_BT &&
				r.x < 0 + m_loc.w - m_BR &&
				r.y < 0 + m_loc.h - m_BB ) 
		{
			Location client = getClientArea();
			setClip(gfx, m_BL, m_BT, client.w, client.h);
			paintBackground(gfx);
			restoreClip(gfx);
			
			paintChildren(gfx);
		}
	}
	
	protected void paintChildren(Graphics2D gfx)
	{
		// the graphic origin is set in local space
		// translate it into client space:
		gfx.translate(m_BL, m_BT);
		
		Rectangle r = gfx.getClipBounds();
		Location client = getClientArea();
		
		//draw children:
		synchronized (m_Controls) {
			for (int n = m_Controls.size(), i = 0; i<n; i++)
			{
				VisualComponent c = m_Controls.get(i);
				Location cT = c.m_loc.translate(m_clTranslateX, m_clTranslateY);
				if (c.m_visible && cT.intersectRect(r))
				{
					// limit clip area to the control location
					setClip(gfx, cT.limitToArea(client));
					gfx.translate(cT.x, cT.y);
					c.paint(gfx);
					gfx.translate(-cT.x, -cT.y);
					restoreClip(gfx);
				}
			}
		}
		
		// restore initial transform:
		gfx.translate(-m_BL, -m_BT);
	}
	
	@Override
	protected void handleSizeChanged()
	{
		//TODO: implement a layout scheme, and rearrange controls here
	}
	
	@Override
	protected void updateGfx(Graphics2D gfx) 
	{
		super.updateGfx(gfx);
		for (int i = m_Controls.size()-1; i>=0; i--)
		{
			m_Controls.get(i).updateGfx(gfx);
		}
	}
	
	public VisualComponent getFocused() { return m_focused; }
	public void setFocus(VisualComponent c) 
	{
		if (c == m_focused)
			return;
		
		VisualComponent cLast = m_focused;
		m_focused = c;
		if (cLast != null) {
			cLast.handleLostFocus();
		}
		if (m_focused != null) {
			m_focused.handleGetFocus();
		}
	}
	
	protected void setMouseCaptureOverride(VisualComponent comp) 
	{
		checkAttachedToParent();
		m_parent.setMouseCaptureOverride(comp);
	}
	
	@Override
	protected boolean handleKeyPressed(KeyArgs arg) 
	{
		onKeyPressed.fire(arg); // broadcast the event
		// do some stuff with the key
		
		// now we need to check the focused control and forward this event to it.
		if (m_focused != null)
			return m_focused.handleKeyPressed(arg);
		else
			return false;
	}
	
	@Override
	protected boolean handleKeyReleased(KeyArgs arg) 
	{
		onKeyReleased.fire(arg); // broadcast the event
		// do some stuff with the key 
		
		// now we need to check the focused control and forward this event to it.
		if (m_focused != null)
			return m_focused.handleKeyReleased(arg);
		else
			return false;
	}
	
	@Override
	protected boolean handleKeyTyped(KeyArgs arg) 
	{
		onKeyTyped.fire(arg); // broadcast the event
		// do some stuff with the key 
		
		// now we need to check the focused control and forward this event to it.
		if (m_focused != null)
			return m_focused.handleKeyTyped(arg);
		else
			return false;
	}
	
	@Override
	protected void handleLostFocus() 
	{
		onFocusLost.fire();
		if (m_focused != null)
			m_focused.handleLostFocus();
	}
	
	@Override
	protected void handleGetFocus() 
	{
		onFocus.fire();
		if (m_focused != null)
			m_focused.handleGetFocus();
	}
	
	protected int m_clTranslateX=0;
	protected int m_clTranslateY=0;
	
	// ==== Public methods ===========================================
	
	/**
	 * sets a translation for the contents of this container. this has the efect of
	 * shifting all sub-controls inside the container. 
	 */
	public void setClientTranslation(Integer tx, Integer ty) {
		m_clTranslateX = tx;
		m_clTranslateY = ty;
		onClientScroll.fire();
		repaint();
	}
	
	public Location getClientTranslation() { return new Location(m_clTranslateX, m_clTranslateY, 0,0); }
	
	@Override
	public String toString() {
		return "Container" + Message.hashString(this);
	}

	/**
	 * adds a control to the current container. if the control was already bound to another container,
	 * it will be removed from it.
	 * @return returns the handle to the newly added component
	 */
	public void addControl(VisualComponent c)
	{
		checkAttachedToParent();
		synchronized (m_Controls) {
			if (c.setParent(this))
			{
				c.setEventsToApp();
				c.suspendPaint();
				c.init();
				c.m_initialized = true;
				if (c.text != null && c.m_textLayout == null)
					c.setText(c.text);
				c.resumePaint(false);
				synchronized (m_Controls) { 
					m_Controls.add(c);
				}
				if (c.m_visible)
					requestPaint(c, c.m_loc);
				synchronized (c.m_lifeState)
				{
					c.m_lifeState = RTEnvObj_LifeState.Available;
				}
			}
		}
	}
	
	public void removeControl(VisualComponent c) 
	{
		synchronized (m_Controls) {
			if (m_Controls.contains(c))
			{
				if (c.m_visible)
					requestPaint(this, Location.clientToParent(c.m_loc, this));
				
				synchronized (c.m_lifeState)
				{
					c.m_lifeState = RTEnvObj_LifeState.Destroying;
				}
				if (c.m_lifeState != RTEnvObj_LifeState.Destroying)
					c.destroy();
				synchronized (c.m_lifeState)
				{
					c.m_lifeState = RTEnvObj_LifeState.Dead;
				}
				m_Controls.remove(c);
				c.m_parent = null;
			}
		}
	}
	
	/** gets the client area, in client coordinates */
	public Location getClientArea() { return new Location(0, 0, m_loc.w - m_BR - m_BL, m_loc.h - m_BB - m_BT); }

	/** brings the specified control on top of all others, in Z order */
	public void bringControlToTop(VisualComponent c) 
	{
		for (int n = m_Controls.size(), i=0; i<n; i++)
			if (m_Controls.get(i) == c) {
				if (i < n-1) {
					// shift all other foreground controls down by 1 unit:
					for (int j=i; j<n-1; j++)
						m_Controls.set(j, m_Controls.get(j+1));
					// now put the specified one on top:
					m_Controls.set(n-1, c);
				}
				requestPaint(this, Location.clientToParent(c.m_loc, this));
				break;
			}
	}
	
	/** moves the specified control one unit up in Z order */
	public void moveControlUp(VisualComponent c) {
		for (int n = m_Controls.size(), i=0; i<n; i++)
			if (m_Controls.get(i) == c) {
				if (i < n-1) {
					VisualComponent c1 = m_Controls.get(i+1);
					m_Controls.set(i+1, c);
					m_Controls.set(i, c1);
				}
				requestPaint(this, Location.clientToParent(c.m_loc, this));
				break;
			}
	}
	
	/** moves the specified control one unit down in Z order */
	public void moveControlDown(VisualComponent c) {
		for (int n = m_Controls.size(), i=0; i<n; i++)
			if (m_Controls.get(i) == c) {
				if (i > 0) {
					VisualComponent c1 = m_Controls.get(i-1);
					m_Controls.set(i-1, c);
					m_Controls.set(i, c1);
				}
				requestPaint(this, Location.clientToParent(c.m_loc, this));
				break;
			}
	}
	
	@Override
	public void destroy() 
	{
		synchronized (m_lifeState)
		{
			m_lifeState = RTEnvObj_LifeState.Destroying;
		}
		
		suspendPaint();
		hide();
		
		VisualComponent c;
		while (m_Controls.size()>0) {
			c = m_Controls.get(0);
			synchronized (c.m_lifeState)
			{
				c.m_lifeState = RTEnvObj_LifeState.Destroying;
			}
			c.destroy();
			m_Controls.remove(c);
			synchronized (c.m_lifeState)
			{
				c.m_lifeState = RTEnvObj_LifeState.Dead;
			}
		}
		
		m_parent.removeControl(this);
		
		synchronized (m_lifeState)
		{
			m_lifeState = RTEnvObj_LifeState.Dead;
		}
	}
	
	public final Event onClientScroll;

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
