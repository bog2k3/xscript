package XScripter;

/**
* this struct describes a method in a class (for external validation)
**/
public class MethodDesc
{
	/**
	* The name of the method from the scripted interface.
	**/
	public String Name;
	/**
	* data type for the return value of the method
	**/
	public TypeID ReturnType;
	/**
	* class name for the return value of the method, if the method returns an object.
	**/
	public String ReturnClass;
	/**
	* a list of parameters, describing the method's parameters' data types and class names.
	**/
	public PropertyDesc[] ParamDesc;
}
