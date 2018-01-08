package XScripter;

import java.util.ArrayList;

public abstract class IEvent 
{
	protected ArrayList<Functor> listeners = new ArrayList<Functor>();
	protected final Object owner;
	
	public IEvent(Object owner) {
		this.owner = owner;
	}
	
	public void addListener(Functor f) {
		synchronized (listeners) {
			listeners.add(f);
		}
	}
	
	public void removeListener(Functor f) 
	{
		synchronized (listeners) {
			listeners.remove(f); //TODO how? f needs to be saved, and not recreated on-the-spot
		}
	}
	
	public void removeAllListeners() 
	{
		synchronized (listeners) {
			listeners.clear();
		}
	}
}
