package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

public class Browser
{
    // styles of tree display
    private static final int STYLECOLLECTION_ALL = 0;
    private static final int STYLECOLLECTION_AZ = 1;
    private static final int STYLECOLLECTION_SOURCES = 2;
    private static final int STYLESYSTEM_BOOKMARKS = 1;
    private static final int STYLESYSTEM_TREE = 0;
    private static final int styleOne = STYLECOLLECTION_ALL;
    private static final int styleTwo = STYLESYSTEM_TREE;

    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private Configuration cfg;
    private Context context;
    private FileSystemView fileSystemView;
    private Navigator.GuiContext guiContext;
    private transient Logger logger = LogManager.getLogger("applog");
    private Navigator navigator;
    private String os;
    private JProgressBar progressBar;
    private int scanDepth = 0;
    //
    private boolean sortCaseInsensitive = true;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;

    public Browser(Navigator nav, Configuration config, Context ctx, Navigator.GuiContext gctx)
    {
        navigator = nav;
        cfg = config;
        context = ctx;
        guiContext = gctx;

        initialize();
    }

    private TreePath findTreePath(JTree tree, File find)
    {
        for (int i = 0; i < tree.getRowCount(); ++i)
        {
            TreePath treePath = tree.getPathForRow(i);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            File nodeFile = (File) node.getUserObject();
            if (nodeFile == find)
                return treePath;
        }
        return null;
    }

    private boolean initialize()
    {
        os = Utils.getOS();
        logger.debug("Detected local system as " + os);

        JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
        progressBar = new JProgressBar();
        simpleOutput.add(progressBar, BorderLayout.EAST);
        progressBar.setVisible(false);

        fileSystemView = FileSystemView.getFileSystemView();

        // --- BrowserOne ------------------------------------------
        // --- treeCollectionOne
        guiContext.form.treeCollectionOne.setName("treeCollectionOne");
        if (context.publisherRepo != null && context.publisherRepo.isInitialized())
        {
            loadCollectionTree(guiContext.form.treeCollectionOne, context.publisherRepo);
            // loadTable(guiContext.form.treeCollectionOne,
            //        guiContext.form.tableCollectionOne,
            //        (InvisibleNode) guiContext.form.treeCollectionOne.getModel().getRoot());
        }
        else
        {
            setCollectionTreeRoot(guiContext.form.treeCollectionOne, "--Open a publisher profile--");
        }

        guiContext.form.treeCollectionOne.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException
            {
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);

//////                guiContext.form.treeCollectionOne.setSelectionPath(treePath);
            }
        });

/*
        //
        // treeCollectionOne tree expansion event handler
        guiContext.form.treeCollectionOne.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillCollaspe(TreeExpansionEvent treeExpansionEvent)
            {
                // noop
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent treeExpansionEvent)
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);

//////                guiContext.form.treeCollectionOne.setSelectionPath(treePath);
            }
        });
*/
        //
        // treeCollectionOne tree selection event handler
        guiContext.form.treeCollectionOne.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                TreePath treePath = e.getPath();
                NavigatorNode node = (NavigatorNode) guiContext.form.treeCollectionOne.getLastSelectedPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");

                // the real work of populating treeCollectionOne
//                node.removeAllChildren();
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);
            }
        });

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.name"));
        // loadTable(guiContext.form.treeSystemOne,
        //        guiContext.form.tableSystemOne,
        //        (InvisibleNode) guiContext.form.treeSystemOne.getModel().getRoot());
        //

        guiContext.form.treeSystemOne.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException
            {
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);
///////                guiContext.form.treeSystemOne.setSelectionPath(treePath);
            }
        });

/*
        // treeSystemOne tree expansion event handler
        guiContext.form.treeSystemOne.addTreeExpansionListener(new TreeExpansionListener()
        {
            @Override
            public void treeCollapsed(TreeExpansionEvent treeExpansionEvent)
            {
                // noop
            }

            @Override
            public void treeExpanded(TreeExpansionEvent treeExpansionEvent)
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);
///////                guiContext.form.treeSystemOne.setSelectionPath(treePath);
            }
        });
*/
        //
        // treeSystemOne tree selection event handler
        guiContext.form.treeSystemOne.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                TreePath treePath = e.getPath();
                NavigatorNode node = (NavigatorNode) guiContext.form.treeSystemOne.getLastSelectedPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionOne, treePath, node);
//                loadTable(guiContext.form.treeSystemOne, guiContext.form.tableSystemOne, (NavigatorNode) node);
            }
        });


        // --- BrowserTwo ------------------------------------------
        // --- treeCollectionTwo
        guiContext.form.treeCollectionTwo.setName("treeCollectionTwo");
        if (context.subscriberRepo != null && context.subscriberRepo.isInitialized())
        {
            logger.info("treeCollectionTwo");
            loadCollectionTree(guiContext.form.treeCollectionTwo, context.subscriberRepo);
            // loadTable(guiContext.form.treeCollectionTwo,
            //        guiContext.form.tableCollectionTwo,
            //        (InvisibleNode) guiContext.form.treeCollectionTwo.getModel().getRoot());
        }
        else
        {
            setCollectionTreeRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber profile--");
        }
        //
        // treeCollectionTwo tree expansion event handler
        guiContext.form.treeCollectionTwo.addTreeExpansionListener(new TreeExpansionListener()
        {
            @Override
            public void treeCollapsed(TreeExpansionEvent treeExpansionEvent)
            {
                // noop
            }

            @Override
            public void treeExpanded(TreeExpansionEvent treeExpansionEvent)
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusRight.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionTwo, treePath, node);
//////////                guiContext.form.treeCollectionTwo.setSelectionPath(treePath);
            }
        });
        //
        // treeCollectionTwo tree selection event handler
        guiContext.form.treeCollectionTwo.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                TreePath treePath = e.getPath();
                NavigatorNode node = (NavigatorNode) guiContext.form.treeCollectionTwo.getLastSelectedPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusRight.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionTwo, treePath, node);
            }
        });
        //
        // iterate each top-level row (library) of treeCollectionTwo selecting each individually to trigger the real work
/*
        for (int i = 1; i < guiContext.form.treeCollectionTwo.getRowCount(); ++i)
        {
            path = guiContext.form.treeCollectionTwo.getPathForRow(i);
            guiContext.form.treeCollectionTwo.setSelectionPath(path);
        }
        path = guiContext.form.treeCollectionTwo.getPathForRow(0);
        guiContext.form.treeCollectionTwo.setSelectionPath(path);
*/
        //guiContext.form.treeCollectionTwo.setSelectionRow(0);

        // --- treeSystemTwo
        guiContext.form.treeSystemTwo.setName("treeSystemTwo");
        loadSystemTree(guiContext.form.treeSystemTwo, System.getProperty("user.name"));
        // loadTable(guiContext.form.treeSystemTwo,
        //        guiContext.form.tableSystemTwo,
        //        (InvisibleNode) guiContext.form.treeSystemTwo.getModel().getRoot());
        //
        // treeSystemTwo tree expansion event handler
        guiContext.form.treeSystemTwo.addTreeExpansionListener(new TreeExpansionListener()
        {
            @Override
            public void treeCollapsed(TreeExpansionEvent treeExpansionEvent)
            {
                // noop
            }

            @Override
            public void treeExpanded(TreeExpansionEvent treeExpansionEvent)
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavigatorNode node = (NavigatorNode) treePath.getLastPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionTwo, treePath, node);
///////////                guiContext.form.treeSystemTwo.setSelectionPath(treePath);
            }
        });
        //
        // treeSystemTwo tree selection event handler
        guiContext.form.treeSystemTwo.addTreeSelectionListener(new TreeSelectionListener()
        {
            public void valueChanged(TreeSelectionEvent e)
            {
                TreePath treePath = e.getPath();
                NavigatorNode node = (NavigatorNode) guiContext.form.treeSystemTwo.getLastSelectedPathComponent();
                if (node == null)
                    return;
                guiContext.form.labelStatusLeft.setText(node.getChildCount(true) + " items");
                styleTreeAll(guiContext.form.treeCollectionTwo, treePath, node);
//                loadTable(guiContext.form.treeSystemTwo, guiContext.form.tableSystemTwo, (NavigatorNode) node);
            }
        });

        return true;
    }

    private void loadCollectionTree(JTree tree, Repository repo)
    {
        try
        {
            setCollectionTreeRoot(tree, repo.getLibraryData().libraries.description);
            Arrays.sort(repo.getLibraryData().libraries.bibliography);
            switch (styleOne)
            {
                case STYLECOLLECTION_ALL:
                    styleCollectionTreeAll(tree, repo);
                    break;
                case STYLECOLLECTION_AZ:
                    break;
                case STYLECOLLECTION_SOURCES:
                    break;
                default:
                    break;
            }
            ((NavigatorTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
        }
    }

    private void loadSystemTree(JTree tree, String initialLocation)
    {
        try
        {
            switch (styleTwo)
            {
                case STYLESYSTEM_TREE:
                    styleSystemTreeAll(tree, initialLocation);
                    break;
                case STYLESYSTEM_BOOKMARKS:
                    break;
                default:
                    break;
            }
            ((NavigatorTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
        }
    }

    private void loadTable(JTree tree, JTable table, NavigatorNode startNode)
    {
        TableColumn column;
        table.setModel(new BrowserTableModel(tree, startNode));

        // tweak the columns
        // TODO Add remembering & restoring each table's column widths, etc.
        for (int i = 0; i < table.getColumnCount(); ++i)
        {
            column = table.getColumnModel().getColumn(i);
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
    }

    private void setCollectionTreeRoot(JTree tree, String title)
    {
        NavigatorNode root = new NavigatorNode(title);
        root.setAllowsChildren(true);
        NavigatorTreeModel model = new NavigatorTreeModel(root, true);
        model.activateFilter(true);
        tree.setCellRenderer(new NavigatorTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setModel(model);
    }

    private void sortFiles(File[] files)
    {
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File f1, File f2)
            {
                if (sortCaseInsensitive)
                    return f1.getName().compareToIgnoreCase(f2.getName());
                else
                    return f1.getName().compareTo(f2.getName());
            }
        });
    }

    private NavigatorNode sortTree(NavigatorNode node)
    {
        // sort alphabetically
        for (int i = 0; i < node.getChildCount() - 1; i++)
        {
            NavigatorNode child = (NavigatorNode) node.getChildAt(i);
            String nt = child.getUserObject().toString();

            for (int j = i + 1; j <= node.getChildCount() - 1; j++)
            {
                NavigatorNode prevNode = (NavigatorNode) node.getChildAt(j);
                String np = prevNode.getUserObject().toString();
                if (nt.compareToIgnoreCase(np) > 0)
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
        if (sortFoldersBeforeFiles)
        {
            for (int i = 0; i < node.getChildCount() - 1; i++)
            {
                NavigatorNode child = (NavigatorNode) node.getChildAt(i);
                for (int j = i + 1; j <= node.getChildCount() - 1; j++)
                {
                    NavigatorNode prevNode = (NavigatorNode) node.getChildAt(j);
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

    private void styleCollectionTreeAll(JTree tree, Repository repo) throws Exception
    {
        NavigatorTreeModel model = (NavigatorTreeModel) tree.getModel();
        NavigatorNode root = (NavigatorNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            TreeUserObject tuo = new TreeUserObject(lib.name, lib.sources);
            NavigatorNode node = new NavigatorNode(tuo);
            root.add(node);
            TreePath path = node.getTreePath();
            styleTreeAll(tree, path, node);
        }
//        TreePath path = tree.getPathForRow(0);
//        tree.setSelectionPath(path);
    }

    private void styleSystemTreeAll(JTree tree, String initialLocation) throws Exception
    {
        /*
         * BookMarks
         *   Entry
         * Computer
         *   Drive /
         * Home
         *   Directory trh
         */
        // TODO Change to externalized strings

        // setup new empty tree & System tree cell renderer
        TreeUserObject tuo = new TreeUserObject("Box", TreeUserObject.BOX);
        NavigatorNode root = new NavigatorNode(tuo);
        root.setAllowsChildren(true);
        NavigatorTreeModel model = new NavigatorTreeModel(root, true);
        model.activateFilter(true);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setCellRenderer(new NavigatorTreeCellRenderer());
        tree.setModel(model);

        // add Computer root node
        tuo = new TreeUserObject("Computer", TreeUserObject.COMPUTER);
        NavigatorNode rootNode = new NavigatorNode(tuo);
        rootNode.setAllowsChildren(true);
        root.add(rootNode);

        // get all available storage drives
        File[] rootPaths;
        FileSystemView fsv = FileSystemView.getFileSystemView();
        rootPaths = File.listRoots();

        for (int i = 0; i < rootPaths.length; ++i)
        {
            File drive = rootPaths[i];
            tuo = new TreeUserObject(drive.getPath(), drive.getAbsolutePath(), TreeUserObject.DRIVE);
            NavigatorNode treeNode = new NavigatorNode(tuo);
            rootNode.add(treeNode);
            TreePath path = rootNode.getTreePath();
            styleTreeAll(tree, path, rootNode);
        }

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            tuo = new TreeUserObject("Home", System.getProperty("user.home"), TreeUserObject.HOME);
            NavigatorNode homeNode = new NavigatorNode(tuo);
            homeNode.setAllowsChildren(true);
            root.add(homeNode);
            TreePath path = homeNode.getTreePath();
            styleTreeAll(tree, path, homeNode);
        }

        // add Bookmarks root node
        tuo = new TreeUserObject("Bookmarks", TreeUserObject.BOOKMARKS);
        NavigatorNode bookNode = new NavigatorNode(tuo);
        bookNode.setAllowsChildren(true);
        root.add(bookNode);

    }

    synchronized private void styleTreeAll(JTree tree, TreePath treePath, NavigatorNode parent)
    {
        // return if items exist and refresh is not needed (it's already been scanned), or
        // if a fault occurred (to avoid cascading exceptions)
        if (context.fault) // || (node.getChildCount(true) > 0 && !node.isRefresh()))
            return;

        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, TreeUserObject> worker = new SwingWorker<Void, TreeUserObject>()
        {
            NavigatorNode node;
            NavigatorNode root = parent;

            @Override
            public Void doInBackground()
            {
                if (root.getChildCount(true) > 0)
                {
                    // populate tree one-level "down" from the current node
                    // iterate children each individually
                    for (int i = 0; i < root.getChildCount(true); ++i)
                    {
                        node = (NavigatorNode) root.getChildAt(i, true);
                        if (node.isRefresh())
                            queue();
                    }
                }
                else
                {
                    node = root;
                    if (node.isRefresh())
                        queue();
                }
                return null;
            }

            @Override
            protected void done()
            {
                try
                {
                    get();
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Swing worker fault during get of " + root.getTreePath().toString() + ", " + context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    context.fault = true;
                    navigator.stop();
                }
                node.setRefresh(false);
//                sortTree(node);

                ((NavigatorTreeModel) tree.getModel()).reload(parent);
                tree.setEnabled(true);

                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
//                tree.expandPath(treePath);  // IDEA: Could be a one-click option
            }

            @Override
            protected void process(List<TreeUserObject> chunks)
            {
                for (TreeUserObject child : chunks)
                {
                    NavigatorNode nn = new NavigatorNode(child);
                    if (child.isDir)
                    {
                        nn.setAllowsChildren(true);
                    }
                    else
                    {
                        nn.setAllowsChildren(false);
                        nn.setRefresh(false);
                    }
                    node.add(nn);
                }
            }

            protected void queue()
            {
                TreeUserObject tuo = (TreeUserObject) node.getUserObject();
                switch (tuo.type)
                {
                    case TreeUserObject.BOOKMARKS:
                        logger.info("bookmarks");
                        break;
                    case TreeUserObject.BOX:
                        logger.info("box");
                        break;
                    case TreeUserObject.COMPUTER:
                        logger.info("computer");
                        break;
                    case TreeUserObject.DRIVE:
                        logger.info("scanning local drive " + tuo.path);
                        scan(new File(tuo.path).getAbsoluteFile());
                        break;
                    case TreeUserObject.HOME:
                        File file = new File(tuo.path);
                        if (file.isDirectory())
                        {
                            logger.info("scanning home directory " + tuo.path);
                            scan(file.getAbsoluteFile());
                        }
                        break;
                    case TreeUserObject.LIBRARY:
                        if (tuo.sources != null && tuo.sources.length > 0)
                        {
                            for (String path : tuo.sources)
                            {
                                if (cfg.isRemoteSession() && tree.getName().equalsIgnoreCase("treeCollectionTwo"))
                                {
                                    logger.info("scanning remote library " + path);
                                    scanRemote(path);
                                }
                                else
                                {
                                    logger.info("scanning local library " + path);
                                    scan(new File(path).getAbsoluteFile());
                                }
                            }
                        }
//                        sortTree(node);
//                        ((InvisibleTreeModel) tree.getModel()).reload(node);
                        break;
                    case TreeUserObject.REAL:
                        if (tuo.file.isDirectory())
                        {
                            logger.info("scanning local directory " + tuo.file.getAbsolutePath());
                            scan(tuo.file.getAbsoluteFile());
                        }
                        break;
                    case TreeUserObject.REMOTE:
                        if (tuo.isDir)
                        {
                            if (cfg.isRemoteSession() && tree.getName().equalsIgnoreCase("treeCollectionTwo"))
                            {
                                logger.info("scanning remote directory " + tuo.path);
                                scanRemote(tuo.path);
                            }
                            else
                            {
                                logger.info("scanning local folder " + tuo.path);
                                scan(new File(tuo.path).getAbsoluteFile());
                            }
//                            sortTree(node);
//                            ((InvisibleTreeModel) tree.getModel()).reload(node);
                        }
                        break;
                }
            }

            protected void scan(File file)
            {
                if (file.isDirectory())
                {
                    File[] files = fileSystemView.getFiles(file, true);
                    sortFiles(files);
                    logger.info("found " + files.length + " entries from " + file.getAbsolutePath());
                    if (node.isLeaf())
                    {
                        for (File child : files)
                        {
                            if (child.isDirectory())
                            {
                                TreeUserObject tuo = new TreeUserObject(child.getName(), child);
                                publish(tuo);
                                logger.info("  added " + child.getAbsolutePath());
                            }
                        }
                    }
                    //setTableData(files);
                    //loadTable(guiContext.form.treeCollectionOne, guiContext.form.tableCollectionOne, (InvisibleNode) node);
                }
            }

            protected void scanRemote(String target)
            {
                try
                {
                    Vector listing = context.clientSftp.listDirectory(target);
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
                                    TreeUserObject tuo = new TreeUserObject(entry.getFilename(), target + context.subscriberRepo.getSeparator() + entry.getFilename(),
                                            a.getSize(), a.getMTime(), a.isDir());
                                    publish(tuo);
                                }
                            }
                        }
                    }
                    //setTableData(files);
                    //loadTable(guiContext.form.treeCollectionOne, guiContext.form.tableCollectionOne, (InvisibleNode) node);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Could not retrieve listing from " + context.subscriberRepo.getLibraryData().libraries.description + "  ",
                            cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    logger.error(Utils.getStackTrace(e));
                    context.fault = true;
                    navigator.stop();
                }
            }

        };
        worker.execute();
    }

}
