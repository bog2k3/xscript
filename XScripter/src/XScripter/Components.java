package XScripter;

import java.util.ArrayList;

enum ExpressionNodeType
{
	Value,
	Operator,
	Variable,
}

/**
* this object contains references to every context element needed at compile time or at run time
**/
class ScriptContext
{
	/** the script whithin which we work **/
	Script ParentScript;
	/** reference to an Executor object. this is valid during run-time. **/
	Executor Executor = null;
	
	/** under normal circumstances this should be null, however if some runtime error occurs, 
	 * it will contain the error description */
	RuntimeError Error = null;
	
	/** the current instruction-block stack **/
	ArrayList<InstructionBlock> BlockStack = null;
	/** varible stack **/
	ArrayList<Variable> VarStack = null;
	/** function call stack **/
	ArrayList<Function> FuncStack = null;

	/** currently compiling function **/
	Function CurrentFunction;
	InstructionBlock CurrentInstrBlock()
	{
		return BlockStack.get(BlockStack.size() - 1); 
	}

	/**
	* returns a handle to a variable with the specified local/global index or external global identified
	* by the given name
	 * @throws Exception 
	**/
	Variable VarByID(int id, String extGlobalName)
	{
		if ((id & 0x8000) != 0)
			return ParentScript.variables.get(id & 0x3fff);
		else
			if ((id&0x4000) != 0)
			{
				Object v = ParentScript.extGlobals.get(extGlobalName);
				if (v == null)
				{
					ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.ExtGlobalNotFound, this, "ExtGlobal Name: " + extGlobalName));
					return null;
				}
				return (Variable)v;
			}
		else
			return VarStack.get(id + (CurrentFunction != null ? CurrentFunction.nBaseVarOffs : 0));
	}

	/**
	* this list contains references to all temporary variables that were used by the previously
	* read instruction. the variables must be reset to TypeID.None, and the list cleared
	* after each instruction is read.
	**/
	ArrayList<Variable> TempVars = new ArrayList<Variable>();
	
	/** holds null or a reference to an implementation of ILibraryFetcher interface,
	 * during compile time. if it's not null, it is invoked when an #import directive is 
	 * found, in order to retrieve the library represented by the import string.
	 */
	public ILibraryFetcher libFetcher;
	
	/** stops the execution of the script and uses the user-defined runtime error handler (if present)
	 * to report the error.
	 * @param err
	 */
	void ThrowRuntimeError(RuntimeError err)
	{
		if (err.code != RuntimeErrorCode.DebugRequested)
			Error = err;
		if (Executor.m_RTEHandler != null)
			Executor.m_RTEHandler.handleScriptRuntimeError(err);
	}
}

/**
* this holds a tree describing an expression, and it's result type
**/
class Expression
{
	Script ParentScript = null;
	
	Expression(Script parent)
	{
		ParentScript = parent;
	}
	
	ExpressionNode RootNode;
	transient ArrayList<ExpressionNode> AuxNodes = new ArrayList<ExpressionNode>();

	TypeID ResultType;
	/** this holds the id of the registered class type for TypeID.Object result types **/
	String ResultClassID = null;

	Object Evaluate(ScriptContext C)
	{
		return RootNode.Evaluate(C);
	}

	Expression CreateOffsetedClone(int varOffs)
	{
		Expression e = new Expression(ParentScript);
		e.RootNode = RootNode.CreateOffsetedClone(e,varOffs);
		e.ResultClassID = ResultClassID;
		e.ResultType = ResultType;
		
		return e;
	}
}

class InstructionBlock
{
	Instruction RootInstr = null;
	
	transient Instruction CurrentInstr = null;
	ArrayList<Variable> LocalVars = new ArrayList<Variable>();

	/** this is set to true only on child blocks of repetitive structures **/
	boolean breakable = false;
	transient boolean AdvanceInstruction = true; //TODO de pus transient pe unde mai trebuie, si in GUI
	Script parentScript;
	
	InstructionBlock(Script script)
	{
		parentScript = script;
	}

	Variable CreateVariable(TypeID type)
	{
		Variable v = new Variable(type);
		LocalVars.add(v);
		return v;
	}

	void AddInstruction(Instruction instr)
	{
		if (RootInstr == null)
			RootInstr = CurrentInstr = instr;
		else
		{
			instr.Prev = CurrentInstr;
			CurrentInstr = (CurrentInstr.Next = instr);
		}
	}

	InstructionBlock CreateInstance()
	{
		InstructionBlock i = new InstructionBlock(parentScript);
		i.LocalVars = LocalVars;
		i.RootInstr = RootInstr;
		i.CurrentInstr = RootInstr;
		i.breakable = breakable;
		return i;
	}
	
	/** check for unreachable code situations or partial return paths.
	 **/
	ReturnPathCode CheckReturnPaths()
	{
		Instruction i = RootInstr;
		boolean partial = false;
		while (i != null)
		{
			ReturnPathCode ret;
			if (i instanceof ComplexInstruction)
				ret = ((ComplexInstruction)i).EvaluateReturnPath();
			else
				ret = (i instanceof InstrReturn) ? ReturnPathCode.AllReturn : ReturnPathCode.NoReturn;
				
			if (ret == ReturnPathCode.UnreachableCode)
				return ReturnPathCode.UnreachableCode;
				
			if (ret == ReturnPathCode.PartialReturn)
				partial = true;
			
			if (ret == ReturnPathCode.AllReturn && i.Next != null && !(i instanceof ComplexInstruction))
				return ReturnPathCode.UnreachableCode;
				
			if (i.Next == null)
			{
				if (partial && ret == ReturnPathCode.AllReturn)
					return ret;
				if (partial && ret == ReturnPathCode.NoReturn)
					return ReturnPathCode.PartialReturn;
				return ret;
			}
			
			i = i.Next;
		}
		return ReturnPathCode.NoReturn;
	}

	/**
	* Removes last instruction from this block
	 * @throws Exception 
	**/
	void RemoveLast()
	{
		if (RootInstr == null)
		{
			parentScript.SetError(new ScriptError(ScriptErrorCode.InternalError, parentScript, 0, null, "Instruction block is empty"));
			return;
		}
			
		if (RootInstr == CurrentInstr)
			RootInstr = CurrentInstr = null;
		else
		{
			CurrentInstr = CurrentInstr.Prev;
			CurrentInstr.Next = null;
		}
	}

	InstructionBlock CreateOffsetedClone(int varOffs)
	{
		InstructionBlock b = new InstructionBlock(parentScript);
		b.breakable = breakable;
		b.LocalVars = LocalVars;
		// clone instructions recursively:
		b.RootInstr = RootInstr.CreateOffsetedClone(null, varOffs);
		return b;
	}

	boolean UpdateFunctionReferences(ArrayList<Function> Functions)
	{
		return RootInstr.UpdateFunctionReferences(Functions);
	}
}

enum ReturnPathCode
{
	NoReturn,
	AllReturn,
	PartialReturn,
	UnreachableCode,
}

class TextStream
{
	String Text = null;
	int cursor = 0;
	int current_line = 1;
	int length;
	
	private int saveCursor;
	private int saveCrtLine;
	
	void SavePos()
	{
		saveCursor = cursor;
		saveCrtLine = current_line;
	}
	
	void RestorePos()
	{
		cursor = saveCursor;
		current_line = saveCrtLine;
	}

	TextStream(String T)
	{
		Text = T;
		Reset();
	}
	
	void Reset() 
	{
		cursor = 0;
		current_line = 1;
		length = Text.length();
		SavePos();
	}
	
	boolean eof()
	{
		return cursor >= length;
	}

	char Pick()
	{
		return Text.charAt(cursor);
	}
	
	String PickString()
	{
		return Character.toString(Pick());
	}
	
	char PickAt(int pos)
	{
		return Text.charAt(pos);
	}
	
	/**
	* skips white spaces and line endings
	**/
	void Advance(int amount)
	{
		if (amount < 0) // negative values mean only increase pointer, don't skip spaces (for reading strings)
		{
			cursor -= amount;
			return;
		}
		
		cursor += amount;
		
		if (cursor >= length)
			return;
		char c = Text.charAt(cursor);  
		while (c == ' ' || c == 0xff)
		{
			if (c == 0xff)
				current_line++;
			cursor++;
			if (cursor >= length) 
				return;
			c = Text.charAt(cursor);
		}
	}
}
