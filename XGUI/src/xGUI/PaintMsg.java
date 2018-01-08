package xGUI;

import kernel.Message;

public class PaintMsg extends Message 
{
	public final Location paintArea;
	public final Long threadID;

	public PaintMsg(Object recipient, Location area, Object sender) {
		super(recipient, sender);
		paintArea = area;
		threadID = Thread.currentThread().getId();
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof VisualComponent))
		{
			throw new RuntimeException("Attempting to deliver a PaintMsg to a non-VisualComponent: "+recipient);
		}
		((VisualComponent)sender).invokePaint(paintArea, (VisualComponent)recipient, threadID);
	}
	
	@Override
	public String toString() {
		return "PaintMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+" area:"+paintArea+"]";
	}

}
