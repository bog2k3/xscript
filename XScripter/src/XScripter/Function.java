package XScripter;

import java.util.ArrayList;

/**
* A function defined in the script, in it's compiled form.
**/
public class Function
{
	/** this holds a reference to an external variable, assigned to when the function was called.
	* it does not create a new variable (if the function result was not assigned to any variable
	* in the call statement, the value is lost).
	* (for prototype instances, it holds a reference to a variable of the function's return type type).
	*/
	public Variable ReturnValue;
	/** this is the main instruction block of the function */
	InstructionBlock InstrBlock;
	/** name of the function (by which it is known and called in the script and from outside). **/
	public String Name;
	/** the formal parameters of the function. The actual parameters are received as a list when
	* the function is called, and are pushed on the variable stack. **/
	public ArrayList<Variable> Parameters = new ArrayList<Variable>();
	
	/**
	 * the function signatures uniquely identifies the prototype of this function.
	 * it is also unique among multiple versions of overloaded functions.
	 */
	int signature;
	
	/** this is the offset of the function instance's main block in the block stack **/
	transient int nBlockOffset;
	
	/** this is the base offset of the function instance on the variable stack.
	* all variable indices in the current function are relative to this base offset. **/
	transient int nBaseVarOffs;
	
	/** the line in the script where it is defined */
	public int defineLine;

	/** creates an instance of this function. instances are needed at run-time, if a certain function is called multiple
	* times on the stack during the same operation.
	* the offsets for code blocks and local vars are duplicated and modfied for each instance. **/
	Function CreateInstance()
	{
		Function f = new Function();
		f.InstrBlock = InstrBlock;
		f.Name = Name;
		f.Parameters = Parameters;
		return f;
	}
	
	/** creates a complete clone of the function, also clonning it's instruction block, local variables, and
	* offsets every global variable reference by the given amount.
	* this is needed when local functions are copied from a library. 
	**/
	Function CreateOffsetedClone(int varOffs)
	{
		Function f = new Function();
		f.ReturnValue = ReturnValue;
		f.Name = Name;
		f.Parameters = Parameters;
		f.InstrBlock = InstrBlock.CreateOffsetedClone(varOffs);
		return f;
	}

	/** computes an unique signature for a given function name and a list of parameter types.
	 * this signature is used to identify different versions of an overloaded function.
	 * @param fname
	 * @param param
	 * @return
	 */
	public static int computeSignature(String fname, ArrayList<Variable> param) 
	{
		int sig = fname.hashCode();
		for (int i=0; i<param.size(); i++)
		{
			Variable v = param.get(i); 
			sig += (i+1) * v.varType.hashCode();
			if (v.classID != null)
				sig += (i+1) * v.classID.hashCode();
		}
		return sig;
	}

	public static int computeCallSignature(String fname, ArrayList<Expression> elist) 
	{
		int sig = fname.hashCode();
		for (int i=0; i<elist.size(); i++)
		{
			Expression e = elist.get(i); 
			sig += (i+1) * e.ResultType.hashCode();
			if (e.ResultClassID != null)
				sig += (i+1) * e.ResultClassID.hashCode();
		}
		return sig;
	}
}
