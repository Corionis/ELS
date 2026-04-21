package com.corionis.els.gui.tools.duplicateFinder;

import com.corionis.els.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class DupesTableCellRenderer extends DefaultTableCellRenderer
{
    private Context context;
    private JTable table;
    private Logger logger = LogManager.getLogger("applog");

    public DupesTableCellRenderer(Context context, JTable table)
    {
        this.context = context;
        this.table = table;
    }

    private String getTip(int row, int column)
    {
        String tip = null;
        String value = null;
        int rows = table.getModel().getRowCount();
        int columns = table.getModel().getColumnCount();
        if (row >= 0 && row < rows &&
                column >= 0 && column < columns)
        {
            // get the object
            Object object = table.getModel().getValueAt(row, column);
            if (object != null)
            {
                // get value for tooltip
                if (object instanceof String)
                    value = (String) object;

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
        return tip;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == 0)
        {
            String tip = getTip(row, column);
            ((JLabel)rendererComponent).setToolTipText(tip);
        }
        return rendererComponent;
    }

}
