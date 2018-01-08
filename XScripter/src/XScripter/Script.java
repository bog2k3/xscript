package XScripter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
* this holds information about the location and count of components imported
* from libraries or owned by the script, inside it's lists.
**/
class LibStackMapping
{
	/**
	* function list offset
	**/
	int FunctionLevel = 0;
	/**
	* number of functions
	**/
	int FunctionCount = 0;
	/**
	* variable list offset
	**/
	int VariableLevel = 0;
	/**
	* number of variables
	**/
	int VariableCount = 0;
	/**
	* external function list offset
	**/
	int ExtFunctionLevel = 0;
	/**
	* external function count
	**/
	//int ExtFunctionCount;
	/**
	* class list offset
	**/
	int ClassLevel = 0;
	/**
	* class count
	**/
	int ClassCount = 0;

	LibStackMapping(LibStackMapping Original)
	{
		FunctionCount = Original.FunctionCount;
		FunctionLevel = Original.ExtFunctionLevel;
		VariableCount = Original.VariableCount;
		VariableLevel = Original.VariableLevel;
		ExtFunctionLevel = Original.ExtFunctionLevel;
		ClassCount = Original.ClassCount;
		ClassLevel = Original.ClassLevel;
	}

	LibStackMapping() {}

}

public class Script
{
	// public methods & fields:

	/**
	* this is the name of the script. it is given inside the script with the 
	* #unit "name" 
	* directive. it is only valid after compilation.
	**/
	public String Name = null;
	public String FileName = null;

	/**
	* indicates the line where the compile error occured
	**/
	public int getCurrentLine()
	{
		return m_Text.current_line;
	}

	/**
	* returns true on success and false if the file does not exist
	 * @throws IOException 
	**/
	public boolean LoadFromFile(String path)
	{
		if (m_Compiled) return false;
		
		FileName = path;
		
		File f;
		FileReader fr;
		BufferedReader r;
		
		char[] Chars;		
		try
		{
			f = new File(path);
			fr = new FileReader(path);
			r = new BufferedReader(fr);
			Chars = new char[(int)f.length()];
			r.read(Chars, 0, Chars.length);
			r.close();
			fr.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}

		return LoadFromCharArray(Chars);
	}

	/**
	* loads the script from a String
	**/
	public boolean LoadFromString(String text)
	{
		return LoadFromCharArray(text.toCharArray());
	}

	/**
	* loads the script from a char[]
	**/
	public boolean LoadFromCharArray(char[] text)
	{
		if (m_Compiled) return false;

		char[] Chars = text;
		// replace line ends from the original Chars with char 0xff
		// and tabs with spaces
		for (int i = 0; i < Chars.length; i++)
		{
			if (Chars[i] == 10)
				Chars[i] = (char)0xff;
			if (Chars[i] == 13 || Chars[i] == 9)
				Chars[i] = ' ';
			if (Chars[i] == '/' && Chars[i + 1] == '/')
			{
				do
				{
					Chars[i] = ' ';
					i++;
				} while (i < Chars.length && Chars[i] != 13 && Chars[i] != 10);
				i--;
			}
			if (Chars[i] == '/' && Chars[i + 1] == '*')
			{
				do
				{
					if (Chars[i] == 10)
						Chars[i] = (char)0xff;
					else
						Chars[i] = ' ';
					i++;
				} while (i < Chars.length-1 && (Chars[i] != '*' || Chars[i+1] != '/'));
				Chars[i] = Chars[i+1] = ' ';
			}
		}
		m_Text = new TextStream(new String(Chars));
		return true;
	}
	
	/**
	* true if the script is in compiled form.
	**/
	public boolean IsCompiled() { return m_Compiled; }

	/**
	* compiles the script and returns null on success or a ScriptError on error.
	 * @throws Exception 
	**/
	public ScriptError Compile(ILibraryFetcher libFetcher, IWarningHandler warnHandler)
	{
		if (m_Compiled)
			return null;
		
		m_warnHandler = warnHandler;
		
		populateIntrinsicFunctions();
		
		m_Text.Reset();

		ScriptContext context = new ScriptContext();
		context.CurrentFunction = null;
		context.ParentScript = this;
		context.BlockStack = blockStack;
		context.VarStack = varStack;
		context.libFetcher = libFetcher;
		try
		{
			while (!m_Text.eof())
			{
				if (!ReadTopLevelItem(functions, variables, context, null))
				{
					return m_Error;
				}
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			return new ScriptError(ScriptErrorCode.EndOfFile, this, getCurrentLine(), context.CurrentFunction, null);
		}

		// now check if we have a body definition for each function in our script:
		for (int i = 0; i < functions.size(); i++)
			if (functions.get(i).InstrBlock == null)
			{
				//add parameter description to error (for function versioning)
				Function f = functions.get(i);
				String funcSig = f.Name + "(";
				for (int n=f.Parameters.size(), k=0; k<n; k++)
				{
					Variable param = f.Parameters.get(k);
					funcSig += param.varType.toString();
					if (param.varType.BaseType() == TypeID.Object)
						funcSig += "<"+param.classID+">";
					if (k < n-1)
						funcSig += ", ";
					else
						funcSig += ")";
				}
				return new ScriptError(ScriptErrorCode.FuncDefMissing, this, getCurrentLine(), functions.get(i), funcSig);
			}
		
		// check if all classes are defined:
		for (String sclass : extClasses.keySet()) {
			if (!extClasses.get(sclass).isDefined) {
				return new ScriptError(ScriptErrorCode.ClassDefMissing, this, getCurrentLine(), null, sclass);
			}
		}

		m_Compiled = true;
		m_Text = null;
		
		// save the number of owned components:
		ownLevel.ClassCount = classList.size() - ownLevel.ClassLevel;
		//ownLevel.ExtFunctionCount = ExtFunctions.size() - ownLevel.ExtFunctionLevel;
		ownLevel.FunctionCount = functions.size() - ownLevel.FunctionLevel;
		ownLevel.VariableCount = variables.size() - ownLevel.VariableLevel;

		// release the stacks, we no longer need them:
		varStack = null;
		blockStack = null;

		return null;
	}
	
	private void sendWarning(ScriptError warn)
	{
		if (m_warnHandler != null)
			m_warnHandler.handleScriptWarning(warn);
	}
	
	private void populateIntrinsicFunctions()
	{
		intrinsicFunctions.add(new IntrinsicFunction(TypeID.Int, "length", TypeID.AnyArray) {			
			@Override
			Object exec(ArrayList<Variable> l) {
				Object[] obj = (Object[])l.get(0).value;
				return new Integer(obj.length);
			}
		});
		
		intrinsicFunctions.add(new IntrinsicFunction(TypeID.String, "str", TypeID.Any) {
			@Override
			Object exec(ArrayList<Variable> l) {
				Object obj = l.get(0).value;
				return obj.toString();
			}
		});
	}

	/**
	* this list will contain after compilation the names of the units
	* that this script needs to run.
	* these are the units specified inside the script with the #imports directive
	**/
	public ArrayList<String> Dependencies = new ArrayList<String>();

	/**
	* Registers an external class implementation with the script.
	* This is needed before executor run-time (not at script compile time).
	* The external class is only registered if it was referenced within the script or within a library used by the script.
	*
	* @param ClassName name of the class, as it will be known in the script
	* @param ClassType the type of the class
	* @param validate true to perform validation on the external prototype
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	**/
	public ScriptError RegisterClass(String ClassName, Class<? extends IScriptable> ClassType, boolean validate)
	{
		for (ScriptClass c : extClasses.values())
		{
			if (c.Name.equals(ClassName))
			{
				if (validate)
				{
					try {
						Class<?>[] params = { PropertyDesc[].class, MethodDesc[].class, String.class, boolean.class };
						Method m = ClassType.getDeclaredMethod("ValidateInterface", params);
						Object res = m.invoke(null, c.getPropertyDesc(), c.getMethodDesc(), c.baseClass != null ? c.baseClass.Name : null, c.Abstract);
						if (!(Boolean)res)
							return new ScriptError(ScriptErrorCode.ClassMismatch, this, 0, null, ClassName);
					} catch (NoSuchMethodException e) {
						return new ScriptError(ScriptErrorCode.InvalidClass, this, 0, null, ClassName+"\n" +
								"The class MUST implement the method\n"+
								"public static boolean ValidateInterface(PropertyDesc[] Properties, MethodDesc[] Methods, String BaseClassName, boolean isAbstract)"
						);
					} catch (Exception e) {
						return new ScriptError(ScriptErrorCode.InvalidClass, this, 0, null, ClassName+"\n"+e);
					}
				}
				c.ExtClassType = ClassType;
				return null;
			}
		}
		return null;
	}

	/**
	* Call this function to supply the script with a set of external global variables
	* that are in ready-to-use form.
	* The values of the hashtable must be of XScripter.Variable type, containing type description as well.
	* The keys of the hashtable are the names of the variables.
	* the hashtable is used as it is, without being copied.
	* This method must be called before compilation.
	**/
	public void SetExtGlobals(HashMap<String, Variable> h)
	{
		extGlobals = h;
	}

	/**
	* this method returns a collection of the classes contained in the script
	**/
	public ArrayList<ScriptClass> GetClassList()
	{
		return classList;
	}

	/**
	* scans the script and creates a list of units that the script imports.
	* it only works on non-compiled scripts.
	**/
	public ArrayList<String> GetPreDependencyList()
	{
		ArrayList<String> list = new ArrayList<String>();
		if (m_Compiled)
			return null;
		try
		{
			int idx = 0;
			do
			{
				idx = m_Text.Text.indexOf('#', idx);
				if (idx != -1)
				{
					if (m_Text.Text.substring(idx + 1, idx + 8).equals("imports"))
					{
						// jump over whitespace:
						idx += 8;
						while (!IsLetter(m_Text.PickAt(idx))) idx++;
						String depName = "";
						// get the dependency name:
						char c = m_Text.PickAt(idx); 
						while (c != ' ' && c != '\t' && c != '\r' && c != '\n')
						{
							depName += c;
							c = m_Text.PickAt(++idx);
						}
						// add it to the list:
						list.add(depName);
					}
					else
						idx++;
				}
			} while (idx != -1);
		}
		catch (IndexOutOfBoundsException e)
		{
		}

		return list;
	}

	public transient ArrayList<IntrinsicFunction> intrinsicFunctions = new ArrayList<IntrinsicFunction>();
	
	/**
	* a list of functions exposed by the script (and it's dependencies also)
	**/
	public ArrayList<Function> functions = new ArrayList<Function>();
	/**
	* a list of variables exposed by the script (and it's dependencies)
	**/
	public ArrayList<Variable> variables = new ArrayList<Variable>();
	/**
	* gets the description for script's own components' (functions, variables, classes, external functions)
	* location within the lists.
	**/
	public LibStackMapping getOwnLevel() { return new LibStackMapping(ownLevel); }

	// internal methods and fields:

	transient private TextStream m_Text;

	transient ArrayList<InstructionBlock> blockStack = new ArrayList<InstructionBlock>();
	transient ArrayList<Variable> varStack = new ArrayList<Variable>();

	HashMap<String,ScriptClass> extClasses = new HashMap<String, ScriptClass>();
	ArrayList<ScriptClass> classList = new ArrayList<ScriptClass>();

 	ArrayList<LibStackMapping> libStack = new ArrayList<LibStackMapping>();
	LibStackMapping ownLevel = new LibStackMapping(); // this is set when directives are over

	transient HashMap<String, Variable> extGlobals = null;
	
	private IWarningHandler m_warnHandler = null;

	/**
	* this gets set to true once the script has been successfully compiled.
	* after that, the script can no longer be changed.
	**/
	private boolean m_Compiled = false;

	transient private boolean m_DirectivesAllowed = true;

	static boolean IsDigit(char l)
	{
		return l >= '0' && l <= '9';
	}

	static boolean IsLetter(char l)
	{
		return ((l >= 'a' && l <= 'z') || (l >= 'A' && l <= 'Z') || l == '_' || (l >= '0' && l <= '9'));
		}

	/**
	 * this method reads a top-level item from the script (that is, something at the root level in the script,
	 * not inside a function).
	 * Such items may be : variables (global or in classes), functions (global or in classes) external function
	 * declarations and external class interfaces.
	 * When a class definition is encountered, the method calls itself recursively to read the members of the class.
	 *
	 * @param Functions list to hold the read functions
	 * @param Variables list to hold the variables
	 * @param ExtFunctions list to hold external function decl
	 * @param extClasses list for classes
	 * @param context compile context
	 * @param Container null if we're outside any class, or the class from which we read.
	 * @throws Exception 
	**/
	boolean ReadTopLevelItem(
		ArrayList<Function> Functions, 
		ArrayList<Variable> Variables,
		ScriptContext context,
		ScriptClass Container)
	{
		m_Text.Advance(0);
		// read identifiers:
		TypeID type = TypeID.None;
		String ident = null;
		String classID = null;

		if (m_Text.Pick() == '#') // found a directive
		{
			if (!m_DirectivesAllowed)
			{
				SetError(new ScriptError(ScriptErrorCode.IllegalDirective, this, getCurrentLine(), null, null));
				return false;
			}
			if (Container != null)
			{
				SetError(new ScriptError(ScriptErrorCode.IllegalDirective, this, getCurrentLine(), null, null));
				return false;
			}
			return ReadDirective(context);
		}
		else
		if (m_DirectivesAllowed)
			{
				m_DirectivesAllowed = false;
				// search all imported functions and update any function call references to our local copies.
				if (!UpdateLocalFunctionReferences())
					return false;
			}
		
		boolean external = false;

		// check for externals:
		m_Text.SavePos();
		ident = ReadIdent(false, context);
		if (ident == null)
			return false;
		if (ident.equals(ResWord.RWI_EXTERNAL))
		{
			if (Container != null)
			{
				SetError(new ScriptError(ScriptErrorCode.TypeIDExpected, this, getCurrentLine(), null, "Cannot use <external> inside class."));
				return false;
			}

			m_Text.SavePos();
			ident = ReadIdent(false, context);
			if (ident == null)
				return false;
			
			boolean Abstract = false;
			if (ident.equals(ResWord.RWI_ABSTRACT))
			{
				Abstract = true;
				ident = ReadIdent(false, context);
				if (ident == null)
					return false;
			}

			// class definition:
			if (ident.equals(ResWord.RWI_CLASS))
			{
				ident = ReadIdent(false, context);
				if (ident == null)
					return false;

				// check if identifier is safe:
				for (int i = 0; i < Variables.size(); i++)
					if (Variables.get(i).name.equals(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, ident));
						return false;
					}
				for (int i = 0; i < ResWord.WordList.length; i++)
					if (ResWord.WordList[i].equals(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, ident));
						return false;
					}
				for (int i = 0; i < Functions.size(); i++)
					if (Functions.get(i).Name.equals(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, ident));
						return false;
					}
				if (extClasses.containsKey(ident) && extClasses.get(ident).isDefined)
				{
					SetError(new ScriptError(ScriptErrorCode.ClassAlreadyDefined, this, getCurrentLine(), null, ident));
					return false;
				}
				if (extGlobals != null && extGlobals.containsKey(ident))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, ident));
					return false;
				}
				//<<<

				ScriptClass c = extClasses.get(ident);
				if (c == null) {
					c = new ScriptClass(this, ident);
					extClasses.put(ident, c);
				}
				c.Abstract = Abstract;
				classList.add(c);
				if (m_Text.Pick() == ':')
				{
					m_Text.Advance(1);
					// read base class type:
					ident = ReadIdent(false, context);
					if (ident == null)
						return false;
					if (!extClasses.containsKey(ident) || !extClasses.get(ident).isDefined)
					{
						SetError(new ScriptError(ScriptErrorCode.ClassIdentExpected, this, getCurrentLine(), context.CurrentFunction, "Superclass "+ident+" must be defined before being used."));
						return false;
					}
					c.baseClass = (ScriptClass)extClasses.get(ident);
				}

				if (m_Text.Pick() != '{')
				{
					SetError(new ScriptError(ScriptErrorCode.LBraceExpected, this, getCurrentLine(), null, "{ expected after class header"));
					return false;
				}
				m_Text.Advance(1);
				while (m_Text.Pick() != '}')
				{
					if (!ReadTopLevelItem(c.methods, c.properties, context, c))
						return false;
				}
				m_Text.Advance(1);

				c.isDefined = true;
				return true;
			}
			else
			{
				m_Text.RestorePos();
				if (Abstract) {
					SetError(new ScriptError(ScriptErrorCode.IllegalModifier, this, getCurrentLine(), null, "abstract can only be used on classes."));
					return false;
				}
			}
			//<<<

			// external var maybe
			external = true;
			//<<<

		}
		else
		{
			m_Text.RestorePos();
		}
		//<<<
		
		ident = ReadIdent(false, context);
		if (ident == null)
			return false;
		
		boolean staticDecl = false;
		boolean readonly = false;
		boolean event = false;
		
		if (ident.equals(ResWord.RWK_STATIC)) {
			staticDecl = true;
			
			if (Container == null) {
				SetError(new ScriptError(ScriptErrorCode.StaticOutsideClass, this, getCurrentLine(), context.CurrentFunction, null));
				return false;
			}
			
			ident = ReadIdent(false, context);
			if (ident == null)
				return false;
		}
		
		if (ident.equals(ResWord.RWK_READONLY)) {
			readonly = true;
			type = ReadTypeIdent(context);
		} else
		if (ident.equals(ResWord.RWI_EVENT)) {
			if (Container == null) {
				SetError(new ScriptError(ScriptErrorCode.EventOutsideClass, this, getCurrentLine(), context.CurrentFunction, null));
				return false;
			}
			if (readonly) {
				SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), context.CurrentFunction, "Illegal readonly"));
				return false;
			}
			if (staticDecl) {
				SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), context.CurrentFunction, "Events cannot be declared static"));
				return false;
			}
			event = true;
		} else
			type = TypeID.FromString(ident);
		
		if (type == TypeID.None && !event) {
			SetError(new ScriptError(ScriptErrorCode.TypeIDExpected, this, getCurrentLine(), context.CurrentFunction, ident));
			return false;
		}

		// check special case : object type: read <subtype>
		if (type == TypeID.Object)
		{
			classID = ReadClassID(context);
			if (classID == null)
				return false;
			if (classID.equals("!empty!"))
				classID = null;
		}
		//<<<
		
		// check array declaration:
		if (m_Text.Pick() == '[')
		{
			if (event) {
				SetError(new ScriptError(ScriptErrorCode.IdentifierExpected, this, getCurrentLine(), context.CurrentFunction, null));
				return false;
			}
			m_Text.Advance(1);
			if (m_Text.Pick() != ']')
			{
				SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(),context.CurrentFunction,m_Text.PickString()));
				return false;
			}
			m_Text.Advance(1);
			if (TypeID.ToArrayType(type) == TypeID.None) {
				SetError(new ScriptError(ScriptErrorCode.InvalidArrayType, this, getCurrentLine(), context.CurrentFunction, type.toString() + "[]"));
				return false;
			}
			type = TypeID.ToArrayType(type);
		}
		
		if (type == TypeID.Object && m_Text.Pick() == '(' && Container != null) {
			if (staticDecl) {
				SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), context.CurrentFunction, "Constructors cannot be declared static"));
				return false;
			}
			// possible constructor encountered.
			if (classID == null || !classID.equals(Container.Name)) {
				SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), null, "Constructors must have the same class name as the enclosing class."));
				return false;
			}
			// ok, constructor seems to be valid :D
			ident = null;
		} else {
			// read the name of the variable / function
			ident = ReadIdent(false, context);
			if (ident == null)
				return false;
		}

		if (m_Text.Pick() == '(')
		// read function declaration
		{
			if (external) {
				SetError(new ScriptError(ScriptErrorCode.ExternalFunc, this, getCurrentLine(), context.CurrentFunction, ident));
				return false;
			}
			if (readonly) {
				SetError(new ScriptError(ScriptErrorCode.ReadonlyFunc, this, getCurrentLine(), context.CurrentFunction, ident));
				return false;
			}
			// we encountered a function declaration
			boolean isConstructor = (ident == null);
			String fname = (ident == null) ? classID : ident;
			
			// check if the identifier is free:
			if (!isConstructor) {
				if (Container != null)
				{
					if (Container.getProperty(ident) != null || Container.getStaticField(ident) != null)
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
				}
				else
					for (int i = 0; i < Variables.size(); i++)
						if (Variables.get(i).name.equals(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
							return false;
						}
				for (int i = 0; i < ResWord.WordList.length; i++)
					if (ResWord.WordList[i].equals(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
				if (extClasses.containsKey(ident))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
					return false;
				}
				if (Container == null && extGlobals != null && extGlobals.containsKey(ident))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
					return false;
				}
			}
			// done checking function name
			
			TypeID funcType = type;
			String funcClassID = classID;
			
			// read param list:
			m_Text.Advance(1);
			ArrayList<Variable> params = new ArrayList<Variable>();
			while (m_Text.Pick() != ')')
			{
				// read param type
				type = ReadTypeIdent(context);
				if (type == TypeID.None)
					return false;
				if (type == TypeID.Void)
				{
					SetError(new ScriptError(ScriptErrorCode.VarCannotBeVoid, this, getCurrentLine(), context.CurrentFunction, null));
					return false;
				}
				if (type == TypeID.Object)
				{
					classID = ReadClassID(context);
					if (classID == null)
						return false;
					if (classID.equals("!empty!"))
						classID = null;
				}
				else
					classID = null;
				if (m_Text.Pick() == '[')
				{
					// array type
					m_Text.Advance(1);
					if (m_Text.Pick() != ']') {
						SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), context.CurrentFunction, m_Text.PickString()));
						return false;
					}
					m_Text.Advance(1);
					if (TypeID.ToArrayType(type) == TypeID.None) {
						SetError(new ScriptError(ScriptErrorCode.InvalidArrayType, this, getCurrentLine(), context.CurrentFunction, type.toString() + "[]"));
						return false;
					}
					type = TypeID.ToArrayType(type);
				}
				//<<<

				// read the name of the parameter:
				ident = ReadIdent(false, context);
				if (ident == null)
					return false;

				params.add(new Variable(type));
				if (Container == null)
					params.get(params.size() - 1).name = ident;
				params.get(params.size() - 1).classID = classID;
				//<<<

				if (m_Text.Pick() == ',')
					m_Text.Advance(1);
				else
					if (m_Text.Pick() != ')')
					{
						SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), context.CurrentFunction, Character.toString(m_Text.Pick())));
						return false;
					}
			}
			//<<< done reading parameter list
			
			// compute the signature of the function header:
			int sig = Function.computeSignature(fname, params);
			boolean declaredBefore = false;
			// check out if the function header had already been declared before
			// if so, there must be no existing definition for the function
			Function func = null;
			
			if (event) {
				if (Container.getEvent(ident) != null) {
					SetError(new ScriptError(ScriptErrorCode.DuplicateEvent, this, getCurrentLine(), null, ident));
					return false;
				}
			} else {
				if (Container == null) {
					for (Function f : Functions) {
						if (f.signature == sig) {
							func = f;
							declaredBefore = true;
							break;
						}
					}
				} else {
					if (isConstructor)
						func = Container.getConstructor(sig);
					else
					if (staticDecl)
						func = Container.getStaticMethod(sig);
					else
						func = Container.getMethod(sig);
					
					declaredBefore = (func != null);
				}
			}
			
			if (declaredBefore) 
			{
				if (Container != null) {
					// inside classes we cannot declare function prototypes multiple times, 
					// like "forward" declarations of functions outside classes
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), func, "This prototype of the function already exists."));
					return false;
				}
				// function prototype exists, check return type consistency:
				if ((func.ReturnValue == null && funcType != TypeID.Void) ||
					(func.ReturnValue != null && (func.ReturnValue.varType != funcType || 
					(func.ReturnValue.classID != null && !func.ReturnValue.classID.equals(funcClassID))))
				) {
					SetError(new ScriptError(ScriptErrorCode.OverloadOnlyReturn, this, getCurrentLine(), func, null));
					return false;
				}
				if (func.InstrBlock != null) {
					SetError(new ScriptError(ScriptErrorCode.FnAlreadyDef, this, getCurrentLine(), func, null));
					return false;
				}
			} else {
				// create the function here, it hasn't been declared before
				func = new Function();
				func.Name = fname;
				func.signature = sig;
				func.Parameters = params;
				
				if (event) {
					Container.events.add(func);
				}
				else {
					if (!isConstructor && funcType != TypeID.Void)
					{
						func.ReturnValue = new Variable(funcType);
						func.ReturnValue.classID = funcClassID;
					}
					
					if (staticDecl) {
						Container.staticMethods.add(func);
					} 
					else if (Container != null) 
					{
						if (isConstructor)
							Container.constructors.add(func);
						else if (!staticDecl)
							Container.methods.add(func);
					} else
						Functions.add(func);
				}
			}

			m_Text.Advance(1);
			char c = m_Text.Pick();
			if (c != '{' && c != ';') // expect '{' for definition or ';' for declaration only
			{
				SetError(new ScriptError(ScriptErrorCode.FnPostHdrExpected, this, getCurrentLine(), func, Character.toString(c)));
				return false;
			}

			// read following code block
			if (c == '{')
			{
				if (event) {
					SetError(new ScriptError(ScriptErrorCode.EventBody, this, getCurrentLine(), null, fname));
					return false;
				}
				
				if (Container != null)
				{
					SetError(new ScriptError(ScriptErrorCode.FuncBodyInClass, this, getCurrentLine(), func, null));
					return false;
				}
				
				func.defineLine = getCurrentLine();

				func.InstrBlock = new InstructionBlock(this);
				if (varStack.size() != 0)
				{
					SetError(new ScriptError(ScriptErrorCode.InternalError, this, 0, func, "Redundant data detected on the variable stack."));
					return false;
				}
				func.nBaseVarOffs = 0;
				// add the return-variable on the stack :
				varStack.add(new Variable(TypeID.None));

				context.CurrentFunction = func;
				if (blockStack.size() != 0)
				{
					SetError(new ScriptError(ScriptErrorCode.InternalError, this, 0, func, "Redundant data detected on the variable stack."));
					return false;
				}
				blockStack.add(func.InstrBlock);
				// insert the function parameters, if any, into the local scope
				for (int i = 0; i < func.Parameters.size(); i++)
					varStack.add(func.Parameters.get(i));
				// read the code block:
				if (!ReadCodeBlock(context))
					return false;
				// check if the function has a return type, and if so
				// there must be a return <value> instruction at the end of the
				// main block. if no return type and no return instruction,
				// append an empty return statement.

				// check return paths
				switch (func.InstrBlock.CheckReturnPaths())
				{
					case NoReturn:
						if (func.ReturnValue == null)
						{
							// append an empty return:
							func.InstrBlock.AddInstruction(new InstrReturn(this));
						}
						else
						{
							SetError(new ScriptError(ScriptErrorCode.MissingRet, this, getCurrentLine(), func, null));
							return false;
						}
						break;
					case AllReturn:
						// OK - the normal case
						break;
					case PartialReturn:
						if (func.ReturnValue == null)
							// append an empty return:
							func.InstrBlock.AddInstruction(new InstrReturn(this));
						else
						{
							SetError(new ScriptError(ScriptErrorCode.IncompleteReturnPath, this, getCurrentLine(), func, null));
							return false;
						}
						break;
					case UnreachableCode:
					{
						sendWarning(new ScriptError(ScriptErrorCode.UnreachableCode, this, func.defineLine, func, null));
					}
				}
				//<<<

				// done. cleanup :
				context.CurrentFunction = null;
				// pop instruction block from the block stack :
				blockStack.remove(blockStack.size() - 1);
				// pop parameters from the scope:
				varStack.clear();
			}
			else
			{
				m_Text.Advance(1);
			}
			//<<<
		}
		//<<<
		else
		// read variable
		{
			if (event) {
				SetError(new ScriptError(ScriptErrorCode.LBracketExpected, this, getCurrentLine(), null, "Event declaration needs argument list"));
				return false;
			}
			
			boolean exist = false;
			do
			{
				// create global variable / class property
				// if there is an initialization value for the global vars,
				// generate error (global vars cannot be initialized)

				// check for duplicate id:
				if (Container != null)
				{
					if (staticDecl) {
						if (Container.getStaticField(ident) != null)
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
							return false;
						}
					} 
					else
					if (Container.getProperty(ident) != null)
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
				}
				else
					if (!external) {
						for (int i = 0; i < Variables.size(); i++)
							if (Variables.get(i).name.equals(ident))
							{
								SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
								return false;
							}
					}
				
				for (int i = 0; i < ResWord.WordList.length; i++)
					if (ResWord.WordList[i].equals(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
				if (Container != null)
				{
					if (staticDecl) {
						if (Container.hasStaticMethodsWithName(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
							return false;
						}
					}
					else
					if (Container.hasMethodsWithName(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
				}
				else
					if (!external) {
						for (int i = 0; i < Functions.size(); i++)
							if (Functions.get(i).Name.equals(ident))
							{
								SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
								return false;
							}
					}
				if (extClasses.containsKey(ident))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), context.CurrentFunction, ident));
					return false;
				}
				
				if (external) {
					if ((extGlobals == null || !extGlobals.containsKey(ident)))
					{
						SetError(new ScriptError(ScriptErrorCode.ExtVarNotFound, this, getCurrentLine(), context.CurrentFunction, ident));
						return false;
					}
					if (m_Text.Pick() != ';')
					{
						SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), context.CurrentFunction, "; expected after external var"));
						return false;
					}
					m_Text.Advance(1);
					
					//TODO for EXTERNALS check readonly, type and clsID
					//if (readonly != ExtGlobals.get(ident).readonly)
					return true;
				}
				//<<<

				// create var:
				Variable v = new Variable(type);
				v.readonly = readonly;
				v.classID = classID;
				v.name = ident;
				if (staticDecl)
					Container.staticFields.add(v);
				else
					Variables.add(v);
				// expect , or ;
				m_Text.Advance(0);
				if (m_Text.Pick() == '=')
				{
					SetError(new ScriptError(Container == null ? ScriptErrorCode.GlobalInit : ScriptErrorCode.FieldInit, this, getCurrentLine(), context.CurrentFunction, ident));
					return false;
				}
				if (m_Text.Pick() != ',' && m_Text.Pick() != ';')
				{
					SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), context.CurrentFunction, "; or , expected. Found '" + m_Text.Pick() + "'"));
					return false;
				}
				if (m_Text.Pick() == ',')
				{
					m_Text.Advance(1);
					ident = ReadIdent(false, context);
					if (ident == null)
						return false;
					exist = true; // there is still a var to process
				}
				else
				{
					exist = false; // no more vars
				}
			} while (exist);
			// now we are positioned on a ';'. skip it:
			m_Text.Advance(1);
		}
		//<<<

		return true;
	}

	/**
	* search for call instructions in all imported functions and update any reference to functions with the local copies.
	 * @throws Exception 
	**/
	private boolean UpdateLocalFunctionReferences()
	{
		for (Function f : functions)
		{
			if (!f.InstrBlock.UpdateFunctionReferences(functions))
				return false;
		}
		return true;
	}
	
	/**
	* copies components from the given library, from the specified stack region, performing duplicate id checking
	* and resolving relocation references for inner functions and variables (by duplicating functions, 
	* offseting variable references and updating function call pointers).
	*
	* @param s library to copy from
	* @param from region to copy from
	 * @throws Exception 
	**/
	private boolean AddLibraryComponents(Script s, LibStackMapping from)
	{
		LibStackMapping lmap = new LibStackMapping(ownLevel);
		// compute the offset for variables:
		int varOffs = ownLevel.VariableLevel - s.ownLevel.VariableLevel;
		// now start copying components:
		
		// variables:
		for (int k = from.VariableLevel, cnt = 0; cnt < from.VariableCount; cnt++, k++)
		{
			Variable v = s.variables.get(k);
			// duplicate name check
			for (Function fn : functions)
				if (v.name.equals(fn.Name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, v.name + " in imported library."));
					return false;
				}
			for (Variable vn : variables)
				if (v.name.equals(vn.name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, v.name + " in imported library."));
					return false;
				}
			for (ScriptClass c : extClasses.values())
				if (v.name.equals(c.Name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, v.name + " in imported library."));
					return false;
				}
			//<<<
			variables.add(v);
			lmap.VariableCount++;
			ownLevel.VariableLevel++;
		}
		// functions:
		for (int k = from.FunctionLevel, cnt = 0; cnt < from.FunctionCount; cnt++, k++)
		{
			Function f = s.functions.get(k);
			// duplicate name check
			for (Function fn : functions)
				if (f.Name.equals(fn.Name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			for (Variable v : variables)
				if (f.Name.equals(v.name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			for (ScriptClass c : extClasses.values())
				if (f.Name.equals(c.Name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			//<<<
			functions.add(f.CreateOffsetedClone(varOffs));
			if (m_Error != null)
				return false;
			lmap.FunctionCount++;
			ownLevel.FunctionLevel++;
		}
		// classes:
		for (int k = from.ClassLevel, cnt = 0; cnt < from.ClassCount; cnt++, k++)
		{
			ScriptClass f = s.classList.get(k);
			// duplicate name check
			for (Function fn : functions)
				if (f.Name.equals(fn.Name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			for (Variable v : variables)
				if (f.Name.equals(v.name))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			for (String sn : extClasses.keySet())
				if (f.Name.equals(sn))
				{
					SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), null, f.Name + " in imported library."));
					return false;
				}
			//<<<
			extClasses.put(f.Name, f);
			classList.add(f);
			lmap.ClassCount++;
			ownLevel.ClassLevel++;
		}
		
		libStack.add(lmap);
		return true;
	}

	/**
	* adds a library along with it's dependencies, checking for duplicate imports, 
	* and saving the stack region mapping of each library.
	 * @throws Exception 
	**/
	private boolean AddLibrary(Script s)
	{
		if (Dependencies.contains(s.Name)) // already in the list
			return true;
			
		// check the library's own dependencies:
		for (int i = 0; i < s.Dependencies.size(); i++)
		{
			if (Dependencies.contains(s.Dependencies.get(i)))
				continue;
			
			// we need this sub-dependency:
			Dependencies.add(s.Dependencies.get(i));
			// now we need to copy that sub-dependency's components as well:
			if (!AddLibraryComponents(s,s.libStack.get(i)))
				return false;
		}

		// add the lib name to our dependencies list:
		Dependencies.add(s.Name);
		// add lib's own components :
		return AddLibraryComponents(s,s.ownLevel);
	}

	private boolean ReadDirective(ScriptContext C)
	{
		if (m_Text.Pick() != '#')
		{
			SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), null, null));
			return false;
		}
		m_Text.Advance(1);
		String dir = null;
		dir = ReadIdent(false, C);
		if (dir == null)
		{
			m_Error.Code = ScriptErrorCode.DirectiveExpected;
			return false;
		}
		if (dir.equals("unit"))
		{
			if (this.Name != null)
			{
				SetError(new ScriptError(ScriptErrorCode.UnitDirectiveRecurence, this, getCurrentLine(), null, null));
				return false;
			}
			Name = ReadIdent(false, C);
			return (Name != null);
		}

		if (dir.equals("import"))
		{
			String libURL = "";
			//libName = ReadIdent(false, C);
			char ch;
			while ((ch = m_Text.Pick()) != (char)0xff) {
				libURL += ch;
				m_Text.Advance(-1);
			}
			m_Text.Advance(0);
			if (libURL == "")
				return false;

			if (C.libFetcher == null) {
				SetError(new ScriptError(ScriptErrorCode.NoLibraryFetcher, this, getCurrentLine(), null, null));
				return false;
			}
			
			Script slib = C.libFetcher.getLibrary(libURL);
			if (slib == null) {
				SetError(new ScriptError(ScriptErrorCode.LibraryNotFound, this, getCurrentLine(), null, libURL));
				return false;
			}
			
			if (!slib.IsCompiled()) {
				SetError(new ScriptError(ScriptErrorCode.LibraryNotCompiled, this, getCurrentLine(), null, libURL));
				return false;
			}

			// check to see if the library is already in our dependency list:
			for (int i = 0; i < Dependencies.size(); i++)
			{
				if (Dependencies.get(i).equals(slib.Name))
					return true;
			}
			
			return AddLibrary(slib);
		}
		
		SetError(new ScriptError(ScriptErrorCode.UnknownDirective, this, getCurrentLine(), null, dir));
		return false;
	}

	/**
	* Reads the next identifier from the text.
	*
	* @param text the String to receive the identifier in
	* @param number true if the identifier expected is a number.
	* if this is set to false and the first character in the next identifier is a digit,
	* an error is raised.
	* @param context the script context
	**/
	String ReadIdent(boolean number, ScriptContext context)
	{
		m_Text.Advance(0);
		String s = null;

		if (IsDigit(m_Text.Pick()) && !number)
		{
			SetError(new ScriptError(ScriptErrorCode.IdentifierExpected, this, getCurrentLine(), context.CurrentFunction, null));
			return null;
		}

		while (IsLetter(m_Text.Pick()))
		{
			if (s == null)
			{
				s = Character.toString(m_Text.Pick());
				m_Text.Advance(-1);
			}
			else
			{
				s = s.concat(Character.toString(m_Text.Pick()));
				m_Text.Advance(-1);
			}
		}
		m_Text.Advance(0);
		if (s == null)
		{
			SetError(new ScriptError(ScriptErrorCode.IdentifierExpected, this, getCurrentLine(), context.CurrentFunction, null));
			return null;
		}
		return s;
	}

	private TypeID ReadTypeIdent(ScriptContext context)
	{
		String ident = ReadIdent(false, context);
		if (ident == null)
		{
			SetError(new ScriptError(ScriptErrorCode.TypeIDExpected, this, getCurrentLine(), context.CurrentFunction, null));
			return TypeID.None;
		}
		TypeID t = TypeID.FromString(ident);
		if (t == TypeID.None)
		{
			SetError(new ScriptError(ScriptErrorCode.TypeIDExpected, this, getCurrentLine(), context.CurrentFunction, ident));
			return TypeID.None;
		}
		return t;
	}

	private String ReadClassID(ScriptContext C)
	{
		if (m_Text.Pick() != '<')
		{
			SetError(new ScriptError(ScriptErrorCode.ObjTypeExpected, this, getCurrentLine(), C.CurrentFunction, "'<' expected."));
			return null;
		}
		m_Text.Advance(1);

		// check for empty class id (generic object)
		if (m_Text.Pick() == '>')
		{
			m_Text.Advance(1);
			return "!empty!";
		}

		String ident = ReadIdent(false, C);
		if (ident == null)
			return null;
		if (!extClasses.containsKey(ident))
		{
			//SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, "Unknown class identifier: " + ident));
			//return null;
			extClasses.put(ident, new ScriptClass(this, ident));
		}
		if (m_Text.Pick() != '>')
		{
			SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "'>' expected after class identifier."));
			return null;
		}
		
		m_Text.Advance(1);
		return ident;
	}

	private String ReadSymbol()
	{
		String symb = null;
		m_Text.Advance(0);
		char c = m_Text.Pick();
		if ((c >= '!' && c <= '/')
			|| (c >= ':' && c <= '@')
			|| (c >= '[' && c <= '^')
			|| (c >= '{' && c <= '}'))
			symb = Character.toString(c);
		m_Text.Advance(1);
		char c1 = m_Text.Pick();
		if (c1 == '=' || (c == '+' && c == '+')
			|| (c1 == '-' && c == '-'))
		{
			symb += c1;
			m_Text.Advance(1);
		}
		return symb;
	}

	private boolean ReadInstruction(ScriptContext C)
	{
		m_Text.Advance(0);

		boolean ExpectSemiColon = true;
		// this reads a single instruction which may actually be 
		// an instruction block or variable declaration
		Instruction pBaseInstr = null;

		if (m_Text.Pick() == '#')
		{
			SetError(new ScriptError(ScriptErrorCode.IllegalDirective, this, getCurrentLine(), C.CurrentFunction, null));
			return false;
		}

		if (m_Text.Pick() == '{')
		{
			// create a new instruction block and read it
			// instr block
			InstrBlock pInstr = new InstrBlock(this);
			pInstr.Block = new InstructionBlock(this);
			blockStack.add(pInstr.Block);
			if (!ReadCodeBlock(C))
				return false;
			blockStack.remove(blockStack.size() - 1);
			pBaseInstr = pInstr;
			m_Text.Advance(0);
			ExpectSemiColon = false;
			//<<<
		}
		else
		{
			// 1 . assignment
			// 2 . function/method call
			// 3 . return / while / for / if
			// 4 . var decl
			String ident = ReadIdent(false, C);
			if (ident == null)
				return false;
			boolean readonly = false;
			if (ident.equals(ResWord.RWK_READONLY)) {
				readonly = true;
				ident = ReadIdent(false, C);
				if (ident == null)
					return false;
			}
			TypeID type = TypeID.FromString(ident);
			String ExtInterfaceID = null; // for objects
			if (type != TypeID.None)
			{
				// var decl
				if (m_Text.Pick() == ';')
				{
					SetError(new ScriptError(ScriptErrorCode.IdentifierExpected, this, getCurrentLine(), C.CurrentFunction, "Found ';' after type name"));
					return false;
				}

				// check special case : object type: read <subtype>
				if (type == TypeID.Object)
				{
					ExtInterfaceID = ReadClassID(C);
					if (ExtInterfaceID == null)
						return false;
					if (ExtInterfaceID.equals("!empty!"))
						ExtInterfaceID = null;
				}
				//<<<
				// check for array-type:
				if (m_Text.Pick() == '[')
				{
					m_Text.Advance(1);
					if (m_Text.Pick() != ']') {
						SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
						return false;
					}
					m_Text.Advance(1);
					if (TypeID.ToArrayType(type) == TypeID.None) {
						SetError(new ScriptError(ScriptErrorCode.InvalidArrayType, this, getCurrentLine(), C.CurrentFunction, type.toString() + "[]"));
						return false;
					}
					type = TypeID.ToArrayType(type);
				}

				while (m_Text.Pick() != ';')
				{
					// read a sequence of vars
					ident = ReadIdent(false, C);
					if (ident == null)
						return false;
					// check to see if the name already is taken in the current scope:
					// duplicate ident check
					// first the functions
					for (int i = 0; i < functions.size(); i++)
						if (functions.get(i).Name.equals(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
							return false;
						}
					// scope variables:
					for (int i = 0; i < varStack.size(); i++)
						if (varStack.get(i).name != null && varStack.get(i).name.equals(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
							return false;
						}
					// global vars:
					for (int i = 0; i < variables.size(); i++)
						if (variables.get(i).name.equals(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
							return false;
						}
					// external global vars:
					if (extGlobals != null && extGlobals.containsKey(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
						return false;
					}
					// classes:
					if (extClasses.containsKey(ident))
					{
						SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
						return false;
					}
					// check the reserved words:
					for (int i = 0; i < ResWord.WordList.length; i++)
						if (ResWord.WordList[i].equals(ident))
						{
							SetError(new ScriptError(ScriptErrorCode.DuplicateIdentifier, this, getCurrentLine(), C.CurrentFunction, ident));
							return false;
						}
					//<<<
					// ok, the identifier is safe, let's create the var :D
					C.CurrentInstrBlock().CreateVariable(type);
					Variable v = C.CurrentInstrBlock().LocalVars.get(C.CurrentInstrBlock().LocalVars.size() - 1); 
					if (type == TypeID.Object || type.isArray())
						v.classID = ExtInterfaceID;
					v.name = ident;
					v.readonly = readonly;
					varStack.add(v);
					// init value
					if (m_Text.Pick() == '=')
					{
						// read an expression and create
						// an initialization instruction
						m_Text.Advance(1);
						pBaseInstr = new InstrAssign(this);
						// compute the relative index of the variable:
						int varID = varStack.size() - 1 - C.CurrentFunction.nBaseVarOffs;
						// read expression:
						((InstrAssign)pBaseInstr).Expression = ReadExpression(m_Text, C);
						if (((InstrAssign)pBaseInstr).Expression == null) 
							return false;
						// special case : treat null assignment:
						//if ((pBaseInstr as InstrAssign).Expression.IsNull())
						//{
						//    if (C.CurrentInstrBlock().LocalVars[C.CurrentInstrBlock().LocalVars.size() - 1].VarType != TypeID.Object)
						//        return new ScriptError(ScriptErrorCode.IncompatTypeOp, getCurrentLine(), C.CurrentFunction, "(Initial value)");
						//}
						//else
						if (TypeID.None == ExpressionNode.CheckTypes(
								v.varType, v.classID,
								((InstrAssign)pBaseInstr).Expression.ResultType,
								((InstrAssign)pBaseInstr).Expression.ResultClassID,
								ExpressionOperator.OpAttrib, this))
						{
							SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, "(Initial value)"));
							return false;
						}
						// fill in the instruction:
						((InstrAssign)pBaseInstr).DestVarOffs = varID;
						C.CurrentInstrBlock().AddInstruction(pBaseInstr);
					}
					if (m_Text.Pick() != ',' && m_Text.Pick() != ';')
					{
						SetError(new ScriptError(ScriptErrorCode.DelimiterExpected, this, getCurrentLine(), C.CurrentFunction, null));
						return false;
					}
					if (m_Text.Pick() == ',')
					{
						m_Text.Advance(1);
						if (m_Text.Pick() == ';')
						{
							SetError(new ScriptError(ScriptErrorCode.IdentifierExpected, this, getCurrentLine(), C.CurrentFunction, null));
							return false;
						}
					}
					//<<<
				}
				//<<<
				m_Text.Advance(1);
				return true;
			} // done with var decl 
			if (readonly) {
				SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "Illegal readonly"));
				return false;
			}
			if (ident.equals(ResWord.RWF_DEBUG))
			{
				pBaseInstr = new InstrDebug(this);
			}
			else
			if (ident.equals(ResWord.RWF_BREAK))
			{
				// break
				pBaseInstr = new InstrBreak(this);
				for (int i = C.BlockStack.size() - 1; i >= 0; i--)
					if (C.BlockStack.get(i).breakable)
					{
						((InstrBreak)pBaseInstr).BlockCount = C.BlockStack.size() - i;
					}
				if (((InstrBreak)pBaseInstr).BlockCount == 0)
				{
					SetError(new ScriptError(ScriptErrorCode.IllegalBreak, this, getCurrentLine(), C.CurrentFunction, null));
					return false;
				}
				//<<<
			}
			else
				if (ident.equals(ResWord.RWF_RETURN))
				{
					// return
					pBaseInstr = new InstrReturn(this);
					if (m_Text.Pick() != ';')
					{
						// we have an expression following return:
						((InstrReturn)pBaseInstr).ReturnValue = ReadExpression(m_Text, C);
						if (((InstrReturn)pBaseInstr).ReturnValue == null)
							return false;
						// type-check the expression against the function return type:
						if (C.CurrentFunction.ReturnValue == null)
						{
							SetError(new ScriptError(ScriptErrorCode.FuncCannotRetValue, this, getCurrentLine(), C.CurrentFunction, null));
							return false;
						}
						if (TypeID.None == ExpressionNode.CheckTypes(
								C.CurrentFunction.ReturnValue.varType, C.CurrentFunction.ReturnValue.classID,
								((InstrReturn)pBaseInstr).ReturnValue.ResultType, ((InstrReturn)pBaseInstr).ReturnValue.ResultClassID,
								ExpressionOperator.OpAttrib, this))
						{
							SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, "Function return type missmatch"));
							return false;
						}
						// it is safe.
					}
					else if (C.CurrentFunction.ReturnValue != null)
					{
						SetError(new ScriptError(ScriptErrorCode.MissingRet, this, getCurrentLine(), C.CurrentFunction, null));
						return false;
					}
					//<<<
				}
				else
					if (ident.equals(ResWord.RWC_IF))
					{
						// if
						// read expression then instruction (which may be a block)
						// if there is an "else" after the block, read one more instruction
						// read expression
						Expression pExpr = null;
						if (m_Text.Pick() != '(')
						{
							SetError(new ScriptError(ScriptErrorCode.LBracketExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
							return false;
						}
						m_Text.Advance(1);
						pExpr = ReadExpression(m_Text, C);
						if (pExpr == null)
							return false;
						if (pExpr.ResultType != TypeID.Bool) {
							SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, "if statement needs bool type expression"));
							return false;
						}
						if (m_Text.Pick() != ')')
						{
							SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
							return false;
						}
						m_Text.Advance(1);
						pBaseInstr = new InstrIf(this);
						((InstrIf)pBaseInstr).Expression = pExpr;
						//<<<
						// check if the true-block is made up of a single instruction:
						// true block
						boolean explicit_block = true;
						if (m_Text.Pick() != '{')
						{
							explicit_block = false;
							// if the only instruction found is a variable declaration, generate an error:
							m_Text.SavePos();
							String id = null;
							id = ReadIdent(false, C);
							if (id == null)
								return false;
							if (TypeID.FromString(id) != TypeID.None)
							{
								SetError(new ScriptError(ScriptErrorCode.VarNeverUsed, this, getCurrentLine(), C.CurrentFunction, id));
								return false;
							}
							m_Text.RestorePos();
						}
						// read the true-block instruction (which may be a block):
						// create a temporary block in which to read, in order
						// to avoid the instruction being appended to the outside block
						InstructionBlock pBlock = null;
						if (!explicit_block)
						{
							pBlock = new InstructionBlock(this);
							blockStack.add(pBlock);
						}
						if (!ReadInstruction(C))
							return false;
						
						if (explicit_block)
						{
							((InstrIf)pBaseInstr).IfInstr = C.CurrentInstrBlock().CurrentInstr; // the instruction block
							// remove this instr from the outside block:
							C.CurrentInstrBlock().RemoveLast();
							if (m_Error != null)
								return false;
						}
						else
						{
							// remove the local variables of pBlock from the stack:
							Tools.removeRange(varStack, varStack.size() - pBlock.LocalVars.size(), pBlock.LocalVars.size());
							// pop the block from the block stack:
							blockStack.remove(blockStack.size() - 1);
							((InstrIf)pBaseInstr).IfInstr = new InstrBlock(this);
							((InstrBlock)((InstrIf)pBaseInstr).IfInstr).Block = pBlock;
						}
						pBlock = null;
						m_Text.Advance(0);
						//<<<
						// check if we have an else statement:
						// false block
						m_Text.SavePos();
						ident = ReadIdent(false, C);
						if (ident != null && ident.equals(ResWord.RWC_ELSE))
						{
							// check if the false-block is made up of a single instruction:
							explicit_block = true;
							if (m_Text.Pick() != '{')
							{
								explicit_block = false;
								// if the only instruction found is a variable declaration, generate an error:
								m_Text.SavePos();
								String id = ReadIdent(false, C);
								if (id == null)
									return false;
								if (TypeID.FromString(id) != TypeID.None)
								{
									SetError(new ScriptError(ScriptErrorCode.VarNeverUsed, this, getCurrentLine(), C.CurrentFunction, id));
									return false;
								}
								m_Text.RestorePos();
							}
							if (!explicit_block)
							{
								pBlock = new InstructionBlock(this);
								blockStack.add(pBlock);
							}
							if (!ReadInstruction(C))
								return false;
							if (!explicit_block)
							{
								Tools.removeRange(varStack, varStack.size() - pBlock.LocalVars.size(), pBlock.LocalVars.size());
								blockStack.remove(blockStack.size() - 1);
							}
							if (explicit_block)
							{
								((InstrIf)pBaseInstr).ElseInstr = C.CurrentInstrBlock().CurrentInstr;
								C.CurrentInstrBlock().RemoveLast();
							}
							else
							{
								((InstrIf)pBaseInstr).ElseInstr = new InstrBlock(this);
								((InstrBlock)((InstrIf)pBaseInstr).ElseInstr).Block = pBlock;
							}
							pBlock = null;
							m_Text.Advance(0);
						}
						else
						{
							m_Error = null;
							m_Text.RestorePos();
						}
						//<<<
						ExpectSemiColon = false;
						//<<<
					}
					else
						if (ident.equals(ResWord.RWC_FOR))
						{
							// for
							// create a container block for this structure:
							InstructionBlock fb = new InstructionBlock(this);
							// set this block as breakable:
							fb.breakable = true;
							C.BlockStack.add(fb);
							// now read the initial instruction:
							if (m_Text.Pick() != '(')
							{
								SetError(new ScriptError(ScriptErrorCode.LBracketExpected, this, getCurrentLine(), C.CurrentFunction, "<for> statement missing '('"));
								return false;
							}
							m_Text.Advance(1);
							if (!ReadInstruction(C))
								return false;
							// eval block
							InstrFor f = new InstrFor(this);
							f.EvalTempVarID = GetTempVar(C, TypeID.Bool);
							if (m_Error != null)
								return false;
							f.EvalBlock = new InstructionBlock(this);
							C.BlockStack.add(f.EvalBlock);
							InstrAssign asgn = new InstrAssign(this);
							asgn.DestVarOffs = f.EvalTempVarID;
							asgn.Expression = ReadExpression(m_Text, C);
							if (asgn.Expression == null)
								return false;
							if (asgn.Expression.ResultType != TypeID.Bool)
							{
								SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, "<for> statement continuation condition must be of type <boolean>"));
								return false;
							}
							if (m_Text.Pick() != ';')
							{
								SetError(new ScriptError(ScriptErrorCode.SemiColonExpected, this, getCurrentLine(), C.CurrentFunction, "Missing ; after <for> expression"));
								return false;
							}
							m_Text.Advance(1);
							C.CurrentInstrBlock().AddInstruction(asgn);
							Tools.removeRange(C.VarStack, C.VarStack.size() - C.CurrentInstrBlock().LocalVars.size(), C.CurrentInstrBlock().LocalVars.size());
							C.BlockStack.remove(C.BlockStack.size() - 1); // eval block
							//<<<
							// read step instruction
							Instruction cinstr = fb.CurrentInstr;
							if (!ReadInstruction(C))
								return false;
							// this will be appended later to the end of the do-block, and removed from here.
							//<<<
							if (m_Text.Pick() != ')')
							{
								SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), C.CurrentFunction, "<for> statement missing ')'"));
								return false;
							}
							m_Text.Advance(1);
							// read do-block
							boolean explicit_block = true;
							if (m_Text.Pick() != '{')
							{
								explicit_block = false;
								// if the only instruction found is a variable declaration, generate an error:
								m_Text.SavePos();
								String id = null;
								id = ReadIdent(false, C);
								if (id == null)
									return false;
								if (TypeID.FromString(id) != TypeID.None)
								{
									SetError(new ScriptError(ScriptErrorCode.VarNeverUsed, this, getCurrentLine(), C.CurrentFunction, id));
									return false;
								}
								m_Text.RestorePos();
							}
							// read the do-block instruction (which may be a block):
							// create a temporary block in which to read, in order
							// to avoid the instruction being appended to the outside block
							InstructionBlock pBlock = null;
							if (!explicit_block)
							{
								pBlock = new InstructionBlock(this);
								blockStack.add(pBlock);
							}
							if (!ReadInstruction(C))
								return false;
							if (!explicit_block)
							{
								// remove the local variables of pBlock from the stack:
								Tools.removeRange(varStack, varStack.size() - pBlock.LocalVars.size(), pBlock.LocalVars.size());
								// pop the block from the block stack:
								blockStack.remove(blockStack.size() - 1);
							}
							if (explicit_block)
							{
								f.DoBlock = ((InstrBlock)C.CurrentInstrBlock().CurrentInstr).Block;
								C.CurrentInstrBlock().RemoveLast();
							}
							else
								f.DoBlock = pBlock;
							pBlock = null;
							m_Text.Advance(0);
							//<<<
							Tools.removeRange(C.VarStack, C.VarStack.size() - C.CurrentInstrBlock().LocalVars.size(), C.CurrentInstrBlock().LocalVars.size());
							C.BlockStack.remove(C.BlockStack.size() - 1); // remove fb - container block
							f.DoBlock.AddInstruction(cinstr.Next);
							cinstr.Next = null;
							fb.CurrentInstr = cinstr;
							fb.AddInstruction(f);
							pBaseInstr = new InstrBlock(this);
							((InstrBlock)pBaseInstr).Block = fb;
							ExpectSemiColon = false;
							//<<<
						}
						else
							if (ident.equals(ResWord.RWC_WHILE))
							{
								// while
								// create a container block:
								InstructionBlock wb = new InstructionBlock(this);
								wb.breakable = true;
								C.BlockStack.add(wb);
								// parse expression and evaluate block
								if (m_Text.Pick() != '(')
								{
									SetError(new ScriptError(ScriptErrorCode.LBracketExpected, this, getCurrentLine(), C.CurrentFunction, "<while> statement missing '('"));
									return false;
								}
								m_Text.Advance(1);
								// create a temp var in the current block, then
								// create a block into which we read the expression
								// and add an assignment instruction to assign the value of the expression
								// to our temp var (which will be used by the loop to determine when to stop)
								InstrWhile whi = new InstrWhile(this);
								whi.EvalTempVarID = GetTempVar(C, TypeID.Bool);
								if (m_Error != null)
									return false;
								whi.EvalBlock = new InstructionBlock(this);
								C.BlockStack.add(whi.EvalBlock);
								InstrAssign asgn = new InstrAssign(this);
								asgn.DestVarOffs = whi.EvalTempVarID;
								asgn.Expression = ReadExpression(m_Text, C);
								if (asgn.Expression == null)
									return false;
								if (m_Text.Pick() != ')')
								{
									SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), C.CurrentFunction, "<while> statement missing ')'"));
									return false;
								}
								m_Text.Advance(1);
								C.CurrentInstrBlock().AddInstruction(asgn);
								InstructionBlock blk = C.BlockStack.get(C.BlockStack.size() - 1);
								Tools.removeRange(C.VarStack, C.VarStack.size() - blk.LocalVars.size() - 1, blk.LocalVars.size());
								C.BlockStack.remove(C.BlockStack.size() - 1);
								//<<<
								// then read an additional block containing the following instruction
								// read do-block
								boolean explicit_block = true;
								if (m_Text.Pick() != '{')
								{
									explicit_block = false;
									// if the only instruction found is a variable declaration, generate an error:
									m_Text.SavePos();
									String id = ReadIdent(false, C);
									if (id == null)
										return false;
									if (TypeID.FromString(id) != TypeID.None)
									{
										SetError(new ScriptError(ScriptErrorCode.VarNeverUsed, this, getCurrentLine(), C.CurrentFunction, id));
										return false;
									}
									m_Text.RestorePos();
								}
								// read the true-block instruction (which may be a block):
								InstructionBlock pBlock = null;
								if (!explicit_block)
								{
									// create a temporary block in which to read, in order
									// to avoid the instruction being appended to the outside block
									pBlock = new InstructionBlock(this);
									blockStack.add(pBlock);
								}
								if (!ReadInstruction(C))
									return false;
								if (!explicit_block)
								{
									// remove the local variables of pBlock from the stack:
									Tools.removeRange(varStack, varStack.size() - pBlock.LocalVars.size(), pBlock.LocalVars.size());
									// pop the block from the block stack:
									blockStack.remove(blockStack.size() - 1);
								}
								if (explicit_block)
								{
									whi.DoBlock = ((InstrBlock)C.CurrentInstrBlock().CurrentInstr).Block;
									C.CurrentInstrBlock().RemoveLast();
								}
								else
									whi.DoBlock = pBlock;
								pBlock = null;
								m_Text.Advance(0);
								//<<<
								wb.AddInstruction(whi);
								Tools.removeRange(C.VarStack, C.VarStack.size() - C.CurrentInstrBlock().LocalVars.size(), C.CurrentInstrBlock().LocalVars.size());
								C.BlockStack.remove(C.BlockStack.size() - 1); // remove wb - container block
								pBaseInstr = new InstrBlock(this);
								((InstrBlock)pBaseInstr).Block = wb;
								ExpectSemiColon = false;
								//<<<
							}
							else
								if (ident.equals(ResWord.RWC_DO))
								{
									// do
									// create a container block:
									InstructionBlock db = new InstructionBlock(this);
									db.breakable = true;
									C.BlockStack.add(db);

									int varID = GetTempVar(C, TypeID.Bool);
									if (m_Error != null)
										return false;
									// do block
									boolean explicit_block = true;
									if (m_Text.Pick() != '{')
									{
										explicit_block = false;
										// if the only instruction found is a variable declaration, generate an error:
										m_Text.SavePos();
										String id = ReadIdent(false, C);
										if (id == null)
											return false;
										if (TypeID.FromString(id) != TypeID.None)
										{
											SetError(new ScriptError(ScriptErrorCode.VarNeverUsed, this, getCurrentLine(), C.CurrentFunction, id));
											return false;
										}
										m_Text.RestorePos();
									}
									// read the do-block instruction (which may be a block):
									InstructionBlock pBlock = null;
									if (!explicit_block)
									{
										// create a temporary block in which to read, in order
										// to avoid the instruction being appended to the outside block
										pBlock = new InstructionBlock(this);
										blockStack.add(pBlock);
									}
									if (!ReadInstruction(C))
										return false;
									// now expect "until" and read the expression following it
									ident = ReadIdent(false, C);
									if (ident == null)
										return false;
									if (!ident.equals(ResWord.RWC_UNTIL))
									{
										SetError(new ScriptError(ScriptErrorCode.UntilExpected, this, getCurrentLine(), C.CurrentFunction, ident));
										return false;
									}
									// until expression
									if (m_Text.Pick() != '(')
									{
										SetError(new ScriptError(ScriptErrorCode.LBracketExpected, this, getCurrentLine(), C.CurrentFunction, "Missing '(' after <until>"));
										return false;
									}
									m_Text.Advance(1);
									InstrAssign ias = new InstrAssign(this);
									ias.DestVarOffs = varID;
									ias.Expression = ReadExpression(m_Text, C);
									if (ias.Expression == null)
										return false;
									if (m_Text.Pick() != ')')
									{
										SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), C.CurrentFunction, null));
										return false;
									}
									m_Text.Advance(1);
									//<<<
									if (!explicit_block)
									{
										// remove the local variables of pBlock from the stack:
										Tools.removeRange(varStack, varStack.size() - pBlock.LocalVars.size(), pBlock.LocalVars.size());
										// pop the block from the block stack:
										blockStack.remove(blockStack.size() - 1);
									}
									pBaseInstr = new InstrDo(this);
									((InstrDo)pBaseInstr).EvalVarID = varID;
									if (explicit_block)
									{
										((InstrDo)pBaseInstr).DoBlock = ((InstrBlock)C.CurrentInstrBlock().CurrentInstr).Block;
										C.CurrentInstrBlock().RemoveLast();
									}
									else
										((InstrDo)pBaseInstr).DoBlock = pBlock;
									((InstrDo)pBaseInstr).DoBlock.AddInstruction(ias);
									pBlock = null;

									// pop container block:
									Tools.removeRange(C.VarStack, C.VarStack.size() - C.CurrentInstrBlock().LocalVars.size(), C.CurrentInstrBlock().LocalVars.size());
									C.BlockStack.remove(C.BlockStack.size() - 1); // remove db - container block

									db.AddInstruction(pBaseInstr);
									pBaseInstr = new InstrBlock(this);
									((InstrBlock)pBaseInstr).Block = db;
									m_Text.Advance(0);
									//<<<
									//<<<
								}
								else
								{
									// no more reserved words. possible scenarios left :
									// assignment / function call / ext object property set / ext object method call / error :D
									// check functions first:
									boolean found = false;
									for (int i=0; i<functions.size(); i++)
										if (functions.get(i).Name.equals(ident)) {
											found = true;
											break;
										}
									if (found) {
										// function call
										ArrayList<Expression> elist = ReadCallParamList(C, m_Text);
										if (elist == null)
											return false;
										// try to find a matching function prototype:
										Function func = MatchFuncVersion(ident, elist, functions);
										if (func == null) {
											SetError(new ScriptError(ScriptErrorCode.OverloadNotFound, this, getCurrentLine(), C.CurrentFunction, ident));
											return false;
										}
										
										pBaseInstr = new InstrCall(this);
										((InstrCall)pBaseInstr).func = func;
										((InstrCall)pBaseInstr).parameters = elist;
										elist = null;
									}

									//<<<
									if (!found)
									{
										// assignment
										// check vars:
										int varID = -1;
										String staticClassID = null;
										
										for (int i = varStack.size() - 1; i >= 0; i--)
											if (ident.equals(varStack.get(i).name))
											{
												varID = i - C.CurrentFunction.nBaseVarOffs;
												found = true;
												break;
											}
										if (!found) // then search global vars:
											for (int i = 0; i < variables.size(); i++)
											{
												if (ident.equals(variables.get(i).name))
												{
													varID = 0x8000 | i;
													found = true;
													break;
												}
											}
										if (!found)
											if (extGlobals != null && extGlobals.containsKey(ident))
											{
												varID = 0x4000;
												found = true;
												// check if the global variable type is known (for class types only)
												if (!CheckExtGlobal(C, C.VarByID(varID, ident)))
													return false;
											}
										if (!found) {
											if (extClasses.containsKey(ident)) {
												staticClassID = ident;
												found = true;
											}
										}
										if (found)
										{
											// check for arrays:
											Expression exprArrayIndex = null;
											if (varID != -1 && C.VarByID(varID, ident).varType.isArray() && m_Text.Pick() == '[')
											{
												m_Text.Advance(1);
												exprArrayIndex = ReadExpression(m_Text, C);
												if (exprArrayIndex == null)
													return false;
												if (exprArrayIndex.ResultType != TypeID.Int) {
													SetError(new ScriptError(ScriptErrorCode.InvalidArrayIndexSpecifier, this, getCurrentLine(), C.CurrentFunction, 
															"Expected int, received " + exprArrayIndex.ResultType.toString()));
												}
												if (m_Text.Pick() != ']') {
													SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
													return false;
												}
												m_Text.Advance(1);
											}
											// read symbol
											// expect assignment, ++/-- or '.' (member access)
											String symb = ReadSymbol();
											if (symb == null)
											{
												SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, null));
												return false;
											}

											// this holds the id of the variable that contains a reference to an object that we access
											// if the operator is '.'
											int objID = -1;
											Variable prop = null;
											// if it's null, then no member access takes place.
											// prop will contain the handle of the accessed property.

											boolean ObjMethodCall = false;
											boolean eventSubscription = false;
											boolean staticAccess = false;
											Expression exprPropertyArrayIndex = null;
											
											if (staticClassID != null) {
												// apparently this is a static access / call
												if (!symb.equals(".")) {
													SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "'.' expected after class name \""+staticClassID+"\" for static access."));
													return false;
												}												
												staticAccess = true;												
											}
											
											// check object member access
											String objName = ident;
											if (staticAccess || (symb.equals(".") && C.VarByID(varID, objName).varType.BaseType() == TypeID.Object))
											{
												if (!staticAccess && C.VarByID(varID, objName).classID == null)
												{
													SetError(new ScriptError(ScriptErrorCode.GenericObjectAccess, this, getCurrentLine(), C.CurrentFunction, objName));
													return false;
												}

												objID = varID;
												varID = -1;
												
												ident = ReadIdent(false, C); // read the member name
												if (ident == null)
													return false;
												
												// property write
												if (staticAccess)
													prop = GetClass(staticClassID).getStaticField(ident);
												else
													prop = GetClass(C.VarByID(objID, objName).classID).getProperty(ident);
												if (prop != null)
												{
													// check for indexed property:												
													if (prop.varType.isArray() && m_Text.Pick() == '[')
													{
														m_Text.Advance(1);
														exprPropertyArrayIndex = ReadExpression(m_Text, C);
														if (exprPropertyArrayIndex == null)
															return false;
														if (exprPropertyArrayIndex.ResultType != TypeID.Int) {
															SetError(new ScriptError(ScriptErrorCode.InvalidArrayIndexSpecifier, this, getCurrentLine(), C.CurrentFunction, 
																	"Expected int, received " + exprPropertyArrayIndex.ResultType.toString()));
															return false;
														}
														if (m_Text.Pick() != ']') {
															SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
															return false;
														}
														m_Text.Advance(1);
													}
													//======
													
													// object with var index (objID) is assigned a value to it's field <prop>
													// OR
													// class with name staticClassID is assigned a value to it's static field <prop>
													symb = ReadSymbol();
													if (symb == null)
													{
														SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, null));
														return false;
													}
												}
												//<<<
												// method call
												if (prop == null && 
													(
														(staticAccess && GetClass(staticClassID).hasStaticMethodsWithName(ident))
														||
														GetClass(C.VarByID(objID, objName).classID).hasMethodsWithName(ident)
													)
												) // no properties found with that name; it must be a method, or an event
												{
													ArrayList<Expression> elist = ReadCallParamList(C, m_Text);
													if (elist == null)
														return false;
													Function meth;
													if (staticAccess)
														meth = GetClass(staticClassID).matchStaticMethod(ident, elist);
													else
														meth = GetClass(C.VarByID(objID, objName).classID).matchMethod(ident, elist);
													
													if (meth == null) {
														SetError(new ScriptError(ScriptErrorCode.OverloadNotFound, this, getCurrentLine(), C.CurrentFunction, ident));
														return false;
													}
													// now create the call
													pBaseInstr = new InstrMethodCall(this);
													((InstrMethodCall)pBaseInstr).MethodName = meth.Name;
													((InstrMethodCall)pBaseInstr).className = staticClassID;
													((InstrMethodCall)pBaseInstr).staticCall = staticAccess;
													((InstrMethodCall)pBaseInstr).ObjectID = objID;
													((InstrMethodCall)pBaseInstr).exprIndexFrom = exprArrayIndex;
													if (objID == 0x4000) // external global object
														((InstrMethodCall)pBaseInstr).ObjectName = objName;
													((InstrMethodCall)pBaseInstr).Parameters = elist;
													elist = null;
													ObjMethodCall = true;
												}
												//<<<
												// event subscription:
												if (prop == null && !ObjMethodCall && !staticAccess) {
													Function event = GetClass(C.VarByID(objID, objName).classID).getEvent(ident); 
													if (event == null) {
														SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, ident));
														return false;
													}
													
													// read the right side of the assignment:
													String s = ReadSymbol();
													if (s == null || !s.equals("="))
													{
														SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "'=' Expected"));
														return false;
													}
													
													s = ReadIdent(false, C);
													if (s == null)
														return false;
													
													// search for a function with the given name and the same parameters as the event
													Function handler = null;
													for (Function f : functions) {
														if (f.Parameters.size() != event.Parameters.size())
															continue;
														if (!f.Name.equals(s))
															continue;
														handler = f;
														for (int i=0; i<f.Parameters.size(); i++) {
															Variable ep = event.Parameters.get(i);
															Variable fp = f.Parameters.get(i);
															if (ep.varType != fp.varType) {
																handler = null;
																break;
															}
															if (ep.varType == TypeID.Object) {
																if (ep.classID != null) {
																	if (fp.classID == null || !ep.classID.equals(fp.classID)) {
																		handler = null;
																		break;
																	}
																} else
																	if (fp.classID != null) {
																		handler = null;
																		break;
																	}
															}
														}
													}
													if (handler == null) {
														SetError(new ScriptError(ScriptErrorCode.InvalidEvHandler, this, getCurrentLine(), C.CurrentFunction, s));
														return false;
													}
													if (handler.ReturnValue != null) {
														SetError(new ScriptError(ScriptErrorCode.InvalidEvHandler, this, getCurrentLine(), C.CurrentFunction, s + ": return type for handlers must be void."));
														return false;
													}
													
													// create the instruction
													pBaseInstr = new InstrAssignEvent(this);
													((InstrAssignEvent)pBaseInstr).eventName = ident;
													((InstrAssignEvent)pBaseInstr).ObjectID = objID;
													((InstrAssignEvent)pBaseInstr).exprIndexFrom = exprArrayIndex;
													if (objID == 0x4000) // external global object
														((InstrAssignEvent)pBaseInstr).ObjectName = objName;
													((InstrAssignEvent)pBaseInstr).handlerName = handler.Name;
													
													eventSubscription = true;
												}
												//<<<
											}
											//<<<
											if (!ObjMethodCall && !eventSubscription)
											{
												if ((symb.charAt(symb.length() - 1) != '=' || symb.equals("=="))
													&& !symb.equals("++") && !symb.equals("--"))
												{
													SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, symb));
													return false;
												}
												m_Text.Advance(0);
												Expression pExpr = null;
												TypeID tid = (objID == -1 && !staticAccess)
													? C.VarByID(varID, ident).varType
													: prop.varType;
												if ((exprArrayIndex != null && objID == -1 && !staticAccess) ||
													exprPropertyArrayIndex != null)
													tid = tid.BaseType();
												if (symb.equals("++") || symb.equals("--"))
												{
													// only int and float cand be incremented/decremented
													if (tid != TypeID.Float && tid != TypeID.Int)
													{
														SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, "Invalid operator '" + symb + "' for variable type"));
														return false;
													}
													pExpr = new Expression(this);
													pExpr.ResultType = tid;
													pExpr.RootNode = new ExpressionNode();
													pExpr.RootNode.Operator = symb.equals("++") ? ExpressionOperator.OpAdd : ExpressionOperator.OpSubtract;
													pExpr.RootNode.Parent = pExpr;
													pExpr.RootNode.NodeType = ExpressionNodeType.Operator;
													pExpr.RootNode.valueType = tid;
													int tmpID = 0;
													if (objID != -1 || staticAccess)
													{
														tmpID = GetTempVar(C, tid);
														if (m_Error != null)
															return false;
														// create a property-read instruction before this one, to get the property value to increment
														InstrPropRead pr = new InstrPropRead(this);
														pr.ObjectID = objID;
														if (objID == 0x4000) // external global object
															pr.ObjectName = objName;
														pr.className = staticClassID;
														pr.staticRead = staticAccess;
														pr.exprIndexFrom = exprArrayIndex;
														pr.exprIndexInto = exprPropertyArrayIndex;
														pr.Property = prop;
														pr.DestVarID = tmpID;
														C.CurrentInstrBlock().AddInstruction(pr);
													}
													
													// if modifying an array element, we need to create an array-read instruction and substitute
													// the refered element in the expression with the temp var to wich we read
													if (objID == -1 && exprArrayIndex != null) 
													{
														tmpID = GetTempVar(C, tid);
														if (m_Error != null)
															return false;
														InstrArrayRead ird = new InstrArrayRead(this);
														ird.arrayID = varID;
														ird.destVarID = tmpID;
														ird.exprIndex = exprArrayIndex;
														ird.extArrayName = ident;
														C.CurrentInstrBlock().AddInstruction(ird);
													}
													
													pExpr.RootNode.Left = new ExpressionNode();
													pExpr.RootNode.Left.Parent = pExpr;
													pExpr.RootNode.Left.NodeType = ExpressionNodeType.Variable;
													pExpr.RootNode.Left.valueRef = (objID == -1 && exprArrayIndex == null && !staticAccess) ? varID : tmpID;
													if ((Integer)pExpr.RootNode.Left.valueRef == 0x4000)
														pExpr.RootNode.Left.ExtGlobalName = ident;
													pExpr.RootNode.Left.valueType = tid;
													pExpr.RootNode.Right = new ExpressionNode();
													pExpr.RootNode.Right.Parent = pExpr;
													pExpr.RootNode.Right.NodeType = ExpressionNodeType.Value;
													pExpr.RootNode.Right.valueRef = (int)1;
													pExpr.RootNode.Right.valueType = TypeID.Int;
												}
											//<<<
												// read the expression on the right
												//*********	***************************************************************************************************************************************
												// and assign it to the var in different ways
												else
												{
													// read expression
													int tmpID = 0;
													if ((objID != -1 || staticAccess) && !symb.equals("="))
													{
														tmpID = GetTempVar(C, tid);
														if (m_Error != null)
															return false;
														// create a property-read instruction before this one, to get the property value to modify
														InstrPropRead pr = new InstrPropRead(this);
														pr.ObjectID = objID;
														pr.className = staticClassID;
														pr.staticRead = staticAccess;
														if (objID == 0x4000) // external global object
															pr.ObjectName = objName;
														pr.Property = prop;
														pr.DestVarID = tmpID;
														pr.exprIndexFrom = exprArrayIndex;
														pr.exprIndexInto = exprPropertyArrayIndex;
														C.CurrentInstrBlock().AddInstruction(pr);
													}
													if (objID == -1 && !staticAccess && exprArrayIndex != null && !symb.equals("="))
													{
														// create an array read instruction to get the var to modify from the array
														tmpID = GetTempVar(C, tid);
														if (m_Error != null)
															return false;
														InstrArrayRead ird = new InstrArrayRead(this);
														ird.arrayID = varID;
														ird.destVarID = tmpID;
														ird.exprIndex = exprArrayIndex;
														ird.extArrayName = ident;
														C.CurrentInstrBlock().AddInstruction(ird);
													}
													
													pExpr = ReadExpression(m_Text, C);
													if (pExpr == null) 
														return false;
													if (!symb.equals("=") && !symb.equals("+=") &&
														!symb.equals("-=") && !symb.equals("*=") &&
														!symb.equals("/=") && !symb.equals("|=") && !symb.equals("&=") && !symb.equals("^="))
													{
														SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, symb));
														return false;
													}
													//create expressions for other symbols, so the assignment is always '='
													if (!symb.equals("="))
													{
														pExpr.AuxNodes.add(pExpr.RootNode);
														pExpr.RootNode = new ExpressionNode();
														pExpr.RootNode.Operator = ExpressionOperator.FromString(symb.substring(0, symb.length() - 1));
														pExpr.RootNode.Parent = pExpr;
														pExpr.RootNode.NodeType = ExpressionNodeType.Operator;
														TypeID tr = ExpressionNode.CheckTypes(
																tid, null,
																pExpr.ResultType, null,
																pExpr.RootNode.Operator, this);
														if (tr == TypeID.None)
														{
															SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, null));
															return false;
														}
														pExpr.RootNode.valueType = tr;
														pExpr.ResultType = pExpr.RootNode.valueType;
														pExpr.ResultClassID = null;
														pExpr.RootNode.Left = new ExpressionNode();
														pExpr.RootNode.Left.Parent = pExpr;
														pExpr.RootNode.Left.NodeType = ExpressionNodeType.Variable;
														pExpr.RootNode.Left.valueRef = (objID == -1 && !staticAccess && exprArrayIndex == null) ? varID : tmpID;
														if ((Integer)pExpr.RootNode.Left.valueRef == 0x4000)
															pExpr.RootNode.Left.ExtGlobalName = ident;
														pExpr.RootNode.Left.valueType = tid;
														pExpr.RootNode.Right = pExpr.AuxNodes.get(pExpr.AuxNodes.size() - 1);
														pExpr.AuxNodes.remove(pExpr.AuxNodes.size() - 1);
													}
													else
													{
														// check the type compatibility
														String cid = (objID == -1 && !staticAccess) ? C.VarByID(varID, ident).classID : prop.classID;
														if (TypeID.None == ExpressionNode.CheckTypes(
																tid, cid,
																pExpr.ResultType, pExpr.ResultClassID,
																ExpressionOperator.OpAttrib, this))
														{
															SetError(new ScriptError(ScriptErrorCode.IncompatTypeOp, this, getCurrentLine(), C.CurrentFunction, null));
															return false;
														}
													}
												}
													//<<<
												if (objID == -1 && !staticAccess)
												{
													if (C.VarByID(varID, ident).readonly && exprArrayIndex == null) {
														SetError(new ScriptError(ScriptErrorCode.ReadonlyModify, this, getCurrentLine(), C.CurrentFunction, C.VarByID(varID, ident).name));
														return false;
													}
													pBaseInstr = new InstrAssign(this);
													((InstrAssign)pBaseInstr).DestVarOffs = varID;
													if (varID == 0x4000)
														((InstrAssign)pBaseInstr).ExtGlobalName = ident;
													((InstrAssign)pBaseInstr).Expression = pExpr;
													((InstrAssign)pBaseInstr).exprIndex = exprArrayIndex;
												}
												else
												{
													if (prop.readonly && exprPropertyArrayIndex == null) {
														SetError(new ScriptError(ScriptErrorCode.ReadonlyModify, this, getCurrentLine(), C.CurrentFunction, prop.name));
														return false;
													}
													pBaseInstr = new InstrPropWrite(this);
													((InstrPropWrite)pBaseInstr).exprValue = pExpr;
													((InstrPropWrite)pBaseInstr).ObjectID = objID;
													((InstrPropWrite)pBaseInstr).staticWrite = staticAccess;
													((InstrPropWrite)pBaseInstr).className = staticClassID;
													if (objID == 0x4000)
														((InstrPropWrite)pBaseInstr).ObjectName = objName;
													((InstrPropWrite)pBaseInstr).Property = prop;
													((InstrPropWrite)pBaseInstr).exprIndexFrom = exprArrayIndex;
													((InstrPropWrite)pBaseInstr).exprIndexInto = exprPropertyArrayIndex;
												}
											}
										}
										//<<<
									}
									if (!found) 
									{
										SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, ident));
										return false;
									}
								}
		}

		// end of instruction, expect ';'
		m_Text.Advance(0);
		if (ExpectSemiColon)
		{
			if (m_Text.Pick() != ';')
			{
				SetError(new ScriptError(ScriptErrorCode.SemiColonExpected, this, getCurrentLine(), C.CurrentFunction, m_Text.PickString()));
				return false;
			}
			m_Text.Advance(1);
		}

		C.CurrentInstrBlock().AddInstruction(pBaseInstr);

		// clear temp variables:
		for (Variable v : C.TempVars)
		{
			v.varType = TypeID.None;
		}
		C.TempVars.clear();

		return true;
	}

	/**
	* This method checks if the class type of the given external global variable is known to the current context
	**/
	boolean CheckExtGlobal(ScriptContext C, Variable variable)
	{
		if (variable.varType == TypeID.Object && variable.classID != null && !extClasses.containsKey(variable.classID))
		{
			SetError(new ScriptError(ScriptErrorCode.UnknownExtGlobalClassType, this, getCurrentLine(), C.CurrentFunction, variable.classID));
			return false;
		}
		return true;
	}

	boolean ReadCodeBlock(ScriptContext context)
	{
		if (m_Text.Pick() != '{')
		{
			SetError(new ScriptError(ScriptErrorCode.LBraceExpected, this, getCurrentLine(), context.CurrentFunction, m_Text.PickString()));
			return false;
		}
		m_Text.Advance(1);

		// check for empty block:
		if (m_Text.Pick() == '}')
		{
			sendWarning(new ScriptError(ScriptErrorCode.EmptyBlock, this, getCurrentLine(), context.CurrentFunction, null));
		}

		// read instructions:
		while (m_Text.Pick() != '}')
		{
			if (!ReadInstruction(context))
				return false;
		}
		m_Text.Advance(1);
		// pop local vars from the scope:
		Tools.removeRange(varStack, varStack.size() - context.CurrentInstrBlock().LocalVars.size(), context.CurrentInstrBlock().LocalVars.size());
		return true;
	}

	Expression ReadExpression(TextStream text, ScriptContext C)
	{
		// ReadExpression stops when it comes by a ',' ';' or a ')'
		// reads an expression contained or not in '()'
		// if it finds function calls, it takes them outside (before the evaluation) and reads their 
		// parameters as expressions recursively, replacing the calls in the initial expression with 
		// temporary variables to which the function results have been assigned.

		text.Advance(0);
		String str = ""; int ipar = 0;
		String ident = "";
		boolean quote = false;
		char c;
		while ((quote && (c=text.Pick()) != 0) || ((c = text.Pick()) != ',' && c != ';' && c != ']' && (c != ')' || ipar > 0)))
		{
			boolean fcall = false;

			if (c == '"')
				quote = !quote;

			if (!quote)
			{
				if (IsLetter(c) && (!IsDigit(c) || !ident.isEmpty()))
					ident += c;
				else
				{
					// we have a possible identifier.
					if (!ident.isEmpty())
					{
						text.Advance(0);
						// check if the identifier is the "new" operator:
						// new operator
						if (ident.equals(ResWord.RWI_NEW))
						{
							// read type identifier:
							TypeID typeID = ReadTypeIdent(C);
							if (typeID == TypeID.None) {
								SetError(new ScriptError(ScriptErrorCode.TypeIDExpected, this, getCurrentLine(), C.CurrentFunction, "new operator."));
								return null;
							}
							String classID = null;
							if (typeID == TypeID.Object)
							{
								classID = ReadClassID(C);
								if (classID == null) {
									SetError(new ScriptError(ScriptErrorCode.ClassExpected, this, getCurrentLine(), C.CurrentFunction, "new operator."));
									return null;
								}
								if (classID.equals("!empty!"))
									classID = null;
							}
							if (text.Pick() == '[' || classID == null)
							{
								// we have a potential array here; check brackets
								if (text.Pick() != '[') {
									SetError(new ScriptError(ScriptErrorCode.ArraySizeExpected, this, getCurrentLine(), C.CurrentFunction, "Creating array of type "+typeID.toString()));
									return null;
								}
								text.Advance(1);
								Expression exprSize = ReadExpression(text, C);
								if (exprSize == null)
									return null;
								if (exprSize.ResultType != TypeID.Int)
								{
									SetError(new ScriptError(ScriptErrorCode.InvalidArraySizeSpecifier, this, getCurrentLine(), C.CurrentFunction, "Expected int, received " + exprSize.ResultType.toString()));
									return null;
								}
								if (text.Pick() != ']') {
									SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, text.PickString()));
									return null;
								}
								text.Advance(1);
								if (TypeID.ToArrayType(typeID) == TypeID.None) {
									SetError(new ScriptError(ScriptErrorCode.InvalidArrayType, this, getCurrentLine(), C.CurrentFunction, typeID.toString() + "[]"));
									return null;
								}
								typeID = TypeID.ToArrayType(typeID); 
								InstrNewArray inewa = new InstrNewArray(this);
								inewa.destVarID = GetTempVar(C, typeID);
								if (m_Error != null)
									return null;
								C.VarByID(inewa.destVarID, null).classID = classID;
								inewa.exprSize = exprSize;
								inewa.classID = classID;
								inewa.elementType = typeID.BaseType();
								C.CurrentInstrBlock().AddInstruction(inewa);
								str = C.VarByID(inewa.destVarID, null).name;

								if (text.Pick() == '(') {
									SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "'(' Array initialization cannot specify constructor parameters."));
									return null;
								}
								break;
							}
							// --- done with arrayz
							
							if (!extClasses.containsKey(classID)) {
								SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, "Unknown class : " + classID));
								return null;
							}
							if (extClasses.get(classID).Abstract) {
								SetError(new ScriptError(ScriptErrorCode.AbstractInstantiate, this, getCurrentLine(), C.CurrentFunction, classID));
								return null;
							}
							
							// check if we have constructor parameters
							if (text.Pick() != '(') {
								SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "'new' operator: '(' or '[' expected after type name"));
								return null;
							}
							ArrayList<Expression> lparams = ReadCallParamList(C, text);
							if (lparams == null)
								return null;
							Function constr = extClasses.get(classID).getConstructor(Function.computeCallSignature(classID, lparams));
							if (constr == null) {
								SetError(new ScriptError(ScriptErrorCode.OverloadNotFound, this, getCurrentLine(), C.CurrentFunction, "Constructor "+classID));
								return null;
							}
							
							InstrNew inew = new InstrNew(this);
							inew.ClassID = classID;
							inew.paramList = lparams;
							inew.DestVarID = GetTempVar(C, TypeID.Object);
							if (m_Error != null)
								return null;
							C.VarByID(inew.DestVarID, null).classID = classID;
							C.CurrentInstrBlock().AddInstruction(inew);
							str = C.VarByID(inew.DestVarID, null).name;
							break;
						}
						//<<<
						// check if the identifier is a function call:
						Function func = null;
						boolean intrinsicFunc = false;
						for (int i = 0; i < functions.size(); i++)
							if (ident.equals(functions.get(i).Name))
							{
								func = functions.get(i);
								break;
							}
						if (func == null)
							for (int i = 0; i < intrinsicFunctions.size(); i++)
								if (ident.equals(intrinsicFunctions.get(i).Name))
								{
									func = functions.get(i);
									intrinsicFunc = true;
									break;
								}
						if (func != null)
						{
							// read the parameter list:
							ArrayList<Expression> lparams = ReadCallParamList(C, text);
							if (lparams == null) 
								return null;
							func = MatchFuncVersion(ident, lparams, intrinsicFunc?intrinsicFunctions:functions);
							if (func == null) {
								SetError(new ScriptError(ScriptErrorCode.OverloadNotFound, this, getCurrentLine(), C.CurrentFunction, ident));
								return null;
							}
							
							// check for void function:
							if (func.ReturnValue == null)
							{
								SetError(new ScriptError(ScriptErrorCode.VoidFuncInExpr, this, getCurrentLine(), C.CurrentFunction, func.Name));
								return null;
							}
							// we found a valid function call inside the expression.
							
							// create the call instruction:
							InstrCall instr = new InstrCall(this);
							instr.func = func;
							instr.parameters = lparams;
							instr.intrinsic = intrinsicFunc;
							C.CurrentInstrBlock().AddInstruction(instr);
							// now use/create a temporary variable to store the return value:
							int varID = GetTempVar(C, func.ReturnValue.varType);
							if (m_Error != null)
								return null;
							C.VarByID(varID, null).classID = func.ReturnValue.classID;
							InstrAssign asgn = new InstrAssign(this);
							asgn.DestVarOffs = varID;
							asgn.Expression = new Expression(this);
							asgn.Expression.ResultType = func.ReturnValue.varType;
							asgn.Expression.RootNode = new ExpressionNode();
							asgn.Expression.RootNode.Parent = asgn.Expression;
							asgn.Expression.RootNode.NodeType = ExpressionNodeType.Variable;
							asgn.Expression.RootNode.valueType = func.ReturnValue.varType;
							asgn.Expression.RootNode.valueRef = 0; // the id of the return variable
							C.CurrentInstrBlock().AddInstruction(asgn);
							// remove the name of the function from the expression:
							str = str.substring(0, str.length() - ident.length());
							// now insert into our expression the name of the temp variable we used
							str += C.VarByID(varID, null).name;
							fcall = true;
						}
						//<<<
						// check for class member read
						// class member read or method call
						// check for array-access
						Expression exprIndexFrom = null;
						Expression exprIndexInto = null;
						int arrayVarID = -1;
						if (text.Pick() == '[')
						{
							// check if the identifier is a valid variable name
							for (int i = varStack.size() - 1; i >= 0; i--)
								if (ident.equals(varStack.get(i).name))
								{
									arrayVarID = i - C.CurrentFunction.nBaseVarOffs;
									break;
								}
							if (arrayVarID == -1) // search global vars:
								for (int i = 0; i < variables.size(); i++)
								{
									if (ident.equals(variables.get(i).name))
									{
										arrayVarID = 0x8000 | i;
										break;
									}
								}
							if (arrayVarID == -1) // search external globals:
								if (extGlobals != null && extGlobals.containsKey(ident))
								{
									arrayVarID = 0x4000;
									// check if the global variable type is known (for class types only)
									if (!CheckExtGlobal(C, C.VarByID(arrayVarID, ident)))
										return null;
								}
							if (arrayVarID == -1)
							{
								SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, ident));
								return null;
							}
							if (!C.VarByID(arrayVarID, null).varType.isArray())
							{
								SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "Cannot use indexing operator [] on variables that are not arrays."));
								return null;
							}
							
							text.Advance(1);
							exprIndexFrom = ReadExpression(text, C);
							if (exprIndexFrom == null)
								return null;
							if (exprIndexFrom.ResultType != TypeID.Int) {
								SetError(new ScriptError(ScriptErrorCode.InvalidArrayIndexSpecifier, this, getCurrentLine(), C.CurrentFunction, "Expected int, received " + exprIndexFrom.ResultType.toString()));
								return null;
							}
							if (text.Pick() != ']') {
								SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, null));
								return null;
							}
							text.Advance(1);
						}
						//<<<
						if (text.Pick() == '.')
						{
							// check if the identifier is a valid object variable name
							int objID = -1;
							for (int i = varStack.size() - 1; i >= 0; i--)
								if (ident.equals(varStack.get(i).name))
								{
									objID = i - C.CurrentFunction.nBaseVarOffs;
									break;
								}
							if (objID == -1) // search global vars:
								for (int i = 0; i < variables.size(); i++)
								{
									if (ident.equals(variables.get(i).name))
									{
										objID = 0x8000 | i;
										break;
									}
								}
							if (objID == -1) // search external globals:
								if (extGlobals != null && extGlobals.containsKey(ident))
								{
									objID = 0x4000;
									// check if the global variable type is known (for class types only)
									if (!CheckExtGlobal(C, C.VarByID(objID, ident)))
										return null;
								}
							String objName = ident;
							boolean staticAccess = false;
							String staticClassID = null;
							if (objID == -1 && extClasses.containsKey(ident)) {
								staticAccess = true;
								staticClassID = ident;
							}
							
							if (objID == -1 && !staticAccess)
							{
								SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, ident));
								return null;
							}
							if (!staticAccess && C.VarByID(objID, objName).varType.BaseType() != TypeID.Object)
							{
								SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "Cannot use '.' operator on variables that are not objects"));
								return null;
							}
							if (!staticAccess && C.VarByID(objID, objName).classID == null)
							{
								SetError(new ScriptError(ScriptErrorCode.GenericObjectAccess, this, getCurrentLine(), C.CurrentFunction, objName));
								return null;
							}
							text.Advance(1);
							String member = ReadIdent(false, C);
							if (member == null)
								return null;
							Variable prop;
							if (staticAccess)
								prop = GetClass(staticClassID).getStaticField(member);
							else
								prop = GetClass(C.VarByID(objID, objName).classID).getProperty(member);
							if (prop != null)
							{
								// check for indexed property:
								if (text.Pick() == '[')
								{
									// check if the field is an array
									if (!prop.varType.isArray())
									{
										SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "Cannot use indexing operator [] on variables that are not arrays."));
										return null;
									}
									
									text.Advance(1);
									exprIndexInto = ReadExpression(text, C);
									if (exprIndexInto == null)
										return null;
									if (exprIndexInto.ResultType != TypeID.Int) {
										SetError(new ScriptError(ScriptErrorCode.InvalidArrayIndexSpecifier, this, getCurrentLine(), C.CurrentFunction, "Expected int, received " + exprIndexInto.ResultType.toString()));
										return null;
									}
									if (text.Pick() != ']') {
										SetError(new ScriptError(ScriptErrorCode.RightIndexBracketExpected, this, getCurrentLine(), C.CurrentFunction, null));
										return null;
									}
									text.Advance(1);
								}
								// --------
								
								// create a read instruction and replace the text in expression with temp var name
								int tmpID = GetTempVar(C, prop.varType.BaseType());
								if (m_Error != null)
									return null;
								C.VarByID(tmpID, null).classID = prop.classID;
								InstrPropRead iread = new InstrPropRead(this);
								iread.DestVarID = tmpID;
								iread.ObjectID = objID;
								iread.className = staticClassID;
								iread.staticRead = staticAccess;
								if (objID == 0x4000)
									iread.ObjectName = objName;
								iread.exprIndexFrom = exprIndexFrom;
								iread.exprIndexInto = exprIndexInto;
								iread.Property = prop;
								C.CurrentInstrBlock().AddInstruction(iread);
								// remove the name of the object from the expression:
								str = str.substring(0, str.length() - objName.length());
								// now insert into our expression the name of the temp variable we used
								str += C.VarByID(tmpID, null).name;
								fcall = true;
							}
							else
							{
								// check methods
								if (
									(staticAccess && GetClass(staticClassID).hasStaticMethodsWithName(member))
									||
									(!staticAccess && GetClass(C.VarByID(objID, objName).classID).hasMethodsWithName(member))
								)
								{
									ArrayList<Expression> elist = ReadCallParamList(C, text);
									if (elist == null)
										return null;
									Function meth;
									if (staticAccess)
										meth = GetClass(staticClassID).matchStaticMethod(member, elist);
									else
										meth = GetClass(C.VarByID(objID, objName).classID).matchMethod(member, elist);
									if (meth == null) {
										SetError(new ScriptError(ScriptErrorCode.OverloadNotFound, this, getCurrentLine(), C.CurrentFunction, member));
										return null;
									}
									InstrMethodCall icall = new InstrMethodCall(this);
									icall.MethodName = member;
									icall.ObjectID = objID;
									icall.staticCall = staticAccess;
									icall.className = staticClassID;
									if (objID == 0x4000)
										icall.ObjectName = objName;
									icall.Parameters = elist;
									icall.exprIndexFrom = exprIndexFrom;
									C.CurrentInstrBlock().AddInstruction(icall);

									int tmpID = GetTempVar(C, meth.ReturnValue.varType);
									if (m_Error != null)
										return null;
									C.VarByID(tmpID, null).classID = meth.ReturnValue.classID;
									InstrAssign asgn = new InstrAssign(this);
									asgn.DestVarOffs = tmpID;
									asgn.Expression = new Expression(this);
									asgn.Expression.ResultType = meth.ReturnValue.varType;
									asgn.Expression.RootNode = new ExpressionNode();
									asgn.Expression.RootNode.Parent = asgn.Expression;
									asgn.Expression.RootNode.NodeType = ExpressionNodeType.Variable;
									asgn.Expression.RootNode.valueType = meth.ReturnValue.varType;
									asgn.Expression.RootNode.valueRef = 0; // the id of the return variable
									C.CurrentInstrBlock().AddInstruction(asgn);
									// remove the name of the object from the expression:
									str = str.substring(0, str.length() - ident.length());
									// now insert into our expression the name of the temp variable we used
									str += C.VarByID(tmpID, null).name;
									fcall = true;
								}
								else
								{
									// we're in a obj."stuff" scenario, stuff is neither a prop nor a meth
									
									// maybe someone tried to access a static through an instance. let's check:
									if (objID != -1 && (
											GetClass(C.VarByID(objID, objName).classID).hasStaticMethodsWithName(member)
											||
											GetClass(C.VarByID(objID, objName).classID).getStaticField(member) != null
											)
										) {
										
										SetError(new ScriptError(ScriptErrorCode.StaticAccessThroughInstance, this, getCurrentLine(), C.CurrentFunction, member));
										return null;
									}
									
									// otherwise, only one choice left:
									SetError(new ScriptError(ScriptErrorCode.UnknownIdent, this, getCurrentLine(), C.CurrentFunction, member));
									return null;
									//}
								}
							}
						}
						else // there is no dot access.
						if (exprIndexFrom != null)
						{
							TypeID arrayType = C.VarByID(arrayVarID, null).varType;
							InstrArrayRead iard = new InstrArrayRead(this);
							iard.destVarID = GetTempVar(C, arrayType.BaseType());
							if (m_Error != null)
								return null;
							iard.exprIndex = exprIndexFrom;
							iard.arrayID = arrayVarID;
							iard.extArrayName = iard.arrayID == 0x4000 ? ident : null;
							C.VarByID(iard.destVarID, null).classID = C.VarByID(arrayVarID, ident).classID;
							C.CurrentInstrBlock().AddInstruction(iard);
							
							// remove the name of the array from the expression:
							str = str.substring(0, str.length() - ident.length());
							// now insert into our expression the name of the temp variable we used
							str += C.VarByID(iard.destVarID, null).name;
							continue;
						}
						//-----------
					}
					ident = "";
				}
			}

			if (fcall) continue;

			if ((text.Pick() != ' ' || quote) && text.Pick() != (char)0xff)
				str += text.Pick();
			if (text.Pick() == '(') ipar++;
			if (text.Pick() == ')') ipar--;
			text.Advance(-1);
		}

		if (ipar > 0)
		{
			SetError(new ScriptError(ScriptErrorCode.RBracketExpected, this, getCurrentLine(), C.CurrentFunction, null));
			return null;
		}
		
		if (str == null || str.isEmpty()) {
			SetError(new ScriptError(ScriptErrorCode.Syntax, this, getCurrentLine(), C.CurrentFunction, "Missing expression."));
			return null;
		}

		Expression Expr = new Expression(this);
		Expr.RootNode = new ExpressionNode();
		Expr.RootNode.Parent = Expr;
		Expr.RootNode.Text = str;

		if (!Expr.RootNode.Split(C))
			return null;
		Expr.ResultType = Expr.RootNode.valueType;
		Expr.ResultClassID = Expr.RootNode.ClassID;
		return Expr;
	}

	/**
	* this function returns the id of the first unused temp variable in the current scope.
	* if none is found, it creates a new one.
	 * @throws Exception 
	**/
	private int GetTempVar(ScriptContext C, TypeID type)
	{
		int varID = -1;
		for (int k = C.VarStack.size() - 1; k > C.CurrentFunction.nBaseVarOffs; k--)
			if (C.VarStack.get(k).varType == TypeID.None)
			{
				// found an unused temp var
				varID = k - C.CurrentFunction.nBaseVarOffs;
				break;
			}
		if (varID == -1)
		{
			// no available temp vars found. create a new one:
			Variable v = C.CurrentInstrBlock().CreateVariable(TypeID.None);
			C.VarStack.add(v);
			varID = C.VarStack.size() - 1 - C.CurrentFunction.nBaseVarOffs;
			// give the variable a unique name:
			v.name = "temp_" + Long.toString(Double.doubleToRawLongBits(Math.abs(Math.cos(varID*15+12345)) * 1e10));
		}
		// add the temp var to our list:
		C.TempVars.add(C.VarByID(varID, null));
		// set the type of the temp variable to what we need now:
		C.VarByID(varID, null).varType = type;

		return varID;
	}

	Function GetFunctionByName(String name)
	{
		for (Function f : functions)
		{
			if (f.Name.equals(name))
				return f;
		}
		return null;
	}

	ArrayList<Expression> ReadCallParamList(ScriptContext C, TextStream text)
	{
		// expect '(' then read param list (expressions)
		if (text.Pick() != '(')
		{
			SetError(new ScriptError(ScriptErrorCode.LBracketExpected, C.ParentScript, C.ParentScript.getCurrentLine(), C.CurrentFunction, null));
			return null;
		}
		// read args, check types, create function call
		text.Advance(1);
		ArrayList<Expression> elist = new ArrayList<Expression>();
		Expression expr = null;
		while (text.Pick() != ')')
		{
			expr = ReadExpression(text, C);
			if (expr == null)
				return null;
			elist.add(expr);
			if (text.Pick() != ')' && text.Pick() != ',')
			{
				SetError(new ScriptError(ScriptErrorCode.IllegalChar, C.ParentScript, getCurrentLine(), C.CurrentFunction, text.PickString()));
				return null;
			}
			if (text.Pick() == ',')
			{
				text.Advance(1);
			}
		}
		text.Advance(1);
		return elist;
	}

	/**
	 * tries to find a matching function for the given parameter list.
	 * if no prototype is found, null is returned.
	 * @param paramList
	 * @param arrayList
	 * @return
	 */
	Function MatchFuncVersion(String funcName, ArrayList<Expression> elist, ArrayList<? extends Function> arrayList)
	{
		for (Function f : arrayList) 
		{
			if (elist.size() != f.Parameters.size())
				continue;
			if (!funcName.equals(f.Name))
				continue;
			
			for (int j = 0; j < elist.size(); j++)
			{
				if (TypeID.None == ExpressionNode.CheckTypes(
						f.Parameters.get(j).varType, f.Parameters.get(j).classID,
						elist.get(j).ResultType, elist.get(j).ResultClassID,
						ExpressionOperator.OpAttrib, this))
				
					continue;
			}
			for (int j=0; j<elist.size(); j++) 
			{
				if (f.Parameters.get(j).varType == TypeID.Float)
					elist.get(j).ResultType = TypeID.Float;
			}
			
			return f;
		}
		return null;
	}
	
	ScriptClass GetClass(String ClassID)
	{
		if (ClassID == null)
			return null;
		return extClasses.get(ClassID);
	}

	private ScriptError m_Error = null;
	void SetError(ScriptError err)
	{
		m_Error = err;
	}

	public Function MatchFuncVersion(String functionName, Object[] parameters) 
	{
		int nFunctions = 0;
		Function fSolo = null;
		for (Function f : functions) 
		{
			if (parameters.length != f.Parameters.size())
				continue;
			if (!functionName.equals(f.Name))
				continue;
			
			fSolo = f;
			nFunctions++;
			
			boolean match = true;
			
			for (int j = 0; j < parameters.length; j++)
			{
				TypeID paramType = TypeID.fromClass(parameters[j].getClass());
				String clsID = null;
				if (paramType == TypeID.Object || paramType == TypeID.ObjectArray)
					clsID = parameters[j].getClass().getSimpleName();
				if (TypeID.None == ExpressionNode.CheckTypes(
						f.Parameters.get(j).varType, f.Parameters.get(j).classID,
						paramType, clsID,
						ExpressionOperator.OpAttrib, this)) {
				
					match = false;
					break;
				}
			}
			
			if (match)
				return f;
		}
		
		if (nFunctions == 1) {
			try {
				for (int j=0; j < parameters.length; j++)
					parameters[j].getClass().asSubclass(fSolo.Parameters.get(j).varType.GetRuntimeInternalClass());
				return fSolo;
			} catch (ClassCastException e) {
				return null;
			}
		}
		
		return null;
	}
}
