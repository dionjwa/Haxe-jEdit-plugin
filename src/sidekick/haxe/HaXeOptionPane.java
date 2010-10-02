package sidekick.haxe;

import java.io.File;

import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{
//    private JCheckBox buildOnSave;
    private JTextField launchCommand;
    private JTextField installDir;

    private JTextField haxelibDir;
    private JTextField stdDir;

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
        installDirTxt = installDirTxt == null || installDirTxt.trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() : installDirTxt;
        installDir = new JTextField(installDirTxt);
        addComponent(jEdit.getProperty("options.haxe.installDir.label"), installDir);

        String haxelibDirTxt = jEdit.getProperty("options.haxe.haxelibDir");
        haxelibDirTxt = haxelibDirTxt == null || haxelibDirTxt.trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "haxelib" : haxelibDirTxt;
        haxelibDir = new JTextField(haxelibDirTxt);
        addComponent(jEdit.getProperty("options.haxe.haxelibDir.label"), haxelibDir);

        String stdDirTxt = jEdit.getProperty("options.haxe.stdDir");
        stdDirTxt = stdDirTxt == null || stdDirTxt.trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "std" : stdDirTxt;
        stdDir = new JTextField(stdDirTxt);
        addComponent(jEdit.getProperty("options.haxe.stdDir.label"), stdDir);

        revalidate();
    }

    /**
     * Save he properties that have been set in the GUI
     */
    @Override
    public void _save ()
    {
        jEdit.setProperty("options.haxe.launchCommand", launchCommand.getText());
        jEdit.setProperty("options.haxe.installDir", installDir.getText() == null || installDir.getText().trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() : installDir.getText());

        jEdit.setProperty("options.haxe.stdDir", stdDir.getText() == null || stdDir.getText().trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "std" : stdDir.getText());
        jEdit.setProperty("options.haxe.haxelibDir", haxelibDir.getText() == null || haxelibDir.getText().trim().equals("") ? HaXeSideKickPlugin.getSystemDefaultHaxeInstallPath() + File.separator + "haxelib" : haxelibDir.getText());

//        jEdit.setBooleanProperty("options.haxe.buildOnSave", buildOnSave.isSelected());
    }

}
