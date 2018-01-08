/*
 *	MOUSEEVENT library for xapps.
 *  contains mouse event argument class and mouse codes
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			january, 5th 2011
 */
 
 #unit MOUSEEVENT

external abstract class MouseArgs
{
	/*
	 * Use these constants with the checkModifier(...) method
	 *		if (checkModifier(BUTTON3_DOWN_MASK)) ...
	 * or check manualy like this:
	 * 		if (modifiers & BUTTON3_DOWN_MASK != 0) ...
	 */
	static readonly int BUTTON1_DOWN_MASK;
	static readonly int BUTTON2_DOWN_MASK;
	static readonly int BUTTON3_DOWN_MASK;
	
	static readonly int SHIFT_DOWN_MASK;
	static readonly int CTRL_DOWN_MASK;
	static readonly int ALT_DOWN_MASK;
	static readonly int ALTG_DOWN_MASK;
	
	/** coordinates are in virtual applet screen space (gui destkop space).
	 * use screenToParent or screenToClient to obtain local or parent-client coordinates from these.
	 */
	readonly int x, y;
	
	/** a combination of one ore more flags that indicates which buttons are pressed and which special
	 * keys were pressed at the time when the event occured.
	 */
	readonly int modifiers;
	
	/** indicates the mouse button that triggered the event.
	 * 1 - left, 2 - right, 3 - middle.
	 */
	readonly int button;
	
	readonly int wheelSteps;	
	
	/** the VisualComponent below mouse when the event happened */
	readonly object<> target;
	
	/** checks whether a special key was being held down, or which mouse buttons where down when the event occured.
	 * use the constants defined in this class to check for specific actions.
	 * param : one or more BUTTON_XXX or KEY_XXX flags (combine them with "|" - logical OR ). 
	 */
	bool checkModifier(int flags);
}