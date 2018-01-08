package XScripter;

import java.util.ArrayList;

public class InstrWhile extends Instruction implements ComplexInstruction 
{
	InstrWhile(Script parent) {
		super(parent);
	}

	InstructionBlock EvalBlock;
	InstructionBlock DoBlock;
	/**
	* this is the id of the temporary variable in the same block as the while statement,
	* where the value of the expression is saved during the evaluation phase.
	**/
	int EvalTempVarID;
	
	transient private LoopState State = LoopState.Idle;
	
	@Override 
	void Exec(ScriptContext C)
	{
		switch (State)
		{
			case Exec:
			case Idle:
				// signal the parent block to keep executing this instruction
				C.CurrentInstrBlock().AdvanceInstruction = false;
				// push the eval block and wait for it to finish.
				C.Executor.PushBlock(EvalBlock.CreateInstance());
				C.CurrentInstrBlock().AdvanceInstruction = false;
				State = LoopState.Evaluate;
				break;
			case Evaluate:
				// the eval block completed.
				if ((Boolean)C.VarByID(EvalTempVarID,null).value)
				{
					C.CurrentInstrBlock().AdvanceInstruction = false;
					C.Executor.PushBlock(DoBlock.CreateInstance());
					State = LoopState.Exec;
					C.CurrentInstrBlock().AdvanceInstruction = false;
				}
				else
				{
					State = LoopState.Idle;
				}
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
		InstrWhile i = new InstrWhile(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.EvalBlock = EvalBlock.CreateOffsetedClone(varOffs);
		i.DoBlock = DoBlock.CreateOffsetedClone(varOffs);
		i.EvalTempVarID = EvalTempVarID;
		
		return i;
	}

	@Override 
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		if (!EvalBlock.UpdateFunctionReferences(functions))
			return false;
		if (!DoBlock.UpdateFunctionReferences(functions))
			return false;
		
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}
}
