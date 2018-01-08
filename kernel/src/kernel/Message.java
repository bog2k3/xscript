package kernel;


public abstract class Message 
{
	public final Object recipient;
	public final Object sender;
	
	public Message(Object recipient, Object sender)
	{
		this.recipient = recipient;
		this.sender = sender;
	}
	
	/** override this to implement the self-delivery of the message to the recipient.
	 * this is called on the message handling thread, in the kernel.
	 */
	public abstract void deliver();
	
	@Override
	public String toString() {
		return "Message"+Message.hashString(this)+ "[recipient:"+recipient+" sender:"+sender+"]";
	}
	
	public static String hashString(Object obj)
	{
		return "$"+Integer.toHexString(obj.hashCode())+"$";
	}
}
