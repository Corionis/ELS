package com.corionis.els.gui.libraries;

import com.corionis.els.Context;

import javax.swing.table.DefaultTableModel;

public class LibrarySelectorTableModel extends DefaultTableModel
{
    Context context;
    private boolean initialized = false;
    LibrarySelector[] checkboxLibraries;

    public LibrarySelectorTableModel(Context context, LibrarySelector[] checkboxLibraries)
    {
        super();
        this.context = context;
        this.checkboxLibraries = checkboxLibraries;
        this.initialized = true;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
            case 0:
                return "";
            case 1:
                return context.cfg.gs("Libraries.labelBiblioLibraries.text");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (checkboxLibraries != null)
            return checkboxLibraries.length;
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (checkboxLibraries != null && checkboxLibraries.length > 0 && row < checkboxLibraries.length)
        {
            if (column == 0)
                return checkboxLibraries[row].selected;  //checkBox;
            else if (column == 1)
                return checkboxLibraries[row].name;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        if (col == 0)
            return true;
        return false;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }

    public void setCheckboxLibraries(LibrarySelector[] checkboxLibraries)
    {
        this.checkboxLibraries = checkboxLibraries;
    }

    @Override
    public void setValueAt(Object object, int row, int col)
    {
        if (checkboxLibraries != null && checkboxLibraries.length > 0 && row < checkboxLibraries.length)
        {
            if (col == 0)
            {
                boolean sense = (boolean) object;
                this.checkboxLibraries[row].selected = sense;
            }
        }
    }

}
