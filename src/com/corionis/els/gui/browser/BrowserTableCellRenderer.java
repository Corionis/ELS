package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.gui.hints.HintDate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class BrowserTableCellRenderer extends DefaultTableCellRenderer
{
    private JTable table;

    public BrowserTableCellRenderer(JTable table)
    {
        this.table = table;
    }

    private String getTip(int row, int column)
    {
        String tip = null;
        String value = null;
        if (row >= 0 && row < table.getModel().getRowCount() &&
                column >= 0 && column < table.getModel().getColumnCount())
        {
            // get the object
            Object object = table.getModel().getValueAt(row, column);
            if (object != null)
            {
                // get the description from an ImageIcon for the tooltip
                if (object instanceof ImageIcon)
                    tip = ((ImageIcon) object).getDescription();
                else
                {
                    // get value for tooltip
                    if (object instanceof String)
                        value = (String) object;
                    else if (object instanceof HintDate)
                        value = ((HintDate) object).toString();
                    else if (object instanceof DateColumn)
                        value = ((DateColumn) object).toString();
                    else if (object instanceof NavTreeUserObject)
                        value = ((NavTreeUserObject) object).name;

                    if (value != null)
                    {
                        // get the widths of text & column
                        Component component = table.getComponentAt(row, column);
                        FontMetrics metrics = component.getFontMetrics(component.getFont());
                        int textWidth = SwingUtilities.computeStringWidth(metrics, value);
                        int columnWidth = table.getColumnModel().getColumn(column).getWidth();

                        // will the text fit, i.e. is the ellipsis showing?
                        textWidth += 6; // fudge factor
                        if (columnWidth < textWidth)
                            tip = value;
                    }
                }
            }
        }
        return tip;
    }

   @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column > 0 && column <= 8) // the Browser has 4 columns, the table for Hints has 8 + 1 for each system
        {
            String tip = getTip(row, column);
            ((JLabel)rendererComponent).setToolTipText(tip);
        }
        return rendererComponent;
    }

}
