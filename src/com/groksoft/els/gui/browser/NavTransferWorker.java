package com.groksoft.els.gui.browser;

import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.Progress;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NavTransferWorker extends SwingWorker<Object, Object>
{
    private transient Logger logger = LogManager.getLogger("applog");
    private int action = TransferHandler.NONE;
    private int depth = 0;
    private int fileNumber = 0;
    private int filesToCopy = 0;
    private long filesSize = 0L;
    private GuiContext guiContext;
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

    public NavTransferWorker(GuiContext gct)
    {
        guiContext = gct;
        queue = new ArrayList<Batch>();
    }

    @Override
    protected Object doInBackground() throws Exception
    {
        depth = 0;
        boolean error = false;

        guiContext.progress.display();

        for (int i = 0; i < queue.size(); ++i)
        {
            if (isCancelled())
                break;

            Batch batch = queue.get(i);
            action = batch.action;
            transferData = batch.transferData;
            targetTree = batch.targetTree;
            targetTuo = batch.targetTuo;
            targetIsPublisher = targetTree.getName().toLowerCase().endsWith("one");

            // iterate the selected source row's user object
            for (NavTreeUserObject sourceTuo : transferData)
            {
                if (isCancelled())
                    break;

                NavTreeNode sourceNode = sourceTuo.node;
                sourceTree = sourceNode.getMyTree();
                //sourceTable = sourceNode.getMyTable();

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
                exportHints(transferData, targetTuo);
                removeTransferData(transferData);
            }
            else
            {
                break;
            }
        }
        if (isCancelled())
            logger.warn(guiContext.cfg.gs("NavTransferWorker.cancelled"));
        return null;
    }

    @Override
    protected void done()
    {
        guiContext.progress.done();
        guiContext.form.labelStatusMiddle.setText(MessageFormat.format(guiContext.cfg.gs("Transfer.of.complete"), filesToCopy));

        // reset the queue
        queue = new ArrayList<Batch>();
        filesToCopy = 0;
    }

    public void add(int action, int count, long size, ArrayList<NavTreeUserObject> transferData, JTree target, NavTreeUserObject tuo)
    {
        Batch batch = new Batch(action, transferData, target, tuo);
        queue.add(batch);
        filesToCopy += count;
        filesSize += size;

        // create a fresh dialog here so it exists to be updated with new stats
        if (guiContext.progress == null || !guiContext.progress.isActive())
        {
            guiContext.progress = null; // suggest clean-up
            guiContext.progress = new Progress(guiContext);
        }

        if (guiContext.progress.isVisible()) // can be minimized
            guiContext.progress.toFront();
        guiContext.progress.update(filesToCopy, filesSize);
    }

    /**
     * Export one or more Hint files to subscriber
     * <br/>
     * Only move operations that impact a media server collection are tracked with Hints
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
                guiContext.browser.navTransferHandler.exportHint("mv", sourceTuo, targetTuo);
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

        sourceRepo = guiContext.context.transfer.getRepo(sourceTuo);
        sourceSep = sourceRepo.getSeparator();
        targetRepo = guiContext.context.transfer.getRepo(targetTuo);
        targetSep = targetRepo.getSeparator();

        // get the directory
        if (targetTuo.type == NavTreeUserObject.LIBRARY)
        {
            directory = guiContext.context.transfer.getTarget(sourceRepo, targetTuo.name, sourceTuo.size, targetRepo, targetTuo.isRemote, sourceTuo.path);
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

    private boolean removeItem(NavTreeUserObject sourceTuo)
    {
        boolean error = false;
        if (sourceTuo.isDir)
            error = guiContext.browser.navTransferHandler.removeDirectory(sourceTuo);
        else
            error = guiContext.browser.navTransferHandler.removeFile(sourceTuo);

        if (!error && !guiContext.context.fault)
        {
            NavTreeNode parent = (NavTreeNode) sourceTuo.node.getParent();
            parent.remove(sourceTuo.node);

            guiContext.browser.refreshTree(sourceTree);
            guiContext.browser.refreshTree(targetTree);
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

        // setup the user object
        NavTreeUserObject toTuo = toNode.getUserObject();
        toTuo.node = toNode;
        toTuo.path = path;
        toTuo.isRemote = !targetIsPublisher && guiContext.cfg.isRemoteSession();
        toTuo.file = new File(path);
        toNode.setAllowsChildren(toTuo.isDir);

        // add the new node on the target
        if (!exists && !guiContext.cfg.isDryRun())
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
        catch (Exception e)
        {
            if (!isCancelled())
            {
                guiContext.browser.printLog(Utils.getStackTrace(e), true);
                JOptionPane.showConfirmDialog(guiContext.form, guiContext.cfg.gs("Browser.error") + e,
                        guiContext.cfg.getNavigatorName(), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
            }
            error = true;
        }
        return error;
    }

    private boolean transferFile(NavTreeUserObject sourceTuo, JTree targetTree, NavTreeUserObject targetTuo)
    {
        boolean error = false;
        String path = "";
        String msg = guiContext.browser.navTransferHandler.getOperation(action,true).toLowerCase() +
                (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Browser.dry.run") : "") +
                guiContext.cfg.gs("NavTransferHandler.transfer.file.from") + sourceTuo.path;

        try
        {
            ++fileNumber;
            guiContext.progress.update(fileNumber, sourceTuo.size, sourceTuo.name);

            path = makeToPath(sourceTuo, targetTuo);
            msg += guiContext.cfg.gs("NavTransferHandler.transfer.file.to") + path;

            // perform the transfer
            if (!sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // local copy
                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.local") + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    if (action == TransferHandler.MOVE)
                    {
                        guiContext.context.transfer.moveFile(sourceTuo.path, sourceTuo.fileTime, path, true);
                    }
                    else
                    {
                        guiContext.context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, false, true);
                    }
                    setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (!sourceTuo.isRemote && targetTuo.isRemote)
            {
                // put to remote
                guiContext.browser.printLog(guiContext.cfg.gs("NavTransferHandler.put") + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    guiContext.context.transfer.copyFile(sourceTuo.path, sourceTuo.fileTime, path, true, false);
                    setupToNode(sourceTuo, targetTuo, path);
                }
            }
            else if (sourceTuo.isRemote && !targetTuo.isRemote)
            {
                // get from remote
                guiContext.browser.printLog(guiContext.cfg.gs("NavTransferHandler.get") + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    String dir = Utils.getLeftPath(path, targetRepo.getSeparator());
                    Files.createDirectories(Paths.get(dir));
                    guiContext.context.clientSftp.get(sourceTuo.path, path);
                    setupToNode(sourceTuo, targetTuo, path);
                    if (guiContext.preferences.isPreserveFileTimes())
                        Files.setLastModifiedTime(Paths.get(path), sourceTuo.fileTime);
                }
            }
            else if (sourceTuo.isRemote && targetTuo.isRemote)
            {
                // send command to remote
                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.remote") + msg);
                if (!guiContext.cfg.isDryRun())
                {
                    String dir = Utils.getLeftPath(path, targetRepo.getSeparator());
                    Files.createDirectories(Paths.get(dir));
                    String command;
                    if (action == TransferHandler.MOVE)
                    {
                        command = "move \"" + sourceTuo.path + "\" \"" + path + "\"";
                    }
                    else
                    {
                        command = "copy \"" + sourceTuo.path + "\" \"" + path + "\"";
                    }
                    String response = guiContext.context.clientStty.roundTrip(command);
                    if (response.equalsIgnoreCase("true"))
                    {
                        setupToNode(sourceTuo, targetTuo, path);
                        if (guiContext.preferences.isPreserveFileTimes())
                            guiContext.context.clientSftp.setDate(path, (int) sourceTuo.fileTime.to(TimeUnit.SECONDS));
                    }
                    else
                        throw new MungeException(guiContext.cfg.gs("NavTreeNode.remote") + guiContext.browser.navTransferHandler.getOperation(action,true).toLowerCase() +
                                guiContext.cfg.gs("NavTransferHandler.progress.of") + sourceTuo.name + guiContext.cfg.gs("NavTransferHandler.failed"));
                }
            }

            // update trees with progress so far, do again in when done
            guiContext.browser.refreshTree(sourceTree);
            guiContext.browser.refreshTree(targetTree);
        }
        catch (Exception e)
        {
            guiContext.browser.printLog(Utils.getStackTrace(e), true);
            if (!isCancelled())
            {
                int reply = JOptionPane.showConfirmDialog(guiContext.form, guiContext.cfg.gs("Browser.error") +
                                guiContext.browser.navTransferHandler.getOperation(action, true) + ": " + e.toString() + "\n\n" + guiContext.cfg.gs("NavTransferHandler.continue"),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
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

        public Batch(int act, ArrayList<NavTreeUserObject> td, JTree target, NavTreeUserObject tuo)
        {
            action = act;
            transferData = td;
            targetTree = target;
            targetTuo = tuo;
        }
    }
}
