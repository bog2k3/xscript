/*
 *	XMLUtils library for xapps.
 *  use XML classes to access and manipulate xml trees loaded from xml files by MediaManager
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 27th 2010
 */
 
#unit XMLUtils

#import xsdk/kernel.xs

external class XMLDocument
{
	readonly string documentURL;
	object<XMLNode> rootNode;
}

external class XMLNode
{
	readonly string name;
	
	object<XMLNode>(string name);
	
	void addChild(object<XMLNode> child);
	void setAttribute(string attribute, string value);
	
	string			getAttribute	(string attrib, string			defaultValue);
	int				getIntAttr		(string attrib, int				defaultValue);
	bool			getBooleanAttr	(string attrib, bool			defaultValue);
	object<Color> 	getColorAttr	(string attrib, object<Color>	defaultValue);
	
	/** returns a list of the names of all attributes of this node */
	string[] getAllAttribNames();
	/** returns a list of the values of all attributes of this node */
	string[] getAllAttribValues();

	/** This returns the first subnode with the given name or the absolute first, regardless of the name if the 
	  * parameter is null, or the special value "*". */
	object<XMLNode> 	getSubnode(string tag);
	/** This returns a list of all subnodes. if tag is different from null and "*"
	 * only the subnodes with the given name are returned.*/
	object<XMLNode>[]	getAllSubnodesByTag(string tag);
}