package com.groksoft.els.gui.browser;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
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

public class NavTreeNode extends DefaultMutableTreeNode
{
    private final boolean traceActions = false; // dev-debug
    public SortFoldersBeforeFiles sortFoldersBeforeFiles;
    public SortTreeAlphabetically sortTreeAlphabetically;
    public GuiContext guiContext;
    private boolean loaded = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JLabel myStatus;
    private JTable myTable;
    private JTree myTree;
    private boolean refresh = true;
    private boolean shown = true;

    private NavTreeNode()
    {
        // hide default constructor
    }

    public NavTreeNode(GuiContext guiContext, JTree tree)
    {
        super();
        this.guiContext = guiContext;
        this.myTree = tree;
        this.allowsChildren = true;
        this.shown = true;

        sortTreeAlphabetically = new SortTreeAlphabetically();
        sortFoldersBeforeFiles = new SortFoldersBeforeFiles();

        if (tree.getName().equalsIgnoreCase("treeCollectionOne"))
        {
            this.myTable = guiContext.form.tableCollectionOne;
            this.myStatus = guiContext.form.labelStatusLeft;
        }
        else if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            this.myTable = guiContext.form.tableSystemOne;
            this.myStatus = guiContext.form.labelStatusLeft;
        }
        else if (tree.getName().equalsIgnoreCase("treeCollectionTwo"))
        {
            this.myTable = guiContext.form.tableCollectionTwo;
            this.myStatus = guiContext.form.labelStatusRight;
        }
        else if (tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            this.myTable = guiContext.form.tableSystemTwo;
            this.myStatus = guiContext.form.labelStatusRight;
        }
    }

    @Override
    public Object clone()
    {
        NavTreeNode object = new NavTreeNode(guiContext, myTree);
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
     * Brute force deep directory tree scan, not lazy-loaded
     */
    public void deepScanChildren()
    {
        NavTreeUserObject myTuo = getUserObject();
        assert (myTuo.type == NavTreeUserObject.REAL);
        List<NavTreeNode> nodeArray = new ArrayList<NavTreeNode>();

        if (myTuo.isDir)
        {
            if (myTuo.isRemote)
            {
                guiContext.form.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.deep.scan.remote.directory") + myTuo.path);
                try
                {
                    Vector listing = guiContext.context.clientSftp.listDirectory(myTuo.path);
                    logger.info(Utils.formatInteger(listing.size()) + guiContext.cfg.gs("NavTreeNode.received.entries.from") + myTuo.path);
                    for (int i = 0; i < listing.size(); ++i)
                    {
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            SftpATTRS a = entry.getAttrs();
                            NavTreeNode node = new NavTreeNode(guiContext, myTree);
                            NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getFilename(),
                                    myTuo.path + guiContext.context.subscriberRepo.getSeparator() + entry.getFilename(),
                                    a.getSize(), a.getATime(), a.isDir());
                            node.setNavTreeUserObject(tuo);
                            nodeArray.add(node);
                            if (!a.isDir())
                                node.setVisible(false);
                            else
                                node.deepScanChildren();
                        }
                    }
                    setChildren(nodeArray, false);
                }
                catch (Exception e)
                {
                    guiContext.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    JOptionPane.showMessageDialog(guiContext.form, guiContext.cfg.gs("NavTreeNode.could.not.retrieve.listing.from") + guiContext.context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    guiContext.context.fault = true;
                    guiContext.navigator.stop();
                }
                guiContext.form.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            else
            {
                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.deep.scan.local.directory") + myTuo.file.getAbsolutePath());
                File[] files = FileSystemView.getFileSystemView().getFiles(myTuo.file.getAbsoluteFile(), false);
                logger.info(Utils.formatInteger(files.length) + guiContext.cfg.gs("NavTreeNode.received.entries.from") + myTuo.file.getAbsoluteFile());
                for (File entry : files)
                {
                    NavTreeNode node = new NavTreeNode(guiContext, myTree);
                    NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getName(), entry);
                    node.setNavTreeUserObject(tuo);
                    nodeArray.add(node);
                    if (!entry.isDirectory())
                        node.setVisible(false);
                    else
                        node.deepScanChildren();
                }
                setChildren(nodeArray, false);
            }
        }
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

            if (hideFilesInTreeFilterActive && guiContext.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && guiContext.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
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

    public NavTreeNode findChildTuoPath(String path)
    {
        NavTreeNode node = null;
        int childCount = getChildCount(false, false);
        for (int i = 0; i < childCount; ++i)
        {
            if (path.equalsIgnoreCase(((NavTreeNode) getChildAt(i, false, false)).getUserObject().path))
            {
                node = (NavTreeNode) getChildAt(i, false, false);
                break;
            }
        }
        return node;
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
            if (hideFilesInTreeFilterActive && guiContext.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && guiContext.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
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

            if (hideFilesInTreeFilterActive && guiContext.preferences.isHideFilesInTree() && !node.isVisible())
                sense = false;
            if (hideHiddenFilterActive && guiContext.preferences.isHideHiddenFiles() && node.getUserObject().isHidden == true)
                sense = false;

            if (sense)
            {
                ++count;
            }
        }
        return count;
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

    public void loadChildren(boolean doLoadTable)
    {
        // return if items exist and refresh is not needed (it's already been scanned), or
        // if a fault occurred (to avoid cascading exceptions)
        if (guiContext.context.fault || !isRefresh())
            return;

        SwingWorker<List<NavTreeNode>, Void> worker = new SwingWorker<List<NavTreeNode>, Void>()
        {
            NavTreeUserObject myTuo;
            List<NavTreeNode> nodeArray;

            @Override
            protected List<NavTreeNode> doInBackground() throws Exception
            {
                nodeArray = new ArrayList<NavTreeNode>();
                myTuo = (NavTreeUserObject) getUserObject();
                switch (myTuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        //logger.debug("bookmarks");
                        break;
                    case NavTreeUserObject.SYSTEM: // for completeness, hidden
                        //logger.debug("system");
                        break;
                    case NavTreeUserObject.COLLECTION:
                        //logger.debug("collection"); // root of collection
                        break;
                    case NavTreeUserObject.COMPUTER: // virtual node, not processed
                        //logger.debug("computer");
                        break;
                    case NavTreeUserObject.DRIVE:
                        if (myTuo.isDir)
                        {
                            if (myTuo.isRemote)
                            {
                                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.remote.drive") + myTuo.path);
                                scanRemote(myTuo.path);
                            }
                            else
                            {
                                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.local.drive") + myTuo.path);
                                scan(new File(myTuo.path).getAbsoluteFile());
                            }
                        }
                        break;
                    case NavTreeUserObject.HOME:
                        File file = new File(myTuo.path);
                        if (file.isDirectory())
                        {
                            guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.home.directory") + myTuo.path);
                            scan(file.getAbsoluteFile());
                        }
                        break;
                    case NavTreeUserObject.LIBRARY:
                        if (myTuo.sources != null && myTuo.sources.length > 0)
                        {
                            for (String path : myTuo.sources)
                            {
                                if (myTuo.isRemote)
                                {
                                    guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.remote.library") + path);
                                    scanRemote(path);
                                }
                                else
                                {
                                    guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.local.library") + path);
                                    scan(new File(path).getAbsoluteFile());
                                }
                            }
                        }
                        break;
                    case NavTreeUserObject.REAL:
                        if (myTuo.isDir)
                        {
                            if (myTuo.isRemote)
                            {
                                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.remote.directory") + myTuo.path);
                                scanRemote(myTuo.path);
                            }
                            else
                            {
                                guiContext.browser.printLog(guiContext.cfg.gs("NavTreeNode.scanning.local.directory") + myTuo.file.getAbsolutePath());
                                scan(myTuo.file.getAbsoluteFile());
                            }
                        }
                        break;
                }
                return nodeArray;
            }

            @Override
            protected void done()
            {
                try
                {
                    if (myTuo.type != NavTreeUserObject.COMPUTER && myTuo.type != NavTreeUserObject.COLLECTION)
                    {
                        setChildren(get(), doLoadTable);
                    }
                    NavTreeModel model = (NavTreeModel) myTree.getModel();
                    model.nodeStructureChanged(NavTreeNode.this);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, guiContext.cfg.gs("NavTreeNode.swing.worker.fault.during.get.of") +
                                    ((NavTreeNode) parent).getTreePath().toString() + ", " +
                                    guiContext.context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    guiContext.context.fault = true;
                    guiContext.navigator.stop();
                }

                if (traceActions)
                    guiContext.browser.printLog(((NavTreeUserObject) getUserObject()).name + " has " +
                            Utils.formatInteger(getChildCount(false, false)) + " node(s)");

                super.done();
            }

            protected void scan(File file)
            {
                if (file.isDirectory())
                {
                    File[] files = FileSystemView.getFileSystemView().getFiles(file, false);
                    logger.info(Utils.formatInteger(files.length) + guiContext.cfg.gs("NavTreeNode.received.entries.from") + file.getAbsolutePath());
                    for (File entry : files)
                    {
                        NavTreeNode node = new NavTreeNode(guiContext, myTree);
                        NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getName(), entry);
                        node.setNavTreeUserObject(tuo);
                        if (!entry.isDirectory())
                            node.setVisible(false);
                        nodeArray.add(node);
                    }
                }
            }

            protected void scanRemote(String target)
            {
                try
                {
                    Vector listing = guiContext.context.clientSftp.listDirectory(target);
                    logger.info(Utils.formatInteger(listing.size()) + guiContext.cfg.gs("NavTreeNode.received.entries.from") + target);
                    for (int i = 0; i < listing.size(); ++i)
                    {
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            SftpATTRS a = entry.getAttrs();
                            NavTreeNode node = new NavTreeNode(guiContext, myTree);
                            NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getFilename(),
                                    target + guiContext.context.subscriberRepo.getSeparator() + entry.getFilename(),
                                    a.getSize(), a.getMTime(), a.isDir());
                            node.setNavTreeUserObject(tuo);
                            if (!a.isDir())
                                node.setVisible(false);
                            nodeArray.add(node);
                        }
                    }
                }
                catch (Exception e)
                {
                    String msg = guiContext.cfg.gs("NavTreeNode.could.not.retrieve.listing.from") + guiContext.context.subscriberRepo.getLibraryData().libraries.description + ": " + target;
                    guiContext.browser.printLog(msg, true);
                    JOptionPane.showMessageDialog(guiContext.form, msg, guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    protected void loadStatus()
    {
        if (myStatus != null)
        {
            int count = getChildCount(false, true);
            myStatus.setText(Utils.formatInteger(count) + (count > 1 ? guiContext.cfg.gs("NavTreeNode.items") : guiContext.cfg.gs("NavTreeNode.item")));
        }
        NavTreeUserObject tuo = getUserObject();
        if (tuo != null)
        {
            guiContext.form.textFieldLocation.setText(tuo.getPath());
            guiContext.browser.printProperties(tuo);
        }
    }

    public void loadTable()
    {
        DefaultTableModel btm = (DefaultTableModel) myTable.getModel();
        DefaultRowSorter sorter = ((DefaultRowSorter) myTable.getRowSorter());

        // remove all & set new data to use
        btm.getDataVector().removeAllElements();
        ((BrowserTableModel) btm).setNode(this);

        // initialize model when there are columns; done once per table
        if (!((BrowserTableModel)btm).isInitialized() && btm.getColumnCount() > 0)
        {
            // restore saved column sizes
            guiContext.preferences.fixColumnSizes(guiContext, myTable);

            // start with sorting by name ascending
            List sortKeys = new ArrayList();
            RowSorter.SortKey sortkey = new RowSorter.SortKey(1, SortOrder.ASCENDING);
            sortKeys.add(sortkey);
            sorter.setSortKeys(sortKeys);

            ((BrowserTableModel) btm).setInitialized(true);
        }

        sorter.sort();
        btm.fireTableDataChanged();

        loadStatus(); // set the left or right status message & properties
    }

    public void selectMe()
    {
        myTree.requestFocus();
        myTree.scrollPathToVisible(getTreePath());
        myTree.setSelectionPath(getTreePath());
    }

    public void selectMyTab()
    {
        int index = 0;
        JTabbedPane tabbedPane = null;
        switch (myTree.getName())
        {
            case "treeCollectionOne":
                tabbedPane = guiContext.form.tabbedPaneBrowserOne;
                break;
            case "treeSystemOne":
                index = 1;
                tabbedPane = guiContext.form.tabbedPaneBrowserOne;
                break;
            case "treeCollectionTwo":
                tabbedPane = guiContext.form.tabbedPaneBrowserTwo;
                break;
            case "treeSystemTwo":
                index = 1;
                tabbedPane = guiContext.form.tabbedPaneBrowserTwo;
        }
        tabbedPane.setSelectedIndex(index);
    }

    protected void setChildren(List<NavTreeNode> children, boolean doLoadTable)
    {
        if (children != null)
        {
            removeAllChildren();
            Collections.sort(children, sortTreeAlphabetically);
            if (guiContext.preferences.isSortFoldersBeforeFiles())
                Collections.sort(children, sortFoldersBeforeFiles);
            setAllowsChildren(getUserObject().isDir);
            for (NavTreeNode ntn : children)
            {
                add(ntn);
            }
        }
        if (doLoadTable)
            loadTable();
        setLoaded(true);
    }

    public void setLoaded(boolean loaded)
    {
        this.loaded = loaded;
        if (this.loaded)
            setRefresh(false);
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
            Collections.sort(children, sortTreeAlphabetically);
            if (guiContext.preferences.isSortFoldersBeforeFiles())
                Collections.sort(children, sortFoldersBeforeFiles);
            for (Object child : children)
            {
                NavTreeNode node = (NavTreeNode) child;
                if (node.getUserObject().isDir)
                    node.sort();
            }
        }
    }

    // ==========================================

    class SortTreeAlphabetically implements Comparator<NavTreeNode>
    {
        public int compare(NavTreeNode a, NavTreeNode b)
        {
            if (guiContext.preferences.isSortReverse())
            {
                if (guiContext.preferences.isSortCaseInsensitive())
                {
                    return b.getUserObject().name.compareToIgnoreCase(a.getUserObject().name);
                }
                return b.getUserObject().name.compareTo(a.getUserObject().name);
            }
            else
            {
                if (guiContext.preferences.isSortCaseInsensitive())
                {
                    return a.getUserObject().name.compareToIgnoreCase(b.getUserObject().name);
                }
                return a.getUserObject().name.compareTo(b.getUserObject().name);
            }
        }
    }

    class SortFoldersBeforeFiles implements Comparator<NavTreeNode>
    {
        public int compare(NavTreeNode a, NavTreeNode b)
        {
            return (a.getUserObject().isDir && !b.getUserObject().isDir) ? -1 : 0;
        }
    }

}
