package sidekick.haxe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.util.Log;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import console.Console;
import console.ConsolePlugin;
import console.Output;
import console.Shell;
import console.SystemShell;
import errorlist.ErrorList;
import errorlist.ErrorSource;

public class HaXeSideKickPlugin extends EditPlugin
{
    public final static String NAME = "sidekick.haxe";
    public final static String OPTION_PREFIX = "options.sidekick.haxe.";
    public final static String PROPERTY_PREFIX = "plugin.sidekick.haxe.";

    public void start ()
    {
        copyBundledProperties();
// loadProperties();
        registerServices();
    }

    public void stop ()
    {
// ErrorSource.unregisterErrorSource(_errorSource);
    }

//    public void handleMessage (EBMessage message)
//    {
//        // TODO Auto-generated method stub
//
//    }

    private void copyBundledProperties ()
    {
        // Copy the haxe commando
        String commandoResource = "commando.xml";
        File outfile = new File(ConsolePlugin.getUserCommandDirectory(), "haxe.xml");
        Log.log(Log.WARNING, this, "outfile=" + outfile.toString());

        if (!outfile.exists()) {
            copyToFile(getClass().getClassLoader().getResourceAsStream(commandoResource), outfile);
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

    public static void launchProject ()
    {
        Log.log(Log.NOTICE, NAME, "launchProject");
        Log.log(Log.NOTICE, NAME, "building");
        buildProject();
        Log.log(Log.NOTICE, NAME, "finish building");
        Log.log(Log.NOTICE, NAME, "isErrors()=" + isErrors());
        SystemShell shell = ConsolePlugin.getSystemShell();

        DockableWindowManager wm = jEdit.getActiveView().getDockableWindowManager();
        Console console = (Console)wm.getDockable("console");
        console.setShell(shell);

        waitOnShell(console, shell);

        if (!isErrors()) {
            String launchCommand = jEdit.getProperty("plugin.sidekick.haxe.HaXeSideKickPlugin.launchCommand");

            Output output = console.getOutput();
            // Switch to the project root directory
            shell.execute(console, getProjectRoot(), output);
            Log.log(Log.NOTICE, NAME, "launch command=" + launchCommand);
            shell.execute(console, launchCommand, output);
        } else {
            Log.log(Log.ERROR, NAME, "Cannot launch project due to errors");
        }
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

    // Get the first *.hxml file we find
    public static File getBuildFile ()
    {
        String projectRootPath = getProjectRoot();
        // Get the first *.hxml file we find
        for (String filename : new File(projectRootPath).list()) {
            if (filename.endsWith(".hxml")) {
                return new File(projectRootPath, filename);
            }
        }

        return null;
    }

    public static String getProjectRoot ()
    {
        if (jEdit.getPlugin("projectviewer.ProjectPlugin", false) == null) {
            Log.log(Log.ERROR, NAME, "projectviewer.ProjectPlugin not available");
            return null;
        }
        VPTProject prj = ProjectViewer.getActiveProject(jEdit.getActiveView());
        return prj.getRootPath();
    }

    public static void buildProject ()
    {
        String projectRootPath = getProjectRoot();
        Log.log(Log.DEBUG, NAME, "projectRootPath=" + projectRootPath);

        File hxmlFile = getBuildFile();

        Log.log(Log.DEBUG, NAME, "hxmlFileName=" + hxmlFile);

        if (hxmlFile == null) {
            Log.log(Log.ERROR, NAME, "buildProject, but no *.hxml at the project root.");
            return;
        }

        View view = jEdit.getActiveView();
        DockableWindowManager wm = view.getDockableWindowManager();
        SystemShell shell = ConsolePlugin.getSystemShell();
        wm.addDockableWindow("console");
        Console console = (Console)wm.getDockable("console");
        console.setShell(shell);
        Output output = console.getOutput();
        // Switch to the project root directory
        shell.execute(console, projectRootPath, NullConsoleOutput.NULL);
        shell.execute(console, "haxe " + hxmlFile.getName(), output);
        ((ErrorList)wm.getDockableWindow("error-list")).nextError();
        view.getTextArea().requestFocus();
    }

    public static String getCodeCompletionXML (EditPane editPane, int caret)
    {
        Log.log(Log.NOTICE, NAME, "getCodeCompletionXML, caret=" + caret);
        String projectRootPath = getProjectRoot();
        Log.log(Log.DEBUG, NAME, "projectRootPath=" + projectRootPath);

        File hxmlFile = getBuildFile();

        Log.log(Log.DEBUG, NAME, "hxmlFileName=" + hxmlFile);

        if (hxmlFile == null) {
            Log.log(Log.ERROR, NAME, "buildProject, but no *.hxml at the project root.");
            return null;
        }

        String command = "haxe " + hxmlFile.getName() + " --display src/Morphogen.hx@" + caret;
        Log.log(Log.NOTICE, NAME, "call=" + command);
//        String output = JavaSystemCaller.systemCall(call, getProjectRoot());
//        Log.log(Log.NOTICE, NAME, "output=" + output);

        View view = jEdit.getActiveView();
        DockableWindowManager wm = view.getDockableWindowManager();
        Console console = (Console)wm.getDockable("console");
        SystemShell shell = ConsolePlugin.getSystemShell();
        ConsoleStringOutput output = new ConsoleStringOutput();
//        console.run(shell, output, command);
        shell.execute(console, projectRootPath, NullConsoleOutput.NULL);
        shell.execute(console, command, output);

        waitOnShell(console, shell);

        Log.log(Log.NOTICE, NAME, "output=" + output.getStringOutput());
        return output.getStringOutput();

//        boolean isConsoleShowing = wm.isDockableWindowVisible("console");
//        wm.addDockableWindow("console");
//        console.setShell(shell);
//        // Switch to the project root directory
//        shell.execute(console, projectRootPath, output);
//        shell.execute(console, "haxe " + hxmlFile.getName(), output);
//        if (!isConsoleShowing) {
//            wm.hideDockableWindow("console");
//        }

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

    public static void registerServices ()
    {}
}
