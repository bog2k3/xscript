package kernel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import kernel.Messages.MouseClickedMsg;
import kernel.Messages.MousePressedMsg;
import kernel.Messages.MouseReleasedMsg;

public class KMouseListener implements MouseListener
{
	protected ArrayList<MouseListener> subscribers = new ArrayList<MouseListener>();
	protected void addSubscriber(MouseListener ml) {
		subscribers.add(ml);
	}
	protected void removeSubscriber(MouseListener ml) {
		subscribers.remove(ml);
	}
	
	private XKernel kernel;
	
	protected KMouseListener(XKernel kernel)
	{
		this.kernel = kernel;
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		kernel.postMessage(new MouseClickedMsg(subscribers, this, arg0));
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// empty...		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// empty...		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		kernel.postMessage(new MousePressedMsg(subscribers, this, arg0));
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
		kernel.postMessage(new MouseReleasedMsg(subscribers, this, arg0));		
	}

}
