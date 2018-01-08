package wrappers;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public class Color extends IScriptable 
{
	public final Double a,r,g,b;
	
	public final java.awt.Color awtClr;
	
	public static final Color transparent = new Color(1.,0.,1.,0.);
	public static final Color black = new Color(0.,0.,0.,1.);
	public static final Color white = new Color(1.,1.,1.,1.);
	public static final Color gray25 = new Color(0.75,0.75,0.75,1.);
	public static final Color gray50 = new Color(0.5,0.5,0.5,1.);
	public static final Color gray75 = new Color(0.25,0.25,0.25,1.);
	public static final Color red = new Color(1.,0.,0.,1.);
	public static final Color green = new Color(0.,1.,0.,1.);
	public static final Color blue = new Color(0.,0.,1.,1.);
	public static final Color cyan = new Color(0.,1.,1.,1.);
	public static final Color magenta = new Color(1.,0.,1.,1.);
	public static final Color yellow = new Color(1.,1.,0.,1.);
	public static final Color orange = new Color(1.,0.5,0.,1.);
	
	public Color(Double r, Double g, Double b, Double a)
	{
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
		awtClr = new java.awt.Color(this.r.floatValue(),this.g.floatValue(),this.b.floatValue(),this.a.floatValue()); 
	}
	
	public Color(Integer r, Integer g, Integer b)
	{
		this(r/255.0, g/255.0, b/255.0, 1.0);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

	public static Color decode(String nm)
	{
		java.awt.Color clr = java.awt.Color.decode(nm);
		return new Color(clr.getRed() / 255.0, clr.getGreen() / 255.0, clr.getBlue() / 255.0, clr.getAlpha() / 255.0);
	}
}
