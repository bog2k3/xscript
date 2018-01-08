/*
 *	XGUI library for xapps.
 *  contains classes for working with the graphical end-user interface.
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */

#unit XGUI

#import xsdk/kernel.xs
#import xsdk/mediaman.xs
#import xsdk/keyEvent.xs
#import xsdk/mouseEvent.xs

external class Location
{
	readonly int x, y, w, h;
	
	object<Location>(object<Location> loc);
	object<Location>(int x, int y, int w, int h);
	
	object<Location> translate(int dx, int dy);
	
	bool intersects(object<Location> loc);
	
	string toString();
}

external abstract class VisualComponent
{
	object<Container> getParent();
	
	object<Location> getLocation();
	object<Location> getScreenLocation();
	
	string getText();
	
	void hide();
	void show();
	
	bool isEnabled();
	bool isFocusable();
	bool isFocused();
	bool isVisible();
	
	object<Location> screenToClient(int x, int y);
	object<Location> screenToLocal(int x, int y);
	object<Location> screenToParent(int x, int y);
	
	void setEnabled(bool enabled);
	void setFocus();
	void setFocusable(bool focusable);
	void setFont(object<> font);
	void setText(string text);
	
	void setLocation(int x, int y, int w, int h);
	void setLocation(object<Location> loc);
	void setPosition(int x, int y);
	void setPosition(object<Location> pos);
	
	object<Color> getBackgroundColor();
	void setBackgroundColor(object<Color> cl);
	
	string toString();
	
	event onDestroy			(object<VisualComponent> sender);
	event onFocus			(object<VisualComponent> sender);
	event onFocusLost		(object<VisualComponent> sender);
	
	event onKeyPressed		(object<VisualComponent> sender, object<KeyArgs> arg);
	event onKeyReleased		(object<VisualComponent> sender, object<KeyArgs> arg);
	event onKeyTyped		(object<VisualComponent> sender, object<KeyArgs> arg);
	
	event onMouseClicked	(object<VisualComponent> sender, object<MouseArgs> arg);
	event onMouseDragged	(object<VisualComponent> sender, object<MouseArgs> arg);
	event onMouseMoved		(object<VisualComponent> sender, object<MouseArgs> arg);
	event onMouseEnter		(object<VisualComponent> sender);
	event onMouseExit		(object<VisualComponent> sender);
	event onMousePressed	(object<VisualComponent> sender, object<MouseArgs> arg);
	event onMouseReleased	(object<VisualComponent> sender, object<MouseArgs> arg);
	event onMouseWheel		(object<VisualComponent> sender, object<MouseArgs> arg);
}

external class Container : VisualComponent
{
	void addControl(object<VisualComponent> comp);
	void bringControlToTop(object<VisualComponent> comp);
	void moveControlDown(object<VisualComponent> comp);
	void moveControlUp(object<VisualComponent> comp);
	void removeControl(object<VisualComponent> comp);
	
	object<Location> getClientArea();
}

external class Window : Container
{
	void activate();
	void bringToTop();
	bool isActive();
}

external class WindowDesc
{
	static readonly int STARTUP_POS_DEFAULT;
	static readonly int STARTUP_POS_CENTERED;
	static readonly int STARTUP_POS_USER;
	void setStartupPos(int enumIntValue);
	
	int usrStartX;
	int usrStartY;
	int width;
	int height;
	string title;
	
	object<WindowDesc>(string title, int width, int height);
	
	string toString();
}

external abstract class XGUI 
{
	object<Desktop> getDesktop();
	object<Window> getWindowByName(string title, object<Window> previous);
	object<Window> getWindow(int index);
}

external class Desktop : Container
{
}

external class Label : VisualComponent
{
	object<Label>(string text, int x, int y, int w, int h);
	
	void setAutoSize(bool autosize);
	bool getAutoSize();
	
	static readonly int BORDER_NONE;
	static readonly int BORDER_SIMPLE;
	static readonly int BORDER_3D_IN;
	static readonly int BORDER_3D_OUT;
	int getBorderStyle();
	void setBorderStyle(int style);
}

external class Button : VisualComponent
{
	static readonly int BTNLAYOUT_TEXT;
	static readonly int BTNLAYOUT_IMAGELEFT;
	static readonly int BTNLAYOUT_IMAGERIGHT;
	static readonly int BTNLAYOUT_IMAGEABOVE;
	static readonly int BTNLAYOUT_IMAGEBELOW;
	static readonly int BTNLAYOUT_IMAGEUNDER;
	
	object<Button>(int x, int y, int w, int h);
	object<Button>(int x, int y, int w, int h, string name);
	
	event onClick(object<Button> sender);
	
	void setImage(string imageURL);
	void setLayout(int enumIntValue);
	
}

external class CheckBox : Button
{
	object<CheckBox>(int x, int y, string text);
	void setPadding(int padding);
	
	bool isChecked();
	void setChecked(bool value);
}

external class EditBox : VisualComponent
{
	object<EditBox>(int x, int y, int w);
	
	int getCaretPos();
	void setCaretPos(int pos, bool shiftDown);
	void moveCaret(int delta, bool shiftDown);
	void hideCaret();
	void showCaret();
}

external class Canvas : VisualComponent
{
	object<Canvas>(int x, int y, int w, int h);
	object<Canvas>(object<Location> loc);
	
	void clear(object<Color> color);
	
	void drawImage(object<WebImage> img, int x, int y);
	void drawImageScaled(object<WebImage> img, int x, int y, int w, int h);
	
	void setColor(object<Color> color);
	
	void drawText(int x, int y, string txt);
	void drawRectangle(int x, int y, int w, int h);
	
	void fillRectangle(int x, int y, int w, int h);
	
// nu sunt implementate:
	object<Color> getPixel(int x, int y);
	void putPixel(int x, int y, object<Color> color);
	
	void drawLine(int x1, int y1, int x2, int y2);
//----------------------
	
	void update(); // after painting, call this to update the graphics from the backbuffer
}

external class Timer : VisualComponent
{
	event onTick(object<Timer> sender);
	
	object<Timer>(int timeout);
	int getInterval();
	void setInterval(int interval);
}

external class ScrollableView : Container
{
	object<ScrollableView>(int x, int y, int w, int h);
	object<ScrollableView>(object<Location> loc);
	
	/** Forces scrollbars to always be visible (true), or leaves them up to the view to display (false). */
	void forceScrollbars(bool force);
	
	/** set the padding of the scrollbars : how many pixels to leave before the scrollbar(lead) and how many after (trail).
	* if this is not set (default 0,0), the scrollbars will use all the length of the view */
	void setHScrollbarPadding(int lead, int trail);
	void setVScrollbarPadding(int lead, int trail);
}

external class ListView : ScrollableView
{
    object<ListView>(int x, int y, int w, int h);
    
    void setRowHeight(int height);                  // set the height of all rows, in pixels
    void addColumn(string title, int width);        // the title appears in the header 
    void addRecord(int customID, string[] data);    // add a new record. customID is application-specific
                                                    // you can use customID to associate the entry with
                                                    // other app-specific data (such as DB entries)
    void clear();
    int[] getSelectedLines();                       // return an array with indices of all lines
                                                    // that are selected by the user
    int[] getSelectedIDs();                         // return an array with customIDs of all selected lines
    int getLineID(int index);                       // return the customID of the line at index
    void selectLines(int[] lineIDs);                // select lines based on app-specific customIDs
    
    
    event onSelectionChanged(object<ListView> sender);
}