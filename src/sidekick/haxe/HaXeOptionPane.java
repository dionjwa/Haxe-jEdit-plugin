package sidekick.haxe;

import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{
//    private JCheckBox buildOnSave;
    private JTextField launchCommand;
    private JTextField installDir;

    public HaXeOptionPane ()
    {
        super("haxe");
    }

    @Override
    public void _init ()
    {
//        buildOnSave = new JCheckBox(jEdit.getProperty("options.haxe.buildOnSave.label"),
//            jEdit.getBooleanProperty("options.haxe.buildOnSave", true));
//        addComponent(buildOnSave);

        launchCommand = new JTextField(jEdit.getProperty("options.haxe.launchCommand"));
        addComponent(jEdit.getProperty("options.haxe.launchCommand.label"), launchCommand);

        String installDirTxt = jEdit.getProperty("options.haxe.installDir");
        installDirTxt = installDirTxt == null || installDirTxt.trim().equals("") ? "System Default" : installDirTxt;
        installDir = new JTextField(installDirTxt);
        addComponent(jEdit.getProperty("options.haxe.installDir.label"), installDir);

        revalidate();
    }

    /**
     * Save he properties that have been set in the GUI
     */
    @Override
    public void _save ()
    {
        jEdit.setProperty("options.haxe.launchCommand", launchCommand.getText());
        jEdit.setProperty("options.haxe.installDir", installDir.getText() == null || installDir.getText().trim().equals("") ? "System Default" : installDir.getText());
//        jEdit.setBooleanProperty("options.haxe.buildOnSave", buildOnSave.isSelected());
    }

}
