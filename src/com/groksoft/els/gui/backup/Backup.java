package com.groksoft.els.gui.backup;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.backup.BackupTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class Backup
{
    private JTable configItems;
    private JComboBox comboBoxMode;
    private BackupConfigModel configModel;
    private BackupTool currentBackup;
    private int currentConfigIndex = -1;
    private ArrayList<BackupTool> deletedTools;
    private GuiContext guiContext;
    private NavHelp helpDialog;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private Mode[] modes;
    private SwingWorker<Void, Void> worker;
    private BackupTool workerBackup = null;
    private boolean workerRunning = false;

    private Backup()
    {
        // hide default constructor
    }

    public Backup(GuiContext guiContext)
    {
        this.guiContext = guiContext;
        this.configItems = guiContext.mainFrame.backupConfigItems;

        // scale the help icon
        Icon icon = guiContext.mainFrame.labelBackupHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        guiContext.mainFrame.labelBackupHelp.setIcon(replacement);

        // dividers
        if (guiContext.preferences.getBackupDividerLocation() > 0)
        {
            guiContext.mainFrame.splitPaneBackup.setDividerLocation(guiContext.preferences.getBackupDividerLocation());
        }
        if (guiContext.preferences.getBackupDividerConfigLocation() > 0)
        {
            guiContext.mainFrame.splitPaneBackupContent.setDividerLocation(guiContext.preferences.getBackupDividerConfigLocation());
        }

        // setup the left-side list of configurations
        configModel = new BackupConfigModel(guiContext, this);
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

        /* Combobox order:
             Local publish w/ Navigator option
             Remote publish w/ Navigator option
             Subscriber listener

             Job
             Hint status server

             Publisher terminal
             Publisher listener
             Subscriber terminal

             Hint server force quit
             Subscriber listener force quit
        */

        // make Mode objects
        modes = new Mode[10];
        modes[0] = new Mode(guiContext.cfg.gs("Backup.mode.localPublish"), "full", Configuration.NOT_REMOTE);
        modes[1] = new Mode(guiContext.cfg.gs("Backup.mode.remotePublish"), "full", Configuration.PUBLISH_REMOTE);
        modes[2] = new Mode(guiContext.cfg.gs("Backup.mode.subscriberListener"), "listener", Configuration.SUBSCRIBER_LISTENER);
        modes[3] = new Mode(guiContext.cfg.gs("Backup.mode.job"), "full", Configuration.JOB_PROCESS);
        modes[4] = new Mode(guiContext.cfg.gs("Backup.mode.hintServer"), "full", Configuration.STATUS_SERVER);
        modes[5] = new Mode(guiContext.cfg.gs("Backup.mode.publisherTerminal"), "terminal", Configuration.PUBLISHER_MANUAL);
        modes[6] = new Mode(guiContext.cfg.gs("Backup.mode.publisherListener"), "listener", Configuration.PUBLISHER_LISTENER);
        modes[7] = new Mode(guiContext.cfg.gs("Backup.mode.subscriberTerminal"), "terminal", Configuration.SUBSCRIBER_TERMINAL);
        modes[8] = new Mode(guiContext.cfg.gs("Backup.mode.hintForceQuit"), "quit", Configuration.STATUS_SERVER_FORCE_QUIT);
        modes[9] = new Mode(guiContext.cfg.gs("Backup.mode.subscriberForceQuit"), "quit", Configuration.SUBSCRIBER_SERVER_FORCE_QUIT);

        comboBoxMode = new JComboBox<>();
        comboBoxMode.setModel(new DefaultComboBoxModel<>(new Mode[]{
        }));

        comboBoxMode.removeAllItems();
        for (Mode m : modes)
        {
            comboBoxMode.addItem(m);
        }

        initialize();
        loadConfigurations();
        deletedTools = new ArrayList<BackupTool>();
    }

    private void actionCancelClicked(ActionEvent e)
    {
    }

    private void actionCopyClicked(ActionEvent e)
    {
    }

    private void actionDeleteClicked(ActionEvent e)
    {
    }

    private void actionGenerateClicked(ActionEvent e)
    {
    }

    private void actionNewClicked(ActionEvent e)
    {

        /* Card types:
            FULL
             Local publish w/ Navigator option
             Remote publish w/ Navigator option
             Job
             Hint status server

            LISTENER
             Publisher listener
             Subscriber listener

            TERMINAL
             Publisher terminal
             Subscriber terminal

            QUIT
             Hint server force quit
             Subscriber listener force quit
        */

        if (configModel.find(guiContext.cfg.gs("Z.untitled"), null) == null)
        {
            String message = guiContext.cfg.gs("Backup.mode.select.type");
            Object[] params = {message, comboBoxMode};

            // get ELS operation/mode
            int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, guiContext.cfg.gs("Backup.mode.type.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {

            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
    }

    private void actionRunClicked(ActionEvent e)
    {
    }

    private void actionSaveClicked(ActionEvent e)
    {
    }

    private void configItemsMouseClicked(MouseEvent e)
    {
    }

    private void initialize()
    {
        guiContext.mainFrame.buttonNewBackup.addActionListener(e -> actionNewClicked(e));
        guiContext.mainFrame.buttonCopyBackup.addActionListener(e -> actionCopyClicked(e));
        guiContext.mainFrame.buttonDeleteBackup.addActionListener(e -> actionDeleteClicked(e));
        guiContext.mainFrame.buttonRunBackup.addActionListener(e -> actionRunClicked(e));
        guiContext.mainFrame.buttonGenerateBackup.addActionListener(e -> actionGenerateClicked(e));
        guiContext.mainFrame.labelBackupHelp.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                actionHelpClicked(e);
            }
        });
        guiContext.mainFrame.buttonBackupSave.addActionListener(e -> actionSaveClicked(e));
        guiContext.mainFrame.buttonBackupCancel.addActionListener(e -> actionCancelClicked(e));
        guiContext.mainFrame.backupConfigItems.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                //super.mouseClicked(mouseEvent);
                configItemsMouseClicked(e);
            }
        });
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    public ArrayList<BackupTool> getDeletedTools()
    {
        return deletedTools;
    }

    private void loadConfigurations()
    {
        try
        {
            Tools tools = new Tools();
            ArrayList<AbstractTool> toolList = tools.loadAllTools(guiContext, BackupTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                BackupTool backup = (BackupTool) tool;
                configModel.addRow(new Object[]{backup});
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                guiContext.browser.printLog(msg, true);
                JOptionPane.showMessageDialog(guiContext.mainFrame, msg, guiContext.cfg.gs("Navigator.splitPane.Browser.tab.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (configModel.getRowCount() == 0)
        {
            guiContext.mainFrame.buttonCopyBackup.setEnabled(false);
            guiContext.mainFrame.buttonDeleteBackup.setEnabled(false);
            guiContext.mainFrame.buttonRunBackup.setEnabled(false);
            guiContext.mainFrame.buttonGenerateBackup.setEnabled(false);
            guiContext.mainFrame.buttonBackupSave.setEnabled(false);
            guiContext.mainFrame.buttonBackupCancel.setEnabled(false);
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
        currentBackup = null;
    }

    public void savePreferences()
    {
        guiContext.preferences.setBackupDividerLocation(guiContext.mainFrame.splitPaneBackup.getDividerLocation());
        guiContext.preferences.setBackupDividerConfigLocation(guiContext.mainFrame.splitPaneBackupContent.getDividerLocation());
    }

    // ================================================================================================================

    private class Mode
    {
        String displayName;
        String cardType;
        int operation;

        public Mode(String displayName, String cardType, int operation)
        {
            this.displayName = displayName;
            this.cardType = cardType;
            this.operation = operation;
        }

        @Override
        public String toString()
        {
            return displayName;
        }
    }

}
