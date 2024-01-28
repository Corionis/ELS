package com.corionis.els.gui.util;

import com.corionis.els.gui.browser.DateColumn;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.hints.HintDate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TooltipsTable extends JTable
{
    public String getToolTipText(MouseEvent e)
    {
        String tip = null;
        String value = null;

        // get the row & column from the mouse point
        java.awt.Point point = e.getPoint();
        int row = rowAtPoint(point);
        int column = columnAtPoint(point);

        // within the dataset?
        if (row >= 0 && row < getRowCount() &&
            column >= 0 && column < getColumnCount())
        {
            // get the object
            Object object = getValueAt(row, column);
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
                    int columnWidth = getColumnModel().getColumn(column).getWidth();

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
