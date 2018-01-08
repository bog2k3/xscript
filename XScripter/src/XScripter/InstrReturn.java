package XScripter;

import java.util.ArrayList;

public class InstrReturn extends Instruction
{
	InstrReturn(Script parent) {
		super(parent);
	}

	Expression ReturnValue;

	@Override
	void Exec(ScriptContext C)
	{

		C.Executor.PopFunction(ReturnValue != null ? ReturnValue.Evaluate(C) : null);
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrReturn i = new InstrReturn(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.ReturnValue = ReturnValue.CreateOffsetedClone(varOffs);
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
