package xGUI;

import wrappers.XMLNode;

public enum TextAlign 
{
	Left,
	Right,
	Center,
	Top,
	Bottom,
	
	Invalid;
	
	public static TextAlign fromString(String s) {
		if (s == null) 
			return Invalid;
		
		if (s.toLowerCase().equals("left"))
			return Left;
		if (s.toLowerCase().equals("right"))
			return Right;
		if (s.toLowerCase().equals("center"))
			return Center;
		if (s.toLowerCase().equals("top"))
			return Top;
		if (s.toLowerCase().equals("bottom"))
			return Bottom;
		
		return Invalid;
	}
	
	/** creates a TextAlign value from a named attribute contained in an XML node
	 * @param e XML Element containing attribute
	 * @param attr name of the attribute to read
	 * @param defaultValue default value to return if attribute does not exist or is invalid.
	 * @return
	 */
	public static TextAlign fromXMLAttrib(XMLNode n, String attr, TextAlign defaultValue) {
		String s = n.getAttribute(attr, null);
		TextAlign t = TextAlign.fromString(s);
		if (t==Invalid)
			return defaultValue;
		else
			return t;
	}
}
