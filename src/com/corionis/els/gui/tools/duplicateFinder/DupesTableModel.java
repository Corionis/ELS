package com.corionis.els.gui.tools.duplicateFinder;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.browser.DateColumn;
import com.corionis.els.gui.browser.SizeColumn;
import com.corionis.els.repository.Item;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class DupesTableModel extends DefaultTableModel
{
    private Context context;
    private ArrayList<Dupe> dupes;

    public DupesTableModel(Context context, ArrayList<Dupe> dupes)
    {
        super();
        this.context = context;
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
                return SizeColumn.class;
            case 2:
                return DateColumn.class;
            case 3:
                return JButton.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 4;
    }

    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
            case 0:
                return "Duplicates";
            case 1:
                return context.cfg.gs("BrowserTable.column.size");
            case 2:
                return context.cfg.gs("BrowserTable.column.modified");
            case 3:
                return context.cfg.gs("DuplicateFinder.action");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        return (dupes == null) ? 0 : dupes.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (dupes != null && dupes.get(row).item != null)
        {
            Dupe dupe = dupes.get(row);
            Item item = dupe.item;
            if (column == 0)
            {
                if (dupe.isTop)
                {
                    String name;
                    if (item.getItemShortName() == null || item.getItemShortName().length() == 0)
                        name = Utils.getRightPath(item.getItemPath(), Utils.getSeparatorFromPath(item.getFullPath()));
                    else
                        name = item.getItemShortName();
                    return "<html><b>" + name + "</b></html>";
                }
                return "  " + item.getFullPath();
            }
            if (column == 1)
            {
                if (!dupe.isTop && !item.isDirectory())
                    return new SizeColumn(item.getSize(), context.cfg.getLongScale());
            }
            if (column == 2)
            {
                if (!dupe.isTop)
                    return new DateColumn(context, item.getModifiedDate());
            }
            if (column == 3)
            {
                if (!dupe.isTop && dupe.item != null)
                    return dupe;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        if (dupes.get(row).item != null && col == 3)
            return true;
        return false;
    }

    public void setDupes(ArrayList<Dupe> dupes)
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
