package com.groksoft.els.gui.tools.junkremover;

import com.groksoft.els.Configuration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class JunkTableModel extends DefaultTableModel
{
    private Configuration cfg;
    private JunkRemoverTool tool;

    public JunkTableModel(Configuration config)
    {
        super();
        cfg = config;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return JTextField.class;
            case 1:
                return Boolean.class;
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
                return cfg.gs("JunkRemover.junk.pattern");
            case 1:
                return cfg.gs("JunkRemover.case");
        }
        return cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (tool != null)
            return tool.getJunkList().size();
        return 0;
    }

    public Object getValueAt(int row, int column)
    {
        if (tool != null)
        {
            if (column == 0)
            {
                return tool.getJunkList().get(row).wildcard;
            }

            if (column == 1)
            {
                return tool.getJunkList().get(row).caseSensitive;
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return true;
    }

    public void setTool(JunkRemoverTool junkRemoverTool)
    {
        tool = junkRemoverTool;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (tool != null)
        {
            JunkRemoverTool.JunkItem ji = tool.getJunkList().get(row);
            if (column == 0)
            {
                ji.wildcard = ((JTextField) object).getText();
            }

            if (column == 1)
            {
                ji.caseSensitive = ((Boolean) object).booleanValue();
            }
        }
    }

}
