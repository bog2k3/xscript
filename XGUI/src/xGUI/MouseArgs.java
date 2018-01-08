package xGUI;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public final class MouseArgs extends IScriptable 
{
	public final static Integer BUTTON1_DOWN_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	public final static Integer BUTTON2_DOWN_MASK = MouseEvent.BUTTON2_DOWN_MASK;
	public final static Integer BUTTON3_DOWN_MASK = MouseEvent.BUTTON3_DOWN_MASK;
	
	public final static Integer SHIFT_DOWN_MASK = MouseEvent.SHIFT_DOWN_MASK;
	public final static Integer CTRL_DOWN_MASK = MouseEvent.CTRL_DOWN_MASK;
	public final static Integer ALT_DOWN_MASK = MouseEvent.ALT_DOWN_MASK;
	public final static Integer ALTG_DOWN_MASK = MouseEvent.ALT_GRAPH_DOWN_MASK;
	
	/** coordinates are in virtual applet screen space (gui destkop space).
	 * use screenToParent or screenToClient to obtain local or parent-client coordinates from these.
	 */
	public final Integer x, y;
	/** a combination of one ore more flags that indicates which buttons are pressed and which special
	 * keys were pressed at the time when the event occured.
	 */
	public final Integer modifiers;
	/** indicates the mouse button that triggered the event.
	 * 1 - left, 2 - right, 3 - middle.
	 */
	public final Integer button;
	
	public final Integer wheelSteps;	
	
	/** the control below mouse when the event happened */
	public final VisualComponent target;
	
	private MouseArgs(VisualComponent target, int x, int y, int button, int wheelSteps, int modifiers) {
		this.x = x;
		this.y = y;
		this.button = button;
		this.wheelSteps = wheelSteps;
		this.modifiers = modifiers;
		this.target = target;
	}
	
	public final static MouseArgs fromMouseEvent(VisualComponent target, MouseEvent e)
	{
		return new MouseArgs(target, e.getX(), e.getY(), e.getButton(), 0, e.getModifiersEx());
	}
	
	public final static MouseArgs fromMouseWheelEvent(VisualComponent target, MouseWheelEvent e)
	{
		return new MouseArgs(target, e.getX(), e.getY(), e.getButton(), e.getWheelRotation(), e.getModifiersEx());
	}
	
	/** checks whether a special key was being held down, or which mouse buttons where down when the event occured.
	 * use the constants defined in this class to check for specific actions.
	 * @param flags a combination of one or more BUTTON_XXX or KEY_XXX flags (combine them with "|" - logical OR ). 
	 * @return 
	 */
	public final Boolean checkModifier(Integer flags) {
		return (modifiers & flags) == flags;
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
