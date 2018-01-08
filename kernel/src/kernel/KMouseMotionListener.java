package kernel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import kernel.Messages.MouseDraggedMsg;
import kernel.Messages.MouseMovedMsg;

public class KMouseMotionListener implements MouseMotionListener 
{
	
	protected ArrayList<MouseMotionListener> subscribers = new ArrayList<MouseMotionListener>();
	protected void addSubscriber(MouseMotionListener ml) {
		subscribers.add(ml);
	}
	protected void removeSubscriber(MouseMotionListener ml) {
		subscribers.remove(ml);
	}
	
	private XKernel kernel;
	
	protected KMouseMotionListener(XKernel kernel)
	{
		this.kernel = kernel;
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		kernel.postMessage(new MouseDraggedMsg(subscribers, this, arg0));
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		kernel.postMessage(new MouseMovedMsg(subscribers, this, arg0));
	}

}
