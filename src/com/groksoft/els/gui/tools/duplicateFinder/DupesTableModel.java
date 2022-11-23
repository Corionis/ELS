package com.groksoft.els.gui.tools.duplicateFinder;

import com.groksoft.els.Configuration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class DupesTableModel extends DefaultTableModel
{
    private Configuration cfg;
    private ArrayList<DuplicateFinderUI.Dupe> dupes;

    public DupesTableModel(Configuration cfg, ArrayList<DuplicateFinderUI.Dupe> dupes)
    {
        super();
        this.cfg = cfg;
        this.dupes = dupes;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return String.class;
            case 1:
                return JPanel.class;
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
                return "Duplicates";
            case 1:
                return cfg.gs("Actions");
        }
        return cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        return (dupes == null) ? 0 : dupes.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (dupes != null)
        {
            if (column == 0)
            {
                if (dupes.get(row).item != null)
                {
                    if (dupes.get(row).separator)
                        return "<html><b>" + dupes.get(row).item.getItemPath() + "</b></html>";
                    return "  " + dupes.get(row).item.getFullPath();
                }
                else
                    return " ";
            }

            if (column == 1)
            {
                if (dupes.get(row).item != null)
                    return null;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        if (dupes.get(row).item != null && col == 1)
            return true;
        return false;
    }

    public void setDupes(ArrayList<DuplicateFinderUI.Dupe> dupes)
    {
        this.dupes = dupes;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (dupes != null)
        {
        }
    }

}

