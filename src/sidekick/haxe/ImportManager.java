package sidekick.haxe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import completion.util.CompletionUtil;

import static sidekick.haxe.HaXeSideKickPlugin.trace;
import static sidekick.haxe.HaXeSideKickPlugin.getHaxelibPath;
import static sidekick.haxe.HaXeSideKickPlugin.getStdLibPath;

public class ImportManager
{
    public static void addMissingImports (final View view)
    {
        addImports(view, false);
    }

    public static void clearImportCache ()
    {
        importableClassesCache = null;
        currentProjectRootForImporting = null;
    }

    /**
     * Adds the import at the cursor
     * @param view
     */
    public static void addImport (final View view)
    {
        addImports(view, true);
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
            importTokens = getImportableTokens(lines);
        }


        //Remove the class name from the list of import tokens, so you don't import yourself
        String filename = view.getBuffer().getName();
        importTokens.remove(filename.substring(0, filename.length() - 3));
        importTokens.remove("Public");

        Set<String> existingImports = getCurrentImports(lines);
        Map<String, Set<String>> classPackages = getAllImportableClasses();

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

    protected static Set<String> getImportableTokens (final String[] lines)
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

    protected static Map<String, Set<String>> getAllClassPackages ()
    {
        File hxmlFile = HaXeSideKickPlugin.getBuildFile();
        if (hxmlFile == null) {
            Log.log(Log.ERROR, "HaXe", "No .hxml file found to get class paths");
            JOptionPane.showMessageDialog(null, "No .hxml file found to get class paths", "Warning", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return getPackagesFromHXMLFile(hxmlFile);
    }

    protected static Map<String, Set<String>> getAllImportableClasses ()
    {
        String projectRoot = HaXeSideKickPlugin.getCurrentProject() == null ? null : HaXeSideKickPlugin.getCurrentProject().getRootPath();//getProjectRoot();
        if (currentProjectRootForImporting != projectRoot) {
            currentProjectRootForImporting = projectRoot;
            importableClassesCache = getAllClassPackages();
        }

        if (importableClassesCache != null) {
            return importableClassesCache;
        }

        return getAllClassPackages();
    }

    protected static Set<String> getCurrentImports (String[] lines)
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

    protected static Map<String, Set<String>> getPackagesFromHXMLFile (File hxmlFile)
    {
        Map<String, Set<String>> classPackages = new HashMap<String, Set<String>>();
        Set<String> classPaths = new HashSet<String>();
        Set<String> haxelibs = new HashSet<String>();

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
                    haxelibs.add(m.group(1));
                }
            }
            in.close();
            reader.close();
        } catch (IOException e) {
            Log.log(Log.ERROR, "HaXe", e.toString());
        }

        //Create a regex pattern for searching the haxelibs
        StringBuilder libsString = new StringBuilder();
        libsString.append(".*(");
        Iterator<String> libsIter = haxelibs.iterator();
        if (libsIter.hasNext()) {
            libsString.append(libsIter.next());
        }
        while (libsIter.hasNext()) {
            libsString.append("|" + libsIter.next());
        }
        libsString.append(").*");
        Pattern libsPattern = Pattern.compile(libsString.toString());

        File haxelib = new File(getHaxelibPath());
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

        if (getStdLibPath() != null) {
            classPaths.add(getStdLibPath());
        } else {
            Log.log(Log.ERROR, null, "Could not find haxe std lib path");
        }

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

    protected static String removeDuplicateEmptyLines (String s)
    {
        int idx = s.indexOf("\n\n\n");
        while (idx > -1) {
            s = s.replace("\n\n\n", "\n\n");
            idx = s.indexOf("\n\n\n");
        }
        return s;
    }

    protected static Pattern patternImport = Pattern.compile("^[ \t]*(import|using)[ \t]+(.*);.*");
    protected static Pattern patternArgument = Pattern.compile(".*:[ \t]*([A-Z][A-Za-z0-9_]*).*");
    protected static Pattern patternExtends = Pattern.compile("^.*class[ \t]+([A-Za-z0-9_]+)[ \t]extends[ \t]([A-Za-z0-9_]+).*");
    protected static Pattern patternGenerics = Pattern.compile(".*<[ \t]*([A-Z_]+[A-Za-z0-9_]*)[ \t]*>.*");
    protected static Pattern patternImplements = Pattern.compile(".*[ \t]implements[ \t]+(.*)");
    protected static Pattern patternNew = Pattern.compile("^.*[ \t\\(\\[]+new[ \t]+([A-Za-z0-9_]+).*");
    protected static Pattern patternPastImportZone = Pattern.compile("^[ \t]*(@|private|public|class|interface|/\\*|typedef|enum).*");
    protected static Pattern patternStatics = Pattern.compile(".*[{ \t\\(]([A-Z_][A-Za-z0-9_]*).*");
    protected static Pattern patternVar = Pattern.compile(".*[ \t]var[ \t].*:[ \t]*([A-Za-z0-9_]+).*");

    private static Map<String, Set<String>> importableClassesCache;
    private static String currentProjectRootForImporting;

}
