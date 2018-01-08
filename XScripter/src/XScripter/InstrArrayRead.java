package XScripter;

import java.util.ArrayList;

public class InstrArrayRead extends Instruction {
	
	InstrArrayRead(Script parent) {
		super(parent);
	}

	int destVarID;
	int arrayID;
	Expression exprIndex;
	String extArrayName;

	@Override
	Instruction CreateOffsetedClone(Instruction prev, int varOffs) {
		InstrArrayRead i = new InstrArrayRead(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i,varOffs);
		i.destVarID = destVarID + ((destVarID & 0xC000) != 0 ? varOffs : 0);
		i.extArrayName = extArrayName;
		i.arrayID = arrayID + ((arrayID & 0xC000) != 0 ? varOffs : 0);
		i.exprIndex = exprIndex.CreateOffsetedClone(varOffs);
		return i;
	}

	@Override
	void Exec(ScriptContext C) {
		Integer index = (Integer)exprIndex.Evaluate(C);
		if (C.Error != null)
			return;
		Object[] array = (Object[])C.VarByID(arrayID, extArrayName).value;
		if (array == null) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.NullAccess, C, "Array is not initialized."));
			return;
		}
		if (index < 0 || index >= array.length) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ArrayIndexOutOfBounds, C, "Array size is "+String.valueOf(array.length)+", index is "+index.toString()));
			return;
		}
		
		C.VarByID(destVarID, null).value = (C.VarByID(destVarID, null).varType == TypeID.Float)
				? Tools.DblValue(array[index], C)
				: array[index];
	}

	@Override
	boolean UpdateFunctionReferences(ArrayList<Function> functions) {
		if (Next != null)
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}

}
