package com.corionis.els.gui.tools.operations;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.ConfigModel;
import com.corionis.els.gui.jobs.Conflict;
import com.corionis.els.gui.util.DisableJListSelectionModel;
import com.corionis.els.jobs.Job;
import com.corionis.els.jobs.Task;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

@SuppressWarnings(value = "unchecked")

public class OperationsUI extends AbstractToolDialog
{
    private JComboBox comboBoxMode;
    private JTable configItems;
    private Context context;
    private ConfigModel configModel;
    private int currentConfigIndex = -1;
    private OperationsTool currentTool;
    private String displayName;
    private boolean inUpdateOnChange = false;
    private boolean pickerAnyFile = false;
    private boolean pickerKeys = false;
    private boolean pickerFileMustExist = false;
    private File lastFile;
    private JList<String> libJList = null;
    private NavHelp helpDialog;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private Mode[] modes;
    private SwingWorker<Void, Void> worker;
    private OperationsTool workerOperation = null;

    public OperationsUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        this.displayName = context.cfg.gs("Operations.displayName");
        initComponents();
        initialize();
    }

    private void actionCancelClicked(ActionEvent e)
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

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            OperationsTool original = (OperationsTool) configModel.getValueAt(index, 0);
            String rename = original.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                OperationsTool copy = original.clone();
                copy.setConfigName(rename);
                copy.setDataHasChanged();
                configModel.addRow(new Object[]{copy});

                currentConfigIndex = configModel.getRowCount() - 1;
                loadOptions(currentConfigIndex);
                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, displayName, JOptionPane.WARNING_MESSAGE);
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

            OperationsTool tool = (OperationsTool) configModel.getValueAt(index, 0);
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + tool.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                int answer = configModel.checkJobConflicts(tool.getConfigName(), null, tool.getInternalName(), false);
                if (answer >= 0)
                {
                    // add to delete list if file exists
                    File file = new File(tool.getFullPath());
                    if (file.exists())
                    {
                        deletedTools.add(tool);
                        currentTool.setDataHasChanged();
                    }

                    configModel.removeRow(index);
                    if (index > configModel.getRowCount() - 1)
                        index = configModel.getRowCount() - 1;
                    currentConfigIndex = index;
                    configModel.fireTableDataChanged();
                    if (configModel.getRowCount() > 0)
                    {
                        configItems.changeSelection(index, 0, false, false);
                        loadOptions(index);
                    }
                    else
                    {
                        ((CardLayout) panelOperationCards.getLayout()).show(panelOperationCards, "gettingStarted");
                        labelOperationMode.setText("");
                        buttonCopyOperation.setEnabled(false);
                        buttonDeleteOperation.setEnabled(false);
                        buttonOperationSave.setEnabled(false);
                        currentConfigIndex = 0;
                    }
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        helpDialog = new NavHelp(this, this, context, context.cfg.gs("OperationsUI.help"), "operations_" + context.preferences.getLocale() + ".html", false);
        if (!helpDialog.fault)
            helpDialog.buttonFocus();
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            String message = context.cfg.gs("OperationsUI.mode.select.type");
            String line = context.cfg.gs(("OperationsUI.mode.select.use.publish.for.navigator"));
            Object[] params = {message, line, comboBoxMode};
            comboBoxMode.setSelectedIndex(0);

            // get ELS operation/mode
            int opt = JOptionPane.showConfirmDialog(this, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                currentTool = new OperationsTool(context);
                Mode mode = modes[comboBoxMode.getSelectedIndex()];
                currentTool.setConfigName(context.cfg.gs("Z.untitled"));
                currentTool.setOperation(mode.operation);
                currentTool.setCard(mode.card);
                currentTool.setDataHasChanged();
                initNewCard();

                buttonCopyOperation.setEnabled(true);
                buttonDeleteOperation.setEnabled(true);
                buttonOperationSave.setEnabled(true);

                configModel.addRow(new Object[]{currentTool});
                currentConfigIndex = configModel.getRowCount() - 1;
                loadOptions(currentConfigIndex);

                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
                configItems.requestFocus();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), displayName, JOptionPane.WARNING_MESSAGE);
        }
    }

    public void actionOperationAddRowClicked(ActionEvent e)
    {
        if (currentTool != null)
        {
            if (e.getSource().getClass().equals(JButton.class))
            {
                JButton button = (JButton) e.getSource();
                libraryPicker(button);
            }
        }
    }

    public void actionOperationRemoveRowClicked(ActionEvent e)
    {
        if (currentTool != null)
        {
            if (e.getSource().getClass().equals(JButton.class))
            {
                int cardVar = 0;
                JButton button = (JButton) e.getSource();
                if (button.getName().toLowerCase().equals("removeincexc"))
                    cardVar = 1;
                else if (button.getName().toLowerCase().equals("removeexc"))
                    cardVar = 2;

                int indices[] = {};
                if (cardVar == 1 && listOperationIncludeExclude.getModel().getSize() > 0)
                    indices = listOperationIncludeExclude.getSelectedIndices();
                else if (cardVar == 2 && listOperationExclude.getModel().getSize() > 0)
                    indices = listOperationExclude.getSelectedIndices();
                if (indices.length > 0)
                {
                    if (cardVar == 1)
                        listOperationIncludeExclude.requestFocus();
                    else if (cardVar == 2)
                        listOperationExclude.requestFocus();
                    int count = indices.length;

                    // confirm deletions
                    int reply = JOptionPane.showConfirmDialog(this,
                            MessageFormat.format(context.cfg.gs("OperationsUI.are.you.sure.you.want.delete.entries"), count), displayName,
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        java.util.List<String> selections = listOperationIncludeExclude.getSelectedValuesList();
                        Arrays.sort(indices);

                        // remove in reverse sorted order so indices do not change
                        boolean changed = false;
                        if (cardVar == 1 && libraryRemover(1, true, indices))
                            changed = true;
                        if (libraryRemover(2, false, indices))
                            changed = true;

                        if (changed)
                        {
                            currentTool.setDataHasChanged();
                            if (cardVar == 1)
                                loadIncludeExcludeList();
                            else if (cardVar == 2)
                                loadExcludeList();
                        }
                    }
                }
            }
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

    private void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((OperationsTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
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
            if (((OperationsTool) configModel.getValueAt(i, 0)).isDataChanged())
                return true;
        }
        return false;
    }

    /**
     * Check for Hint Keys removal conflict in Hint-enabled Jobs
     *
     * @return int -1 = cancel, 0 = no conflicts, 1 = cleared
     */
    public int checkHintKeysConflicts()
    {
        int answer = 0;
        JList<String> conflictJList = new JList<String>();

        try
        {
            // get list of conflicts
            int count = 0;
            ArrayList<Conflict> conflicts = new ArrayList<>();
            ConfigModel jobsConfigModel = configModel.getJobsConfigModel();

            for (int i = 0; i < jobsConfigModel.getRowCount(); ++i)
            {
                Job job = (Job) jobsConfigModel.getValueAt(i, 0);

                for (int j = 0; j < job.getTasks().size(); ++j)
                {
                    Task task = job.getTasks().get(j);
                    if (task.getConfigName().equals(currentTool.getConfigName()) && !task.getHintsKey().isEmpty())
                    {
                        Conflict conflict = new Conflict();
                        conflict.job = job;
                        conflict.newName = null;
                        conflict.taskNumber = j;
                        conflicts.add(conflict);
                        ++count;
                    }
                }
            }

            if (count > 0)
            {
                answer = -1;

                // make list of displayable conflict names
                ArrayList<String> conflictNames = new ArrayList<>();
                for (Conflict conflict : conflicts)
                {
                    conflictNames.add(conflict.toString(context));
                }
                Collections.sort(conflictNames);

                // add the Strings to the JList model
                DefaultListModel<String> dialogList = new DefaultListModel<String>();
                for (String name : conflictNames)
                {
                    dialogList.addElement(name);
                }
                conflictJList.setModel(dialogList);
                conflictJList.setSelectionModel(new DisableJListSelectionModel());

                String message = MessageFormat.format(context.cfg.gs("Jobs.references.for.found.in.jobs"), currentTool.getConfigName());
                JScrollPane pane = new JScrollPane();
                pane.setViewportView(conflictJList);
                String question = context.cfg.gs("OperationsUI.clear.hints");
                Object[] params = {message, pane, question};

                int opt = JOptionPane.showConfirmDialog(this, params, getTitle(), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    answer = 1;

                    for (Conflict conflict : conflicts)
                    {
                        Job job = (Job) conflict.job;
                        Task task = job.getTasks().get(conflict.taskNumber);
                        task.setContext(context);
                        task.setHintsRemote(false);
                        task.setHintsOverrideHost(false);
                        task.setHintsKey("");
                        task.setHintsPath("");
                        job.setDataHasChanged();
                    }
                }
            }
        }
        catch (Exception e)
        {
            answer = -1;
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, getTitle(), JOptionPane.ERROR_MESSAGE);
        }

        return answer;
    }

    private void filePicker(JButton button)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileHidingEnabled(false);
        fc.setDialogTitle(button.getToolTipText());

        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                if (pickerKeys)
                    return (file.getName().toLowerCase().endsWith(".keys"));
                if (!pickerAnyFile)
                    return (file.getName().toLowerCase().endsWith(".json"));
                return true;
            }

            @Override
            public String getDescription()
            {
                String desc = "";
                pickerKeys = false;
                switch (button.getName().toLowerCase())
                {
                    case "authkeys":
                    case "authkeys3":
                        desc = context.cfg.gs("OperationsUI.els.auth.keys.file");
                        pickerAnyFile = false;
                        pickerFileMustExist = true;
                        pickerKeys = true;
                        break;
                    case "blacklist":
                    case "blacklist3":
                        desc = context.cfg.gs("OperationsUI.els.blacklist.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = true;
                        break;
                    case "ipwhitelist":
                    case "ipwhitelist3":
                        desc = context.cfg.gs("OperationsUI.els.whitelist.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = true;
                        break;
                    case "targets":
                    case "targets2":
                        desc = context.cfg.gs("OperationsUI.els.targets.file.json");
                        pickerAnyFile = false;
                        pickerFileMustExist = true;
                        break;
                    case "mismatches":
                        desc = context.cfg.gs("OperationsUI.els.mismatches.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = false;
                        break;
                    case "whatsnew":
                        desc = context.cfg.gs("OperationsUI.els.what.s.new.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = false;
                        break;
                    case "exporttext":
                        desc = context.cfg.gs("OperationsUI.els.export.text.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = false;
                        break;
                    case "exportitems":
                        desc = context.cfg.gs("OperationsUI.els.export.items.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = false;
                        break;
                    case "hintkeys":
                    case "hintkeys2":
                    case "hintkeys3":
                        desc = context.cfg.gs("OperationsUI.els.hint.keys.file");
                        pickerAnyFile = true;
                        pickerFileMustExist = true;
                        pickerKeys = true;
                        break;
                }
                return desc;
            }
        });

        String fileName = "";
        switch (button.getName().toLowerCase())
        {
            case "authkeys":
                fileName = textFieldOperationAuthKeys.getText();
                break;
            case "authkeys3":
                fileName = textFieldOperationAuthKeys3.getText();
                break;
            case "blacklist":
                fileName = textFieldOperationBlacklist.getText();
                break;
            case "blacklist3":
                fileName = textFieldOperationBlacklist3.getText();
                break;
            case "ipwhitelist":
                fileName = textFieldOperationIpWhitelist.getText();
                break;
            case "ipwhitelist3":
                fileName = textFieldOperationIpWhitelist3.getText();
                break;
            case "targets":
                fileName = textFieldOperationTargets.getText();
                break;
            case "targets2":
                fileName = textFieldOperationTargets2.getText();
                break;
            case "mismatches":
                fileName = textFieldOperationMismatches.getText();
                break;
            case "whatsnew":
                fileName = textFieldOperationWhatsNew.getText();
                break;
            case "exporttext":
                fileName = textFieldOperationExportText.getText();
                break;
            case "exportitems":
                fileName = textFieldOperationExportItems.getText();
                break;
            case "hintkeys":
                fileName = textFieldOperationHintKeys.getText();
                break;
            case "hintkeys2":
                fileName = textFieldOperationHintKeys2.getText();
                break;
            case "hintkeys3":
                fileName = textFieldOperationHintKeys3.getText();
                break;
        }

        File dir;
        File file;
        if (fileName.length() > 0)
        {
            dir = new File(filePickerDirectory(fileName));
            try
            {
                fc.setCurrentDirectory(dir.getCanonicalFile());
            }
            catch (IOException e)
            {
                fc.setCurrentDirectory(dir);
            }

            file = new File(fileName);
            fc.setSelectedFile(file);
        }
        else if (lastFile != null)
        {
            fc.setCurrentDirectory(lastFile);
        }
        else
        {
            // default to ELS home
            fc.setCurrentDirectory(new File(context.cfg.getWorkingDirectory()));
        }

        fc.setDialogType(pickerFileMustExist ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

        while (true)
        {
            int selection = fc.showOpenDialog(this);
            if (selection == JFileChooser.APPROVE_OPTION)
            {

                lastFile = fc.getCurrentDirectory();
                file = fc.getSelectedFile();

                // sanity checks
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(this,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (pickerFileMustExist && !file.exists())
                {
                    JOptionPane.showMessageDialog(this,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (pickerFileMustExist && !file.canWrite())
                {
                    JOptionPane.showMessageDialog(this,
                            context.cfg.gs("Navigator.open.error.file.not.writable") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }


                // make path relative if possible
                String path = "";
                if (!file.getPath().equals(context.cfg.getWorkingDirectory()) && file.getPath().startsWith(context.cfg.getWorkingDirectory()))
                    path = file.getPath().substring(context.cfg.getWorkingDirectory().length() + 1);
                else
                    path = file.getPath();

                // save value & fire updateOnChange()
                switch (button.getName().toLowerCase())
                {
                    // textFieldOperation
                    case "authkeys":
                        textFieldOperationAuthKeys.setText(path);
                        textFieldOperationAuthKeys.postActionEvent();
                        break;
                    case "authkeys3":
                        textFieldOperationAuthKeys3.setText(path);
                        textFieldOperationAuthKeys3.postActionEvent();
                        break;
                    case "blacklist":
                        textFieldOperationBlacklist.setText(path);
                        textFieldOperationBlacklist.postActionEvent();
                        break;
                    case "blacklist3":
                        textFieldOperationBlacklist3.setText(path);
                        textFieldOperationBlacklist3.postActionEvent();
                        break;
                    case "ipwhitelist":
                        textFieldOperationIpWhitelist.setText(path);
                        textFieldOperationIpWhitelist.postActionEvent();
                        break;
                    case "ipwhitelist3":
                        textFieldOperationIpWhitelist3.setText(path);
                        textFieldOperationIpWhitelist3.postActionEvent();
                        break;
                    case "targets":
                        textFieldOperationTargets.setText(path);
                        textFieldOperationTargets.postActionEvent();
                        break;
                    case "targets2":
                        textFieldOperationTargets2.setText(path);
                        textFieldOperationTargets2.postActionEvent();
                        break;
                    case "mismatches":
                        textFieldOperationMismatches.setText(path);
                        textFieldOperationMismatches.postActionEvent();
                        break;
                    case "whatsnew":
                        textFieldOperationWhatsNew.setText(path);
                        textFieldOperationWhatsNew.postActionEvent();
                        break;
                    case "exporttext":
                        textFieldOperationExportText.setText(path);
                        textFieldOperationExportText.postActionEvent();
                        break;
                    case "exportitems":
                        textFieldOperationExportItems.setText(path);
                        textFieldOperationExportItems.postActionEvent();
                        break;
                    case "hintkeys":
                        textFieldOperationHintKeys.setText(path);
                        textFieldOperationHintKeys.postActionEvent();
                        break;
                    case "hintkeys2":
                        textFieldOperationHintKeys2.setText(path);
                        textFieldOperationHintKeys2.postActionEvent();
                        break;
                    case "hintkeys3":
                        textFieldOperationHintKeys3.setText(path);
                        textFieldOperationHintKeys3.postActionEvent();
                        break;
                }
            }
            break;
        }
    }

    private String filePickerDirectory(String path)
    {
        if (Utils.isRelativePath(path))
            path = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + path;
        return Utils.getLeftPath(path, Utils.getSeparatorFromPath(path));
    }

    /**
     * Generic ActionEvent handler
     * <b/>
     * localContext.operations.genericAction
     *
     * @param e ActionEvent
     */
    public void genericAction(ActionEvent e)
    {
        if (e.getSource().getClass().equals(JButton.class))
        {
            JButton button = (JButton) e.getSource();
            if (button.getActionCommand().toLowerCase().endsWith("filepick"))
                filePicker(button);
            else
                updateOnChange(e.getSource());
        }
        else
        {
            updateOnChange(e.getSource());
        }
    }

    /**
     * Generic TextField focus handler
     *
     * @param e ActionEvent
     */
    public void genericTextFieldFocusLost(FocusEvent e)
    {
        if (e.getOppositeComponent() instanceof JTable)
        {
            // ignore config selection changes to avoid invalid updates
        }
        else
            updateOnChange(e.getSource());
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    private int getModeOperationIndex()
    {
        int index = -1;
        for (int i = 0; i < modes.length; ++i)
        {
            if (modes[i].operation == currentTool.getOperation() && modes[i].card == currentTool.getCard())
            {
                index = i;
                break;
            }
        }
        return index;
    }

    public void initialize()
    {
        this.configItems = operationConfigItems;

        // scale the help icon
        Icon icon = labelOperationHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelOperationHelp.setIcon(replacement);

        // position, size & divider
        if (context.preferences.getToolsOperationsXpos() != -1 && Utils.isOnScreen(context.preferences.getToolsOperationsXpos(),
                context.preferences.getToolsOperationsYpos()))
        {
            this.setLocation(context.preferences.getToolsOperationsXpos(), context.preferences.getToolsOperationsYpos());
            Dimension dim = new Dimension(context.preferences.getToolsOperationsWidth(), context.preferences.getToolsOperationsHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        splitPaneOperationContent.setDividerLocation(context.preferences.getToolOperationsDividerConfigLocation());

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                buttonOperationCancel.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // setup the left-side list of configurations
        configModel = new ConfigModel(context, this);
        configModel.setColumnCount(1);
        configItems.setModel(configModel);

        configItems.getTableHeader().setUI(null);
        configItems.setTableHeader(null);
        scrollPaneOperationConfig.setColumnHeaderView(null);

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
                    if (index != currentConfigIndex && currentConfigIndex >= 0)
                    {
                        currentConfigIndex = index;
                        loadOptions(currentConfigIndex);
                    }
                }
            }
        });
        configItems.setTableHeader(null);

        // Make Mode objects
        //  * publisher has base [objects]
        //  * listener has [objects]2
        //  * hint server has [objects]3
        //  * hint quit has [objects]6
        // See OperationsTool.Cards
        modes = new Mode[8];
        modes[0] = new Mode(context.cfg.gs("OperationsUI.mode.localPublish"), OperationsTool.Cards.Publisher, OperationsTool.Operations.PublisherOperation);
        modes[1] = new Mode(context.cfg.gs("OperationsUI.mode.subscriberListener"), OperationsTool.Cards.Listener, OperationsTool.Operations.SubscriberListener);
        modes[2] = new Mode(context.cfg.gs("OperationsUI.mode.hintServer"), OperationsTool.Cards.HintServer, OperationsTool.Operations.StatusServer);
        modes[3] = new Mode(context.cfg.gs("OperationsUI.mode.publisherTerminal"), OperationsTool.Cards.Terminal, OperationsTool.Operations.PublisherManual);
        modes[4] = new Mode(context.cfg.gs("OperationsUI.mode.publisherListener"), OperationsTool.Cards.Listener, OperationsTool.Operations.PublisherListener);
        modes[5] = new Mode(context.cfg.gs("OperationsUI.mode.subscriberTerminal"), OperationsTool.Cards.Terminal, OperationsTool.Operations.SubscriberTerminal);
        modes[6] = new Mode(context.cfg.gs("OperationsUI.mode.hintForceQuit"), OperationsTool.Cards.StatusQuit, OperationsTool.Operations.StatusServerQuit);
        modes[7] = new Mode(context.cfg.gs("OperationsUI.mode.subscriberForceQuit"), OperationsTool.Cards.SubscriberQuit, OperationsTool.Operations.SubscriberListenerQuit);

        // make New combobox
        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[]{}));
        comboBoxMode.removeAllItems();
        for (Mode m : modes)
        {
            comboBoxMode.addItem(m);
        }

        initializeComboBoxes();
        loadConfigurations();
        context.navigator.enableDisableToolMenus(this, false);
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void initializeComboBoxes()
    {
        comboBoxOperationWhatsNew.removeAllItems();
        comboBoxOperationWhatsNew.addItem(context.cfg.gs("OperationsUI.comboBoxOperationWhatsNew.0.whatsNew"));
        comboBoxOperationWhatsNew.addItem(context.cfg.gs("OperationsUI.comboBoxOperationWhatsNew.1.whatsNewAll"));

        comboBoxOperationHintKeys.removeAllItems();
        comboBoxOperationHintKeys.addItem(context.cfg.gs("OperationsUI.comboBoxOperationHintKeys.0.keys"));
        comboBoxOperationHintKeys.addItem(context.cfg.gs("OperationsUI.comboBoxOperationHintKeys.1.keysOnly"));
    }

    private void libraryLoader(int which)
    {
        if (libJList == null)
            libJList = new JList<String>();

        Repository repo;
        if (which == 0) // publisher
            repo = context.publisherRepo;
        else
            repo = context.subscriberRepo;

        if (repo != null)
        {
            Library[] biblio = repo.getLibraryData().libraries.bibliography;
            ArrayList<String> libraries = new ArrayList<>();
            for (int i = 0; i < biblio.length; ++i)
                libraries.add(biblio[i].name);
            Collections.sort(libraries);

            DefaultListModel<String> dialogList = new DefaultListModel<String>();
            for (String name : libraries)
            {
                dialogList.addElement(name);
            }
            libJList.setModel(dialogList);
            libJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            libJList.setSelectedIndex(0);
        }
    }

    private void initNewCard()
    {
        loading = true;
        switch (currentTool.getOperation())
        {
            case PublisherOperation:
            case SubscriberListener:
                currentTool.setOptKeys(context.cfg.getHintKeysFile());
                if (context.cfg.getHintTrackerFilename().length() > 0)
                {
                    comboBoxOperationHintKeys.setSelectedIndex(0);
                }
                if (context.cfg.getHintsDaemonFilename().length() > 0)
                {
                    comboBoxOperationHintKeys.setSelectedIndex(1);
                }
                break;
            case StatusServer:
                currentTool.setOptKeys(context.cfg.getHintKeysFile());
                break;
            case PublisherManual:
                break;
            case PublisherListener:
                break;
            case SubscriberTerminal:
                break;
            case StatusServerQuit:
                break;
            case SubscriberListenerQuit:
                break;
        }
        currentTool.setDataHasChanged();
        updateState();
        loading = false;
    }

    private void libraryPicker(JButton button)
    {
        try
        {
            JCheckBox checkBox = new JCheckBox(context.cfg.gs("OperationsUI.include.selections"));
            checkBox.setToolTipText(context.cfg.gs("OperationsUI.uncheck.to.exclude.selections"));
            checkBox.setSelected(true);
            if (button.getName().toLowerCase().equals("addexc"))
                checkBox.setSelected(false);

            JComboBox combo = new JComboBox();
            combo.addItem(context.cfg.gs("OperationsUI.publisher.libraries"));
            combo.addItem(context.cfg.gs("OperationsUI.subscriber.libraries"));
            combo.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String cmd = actionEvent.getActionCommand();
                    if (cmd.equals("comboBoxChanged"))
                    {
                        int selected = combo.getSelectedIndex();
                        libraryLoader(selected);
                    }
                }
            });

            libraryLoader(0);

            JScrollPane pane = new JScrollPane();
            pane.setViewportView(libJList);
            libJList.requestFocus();
            Object[] params1 = {context.cfg.gs("OperationsUI.select.included.excluded.libraries"), combo, checkBox, pane};
            Object[] params2 = {context.cfg.gs("OperationsUI.select.excluded.libraries"), combo, pane};

            int opt = 0;
            if (button.getName().toLowerCase().equals("addincexc"))
                opt = JOptionPane.showConfirmDialog(this, params1, displayName, JOptionPane.OK_CANCEL_OPTION);
            else if (button.getName().toLowerCase().equals("addexc"))
                opt = JOptionPane.showConfirmDialog(this, params2, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                java.util.List<String> selections = libJList.getSelectedValuesList();
                String[] libs = checkBox.isSelected() ? currentTool.getOptLibrary() : currentTool.getOptExclude();
                String[] allLibs = null;
                if (currentTool.getOptLibrary() != null && currentTool.getOptExclude() == null)
                    allLibs = currentTool.getOptLibrary();
                else if (currentTool.getOptLibrary() == null && currentTool.getOptExclude() != null)
                    allLibs = currentTool.getOptExclude();
                else if (currentTool.getOptLibrary() != null && currentTool.getOptExclude() != null)
                    allLibs = ArrayUtils.addAll(currentTool.getOptLibrary(), currentTool.getOptExclude());

                int start = 0;
                if (libs == null)
                    libs = new String[selections.size()];
                else
                {
                    start = libs.length;
                    String[] newLibs = new String[start + selections.size()];
                    System.arraycopy(libs, 0, newLibs, 0, start);
                    libs = newLibs;
                }

                boolean changed = false;
                for (String name : selections)
                {
                    // duplicates not allowed
                    if (allLibs != null)
                    {
                        boolean duplicate = false;
                        for (int i = 0; i < allLibs.length; ++i)
                        {
                            if (name.equals(allLibs[i]))
                            {
                                duplicate = true;
                                break;
                            }
                        }
                        if (duplicate)
                        {
                            JOptionPane.showMessageDialog(this,
                                    name + context.cfg.gs("OperationsUI.is.a.duplicate"),
                                    displayName, JOptionPane.WARNING_MESSAGE);
                            continue;
                        }
                    }
                    // add new element
                    libs[start++] = name;
                    changed = true;
                }

                if (changed)
                {
                    if (checkBox.isSelected())
                        currentTool.setOptLibrary(libs);
                    else
                        currentTool.setOptExclude(libs);

                    currentTool.setDataHasChanged();
                    if (button.getName().toLowerCase().equals("addincexc"))
                        loadIncludeExcludeList();
                    else if (button.getName().toLowerCase().equals("addexc"))
                        loadExcludeList();
                }
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean libraryRemover(int cardVar, boolean includes, int[] indices)
    {
        boolean changed = false;
        List<String> selections = (cardVar == 1) ? listOperationIncludeExclude.getSelectedValuesList() :
                listOperationExclude.getSelectedValuesList();
        for (int i = indices.length - 1; i >= 0; --i)
        {
            String dn = selections.get(i);
            if (dn.startsWith(context.cfg.gs("OperationsUI.include")) && !includes)
                continue;
            if (dn.startsWith(context.cfg.gs("OperationsUI.exclude")) && includes)
                continue;

            String cn = "";
            if (includes)
                cn = dn.substring(context.cfg.gs("OperationsUI.include").length());
            else
                cn = dn.substring(context.cfg.gs("OperationsUI.exclude").length());

            int removed = 0;
            String[] libs = (includes) ? currentTool.getOptLibrary() : currentTool.getOptExclude();
            if (libs != null)
            {
                for (int j = 0; j < libs.length; ++j)
                {
                    if (libs[j].equals(cn))
                    {
                        libs[j] = null;
                        ++removed;
                    }
                }
            }
            String[] newLibs;
            if (removed > 0)
            {
                changed = true;
                int j = libs.length - removed;
                if (j > 0)
                {
                    newLibs = new String[j];
                    j = 0;
                    for (int k = 0; k < libs.length; ++k)
                    {
                        if (libs[k] != null)
                            newLibs[j++] = libs[k];
                    }
                }
                else
                    newLibs = null;
            }
            else
                newLibs = libs;
            if (includes)
                currentTool.setOptLibrary(newLibs);
            else
                currentTool.setOptExclude(newLibs);
        }
        return changed;
    }

    private void loadConfigurations()
    {
        ArrayList<AbstractTool> toolList = null;
        try
        {
            toolList = context.tools.loadAllTools(context, OperationsTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                OperationsTool operation = (OperationsTool) tool;
                configModel.addRow(new Object[]{operation});
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        configModel.setToolList(toolList);
        configModel.loadJobsConfigurations(this, null);

        if (configModel.getRowCount() == 0)
        {
            buttonCopyOperation.setEnabled(false);
            buttonDeleteOperation.setEnabled(false);
            buttonOperationSave.setEnabled(false);
        }
        else
        {
            loadOptions(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
        currentConfigIndex = 0;
    }

    private void loadExcludeList()
    {
        ArrayList<String> exc = new ArrayList<>();
        if (currentTool.getOptExclude() != null)
        {
            for (int i = 0; i < currentTool.getOptExclude().length; ++i)
            {
                exc.add(context.cfg.gs("OperationsUI.exclude") + currentTool.getOptExclude()[i]);
            }
        }
        DefaultListModel<String> model = new DefaultListModel<String>();
        if (exc.size() > 0)
        {
            Collections.sort(exc);
            for (String element : exc)
            {
                model.addElement(element);
            }
        }
        listOperationExclude.setModel(model);
        scrollPaneOperationExclude.setViewportView(listOperationExclude);
        listOperationExclude.setSelectionInterval(0, 0);
    }

    private void loadIncludeExcludeList()
    {
        ArrayList<String> incExc = new ArrayList<>();
        if (currentTool.getOptExclude() != null)
        {
            for (int i = 0; i < currentTool.getOptExclude().length; ++i)
            {
                incExc.add(context.cfg.gs("OperationsUI.exclude") + currentTool.getOptExclude()[i]);
            }
        }
        if (currentTool.getOptLibrary() != null)
        {
            for (int i = 0; i < currentTool.getOptLibrary().length; ++i)
            {
                incExc.add(context.cfg.gs("OperationsUI.include") + currentTool.getOptLibrary()[i]);
            }
        }
        DefaultListModel<String> model = new DefaultListModel<String>();
        if (incExc.size() > 0)
        {
            Collections.sort(incExc);
            for (String element : incExc)
            {
                model.addElement(element);
            }
        }
        else
        {
            listOperationIncludeExclude.removeAll();
            model.removeAllElements();
            model.clear();
        }
        listOperationIncludeExclude.setModel(model);
        scrollPaneOperationIncludeExclude.setViewportView(listOperationIncludeExclude);
        listOperationIncludeExclude.setSelectionInterval(0, 0);
    }

    private void loadOptions(int index)
    {
        currentTool = null;
        if (index >= 0 && index < configModel.getRowCount())
        {
            currentTool = (OperationsTool) configModel.getValueAt(index, 0);
            currentConfigIndex = index;
        }

        if ((index >= 0 && index < configModel.getRowCount()) && currentTool != null)
        {
            loading = true;
            int cardVar = 1;
            ((CardLayout) panelOperationCards.getLayout()).show(panelOperationCards, currentTool.getCard().name().toLowerCase());
            int moi = getModeOperationIndex();
            if (moi < 0)
                moi = 0;
            labelOperationMode.setText(modes[moi].description);

            // populate card
            switch (currentTool.getOperation())
            {
                case PublisherOperation:
                    loadOptionsPublisher();
                    break;
                case SubscriberListener:
                    loadOptionsListener();
                    cardVar = 2;
                    break;
                case StatusServer:
                    loadOptionsHintServer();
                    break;
                case PublisherManual:
                    break;
                case PublisherListener:
                    loadOptionsListener();
                    cardVar = 2;
                    break;
                case SubscriberTerminal:
                    break;
                case StatusServerQuit:
                    loadOptionsHintsQuit();
                    break;
                case SubscriberListenerQuit:
                    break;
            }
            updateTextFieldToolTips(cardVar);
            updateState();
            loading = false;
        }
    }

    private void loadOptionsHintServer()
    {
        // ### LEFT SIDE
        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            textFieldOperationHintKeys3.setText(currentTool.getOptKeys());
        }
        else
        {
            textFieldOperationHintKeys3.setText("");
        }

        textFieldOperationAuthKeys3.setText(currentTool.getOptAuthKeys());
        checkBoxOperationKeepGoing3.setSelected(currentTool.isOptListenerKeepGoing());
        textFieldOperationBlacklist3.setText(currentTool.getOptBlacklist());
        textFieldOperationIpWhitelist3.setText(currentTool.getOptIpWhitelist());

        // ### RIGHT SIDE
        // none
    }

    private void loadOptionsHintsQuit()
    {
        // ### LEFT SIDE
        // none

        // ### RIGHT SIDE
        // none
    }

    private void loadOptionsListener()
    {
        // ### LEFT SIDE
        // --- General
        if (currentTool.getOptTargets().length() > 0)
        {
            textFieldOperationTargets2.setText(currentTool.getOptTargets());
        }
        else
        {
            textFieldOperationTargets2.setText("");
        }
        if (currentTool.getOptAuthorize() != null && currentTool.getOptAuthorize().length > 0)
            passwordFieldOperationsAuthorize.setText(new String(currentTool.getOptAuthorize()));
        else
            passwordFieldOperationsAuthorize.setText("");
        passwordFieldOperationsAuthorize.setEchoChar((char) 0); // do not hide password
        textFieldOperationAuthKeys.setText(currentTool.getOptAuthKeys());
        textFieldOperationBlacklist.setText(currentTool.getOptBlacklist());
        textFieldOperationIpWhitelist.setText(currentTool.getOptIpWhitelist());

        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            textFieldOperationHintKeys2.setText(currentTool.getOptKeys());
        }
        else
        {
            textFieldOperationHintKeys2.setText("");
        }
        checkBoxOperationKeepGoing2.setSelected(currentTool.isOptListenerKeepGoing());

        // ### RIGHT SIDE
        // --- Include/Exclude
        loadExcludeList();

        // --- Runtime Options
        checkBoxOperationOverwrite2.setSelected(currentTool.isOptOverwrite());
        checkBoxOperationPreserveDates2.setSelected(currentTool.isOptPreserveDates());
        checkBoxOperationDecimalScale2.setSelected(currentTool.isOptDecimalScale());
    }

    private void loadOptionsPublisher()
    {
        // ### LEFT SIDE
        // --- General
        checkBoxOperationNavigator.setSelected(currentTool.isOptNavigator());
        if (currentTool.getOptTargets().length() > 0)
        {
            textFieldOperationTargets.setText(currentTool.getOptTargets());
        }
        else
        {
            textFieldOperationTargets.setText("");
        }
        textFieldOperationMismatches.setText(currentTool.getOptMismatches());
        if (currentTool.getOptWhatsNew().length() > 0)
        {
            comboBoxOperationWhatsNew.setSelectedIndex(0);
            textFieldOperationWhatsNew.setText(currentTool.getOptWhatsNew());
        }
        else if (currentTool.getOptWhatsNewAll().length() > 0)
        {
            comboBoxOperationWhatsNew.setSelectedIndex(1);
            textFieldOperationWhatsNew.setText(currentTool.getOptWhatsNewAll());
        }
        else
        {
            comboBoxOperationWhatsNew.setSelectedIndex(0);
            textFieldOperationWhatsNew.setText("");
        }
        textFieldOperationExportText.setText(currentTool.getOptExportText());
        textFieldOperationExportItems.setText(currentTool.getOptExportItems());

        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            comboBoxOperationHintKeys.setSelectedIndex(0);
            textFieldOperationHintKeys.setText(currentTool.getOptKeys());
        }
        else if (currentTool.getOptKeysOnly().length() > 0)
        {
            comboBoxOperationHintKeys.setSelectedIndex(1);
            textFieldOperationHintKeys.setText(currentTool.getOptKeysOnly());
        }
        else
        {
            comboBoxOperationHintKeys.setSelectedIndex(0);
            textFieldOperationHintKeys.setText("");
        }
        checkBoxOperationQuitStatus.setSelected(currentTool.isOptQuitStatus());
        checkBoxOperationKeepGoing.setSelected(currentTool.isOptListenerKeepGoing());

        // ### RIGHT SIDE
        // --- Include/Exclude
        loadIncludeExcludeList();

        // --- Runtime Options
        checkBoxOperationOverwrite.setSelected(currentTool.isOptOverwrite());
        checkBoxOperationPreserveDates.setSelected(currentTool.isOptPreserveDates());
        checkBoxOperationDecimalScale.setSelected(currentTool.isOptDecimalScale());
        checkBoxOperationNoBackFill.setSelected(currentTool.isOptNoBackFill());
        checkBoxOperationValidate.setSelected(currentTool.isOptValidate());

        // --- Reporting
        checkBoxOperationDuplicates.setSelected(currentTool.isOptDuplicates());
        checkBoxOperationCrossCheck.setSelected(currentTool.isOptCrossCheck());
        checkBoxOperationEmptyDirectories.setSelected(currentTool.isOptEmptyDirectories());
        checkBoxOperationIgnored.setSelected(currentTool.isOptIgnored());
    }

    private boolean saveConfigurations()
    {
        OperationsTool tool = null;
        try
        {
            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = (OperationsTool) deletedTools.get(i);
                File file = new File(tool.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
            }
            deletedTools = new ArrayList<AbstractTool>();

            // validate changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (OperationsTool) configModel.getValueAt(i, 0);
                if (tool.isDataChanged())
                {
                    if (!validateTool(tool))
                        return false;
                }
            }

            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (OperationsTool) configModel.getValueAt(i, 0);
                if (tool.isDataChanged())
                {
                    validateTool(tool);
                    tool.write();
                }
                tool.setDataHasChanged(false);
            }

            // write/update changed Job JSON configuration files
            configModel.saveJobsConfigurations(null);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
        return true;
    }

    public void savePreferences()
    {
        context.preferences.setToolsOperationsHeight(this.getHeight());
        context.preferences.setToolsOperationsWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsOperationsXpos(location.x);
        context.preferences.setToolsOperationsYpos(location.y);
        context.preferences.setToolOperationsDividerConfigLocation(splitPaneOperationContent.getDividerLocation());
    }

    private void updateOnChange(Object source)
    {
        if (inUpdateOnChange)
            return;

        inUpdateOnChange = true;
        int cardVar = 1;  // 1 publisher; 2 listener, 3 hint server, 6 hint quit
        String name = null;
        int selection = -1;
        if (source != null && currentTool != null && !loading)
        {
            if (source instanceof JTextField)
            {
                String current = null;
                boolean isPwd = false;
                JTextField tf = (JTextField) source;
                name = tf.getName();
                switch (name.toLowerCase())
                {
                    case "authkeys3":
                        current = currentTool.getOptAuthKeys();
                        currentTool.setOptAuthKeys(tf.getText());
                        break;
                    case "authkeys":
                        current = currentTool.getOptAuthKeys();
                        currentTool.setOptAuthKeys(tf.getText());
                        break;
                    case "blacklist3":
                    case "blacklist":
                        current = currentTool.getOptBlacklist();
                        currentTool.setOptBlacklist(tf.getText());
                        break;
                    case "exportitems":
                        current = currentTool.getOptExportItems();
                        currentTool.setOptExportItems(tf.getText());
                        break;
                    case "exporttext":
                        current = currentTool.getOptExportText();
                        currentTool.setOptExportText(tf.getText());
                        break;
                    case "hintkeys2":  // Listener
                        cardVar = 2;
                    case "hintkeys":  // Publisher
                        selection = (cardVar == 2) ? 0 : comboBoxOperationHintKeys.getSelectedIndex();
                        if (selection == 0)
                        {
                            current = currentTool.getOptKeys();
                            if (!current.isEmpty() && tf.getText().isEmpty()) // has been cleared
                            {
                                if (checkHintKeysConflicts() < 0)
                                {
                                    inUpdateOnChange = false;
                                    return;
                                }
                            }
                            currentTool.setOptKeys(tf.getText());
                        }
                        else if (selection == 1)
                        {
                            current = currentTool.getOptKeysOnly();
                            if (!current.isEmpty() && tf.getText().isEmpty()) // has been cleared
                            {
                                if (checkHintKeysConflicts() < 0)
                                {
                                    inUpdateOnChange = false;
                                    return;
                                }
                            }
                            currentTool.setOptKeysOnly(tf.getText());
                        }
                        break;
                    case "hintkeys3":  // Hint Status Server
                        current = currentTool.getOptKeys();
                        currentTool.setOptKeys(tf.getText());
                        break;
                    case "ipwhitelist3":
                    case "ipwhitelist":
                        current = currentTool.getOptIpWhitelist();
                        currentTool.setOptIpWhitelist(tf.getText());
                        break;
                    case "mismatches":
                        current = currentTool.getOptMismatches();
                        currentTool.setOptMismatches(tf.getText());
                        break;
                    case "targets2":
                    case "targets":
                        current = currentTool.getOptTargets();
                        currentTool.setOptTargets(tf.getText());
                        break;
                    case "whatsnew":
                        if (comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptWhatsNew();
                            currentTool.setOptWhatsNew(tf.getText());
                        }
                        else if (comboBoxOperationWhatsNew.getSelectedIndex() == 1)
                        {
                            current = currentTool.getOptWhatsNewAll();
                            currentTool.setOptWhatsNewAll(tf.getText());
                        }
                        break;
                    case "authpassword":
                        isPwd = true;
                        JPasswordField pf = (JPasswordField) source;
                        char[] pw = currentTool.getOptAuthorize();
                        currentTool.setOptAuthorize(pf.getPassword());
                        if (pw != null && !pw.equals(pf.getPassword()))
                        {
                            currentTool.setDataHasChanged();
                            updateState();
                        }
                        break;
                }
                if (!isPwd && !current.equals(tf.getText()))
                {
                    currentTool.setDataHasChanged();
                    updateState();
                }
            }
            else if (source instanceof JCheckBox)
            {
                boolean state = false;
                JCheckBox cb = (JCheckBox) source;
                name = cb.getName();
                switch (name.toLowerCase())
                {
                    case "decimalscale2":
                    case "decimalscale":
                        state = currentTool.isOptDecimalScale();
                        currentTool.setOptDecimalScale(cb.isSelected());
                        break;
                    case "duplicates":
                        state = currentTool.isOptDuplicates();
                        currentTool.setOptDuplicates(cb.isSelected());
                        break;
                    case "crosscheck":
                        state = currentTool.isOptCrossCheck();
                        currentTool.setOptCrossCheck(cb.isSelected());
                        break;
                    case "emptydirectories":
                        state = currentTool.isOptEmptyDirectories();
                        currentTool.setOptEmptyDirectories(cb.isSelected());
                        break;
                    case "ignored":
                        state = currentTool.isOptIgnored();
                        currentTool.setOptIgnored(cb.isSelected());
                        break;
                    case "keepgoing":
                    case "keepgoing2":
                    case "keepgoing3":
                        state = currentTool.isOptListenerKeepGoing();
                        currentTool.setOptListenerKeepGoing(cb.isSelected());
                        break;
                    case "navigator":
                        state = currentTool.isOptNavigator();
                        currentTool.setOptNavigator(cb.isSelected());
                        break;
                    case "nobackfill":
                        state = currentTool.isOptNoBackFill();
                        currentTool.setOptNoBackFill(cb.isSelected());
                        break;
                    case "overwrite2":
                    case "overwrite":
                        state = currentTool.isOptOverwrite();
                        currentTool.setOptOverwrite(cb.isSelected());
                        break;
                    case "preservedates2":
                    case "preservedates":
                        state = currentTool.isOptPreserveDates();
                        currentTool.setOptPreserveDates(cb.isSelected());
                        break;
                    case "quitstatusserver":
                        state = currentTool.isOptQuitStatus();
                        currentTool.setOptQuitStatus(cb.isSelected());
                        break;
                    case "validate":
                        state = currentTool.isOptValidate();
                        currentTool.setOptValidate(cb.isSelected());
                        break;
                }
                if (state != cb.isSelected())
                {
                    currentTool.setDataHasChanged();
                    updateState();
                }
            }
            else if (source instanceof JComboBox)
            {
                JComboBox combo = (JComboBox) source;
                name = combo.getName();
//                combo.getUI().setPopupVisible(combo, false);
                int current = -1;
                int index = combo.getSelectedIndex();
                String value = "";
                switch (name.toLowerCase())
                {
                    case "keys2":
                        cardVar = 2;
                    case "keys":
                        if (currentTool.getOptKeys().length() > 0)
                            current = 0;
                        else if (currentTool.getOptKeysOnly().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            value = (cardVar == 2) ? textFieldOperationHintKeys2.getText() :
                                    textFieldOperationHintKeys.getText();
                            currentTool.setOptKeys(value);
                            currentTool.setOptKeysOnly("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? textFieldOperationHintKeys2.getText() :
                                    textFieldOperationHintKeys.getText();
                            currentTool.setOptKeysOnly(value);
                            currentTool.setOptKeys("");
                        }
                        break;
                    case "whatsnew":
                        if (currentTool.getOptWhatsNew().length() > 0)
                            current = 0;
                        else if (currentTool.getOptWhatsNewAll().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            currentTool.setOptWhatsNew(textFieldOperationWhatsNew.getText());
                            currentTool.setOptWhatsNewAll("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptWhatsNewAll(textFieldOperationWhatsNew.getText());
                            currentTool.setOptWhatsNew("");
                        }
                        break;
                }
                if (index != current)
                {
                    currentTool.setDataHasChanged();
                    updateState();
                }
            }
            // Note: JList listOperationIncludeExclude is handled in loadIncludeExcludeList() and libraryPicker()
        }

        if (currentTool != null)
            updateTextFieldToolTips(cardVar);

        inUpdateOnChange = false;
    }

    private void updateTextFieldToolTips(int carVar)
    {
        String current;
        int selected = 0;
        if (carVar == 1)  // 1 publisher; 2 listener, 3 hint server, 6 hint quit
        {
            current = currentTool.getOptExportItems();
            textFieldOperationExportItems.setToolTipText(current);

            current = currentTool.getOptExportText();
            textFieldOperationExportText.setToolTipText(current);

            current = currentTool.getOptMismatches();
            textFieldOperationMismatches.setToolTipText(current);

            current = "";
            if (comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                current = currentTool.getOptWhatsNew();
            else if (comboBoxOperationWhatsNew.getSelectedIndex() == 1)
                current = currentTool.getOptWhatsNewAll();
            textFieldOperationWhatsNew.setToolTipText(current);
        }

        if (carVar == 2)
        {
            current = currentTool.getOptAuthKeys();
            textFieldOperationAuthKeys.setToolTipText(current);

            current = currentTool.getOptBlacklist();
            textFieldOperationBlacklist.setToolTipText(current);

            current = currentTool.getOptIpWhitelist();
            textFieldOperationIpWhitelist.setToolTipText(current);
        }

        current = currentTool.getOptTargets();
        if (carVar == 1)
            textFieldOperationTargets.setToolTipText(current);
        else
            textFieldOperationTargets2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = comboBoxOperationHintKeys.getSelectedIndex();
        else
            selected = 0;
        if (selected == 0)
            current = currentTool.getOptKeys();
        else if (selected == 1)
            current = currentTool.getOptKeysOnly();
        if (carVar == 1)
            textFieldOperationHintKeys.setToolTipText(current);
        else
            textFieldOperationHintKeys2.setToolTipText(current);

        current = "";
    }

    private void updateState()
    {
        if (currentTool.getCard() == OperationsTool.Cards.Publisher)
        {
            if (currentTool.isOptDuplicates())
            {
                labelOperationCrossCheck.setEnabled(true);
                checkBoxOperationCrossCheck.setEnabled(true);
            }
            else
            {
                labelOperationCrossCheck.setEnabled(false);
                checkBoxOperationCrossCheck.setEnabled(false);
            }
        }
    }

    private boolean validateTool(OperationsTool tool)
    {
        if (tool.getCard().equals(OperationsTool.Cards.HintServer))
        {
            if (tool.getOptKeys().isEmpty())
            {
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("OperationsUI.hint.status.server.requires.hint.keys") + tool.getConfigName());
                textFieldOperationHintKeys3.requestFocus();
                return false;
            }

            if (tool.getOptAuthKeys().isEmpty())
            {
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("OperationsUI.hint.status.server.requires.authentication.keys") + tool.getConfigName());
                textFieldOperationAuthKeys3.requestFocus();
                return false;
            }
        }
        return true;
    }

    private void windowClosing(WindowEvent e)
    {
        buttonOperationCancel.doClick();
    }

    private void windowHidden(ComponentEvent e)
    {
        context.navigator.enableDisableToolMenus(this, true);
    }

    // ================================================================================================================

    private class Mode
    {
        String description;
        OperationsTool.Cards card;
        OperationsTool.Operations operation;

        public Mode(String description, OperationsTool.Cards card, OperationsTool.Operations operation)
        {
            this.description = description;
            this.card = card;
            this.operation = operation;
        }

        @Override
        public String toString()
        {
            return description;
        }
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelOperationButtons = new JPanel();
        panelTopOperationButtons = new JPanel();
        buttonNewOperation = new JButton();
        buttonCopyOperation = new JButton();
        buttonDeleteOperation = new JButton();
        panelOperationHelp = new JPanel();
        labelOperationHelp = new JLabel();
        splitPaneOperationContent = new JSplitPane();
        scrollPaneOperationConfig = new JScrollPane();
        operationConfigItems = new JTable();
        panelOperationOptions = new JPanel();
        panelOperationControls = new JPanel();
        topOperationOptions = new JPanel();
        vSpacer0 = new JPanel(null);
        panelOperationMode = new JPanel();
        hSpacer3 = new JPanel(null);
        labelOperationMode = new JLabel();
        scrollPaneOperationCards = new JScrollPane();
        panelOperationCards = new JPanel();
        panelCardGettingStarted = new JPanel();
        labelOperationGettingStarted = new JLabel();
        panelCardPublisher = new JPanel();
        hSpacer4 = new JPanel(null);
        vSpacer3 = new JPanel(null);
        hSpacer5 = new JPanel(null);
        labelOperationNavigatorCheckbox = new JLabel();
        checkBoxOperationNavigator = new JCheckBox();
        vSpacer33 = new JPanel(null);
        panelOperationIncludeExcludeBox = new JPanel();
        scrollPaneOperationIncludeExclude = new JScrollPane();
        listOperationIncludeExclude = new JList<>();
        panelOperationIncludeExcludeButtons = new JPanel();
        buttonOperationAddIncludeExclude = new JButton();
        buttonOperationRemoveIncludeExclude = new JButton();
        labelOperationIncludeExclude = new JLabel();
        vSpacer4 = new JPanel(null);
        labelOperationTargets = new JLabel();
        textFieldOperationTargets = new JTextField();
        buttonOperationTargetsFilePick = new JButton();
        vSpacer5 = new JPanel(null);
        labelOperationsMismatches = new JLabel();
        textFieldOperationMismatches = new JTextField();
        buttonOperationMismatchesFilePick = new JButton();
        vSpacer6 = new JPanel(null);
        comboBoxOperationWhatsNew = new JComboBox<>();
        textFieldOperationWhatsNew = new JTextField();
        buttonOperationWhatsNewFilePick = new JButton();
        vSpacer7 = new JPanel(null);
        labelOperationDecimalScale = new JLabel();
        checkBoxOperationDecimalScale = new JCheckBox();
        labelOperationExportText = new JLabel();
        textFieldOperationExportText = new JTextField();
        buttonOperationExportTextFilePick = new JButton();
        vSpacer9 = new JPanel(null);
        labelOperationNoBackfill = new JLabel();
        checkBoxOperationNoBackFill = new JCheckBox();
        labelOperationExportItems = new JLabel();
        textFieldOperationExportItems = new JTextField();
        buttonOperationExportItemsFilePick = new JButton();
        vSpacer10 = new JPanel(null);
        labelOperationOverwrite = new JLabel();
        checkBoxOperationOverwrite = new JCheckBox();
        vSpacer11 = new JPanel(null);
        labelOperationPreservedDates = new JLabel();
        checkBoxOperationPreserveDates = new JCheckBox();
        comboBoxOperationHintKeys = new JComboBox<>();
        textFieldOperationHintKeys = new JTextField();
        buttonOperationHintKeysFilePick = new JButton();
        vSpacer19 = new JPanel(null);
        labelOperationValidate = new JLabel();
        checkBoxOperationValidate = new JCheckBox();
        labelOperationKeepGoing = new JLabel();
        checkBoxOperationKeepGoing = new JCheckBox();
        vSpacer18 = new JPanel(null);
        labelOperationQuitStatusServer = new JLabel();
        checkBoxOperationQuitStatus = new JCheckBox();
        vSpacer17 = new JPanel(null);
        labelOperationDuplicates = new JLabel();
        checkBoxOperationDuplicates = new JCheckBox();
        vSpacer16 = new JPanel(null);
        labelOperationCrossCheck = new JLabel();
        checkBoxOperationCrossCheck = new JCheckBox();
        vSpacer15 = new JPanel(null);
        labelOperationEmptyDirectories = new JLabel();
        checkBoxOperationEmptyDirectories = new JCheckBox();
        vSpacer14 = new JPanel(null);
        labelOperationIgnored = new JLabel();
        checkBoxOperationIgnored = new JCheckBox();
        panelCardListener = new JPanel();
        hSpacer6 = new JPanel(null);
        vSpacer40 = new JPanel(null);
        hSpacer7 = new JPanel(null);
        labelOperationTargets2 = new JLabel();
        textFieldOperationTargets2 = new JTextField();
        buttonOperationTargetsFilePick2 = new JButton();
        vSpacer32 = new JPanel(null);
        panelOperationExcludeBox = new JPanel();
        scrollPaneOperationExclude = new JScrollPane();
        listOperationExclude = new JList<>();
        panelOperationExcludeButtons = new JPanel();
        buttonOperationAddExclude = new JButton();
        buttonOperationRemoveExclude = new JButton();
        labelOperationExclude = new JLabel();
        vSpacer8 = new JPanel(null);
        labelOperationAuthorize = new JLabel();
        passwordFieldOperationsAuthorize = new JPasswordField();
        vSpacer12 = new JPanel(null);
        labelOperationAuthKeys = new JLabel();
        textFieldOperationAuthKeys = new JTextField();
        buttonOperationAuthKeysFilePick = new JButton();
        vSpacer20 = new JPanel(null);
        labelOperationBlacklist = new JLabel();
        textFieldOperationBlacklist = new JTextField();
        buttonOperationBlacklistFilePick = new JButton();
        vSpacer21 = new JPanel(null);
        labelOperationDecimalScale2 = new JLabel();
        checkBoxOperationDecimalScale2 = new JCheckBox();
        labelOperationIpWhitelist = new JLabel();
        textFieldOperationIpWhitelist = new JTextField();
        buttonOperationIpWhitelistFilePick = new JButton();
        vSpacer22 = new JPanel(null);
        labelOperationOverwrite2 = new JLabel();
        checkBoxOperationOverwrite2 = new JCheckBox();
        vSpacer23 = new JPanel(null);
        labelOperationPreservedDates2 = new JLabel();
        checkBoxOperationPreserveDates2 = new JCheckBox();
        labelOperationHintKeys = new JLabel();
        textFieldOperationHintKeys2 = new JTextField();
        buttonOperationHintKeysFilePick2 = new JButton();
        vSpacer24 = new JPanel(null);
        labelOperationKeepGoing2 = new JLabel();
        checkBoxOperationKeepGoing2 = new JCheckBox();
        vSpacer26 = new JPanel(null);
        panelCardHintServer = new JPanel();
        vSpacer41 = new JPanel(null);
        labelOperationHintKeys2 = new JLabel();
        textFieldOperationHintKeys3 = new JTextField();
        buttonOperationHintKeysFilePick3 = new JButton();
        vSpacer34 = new JPanel(null);
        label1 = new JLabel();
        textFieldOperationAuthKeys3 = new JTextField();
        buttonOperationAuthKeysFilePick3 = new JButton();
        vSpacer44 = new JPanel(null);
        labelOperationKeepGoing3 = new JLabel();
        checkBoxOperationKeepGoing3 = new JCheckBox();
        vSpacer36 = new JPanel(null);
        vSpacer39 = new JPanel(null);
        labelOperationBlacklist3 = new JLabel();
        textFieldOperationBlacklist3 = new JTextField();
        buttonOperationBlacklistFilePick3 = new JButton();
        vSpacer37 = new JPanel(null);
        labelOperationIpWhitelist3 = new JLabel();
        textFieldOperationIpWhitelist3 = new JTextField();
        buttonOperationIpWhitelistFilePick3 = new JButton();
        vSpacer38 = new JPanel(null);
        panelCardTerminal = new JPanel();
        labelOperationsTerminal = new JLabel();
        panelCardQuit = new JPanel();
        labelOperationsQuitter = new JLabel();
        panelCardQuitHints = new JPanel();
        labelOperationsQuitHints = new JLabel();
        buttonBar = new JPanel();
        buttonOperationSave = new JButton();
        buttonOperationCancel = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("Operations.displayName"));
        setName("operationsUI");
        setMinimumSize(new Dimension(150, 126));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                OperationsUI.this.windowClosing(e);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                windowHidden(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelOperationButtons ========
                {
                    panelOperationButtons.setMinimumSize(new Dimension(0, 0));
                    panelOperationButtons.setPreferredSize(new Dimension(614, 38));
                    panelOperationButtons.setLayout(new BorderLayout());

                    //======== panelTopOperationButtons ========
                    {
                        panelTopOperationButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopOperationButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNewOperation ----
                        buttonNewOperation.setText(context.cfg.gs("OperationsUI.buttonNewOperation.text"));
                        buttonNewOperation.setToolTipText(context.cfg.gs("OperationsUI.buttonNewOperation.toolTipText"));
                        buttonNewOperation.setMnemonic(context.cfg.gs("OperationsUI.buttonNewOperation.mnemonic").charAt(0));
                        buttonNewOperation.addActionListener(e -> actionNewClicked(e));
                        panelTopOperationButtons.add(buttonNewOperation);

                        //---- buttonCopyOperation ----
                        buttonCopyOperation.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                        buttonCopyOperation.setMnemonic(context.cfg.gs("OperationsUI.buttonCopyOperation.mnemonic").charAt(0));
                        buttonCopyOperation.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                        buttonCopyOperation.addActionListener(e -> actionCopyClicked(e));
                        panelTopOperationButtons.add(buttonCopyOperation);

                        //---- buttonDeleteOperation ----
                        buttonDeleteOperation.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                        buttonDeleteOperation.setMnemonic(context.cfg.gs("OperationsUI.buttonDeleteOperation.mnemonic").charAt(0));
                        buttonDeleteOperation.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
                        buttonDeleteOperation.addActionListener(e -> actionDeleteClicked(e));
                        panelTopOperationButtons.add(buttonDeleteOperation);
                    }
                    panelOperationButtons.add(panelTopOperationButtons, BorderLayout.WEST);

                    //======== panelOperationHelp ========
                    {
                        panelOperationHelp.setPreferredSize(new Dimension(40, 38));
                        panelOperationHelp.setMinimumSize(new Dimension(0, 38));
                        panelOperationHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                        //---- labelOperationHelp ----
                        labelOperationHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelOperationHelp.setPreferredSize(new Dimension(32, 30));
                        labelOperationHelp.setMinimumSize(new Dimension(32, 30));
                        labelOperationHelp.setMaximumSize(new Dimension(32, 30));
                        labelOperationHelp.setToolTipText(context.cfg.gs("OperationsUI.help"));
                        labelOperationHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelOperationHelp.setIconTextGap(0);
                        labelOperationHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                actionHelpClicked(e);
                            }
                        });
                        panelOperationHelp.add(labelOperationHelp);
                    }
                    panelOperationButtons.add(panelOperationHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelOperationButtons, BorderLayout.NORTH);

                //======== splitPaneOperationContent ========
                {
                    splitPaneOperationContent.setDividerLocation(142);
                    splitPaneOperationContent.setLastDividerLocation(142);
                    splitPaneOperationContent.setMinimumSize(new Dimension(0, 0));

                    //======== scrollPaneOperationConfig ========
                    {
                        scrollPaneOperationConfig.setMinimumSize(new Dimension(0, 0));
                        scrollPaneOperationConfig.setPreferredSize(new Dimension(142, 146));

                        //---- operationConfigItems ----
                        operationConfigItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        operationConfigItems.setShowVerticalLines(false);
                        operationConfigItems.setFillsViewportHeight(true);
                        scrollPaneOperationConfig.setViewportView(operationConfigItems);
                    }
                    splitPaneOperationContent.setLeftComponent(scrollPaneOperationConfig);

                    //======== panelOperationOptions ========
                    {
                        panelOperationOptions.setMinimumSize(new Dimension(0, 0));
                        panelOperationOptions.setLayout(new BorderLayout());

                        //======== panelOperationControls ========
                        {
                            panelOperationControls.setMinimumSize(new Dimension(0, 0));
                            panelOperationControls.setLayout(new BorderLayout());

                            //======== topOperationOptions ========
                            {
                                topOperationOptions.setMinimumSize(new Dimension(0, 0));
                                topOperationOptions.setLayout(new BorderLayout());

                                //---- vSpacer0 ----
                                vSpacer0.setPreferredSize(new Dimension(10, 2));
                                vSpacer0.setMinimumSize(new Dimension(10, 2));
                                vSpacer0.setMaximumSize(new Dimension(10, 2));
                                topOperationOptions.add(vSpacer0, BorderLayout.NORTH);

                                //======== panelOperationMode ========
                                {
                                    panelOperationMode.setMinimumSize(new Dimension(0, 0));
                                    panelOperationMode.setLayout(new BoxLayout(panelOperationMode, BoxLayout.X_AXIS));

                                    //---- hSpacer3 ----
                                    hSpacer3.setPreferredSize(new Dimension(4, 10));
                                    hSpacer3.setMinimumSize(new Dimension(4, 12));
                                    hSpacer3.setMaximumSize(new Dimension(4, 32767));
                                    panelOperationMode.add(hSpacer3);

                                    //---- labelOperationMode ----
                                    labelOperationMode.setMaximumSize(new Dimension(800, 16));
                                    labelOperationMode.setFont(labelOperationMode.getFont().deriveFont(labelOperationMode.getFont().getStyle() | Font.BOLD, labelOperationMode.getFont().getSize() + 1f));
                                    labelOperationMode.setPreferredSize(new Dimension(800, 16));
                                    labelOperationMode.setMinimumSize(new Dimension(110, 16));
                                    panelOperationMode.add(labelOperationMode);
                                }
                                topOperationOptions.add(panelOperationMode, BorderLayout.WEST);
                            }
                            panelOperationControls.add(topOperationOptions, BorderLayout.NORTH);
                        }
                        panelOperationOptions.add(panelOperationControls, BorderLayout.NORTH);

                        //======== scrollPaneOperationCards ========
                        {
                            scrollPaneOperationCards.setMinimumSize(new Dimension(0, 0));

                            //======== panelOperationCards ========
                            {
                                panelOperationCards.setMinimumSize(new Dimension(0, 0));
                                panelOperationCards.setMaximumSize(null);
                                panelOperationCards.setLayout(new CardLayout());

                                //======== panelCardGettingStarted ========
                                {
                                    panelCardGettingStarted.setPreferredSize(new Dimension(824, 500));
                                    panelCardGettingStarted.setMinimumSize(new Dimension(0, 0));
                                    panelCardGettingStarted.setLayout(new BorderLayout());

                                    //---- labelOperationGettingStarted ----
                                    labelOperationGettingStarted.setText(context.cfg.gs("OperationsUI.labelOperationGettingStarted.text"));
                                    labelOperationGettingStarted.setFont(labelOperationGettingStarted.getFont().deriveFont(labelOperationGettingStarted.getFont().getStyle() | Font.BOLD));
                                    labelOperationGettingStarted.setHorizontalAlignment(SwingConstants.CENTER);
                                    labelOperationGettingStarted.setPreferredSize(new Dimension(630, 470));
                                    panelCardGettingStarted.add(labelOperationGettingStarted, BorderLayout.CENTER);
                                }
                                panelOperationCards.add(panelCardGettingStarted, "gettingStarted");

                                //======== panelCardPublisher ========
                                {
                                    panelCardPublisher.setName("publisher");
                                    panelCardPublisher.setMinimumSize(new Dimension(0, 0));
                                    panelCardPublisher.setPreferredSize(new Dimension(824, 500));
                                    panelCardPublisher.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelCardPublisher.getLayout()).rowHeights = new int[] {0, 0, 28, 34, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                    ((GridBagLayout)panelCardPublisher.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0};
                                    ((GridBagLayout)panelCardPublisher.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                    //---- hSpacer4 ----
                                    hSpacer4.setMinimumSize(new Dimension(154, 2));
                                    hSpacer4.setPreferredSize(new Dimension(154, 2));
                                    hSpacer4.setMaximumSize(new Dimension(154, 2));
                                    panelCardPublisher.add(hSpacer4, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer3 ----
                                    vSpacer3.setMinimumSize(new Dimension(2, 8));
                                    vSpacer3.setMaximumSize(new Dimension(2, 8));
                                    vSpacer3.setPreferredSize(new Dimension(2, 8));
                                    panelCardPublisher.add(vSpacer3, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- hSpacer5 ----
                                    hSpacer5.setMinimumSize(new Dimension(154, 2));
                                    hSpacer5.setPreferredSize(new Dimension(154, 2));
                                    hSpacer5.setMaximumSize(new Dimension(154, 2));
                                    panelCardPublisher.add(hSpacer5, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationNavigatorCheckbox ----
                                    labelOperationNavigatorCheckbox.setText(context.cfg.gs("OperationsUI.labelOperationNavigatorCheckbox.text"));
                                    panelCardPublisher.add(labelOperationNavigatorCheckbox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- checkBoxOperationNavigator ----
                                    checkBoxOperationNavigator.setName("navigator");
                                    checkBoxOperationNavigator.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationNavigator.toolTipText"));
                                    checkBoxOperationNavigator.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationNavigator, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer33 ----
                                    vSpacer33.setMinimumSize(new Dimension(4, 30));
                                    vSpacer33.setPreferredSize(new Dimension(10, 30));
                                    vSpacer33.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer33, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //======== panelOperationIncludeExcludeBox ========
                                    {
                                        panelOperationIncludeExcludeBox.setPreferredSize(new Dimension(240, 120));
                                        panelOperationIncludeExcludeBox.setMinimumSize(new Dimension(240, 120));
                                        panelOperationIncludeExcludeBox.setLayout(new BoxLayout(panelOperationIncludeExcludeBox, BoxLayout.Y_AXIS));

                                        //======== scrollPaneOperationIncludeExclude ========
                                        {
                                            scrollPaneOperationIncludeExclude.setPreferredSize(new Dimension(52, 120));

                                            //---- listOperationIncludeExclude ----
                                            listOperationIncludeExclude.setName("includeexclude");
                                            listOperationIncludeExclude.setVisibleRowCount(5);
                                            listOperationIncludeExclude.setModel(new AbstractListModel<String>() {
                                                String[] values = {
                                                    "Item 1",
                                                    "Item 2",
                                                    "Item 3",
                                                    "Item 4",
                                                    "Item 5",
                                                    "Item 6"
                                                };
                                                @Override
                                                public int getSize() { return values.length; }
                                                @Override
                                                public String getElementAt(int i) { return values[i]; }
                                            });
                                            listOperationIncludeExclude.setToolTipText(context.cfg.gs("OperationsUI.listOperationIncludeExclude.toolTipText"));
                                            listOperationIncludeExclude.setMaximumSize(new Dimension(32767, 120));
                                            scrollPaneOperationIncludeExclude.setViewportView(listOperationIncludeExclude);
                                        }
                                        panelOperationIncludeExcludeBox.add(scrollPaneOperationIncludeExclude);

                                        //======== panelOperationIncludeExcludeButtons ========
                                        {
                                            panelOperationIncludeExcludeButtons.setPreferredSize(new Dimension(250, 28));
                                            panelOperationIncludeExcludeButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                            //---- buttonOperationAddIncludeExclude ----
                                            buttonOperationAddIncludeExclude.setText(context.cfg.gs("OperationsUI.buttonOperationAddIncludeExclude.text"));
                                            buttonOperationAddIncludeExclude.setFont(buttonOperationAddIncludeExclude.getFont().deriveFont(buttonOperationAddIncludeExclude.getFont().getSize() - 2f));
                                            buttonOperationAddIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                            buttonOperationAddIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                            buttonOperationAddIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                            buttonOperationAddIncludeExclude.setMnemonic(context.cfg.gs("OperationsUI.buttonOperationAddIncludeExclude.mnemonic").charAt(0));
                                            buttonOperationAddIncludeExclude.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationAddIncludeExclude.toolTipText"));
                                            buttonOperationAddIncludeExclude.setName("addincexc");
                                            buttonOperationAddIncludeExclude.addActionListener(e -> actionOperationAddRowClicked(e));
                                            panelOperationIncludeExcludeButtons.add(buttonOperationAddIncludeExclude);

                                            //---- buttonOperationRemoveIncludeExclude ----
                                            buttonOperationRemoveIncludeExclude.setText(context.cfg.gs("OperationsUI.buttonOperationRemoveIncludeExclude.text"));
                                            buttonOperationRemoveIncludeExclude.setFont(buttonOperationRemoveIncludeExclude.getFont().deriveFont(buttonOperationRemoveIncludeExclude.getFont().getSize() - 2f));
                                            buttonOperationRemoveIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                            buttonOperationRemoveIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                            buttonOperationRemoveIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                            buttonOperationRemoveIncludeExclude.setMnemonic(context.cfg.gs("OperationsUI.buttonOperationRemoveIncludeExclude.mnemonic").charAt(0));
                                            buttonOperationRemoveIncludeExclude.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationRemoveIncludeExclude.toolTipText"));
                                            buttonOperationRemoveIncludeExclude.setName("removeincexc");
                                            buttonOperationRemoveIncludeExclude.addActionListener(e -> actionOperationRemoveRowClicked(e));
                                            panelOperationIncludeExcludeButtons.add(buttonOperationRemoveIncludeExclude);
                                        }
                                        panelOperationIncludeExcludeBox.add(panelOperationIncludeExcludeButtons);
                                    }
                                    panelCardPublisher.add(panelOperationIncludeExcludeBox, new GridBagConstraints(5, 1, 1, 4, 0.0, 0.0,
                                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationIncludeExclude ----
                                    labelOperationIncludeExclude.setText(context.cfg.gs("OperationsUI.labelOperationIncludeExclude.text"));
                                    panelCardPublisher.add(labelOperationIncludeExclude, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer4 ----
                                    vSpacer4.setMinimumSize(new Dimension(4, 30));
                                    vSpacer4.setPreferredSize(new Dimension(10, 30));
                                    vSpacer4.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer4, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationTargets ----
                                    labelOperationTargets.setText(context.cfg.gs("OperationsUI.labelOperationTargets.text"));
                                    panelCardPublisher.add(labelOperationTargets, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationTargets ----
                                    textFieldOperationTargets.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationTargets.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationTargets.setName("targets");
                                    textFieldOperationTargets.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationTargets.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationTargets.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationTargets, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationTargetsFilePick ----
                                    buttonOperationTargetsFilePick.setText("...");
                                    buttonOperationTargetsFilePick.setFont(buttonOperationTargetsFilePick.getFont().deriveFont(buttonOperationTargetsFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationTargetsFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationTargetsFilePick.setIconTextGap(0);
                                    buttonOperationTargetsFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationTargetsFilePick.setActionCommand("targetsFilePick");
                                    buttonOperationTargetsFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationTargetsFilePick.toolTipText"));
                                    buttonOperationTargetsFilePick.setName("targets");
                                    buttonOperationTargetsFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationTargetsFilePick, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer5 ----
                                    vSpacer5.setMinimumSize(new Dimension(4, 30));
                                    vSpacer5.setPreferredSize(new Dimension(10, 30));
                                    vSpacer5.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer5, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationsMismatches ----
                                    labelOperationsMismatches.setText(context.cfg.gs("OperationsUI.labelOperationsMismatches.text"));
                                    panelCardPublisher.add(labelOperationsMismatches, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationMismatches ----
                                    textFieldOperationMismatches.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationMismatches.setName("mismatches");
                                    textFieldOperationMismatches.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationMismatches.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationMismatches.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationMismatches.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationMismatches, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationMismatchesFilePick ----
                                    buttonOperationMismatchesFilePick.setText("...");
                                    buttonOperationMismatchesFilePick.setFont(buttonOperationMismatchesFilePick.getFont().deriveFont(buttonOperationMismatchesFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationMismatchesFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationMismatchesFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationMismatchesFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationMismatchesFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationMismatchesFilePick.setIconTextGap(0);
                                    buttonOperationMismatchesFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationMismatchesFilePick.setActionCommand("mismatchesFilePick");
                                    buttonOperationMismatchesFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationMismatchesFilePick.toolTipText"));
                                    buttonOperationMismatchesFilePick.setName("mismatches");
                                    buttonOperationMismatchesFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationMismatchesFilePick, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer6 ----
                                    vSpacer6.setMinimumSize(new Dimension(4, 30));
                                    vSpacer6.setPreferredSize(new Dimension(10, 30));
                                    vSpacer6.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer6, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- comboBoxOperationWhatsNew ----
                                    comboBoxOperationWhatsNew.setPrototypeDisplayValue(context.cfg.gs("OperationsUI.comboBoxOperationWhatsNew.prototypeDisplayValue"));
                                    comboBoxOperationWhatsNew.setModel(new DefaultComboBoxModel<>(new String[] {
                                        "What's New:",
                                        "What's New, all:"
                                    }));
                                    comboBoxOperationWhatsNew.setName("whatsnew");
                                    comboBoxOperationWhatsNew.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(comboBoxOperationWhatsNew, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationWhatsNew ----
                                    textFieldOperationWhatsNew.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationWhatsNew.setName("whatsNew");
                                    textFieldOperationWhatsNew.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationWhatsNew.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationWhatsNew.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationWhatsNew.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationWhatsNew, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationWhatsNewFilePick ----
                                    buttonOperationWhatsNewFilePick.setText(context.cfg.gs("OperationsUI.buttonOperationWhatsNewFilePick.text"));
                                    buttonOperationWhatsNewFilePick.setFont(buttonOperationWhatsNewFilePick.getFont().deriveFont(buttonOperationWhatsNewFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationWhatsNewFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationWhatsNewFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationWhatsNewFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationWhatsNewFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationWhatsNewFilePick.setIconTextGap(0);
                                    buttonOperationWhatsNewFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationWhatsNewFilePick.setActionCommand("whatsnewFilePick");
                                    buttonOperationWhatsNewFilePick.setName("whatsnew");
                                    buttonOperationWhatsNewFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationWhatsNewFilePick.toolTipText"));
                                    buttonOperationWhatsNewFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationWhatsNewFilePick, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer7 ----
                                    vSpacer7.setMinimumSize(new Dimension(4, 30));
                                    vSpacer7.setPreferredSize(new Dimension(10, 30));
                                    vSpacer7.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer7, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationDecimalScale ----
                                    labelOperationDecimalScale.setText(context.cfg.gs("OperationsUI.labelOperationDecimalScale.text"));
                                    panelCardPublisher.add(labelOperationDecimalScale, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationDecimalScale ----
                                    checkBoxOperationDecimalScale.setName("decimalScale");
                                    checkBoxOperationDecimalScale.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationDecimalScale.toolTipText"));
                                    checkBoxOperationDecimalScale.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationDecimalScale, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationExportText ----
                                    labelOperationExportText.setText(context.cfg.gs("OperationsUI.labelOperationExportText.text"));
                                    panelCardPublisher.add(labelOperationExportText, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationExportText ----
                                    textFieldOperationExportText.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationExportText.setName("exportText");
                                    textFieldOperationExportText.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationExportText.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationExportText.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationExportText.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationExportText, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationExportTextFilePick ----
                                    buttonOperationExportTextFilePick.setText("...");
                                    buttonOperationExportTextFilePick.setFont(buttonOperationExportTextFilePick.getFont().deriveFont(buttonOperationExportTextFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationExportTextFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationExportTextFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationExportTextFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationExportTextFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationExportTextFilePick.setIconTextGap(0);
                                    buttonOperationExportTextFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationExportTextFilePick.setActionCommand("exportTextFilePick");
                                    buttonOperationExportTextFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationExportTextFilePick.toolTipText"));
                                    buttonOperationExportTextFilePick.setName("exporttext");
                                    buttonOperationExportTextFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationExportTextFilePick, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer9 ----
                                    vSpacer9.setMinimumSize(new Dimension(4, 30));
                                    vSpacer9.setPreferredSize(new Dimension(10, 30));
                                    vSpacer9.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer9, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationNoBackfill ----
                                    labelOperationNoBackfill.setText(context.cfg.gs("OperationsUI.labelOperationNoBackFill.text"));
                                    panelCardPublisher.add(labelOperationNoBackfill, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationNoBackFill ----
                                    checkBoxOperationNoBackFill.setName("noBackFill");
                                    checkBoxOperationNoBackFill.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationNoBackFill.toolTipText"));
                                    checkBoxOperationNoBackFill.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationNoBackFill, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationExportItems ----
                                    labelOperationExportItems.setText(context.cfg.gs("OperationsUI.labelOperationExportItems.text"));
                                    panelCardPublisher.add(labelOperationExportItems, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationExportItems ----
                                    textFieldOperationExportItems.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationExportItems.setName("exportItems");
                                    textFieldOperationExportItems.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationExportItems.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationExportItems.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationExportItems.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationExportItems, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationExportItemsFilePick ----
                                    buttonOperationExportItemsFilePick.setText("...");
                                    buttonOperationExportItemsFilePick.setFont(buttonOperationExportItemsFilePick.getFont().deriveFont(buttonOperationExportItemsFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationExportItemsFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationExportItemsFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationExportItemsFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationExportItemsFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationExportItemsFilePick.setIconTextGap(0);
                                    buttonOperationExportItemsFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationExportItemsFilePick.setActionCommand("exportItemsFilePick");
                                    buttonOperationExportItemsFilePick.setName("exportitems");
                                    buttonOperationExportItemsFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationExportItemsFilePick.toolTipText"));
                                    buttonOperationExportItemsFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationExportItemsFilePick, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer10 ----
                                    vSpacer10.setMinimumSize(new Dimension(4, 30));
                                    vSpacer10.setPreferredSize(new Dimension(10, 30));
                                    vSpacer10.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer10, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationOverwrite ----
                                    labelOperationOverwrite.setText(context.cfg.gs("OperationsUI.labelOperationOverwrite.text"));
                                    panelCardPublisher.add(labelOperationOverwrite, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationOverwrite ----
                                    checkBoxOperationOverwrite.setName("overwrite");
                                    checkBoxOperationOverwrite.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationOverwrite.toolTipText"));
                                    checkBoxOperationOverwrite.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationOverwrite, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer11 ----
                                    vSpacer11.setMinimumSize(new Dimension(4, 30));
                                    vSpacer11.setPreferredSize(new Dimension(10, 30));
                                    vSpacer11.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer11, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationPreservedDates ----
                                    labelOperationPreservedDates.setText(context.cfg.gs("OperationsUI.labelOperationPreservedDates.text"));
                                    panelCardPublisher.add(labelOperationPreservedDates, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationPreserveDates ----
                                    checkBoxOperationPreserveDates.setName("preserveDates");
                                    checkBoxOperationPreserveDates.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationPreserveDates.toolTipText"));
                                    checkBoxOperationPreserveDates.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationPreserveDates, new GridBagConstraints(5, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- comboBoxOperationHintKeys ----
                                    comboBoxOperationHintKeys.setPrototypeDisplayValue(context.cfg.gs("OperationsUI.comboBoxOperationHintKeys.prototypeDisplayValue"));
                                    comboBoxOperationHintKeys.setModel(new DefaultComboBoxModel<>(new String[] {
                                        "Hint keys:",
                                        "Hint keys, only:"
                                    }));
                                    comboBoxOperationHintKeys.setMinimumSize(new Dimension(60, 30));
                                    comboBoxOperationHintKeys.setName("keys");
                                    comboBoxOperationHintKeys.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(comboBoxOperationHintKeys, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationHintKeys ----
                                    textFieldOperationHintKeys.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys.setName("hintKeys");
                                    textFieldOperationHintKeys.setMaximumSize(new Dimension(32767, 30));
                                    textFieldOperationHintKeys.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationHintKeys.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(textFieldOperationHintKeys, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationHintKeysFilePick ----
                                    buttonOperationHintKeysFilePick.setText("...");
                                    buttonOperationHintKeysFilePick.setFont(buttonOperationHintKeysFilePick.getFont().deriveFont(buttonOperationHintKeysFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationHintKeysFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationHintKeysFilePick.setIconTextGap(0);
                                    buttonOperationHintKeysFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationHintKeysFilePick.setActionCommand("hintKeysFilePick");
                                    buttonOperationHintKeysFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationHintKeysFilePick.toolTipText"));
                                    buttonOperationHintKeysFilePick.setName("hintkeys");
                                    buttonOperationHintKeysFilePick.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(buttonOperationHintKeysFilePick, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer19 ----
                                    vSpacer19.setMinimumSize(new Dimension(4, 30));
                                    vSpacer19.setPreferredSize(new Dimension(10, 30));
                                    vSpacer19.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer19, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationValidate ----
                                    labelOperationValidate.setText(context.cfg.gs("OperationsUI.labelOperationValidate.text"));
                                    panelCardPublisher.add(labelOperationValidate, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationValidate ----
                                    checkBoxOperationValidate.setName("validate");
                                    checkBoxOperationValidate.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationValidate.toolTipText"));
                                    checkBoxOperationValidate.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationValidate, new GridBagConstraints(5, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationKeepGoing ----
                                    labelOperationKeepGoing.setText(context.cfg.gs("OperationsUI.labelOperationKeepGoing.text"));
                                    panelCardPublisher.add(labelOperationKeepGoing, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- checkBoxOperationKeepGoing ----
                                    checkBoxOperationKeepGoing.setName("keepgoing");
                                    checkBoxOperationKeepGoing.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationKeepGoing.toolTipText"));
                                    checkBoxOperationKeepGoing.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationKeepGoing, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer18 ----
                                    vSpacer18.setMinimumSize(new Dimension(4, 30));
                                    vSpacer18.setPreferredSize(new Dimension(10, 30));
                                    vSpacer18.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer18, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationQuitStatusServer ----
                                    labelOperationQuitStatusServer.setText(context.cfg.gs("OperationsUI.labelOperationQuitStatusServer.text"));
                                    panelCardPublisher.add(labelOperationQuitStatusServer, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- checkBoxOperationQuitStatus ----
                                    checkBoxOperationQuitStatus.setName("quitstatusserver");
                                    checkBoxOperationQuitStatus.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationQuitStatus.toolTipText"));
                                    checkBoxOperationQuitStatus.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationQuitStatus, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer17 ----
                                    vSpacer17.setMinimumSize(new Dimension(4, 30));
                                    vSpacer17.setPreferredSize(new Dimension(10, 30));
                                    vSpacer17.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer17, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationDuplicates ----
                                    labelOperationDuplicates.setText(context.cfg.gs("OperationsUI.labelOperationDuplicates.text"));
                                    panelCardPublisher.add(labelOperationDuplicates, new GridBagConstraints(4, 11, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationDuplicates ----
                                    checkBoxOperationDuplicates.setName("duplicates");
                                    checkBoxOperationDuplicates.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationDuplicates.toolTipText"));
                                    checkBoxOperationDuplicates.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationDuplicates, new GridBagConstraints(5, 11, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer16 ----
                                    vSpacer16.setMinimumSize(new Dimension(4, 30));
                                    vSpacer16.setPreferredSize(new Dimension(10, 30));
                                    vSpacer16.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer16, new GridBagConstraints(3, 12, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationCrossCheck ----
                                    labelOperationCrossCheck.setText(context.cfg.gs("OperationsUI.labelOperationCrossCheck.text"));
                                    panelCardPublisher.add(labelOperationCrossCheck, new GridBagConstraints(4, 12, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 8, 4, 4), 0, 0));

                                    //---- checkBoxOperationCrossCheck ----
                                    checkBoxOperationCrossCheck.setName("crossCheck");
                                    checkBoxOperationCrossCheck.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationCrossCheck.toolTipText"));
                                    checkBoxOperationCrossCheck.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationCrossCheck, new GridBagConstraints(5, 12, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer15 ----
                                    vSpacer15.setMinimumSize(new Dimension(4, 30));
                                    vSpacer15.setPreferredSize(new Dimension(10, 30));
                                    vSpacer15.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer15, new GridBagConstraints(3, 13, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationEmptyDirectories ----
                                    labelOperationEmptyDirectories.setText(context.cfg.gs("OperationsUI.labelOperationEmptyDirectories.text"));
                                    panelCardPublisher.add(labelOperationEmptyDirectories, new GridBagConstraints(4, 13, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationEmptyDirectories ----
                                    checkBoxOperationEmptyDirectories.setName("emptyDirectories");
                                    checkBoxOperationEmptyDirectories.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationEmptyDirectories.toolTipText"));
                                    checkBoxOperationEmptyDirectories.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationEmptyDirectories, new GridBagConstraints(5, 13, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer14 ----
                                    vSpacer14.setMinimumSize(new Dimension(4, 30));
                                    vSpacer14.setPreferredSize(new Dimension(10, 30));
                                    vSpacer14.setMaximumSize(new Dimension(20, 30));
                                    panelCardPublisher.add(vSpacer14, new GridBagConstraints(3, 14, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- labelOperationIgnored ----
                                    labelOperationIgnored.setText(context.cfg.gs("OperationsUI.labelOperationIgnored.text"));
                                    panelCardPublisher.add(labelOperationIgnored, new GridBagConstraints(4, 14, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- checkBoxOperationIgnored ----
                                    checkBoxOperationIgnored.setName("ignored");
                                    checkBoxOperationIgnored.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationIgnored.toolTipText"));
                                    checkBoxOperationIgnored.addActionListener(e -> genericAction(e));
                                    panelCardPublisher.add(checkBoxOperationIgnored, new GridBagConstraints(5, 14, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));
                                }
                                panelOperationCards.add(panelCardPublisher, "publisher");

                                //======== panelCardListener ========
                                {
                                    panelCardListener.setName("listener");
                                    panelCardListener.setPreferredSize(new Dimension(824, 500));
                                    panelCardListener.setMinimumSize(new Dimension(0, 0));
                                    panelCardListener.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelCardListener.getLayout()).rowHeights = new int[] {0, 0, 28, 34, 32, 0, 0, 0, 0, 0, 0};
                                    ((GridBagLayout)panelCardListener.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0};
                                    ((GridBagLayout)panelCardListener.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                    //---- hSpacer6 ----
                                    hSpacer6.setMinimumSize(new Dimension(154, 2));
                                    hSpacer6.setPreferredSize(new Dimension(154, 2));
                                    hSpacer6.setMaximumSize(new Dimension(154, 2));
                                    panelCardListener.add(hSpacer6, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer40 ----
                                    vSpacer40.setPreferredSize(new Dimension(2, 8));
                                    vSpacer40.setMinimumSize(new Dimension(2, 8));
                                    vSpacer40.setMaximumSize(new Dimension(2, 8));
                                    panelCardListener.add(vSpacer40, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- hSpacer7 ----
                                    hSpacer7.setMinimumSize(new Dimension(154, 2));
                                    hSpacer7.setPreferredSize(new Dimension(154, 2));
                                    hSpacer7.setMaximumSize(new Dimension(154, 2));
                                    panelCardListener.add(hSpacer7, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationTargets2 ----
                                    labelOperationTargets2.setText(context.cfg.gs("OperationsUI.labelOperationTargets2.text"));
                                    panelCardListener.add(labelOperationTargets2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationTargets2 ----
                                    textFieldOperationTargets2.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationTargets2.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationTargets2.setName("targets2");
                                    textFieldOperationTargets2.setMargin(new Insets(0, 6, 2, 6));
                                    textFieldOperationTargets2.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationTargets2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(textFieldOperationTargets2, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationTargetsFilePick2 ----
                                    buttonOperationTargetsFilePick2.setText("...");
                                    buttonOperationTargetsFilePick2.setFont(buttonOperationTargetsFilePick2.getFont().deriveFont(buttonOperationTargetsFilePick2.getFont().getStyle() | Font.BOLD));
                                    buttonOperationTargetsFilePick2.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick2.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick2.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationTargetsFilePick2.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationTargetsFilePick2.setIconTextGap(0);
                                    buttonOperationTargetsFilePick2.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationTargetsFilePick2.setActionCommand("targetsFilePick");
                                    buttonOperationTargetsFilePick2.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationTargetsFilePick2.toolTipText"));
                                    buttonOperationTargetsFilePick2.setName("targets2");
                                    buttonOperationTargetsFilePick2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(buttonOperationTargetsFilePick2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer32 ----
                                    vSpacer32.setMinimumSize(new Dimension(4, 30));
                                    vSpacer32.setPreferredSize(new Dimension(10, 30));
                                    vSpacer32.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer32, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //======== panelOperationExcludeBox ========
                                    {
                                        panelOperationExcludeBox.setPreferredSize(new Dimension(240, 120));
                                        panelOperationExcludeBox.setMinimumSize(new Dimension(240, 120));
                                        panelOperationExcludeBox.setLayout(new BoxLayout(panelOperationExcludeBox, BoxLayout.Y_AXIS));

                                        //======== scrollPaneOperationExclude ========
                                        {
                                            scrollPaneOperationExclude.setPreferredSize(new Dimension(52, 120));

                                            //---- listOperationExclude ----
                                            listOperationExclude.setName("exclude");
                                            listOperationExclude.setVisibleRowCount(5);
                                            listOperationExclude.setModel(new AbstractListModel<String>() {
                                                String[] values = {
                                                    "Item 1",
                                                    "Item 2",
                                                    "Item 3",
                                                    "Item 4",
                                                    "Item 5",
                                                    "Item 6"
                                                };
                                                @Override
                                                public int getSize() { return values.length; }
                                                @Override
                                                public String getElementAt(int i) { return values[i]; }
                                            });
                                            listOperationExclude.setToolTipText(context.cfg.gs("OperationsUI.listOperationExclude.toolTipText"));
                                            listOperationExclude.setMaximumSize(new Dimension(32767, 32767));
                                            scrollPaneOperationExclude.setViewportView(listOperationExclude);
                                        }
                                        panelOperationExcludeBox.add(scrollPaneOperationExclude);

                                        //======== panelOperationExcludeButtons ========
                                        {
                                            panelOperationExcludeButtons.setPreferredSize(new Dimension(250, 28));
                                            panelOperationExcludeButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                            //---- buttonOperationAddExclude ----
                                            buttonOperationAddExclude.setText(context.cfg.gs("OperationsUI.buttonOperationAddExclude.text"));
                                            buttonOperationAddExclude.setFont(buttonOperationAddExclude.getFont().deriveFont(buttonOperationAddExclude.getFont().getSize() - 2f));
                                            buttonOperationAddExclude.setPreferredSize(new Dimension(78, 24));
                                            buttonOperationAddExclude.setMinimumSize(new Dimension(78, 24));
                                            buttonOperationAddExclude.setMaximumSize(new Dimension(78, 24));
                                            buttonOperationAddExclude.setMnemonic(context.cfg.gs("OperationsUI.buttonOperationAddExclude.mnemonic").charAt(0));
                                            buttonOperationAddExclude.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationAddExclude.toolTipText"));
                                            buttonOperationAddExclude.setName("addexc");
                                            buttonOperationAddExclude.addActionListener(e -> actionOperationAddRowClicked(e));
                                            panelOperationExcludeButtons.add(buttonOperationAddExclude);

                                            //---- buttonOperationRemoveExclude ----
                                            buttonOperationRemoveExclude.setText(context.cfg.gs("OperationsUI.buttonOperationRemoveExclude.text"));
                                            buttonOperationRemoveExclude.setFont(buttonOperationRemoveExclude.getFont().deriveFont(buttonOperationRemoveExclude.getFont().getSize() - 2f));
                                            buttonOperationRemoveExclude.setPreferredSize(new Dimension(78, 24));
                                            buttonOperationRemoveExclude.setMinimumSize(new Dimension(78, 24));
                                            buttonOperationRemoveExclude.setMaximumSize(new Dimension(78, 24));
                                            buttonOperationRemoveExclude.setMnemonic(context.cfg.gs("OperationsUI.buttonOperationRemoveExclude.mnemonic").charAt(0));
                                            buttonOperationRemoveExclude.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationRemoveExclude.toolTipText"));
                                            buttonOperationRemoveExclude.setName("removeexc");
                                            buttonOperationRemoveExclude.addActionListener(e -> actionOperationRemoveRowClicked(e));
                                            panelOperationExcludeButtons.add(buttonOperationRemoveExclude);
                                        }
                                        panelOperationExcludeBox.add(panelOperationExcludeButtons);
                                    }
                                    panelCardListener.add(panelOperationExcludeBox, new GridBagConstraints(5, 1, 1, 4, 0.0, 0.0,
                                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationExclude ----
                                    labelOperationExclude.setText(context.cfg.gs("OperationsUI.labelOperationExclude.text"));
                                    panelCardListener.add(labelOperationExclude, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer8 ----
                                    vSpacer8.setMinimumSize(new Dimension(4, 30));
                                    vSpacer8.setPreferredSize(new Dimension(10, 30));
                                    vSpacer8.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer8, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationAuthorize ----
                                    labelOperationAuthorize.setText(context.cfg.gs("OperationsUI.labelOperationAuthorize.text"));
                                    panelCardListener.add(labelOperationAuthorize, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- passwordFieldOperationsAuthorize ----
                                    passwordFieldOperationsAuthorize.setToolTipText(context.cfg.gs("OperationsUI.passwordFieldOperationsAuthorize.toolTipText"));
                                    passwordFieldOperationsAuthorize.setName("authpassword");
                                    passwordFieldOperationsAuthorize.setPreferredSize(new Dimension(240, 30));
                                    passwordFieldOperationsAuthorize.setMinimumSize(new Dimension(240, 30));
                                    passwordFieldOperationsAuthorize.addActionListener(e -> genericAction(e));
                                    passwordFieldOperationsAuthorize.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    panelCardListener.add(passwordFieldOperationsAuthorize, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer12 ----
                                    vSpacer12.setMinimumSize(new Dimension(4, 30));
                                    vSpacer12.setPreferredSize(new Dimension(10, 30));
                                    vSpacer12.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer12, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationAuthKeys ----
                                    labelOperationAuthKeys.setText(context.cfg.gs("OperationsUI.labelOperationAuthKeys.text"));
                                    panelCardListener.add(labelOperationAuthKeys, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationAuthKeys ----
                                    textFieldOperationAuthKeys.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationAuthKeys.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationAuthKeys.setName("authkeys");
                                    textFieldOperationAuthKeys.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationAuthKeys.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(textFieldOperationAuthKeys, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationAuthKeysFilePick ----
                                    buttonOperationAuthKeysFilePick.setText("...");
                                    buttonOperationAuthKeysFilePick.setFont(buttonOperationAuthKeysFilePick.getFont().deriveFont(buttonOperationAuthKeysFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationAuthKeysFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationAuthKeysFilePick.setIconTextGap(0);
                                    buttonOperationAuthKeysFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationAuthKeysFilePick.setActionCommand("authKeysFilePick");
                                    buttonOperationAuthKeysFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationAuthKeysFilePick.toolTipText"));
                                    buttonOperationAuthKeysFilePick.setName("authkeys");
                                    buttonOperationAuthKeysFilePick.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(buttonOperationAuthKeysFilePick, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer20 ----
                                    vSpacer20.setMinimumSize(new Dimension(4, 30));
                                    vSpacer20.setPreferredSize(new Dimension(10, 30));
                                    vSpacer20.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer20, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationBlacklist ----
                                    labelOperationBlacklist.setText(context.cfg.gs("OperationsUI.labelOperationBlacklist.text"));
                                    panelCardListener.add(labelOperationBlacklist, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationBlacklist ----
                                    textFieldOperationBlacklist.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationBlacklist.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationBlacklist.setName("blacklist");
                                    textFieldOperationBlacklist.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationBlacklist.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(textFieldOperationBlacklist, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationBlacklistFilePick ----
                                    buttonOperationBlacklistFilePick.setText("...");
                                    buttonOperationBlacklistFilePick.setFont(buttonOperationBlacklistFilePick.getFont().deriveFont(buttonOperationBlacklistFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationBlacklistFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationBlacklistFilePick.setIconTextGap(0);
                                    buttonOperationBlacklistFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationBlacklistFilePick.setActionCommand("blacklistFilePick");
                                    buttonOperationBlacklistFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationBlacklistFilePick.toolTipText"));
                                    buttonOperationBlacklistFilePick.setName("blacklist");
                                    buttonOperationBlacklistFilePick.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(buttonOperationBlacklistFilePick, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer21 ----
                                    vSpacer21.setMinimumSize(new Dimension(4, 30));
                                    vSpacer21.setPreferredSize(new Dimension(10, 30));
                                    vSpacer21.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer21, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationDecimalScale2 ----
                                    labelOperationDecimalScale2.setText(context.cfg.gs("OperationsUI.labelOperationDecimalScale2.text"));
                                    panelCardListener.add(labelOperationDecimalScale2, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationDecimalScale2 ----
                                    checkBoxOperationDecimalScale2.setName("decimalScale2");
                                    checkBoxOperationDecimalScale2.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationDecimalScale2.toolTipText"));
                                    checkBoxOperationDecimalScale2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(checkBoxOperationDecimalScale2, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationIpWhitelist ----
                                    labelOperationIpWhitelist.setText(context.cfg.gs("OperationsUI.labelOperationIpWhitelist.text"));
                                    panelCardListener.add(labelOperationIpWhitelist, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationIpWhitelist ----
                                    textFieldOperationIpWhitelist.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationIpWhitelist.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationIpWhitelist.setName("ipwhitelist");
                                    textFieldOperationIpWhitelist.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationIpWhitelist.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(textFieldOperationIpWhitelist, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationIpWhitelistFilePick ----
                                    buttonOperationIpWhitelistFilePick.setText("...");
                                    buttonOperationIpWhitelistFilePick.setFont(buttonOperationIpWhitelistFilePick.getFont().deriveFont(buttonOperationIpWhitelistFilePick.getFont().getStyle() | Font.BOLD));
                                    buttonOperationIpWhitelistFilePick.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationIpWhitelistFilePick.setIconTextGap(0);
                                    buttonOperationIpWhitelistFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationIpWhitelistFilePick.setActionCommand("ipWhitelistFilePick");
                                    buttonOperationIpWhitelistFilePick.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationIpWhitelistFilePick.toolTipText"));
                                    buttonOperationIpWhitelistFilePick.setName("ipwhitelist");
                                    buttonOperationIpWhitelistFilePick.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(buttonOperationIpWhitelistFilePick, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer22 ----
                                    vSpacer22.setMinimumSize(new Dimension(4, 30));
                                    vSpacer22.setPreferredSize(new Dimension(10, 30));
                                    vSpacer22.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer22, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationOverwrite2 ----
                                    labelOperationOverwrite2.setText(context.cfg.gs("OperationsUI.labelOperationOverwrite2.text"));
                                    panelCardListener.add(labelOperationOverwrite2, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationOverwrite2 ----
                                    checkBoxOperationOverwrite2.setName("overwrite2");
                                    checkBoxOperationOverwrite2.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationOverwrite2.toolTipText"));
                                    checkBoxOperationOverwrite2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(checkBoxOperationOverwrite2, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer23 ----
                                    vSpacer23.setMinimumSize(new Dimension(4, 30));
                                    vSpacer23.setPreferredSize(new Dimension(10, 30));
                                    vSpacer23.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer23, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationPreservedDates2 ----
                                    labelOperationPreservedDates2.setText(context.cfg.gs("OperationsUI.labelOperationPreservedDates2.text"));
                                    panelCardListener.add(labelOperationPreservedDates2, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationPreserveDates2 ----
                                    checkBoxOperationPreserveDates2.setName("preserveDates2");
                                    checkBoxOperationPreserveDates2.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationPreserveDates2.toolTipText"));
                                    checkBoxOperationPreserveDates2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(checkBoxOperationPreserveDates2, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationHintKeys ----
                                    labelOperationHintKeys.setText(context.cfg.gs("OperationsUI.labelOperationHintKeys.text"));
                                    labelOperationHintKeys.setName("keys2");
                                    panelCardListener.add(labelOperationHintKeys, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 4, 4), 0, 0));

                                    //---- textFieldOperationHintKeys2 ----
                                    textFieldOperationHintKeys2.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys2.setName("hintKeys2");
                                    textFieldOperationHintKeys2.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys2.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationHintKeys2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(textFieldOperationHintKeys2, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationHintKeysFilePick2 ----
                                    buttonOperationHintKeysFilePick2.setText("...");
                                    buttonOperationHintKeysFilePick2.setFont(buttonOperationHintKeysFilePick2.getFont().deriveFont(buttonOperationHintKeysFilePick2.getFont().getStyle() | Font.BOLD));
                                    buttonOperationHintKeysFilePick2.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick2.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick2.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick2.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationHintKeysFilePick2.setIconTextGap(0);
                                    buttonOperationHintKeysFilePick2.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationHintKeysFilePick2.setActionCommand("hintKeysFilePick");
                                    buttonOperationHintKeysFilePick2.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationHintKeysFilePick2.toolTipText"));
                                    buttonOperationHintKeysFilePick2.setName("hintkeys2");
                                    buttonOperationHintKeysFilePick2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(buttonOperationHintKeysFilePick2, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer24 ----
                                    vSpacer24.setMinimumSize(new Dimension(4, 30));
                                    vSpacer24.setPreferredSize(new Dimension(10, 30));
                                    vSpacer24.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer24, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationKeepGoing2 ----
                                    labelOperationKeepGoing2.setText(context.cfg.gs("OperationsUI.labelOperationKeepGoing2.text"));
                                    panelCardListener.add(labelOperationKeepGoing2, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 0, 4), 0, 0));

                                    //---- checkBoxOperationKeepGoing2 ----
                                    checkBoxOperationKeepGoing2.setName("keepgoing2");
                                    checkBoxOperationKeepGoing2.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationKeepGoing2.toolTipText"));
                                    checkBoxOperationKeepGoing2.addActionListener(e -> genericAction(e));
                                    panelCardListener.add(checkBoxOperationKeepGoing2, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- vSpacer26 ----
                                    vSpacer26.setMinimumSize(new Dimension(4, 30));
                                    vSpacer26.setPreferredSize(new Dimension(10, 30));
                                    vSpacer26.setMaximumSize(new Dimension(20, 30));
                                    panelCardListener.add(vSpacer26, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));
                                }
                                panelOperationCards.add(panelCardListener, "listener");

                                //======== panelCardHintServer ========
                                {
                                    panelCardHintServer.setName("hintserver");
                                    panelCardHintServer.setPreferredSize(new Dimension(824, 500));
                                    panelCardHintServer.setMinimumSize(new Dimension(0, 0));
                                    panelCardHintServer.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelCardHintServer.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
                                    ((GridBagLayout)panelCardHintServer.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                    //---- vSpacer41 ----
                                    vSpacer41.setMaximumSize(new Dimension(32767, 8));
                                    vSpacer41.setMinimumSize(new Dimension(12, 8));
                                    vSpacer41.setPreferredSize(new Dimension(10, 8));
                                    panelCardHintServer.add(vSpacer41, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationHintKeys2 ----
                                    labelOperationHintKeys2.setText(context.cfg.gs("OperationsUI.labelOperationHintKeys2.text"));
                                    panelCardHintServer.add(labelOperationHintKeys2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- textFieldOperationHintKeys3 ----
                                    textFieldOperationHintKeys3.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys3.setName("hintKeys3");
                                    textFieldOperationHintKeys3.setMaximumSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys3.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationHintKeys3.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationHintKeys3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(textFieldOperationHintKeys3, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationHintKeysFilePick3 ----
                                    buttonOperationHintKeysFilePick3.setText("...");
                                    buttonOperationHintKeysFilePick3.setFont(buttonOperationHintKeysFilePick3.getFont().deriveFont(buttonOperationHintKeysFilePick3.getFont().getStyle() | Font.BOLD));
                                    buttonOperationHintKeysFilePick3.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick3.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick3.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationHintKeysFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationHintKeysFilePick3.setIconTextGap(0);
                                    buttonOperationHintKeysFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationHintKeysFilePick3.setActionCommand("hintKeysFilePick");
                                    buttonOperationHintKeysFilePick3.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationHintKeysFilePick3.toolTipText"));
                                    buttonOperationHintKeysFilePick3.setName("hintkeys3");
                                    buttonOperationHintKeysFilePick3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(buttonOperationHintKeysFilePick3, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer34 ----
                                    vSpacer34.setMinimumSize(new Dimension(10, 30));
                                    vSpacer34.setPreferredSize(new Dimension(20, 30));
                                    vSpacer34.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer34, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- label1 ----
                                    label1.setText(context.cfg.gs("OperationsUI.labelOperationAuthKeys.text"));
                                    panelCardHintServer.add(label1, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- textFieldOperationAuthKeys3 ----
                                    textFieldOperationAuthKeys3.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationAuthKeys3.setName("authkeys3");
                                    textFieldOperationAuthKeys3.setMaximumSize(new Dimension(240, 30));
                                    textFieldOperationAuthKeys3.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationAuthKeys3.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationAuthKeys3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(textFieldOperationAuthKeys3, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationAuthKeysFilePick3 ----
                                    buttonOperationAuthKeysFilePick3.setText("...");
                                    buttonOperationAuthKeysFilePick3.setFont(buttonOperationAuthKeysFilePick3.getFont().deriveFont(buttonOperationAuthKeysFilePick3.getFont().getStyle() | Font.BOLD));
                                    buttonOperationAuthKeysFilePick3.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick3.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick3.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationAuthKeysFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationAuthKeysFilePick3.setIconTextGap(0);
                                    buttonOperationAuthKeysFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationAuthKeysFilePick3.setActionCommand("authKeysFilePick");
                                    buttonOperationAuthKeysFilePick3.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationAuthKeysFilePick.toolTipText"));
                                    buttonOperationAuthKeysFilePick3.setName("authkeys3");
                                    buttonOperationAuthKeysFilePick3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(buttonOperationAuthKeysFilePick3, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer44 ----
                                    vSpacer44.setMinimumSize(new Dimension(10, 30));
                                    vSpacer44.setPreferredSize(new Dimension(20, 30));
                                    vSpacer44.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer44, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationKeepGoing3 ----
                                    labelOperationKeepGoing3.setText(context.cfg.gs("OperationsUI.labelOperationKeepGoing3.text"));
                                    panelCardHintServer.add(labelOperationKeepGoing3, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- checkBoxOperationKeepGoing3 ----
                                    checkBoxOperationKeepGoing3.setName("keepgoing3");
                                    checkBoxOperationKeepGoing3.setToolTipText(context.cfg.gs("OperationsUI.checkBoxOperationKeepGoing3.toolTipText"));
                                    checkBoxOperationKeepGoing3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(checkBoxOperationKeepGoing3, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer36 ----
                                    vSpacer36.setMinimumSize(new Dimension(10, 30));
                                    vSpacer36.setPreferredSize(new Dimension(20, 30));
                                    vSpacer36.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer36, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer39 ----
                                    vSpacer39.setMinimumSize(new Dimension(10, 30));
                                    vSpacer39.setPreferredSize(new Dimension(20, 30));
                                    vSpacer39.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer39, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationBlacklist3 ----
                                    labelOperationBlacklist3.setText(context.cfg.gs("OperationsUI.labelOperationBlacklist3.text"));
                                    panelCardHintServer.add(labelOperationBlacklist3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- textFieldOperationBlacklist3 ----
                                    textFieldOperationBlacklist3.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationBlacklist3.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationBlacklist3.setName("blacklist3");
                                    textFieldOperationBlacklist3.setMaximumSize(new Dimension(240, 30));
                                    textFieldOperationBlacklist3.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationBlacklist3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(textFieldOperationBlacklist3, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- buttonOperationBlacklistFilePick3 ----
                                    buttonOperationBlacklistFilePick3.setText("...");
                                    buttonOperationBlacklistFilePick3.setFont(buttonOperationBlacklistFilePick3.getFont().deriveFont(buttonOperationBlacklistFilePick3.getFont().getStyle() | Font.BOLD));
                                    buttonOperationBlacklistFilePick3.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick3.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick3.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationBlacklistFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationBlacklistFilePick3.setIconTextGap(0);
                                    buttonOperationBlacklistFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationBlacklistFilePick3.setActionCommand("blacklistFilePick");
                                    buttonOperationBlacklistFilePick3.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationBlacklistFilePick3.toolTipText"));
                                    buttonOperationBlacklistFilePick3.setName("blacklist3");
                                    buttonOperationBlacklistFilePick3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(buttonOperationBlacklistFilePick3, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- vSpacer37 ----
                                    vSpacer37.setMinimumSize(new Dimension(10, 30));
                                    vSpacer37.setPreferredSize(new Dimension(20, 30));
                                    vSpacer37.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer37, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 4, 4), 0, 0));

                                    //---- labelOperationIpWhitelist3 ----
                                    labelOperationIpWhitelist3.setText(context.cfg.gs("OperationsUI.labelOperationIpWhitelist3.text"));
                                    panelCardHintServer.add(labelOperationIpWhitelist3, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- textFieldOperationIpWhitelist3 ----
                                    textFieldOperationIpWhitelist3.setPreferredSize(new Dimension(240, 30));
                                    textFieldOperationIpWhitelist3.setMinimumSize(new Dimension(240, 30));
                                    textFieldOperationIpWhitelist3.setName("ipwhitelist3");
                                    textFieldOperationIpWhitelist3.setMaximumSize(new Dimension(240, 30));
                                    textFieldOperationIpWhitelist3.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    textFieldOperationIpWhitelist3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(textFieldOperationIpWhitelist3, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- buttonOperationIpWhitelistFilePick3 ----
                                    buttonOperationIpWhitelistFilePick3.setText("...");
                                    buttonOperationIpWhitelistFilePick3.setFont(buttonOperationIpWhitelistFilePick3.getFont().deriveFont(buttonOperationIpWhitelistFilePick3.getFont().getStyle() | Font.BOLD));
                                    buttonOperationIpWhitelistFilePick3.setMaximumSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick3.setMinimumSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick3.setPreferredSize(new Dimension(32, 24));
                                    buttonOperationIpWhitelistFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                    buttonOperationIpWhitelistFilePick3.setIconTextGap(0);
                                    buttonOperationIpWhitelistFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                    buttonOperationIpWhitelistFilePick3.setActionCommand("ipWhitelistFilePick");
                                    buttonOperationIpWhitelistFilePick3.setToolTipText(context.cfg.gs("OperationsUI.buttonOperationIpWhitelistFilePick3.toolTipText"));
                                    buttonOperationIpWhitelistFilePick3.setName("ipwhitelist3");
                                    buttonOperationIpWhitelistFilePick3.addActionListener(e -> genericAction(e));
                                    panelCardHintServer.add(buttonOperationIpWhitelistFilePick3, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));

                                    //---- vSpacer38 ----
                                    vSpacer38.setMinimumSize(new Dimension(10, 30));
                                    vSpacer38.setPreferredSize(new Dimension(20, 30));
                                    vSpacer38.setMaximumSize(new Dimension(20, 30));
                                    panelCardHintServer.add(vSpacer38, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 4), 0, 0));
                                }
                                panelOperationCards.add(panelCardHintServer, "hintserver");

                                //======== panelCardTerminal ========
                                {
                                    panelCardTerminal.setName("terminal");
                                    panelCardTerminal.setMinimumSize(new Dimension(0, 0));
                                    panelCardTerminal.setPreferredSize(new Dimension(824, 500));
                                    panelCardTerminal.setLayout(new BorderLayout());

                                    //---- labelOperationsTerminal ----
                                    labelOperationsTerminal.setText(context.cfg.gs("OperationsUI.labelOperationsTerminal.text"));
                                    labelOperationsTerminal.setHorizontalAlignment(SwingConstants.CENTER);
                                    panelCardTerminal.add(labelOperationsTerminal, BorderLayout.CENTER);
                                }
                                panelOperationCards.add(panelCardTerminal, "terminal");

                                //======== panelCardQuit ========
                                {
                                    panelCardQuit.setName("quit");
                                    panelCardQuit.setMinimumSize(new Dimension(0, 0));
                                    panelCardQuit.setPreferredSize(new Dimension(824, 500));
                                    panelCardQuit.setLayout(new BorderLayout());

                                    //---- labelOperationsQuitter ----
                                    labelOperationsQuitter.setText(context.cfg.gs("OperationsUI.labelOperationsQuitter.text"));
                                    labelOperationsQuitter.setHorizontalAlignment(SwingConstants.CENTER);
                                    panelCardQuit.add(labelOperationsQuitter, BorderLayout.CENTER);
                                }
                                panelOperationCards.add(panelCardQuit, "subscriberquit");

                                //======== panelCardQuitHints ========
                                {
                                    panelCardQuitHints.setName("hintserver");
                                    panelCardQuitHints.setPreferredSize(new Dimension(824, 500));
                                    panelCardQuitHints.setMinimumSize(new Dimension(0, 0));
                                    panelCardQuitHints.setLayout(new BorderLayout());

                                    //---- labelOperationsQuitHints ----
                                    labelOperationsQuitHints.setText(context.cfg.gs("OperationsUI.labelOperationsQuitHints.text"));
                                    labelOperationsQuitHints.setHorizontalAlignment(SwingConstants.CENTER);
                                    panelCardQuitHints.add(labelOperationsQuitHints, BorderLayout.CENTER);
                                }
                                panelOperationCards.add(panelCardQuitHints, "statusquit");
                            }
                            scrollPaneOperationCards.setViewportView(panelOperationCards);
                        }
                        panelOperationOptions.add(scrollPaneOperationCards, BorderLayout.CENTER);
                    }
                    splitPaneOperationContent.setRightComponent(panelOperationOptions);
                }
                contentPanel.add(splitPaneOperationContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- buttonOperationSave ----
                buttonOperationSave.setText(context.cfg.gs("Z.save"));
                buttonOperationSave.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                buttonOperationSave.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(buttonOperationSave, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- buttonOperationCancel ----
                buttonOperationCancel.setText(context.cfg.gs("Z.cancel"));
                buttonOperationCancel.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
                buttonOperationCancel.addActionListener(e -> actionCancelClicked(e));
                buttonBar.add(buttonOperationCancel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    public JPanel dialogPane;
    public JPanel contentPanel;
    public JPanel panelOperationButtons;
    public JPanel panelTopOperationButtons;
    public JButton buttonNewOperation;
    public JButton buttonCopyOperation;
    public JButton buttonDeleteOperation;
    public JPanel panelOperationHelp;
    public JLabel labelOperationHelp;
    public JSplitPane splitPaneOperationContent;
    public JScrollPane scrollPaneOperationConfig;
    public JTable operationConfigItems;
    public JPanel panelOperationOptions;
    public JPanel panelOperationControls;
    public JPanel topOperationOptions;
    public JPanel vSpacer0;
    public JPanel panelOperationMode;
    public JPanel hSpacer3;
    public JLabel labelOperationMode;
    public JScrollPane scrollPaneOperationCards;
    public JPanel panelOperationCards;
    public JPanel panelCardGettingStarted;
    public JLabel labelOperationGettingStarted;
    public JPanel panelCardPublisher;
    public JPanel hSpacer4;
    public JPanel vSpacer3;
    public JPanel hSpacer5;
    public JLabel labelOperationNavigatorCheckbox;
    public JCheckBox checkBoxOperationNavigator;
    public JPanel vSpacer33;
    public JPanel panelOperationIncludeExcludeBox;
    public JScrollPane scrollPaneOperationIncludeExclude;
    public JList<String> listOperationIncludeExclude;
    public JPanel panelOperationIncludeExcludeButtons;
    public JButton buttonOperationAddIncludeExclude;
    public JButton buttonOperationRemoveIncludeExclude;
    public JLabel labelOperationIncludeExclude;
    public JPanel vSpacer4;
    public JLabel labelOperationTargets;
    public JTextField textFieldOperationTargets;
    public JButton buttonOperationTargetsFilePick;
    public JPanel vSpacer5;
    public JLabel labelOperationsMismatches;
    public JTextField textFieldOperationMismatches;
    public JButton buttonOperationMismatchesFilePick;
    public JPanel vSpacer6;
    public JComboBox<String> comboBoxOperationWhatsNew;
    public JTextField textFieldOperationWhatsNew;
    public JButton buttonOperationWhatsNewFilePick;
    public JPanel vSpacer7;
    public JLabel labelOperationDecimalScale;
    public JCheckBox checkBoxOperationDecimalScale;
    public JLabel labelOperationExportText;
    public JTextField textFieldOperationExportText;
    public JButton buttonOperationExportTextFilePick;
    public JPanel vSpacer9;
    public JLabel labelOperationNoBackfill;
    public JCheckBox checkBoxOperationNoBackFill;
    public JLabel labelOperationExportItems;
    public JTextField textFieldOperationExportItems;
    public JButton buttonOperationExportItemsFilePick;
    public JPanel vSpacer10;
    public JLabel labelOperationOverwrite;
    public JCheckBox checkBoxOperationOverwrite;
    public JPanel vSpacer11;
    public JLabel labelOperationPreservedDates;
    public JCheckBox checkBoxOperationPreserveDates;
    public JComboBox<String> comboBoxOperationHintKeys;
    public JTextField textFieldOperationHintKeys;
    public JButton buttonOperationHintKeysFilePick;
    public JPanel vSpacer19;
    public JLabel labelOperationValidate;
    public JCheckBox checkBoxOperationValidate;
    public JLabel labelOperationKeepGoing;
    public JCheckBox checkBoxOperationKeepGoing;
    public JPanel vSpacer18;
    public JLabel labelOperationQuitStatusServer;
    public JCheckBox checkBoxOperationQuitStatus;
    public JPanel vSpacer17;
    public JLabel labelOperationDuplicates;
    public JCheckBox checkBoxOperationDuplicates;
    public JPanel vSpacer16;
    public JLabel labelOperationCrossCheck;
    public JCheckBox checkBoxOperationCrossCheck;
    public JPanel vSpacer15;
    public JLabel labelOperationEmptyDirectories;
    public JCheckBox checkBoxOperationEmptyDirectories;
    public JPanel vSpacer14;
    public JLabel labelOperationIgnored;
    public JCheckBox checkBoxOperationIgnored;
    public JPanel panelCardListener;
    public JPanel hSpacer6;
    public JPanel vSpacer40;
    public JPanel hSpacer7;
    public JLabel labelOperationTargets2;
    public JTextField textFieldOperationTargets2;
    public JButton buttonOperationTargetsFilePick2;
    public JPanel vSpacer32;
    public JPanel panelOperationExcludeBox;
    public JScrollPane scrollPaneOperationExclude;
    public JList<String> listOperationExclude;
    public JPanel panelOperationExcludeButtons;
    public JButton buttonOperationAddExclude;
    public JButton buttonOperationRemoveExclude;
    public JLabel labelOperationExclude;
    public JPanel vSpacer8;
    public JLabel labelOperationAuthorize;
    public JPasswordField passwordFieldOperationsAuthorize;
    public JPanel vSpacer12;
    public JLabel labelOperationAuthKeys;
    public JTextField textFieldOperationAuthKeys;
    public JButton buttonOperationAuthKeysFilePick;
    public JPanel vSpacer20;
    public JLabel labelOperationBlacklist;
    public JTextField textFieldOperationBlacklist;
    public JButton buttonOperationBlacklistFilePick;
    public JPanel vSpacer21;
    public JLabel labelOperationDecimalScale2;
    public JCheckBox checkBoxOperationDecimalScale2;
    public JLabel labelOperationIpWhitelist;
    public JTextField textFieldOperationIpWhitelist;
    public JButton buttonOperationIpWhitelistFilePick;
    public JPanel vSpacer22;
    public JLabel labelOperationOverwrite2;
    public JCheckBox checkBoxOperationOverwrite2;
    public JPanel vSpacer23;
    public JLabel labelOperationPreservedDates2;
    public JCheckBox checkBoxOperationPreserveDates2;
    public JLabel labelOperationHintKeys;
    public JTextField textFieldOperationHintKeys2;
    public JButton buttonOperationHintKeysFilePick2;
    public JPanel vSpacer24;
    public JLabel labelOperationKeepGoing2;
    public JCheckBox checkBoxOperationKeepGoing2;
    public JPanel vSpacer26;
    public JPanel panelCardHintServer;
    public JPanel vSpacer41;
    public JLabel labelOperationHintKeys2;
    public JTextField textFieldOperationHintKeys3;
    public JButton buttonOperationHintKeysFilePick3;
    public JPanel vSpacer34;
    public JLabel label1;
    public JTextField textFieldOperationAuthKeys3;
    public JButton buttonOperationAuthKeysFilePick3;
    public JPanel vSpacer44;
    public JLabel labelOperationKeepGoing3;
    public JCheckBox checkBoxOperationKeepGoing3;
    public JPanel vSpacer36;
    public JPanel vSpacer39;
    public JLabel labelOperationBlacklist3;
    public JTextField textFieldOperationBlacklist3;
    public JButton buttonOperationBlacklistFilePick3;
    public JPanel vSpacer37;
    public JLabel labelOperationIpWhitelist3;
    public JTextField textFieldOperationIpWhitelist3;
    public JButton buttonOperationIpWhitelistFilePick3;
    public JPanel vSpacer38;
    public JPanel panelCardTerminal;
    public JLabel labelOperationsTerminal;
    public JPanel panelCardQuit;
    public JLabel labelOperationsQuitter;
    public JPanel panelCardQuitHints;
    public JLabel labelOperationsQuitHints;
    public JPanel buttonBar;
    public JButton buttonOperationSave;
    public JButton buttonOperationCancel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    //
    // @formatter:on
    // </editor-fold>
}
