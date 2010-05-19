package sidekick.haxe;

import java.io.File;

public class HaxeCompilerOutput
{
    public File buildFile;
    public SystemProcessOutput output;
    public HaxeCompilerOutput (File buildFile, SystemProcessOutput output)
    {
        super();
        this.buildFile = buildFile;
        this.output = output;
    }

    public String getProjectRoot ()
    {
        return buildFile == null ? null : buildFile.getParentFile().getAbsolutePath();
    }

    @Override
    public String toString ()
    {
        return "HaxeCompilerOutput, buildFile=" + buildFile.getAbsolutePath() +
            "\n   output" + output.output +
            "\n   errors" + output.errors;

    }
}
