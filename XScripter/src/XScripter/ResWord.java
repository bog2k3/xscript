package XScripter;

class ResWord 
{
	// Type identifiers
	
	/**
	 defines a void return type for a function
	**/
	public static final String RWT_VOID = "void";
	public static final String RWT_CHAR = "char";
	public static final String RWT_BOOL = "bool";
	public static final String RWT_INT = "int";
	public static final String RWT_FLOAT = "float";
	public static final String RWT_STRING = "string";
	/**
	/// defines a reference to an external object
	**/
	public static final String RWT_OBJECT = "object";

	// Control structures
	
	public static final String RWC_IF = "if";
	public static final String RWC_ELSE = "else";
	public static final String RWC_WHILE = "while";
	public static final String RWC_DO = "do";
	public static final String RWC_UNTIL = "until";
	public static final String RWC_FOR = "for";

	// Boolean immediate values:
	public static final String RWB_TRUE = "true";
	public static final String RWB_FALSE = "false";
	
	// standard keywords
	public static final String RWK_NULL = "null";
	public static final String RWK_READONLY = "readonly";
	public static final String RWK_STATIC = "static";

	// Flow control
	public static final String RWF_RETURN = "return";
	public static final String RWF_BREAK = "break";
	public static final String RWF_CONTINUE = "continue";
	public static final String RWF_DEBUG = "debug";
	
	// interfacing
	public static final String RWI_EXTERNAL = "external";
	public static final String RWI_CLASS = "class";
	public static final String RWI_ABSTRACT = "abstract";
	public static final String RWI_NEW = "new";
	public static final String RWI_EVENT = "event";

	public static String[] WordList = 
	{
		RWT_FLOAT, RWT_INT, RWT_OBJECT, RWT_STRING, RWT_VOID, RWT_BOOL,
		RWC_IF, RWC_ELSE, RWC_FOR, RWC_WHILE, RWC_DO, RWC_UNTIL, 
		RWF_RETURN, RWF_BREAK, RWF_CONTINUE,
		RWB_FALSE, RWB_TRUE, RWI_CLASS, RWI_EXTERNAL, RWI_NEW, RWI_ABSTRACT, RWI_EVENT,
		RWK_NULL, RWK_READONLY, RWK_STATIC, RWF_DEBUG
	};
}
