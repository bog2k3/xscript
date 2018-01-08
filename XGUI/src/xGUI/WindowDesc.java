package xGUI;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

/**
 * Describes a window
 * @author bog
 *
 */
public class WindowDesc extends IScriptable
{
	public enum StartupPos
	{
		Default,
		Centered,
		User;
		
		@Override
		public String toString() {
			switch (this) {
			case Default:
				return "Default";
			case Centered:
				return "Centered";
			case User:
				return "User";
			default:
				return "Invalid";
			}
		};
		
		static StartupPos[] valArray = values();
	}
	
	public StartupPos startupPos = StartupPos.Default;
	
	/** user-defined startup x position */
	public Integer usrStartX = 0;
	/** user-defined startup y position */
	public Integer usrStartY = 0;
	
	/** window width */
	public Integer width = 320;
	/** window height */
	public Integer height = 240;
	
	public String title = null;
	
	public static int STARTUP_POS_DEFAULT = StartupPos.Default.ordinal();
	public static int STARTUP_POS_CENTERED = StartupPos.Centered.ordinal();
	public static int STARTUP_POS_USER = StartupPos.User.ordinal();
	public void setStartupPos(Integer enumIntValue) {
		startupPos = StartupPos.valArray[enumIntValue];
	}
	
	/**
	 * describes the creation attributes for a window
	 * @param app the owner application
	 * @param title window title
	 * @param width window width
	 * @param height window height
	 */
	public WindowDesc(String title, Integer width, Integer height)
	{
		this.title = title;
		this.width = width;
		this.height = height;
	}
	
	@Override
	public String toString() {
		return super.toString()+"[startupPos:"+startupPos+" usrX:"+usrStartX+" usrY:"+
		usrStartY+" width:"+width+" height:"+height+" title:\""+title+"\"]";
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties,
			MethodDesc[] Methods, String BaseClassName, boolean isAbstract) {
		// TODO check
		return true;
	}
}
