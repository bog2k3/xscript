/*
 *	Test application
 *
 *	Author: 
 *			Bogdan Ionita
 *			<bog2k3@gmail.com>
 *
 *	Date:
 *			december, 2nd 2010
 */

#unit canvas_test

#import xsdk/xapp.xs
#import xsdk/xgui.xs
#import xsdk/mediaman.xs

/*class myClass
{
	float myF;
	int myI;
	bool[] myBs;
	
	myClass(float f1, int i1) {
		myF = f1;
		myI = i1;
	}
	
	float getF() { return myF; }
}*/

object<XApp> crtInstance;
object<Window> wnd1;
object<Canvas> canv;

object<WebImage> img1;

void move_right(object<Button> sender);
void move_left(object<Button> sender);

float m_alpha;
int m_x, m_y;

void keyTyped(object<VisualComponent> sender, object<KeyArgs> arg);
void keyPressed(object<VisualComponent> sender, object<KeyArgs> arg);

void timer1Tick(object<Timer> sender);

void paint();

void wnd1MouseMoved(object<VisualComponent> sender, object<MouseArgs> arg);
void wnd1MouseDown(object<VisualComponent> sender, object<MouseArgs> arg);
void wnd1MouseUp(object<VisualComponent> sender, object<MouseArgs> arg);

string initApp(object<XApp> app)
{
	int a, b; a = 5;
	b=20*(a+1)*30;
	app.log("b = "+str(b));
	
	
	
	crtInstance = app;
	
	wnd1 = app.createWindow(new object<WindowDesc>("please wait...", 300, 300));
	canv = new object<Canvas>(10, 10, 200, 160);
	wnd1.addControl(canv);
	object<Button> btn = new object<Button>(220, 45, 25, 25, "x++");
	wnd1.addControl(btn);
	btn.onClick = move_right;
	btn = new object<Button>(220, 10, 25, 25, "--x");
	wnd1.addControl(btn);
	btn.onClick = move_left;
	
	wnd1.onKeyTyped = keyTyped;
	wnd1.onKeyPressed = keyPressed;
	wnd1.onMouseMoved = wnd1MouseMoved;
	//daca vrei sa primesti eventuri de miscare si cand e un buton apasat de mouse, tre sa pui handler
	//pe onMouseDragged. poti sa pui aceeasi functie daca nu te intereseaza actiuni diferite.
	// ex: wnd1.onMouseDragged = wnd1MouseMoved;
	
	wnd1.onMousePressed = wnd1MouseDown;
	wnd1.onMouseReleased = wnd1MouseUp;
	
	m_alpha = 0;
	
	wnd1.show();
	
	img1 = _mediaManager.requestImage("images/joc dan/ball18.png");
	//img1 = _mediaManager.requestImage("images/mario.tga");
	//while (img1.getLoadState() != WebImage.STATE_LOADED) {}   // wait for the image to load. 
	// asta nu e modul bun, ca se blocheaza aici daca imaginea faileaza (file not found), normal trebuie pus un 
	// timer care sa verifice..
	// da acu stim noi sigur ca nu o sa faileze imaginea si merge si asa :-D
	
	wnd1.setText("canvas test");
	
	object<Timer> t1 = new object<Timer>(10);
	wnd1.addControl(t1);
	t1.onTick = timer1Tick;
	
	object<Window> wndDummy = app.createWindow(new object<WindowDesc>("Dummy win to test focus", 600, 230));
	//object<ScrollableView> sv = new object<ScrollableView>(wndDummy.getClientArea());
    object<ScrollableView> sv = new object<ScrollableView>(new object<Location>(0,0,200,200));
	wndDummy.addControl(sv);
	sv.addControl(new object<Button>(10,10,300,30,"But1"));
	sv.addControl(new object<Button>(10,310,100,30,"But2"));
    
    object<ListView> lv = new object<ListView>(201,0,400,200);
    wndDummy.addControl(lv);
    lv.addColumn("ID", 120);
    lv.addColumn("Name", 120);
    lv.addColumn("Description",200);
    lv.setRowHeight(25);
    //debug;
    string[] row = new string[3];
    row[0] = "1"; row[1] = "numele_1"; row[2] = "asta e primu item";
        lv.addRecord(1,row);
    row[0] = "2"; row[1] = "numele_2"; row[2] = "asta e al doilea item";
        lv.addRecord(2,row);
    
	wndDummy.show();
	
	/*
	wndDummy = app.createWindow(new object<WindowDesc>("Win7", 320,240)); wndDummy.show();
	wndDummy = app.createWindow(new object<WindowDesc>("Win8", 320,200)); wndDummy.show();
	wndDummy = app.createWindow(new object<WindowDesc>("Win9", 320,400)); wndDummy.show();
	*/
		
	return null;
}

void wnd1MouseMoved(object<VisualComponent> sender, object<MouseArgs> arg)
{
	//crtInstance.log("MouseMoved "+str(arg.x)+", "+str(arg.y));
	
	object<Location> locC = wnd1.screenToLocal(arg.x, arg.y);
	// in loc de wnd1.screenToLocal, pui controlu_care_vrei_coordonate_pentru.screenToLocal, iti converteste
	// in spatiul controlului respectiv, de exemplu canvas.screenToLocal.
	
	//crtInstance.log("		local coords: "+str(locC.x)+", "+str(locC.y));
}

void wnd1MouseDown(object<VisualComponent> sender, object<MouseArgs> arg)
{
	//crtInstance.log("MouseDown "+str(arg.button)+", "+str(arg.modifiers));
}

void wnd1MouseUp(object<VisualComponent> sender, object<MouseArgs> arg)
{
	//crtInstance.log("MouseUp "+str(arg.button)+", "+str(arg.modifiers));
}

void move_right(object<Button> sender)
{
	m_x += 5;
	paint();
}

void move_left(object<Button> sender)
{
	m_x -= 5;
	paint();
}

void keyTyped(object<VisualComponent> sender, object<KeyArgs> arg)
{
	wnd1.setText("ai apasat litera : "+arg.charStr);
}

void keyPressed(object<VisualComponent> sender, object<KeyArgs> arg)
{
	if (arg.keyCode == KeyArgs.KEY_LEFT)
		move_left(null);
		
	if (arg.keyCode == KeyArgs.KEY_RIGHT)
		move_right(null);
}

void paint()
{
	//debug; //debug forces a breakpoint :D
	
	canv.clear(Color.transparent);
	
	canv.setColor(Color.blue);
	canv.drawRectangle(20,20,160,120);
	
	//canv.drawImageScaled(img1, m_x, m_y, 46, 71);
	canv.drawImage(img1, m_x, m_y);
	canv.update();
	
	canv.setColor(Color.red);
	canv.drawText(10,15,"hahaha");
}

void timer1Tick(object<Timer> sender)
{
	m_alpha += Math.PI / 200;
	
	m_x = 70 + Math.round(50 * Math.cos(m_alpha));
	m_y = 50 + Math.round(50 * Math.sin(m_alpha));
	
	//crtInstance.log("m_x = "+Utils.float2str(m_x));
	
	paint();
}