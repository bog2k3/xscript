package XScripter;

import java.util.ArrayList;

public class Tools 
{
	public static void removeRange(ArrayList<?> a, int index, int count)
	{
		for (int i=count; i>0; i--)
			a.remove(index);
	}
	
	public static Integer IntValue(Object v, ScriptContext C)
	{
		if (v instanceof Integer)
			return (Integer)v;
		if (v instanceof Double)
			return new Integer(((Double)v).intValue());
		if (v instanceof Float)
			return (Integer)((Float)v).intValue();
		
		C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidObjectClass, C,
				"Cannot convert (" + v.getClass().toString() + ") to (Integer)"
			));
		return 0;
	}
	
	public static Double DblValue(Object v, ScriptContext C)
	{
		if (v instanceof Double)
			return (Double)v;
		if (v instanceof Float)
			return (Double)((Float)v).doubleValue();
		if (v instanceof Integer)
			return new Double(((Integer)v).doubleValue());
		
		C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidObjectClass, C, 
				"Cannot convert (" + v.getClass().toString() + ") to (Double)" 
			));
		return 0.0;
	}
}
