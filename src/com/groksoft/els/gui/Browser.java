package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
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
import java.util.*;

public class Browser
{
    // styles of tree display
    private static final int STYLECOLLECTION_ALL = 0;
    private static final int STYLECOLLECTION_AZ = 1;
    private static final int STYLECOLLECTION_SOURCES = 2;
    private static final int STYLESYSTEM_BOOKMARKS = 1;
    private static final int STYLESYSTEM_TREE = 0;

    private static int styleOne = STYLECOLLECTION_ALL;
    private static int styleTwo = STYLESYSTEM_TREE;

    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private Configuration cfg;
    private Context context;
    private GuiContext guiContext;
    private transient Logger logger = LogManager.getLogger("applog");
    private Navigator navigator;
    private String os;
    private JProgressBar progressBar;

    public Browser(Navigator nav, Configuration config, Context ctx, GuiContext gctx)
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
        //
        // treeCollectionOne tree expansion event handler
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
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                node.loadChildren();
            }
        });

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.name"));
        // loadTable(guiContext.form.treeSystemOne,
        //        guiContext.form.tableSystemOne,
        //        (InvisibleNode) guiContext.form.treeSystemOne.getModel().getRoot());
        //
        //
        // treeSystemOne tree expansion event handlers
        guiContext.form.treeSystemOne.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            @Override
            public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent)
            {
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent treeExpansionEvent)
            {
                TreePath treePath = treeExpansionEvent.getPath();
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                node.loadChildren();
            }
        });

/*

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
//               guiContext.form.treeSystemTwo.setSelectionPath(treePath);
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
*/

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
            ((NavTreeModel) tree.getModel()).reload();
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
            ((NavTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
        }
    }

    private void loadTable(JTree tree, JTable table, NavTreeNode startNode)
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
        NavTreeNode root = new NavTreeNode(guiContext, tree, title);
        root.setAllowsChildren(true);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(true);
        tree.setCellRenderer(new NavTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setModel(model);
    }

    private void styleCollectionTreeAll(JTree tree, Repository repo) throws Exception
    {
        NavTreeModel model = (NavTreeModel) tree.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            NavTreeUserObject tuo = new NavTreeUserObject(lib.name, lib.sources);
            NavTreeNode node = new NavTreeNode(guiContext, tree, tuo);
            root.add(node);
            node.loadChildren();
        }
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
        NavTreeUserObject tuo = new NavTreeUserObject("Box", NavTreeUserObject.BOX);
        NavTreeNode root = new NavTreeNode(guiContext, tree, tuo);
        root.setAllowsChildren(true);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(true);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setCellRenderer(new NavTreeCellRenderer());
        tree.setModel(model);

        // add Computer root node
        tuo = new NavTreeUserObject("Computer", NavTreeUserObject.COMPUTER);
        NavTreeNode rootNode = new NavTreeNode(guiContext, tree, tuo);
        rootNode.setAllowsChildren(true);
        root.add(rootNode);

        // get all available storage drives
        File[] rootPaths;
        FileSystemView fsv = FileSystemView.getFileSystemView();
        rootPaths = File.listRoots();

        for (int i = 0; i < rootPaths.length; ++i)
        {
            File drive = rootPaths[i];
            tuo = new NavTreeUserObject(drive.getPath(), drive.getAbsolutePath(), NavTreeUserObject.DRIVE);
            NavTreeNode node = new NavTreeNode(guiContext, tree, tuo);
            rootNode.add(node);
            node.loadChildren();
        }
/*

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            tuo = new TreeUserObject("Home", System.getProperty("user.home"), TreeUserObject.HOME);
            NavigatorNode homeNode = new NavigatorNode(tuo);
            homeNode.setAllowsChildren(true);
            root.add(homeNode);
//            TreePath path = homeNode.getTreePath();
//            styleTreeAll(tree, path, homeNode);
        }
*/
/*

        // add Bookmarks root node
        tuo = new NavTreeUserObject("Bookmarks", NavTreeUserObject.BOOKMARKS);
        NavTreeNode bookNode = new NavTreeNode(guiContext, tree, tuo);
        bookNode.setAllowsChildren(true);
        root.add(bookNode);
*/

    }


}
