package utils;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

public class strut extends IScriptable 
{
	public static String substr(String s, Integer start, Integer length) {
		return s.substring(start, start+length);
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
