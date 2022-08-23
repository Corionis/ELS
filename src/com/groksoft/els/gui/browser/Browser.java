package com.groksoft.els.gui.browser;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.*;
import com.groksoft.els.gui.bookmarks.Bookmark;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import org.apache.commons.io.FilenameUtils;
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
import java.net.SocketException;
import java.net.URL;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

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
    public boolean trackingHints = false;
    private GuiContext guiContext;
    private Color hintTrackingColor;
    private String keyBuffer = "";
    private long keyTime = 0L;
    private transient Logger logger = LogManager.getLogger("applog");
    private Stack<NavItem>[] navStack = new Stack[4];
    private int[] navStackIndex = { -1, -1, -1, -1 };
    private String os;
    private int tabStop = 0;
    private int[] tabStops = {0, 1, 4, 5};

    public Browser(GuiContext gctx)
    {
        this.guiContext = gctx;
        this.guiContext.browser = this;
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
                            lastComponent = guiContext.mainFrame.treeCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "tableCollectionOne":
                            lastTab = 1;
                            lastComponent = guiContext.mainFrame.tableCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "treeSystemOne":
                            lastTab = 0;
                            lastComponent = guiContext.mainFrame.treeSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "tableSystemOne":
                            lastTab = 1;
                            lastComponent = guiContext.mainFrame.tableSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "treeCollectionTwo":
                            lastTab = 2;
                            lastComponent = guiContext.mainFrame.treeCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "tableCollectionTwo":
                            lastTab = 3;
                            lastComponent = guiContext.mainFrame.tableCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "treeSystemTwo":
                            lastTab = 2;
                            lastComponent = guiContext.mainFrame.treeSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                        case "tableSystemTwo":
                            lastTab = 3;
                            lastComponent = guiContext.mainFrame.tableSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                    }

                    NavTreeUserObject tuo = getSelectedUserObject(active);
                    if (tuo != null)
                    {
                        guiContext.mainFrame.textFieldLocation.setText(tuo.getPath());
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
                guiContext.mainFrame.labelStatusMiddle.setText("");
                JTable target = (JTable) mouseEvent.getSource();
                target.requestFocus();
                JTree eventTree = null;
                switch (target.getName())
                {
                    case "tableCollectionOne":
                        eventTree = guiContext.mainFrame.treeCollectionOne;
                        break;
                    case "tableSystemOne":
                        eventTree = guiContext.mainFrame.treeSystemOne;
                        break;
                    case "tableCollectionTwo":
                        eventTree = guiContext.mainFrame.treeCollectionTwo;
                        break;
                    case "tableSystemTwo":
                        eventTree = guiContext.mainFrame.treeSystemTwo;
                        break;
                }
                int row = target.getSelectedRow();
                if (row >= 0)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) target.getValueAt(row, 1);
                    if (tuo != null)
                    {
                        boolean doubleClick = (mouseEvent.getClickCount() == 2);
                        guiContext.mainFrame.textFieldLocation.setText(tuo.getPath());
                        printProperties(tuo);
                        if (doubleClick)
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
                                        JOptionPane.showMessageDialog(guiContext.mainFrame,
                                                guiContext.cfg.gs("Browser.error.launching.item"),
                                                guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Browser.launch.of") +
                                                    (tuo.isRemote ? guiContext.cfg.gs("Z.remote.lowercase") : "") +
                                                    guiContext.cfg.gs("Browser.launch.of.items.not.supported"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                    for (ActionListener listener : guiContext.mainFrame.menuItemRename.getActionListeners())
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
                        guiContext.mainFrame.menuItemShowHidden.setSelected(false);
                    else
                        guiContext.mainFrame.menuItemShowHidden.setSelected(true);

                    refreshTree(guiContext.mainFrame.treeCollectionOne);
                    refreshTree(guiContext.mainFrame.treeSystemOne);
                    refreshTree(guiContext.mainFrame.treeCollectionTwo);
                    refreshTree(guiContext.mainFrame.treeSystemTwo);
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
                    if (object instanceof JTextField)
                    {
//                        JTextField field = (JTextField) object;
//                        if (field.getName() != null && field.getName().equals("location"))
//                        {
//                            JOptionPane.showMessageDialog(guiContext.form,
//                                    "change location",
//                                    guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
//                        }
                    }
                    else
                    {
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
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Browser.error.launching.item"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                            else
                            {
                                JOptionPane.showMessageDialog(guiContext.mainFrame,
                                        guiContext.cfg.gs("Browser.launch.of") +
                                                (tuo.isRemote ? guiContext.cfg.gs("Z.remote.lowercase") : "") +
                                                guiContext.cfg.gs("Browser.launch.of.items.not.supported"),
                                        guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                            }
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
        guiContext.mainFrame.treeCollectionOne.addKeyListener(browserKeyListener);
        guiContext.mainFrame.tableCollectionOne.addKeyListener(browserKeyListener);
        guiContext.mainFrame.treeSystemOne.addKeyListener(browserKeyListener);
        guiContext.mainFrame.tableSystemOne.addKeyListener(browserKeyListener);
        guiContext.mainFrame.treeCollectionTwo.addKeyListener(browserKeyListener);
        guiContext.mainFrame.tableCollectionTwo.addKeyListener(browserKeyListener);
        guiContext.mainFrame.treeSystemTwo.addKeyListener(browserKeyListener);
        guiContext.mainFrame.tableSystemTwo.addKeyListener(browserKeyListener);
    }

    private void bookmarkCreate(NavTreeNode node, String name, String panelName)
    {
        Repository repo = node.getUserObject().getRepo();
        if (repo != null)
        {
            Object obj = JOptionPane.showInputDialog(guiContext.mainFrame,
                    repo.getLibraryData().libraries.description + " " + guiContext.cfg.gs(("Browser.bookmark.name")),
                    guiContext.cfg.gs("Browser.add.bookmark"), JOptionPane.QUESTION_MESSAGE,
                    null, null, name);
            name = (String) obj;
            if (name != null && name.length() > 0)
            {
                Bookmark bm = new Bookmark();
                bm.name = name;
                bm.panel = panelName;
                TreePath tp = node.getTreePath();
                if (tp.getPathCount() > 0)
                {
                    bm.pathElements = new String[tp.getPathCount()];
                    Object[] objs = tp.getPath();
                    for (int i = 0; i < tp.getPathCount(); ++i)
                    {
                        node = (NavTreeNode) objs[i];
                        bm.pathElements[i] = node.getUserObject().name;
                    }
                    guiContext.navigator.bookmarks.add(bm);
                    try
                    {
                        guiContext.navigator.bookmarks.write();
                        guiContext.navigator.loadBookmarksMenu();
                    }
                    catch (Exception e)
                    {
                        guiContext.browser.printLog(Utils.getStackTrace(e), true);
                        JOptionPane.showMessageDialog(guiContext.mainFrame,
                                guiContext.cfg.gs("Browser.error.saving.bookmarks") + e.getMessage(),
                                guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    /**
     * Navigate to and select a bookmark
     *
     * @param bookmark
     */
    public void bookmarkGoto(Bookmark bookmark)
    {
        String panelName = bookmark.panel.toLowerCase();

        // determine which
        JTree tree;
        if (panelName.endsWith("one")) // publisher
            tree = panelName.contains("collection") ? guiContext.mainFrame.treeCollectionOne : guiContext.mainFrame.treeSystemOne;
        else // subscriber
            tree = panelName.contains("collection") ? guiContext.mainFrame.treeCollectionTwo : guiContext.mainFrame.treeSystemTwo;

        NavTreeNode node = (NavTreeNode) tree.getModel().getRoot();
        // root should be first path element
        if (node.getUserObject().name.equals(bookmark.pathElements[0]))
        {
            // select the Browser tab and one of it's 8 panels
            guiContext.mainFrame.tabbedPaneMain.setSelectedIndex(0);
            int panelNo = guiContext.browser.getPanelNumber(bookmark.panel);
            guiContext.browser.selectPanelNumber(panelNo);
            scanSelectPath(panelName, bookmark.pathElements, true);
        }
        else
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame,
                    java.text.MessageFormat.format(guiContext.cfg.gs("Browser.library.is.not.loaded"), bookmark.pathElements[0]),
                    guiContext.cfg.gs("Navigator.menu.Bookmarks.text"), JOptionPane.WARNING_MESSAGE);
        }
    }

    public void bookmarkSelected(JTable sourceTable)
    {
        int[] rows = sourceTable.getSelectedRows();
        if (rows != null && rows.length != 1)
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame,
                    guiContext.cfg.gs(("Browser.please.select.a.single.item.to.bookmark")),
                    guiContext.cfg.gs("Browser.add.bookmark"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        //int row = sourceTable.getSelectedRow();
        NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[0], 1);
        NavTreeNode node = tuo.node;
        String name = tuo.name;

        bookmarkCreate(node, name, sourceTable.getName());
    }

    public void bookmarkSelected(JTree sourceTree)
    {
        TreePath[] paths = sourceTree.getSelectionPaths();
        if ((paths != null && paths.length != 1) || paths == null)
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame,
                    guiContext.cfg.gs(("Browser.please.select.a.single.item.to.bookmark")),
                    guiContext.cfg.gs("Browser.add.bookmark"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        NavTreeNode node = (NavTreeNode) sourceTree.getLastSelectedPathComponent();
        String name = node.getUserObject().name;

        bookmarkCreate(node, name, sourceTree.getName());
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.Delete.cannot") + tuo.name,
                            guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren(true);
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
            if (guiContext.preferences.isShowDeleteConfirmation())
            {
                String msg = MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Delete.are.you.sure1"),
                        rows.length, isRemote ? 0 : 1, rows.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false));
                msg += (dirCount > 0 ? MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Delete.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, msg,
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            boolean error = false;
            if (reply == JOptionPane.YES_OPTION)
            {
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        if (tuo.isDir)
                        {
                            if (navTransferHandler.removeDirectory(tuo))
                            {
                                error = true;
                                break;
                            }
                        }
                        else
                        {
                            if (navTransferHandler.removeFile(tuo))
                            {
                                error = true;
                                break;
                            }
                        }
                        if (!error)
                        {
                            try
                            {
                                navTransferHandler.exportHint("rm", tuo, null);
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.error.writing.hint") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog(guiContext.cfg.gs("Browser.skipping") + tuo.name);
                    }
                }
                if (!error)
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Delete.cannot") + tuo.name, guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren(true);
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
            if (guiContext.preferences.isShowDeleteConfirmation())
            {
                String msg = MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Delete.are.you.sure1"),
                        paths.length, isRemote ? 0 : 1, paths.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false));
                msg += (dirCount > 0 ? MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Delete.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, msg,
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            boolean error = false;
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
                            if (navTransferHandler.removeDirectory(tuo))
                            {
                                error = true;
                                break;
                            }
                        }
                        else
                        {
                            if (navTransferHandler.removeFile(tuo))
                            {
                                error = true;
                                break;
                            }
                        }
                        if (!error)
                        {
                            try
                            {
                                navTransferHandler.exportHint("rm", tuo, null);
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.error.writing.hint") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog(guiContext.cfg.gs("Browser.skipping") + tuo.name);
                    }
                }
                if (!error)
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

    public void deepScanCollectionTree(JTree tree, Repository repo, boolean remote, boolean recursive)
    {
        try
        {
            NavTreeNode root = setCollectionRoot(repo, tree, repo.getLibraryData().libraries.description, remote);
            if (repo.getLibraryData().libraries.bibliography != null)
            {
                Arrays.sort(repo.getLibraryData().libraries.bibliography);
                switch (styleOne)
                {
                    case STYLE_COLLECTION_ALL:
                        styleCollectionAll(tree, repo, remote, true, recursive);
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
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
        }
    }

    public void deepScanSystemTree(JTree tree, Repository repo, boolean remote, boolean recursive)
    {
        try
        {
            NavTreeNode root = null;
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    root = styleSystemAll(tree, repo, remote, true, recursive);
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

    private int getActiveNavStack(Component comp)
    {
        int active = -1;
        if (comp == null)
            comp = lastComponent;
        if (comp != null)
        {
            if (comp.getName().endsWith("CollectionOne"))
                active = 0;
            else if (comp.getName().endsWith("SystemOne"))
                active = 1;
            else if (comp.getName().endsWith("CollectionTwo"))
                active = 2;
            else if (comp.getName().endsWith("SystemTwo"))
                active = 3;
        }
        assert(active > -1);
        return active;
    }

    public long getFreespace(NavTreeUserObject tuo) throws Exception
    {
        return getFreespace(tuo.path, tuo.isRemote);
    }

    public long getFreespace(String path, boolean isRemote) throws Exception
    {
        long space = 0L;
        if (isRemote && guiContext.cfg.isRemoteSession())
        {
            try
            {
                // remote subscriber
                space = guiContext.context.clientStty.availableSpace(path);
            }
            catch (Exception e)
            {
                if (e instanceof SocketException && e.toString().contains("broken pipe"))
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Browser.connection.lost"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Z.exception"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
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

    public int getPanelNumber(String name)
    {
        int panelNo = -1;
        if (name.length() > 0)
        {
            name = name.toLowerCase();
            switch (name)
            {
                case "treecollectionone":
                    panelNo = 0;
                    break;
                case "tablecollectionone":
                    panelNo = 1;
                    break;
                case "treesystemone":
                    panelNo = 2;
                    break;
                case "tablesystemone":
                    panelNo = 3;
                    break;
                case "treecollectiontwo":
                    panelNo = 4;
                    break;
                case "tablecollectiontwo":
                    panelNo = 5;
                    break;
                case "treesystemtwo":
                    panelNo = 6;
                    break;
                case "tablesystemtwo":
                    panelNo = 7;
                    break;
            }
        }
        return panelNo;
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

    public JComponent getTabComponent(int index)
    {
        JComponent nextComponent = null;
        switch (index)
        {
            case 0:
                nextComponent = guiContext.mainFrame.treeCollectionOne;
                break;
            case 1:
                nextComponent = guiContext.mainFrame.tableCollectionOne;
                break;
            case 2:
                nextComponent = guiContext.mainFrame.treeSystemOne;
                break;
            case 3:
                nextComponent = guiContext.mainFrame.tableSystemOne;
                break;
            case 4:
                nextComponent = guiContext.mainFrame.treeCollectionTwo;
                break;
            case 5:
                nextComponent = guiContext.mainFrame.tableCollectionTwo;
                break;
            case 6:
                nextComponent = guiContext.mainFrame.treeSystemTwo;
                break;
            case 7:
                nextComponent = guiContext.mainFrame.tableSystemTwo;
                break;
        }
        assert (nextComponent != null);
        return nextComponent;
    }

    private boolean initialize()
    {
        navTransferHandler = new NavTransferHandler(guiContext);  // single instance

        for (int i = 0; i < navStack.length; ++i) // four individual NavStacks for the four browser tabs
            navStack[i] = new Stack<NavItem>();

        printLog(guiContext.cfg.getNavigatorName() + " " + guiContext.cfg.getVersionStamp());
        initializeToolbar();
        initializeNavigation();
        initializeBrowserOne();
        initializeBrowserTwo();
        addKeyListener();

        // handle mouse back/forward buttons
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() && MouseInfo.getNumberOfButtons() > 3)
        {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (guiContext.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
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
                }
            }, AWTEvent.MOUSE_EVENT_MASK);
        }

        // handle setting the size of the bottom window using the divider location
        guiContext.mainFrame.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                guiContext.preferences.fixBrowserDivider(guiContext, -1);
            }
        });
        //
        guiContext.mainFrame.tabbedPaneNavigatorBottom.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                guiContext.preferences.setBrowserBottomSize(componentEvent.getComponent().getHeight());
            }
        });

        // set default start location and related data
        initializeStatus(guiContext.mainFrame.treeCollectionTwo);
        initializeStatus(guiContext.mainFrame.treeCollectionOne); // do One last for focus

        return true;
    }

    private void initializeBrowserOne()
    {
        // --- BrowserOne ------------------------------------------
        //
        // --- tab selection handler
        guiContext.mainFrame.tabbedPaneBrowserOne.addChangeListener(new ChangeListener()
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
                        lastComponent = guiContext.mainFrame.treeCollectionOne;
                        model = guiContext.mainFrame.treeCollectionOne.getModel();
                        node = (NavTreeNode) guiContext.mainFrame.treeCollectionOne.getLastSelectedPathComponent();
                        tabStops[0] = 0;
                        tabStops[1] = 1;
                        break;
                    case 1:
                        lastComponent = guiContext.mainFrame.treeSystemOne;
                        model = guiContext.mainFrame.treeSystemOne.getModel();
                        node = (NavTreeNode) guiContext.mainFrame.treeSystemOne.getLastSelectedPathComponent();
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
        guiContext.mainFrame.treeCollectionOne.setName("treeCollectionOne");
        if (guiContext.context.publisherRepo != null && guiContext.context.publisherRepo.isInitialized())
        {
            File json = new File(guiContext.context.publisherRepo.getJsonFilename());
            String path = json.getAbsolutePath();
            guiContext.preferences.setLastPublisherOpenFile(path);
            guiContext.preferences.setLastPublisherOpenPath(FilenameUtils.getFullPathNoEndSeparator(path));

            loadCollectionTree(guiContext.mainFrame.treeCollectionOne, guiContext.context.publisherRepo, false);
        }
        else
        {
            setCollectionRoot(null, guiContext.mainFrame.treeCollectionOne, guiContext.cfg.gs("Browser.open.a.publisher"), false);
        }
        //
        // treeCollectionOne tree expansion event handler
        guiContext.mainFrame.treeCollectionOne.addTreeWillExpandListener(new TreeWillExpandListener()
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
        guiContext.mainFrame.treeCollectionOne.addTreeSelectionListener(new TreeSelectionListener()
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
        guiContext.mainFrame.treeCollectionOne.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.mainFrame.treeCollectionOne);
        addHandlersToTable(guiContext.mainFrame.tableCollectionOne);
        addFocusListener(guiContext.mainFrame.tableCollectionOne);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(guiContext.mainFrame.treeCollectionOne);

        // --- treeSystemOne
        guiContext.mainFrame.treeSystemOne.setName("treeSystemOne");
        if (guiContext.context.publisherRepo != null && guiContext.context.publisherRepo.isInitialized())
        {
            loadSystemTree(guiContext.mainFrame.treeSystemOne, guiContext.context.publisherRepo, false);
        }
        else
        {
            setCollectionRoot(null, guiContext.mainFrame.treeSystemOne, guiContext.cfg.gs("Browser.open.a.publisher"), false);
        }
        //
        // treeSystemOne tree expansion event handler
        guiContext.mainFrame.treeSystemOne.addTreeWillExpandListener(new TreeWillExpandListener()
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
        guiContext.mainFrame.treeSystemOne.addTreeSelectionListener(new TreeSelectionListener()
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
        guiContext.mainFrame.treeSystemOne.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.mainFrame.treeSystemOne);
        addHandlersToTable(guiContext.mainFrame.tableSystemOne);
        addFocusListener(guiContext.mainFrame.tableSystemOne);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(guiContext.mainFrame.treeSystemOne);
    }

    private void initializeBrowserTwo()
    {
        // --- BrowserTwo ------------------------------------------
        //
        // --- tab selection handler
        guiContext.mainFrame.tabbedPaneBrowserTwo.addChangeListener(new ChangeListener()
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
                        lastComponent = guiContext.mainFrame.treeCollectionTwo;
                        model = guiContext.mainFrame.treeCollectionTwo.getModel();
                        node = (NavTreeNode) guiContext.mainFrame.treeCollectionTwo.getLastSelectedPathComponent();
                        tabStops[2] = 4;
                        tabStops[3] = 5;
                        break;
                    case 1:
                        lastComponent = guiContext.mainFrame.treeSystemTwo;
                        model = guiContext.mainFrame.treeSystemTwo.getModel();
                        node = (NavTreeNode) guiContext.mainFrame.treeSystemTwo.getLastSelectedPathComponent();
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
        guiContext.mainFrame.treeCollectionTwo.setName("treeCollectionTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            File json = new File(guiContext.context.subscriberRepo.getJsonFilename());
            String path = json.getAbsolutePath();
            guiContext.preferences.setLastSubscriberOpenFile(path);
            guiContext.preferences.setLastSubscriberOpenPath(FilenameUtils.getFullPathNoEndSeparator(path));

            loadCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, guiContext.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(null, guiContext.mainFrame.treeCollectionTwo, guiContext.cfg.gs("Browser.open.a.subscriber"), guiContext.cfg.isRemoteSession());
        }
        //
        // treeCollectionTwo tree expansion event handler
        guiContext.mainFrame.treeCollectionTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
        guiContext.mainFrame.treeCollectionTwo.addTreeSelectionListener(new TreeSelectionListener()
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
        guiContext.mainFrame.treeCollectionTwo.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.mainFrame.treeCollectionTwo);
        addHandlersToTable(guiContext.mainFrame.tableCollectionTwo);
        addFocusListener(guiContext.mainFrame.tableCollectionTwo);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(guiContext.mainFrame.treeCollectionTwo);

        // --- treeSystemTwo
        guiContext.mainFrame.treeSystemTwo.setName("treeSystemTwo");
        if (guiContext.context.subscriberRepo != null && guiContext.context.subscriberRepo.isInitialized())
        {
            loadSystemTree(guiContext.mainFrame.treeSystemTwo, guiContext.context.subscriberRepo, guiContext.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(null, guiContext.mainFrame.treeSystemTwo, guiContext.cfg.gs("Browser.open.a.subscriber"), guiContext.cfg.isRemoteSession());
        }
        //
        // treeSystemTwo tree expansion event handler
        guiContext.mainFrame.treeSystemTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
        guiContext.mainFrame.treeSystemTwo.addTreeSelectionListener(new TreeSelectionListener()
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
        guiContext.mainFrame.treeSystemTwo.setTransferHandler(navTransferHandler);
        addFocusListener(guiContext.mainFrame.treeSystemTwo);
        addHandlersToTable(guiContext.mainFrame.tableSystemTwo);
        addFocusListener(guiContext.mainFrame.tableSystemTwo);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(guiContext.mainFrame.treeSystemTwo);
    }

    private void initializeNavigation()
    {
        guiContext.mainFrame.buttonBack.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navBack();
            }
        });

        guiContext.mainFrame.buttonForward.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navForward();
            }
        });

        guiContext.mainFrame.buttonUp.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navUp();
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
        if (!guiContext.navigator.showHintTrackingButton)
        {
            guiContext.mainFrame.panelHintTracking.setVisible(false);
            trackingHints = false;
        }
        else
            trackingHints = true;

        guiContext.mainFrame.buttonHintTracking.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (actionEvent.getActionCommand() != null && actionEvent.getActionCommand().equalsIgnoreCase("hints"))
                {
                    if (guiContext.mainFrame.panelHintTracking.isVisible())
                    {
                        if (!trackingHints) // toggle hint tacking
                        {
                            try
                            {
                                URL url = Thread.currentThread().getContextClassLoader().getResource("hint-green.png");
                                Image icon = ImageIO.read(url);
                                guiContext.mainFrame.buttonHintTracking.setIcon(new ImageIcon(icon));
                                guiContext.mainFrame.buttonHintTracking.setToolTipText(guiContext.cfg.gs("Navigator.button.HintTracking.enabled.tooltip"));
                                trackingHints = true;
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        else
                        {
                            try
                            {
                                URL url = Thread.currentThread().getContextClassLoader().getResource("hint-red.png");
                                Image icon = ImageIO.read(url);
                                guiContext.mainFrame.buttonHintTracking.setIcon(new ImageIcon(icon));
                                guiContext.mainFrame.buttonHintTracking.setToolTipText(guiContext.cfg.gs("Navigator.button.HintTracking.disabled.tooltip"));
                                trackingHints = false;
                            }
                            catch (Exception e)
                            {
                            }
                        }
                    }
                }
            }
        });
        hintTrackingColor = guiContext.mainFrame.buttonHintTracking.getBackground();
    }

    public void loadCollectionTree(JTree tree, Repository repo, boolean remote)
    {
        try
        {
            NavTreeNode root = setCollectionRoot(repo, tree, repo.getLibraryData().libraries.description, remote);
            if (repo.getLibraryData().libraries.bibliography != null)
            {
                Arrays.sort(repo.getLibraryData().libraries.bibliography);
                switch (styleOne)
                {
                    case STYLE_COLLECTION_ALL:
                        styleCollectionAll(tree, repo, remote,  false, false);
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
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
        }
    }

    public void loadSystemTree(JTree tree, Repository repo, boolean remote)
    {
        try
        {
            NavTreeNode root = null;
            switch (styleTwo)
            {
                case STYLE_SYSTEM_ALL:
                    root = styleSystemAll(tree, repo, remote, false, false);
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
        NavItem ni = navStackPop();
        if (ni.node != null)
        {
            ni.node.selectMyTab();
            ni.node.selectMe(); // loads table
            ni.component.requestFocus();
        }
    }

    private void navForward()
    {
        int ans = getActiveNavStack(null);
        if (navStackIndex[ans] + 1 <= navStack[ans].lastIndexOf(navStack[ans].lastElement()))
        {
            ++navStackIndex[ans];
        }
        NavItem ni = navStack[ans].get(navStackIndex[ans]);
        if (ni.node != null)
        {
            ni.node.selectMyTab();
            ni.node.selectMe(); // loads table
            ni.component.requestFocus();
        }
    }

    private NavItem navStackPop()
    {
        int ans = getActiveNavStack(null);
        NavItem ni;
        if (navStackIndex[ans] > 0)
        {
            --navStackIndex[ans];
            ni = navStack[ans].get(navStackIndex[ans]);
        }
        else
            ni = (navStackIndex[ans] > -1) ? navStack[ans].get(0) : null;
        return ni;
    }

    private void navStackPush(NavTreeNode node)
    {
        int ans = getActiveNavStack(node.getMyTree());
        if (navStackIndex[ans] < 0 || navStack[ans].get(navStackIndex[ans]).node != node)
        {
            if (navStackIndex[ans] > -1)
                navStack[ans].setSize(navStackIndex[ans] + 1); // truncate anything beyond this index
            NavItem ni = new NavItem(node, lastComponent);
            navStack[ans].push(ni);
            ++navStackIndex[ans];
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

    private void navUp()
    {
        int ans = getActiveNavStack(null);
        NavItem ni = navStack[ans].get(navStackIndex[ans]);
        NavTreeNode node = (NavTreeNode) ni.node.getParent();
        if (node != null)
        {
            node.selectMyTab();
            node.selectMe();
            lastComponent = lastComponent;
            lastComponent.requestFocus();
            navStackPush(node);
        }
    }

    public synchronized void printLog(String text, boolean isError)
    {
        if (isError)
        {
            logger.error(text);
            guiContext.mainFrame.textAreaLog.append(guiContext.cfg.gs("Browser.error") + text + System.getProperty("line.separator"));
        }
        else
            printLog(text);
    }

    public synchronized void printLog(String text)
    {
        logger.info(text);
        guiContext.mainFrame.textAreaLog.append(text + System.getProperty("line.separator"));
        guiContext.mainFrame.textAreaLog.repaint();
    }

    public synchronized void printProperties(NavTreeUserObject tuo)
    {
        guiContext.mainFrame.textAreaProperties.setText("");
        String msg = "<html>";
        msg += "<style>table { margin:0; padding:0; }" +
                "th { margin:0; padding:0; }" +
                "td { text-align:left; }" +
                "</style>";
        msg += "<body>";
        msg += guiContext.cfg.gs("Properties.type") + tuo.getType() + "<br/>" + System.getProperty("line.separator");

        try
        {
            switch (tuo.type)
            {
                case NavTreeUserObject.BOOKMARKS:
                    break;
                case NavTreeUserObject.COLLECTION:
                    msg += guiContext.cfg.gs("Properties.libraries") + tuo.node.getChildCount(false, false) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.COMPUTER:
                    msg += guiContext.cfg.gs("Properties.drives") + tuo.node.getChildCount(false, false) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.DRIVE:
                    msg += guiContext.cfg.gs("Properties.free") + Utils.formatLong(getFreespace(tuo), true) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.HOME:
                    msg += guiContext.cfg.gs("Properties.free") + Utils.formatLong(getFreespace(tuo), true) + "<br/>" + System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.LIBRARY:
                    msg += "<table cellpadding=\"0\" cellspacing=\"0\">" +
                            "<tr><td>" + MessageFormat.format(guiContext.cfg.gs("Properties.sources"), tuo.sources.length) + "</td> <td></td> <td>" +
                            guiContext.cfg.gs("Properties.free") + "</td></tr>";
                    if (tuo.isRemote)
                        guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    for (String source : tuo.sources)
                    {
                        String free = Utils.formatLong(getFreespace(source, tuo.isRemote), true);
                        msg += "<tr><td>" + source + "</td> <td><div>&nbsp;&nbsp;&nbsp;&nbsp;</div></td> <td>" + free + "</td></tr>";
                    }
                    guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    msg += "</table>";
                    msg += System.getProperty("line.separator");
                    break;
                case NavTreeUserObject.REAL:
                    msg += guiContext.cfg.gs("Properties.path") + tuo.path + "<br/>" + System.getProperty("line.separator");
                    if (!tuo.isDir)
                        msg += guiContext.cfg.gs("Properties.size") + Utils.formatLong(tuo.size, true) + "<br/>" + System.getProperty("line.separator");
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
            guiContext.mainFrame.textAreaProperties.setText(msg);
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    public void refreshAll()
    {
        refreshTree(guiContext.mainFrame.treeCollectionOne);
        refreshTree(guiContext.mainFrame.treeSystemOne);
        refreshTree(guiContext.mainFrame.treeCollectionTwo);
        refreshTree(guiContext.mainFrame.treeSystemTwo);
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

    public void refreshTree(JTree tree)
    {
        if (tree != null)
        {
            tree.setEnabled(false);
            TreePath rootPath = ((NavTreeNode) tree.getModel().getRoot()).getTreePath();
            Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(rootPath);
            TreePath[] paths = tree.getSelectionPaths();
            tree.setExpandsSelectedPaths(true);
            if (expandedDescendants != null)
            {
                while (expandedDescendants.hasMoreElements())
                {
                    TreePath tp = expandedDescendants.nextElement();
                    tree.expandPath(tp);
                }
            }
            if (paths != null && paths.length > 0)
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

    public TreePath scanSelectPath(String panelName, String[] pathElements, boolean doTable)
    {
        TreePath treePath = null;
        if (panelName != null && panelName.length() > 0 && pathElements != null && pathElements.length > 0)
        {
            // determine which
            JTree tree;
            JTable table;
            if (panelName.endsWith("one")) // publisher
            {
                tree = panelName.contains("collection") ? guiContext.mainFrame.treeCollectionOne : guiContext.mainFrame.treeSystemOne;
                table = panelName.contains("collection") ? guiContext.mainFrame.tableCollectionOne : guiContext.mainFrame.tableSystemOne;
            }
            else // subscriber
            {
                tree = panelName.contains("collection") ? guiContext.mainFrame.treeCollectionTwo : guiContext.mainFrame.treeSystemTwo;
                table = panelName.contains("collection") ? guiContext.mainFrame.tableCollectionTwo : guiContext.mainFrame.tableSystemTwo;
            }

            int nodeIndex = 0;
            NavTreeNode[] nodes = new NavTreeNode[pathElements.length];

            NavTreeNode node = (NavTreeNode) tree.getModel().getRoot();
            nodes[nodeIndex++] = node;

            // find or scan each path segment
            NavTreeNode next;
            int occurrence = 1;
            for (int i = 1; i < pathElements.length; ++i)
            {
                next = node.findChildName(pathElements[i], occurrence);
                if (next != null)
                {
                    nodes[nodeIndex++] = next;
                    node = next;
                }
                else
                {
                    // if a directory scan it
                    if (node.getUserObject().isDir)
                    {
                        node.deepScanChildren(false);
                    }
                    // search again
                    next = node.findChildName(pathElements[i]);
                    if (next != null)
                    {
                        nodes[nodeIndex++] = next;
                        node = next;
                    }
                    else // not found, see if there is another occurrence
                    {
                        ++occurrence;
                        node = nodes[nodeIndex - 2];
                        next = node.findChildName(pathElements[i - 1], occurrence);

                        // if another occurrence step backward and search it
                        if (next != null)
                        {
                            node = next;
                            --i;
                            --nodeIndex;
                            nodes[nodeIndex++] = next;
                            occurrence = 1;
                        }
                        else // not found, use what was found
                            break;
                    }
                }
            }

            // resize & shuffle if element(s) missing
            if (nodeIndex != pathElements.length)
            {
                NavTreeNode[] shorter = new NavTreeNode[nodeIndex]; // TODO: emit warning path was truncated
                for (int k = 0; k < nodeIndex; ++k)
                    shorter[k] = nodes[k];
                nodes = new NavTreeNode[nodeIndex];
                for (int k = 0; k < nodeIndex; ++k)
                    nodes[k] = shorter[k];
            }

            // remove last segment if it's a file but not being shown, or a directory in the table
            if ((!nodes[nodeIndex - 1].getUserObject().isDir && guiContext.preferences.isHideFilesInTree()))
            {
                NavTreeNode[] navNodes = new NavTreeNode[nodeIndex - 1];
                for (int j = 0; j < nodeIndex - 1; ++j)
                    navNodes[j] = nodes[j];
                treePath = new TreePath(navNodes);
            }
            else
                treePath = new TreePath(nodes);

            // expand, scroll to and select the node tree
            if (!node.isLoaded())
                node.deepScanChildren(false);
            tree.setSelectionPath(treePath);
            tree.scrollPathToVisible(treePath);

            // highlight last item if it's a table
            if (doTable)
            {
                if (panelName.startsWith("table"))
                {
                    int panelNo = guiContext.browser.getPanelNumber(panelName);
                    table = (JTable) guiContext.browser.getTabComponent(panelNo);
                    if (table != null)
                    {
                        table.requestFocus();
                        int index = guiContext.browser.findRowIndex(table, pathElements[pathElements.length - 1]); // last element
                        if (index > -1)
                        {
                            table.setRowSelectionInterval(index, index);
                        }
                    }
                }
                else
                {
                    table.clearSelection();
                }
            }
        }
        return treePath;
    }

    public String selectLibrarySource(NavTreeUserObject tuo)
    {
        String path = "";
        if (tuo.type == NavTreeUserObject.REAL)
            path = tuo.path;
        else if (tuo.type == NavTreeUserObject.LIBRARY)
        {
            if (tuo.sources.length == 1)
                path = tuo.sources[0];
            else
            {
                // make dialog pieces
                String message = java.text.MessageFormat.format(guiContext.cfg.gs("Navigator.menu.New.folder.select.library.source"), tuo.sources.length, tuo.name);
                JList<String> sources = new JList<String>();
                DefaultListModel<String> listModel = new DefaultListModel<String>();
                sources.setModel(listModel);
                sources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                for (String src : tuo.sources)
                {
                    listModel.addElement(src);
                }
                sources.setSelectedIndex(0);

                JScrollPane pane = new JScrollPane();
                pane.setViewportView(sources);
                sources.requestFocus();
                Object[] params = {message, pane};

                int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, guiContext.cfg.getNavigatorName(), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    int index = sources.getSelectedIndex();
                    path = sources.getSelectedValue();
                }
                else
                {
                    path = "_cancelled_";
                }
            }
        }
        return path;
    }

    public void selectPanelNumber(int panelNo)
    {
        if (panelNo >= 0)
        {
            switch (panelNo)
            {
                case 0:
                    guiContext.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(0);
                    guiContext.mainFrame.treeCollectionOne.requestFocus();
                    break;
                case 1:
                    guiContext.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(0);
                    guiContext.mainFrame.tableCollectionOne.requestFocus();
                    break;
                case 2:
                    guiContext.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(1);
                    guiContext.mainFrame.treeSystemOne.requestFocus();
                    break;
                case 3:
                    guiContext.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(1);
                    guiContext.mainFrame.tableSystemOne.requestFocus();
                    break;
                case 4:
                    guiContext.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(0);
                    guiContext.mainFrame.treeCollectionTwo.requestFocus();
                    break;
                case 5:
                    guiContext.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(0);
                    guiContext.mainFrame.tableCollectionTwo.requestFocus();
                    break;
                case 6:
                    guiContext.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(1);
                    guiContext.mainFrame.treeSystemTwo.requestFocus();
                    break;
                case 7:
                    guiContext.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(1);
                    guiContext.mainFrame.tableSystemTwo.requestFocus();
                    break;
            }
        }
    }
    
    private NavTreeNode setCollectionRoot(Repository repo, JTree tree, String title, boolean remote)
    {
        NavTreeNode root = new NavTreeNode(guiContext, repo, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, title, NavTreeUserObject.COLLECTION, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(guiContext.preferences.isHideFilesInTree());
        tree.setCellRenderer(new NavTreeCellRenderer(guiContext));
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

    private void styleCollectionAll(JTree tree, Repository repo, boolean remote, boolean deep, boolean recursive) throws Exception
    {
        NavTreeModel model = (NavTreeModel) tree.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            NavTreeNode node = new NavTreeNode(guiContext, repo, tree);
            NavTreeUserObject tuo = new NavTreeUserObject(node, lib.name, lib.sources, remote);
            node.setNavTreeUserObject(tuo);
            root.add(node);
            if (deep)
                node.deepScanChildren(recursive);
            else
                node.loadChildren(false);
        }
        root.setLoaded(true);
    }

    private NavTreeNode styleSystemAll(JTree tree, Repository repo, boolean remote, boolean deep, boolean recursive) throws Exception
    {
        // setup new invisible root for Computer, Home & Bookmarks
        NavTreeNode root = new NavTreeNode(guiContext, repo, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, guiContext.cfg.gs("Browser.system"), NavTreeUserObject.SYSTEM, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(guiContext.preferences.isHideFilesInTree());
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setCellRenderer(new NavTreeCellRenderer(guiContext));
        tree.setModel(model);

        // add Computer node
        NavTreeNode rootNode = new NavTreeNode(guiContext, repo, tree);
        tuo = new NavTreeUserObject(rootNode, guiContext.cfg.gs("Browser.computer"), NavTreeUserObject.COMPUTER, remote);
        rootNode.setNavTreeUserObject(tuo);
        root.add(rootNode);
        if (remote && tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            NavTreeNode node = new NavTreeNode(guiContext, repo, tree);
            tuo = new NavTreeUserObject(node, "/", "/", NavTreeUserObject.DRIVE, remote);
            node.setNavTreeUserObject(tuo);
            rootNode.add(node);
            if (deep)
                node.deepScanChildren(recursive);
            else
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
                NavTreeNode node = new NavTreeNode(guiContext, repo, tree);
                tuo = new NavTreeUserObject(node, drive.getPath(), drive.getAbsolutePath(), NavTreeUserObject.DRIVE, false);
                node.setNavTreeUserObject(tuo);
                rootNode.add(node);
                if (deep)
                    node.deepScanChildren(recursive);
                else
                    node.loadChildren(false);
            }
        }
        rootNode.setLoaded(true);

        if (tree.getName().equalsIgnoreCase("treeSystemOne"))
        {
            // add Home root node
            NavTreeNode homeNode = new NavTreeNode(guiContext, repo, tree);
            tuo = new NavTreeUserObject(homeNode, guiContext.cfg.gs("Browser.home"), System.getProperty("user.home"), NavTreeUserObject.HOME, false);
            homeNode.setNavTreeUserObject(tuo);
            root.add(homeNode);
            if (deep)
                homeNode.deepScanChildren(recursive);
            else
                homeNode.loadChildren(false);
        }

        root.setLoaded(true);
        return root;
    }

    public void touchSelected(JTable sourceTable)
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Touch.cannot") + tuo.name, guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren(true);
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
            if (guiContext.preferences.isShowTouchConfirmation())
            {
                String msg = MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Touch.are.you.sure1"),
                        rows.length, isRemote ? 0 : 1, rows.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false));
                msg += (dirCount > 0 ? MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Touch.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, msg,
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            if (reply == JOptionPane.YES_OPTION)
            {
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        try
                        {
                            long seconds = guiContext.context.transfer.touch(tuo.path, tuo.isRemote);
                            if (tuo.isRemote)
                            {
                                tuo.mtime = (int) seconds;
                            }
                            tuo.fileTime = FileTime.from(seconds, TimeUnit.SECONDS);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Touch.error") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                        NavTreeNode parent = (NavTreeNode) tuo.node.getParent();
                        if (parent != null)
                        {
                            DefaultRowSorter sorter = ((DefaultRowSorter) sourceTable.getRowSorter());
                            sorter.sort();
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog(guiContext.cfg.gs("Browser.skipping") + tuo.name);
                    }
                }
            }
        }
    }

    public void touchSelected(JTree sourceTree)
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Touch.cannot") + tuo.name, guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                isRemote = tuo.isRemote;
                if (tuo.isDir)
                {
                    ++dirCount;
                    tuo.node.deepScanChildren(true);
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
            if (guiContext.preferences.isShowTouchConfirmation())
            {
                String msg = MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Touch.are.you.sure1"),
                        paths.length, isRemote ? 0 : 1, paths.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false));
                msg += (dirCount > 0 ? MessageFormat.format(guiContext.cfg.gs("Navigator.menu.Touch.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (guiContext.cfg.isDryRun() ? guiContext.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, msg,
                        guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            }

            if (reply == JOptionPane.YES_OPTION)
            {
                for (TreePath path : paths)
                {
                    NavTreeNode ntn = (NavTreeNode) path.getLastPathComponent();
                    NavTreeUserObject tuo = ntn.getUserObject();
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        try
                        {
                            long seconds = guiContext.context.transfer.touch(tuo.path, tuo.isRemote);
                            if (tuo.isRemote)
                            {
                                tuo.mtime = (int) seconds;
                            }
                            tuo.fileTime = FileTime.from(seconds, TimeUnit.SECONDS);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Touch.error") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                        NavTreeNode parent = (NavTreeNode) tuo.node.getParent();
                        if (parent != null)
                        {
                            refreshTree(parent.getMyTree());
//                            parent.selectMe();
                        }
                    }
                    else
                    {
                        guiContext.browser.printLog(guiContext.cfg.gs("Browser.skipping") + tuo.name);
                    }
                }
            }
        }
    }

    // ================================================================================================================

    public class NavItem
    {
        Component component; // focused component
        NavTreeNode node; // tree node (data)

        public NavItem(NavTreeNode node, Component component)
        {
            this.node = node;
            if (component != null)
                this.component = component;
            else
                this.component = node.getMyTree();
        }
    }

}
