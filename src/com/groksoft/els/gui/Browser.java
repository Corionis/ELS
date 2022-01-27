package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Stack;

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
    //
    public JComponent lastComponent = null;
    public int lastTab = 0;
    public NavTransferHandler navTransferHandler;
    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private GuiContext guiContext;
    public boolean trackingHints = false;
    private Color hintTrackingColor;
    private String keyBuffer = "";
    private long keyTime = 0L;
    private transient Logger logger = LogManager.getLogger("applog");
    private Stack<NavTreeNode> navStack = new Stack<>();
    private int navStackIndex = -1;
    private String os;
    private JProgressBar progressBar;
    private int tabStop = 0;
    private int[] tabStops = {0, 1, 4, 5};

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
                            lastTab = 0;
                            lastComponent = guiContext.form.treeCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "tableCollectionOne":
                            lastTab = 1;
                            lastComponent = guiContext.form.tableCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "treeSystemOne":
                            lastTab = 0;
                            lastComponent = guiContext.form.treeSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "tableSystemOne":
                            lastTab = 1;
                            lastComponent = guiContext.form.tableSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "treeCollectionTwo":
                            lastTab = 2;
                            lastComponent = guiContext.form.treeCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "tableCollectionTwo":
                            lastTab = 3;
                            lastComponent = guiContext.form.tableCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "treeSystemTwo":
                            lastTab = 2;
                            lastComponent = guiContext.form.treeSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                        case "tableSystemTwo":
                            lastTab = 3;
                            lastComponent = guiContext.form.tableSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                    }
                    //logger.info("focused on " + name + ", index " + lastFocused + ", panel " + lastPanel);
                    NavTreeUserObject tuo = getSelectedUserObject(active);
                    if (tuo != null)
                    {
                        guiContext.form.textFieldLocation.setText(tuo.getPath());
                        printProperties(tuo);
                    }
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

    private void addHandlersToTable(JTable table)
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
                                    JOptionPane.showMessageDialog(guiContext.form, "Launch of " + (tuo.isRemote ? "remote " : "") + "items not supported", guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                                }
                            }
                        }
                    }
                }
            }
        };

        table.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent keyEvent)
            {
                super.keyReleased(keyEvent);
                if (keyEvent.getKeyCode() == KeyEvent.VK_UP || keyEvent.getKeyCode() == KeyEvent.VK_DOWN)
                {
                    JTree tree = null;
                    NavTreeUserObject tuo = null;
                    Object object = keyEvent.getSource();
                    if (object instanceof JTable)
                    {
                        int[] rows = {0};
                        tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
                        rows = ((JTable) object).getSelectedRows();
                        if (rows.length > 0)
                        {
                            tuo = (NavTreeUserObject) ((JTable) object).getValueAt(rows[0], 1);
                        }
                        else
                            return;
                        tuo.node.loadStatus();
                    }
                }
            }
        });
        table.addMouseListener(tableMouseListener);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
        table.getActionMap().put("Enter", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                // do nothing on JTable Enter pressed
            }
        });

        table.setTransferHandler(navTransferHandler);
    }

    private void addKeyListener()
    {
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
                // handle F2 Rename
                if (keyEvent.getKeyCode() == KeyEvent.VK_F2 && keyEvent.getModifiers() == 0)
                {
                    for (ActionListener listener : guiContext.form.menuItemRename.getActionListeners())
                    {
                        listener.actionPerformed(new ActionEvent(keyEvent.getSource(), ActionEvent.ACTION_PERFORMED, null));
                    }
                }

                // handle F5 Refresh
                if (keyEvent.getKeyCode() == KeyEvent.VK_F5 && keyEvent.getModifiers() == 0)
                {
                    rescanByObject(keyEvent.getSource());
                }
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

                // handle Ctrl-R to Refresh current selection
                else if (keyEvent.getKeyChar() == KeyEvent.VK_ALT && (keyEvent.getModifiers() & KeyEvent.CTRL_MASK) != 0)
                {
                    refreshByObject(keyEvent.getSource());
                }

                // handle Tab forward and backward
                else if ((keyEvent.getKeyChar() == KeyEvent.VK_TAB) && keyEvent.getModifiers() == 0)
                {
                    navTabKey(true);
                }
                else if ((keyEvent.getKeyChar() == KeyEvent.VK_TAB) && (keyEvent.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
                {
                    navTabKey(false);
                }

                // handle ENTER key
                else if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER)
                {
                    JTree tree = null;
                    NavTreeUserObject tuo = null;
                    Object object = keyEvent.getSource();
                    if (object instanceof JTree)
                    {
                        tree = (JTree) object;
                        TreePath[] paths = tree.getSelectionPaths();
                        if (paths.length > 0)
                        {
                            NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                            tuo = ntn.getUserObject();
                        }
                        else if (paths.length == 0)
                            return;
                    }
                    else if (object instanceof JTable)
                    {
                        int[] rows = {0};
                        tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
                        rows = ((JTable) object).getSelectedRows();
                        if (rows.length > 0)
                        {
                            tuo = (NavTreeUserObject) ((JTable) object).getValueAt(rows[0], 1);
                        }
                        else if (rows.length == 0)
                            return;
                    }
                    if (tuo.isDir)
                    {
                        tree.expandPath(tuo.node.getTreePath());
                        tree.setSelectionPath(tuo.node.getTreePath());
                        tree.scrollPathToVisible(tuo.node.getTreePath());
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
                            JOptionPane.showMessageDialog(guiContext.form, "Launch of " + (tuo.isRemote ? "remote " : "") + "items not supported", guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }

                // handle printable character speed search in tables (only)
                else if ((keyEvent.getModifiers() & KeyEvent.CTRL_MASK) == 0 &&
                        (keyEvent.getModifiers() & KeyEvent.ALT_MASK) == 0)
                {
                    char c = keyEvent.getKeyChar();
                    if (c >= 32 && c <= 127)
                    {
                        if (keyEvent.getSource() instanceof JTable)
                        {
                            JTable table = (JTable) keyEvent.getSource();
                            // reset the buffer after 2 seconds
                            if (keyTime == 0 || (keyEvent.getWhen() - keyTime > 2000))
                            {
                                keyBuffer = "";
                                keyTime = keyEvent.getWhen();
                            }
                            keyBuffer += c;
                            int index = findRowIndex(table, keyBuffer);
                            if (index >= 0)
                            {
                                table.setRowSelectionInterval(index, index);
                                table.scrollRectToVisible(new Rectangle((table.getCellRect(index, 0, true))));
                            }

                        }
                    }
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
    }

    public void deleteSelected(JTable sourceTable)
    {
        int row = sourceTable.getSelectedRow();
        if (row > -1)
        {
            int dirCount = 0;
            int fileCount = 0;
            boolean isRemote = false;
            long size = 0L;
            int[] rows = sourceTable.getSelectedRows();
            for (int i = 0; i < rows.length; ++i)
            {
                NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                if (tuo.type != NavTreeUserObject.REAL)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Cannot delete " + tuo.name, guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren();
                    fileCount += tuo.node.deepGetFileCount();
                    size += tuo.node.deepGetFileSize();
                }
                else
                {
                    ++fileCount;
                    size += tuo.size;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            if (guiContext.preferences.isShowConfirmations())
            {
                reply = JOptionPane.showConfirmDialog(guiContext.form, "Are you sure you want to delete " +
                                rows.length + (isRemote ? " remote" : "") + " item" + (rows.length > 1 ? "s" : "") + " containing " +
                                fileCount + " file" + (fileCount > 1 ? "s" : "") + ", " + Utils.formatLong(size, false) +
                                (dirCount > 0 ? " and the content of " + (dirCount > 1 ? "all directories" : "the directory") : "") +
                                (guiContext.cfg.isDryRun() ? " (dry-run)?" : "?"),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            boolean errored = false;
            if (reply == JOptionPane.YES_OPTION)
            {
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        if (tuo.isDir)
                        {
                            if (!navTransferHandler.removeDirectory(tuo))
                            {
                                errored = true;
                                break;
                            }
                        }
                        else
                        {
                            if (!navTransferHandler.removeFile(tuo))
                            {
                                errored = true;
                                break;
                            }
                        }
                        if (!errored && guiContext.browser.trackingHints)
                        {
                            try
                            {
                                navTransferHandler.exportHint("rm", tuo, null);
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.form, "Error writing Hint:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog("Skipping " + tuo.name);
                    }
                }
                if (!errored)
                {
                    NavTreeNode parent = null;
                    for (int i = rows.length - 1; i > -1; --i)
                    {
                        NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                        if (tuo.type == NavTreeUserObject.REAL)
                        {
                            parent = (NavTreeNode) tuo.node.getParent();
                            parent.remove(tuo.node);
                        }
                    }
                    if (parent != null)
                    {
                        refreshTree(parent.getMyTree());
                        parent.selectMe();
                    }
                }
            }
        }
    }

    public void deleteSelected(JTree sourceTree)
    {
        int row = sourceTree.getLeadSelectionRow();
        if (row > -1)
        {
            int dirCount = 0;
            int fileCount = 0;
            boolean isRemote = false;
            long size = 0L;
            TreePath[] paths = sourceTree.getSelectionPaths();
            for (TreePath path : paths)
            {
                NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                NavTreeUserObject tuo = ntn.getUserObject();
                if (tuo.type != NavTreeUserObject.REAL)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Cannot delete " + tuo.name, guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren();
                    fileCount += tuo.node.deepGetFileCount();
                    size += tuo.node.deepGetFileSize();
                }
                else
                {
                    ++fileCount;
                    size += tuo.size;
                }
            }

            int reply = JOptionPane.YES_OPTION;
            if (guiContext.preferences.isShowConfirmations())
            {
                reply = JOptionPane.showConfirmDialog(guiContext.form, "Are you sure you want to delete " +
                                paths.length + (isRemote ? " remote" : "") + " item" + (paths.length > 1 ? "s" : "") + " containing " +
                                fileCount + " file" + (fileCount > 1 ? "s" : "") + ", " + Utils.formatLong(size, false) +
                                (dirCount > 0 ? " and the content of " + (dirCount > 1 ? "all directories" : "the directory") : "") +
                                (guiContext.cfg.isDryRun() ? " (dry-run)?" : "?"),
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            boolean errored = false;
            if (reply == JOptionPane.YES_OPTION)
            {
                for (TreePath path : paths)
                {
                    NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                    NavTreeUserObject tuo = ntn.getUserObject();
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        if (tuo.isDir)
                        {
                            if (!navTransferHandler.removeDirectory(tuo))
                            {
                                errored = true;
                                break;
                            }
                        }
                        else
                        {
                            if (!navTransferHandler.removeFile(tuo))
                            {
                                errored = true;
                                break;
                            }
                        }
                        if (!errored && guiContext.browser.trackingHints)
                        {
                            try
                            {
                                navTransferHandler.exportHint("rm", tuo, null);
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.form, "Error writing Hint:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog("Skipping " + tuo.name);
                    }
                }
                if (!errored)
                {
                    NavTreeNode parent = null;
                    for (int i = paths.length - 1; i > -1; --i)
                    {
                        TreePath path = paths[i];
                        NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                        NavTreeUserObject tuo = ntn.getUserObject();
                        if (tuo.type == NavTreeUserObject.REAL)
                        {
                            parent = (NavTreeNode) tuo.node.getParent();
                            parent.remove(tuo.node);
                        }
                    }
                    if (parent != null)
                    {
                        refreshTree(parent.getMyTree());
                        parent.selectMe();
                    }
                }
            }
        }
    }

    public int findRowIndex(JTable table, String name)
    {
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            NavTreeUserObject rowTuo = (NavTreeUserObject) table.getValueAt(i, 1);
            if (rowTuo.name.toLowerCase().startsWith(name.toLowerCase()))
                return i;
        }
        return -1;
    }

    public int findRowIndex(JTable table, NavTreeUserObject tuo)
    {
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            NavTreeUserObject rowTuo = (NavTreeUserObject) table.getValueAt(i, 1);
            if (rowTuo.path.equals(tuo.path))
                return i;
        }
        return -1;
    }

    public long getFreespace(NavTreeUserObject tuo) throws Exception
    {
        return getFreespace(tuo.path, tuo.isRemote);
    }

    public long getFreespace(String path, boolean isRemote) throws Exception
    {
        long space;
        if (isRemote && guiContext.cfg.isRemoteSession())
        {
            // remote subscriber
            space = guiContext.context.clientStty.availableSpace(path);
        }
        else
        {
            space = Utils.availableSpace(path);
        }
        return space;

    }

    public Color getHintTrackingColor()
    {
        return hintTrackingColor;
    }

    public NavTreeUserObject getSelectedUserObject(Object object)
    {
        int[] rows = {0};
        JTree tree = null;
        NavTreeUserObject tuo = null;
        if (object instanceof JTable)
        {
            tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
            rows = ((JTable) object).getSelectedRows();
            if (rows.length > 0)
            {
                tuo = (NavTreeUserObject) ((JTable) object).getValueAt(rows[0], 1);
            }
        }
        if (object instanceof JTree || (tree != null && tuo == null))
        {
            if (tree == null)
                tree = (JTree) object;
            TreePath[] paths = tree.getSelectionPaths();
            if (paths != null && paths.length > 0)
            {
                NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                tuo = ntn.getUserObject();
            }
        }
        return tuo;
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
        assert (nextComponent != null);
        return nextComponent;
    }

    private boolean initialize()
    {
        navTransferHandler = new NavTransferHandler(guiContext);  // single instance

        JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
        progressBar = new JProgressBar();
        simpleOutput.add(progressBar, BorderLayout.EAST);
        progressBar.setVisible(false);

        printLog(guiContext.cfg.getNavigatorName() + " " + guiContext.cfg.getProgramVersion());
        initializeToolbar();
        initializeNavigation();
        initializeBrowserOne();
        initializeBrowserTwo();
        addKeyListener();

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

        // handle setting the size of the bottom window using the divider location
        guiContext.form.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);

                int whole = guiContext.form.splitPaneBrowser.getHeight();
                int divider = guiContext.form.splitPaneBrowser.getDividerSize();
                int pos = whole - divider - guiContext.preferences.getBrowserBottomSize();
                guiContext.form.splitPaneBrowser.setDividerLocation(pos);
            }
        });
        //
        guiContext.form.tabbedPaneNavigatorBottom.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                guiContext.preferences.setBrowserBottomSize(componentEvent.getComponent().getHeight());
            }
        });

        // set default start location and related data
        initializeStatus(guiContext.form.treeCollectionTwo);
        initializeStatus(guiContext.form.treeCollectionOne);

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
            setCollectionRoot(guiContext.form.treeCollectionOne, "--Open a publisher library--", false);
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
        addHandlersToTable(guiContext.form.tableCollectionOne);
        addFocusListener(guiContext.form.tableCollectionOne);

        // --- treeSystemOne
        guiContext.form.treeSystemOne.setName("treeSystemOne");
        if (guiContext.context.publisherRepo != null && guiContext.context.publisherRepo.isInitialized())
        {
            loadSystemTree(guiContext.form.treeSystemOne, false);
        }
        else
        {
            setCollectionRoot(guiContext.form.treeSystemOne, "--Open a publisher library--", false);
        }
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
        addHandlersToTable(guiContext.form.tableSystemOne);
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
                TreeModel model = null;
                NavTreeNode node = null;
                switch (pane.getSelectedIndex())
                {
                    case 0:
                        model = guiContext.form.treeCollectionTwo.getModel();
                        node = (NavTreeNode) guiContext.form.treeCollectionTwo.getLastSelectedPathComponent();
                        tabStops[2] = 4;
                        tabStops[3] = 5;
                        break;
                    case 1:
                        model = guiContext.form.treeSystemTwo.getModel();
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
            setCollectionRoot(guiContext.form.treeCollectionTwo, "--Open a subscriber library--", guiContext.cfg.isRemoteSession());
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
        addHandlersToTable(guiContext.form.tableCollectionTwo);
        addFocusListener(guiContext.form.tableCollectionTwo);

        // --- treeSystemTwo
        guiContext.form.treeSystemTwo.setName("treeSystemTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadSystemTree(guiContext.form.treeSystemTwo, guiContext.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(guiContext.form.treeSystemTwo, "--Open a subscriber library--", guiContext.cfg.isRemoteSession());
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
        addHandlersToTable(guiContext.form.tableSystemTwo);
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

    private void initializeToolbar()
    {
        guiContext.form.buttonNewFolder.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                ActionListener[] listeners = guiContext.form.menuItemNewFolder.getActionListeners();
                for (ActionListener listener : listeners)
                {
                    listener.actionPerformed(actionEvent);
                }
            }
        });

        //if (guiContext.navigator.showHintTrackingButton)
        {
            guiContext.form.buttonHintTracking.addActionListener(new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    if (!trackingHints)
                    {
                        try
                        {
                            guiContext.form.buttonHintTracking.setBackground(new Color(Integer.parseInt(guiContext.preferences.getHintTrackingColor(), 16)));
                            URL url = Thread.currentThread().getContextClassLoader().getResource("hint-tracking.png");
                            Image icon = ImageIO.read(url);
                            guiContext.form.buttonHintTracking.setIcon(new ImageIcon(icon));
                        }
                        catch (Exception e)
                        {
                        }
                    }
                    else
                    {
                        guiContext.form.buttonHintTracking.setBackground(hintTrackingColor);
                        guiContext.form.buttonHintTracking.setIcon(null);
                    }
                    trackingHints = !trackingHints;
                }
            });
            hintTrackingColor = guiContext.form.buttonHintTracking.getBackground();
        }
        //else
        if (!guiContext.navigator.showHintTrackingButton)
        {
            guiContext.form.buttonHintTracking.setVisible(false);
        }
    }

    public void loadCollectionTree(JTree tree, Repository repo, boolean remote)
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

    public void loadSystemTree(JTree tree, boolean remote)
    {
        try
        {
            NavTreeNode root = null;
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    root = styleSystemAll(tree, remote);
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
        if (navStackIndex > 1)
        {
            --navStackIndex;
            node = navStack.get(navStackIndex);
        }
        else
            node = (navStackIndex > -1) ? navStack.get(1) : null;
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
        tabStop = lastTab;
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
        String msg = "<html>";
        msg += "<style>table { margin:0; padding:0; }" +
                "th { margin:0; padding:0; }" +
                "td { text-align:left; }" +
                "</style>";
        msg += "<body>";
        msg += "Type: " + tuo.getType() + "<br/>" + System.getProperty("line.separator");

        try
        {
            switch (tuo.type)
            {
                case NavTreeUserObject.BOOKMARKS:
                    break;
                case NavTreeUserObject.COLLECTION:
                    msg += "Libraries: " + tuo.node.getChildCount(false, false) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.COMPUTER:
                    msg += "Drives: " + tuo.node.getChildCount(false, false) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.DRIVE:
                    msg += "Free: " + Utils.formatLong(getFreespace(tuo), true) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.HOME:
                    msg += "Free: " + Utils.formatLong(getFreespace(tuo), true) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.LIBRARY:
                    msg += "<table cellpadding=\"0\" cellspacing=\"0\">" +
                            "<tr><td>Location:</td> <td></td> <td>Free:</td></tr>";
                    for (String source : tuo.sources)
                    {
                        String free = Utils.formatLong(getFreespace(source, tuo.isRemote), true);
                        msg += "<tr><td>" + source + "</td> <td><div>&nbsp;&nbsp;&nbsp;&nbsp;</div></td> <td>" + free + "</td></tr>";
                    }
                    msg += "</table>";
                    msg += System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.REAL:
                    msg += "Path: " + tuo.path + "<br/>" + System.getProperty("line.separator");
                    msg += "Size: " + Utils.formatLong(tuo.size, true) + "<br/>" + System.getProperty("line.separator");
                    //msg += "isDir: " + tuo.isDir + "<br/>" + System.getProperty("line.separator");
                    if (tuo.path.endsWith(".els"))
                    {
                        String content = guiContext.context.transfer.readTextFile(tuo);
                        if (content.length() > 0)
                        {
                            content = content.replaceAll("\r\n", "<br/>" + System.getProperty("line.separator"));
                            content = content.replaceAll("\n", "<br/>" + System.getProperty("line.separator"));
                            content = content.replaceAll("\r", "<br/>" + System.getProperty("line.separator"));
                        }
                        msg += "<hr>" + System.getProperty("line.separator");
                        msg += content;
                    }
                    break;
                case NavTreeUserObject.SYSTEM:
                    break;
            }
            msg += "</body></html>";
            guiContext.form.textAreaProperties.setText(msg);
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    public void refreshByObject(Object object)
    {
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            refreshTree(sourceTree);
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            JTree sourceTree = ((BrowserTableModel) sourceTable.getModel()).getNode().getMyTree();
            refreshTree(sourceTree);
        }
    }

    public void refreshAll()
    {
        refreshTree(guiContext.form.treeCollectionOne);
        refreshTree(guiContext.form.treeSystemOne);
        refreshTree(guiContext.form.treeCollectionTwo);
        refreshTree(guiContext.form.treeSystemTwo);
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
            if (expandedDescendants != null)
            {
                while (expandedDescendants.hasMoreElements())
                {
                    TreePath tp = expandedDescendants.nextElement();
                    tree.expandPath(tp);
                }
            }
            tree.setSelectionPaths(paths);
            ((NavTreeNode) tree.getModel().getRoot()).sort();
            tree.setEnabled(true);
        }
    }

    public void rescanByObject(Object object)
    {
        JTree sourceTree = null;
        Object sel = null;
        if (object instanceof JTree)
        {
            sourceTree = (JTree) object;
            sel = sourceTree.getLastSelectedPathComponent();
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            sourceTree = ((BrowserTableModel) sourceTable.getModel()).getNode().getMyTree();
            sel = sourceTree.getLastSelectedPathComponent();
        }
        if (sel != null)
        {
            sourceTree.collapsePath(((NavTreeNode) sel).getTreePath());
            ((NavTreeNode) sel).setRefresh(true);
            ((NavTreeNode) sel).loadChildren(true);
            refreshTree(sourceTree);
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

    public void setHintTrackingColor(Color hintTrackingColor)
    {
        this.hintTrackingColor = hintTrackingColor;
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

    private NavTreeNode styleSystemAll(JTree tree, boolean remote) throws Exception
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
