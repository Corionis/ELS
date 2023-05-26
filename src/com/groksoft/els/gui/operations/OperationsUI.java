package com.groksoft.els.gui.operations;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.Generator;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.libraries.LibrariesUI;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.operations.OperationsTool;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings(value = "unchecked")
/**
 * Operations tab and Tool
 */
public class OperationsUI
{
    private JComboBox comboBoxMode;
    private JTable configItems;
    private Context context;
    private ConfigModel configModel;
    private int currentConfigIndex = -1;
    private OperationsTool currentTool;
    private ArrayList<OperationsTool> deletedTools;
    private String displayName;
    private boolean fileAny = false;
    private boolean fileKeys = false;
    private boolean fileMustExist = false;
    private File lastFile;
    private boolean lastIncluded = true;
    private JList<String> libJList = null;
    private NavHelp helpDialog;
    private LibrariesUI librariesUI;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private Mode[] modes;
    private SwingWorker<Void, Void> worker;
    private OperationsTool workerOperation = null;
    private boolean workerRunning = false;

    private OperationsUI()
    {
        // hide default constructor
    }

    public OperationsUI(Context context)
    {
        this.context = context;
        this.context.operationsUI = this;
        this.displayName = context.cfg.gs("Operations.displayName");
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && worker != null)
        {
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Operations.stop.running.operation"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerOperation.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Operations.config.cancelled"), workerOperation.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Z.cancel.all.changes"),
                        context.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    cancelChanges();
                }
            }
        }
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
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, displayName, JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            OperationsTool tool = (OperationsTool) configModel.getValueAt(index, 0);

            // TODO check if Tool is used in any Jobs, prompt user accordingly AND handle for rename too

            int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + tool.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                tool.setDataHasChanged();
                deletedTools.add(tool);
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
                    ((CardLayout) context.mainFrame.panelOperationCards.getLayout()).show(context.mainFrame.panelOperationCards, "gettingStarted");
                    context.mainFrame.labelOperationMode.setText("");
                    context.mainFrame.buttonCopyOperation.setEnabled(false);
                    context.mainFrame.buttonDeleteOperation.setEnabled(false);
                    context.mainFrame.buttonRunOperation.setEnabled(false);
                    context.mainFrame.buttonGenerateOperation.setEnabled(false);
                    context.mainFrame.buttonOperationSave.setEnabled(false);
                    context.mainFrame.buttonOperationCancel.setEnabled(false);
                    currentConfigIndex = 0;
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionGenerateClicked(ActionEvent evt)
    {
        if (configItems.isEditing())
            configItems.getCellEditor().stopCellEditing();
        Generator generator = new Generator(context);
        generator.showDialog(context.mainFrame, currentTool, currentTool.getConfigName());
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(context.mainFrame, context.mainFrame, context,
                    context.cfg.gs("Operations.help"), "operations_" + context.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            helpDialog.setVisible(true);
            // offset the help dialog from the parent dialog
            Point loc = context.mainFrame.getLocation();
            loc.x = loc.x + 32;
            loc.y = loc.y + 32;
            helpDialog.setLocation(loc);
        }
        else
        {
            helpDialog.toFront();
        }
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            String message = context.cfg.gs("Operations.mode.select.type");
            String line = context.cfg.gs("Use Local/Remote publish for Navigator. ");
            Object[] params = {message, line, comboBoxMode};
            comboBoxMode.setSelectedIndex(0);

            // get ELS operationsUI/mode
            int opt = JOptionPane.showConfirmDialog(context.mainFrame, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                currentTool = new OperationsTool(context);
                Mode mode = modes[comboBoxMode.getSelectedIndex()];
                currentTool.setConfigName(context.cfg.gs("Z.untitled"));
                currentTool.setOperation(mode.operation);
                currentTool.setCard(mode.card);
                currentTool.setDataHasChanged();
                initNewCard();

                context.mainFrame.buttonCopyOperation.setEnabled(true);
                context.mainFrame.buttonDeleteOperation.setEnabled(true);
                context.mainFrame.buttonRunOperation.setEnabled(true);
                context.mainFrame.buttonGenerateOperation.setEnabled(true);
                context.mainFrame.buttonOperationSave.setEnabled(true);
                context.mainFrame.buttonOperationCancel.setEnabled(true);

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
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.rename.the.existing") +
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
                if (cardVar == 1 && context.mainFrame.listOperationIncludeExclude.getModel().getSize() > 0)
                    indices = context.mainFrame.listOperationIncludeExclude.getSelectedIndices();
                else if (cardVar == 2 && context.mainFrame.listOperationExclude.getModel().getSize() > 0)
                    indices = context.mainFrame.listOperationExclude.getSelectedIndices();
                if (indices.length > 0)
                {
                    if (cardVar == 1)
                        context.mainFrame.listOperationIncludeExclude.requestFocus();
                    else if (cardVar == 2)
                        context.mainFrame.listOperationExclude.requestFocus();
                    int count = indices.length;

                    // confirm deletions
                    int reply = JOptionPane.showConfirmDialog(context.mainFrame,
                            MessageFormat.format(context.cfg.gs("Operations.are.you.sure.you.want.delete.entries"), count), displayName,
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        List<String> selections = context.mainFrame.listOperationIncludeExclude.getSelectedValuesList();
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

    private void actionRunClicked(ActionEvent e)
    {
        if (currentTool != null)
        {
            // confirm run of job
            String message = java.text.MessageFormat.format(context.cfg.gs("Operations.run.as.defined"), currentTool.getConfigName());
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, message, context.cfg.gs("Navigator.splitPane.Operations.tab.title"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerOperation = currentTool.clone();
                process();
            }
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        saveConfigurations();
        savePreferences();
    }

    public void cancelChanges()
    {
        configModel.setRowCount(0);
        configItems.revalidate();
        loadConfigurations();
        deletedTools = new ArrayList<OperationsTool>();
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
            {
                //logger.warn("unsaved changes in " + ((OperationsTool) configModel.getValueAt(i, 0)).getConfigName());
                return true;
            }
        }
        return false;
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
                if (fileKeys)
                    return (file.getName().toLowerCase().endsWith(".keys"));
                if (!fileAny)
                    return (file.getName().toLowerCase().endsWith(".json"));
                return true;
            }

            @Override
            public String getDescription()
            {
                String desc = "";
                fileKeys = false;
                switch(button.getName().toLowerCase())
                {
                    case "authkeys":
                        desc = context.cfg.gs("Operations.els.authkeys.file");
                        fileAny = false;
                        fileMustExist = true;
                        fileKeys = true;
                        break;
                    case "blacklist":
                    case "blacklist3":
                        desc = context.cfg.gs("Operations.els.blacklist.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
                    case "ipwhitelist":
                    case "ipwhitelist3":
                        desc = context.cfg.gs("Operations.els.ipwhitelist.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
                    case "targets":
                    case "targets2":
                        desc = context.cfg.gs("Operations.els.targets.file.json");
                        fileAny = false;
                        fileMustExist = true;
                        break;
                    case "mismatches":
                        desc = context.cfg.gs("Operations.els.mismatches.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "whatsnew":
                        desc = context.cfg.gs("Operations.els.what.s.new.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "exporttext":
                        desc = context.cfg.gs("Operations.els.export.text.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "exportitems":
                        desc = context.cfg.gs("Operations.els.export.items.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "hintkeys":
                    case "hintkeys2":
                    case "hintkeys3":
                        desc = context.cfg.gs("Operations.els.hint.keys.file");
                        fileAny = true;
                        fileMustExist = true;
                        fileKeys = true;
                        break;
                    case "hints":
                    case "hints2":
                    case "hints3":
                        desc = context.cfg.gs("Operations.els.hints.server.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
/*
                    case "log":
                    case "log2":
                        desc = context.cfg.gs("Operations.els.log.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
*/
                }
                return desc;
            }
        });

        String fileName = "";
        switch(button.getName().toLowerCase())
        {
            case "authkeys":
                fileName = context.mainFrame.textFieldOperationAuthKeys.getText();
                break;
            case "blacklist":
                fileName = context.mainFrame.textFieldOperationBlacklist.getText();
                break;
            case "blacklist3":
                fileName = context.mainFrame.textFieldOperationBlacklist3.getText();
                break;
            case "ipwhitelist":
                fileName = context.mainFrame.textFieldOperationIpWhitelist.getText();
                break;
            case "ipwhitelist3":
                fileName = context.mainFrame.textFieldOperationIpWhitelist3.getText();
                break;
            case "targets":
                fileName = context.mainFrame.textFieldOperationTargets.getText();
                break;
            case "targets2":
                fileName = context.mainFrame.textFieldOperationTargets2.getText();
                break;
            case "mismatches":
                fileName = context.mainFrame.textFieldOperationMismatches.getText();
                break;
            case "whatsnew":
                fileName = context.mainFrame.textFieldOperationWhatsNew.getText();
                break;
            case "exporttext":
                fileName = context.mainFrame.textFieldOperationExportText.getText();
                break;
            case "exportitems":
                fileName = context.mainFrame.textFieldOperationExportItems.getText();
                break;
            case "hintkeys":
                fileName = context.mainFrame.textFieldOperationHintKeys.getText();
                break;
            case "hintkeys2":
                fileName = context.mainFrame.textFieldOperationHintKeys2.getText();
                break;
            case "hintkeys3":
                fileName = context.mainFrame.textFieldOperationHintKeys3.getText();
                break;
            case "hints":
                fileName = context.mainFrame.textFieldOperationHints.getText();
                break;
            case "hints2":
                fileName = context.mainFrame.textFieldOperationHints2.getText();
                break;
            case "hints3":
                fileName = context.mainFrame.textFieldOperationHints3.getText();
                break;
        }

        File dir;
        File file;
        if (fileName.length() > 0)
        {
            dir = new File(filePickerDirectory(fileName));
            fc.setCurrentDirectory(dir.getAbsoluteFile());

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

        fc.setDialogType(fileMustExist ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {

                lastFile = fc.getCurrentDirectory();
                file = fc.getSelectedFile();

                // sanity checks
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (fileMustExist && !file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // make path relative if possible
                String path = "";
                if (file.getPath().startsWith(context.cfg.getWorkingDirectory()))
                    path = file.getPath().substring(context.cfg.getWorkingDirectory().length() + 1);
                else
                    path = file.getPath();

                // save value & fire updateOnChange()
                switch (button.getName().toLowerCase())
                {
                    // context.mainFrame.textFieldOperation
                    case "authkeys":
                        context.mainFrame.textFieldOperationAuthKeys.setText(path);
                        context.mainFrame.textFieldOperationAuthKeys.postActionEvent();
                        break;
                    case "blacklist":
                        context.mainFrame.textFieldOperationBlacklist.setText(path);
                        context.mainFrame.textFieldOperationBlacklist.postActionEvent();
                        break;
                    case "blacklist3":
                        context.mainFrame.textFieldOperationBlacklist3.setText(path);
                        context.mainFrame.textFieldOperationBlacklist3.postActionEvent();
                        break;
                    case "ipwhitelist":
                        context.mainFrame.textFieldOperationIpWhitelist.setText(path);
                        context.mainFrame.textFieldOperationIpWhitelist.postActionEvent();
                        break;
                    case "ipwhitelist3":
                        context.mainFrame.textFieldOperationIpWhitelist3.setText(path);
                        context.mainFrame.textFieldOperationIpWhitelist3.postActionEvent();
                        break;
                    case "targets":
                        context.mainFrame.textFieldOperationTargets.setText(path);
                        context.mainFrame.textFieldOperationTargets.postActionEvent();
                        break;
                    case "targets2":
                        context.mainFrame.textFieldOperationTargets2.setText(path);
                        context.mainFrame.textFieldOperationTargets2.postActionEvent();
                        break;
                    case "mismatches":
                        context.mainFrame.textFieldOperationMismatches.setText(path);
                        context.mainFrame.textFieldOperationMismatches.postActionEvent();
                        break;
                    case "whatsnew":
                        context.mainFrame.textFieldOperationWhatsNew.setText(path);
                        context.mainFrame.textFieldOperationWhatsNew.postActionEvent();
                        break;
                    case "exporttext":
                        context.mainFrame.textFieldOperationExportText.setText(path);
                        context.mainFrame.textFieldOperationExportText.postActionEvent();
                        break;
                    case "exportitems":
                        context.mainFrame.textFieldOperationExportItems.setText(path);
                        context.mainFrame.textFieldOperationExportItems.postActionEvent();
                        break;
                    case "hintkeys":
                        context.mainFrame.textFieldOperationHintKeys.setText(path);
                        context.mainFrame.textFieldOperationHintKeys.postActionEvent();
                        break;
                    case "hintkeys2":
                        context.mainFrame.textFieldOperationHintKeys2.setText(path);
                        context.mainFrame.textFieldOperationHintKeys2.postActionEvent();
                        break;
                    case "hintkeys3":
                        context.mainFrame.textFieldOperationHintKeys3.setText(path);
                        context.mainFrame.textFieldOperationHintKeys3.postActionEvent();
                        break;
                    case "hints":
                        context.mainFrame.textFieldOperationHints.setText(path);
                        context.mainFrame.textFieldOperationHints.postActionEvent();
                        break;
                    case "hints2":
                        context.mainFrame.textFieldOperationHints2.setText(path);
                        context.mainFrame.textFieldOperationHints2.postActionEvent();
                        break;
                    case "hints3":
                        context.mainFrame.textFieldOperationHints3.setText(path);
                        context.mainFrame.textFieldOperationHints3.postActionEvent();
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
     * context.operations.genericAction
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
     * <b/>
     * context.operations.genericTextFieldFocusLost
     *
     * @param e ActionEvent
     */
    public void genericTextFieldFocusLost(FocusEvent e)
    {
        updateOnChange(e.getSource());
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    public ArrayList<OperationsTool> getDeletedTools()
    {
        return deletedTools;
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

    private void initNewCard()
    {
        currentTool.setDataHasChanged();

    }

    public void initialize()
    {
        this.configItems = context.mainFrame.operationConfigItems;

        // scale the help icon
        Icon icon = context.mainFrame.labelOperationHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        context.mainFrame.labelOperationHelp.setIcon(replacement);

        // dividers
        if (context.preferences.getOperationDividerLocation() > 0)
        {
            context.mainFrame.splitPaneOperation.setDividerLocation(context.preferences.getOperationDividerLocation());
        }
        if (context.preferences.getOperationDividerConfigLocation() > 0)
        {
            context.mainFrame.splitPaneOperationContent.setDividerLocation(context.preferences.getOperationDividerConfigLocation());
        }

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
                    if (index != currentConfigIndex && currentConfigIndex >= 0)
                    {
                        currentConfigIndex = index;
                        loadOptions(currentConfigIndex);
                    }
                }
            }
        });
        configItems.setTableHeader(null);

        // make Mode objects
        //  * publisher has base [objects]
        //  * listener has [objects]2
        //  * hint server has [objects]3
        modes = new Mode[9];
        modes[0] = new Mode(context.cfg.gs("Operations.mode.localPublish"), OperationsTool.Cards.Publisher, Configuration.Operations.NotRemote);
        modes[1] = new Mode(context.cfg.gs("Operations.mode.remotePublish"), OperationsTool.Cards.Publisher, Configuration.Operations.PublishRemote);
        modes[2] = new Mode(context.cfg.gs("Operations.mode.subscriberListener"), OperationsTool.Cards.Listener, Configuration.Operations.SubscriberListener);
        modes[3] = new Mode(context.cfg.gs("Operations.mode.hintServer"), OperationsTool.Cards.HintServer, Configuration.Operations.StatusServer);
        modes[4] = new Mode(context.cfg.gs("Operations.mode.publisherTerminal"), OperationsTool.Cards.Terminal, Configuration.Operations.PublisherManual);
        modes[5] = new Mode(context.cfg.gs("Operations.mode.publisherListener"), OperationsTool.Cards.Listener, Configuration.Operations.PublisherListener);
        modes[6] = new Mode(context.cfg.gs("Operations.mode.subscriberTerminal"), OperationsTool.Cards.Terminal, Configuration.Operations.SubscriberTerminal);
        modes[7] = new Mode(context.cfg.gs("Operations.mode.hintForceQuit"), OperationsTool.Cards.Quitter, Configuration.Operations.StatusServerForceQuit);
        modes[8] = new Mode(context.cfg.gs("Operations.mode.subscriberForceQuit"), OperationsTool.Cards.Quitter, Configuration.Operations.SubscriberListenerForceQuit);

        // make New combobox
        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[] { }));
        comboBoxMode.removeAllItems();
        for (Mode m : modes)
        {
            comboBoxMode.addItem(m);
        }

        context.mainFrame.buttonNewOperation.addActionListener(e -> actionNewClicked(e));
        context.mainFrame.buttonCopyOperation.addActionListener(e -> actionCopyClicked(e));
        context.mainFrame.buttonDeleteOperation.addActionListener(e -> actionDeleteClicked(e));
        context.mainFrame.buttonRunOperation.addActionListener(e -> actionRunClicked(e));
        context.mainFrame.buttonGenerateOperation.addActionListener(e -> actionGenerateClicked(e));
        context.mainFrame.labelOperationHelp.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                actionHelpClicked(e);
            }
        });
        context.mainFrame.buttonOperationSave.addActionListener(e -> actionSaveClicked(e));
        context.mainFrame.buttonOperationCancel.addActionListener(e -> actionCancelClicked(e));

        librariesUI = new LibrariesUI(context);

        initializeComboBoxes();
        loadConfigurations();
        deletedTools = new ArrayList<OperationsTool>();
    }

    private void initializeComboBoxes()
    {
        context.mainFrame.comboBoxOperationWhatsNew.removeAllItems();
        context.mainFrame.comboBoxOperationWhatsNew.addItem(context.cfg.gs("Operations.comboBoxOperationWhatsNew.0.whatsNew"));
        context.mainFrame.comboBoxOperationWhatsNew.addItem(context.cfg.gs("Operations.comboBoxOperationWhatsNew.1.whatsNewAll"));

        context.mainFrame.comboBoxOperationHintKeys.removeAllItems();
        context.mainFrame.comboBoxOperationHintKeys.addItem(context.cfg.gs("Operations.comboBoxOperationHintKeys.0.keys"));
        context.mainFrame.comboBoxOperationHintKeys.addItem(context.cfg.gs("Operations.comboBoxOperationHintKeys.1.keysOnly"));

        context.mainFrame.comboBoxOperationHintsAndServer.removeAllItems();
        context.mainFrame.comboBoxOperationHintsAndServer.addItem(context.cfg.gs("Operations.comboBoxOperationHintsAndServer.0.hints"));
        context.mainFrame.comboBoxOperationHintsAndServer.addItem(context.cfg.gs("Operations.comboBoxOperationHintsAndServer.1.hintServer"));
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

    private void libraryPicker(JButton button)
    {
        try
        {
            JCheckBox checkBox = new JCheckBox(context.cfg.gs("Operations.include.selections"));
            checkBox.setToolTipText(context.cfg.gs("Operations.uncheck.to.exclude.selections"));
            checkBox.setSelected(true);
            if (button.getName().toLowerCase().equals("addexc"))
                checkBox.setSelected(false);

            JComboBox combo = new JComboBox();
            combo.addItem(context.cfg.gs("Operations.publisher.libraries"));
            combo.addItem(context.cfg.gs("Operations.subscriber.libraries"));
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
            Object[] params1 = {context.cfg.gs("Operations.select.included.excluded.libraries"), combo, checkBox, pane};
            Object[] params2 = {context.cfg.gs("Operations.select.excluded.libraries"), combo, pane};

            int opt = 0;
            if (button.getName().toLowerCase().equals("addincexc"))
                opt = JOptionPane.showConfirmDialog(context.mainFrame, params1, displayName, JOptionPane.OK_CANCEL_OPTION);
            else if (button.getName().toLowerCase().equals("addexc"))
                opt = JOptionPane.showConfirmDialog(context.mainFrame, params2, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                List<String> selections = libJList.getSelectedValuesList();
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
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    name + context.cfg.gs("Operations.is.a.duplicate"),
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
            JOptionPane.showMessageDialog(context.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean libraryRemover(int cardVar, boolean includes, int[] indices)
    {
        boolean changed = false;
        List<String> selections = (cardVar == 1) ? context.mainFrame.listOperationIncludeExclude.getSelectedValuesList() :
                context.mainFrame.listOperationExclude.getSelectedValuesList();
        for (int i = indices.length - 1; i >= 0; --i)
        {
            String dn = selections.get(i);
            if (dn.startsWith(context.cfg.gs("Operations.include")) && !includes)
                continue;
            if (dn.startsWith(context.cfg.gs("Operations.exclude")) && includes)
                continue;

            String cn = "";
            if (includes)
                cn = dn.substring(context.cfg.gs("Operations.include").length());
            else
                cn = dn.substring(context.cfg.gs("Operations.exclude").length());

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
        try
        {
            Tools tools = new Tools();
            ArrayList<AbstractTool> toolList = tools.loadAllTools(context, OperationsTool.INTERNAL_NAME);
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
                JOptionPane.showMessageDialog(context.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (configModel.getRowCount() == 0)
        {
            context.mainFrame.buttonCopyOperation.setEnabled(false);
            context.mainFrame.buttonDeleteOperation.setEnabled(false);
            context.mainFrame.buttonRunOperation.setEnabled(false);
            context.mainFrame.buttonGenerateOperation.setEnabled(false);
            context.mainFrame.buttonOperationSave.setEnabled(false);
            context.mainFrame.buttonOperationCancel.setEnabled(false);
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
                exc.add(context.cfg.gs("Operations.exclude") + currentTool.getOptExclude()[i]);
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
        context.mainFrame.listOperationExclude.setModel(model);
        context.mainFrame.scrollPaneOperationExclude.setViewportView(context.mainFrame.listOperationExclude);
        context.mainFrame.listOperationExclude.setSelectionInterval(0, 0);
    }

    private void loadIncludeExcludeList()
    {
        ArrayList<String> incExc = new ArrayList<>();
        if (currentTool.getOptExclude() != null)
        {
            for (int i = 0; i < currentTool.getOptExclude().length; ++i)
            {
                incExc.add(context.cfg.gs("Operations.exclude") + currentTool.getOptExclude()[i]);
            }
        }
        if (currentTool.getOptLibrary() != null)
        {
            for (int i = 0; i < currentTool.getOptLibrary().length; ++i)
            {
                incExc.add(context.cfg.gs("Operations.include") + currentTool.getOptLibrary()[i]);
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
            context.mainFrame.listOperationIncludeExclude.removeAll();
            model.removeAllElements();
            model.clear();
        }
        context.mainFrame.listOperationIncludeExclude.setModel(model);
        context.mainFrame.scrollPaneOperationIncludeExclude.setViewportView(context.mainFrame.listOperationIncludeExclude);
        context.mainFrame.listOperationIncludeExclude.setSelectionInterval(0, 0);
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
            //OperationsTool.Cards cardName = modes[getModeOperationIndex()].card;
            ((CardLayout) context.mainFrame.panelOperationCards.getLayout()).show(context.mainFrame.panelOperationCards, currentTool.getCard().name().toLowerCase());
            context.mainFrame.labelOperationMode.setText(modes[getModeOperationIndex()].name);

            // populate card
            switch (currentTool.getOperation())
            {
                case NotRemote:
                    loadOptionsPublisher();
                    break;
                case PublishRemote:
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
                case StatusServerForceQuit:
                    break;
                case SubscriberListenerForceQuit:
                    break;
            }
            updateTextFieldToolTips(cardVar);
            loading = false;
        }
    }

    private void loadOptionsHintServer()
    {
        MainFrame mf = context.mainFrame;

        // ### LEFT SIDE
        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            mf.textFieldOperationHintKeys3.setText(currentTool.getOptKeys());
        }
        else
        {
            mf.textFieldOperationHintKeys3.setText("");
        }

        if (currentTool.getOptHintServer().length() > 0)
        {
            mf.textFieldOperationHints3.setText(currentTool.getOptHintServer());
        }
        else
        {
            mf.textFieldOperationHints3.setText("");
        }

        mf.checkBoxOperationKeepGoing3.setSelected(currentTool.isOptListenerKeepGoing());

        mf.textFieldOperationBlacklist3.setText(currentTool.getOptBlacklist());
        mf.textFieldOperationIpWhitelist3.setText(currentTool.getOptIpWhitelist());

        // ### RIGHT SIDE
        // none
    }

    private void loadOptionsListener()
    {
        MainFrame mf = context.mainFrame;

        // ### LEFT SIDE
        // --- General
        if (currentTool.getOptTargets().length() > 0)
        {
            mf.textFieldOperationTargets2.setText(currentTool.getOptTargets());
        }
        else
        {
            mf.textFieldOperationTargets2.setText("");
        }
        if (currentTool.getOptAuthorize() != null && currentTool.getOptAuthorize().length > 0)
            mf.passwordFieldOperationsAuthorize.setText(new String(currentTool.getOptAuthorize()));
        else
            mf.passwordFieldOperationsAuthorize.setText("");
        mf.passwordFieldOperationsAuthorize.setEchoChar((char)0); // do not hide password
        mf.textFieldOperationAuthKeys.setText(currentTool.getOptAuthKeys());
        mf.textFieldOperationBlacklist.setText(currentTool.getOptBlacklist());
        mf.textFieldOperationIpWhitelist.setText(currentTool.getOptIpWhitelist());

        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            mf.textFieldOperationHintKeys2.setText(currentTool.getOptKeys());
        }
        else
        {
            mf.textFieldOperationHintKeys2.setText("");
        }
        if (currentTool.getOptHints().length() > 0)
        {
            mf.comboBoxOperationHintsAndServer2.setSelectedIndex(0);
            mf.textFieldOperationHints2.setText(currentTool.getOptHints());
        }
        else if (currentTool.getOptHintServer().length() > 0)
        {
            mf.comboBoxOperationHintsAndServer2.setSelectedIndex(1);
            mf.textFieldOperationHints2.setText(currentTool.getOptHintServer());
        }
        else
        {
            mf.comboBoxOperationHintsAndServer2.setSelectedIndex(0);
            mf.textFieldOperationHints2.setText("");
        }
        mf.checkBoxOperationKeepGoing2.setSelected(currentTool.isOptListenerKeepGoing());

        // ### RIGHT SIDE
        // --- Include/Exclude
        loadExcludeList();

        // --- Runtime Options
        mf.checkBoxOperationOverwrite2.setSelected(currentTool.isOptOverwrite());
        mf.checkBoxOperationPreserveDates2.setSelected(currentTool.isOptPreserveDates());
        mf.checkBoxOperationDecimalScale2.setSelected(currentTool.isOptDecimalScale());
    }

    private void loadOptionsPublisher()
    {
        MainFrame mf = context.mainFrame;

        // ### LEFT SIDE
        // --- General
        mf.checkBoxOperationNavigator.setSelected(currentTool.isOptNavigator());
        if (currentTool.getOptTargets().length() > 0)
        {
            mf.textFieldOperationTargets.setText(currentTool.getOptTargets());
        }
        else
        {
            mf.textFieldOperationTargets.setText("");
        }
        mf.textFieldOperationMismatches.setText(currentTool.getOptMismatches());
        if (currentTool.getOptWhatsNew().length() > 0)
        {
            mf.comboBoxOperationWhatsNew.setSelectedIndex(0);
            mf.textFieldOperationWhatsNew.setText(currentTool.getOptWhatsNew());
        }
        else if (currentTool.getOptWhatsNewAll().length() > 0)
        {
            mf.comboBoxOperationWhatsNew.setSelectedIndex(1);
            mf.textFieldOperationWhatsNew.setText(currentTool.getOptWhatsNewAll());
        }
        else
        {
            mf.comboBoxOperationWhatsNew.setSelectedIndex(0);
            mf.textFieldOperationWhatsNew.setText("");
        }
        mf.textFieldOperationExportText.setText(currentTool.getOptExportText());
        mf.textFieldOperationExportItems.setText(currentTool.getOptExportItems());

        // --- Hints
        if (currentTool.getOptKeys().length() > 0)
        {
            mf.comboBoxOperationHintKeys.setSelectedIndex(0);
            mf.textFieldOperationHintKeys.setText(currentTool.getOptKeys());
        }
        else if (currentTool.getOptKeysOnly().length() > 0)
        {
            mf.comboBoxOperationHintKeys.setSelectedIndex(1);
            mf.textFieldOperationHintKeys.setText(currentTool.getOptKeysOnly());
        }
        else
        {
            mf.comboBoxOperationHintKeys.setSelectedIndex(0);
            mf.textFieldOperationHintKeys.setText("");
        }
        if (currentTool.getOptHints().length() > 0)
        {
            mf.comboBoxOperationHintsAndServer.setSelectedIndex(0);
            mf.textFieldOperationHints.setText(currentTool.getOptHints());
        }
        else if (currentTool.getOptHintServer().length() > 0)
        {
            mf.comboBoxOperationHintsAndServer.setSelectedIndex(1);
            mf.textFieldOperationHints.setText(currentTool.getOptHintServer());
        }
        else
        {
            mf.comboBoxOperationHintsAndServer.setSelectedIndex(0);
            mf.textFieldOperationHints.setText("");
        }
        mf.checkBoxOperationQuitStatus.setSelected(currentTool.isOptQuitStatus());
        mf.checkBoxOperationKeepGoing.setSelected(currentTool.isOptListenerKeepGoing());

        // ### RIGHT SIDE
        // --- Include/Exclude
        loadIncludeExcludeList();

        // --- Runtime Options
        mf.checkBoxOperationOverwrite.setSelected(currentTool.isOptOverwrite());
        mf.checkBoxOperationPreserveDates.setSelected(currentTool.isOptPreserveDates());
        mf.checkBoxOperationDecimalScale.setSelected(currentTool.isOptDecimalScale());
        mf.checkBoxOperationDryRun.setSelected(currentTool.isOptDryRun());
        mf.checkBoxOperationNoBackFill.setSelected(currentTool.isOptNoBackFill());
        mf.checkBoxOperationValidate.setSelected(currentTool.isOptValidate());

        // --- Reporting
        mf.checkBoxOperationDuplicates.setSelected(currentTool.isOptDuplicates());
        mf.checkBoxOperationCrossCheck.setSelected(currentTool.isOptCrossCheck());
        mf.checkBoxOperationEmptyDirectories.setSelected(currentTool.isOptEmptyDirectories());
        mf.checkBoxOperationIgnored.setSelected(currentTool.isOptIgnored());
    }

    private void process()
    {
        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        context.navigator.disableComponent(true, context.mainFrame.panelOperationTop);
        context.mainFrame.buttonOperationCancel.setEnabled(true);
        context.mainFrame.buttonOperationCancel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        context.mainFrame.labelOperationHelp.setEnabled(true);
        context.mainFrame.labelOperationHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Repository pubRepo = context.publisherRepo;
        Repository subRepo = context.subscriberRepo;

        try
        {
            worker = workerOperation.processToolThread(context, pubRepo.getJsonFilename(), subRepo.getJsonFilename(), false);
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
                                processTerminated(currentTool);
                        }
                    }
                });

                logger.info(context.cfg.gs("Operations.running.operation") + currentTool.getConfigName());
                context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Operations.running.operation") + currentTool.getConfigName());
                worker.execute();
            }
            else
                processTerminated(currentTool);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(context.mainFrame, Utils.getStackTrace(e), displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processTerminated(OperationsTool operation)
    {
/*
        if (context.progress != null)
            context.progress.done();
*/

        context.navigator.disableGui(false);
        context.navigator.disableComponent(false, context.mainFrame.panelOperationTop);
        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;

        if (operation.isRequestStop())
        {
            logger.info(operation.getConfigName() + context.cfg.gs("Z.cancelled"));
            context.mainFrame.labelStatusMiddle.setText(operation.getConfigName() + context.cfg.gs("Z.cancelled"));
        }
        else
        {
            logger.info(context.cfg.gs("Operations.operation") + operation.getConfigName() + context.cfg.gs("Z.completed"));
            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Operations.operation") + operation.getConfigName() + context.cfg.gs("Z.completed"));
        }
    }

    private void saveConfigurations()
    {
        OperationsTool tool = null;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (OperationsTool) configModel.getValueAt(i, 0);
                if (tool.isDataChanged())
                    tool.write();
                tool.setDataHasChanged(false);
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = deletedTools.get(i);
                File file = new File(tool.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
                tool.setDataHasChanged(false);
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(context.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    public void savePreferences()
    {
        context.preferences.setOperationDividerLocation(context.mainFrame.splitPaneOperation.getDividerLocation());
        context.preferences.setOperationDividerConfigLocation(context.mainFrame.splitPaneOperationContent.getDividerLocation());
    }

    private void updateOnChange(Object source)
    {
        int cardVar = 1;  // 1 publisher; 2 listener, 3 hint server
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
                    case "hints2":
                        cardVar = 2;
                    case "hints":
                        selection = (cardVar == 2) ? context.mainFrame.comboBoxOperationHintsAndServer2.getSelectedIndex() :
                                context.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex();
                        if (selection == 0)
                        {
                            current = currentTool.getOptHints();
                            currentTool.setOptHints(tf.getText());
                        }
                        else if (selection == 1)
                        {
                            current = currentTool.getOptHintServer();
                            currentTool.setOptHintServer(tf.getText());
                        }
                        break;
                    case "hints3":
                        current = currentTool.getOptHintServer();
                        currentTool.setOptHintServer(tf.getText());
                        break;
                    case "hintkeys2":
                        cardVar = 2;
                    case "hintkeys":
                        selection = (cardVar == 2) ? 0 : context.mainFrame.comboBoxOperationHintKeys.getSelectedIndex();
                        if (selection == 0)
                        {
                            current = currentTool.getOptKeys();
                            currentTool.setOptKeys(tf.getText());
                        }
                        else if (selection == 1)
                        {
                            current = currentTool.getOptKeysOnly();
                            currentTool.setOptKeysOnly(tf.getText());
                        }
                        break;
                    case "hintkeys3":
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
                        if (context.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptWhatsNew();
                            currentTool.setOptWhatsNew(tf.getText());
                        }
                        else if (context.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 1)
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
                    case "dryrun":
                        state = currentTool.isOptDryRun();
                        currentTool.setOptDryRun(cb.isSelected());
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
                int current = -1;
                int index = combo.getSelectedIndex();
                String value = "";
                switch (name.toLowerCase())
                {
                    case "hints2":
                        cardVar = 2;
                    case "hints":
                        if (currentTool.getOptHints().length() > 0)
                            current = 0;
                        else if (currentTool.getOptHintServer().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            value = (cardVar == 2) ? context.mainFrame.textFieldOperationHints2.getText() :
                                    context.mainFrame.textFieldOperationHints.getText();
                            currentTool.setOptHints(value);
                            currentTool.setOptHintServer("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? context.mainFrame.textFieldOperationHints2.getText() :
                                    context.mainFrame.textFieldOperationHints.getText();
                            currentTool.setOptHintServer(value);
                            currentTool.setOptHints("");
                        }
                        break;
                    case "keys2":
                        cardVar = 2;
                    case "keys":
                        if (currentTool.getOptKeys().length() > 0)
                            current = 0;
                        else if (currentTool.getOptKeysOnly().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            value = (cardVar == 2) ? context.mainFrame.textFieldOperationHintKeys2.getText() :
                                    context.mainFrame.textFieldOperationHintKeys.getText();
                            currentTool.setOptKeys(value);
                            currentTool.setOptKeysOnly("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? context.mainFrame.textFieldOperationHintKeys2.getText() :
                                    context.mainFrame.textFieldOperationHintKeys.getText();
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
                            currentTool.setOptWhatsNew(context.mainFrame.textFieldOperationWhatsNew.getText());
                            currentTool.setOptWhatsNewAll("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptWhatsNewAll(context.mainFrame.textFieldOperationWhatsNew.getText());
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
    }

    private void updateTextFieldToolTips(int carVar)
    {
        String current;
        int selected = 0;
        if (carVar == 1)
        {
            current = currentTool.getOptExportItems();
            context.mainFrame.textFieldOperationExportItems.setToolTipText(current);

            current = currentTool.getOptExportText();
            context.mainFrame.textFieldOperationExportText.setToolTipText(current);

            current = currentTool.getOptMismatches();
            context.mainFrame.textFieldOperationMismatches.setToolTipText(current);

            current = "";
            if (context.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                current = currentTool.getOptWhatsNew();
            else if (context.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 1)
                current = currentTool.getOptWhatsNewAll();
            context.mainFrame.textFieldOperationWhatsNew.setToolTipText(current);
        }

        if (carVar == 2)
        {
            current = currentTool.getOptAuthKeys();
            context.mainFrame.textFieldOperationAuthKeys.setToolTipText(current);

            current = currentTool.getOptBlacklist();
            context.mainFrame.textFieldOperationBlacklist.setToolTipText(current);

            current = currentTool.getOptIpWhitelist();
            context.mainFrame.textFieldOperationIpWhitelist.setToolTipText(current);
        }

        current = currentTool.getOptTargets();
        if (carVar == 1)
            context.mainFrame.textFieldOperationTargets.setToolTipText(current);
        else
            context.mainFrame.textFieldOperationTargets2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = context.mainFrame.comboBoxOperationHintKeys.getSelectedIndex();
        else
            selected = 0;
        if (selected == 0)
            current = currentTool.getOptKeys();
        else if (selected == 1)
            current = currentTool.getOptKeysOnly();
        if (carVar == 1)
            context.mainFrame.textFieldOperationHintKeys.setToolTipText(current);
        else
            context.mainFrame.textFieldOperationHintKeys2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = context.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex();
        else
            selected = context.mainFrame.comboBoxOperationHintsAndServer2.getSelectedIndex();
        if (selected == 0)
            current = currentTool.getOptHints();
        else if (selected == 1)
            current = currentTool.getOptHintServer();
        if (carVar == 1)
            context.mainFrame.textFieldOperationHints.setToolTipText(current);
        else
            context.mainFrame.textFieldOperationHints2.setToolTipText(current);
    }

    private void updateState()
    {
/*
        if (currentCard == panelCaseChangeCard)
        {
        }
        else if (currentCard == panelInsertCard)
        {
            if (currentOperation.isOption1())
            {
                checkBoxInsertAtEnd.setEnabled(false);
            }
            else
            {
                checkBoxInsertAtEnd.setEnabled(true);
            }
            if (currentOperation.isOption2())
            {
                textFieldInsertPosition.setEnabled(false);
                checkBoxInsertFromEnd.setEnabled(false);
                checkBoxInsertOverwrite.setEnabled(false);
            }
            else
            {
                textFieldInsertPosition.setEnabled(true);
                checkBoxInsertFromEnd.setEnabled(true);
                checkBoxInsertOverwrite.setEnabled(true);
            }
            setNumberFilter(textFieldInsertPosition);
        }
        else if (currentCard == panelNumberingCard)
        {
            if (currentOperation.isOption1())
            {
                checkBoxNumberingAtEnd.setEnabled(false);
            }
            else
            {
                checkBoxNumberingAtEnd.setEnabled(true);
            }
            if (currentOperation.isOption2())
            {
                textFieldNumberingPosition.setEnabled(false);
                checkBoxNumberingFromEnd.setEnabled(false);
                checkBoxNumberingOverwrite.setEnabled(false);
            }
            else
            {
                textFieldNumberingPosition.setEnabled(true);
                checkBoxNumberingFromEnd.setEnabled(true);
                checkBoxNumberingOverwrite.setEnabled(true);
            }
            setNumberFilter(textFieldNumberingStart);
            setNumberFilter(textFieldNumberingZeros);
            setNumberFilter(textFieldNumberingPosition);
        }
        else if (currentCard == panelRemoveCard)
        {
            if (currentOperation.isOption1())
                textFieldFrom.setEnabled(false);
            else
                textFieldFrom.setEnabled(true);
            setNumberFilter(textFieldFrom);
            setNumberFilter(textFieldLength);
        }
        else if (currentCard == panelReplaceCard)
        {
            if (currentOperation.isOption1())
            {
                 checkBoxRegularExpr.setEnabled(true);
                checkBoxCase.setEnabled(false);
            }
            else if (currentOperation.isOption2())
            {
                checkBoxRegularExpr.setEnabled(false);
                checkBoxCase.setEnabled(true);
            }
            else
            {
                checkBoxRegularExpr.setEnabled(true);
                checkBoxCase.setEnabled(true);
            }
        }
*/
    }

    // ================================================================================================================

    private class Mode
    {
        String name;
        OperationsTool.Cards card;
        Configuration.Operations operation;

        public Mode(String name, OperationsTool.Cards card, Configuration.Operations operation)
        {
            this.name = name;
            this.card = card;
            this.operation = operation;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

}
