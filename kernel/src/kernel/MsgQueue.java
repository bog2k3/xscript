package kernel;

import java.util.ArrayDeque;


public class MsgQueue 
{
	private ArrayDeque<Message> msgList = new ArrayDeque<Message>();
	private XKernel kernel = null;
	private final int msgQueueLogChannel;
	private boolean enableLog = false;
	
	public MsgQueue(XKernel kernel)
	{
		this.kernel = kernel;
		msgQueueLogChannel = kernel.registerLogChannel("MSGQUEUE");
	}
	
	/**
	 * pushes a new message to the queue. this method is run on the caller's thread.
	 * the message is later received and processed by the kernel on a different thread.
	 * the call returns immediately, and does not wait for the message to be processed.
	 * returns true on success, or false if the message could not be pushed, due to
	 * no space available. in this case, wait a while and try again.
	 * @param msg
	 */
	public void push(Message msg)
	{
		if (enableLog) kernel.log(msgQueueLogChannel, LogLevel.Debug, "PUSH " + msg.toString());
		// save the message
		synchronized (msgList) {
			msgList.add(msg);
		}
		synchronized (this) {
			notifyAll();
		}
	}
	
	public void enableLogging(boolean enable)
	{
		enableLog = enable;
	}
	
	/**
	 * returns true if the queue has messages ready to pop, or false if the queue is empty
	 * @return
	 */
	public boolean checkAvail()
	{
		return (!msgList.isEmpty());
	}
	
	/**
	 * pops and returns the oldest message from the message queue
	 * @return
	 */
	public Message pop()
	{
		if (msgList.isEmpty())
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				return null;
			}
		
		Message msg;		
		synchronized (msgList) {
			// retrieve the oldest message:
			msg = msgList.poll();
		}
		
		if (enableLog) kernel.log(msgQueueLogChannel, LogLevel.Debug, "POP " + msg.toString());
		
		return msg;
	}
	
	/**
	 * clears the message queue, discarding all pending messages
	 */
	public synchronized void clear()
	{
		msgList.clear();
	}
}
