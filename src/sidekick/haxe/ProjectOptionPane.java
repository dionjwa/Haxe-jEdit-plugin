package sidekick.haxe;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.RolloverButton;

import projectviewer.ProjectManager;
import projectviewer.config.OptionsService;
import projectviewer.vpt.VPTProject;
import ctagsinterface.index.TagIndex;
import ctagsinterface.main.CtagsInterfacePlugin;
import ctagsinterface.projects.ProjectWatcher;

public class ProjectOptionPane extends AbstractOptionPane
{
    private static final String PROJECT_DEPENDENCY = "projectDependency";
    private static final String TREE_DEPENDENCY = "treeDependency";
    JList projects;
    JList trees;
    DefaultListModel projectsModel;
    DefaultListModel treesModel;
    VPTProject project;

    public ProjectOptionPane(VPTProject project)
    {
        super("haxe-projectpaths");
        this.project = project;
    }

    private interface DependencyAsker
    {
        String getDependency();
    }
    protected void _init()
    {
        projectsModel = getListModel(PROJECT_DEPENDENCY);
        projects = createList("Projects:", projectsModel, new DependencyAsker ()
        {
            public String getDependency()
            {
                return showProjectSelectionDialog();
            }
        });
        addSeparator();
        treesModel = getListModel(TREE_DEPENDENCY);
        trees = createList("Trees:", treesModel, new DependencyAsker () {
            public String getDependency() {
                return showSourceTreeSelectionDialog();
            }
        });
    }

    private void setListModel(String propertyName, DefaultListModel model)
    {
        Vector<String> list = new Vector<String>();
        for (int i = 0; i < model.size(); i++)
            list.add((String) model.getElementAt(i));
        setListProperty(propertyName, list);
    }
    private DefaultListModel getListModel(String propertyName)
    {
        Vector<String> list = getListProperty(propertyName);
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < list.size(); i++)
            model.addElement(list.get(i));
        return model;
    }

    private Vector<String> getListProperty(String propertyName)
    {
        return getListProperty(project, propertyName);
    }
    private void setListProperty(String propertyName, Vector<String> list)
    {
        for (int i = 0; i < list.size(); i++)
            project.setProperty(propertyName + i, list.get(i));
        for (int i = list.size(); true; i++)
        {
            String prop = propertyName + i;
            if (project.getProperty(prop) == null)
                break;
            project.removeProperty(prop);
        }
    }

    private JList createList(String title, final DefaultListModel model,
        final DependencyAsker da)
    {
        addComponent(new JLabel(title));
        final JList list = new JList(model);
        addComponent(new JScrollPane(list), GridBagConstraints.HORIZONTAL);
        JPanel buttons = new JPanel();
        JButton add = new RolloverButton(GUIUtilities.loadIcon("Plus.png"));
        add.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String s = da.getDependency();
                if (s != null)
                {
                    int index = list.getSelectedIndex();
                    model.add(index + 1, s);
                    list.setSelectedIndex(index + 1);
                }
            }
        });
        JButton remove = new RolloverButton(GUIUtilities.loadIcon("Minus.png"));
        remove.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int index = list.getSelectedIndex();
                if (index >= 0)
                {
                    model.removeElementAt(index);
                    if (index < model.size())
                        list.setSelectedIndex(index);
                    else if (! model.isEmpty())
                        list.setSelectedIndex(model.size() - 1);
                }
            }
        });
        buttons.add(add);
        buttons.add(remove);
        addComponent(buttons);
        return list;
    }

    private String showProjectSelectionDialog()
    {
        ProjectWatcher pw = CtagsInterfacePlugin.getProjectWatcher();
        if (pw == null)
        {
            JOptionPane.showMessageDialog(this, jEdit.getProperty(
                "messages.CtagsInterface.noPVSupport"));
            return null;
        }
        String project = pw.getActiveProject(jEdit.getActiveView());
        Vector<String> nameVec = pw.getProjects();
        nameVec.remove(project);
        String [] names = new String[nameVec.size()];
        nameVec.toArray(names);
        String selected = (String) JOptionPane.showInputDialog(this, "Select project:",
            "Projects", JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        return selected;
    }
    private String showSourceTreeSelectionDialog()
    {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select root of source tree");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = fc.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        String dir = fc.getSelectedFile().getAbsolutePath();
        return MiscUtilities.resolveSymlinks(dir);
    }
    protected void _save()
    {
        setListModel(PROJECT_DEPENDENCY, projectsModel);
        setListModel(TREE_DEPENDENCY, treesModel);
    }

    public static Vector<String> getListProperty(VPTProject project, String propertyName)
    {
        Vector<String> list = new Vector<String>();
        int i = 0;
        while (true)
        {
            String value = project.getProperty(propertyName + i);
            if (value == null)
                break;
            list.add(value);
            i++;
        }
        return list;
    }

    public static HashMap<String, Vector<String>> getDependencies(String projectName)
    {
        HashMap<String, Vector<String>> map = new HashMap<String, Vector<String>>();
        VPTProject project = ProjectManager.getInstance().getProject(projectName);
        if (project == null)
            return map;
        Vector<String> projectDeps = getListProperty(project, PROJECT_DEPENDENCY);
        map.put(TagIndex.OriginType.PROJECT.name, projectDeps);
        Vector<String> treeDeps = getListProperty(project, TREE_DEPENDENCY);
        map.put(TagIndex.OriginType.DIRECTORY.name, treeDeps);
        return map;
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
