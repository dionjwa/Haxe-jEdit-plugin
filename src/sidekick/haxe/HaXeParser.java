package sidekick.haxe;

import javax.swing.JPanel;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

import projectviewer.ProjectViewer;
import projectviewer.vpt.VPTProject;
import sidekick.SideKickCompletion;
import sidekick.SideKickParsedData;
import sidekick.SideKickParser;
import ctags.sidekick.Parser;
import errorlist.DefaultErrorSource;

public class HaXeParser extends SideKickParser implements EBComponent
{

    public void handleMessage (EBMessage message)
    {
        // TODO Auto-generated method stub

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
    public void activate( View view )
    {
//        EditBus.addToBus(this);
    }

    @Override
    public void deactivate( View view )
    {

    }


    /**
     * We use the Ctags display of the code structure.
     */
    public SideKickParsedData parse (Buffer buffer, DefaultErrorSource errorSource)
    {
        Log.log(Log.DEBUG, this, "parse request");
//        HaXeSideKickPlugin.buildProject();
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

        VPTProject prj = ProjectViewer.getActiveProject(editPane.getView());
        if (prj == null) {
            System.out.println("File has no project");
            return null;
        }
        String rootPath = prj.getRootPath();
        // TODO call the compiler here.
        return null;
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
        return false;
    }

}
