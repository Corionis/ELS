package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.*;

// LEFTOFF: http://www.java2s.com/Tutorials/Java/Swing_How_to/JTree/Expand_Collapse_Expand_with_JTree_Lazy_loading.htm

class NavTreeNode extends DefaultMutableTreeNode
{
    private GuiContext guiContext;
    private boolean loaded = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private JTree myTree;
    private boolean refresh = true;
    private boolean visible = true;
    private NavTreeNode()
    {
        // hide default constructor
    }
    public NavTreeNode(GuiContext guiContext, JTree tree, Object userObject)
    {
        super(userObject, true);
        this.guiContext = guiContext;
        this.myTree = tree;
        this.visible = true;
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
    public boolean isLeaf()
    {
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

    public void loadChildren()
    {
        // return if items exist and refresh is not needed (it's already been scanned), or
        // if a fault occurred (to avoid cascading exceptions)
        if (guiContext.context.fault || !isRefresh())
            return;

        //progressBar.setVisible(true);
        //progressBar.setIndeterminate(true);

        SwingWorker<List<NavTreeNode>, Void> worker = new SwingWorker<List<NavTreeNode>, Void>()
        {
            List<NavTreeNode> nodeArray;
            @Override
            protected List<NavTreeNode> doInBackground() throws Exception
            {
                nodeArray = new ArrayList<NavTreeNode>();
                NavTreeUserObject tuo = (NavTreeUserObject) getUserObject();
                switch (tuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        logger.info("bookmarks");
                        break;
                    case NavTreeUserObject.BOX:
                        logger.debug("box");
                        break;
                    case NavTreeUserObject.COMPUTER:
                        logger.debug("computer");
                        break;
                    case NavTreeUserObject.DRIVE:
                        logger.debug("scanning local drive " + tuo.path);
                        scan(new File(tuo.path).getAbsoluteFile());
                        break;
                    case NavTreeUserObject.HOME:
                        File file = new File(tuo.path);
                        if (file.isDirectory())
                        {
                            logger.debug("scanning home directory " + tuo.path);
                            scan(file.getAbsoluteFile());
                        }
                        break;
                    case NavTreeUserObject.LIBRARY:
                        if (tuo.sources != null && tuo.sources.length > 0)
                        {
                            for (String path : tuo.sources)
                            {
                                // FIXME: Duplicates appear of shows spread across one than one drive
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
//                        sortTree(node);
//                        ((InvisibleTreeModel) tree.getModel()).reload(node);
                        break;
                    case NavTreeUserObject.REAL:
                        if (tuo.file.isDirectory())
                        {
                            logger.debug("scanning local directory " + tuo.file.getAbsolutePath());
                            // QUESTION Iterate children??
                            scan(tuo.file.getAbsoluteFile());
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        if (tuo.isDir)
                        {
                            if (guiContext.cfg.isRemoteSession() && myTree.getName().equalsIgnoreCase("treeCollectionTwo"))
                            {
                                logger.debug("scanning remote directory " + tuo.path);
                                scanRemote(tuo.path);
                            }
                            else
                            {
                                logger.debug("scanning local folder " + tuo.path);
                                scan(new File(tuo.path).getAbsoluteFile());
                            }
//                            sortTree(node);
//                            ((InvisibleTreeModel) tree.getModel()).reload(node);
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
                    NavTreeModel model = (NavTreeModel) myTree.getModel();
                    setChildren(get());
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
                //node.setRefresh(false);
//                sortTree(node);

                logger.info(((NavTreeUserObject) getUserObject()).name + " has " + getChildCount(true) + " node(s)");

                //((NavigatorTreeModel)tree.getModel();
                //model.nodeStructureChanged(NavTreeNode.this);
                //((NavigatorTreeModel) tree.getModel()).reload(selectedNode);
                //myTree.setEnabled(true);

//                ((NavigatorTreeModel) tree.getModel()).reload();

                //progressBar.setIndeterminate(false);
                //progressBar.setVisible(false);
                super.done();
//                tree.expandPath(treePath);  // IDEA: Could be a one-click option
            }

/*
            @Override
            protected void process(List<NavTreeUserObject> chunks)
            {
                for (NavTreeUserObject child : chunks)
                {
                    NavTreeNode nn = new NavTreeNode(guiContext, myTree, child);
                    if (child.isDir)
                    {
                        nn.setAllowsChildren(true);
                    }
                    else
                    {
                        nn.setAllowsChildren(false);
                        //nn.setRefresh(false);
                    }
                    add(nn);
                    logger.info("  added " + child.getType() + ": " + ((NavTreeUserObject) getUserObject()).name + " -> " + child.name);
                }
            }
*/

            protected void scan(File file)
            {
                if (file.isDirectory())
                {
                    File[] files = guiContext.fileSystemView.getFiles(file, true);
                    sortFiles(files);
                    logger.info("found " + files.length + " entries from " + file.getAbsolutePath());
                    //if (isLeaf())
                    {
                        for (File entry : files)
                        {
                            if (entry.isDirectory())
                            {
                                NavTreeUserObject tuo = new NavTreeUserObject(entry.getName(), entry);
                                nodeArray.add(new NavTreeNode(guiContext, myTree, tuo));
                            }
                        }
                    }
//                    else
//                    {
//                        logger.info("Not scanning: " + file.getAbsolutePath());
//                    }
                    //setTableData(files);
                    //loadTable(guiContext.form.treeCollectionOne, guiContext.form.tableCollectionOne, (InvisibleNode) node);
                }
            }

            protected void scanRemote(String target)
            {
                try
                {
                    Vector listing = guiContext.context.clientSftp.listDirectory(target);
                    //if (node.isLeaf())
                    {
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
                    //setTableData(files);
                    //loadTable(guiContext.form.treeCollectionOne, guiContext.form.tableCollectionOne, (InvisibleNode) node);
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

    protected void setChildren(List<NavTreeNode> children)
    {
        if (children != null)
        {
            removeAllChildren();
            setAllowsChildren(children.size() > 0);
            for (NavTreeNode ntn : children)
            {
                add(ntn);
            }
        }
        loaded = true;
    }

    public void setLoaded(boolean loaded)
    {
        this.loaded = loaded;
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

    private NavTreeNode sortTree(NavTreeNode node)
    {
        // sort alphabetically
        for (int i = 0; i < node.getChildCount() - 1; i++)
        {
            NavTreeNode child = (NavTreeNode) node.getChildAt(i);
            String tn = child.getUserObject().toString();

            for (int j = i + 1; j <= node.getChildCount() - 1; j++)
            {
                NavTreeNode prevNode = (NavTreeNode) node.getChildAt(j);
                String pn = prevNode.getUserObject().toString();
                if (tn.compareToIgnoreCase(pn) > 0)
                {
                    node.insert(child, j);
                    node.insert(prevNode, i);
                }
            }
            if (child.getChildCount() > 0)
            {
                sortTree(child);
            }
        }

        // put folders first - normal on Windows and some flavors of Linux but not on Mac OS X.
        if (guiContext.preferences.isSortFoldersBeforeFiles())
        {
            for (int i = 0; i < node.getChildCount() - 1; i++)
            {
                NavTreeNode child = (NavTreeNode) node.getChildAt(i);
                for (int j = i + 1; j <= node.getChildCount() - 1; j++)
                {
                    NavTreeNode prevNode = (NavTreeNode) node.getChildAt(j);
                    if (!prevNode.isLeaf() && child.isLeaf())
                    {
                        node.insert(child, j);
                        node.insert(prevNode, i);
                    }
                }
            }
        }

        return node;
    }

}
