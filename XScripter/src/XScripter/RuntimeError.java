package XScripter;

/** this class describes an error that occurs at run-time in the executor */
public class RuntimeError 
{
	public RuntimeErrorCode code;
	public ScriptContext context;
	public String message;
	
	public RuntimeError(RuntimeErrorCode code, ScriptContext context, String message)
	{
		this.code = code;
		this.context = context;
		this.message = message;
	}
	
	@Override
	public String toString()
	{
		Instruction instr = context == null? null: (context.BlockStack.size() != 0 ? context.CurrentInstrBlock().CurrentInstr : null); 
		String s = "Runtime error occured in script [" + context.ParentScript.Name + "] at :\n" +
			(instr != null ? instr.toString() : "<INITIALIZING SCRIPT>") + "\n";
		for (int i=context.FuncStack.size() - 1; i>=0; i--)
		{
			Function f = context.FuncStack.get(i); 
			s += f.Name + "(";
			for (int n=f.Parameters.size()+1, k=0; k<n; k++)
			{
				if (k < n-1)
				{
					Variable p = f.Parameters.get(k);
					s += p.varType.toString() + " " + p.name;
					if (k < n-2) s += ", ";
				}
				else
					s += ")\n";
			}
		}
		s += "\nError code : " + code.toString();
		s += "\nError Message : " + message + "\n";
		s += "=======================\n\n";
		
		return s;
	}
}
