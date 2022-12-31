package com.groksoft.els.gui.operations;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.tools.operations.OperationsTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private MainFrame myDialog;
    private GuiContext guiContext;
    private OperationsUI ui;

    public ConfigModel(GuiContext guiContext, OperationsUI ui)
    {
        super();
        this.guiContext = guiContext;
        this.ui = ui;
        this.myDialog = guiContext.mainFrame;
    }

    /**
     * Find a OperationsUI in the table
     *
     * @param configName OperationsUI configuration name to find
     * @param operation If not null that OperationsUI is skipped (a duplicate check)
     * @return OperationsUI found, or null if not found
     */
    public OperationsTool find(String configName, OperationsTool operation)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((OperationsTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                OperationsTool value = (OperationsTool) getValueAt(i, 0);
                if (operation == null || operation != value)
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
            OperationsTool operation = (OperationsTool) getValueAt(index, 0);
            if (operation != null)
            {
                OperationsTool tmp = find(name, operation);

                // TODO check if Tool is used in any Jobs, prompt user accordingly

                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            guiContext.cfg.gs(("Z.that.configuration.already.exists")),
                            guiContext.cfg.gs("OperationsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!operation.getConfigName().equals(name))
                    {
                        File file = new File(operation.getFullPath());
                        if (file.exists())
                        {
                            ui.getDeletedTools().add(operation.clone());
                        }
                        operation.setConfigName(name);
                        operation.setDataHasChanged();
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
