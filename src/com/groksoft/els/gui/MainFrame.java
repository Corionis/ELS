package com.groksoft.els.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.browser.BrowserTableModel;
import com.groksoft.els.gui.util.RotatedIcon;
import com.groksoft.els.gui.util.SmartScroller;
import com.groksoft.els.gui.util.TextIcon;
import com.groksoft.els.gui.util.VerticalLabel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;

/**
 * Navigator graphical user interface main JFrame.
 * <p><br/>
 * This class is primarily the definitions of the GUI. The functional implementations are
 * in the main tab-named related classes.<br/>
 * <br/>
 * Designed with: <br/>
 *  - JFormDesigner, https://www.formdev.com/jformdesigner/doc/ <br/>
 * <br/>
 * Uses free components from FormDev: <br/>
 *  - FlatLaf look 'n feel, https://www.formdev.com/flatlaf/ <br/>
 *  - Download from: https://search.maven.org/artifact/com.formdev/flatlaf <br/>
 */
public class MainFrame extends JFrame
{
    private transient Logger logger = LogManager.getLogger("applog");
    private GuiContext guiContext;
    private LookAndFeel laf;

    public MainFrame(GuiContext guiContext)
    {
        this.guiContext = guiContext;

        try
        {
            if (guiContext.preferences.getLookAndFeel() == 0)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            else
            {
                laf = getLookAndFeel(guiContext.preferences.getLookAndFeel());
                UIManager.setLookAndFeel(laf);
            }

            initComponents();
            setTitle(guiContext.cfg.getNavigatorName()); // + " " + guiContext.cfg.getProgramVersion());
            rotateBrowserTabs();

            // setup the right-side tables
            tableCollectionOne.setName("tableCollectionOne");
            tableCollectionOne.setAutoCreateRowSorter(true);
            tableCollectionOne.setShowGrid(false);
            tableCollectionOne.getTableHeader().setReorderingAllowed(false);
            tableCollectionOne.setRowSelectionAllowed(true);
            tableCollectionOne.setColumnSelectionAllowed(false);
            tableCollectionOne.setModel(new BrowserTableModel(guiContext.cfg));
            adjustTableColumns(tableCollectionOne);

            tableSystemOne.setName("tableSystemOne");
            tableSystemOne.setAutoCreateRowSorter(true);
            tableSystemOne.setShowGrid(false);
            tableSystemOne.getTableHeader().setReorderingAllowed(false);
            tableSystemOne.setRowSelectionAllowed(true);
            tableSystemOne.setColumnSelectionAllowed(false);
            tableSystemOne.setModel(new BrowserTableModel(guiContext.cfg));
            adjustTableColumns(tableSystemOne);

            tableCollectionTwo.setName("tableCollectionTwo");
            tableCollectionTwo.setAutoCreateRowSorter(true);
            tableCollectionTwo.setShowGrid(false);
            tableCollectionTwo.getTableHeader().setReorderingAllowed(false);
            tableCollectionTwo.setRowSelectionAllowed(true);
            tableCollectionTwo.setColumnSelectionAllowed(false);
            tableCollectionTwo.setModel(new BrowserTableModel(guiContext.cfg));
            adjustTableColumns(tableCollectionTwo);

            tableSystemTwo.setName("tableSystemTwo");
            tableSystemTwo.setAutoCreateRowSorter(true);
            tableSystemTwo.setShowGrid(false);
            tableSystemTwo.getTableHeader().setReorderingAllowed(false);
            tableSystemTwo.setRowSelectionAllowed(true);
            tableSystemTwo.setColumnSelectionAllowed(false);
            tableSystemTwo.setModel(new BrowserTableModel(guiContext.cfg));
            adjustTableColumns(tableSystemTwo);

            // set Back/Forward keys
            buttonBack.setMnemonic(KeyEvent.VK_LEFT);
            buttonForward.setMnemonic(KeyEvent.VK_RIGHT);

            // set Up key and rotate the text
            // http://www.camick.com/java/source/TextIcon.java
            TextIcon t1 = new TextIcon(buttonUp, ">", TextIcon.Layout.HORIZONTAL);
            buttonUp.setText("");
            // http://www.camick.com/java/source/RotatedIcon.java
            RotatedIcon r1 = new RotatedIcon(t1, RotatedIcon.Rotate.UP);
            buttonUp.setIcon(r1);
            buttonUp.setMnemonic(KeyEvent.VK_UP);

            // add smart scroll to the log
            // https://tips4java.wordpress.com/2013/03/03/smart-scrolling/
            new SmartScroller(scrollPaneLog);
        }
        catch(Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            guiContext.context.fault = true;
        }
    }

    private void adjustTableColumns(JTable table)
    {
        for (int i = 0; i < table.getColumnCount(); ++i)
        {
            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            TableColumn column = table.getColumnModel().getColumn(i);
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
                    cellRenderer.setHorizontalAlignment(JLabel.LEFT);
                    column.setCellRenderer(cellRenderer);
                    column.setResizable(true);
                    break;
                case 2:
                    cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
                    column.setCellRenderer(cellRenderer);
                    column.setResizable(true);
                    break;
                case 3:
                    column.setResizable(true);
                    cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
                    column.setCellRenderer(cellRenderer);
                    break;
            }
        }
    }

    public LookAndFeel getLookAndFeel(int value)
    {
        switch (value)
        {
            // Built-in themes
            case 1:
                laf = new MetalLookAndFeel();
                break;
            case 2:
                laf = new NimbusLookAndFeel();
                break;
            // FlatLaf themes
            case 3:
                laf = new FlatLightLaf();
                break;
            case 4:
                laf = new FlatDarkLaf();
                break;
            case 5:
                laf = new FlatIntelliJLaf();
                break;
            case 6:
            default:
                laf = new FlatDarculaLaf();
                break;
        }
        return laf;
    }

    private void menuItemFileQuitActionPerformed(ActionEvent e)
    {
        if (verifyClose())
            guiContext.navigator.stop();
    }

    public void rotateBrowserTabs()
    {
        // change browser tabs orientation to vertical
        JLabel label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionOne.tab.title"));
        label.setUI(new VerticalLabel(false));
        tabbedPaneBrowserOne.setTabComponentAt(0, label);
        //
        label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemOne.tab.title"));
        label.setUI(new VerticalLabel(false));
        tabbedPaneBrowserOne.setTabComponentAt(1, label);

        label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionTwo.tab.title"));
        label.setUI(new VerticalLabel(false));
        tabbedPaneBrowserTwo.setTabComponentAt(0, label);
        //
        label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemTwo.tab.title"));
        label.setUI(new VerticalLabel(false));
        tabbedPaneBrowserTwo.setTabComponentAt(1, label);
    }

    private void thisWindowClosing(WindowEvent e)
    {
        if (verifyClose())
            guiContext.navigator.stop();
    }

    public boolean verifyClose()
    {
        if (guiContext.progress != null && guiContext.progress.isBeingUsed())
        {
            int r = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                    guiContext.cfg.gs("MainFrame.transfers.are.active.are.you.sure"),
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                return false;
        }
        if (guiContext.browser.navTransferHandler.getTransferWorker() != null &&
                !guiContext.browser.navTransferHandler.getTransferWorker().isDone())
        {
            logger.warn(guiContext.cfg.gs("MainFrame.cancelling.transfers.at.user.request"));
            guiContext.browser.navTransferHandler.getTransferWorker().cancel(true);
            guiContext.context.fault = true;
        }
        return true;
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        menuBarMain = new JMenuBar();
        menuFile = new JMenu();
        menuItemOpenPublisher = new JMenuItem();
        menuItemOpenSubscriber = new JMenuItem();
        menuItemOpenHintKeys = new JMenuItem();
        menuItemOpenHintServer = new JMenuItem();
        menuItemSaveLayout = new JMenuItem();
        menuItemQuitTerminate = new JMenuItem();
        menuItemFileQuit = new JMenuItem();
        menuEdit = new JMenu();
        menuItemFind = new JMenuItem();
        menuItemNewFolder = new JMenuItem();
        menuItemRename = new JMenuItem();
        menuItemTouch = new JMenuItem();
        menuItemCopy = new JMenuItem();
        menuItemCut = new JMenuItem();
        menuItemPaste = new JMenuItem();
        menuItemDelete = new JMenuItem();
        menuItemSettings = new JMenuItem();
        menuView = new JMenu();
        menuItemRefresh = new JMenuItem();
        menuItemProgress = new JMenuItem();
        menuItemShowHidden = new JCheckBoxMenuItem();
        menuBookmarks = new JMenu();
        menuItemAddBookmark = new JMenuItem();
        menuItemBookmarksDelete = new JMenuItem();
        menuTools = new JMenu();
        menuItemDuplicates = new JMenuItem();
        menuItemEmptyFinder = new JMenuItem();
        menuItemJunk = new JMenuItem();
        menuItemRenamer = new JMenuItem();
        menuItemExternalTools = new JMenuItem();
        menuItem1 = new JMenuItem();
        menuItemPlexGenerator = new JMenuItem();
        menuJobs = new JMenu();
        menuItemJobsManage = new JMenuItem();
        menuWindows = new JMenu();
        menuItemMaximize = new JMenuItem();
        menuItemMinimize = new JMenuItem();
        menuItemRestore = new JMenuItem();
        menuItemSplitHorizontal = new JMenuItem();
        menuItemSplitVertical = new JMenuItem();
        menuHelp = new JMenu();
        menuItemControls = new JMenuItem();
        menuItemDocumentation = new JMenuItem();
        menuItemGitHubProject = new JMenuItem();
        menuItemUpdates = new JMenuItem();
        menuItemAbout = new JMenuItem();
        panelMain = new JPanel();
        tabbedPaneMain = new JTabbedPane();
        splitPaneBrowser = new JSplitPane();
        panelBrowserTop = new JPanel();
        panelLocationAndButtons = new JPanel();
        vSpacer1 = new JPanel(null);
        panelLocation = new JPanel();
        panelLocationLeft = new JPanel();
        buttonBack = new JButton();
        buttonForward = new JButton();
        buttonUp = new JButton();
        textFieldLocation = new JTextField();
        hSpacer1 = new JPanel(null);
        panelHintTracking = new JPanel();
        buttonHintTracking = new JButton();
        hSpacer2 = new JPanel(null);
        vSpacer2 = new JPanel(null);
        splitPaneTwoBrowsers = new JSplitPane();
        tabbedPaneBrowserOne = new JTabbedPane();
        panelCollectionOne = new JPanel();
        splitPaneCollectionOne = new JSplitPane();
        scrollPaneTreeCollectionOne = new JScrollPane();
        treeCollectionOne = new JTree();
        scrollPaneTableCollectionOne = new JScrollPane();
        tableCollectionOne = new JTable();
        panelSystemOne = new JPanel();
        splitPaneSystemOne = new JSplitPane();
        scrollPaneTreeSystemOne = new JScrollPane();
        treeSystemOne = new JTree();
        scrollPaneTableSystemOne = new JScrollPane();
        tableSystemOne = new JTable();
        tabbedPaneBrowserTwo = new JTabbedPane();
        panelCollectionTwo = new JPanel();
        splitPaneCollectionTwo = new JSplitPane();
        scrollPaneTreeCollectionTwo = new JScrollPane();
        treeCollectionTwo = new JTree();
        scrollPaneTableCollectionTwo = new JScrollPane();
        tableCollectionTwo = new JTable();
        panelSystemTwo = new JPanel();
        splitPaneSystemTwo = new JSplitPane();
        scrollPaneTreeSystemTwo = new JScrollPane();
        treeSystemTwo = new JTree();
        scrollPaneTableSystemTwo = new JScrollPane();
        tableSystemTwo = new JTable();
        tabbedPaneNavigatorBottom = new JTabbedPane();
        scrollPaneLog = new JScrollPane();
        textAreaLog = new JTextArea();
        scrollPaneProperties = new JScrollPane();
        textAreaProperties = new JEditorPane();
        splitPaneBackup = new JSplitPane();
        panelLibraries = new JPanel();
        panelStatus = new JPanel();
        labelStatusLeft = new JLabel();
        labelStatusMiddle = new JLabel();
        labelStatusRight = new JLabel();
        popupMenuBrowser = new JPopupMenu();
        popupMenuItemFind = new JMenuItem();
        popupMenuItemNewFolder = new JMenuItem();
        popupMenuItemRename = new JMenuItem();
        popupMenuItemTouch = new JMenuItem();
        popupMenuItemCopy = new JMenuItem();
        popupMenuItemCut = new JMenuItem();
        popupMenuItemPaste = new JMenuItem();
        popupMenuItemDelete = new JMenuItem();
        popupMenuLog = new JPopupMenu();
        popupMenuItemBottom = new JMenuItem();
        popupMenuItemClear = new JMenuItem();

        //======== this ========
        setMinimumSize(new Dimension(100, 100));
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationByPlatform(true);
        setTitle("ELS Navigator");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== menuBarMain ========
        {

            //======== menuFile ========
            {
                menuFile.setText(guiContext.cfg.gs("Navigator.menu.File.text"));
                menuFile.setMnemonic(guiContext.cfg.gs("Navigator.menu.File.mnemonic").charAt(0));

                //---- menuItemOpenPublisher ----
                menuItemOpenPublisher.setText(guiContext.cfg.gs("Navigator.menu.OpenPublisher.text"));
                menuItemOpenPublisher.setMnemonic(guiContext.cfg.gs("Navigator.menu.OpenPublisher.mnemonic").charAt(0));
                menuItemOpenPublisher.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemOpenPublisher.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemOpenPublisher.setDisplayedMnemonicIndex(5);
                menuFile.add(menuItemOpenPublisher);

                //---- menuItemOpenSubscriber ----
                menuItemOpenSubscriber.setText(guiContext.cfg.gs("Navigator.menu.OpenSubscriber.text"));
                menuItemOpenSubscriber.setMnemonic(guiContext.cfg.gs("Navigator.menu.OpenSubscriber.mnemonic").charAt(0));
                menuItemOpenSubscriber.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemOpenSubscriber.setHorizontalTextPosition(SwingConstants.LEFT);
                menuFile.add(menuItemOpenSubscriber);

                //---- menuItemOpenHintKeys ----
                menuItemOpenHintKeys.setText(guiContext.cfg.gs("Navigator.menu.OpenHintKeys.text"));
                menuItemOpenHintKeys.setSelected(true);
                menuItemOpenHintKeys.setMnemonic(guiContext.cfg.gs("Navigator.menu.OpenHintKeys.mnemonic").charAt(0));
                menuItemOpenHintKeys.setHorizontalTextPosition(SwingConstants.LEFT);
                menuFile.add(menuItemOpenHintKeys);

                //---- menuItemOpenHintServer ----
                menuItemOpenHintServer.setText(guiContext.cfg.gs("Navigator.menu.OpenHintServer.text"));
                menuItemOpenHintServer.setMnemonic(guiContext.cfg.gs("Navigator.menu.OpenHintServer.mnemonic").charAt(0));
                menuItemOpenHintServer.setEnabled(false);
                menuItemOpenHintServer.setHorizontalTextPosition(SwingConstants.LEFT);
                menuFile.add(menuItemOpenHintServer);
                menuFile.addSeparator();

                //---- menuItemSaveLayout ----
                menuItemSaveLayout.setText(guiContext.cfg.gs("Navigator.menu.SaveLayout.text"));
                menuItemSaveLayout.setMnemonic(guiContext.cfg.gs("Navigator.menu.SaveLayout.mnemonic_3").charAt(0));
                menuItemSaveLayout.setHorizontalTextPosition(SwingConstants.LEFT);
                menuFile.add(menuItemSaveLayout);
                menuFile.addSeparator();

                //---- menuItemQuitTerminate ----
                menuItemQuitTerminate.setText(guiContext.cfg.gs("Navigator.menu.QuitTerminate.text"));
                menuItemQuitTerminate.setMnemonic(guiContext.cfg.gs("Navigator.menu.QuitTerminate.mnemonic").charAt(0));
                menuItemQuitTerminate.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemQuitTerminate.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemQuitTerminate.setDisplayedMnemonicIndex(8);
                menuFile.add(menuItemQuitTerminate);

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(guiContext.cfg.gs("Navigator.menu.Quit.text"));
                menuItemFileQuit.setMnemonic(guiContext.cfg.gs("Navigator.menu.Quit.mnemonic").charAt(0));
                menuItemFileQuit.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemFileQuit.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFileQuit.addActionListener(e -> menuItemFileQuitActionPerformed(e));
                menuFile.add(menuItemFileQuit);
            }
            menuBarMain.add(menuFile);

            //======== menuEdit ========
            {
                menuEdit.setText(guiContext.cfg.gs("Navigator.menu.Edit.text"));
                menuEdit.setMnemonic(guiContext.cfg.gs("Navigator.menu.Edit.mnemonic").charAt(0));

                //---- menuItemFind ----
                menuItemFind.setText(guiContext.cfg.gs("Navigator.menu.Find.text"));
                menuItemFind.setMnemonic(guiContext.cfg.gs("Navigator.menu.Find.mnemonic").charAt(0));
                menuItemFind.setEnabled(false);
                menuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
                menuItemFind.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFind.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemFind);

                //---- menuItemNewFolder ----
                menuItemNewFolder.setText(guiContext.cfg.gs("Navigator.menu.New.folder.text"));
                menuItemNewFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
                menuItemNewFolder.setMnemonic(guiContext.cfg.gs("Navigator.menu.New.folder.mnemonic").charAt(0));
                menuItemNewFolder.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemNewFolder.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemNewFolder);

                //---- menuItemRename ----
                menuItemRename.setText(guiContext.cfg.gs("Navigator.menu.Rename.text"));
                menuItemRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
                menuItemRename.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemRename.setMnemonic(guiContext.cfg.gs("Navigator.menu.Rename.mnemonic").charAt(0));
                menuItemRename.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemRename);

                //---- menuItemTouch ----
                menuItemTouch.setText(guiContext.cfg.gs("Navigator.menu.Touch.text"));
                menuItemTouch.setMnemonic(guiContext.cfg.gs("Navigator.menu.Touch.mnemonic").charAt(0));
                menuItemTouch.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemTouch.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemTouch);
                menuEdit.addSeparator();

                //---- menuItemCopy ----
                menuItemCopy.setText(guiContext.cfg.gs("Navigator.menu.Copy.text"));
                menuItemCopy.setMnemonic(guiContext.cfg.gs("Navigator.menu.Copy.mnemonic").charAt(0));
                menuItemCopy.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
                menuItemCopy.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(guiContext.cfg.gs("Navigator.menu.Cut.text"));
                menuItemCut.setMnemonic(guiContext.cfg.gs("Navigator.menu.Cut.mnemonic").charAt(0));
                menuItemCut.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
                menuItemCut.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(guiContext.cfg.gs("Navigator.menu.Paste.text"));
                menuItemPaste.setMnemonic(guiContext.cfg.gs("Navigator.menu.Paste.mnemonic").charAt(0));
                menuItemPaste.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
                menuItemPaste.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemDelete ----
                menuItemDelete.setText(guiContext.cfg.gs("Navigator.menu.Delete.text"));
                menuItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                menuItemDelete.setMnemonic(guiContext.cfg.gs("Navigator.menu.Delete.mnemonic").charAt(0));
                menuItemDelete.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDelete.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemDelete);
                menuEdit.addSeparator();

                //---- menuItemSettings ----
                menuItemSettings.setText(guiContext.cfg.gs("Navigator.menu.Settings.text"));
                menuItemSettings.setMnemonic(guiContext.cfg.gs("Navigator.menu.Settings.mnemonic").charAt(0));
                menuItemSettings.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSettings.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemSettings);
            }
            menuBarMain.add(menuEdit);

            //======== menuView ========
            {
                menuView.setText(guiContext.cfg.gs("Navigator.menu.View.text"));
                menuView.setMnemonic(guiContext.cfg.gs("Navigator.menu.View.mnemonic").charAt(0));

                //---- menuItemRefresh ----
                menuItemRefresh.setText(guiContext.cfg.gs("Navigator.menu.Refresh.text"));
                menuItemRefresh.setMnemonic(guiContext.cfg.gs("Navigator.menu.Refresh.mnemonic").charAt(0));
                menuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
                menuItemRefresh.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemRefresh.setHorizontalTextPosition(SwingConstants.LEFT);
                menuView.add(menuItemRefresh);

                //---- menuItemProgress ----
                menuItemProgress.setText(guiContext.cfg.gs("Navigator.menu.Progress.text"));
                menuItemProgress.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemProgress.setMnemonic(guiContext.cfg.gs("Navigator.menu.Progress.mnemonic").charAt(0));
                menuItemProgress.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemProgress.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
                menuView.add(menuItemProgress);
                menuView.addSeparator();

                //---- menuItemShowHidden ----
                menuItemShowHidden.setText(guiContext.cfg.gs("Navigator.menu.ShowHidden.text"));
                menuItemShowHidden.setMnemonic(guiContext.cfg.gs("Navigator.menu.ShowHidden.mnemonic").charAt(0));
                menuItemShowHidden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
                menuItemShowHidden.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemShowHidden.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemShowHidden.setDisplayedMnemonicIndex(5);
                menuView.add(menuItemShowHidden);
            }
            menuBarMain.add(menuView);

            //======== menuBookmarks ========
            {
                menuBookmarks.setText(guiContext.cfg.gs("Navigator.menu.Bookmarks.text"));
                menuBookmarks.setMnemonic(guiContext.cfg.gs("Navigator.menu.Bookmarks.mnemonic").charAt(0));

                //---- menuItemAddBookmark ----
                menuItemAddBookmark.setText(guiContext.cfg.gs("Navigator.menu.AddBookmark.text"));
                menuItemAddBookmark.setMnemonic(guiContext.cfg.gs("Navigator.menu.AddBookmark.mnemonic").charAt(0));
                menuItemAddBookmark.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAddBookmark.setHorizontalAlignment(SwingConstants.LEFT);
                menuBookmarks.add(menuItemAddBookmark);

                //---- menuItemBookmarksDelete ----
                menuItemBookmarksDelete.setText(guiContext.cfg.gs("Navigator.menu.BookmarksManage.text"));
                menuItemBookmarksDelete.setMnemonic(guiContext.cfg.gs("Navigator.menu.BookmarksManage.mnemonic").charAt(0));
                menuItemBookmarksDelete.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemBookmarksDelete.setHorizontalAlignment(SwingConstants.LEFT);
                menuBookmarks.add(menuItemBookmarksDelete);
                menuBookmarks.addSeparator();
            }
            menuBarMain.add(menuBookmarks);

            //======== menuTools ========
            {
                menuTools.setText(guiContext.cfg.gs("Navigator.menu.Tools.text"));
                menuTools.setMnemonic(guiContext.cfg.gs("Navigator.menu.Tools.mnemonic").charAt(0));

                //---- menuItemDuplicates ----
                menuItemDuplicates.setText(guiContext.cfg.gs("Navigator.menu.Duplicates.text"));
                menuItemDuplicates.setMnemonic(guiContext.cfg.gs("Navigator.menu.Duplicates.mnemonic").charAt(0));
                menuItemDuplicates.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDuplicates.setEnabled(false);
                menuItemDuplicates.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemDuplicates);

                //---- menuItemEmptyFinder ----
                menuItemEmptyFinder.setText(guiContext.cfg.gs("Navigator.menuItemEmptyFinder.text"));
                menuItemEmptyFinder.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemEmptyFinder.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemEmptyFinder.setEnabled(false);
                menuItemEmptyFinder.setMnemonic(guiContext.cfg.gs("Navigator.menuItemEmptyFinder.mnemonic").charAt(0));
                menuTools.add(menuItemEmptyFinder);

                //---- menuItemJunk ----
                menuItemJunk.setText(guiContext.cfg.gs("Navigator.menu.Junk.text"));
                menuItemJunk.setMnemonic(guiContext.cfg.gs("Navigator.menu.Junk.mnemonic").charAt(0));
                menuItemJunk.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemJunk.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemJunk);

                //---- menuItemRenamer ----
                menuItemRenamer.setText(guiContext.cfg.gs("Navigator.menu.Renamer.text"));
                menuItemRenamer.setMnemonic(guiContext.cfg.gs("Navigator.menu.Renamer.mnemonic").charAt(0));
                menuItemRenamer.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemRenamer.setEnabled(false);
                menuItemRenamer.setHorizontalTextPosition(SwingConstants.LEFT);
                menuTools.add(menuItemRenamer);
                menuTools.addSeparator();

                //---- menuItemExternalTools ----
                menuItemExternalTools.setText(guiContext.cfg.gs("Navigator.menu.ExternalTools.text"));
                menuItemExternalTools.setMnemonic(guiContext.cfg.gs("Navigator.menuItemExternalTools.mnemonic").charAt(0));
                menuItemExternalTools.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemExternalTools.setEnabled(false);
                menuItemExternalTools.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemExternalTools.setDisplayedMnemonicIndex(Integer.parseInt(guiContext.cfg.gs("Navigator.menuItemExternalTools.displayedMnemonicIndex")));
                menuTools.add(menuItemExternalTools);

                //---- menuItem1 ----
                menuItem1.setText("Handbrake");
                menuItem1.setMargin(new Insets(2, 6, 2, 2));
                menuItem1.setEnabled(false);
                menuTools.add(menuItem1);

                //---- menuItemPlexGenerator ----
                menuItemPlexGenerator.setText(guiContext.cfg.gs("Navigator.menu.PlexGenerator.text"));
                menuItemPlexGenerator.setMnemonic(guiContext.cfg.gs("Navigator.menu.PlexGenerator.mnemonic").charAt(0));
                menuItemPlexGenerator.setEnabled(false);
                menuItemPlexGenerator.setMargin(new Insets(2, 6, 2, 2));
                menuTools.add(menuItemPlexGenerator);
            }
            menuBarMain.add(menuTools);

            //======== menuJobs ========
            {
                menuJobs.setText(guiContext.cfg.gs("Navigator.menu.Jobs.text"));
                menuJobs.setMnemonic(guiContext.cfg.gs("Navigator.menu.Jobs.mnemonic").charAt(0));

                //---- menuItemJobsManage ----
                menuItemJobsManage.setText(guiContext.cfg.gs("Navigator.menu.JobsManage.text"));
                menuItemJobsManage.setMnemonic(guiContext.cfg.gs("Navigator.menu.JobsManage.mnemonic").charAt(0));
                menuItemJobsManage.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemJobsManage.setHorizontalTextPosition(SwingConstants.LEFT);
                menuJobs.add(menuItemJobsManage);
                menuJobs.addSeparator();
            }
            menuBarMain.add(menuJobs);

            //======== menuWindows ========
            {
                menuWindows.setText(guiContext.cfg.gs("Navigator.menu.Windows.text"));
                menuWindows.setMnemonic(guiContext.cfg.gs("Navigator.menu.Windows.mnemonic").charAt(0));

                //---- menuItemMaximize ----
                menuItemMaximize.setText(guiContext.cfg.gs("Navigator.menu.Maximize.text"));
                menuItemMaximize.setMnemonic(guiContext.cfg.gs("Navigator.menu.Maximize.mnemonic").charAt(0));
                menuItemMaximize.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemMaximize.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(guiContext.cfg.gs("Navigator.menu.Minimize.text"));
                menuItemMinimize.setMnemonic(guiContext.cfg.gs("Navigator.menu.Minimize.mnemonic").charAt(0));
                menuItemMinimize.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemMinimize.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemMinimize);

                //---- menuItemRestore ----
                menuItemRestore.setText(guiContext.cfg.gs("Navigator.menu.Restore.text"));
                menuItemRestore.setMnemonic(guiContext.cfg.gs("Navigator.menu.Restore.mnemonic").charAt(0));
                menuItemRestore.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemRestore.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemRestore);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(guiContext.cfg.gs("Navigator.menu.SplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(guiContext.cfg.gs("Navigator.menu.SplitHorizontal.mnemonic").charAt(0));
                menuItemSplitHorizontal.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSplitHorizontal.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(guiContext.cfg.gs("Navigator.menu.SplitVertical.text"));
                menuItemSplitVertical.setMnemonic(guiContext.cfg.gs("Navigator.menu.SplitVertical.mnemonic").charAt(0));
                menuItemSplitVertical.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSplitVertical.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemSplitVertical);
            }
            menuBarMain.add(menuWindows);

            //======== menuHelp ========
            {
                menuHelp.setText(guiContext.cfg.gs("Navigator.menu.Help.text"));
                menuHelp.setMnemonic(guiContext.cfg.gs("Navigator.menu.Help.mnemonic").charAt(0));

                //---- menuItemControls ----
                menuItemControls.setText(guiContext.cfg.gs("Navigator.menu.Controls.text"));
                menuItemControls.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemControls.setMnemonic(guiContext.cfg.gs("Navigator.menu.Controls.mnemonic").charAt(0));
                menuItemControls.setHorizontalTextPosition(SwingConstants.LEFT);
                menuHelp.add(menuItemControls);

                //---- menuItemDocumentation ----
                menuItemDocumentation.setText(guiContext.cfg.gs("Navigator.menu.Documentation.text"));
                menuItemDocumentation.setMnemonic(guiContext.cfg.gs("Navigator.menu.Documentation.mnemonic").charAt(0));
                menuItemDocumentation.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDocumentation.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemDocumentation.setToolTipText(guiContext.cfg.gs("Navigator.menuItemDocumentation.toolTipText"));
                menuHelp.add(menuItemDocumentation);

                //---- menuItemGitHubProject ----
                menuItemGitHubProject.setText(guiContext.cfg.gs("Navigator.menu.GitHubProject.text"));
                menuItemGitHubProject.setMnemonic(guiContext.cfg.gs("Navigator.menu.GitHubProject.mnemonic").charAt(0));
                menuItemGitHubProject.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemGitHubProject.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemGitHubProject.setToolTipText(guiContext.cfg.gs("Navigator.menuItemGitHubProject.toolTipText"));
                menuHelp.add(menuItemGitHubProject);
                menuHelp.addSeparator();

                //---- menuItemUpdates ----
                menuItemUpdates.setText(guiContext.cfg.gs("Navigator.menuItemUpdates.text"));
                menuItemUpdates.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemUpdates.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemUpdates.setMnemonic(guiContext.cfg.gs("Navigator.menuItemUpdates.mnemonic").charAt(0));
                menuItemUpdates.setEnabled(false);
                menuHelp.add(menuItemUpdates);

                //---- menuItemAbout ----
                menuItemAbout.setText(guiContext.cfg.gs("Navigator.menu.About.text"));
                menuItemAbout.setMnemonic(guiContext.cfg.gs("Navigator.menu.About.mnemonic").charAt(0));
                menuItemAbout.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAbout.setHorizontalAlignment(SwingConstants.LEFT);
                menuHelp.add(menuItemAbout);
            }
            menuBarMain.add(menuHelp);
        }
        setJMenuBar(menuBarMain);

        //======== panelMain ========
        {
            panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));

            //======== tabbedPaneMain ========
            {
                tabbedPaneMain.setFocusable(false);

                //======== splitPaneBrowser ========
                {
                    splitPaneBrowser.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBrowser.setLastDividerLocation(400);
                    splitPaneBrowser.setMinimumSize(new Dimension(0, 0));
                    splitPaneBrowser.setContinuousLayout(true);
                    splitPaneBrowser.setDividerLocation(400);

                    //======== panelBrowserTop ========
                    {
                        panelBrowserTop.setLayout(new BorderLayout());

                        //======== panelLocationAndButtons ========
                        {
                            panelLocationAndButtons.setFocusable(false);
                            panelLocationAndButtons.setPreferredSize(new Dimension(952, 36));
                            panelLocationAndButtons.setMinimumSize(new Dimension(219, 36));
                            panelLocationAndButtons.setMaximumSize(new Dimension(2147483647, 36));
                            panelLocationAndButtons.setLayout(new BorderLayout());

                            //---- vSpacer1 ----
                            vSpacer1.setPreferredSize(new Dimension(10, 4));
                            vSpacer1.setMinimumSize(new Dimension(10, 4));
                            vSpacer1.setMaximumSize(new Dimension(10, 4));
                            panelLocationAndButtons.add(vSpacer1, BorderLayout.NORTH);

                            //======== panelLocation ========
                            {
                                panelLocation.setFocusable(false);
                                panelLocation.setPreferredSize(new Dimension(952, 30));
                                panelLocation.setMinimumSize(new Dimension(191, 30));
                                panelLocation.setMaximumSize(new Dimension(2147483647, 30));
                                panelLocation.setLayout(new BorderLayout());

                                //======== panelLocationLeft ========
                                {
                                    panelLocationLeft.setPreferredSize(new Dimension(142, 30));
                                    panelLocationLeft.setMinimumSize(new Dimension(142, 30));
                                    panelLocationLeft.setMaximumSize(new Dimension(2147483647, 30));
                                    panelLocationLeft.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelLocationLeft.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
                                    ((GridBagLayout)panelLocationLeft.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0, 0.0, 1.0E-4};
                                    ((GridBagLayout)panelLocationLeft.getLayout()).rowWeights = new double[] {1.0};

                                    //---- buttonBack ----
                                    buttonBack.setText("<html>&lt;</html>");
                                    buttonBack.setMaximumSize(new Dimension(36, 30));
                                    buttonBack.setMinimumSize(new Dimension(36, 30));
                                    buttonBack.setPreferredSize(new Dimension(36, 30));
                                    buttonBack.setToolTipText(guiContext.cfg.gs("Navigator.button.Back.toolTipText"));
                                    buttonBack.setActionCommand("navBack");
                                    buttonBack.setFocusable(false);
                                    buttonBack.setDefaultCapable(false);
                                    buttonBack.setHorizontalTextPosition(SwingConstants.CENTER);
                                    panelLocationLeft.add(buttonBack, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 0, 0), 0, 0));

                                    //---- buttonForward ----
                                    buttonForward.setText("<html>&gt;</html>");
                                    buttonForward.setMaximumSize(new Dimension(36, 30));
                                    buttonForward.setMinimumSize(new Dimension(36, 30));
                                    buttonForward.setPreferredSize(new Dimension(36, 30));
                                    buttonForward.setToolTipText(guiContext.cfg.gs("Navigator.button.Forward.toolTipText"));
                                    buttonForward.setActionCommand("NavForward");
                                    buttonForward.setFocusable(false);
                                    buttonForward.setDefaultCapable(false);
                                    buttonForward.setHorizontalTextPosition(SwingConstants.CENTER);
                                    panelLocationLeft.add(buttonForward, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 0, 0), 0, 0));

                                    //---- buttonUp ----
                                    buttonUp.setText("^");
                                    buttonUp.setMaximumSize(new Dimension(36, 30));
                                    buttonUp.setMinimumSize(new Dimension(36, 30));
                                    buttonUp.setPreferredSize(new Dimension(36, 30));
                                    buttonUp.setToolTipText(guiContext.cfg.gs("Navigator.button.Up.toolTipText"));
                                    buttonUp.setActionCommand("NavUp");
                                    buttonUp.setFocusable(false);
                                    buttonUp.setDefaultCapable(false);
                                    buttonUp.setHorizontalTextPosition(SwingConstants.CENTER);
                                    panelLocationLeft.add(buttonUp, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 4, 0, 4), 0, 0));
                                }
                                panelLocation.add(panelLocationLeft, BorderLayout.WEST);

                                //---- textFieldLocation ----
                                textFieldLocation.setPreferredSize(new Dimension(810, 30));
                                textFieldLocation.setHorizontalAlignment(SwingConstants.LEFT);
                                textFieldLocation.setToolTipText(guiContext.cfg.gs("Navigator.textField.Location.toolTipText"));
                                textFieldLocation.setName("location");
                                textFieldLocation.setEditable(false);
                                textFieldLocation.setMaximumSize(new Dimension(2147483647, 30));
                                panelLocation.add(textFieldLocation, BorderLayout.CENTER);

                                //---- hSpacer1 ----
                                hSpacer1.setPreferredSize(new Dimension(4, 30));
                                hSpacer1.setMinimumSize(new Dimension(4, 30));
                                hSpacer1.setMaximumSize(new Dimension(4, 30));
                                panelLocation.add(hSpacer1, BorderLayout.EAST);
                            }
                            panelLocationAndButtons.add(panelLocation, BorderLayout.CENTER);

                            //======== panelHintTracking ========
                            {
                                panelHintTracking.setMaximumSize(new Dimension(144, 30));
                                panelHintTracking.setMinimumSize(new Dimension(144, 30));
                                panelHintTracking.setPreferredSize(new Dimension(144, 30));
                                panelHintTracking.setLayout(new BorderLayout());

                                //---- buttonHintTracking ----
                                buttonHintTracking.setText(guiContext.cfg.gs("Navigator.button.HintTracking.text"));
                                buttonHintTracking.setMnemonic(guiContext.cfg.gs("Navigator.button.HintTracking.mnemonic").charAt(0));
                                buttonHintTracking.setToolTipText(guiContext.cfg.gs("Navigator.button.HintTracking.toolTipText"));
                                buttonHintTracking.setFocusable(false);
                                panelHintTracking.add(buttonHintTracking, BorderLayout.CENTER);

                                //---- hSpacer2 ----
                                hSpacer2.setPreferredSize(new Dimension(4, 30));
                                hSpacer2.setMinimumSize(new Dimension(4, 30));
                                hSpacer2.setMaximumSize(new Dimension(4, 30));
                                panelHintTracking.add(hSpacer2, BorderLayout.EAST);
                            }
                            panelLocationAndButtons.add(panelHintTracking, BorderLayout.EAST);

                            //---- vSpacer2 ----
                            vSpacer2.setPreferredSize(new Dimension(10, 2));
                            vSpacer2.setMinimumSize(new Dimension(10, 2));
                            vSpacer2.setMaximumSize(new Dimension(10, 2));
                            panelLocationAndButtons.add(vSpacer2, BorderLayout.SOUTH);
                        }
                        panelBrowserTop.add(panelLocationAndButtons, BorderLayout.PAGE_START);

                        //======== splitPaneTwoBrowsers ========
                        {
                            splitPaneTwoBrowsers.setDividerLocation(506);
                            splitPaneTwoBrowsers.setLastDividerLocation(516);
                            splitPaneTwoBrowsers.setPreferredSize(new Dimension(812, 390));
                            splitPaneTwoBrowsers.setResizeWeight(0.5);
                            splitPaneTwoBrowsers.setContinuousLayout(true);
                            splitPaneTwoBrowsers.setMinimumSize(new Dimension(0, 0));
                            splitPaneTwoBrowsers.setFocusable(false);

                            //======== tabbedPaneBrowserOne ========
                            {
                                tabbedPaneBrowserOne.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserOne.setFocusable(false);
                                tabbedPaneBrowserOne.setMinimumSize(new Dimension(0, 0));

                                //======== panelCollectionOne ========
                                {
                                    panelCollectionOne.setFocusable(false);
                                    panelCollectionOne.setMinimumSize(new Dimension(0, 0));
                                    panelCollectionOne.setLayout(new BoxLayout(panelCollectionOne, BoxLayout.X_AXIS));

                                    //======== splitPaneCollectionOne ========
                                    {
                                        splitPaneCollectionOne.setDividerLocation(150);
                                        splitPaneCollectionOne.setFocusable(false);
                                        splitPaneCollectionOne.setBorder(null);
                                        splitPaneCollectionOne.setResizeWeight(0.5);
                                        splitPaneCollectionOne.setContinuousLayout(true);
                                        splitPaneCollectionOne.setMinimumSize(new Dimension(0, 0));

                                        //======== scrollPaneTreeCollectionOne ========
                                        {
                                            scrollPaneTreeCollectionOne.setFocusable(false);
                                            scrollPaneTreeCollectionOne.setPreferredSize(new Dimension(103, 384));
                                            scrollPaneTreeCollectionOne.setMinimumSize(new Dimension(0, 0));

                                            //---- treeCollectionOne ----
                                            treeCollectionOne.setAutoscrolls(true);
                                            treeCollectionOne.setDragEnabled(true);
                                            treeCollectionOne.setDropMode(DropMode.ON_OR_INSERT);
                                            treeCollectionOne.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTreeCollectionOne.setViewportView(treeCollectionOne);
                                        }
                                        splitPaneCollectionOne.setLeftComponent(scrollPaneTreeCollectionOne);

                                        //======== scrollPaneTableCollectionOne ========
                                        {
                                            scrollPaneTableCollectionOne.setFocusable(false);
                                            scrollPaneTableCollectionOne.setPreferredSize(new Dimension(756, 384));
                                            scrollPaneTableCollectionOne.setMinimumSize(new Dimension(0, 0));

                                            //---- tableCollectionOne ----
                                            tableCollectionOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableCollectionOne.setFillsViewportHeight(true);
                                            tableCollectionOne.setDragEnabled(true);
                                            tableCollectionOne.setDropMode(DropMode.ON_OR_INSERT_ROWS);
                                            tableCollectionOne.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTableCollectionOne.setViewportView(tableCollectionOne);
                                        }
                                        splitPaneCollectionOne.setRightComponent(scrollPaneTableCollectionOne);
                                    }
                                    panelCollectionOne.add(splitPaneCollectionOne);
                                }
                                tabbedPaneBrowserOne.addTab(guiContext.cfg.gs("Navigator.panel.CollectionOne.tab.title"), panelCollectionOne);
                                tabbedPaneBrowserOne.setMnemonicAt(0, guiContext.cfg.gs("Navigator.panel.CollectionOne.tab.mnemonic").charAt(0));

                                //======== panelSystemOne ========
                                {
                                    panelSystemOne.setFocusable(false);
                                    panelSystemOne.setMinimumSize(new Dimension(0, 0));
                                    panelSystemOne.setLayout(new BoxLayout(panelSystemOne, BoxLayout.X_AXIS));

                                    //======== splitPaneSystemOne ========
                                    {
                                        splitPaneSystemOne.setBorder(null);
                                        splitPaneSystemOne.setDividerLocation(150);
                                        splitPaneSystemOne.setResizeWeight(0.5);
                                        splitPaneSystemOne.setContinuousLayout(true);
                                        splitPaneSystemOne.setMinimumSize(new Dimension(0, 0));

                                        //======== scrollPaneTreeSystemOne ========
                                        {
                                            scrollPaneTreeSystemOne.setMinimumSize(new Dimension(0, 0));

                                            //---- treeSystemOne ----
                                            treeSystemOne.setAutoscrolls(true);
                                            treeSystemOne.setDragEnabled(true);
                                            treeSystemOne.setDropMode(DropMode.ON_OR_INSERT);
                                            treeSystemOne.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTreeSystemOne.setViewportView(treeSystemOne);
                                        }
                                        splitPaneSystemOne.setLeftComponent(scrollPaneTreeSystemOne);

                                        //======== scrollPaneTableSystemOne ========
                                        {
                                            scrollPaneTableSystemOne.setMinimumSize(new Dimension(0, 0));

                                            //---- tableSystemOne ----
                                            tableSystemOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableSystemOne.setFillsViewportHeight(true);
                                            tableSystemOne.setDragEnabled(true);
                                            tableSystemOne.setDropMode(DropMode.ON_OR_INSERT_ROWS);
                                            tableSystemOne.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTableSystemOne.setViewportView(tableSystemOne);
                                        }
                                        splitPaneSystemOne.setRightComponent(scrollPaneTableSystemOne);
                                    }
                                    panelSystemOne.add(splitPaneSystemOne);
                                }
                                tabbedPaneBrowserOne.addTab(guiContext.cfg.gs("Navigator.panel.SystemOne.tab.title"), panelSystemOne);
                                tabbedPaneBrowserOne.setMnemonicAt(1, guiContext.cfg.gs("Navigator.panel.SystemOne.tab.mnemonic").charAt(0));
                            }
                            splitPaneTwoBrowsers.setLeftComponent(tabbedPaneBrowserOne);

                            //======== tabbedPaneBrowserTwo ========
                            {
                                tabbedPaneBrowserTwo.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserTwo.setPreferredSize(new Dimension(950, 427));
                                tabbedPaneBrowserTwo.setMinimumSize(new Dimension(0, 0));
                                tabbedPaneBrowserTwo.setAutoscrolls(true);
                                tabbedPaneBrowserTwo.setFocusable(false);
                                tabbedPaneBrowserTwo.setComponentPopupMenu(popupMenuBrowser);

                                //======== panelCollectionTwo ========
                                {
                                    panelCollectionTwo.setMinimumSize(new Dimension(0, 0));
                                    panelCollectionTwo.setFocusable(false);
                                    panelCollectionTwo.setLayout(new BoxLayout(panelCollectionTwo, BoxLayout.X_AXIS));

                                    //======== splitPaneCollectionTwo ========
                                    {
                                        splitPaneCollectionTwo.setBorder(null);
                                        splitPaneCollectionTwo.setDividerLocation(150);
                                        splitPaneCollectionTwo.setResizeWeight(0.5);
                                        splitPaneCollectionTwo.setContinuousLayout(true);
                                        splitPaneCollectionTwo.setMinimumSize(new Dimension(0, 0));
                                        splitPaneCollectionTwo.setFocusable(false);

                                        //======== scrollPaneTreeCollectionTwo ========
                                        {
                                            scrollPaneTreeCollectionTwo.setPreferredSize(new Dimension(103, 384));
                                            scrollPaneTreeCollectionTwo.setMinimumSize(new Dimension(0, 0));

                                            //---- treeCollectionTwo ----
                                            treeCollectionTwo.setDragEnabled(true);
                                            treeCollectionTwo.setDropMode(DropMode.ON_OR_INSERT);
                                            treeCollectionTwo.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTreeCollectionTwo.setViewportView(treeCollectionTwo);
                                        }
                                        splitPaneCollectionTwo.setLeftComponent(scrollPaneTreeCollectionTwo);

                                        //======== scrollPaneTableCollectionTwo ========
                                        {
                                            scrollPaneTableCollectionTwo.setPreferredSize(new Dimension(756, 384));
                                            scrollPaneTableCollectionTwo.setMinimumSize(new Dimension(0, 0));

                                            //---- tableCollectionTwo ----
                                            tableCollectionTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableCollectionTwo.setFillsViewportHeight(true);
                                            tableCollectionTwo.setDragEnabled(true);
                                            tableCollectionTwo.setDropMode(DropMode.ON_OR_INSERT_ROWS);
                                            tableCollectionTwo.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTableCollectionTwo.setViewportView(tableCollectionTwo);
                                        }
                                        splitPaneCollectionTwo.setRightComponent(scrollPaneTableCollectionTwo);
                                    }
                                    panelCollectionTwo.add(splitPaneCollectionTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(guiContext.cfg.gs("Navigator.panel.CollectionTwo.tab.title"), panelCollectionTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(0, guiContext.cfg.gs("Navigator.panel.CollectionTwo.tab.mnemonic").charAt(0));

                                //======== panelSystemTwo ========
                                {
                                    panelSystemTwo.setMinimumSize(new Dimension(0, 0));
                                    panelSystemTwo.setLayout(new BoxLayout(panelSystemTwo, BoxLayout.X_AXIS));

                                    //======== splitPaneSystemTwo ========
                                    {
                                        splitPaneSystemTwo.setBorder(null);
                                        splitPaneSystemTwo.setDividerLocation(150);
                                        splitPaneSystemTwo.setResizeWeight(0.5);
                                        splitPaneSystemTwo.setContinuousLayout(true);
                                        splitPaneSystemTwo.setMinimumSize(new Dimension(0, 0));

                                        //======== scrollPaneTreeSystemTwo ========
                                        {
                                            scrollPaneTreeSystemTwo.setMinimumSize(new Dimension(0, 0));

                                            //---- treeSystemTwo ----
                                            treeSystemTwo.setAutoscrolls(true);
                                            treeSystemTwo.setDragEnabled(true);
                                            treeSystemTwo.setDropMode(DropMode.ON_OR_INSERT);
                                            treeSystemTwo.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTreeSystemTwo.setViewportView(treeSystemTwo);
                                        }
                                        splitPaneSystemTwo.setLeftComponent(scrollPaneTreeSystemTwo);

                                        //======== scrollPaneTableSystemTwo ========
                                        {
                                            scrollPaneTableSystemTwo.setMinimumSize(new Dimension(0, 0));

                                            //---- tableSystemTwo ----
                                            tableSystemTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableSystemTwo.setFillsViewportHeight(true);
                                            tableSystemTwo.setDragEnabled(true);
                                            tableSystemTwo.setDropMode(DropMode.ON_OR_INSERT_ROWS);
                                            tableSystemTwo.setComponentPopupMenu(popupMenuBrowser);
                                            scrollPaneTableSystemTwo.setViewportView(tableSystemTwo);
                                        }
                                        splitPaneSystemTwo.setRightComponent(scrollPaneTableSystemTwo);
                                    }
                                    panelSystemTwo.add(splitPaneSystemTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(guiContext.cfg.gs("Navigator.panel.SystemTwo.tab.title"), panelSystemTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(1, guiContext.cfg.gs("Navigator.panel.SystemTwo.tab.mnemonic").charAt(0));
                            }
                            splitPaneTwoBrowsers.setRightComponent(tabbedPaneBrowserTwo);
                        }
                        panelBrowserTop.add(splitPaneTwoBrowsers, BorderLayout.CENTER);
                    }
                    splitPaneBrowser.setTopComponent(panelBrowserTop);

                    //======== tabbedPaneNavigatorBottom ========
                    {
                        tabbedPaneNavigatorBottom.setTabPlacement(SwingConstants.BOTTOM);
                        tabbedPaneNavigatorBottom.setPreferredSize(new Dimension(1160, 90));
                        tabbedPaneNavigatorBottom.setFocusable(false);
                        tabbedPaneNavigatorBottom.setMinimumSize(new Dimension(0, 0));
                        tabbedPaneNavigatorBottom.setAutoscrolls(true);

                        //======== scrollPaneLog ========
                        {
                            scrollPaneLog.setFocusable(false);
                            scrollPaneLog.setMinimumSize(new Dimension(0, 0));
                            scrollPaneLog.setAutoscrolls(true);

                            //---- textAreaLog ----
                            textAreaLog.setEditable(false);
                            textAreaLog.setTabSize(4);
                            textAreaLog.setLineWrap(true);
                            textAreaLog.setMinimumSize(new Dimension(0, 0));
                            textAreaLog.setComponentPopupMenu(popupMenuLog);
                            textAreaLog.setVerifyInputWhenFocusTarget(false);
                            scrollPaneLog.setViewportView(textAreaLog);
                        }
                        tabbedPaneNavigatorBottom.addTab(guiContext.cfg.gs("Navigator.scrollPane.Log.tab.title"), scrollPaneLog);
                        tabbedPaneNavigatorBottom.setMnemonicAt(0, guiContext.cfg.gs("Navigator.scrollPane.Log.tab.mnemonic").charAt(0));

                        //======== scrollPaneProperties ========
                        {
                            scrollPaneProperties.setFocusable(false);
                            scrollPaneProperties.setMinimumSize(new Dimension(0, 0));

                            //---- textAreaProperties ----
                            textAreaProperties.setEditable(false);
                            textAreaProperties.setMinimumSize(new Dimension(0, 0));
                            textAreaProperties.setContentType("text/html");
                            scrollPaneProperties.setViewportView(textAreaProperties);
                        }
                        tabbedPaneNavigatorBottom.addTab(guiContext.cfg.gs("Navigator.scrollPane.Properties.tab.title"), scrollPaneProperties);
                        tabbedPaneNavigatorBottom.setMnemonicAt(1, guiContext.cfg.gs("Navigator.scrollPane.Properties.tab.mnemonic").charAt(0));
                    }
                    splitPaneBrowser.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab(guiContext.cfg.gs("Navigator.splitPane.Browser.tab.title"), splitPaneBrowser);
                tabbedPaneMain.setMnemonicAt(0, guiContext.cfg.gs("Navigator.splitPane.Browser.tab.mnemonic").charAt(0));

                //======== splitPaneBackup ========
                {
                    splitPaneBackup.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBackup.setDividerLocation(450);
                    splitPaneBackup.setLastDividerLocation(450);
                }
                tabbedPaneMain.addTab(guiContext.cfg.gs("Navigator.splitPane.Backup.tab.title"), splitPaneBackup);
                tabbedPaneMain.setMnemonicAt(1, guiContext.cfg.gs("Navigator.splitPane.Backup.tab.mnemonic").charAt(0));

                //======== panelLibraries ========
                {
                    panelLibraries.setLayout(new BorderLayout());
                }
                tabbedPaneMain.addTab(guiContext.cfg.gs("Navigator.splitPane.Libraries.tab.title"), panelLibraries);
                tabbedPaneMain.setMnemonicAt(2, guiContext.cfg.gs("Navigator.splitPane.Libraries.tab.mnemonic").charAt(0));
            }
            panelMain.add(tabbedPaneMain);
        }
        contentPane.add(panelMain, BorderLayout.CENTER);

        //======== panelStatus ========
        {
            panelStatus.setLayout(new GridBagLayout());

            //---- labelStatusLeft ----
            labelStatusLeft.setText(guiContext.cfg.gs("Navigator.label.StatusLeft.text"));
            labelStatusLeft.setHorizontalAlignment(SwingConstants.LEFT);
            panelStatus.add(labelStatusLeft, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                new Insets(0, 4, 0, 4), 0, 0));

            //---- labelStatusMiddle ----
            labelStatusMiddle.setHorizontalAlignment(SwingConstants.CENTER);
            labelStatusMiddle.setText("Status Middle");
            panelStatus.add(labelStatusMiddle, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 4), 0, 0));

            //---- labelStatusRight ----
            labelStatusRight.setText(guiContext.cfg.gs("Navigator.label.StatusRight.text"));
            labelStatusRight.setHorizontalAlignment(SwingConstants.RIGHT);
            panelStatus.add(labelStatusRight, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 8), 0, 0));
        }
        contentPane.add(panelStatus, BorderLayout.SOUTH);
        setSize(1024, 640);
        setLocationRelativeTo(getOwner());

        //======== popupMenuBrowser ========
        {

            //---- popupMenuItemFind ----
            popupMenuItemFind.setText(guiContext.cfg.gs("Navigator.popupMenu.Find.text"));
            popupMenuItemFind.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemFind.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Find.mnemonic").charAt(0));
            popupMenuItemFind.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuItemFind.setEnabled(false);
            popupMenuBrowser.add(popupMenuItemFind);

            //---- popupMenuItemNewFolder ----
            popupMenuItemNewFolder.setText(guiContext.cfg.gs("Navigator.popupMenu.NewFolder.text"));
            popupMenuItemNewFolder.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemNewFolder.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.NewFolder.mnemonic").charAt(0));
            popupMenuItemNewFolder.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemNewFolder);

            //---- popupMenuItemRename ----
            popupMenuItemRename.setText(guiContext.cfg.gs("Navigator.popupMenu.Rename.text"));
            popupMenuItemRename.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemRename.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Rename.mnemonic").charAt(0));
            popupMenuItemRename.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemRename);

            //---- popupMenuItemTouch ----
            popupMenuItemTouch.setText(guiContext.cfg.gs("Navigator.popupMenu.Touch.text"));
            popupMenuItemTouch.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemTouch.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Touch.mnemonic").charAt(0));
            popupMenuItemTouch.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemTouch);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemCopy ----
            popupMenuItemCopy.setText(guiContext.cfg.gs("Navigator.popupMenu.Copy.text"));
            popupMenuItemCopy.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemCopy.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Copy.mnemonic").charAt(0));
            popupMenuItemCopy.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemCopy);

            //---- popupMenuItemCut ----
            popupMenuItemCut.setText(guiContext.cfg.gs("Navigator.popupMenu.Cut.text"));
            popupMenuItemCut.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemCut.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Cut.mnemonic").charAt(0));
            popupMenuItemCut.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemCut);

            //---- popupMenuItemPaste ----
            popupMenuItemPaste.setText(guiContext.cfg.gs("Navigator.popupMenu.Paste.text"));
            popupMenuItemPaste.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemPaste.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Paste.mnemonic").charAt(0));
            popupMenuItemPaste.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemPaste);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemDelete ----
            popupMenuItemDelete.setText(guiContext.cfg.gs("Navigator.popupMenu.Delete.text"));
            popupMenuItemDelete.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemDelete.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Delete.mnemonic").charAt(0));
            popupMenuItemDelete.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuBrowser.add(popupMenuItemDelete);
        }

        //======== popupMenuLog ========
        {

            //---- popupMenuItemBottom ----
            popupMenuItemBottom.setText(guiContext.cfg.gs("Navigator.popupMenu.Bottom.text"));
            popupMenuItemBottom.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemBottom.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Bottom.mnemonic").charAt(0));
            popupMenuItemBottom.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuLog.add(popupMenuItemBottom);

            //---- popupMenuItemClear ----
            popupMenuItemClear.setText(guiContext.cfg.gs("Navigator.popupMenu.Clear.text"));
            popupMenuItemClear.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemClear.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Clear.mnemonic").charAt(0));
            popupMenuItemClear.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuLog.add(popupMenuItemClear);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JMenuBar menuBarMain;
    public JMenu menuFile;
    public JMenuItem menuItemOpenPublisher;
    public JMenuItem menuItemOpenSubscriber;
    public JMenuItem menuItemOpenHintKeys;
    public JMenuItem menuItemOpenHintServer;
    public JMenuItem menuItemSaveLayout;
    public JMenuItem menuItemQuitTerminate;
    public JMenuItem menuItemFileQuit;
    public JMenu menuEdit;
    public JMenuItem menuItemFind;
    public JMenuItem menuItemNewFolder;
    public JMenuItem menuItemRename;
    public JMenuItem menuItemTouch;
    public JMenuItem menuItemCopy;
    public JMenuItem menuItemCut;
    public JMenuItem menuItemPaste;
    public JMenuItem menuItemDelete;
    public JMenuItem menuItemSettings;
    public JMenu menuView;
    public JMenuItem menuItemRefresh;
    public JMenuItem menuItemProgress;
    public JCheckBoxMenuItem menuItemShowHidden;
    public JMenu menuBookmarks;
    public JMenuItem menuItemAddBookmark;
    public JMenuItem menuItemBookmarksDelete;
    public JMenu menuTools;
    public JMenuItem menuItemDuplicates;
    public JMenuItem menuItemEmptyFinder;
    public JMenuItem menuItemJunk;
    public JMenuItem menuItemRenamer;
    public JMenuItem menuItemExternalTools;
    public JMenuItem menuItem1;
    public JMenuItem menuItemPlexGenerator;
    public JMenu menuJobs;
    public JMenuItem menuItemJobsManage;
    public JMenu menuWindows;
    public JMenuItem menuItemMaximize;
    public JMenuItem menuItemMinimize;
    public JMenuItem menuItemRestore;
    public JMenuItem menuItemSplitHorizontal;
    public JMenuItem menuItemSplitVertical;
    public JMenu menuHelp;
    public JMenuItem menuItemControls;
    public JMenuItem menuItemDocumentation;
    public JMenuItem menuItemGitHubProject;
    public JMenuItem menuItemUpdates;
    public JMenuItem menuItemAbout;
    public JPanel panelMain;
    public JTabbedPane tabbedPaneMain;
    public JSplitPane splitPaneBrowser;
    public JPanel panelBrowserTop;
    public JPanel panelLocationAndButtons;
    public JPanel vSpacer1;
    public JPanel panelLocation;
    public JPanel panelLocationLeft;
    public JButton buttonBack;
    public JButton buttonForward;
    public JButton buttonUp;
    public JTextField textFieldLocation;
    public JPanel hSpacer1;
    public JPanel panelHintTracking;
    public JButton buttonHintTracking;
    public JPanel hSpacer2;
    public JPanel vSpacer2;
    public JSplitPane splitPaneTwoBrowsers;
    public JTabbedPane tabbedPaneBrowserOne;
    public JPanel panelCollectionOne;
    public JSplitPane splitPaneCollectionOne;
    public JScrollPane scrollPaneTreeCollectionOne;
    public JTree treeCollectionOne;
    public JScrollPane scrollPaneTableCollectionOne;
    public JTable tableCollectionOne;
    public JPanel panelSystemOne;
    public JSplitPane splitPaneSystemOne;
    public JScrollPane scrollPaneTreeSystemOne;
    public JTree treeSystemOne;
    public JScrollPane scrollPaneTableSystemOne;
    public JTable tableSystemOne;
    public JTabbedPane tabbedPaneBrowserTwo;
    public JPanel panelCollectionTwo;
    public JSplitPane splitPaneCollectionTwo;
    public JScrollPane scrollPaneTreeCollectionTwo;
    public JTree treeCollectionTwo;
    public JScrollPane scrollPaneTableCollectionTwo;
    public JTable tableCollectionTwo;
    public JPanel panelSystemTwo;
    public JSplitPane splitPaneSystemTwo;
    public JScrollPane scrollPaneTreeSystemTwo;
    public JTree treeSystemTwo;
    public JScrollPane scrollPaneTableSystemTwo;
    public JTable tableSystemTwo;
    public JTabbedPane tabbedPaneNavigatorBottom;
    public JScrollPane scrollPaneLog;
    public JTextArea textAreaLog;
    public JScrollPane scrollPaneProperties;
    public JEditorPane textAreaProperties;
    public JSplitPane splitPaneBackup;
    public JPanel panelLibraries;
    public JPanel panelStatus;
    public JLabel labelStatusLeft;
    public JLabel labelStatusMiddle;
    public JLabel labelStatusRight;
    public JPopupMenu popupMenuBrowser;
    public JMenuItem popupMenuItemFind;
    public JMenuItem popupMenuItemNewFolder;
    public JMenuItem popupMenuItemRename;
    public JMenuItem popupMenuItemTouch;
    public JMenuItem popupMenuItemCopy;
    public JMenuItem popupMenuItemCut;
    public JMenuItem popupMenuItemPaste;
    public JMenuItem popupMenuItemDelete;
    public JPopupMenu popupMenuLog;
    public JMenuItem popupMenuItemBottom;
    public JMenuItem popupMenuItemClear;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
