package wrappers;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public class Cursor extends IScriptable 
{
	public static final Cursor DEFAULT = new Cursor(java.awt.Cursor.DEFAULT_CURSOR);
	public static final Cursor HAND = new Cursor(java.awt.Cursor.HAND_CURSOR);
	public static final Cursor RESIZE_E = new Cursor(java.awt.Cursor.E_RESIZE_CURSOR);
	public static final Cursor RESIZE_W = new Cursor(java.awt.Cursor.W_RESIZE_CURSOR);
	public static final Cursor RESIZE_N = new Cursor(java.awt.Cursor.N_RESIZE_CURSOR);
	public static final Cursor RESIZE_S = new Cursor(java.awt.Cursor.S_RESIZE_CURSOR);
	public static final Cursor RESIZE_SE = new Cursor(java.awt.Cursor.SE_RESIZE_CURSOR);
	public static final Cursor RESIZE_SW = new Cursor(java.awt.Cursor.SW_RESIZE_CURSOR);
	public static final Cursor RESIZE_NE = new Cursor(java.awt.Cursor.NE_RESIZE_CURSOR);
	public static final Cursor RESIZE_NW = new Cursor(java.awt.Cursor.NW_RESIZE_CURSOR);
	public static final Cursor CROSSHAIR = new Cursor(java.awt.Cursor.CROSSHAIR_CURSOR);
	public static final Cursor MOVE = new Cursor(java.awt.Cursor.MOVE_CURSOR);
	public static final Cursor TEXT = new Cursor(java.awt.Cursor.TEXT_CURSOR);
	public static final Cursor WAIT = new Cursor(java.awt.Cursor.WAIT_CURSOR);
	
	public final java.awt.Cursor awtCursor;	
	private Cursor(int type) {
		awtCursor = java.awt.Cursor.getPredefinedCursor(type);
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
