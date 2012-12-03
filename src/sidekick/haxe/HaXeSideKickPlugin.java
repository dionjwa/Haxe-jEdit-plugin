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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.w3c.dom.NodeList;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import sidekick.haxe.JavaSystemCaller.StreamGobbler;
import errorlist.DefaultErrorSource.DefaultError;
import errorlist.ErrorSource;

public class HaXeSideKickPlugin extends EditPlugin
{
    static HaXeErrorSource _errorSource = new HaXeErrorSource();

    public static LastEditLocation lastEditLocation;
    public final static String NAME = "sidekick.haxe";
    public final static String OPTION_PREFIX = "options.haxe.";
    public final static String PROPERTY_PREFIX = "plugin.sidekick.haxe.";

    /** 
     * Use (in order) the custom command, the first hxml file found in the project root, or the first *.hxproj found
     * in the project root.
     */
    public static String getBuildCommand()
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        String buildString = prj.getProperty(ProjectOptionPane.PROJECT_BUILD_CMD);
        if (buildString != null && !buildString.trim().isEmpty()) {
            return buildString.trim();
        }
        
        File hxprojFile = getHxprojFile();
        if (hxprojFile != null) {
            return getBuildCommandFromHxprojFile(hxprojFile);
        }
        
        File hxmlFile = getFirstProjectHXMLFile(prj);
        if (hxmlFile != null) {
            return getBuildCommandFromHxmlFile(hxmlFile);
        }
        
        return null;
    }
    
    public static String getBuildCommandFromHxmlFile(File hxmlFile)
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        
        String projectRootPath = prj.getNodePath();

        String command = "haxe ";

        String hxmlfile = hxmlFile.getAbsolutePath().replace(projectRootPath, "");
        if (hxmlfile.startsWith(File.separator)) {
            hxmlfile = hxmlfile.substring(1);
        }
        command += hxmlfile;
        
        
        return command;
    }
    
    public static String getBuildCommandFromHxprojFile(File hxprojFile)
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        
        String command = "haxe";
        
        javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(hxprojFile.getAbsolutePath());
        
        try {
            
            String evalString = "/project/classpaths//@path";
            //source
            NodeList results = (NodeList) xpath.evaluate(evalString, inputSource, XPathConstants.NODESET);
            if (results != null) {
                for (int i = 0; i < results.getLength(); i++) {
                    command += " -cp " + results.item(i).getTextContent();
                }
            }
            
            //Main class
            command += " -main " + xpath.evaluate("/project/build//@mainClass", inputSource);
            
            //Compiler options
            if (xpath.evaluate("/project/build//@enabledebug", inputSource).equalsIgnoreCase("True")) {
                command += " -debug";
            }
            if (xpath.evaluate("/project/build//@flashStrict", inputSource).equalsIgnoreCase("True")) {
                command += " --flash-strict";
            }
            
            //haxelibs
            results = (NodeList) xpath.evaluate("/project/haxelib//@name", inputSource, XPathConstants.NODESET);
            if (results != null) {
                for (int i = 0; i < results.getLength(); i++) {
                    command += " -lib " + results.item(i).getTextContent();
                }
            }
            
            //Output
            evalString = "/project/output//@platform";
            trace(evalString + ": " + (xpath.evaluate(evalString, inputSource)));
            if (xpath.evaluate(evalString, inputSource) != null && xpath.evaluate(evalString, inputSource).trim().toLowerCase().equals("flash player")) {
                String swfheader = ""; 
                if (xpath.evaluate("/project/output//@width", inputSource) != null && xpath.evaluate("/project/output//@height", inputSource) != null) {
                    swfheader += " -swf-header " + xpath.evaluate("/project/output//@width", inputSource) + ":" + xpath.evaluate("/project/output//@height", inputSource);
                }
                if (xpath.evaluate("/project/output//@fps", inputSource) != null) {
                    swfheader += ":" + xpath.evaluate("/project/output//@fps", inputSource);
                }
                if (xpath.evaluate("/project/output//@background", inputSource) != null) {
                    swfheader += ":" + xpath.evaluate("/project/output//@background", inputSource).replace("#", "");
                }
                if (swfheader.length() > 0) {
                    command += swfheader;
                }
                
                if (xpath.evaluate("/project/output//@version", inputSource) != null) {
                    command += " -swf-version " + xpath.evaluate("/project/output//@version", inputSource);
                }
                
                command += " -swf " + xpath.evaluate("/project/build//@mainClass", inputSource) + ".swf";
                command += " --no-output";
            }
            
        } catch (XPathExpressionException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        return command;
    }
    
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

//        checkAndUpdateProjectHaxeBuildFile(getCurrentProject());

        HaxeCompilerOutput output = getHaxeBuildOutput(editPane, 0, false, true);
        checkCompilerOutputForErrors(output);
        return output;
    }

    public static void checkCompilerOutputForErrors (HaxeCompilerOutput output)
    {
        if (output != null) {
            handleBuildErrors(output.output.errors, _errorSource, getCurrentProject().getNodePath(), getHxmlFile());
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
            return getFirstHxmlFileInDir(projectRoot);
        }
        return null;
    }

    /**
     *  Get the first hxml file found in the project root.
     * @return
     */
    public static File getHxprojFile ()
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        return getFirstFlashDevelopFileInDir(prj.getRootPath());
    }
    
    /**
     *  Get the first hxml file found in the project root.
     * @return
     */
    public static File getHxmlFile ()
    {
        VPTProject prj = getCurrentProject();
        if (prj == null) {
            return null;
        }
        return getFirstHxmlFileInDir(prj.getRootPath());
    }

    protected static File getFirstHxmlFileInDir (String path)
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
    
    protected static File getFirstFlashDevelopFileInDir (String path)
    {
        if (!new File(path).exists()) {
            return null;
        }
        // Get the first *.hxml file we find
        for (String filename : new File(path).list()) {
            if (filename.toLowerCase().endsWith(".hxproj")) {
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
    	
    	String command = getBuildCommand();
    	
        File hxmlFile = getHxmlFile();

        if (command == null) {
            Log.log(
                Log.ERROR,
                NAME,
                "Attempting to build haxe project, but not custom build comamnd or *.hxml at the project root.");
            if (showErrorPopups) {
                GUIUtilities.error(jEdit.getActiveView(), "haxe.error.noBuildFile", null);
            }
            return null;
        }

        String projectRootPath = prj.getNodePath();

        if (getCodeCompletion) {
            String path = editPane.getBuffer().getPath();
            command += " --display " + path + "@" + caret;
        }
        trace("  command=" + command);

        SystemProcessOutput output = JavaSystemCaller.systemCall(command, projectRootPath, null, null);
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
                        projectRootPath.replace(System.getProperty("user.dir"), ""), 0, 0, 0, errorLine));
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

    protected static String getHaxelibPath ()
    {
        String[] envp = {"HOME=" + System.getProperty("user.home")};
        SystemProcessOutput output = JavaSystemCaller.systemCall("haxelib config", System.getProperty("user.home"), null, envp);
        String path = output.output.trim();
        if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        trace("haxelib path=" + path);
        return path;
    }

    protected static String getStdLibPath ()
    {
        String path = System.getenv("HAXE_LIBRARY_PATH");
        if (path != null && path.trim().length() > 0) {
            return path;
        }

        return getSystemDefaultHaxeInstallPath() + File.separator + "std";
    }


    protected static Pattern patternError = Pattern.compile("(.*):[ ]*([0-9]+):(.*:.*)");
    protected static Pattern patternComment = Pattern.compile("^[ \t]*(/\\*|\\*|#|//).*");

}
