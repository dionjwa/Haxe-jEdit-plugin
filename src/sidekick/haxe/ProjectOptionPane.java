package sidekick.haxe;

import java.awt.Dimension;

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
    public static final String PROJECT_PRE_BUILD_CMD = "projectPreBuildCommand";
    public static final String PROJECT_CURRENT_BUILD_CMD = "projectCurrentBuildCommand";

    VPTProject project;

    private JTextField launchCommand;
    private JTextField preBuildCommand;
    private JTextField buildCommand;

    public ProjectOptionPane(VPTProject project)
    {
        super("haxe-project-options");
        this.project = project;
    }

    @Override
    protected void _init()
    {
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_PRE_BUILD_CMD + ".label")));
        buildCommand = new JTextField(project.getProperty(PROJECT_PRE_BUILD_CMD));
        addComponent("", buildCommand);
        addSeparator();
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_BUILD_CMD + ".label")));
        buildCommand = new JTextField(project.getProperty(PROJECT_BUILD_CMD));
        addComponent("", buildCommand);
        addSeparator();
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_CURRENT_BUILD_CMD + ".label")));
        JTextField command = new JTextField(HaXeSideKickPlugin.getBuildCommand());
        command.setMinimumSize(new Dimension(500, 300));
        command.setMaximumSize(new Dimension(500, 1000));
        command.setEditable(false);
        addComponent(command);
        addSeparator();
        addComponent(new JLabel(jEdit.getProperty(OPTION_PREFIX + PROJECT_LAUNCH_CMD + ".label")));
        launchCommand = new JTextField(project.getProperty(PROJECT_LAUNCH_CMD));
        addComponent("", launchCommand);
    }

    @Override
    protected void _save()
    {
        project.setProperty(PROJECT_LAUNCH_CMD, launchCommand.getText().trim());
        project.setProperty(PROJECT_BUILD_CMD, buildCommand.getText().trim());
        project.setProperty(PROJECT_PRE_BUILD_CMD, preBuildCommand.getText().trim());
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
