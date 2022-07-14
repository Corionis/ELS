package com.groksoft.els.gui.tools.renamer;

import com.groksoft.els.Configuration;

import javax.swing.table.DefaultTableModel;

public class ChangesTableModel extends DefaultTableModel
{
    private Configuration cfg;

    public ChangesTableModel(Configuration config)
    {
        super();
        cfg = config;
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
                return cfg.gs("Renamer.browser.selections");
            case 1:
                return cfg.gs("Renamer.new.name");
        }
        return cfg.gs("NavTreeNode.unknown");
    }

}
