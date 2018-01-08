package XScripter;

/**
 * describes a variable inside the script
**/
public class Variable 
{
	
	/** the type of the variable */
	public TypeID varType = TypeID.None;
	
	/** name of external class, for TypeID.Object */
	public String classID = null;
	
	/** this holds the value of the variable;
	 * the type of object it references, depends on the type of variable */
	public Object value = null;
	
	/** name of the variable (used at compile time only) */
	public String name = null;

	/** modifier specified with the "readonly" keyword, prevents writes to this variable */
	public boolean readonly;
	
	/** constructor for creating variables to register as external function argument descriptors.
	 * @param name
	 * @param type
	 * @param ClassID The ID of the registered class type, obtained by calling Script.GetClassID(...)
	 */
	Variable(String name, TypeID type, String ClassID)
	{
		this.varType = type;
		this.name = name;
		this.classID = ClassID;
	}
	
	/** creates a variable of the given type and class, initializing it with the given value
	 * @param name name of the variable
	 * @param type type of the variable
	 * @param ClassID name of the class if the var is a TypeID.Object
	 * @param value initial value for this variable
	 * @param arrayCount specify a number different than 0 to produce an array
	 */
	public Variable(String name, TypeID type, String ClassID, int arrayCount, Object value)
	{
		this.varType = type;
		this.name = name;
		this.classID = ClassID;
		this.value = value;
	}
	
	/** creates a variable of the given type and class, initializing it with the given value
	 * @param name name of the variable
	 * @param type type of the variable
	 * @param ClassID name of the class if the var is a TypeID.Object
	 * @param value initial value for this variable
	 */
	public Variable(String name, TypeID type, String ClassID, Object value)
	{
		this(name, type, ClassID, -1, value);
	}

	Variable(TypeID type)
	{
		varType = type;
		switch (type)
		{
			case Int:
				value = new Integer(0);
				break;
			case Float:
				value = new Double(0.0);
				break;
			case Bool:
				value = new Boolean(false);
				break;
			case Object:
				value = null;
				break;
			default:
				value = null;
				break;
		}
	}
}
