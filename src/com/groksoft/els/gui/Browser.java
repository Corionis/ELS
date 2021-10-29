package com.groksoft.els.gui;

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
    private static final int STYLE_COLLECTION_ALL = 0;
    private static final int STYLE_COLLECTION_AZ = 1;
    private static final int STYLE_COLLECTION_SOURCES = 2;
    private static final int STYLE_SYSTEM_ALL = 0;

    private static int styleOne = STYLE_COLLECTION_ALL;
    private static int styleTwo = STYLE_SYSTEM_ALL;

    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private GuiContext guiContext;
    private transient Logger logger = LogManager.getLogger("applog");
    private String os;
    private JProgressBar progressBar;

    public Browser(GuiContext gctx)
    {
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
        if (guiContext.context.publisherRepo != null && guiContext.context.publisherRepo.isInitialized())
        {
            loadCollectionTree(guiContext.form.treeCollectionOne, guiContext.context.publisherRepo);
//            loadTable(guiContext.form.treeCollectionOne,
//                    guiContext.form.tableCollectionOne,
//                    (NavTreeNode) guiContext.form.treeCollectionOne.getModel().getRoot());
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionOne, "--Open a publisher profile--");
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
                //guiContext.form.labelStatusLeft.setText(node.getChildCount(false) + " items");
                node.loadChildren();
            }
        });
        //
        // treeCollectionOne tree selection event handler
        guiContext.form.treeCollectionOne.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent)
            {
                TreePath treePath = treeSelectionEvent.getPath();
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                if (!node.isLoaded())
                    node.loadChildren();
                else
                    node.loadTable();
//                    loadTable(guiContext.form.treeCollectionOne,
//                            guiContext.form.tableCollectionOne,
//                            node);
                //guiContext.form.labelStatusLeft.setText(node.getChildCount(false) + " items");
            }
        });

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.name"));
//        loadTable(guiContext.form.treeSystemOne,
//                guiContext.form.tableSystemOne,
//                (NavTreeNode) guiContext.form.treeSystemOne.getModel().getRoot());
        //
        // treeSystemOne tree expansion event handlers
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
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                //guiContext.form.labelStatusLeft.setText(node.getChildCount(false) + " items");
                node.loadChildren();
            }
        });
        //
        // treeSystemOne tree selection event handler
        guiContext.form.treeSystemOne.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent)
            {
                TreePath treePath = treeSelectionEvent.getPath();
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                if (!node.isLoaded())
                    node.loadChildren();
                else
                    node.loadTable();
//                loadTable(guiContext.form.treeSystemOne,
//                        guiContext.form.tableSystemOne,
//                        node);
                //guiContext.form.labelStatusLeft.setText(node.getChildCount(false) + " items");
            }
        });


/*

        // --- BrowserTwo ------------------------------------------
        // --- treeCollectionTwo
        guiContext.form.treeCollectionTwo.setName("treeCollectionTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            logger.info("treeCollectionTwo");
            loadCollectionTree(guiContext.form.treeCollectionTwo, guiContext.context.subscriberRepo);
            loadTable(guiContext.form.treeCollectionTwo,
                    guiContext.form.tableCollectionTwo,
                    (NavTreeNode) guiContext.form.treeCollectionTwo.getModel().getRoot());
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber profile--");
        }
        //
        // treeCollectionTwo tree expansion event handler
        guiContext.form.treeCollectionTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
                //guiContext.form.labelStatusRight.setText(node.getChildCount(false) + " items");
                node.loadChildren();
            }
        });
        //
        // treeCollectionTwo tree selection event handler
        guiContext.form.treeCollectionTwo.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent)
            {
                TreePath treePath = treeSelectionEvent.getPath();
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                loadTable(guiContext.form.treeCollectionTwo,
                        guiContext.form.tableCollectionTwo,
                        node);
                //guiContext.form.labelStatusRight.setText(node.getChildCount(false) + " items");
            }
        });

        // --- treeSystemTwo
        guiContext.form.treeSystemTwo.setName("treeSystemTwo");
        loadSystemTree(guiContext.form.treeSystemTwo, System.getProperty("user.name"));
        loadTable(guiContext.form.treeSystemTwo,
                guiContext.form.tableSystemTwo,
                (NavTreeNode) guiContext.form.treeSystemTwo.getModel().getRoot());
        //
        // treeSystemTwo tree expansion event handler
        guiContext.form.treeSystemTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
                //guiContext.form.labelStatusRight.setText(node.getChildCount(false) + " items");
                node.loadChildren();
            }
        });
        //
        // treeSystemTwo tree selection event handler
        guiContext.form.treeSystemTwo.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent)
            {
                TreePath treePath = treeSelectionEvent.getPath();
                NavTreeNode node = (NavTreeNode) treePath.getLastPathComponent();
                loadTable(guiContext.form.treeSystemTwo,
                        guiContext.form.tableSystemTwo,
                        node);
                //guiContext.form.labelStatusRight.setText(node.getChildCount(false) + " items");
            }
        });
*/

        return true;
    }

    private void loadCollectionTree(JTree tree, Repository repo)
    {
        try
        {
            setCollectionRoot(tree, repo.getLibraryData().libraries.description);
            Arrays.sort(repo.getLibraryData().libraries.bibliography);
            switch (styleOne)
            {
                case STYLE_COLLECTION_ALL:
                    styleCollectionAll(tree, repo);
                    break;
                case STYLE_COLLECTION_AZ:
                    break;
                case STYLE_COLLECTION_SOURCES:
                    break;
                default:
                    break;
            }
            ((NavTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
        }
    }

    private void loadSystemTree(JTree tree, String initialLocation)
    {
        try
        {
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    styleSystemAll(tree, initialLocation);
                    break;
                default:
                    break;
            }
            ((NavTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
        }
    }

/*
    private void loadTable(JTree tree, JTable table, NavTreeNode startNode)
    {
        TableColumn column;
        table.setModel(new BrowserTableModel(startNode));

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
*/

    private void setCollectionRoot(JTree tree, String title)
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

    private void styleCollectionAll(JTree tree, Repository repo) throws Exception
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
        root.loadTable();
    }

    private void styleSystemAll(JTree tree, String initialLocation) throws Exception
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

        // setup new invisible root for Computer & Bookmarks
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

        // add Computer node
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

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            tuo = new NavTreeUserObject("Home", System.getProperty("user.home"), NavTreeUserObject.HOME);
            NavTreeNode homeNode = new NavTreeNode(guiContext, tree, tuo);
            homeNode.setAllowsChildren(true);
            root.add(homeNode);
            homeNode.loadChildren();
        }

        // add Bookmarks root node
        tuo = new NavTreeUserObject("Bookmarks", NavTreeUserObject.BOOKMARKS);
        NavTreeNode bookNode = new NavTreeNode(guiContext, tree, tuo);
        bookNode.setAllowsChildren(true);
        root.add(bookNode);
        ////////////////////////////bookNode.loadChildren();
    }


}
