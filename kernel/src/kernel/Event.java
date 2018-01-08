package kernel;

import app.IApplication;


import XScripter.Functor;
import XScripter.IEvent;

public class Event extends IEvent
{
	private IApplication app = null;
	
	public Event(Object owner, IApplication app) {
		super(owner);
		this.app = app;
	}
	
	public Event(Object owner) {
		super(owner);
	}
	
	public final void setApp(IApplication app) {
		if (this.app != null)
			return;
		this.app = app;
	}

	public final void fire(Object... params) {
		if (app == null) {
			/*//if app is null, broadcast to all apps
			IApplication[] apps = kernel.getAppList();
			for (int i=0; i<apps.length; i++)
				apps[i].postMessage(new EventMsg(owner, f, params));*/
			throw new NullPointerException("Event is not assigned to an application.");
		} else
			for (Functor f : listeners)
				app.postMessage(new EventMsg(owner, f, params));
	}
	
	final void fireOnThisThread(Object... params) {
		for (Functor f : listeners)
			if (params.length != 0)
				f.Execute(owner, params);
			else
				f.Execute(owner);
	}
}
