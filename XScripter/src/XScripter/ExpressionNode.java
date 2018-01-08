package XScripter;

/**
 * ExpressionNode
 * @author Bogdan.Ionita
 *
 */

/* operator precedence :

unary			:	!, -, +
binary			:	*, /, %
binary			:	+,-
binary bitwise	:	&, |, ^	
relational		:	<, >, !=, ==, <=, >=
logical			:	&&, ||, ^^
*/

enum ExpressionOperator
{
	OpInvalid,
	OpNOT,
	OpNegative,
	OpPositive,
	OpMultiply,
	OpDivide,
	OpRemainder,
	OpAdd,
	OpSubtract,
	OpBitAND,
	OpBitOR,
	OpBitXOR,
	OpCmpLess,
	OpCmpGreater,
	OpCmpNotEqual,
	OpCmpEqual,
	OpCmpLessEqual,
	OpCmpGreaterEqual,
	OpBoolAND,
	OpBoolOR,
	OpBoolXOR,
	OpAttrib;
	
	static ExpressionOperator FromString(String s)
	{
		if (s.equals("!")) return OpNOT;
		if (s.equals("*")) return OpMultiply;
		if (s.equals("/")) return OpDivide;
		if (s.equals("%")) return OpRemainder;
		if (s.equals("+")) return OpAdd;
		if (s.equals("-")) return OpSubtract;
		if (s.equals("&")) return OpBitAND;
		if (s.equals("|")) return OpBitOR;
		if (s.equals("^")) return OpBitXOR;
		if (s.equals("<")) return OpCmpLess;
		if (s.equals(">")) return OpCmpGreater;
		if (s.equals("!=")) return OpCmpNotEqual;
		if (s.equals("==")) return OpCmpEqual;
		if (s.equals("<=")) return OpCmpLessEqual;
		if (s.equals(">=")) return OpCmpGreaterEqual;
		if (s.equals("&&")) return OpBoolAND;
		if (s.equals("||")) return OpBoolOR;
		if (s.equals("^^")) return OpBoolXOR;
		if (s.equals("=")) return OpAttrib;
		return OpInvalid;
	}
}

class ExpressionNode
{
	/** immediate value / operator / variable **/
	ExpressionNodeType NodeType;
	/** int, float etc (is computed from what it contains at compile time) **/
	TypeID valueType = TypeID.Int;
	/** this holds the id of the registerd class type if the current node contains a reference to an object-type variable **/
	String ClassID = null;
	/** this is the name of the external global variable, if the node references such a variable. null otherwise. **/
	String ExtGlobalName = null;

	ExpressionOperator Operator;
	
	/**
	* imediate value or variable reference
	* or id of temporary variable (for function result)
	* it can store the immediat value, or can be an index into the variable stack
	* the index is usually relative to the current block, but can be absolute if 
	* the 16th bit (position 15) is set (0x8000) , indicating a global variable
	**/
	Object valueRef;
	transient String Text;

	ExpressionNode Left, Right;
	Expression Parent;

	/** transforms the text into operands and operators. **/
	/** also places necessary function calls in the C.InstrBlock block. 
	 * @throws Exception **/
	boolean Split(ScriptContext C)
	{
		// search all String immediate values and extract them to temporary nodes:
		for (int i = 0; i < Text.length(); i++)
		{
			if (Text.charAt(i) == '"')
			{
				int istart = i;
				String s = "";
				i++;
				while (i < Text.length() && Text.charAt(i) != '"')
					s += Text.charAt(i++);
				if (i == Text.length())
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.QuoteExpected, Parent.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				ExpressionNode node = new ExpressionNode();
				node.Parent = this.Parent;
				node.NodeType = ExpressionNodeType.Value;
				node.Text = "<String>";
				node.valueType = TypeID.String;
				node.valueRef = s;
				Parent.AuxNodes.add(node);
				Text = Text.substring(0, istart) + Character.toString((char)0xff) + Character.toString((char)(Parent.AuxNodes.size() - 1)) + Text.substring(i + 1);
				i = istart;
			}
		}
		// find all paranthesis pairs
		// replacing them with temporary node identifiers:
		// Paranthesis
		boolean found; // found paranthesis ?
		int len;
		do
		{
			found = false;
			int i = 0, ipar = 0;
			len = Text.length();
			int iStart = 0, iEnd = 0;
			while (i < len && Text.charAt(i) != '(')
				i++;
			if (i != len)
			{ // found a paranthesis
				ipar = 1; found = true; i++;
				iStart = i;
				if (Text.charAt(i) == ')') 
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				while (i < len && (Text.charAt(i) != ')' || ipar > 1))
				{
					if (Text.charAt(i) == '(') ipar++;
					if (Text.charAt(i) == ')') ipar--;
					i++;
				}
				if (i == len)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.RBracketExpected, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				iEnd = i - 1;
			}
			if (found)
			{ // extract the text between iStart and iEnd and
				// create an aux node out of it
				ExpressionNode node = new ExpressionNode(); 
				Parent.AuxNodes.add(node);
				node.Parent = this.Parent;
				node.Text = Text.substring(iStart, iEnd + 1);
				if (!node.Split(C))
					return false;
				// node has splitted ok, let's replace it's text from ours with
				// an identifier
				Text = Text.substring(0, iStart - 1) + Character.toString((char)0xff) + Character.toString((char)(Parent.AuxNodes.size() - 1)) + Text.substring(iEnd + 2);
			}
		} while (found);

		// search for operators from back to front, in reverse priority order
		/* operator precedence :
			
				unary			:	!, -, +
				binary			:	*, /, %
				binary			:	+,-
				binary bitwise	:	&, |, ^	
				relational		:	<, >, !=, ==, <=, >=
				logical			:	&&, ||, ^^
		*/

		len = Text.length();

		// search for logical operators
		for (int i = len - 1; i >= 0; i--)
		{
			if (Text.charAt(i) == '&' || Text.charAt(i) == '|' || Text.charAt(i) == '^')
			{
				if (i == 0 || i == len - 1)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
					return false;
				}
				if (Text.charAt(i - 1) != Text.charAt(i))
					continue; // this is possibly a bitwise operator. if so, it is taken care of, later.

				// found
				i--;
				String symb = String.valueOf(Text.charAt(i)) + String.valueOf(Text.charAt(i)); 
				this.Operator = ExpressionOperator.FromString(symb);
				this.NodeType = ExpressionNodeType.Operator;
				this.Left = new ExpressionNode();
				this.Right = new ExpressionNode();
				this.Left.Parent = this.Right.Parent = this.Parent;
				this.Left.Text = Text.substring(0, i);
				this.Right.Text = Text.substring(i + symb.length());
				if (!Left.Split(C))
					return false;
				if (!Right.Split(C))
					return false;
				TypeID tdw = CheckTypes(Left.valueType, Left.ClassID, Right.valueType, Right.ClassID, this.Operator, Parent.ParentScript);
				if (tdw == TypeID.None)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				this.valueType = tdw;
				return true;
			}
		}

		// search for relational operators
		for (int i = len - 1; i >= 0; i--)
		{
			char c = Text.charAt(i);
			if (c == '=' || c == '<' || c == '>')
			{
				// found
				String symb = String.valueOf(Text.charAt(i));
				if (i == 0 || i == len - 1)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
					return false;
				}
				c = Text.charAt(i - 1);
				if (c == '=' || c == '!' || c == '<' || c == '>')
				{
					symb = String.valueOf(c) + symb;
					i--;
				}
				if (!symb.equals("==") && !symb.equals("!=") && !symb.equals("<=")
					&& !symb.equals(">=") && !symb.equals(">") && !symb.equals("<"))
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Unknown operator : " + symb));
					return false;
				}
				this.Operator = ExpressionOperator.FromString(symb);
				this.NodeType = ExpressionNodeType.Operator;
				this.Left = new ExpressionNode();
				this.Right = new ExpressionNode();
				this.Left.Parent = this.Right.Parent = this.Parent;
				this.Left.Text = Text.substring(0, i);
				this.Right.Text = Text.substring(i + symb.length());
				if (!Left.Split(C))
					return false;
				if (!Right.Split(C))
					return false;
				TypeID tdw = CheckTypes(Left.valueType, Left.ClassID, Right.valueType, Right.ClassID, this.Operator, Parent.ParentScript);
				if (tdw == TypeID.None)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				this.valueType = tdw;
				return true;
			}
		}

		// check for '&', '|', or '^' (bitwise operators)
		for (int i = len - 1; i >= 0; i--)
			if (Text.charAt(i) == '^' || Text.charAt(i) == '&' || Text.charAt(i) == '|')
			{ // found
				if (i == 0 || i == len - 1)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
					return false;
				}
				this.Operator = ExpressionOperator.FromString(Text.substring(i,i+1));
				this.NodeType = ExpressionNodeType.Operator;
				this.Left = new ExpressionNode();
				this.Right = new ExpressionNode();
				this.Left.Parent = this.Right.Parent = this.Parent;
				this.Left.Text = Text.substring(0, i);
				this.Right.Text = Text.substring(i + 1);
				if (!Left.Split(C))
					return false;
				if (!Right.Split(C))
					return false;
				TypeID tdw = CheckTypes(Left.valueType, Left.ClassID, Right.valueType, Right.ClassID, this.Operator, Parent.ParentScript);
				if (tdw == TypeID.None)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				this.valueType = tdw;
				return true;
			}

		// search for '+' or '-'
		for (int i = len - 1; i >= 0; i--)
		{
			char c = Text.charAt(i);
			if (c == '+' || c == '-')
			{ // found, now check for
				// unary operator (relational cannot be here anymore)
				if (i == 0 || Text.charAt(i - 1) == '+' || Text.charAt(i - 1) == '-' || Text.charAt(i - 1) == '*' ||
					Text.charAt(i - 1) == '/' || Text.charAt(i - 1) == '&' || Text.charAt(i - 1) == '|' ||
					Text.charAt(i - 1) == '^' || Text.charAt(i - 1) == '%') // unary

					i--;
				if (i < 0)
				{ // this becomes the unary node
					this.Operator = ExpressionOperator.FromString(Text.substring(0, 1));
					this.NodeType = ExpressionNodeType.Operator;
					this.Right = new ExpressionNode();
					this.Right.Parent = this.Parent;
					this.Right.Text = this.Text.substring(1);
					if (!this.Right.Split(C))
						return false;
					TypeID tdw = CheckTypes(TypeID.None, null, Right.valueType, Right.ClassID, Operator, Parent.ParentScript);
					if (tdw == TypeID.None)
					{
						Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
						return false;
					}
					this.valueType = tdw;
					return true;
				}
				else
				{ // not unary : split into 2 operands
					if (i == 0 || i == len - 1)
					{
						Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
						return false;
					}
					this.Operator = ExpressionOperator.FromString(Text.substring(i,i+1));
					this.NodeType = ExpressionNodeType.Operator;
					this.Left = new ExpressionNode();
					this.Right = new ExpressionNode();
					this.Left.Parent = this.Right.Parent = this.Parent;
					this.Left.Text = Text.substring(0, i);
					this.Right.Text = Text.substring(i + 1);
					if (!Left.Split(C))
						return false;
					if (!Right.Split(C))
						return false;
					TypeID tdw = CheckTypes(Left.valueType, Left.ClassID, Right.valueType, Right.ClassID, this.Operator, Parent.ParentScript);
					if (tdw == TypeID.None)
					{
						Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
						return false;
					}
					this.valueType = tdw;
					return true;
				}
			}
		}
		// didn't find any '+' or '-'
		// now check for '*' and '/' (no more unary operators from now on, until '!')
		for (int i = len - 1; i >= 0; i--)
			if (Text.charAt(i) == '*' || Text.charAt(i) == '/' || Text.charAt(i) == '%')
			{ // found
				if (i == 0 || i == len - 1)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
					return false;
				}
				this.Operator = ExpressionOperator.FromString(Text.substring(i,i+1));
				this.NodeType = ExpressionNodeType.Operator;
				this.Left = new ExpressionNode();
				this.Right = new ExpressionNode();
				this.Left.Parent = this.Right.Parent = this.Parent;
				this.Left.Text = Text.substring(0, i);
				this.Right.Text = Text.substring(i + 1);
				if (!Left.Split(C))
					return false;
				if (!Right.Split(C))
					return false;
				TypeID tdw = CheckTypes(Left.valueType, Left.ClassID, Right.valueType, Right.ClassID, this.Operator, Parent.ParentScript);
				if (tdw == TypeID.None)
				{
					Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				this.valueType = tdw;
				return true;
			}
		// only '!' left to check for
		if (Text.charAt(0) == '!')
		{ // create unary node
			if (len == 1)
			{
				Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Missing operand"));
				return false;
			}
			this.Operator = ExpressionOperator.FromString(Text.substring(0,1));
			this.NodeType = ExpressionNodeType.Operator;
			this.Right = new ExpressionNode();
			this.Right.Parent = this.Parent;
			this.Right.Text = Text.substring(1);
			if (!Right.Split(C))
				return false;
			TypeID tdw = CheckTypes(TypeID.None, null, Right.valueType, Right.ClassID, Operator, Parent.ParentScript);
			if (tdw == TypeID.None)
			{
				Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
				return false;
			}
			this.valueType = tdw;
			return true;
		}

		// alright, we did not find any operators.
		// there can only be an imediate value in the node, or 
		// a variable / aux node
		// if it starts with a digit, there must be an immediat value
		String str;
		if (Script.IsDigit(Text.charAt(0)))
		{ // numerical immediate value
			str = ""; int nDot = 0;
			while (str.length() < len && (Script.IsDigit(Text.charAt(str.length())) || (str.length() > 0 && nDot == 0 && Text.charAt(str.length()) == '.')))
			{
				str += Text.charAt(str.length());
				if (Text.charAt(str.length()-1) == '.') nDot++;
			}
			if (str.length() != len)
			{
				Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, "Illegal character in number"));
				return false;
			}
			if (nDot == 0)
				valueRef = Integer.valueOf(str);
			else
				valueRef = Double.valueOf(str);
			this.NodeType = ExpressionNodeType.Value;
			this.valueType = nDot == 0 ? TypeID.Int : TypeID.Float;
			return true;
		}
		// no numerical immediate value -> must be a bool immediat value or variable / aux node 
		// check for aux node:
		if ((byte)Text.charAt(0) == (byte)0xff)
		{ // aux node ;)
			int id = (byte)Text.charAt(1);
			Text = null;
			ExpressionNode auxNode = Parent.AuxNodes.get(id); 
			this.Left = auxNode.Left;
			this.NodeType = auxNode.NodeType;
			this.Operator = auxNode.Operator;
			this.Right = auxNode.Right;
			this.Text = auxNode.Text;
			this.valueRef = auxNode.valueRef;
			this.valueType = auxNode.valueType;
			Parent.AuxNodes.set(id, this);
			return true;
		}
		// check for bool immediate value or variable id:
		str = "";
		while (str.length() < len && Script.IsLetter(Text.charAt(str.length())))
			str += Text.charAt(str.length());
		// got the id, let's check to see what it is
		// bool immediate value ?
		if (str.equals("true") || str.equals("false"))
		{
			this.NodeType = ExpressionNodeType.Value;
			this.valueType = TypeID.Bool;
			this.valueRef = (boolean)(str.equals("true"));
			return true;
		}
		// null keyword ?
		if (str.equals("null"))
		{
			this.NodeType = ExpressionNodeType.Value;
			this.valueType = TypeID.Null;
			this.valueRef = null;
			return true;
		}

		found = false;
		int varID = -1;
		for (int i = C.ParentScript.varStack.size() - 1; i >= 0; i--)
			if (str.equals(C.ParentScript.varStack.get(i).name))
			{
				varID = i - C.CurrentFunction.nBaseVarOffs;
				found = true;
				break;
			}
		if (!found)
			for (int i = 0; i < C.ParentScript.variables.size(); i++)
				if (str.equals(C.ParentScript.variables.get(i).name))
				{
					varID = i | 0x8000;
					found = true;
					break;
				}
		if (!found)
			if (C.ParentScript.extGlobals != null && C.ParentScript.extGlobals.containsKey(str))
			{
				varID = 0x4000;
				// check if the global variable type is known (for class types only)
				if (!C.ParentScript.CheckExtGlobal(C, C.VarByID(varID, str)))
					return false;
				found = true;
			}
		if (found)
		{
			// var id
			if (str.length() != len)
			{
				Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.Syntax, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
				return false;
			}
			this.NodeType = ExpressionNodeType.Variable;
			this.valueType = C.VarByID(varID, str).varType;
			this.ClassID = C.VarByID(varID, str).classID;
			this.valueRef = varID;
			if (varID == 0x4000)
				this.ExtGlobalName = str;
			return true;
		}
		else
		{
			Parent.ParentScript.SetError(new ScriptError(ScriptErrorCode.UnknownIdent, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, str));
			return false;
		}
	}

	/** evaluates the node value recursively **/
	Object Evaluate(ScriptContext C)
	{
		if (C.Error != null)
			return null;
		
		if (Left != null)
		{
			switch (Operator)
			{
			case OpAdd: switch (valueType)
				{
				case Float: return Tools.DblValue(Left.Evaluate(C), C) + Tools.DblValue(Right.Evaluate(C), C);
				case Int: return Tools.IntValue(Left.Evaluate(C), C) + Tools.IntValue(Right.Evaluate(C), C);
				case String: return (String)Left.Evaluate(C) + (String)Right.Evaluate(C);
				default:
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
					return null;
				}
			case OpSubtract: switch (valueType)
				{
				case Float: return Tools.DblValue(Left.Evaluate(C), C) - Tools.DblValue(Right.Evaluate(C), C);
				case Int: return Tools.IntValue(Left.Evaluate(C), C) - Tools.IntValue(Right.Evaluate(C), C);
				default:
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
					return null;
				}
			case OpMultiply: switch (valueType)
				{
				case Float: return Tools.DblValue(Left.Evaluate(C), C) * Tools.DblValue(Right.Evaluate(C), C);
				case Int: return Tools.IntValue(Left.Evaluate(C), C) * Tools.IntValue(Right.Evaluate(C), C);
				default:
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
					return null;
				}
			case OpDivide: switch (valueType)
				{
				case Float: return Tools.DblValue(Left.Evaluate(C), C) / Tools.DblValue(Right.Evaluate(C), C);
				case Int: return Tools.IntValue(Left.Evaluate(C), C) / Tools.IntValue(Right.Evaluate(C), C);
				default:
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
					return null;
				}
			case OpRemainder: return Tools.IntValue(Left.Evaluate(C), C) % Tools.IntValue(Right.Evaluate(C), C);
			case OpBitXOR: return Tools.IntValue(Left.Evaluate(C), C) ^ Tools.IntValue(Right.Evaluate(C), C);
			case OpBitAND: return Tools.IntValue(Left.Evaluate(C), C) & Tools.IntValue(Right.Evaluate(C), C);
			case OpBitOR: return Tools.IntValue(Left.Evaluate(C), C) | Tools.IntValue(Right.Evaluate(C), C);
			case OpCmpLess: return Tools.DblValue(Left.Evaluate(C), C) < Tools.DblValue(Right.Evaluate(C), C);
			case OpCmpGreater: return Tools.DblValue(Left.Evaluate(C), C) > Tools.DblValue(Right.Evaluate(C), C);
			case OpCmpEqual: switch (Left.valueType)
				{
				case Int:
				case Float:
					return Tools.DblValue(Left.Evaluate(C), C).equals(Tools.DblValue(Right.Evaluate(C), C));
				case String:
					return ((String)Left.Evaluate(C)).equals(Right.Evaluate(C));
				case Object:
					return Left.Evaluate(C) == Right.Evaluate(C);
				default:
					if (Left.valueType.isArray() && Right.valueType.isArray())
						return Left.Evaluate(C) == Right.Evaluate(C);
					
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,Left.valueType.toString()));
					return null;
				}
			case OpCmpNotEqual: switch (Left.valueType)
				{
				case Int:
				case Float:
					return !Tools.DblValue(Left.Evaluate(C), C).equals(Tools.DblValue(Right.Evaluate(C), C));
				case String:
					return !((String)Left.Evaluate(C)).equals(Right.Evaluate(C));
				case Object:
					return Left.Evaluate(C) != (Object)Right.Evaluate(C);
				default:
					if (Left.valueType.isArray() && Right.valueType.isArray())
						return Left.Evaluate(C) != Right.Evaluate(C);
				
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,Left.valueType.toString()));
					return null;
				}
			case OpCmpLessEqual: return Tools.DblValue(Left.Evaluate(C), C) <= Tools.DblValue(Right.Evaluate(C), C);
			case OpCmpGreaterEqual: return Tools.DblValue(Left.Evaluate(C), C) >= Tools.DblValue(Right.Evaluate(C), C);
			case OpBoolOR: return ((Boolean)Left.Evaluate(C)) || ((Boolean)Right.Evaluate(C));
			case OpBoolAND: return ((Boolean)Left.Evaluate(C)) && ((Boolean)Right.Evaluate(C));
			case OpBoolXOR:
				Boolean l = (Boolean)Left.Evaluate(C), r = (Boolean)Right.Evaluate(C);
				return ((l && !r) || (!l && r));
			default:
				C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeOperator,C,Operator.toString()));
				return null;
			}
		}
		else
			if (Right != null)
			{// unary Operator
				switch (Operator)
				{
				case OpSubtract: switch (valueType)
					{
					case Float: return -Tools.DblValue(Right.Evaluate(C), C);
					case Int: return -Tools.IntValue(Right.Evaluate(C), C);
					default:
						C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
						return null;
					}
				case OpAdd: return Right.Evaluate(C);
				case OpNOT: return !(Boolean)Right.Evaluate(C);
				default:
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeOperator,C,Operator.toString()));
					return null;
				}
			}
			else // immediate value / var
				switch (valueType)
				{
				case Int:
					if (NodeType == ExpressionNodeType.Value)
						return Tools.IntValue(valueRef, C);
					else
						return Tools.IntValue(C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value, C);
				case Float:
					if (NodeType == ExpressionNodeType.Value)
						return Tools.DblValue(valueRef, C);
					else
						return Tools.DblValue(C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value, C);
				case Object:
					if (NodeType == ExpressionNodeType.Value)
						return valueRef;
					else
						return C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value;
				case String:
					if (NodeType == ExpressionNodeType.Value)
						return (String)valueRef;
					else
						return (String)C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value;
				case Bool:
					if (NodeType == ExpressionNodeType.Value)
						return (Boolean)valueRef;
					else
						return (Boolean)C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value;
				case Null:
					return null;
				default:
					if (valueType.isArray())
						if (NodeType == ExpressionNodeType.Value)
							return valueRef;
						else
							return C.VarByID(Tools.IntValue(valueRef, C), ExtGlobalName).value;
				
					C.ThrowRuntimeError(new RuntimeError(RuntimeErrorCode.InvalidExpressionNodeResultType,C,valueType.toString()));
					return null;
				}
	}

	/**
	* checks for compatibility between types and operators, and determines the resulting type
	* return true for compatibility or false for error.
	*
	* @param t1 TypeID of the first operand (may be none for unary operation
	* @param t2 TypeID of the second operand
	* @param class1 ID of registered class of the first operand (for TypeID.Object
	* @param class2 ID of registered class of the second operand (for TypeID.Object
	* @param Operator
	* @param script
	* @param res This will receive the resulting type of the operation
	**/
	static TypeID CheckTypes(TypeID t1, String class1, TypeID t2, String class2, ExpressionOperator Operator, Script script)
	{
		if (t1 == TypeID.Void || t2 == TypeID.Void) return TypeID.None;
		if (t1.isArray() || t2.isArray())
		{
			if ( (t1 != TypeID.Null && !t1.isArray()) || (t2 != TypeID.Null && !t2.isArray()) )
				return TypeID.None;			
			if (t1 != TypeID.Null && t2 != TypeID.Null && t1.BaseType() != t2.BaseType())
				return TypeID.None;
			if (t1 != TypeID.Null && t2 != TypeID.Null && (class1 != null || class2 != null) && (class1 == null || class2==null || !class1.equals(class2)))
				return TypeID.None;
			
			switch (Operator)
			{
			case OpAttrib: return t1;
			
			case OpCmpEqual:
			case OpCmpNotEqual:
				return TypeID.Bool;
				
			default: return TypeID.None;
			}
		}
		switch (t1)
		{
			case None: // unary operator:
				if (((t2 == TypeID.Int || t2 == TypeID.Float) && Operator != ExpressionOperator.OpNOT) ||
					(t2 == TypeID.Bool && Operator == ExpressionOperator.OpNOT))
					return t2;
			case Any:
				if (Operator == ExpressionOperator.OpAttrib)
					return TypeID.Any;
				else
					return TypeID.None;
			case Int:
				if (Operator == ExpressionOperator.OpAttrib) return (t2 == TypeID.Int ? t2 : TypeID.None);
				if (Operator == ExpressionOperator.OpCmpLess || Operator == ExpressionOperator.OpCmpGreater || 
					Operator == ExpressionOperator.OpCmpGreaterEqual || Operator == ExpressionOperator.OpCmpLessEqual ||
					Operator == ExpressionOperator.OpCmpNotEqual || Operator == ExpressionOperator.OpCmpEqual)
				{
					if (t2 == TypeID.Int || t2 == TypeID.Float)
						return TypeID.Bool;
					else
						return TypeID.None;
				}
				if (Operator == ExpressionOperator.OpMultiply || Operator == ExpressionOperator.OpDivide || 
					Operator == ExpressionOperator.OpAdd || Operator == ExpressionOperator.OpSubtract)
				{
					if (t2 == TypeID.Float || t2 == TypeID.Int)
						return (t2 == TypeID.Float) ? TypeID.Float : TypeID.Int;
					else
						return TypeID.None;
				}
				if (Operator == ExpressionOperator.OpBitXOR || Operator == ExpressionOperator.OpBitAND || 
					Operator == ExpressionOperator.OpBitOR || Operator == ExpressionOperator.OpRemainder)
				{
					return (t2 == TypeID.Int ? t2 : TypeID.None);
				}
				return TypeID.None;
			case Float:
				if (Operator == ExpressionOperator.OpAttrib) return (t2 == TypeID.Int || t2 == TypeID.Float) ? t2 : TypeID.None;
				if (Operator == ExpressionOperator.OpCmpLess || Operator == ExpressionOperator.OpCmpGreater || 
					Operator == ExpressionOperator.OpCmpGreaterEqual || Operator == ExpressionOperator.OpCmpLessEqual ||
					Operator == ExpressionOperator.OpCmpNotEqual)
				{
					return (t2 == TypeID.Int || t2 == TypeID.Float) ? TypeID.Bool : TypeID.None;
				}
				if (Operator == ExpressionOperator.OpMultiply || Operator == ExpressionOperator.OpDivide || 
					Operator == ExpressionOperator.OpAdd || Operator == ExpressionOperator.OpSubtract)
				{
					return (t2 == TypeID.Float || t2 == TypeID.Int) ? TypeID.Float : TypeID.None;
				}
				return TypeID.None;
			case Bool:
				if ((t2 == TypeID.Bool && (Operator == ExpressionOperator.OpAttrib || Operator == ExpressionOperator.OpBoolAND || 
						Operator == ExpressionOperator.OpBoolOR || Operator == ExpressionOperator.OpBoolXOR)))
					return TypeID.Bool;
				else
					return TypeID.None;
			case String:
				if (t2 != TypeID.String && t2 != TypeID.Null)
					return TypeID.None;
				if (Operator == ExpressionOperator.OpAttrib)
				{
					return TypeID.String;
				}
				if (Operator == ExpressionOperator.OpCmpEqual || Operator == ExpressionOperator.OpCmpNotEqual)
				{
					return TypeID.Bool;
				}
				if (Operator == ExpressionOperator.OpAdd)
				{
					return (t2 != TypeID.Null) ? TypeID.String : TypeID.None;
				}
				return TypeID.None;
			case Object:
				if (t2 != TypeID.Object && t2 != TypeID.Null)
					return TypeID.None;
				if (Operator == ExpressionOperator.OpAttrib)
					if((t2 == TypeID.Null || class1.equals(class2) || class1 == null || 
					((ScriptClass)script.extClasses.get(class2)).isSubclassOf(class1) || t2 == TypeID.Null))
						return TypeID.Object;
					else
						return TypeID.None;
				if (Operator == ExpressionOperator.OpCmpEqual || Operator == ExpressionOperator.OpCmpNotEqual)
				{
					if ((class1 == class2 || class1 == null || class2 == null
						|| ((ScriptClass)script.extClasses.get(class2)).isSubclassOf(class1)
						|| ((ScriptClass)script.extClasses.get(class1)).isSubclassOf(class2)
						|| t2 == TypeID.Null || t1 == TypeID.Null))
						return TypeID.Bool;
					else
						return TypeID.None;
				}
				return TypeID.None;
		}
		return TypeID.None;
	}
	
	ExpressionNode CreateOffsetedClone(Expression parent, int varOffs)
	{
		ExpressionNode n = new ExpressionNode();
		n.NodeType = NodeType;
		n.valueType = valueType;
		n.ClassID = ClassID;
		n.ExtGlobalName = ExtGlobalName;
		n.Operator = Operator;
		n.Parent = parent;
		n.valueRef = (NodeType == ExpressionNodeType.Variable) ? (((Integer)valueRef & 0x8000) != 0 ? (Integer)valueRef + varOffs : valueRef) : valueRef;
		if (Left != null)
			n.Left = Left.CreateOffsetedClone(parent, varOffs);
		if (Right != null)
			n.Right = Right.CreateOffsetedClone(parent, varOffs);
		return n;
	}
}
