/**
 * 
 */
package XScripter;

/**
 * @author bog
 * This class is used as return value for external class implementation methods.
 * It contains a result value of type Object and an error description of type String.
 */
public class ExtResult 
{
	Object Result = null;
	String Error = null;
	
	ExtResult(Object res, String err)
	{
		Result = res;
		Error = err;
	}
	
	/**
	 * Creates an ExtResult object containing a valid reference to a value. 
	 * @param v Value to return to script
	 * @return
	 */
	public static ExtResult Value(Object v)
	{
		return new ExtResult(v, null);
	}
	
	/**
	 * Creates an ExtResult object containing an error message to inform the 
	 * script that the requested value could not be retrieved.
	 * @param err Error description
	 * @return
	 */
	public static ExtResult Error(String err)
	{
		return new ExtResult(null, err);
	}
}
