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
    public static final String PROJECT_LAUNCH_CMD = "projectLaunchCommand";
    public static final String PROJECT_BUILD_CMD = "projectBuildCommand";

    VPTProject project;

    private JTextField launchCommand;
    private JTextField buildCommand;

    public ProjectOptionPane(VPTProject project)
    {
        super("haxe-project-options");
        this.project = project;
    }

    @Override
    protected void _init()
    {
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_BUILD_CMD + ".label")));
        buildCommand = new JTextField(project.getProperty(PROJECT_BUILD_CMD));
        addComponent("", buildCommand);
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_LAUNCH_CMD + ".label")));
        launchCommand = new JTextField(project.getProperty(PROJECT_LAUNCH_CMD));
        addComponent("", launchCommand);
    }

    @Override
    protected void _save()
    {
        project.setProperty(PROJECT_LAUNCH_CMD, launchCommand.getText().trim());
        project.setProperty(PROJECT_BUILD_CMD, buildCommand.getText().trim());
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
            return new ProjectOptionPane(proj);
        }
    }
}
