package sidekick.haxe;

import static sidekick.haxe.HaXeSideKickPlugin.getHaxelibPath;
import static sidekick.haxe.HaXeSideKickPlugin.getStdLibPath;
import static sidekick.haxe.HaXeSideKickPlugin.trace;

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
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

import completion.util.CompletionUtil;

public class ImportManager
{
    public static void addMissingImports (final View view)
    {
        addImports(view, false, false);
    }

    public static void clearImportCache ()
    {
        importableClassesCache = null;
        currentProjectRootForImporting = null;
    }

    /**
     * Adds the import at the cursor to the imports at the top
     * @param view
     */
    public static void addImport (final View view, boolean using)
    {
        addImports(view, true, using);
    }

    public static void addImport (final View view)
    {
        addImports(view, true, false);
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

    public static String getFullClassName (String baseClassName)
    {
        Map<String, List<String>> classPackages = getAllImportableClasses();

        if (classPackages == null || classPackages.size() == 0) {
            return null;
        }

        if (classPackages.containsKey(baseClassName)) {
            if (classPackages.get(baseClassName).size() == 1) {
                return classPackages.get(baseClassName).get(0);
            } else {//Handle the duplicates
                List<String> dups = classPackages.get(baseClassName);
                String[] options = new String[dups.size()];
                options = dups.toArray(options);

                int n = JOptionPane.showOptionDialog(jEdit.getActiveView(),
                    "Resolve import " + baseClassName,
                    "Resolve import " + baseClassName,
                    JOptionPane.NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (n >= 0) {
                    return options[n];
                }
            }
        } else {
            Log.log(Log.NOTICE, "HaXe", "No import found for " + baseClassName);
        }
        return null;
    }

    public static List<String> getFullClassNames (String baseClassName)
    {
        Map<String, List<String>> classPackages = getAllImportableClasses();

        if (classPackages == null || classPackages.size() == 0) {
            return null;
        }
        return classPackages.get(baseClassName);
    }

    protected static void addImports (final View view, boolean onlyAtCaret, boolean using)
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


        Set<String> existingImports = getCurrentImports(lines, using);
        List<String> importsToAdd = new ArrayList<String>();

        //Don't import classes in the same package
        String bufferPackage = getBufferPackage(buffer);

        for (String importToken : importTokens) {
            if (!existingImports.contains(importToken)) {

                String fullName = getFullClassName(importToken);
                String packageName = getPackageOfFullClassName(fullName);
                if (fullName != null && (bufferPackage == null || !bufferPackage.equals(packageName))) {
                    importsToAdd.add((using ? "using " : "import ") + fullName + ";");
                }
            }
        }

        //Add to existing imports at the top of the source file
        String line;
        Matcher m;

        List<String> conditionalTokens = new ArrayList<String>();
        //To keep the conditional imports in the correct order
        Map<String, String> conditionalCompilationCodeBefore = new HashMap<String, String>();
        Map<String, String> conditionalCompilationCodeAfter = new HashMap<String, String>();


        boolean isInCompilerConditional = false;
        boolean addedImportInConditional = false;
        String currentConditionalToken = null;
        for (int ii = 0; ii < buffer.getLineCount(); ++ii) {
            line = buffer.getLineText(ii);

            if (line.trim().startsWith("#if")) {
                isInCompilerConditional = true;
                addedImportInConditional = false;
                currentConditionalToken = line;
            }

            if (line.trim().startsWith("#end")) {
                if (addedImportInConditional) {
                    currentConditionalToken += "\n" + line;
                    trace("adding currentConditionalToken=" + currentConditionalToken);
                    conditionalTokens.add(currentConditionalToken);
                }
                currentConditionalToken = null;
                isInCompilerConditional = false;
                addedImportInConditional = false;
            }

//            m = patternImport.matcher(line);
            if (patternImport.matcher(line).matches() || patternUsing.matcher(line).matches()) {
                if (isInCompilerConditional) {
                    addedImportInConditional = true;
                    currentConditionalToken += "\n" + line.trim();
                } else {
                    importsToAdd.add(line.trim());
                }
//                if (ii - 1 >= 0 && buffer.getLineText(ii - 1).trim().startsWith("#")) {
//                    conditionalCompilationCodeBefore.put(line.trim(), buffer.getLineText(ii - 1).trim());
//                }
//                //If there's conditional compilation code under us, AND there's no import under that, bind the code to this line
//                if (ii + 1 < buffer.getLineCount() && buffer.getLineText(ii + 1).trim().startsWith("#") && (ii + 2 >= buffer.getLineCount() || !packagePrefixPattern.matcher(buffer.getLineText(ii + 2)).matches())) {
//                    conditionalCompilationCodeAfter.put(line.trim(), buffer.getLineText(ii + 1).trim());
//                }
            }
        }

//        trace("conditional tokens");
//        for(String t : conditionalTokens) {
//            trace("token: " + t);
//        }

        //Sort imports
        Collections.sort(importsToAdd);
//        trace("importsToAdd");
//        for(String t : importsToAdd) {
//            trace("import to add " + t);
//        }

        // Insert imports
        StringBuffer bufferText = new StringBuffer();
        boolean addedImports = false;
        Pattern packagePattern = Pattern.compile("^[ \t]*package[ \t;$].*");

        Pattern packagePrefixPattern = Pattern.compile("^[ \t]*(import|using)[ \t]+([a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?+).*");
        boolean pastImportZone = false;
//        boolean isInImportCompilerConditional = false;
        isInCompilerConditional = false;
        for (int ii = 0; ii < buffer.getLineCount(); ++ii) {
            line = buffer.getLineText(ii);

            if (!addedImports) {
//                if (line.trim().startsWith("#if")) {
//                    isInCompilerConditional = true;
////                    isInImportCompilerConditional = false;
//                }
//                if (line.trim().startsWith("#end")) {
//                    isInCompilerConditional = false;
////                    isInImportCompilerConditional = false;
//                }
//                if (isInCompilerConditional) {
//                    if (patternImport.matcher(line).matches() || patternUsing.matcher(line).matches()) {
//                        isInImportCompilerConditional = true;
//                    }
//                }

//                if (!(line.trim().startsWith("#if") || line.trim().startsWith("#end") || line.trim().startsWith("#else"))) {
                    bufferText.append(line + "\n");
//                }

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
                    //Add the conditional blocks
                    for (String conditional : conditionalTokens) {
                        bufferText.append(conditional + "\n");
                    }

                    bufferText.append("\n");
                    addedImports = true;
//                    trace("after adding imports, bufferText=" + bufferText);
                }
            } else if (!pastImportZone) {
                if (patternPastImportZone.matcher(line).matches()) {
//                    trace("adding NOT past the import zone: " + line);
                    bufferText.append(line + "\n");
                    pastImportZone = true;
                }
            } else if (!(patternImport.matcher(line).matches() || patternUsing.matcher(line).matches())) {
                bufferText.append(line + "\n");
            }
        }
        String caretLine = textArea.getLineText(textArea.getCaretLine()).trim();
        int caretLineOffset = textArea.getCaretPosition() - textArea.getLineStartOffset(textArea.getCaretLine());

        String firstLineTest = textArea.getLineText(textArea.getFirstLine()).trim();
        String textString = bufferText.toString().trim();
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

    protected static Map<String, List<String>> getAllClassPackages ()
    {
        File hxmlFile = HaXeSideKickPlugin.getBuildFile();
        if (hxmlFile == null) {
            Log.log(Log.ERROR, "HaXe", "No .hxml file found to get class paths");
            JOptionPane.showMessageDialog(null, "No .hxml file found to get class paths", "Warning", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return getPackagesFromHXMLFile(hxmlFile);
    }

    protected static Map<String, List<String>> getAllImportableClasses ()
    {
        long now = System.currentTimeMillis();
        if (now - lastImportQueryTime > IMPORT_CACHE_EXPIRE_DELAY) {
            importableClassesCache = null;
        }
        String projectRoot = HaXeSideKickPlugin.getCurrentProject() == null ? null : HaXeSideKickPlugin.getCurrentProject().getRootPath();//getProjectRoot();
        if (currentProjectRootForImporting != projectRoot) {
            currentProjectRootForImporting = projectRoot;
            //Allow these results to last a while before recomputing
            lastImportQueryTime = System.currentTimeMillis();
            importableClassesCache = getAllClassPackages();
        }

        if (importableClassesCache != null) {
            return importableClassesCache;
        }

        //Allow these results to last a while before recomputing
        lastImportQueryTime = System.currentTimeMillis();
        return getAllClassPackages();
    }

    protected static Set<String> getCurrentImports (String[] lines, boolean using)
    {
        Set<String> existingImports = new HashSet<String>();

        Matcher m;

        for (String line : lines) {
            if (using) {
                m = patternUsing.matcher(line);
            } else {
                m = patternImport.matcher(line);
            }
            if (m.matches()) {
                String fullClassName = m.group(1);
                String[] tokens = fullClassName.split("\\.");
                existingImports.add(tokens[tokens.length - 1]);
            }
        }
        return existingImports;
    }

    protected static Map<String, List<String>> getPackagesFromHXMLFile (File hxmlFile)
    {
        return getPackagesFromHXMLFile(hxmlFile, null);
    }
    protected static Map<String, List<String>> getPackagesFromHXMLFile (File hxmlFile, File root)
    {
        if (root == null) {
            root = hxmlFile.getParentFile();
        }
        Map<String, Set<String>> classPackages = new HashMap<String, Set<String>>();
        Set<String> classPaths = new HashSet<String>();
        Set<String> haxelibs = new HashSet<String>();

        if (!hxmlFile.exists()) {
            Log.log(Log.ERROR, "HaXe", "*.hxml file doesn't exist: " + hxmlFile);
            return null;
        }
        // Get the classpaths from the *.hxml file
        List<String> inlineHxmlFiles = new ArrayList<String>();
        try {
            String fileContents = readFile(hxmlFile.getAbsolutePath());
            Pattern cp = Pattern.compile("^[ \t]*-cp[ \t]+(.*)");
            Pattern libPattern = Pattern.compile("^[ \t]*-lib[ \t]+(.*)");
            Matcher m;
            for (String str : fileContents.split(System.getProperty("line.separator"))) {
                m = cp.matcher(str);
                if (m.matches()) {
                    classPaths.add(root.getAbsolutePath() + File.separator
                        + m.group(1));
                }

                m = libPattern.matcher(str);
                if (m.matches()) {
                    haxelibs.add(m.group(1));
                }

                if (patternInlineHxmlFile.matcher(str).matches()) {
                    inlineHxmlFiles.add(str);
                }
            }
        } catch (IOException e) {
            Log.log(Log.ERROR, "HaXe", e.toString());
        }

//        Log.log(Log.MESSAGE, "HaXe", "haxe haxelibs=" + haxelibs);
//        Log.log(Log.MESSAGE, "HaXe", "haxe classPaths=" + classPaths);
//        Log.log(Log.MESSAGE, "HaXe", "haxe inlineHxmlFiles=" + inlineHxmlFiles);

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
            Log.log(Log.ERROR, null, "haxelib folder " + haxelib + " doesn't exist.  Check the \"Installation Directory\" option in Plugins->Plugin Options->Haxe");
        }

        Pattern startsWithNumber = Pattern.compile("^[0-9].*");
        if (haxelib.exists() && haxelib.isDirectory()) {


            for (File libDir : haxelib.listFiles()) {
                //Get the dev dir if present
                File devDir = getHaxelibDevDir(libDir);
                if (devDir != null) {
//                    Log.log(Log.MESSAGE, "HaXe", "Adding dev classpath dir: " + devDir);
                    classPaths.add(devDir.getAbsolutePath().replace("flash9", "flash"));
                } else {
                    if (libDir.isDirectory()) {
                        for (File versioned : libDir.listFiles()) {
                            if (versioned.isDirectory() && libsPattern.matcher(versioned.getAbsolutePath()).matches() && startsWithNumber.matcher(versioned.getName()).matches()) {
                                classPaths.add(versioned.getAbsolutePath().replace("flash9", "flash"));
                            }
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

//        Log.log(Log.MESSAGE, "HaXe classPaths", "haxe classpaths iterating=" + classPaths);

        // Go through the classpaths and add the *.hx files
        try {
            for (String path : classPaths) {
                Log.log(Log.MESSAGE, "HaXe", "    getting source files from " + path);
                List<File> haxeFiles = getFileListingNoSort(new File(path));

                // Break down the name to correctly be the package
                for (File haxeFile : haxeFiles) {
                    String fullPath = haxeFile.getAbsolutePath();
                    //Remove the suffix
                    fullPath = fullPath.substring(0, fullPath.length() - 3);
                    //Just the package path, without the absolute path part
                    String packagePath = fullPath.substring(path.length() + 1);
                    //Cleanup
                    packagePath = packagePath.replace('/', '.');
                    packagePath = packagePath.replace('\\', '.');
                    packagePath = packagePath.replace("flash9", "flash");
                    String className = haxeFile.getName();
                    className = className.substring(0, className.length() - 3);

                    //Classes without packages don't need to be imported
                    if (packagePath.indexOf('.') == -1) {
                        continue;
                    }

                    if (packagePath.contains("_std") || packagePath.startsWith("std.")) {
                        continue;
                    }

                    if (!classPackages.containsKey(className)) {
                        classPackages.put(className, new HashSet<String>());
                    }

//                    Log.log(Log.MESSAGE, "HaXe", "         " + packagePath);
                    classPackages.get(className).add(packagePath);
                }
            }
        } catch (FileNotFoundException e) {
            Log.log(Log.ERROR, "HaXe", e.toString());
        }
//        Log.log(Log.MESSAGE, "HaXe", "Constants=" + classPackages.get("Constants"));


        Map<String, List<String>> results = new HashMap<String, List<String>>();
        for (String baseclass : classPackages.keySet()) {
            List<String> imports = new ArrayList<String>();
            for(Object fullImport : classPackages.get(baseclass).toArray()) {
                imports.add((String)fullImport);
            }
            results.put(baseclass, imports);
        }

        for (String inlineHxmlFile : inlineHxmlFiles) {
            File f = new File(root + File.separator + inlineHxmlFile);
            if (!f.exists()) {
                Log.log(Log.ERROR, "HaXe", "No inline hxml file found: " + f);
                continue;
            }
            Map<String, List<String>> inlineResults = getPackagesFromHXMLFile(f, root);
            for (String k : inlineResults.keySet()) {
                if (results.containsKey(k)) {
                    results.get(k).addAll(inlineResults.get(k));
                } else {
                    results.put(k, inlineResults.get(k));
                }
            }
        }
        return results;
    }

    static protected File getHaxelibDevDir (File haxelibProjectDir)
    {
//        Log.log(Log.MESSAGE, "HaXe", "haxe, looking for a dev file in " + haxelibProjectDir);
        if (haxelibProjectDir == null || !haxelibProjectDir.isDirectory()) {
//            Log.log(Log.MESSAGE, "HaXe", "      not a directory");
            return null;
        }
        for (File file : haxelibProjectDir.listFiles()) {
//            Log.log(Log.MESSAGE, "HaXe", "      " + file);
            if (file.isFile() && file.getName().equals(".dev")) {
//                Log.log(Log.MESSAGE, "HaXe", "haxe, found dev file for " + haxelibProjectDir);
                try {
                    String fileContents = readFile(file.getAbsolutePath());
                    fileContents = fileContents.trim();
                    File f = new File(fileContents);
                    if (!f.exists()) {
                        Log.log(Log.ERROR, "HaXe", "dev path doesn't exist: " + fileContents);
                        return null;
                    }
                    return f;

                } catch (IOException e) {
                    // TODO: handle exception
                }
            }
        }
        return null;
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

    protected static String getBufferPackage(Buffer buffer)
    {
        String line;
        Matcher m;
        for(int ii = 0; ii < buffer.getLineCount() ; ii++) {
            line = buffer.getLineText(ii);
            if (line != null) {
                m = patternPackage.matcher(line);
                if (m.matches()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

    protected static String getPackageOfFullClassName (String classname)
    {
        if (classname == null || !classname.contains(".")) {
            return classname;
        }

        return classname.substring(0, classname.lastIndexOf('.'));
    }

    private static String readFile( String file ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader (file));
        String line  = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while( ( line = reader.readLine() ) != null ) {
            stringBuilder.append( line );
            stringBuilder.append( ls );
        }
        return stringBuilder.toString();
     }

    protected static Pattern patternImport = Pattern.compile("^[ \t]*import[ \t]+(.*);.*");
    protected static Pattern patternUsing = Pattern.compile("^[ \t]*using[ \t]+(.*);.*");
    protected static Pattern patternArgument = Pattern.compile(".*:[ \t]*([A-Z][A-Za-z0-9_]*).*");
    protected static Pattern patternExtends = Pattern.compile("^.*class[ \t]+([A-Za-z0-9_]+)[ \t]extends[ \t]([A-Za-z0-9_]+).*");
    protected static Pattern patternGenerics = Pattern.compile(".*<[ \t]*([A-Z_]+[A-Za-z0-9_]*)[ \t]*>.*");
    protected static Pattern patternImplements = Pattern.compile(".*[ \t]implements[ \t]+(.*)");
    protected static Pattern patternNew = Pattern.compile("^.*[ \t\\(\\[]+new[ \t]+([A-Za-z0-9_]+).*");
    protected static Pattern patternPastImportZone = Pattern.compile("^[ \t]*((@|private|public|class|interface|/\\*|typedef|enum)|#).*");
    protected static Pattern patternStatics = Pattern.compile(".*[{ \t\\(]([A-Z_][A-Za-z0-9_]*).*");
    protected static Pattern patternVar = Pattern.compile(".*[ \t]var[ \t].*:[ \t]*([A-Za-z0-9_]+).*");
    protected static Pattern patternPackage = Pattern.compile("^[ \t]*package[ \t]+([a-z][a-zA-Z0-9_\\.]*)[ \t;\n].*");
    protected static Pattern patternInlineHxmlFile = Pattern.compile("^[ \t]*(.*hxml)");

    private static Map<String, List<String>> importableClassesCache;
    private static long lastImportQueryTime = 0;
    private static long IMPORT_CACHE_EXPIRE_DELAY = 10 * 1000;//10 seconds
    private static String currentProjectRootForImporting;

}

