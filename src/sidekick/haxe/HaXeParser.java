package sidekick.haxe;

import java.util.Arrays;
import javax.swing.JPanel;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

import sidekick.GenericSideKickCompletion;
import sidekick.SideKickCompletion;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;
import errorlist.DefaultErrorSource;
import errorlist.ErrorSource;

public class HaXeParser extends SideKickParser
{
    public HaXeParser ()
    {
        super("haxe");
    }

    @Override
    public void activate (View view)
    {
        if (_ctagsParser == null && jEdit.getPlugin("ctags.sidekick.Plugin") != null) {
            _ctagsParser = ServiceManager.getService(SideKickParser.class, "ctags");
        }
        ErrorSource.registerErrorSource(HaXeSideKickPlugin._errorSource);
    }

    @Override
    public boolean canHandleBackspace ()
    {
        return false;
    }

    @Override
    public SideKickCompletion complete (EditPane editPane, int caret)
    {
        //If the caret is at a ".", use the Haxe compiler to provide completion hints
        if (editPane.getBuffer().getText(caret - 1, 1).equals(".")) {
            GenericSideKickCompletion completion = HaXeSideKickPlugin.getSideKickCompletion(editPane, caret);
            return completion;
        } else if (_ctagsParser != null) {
            trace("complete, _ctagsParser=" + _ctagsParser);
            String[] keywords = editPane.getBuffer().getKeywordMapAtOffset(caret).getKeywords();
            System.out.println("keywords=" + Arrays.toString(keywords));
//            if (keywords.length > 0) {
//                String word = getWordAtCaret( editPane, caret );
//                if (word != null && word.length() > 0) {
//                    List possibles = new ArrayList();
//                    for (int i = 0; i < keywords.length; i++) {
//                        String kw = keywords[i];
//                        if (kw.startsWith(word) && !kw.equals(word)) {
//                            possibles.add(keywords[i]);
//                        }
//                    }
//                    Collections.sort(possibles);
//                    return new ConcreteSideKickCompletion(editPane.getView(), word, possibles);
//                }
//            }
            SideKickCompletion ctagsCompletion = _ctagsParser.complete(editPane, caret);
//            for (int ii = 0; ii < ctagsCompletion.size(); ++ii) {
//                CodeCompletionVariable ccvar = new CodeCompletionVariable();
//                ccvar.name = ctagsCompletion.get(ii).toString();
//            }


//            if (ctagsCompletion.size())
//            EditAction action = jEdit.getAction("ctags-interface-complete-from-db");
//            action.invoke(editPane.getView());
            return ctagsCompletion;
        }
        return null;
    }

    protected void trace(Object ... args)
    {
        HaXeSideKickPlugin.trace(args);
    }

    @Override
    public void deactivate (View view)
    {
        ErrorSource.unregisterErrorSource(HaXeSideKickPlugin._errorSource);
    }

    @Override
    public JPanel getPanel()
    {
        if (_ctagsParser == null) {
            return null;
        }
        return _ctagsParser.getPanel();
    }

    @Override
    public String getInstantCompletionTriggers ()
    {
        return COMPLETION_CHARS;
    }

    /**
     * We use the Ctags display of the code structure.
     */
    @Override
    public SideKickParsedData parse (Buffer buffer, DefaultErrorSource errorSource)
    {
        Log.log(Log.DEBUG, this, "parse request");
        if (_ctagsParser != null) {
            _ctagsParsed = _ctagsParser.parse(buffer, errorSource);
            return _ctagsParsed;
        }
        return new SideKickParsedData(buffer.getPath());
    }

    @Override
    public boolean supportsCompletion ()
    {
        return true;
    }

    private SideKickParsedData _ctagsParsed;
    private SideKickParser _ctagsParser;
    private final static String COMPLETION_CHARS = ".";

}
