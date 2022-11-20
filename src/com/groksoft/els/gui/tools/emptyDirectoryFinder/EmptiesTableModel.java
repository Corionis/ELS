package com.groksoft.els.gui.tools.emptyDirectoryFinder;

import com.groksoft.els.Configuration;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class EmptiesTableModel extends DefaultTableModel
{
    private Configuration cfg;
    private ArrayList<EmptyDirectoryFinder.Empty> empties;

    public EmptiesTableModel(Configuration cfg, ArrayList<EmptyDirectoryFinder.Empty> empties)
    {
        super();
        this.cfg = cfg;
        this.empties = empties;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return Boolean.class;
            case 1:
                return JTextField.class;
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
                return cfg.gs("EmptyDirectoryFinder.directories");
        }
        return cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        return (empties == null) ? 0 : empties.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (empties != null)
        {
            if (column == 0)
            {
                return empties.get(row).isSelected;
            }

            if (column == 1)
            {
                return empties.get(row).path;
            }
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

    public void setEmptyies(ArrayList<EmptyDirectoryFinder.Empty> empties)
    {
        this.empties = empties;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (empties != null)
        {
            if (column == 0)
            {
                empties.get(row).isSelected = ((Boolean) object).booleanValue();
            }
        }
    }

}
