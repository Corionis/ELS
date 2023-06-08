package com.groksoft.els.gui.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.Generator;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.jobs.*;
import com.groksoft.els.repository.Repositories;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.gui.util.RotatedIcon;
import com.groksoft.els.gui.util.TextIcon;
import com.groksoft.els.tools.operations.OperationsTool;
import com.groksoft.els.tools.sleep.SleepTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
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

@SuppressWarnings(value = "unchecked")
public class JobsUI extends JDialog
{
    // combobox element types
    private static final int CACHED_LAST_TASK = 0;
    private static final int ANY_PUBLISHER = 1;
    private static final int SPECIFIC_PUBLISHER = 2;
    private static final int ANY_SUBSCRIBER = 3;
    private static final int LOCAL_SUBSCRIBER = 4;
    private static final int REMOTE_SUBSCRIBER = 5;

    // PubSub enable/disable combinations
    private final int WANT_CACHED = 0;
    private final int WANT_NO_PUBSUB = 1;
    private final int WANT_PUB = 2;
    private final int WANT_PUBSUB = 3;
    private final int WANT_PUBSUB_NO_ORIGINS = 4;

    private ConfigModel configModel;
    private Context context;
    private Job currentJob = null;
    private Task currentTask = null;
    private ArrayList<Job> deletedJobs;
    private JobsUI jobsUi;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private ArrayList<ArrayList<Origin>> originsArray = null;
    private ArrayList<Origin> savedOrigins = null;
    private Tools toolsHandler;
    private ArrayList<AbstractTool> toolList;
    private SwingWorker<Void, Void> worker;
    private boolean workerRunning = false;

    private JobsUI()
    {
        // hide default constructor
    }

    public JobsUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        this.jobsUi = this;
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
        if (context.preferences.getJobsXpos() > 0)
        {
            this.setLocation(context.preferences.getJobsXpos(), context.preferences.getJobsYpos());
            Dimension dim = new Dimension(context.preferences.getJobsWidth(), context.preferences.getJobsHeight());
            this.setSize(dim);
            this.splitPaneContent.setDividerLocation(context.preferences.getJobsTaskDividerLocation());
            this.splitPaneToolsOrigin.setDividerLocation(context.preferences.getJobsOriginDividerLocation());
        }
        else
        {
            Point parentPos = this.getParent().getLocation();
            Dimension parentSize = this.getParent().getSize();
            Dimension mySize = this.getSize();
            Point myPos = new Point(parentPos.x + (parentSize.width / 2 - mySize.width / 2),
                    parentPos.y + (parentSize.height / 2 - mySize.height / 2));
            this.setLocation(myPos);
        }

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
        configModel = new ConfigModel(context, this);
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
        Border border = buttonPub.getBorder();
        panelPubSub.setBorder(border);

        loadConfigurations();
        deletedJobs = new ArrayList<Job>();

    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && currentJob != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("JobsUI.stop.currently.running.job"),
                    context.cfg.gs("Z.cancel.run"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                currentJob.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Job.config.cancelled"), currentJob.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.cancel.all.changes"),
                        context.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    cancelChanges();
                    setVisible(false);
                }
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
            String rename = origJob.getConfigName() + context.cfg.gs("Z.copy");
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
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job job = (Job) configModel.getValueAt(index, 0);

            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + job.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                job.setDataHasChanged();
                deletedJobs.add(job);
                configModel.removeRow(index);
                if (index > configModel.getRowCount() - 1)
                    index = configModel.getRowCount() - 1;
                configModel.fireTableDataChanged();
                if (index >= 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadTasks(index);
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionGenerateClicked(ActionEvent evt)
    {
        Generator generator = new Generator(context);
        generator.showDialog(this, currentJob, currentJob.getConfigName());
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("JobsUI.help"), "jobs_" + context.preferences.getLocale() + ".html");
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
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            Job job = new Job(context, context.cfg.gs("Z.untitled"));
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
            }

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionOriginAddClicked(ActionEvent evt)
    {
        if (currentTask != null)
        {
            ArrayList<Origin> origins = new ArrayList<Origin>();
            // TODO match pub/sub combo for source ;; OR show warning when selection does not match
            try
            {
                boolean isSubscriber = Origins.makeOriginsFromSelected(context, this, origins, currentTask.isRealOnly()); // can return null
                if (origins != null && origins.size() > 0)
                {
                    listOrigins.requestFocus();
                    int count = origins.size();

                    // make dialog pieces
                    String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.add.N.origins"), count, which);

                    // confirm adds
                    int reply = JOptionPane.showConfirmDialog(this, message, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        currentTask.addOrigins(origins);
                        currentJob.setDataHasChanged();
                        loadOrigins(currentTask);
                    }
                }
            }
            catch (Exception e)
            {
                // there will be no exceptions thrown
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
                String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.remove.N.origins"), count);

                // confirm deletions
                int reply = JOptionPane.showConfirmDialog(this, message, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
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
            JList<String> list = new JList<String>();
            int id = 0;
            DefaultListModel<String> listModel = new DefaultListModel<String>();
            int selectedCombo = -1;
            int selectedList = -1;
            String cachedName = "";
            String text = null;
            String title = null;
            String tip = null;
            Repositories repositories = getRepositories();

            // make dialog pieces

            // Cached last task
            if (currentTask.isCachedLastTask(context))
            {
                int taskIndex = findTaskIndex(currentTask.getConfigName());
                if (taskIndex >= 0)
                {
                    cachedName = findCachedLastTask(currentJob, taskIndex);
                    if (cachedName.length() > 0)
                    {
                        combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.cached.task") + cachedName, CACHED_LAST_TASK));
                        if (currentTask.getPublisherKey().equals(Task.CACHEDLASTTASK))
                            selectedCombo = id - 1;
                    }
                }
            }

            title = (which == 0) ? context.cfg.gs("JobsUI.combo.select.publisher") : context.cfg.gs("JobsUI.combo.select.subscriber");

            if (which == 0 || which == 99) // publisher or both
            {
                title = context.cfg.gs("JobsUI.combo.select.publisher");
                tip = context.cfg.gs("JobsUI.select.publisher.tooltip");

                combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.any.publisher"), ANY_PUBLISHER));
                if (currentTask.getPublisherKey().equals(Task.ANY_SERVER))
                    selectedCombo = id - 1;

                combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.publisher.specific"), SPECIFIC_PUBLISHER));
                if (currentTask.getPublisherKey().length() > 0 &&
                        !currentTask.getPublisherKey().equals(Task.ANY_SERVER) &&
                        !currentTask.getPublisherKey().equals(Task.CACHEDLASTTASK))
                {
                    selectedCombo = id - 1;
                    Repositories.Meta meta = repositories.find(currentTask.getPublisherKey());
                    if (meta != null)
                        selectedList = repositories.indexOf(currentTask.getPublisherKey());
                }
            }

            if (which == 1 || which == 99) // subscriber or both
            {
                title = context.cfg.gs("JobsUI.combo.select.subscriber");
                tip = context.cfg.gs("JobsUI.select.subscriber.tooltip");

                combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.any.subscriber"), ANY_SUBSCRIBER));
                if (currentTask.getSubscriberKey().equals(Task.ANY_SERVER))
                    selectedCombo = id - 1;

                text = context.cfg.gs("JobsUI.subscriber.local");
                combo.addItem(new ComboItem(id++, text, LOCAL_SUBSCRIBER));

                text = context.cfg.gs("JobsUI.subscriber.remote");
                combo.addItem(new ComboItem(id++, text, REMOTE_SUBSCRIBER));

                if (currentTask.getSubscriberKey().length() > 0 &&
                        !currentTask.getSubscriberKey().equals(Task.ANY_SERVER) &&
                        !currentTask.getPublisherKey().equals(Task.CACHEDLASTTASK)) // check publisher key used to indicate last task
                {
                    selectedCombo = id - 2;
                    Repositories.Meta meta = repositories.find(currentTask.getSubscriberKey());
                    if (meta != null)
                    {
                        if (currentTask.isSubscriberRemote())
                            selectedCombo = id - 1;
                        selectedList = repositories.indexOf(currentTask.getSubscriberKey());
                    }
                }
            }

            if (which == 99)
            {
                title = context.cfg.gs("JobsUI.combo.select.publisher.or.subscriber");
                tip = context.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
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
                        int selected = combo.getSelectedIndex();
                        ComboItem item = (ComboItem) combo.getItemAt(selected);
                        if (item.type == CACHED_LAST_TASK || item.type == ANY_PUBLISHER || item.type == ANY_SUBSCRIBER)
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

            // add list of repositories
            for (int i = 0; i < repositories.getList().size(); ++i)
            {
                text = (repositories.getList().get(i)).description;
                listModel.addElement(text);
            }
            list.setModel(listModel);
            if (selectedList >= 0)
                list.setSelectedIndex(selectedList);

            // dialog
            JScrollPane pane = new JScrollPane();
            pane.setViewportView(list);
            list.requestFocus();
            Object[] params = {title, combo, pane};

            // prompt user
            int opt = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("JobsUI.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                String key;
                int selected = combo.getSelectedIndex();
                ComboItem item = (ComboItem) combo.getItemAt(selected);

                // what did they pick?
                if (item.type == CACHED_LAST_TASK)
                {
                    // publisher key (only) is used to indicate last task
                    key = Task.CACHEDLASTTASK;
                    if (currentTask.getOrigins() != null && currentTask.getOrigins().size() > 0)
                        savedOrigins = currentTask.getOrigins();
                    else
                        savedOrigins = null;
                    currentTask.setOrigins(new ArrayList<Origin>());
                }
                else if (item.type == ANY_PUBLISHER || item.type == ANY_SUBSCRIBER)
                {
                    key = Task.ANY_SERVER;
                    if (savedOrigins != null)
                        currentTask.setOrigins(savedOrigins);
                }
                else
                {
                    if (savedOrigins != null)
                        currentTask.setOrigins(savedOrigins);

                    int index = list.getSelectedIndex();
                    if (index < 0)
                    {
                        String msg = (which == 0 ? context.cfg.gs("JobsUI.combo.select.publisher") :
                                (which == 1 ? context.cfg.gs("JobsUI.combo.select.subscriber") :
                                        context.cfg.gs("JobsUI.combo.select.publisher.or.subscriber")));
                        JOptionPane.showMessageDialog(this, msg,
                                context.cfg.gs("JobsUI.title"), JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    key = (repositories.getList().get(index)).key;
                    if (item.type == ANY_SUBSCRIBER || item.type == REMOTE_SUBSCRIBER) // remote subscriber
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

                // populate task data
                if (item.type == CACHED_LAST_TASK)
                {
                    currentTask.setPublisherKey(key); // use publisher key only to indicate last task
                    currentTask.setSubscriberRemote(false);
                    currentTask.setSubscriberKey("");
                    currentJob.setDataHasChanged();
                }
                else
                {
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
                        if (item.type == ANY_PUBLISHER || item.type == SPECIFIC_PUBLISHER) // a publisher selection
                        {
                            if (!currentTask.getPublisherKey().equals(key))
                            {
                                currentTask.setPublisherKey(key);
                                currentTask.setSubscriberKey("");
                                currentJob.setDataHasChanged();
                            }
                        }
                        else if (item.type == ANY_SUBSCRIBER || item.type == LOCAL_SUBSCRIBER || item.type == REMOTE_SUBSCRIBER) // a subscriber selection
                        {
                            if (!currentTask.getSubscriberKey().equals(key))
                            {
                                currentTask.setSubscriberKey(key);
                                currentTask.setPublisherKey("");
                                currentJob.setDataHasChanged();
                            }
                        }
                    }
                }
                loadPubSubs(currentTask);
                loadOrigins(currentTask);
            }
        }
    }

    private void actionRunClicked(ActionEvent evt)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job job = (Job) configModel.getValueAt(index, 0);
            processJob(job);
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        if (saveConfigurations())
        {
            savePreferences();
            setVisible(false);
        }
    }

    private void actionTaskAddClicked(ActionEvent evt)
    {
        if (currentJob != null)
        {
            listTasks.requestFocus();

            // make dialog pieces
            String message = context.cfg.gs("JobsUI.select.tool");

            JList<String> toolJList = new JList<String>();
            ArrayList<AbstractTool> toolList = null;

            try
            {
                // load Job tools
                Jobs jobsHandler = new Jobs(context);
                toolList = jobsHandler.loadAllJobs(); // creates the ArrayList

                // add all the other tools
                Tools toolsHandler = new Tools();
                toolList = toolsHandler.loadAllTools(context, null, toolList);

                // make the String list for display
                ArrayList<String> toolNames = new ArrayList<>();
                for (AbstractTool tool : toolList)
                {
                    toolNames.add(tool.getListName());
                }
                Collections.sort(toolNames);

                // add the Strings to the JList model
                DefaultListModel<String> dialogList = new DefaultListModel<String>();
                for (String name : toolNames)
                {
                    dialogList.addElement(name);
                }
                toolJList.setModel(dialogList);
                toolJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                toolJList.setSelectedIndex(0);
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg,
                        context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }

            JScrollPane pane = new JScrollPane();
            pane.setViewportView(toolJList);
            toolJList.requestFocus();
            Object[] params = {message, pane};

            int opt = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("JobsUI.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                String name = toolJList.getSelectedValue();
                int index = 0;
                for (; index < toolList.size(); ++index)
                {
                    if (name.equals(((AbstractTool) toolList.get(index)).getListName()))
                    {
                        break; // it is not possible for index to be invalid
                    }
                }
                AbstractTool tool = toolList.get(index);
                currentTask = new Task(tool.getInternalName(), tool.getConfigName());
                currentTask.setDual(tool.isDualRepositories());
                currentTask.setRealOnly(tool.isRealOnly());
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
                String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.remove.N.tasks"), count);

                // confirm deletions
                int reply = JOptionPane.showConfirmDialog(this, message, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
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

    public void cancelChanges()
    {
        if (deletedJobs.size() > 0)
            deletedJobs = new ArrayList<Job>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((Job) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }
    }

    public boolean checkForChanges()
    {
        for (int i = 0; i < deletedJobs.size(); ++i)
        {
            if (deletedJobs.get(i).isDataChanged())
                return true;
        }

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
            {
                loadTasks(jobIndex);
                configItems.setRowSelectionInterval(jobIndex, jobIndex);
                if (currentJob.getTasks().size() > 0)
                    listTasks.setSelectedIndex(0);
            }
        }
    }

    private void enableDisableOrigins(boolean sense)
    {
        buttonAddOrigin.setEnabled(sense);
        buttonOriginUp.setEnabled(sense);
        buttonOriginDown.setEnabled(sense);
        buttonRemoveOrigin.setEnabled(sense);
    }

    private String findCachedLastTask(Job job, int index)
    {
        String name = "";
        if (job.getTasks().size() > 0)
        {
            for (int j = index - 1; j >= 0; --j)
            {
                Task task = job.getTasks().get(j);

                // is the task a Job?
                if (task.isJob())
                {
                    Job subJob = findJob(task.getConfigName());
                    if (subJob != null)
                        name = findCachedLastTask(subJob, subJob.getTasks().size());
                    if (name.length() > 0)
                        break;
                }
                else if (task.isCachedLastTask(context) && task != currentTask)
                {
                    name = task.getConfigName(); // qualifies for cached last task
                    break;
                }
            }
        }
        return name;
    }

    private Job findJob(String name)
    {
        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            Job job = (Job) configModel.getValueAt(i, 0);
            if (job.getConfigName().equals(name))
                return job;
        }
        return null;
    }

    private int findJobIndex(String name)
    {
        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            Job job = (Job) configModel.getValueAt(i, 0);
            if (job.getConfigName().equals(name))
                return i;
        }
        return -1;
    }

    private Task findTask(String name)
    {
        for (int i = 0; i < currentJob.getTasks().size(); ++i)
        {
            Task task = currentJob.getTasks().get(i);
            if (task.getConfigName().equals(name))
                return task;
        }
        return null;
    }

    private int findTaskIndex(String name)
    {
        for (int i = 0; i < currentJob.getTasks().size(); ++i)
        {
            Task task = currentJob.getTasks().get(i);
            if (task.getConfigName().equals(name))
                return i;
        }
        return -1;
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
        String path = job.getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(job.getConfigName()) + ".json";
        return path;
    }

    public String getOriginType(int type)
    {
        switch (type)
        {
            case NavTreeUserObject.BOOKMARKS:
                return context.cfg.gs("NavTreeNode.bookmark");
            case NavTreeUserObject.COLLECTION:
                return context.cfg.gs("NavTreeNode.collection");
            case NavTreeUserObject.COMPUTER:
                return context.cfg.gs("NavTreeNode.computer");
            case NavTreeUserObject.DRIVE:
                return context.cfg.gs("NavTreeNode.drive");
            case NavTreeUserObject.HOME:
                return context.cfg.gs("NavTreeNode.home");
            case NavTreeUserObject.LIBRARY:
                return context.cfg.gs("NavTreeNode.library");
            case NavTreeUserObject.REAL:
                return "";
            case NavTreeUserObject.SYSTEM:
                return context.cfg.gs("NavTreeNode.system");
            default:
                return context.cfg.gs("NavTreeNode.unknown");
        }
    }

    /**
     * Get task want for PubSub and Origins
     *
     * @param task Task
     * @return WANT_*
     * @throws Exception Possible exception loading a tool
     * @see OperationsTool.Cards
     */
    private int getOriginWant(Task task) throws Exception
    {
        int want = -1;

        if (task.getPublisherKey().equals(Task.CACHEDLASTTASK)) // publisher key (only) is used to indicate cached last task
            want = WANT_CACHED;
        else if (task.getInternalName().equals(Job.INTERNAL_NAME) ||
                task.getInternalName().equals(SleepTool.INTERNAL_NAME) ||
                (task.getInternalName().equals(OperationsTool.INTERNAL_NAME) && ((OperationsTool)task.getTool()).getCard().equals(OperationsTool.Cards.HintServer)))
            want = WANT_NO_PUBSUB;
        else if (task.getInternalName().equals(OperationsTool.INTERNAL_NAME) && ((OperationsTool)task.getTool()).getCard().equals(OperationsTool.Cards.StatusQuit))
            want = WANT_PUB;
        else if (task.getInternalName().equals(OperationsTool.INTERNAL_NAME))
            want = WANT_PUBSUB_NO_ORIGINS;
        else
            want = WANT_PUBSUB;

        return want;
    }

    public Repositories getRepositories()
    {
        Repositories repositories = null;
        try
        {
            repositories = new Repositories();
            repositories.loadList(context);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg,
                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }
        return repositories;
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
            {
                savedOrigins = null;
                loadOrigins(currentJob.getTasks().get(taskIndex));
            }
        }
    }

    private void loadConfigurations()
    {
        Job tmpJob = new Job(context, "temp");
        File jobsDir = new File(tmpJob.getDirectoryPath());
        if (jobsDir.exists())
        {
            toolsHandler = new Tools();
            toolList = null;
            try
            {
                toolList = toolsHandler.loadAllTools(context, null);
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg,
                        context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }

            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(java.lang.reflect.Type type)
                {
                    return new Job(context, "");
                }
            }
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Job.class, new objInstanceCreator());

            ArrayList<Job> jobsArray = new ArrayList<>();
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null && json.length() > 0)
                        {
                            Job job = builder.create().fromJson(json, Job.class);
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
                                            task.setRealOnly(tool.isRealOnly());
                                            task.setContext(context);
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
                        String msg = context.cfg.gs("Z.exception") + entry.getName() + " " + Utils.getStackTrace(e);
                        logger.error(msg);
                        JOptionPane.showMessageDialog(this, msg,
                                context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
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
            enableDisableOrigins(false);
        }
        else
        {
            loadTasks(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
            if (currentJob.getTasks().size() > 0)
                listTasks.setSelectedIndex(0);
        }
    }

    private void loadTasks(int jobIndex)
    {
        DefaultListModel<String> model = new DefaultListModel<String>();
        savedOrigins = null;
        if (jobIndex < configModel.getRowCount())
        {
            if (jobIndex >= 0)
                currentJob = (Job) configModel.getValueAt(jobIndex, 0);

            if (currentJob.getTasks() != null && currentJob.getTasks().size() > 0)
            {
                for (int i = 0; i < currentJob.getTasks().size(); ++i)
                {
                    Task task = currentJob.getTasks().get(i);
                    task.setContext(context);
                    String i18n = task.getInternalName() + ".displayName";
                    i18n = context.cfg.gs(i18n);
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
        DefaultListModel<String> model = new DefaultListModel<String>();

        currentTask = task;
        enableDisableOrigins(currentTask.isOriginPathsAllowed(context));

        int want = -1;
        try
        {
            want = getOriginWant(currentTask);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg,
                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }

        switch (want)
        {
            case WANT_CACHED:
                loadPubSubs(currentTask);
                labelOrigins.setEnabled(true);
                buttonPub.setEnabled(true);
                enableDisableOrigins(false);
                break;
            case WANT_NO_PUBSUB:
                loadPubSubs(null);
                labelOrigins.setEnabled(false);
                buttonPub.setEnabled(false);
                enableDisableOrigins(false);
                break;
            case WANT_PUB:
                task.setDual(false);
                loadPubSubs(currentTask);
                labelOrigins.setEnabled(true);
                buttonPub.setEnabled(true);
                enableDisableOrigins(false);
                break;
            case WANT_PUBSUB:
            case WANT_PUBSUB_NO_ORIGINS:
                loadPubSubs(currentTask);
                buttonPub.setEnabled(true);
                labelOrigins.setEnabled(true);
                enableDisableOrigins(want == WANT_PUBSUB ? true : false);

                if (want == WANT_PUBSUB && currentTask.getOrigins().size() > 0)
                {
                    for (int i = 0; i < currentTask.getOrigins().size(); ++i)
                    {
                        Origin origin = currentTask.getOrigins().get(i);
                        String ot = getOriginType(origin.getType());
                        String id = getOriginType(origin.getType()) + (ot.length() > 0 ? ": " : "") + origin.getLocation();
                        model.addElement(id);
                    }
                    if (listOrigins.getModel().getSize() > 0)
                        listOrigins.setSelectedIndex(0);
                }
                break;
        }

        listOrigins.setModel(model);
    }

    private void loadPubSubs(Task task)
    {
        Repositories repositories = getRepositories();

        labelPub.setText(getPubSubValue(task, 0, 0, repositories));
        buttonPub.setToolTipText(getPubSubValue(task, 0, 1, repositories));

        if (task == null || !task.isDual())
        {
            labelSub.setVisible(false);
            buttonSub.setVisible(false);
        }
        else
        {
            labelSub.setVisible(true);
            buttonSub.setVisible(true);
            labelSub.setText(getPubSubValue(task, 1, 0, repositories));
            buttonSub.setToolTipText(getPubSubValue(task, 1, 1, repositories));
        }
    }

    private String getPubSubDesc(Task task, boolean isPublisher, boolean isRemote, Repositories repositories, String key)
    {
        String desc = "";
        if (key.trim().length() == 0)
        {
            if (task.isDual())
            {
                if (isPublisher)
                    desc = context.cfg.gs("JobsUI.select.publisher");
                else
                    desc = context.cfg.gs("JobsUI.select.subscriber");
            }
            else
                desc = context.cfg.gs("JobsUI.select.publisher.or.subscriber");
        }
        else if (key.equals(Task.CACHEDLASTTASK))
        {
            String name = context.cfg.gs("Z.not.found");
            int taskIndex = findTaskIndex(task.getConfigName());
            if (taskIndex >= 0)
            {
                String cachedName = findCachedLastTask(currentJob, taskIndex);
                if (cachedName.length() > 0)
                {
                    name = cachedName;
                }
            }
            desc = context.cfg.gs("JobsUI.cached.task") + name;
        }
        else if (key.equals(Task.ANY_SERVER))
        {
            if (isPublisher)
                desc = context.cfg.gs("JobsUI.any.publisher");
            else
                desc = context.cfg.gs("JobsUI.any.subscriber");
        }
        else
        {
            Repositories.Meta meta = repositories.find(key);
            if (meta != null)
            {
                if (isRemote)
                    desc = context.cfg.gs("Z.remote.uppercase");
                else
                {
                    if (!isPublisher)
                        desc = context.cfg.gs("Z.local.uppercase");
                }
                desc += (isPublisher ? context.cfg.gs("Z.publisher") : context.cfg.gs("Z.subscriber")) + ": " + meta.description;
            }
            else
                desc = context.cfg.gs("Z.cannot.find") + key;
        }
        return desc;
    }

    public String getPubSubValue(Task task, int row, int column, Repositories repositories)
    {
        if (task != null)
        {
            if (column == 0)
            {
                if (task.isDual())
                {
                    if (row == 0)
                        return getPubSubDesc(task, true, false, repositories, task.getPublisherKey());
                    return getPubSubDesc(task, false, task.isSubscriberRemote(), repositories, task.getSubscriberKey());
                }
                else
                {
                    String key = "";
                    boolean isPublisher = true;
                    if (task.getPublisherKey().length() > 0)
                        key = task.getPublisherKey();
                    else if (task.getSubscriberKey().length() > 0)
                    {
                        isPublisher = false;
                        key = task.getSubscriberKey();
                    }
                    return getPubSubDesc(task, isPublisher, (isPublisher ? false : task.isSubscriberRemote()), repositories, key);
                }
            }

            if (column == 1)
            {
                String toolTip = "";
                if (!task.isDual())
                    toolTip = context.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
                else
                {
                    if (row == 0)
                        toolTip = context.cfg.gs("JobsUI.select.publisher.tooltip");
                    else
                        toolTip = context.cfg.gs("JobsUI.select.subscriber.tooltip");
                }
                return toolTip;
            }
        }
        return null;
    }

    private void processJob(Job job)
    {
        // validate job tasks and origins
        String status = job.validate(context.cfg);
        if (status.length() == 0)
        {
            // make dialog pieces
            String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
            JCheckBox checkbox = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
            checkbox.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
            checkbox.setSelected(true);
            Object[] params = {message, checkbox};

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
            isDryRun = checkbox.isSelected();
            if (reply == JOptionPane.YES_OPTION)
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                context.navigator.disableComponent(true, getContentPane());
                cancelButton.setEnabled(true);
                cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                labelHelp.setEnabled(true);
                labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                // capture current selections
                try
                {
                    originsArray = Origins.makeAllOrigins(context, context.mainFrame);
                }
                catch (Exception e)
                {
                    if (!e.getMessage().equals("HANDLED_INTERNALLY"))
                    {
                        String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                        if (context.navigator != null)
                        {
                            logger.error(msg);
                            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                        }
                        else
                            logger.error(msg);
                    }
                }

                worker = job.process(context, this, this.getTitle(), job, isDryRun);
                if (worker != null)
                {
                    workerRunning = true;
                    context.navigator.disableGui(true);
                    worker.addPropertyChangeListener(new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange(PropertyChangeEvent e)
                        {
                            if (e.getPropertyName().equals("state"))
                            {
                                if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                    processTerminated(job);
                            }
                        }
                    });
                    worker.execute();
                }
                else
                    processTerminated(job);
            }
        }
        else
            JOptionPane.showMessageDialog(this, status, context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
    }

    private void processTerminated(Job job)
    {
        try
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            // reset and reload relevant trees
            if (!isDryRun)
            {
                if (job.usesPublisher())
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.publisher"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false, false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false, false);
                }
                if (job.usesSubscriber())
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.subscriber"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.cfg.isRemoteSession(), false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.cfg.isRemoteSession(), false);
                }
            }

            if (context.progress != null)
                context.progress.done();

            workerRunning = false;
            context.navigator.disableGui(false);
            context.navigator.disableComponent(false, getContentPane());

            if (originsArray != null && originsArray.size() == 8)
                Origins.setAllOrigins(context, context.mainFrame, originsArray);

            this.requestFocus();
            context.navigator.reconnectRemote(context, context.publisherRepo, context.subscriberRepo);
        }
        catch (Exception e)
        {
        }

        if (job.isRequestStop())
        {
            logger.info(job.getConfigName() + context.cfg.gs("Z.cancelled"));
            context.mainFrame.labelStatusMiddle.setText(job.getConfigName() + context.cfg.gs("Z.cancelled"));
        }
        else
            context.mainFrame.labelStatusMiddle.setText(job.getConfigName() + context.cfg.gs("Z.completed"));
    }

    private boolean saveConfigurations()
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
                    if (!validateJob(job, false))
                        return false;
                    write(job);
                    changed = true;
                    job.setDataHasChanged(false);
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
                job.setDataHasChanged(false);
            }

            if (changed)
                context.navigator.loadJobsMenu();
        }
        catch (Exception e)
        {
            String name = (job != null) ? job.getConfigName() + " " : " ";
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(this,
                    context.cfg.gs("Z.error.writing") + name + e.getMessage(),
                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }
        return true;
    }

    private void savePreferences()
    {
        context.preferences.setJobsHeight(this.getHeight());
        context.preferences.setJobsWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setJobsXpos(location.x);
        context.preferences.setJobsYpos(location.y);
        context.preferences.setJobsTaskDividerLocation(splitPaneContent.getDividerLocation());
        context.preferences.setJobsOriginDividerLocation(splitPaneToolsOrigin.getDividerLocation());
    }

    private boolean validateJob(Job job, boolean onlyFound) throws Exception
    {
        boolean cachedFound = false;
        boolean sense = true;

        // reload tools from disk if they've changed while Jobs were shown
        toolList = toolsHandler.loadAllTools(context, null);

        for (int i = 0; i < job.getTasks().size(); ++i)
        {
            Task task = job.getTasks().get(i);
            if (!task.getInternalName().equals(Job.INTERNAL_NAME) && !task.getInternalName().equals(SleepTool.INTERNAL_NAME))
            {
                AbstractTool tool = toolsHandler.getTool(toolList, task.getInternalName(), task.getConfigName());
                if (tool != null)
                {
                    if (tool.isCachedLastTask())
                    {
                        if (task.getPublisherKey().equals(Task.CACHEDLASTTASK))
                        {
                            if (task.getOrigins().size() == 0)
                            {
                                if (!cachedFound)
                                {
                                    sense = false;
                                    JOptionPane.showMessageDialog(this, context.cfg.gs("JobsUI.task.has.no.origins") +
                                            job.getConfigName() + ", " + task.getConfigName(), context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                                }
                            }
                        }
                        else if (task.getOrigins().size() > 0)
                            cachedFound = true;
                    }
                }
                else
                {
                    sense = false;
                    JOptionPane.showMessageDialog(this, context.cfg.gs("JobsUI.tool.not.found") +
                            job.getConfigName() + ", " + task.getConfigName(), context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
            else if (!task.getInternalName().equals(SleepTool.INTERNAL_NAME))
            {
                Job subJob = job.load(task.getConfigName());
                cachedFound = validateJob(subJob, true);
                onlyFound = false;
            }
        }
        return (onlyFound) ? cachedFound : sense;
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
            throw new MungeException(context.cfg.gs("Z.error.writing") + getFullPath(job) + ": " + Utils.getStackTrace(fnf));
        }
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
        public int type;

        public ComboItem(int id, String text, int type)
        {
            this.id = id;
            this.text = text;
            this.type = type;
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
        labelPub = new JLabel();
        buttonPub = new JButton();
        labelSub = new JLabel();
        buttonSub = new JButton();
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
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("JobsUI.title"));
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
                        buttonNew.setText(context.cfg.gs("JobsUI.buttonNew.text"));
                        buttonNew.setMnemonic(context.cfg.gs("JobsUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("JobsUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("JobsUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("JobsUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("JobsUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("JobsUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("JobsUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("JobsUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("JobsUI.buttonRun.text"));
                        buttonRun.setMnemonic(context.cfg.gs("JobsUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("JobsUI.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);

                        //---- hSpacerBeforeGenerate ----
                        hSpacerBeforeGenerate.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeGenerate.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeGenerate);

                        //---- buttonGenerate ----
                        buttonGenerate.setText(context.cfg.gs("JobsUI.buttonGenerate.text"));
                        buttonGenerate.setMnemonic(context.cfg.gs("JobsUI.buttonGenerate.mnemonic_2").charAt(0));
                        buttonGenerate.setToolTipText(context.cfg.gs("JobsUI.buttonGenerate.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("JobsUI.labelHelp.toolTipText"));
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

                        //---- configItems ----
                        configItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        configItems.setFillsViewportHeight(true);
                        configItems.setShowVerticalLines(false);
                        configItems.setShowHorizontalLines(false);
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
                                labelTasks.setText(context.cfg.gs("JobsUI.labelTasks.text"));
                                labelTasks.setHorizontalAlignment(SwingConstants.LEFT);
                                labelTasks.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelTasks.setFont(labelTasks.getFont().deriveFont(labelTasks.getFont().getStyle() | Font.BOLD, labelTasks.getFont().getSize() + 1f));
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
                                labelOrigins.setText(context.cfg.gs("JobsUI.labelOrigins.text"));
                                labelOrigins.setFont(labelOrigins.getFont().deriveFont(labelOrigins.getFont().getStyle() | Font.BOLD, labelOrigins.getFont().getSize() + 1f));
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
                                        panelPubSub.setLayout(new GridBagLayout());
                                        ((GridBagLayout)panelPubSub.getLayout()).columnWidths = new int[] {0, 0, 0};
                                        ((GridBagLayout)panelPubSub.getLayout()).rowHeights = new int[] {0, 0, 0};
                                        ((GridBagLayout)panelPubSub.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                                        ((GridBagLayout)panelPubSub.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                        //---- labelPub ----
                                        labelPub.setMaximumSize(new Dimension(24, 18));
                                        labelPub.setMinimumSize(new Dimension(24, 18));
                                        labelPub.setPreferredSize(new Dimension(24, 18));
                                        labelPub.setFont(labelPub.getFont().deriveFont(labelPub.getFont().getSize() + 1f));
                                        panelPubSub.add(labelPub, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 4, 0, 8), 0, 0));

                                        //---- buttonPub ----
                                        buttonPub.setText("...");
                                        buttonPub.setMaximumSize(new Dimension(32, 24));
                                        buttonPub.setMinimumSize(new Dimension(32, 24));
                                        buttonPub.setPreferredSize(new Dimension(32, 24));
                                        buttonPub.setVerticalTextPosition(SwingConstants.TOP);
                                        buttonPub.setFont(buttonPub.getFont().deriveFont(buttonPub.getFont().getStyle() | Font.BOLD));
                                        buttonPub.setHorizontalTextPosition(SwingConstants.LEADING);
                                        buttonPub.setIconTextGap(0);
                                        buttonPub.setActionCommand("0");
                                        buttonPub.addActionListener(e -> actionPubSubClicked(e));
                                        panelPubSub.add(buttonPub, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));

                                        //---- labelSub ----
                                        labelSub.setMaximumSize(new Dimension(24, 18));
                                        labelSub.setMinimumSize(new Dimension(24, 18));
                                        labelSub.setPreferredSize(new Dimension(24, 18));
                                        labelSub.setFont(labelSub.getFont().deriveFont(labelSub.getFont().getSize() + 1f));
                                        panelPubSub.add(labelSub, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 4, 0, 8), 0, 0));

                                        //---- buttonSub ----
                                        buttonSub.setText("...");
                                        buttonSub.setMaximumSize(new Dimension(32, 24));
                                        buttonSub.setMinimumSize(new Dimension(32, 24));
                                        buttonSub.setPreferredSize(new Dimension(32, 24));
                                        buttonSub.setVerticalTextPosition(SwingConstants.TOP);
                                        buttonSub.setFont(buttonSub.getFont().deriveFont(buttonSub.getFont().getStyle() | Font.BOLD));
                                        buttonSub.setHorizontalTextPosition(SwingConstants.LEADING);
                                        buttonSub.setIconTextGap(0);
                                        buttonSub.setActionCommand("1");
                                        buttonSub.addActionListener(e -> actionPubSubClicked(e));
                                        panelPubSub.add(buttonSub, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
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
                                        buttonAddOrigin.setText(context.cfg.gs("JobsUI.buttonAddOrigin.text"));
                                        buttonAddOrigin.setFont(buttonAddOrigin.getFont().deriveFont(buttonAddOrigin.getFont().getSize() - 2f));
                                        buttonAddOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMnemonic(context.cfg.gs("JobsUI.buttonAddOrigin.mnemonic").charAt(0));
                                        buttonAddOrigin.setToolTipText(context.cfg.gs("JobsUI.buttonAddOrigin.toolTipText"));
                                        buttonAddOrigin.addActionListener(e -> actionOriginAddClicked(e));
                                        panelOriginsButtons.add(buttonAddOrigin);

                                        //---- buttonOriginUp ----
                                        buttonOriginUp.setText("^");
                                        buttonOriginUp.setMaximumSize(new Dimension(24, 24));
                                        buttonOriginUp.setMinimumSize(new Dimension(24, 24));
                                        buttonOriginUp.setPreferredSize(new Dimension(24, 24));
                                        buttonOriginUp.setFont(buttonOriginUp.getFont().deriveFont(buttonOriginUp.getFont().getSize() - 2f));
                                        buttonOriginUp.setToolTipText(context.cfg.gs("JobsUI.buttonOriginUp.toolTipText"));
                                        buttonOriginUp.addActionListener(e -> actionOriginUpClicked(e));
                                        panelOriginsButtons.add(buttonOriginUp);

                                        //---- buttonOriginDown ----
                                        buttonOriginDown.setText("v");
                                        buttonOriginDown.setFont(buttonOriginDown.getFont().deriveFont(buttonOriginDown.getFont().getSize() - 2f));
                                        buttonOriginDown.setMaximumSize(new Dimension(24, 24));
                                        buttonOriginDown.setMinimumSize(new Dimension(24, 24));
                                        buttonOriginDown.setPreferredSize(new Dimension(24, 24));
                                        buttonOriginDown.setToolTipText(context.cfg.gs("JobsUI.buttonOriginDown.toolTipText"));
                                        buttonOriginDown.addActionListener(e -> actionOriginDownClicked(e));
                                        panelOriginsButtons.add(buttonOriginDown);

                                        //---- buttonRemoveOrigin ----
                                        buttonRemoveOrigin.setText(context.cfg.gs("JobsUI.buttonRemoveOrigin.text"));
                                        buttonRemoveOrigin.setFont(buttonRemoveOrigin.getFont().deriveFont(buttonRemoveOrigin.getFont().getSize() - 2f));
                                        buttonRemoveOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMnemonic(context.cfg.gs("JobsUI.buttonRemoveOrigin.mnemonic_2").charAt(0));
                                        buttonRemoveOrigin.setToolTipText(context.cfg.gs("JobsUI.buttonRemoveOrigin.toolTipText"));
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
                            buttonAddTask.setText(context.cfg.gs("JobsUI.buttonAddTask.text"));
                            buttonAddTask.setFont(buttonAddTask.getFont().deriveFont(buttonAddTask.getFont().getSize() - 2f));
                            buttonAddTask.setPreferredSize(new Dimension(78, 24));
                            buttonAddTask.setMinimumSize(new Dimension(78, 24));
                            buttonAddTask.setMaximumSize(new Dimension(78, 24));
                            buttonAddTask.setMnemonic(context.cfg.gs("JobsUI.buttonAddTask.mnemonic").charAt(0));
                            buttonAddTask.setToolTipText(context.cfg.gs("JobsUI.buttonAddTask.toolTipText"));
                            buttonAddTask.addActionListener(e -> actionTaskAddClicked(e));
                            panelToolButtons.add(buttonAddTask);

                            //---- buttonTaskUp ----
                            buttonTaskUp.setText("^");
                            buttonTaskUp.setMaximumSize(new Dimension(24, 24));
                            buttonTaskUp.setMinimumSize(new Dimension(24, 24));
                            buttonTaskUp.setPreferredSize(new Dimension(24, 24));
                            buttonTaskUp.setFont(buttonTaskUp.getFont().deriveFont(buttonTaskUp.getFont().getSize() - 2f));
                            buttonTaskUp.setToolTipText(context.cfg.gs("JobsUI.buttonTaskUp.toolTipText"));
                            buttonTaskUp.addActionListener(e -> actionTaskUpClicked(e));
                            panelToolButtons.add(buttonTaskUp);

                            //---- buttonTaskDown ----
                            buttonTaskDown.setText("v");
                            buttonTaskDown.setFont(buttonTaskDown.getFont().deriveFont(buttonTaskDown.getFont().getSize() - 2f));
                            buttonTaskDown.setMaximumSize(new Dimension(24, 24));
                            buttonTaskDown.setMinimumSize(new Dimension(24, 24));
                            buttonTaskDown.setPreferredSize(new Dimension(24, 24));
                            buttonTaskDown.setToolTipText(context.cfg.gs("JobsUI.buttonTaskDown.toolTipText"));
                            buttonTaskDown.addActionListener(e -> actionTaskDownClicked(e));
                            panelToolButtons.add(buttonTaskDown);

                            //---- buttonRemoveTask ----
                            buttonRemoveTask.setText(context.cfg.gs("JobsUI.buttonRemoveTask.text"));
                            buttonRemoveTask.setFont(buttonRemoveTask.getFont().deriveFont(buttonRemoveTask.getFont().getSize() - 2f));
                            buttonRemoveTask.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveTask.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveTask.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveTask.setMnemonic(context.cfg.gs("JobsUI.buttonRemoveTask.mnemonic").charAt(0));
                            buttonRemoveTask.setToolTipText(context.cfg.gs("JobsUI.buttonRemoveTask.toolTipText"));
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

                //---- saveButton ----
                saveButton.setText(context.cfg.gs("Z.save"));
                saveButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                saveButton.setActionCommand(context.cfg.gs("Z.save"));
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
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
    public JPanel dialogPane;
    public JPanel contentPanel;
    public JPanel panelTop;
    public JPanel panelTopButtons;
    public JButton buttonNew;
    public JButton buttonCopy;
    public JButton buttonDelete;
    public JPanel hSpacerBeforeRun;
    public JButton buttonRun;
    public JPanel hSpacerBeforeGenerate;
    public JButton buttonGenerate;
    public JPanel panelHelp;
    public JLabel labelHelp;
    public JSplitPane splitPaneContent;
    public JScrollPane scrollPaneConfig;
    public JTable configItems;
    public JPanel panelJob;
    public JSplitPane splitPaneToolsOrigin;
    public JPanel panelTasks;
    public JLabel labelTasks;
    public JScrollPane scrollPaneTasks;
    public JList listTasks;
    public JPanel panelOrigin;
    public JLabel labelSpacer;
    public JLabel labelOrigins;
    public JPanel panelOriginInstance;
    public JPanel panelPubSub;
    public JLabel labelPub;
    public JButton buttonPub;
    public JLabel labelSub;
    public JButton buttonSub;
    public JScrollPane scrollPaneOrigins;
    public JList listOrigins;
    public JPanel panelOriginsButtons;
    public JButton buttonAddOrigin;
    public JButton buttonOriginUp;
    public JButton buttonOriginDown;
    public JButton buttonRemoveOrigin;
    public JPanel panelToolButtons;
    public JButton buttonAddTask;
    public JButton buttonTaskUp;
    public JButton buttonTaskDown;
    public JButton buttonRemoveTask;
    public JPanel buttonBar;
    public JButton saveButton;
    public JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    //
    // @formatter:on
    // </editor-fold>

}
