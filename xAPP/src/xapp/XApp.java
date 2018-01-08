package xapp;

import app.IApplication;
import app.IRuntimeEnvObject;
import xGUI.Window;
import xGUI.WindowDesc;
import xGUI.XGUI;

import kernel.LogLevel;
import kernel.MediaManager;
import kernel.Message;
import kernel.XKernel;
import XScripter.EntryPoint;
import XScripter.Executor;
import XScripter.MethodDesc;
import XScripter.PropertyDesc;
import XScripter.RunMode;
import XScripter.RuntimeError;
import XScripter.RuntimeErrorCode;
import XScripter.RuntimeErrorHandler;
import XScripter.Script;

/**
 * This class implements a standard x-application, which integrates with the XGUI system
 * and is driven by the XScripter engine.
 * @author Bogdan.Ionita
 *
 */
public class XApp extends IApplication implements Runnable, RuntimeErrorHandler
{
	private Script script = null;
	private Executor exec = null;
	protected final Thread appThread;
	protected final String appName;
	protected final XGUI xgui;
	protected int appLogChannel = -1;
	
	/**
	 * this constructor is internal and only to be used by subclasses of XApp.
	 */
	protected XApp(XGUI xgui, String appName)
	{
		super(xgui.getKernel());
		this.xgui = xgui;
		this.appName = appName;
		appThread = new Thread(this);
		appThread.setName(appName);
		appLogChannel = kernel.registerLogChannel("XAPP");
		
		setUserDataField("NAME", appName);
	}
	
	/**
	 * creates and initializes a new instance of XApp with the given script as
	 * executable code
	 * @param script
	 */
	public XApp(Script script, XGUI xgui)
	{
		this(xgui, script.Name);
		this.script = script;
		
		// do the setup here (executor and stuff, setup error handler)
		exec = new Executor(script);
		exec.SetRuntimeErrorHandler(this);
		
		setUserDataField("PATH", script.FileName);
	}
	
	public Window createWindow(WindowDesc desc)
	{
		return xgui.createWindow(this, desc);
	}
	
	/**
	 * this method initializes the application. it is called as soon as the app starts.
	 * by default, this calls the "initApp()" function in the associated script.
	 * override this in a custom application.
	 */
	public boolean init() 
	{
		//call "initApp()" in the script
		EntryPoint ep = new EntryPoint("initApp", this);
		if (exec.Execute(ep, RunMode.Blocking) != null)
			return false;
		String retCode = (String)ep.ReturnValue();
		if (retCode != null) {
			log(LogLevel.Error, "initApp() returned the following error :\""+retCode+"\"");
			return false;
		}
		return true;
	}
	
	/**
	 * start running the application
	 * @return
	 */
	public boolean start()
	{
		if (appThread.isAlive())
			return false;
		
		appThread.start();
		return true;
	}
	
	public void Kill()
	{
		
	}
	
	private Boolean shutDownFlag = false;
	public boolean enableLog = false;

	@Override
	public final void run() 
	{
		log(LogLevel.Default, "application thread started...");
		
		if (init()) {
			
			log(LogLevel.Default, "application initialization complete. starting message poll...");
		
			while (!shutDownFlag) // nothing lasts forever... :-) 
			{
				try {
					Message msg = msgQueue.pop();
					
					if (msg == null)
						break;
					
					if (enableLog) log(LogLevel.Debug, "DELIVER msg "+msg);
					msg.deliver();
					if (enableLog) log(LogLevel.Debug, "FINISHED delivering msg "+msg);
					
				} catch (Exception e) {
					log(LogLevel.Error, "EXCEPTION thrown: "+XKernel.getStackTrace(e));
				}
			}
		} else
			log(LogLevel.Default, "application initialization FAILED. aborting...");

		//close all associated windows:
		for (IRuntimeEnvObject obj : envObjects)
			obj.destroy();
		envObjects.clear();
	}
	
	public final void log (LogLevel level, String msg) { 
		kernel.log(appLogChannel, level, msg);
	}
	
	public final void log(String msg) {
		log(LogLevel.Info, msg);
	}

	@Override
	public void handleScriptRuntimeError(RuntimeError err) {
		log(LogLevel.Error, "Runtime error occured in script \""+script.Name+"\":\n"+err);
		if (err.code == RuntimeErrorCode.DebugRequested)
		{
			@SuppressWarnings("unused")
			int we_need_to_place_a_break_point_here_to_intercept_the_request = 0;
			System.out.printf("!!!BREAKPOINT!!!\n");
		}
		else
			shutDownFlag = true;
	}
	
	public MediaManager getMediaManager()
	{
		return kernel.getMediaManager();
	}
	
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		return true;
		//TODO check
	}

	@Override
	public void destroy() 
	{
		synchronized (shutDownFlag)
		{
			shutDownFlag = true;
		}
		appThread.interrupt();
	}
}
