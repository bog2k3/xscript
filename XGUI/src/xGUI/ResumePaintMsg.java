package xGUI;

import java.util.HashMap;

import kernel.Message;

public class ResumePaintMsg extends Message 
{
	boolean performPendingRequests = false;
	public final Long threadID;

	public ResumePaintMsg(Object recipient, Object sender, boolean performPendingRequests) {
		super(recipient, sender);
		this.performPendingRequests = performPendingRequests;
		threadID = Thread.currentThread().getId();
	}

	@Override
	public void deliver() 
	{
		VisualComponent c = (VisualComponent)recipient;
		HashMap<Long, Integer> hCounters = c.m_paintSuspendCounters;
		int iCrtCounter = hCounters.get(threadID);
		
		if (iCrtCounter > 0)
		{
			iCrtCounter--;
			hCounters.put(threadID, iCrtCounter);
			if (iCrtCounter == 0)
			{
				if (performPendingRequests)
					c.resolvePendingPaintReq(threadID);
				
				c.updateRgn.get(threadID).clear();
			}
		}
	}
	
	@Override
	public String toString() {
		return "ResumePaintMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" threadID:"+threadID+" performPending:"+performPendingRequests+"]";
	}

}
