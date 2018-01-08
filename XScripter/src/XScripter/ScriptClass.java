package XScripter;

import java.util.ArrayList;

/**
 this describes the interface of an external class.
**/
public class ScriptClass
{
	/** name of the class. */
	String Name = null;

	/** this indicates whether the class is abstract (abstract cannot be instantiated) */
	boolean Abstract = false;

	/** a list of properties exposed by the class. */
	ArrayList<Variable> properties = new ArrayList<Variable>();
	
	/** a list of static fields exposed by the class. */
	ArrayList<Variable> staticFields = new ArrayList<Variable>();
	
	/** a list of methods exposed by the class. */
	ArrayList<Function> methods = new ArrayList<Function>();
	
	/** a list of static methods exposed by the class. */
	ArrayList<Function> staticMethods = new ArrayList<Function>();
	
	/** a list of constructors for this class */
	ArrayList<Function> constructors = new ArrayList<Function>();
	
	/** a list of events exposed by the class. */
	ArrayList<Function> events = new ArrayList<Function>();
	
	/**
	* the base class of this class (if it inherits another one).
	**/
	ScriptClass baseClass = null;
	
	boolean isDefined = false;
	
	/**
	* returns a description of the requested property if it exists in the class or in the inheritance chain,
	* or null otherwise.
	* @param propertyName name of requested property.
	**/
	public Variable getProperty(String propertyName)
	{
		// search own properties:
		for (int i=0; i<properties.size(); i++)
		{
			if (properties.get(i).name.equals(propertyName))
				return properties.get(i);
		}
		// if we have a base class, let it search for properties, or return null
		if (baseClass != null)
			return baseClass.getProperty(propertyName);
		else
			return null;
	}
	
	/**
	* returns a description of the requested static field if it exists in the class,
	* or null otherwise.
	* @param propertyName name of requested static field.
	**/
	public Variable getStaticField(String propertyName)
	{
		// search own properties:
		for (int i=0; i<staticFields.size(); i++)
		{
			if (staticFields.get(i).name.equals(propertyName))
				return staticFields.get(i);
		}
		return null;
	}
	
	/**
	* returns a description of the requested method if it exists in the class or in the inheritance chain,
	* or null otherwise.
	* @param methodName
	**/
	public Function getMethod(int signature)
	{
		// search own methods:
		for (Function fn : methods)
		{
			if (fn.signature == signature)
				return fn;
		}
		// if we have a base class, let it search for methods, or return null
		if (baseClass != null)
			return baseClass.getMethod(signature);
		else
			return null;
	}
	
	/**
	* returns a description of the requested static method if it exists in the class 
	* or null otherwise.
	* @param methodName
	**/
	public Function getStaticMethod(int signature)
	{
		// search own methods:
		for (Function fn : staticMethods)
		{
			if (fn.signature == signature)
				return fn;
		}
		
		return null;
	}
	
	/**
	* returns a description of the requested method if it exists in the class or in the inheritance chain,
	* or null otherwise.
	* @param methodName
	**/
	public Function getConstructor(int signature)
	{
		// search own methods:
		for (Function fn : constructors)
		{
			if (fn.signature == signature)
				return fn;
		}
		return null;
	}
	
	Script Parent = null;

	/**
	* this member is not assigned to, until the external class is registered and validated
	**/
	Class<? extends IScriptable> ExtClassType = null;

	ScriptClass(Script parent, String name)
	{
		Name = name;
		Parent = parent;
	}

	/**
	* this method returns a list of descriptions for each property the class contains.
	* it is used by the external implementation to validate the class
	**/
	PropertyDesc[] getPropertyDesc()
	{
		PropertyDesc[] p = new PropertyDesc[properties.size()];
		int i = 0;
		for (Variable v : properties)
		{
			p[i++] = new PropertyDesc(v.name, v.varType, v.classID);
		}
		return p;
	}

	MethodDesc[] getMethodDesc()
	{
		MethodDesc[] d = new MethodDesc[methods.size()];
		int i = 0;
		for (Function m : methods)
		{
			d[i] = new MethodDesc();
			d[i].Name = m.Name;
			d[i].ReturnType = m.ReturnValue != null ? m.ReturnValue.varType : TypeID.Void;
			d[i].ReturnClass = m.ReturnValue != null ? m.ReturnValue.classID : null;
			if (m.Parameters.size() > 0)
			{
				d[i].ParamDesc =  new PropertyDesc[m.Parameters.size()];
				int k = 0;
				for (Variable v : m.Parameters)
				{
					d[i].ParamDesc[k] = new PropertyDesc(v.name, v.varType, v.classID);
					k++;
				}
			}
			i++;
		}
		return d;
	}
	
	MethodDesc[] getStaticMethodDesc()
	{
		MethodDesc[] d = new MethodDesc[staticMethods.size()];
		int i = 0;
		for (Function m : staticMethods)
		{
			d[i] = new MethodDesc();
			d[i].Name = m.Name;
			d[i].ReturnType = m.ReturnValue != null ? m.ReturnValue.varType : TypeID.Void;
			d[i].ReturnClass = m.ReturnValue != null ? m.ReturnValue.classID : null;
			if (m.Parameters.size() > 0)
			{
				d[i].ParamDesc =  new PropertyDesc[m.Parameters.size()];
				int k = 0;
				for (Variable v : m.Parameters)
				{
					d[i].ParamDesc[k] = new PropertyDesc(v.name, v.varType, v.classID);
					k++;
				}
			}
			i++;
		}
		return d;
	}

	/**
	* Determines whether the current class is inheriting (directly or indirectly) the specified class.
	* @param className base class name
	**/
	public boolean isSubclassOf(String className)
	{
		if (baseClass == null)
			return false;
			
		if (baseClass.Name.equals(className))
			return true;
		else
			return baseClass.isSubclassOf(className);
	}
	
	public boolean hasMethodsWithName(String methodName) 
	{
		// search own methods:
		for (Function fn : methods)
		{
			if (fn.Name.equals(methodName))
				return true;
		}
		// if we have a base class, let it search for methods, or return null
		if (baseClass != null)
			return baseClass.hasMethodsWithName(methodName);
		else
			return false;
	}
	
	public boolean hasStaticMethodsWithName(String methodName) 
	{
		// search own methods:
		for (Function fn : staticMethods)
		{
			if (fn.Name.equals(methodName))
				return true;
		}
		return false;
	}

	public Function matchMethod(String methName, ArrayList<Expression> elist) 
	{
		for (Function f : methods) 
		{
			if (elist.size() != f.Parameters.size())
				continue;
			if (!methName.equals(f.Name))
				continue;
			
			for (int j = 0; j < elist.size(); j++)
			{
				if (TypeID.None == ExpressionNode.CheckTypes(
						f.Parameters.get(j).varType, f.Parameters.get(j).classID,
						elist.get(j).ResultType, elist.get(j).ResultClassID,
						ExpressionOperator.OpAttrib, Parent))
				
					continue;
			}
			for (int j=0; j<elist.size(); j++) 
			{
				if (f.Parameters.get(j).varType == TypeID.Float)
					elist.get(j).ResultType = TypeID.Float;
			}
			
			return f;
		}
		if (baseClass != null)
			return baseClass.matchMethod(methName, elist);
		else
			return null;
	}
	
	public Function matchStaticMethod(String methName, ArrayList<Expression> elist) 
	{
		for (Function f : staticMethods) 
		{
			if (elist.size() != f.Parameters.size())
				continue;
			if (!methName.equals(f.Name))
				continue;
			
			for (int j = 0; j < elist.size(); j++)
			{
				if (TypeID.None == ExpressionNode.CheckTypes(
						f.Parameters.get(j).varType, f.Parameters.get(j).classID,
						elist.get(j).ResultType, elist.get(j).ResultClassID,
						ExpressionOperator.OpAttrib, Parent))
				
					continue;
			}
			for (int j=0; j<elist.size(); j++) 
			{
				if (f.Parameters.get(j).varType == TypeID.Float)
					elist.get(j).ResultType = TypeID.Float;
			}
			
			return f;
		}
		return null;
	}

	public Function getEvent(String eventName) 
	{
		// search own methods:
		for (Function fn : events)
		{
			if (fn.Name.equals(eventName))
				return fn;
		}
		// if we have a base class, let it search for events, or return null
		if (baseClass != null)
			return baseClass.getEvent(eventName);
		else
			return null;
	}

	public String getName() {
		return Name;
	}
}