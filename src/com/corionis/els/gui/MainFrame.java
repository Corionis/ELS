package com.corionis.els.gui;

import javax.swing.border.*;
import javax.swing.event.*;

import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.browser.BrowserTableModel;
import com.corionis.els.gui.util.RotatedIcon;
import com.corionis.els.gui.util.SmartScroller;
import com.corionis.els.gui.util.TextIcon;
import com.corionis.els.gui.util.VerticalLabel;
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
 *  - JFormDesigner: https://www.formdev.com/jformdesigner/doc/ <br/>
 *  - GitHub project: https://github.com/JFormDesigner/FlatLaf <br/>
 * <br/>
 * Uses free components from FormDev: <br/>
 *  - FlatLaf, https://www.formdev.com/flatlaf/ <br/>
 *  - FlatLaf Themes: https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-intellij-themes <br/>
 *  - FlatLaf Extras: https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-extras <br/>
 * <br/>
 * See also: <br/>
 *
 *  Menu icon color:  #3592C4
 *  Menu icon size:   14x14 px
 *  Accent color:     #2675BF  see Preferences
 *
 *  Icon source:  https://www.iconsdb.com
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
//            context.preferences.initLookAndFeel();
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
        // TODO EXTEND+ Add other Tool checkForChanges() here
        if (context.libraries != null && context.libraries.checkForChanges())
            changes = true;
        else if (context.navigator.dialogJobs != null && context.navigator.dialogJobs.checkForChanges())
            changes = true;
        else if (context.navigator.dialogJunkRemover != null && context.navigator.dialogJunkRemover.checkForChanges())
            changes = true;
        else if (context.navigator.dialogOperations != null && context.navigator.dialogOperations.checkForChanges())
            changes = true;
        else if (context.navigator.dialogRenamer != null && context.navigator.dialogRenamer.checkForChanges())
            changes = true;
        else if (context.navigator.dialogSleep != null && context.navigator.dialogSleep.checkForChanges())
            changes = true;
        return changes;
    }

    private void changesGotoUnsaved()
    {
        // TODO EXTEND+ Add other Tool checkForChanges() here
        if (context.libraries != null && context.libraries.checkForChanges())
        {
            context.mainFrame.tabbedPaneMain.setSelectedIndex(1);
            context.mainFrame.saveButton.requestFocus();
        }
        else if (context.navigator.dialogJobs != null && context.navigator.dialogJobs.checkForChanges())
        {
            context.navigator.dialogJobs.setVisible(true);
            context.navigator.dialogJobs.toFront();
            context.navigator.dialogJobs.requestFocus();
            context.navigator.dialogJobs.toFront();
            context.navigator.dialogJobs.requestFocus();
            context.navigator.dialogJobs.saveButton.requestFocus();
        }
        else if (context.navigator.dialogJunkRemover != null && context.navigator.dialogJunkRemover.checkForChanges())
        {
            context.navigator.dialogJunkRemover.setVisible(true);
            context.navigator.dialogJunkRemover.toFront();
            context.navigator.dialogJunkRemover.requestFocus();
            context.navigator.dialogJunkRemover.toFront();
            context.navigator.dialogJunkRemover.requestFocus();
            context.navigator.dialogJunkRemover.saveButton.requestFocus();
        }
        else if (context.navigator.dialogOperations != null && context.navigator.dialogOperations.checkForChanges())
        {
            context.navigator.dialogOperations.setVisible(true);
            context.navigator.dialogOperations.toFront();
            context.navigator.dialogOperations.requestFocus();
            context.navigator.dialogOperations.toFront();
            context.navigator.dialogOperations.requestFocus();
            context.navigator.dialogOperations.buttonOperationSave.requestFocus();
        }
        else if (context.navigator.dialogRenamer != null && context.navigator.dialogRenamer.checkForChanges())
        {
            context.navigator.dialogRenamer.toFront();
            context.navigator.dialogRenamer.requestFocus();
            context.navigator.dialogRenamer.toFront();
            context.navigator.dialogRenamer.requestFocus();
            context.navigator.dialogRenamer.saveButton.requestFocus();
        }
        else if (context.navigator.dialogSleep != null && context.navigator.dialogSleep.checkForChanges())
        {
            context.navigator.dialogSleep.toFront();
            context.navigator.dialogSleep.requestFocus();
            context.navigator.dialogSleep.toFront();
            context.navigator.dialogSleep.requestFocus();
            context.navigator.dialogSleep.saveButton.requestFocus();
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

   private void cardShown(ComponentEvent e)
   {
        context.libraries.cardShown(e);
    }

    private void tabbedPaneMainStateChanged(ChangeEvent e)
    {
        labelStatusMiddle.setText(" ");
        int index = tabbedPaneMain.getSelectedIndex();
        if (index == 0)
        {
            context.mainFrame.menuItemNewFolder.setEnabled(true);
            context.mainFrame.menuItemRename.setEnabled(true);
            context.mainFrame.menuItemTouch.setEnabled(true);
            context.mainFrame.menuItemFind.setEnabled(true);
            context.mainFrame.menuItemFindNext.setEnabled(true);
            context.mainFrame.menuItemRefresh.setEnabled(true);
            context.mainFrame.menuItemAutoRefresh.setEnabled(true);
            context.mainFrame.menuItemShowHidden.setEnabled(true);
            context.mainFrame.menuItemWordWrap.setEnabled(true);

            if (context.browser != null)
                context.browser.selectPanelNumber(context.browser.lastPanelNumber);
        }
        else if (index == 1)
        {
            context.libraries.tabbedPaneLibrarySpacesStateChanged(null);

            context.mainFrame.menuItemNewFolder.setEnabled(false);
            context.mainFrame.menuItemRename.setEnabled(false);
            context.mainFrame.menuItemTouch.setEnabled(false);
            context.mainFrame.menuItemFind.setEnabled(false);
            context.mainFrame.menuItemFindNext.setEnabled(false);
            context.mainFrame.menuItemRefresh.setEnabled(false);
            context.mainFrame.menuItemAutoRefresh.setEnabled(false);
            context.mainFrame.menuItemShowHidden.setEnabled(false);
            context.mainFrame.menuItemWordWrap.setEnabled(false);

            if (context.browser != null)
                context.libraries.selectLastTab();
        }
    }

    private void thisWindowOpened(WindowEvent e) {
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
        menuItemClose = new JMenu();
        menuItemClosePublisher = new JMenuItem();
        menuItemCloseSubscriber = new JMenuItem();
        menuItemCloseHintKeys = new JMenuItem();
        menuItemCloseHintTracking = new JMenuItem();
        menuItemGenerate = new JMenuItem();
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
        menuItemAuthKeys = new JMenuItem();
        menuItemHintKeys = new JMenuItem();
        menuItemBlacklist = new JMenuItem();
        menuItemWhitelist = new JMenuItem();
        menuItemSettings = new JMenuItem();
        menuWindows = new JMenu();
        menuItemMaximize = new JMenuItem();
        menuItemMinimize = new JMenuItem();
        menuItemRestore = new JMenuItem();
        menuItemSplitHorizontal = new JMenuItem();
        menuItemSplitVertical = new JMenuItem();
        menuHelp = new JMenu();
        menuItemControls = new JMenuItem();
        menuItemDiscussions = new JMenuItem();
        menuItemDocumentation = new JMenuItem();
        menuItemGettingStarted = new JMenuItem();
        menuItemGitHubProject = new JMenuItem();
        menuItemIssue = new JMenuItem();
        menuItemChangelist = new JMenuItem();
        menuItemReleaseNotes = new JMenuItem();
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
        panelLibsTop = new JPanel();
        panelTopButtons = new JPanel();
        hSpacer7 = new JPanel(null);
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        panelHelp = new JPanel();
        labelLibrariesHelp = new JLabel();
        splitPaneLibs = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        librariesConfigItems = new JTable();
        panelOptions = new JPanel();
        panelControls = new JPanel();
        topType = new JPanel();
        vSpacer0 = new JPanel(null);
        panelLibraryType = new JPanel();
        hSpacer3 = new JPanel(null);
        labelLibaryType = new JLabel();
        panelCardBox = new JPanel();
        vSpacer3 = new JPanel(null);
        separator13 = new JSeparator();
        vSpacer4 = new JPanel(null);
        tabbedPaneLibrarySpaces = new JTabbedPane();
        generalTab = new JPanel();
        panelGettingStartedCard = new JPanel();
        labelOperationGettingStarted = new JLabel();
        panelLibraryCard = new JPanel();
        hSpacer4 = new JPanel(null);
        hSpacer6 = new JPanel(null);
        vSpacer6 = new JPanel(null);
        hSpacer5 = new JPanel(null);
        labelKey = new JLabel();
        textFieldKey = new JTextField();
        vSpacer33 = new JPanel(null);
        buttonLibraryGenerateKey = new JButton();
        labelHost = new JLabel();
        textFieldHost = new JTextField();
        vSpacer34 = new JPanel(null);
        labelListen = new JLabel();
        textFieldListen = new JTextField();
        vSpacer35 = new JPanel(null);
        labelTimeout = new JLabel();
        textFieldTimeout = new JTextField();
        vSpacer36 = new JPanel(null);
        labelFlavor = new JLabel();
        comboBoxFlavor = new JComboBox<>();
        vSpacer37 = new JPanel(null);
        labelCase = new JLabel();
        checkBoxCase = new JCheckBox();
        vSpacer38 = new JPanel(null);
        labelTempDated = new JLabel();
        checkBoxTempDated = new JCheckBox();
        vSpacer42 = new JPanel(null);
        labelTempLocation = new JLabel();
        textFieldTempLocation = new JTextField();
        vSpacer39 = new JPanel(null);
        buttonLibrarySelectTempLocation = new JButton();
        labelTerminalAllosed = new JLabel();
        checkBoxTerminalAllowed = new JCheckBox();
        vSpacer40 = new JPanel(null);
        labelIgnores = new JLabel();
        panelLibrariesIgnorePatternsBox = new JPanel();
        scrollPaneLibrariesIgnorePatterns = new JScrollPane();
        listLibrariesIgnorePatterns = new JList<>();
        panelLibrariesIgnorePatternsButtons = new JPanel();
        buttonLibrariesAddIgnore = new JButton();
        buttonLibrariesRemoveIgnore = new JButton();
        vSpacer41 = new JPanel(null);
        panelHintServerCard = new JPanel();
        panelTargetsCard = new JPanel();
        panelXCard = new JPanel();
        panelYCard = new JPanel();
        locationsTab = new JPanel();
        scrollPaneLocations = new JScrollPane();
        tableLocations = new JTable();
        panelLocButtons = new JPanel();
        buttonAddLocation = new JButton();
        buttonRemoveLocation = new JButton();
        bibliographyTab = new JPanel();
        splitPanelBiblio = new JSplitPane();
        panelBiblioLibraries = new JPanel();
        labelBiblioLibraries = new JLabel();
        scrollPaneBiblioLibraries = new JScrollPane();
        tableBiblioLibraries = new JTable();
        panelSources = new JPanel();
        panelSourcesTop = new JPanel();
        labelSpacer42 = new JLabel();
        labelSources = new JLabel();
        scrollPaneSources = new JScrollPane();
        listSources = new JList();
        panelSourceButtons = new JPanel();
        buttonAddSource = new JButton();
        buttonAddMultiSource = new JButton();
        buttonUpSource = new JButton();
        buttonDownSource = new JButton();
        buttonRemoveSource = new JButton();
        panelBiblioButtons = new JPanel();
        buttonAddLibrary = new JButton();
        buttonRemoveLibrary = new JButton();
        buttonBarLibs = new JPanel();
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
        setTitle("Corionis ELS Navigator");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
            @Override
            public void windowOpened(WindowEvent e) {
                thisWindowOpened(e);
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
                menuItemOpenPublisher.setIcon(new ImageIcon(getClass().getResource("/open-publisher.png")));
                menuFile.add(menuItemOpenPublisher);

                //---- menuItemOpenSubscriber ----
                menuItemOpenSubscriber.setText(context.cfg.gs("Navigator.menu.OpenSubscriber.text"));
                menuItemOpenSubscriber.setMnemonic(context.cfg.gs("Navigator.menu.OpenSubscriber.mnemonic").charAt(0));
                menuItemOpenSubscriber.setIcon(new ImageIcon(getClass().getResource("/open-subscriber.png")));
                menuFile.add(menuItemOpenSubscriber);

                //---- menuItemOpenHintKeys ----
                menuItemOpenHintKeys.setText(context.cfg.gs("Navigator.menu.OpenHintKeys.text"));
                menuItemOpenHintKeys.setSelected(true);
                menuItemOpenHintKeys.setMnemonic(context.cfg.gs("Navigator.menu.OpenHintKeys.mnemonic").charAt(0));
                menuItemOpenHintKeys.setIcon(new ImageIcon(getClass().getResource("/open-hint-keys.png")));
                menuFile.add(menuItemOpenHintKeys);

                //---- menuItemOpenHintTracking ----
                menuItemOpenHintTracking.setText(context.cfg.gs("Navigator.menuItemOpenHintTracking.text"));
                menuItemOpenHintTracking.setSelected(true);
                menuItemOpenHintTracking.setMnemonic(context.cfg.gs("Navigator.menuItemOpenHintTracking.mnemonic_2").charAt(0));
                menuItemOpenHintTracking.setDisplayedMnemonicIndex(Integer.parseInt(context.cfg.gs("Navigator.menuItemOpenHintTracking.displayedMnemonicIndex")));
                menuItemOpenHintTracking.setIcon(new ImageIcon(getClass().getResource("/open-hint-tracking.png")));
                menuFile.add(menuItemOpenHintTracking);
                menuFile.addSeparator();

                //======== menuItemClose ========
                {
                    menuItemClose.setText(context.cfg.gs("Navigator.menuItemClose.text"));
                    menuItemClose.setIcon(new ImageIcon(getClass().getResource("/close.png")));
                    menuItemClose.setMnemonic(context.cfg.gs("Navigator.menuItemClose.mnemonic").charAt(0));

                    //---- menuItemClosePublisher ----
                    menuItemClosePublisher.setText(context.cfg.gs("Navigator.menuItemClosePublisher.text"));
                    menuItemClosePublisher.setMnemonic(context.cfg.gs("Navigator.menuItemClosePublisher.mnemonic").charAt(0));
                    menuItemClose.add(menuItemClosePublisher);

                    //---- menuItemCloseSubscriber ----
                    menuItemCloseSubscriber.setText(context.cfg.gs("Navigator.menuItemCloseSubscriber.text"));
                    menuItemCloseSubscriber.setMnemonic(context.cfg.gs("Navigator.menuItemCloseSubscriber.mnemonic").charAt(0));
                    menuItemClose.add(menuItemCloseSubscriber);

                    //---- menuItemCloseHintKeys ----
                    menuItemCloseHintKeys.setText(context.cfg.gs("Navigator.menuItemCloseHintKeys.text"));
                    menuItemCloseHintKeys.setMnemonic(context.cfg.gs("Navigator.menuItemCloseHintKeys.mnemonic").charAt(0));
                    menuItemClose.add(menuItemCloseHintKeys);

                    //---- menuItemCloseHintTracking ----
                    menuItemCloseHintTracking.setText(context.cfg.gs("Navigator.menuItemCloseHintTracking.text"));
                    menuItemCloseHintTracking.setMnemonic(context.cfg.gs("Navigator.menuItemCloseHintTracking.mnemonic").charAt(0));
                    menuItemCloseHintTracking.setDisplayedMnemonicIndex(Integer.parseInt(context.cfg.gs("Navigator.menuItemCloseHintTracking.displayedMnemonicIndex")));
                    menuItemClose.add(menuItemCloseHintTracking);
                }
                menuFile.add(menuItemClose);
                menuFile.addSeparator();

                //---- menuItemGenerate ----
                menuItemGenerate.setText(context.cfg.gs("Navigator.menuItemGenerate.text"));
                menuItemGenerate.setMnemonic(context.cfg.gs("Navigator.menuItemGenerate.mnemonic").charAt(0));
                menuItemGenerate.setIcon(new ImageIcon(getClass().getResource("/generate.png")));
                menuFile.add(menuItemGenerate);

                //---- menuItemSaveLayout ----
                menuItemSaveLayout.setText(context.cfg.gs("Navigator.menu.SaveLayout.text"));
                menuItemSaveLayout.setMnemonic(context.cfg.gs("Navigator.menu.SaveLayout.mnemonic_3").charAt(0));
                menuItemSaveLayout.setIcon(new ImageIcon(getClass().getResource("/save-layout.png")));
                menuFile.add(menuItemSaveLayout);
                menuFile.addSeparator();

                //---- menuItemQuitTerminate ----
                menuItemQuitTerminate.setText(context.cfg.gs("Navigator.menu.QuitTerminate.text"));
                menuItemQuitTerminate.setMnemonic(context.cfg.gs("Navigator.menuItemQuitTerminate.mnemonic").charAt(0));
                menuItemQuitTerminate.setDisplayedMnemonicIndex(12);
                menuItemQuitTerminate.setIcon(new ImageIcon(getClass().getResource("/quit-and-stop.png")));
                menuFile.add(menuItemQuitTerminate);

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(context.cfg.gs("Navigator.menu.Quit.text"));
                menuItemFileQuit.setMnemonic(context.cfg.gs("Navigator.menu.Quit.mnemonic").charAt(0));
                menuItemFileQuit.setIcon(new ImageIcon(getClass().getResource("/quit.png")));
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
                menuItemCopy.setIcon(new ImageIcon(getClass().getResource("/copy.png")));
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(context.cfg.gs("Navigator.menu.Cut.text"));
                menuItemCut.setMnemonic(context.cfg.gs("Navigator.menu.Cut.mnemonic").charAt(0));
                menuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
                menuItemCut.setIcon(new ImageIcon(getClass().getResource("/cut.png")));
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(context.cfg.gs("Navigator.menu.Paste.text"));
                menuItemPaste.setMnemonic(context.cfg.gs("Navigator.menu.Paste.mnemonic").charAt(0));
                menuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
                menuItemPaste.setIcon(new ImageIcon(getClass().getResource("/paste.png")));
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemDelete ----
                menuItemDelete.setText(context.cfg.gs("Navigator.menu.Delete.text"));
                menuItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                menuItemDelete.setMnemonic(context.cfg.gs("Navigator.menu.Delete.mnemonic").charAt(0));
                menuItemDelete.setIcon(new ImageIcon(getClass().getResource("/delete-x.png")));
                menuEdit.add(menuItemDelete);
                menuEdit.addSeparator();

                //---- menuItemNewFolder ----
                menuItemNewFolder.setText(context.cfg.gs("Navigator.menu.New.folder.text"));
                menuItemNewFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
                menuItemNewFolder.setMnemonic(context.cfg.gs("Navigator.menu.New.folder.mnemonic").charAt(0));
                menuItemNewFolder.setIcon(new ImageIcon(getClass().getResource("/new-folder.png")));
                menuEdit.add(menuItemNewFolder);

                //---- menuItemRename ----
                menuItemRename.setText(context.cfg.gs("Navigator.menu.Rename.text"));
                menuItemRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
                menuItemRename.setMnemonic(context.cfg.gs("Navigator.menu.Rename.mnemonic").charAt(0));
                menuItemRename.setIcon(new ImageIcon(getClass().getResource("/rename.png")));
                menuEdit.add(menuItemRename);

                //---- menuItemTouch ----
                menuItemTouch.setText(context.cfg.gs("Navigator.menu.Touch.text"));
                menuItemTouch.setMnemonic(context.cfg.gs("Navigator.menu.Touch.mnemonic").charAt(0));
                menuItemTouch.setIcon(new ImageIcon(getClass().getResource("/touch.png")));
                menuItemTouch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));
                menuEdit.add(menuItemTouch);
                menuEdit.addSeparator();

                //---- menuItemFind ----
                menuItemFind.setText(context.cfg.gs("Navigator.menu.Find.text"));
                menuItemFind.setMnemonic(context.cfg.gs("Navigator.menu.Find.mnemonic").charAt(0));
                menuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
                menuItemFind.setIcon(new ImageIcon(getClass().getResource("/find.png")));
                menuEdit.add(menuItemFind);

                //---- menuItemFindNext ----
                menuItemFindNext.setText(context.cfg.gs("Navigator.menuItemFindNext.text"));
                menuItemFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
                menuItemFindNext.setMnemonic(context.cfg.gs("Navigator.menuItemFindNext.mnemonic").charAt(0));
                menuItemFindNext.setIcon(new ImageIcon(getClass().getResource("/find-next.png")));
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
                menuItemProgress.setIcon(new ImageIcon(getClass().getResource("/progress.png")));
                menuView.add(menuItemProgress);

                //---- menuItemRefresh ----
                menuItemRefresh.setText(context.cfg.gs("Navigator.menu.Refresh.text"));
                menuItemRefresh.setMnemonic(context.cfg.gs("Navigator.menuItemRefresh.mnemonic").charAt(0));
                menuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
                menuItemRefresh.setIcon(new ImageIcon(getClass().getResource("/refresh.png")));
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
                menuItemShowHidden.setIcon(null);
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
                menuItemAddBookmark.setIcon(new ImageIcon(getClass().getResource("/bookmark.png")));
                menuBookmarks.add(menuItemAddBookmark);

                //---- menuItemBookmarksDelete ----
                menuItemBookmarksDelete.setText(context.cfg.gs("Navigator.menu.BookmarksManage.text"));
                menuItemBookmarksDelete.setMnemonic(context.cfg.gs("Navigator.menu.BookmarksManage.mnemonic").charAt(0));
                menuItemBookmarksDelete.setIcon(new ImageIcon(getClass().getResource("/bookmark-delete.png")));
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
                menuTools.addSeparator();

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
                menuItemExternalTools.setToolTipText(context.cfg.gs("Z.not.implemented.yet"));
                menuTools.add(menuItemExternalTools);

                //---- menuItemPlexGenerator ----
                menuItemPlexGenerator.setText(context.cfg.gs("Navigator.menu.PlexGenerator.text"));
                menuItemPlexGenerator.setEnabled(false);
                menuItemPlexGenerator.setMargin(new Insets(2, 18, 2, 2));
                menuItemPlexGenerator.setToolTipText(context.cfg.gs("Z.not.implemented.yet"));
                menuTools.add(menuItemPlexGenerator);

                //---- menuItem1 ----
                menuItem1.setText("Handbrake");
                menuItem1.setMargin(new Insets(2, 18, 2, 2));
                menuItem1.setEnabled(false);
                menuItem1.setToolTipText(context.cfg.gs("Z.not.implemented.yet"));
                menuTools.add(menuItem1);
            }
            menuBarMain.add(menuTools);

            //======== menuJobs ========
            {
                menuJobs.setText(context.cfg.gs("Navigator.menu.Jobs.text"));
                menuJobs.setMnemonic(context.cfg.gs("Navigator.menu.Jobs.mnemonic").charAt(0));
                menuJobs.setIcon(null);

                //---- menuItemJobsManage ----
                menuItemJobsManage.setText(context.cfg.gs("Navigator.menu.JobsManage.text"));
                menuItemJobsManage.setMnemonic(context.cfg.gs("Navigator.menu.JobsManage.mnemonic").charAt(0));
                menuItemJobsManage.setIcon(new ImageIcon(getClass().getResource("/jobs-manage.png")));
                menuJobs.add(menuItemJobsManage);
                menuJobs.addSeparator();
            }
            menuBarMain.add(menuJobs);

            //======== menuSystem ========
            {
                menuSystem.setText("System");
                menuSystem.setMnemonic(context.cfg.gs("Navigator.menuSystem.mnemonic").charAt(0));

                //---- menuItemAuthKeys ----
                menuItemAuthKeys.setText(context.cfg.gs("Navigator.menuItemAuthKeys.text"));
                menuItemAuthKeys.setMnemonic(context.cfg.gs("Navigator.menuItemAuthKeys.mnemonic").charAt(0));
                menuItemAuthKeys.setIcon(new ImageIcon(getClass().getResource("/auth-keys.png")));
                menuSystem.add(menuItemAuthKeys);

                //---- menuItemHintKeys ----
                menuItemHintKeys.setText(context.cfg.gs("Navigator.menuItemHintKeys.text"));
                menuItemHintKeys.setMnemonic(context.cfg.gs("Navigator.menuItemHintKeys.mnemonic").charAt(0));
                menuItemHintKeys.setIcon(new ImageIcon(getClass().getResource("/hint-keys.png")));
                menuSystem.add(menuItemHintKeys);
                menuSystem.addSeparator();

                //---- menuItemBlacklist ----
                menuItemBlacklist.setText(context.cfg.gs("Navigator.menuItemBlacklist.text"));
                menuItemBlacklist.setMnemonic(context.cfg.gs("Navigator.menuItemBlacklist.mnemonic").charAt(0));
                menuItemBlacklist.setIcon(new ImageIcon(getClass().getResource("/blacklist.png")));
                menuSystem.add(menuItemBlacklist);

                //---- menuItemWhitelist ----
                menuItemWhitelist.setText(context.cfg.gs("Navigator.menuItemWhitelist.text"));
                menuItemWhitelist.setMnemonic(context.cfg.gs("Navigator.menuItemWhitelist.mnemonic").charAt(0));
                menuItemWhitelist.setIcon(new ImageIcon(getClass().getResource("/whitelist.png")));
                menuSystem.add(menuItemWhitelist);
                menuSystem.addSeparator();

                //---- menuItemSettings ----
                menuItemSettings.setText(context.cfg.gs("Navigator.menu.Settings.text"));
                menuItemSettings.setMnemonic(context.cfg.gs("Navigator.menu.Settings.mnemonic").charAt(0));
                menuItemSettings.setIcon(new ImageIcon(getClass().getResource("/settings.png")));
                menuSystem.add(menuItemSettings);
            }
            menuBarMain.add(menuSystem);

            //======== menuWindows ========
            {
                menuWindows.setText(context.cfg.gs("Navigator.menu.Windows.text"));
                menuWindows.setMnemonic(context.cfg.gs("Navigator.menu.Windows.mnemonic").charAt(0));

                //---- menuItemMaximize ----
                menuItemMaximize.setText(context.cfg.gs("Navigator.menu.Maximize.text"));
                menuItemMaximize.setMnemonic(context.cfg.gs("Navigator.menu.Maximize.mnemonic").charAt(0));
                menuItemMaximize.setIcon(new ImageIcon(getClass().getResource("/maximize.png")));
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(context.cfg.gs("Navigator.menu.Minimize.text"));
                menuItemMinimize.setMnemonic(context.cfg.gs("Navigator.menu.Minimize.mnemonic").charAt(0));
                menuItemMinimize.setIcon(new ImageIcon(getClass().getResource("/minimize.png")));
                menuWindows.add(menuItemMinimize);

                //---- menuItemRestore ----
                menuItemRestore.setText(context.cfg.gs("Navigator.menu.Restore.text"));
                menuItemRestore.setMnemonic(context.cfg.gs("Navigator.menu.Restore.mnemonic").charAt(0));
                menuItemRestore.setIcon(new ImageIcon(getClass().getResource("/restore.png")));
                menuWindows.add(menuItemRestore);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(context.cfg.gs("Navigator.menu.SplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(context.cfg.gs("Navigator.menu.SplitHorizontal.mnemonic").charAt(0));
                menuItemSplitHorizontal.setIcon(new ImageIcon(getClass().getResource("/horizontal.png")));
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(context.cfg.gs("Navigator.menu.SplitVertical.text"));
                menuItemSplitVertical.setMnemonic(context.cfg.gs("Navigator.menu.SplitVertical.mnemonic").charAt(0));
                menuItemSplitVertical.setIcon(new ImageIcon(getClass().getResource("/vertical.png")));
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
                menuItemControls.setIcon(new ImageIcon(getClass().getResource("/controls.png")));
                menuHelp.add(menuItemControls);

                //---- menuItemDiscussions ----
                menuItemDiscussions.setText(context.cfg.gs("Navigator.menuItemDiscussions.text"));
                menuItemDiscussions.setMnemonic(context.cfg.gs("Navigator.menuItemDiscussions.mnemonic").charAt(0));
                menuItemDiscussions.setIcon(new ImageIcon(getClass().getResource("/discuss.png")));
                menuHelp.add(menuItemDiscussions);

                //---- menuItemDocumentation ----
                menuItemDocumentation.setText(context.cfg.gs("Navigator.menu.Documentation.text"));
                menuItemDocumentation.setMnemonic(context.cfg.gs("Navigator.menu.Documentation.mnemonic").charAt(0));
                menuItemDocumentation.setIcon(new ImageIcon(getClass().getResource("/external-link.png")));
                menuHelp.add(menuItemDocumentation);

                //---- menuItemGettingStarted ----
                menuItemGettingStarted.setText(context.cfg.gs("Navigator.menuItemGettingStarted.text"));
                menuItemGettingStarted.setIcon(new ImageIcon(getClass().getResource("/getting-started.png")));
                menuItemGettingStarted.setMnemonic(context.cfg.gs("Navigator.menuItemGettingStarted.mnemonic_2").charAt(0));
                menuHelp.add(menuItemGettingStarted);

                //---- menuItemGitHubProject ----
                menuItemGitHubProject.setText(context.cfg.gs("Navigator.menu.GitHubProject.text"));
                menuItemGitHubProject.setMnemonic(context.cfg.gs("Navigator.menuItemGitHubProject.mnemonic").charAt(0));
                menuItemGitHubProject.setIcon(new ImageIcon(getClass().getResource("/github.png")));
                menuHelp.add(menuItemGitHubProject);

                //---- menuItemIssue ----
                menuItemIssue.setText(context.cfg.gs("Navigator.menuItemIssue.text"));
                menuItemIssue.setMnemonic(context.cfg.gs("Navigator.menuItemIssue.mnemonic").charAt(0));
                menuItemIssue.setIcon(new ImageIcon(getClass().getResource("/issue.png")));
                menuItemIssue.setDisplayedMnemonicIndex(Integer.parseInt(context.cfg.gs("Navigator.menuItemIssue.displayedMnemonicIndex")));
                menuHelp.add(menuItemIssue);
                menuHelp.addSeparator();

                //---- menuItemChangelist ----
                menuItemChangelist.setText(context.cfg.gs("Navigator.menuItemChangelist.text"));
                menuItemChangelist.setMnemonic(context.cfg.gs("Navigator.menuItemChangelist.mnemonic").charAt(0));
                menuItemChangelist.setIcon(new ImageIcon(getClass().getResource("/changes.png")));
                menuHelp.add(menuItemChangelist);

                //---- menuItemReleaseNotes ----
                menuItemReleaseNotes.setText(context.cfg.gs("Navigator.menuItemReleaseNotes.text"));
                menuItemReleaseNotes.setIcon(new ImageIcon(getClass().getResource("/release-notes.png")));
                menuItemReleaseNotes.setMnemonic(context.cfg.gs("Navigator.menuItemReleaseNotes.mnemonic").charAt(0));
                menuHelp.add(menuItemReleaseNotes);
                menuHelp.addSeparator();

                //---- menuItemUpdates ----
                menuItemUpdates.setText(context.cfg.gs("Navigator.menuItemUpdates.text"));
                menuItemUpdates.setMnemonic(context.cfg.gs("Navigator.menuItemUpdates.mnemonic").charAt(0));
                menuItemUpdates.setIcon(new ImageIcon(getClass().getResource("/updates.png")));
                menuHelp.add(menuItemUpdates);

                //---- menuItemAbout ----
                menuItemAbout.setText(context.cfg.gs("Navigator.menu.About.text"));
                menuItemAbout.setMnemonic(context.cfg.gs("Navigator.menu.About.mnemonic").charAt(0));
                menuItemAbout.setIcon(new ImageIcon(getClass().getResource("/about.png")));
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
                tabbedPaneMain.setName("main");
                tabbedPaneMain.addChangeListener(e -> tabbedPaneMainStateChanged(e));

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
                                tabbedPaneBrowserOne.setName("browserOne");

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
                                tabbedPaneBrowserTwo.setName("browserTwo");

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
                    panelLibraries.setFocusCycleRoot(true);
                    panelLibraries.setLayout(new BorderLayout());

                    //======== panelLibsTop ========
                    {
                        panelLibsTop.setMinimumSize(new Dimension(140, 38));
                        panelLibsTop.setPreferredSize(new Dimension(614, 38));
                        panelLibsTop.setLayout(new BorderLayout());

                        //======== panelTopButtons ========
                        {
                            panelTopButtons.setMinimumSize(new Dimension(140, 38));
                            panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                            //---- hSpacer7 ----
                            hSpacer7.setPreferredSize(new Dimension(1, 10));
                            hSpacer7.setMinimumSize(new Dimension(1, 10));
                            panelTopButtons.add(hSpacer7);

                            //---- buttonNew ----
                            buttonNew.setText(context.cfg.gs("Navigator.buttonNew.text"));
                            buttonNew.setMnemonic(context.cfg.gs("Navigator.buttonNew.mnemonic").charAt(0));
                            buttonNew.setToolTipText(context.cfg.gs("Navigator.buttonNew.toolTipText"));
                            buttonNew.setPreferredSize(new Dimension(78, 31));
                            buttonNew.setMinimumSize(new Dimension(78, 31));
                            buttonNew.setMaximumSize(new Dimension(78, 31));
                            panelTopButtons.add(buttonNew);

                            //---- buttonCopy ----
                            buttonCopy.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                            buttonCopy.setMnemonic(context.cfg.gs("Navigator.buttonCopy.mnemonic").charAt(0));
                            buttonCopy.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                            panelTopButtons.add(buttonCopy);

                            //---- buttonDelete ----
                            buttonDelete.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                            buttonDelete.setMnemonic(context.cfg.gs("Navigator.buttonDelete.mnemonic").charAt(0));
                            buttonDelete.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
                            panelTopButtons.add(buttonDelete);
                        }
                        panelLibsTop.add(panelTopButtons, BorderLayout.WEST);

                        //======== panelHelp ========
                        {
                            panelHelp.setPreferredSize(new Dimension(40, 38));
                            panelHelp.setMinimumSize(new Dimension(0, 38));
                            panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                            //---- labelLibrariesHelp ----
                            labelLibrariesHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                            labelLibrariesHelp.setPreferredSize(new Dimension(32, 30));
                            labelLibrariesHelp.setMinimumSize(new Dimension(32, 30));
                            labelLibrariesHelp.setMaximumSize(new Dimension(32, 30));
                            labelLibrariesHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            labelLibrariesHelp.setIconTextGap(0);
                            panelHelp.add(labelLibrariesHelp);
                        }
                        panelLibsTop.add(panelHelp, BorderLayout.EAST);
                    }
                    panelLibraries.add(panelLibsTop, BorderLayout.NORTH);

                    //======== splitPaneLibs ========
                    {
                        splitPaneLibs.setDividerLocation(142);
                        splitPaneLibs.setLastDividerLocation(142);
                        splitPaneLibs.setResizeWeight(1.0);

                        //======== scrollPaneConfig ========
                        {
                            scrollPaneConfig.setMinimumSize(new Dimension(140, 16));
                            scrollPaneConfig.setPreferredSize(new Dimension(142, 146));
                            scrollPaneConfig.setName("librariesConfigScroll");

                            //---- librariesConfigItems ----
                            librariesConfigItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            librariesConfigItems.setShowVerticalLines(false);
                            librariesConfigItems.setFillsViewportHeight(true);
                            librariesConfigItems.setShowHorizontalLines(false);
                            librariesConfigItems.setName("librariesConfig");
                            scrollPaneConfig.setViewportView(librariesConfigItems);
                        }
                        splitPaneLibs.setLeftComponent(scrollPaneConfig);

                        //======== panelOptions ========
                        {
                            panelOptions.setMinimumSize(new Dimension(0, 78));
                            panelOptions.setLayout(new BorderLayout());

                            //======== panelControls ========
                            {
                                panelControls.setLayout(new BorderLayout());

                                //======== topType ========
                                {
                                    topType.setEnabled(false);
                                    topType.setVisible(false);
                                    topType.setLayout(new BorderLayout());

                                    //---- vSpacer0 ----
                                    vSpacer0.setPreferredSize(new Dimension(10, 2));
                                    vSpacer0.setMinimumSize(new Dimension(10, 2));
                                    vSpacer0.setMaximumSize(new Dimension(10, 2));
                                    topType.add(vSpacer0, BorderLayout.NORTH);

                                    //======== panelLibraryType ========
                                    {
                                        panelLibraryType.setLayout(new BoxLayout(panelLibraryType, BoxLayout.X_AXIS));

                                        //---- hSpacer3 ----
                                        hSpacer3.setPreferredSize(new Dimension(4, 10));
                                        hSpacer3.setMinimumSize(new Dimension(4, 12));
                                        hSpacer3.setMaximumSize(new Dimension(4, 32767));
                                        panelLibraryType.add(hSpacer3);

                                        //---- labelLibaryType ----
                                        labelLibaryType.setText("Library");
                                        labelLibaryType.setMaximumSize(new Dimension(110, 16));
                                        labelLibaryType.setFont(labelLibaryType.getFont().deriveFont(labelLibaryType.getFont().getStyle() | Font.BOLD, labelLibaryType.getFont().getSize() + 1f));
                                        labelLibaryType.setPreferredSize(new Dimension(110, 16));
                                        labelLibaryType.setMinimumSize(new Dimension(110, 16));
                                        panelLibraryType.add(labelLibaryType);
                                    }
                                    topType.add(panelLibraryType, BorderLayout.WEST);

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
                                    }
                                    topType.add(panelCardBox, BorderLayout.SOUTH);
                                }
                                panelControls.add(topType, BorderLayout.NORTH);
                            }
                            panelOptions.add(panelControls, BorderLayout.NORTH);

                            //======== tabbedPaneLibrarySpaces ========
                            {

                                //======== generalTab ========
                                {
                                    generalTab.setLayout(new CardLayout());

                                    //======== panelGettingStartedCard ========
                                    {
                                        panelGettingStartedCard.setLayout(new BorderLayout());

                                        //---- labelOperationGettingStarted ----
                                        labelOperationGettingStarted.setText(context.cfg.gs("Navigator.labelLibrariesGettingStarted.text"));
                                        labelOperationGettingStarted.setFont(labelOperationGettingStarted.getFont().deriveFont(labelOperationGettingStarted.getFont().getStyle() | Font.BOLD));
                                        labelOperationGettingStarted.setHorizontalAlignment(SwingConstants.CENTER);
                                        panelGettingStartedCard.add(labelOperationGettingStarted, BorderLayout.CENTER);
                                    }
                                    generalTab.add(panelGettingStartedCard, "cardGettingStarted");

                                    //======== panelLibraryCard ========
                                    {
                                        panelLibraryCard.setLayout(new GridBagLayout());
                                        ((GridBagLayout)panelLibraryCard.getLayout()).columnWidths = new int[] {0, 190, 16, 0, 24};
                                        ((GridBagLayout)panelLibraryCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                        ((GridBagLayout)panelLibraryCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                        //---- hSpacer4 ----
                                        hSpacer4.setMinimumSize(new Dimension(0, 0));
                                        hSpacer4.setPreferredSize(new Dimension(154, 10));
                                        panelLibraryCard.add(hSpacer4, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- hSpacer6 ----
                                        hSpacer6.setMinimumSize(new Dimension(0, 0));
                                        hSpacer6.setPreferredSize(new Dimension(240, 10));
                                        panelLibraryCard.add(hSpacer6, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer6 ----
                                        vSpacer6.setPreferredSize(new Dimension(10, 8));
                                        vSpacer6.setMinimumSize(new Dimension(2, 1));
                                        vSpacer6.setMaximumSize(new Dimension(32767, 8));
                                        panelLibraryCard.add(vSpacer6, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- hSpacer5 ----
                                        hSpacer5.setMinimumSize(new Dimension(0, 0));
                                        hSpacer5.setPreferredSize(new Dimension(154, 10));
                                        panelLibraryCard.add(hSpacer5, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelKey ----
                                        labelKey.setText(context.cfg.gs("Navigator.labelKey.text"));
                                        panelLibraryCard.add(labelKey, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- textFieldKey ----
                                        textFieldKey.setPreferredSize(new Dimension(240, 30));
                                        textFieldKey.setName("key");
                                        textFieldKey.setToolTipText(context.cfg.gs("Libraries.textFieldKey.toolTipText"));
                                        panelLibraryCard.add(textFieldKey, new GridBagConstraints(1, 1, 3, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer33 ----
                                        vSpacer33.setMinimumSize(new Dimension(10, 30));
                                        vSpacer33.setPreferredSize(new Dimension(20, 30));
                                        vSpacer33.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer33, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- buttonLibraryGenerateKey ----
                                        buttonLibraryGenerateKey.setText("...");
                                        buttonLibraryGenerateKey.setFont(buttonLibraryGenerateKey.getFont().deriveFont(buttonLibraryGenerateKey.getFont().getStyle() | Font.BOLD));
                                        buttonLibraryGenerateKey.setMaximumSize(new Dimension(32, 24));
                                        buttonLibraryGenerateKey.setMinimumSize(new Dimension(32, 24));
                                        buttonLibraryGenerateKey.setPreferredSize(new Dimension(32, 24));
                                        buttonLibraryGenerateKey.setVerticalTextPosition(SwingConstants.TOP);
                                        buttonLibraryGenerateKey.setIconTextGap(0);
                                        buttonLibraryGenerateKey.setHorizontalTextPosition(SwingConstants.LEADING);
                                        buttonLibraryGenerateKey.setActionCommand("generateUUID");
                                        buttonLibraryGenerateKey.setToolTipText(context.cfg.gs("Navigator.buttonLibraryGenerateKey.toolTipText"));
                                        buttonLibraryGenerateKey.setName("generate");
                                        panelLibraryCard.add(buttonLibraryGenerateKey, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                            new Insets(0, 0, 4, 0), 0, 0));

                                        //---- labelHost ----
                                        labelHost.setText(context.cfg.gs("Navigator.labelHost.text"));
                                        panelLibraryCard.add(labelHost, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- textFieldHost ----
                                        textFieldHost.setName("host");
                                        textFieldHost.setToolTipText(context.cfg.gs("Libraries.textFieldHost.toolTipText"));
                                        panelLibraryCard.add(textFieldHost, new GridBagConstraints(1, 2, 3, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer34 ----
                                        vSpacer34.setMinimumSize(new Dimension(10, 30));
                                        vSpacer34.setPreferredSize(new Dimension(20, 30));
                                        vSpacer34.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer34, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelListen ----
                                        labelListen.setText(context.cfg.gs("Navigator.labelListen.text"));
                                        panelLibraryCard.add(labelListen, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- textFieldListen ----
                                        textFieldListen.setName("listen");
                                        textFieldListen.setToolTipText(context.cfg.gs("Libraries.textFieldListen.toolTipText"));
                                        panelLibraryCard.add(textFieldListen, new GridBagConstraints(1, 3, 3, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer35 ----
                                        vSpacer35.setMinimumSize(new Dimension(10, 30));
                                        vSpacer35.setPreferredSize(new Dimension(20, 30));
                                        vSpacer35.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer35, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelTimeout ----
                                        labelTimeout.setText(context.cfg.gs("Navigator.labelTimeout.text"));
                                        panelLibraryCard.add(labelTimeout, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- textFieldTimeout ----
                                        textFieldTimeout.setName("timeout");
                                        textFieldTimeout.setPreferredSize(new Dimension(104, 30));
                                        textFieldTimeout.setMinimumSize(new Dimension(101104, 30));
                                        textFieldTimeout.setToolTipText(context.cfg.gs("Libraries.textFieldTimeout.toolTipText"));
                                        panelLibraryCard.add(textFieldTimeout, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer36 ----
                                        vSpacer36.setMinimumSize(new Dimension(10, 30));
                                        vSpacer36.setPreferredSize(new Dimension(20, 30));
                                        vSpacer36.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer36, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelFlavor ----
                                        labelFlavor.setText(context.cfg.gs("Navigator.labelFlavor.text"));
                                        panelLibraryCard.add(labelFlavor, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- comboBoxFlavor ----
                                        comboBoxFlavor.setModel(new DefaultComboBoxModel<>(new String[] {
                                            "Linux",
                                            "Mac",
                                            "Windows"
                                        }));
                                        comboBoxFlavor.setName("flavor");
                                        comboBoxFlavor.setMinimumSize(new Dimension(104, 30));
                                        comboBoxFlavor.setPreferredSize(new Dimension(104, 30));
                                        comboBoxFlavor.setToolTipText(context.cfg.gs("Libraries.comboBoxFlavor.toolTipText"));
                                        panelLibraryCard.add(comboBoxFlavor, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer37 ----
                                        vSpacer37.setMinimumSize(new Dimension(10, 30));
                                        vSpacer37.setPreferredSize(new Dimension(20, 30));
                                        vSpacer37.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer37, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelCase ----
                                        labelCase.setText(context.cfg.gs("Navigator.labelCase.text"));
                                        panelLibraryCard.add(labelCase, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- checkBoxCase ----
                                        checkBoxCase.setName("case");
                                        checkBoxCase.setToolTipText(context.cfg.gs("Libraries.checkBoxCase.toolTipText"));
                                        panelLibraryCard.add(checkBoxCase, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer38 ----
                                        vSpacer38.setMinimumSize(new Dimension(10, 30));
                                        vSpacer38.setPreferredSize(new Dimension(20, 30));
                                        vSpacer38.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer38, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelTempDated ----
                                        labelTempDated.setText(context.cfg.gs("Navigator.labelTempDated.text"));
                                        panelLibraryCard.add(labelTempDated, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- checkBoxTempDated ----
                                        checkBoxTempDated.setName("tempdated");
                                        checkBoxTempDated.setToolTipText(context.cfg.gs("Libraries.checkBoxTempDated.toolTipText"));
                                        panelLibraryCard.add(checkBoxTempDated, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer42 ----
                                        vSpacer42.setMinimumSize(new Dimension(10, 30));
                                        vSpacer42.setPreferredSize(new Dimension(20, 30));
                                        vSpacer42.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer42, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelTempLocation ----
                                        labelTempLocation.setText(context.cfg.gs("Navigator.labelTempLocation.text"));
                                        panelLibraryCard.add(labelTempLocation, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 8), 0, 0));

                                        //---- textFieldTempLocation ----
                                        textFieldTempLocation.setPreferredSize(new Dimension(240, 30));
                                        textFieldTempLocation.setMaximumSize(new Dimension(240, 2147483647));
                                        textFieldTempLocation.setName("templocation");
                                        textFieldTempLocation.setToolTipText(context.cfg.gs("Libraries.textFieldTempLocation.toolTipText"));
                                        panelLibraryCard.add(textFieldTempLocation, new GridBagConstraints(1, 8, 3, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer39 ----
                                        vSpacer39.setMinimumSize(new Dimension(10, 30));
                                        vSpacer39.setPreferredSize(new Dimension(20, 30));
                                        vSpacer39.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer39, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- buttonLibrarySelectTempLocation ----
                                        buttonLibrarySelectTempLocation.setText("...");
                                        buttonLibrarySelectTempLocation.setFont(buttonLibrarySelectTempLocation.getFont().deriveFont(buttonLibrarySelectTempLocation.getFont().getStyle() | Font.BOLD));
                                        buttonLibrarySelectTempLocation.setMaximumSize(new Dimension(32, 24));
                                        buttonLibrarySelectTempLocation.setMinimumSize(new Dimension(32, 24));
                                        buttonLibrarySelectTempLocation.setPreferredSize(new Dimension(32, 24));
                                        buttonLibrarySelectTempLocation.setVerticalTextPosition(SwingConstants.TOP);
                                        buttonLibrarySelectTempLocation.setIconTextGap(0);
                                        buttonLibrarySelectTempLocation.setHorizontalTextPosition(SwingConstants.LEADING);
                                        buttonLibrarySelectTempLocation.setActionCommand("generateUUID");
                                        buttonLibrarySelectTempLocation.setToolTipText(context.cfg.gs("Libraries.select.temp.location.path"));
                                        buttonLibrarySelectTempLocation.setName("tempLocation");
                                        panelLibraryCard.add(buttonLibrarySelectTempLocation, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                            new Insets(0, 0, 4, 0), 0, 0));

                                        //---- labelTerminalAllosed ----
                                        labelTerminalAllosed.setText(context.cfg.gs("Navigator.labelTerminal.Allowed.text"));
                                        panelLibraryCard.add(labelTerminalAllosed, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- checkBoxTerminalAllowed ----
                                        checkBoxTerminalAllowed.setName("terminalallowed");
                                        checkBoxTerminalAllowed.setToolTipText(context.cfg.gs("Libraries.checkBoxTerminalAllowed.toolTipText"));
                                        panelLibraryCard.add(checkBoxTerminalAllowed, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- vSpacer40 ----
                                        vSpacer40.setMinimumSize(new Dimension(10, 30));
                                        vSpacer40.setPreferredSize(new Dimension(20, 30));
                                        vSpacer40.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer40, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //---- labelIgnores ----
                                        labelIgnores.setText(context.cfg.gs("Navigator.labelIgnores.text"));
                                        panelLibraryCard.add(labelIgnores, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));

                                        //======== panelLibrariesIgnorePatternsBox ========
                                        {
                                            panelLibrariesIgnorePatternsBox.setPreferredSize(new Dimension(240, 120));
                                            panelLibrariesIgnorePatternsBox.setMinimumSize(new Dimension(168, 120));
                                            panelLibrariesIgnorePatternsBox.setLayout(new BoxLayout(panelLibrariesIgnorePatternsBox, BoxLayout.Y_AXIS));

                                            //======== scrollPaneLibrariesIgnorePatterns ========
                                            {

                                                //---- listLibrariesIgnorePatterns ----
                                                listLibrariesIgnorePatterns.setName("ignorepatterns");
                                                listLibrariesIgnorePatterns.setVisibleRowCount(5);
                                                listLibrariesIgnorePatterns.setModel(new AbstractListModel<String>() {
                                                    String[] values = {
                                                        "Item 1",
                                                        "Item 2",
                                                        "Item 3",
                                                        "Item 4",
                                                        "Item 5",
                                                        "Item 6"
                                                    };
                                                    @Override
                                                    public int getSize() { return values.length; }
                                                    @Override
                                                    public String getElementAt(int i) { return values[i]; }
                                                });
                                                listLibrariesIgnorePatterns.setToolTipText(context.cfg.gs("Libraries.listIgnorePatterns.toolTipText"));
                                                scrollPaneLibrariesIgnorePatterns.setViewportView(listLibrariesIgnorePatterns);
                                            }
                                            panelLibrariesIgnorePatternsBox.add(scrollPaneLibrariesIgnorePatterns);

                                            //======== panelLibrariesIgnorePatternsButtons ========
                                            {
                                                panelLibrariesIgnorePatternsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                //---- buttonLibrariesAddIgnore ----
                                                buttonLibrariesAddIgnore.setText(context.cfg.gs("Navigator.buttonLibrariesAddIgnore.text"));
                                                buttonLibrariesAddIgnore.setFont(buttonLibrariesAddIgnore.getFont().deriveFont(buttonLibrariesAddIgnore.getFont().getSize() - 2f));
                                                buttonLibrariesAddIgnore.setPreferredSize(new Dimension(78, 24));
                                                buttonLibrariesAddIgnore.setMinimumSize(new Dimension(78, 24));
                                                buttonLibrariesAddIgnore.setMaximumSize(new Dimension(78, 24));
                                                buttonLibrariesAddIgnore.setMnemonic(context.cfg.gs("Navigator.buttonLibrariesAddIgnore.mnemonic").charAt(0));
                                                buttonLibrariesAddIgnore.setToolTipText(context.cfg.gs("Navigator.buttonLibrariesAddIgnore.toolTipText"));
                                                buttonLibrariesAddIgnore.setName("addincexc");
                                                buttonLibrariesAddIgnore.setMargin(new Insets(0, -10, 0, -10));
                                                panelLibrariesIgnorePatternsButtons.add(buttonLibrariesAddIgnore);

                                                //---- buttonLibrariesRemoveIgnore ----
                                                buttonLibrariesRemoveIgnore.setText(context.cfg.gs("Navigator.buttonLibrariesRemoveIgnore.text"));
                                                buttonLibrariesRemoveIgnore.setFont(buttonLibrariesRemoveIgnore.getFont().deriveFont(buttonLibrariesRemoveIgnore.getFont().getSize() - 2f));
                                                buttonLibrariesRemoveIgnore.setPreferredSize(new Dimension(78, 24));
                                                buttonLibrariesRemoveIgnore.setMinimumSize(new Dimension(78, 24));
                                                buttonLibrariesRemoveIgnore.setMaximumSize(new Dimension(78, 24));
                                                buttonLibrariesRemoveIgnore.setMnemonic(context.cfg.gs("Navigator.buttonLibrariesRemoveIgnore.mnemonic_2").charAt(0));
                                                buttonLibrariesRemoveIgnore.setToolTipText(context.cfg.gs("Navigator.buttonLibrariesRemoveIgnore.toolTipText"));
                                                buttonLibrariesRemoveIgnore.setName("removeincexc");
                                                buttonLibrariesRemoveIgnore.setMargin(new Insets(0, -10, 0, -10));
                                                panelLibrariesIgnorePatternsButtons.add(buttonLibrariesRemoveIgnore);
                                            }
                                            panelLibrariesIgnorePatternsBox.add(panelLibrariesIgnorePatternsButtons);
                                        }
                                        panelLibraryCard.add(panelLibrariesIgnorePatternsBox, new GridBagConstraints(1, 10, 3, 6, 0.0, 0.0,
                                            GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                            new Insets(0, 0, 0, 4), 0, 0));

                                        //---- vSpacer41 ----
                                        vSpacer41.setMinimumSize(new Dimension(10, 30));
                                        vSpacer41.setPreferredSize(new Dimension(20, 30));
                                        vSpacer41.setMaximumSize(new Dimension(20, 30));
                                        panelLibraryCard.add(vSpacer41, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 4, 4), 0, 0));
                                    }
                                    generalTab.add(panelLibraryCard, "Library");

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
                                    generalTab.add(panelHintServerCard, "HintServer");

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
                                    generalTab.add(panelTargetsCard, "Targets");

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
                                    generalTab.add(panelXCard, "cardX");

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
                                    generalTab.add(panelYCard, "cardY");
                                }
                                tabbedPaneLibrarySpaces.addTab(context.cfg.gs("Navigator.generalTab.tab.title"), generalTab);
                                tabbedPaneLibrarySpaces.setMnemonicAt(0, context.cfg.gs("Navigator.generalTab.tab.mnemonic").charAt(0));

                                //======== locationsTab ========
                                {
                                    locationsTab.setLayout(new BorderLayout());

                                    //======== scrollPaneLocations ========
                                    {

                                        //---- tableLocations ----
                                        tableLocations.setFillsViewportHeight(true);
                                        tableLocations.setShowHorizontalLines(false);
                                        tableLocations.setShowVerticalLines(false);
                                        tableLocations.setName("tableLocations");
                                        scrollPaneLocations.setViewportView(tableLocations);
                                    }
                                    locationsTab.add(scrollPaneLocations, BorderLayout.CENTER);

                                    //======== panelLocButtons ========
                                    {
                                        panelLocButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                        //---- buttonAddLocation ----
                                        buttonAddLocation.setText(context.cfg.gs("Navigator.buttonAddLocation.text"));
                                        buttonAddLocation.setFont(buttonAddLocation.getFont().deriveFont(buttonAddLocation.getFont().getSize() - 2f));
                                        buttonAddLocation.setPreferredSize(new Dimension(78, 24));
                                        buttonAddLocation.setMinimumSize(new Dimension(78, 24));
                                        buttonAddLocation.setMaximumSize(new Dimension(78, 24));
                                        buttonAddLocation.setMnemonic(context.cfg.gs("Navigator.buttonAddLocation.mnemonic_2").charAt(0));
                                        buttonAddLocation.setToolTipText(context.cfg.gs("Navigator.buttonAddLocation.toolTipText"));
                                        buttonAddLocation.setMargin(new Insets(0, -10, 0, -10));
                                        buttonAddLocation.setName("selectlocation");
                                        panelLocButtons.add(buttonAddLocation);

                                        //---- buttonRemoveLocation ----
                                        buttonRemoveLocation.setText(context.cfg.gs("Navigator.buttonRemoveLocation.text"));
                                        buttonRemoveLocation.setFont(buttonRemoveLocation.getFont().deriveFont(buttonRemoveLocation.getFont().getSize() - 2f));
                                        buttonRemoveLocation.setPreferredSize(new Dimension(78, 24));
                                        buttonRemoveLocation.setMinimumSize(new Dimension(78, 24));
                                        buttonRemoveLocation.setMaximumSize(new Dimension(78, 24));
                                        buttonRemoveLocation.setMnemonic(context.cfg.gs("Navigator.buttonRemoveLocation.mnemonic").charAt(0));
                                        buttonRemoveLocation.setToolTipText(context.cfg.gs("Navigator.buttonRemoveLocation.toolTipText"));
                                        buttonRemoveLocation.setMargin(new Insets(0, -10, 0, -10));
                                        panelLocButtons.add(buttonRemoveLocation);
                                    }
                                    locationsTab.add(panelLocButtons, BorderLayout.SOUTH);
                                }
                                tabbedPaneLibrarySpaces.addTab(context.cfg.gs("Navigator.locationsTab.tab.title"), locationsTab);
                                tabbedPaneLibrarySpaces.setMnemonicAt(1, context.cfg.gs("Navigator.locationsTab.tab.mnemonic").charAt(0));

                                //======== bibliographyTab ========
                                {
                                    bibliographyTab.setLayout(new BorderLayout());

                                    //======== splitPanelBiblio ========
                                    {
                                        splitPanelBiblio.setDividerLocation(201);

                                        //======== panelBiblioLibraries ========
                                        {
                                            panelBiblioLibraries.setPreferredSize(new Dimension(200, 40));
                                            panelBiblioLibraries.setMinimumSize(new Dimension(200, 40));
                                            panelBiblioLibraries.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelBiblioLibraries.getLayout()).columnWidths = new int[] {200, 0};
                                            ((GridBagLayout)panelBiblioLibraries.getLayout()).rowHeights = new int[] {0, 0, 0};
                                            ((GridBagLayout)panelBiblioLibraries.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                                            ((GridBagLayout)panelBiblioLibraries.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                            //---- labelBiblioLibraries ----
                                            labelBiblioLibraries.setText(context.cfg.gs("Libraries.labelBiblioLibraries.text"));
                                            labelBiblioLibraries.setFont(labelBiblioLibraries.getFont().deriveFont(labelBiblioLibraries.getFont().getStyle() | Font.BOLD, labelBiblioLibraries.getFont().getSize() + 1f));
                                            labelBiblioLibraries.setHorizontalAlignment(SwingConstants.LEFT);
                                            labelBiblioLibraries.setHorizontalTextPosition(SwingConstants.LEFT);
                                            panelBiblioLibraries.add(labelBiblioLibraries, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(8, 4, 0, 0), 0, 0));

                                            //======== scrollPaneBiblioLibraries ========
                                            {

                                                //---- tableBiblioLibraries ----
                                                tableBiblioLibraries.setFillsViewportHeight(true);
                                                tableBiblioLibraries.setShowVerticalLines(false);
                                                tableBiblioLibraries.setShowHorizontalLines(false);
                                                tableBiblioLibraries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                                                scrollPaneBiblioLibraries.setViewportView(tableBiblioLibraries);
                                            }
                                            panelBiblioLibraries.add(scrollPaneBiblioLibraries, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        splitPanelBiblio.setLeftComponent(panelBiblioLibraries);

                                        //======== panelSources ========
                                        {
                                            panelSources.setLayout(new BorderLayout());

                                            //======== panelSourcesTop ========
                                            {
                                                panelSourcesTop.setLayout(new GridBagLayout());
                                                ((GridBagLayout)panelSourcesTop.getLayout()).columnWidths = new int[] {0, 0};
                                                ((GridBagLayout)panelSourcesTop.getLayout()).rowHeights = new int[] {0, 0, 0};
                                                ((GridBagLayout)panelSourcesTop.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                                                ((GridBagLayout)panelSourcesTop.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                                //---- labelSpacer42 ----
                                                labelSpacer42.setText(" ");
                                                panelSourcesTop.add(labelSpacer42, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                    new Insets(0, 0, 0, 0), 0, 0));

                                                //---- labelSources ----
                                                labelSources.setText(context.cfg.gs("Navigator.labelSources.text"));
                                                labelSources.setFont(labelSources.getFont().deriveFont(labelSources.getFont().getStyle() | Font.BOLD, labelSources.getFont().getSize() + 1f));
                                                labelSources.setHorizontalAlignment(SwingConstants.LEFT);
                                                labelSources.setPreferredSize(new Dimension(200, 16));
                                                labelSources.setMinimumSize(new Dimension(200, 16));
                                                panelSourcesTop.add(labelSources, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                    new Insets(6, 4, 0, 0), 0, 0));
                                            }
                                            panelSources.add(panelSourcesTop, BorderLayout.NORTH);

                                            //======== scrollPaneSources ========
                                            {
                                                scrollPaneSources.setMinimumSize(new Dimension(408, 20));
                                                scrollPaneSources.setViewportView(listSources);
                                            }
                                            panelSources.add(scrollPaneSources, BorderLayout.CENTER);

                                            //======== panelSourceButtons ========
                                            {
                                                panelSourceButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                //---- buttonAddSource ----
                                                buttonAddSource.setText(context.cfg.gs("Navigator.buttonAddSource.text"));
                                                buttonAddSource.setFont(buttonAddSource.getFont().deriveFont(buttonAddSource.getFont().getSize() - 2f));
                                                buttonAddSource.setPreferredSize(new Dimension(78, 24));
                                                buttonAddSource.setMinimumSize(new Dimension(78, 24));
                                                buttonAddSource.setMaximumSize(new Dimension(78, 24));
                                                buttonAddSource.setMnemonic(context.cfg.gs("Navigator.buttonAddSource.mnemonic").charAt(0));
                                                buttonAddSource.setToolTipText(context.cfg.gs("Navigator.buttonAddSource.toolTipText"));
                                                buttonAddSource.setName("addSource");
                                                buttonAddSource.setMargin(new Insets(0, -10, 0, -10));
                                                panelSourceButtons.add(buttonAddSource);

                                                //---- buttonAddMultiSource ----
                                                buttonAddMultiSource.setText(context.cfg.gs("Navigator.buttonAddMultiSource.text"));
                                                buttonAddMultiSource.setFont(buttonAddMultiSource.getFont().deriveFont(buttonAddMultiSource.getFont().getSize() - 2f));
                                                buttonAddMultiSource.setPreferredSize(new Dimension(78, 24));
                                                buttonAddMultiSource.setMinimumSize(new Dimension(78, 24));
                                                buttonAddMultiSource.setMaximumSize(new Dimension(78, 24));
                                                buttonAddMultiSource.setMnemonic(context.cfg.gs("Navigator.buttonAddMultiSource.mnemonic").charAt(0));
                                                buttonAddMultiSource.setToolTipText(context.cfg.gs("Navigator.buttonAddMultiSource.toolTipText"));
                                                buttonAddMultiSource.setName("addMultiSource");
                                                buttonAddMultiSource.setMargin(new Insets(0, -10, 0, -10));
                                                panelSourceButtons.add(buttonAddMultiSource);

                                                //---- buttonUpSource ----
                                                buttonUpSource.setText("^");
                                                buttonUpSource.setMaximumSize(new Dimension(24, 24));
                                                buttonUpSource.setMinimumSize(new Dimension(24, 24));
                                                buttonUpSource.setPreferredSize(new Dimension(24, 24));
                                                buttonUpSource.setFont(buttonUpSource.getFont().deriveFont(buttonUpSource.getFont().getSize() - 2f));
                                                buttonUpSource.setToolTipText(context.cfg.gs("Navigator.buttonUpSource.toolTipText"));
                                                panelSourceButtons.add(buttonUpSource);

                                                //---- buttonDownSource ----
                                                buttonDownSource.setText("v");
                                                buttonDownSource.setFont(buttonDownSource.getFont().deriveFont(buttonDownSource.getFont().getSize() - 2f));
                                                buttonDownSource.setMaximumSize(new Dimension(24, 24));
                                                buttonDownSource.setMinimumSize(new Dimension(24, 24));
                                                buttonDownSource.setPreferredSize(new Dimension(24, 24));
                                                buttonDownSource.setToolTipText(context.cfg.gs("Navigator.buttonDownSource.toolTipText"));
                                                panelSourceButtons.add(buttonDownSource);

                                                //---- buttonRemoveSource ----
                                                buttonRemoveSource.setText(context.cfg.gs("Navigator.buttonRemoveSource.text"));
                                                buttonRemoveSource.setFont(buttonRemoveSource.getFont().deriveFont(buttonRemoveSource.getFont().getSize() - 2f));
                                                buttonRemoveSource.setPreferredSize(new Dimension(78, 24));
                                                buttonRemoveSource.setMinimumSize(new Dimension(78, 24));
                                                buttonRemoveSource.setMaximumSize(new Dimension(78, 24));
                                                buttonRemoveSource.setMnemonic(context.cfg.gs("Navigator.buttonRemoveSource.mnemonic").charAt(0));
                                                buttonRemoveSource.setToolTipText(context.cfg.gs("Navigator.buttonRemoveSource.toolTipText"));
                                                buttonRemoveSource.setMargin(new Insets(0, -10, 0, -10));
                                                panelSourceButtons.add(buttonRemoveSource);
                                            }
                                            panelSources.add(panelSourceButtons, BorderLayout.SOUTH);
                                        }
                                        splitPanelBiblio.setRightComponent(panelSources);
                                    }
                                    bibliographyTab.add(splitPanelBiblio, BorderLayout.CENTER);

                                    //======== panelBiblioButtons ========
                                    {
                                        panelBiblioButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                        //---- buttonAddLibrary ----
                                        buttonAddLibrary.setText(context.cfg.gs("Navigator.buttonAddLibrary.text"));
                                        buttonAddLibrary.setFont(buttonAddLibrary.getFont().deriveFont(buttonAddLibrary.getFont().getSize() - 2f));
                                        buttonAddLibrary.setPreferredSize(new Dimension(78, 24));
                                        buttonAddLibrary.setMinimumSize(new Dimension(78, 24));
                                        buttonAddLibrary.setMaximumSize(new Dimension(78, 24));
                                        buttonAddLibrary.setMnemonic(context.cfg.gs("Navigator.buttonAddLibrary.mnemonic_2").charAt(0));
                                        buttonAddLibrary.setToolTipText(context.cfg.gs("Navigator.buttonAddLibrary.toolTipText"));
                                        buttonAddLibrary.setMargin(new Insets(0, -10, 0, -10));
                                        panelBiblioButtons.add(buttonAddLibrary);

                                        //---- buttonRemoveLibrary ----
                                        buttonRemoveLibrary.setText(context.cfg.gs("Navigator.buttonRemoveLibrary.text"));
                                        buttonRemoveLibrary.setFont(buttonRemoveLibrary.getFont().deriveFont(buttonRemoveLibrary.getFont().getSize() - 2f));
                                        buttonRemoveLibrary.setPreferredSize(new Dimension(78, 24));
                                        buttonRemoveLibrary.setMinimumSize(new Dimension(78, 24));
                                        buttonRemoveLibrary.setMaximumSize(new Dimension(78, 24));
                                        buttonRemoveLibrary.setToolTipText(context.cfg.gs("Navigator.buttonRemoveLibrary.toolTipText"));
                                        buttonRemoveLibrary.setMargin(new Insets(0, -10, 0, -10));
                                        panelBiblioButtons.add(buttonRemoveLibrary);
                                    }
                                    bibliographyTab.add(panelBiblioButtons, BorderLayout.SOUTH);
                                }
                                tabbedPaneLibrarySpaces.addTab(context.cfg.gs("Navigator.bibliographyTab.tab.title"), bibliographyTab);
                                tabbedPaneLibrarySpaces.setMnemonicAt(2, context.cfg.gs("Navigator.bibliographyTab.tab.mnemonic_2").charAt(0));
                            }
                            panelOptions.add(tabbedPaneLibrarySpaces, BorderLayout.CENTER);
                        }
                        splitPaneLibs.setRightComponent(panelOptions);
                    }
                    panelLibraries.add(splitPaneLibs, BorderLayout.CENTER);

                    //======== buttonBarLibs ========
                    {
                        buttonBarLibs.setBorder(new EmptyBorder(12, 0, 0, 0));
                        buttonBarLibs.setPreferredSize(new Dimension(256, 42));
                        buttonBarLibs.setMinimumSize(new Dimension(256, 42));
                        buttonBarLibs.setMaximumSize(new Dimension(2147483647, 42));
                        buttonBarLibs.setLayout(new GridBagLayout());
                        ((GridBagLayout)buttonBarLibs.getLayout()).columnWidths = new int[] {80, 80};

                        //---- saveButton ----
                        saveButton.setText(context.cfg.gs("Z.ok"));
                        saveButton.setToolTipText(context.cfg.gs("Libraries.save.toolTip.text"));
                        saveButton.setMnemonic(context.cfg.gs("Navigator.saveButton.mnemonic").charAt(0));
                        buttonBarLibs.add(saveButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

                        //---- cancelButton ----
                        cancelButton.setText(context.cfg.gs("Libraries.undo"));
                        cancelButton.setToolTipText(context.cfg.gs("Libraries.cancel.toolTip.text"));
                        cancelButton.setMnemonic(context.cfg.gs("Navigator.cancelButton.mnemonic_2").charAt(0));
                        buttonBarLibs.add(cancelButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));
                    }
                    panelLibraries.add(buttonBarLibs, BorderLayout.SOUTH);
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
            labelStatusLeft.setIconTextGap(0);
            panelStatus.add(labelStatusLeft, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                new Insets(0, 4, 0, 4), 0, 0));

            //---- labelStatusMiddle ----
            labelStatusMiddle.setHorizontalAlignment(SwingConstants.CENTER);
            labelStatusMiddle.setHorizontalTextPosition(SwingConstants.CENTER);
            labelStatusMiddle.setIconTextGap(0);
            panelStatus.add(labelStatusMiddle, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 4), 0, 0));

            //---- labelStatusRight ----
            labelStatusRight.setHorizontalAlignment(SwingConstants.RIGHT);
            labelStatusRight.setIconTextGap(0);
            panelStatus.add(labelStatusRight, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 4), 0, 0));
        }
        contentPane.add(panelStatus, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());

        //======== popupMenuBrowser ========
        {
            popupMenuBrowser.setPreferredSize(new Dimension(212, 194));

            //---- popupMenuItemRefresh ----
            popupMenuItemRefresh.setText(context.cfg.gs("Navigator.popupMenuItemRefresh.text"));
            popupMenuItemRefresh.setMnemonic(context.cfg.gs("Navigator.popupMenuItemRefresh.mnemonic").charAt(0));
            popupMenuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            popupMenuItemRefresh.setIcon(new ImageIcon(getClass().getResource("/refresh.png")));
            popupMenuBrowser.add(popupMenuItemRefresh);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemCopy ----
            popupMenuItemCopy.setText(context.cfg.gs("Navigator.popupMenu.Copy.text"));
            popupMenuItemCopy.setMnemonic(context.cfg.gs("Navigator.popupMenu.Copy.mnemonic").charAt(0));
            popupMenuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemCopy.setIcon(new ImageIcon(getClass().getResource("/copy.png")));
            popupMenuBrowser.add(popupMenuItemCopy);

            //---- popupMenuItemCut ----
            popupMenuItemCut.setText(context.cfg.gs("Navigator.popupMenu.Cut.text"));
            popupMenuItemCut.setMnemonic(context.cfg.gs("Navigator.popupMenu.Cut.mnemonic").charAt(0));
            popupMenuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemCut.setIcon(new ImageIcon(getClass().getResource("/cut.png")));
            popupMenuBrowser.add(popupMenuItemCut);

            //---- popupMenuItemPaste ----
            popupMenuItemPaste.setText(context.cfg.gs("Navigator.popupMenu.Paste.text"));
            popupMenuItemPaste.setMnemonic(context.cfg.gs("Navigator.popupMenu.Paste.mnemonic").charAt(0));
            popupMenuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemPaste.setIcon(new ImageIcon(getClass().getResource("/paste.png")));
            popupMenuBrowser.add(popupMenuItemPaste);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemDelete ----
            popupMenuItemDelete.setText(context.cfg.gs("Navigator.popupMenu.Delete.text"));
            popupMenuItemDelete.setMnemonic(context.cfg.gs("Navigator.popupMenu.Delete.mnemonic").charAt(0));
            popupMenuItemDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            popupMenuItemDelete.setIcon(new ImageIcon(getClass().getResource("/delete-x.png")));
            popupMenuBrowser.add(popupMenuItemDelete);
            popupMenuBrowser.addSeparator();

            //---- popupMenuItemNewFolder ----
            popupMenuItemNewFolder.setText(context.cfg.gs("Navigator.popupMenu.NewFolder.text"));
            popupMenuItemNewFolder.setMnemonic(context.cfg.gs("Navigator.popupMenu.NewFolder.mnemonic").charAt(0));
            popupMenuItemNewFolder.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
            popupMenuItemNewFolder.setIcon(new ImageIcon(getClass().getResource("/new-folder.png")));
            popupMenuBrowser.add(popupMenuItemNewFolder);

            //---- popupMenuItemRename ----
            popupMenuItemRename.setText(context.cfg.gs("Navigator.popupMenu.Rename.text"));
            popupMenuItemRename.setMnemonic(context.cfg.gs("Navigator.popupMenu.Rename.mnemonic").charAt(0));
            popupMenuItemRename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
            popupMenuItemRename.setIcon(new ImageIcon(getClass().getResource("/rename.png")));
            popupMenuBrowser.add(popupMenuItemRename);

            //---- popupMenuItemTouch ----
            popupMenuItemTouch.setText(context.cfg.gs("Navigator.popupMenu.Touch.text"));
            popupMenuItemTouch.setMnemonic(context.cfg.gs("Navigator.popupMenu.Touch.mnemonic").charAt(0));
            popupMenuItemTouch.setIcon(new ImageIcon(getClass().getResource("/touch.png")));
            popupMenuItemTouch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));
            popupMenuBrowser.add(popupMenuItemTouch);
        }

        //======== popupMenuLog ========
        {
            popupMenuLog.setPreferredSize(new Dimension(188, 156));

            //---- popupMenuItemFindNext ----
            popupMenuItemFindNext.setText(context.cfg.gs("Navigator.popupMenuItemFindNext.text"));
            popupMenuItemFindNext.setMnemonic(context.cfg.gs("Navigator.popupMenuItemFindNext.mnemonic").charAt(0));
            popupMenuItemFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            popupMenuItemFindNext.setIcon(new ImageIcon(getClass().getResource("/find-next.png")));
            popupMenuLog.add(popupMenuItemFindNext);

            //---- popupMenuItemFind ----
            popupMenuItemFind.setText(context.cfg.gs("Navigator.popupMenuItemFind.text"));
            popupMenuItemFind.setMnemonic(context.cfg.gs("Navigator.popupMenuItemFind.mnemonic").charAt(0));
            popupMenuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemFind.setIcon(new ImageIcon(getClass().getResource("/find.png")));
            popupMenuLog.add(popupMenuItemFind);
            popupMenuLog.addSeparator();

            //---- popupMenuItemTop ----
            popupMenuItemTop.setText(context.cfg.gs("Navigator.popupMenuItemTop.text"));
            popupMenuItemTop.setMnemonic(context.cfg.gs("Navigator.popupMenuItemTop.mnemonic").charAt(0));
            popupMenuItemTop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemTop.setIcon(new ImageIcon(getClass().getResource("/top.png")));
            popupMenuLog.add(popupMenuItemTop);

            //---- popupMenuItemBottom ----
            popupMenuItemBottom.setText(context.cfg.gs("Navigator.popupMenu.Bottom.text"));
            popupMenuItemBottom.setMnemonic(context.cfg.gs("Navigator.popupMenu.Bottom.mnemonic").charAt(0));
            popupMenuItemBottom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK));
            popupMenuItemBottom.setIcon(new ImageIcon(getClass().getResource("/bottom.png")));
            popupMenuLog.add(popupMenuItemBottom);
            popupMenuLog.addSeparator();

            //---- popupMenuItemClear ----
            popupMenuItemClear.setText(context.cfg.gs("Navigator.popupMenu.Clear.text"));
            popupMenuItemClear.setMnemonic(context.cfg.gs("Navigator.popupMenu.Clear.mnemonic").charAt(0));
            popupMenuItemClear.setIcon(new ImageIcon(getClass().getResource("/clear.png")));
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
    public JMenu menuItemClose;
    public JMenuItem menuItemClosePublisher;
    public JMenuItem menuItemCloseSubscriber;
    public JMenuItem menuItemCloseHintKeys;
    public JMenuItem menuItemCloseHintTracking;
    public JMenuItem menuItemGenerate;
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
    public JMenuItem menuItemAuthKeys;
    public JMenuItem menuItemHintKeys;
    public JMenuItem menuItemBlacklist;
    public JMenuItem menuItemWhitelist;
    public JMenuItem menuItemSettings;
    public JMenu menuWindows;
    public JMenuItem menuItemMaximize;
    public JMenuItem menuItemMinimize;
    public JMenuItem menuItemRestore;
    public JMenuItem menuItemSplitHorizontal;
    public JMenuItem menuItemSplitVertical;
    public JMenu menuHelp;
    public JMenuItem menuItemControls;
    public JMenuItem menuItemDiscussions;
    public JMenuItem menuItemDocumentation;
    public JMenuItem menuItemGettingStarted;
    public JMenuItem menuItemGitHubProject;
    public JMenuItem menuItemIssue;
    public JMenuItem menuItemChangelist;
    public JMenuItem menuItemReleaseNotes;
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
    public JPanel panelLibsTop;
    public JPanel panelTopButtons;
    public JPanel hSpacer7;
    public JButton buttonNew;
    public JButton buttonCopy;
    public JButton buttonDelete;
    public JPanel panelHelp;
    public JLabel labelLibrariesHelp;
    public JSplitPane splitPaneLibs;
    public JScrollPane scrollPaneConfig;
    public JTable librariesConfigItems;
    public JPanel panelOptions;
    public JPanel panelControls;
    public JPanel topType;
    public JPanel vSpacer0;
    public JPanel panelLibraryType;
    public JPanel hSpacer3;
    public JLabel labelLibaryType;
    public JPanel panelCardBox;
    public JPanel vSpacer3;
    public JSeparator separator13;
    public JPanel vSpacer4;
    public JTabbedPane tabbedPaneLibrarySpaces;
    public JPanel generalTab;
    public JPanel panelGettingStartedCard;
    public JLabel labelOperationGettingStarted;
    public JPanel panelLibraryCard;
    public JPanel hSpacer4;
    public JPanel hSpacer6;
    public JPanel vSpacer6;
    public JPanel hSpacer5;
    public JLabel labelKey;
    public JTextField textFieldKey;
    public JPanel vSpacer33;
    public JButton buttonLibraryGenerateKey;
    public JLabel labelHost;
    public JTextField textFieldHost;
    public JPanel vSpacer34;
    public JLabel labelListen;
    public JTextField textFieldListen;
    public JPanel vSpacer35;
    public JLabel labelTimeout;
    public JTextField textFieldTimeout;
    public JPanel vSpacer36;
    public JLabel labelFlavor;
    public JComboBox<String> comboBoxFlavor;
    public JPanel vSpacer37;
    public JLabel labelCase;
    public JCheckBox checkBoxCase;
    public JPanel vSpacer38;
    public JLabel labelTempDated;
    public JCheckBox checkBoxTempDated;
    public JPanel vSpacer42;
    public JLabel labelTempLocation;
    public JTextField textFieldTempLocation;
    public JPanel vSpacer39;
    public JButton buttonLibrarySelectTempLocation;
    public JLabel labelTerminalAllosed;
    public JCheckBox checkBoxTerminalAllowed;
    public JPanel vSpacer40;
    public JLabel labelIgnores;
    public JPanel panelLibrariesIgnorePatternsBox;
    public JScrollPane scrollPaneLibrariesIgnorePatterns;
    public JList<String> listLibrariesIgnorePatterns;
    public JPanel panelLibrariesIgnorePatternsButtons;
    public JButton buttonLibrariesAddIgnore;
    public JButton buttonLibrariesRemoveIgnore;
    public JPanel vSpacer41;
    public JPanel panelHintServerCard;
    public JPanel panelTargetsCard;
    public JPanel panelXCard;
    public JPanel panelYCard;
    public JPanel locationsTab;
    public JScrollPane scrollPaneLocations;
    public JTable tableLocations;
    public JPanel panelLocButtons;
    public JButton buttonAddLocation;
    public JButton buttonRemoveLocation;
    public JPanel bibliographyTab;
    public JSplitPane splitPanelBiblio;
    public JPanel panelBiblioLibraries;
    public JLabel labelBiblioLibraries;
    public JScrollPane scrollPaneBiblioLibraries;
    public JTable tableBiblioLibraries;
    public JPanel panelSources;
    public JPanel panelSourcesTop;
    public JLabel labelSpacer42;
    public JLabel labelSources;
    public JScrollPane scrollPaneSources;
    public JList listSources;
    public JPanel panelSourceButtons;
    public JButton buttonAddSource;
    public JButton buttonAddMultiSource;
    public JButton buttonUpSource;
    public JButton buttonDownSource;
    public JButton buttonRemoveSource;
    public JPanel panelBiblioButtons;
    public JButton buttonAddLibrary;
    public JButton buttonRemoveLibrary;
    public JPanel buttonBarLibs;
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
