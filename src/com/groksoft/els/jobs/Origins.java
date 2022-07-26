package com.groksoft.els.jobs;

import com.groksoft.els.MungeException;
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
    private static boolean isValidOrigin(NavTreeUserObject tuo, boolean realOnly)
    {
        if (realOnly && tuo.type == NavTreeUserObject.REAL)
            return true;
        else if (!realOnly && (tuo.type == NavTreeUserObject.COLLECTION ||
                tuo.type == NavTreeUserObject.LIBRARY ||
                tuo.type == NavTreeUserObject.REAL))
            return true;
        return false;
    }

    public static boolean makeOriginsFromSelected(Component component, ArrayList<Origin> origins, boolean realOnly) throws MungeException
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
                    boolean child = false;
                    for (TreePath cp : paths)
                    {
                        NavTreeNode ctn = (NavTreeNode) cp.getLastPathComponent();
                        if (ctn != ntn && ctn.isNodeChild(ntn))
                        {
                            child = true;
                        }
                    }
                    if (child)
                        continue;
                    NavTreeUserObject tuo = ntn.getUserObject();
                    if (!isValidOrigin(tuo, realOnly))
                    {
                        JOptionPane.showMessageDialog(component, guiContext.cfg.gs("Z.invalid.selection") + tuo.name,
                                guiContext.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
                        origins = null;
                        throw new MungeException("HANDLED_INTERNALLY");
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
                    if (!isValidOrigin(tuo, realOnly))
                    {
                        JOptionPane.showMessageDialog(component, guiContext.cfg.gs("Z.invalid.selection") + tuo.name,
                                guiContext.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
                        origins = null;
                        throw new MungeException("HANDLED_INTERNALLY");
                    }
                    isSubscriber = tuo.isSubscriber();
                    NavTreeNode ntn = (NavTreeNode) tuo.node.getMyTree().getLastSelectedPathComponent();
                    TreePath tp = ntn.getTreePath();
                    origins.add(new Origin(sourceTable, tp, rows[i], tuo));
                }
            }
        }

        return isSubscriber;
    }

    public static boolean setSelectedFromOrigins(GuiContext guiContext, Component component, ArrayList<Origin> origins)
    {
        boolean state = true;
        if (origins != null && origins.size() > 0)
        {
            JTree tree;
            // tree
            if (origins.get(0).sourceTree != null)
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
                    {
                        paths[i] = guiContext.browser.scanSelectPath(panel, pathElements, false); // scan
                    }
                }
                // select all tree path(s)
                tree.setSelectionPaths(paths);
                tree.scrollPathToVisible(origins.get(origins.size() - 1).treePath);
                guiContext.browser.refreshTree(tree);
            }
            else if (origins.get(0).sourceTable != null) // table
            {
                // for a table there is only one tree selection
                Origin origin = origins.get(0);
                tree = origin.tuo.node.getMyTree();

                // assemble single tree path, then scan to it & select tree node
                TreePath tp = origin.treePath;
                Object[] objs = tp.getPath();
                String[] pathElements = new String[tp.getPathCount()];
                for (int j = 0; j < tp.getPathCount(); ++j)
                {
                    NavTreeNode node = (NavTreeNode) objs[j];
                    pathElements[j] = node.getUserObject().name;
                }
                String panel = origin.sourceTable.getName().toLowerCase();
                if (panel.length() > 0)
                    guiContext.browser.scanSelectPath(panel, pathElements, false); // scan
                guiContext.browser.refreshTree(tree);

                // select matching items in table
                JTable table = origin.sourceTable;
                ListSelectionModel model = table.getSelectionModel();
                for (int i = 0; i < origins.size(); ++i)
                {
                    // select all matching table rows
                    model.addSelectionInterval(origins.get(i).tableRow, origins.get(i).tableRow);
                }
                table.requestFocus();
            }
        }

        return state;
    }

}
