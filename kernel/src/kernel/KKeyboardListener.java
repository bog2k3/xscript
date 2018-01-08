package kernel;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import kernel.Messages.KeyPressedMsg;
import kernel.Messages.KeyReleasedMsg;
import kernel.Messages.KeyTypedMsg;

public class KKeyboardListener implements KeyListener 
{
	protected ArrayList<KeyListener> subscribers = new ArrayList<KeyListener>();
	protected void addSubscriber(KeyListener ml) {
		subscribers.add(ml);
	}
	protected void removeSubscriber(KeyListener ml) {
		subscribers.remove(ml);
	}
	
	private XKernel kernel;
	
	protected KKeyboardListener(XKernel kernel)
	{
		this.kernel = kernel;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		kernel.postMessage(new KeyPressedMsg(subscribers, this, arg0));
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		kernel.postMessage(new KeyReleasedMsg(subscribers, this, arg0));
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		kernel.postMessage(new KeyTypedMsg(subscribers, this, arg0));
	}

}
