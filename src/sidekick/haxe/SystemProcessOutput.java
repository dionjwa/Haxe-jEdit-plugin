package sidekick.haxe;

public class SystemProcessOutput
{
    public String output;
    public String errors;

    public SystemProcessOutput (String output, String errors)
    {
        super();
        this.output = output;
        this.errors = errors;
    }

    public SystemProcessOutput ()
    {
        super();
    }
}
