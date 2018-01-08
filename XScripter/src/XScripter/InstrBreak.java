package XScripter;

import java.util.ArrayList;

public class InstrBreak extends Instruction 
{

	InstrBreak(Script parent) {
		super(parent);
	}

	/**
	* how many blocks to break through. this is determined at compile time.
	**/
	int BlockCount = 0;
	
	@Override 
	void Exec(ScriptContext C)
	{
		for (int i=0; i<BlockCount; i++)
			C.Executor.PopBlock();
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrBreak i = new InstrBreak(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.BlockCount = BlockCount;
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
