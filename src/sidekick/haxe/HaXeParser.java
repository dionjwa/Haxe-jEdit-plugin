package sidekick.haxe;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sidekick.CodeCompletion;
import sidekick.CodeCompletionField;
import sidekick.CodeCompletionMethod;
import sidekick.GenericSideKickCompletion;
import sidekick.SideKickCompletion;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;
import ctags.sidekick.Parser;
import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

public class HaXeParser extends SideKickParser
{
    public HaXeParser ()
    {
        super("haxe");
        _ctagsParser = new Parser("haxe");
    }

    @Override
    public void activate (View view)
    {
        ErrorSource.registerErrorSource(HaXeSideKickPlugin._errorSource);
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
        List<String> output = HaXeSideKickPlugin.getHaxeBuildOutput(editPane, caret, true);
        String completionXMLString = output.get(1).trim();

        if (completionXMLString == null || completionXMLString.equals("")
            || !completionXMLString.startsWith("<")) {
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
                    String argString = ((Element)element.getElementsByTagName("t").item(0)).getTextContent();
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
                                argsTypes.add(argTokens[1]);
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
            "", codeCompletions, null);
        return completion;
    }

    @Override
    public void deactivate (View view)
    {
        ErrorSource.unregisterErrorSource(HaXeSideKickPlugin._errorSource);
    }

    @Override
    public String getInstantCompletionTriggers ()
    {
        return COMPLETION_CHARS;
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
    public boolean supportsCompletion ()
    {
        return true;
    }

    private SideKickParsedData _ctagsParsed;
    private Parser _ctagsParser;
    private final static String COMPLETION_CHARS = ".";

}
