package sidekick.haxe;

import javax.swing.JPanel;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.util.Log;

import sidekick.CodeCompletion;
import sidekick.CodeCompletionMethod;
import sidekick.GenericSideKickCompletion;
import sidekick.SideKickCompletion;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;
import ctags.sidekick.Parser;
import errorlist.DefaultErrorSource;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HaXeParser extends SideKickParser
    implements EBComponent
{

    public void handleMessage (EBMessage message)
    {
        if (message instanceof BufferUpdate && ((BufferUpdate) message).getWhat() == BufferUpdate.SAVED) {
            Buffer buffer = ((BufferUpdate) message).getBuffer();
            VPTProject prj = ProjectViewer.getActiveProject(jEdit.getActiveView());
            if (prj.isInProject(buffer.getPath())) {
                HaXeSideKickPlugin.buildProject();
            }
        }
    }

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
        EditBus.addToBus(this);
    }

    @Override
    public void deactivate (View view)
    {
        EditBus.removeFromBus(this);
    }

    /**
     * We use the Ctags display of the code structure.
     */
    public SideKickParsedData parse (Buffer buffer, DefaultErrorSource errorSource)
    {
        Log.log(Log.DEBUG, this, "parse request");
        HaXeSideKickPlugin.buildProject();
        _ctagsParsed = _ctagsParser.parse(buffer, errorSource);
        return _ctagsParsed;
    }

    @Override
    public boolean canHandleBackspace ()
    {
        // TODO recall the compiler
        return false;
    }

    @Override
    public SideKickCompletion complete (EditPane editPane, int caret)
    {
        String completionXMLString = HaXeSideKickPlugin.getCodeCompletionXML(editPane, caret);

        if (completionXMLString == null || completionXMLString.trim().equals("")) {
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

// @Override
// public SideKickCompletionPopup getCompletionPopup (View view, int caretPosition,
// SideKickCompletion complete, boolean active)
// {
// // TODO Auto-generated method stub
// return super.getCompletionPopup(view, caretPosition, complete, active);
// }

    @Override
    public String getInstantCompletionTriggers ()
    {
        return COMPLETION_CHARS;
    }

    @Override
    public JPanel getPanel ()
    {
        // TODO Auto-generated method stub
        return super.getPanel();
    }

    @Override
    public boolean supportsCompletion ()
    {
        return true;
    }

}
