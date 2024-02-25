package com.corionis.els.gui.util;

import com.corionis.els.gui.browser.BrowserTableModel;
import com.corionis.els.gui.browser.DateColumn;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.hints.HintDate;
import com.corionis.els.gui.hints.HintsTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TooltipsTable extends JTable
{
    public TooltipsTable()
    {
        super();
    }

    @Override
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


    @Override
    public int getRowCount()
    {
        int result = 0;

        TableModel tm = getModel();
        if (tm instanceof BrowserTableModel)
        {
            BrowserTableModel btm = (BrowserTableModel) getModel();
            result = btm.getRowCount();
        }
        else if (tm instanceof HintsTableModel)
        {
            HintsTableModel htm = (HintsTableModel) getModel();
            result = htm.getRowCount();
        }
        else
        {
            DefaultTableModel dtm = (DefaultTableModel) getModel();
            result = dtm.getRowCount();
        }
        return result;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Object obj = null;

        TableModel tm = getModel();
        if (tm instanceof BrowserTableModel)
        {
            BrowserTableModel btm = (BrowserTableModel) getModel();
            obj = btm.getValueAt(row, column);
        }
        else if (tm instanceof HintsTableModel)
        {
            HintsTableModel htm = (HintsTableModel) getModel();
            obj = htm.getValueAt(row, column);
        }
        else
        {
            DefaultTableModel dtm = (DefaultTableModel) getModel();
            obj = dtm.getValueAt(row, column);
        }
        return obj;
    }

}
