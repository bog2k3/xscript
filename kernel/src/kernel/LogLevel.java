package kernel;

/**
 * use these fields to set logging levels to predefined values, for global consistency
 * 
 * @author Bogdan.Ionita
 *
 */
public enum LogLevel 
{
	/** highest priority logging level, will always appear on log, regardless of the global
	 * logging level set. use this only to report fatal errors, that prevent the
	 * application from continuing.
	 **/
	Error,
	
	/** default logging level, use it only for relevant and non-reccuring information that
	 * should also be logged in the release version. */
	Default,
	
	/** weaker information level, use for stuff that is not very important, or should
	 * not appear in the release version. */
	Info,
	
	/** lowest priority, will only show on the log when logging level is set to Debug.
	 * use it for stuff that should not normally be placed in the log. */ 
	Debug;
	
	public boolean pass (LogLevel lev, LogLevel globalLevel) {
		if (lev == null)
			return this.ordinal() <= globalLevel.ordinal();
		else
			return this.ordinal() <= lev.ordinal();
	}
	
	@Override
	public String toString() {
		switch (this)
		{
		case Error: return "!ERROR!";
		case Default: return "";
		case Info: return "info";
		case Debug: return "debug";
		default: return "unknown_level";
		}
	}
}
