package XScripter;

import java.util.ArrayList;

public class InstrMethodCall extends Instruction {

	InstrMethodCall(Script parent) {
		super(parent);
	}

	/**
	* id of variable that contains the object to call from
	**/
	int ObjectID;
	/**
	* this is the name of the external global object instance, if the instruction references such an object.
	**/
	String ObjectName = null;
	/**
	* name of the method to call
	**/
	String MethodName; 
	/**
	* parameters for the called method
	**/
	ArrayList<Expression> Parameters;
	
	/** if this is true, then the instruction attepts to call a static method from the class*/
	boolean staticCall;
	
	/** for static calls, this keeps the name of the class we call from */
	String className;
	
	/** if the method is called on an object from an array, this gives the index of the object */
	Expression exprIndexFrom = null;

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
			if (!staticCall)
				source = C.VarByID(ObjectID, ObjectName).value;
		
		ScriptClass cls;
		if (staticCall)
			cls = C.ParentScript.extClasses.get(className);
		else
			cls = C.ParentScript.extClasses.get(C.VarByID(ObjectID, ObjectName).classID);
		if (cls == null || cls.ExtClassType == null) {
			String clsName;
			if (staticCall)
				clsName = className;
			else
				clsName = C.VarByID(ObjectID, ObjectName).classID;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ClassNotRegistered, C, clsName));
			return;
		}
		
		if (source == null && !staticCall)
		{
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "Calling method ("+MethodName+") on null object."));
			return;
		}
		
		// evaluate each expression in the parameter list:
		Object[] l = new Object[Parameters.size()];
		for (int i = 0; i < Parameters.size(); i++)
		{
			if (Parameters.get(i).ResultType == TypeID.Float)
				l[i] = Tools.DblValue(Parameters.get(i).Evaluate(C), C);
			else
			if (Parameters.get(i).ResultType == TypeID.Int)
				l[i] = Tools.IntValue(Parameters.get(i).Evaluate(C), C);
			else
				l[i] = Parameters.get(i).Evaluate(C);
			
			if (C.Error != null)
				return;
		}
		
		ExtResult res;
		if (staticCall)
			res = IScriptable.callStaticMethod(cls, MethodName, l);
		else
			res = ((IScriptable)source).callMethod(MethodName, l);

		if (res.Error != null)
		{
			if (staticCall)
				res.Error = "Class : " + className + "; static method : " + MethodName + "\n" + res.Error;
			else
				res.Error = "Class : " + C.VarByID(ObjectID, ObjectName).classID + "; Method : " + MethodName + "\n" + res.Error;
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.MethodInvokeFailed, C, res.Error));
			return;
		}
		
		C.VarByID(0, null).value = res.Result;
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrMethodCall i = new InstrMethodCall(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ObjectID = ObjectID + ((ObjectID & 0x8000) != 0 ? varOffs : 0);
		i.ObjectName = ObjectName;
		i.MethodName = MethodName;
		i.staticCall = staticCall;
		i.className = className;
		i.Parameters = new ArrayList<Expression>();
		i.exprIndexFrom = exprIndexFrom.CreateOffsetedClone(varOffs);
		for (Expression e : Parameters)
		{
			i.Parameters.add(e.CreateOffsetedClone(varOffs));
		}
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
