package kernel.Messages;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import kernel.Message;

public class MouseReleasedMsg extends Message 
{
	public final MouseEvent m_args;

	public MouseReleasedMsg(Object recipient, Object sender, MouseEvent arg0) {
		super(recipient, sender);
		m_args = arg0;
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof ArrayList))
			throw new RuntimeException("recipient must be ArrayList<MouseListener>");
		@SuppressWarnings("unchecked")
		ArrayList<MouseListener> subs = (ArrayList<MouseListener>) recipient; 
		for (MouseListener ml : subs)
			ml.mouseReleased(m_args);
	}
	
	@Override
	public String toString() {
		return "MouseReleasedMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" args:"+m_args+"]";
	}

}
