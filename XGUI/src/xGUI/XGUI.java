package xGUI;
import java.applet.Applet;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import wrappers.Color;
import wrappers.XMLDocument;
import wrappers.XMLNode;

import app.IApplication;



import XScripter.Functor;
import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import kernel.LogLevel;
import kernel.MediaManager;
import kernel.XKernel;
import kernel.Messages.NotifyMsg;

/**
 * Use this class to manage the GUI
 * @author Bogdan Ionita
 */
public class XGUI extends IScriptable
{
	// ==== private stuff ============================================================
	
	private Applet app = null;
	private XKernel kernel = null;
	private final int guiLogChannel;
	
	private BufferedImage screenBuffer = null;
	private Graphics2D screenGfx = null;
	protected ArrayList<Rectangle> dirtyRgn = new ArrayList<Rectangle>();
	private boolean initialized = false;
	
	private Desktop desktop = null;
	
	private HashMap<String, IStyleLoader> hashStyles = new HashMap<String, IStyleLoader>();
	private XMLNode eThemeRoot = null;
	private String themePath = null;
	
	// default gui attributes:
	private Font defaultFont = null;
	private Color defaultTextColor = null;
	
	// default attributes getters
	public Font getDefaultFont() { return defaultFont; }
	public Color getDefaultTextColor() { return defaultTextColor; }
	
	public boolean enableLog = false;
	
	/**
	 * computes how much the given rectangle occludes other windows
	 */
	private double ComputeOcclusion(int x, int y, int w, int h)
	{
		double occ = 0;  
		for (int n=desktop.getWindowCount(), i=0; i<n; i++)
		{
			Location loc = desktop.getWindow(i).getLocation();
			if (loc.x >= x+w || loc.y >= y+h || loc.x+loc.w <= x || loc.y+loc.h <= y)
				continue; // not intersecting
			int x1 = Math.max(x, loc.x), x2 = Math.min(x+w, loc.x+loc.w);
			int y1 = Math.max(y, loc.y), y2 = Math.min(y+h, loc.y+loc.h);
			double delta = (x2-x1) * (y2-y1); // area occluded
			delta = delta / (loc.w*loc.h); // percentage of window area occluded
			delta = Math.pow(delta + 0.5, 2);
			occ += delta;
		}
		return occ;
	}
	
	/**
	 * finds the best location on the screen to position a window to.
	 * this is based on how much the window will occlude other windows.
	 * @param desc
	 * @return
	 */
	private Location GetBestLocation(WindowDesc desc) 
	{
		if (desktop.getWindowCount() == 0)
			return new Location(10, 10, 0, 0);
		
		int dw = desktop.getClientArea().w - 20; // desktop width
		int dh = desktop.getClientArea().h - 20; // desktop height
		
		int w = desc.width;
		int h = desc.height;
		double occTol = 0.5; // 20% of window area occlusion tolerance 
		int x = 0, y = 0;
		
		// compute best start position :
		Location loc = desktop.getWindow(desktop.getWindowCount()-1).getLocation();
		if (loc.x > dw - loc.x - loc.w) // if there's more room to the left than to the right
			x = Math.max(10, loc.x - w - 10);
		else
			x = Math.min(dw-w, loc.x+loc.w + 10);
		if (loc.y > dh - loc.y - loc.h)
			y = Math.max(10, loc.y - h - 10);
		else
			y = Math.min(dh-h, loc.y + loc.h + 10);
		
		double occ = ComputeOcclusion(x, y, w, h);
		
		// now sweep all directions to search for best location
		boolean foundBetter = false;
		int dx = Math.max(10, dw / 100);	// step size
		int dy = Math.max(10, dh / 100);	// step size
		do {
			foundBetter = false;
			
			if (occ < occTol)
				return new Location(x,y,0,0);
			
			// not yet in the tolerance area, try to improve:
			double occP, occN;
			// horizontal sweep:
			occP = (x <= dw - w - dx) ? ComputeOcclusion(x+dx,y,w,h) : Integer.MAX_VALUE;
			occN = (x >= dx) ? ComputeOcclusion(x-dx,y,w,h) : Integer.MAX_VALUE;
			if (occP < occN && occP < occ) {
				x += dx;
				occ = occP;
				foundBetter = true;
			}
			if (occN < occP && occN < occ) {
				x -= dx;
				occ = occN;
				foundBetter = true;
			}
			// vertical sweep:
			occP = (y <= dh - h - dy) ? ComputeOcclusion(x,y+dy,w,h) : Integer.MAX_VALUE;
			occN = (y >= dy) ? ComputeOcclusion(x,y-dy,w,h) : Integer.MAX_VALUE;
			if (occP < occN && occP < occ) {
				y += dy;
				occ = occP;
				foundBetter = true;
			}
			if (occN < occP && occN < occ) {
				y -= dy;
				occ = occN;
				foundBetter = true;
			}
			
		} while (foundBetter);
		return new Location(x,y,0,0);
	}
	
	// ==== internal stuff ============================================================
	
	/** this is called by the screen update thread whenever it is awakened by
	 * the kernel (when the message queue becomes empty) if there are dirty regions in the buffer. 
	 **/
	protected synchronized void Update()
	{
		Graphics2D gfx = (Graphics2D)app.getGraphics();
		Rectangle r = gfx.getClipBounds();
		if (isUpToDate())
			return;
		Rectangle2D r0 = dirtyRgn.get(0);
		for (int n = dirtyRgn.size(), i=1; i<n; i++)
			r0 = r0.createUnion(dirtyRgn.get(i));
		gfx.setClip(r0);
		paint(gfx);
		gfx.setClip(r);
		dirtyRgn.clear();
	}
	
	private void setDefaultAttributes() 
	{
		defaultFont = new Font("Arial",0,12);
		defaultTextColor = XColors.text;
	}
	
	// ==== public methods ============================================================
	
	public XKernel getKernel() { return kernel; }
	public Applet getApplet() { return app; }
	
	public void log(LogLevel lev, String message)
	{
		kernel.log(guiLogChannel, lev, message); 
	}
	
	public XGUI(XKernel kernel)
	{
		if (kernel == null || !kernel.isInitialized)
			throw new RuntimeException("XGUI : Kernel must be initialized first.");
		
		this.kernel = kernel;
		guiLogChannel = kernel.registerLogChannel("XGUI");
		
		log(LogLevel.Default, "initializing XGUI...");
		
		setDefaultAttributes();
			
		app = kernel.getApplet();
	
		//screenBuffer = new BufferedImage(app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
		screenBuffer = new BufferedImage(app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
		screenGfx = screenBuffer.createGraphics();
		
		desktop = new Desktop(this, screenGfx);
		desktop.init();
		desktop.show();
		
		kernel.registerMouseListener(desktop);
		kernel.registerMouseMotionListener(desktop);
		kernel.registerMouseWheelListener(desktop);
		kernel.registerKeyListener(desktop);
		
		app.setFocusable(true);
		
		kernel.onEnterIdle.addListener(new Functor() {
			
			@Override
			public synchronized void Execute(Object sender, Object... params) {
				Update();
			}
		});
		
		initialized = true;
		log(LogLevel.Default, "XGUI initialized.");
	}

	public synchronized void paint(Graphics arg0) 
	{
		// just copy our screen buffer to the given graphics object.
		arg0.drawImage(screenBuffer, 0, 0, app);
		
		// the actual drawing of the desktop happens whenever some object
		// triggers a paint request;
		// window content redrawing can only be triggered from within the window.
	}
	
	/** creates and returnes a handle to a window inside the GUI */
	public synchronized Window createWindow(IApplication app, WindowDesc desc)
	{
		if (app == null)
			return null;
		
		if (enableLog) log(LogLevel.Info, "creating Window from "+desc+"...");
		int sx = 0, sy = 0;
		switch (desc.startupPos) {
		case Centered:
			sx = desktop.getLocation().w / 2 - desc.width / 2;
			sy = desktop.getLocation().h / 2 - desc.height / 2;
			break;
		case Default:
			Location loc = GetBestLocation(desc);
			sx = loc.x; sy = loc.y;
			break;
		case User:
			sx = desc.usrStartX; sy = desc.usrStartY;
			break;
		}
		Location loc = new Location(sx, sy, desc.width, desc.height);
		Window w = new Window(desc.title, loc, app);
		desktop.addControl(w);
		if (enableLog) log(LogLevel.Info, "Window created.");
		
		return w;
	}

	public Desktop getDesktop() {
		return desktop;
	}
	
	/**
	 * returns true if the graphics are up-to-date, or false if an update is pending.
	 * @return
	 */
	public synchronized boolean isUpToDate()
	{
		return dirtyRgn.isEmpty();
	}
	
	/**
	 * returns the window with the specified index. windows are sorted by their z-value, which means
	 * that index 0 is tompost window, index 1 the one right underneath it, and so on.
	 * @param idx index of the window to retrieve
	 * @return returns null if the index is not a valid window index.
	 */
	public Window getWindow(Integer idx) {
		return desktop.getWindow(idx);
	}
	
	/**
	 * searches for and retrieves the next window with the given title (titles may not be unique)
	 * @param title The title of the window to search for
	 * @param previous null to search for the first matching window, or the previous value
	 * returned by this method, to search for the next occurence 
	 * @return returns a handle to the next matching window or null if none is found
	 */
	public Window getWindowByName(String title, Window previous) {
		return desktop.getWindowByName(title, previous);
	}
	
	public Boolean isInitialized() {
		return initialized;
	}
	
	public void loadTheme(String url) 
	{
		log(LogLevel.Default, "Loading theme from "+url);
		XMLDocument doc = getMediaMan().parseXML(url);
		if (doc == null) {
			log(LogLevel.Default, "Failed loading theme " + url);
			return;
		}
		
		setDefaultAttributes();
		
		int ibs = url.lastIndexOf("\\");
		int is = url.lastIndexOf("/");
		themePath = url.substring(0, Math.max(ibs, is)) + "/";		
		eThemeRoot = doc.rootNode;
		
		XMLNode e = eThemeRoot.getSubnode("wallpaper");
		if (e != null)
			desktop.setWallpaper(themePath + e.getAttribute("image", ""));
		
		e = eThemeRoot.getSubnode("font");
		XMLFontInfo xf = new XMLFontInfo(e, getDefaultFont(), getDefaultTextColor());
		defaultFont = xf.font;
		defaultTextColor = xf.color;
		
		e = eThemeRoot.getSubnode( "systemColors");
		if (e != null) {
			XColors.desktop = e.getColorAttr("desktop", Color.magenta);
			XColors.windowTitleActive = e.getColorAttr("activeTitleText", Color.magenta);
			XColors.windowTitleInactive = e.getColorAttr("inactiveTitleText", Color.magenta);
			XColors.windowBackground = e.getColorAttr("window", Color.magenta);
			XColors.editBackground = e.getColorAttr("editTextBackground", Color.magenta);
			XColors.selection = e.getColorAttr("selection", Color.magenta);
			XColors.selectionUnfocused = e.getColorAttr("selectionUnfocused", Color.magenta);
			XColors.caret = e.getColorAttr("textCaret", Color.magenta);
			XColors.disabledLight = e.getColorAttr("disabledShadeLight", Color.magenta);
			XColors.disabledMed = e.getColorAttr("disabledShadeMedium", Color.magenta);
			XColors.disabledDark = e.getColorAttr("disabledShadeDark", Color.magenta);
		}
		
		// load styles:
		Set<String> skeys = hashStyles.keySet(); 
		for (String sID : skeys) {
			e = eThemeRoot.getSubnode(sID);
			if (e != null) {
				hashStyles.get(sID).loadStyle(e, themePath);
			}
		}		
		
		desktop.requestPaint(desktop, desktop.m_loc);
	}
	
	public IStyleLoader getStyle(String styleID, Class<? extends IStyleLoader> class1) 
	{
		synchronized (hashStyles) {
			IStyleLoader style = hashStyles.get(styleID);
			if (style == null) {
				try {
					style = (IStyleLoader) class1.getConstructor(XGUI.class).newInstance(this);
					addStyle(styleID, style);
					hashStyles.put(styleID, style);
				} catch (Exception e) {
					log(LogLevel.Error, "Failed instantiating style "+class1+" "+XKernel.getStackTrace(e));
					return null;
				}
			}
			return style;
		}
	}
	
	public void addStyle(String styleid, IStyleLoader style) 
	{
		synchronized (hashStyles) {
			hashStyles.put(styleid, style);
		}
		
		if (eThemeRoot != null) {
			XMLNode e = eThemeRoot.getSubnode(styleid);
			if (e != null)
				style.loadStyle(e, themePath);
		}
	}
	
	public synchronized void shutDown() 
	{
		log(LogLevel.Default, "shutting down XGUI...");
		
		initialized = false;
		
		log(LogLevel.Default, "XGUI shutted down successfully.");
	}
	
	public void addDirtyRegion(Location loc) {
		if (loc == null)
			loc = new Location(0, 0, app.getWidth(), app.getHeight());
		dirtyRgn.add(new Rectangle(loc.x, loc.y, loc.w, loc.h));
		kernel.postMessage(new NotifyMsg(kernel, this));
	}
	
	private MediaManager m_MediaMan = null;
	public MediaManager getMediaMan() 
	{
		if (m_MediaMan == null)
			m_MediaMan = getKernel().getMediaManager();
		
		return m_MediaMan;
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties,
			MethodDesc[] Methods, String BaseClassName, boolean isAbstract) {
		return true; //TODO check
	}
	
	/** draws a frame.
	 * 
	 * @param gfx
	 * @param loc
	 * @param bkColor if !null, fills background with specified color
	 * @param raised true to draw the frame raised from the default plane, or false to make it
	 * inset to the surface.
	 */
	public void draw3DFrame(Graphics2D gfx, Location loc, Color bkColor, Boolean raised) 
	{
		gfx.setColor(raised ? XColors.frameLight2.awtClr : XColors.frameDark1.awtClr);
		gfx.drawLine(loc.x, loc.y, loc.x+loc.w, loc.y);
		gfx.drawLine(loc.x, loc.y, loc.x, loc.y+loc.h);
		gfx.setColor(raised ? XColors.frameLight1.awtClr : XColors.frameDark2.awtClr);
		gfx.drawLine(loc.x+1, loc.y+1, loc.x+loc.w-1, loc.y+1);
		gfx.drawLine(loc.x+1, loc.y+1, loc.x+1, loc.y+loc.h-1);
		gfx.setColor(raised ? Color.black.awtClr : XColors.frameLight1.awtClr);
		gfx.drawLine(loc.x+loc.w-1, loc.y+1, loc.x+loc.w-1, loc.y+loc.h-1);
		gfx.drawLine(loc.x+1, loc.y+loc.h-1, loc.x+loc.w, loc.y+loc.h-1);
		gfx.setColor(raised ? XColors.frameDark1.awtClr : XColors.frameLight2.awtClr);
		gfx.drawLine(loc.x+loc.w-2, loc.y+2, loc.x+loc.w-2, loc.y+loc.h-2);
		gfx.drawLine(loc.x+2, loc.y+loc.h-2, loc.x+loc.w-2, loc.y+loc.h-2);
		
		if (bkColor != null) {
			gfx.setColor(bkColor.awtClr);
			gfx.fillRect(loc.x+2, loc.y+2, loc.w-4, loc.h-4);
		}
	}

}
