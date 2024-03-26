package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.gui.hints.HintDate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class BrowserTableCellRenderer extends DefaultTableCellRenderer
{
    private Context context;
    private JTable table;

    public BrowserTableCellRenderer(Context context, JTable table)
    {
        this.context = context;
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
            row = table.getRowSorter().convertRowIndexToModel(row);
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
                        // table.getComponent() returns null with tables > about 1000 rows
                        // See Settings, Browser tab, tooltipLargeTableCheckBox
                        // When enabled just use a big textWidth value
                        int textWidth = (context.preferences.isTooltipsLargeTables() ? 32768 : 0);

                        // get the widths of text & column
                        Component component = table.getComponentAt(row, column);
                        if (component != null) // covers table > 1000 rows
                        {
                            FontMetrics metrics = component.getFontMetrics(component.getFont());
                            textWidth = SwingUtilities.computeStringWidth(metrics, value);
                        }

                        // will the text fit, i.e. is the ellipsis showing?
                        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
                        textWidth += 10; // fudge factor
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
