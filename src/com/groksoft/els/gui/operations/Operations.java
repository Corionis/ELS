package com.groksoft.els.gui.operations;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.operations.OperationsTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

public class Operations
{
    private JTable configItems;
    private JComboBox comboBoxMode;
    private ConfigModel configModel;
    private OperationsTool currentTool;
    private int currentConfigIndex = -1;
    private ArrayList<OperationsTool> deletedTools;
    private String displayName;
    private GuiContext guiContext;
    private NavHelp helpDialog;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private Mode[] modes;
    private SwingWorker<Void, Void> worker;
    private OperationsTool workerOperation = null;
    private boolean workerRunning = false;

    private Operations()
    {
        // hide default constructor
    }

    public Operations(GuiContext guiContext)
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
                guiContext.browser.printLog(java.text.MessageFormat.format(guiContext.cfg.gs("Operations.config.cancelled"), workerOperation.getConfigName()));
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
                deletedTools.add(tool);
                configModel.removeRow(index);
                configModel.fireTableDataChanged();
                if (index > 0)
                    index = configModel.getRowCount() - 1;
                configItems.requestFocus();
                if (index >= 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadOptions(index);
                }
                currentConfigIndex = index;
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

            JOptionPane.showInputDialog(guiContext.mainFrame,
                    guiContext.cfg.gs("Z.generated") + currentTool.getConfigName() +
                            "                                                                                    ",
                    displayName, JOptionPane.PLAIN_MESSAGE,null, null, generated);
        }
        catch (Exception e)
        {
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
            Object[] params = {message, comboBoxMode};
            comboBoxMode.setSelectedIndex(0);

            // get ELS operations/mode
            int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, displayName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                currentTool = new OperationsTool(guiContext, guiContext.cfg, guiContext.context);
                currentTool.setDataHasChanged();
                currentTool.setConfigName(guiContext.cfg.gs("Z.untitled"));
                Mode mode = modes[comboBoxMode.getSelectedIndex()];
                currentTool.setOperation(mode.operation);
                initNewCard(currentTool.getOperation());

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

    private void actionRunClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            currentConfigIndex = index;
            OperationsTool tool = (OperationsTool) configModel.getValueAt(index, 0);
            currentTool = tool;

            // make dialog pieces
            String message = java.text.MessageFormat.format(guiContext.cfg.gs("Operations.run.as.defined"), tool.getConfigName());

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, message, guiContext.cfg.gs("Navigator.splitPane.Operations.tab.title"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerOperation = tool.clone();
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
        if (deletedTools.size() > 0)
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((OperationsTool) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    public void eventOperationAddRowClicked(ActionEvent e)
    {
    }

    public void eventOperationRemoveRowClicked(ActionEvent e)
    {
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
        updateOnChange(e.getSource());
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
            if (modes[i].operation == currentTool.getOperation())
            {
                index = i;
                break;
            }
        }
        return index;
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
        modes = new Mode[10];
        modes[0] = new Mode(guiContext.cfg.gs("Operations.mode.localPublish"), "publisher", Configuration.NOT_REMOTE);
        modes[1] = new Mode(guiContext.cfg.gs("Operations.mode.remotePublish"), "publisher", Configuration.PUBLISH_REMOTE);
        modes[2] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberListener"), "listener", Configuration.SUBSCRIBER_LISTENER);
        modes[3] = new Mode(guiContext.cfg.gs("Operations.mode.job"), "publisher", Configuration.JOB_PROCESS);
        modes[4] = new Mode(guiContext.cfg.gs("Operations.mode.hintServer"), "publisher", Configuration.STATUS_SERVER);
        modes[5] = new Mode(guiContext.cfg.gs("Operations.mode.publisherTerminal"), "terminal", Configuration.PUBLISHER_MANUAL);
        modes[6] = new Mode(guiContext.cfg.gs("Operations.mode.publisherListener"), "listener", Configuration.PUBLISHER_LISTENER);
        modes[7] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberTerminal"), "terminal", Configuration.SUBSCRIBER_TERMINAL);
        modes[8] = new Mode(guiContext.cfg.gs("Operations.mode.hintForceQuit"), "quit", Configuration.STATUS_SERVER_FORCE_QUIT);
        modes[9] = new Mode(guiContext.cfg.gs("Operations.mode.subscriberForceQuit"), "quit", Configuration.SUBSCRIBER_SERVER_FORCE_QUIT);

        // make New combobox
        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[]{
        }));
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
/*
        guiContext.mainFrame.operationConfigItems.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                //super.mouseClicked(mouseEvent);
                configItemsMouseClicked(e);
            }
        });
*/

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

    private void initNewCard(int type)
    {
        currentTool.setDataHasChanged();

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
                guiContext.browser.printLog(msg, true);
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
            String cardName = modes[getModeOperationIndex()].cardType;
            ((CardLayout) guiContext.mainFrame.panelOperationCards.getLayout()).show(guiContext.mainFrame.panelOperationCards, cardName);
            guiContext.mainFrame.labelOperationMode.setText(modes[getModeOperationIndex()].name);

            // populate card
            switch (currentTool.getOperation())
            {
                case Configuration.NOT_REMOTE:
                    loadOptionsPublish();
                    break;
                case Configuration.PUBLISH_REMOTE:
                    loadOptionsPublish();
                    break;
                case Configuration.SUBSCRIBER_LISTENER:
                    break;
                case Configuration.JOB_PROCESS:
                    loadOptionsPublish();
                    break;
                case Configuration.STATUS_SERVER:
                    loadOptionsPublish();
                    break;
                case Configuration.PUBLISHER_MANUAL:
                    break;
                case Configuration.PUBLISHER_LISTENER:
                    break;
                case Configuration.SUBSCRIBER_TERMINAL:
                    break;
                case Configuration.STATUS_SERVER_FORCE_QUIT:
                    break;
                case Configuration.SUBSCRIBER_SERVER_FORCE_QUIT:
                    break;
            }
            loading = false;
        }
    }

    private void loadOptionsPublish()
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

        // --- Runtime Options
        mf.checkBoxOperationDryRun.setSelected(currentTool.isOptDryRun());
        mf.checkBoxOperationNoBackFill.setSelected(currentTool.isOptNoBackFill());
        mf.checkBoxOperationOverwrite.setSelected(currentTool.isOptOverwrite());
        mf.checkBoxOperationPreserveDates.setSelected(currentTool.isOptPreserveDates());
        mf.checkBoxOperationDecimalScale.setSelected(currentTool.isOptDecimalScale());
        mf.checkBoxOperationValidate.setSelected(currentTool.isOptValidate());

        // --- Reporting
        mf.checkBoxOperationDuplicates.setSelected(currentTool.isOptDuplicates());
        mf.checkBoxOperationCrossCheck.setSelected(currentTool.isOptCrossCheck());
        mf.checkBoxOperationEmptyDirectories.setSelected(currentTool.isOptEmptyDirectories());
        mf.checkBoxOperationIgnored.setSelected(currentTool.isOptIgnored());
    }

    private void process ()
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
        guiContext.navigator.setComponentEnabled(true,  guiContext.mainFrame.panelOperationTop);
        guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;

        if (operation.isRequestStop())
        {
            guiContext.browser.printLog(operation.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
            guiContext.mainFrame.labelStatusMiddle.setText(operation.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
        }
        else
        {
            guiContext.browser.printLog(operation.getConfigName() + guiContext.cfg.gs("Z.completed"));
            guiContext.mainFrame.labelStatusMiddle.setText(operation.getConfigName() + guiContext.cfg.gs("Z.completed"));
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
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                guiContext.browser.printLog(msg, true);
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

    private void setOptions()
    {
        if (currentTool != null && !loading)
        {
            currentTool.setDataHasChanged();

        }
    }

    private void updateOnChange(Object source)
    {
        String name = null;
        if (source != null && currentTool != null && !loading)
        {
            if (source instanceof JTextField)
            {
                String current = null;
                JTextField tf = (JTextField) source;
                name = tf.getName();
                switch (name)
                {
                    case "exportItems":
                        current = currentTool.getOptExportItems();
                        currentTool.setOptExportItems(tf.getText());
                        break;
                    case "exportText":
                        current = currentTool.getOptExportText();
                        currentTool.setOptExportText(tf.getText());
                        break;
                    case "hints":
                        if (guiContext.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptHints();
                            currentTool.setOptHints(tf.getText());
                        }
                        else if (guiContext.mainFrame.comboBoxOperationHintsAndServer.getSelectedIndex() == 1)
                        {
                            current = currentTool.getOptHintServer();
                            currentTool.setOptHintServer(tf.getText());
                        }
                        break;
                    case "hintKeys":
                        if (guiContext.mainFrame.comboBoxOperationHintKeys.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptKeys();
                            currentTool.setOptKeys(tf.getText());
                        }
                        else if (guiContext.mainFrame.comboBoxOperationHintKeys.getSelectedIndex() == 1)
                        {
                            current = currentTool.getOptKeysOnly();
                            currentTool.setOptKeysOnly(tf.getText());
                        }
                        break;
                    case "job":
                        current = currentTool.getOptJob();
                        currentTool.setOptJob(tf.getText());
                        break;
                    case "log":
                        if (guiContext.mainFrame.comboBoxOperationLog.getSelectedIndex() == 0)
                        {
                            current = currentTool.getOptLogFile();
                            currentTool.setOptLogFile(tf.getText());
                        }
                        else if (guiContext.mainFrame.comboBoxOperationLog.getSelectedIndex() == 1)
                        {
                            current = currentTool.getOptLogFileOverwrite();
                            currentTool.setOptLogFileOverwrite(tf.getText());
                        }
                        break;
                    case "mismatches":
                        current = currentTool.getOptMismatches();
                        currentTool.setOptMismatches(tf.getText());
                        break;
                    case "targets":
                        current = currentTool.getOptTargets();
                        currentTool.setOptTargets(tf.getText());
                        break;
                    case "whatsNew":
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
                }
                if (!current.equals(tf.getText()))
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
                switch (name)
                {
                    case "decimalScale":
                        state = currentTool.isOptDecimalScale();
                        currentTool.setOptDecimalScale(cb.isSelected());
                        break;
                    case "dryRun":
                        state = currentTool.isOptDryRun();
                        currentTool.setOptDryRun(cb.isSelected());
                        break;
                    case "duplicates":
                        state = currentTool.isOptDuplicates();
                        currentTool.setOptDuplicates(cb.isSelected());
                        break;
                    case "crossCheck":
                        state = currentTool.isOptCrossCheck();
                        currentTool.setOptCrossCheck(cb.isSelected());
                        break;
                    case "emptyDirectories":
                        state = currentTool.isOptEmptyDirectories();
                        currentTool.setOptEmptyDirectories(cb.isSelected());
                        break;
                    case "ignored":
                        state = currentTool.isOptIgnored();
                        currentTool.setOptIgnored(cb.isSelected());
                        break;
                    case "keepGoing":
                        state = currentTool.isOptListenerKeepGoing();
                        currentTool.setOptListenerKeepGoing(cb.isSelected());
                        break;
                    case "navigator":
                        state = currentTool.isOptNavigator();
                        currentTool.setOptNavigator(cb.isSelected());
                        break;
                    case "noBackFill":
                        state = currentTool.isOptNoBackFill();
                        currentTool.setOptNoBackFill(cb.isSelected());
                        break;
                    case "overwrite":
                        state = currentTool.isOptOverwrite();
                        currentTool.setOptOverwrite(cb.isSelected());
                        break;
                    case "preserveDates":
                        state = currentTool.isOptPreserveDates();
                        currentTool.setOptPreserveDates(cb.isSelected());
                        break;
                    case "quitStatusServer":
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
                switch (name)
                {
                    case "consolelevel":
                        current = getLogLevelIndex(currentTool.getOptConsoleLevel());
                        currentTool.setOptConsoleLevel((String) combo.getItemAt(combo.getSelectedIndex()));
                        break;
                    case "debuglevel":
                        current = getLogLevelIndex(currentTool.getOptDebugLevel());
                        currentTool.setOptDebugLevel((String) combo.getItemAt(combo.getSelectedIndex()));
                        break;
                    case "hints":
                        if (currentTool.getOptHints().length() > 0)
                            current = 0;
                        else if (currentTool.getOptHintServer().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            currentTool.setOptHints(guiContext.mainFrame.textFieldOperationHints.getText());
                            currentTool.setOptHintServer("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptHintServer(guiContext.mainFrame.textFieldOperationHints.getText());
                            currentTool.setOptHints("");
                        }
                        break;
                    case "keys":
                        if (currentTool.getOptKeys().length() > 0)
                            current = 0;
                        else if (currentTool.getOptKeysOnly().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            currentTool.setOptKeys(guiContext.mainFrame.textFieldOperationHintKeys.getText());
                            currentTool.setOptKeysOnly("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptKeysOnly(guiContext.mainFrame.textFieldOperationHintKeys.getText());
                            currentTool.setOptKeys("");
                        }
                        break;
                    case "log":
                        if (currentTool.getOptLogFile().length() > 0)
                            current = 0;
                        else if (currentTool.getOptLogFileOverwrite().length() > 0)
                            current = 1;
                        if (index == 0)
                        {
                            currentTool.setOptLogFile(guiContext.mainFrame.textFieldOperationLog.getText());
                            currentTool.setOptLogFileOverwrite("");
                        }
                        else if (index == 1)
                        {
                            currentTool.setOptLogFileOverwrite(guiContext.mainFrame.textFieldOperationLog.getText());
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
        }
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
        String cardType;
        int operation;

        public Mode(String name, String cardType, int operation)
        {
            this.name = name;
            this.cardType = cardType;
            this.operation = operation;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

}
