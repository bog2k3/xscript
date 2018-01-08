package XScripter;

import java.util.ArrayList;

public class InstrAssign extends Instruction {

		InstrAssign(Script parent) {
		super(parent);
	}

		int DestVarOffs;
		/**
		* this is the name of the external global object, if the instruction has such a destination
		**/
		String ExtGlobalName = null;
		Expression Expression;
		Expression exprIndex = null; // for writing to indexed arrays

		@Override
		void Exec(ScriptContext C) 
		{
			if (exprIndex != null) {
				Object[] array = (Object[])C.VarByID(DestVarOffs,ExtGlobalName).value;
				array[(Integer)exprIndex.Evaluate(C)] = 
					(C.VarByID(DestVarOffs, ExtGlobalName).varType.BaseType() == TypeID.Float) 
						? Tools.DblValue(Expression.Evaluate(C), C) 
						: Expression.Evaluate(C);
			} else
				C.VarByID(DestVarOffs,ExtGlobalName).value =
					(C.VarByID(DestVarOffs, ExtGlobalName).varType == TypeID.Float) 
						? Tools.DblValue(Expression.Evaluate(C), C) 
						: Expression.Evaluate(C);
		}

		@Override
		Instruction CreateOffsetedClone(Instruction prev, int varOffs)
		{
			InstrAssign i = new InstrAssign(parentScript);
			i.Prev = prev;
			if (Next != null)
				i.Next = Next.CreateOffsetedClone(i,varOffs);
			i.DestVarOffs = DestVarOffs + ((DestVarOffs & 0xC000) != 0 ? varOffs : 0);
			i.ExtGlobalName = ExtGlobalName;
			i.Expression = Expression.CreateOffsetedClone(varOffs);
			if (exprIndex != null)
				i.exprIndex = exprIndex.CreateOffsetedClone(varOffs);
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
