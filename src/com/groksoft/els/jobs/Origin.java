package com.groksoft.els.jobs;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.sun.org.apache.xpath.internal.operations.Or;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

import static com.groksoft.els.gui.Navigator.guiContext;

public class Origin
{
    private String name;
    private int type;

    public Origin(NavTreeUserObject tuo)
    {
        this.name = tuo.getPath();
        this.type = tuo.type;
    }

    public Origin(String name, int type)
    {
        this.name = name;
        this.type = type;
    }

    public Origin clone()
    {
        return new Origin(this.name, this.type);
    }

    public String getName()
    {
        return name;
    }

    public int getType()
    {
        return type;
    }

    public static boolean isValidOrigin(NavTreeUserObject tuo)
    {
        if (tuo.type == NavTreeUserObject.COLLECTION ||
                tuo.type == NavTreeUserObject.LIBRARY ||
                tuo.type == NavTreeUserObject.REAL)
            return true;
        return false;
    }

    public static boolean makeOriginsFromSelected(Component component, ArrayList<Origin> origins)
    {
        boolean isSubscriber = false;
        Object object = guiContext.browser.lastComponent;
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            int row = sourceTree.getLeadSelectionRow();
            if (row > -1)
            {
                TreePath[] paths = sourceTree.getSelectionPaths();
                for (TreePath tp : paths)
                {
                    NavTreeNode ntn = (NavTreeNode) tp.getLastPathComponent();
                    NavTreeUserObject tuo = ntn.getUserObject();
                    if (!isValidOrigin(tuo))
                    {
                        JOptionPane.showMessageDialog(component, guiContext.cfg.gs("Z.invalid.selection") + tuo.name,
                                guiContext.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
                        origins = null;
                        break;
                    }
                    isSubscriber = tuo.isSubscriber();
                    origins.add(new Origin(tuo));
                }
            }
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            int row = sourceTable.getSelectedRow();
            if (row > -1)
            {
                int[] rows = sourceTable.getSelectedRows();
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                    if (!isValidOrigin(tuo))
                    {
                        JOptionPane.showMessageDialog(component, guiContext.cfg.gs("Z.invalid.selection") + tuo.name,
                                guiContext.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
                        origins = null;
                        break;
                    }
                    isSubscriber = tuo.isSubscriber();
                    origins.add(new Origin(tuo));
                }
            }
        }

        return isSubscriber;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setType(int type)
    {
        this.type = type;
    }

}
