package sidekick.haxe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import completion.util.CompletionUtil;

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
        return buildProject(jEdit.getActiveView().getEditPane());
    }

    public static HaxeCompilerOutput buildProject (EditPane editPane)
    {
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
        if (output != null) {
            handleBuildErrors(output.output.errors, _errorSource, output.getProjectRoot(), getBuildFile(editPane.getBuffer()));
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
            return getFirstBuildFileInDir(projectRoot);
        }
        return null;
    }

    // Get the first *.hxml file we find
    public static File getBuildFile (Buffer buffer)
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

        JOptionPane.showMessageDialog(null, "No .hxml file found in the project root folder, nor any parent folder.", "Error", JOptionPane.ERROR_MESSAGE);
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

        File hxmlFile = getBuildFile(editPane.getBuffer());


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

        String command = "haxe ";

        String haxeExecutableProp = prj.getProperty(ProjectOptionPane.PROJECT_HAXE_EXECUTABLE);
        if (haxeExecutableProp != null && haxeExecutableProp.length() > 0) {
            command = haxeExecutableProp + " ";
        }

        command += hxmlFile.getName();

        if (getCodeCompletion) {
            String path = editPane.getBuffer().getPath();
            path = path.substring(projectRootPath.length());
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }
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
//            	trace("Errorline:" + errorLine);
                m = patternError.matcher(errorLine);
                if (m.matches()) {
                    DefaultError error = new DefaultError(errorSource, ErrorSource.ERROR,
                        projectRootPath + File.separatorChar + m.group(1), Integer.parseInt(m.group(2)) - 1, 0, 0, m.group(3));
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

    public static void addMissingImports (final View view)
    {
        addImports(view, false);
    }

    protected static void addImports (final View view, boolean onlyAtCaret)
    {
        Buffer buffer = view.getBuffer();
        JEditTextArea textArea = view.getTextArea();
        Set<String> importTokens = null;
        String[] lines = view.getTextArea().getText().split("\n");
        if (onlyAtCaret) {
            String importString = CompletionUtil.getWordAtCaret(view);
            importTokens = new HashSet<String>();
            importTokens.add(importString);
        } else {
            importTokens = getImportableClasses(lines);
        }


        //Remove the class name from the list of import tokens, so you don't import yourself
        String filename = view.getBuffer().getName();
        importTokens.remove(filename.substring(0, filename.length() - 3));
        importTokens.remove("Public");

        Set<String> existingImports = getCurrentImports(lines);
        Map<String, Set<String>> classPackages = getImportableClasses(buffer);

        if (classPackages == null || classPackages.size() == 0) {
        	return;
        }

        List<String> importsToAdd = new ArrayList<String>();

        for (String importToken : importTokens) {
            if (!existingImports.contains(importToken)) {
                if (classPackages.containsKey(importToken)) {
                    if (classPackages.get(importToken).size() == 1) {
                        importsToAdd.add("import " + classPackages.get(importToken).iterator().next() + ";");
                    } else {//Handle the duplicates

                        Set<String> dups = classPackages.get(importToken);
                        String[] options = new String[dups.size()];
                        options = dups.toArray(options);

                        int n = JOptionPane.showOptionDialog(view,
                            "Resolve import " + importToken,
                            "Resolve import " + importToken,
                            JOptionPane.NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
                        if (n >= 0) {
                            importsToAdd.add("import " + options[n] + ";");
                        }
                    }
                } else {
                    Log.log(Log.NOTICE, "HaXe", "No import found for " + importToken);
                }
            }
        }

        //Add existing imports
        String line;
        Matcher m;

        //To keep the conditional imports in the correct order
        Map<String, String> conditionalCompilationCodeBefore = new HashMap<String, String>();
        Map<String, String> conditionalCompilationCodeAfter = new HashMap<String, String>();

        Pattern packagePrefixPattern = Pattern.compile("^[ \t]*(import|using)[ \t]+([a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?+).*");

        for (int ii = 0; ii < buffer.getLineCount(); ++ii) {
            line = buffer.getLineText(ii);
            m = patternImport.matcher(line);
            if (m.matches()) {
                importsToAdd.add(line.trim());
                if (ii - 1 >= 0 && buffer.getLineText(ii - 1).trim().startsWith("#")) {
                    conditionalCompilationCodeBefore.put(line.trim(), buffer.getLineText(ii - 1).trim());
                }
                //If there's conditional compilation code under us, AND there's no import under that, bind the code to this line
                if (ii + 1 < buffer.getLineCount() && buffer.getLineText(ii + 1).trim().startsWith("#") && (ii + 2 >= buffer.getLineCount() || !packagePrefixPattern.matcher(buffer.getLineText(ii + 2)).matches())) {
                    conditionalCompilationCodeAfter.put(line.trim(), buffer.getLineText(ii + 1).trim());
                }
            }
        }

        //Sort imports
        Collections.sort(importsToAdd);

        // Insert imports
        StringBuffer bufferText = new StringBuffer();
        boolean addedImports = false;
        Pattern packagePattern = Pattern.compile("^[ \t]*package[ \t;$].*");

        boolean pastImportZone = false;
        for (int ii = 0; ii < buffer.getLineCount(); ++ii) {
            line = buffer.getLineText(ii);
            if (!addedImports) {
                bufferText.append(line + "\n");
                if (packagePattern.matcher(line).matches()) {
                    String currentPackagePrefix = "";
                    //Add all the imports
                    for (String newImport : importsToAdd) {
                        m = packagePrefixPattern.matcher(newImport);
                        String packagePrefix = null;
                        if (m != null && m.matches()) {
                            packagePrefix = m.group(2);
                        }

                        if (!currentPackagePrefix.equals(packagePrefix)) {
                            bufferText.append("\n");
                            currentPackagePrefix = packagePrefix;
                        }
                        if (conditionalCompilationCodeBefore.containsKey(newImport)) {
                            bufferText.append(conditionalCompilationCodeBefore.get(newImport) + "\n");
                        }
                        bufferText.append(newImport + "\n");
                        if (conditionalCompilationCodeAfter.containsKey(newImport)) {
                            bufferText.append(conditionalCompilationCodeAfter.get(newImport) + "\n");
                        }
                    }
                    bufferText.append("\n");
                    addedImports = true;
                }
            } else if (!pastImportZone) {
                if (patternPastImportZone.matcher(line).matches()) {
                    bufferText.append(line + "\n");
                    pastImportZone = true;
                }
            } else if (!patternImport.matcher(line).matches()) {
                bufferText.append(line + "\n");
            }
        }

        String caretLine = textArea.getLineText(textArea.getCaretLine()).trim();
        int caretLineOffset = textArea.getCaretPosition() - textArea.getLineStartOffset(textArea.getCaretLine());

        String firstLineTest = textArea.getLineText(textArea.getFirstLine()).trim();
        String textString = bufferText.toString();
        textString = removeDuplicateEmptyLines(textString);
        textArea.setText(textString);

        //Make sure the caret is in the same loc
        for (int ii = 0; ii < textArea.getLineCount(); ii++) {
            if (textArea.getLineText(ii).trim().equals(caretLine)) {
                textArea.setCaretPosition(textArea.getLineStartOffset(ii) + caretLineOffset);
                break;
            }
        }

        //Make sure the text area stays with the same view
        for (int ii = 0; ii < textArea.getLineCount(); ii++) {
            if (textArea.getLineText(ii).trim().equals(firstLineTest)) {
                textArea.setFirstLine(ii);
                break;
            }
        }
    }

    /**
     * Adds the import at the cursor
     * @param view
     */
    public static void addImport (final View view)
    {
        addImports(view, true);
    }

    public static void goToLastEditLocation (final View view)
    {
        lastEditLocation.goToLastEditLocation();
    }

    static protected List<File> getFileListingNoSort (File aStartingDir)
        throws FileNotFoundException
    {
        List<File> result = new ArrayList<File>();
        if (!aStartingDir.exists()) {
        	return result;
        }
        File[] filesAndDirs = aStartingDir.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        for (File file : filesDirs) {
            if (!file.exists()) {
                Log.log(Log.ERROR, "HaXe", "getFileListingNoSort, file doesn't exist:" + file);
                continue;
            }
            if (file.isDirectory()) {
                // recursive call!
                List<File> deeperList = getFileListingNoSort(file);
                result.addAll(deeperList);
            } else if (file.getName().endsWith(".hx")) {// add if haxe file
                result.add(file);
            }
        }
        return result;
    }

    protected static Set<String> getImportableClasses (final String[] lines)
    {
        Set<String> importTokens = new HashSet<String>();

        if (lines == null || lines.length == 0) {
            return importTokens;
        }
        try {
            Matcher m;
            for (String line : lines) {

                if (line.trim().startsWith("//") || line.trim().startsWith("*") || line.trim().startsWith("/*")) {
                    continue;
                }

                m = patternVar.matcher(line);
                if (m.matches()) {
                    importTokens.add(m.group(1));
                }

                m = patternExtends.matcher(line);
                if (m.matches()) {
                    importTokens.add(m.group(2));
                }

                m = patternNew.matcher(line);
                if (m.matches()) {
                    importTokens.add(m.group(1));
                }

                getRepeatedMatches(patternStatics, line, importTokens);
                getRepeatedMatches(patternGenerics, line, importTokens);
                getRepeatedMatches(patternArgument, line, importTokens);

                m = patternImplements.matcher(line);
                if (m.matches()) {
                    String implementsStuff = m.group(1);
                    implementsStuff = implementsStuff.replace("{", "");
                    String[] tokens = implementsStuff.split(",");
                    for (String token : tokens) {
                        token = token.trim();
                        if (!token.equals("")) {
                            importTokens.add(token);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            trace("EXCEPTION=" + e);
        }
        return importTokens;
    }

    protected static Map<String, Set<String>> getAllClassPackages (Buffer buffer)
    {
        File hxmlFile = HaXeSideKickPlugin.getBuildFile(buffer);
        if (hxmlFile == null) {
            Log.log(Log.ERROR, "HaXe", "No .hxml file found to get class paths");
            JOptionPane.showMessageDialog(null, "No .hxml file found to get class paths", "Warning", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return getPackagesFromHXMLFile(hxmlFile);
    }

    static protected Set<String> getCurrentImports (String[] lines)
    {
        Set<String> existingImports = new HashSet<String>();

        Matcher m;

        for (String line : lines) {
            m = patternImport.matcher(line);
            if (m.matches()) {
                String fullClassName = m.group(2);
                String[] tokens = fullClassName.split("\\.");
                existingImports.add(tokens[tokens.length - 1]);
            }
        }
        return existingImports;
    }

    protected static String removeDuplicateEmptyLines (String s)
    {
        int idx = s.indexOf("\n\n\n");
        while (idx > -1) {
            s = s.replace("\n\n\n", "\n\n");
            idx = s.indexOf("\n\n\n");
        }
        return s;
    }

    protected static Map<String, Set<String>> getPackagesFromHXMLFile (File hxmlFile)
    {
        Map<String, Set<String>> classPackages = new HashMap<String, Set<String>>();
        Set<String> classPaths = new HashSet<String>();
        Set<String> stdLibs = new HashSet<String>();

        if (!hxmlFile.exists()) {
            Log.log(Log.ERROR, "HaXe", "*.hxml file doesn't exist: " + hxmlFile);
            return classPackages;
        }
        // Get the classpaths from the *.hxml file
        try {
            FileReader reader = new FileReader(hxmlFile);
            BufferedReader in = new BufferedReader(reader);
            String str;
            Pattern cp = Pattern.compile("^[ \t]*-cp[ \t]+(.*)");
            Pattern libPattern = Pattern.compile("^[ \t]*-lib[ \t]+(.*)");
            Matcher m;
            while ((str = in.readLine()) != null) {
                m = cp.matcher(str);
                if (m.matches()) {
                    classPaths.add(hxmlFile.getParentFile().getAbsolutePath() + File.separator
                        + m.group(1));
                }

                m = libPattern.matcher(str);
                if (m.matches()) {
                    stdLibs.add(m.group(1));
                }
            }
            in.close();
            reader.close();
        } catch (IOException e) {
            Log.log(Log.ERROR, "HaXe", e.toString());
        }

        StringBuilder libsString = new StringBuilder();
        libsString.append(".*(");
        Iterator<String> libsIter = stdLibs.iterator();
        if (libsIter.hasNext()) {
            libsString.append(libsIter.next());
        }
        while (libsIter.hasNext()) {
            libsString.append("|" + libsIter.next());
        }
        libsString.append(").*");
        Pattern libsPattern = Pattern.compile(libsString.toString());

        //jEdit.getProperty("options.haxe.installDir");
        //Add the system classpaths
        String installDirString = jEdit.getProperty(OPTION_PREFIX + "installDir");

        if (installDirString == null || installDirString.trim().equals("") || installDirString.indexOf("System Default") >= 0) {
        	installDirString = HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath();
        }


        File haxelib = new File(jEdit.getProperty("options.haxe.haxelibDir"));//new File(jEdit.getProperty("options.haxe.stdDir"));//installDir.getAbsolutePath() + File.separator + "lib");

        if (!haxelib.exists()) {
            JOptionPane.showMessageDialog(null, "haxelib folder " + haxelib + " doesn't exist.  Check the \"Installation Directory\" option in Plugins->Plugin Options->Haxe", "Error", JOptionPane.ERROR_MESSAGE);
        }

        Pattern startsWithNumber = Pattern.compile("^[0-9].*");
        if (haxelib.exists() && haxelib.isDirectory()) {
            for (File libDir : haxelib.listFiles()) {
                if (libDir.isDirectory()) {
                    for (File versioned : libDir.listFiles()) {
                        if (versioned.isDirectory() && libsPattern.matcher(versioned.getAbsolutePath()).matches() && startsWithNumber.matcher(versioned.getName()).matches()) {
                            classPaths.add(versioned.getAbsolutePath().replace("flash9", "flash"));
                        }
                    }
                }
            }
        } else {
            Log.log(Log.ERROR, "HaXe", "HaXe stdlib directory doesn't exist: " + haxelib);
        }

        classPaths.add(jEdit.getProperty("options.haxe.stdDir"));

        // Go through the classpaths and add the *.hx files
        try {
            for (String path : classPaths) {
                List<File> haxeFiles = getFileListingNoSort(new File(path));

                // Break down the name to correctly be the package
                for (File haxeFile : haxeFiles) {
                    String fullPath = haxeFile.getAbsolutePath();
                    fullPath = fullPath.substring(0, fullPath.length() - 3);
                    String packagePath = fullPath.substring(path.length() + 1);
                    packagePath = packagePath.replace('/', '.');
                    packagePath = packagePath.replace('\\', '.');
                    packagePath = packagePath.replace("flash9", "flash");
                    String className = haxeFile.getName();
                    className = className.substring(0, className.length() - 3);

                    if (className.length() == packagePath.length()) {
                        continue;
                    }

                    if (packagePath.contains("_std") || packagePath.startsWith("std.")) {
                        continue;
                    }

                    if (!classPackages.containsKey(className)) {
                        classPackages.put(className, new HashSet<String>());
                    }

                    classPackages.get(className).add(packagePath);
                }
            }
        } catch (FileNotFoundException e) {
            Log.log(Log.ERROR, "HaXe", e.toString());
        }

        return classPackages;
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

    public static void clearImportCache ()
    {
        importableClassesCache = null;
        currentProjectRootForImporting = null;
    }

    public static void getRepeatedMatches (Pattern pattern, String line, Set<String> matches)
    {
        Matcher m = pattern.matcher(line);
        if (m.matches()) {

            String strippedLine = line;
            String group = m.group(1);
            int groupIndex = strippedLine.indexOf(group);
            //Add the first
            //And search for other arguments

            do  {
                matches.add(m.group(1));
                group = m.group(1);
                groupIndex = strippedLine.indexOf(group);
                if (groupIndex < 0 || group.length() == 0) {
                    break;
                }
                strippedLine = strippedLine.replace(m.group(1), "");
                m = pattern.matcher(strippedLine);

            } while (m.matches());
        }
    }

    protected static Map<String, Set<String>> getImportableClasses (Buffer buffer)
    {
        if (jEdit.getPlugin("projectviewer.ProjectPlugin", false) != null) {
            String projectRoot = getCurrentProject() == null ? null : getCurrentProject().getRootPath();//getProjectRoot();
            if (currentProjectRootForImporting != projectRoot) {
                currentProjectRootForImporting = projectRoot;
                importableClassesCache = getAllClassPackages(buffer);
            }
        }

        if (importableClassesCache != null) {
            return importableClassesCache;
        }

        return getAllClassPackages(buffer);
    }

    protected static void checkAndUpdateProjectHaxeBuildFile (VPTProject prj)
    {
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


    protected static Pattern patternVar = Pattern.compile(".*[ \t]var[ \t].*:[ \t]*([A-Za-z0-9_]+).*");
    protected static Pattern patternExtends = Pattern.compile("^.*class[ \t]+([A-Za-z0-9_]+)[ \t]extends[ \t]([A-Za-z0-9_]+).*");
    protected static Pattern patternImplements = Pattern.compile(".*[ \t]implements[ \t]+(.*)");
    protected static Pattern patternNew = Pattern.compile("^.*[ \t\\(\\[]+new[ \t]+([A-Za-z0-9_]+).*");
    protected static Pattern patternStatics = Pattern.compile(".*[{ \t\\(]([A-Z_][A-Za-z0-9_]*).*");
    protected static Pattern patternArgument = Pattern.compile(".*:[ \t]*([A-Z][A-Za-z0-9_]*).*");
    protected static Pattern patternImport = Pattern.compile("^[ \t]*(import|using)[ \t]+(.*);.*");
    protected static Pattern patternError = Pattern.compile("(.*):[ ]*([0-9]+):(.*:.*)");
    protected static Pattern patternGenerics = Pattern.compile(".*<[ \t]*([A-Z_]+[A-Za-z0-9_]*)[ \t]*>.*");
    protected static Pattern patternPastImportZone = Pattern.compile("^[ \t]*(@|class|interface|/\\*|typedef|enum).*");
    protected static Pattern patternComment = Pattern.compile("^[ \t]*(/\\*|\\*|#|//).*");

    private static Map<String, Set<String>> importableClassesCache;
    private static String currentProjectRootForImporting;

}

