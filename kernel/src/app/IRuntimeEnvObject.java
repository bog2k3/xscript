package app;

/**
 * The IRuntimeEnvObject represent an object belonging to the execution environment
 * of an x-application (such as a GUI or other resource) that is strictly linked
 * to its owner x-application, being created by the application. These objects must
 * always be destroyed when the application exits, either willingly or forcefully; in either
 * case, the environment must be cleared of these objects.
 * @author Bogdan.Ionita
 *
 */
public interface IRuntimeEnvObject 
{
	public enum RTEnvObj_LifeState 
	{
		Initializing,
		Available,
		Destroying,
		Dead
	}
	
	public void destroy();
	
	public RTEnvObj_LifeState getLifeState();
}
