package com.groksoft.els.gui.libraries;

import com.groksoft.els.Context;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private Context context;
    private String displayName;

    public ConfigModel(Context context, String displayName)
    {
        super();
        this.context = context;
        this.displayName = displayName;
    }

    /**
     * Find a library in the table
     *
     * @param configName Library configuration name to find
     * @param myMeta Library Meta, if not null that LibMeta is skipped (a duplicate check)
     * @return LibMeta found, or null if not found or skipped
     */
    public LibrariesUI.LibMeta find(String configName, LibrariesUI.LibMeta myMeta)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) getValueAt(i, 0);
            if (libMeta.description.equalsIgnoreCase(configName))
            {
                if (libMeta == null || libMeta != myMeta)
                {
                    return libMeta;
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
            LibrariesUI.LibMeta meta = (LibrariesUI.LibMeta) getValueAt(index, 0);
            if (meta != null)
            {
                LibrariesUI.LibMeta tmp = find(name, meta);
                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            displayName, JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!meta.description.equals(name))
                    {
                        File file = new File(meta.path);
                        if (file.exists())
                        {
                            context.libraries.getDeletedLibraries().add(meta.clone());
                        }
                        meta.description = name;
                        meta.setDataHasChanged();
                    }
                    success = true;
                }
            }
        }

        context.mainFrame.librariesConfigItems.requestFocus();
        context.mainFrame.librariesConfigItems.changeSelection(index, 0, false, false);

        return success;
    }

}
