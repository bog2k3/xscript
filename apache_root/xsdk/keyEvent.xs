/*
 *	KEYEVENT library for xapps.
 *  contains keyboard event argument class and virtual key codes
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */

#unit KEYEVENT

external abstract class KeyArgs
{
	// === MEMBERS ===================================================================	
	readonly int modifiers;
	readonly int keyCode;
	readonly string charStr;
	
	bool checkModifier(int flags);
	bool isChar();
	
	// === MODIFIER FLAGS ==========================================================
	static readonly int SHIFT_DOWN_MASK;
	static readonly int CTRL_DOWN_MASK;
	static readonly int ALT_DOWN_MASK;
	static readonly int ALTG_DOWN_MASK;
	
	// === VIRTUAL KEY CODES =======================================================
	static readonly int KEY_ESC;
	static readonly int KEY_F1;
	static readonly int KEY_F2;
	static readonly int KEY_F3;
	static readonly int KEY_F4;
	static readonly int KEY_F5;
	static readonly int KEY_F6;
	static readonly int KEY_F7;
	static readonly int KEY_F8;
	static readonly int KEY_F9;
	static readonly int KEY_F10;
	static readonly int KEY_F11;
	static readonly int KEY_F12;
	static readonly int KEY_TILDE;
	static readonly int KEY_1;
	static readonly int KEY_2;
	static readonly int KEY_3;
	static readonly int KEY_4;
	static readonly int KEY_5;
	static readonly int KEY_6;
	static readonly int KEY_7;
	static readonly int KEY_8;
	static readonly int KEY_9;
	static readonly int KEY_0;
	static readonly int KEY_Q;
	static readonly int KEY_W;
	static readonly int KEY_E;
	static readonly int KEY_R;
	static readonly int KEY_T;
	static readonly int KEY_Y;
	static readonly int KEY_U;
	static readonly int KEY_I;
	static readonly int KEY_O;
	static readonly int KEY_P;
	static readonly int KEY_A;
	static readonly int KEY_S;
	static readonly int KEY_D;
	static readonly int KEY_F;
	static readonly int KEY_G;
	static readonly int KEY_H;
	static readonly int KEY_J;
	static readonly int KEY_K;
	static readonly int KEY_L;
	static readonly int KEY_Z;
	static readonly int KEY_X;
	static readonly int KEY_C;
	static readonly int KEY_V;
	static readonly int KEY_B;
	static readonly int KEY_N;
	static readonly int KEY_M;
	static readonly int KEY_MINUS;
	static readonly int KEY_EQUALS;
	static readonly int KEY_BACKSPACE;
	static readonly int KEY_TAB;
	static readonly int KEY_CAPSLOCK;
	static readonly int KEY_SHIFT;
	static readonly int KEY_CTRL;
	static readonly int KEY_ALT;
	static readonly int KEY_WIN;
	static readonly int KEY_SPACE;
	static readonly int KEY_MENU;
	static readonly int KEY_ENTER;
	static readonly int KEY_SLASH;
	static readonly int KEY_BACKSLASH;
	static readonly int KEY_LEFTBRACE;
	static readonly int KEY_RIGHTBRACE;
	static readonly int KEY_SEMICOLON;
	static readonly int KEY_QUOTE;
	static readonly int KEY_COMMA;
	static readonly int KEY_PERIOD;
	static readonly int KEY_INSERT;
	static readonly int KEY_HOME;
	static readonly int KEY_PAGEUP;
	static readonly int KEY_PAGEDOWN;
	static readonly int KEY_DELETE;
	static readonly int KEY_END;
	static readonly int KEY_UP;
	static readonly int KEY_DOWN;
	static readonly int KEY_LEFT;
	static readonly int KEY_RIGHT;
	static readonly int KEY_NUMLOCK;
	static readonly int KEY_NUMPADDIVIDE;
	static readonly int KEY_NUMPADMULTIPLY;
	static readonly int KEY_NUMPADADD;
	static readonly int KEY_NUMPAD1;
	static readonly int KEY_NUMPAD2;
	static readonly int KEY_NUMPAD3;
	static readonly int KEY_NUMPAD4;
	static readonly int KEY_NUMPAD5;
	static readonly int KEY_NUMPAD6;
	static readonly int KEY_NUMPAD7;
	static readonly int KEY_NUMPAD8;
	static readonly int KEY_NUMPAD9;
	static readonly int KEY_NUMPAD0;	
}