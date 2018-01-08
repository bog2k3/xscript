package kernel;
import java.applet.Applet;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import app.IApplication;
import app.IRuntimeEnvObject;

import XScripter.Functor;




/**
 * implements the kernel for x-app system
 * @author Bogdan.Ionita
 *
 */
public class XKernel implements Runnable 
{
	private Applet applet = null;
	private Thread threadKernel = null;
	private KMouseListener mouseListener = null;
	private KMouseMotionListener mouseMotionListener = null;
	private KMouseWheelListener mouseWheelListener = null;
	private KKeyboardListener keyboardListener = null;
	protected boolean shutDownFlag = false;
	private MsgQueue msgQueue = null;
	private final int kernelLogChannel;
	
	public final Event onEnterIdle; 
	
	private MediaManager mediaMan = null;
	public MediaManager getMediaManager() { return mediaMan; }
	
	/**
	 * this should be checked after constructing the kernel.
	 * if it's false then some error occured and execution should not continue.
	 */
	public final boolean isInitialized;
	
	/** initializes the kernel.
	 * @param applet the applet to which the kernel is bound
	 * @return true on success, false on failure. when the caller receives 
	 * false, the program should be terminated and no further kernel calls be made.
	 */
	public XKernel(Applet applet) 
	{
		kernelLogChannel = registerLogChannel("KERNEL");
		log(kernelLogChannel, LogLevel.Default, "initializing kernel....");
		
		onEnterIdle = new Event(this);
		
		msgQueue = new MsgQueue(this);		
		this.applet = applet;
		
		try {
			mediaMan = new MediaManager(this);
		} catch (Exception e) {
			log(kernelLogChannel, LogLevel.Error, "error initializing MediaManager : exception thrown\n"+e);
			e.printStackTrace();
			isInitialized = false;
			return;
		}
		
		//run kernel on a different thread:
		threadKernel = new Thread(this);
		threadKernel.setName("kern");
		threadKernel.setPriority(Thread.MAX_PRIORITY);
		if (threadKernel == null) {
			log(kernelLogChannel, LogLevel.Error, "failed creating kernel thread.");
			isInitialized = false;
			return;
		}
		
		threadKernel.start();
		
		log(kernelLogChannel, LogLevel.Default, "kernel initialization complete.");
		
		isInitialized = true;
	}
	
	/** gets the applet to which the kernel is bound */
	public Applet getApplet() {
		return applet;
	}

	/** posts a message to the kernel message queue and returns immediately.
	 * the message is later retrieved and processed by the kernel.
	 * @param msg
	 */
	public void postMessage(Message msg)
	{
		if (!isInitialized)
			throw new RuntimeException("Kernel not initialized.");
		
		msgQueue.push(msg);
	}
	
	public void registerMouseListener(MouseListener listener) 
	{
		mouseListener = new KMouseListener(this);
		mouseListener.addSubscriber(listener);
		applet.addMouseListener(mouseListener);
	}

	public void registerMouseMotionListener(MouseMotionListener listener) 
	{
		mouseMotionListener = new KMouseMotionListener(this);
		mouseMotionListener.addSubscriber(listener);
		applet.addMouseMotionListener(mouseMotionListener);
	}

	public void registerMouseWheelListener(MouseWheelListener listener) {
		mouseWheelListener = new KMouseWheelListener(this);
		mouseWheelListener.addSubscriber(listener);
		applet.addMouseWheelListener(mouseWheelListener);
		
	}

	public void registerKeyListener(KeyListener keyboard) 
	{
		keyboardListener = new KKeyboardListener(this);
		keyboardListener.addSubscriber(keyboard);
		applet.addKeyListener(keyboardListener);
	}

	/**
	 * this is the main process of the kernel, running on the kernel thread.
	 * it polls the message queue and dispatches messages when they arrive to their respective
	 * recipients.
	 */
	@Override
	public void run()
	{
		log(kernelLogChannel, LogLevel.Default, "kernel thread started...");
		
		while (!shutDownFlag) // nothing lasts forever... :-) 
		{
			try {
				Message msg;
				
				if (msgQueue.checkAvail()) {
					msg = msgQueue.pop();
					if (kernelDebugLog) log(kernelLogChannel, LogLevel.Debug, "DELIVER msg "+msg);
					msg.deliver();
					if (kernelDebugLog) log(kernelLogChannel, LogLevel.Debug, "FINISHED delivering msg "+msg);
				
					Thread.yield();
				} else {
					onEnterIdle.fireOnThisThread(); // this is very important, it triggers the screen update
					synchronized (msgQueue) {
						msgQueue.wait();
					}
				}
			} catch (InterruptedException e) {
				// just resume the loop here
			} catch (Exception e) {
				log(kernelLogChannel, LogLevel.Error, "EXCEPTION thrown: "+getStackTrace(e));
			}
		}
		
		log(kernelLogChannel, LogLevel.Default, "kernel shutdown...");
		while (!apps.isEmpty()) {
			killApp(apps.get(0).getID());
		}
		if (mouseListener != null)
			applet.removeMouseListener(mouseListener);
		if (mouseMotionListener != null)
			applet.removeMouseMotionListener(mouseMotionListener);
		if (mouseWheelListener != null)
			applet.removeMouseWheelListener(mouseWheelListener);
		if (keyboardListener != null)
			applet.removeKeyListener(keyboardListener);

		log(kernelLogChannel, LogLevel.Default, "kernel shutted down successfully.");
	}

	public synchronized void shutDown() {
		postMessage(new KernelShutDownMsg(this, this));
		log(kernelLogChannel, LogLevel.Default, "waiting for kernel thread to stop...");
		try {
			threadKernel.join();
		} catch (InterruptedException e) {
			log(kernelLogChannel, LogLevel.Error, "error encountered waiting for kernel thread to stop\n"+e);
		}
		log(kernelLogChannel, LogLevel.Default, "kernel terminated.");
	}
	
	
	// --- Logging -------------------------------------------- //
	
	private final int nMaxLogChannels = 16;
	private HashMap<String, Integer> hashChannels = new HashMap<String, Integer>();
	private String[] logChannels = new String[nMaxLogChannels];
	private LogLevel[] logChannelLevelsOverride = new LogLevel[nMaxLogChannels];
	private int nUsedLogChannels = 0;
	private LogLevel globalLogLevel = LogLevel.Default;
	private boolean kernelDebugLog = false;
	
	public void setGlobalLogLevel(LogLevel l)
	{
		globalLogLevel = l;
		
		kernelDebugLog = (getChannelLogLevel(kernelLogChannel) == LogLevel.Debug);
	}
	
	public int registerLogChannel(String name)
	{
		if (hashChannels.containsKey(name))
			return hashChannels.get(name);
		
		if (nUsedLogChannels == nMaxLogChannels)
			return -1;
		
		logChannels[nUsedLogChannels] = name;
		hashChannels.put(name, nUsedLogChannels);
		logChannelLevelsOverride[nUsedLogChannels] = null;
		return nUsedLogChannels++;
	}
	
	public void overrideLogChannelLevel(String channelName, LogLevel level)
	{
		Integer channel = hashChannels.get(channelName);
		if (channel == null)
			channel = registerLogChannel(channelName);
		
		if (channel < 0 || channel >= nUsedLogChannels)
			return;

		logChannelLevelsOverride[channel] = level;
		if (channel == kernelLogChannel)
			kernelDebugLog = (level == LogLevel.Debug);
	}
	
	public LogLevel getChannelLogLevel(int channel)
	{
		if (channel < 0 || channel >= nUsedLogChannels)
			return LogLevel.Default;
		if (logChannelLevelsOverride[channel] == null)
			return globalLogLevel;
		else
			return logChannelLevelsOverride[channel];
	}
	
	public void log(int channel, LogLevel level, String msg)
	{
		if (channel < 0 || channel >= nUsedLogChannels)
			return;
		if (!level.pass(logChannelLevelsOverride[channel], globalLogLevel))
			return;
		String sLogLine = "["+logChannels[channel]+":"+level+" @thread:\""+Thread.currentThread().getName()+"\"] "+msg+"\n";
		System.out.print(sLogLine);
	}
	
	public static String getStackTrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
	// -- /Logging 
	// ------------------------------------------------------------

	public void setHttpRoot(String url) {
		mediaMan.setHttpRoot(url);
	}
	
	class TimerWrapper {
		Timer timer;
		Functor onTimeout;
		TimerWrapper(Timer t, Functor f) {
			timer = t;
			onTimeout = f;
		}
	}
	
	private final ArrayList<TimerWrapper> timers = new ArrayList<TimerWrapper>();

	public int createTimer(int interval, final Functor onTimeout) {
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				onTimeout.Execute(null);				
			}
		}, 
		interval, interval);
		
		synchronized (timers) {
			int i, n=timers.size();
			for (i=0; i<n; i++)
				if (timers.get(i) == null)
					break;
			if (i<n)
				timers.set(i, new TimerWrapper(t, onTimeout));
			else {
				timers.add(new TimerWrapper(t, onTimeout));
				i = timers.size()-1;
			}
			return i;
		}
	}

	public void setTimer(int timerID, int interval) {
		final TimerWrapper t;
		synchronized (timers) {
			t = timers.get(timerID);
		}
		t.timer.cancel();
		t.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				t.onTimeout.Execute(null);
			}
		}, 
		interval, interval);
	}
	
	public void removeTimer(int timerID) 
	{
		synchronized (timers) {
			TimerWrapper t = timers.get(timerID);
			t.timer.cancel();
			timers.set(timerID, null);
		}
	}

	public boolean saveTextFile(String xml, String url) 
	{
		//TODO send request to servlet
		return false;
	}

	//--- application tracking ------------------------
	
	private ArrayList<IApplication> apps = new ArrayList<IApplication>();
	
	public void registerApp(IApplication iapp) 
	{
		synchronized (apps) {
			apps.add(iapp);
		}
	}

	public IApplication[] getAppList() 
	{
		synchronized (apps) {
			IApplication[] iapps = new IApplication[apps.size()];
			return apps.toArray(iapps);
		}
	}

	public void killApp(Integer appID) 
	{
		IApplication iApp = getApplication(appID);
		log(kernelLogChannel, LogLevel.Default, "killing app "+iApp.getUserDataField("NAME")+"(ID:"+appID+")...");
		synchronized (iApp)
		{
			for (IRuntimeEnvObject rto : iApp.envObjects)
			{
				rto.destroy();
			}
			iApp.envObjects.clear();
			iApp.destroy();
		}
		
		synchronized(apps) {
			apps.remove(iApp);
		}
		log(kernelLogChannel, LogLevel.Default, "Application shutted down.");
	}

	public IApplication getApplication(Integer appID) 
	{
		synchronized (apps) {
			for (IApplication a : apps)
				if (a.getID().equals(appID)) {
					return a;
				}
		}
		return null;
	}
	
	// --- /app tracking ------------------------------
	
	

}
