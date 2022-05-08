package com.groksoft.els.gui.browser;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
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
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Handler for Drag 'n Drop (DnD) and Copy/Cut/Paste (CCP) for local and/or remote
 */
public class NavTransferHandler extends TransferHandler
{
    private final DataFlavor flavor = new ActivationDataFlavor(ArrayList.class, "application/x-java-object;class=java.util.ArrayList", "ArrayList of NavTreeUserObject");
    private final boolean traceActions = false; // dev-debug
    public int fileNumber = 0;
    public int filesToCopy = 0;
    private int action = TransferHandler.NONE;
    private GuiContext guiContext;
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

    public NavTransferHandler(GuiContext gctxt)
    {
        this.guiContext = gctxt;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        guiContext.context.fault = false;
        guiContext.mainFrame.labelStatusMiddle.setText("");
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
        if (guiContext.browser.trackingHints == true)
        {
            String hintPath = guiContext.context.transfer.writeHint(act, guiContext.preferences.isLastIsWorkstation(), sourceTuo, targetTuo);

            // create a tree node if a new Hint file was created
            if (hintPath.length() > 0)
            {
                if (hintPath.toLowerCase().equals("false"))
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.hint.could.not.be.created"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                else
                {
                    // make tuo and add node
                    NavTreeUserObject createdTuo = null;
                    NavTreeNode createdNode = new NavTreeNode(guiContext, sourceTuo.node.getMyTree());
                    if (sourceTuo.isRemote)
                    {
                        createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(hintPath, null),
                                hintPath, 0, LocalTime.now().toSecondOfDay(), false);
                    }
                    else
                    {
                        createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(hintPath, null), new File(hintPath));
                    }
                    createdNode.setNavTreeUserObject(createdTuo);
                    createdNode.setAllowsChildren(false);
                    if (guiContext.preferences.isHideFilesInTree())
                        createdNode.setVisible(false);
                    else
                        createdNode.setVisible(true);
                    ((NavTreeNode) sourceTuo.node.getParent()).add(createdNode);
                }
            }
        }
    }

    public synchronized String getOperation(int actionValue, boolean currentTense)
    {
        String op = "";
        if (actionValue == TransferHandler.COPY)
        {
            op = (currentTense ? guiContext.cfg.gs("NavTransferHandler.copy") : guiContext.cfg.gs("NavTransferHandler.copied"));
        }
        else if (actionValue == TransferHandler.MOVE)
        {
            op = (currentTense ? guiContext.cfg.gs("NavTransferHandler.move") : guiContext.cfg.gs("NavTransferHandler.moved"));
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
                targetTable = guiContext.mainFrame.tableCollectionOne;
                break;
            case "treeSystemOne":
                targetTable = guiContext.mainFrame.tableSystemOne;
                break;
            case "treeCollectionTwo":
                targetTable = guiContext.mainFrame.tableCollectionTwo;
                break;
            case "treeSystemTwo":
                targetTable = guiContext.mainFrame.tableSystemTwo;
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
                targetTree = guiContext.mainFrame.treeCollectionOne;
                break;
            case "tableSystemOne":
                targetTree = guiContext.mainFrame.treeSystemOne;
                break;
            case "tableCollectionTwo":
                targetTree = guiContext.mainFrame.treeCollectionTwo;
                break;
            case "tableSystemTwo":
                targetTree = guiContext.mainFrame.treeSystemTwo;
                break;
        }
        assert (targetTree != null);
        return targetTree;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        fileNumber = 0;
        guiContext.context.fault = false;

        isDrop = info.isDrop();
        if (isDrop)
        {
            action = info.getUserDropAction();
        }
        if (action == TransferHandler.NONE)
        {
            guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("NavTransferHandler.nothing.to.do"));
            guiContext.browser.printLog(guiContext.cfg.gs("NavTransferHandler.nothing.to.do"));
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
            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.cannot.transfer.to.currently.selected.location"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.source.target.are.the.same"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("NavTransferHandler.action.cancelled"));
                    guiContext.browser.printLog(guiContext.cfg.gs("NavTransferHandler.action.cancelled"));
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.cannot.transfer") + sourceTuo.getType(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("NavTransferHandler.action.cancelled"));
                    guiContext.browser.printLog(guiContext.cfg.gs("NavTransferHandler.action.cancelled"));
                    action = TransferHandler.NONE;
                    return false;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            boolean confirm = (isDrop ? guiContext.preferences.isShowDnDConfirmation() : guiContext.preferences.isShowCcpConfirmation());
            if (confirm)
            {
                String msg = MessageFormat.format(guiContext.cfg.gs("NavTransferHandler.are.you.sure.you.want.to"),
                        getOperation(action,true).toLowerCase(), Utils.formatLong(size, false),
                        Utils.formatInteger(count), count > 1 ? 0 : 1, targetTuo.name);
                msg += (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Browser.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, msg, guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            // process the selections
            if (reply == JOptionPane.YES_OPTION)
            {
                filesToCopy = count;
                process(action, count, size, transferData, targetTree, targetTuo); // fire NavTransferWorker thread <<<<<<<<<<<<<<<<
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
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("Browser.error") + e.toString(),
                    guiContext.cfg.getNavigatorName(), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
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
            transferWorker = new NavTransferWorker(guiContext);
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
                    msg = guiContext.cfg.gs("NavTreeNode.remote");
                else
                    msg = guiContext.cfg.gs("NavTreeNode.local");
                msg += MessageFormat.format(guiContext.cfg.gs("NavTransferHandler.delete.directory.message"), guiContext.cfg.isDryRun() ? 0 : 1, sourceTuo.path);
                guiContext.browser.printLog(msg);

                // remove directory itself
                if (!guiContext.cfg.isDryRun())
                {
                    guiContext.context.transfer.remove(sourceTuo.path, true, sourceTuo.isRemote);
                }
            }
        }
        catch (Exception e)
        {
            //guiContext.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.delete.directory.error") +
                            e.toString() + "\n\n" + guiContext.cfg.gs("NavTransferHandler.continue"),
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
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
                msg = guiContext.cfg.gs("NavTreeNode.remote");
            else
                msg = guiContext.cfg.gs("NavTreeNode.local");
            msg += MessageFormat.format(guiContext.cfg.gs("NavTransferHandler.delete.file.message"), guiContext.cfg.isDryRun() ? 0 : 1,sourceTuo.path);

            if (!guiContext.cfg.isDryRun())
            {
                guiContext.context.transfer.remove(sourceTuo.path, false, sourceTuo.isRemote);
                guiContext.browser.printLog(msg); // not printed if file is missing
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
                //guiContext.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                guiContext.browser.printLog(Utils.getStackTrace(e), true);
                int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, guiContext.cfg.gs("NavTransferHandler.delete.file.error") +
                                e.toString() + "\n\n" + guiContext.cfg.gs("NavTransferHandler.continue"),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (reply == JOptionPane.NO_OPTION)
                    error = true;
            }
        }
        return error;
    }

}
