package XScripter;

import java.util.ArrayList;

public class InstrIf extends Instruction implements ComplexInstruction {

	InstrIf(Script parent) {
		super(parent);
	}

	Instruction IfInstr;
	Instruction ElseInstr;
	Expression Expression;

	@Override 
	void Exec(ScriptContext C)
	{
		if ((Boolean)Expression.Evaluate(C))
			IfInstr.Exec(C);
		else
			if (ElseInstr != null)
				ElseInstr.Exec(C);
	}

	@Override
	public ReturnPathCode EvaluateReturnPath()
	{
		ReturnPathCode ifret = (IfInstr instanceof ComplexInstruction) ? ((ComplexInstruction)IfInstr).EvaluateReturnPath() : 
			((IfInstr instanceof InstrReturn) ? ReturnPathCode.AllReturn : ReturnPathCode.NoReturn);
			
		if (ifret == ReturnPathCode.UnreachableCode || ifret == ReturnPathCode.PartialReturn)
			return ifret;
			
		if (ElseInstr == null)
			return ((ifret==ReturnPathCode.AllReturn || ifret==ReturnPathCode.PartialReturn) ? ReturnPathCode.PartialReturn : ifret);
		else 
		{
			ReturnPathCode elseret = (ElseInstr instanceof ComplexInstruction) ? ((ComplexInstruction)ElseInstr).EvaluateReturnPath() :
				((ElseInstr instanceof InstrReturn) ? ReturnPathCode.AllReturn : ReturnPathCode.NoReturn);

			if (elseret == ReturnPathCode.UnreachableCode || elseret == ReturnPathCode.PartialReturn)
				return elseret;
				
			if (ifret == ReturnPathCode.AllReturn && elseret == ReturnPathCode.AllReturn)
				return ReturnPathCode.AllReturn;
			else
				if (ifret == ReturnPathCode.NoReturn && elseret == ReturnPathCode.NoReturn)
					return ReturnPathCode.NoReturn;
				else
					return ReturnPathCode.PartialReturn;
		}
	}

	@Override 
	Instruction CreateOffsetedClone(Instruction prev, int varOffs)
	{
		InstrIf i = new InstrIf(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.IfInstr = IfInstr.CreateOffsetedClone(null, varOffs);
		i.ElseInstr = ElseInstr.CreateOffsetedClone(null, varOffs);
		i.Expression = Expression.CreateOffsetedClone(varOffs);
		return i;
	}

	@Override 
	boolean UpdateFunctionReferences(ArrayList<Function> functions)
	{
		if (!IfInstr.UpdateFunctionReferences(functions))
			return false;
		if (ElseInstr != null && !ElseInstr.UpdateFunctionReferences(functions))
			return false;
		
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}
}
