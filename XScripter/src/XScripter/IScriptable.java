package XScripter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
* extend this class to allow your classes to be used from within a script
**/
public abstract class IScriptable
{
	/**
	* this method should validate the internal class, by checking the types of each property
	* and the return types and parameter types of each method.
	*
	* @param Properties A list of properties that the script interface declares
	* @param Methods A list of methods the script interface declares
	* @param BaseClassName The name of the Base Class for this class (or null)
	* @param isAbstract Indicates whether the class was declared in the script as abstract
	* @returns true for success, false if the class description does not match the external class
	**/
	public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)
	{
		throw new RuntimeException("The class MUST implement the method\n"+
				"public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)"
			);
	}

	/**
	* this method performs a call of the given function in the outside environment, and returns the result
	*
	* @param MethodName method identifier to call
	* @param Parameters list of method parameters
	* @returns the result of the called method or, if an error occured, a description of the error.
	**/
	ExtResult callMethod(String MethodName, Object[] Parameters) {
		Class<?>[] clsDesc = new Class[Parameters.length];
		for (int i=0; i<Parameters.length; i++)
			clsDesc[i] = Parameters[i] != null ? Parameters[i].getClass() : null;
		try {
			Method[] meths = this.getClass().getMethods();
			for (Method m : meths) {
				if (!m.getName().equals(MethodName))
					continue;
				Class<?>[] params = m.getParameterTypes();
				if (params.length != clsDesc.length)
					continue;
				
				try {
					for (int i=0; i<params.length; i++)
						if (clsDesc[i] != null)
							clsDesc[i] = clsDesc[i].asSubclass(params[i]);
				} catch (ClassCastException e) {
					continue;
				}
				
				return ExtResult.Value(m.invoke(this, Parameters));
			}
			return ExtResult.Error("No matching method found.");
		} catch (InvocationTargetException e) {
			return ExtResult.Error(e.getTargetException().toString());
		} catch (Exception e) {
			return ExtResult.Error(e.toString());
		}		
	}

	/**
	* This method should return the value of the property identified by the given name
	* or an error decription in case of something goes wrong.
	*
	* @param PropertyID name of the property requested.
	* @returns
	*/
	ExtResult getPropertyValue(String PropertyID) {
		try {
			Object field = this.getClass().getField(PropertyID).get(this);
			return ExtResult.Value(field);
		} catch (Exception e) {
			return ExtResult.Error(e.toString());
		}
	}

	/**
	* this method sets the specified property to the given value.
	* If the operation succeeds, it should return null, otherwise an error description.
	*
	* @param PropertyID name of the requested property
	* @param Value new value to set to the given property
	* @return null for success, or error description
	*/
	ExtResult setPropertyValue(String PropertyID, Object Value) {
		try {
			this.getClass().getField(PropertyID).set(this, Value);
			return null;
		} catch (Exception e){
			return ExtResult.Error(e.toString());
		}
	}
	
	/**
	 * this method assigns a handler name to the given event
	 * @param eventName
	 * @param handlerName
	 * @return
	 */
	ExtResult assignEvent(final Executor ex, String eventName, final String handlerName) 
	{
		//TODO prevent assigning cross-application events

		ExtResult res = getPropertyValue(eventName);
		if (res.Error != null)
			return res;
		IEvent ev = (IEvent)res.Result;
		
		ev.addListener(
				new Functor() {
					@Override
					public void Execute(Object sender, Object... params) {
						EntryPoint ep;
						if (params.length != 0) {
							Object[] par = new Object[params.length+1];
							par[0] = sender;
							for (int i=0; i<params.length; i++)
								par[i+1] = params[i];
							ep = new EntryPoint(handlerName, par);
						} else
							ep = new EntryPoint(handlerName, sender);
						ex.Execute(ep, RunMode.Blocking);
					}
				}
			);
		return null;
	}

	/**
	* this method performs a static call on the given static function in the 
	* outside environment, and returns the result
	*
	* @param methodName static method identifier to call
	* @param params list of method parameters
	* @returns the result of the called method or, if an error occured, a description of the error.
	**/
	public static ExtResult callStaticMethod(ScriptClass cls,
			String methodName, Object[] params) 
	{
		Class<?>[] clsDesc = new Class[params.length];
		for (int i=0; i<params.length; i++)
			clsDesc[i] = params[i].getClass();
		try {
			Method[] meths = cls.ExtClassType.getMethods();
			for (Method m : meths) {
				if (!m.getName().equals(methodName))
					continue;
				Class<?>[] paramsCls = m.getParameterTypes();
				if (paramsCls.length != clsDesc.length)
					continue;
				
				for (int i=0; i<paramsCls.length; i++)
					clsDesc[i] = clsDesc[i].asSubclass(paramsCls[i]);
				
				return ExtResult.Value(m.invoke(null, params));
			}
			return ExtResult.Error("No matching method found.");
		} catch (ClassCastException e) {
			return ExtResult.Error("No matching method found.");
		} catch (InvocationTargetException e) {
			return ExtResult.Error(e.getTargetException().toString());
		} catch (Exception e) {
			return ExtResult.Error(e.toString());
		}
	}

	/**
	* This method should return the value of the static field identified by the given name
	* or an error decription in case of something goes wrong.
	*
	* @param name name of the property requested.
	* @returns
	*/
	public static ExtResult getStaticFieldValue(ScriptClass cls, String name)
	{
		try {
			Object field = cls.ExtClassType.getField(name).get(null);
			return ExtResult.Value(field);
		} catch (Exception e) {
			return ExtResult.Error(e.toString());
		}
	}

	/**
	* this method sets the specified static field to the given value.
	* If the operation succeeds, it should return null, otherwise an error description.
	*
	* @param name name of the requested static field
	* @param value new value to set to the given static field
	* @return null for success, or error description
	*/
	public static ExtResult setStaticFieldValue(ScriptClass cls, String name, Object value) 
	{
		try {
			cls.ExtClassType.getField(name).set(null, value);
			return null;
		} catch (Exception e){
			return ExtResult.Error(e.toString());
		}
	}
}