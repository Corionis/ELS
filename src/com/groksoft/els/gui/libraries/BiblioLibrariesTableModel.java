package com.groksoft.els.gui.libraries;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BiblioLibrariesTableModel extends DefaultTableModel
{
    Context context;
    String displayName;
    LibrariesUI.LibMeta libMeta;

    public BiblioLibrariesTableModel(Context context, String displayName, LibrariesUI.LibMeta libMeta)
    {
        super();
        this.context = context;
        this.displayName = displayName;
        this.libMeta = libMeta;
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
                    if (libMeta.repo.getLibrary((String) object) != null)
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
                catch (MungeException e)
                {
                    // TODO
                }
            }
        }
        context.mainFrame.tableBiblioLibraries.requestFocus();
        context.mainFrame.tableBiblioLibraries.changeSelection(row, 0, false, false);
    }

}
