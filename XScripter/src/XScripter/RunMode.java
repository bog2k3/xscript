package XScripter;

public enum RunMode 
{
	/**
	 * in blocking mode, the entire function is executed and then the script returns control to the caller.
	 */
	Blocking,
	/**
	 * the debug mode runs instructions step-by-step, in blocking mode.
	 * The user is required to call Script.Step() to execute one intruction at a time.
	 */
	Debug,
}
