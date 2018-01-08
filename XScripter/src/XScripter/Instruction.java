package XScripter;

import java.util.ArrayList;

/**
 * this is used by repetitive macro-instructions such as while/for/do to describe their current execution state.
**/
enum LoopState
{
	Idle,
	Evaluate,
	Exec,
}

abstract class Instruction 
{
	abstract void Exec(ScriptContext C);
		
	Instruction Next = null;
	Instruction Prev = null;
	final Script parentScript;
	
	Instruction(Script parent) {
		parentScript = parent;
	}

	/** 
	 * this method clones the current instruction, if the instruction contains any GLOBAL variable references, they
	 * must be offseted by varOffs (+), and the next instruction must be cloned recursively.
	 * @throws Exception 
	 * @param varOffs
	 * @param prev
	**/
	abstract Instruction CreateOffsetedClone(Instruction prev, int varOffs);
	
	/**
	 * this method must update all function references from the current instruction with the ones specified in
	 * the list given as parameter. The functions are searched by name.
	 * The next instruction must be updated recursively.
	 * @throws Exception
	 * @param functions
	 */
	abstract boolean UpdateFunctionReferences(ArrayList<Function> functions);
}

/**
 * this interface is implemented by those instructions that are made up of
 * several standard or simple instructions (such as blocks, if, for, etc)
 * they must implement the EvaluateReturnPath function to check all possible execution paths
 * for return statements.
**/
interface ComplexInstruction
{
	ReturnPathCode EvaluateReturnPath();
}

