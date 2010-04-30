package sidekick.haxe;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{
    private JCheckBox buildOnSave;
    private JTextField compilerLocation;
    private JTextField launchCommand;

    public HaXeOptionPane ()
    {
        super("haxe");
    }

    public void _init ()
    {
        buildOnSave = new JCheckBox(jEdit.getProperty("options.haxe.buildOnSave.label"),
            jEdit.getBooleanProperty("options.haxe.buildOnSave", true));
        addComponent(buildOnSave);

        compilerLocation = new JTextField(jEdit.getProperty("options.haxe.compilerLocation"));
        addComponent(jEdit.getProperty("options.haxe.compilerLocation.label"), compilerLocation);

        launchCommand = new JTextField(jEdit.getProperty("options.haxe.launchCommand"));
        addComponent(jEdit.getProperty("options.haxe.launchCommand.label"), launchCommand);

        revalidate();
    }

    /**
     * Save he properties that have been set in the GUI
     */
    public void _save ()
    {
        jEdit.setProperty("options.haxe.compilerLocation", compilerLocation.getText());
        jEdit.setProperty("options.haxe.launchCommand", launchCommand.getText());
        jEdit.setBooleanProperty("options.haxe.buildOnSave", buildOnSave.isSelected());
    }

}
