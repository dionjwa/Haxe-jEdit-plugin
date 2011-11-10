package sidekick.haxe;

import static completion.util.CompletionUtil.createAbbrev;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.TextArea;

import superabbrevs.SuperAbbrevs;

import completion.service.CompletionCandidate;
import completion.util.CompletionUtil;
import completion.util.CtagsCompletionCandidate;

import ctagsinterface.main.KindIconProvider;

public class CompletionCandidateFullPackageName extends DefaultListCellRenderer
    implements CompletionCandidate
{
    protected String fullClassName;
    public CompletionCandidateFullPackageName (String fullClassName)
    {
        super();
        this.fullClassName = fullClassName;
    }

    @Override
    public Component getListCellRendererComponent (JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        String kind = "class";
        setIcon(KindIconProvider.getIcon(kind));
        setText(CompletionUtil.prefixByIndex(getDescription(), index));
        return this;
    }

    @Override
    public void complete (View view)
    {
        TextArea textArea = view.getTextArea();
        String prefix = CompletionUtil.getCompletionPrefix(view);
        int caret = textArea.getCaretPosition();
        JEditBuffer buffer = textArea.getBuffer();
        if (prefix.length() > 0) {
            buffer.remove(caret - prefix.length(), prefix.length());
        }

        // Check if a parametrized abbreviation is needed
        String sig = getDescription();
        if (sig == null || sig.length() == 0)
            return;
        String abbrev = createAbbrev(sig);
        SuperAbbrevs.expandAbbrev(view, abbrev, null);
    }

    @Override
    public ListCellRenderer getCellRenderer ()
    {
        return this;
    }

    @Override
    public String getDescription ()
    {
        return null;
    }

    @Override
    public String getLabelText ()
    {
        return fullClassName;
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionUtil.getCompletionPrefix(view);
        return prefix != null && prefix.length() > 0 && fullClassName.endsWith(prefix);
    }

    @Override
    public int compareTo (CompletionCandidate o)
    {
        if (o instanceof CtagsCompletionCandidate) {
            fullClassName.compareTo(((CtagsCompletionCandidate)o).tag.getName());
        }
        return fullClassName.compareTo(o.getDescription());
    }

}
