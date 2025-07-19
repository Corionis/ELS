package com.corionis.els.gui.libraries;

import com.corionis.els.Context;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MatchDateCell extends AbstractCellEditor implements TableCellEditor, TableCellRenderer
{
    JCheckBox checkBox = new JCheckBox();
    Context context;

    /**
     * Add a tooltip to the Libraries, Bibliography, Libraries, Match Dates table cell
     *
     * @param context
     */
    public MatchDateCell(Context context)
    {
        this.context  = context;

        checkBox.setToolTipText(context.cfg.gs("MatchDateCell.checkBox.tooltip"));
    }

    @Override
    public Object getCellEditorValue()
    {
        return checkBox.isSelected();
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object o, boolean b, int i, int i1)
    {
        return checkBox;
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        checkBox.setSelected((Boolean) value);
        return checkBox;
    }

}
