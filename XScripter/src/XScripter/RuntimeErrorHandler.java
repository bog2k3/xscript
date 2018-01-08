/**
 * 
 */
package XScripter;

/**
 * @author bog
 * implement this interface to receive errors generated at script runtime.
 */
public interface RuntimeErrorHandler 
{
	void handleScriptRuntimeError(RuntimeError err);
}
