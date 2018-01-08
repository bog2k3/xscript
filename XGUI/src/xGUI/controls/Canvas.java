package xGUI.controls;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import kernel.MediaManager.WebImage;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

import wrappers.Color;
import xGUI.Location;
import xGUI.VisualComponent;

public class Canvas extends VisualComponent 
{
	
	/** creates a new canvas at the specified position, with the given client size.
	 * actual canvas size (including border) will be slightly larger.
	 * @param loc.x - position
	 * @param loc.y - position
	 * @param loc.w - client width
	 * @param loc.h - client height
	 */
	public Canvas(Location loc) {
		init_loc = loc;
		m_visible = true;
	}
	
	/** creates a new canvas at the specified position, with the given client size.
	 * actual canvas size (including border) will be slightly larger.
	 * @param x - position
	 * @param y - position
	 * @param w - client width
	 * @param h - client height
	 */
	public Canvas(Integer x, Integer y, Integer w, Integer h) {
		this(new Location(x,y,w,h));
	}
	
	private int borderX = 2;
	private int borderY = 2;
	private BufferedImage imgBuffer;
	private Graphics2D imgGfx;
	
	@Override
	protected void init()
	{
		suspendPaint();
		setLocation(init_loc.x, init_loc.y, init_loc.w+2*borderX, init_loc.h+2*borderY);
		resumePaint(false);
	}
	
	@Override
	protected void handleSizeChanged() {
		if (imgBuffer != null)
			imgBuffer.flush();
		imgBuffer = new BufferedImage(m_loc.w-2*borderX, m_loc.h-2*borderY, BufferedImage.TYPE_INT_ARGB);
		if (imgGfx != null)
			imgGfx.dispose();
		imgGfx = imgBuffer.createGraphics();
		imgGfx.setBackground(Color.transparent.awtClr);
		imgGfx.clearRect(0, 0, imgBuffer.getWidth(), imgBuffer.getHeight());
	}
	
	/** converts screen coordinates in local client coordinates.
	 * we override it because we have custom client area (with custom border)
	 **/
	@Override
	public Location screenToClient(Integer x, Integer y) {
		Location l = screenToParent(x,y);
		x = l.x - m_loc.x - borderX;
		y = l.y - m_loc.y - borderY;
		return new Location(x,y,0,0);
	}

	@Override
	protected void paint(Graphics2D gfx) 
	{
		getGUI().draw3DFrame(gfx, m_loc.untranslate(), clBackground, false);
		synchronized (imgBuffer) {
			synchronized (imgGfx) {
				gfx.drawImage(imgBuffer, null, borderX, borderY);
			}
		}
	}
	
	@Override
	protected void updateFromStyle() {
		//canvas does not use styles
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
	
	// ------------------------------------------------------------------
	// drawing functions:
	
	/** updates the graphics after drawing on the backbuffer */
	public void update()
	{
		requestPaint(this, m_loc);
	}
	
	public void drawImage(WebImage img, Integer x, Integer y) 
	{
		synchronized (imgGfx) {
			drawImageEx(imgGfx, img, x, y, -1, -1);
		}
	}
	
	public void drawImageScaled(WebImage img, Integer x, Integer y, Integer w, Integer h) 
	{
		synchronized (imgGfx) {
			drawImageEx(imgGfx, img, x, y, w, h);
		}
	}
	
	public void setColor(Color color)
	{
		synchronized (imgGfx) {
			imgGfx.setColor(color.awtClr);
		}
	}
	
	public void drawText(Integer x, Integer y, String txt)
	{
		synchronized (imgGfx) {
			//TODO mai destept aici
			imgGfx.drawChars(txt.toCharArray(), 0, txt.length(), x, y);
		}
	}
	
	public void drawRectangle(Integer x, Integer y, Integer w, Integer h)
	{
		synchronized (imgGfx) {
			imgGfx.drawRect(x, y, w, h);
		}
	}
	
	public void fillRectangle(Integer x, Integer y, Integer w, Integer h)
	{
		synchronized (imgGfx) {
			imgGfx.fillRect(x, y, w, h);
		}
	}
	
	public void clear(Color color)
	{
		synchronized (imgGfx) {
			imgGfx.setBackground(color.awtClr);
			imgGfx.clearRect(0, 0, imgBuffer.getWidth(), imgBuffer.getHeight());
		}
	}

	@Override
	public void destroy() {
		if (imgGfx != null)
			imgGfx.dispose();
		imgGfx = null;
		if (imgBuffer != null)
			imgBuffer.flush();
		imgBuffer = null;
	}
}
