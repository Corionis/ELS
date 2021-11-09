package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.sun.jndi.toolkit.url.Uri;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;

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
    private int splitPaneTwoBrowsersLastDividerLocation;
    private int splitPanelBrowserLastDividerLocation;

    public Browser(GuiContext gctx)
    {
        guiContext = gctx;
        initialize();
    }

    private void addMouseListenerToTable(JTable table)
    {
        MouseAdapter tableMouseListener = new MouseAdapter()
        {
            synchronized public void mouseClicked(MouseEvent mouseEvent)
            {
                JTable target = (JTable) mouseEvent.getSource();
                target.requestFocus();
                JTree eventTree = null;
                switch (target.getName())
                {
                    case "tableCollectionOne":
                        eventTree = guiContext.form.treeCollectionOne;
                        break;
                    case "tableSystemOne":
                        eventTree = guiContext.form.treeSystemOne;
                        break;
                    case "tableCollectionTwo":
                        eventTree = guiContext.form.treeCollectionTwo;
                        break;
                    case "tableSystemTwo":
                        eventTree = guiContext.form.treeSystemTwo;
                        break;
                }
                int row = target.getSelectedRow();
                if (row >= 0)
                {
                    NavTreeNode node = (NavTreeNode) target.getModel().getValueAt(row, 4);
                    guiContext.form.textFieldLocation.setText(node.getUserObject().getPath());
                    if (mouseEvent.getClickCount() == 2)
                    {
                        if (node.getUserObject().isDir)
                        {
                            TreeSelectionEvent evt = new TreeSelectionEvent(node, node.getTreePath(), true, null, null);
                            fireTreeSelectionEvent(eventTree, evt);
                            eventTree.setSelectionPath(node.getTreePath());
                        }
                        else
                        {
                            NavTreeUserObject tuo = node.getUserObject();
                            if (tuo.type == NavTreeUserObject.REAL)
                            {
                                try
                                {
                                    Desktop.getDesktop().open(tuo.file);
                                }
                                catch (Exception e)
                                {
                                    JOptionPane.showMessageDialog(guiContext.form, "Error launching item", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                            else
                            {
                                JOptionPane.showMessageDialog(guiContext.form, "Cannot launch " + (guiContext.cfg.isRemoteSession() ? "remote " : "") + "item", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        };
        table.addMouseListener(tableMouseListener);
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

    private void fireTreeSelectionEvent(JTree tree, TreeSelectionEvent evt)
    {
        TreeSelectionListener[] listeners = tree.getTreeSelectionListeners();
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i].valueChanged(evt);
        }
    }

    private boolean initialize()
    {
        os = Utils.getOS();
        logger.debug("Detected local system as " + os);

        JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
        progressBar = new JProgressBar();
        simpleOutput.add(progressBar, BorderLayout.EAST);
        progressBar.setVisible(false);

        initializeMainMenu();

        // --- BrowserOne ------------------------------------------
        //
        // --- tab selection handler
        guiContext.form.tabbedPaneBrowserOne.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent changeEvent)
            {
                JTabbedPane pane = (JTabbedPane) changeEvent.getSource();
                NavTreeModel model = null;
                NavTreeNode node = null;
                switch (pane.getSelectedIndex())
                {
                    case 0:
                        model = (NavTreeModel) guiContext.form.treeCollectionOne.getModel();
                        node = (NavTreeNode) guiContext.form.treeCollectionOne.getLastSelectedPathComponent();
                        break;
                    case 1:
                        model = (NavTreeModel) guiContext.form.treeSystemOne.getModel();
                        node = (NavTreeNode) guiContext.form.treeSystemOne.getLastSelectedPathComponent();
                        break;
                }
                if (node == null)
                    node = (NavTreeNode) model.getRoot();
                node.loadStatus();
            }
        });

        // --- treeCollectionOne
        guiContext.form.treeCollectionOne.setName("treeCollectionOne");
        if (guiContext.context.publisherRepo != null && guiContext.context.publisherRepo.isInitialized())
        {
            loadCollectionTree(guiContext.form.treeCollectionOne, guiContext.context.publisherRepo);
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
                node.loadChildren(false);
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
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        addMouseListenerToTable(guiContext.form.tableCollectionOne);

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.name"));
        //
        // treeSystemOne tree expansion event handler
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
                node.loadChildren(false);
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
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        addMouseListenerToTable(guiContext.form.tableSystemOne);


        // --- BrowserTwo ------------------------------------------
        // --- treeCollectionTwo
        guiContext.form.treeCollectionTwo.setName("treeCollectionTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadCollectionTree(guiContext.form.treeCollectionTwo, guiContext.context.subscriberRepo);
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
                node.loadChildren(false);
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
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        addMouseListenerToTable(guiContext.form.tableCollectionTwo);

        // --- treeSystemTwo
        guiContext.form.treeSystemTwo.setName("treeSystemTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadSystemTree(guiContext.form.treeSystemTwo, System.getProperty("user.name"));
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber profile--");
        }
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
                node.loadChildren(false);
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
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        addMouseListenerToTable(guiContext.form.tableSystemTwo);

        NavTreeModel model = (NavTreeModel) guiContext.form.treeCollectionOne.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        root.loadStatus();

        return true;
    }

    private void initializeMainMenu()
    {
        // --- Main Menu ------------------------------------------
        //
        // -- File Menu

        //
        // -- Window Menu
        guiContext.form.menuItemMaximize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(guiContext.form.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });
        //
        guiContext.form.menuItemMinimize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setState(JFrame.ICONIFIED);
            }
        });
        //
        guiContext.form.menuItemRestore.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(JFrame.NORMAL);
            }
        });
        //
        guiContext.form.menuItemSplitHorizontal.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getHeight();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.VERTICAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });
        //
        guiContext.form.menuItemSplitVertical.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getWidth();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // -- Help Menu
        //
        guiContext.form.menuItemDocumentation.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS/wiki");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        //
        guiContext.form.menuItemGitHubProject.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

    }

    private void loadCollectionTree(JTree tree, Repository repo)
    {
        try
        {
            NavTreeNode root = setCollectionRoot(tree, repo.getLibraryData().libraries.description);
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
            root.loadTable();
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
            NavTreeNode root = null;
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    root = styleSystemAll(tree, initialLocation);
                    break;
                default:
                    break;
            }
            ((NavTreeModel) tree.getModel()).reload();
            root.loadTable();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
        }
    }

    private NavTreeNode setCollectionRoot(JTree tree, String title)
    {
        NavTreeUserObject tuo = new NavTreeUserObject(title, NavTreeUserObject.COLLECTION);
        NavTreeNode root = new NavTreeNode(guiContext, tree, tuo);
        root.setAllowsChildren(true);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(true);
        tree.setCellRenderer(new NavTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setModel(model);
        return root;
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
            node.loadChildren(false);
        }
        root.setLoaded(true);
    }

    private NavTreeNode styleSystemAll(JTree tree, String initialLocation) throws Exception
    {
        /*
         * Computer
         *   Drive /
         * Home
         *   Directory trh
         * BookMarks
         *   Entry
         */
        // TODO Change to externalized strings

        // setup new invisible root for Computer, Home & Bookmarks
        NavTreeUserObject tuo = new NavTreeUserObject("System", NavTreeUserObject.SYSTEM);
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
        if (guiContext.cfg.isRemoteSession() && tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            tuo = new NavTreeUserObject("/", "/", NavTreeUserObject.REMOTE);
            NavTreeNode node = new NavTreeNode(guiContext, tree, tuo);
            rootNode.add(node);
            node.loadChildren(false);
        }
        else
        {
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
                node.loadChildren(false);
            }
        }
        rootNode.setLoaded(true);

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            tuo = new NavTreeUserObject("Home", System.getProperty("user.home"), NavTreeUserObject.HOME);
            NavTreeNode homeNode = new NavTreeNode(guiContext, tree, tuo);
            homeNode.setAllowsChildren(true);
            root.add(homeNode);
            homeNode.loadChildren(false);
        }

        root.setLoaded(true);
        return root;
    }
}
