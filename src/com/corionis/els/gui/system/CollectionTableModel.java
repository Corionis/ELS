package com.corionis.els.gui.system;

import com.corionis.els.Context;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class CollectionTableModel extends DefaultTableModel
{
    private Context context;
    private ArrayList<CollectionSelector> collectionSelectors = null;

    public CollectionTableModel(Context context, ArrayList<CollectionSelector> collectionSelectors)
    {
        super();
        this.context = context;
        this.collectionSelectors = collectionSelectors;
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
                return context.cfg.gs(("FileEditor.description"));
            case 1:
                return context.cfg.gs(("FileEditor.collection.uuid.key"));
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (collectionSelectors != null && collectionSelectors.size() > 0)
            return collectionSelectors.size();
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (collectionSelectors != null && collectionSelectors.size() > 0)
        {
            if (row < collectionSelectors.size())
            {
                if (column == 0)
                    return collectionSelectors.get(row).description;
                return collectionSelectors.get(row).key;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

}
