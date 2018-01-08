import java.applet.Applet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

import kernel.LogLevel;
import kernel.MediaManager;
import kernel.MediaManager.WebImage;
import kernel.XKernel;

import stdApps.TaskManagerApp;
import wrappers.XMLDocument;
import wrappers.XMLNode;
import xGUI.Container;
import xGUI.KeyArgs;
import xGUI.Location;
import xGUI.MouseArgs;
import xGUI.VisualComponent;
import xGUI.Window;
import xGUI.WindowDesc;
import xGUI.XGUI;
import xGUI.controls.Button;
import xGUI.controls.Canvas;
import xGUI.controls.CheckBox;
import xGUI.controls.EditBox;
import xGUI.controls.Label;
import xGUI.controls.ListView;
import xGUI.controls.ScrollableView;
import xGUI.controls.Timer;
import xapp.AppLoader;
import xapp.XApp;


public class AppletMain extends Applet 
{
	private static final long serialVersionUID = 1L;
	
	private XKernel kernel;
	private XGUI xgui;
	private final int appLogChannel;
	
	private final int Width = 1024;
	private final int Height = 720;
	
	@Override 
	public void paint(Graphics arg0) 
	{
		if (xgui.isInitialized())
		{
			xgui.addDirtyRegion(null);
			while (!xgui.isUpToDate())
				Thread.yield();
		}
		else
		{
			Graphics2D gfx = (Graphics2D)arg0;
			gfx.setColor(Color.white);
			gfx.clearRect(0, 0, this.Width, this.Height);
			TextLayout tl = new TextLayout(
				"Sorry, the applet encountered an error and cannot continue.",
				gfx.getFont(),
				gfx.getFontRenderContext()
				);
			gfx.setColor(Color.black);
			tl.draw(gfx, 10, 10);
		}
	}
	
	@Override
	public String getAppletInfo() {
		return "x-GUI Test";
	}
	
	public AppletMain() 
	{
		kernel	 = new XKernel(this);
		kernel.setHttpRoot("http://ixro-bionita/");
		appLogChannel = kernel.registerLogChannel("APPLET");
		
		kernel.setGlobalLogLevel(LogLevel.Info);
		kernel.overrideLogChannelLevel("MEDIAMANAGER", LogLevel.Default);
		//kernel.overrideLogChannelLevel("KERNEL", LogLevel.Default);
		kernel.overrideLogChannelLevel("XGUI", LogLevel.Debug);
	}

	@Override
	public void init() {
	try {
		
		Thread.currentThread().setName("MainApplet");
		
		super.init();
		this.resize(this.Width,this.Height);
		
		if (!kernel.isInitialized) {
			kernel.log(appLogChannel, LogLevel.Error, "error: kernel could not initialize. aborting...");
			return;
		}
		
		xgui = new XGUI(kernel);
		xgui.enableLog = true;
		xgui.loadTheme("themes/default/default_theme.xml");
		
		// cream loaderul de aplicatii:
		AppLoader loader = new AppLoader(xgui);
		// inregistram toate clasele expuse inspre scripturi:
		loader.addClass(Location.class);
		loader.addClass(VisualComponent.class);
		loader.addClass(Container.class);
		loader.addClass(Window.class);
		loader.addClass(WindowDesc.class);
		loader.addClass(XGUI.class);
		loader.addClass(Button.class);
		loader.addClass(CheckBox.class);
		loader.addClass(EditBox.class);
		loader.addClass(Canvas.class);
		loader.addClass(XApp.class);
		loader.addClass(WebImage.class);
		loader.addClass(MediaManager.class);
		loader.addClass(wrappers.Color.class);
		loader.addClass(KeyArgs.class);
		loader.addClass(Timer.class);
		loader.addClass(wrappers.Math.class);
		loader.addClass(MouseArgs.class);
		loader.addClass(XMLDocument.class);
		loader.addClass(XMLNode.class);
		loader.addClass(Label.class);
		loader.addClass(ScrollableView.class);
		loader.addClass(ListView.class);
		// adaugam globale:
		loader.addGlobal("_mediaManager", kernel.getMediaManager(), true);
		
		
		XApp taskMan = new TaskManagerApp(xgui);
		taskMan.start();
		
		// acu incarcam o aplicatie si o rulam:
		XApp app1 = loader.loadApp("daBong.xs"); // complete url will be "http://%ROOT%/app1.xs"
		//XApp app1 = loader.loadApp("app_test.xs"); // complete url will be "http://%ROOT%/app1.xs"
		//XApp app1 = loader.loadApp("xmltest.xs"); // complete url will be "http://%ROOT%/app1.xs"
		if (app1 == null) {
			kernel.log(appLogChannel, LogLevel.Error, "Failed to load application");
			return;
		}
		app1.start();
		
		//XApp testApp = new CustomApp1(xgui, "appTest1");
		//testApp.start();
		
	} catch (Exception e) {
		kernel.log(appLogChannel, LogLevel.Error, "EXCEPTION thrown: "+XKernel.getStackTrace(e));
	}
	}
	
	@Override
	public void destroy() 
	{
		kernel.log(appLogChannel, LogLevel.Default, "shutting down applet...");
		xgui.shutDown();
		kernel.shutDown();
		kernel.log(appLogChannel, LogLevel.Default, "shutdown complete.");
		
		super.destroy();
	}

}
