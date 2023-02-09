package com.groksoft.els.gui.tools.renamer;

import com.groksoft.els.Context;

import javax.swing.table.DefaultTableModel;

public class ChangesTableModel extends DefaultTableModel
{
    private Context context;

    public ChangesTableModel(Context context)
    {
        super();
        this.context = context;
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
                return context.cfg.gs("Renamer.old.name");
            case 1:
                return context.cfg.gs("Renamer.new.name");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

}
