package sidekick.haxe;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;

import projectviewer.config.OptionsService;
import projectviewer.vpt.VPTProject;

public class ProjectOptionPane extends AbstractOptionPane
{
    private static final String OPTION_PREFIX = "options.haxe.";
    public static final String PROJECT_HXML_FILE = "projectHXMLFile";
    public static final String PROJECT_STD_DIR = "projectStdDir";
    public static final String PROJECT_HAXE_EXECUTABLE = "projectHaxeExecutable";

    VPTProject project;

    private JTextField hxmlFile;
    private JTextField haxeStdDir;
    private JTextField haxeExecutable;

    public ProjectOptionPane(VPTProject project)
    {
        super("haxe-project-options");
        this.project = project;
    }

    @Override
    protected void _init()
    {
        addComponent(new JLabel("Advanced haXe options.  Leave empty for defaults."));
        addSeparator();
        hxmlFile = new JTextField(project.getProperty(PROJECT_HXML_FILE));
        addComponent(jEdit.getProperty(OPTION_PREFIX + PROJECT_HXML_FILE + ".label"), hxmlFile);

        addSeparator();
        haxeStdDir = new JTextField(project.getProperty(PROJECT_STD_DIR));
        addComponent(jEdit.getProperty(OPTION_PREFIX + PROJECT_STD_DIR + ".label"), haxeStdDir);

        addSeparator();
        haxeExecutable = new JTextField(project.getProperty(PROJECT_HAXE_EXECUTABLE));
        addComponent(jEdit.getProperty(OPTION_PREFIX + PROJECT_HAXE_EXECUTABLE + ".label"), haxeExecutable);
    }

    @Override
    protected void _save()
    {
        if (hxmlFile.getText().trim().length() > 0) {
            project.setProperty(PROJECT_HXML_FILE, hxmlFile.getText().trim());
        }
        if (haxeStdDir.getText().trim().length() > 0) {
            project.setProperty(PROJECT_STD_DIR, haxeStdDir.getText().trim());
        }
        if (haxeExecutable.getText().trim().length() > 0) {
            project.setProperty(PROJECT_HAXE_EXECUTABLE, haxeExecutable.getText().trim());
        }
    }


    public static class ProjectOptionService
        implements OptionsService
    {
        public OptionGroup getOptionGroup(VPTProject proj)
        {
            return null;
        }

        public OptionPane getOptionPane(VPTProject proj)
        {
            if (HaXeSideKickPlugin.getFirstProjectHXMLFile(proj) != null) {
                return new ProjectOptionPane(proj);
            } else {
                return null;
            }
        }
    }
}
