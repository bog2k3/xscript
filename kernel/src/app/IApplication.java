package app;

import java.util.ArrayList;
import java.util.HashMap;

import XScripter.IScriptable;

import kernel.Message;
import kernel.MsgQueue;
import kernel.XKernel;

public abstract class IApplication extends IScriptable
{
	public final ArrayList<IRuntimeEnvObject> envObjects = new ArrayList<IRuntimeEnvObject>();
	
	/**
	 * posts a message to the message queue associated with this application
	 * the application periodically polls the queue for new messages and processes them
	 * when available.
	 */
	public final void postMessage(Message msg) 
	{
		msgQueue.push(msg);
	}
	
	public abstract boolean start();
	
	public abstract void destroy();
	
	public final XKernel kernel;
	
	public final MsgQueue msgQueue;
	
	public IApplication(XKernel kernel) {
		this.kernel = kernel;
		msgQueue = new MsgQueue(kernel);
		kernel.registerApp(this);
	}

	private HashMap<String, Object> userDataMap = new HashMap<String, Object>();
	
	public final void setUserDataField(String field, Object value) {
		userDataMap.put(field, value);
	}
	
	public final Object getUserDataField(String field) {
		return userDataMap.get(field);
	}

	public Integer getID() 
	{
		return hashCode();
	}
}
