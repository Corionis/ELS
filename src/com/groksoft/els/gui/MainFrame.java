package com.groksoft.els.gui;

import javax.swing.border.*;
import javax.swing.table.*;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.browser.BrowserTableModel;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.gui.util.RotatedIcon;
import com.groksoft.els.gui.util.SmartScroller;
import com.groksoft.els.gui.util.TextIcon;
import com.groksoft.els.gui.util.VerticalLabel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLabelUI;
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
 *  - https://github.com/JFormDesigner/FlatLaf <br/>
 *  - Download from: https://search.maven.org/artifact/com.formdev/flatlaf <br/>
 * <br/>
 * See also: <br/>
 *  - https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-extras <br/>
 *  - https://github.com/JFormDesigner/svgSalamander <br/>
 */
public class MainFrame extends JFrame
{
    private transient Logger logger = LogManager.getLogger("applog");
    private Context context;
    private LookAndFeel laf;

    public MainFrame(Context context)
    {
        context.mainFrame = this;
        this.context = context;

        try
        {
            context.preferences.initLookAndFeel();
            initComponents();
            setTitle(context.cfg.getNavigatorName());
            setBrowserTabs(-1);

            // re-create the right-side tables, for getToolTipText(), and re-setup
            tableCollectionOne = new JTable ()
            {
                @Override
                public String getToolTipText(MouseEvent e)
                {
                    String tip = null;
                    java.awt.Point p = e.getPoint();
                    int row = rowAtPoint(p);
                    int col = columnAtPoint(p);
                    try
                    {
                        if (row != 0)
                        {
                            NavTreeUserObject tuo = (NavTreeUserObject) getValueAt(row, 1);
                            if (tuo.path.toLowerCase().endsWith(".els"))
                            {
                                tip = context.cfg.gs("Mainframe.select.to.see.details.in.properties.tab");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        // nop
                    }
                    return tip;
                }
            };
            tableCollectionOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
            tableCollectionOne.setFillsViewportHeight(true);
            tableCollectionOne.setDragEnabled(true);
            tableCollectionOne.setDropMode(DropMode.ON_OR_INSERT_ROWS);
            tableCollectionOne.setComponentPopupMenu(popupMenuBrowser);
            tableCollectionOne.setShowHorizontalLines(false);
            tableCollectionOne.setShowVerticalLines(false);
            tableCollectionOne.setName("tableCollectionOne");
            tableCollectionOne.setAutoCreateRowSorter(true);
            tableCollectionOne.setShowGrid(false);
            tableCollectionOne.getTableHeader().setReorderingAllowed(false);
            tableCollectionOne.setRowSelectionAllowed(true);
            tableCollectionOne.setColumnSelectionAllowed(false);
            tableCollectionOne.setModel(new BrowserTableModel(context));
            adjustTableColumns(tableCollectionOne);
            scrollPaneTableCollectionOne.setViewportView(tableCollectionOne);

            tableSystemOne = new JTable ()
            {
                @Override
                public String getToolTipText(MouseEvent e)
                {
                    String tip = null;
                    java.awt.Point p = e.getPoint();
                    int row = rowAtPoint(p);
                    int col = columnAtPoint(p);
                    try
                    {
                        if (row != 0)
                        {
                            NavTreeUserObject tuo = (NavTreeUserObject) getValueAt(row, 1);
                            if (tuo.path.toLowerCase().endsWith(".els"))
                            {
                                tip = context.cfg.gs("Mainframe.select.to.see.details.in.properties.tab");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        // nop
                    }
                    return tip;
                }
            };
            tableSystemOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
            tableSystemOne.setFillsViewportHeight(true);
            tableSystemOne.setDragEnabled(true);
            tableSystemOne.setDropMode(DropMode.ON_OR_INSERT_ROWS);
            tableSystemOne.setComponentPopupMenu(popupMenuBrowser);
            tableSystemOne.setShowHorizontalLines(false);
            tableSystemOne.setShowVerticalLines(false);
            tableSystemOne.setName("tableSystemOne");
            tableSystemOne.setAutoCreateRowSorter(true);
            tableSystemOne.setShowGrid(false);
            tableSystemOne.getTableHeader().setReorderingAllowed(false);
            tableSystemOne.setRowSelectionAllowed(true);
            tableSystemOne.setColumnSelectionAllowed(false);
            tableSystemOne.setModel(new BrowserTableModel(context));
            adjustTableColumns(tableSystemOne);
            scrollPaneTableSystemOne.setViewportView(tableSystemOne);

            tableCollectionTwo = new JTable ()
            {
                @Override
                public String getToolTipText(MouseEvent e)
                {
                    String tip = null;
                    java.awt.Point p = e.getPoint();
                    int row = rowAtPoint(p);
                    int col = columnAtPoint(p);
                    try
                    {
                        if (row != 0)
                        {
                            NavTreeUserObject tuo = (NavTreeUserObject) getValueAt(row, 1);
                            if (tuo.path.toLowerCase().endsWith(".els"))
                            {
                                tip = context.cfg.gs("Mainframe.select.to.see.details.in.properties.tab");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        // nop
                    }
                    return tip;
                }
            };
            tableCollectionTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
            tableCollectionTwo.setFillsViewportHeight(true);
            tableCollectionTwo.setDragEnabled(true);
            tableCollectionTwo.setDropMode(DropMode.ON_OR_INSERT_ROWS);
            tableCollectionTwo.setComponentPopupMenu(popupMenuBrowser);
            tableCollectionTwo.setShowHorizontalLines(false);
            tableCollectionTwo.setShowVerticalLines(false);
            tableCollectionTwo.setName("tableCollectionTwo");
            tableCollectionTwo.setAutoCreateRowSorter(true);
            tableCollectionTwo.setShowGrid(false);
            tableCollectionTwo.getTableHeader().setReorderingAllowed(false);
            tableCollectionTwo.setRowSelectionAllowed(true);
            tableCollectionTwo.setColumnSelectionAllowed(false);
            tableCollectionTwo.setModel(new BrowserTableModel(context));
            adjustTableColumns(tableCollectionTwo);
            scrollPaneTableCollectionTwo.setViewportView(tableCollectionTwo);

            tableSystemTwo = new JTable ()
            {
                @Override
                public String getToolTipText(MouseEvent e)
                {
                    String tip = null;
                    java.awt.Point p = e.getPoint();
                    int row = rowAtPoint(p);
                    int col = columnAtPoint(p);
                    try
                    {
                        if (row != 0)
                        {
                            NavTreeUserObject tuo = (NavTreeUserObject) getValueAt(row, 1);
                            if (tuo.path.toLowerCase().endsWith(".els"))
                            {
                                tip = context.cfg.gs("Mainframe.select.to.see.details.in.properties.tab");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        // nop
                    }
                    return tip;
                }
            };
            tableSystemTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
            tableSystemTwo.setFillsViewportHeight(true);
            tableSystemTwo.setDragEnabled(true);
            tableSystemTwo.setDropMode(DropMode.ON_OR_INSERT_ROWS);
            tableSystemTwo.setComponentPopupMenu(popupMenuBrowser);
            tableSystemTwo.setShowHorizontalLines(false);
            tableSystemTwo.setShowVerticalLines(false);
            tableSystemTwo.setName("tableSystemTwo");
            tableSystemTwo.setAutoCreateRowSorter(true);
            tableSystemTwo.setShowGrid(false);
            tableSystemTwo.getTableHeader().setReorderingAllowed(false);
            tableSystemTwo.setRowSelectionAllowed(true);
            tableSystemTwo.setColumnSelectionAllowed(false);
            tableSystemTwo.setModel(new BrowserTableModel(context));
            adjustTableColumns(tableSystemTwo);
            scrollPaneTableSystemTwo.setViewportView(tableSystemTwo);

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

            // set the fixed-space font for the logs
            if (Utils.isOsLinux())
            {
                textAreaLog.setFont(new Font("Courier 10 Pitch", Font.PLAIN, 13));
            }
            else
            {
                textAreaLog.setFont(new Font("Courier New", Font.PLAIN, 13));
            }
            textAreaLog.setDisabledTextColor(tabbedPaneMain.getForeground());

            // add smart scroll to the logs
            // https://tips4java.wordpress.com/2013/03/03/smart-scrolling/
            new SmartScroller(scrollPaneLog);

            // Change default JOptionPanel button names based on the locale
            // TODO add tool tip text & mnemonic
            UIManager.put("OptionPane.cancelButtonText", context.cfg.gs("Z.cancel"));
            // TODO add all the FileChooser buttons
            UIManager.put("FileChooser.openButtonText", context.cfg.gs("Z.open"));
            UIManager.put("FileChooser.cancelButtonText", context.cfg.gs("Z.cancel"));
            UIManager.put("OptionPane.noButtonText", context.cfg.gs("Z.no"));
            UIManager.put("OptionPane.okButtonText", context.cfg.gs("Z.ok"));
            UIManager.put("OptionPane.yesButtonText", context.cfg.gs("Z.yes"));

        }
        catch(Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            context.fault = true;
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

    private boolean changesCheckAll()
    {
        boolean changes = false;
        if (context.navigator.dialogJunkRemover != null && context.navigator.dialogJunkRemover.checkForChanges())
            changes = true;
        else if (context.navigator.dialogRenamer != null && context.navigator.dialogRenamer.checkForChanges())
            changes = true;
        else if (context.navigator.dialogJobs != null && context.navigator.dialogJobs.checkForChanges())
            changes = true;
        return changes;
    }

    private void changesGotoUnsaved()
    {
        boolean changes = false;
        if (context.navigator.dialogJunkRemover != null && context.navigator.dialogJunkRemover.checkForChanges())
        {
            context.navigator.dialogJunkRemover.setVisible(true);
            context.navigator.dialogJunkRemover.toFront();
            context.navigator.dialogJunkRemover.requestFocus();
            context.navigator.dialogJunkRemover.toFront();
            context.navigator.dialogJunkRemover.requestFocus();
            context.navigator.dialogJunkRemover.saveButton.requestFocus();
        }
        else if (context.navigator.dialogRenamer != null && context.navigator.dialogRenamer.checkForChanges())
        {
            context.navigator.dialogRenamer.toFront();
            context.navigator.dialogRenamer.requestFocus();
            context.navigator.dialogRenamer.toFront();
            context.navigator.dialogRenamer.requestFocus();
            context.navigator.dialogRenamer.saveButton.requestFocus();
        }
        else if (context.navigator.dialogJobs != null && context.navigator.dialogJobs.checkForChanges())
        {
            context.navigator.dialogJobs.toFront();
            context.navigator.dialogJobs.requestFocus();
            context.navigator.dialogJobs.toFront();
            context.navigator.dialogJobs.requestFocus();
            context.navigator.dialogJobs.saveButton.requestFocus();
        }
    }

    private void menuItemFileQuitActionPerformed(ActionEvent e)
    {
        if (verifyClose())
            context.navigator.stop();
    }

    public void setBrowserTabs(int tabPlacementIndex)
    {
        int tabPlacement;
        if (tabPlacementIndex < 0)
        {
            tabPlacement = context.preferences.getTabPlacement();
            tabPlacementIndex = context.preferences.getTabPlacementIndex();
        }
        else
            tabPlacement = context.preferences.getTabPlacement(tabPlacementIndex);

        tabbedPaneBrowserOne.setTabPlacement(tabPlacement);
        tabbedPaneBrowserTwo.setTabPlacement(tabPlacement);

        if (tabPlacementIndex > 1) // left or right, rotate
        {
            // change browser tabs orientation to vertical
            JLabel label = new JLabel(context.cfg.gs("Navigator.panel.CollectionOne.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserOne.setTabComponentAt(0, label);
            //
            label = new JLabel(context.cfg.gs("Navigator.panel.SystemOne.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserOne.setTabComponentAt(1, label);

            label = new JLabel(context.cfg.gs("Navigator.panel.CollectionTwo.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserTwo.setTabComponentAt(0, label);
            //
            label = new JLabel(context.cfg.gs("Navigator.panel.SystemTwo.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserTwo.setTabComponentAt(1, label);
        }
        else // top or bottom
        {
            // change browser tabs orientation to vertical
            JLabel label = new JLabel(context.cfg.gs("Navigator.panel.CollectionOne.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserOne.setTabComponentAt(0, label);
            //
            label = new JLabel(context.cfg.gs("Navigator.panel.SystemOne.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserOne.setTabComponentAt(1, label);

            label = new JLabel(context.cfg.gs("Navigator.panel.CollectionTwo.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserTwo.setTabComponentAt(0, label);
            //
            label = new JLabel(context.cfg.gs("Navigator.panel.SystemTwo.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserTwo.setTabComponentAt(1, label);
        }
    }

    private void thisWindowClosing(WindowEvent e)
    {
        if (verifyClose())
            context.navigator.stop();
    }

    public boolean verifyClose()
    {
        if (changesCheckAll())
        {
            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                    context.cfg.gs("MainFrame.unsaved.changes.are.you.sure"),
                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
            {
                changesGotoUnsaved();
                return false;
            }
        }

        if (context.progress != null && context.progress.isBeingUsed())
        {
            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                    context.cfg.gs("MainFrame.transfers.are.active.are.you.sure"),
                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                return false;
        }
        if (context.browser.navTransferHandler.getTransferWorker() != null &&
                !context.browser.navTransferHandler.getTransferWorker().isDone())
        {
            logger.warn(context.cfg.gs("MainFrame.cancelling.transfers.as.requested"));
            context.browser.navTransferHandler.getTransferWorker().cancel(true);
            context.fault = true;
        }
        return true;
    }

    private void actionNewClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionCopyClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionDeleteClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionRunClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionHelpClicked(MouseEvent e) {
        // TODO add your code here
    }

    private void actionSaveClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionCancelClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void configItemsMouseClicked(MouseEvent e) {
        // TODO add your code here
    }

    private void actionRecursiveClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionFilesOnlyClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionFilenameSegmentClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void cardShown(ComponentEvent e) {
        // TODO add your code here
    }

    private void actionCaseChangeClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void genericAction(ActionEvent e) {
        // TODO add your code here
    }

    private void tabKeyPressed(KeyEvent e) {
        // TODO add your code here
    }

    private void genericTextFieldFocusLost(FocusEvent e) {
        // TODO add your code here
    }

    private void actionRefreshClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionOriginAddClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionOriginUpClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionOriginDownClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void actionOriginRemoveClicked(ActionEvent e) {
        // TODO add your code here
    }

    // ================================================================================================================

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
        menuItemOpenHintTracking = new JMenuItem();
        menuItemSaveLayout = new JMenuItem();
        menuItemQuitTerminate = new JMenuItem();
        menuItemFileQuit = new JMenuItem();
        menuEdit = new JMenu();
        menuItemCopy = new JMenuItem();
        menuItemCut = new JMenuItem();
        menuItemPaste = new JMenuItem();
        menuItemDelete = new JMenuItem();
        menuItemNewFolder = new JMenuItem();
        menuItemRename = new JMenuItem();
        menuItemTouch = new JMenuItem();
        menuItemFind = new JMenuItem();
        menuItemFindNext = new JMenuItem();
        menuView = new JMenu();
        menuItemProgress = new JMenuItem();
        menuItemRefresh = new JMenuItem();
        menuItemAutoRefresh = new JCheckBoxMenuItem();
        menuItemShowHidden = new JCheckBoxMenuItem();
        menuItemWordWrap = new JCheckBoxMenuItem();
        menuBookmarks = new JMenu();
        menuItemAddBookmark = new JMenuItem();
        menuItemBookmarksDelete = new JMenuItem();
        menuTools = new JMenu();
        menuItemDuplicates = new JMenuItem();
        menuItemEmptyFinder = new JMenuItem();
        menuItemJunk = new JMenuItem();
        menuItemOperations = new JMenuItem();
        menuItemRenamer = new JMenuItem();
        menuItemSleep = new JMenuItem();
        menuItemExternalTools = new JMenuItem();
        menuItemPlexGenerator = new JMenuItem();
        menuItem1 = new JMenuItem();
        menuJobs = new JMenu();
        menuItemJobsManage = new JMenuItem();
        menuSystem = new JMenu();
        menuItemSettings = new JMenuItem();
        menuItemAuthKeys = new JMenuItem();
        menuItemHintKeys = new JMenuItem();
        menuItemBlacklist = new JMenuItem();
        menuItemWhitelist = new JMenuItem();
        menuWindows = new JMenu();
        menuItemMaximize = new JMenuItem();
        menuItemMinimize = new JMenuItem();
        menuItemRestore = new JMenuItem();
        menuItemSplitHorizontal = new JMenuItem();
        menuItemSplitVertical = new JMenuItem();
        menuHelp = new JMenu();
        menuItemControls = new JMenuItem();
        menuItemDocumentation = new JMenuItem();
        menuItemGettingStarted = new JMenuItem();
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
        panelLibraries = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        configItems = new JTable();
        panelOptions = new JPanel();
        panelControls = new JPanel();
        topOptions = new JPanel();
        vSpacer0 = new JPanel(null);
        panelLibraryType = new JPanel();
        hSpacer3 = new JPanel(null);
        labelLibaryType = new JLabel();
        panelCardBox = new JPanel();
        vSpacer3 = new JPanel(null);
        separator13 = new JSeparator();
        vSpacer4 = new JPanel(null);
        panelLibraryTypeCards = new JPanel();
        panelGettingStartedCard = new JPanel();
        labelOperationGettingStarted = new JLabel();
        panelLibraryCard = new JPanel();
        panelHintServerCard = new JPanel();
        panelTargetsCard = new JPanel();
        panelXCard = new JPanel();
        panelYCard = new JPanel();
        tabbedPaneLibrarySpaces = new JTabbedPane();
        bibliographyTab = new JPanel();
        librariesSplit = new JSplitPane();
        scrollPaneLibraries = new JScrollPane();
        tableLibraries = new JTable();
        panelSources = new JPanel();
        labelSpacer42 = new JLabel();
        labelSources = new JLabel();
        scrollPane1 = new JScrollPane();
        table1 = new JTable();
        panel1 = new JPanel();
        buttonAddOrigin = new JButton();
        buttonOriginUp = new JButton();
        buttonOriginDown = new JButton();
        buttonRemoveOrigin = new JButton();
        panelBiblioButtons = new JPanel();
        buttonNewLibrary = new JButton();
        locationsTab = new JPanel();
        scrollPaneLocations = new JScrollPane();
        tableLocations = new JTable();
        panelLocButtons = new JPanel();
        buttonNewLocation = new JButton();
        buttonBar = new JPanel();
        saveButton = new JButton();
        cancelButton = new JButton();
        panelStatus = new JPanel();
        labelStatusLeft = new JLabel();
        labelStatusMiddle = new JLabel();
        labelStatusRight = new JLabel();
        popupMenuBrowser = new JPopupMenu();
        popupMenuItemRefresh = new JMenuItem();
        popupMenuItemCopy = new JMenuItem();
        popupMenuItemCut = new JMenuItem();
        popupMenuItemPaste = new JMenuItem();
        popupMenuItemDelete = new JMenuItem();
        popupMenuItemNewFolder = new JMenuItem();
        popupMenuItemRename = new JMenuItem();
        popupMenuItemTouch = new JMenuItem();
        popupMenuLog = new JPopupMenu();
        popupMenuItemFindNext = new JMenuItem();
        popupMenuItemFind = new JMenuItem();
        popupMenuItemTop = new JMenuItem();
        popupMenuItemBottom = new JMenuItem();
        popupMenuItemClear = new JMenuItem();
        popupCheckBoxMenuItemWordWrap = new JCheckBoxMenuItem();

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
                menuFile.setText(context.cfg.gs("Navigator.menu.File.text"));
                menuFile.setMnemonic(context.cfg.gs("Navigator.menu.File.mnemonic").charAt(0));

                //---- menuItemOpenPublisher ----
                menuItemOpenPublisher.setText(context.cfg.gs("Navigator.menu.OpenPublisher.text"));
                menuItemOpenPublisher.setMnemonic(context.cfg.gs("Navigator.menu.OpenPublisher.mnemonic").charAt(0));
                menuItemOpenPublisher.setDisplayedMnemonicIndex(5);
                menuFile.add(menuItemOpenPublisher);

                //---- menuItemOpenSubscriber ----
                menuItemOpenSubscriber.setText(context.cfg.gs("Navigator.menu.OpenSubscriber.text"));
                menuItemOpenSubscriber.setMnemonic(context.cfg.gs("Navigator.menu.OpenSubscriber.mnemonic").charAt(0));
                menuFile.add(menuItemOpenSubscriber);

                //---- menuItemOpenHintKeys ----
                menuItemOpenHintKeys.setText(context.cfg.gs("Navigator.menu.OpenHintKeys.text"));
                menuItemOpenHintKeys.setSelected(true);
                menuItemOpenHintKeys.setMnemonic(context.cfg.gs("Navigator.menu.OpenHintKeys.mnemonic").charAt(0));
                menuFile.add(menuItemOpenHintKeys);

                //---- menuItemOpenHintTracking ----
                menuItemOpenHintTracking.setText(context.cfg.gs("Navigator.menuItemOpenHintTracking.text"));
                menuItemOpenHintTracking.setSelected(true);
                menuItemOpenHintTracking.setMnemonic(context.cfg.gs("Navigator.menuItemOpenHintTracking.mnemonic_2").charAt(0));
                menuItemOpenHintTracking.setDisplayedMnemonicIndex(Integer.parseInt(context.cfg.gs("Navigator.menuItemOpenHintTracking.displayedMnemonicIndex")));
                menuFile.add(menuItemOpenHintTracking);
                menuFile.addSeparator();

                //---- menuItemSaveLayout ----
                menuItemSaveLayout.setText(context.cfg.gs("Navigator.menu.SaveLayout.text"));
                menuItemSaveLayout.setMnemonic(context.cfg.gs("Navigator.menu.SaveLayout.mnemonic_3").charAt(0));
                menuFile.add(menuItemSaveLayout);
                menuFile.addSeparator();

                //---- menuItemQuitTerminate ----
                menuItemQuitTerminate.setText(context.cfg.gs("Navigator.menu.QuitTerminate.text"));
                menuItemQuitTerminate.setMnemonic(context.cfg.gs("Navigator.menuItemQuitTerminate.mnemonic").charAt(0));
                menuItemQuitTerminate.setDisplayedMnemonicIndex(12);
                menuFile.add(menuItemQuitTerminate);

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(context.cfg.gs("Navigator.menu.Quit.text"));
                menuItemFileQuit.setMnemonic(context.cfg.gs("Navigator.menu.Quit.mnemonic").charAt(0));
                menuItemFileQuit.addActionListener(e -> menuItemFileQuitActionPerformed(e));
                menuFile.add(menuItemFileQuit);
            }
            menuBarMain.add(menuFile);

            //======== menuEdit ========
            {
                menuEdit.setText(context.cfg.gs("Navigator.menu.Edit.text"));
                menuEdit.setMnemonic(context.cfg.gs("Navigator.menu.Edit.mnemonic").charAt(0));

                //---- menuItemCopy ----
                menuItemCopy.setText(context.cfg.gs("Navigator.menu.Copy.text"));
                menuItemCopy.setMnemonic(context.cfg.gs("Navigator.menu.Copy.mnemonic").charAt(0));
                menuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(context.cfg.gs("Navigator.menu.Cut.text"));
                menuItemCut.setMnemonic(context.cfg.gs("Navigator.menu.Cut.mnemonic").charAt(0));
                menuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(context.cfg.gs("Navigator.menu.Paste.text"));
                menuItemPaste.setMnemonic(context.cfg.gs("Navigator.menu.Paste.mnemonic").charAt(0));
                menuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemDelete ----
                menuItemDelete.setText(context.cfg.gs("Navigator.menu.Delete.text"));
                menuItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                menuItemDelete.setMnemonic(context.cfg.gs("Navigator.menu.Delete.mnemonic").charAt(0));
                menuEdit.add(menuItemDelete);
                menuEdit.addSeparator();

                //---- menuItemNewFolder ----
                menuItemNewFolder.setText(context.cfg.gs("Navigator.menu.New.folder.text"));
                menuItemNewFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
                menuItemNewFolder.setMnemonic(context.cfg.gs("Navigator.menu.New.folder.mnemonic").charAt(0));
                menuEdit.add(menuItemNewFolder);

                //---- menuItemRename ----
                menuItemRename.setText(context.cfg.gs("Navigator.menu.Rename.text"));
                menuItemRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
                menuItemRename.setMnemonic(context.cfg.gs("Navigator.menu.Rename.mnemonic").charAt(0));
                menuEdit.add(menuItemRename);

                //---- menuItemTouch ----
                menuItemTouch.setText(context.cfg.gs("Navigator.menu.Touch.text"));
                menuItemTouch.setMnemonic(context.cfg.gs("Navigator.menu.Touch.mnemonic").charAt(0));
                menuEdit.add(menuItemTouch);
                menuEdit.addSeparator();

                //---- menuItemFind ----
                menuItemFind.setText(context.cfg.gs("Navigator.menu.Find.text"));
                menuItemFind.setMnemonic(context.cfg.gs("Navigator.menu.Find.mnemonic").charAt(0));
                menuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
                menuEdit.add(menuItemFind);

                //---- menuItemFindNext ----
                menuItemFindNext.setText(context.cfg.gs("Navigator.menuItemFindNext.text"));
                menuItemFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
                menuItemFindNext.setMnemonic(context.cfg.gs("Navigator.menuItemFindNext.mnemonic").charAt(0));
                menuEdit.add(menuItemFindNext);
            }
            menuBarMain.add(menuEdit);

            //======== menuView ========
            {
                menuView.setText(context.cfg.gs("Navigator.menu.View.text"));
                menuView.setMnemonic(context.cfg.gs("Navigator.menu.View.mnemonic").charAt(0));

                //---- menuItemProgress ----
                menuItemProgress.setText(context.cfg.gs("Navigator.menu.Progress.text"));
                menuItemProgress.setMnemonic(context.cfg.gs("Navigator.menu.Progress.mnemonic").charAt(0));
                menuView.add(menuItemProgress);

                //---- menuItemRefresh ----
                menuItemRefresh.setText(context.cfg.gs("Navigator.menu.Refresh.text"));
                menuItemRefresh.setMnemonic(context.cfg.gs("Navigator.menuItemRefresh.mnemonic").charAt(0));
                menuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
                menuView.add(menuItemRefresh);
                menuView.addSeparator();

                //---- menuItemAutoRefresh ----
                menuItemAutoRefresh.setText(context.cfg.gs("Navigator.menuItemAutoRefresh.text"));
                menuItemAutoRefresh.setMnemonic(context.cfg.gs("Navigator.menuItemAutoRefresh.mnemonic").charAt(0));
                menuView.add(menuItemAutoRefresh);

                //---- menuItemShowHidden ----
                menuItemShowHidden.setText(context.cfg.gs("Navigator.menu.ShowHidden.text"));
                menuItemShowHidden.setMnemonic(context.cfg.gs("Navigator.menu.ShowHidden.mnemonic").charAt(0));
                menuItemShowHidden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
                menuItemShowHidden.setDisplayedMnemonicIndex(5);
                menuView.add(menuItemShowHidden);

                //---- menuItemWordWrap ----
                menuItemWordWrap.setText(context.cfg.gs("Navigator.menuItemWordWrap.text"));
                menuItemWordWrap.setMnemonic(context.cfg.gs("Navigator.menuItemWordWrap.mnemonic").charAt(0));
                menuItemWordWrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
                menuView.add(menuItemWordWrap);
            }
            menuBarMain.add(menuView);

            //======== menuBookmarks ========
            {
                menuBookmarks.setText(context.cfg.gs("Navigator.menu.Bookmarks.text"));
                menuBookmarks.setMnemonic(context.cfg.gs("Navigator.menu.Bookmarks.mnemonic").charAt(0));

                //---- menuItemAddBookmark ----
                menuItemAddBookmark.setText(context.cfg.gs("Navigator.menu.AddBookmark.text"));
                menuItemAddBookmark.setMnemonic(context.cfg.gs("Navigator.menu.AddBookmark.mnemonic").charAt(0));
                menuBookmarks.add(menuItemAddBookmark);

                //---- menuItemBookmarksDelete ----
                menuItemBookmarksDelete.setText(context.cfg.gs("Navigator.menu.BookmarksManage.text"));
                menuItemBookmarksDelete.setMnemonic(context.cfg.gs("Navigator.menu.BookmarksManage.mnemonic").charAt(0));
                menuBookmarks.add(menuItemBookmarksDelete);
                menuBookmarks.addSeparator();
            }
            menuBarMain.add(menuBookmarks);

            //======== menuTools ========
            {
                menuTools.setText(context.cfg.gs("Navigator.menu.Tools.text"));
                menuTools.setMnemonic(context.cfg.gs("Navigator.menu.Tools.mnemonic").charAt(0));

                //---- menuItemDuplicates ----
                menuItemDuplicates.setText(context.cfg.gs("Navigator.menu.Duplicates.text"));
                menuItemDuplicates.setMnemonic(context.cfg.gs("Navigator.menu.Duplicates.mnemonic").charAt(0));
                menuTools.add(menuItemDuplicates);

                //---- menuItemEmptyFinder ----
                menuItemEmptyFinder.setText(context.cfg.gs("Navigator.menuItemEmptyFinder.text"));
                menuItemEmptyFinder.setMnemonic(context.cfg.gs("Navigator.menuItemEmptyFinder.mnemonic").charAt(0));
                menuTools.add(menuItemEmptyFinder);

                //---- menuItemJunk ----
                menuItemJunk.setText(context.cfg.gs("Navigator.menu.Junk.text"));
                menuItemJunk.setMnemonic(context.cfg.gs("Navigator.menu.Junk.mnemonic").charAt(0));
                menuTools.add(menuItemJunk);

                //---- menuItemOperations ----
                menuItemOperations.setText(context.cfg.gs("Navigator.menuItemOperations.text"));
                menuItemOperations.setMnemonic(context.cfg.gs("Navigator.menuItemOperations.mnemonic").charAt(0));
                menuTools.add(menuItemOperations);

                //---- menuItemRenamer ----
                menuItemRenamer.setText(context.cfg.gs("Navigator.menu.Renamer.text"));
                menuItemRenamer.setMnemonic(context.cfg.gs("Navigator.menu.Renamer.mnemonic").charAt(0));
                menuTools.add(menuItemRenamer);

                //---- menuItemSleep ----
                menuItemSleep.setText(context.cfg.gs("Navigator.menuItemSleep.text"));
                menuItemSleep.setMnemonic(context.cfg.gs("Navigator.menuItemSleep.mnemonic").charAt(0));
                menuTools.add(menuItemSleep);
                menuTools.addSeparator();

                //---- menuItemExternalTools ----
                menuItemExternalTools.setText(context.cfg.gs("Navigator.menu.ExternalTools.text"));
                menuItemExternalTools.setMnemonic(context.cfg.gs("Navigator.menuItemExternalTools.mnemonic").charAt(0));
                menuItemExternalTools.setEnabled(false);
                menuItemExternalTools.setDisplayedMnemonicIndex(Integer.parseInt(context.cfg.gs("Navigator.menuItemExternalTools.displayedMnemonicIndex")));
                menuTools.add(menuItemExternalTools);

                //---- menuItemPlexGenerator ----
                menuItemPlexGenerator.setText(context.cfg.gs("Navigator.menu.PlexGenerator.text"));
                menuItemPlexGenerator.setMnemonic(context.cfg.gs("Navigator.menu.PlexGenerator.mnemonic").charAt(0));
                menuItemPlexGenerator.setEnabled(false);
                menuItemPlexGenerator.setMargin(new Insets(2, 18, 2, 2));
                menuTools.add(menuItemPlexGenerator);

                //---- menuItem1 ----
                menuItem1.setText("Handbrake");
                menuItem1.setMargin(new Insets(2, 18, 2, 2));
                menuItem1.setEnabled(false);
                menuTools.add(menuItem1);
            }
            menuBarMain.add(menuTools);

            //======== menuJobs ========
            {
                menuJobs.setText(context.cfg.gs("Navigator.menu.Jobs.text"));
                menuJobs.setMnemonic(context.cfg.gs("Navigator.menu.Jobs.mnemonic").charAt(0));

                //---- menuItemJobsManage ----
                menuItemJobsManage.setText(context.cfg.gs("Navigator.menu.JobsManage.text"));
                menuItemJobsManage.setMnemonic(context.cfg.gs("Navigator.menu.JobsManage.mnemonic").charAt(0));
                menuJobs.add(menuItemJobsManage);
                menuJobs.addSeparator();
            }
            menuBarMain.add(menuJobs);

            //======== menuSystem ========
            {
                menuSystem.setText("System");
                menuSystem.setMnemonic(context.cfg.gs("Navigator.menuSystem.mnemonic").charAt(0));

                //---- menuItemSettings ----
                menuItemSettings.setText(context.cfg.gs("Navigator.menu.Settings.text"));
                menuItemSettings.setMnemonic(context.cfg.gs("Navigator.menu.Settings.mnemonic").charAt(0));
                menuSystem.add(menuItemSettings);
                menuSystem.addSeparator();

                //---- menuItemAuthKeys ----
                menuItemAuthKeys.setText(context.cfg.gs("Navigator.menuItemAuthKeys.text"));
                menuItemAuthKeys.setMnemonic(context.cfg.gs("Navigator.menuItemAuthKeys.mnemonic").charAt(0));
                menuItemAuthKeys.setEnabled(false);
                menuSystem.add(menuItemAuthKeys);

                //---- menuItemHintKeys ----
                menuItemHintKeys.setText(context.cfg.gs("Navigator.menuItemHintKeys.text"));
                menuItemHintKeys.setMnemonic(context.cfg.gs("Navigator.menuItemHintKeys.mnemonic").charAt(0));
                menuItemHintKeys.setEnabled(false);
                menuSystem.add(menuItemHintKeys);
                menuSystem.addSeparator();

                //---- menuItemBlacklist ----
                menuItemBlacklist.setText(context.cfg.gs("Navigator.menuItemBlacklist.text"));
                menuItemBlacklist.setMnemonic(context.cfg.gs("Navigator.menuItemBlacklist.mnemonic").charAt(0));
                menuItemBlacklist.setEnabled(false);
                menuSystem.add(menuItemBlacklist);

                //---- menuItemWhitelist ----
                menuItemWhitelist.setText(context.cfg.gs("Navigator.menuItemWhitelist.text"));
                menuItemWhitelist.setEnabled(false);
                menuItemWhitelist.setMnemonic(context.cfg.gs("Navigator.menuItemWhitelist.mnemonic").charAt(0));
                menuSystem.add(menuItemWhitelist);
            }
            menuBarMain.add(menuSystem);

            //======== menuWindows ========
            {
                menuWindows.setText(context.cfg.gs("Navigator.menu.Windows.text"));
                menuWindows.setMnemonic(context.cfg.gs("Navigator.menu.Windows.mnemonic").charAt(0));

                //---- menuItemMaximize ----
                menuItemMaximize.setText(context.cfg.gs("Navigator.menu.Maximize.text"));
                menuItemMaximize.setMnemonic(context.cfg.gs("Navigator.menu.Maximize.mnemonic").charAt(0));
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(context.cfg.gs("Navigator.menu.Minimize.text"));
                menuItemMinimize.setMnemonic(context.cfg.gs("Navigator.menu.Minimize.mnemonic").charAt(0));
                menuWindows.add(menuItemMinimize);

                //---- menuItemRestore ----
                menuItemRestore.setText(context.cfg.gs("Navigator.menu.Restore.text"));
                menuItemRestore.setMnemonic(context.cfg.gs("Navigator.menu.Restore.mnemonic").charAt(0));
                menuWindows.add(menuItemRestore);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(context.cfg.gs("Navigator.menu.SplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(context.cfg.gs("Navigator.menu.SplitHorizontal.mnemonic").charAt(0));
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(context.cfg.gs("Navigator.menu.SplitVertical.text"));
                menuItemSplitVertical.setMnemonic(context.cfg.gs("Navigator.menu.SplitVertical.mnemonic").charAt(0));
                menuWindows.add(menuItemSplitVertical);
            }
            menuBarMain.add(menuWindows);

            //======== menuHelp ========
            {
                menuHelp.setText(context.cfg.gs("Navigator.menu.Help.text"));
                menuHelp.setMnemonic(context.cfg.gs("Navigator.menu.Help.mnemonic").charAt(0));

                //---- menuItemControls ----
                menuItemControls.setText(context.cfg.gs("Navigator.menu.Controls.text"));
                menuItemControls.setMnemonic(context.cfg.gs("Navigator.menu.Controls.mnemonic").charAt(0));
                menuHelp.add(menuItemControls);

                //---- menuItemDocumentation ----
                menuItemDocumentation.setText(context.cfg.gs("Navigator.menu.Documentation.text"));
                menuItemDocumentation.setMnemonic(context.cfg.gs("Navigator.menu.Documentation.mnemonic").charAt(0));
                menuItemDocumentation.setToolTipText(context.cfg.gs("Navigator.menuItemDocumentation.toolTipText"));
                menuHelp.add(menuItemDocumentation);

                //---- menuItemGettingStarted ----
                menuItemGettingStarted.setText(context.cfg.gs("Navigator.menuItemGettingStarted.text"));
                menuItemGettingStarted.setEnabled(false);
                menuHelp.add(menuItemGettingStarted);

                //---- menuItemGitHubProject ----
                menuItemGitHubProject.setText(context.cfg.gs("Navigator.menu.GitHubProject.text"));
                menuItemGitHubProject.setMnemonic(context.cfg.gs("Navigator.menu.GitHubProject.mnemonic").charAt(0));
                menuItemGitHubProject.setToolTipText(context.cfg.gs("Navigator.menuItemGitHubProject.toolTipText"));
                menuHelp.add(menuItemGitHubProject);
                menuHelp.addSeparator();

                //---- menuItemUpdates ----
                menuItemUpdates.setText(context.cfg.gs("Navigator.menuItemUpdates.text"));
                menuItemUpdates.setMnemonic(context.cfg.gs("Navigator.menuItemUpdates.mnemonic").charAt(0));
                menuItemUpdates.setEnabled(false);
                menuHelp.add(menuItemUpdates);

                //---- menuItemAbout ----
                menuItemAbout.setText(context.cfg.gs("Navigator.menu.About.text"));
                menuItemAbout.setMnemonic(context.cfg.gs("Navigator.menu.About.mnemonic").charAt(0));
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
                                    buttonBack.setToolTipText(context.cfg.gs("Navigator.button.Back.toolTipText"));
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
                                    buttonForward.setToolTipText(context.cfg.gs("Navigator.button.Forward.toolTipText"));
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
                                    buttonUp.setToolTipText(context.cfg.gs("Navigator.button.Up.toolTipText"));
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
                                textFieldLocation.setToolTipText(context.cfg.gs("Navigator.textField.Location.toolTipText"));
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
                                panelHintTracking.setMaximumSize(new Dimension(136, 30));
                                panelHintTracking.setMinimumSize(new Dimension(136, 30));
                                panelHintTracking.setPreferredSize(new Dimension(136, 30));
                                panelHintTracking.setLayout(new BorderLayout());

                                //---- buttonHintTracking ----
                                buttonHintTracking.setText(context.cfg.gs("Navigator.button.HintTracking.text"));
                                buttonHintTracking.setMnemonic(context.cfg.gs("Navigator.buttonHintTracking.mnemonic").charAt(0));
                                buttonHintTracking.setFocusable(false);
                                buttonHintTracking.setPreferredSize(new Dimension(124, 30));
                                buttonHintTracking.setMinimumSize(new Dimension(124, 30));
                                buttonHintTracking.setMaximumSize(new Dimension(124, 30));
                                buttonHintTracking.setIcon(new ImageIcon(getClass().getResource("/hint-green.png")));
                                buttonHintTracking.setActionCommand("hints");
                                buttonHintTracking.setToolTipText(context.cfg.gs("Navigator.button.HintTracking.enabled.tooltip"));
                                panelHintTracking.add(buttonHintTracking, BorderLayout.CENTER);

                                //---- hSpacer2 ----
                                hSpacer2.setPreferredSize(new Dimension(6, 30));
                                hSpacer2.setMinimumSize(new Dimension(6, 30));
                                hSpacer2.setMaximumSize(new Dimension(6, 30));
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
                                            treeCollectionOne.setMaximumSize(new Dimension(32767, 72));
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
                                            tableCollectionOne.setShowHorizontalLines(false);
                                            tableCollectionOne.setShowVerticalLines(false);
                                            scrollPaneTableCollectionOne.setViewportView(tableCollectionOne);
                                        }
                                        splitPaneCollectionOne.setRightComponent(scrollPaneTableCollectionOne);
                                    }
                                    panelCollectionOne.add(splitPaneCollectionOne);
                                }
                                tabbedPaneBrowserOne.addTab(context.cfg.gs("Navigator.panel.CollectionOne.tab.title"), panelCollectionOne);
                                tabbedPaneBrowserOne.setMnemonicAt(0, context.cfg.gs("Navigator.panel.CollectionOne.tab.mnemonic").charAt(0));

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
                                            tableSystemOne.setShowHorizontalLines(false);
                                            tableSystemOne.setShowVerticalLines(false);
                                            scrollPaneTableSystemOne.setViewportView(tableSystemOne);
                                        }
                                        splitPaneSystemOne.setRightComponent(scrollPaneTableSystemOne);
                                    }
                                    panelSystemOne.add(splitPaneSystemOne);
                                }
                                tabbedPaneBrowserOne.addTab(context.cfg.gs("Navigator.panel.SystemOne.tab.title"), panelSystemOne);
                                tabbedPaneBrowserOne.setMnemonicAt(1, context.cfg.gs("Navigator.panel.SystemOne.tab.mnemonic").charAt(0));
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
                                            tableCollectionTwo.setShowHorizontalLines(false);
                                            tableCollectionTwo.setShowVerticalLines(false);
                                            scrollPaneTableCollectionTwo.setViewportView(tableCollectionTwo);
                                        }
                                        splitPaneCollectionTwo.setRightComponent(scrollPaneTableCollectionTwo);
                                    }
                                    panelCollectionTwo.add(splitPaneCollectionTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(context.cfg.gs("Navigator.panel.CollectionTwo.tab.title"), panelCollectionTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(0, context.cfg.gs("Navigator.panel.CollectionTwo.tab.mnemonic").charAt(0));

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
                                            tableSystemTwo.setShowHorizontalLines(false);
                                            tableSystemTwo.setShowVerticalLines(false);
                                            scrollPaneTableSystemTwo.setViewportView(tableSystemTwo);
                                        }
                                        splitPaneSystemTwo.setRightComponent(scrollPaneTableSystemTwo);
                                    }
                                    panelSystemTwo.add(splitPaneSystemTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(context.cfg.gs("Navigator.panel.SystemTwo.tab.title"), panelSystemTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(1, context.cfg.gs("Navigator.panel.SystemTwo.tab.mnemonic").charAt(0));
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
                            textAreaLog.setFont(new Font("Courier 10 Pitch", Font.PLAIN, 12));
                            textAreaLog.setWrapStyleWord(true);
                            scrollPaneLog.setViewportView(textAreaLog);
                        }
                        tabbedPaneNavigatorBottom.addTab(context.cfg.gs("Navigator.scrollPane.Log.tab.title"), scrollPaneLog);
                        tabbedPaneNavigatorBottom.setMnemonicAt(0, context.cfg.gs("Navigator.scrollPaneLog.tab.mnemonic_2").charAt(0));

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
                        tabbedPaneNavigatorBottom.addTab(context.cfg.gs("Navigator.scrollPane.Properties.tab.title"), scrollPaneProperties);
                        tabbedPaneNavigatorBottom.setMnemonicAt(1, context.cfg.gs("Navigator.scrollPaneProperties.tab.mnemonic_2").charAt(0));
                    }
                    splitPaneBrowser.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab(context.cfg.gs("Navigator.splitPane.Browser.tab.title"), splitPaneBrowser);
                tabbedPaneMain.setMnemonicAt(0, context.cfg.gs("Navigator.splitPane.Browser.tab.mnemonic").charAt(0));

                //======== panelLibraries ========
                {
                    panelLibraries.setLayout(new BorderLayout());

                    //======== panelTop ========
                    {
                        panelTop.setMinimumSize(new Dimension(140, 38));
                        panelTop.setPreferredSize(new Dimension(614, 38));
                        panelTop.setLayout(new BorderLayout());

                        //======== panelTopButtons ========
                        {
                            panelTopButtons.setMinimumSize(new Dimension(140, 38));
                            panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                            //---- buttonNew ----
                            buttonNew.setText(context.cfg.gs("Navigator.buttonNew.text"));
                            buttonNew.setMnemonic(context.cfg.gs("Navigator.buttonNew.mnemonic").charAt(0));
                            buttonNew.setToolTipText(context.cfg.gs("Navigator.buttonNew.toolTipText"));
                            buttonNew.addActionListener(e -> actionNewClicked(e));
                            panelTopButtons.add(buttonNew);

                            //---- buttonCopy ----
                            buttonCopy.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                            buttonCopy.setMnemonic(context.cfg.gs("Navigator.buttonCopy.mnemonic").charAt(0));
                            buttonCopy.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                            buttonCopy.addActionListener(e -> actionCopyClicked(e));
                            panelTopButtons.add(buttonCopy);

                            //---- buttonDelete ----
                            buttonDelete.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                            buttonDelete.setMnemonic(context.cfg.gs("Navigator.buttonDelete.mnemonic").charAt(0));
                            buttonDelete.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
                            buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                            panelTopButtons.add(buttonDelete);
                        }
                        panelTop.add(panelTopButtons, BorderLayout.WEST);

                        //======== panelHelp ========
                        {
                            panelHelp.setPreferredSize(new Dimension(40, 38));
                            panelHelp.setMinimumSize(new Dimension(0, 38));
                            panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                            //---- labelHelp ----
                            labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                            labelHelp.setPreferredSize(new Dimension(32, 30));
                            labelHelp.setMinimumSize(new Dimension(32, 30));
                            labelHelp.setMaximumSize(new Dimension(32, 30));
                            labelHelp.setToolTipText(context.cfg.gs("Navigator.labelHelp.toolTipText"));
                            labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            labelHelp.setIconTextGap(0);
                            labelHelp.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    actionHelpClicked(e);
                                }
                            });
                            panelHelp.add(labelHelp);
                        }
                        panelTop.add(panelHelp, BorderLayout.EAST);
                    }
                    panelLibraries.add(panelTop, BorderLayout.NORTH);

                    //======== splitPaneContent ========
                    {
                        splitPaneContent.setDividerLocation(142);
                        splitPaneContent.setLastDividerLocation(142);
                        splitPaneContent.setMinimumSize(new Dimension(140, 80));

                        //======== scrollPaneConfig ========
                        {
                            scrollPaneConfig.setMinimumSize(new Dimension(140, 16));
                            scrollPaneConfig.setPreferredSize(new Dimension(142, 146));

                            //---- configItems ----
                            configItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            configItems.setShowVerticalLines(false);
                            configItems.setFillsViewportHeight(true);
                            configItems.setShowHorizontalLines(false);
                            configItems.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    configItemsMouseClicked(e);
                                }
                            });
                            scrollPaneConfig.setViewportView(configItems);
                        }
                        splitPaneContent.setLeftComponent(scrollPaneConfig);

                        //======== panelOptions ========
                        {
                            panelOptions.setMinimumSize(new Dimension(0, 78));
                            panelOptions.setLayout(new BorderLayout());

                            //======== panelControls ========
                            {
                                panelControls.setLayout(new BorderLayout());

                                //======== topOptions ========
                                {
                                    topOptions.setLayout(new BorderLayout());

                                    //---- vSpacer0 ----
                                    vSpacer0.setPreferredSize(new Dimension(10, 2));
                                    vSpacer0.setMinimumSize(new Dimension(10, 2));
                                    vSpacer0.setMaximumSize(new Dimension(10, 2));
                                    topOptions.add(vSpacer0, BorderLayout.NORTH);

                                    //======== panelLibraryType ========
                                    {
                                        panelLibraryType.setLayout(new BoxLayout(panelLibraryType, BoxLayout.X_AXIS));

                                        //---- hSpacer3 ----
                                        hSpacer3.setPreferredSize(new Dimension(4, 10));
                                        hSpacer3.setMinimumSize(new Dimension(4, 12));
                                        hSpacer3.setMaximumSize(new Dimension(4, 32767));
                                        panelLibraryType.add(hSpacer3);

                                        //---- labelLibaryType ----
                                        labelLibaryType.setText("Library Type");
                                        labelLibaryType.setMaximumSize(new Dimension(110, 16));
                                        labelLibaryType.setFont(labelLibaryType.getFont().deriveFont(labelLibaryType.getFont().getStyle() | Font.BOLD, labelLibaryType.getFont().getSize() + 1f));
                                        labelLibaryType.setPreferredSize(new Dimension(110, 16));
                                        labelLibaryType.setMinimumSize(new Dimension(110, 16));
                                        panelLibraryType.add(labelLibaryType);
                                    }
                                    topOptions.add(panelLibraryType, BorderLayout.WEST);

                                    //======== panelCardBox ========
                                    {
                                        panelCardBox.setLayout(new BoxLayout(panelCardBox, BoxLayout.Y_AXIS));

                                        //---- vSpacer3 ----
                                        vSpacer3.setMinimumSize(new Dimension(12, 2));
                                        vSpacer3.setMaximumSize(new Dimension(32767, 2));
                                        vSpacer3.setPreferredSize(new Dimension(10, 2));
                                        panelCardBox.add(vSpacer3);
                                        panelCardBox.add(separator13);

                                        //---- vSpacer4 ----
                                        vSpacer4.setMinimumSize(new Dimension(12, 2));
                                        vSpacer4.setMaximumSize(new Dimension(32767, 2));
                                        vSpacer4.setPreferredSize(new Dimension(10, 2));
                                        panelCardBox.add(vSpacer4);

                                        //======== panelLibraryTypeCards ========
                                        {
                                            panelLibraryTypeCards.setMaximumSize(new Dimension(32676, 184));
                                            panelLibraryTypeCards.setPreferredSize(new Dimension(343, 184));
                                            panelLibraryTypeCards.setMinimumSize(new Dimension(343, 184));
                                            panelLibraryTypeCards.setLayout(new CardLayout());

                                            //======== panelGettingStartedCard ========
                                            {
                                                panelGettingStartedCard.setLayout(new BorderLayout());

                                                //---- labelOperationGettingStarted ----
                                                labelOperationGettingStarted.setText(context.cfg.gs("Navigator.labelLibariesGettingStarted.text"));
                                                labelOperationGettingStarted.setFont(labelOperationGettingStarted.getFont().deriveFont(labelOperationGettingStarted.getFont().getStyle() | Font.BOLD));
                                                labelOperationGettingStarted.setHorizontalAlignment(SwingConstants.CENTER);
                                                panelGettingStartedCard.add(labelOperationGettingStarted, BorderLayout.CENTER);
                                            }
                                            panelLibraryTypeCards.add(panelGettingStartedCard, "cardGettingStarted");

                                            //======== panelLibraryCard ========
                                            {
                                                panelLibraryCard.addComponentListener(new ComponentAdapter() {
                                                    @Override
                                                    public void componentShown(ComponentEvent e) {
                                                        cardShown(e);
                                                    }
                                                });
                                                panelLibraryCard.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelLibraryCard.getLayout()).rowHeights = new int[] {0, 0, 0};
                                                ((GridBagLayout)panelLibraryCard.getLayout()).columnWeights = new double[] {1.0};
                                                ((GridBagLayout)panelLibraryCard.getLayout()).rowWeights = new double[] {1.0, 1.0, 1.0E-4};
                                            }
                                            panelLibraryTypeCards.add(panelLibraryCard, "cardLibrary");

                                            //======== panelHintServerCard ========
                                            {
                                                panelHintServerCard.addComponentListener(new ComponentAdapter() {
                                                    @Override
                                                    public void componentShown(ComponentEvent e) {
                                                        cardShown(e);
                                                    }
                                                });
                                                panelHintServerCard.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelHintServerCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                                ((GridBagLayout)panelHintServerCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                            }
                                            panelLibraryTypeCards.add(panelHintServerCard, "cardNumbering");

                                            //======== panelTargetsCard ========
                                            {
                                                panelTargetsCard.addComponentListener(new ComponentAdapter() {
                                                    @Override
                                                    public void componentShown(ComponentEvent e) {
                                                        cardShown(e);
                                                    }
                                                });
                                                panelTargetsCard.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelTargetsCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                                ((GridBagLayout)panelTargetsCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                            }
                                            panelLibraryTypeCards.add(panelTargetsCard, "cardTargets");

                                            //======== panelXCard ========
                                            {
                                                panelXCard.addComponentListener(new ComponentAdapter() {
                                                    @Override
                                                    public void componentShown(ComponentEvent e) {
                                                        cardShown(e);
                                                    }
                                                });
                                                panelXCard.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelXCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                                ((GridBagLayout)panelXCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                            }
                                            panelLibraryTypeCards.add(panelXCard, "cardX");

                                            //======== panelYCard ========
                                            {
                                                panelYCard.addComponentListener(new ComponentAdapter() {
                                                    @Override
                                                    public void componentShown(ComponentEvent e) {
                                                        cardShown(e);
                                                    }
                                                });
                                                panelYCard.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelYCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                                ((GridBagLayout)panelYCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                            }
                                            panelLibraryTypeCards.add(panelYCard, "cardY");
                                        }
                                        panelCardBox.add(panelLibraryTypeCards);
                                    }
                                    topOptions.add(panelCardBox, BorderLayout.SOUTH);
                                }
                                panelControls.add(topOptions, BorderLayout.NORTH);
                            }
                            panelOptions.add(panelControls, BorderLayout.NORTH);

                            //======== tabbedPaneLibrarySpaces ========
                            {

                                //======== bibliographyTab ========
                                {
                                    bibliographyTab.setLayout(new BorderLayout());

                                    //======== librariesSplit ========
                                    {
                                        librariesSplit.setDividerLocation(201);

                                        //======== scrollPaneLibraries ========
                                        {
                                            scrollPaneLibraries.setViewportView(tableLibraries);
                                        }
                                        librariesSplit.setLeftComponent(scrollPaneLibraries);

                                        //======== panelSources ========
                                        {
                                            panelSources.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelSources.getLayout()).columnWidths = new int[] {0, 0, 0};
                                            ((GridBagLayout)panelSources.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelSources.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                                            ((GridBagLayout)panelSources.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelSpacer42 ----
                                            labelSpacer42.setText(" ");
                                            panelSources.add(labelSpacer42, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelSources ----
                                            labelSources.setText(context.cfg.gs("Navigator.labelSources.text"));
                                            labelSources.setFont(labelSources.getFont().deriveFont(labelSources.getFont().getStyle() | Font.BOLD, labelSources.getFont().getSize() + 1f));
                                            labelSources.setHorizontalAlignment(SwingConstants.LEFT);
                                            labelSources.setPreferredSize(new Dimension(200, 16));
                                            labelSources.setMinimumSize(new Dimension(200, 16));
                                            panelSources.add(labelSources, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 4, 0, 0), 0, 0));

                                            //======== scrollPane1 ========
                                            {
                                                scrollPane1.setMinimumSize(new Dimension(408, 20));
                                                scrollPane1.setViewportView(table1);
                                            }
                                            panelSources.add(scrollPane1, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panel1 ========
                                            {
                                                panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                //---- buttonAddOrigin ----
                                                buttonAddOrigin.setText(context.cfg.gs("Navigator.buttonAddOrigin.text"));
                                                buttonAddOrigin.setFont(buttonAddOrigin.getFont().deriveFont(buttonAddOrigin.getFont().getSize() - 2f));
                                                buttonAddOrigin.setPreferredSize(new Dimension(78, 24));
                                                buttonAddOrigin.setMinimumSize(new Dimension(78, 24));
                                                buttonAddOrigin.setMaximumSize(new Dimension(78, 24));
                                                buttonAddOrigin.setMnemonic(context.cfg.gs("Navigator.buttonAddOrigin.mnemonic").charAt(0));
                                                buttonAddOrigin.setToolTipText(context.cfg.gs("Navigator.buttonAddOrigin.toolTipText"));
                                                buttonAddOrigin.addActionListener(e -> actionOriginAddClicked(e));
                                                panel1.add(buttonAddOrigin);

                                                //---- buttonOriginUp ----
                                                buttonOriginUp.setText("^");
                                                buttonOriginUp.setMaximumSize(new Dimension(24, 24));
                                                buttonOriginUp.setMinimumSize(new Dimension(24, 24));
                                                buttonOriginUp.setPreferredSize(new Dimension(24, 24));
                                                buttonOriginUp.setFont(buttonOriginUp.getFont().deriveFont(buttonOriginUp.getFont().getSize() - 2f));
                                                buttonOriginUp.setToolTipText(context.cfg.gs("Navigator.buttonOriginUp.toolTipText"));
                                                buttonOriginUp.addActionListener(e -> actionOriginUpClicked(e));
                                                panel1.add(buttonOriginUp);

                                                //---- buttonOriginDown ----
                                                buttonOriginDown.setText("v");
                                                buttonOriginDown.setFont(buttonOriginDown.getFont().deriveFont(buttonOriginDown.getFont().getSize() - 2f));
                                                buttonOriginDown.setMaximumSize(new Dimension(24, 24));
                                                buttonOriginDown.setMinimumSize(new Dimension(24, 24));
                                                buttonOriginDown.setPreferredSize(new Dimension(24, 24));
                                                buttonOriginDown.setToolTipText(context.cfg.gs("Navigator.buttonOriginDown.toolTipText"));
                                                buttonOriginDown.addActionListener(e -> actionOriginDownClicked(e));
                                                panel1.add(buttonOriginDown);

                                                //---- buttonRemoveOrigin ----
                                                buttonRemoveOrigin.setText(context.cfg.gs("Navigator.buttonRemoveOrigin.text"));
                                                buttonRemoveOrigin.setFont(buttonRemoveOrigin.getFont().deriveFont(buttonRemoveOrigin.getFont().getSize() - 2f));
                                                buttonRemoveOrigin.setPreferredSize(new Dimension(78, 24));
                                                buttonRemoveOrigin.setMinimumSize(new Dimension(78, 24));
                                                buttonRemoveOrigin.setMaximumSize(new Dimension(78, 24));
                                                buttonRemoveOrigin.setMnemonic(context.cfg.gs("Navigator.buttonRemoveOrigin.mnemonic").charAt(0));
                                                buttonRemoveOrigin.setToolTipText(context.cfg.gs("Navigator.buttonRemoveOrigin.toolTipText"));
                                                buttonRemoveOrigin.addActionListener(e -> actionOriginRemoveClicked(e));
                                                panel1.add(buttonRemoveOrigin);
                                            }
                                            panelSources.add(panel1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        librariesSplit.setRightComponent(panelSources);
                                    }
                                    bibliographyTab.add(librariesSplit, BorderLayout.CENTER);

                                    //======== panelBiblioButtons ========
                                    {
                                        panelBiblioButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                        //---- buttonNewLibrary ----
                                        buttonNewLibrary.setText(context.cfg.gs("Navigator.buttonNewLibrary.text"));
                                        buttonNewLibrary.setFont(buttonNewLibrary.getFont().deriveFont(buttonNewLibrary.getFont().getSize() - 2f));
                                        buttonNewLibrary.setPreferredSize(new Dimension(78, 24));
                                        buttonNewLibrary.setMinimumSize(new Dimension(78, 24));
                                        buttonNewLibrary.setMaximumSize(new Dimension(78, 24));
                                        buttonNewLibrary.setMnemonic(context.cfg.gs("Navigator.buttonNewLibrary.mnemonic_2").charAt(0));
                                        buttonNewLibrary.setToolTipText(context.cfg.gs("Navigator.buttonNewLibrary.toolTipText"));
                                        buttonNewLibrary.addActionListener(e -> actionRefreshClicked(e));
                                        panelBiblioButtons.add(buttonNewLibrary);
                                    }
                                    bibliographyTab.add(panelBiblioButtons, BorderLayout.SOUTH);
                                }
                                tabbedPaneLibrarySpaces.addTab(context.cfg.gs("Navigator.bibliographyTab.tab.title"), bibliographyTab);

                                //======== locationsTab ========
                                {
                                    locationsTab.setLayout(new BorderLayout());

                                    //======== scrollPaneLocations ========
                                    {
                                        scrollPaneLocations.setViewportView(tableLocations);
                                    }
                                    locationsTab.add(scrollPaneLocations, BorderLayout.CENTER);

                                    //======== panelLocButtons ========
                                    {
                                        panelLocButtons.setLayout(new FlowLayout());

                                        //---- buttonNewLocation ----
                                        buttonNewLocation.setText(context.cfg.gs("Navigator.buttonNewLocation.text"));
                                        panelLocButtons.add(buttonNewLocation);
                                    }
                                    locationsTab.add(panelLocButtons, BorderLayout.SOUTH);
                                }
                                tabbedPaneLibrarySpaces.addTab(context.cfg.gs("Navigator.locationsTab.tab.title"), locationsTab);
                            }
                            panelOptions.add(tabbedPaneLibrarySpaces, BorderLayout.CENTER);
                        }
                        splitPaneContent.setRightComponent(panelOptions);
                    }
                    panelLibraries.add(splitPaneContent, BorderLayout.CENTER);

                    //======== buttonBar ========
                    {
                        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                        buttonBar.setLayout(new GridBagLayout());
                        ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 82, 80};
                        ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                        //---- saveButton ----
                        saveButton.setText(context.cfg.gs("Navigator.saveButton.text"));
                        saveButton.setToolTipText(context.cfg.gs("Navigator.saveButton.toolTipText"));
                        saveButton.addActionListener(e -> actionSaveClicked(e));
                        buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 2), 0, 0));

                        //---- cancelButton ----
                        cancelButton.setText(context.cfg.gs("Navigator.cancelButton.text"));
                        cancelButton.setToolTipText(context.cfg.gs("Navigator.cancelButton.toolTipText"));
                        cancelButton.addActionListener(e -> actionCancelClicked(e));
                        buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));
                    }
                    panelLibraries.add(buttonBar, BorderLayout.SOUTH);
                }
                tabbedPaneMain.addTab(context.cfg.gs("Navigator.splitPane.Libraries.tab.title"), panelLibraries);
                tabbedPaneMain.setMnemonicAt(1, context.cfg.gs("Navigator.splitPane.Libraries.tab.mnemonic").charAt(0));
            }
            panelMain.add(tabbedPaneMain);
        }
        contentPane.add(panelMain, BorderLayout.CENTER);

        //======== panelStatus ========
        {
            panelStatus.setLayout(new GridBagLayout());

            //---- labelStatusLeft ----
            labelStatusLeft.setHorizontalAlignment(SwingConstants.LEFT);
            panelStatus.add(labelStatusLeft, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                new Insets(0, 4, 0, 4), 0, 0));

            //---- labelStatusMiddle ----
            labelStatusMiddle.setHorizontalAlignment(SwingConstants.CENTER);
            panelStatus.add(labelStatusMiddle, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 4), 0, 0));

            //---- labelStatusRight ----
            labelStatusRight.setHorizontalAlignment(SwingConstants.RIGHT);
            panelStatus.add(labelStatusRight, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 8), 0, 0));
        }
        contentPane.add(panelStatus, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());

        //======== popupMenuBrowser ========
        {
            popupMenuBrowser.setPreferredSize(new Dimension(180, 194));

            //---- popupMenuItemRefresh ----
            popupMenuItemRefresh.setText(context.cfg.gs("Navigator.popupMenuItemRefresh.text"));
            popupMenuItemRefresh.setMnemonic(context.cfg.gs("Navigator.popupMenuItemRefresh.mnemonic").charAt(0));
            popupMenuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            popupMenuBrowser.add(popupMenuItemRefresh);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemCopy ----
            popupMenuItemCopy.setText(context.cfg.gs("Navigator.popupMenu.Copy.text"));
            popupMenuItemCopy.setMnemonic(context.cfg.gs("Navigator.popupMenu.Copy.mnemonic").charAt(0));
            popupMenuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
            popupMenuBrowser.add(popupMenuItemCopy);

            //---- popupMenuItemCut ----
            popupMenuItemCut.setText(context.cfg.gs("Navigator.popupMenu.Cut.text"));
            popupMenuItemCut.setMnemonic(context.cfg.gs("Navigator.popupMenu.Cut.mnemonic").charAt(0));
            popupMenuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
            popupMenuBrowser.add(popupMenuItemCut);

            //---- popupMenuItemPaste ----
            popupMenuItemPaste.setText(context.cfg.gs("Navigator.popupMenu.Paste.text"));
            popupMenuItemPaste.setMnemonic(context.cfg.gs("Navigator.popupMenu.Paste.mnemonic").charAt(0));
            popupMenuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
            popupMenuBrowser.add(popupMenuItemPaste);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemDelete ----
            popupMenuItemDelete.setText(context.cfg.gs("Navigator.popupMenu.Delete.text"));
            popupMenuItemDelete.setMnemonic(context.cfg.gs("Navigator.popupMenu.Delete.mnemonic").charAt(0));
            popupMenuItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            popupMenuBrowser.add(popupMenuItemDelete);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemNewFolder ----
            popupMenuItemNewFolder.setText(context.cfg.gs("Navigator.popupMenu.NewFolder.text"));
            popupMenuItemNewFolder.setMnemonic(context.cfg.gs("Navigator.popupMenu.NewFolder.mnemonic").charAt(0));
            popupMenuItemNewFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
            popupMenuBrowser.add(popupMenuItemNewFolder);

            //---- popupMenuItemRename ----
            popupMenuItemRename.setText(context.cfg.gs("Navigator.popupMenu.Rename.text"));
            popupMenuItemRename.setMnemonic(context.cfg.gs("Navigator.popupMenu.Rename.mnemonic").charAt(0));
            popupMenuItemRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
            popupMenuBrowser.add(popupMenuItemRename);

            //---- popupMenuItemTouch ----
            popupMenuItemTouch.setText(context.cfg.gs("Navigator.popupMenu.Touch.text"));
            popupMenuItemTouch.setMnemonic(context.cfg.gs("Navigator.popupMenu.Touch.mnemonic").charAt(0));
            popupMenuBrowser.add(popupMenuItemTouch);
        }

        //======== popupMenuLog ========
        {
            popupMenuLog.setPreferredSize(new Dimension(180, 156));

            //---- popupMenuItemFindNext ----
            popupMenuItemFindNext.setText(context.cfg.gs("Navigator.popupMenuItemFindNext.text"));
            popupMenuItemFindNext.setMnemonic(context.cfg.gs("Navigator.popupMenuItemFindNext.mnemonic").charAt(0));
            popupMenuItemFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            popupMenuLog.add(popupMenuItemFindNext);

            //---- popupMenuItemFind ----
            popupMenuItemFind.setText(context.cfg.gs("Navigator.popupMenuItemFind.text"));
            popupMenuItemFind.setMnemonic(context.cfg.gs("Navigator.popupMenuItemFind.mnemonic").charAt(0));
            popupMenuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
            popupMenuLog.add(popupMenuItemFind);
            popupMenuLog.addSeparator();

            //---- popupMenuItemTop ----
            popupMenuItemTop.setText(context.cfg.gs("Navigator.popupMenuItemTop.text"));
            popupMenuItemTop.setMnemonic(context.cfg.gs("Navigator.popupMenuItemTop.mnemonic").charAt(0));
            popupMenuItemTop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK));
            popupMenuLog.add(popupMenuItemTop);

            //---- popupMenuItemBottom ----
            popupMenuItemBottom.setText(context.cfg.gs("Navigator.popupMenu.Bottom.text"));
            popupMenuItemBottom.setMnemonic(context.cfg.gs("Navigator.popupMenu.Bottom.mnemonic").charAt(0));
            popupMenuItemBottom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK));
            popupMenuLog.add(popupMenuItemBottom);
            popupMenuLog.addSeparator();

            //---- popupMenuItemClear ----
            popupMenuItemClear.setText(context.cfg.gs("Navigator.popupMenu.Clear.text"));
            popupMenuItemClear.setMnemonic(context.cfg.gs("Navigator.popupMenu.Clear.mnemonic").charAt(0));
            popupMenuLog.add(popupMenuItemClear);
            popupMenuLog.addSeparator();

            //---- popupCheckBoxMenuItemWordWrap ----
            popupCheckBoxMenuItemWordWrap.setText(context.cfg.gs("Navigator.popupCheckBoxMenuItemWordWrap.text"));
            popupCheckBoxMenuItemWordWrap.setMnemonic(context.cfg.gs("Navigator.popupCheckBoxMenuItemWordWrap.mnemonic").charAt(0));
            popupCheckBoxMenuItemWordWrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
            popupMenuLog.add(popupCheckBoxMenuItemWordWrap);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JMenuBar menuBarMain;
    public JMenu menuFile;
    public JMenuItem menuItemOpenPublisher;
    public JMenuItem menuItemOpenSubscriber;
    public JMenuItem menuItemOpenHintKeys;
    public JMenuItem menuItemOpenHintTracking;
    public JMenuItem menuItemSaveLayout;
    public JMenuItem menuItemQuitTerminate;
    public JMenuItem menuItemFileQuit;
    public JMenu menuEdit;
    public JMenuItem menuItemCopy;
    public JMenuItem menuItemCut;
    public JMenuItem menuItemPaste;
    public JMenuItem menuItemDelete;
    public JMenuItem menuItemNewFolder;
    public JMenuItem menuItemRename;
    public JMenuItem menuItemTouch;
    public JMenuItem menuItemFind;
    public JMenuItem menuItemFindNext;
    public JMenu menuView;
    public JMenuItem menuItemProgress;
    public JMenuItem menuItemRefresh;
    public JCheckBoxMenuItem menuItemAutoRefresh;
    public JCheckBoxMenuItem menuItemShowHidden;
    public JCheckBoxMenuItem menuItemWordWrap;
    public JMenu menuBookmarks;
    public JMenuItem menuItemAddBookmark;
    public JMenuItem menuItemBookmarksDelete;
    public JMenu menuTools;
    public JMenuItem menuItemDuplicates;
    public JMenuItem menuItemEmptyFinder;
    public JMenuItem menuItemJunk;
    public JMenuItem menuItemOperations;
    public JMenuItem menuItemRenamer;
    public JMenuItem menuItemSleep;
    public JMenuItem menuItemExternalTools;
    public JMenuItem menuItemPlexGenerator;
    public JMenuItem menuItem1;
    public JMenu menuJobs;
    public JMenuItem menuItemJobsManage;
    public JMenu menuSystem;
    public JMenuItem menuItemSettings;
    public JMenuItem menuItemAuthKeys;
    public JMenuItem menuItemHintKeys;
    public JMenuItem menuItemBlacklist;
    public JMenuItem menuItemWhitelist;
    public JMenu menuWindows;
    public JMenuItem menuItemMaximize;
    public JMenuItem menuItemMinimize;
    public JMenuItem menuItemRestore;
    public JMenuItem menuItemSplitHorizontal;
    public JMenuItem menuItemSplitVertical;
    public JMenu menuHelp;
    public JMenuItem menuItemControls;
    public JMenuItem menuItemDocumentation;
    public JMenuItem menuItemGettingStarted;
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
    public JPanel panelLibraries;
    public JPanel panelTop;
    public JPanel panelTopButtons;
    public JButton buttonNew;
    public JButton buttonCopy;
    public JButton buttonDelete;
    public JPanel panelHelp;
    public JLabel labelHelp;
    public JSplitPane splitPaneContent;
    public JScrollPane scrollPaneConfig;
    public JTable configItems;
    public JPanel panelOptions;
    public JPanel panelControls;
    public JPanel topOptions;
    public JPanel vSpacer0;
    public JPanel panelLibraryType;
    public JPanel hSpacer3;
    public JLabel labelLibaryType;
    public JPanel panelCardBox;
    public JPanel vSpacer3;
    public JSeparator separator13;
    public JPanel vSpacer4;
    public JPanel panelLibraryTypeCards;
    public JPanel panelGettingStartedCard;
    public JLabel labelOperationGettingStarted;
    public JPanel panelLibraryCard;
    public JPanel panelHintServerCard;
    public JPanel panelTargetsCard;
    public JPanel panelXCard;
    public JPanel panelYCard;
    public JTabbedPane tabbedPaneLibrarySpaces;
    public JPanel bibliographyTab;
    public JSplitPane librariesSplit;
    public JScrollPane scrollPaneLibraries;
    public JTable tableLibraries;
    public JPanel panelSources;
    public JLabel labelSpacer42;
    public JLabel labelSources;
    public JScrollPane scrollPane1;
    public JTable table1;
    public JPanel panel1;
    public JButton buttonAddOrigin;
    public JButton buttonOriginUp;
    public JButton buttonOriginDown;
    public JButton buttonRemoveOrigin;
    public JPanel panelBiblioButtons;
    public JButton buttonNewLibrary;
    public JPanel locationsTab;
    public JScrollPane scrollPaneLocations;
    public JTable tableLocations;
    public JPanel panelLocButtons;
    public JButton buttonNewLocation;
    public JPanel buttonBar;
    public JButton saveButton;
    public JButton cancelButton;
    public JPanel panelStatus;
    public JLabel labelStatusLeft;
    public JLabel labelStatusMiddle;
    public JLabel labelStatusRight;
    public JPopupMenu popupMenuBrowser;
    public JMenuItem popupMenuItemRefresh;
    public JMenuItem popupMenuItemCopy;
    public JMenuItem popupMenuItemCut;
    public JMenuItem popupMenuItemPaste;
    public JMenuItem popupMenuItemDelete;
    public JMenuItem popupMenuItemNewFolder;
    public JMenuItem popupMenuItemRename;
    public JMenuItem popupMenuItemTouch;
    public JPopupMenu popupMenuLog;
    public JMenuItem popupMenuItemFindNext;
    public JMenuItem popupMenuItemFind;
    public JMenuItem popupMenuItemTop;
    public JMenuItem popupMenuItemBottom;
    public JMenuItem popupMenuItemClear;
    public JCheckBoxMenuItem popupCheckBoxMenuItemWordWrap;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
