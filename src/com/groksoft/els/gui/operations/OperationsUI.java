package com.groksoft.els.gui.operations;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.libraries.LibrariesUI;
import com.groksoft.els.jobs.Jobs;
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
    private ConfigModel configModel;
    private int currentConfigIndex = -1;
    private OperationsTool currentTool;
    private ArrayList<OperationsTool> deletedTools;
    private String displayName;
    private boolean fileAny = false;
    private boolean fileKeys = false;
    private boolean fileMustExist = false;
    private GuiContext guiContext;
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

    public OperationsUI(GuiContext guiContext)
    {
        this.guiContext = guiContext;
        this.displayName = guiContext.cfg.gs("Operations.displayName");
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && worker != null)
        {
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("Operations.stop.running.operation"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerOperation.requestStop();
                logger.info(java.text.MessageFormat.format(guiContext.cfg.gs("Operations.config.cancelled"), workerOperation.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.cancel.all.changes"),
                        guiContext.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
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
            String rename = original.getConfigName() + guiContext.cfg.gs("Z.copy");
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
                JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.please.rename.the.existing") +
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

            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + tool.getConfigName(),
                    guiContext.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
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
                    ((CardLayout) guiContext.mainFrame.panelOperationCards.getLayout()).show(guiContext.mainFrame.panelOperationCards, "gettingStarted");
                    guiContext.mainFrame.labelOperationMode.setText("");
                    guiContext.mainFrame.buttonCopyOperation.setEnabled(false);
                    guiContext.mainFrame.buttonDeleteOperation.setEnabled(false);
                    guiContext.mainFrame.buttonRunOperation.setEnabled(false);
                    guiContext.mainFrame.buttonGenerateOperation.setEnabled(false);
                    guiContext.mainFrame.buttonOperationSave.setEnabled(false);
                    guiContext.mainFrame.buttonOperationCancel.setEnabled(false);
                    currentConfigIndex = 0;
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionGenerateClicked(ActionEvent evt)
    {
        try
        {
            // TODO change when JRE is embedded in ELS distro
            String jar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            String generated = "java -jar " + jar + " " + currentTool.generateCommandLine(guiContext.context.publisherRepo, guiContext.context.subscriberRepo);

            Object[] opts = {guiContext.cfg.gs("Z.ok")};
            JOptionPane.showInputDialog(guiContext.mainFrame,
                    "<html><body>" + guiContext.cfg.gs("Z.generated") + currentTool.getConfigName() +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</body></html>",
                    displayName, JOptionPane.PLAIN_MESSAGE, null, null, generated);
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(guiContext.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(guiContext.mainFrame, guiContext.mainFrame, guiContext,
                    guiContext.cfg.gs("Operations.help"), "operations_" + guiContext.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            helpDialog.setVisible(true);
            // offset the help dialog from the parent dialog
            Point loc = guiContext.mainFrame.getLocation();
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
        if (configModel.find(guiContext.cfg.gs("Z.untitled"), null) == null)
        {
            String message = guiContext.cfg.gs("Operations.mode.select.type");
            String line = guiContext.cfg.gs("Use Local/Remote publish for Navigator. ");
            Object[] params = {message, line, comboBoxMode};
            comboBoxMode.setSelectedIndex(0);

            // get ELS operationsUI/mode
            int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                currentTool = new OperationsTool(guiContext, guiContext.cfg, guiContext.context);
                Mode mode = modes[comboBoxMode.getSelectedIndex()];
                currentTool.setConfigName(guiContext.cfg.gs("Z.untitled"));
                currentTool.setOperation(mode.operation);
                currentTool.setCard(mode.card);
                currentTool.setDataHasChanged();
                initNewCard();

                guiContext.mainFrame.buttonCopyOperation.setEnabled(true);
                guiContext.mainFrame.buttonDeleteOperation.setEnabled(true);
                guiContext.mainFrame.buttonRunOperation.setEnabled(true);
                guiContext.mainFrame.buttonGenerateOperation.setEnabled(true);
                guiContext.mainFrame.buttonOperationSave.setEnabled(true);
                guiContext.mainFrame.buttonOperationCancel.setEnabled(true);

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
            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.please.rename.the.existing") +
                    guiContext.cfg.gs("Z.untitled"), displayName, JOptionPane.WARNING_MESSAGE);
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
                if (cardVar == 1 && guiContext.mainFrame.listOperationIncludeExclude.getModel().getSize() > 0)
                    indices = guiContext.mainFrame.listOperationIncludeExclude.getSelectedIndices();
                else if (cardVar == 2 && guiContext.mainFrame.listOperationExclude.getModel().getSize() > 0)
                    indices = guiContext.mainFrame.listOperationExclude.getSelectedIndices();
                if (indices.length > 0)
                {
                    if (cardVar == 1)
                        guiContext.mainFrame.listOperationIncludeExclude.requestFocus();
                    else if (cardVar == 2)
                        guiContext.mainFrame.listOperationExclude.requestFocus();
                    int count = indices.length;

                    // confirm deletions
                    int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                            MessageFormat.format(guiContext.cfg.gs("Operations.are.you.sure.you.want.delete.entries"), count), displayName,
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        List<String> selections = guiContext.mainFrame.listOperationIncludeExclude.getSelectedValuesList();
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
            String message = java.text.MessageFormat.format(guiContext.cfg.gs("Operations.run.as.defined"), currentTool.getConfigName());
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, message, guiContext.cfg.gs("Navigator.splitPane.Operations.tab.title"), JOptionPane.YES_NO_OPTION);
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
                        desc = guiContext.cfg.gs("Operations.els.authkeys.file");
                        fileAny = false;
                        fileMustExist = true;
                        fileKeys = true;
                        break;
                    case "blacklist":
                        desc = guiContext.cfg.gs("Operations.els.blacklist.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
                    case "ipwhitelist":
                        desc = guiContext.cfg.gs("Operations.els.ipwhitelist.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
                    case "targets":
                    case "targets2":
                        desc = guiContext.cfg.gs("Operations.els.targets.file.json");
                        fileAny = false;
                        fileMustExist = true;
                        break;
                    case "mismatches":
                        desc = guiContext.cfg.gs("Operations.els.mismatches.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "whatsnew":
                        desc = guiContext.cfg.gs("Operations.els.what.s.new.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "exporttext":
                        desc = guiContext.cfg.gs("Operations.els.export.text.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "exportitems":
                        desc = guiContext.cfg.gs("Operations.els.export.items.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                    case "hintkeys":
                    case "hintkeys2":
                        desc = guiContext.cfg.gs("Operations.els.hint.keys.file");
                        fileAny = true;
                        fileMustExist = true;
                        fileKeys = true;
                        break;
                    case "hints":
                    case "hints2":
                        desc = guiContext.cfg.gs("Operations.els.hints.server.file");
                        fileAny = true;
                        fileMustExist = true;
                        break;
                    case "log":
                    case "log2":
                        desc = guiContext.cfg.gs("Operations.els.log.file");
                        fileAny = true;
                        fileMustExist = false;
                        break;
                }
                return desc;
            }
        });

        String fileName = "";
        switch(button.getName().toLowerCase())
        {
            case "authkeys":
                fileName = guiContext.mainFrame.textFieldOperationAuthKeys.getText();
                break;
            case "blacklist":
                fileName = guiContext.mainFrame.textFieldOperationBlacklist.getText();
                break;
            case "ipwhitelist":
                fileName = guiContext.mainFrame.textFieldOperationIpWhitelist.getText();
                break;
            case "targets":
                fileName = guiContext.mainFrame.textFieldOperationTargets.getText();
                break;
            case "targets2":
                fileName = guiContext.mainFrame.textFieldOperationTargets2.getText();
                break;
            case "mismatches":
                fileName = guiContext.mainFrame.textFieldOperationMismatches.getText();
                break;
            case "whatsnew":
                fileName = guiContext.mainFrame.textFieldOperationWhatsNew.getText();
                break;
            case "exporttext":
                fileName = guiContext.mainFrame.textFieldOperationExportText.getText();
                break;
            case "exportitems":
                fileName = guiContext.mainFrame.textFieldOperationExportItems.getText();
                break;
            case "hintkeys":
                fileName = guiContext.mainFrame.textFieldOperationHintKeys.getText();
                break;
            case "hintkeys2":
                fileName = guiContext.mainFrame.textFieldOperationHintKeys2.getText();
                break;
            case "hints":
                fileName = guiContext.mainFrame.textFieldOperationHints.getText();
                break;
            case "hints2":
                fileName = guiContext.mainFrame.textFieldOperationHints2.getText();
                break;
            case "log":
                fileName = guiContext.mainFrame.textFieldOperationLog.getText();
                break;
            case "log2":
                fileName = guiContext.mainFrame.textFieldOperationLog2.getText();
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
            fc.setCurrentDirectory(new File(Utils.getElsHome()));
        }

        fc.setDialogType(fileMustExist ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);

        while (true)
        {
            int selection = fc.showOpenDialog(guiContext.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {

                lastFile = fc.getCurrentDirectory();
                file = fc.getSelectedFile();

                // sanity checks
                if (fileMustExist && !file.exists())
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            displayName, JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.open.error.select.a.file.only"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // make path relative if possible
                String path = "";
                if (file.getPath().startsWith(Utils.getElsHome()))
                    path = file.getPath().substring(Utils.getElsHome().length() + 1);
                else
                    path = file.getPath();

                // save value & fire updateOnChange()
                switch (button.getName().toLowerCase())
                {
                    // guiContext.mainFrame.textFieldOperation
                    case "authkeys":
                        guiContext.mainFrame.textFieldOperationAuthKeys.setText(path);
                        guiContext.mainFrame.textFieldOperationAuthKeys.postActionEvent();
                        break;
                    case "blacklist":
                        guiContext.mainFrame.textFieldOperationBlacklist.setText(path);
                        guiContext.mainFrame.textFieldOperationBlacklist.postActionEvent();
                        break;
                    case "ipwhitelist":
                        guiContext.mainFrame.textFieldOperationIpWhitelist.setText(path);
                        guiContext.mainFrame.textFieldOperationIpWhitelist.postActionEvent();
                        break;
                    case "targets":
                        guiContext.mainFrame.textFieldOperationTargets.setText(path);
                        guiContext.mainFrame.textFieldOperationTargets.postActionEvent();
                        break;
                    case "targets2":
                        guiContext.mainFrame.textFieldOperationTargets2.setText(path);
                        guiContext.mainFrame.textFieldOperationTargets2.postActionEvent();
                        break;
                    case "mismatches":
                        guiContext.mainFrame.textFieldOperationMismatches.setText(path);
                        guiContext.mainFrame.textFieldOperationMismatches.postActionEvent();
                        break;
                    case "whatsnew":
                        guiContext.mainFrame.textFieldOperationWhatsNew.setText(path);
                        guiContext.mainFrame.textFieldOperationWhatsNew.postActionEvent();
                        break;
                    case "exporttext":
                        guiContext.mainFrame.textFieldOperationExportText.setText(path);
                        guiContext.mainFrame.textFieldOperationExportText.postActionEvent();
                        break;
                    case "exportitems":
                        guiContext.mainFrame.textFieldOperationExportItems.setText(path);
                        guiContext.mainFrame.textFieldOperationExportItems.postActionEvent();
                        break;
                    case "hintkeys":
                        guiContext.mainFrame.textFieldOperationHintKeys.setText(path);
                        guiContext.mainFrame.textFieldOperationHintKeys.postActionEvent();
                        break;
                    case "hintkeys2":
                        guiContext.mainFrame.textFieldOperationHintKeys2.setText(path);
                        guiContext.mainFrame.textFieldOperationHintKeys2.postActionEvent();
                        break;
                    case "hints":
                        guiContext.mainFrame.textFieldOperationHints.setText(path);
                        guiContext.mainFrame.textFieldOperationHints.postActionEvent();
                        break;
                    case "hints2":
                        guiContext.mainFrame.textFieldOperationHints2.setText(path);
                        guiContext.mainFrame.textFieldOperationHints2.postActionEvent();
                        break;
                    case "log":
                        guiContext.mainFrame.textFieldOperationLog.setText(path);
                        guiContext.mainFrame.textFieldOperationLog.postActionEvent();
                        break;
                    case "log2":
                        guiContext.mainFrame.textFieldOperationLog2.setText(path);
                        guiContext.mainFrame.textFieldOperationLog2.postActionEvent();
                        break;
                }
            }
            break;
        }
    }

    private String filePickerDirectory(String path)
    {
        if (Utils.isRelativePath(path))
            path = Utils.getElsHome() + System.getProperty("file.separator") + path;
        return Utils.getLeftPath(path, Utils.getSeparatorFromPath(path));
    }

    /**
     * Generic ActionEvent handler
     * <b/>
     * guiContext.operations.genericAction
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
            else if (button.getActionCommand().toLowerCase().endsWith("jobpick"))
                jobPicker(button);
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
     * guiContext.operations.genericTextFieldFocusLost
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

    private int getLogLevelIndex(String level)
    {
        int index = -1;
        ComboBoxModel<String> model = guiContext.mainFrame.comboBoxOperationConsoleLevel.getModel();
        for (int i = 0; i < model.getSize(); ++i)
        {
            if (level.equals(model.getElementAt(i)))
            {
                index = i;
                break;
            }
        }
        return index;
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
        this.configItems = guiContext.mainFrame.operationConfigItems;

        // scale the help icon
        Icon icon = guiContext.mainFrame.labelOperationHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        guiContext.mainFrame.labelOperationHelp.setIcon(replacement);

        // dividers
        if (guiContext.preferences.getOperationDividerLocation() > 0)
        {
            guiContext.mainFrame.splitPaneOperation.setDividerLocation(guiContext.preferences.getOperationDividerLocation());
        }
        if (guiContext.preferences.getOperationDividerConfigLocation() > 0)
        {
            guiContext.mainFrame.splitPaneOperationContent.setDividerLocation(guiContext.preferences.getOperationDividerConfigLocation());
        }

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
        //  * publisher has base objects
        //  * listener has objects2
// LEFTOFF Jobs should not be here or in Operations - Jobs are Jobs, this would allow infinite loops
        modes = new Mode[11];
        modes[0] = new Mode(guiContext.cfg.gs("Operations.mode.localPublish"), OperationsTool.Cards.Publisher, Configuration.Operations.NotRemote);
        modes[1] = new Mode(guiContext.cfg.gs("Operations.mode.remotePublish"), OperationsTool.Cards.Publisher, Configuration.Operations.PublishRemote);
        modes[2] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberListener"), OperationsTool.Cards.Listener, Configuration.Operations.SubscriberListener);
        modes[3] = new Mode(guiContext.cfg.gs("Operations.mode.job.publisher"), OperationsTool.Cards.Publisher, Configuration.Operations.JobProcess);
        modes[4] = new Mode(guiContext.cfg.gs("Operations.mode.job.subscriber"), OperationsTool.Cards.Listener, Configuration.Operations.JobProcess);
        modes[5] = new Mode(guiContext.cfg.gs("Operations.mode.hintServer"), OperationsTool.Cards.HintServer, Configuration.Operations.StatusServer);
        modes[6] = new Mode(guiContext.cfg.gs("Operations.mode.publisherTerminal"), OperationsTool.Cards.Terminal, Configuration.Operations.PublisherManual);
        modes[7] = new Mode(guiContext.cfg.gs("Operations.mode.publisherListener"), OperationsTool.Cards.Listener, Configuration.Operations.PublisherListener);
        modes[8] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberTerminal"), OperationsTool.Cards.Terminal, Configuration.Operations.SubscriberTerminal);
        modes[9] = new Mode(guiContext.cfg.gs("Operations.mode.hintForceQuit"), OperationsTool.Cards.Quitter, Configuration.Operations.StatusServerForceQuit);
        modes[10] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberForceQuit"), OperationsTool.Cards.Quitter, Configuration.Operations.SubscriberListenerForceQuit);

        // make New combobox
        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[] { }));
        comboBoxMode.removeAllItems();
        for (Mode m : modes)
        {
            comboBoxMode.addItem(m);
        }

        guiContext.mainFrame.buttonNewOperation.addActionListener(e -> actionNewClicked(e));
        guiContext.mainFrame.buttonCopyOperation.addActionListener(e -> actionCopyClicked(e));
        guiContext.mainFrame.buttonDeleteOperation.addActionListener(e -> actionDeleteClicked(e));
        guiContext.mainFrame.buttonRunOperation.addActionListener(e -> actionRunClicked(e));
        guiContext.mainFrame.buttonGenerateOperation.addActionListener(e -> actionGenerateClicked(e));
        guiContext.mainFrame.labelOperationHelp.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                actionHelpClicked(e);
            }
        });
        guiContext.mainFrame.buttonOperationSave.addActionListener(e -> actionSaveClicked(e));
        guiContext.mainFrame.buttonOperationCancel.addActionListener(e -> actionCancelClicked(e));

        librariesUI = new LibrariesUI(guiContext);

        initializeComboBoxes();
        loadConfigurations();
        deletedTools = new ArrayList<OperationsTool>();
    }

    private void initializeComboBoxes()
    {
        guiContext.mainFrame.comboBoxOperationWhatsNew.removeAllItems();
        guiContext.mainFrame.comboBoxOperationWhatsNew.addItem(guiContext.cfg.gs("Operations.comboBoxOperationWhatsNew.0.whatsNew"));
        guiContext.mainFrame.comboBoxOperationWhatsNew.addItem(guiContext.cfg.gs("Operations.comboBoxOperationWhatsNew.1.whatsNewAll"));

        guiContext.mainFrame.comboBoxOperationHintKeys.removeAllItems();
        guiContext.mainFrame.comboBoxOperationHintKeys.addItem(guiContext.cfg.gs("Operations.comboBoxOperationHintKeys.0.keys"));
        guiContext.mainFrame.comboBoxOperationHintKeys.addItem(guiContext.cfg.gs("Operations.comboBoxOperationHintKeys.1.keysOnly"));

        guiContext.mainFrame.comboBoxOperationHintsAndServer.removeAllItems();
        guiContext.mainFrame.comboBoxOperationHintsAndServer.addItem(guiContext.cfg.gs("Operations.comboBoxOperationHintsAndServer.0.hints"));
        guiContext.mainFrame.comboBoxOperationHintsAndServer.addItem(guiContext.cfg.gs("Operations.comboBoxOperationHintsAndServer.1.hintServer"));

        guiContext.mainFrame.comboBoxOperationLog.removeAllItems();
        guiContext.mainFrame.comboBoxOperationLog.addItem(guiContext.cfg.gs("Operations.comboBoxOperationLog.0.log"));
        guiContext.mainFrame.comboBoxOperationLog.addItem(guiContext.cfg.gs("Operations.comboBoxOperationLog.1.logOverwrite"));
    }

    private void jobPicker(JButton button)
    {
        Jobs jobsHandler = new Jobs(guiContext);
        try
        {
            class JobItem implements Comparable
            {
                String displayName;
                String configName;

                public JobItem(String displayName, String configName)
                {
                    this.displayName = displayName;
                    this.configName = configName;
                }

                @Override
                public String toString()
                {
                    return displayName;
                }

                @Override
                public int compareTo(Object o)
                {
                    return toString().compareTo(o.toString());
                }
            }

            // get all Jobs
            ArrayList<AbstractTool> jobs = jobsHandler.loadAllJobs();

            // make the String list for display
            ArrayList<JobItem> jobItems = new ArrayList<>();
            for (AbstractTool job : jobs)
            {
                jobItems.add(new JobItem(job.getListName(), job.getConfigName()));
            }
            Collections.sort(jobItems);

            // add the Strings to the JList model
            int selected = 0;
            DefaultListModel<String> dialogList = new DefaultListModel<String>();
            for (int i = 0; i < jobItems.size(); ++i)
            {
                dialogList.addElement(jobItems.get(i).displayName);
                if (currentTool.getOptJob() != null && currentTool.getOptJob().length() > 0)
                {
                    if (currentTool.getOptJob().equals(jobItems.get(i).configName))
                        selected = i;
                }
            }
            JList<String> toolJList = new JList<String>();
            toolJList.setModel(dialogList);
            toolJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            JScrollPane pane = new JScrollPane();
            pane.setViewportView(toolJList);
            toolJList.requestFocus();
            toolJList.setSelectedIndex(selected);
            toolJList.ensureIndexIsVisible(selected);
            Object[] params = {guiContext.cfg.gs("Operations.select.job"), pane};

            int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                String name = toolJList.getSelectedValue();
                int index = 0;
                for (; index < jobs.size(); ++index)
                {
                    if (name.equals(((AbstractTool) jobs.get(index)).getListName()))
                    {
                        break; // it is not possible for index to be invalid
                    }
                }
                AbstractTool tool = jobs.get(index);
                switch (button.getName().toLowerCase())
                {
                    case "job":
                        guiContext.mainFrame.textFieldOperationJob.setText(tool.getConfigName());
                        guiContext.mainFrame.textFieldOperationJob.postActionEvent();
                        break;
                    case "job2":
                        guiContext.mainFrame.textFieldOperationJob2.setText(tool.getConfigName());
                        guiContext.mainFrame.textFieldOperationJob2.postActionEvent();
                        break;
                }
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(guiContext.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void libraryLoader(int which)
    {
        if (libJList == null)
            libJList = new JList<String>();

        Repository repo;
        if (which == 0) // publisher
            repo = guiContext.context.publisherRepo;
        else
            repo = guiContext.context.subscriberRepo;

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
            JCheckBox checkBox = new JCheckBox(guiContext.cfg.gs("Operations.include.selections"));
            checkBox.setToolTipText(guiContext.cfg.gs("Operations.uncheck.to.exclude.selections"));
            checkBox.setSelected(true);
            if (button.getName().toLowerCase().equals("addexc"))
                checkBox.setSelected(false);

            JComboBox combo = new JComboBox();
            combo.addItem(guiContext.cfg.gs("Operations.publisher.libraries"));
            combo.addItem(guiContext.cfg.gs("Operations.subscriber.libraries"));
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
            Object[] params1 = {guiContext.cfg.gs("Operations.select.included.excluded.libraries"), combo, checkBox, pane};
            Object[] params2 = {guiContext.cfg.gs("Operations.select.excluded.libraries"), combo, pane};

            int opt = 0;
            if (button.getName().toLowerCase().equals("addincexc"))
                opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params1, displayName, JOptionPane.OK_CANCEL_OPTION);
            else if (button.getName().toLowerCase().equals("addexc"))
                opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params2, displayName, JOptionPane.OK_CANCEL_OPTION);
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
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    name + guiContext.cfg.gs("Operations.is.a.duplicate"),
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
            String msg = guiContext.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(guiContext.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean libraryRemover(int cardVar, boolean includes, int[] indices)
    {
        boolean changed = false;
        List<String> selections = (cardVar == 1) ? guiContext.mainFrame.listOperationIncludeExclude.getSelectedValuesList() :
                guiContext.mainFrame.listOperationExclude.getSelectedValuesList();
        for (int i = indices.length - 1; i >= 0; --i)
        {
            String dn = selections.get(i);
            if (dn.startsWith(guiContext.cfg.gs("Operations.include")) && !includes)
                continue;
            if (dn.startsWith(guiContext.cfg.gs("Operations.exclude")) && includes)
                continue;

            String cn = "";
            if (includes)
                cn = dn.substring(guiContext.cfg.gs("Operations.include").length());
            else
                cn = dn.substring(guiContext.cfg.gs("Operations.exclude").length());

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
            ArrayList<AbstractTool> toolList = tools.loadAllTools(guiContext, OperationsTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                OperationsTool operation = (OperationsTool) tool;
                configModel.addRow(new Object[]{operation});
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(guiContext.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (configModel.getRowCount() == 0)
        {
            guiContext.mainFrame.buttonCopyOperation.setEnabled(false);
            guiContext.mainFrame.buttonDeleteOperation.setEnabled(false);
            guiContext.mainFrame.buttonRunOperation.setEnabled(false);
            guiContext.mainFrame.buttonGenerateOperation.setEnabled(false);
            guiContext.mainFrame.buttonOperationSave.setEnabled(false);
            guiContext.mainFrame.buttonOperationCancel.setEnabled(false);
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
                exc.add(guiContext.cfg.gs("Operations.exclude") + currentTool.getOptExclude()[i]);
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
        guiContext.mainFrame.listOperationExclude.setModel(model);
        guiContext.mainFrame.scrollPaneOperationExclude.setViewportView(guiContext.mainFrame.listOperationExclude);
        guiContext.mainFrame.listOperationExclude.setSelectionInterval(0, 0);
    }

    private void loadIncludeExcludeList()
    {
        ArrayList<String> incExc = new ArrayList<>();
        if (currentTool.getOptExclude() != null)
        {
            for (int i = 0; i < currentTool.getOptExclude().length; ++i)
            {
                incExc.add(guiContext.cfg.gs("Operations.exclude") + currentTool.getOptExclude()[i]);
            }
        }
        if (currentTool.getOptLibrary() != null)
        {
            for (int i = 0; i < currentTool.getOptLibrary().length; ++i)
            {
                incExc.add(guiContext.cfg.gs("Operations.include") + currentTool.getOptLibrary()[i]);
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
            guiContext.mainFrame.listOperationIncludeExclude.removeAll();
            model.removeAllElements();
            model.clear();
        }
        guiContext.mainFrame.listOperationIncludeExclude.setModel(model);
        guiContext.mainFrame.scrollPaneOperationIncludeExclude.setViewportView(guiContext.mainFrame.listOperationIncludeExclude);
        guiContext.mainFrame.listOperationIncludeExclude.setSelectionInterval(0, 0);
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
            ((CardLayout) guiContext.mainFrame.panelOperationCards.getLayout()).show(guiContext.mainFrame.panelOperationCards, currentTool.getCard().name().toLowerCase());
            guiContext.mainFrame.labelOperationMode.setText(modes[getModeOperationIndex()].name);

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
                case JobProcess:
                    loadOptionsPublisher();
                    break;
                case StatusServer:
                    loadOptionsPublisher();
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

    private void loadOptionsListener()
    {
        MainFrame mf = guiContext.mainFrame;

        // ### LEFT SIDE
        // --- General
        mf.textFieldOperationJob2.setText(currentTool.getOptJob());
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

        // --- Logging
        if (currentTool.getOptLogFile().length() > 0)
        {
            mf.comboBoxOperationLog2.setSelectedIndex(0);
            mf.textFieldOperationLog2.setText(currentTool.getOptLogFile());
        }
        else if (currentTool.getOptLogFileOverwrite().length() > 0)
        {
            mf.comboBoxOperationLog2.setSelectedIndex(1);
            mf.textFieldOperationLog2.setText(currentTool.getOptLogFileOverwrite());
        }
        else
        {
            mf.comboBoxOperationLog2.setSelectedIndex(0);
            mf.textFieldOperationLog2.setText("");
        }
        mf.comboBoxOperationConsoleLevel2.setSelectedIndex(getLogLevelIndex(currentTool.getOptConsoleLevel()));
        mf.comboBoxOperationDebugLevel2.setSelectedIndex(getLogLevelIndex(currentTool.getOptDebugLevel()));


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
        MainFrame mf = guiContext.mainFrame;

        // ### LEFT SIDE
        // --- General
        mf.checkBoxOperationNavigator.setSelected(currentTool.isOptNavigator());
        mf.textFieldOperationJob.setText(currentTool.getOptJob());
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

        // --- Logging
        if (currentTool.getOptLogFile().length() > 0)
        {
            mf.comboBoxOperationLog.setSelectedIndex(0);
            mf.textFieldOperationLog.setText(currentTool.getOptLogFile());
        }
        else if (currentTool.getOptLogFileOverwrite().length() > 0)
        {
            mf.comboBoxOperationLog.setSelectedIndex(1);
            mf.textFieldOperationLog.setText(currentTool.getOptLogFileOverwrite());
        }
        else
        {
            mf.comboBoxOperationLog.setSelectedIndex(0);
            mf.textFieldOperationLog.setText("");
        }
        mf.comboBoxOperationConsoleLevel.setSelectedIndex(getLogLevelIndex(currentTool.getOptConsoleLevel()));
        mf.comboBoxOperationDebugLevel.setSelectedIndex(getLogLevelIndex(currentTool.getOptDebugLevel()));


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
        guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        guiContext.navigator.setComponentEnabled(false, guiContext.mainFrame.panelOperationTop);
        guiContext.mainFrame.buttonOperationCancel.setEnabled(true);
        guiContext.mainFrame.buttonOperationCancel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        guiContext.mainFrame.labelOperationHelp.setEnabled(true);
        guiContext.mainFrame.labelOperationHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Repository pubRepo = guiContext.context.publisherRepo;
        Repository subRepo = guiContext.context.subscriberRepo;

        worker = workerOperation.processToolThread(guiContext, pubRepo, subRepo, null, false);
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
                            processTerminated(currentTool);
                    }
                }
            });

            logger.info(guiContext.cfg.gs("Operations.running.operation") + currentTool.getConfigName());
            guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("Operations.running.operation") + currentTool.getConfigName());
            worker.execute();
        }
        else
            processTerminated(currentTool);
    }

    private void processTerminated(OperationsTool operation)
    {
        if (guiContext.progress != null)
            guiContext.progress.done();

        guiContext.navigator.disableGui(false);
        guiContext.navigator.setComponentEnabled(true, guiContext.mainFrame.panelOperationTop);
        guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;

        if (operation.isRequestStop())
        {
            logger.info(operation.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
            guiContext.mainFrame.labelStatusMiddle.setText(operation.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
        }
        else
        {
            logger.info(guiContext.cfg.gs("Operations.operation") + operation.getConfigName() + guiContext.cfg.gs("Z.completed"));
            guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("Operations.operation") + operation.getConfigName() + guiContext.cfg.gs("Z.completed"));
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
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(guiContext.mainFrame, msg, displayName, JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    public void savePreferences()
    {
        guiContext.preferences.setOperationDividerLocation(guiContext.mainFrame.splitPaneOperation.getDividerLocation());
        guiContext.preferences.setOperationDividerConfigLocation(guiContext.mainFrame.splitPaneOperationContent.getDividerLocation());
    }

    private void updateOnChange(Object source)
    {
        int cardVar = 1;  // 1 publisher; 2 listener
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
                        selection = (cardVar == 2) ? guiContext.mainFrame.comboBoxOperationHintsAndServer2.getSelectedIndex() :
                                guiContext.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex();
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
                    case "hintkeys2":
                        cardVar = 2;
                    case "hintkeys":
                        selection = (cardVar == 2) ? 0 : guiContext.mainFrame.comboBoxOperationHintKeys.getSelectedIndex();
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
                    case "ipwhitelist":
                        current = currentTool.getOptIpWhitelist();
                        currentTool.setOptIpWhitelist(tf.getText());
                        break;
                    case "job2":
                    case "job":
                        current = currentTool.getOptJob();
                        currentTool.setOptJob(tf.getText());
                        break;
                    case "log2":
                        cardVar = 2;
                    case "log":
                        selection = (cardVar == 2) ? guiContext.mainFrame.comboBoxOperationLog2.getSelectedIndex() :
                                guiContext.mainFrame.comboBoxOperationLog.getSelectedIndex();
                        if (selection == 0)
                        {
                            current = currentTool.getOptLogFile();
                            currentTool.setOptLogFile(tf.getText());
                        }
                        else if (selection == 1)
                        {
                            current = currentTool.getOptLogFileOverwrite();
                            currentTool.setOptLogFileOverwrite(tf.getText());
                        }
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
                        if (guiContext.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptWhatsNew();
                            currentTool.setOptWhatsNew(tf.getText());
                        }
                        else if (guiContext.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 1)
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
                    case "keepgoing2":
                    case "keepgoing":
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
                    case "consolelevel2":
                    case "consolelevel":
                        current = getLogLevelIndex(currentTool.getOptConsoleLevel());
                        currentTool.setOptConsoleLevel((String) combo.getItemAt(combo.getSelectedIndex()));
                        break;
                    case "debuglevel2":
                    case "debuglevel":
                        current = getLogLevelIndex(currentTool.getOptDebugLevel());
                        currentTool.setOptDebugLevel((String) combo.getItemAt(combo.getSelectedIndex()));
                        break;
                    case "hints2":
                        cardVar = 2;
                    case "hints":
                        if (currentTool.getOptHints().length() > 0)
                            current = 0;
                        else if (currentTool.getOptHintServer().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationHints2.getText() :
                                    guiContext.mainFrame.textFieldOperationHints.getText();
                            currentTool.setOptHints(value);
                            currentTool.setOptHintServer("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationHints2.getText() :
                                    guiContext.mainFrame.textFieldOperationHints.getText();
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
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationHintKeys2.getText() :
                                    guiContext.mainFrame.textFieldOperationHintKeys.getText();
                            currentTool.setOptKeys(value);
                            currentTool.setOptKeysOnly("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationHintKeys2.getText() :
                                    guiContext.mainFrame.textFieldOperationHintKeys.getText();
                            currentTool.setOptKeysOnly(value);
                            currentTool.setOptKeys("");
                        }
                        break;
                    case "log2":
                        cardVar = 2;
                    case "log":
                        if (currentTool.getOptLogFile().length() > 0)
                            current = 0;
                        else if (currentTool.getOptLogFileOverwrite().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationLog2.getText() :
                                    guiContext.mainFrame.textFieldOperationLog.getText();
                            currentTool.setOptLogFile(value);
                            currentTool.setOptLogFileOverwrite("");
                        }
                        else if (index == 1)
                        {
                            value = (cardVar == 2) ? guiContext.mainFrame.textFieldOperationLog2.getText() :
                                    guiContext.mainFrame.textFieldOperationLog.getText();
                            currentTool.setOptLogFileOverwrite(value);
                            currentTool.setOptLogFile("");
                        }
                        break;
                    case "whatsnew":
                        if (currentTool.getOptWhatsNew().length() > 0)
                            current = 0;
                        else if (currentTool.getOptWhatsNewAll().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            currentTool.setOptWhatsNew(guiContext.mainFrame.textFieldOperationWhatsNew.getText());
                            currentTool.setOptWhatsNewAll("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptWhatsNewAll(guiContext.mainFrame.textFieldOperationWhatsNew.getText());
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
            guiContext.mainFrame.textFieldOperationExportItems.setToolTipText(current);

            current = currentTool.getOptExportText();
            guiContext.mainFrame.textFieldOperationExportText.setToolTipText(current);

            current = currentTool.getOptMismatches();
            guiContext.mainFrame.textFieldOperationMismatches.setToolTipText(current);

            current = "";
            if (guiContext.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 0)
                current = currentTool.getOptWhatsNew();
            else if (guiContext.mainFrame.comboBoxOperationWhatsNew.getSelectedIndex() == 1)
                current = currentTool.getOptWhatsNewAll();
            guiContext.mainFrame.textFieldOperationWhatsNew.setToolTipText(current);
        }

        if (carVar == 2)
        {
            current = currentTool.getOptAuthKeys();
            guiContext.mainFrame.textFieldOperationAuthKeys.setToolTipText(current);

            current = currentTool.getOptBlacklist();
            guiContext.mainFrame.textFieldOperationBlacklist.setToolTipText(current);

            current = currentTool.getOptIpWhitelist();
            guiContext.mainFrame.textFieldOperationIpWhitelist.setToolTipText(current);
        }

        current = currentTool.getOptJob();
        if (carVar == 1)
            guiContext.mainFrame.textFieldOperationJob.setToolTipText(current);
        else
            guiContext.mainFrame.textFieldOperationJob2.setToolTipText(current);

        current = currentTool.getOptTargets();
        if (carVar == 1)
            guiContext.mainFrame.textFieldOperationTargets.setToolTipText(current);
        else
            guiContext.mainFrame.textFieldOperationTargets2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = guiContext.mainFrame.comboBoxOperationHintKeys.getSelectedIndex();
        else
            selected = 0;
        if (selected == 0)
            current = currentTool.getOptKeys();
        else if (selected == 1)
            current = currentTool.getOptKeysOnly();
        if (carVar == 1)
            guiContext.mainFrame.textFieldOperationHintKeys.setToolTipText(current);
        else
            guiContext.mainFrame.textFieldOperationHintKeys2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = guiContext.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex();
        else
            selected = guiContext.mainFrame.comboBoxOperationHintsAndServer2.getSelectedIndex();
        if (selected == 0)
            current = currentTool.getOptHints();
        else if (selected == 1)
            current = currentTool.getOptHintServer();
        if (carVar == 1)
            guiContext.mainFrame.textFieldOperationHints.setToolTipText(current);
        else
            guiContext.mainFrame.textFieldOperationHints2.setToolTipText(current);

        current = "";
        if (carVar == 1)
            selected = guiContext.mainFrame.comboBoxOperationLog.getSelectedIndex();
        else
            selected = guiContext.mainFrame.comboBoxOperationLog2.getSelectedIndex();
        if (selected == 0)
            current = currentTool.getOptLogFile();
        else if (selected == 1)
            current = currentTool.getOptLogFileOverwrite();
        if (carVar == 1)
            guiContext.mainFrame.textFieldOperationLog.setToolTipText(current);
        else
            guiContext.mainFrame.textFieldOperationLog2.setToolTipText(current);
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
