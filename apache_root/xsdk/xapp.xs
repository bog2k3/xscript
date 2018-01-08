/*
 *	XAPP library for xapps.
 *  contains the basic application interface.
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */

#unit XAPP

#import xsdk/xgui.xs

external abstract class XApp
{
	bool enableLog;
	void log(string msg);
	
	object<Window> createWindow(object<WindowDesc> desc);
}

/** string utils */
external abstract class strut
{
	static string substr(string s, int start, int length);
}