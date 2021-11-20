package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.*;

/**
 * NavTreeNode class is a customized DefaultMutableTreeNode
 */

class NavTreeNode extends DefaultMutableTreeNode
{
    SortFoldersBeforeFiles sortFoldersBeforeFiles;
    SortTreeAlphabetically sortTreeAlphabetically;
    private GuiContext guiContext;
    private boolean loaded = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JLabel myStatus;
    private JTable myTable;
    private JTree myTree;
    private boolean refresh = true;
    private boolean visible = true;

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
        this.visible = true;

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
        assert(myTuo.type == NavTreeUserObject.REAL);
        List<NavTreeNode> nodeArray = new ArrayList<NavTreeNode>();

        if (myTuo.isDir)
        {
            if (myTuo.isRemote)
            {
                guiContext.browser.printLog("Deep scan remote directory " + myTuo.path);
                try
                {
                    Vector listing = guiContext.context.clientSftp.listDirectory(myTuo.path);
                    logger.info("received " + listing.size() + " entries from " + myTuo.path);
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
                    JOptionPane.showMessageDialog(guiContext.form, "Could not retrieve listing from " + guiContext.context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    guiContext.context.fault = true;
                    guiContext.navigator.stop();
                }
            }
            else
            {
                guiContext.browser.printLog("Deep scan local directory " + myTuo.file.getAbsolutePath());
                File[] files = guiContext.fileSystemView.getFiles(myTuo.file.getAbsoluteFile(), false);
                logger.info("found " + files.length + " entries from " + myTuo.file.getAbsoluteFile());
                for (File entry : files)
                {
                    NavTreeNode node = new NavTreeNode(guiContext, myTree);
                    NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getName(), entry);
                    node.setNavTreeUserObject(tuo);
                    nodeArray.add(node);
                    if (!entry.isDirectory())  // TODO add hidden condition
                        node.setVisible(false);
                    else
                        node.deepScanChildren();
                }
                setChildren(nodeArray, false);
            }
        }
    }

    public TreeNode getChildAt(int index, boolean filterIsActive, boolean visibleIsActive)
    {
//        if (!filterIsActive)
//        {
//            return super.getChildAt(index);
//        }

        if (children == null)
        {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }

        int realIndex = -1;
        int visibleIndex = -1;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            NavTreeNode node = (NavTreeNode) e.nextElement();
            if ((!filterIsActive || (filterIsActive && node.isVisible())) && (!visibleIsActive || guiContext.preferences.isViewHidden() || node.getUserObject().isHidden == false))
            {
                visibleIndex++;
            }

            realIndex++;
            if (visibleIndex == index)
            {
                return (TreeNode) children.elementAt(realIndex);
            }
        }
        throw new ArrayIndexOutOfBoundsException("index unmatched");
    }

    public int getChildCount(boolean filterIsActive, boolean visibleIsActive)
    {
//        if (!filterIsActive)
//        {
//            return super.getChildCount();
//        }

        if (children == null)
        {
            return 0;
        }

        int count = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            NavTreeNode node = (NavTreeNode) e.nextElement();
//            if (node.isVisible())
            if ((!filterIsActive || (filterIsActive && node.isVisible())) && (!visibleIsActive || guiContext.preferences.isViewHidden() || node.getUserObject().isHidden == false))
            {
                count++;
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
        return visible;
    }

    public void loadChildren(boolean doLoadTable)
    {
        // return if items exist and refresh is not needed (it's already been scanned), or
        // if a fault occurred (to avoid cascading exceptions)
        if (guiContext.context.fault || !isRefresh())
            return;

        //progressBar.setVisible(true);
        //progressBar.setIndeterminate(true);

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
                        logger.debug("bookmarks");
                        break;
                    case NavTreeUserObject.SYSTEM: // for completeness, hidden
                        logger.debug("system");
                        break;
                    case NavTreeUserObject.COLLECTION:
                        logger.debug("collection"); // root of collection
                        break;
                    case NavTreeUserObject.COMPUTER: // virtual node, not processed
                        logger.debug("computer");
                        break;
                    case NavTreeUserObject.DRIVE:
                        if (myTuo.isDir)
                        {
                            if (myTuo.isRemote)
                            {
                                guiContext.browser.printLog("Scanning remote directory " + myTuo.path);
                                scanRemote(myTuo.path);
                            }
                            else
                            {
                                guiContext.browser.printLog("Scanning local drive " + myTuo.path);
                                scan(new File(myTuo.path).getAbsoluteFile());
                            }
                        }
                        break;
                    case NavTreeUserObject.HOME:
                        File file = new File(myTuo.path);
                        if (file.isDirectory())
                        {
                            guiContext.browser.printLog("Scanning home directory " + myTuo.path);
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
                                    guiContext.browser.printLog("Scanning remote library " + path);
                                    scanRemote(path);
                                }
                                else
                                {
                                    guiContext.browser.printLog("Scanning local library " + path);
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
                                guiContext.browser.printLog("Scanning remote directory " + myTuo.path);
                                scanRemote(myTuo.path);
                            }
                            else
                            {
                                guiContext.browser.printLog("Scanning local directory " + myTuo.file.getAbsolutePath());
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
                    JOptionPane.showMessageDialog(guiContext.form, "Swing worker fault during get of " + ((NavTreeNode) parent).getTreePath().toString() + ", " + guiContext.context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    guiContext.context.fault = true;
                    guiContext.navigator.stop();
                }

                guiContext.browser.printLog(((NavTreeUserObject) getUserObject()).name + " has " + getChildCount(false, false) + " node(s)");
                super.done();
            }

            protected void scan(File file)
            {
                if (file.isDirectory())
                {
                    File[] files = guiContext.fileSystemView.getFiles(file, false);
                    logger.info("found " + files.length + " entries from " + file.getAbsolutePath());
                    for (File entry : files)
                    {
                        NavTreeNode node = new NavTreeNode(guiContext, myTree);
                        NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getName(), entry);
                        node.setNavTreeUserObject(tuo);
                        if (!entry.isDirectory() || (!guiContext.preferences.isViewHidden() && tuo.isHidden))
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
                    logger.info("received " + listing.size() + " entries from " + target);
                    for (int i = 0; i < listing.size(); ++i)
                    {
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            SftpATTRS a = entry.getAttrs();
                            NavTreeNode node = new NavTreeNode(guiContext, myTree);
                            NavTreeUserObject tuo = new NavTreeUserObject(node, entry.getFilename(),
                                    target + guiContext.context.subscriberRepo.getSeparator() + entry.getFilename(),
                                    a.getSize(), a.getATime(), a.isDir());
                            node.setNavTreeUserObject(tuo);
                            if (!a.isDir() || (!guiContext.preferences.isViewHidden() && tuo.isHidden))
                                node.setVisible(false);
                            nodeArray.add(node);
                        }
                    }
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Could not retrieve listing from " + guiContext.context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    guiContext.context.fault = true;
                    guiContext.navigator.stop();
                }
            }

        };
        worker.execute();
    }

    protected void loadStatus()
    {
        if (myStatus != null)
        {
            int c = getChildCount(false, true);
            myStatus.setText(c + " item" + (c != 1 ? "s" : ""));
        }
        NavTreeUserObject tuo = getUserObject();
        if (tuo != null)
        {
            guiContext.form.textFieldLocation.setText(tuo.getPath());
            guiContext.browser.printProperties(tuo);
        }
        guiContext.form.labelStatusMiddle.setText("");
    }

    protected void loadTable()
    {
        TableColumn column;
        BrowserTableModel btm = new BrowserTableModel(this);
        myTable.setModel(btm);

        // tweak the columns
        // TODO Add remembering & restoring each table's column widths, etc.

        for (int i = 0; i < myTable.getColumnCount(); ++i)
        {
            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer(); //(DefaultTableCellRenderer) myTable.getCellRenderer(1, i);
            column = myTable.getColumnModel().getColumn(i);
            switch (i)
            {
                case 0:
                    column.setResizable(false);
                    column.setWidth(22);
                    column.setPreferredWidth(22);
                    column.setMaxWidth(22);
                    column.setMinWidth(22);
                    break;
                case 1:
                    cellRenderer.setHorizontalAlignment(JLabel.LEFT);
                    column.setCellRenderer(cellRenderer);
                    column.setResizable(true);
                    break;
                case 2:
                    cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
                    column.setCellRenderer(cellRenderer);
                    column.setResizable(true);
                    break;
                case 3:
                    column.setResizable(true);
                    cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
                    column.setCellRenderer(cellRenderer);
                    break;
            }
        }

        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        // TODO Add logic to sort each table based on the saved last-used values from Preferences
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        DefaultRowSorter sorter = ((DefaultRowSorter) myTable.getRowSorter());
        sorter.setSortKeys(sortKeys);

/*
        if (!guiContext.preferences.isViewHidden())
        {
            RowFilter<BrowserTableModel, Object> rowFilter = null;
            try
            {
                rowFilter = RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 0);
                sorter.setRowFilter(rowFilter);
            }
            catch (IllegalArgumentException e)
            {
            }
        }
*/

        sorter.sort();

        loadStatus();
    }

    public void selectMe()
    {
        myTree.scrollPathToVisible(getTreePath());
        myTree.requestFocus();
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
            setAllowsChildren(children.size() > 0);
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

    void setNavTreeUserObject(NavTreeUserObject ntuo)
    {
        this.setUserObject(ntuo);
    }

    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
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
