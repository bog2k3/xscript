package kernel.Messages;

import kernel.Message;

/**
 * this is used as a dummy message just to wake up a thread waiting on an empty message queue. 
 * @author Bogdan.Ionita
 *
 */
public class NotifyMsg extends Message 
{

	public NotifyMsg(Object recipient, Object sender) {
		super(recipient, sender);
	}

	@Override
	public void deliver() {
		// we do nothing. just wake up the message queue.
	}

}
