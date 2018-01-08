package XScripter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class InstrNew extends Instruction 
{
	InstrNew(Script parent) {
		super(parent);
	}

	String ClassID;
	int DestVarID;
	ArrayList<Expression> paramList;

	@Override 
	void Exec(ScriptContext C)
	{
		Class<? extends IScriptable> ClassType = C.ParentScript.GetClass(ClassID).ExtClassType;
		
		if (ClassType == null)
		{
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ClassNotRegistered, C, C.ParentScript.GetClass(ClassID).Name));
			return;
		}
		
		// evaluate each expression in the parameter list:
		Object[] l = new Object[paramList.size()];
		for (int i = 0; i < paramList.size(); i++)
		{
			if (paramList.get(i).ResultType == TypeID.Float)
				l[i] = Tools.DblValue(paramList.get(i).Evaluate(C), C);
			else
			if (paramList.get(i).ResultType == TypeID.Int)
				l[i] = Tools.IntValue(paramList.get(i).Evaluate(C), C);
			else
				l[i] = paramList.get(i).Evaluate(C);
			
			if (C.Error != null)
				return;
		}
		
		Class<?>[] clsDesc = new Class[l.length];
		for (int i=0; i<l.length; i++)
			clsDesc[i] = l[i].getClass();

		Constructor<? extends IScriptable> constr = null;
		try {
			constr = ClassType.getConstructor(clsDesc);
		} catch (Exception e) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ConstructorNotFound, C, 
					"Exception thrown while trying to retrieve a matching constructor for class " + ClassID
					+ "\nException: " + e.toString()));
			e.printStackTrace();
		}
			
		try {
			C.VarByID(DestVarID,null).value = constr.newInstance(l);
		} catch (Exception e) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ExceptionThrown, C, 
					"Exception thrown while trying to instantiate class " + ClassID
					+ "\nException: " + e.toString()));
			e.printStackTrace();
		}
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrNew i = new InstrNew(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ClassID = ClassID;
		i.DestVarID = DestVarID + ((DestVarID & 0x8000) != 0 ? varOffs : 0);
		i.paramList = new ArrayList<Expression>();
		for (Expression e : paramList)
		{
			i.paramList.add(e.CreateOffsetedClone(varOffs));
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
