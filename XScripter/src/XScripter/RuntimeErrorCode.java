package XScripter;

public enum RuntimeErrorCode 
{
	Unknown,
	
	ExtGlobalNotFound,
	
	MethodInvokeFailed,
	PropertyAccessFailed,
	InvalidObjectClass,
	InvalidExpressionNodeResultType,
	InvalidExpressionNodeOperator,
	InvalidArraySize,
	ArrayIndexOutOfBounds,
	
	ExecutorNotRunning,
	ExecutorBound,
	FunctionVersionNotFound,
	ArgumentCountMismatch,
	ArgumentTypeMismatch, 
	FeatureNotImplemented,
	ClassNotRegistered, 
	ConstructorNotFound, 
	ExceptionThrown,
	NullAccess, 
	DebugRequested,
	
}
