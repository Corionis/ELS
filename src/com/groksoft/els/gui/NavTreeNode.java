package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.*;

// NavTreeNode class is a customized DefaultMutableTreeNode
//
// Follows some of the ideas from http://www.java2s.com/Tutorials/Java/Swing_How_to/JTree/Expand_Collapse_Expand_with_JTree_Lazy_loading.htm

class NavTreeNode extends DefaultMutableTreeNode
{
    private GuiContext guiContext;
    private boolean loaded = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JLabel myStatus;
    private JTable myTable;
    private JTree myTree;
    private boolean refresh = true;
    SortTreeAlphabetically sortTreeAlphabetically;
    SortFoldersBeforeFiles sortFoldersBeforeFiles;
    private boolean visible = true;

    private NavTreeNode()
    {
        // hide default constructor
    }

    public NavTreeNode(GuiContext guiContext, JTree tree, NavTreeUserObject userObject)
    {
        super(userObject, true);
        this.guiContext = guiContext;
        this.myTree = tree;
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

    public TreeNode getChildAt(int index, boolean filterIsActive)
    {
        if (!filterIsActive)
        {
            return super.getChildAt(index);
        }

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
            if (node.isVisible())
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
        //return (TreeNode)children.elementAt(index);
    }

    public int getChildCount(boolean filterIsActive)
    {
        if (!filterIsActive)
        {
            return super.getChildCount();
        }

        if (children == null)
        {
            return 0;
        }

        int count = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            NavTreeNode node = (NavTreeNode) e.nextElement();
            if (node.isVisible())
            {
                count++;
            }
        }

        return count;
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
                        logger.info("bookmarks");
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
                        logger.debug("scanning local drive " + myTuo.path);
                        scan(new File(myTuo.path).getAbsoluteFile());
                        break;
                    case NavTreeUserObject.HOME:
                        File file = new File(myTuo.path);
                        if (file.isDirectory())
                        {
                            logger.debug("scanning home directory " + myTuo.path);
                            scan(file.getAbsoluteFile());
                        }
                        break;
                    case NavTreeUserObject.LIBRARY:
                        if (myTuo.sources != null && myTuo.sources.length > 0)
                        {
                            for (String path : myTuo.sources)
                            {
                                if (guiContext.cfg.isRemoteSession() && myTree.getName().equalsIgnoreCase("treeCollectionTwo"))
                                {
                                    logger.debug("scanning remote library " + path);
                                    scanRemote(path);
                                }
                                else
                                {
                                    logger.debug("scanning local library " + path);
                                    scan(new File(path).getAbsoluteFile());
                                }
                            }
                        }
                        break;
                    case NavTreeUserObject.REAL:
                        if (myTuo.file.isDirectory())
                        {
                            logger.debug("scanning local directory " + myTuo.file.getAbsolutePath());
                            scan(myTuo.file.getAbsoluteFile());
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        if (myTuo.isDir)
                        {
                            if (guiContext.cfg.isRemoteSession() && myTree.getName().equalsIgnoreCase("treeCollectionTwo"))
                            {
                                logger.debug("scanning remote directory " + myTuo.path);
                                scanRemote(myTuo.path);
                            }
                            else
                            {
                                logger.debug("scanning local folder " + myTuo.path);
                                scan(new File(myTuo.path).getAbsoluteFile());
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

                logger.debug(((NavTreeUserObject) getUserObject()).name + " has " + getChildCount(false) + " node(s)");
                super.done();
                //myTree.expandPath(getTreePath());  // IDEA: Could be a one-click option
            }

            protected void scan(File file)
            {
                if (file.isDirectory())
                {
                    File[] files = guiContext.fileSystemView.getFiles(file, true);
                    //sortFiles(files);
                    logger.info("found " + files.length + " entries from " + file.getAbsolutePath());
                    for (File entry : files)
                    {
                        NavTreeUserObject tuo = new NavTreeUserObject(entry.getName(), entry);
                        NavTreeNode node = new NavTreeNode(guiContext, myTree, tuo);
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
                    logger.info("received " + listing.size() + " entries from " + target);
                    for (int i = 0; i < listing.size(); ++i)
                    {
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            SftpATTRS a = entry.getAttrs();
                            if (a.isDir())
                            {
                                NavTreeUserObject tuo = new NavTreeUserObject(entry.getFilename(),
                                        target + guiContext.context.subscriberRepo.getSeparator() + entry.getFilename(),
                                        a.getSize(), a.getMTime(), a.isDir());
                                nodeArray.add(new NavTreeNode(guiContext, myTree, tuo));
                            }
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
            myStatus.setText(getChildCount(false) + " items");
        //guiContext.form.labelStatusMiddle.setText(((NavTreeUserObject)getUserObject()).name);
        NavTreeUserObject tuo = (NavTreeUserObject) getUserObject();
        if (tuo != null)
            guiContext.form.textFieldLocation.setText(tuo.getPath());
    }

    protected void loadTable()
    {
        TableColumn column;
        myTable.setModel(new BrowserTableModel(this));

        // tweak the columns
        // TODO Add remembering & restoring each table's column widths, etc.
        for (int i = 0; i < myTable.getColumnCount(); ++i)
        {
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
                    column.setResizable(true);
                    break;
                case 2:
                    column.setResizable(true);
                    break;
                case 3:
                    column.setResizable(true);
                    break;
            }
        }

        loadStatus();
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

    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    private void sortFiles(File[] files)
    {
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File f1, File f2)
            {
                if (guiContext.preferences.isSortCaseInsensitive())
                    return f1.getName().compareToIgnoreCase(f2.getName());
                else
                    return f1.getName().compareTo(f2.getName());
            }
        });
    }

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
