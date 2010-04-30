package sidekick;


import java.util.LinkedList;
import java.util.List;

/**
 * Tokens suggested for code completion that are loca
 * variables or similar.
 * @author dion
 *
 */
public class CodeCompletionVariable implements CodeCompletion
{

	public String name;

	/**
	 * List of the class followed by the super classes.
	 */
	private List<String> superClasses;

	private String className;

	public CodeCompletionType type;

	public List<Integer> locations;

	public CodeCompletionVariable()
	{
		name = "";
		locations = new LinkedList<Integer>();
		className = "";
		superClasses = new LinkedList<String>();
		type = CodeCompletionType.VARIABLE;
	}

	public CodeCompletionVariable(String name)
	{
		super();
		this.name = name;
	}

	public int compareTo(CodeCompletion o)
	{
		return getCodeCompletionString().compareTo(o.getCodeCompletionString());
	}
	public String getCodeCompletionString()
	{
		if(type == CodeCompletionType.CLASS)
			return name;
		else
			return name + " : " + className;
	}

	public String getStringForInsertion()
	{
		return name;
	}

	public CodeCompletionType getType()
	{
		return type;
	}

	public String toString()
	{
		return name + "(" + superClasses + ") " + locations;
	}

	public List<String> getSuperClasses()
	{
		return superClasses;
	}

	public void setSuperClasses(List<String> superClasses)
	{
		this.superClasses = superClasses;
	}

	public String getClassName()
	{
		return className;
	}

	public void setClassName(String className)
	{
		this.className = className;
	}
}
