package sidekick.haxe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import sidekick.haxe.JavaSystemCaller.StreamGobbler;

import errorlist.ErrorSource;
import errorlist.DefaultErrorSource.DefaultError;

public class HaXeSideKickPlugin extends EditPlugin
{
    static HaXeErrorSource _errorSource = new HaXeErrorSource();

    public static LastEditLocation lastEditLocation;
    public final static String NAME = "sidekick.haxe";
    public final static String OPTION_PREFIX = "options.haxe.";
    public final static String PROPERTY_PREFIX = "plugin.sidekick.haxe.";

    public static HaxeCompilerOutput buildProject ()
    {
        EditPane editPane = jEdit.getActiveView().getEditPane();
    	if (editPane == null) {
            JOptionPane.showMessageDialog(null, "EditPane is null", "Error", JOptionPane.ERROR_MESSAGE);
    		return null;
    	}
        if (editPane.getBuffer().isDirty()) {
            editPane.getBuffer().save(editPane.getView(), null, false, true);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        checkAndUpdateProjectHaxeBuildFile(getCurrentProject());

        HaxeCompilerOutput output = getHaxeBuildOutput(editPane, 0, false, true);
        checkCompilerOutputForErrors(output);
        return output;
    }

    public static void checkCompilerOutputForErrors (HaxeCompilerOutput output)
    {
        if (output != null) {
            handleBuildErrors(output.output.errors, _errorSource, output.getProjectRoot(), getBuildFile());
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

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // any error???
            final int exitVal = proc.waitFor();

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


    public static File getFirstProjectHXMLFile (VPTProject prj)
    {
        if (prj == null) {
            return null;
        }
        // If there's a project selected, try looking there first
        String projectRoot = prj.getRootPath();
        if (projectRoot != null && projectRoot.length() > 0) {
            trace("projectRoot=" + projectRoot);
            return getFirstBuildFileInDir(projectRoot);
        }
        return null;
    }

    // Get the project defined hxml file.
    public static File getBuildFile ()
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        checkAndUpdateProjectHaxeBuildFile(prj);
        File buildFile = new File(prj.getRootPath() + File.separator + prj.getProperty(ProjectOptionPane.PROJECT_HXML_FILE));
        if (buildFile.exists()) {
            return buildFile;
        }

        JOptionPane.showMessageDialog(null, "No .hxml file found in the project root folder", "Error", JOptionPane.ERROR_MESSAGE);
        return null;
    }

    protected static File getFirstBuildFileInDir (String path)
    {
        if (!new File(path).exists()) {
            return null;
        }
        // Get the first *.hxml file we find
        for (String filename : new File(path).list()) {
            if (filename.toLowerCase().endsWith(".hxml")) {
                return new File(path, filename);
            }
        }
        return null;
    }

    public static HaxeCompilerOutput getHaxeBuildOutput (EditPane editPane, int caret,
        boolean getCodeCompletion)
    {
        return getHaxeBuildOutput(editPane, caret, getCodeCompletion, false);
    }

    public static HaxeCompilerOutput getHaxeBuildOutput (EditPane editPane, int caret,
        boolean getCodeCompletion, boolean showErrorPopups)
    {
    	if (editPane == null) {
    		Log.log(Log.ERROR, NAME, "getHaxeBuildOutput, editPane=" + editPane);
    		return null;
    	}

    	VPTProject prj = getCurrentProject();

    	if (prj == null) {
    	    Log.log(
                Log.ERROR,
                NAME,
                "Attempting to build haxe project, but no project currently selected.");
    	    return null;
    	}

        File hxmlFile = getBuildFile();

        if (hxmlFile == null) {
            Log.log(
                Log.ERROR,
                NAME,
                "Attempting to build haxe project, but no *.hxml at the project root.");
            if (showErrorPopups) {
                GUIUtilities.error(jEdit.getActiveView(), "haxe.error.noBuildFile", null);
            }
            return null;
        }

        String projectRootPath = prj.getNodePath();

        String command = "haxe ";

        String haxeExecutableProp = prj.getProperty(ProjectOptionPane.PROJECT_HAXE_EXECUTABLE);
        if (haxeExecutableProp != null && haxeExecutableProp.length() > 0) {
            command = haxeExecutableProp + " ";
        }

        String hxmlfile = hxmlFile.getAbsolutePath().replace(projectRootPath, "");
        if (hxmlfile.startsWith(File.separator)) {
            hxmlfile = hxmlfile.substring(1);
        }
        command += hxmlfile;

        if (getCodeCompletion) {
            String path = editPane.getBuffer().getPath();
            command += " --display " + path + "@" + caret;
        }
        trace("  command=" + command);

        //Figure out if we are using custom std lib location
        String stdLibDirProp = prj.getProperty(ProjectOptionPane.PROJECT_STD_DIR);
        ArrayList<String> env = new ArrayList<String>();
        env.add("HOME=" + System.getProperty("user.home"));
        if (stdLibDirProp != null && stdLibDirProp.length() > 0) {
            env.add("HAXE_LIBRARY_PATH=" + stdLibDirProp);
        }
        String[] envp = env.toArray(new String[env.size()]);

        SystemProcessOutput output = JavaSystemCaller.systemCall(command, projectRootPath, null, envp);
        return new HaxeCompilerOutput(hxmlFile, output);
    }

    public static VPTProject getCurrentProject ()
    {
        if (!isProjectSelected()) {
            return null;
        }
        return ProjectViewer.getActiveProject(jEdit.getActiveView());
    }

    public static void handleBuildErrors (String errorOutput, HaXeErrorSource errorSource,
        String projectRootPath, File buildFile)
    {
        if (errorOutput == null) {
            trace("  errorOutput==null");
            return;
        }
        if (errorSource != null) {
            errorSource.clear();

            String[] errorLines = errorOutput.split(System.getProperty("line.separator"));
            Matcher m;
            for (String errorLine : errorLines) {
            	if (errorLine == null || errorLine.trim().length() == 0) {
            		continue;
            	}
                m = patternError.matcher(errorLine);
                if (m.matches()) {
                    String path = m.group(1);
                    if (!new File(path).exists()) {
                        path = projectRootPath + File.separatorChar + m.group(1);
                    }
                    DefaultError error = new DefaultError(errorSource, ErrorSource.ERROR,
                        path, Integer.parseInt(m.group(2)) - 1, 0, 0, m.group(3));
                    errorSource.addError(error);
                }
                else {
                    trace("no error pattern match: " + errorLine);
                    errorSource.addError(new DefaultError(errorSource, ErrorSource.ERROR,
                    		buildFile.getAbsolutePath().replace(System.getProperty("user.dir"), ""), 0, 0, 0, errorLine));
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
        HaxeCompilerOutput output = buildProject();
        if (output != null && !isErrors()) {
            String launchCommand = getLaunchCommand();
            if (launchCommand != null && getCurrentProject() != null) {
                executeShellCommand(launchCommand, getCurrentProject().getRootPath());
            } else {
                trace("Null launch command");
            }
        }
    }

    protected static String getLaunchCommand ()
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        return prj.getProperty(ProjectOptionPane.PROJECT_LAUNCH_CMD);
    }

    public static void trace (Object... arguments)
    {
        StringBuffer sb = new StringBuffer();
        for (Object s : arguments) {
            sb.append(s.toString() + " ");
        }
        Log.log(Log.MESSAGE, "HaXe", sb.toString());
    }

    protected static String getSystemDefaultHaxeInstallPath ()
    {
        String os = System.getProperty("os.name").toLowerCase();

        if(os.equalsIgnoreCase("Windows 7")) {
            return jEdit.getProperty(OPTION_PREFIX + "defaultInstallDirWindows7");
        } else if(os.indexOf("win") >= 0) {
            return jEdit.getProperty(OPTION_PREFIX + "defaultInstallDirWindows");
        } else if (os.indexOf("mac") >= 0) {
            return jEdit.getProperty(OPTION_PREFIX + "defaultInstallDirMac");
        } else {
            return jEdit.getProperty(OPTION_PREFIX + "defaultInstallDirLinux");
        }
    }



    public static void goToLastEditLocation (final View view)
    {
        lastEditLocation.goToLastEditLocation();
    }

    @Override
    public void start ()
    {
        if (lastEditLocation == null) {
            lastEditLocation = new LastEditLocation();
        }
    }

    @Override
    public void stop ()
    {}

    protected static void checkAndUpdateProjectHaxeBuildFile (VPTProject prj)
    {
        if (prj == null) {
            return;
        }

        String buildFileProp = prj.getProperty(ProjectOptionPane.PROJECT_HXML_FILE);
        if (buildFileProp != null && buildFileProp.toLowerCase().endsWith(".hxml")) {
            File buildFile = new File(prj.getRootPath() + File.separator + buildFileProp);
            if (buildFile.exists()) {
                return;
            }
        }

        File buildFile = getFirstProjectHXMLFile(prj);
        if (buildFile != null && buildFile.exists()) {
            prj.setProperty(ProjectOptionPane.PROJECT_HXML_FILE, buildFile.getAbsolutePath().substring(prj.getRootPath().length() + 1));
        } else {
            prj.removeProperty(ProjectOptionPane.PROJECT_HXML_FILE);
        }
    }

    protected static String getHaxelibPath ()
    {
        String[] envp = {"HOME=" + System.getProperty("user.home")};
        SystemProcessOutput output = JavaSystemCaller.systemCall("haxelib config", System.getProperty("user.home"), null, envp);
        String path = output.output.trim();
        if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    protected static String getStdLibPath ()
    {
        VPTProject prj = getCurrentProject();
        if (prj != null) {
            String libraryPathProp = prj.getProperty(ProjectOptionPane.PROJECT_STD_DIR);
            if (libraryPathProp != null && libraryPathProp.trim().length() > 0) {
                trace("returning project std dir");
                return libraryPathProp.trim();
            }
        }
        String path = System.getenv("HAXE_LIBRARY_PATH");
        if (path != null && path.trim().length() > 0) {
            return path;
        }

        return getSystemDefaultHaxeInstallPath() + File.separator + "std";
    }


    protected static Pattern patternError = Pattern.compile("(.*):[ ]*([0-9]+):(.*:.*)");
    protected static Pattern patternComment = Pattern.compile("^[ \t]*(/\\*|\\*|#|//).*");

}
