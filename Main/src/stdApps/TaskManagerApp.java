package stdApps;

import app.IApplication;
import XScripter.Functor;
import xGUI.Location;
import xGUI.Window;
import xGUI.WindowDesc;
import xGUI.XGUI;
import xGUI.controls.Button;
import xGUI.controls.ListView;
import xGUI.controls.Timer;
import xapp.XApp;


public class TaskManagerApp extends XApp 
{
	public TaskManagerApp(XGUI xgui) {
		super(xgui, "Task Manager");
	}
	
	private Window wnd;
	private ListView list;
	private Button btnEndTask;
	private Button btnClose;
	private Timer timerUpdate;
	
	@Override
	public boolean init() 
	{
		wnd = xgui.createWindow(this, new WindowDesc("Task Manager", 300, 300));
		
		wnd.addControl(list = new ListView(0, 0, 1, 1));
		wnd.addControl(btnEndTask = new Button(0,0,60,25,"Kill app"));
			btnEndTask.setEnabled(false);
			btnEndTask.onClick.addListener(new Functor() {
				@Override
				public void Execute(Object sender, Object... params) {
					btnEndTask_onClick(sender);				
				}});
		wnd.addControl(btnClose = new Button(0,0,60,25,"Close"));
			btnClose.onClick.addListener(new Functor() {
				@Override
				public void Execute(Object sender, Object... params) {
					btnClose_onClick(sender);				
				}});
		wnd.onResize.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				wnd_onResize(sender);
			}});
		
		wnd.addControl(timerUpdate = new Timer(1000));
			timerUpdate.onTick.addListener(new Functor() {				
				@Override
				public void Execute(Object sender, Object... params) {
					update();
				}});
			timerUpdate.setEnabled(true);
		
		list.addColumn("Application", 160);
		list.addColumn("Path", 180);
		list.onSelectionChanged.addListener(new Functor() {
			@Override
			public void Execute(Object sender, Object... params) {
				updateSelection();
			}});
		
		wnd_onResize(null);
		update();
		wnd.show();
		
		return true;
	}
	
	protected void updateSelection() 
	{
		Integer[] sel = list.getSelectedLines();
		btnEndTask.setEnabled(sel.length > 0);
	}
	
	IApplication[] apps = null;

	protected void update() {
		apps = kernel.getAppList();
		Integer[] appID = list.getSelectedIDs();
		list.clear();
		
		list.suspendPaint();
		
		for (int i=0; i<apps.length; i++) {
			IApplication a = apps[i];
			String name = a.getUserDataField("NAME").toString();
			Object pathObj = a.getUserDataField("PATH");
			String path = pathObj != null ? pathObj.toString() : "N/A";
			list.addRecord(apps[i].getID(), new String[] {name, path});
		}
		list.addRecord(-1, new String[] {"-------------", "-------------"});
		list.addRecord(-2, new String[] {"beware", "crap from here on"});
		list.addRecord(-3, new String[] {"jajaj", "h4k2j34h234"});
		list.addRecord(-4, new String[] {"fhgg", "2 32423 423"});
		list.addRecord(-5, new String[] {"&# UJF", "QWEURH Q## "});
		list.addRecord(-6, new String[] {"}{>L::><", "#$ !@^%^"});
		
		list.selectLines(appID);
		
		list.resumePaint(true);
	}

	protected void btnClose_onClick(Object sender) {
		wnd.hide();
	}

	protected void btnEndTask_onClick(Object sender) 
	{
		Integer[] sel = list.getSelectedLines();
		for (int i=0; i<sel.length; i++) {
			int appID = list.getLineID(sel[i]);
			kernel.killApp(appID);
		}
	}

	private void wnd_onResize(Object sender) 
	{
		// we update the layout here
		Location cl = wnd.getClientArea();
		list.setLocation(5, 5, cl.w-10, cl.h-40);
		btnClose.setPosition(cl.w - btnClose.getLocation().w - 5, cl.h - btnClose.getLocation().h - 5);
		btnEndTask.setPosition(btnClose.getLocation().x - btnEndTask.getLocation().w - 10, btnClose.getLocation().y);
	}

}
