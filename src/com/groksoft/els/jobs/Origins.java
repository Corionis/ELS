package com.groksoft.els.jobs;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

import static com.groksoft.els.gui.Navigator.guiContext;

public class Origins
{
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
                    origins.add(new Origin(sourceTree, tp, tuo));
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
                    origins.add(new Origin(sourceTable, i, tuo));
                }
            }
        }

        return isSubscriber;
    }

    public static boolean setSelectionsFromOrigins(GuiContext guiContext, Component component, ArrayList<Origin> origins)
    {
        boolean state = true;
        if (origins != null && origins.size() > 0)
        {
            JTree tree;
            // tree
            if (origins.get(0).treePath != null)
            {
                tree = origins.get(0).sourceTree;
                TreePath[] paths = new TreePath[origins.size()];
                // assemble tree path(s) and scan to each
                for (int i = 0; i < origins.size(); ++i)
                {
                    Origin origin = origins.get(i);
                    TreePath tp = origin.treePath;
                    Object[] objs = tp.getPath();
                    String[] pathElements = new String[tp.getPathCount()];
                    paths[i] = tp;
                    for (int j = 0; j < tp.getPathCount(); ++j)
                    {
                        NavTreeNode node = (NavTreeNode) objs[j];
                        pathElements[j] = node.getUserObject().name;
                    }
                    String panel = origin.sourceTree.getName().toLowerCase();
                    if (panel.length() > 0)
                        guiContext.browser.scanSelectPath(panel, pathElements); // scan & select
                }
                // select all tree path(s)
                tree.setSelectionPaths(paths);
                tree.scrollPathToVisible(origins.get(origins.size() - 1).treePath);
            }
            else if (origins.get(0).sourceTable != null)
            {
                // for a table there is only one tree selection
                Origin origin = origins.get(0);
                tree = origin.tuo.node.getMyTree();

                // assemble single tree path, then scan to it & select tree node
                TreePath tp = origin.tuo.node.getTreePath();
                Object[] objs = tp.getPath();
                String[] pathElements = new String[tp.getPathCount()];
                for (int j = 0; j < tp.getPathCount(); ++j)
                {
                    NavTreeNode node = (NavTreeNode) objs[j];
                    pathElements[j] = node.getUserObject().name;
                }
                String panel = origin.sourceTable.getName().toLowerCase();
                if (panel.length() > 0)
                    guiContext.browser.scanSelectPath(panel, pathElements); // scan & select

                // select matching items in table
                JTable table = origin.sourceTable;
                ListSelectionModel model = table.getSelectionModel();
                model.clearSelection();
                for (int i = 0; i < origins.size(); ++i)
                {
                    // select all matching table rows
                    model.addSelectionInterval(origins.get(i).tableRow, origins.get(i).tableRow);
                }
            }
        }

        return state;
    }

}
