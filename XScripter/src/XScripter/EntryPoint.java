package XScripter;

/**
* this class describes which function to run and the parameters to it
* an instance of this class will be locked by the executing thread in an async run mode.
**/
public class EntryPoint
{
	/**
	* the name of the function to run
	**/
	public String FunctionName = null;
	/**
	* a list of objects to pass as parameters to the function
	**/
	public Object[] Parameters;
	/**
	* this will hold the return value of the function upon return.
	**/
	public Object ReturnValue() { return functionReturn != null ? functionReturn.value : null; }

	/**
	* the function will be pointed to this variable to store it's return value:
	**/
	Variable functionReturn = null;

	/**
	* Creates an Executor with the given parameters.
	*
	* @param script script to bind the executor to 
	* @param Function Function name to execute 
	* @param OnFinish a delegate to invoke when the execution is finished (for async mode) 
	* @param parameters parameters to pass to the entry function
	**/ 
	public EntryPoint(String Function, Object... parameters)
	{
		this.FunctionName = Function;
		this.Parameters = parameters;
	}
}