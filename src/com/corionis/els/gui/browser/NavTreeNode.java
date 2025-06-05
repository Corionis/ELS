package com.corionis.els.gui.browser;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.repository.Repository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * NavTreeNode class is a customized DefaultMutableTreeNode
 */

@SuppressWarnings(value = "unchecked")
public class NavTreeNode extends DefaultMutableTreeNode
{
    private final boolean traceActions = false; // dev-debug
    public Context context;
    public SortFoldersBeforeFiles sortFoldersBeforeFiles;
    public SortTreeAlphabetically sortTreeAlphabetically;
    private boolean forceReload = false;
    private boolean loaded = false;
    private Repository myRepo;
    private JLabel myStatus;
    private JTable myTable;
    private JTree myTree;
    private boolean refresh = true;
    private boolean shown = true;
    private transient Logger logger = LogManager.getLogger("applog");

    private NavTreeNode()
    {
        // hide default constructor
    }

    public NavTreeNode(Context context, Repository repo, JTree tree)
    {
        super();
        this.context = context;
        this.myRepo = repo;
        this.myTree = tree;
        this.allowsChildren = true;
        this.shown = true;

        sortTreeAlphabetically = new SortTreeAlphabetically();
        sortFoldersBeforeFiles = new SortFoldersBeforeFiles();

        if (tree.getName().equalsIgnoreCase("treeCollectionOne"))
        {
            this.myTable = context.mainFrame.tableCollectionOne;
            this.myStatus = context.mainFrame.labelStatusLeft;
        }
        else if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            this.myTable = context.mainFrame.tableSystemOne;
            this.myStatus = context.mainFrame.labelStatusLeft;
        }
        else if (tree.getName().equalsIgnoreCase("treeCollectionTwo"))
        {
            this.myTable = context.mainFrame.tableCollectionTwo;
            this.myStatus = context.mainFrame.labelStatusRight;
        }
        else if (tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            this.myTable = context.mainFrame.tableSystemTwo;
            this.myStatus = context.mainFrame.labelStatusRight;
        }
    }

    public ArrayList<NavTreeUserObject> addChildUserObjectsToList(ArrayList<NavTreeUserObject> list)
    {
        for (int i = 0; i < getChildCount(); ++i)
        {
            NavTreeNode child = (NavTreeNode) getChildAt(i, false, false);
            NavTreeUserObject tuo = child.getUserObject();
            list.add(tuo);
            if (tuo.isDir)
            {
                list = child.addChildUserObjectsToList(list);
            }
        }
        return list;
    }

    @Override
    public Object clone()
    {
        NavTreeNode object = new NavTreeNode(context, myRepo, myTree);
        object.loaded = this.loaded;
        object.refresh = this.refresh;
        object.shown = this.shown;
        NavTreeUserObject tuo = (NavTreeUserObject) this.getUserObject().clone();
        tuo.node = this;
        object.setUserObject(tuo);
        return object;
    }

    public int deepGetFileCount()
    {
        int count = 0;
        int childCount = getChildCount(false, false);
        for (int i = 0; i < childCount; ++i)
        {
            NavTreeNode child = (NavTreeNode) getChildAt(i, false, false);
            NavTreeUserObject tuo = child.getUserObject();
            if (!tuo.isDir)
                ++count;
            else
                count = count + child.deepGetFileCount();
        }
        return count;
    }

    /**
     * Total the file sizes after deepScanChildren()
     *
     * @return Total size of all children
     */
    public long deepGetFileSize()
    {
        long size = 0L;
        int childCount = getChildCount(false, false);
        for (int i = 0; i < childCount; ++i)
        {
            NavTreeNode child = (NavTreeNode) getChildAt(i, false, false);
            NavTreeUserObject tuo = child.getUserObject();
            if (!tuo.isDir)
                size = size + tuo.size;
            else
                size = size + child.deepGetFileSize();
        }
        return size;
    }

    /**
     * Brute force directory tree scan, not lazy-loaded, table is not loaded
     *
     * @param recursive True to recurse subdirectories, false to scan one level only
     */
    public void deepScanChildren(boolean recursive)
    {
        List<NavTreeNode> nodeArray = scan(recursive);
        setChildren(nodeArray, false);
    }

    public int findChildIndex(NavTreeNode find, boolean hideFilesInTreeFilterActive, boolean hideHiddenFilterActive)
    {
        if (children == null)
            return 0;

        int index = -1;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            boolean sense = true;
            NavTreeNode node = (NavTreeNode) e.nextElement();

            if (hideFilesInTreeFilterActive && context.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && context.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
                sense = false;

            if (sense)
            {
                ++index;
                if (node == find)
                    return index;
            }
        }
        return (index > 0 ? index : 0);
    }

    public NavTreeNode findChildName(String name)
    {
        return findChildName(name, 1);
    }

    public NavTreeNode findChildName(String name, int occurrence)
    {
        NavTreeNode ntn = null;
        if (children != null)
        {
            int count = 0;
            Enumeration e = children.elements();
            while (e.hasMoreElements())
            {
                NavTreeNode node = (NavTreeNode) e.nextElement();
                NavTreeUserObject tuo = node.getUserObject();
                boolean sensitive = node.getMyRepo().getLibraryData().libraries.case_sensitive;
                boolean match = (sensitive ? tuo.name.equals(name) : tuo.name.equalsIgnoreCase(name));
                if (match)
                {
                    ++count;
                    if (count == occurrence)
                    {
                        ntn = node;
                        break;
                    }
                }
                else
                {
                    // if count > 0 then one or more were found and
                    // this is past any that might match - avoid an infinite loop
                    if (count > 0)
                        break;
                }
            }
        }
        return ntn;
    }

    public NavTreeNode findChildTuoPath(String path, boolean recursive)
    {
        NavTreeNode value = null;
        int childCount = getChildCount(false, false);
        for (int i = 0; i < childCount; ++i)
        {
            NavTreeNode node = (NavTreeNode) getChildAt(i, false, false);
            if (path.equalsIgnoreCase(node.getUserObject().getPath()))
            {
                value = node;
                break;
            }
            if (recursive && node.getChildCount(false, false) > 0)
            {
                NavTreeNode child = node.findChildTuoPath(path, recursive);
                if (child != null)
                {
                    value = child;
                    break;
                }
            }
        }
        return value;
    }

    public TreeNode getChildAt(int index, boolean hideFilesInTreeFilterActive, boolean hideHiddenFilterActive)
    {
        if (children == null)
            throw new ArrayIndexOutOfBoundsException("node has no children");

        int realIndex = -1;
        int visibleIndex = -1;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            boolean sense = true;
            NavTreeNode node = (NavTreeNode) e.nextElement();
            if (hideFilesInTreeFilterActive && context.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && context.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
                sense = false;

            if (sense)
            {
                visibleIndex++;
            }

            realIndex++;
            if (visibleIndex == index)
            {
                TreeNode child = (TreeNode) children.elementAt(realIndex);
                return child;
            }
        }
        return this;
    }

    public int getChildCount(boolean hideFilesInTreeFilterActive, boolean hideHiddenFilterActive)
    {
        if (children == null)
            return 0;

        int count = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            boolean sense = true;
            NavTreeNode node = (NavTreeNode) e.nextElement();

            if (hideFilesInTreeFilterActive && context.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && context.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
                sense = false;

            if (sense)
            {
                ++count;
            }
        }
        return count;
    }

    public Repository getMyRepo()
    {
        return myRepo;
    }

    public JLabel getMyStatus()
    {
        return myStatus;
    }

    public JTable getMyTable()
    {
        return myTable;
    }

    public JTree getMyTree()
    {
        return myTree;
    }

    public TreePath getTreePath()
    {
        List<Object> nodes = new ArrayList<Object>();
        NavTreeNode treeNode;
        nodes.add(this);
        treeNode = (NavTreeNode) this.getParent();
        while (treeNode != null)
        {
            nodes.add(0, treeNode);
            treeNode = (NavTreeNode) treeNode.getParent();
        }
        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    @Override
    public NavTreeUserObject getUserObject()
    {
        return (NavTreeUserObject) this.userObject;
    }

    public int indexInTable()
    {
        int index = -1;
        if (myTable != null)
        {
            for (int row = 0; row < myTable.getRowCount(); ++row)
            {
                NavTreeUserObject tuo = (NavTreeUserObject) myTable.getValueAt(row, 1);
                if (tuo == getUserObject())
                {
                    index = row;
                    break;
                }
            }
        }
        return index;
    }

    public boolean isForceReload()
    {
        return forceReload;
    }

    @Override
    public boolean isLeaf()
    {
        NavTreeUserObject tuo = (NavTreeUserObject) getUserObject();
        if (tuo != null)
            return !tuo.isDir;
        return false;
    }

    public boolean isLoaded()
    {
        return loaded;
    }

    public boolean isRefresh()
    {
        return refresh;
    }

    public boolean isVisible()
    {
        return shown;
    }

    public SwingWorker<List<NavTreeNode>, Void> loadChildren(boolean doLoadTable)
    {
        // return if items exist and refresh is not needed (it's already been scanned), or
        // if a fault occurred (to avoid cascading exceptions)
        if (context.fault || !isRefresh())
            return null;

        SwingWorker<List<NavTreeNode>, Void> worker = new SwingWorker<List<NavTreeNode>, Void>()
        {
            NavTreeUserObject myTuo;
            List<NavTreeNode> nodeArray;

            @Override
            protected List<NavTreeNode> doInBackground() throws Exception
            {
                myTuo = (NavTreeUserObject) getUserObject();
                nodeArray = scan(false);
                return nodeArray;
            }

            @Override
            protected void done()
            {
                try
                {
                    if (myTuo.type != NavTreeUserObject.COLLECTION && myTuo.type != NavTreeUserObject.SYSTEM)
                    {
                        setChildren(get(), doLoadTable);
                    }
                    NavTreeModel model = (NavTreeModel) myTree.getModel();
                    model.nodeStructureChanged(NavTreeNode.this);

                    myTable.updateUI();
                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("NavTreeNode.swing.worker.fault.during.get.of") +
                                    ((NavTreeNode) parent).getTreePath().toString() + ", " +
                                    context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    context.fault = true;
                    context.navigator.stop();
                }

                if (traceActions)
                    logger.info(((NavTreeUserObject) getUserObject()).name + " has " +
                            Utils.formatInteger(getChildCount(false, false)) + " node(s)");

                super.done();
            }

        };
        worker.execute();
        return worker;
    }

    protected void loadProperties()
    {
        if (myRepo != null)
        {
            if (myStatus != null)
            {
                int count = getChildCount(false, true);
                myStatus.setText(Utils.formatInteger(count) + " " +
                        (myRepo.isPublisher() ? context.cfg.gs("Z.publisher") : context.cfg.gs("Z.subscriber")) +
                        (count > 1 ? context.cfg.gs("NavTreeNode.items") : context.cfg.gs("NavTreeNode.item")));
            }
            NavTreeUserObject tuo = getUserObject();
            if (tuo != null)
            {
                context.mainFrame.textFieldLocation.setText(tuo.getDisplayPath());
                context.browser.propertiesPrint(tuo);
            }
        }
    }

    public void loadTable()
    {
        BrowserTableModel btm = (BrowserTableModel) myTable.getModel();
        btm.setNode(this);

        DefaultRowSorter sorter = ((DefaultRowSorter) myTable.getRowSorter());
        btm.getDataVector().removeAllElements();

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        // initialize model when there are columns; done once per table
        if (!((BrowserTableModel) btm).isInitialized() && btm.getColumnCount() > 0)
        {
            // restore saved column sizes
            context.preferences.fixColumnSizes(context, myTable);

            // start with sorting by name ascending
            List sortKeys = new ArrayList();
            RowSorter.SortKey sortkey = new RowSorter.SortKey(1, SortOrder.ASCENDING);
            sortKeys.add(sortkey);
            sorter.setSortKeys(sortKeys);

            ((BrowserTableModel) btm).setInitialized(true);
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            sorter.sort();
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        else
            // restore saved column sizes
            context.preferences.fixColumnSizes(context, myTable);

        btm.fireTableDataChanged();

        loadProperties(); // set the left or right status message & properties
    }

    protected List<NavTreeNode> scan(boolean recursive)
    {
        NavTreeUserObject myTuo;
        List<NavTreeNode> nodeArray;
        String path = "undefined";

        nodeArray = new ArrayList<NavTreeNode>();
        myTuo = (NavTreeUserObject) getUserObject();

        try
        {
            switch (myTuo.type)
            {
                case NavTreeUserObject.BOOKMARKS:
                    //logger.debug("bookmarks");
                    break;
                case NavTreeUserObject.SYSTEM: // for completeness, hidden
                    //logger.debug("system");
                    break;
                case NavTreeUserObject.COLLECTION: // root of collection
                    //logger.debug("collection");
                    break;
                case NavTreeUserObject.COMPUTER: // expand all local drives
                    //logger.debug("computer");
                    break;
                case NavTreeUserObject.DRIVE: // a particular drive
                    if (myTuo.isDir)
                    {
                        if (myTuo.isRemote)
                        {
                            path = context.cfg.getFullPathSubscriber(myTuo.getPath());
                            logger.info(context.cfg.gs("NavTreeNode.scanning.remote.drive") + path);
                            scanRemote(myTuo, path, nodeArray, recursive);
                        }
                        else
                        {
                            path = Utils.getFullPathLocal(myTuo.getPath());
                            logger.info(context.cfg.gs("NavTreeNode.scanning.local.drive") + path);
                            scanLocal(new File(path), nodeArray, recursive);
                        }
                    }
                    break;
                case NavTreeUserObject.HOME:
                    File file = new File(Utils.getFullPathLocal(myTuo.getPath()));
                    if (file.isDirectory())
                    {
                        path = file.getPath();
                        logger.info(context.cfg.gs("NavTreeNode.scanning.home.directory") + path);
                        scanLocal(file, nodeArray, recursive);
                    }
                    break;
                case NavTreeUserObject.LIBRARY:
                    if (myTuo.sources != null && myTuo.sources.length > 0)
                    {
                        for (String source : myTuo.sources)
                        {
                            if (myTuo.isRemote)
                            {
                                path = context.cfg.getFullPathSubscriber(source);
                                logger.info(context.cfg.gs("NavTreeNode.scanning.remote.library") + path);
                                scanRemote(myTuo, path, nodeArray, recursive);
                            }
                            else
                            {
                                path = Utils.getFullPathLocal(source);
                                logger.info(context.cfg.gs("NavTreeNode.scanning.local.library") + path);
                                scanLocal(new File(path), nodeArray, recursive);
                            }
                        }
                    }
                    break;
                case NavTreeUserObject.REAL:
                    if (myTuo.isDir)
                    {
                        if (myTuo.isRemote)
                        {
                            path = myTuo.getPath();
                            logger.info(context.cfg.gs("NavTreeNode.scanning.remote.directory") + path);
                            scanRemote(myTuo, path, nodeArray, recursive);
                        }
                        else
                        {
                            path = myTuo.file.getPath();
                            logger.info(context.cfg.gs("NavTreeNode.scanning.local.directory") + path);
                            scanLocal(myTuo.file, nodeArray, recursive);
                        }
                    }
                    break;
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("NavTreeNode.could.not.retrieve.listing.from") +
                    context.subscriberRepo.getLibraryData().libraries.description + ", " + path;
            logger.error(msg);
            context.fault = true;
            JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
        return nodeArray;
    }

    protected void scanLocal(File file, List<NavTreeNode> nodeArray, boolean recursive)
    {
        if (file.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(file, (Utils.isOsMac() ? true : false));
            logger.info(Utils.formatInteger(files.length) + context.cfg.gs("NavTreeNode.received.entries.from") + file.getPath());
            for (File entry : files)
            {
                NavTreeNode node = new NavTreeNode(context, myRepo, myTree);
                NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getName(), file.getPath(), entry);
                node.setNavTreeUserObject(tuo);
                if (!entry.isDirectory())
                    node.setVisible(false);
                else
                {
                    if (recursive)
                    {
                        node.deepScanChildren(recursive);
                        node.setLoaded(true);
                    }
                }
                nodeArray.add(node);
            }
        }
    }

    protected void scanRemote(NavTreeUserObject myTuo, String directory, List<NavTreeNode> nodeArray, boolean recursive) throws Exception
    {
        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Vector listing = context.clientSftp.listDirectory(directory);

        if (directory.matches("^\\\\[a-zA-Z]:.*") || directory.matches("^/[a-zA-Z]:.*"))
            directory = directory.substring(1);

        if (directory.endsWith("/") || directory.endsWith("\\"))
            directory = directory.substring(0, directory.length() - 1);

        int count = 0;
        for (int i = 0; i < listing.size(); ++i)
        {
            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
            if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
            {
                // exclude certain DOS/Windows "system" items; TODO EXTEND+ Adjust excluded Windows items as necessary
                String longname = entry.getLongname();
                if (longname.matches("(?i).*OWNER\\@.*GROUP\\@.*") ||
                        (longname.matches("(?i).*Administrators.*BUILTIN.*")) ||
                        (longname.matches("(?i).*NTUSER\\.DAT.*")) ||
                        (longname.matches("(?i).*TrustedInstaller.*NT SERVICE.*") &&
                                !(entry.getFilename().toLowerCase().startsWith("program files") || entry.getFilename().toLowerCase().equals("windows"))) ||
                        entry.getFilename().equalsIgnoreCase("$Recycle.Bin") ||
                        entry.getFilename().equalsIgnoreCase("$SysReset") ||
                        entry.getFilename().equalsIgnoreCase("$WINRE_BACKUP_PARTITION.MARKER") ||
                        entry.getFilename().equals("BOOTNXT") ||
                        entry.getFilename().equals("Documents and Settings") ||
                        entry.getFilename().equals("DumpStack.log.tmp") ||
                        entry.getFilename().equals("pagefile.sys") ||
                        entry.getFilename().equals("PerfLogs") ||
                        entry.getFilename().equals("ProgramData") ||
                        entry.getFilename().equals("swapfile.sys") ||
                        entry.getFilename().equals("System Volume Information") ||
                        entry.getFilename().equals(".DS_Store" ))
                    continue;

                String path = Utils.pipe(directory + context.subscriberRepo.getSeparator() + entry.getFilename());
                path = Utils.unpipe(myRepo, path);
                SftpATTRS attr = entry.getAttrs();
                NavTreeNode node = new NavTreeNode(context, myRepo, myTree);
                NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getFilename(), path, attr.getSize(), attr.getMTime(), attr.isDir());
                node.setNavTreeUserObject(tuo);
                if (!attr.isDir())
                    node.setVisible(false);
                else
                {
                    if (recursive)
                    {
                        node.deepScanChildren(recursive);
                        node.setLoaded(true);
                    }
                }
                nodeArray.add(node);
                ++count;
            }
        }
        logger.info(Utils.formatInteger(count) + context.cfg.gs("NavTreeNode.received.entries.from") + myTuo.getDisplayPath());
        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void selectMe()
    {
        myTree.requestFocus();
        myTree.setExpandsSelectedPaths(true);
        myTree.setSelectionPath(getTreePath());
        myTree.scrollPathToVisible(getTreePath());
    }

    public void selectMyTab()
    {
        int index = 0;
        JTabbedPane tabbedPane = null;
        switch (myTree.getName())
        {
            case "treeCollectionOne":
                tabbedPane = context.mainFrame.tabbedPaneBrowserOne;
                break;
            case "treeSystemOne":
                index = 1;
                tabbedPane = context.mainFrame.tabbedPaneBrowserOne;
                break;
            case "treeCollectionTwo":
                tabbedPane = context.mainFrame.tabbedPaneBrowserTwo;
                break;
            case "treeSystemTwo":
                index = 1;
                tabbedPane = context.mainFrame.tabbedPaneBrowserTwo;
        }
        tabbedPane.setSelectedIndex(index);
    }

    protected void setChildren(List<NavTreeNode> children, boolean doLoadTable)
    {
        if (children != null)
        {
            removeAllChildren();
            Collections.sort(children, sortTreeAlphabetically);
            if (context.preferences.isSortFoldersBeforeFiles())
                Collections.sort(children, sortFoldersBeforeFiles);
            setAllowsChildren(getUserObject().isDir);
            for (NavTreeNode ntn : children)
            {
                if (!ntn.getUserObject().isDir)
                    ntn.setLoaded(true);
                add(ntn);
            }
        }
        setLoaded(true);
        if (doLoadTable)
            loadTable();
    }

    public void setForceReload(boolean forceReload)
    {
        this.forceReload = forceReload;
    }

    public void setLoaded(boolean loaded)
    {
        this.loaded = loaded;
        if (this.loaded)
        {
            setForceReload(false);
            setRefresh(false);
        }
    }

    public void setMyRepo(Repository myRepo)
    {
        this.myRepo = myRepo;
    }

    public void setMyStatus(JLabel myStatus)
    {
        this.myStatus = myStatus;
    }

    public void setMyTable(JTable myTable)
    {
        this.myTable = myTable;
    }

    public void setMyTree(JTree myTree)
    {
        this.myTree = myTree;
    }

    public void setNavTreeUserObject(NavTreeUserObject ntuo)
    {
        this.setUserObject(ntuo);
    }

    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }

    public void setVisible(boolean visible)
    {
        this.shown = visible;
    }

    public void sort()
    {
        if (children != null)
        {
            children.sort(sortTreeAlphabetically);
            //Collections.sort(children, sortTreeAlphabetically);

            if (context.preferences.isSortFoldersBeforeFiles())
            {
                children.sort(sortFoldersBeforeFiles);
                //Collections.sort(children, sortFoldersBeforeFiles);
            }

            for (Object child : children)
            {
                NavTreeNode node = (NavTreeNode) child;
                if (node.getUserObject().isDir)
                    node.sort();
            }
        }
    }

    // ==========================================

    class SortTreeAlphabetically implements Comparator<Object>
    {
        public int compare(Object a, Object b)
        {
            if (context.preferences.isSortReverse())
            {
                if (context.preferences.isSortCaseInsensitive())
                {
                    return ((NavTreeNode) b).getUserObject().name.compareToIgnoreCase(((NavTreeNode) a).getUserObject().name);
                }
                return ((NavTreeNode) b).getUserObject().name.compareTo(((NavTreeNode) a).getUserObject().name);
            }
            else
            {
                if (context.preferences.isSortCaseInsensitive())
                {
                    return ((NavTreeNode) a).getUserObject().name.compareToIgnoreCase(((NavTreeNode) b).getUserObject().name);
                }
                return ((NavTreeNode) a).getUserObject().name.compareTo(((NavTreeNode) b).getUserObject().name);
            }
        }
    }

    class SortFoldersBeforeFiles implements Comparator<Object>
    {
        public int compare(Object a, Object b)
        {
            return (((NavTreeNode) a).getUserObject().isDir && !((NavTreeNode) b).getUserObject().isDir) ? -1 : 0;
        }
    }

}
