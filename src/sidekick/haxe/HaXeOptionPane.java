package sidekick.haxe;

import javax.swing.JTextField;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class HaXeOptionPane extends AbstractOptionPane
{
//    private JCheckBox buildOnSave;
//    private JTextField compilerLocationMac;
//    private JTextField compilerLocationLinux;
//    private JTextField compilerLocationWindows;
    private JTextField launchCommand;

    public HaXeOptionPane ()
    {
        super("haxe");
    }

    public void _init ()
    {
//        buildOnSave = new JCheckBox(jEdit.getProperty("options.haxe.buildOnSave.label"),
//            jEdit.getBooleanProperty("options.haxe.buildOnSave", true));
//        addComponent(buildOnSave);

//	addComponent(new JLabel(jEdit.getProperty("options.haxe.compilerLocation.label")));
//        compilerLocationMac = new JTextField(jEdit.getProperty("options.haxe.compilerLocationMac"));
//        addComponent("          " + jEdit.getProperty("options.haxe.compilerLocationMac.label"), compilerLocationMac);
//
//        compilerLocationLinux = new JTextField(jEdit.getProperty("options.haxe.compilerLocationLinux"));
//        addComponent("          " + jEdit.getProperty("options.haxe.compilerLocationLinux.label"), compilerLocationLinux);
//
//        compilerLocationWindows = new JTextField(jEdit.getProperty("options.haxe.compilerLocationWindows"));
//        addComponent("          " + jEdit.getProperty("options.haxe.compilerLocationWindows.label"), compilerLocationWindows);

        launchCommand = new JTextField(jEdit.getProperty("options.haxe.launchCommand"));
        addComponent(jEdit.getProperty("options.haxe.launchCommand.label"), launchCommand);

        revalidate();
    }

    /**
     * Save he properties that have been set in the GUI
     */
    public void _save ()
    {
//        jEdit.setProperty("options.haxe.compilerLocationMac", compilerLocationMac.getText());
//        jEdit.setProperty("options.haxe.compilerLocationLinux", compilerLocationLinux.getText());
//        jEdit.setProperty("options.haxe.compilerLocationWindows", compilerLocationWindows.getText());
        jEdit.setProperty("options.haxe.launchCommand", launchCommand.getText());
//        jEdit.setBooleanProperty("options.haxe.buildOnSave", buildOnSave.isSelected());
    }

}
