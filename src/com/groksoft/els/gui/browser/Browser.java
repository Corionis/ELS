package com.groksoft.els.gui.browser;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.bookmarks.Bookmark;
import com.groksoft.els.repository.Item;
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

@SuppressWarnings(value = "unchecked")

/**
 * Browser tab
 */
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
    private Context context;
    public JComponent lastComponent = null;
    public int lastTab = 0;
    public boolean hintTrackingEnabled = false;
    private String keyBuffer = "";
    private long keyTime = 0L;
    private transient Logger logger = LogManager.getLogger("applog");
    private Stack<NavItem>[] navStack = new Stack[4];
    private int[] navStackIndex = { -1, -1, -1, -1 };
    public NavTransferHandler navTransferHandler;
    static boolean printPropertiesInUse = false;
    private int tabStop = 0;
    private int[] tabStops = {0, 1, 4, 5};

    public Browser(Context context)
    {
        this.context = context;
        this.context.browser = this;
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
                //if (active.isEnabled())
                name = active.getName();
                if (name.length() > 0)
                {
                    switch (name)
                    {
                        case "treeCollectionOne":
                            lastTab = 0;
                            lastComponent = context.mainFrame.treeCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "tableCollectionOne":
                            lastTab = 1;
                            lastComponent = context.mainFrame.tableCollectionOne;
                            tabStops[0] = 0;
                            tabStops[1] = 1;
                            break;
                        case "treeSystemOne":
                            lastTab = 0;
                            lastComponent = context.mainFrame.treeSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "tableSystemOne":
                            lastTab = 1;
                            lastComponent = context.mainFrame.tableSystemOne;
                            tabStops[0] = 2;
                            tabStops[1] = 3;
                            break;
                        case "treeCollectionTwo":
                            lastTab = 2;
                            lastComponent = context.mainFrame.treeCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "tableCollectionTwo":
                            lastTab = 3;
                            lastComponent = context.mainFrame.tableCollectionTwo;
                            tabStops[2] = 4;
                            tabStops[3] = 5;
                            break;
                        case "treeSystemTwo":
                            lastTab = 2;
                            lastComponent = context.mainFrame.treeSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                        case "tableSystemTwo":
                            lastTab = 3;
                            lastComponent = context.mainFrame.tableSystemTwo;
                            tabStops[2] = 6;
                            tabStops[3] = 7;
                            break;
                    }

                    NavTreeUserObject tuo = getSelectedUserObject(active);
                    if (tuo != null)
                    {
                        context.mainFrame.textFieldLocation.setText(tuo.getPath());
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
                context.mainFrame.labelStatusMiddle.setText("");
                JTable target = (JTable) mouseEvent.getSource();
                target.requestFocus();
                JTree eventTree = null;
                switch (target.getName())
                {
                    case "tableCollectionOne":
                        eventTree = context.mainFrame.treeCollectionOne;
                        break;
                    case "tableSystemOne":
                        eventTree = context.mainFrame.treeSystemOne;
                        break;
                    case "tableCollectionTwo":
                        eventTree = context.mainFrame.treeCollectionTwo;
                        break;
                    case "tableSystemTwo":
                        eventTree = context.mainFrame.treeSystemTwo;
                        break;
                }
                int row = target.getSelectedRow();
                if (row >= 0)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) target.getValueAt(row, 1);
                    if (tuo != null)
                    {
                        boolean doubleClick = (mouseEvent.getClickCount() == 2);
                        context.mainFrame.textFieldLocation.setText(tuo.getPath());
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
                                        JOptionPane.showMessageDialog(context.mainFrame,
                                                context.cfg.gs("Browser.error.launching.item"),
                                                context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Browser.launch.of") +
                                                    (tuo.isRemote ? context.cfg.gs("Z.remote.lowercase") : "") +
                                                    context.cfg.gs("Browser.launch.of.items.not.supported"),
                                            context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                        tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
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
                    for (ActionListener listener : context.mainFrame.menuItemRename.getActionListeners())
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
                    toggleShowHiddenFiles();
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
//                            JOptionPane.showMessageDialog(context.form,
//                                    "change location",
//                                    context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                            tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
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
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Browser.error.launching.item"),
                                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                            else
                            {
                                JOptionPane.showMessageDialog(context.mainFrame,
                                        context.cfg.gs("Browser.launch.of") +
                                                (tuo.isRemote ? context.cfg.gs("Z.remote.lowercase") : "") +
                                                context.cfg.gs("Browser.launch.of.items.not.supported"),
                                        context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
        context.mainFrame.treeCollectionOne.addKeyListener(browserKeyListener);
        context.mainFrame.tableCollectionOne.addKeyListener(browserKeyListener);
        context.mainFrame.treeSystemOne.addKeyListener(browserKeyListener);
        context.mainFrame.tableSystemOne.addKeyListener(browserKeyListener);
        context.mainFrame.treeCollectionTwo.addKeyListener(browserKeyListener);
        context.mainFrame.tableCollectionTwo.addKeyListener(browserKeyListener);
        context.mainFrame.treeSystemTwo.addKeyListener(browserKeyListener);
        context.mainFrame.tableSystemTwo.addKeyListener(browserKeyListener);
    }

    private void bookmarkCreate(NavTreeNode node, String name, String panelName)
    {
        Repository repo = node.getUserObject().getRepo();
        if (repo != null)
        {
            Object obj = JOptionPane.showInputDialog(context.mainFrame,
                    repo.getLibraryData().libraries.description + " " + context.cfg.gs(("Browser.bookmark.name")),
                    context.cfg.gs("Browser.add.bookmark"), JOptionPane.QUESTION_MESSAGE,
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
                    context.navigator.bookmarks.add(bm);
                    try
                    {
                        context.navigator.bookmarks.write();
                        context.navigator.loadBookmarksMenu();
                    }
                    catch (Exception e)
                    {
                        logger.error(Utils.getStackTrace(e));
                        JOptionPane.showMessageDialog(context.mainFrame,
                                context.cfg.gs("Browser.error.saving.bookmarks") + e.getMessage(),
                                context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    public Bookmark bookmarkCreate(Item item, String name, boolean isPublisher)
    {
        Bookmark bm = new Bookmark();
        bm.name = name;
        bm.panel = (isPublisher) ? "tableCollectionOne" : "tableCollectionTwo";
        Repository repo = (isPublisher) ? context.publisherRepo : context.subscriberRepo;

        String sep = Utils.getSeparatorFromPath(item.getFullPath());
        String[] split = item.getItemPath().split(sep);
        if (split != null && split.length > 0)
        {
            bm.pathElements = new String[split.length + 2];
            bm.pathElements[0] = repo.getLibraryData().libraries.description;
            bm.pathElements[1] = item.getLibrary();
            for (int i = 0; i < split.length; ++i)
                bm.pathElements[i + 2] = split[i];
        }
        return bm;
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
            tree = panelName.contains("collection") ? context.mainFrame.treeCollectionOne : context.mainFrame.treeSystemOne;
        else // subscriber
            tree = panelName.contains("collection") ? context.mainFrame.treeCollectionTwo : context.mainFrame.treeSystemTwo;

        NavTreeNode node = (NavTreeNode) tree.getModel().getRoot();
        // root should be first path element
        if (node.getUserObject().name.equals(bookmark.pathElements[0]))
        {
            // select the Browser tab and one of it's 8 panels
            context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
            int panelNo = context.browser.getPanelNumber(bookmark.panel);
            context.browser.selectPanelNumber(panelNo);
            scanSelectPath(panelName, bookmark.pathElements, true);
        }
        else
        {
            JOptionPane.showMessageDialog(context.mainFrame,
                    java.text.MessageFormat.format(context.cfg.gs("Browser.library.is.not.loaded"), bookmark.pathElements[0]),
                    context.cfg.gs("Navigator.menu.Bookmarks.text"), JOptionPane.WARNING_MESSAGE);
        }
    }

    public void bookmarkSelected(JTable sourceTable)
    {
        int[] rows = sourceTable.getSelectedRows();
        if (rows != null && rows.length != 1)
        {
            JOptionPane.showMessageDialog(context.mainFrame,
                    context.cfg.gs(("Browser.please.select.a.single.item.to.bookmark")),
                    context.cfg.gs("Browser.add.bookmark"), JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(context.mainFrame,
                    context.cfg.gs(("Browser.please.select.a.single.item.to.bookmark")),
                    context.cfg.gs("Browser.add.bookmark"), JOptionPane.WARNING_MESSAGE);
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
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Delete.cannot") + tuo.name,
                            context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
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
            if (context.preferences.isShowDeleteConfirmation())
            {
                String msg = MessageFormat.format(context.cfg.gs("Navigator.menu.Delete.are.you.sure1"),
                        rows.length, isRemote ? 0 : 1, rows.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false, context.cfg.getLongScale()));
                msg += (dirCount > 0 ? MessageFormat.format(context.cfg.gs("Navigator.menu.Delete.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
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
                                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.writing.hint") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        logger.info(context.cfg.gs("Browser.skipping") + tuo.name);
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
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Delete.cannot") + tuo.name, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
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
            if (context.preferences.isShowDeleteConfirmation())
            {
                String msg = MessageFormat.format(context.cfg.gs("Navigator.menu.Delete.are.you.sure1"),
                        paths.length, isRemote ? 0 : 1, paths.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false, context.cfg.getLongScale()));
                msg += (dirCount > 0 ? MessageFormat.format(context.cfg.gs("Navigator.menu.Delete.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
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
                                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.writing.hint") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    else
                    {
                        logger.info(context.cfg.gs("Browser.skipping") + tuo.name);
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
            context.fault = true;
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
            context.fault = true;
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
        if (isRemote && context.cfg.isRemoteSession())
        {
            try
            {
                // remote subscriber
                space = context.clientStty.availableSpace(path);
            }
            catch (Exception e)
            {
                if (e instanceof SocketException && e.toString().contains("broken pipe"))
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Browser.connection.lost"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Z.exception"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
        }
        else
        {
            space = Utils.availableSpace(path);
        }
        return space;

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
            tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
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
                nextComponent = context.mainFrame.treeCollectionOne;
                break;
            case 1:
                nextComponent = context.mainFrame.tableCollectionOne;
                break;
            case 2:
                nextComponent = context.mainFrame.treeSystemOne;
                break;
            case 3:
                nextComponent = context.mainFrame.tableSystemOne;
                break;
            case 4:
                nextComponent = context.mainFrame.treeCollectionTwo;
                break;
            case 5:
                nextComponent = context.mainFrame.tableCollectionTwo;
                break;
            case 6:
                nextComponent = context.mainFrame.treeSystemTwo;
                break;
            case 7:
                nextComponent = context.mainFrame.tableSystemTwo;
                break;
        }
        assert (nextComponent != null);
        return nextComponent;
    }

    private boolean initialize()
    {
        navTransferHandler = new NavTransferHandler(context);  // single instance

        // four individual NavStacks for the four browser tabs
        for (int i = 0; i < navStack.length; ++i)
            navStack[i] = new Stack<NavItem>();

        //logger.info(context.cfg.getNavigatorName() + " " + context.cfg.getVersionStamp());
        initializeToolbar();
        initializeNavigation();
        initializeBrowserOne();
        initializeBrowserTwo();
        addKeyListener();

        // handle mouse back/forward buttons
        if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() && MouseInfo.getNumberOfButtons() > 3)
        {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
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

        // handle setting the size of the Browser bottom window using the divider location
        context.mainFrame.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                context.preferences.fixBrowserDivider(context, -1);
                context.preferences.fixOperationsDivider(context, -1);
            }
        });
        //
        context.mainFrame.tabbedPaneNavigatorBottom.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                context.preferences.setBrowserBottomSize(componentEvent.getComponent().getHeight());
            }
        });

        // handle setting the size of the Operations bottom window using the divider location
        context.mainFrame.tabbedPaneOperationBottom.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent componentEvent)
            {
                super.componentResized(componentEvent);
                context.preferences.setOperationDividerBottomSize(componentEvent.getComponent().getHeight());
            }
        });

        // set default start location and related data
        initializeStatus(context.mainFrame.treeCollectionTwo);
        initializeStatus(context.mainFrame.treeCollectionOne); // do One last for focus

        return true;
    }

    private void initializeBrowserOne()
    {
        // --- BrowserOne ------------------------------------------
        //
        // --- tab selection handler
        context.mainFrame.tabbedPaneBrowserOne.addChangeListener(new ChangeListener()
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
                        lastComponent = context.mainFrame.treeCollectionOne;
                        model = context.mainFrame.treeCollectionOne.getModel();
                        node = (NavTreeNode) context.mainFrame.treeCollectionOne.getLastSelectedPathComponent();
                        tabStops[0] = 0;
                        tabStops[1] = 1;
                        break;
                    case 1:
                        lastComponent = context.mainFrame.treeSystemOne;
                        model = context.mainFrame.treeSystemOne.getModel();
                        node = (NavTreeNode) context.mainFrame.treeSystemOne.getLastSelectedPathComponent();
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
        context.mainFrame.treeCollectionOne.setName("treeCollectionOne");
        if (context.publisherRepo != null && context.publisherRepo.isInitialized())
        {
            File json = new File(context.publisherRepo.getJsonFilename());
            String path = json.getAbsolutePath();
            context.preferences.setLastPublisherOpenFile(path);
            context.preferences.setLastPublisherOpenPath(FilenameUtils.getFullPathNoEndSeparator(path));

            loadCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false);
        }
        else
        {
            setCollectionRoot(null, context.mainFrame.treeCollectionOne, context.cfg.gs("Browser.open.a.publisher"), false);
        }
        //
        // treeCollectionOne tree expansion event handler
        context.mainFrame.treeCollectionOne.addTreeWillExpandListener(new TreeWillExpandListener()
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
        context.mainFrame.treeCollectionOne.addTreeSelectionListener(new TreeSelectionListener()
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
        context.mainFrame.treeCollectionOne.setTransferHandler(navTransferHandler);
        addFocusListener(context.mainFrame.treeCollectionOne);
        addHandlersToTable(context.mainFrame.tableCollectionOne);
        addFocusListener(context.mainFrame.tableCollectionOne);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(context.mainFrame.treeCollectionOne);

        // --- treeSystemOne
        context.mainFrame.treeSystemOne.setName("treeSystemOne");
        if (context.publisherRepo != null && context.publisherRepo.isInitialized())
        {
            loadSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false);
        }
        else
        {
            setCollectionRoot(null, context.mainFrame.treeSystemOne, context.cfg.gs("Browser.open.a.publisher"), false);
        }
        //
        // treeSystemOne tree expansion event handler
        context.mainFrame.treeSystemOne.addTreeWillExpandListener(new TreeWillExpandListener()
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
        context.mainFrame.treeSystemOne.addTreeSelectionListener(new TreeSelectionListener()
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
        context.mainFrame.treeSystemOne.setTransferHandler(navTransferHandler);
        addFocusListener(context.mainFrame.treeSystemOne);
        addHandlersToTable(context.mainFrame.tableSystemOne);
        addFocusListener(context.mainFrame.tableSystemOne);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(context.mainFrame.treeSystemOne);
    }

    private void initializeBrowserTwo()
    {
        // --- BrowserTwo ------------------------------------------
        //
        // --- tab selection handler
        context.mainFrame.tabbedPaneBrowserTwo.addChangeListener(new ChangeListener()
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
                        lastComponent = context.mainFrame.treeCollectionTwo;
                        model = context.mainFrame.treeCollectionTwo.getModel();
                        node = (NavTreeNode) context.mainFrame.treeCollectionTwo.getLastSelectedPathComponent();
                        tabStops[2] = 4;
                        tabStops[3] = 5;
                        break;
                    case 1:
                        lastComponent = context.mainFrame.treeSystemTwo;
                        model = context.mainFrame.treeSystemTwo.getModel();
                        node = (NavTreeNode) context.mainFrame.treeSystemTwo.getLastSelectedPathComponent();
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
        context.mainFrame.treeCollectionTwo.setName("treeCollectionTwo");
        if (context.subscriberRepo != null && context.subscriberRepo.isInitialized())
        {
            File json = new File(context.subscriberRepo.getJsonFilename());
            String path = json.getAbsolutePath();
            context.preferences.setLastSubscriberOpenFile(path);
            context.preferences.setLastSubscriberOpenPath(FilenameUtils.getFullPathNoEndSeparator(path));

            loadCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(null, context.mainFrame.treeCollectionTwo, context.cfg.gs("Browser.open.a.subscriber"), context.cfg.isRemoteSession());
        }
        //
        // treeCollectionTwo tree expansion event handler
        context.mainFrame.treeCollectionTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
        context.mainFrame.treeCollectionTwo.addTreeSelectionListener(new TreeSelectionListener()
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
        context.mainFrame.treeCollectionTwo.setTransferHandler(navTransferHandler);
        addFocusListener(context.mainFrame.treeCollectionTwo);
        addHandlersToTable(context.mainFrame.tableCollectionTwo);
        addFocusListener(context.mainFrame.tableCollectionTwo);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(context.mainFrame.treeCollectionTwo);

        // --- treeSystemTwo
        context.mainFrame.treeSystemTwo.setName("treeSystemTwo");
        if (context.subscriberRepo != null && context.subscriberRepo.isInitialized())
        {
            loadSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.cfg.isRemoteSession());
        }
        else
        {
            setCollectionRoot(null, context.mainFrame.treeSystemTwo, context.cfg.gs("Browser.open.a.subscriber"), context.cfg.isRemoteSession());
        }
        //
        // treeSystemTwo tree expansion event handler
        context.mainFrame.treeSystemTwo.addTreeWillExpandListener(new TreeWillExpandListener()
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
        context.mainFrame.treeSystemTwo.addTreeSelectionListener(new TreeSelectionListener()
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
        context.mainFrame.treeSystemTwo.setTransferHandler(navTransferHandler);
        addFocusListener(context.mainFrame.treeSystemTwo);
        addHandlersToTable(context.mainFrame.tableSystemTwo);
        addFocusListener(context.mainFrame.tableSystemTwo);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(context.mainFrame.treeSystemTwo);
    }

    private void initializeNavigation()
    {
        context.mainFrame.buttonBack.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navBack();
            }
        });

        context.mainFrame.buttonForward.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                navForward();
            }
        });

        context.mainFrame.buttonUp.addActionListener(new ActionListener()
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
        if (!context.navigator.showHintTrackingButton)
        {
            context.mainFrame.panelHintTracking.setVisible(false);
            hintTrackingEnabled = false;
        }
        else
            hintTrackingEnabled = true;

        // toggle hint tracking
        context.mainFrame.buttonHintTracking.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (actionEvent.getActionCommand() != null && actionEvent.getActionCommand().equalsIgnoreCase("hints"))
                {
                    // 1 hints, 2 tracker, 3 server
                    int level = context.cfg.getHintsDaemonFilename().length() > 0 ? 3 : (context.cfg.getHintTrackerFilename().length() > 0 ? 2 : 1);

                    if (context.mainFrame.panelHintTracking.isVisible())
                    {
                        if (!hintTrackingEnabled)
                        {
                            try
                            {
                                URL url = Thread.currentThread().getContextClassLoader().getResource("hint-green.png");
                                Image icon = ImageIO.read(url);
                                context.mainFrame.buttonHintTracking.setIcon(new ImageIcon(icon));
                                hintTrackingEnabled = true;
                                setHintTrackingButton(true);
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
                                context.mainFrame.buttonHintTracking.setIcon(new ImageIcon(icon));
                                String tt = level == 3 ? context.cfg.gs("Navigator.button.HintServer.disabled.tooltip") :
                                        (level == 2 ? context.cfg.gs("Navigator.button.HintTracking.disabled.tooltip") :
                                         context.cfg.gs("Navigator.button.Hints.disabled.tooltip"));
                                context.mainFrame.buttonHintTracking.setToolTipText(tt);
                                hintTrackingEnabled = false;
                                setHintTrackingButton(false);
                            }
                            catch (Exception e)
                            {
                            }
                        }
                    }
                }
            }
        });

        setHintTrackingButton(hintTrackingEnabled);
    }

    public boolean isHintTrackingEnabled()
    {
        return this.hintTrackingEnabled;
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
            context.fault = true;
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
            context.fault = true;
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

    public void printProperties(NavTreeUserObject tuo)
    {
        if (tuo == null || tuo.node == null || !tuo.node.isLoaded())
            return;

        if (!printPropertiesInUse)
        {
            printPropertiesInUse = true;
            context.mainFrame.textAreaProperties.setText("");
            String msg = "<html>";
            msg += "<style>table { margin:0; padding:0; }" +
                    "th { margin:0; padding:0; }" +
                    "td { text-align:left; }" +
                    "</style>";
            msg += "<body>";
            msg += context.cfg.gs("Properties.type") + tuo.getType() + "<br/>" + System.getProperty("line.separator");

            try
            {
                switch (tuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        break;
                    case NavTreeUserObject.COLLECTION:
                        msg += context.cfg.gs("Properties.libraries") + tuo.node.getChildCount(false, false) +
                                "<br/>" + System.getProperty("line.separator") +
                                context.cfg.gs("Properties.path") + tuo.node.getMyRepo().getJsonFilename() +
                                "<br/>" + System.getProperty("line.separator");
                        break;
                    case NavTreeUserObject.COMPUTER:
                        msg += context.cfg.gs("Properties.drives") + tuo.node.getChildCount(false, false) + "<br/>" + System.getProperty("line.separator");
                        break;
                    case NavTreeUserObject.DRIVE:
                        msg += context.cfg.gs("Properties.free") + Utils.formatLong(getFreespace(tuo), true, context.cfg.getLongScale()) + "<br/>" + System.getProperty("line.separator");
                        break;
                    case NavTreeUserObject.HOME:
                        msg += context.cfg.gs("Properties.free") + Utils.formatLong(getFreespace(tuo), true, context.cfg.getLongScale()) + "<br/>" + System.getProperty("line.separator");
                        break;
                    case NavTreeUserObject.LIBRARY:
                        msg += "<table cellpadding=\"0\" cellspacing=\"0\">" +
                                "<tr><td>" + MessageFormat.format(context.cfg.gs("Properties.sources"), tuo.sources.length) + "</td> <td></td> <td>" +
                                context.cfg.gs("Properties.free") + "</td></tr>";
                        if (tuo.isRemote)
                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        for (String source : tuo.sources)
                        {
                            String free = Utils.formatLong(getFreespace(source, tuo.isRemote), true, context.cfg.getLongScale());
                            msg += "<tr><td>" + source + "</td> <td><div>&nbsp;&nbsp;&nbsp;&nbsp;</div></td> <td>" + free + "</td></tr>";
                        }
                        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        msg += "</table>";
                        msg += System.getProperty("line.separator");
                        break;
                    case NavTreeUserObject.REAL:
                        msg += context.cfg.gs("Properties.path") + tuo.path + "<br/>" + System.getProperty("line.separator");
                        if (!tuo.isDir)
                            msg += context.cfg.gs("Properties.size") + Utils.formatLong(tuo.size, true, context.cfg.getLongScale()) + "<br/>" + System.getProperty("line.separator");
                        if (tuo.path.endsWith(".els"))
                        {
                            String content = context.transfer.readTextFile(tuo);
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
                context.mainFrame.textAreaProperties.setText(msg);
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
            printPropertiesInUse = false;
        }
    }

    public void refreshAll()
    {
        refreshTree(context.mainFrame.treeCollectionOne);
        refreshTree(context.mainFrame.treeSystemOne);
        refreshTree(context.mainFrame.treeCollectionTwo);
        refreshTree(context.mainFrame.treeSystemTwo);
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

    public synchronized void refreshTree(JTree tree)
    {
        if (tree != null)
        {
            tree.setEnabled(false);
            TreePath rootPath = ((NavTreeNode) tree.getModel().getRoot()).getTreePath();
            Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(rootPath);
            TreePath[] paths = tree.getSelectionPaths();
            ((NavTreeModel)tree.getModel()).reload();
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
                tree = panelName.contains("collection") ? context.mainFrame.treeCollectionOne : context.mainFrame.treeSystemOne;
                table = panelName.contains("collection") ? context.mainFrame.tableCollectionOne : context.mainFrame.tableSystemOne;
            }
            else // subscriber
            {
                tree = panelName.contains("collection") ? context.mainFrame.treeCollectionTwo : context.mainFrame.treeSystemTwo;
                table = panelName.contains("collection") ? context.mainFrame.tableCollectionTwo : context.mainFrame.tableSystemTwo;
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
            if ((!nodes[nodeIndex - 1].getUserObject().isDir && context.preferences.isHideFilesInTree()))
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
                    int panelNo = context.browser.getPanelNumber(panelName);
                    table = (JTable) context.browser.getTabComponent(panelNo);
                    if (table != null)
                    {
                        table.requestFocus();
                        int index = context.browser.findRowIndex(table, pathElements[pathElements.length - 1]); // last element
                        if (index > -1)
                        {
                            // select and scroll into view
                            table.setRowSelectionInterval(index, index);
                            table.scrollRectToVisible(new Rectangle(table.getCellRect(index, index, true)));
                            table.scrollRectToVisible(new Rectangle(table.getCellRect(index, index, true)));
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
                String message = java.text.MessageFormat.format(context.cfg.gs("Navigator.menu.New.folder.select.library.source"), tuo.sources.length, tuo.name);
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

                int opt = JOptionPane.showConfirmDialog(context.mainFrame, params, context.cfg.getNavigatorName(), JOptionPane.OK_CANCEL_OPTION);
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
                    context.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(0);
                    context.mainFrame.treeCollectionOne.requestFocus();
                    break;
                case 1:
                    context.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(0);
                    context.mainFrame.tableCollectionOne.requestFocus();
                    break;
                case 2:
                    context.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(1);
                    context.mainFrame.treeSystemOne.requestFocus();
                    break;
                case 3:
                    context.mainFrame.tabbedPaneBrowserOne.setSelectedIndex(1);
                    context.mainFrame.tableSystemOne.requestFocus();
                    break;
                case 4:
                    context.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(0);
                    context.mainFrame.treeCollectionTwo.requestFocus();
                    break;
                case 5:
                    context.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(0);
                    context.mainFrame.tableCollectionTwo.requestFocus();
                    break;
                case 6:
                    context.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(1);
                    context.mainFrame.treeSystemTwo.requestFocus();
                    break;
                case 7:
                    context.mainFrame.tabbedPaneBrowserTwo.setSelectedIndex(1);
                    context.mainFrame.tableSystemTwo.requestFocus();
                    break;
            }
        }
    }

    public void setHintTrackingButton(boolean enabled)
    {
        String tt;
        tt = context.cfg.getHintsDaemonFilename().length() > 0 ? context.cfg.gs("Navigator.button.HintServer.text") :
                (context.cfg.getHintTrackerFilename().length() > 0 ? context.cfg.gs("Navigator.button.HintTracking.text") :
                        context.cfg.gs("Navigator.button.HintsKeys.text"));
        context.mainFrame.buttonHintTracking.setText(tt);

        if (enabled)
        {
            tt = context.cfg.getHintsDaemonFilename().length() > 0 ? context.cfg.gs("Navigator.button.HintServer.enabled.tooltip") :
                    (context.cfg.getHintTrackerFilename().length() > 0 ? context.cfg.gs("Navigator.button.HintTracking.enabled.tooltip") :
                            context.cfg.gs("Navigator.button.Hints.enabled.tooltip"));
        }
        else
        {
            tt = context.cfg.getHintsDaemonFilename().length() > 0  ? context.cfg.gs("Navigator.button.HintServer.disabled.tooltip") :
                    (context.cfg.getHintTrackerFilename().length() > 0 ? context.cfg.gs("Navigator.button.HintTracking.disabled.tooltip") :
                            context.cfg.gs("Navigator.button.Hints.disabled.tooltip"));
        }
        context.mainFrame.buttonHintTracking.setToolTipText(tt);
    }

    private NavTreeNode setCollectionRoot(Repository repo, JTree tree, String title, boolean remote)
    {
        NavTreeNode root = new NavTreeNode(context, repo, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, title, NavTreeUserObject.COLLECTION, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(context.preferences.isHideFilesInTree());
        tree.setCellRenderer(new NavTreeCellRenderer(context));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
        tree.setModel(model);
        return root;
    }

    private void styleCollectionAll(JTree tree, Repository repo, boolean remote, boolean deep, boolean recursive) throws Exception
    {
        NavTreeModel model = (NavTreeModel) tree.getModel();
        NavTreeNode root = (NavTreeNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            NavTreeNode node = new NavTreeNode(context, repo, tree);
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
        NavTreeNode root = new NavTreeNode(context, repo, tree);
        NavTreeUserObject tuo = new NavTreeUserObject(root, context.cfg.gs("Browser.system"), NavTreeUserObject.SYSTEM, remote);
        root.setNavTreeUserObject(tuo);
        NavTreeModel model = new NavTreeModel(root, true);
        model.activateFilter(context.preferences.isHideFilesInTree());
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setCellRenderer(new NavTreeCellRenderer(context));
        tree.setModel(model);

        // add Computer node
        NavTreeNode rootNode = new NavTreeNode(context, repo, tree);
        tuo = new NavTreeUserObject(rootNode, context.cfg.gs("Browser.computer"), NavTreeUserObject.COMPUTER, remote);
        rootNode.setNavTreeUserObject(tuo);
        root.add(rootNode);
        if (remote && tree.getName().equalsIgnoreCase("treeSystemTwo"))
        {
            NavTreeNode node = new NavTreeNode(context, repo, tree);
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
                NavTreeNode node = new NavTreeNode(context, repo, tree);
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
            // add user's Home directory root node
            NavTreeNode homeNode = new NavTreeNode(context, repo, tree);
            tuo = new NavTreeUserObject(homeNode, context.cfg.gs("Browser.home"), System.getProperty("user.home"), NavTreeUserObject.HOME, false);
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

    public void toggleShowHiddenFiles()
    {
        Object object = lastComponent;

        context.preferences.setHideHiddenFiles(!context.preferences.isHideHiddenFiles());
        if (context.preferences.isHideHiddenFiles())
            context.mainFrame.menuItemShowHidden.setSelected(false);
        else
            context.mainFrame.menuItemShowHidden.setSelected(true);

        refreshAll();
        JTree tree = null;
        if (object instanceof JTree)
        {
            tree = (JTree) object;
        }
        else if (object instanceof JTable)
        {
            tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
        }
        if (tree != null)
        {
            NavTreeNode node = (NavTreeNode) tree.getLastSelectedPathComponent();
            if (node != null)
                node.loadTable();
        }
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
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Touch.cannot") + tuo.name, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
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
            if (context.preferences.isShowTouchConfirmation())
            {
                String msg = MessageFormat.format(context.cfg.gs("Navigator.menu.Touch.are.you.sure1"),
                        rows.length, isRemote ? 0 : 1, rows.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false, context.cfg.getLongScale()));
                msg += (dirCount > 0 ? MessageFormat.format(context.cfg.gs("Navigator.menu.Touch.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
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
                            long seconds = context.transfer.touch(tuo.path, tuo.isRemote);
                            if (tuo.isRemote)
                            {
                                tuo.mtime = (int) seconds;
                            }
                            tuo.fileTime = FileTime.from(seconds, TimeUnit.SECONDS);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Touch.error") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                        logger.info(context.cfg.gs("Browser.skipping") + tuo.name);
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
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Touch.cannot") + tuo.name, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
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
            if (context.preferences.isShowTouchConfirmation())
            {
                String msg = MessageFormat.format(context.cfg.gs("Navigator.menu.Touch.are.you.sure1"),
                        paths.length, isRemote ? 0 : 1, paths.length > 1 ? 0 : 1,
                        fileCount, fileCount > 1 ? 0 : 1, Utils.formatLong(size, false, context.cfg.getLongScale()));
                msg += (dirCount > 0 ? MessageFormat.format(context.cfg.gs("Navigator.menu.Touch.are.you.sure2"), dirCount > 1 ? 0 : 1) : "");
                msg += (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : "");
                reply = JOptionPane.showConfirmDialog(context.mainFrame, msg,
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
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
                            long seconds = context.transfer.touch(tuo.path, tuo.isRemote);
                            if (tuo.isRemote)
                            {
                                tuo.mtime = (int) seconds;
                            }
                            tuo.fileTime = FileTime.from(seconds, TimeUnit.SECONDS);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Touch.error") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                        logger.info(context.cfg.gs("Browser.skipping") + tuo.name);
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
