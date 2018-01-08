package XScripter;

import java.util.ArrayList;

/**
* this class is used to run scripts.
**/
public class Executor
{
	private boolean m_running = false;
	/**
	* true if the executor is active (running a function).
	* false if the execution finished, or didn't start.
	**/
	public boolean Running() { return m_running; }

	private ScriptContext context = null;
	
	public final Script script;
	
	RuntimeErrorHandler m_RTEHandler = null;
	/** sets a user-defined runtime error handler for the script */
	public void SetRuntimeErrorHandler(RuntimeErrorHandler h) { m_RTEHandler = h; }
	
	public Executor(Script script) {
		this.script = script;
	}

	/**
	* this function starts the execution at the specified point.
	* Once this function is called, this object gets bound to that execution stream,
	* and cannot start another execution until this one has finished.
	*
	* @param entry entry point to execute from 
	* @param mode execution mode 
	 * @throws Exception 
	**/
	public RuntimeError Execute(EntryPoint entry, RunMode mode)
	{
		if (m_running)
		{
			RuntimeError err = new RuntimeError(RuntimeErrorCode.ExecutorBound, context, "This Executor is currently bound to another script: " + context.ParentScript.Name);
			if (m_RTEHandler != null)
				m_RTEHandler.handleScriptRuntimeError(err);
			return err;
		}

		context = new ScriptContext();
		context.BlockStack = new ArrayList<InstructionBlock>();
		context.Executor = this;
		context.FuncStack = new ArrayList<Function>();
		context.ParentScript = script;
		context.VarStack = new ArrayList<Variable>();
		Function f = script.MatchFuncVersion(entry.FunctionName, entry.Parameters);
		// check if the requested function exists:
		if (f == null)
		{
			String funcSig = entry.FunctionName + "(";
			for (int n=entry.Parameters.length, i=0; i<n; i++)
			{
				Class<?> paramClass = entry.Parameters[i].getClass(); 
				TypeID paramType = TypeID.fromClass(paramClass);
				if (paramType == TypeID.Object || paramType == TypeID.ObjectArray)
					funcSig += paramClass;
				else
					funcSig += paramType.toString();
				if (i < n-1)
					funcSig += ", ";
				else
					funcSig += ")";
			}
			RuntimeError err = new RuntimeError(RuntimeErrorCode.FunctionVersionNotFound, context, funcSig);
			if (m_RTEHandler != null)
				m_RTEHandler.handleScriptRuntimeError(err);
			context = null;
			return err;
		}
		if (f.ReturnValue != null)
		{
			context.VarStack.add(new Variable(f.ReturnValue.varType));
			entry.functionReturn = context.VarStack.get(0);
		}
		// do a preliminary parameter check:
		if (f.Parameters.size() != entry.Parameters.length)
		{
			RuntimeError err = new RuntimeError(RuntimeErrorCode.ArgumentCountMismatch, context, "Calling function : " + entry.FunctionName);
			if (m_RTEHandler != null)
				m_RTEHandler.handleScriptRuntimeError(err);
			context = null;
			return err;
		}
		// type-check parameters and add them to a list:
		ArrayList<Variable> lParam = new ArrayList<Variable>();
		for (int i = 0; i < f.Parameters.size(); i++)
		{
			Variable v = new Variable(f.Parameters.get(i).varType);
			v.classID = f.Parameters.get(i).classID;
			// type-checking
			switch (v.varType)
			{
				case Int:
					if (entry.Parameters[i].getClass() != Integer.class)
					{
						RuntimeError err = new RuntimeError(RuntimeErrorCode.ArgumentTypeMismatch, context, entry.Parameters[i].getClass().toString() + "received, expected Integer.");
						if (m_RTEHandler != null)
							m_RTEHandler.handleScriptRuntimeError(err);
						context = null;
						return err;
					}
					break;
				case Float:
					if (entry.Parameters[i].getClass() != Double.class
						&& entry.Parameters[i].getClass() != Float.class
						&& entry.Parameters[i].getClass() != Integer.class)
					{
						RuntimeError err = new RuntimeError(RuntimeErrorCode.ArgumentTypeMismatch, context, entry.Parameters[i].getClass().toString() + "received, expected Double or convertible equivalent.");
						if (m_RTEHandler != null)
							m_RTEHandler.handleScriptRuntimeError(err);
						context = null;
						return err;
					}
					break;
				case Object:
					break;
				case String:
					if (entry.Parameters[i].getClass() != String.class)
					{
						RuntimeError err = new RuntimeError(RuntimeErrorCode.ArgumentTypeMismatch, context, entry.Parameters[i].getClass().toString() + "received, expected String.");
						if (m_RTEHandler != null)
							m_RTEHandler.handleScriptRuntimeError(err);
						context = null;
						return err;
					}
					break;
				case Bool:
					if (entry.Parameters[i].getClass() != Boolean.class)
					{
						RuntimeError err = new RuntimeError(RuntimeErrorCode.ArgumentTypeMismatch, context, entry.Parameters[i].getClass().toString() + "received, expected Boolean.");
						if (m_RTEHandler != null)
							m_RTEHandler.handleScriptRuntimeError(err);
						context = null;
						return err;
					}
					break;
				default:
					break;
			}
			//------------------
			if (v.varType == TypeID.Float)
				v.value = Tools.DblValue(entry.Parameters[i], context);
			else
				v.value = entry.Parameters[i];
			lParam.add(v);
		}
		// ok, now push the function on the stack and start execution:
		PushFunction(f, lParam);
		
		m_running = true;

		switch (mode)
		{
			case Blocking:
				RuntimeError err;
				while (m_running)
				{
					err = Step();
					if (err != null) {
						if (m_RTEHandler != null)
							m_RTEHandler.handleScriptRuntimeError(err);
						return err;
					}
				}
				return null;
			case Debug:
				err = new RuntimeError(RuntimeErrorCode.FeatureNotImplemented, context, "Running script in Debug mode.");
				if (m_RTEHandler != null)
					m_RTEHandler.handleScriptRuntimeError(err);
				return err;
			default: return null;
		}
	}

	public RuntimeError Step()
	{
		if (!m_running) {
			RuntimeError err = new RuntimeError(RuntimeErrorCode.ExecutorNotRunning, context, "Cannot step before execution is started.");
			if (m_RTEHandler != null)
				m_RTEHandler.handleScriptRuntimeError(err);
			return err;
		}
		
		context.CurrentInstrBlock().AdvanceInstruction = true;
		Instruction crtInstr = context.CurrentInstrBlock().CurrentInstr;

		if (crtInstr != null)
			crtInstr.Exec(context);
		if (context.Error != null) 
			return context.Error;
		
		if (context.BlockStack.size() == 0)
		{
			// finished execution
			context = null;
			m_running = false;
			return null;
		}
		if (context.CurrentInstrBlock().AdvanceInstruction)
		// if the previous instruction requested not to advance, we keep it
		{
			boolean next = false; // this gets set when we advance to the next instruction
			while (context.BlockStack.size() > 0 && !next)
			{
				crtInstr = context.CurrentInstrBlock().CurrentInstr;
				if (crtInstr != null && crtInstr.Next != null)
				{
					context.CurrentInstrBlock().CurrentInstr = crtInstr.Next;
					next = true;
				}
				else // the current block finished. pop it:
				{
					PopBlock();
					if (context.BlockStack.size() != 0 && !context.CurrentInstrBlock().AdvanceInstruction)
						next = true;
				}
			}
			if (context.BlockStack.size() == 0)
			{
				// finished execution
				context = null;
				m_running = false;
			}
		}

		return null;
	}

	void PushBlock(InstructionBlock instructionBlock)
	{
		// add the block to the stack:
		context.BlockStack.add(instructionBlock);
		// now create it's local variables on the stack:
		for (int i = 0; i < context.CurrentInstrBlock().LocalVars.size(); i++)
		{
			context.VarStack.add(new Variable(context.CurrentInstrBlock().LocalVars.get(i).varType));
			context.VarStack.get(context.VarStack.size()-1).classID = context.CurrentInstrBlock().LocalVars.get(i).classID;
		}
	}

	/**
	* pops the current block from the stack. called by break instructions.
	**/
	void PopBlock()
	{
		Tools.removeRange(context.VarStack, 
				context.VarStack.size() - context.CurrentInstrBlock().LocalVars.size(),
				context.CurrentInstrBlock().LocalVars.size());
		
		context.BlockStack.remove(context.BlockStack.size() - 1);
	}

	/**
	* pops a function from the stack, giving it the specified return value.
	* this is called by the return instructions.
	*
	* @param returnValue
	**/  
	void PopFunction(Object returnValue)
	{
		if (context.CurrentFunction.ReturnValue != null)
		{
			if (context.CurrentFunction.ReturnValue.varType == TypeID.Float)
				context.CurrentFunction.ReturnValue.value = (Double)returnValue;
			else
				context.CurrentFunction.ReturnValue.value = returnValue;
		}
		Tools.removeRange(context.BlockStack, context.CurrentFunction.nBlockOffset, context.BlockStack.size() - context.CurrentFunction.nBlockOffset);
		Tools.removeRange(context.VarStack, context.CurrentFunction.nBaseVarOffs, context.VarStack.size() - context.CurrentFunction.nBaseVarOffs);
		context.FuncStack.remove(context.FuncStack.size() - 1);
		context.CurrentFunction = context.FuncStack.size() > 0 ? context.FuncStack.get(context.FuncStack.size() - 1) : null;
	}

	/**
	* this creates an instance of a function and pushes it on the stack,
	* starting execution in it's main block.
	* it is used implicitly by outside function executions and
	* explicitly by inside function calls
	*
	* @param Function  
	* @param Parameters
	* @throws Exception 
	**/  
	void PushFunction(Function Function, ArrayList<Variable> Parameters)
	{
		// get a pointer to the current return variable:
		Variable ret = (Function.ReturnValue != null) ? context.VarByID(0,null) : null;
		// instantiate the function and push the parameters on the stack:
		context.FuncStack.add(Function.CreateInstance());
		context.CurrentFunction = context.FuncStack.get(context.FuncStack.size() - 1);
		context.CurrentFunction.nBaseVarOffs = context.VarStack.size();
		if (Function.ReturnValue != null)
			context.CurrentFunction.ReturnValue = ret;
		context.CurrentFunction.nBlockOffset = context.BlockStack.size();
		// create the return variable for subsequent function calls:
		context.VarStack.add(new Variable(TypeID.None));
		// push parameters:
		for (int i = 0; i < Parameters.size(); i++)
			context.VarStack.add(Parameters.get(i));
		// push the main instruction block :
		PushBlock(context.CurrentFunction.InstrBlock.CreateInstance());
	}
}
