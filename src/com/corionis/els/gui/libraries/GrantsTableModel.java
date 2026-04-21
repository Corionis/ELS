package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.repository.Grant;
import com.corionis.els.repository.Grants;

import javax.swing.table.DefaultTableModel;

public class GrantsTableModel extends DefaultTableModel
{
    private Context context;
    private Grants grants;
    private LibrariesUI gui;

    private GrantsTableModel()
    {
    }

    public GrantsTableModel(LibrariesUI gui, Context context, Grants grants)
    {
        super();
        this.gui = gui;
        this.context = context;
        this.grants = grants;
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
            case 2:
                return Boolean.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public String getColumnName(int column)
    {
        String name = "";
        switch (column)
        {
            case 0:
                name = context.cfg.gs("Navigator.user.grant.library");
                break;
            case 1:
                name = context.cfg.gs("Navigator.user.grant.read");
                break;
            case 2:
                name = context.cfg.gs("Navigator.user.grant.write");
                break;
        }
        return name;
    }

    @Override
    public int getRowCount()
    {
        return (grants == null) ? 0 : grants.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Grant grant = null;
        if (row < grants.size())
        {
            grant = (Grant) grants.get(row);
            switch (column)
            {
                case 0:
                    return grant.library;
                case 1:
                    return grant.read;
                case 2:
                    return grant.write;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return column != 0;
    }

    public void setGrants(Grants grants)
    {
        this.grants = grants;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        boolean orig;
        int configIndex = gui.configItems.getSelectedRow();
        LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) gui.configItems.getValueAt(configIndex, 0);
        Grant grant = (Grant) grants.get(row);
        if (column == 1)
        {
            orig = grant.read;
            grant.read = (Boolean) object;
            if (grant.read != orig)
            {
                libMeta.setDataHasChanged();
                fireTableRowsUpdated(row, row);
                gui.configModel.fireTableRowsUpdated(configIndex, configIndex);
            }
            if (grant.read == false && grant.write == true)
            {
                grant.write = false;
                libMeta.setDataHasChanged();
                fireTableRowsUpdated(row, row);
                gui.configModel.fireTableRowsUpdated(configIndex, configIndex);
            }
        }
        else if (column == 2)
        {
            orig = grant.write;
            grant.write = (Boolean) object;
            if (grant.write != orig)
            {
                libMeta.setDataHasChanged();
                fireTableRowsUpdated(row, row);
                gui.configModel.fireTableRowsUpdated(configIndex, configIndex);
            }
            if (grant.read == false && grant.write == true)
            {
                grant.read = true;
                libMeta.setDataHasChanged();
                fireTableRowsDeleted(row, row);
                gui.configModel.fireTableRowsUpdated(configIndex, configIndex);
            }
        }
        gui.showUserCounts();
    }

}
