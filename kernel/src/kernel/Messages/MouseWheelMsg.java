package kernel.Messages;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import kernel.Message;

public class MouseWheelMsg extends Message 
{
	public final MouseWheelEvent m_args;

	public MouseWheelMsg(Object recipient, Object sender, MouseWheelEvent arg0) {
		super(recipient, sender);
		m_args = arg0;
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof ArrayList))
			throw new RuntimeException("recipient must be ArrayList<MouseListener>");
		@SuppressWarnings("unchecked")
		ArrayList<MouseWheelListener> subs = (ArrayList<MouseWheelListener>) recipient; 
		for (MouseWheelListener ml : subs)
			ml.mouseWheelMoved(m_args);
	}
	
	@Override
	public String toString() {
		return "MouseWheelMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" args:"+m_args+"]";
	}

}
