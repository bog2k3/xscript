package xGUI;

import java.awt.event.KeyEvent;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public final class KeyArgs extends IScriptable
{
	// === MODIFIER FLAGS ==========================================================
	public final static int SHIFT_DOWN_MASK = KeyEvent.SHIFT_DOWN_MASK;
	public final static int CTRL_DOWN_MASK = KeyEvent.CTRL_DOWN_MASK;
	public final static int ALT_DOWN_MASK = KeyEvent.ALT_DOWN_MASK;
	public final static int ALTG_DOWN_MASK = KeyEvent.ALT_GRAPH_DOWN_MASK;
	
	// === VIRTUAL KEY CODES =======================================================
	public final static int KEY_ESC = KeyEvent.VK_ESCAPE;
	public final static int KEY_F1 = KeyEvent.VK_F1;
	public final static int KEY_F2 = KeyEvent.VK_F2;
	public final static int KEY_F3 = KeyEvent.VK_F3;
	public final static int KEY_F4 = KeyEvent.VK_F4;
	public final static int KEY_F5 = KeyEvent.VK_F5;
	public final static int KEY_F6 = KeyEvent.VK_F6;
	public final static int KEY_F7 = KeyEvent.VK_F7;
	public final static int KEY_F8 = KeyEvent.VK_F8;
	public final static int KEY_F9 = KeyEvent.VK_F9;
	public final static int KEY_F10 = KeyEvent.VK_F10;
	public final static int KEY_F11 = KeyEvent.VK_F11;
	public final static int KEY_F12 = KeyEvent.VK_F12;
	public final static int KEY_TILDE = KeyEvent.VK_BACK_QUOTE;
	public final static int KEY_1 = KeyEvent.VK_1;
	public final static int KEY_2 = KeyEvent.VK_2;
	public final static int KEY_3 = KeyEvent.VK_3;
	public final static int KEY_4 = KeyEvent.VK_4;
	public final static int KEY_5 = KeyEvent.VK_5;
	public final static int KEY_6 = KeyEvent.VK_6;
	public final static int KEY_7 = KeyEvent.VK_7;
	public final static int KEY_8 = KeyEvent.VK_8;
	public final static int KEY_9 = KeyEvent.VK_9;
	public final static int KEY_0 = KeyEvent.VK_0;
	public final static int KEY_Q = KeyEvent.VK_Q;
	public final static int KEY_W = KeyEvent.VK_W;
	public final static int KEY_E = KeyEvent.VK_E;
	public final static int KEY_R = KeyEvent.VK_R;
	public final static int KEY_T = KeyEvent.VK_T;
	public final static int KEY_Y = KeyEvent.VK_Y;
	public final static int KEY_U = KeyEvent.VK_U;
	public final static int KEY_I = KeyEvent.VK_I;
	public final static int KEY_O = KeyEvent.VK_O;
	public final static int KEY_P = KeyEvent.VK_P;
	public final static int KEY_A = KeyEvent.VK_A;
	public final static int KEY_S = KeyEvent.VK_S;
	public final static int KEY_D = KeyEvent.VK_D;
	public final static int KEY_F = KeyEvent.VK_F;
	public final static int KEY_G = KeyEvent.VK_G;
	public final static int KEY_H = KeyEvent.VK_H;
	public final static int KEY_J = KeyEvent.VK_J;
	public final static int KEY_K = KeyEvent.VK_K;
	public final static int KEY_L = KeyEvent.VK_L;
	public final static int KEY_Z = KeyEvent.VK_Z;
	public final static int KEY_X = KeyEvent.VK_X;
	public final static int KEY_C = KeyEvent.VK_C;
	public final static int KEY_V = KeyEvent.VK_V;
	public final static int KEY_B = KeyEvent.VK_B;
	public final static int KEY_N = KeyEvent.VK_N;
	public final static int KEY_M = KeyEvent.VK_M;
	public final static int KEY_MINUS = KeyEvent.VK_SUBTRACT;
	public final static int KEY_EQUALS = KeyEvent.VK_EQUALS;
	public final static int KEY_BACKSPACE = KeyEvent.VK_BACK_SPACE;
	public final static int KEY_TAB = KeyEvent.VK_TAB;
	public final static int KEY_CAPSLOCK = KeyEvent.VK_CAPS_LOCK;
	public final static int KEY_SHIFT = KeyEvent.VK_SHIFT;
	public final static int KEY_CTRL = KeyEvent.VK_CONTROL;
	public final static int KEY_ALT = KeyEvent.VK_ALT;
	public final static int KEY_WIN = KeyEvent.VK_WINDOWS;
	public final static int KEY_SPACE = KeyEvent.VK_SPACE;
	public final static int KEY_MENU = KeyEvent.VK_CONTEXT_MENU;
	public final static int KEY_ENTER = KeyEvent.VK_ENTER;
	public final static int KEY_SLASH = KeyEvent.VK_SLASH;
	public final static int KEY_BACKSLASH = KeyEvent.VK_BACK_SLASH;
	public final static int KEY_LEFTBRACE = KeyEvent.VK_BRACELEFT;
	public final static int KEY_RIGHTBRACE = KeyEvent.VK_BRACERIGHT;
	public final static int KEY_SEMICOLON = KeyEvent.VK_SEMICOLON;
	public final static int KEY_QUOTE = KeyEvent.VK_QUOTE;
	public final static int KEY_COMMA = KeyEvent.VK_COMMA;
	public final static int KEY_PERIOD = KeyEvent.VK_PERIOD;
	public final static int KEY_INSERT = KeyEvent.VK_INSERT;
	public final static int KEY_HOME = KeyEvent.VK_HOME;
	public final static int KEY_PAGEUP = KeyEvent.VK_PAGE_UP;
	public final static int KEY_PAGEDOWN = KeyEvent.VK_PAGE_DOWN;
	public final static int KEY_DELETE = KeyEvent.VK_DELETE;
	public final static int KEY_END = KeyEvent.VK_END;
	public final static int KEY_UP = KeyEvent.VK_UP;
	public final static int KEY_DOWN = KeyEvent.VK_DOWN;
	public final static int KEY_LEFT = KeyEvent.VK_LEFT;
	public final static int KEY_RIGHT = KeyEvent.VK_RIGHT;
	public final static int KEY_NUMLOCK = KeyEvent.VK_NUM_LOCK;
	public final static int KEY_NUMPADDIVIDE = KeyEvent.VK_DIVIDE;
	public final static int KEY_NUMPADMULTIPLY = KeyEvent.VK_MULTIPLY;
	public final static int KEY_NUMPADADD = KeyEvent.VK_ADD;
	public final static int KEY_NUMPAD1 = KeyEvent.VK_NUMPAD1;
	public final static int KEY_NUMPAD2 = KeyEvent.VK_NUMPAD2;
	public final static int KEY_NUMPAD3 = KeyEvent.VK_NUMPAD3;
	public final static int KEY_NUMPAD4 = KeyEvent.VK_NUMPAD4;
	public final static int KEY_NUMPAD5 = KeyEvent.VK_NUMPAD5;
	public final static int KEY_NUMPAD6 = KeyEvent.VK_NUMPAD6;
	public final static int KEY_NUMPAD7 = KeyEvent.VK_NUMPAD7;
	public final static int KEY_NUMPAD8 = KeyEvent.VK_NUMPAD8;
	public final static int KEY_NUMPAD9 = KeyEvent.VK_NUMPAD9;
	public final static int KEY_NUMPAD0 = KeyEvent.VK_NUMPAD0;


	// === MEMBERS ===================================================================	
	public final Integer modifiers;
	public final Integer keyCode;
	public final char keyChar;
	public final String charStr;
	
	private KeyArgs(int modifiers, int keyCode, char keyChar) {
		this.modifiers = modifiers;
		this.keyChar = keyChar;
		this.keyCode = keyCode;
		this.charStr = String.valueOf(keyChar);
	}
	
	public final static KeyArgs fromKeyEvent(KeyEvent e) {
		return new KeyArgs(e.getModifiersEx(), e.getKeyCode(), e.getKeyChar());
	}

	/** checks whether a special key was being held down when the event occured.
	 * use the constants defined in this class to check for specific actions.
	 * @param flags a combination of one or more xxx_DOWN flags (combine them with "|" - logical OR ). 
	 * @return 
	 */
	public final Boolean checkModifier(Integer flags) {
		return (modifiers & flags) == flags;
	}

	/** returns true if the inner key is a printable character */
	public Boolean isChar() {
		return (
			(keyChar >= 'a' && keyChar <= 'z')
			|| (keyChar >= 'A' && keyChar <= 'Z')
			|| (keyChar >= '0' && keyChar <= '9')
			|| keyChar == '`' || keyChar == '~'
			|| keyChar == '!'
			|| keyChar == '@'
			|| keyChar == '#'
			|| keyChar == '$'
			|| keyChar == '%'
			|| keyChar == '^'
			|| keyChar == '&'
			|| keyChar == '*'
			|| keyChar == '('
			|| keyChar == ')'
			|| keyChar == '-' || keyChar == '_'
			|| keyChar == '=' || keyChar == '+'
			|| keyChar == ' '
			|| keyChar == '\\' || keyChar == '|'
			|| keyChar == '[' || keyChar == '{'
			|| keyChar == ']' || keyChar == '}'
			|| keyChar == ';' || keyChar == ':'
			|| keyChar == '\'' || keyChar == '"'
			|| keyChar == ',' || keyChar == '<'
			|| keyChar == '.' || keyChar == '>'
			|| keyChar == '/' || keyChar == '?'
		);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
