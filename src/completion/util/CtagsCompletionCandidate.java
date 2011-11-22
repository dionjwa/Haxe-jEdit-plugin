package completion.util;

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

import ctagsinterface.main.KindIconProvider;
import ctagsinterface.main.Tag;
/**
 * Use this class for Ctags code completions.
 *
 */
public class CtagsCompletionCandidate extends DefaultListCellRenderer
    implements CompletionCandidate
{
    public Tag tag;

    public CtagsCompletionCandidate (Tag tag)
    {
        this.tag = tag;
    }

    @Override
    public Component getListCellRendererComponent (JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        String kind = tag.getKind();
        if (kind == null)
            kind = "";
        setIcon(KindIconProvider.getIcon(kind));
        setText(CompletionUtil.prefixByIndex(getLabelText(), index));
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
        StringBuilder sb = new StringBuilder();
        sb.append(tag.getName());
        String signature = tag.getExtension("signature");
        if (signature != null && signature.length() > 0)
            sb.append(signature);
        String namespace = tag.getNamespace();
        if (namespace != null && namespace.length() > 0)
            sb.append(" - " + namespace);
        return sb.toString();
    }

    @Override
    public boolean isValid (View view)
    {
        String prefix = CompletionUtil.getCompletionPrefix(view);
        if (prefix == null || prefix.length() == 0) {
            return true;
        }

        //If the completion matches the prefix exactly, ignore it (it doesn't add anything)
        if (prefix.trim().equals(getDescription().trim())) {
            return false;
        }

        //If the prefix is all in lower case, ignore case.
        if (prefix.toLowerCase().equals(prefix)) {
            return tag.getName().toLowerCase().startsWith(prefix.toLowerCase());
        } else {
            return tag.getName().startsWith(prefix);
        }
    }

    @Override
    public int compareTo (CompletionCandidate o)
    {
        if (o instanceof CtagsCompletionCandidate) {
            tag.getName().compareTo(((CtagsCompletionCandidate)o).tag.getName());
        }
        return tag.getName().compareTo(o.getDescription());
    }
}