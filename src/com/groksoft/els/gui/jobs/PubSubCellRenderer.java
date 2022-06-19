package com.groksoft.els.gui.jobs;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PubSubCellRenderer implements TableCellRenderer
{
    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int col)
    {
        Component comp = null;

        if (col == 1)
        {
            JButton button = (JButton) value;
            comp = button;
        }

        return comp;
    }
}
