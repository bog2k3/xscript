/**
 * 
 */
package XScripter;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * @author bog
 * Creates a new array and assigns the value to a variable
 */
public class InstrNewArray extends Instruction {
	
	InstrNewArray(Script parent) {
		super(parent);
	}

	int destVarID;
	Expression exprSize;
	String classID;
	TypeID elementType;

	@Override
	Instruction CreateOffsetedClone(Instruction prev, int varOffs) {
		InstrNewArray i = new InstrNewArray(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		i.destVarID = destVarID + ((destVarID & 0x8000) != 0 ? varOffs : 0);
		i.exprSize = exprSize.CreateOffsetedClone(varOffs);
		return i;
	}

	@Override
	void Exec(ScriptContext C) {
		Integer size = (Integer)exprSize.Evaluate(C);
		if (C.Error != null)
			return;
		if (size <= 0) {
			C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidArraySize,C,size.toString()));
			return;
		}

		Class<?> clazz;
		if (elementType == TypeID.Object)
			clazz = C.ParentScript.extClasses.get(classID).ExtClassType;
		else
			clazz = elementType.GetRuntimeInternalClass();
		C.VarByID(destVarID,null).value = Array.newInstance(clazz, size);
	}

	@Override
	boolean UpdateFunctionReferences(ArrayList<Function> functions) {
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}

}
