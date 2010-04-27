package sidekick;


public interface CodeCompletion extends Comparable<CodeCompletion>
{
	public String getCodeCompletionString();

	public String getStringForInsertion();

	public CodeCompletionType getType();
}
