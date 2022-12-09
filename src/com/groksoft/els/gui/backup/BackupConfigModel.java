package com.groksoft.els.gui.backup;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.tools.backup.BackupTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class BackupConfigModel extends DefaultTableModel
{
    private MainFrame myDialog;
    private GuiContext guiContext;
    private Backup ui;

    public BackupConfigModel(GuiContext guiContext, Backup ui)
    {
        super();
        this.guiContext = guiContext;
        this.ui = ui;
        this.myDialog = guiContext.mainFrame;
    }

    /**
     * Find a Backup in the table
     *
     * @param configName Backup configuration name to find
     * @param Backup If not null that Backup is skipped (a duplicate check)
     * @return Backup found, or null if not found
     */
    public BackupTool find(String configName, BackupTool Backup)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((BackupTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                BackupTool value = (BackupTool) getValueAt(i, 0);
                if (Backup == null || Backup != value)
                {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        updateListName((String) object, row);
    }

    private boolean updateListName(String name, int index)
    {
        boolean success = false;
        if (index >= 0)
        {
            BackupTool backup = (BackupTool) getValueAt(index, 0);
            if (backup != null)
            {
                BackupTool tmp = find(name, backup);

                // TODO check if Tool is used in any Jobs, prompt user accordingly

                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            guiContext.cfg.gs(("Z.that.configuration.already.exists")),
                            guiContext.cfg.gs("Backup.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!backup.getConfigName().equals(name))
                    {
                        File file = new File(backup.getFullPath());
                        if (file.exists())
                        {
                            ui.getDeletedTools().add(backup.clone());
                        }
                        backup.setConfigName(name);
                        backup.setDataHasChanged();
                    }
                    success = true;
                }
            }
        }

        ui.getConfigItems().requestFocus();
        ui.getConfigItems().changeSelection(index, 0, false, false);

        return success;
    }

}
