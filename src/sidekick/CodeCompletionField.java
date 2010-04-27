package sidekick;



public class CodeCompletionField extends CodeCompletionVariable
{
	public CodeCompletionField()
	{
		super();
		type = CodeCompletionType.FIELD;
	}
	@Override
	public String toString()
	{
		return name + " : " + getClassName();
	}


}
