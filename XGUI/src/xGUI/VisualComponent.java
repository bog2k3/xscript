package xGUI;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import wrappers.Color;
import wrappers.Cursor;
import wrappers.XMLNode;

import app.IApplication;
import app.IRuntimeEnvObject;
import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;


import kernel.Event;
import kernel.LogLevel;
import kernel.MediaManager;
import kernel.XKernel;
import kernel.MediaManager.ResourceState;
import kernel.MediaManager.WebImage;


/**
 * Represents the ancestor of all windows and controls in the X-GUI system.
 * 
 * @author bog
 */
public abstract class VisualComponent extends IScriptable implements ImageObserver, IRuntimeEnvObject 
{
	// ==== Private members ==========================================
	
	protected RTEnvObj_LifeState m_lifeState;
	@Override
	public RTEnvObj_LifeState getLifeState() { synchronized(m_lifeState) {return m_lifeState; }}
	
	protected HashMap<Long, Integer> m_paintSuspendCounters = new HashMap<Long, Integer>();
	protected HashMap<Long, ArrayList<Rectangle>> updateRgn = new HashMap<Long, ArrayList<Rectangle>>();
	private Stack<Rectangle> m_ClipStack = new Stack<Rectangle>();
	protected ArrayList<Location> cursorAreas = new ArrayList<Location>();
	protected ArrayList<Cursor> areaCursors = new ArrayList<Cursor>();
	
	protected Location init_loc;
	
	private void updateTextLayout() 
	{
		Graphics2D gfx = getGfx();
		if (gfx == null || text == null)
			return;
		if (fontInfo == null)
			setDefaultFont();
		m_textLayout = new TextLayout(text.isEmpty() ? " " : text, fontInfo.font, gfx.getFontRenderContext());
		m_recText = m_textLayout.getPixelBounds(gfx.getFontRenderContext(), 0, 0);
		handleTextChanged();
	}
	
	// ==== protected members ========================================
	
	protected Location m_loc = null;
	protected Container m_parent = null;
	
	protected String text = null;
	protected TextLayout m_textLayout = null;
	protected Rectangle2D m_recText = null;
	protected FontInfo fontInfo = null;
	
	protected boolean m_visible = false;
	private boolean m_enabled = true;
	protected boolean focusable = false;
	
	boolean m_initialized = false;
	public Boolean isInitialized() { return m_initialized; }
	
	protected Color clBackground = XColors.windowBackground;
	public Color getBackgroundColor() { return clBackground; }
	public void setBackgroundColor(Color cl) { 
		clBackground = cl;
		repaint();
	}
	
	protected VisualComponent()
	{
		m_lifeState = RTEnvObj_LifeState.Initializing;
		
		m_Events.add(onDestroy = new Event(this));
		m_Events.add(onFocus = new Event(this));
		m_Events.add(onFocusLost = new Event(this));
		m_Events.add(onKeyPressed = new Event(this));
		m_Events.add(onKeyReleased = new Event(this));
		m_Events.add(onMouseEnter = new Event(this));
		m_Events.add(onMouseExit = new Event(this));
		m_Events.add(onMouseMoved = new Event(this));
		m_Events.add(onMouseDragged = new Event(this));
		m_Events.add(onMouseWheel = new Event(this));
		m_Events.add(onMousePressed = new Event(this));
		m_Events.add(onMouseReleased = new Event(this));
		m_Events.add(onMouseClicked = new Event(this));
		m_Events.add(onKeyTyped = new Event(this));
		m_Events.add(onResize = new Event(this));
	}
	
	/**
	 * this is the owner application, the one that created this control
	 */
	protected IApplication ownerApp = null;
	
	/** propagates a request to paint the control passed as parameter up the hierarchy.
	 * this method is overriden by top-level objects, such as windows and the desktop, in order
	 * to a paint action back to the targeted control along with the graphics context required to paint.
	 * This method should only be called by the kernel, NEVER directly (use PaintMsg instead).
	 * @param loc location to clip drawing operations to (coordinates in control's parent's client space)
	 * @param control target control to paint
	 * @param threadID the id of the thread who initiated the paint request by posting a PaintMsg
	 */
	void invokePaint(Location loc, VisualComponent control, long threadID)
	{
		if ((!m_visible && control!=m_parent))
			return;
		
		if (updateRgn.get(threadID) == null)
			updateRgn.put(threadID, new ArrayList<Rectangle>());
		ArrayList<Rectangle> rgn = updateRgn.get(threadID);
		
		if (control == this) {
			if (isPaintSuspended(threadID)) {
				if (loc == null) {
					return;
				}
				rgn.add(new Rectangle(loc.x,loc.y,loc.w,loc.h));
				return;
			}
			
			if (loc == null) {
				if (rgn.isEmpty()) {
					return;
				}
				Rectangle2D r0 = rgn.get(0);
				for (int n = rgn.size(), i=1; i<n; i++)
					r0 = r0.createUnion(rgn.get(i));
				loc = new Location((int)r0.getX(), (int)r0.getY(), (int)r0.getWidth(), (int)r0.getHeight());
			}
		}
		
		if (m_parent != null)
		{
			// location coordinates are in the given control's parent's client space
			m_parent.invokePaint(loc, control, threadID);
		} else {
			// this should be the desktop case, which is top-level and has own graphics.
			setClip(getGfx(), loc);
			paint(getGfx());
			restoreClip(getGfx());
		}
	}
	
	protected boolean isPaintSuspended(Long threadID) {
		Integer cnt = m_paintSuspendCounters.get(threadID);
		return cnt != null && cnt > 0; 
	}
	
	/** override this method to paint the control. The graphic origin is set to the location of 
	 * the object, such that (0,0) is the upper-left corner of the control. The clip area is guaranteed not
	 * to exceed the control's boundaries, but may be smaller.
	 **/
	protected abstract void paint(Graphics2D gfx);
	
	// ==== Internal stuff ===========================================
	
	/**
	 * use this to initialize all context-sensitive data, since the constructor runs without
	 * context and all actions that require one will fail at construction time.
	 */
	protected abstract void init();
	
	/**
	 * this method is only called internaly when a control is added to a container.
	 * @param parent
	 */
	boolean setParent(Container parent) {
		if (m_parent != null)
			return false;
		m_parent = parent;
		if (!(m_parent instanceof Desktop))
			ownerApp = m_parent.ownerApp;
		return true;
	}
	
	/** saves the current clipping area and sets a new one.
	 * the new clipping area is intersected with the current one, so it cannot exceed it
	 */
	final protected void setClip(Graphics2D gfx, int x, int y, int w, int h)
	{
		Rectangle crtClip = gfx.getClipBounds();
		m_ClipStack.push(crtClip);
		
		// intersect the requested rectangle with the current one
		int left = Math.max(x, crtClip.x);
		int top = Math.max(y, crtClip.y);
		int right = Math.min(x+w,crtClip.x+crtClip.width);
		int bottom = Math.min(y+h,crtClip.y+crtClip.height);
		// set the new clip area
		gfx.setClip(left,top,Math.max(0, right-left),Math.max(0, bottom-top));
	}
	
	/** saves the current clipping area and sets a new one.
	 * the new clipping area is intersected with the current one, so it cannot exceed it
	 */
	final protected void setClip(Graphics2D gfx, Location loc) {
		setClip(gfx, loc.x, loc.y, loc.w, loc.h);
	}
	
	/** restores the previous clipping area saved by the setClip method */
	final protected void restoreClip(Graphics2D gfx)
	{
		if (!m_ClipStack.isEmpty()) {
			Rectangle clip = m_ClipStack.pop();
			gfx.setClip(clip);
		}
	}
	
	private Graphics2D m_GFX = null;
	protected Graphics2D getGfx() {
		if (m_GFX == null && m_parent != null) 
			m_GFX = m_parent.getGfx();
		return m_GFX;
	}
	protected void updateGfx(Graphics2D gfx)
	{
		m_GFX = gfx;
	}

	// === Behaviour ============================================================================
	
	/**
	 * this gets triggered when size is modified externaly by calling setLocation. 
	 * it gives a chance to rearrange contents and layout before paint
	 */
	protected void handleSizeChanged() {
		onResize.fire();
	}
	
	/** this gets triggered when the text of the object changes, right before it is repainted */
	protected void handleTextChanged() {}
	
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseDragged(MouseArgs arg0) {
		onMouseDragged.fire(arg0);
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseMoved(MouseArgs arg0) {
		onMouseMoved.fire(arg0);
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseClicked(MouseArgs arg0) {
		onMouseClicked.fire(arg0);
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMousePressed(MouseArgs arg0) {
		onMousePressed.fire(arg0);
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseReleased(MouseArgs arg0) {
		onMouseReleased.fire(arg0);
	}
	
	/** override this to receive mouse wheel events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseWheel(MouseArgs arg0) {
		onMouseWheel.fire(arg0);
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseExit(MouseArgs arg0) {
		onMouseExit.fire();
	}
	
	/** override this to receive mouse events. Mouse position is in screen coordinates,
	 * use screenToClient() to convert them to client coordinates. 
	 * @param arg0 event description
	 */
	protected void handleMouseEntered(MouseArgs arg0) {
		onMouseEnter.fire();
	}
	
	/** this is called when the control loses keyboard focus. */
	protected void handleLostFocus() {
		onFocus.fire();
	}
	/** this is called when the control captures keyboard focus. */
	protected void handleGetFocus() {
		onFocusLost.fire();
	}
	
	protected boolean handleKeyPressed(KeyArgs arg) {
		onKeyPressed.fire(arg);
		return false; 
	}
	protected boolean handleKeyReleased(KeyArgs arg) {
		onKeyReleased.fire(arg);
		return false; 
	}
	protected boolean handleKeyTyped(KeyArgs arg) {
		onKeyTyped.fire(arg);
		return false; 
	}
	
	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w,
			int h)
	{
		int errFlags = ERROR | ABORT;
		if ((infoflags & errFlags) != 0) {
			if (logging()) log(LogLevel.Default, "Error loading image: "+img);
			getKernel().getMediaManager().setImageState(img, ResourceState.Errored);
			requestPaint(this, m_loc);
			return false;
		}
		if ((infoflags & ALLBITS) == ALLBITS) {
			getKernel().getMediaManager().setImageState(img, ResourceState.Loaded);
			requestPaint(this, m_loc);
			return false;
		}
		
		return true;
	}
	
	/** override this to be notified when the associated style has been updated, so 
	 * the control can update it's properties based on the style. a repaint is automatically
	 * issued after this call.
	 */
	protected abstract void updateFromStyle();
		
	// ==== Public stuff ==========================================
	
	/** this defines a custom mouse cursor for this control. if it's null, the default
	 * cursor is used instead. 
	 */
	public Cursor cursor = null;
	
	/**
	 * draws an image to x, y and default scaling of (1.0, 1.0)
	 */
	protected final void drawImage(WebImage img, int x, int y) {
		drawImage(img, x, y, -1, -1);
	}
	
	/**
	 * draws image img at position (x,y) scaling it to (w,h) dimensions.
	 * if either w or h is -1, the dimensions are taken from the image.
	 */
	protected final void drawImage(WebImage img, int x, int y, int w, int h)
	{
		Graphics2D gfx = getGfx();
		drawImageEx(gfx, img, x, y, w, h);
	}
	
	/**
	 * draws image img at position (x,y) on the specified graphics context, 
	 * scaling it to (w,h) dimensions.
	 * if either w or h is -1, the dimensions are taken from the image.
	 */
	protected final void drawImageEx(Graphics2D gfx, WebImage img, int x, int y, int w, int h)
	{
		ResourceState state = img.getState(); 
		if (state == ResourceState.Loaded || state == ResourceState.InProgress) {
			if (w >= 0 && h >= 0)
				gfx.drawImage(img.getImage(), x, y, w, h, Color.transparent.awtClr, this);
			else
				gfx.drawImage(img.getImage(), x, y, this);
			if (state == ResourceState.InProgress)
				gfx.drawImage(getKernel().getMediaManager().getLoadingIcon(), x+1, y+1, this);
		}
		
		if (state == ResourceState.Errored) {
			if (w >= 0 && h >= 0)
				gfx.drawImage(getKernel().getMediaManager().getErroredIcon(), x, y, w, h, Color.transparent.awtClr, this);
			else
				gfx.drawImage(getKernel().getMediaManager().getErroredIcon(), x, y, this);
		}
	}
	
	/**
	 * draws the control using the spcified images as skin components.
	 * corner images are not stretched.
	 * borders are stretched only along their direction (longitudinal, not transversal)
	 * middle stretch is stretched all over the middle of the control.
	 * @param ctl	corner-top-left
	 * @param bt	border-top
	 * @param ctr	corner_top_right
	 * @param br	border_right
	 * @param cbr	corner_bottom_right
	 * @param bb	border_bottom
	 * @param cbl	corner_bottom_left
	 * @param bl	border_left
	 * @param ms	middle_stretch
	 * @return		true if successful, or false if at least one of the images is null or not yet
	 * 				fully loaded (or errored).
	 */
	protected boolean drawSkin(WebImage ctl, WebImage bt, WebImage ctr,
			WebImage br, WebImage cbr, WebImage bb, WebImage cbl, WebImage bl,
			WebImage ms) 
	{
		if (getMediaMan().checkAllImagesLoaded(this, bl, br, bt, bb, ctl, ctr, cbl, cbr, ms)) {
			
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
			
			drawImage(ms, blw, bth, mw, mh);
			drawImage(ctl, 0, 0);
			drawImage(bt, ctlw, 0, mtw, bth);
			drawImage(ctr, ctlw+mtw, 0);
			drawImage(bl, 0, ctlh, blw, blh);
			drawImage(br, blw+mw, ctrh, brw, brh);
			drawImage(cbl, 0, ctlh+blh);
			drawImage(bb, cblw, bth+mh, bbw, bbh);
			drawImage(cbr, cblw+bbw, ctrh+brh);
			
			return true;			
		}
		
		return false;
	}
	
	public final void setLocation(Integer x, Integer y, Integer w, Integer h) {
		setLocation(new Location(x,y,w,h));
	}
	
	public final void setLocation(Location loc)
	{
		if (m_parent == null) {
			// desktop case, top-level
			if (m_loc == null || m_loc.w != loc.w || m_loc.h != loc.h) {
				m_loc = loc;				// different size
				handleSizeChanged();
			} else
				m_loc = loc;
			return;
		}
		
		m_parent.suspendPaint();
		if (m_loc != null)
		{
			m_parent.requestPaint(m_parent, Location.clientToParent(m_loc, m_parent));
		}
		suspendPaint();
		if (m_loc == null || m_loc.w != loc.w || m_loc.h != loc.h) {
			m_loc = loc;				// different size
			handleSizeChanged();
		} else
			m_loc = loc;
		resumePaint(true);
		m_parent.resumePaint(true);
	}
	public final Location getLocation() { return m_loc; }
	/** computes the screen coordinates of the current component (int applet's virtual screen space) */
	public Location getScreenLocation() {
		Location ploc = m_parent.getScreenLocation().translate(m_parent.m_BL+m_parent.m_clTranslateX, m_parent.m_BT+m_parent.m_clTranslateY);
		return new Location(m_loc.x + ploc.x, m_loc.y + ploc.y, m_loc.w, m_loc.h);
	}
	
	public final void setPosition(Integer x, Integer y) {
		setLocation(new Location(x, y, m_loc.w, m_loc.h));
	}
	
	public final void setPosition(Location pos) {
		setPosition(pos.x, pos.y);
	}
	
	private XGUI m_gui = null;
	protected XGUI getGUI()
	{
		if (m_gui == null)
			m_gui = m_parent.getGUI();
		
		return m_gui;
	}
	
	protected boolean logging() { return getGUI().enableLog; }
	
	protected void log(LogLevel lev, String msg)
	{
		getGUI().log(lev, this.toString()+": "+msg);
	}
	
	private MediaManager m_mediaMan = null;
	protected MediaManager getMediaMan()
	{
		if (m_mediaMan == null)
			m_mediaMan = getGUI().getMediaMan();
		
		return m_mediaMan;
	}
	
	private XKernel m_kernel = null;
	protected XKernel getKernel()
	{
		if (m_kernel == null)
			m_kernel = getGUI().getKernel();
		
		return m_kernel;
	}
	
	/** converts screen coordinates into local coordinates (into caller's space) */
	public final Location screenToLocal(Integer x, Integer y) {
		Location l = screenToParent(x,y);
		x = l.x - m_loc.x;
		y = l.y - m_loc.y;
		return new Location(x,y,0,0);
	}
	
	/** converts screen coordinates in local client coordinates
	 * override this if your client area is custom-defined. 
	 **/
	public Location screenToClient(Integer x, Integer y) {
		Location l = screenToParent(x,y);
		x = l.x - m_loc.x;
		y = l.y - m_loc.y;
		if (this instanceof Container) {
			x -= ((Container)this).m_BL + ((Container)this).m_clTranslateX;
			y -= ((Container)this).m_BT + ((Container)this).m_clTranslateY;
		}
		return new Location(x,y,0,0);
	}
	
	/** converts screen coordinates in current components's parent's client coordinates */ 
	public final Location screenToParent(Integer x, Integer y) {
		Container c = m_parent;
		while (c != null) {
			x -= c.m_loc.x + c.m_BL + c.m_clTranslateX;
			y -= c.m_loc.y + c.m_BT + c.m_clTranslateY;
			c = c.m_parent;
		}
		return new Location(x,y,0,0);
	}
	
	protected final void requestPaint(VisualComponent target, Location loc) {
		getKernel().postMessage(new PaintMsg(target, loc, this));
	}
	
	public final void show() {
		checkAttachedToParent();
		if (m_visible) 
			return;
		m_visible = true;
		requestPaint(this, m_loc);
	}
	
	protected void checkAttachedToParent() {
		if (m_parent == null) {
			throw new RuntimeException("Missing context, control needs to be attached to parent first!");
		}
	}

	public final void hide() {
		if (!m_visible) 
			return;
		m_visible = false;
		m_parent.requestPaint(m_parent, Location.clientToParent(m_loc, m_parent));
	}
	public final Boolean isVisible() { return m_visible; }
	
	public final void setEnabled(Boolean enabled) {
		checkAttachedToParent();
		if (m_enabled == enabled) 
			return;
		m_enabled = enabled;
		requestPaint(this, m_loc);
	}
	public Boolean isEnabled() { checkAttachedToParent(); return m_enabled && m_parent.isEnabled(); }
	
	public final Boolean isFocusable() { return focusable; }
	public final void setFocusable(Boolean f) { focusable = f;}
	
	public final String getText() { return text; }
	public final void setText(String text)
	{
		checkAttachedToParent();
		
		if (text == null)
			text = "<null>";
		this.text = text;
		
		Graphics2D gfx = getGfx();
		if (gfx != null) {
			updateTextLayout();
			requestPaint(this, m_loc);
		}
	}
	
	public final void setFont(Font font) {
		if (font == null) {
			setDefaultFont();
			return;
		}
		fontInfo = new FontInfo(font, getGfx());
		updateTextLayout();
		requestPaint(this, m_loc);
	}
	
	protected final void setDefaultFont() {
		Graphics2D gfx = getGfx();
		if (gfx != null)
			setFont(getGfx().getFont());
	}
	
	public final Container getParent() { return m_parent; }
	
	/**
	 * temporarily suspends the painting of the control, by postponing paint requests 
	 * that are subsequently sent to this control. the subsequent requests are recorded
	 * in a buffer and are available for servicing at the end of the suspend cycle, when
	 * resumePaint is called. use this mechanism when a control is about tu issue multiple
	 * paint requests and you want to avoid painting the control multiple times, for optimization
	 * the whole paint requests will be handled in a single call at the end of resumPaint().
	 */
	public final void suspendPaint() { 
		getKernel().postMessage(new SuspendPaintMsg(this, this));
	}
	/**
	 * resumes the painting of the control, which means subsequent paint requests will
	 * be handled accordingly, and not postponed.
	 * @param performPendingRequests true to perform pending requests since the first 
	 * suspendPaint() was called. false to discard them and not repaint the control.
	 */
	public final void resumePaint(Boolean performPendingRequests) 
	{
		getKernel().postMessage(new ResumePaintMsg(this, this, performPendingRequests));
	}
	
	protected final void resolvePendingPaintReq(Long threadID)
	{
		invokePaint(null, this, threadID);
	}
	
	public final void focus() {
		checkAttachedToParent();
		m_parent.setFocus(this);
	}
	
	public Boolean isFocused() {
		checkAttachedToParent();
		return m_parent.getFocused() == this && m_parent.isFocused();
	}
	
	public final void captureMouse() {
		checkAttachedToParent();
		m_parent.setMouseCaptureOverride(this);
	}
	
	/**
	 * defines an area inside the control where the mouse cursor is set to the given value.
	 * @param loc - location
	 * @param cur - cursor to be set when the mouse enters the specified area.
	 * @return areaID. The ID of the area. use this in later calls to modifyCursorArea and removeCursorArea
	 */
	public final Integer addCursorArea(Location loc, Cursor cur)
	{
		int id = 0;
		while (id < cursorAreas.size() && cursorAreas.get(id) != null)
			id++;
		if (id == cursorAreas.size()) {
			cursorAreas.add(loc);
			areaCursors.add(cur);
		} else {
			cursorAreas.set(id, loc);
			areaCursors.set(id, cur);
		}
		
		return id;
	}
	
	/**
	 * modifies the area identified by the ID. you can specify only one of the two parameters to
	 * change (actual area location or cursor), or both of these parameters. if you want to leave
	 * one attribute unchanged, pass null for it.
	 * Example:
	 * 		- only modify location, keep current cursor:
	 * 			modifyCursorArea(id, newLocation, null);
	 * @param areaID - id of the area to modify
	 * @param loc - null to keep or location to set
	 * @param cur - null to keep or cursor to set.
	 */
	public final void updateCursorArea(Integer areaID, Location loc, Cursor cur)
	{
		if (areaID < 0 || areaID >= cursorAreas.size())
			return;
		
		if (loc != null)
			cursorAreas.set(areaID, loc);
		if (cur != null)
			areaCursors.set(areaID, cur);
	}
	
	/**
	 * removes the custom cursor area from the control. any further references to this areaId are invalid.
	 * @param areaID - id of area to remove
	 */
	public final void removeCursorArea(Integer areaID)
	{
		if (areaID < 0 || areaID >= cursorAreas.size())
			return;
		
		cursorAreas.set(areaID, null);
		areaCursors.set(areaID, null);
	}
	
	/**
	 * returns a cursor for the given point in local coordinates. if no cursor is defined
	 * for that point, returns null.
	 * The algorithm is as follows:
	 * 		1. if any custom area contains that point, the corresponding cursor is returned.
	 * 		2. if no area contains the point, the control's default cursor is returned.
	 * @param localPoint
	 * @return
	 */
	public final Cursor cursorForPoint(Location localPoint) 
	{
		for (int i=0, n=cursorAreas.size(); i<n; i++) {
			Location ca = cursorAreas.get(i);
			if (ca != null && ca.containsPoint(localPoint.x, localPoint.y))
				return areaCursors.get(i);
		}
		return cursor;
	}
	
	protected void repaint() {
		//TODO add area to repaint parameter ;) or null by default -> all area
		if (this instanceof Window)
			((Window)this).m_bNeedRepaint = true;
		requestPaint(this, m_loc);
	}
	
	private HashMap<String, Object> userDataMap = new HashMap<String, Object>();
	
	public final void setCustomProperty(String field, Object value) {
		userDataMap.put(field, value);
	}
	
	public final Object getCustomProperty(String field) {
		return userDataMap.get(field);
	}
	
	// ==== User Events =============================================
	
	public final ArrayList<Event> m_Events = new ArrayList<Event>();
	
	/**
	 * this is sent when the component is about to be destroyed.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onDestroy; //TODO (low) trigger
	
	/**
	 * triggers when the component is focused.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onFocus;
	
	/**
	 * triggers when the component loses focus.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onFocusLost;
	
	/**
	 * triggers when the control is resized, just before the paint is triggered, so
	 * the user has a chance to reorganize the contents of the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onResize;
	
	/**
	 * triggers when the mouse enters the area of this control.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onMouseEnter;
	
	/**
	 * triggers when the mouse exits the area of this control.
	 * @param VisualComponent sender - the control who triggered the event.
	 */
	public final Event onMouseExit;
	
	/**
	 * triggers when mouse is moved above the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event
	 */
	public final Event onMouseMoved;
	
	/**
	 * triggers when mouse is dragged (moved with a button down) above the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event
	 */
	public final Event onMouseDragged;
	
	/**
	 * triggers when a mouse button is pressed on the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event
	 */
	public final Event onMousePressed;
	
	/**
	 * triggers when a mouse button is released on the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event
	 */
	public final Event onMouseReleased;
	
	/**
	 * triggers when a mouse button is clicked on the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event
	 */
	public final Event onMouseClicked;
	
	/**
	 * triggers when the mouse wheel was rotated above the control.
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param MouseArgs arg - contains info about the event.
	 * the wheelSteps member of MouseArgs shows how many steps the wheel moved.
	 */
	public final Event onMouseWheel;
	
	/**
	 * triggers when a key is pressed and control has focus
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param KeyArgs arg - contains info about the event
	 */
	public final Event onKeyPressed;
	
	/**
	 * triggers when a key is released and control has focus
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param KeyArgs arg - contains info about the event
	 */
	public final Event onKeyReleased;
	
	/**
	 * triggers when a key is typed and control has focus
	 * @param VisualComponent sender - the control who triggered the event.
	 * @param KeyArgs arg - contains info about the event
	 */
	public final Event onKeyTyped;
	
	// === skin stuff =============================
	
	public static class SkinState
	{
		private HashMap<String, WebImage> hashElements;
		private Color textColor;
		private Font font;
		
		public WebImage getElement(String name) {
			return hashElements.get(name);
		}
		public Color getTextColor() { return textColor; }
		public Font getFont() { return font; }
		
		public SkinState (XMLNode xmlNode, String themeRootURL, XGUI gui)
		{
			hashElements = new HashMap<String, WebImage>();
			XMLNode[] nl = xmlNode.getAllSubnodesByTag("element");
			for (int i=0, len = nl.length; i<len; i++)
			{
				String imgPath = themeRootURL + nl[i].getAttribute("image", "missing_image_url");
				hashElements.put(nl[i].getAttribute("name", "missing_element_name"), gui.getMediaMan().requestImage(imgPath, null));
			}
			
			XMLNode eFont = xmlNode.getSubnode("font");
			XMLFontInfo xf = new XMLFontInfo(eFont, gui.getDefaultFont(), gui.getDefaultTextColor());
			font = xf.font;
			textColor = xf.color;
		}
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
	void setEventsToApp() {
		for (Event e : m_Events)
			e.setApp(ownerApp);
	}
}
