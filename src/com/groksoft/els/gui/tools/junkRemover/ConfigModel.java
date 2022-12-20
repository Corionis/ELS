package com.groksoft.els.gui.tools.junkRemover;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private JunkRemoverUI myDialog;
    private GuiContext guiContext;

    public ConfigModel(GuiContext guiContext, JunkRemoverUI ui)
    {
        super();
        this.guiContext = guiContext;
        this.myDialog = ui;
    }

    /**
     * Find a JunkRemover in the table
     *
     * @param configName JunkRemover configuration name to find
     * @param jrt If not null that JunkRemover is skipped (a duplicate check)
     * @return JunkRemover found, or null if not found
     */
    public JunkRemoverTool find(String configName, JunkRemoverTool jrt)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((JunkRemoverTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                JunkRemoverTool value = (JunkRemoverTool) getValueAt(i, 0);
                if (jrt == null || jrt != value)
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
            JunkRemoverTool jrt = (JunkRemoverTool) getValueAt(index, 0);
            if (jrt != null)
            {
                JunkRemoverTool tmp = find(name, jrt);

                // TODO check if Tool is used in any Jobs, prompt user accordingly

                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            guiContext.cfg.gs(("Z.that.configuration.already.exists")),
                            guiContext.cfg.gs("JunkRemover.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!jrt.getConfigName().equals(name))
                    {
                        File file = new File(jrt.getFullPath());
                        if (file.exists())
                        {
                            myDialog.getDeletedTools().add(jrt.clone());
                        }
                        jrt.setConfigName(name);
                        jrt.setDataHasChanged();
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
