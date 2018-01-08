package XScripter;

import java.util.ArrayList;

public class InstrDo extends Instruction implements ComplexInstruction {

	InstrDo(Script parent) {
		super(parent);
	}

	InstructionBlock DoBlock;
	int EvalVarID;
	
	transient LoopState State = LoopState.Idle;
	
	@Override 
	void Exec(ScriptContext C)
	{
		switch (State)
		{
			case Idle:
				C.CurrentInstrBlock().AdvanceInstruction = false;
				C.Executor.PushBlock(DoBlock.CreateInstance());
				C.CurrentInstrBlock().AdvanceInstruction = false;
				State = LoopState.Exec;
				break;
			case Exec:
				State = LoopState.Idle;
				if (!(Boolean)C.VarByID(EvalVarID,null).value)
					Exec(C);
				break;
		}
	}

	@Override
	public ReturnPathCode EvaluateReturnPath()
	{
		return DoBlock.CheckReturnPaths();
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrDo i = new InstrDo(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.DoBlock = DoBlock.CreateOffsetedClone(varOffs);
		i.EvalVarID = EvalVarID;
		
		return i;
	}

	@Override 
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		if (!DoBlock.UpdateFunctionReferences(functions))
			return false;
		
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}

}
