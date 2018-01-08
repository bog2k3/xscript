package xGUI;

import java.awt.Graphics2D;

import kernel.Message;

public class PaintBorderMsg extends Message 
{
	public final Graphics2D gfx;

	public PaintBorderMsg(Object recipient, Graphics2D gfx, Object sender) {
		super(recipient, sender);
		this.gfx = gfx;
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof Container))
			throw new RuntimeException("PaintBorderMsg delivered to non-Container object!");
		((Container)recipient).paintBorder(gfx);
	}
	
	@Override
	public String toString() {
		return "PaintBorderMsg"+Message.hashString(this)+"[recipient:"+recipient+" sender:"+sender+"]";
	}

}
