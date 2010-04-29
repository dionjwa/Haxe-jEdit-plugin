package sidekick.haxe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import sidekick.haxe.JavaSystemCaller.StreamGobbler;
import console.Console;
import console.ConsolePlugin;
import console.Shell;
import errorlist.ErrorSource;
import errorlist.DefaultErrorSource.DefaultError;

public class HaXeSideKickPlugin extends EditPlugin
{
    static HaXeErrorSource _errorSource = new HaXeErrorSource();
    public final static String NAME = "sidekick.haxe";
    public final static String OPTION_PREFIX = "options.sidekick.haxe.";

    public final static String PROPERTY_PREFIX = "plugin.sidekick.haxe.";

    public static void buildProject ()
    {
        List<String> output = getHaxeBuildOutput(null, 0, false);
        if (output != null && output.size() > 0) {
            handleBuildErrors(output.get(1), _errorSource, getProjectRoot());
        }
    }

    /**
     * Copies a stream to a file. If destination file exists, it will be overwritten. The input
     * stream may be closed when this method returns.
     *
     * @param from stream to copy from, will be closed after copy
     * @param to file to write
     * @exception Exception most likely an IOException
     */
    public static void copyToFile (InputStream from, File to)
    {
        try {
            FileOutputStream out = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int bytes_read;
            while (true) {
                bytes_read = from.read(buffer);
                if (bytes_read == -1) {
                    break;
                }
                out.write(buffer, 0, bytes_read);
            }
            out.flush();
            out.close();
            from.close();
        } catch (Exception e) {
            Log.log(Log.ERROR, NAME, Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public static List<String> executeShellCommand (final String command, String workingDirectory)
    {
        String output = "";
        String errors = "";

        try {
            final Runtime rt = Runtime.getRuntime();

            final Process proc = rt.exec(command, null, new File(workingDirectory));
            // any error message?
            final StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

            // any output?
            final StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            final int exitVal = proc.waitFor();
            System.out.println("ExitValue: " + exitVal);

            output = outputGobbler.getOutput();
            errors = errorGobbler.getOutput();

        } catch (final Throwable t) {
            t.printStackTrace();
        }

        List<String> out = new ArrayList<String>(2);
        out.add(output);
        out.add(errors);
        return out;
    }

    // Get the first *.hxml file we find
    public static File getBuildFile ()
    {
        String projectRootPath = getProjectRoot();
        if (projectRootPath == null) {
            return null;
        }
        // Get the first *.hxml file we find
        for (String filename : new File(projectRootPath).list()) {
            if (filename.endsWith(".hxml")) {
                return new File(projectRootPath, filename);
            }
        }

        return null;
    }

    public static List<String> getHaxeBuildOutput (EditPane editPane, int caret,
        boolean getCodeCompletion)
    {
        if (!isProjectSelected()) {
            Log.log(Log.WARNING, NAME, "buildProject but projectRootPath is null");
            return null;
        }

        if (editPane != null) {
            editPane.getBuffer().save(editPane.getView(), null, false, true);
        }

        Log.log(Log.NOTICE, NAME, "getCodeCompletionXML, caret=" + caret);
        String projectRootPath = getProjectRoot();

        Log.log(Log.DEBUG, NAME, "projectRootPath=" + projectRootPath);

        File hxmlFile = getBuildFile();

        Log.log(Log.DEBUG, NAME, "hxmlFileName=" + hxmlFile);

        if (hxmlFile == null) {
            Log.log(Log.ERROR, NAME, "buildProject, but no *.hxml at the project root.");
            return null;
        }

        String command = "haxe " + hxmlFile.getName();
        if (getCodeCompletion) {
            command += " --display src/Morphogen.hx@" + caret;
        }

        List<String> out = HaXeSideKickPlugin.executeShellCommand(command, projectRootPath);
        String output = out.get(0);
        String errorOutput = out.get(1);
        Log.log(Log.NOTICE, "getHaxeBuildOutput", "output=" + output + ", errors=" + errorOutput);
        return out;
    }

    public static String getProjectRoot ()
    {
        if (!isProjectSelected()) {
            return null;
        }
        VPTProject prj = ProjectViewer.getActiveProject(jEdit.getActiveView());
        return prj == null ? null : prj.getRootPath();
    }

    public static void handleBuildErrors (String errorOutput, HaXeErrorSource errorSource,
        String projectRootPath)
    {
        Log.log(Log.NOTICE, "handleBuildErrors", "errorOutput=" + errorOutput);

        if (errorSource != null) {
            errorSource.clear();

            String[] errorLines = errorOutput.split("\\n");
            for (String errorLine : errorLines) {
                if (errorLine.matches("((?:\\w:)?[^:]+?):(\\d+):\\s*(.+)")) {
                    trace("Error line match=" + errorLine);
                    String[] tokens = errorLine.split(":");
                    String fileName = projectRootPath + File.separatorChar + tokens[0];
                    trace("fileName=" + fileName);
                    int line = Integer.parseInt(tokens[1]) - 1;
                    String comment = tokens[3];
                    DefaultError error = new DefaultError(errorSource, ErrorSource.ERROR,
                        fileName, line, 0, 0, comment);
                    errorSource.addError(error);
                }
            }
        }
    }

    protected static boolean isErrors ()
    {
        for (ErrorSource src : ErrorSource.getErrorSources()) {
            if (src instanceof errorlist.DefaultErrorSource && src.getErrorCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isProjectSelected ()
    {
        if (jEdit.getPlugin("projectviewer.ProjectPlugin", false) == null) {
            Log.log(Log.ERROR, NAME, "projectviewer.ProjectPlugin not available");
            return false;
        }
        return ProjectViewer.getActiveProject(jEdit.getActiveView()) != null;
    }

    public static void launchProject ()
    {
// Log.log(Log.NOTICE, NAME, "launchProject");
// Log.log(Log.NOTICE, NAME, "building");
// buildProject();
// Log.log(Log.NOTICE, NAME, "finish building");
// Log.log(Log.NOTICE, NAME, "isErrors()=" + isErrors());
// SystemShell shell = ConsolePlugin.getSystemShell();
//
// DockableWindowManager wm = jEdit.getActiveView().getDockableWindowManager();
// Console console = (Console)wm.getDockable("console");
// console.setShell(shell);
//
// waitOnShell(console, shell);
//
// if (!isErrors()) {
// String launchCommand =
    // jEdit.getProperty("plugin.sidekick.haxe.HaXeSideKickPlugin.launchCommand");
//
// // Output output = console.getOutput();
// // Switch to the project root directory
// shell.execute(console, getProjectRoot(), NullConsoleOutput.NULL);
// Log.log(Log.NOTICE, NAME, "launch command=" + launchCommand);
// shell.execute(console, launchCommand, NullConsoleOutput.NULL);
// } else {
// Log.log(Log.ERROR, NAME, "Cannot launch project due to errors");
// }
    }

    public static void trace (String line)
    {
        Log.log(Log.DEBUG, NAME, line);
    }

    protected static void waitOnShell (Console console, Shell shell)
    {
        while (!shell.waitFor(console)) {
            Log.log(Log.NOTICE, NAME, "waiting for console");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Log.log(Log.ERROR, NAME, ie.getMessage());
            }
        }
    }

    private void copyBundledProperties ()
    {
        // Copy the haxe commando if it doesn't exist
        String commandoResource = "commando.xml";
        File outfile = new File(ConsolePlugin.getUserCommandDirectory(), "haxe.xml");
        if (!outfile.exists()) {
            copyToFile(getClass().getClassLoader().getResourceAsStream(commandoResource), outfile);
        }

        // If there's no haxe mode, add the mode from the jar and edit the mode catalog
        File haxeModeApp = new File(jEdit.getJEditHome() + File.separatorChar + "modes"
            + File.separatorChar + "haxe.xml");
        File haxeModeSettings = new File(jEdit.getSettingsDirectory() + File.separatorChar
            + "modes" + File.separatorChar + "haxe.xml");
        Log.log(Log.NOTICE, this, "haxeModeApp=" + haxeModeApp.exists());
        if (!haxeModeApp.exists()) {// No system haxe mode
            Log.log(Log.NOTICE, this, "haxeModeSettings=" + haxeModeSettings.exists());
            if (!haxeModeSettings.exists()) {

                new File(jEdit.getSettingsDirectory() + File.separatorChar + "modes").mkdirs();
                // No local haxe mode, so copy the "mode.xml" from the jar to "modes/haxe.xml"
                String modeResource = "mode.xml";
                Log.log(Log.NOTICE, this, "Copying mode.xml to modes/haxe.xml");
                copyToFile(getClass().getClassLoader().getResourceAsStream(modeResource),
                    haxeModeSettings);

                File catalogFile = new File(jEdit.getSettingsDirectory() + File.separatorChar
                    + "modes" + File.separatorChar + "catalog");
                StringBuffer contents = new StringBuffer();
                if (!catalogFile.exists()) {
                    // Write a catalog with just the haxe mode entry.
                    contents.append("<?xml version=\"1.0\"?>\n<!DOCTYPE MODES SYSTEM \"catalog.dtd\">\n<MODES>\n<MODE NAME=\"haxe\" FILE=\"haxe.xml\" FILE_NAME_GLOB=\"*.hx\" />\n</MODES>");
                } else {

                    // Now we have to edit the catalog
                    // On the line before the closing tag, insert the haxe entry
                    try {
                        BufferedReader input = new BufferedReader(new FileReader(catalogFile));
                        try {
                            String line = null; // not declared within while loop
                            while ((line = input.readLine()) != null) {
                                if (line.indexOf("</MODES>") > -1) {
                                    contents.append("<MODE NAME=\"haxe\" FILE=\"haxe.xml\" FILE_NAME_GLOB=\"*.hx\" />\n");
                                }
                                contents.append(line + "\n");
                            }
                        } finally {
                            input.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                // Now write the file

                Log.log(Log.ERROR, this, "TO write:" + contents.toString());
                try {
                    Writer output = new BufferedWriter(new FileWriter(catalogFile));
                    try {
                        output.write(contents.toString());
                    } finally {
                        output.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                jEdit.reloadModes();
            }
        }
    }

    public void start ()
    {
        copyBundledProperties();
    }

    public void stop ()
    {}

}
