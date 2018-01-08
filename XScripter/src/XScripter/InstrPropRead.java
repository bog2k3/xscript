package XScripter;

import java.util.ArrayList;

public class InstrPropRead extends Instruction 
{
	InstrPropRead(Script parent) {
		super(parent);
	}

	/**
	* id of object variable
	**/
	int ObjectID;
	/**
	* this is the name of the external global object instance, if the instruction references such an object.
	**/
	String ObjectName = null;
	
	/** if this is true, then the instruction attepts to read a static field from the class*/
	boolean staticRead;
	
	/** for static reads, this keeps the name of the class we read from */
	String className;
	
	/** if this is different from null, then the object from wich the property is read, is taken from an array
	 * at the index obtained by evaluating this expression. */
	Expression exprIndexFrom = null;
	/** if this is != null, then we read from an array-property at the index obtained by evaluating
	 * this expression. */
	Expression exprIndexInto = null;
	/**
	* prototype of property to read (this is a reference of a property field from the target class)
	**/
	Variable Property;
	/**
	* id of destination variable to store the result in.
	**/
	int DestVarID;
	
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
			if (!staticRead)
				source = C.VarByID(ObjectID, ObjectName).value;
		
		ScriptClass cls;
		if (staticRead)
			cls = C.ParentScript.extClasses.get(className);
		else
			cls = C.ParentScript.extClasses.get(C.VarByID(ObjectID, ObjectName).classID);
		
		if (cls == null || cls.ExtClassType == null) {
			String clsName;
			if (staticRead)
				clsName = className;
			else
				clsName = C.VarByID(ObjectID, ObjectName).classID;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ClassNotRegistered, C, clsName));
			return;
		}
		
		if (source == null && !staticRead)
		{
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "reading property ("+Property.name+") on null object."));
			return;
		}

		ExtResult res;
		if (staticRead)
			res = IScriptable.getStaticFieldValue(cls, Property.name);
		else
			res = ((IScriptable)source).getPropertyValue(Property.name);
		
		if (res.Error != null)
		{
			if (staticRead)
				res.Error = "Class : " + className + "; READING static field : " + Property.name + "\n" + res.Error;
			else
				res.Error = "Class : " + C.VarByID(ObjectID, ObjectName).classID + "; READING Property : " + Property.name + "\n" + res.Error;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.PropertyAccessFailed, C, res.Error));
			return;
		}
		
		if (exprIndexInto != null) {
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
			C.VarByID(DestVarID, null).value = array[propIndex];
		} else
			C.VarByID(DestVarID, null).value = res.Result;
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrPropRead i = new InstrPropRead(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ObjectID = ObjectID + ((ObjectID & 0x8000) != 0 ? varOffs : 0);
		i.ObjectName = ObjectName;
		i.staticRead = staticRead;
		i.className = className;
		i.Property = Property;
		i.DestVarID = DestVarID;
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
