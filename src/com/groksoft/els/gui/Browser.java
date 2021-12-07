package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class Browser
{
    // style definitions of tree display
    private static final int STYLE_COLLECTION_ALL = 0;
    private static final int STYLE_COLLECTION_AZ = 1;
    private static final int STYLE_COLLECTION_SOURCES = 2;
    private static final int STYLE_SYSTEM_ALL = 0;

    // style selections
    private static int styleOne = STYLE_COLLECTION_ALL;
    private static int styleTwo = STYLE_SYSTEM_ALL;
    public JComponent lastComponent = null;
    public int lastFocused = 1;
    public int lastPanel = 1;
    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private GuiContext guiContext;
    private transient Logger logger = LogManager.getLogger("applog");
    private Stack<NavTreeNode> navStack = new Stack<>();
    private int navStackIndex = -1;
    private NavTransferHandler navTransferHandler;
    private String os;
    private int tabStop = 0;
    private int[] tabStops = {1, 2, 4, 5};
    private JProgressBar progressBar;

    public Browser(GuiContext gctx)
    {
        guiContext = gctx;
        guiContext.browser = this;
        initialize();
    }

    private void addFocusListener(Component component)
    {
        FocusListener focusListener = new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent focusEvent)
            {
                String name = "";
                Component active = focusEvent.getComponent();
                name = active.getName();
                if (name.length() > 0)
                {
                    switch (name)
                    {
                        case "treeCollectionOne":
                            lastFocused = 0;
                            lastPanel = 1;
                            lastComponent = guiContext.form.treeCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "tableCollectionOne":
                            lastFocused = 1;
                            lastPanel = 1;
                            lastComponent = guiContext.form.tableCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "treeSystemOne":
                            lastFocused = 2;
                            lastPanel = 2;
                            lastComponent = guiContext.form.treeSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "tableSystemOne":
                            lastFocused = 3;
                            lastPanel = 2;
                            lastComponent = guiContext.form.tableSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "treeCollectionTwo":
                            lastFocused = 4;
                            lastPanel = 3;
                            lastComponent = guiContext.form.treeCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "tableCollectionTwo":
                            lastFocused = 5;
                            lastPanel = 3;
                            lastComponent = guiContext.form.tableCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "treeSystemTwo":
                            lastFocused = 6;
                            lastPanel = 4;
                            lastComponent = guiContext.form.treeSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                        case "tableSystemTwo":
                            lastFocused = 7;
                            lastPanel = 4;
                            lastComponent = guiContext.form.tableSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                    }
                    //logger.info("focused on " + name + ", index " + lastFocused + ", panel " + lastPanel);
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent)
            {
            }
        };
        component.addFocusListener(focusListener);
        component.setFocusTraversalKeysEnabled(false);
    }

    private void addMouseListenerToTable(JTable table)
    {
        MouseAdapter tableMouseListener = new MouseAdapter()
        {
            synchronized public void mouseClicked(MouseEvent mouseEvent)
            {
                guiContext.form.labelStatusMiddle.setText("");
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
                    NavTreeUserObject tuo = (NavTreeUserObject) target.getValueAt(row, 1);
                    if (tuo != null)
                    {
                        guiContext.form.textFieldLocation.setText(tuo.getPath());
                        printProperties(tuo);
                        if (mouseEvent.getClickCount() == 2)
                        {
                            if (tuo.isDir)
                            {
                                NavTreeNode node = tuo.node;
                                TreeSelectionEvent evt = new TreeSelectionEvent(node, node.getTreePath(), true, null, null);
                                eventTree.setSelectionPath(node.getTreePath());
                                eventTree.scrollPathToVisible(node.getTreePath());
                            }
                            else
                            {
                                if (tuo.type == NavTreeUserObject.REAL && !tuo.isRemote)
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
                                    JOptionPane.showMessageDialog(guiContext.form, "Cannot launch " + (tuo.isRemote ? "remote " : "") + "item", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                }
            }
        };
        table.addMouseListener(tableMouseListener);

        table.setTransferHandler(navTransferHandler);
    }

    private JComponent getTabComponent(int index)
    {
        JComponent nextComponent = null;
        switch (index)
        {
            case 0:
                nextComponent = guiContext.form.treeCollectionOne;
                break;
            case 1:
                nextComponent = guiContext.form.tableCollectionOne;
                break;
            case 2:
                nextComponent = guiContext.form.treeSystemOne;
                break;
            case 3:
                nextComponent = guiContext.form.tableSystemOne;
                break;
            case 4:
                nextComponent = guiContext.form.treeCollectionTwo;
                break;
            case 5:
                nextComponent = guiContext.form.tableCollectionTwo;
                break;
            case 6:
                nextComponent = guiContext.form.treeSystemTwo;
                break;
            case 7:
                nextComponent = guiContext.form.tableSystemTwo;
                break;
        }
        assert(nextComponent != null);
        return nextComponent;
    }

    public long getFreespace(NavTreeUserObject tuo) throws Exception
    {
        long space;
        if (tuo.isRemote && guiContext.cfg.isRemoteSession())
        {
            // remote subscriber
            space = guiContext.context.clientStty.availableSpace(tuo.path);
        }
        else
        {
            space = Utils.availableSpace(tuo.path);
        }
        return space;
    }

    private boolean initialize()
    {
        navTransferHandler = new NavTransferHandler(guiContext);  // single instance

        JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
        progressBar = new JProgressBar();
        simpleOutput.add(progressBar, BorderLayout.EAST);
        progressBar.setVisible(false);

        printLog(guiContext.cfg.getNavigatorName() + " " + guiContext.cfg.getProgramVersion());
        initializeNavigation();
        initializeBrowserOne();
        initializeBrowserTwo();

        // add browser key listener
        KeyListener browserKeyListener = new KeyListener()
        {
            @Override
            public void keyPressed(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyTyped(KeyEvent keyEvent)
            {
                // handle Ctrl-H to toggle Show Hidden
                if ((keyEvent.getKeyCode() == KeyEvent.VK_H) && (keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0)
                {
                    guiContext.preferences.setHideHiddenFiles(!guiContext.preferences.isHideHiddenFiles());
                    if (guiContext.preferences.isHideHiddenFiles())
                        guiContext.form.menuItemShowHidden.setSelected(false);
                    else
                        guiContext.form.menuItemShowHidden.setSelected(true);

                    refreshTree(guiContext.form.treeCollectionOne);
                    refreshTree(guiContext.form.treeSystemOne);
                    refreshTree(guiContext.form.treeCollectionTwo);
                    refreshTree(guiContext.form.treeSystemTwo);
                }
                else if ((keyEvent.getKeyChar() == KeyEvent.VK_TAB) && keyEvent.getModifiers() == 0)
                {
                    navTabKey(true);
                }
                else if ((keyEvent.getKeyChar() == KeyEvent.VK_TAB) && (keyEvent.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
                {
                    navTabKey(false);
                }
            }
        };
        guiContext.form.treeCollectionOne.addKeyListener(browserKeyListener);
        guiContext.form.tableCollectionOne.addKeyListener(browserKeyListener);
        guiContext.form.treeSystemOne.addKeyListener(browserKeyListener);
        guiContext.form.tableSystemOne.addKeyListener(browserKeyListener);
        guiContext.form.treeCollectionTwo.addKeyListener(browserKeyListener);
        guiContext.form.tableCollectionTwo.addKeyListener(browserKeyListener);
        guiContext.form.treeSystemTwo.addKeyListener(browserKeyListener);
        guiContext.form.tableSystemTwo.addKeyListener(browserKeyListener);

        // handle mouse back/forward buttons
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() && MouseInfo.getNumberOfButtons() > 3)
        {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event instanceof MouseEvent && MouseInfo.getNumberOfButtons() > 3)
                {
                    MouseEvent mouseEvent = (MouseEvent) event;
                    // if there are more than 5 buttons forward & back are shifted by 2
                    int base = MouseInfo.getNumberOfButtons() > 5 ? 6 : 4;
                    if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED)
                    {
                        if (mouseEvent.getButton() == base)
                        {
                            navBack();
                        }
                        else if (mouseEvent.getButton() == base + 1)
                        {
                            navForward();
                        }
                    }
                }
            }, AWTEvent.MOUSE_EVENT_MASK);
        }

        // set default start location and related data
        initializeStatus(guiContext.form.treeCollectionTwo);
        initializeStatus(guiContext.form.treeCollectionOne); // initial focus on left/top

        return true;
    }

    private void initializeBrowserOne()
    {
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
                        tabStops[0] = 0;
                        tabStops[1] = 1;
                        break;
                    case 1:
                        model = (NavTreeModel) guiContext.form.treeSystemOne.getModel();
                        node = (NavTreeNode) guiContext.form.treeSystemOne.getLastSelectedPathComponent();
                        tabStops[0] = 2;
                        tabStops[1] = 3;
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
            loadCollectionTree(guiContext.form.treeCollectionOne, guiContext.context.publisherRepo, false);
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionOne, "--Open a publisher profile--", false);
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
                node.loadChildren(true);
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
                navStackPush(node);
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        guiContext.form.treeCollectionOne.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.form.treeCollectionOne);
        addMouseListenerToTable(guiContext.form.tableCollectionOne);
        addFocusListener(guiContext.form.tableCollectionOne);

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.home"), false);
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
                node.loadChildren(true);
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
                navStackPush(node);
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        guiContext.form.treeSystemOne.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.form.treeSystemOne);
        addMouseListenerToTable(guiContext.form.tableSystemOne);
        addFocusListener(guiContext.form.tableSystemOne);
    }

    private void initializeBrowserTwo()
    {
        // --- BrowserTwo ------------------------------------------
        //
        // --- tab selection handler
        guiContext.form.tabbedPaneBrowserTwo.addChangeListener(new ChangeListener()
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
                        model = (NavTreeModel) guiContext.form.treeCollectionTwo.getModel();
                        node = (NavTreeNode) guiContext.form.treeCollectionTwo.getLastSelectedPathComponent();
                        tabStops[2] = 4;
                        tabStops[3] = 5;
                        break;
                    case 1:
                        model = (NavTreeModel) guiContext.form.treeSystemTwo.getModel();
                        node = (NavTreeNode) guiContext.form.treeSystemTwo.getLastSelectedPathComponent();
                        tabStops[2] = 6;
                        tabStops[3] = 7;
                        break;
                }
                if (node == null)
                    node = (NavTreeNode) model.getRoot();
                node.loadStatus();
            }
        });

        // --- treeCollectionTwo
        guiContext.form.treeCollectionTwo.setName("treeCollectionTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadCollectionTree(guiContext.form.treeCollectionTwo, guiContext.context.subscriberRepo, guiContext.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber profile--", guiContext.cfg.isRemoteSession());
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
                node.loadChildren(true);
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
                navStackPush(node);
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        guiContext.form.treeCollectionTwo.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.form.treeCollectionTwo);
        addMouseListenerToTable(guiContext.form.tableCollectionTwo);
        addFocusListener(guiContext.form.tableCollectionTwo);

        // --- treeSystemTwo
        guiContext.form.treeSystemTwo.setName("treeSystemTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadSystemTree(guiContext.form.treeSystemTwo, "/", guiContext.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber profile--", guiContext.cfg.isRemoteSession());
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
                node.loadChildren(true);
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
                navStackPush(node);
                if (!node.isLoaded())
                    node.loadChildren(true);
                else
                    node.loadTable();
            }
        });
        guiContext.form.treeSystemTwo.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.form.treeSystemTwo);
        addMouseListenerToTable(guiContext.form.tableSystemTwo);
        addFocusListener(guiContext.form.tableSystemTwo);
    }

    private void initializeNavigation()
    {
        guiContext.form.buttonBack.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navBack();
            }
        });

        guiContext.form.buttonForward.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navForward();
            }
        });
    }

    private void initializeStatus(JTree tree)
    {
        NavTreeModel model = (NavTreeModel) tree.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        root.loadStatus();
        root.selectMe();
    }

    private void loadCollectionTree(JTree tree, Repository repo, boolean remote)
    {
        try
        {
            NavTreeNode root = setCollectionRoot(tree, repo.getLibraryData().libraries.description, remote);
            Arrays.sort(repo.getLibraryData().libraries.bibliography);
            switch (styleOne)
            {
                case STYLE_COLLECTION_ALL:
                    styleCollectionAll(tree, repo, remote);
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

    private void loadSystemTree(JTree tree, String initialLocation, boolean remote)
    {
        try
        {
            NavTreeNode root = null;
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    root = styleSystemAll(tree, initialLocation, remote);
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

    private void navBack()
    {
        NavTreeNode node = navStackPop();
        if (node != null)
        {
            node.selectMyTab();
            node.selectMe();
        }
    }

    private void navForward()
    {
        if (navStackIndex + 1 <= navStack.lastIndexOf(navStack.lastElement()))
        {
            ++navStackIndex;
        }
        NavTreeNode node = navStack.get(navStackIndex);
        if (node != null)
        {
            node.selectMyTab();
            node.selectMe();
        }
    }

    private NavTreeNode navStackPop()
    {
        NavTreeNode node;
        if (navStackIndex > 0)
        {
            --navStackIndex;
            node = navStack.get(navStackIndex);
        }
        else
            node = (navStackIndex > -1) ? navStack.get(0) : null;
        return node;
    }

    private void navStackPush(NavTreeNode node)
    {
        if (navStackIndex < 0 || navStack.get(navStackIndex) != node)
        {
            if (navStackIndex > -1)
                navStack.setSize(navStackIndex + 1); // truncate anything beyond this index
            navStack.push(node);
            ++navStackIndex;
        }
    }

    private void navTabKey(boolean forward)
    {
        int next;
        JComponent nextComponent = null;
        if (forward)
        {
            ++tabStop;
            if (tabStop >= tabStops.length)
                tabStop = 0;
        }
        else
        {
            --tabStop;
            if (tabStop < 0)
                tabStop = tabStops.length - 1;
        }
        next = tabStops[tabStop];
        nextComponent = getTabComponent(next);
        nextComponent.requestFocus();
    }

    public void printLog(String text, boolean isError)
    {
        if (isError)
        {
            logger.error(text);
            guiContext.form.textAreaLog.append("ERROR: " + text + System.getProperty("line.separator"));
        }
        else
            printLog(text);
    }

    public void printLog(String text)
    {
        logger.info(text);
        guiContext.form.textAreaLog.append(text + System.getProperty("line.separator"));
    }

    public void printProperties(NavTreeUserObject tuo)
    {
        guiContext.form.textAreaProperties.setText("");
        guiContext.form.textAreaProperties.append("Type: " + tuo.getType() + System.getProperty("line.separator"));
        try
        {
            switch (tuo.type)
            {
                case NavTreeUserObject.BOOKMARKS:
                    break;
                case NavTreeUserObject.COLLECTION:
                    guiContext.form.textAreaProperties.append("Libraries: " + tuo.node.getChildCount(false, false) + System.getProperty("line.separator"));
                    break;
                case NavTreeUserObject.COMPUTER:
                    guiContext.form.textAreaProperties.append("Drives: " + tuo.node.getChildCount(false, false) + System.getProperty("line.separator"));
                    break;
                case NavTreeUserObject.DRIVE:
                    guiContext.form.textAreaProperties.append("Free: " + Utils.formatLong(getFreespace(tuo), true) + System.getProperty("line.separator"));
                    break;
                case NavTreeUserObject.HOME:
                    guiContext.form.textAreaProperties.append("Free: " + Utils.formatLong(getFreespace(tuo), true) + System.getProperty("line.separator"));
                    break;
                case NavTreeUserObject.LIBRARY:
                    for (String source : tuo.sources)
                    {
                        guiContext.form.textAreaProperties.append("Location: " + source + System.getProperty("line.separator"));
                    }
                    break;
                case NavTreeUserObject.REAL:
                    if (tuo.file != null && Files.exists(tuo.file.toPath()))
                    {
                        guiContext.form.textAreaProperties.append("Path: " + tuo.path + System.getProperty("line.separator"));
                        if (tuo.isRemote)
                            guiContext.form.textAreaProperties.append("Size: " + Utils.formatLong(tuo.size, true) + System.getProperty("line.separator"));
                        else
                            guiContext.form.textAreaProperties.append("Size: " + Utils.formatLong(Files.size(tuo.file.toPath()), true) + System.getProperty("line.separator"));
                        guiContext.form.textAreaProperties.append("isDir: " + tuo.isDir + System.getProperty("line.separator"));
                    }
                    else
                    {
                        guiContext.form.textAreaProperties.setText("");
                    }
                    break;
                case NavTreeUserObject.SYSTEM:
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    public void refreshTree(JTree tree)
    {
        if (tree != null)
        {
            tree.setEnabled(false);
            TreePath rootPath = ((NavTreeNode) tree.getModel().getRoot()).getTreePath();
            Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(rootPath);
            TreePath[] paths = tree.getSelectionPaths();
            tree.setExpandsSelectedPaths(true);
            ((NavTreeModel) tree.getModel()).reload();
            while (expandedDescendants.hasMoreElements())
            {
                TreePath tp = expandedDescendants.nextElement();
                tree.expandPath(tp);
            }
            tree.setSelectionPaths(paths);
            ((NavTreeNode)tree.getModel().getRoot()).sort();
            tree.setEnabled(true);
        }
    }

    private NavTreeNode setCollectionRoot(JTree tree, String title, boolean remote)
    {
        NavTreeNode root = new NavTreeNode(guiContext, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, title, NavTreeUserObject.COLLECTION, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(guiContext.preferences.isHideFilesInTree());
        tree.setCellRenderer(new NavTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setModel(model);
        return root;
    }

    private void styleCollectionAll(JTree tree, Repository repo, boolean remote) throws Exception
    {
        NavTreeModel model = (NavTreeModel) tree.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            NavTreeNode node = new NavTreeNode(guiContext, tree);
            NavTreeUserObject tuo = new NavTreeUserObject(node, lib.name, lib.sources, remote);
            node.setNavTreeUserObject(tuo);
            root.add(node);
            node.loadChildren(false);
        }
        root.setLoaded(true);
    }

    private NavTreeNode styleSystemAll(JTree tree, String initialLocation, boolean remote) throws Exception
    {
        /*
         * Computer
         *   Drive /
         * Home
         *   Directory trh
        */
        // TODO Change to externalized strings

        // setup new invisible root for Computer, Home & Bookmarks
        NavTreeNode root = new NavTreeNode(guiContext, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, "System", NavTreeUserObject.SYSTEM, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(guiContext.preferences.isHideFilesInTree());
        tree.setShowsRootHandles(true);
//        tree.setRootVisible(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setCellRenderer(new NavTreeCellRenderer());
        tree.setModel(model);

        // add Computer node
        NavTreeNode rootNode = new NavTreeNode(guiContext, tree);
        tuo = new NavTreeUserObject(rootNode, "Computer", NavTreeUserObject.COMPUTER, remote);
        rootNode.setNavTreeUserObject(tuo);
        root.add(rootNode);
        if (remote && tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            NavTreeNode node = new NavTreeNode(guiContext, tree);
            tuo = new NavTreeUserObject(node, "/", "/", NavTreeUserObject.DRIVE, remote);
            node.setNavTreeUserObject(tuo);
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
                NavTreeNode node = new NavTreeNode(guiContext, tree);
                tuo = new NavTreeUserObject(node, drive.getPath(), drive.getAbsolutePath(), NavTreeUserObject.DRIVE, false);
                node.setNavTreeUserObject(tuo);
                rootNode.add(node);
                node.loadChildren(false);
            }
        }
        rootNode.setLoaded(true);

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            NavTreeNode homeNode = new NavTreeNode(guiContext, tree);
            tuo = new NavTreeUserObject(homeNode, "Home", System.getProperty("user.home"), NavTreeUserObject.HOME, false);
            homeNode.setNavTreeUserObject(tuo);
            root.add(homeNode);
            homeNode.loadChildren(false);
        }

        root.setLoaded(true);
        return root;
    }
}
