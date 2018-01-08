package XScripter;

import java.util.ArrayList;

public class InstrDebug extends Instruction {

	InstrDebug(Script parent) {
		super(parent);
	}

	@Override
	void Exec(ScriptContext C) {
		C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.DebugRequested, C, "DEBUG BREAK requested from script : " + C.ParentScript.FileName));
	}

	@Override
	Instruction CreateOffsetedClone(Instruction prev, int varOffs) {
		InstrDebug i = new InstrDebug(parentScript);
		i.Prev = prev;
		if (Next != null)
			i.Next = Next.CreateOffsetedClone(i, varOffs);
		return i;
	}

	@Override
	boolean UpdateFunctionReferences(ArrayList<Function> functions) {
		if (Next != null) 
			return Next.UpdateFunctionReferences(functions);
		else
			return true;
	}

}
