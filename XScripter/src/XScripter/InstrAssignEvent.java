package XScripter;

import java.util.ArrayList;

public class InstrAssignEvent extends Instruction 
{
	
	/**
	* id of object variable
	**/
	int ObjectID;
	/**
	* this is the name of the external global object instance, if the instruction references such an object.
	**/
	String ObjectName;

	/**
	* name of the event from the class
	**/
	String eventName;
	
	/** 
	 * the name of the handler to be assigned to the event
	 */
	String handlerName;
	
	/** if this is different from null, then the object to which the event listener is assigned, 
	 * is taken from an array at the index obtained by evaluating this expression. */
	Expression exprIndexFrom = null;

	InstrAssignEvent(Script parent) 
	{
		super(parent);
	}

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
			source = C.VarByID(ObjectID, ObjectName).value;
		
		ScriptClass cls = C.ParentScript.extClasses.get(C.VarByID(ObjectID, ObjectName).classID);
		if (cls == null || cls.ExtClassType == null) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ClassNotRegistered, C, C.VarByID(ObjectID, ObjectName).classID));
			return;
		}

		ExtResult res = ((IScriptable)source).assignEvent(C.Executor, eventName, handlerName);
		if (res != null)
		{
			res.Error = "Class : " + C.VarByID(ObjectID, ObjectName).classID + "; ASSIGNING Event : " + eventName + "\n" + res.Error;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.PropertyAccessFailed, C, res.Error));
			return;
		}
	}

	@Override
	Instruction CreateOffsetedClone(Instruction prev, int varOffs) 
	{
		InstrAssignEvent i = new InstrAssignEvent(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ObjectID = ObjectID + ((ObjectID & 0x8000) != 0 ? varOffs : 0);
		i.ObjectName = ObjectName;
		i.eventName = eventName;
		i.handlerName = handlerName;
		if (exprIndexFrom != null)
			i.exprIndexFrom = exprIndexFrom.CreateOffsetedClone(varOffs);
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
