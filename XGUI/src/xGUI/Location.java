package xGUI;

import java.awt.Rectangle;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public class Location extends IScriptable 
{
	public final Integer x, y, w, h;
	
	public Location(Integer x, Integer y, Integer w, Integer h)
	{
		this.x = x; this.y = y;
		this.w = w; this.h = h;
	}

	@Override
	public String toString() {
		return "Location [x:"+x+" y:"+y+" w:"+w+" h:"+h+"]";
	}
	
	public Location(Location loc)
	{
		x = loc.x;
		y = loc.y;
		w = loc.w;
		h = loc.h;
	}


	/**
	 * converts local client coordinates to parent's client coordinates (from client-space to parent-space)
	 * @param loc client-space location
	 * @param clientArea client area defined in parent's space coordinates
	 */
	public static Location clientToParent(Location loc, Container parent) {
		return new Location(loc.x + parent.m_loc.x + parent.m_BL + parent.m_clTranslateX, loc.y + parent.m_loc.y + parent.m_BT + parent.m_clTranslateY, loc.w, loc.h);
	}

	Boolean containsPoint(int x, int y) {
		return x >= this.x && x < this.x+this.w && y >= this.y && y < this.y+this.h;
	}

	public Boolean intersectRect(Rectangle r) 
	{
		return
		x < r.x + r.width && 
		y < r.y + r.height &&
		x + w > r.x &&
		y + h > r.y;
	}


	public Location limitToArea(Location area) 
	{
		int left = Math.max(x, area.x);
		int top = Math.max(y, area.y);
		int right = Math.min(x+w,area.x+area.w);
		int bottom = Math.min(y+h,area.y+area.h);
		return new Location(left,top,Math.max(0,right-left),Math.max(0,bottom-top));
	}
	
	public Location limitToArea(Integer x, Integer y, Integer w, Integer h) 
	{
		int left = Math.max(this.x, x);
		int top = Math.max(this.y, y);
		int right = Math.min(this.x+this.w,x+w);
		int bottom = Math.min(this.y+this.h,y+h);
		return new Location(left,top,Math.max(0,right-left),Math.max(0,bottom-top));
	}


	public Location translate(Integer dx, Integer dy) {
		return new Location(x+dx, y+dy, w, h);
	}


	public Boolean intersects(Location loc) {
		return
		x < loc.x + loc.w && 
		y < loc.y + loc.h &&
		x + w > loc.x &&
		y + h > loc.y;
	}

	/** returns a new location with position set to 0,0 and the size of this location */
	public Location untranslate() {
		return new Location(0, 0, w, h);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
