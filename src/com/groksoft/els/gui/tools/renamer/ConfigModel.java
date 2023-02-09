package com.groksoft.els.gui.tools.renamer;

import com.groksoft.els.Context;
import com.groksoft.els.tools.renamer.RenamerTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private RenamerUI myDialog;
    private Context context;

    public ConfigModel(Context context, RenamerUI ui)
    {
        super();
        this.context = context;
        this.myDialog = ui;
    }

    /**
     * Find a Renamer in the table
     *
     * @param configName Renamer configuration name to find
     * @param renamer If not null that Renamer is skipped (a duplicate check)
     * @return Renamer found, or null if not found
     */
    public RenamerTool find(String configName, RenamerTool renamer)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((RenamerTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                RenamerTool value = (RenamerTool) getValueAt(i, 0);
                if (renamer == null || renamer != value)
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
            RenamerTool renamer = (RenamerTool) getValueAt(index, 0);
            if (renamer != null)
            {
                RenamerTool tmp = find(name, renamer);

                // TODO check if Tool is used in any Jobs, prompt user accordingly

                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            context.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!renamer.getConfigName().equals(name))
                    {
                        File file = new File(renamer.getFullPath());
                        if (file.exists())
                        {
                            myDialog.getDeletedTools().add(renamer.clone());
                        }
                        renamer.setConfigName(name);
                        renamer.setDataHasChanged();
                    }
                    success = true;
                }
            }
        }

        myDialog.getConfigItems().requestFocus();
        myDialog.getConfigItems().changeSelection(index, 0, false, false);

        return success;
    }

}
