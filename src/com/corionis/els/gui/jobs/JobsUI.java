package com.corionis.els.gui.jobs;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.Generator;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.util.TextIcon;
import com.corionis.els.repository.RepoMeta;
import com.corionis.els.repository.Repositories;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import com.corionis.els.tools.sleep.SleepTool;
import com.corionis.els.jobs.*;
import com.corionis.els.gui.util.RotatedIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

@SuppressWarnings(value = "unchecked")

public class JobsUI extends AbstractToolDialog
{
    // combobox element types
    private static final int CACHED_LAST_TASK = 0;
    private static final int ANY_PUBLISHER = 1;
    private static final int SPECIFIC_PUBLISHER = 2;
    private static final int ANY_SUBSCRIBER = 3;
    private static final int LOCAL = 4;
    private static final int REMOTE_CUSTOM = 8;
    private static final int REMOTE_HOST = 5;
    private static final int REMOTE_LISTEN = 6;
    private static final int PUBLISHER = 7;

    private ConfigModel configModel;
    private Context context;
    private Job currentJob = null;
    private Task currentTask = null;
    private AbstractTool currentTool = null;
    private Jobs jobsHandler = null;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private ArrayList<ArrayList<Origin>> originsArray = null;
    private Repositories repositories = null;
    private ArrayList<Origin> savedOrigins = null;
    private ArrayList<AbstractTool> toolList;
    private SwingWorker<Void, Void> worker;

    public JobsUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
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
        if (context.preferences.getJobsXpos() != -1 && Utils.isOnScreen(context.preferences.getJobsXpos(), context.preferences.getJobsYpos()))
        {
            this.setLocation(context.preferences.getJobsXpos(), context.preferences.getJobsYpos());
            Dimension dim = new Dimension(context.preferences.getJobsWidth(), context.preferences.getJobsHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        this.splitPaneContent.setDividerLocation(context.preferences.getJobsTaskDividerLocation());
        this.splitPaneToolsOrigin.setDividerLocation(context.preferences.getJobsOriginDividerLocation());

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
        configModel.setJobsConfigModel(configModel);
        configItems.setModel(configModel);

        configItems.getTableHeader().setUI(null);
        configItems.setTableHeader(null);
        scrollPaneConfig.setColumnHeaderView(null);

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

        repositories = getRepositories();
        loadConfigurations();
        context.navigator.enableDisableToolMenus(this, false);
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (context.navigator.isWorkerRunning() && currentJob != null)
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
                Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no")};
                int reply = JOptionPane.showOptionDialog(this,
                        context.cfg.gs("Z.cancel.all.changes"),
                        getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opts, opts[1]);
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
            if (configItems.isEditing())
                configItems.getCellEditor().stopCellEditing();

            Job job = (Job) configModel.getValueAt(index, 0);

            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + job.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                int answer = configModel.checkJobConflicts(job.getConfigName(), null, job.getInternalName(), false);
                if (answer >= 0)
                {
                    // add to delete list if file exists
                    File file = new File(job.getFullPath());
                    if (file.exists())
                    {
                        job.setDataHasChanged();
                        deletedTools.add(job);
                    }

                    configModel.removeRow(index);
                    if (index > configModel.getRowCount() - 1)
                        index = configModel.getRowCount() - 1;
                    configModel.fireTableDataChanged();
                    if (index >= 0)
                    {
                        configItems.changeSelection(index, 0, false, false);
                        loadTasks(index);
                    }
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionGenerateClicked(ActionEvent evt)
    {
        Generator generator = new Generator(context, false);
        generator.showDialog(this, currentJob, currentJob.getConfigName());
    }

    private void actionHelpClicked(MouseEvent e)
    {
        helpDialog = new NavHelp(this, this, context, context.cfg.gs("JobsUI.help"), "jobs_" + context.preferences.getLocale() + ".html", false);
        if (!helpDialog.fault)
            helpDialog.buttonFocus();
    }

    private void actionNewClicked(ActionEvent evt)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            Job job = new Job(context, context.cfg.gs("Z.untitled"));
            job.setDataHasChanged();
            configModel.addRow(new Object[]{job});
            this.currentJob = job;
            loadTasks(-1);

            if (!buttonAddOrigin.isEnabled())
            {
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
                buttonGenerate.setEnabled(true);
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
                boolean isSubscriber = Origins.makeOriginsFromSelected(context, this, origins); // can return null
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
                        for (Origin origin : origins)
                        {
                            origin.setLocation(context.cfg.makeRelativePathSubscriber(origin.getLocation()));
                        }
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
        String command = evt.getActionCommand();

        if (currentTask != null)
        {
            boolean anySelected = false;
            JComboBox combo = new JComboBox();
            final JTextField customAddress = new JTextField();
            final JLabel customLabel = new JLabel();
            final JPanel customPanel = new JPanel(new BorderLayout());
            int id = 0;
            String key = "";
            String path = "";
            int selectedCombo = -1;
            int selectedList = -1;
            String cachedName = "";
            String text = null;
            String title = null;
            String tip = null;
            Vector vector = new Vector();
            Vector vectorRepositories = new Vector();

            // Cached last task
            if (currentTask.isToolCachedOrigins(context))
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

            // title & tooltip
            if (command.equals("buttonPub"))
            {
                if (currentTool.isToolPubOrSub())
                {
                    title = context.cfg.gs("JobsUI.combo.select.publisher.or.subscriber");
                    tip = context.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
                }
                else
                {
                    title = context.cfg.gs("JobsUI.combo.select.publisher");
                    tip = context.cfg.gs("JobsUI.select.publisher.tooltip");
                }
            }
            else if (command.equals("buttonSub"))
            {
                title = context.cfg.gs("JobsUI.combo.select.subscriber");
                tip = context.cfg.gs("JobsUI.select.subscriber.tooltip");
            }
            else if (command.equals("buttonHints"))
            {
                title = context.cfg.gs("JobsUI.combo.select.hints");
                tip = context.cfg.gs("JobsUI.select.hint.address.tooltip");
            }

            combo.setToolTipText(tip);

            // Build combobox
            if (command.equals("buttonPub") || currentTool.isToolPubOrSub()) // -------------------- Publisher or both
            {
                combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.any.publisher"), ANY_PUBLISHER));
                if (currentTask.getPublisherKey().equals(Task.ANY_SERVER) || currentTask.getPublisherKey().isEmpty())
                {
                    selectedCombo = id - 1;
                    anySelected = true;
                }

                combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.publisher.specific"), SPECIFIC_PUBLISHER));
                if (currentTask.getPublisherKey().length() > 0 &&
                        !currentTask.getPublisherKey().equals(Task.ANY_SERVER) &&
                        !currentTask.getPublisherKey().equals(Task.CACHEDLASTTASK))
                {
                    selectedCombo = id - 1;
                    anySelected = false;
                    RepoMeta repoMeta = repositories.findMetaPath(currentTask.publisherPath);
                    if (repoMeta != null)
                        path = repoMeta.path;
                }
            }

            if (command.equals("buttonSub") || currentTool.isToolPubOrSub()) // ------------------- Subscriber or both
            {
                if (currentTool.isToolPublisher())
                {
                    if (!currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) ||
                        (currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) &&
                        !((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.SubscriberQuit) &&
                        !((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.Terminal)))
                    {
                        combo.addItem(new ComboItem(id++, context.cfg.gs("JobsUI.any.subscriber"), ANY_SUBSCRIBER));
                        if (currentTask.getSubscriberKey().equals(Task.ANY_SERVER))
                        {
                            selectedCombo = id - 1;
                            anySelected = true;
                        }

                        text = context.cfg.gs("JobsUI.subscriber.local");
                        combo.addItem(new ComboItem(id++, text, LOCAL));
                    }
                }

                // listeners use the Listen port
                if (!currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) ||
                        !((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.Listener))
                {
                    text = context.cfg.gs("JobsUI.subscriber.remote.host");
                    combo.addItem(new ComboItem(id++, text, REMOTE_HOST));
                }

                text = context.cfg.gs("JobsUI.subscriber.remote.listen");
                combo.addItem(new ComboItem(id++, text, REMOTE_LISTEN));

                text = context.cfg.gs("JobsUI.subscriber.remote.custom");
                combo.addItem(new ComboItem(id++, text, REMOTE_CUSTOM));

                if (currentTask.getSubscriberKey().length() > 0 &&
                        !currentTask.getSubscriberKey().equals(Task.ANY_SERVER) &&
                        !currentTask.getPublisherKey().equals(Task.CACHEDLASTTASK)) // check publisher key used to indicate last task
                {
                    selectedCombo = id - 4;
                    RepoMeta repoMeta = repositories.findMetaPath(currentTask.subscriberPath);
                    if (repoMeta != null)
                    {
                        if (currentTask.isSubscriberRemote())
                        {
                            if (currentTask.getSubscriberOverride().trim().length() == 0)
                                selectedCombo = id - 3;
                            else if (currentTask.getSubscriberOverride().trim().equals("true"))
                                selectedCombo = id - 2;
                            else
                                selectedCombo = id - 1; // custom
                        }
                        path = repoMeta.path;
                        anySelected = false;
                    }
                }
            }

            if (command.equals("buttonHints")) // --------------------------------------------------- Hint Tracker/Server
            {
                if (currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) &&
                        ((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.Publisher))
                {
                    text = context.cfg.gs(("JobsUI.hints.local.tracker"));
                    combo.addItem(new ComboItem(id++, text, LOCAL));
                    if (!currentTask.isHintsRemote() /*&& !((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.HintServer)*/)
                        selectedCombo = id - 1;
                }

                if (!currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) ||
                        !((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.HintServer))
                {
                    text = context.cfg.gs("JobsUI.hints.remote.host");
                    combo.addItem(new ComboItem(id++, text, REMOTE_HOST));
                    if (currentTask.isHintsRemote() && !currentTask.isHintsOverrideHost())
                        selectedCombo = id - 1;
                }

                text = context.cfg.gs("JobsUI.hints.remote.listen");
                combo.addItem(new ComboItem(id++, text, REMOTE_LISTEN));
                if (currentTask.isHintsRemote() && currentTask.isHintsOverrideHost())
                    selectedCombo = id - 1;

                RepoMeta repoMeta = repositories.findMetaPath(currentTask.hintsPath);
                if (repoMeta != null)
                    path = repoMeta.path;
            }

            if (selectedCombo < 0)
                selectedCombo = 0;

            // Build select vector
            int vi = -1;
            for (int i = 0; i < repositories.getRepoList().size(); ++i)
            {
                Repository repo = repositories.getRepoList().get(i);
                if (!repo.isDynamic()) // do not include dynamically-loaded repo
                {
                    text = repo.getLibraryData().libraries.description;
                    if (repo.getLibraryData().libraries.listen.equals(repo.getLibraryData().libraries.host))
                        text += " *";
                    vector.add(text);
                    vectorRepositories.add(repo);
                    ++vi;
                    if (repo.getJsonFilename().equals(path))
                        selectedList = vi;
                }
            }

            // Setup list
            JList<String> list = new JList(vector)
            {
                public String getToolTipText(MouseEvent me)
                {
                    String tip = "";
                    int index = locationToIndex(me.getPoint());
                    if (index >= 0 && index < vectorRepositories.size())
                    {
                        Repository repo = (Repository) vectorRepositories.get(index);
                        tip = context.cfg.gs("Navigator.labelHostInternet.text");
                        tip += " " + repo.getLibraryData().libraries.host + "\n";
                        tip += context.cfg.gs("Navigator.labelListenLan.text");
                        tip += " " + repo.getLibraryData().libraries.listen;
                    }
                    return tip;
                }
            };

            if (selectedList >= 0)
                list.setSelectedIndex(selectedList);

            if (/*command.equals("buttonHints") ||*/ anySelected)
                list.setEnabled(false);

            combo.setSelectedIndex(selectedCombo);

            // Add combobox listener
            Object customObject = null;
            customAddress.setToolTipText(context.cfg.gs("Navigator.menu.Open.valid.port.tooltip"));

            if (command.equals("buttonPub") || command.equals("buttonSub") || currentTool.isToolPubOrSub())
            {
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

                            // update custom state
                            customLabel.setEnabled(item.type == REMOTE_CUSTOM);
                            customAddress.setEnabled(item.type == REMOTE_CUSTOM);
                            if (item.type == REMOTE_CUSTOM)
                            {
                                if (customAddress.getText().isEmpty())
                                {
                                    String override = currentTask.subscriberOverride;
                                    if (override.trim().equals("true"))
                                        override = "";
                                    if (override.isEmpty())
                                        override = context.preferences.getLastOverrideSubscriber();
                                    if (override.trim().equals("true")) // check twice
                                        override = "";
                                    customAddress.setText(override);
                                    customAddress.requestFocus();
                                }
                            }
                        }
                    }
                });

                if (command.equals("buttonSub"))
                {
                    // set initial custom state
                    ComboItem item = (ComboItem) combo.getItemAt(selectedCombo);
                    customLabel.setEnabled(item.type == REMOTE_CUSTOM);
                    customAddress.setEnabled(item.type == REMOTE_CUSTOM);

                    customLabel.setText(context.cfg.gs("Navigator.labelCustom.text"));
                    if (item.type == REMOTE_CUSTOM)
                    {
                        String override = currentTask.subscriberOverride;
                        if (override.isEmpty())
                            override = context.preferences.getLastOverrideSubscriber();
                        if (override.trim().equals("true"))
                            override = "";
                        customAddress.setText(override);
                    }
                    customPanel.add(customLabel, BorderLayout.WEST);
                    customPanel.add(customAddress, BorderLayout.CENTER);
                    customObject = customPanel;
                }
            }

            String comment = context.cfg.gs("JobsUI.list.comment");

            // dialog
            JScrollPane pane = new JScrollPane();
            pane.setViewportView(list);
            list.requestFocus();

            Object[] params = {title, combo, pane, comment, customObject};
            while (true)
            {
                // prompt user
                int opt = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("JobsUI.title"), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    int selected = combo.getSelectedIndex();
                    ComboItem item = (ComboItem) combo.getItemAt(selected);

                    // what combobox item did they pick?
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
                            String msg = (currentTool.isToolPublisher() ? context.cfg.gs("JobsUI.select.publisher.tooltip") :
                                    (currentTool.isToolSubscriber() ? context.cfg.gs("JobsUI.select.subscriber.tooltip") :
                                            (currentTool.isToolPubOrSub() ? context.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip") :
                                                    context.cfg.gs("JobsUI.select.hint.address.tooltip"))));
                            JOptionPane.showMessageDialog(this, msg,
                                    context.cfg.gs("JobsUI.title"), JOptionPane.INFORMATION_MESSAGE);
                            continue;
                        }

                        Repository repo = (Repository) vectorRepositories.get(index);
                        key = repo.getLibraryData().libraries.key;
                        path = repo.getJsonFilename();
                    }

                    // Set remote and overrides
                    if (command.equals("buttonSub") || currentTool.isToolPubOrSub()) // subscriber
                    {
                        if (item.type == REMOTE_HOST || item.type == REMOTE_LISTEN || item.type == REMOTE_CUSTOM)
                        {
                            if (!currentTask.isSubscriberRemote())
                            {
                                currentTask.setSubscriberRemote(true);
                                currentJob.setDataHasChanged();
                            }

                            if (item.type == REMOTE_HOST)
                            {
                                if (!currentTask.getSubscriberOverride().isEmpty())
                                {
                                    currentJob.setDataHasChanged();
                                    currentTask.setSubscriberOverride("");
                                }
                            }
                            else if (!currentTask.getSubscriberOverride().trim().equals("true") && item.type == REMOTE_LISTEN)
                            {
                                currentTask.setSubscriberOverride("true");
                                currentJob.setDataHasChanged();
                            }
                            else if ((currentTask.getSubscriberOverride().isEmpty() ||
                                    currentTask.getSubscriberOverride().trim().equals("true") ||
                                    !currentTask.getSubscriberOverride().trim().equals(customAddress.getText())) && item.type == REMOTE_CUSTOM)
                            {
                                // validate the custom hostname:port
                                boolean bad = false;
                                if (customAddress.getText().isEmpty())
                                    bad = true;
                                else
                                {
                                    String host = Utils.parseHost(customAddress.getText());
                                    String port = Utils.parsePort(customAddress.getText());
                                    int p = -1;
                                    if (!port.isEmpty())
                                        p = Integer.parseInt(port);
                                    if (host.isEmpty() || p < 1 || p > 65535)
                                        bad = true;
                                }
                                if (bad)
                                {
                                    JOptionPane.showMessageDialog(this, context.cfg.gs("Navigator.menu.Open.valid.port"),
                                            context.cfg.gs("JobsUI.title"), JOptionPane.INFORMATION_MESSAGE);
                                    continue;
                                }

                                context.preferences.setLastOverrideSubscriber(customAddress.getText());
                                currentTask.setSubscriberOverride(customAddress.getText());
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

                            if (!currentTask.getSubscriberOverride().isEmpty())
                            {
                                currentTask.setSubscriberOverride("");
                                currentJob.setDataHasChanged();
                            }
                        }
                    }

                    if (command.equals("buttonHints")) // Hints
                    {
                        if (item.type == LOCAL)
                        {
                            if (currentTask.isHintsRemote())
                            {
                                currentTask.setHintsRemote(false);
                                currentJob.setDataHasChanged();
                            }
                            if (currentTask.isHintsOverrideHost())
                            {
                                currentTask.setHintsOverrideHost(false);
                                currentJob.setDataHasChanged();
                            }
                        }
                        else if (item.type == REMOTE_HOST || item.type == REMOTE_LISTEN)
                        {
                            if (!currentTask.isHintsRemote())
                            {
                                currentTask.setHintsRemote(true);
                                currentJob.setDataHasChanged();
                            }

                            if (!currentTask.isHintsOverrideHost() && item.type == REMOTE_LISTEN)
                            {
                                currentTask.setHintsOverrideHost(true);
                                currentJob.setDataHasChanged();
                            }
                            else if (currentTask.isHintsOverrideHost() && item.type == REMOTE_HOST)
                            {
                                currentTask.setHintsOverrideHost(false);
                                currentJob.setDataHasChanged();
                            }
                        }
                        else // anything else is not remote
                        {
                            if (currentTask.isHintsRemote())
                            {
                                currentTask.setHintsRemote(false);
                                currentJob.setDataHasChanged();
                            }

                            if (currentTask.isHintsOverrideHost())
                            {
                                currentTask.setHintsOverrideHost(false);
                                currentJob.setDataHasChanged();
                            }
                        }
                    }

                    // Populate task data
                    if (item.type == CACHED_LAST_TASK)
                    {
                        if (doesNotMatch(currentTask.getPublisherKey(), key) || !currentTask.getPublisherPath().equals(path))
                        {
                            currentTask.setPublisherKey(key); // use publisher key only to indicate last task
                            currentTask.setPublisherPath("");
                            currentTask.setSubscriberKey("");
                            currentTask.setSubscriberPath("");
                            currentJob.setDataHasChanged();
                        }
                    }
                    else
                    {
                        if (currentTool.isToolPubOrSub())
                        {
                            if (item.type == ANY_PUBLISHER || item.type == SPECIFIC_PUBLISHER) // a publisher selection
                            {
                                if (doesNotMatch(currentTask.getPublisherKey(), key) || !currentTask.getPublisherPath().equals(path))
                                {
                                    currentTask.setPublisherKey(key);
                                    if (item.type == ANY_PUBLISHER)
                                        currentTask.setPublisherPath("");
                                    else
                                        currentTask.setPublisherPath(path);
                                    currentTask.setSubscriberKey("");
                                    currentTask.setSubscriberPath("");
                                    currentJob.setDataHasChanged();
                                }
                            }
                            else if (item.type == ANY_SUBSCRIBER || item.type == LOCAL ||
                                    item.type == REMOTE_HOST || item.type == REMOTE_LISTEN) // a subscriber selection
                            {
                                if (doesNotMatch(currentTask.getSubscriberKey(), key) || !currentTask.getSubscriberPath().equals(path))
                                {
                                    currentTask.setSubscriberKey(key);
                                    if (item.type == ANY_SUBSCRIBER)
                                        currentTask.setSubscriberPath("");
                                    else
                                        currentTask.setSubscriberPath(path);
                                    currentTask.setPublisherKey("");
                                    currentTask.setPublisherPath("");
                                    currentJob.setDataHasChanged();
                                }
                            }
                        }
                        else if (command.equals("buttonPub"))
                        {
                            if (doesNotMatch(currentTask.getPublisherKey(), key) || !currentTask.getPublisherPath().equals(path))
                            {
                                currentTask.setPublisherKey(key);
                                currentTask.setPublisherPath(path);
                                currentJob.setDataHasChanged();
                            }
                        }
                        else // subscriber or Hints
                        {
                            if (command.equals("buttonSub"))
                            {
                                if (doesNotMatch(currentTask.getSubscriberKey(), key) || !currentTask.getSubscriberPath().equals(path))
                                {
                                    currentTask.setSubscriberKey(key);
                                    currentTask.setSubscriberPath(path);
                                    currentJob.setDataHasChanged();
                                }
                            }
                            else if (command.equals("buttonHints"))
                            {
                                if (doesNotMatch(currentTask.getHintsKey(), key) || !currentTask.getHintsPath().equals(path))
                                {
                                    currentTask.setHintsKey(key);
                                    currentTask.setHintsPath(path);
                                    currentJob.setDataHasChanged();
                                }
                            }
                        }
                    }

                    // 2024-06-13 populate newly-added hintsKey
                    if (currentTask.getHintsKey() == null || currentTask.getHintsKey().isEmpty())
                        currentTask.setHintsKey("");

                   loadOrigins(currentTask);
                    break;
                }
                else
                    break;
            }
        }
    }

    private void actionRunClicked(ActionEvent evt)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Job job = (Job) configModel.getValueAt(index, 0);
            if (job.isDataChanged())
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.save.then.run"), context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                return;
            }
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

            try
            {
                // make the String list for display
                ArrayList<String> toolNames = new ArrayList<>();
                for (AbstractTool tool : toolList)
                {
                    // exclude deleted but not "saved", i.e. removed yet
                    boolean deleted = false;
                    for (int i = 0; i < deletedTools.size(); ++i)
                    {
                        if (tool.getFullPath().equals(deletedTools.get(i).getFullPath()))
                        {
                            deleted = true;
                            break;
                        }
                    }
                    if (!deleted)
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
                return;
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

                if (index >= 0)
                {
                    currentTask = new Task(tool.getInternalName(), tool.getConfigName());
                    currentTask.setContext(context);
                    try
                    {
                        currentTool = currentTask.getTool();
                    }
                    catch (Exception e)
                    {
                        String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
                        logger.error(msg);
                        JOptionPane.showMessageDialog(this, msg,
                                context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                    }

                    currentJob.getTasks().add(currentTask);
                    currentJob.setDataHasChanged();
                    loadTasks(-1);
                    listTasks.setSelectedIndex(currentJob.getTasks().size() - 1);
                    loadOrigins(currentTask);
                }
                else
                    listTasks.requestFocus();
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
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((Job) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }

        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
    }

    public boolean checkForChanges()
    {
        for (int i = 0; i < deletedTools.size(); ++i)
        {
            if (deletedTools.get(i).isDataChanged())
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
    }

    private boolean doesNotMatch(String value, String compare)
    {
        boolean sense = true; // does not match
        if (value != null && !value.isEmpty() && compare != null && !compare.isEmpty() && value.equals(compare))
            sense = false; // matches
        return sense;
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
                else if (task.isToolCachedOrigins(context) && task != currentTask)
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
            {
                task.setContext(context);
                return task;
            }
        }
        return null;
    }

    private int findTaskIndex(String name)
    {
        for (int i = 0; i < currentJob.getTasks().size(); ++i)
        {
            Task task = currentJob.getTasks().get(i);
            if (task.getConfigName().equals(name))
            {
                task.setContext(context);
                return i;
            }
        }
        return -1;
    }

    private AbstractTool findTool(Task task)
    {
        for (int i = 0; i < toolList.size(); ++i)
        {
            AbstractTool tool = toolList.get(i);
            if (task.getConfigName().equals(tool.getConfigName()))
                return tool;
        }
        return null;
    }

    public JTable getConfigItems()
    {
        return configItems;
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
        try
        {
            // load Job tools
            jobsHandler = new Jobs(context);
            toolList = jobsHandler.loadAllJobs(); // creates the ArrayList

            // add all the other tools
            toolList = context.tools.loadAllTools(context, null, toolList);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg,
                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }

        configModel.setToolList(toolList);
        configModel.loadJobsConfigurations(this, configModel);

        if (configModel.getRowCount() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonGenerate.setEnabled(false);
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

        context.navigator.loadJobsMenu();
    }

    private void loadEmpty()
    {
        labelPub.setVisible(false);
        buttonPub.setVisible(false);
        labelSub.setVisible(false);
        buttonSub.setVisible(false);
        labelHints.setVisible(false);
        buttonHints.setVisible(false);
        enableDisableOrigins(false);
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
            }
        }

        if (currentJob.getTasks().size() == 0)
        {
            DefaultListModel<String> omodel = new DefaultListModel<String>();
            listOrigins.setModel(omodel);
            loadEmpty();
        }

        listTasks.setModel(model);
        if (model.size() > 0)
            listTasks.setSelectedIndex(0);

        if (model.size() == 0)
        {
            buttonAddTask.setEnabled(true);
            buttonTaskUp.setEnabled(false);
            buttonTaskDown.setEnabled(false);
            buttonRemoveTask.setEnabled(false);
        }
        else
        {
            buttonAddTask.setEnabled(true);
            if (model.size() > 1)
            {
                buttonTaskUp.setEnabled(true);
                buttonTaskDown.setEnabled(true);
            }
            else
            {
                buttonTaskUp.setEnabled(false);
                buttonTaskDown.setEnabled(false);
            }
            if (model.size() > 0)
                buttonRemoveTask.setEnabled(true);
        }
    }

    private void loadOrigins(Task task)
    {
        String key = "";
        String value = "";
        DefaultListModel<String> model = new DefaultListModel<String>();

        try
        {
            currentTask = task;
            currentTask.setContext(context);
            currentTool = currentTask.getTool();
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg,
                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        labelPub.setVisible(false);
        buttonPub.setVisible(false);
        labelSub.setVisible(false);
        buttonSub.setVisible(false);
        labelHints.setVisible(false);
        buttonHints.setVisible(false);

        if (currentTool == null)
        {
            if (!currentTask.getInternalName().equals(Job.INTERNAL_NAME))
            {
                labelPub.setVisible(true);
                labelPub.setText(context.cfg.gs("JobsUI.tool.not.found") + currentTask.getConfigName());
            }
            enableDisableOrigins(false);
            listOrigins.setModel(model);
            return;
        }

        boolean needPublisher = currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) &&
                ((OperationsTool) currentTool).isToolSubscriber() &&
                ((OperationsTool) currentTool).getOptAuthKeys().isEmpty();
        boolean needSubscriber = false;
        boolean needHints = currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) &&
                ((((OperationsTool) currentTool).isToolHintServer() || ((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.StatusQuit)) ||
                (!((OperationsTool) currentTool).getOptKeys().isEmpty()));

        if (needPublisher || currentTool.isToolPublisher()) // -------------------------------------------------------- Publisher
        {
            value = "";
            boolean isSub = false;
            labelPub.setVisible(true);
            key = currentTask.getPublisherKey();
            if (currentTool.isToolPubOrSub() && key.trim().length() == 0) // no publisher key, use subscriber
            {
                isSub = true;
                key = currentTask.getSubscriberKey(); // get the OR key
            }

            if (key.trim().length() == 0)
            {
                if (currentTool.isToolPubOrSub())
                    value = context.cfg.gs("JobsUI.select.publisher.or.subscriber");
                else
                {
                    value = context.cfg.gs("JobsUI.select.publisher");
                    needSubscriber = true;
                }
            }
            else if (key.equals(Task.ANY_SERVER))
            {
                if (!isSub)
                {
                    value = context.cfg.gs("JobsUI.any.publisher");
                    needSubscriber = true;
                }
                else
                    value = context.cfg.gs("JobsUI.any.subscriber");
            }
            else if (key.equals(Task.CACHEDLASTTASK))
            {
                String name = "";
                int taskIndex = findTaskIndex(currentTask.getConfigName());
                if (taskIndex >= 0)
                {
                    String cachedName = findCachedLastTask(currentJob, taskIndex);
                    if (cachedName.length() > 0)
                        name = cachedName;
                }
                if (!name.isEmpty())
                    value = context.cfg.gs("JobsUI.cached.task") + name;
                else
                    value = currentTask.getConfigName() + context.cfg.gs("JobsUI.cached.task.not.found");
            }
            else
            {
                RepoMeta repoMeta = repositories.findMetaAdd(context, key, (!isSub ? currentTask.publisherPath : currentTask.subscriberPath),
                        (isSub ? Repository.SUBSCRIBER : Repository.PUBLISHER));
                if (repoMeta != null)
                {
                    if (!isSub)
                    {
                        value = context.cfg.gs("Z.publisher");
                        value += ": " + repoMeta.description;
                        needSubscriber = true;
                    }
                    else
                    {
                        if (currentTask.isSubscriberRemote())
                            value = context.cfg.gs("Z.remote.uppercase");
                        else
                            value = context.cfg.gs("Z.local.uppercase");
                        value += context.cfg.gs("Z.subscriber");
                        value += ": " + repoMeta.description;
                        if (currentTask.isSubscriberRemote())
                        {
                            if (currentTask.getSubscriberOverride().trim().isEmpty())
                                value += context.cfg.gs("Z.host");
                            else if (currentTask.getSubscriberOverride().trim().equals("true"))
                                value += context.cfg.gs("Z.listen");
                            else
                                value += " (" + context.cfg.gs(currentTask.getSubscriberOverride().trim()) + ")";
                        }
                    }
                }
                else
                    value += key + context.cfg.gs("Z.not.found");
            }
            labelPub.setText(value);
            labelPub.setToolTipText(value);

            buttonPub.setVisible(true);
            if (currentTool.isToolPubOrSub())
                value = context.cfg.gs("JobsUI.select.publisher.or.subscriber.tooltip");
            else
                value = context.cfg.gs("JobsUI.select.publisher.tooltip");
            buttonPub.setToolTipText(value);

            if (needSubscriber)
            {
                // not for these things
                if (currentTool.isToolPubOrSub() ||
                        (currentTask.getInternalName().equals(OperationsTool.INTERNAL_NAME) &&
                                (((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.HintServer) ||
                                ((OperationsTool) currentTool).getCard().equals(OperationsTool.Cards.StatusQuit))))
                    needSubscriber = false;
            }
        }

        if (needSubscriber || currentTool.isToolSubscriber()) // ------------------------------------- Subscriber
        {
            value = "";
            labelSub.setVisible(true);

            key = currentTask.getSubscriberKey();
            if (key.trim().length() == 0)
                value = context.cfg.gs("JobsUI.select.subscriber");
            else if (key.equals(Task.ANY_SERVER))
                value = context.cfg.gs("JobsUI.any.subscriber");
            else
            {
                RepoMeta repoMeta = repositories.findMetaAdd(context, key, currentTask.getSubscriberPath(), Repository.SUBSCRIBER);
                if (repoMeta != null)
                {
                    if (currentTask.isSubscriberRemote())
                        value = context.cfg.gs("Z.remote.uppercase");
                    else
                        value = context.cfg.gs("Z.local.uppercase");
                    value += context.cfg.gs("Z.subscriber");
                    value += ": " + repoMeta.description;
                    if (currentTask.isSubscriberRemote())
                    {
                        if (currentTask.getSubscriberOverride().trim().isEmpty())
                            value += context.cfg.gs("Z.host");
                        else if (currentTask.getSubscriberOverride().trim().equals("true"))
                            value += context.cfg.gs("Z.listen");
                        else
                            value += " (" + context.cfg.gs(currentTask.getSubscriberOverride().trim()) + ")";
                    }
                }
                else
                    value += key + context.cfg.gs("Z.not.found");
            }
            labelSub.setText(value);
            labelSub.setToolTipText(value);

            buttonSub.setVisible(true);
            value = context.cfg.gs("JobsUI.select.subscriber.tooltip");
            buttonSub.setToolTipText(value);

            if (currentTool.isToolSubscriber())
                needHints = false;
        }

        if (needHints || currentTool.isToolHintServer()) // ------------------------------------------ Hint Server
        {
            value = "";
            labelHints.setVisible(true);

            key = currentTask.getHintsKey();
            if (key.trim().length() == 0)
                value = context.cfg.gs("JobsUI.select.hint.address");
            else
            {
                RepoMeta repoMeta = repositories.findMetaAdd(context, key, currentTask.getSubscriberPath(), Repository.HINT_SERVER);
                if (repoMeta != null)
                {
                    if (currentTask.isHintsRemote() || currentTask.isHintsOverrideHost())
                    {
                        value = context.cfg.gs("Z.remote.uppercase");
                        value += context.cfg.gs("Z.hint.server");
                    }
                    else
                    {
                        value = context.cfg.gs("Z.local.uppercase");
                        value += context.cfg.gs("Z.hint.tracker");
                    }
                    value += ": " + repoMeta.description;
                    if (currentTask.isHintsRemote() || currentTask.isHintsOverrideHost())
                    {
                        if (currentTask.isHintsOverrideHost() || currentTool.isToolHintServer())
                            value += context.cfg.gs("Z.listen");
                        else
                            value += context.cfg.gs("Z.host");
                    }
                }
                else
                    value += key + context.cfg.gs("Z.not.found");
            }
            labelHints.setText(value);
            labelHints.setToolTipText(value);

            buttonHints.setVisible(true);
            value = context.cfg.gs("JobsUI.select.hint.address.tooltip");
            buttonHints.setToolTipText(value);
        }

        enableDisableOrigins(currentTool.isToolOriginsUsed());

        if (currentTool.isToolOriginsUsed() && currentTask.getOrigins().size() > 0)
        {
            for (int i = 0; i < currentTask.getOrigins().size(); ++i)
            {
                Origin origin = currentTask.getOrigins().get(i);
                String ot = getOriginType(origin.getType());
                String path = origin.getLocation();
                if (path.matches("^\\\\[a-zA-Z]:.*") || path.matches("^/[a-zA-Z]:.*"))
                    path = path.substring(1);
                String id = getOriginType(origin.getType()) + (ot.length() > 0 ? ": " : "") + path;
                model.addElement(id);
            }
            if (listOrigins.getModel().getSize() > 0)
                listOrigins.setSelectedIndex(0);
        }

        listOrigins.setModel(model);
    }

    /**
     * Used by the Run button
     *
     * @param job
     */
    public void processJob(Job job)
    {
        // validate job tasks and origins
        String status = job.validate(context.cfg);
        if (status.length() == 0)
        {
            // make dialog pieces
            String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
            JCheckBox checkbox = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
            checkbox.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
            checkbox.setSelected(context.preferences.isDefaultDryrun());
            Object[] params = {message, checkbox};

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
            isDryRun = checkbox.isSelected();
            if (reply == JOptionPane.YES_OPTION)
            {
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
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    context.navigator.disableComponent(true, getContentPane());
                    context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                    context.mainFrame.menuItemFileQuit.setEnabled(true);
                    cancelButton.setEnabled(true);
                    cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    labelHelp.setEnabled(true);
                    labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    context.navigator.setBlockingProcessRunning(true);
                    context.navigator.setWorkerRunning(true);
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
                            }  // try STARTED
                        }
                    });

                    worker.execute();

                    JScrollBar vertical = context.mainFrame.scrollPaneLog.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());

                    try
                    {
                        logger.debug(context.cfg.gs("Jobs.waiting.for.job.thread.to.complete"));
                        worker.wait();
                    }
                    catch (Exception e)
                    {
                    }
                    logger.debug(context.cfg.gs("Jobs.waiting.is.over.for.the.job.thread"));
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
                if (job.usesPublisher() && context.publisherRepo != null)
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.publisher"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false, false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false, false);
                }
                if (job.usesSubscriber() && context.subscriberRepo != null)
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.subscriber"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.cfg.isRemoteOperation(), false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.cfg.isRemoteOperation(), false);
                }
            }

            if (context.progress != null)
            {
                context.progress.done();
                context.progress.dispose();
                context.progress = null;
            }

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
        {
            String msg = java.text.MessageFormat.format(context.cfg.gs(context.fault ? "Job.failed.job" : "Job.completed.job"),
                    job.getConfigName() + (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : ""));
            logger.info(msg);
            context.mainFrame.labelStatusMiddle.setText(msg);
            context.main.stopVerbiage();
        }

        context.navigator.setWorkerRunning(false);
        context.navigator.disableGui(false);
        context.navigator.disableComponent(false, getContentPane());
        context.navigator.setBlockingProcessRunning(false);  // do last
    }

    private boolean saveConfigurations()
    {
        // remove any deleted tools JSON configuration file
        for (int i = 0; i < deletedTools.size(); ++i)
        {
            AbstractTool tool = deletedTools.get(i);
            File del = new File(tool.getFullPath());
            if (del.exists())
                del.delete();
        }
        deletedTools = new ArrayList<AbstractTool>();

        configModel.saveJobsConfigurations(configModel);
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

        for (int i = 0; i < job.getTasks().size(); ++i)
        {
            Task task = job.getTasks().get(i);
            if (!task.getInternalName().equals(Job.INTERNAL_NAME) && !task.getInternalName().equals(SleepTool.INTERNAL_NAME))
            {
                AbstractTool tool = context.tools.getTool(task.getInternalName(), task.getConfigName());
                if (tool != null)
                {
                    if (tool.isToolCachedOrigins())
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

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    private void windowHidden(ComponentEvent e)
    {
        context.navigator.enableDisableToolMenus(this, true);
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

    // <editor-fold desc="Generated code (Fold)">-
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
        labelHints = new JLabel();
        buttonHints = new JButton();
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
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                windowHidden(e);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JobsUI.this.windowClosing(e);
            }
        });
        var contentPane = getContentPane();
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
                        buttonCopy.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("JobsUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("JobsUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("Z.run.ellipsis"));
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
                        labelHelp.setToolTipText(context.cfg.gs("JobsUI.help"));
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
                                        ((GridBagLayout)panelPubSub.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                        ((GridBagLayout)panelPubSub.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                                        ((GridBagLayout)panelPubSub.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

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
                                        buttonPub.setActionCommand("buttonPub");
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
                                        buttonSub.setActionCommand("buttonSub");
                                        buttonSub.addActionListener(e -> actionPubSubClicked(e));
                                        panelPubSub.add(buttonSub, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));

                                        //---- labelHints ----
                                        labelHints.setMaximumSize(new Dimension(24, 18));
                                        labelHints.setMinimumSize(new Dimension(24, 18));
                                        labelHints.setPreferredSize(new Dimension(24, 18));
                                        labelHints.setFont(labelHints.getFont().deriveFont(labelHints.getFont().getSize() + 1f));
                                        panelPubSub.add(labelHints, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 4, 0, 8), 0, 0));

                                        //---- buttonHints ----
                                        buttonHints.setText("...");
                                        buttonHints.setMaximumSize(new Dimension(32, 24));
                                        buttonHints.setMinimumSize(new Dimension(32, 24));
                                        buttonHints.setPreferredSize(new Dimension(32, 24));
                                        buttonHints.setVerticalTextPosition(SwingConstants.TOP);
                                        buttonHints.setFont(buttonHints.getFont().deriveFont(buttonHints.getFont().getStyle() | Font.BOLD));
                                        buttonHints.setHorizontalTextPosition(SwingConstants.LEADING);
                                        buttonHints.setIconTextGap(0);
                                        buttonHints.setActionCommand("buttonHints");
                                        buttonHints.addActionListener(e -> actionPubSubClicked(e));
                                        panelPubSub.add(buttonHints, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
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
                                        buttonAddOrigin.setMargin(new Insets(0, -10, 0, -10));
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
                                        buttonRemoveOrigin.setMargin(new Insets(0, -10, 0, -10));
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
                            buttonAddTask.setMargin(new Insets(0, -10, 0, -10));
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
                            buttonRemoveTask.setMargin(new Insets(0, -10, 0, -10));
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
    public JLabel labelHints;
    public JButton buttonHints;
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
