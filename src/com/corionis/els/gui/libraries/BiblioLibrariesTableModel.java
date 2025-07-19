package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.repository.Library;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BiblioLibrariesTableModel extends DefaultTableModel
{
    Context context;
    String displayName;
    LibrariesUI.LibMeta libMeta;

    public BiblioLibrariesTableModel(Context context)
    {
        super();
        this.context = context;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return String.class;
            case 1:
                return Boolean.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Object value = null;
        if (libMeta != null && libMeta.repo != null && libMeta.repo.getLibraryData().libraries.bibliography != null)
        {
            if (row < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                Library lib = libMeta.repo.getLibraryData().libraries.bibliography[row];
                if (column == 0)
                    value = lib; //.name;
                else
                    value = lib.matchDates;
            }
        }
        return value;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setLibMeta(LibrariesUI.LibMeta libMeta)
    {
        this.libMeta = libMeta;
    }

    @Override
    public void setValueAt(Object object, int row, int col)
    {
        if (libMeta != null && libMeta.repo != null && libMeta.repo.getLibraryData().libraries.bibliography != null)
        {
            if (row < libMeta.repo.getLibraryData().libraries.bibliography.length)
            {
                try
                {
                    if (col == 0)
                    {
                        Library lib = libMeta.repo.getLibrary((String) object);

                        if (lib != null && lib != libMeta.repo.getLibraryData().libraries.bibliography[row])
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Libraries.that.library.already.exists"),
                                    displayName, JOptionPane.WARNING_MESSAGE);
                        }
                        else
                        {
                            libMeta.repo.getLibraryData().libraries.bibliography[row].name = (String) object;
                            libMeta.setDataHasChanged();
                        }
                    }
                    else
                    {
                        libMeta.repo.getLibraryData().libraries.bibliography[row].matchDates = ((Boolean) object).booleanValue();
                        libMeta.setDataHasChanged();
                    }
                }
                catch (MungeException e)
                {
                    // should never happen
                }
            }
        }
        context.mainFrame.tableBiblioLibraries.requestFocus();
        context.mainFrame.tableBiblioLibraries.changeSelection(row, 0, false, false);
    }

}
