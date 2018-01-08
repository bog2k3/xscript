/*
 *	MediaManager library for xapps.
 *  use MediaManager class to retrieve media files from http.
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */

#unit MEDIAMANAGER

#import xsdk/xml.xs

external object<MediaManager> _mediaManager;

external abstract class WebImage
{
	static readonly int STATE_INPROGRESS;
	static readonly int STATE_LOADED;
	static readonly int STATE_ERRORED;
	
	int getLoadState();
	int getWidth();
	int getHeight();
}

external abstract class MediaManager
{
	string getAbsoluteURL(string partialURL);
	
	/* all functions use partial urls 
	(ex. "userfiles/data1.txt" is automatically converted to "http://ROOT_ADDRESS/userfiles/data1.txt")
	*/
	object<WebImage> requestImage(string url);
	object<XMLDocument> parseXML(string URL);
	object<XMLDocument> createXMLDocument();
	string readTextDoc(string URL);
	
	void saveXMLDocument(object<XMLDocument> doc, string url);
}
