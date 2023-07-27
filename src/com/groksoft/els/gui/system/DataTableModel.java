package com.groksoft.els.gui.system;

import com.groksoft.els.Context;
import com.groksoft.els.repository.HintKey;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class DataTableModel extends DefaultTableModel
{
    private Context context;
    private boolean dataChanged = false;
    private ArrayList<HintKey> keys;
    private ArrayList<String> ipAddresses;

    public DataTableModel(Context context, ArrayList<HintKey> hintKeys, ArrayList<String> ipAddresses)
    {
        super();
        this.context = context;
        this.keys = hintKeys;
        this.ipAddresses = ipAddresses;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return String.class;
            case 1:
                return String.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        if (keys != null)
            return 2;
        return 1;
    }

    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
            case 0:
                if (keys != null)
                    return context.cfg.gs("Name");
                return context.cfg.gs("IP Address");
            case 1:
                if (keys != null)
                    return context.cfg.gs("Collection UUID Key");
                return "";
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (keys != null)
            return keys.size();
        if (ipAddresses != null)
            return ipAddresses.size();
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (column == 0)
        {
            if (keys != null)
                return keys.get(row).name;
            if (ipAddresses != null)
                return ipAddresses.get(row);
        }
        else if (column == 1)
        {
            if (keys != null)
                return keys.get(row).uuid;
            return "";
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return true;
    }

    public boolean isDataChanged()
    {
        return dataChanged;
    }

    @Override
    public void removeRow(int row)
    {
        if (keys != null)
            keys.remove(row);
        if (ipAddresses != null)
            ipAddresses.remove(row);
    }

    public void setDataHasChanged()
    {
        this.dataChanged = true;
    }

    @Override
    public void setValueAt(Object object, int row, int col)
    {
        if (col == 0)
        {
            if (keys != null)
                keys.get(row).name = (String) object;
            if (ipAddresses != null)
                ipAddresses.set(row, (String) object);
        }
        if (col == 1)
        {
            if (keys != null)
                keys.get(row).uuid = (String) object;
        }

        if (keys != null)
        {
            if (row == keys.size() - 1)
                keys.add(new HintKey());
        }
        if (ipAddresses != null)
        {
            if (row == ipAddresses.size() - 1)
                ipAddresses.add("");
        }
        setDataHasChanged();
    }

}
