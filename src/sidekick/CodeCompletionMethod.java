package sidekick;


import java.util.LinkedList;
import java.util.List;

/**
 * Container describing a method from a
 * @author dion
 *
 */
public class CodeCompletionMethod extends CodeCompletionVariable
{

	@Override
	public String toString()
	{
	    return getCodeCompletionString();
//		StringBuilder s = new StringBuilder(name + "(");
//		for (int i = 0; i < arguments.size(); i++)
//		{
//			if(i>0)
//				s.append(", ");
//			s.append(arguments.get(i));
//			if(argumentTypes.get(i).length() > 0)
//				s.append(":"+argumentTypes.get(i));
//		}
//		s.append("): " + returnType);
//		return s.toString();
	}
	public List<String> arguments;
	public List<String> argumentTypes;
	public String returnType;
	public CodeCompletionMethod()
	{
		arguments = new LinkedList<String>();
		argumentTypes = new LinkedList<String>();
		name = "";
		returnType = "";
		type = CodeCompletionType.METHOD;
	}


	public String getName()
	{
		return name;
	}

	@Override
	public String getCodeCompletionString()
	{
		StringBuilder s = new StringBuilder(name + "(");
		for (int i = 0; i < arguments.size(); i++)
		{
			s.append((i==0?"":",") +arguments.get(i));
			if(i < argumentTypes.size() && argumentTypes.get(i).length() > 0) {
				s.append(":"+argumentTypes.get(i));
			}
		}
		s.append("): " + returnType);
		return s.toString();
	}

	@Override
	public String getStringForInsertion()
	{
		StringBuilder s = new StringBuilder(name+"(");
		for (int i = 0; i < arguments.size(); i++)
		{
			s.append((i==0?"":",") + arguments.get(i));
		}
		s.append(")");
		return s.toString();
	}
}
