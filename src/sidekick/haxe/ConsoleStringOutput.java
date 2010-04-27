package sidekick.haxe;

import java.awt.Color;

import javax.swing.text.AttributeSet;

import console.Output;

public class ConsoleStringOutput
    implements Output
{

    public void print (Color color, String msg)
    {
        _buf.append(msg);
        _buf.append('\n');
    }

    public void writeAttrs (AttributeSet attrs, String msg)
    {
        _buf.append(msg);
    }

    public void setAttrs (int length, AttributeSet attrs)
    {}

    public void commandDone ()
    {}

    public String getStringOutput ()
    {
        return _buf.toString();
    }

    private StringBuffer _buf = new StringBuffer();

}
