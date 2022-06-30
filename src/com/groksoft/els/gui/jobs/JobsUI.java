package com.groksoft.els.gui.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.repository.Repositories;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.gui.util.RotatedIcon;
import com.groksoft.els.gui.util.TextIcon;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class JobsUI extends JDialog
{
    private ConfigModel configModel;
    private Job currentJob = null;
    private Task currentTask = null;
    private ArrayList<Job> deletedJobs;
    private GuiContext guiContext;
    private boolean loadingCombo = false;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private SwingWorker<Void, Void> worker;
    private boolean workerRunning = false;

    public JobsUI(Window owner, GuiContext guiContext)
    {
        super(owner);
        this.guiContext = guiContext;
        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // Rotate up/down button labels
        // http://www.camick.com/java/source/TextIcon.java
        TextIcon t1 = new TextIcon(buttonTaskUp, ">", TextIcon.Layout.HORIZONTAL);
        buttonTaskUp.setText("");
        // http://www.camick.com/java/source/RotatedIcon.java
        RotatedIcon r1 = new RotatedIcon(t1, RotatedIcon.Rotate.UP);
        buttonTaskUp.setIcon(r1);
        //
        t1 = new TextIcon(buttonTaskDown, ">", TextIcon.Layout.HORIZONTAL);
        buttonTaskDown.setText("");
        r1 = new RotatedIcon(t1, RotatedIcon.Rotate.DOWN);
        buttonTaskDown.setIcon(r1);
        //
        t1 = new TextIcon(buttonOriginUp, ">", TextIcon.Layout.HORIZONTAL);
        buttonOriginUp.setText("");
        r1 = new RotatedIcon(t1, RotatedIcon.Rotate.UP);
        buttonOriginUp.setIcon(r1);
        //
        t1 = new TextIcon(buttonOriginDown, ">", TextIcon.Layout.HORIZONTAL);
        buttonOriginDown.setText("");
        r1 = new RotatedIcon(t1, RotatedIcon.Rotate.DOWN);
        buttonOriginDown.setIcon(r1);

        // position, size & dividers
        if (guiContext.preferences.getJobsYpos() > -1)
            this.setLocation(guiContext.preferences.getJobsXpos(), guiContext.preferences.getJobsYpos());
        //
        if (guiContext.preferences.getJobsHeight() > -1)
        {
            Dimension dim = new Dimension(guiContext.preferences.getJobsWidth(), guiContext.preferences.getJobsHeight());
            this.setSize(dim);
        }
        //
        if (guiContext.preferences.getJobsTaskDividerLocation() > -1)
            this.splitPaneContent.setDividerLocation(guiContext.preferences.getJobsTaskDividerLocation());
        //
        if (guiContext.preferences.getJobsOriginDividerLocation() > -1)
            this.splitPaneToolsOrigin.setDividerLocation(guiContext.preferences.getJobsOriginDividerLocation());

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // setup the left-side list of configurations
        configModel = new ConfigModel(guiContext, this);
        configModel.setColumnCount(1);
        configItems.setModel(configModel);
        configItems.getTableHeader().setUI(null);
        //
        ListSelectionModel lsm = configItems.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!listSelectionEvent.getValueIsAdjusting())
                {
                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    loadTasks(index);
                }
            }
        });

        // setup the publisher/subscriber Task Origins table
        Border border = guiContext.mainFrame.textFieldLocation.getBorder();
        panelPubSub.setBorder(border);
        tablePubSub.getTableHeader().setUI(null);
        tablePubSub.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent mouseEvent)
            {
                //super.mouseClicked(mouseEvent);
                int row = mouseEvent.getY() / tablePubSub.getRowHeight();
                int col = tablePubSub.getColumnModel().getColumnIndexAtX(mouseEvent.getX());

                if (row < tablePubSub.getRowCount() && row >= 0 &&
                        col < tablePubSub.getColumnCount() && col >= 0)
                {
                    Object object = tablePubSub.getValueAt(row, col);
                    if (object instanceof JButton)
                        ((JButton) object).doClick();
                }
            }
        });

        loadConfigurations();
        deletedJobs = new ArrayList<Job>();

    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && currentJob != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("JobsUI.stop.currently.running.job"),
                    guiContext.cfg.gs("Z.cancel.run"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                currentJob.requestStop();
                guiContext.browser.printLog(java.text.MessageFormat.format(guiContext.cfg.gs("Job.config.cancelled"), currentJob.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.cancel.all.changes"),
                        "Z.cancel.changes", JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                    setVisible(false);
            }
            else
                setVisible(false);
        }
    }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job origJob = (Job) configModel.getValueAt(index, 0);
            String rename = origJob.getConfigName() + guiContext.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                Job job = origJob.clone();
                job.setConfigName(rename);
                job.setDataHasChanged();
                configModel.addRow(new Object[]{job});
                loadTasks(configModel.getRowCount() - 1);

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                        rename, guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job job = (Job) configModel.getValueAt(index, 0);

            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + job.getConfigName(),
                    guiContext.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                deletedJobs.add(job);
                configModel.removeRow(index);
                configModel.fireTableDataChanged();
                if (index > configModel.getRowCount() - 1)
                    index = configModel.getRowCount() - 1;
                configItems.requestFocus();
                if (index >= 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadTasks(index);
                }
            }
        }
    }

    private void actionGenerateClicked(ActionEvent evt)
    {
        try
        {
            // TODO change when JRE is embedded in ELS distro
            String jarPath = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            String jar = jarPath;
            String generated = "java -jar " + jar + " -c debug -d debug -j \"" + currentJob.getConfigName() + "\" -F \"" + currentJob.getConfigName() + ".log\"";

            Object obj = JOptionPane.showInputDialog(this,
                    guiContext.cfg.gs("JobsUI.generated") + currentJob.getConfigName(),
                    guiContext.cfg.gs("JobsUI.title"), JOptionPane.INFORMATION_MESSAGE,
                    null, null, generated);
        }
        catch (Exception e)
        {
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("JobsUI.help"), "jobs_" + guiContext.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            // offset the help dialog from the parent dialog
            Point loc = this.getLocation();
            loc.x = loc.x + 32;
            loc.y = loc.y + 32;
            helpDialog.setLocation(loc);
            helpDialog.setVisible(true);
        }
        else
        {
            helpDialog.toFront();
        }
    }

    private void actionNewClicked(ActionEvent evt)
    {
        if (configModel.find(guiContext.cfg.gs("Z.untitled"), null) == null)
        {
            Job job = new Job(guiContext.cfg.gs("Z.untitled"));
            configModel.addRow(new Object[]{job});
            this.currentJob = job;
            loadTasks(-1);

            if (configModel.getRowCount() == 0)
            {
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
                buttonRun.setEnabled(true);
                buttonTaskUp.setEnabled(true);
                buttonTaskDown.setEnabled(true);
                buttonAddTask.setEnabled(true);
                buttonRemoveTask.setEnabled(true);
                buttonAddOrigin.setEnabled(true);
                buttonRemoveOrigin.setEnabled(true);
            }

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                    guiContext.cfg.gs("Z.untitled"), guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        saveConfigurations();
        savePreferences();
        setVisible(false);
    }

    private void actionOriginAddClicked(ActionEvent evt)
    {
        if (currentTask != null)
        {
            ArrayList<Origin> origins = new ArrayList<Origin>();
            // TODO match pub/sub combo for source ;; OR show warning when selection does not match
            boolean isSubscriber = Origin.makeOriginsFromSelected(this, origins); // can return null
            if (origins != null && origins.size() > 0)
            {
                listOrigins.requestFocus();
                int count = origins.size();

                // make dialog pieces
                String which = (isSubscriber) ? guiContext.cfg.gs("Z.subscriber") : guiContext.cfg.gs("Z.publisher");
                String message = java.text.MessageFormat.format(guiContext.cfg.gs("JobsUI.add.N.origins"), count, which);

                // confirm adds
                int reply = JOptionPane.showConfirmDialog(this, message, guiContext.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    currentTask.addOrigins(origins);
                    currentJob.setDataHasChanged();
                    loadOrigins(currentTask);
                }
            }
        }
    }

    private void actionOriginDownClicked(ActionEvent e)
    {
        int indices[] = listOrigins.getSelectedIndices();
        if (indices.length == 1)
        {
            int index = indices[0];
            ArrayList<Origin> orgs = currentTask.getOrigins();
            if (index < orgs.size() - 1)
            {
                listOrigins.requestFocus();

                Origin o1 = orgs.get(index).clone();
                Origin o2 = orgs.get(index + 1).clone();
                orgs.set(index + 1, o1);
                orgs.set(index, o2);
                currentTask.setOrigins(orgs);
                currentJob.setDataHasChanged();
                loadOrigins(currentTask);
                listOrigins.setSelectedIndex(index + 1);
            }
        }
    }

    private void actionOriginRemoveClicked(ActionEvent e)
    {
        if (currentTask != null)
        {
            int indices[] = listOrigins.getSelectedIndices();
            if (indices.length > 0)
            {
                listOrigins.requestFocus();

                int count = indices.length;

                // make dialog pieces
                String message = java.text.MessageFormat.format(guiContext.cfg.gs("JobsUI.remove.N.origins"), count);

                // confirm deletions
                int reply = JOptionPane.showConfirmDialog(this, message, guiContext.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    Arrays.sort(indices);
                    // remove in reverse sorted order so indices do not change
                    ArrayList<Origin> orgs = currentTask.getOrigins();
                    for (int i = indices.length - 1; i >= 0; --i)
                    {
                        orgs.remove(indices[i]);
                    }
                    currentTask.setOrigins(orgs);
                    currentJob.setDataHasChanged();
                    loadOrigins(currentTask);
                }
            }
        }
    }

    private void actionOriginUpClicked(ActionEvent e)
    {
        int indices[] = listOrigins.getSelectedIndices();
        if (indices.length == 1)
        {
            int index = indices[0];
            ArrayList<Origin> orgs = currentTask.getOrigins();
            if (index > 0)
            {
                listOrigins.requestFocus();

                Origin o1 = orgs.get(index).clone();
                Origin o2 = orgs.get(index - 1).clone();
                orgs.set(index - 1, o1);
                orgs.set(index, o2);
                currentTask.setOrigins(orgs);
                currentJob.setDataHasChanged();
                loadOrigins(currentTask);
                listOrigins.setSelectedIndex(index - 1);
            }
        }
    }

    public void actionPubSubClicked(ActionEvent evt)
    {
        if (currentTask != null)
        {
            // determine whether it is publisher, subscriber, or both
            int which = -1;
            if (!currentTask.isDual())
                which = 99; // both
            else
            {
                String command = evt.getActionCommand();
                if (command.equals("0"))
                    which = 0; // publisher
                else if (command.equals("1"))
                    which = 1; // subscriber
            }
            if (which < 0)
                return; // should never happen

            JComboBox combo = new JComboBox();
            JList<String> list = new JList<>();
            int id = 0;
            DefaultListModel<String> listModel = new DefaultListModel<String>();
            int selectedCombo = -1;
            int selectedList = -1;
            String text = null;
            String title = null;
            String tip = null;
            Repositories repositories = getRepositories();

            // make dialog pieces
            title = which == 0 ? guiContext.cfg.gs("JobsUI.pubsub.select.publisher") : guiContext.cfg.gs("JobsUI.pubsub.select.subscriber");

            if (which == 0 || which == 99) // publisher or both
            {
                title = guiContext.cfg.gs("JobsUI.pubsub.select.publisher");
                tip = guiContext.cfg.gs("JobsUI.select.publisher.tooltip");

                combo.addItem(new ComboItem(id++, guiContext.cfg.gs("JobsUI.any.publisher")));
                selectedCombo = id - 1;

                text = guiContext.cfg.gs("JobsUI.publisher.specific");
                if (currentTask.getPublisherKey().length() > 0)
                {
                    selectedCombo = id - 1;
                    if (!currentTask.getPublisherKey().equals(Task.ANY_SERVER))
                    {
                        selectedCombo = id;
                        Repositories.Meta meta = repositories.find(currentTask.getPublisherKey());
                        //text = guiContext.cfg.gs("JobsUI.publisher.specific");
                        if (meta != null)
                            selectedList = repositories.indexOf(currentTask.getPublisherKey());
                    }
                }
                combo.addItem(new ComboItem(id++, text));
            }

            if (which == 1 || which == 99) // subscriber or both
            {
                title = guiContext.cfg.gs("JobsUI.pubsub.select.subscriber");
                tip = guiContext.cfg.gs("JobsUI.select.subscriber.tooltip");

                text = guiContext.cfg.gs("JobsUI.any.subscriber");
                combo.addItem(new ComboItem(id++, text));
                if (which == 1)
                    selectedCombo = id - 1;

                text = guiContext.cfg.gs("JobsUI.subscriber.local");
                combo.addItem(new ComboItem(id++, text));

                text = guiContext.cfg.gs("JobsUI.subscriber.remote");
                combo.addItem(new ComboItem(id++, text));

                if (currentTask.getSubscriberKey().length() > 0)
                {
                    selectedCombo = id - 3;
                    if (!currentTask.getSubscriberKey().equals(Task.ANY_SERVER))
                    {
                        selectedCombo = id - 2;
                        Repositories.Meta meta = repositories.find(currentTask.getSubscriberKey());
                        if (meta != null)
                        {
                            if (currentTask.isSubscriberRemote())
                                selectedCombo = id -1;
                            selectedList = repositories.indexOf(currentTask.getSubscriberKey());
                        }
                    }
                }
            }

            if (which == 99)
            {
                title = guiContext.cfg.gs("JobsUI.pubsub.select.publisher.or.subscriber");
                tip = guiContext.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
            }

            if (selectedCombo < 0)
                selectedCombo = 0;

            combo.setToolTipText(tip);

            combo.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String cmd = actionEvent.getActionCommand();
                    if (cmd.equals("comboBoxChanged"))
                    {
                        int sel = combo.getSelectedIndex();
                        if (sel == 0 || (combo.getModel().getSize() > 3 && sel == 2))
                            list.setEnabled(false);
                        else
                        {
                            list.setEnabled(true);
                            int s = list.getSelectedIndex();
                            if (s < 0)
                                list.setSelectedIndex(0);
                        }
                    }
                }
            });
            combo.setSelectedIndex(selectedCombo);

            for (int i = 0; i < repositories.getList().size(); ++i)
            {
                text = (repositories.getList().get(i)).description;
                listModel.addElement(text);
            }
            list.setModel(listModel);
            if (selectedList >= 0)
                list.setSelectedIndex(selectedList);

            JScrollPane pane = new JScrollPane();
            pane.setViewportView(list);
            list.requestFocus();
            Object[] params = {combo, pane};

            int opt = JOptionPane.showConfirmDialog(this, params, title, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                int selected = combo.getSelectedIndex();
                String key;
                if (selected == 0 || (which == 99 && selected == 2))
                {
                    key = Task.ANY_SERVER;
                }
                else
                {
                    int index = list.getSelectedIndex();
                    if (index < 0)
                    {
                        String msg = (which == 0 ? guiContext.cfg.gs("JobsUI.pubsub.select.publisher") :
                                (which == 1 ? guiContext.cfg.gs("JobsUI.pubsub.select.subscriber") :
                                        guiContext.cfg.gs("JobsUI.pubsub.select.publisher.or.subscriber")));
                        JOptionPane.showMessageDialog(this, msg,
                                guiContext.cfg.gs("JobsUI.title"), JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    key = (repositories.getList().get(index)).key;
                    if (selected == 2 || selected == 4) // remote subscriber
                    {
                        if (!currentTask.isSubscriberRemote())
                        {
                            currentTask.setSubscriberRemote(true);
                            currentJob.setDataHasChanged();
                        }
                    }
                    else // anything else is not remote
                    {
                        if (currentTask.isSubscriberRemote())
                        {
                            currentTask.setSubscriberRemote(false);
                            currentJob.setDataHasChanged();
                        }
                    }
                }

                if (which == 0)
                {
                    if (!currentTask.getPublisherKey().equals(key))
                    {
                        currentTask.setPublisherKey(key);
                        currentJob.setDataHasChanged();
                    }
                }
                else if (which == 1)
                {
                    if (!currentTask.getSubscriberKey().equals(key))
                    {
                        currentTask.setSubscriberKey(key);
                        currentJob.setDataHasChanged();
                    }
                }
                else // both
                {
                    if (selected <= 1) // a publisher selection
                    {
                        if (!currentTask.getPublisherKey().equals(key))
                        {
                            currentTask.setPublisherKey(key);
                            currentTask.setSubscriberKey("");
                            currentJob.setDataHasChanged();
                        }
                    }
                    else // a subscriber selection
                    {
                        if (!currentTask.getSubscriberKey().equals(key))
                        {
                            currentTask.setSubscriberKey(key);
                            currentTask.setPublisherKey("");
                            currentJob.setDataHasChanged();
                        }
                    }
                }
                loadPubSubs(currentTask);
            }
        }
    }

    private void actionRunClicked(ActionEvent evt)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job job = (Job) configModel.getValueAt(index, 0);
            String status = job.validate(guiContext.cfg);
            if (status.length() == 0)
            {
                // make dialog pieces
                String message = java.text.MessageFormat.format(guiContext.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
                JCheckBox checkbox = new JCheckBox(guiContext.cfg.gs("Navigator.dryrun"));
                checkbox.setToolTipText(guiContext.cfg.gs("Navigator.dryrun.tooltip"));
                Object[] params = {message, checkbox};

                // confirm run of job
                int reply = JOptionPane.showConfirmDialog(this, params, guiContext.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                isDryRun = checkbox.isSelected();
                if (reply == JOptionPane.YES_OPTION)
                {
                    process(job, isDryRun);
                }
            }
            else
            {
                JOptionPane.showMessageDialog(this, status,
                        guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionTaskAddClicked(ActionEvent evt)
    {
        if (currentJob != null)
        {
            listTasks.requestFocus();

            // make dialog pieces
            String message = guiContext.cfg.gs("JobsUI.select.tool");
            JList<String> toolJList = new JList<String>();
            ArrayList<AbstractTool> toolList = null;

            try
            {
                Tools toolsHandler = new Tools();
                toolList = toolsHandler.loadAllTools(guiContext, guiContext.cfg, guiContext.context, null);
                DefaultListModel<String> dialogList = new DefaultListModel<String>();
                for (AbstractTool tool : toolList)
                {
                    dialogList.addElement(tool.getDisplayName() + ": " + tool.getConfigName());
                }
                toolJList.setModel(dialogList);
                toolJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                toolJList.setSelectedIndex(0);
            }
            catch (Exception e)
            {
                String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                guiContext.browser.printLog(msg);
                JOptionPane.showMessageDialog(this, msg,
                        guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }

            JScrollPane pane = new JScrollPane();
            pane.setViewportView(toolJList);
            toolJList.requestFocus();
            Object[] params = {message, pane};

            int opt = JOptionPane.showConfirmDialog(this, params, guiContext.cfg.gs("JobsUI.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                int index = toolJList.getSelectedIndex();
                AbstractTool tool = toolList.get(index);
                currentTask = new Task(tool.getInternalName(), tool.getConfigName());
                currentTask.setDual(tool.isDualRepositories());
                currentJob.getTasks().add(currentTask);
                currentJob.setDataHasChanged();
                loadTasks(-1);
                listTasks.setSelectedIndex(currentJob.getTasks().size() - 1);
                loadOrigins(currentTask);
            }
            else
                listTasks.requestFocus();
        }
    }

    private void actionTaskDownClicked(ActionEvent e)
    {
        int indices[] = listTasks.getSelectedIndices();
        if (indices.length == 1)
        {
            int index = indices[0];
            ArrayList<Task> orgs = currentJob.getTasks();
            if (index < orgs.size() - 1)
            {
                listTasks.requestFocus();

                Task o1 = orgs.get(index).clone();
                Task o2 = orgs.get(index + 1).clone();
                orgs.set(index + 1, o1);
                orgs.set(index, o2);
                currentJob.setTasks(orgs);
                currentJob.setDataHasChanged();
                loadTasks(-1);
                listTasks.setSelectedIndex(index + 1);
            }
        }
    }

    private void actionTaskRemoveClicked(ActionEvent e)
    {
        if (currentJob != null)
        {
            int indices[] = listTasks.getSelectedIndices();
            if (indices.length > 0)
            {
                listTasks.requestFocus();
                int count = indices.length;

                // make dialog
                String message = java.text.MessageFormat.format(guiContext.cfg.gs("JobsUI.remove.N.tasks"), count);

                // confirm deletions
                int reply = JOptionPane.showConfirmDialog(this, message, guiContext.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    Arrays.sort(indices);
                    // remove in reverse sorted order so indices do not change
                    ArrayList<Task> orgs = currentJob.getTasks();
                    for (int i = indices.length - 1; i >= 0; --i)
                    {
                        orgs.remove(indices[i]);
                    }
                    currentJob.setTasks(orgs);
                    currentJob.setDataHasChanged();
                    loadTasks(-1);
                }
            }
        }
    }

    private void actionTaskUpClicked(ActionEvent e)
    {
        int indices[] = listTasks.getSelectedIndices();
        if (indices.length == 1)
        {
            int index = indices[0];
            ArrayList<Task> orgs = currentJob.getTasks();
            if (index > 0)
            {
                listTasks.requestFocus();

                Task o1 = orgs.get(index).clone();
                Task o2 = orgs.get(index - 1).clone();
                orgs.set(index - 1, o1);
                orgs.set(index, o2);
                currentJob.setTasks(orgs);
                currentJob.setDataHasChanged();
                loadTasks(-1);
                listTasks.setSelectedIndex(index - 1);
            }
        }
    }

    private boolean checkForChanges()
    {
        if (deletedJobs.size() > 0)
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((Job) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    private void configItemsMouseClicked(MouseEvent e)
    {
        JTable src = (JTable) e.getSource();
        if (e.getClickCount() == 1)
        {
            int jobIndex = src.getSelectedRow();
            if (jobIndex >= 0)
                loadTasks(jobIndex);
        }
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    public ArrayList<Job> getDeletedJobs()
    {
        return deletedJobs;
    }

    public String getFullPath(Job job)
    {
        String path = Job.getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(job.getConfigName()) + ".json";
        return path;
    }

    public Repositories getRepositories()
    {
        Repositories repositories = null;
        try
        {
            repositories = new Repositories();
            repositories.loadList(guiContext.cfg);
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            guiContext.browser.printLog(msg);
            JOptionPane.showMessageDialog(this, msg,
                    guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }
        return repositories;
    }

    public String getOriginType(int type)
    {
        switch (type)
        {
            case NavTreeUserObject.BOOKMARKS:
                return guiContext.cfg.gs("NavTreeNode.bookmark");
            case NavTreeUserObject.COLLECTION:
                return guiContext.cfg.gs("NavTreeNode.collection");
            case NavTreeUserObject.COMPUTER:
                return guiContext.cfg.gs("NavTreeNode.computer");
            case NavTreeUserObject.DRIVE:
                return guiContext.cfg.gs("NavTreeNode.drive");
            case NavTreeUserObject.HOME:
                return guiContext.cfg.gs("NavTreeNode.home");
            case NavTreeUserObject.LIBRARY:
                return guiContext.cfg.gs("NavTreeNode.library");
            case NavTreeUserObject.REAL:
                return "";
            case NavTreeUserObject.SYSTEM:
                return guiContext.cfg.gs("NavTreeNode.system");
            default:
                return guiContext.cfg.gs("NavTreeNode.unknown");
        }

    }

    private void listTasksMouseClicked(MouseEvent e)
    {
        JList src = (JList) e.getSource();
        if (e.getClickCount() == 1)
        {
            int taskIndex = src.locationToIndex(e.getPoint());
            if (taskIndex >= 0)
                loadOrigins(currentJob.getTasks().get(taskIndex));
        }
    }

    private void listTasksValueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            int taskIndex = listTasks.getSelectedIndex();
            if (taskIndex >= 0)
                loadOrigins(currentJob.getTasks().get(taskIndex));
        }
    }

    private void loadConfigurations()
    {
        File jobsDir = new File(Job.getDirectoryPath());
        if (jobsDir.exists())
        {
            Tools toolsHandler = new Tools();
            ArrayList<AbstractTool> toolList = null;
            try
            {
                toolList = toolsHandler.loadAllTools(guiContext, guiContext.cfg, guiContext.context, null);
            }
            catch (Exception e)
            {
                String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                guiContext.browser.printLog(msg);
                JOptionPane.showMessageDialog(this, msg,
                        guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }

            ArrayList<Job> jobsArray = new ArrayList<>();
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        Gson gson = new Gson();
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            Job job = gson.fromJson(json, Job.class);
                            if (job != null)
                            {
                                if (toolList != null)
                                {
                                    ArrayList<Task> tasks = job.getTasks();
                                    for (int i = 0; i < tasks.size(); ++i)
                                    {
                                        Task task = tasks.get(i);
                                        AbstractTool tool = toolsHandler.getTool(toolList, task.getInternalName(), task.getConfigName());
                                        if (tool != null)
                                        {
                                            task.setDual(tool.isDualRepositories());
                                            tasks.set(i, task);
                                        }
                                    }
                                }
                                jobsArray.add(job);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        String msg = guiContext.cfg.gs("Z.exception") + entry.getName() + " " + Utils.getStackTrace(e);
                        guiContext.browser.printLog(msg);
                        JOptionPane.showMessageDialog(this, msg,
                                guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            Collections.sort(jobsArray);
            for (Job job : jobsArray)
            {
                configModel.addRow(new Object[]{job});
            }
        }
        if (configModel.getRowCount() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonTaskUp.setEnabled(false);
            buttonTaskDown.setEnabled(false);
            buttonAddTask.setEnabled(false);
            buttonRemoveTask.setEnabled(false);
            buttonAddOrigin.setEnabled(false);
            buttonRemoveOrigin.setEnabled(false);
        }
        else
        {
            loadTasks(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadTasks(int jobIndex)
    {
        DefaultListModel<String> model = new DefaultListModel<String>();
        if (jobIndex < configModel.getRowCount())
        {
            if (jobIndex >= 0)
                currentJob = (Job) configModel.getValueAt(jobIndex, 0);

            if (currentJob.getTasks() != null && currentJob.getTasks().size() > 0)
            {
                for (int i = 0; i < currentJob.getTasks().size(); ++i)
                {
                    Task task = currentJob.getTasks().get(i);
                    String i18n = task.getInternalName() + ".displayName";
                    i18n = guiContext.cfg.gs(i18n);
                    if (i18n.length() == 0)
                        i18n = task.getInternalName();
                    String id = i18n + ": " + task.getConfigName();
                    model.addElement(id);
                }
                loadOrigins(currentJob.getTasks().get(0));
                listTasks.setSelectedIndex(0);
            }
        }

        if (currentJob.getTasks().size() == 0)
        {
            DefaultListModel<String> omodel = new DefaultListModel<String>();
            listOrigins.setModel(omodel);
            loadPubSubs(null);
        }

        listTasks.setModel(model);
    }

    private void loadOrigins(Task task)
    {
        currentTask = task;
        loadPubSubs(currentTask);

        DefaultListModel<String> model = new DefaultListModel<String>();
        if (currentTask.getOrigins().size() > 0)
        {
            for (int i = 0; i < currentTask.getOrigins().size(); ++i)
            {
                Origin origin = currentTask.getOrigins().get(i);
                String ot = getOriginType(origin.getType());
                String id = getOriginType(origin.getType()) + (ot.length() > 0 ? ": " : "") + origin.getName();
                model.addElement(id);
            }
            if (listOrigins.getModel().getSize() > 0)
                listOrigins.setSelectedIndex(0);
        }
        listOrigins.setModel(model);
    }

    private void loadPubSubs(Task task)
    {
        Repositories repositories = getRepositories();

        PubSubCellRenderer cellRenderer = new PubSubCellRenderer();
        PubSubModel pubSubModel = new PubSubModel(this, guiContext.cfg, repositories, task);
        tablePubSub.setModel(pubSubModel);

        TableColumn column = tablePubSub.getColumnModel().getColumn(0);
        column.setResizable(true);
        column = tablePubSub.getColumnModel().getColumn(1);
        column.setCellRenderer(cellRenderer);
        column.setResizable(false);
        column.setWidth(32);
        column.setPreferredWidth(32);
        column.setMaxWidth(32);
        column.setMinWidth(32);
    }

    private void process(Job job, boolean isDryRun)
    {
        ArrayList<Task> tasks = job.getTasks();
        if (tasks.size() > 0)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            setComponentEnabled(false);
            cancelButton.setEnabled(true);
            cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            labelHelp.setEnabled(true);
            labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            worker = job.process(guiContext, this, this.getTitle(), job, isDryRun);
            if (worker != null)
            {
                workerRunning = true;
                guiContext.navigator.disableGui(true);
                worker.addPropertyChangeListener(new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent e)
                    {
                        if (e.getPropertyName().equals("state"))
                        {
                            if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                processTerminated();
                        }
                    }
                });
                worker.execute();
            }
            else
                processTerminated();
        }
    }

    private void processTerminated()
    {
        if (guiContext.progress != null)
            guiContext.progress.done();

        guiContext.navigator.disableGui(false);
        setComponentEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;
    }

    private void saveConfigurations()
    {
        boolean changed = false;
        Job job = null;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                job = (Job) configModel.getValueAt(i, 0);
                if (job.isDataChanged())
                {
                    write(job);
                    changed = true;
                }
            }

            // remove any deleted jobs JSON configuration file
            for (int i = 0; i < deletedJobs.size(); ++i)
            {
                job = deletedJobs.get(i);
                File file = new File(job.getFullPath());
                if (file.exists())
                {
                    file.delete();
                    changed = true;
                }
            }

            if (changed)
                guiContext.navigator.loadJobsMenu();
        }
        catch (Exception e)
        {
            String name = (job != null) ? job.getConfigName() + " " : " ";
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            JOptionPane.showMessageDialog(this,
                    guiContext.cfg.gs("Z.error.writing") + name + e.getMessage(),
                    guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePreferences()
    {
        guiContext.preferences.setJobsHeight(this.getHeight());
        guiContext.preferences.setJobsWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setJobsXpos(location.x);
        guiContext.preferences.setJobsYpos(location.y);
        guiContext.preferences.setJobsTaskDividerLocation(splitPaneContent.getDividerLocation());
        guiContext.preferences.setJobsOriginDividerLocation(splitPaneToolsOrigin.getDividerLocation());
    }

    public void write(Job job) throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(job);
        try
        {
            File f = new File(getFullPath(job));
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath(job));
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(guiContext.cfg.gs("Z.error.writing") + getFullPath(job) + ": " + Utils.getStackTrace(fnf));
        }
    }

    public void setComponentEnabled(boolean enabled)
    {
        guiContext.navigator.setComponentEnabled(enabled, getContentPane());
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    // ================================================================================================================

    private class ComboItem
    {
        public int id;
        public String text;

        public ComboItem(int id, String text)
        {
            this.id = id;
            this.text = text;
        }

        @Override
        public String toString()
        {
            return text;
        }
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        hSpacerBeforeGenerate = new JPanel(null);
        buttonGenerate = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        configItems = new JTable();
        panelJob = new JPanel();
        splitPaneToolsOrigin = new JSplitPane();
        panelTasks = new JPanel();
        labelTasks = new JLabel();
        scrollPaneTasks = new JScrollPane();
        listTasks = new JList();
        panelOrigin = new JPanel();
        labelSpacer = new JLabel();
        labelOrigins = new JLabel();
        panelOriginInstance = new JPanel();
        panelPubSub = new JPanel();
        tablePubSub = new JTable();
        scrollPaneOrigins = new JScrollPane();
        listOrigins = new JList();
        panelOriginsButtons = new JPanel();
        buttonAddOrigin = new JButton();
        buttonOriginUp = new JButton();
        buttonOriginDown = new JButton();
        buttonRemoveOrigin = new JButton();
        panelToolButtons = new JPanel();
        buttonAddTask = new JButton();
        buttonTaskUp = new JButton();
        buttonTaskDown = new JButton();
        buttonRemoveTask = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("JobsUI.title"));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JobsUI.this.windowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setMinimumSize(new Dimension(140, 38));
                    panelTop.setPreferredSize(new Dimension(570, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("JobsUI.buttonNew.text"));
                        buttonNew.setMnemonic(guiContext.cfg.gs("JobsUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(guiContext.cfg.gs("JobsUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JobsUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("JobsUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("JobsUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JobsUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("JobsUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("JobsUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("JobsUI.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);

                        //---- hSpacerBeforeGenerate ----
                        hSpacerBeforeGenerate.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeGenerate.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeGenerate);

                        //---- buttonGenerate ----
                        buttonGenerate.setText(guiContext.cfg.gs("JobsUI.buttonGenerate.text"));
                        buttonGenerate.setMnemonic(guiContext.cfg.gs("JobsUI.buttonGenerate.mnemonic_2").charAt(0));
                        buttonGenerate.setToolTipText(guiContext.cfg.gs("JobsUI.buttonGenerate.toolTipText"));
                        buttonGenerate.addActionListener(e -> actionGenerateClicked(e));
                        panelTopButtons.add(buttonGenerate);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.WEST);

                    //======== panelHelp ========
                    {
                        panelHelp.setPreferredSize(new Dimension(40, 38));
                        panelHelp.setMinimumSize(new Dimension(0, 38));
                        panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                        //---- labelHelp ----
                        labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelHelp.setPreferredSize(new Dimension(32, 30));
                        labelHelp.setMinimumSize(new Dimension(32, 30));
                        labelHelp.setMaximumSize(new Dimension(32, 30));
                        labelHelp.setToolTipText(guiContext.cfg.gs("JobsUI.labelHelp.toolTipText"));
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelHelp.setIconTextGap(0);
                        labelHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                actionHelpClicked(e);
                            }
                        });
                        panelHelp.add(labelHelp);
                    }
                    panelTop.add(panelHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);
                    splitPaneContent.setMinimumSize(new Dimension(140, 80));

                    //======== scrollPaneConfig ========
                    {
                        scrollPaneConfig.setMinimumSize(new Dimension(140, 16));
                        scrollPaneConfig.setPreferredSize(new Dimension(142, 146));

                        //---- configItems ----
                        configItems.setPreferredSize(new Dimension(128, 54));
                        configItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        configItems.setFillsViewportHeight(true);
                        configItems.setShowVerticalLines(false);
                        configItems.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                configItemsMouseClicked(e);
                            }
                        });
                        scrollPaneConfig.setViewportView(configItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneConfig);

                    //======== panelJob ========
                    {
                        panelJob.setLayout(new BorderLayout());

                        //======== splitPaneToolsOrigin ========
                        {
                            splitPaneToolsOrigin.setDividerLocation(142);
                            splitPaneToolsOrigin.setLastDividerLocation(142);

                            //======== panelTasks ========
                            {
                                panelTasks.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelTasks.getLayout()).columnWidths = new int[] {0, 0};
                                ((GridBagLayout)panelTasks.getLayout()).rowHeights = new int[] {0, 0, 0};
                                ((GridBagLayout)panelTasks.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                                ((GridBagLayout)panelTasks.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                //---- labelTasks ----
                                labelTasks.setText(guiContext.cfg.gs("JobsUI.labelTasks.text"));
                                labelTasks.setHorizontalAlignment(SwingConstants.LEFT);
                                labelTasks.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelTasks.setFont(labelTasks.getFont().deriveFont(labelTasks.getFont().getSize() + 1f));
                                labelTasks.setMaximumSize(new Dimension(37, 18));
                                labelTasks.setMinimumSize(new Dimension(37, 18));
                                labelTasks.setPreferredSize(new Dimension(37, 18));
                                panelTasks.add(labelTasks, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 0), 0, 0));

                                //======== scrollPaneTasks ========
                                {

                                    //---- listTasks ----
                                    listTasks.addMouseListener(new MouseAdapter() {
                                        @Override
                                        public void mouseClicked(MouseEvent e) {
                                            listTasksMouseClicked(e);
                                        }
                                    });
                                    listTasks.addListSelectionListener(e -> listTasksValueChanged(e));
                                    scrollPaneTasks.setViewportView(listTasks);
                                }
                                panelTasks.add(scrollPaneTasks, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            splitPaneToolsOrigin.setLeftComponent(panelTasks);

                            //======== panelOrigin ========
                            {
                                panelOrigin.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelOrigin.getLayout()).columnWidths = new int[] {0, 0};
                                ((GridBagLayout)panelOrigin.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                ((GridBagLayout)panelOrigin.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                                ((GridBagLayout)panelOrigin.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                //---- labelSpacer ----
                                labelSpacer.setText("    ");
                                labelSpacer.setFont(labelSpacer.getFont().deriveFont(labelSpacer.getFont().getSize() + 1f));
                                labelSpacer.setHorizontalAlignment(SwingConstants.LEFT);
                                labelSpacer.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelSpacer.setMaximumSize(new Dimension(57, 18));
                                labelSpacer.setMinimumSize(new Dimension(57, 18));
                                labelSpacer.setPreferredSize(new Dimension(57, 18));
                                panelOrigin.add(labelSpacer, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 0), 0, 0));

                                //---- labelOrigins ----
                                labelOrigins.setText(guiContext.cfg.gs("JobsUI.labelOrigins.text"));
                                labelOrigins.setFont(labelOrigins.getFont().deriveFont(labelOrigins.getFont().getSize() + 1f));
                                labelOrigins.setHorizontalAlignment(SwingConstants.LEFT);
                                labelOrigins.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelOrigins.setMaximumSize(new Dimension(57, 18));
                                labelOrigins.setMinimumSize(new Dimension(57, 18));
                                labelOrigins.setPreferredSize(new Dimension(57, 18));
                                panelOrigin.add(labelOrigins, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 0), 0, 0));

                                //======== panelOriginInstance ========
                                {
                                    panelOriginInstance.setBorder(null);
                                    panelOriginInstance.setLayout(new BorderLayout());

                                    //======== panelPubSub ========
                                    {
                                        panelPubSub.setLayout(new BoxLayout(panelPubSub, BoxLayout.Y_AXIS));

                                        //---- tablePubSub ----
                                        tablePubSub.setCellSelectionEnabled(true);
                                        tablePubSub.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                                        tablePubSub.setShowVerticalLines(false);
                                        panelPubSub.add(tablePubSub);
                                    }
                                    panelOriginInstance.add(panelPubSub, BorderLayout.NORTH);

                                    //======== scrollPaneOrigins ========
                                    {
                                        scrollPaneOrigins.setViewportView(listOrigins);
                                    }
                                    panelOriginInstance.add(scrollPaneOrigins, BorderLayout.CENTER);

                                    //======== panelOriginsButtons ========
                                    {
                                        panelOriginsButtons.setBorder(null);
                                        panelOriginsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                        //---- buttonAddOrigin ----
                                        buttonAddOrigin.setText(guiContext.cfg.gs("JobsUI.buttonAddOrigin.text"));
                                        buttonAddOrigin.setFont(buttonAddOrigin.getFont().deriveFont(buttonAddOrigin.getFont().getSize() - 2f));
                                        buttonAddOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddOrigin.mnemonic").charAt(0));
                                        buttonAddOrigin.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddOrigin.toolTipText"));
                                        buttonAddOrigin.addActionListener(e -> actionOriginAddClicked(e));
                                        panelOriginsButtons.add(buttonAddOrigin);

                                        //---- buttonOriginUp ----
                                        buttonOriginUp.setText("^");
                                        buttonOriginUp.setMaximumSize(new Dimension(24, 24));
                                        buttonOriginUp.setMinimumSize(new Dimension(24, 24));
                                        buttonOriginUp.setPreferredSize(new Dimension(24, 24));
                                        buttonOriginUp.setFont(buttonOriginUp.getFont().deriveFont(buttonOriginUp.getFont().getSize() - 2f));
                                        buttonOriginUp.setToolTipText(guiContext.cfg.gs("JobsUI.buttonOriginUp.toolTipText"));
                                        buttonOriginUp.addActionListener(e -> actionOriginUpClicked(e));
                                        panelOriginsButtons.add(buttonOriginUp);

                                        //---- buttonOriginDown ----
                                        buttonOriginDown.setText("v");
                                        buttonOriginDown.setFont(buttonOriginDown.getFont().deriveFont(buttonOriginDown.getFont().getSize() - 2f));
                                        buttonOriginDown.setMaximumSize(new Dimension(24, 24));
                                        buttonOriginDown.setMinimumSize(new Dimension(24, 24));
                                        buttonOriginDown.setPreferredSize(new Dimension(24, 24));
                                        buttonOriginDown.setToolTipText(guiContext.cfg.gs("JobsUI.buttonOriginDown.toolTipText"));
                                        buttonOriginDown.addActionListener(e -> actionOriginDownClicked(e));
                                        panelOriginsButtons.add(buttonOriginDown);

                                        //---- buttonRemoveOrigin ----
                                        buttonRemoveOrigin.setText(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.text"));
                                        buttonRemoveOrigin.setFont(buttonRemoveOrigin.getFont().deriveFont(buttonRemoveOrigin.getFont().getSize() - 2f));
                                        buttonRemoveOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.mnemonic_2").charAt(0));
                                        buttonRemoveOrigin.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.toolTipText"));
                                        buttonRemoveOrigin.addActionListener(e -> actionOriginRemoveClicked(e));
                                        panelOriginsButtons.add(buttonRemoveOrigin);
                                    }
                                    panelOriginInstance.add(panelOriginsButtons, BorderLayout.SOUTH);
                                }
                                panelOrigin.add(panelOriginInstance, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            splitPaneToolsOrigin.setRightComponent(panelOrigin);
                        }
                        panelJob.add(splitPaneToolsOrigin, BorderLayout.CENTER);

                        //======== panelToolButtons ========
                        {
                            panelToolButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonAddTask ----
                            buttonAddTask.setText(guiContext.cfg.gs("JobsUI.buttonAddTask.text"));
                            buttonAddTask.setFont(buttonAddTask.getFont().deriveFont(buttonAddTask.getFont().getSize() - 2f));
                            buttonAddTask.setPreferredSize(new Dimension(78, 24));
                            buttonAddTask.setMinimumSize(new Dimension(78, 24));
                            buttonAddTask.setMaximumSize(new Dimension(78, 24));
                            buttonAddTask.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddTask.mnemonic").charAt(0));
                            buttonAddTask.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddTask.toolTipText"));
                            buttonAddTask.addActionListener(e -> actionTaskAddClicked(e));
                            panelToolButtons.add(buttonAddTask);

                            //---- buttonTaskUp ----
                            buttonTaskUp.setText("^");
                            buttonTaskUp.setMaximumSize(new Dimension(24, 24));
                            buttonTaskUp.setMinimumSize(new Dimension(24, 24));
                            buttonTaskUp.setPreferredSize(new Dimension(24, 24));
                            buttonTaskUp.setFont(buttonTaskUp.getFont().deriveFont(buttonTaskUp.getFont().getSize() - 2f));
                            buttonTaskUp.setToolTipText(guiContext.cfg.gs("JobsUI.buttonTaskUp.toolTipText"));
                            buttonTaskUp.addActionListener(e -> actionTaskUpClicked(e));
                            panelToolButtons.add(buttonTaskUp);

                            //---- buttonTaskDown ----
                            buttonTaskDown.setText("v");
                            buttonTaskDown.setFont(buttonTaskDown.getFont().deriveFont(buttonTaskDown.getFont().getSize() - 2f));
                            buttonTaskDown.setMaximumSize(new Dimension(24, 24));
                            buttonTaskDown.setMinimumSize(new Dimension(24, 24));
                            buttonTaskDown.setPreferredSize(new Dimension(24, 24));
                            buttonTaskDown.setToolTipText(guiContext.cfg.gs("JobsUI.buttonTaskDown.toolTipText"));
                            buttonTaskDown.addActionListener(e -> actionTaskDownClicked(e));
                            panelToolButtons.add(buttonTaskDown);

                            //---- buttonRemoveTask ----
                            buttonRemoveTask.setText(guiContext.cfg.gs("JobsUI.buttonRemoveTask.text"));
                            buttonRemoveTask.setFont(buttonRemoveTask.getFont().deriveFont(buttonRemoveTask.getFont().getSize() - 2f));
                            buttonRemoveTask.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveTask.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveTask.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveTask.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveTask.mnemonic").charAt(0));
                            buttonRemoveTask.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveTask.toolTipText"));
                            buttonRemoveTask.addActionListener(e -> actionTaskRemoveClicked(e));
                            panelToolButtons.add(buttonRemoveTask);
                        }
                        panelJob.add(panelToolButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelJob);
                }
                contentPanel.add(splitPaneContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 82, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("JobsUI.okButton.text"));
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("JobsUI.cancelButton.text"));
                cancelButton.addActionListener(e -> actionCancelClicked(e));
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JButton buttonNew;
    private JButton buttonCopy;
    private JButton buttonDelete;
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel hSpacerBeforeGenerate;
    private JButton buttonGenerate;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneConfig;
    private JTable configItems;
    private JPanel panelJob;
    private JSplitPane splitPaneToolsOrigin;
    private JPanel panelTasks;
    private JLabel labelTasks;
    private JScrollPane scrollPaneTasks;
    private JList listTasks;
    private JPanel panelOrigin;
    private JLabel labelSpacer;
    private JLabel labelOrigins;
    private JPanel panelOriginInstance;
    private JPanel panelPubSub;
    private JTable tablePubSub;
    private JScrollPane scrollPaneOrigins;
    private JList listOrigins;
    private JPanel panelOriginsButtons;
    private JButton buttonAddOrigin;
    private JButton buttonOriginUp;
    private JButton buttonOriginDown;
    private JButton buttonRemoveOrigin;
    private JPanel panelToolButtons;
    private JButton buttonAddTask;
    private JButton buttonTaskUp;
    private JButton buttonTaskDown;
    private JButton buttonRemoveTask;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    //
    // @formatter:on
    // </editor-fold>

}
