package kernel;


public class KernelShutDownMsg extends Message {

	public KernelShutDownMsg(Object recipient, Object sender) {
		super(recipient, sender);
	}

	@Override
	public void deliver() 
	{
		if (!(recipient instanceof XKernel))
			return;
		
		((XKernel)recipient).shutDownFlag = true;
	}
	
	@Override
	public String toString() {
		return "KernelShutDownMsg"+Message.hashString(this);
	}

}
