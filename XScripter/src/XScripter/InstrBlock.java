package XScripter;

import java.util.ArrayList;

class InstrBlock extends Instruction implements ComplexInstruction 
{
	InstrBlock(Script parent) {
		super(parent);
	}

	InstructionBlock Block;

	@Override
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrBlock i = new InstrBlock(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.Block = Block.CreateOffsetedClone(varOffs);
		return i;
	}

	@Override
	void Exec(ScriptContext C) 
	{
		C.Executor.PushBlock(Block.CreateInstance());
		C.CurrentInstrBlock().AdvanceInstruction = false;
	}

	@Override
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		if (!Block.UpdateFunctionReferences(functions))
			return false;
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}

	@Override
	public ReturnPathCode EvaluateReturnPath() 
	{
		return Block.CheckReturnPaths();
	}

}
