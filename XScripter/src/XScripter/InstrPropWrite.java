package XScripter;

import java.util.ArrayList;

public class InstrPropWrite extends Instruction 
{
	InstrPropWrite(Script parent) {
		super(parent);
	}

	/**
	* id of object variable
	**/
	int ObjectID;
	/**
	* this is the name of the external global object instance, if the instruction references such an object.
	**/
	String ObjectName;
	
	/** if this is true, then the instruction attepts to write to a static field in the class*/
	boolean staticWrite;
	
	/** for static writes, this keeps the name of the class we write to */
	String className;

	/**
	* prototype of property to write to (this is a reference of a property field from the target class)
	**/
	Variable Property;
	
	/** if this is different from null, then the object from wich the property is read, is taken from an array
	 * at the index obtained by evaluating this expression. */
	Expression exprIndexFrom = null;
	/** if this is != null, then we read from an array-property at the index obtained by evaluating
	 * this expression. */
	Expression exprIndexInto = null;
	
	/**
	* Expression to assign to the property.
	**/
	Expression exprValue;

	@Override 
	void Exec(ScriptContext C)
	{
		Object source = null;
		if (exprIndexFrom != null) {
			Integer iSourceIndex = (Integer)exprIndexFrom.Evaluate(C);
			if (C.Error != null)
				return;
			Object[] array = ((Object[])C.VarByID(ObjectID, ObjectName).value);
			if (array == null) {
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "Array is not initialized."));
				return;
			}
			if (iSourceIndex < 0 || iSourceIndex >= array.length) {
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ArrayIndexOutOfBounds, C, "Array size is " + String.valueOf(array.length)+", index is " + iSourceIndex.toString()));
				return;
			}
			source = array[iSourceIndex];
		} else
			if (!staticWrite)
				source = C.VarByID(ObjectID, ObjectName).value;
		
		ScriptClass cls;
		if (staticWrite)
			cls = C.ParentScript.extClasses.get(className);
		else
			cls = C.ParentScript.extClasses.get(C.VarByID(ObjectID, ObjectName).classID);
		
		if (cls == null || cls.ExtClassType == null) {
			String clsName;
			if (staticWrite)
				clsName = className;
			else
				clsName = C.VarByID(ObjectID, ObjectName).classID;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ClassNotRegistered, C, clsName));
			return;
		}
		
		if (source == null && !staticWrite)
		{
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "writing property ("+Property.name+") on null object."));
			return;
		}

		if (exprIndexInto != null) {
			ExtResult res;
			if (staticWrite)
				res = IScriptable.getStaticFieldValue(cls, Property.name);
			else
				res = ((IScriptable)source).getPropertyValue(Property.name);
			if (res.Error != null)
			{
				if (staticWrite)
					res.Error = "Class : " + className + "; READING static field : " + Property.name + "\n" + res.Error;
				else
					res.Error = "Class : " + C.VarByID(ObjectID, ObjectName).classID + "; READING Property : " + Property.name + "\n" + res.Error;
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.PropertyAccessFailed, C, res.Error));
				return;
			}
			Object[] array = (Object[])res.Result;
			
			if (array == null) {
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "Array is not initialized."));
				return;
			}
			Integer propIndex = (Integer)exprIndexInto.Evaluate(C);
			if (C.Error != null)
				return;
			if (propIndex < 0 || propIndex >= array.length) {
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ArrayIndexOutOfBounds, C, "Array size is " + String.valueOf(array.length)+", index is " + propIndex.toString()));
				return;
			}
			Object value = exprValue.Evaluate(C);
			if (C.Error != null)
				return;
			if (Property.varType == TypeID.Float)
				value = Tools.DblValue(value,C);
			if (C.Error != null)
				return;
			array[propIndex] = value; 
		} 
		else 
		{
			Object value = exprValue.Evaluate(C);
			if (C.Error != null)
				return;
			if (Property.varType == TypeID.Float)
				value = Tools.DblValue(value,C);
			if (C.Error != null)
				return;
			ExtResult res;
			if (staticWrite)
				res = IScriptable.setStaticFieldValue(cls, Property.name, value);
			else
				res = ((IScriptable)source).setPropertyValue(Property.name, value);
			if (res != null)
			{
				if (staticWrite)
					res.Error = "Class : " + className + "; WRITING static field : " + Property.name + "\n" + res.Error;
				else
					res.Error = "Class : " + C.VarByID(ObjectID, ObjectName).classID + "; WRITING Property : " + Property.name + "\n" + res.Error;
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.PropertyAccessFailed, C, res.Error));
				return;
			}
		}
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrPropWrite i = new InstrPropWrite(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ObjectID = ObjectID + ((ObjectID & 0x8000) != 0 ? varOffs : 0);
		i.ObjectName = ObjectName;
		i.className = className;
		i.staticWrite = staticWrite;
		i.Property = Property;
		i.exprValue = exprValue.CreateOffsetedClone(varOffs);
		if (exprIndexFrom != null)
			i.exprIndexFrom = exprIndexFrom.CreateOffsetedClone(varOffs);
		if (exprIndexInto != null)
			i.exprIndexInto = exprIndexInto.CreateOffsetedClone(varOffs);
		return i;
	}

	@Override 
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}
}
