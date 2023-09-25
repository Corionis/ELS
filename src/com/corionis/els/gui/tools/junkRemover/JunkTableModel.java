package com.corionis.els.gui.tools.junkRemover;

import com.corionis.els.Context;
import com.corionis.els.tools.junkremover.JunkRemoverTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class JunkTableModel extends DefaultTableModel
{
    private Context context;
    private JunkRemoverTool tool;

    public JunkTableModel(Context context)
    {
        super();
        this.context = context;
    }

    public int find(String pattern)
    {
        for (int i = 0; i < tool.getJunkList().size(); ++i)
        {
            String wc = tool.getJunkList().get(i).wildcard;
            if ((wc == null && pattern.length() == 0) || wc.equals(pattern))
                return i;
        }
        return -1;
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
                return context.cfg.gs("JunkRemover.junk.pattern");
            case 1:
                return context.cfg.gs("JunkRemover.case");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    @Override
    public int getRowCount()
    {
        if (tool != null)
            return tool.getJunkList().size();
        return 0;
    }

    @Override
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

    public JunkRemoverTool getTool()
    {
        return tool;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return true;
    }

    @Override
    public void removeRow(int index)
    {
        if (tool != null)
        {
            tool.getJunkList().remove(index);
            tool.setDataHasChanged();
        }
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
                tool.setDataHasChanged();
            }

            if (column == 1)
            {
                ji.caseSensitive = ((Boolean) object).booleanValue();
                tool.setDataHasChanged();
            }
        }
    }

}
