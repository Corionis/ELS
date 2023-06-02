package com.groksoft.els.gui.tools.sleep;

import com.groksoft.els.Context;
import com.groksoft.els.tools.sleep.SleepTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel  extends DefaultTableModel
{
    private SleepUI myDialog;
    private Context context;

    public ConfigModel(Context context, SleepUI ui)
    {
        super();
        this.context = context;
        this.myDialog = ui;
    }

    /**
     * Find a SleepUI in the table
     *
     * @param configName SleepUI configuration name to find
     * @param tool If not null that SleepUI is skipped (a duplicate check)
     * @return SleepUI found, or null if not found
     */
    public SleepTool find(String configName, SleepTool tool)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((SleepTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                SleepTool value = (SleepTool) getValueAt(i, 0);
                if (tool == null || tool != value)
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
            SleepTool tool = (SleepTool) getValueAt(index, 0);
            if (tool != null)
            {
                SleepTool tmp = find(name, tool);

                // TODO check if Tool is used in any Jobs, prompt user accordingly

                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            context.cfg.gs("Sleep.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!tool.getConfigName().equals(name))
                    {
                        File file = new File(tool.getFullPath());
                        if (file.exists())
                        {
                            myDialog.getDeletedTools().add(tool.clone());
                        }
                        tool.setConfigName(name);
                        tool.setDataHasChanged();
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
