package kernel;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import kernel.Messages.MouseWheelMsg;

public class KMouseWheelListener implements MouseWheelListener 
{
	protected ArrayList<MouseWheelListener> subscribers = new ArrayList<MouseWheelListener>();
	protected void addSubscriber(MouseWheelListener ml) {
		subscribers.add(ml);
	}
	protected void removeSubscriber(MouseWheelListener ml) {
		subscribers.remove(ml);
	}
	
	private XKernel kernel;
	
	protected KMouseWheelListener(XKernel kernel)
	{
		this.kernel = kernel;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		kernel.postMessage(new MouseWheelMsg(subscribers, this, arg0));
	}

}
