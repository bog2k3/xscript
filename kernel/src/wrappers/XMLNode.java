package wrappers;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

/**
 * XML tools
 * @author Bogdan.Ionita
 *
 */

public class XMLNode extends IScriptable 
{
	public final String name;
	
	private Element e;
	
	private HashMap<String, ArrayList<XMLNode>> children = new HashMap<String, ArrayList<XMLNode>>();
	private HashMap<String, String> attribs = new HashMap<String, String>();
	
	public XMLNode(String name)
	{
		this.name = name;
	}
	
	protected XMLNode(Element e) 
	{
		this.e = e;
		name = e.getNodeName();
	}

	public void addChild(XMLNode child)
	{
		getAllSubnodesByTag(child.name);
		getAllSubnodesByTag("*");
		
		// first add to the list of nodes with the same name:
		ArrayList<XMLNode> ch = children.get(child.name);
		if (ch != null) {
			if (!ch.contains(child))
				ch.add(child);
		} else {
			ch = new ArrayList<XMLNode>();
			ch.add(child);
			children.put(child.name, ch);
		}
		
		// now add to the list of all children
		ch = children.get("*");
		if (ch != null) {
			if (!ch.contains(child))
				ch.add(child);
		} else {
			ch = new ArrayList<XMLNode>();
			ch.add(child);
			children.put("*", ch);
		}
	}
	
	public void setAttribute(String attribute, String value)
	{
		attribs.put(attribute, value);
	}
	
	public String getAttribute(String attrib, String defaultValue)
	{
		if (attribs.containsKey(attrib))
			return attribs.get(attrib);
			
		if (e != null && e.hasAttribute(attrib)) {
			String str = e.getAttribute(attrib); 
			attribs.put(attrib, str);
			return str;
		} else
			return defaultValue;
	}
	
	public Integer getIntAttr(String attrib, Integer defaultValue)
	{
		String sProp = getAttribute(attrib, null);
		
		if (sProp == null)
			return defaultValue;
		
		try {
			return Integer.valueOf(sProp);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}
	
	public Boolean getBooleanAttr(String attrib, Boolean defaultValue)
	{
		String sProp = getAttribute(attrib, null);
		
		if (sProp == null)
			return defaultValue;
		
		try {
			return Boolean.valueOf(sProp);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}
	
	public Color getColorAttr(String attrib, Color defaultValue)
	{
		String sProp = getAttribute(attrib, null);
		
		if (sProp == null)
			return defaultValue;
		
		try {
			return Color.decode(sProp);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}
	
	/** returns a list of the names of all attributes of this node */
	public String[] getAllAttribNames()
	{
		if (e != null) {
			NamedNodeMap nm = e.getAttributes();
			for (int i=0, n=nm.getLength(); i<n; i++) 
			{
				Node prop = nm.item(i);
				String pname = prop.getNodeName();
				if (!attribs.containsKey(pname))
					attribs.put(pname, prop.getNodeValue());
			}
		}
		
		String[] attrNames = new String[attribs.size()];
		return attribs.keySet().toArray(attrNames);
	}
	
	/** returns a list of the values of all attributes of this node */
	public String[] getAllAttribValues()
	{
		if (e != null) {
			NamedNodeMap nm = e.getAttributes();
			for (int i=0, n=nm.getLength(); i<n; i++) 
			{
				Node prop = nm.item(i);
				String pname = prop.getNodeName();
				if (!attribs.containsKey(pname))
					attribs.put(pname, prop.getNodeValue());
			}
		}
		
		String[] attrValues = new String[attribs.size()];
		return attribs.values().toArray(attrValues);
	}

	public XMLNode getSubnode(String tag)
	{
		XMLNode[] ch = getAllSubnodesByTag(tag);
		if (ch.length == 0)
			return null;
		else
			return ch[0];
	}
	
	/**
	 * returns a list of all child nodes with the given name, or all child nodes, if tag is null
	 * @param tag a name or null to retrieve all.
	 * @return
	 */
	public XMLNode[] getAllSubnodesByTag(String tag)
	{
		ArrayList<XMLNode> c = null;
		
		if (tag == null)
			tag = "*";
		
		if (children.containsKey(tag))
			c = children.get(tag);
		else if (e != null)
		{
			NodeList nl = e.getElementsByTagName(tag);
	
			c = new ArrayList<XMLNode>();
			for (int i=0; i<nl.getLength(); i++)
				if (nl.item(i) != null && ((Element)nl.item(i).getParentNode() == e))
					c.add(new XMLNode((Element)nl.item(i)));
			
			children.put(name, c);
		}
		
		if (c == null)
			return new XMLNode[0];
		
		XMLNode[] ca = new XMLNode[c.size()];
		return c.toArray(ca);
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

	/**
	 * this method prepares the xml node to be saved, by updating it's native
	 * internal structure from the cached attributes and children links.
	 */
	protected void prepareForSave(XMLDocument xmlDoc) 
	{
		if (e == null)
			e = xmlDoc.doc.createElement(name);
		
		getAllAttribNames();
		for (String attr : attribs.keySet())
			e.setAttribute(attr, attribs.get(attr));
		
		getAllSubnodesByTag("*");
		ArrayList<XMLNode> nodes = children.get("*");
		if (nodes != null)
			for (XMLNode n : nodes) {
				n.prepareForSave(xmlDoc);
				e.appendChild(n.getElement());
			}
	}

	protected Node getElement() {
		return e;
	}
}
