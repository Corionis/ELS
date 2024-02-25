package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.gui.hints.HintDate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;

public class BrowserTableCellRenderer extends DefaultTableCellRenderer
{
    private Context context;
    private JTable table;

    public BrowserTableCellRenderer(Context context, JTable table)
    {
        this.context = context;
        this.table = table;
    }

    @Override
    public String getToolTipText(MouseEvent e)
    {
        String tip = null;
        String value = null;

        // get the row & column from the mouse point
        java.awt.Point point = e.getPoint();
        int row = table.rowAtPoint(point);
        int column = table.columnAtPoint(point);

        // within the dataset?
        if (row >= 0 && row < table.getModel().getRowCount() &&
                column >= 0 && column < table.getModel().getColumnCount())
        {
            // get the object
            Object object = table.getModel().getValueAt(row, column);
            if (object != null)
            {
                // get value for tooltip
                if (object instanceof String)
                    value = (String) object;
                else if (object instanceof HintDate)
                    value = ((HintDate)object).toString();
                else if (object instanceof DateColumn)
                    value = ((DateColumn)object).toString();
                else if (object instanceof NavTreeUserObject)
                    value = ((NavTreeUserObject) object).name;

                if (value != null)
                {
                    // get the widths of text & column
                    Component component = getComponentAt(row, column);
                    FontMetrics metrics = component.getFontMetrics(component.getFont());
                    int textWidth = SwingUtilities.computeStringWidth(metrics, value);
                    int columnWidth = table.getColumnModel().getColumn(column).getWidth();

                    // will the text fit, i.e. is the ellipsis showing?
                    textWidth += 6; // fudge factor
                    if (columnWidth < textWidth)
                        tip = value;
                }

                // get the description from an ImageIcon for the tooltip
                if (object instanceof ImageIcon)
                    tip = ((ImageIcon) object).getDescription();
            }
        }

        return tip;
    }

}
