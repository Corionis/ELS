package com.groksoft.els.gui.libraries;

import com.groksoft.els.Context;
import com.groksoft.els.repository.Location;

import javax.swing.table.DefaultTableModel;

public class LocationsTableModel extends DefaultTableModel
{
    Context context;
    private boolean initialized = false;
    Location[] locations;

    public LocationsTableModel(Context context, Location[] locations)
    {
        super();
        this.context = context;
        this.locations = locations;
        this.initialized = true;
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
        return 2;
    }

    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
            case 0:
                return context.cfg.gs("Libraries.location");
            case 1:
                return context.cfg.gs("Libraries.minimum.free.space");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (locations != null)
            return locations.length;
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (locations != null && locations.length > 0 && row < locations.length)
        {
            if (column == 0)
                return locations[row].location;
            else if (column == 1)
                return locations[row].minimum;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
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

    public void setLocations(Location[] locations)
    {
        this.locations = locations;
    }

}
