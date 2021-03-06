package kernel.Messages;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import kernel.Message;

public class KeyTypedMsg extends Message 
{
	public final KeyEvent m_args;

	public KeyTypedMsg(Object recipient, Object sender, KeyEvent arg0) {
		super(recipient, sender);
		m_args = arg0;
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof ArrayList))
			throw new RuntimeException("recipient must be ArrayList<MouseListener>");
		@SuppressWarnings("unchecked")
		ArrayList<KeyListener> subs = (ArrayList<KeyListener>) recipient; 
		for (KeyListener ml : subs)
			ml.keyTyped(m_args);
	}
	
	@Override
	public String toString() {
		return "KeyTypedMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" args:"+m_args+"]";
	}

}
