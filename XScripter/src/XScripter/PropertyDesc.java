package XScripter;

/**
* this struct describes a field of a class (for external validation)
**/
public class PropertyDesc
{
	/**
	* name of the property
	**/
	public String Name;
	/**
	* TypeID of the field
	**/
	public TypeID Type;
	/**
	* name of the class, if the TypeID is Object.
	**/
	public String ClassID;

	PropertyDesc(String Name, TypeID Type, String ClassID)
	{
		this.Name = Name;
		this.Type = Type;
		this.ClassID = ClassID;
	}
}
