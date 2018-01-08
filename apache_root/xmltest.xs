/*
 *	XML test application
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			january 6th, 2011
 */

#unit xml_test

#import xsdk/xapp.xs
#import xsdk/mediaman.xs
#import xsdk/xml.xs

object<XApp> _app;

void printNode(object<XMLNode> n, int level);

void dosome()
{
}

string initApp(object<XApp> app)
{
	//return null;
	
	_app = app;
	
	object<XMLDocument> doc = _mediaManager.parseXML("themes/default/default_theme.xml");
	
	if (doc == null) 
		return "Failed to load document.";
	
	_app.log(">>>loaded document: "+doc.documentURL);
	object<XMLNode> root = doc.rootNode;
	
	_app.log(">>>printing document...");
	printNode(root, 1);
	_app.log(">>>END of document.");
	
	_app.log(">>> creating document test.xml...");
	doc = _mediaManager.createXMLDocument();
	doc.rootNode = new object<XMLNode>("test_root");
	root = doc.rootNode;
	root.setAttribute("prenume","gheorghe");
	root.setAttribute("varsta", "95");
	
	object<XMLNode> n = new object<XMLNode>("child");
	n.setAttribute("greutate", "53");

	root.addChild(n);
	_mediaManager.saveXMLDocument(doc, "test.xml");
	
	return null;
}

void printNode(object<XMLNode> n, int level)
{
	string sTab = "";
	for (int i=0; i<level; i++;)
		sTab+="    ";
		
	string sTI = sTab+"    "; // tab for inner elements of the node
	string sTI2 = sTI+"    ";
		
	_app.log(sTab+"{node} "+n.name);
	_app.log(sTI+"{attributes}");
	
	string[] sAttr = n.getAllAttribNames();
	string[] sAttrV = n.getAllAttribValues();
	for (int i=0; i<length(sAttr); i++;)
		_app.log(sTI2+sAttr[i]+":"+sAttrV[i]);
		
	_app.log(sTI+"{children nodes}");
	object<XMLNode>[] ch = n.getAllSubnodesByTag(null);
	for (int i=0; i<length(ch); i++;)
		printNode(ch[i], level+2);
	
	_app.log(sTab+"{end node}");
}