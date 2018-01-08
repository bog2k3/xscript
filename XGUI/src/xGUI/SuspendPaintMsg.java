package xGUI;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import kernel.Message;

public class SuspendPaintMsg extends Message 
{
	public final Long threadID;

	public SuspendPaintMsg(Object recipient, Object sender) {
		super(recipient, sender);
		threadID = Thread.currentThread().getId();
	}

	@Override
	public void deliver() 
	{
		VisualComponent c = (VisualComponent)recipient;
		HashMap<Long, Integer> hCounters = c.m_paintSuspendCounters;
		Integer iCrtCounter = hCounters.get(threadID);
		if (iCrtCounter == null)
			iCrtCounter = 0;
		hCounters.put(threadID, iCrtCounter + 1);
		
		if (c.updateRgn.get(threadID) == null)
			c.updateRgn.put(threadID, new ArrayList<Rectangle>());
	}
	
	@Override
	public synchronized String toString() {
		return "SuspendPaintMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" threadID:"+threadID+"]";
	}

}
