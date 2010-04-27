package sidekick.haxe;

import java.awt.Color;

import javax.swing.text.AttributeSet;

import console.Output;

public class NullConsoleOutput
    implements Output
{
    public void commandDone ()
    {}

    public void print (Color color, String msg)
    {}

    public void setAttrs (int length, AttributeSet attrs)
    {}

    public void writeAttrs (AttributeSet attrs, String msg)
    {}

    public static NullConsoleOutput NULL = new NullConsoleOutput();

}
