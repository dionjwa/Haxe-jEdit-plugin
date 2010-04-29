package sidekick.haxe;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

import sidekick.CodeCompletion;
import sidekick.CodeCompletionMethod;
import sidekick.GenericSideKickCompletion;
import sidekick.SideKickCompletion;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;
import ctags.sidekick.Parser;
import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HaXeParser extends SideKickParser
{
    public HaXeParser ()
    {
        super("haxe");
        _ctagsParser = new Parser("haxe");
    }

    private final static String COMPLETION_CHARS = ".";

    private Parser _ctagsParser;
    public SideKickParsedData _ctagsParsed;

    @Override
    public void activate (View view)
    {
        ErrorSource.registerErrorSource(HaXeSideKickPlugin._errorSource);
    }

    @Override
    public void deactivate (View view)
    {
        ErrorSource.unregisterErrorSource(HaXeSideKickPlugin._errorSource);
    }

    /**
     * We use the Ctags display of the code structure.
     */
    public SideKickParsedData parse (Buffer buffer, DefaultErrorSource errorSource)
    {
        Log.log(Log.DEBUG, this, "parse request");
        _ctagsParsed = _ctagsParser.parse(buffer, errorSource);
        return _ctagsParsed;
    }

    @Override
    public boolean canHandleBackspace ()
    {
        // TODO recall the compiler
        return true;
    }

    @Override
    public SideKickCompletion complete (EditPane editPane, int caret)
    {
        List<String> output = HaXeSideKickPlugin.getHaxeBuildOutput(editPane, caret, true);
        String completionXMLString = output.get(1).trim();//HaXeSideKickPlugin.getCodeCompletionXML(editPane, caret);


        if (completionXMLString == null || completionXMLString.equals("") ||
                !completionXMLString.startsWith("<")) {
            return null;
        }

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
                    CodeCompletionMethod cc = new CodeCompletionMethod();
                    cc.name = codeName;
                    codeCompletions.add(cc);
                }
// element.hasAttribute(xmlRecords)
// NodeList name = element.getElementsByTagName("n");
// Element line = (Element) name.item(0);
// System.out.println("Name: " + getCharacterDataFromElement(line));
//
// NodeList title = element.getElementsByTagName("title");
// line = (Element) title.item(0);
// System.out.println("Title: " + getCharacterDataFromElement(line));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        GenericSideKickCompletion completion = new GenericSideKickCompletion(editPane.getView(), "", codeCompletions, null);
        return completion;
    }

    public static String getCharacterDataFromElement (Element e)
    {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData)child;
            return cd.getData();
        }
        return "?";
    }

    @Override
    public String getInstantCompletionTriggers ()
    {
        return COMPLETION_CHARS;
    }

    @Override
    public boolean supportsCompletion ()
    {
        return true;
    }
}
