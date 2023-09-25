package com.corionis.els.gui.libraries;

import com.corionis.els.Context;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private Context context;
    private String displayName;
    private LibrariesUI librariesUI;

    public ConfigModel(Context context, String displayName, LibrariesUI librariesUI)
    {
        super();
        this.context = context;
        this.displayName = displayName;
        this.librariesUI = librariesUI;
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

    /**
     * Update or set a library JSON file path and name
     *
     * @param name New name
     * @param index Index in the configModel of library
     * @return
     */
    private boolean updateListName(String name, int index)
    {
        boolean success = false;
        if (index >= 0 && index < getRowCount())
        {
            LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) getValueAt(index, 0);
            if (libMeta != null)
            {
                LibrariesUI.LibMeta tmp = find(name, libMeta);
                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            displayName, JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!libMeta.description.equals(name))
                    {
                        File file = new File(libMeta.path);
                        if (file.exists())
                        {
                            context.libraries.getDeletedLibraries().add(libMeta.clone());
                        }

                        libMeta.description = name;
                        libMeta.repo.getLibraryData().libraries.description = name;

                        // create or rename filename
                        String jfn;
                        if (libMeta.repo.getJsonFilename() != null && libMeta.repo.getJsonFilename().length() > 0)
                        {
                            jfn = libMeta.repo.getJsonFilename();
                            int sepPos = jfn.lastIndexOf("/");
                            if (sepPos < 0)
                                sepPos = jfn.lastIndexOf("\\");
                            if (sepPos >= 0)
                                jfn = jfn.substring(0, sepPos + 1) + name + ".json";
                            else
                                jfn = librariesUI.getDirectoryPath() + System.getProperty("file.separator") + libMeta.description + ".json";
                        }
                        else
                            jfn = librariesUI.getDirectoryPath() + System.getProperty("file.separator") + libMeta.description + ".json";
                        libMeta.repo.setJsonFilename(jfn);

                        libMeta.setDataHasChanged();
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
