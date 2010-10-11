package sidekick.haxe;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;

public class LastEditLocation extends BufferAdapter
    implements EBComponent
{

    public LastEditLocation ()
    {
        super();
        EditBus.addToBus(this);
    }

    public void goToLastEditLocation ()
    {
        if (_lastEditOffset == -1) {
            Log.log(Log.WARNING, this, "No edits done yet");
            return;
        }
        TextArea ta = _editPane.getTextArea();
        if (_currentBuffer != _lastEditedBuffer) {
            _editPane.setBuffer((Buffer)_lastEditedBuffer);
        }
        ta.setCaretPosition(_lastEditOffset);
        int line = ta.getLineOfOffset(_lastEditOffset);
        line -= ta.getVisibleLines() / 2;
        line = Math.max(line, 0);
        ta.setFirstLine(line);
    }

    @Override
    public void handleMessage (EBMessage message)
    {
        if (message instanceof EditPaneUpdate &&
                (((EditPaneUpdate)message).getWhat() == EditPaneUpdate.BUFFER_CHANGED ||
                        ((EditPaneUpdate)message).getWhat() == EditPaneUpdate.CREATED)) {
            if (_currentBuffer != null) {
                _currentBuffer.removeBufferListener(this);
            }
            _currentBuffer = ((EditPaneUpdate)message).getEditPane().getBuffer();
            _currentBuffer.addBufferListener(this);
            _editPane = ((EditPaneUpdate)message).getEditPane();
        }
    }

    @Override
    public void contentInserted(JEditBuffer buffer, int startLine, int offset,
        int numLines, int length)
    {
        cacheEdit(buffer, offset);
    }

    @Override
    public void contentRemoved(JEditBuffer buffer, int startLine, int offset,
        int numLines, int length)
    {
        cacheEdit(buffer, offset);
    }

    private void cacheEdit (JEditBuffer buffer, int offset)
    {
        _lastEditOffset = offset;
        _lastEditedBuffer = _currentBuffer;
    }

    private int _lastEditOffset = -1;
    private JEditBuffer _currentBuffer;
    private JEditBuffer _lastEditedBuffer;
    private EditPane _editPane;
}
