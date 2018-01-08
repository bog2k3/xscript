package kernel;

import XScripter.Functor;

/**
 * @author Bogdan.Ionita
 *
 */
public class EventMsg extends Message 
{
	Functor func;
	Object[] params;

	public EventMsg(Object sender, Functor func, Object... params) {
		super(null, sender);
		this.func = func;
		this.params = params;
	}

	@Override
	public void deliver() {
		if (params.length > 0)
			func.Execute(sender, params);
		else
			func.Execute(sender);
	}

}
