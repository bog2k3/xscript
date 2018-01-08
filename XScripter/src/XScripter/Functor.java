package XScripter;


public abstract class Functor 
{

	/** this method is triggered by the event to which it is registered
	 */
	public abstract void Execute(Object sender, Object... params);
}
