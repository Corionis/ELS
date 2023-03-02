package com.groksoft.els.gui.browser;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.HintKeys;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Handler for Drag 'n Drop (DnD) and Copy/Cut/Paste (CCP) for local and/or remote
 */
@SuppressWarnings(value = "unchecked")
public class NavTransferHandler extends TransferHandler
{
    private final DataFlavor flavor = new ActivationDataFlavor(ArrayList.class, "application/x-java-object;class=java.util.ArrayList", "ArrayList of NavTreeUserObject");
    private final boolean traceActions = false; // dev-debug
    private Context context;
    public int fileNumber = 0;
    public int filesToCopy = 0;
    private int action = TransferHandler.NONE;
    private boolean isDrop = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JTable sourceTable;
    private JTree sourceTree;
    private JTable targetTable;
    private JTree targetTree;
    public static NavTransferWorker transferWorker = null;

    public NavTransferWorker getTransferWorker()
    {
        return transferWorker;
    }

    public NavTransferHandler(Context context)
    {
        this.context = context;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        context.fault = false;
        context.mainFrame.labelStatusMiddle.setText("");
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
        context.fault = false;
        ArrayList<NavTreeUserObject> rowList = new ArrayList<NavTreeUserObject>();

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
                logger.trace("Create transferable from " + sourceTable.getName() + " starting at row " + row + ", " + rows.length + " rows total");
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
                logger.trace("Create transferable from " + sourceTree.getName() + " starting at row " + row + ", " + paths.length + " rows total");
        }
        return new DataHandler(rowList, flavor.getMimeType());
    }

    @Override
    protected void exportDone(JComponent c, Transferable info, int act)
    {
        action = act;

        if (traceActions)
            logger.trace("end of exportDone");

        if (isDrop) // Drag 'n Drop
        {
            if (traceActions)
            {
                // KEEP for step-wise debugging; DnD and CCP behave differently
                switch (action)
                {
                    case TransferHandler.MOVE:
                        logger.trace("Done MOVE");
                        break;
                    case TransferHandler.COPY:
                        logger.trace("Done COPY");
                        break;
                    case TransferHandler.COPY_OR_MOVE:
                        logger.trace("Done COPY_OR_MOVE");
                        break;
                    case TransferHandler.NONE:
                        logger.trace("Done NONE");
                        break;
                }
            }

            isDrop = false;
            action = TransferHandler.NONE;
        }
    }

    /**
     * Export a Hint to subscriber
     *
     * @param act       Action mv or rm
     * @param sourceTuo
     * @param targetTuo
     * @throws Exception
     */
    public synchronized void exportHint(String act, NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        if (context.browser.isHintTrackingEnabled())
        {
            String hintPath = context.hints.writeHint(act, context.preferences.isLastIsWorkstation(), sourceTuo, targetTuo);
            if (hintPath.length() > 0)
            {
                if (hintPath.toLowerCase().equals("false"))
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.hint.could.not.be.created"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    // make tuo and add node if it doesn't exist
                    NavTreeNode ntn = ((NavTreeNode) sourceTuo.node.getParent()).findChildName(Utils.getRightPath(hintPath, null));
                    if (ntn == null)
                    {
                        NavTreeUserObject createdTuo = null;
                        NavTreeNode createdNode = new NavTreeNode(context, sourceTuo.node.getMyRepo(), sourceTuo.node.getMyTree());
                        if (sourceTuo.isRemote)
                        {
                            Thread.sleep(500L); // give the remote time to register new hint file
                            SftpATTRS attrs = context.clientSftp.stat(hintPath);
                            createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(hintPath, null),
                                    hintPath, attrs.getSize(), attrs.getMTime(), false);
                        }
                        else
                        {
                            createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(hintPath, null), new File(hintPath));
                        }

                        createdNode.setNavTreeUserObject(createdTuo);
                        createdNode.setAllowsChildren(false);
                        createdNode.setVisible(!context.preferences.isHideFilesInTree());
                        ((NavTreeNode) sourceTuo.node.getParent()).add(createdNode);
                    }

                    // update status tracking
                    if (!context.preferences.isLastIsWorkstation() || sourceTuo.isSubscriber())
                    {
                        NavTreeNode node = sourceTuo.getParentLibrary();
                        if (node == null)
                            throw new MungeException("logic fault: cannot find parent library of relevant item");
                        String lib = node.getUserObject().name;
                        String itemPath = sourceTuo.getItemPath(lib, hintPath);
                        HintKeys.HintKey key = context.hintKeys.findKey(sourceTuo.getRepo().getLibraryData().libraries.key);
                        if (key == null)
                            throw new MungeException("Repository not found in ELS keys " + context.hintKeys.getFilename() + " matching key in " + sourceTuo.getRepo().getLibraryData().libraries.description);
                        String backup = key.name;
                        String status = "Done";
                        context.hints.updateStatusTracking(lib, itemPath, backup, status);
                    }
                }
            }
        }
    }

    public synchronized String getOperation(int actionValue, boolean currentTense)
    {
        String op = "";
        if (actionValue == TransferHandler.COPY)
        {
            op = (currentTense ? context.cfg.gs("NavTransferHandler.copy") : context.cfg.gs("NavTransferHandler.copied"));
        }
        else if (actionValue == TransferHandler.MOVE)
        {
            op = (currentTense ? context.cfg.gs("NavTransferHandler.move") : context.cfg.gs("NavTransferHandler.moved"));
        }
        else if (actionValue == TransferHandler.COPY_OR_MOVE)
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

        // Drop operationsUI, otherwise Paste
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
                targetTable = context.mainFrame.tableCollectionOne;
                break;
            case "treeSystemOne":
                targetTable = context.mainFrame.tableSystemOne;
                break;
            case "treeCollectionTwo":
                targetTable = context.mainFrame.tableCollectionTwo;
                break;
            case "treeSystemTwo":
                targetTable = context.mainFrame.tableSystemTwo;
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
                targetTree = context.mainFrame.treeCollectionOne;
                break;
            case "tableSystemOne":
                targetTree = context.mainFrame.treeSystemOne;
                break;
            case "tableCollectionTwo":
                targetTree = context.mainFrame.treeCollectionTwo;
                break;
            case "tableSystemTwo":
                targetTree = context.mainFrame.treeSystemTwo;
                break;
        }
        assert (targetTree != null);
        return targetTree;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        fileNumber = 0;
        context.fault = false;

        isDrop = info.isDrop();
        if (isDrop)
        {
            action = info.getUserDropAction();
        }
        if (action == TransferHandler.NONE)
        {
            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.nothing.to.do"));
            logger.info(context.cfg.gs("NavTransferHandler.nothing.to.do"));
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
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.cannot.transfer.to.currently.selected.location"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

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
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.source.target.are.the.same"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    logger.info(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    action = TransferHandler.NONE;
                    return false;
                }

                // sum the count and size with deep scan
                if (sourceTuo.type == NavTreeUserObject.REAL)
                {
                    if (sourceTuo.isDir)
                    {
                        sourceNode.deepScanChildren(true);
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
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.cannot.transfer") + sourceTuo.getType(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    logger.info(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    action = TransferHandler.NONE;
                    return false;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            boolean confirm = (isDrop ? context.preferences.isShowDnDConfirmation() : context.preferences.isShowCcpConfirmation());
            if (confirm)
            {
                String msg = MessageFormat.format(context.cfg.gs("NavTransferHandler.are.you.sure.you.want.to"),
                        getOperation(action,true), Utils.formatLong(size, false, context.cfg.getLongScale()),
                        Utils.formatInteger(count), count > 1 ? 0 : 1, targetTuo.name);
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            // process the selections
            if (reply == JOptionPane.YES_OPTION)
            {
                filesToCopy = count;
                process(action, count, size, transferData, targetTree, targetTuo); // fire NavTransferWorker thread <<<<<<<<<<<<<<<<
            }

            if (traceActions)
                logger.trace("end of importData");

            if (!isDrop) // Copy, Cut, Paste
            {
                if (traceActions)
                {
                    // KEEP for step-wise debugging; DnD and CCP behave differently
                    switch (action)
                    {
                        case TransferHandler.MOVE:
                            logger.trace("Done MOVE");
                            break;
                        case TransferHandler.COPY:
                            logger.trace("Done COPY");
                            break;
                        case TransferHandler.COPY_OR_MOVE:
                            logger.trace("Done COPY_OR_MOVE");
                            break;
                        case TransferHandler.NONE:
                            logger.trace("Done NONE");
                            break;
                    }
                }
            }

            action = TransferHandler.NONE;
            boolean indicator = (reply == JOptionPane.YES_OPTION && !context.fault);
            if (traceActions)
                logger.trace("Returning " + indicator);

            return indicator;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Browser.error") + e.toString(),
                    context.cfg.getNavigatorName(), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
        }

        action = TransferHandler.NONE;
        return false;
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
    private void process(int action, int count, long size, ArrayList<NavTreeUserObject> transferData, JTree targetTree, NavTreeUserObject targetTuo) throws Exception
     {
        if (transferWorker == null || transferWorker.isDone())
        {
            transferWorker = null; // suggest clean-up
            transferWorker = new NavTransferWorker(context);
        }
        transferWorker.add(action, count, size, transferData, targetTree, targetTuo);
        transferWorker.execute();
    }

    public synchronized boolean removeDirectory(NavTreeUserObject sourceTuo)
    {
        boolean error = false;

        // remove children
        try
        {
            int childCount = sourceTuo.node.getChildCount(false, false);
            for (int i = 0; i < childCount; ++i)
            {
                NavTreeNode child = (NavTreeNode) sourceTuo.node.getChildAt(i, false, false);
                NavTreeUserObject childTuo = child.getUserObject();
                if (childTuo.isDir)
                {
                    if (removeDirectory(childTuo))
                    {
                        error = true;
                        break;
                    }
                }
                else
                {
                    if (removeFile(childTuo))
                    {
                        error = true;
                        break;
                    }
                }
            }

            if (!error)
            {
                String msg;
                if (sourceTuo.isRemote)
                    msg = context.cfg.gs("Z.remote.uppercase");
                else
                    msg = context.cfg.gs("NavTreeNode.local");
                msg += MessageFormat.format(context.cfg.gs("NavTransferHandler.delete.directory.message"), context.cfg.isDryRun() ? 0 : 1, sourceTuo.path);
                logger.info(msg);

                // remove directory itself
                if (!context.cfg.isDryRun())
                {
                    context.transfer.remove(sourceTuo.path, true, sourceTuo.isRemote);
                }
            }
        }
        catch (Exception e)
        {
            //context.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            logger.error(Utils.getStackTrace(e));
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.delete.directory.error") +
                            e.toString() + "\n\n" + context.cfg.gs("NavTransferHandler.continue"),
                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
                error = true;
        }
        return error;
    }

    public synchronized boolean removeFile(NavTreeUserObject sourceTuo)
    {
        boolean error = false;
        try
        {
            String msg;
            if (sourceTuo.isRemote)
                msg = context.cfg.gs("Z.remote.uppercase");
            else
                msg = context.cfg.gs("NavTreeNode.local");
            msg += MessageFormat.format(context.cfg.gs("NavTransferHandler.delete.file.message"), context.cfg.isDryRun() ? 0 : 1,sourceTuo.path);

            if (!context.cfg.isDryRun())
            {
                context.transfer.remove(sourceTuo.path, false, sourceTuo.isRemote);
                logger.info(msg); // not printed if file is missing
            }
        }
        catch (Exception e)
        {
            // ignore missing file on remote or local move
            boolean skip = false;
            String en = e.getClass().getName();
            if (en.equals("com.jcraft.jsch.SftpException") && e.getMessage().contains("java.nio.file.NoSuchFileException"))
                skip = true;
            if (en.equals("java.nio.file.NoSuchFileException"))
                skip = true;
            if (!skip)
            {
                //context.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                logger.error(Utils.getStackTrace(e), true);
                int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.delete.file.error") +
                                e.toString() + "\n\n" + context.cfg.gs("NavTransferHandler.continue"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (reply == JOptionPane.NO_OPTION)
                    error = true;
            }
        }
        return error;
    }

}
