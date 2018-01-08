package kernel.Messages;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import kernel.Message;

public class MouseDraggedMsg extends Message 
{

	public final MouseEvent m_args;

	public MouseDraggedMsg(Object recipient, Object sender, MouseEvent arg0) {
		super(recipient, sender);
		m_args = arg0;
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof ArrayList))
			throw new RuntimeException("recipient must be ArrayList<MouseListener>");
		@SuppressWarnings("unchecked")
		ArrayList<MouseMotionListener> subs = (ArrayList<MouseMotionListener>) recipient; 
		for (MouseMotionListener ml : subs)
			ml.mouseDragged(m_args);
	}
	
	@Override
	public String toString() {
		return "MouseDraggedMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" args:"+m_args+"]";
	}

}
