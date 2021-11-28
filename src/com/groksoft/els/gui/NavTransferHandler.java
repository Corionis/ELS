package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;

/**
 * Handler for Drag 'n Drop (DnD) and Copy/Cut/Paste (CCP) for local and/or remote
 */
public class NavTransferHandler extends TransferHandler
{
    private final DataFlavor flavor = new ActivationDataFlavor(ArrayList.class, "application/x-java-object;class=java.util.ArrayList", "ArrayList of NavTreeUserObject");
    private int action = TransferHandler.NONE;
    private int depth = 0;
    private GuiContext guiContext;
    private boolean isDrop = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JTable sourceTable;
    private JTree sourceTree;
    private JTable targetTable;
    private JTree targetTree;
    private boolean targetIsPublisher = false;

    public NavTransferHandler(GuiContext gctxt)
    {
        guiContext = gctxt;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        guiContext.form.labelStatusMiddle.setText("");
        if (info.getComponent() instanceof JTable)
        {
            JTable targetTable = (JTable) info.getComponent();
            JTree targetTree = getTargetTree(targetTable);
            NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
            if (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0)
                return false;
        }
        else if (info.getComponent() instanceof JTree)
        {
            JTree targetTree = (JTree) info.getComponent();
            JTable targetTable = getTargetTable(targetTree);
            NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
            if (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0)
                return false;
        }
        return (info.isDataFlavorSupported(flavor));
    }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
        ArrayList<NavTreeUserObject> rowList = new ArrayList<NavTreeUserObject>();
        guiContext.form.labelStatusMiddle.setText("");
        if (c instanceof JTable)
        {
            sourceTable = (JTable) c;
            sourceTree = getTargetTree(sourceTable);
            int row = sourceTable.getSelectedRow();
            if (row < 0)
                return null;
            int[] rows = sourceTable.getSelectedRows();
            if (rows.length < 1)
                return null;
            for (int i = 0; i < rows.length; ++i)
            {
                NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                rowList.add(tuo);
            }
            guiContext.browser.printLog("Create transferable from " + sourceTable.getName() + " starting at row " + row + ", " + rows.length + " rows total");
        }
        else if (c instanceof JTree)
        {
            sourceTree = (JTree) c;
            sourceTable = getTargetTable(sourceTree);
            int row = sourceTree.getLeadSelectionRow();
            if (row < 0)
                return null;
            TreePath[] paths = sourceTree.getSelectionPaths();
            for (TreePath path : paths)
            {
                NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                NavTreeUserObject tuo = ntn.getUserObject();
                rowList.add(tuo);
            }
            guiContext.browser.printLog("Create transferable from " + sourceTree.getName() + " starting at row " + row + ", " + paths.length + " rows total");
        }
        return new DataHandler(rowList, flavor.getMimeType());
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int act)
    {
        action = act;
        if (isDrop)
        {
            isDrop = false;
            action = TransferHandler.NONE;
        }

        guiContext.browser.refreshTree(sourceTree);
        guiContext.browser.refreshTree(targetTree);

        /*
        // KEEP for step-wise debugging; DnD and CCP behave differently
        switch (action)
        {
            case TransferHandler.MOVE:
                guiContext.browser.printLog("Done MOVE");
                break;
            case TransferHandler.COPY:
                guiContext.browser.printLog("Done COPY");
                break;
            case TransferHandler.COPY_OR_MOVE:
                guiContext.browser.printLog("Done COPY_OR_MOVE");
                break;
            case TransferHandler.NONE:
                guiContext.browser.printLog("Done NONE");
                break;
        }
*/
    }

    private String getOperation(boolean priorToProcess)
    {
        String op = "";
        if (action == TransferHandler.COPY)
        {
            op = (priorToProcess ? "Copy" : "Copied");
        }
        else if (action == TransferHandler.MOVE)
        {
            op = (priorToProcess ? "Move" : "Moved");
        }
        else if (action == TransferHandler.COPY_OR_MOVE)
        {
            op = "Copy or Move";
        }
        return op;
    }

    @Override
    public int getSourceActions(JComponent c)
    {
        return TransferHandler.COPY_OR_MOVE;
    }

    private NavTreeNode getTargetNode(TransferHandler.TransferSupport info, JTree targetTree, JTable targetTable)
    {
        NavTreeNode targetNode = null;

        // Drop operation, otherwise Paste
        if (info.isDrop())
        {
            if (info.getComponent() instanceof JTable)
            {
                JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
                if (!dl.isInsertRow())
                {
                    // use the dropped-on node in the target table if it is a directory
                    NavTreeUserObject ttuo = (NavTreeUserObject) targetTable.getValueAt(dl.getRow(), 1);
                    if (ttuo.isDir)
                        targetNode = ((NavTreeUserObject) targetTable.getValueAt(dl.getRow(), 1)).node;
                }
            }
            else if (info.getComponent() instanceof JTree)
            {
                JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
                TreePath dlPath = dl.getPath();
                NavTreeNode dlNode = (NavTreeNode) dlPath.getLastPathComponent();
                NavTreeUserObject ttuo = dlNode.getUserObject();
                if (ttuo.isDir)
                    targetNode = dlNode;
            }
        }

        // use the selected table row if it is a directory
        if (targetNode == null && info.getComponent() instanceof JTable && targetTable.getSelectedRow() >= 0)
        {
            NavTreeUserObject tuo = (NavTreeUserObject) targetTable.getValueAt(targetTable.getSelectedRow(), 1);
            targetNode = tuo.node;
            if (!targetNode.getUserObject().isDir)
                targetNode = null;
        }

        // use the selected node in the target tree
        if (targetNode == null)
            targetNode = (NavTreeNode) targetTree.getLastSelectedPathComponent();

        // use the root node in the target tree
        if (targetNode == null)
            targetNode = (NavTreeNode) targetTree.getModel().getRoot();

        return targetNode;
    }

    private JTable getTargetTable(JTree tree)
    {
        JTable targetTable = null;
        switch (tree.getName())
        {
            case "treeCollectionOne":
                targetTable = guiContext.form.tableCollectionOne;
                targetIsPublisher = true;
                break;
            case "treeSystemOne":
                targetTable = guiContext.form.tableSystemOne;
                targetIsPublisher = true;
                break;
            case "treeCollectionTwo":
                targetTable = guiContext.form.tableCollectionTwo;
                targetIsPublisher = false;
                break;
            case "treeSystemTwo":
                targetTable = guiContext.form.tableSystemTwo;
                targetIsPublisher = false;
        }
        assert (targetTable != null);
        return targetTable;
    }

    private JTree getTargetTree(JTable table)
    {
        JTree targetTree = null;
        switch (table.getName())
        {
            case "tableCollectionOne":
                targetTree = guiContext.form.treeCollectionOne;
                targetIsPublisher = true;
                break;
            case "tableSystemOne":
                targetTree = guiContext.form.treeSystemOne;
                targetIsPublisher = true;
                break;
            case "tableCollectionTwo":
                targetTree = guiContext.form.treeCollectionTwo;
                targetIsPublisher = false;
                break;
            case "tableSystemTwo":
                targetTree = guiContext.form.treeSystemTwo;
                targetIsPublisher = false;
                break;
        }
        assert (targetTree != null);
        return targetTree;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        isDrop = info.isDrop();
        if (isDrop)
        {
            action = info.getUserDropAction();
        }
        if (action == TransferHandler.NONE)
        {
            guiContext.form.labelStatusMiddle.setText("Nothing to do");
            guiContext.browser.printLog("Nothing to do");
            isDrop = false;
            return false;
        }

        // get the target information
        targetTable = null;
        targetTree = null;
        if (info.getComponent() instanceof JTable)
        {
            targetTable = (JTable) info.getComponent();
            targetTree = getTargetTree(targetTable);
        }
        else
        {
            targetTree = (JTree) info.getComponent();
            targetTable = getTargetTable(targetTree);
        }

        NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
        NavTreeUserObject targetTuo = targetNode.getUserObject();

        if (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0)
        {
            JOptionPane.showMessageDialog(guiContext.form, "Cannot transfer to currently selected location", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
//            guiContext.form.labelStatusMiddle.setText("Action cancelled");
//            guiContext.browser.printLog("Action cancelled");
//            action = TransferHandler.NONE;
            return false;
        }

        //guiContext.browser.printLog(getOperation(true) + " to " + targetTable.getName() + " of " + targetTree.getName() + ", node " + targetNode.getUserObject().name);
        guiContext.browser.printLog(getOperation(true) + " to " + targetNode.getUserObject().getPath());

        // handle the actual transfers
        try
        {
            int count = 0;
            long size = 0L;
            ArrayList<NavTreeUserObject> transferData = (ArrayList<NavTreeUserObject>) info.getTransferable().getTransferData(flavor);

            // iterate the selected source row's user object
            for (NavTreeUserObject sourceTuo : transferData)
            {
                NavTreeNode sourceNode = sourceTuo.node;
                sourceTree = sourceNode.getMyTree();
                sourceTable = sourceNode.getMyTable();

                NavTreeNode parent = (NavTreeNode) sourceNode.getParent();
                if (parent == targetNode)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Source & target are the same", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    guiContext.form.labelStatusMiddle.setText("Action cancelled");
                    guiContext.browser.printLog("Action cancelled");
                    action = TransferHandler.NONE;
                    return false;
                }

                if (sourceTuo.type == NavTreeUserObject.REAL)
                {
                    if (sourceTuo.isDir)
                    {
                        sourceNode.deepScanChildren();
                        count = count + sourceNode.deepGetFileCount();
                        size = size + sourceNode.deepGetFileSize();
                    }
                    else
                    {
                        ++count;
                        size = size + sourceTuo.size;
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Cannot transfer " + sourceTuo.getType(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    guiContext.form.labelStatusMiddle.setText("Action cancelled");
                    guiContext.browser.printLog("Action cancelled");
                    action = TransferHandler.NONE;
                    return false;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            if (guiContext.preferences.isConfirmation())
            {
                reply = JOptionPane.showConfirmDialog(guiContext.form, "Are you sure you want to " + getOperation(true).toLowerCase() + " " +
                                Utils.formatLong(size, false) + " in " + Utils.formatInteger(count) + " file" + (count > 1 ? "s" : "") + " to " + targetTuo.name + "?" +
                                (guiContext.cfg.isDryRun() ? " (dry-run)" : ""),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            if (reply == JOptionPane.YES_OPTION)
            {
                process(transferData, targetTree, targetTuo);
                guiContext.form.labelStatusMiddle.setText(getOperation(false) + " " + count + " items" + (guiContext.cfg.isDryRun() ? " (dry-run)" : ""));
            }

            action = TransferHandler.NONE;
            boolean indicator = (reply == JOptionPane.YES_OPTION && !guiContext.context.fault);
            guiContext.browser.printLog("Returning " + indicator);
            return indicator;
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
        }

        guiContext.form.labelStatusMiddle.setText("");
        action = TransferHandler.NONE;
        return false;
    }

    private String makeToPath(NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        String directory = "";
        String filename = "";
        String path = "";
        String targetSep = (targetIsPublisher ?
                guiContext.context.publisherRepo.getSeparator() :
                guiContext.context.subscriberRepo.getSeparator());
        String sourceSep = (targetIsPublisher ?
                guiContext.context.subscriberRepo.getSeparator() :
                guiContext.context.publisherRepo.getSeparator());

        if (targetTuo.type == NavTreeUserObject.LIBRARY)
        {

        }
        else if (targetTuo.type == NavTreeUserObject.DRIVE || targetTuo.type == NavTreeUserObject.HOME)
        {
            directory = targetTuo.path;
        }
        else if (targetTuo.type == NavTreeUserObject.REAL)
        {
            directory = targetTuo.path;
            if (!targetTuo.isDir)
            {
                directory = Utils.getLeftPath(directory, targetSep);
            }
        }
        directory = Utils.pipe(directory, targetSep);
        assert (directory.length() > 0);

        NavTreeNode parent = (NavTreeNode) sourceTuo.node.getParent();
        if (parent.getUserObject().type == NavTreeUserObject.LIBRARY)
        {
            // if source parent is library iterate to find matching directory leader
        }
        else
        {
            int dirPos = Utils.rightIndexOf(parent.getUserObject().path, targetSep, depth);
            filename = sourceTuo.path.substring(dirPos);
        }
        filename = Utils.pipe(filename, sourceSep);
        path = directory + filename;

        path = Utils.unpipe(path, targetSep);

        return path;
    }

    /**
     * Process the list of NavTreeUserObjects assembled
     * <br/>
     * The valid target types are:<br/>
     * + REAL local or remote files
     * + DRIVE and HOME
     * + LIBRARY using it's sources
     * <br/>
     *
     * @param transferData
     * @param targetTuo
     * @return
     */
    private int process(ArrayList<NavTreeUserObject> transferData, JTree targetTree, NavTreeUserObject targetTuo)
    {
        int count = 0;
        depth = 0;
        // iterate the selected source row's user object
        for (NavTreeUserObject sourceTuo : transferData)
        {
            NavTreeNode sourceNode = sourceTuo.node;
            sourceTree = sourceNode.getMyTree();
            sourceTable = sourceNode.getMyTable();

            if (sourceTuo.isDir)
                count = count + transferDirectory(sourceTuo, targetTree, targetTuo);
            else
            {
                ++count;
                transferFile(sourceTuo, targetTree, targetTuo);
            }
        }
        return count;
    }

    private int transferDirectory(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        int count = 0;
        int childCount = sourceTuo.node.getChildCount(false, false);
        for (int i = 0; i < childCount; ++i)
        {
            NavTreeNode child = (NavTreeNode) sourceTuo.node.getChildAt(i, false, false);
            NavTreeUserObject childTuo = child.getUserObject();
            if (childTuo.isDir)
            {
                ++depth;
                count = count + transferDirectory(childTuo, targetTree, targetTuo);
                --depth;
            }
            else
            {
                ++count;
                transferFile(childTuo, targetTree, targetTuo);
            }
        }
        return count;
    }

    private void transferFile(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        String path = "";
        //String msg = " from " + sourceTable.getName() + " of " + sourceTree.getName() + ", node " + sourceTuo.name;
        String msg = (guiContext.cfg.isDryRun() ? " dry-run from " : " from ") + sourceTuo.path;

        try
        {
            // perform the transfer
            if (!sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // local copy
                path = makeToPath(sourceTuo, targetTuo);
                msg += " to " + path;
                guiContext.browser.printLog("Local" + msg);
                if (!guiContext.cfg.isDryRun())
                    guiContext.context.transfer.copyFile(sourceTuo.path, path, true);
            }
            else if (!sourceTuo.isRemote && targetTuo.isRemote)
            {
                // put to remote
                path = makeToPath(sourceTuo, targetTuo);
                msg += " to " + path;
                guiContext.browser.printLog("Put" + msg);
                if (!guiContext.cfg.isDryRun())
                    guiContext.context.transfer.copyFile(sourceTuo.path, path, false);
            }
            else if (sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // get from remote
                guiContext.browser.printLog("Get" + msg);
            }
            else if (sourceTuo.isRemote && targetTuo.isRemote)
            {
                // send command to remote
                guiContext.browser.printLog("Remote" + msg);
            }

            if (!guiContext.cfg.isDryRun())
            {
                boolean exists = false;
                NavTreeNode toNode = null;

                // fix-up the source and target trees
                toNode = targetTuo.node.findChildTuoPath(path);
                if (toNode == null)
                {
                    toNode = (NavTreeNode) sourceTuo.node.clone();
                    toNode.setMyStatus(targetTuo.node.getMyStatus());
                    toNode.setMyTable(targetTuo.node.getMyTable());
                    toNode.setMyTree(targetTuo.node.getMyTree());
                }
                else
                    exists = true;

                NavTreeUserObject toTuo = toNode.getUserObject();
                toTuo.node = toNode; // then make changes
                toTuo.path = path;
                if (toTuo.file != null)
                    toTuo.file = new File(path);
// LEFTOFF : Add target tree node(s) for directories
                if (!exists)
                    ((NavTreeNode) targetTuo.node).add(toNode);

                // if a move then remove source tree node & physical file
                if (action == TransferHandler.MOVE)
                {
                    NavTreeNode parent = (NavTreeNode) sourceTuo.node.getParent();
                    parent.remove(sourceTuo.node);

                    if (!sourceTuo.isRemote)
                    {
                        // local
                        msg = " delete " + path;
                        guiContext.browser.printLog("Local" + msg);
                        if (!guiContext.cfg.isDryRun())
                            guiContext.context.transfer.remove(path);
                    }
                    else if (sourceTuo.isRemote)
                    {
                        // remote
                        msg = " delete " + path;
                        guiContext.browser.printLog("Remote" + msg);
                        if (!guiContext.cfg.isDryRun())
                            guiContext.context.transfer.remove(path);
                    }
                }

                // update trees with progress so far
                guiContext.browser.refreshTree(sourceTree);
                guiContext.browser.refreshTree(targetTree);
            }
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            JOptionPane.showMessageDialog(guiContext.form, "Error while copying: " + e.toString(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }

    }

}
