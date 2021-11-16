package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * Handler for Drag 'n Drop and Copy/Cut/Paste
 */
public class NavTransferHandler extends TransferHandler
{
    private final DataFlavor flavor = new ActivationDataFlavor(Integer.class, "application/x-java-Integer;class=java.lang.Integer", "Integer row index");
    private int action = TransferHandler.NONE;
    private JTable fromTable;
    private GuiContext guiContext;
    private boolean isDrop = false;
    private transient Logger logger = LogManager.getLogger("applog");

    public NavTransferHandler(GuiContext gctxt)
    {
        guiContext = gctxt;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info)
    {
        if (info.getComponent() instanceof JTable)
        {
            JTable targetTable = (JTable) info.getComponent();
            JTree targetTree = getTargetTree(targetTable);
            NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
            if (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0)
                return false;
        }
        return (info.isDataFlavorSupported(flavor));
    }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
        fromTable = (JTable) c;
        int row = fromTable.getSelectedRow();
        if (row < 0)
            return null;
        guiContext.browser.printLog("Create " + fromTable.getName() + " at row " + row);
        return new DataHandler(new Integer(row), flavor.getMimeType());
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

    private String getOperation()
    {
        String op = "";
        if (action == TransferHandler.COPY)
        {
            op = "Copy";
        }
        else if (action == TransferHandler.MOVE)
        {
            op = "Move";
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
            JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
            if (!dl.isInsertRow())
            {
                // use the dropped-on node in the target table if it is a directory
                NavTreeUserObject ttuo = (NavTreeUserObject) targetTable.getValueAt(dl.getRow(), 1);
                if (ttuo.isDir)
                    targetNode = ((NavTreeUserObject) targetTable.getValueAt(dl.getRow(), 1)).node;
            }
        }

        // use the selected node in the target tree
        if (targetNode == null)
            targetNode = (NavTreeNode) targetTree.getLastSelectedPathComponent();

        // use the root node in the target tree
        if (targetNode == null)
            targetNode = (NavTreeNode) targetTree.getModel().getRoot();

        return targetNode;
    }

    private JTree getTargetTree(JTable table)
    {
        JTree targetTree = null;
        switch (table.getName())
        {
            case "tableCollectionOne":
                targetTree = guiContext.form.treeCollectionOne;
                break;
            case "tableSystemOne":
                targetTree = guiContext.form.treeSystemOne;
                break;
            case "tableCollectionTwo":
                targetTree = guiContext.form.treeCollectionTwo;
                break;
            case "tableSystemTwo":
                targetTree = guiContext.form.treeSystemTwo;
                break;
        }
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
            guiContext.browser.printLog("Nothing to do");
            isDrop = false;
            return false;
        }

        JTable targetTable = (JTable) info.getComponent();
        JTree targetTree = getTargetTree(targetTable);
        NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
        NavTreeUserObject targetTuo = targetNode.getUserObject();

        String op = getOperation();
        guiContext.browser.printLog(op + " to " + targetTable.getName() + " of " + targetTree.getName() + ", node " + targetNode.getUserObject().name);

        try
        {
            int count = 0;
            // get the first transferable row
            Integer from = (Integer) info.getTransferable().getTransferData(flavor);
            if (from != -1)
            {
                // get the the selected "from" rows
                int[] rows = fromTable.getSelectedRows();
                int iter = 0;
                for (int row : rows)
                {
                    ++count;
                    NavTreeUserObject fromTuo = (NavTreeUserObject) fromTable.getValueAt(row, 1);
                    NavTreeNode fromNode = fromTuo.node;
                    JTree fromTree = fromNode.getMyTree();

                    NavTreeNode parent = (NavTreeNode) fromNode.getParent();
                    if (parent == targetNode)
                    {
                        guiContext.browser.printLog("Source & target are the same, skipping operation");
                        action = TransferHandler.NONE;
                        guiContext.form.labelStatusMiddle.setText("Skipped operation");
                        return false;
                    }
                    guiContext.browser.printLog("From " + fromTable.getName() + " of " + fromTree.getName() + ", node " + fromTuo.name);

                   // if (action == TransferHandler.COPY)
                    {
                        if (fromTuo.type == NavTreeUserObject.REAL && targetTuo.type == NavTreeUserObject.REAL)
                        {
                            // local copy
                            guiContext.browser.printLog("Local copy");
                        }
                        else if (fromTuo.type == NavTreeUserObject.REAL && targetTuo.type == NavTreeUserObject.REMOTE)
                        {
                            // put
                            guiContext.browser.printLog("Put");
                        }
                        else if (fromTuo.type == NavTreeUserObject.REMOTE && targetTuo.type == NavTreeUserObject.REAL)
                        {
                            // get
                            guiContext.browser.printLog("Get");
                        }
                        else if (fromTuo.type == NavTreeUserObject.REMOTE && targetTuo.type == NavTreeUserObject.REMOTE)
                        {
                            // send command
                            guiContext.browser.printLog("Remote command");
                        }
                    }
                //    else if (action == TransferHandler.MOVE)
                    {
                    }

//                    if (action == TransferHandler.MOVE && parent != null)
//                    {
//                        parent.remove(node);
//                    }
//                    target.getSelectionModel().addSelectionInterval(index, index);
                }
                guiContext.form.labelStatusMiddle.setText(getOperation() + " " + count + " items");
                action = TransferHandler.NONE;
                return true;
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        guiContext.form.labelStatusMiddle.setText("");
        action = TransferHandler.NONE;
        return false;
    }

}
