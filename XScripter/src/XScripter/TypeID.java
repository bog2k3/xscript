package XScripter;

public enum TypeID 
{
	/** generic identifier for an invalid data type */
	None,
	/** valid only as function return type */
	Void,
	/** a 32-bit signed integer */
	Int, IntArray,
	/** a 64-bit double precision float */
	Float, FloatArray,
	/** reference to an external object */
	Object, ObjectArray,
	/** reference to a string */
	String, StringArray,
	/** boolean-strict expression evaluating is enabled. */
	Bool, BoolArray,
	/** generic null type, for null keyword, compatible with object types and strings. */
	Null,
	
	// the following two types are only used internaly, with the intrinsic functions
	
	/** array of any type */
	AnyArray, 
	/** any type */
	Any;
	
	@Override
	public String toString() 
	{
		switch (this)
		{
		case None: return "INVALID";
		case Void: return ResWord.RWT_VOID;
		case Int: return ResWord.RWT_INT;
		case IntArray: return ResWord.RWT_INT + "[]";
		case Float: return ResWord.RWT_FLOAT;
		case FloatArray: return ResWord.RWT_FLOAT + "[]";
		case Object: return ResWord.RWT_OBJECT;
		case ObjectArray: return ResWord.RWT_OBJECT + "[]";
		case String: return ResWord.RWT_STRING;
		case StringArray: return ResWord.RWT_STRING + "[]";
		case Bool: return ResWord.RWT_BOOL;
		case BoolArray: return ResWord.RWT_BOOL + "[]";
		case Null: return "null";
		default: return "UNKNOWN";
		}
	}
	
	public static TypeID FromString(String name)
	{
		if (name.equals(ResWord.RWT_FLOAT))
			return TypeID.Float;
		else
			if (name.equals(ResWord.RWT_INT))
				return TypeID.Int;
			else
				if (name.equals(ResWord.RWT_OBJECT))
					return TypeID.Object;
				else
					if (name.equals(ResWord.RWT_STRING))
						return TypeID.String;
					else
						if (name.equals(ResWord.RWT_VOID))
							return TypeID.Void;
						else
							if (name.equals(ResWord.RWT_BOOL))
								return TypeID.Bool;
							else
								return TypeID.None;
	}

	/** obtain an array type with the given element type */ 
	public static TypeID ToArrayType(TypeID type)
	{
		switch (type)
		{
		case Int: return IntArray;
		case Float: return FloatArray;
		case Object: return ObjectArray;
		case String: return StringArray;
		case Bool: return BoolArray;
		default: return None;
		}
	}
	
	/** obtain the base type of an array.
	 * For non-array types, the base type is identical to self 
	 */
	
	public TypeID BaseType()
	{
		switch (this)
		{
		case IntArray: return Int;
		case FloatArray: return Float;
		case ObjectArray: return Object;
		case StringArray: return String;
		case BoolArray: return Bool;
		default: return this;
		}
	}
	
	public boolean isArray()
	{
		return this == AnyArray || this == IntArray || this == FloatArray || this == ObjectArray || this == StringArray || this == BoolArray;
	}
	
	Class<?> GetRuntimeInternalClass()
	{
		switch (this)
		{
		case Int: return Integer.class;
		case Float: return Double.class;
		case Object: return IScriptable.class;
		case String: return String.class;
		case Bool: return Boolean.class;
		default: return null;
		}
	}

	public static TypeID fromClass(Class<?> cls) 
	{
		if (cls.equals(int.class) || cls.equals(Integer.class))
			return Int;
		else
		if (cls.equals(int[].class) || cls.equals(Integer[].class))
			return IntArray;
		else
		if (cls.equals(double.class) || cls.equals(Double.class))
			return Float;
		else
		if (cls.equals(double[].class) || cls.equals(Double[].class))
			return FloatArray;
		else
		if (cls.equals(String.class))
			return String;
		else
		if (cls.equals(String[].class))
			return StringArray;
		else
		if (cls.equals(boolean.class) || cls.equals(Boolean.class))
			return Bool;
		else
		if (cls.equals(boolean[].class) || cls.equals(Boolean[].class))
			return BoolArray;
		else
		if (cls.isArray())
			return ObjectArray;
		else
			return Object;
	}
}
