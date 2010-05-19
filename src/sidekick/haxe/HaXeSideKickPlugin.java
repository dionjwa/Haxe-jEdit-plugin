package sidekick.haxe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import projectviewer.ProjectManager;
import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import sidekick.CodeCompletion;
import sidekick.CodeCompletionField;
import sidekick.CodeCompletionMethod;
import sidekick.GenericSideKickCompletion;
import sidekick.SideKickCompletion;
import sidekick.SideKickParser;
import sidekick.SideKickPlugin;
import sidekick.haxe.JavaSystemCaller.StreamGobbler;
import errorlist.ErrorSource;
import errorlist.DefaultErrorSource.DefaultError;

public class HaXeSideKickPlugin extends EditPlugin
{
    static HaXeErrorSource _errorSource = new HaXeErrorSource();
    public final static String NAME = "sidekick.haxe";
    public final static String OPTION_PREFIX = "options.sidekick.haxe.";
    public final static String PROPERTY_PREFIX = "plugin.sidekick.haxe.";

    public static HaxeCompilerOutput buildProject ()
    {
        return buildProject(jEdit.getActiveView().getEditPane());
    }

    public static HaxeCompilerOutput buildProject (EditPane editPane)
    {
        if (editPane != null && editPane.getBuffer().isDirty()) {
            editPane.getBuffer().save(editPane.getView(), null, false, true);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        HaxeCompilerOutput output = getHaxeBuildOutput(editPane, 0, false, true);
        if (output != null) {
            handleBuildErrors(output.output.errors, _errorSource, output.getProjectRoot());
        }
        return output;
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

    public static GenericSideKickCompletion getSideKickCompletion (EditPane editPane, int caret)
    {
        // If the caret is at a ".", use the Haxe compiler to provide completion hints
        // Save the file if dirty
        if (editPane.getBuffer().isDirty()) {
            editPane.getBuffer().save(editPane.getView(), null, false, true);
            // Wait a bit to allow the save notifications to go through and not
            // bork the reload/popup
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        HaxeCompilerOutput output = HaXeSideKickPlugin.getHaxeBuildOutput(editPane, caret, true);

//        trace("getSideKickCompletion=" + output);

        String completionXMLString = output.output.errors.trim();

        if (completionXMLString == null || completionXMLString.equals("")
            || !completionXMLString.startsWith("<")) {
            return null;
        }

//        trace("completionXMLString=" + completionXMLString);

        List<CodeCompletion> codeCompletions = new ArrayList<CodeCompletion>();

        try {
            // Example see http://www.rgagnon.com/javadetails/java-0573.html
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(completionXMLString));

            Document doc = db.parse(is);
            NodeList insertions = doc.getElementsByTagName("i");

            // iterate the insertions
            for (int i = 0; i < insertions.getLength(); i++) {
                Element element = (Element)insertions.item(i);
                if (element.getNodeName().equals("i")) {
                    // Insertion
                    String codeName = element.getAttribute("n");
                    //HaXeSideKickPlugin.trace(codeName);
                    String argString = ((Element)element.getElementsByTagName("t").item(0)).getTextContent();
                    //HaXeSideKickPlugin.trace(codeName + "=" + argString);
                    String[] methodTokens = argString.split("->");
                    String returns = methodTokens[methodTokens.length - 1];
                    if (methodTokens.length == 1) {
                        CodeCompletionField cc = new CodeCompletionField();
                        cc.name = codeName;
                        cc.setClassName(returns);
                        codeCompletions.add(cc);
                    } else {
                        CodeCompletionMethod cc = new CodeCompletionMethod();
                        cc.name = codeName;
                        cc.returnType = returns;
                        if (methodTokens.length > 1 && !methodTokens[0].trim().equals("Void")) {
                            List<String> args = new ArrayList<String>(methodTokens.length - 1);
                            List<String> argsTypes = new ArrayList<String>(
                                methodTokens.length - 1);
                            for (int jj = 0; jj < methodTokens.length - 1; ++jj) {
                                String[] argTokens = methodTokens[jj].split(":");
                                args.add(argTokens[0]);
                                if (argTokens.length > 1) {
                                    argsTypes.add(argTokens[1]);
                                }
                            }
                            cc.arguments = args;
                            cc.argumentTypes = argsTypes;
                        }
                        codeCompletions.add(cc);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        GenericSideKickCompletion completion = new GenericSideKickCompletion(editPane.getView(),
            "", codeCompletions);
        return completion;
    }

    public static void haxeCodeComplete (View view)
    {
        EditPane editPane = view.getEditPane();
        Buffer buffer = editPane.getBuffer();
        JEditTextArea textArea = editPane.getTextArea();

        SideKickParser parser = SideKickPlugin
            .getParserForBuffer(buffer);

        if (!buffer.getText(textArea.getCaretPosition() - 1, 1).equals(".")) {
//            trace("haxeCodeComplete, not at char '.'");
            return;
        }

        if (parser == null) {
//            trace("haxeCodeComplete, no parser");
            return;
        }

        SideKickCompletion complete = getSideKickCompletion(editPane, textArea.getCaretPosition());

        if(complete == null || complete.size() == 0)
        {
//            trace("SideKickCompletion==null");
        }
        else if(complete.size() == 1)
        {
            // if user invokes complete explicitly, insert the
            // completion immediately.
            //
            // if the user eg enters </ in XML mode, there will
            // only be one completion and / is an instant complete
            // key, so we insert it
                complete.insert(0);
                return;
        }

        // show the popup if
        // - complete has one element and user invoked with delay key
        // - or complete has multiple elements
        // and popup is not already shown because of explicit invocation
        // of the complete action during the trigger delay
        parser.getCompletionPopup(view, textArea.getCaretPosition(), complete, true);
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

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

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
    public static File getBuildFile (EditPane editPane)
    {
        File buildFile;
        // If there's a project selected, try looking there first
        if (jEdit.getPlugin("projectviewer.ProjectPlugin", false) != null) {
            String projectRoot = getProjectRoot();
            if (projectRoot != null) {
                buildFile = getFirstBuildFileInDir(projectRoot);
                if (buildFile != null) {
                    return buildFile;
                }
            }
            //trace("getBuildFile(), no *.hxml in current project");

            if (editPane != null) {
                // Try the project of the current buffer, even if it's a different project to the
                // current selected project
                ProjectManager pm = ProjectManager.getInstance();
                String path = editPane.getBuffer().getPath();
                for (VPTProject prj : pm.getProjects()) {
                    if (prj.isInProject(path)) {
                        buildFile = getFirstBuildFileInDir(prj.getRootPath());
                        if (buildFile != null) {
                            return buildFile;
                        }
                    }
                }
            }
            //trace("getBuildFile(), no *.hxml in project of buffer");
        }

        // Otherwise, search up the file system tree, and grab the first *.hxml we find
        File curDir = editPane == null ? null
            : new File(editPane.getBuffer().getPath()).getParentFile();
        while (curDir != null) {
            buildFile = getFirstBuildFileInDir(curDir.getAbsolutePath());
            if (buildFile != null) {
                return buildFile;
            }
            //trace("getBuildFile(), no *.hxml in " + curDir.getAbsolutePath());
            curDir = curDir.getParentFile();
        }

        return null;
    }

    protected static File getFirstBuildFileInDir (String path)
    {
        // Get the first *.hxml file we find
        for (String filename : new File(path).list()) {
            if (filename.toLowerCase().endsWith(".hxml")) {
                return new File(path, filename);
            }
        }
        return null;
    }

// private static boolean isWindows(){
//
// String os = System.getProperty("os.name").toLowerCase();
// //windows
// return (os.indexOf( "win" ) >= 0);
//
// }
//
// private static boolean isMac(){
//
// String os = System.getProperty("os.name").toLowerCase();
// //Mac
// return (os.indexOf( "mac" ) >= 0);
//
// }
//
// private static boolean isUnix(){
//
// String os = System.getProperty("os.name").toLowerCase();
// //linux or unix
// return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
//
// }

    public static HaxeCompilerOutput getHaxeBuildOutput (EditPane editPane, int caret,
        boolean getCodeCompletion)
    {
        return getHaxeBuildOutput(editPane, caret, getCodeCompletion, false);
    }

    public static HaxeCompilerOutput getHaxeBuildOutput (EditPane editPane, int caret,
        boolean getCodeCompletion, boolean showErrorPopups)
    {
// if (!isProjectSelected() && editPane == null) {
// Log.log(Log.WARNING, NAME, "buildProject but projectRootPath is null && editPane == null");
// return null;
// }

        if (getCodeCompletion && editPane == null) {
            Log.log(Log.ERROR, NAME, "getHaxeBuildOutput, getCodeCompletion=" + getCodeCompletion
                + ", editPane == null");
            return null;
        }

//        Log.log(Log.NOTICE, NAME, "getHaxeBuildOutput, caret=" + caret);
// String projectRootPath = getProjectRoot(editPane);

// Log.log(Log.DEBUG, NAME, "projectRootPath=" + projectRootPath);

// if (projectRootPath == null) {
// Log.log(Log.ERROR, NAME, "getProjectRoot(editPane) returns null");
// return null;
// }
        File hxmlFile = getBuildFile(editPane);

        if (hxmlFile == null) {
            Log.log(
                Log.ERROR,
                NAME,
                "Attempting to build haxe project, but no *.hxml at the project root, or in a parent directory of the current buffer.");
            if (showErrorPopups) {
                GUIUtilities.error(jEdit.getActiveView(), "haxe.error.noBuildFile", null);
            }
            return null;
        }

        String projectRootPath = hxmlFile.getParentFile().getAbsolutePath();

//        Log.log(Log.DEBUG, NAME, "hxmlFileName=" + hxmlFile);

// if (hxmlFile == null) {
// Log.log(Log.ERROR, NAME, "buildProject, but no *.hxml at the project root.");
// return null;
// }

// String compiler = jEdit.getProperty("options.haxe.compilerLocationMac");

// if(isWindows()){
// compiler = jEdit.getProperty("options.haxe.compilerLocationWindows");
// } else if(isUnix()){
// compiler = jEdit.getProperty("options.haxe.compilerLocationLinux");
// }

        String command = "haxe " + hxmlFile.getName();
        if (getCodeCompletion) {
            String path = editPane.getBuffer().getPath();
            path = path.substring(projectRootPath.length());
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }
            command += " --display " + path + "@" + caret;
        }
//        Log.log(Log.NOTICE, NAME, "command=" + command);
//        Log.log(Log.NOTICE, NAME, "projectRootPath=" + projectRootPath);

        SystemProcessOutput output = JavaSystemCaller.systemCall(command, projectRootPath);
//        trace("waiting");
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
// List<String> out = HaXeSideKickPlugin.executeShellCommand(command, projectRootPath);
// String output = out.get(0);
// String errorOutput = out.get(1);
//        Log.log(Log.MESSAGE, "command=" + command + "\ngetHaxeBuildOutput", "output="
//            + output.output + ", errors=" + output.errors);
        return new HaxeCompilerOutput(hxmlFile, output);
    }

    public static String getProjectRoot ()
    {
        if (!isProjectSelected()) {
            return null;
        }
        VPTProject prj = ProjectViewer.getActiveProject(jEdit.getActiveView());
        return prj == null ? null : prj.getRootPath();
    }

    public static String getProjectRoot (EditPane editPane)
    {
        if (editPane == null) {
            return getProjectRoot();
        }
        ProjectManager pm = ProjectManager.getInstance();
        String path = editPane.getBuffer().getPath();
        for (VPTProject prj : pm.getProjects()) {
            if (prj.isInProject(path)) {
                return prj.getRootPath();
            }
        }
        return null;
    }

    public static void handleBuildErrors (String errorOutput, HaXeErrorSource errorSource,
        String projectRootPath)
    {
        if (errorSource != null) {
            errorSource.clear();

            String[] errorLines = errorOutput.split("\\n");
            for (String errorLine : errorLines) {
                if (errorLine.matches("((?:\\w:)?[^:]+?):(\\d+):\\s*(.+)")) {
                    String[] tokens = errorLine.split(":");
                    String fileName = projectRootPath + File.separatorChar + tokens[0];
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
// String projectRoot = getProjectRoot();
// if (projectRoot == null) {
// Log.log(Log.ERROR, NAME, "No project project root.");
// return;
// }

        HaxeCompilerOutput output = buildProject();
        if (output != null && !isErrors()) {
            String launchCommand = jEdit.getProperty("options.haxe.launchCommand");
            executeShellCommand(launchCommand, output.buildFile.getParentFile().getAbsolutePath());
        } else {
            String msg = "Cannot launch project due to errors or failed build";
            Log.log(Log.MESSAGE, NAME, msg);
            jEdit.getFirstView().getStatus().setMessage(msg);
        }
    }

    public static void trace (Object... arguments)
    {
        StringBuffer sb = new StringBuffer();
        for (Object s : arguments) {
            sb.append(s.toString() + " ");
        }
        Log.log(Log.NOTICE, "HaXe", sb.toString());
    }

// protected static void waitOnShell (Console console, Shell shell)
// {
// while (!shell.waitFor(console)) {
// Log.log(Log.NOTICE, NAME, "waiting for console");
// try {
// Thread.sleep(100);
// } catch (InterruptedException ie) {
// Log.log(Log.ERROR, NAME, ie.getMessage());
// }
// }
// }

    @Override
    public void start ()
    {}

    @Override
    public void stop ()
    {}

}
