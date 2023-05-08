package com.groksoft.els.jobs;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;

public class Origins
{
    transient private static Logger logger = LogManager.getLogger("applog");

    /**
     * Determine if NavTreeUserObject is a valid selection
     * @param tuo NavTreeUserObject to check
     * @param realOnly If the current AbstractTool accepts NavTreeUserObject.REAL items only
     * @return true if valid, otherwise false
     */
    private static boolean isValidOrigin(NavTreeUserObject tuo, boolean realOnly)
    {
        if (realOnly && tuo.type == NavTreeUserObject.REAL)
            return true;

        if (!realOnly && (tuo.type == NavTreeUserObject.COLLECTION ||
                tuo.type == NavTreeUserObject.LIBRARY ||
                tuo.type == NavTreeUserObject.REAL))
            return true;

        return false;
    }

    public static ArrayList<Origin>[] makeAllOrigins(Context context, Component component) throws MungeException
    {
        JTree baseTree = null;
        JTable baseTable = null;
        ArrayList<Origin>[] originsArray = new ArrayList[8];

        Object object = context.browser.lastComponent;
        if (object instanceof JTree)
            baseTree = (JTree) object;
        else if (object instanceof JTable)
            baseTable = (JTable) object;

        context.mainFrame.treeCollectionOne.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[0], false);

        context.mainFrame.tableCollectionOne.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[1], false);

        context.mainFrame.treeSystemOne.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[2], false);

        context.mainFrame.tableSystemOne.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[3], false);

        context.mainFrame.treeCollectionTwo.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[4], false);

        context.mainFrame.tableCollectionTwo.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[5], false);

        context.mainFrame.treeSystemTwo.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[6], false);

        context.mainFrame.tableSystemTwo.requestFocus();
        makeOriginsFromSelected(context, component, originsArray[7], false);

        if (baseTree != null)
            baseTree.requestFocus();
        else if (baseTable != null)
            baseTable.requestFocus();

        return originsArray;
    }

    /**
     * Create an ArrayList of Origins from items selected in the Browser
     *
     * @param component The component calling this method, for message dialogs
     * @param origins The ArrayList of Origins to be added to
     * @param realOnly If the current AbstractTool accepts NavTreeUserObject.REAL items only
     * @return boolean true if is Subscriber, else false
     * @throws MungeException with message "HANDLED_INTERNALLY" if a selection is not valid
     */
    public static boolean makeOriginsFromSelected(Context context, Component component, ArrayList<Origin> origins, boolean realOnly) throws MungeException
    {
        boolean isSubscriber = false;
        Object object = context.browser.lastComponent;
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            int row = sourceTree.getLeadSelectionRow();
            if (row > -1)
            {
                TreePath[] paths = sourceTree.getSelectionPaths();
                for (TreePath tp : paths)
                {
                    // do not add items that are children of other items
                    NavTreeNode ctn = (NavTreeNode) tp.getLastPathComponent();
                    boolean child = false;
                    for (TreePath sp : paths)
                    {
                        NavTreeNode ntn = (NavTreeNode) sp.getLastPathComponent();
                        if (ntn != ctn && ntn.isNodeChild(ctn))
                        {
                            logger.info(java.text.MessageFormat.format(context.cfg.gs("Z.skipping.child"),
                                ctn.getUserObject().name, ntn.getUserObject().name));
                            child = true;
                        }
                    }
                    if (child)
                        continue;

                    NavTreeUserObject tuo = ctn.getUserObject();
                    if (!isValidOrigin(tuo, realOnly))
                    {
                        JOptionPane.showMessageDialog(component, context.cfg.gs("Z.invalid.selection") + tuo.name,
                                context.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
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
                        JOptionPane.showMessageDialog(component, context.cfg.gs("Z.invalid.selection") + tuo.name,
                                context.cfg.gs("Z.error"), JOptionPane.WARNING_MESSAGE);
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

    public static void setAllOrigins(Context context, Component component, ArrayList<Origin>[] originsArray) throws MungeException
    {
        JTree baseTree = null;
        JTable baseTable = null;

        if (originsArray.length != 8)
            throw new MungeException("setAllOrigins logic fault");

        Object object = context.browser.lastComponent;
        if (object instanceof JTree)
            baseTree = (JTree) object;
        else if (object instanceof JTable)
            baseTable = (JTable) object;

        context.mainFrame.treeCollectionOne.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[0]);

        context.mainFrame.tableCollectionOne.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[1]);

        context.mainFrame.treeSystemOne.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[2]);

        context.mainFrame.tableSystemOne.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[3]);

        context.mainFrame.treeCollectionTwo.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[4]);

        context.mainFrame.tableCollectionTwo.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[5]);

        context.mainFrame.treeSystemTwo.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[6]);

        context.mainFrame.tableSystemTwo.requestFocus();
        setSelectedFromOrigins(context, component, originsArray[7]);

        if (baseTree != null)
            baseTree.requestFocus();
        else if (baseTable != null)
            baseTable.requestFocus();
    }

    /**
     * Restore previous selected items based on an ArrayList of Origins
     *
     * @param context The Context
     * @param component The component calling this method, for message dialogs
     * @param origins An existing ArrayList of Origins created by makeOriginsFromSelected()
     */
    public static void setSelectedFromOrigins(Context context, Component component, ArrayList<Origin> origins)
    {
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
                        TreePath op = context.browser.scanTreePath(panel, pathElements, true, false, false); // scan
                        if (op != null)
                            paths[i] = op;
                    }
                }

                // select all tree path(s)
                tree.setExpandsSelectedPaths(true);
                tree.setSelectionPaths(paths);
                tree.scrollPathToVisible(origins.get(origins.size() - 1).treePath);
                tree.setAnchorSelectionPath(origins.get(origins.size() - 1).treePath);
                tree.requestFocus();
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
                {
                    TreePath op = context.browser.scanTreePath(panel, pathElements, true, false, false); // scan
                    if (op != null)
                        tp = op;
                }

                // select tree path
                tree.setExpandsSelectedPaths(true);
                tree.setSelectionPath(tp);
                tree.scrollPathToVisible(tp);
                tree.setAnchorSelectionPath(tp);

                // select matching items in table
                JTable table = origin.sourceTable;
                ListSelectionModel model = table.getSelectionModel();
                int row = 0;
                for (int i = 0; i < origins.size(); ++i)
                {
                    // select all matching table rows
                    row = origins.get(i).tableRow;
                    model.addSelectionInterval(row, row);
                }

                table.scrollRectToVisible(new Rectangle(table.getCellRect(row, row, true)));
                table.requestFocus();
            }
        }
    }

}
