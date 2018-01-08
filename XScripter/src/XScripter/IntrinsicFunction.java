package XScripter;

import java.util.ArrayList;

public abstract class IntrinsicFunction extends Function 
{
	
	IntrinsicFunction( TypeID retType, String name, TypeID... argTypes ) 
	{
		if (retType != TypeID.Void)
			ReturnValue = new Variable(retType);
		this.Name = name;
		this.Parameters = new ArrayList<Variable>(argTypes.length);
		for (int i=0; i<argTypes.length; i++)
			this.Parameters.add(new Variable(argTypes[i]));
	}

	abstract Object exec(ArrayList<Variable> l);
	
}
