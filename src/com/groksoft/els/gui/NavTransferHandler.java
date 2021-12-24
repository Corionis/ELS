package com.groksoft.els.gui;

import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Handler for Drag 'n Drop (DnD) and Copy/Cut/Paste (CCP) for local and/or remote
 */
public class NavTransferHandler extends TransferHandler
{
    private final DataFlavor flavor = new ActivationDataFlavor(ArrayList.class, "application/x-java-object;class=java.util.ArrayList", "ArrayList of NavTreeUserObject");
    private final boolean traceActions = false; // dev-debug
    private int action = TransferHandler.NONE;
    private int depth = 0;
    private GuiContext guiContext;
    private boolean isDrop = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JTable sourceTable;
    private JTree sourceTree;
    private boolean targetIsPublisher = false;
    private JTable targetTable;
    private JTree targetTree;
    private Repository sourceRepo;
    private Repository targetRepo;

    public NavTransferHandler(GuiContext gctxt)
    {
        guiContext = gctxt;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        guiContext.context.fault = false;
        guiContext.form.labelStatusMiddle.setText("");
        if (info.getComponent() instanceof JTable)
        {
            JTable targetTable = (JTable) info.getComponent();
            JTree targetTree = getTargetTree(targetTable);
            NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
            if (targetNode == null || (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0))
                return false;
        }
        else if (info.getComponent() instanceof JTree)
        {
            JTree targetTree = (JTree) info.getComponent();
            JTable targetTable = getTargetTable(targetTree);
            NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
            if (targetNode == null || (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0))
                return false;
        }
        return (info.isDataFlavorSupported(flavor));
    }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
        guiContext.context.fault = false;
        ArrayList<NavTreeUserObject> rowList = new ArrayList<NavTreeUserObject>();
        guiContext.form.labelStatusMiddle.setText("CREATE");
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

            if (traceActions)
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

            if (traceActions)
                guiContext.browser.printLog("Create transferable from " + sourceTree.getName() + " starting at row " + row + ", " + paths.length + " rows total");
        }
        return new DataHandler(rowList, flavor.getMimeType());
    }

    @Override
    protected void exportDone(JComponent c, Transferable info, int act)
    {
        action = act;

        if (traceActions)
            guiContext.browser.printLog("end of exportDone");

        if (isDrop) // Drag 'n Drop
        {
            if (traceActions)
            {
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
            }

            // remove moved data
            if (action == TransferHandler.MOVE && !guiContext.context.fault)
            {
                try
                {
                    ArrayList<NavTreeUserObject> transferData = (ArrayList<NavTreeUserObject>) info.getTransferData(flavor);
                    removeData(transferData);
                }
                catch (Exception e)
                {
                    guiContext.browser.printLog(Utils.getStackTrace(e), true);
                    JOptionPane.showMessageDialog(guiContext.form, "Error getting transfer data: " + e.toString(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }

            isDrop = false;
            action = TransferHandler.NONE;
        }

        guiContext.browser.refreshTree(sourceTree);
        guiContext.browser.refreshTree(targetTree);
    }

    private String getOperation(boolean currentTense)
    {
        String op = "";
        if (action == TransferHandler.COPY)
        {
            op = (currentTense ? "Copy" : "Copied");
        }
        else if (action == TransferHandler.MOVE)
        {
            op = (currentTense ? "Move" : "Moved");
        }
        else if (action == TransferHandler.COPY_OR_MOVE)
        {
            op = "Copy or Move";
        }
        return op;
    }

    private Repository getRepo(NavTreeUserObject tuo)
    {
        Repository repo = null;
        switch (tuo.node.getMyTree().getName())
        {
            case "treeCollectionOne":
            case "treeSystemOne":
                repo = guiContext.context.publisherRepo;
                break;
            case "treeCollectionTwo":
            case "treeSystemTwo":
                repo = guiContext.context.subscriberRepo;
                break;
        }
        return repo;
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

    public JTree getTargetTree(JTable table)
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
        guiContext.context.fault = false;
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
            return false;
        }

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
            if (guiContext.preferences.isShowConfirmations())
            {
                reply = JOptionPane.showConfirmDialog(guiContext.form, "Are you sure you want to " + getOperation(true).toLowerCase() + " " +
                                Utils.formatLong(size, false) + " in " + Utils.formatInteger(count) + " file" + (count > 1 ? "s" : "") + " to " + targetTuo.name + "?" +
                                (guiContext.cfg.isDryRun() ? " (dry-run)" : ""),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            // process the selections
            if (reply == JOptionPane.YES_OPTION)
            {
                count = process(transferData, targetTree, targetTuo);
                if (!guiContext.context.fault)
                {
                    String msg = getOperation(false) + " " + count + " item" + (count > 1 ? "s" : "") + (guiContext.cfg.isDryRun() ? " (dry-run)" : "");
                    guiContext.form.labelStatusMiddle.setText(msg);
                    guiContext.browser.printLog(msg);
                }
            }

            if (traceActions)
                guiContext.browser.printLog("end of importData");

            if (!isDrop) // Copy, Cut, Paste
            {
                if (traceActions)
                {
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
                }

                // remove moved data
                if (action == TransferHandler.MOVE && !guiContext.context.fault)
                    removeData(transferData);
            }

            action = TransferHandler.NONE;
            boolean indicator = (reply == JOptionPane.YES_OPTION && !guiContext.context.fault);
            if (traceActions)
                guiContext.browser.printLog("Returning " + indicator);

            return indicator;
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, "Error importing: " + e.toString(),
                    guiContext.cfg.getNavigatorName(), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
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
        String sourceSep;
        String targetSep;

        sourceRepo = getRepo(sourceTuo);
        sourceSep = sourceRepo.getSeparator();
        targetRepo = getRepo(targetTuo);
        targetSep = targetRepo.getSeparator();

        // get the directory
        if (targetTuo.type == NavTreeUserObject.LIBRARY)
        {
            directory = guiContext.context.transfer.getTarget(sourceRepo, targetTuo.name, sourceTuo.size, targetRepo, !targetIsPublisher, sourceTuo.path);
            File physical = new File(directory);
            directory = physical.getAbsolutePath();
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

        int dirPos = Utils.rightIndexOf(sourceTuo.path, sourceSep, (targetTuo.isDir ? 0 : depth));
        filename = sourceTuo.path.substring(dirPos);
        filename = Utils.pipe(filename, sourceSep);

        // put them together
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
                if (!transferFile(sourceTuo, targetTree, targetTuo))
                    break;
            }
        }
        return count;
    }

    private void removeData(ArrayList<NavTreeUserObject> transferData)
    {
        if (action == TransferHandler.MOVE)
        {
            // iterate the selected source row's user object
            for (int i = transferData.size() -1; i > -1; --i)
            {
                NavTreeUserObject sourceTuo = transferData.get(i);
                if (sourceTuo.isDir)
                    removeDirectory(sourceTuo);
                else
                    removeFile(sourceTuo);

                if (guiContext.context.fault)
                    break;

                NavTreeNode parent = (NavTreeNode) sourceTuo.node.getParent();
                parent.remove(sourceTuo.node);
            }
        }
    }

    public boolean removeDirectory(NavTreeUserObject sourceTuo)
    {
        try
        {
            int childCount = sourceTuo.node.getChildCount(false, false);
            for (int i = 0; i < childCount; ++i)
            {
                NavTreeNode child = (NavTreeNode) sourceTuo.node.getChildAt(i, false, false);
                NavTreeUserObject childTuo = child.getUserObject();
                if (childTuo.isDir)
                {
                    if (!removeDirectory(childTuo))
                        break;
                }
                else
                {
                    if (!removeFile(childTuo))
                        break;
                }
            }

            String msg;
            if (sourceTuo.isRemote)
                msg = "Remote";
            else
                msg = "Local";
            msg += " delete directory " + (guiContext.cfg.isDryRun() ? "dry-run " : "") + sourceTuo.path;
            guiContext.browser.printLog(msg);

            if (!guiContext.cfg.isDryRun())
            {
                guiContext.context.transfer.remove(sourceTuo.path, true, sourceTuo.isRemote);
            }
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, "Error deleting directory: " + e.toString() + "\n\nContinue?",
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
                return false;
        }
        return true;
    }

    public boolean removeFile(NavTreeUserObject sourceTuo)
    {
        try
        {
            String msg;
            if (sourceTuo.isRemote)
                msg = "Remote";
            else
                msg = "Local";
            msg += " delete file " + (guiContext.cfg.isDryRun() ? "dry-run " : "") + sourceTuo.path;
            guiContext.browser.printLog(msg);

            if (!guiContext.cfg.isDryRun())
            {
                guiContext.context.transfer.remove(sourceTuo.path, false, sourceTuo.isRemote);
            }
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, "Error deleting file: " + e.toString() + "\n\nContinue?",
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
                return false;
        }
        return true;
    }

    private NavTreeUserObject setupToNode(NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo, String path)
    {
        boolean exists = false;
        NavTreeNode toNode = null;

        // setup a node
        toNode = targetTuo.node.findChildTuoPath(path);
        if (toNode == null)
        {
            toNode = (NavTreeNode) sourceTuo.node.clone();
            toNode.setMyTree(targetTuo.node.getMyTree());
            toNode.setMyTable(targetTuo.node.getMyTable());
            toNode.setMyStatus(targetTuo.node.getMyStatus());
        }
        else
        {
            exists = true;
        }

        // setup it's user object
        NavTreeUserObject toTuo = toNode.getUserObject();
        toTuo.node = toNode;
        toTuo.path = path;
        toTuo.isRemote = !targetIsPublisher;
        toTuo.file = new File(path);
        toNode.setAllowsChildren(toTuo.isDir);

        // add the new node on the target
        if (!exists && !guiContext.cfg.isDryRun())
            ((NavTreeNode) targetTuo.node).add(toNode);

        return toTuo;
    }

    private int transferDirectory(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        int count = 0;

        try
        {
            int childCount = sourceTuo.node.getChildCount(false, false);

            String path = makeToPath(sourceTuo, targetTuo);
            NavTreeUserObject toNodeTuo = setupToNode(sourceTuo, targetTuo, path);

            for (int i = 0; i < childCount; ++i)
            {
                NavTreeNode child = (NavTreeNode) sourceTuo.node.getChildAt(i, false, false);
                NavTreeUserObject childTuo = child.getUserObject();
                if (childTuo.isDir)
                {
                    ++depth;
                    count = count + transferDirectory(childTuo, targetTree, toNodeTuo);
                    --depth;
                }
                else
                {
                    ++count;
                    if (!transferFile(childTuo, targetTree, toNodeTuo))
                        break;
                }
            }
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, "Error copying: " + e.toString(),
                    guiContext.cfg.getNavigatorName(), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
        }
        return count;
    }

    private boolean transferFile(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        String path = "";
        //String msg = " from " + sourceTable.getName() + " of " + sourceTree.getName() + ", node " + sourceTuo.name;
        String msg = " " + getOperation(true).toLowerCase() + (guiContext.cfg.isDryRun() ? " dry-run from " : " from ") + sourceTuo.path;

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
                {
                    guiContext.context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, false, true);
                    NavTreeUserObject thisTuo = setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (!sourceTuo.isRemote && targetTuo.isRemote)
            {
                // put to remote
                path = makeToPath(sourceTuo, targetTuo);
                msg += " to " + path;
                guiContext.browser.printLog("Put" + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    guiContext.context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, true, false);
                    NavTreeUserObject thisTuo = setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // get from remote
                path = makeToPath(sourceTuo, targetTuo);
                msg += " to " + path;
                guiContext.browser.printLog("Get" + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    String dir = Utils.getLeftPath(path, targetRepo.getSeparator());
                    Files.createDirectories(Paths.get(dir));
                    guiContext.context.clientSftp.get(sourceTuo.path, path);
                    NavTreeUserObject thisTuo = setupToNode(sourceTuo, targetTuo, path);
                    if (guiContext.preferences.isPreserveFileTimes())
                        Files.setLastModifiedTime(Paths.get(path), sourceTuo.fileTime);
                }
            }
            else if (sourceTuo.isRemote && targetTuo.isRemote)
            {
                // send command to remote
                path = makeToPath(sourceTuo, targetTuo);
                msg += " to " + path;
                guiContext.browser.printLog("Remote" + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    String dir = Utils.getLeftPath(path, targetRepo.getSeparator());
                    Files.createDirectories(Paths.get(dir));
                    String command = "copy \"" + sourceTuo.path + "\" \"" + path + "\"";
                    String response = guiContext.context.clientStty.roundTrip(command);
                    if (response.equalsIgnoreCase("true"))
                    {
                        NavTreeUserObject thisTuo = setupToNode(sourceTuo, targetTuo, path);
                        if (guiContext.preferences.isPreserveFileTimes())
                            guiContext.context.clientSftp.setDate(path, (int) sourceTuo.fileTime.to(TimeUnit.SECONDS));
                    }
                    else
                        throw new MungeException("Remote copy of " + sourceTuo.name + " failed");
                }
            }

            // update trees with progress so far, refreshed again in exportDone()
            guiContext.browser.refreshTree(sourceTree);
            guiContext.browser.refreshTree(targetTree);
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.form, "Error copying: " + e.toString() + "\n\nContinue transfer?",
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
                return false;
        }
        return true;
    }

}
