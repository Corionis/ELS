package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.bookmarks.Bookmark;
import com.corionis.els.hints.HintKey;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/*
    ### Copy, cut, paste (CCP) and Drag 'n Drop (DnD) implementation
    #---------------------------------------------------------------
    * Uses standards-based x-java-file-list for operations going outside ELS
    * Uses state variables in for internal operations
      - init as null
      - reset at end of importData()
      - reset at end of exportDone()
      - reset on any exception or fault
    #
    ### In createTransferable():
    * Creates state variables for internal use
    * Creates x-java-file-list for external use if the source is not a remote subscriber
    * Local sources
      * Create and return standard Transferable
    * Remote subscriber
      > File is null
      > Has a path
      > isRemote == true
      * FOR NOW, return null so if an outside drop there is nothing to do
        * Later possibly provide a custom DataFlavor, listener and stream type
    #
    ### Going from ELS to ELS:
    * Works using state variables
    #
    ### Going from ELS to outside:
    * Local sources
      * Works in Windows and macOS
      * Linux DnD works
        * Fix Linux CCP
    * Remote subscriber
      * Nothing to do, null
    #
    ### Coming from outside to ELS:
    > All incoming operations are from local
    > No state variables or flavor or action lists do not match
    > Target is known from info
    * Find tuo in publisher System tree to make actionList
    * Works going into both local and remote ELS tabs
    #
    ### Nuances
    * CCP and DnD work inside ELS
    * Going from ELS to outside applications mostly works, see notes
      * Right now there is no way to detect when an outside operation is complete
        * If it was a cut operation the source tab must be refreshed by hand, F5
    * Coming from outside ELS both CCP and DnD work
      * Works with both local publisher and subscriber and with a remote subscriber
    * Linux
      * Going from ELS to outside:
        * DnD out of ELS works
        * CCP out of ELS does not work, yet
      * Going from outside into ELS:
        * Can only copy
          * Cut does not delete from the Linux side
          * There is no way to detect the action inside ELS so copy is the default
    * Windows
      * Going from ELS to outside:
        * Both CCP and DnD work
      * Going from outside into ELS:
        * Both CCP and DnD work
 */

/**
 * Handler for Drag 'n Drop (DnD) and Copy/Cut/Paste (CCP) for internal local/remote and external operations
 */
@SuppressWarnings(value = "unchecked")
public class NavTransferHandler extends TransferHandler
{
    private final boolean traceActions = true; // dev-debug
    private Context context;
    public int fileNumber = 0;
    public int filesToCopy = 0;
    private int action = TransferHandler.NONE;
    private ArrayList<NavTreeUserObject> actionList = null;
    private boolean isDrop = false;
    private boolean isRemote = false;
    private Logger logger = LogManager.getLogger("applog");
    private JTable sourceTable;
    private JTree sourceTree;
    private JTable targetTable;
    private JTree targetTree;
    public static NavTransferWorker transferWorker = null; // singleton
    private boolean transferWorkerRunning = false;

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
        boolean supported = info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        return supported;
    }

    @Override
    protected Transferable createTransferable(JComponent component)
    {
        reset();
        context.fault = false;
        List<File> rowList = new ArrayList<File>(); // for standards-based possible operation outside ELS
        actionList = new ArrayList<NavTreeUserObject>(); // for internal use

        if (component instanceof JTable)
        {
            sourceTable = (JTable) component;
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
                if (sourceTree == null)
                {
                    sourceTree = tuo.node.getMyTree();
                    sourceTable = tuo.node.getMyTable();
                    isRemote = tuo.isRemote;
                }
                actionList.add(tuo); // for internal use
                if (!isRemote)
                {
                    File item = tuo.file;
                    rowList.add(item); // for outside ELS
                }
            }
            if (traceActions)
                logger.trace("Create transferable from " + sourceTable.getName() + " starting at row " + row + ", " + rows.length + " rows total");
        }
        else if (component instanceof JTree)
        {
            sourceTree = (JTree) component;
            sourceTable = getTargetTable(sourceTree);
            int row = sourceTree.getLeadSelectionRow();
            if (row < 0)
                return null;
            TreePath[] paths = sourceTree.getSelectionPaths();
            for (TreePath path : paths)
            {
                NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                NavTreeUserObject tuo = ntn.getUserObject();
                if (sourceTree == null)
                {
                    sourceTree = tuo.node.getMyTree();
                    sourceTable = tuo.node.getMyTable();
                    isRemote = tuo.isRemote;
                }
                actionList.add(tuo); // for internal use
                if (!isRemote)
                {
                    File item = tuo.file;
                    rowList.add(item); // for outside ELS
                }
            }
            if (traceActions)
                logger.trace("Create transferable from " + sourceTree.getName() + " starting at row " + row + ", " + paths.length + " rows total");
        }

        FileTransferable ft = new FileTransferable(rowList);

/*
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        ClipboardOwner owner = new ClipboardOwner()
        {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable transferable)
            {
                logger.debug("lost clipboard ownership");
            }
        };
        clip.setContents(ft, owner);
*/

        return ft;
    }

    @Override
    protected void exportDone(JComponent c, Transferable info, int act)
    {
        action = act;

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
            if (traceActions)
                logger.trace("end of exportDone");
        }
    }

    /**
     * Does the current actionList match the incoming transfer data?
     *
     * @param info TransferHandler.TransferSupport from importDate()
     * @return true if the datasets match, otherwise false
     */
    private boolean datasetsMatch(TransferHandler.TransferSupport info)
    {
        boolean sense = true;
        FileTransferable ftTest = new FileTransferable(null);
        DataFlavor[] flavors = info.getTransferable().getTransferDataFlavors();

        // if the datasets have the same number of flavors, match each entry
        if (ftTest.getTransferDataFlavors().length == flavors.length)
        {
            Transferable data = info.getTransferable();
            boolean typeFound = false;

            for (int index = 0; index < flavors.length; index++)
            {
                String subType = flavors[index].getSubType();
                if (subType.equals("x-java-file-list"))
                {
                    typeFound = true;
                    try
                    {
                        ArrayList fileList = (ArrayList) data.getTransferData(flavors[index]);
                        for (int i = 0; i < fileList.size(); ++i)
                        {
                            File file = (File) fileList.get(i);
                            if (file != null)
                            {
                                if (file != actionList.get(i).file)
                                {
                                    sense = false;
                                    break;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        actionList = null;
                        logger.error(Utils.getStackTrace(e));
                    }
                }
                if (typeFound)
                    break;
            }
            if (!typeFound)
            {
                actionList = null;
                logger.error(context.cfg.gs("NavTransferHandler.unsupported.flavor"));
            }
        }
        else
        {
            sense = false;
        }
        return sense;
    }

    /**
     * Export a Hint to subscriber
     *
     * @param act       Action mv or rm
     * @param sourceTuo Source NavTreeUserObject
     * @param targetTuo Target NavTreeUserObject
     * @throws Exception
     */
    public synchronized void exportHint(String act, NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        if (context.browser.isHintTrackingEnabled() && sourceTuo.node.getMyTree().getName().toLowerCase().contains("collection"))
        {
            String hintPath = context.hints.writeHint(act, context.preferences.isLastPublisherIsWorkstation(), sourceTuo, targetTuo);
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
                    if (!context.preferences.isLastPublisherIsWorkstation() || sourceTuo.isSubscriber())
                    {
                        NavTreeNode node = sourceTuo.getParentLibrary();
                        if (node == null)
                            throw new MungeException("logic fault: cannot find parent library of relevant item");
                        String lib = node.getUserObject().name;
                        String itemPath = sourceTuo.getItemPath(lib, hintPath);
                        HintKey key = context.hintKeys.findKey(sourceTuo.getRepo().getLibraryData().libraries.key);
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

    /**
     * Find a local source NavTreeUserObject (tuo) from a path
     * <br/>
     * The publisher System tree is used for the searches and
     * scanned as needed
     *
     * @param path Fully-qualified path to find
     * @return NavTreeUserObject of the item found, or null
     */
    public NavTreeUserObject findSourceTuo(String path)
    {
        NavTreeUserObject tuo = null;

        String separator = Utils.getSeparatorFromPath(path);
        if (separator.equals("\\"))
            separator = "\\\\";

        String[] pathElements = path.split(separator);
        for (int i = 0; i < pathElements.length; ++i)
        {
            if (pathElements[i] == null || pathElements[i].length() == 0)
                pathElements[i] = separator;
            else if (pathElements[i].matches("^[a-zA-Z]:.*"))
                pathElements[i] = pathElements[i] + "\\";
        }

        if (pathElements != null && pathElements.length > 0)
        {
            JTree searchTree = context.mainFrame.treeSystemOne; // otherwise the system
            String repoName = context.cfg.gs("Browser.system");
            String libName = context.cfg.gs("Browser.computer");

            // use a temporary (unsaved) Bookmark 'Goto Bookmark' method to scan the full tree path using the publisher System tree
            Bookmark bm = context.browser.bookmarkCreate("find-tuo", searchTree, repoName, libName, pathElements);
            TreePath tp = context.browser.scanTreePath(searchTree.getName(), bm.pathElements, false, false, true);
            if (tp != null)
            {
                NavTreeNode ntn = (NavTreeNode) tp.getLastPathComponent();
                tuo = ntn.getUserObject();
            }
        }

        return tuo;
    }

    private String findPathInCollection(JTree currentTree, String[] pathElements)
    {
        String libName = null;
        NavTreeModel model = (NavTreeModel) currentTree.getModel();
        NavTreeNode node = (NavTreeNode) model.getRoot();
        Repository repo = node.getMyRepo();
        if (repo != null && repo.getLibraryData() != null &&
                repo.getLibraryData().libraries != null && repo.getLibraryData().libraries.bibliography != null)
        {
            for (Library lib : repo.getLibraryData().libraries.bibliography)
            {
                for (int i = 0; i < lib.sources.length; ++i)
                {
                    File source = new File(lib.sources[i]);
                    String srcPath = source.getAbsolutePath();
//                    if (matchPathToLibrarySource(pathElements, srcPath))
                    {
                        libName = lib.name;
                        break;
                    }
                }
                if (libName != null)
                    break;
            }
        }
        return libName;
    }

    private boolean matchPathToLibrarySource(String[] pathElements, String libraryPath)
    {
        boolean sense = false;
        String[] libraryElements = libraryPath.split(Utils.getSeparatorFromPath(libraryPath));
        int max = Integer.min(pathElements.length, libraryElements.length);
        for (int i = 0; i < max; )
        {
            if (!pathElements[i].equals(libraryElements[i]))
                break;
            ++i;
            if (i == libraryElements.length) // if the path matches library prefix
            {
                sense = true;
                int size = pathElements.length - i;
                String[] remaining = new String[size];
                for (int j = 0; j < size; ++j)
                {
                    remaining[j] = pathElements[i + j];
                }
                pathElements = remaining;
                break;
            }
        }
        return sense;
    }

    public synchronized String getOperationText(int actionValue, boolean currentTense)
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

        // Drop otherwise Paste
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

    public NavTransferWorker getTransferWorker()
    {
        return transferWorker;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info)
    {
        context.fault = false;
        context.mainFrame.toFront();
        //context.mainFrame.requestFocus();

        isDrop = info.isDrop();
        if (isDrop)
            action = info.getUserDropAction();

        if (action == TransferHandler.NONE)
            action = TransferHandler.COPY;

        // get the target information
        if (info.getComponent() instanceof JTable)
        {
            targetTable = (JTable) info.getComponent();
            targetTree = getTargetTree(targetTable);
            targetTable.requestFocus();
        }
        else
        {
            targetTree = (JTree) info.getComponent();
            targetTable = getTargetTable(targetTree);
            targetTree.requestFocus();
        }

        NavTreeNode targetNode = getTargetNode(info, targetTree, targetTable);
        NavTreeUserObject targetTuo = targetNode.getUserObject();
        if (targetNode.getUserObject().sources == null && targetNode.getUserObject().path.length() == 0)
        {
            reset();
            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.cannot.transfer.to.currently.selected.location"));
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.cannot.transfer.to.currently.selected.location"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try
        {
            fileNumber = 0;
            int count = 0;
            long size = 0L;

            // create actionList of tuo elements if from outside ELS (null or mismatches the existing actionList)
            if (actionList == null || !datasetsMatch(info))
            {
                makeActionListFromPaths(info);
            }

            if (actionList == null || actionList.size() == 0)
            {
                reset();
                logger.warn(context.cfg.gs("NavTransferHandler.nothing.to.do"));
                context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.nothing.to.do"));
                return false;
            }

            // iterate source tuo objects for statistics
            for (NavTreeUserObject sourceTuo : actionList)
            {
                NavTreeNode sourceNode = sourceTuo.node;
                sourceTree = sourceNode.getMyTree();
                sourceTable = sourceNode.getMyTable();

                NavTreeNode parent = (NavTreeNode) sourceNode.getParent();
                if (parent == targetNode)
                {
                    reset();
                    logger.info(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.source.target.are.the.same"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                    reset();
                    logger.warn(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.action.cancelled"));
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.cannot.transfer") + sourceTuo.getType(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            boolean confirm = (isDrop ? context.preferences.isShowDnDConfirmation() : context.preferences.isShowCcpConfirmation());
            if (confirm)
            {
                String msg = MessageFormat.format(context.cfg.gs("NavTransferHandler.are.you.sure.you.want.to"),
                        getOperationText(action, true), Utils.formatLong(size, false, context.cfg.getLongScale()),
                        Utils.formatInteger(count), count > 1 ? 0 : 1, targetTuo.name);
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            // process the batches
            if (reply == JOptionPane.YES_OPTION)
            {
                filesToCopy = count;
                process(action, count, size, actionList, targetTree, targetTuo); // fire NavTransferWorker thread <<<<<<<<<<<<<<<<
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

            boolean indicator = (reply == JOptionPane.YES_OPTION && !context.fault);
            if (traceActions)
                logger.trace("Returning " + indicator);

            reset();
            return indicator;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.mainFrame.labelStatusMiddle.setText("");
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Browser.error") + e.getMessage(),
                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }

        reset();
        return false;
    }

    public boolean isTransferWorkerRunning()
    {
        return isTransferWorkerRunning();
    }

    /**
     * Make the actionList of tuo objects from paths from the transferable data
     * <br/>
     * Used when an operation occurs from outside ELS. Because any outside operation
     * is by definition "local" the publisher System tree is used for the searches.
     *
     * @param info The TransferHandler.TransferSupport from importDate()
     */
    private void makeActionListFromPaths(TransferHandler.TransferSupport info)
    {
        DataFlavor[] flavors = info.getTransferable().getTransferDataFlavors();
        Transferable data = info.getTransferable();
        boolean typeFound = false;
        ArrayList<String> skipped = new ArrayList<>();

        for (int index = 0; index < flavors.length; index++)
        {
            String subType = flavors[index].getSubType();
            if (subType.equals("x-java-file-list"))
            {
                typeFound = true;
                try
                {
                    actionList = new ArrayList<NavTreeUserObject>(); // for internal use
                    List<File> fileList = (List<File>) data.getTransferData(flavors[index]);
                    for (int i = 0; i < fileList.size(); ++i)
                    {
                        File file = (File) fileList.get(i);
                        if (file != null)
                        {
                            // skip Windows system files
                            if (Utils.getOS().toLowerCase().equals("windows"))
                            {
                                Path dfp = Paths.get(file.getPath());
                                DosFileAttributes dattr = Files.readAttributes(dfp, DosFileAttributes.class);
                                if (dattr.isSystem())
                                {
                                    skipped.add(file.getPath());
                                    continue;
                                }
                            }
                            // find the source in the Browser System tab
                            NavTreeUserObject tuo = findSourceTuo(file.getPath());
                            if (tuo != null)
                            {
                                actionList.add(tuo);
                            }
                            else
                                logger.warn(context.cfg.gs("Z.cannot.find") + file.getAbsolutePath());
                        }
                        else
                            logger.warn(context.cfg.gs("NavTransferHandler.empty.element") + i);
                    }
                }
                catch (Exception e)
                {
                    actionList = null;
                    logger.error(Utils.getStackTrace(e));
                }
            }
            if (typeFound)
            {
                // report any skipped items, Windows only; done here after other scans for visibility in log panes
                for (String skip : skipped)
                {
                    logger.warn(context.cfg.gs("NavTransferHandler.skipping.system.item") + skip);
                }
                break;
            }
        }
        if (!typeFound)
        {
            actionList = null;
            logger.error(context.cfg.gs("NavTransferHandler.unsupported.flavor"));
        }
    }

    /**
     * Process the list of NavTreeUserObjects assembled with the singleton NavTransferWorker
     * <br/><br/>
     * The valid target types are:<br/>
     * * REAL local or remote files<br/>
     * * DRIVE and HOME<br/>
     * * LIBRARY using it's sources<br/>
     * <br/>
     * Adds a new Batch to the NavTransferWorker queue and executes it if not running
     *
     * @param transferData
     * @param targetTuo
     */
    private void process(int action, int count, long size, ArrayList<NavTreeUserObject> transferData, JTree targetTree, NavTreeUserObject targetTuo) throws Exception
    {
        // make a new NavTransferWorker
        if (transferWorker == null || transferWorker.isDone())
        {
            transferWorker = null; // suggest clean-up
            transferWorker = new NavTransferWorker(context);
        }
        transferWorker.add(action, count, size, transferData, targetTree, targetTuo);
        if (!transferWorkerRunning)
        {
            transferWorker.execute();
            transferWorkerRunning = true;
        }
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
            logger.error(Utils.getStackTrace(e));
            context.mainFrame.labelStatusMiddle.setText("");
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.delete.directory.error") +
                            e.toString() + "\n\n" + context.cfg.gs("NavTransferHandler.continue"),
                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
            {
                reset();
                error = true;
            }
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
            msg += MessageFormat.format(context.cfg.gs("NavTransferHandler.delete.file.message"), context.cfg.isDryRun() ? 0 : 1, sourceTuo.path);

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
                context.mainFrame.labelStatusMiddle.setText("");
                int reply = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("NavTransferHandler.delete.file.error") +
                                e.toString() + "\n\n" + context.cfg.gs("NavTransferHandler.continue"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (reply == JOptionPane.NO_OPTION)
                {
                    reset();
                    error = true;
                }
            }
        }
        return error;
    }

    private void reset()
    {
        action = TransferHandler.NONE;
        actionList = null;
        isDrop = false;
        isRemote = false;
        sourceTree = null;
        sourceTable = null;
        targetTree = null;
        targetTable = null;
        transferWorkerRunning = false;
    }

    // ==========================================

    public class FileTransferable implements Transferable
    {
        private List listOfFiles;

        public FileTransferable(List listOfFiles)
        {
            this.listOfFiles = listOfFiles;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            if (isDataFlavorSupported(flavor))
                return listOfFiles;
            return null;
        }
    }

}
