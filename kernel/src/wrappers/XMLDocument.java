package wrappers;

import org.w3c.dom.Document;

import XScripter.IScriptable;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;

/**
 * XML tools
 * @author Bogdan.Ionita
 *
 */

public class XMLDocument extends IScriptable 
{
	public final String documentURL;
	public XMLNode rootNode;
	
	public Document doc; 
	
	public XMLDocument(Document d)
	{
		doc = d;
		documentURL = d.getDocumentURI();
		if (d.getDocumentElement() != null)
			rootNode = new XMLNode(d.getDocumentElement());
	}
	
	/**
	 * this method prepares the xml document to be saved, by updating it's native
	 * internal structure from the cached attributes and children links.
	 */
	public void prepareForSave() 
	{
		if (rootNode == null)
			return;
		
		rootNode.prepareForSave(this);
		
		if (doc.hasChildNodes())
			doc.replaceChild(doc.getFirstChild(), rootNode.getElement());
		else
			doc.appendChild(rootNode.getElement());
	}

	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}
}
