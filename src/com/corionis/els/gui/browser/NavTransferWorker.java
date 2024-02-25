package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.Progress;
import com.corionis.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NavTransferWorker extends SwingWorker<Object, Object>
{
    private transient Logger logger = LogManager.getLogger("applog");
    private int action = TransferHandler.NONE;
    private long batchSize = 0L;
    private Context context;
    private int depth = 0;
    private int fileNumber = 0;
    private int filesToCopy = 0;
    private long filesSize = 0L;
    private ArrayList<Batch> queue;
    private Repository sourceRepo;
    private JTree sourceTree;
    private boolean targetIsPublisher = false;
    private Repository targetRepo;
    public JTree targetTree;
    public NavTreeUserObject targetTuo;
    public ArrayList<NavTreeUserObject> transferData;

    private NavTransferWorker()
    {
        // hide default constructor
    }

    public NavTransferWorker(Context context)
    {
        this.context = context;
        queue = new ArrayList<Batch>();
    }

    @Override
    protected Object doInBackground() throws Exception
    {
        depth = 0;
        boolean error = false;

        // create a fresh dialog
        if (context.progress != null && context.progress.isBeingUsed())
        {
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            return false;
        }

        for (int i = 0; i < queue.size(); ++i)
        {
            if (isCancelled())
                break;

            Batch batch = queue.get(i);
            action = batch.action;
            transferData = batch.transferData;
            targetTree = batch.targetTree;
            targetTuo = batch.targetTuo;
            batchSize = batch.batchSize;
            targetIsPublisher = targetTree.getName().toLowerCase().endsWith("one");

            // create a fresh dialog - don't flash it on the screen if the file is too small
            if (batchSize > 524288L && (context.progress == null || !context.progress.isBeingUsed()))
            {
                ActionListener cancelAction = new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (context.browser.navTransferHandler.getTransferWorker() != null &&
                                !context.browser.navTransferHandler.getTransferWorker().isDone())
                        {
                            logger.warn(context.cfg.gs("MainFrame.cancelling.transfers.as.requested"));
                            context.browser.navTransferHandler.getTransferWorker().cancel(true);
                        }
                    }
                };
                context.progress = new Progress(context, context.mainFrame, cancelAction, context.cfg.isDryRun());
                context.progress.display();
            }

            // iterate the selected source row's user object
            for (NavTreeUserObject sourceTuo : transferData)
            {
                if (isCancelled())
                    break;

                NavTreeNode sourceNode = sourceTuo.node;
                sourceTree = sourceNode.getMyTree();

                if (sourceTuo.isDir)
                {
                    if (transferDirectory(sourceTuo, targetTree, targetTuo))
                    {
                        error = true;
                        break;
                    }
                }
                else
                {
                    if (transferFile(sourceTuo, targetTree, targetTuo))
                    {
                        error = true;
                        break;
                    }
                }
            }

            if (!error && !isCancelled())
            {
                if (context.browser.isHintTrackingButtonEnabled())
                    exportHints(transferData, targetTuo);

                removeTransferData(transferData);
            }
            else
            {
                break;
            }
        }
        if (isCancelled())
            logger.warn(context.cfg.gs("NavTransferWorker.cancelled"));
        return null;
    }

    @Override
    protected void done()
    {
        context.mainFrame.labelStatusMiddle.setText(MessageFormat.format(context.cfg.gs("Transfer.of.complete"), filesToCopy));

        // reset the queue
        queue = new ArrayList<Batch>();
        filesToCopy = 0;
        filesSize = 0L;

/*
        if (context.preferences.isAutoRefresh())
        {
            try
            {
                // give other threads a moment to catch-up before refreshing
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            context.browser.refreshTree(sourceTree);
            if (sourceTree != targetTree)
                context.browser.refreshTree(targetTree);
        }
*/

        if (context.progress != null)
        {
            context.progress.done();
            context.progress = null;
        }

    }

    public void add(int action, int count, long size, ArrayList<NavTreeUserObject> transferData, JTree target, NavTreeUserObject tuo)
    {
        Batch batch = new Batch(action, transferData, target, tuo, size);
        queue.add(batch);
        filesToCopy += count;
        filesSize += size;
        logger.info(java.text.MessageFormat.format(context.cfg.gs("NavTransferWorker.added.batch.0.items"), count) +
                Utils.formatLong(size, false, context.cfg.getLongScale()));
    }

    /**
     * Export one or more Hint files to Hint Tracker/Server
     * <br/>
     * Only move operationsUI that impact a collection are tracked with Hints
     *
     * @param transferData
     * @param targetTuo
     * @throws Exception
     */
    private void exportHints(ArrayList<NavTreeUserObject> transferData, NavTreeUserObject targetTuo) throws Exception
    {
        // hints are for moves in the context of DnD/CCP
        // copies to or within a collection are a basic add
        if (action == TransferHandler.MOVE)
        {
            // iterate the selected rows of user objects
            for (NavTreeUserObject sourceTuo : transferData)
            {
                context.browser.navTransferHandler.exportHint("mv", sourceTuo, targetTuo);
            }
        }
    }

    private String makeToPath(NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        String directory = "";
        String filename = "";
        String path = "";
        String sourceSep;
        String targetSep;

        sourceRepo = sourceTuo.getRepo();
        sourceSep = sourceRepo.getSeparator();
        targetRepo = targetTuo.getRepo();
        targetSep = targetRepo.getSeparator();

        // get the directory
        boolean checkSpace = true;
        if (targetTuo.type == NavTreeUserObject.LIBRARY)
        {
            checkSpace = false;
            directory = context.transfer.getTarget(sourceRepo, targetTuo.name, batchSize, targetRepo, targetTuo.isRemote, sourceTuo.path);
            if (directory == null || directory.length() == 0)
            {
                throw new MungeException(MessageFormat.format(context.cfg.gs("Transfer.no.space.on.any.target.location"),
                        targetRepo.getLibraryData().libraries.description, targetTuo.name, Utils.formatLong(batchSize, false, context.cfg.getLongScale())));
            }
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
        assert (directory.length() > 0);

        if (checkSpace && !context.transfer.itFits(targetRepo, directory, targetTuo.isRemote, batchSize, 0, false))
        {
            throw new MungeException(MessageFormat.format(context.cfg.gs("Transfer.no.space.on.any.target.location"),
                    targetRepo.getLibraryData().libraries.description, targetTuo.name, Utils.formatLong(batchSize, false, context.cfg.getLongScale())));
        }

        int dirPos = Utils.rightIndexOf(sourceTuo.path, sourceSep, (targetTuo.isDir ? 0 : depth));
        filename = sourceTuo.path.substring(dirPos);
        filename = Utils.pipe(filename);

        // put them together
        directory = Utils.pipe(directory);
        path = directory + filename;
        path = Utils.unpipe(path, targetSep);
        path = path.replace(":\\\\", ":\\");

        return path;
    }

    private boolean removeItem(NavTreeUserObject sourceTuo)
    {
        boolean error = false;
        if (sourceTuo.isDir)
            error = context.browser.navTransferHandler.removeDirectory(sourceTuo);
        else
            error = context.browser.navTransferHandler.removeFile(sourceTuo);

        if (!error && !context.fault)
        {
            BrowserTableModel btm = (BrowserTableModel) sourceTuo.node.getMyTable().getModel();
            int tableIndex = btm.findNavTreeUserObjectIndex(sourceTuo.node);
            if (tableIndex >= 0)
            {
                JTable table = sourceTuo.node.getMyTable();
                RowSorter sorter = table.getRowSorter();
                sorter.rowsDeleted(tableIndex, tableIndex);
            }

            NavTreeNode parent = (NavTreeNode) sourceTuo.node.getParent();
            btm = (BrowserTableModel) parent.getMyTable().getModel();
            btm.setNode(parent);
            parent.remove(sourceTuo.node);

            if (context.preferences.isAutoRefresh())
            {
                context.browser.refreshTree(sourceTree);
                if (sourceTree != targetTree)
                    context.browser.refreshTree(targetTree);
            }
        }
        return error;
    }

    private void removeTransferData(ArrayList<NavTreeUserObject> transferData)
    {
        if (action == TransferHandler.MOVE)
        {
            boolean error = false;

            // iterate the selected source row's user object
            for (int i = transferData.size() - 1; i > -1; --i)
            {
                NavTreeUserObject sourceTuo = transferData.get(i);
                error = removeItem(sourceTuo);
            }
        }
    }

    private NavTreeUserObject setupToNode(NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo, String path)
    {
        boolean exists = false;
        NavTreeNode toNode = null;

        // setup a node
        toNode = targetTuo.node.findChildTuoPath(path, false);
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

        // setup the user object
        NavTreeUserObject toTuo = toNode.getUserObject();
        toTuo.node = toNode;
        toTuo.path = path;
        toTuo.isRemote = !targetIsPublisher && context.cfg.isRemoteOperation();
        toTuo.file = new File(path);
        if (!toTuo.isRemote || (toTuo.isRemote && !context.cfg.isPreserveDates()))
            toTuo.fileTime = FileTime.fromMillis(toTuo.file.lastModified());
        toNode.setAllowsChildren(toTuo.isDir);

        // add the new node on the target
        if (!exists && !context.cfg.isDryRun())
            targetTuo.node.add(toNode);

        return toTuo;
    }

    private boolean transferDirectory(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        boolean error = false;
        try
        {
            int childCount = sourceTuo.node.getChildCount(false, false);

            String path = makeToPath(sourceTuo, targetTuo);
            NavTreeUserObject toNodeTuo = setupToNode(sourceTuo, targetTuo, path);

            for (int i = 0; i < childCount; ++i)
            {
                if (isCancelled())
                    break;

                NavTreeNode child = (NavTreeNode) sourceTuo.node.getChildAt(i, false, false);
                NavTreeUserObject childTuo = child.getUserObject();
                if (childTuo.isDir)
                {
                    ++depth;
                    if ((error = transferDirectory(childTuo, targetTree, toNodeTuo)) == true)
                    --depth;
                }
                else
                {
                    if ((error = transferFile(childTuo, targetTree, toNodeTuo)) == true)
                        break;
                }
            }
        }
        catch (MungeException me)
        {
            if (!isCancelled() && !error)
            {
                String msg = context.cfg.gs("Browser.error") + me.getMessage();
                logger.error(msg);
                JOptionPane.showMessageDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);

            }
            error = true;
        }
        catch (Exception e)
        {
            if (!isCancelled() && !error)
            {
                logger.error(Utils.getStackTrace(e));
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Browser.error") + e.getMessage(),
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);

            }
            error = true;
        }
        return error;
    }

    private boolean transferFile(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        boolean error = false;
        String path = "";
        String msg = context.browser.navTransferHandler.getOperationText(action,true).toLowerCase() +
                (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "") +
                context.cfg.gs("NavTransferHandler.transfer.file.from") + sourceTuo.path;

        try
        {
            ++fileNumber;
            if (context.progress != null)
            {
                String status = " " + fileNumber + context.cfg.gs("NavTransferHandler.progress.of") + filesToCopy +
                        ", " + Utils.formatLong(sourceTuo.size, false, context.cfg.getLongScale()) + ", " + sourceTuo.name + " ";
                context.progress.update(status);
            }

            path = makeToPath(sourceTuo, targetTuo);
            msg += context.cfg.gs("NavTransferHandler.transfer.file.to") + path;

            // perform the transfer
            if (!sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // local copy
                logger.info(context.cfg.gs("NavTreeNode.local") + msg);
                if (!context.cfg.isDryRun())
                {
                    if (action == TransferHandler.MOVE)
                    {
                        context.transfer.moveFile(sourceTuo.path, sourceTuo.fileTime, path, true);
                    }
                    else
                    {
                        context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, false, true);
                    }
                    setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (!sourceTuo.isRemote && targetTuo.isRemote)
            {
                // put to remote
                logger.info(context.cfg.gs("NavTransferHandler.put") + msg);
                if (!context.cfg.isDryRun())
                {
                    context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, true, false);
                    setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // get from remote
                logger.info(context.cfg.gs("NavTransferHandler.get") + msg);
                if (!context.cfg.isDryRun())
                {
                    String dir = Utils.getLeftPath(path, targetRepo.getSeparator());
                    Files.createDirectories(Paths.get(dir));
                    context.clientSftp.get(sourceTuo.path, path);
                    setupToNode(sourceTuo, targetTuo, path);
                    if (context.preferences.isPreserveFileTimes())
                        Files.setLastModifiedTime(Paths.get(path), sourceTuo.fileTime);
                }
            }
            else if (sourceTuo.isRemote && targetTuo.isRemote)
            {
                // send command to remote
                logger.info(context.cfg.gs("Z.remote.uppercase") + msg);
                if (!context.cfg.isDryRun())
                {
                    String command;
                    if (action == TransferHandler.MOVE)
                    {
                        command = "move \"" + sourceTuo.path + "\" \"" + path + "\"";
                    }
                    else
                    {
                        command = "copy \"" + sourceTuo.path + "\" \"" + path + "\"";
                    }
                    String response = context.clientStty.roundTrip(command, "Sending command: " + command, -1);
                    if (response.equalsIgnoreCase("true"))
                    {
                        setupToNode(sourceTuo, targetTuo, path);
                        if (context.preferences.isPreserveFileTimes())
                            context.clientSftp.setDate(path, (int) sourceTuo.fileTime.to(TimeUnit.SECONDS));
                    }
                    else
                        throw new MungeException(context.cfg.gs("Z.remote.uppercase") + context.browser.navTransferHandler.getOperationText(action,true).toLowerCase() +
                                context.cfg.gs("NavTransferHandler.progress.of") + sourceTuo.name + context.cfg.gs("NavTransferHandler.failed"));
                }
            }
            batchSize = batchSize - sourceTuo.size;

            // update trees with progress so far, do again when done
            if (context.preferences.isAutoRefresh())
            {
                if (action == TransferHandler.MOVE)
                    context.browser.refreshTree(sourceTree);
                if (sourceTree != targetTree)
                    context.browser.refreshTree(targetTree);
            }
        }
        catch (MungeException me)
        {
            if (!isCancelled() && !error)
            {
                msg = context.cfg.gs("Browser.error") + me.getMessage();
                logger.error(msg);
                JOptionPane.showMessageDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);

            }
            error = true;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            if (!isCancelled() && !error)
            {
                int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Browser.error") +
                                context.browser.navTransferHandler.getOperationText(action, true) + ": " + e.toString() + "\n\n" + context.cfg.gs("NavTransferHandler.continue"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (reply == JOptionPane.NO_OPTION)
                    error = true;
            }
            else
                error = true;
        }
        return error;
    }

    // ==========================================
    /**
     * Batch entry for queue
     */
    private class Batch
    {
        public int action;
        public ArrayList<NavTreeUserObject> transferData;
        public JTree targetTree;
        public NavTreeUserObject targetTuo;
        public long batchSize;

        public Batch(int act, ArrayList<NavTreeUserObject> td, JTree target, NavTreeUserObject tuo, long size)
        {
            action = act;
            transferData = td;
            targetTree = target;
            targetTuo = tuo;
            batchSize = size;
        }
    }
}
