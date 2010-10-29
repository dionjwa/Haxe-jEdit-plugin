package sidekick.haxe;

import java.awt.GridBagConstraints;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{
    public static final String HAXE_LIBRARY_PATH = "options.haxe.stdDir";

    private JTextField stdDir;

    public HaXeOptionPane ()
    {
        super("haxe");
    }

    @Override
    public void _init ()
    {
        String stdDirTxt = jEdit.getProperty(HAXE_LIBRARY_PATH);
        stdDirTxt = stdDirTxt == null || stdDirTxt.trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "std" : stdDirTxt;
        stdDir = new JTextField(stdDirTxt);
        addComponent(new JLabel(jEdit.getProperty(HAXE_LIBRARY_PATH + ".label")));
        addComponent(stdDir, GridBagConstraints.HORIZONTAL);

        revalidate();
    }

    /**
     * Save he properties that have been set in the GUI
     */
    @Override
    public void _save ()
    {
        jEdit.setProperty(HAXE_LIBRARY_PATH, stdDir.getText() == null || stdDir.getText().trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "std" : stdDir.getText());
    }
}
