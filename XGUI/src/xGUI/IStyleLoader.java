package xGUI;

import java.util.ArrayList;

import wrappers.XMLNode;

public abstract class IStyleLoader 
{
	/**
	 * this method loads the associated style and skin from the given XML node
	 * @param eRoot
	 * @param themeRootURL
	 */
	public abstract void loadStyle(XMLNode root, String themeRootURL);
	
	private ArrayList<VisualComponent> subscribers = new ArrayList<VisualComponent>();
	
	public void addSubscriber(VisualComponent comp) {
		subscribers.add(comp);
	}
	
	public void removeSubscriber(VisualComponent comp) {
		subscribers.remove(comp);
	}
	
	public void notifyUpdate() {
		for (VisualComponent c : subscribers) {
			c.suspendPaint();
			c.updateFromStyle();
			c.resumePaint(false);
			c.repaint();
		}
	}
}
