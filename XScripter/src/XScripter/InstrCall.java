package XScripter;

import java.util.ArrayList;

public class InstrCall extends Instruction 
{
	InstrCall(Script parent) {
		super(parent);
		intrinsic = false;
	}

	ArrayList<Expression> parameters;
	Function func;
	
	boolean intrinsic;

	@Override
	void Exec(ScriptContext C)
	{
		// evaluate each expression in the parameter list:
		ArrayList<Variable> l = new ArrayList<Variable>();
		for (int i = 0; i < func.Parameters.size(); i++)
		{
			Variable v = new Variable(func.Parameters.get(i).varType); 
			l.add(v);
			if (v.varType == TypeID.Float)
				v.value = (Double)parameters.get(i).Evaluate(C);
			else
				v.value = parameters.get(i).Evaluate(C);
			
			v.classID = parameters.get(i).ResultClassID;
		}
		// check for intrinsic function
		if (intrinsic) 
		{
			IntrinsicFunction ifunc = (IntrinsicFunction)func;
			C.VarByID(0,null).value = ifunc.exec(l);
		}
		else {
			// now call the function:
			C.Executor.PushFunction(func, l);
			C.CurrentInstrBlock().AdvanceInstruction = false;
		}
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrCall i = new InstrCall(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.func = func;
		i.parameters = new ArrayList<Expression>();
		for (Expression e : parameters)
		{
			i.parameters.add(e.CreateOffsetedClone(varOffs));
		}
		return i;
	}

	@Override 
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		boolean found = false;
		for (Function f : functions)
		{
			if (f.Name == func.Name)
			{
				func = f;
				found = true;
				break;
			}
		}
		if (!found)
		{
			parentScript.SetError( new ScriptError(ScriptErrorCode.InternalError, parentScript, 0, null, 
					"Local copy of function not found : "+func.Name));
			return false;
		}
			
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}
	
}
