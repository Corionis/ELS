package com.groksoft.els.gui;

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
                textAreaOperationLog.setFont(new Font("Courier 10 Pitch", Font.PLAIN, 13));
            }
            else
            {
                textAreaLog.setFont(new Font("Courier New", Font.PLAIN, 13));
                textAreaOperationLog.setFont(new Font("Courier New", Font.PLAIN, 13));
            }

            // add smart scroll to the logs
            // https://tips4java.wordpress.com/2013/03/03/smart-scrolling/
            new SmartScroller(scrollPaneLog);
            new SmartScroller(scrollPaneOperationLog);

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
        if (context.operationsUI.checkForChanges())
            changes = true;
        else if (context.navigator.dialogJunkRemover != null && context.navigator.dialogJunkRemover.checkForChanges())
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
        if (context.operationsUI.checkForChanges())
        {
            tabbedPaneMain.setSelectedIndex(1);
            buttonOperationSave.requestFocus();
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
        menuItemRenamer = new JMenuItem();
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
        splitPaneOperation = new JSplitPane();
        panelOperationTop = new JPanel();
        panelOperationButtons = new JPanel();
        panelTopOperationButtons = new JPanel();
        buttonNewOperation = new JButton();
        buttonCopyOperation = new JButton();
        buttonDeleteOperation = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonRunOperation = new JButton();
        hSpacerBeforeGenerate = new JPanel(null);
        buttonGenerateOperation = new JButton();
        panelOperationHelp = new JPanel();
        labelOperationHelp = new JLabel();
        splitPaneOperationContent = new JSplitPane();
        scrollPaneOperationConfig = new JScrollPane();
        operationConfigItems = new JTable();
        panelOperationOptions = new JPanel();
        panelOperationControls = new JPanel();
        topOperationOptions = new JPanel();
        vSpacer0 = new JPanel(null);
        panelOperationMode = new JPanel();
        hSpacer3 = new JPanel(null);
        labelOperationMode = new JLabel();
        scrollPaneOperationCards = new JScrollPane();
        panelOperationCards = new JPanel();
        panelCardGettingStarted = new JPanel();
        labelOperationGettingStarted = new JLabel();
        panelCardPublisher = new JPanel();
        vSpacer3 = new JPanel(null);
        labelOperationNavigatorCheckbox = new JLabel();
        checkBoxOperationNavigator = new JCheckBox();
        vSpacer33 = new JPanel(null);
        panelOperationIncludeExcludeBox = new JPanel();
        scrollPaneOperationIncludeExclude = new JScrollPane();
        listOperationIncludeExclude = new JList<>();
        panelOperationIncludeExcludeButtons = new JPanel();
        buttonOperationAddIncludeExclude = new JButton();
        buttonOperationRemoveIncludeExclude = new JButton();
        labelOperationIncludeExclude = new JLabel();
        vSpacer4 = new JPanel(null);
        labelOperationTargets = new JLabel();
        textFieldOperationTargets = new JTextField();
        buttonOperationTargetsFilePick = new JButton();
        vSpacer5 = new JPanel(null);
        labelOperationsMismatches = new JLabel();
        textFieldOperationMismatches = new JTextField();
        buttonOperationMismatchesFilePick = new JButton();
        vSpacer6 = new JPanel(null);
        comboBoxOperationWhatsNew = new JComboBox<>();
        textFieldOperationWhatsNew = new JTextField();
        buttonOperationWhatsNewFilePick = new JButton();
        vSpacer7 = new JPanel(null);
        labelOperationOverwrite = new JLabel();
        checkBoxOperationOverwrite = new JCheckBox();
        labelOperationExportText = new JLabel();
        textFieldOperationExportText = new JTextField();
        buttonOperationExportTextFilePick = new JButton();
        vSpacer9 = new JPanel(null);
        labelOperationPreservedDates = new JLabel();
        checkBoxOperationPreserveDates = new JCheckBox();
        labelOperationExportItems = new JLabel();
        textFieldOperationExportItems = new JTextField();
        buttonOperationExportItemsFilePick = new JButton();
        vSpacer10 = new JPanel(null);
        labelOperationDecimalScale = new JLabel();
        checkBoxOperationDecimalScale = new JCheckBox();
        vSpacer11 = new JPanel(null);
        labelOperationDryRun = new JLabel();
        checkBoxOperationDryRun = new JCheckBox();
        comboBoxOperationHintKeys = new JComboBox<>();
        textFieldOperationHintKeys = new JTextField();
        buttonOperationHintKeysFilePick = new JButton();
        vSpacer19 = new JPanel(null);
        labelOperationNoBackfill = new JLabel();
        checkBoxOperationNoBackFill = new JCheckBox();
        comboBoxOperationHintsAndServer = new JComboBox<>();
        textFieldOperationHints = new JTextField();
        buttonOperationHintsFilePick = new JButton();
        vSpacer18 = new JPanel(null);
        labelOperationValidate = new JLabel();
        checkBoxOperationValidate = new JCheckBox();
        labelOperationKeepGoing = new JLabel();
        checkBoxOperationKeepGoing = new JCheckBox();
        vSpacer17 = new JPanel(null);
        labelOperationQuitStatusServer = new JLabel();
        checkBoxOperationQuitStatus = new JCheckBox();
        vSpacer16 = new JPanel(null);
        labelOperationDuplicates = new JLabel();
        checkBoxOperationDuplicates = new JCheckBox();
        vSpacer15 = new JPanel(null);
        labelOperationCrossCheck = new JLabel();
        checkBoxOperationCrossCheck = new JCheckBox();
        vSpacer14 = new JPanel(null);
        labelOperationEmptyDirectories = new JLabel();
        checkBoxOperationEmptyDirectories = new JCheckBox();
        panelOperationLogLevels = new JPanel();
        vSpacer13 = new JPanel(null);
        labelOperationIgnored = new JLabel();
        checkBoxOperationIgnored = new JCheckBox();
        panelCardListener = new JPanel();
        vSpacer40 = new JPanel(null);
        labelOperationTargets2 = new JLabel();
        textFieldOperationTargets2 = new JTextField();
        buttonOperationTargetsFilePick2 = new JButton();
        vSpacer32 = new JPanel(null);
        panelOperationExcludeBox = new JPanel();
        scrollPaneOperationExclude = new JScrollPane();
        listOperationExclude = new JList<>();
        panelOperationExcludeButtons = new JPanel();
        buttonOperationAddExclude = new JButton();
        buttonOperationRemoveExclude = new JButton();
        labelOperationExclude = new JLabel();
        vSpacer8 = new JPanel(null);
        labelOperationAuthorize = new JLabel();
        passwordFieldOperationsAuthorize = new JPasswordField();
        vSpacer12 = new JPanel(null);
        labelOperationAuthKeys = new JLabel();
        textFieldOperationAuthKeys = new JTextField();
        buttonOperationAuthKeysFilePick = new JButton();
        vSpacer20 = new JPanel(null);
        labelOperationBlacklist = new JLabel();
        textFieldOperationBlacklist = new JTextField();
        buttonOperationBlacklistFilePick = new JButton();
        vSpacer21 = new JPanel(null);
        labelOperationOverwrite2 = new JLabel();
        checkBoxOperationOverwrite2 = new JCheckBox();
        labelOperationIpWhitelist = new JLabel();
        textFieldOperationIpWhitelist = new JTextField();
        buttonOperationIpWhitelistFilePick = new JButton();
        vSpacer22 = new JPanel(null);
        labelOperationPreservedDates2 = new JLabel();
        checkBoxOperationPreserveDates2 = new JCheckBox();
        vSpacer23 = new JPanel(null);
        labelOperationDecimalScale2 = new JLabel();
        checkBoxOperationDecimalScale2 = new JCheckBox();
        labelOperationHintKeys = new JLabel();
        textFieldOperationHintKeys2 = new JTextField();
        buttonOperationHintKeysFilePick2 = new JButton();
        vSpacer24 = new JPanel(null);
        comboBoxOperationHintsAndServer2 = new JComboBox<>();
        textFieldOperationHints2 = new JTextField();
        buttonOperationHintsFilePick2 = new JButton();
        vSpacer25 = new JPanel(null);
        labelOperationKeepGoing2 = new JLabel();
        checkBoxOperationKeepGoing2 = new JCheckBox();
        vSpacer26 = new JPanel(null);
        vSpacer27 = new JPanel(null);
        vSpacer28 = new JPanel(null);
        vSpacer29 = new JPanel(null);
        panelOperationLogLevels2 = new JPanel();
        vSpacer30 = new JPanel(null);
        vSpacer31 = new JPanel(null);
        panelCardHintServer = new JPanel();
        vSpacer41 = new JPanel(null);
        labelOperationHintKeys2 = new JLabel();
        textFieldOperationHintKeys3 = new JTextField();
        buttonOperationHintKeysFilePick3 = new JButton();
        vSpacer34 = new JPanel(null);
        labelOperationHintKeyServer = new JLabel();
        textFieldOperationHints3 = new JTextField();
        buttonOperationHintsFilePick3 = new JButton();
        vSpacer35 = new JPanel(null);
        labelOperationKeepGoing3 = new JLabel();
        checkBoxOperationKeepGoing3 = new JCheckBox();
        vSpacer36 = new JPanel(null);
        vSpacer39 = new JPanel(null);
        labelOperationBlacklist3 = new JLabel();
        textFieldOperationBlacklist3 = new JTextField();
        buttonOperationBlacklistFilePick3 = new JButton();
        vSpacer37 = new JPanel(null);
        labelOperationIpWhitelist3 = new JLabel();
        textFieldOperationIpWhitelist3 = new JTextField();
        buttonOperationIpWhitelistFilePick3 = new JButton();
        vSpacer38 = new JPanel(null);
        panelCardTerminal = new JPanel();
        labelOperationsTerminal = new JLabel();
        panelCardQuit = new JPanel();
        labelOperationsQuitter = new JLabel();
        panelCardQuitHints = new JPanel();
        vSpacer42 = new JPanel(null);
        labelOperationHintKeyServer6 = new JLabel();
        textFieldOperationHints6 = new JTextField();
        buttonOperationHintsFilePick6 = new JButton();
        vSpacer43 = new JPanel(null);
        panelOperationBottom = new JPanel();
        labelOperationStatus = new JLabel();
        panelOperationBottomButtons = new JPanel();
        buttonOperationSave = new JButton();
        buttonOperationCancel = new JButton();
        tabbedPaneOperationBottom = new JTabbedPane();
        scrollPaneOperationLog = new JScrollPane();
        textAreaOperationLog = new JTextArea();
        panelLibraries = new JPanel();
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
        popupMenuOperationLog = new JPopupMenu();
        popupMenuItemOperationFindNext = new JMenuItem();
        popupMenuItemOperationFind = new JMenuItem();
        popupMenuItemOperationTop = new JMenuItem();
        popupMenuItemOperationBottom = new JMenuItem();
        popupMenuItemOperationClear = new JMenuItem();
        popupCheckBoxMenuItemOperationWordWrap = new JCheckBoxMenuItem();

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

                //---- menuItemRenamer ----
                menuItemRenamer.setText(context.cfg.gs("Navigator.menu.Renamer.text"));
                menuItemRenamer.setMnemonic(context.cfg.gs("Navigator.menu.Renamer.mnemonic").charAt(0));
                menuTools.add(menuItemRenamer);
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

                //======== splitPaneOperation ========
                {
                    splitPaneOperation.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneOperation.setDividerLocation(650);
                    splitPaneOperation.setLastDividerLocation(450);

                    //======== panelOperationTop ========
                    {
                        panelOperationTop.setMinimumSize(new Dimension(0, 0));
                        panelOperationTop.setLayout(new BorderLayout());

                        //======== panelOperationButtons ========
                        {
                            panelOperationButtons.setMinimumSize(new Dimension(0, 0));
                            panelOperationButtons.setPreferredSize(new Dimension(614, 38));
                            panelOperationButtons.setLayout(new BorderLayout());

                            //======== panelTopOperationButtons ========
                            {
                                panelTopOperationButtons.setMinimumSize(new Dimension(140, 38));
                                panelTopOperationButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                                //---- buttonNewOperation ----
                                buttonNewOperation.setText(context.cfg.gs("Operations.buttonNew.text"));
                                buttonNewOperation.setToolTipText(context.cfg.gs("Operations.buttonNew.toolTipText"));
                                buttonNewOperation.setMnemonic(context.cfg.gs("Operations.buttonNew.mnemonic").charAt(0));
                                panelTopOperationButtons.add(buttonNewOperation);

                                //---- buttonCopyOperation ----
                                buttonCopyOperation.setText(context.cfg.gs("Operations.buttonCopy.text"));
                                buttonCopyOperation.setMnemonic(context.cfg.gs("Operations.buttonCopy.mnemonic").charAt(0));
                                buttonCopyOperation.setToolTipText(context.cfg.gs("Operations.buttonCopy.toolTipText"));
                                panelTopOperationButtons.add(buttonCopyOperation);

                                //---- buttonDeleteOperation ----
                                buttonDeleteOperation.setText(context.cfg.gs("Operations.buttonDelete.text"));
                                buttonDeleteOperation.setMnemonic(context.cfg.gs("Operations.buttonDelete.mnemonic").charAt(0));
                                buttonDeleteOperation.setToolTipText(context.cfg.gs("Operations.buttonDelete.toolTipText"));
                                panelTopOperationButtons.add(buttonDeleteOperation);

                                //---- hSpacerBeforeRun ----
                                hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                                hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                                panelTopOperationButtons.add(hSpacerBeforeRun);

                                //---- buttonRunOperation ----
                                buttonRunOperation.setText(context.cfg.gs("Operations.buttonRun.text"));
                                buttonRunOperation.setMnemonic(context.cfg.gs("Navigator.buttonRunOperation.mnemonic").charAt(0));
                                buttonRunOperation.setToolTipText(context.cfg.gs("Operations.buttonRun.toolTipText"));
                                panelTopOperationButtons.add(buttonRunOperation);

                                //---- hSpacerBeforeGenerate ----
                                hSpacerBeforeGenerate.setMinimumSize(new Dimension(22, 6));
                                hSpacerBeforeGenerate.setPreferredSize(new Dimension(22, 6));
                                panelTopOperationButtons.add(hSpacerBeforeGenerate);

                                //---- buttonGenerateOperation ----
                                buttonGenerateOperation.setText(context.cfg.gs("Operations.buttonGenerate.text"));
                                buttonGenerateOperation.setMnemonic(context.cfg.gs("Operations.buttonGenerate.mnemonic").charAt(0));
                                buttonGenerateOperation.setToolTipText(context.cfg.gs("Operations.buttonGenerate.toolTipText"));
                                panelTopOperationButtons.add(buttonGenerateOperation);
                            }
                            panelOperationButtons.add(panelTopOperationButtons, BorderLayout.WEST);

                            //======== panelOperationHelp ========
                            {
                                panelOperationHelp.setPreferredSize(new Dimension(40, 38));
                                panelOperationHelp.setMinimumSize(new Dimension(0, 38));
                                panelOperationHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                                //---- labelOperationHelp ----
                                labelOperationHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                                labelOperationHelp.setPreferredSize(new Dimension(32, 30));
                                labelOperationHelp.setMinimumSize(new Dimension(32, 30));
                                labelOperationHelp.setMaximumSize(new Dimension(32, 30));
                                labelOperationHelp.setToolTipText(context.cfg.gs("Operations.labelHelp.toolTipText"));
                                labelOperationHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                                labelOperationHelp.setIconTextGap(0);
                                panelOperationHelp.add(labelOperationHelp);
                            }
                            panelOperationButtons.add(panelOperationHelp, BorderLayout.EAST);
                        }
                        panelOperationTop.add(panelOperationButtons, BorderLayout.NORTH);

                        //======== splitPaneOperationContent ========
                        {
                            splitPaneOperationContent.setDividerLocation(142);
                            splitPaneOperationContent.setLastDividerLocation(142);
                            splitPaneOperationContent.setMinimumSize(new Dimension(0, 0));

                            //======== scrollPaneOperationConfig ========
                            {
                                scrollPaneOperationConfig.setMinimumSize(new Dimension(0, 0));
                                scrollPaneOperationConfig.setPreferredSize(new Dimension(142, 146));

                                //---- operationConfigItems ----
                                operationConfigItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                                operationConfigItems.setShowVerticalLines(false);
                                operationConfigItems.setFillsViewportHeight(true);
                                scrollPaneOperationConfig.setViewportView(operationConfigItems);
                            }
                            splitPaneOperationContent.setLeftComponent(scrollPaneOperationConfig);

                            //======== panelOperationOptions ========
                            {
                                panelOperationOptions.setMinimumSize(new Dimension(0, 0));
                                panelOperationOptions.setLayout(new BorderLayout());

                                //======== panelOperationControls ========
                                {
                                    panelOperationControls.setMinimumSize(new Dimension(0, 0));
                                    panelOperationControls.setLayout(new BorderLayout());

                                    //======== topOperationOptions ========
                                    {
                                        topOperationOptions.setMinimumSize(new Dimension(0, 0));
                                        topOperationOptions.setLayout(new BorderLayout());

                                        //---- vSpacer0 ----
                                        vSpacer0.setPreferredSize(new Dimension(10, 2));
                                        vSpacer0.setMinimumSize(new Dimension(10, 2));
                                        vSpacer0.setMaximumSize(new Dimension(10, 2));
                                        topOperationOptions.add(vSpacer0, BorderLayout.NORTH);

                                        //======== panelOperationMode ========
                                        {
                                            panelOperationMode.setMinimumSize(new Dimension(0, 0));
                                            panelOperationMode.setLayout(new BoxLayout(panelOperationMode, BoxLayout.X_AXIS));

                                            //---- hSpacer3 ----
                                            hSpacer3.setPreferredSize(new Dimension(4, 10));
                                            hSpacer3.setMinimumSize(new Dimension(4, 12));
                                            hSpacer3.setMaximumSize(new Dimension(4, 32767));
                                            panelOperationMode.add(hSpacer3);

                                            //---- labelOperationMode ----
                                            labelOperationMode.setMaximumSize(new Dimension(800, 16));
                                            labelOperationMode.setFont(labelOperationMode.getFont().deriveFont(labelOperationMode.getFont().getStyle() | Font.BOLD, labelOperationMode.getFont().getSize() + 1f));
                                            labelOperationMode.setPreferredSize(new Dimension(800, 16));
                                            labelOperationMode.setMinimumSize(new Dimension(110, 16));
                                            panelOperationMode.add(labelOperationMode);
                                        }
                                        topOperationOptions.add(panelOperationMode, BorderLayout.WEST);
                                    }
                                    panelOperationControls.add(topOperationOptions, BorderLayout.NORTH);
                                }
                                panelOperationOptions.add(panelOperationControls, BorderLayout.NORTH);

                                //======== scrollPaneOperationCards ========
                                {
                                    scrollPaneOperationCards.setMinimumSize(new Dimension(0, 0));

                                    //======== panelOperationCards ========
                                    {
                                        panelOperationCards.setLayout(new CardLayout());

                                        //======== panelCardGettingStarted ========
                                        {
                                            panelCardGettingStarted.setLayout(new BorderLayout());

                                            //---- labelOperationGettingStarted ----
                                            labelOperationGettingStarted.setText(context.cfg.gs("Operations.labelOperationGettingStarted.text"));
                                            labelOperationGettingStarted.setFont(labelOperationGettingStarted.getFont().deriveFont(labelOperationGettingStarted.getFont().getStyle() | Font.BOLD));
                                            labelOperationGettingStarted.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCardGettingStarted.add(labelOperationGettingStarted, BorderLayout.CENTER);
                                        }
                                        panelOperationCards.add(panelCardGettingStarted, "gettingStarted");

                                        //======== panelCardPublisher ========
                                        {
                                            panelCardPublisher.setName("publisher");
                                            panelCardPublisher.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardPublisher.getLayout()).rowHeights = new int[] {0, 0, 28, 34, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardPublisher.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- vSpacer3 ----
                                            vSpacer3.setPreferredSize(new Dimension(10, 8));
                                            vSpacer3.setMinimumSize(new Dimension(12, 8));
                                            vSpacer3.setMaximumSize(new Dimension(32767, 8));
                                            panelCardPublisher.add(vSpacer3, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationNavigatorCheckbox ----
                                            labelOperationNavigatorCheckbox.setText(context.cfg.gs("Operations.labelOperationNavigatorCheckbox.text"));
                                            labelOperationNavigatorCheckbox.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationNavigatorCheckbox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationNavigator ----
                                            checkBoxOperationNavigator.setName("navigator");
                                            checkBoxOperationNavigator.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationNavigator, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer33 ----
                                            vSpacer33.setMinimumSize(new Dimension(10, 30));
                                            vSpacer33.setPreferredSize(new Dimension(20, 30));
                                            vSpacer33.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer33, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationIncludeExcludeBox ========
                                            {
                                                panelOperationIncludeExcludeBox.setPreferredSize(new Dimension(240, 120));
                                                panelOperationIncludeExcludeBox.setMinimumSize(new Dimension(168, 120));
                                                panelOperationIncludeExcludeBox.setLayout(new BoxLayout(panelOperationIncludeExcludeBox, BoxLayout.Y_AXIS));

                                                //======== scrollPaneOperationIncludeExclude ========
                                                {
                                                    scrollPaneOperationIncludeExclude.setPreferredSize(new Dimension(52, 120));

                                                    //---- listOperationIncludeExclude ----
                                                    listOperationIncludeExclude.setName("includeexclude");
                                                    listOperationIncludeExclude.setVisibleRowCount(5);
                                                    listOperationIncludeExclude.setModel(new AbstractListModel<String>() {
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
                                                    scrollPaneOperationIncludeExclude.setViewportView(listOperationIncludeExclude);
                                                }
                                                panelOperationIncludeExcludeBox.add(scrollPaneOperationIncludeExclude);

                                                //======== panelOperationIncludeExcludeButtons ========
                                                {
                                                    panelOperationIncludeExcludeButtons.setPreferredSize(new Dimension(250, 28));
                                                    panelOperationIncludeExcludeButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                    //---- buttonOperationAddIncludeExclude ----
                                                    buttonOperationAddIncludeExclude.setText(context.cfg.gs("Operations.buttonOperationAddIncludeExclude.text"));
                                                    buttonOperationAddIncludeExclude.setFont(buttonOperationAddIncludeExclude.getFont().deriveFont(buttonOperationAddIncludeExclude.getFont().getSize() - 2f));
                                                    buttonOperationAddIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMnemonic(context.cfg.gs("Operations.buttonOperationAddIncludeExclude.mnemonic").charAt(0));
                                                    buttonOperationAddIncludeExclude.setToolTipText(context.cfg.gs("Operations.buttonOperationAddIncludeExclude.toolTipText"));
                                                    buttonOperationAddIncludeExclude.setName("addincexc");
                                                    buttonOperationAddIncludeExclude.addActionListener(e -> context.operationsUI.actionOperationAddRowClicked(e));
                                                    panelOperationIncludeExcludeButtons.add(buttonOperationAddIncludeExclude);

                                                    //---- buttonOperationRemoveIncludeExclude ----
                                                    buttonOperationRemoveIncludeExclude.setText(context.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.text"));
                                                    buttonOperationRemoveIncludeExclude.setFont(buttonOperationRemoveIncludeExclude.getFont().deriveFont(buttonOperationRemoveIncludeExclude.getFont().getSize() - 2f));
                                                    buttonOperationRemoveIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMnemonic(context.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.mnemonic").charAt(0));
                                                    buttonOperationRemoveIncludeExclude.setToolTipText(context.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.toolTipText"));
                                                    buttonOperationRemoveIncludeExclude.setName("removeincexc");
                                                    buttonOperationRemoveIncludeExclude.addActionListener(e -> context.operationsUI.actionOperationRemoveRowClicked(e));
                                                    panelOperationIncludeExcludeButtons.add(buttonOperationRemoveIncludeExclude);
                                                }
                                                panelOperationIncludeExcludeBox.add(panelOperationIncludeExcludeButtons);
                                            }
                                            panelCardPublisher.add(panelOperationIncludeExcludeBox, new GridBagConstraints(5, 1, 1, 4, 0.0, 0.0,
                                                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIncludeExclude ----
                                            labelOperationIncludeExclude.setText(context.cfg.gs("Operations.labelOperationIncludeExclude.text"));
                                            labelOperationIncludeExclude.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationIncludeExclude, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer4 ----
                                            vSpacer4.setMinimumSize(new Dimension(10, 30));
                                            vSpacer4.setPreferredSize(new Dimension(20, 30));
                                            vSpacer4.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer4, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationTargets ----
                                            labelOperationTargets.setText(context.cfg.gs("Operations.labelOperationTargets.text"));
                                            panelCardPublisher.add(labelOperationTargets, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationTargets ----
                                            textFieldOperationTargets.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationTargets.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationTargets.setName("targets");
                                            textFieldOperationTargets.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationTargets.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationTargets.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationTargets, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationTargetsFilePick ----
                                            buttonOperationTargetsFilePick.setText("...");
                                            buttonOperationTargetsFilePick.setFont(buttonOperationTargetsFilePick.getFont().deriveFont(buttonOperationTargetsFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationTargetsFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationTargetsFilePick.setIconTextGap(0);
                                            buttonOperationTargetsFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationTargetsFilePick.setActionCommand("targetsFilePick");
                                            buttonOperationTargetsFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationTargetsFilePick.toolTipText"));
                                            buttonOperationTargetsFilePick.setName("targets");
                                            buttonOperationTargetsFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationTargetsFilePick, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer5 ----
                                            vSpacer5.setMinimumSize(new Dimension(10, 30));
                                            vSpacer5.setPreferredSize(new Dimension(20, 30));
                                            vSpacer5.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer5, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationsMismatches ----
                                            labelOperationsMismatches.setText(context.cfg.gs("Operations.labelOperationsMismatches.text"));
                                            labelOperationsMismatches.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationsMismatches, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationMismatches ----
                                            textFieldOperationMismatches.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationMismatches.setName("mismatches");
                                            textFieldOperationMismatches.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationMismatches.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationMismatches.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationMismatches.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationMismatches, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationMismatchesFilePick ----
                                            buttonOperationMismatchesFilePick.setText("...");
                                            buttonOperationMismatchesFilePick.setFont(buttonOperationMismatchesFilePick.getFont().deriveFont(buttonOperationMismatchesFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationMismatchesFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationMismatchesFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationMismatchesFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationMismatchesFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationMismatchesFilePick.setIconTextGap(0);
                                            buttonOperationMismatchesFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationMismatchesFilePick.setActionCommand("mismatchesFilePick");
                                            buttonOperationMismatchesFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationMismatchesFilePick.toolTipText"));
                                            buttonOperationMismatchesFilePick.setName("mismatches");
                                            buttonOperationMismatchesFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationMismatchesFilePick, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer6 ----
                                            vSpacer6.setMinimumSize(new Dimension(10, 30));
                                            vSpacer6.setPreferredSize(new Dimension(20, 30));
                                            vSpacer6.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer6, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationWhatsNew ----
                                            comboBoxOperationWhatsNew.setPrototypeDisplayValue(context.cfg.gs("Operations.comboBoxOperationWhatsNew.prototypeDisplayValue"));
                                            comboBoxOperationWhatsNew.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "What's New:",
                                                "What's New, all:"
                                            }));
                                            comboBoxOperationWhatsNew.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationWhatsNew.setName("whatsnew");
                                            comboBoxOperationWhatsNew.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationWhatsNew, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationWhatsNew ----
                                            textFieldOperationWhatsNew.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationWhatsNew.setName("whatsNew");
                                            textFieldOperationWhatsNew.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationWhatsNew.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationWhatsNew.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationWhatsNew.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationWhatsNew, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationWhatsNewFilePick ----
                                            buttonOperationWhatsNewFilePick.setText(context.cfg.gs("Operations.buttonOperationWhatsNewFilePick.text"));
                                            buttonOperationWhatsNewFilePick.setFont(buttonOperationWhatsNewFilePick.getFont().deriveFont(buttonOperationWhatsNewFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationWhatsNewFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationWhatsNewFilePick.setIconTextGap(0);
                                            buttonOperationWhatsNewFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationWhatsNewFilePick.setActionCommand("whatsnewFilePick");
                                            buttonOperationWhatsNewFilePick.setName("whatsnew");
                                            buttonOperationWhatsNewFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationWhatsNewFilePick.toolTipText"));
                                            buttonOperationWhatsNewFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationWhatsNewFilePick, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer7 ----
                                            vSpacer7.setMinimumSize(new Dimension(10, 30));
                                            vSpacer7.setPreferredSize(new Dimension(20, 30));
                                            vSpacer7.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer7, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationOverwrite ----
                                            labelOperationOverwrite.setText(context.cfg.gs("Operations.labelOperationOverwrite.text"));
                                            labelOperationOverwrite.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationOverwrite, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationOverwrite ----
                                            checkBoxOperationOverwrite.setName("overwrite");
                                            checkBoxOperationOverwrite.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationOverwrite, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationExportText ----
                                            labelOperationExportText.setText(context.cfg.gs("Operations.labelOperationExportText.text"));
                                            labelOperationExportText.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationExportText, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationExportText ----
                                            textFieldOperationExportText.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationExportText.setName("exportText");
                                            textFieldOperationExportText.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationExportText.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationExportText.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationExportText.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationExportText, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationExportTextFilePick ----
                                            buttonOperationExportTextFilePick.setText("...");
                                            buttonOperationExportTextFilePick.setFont(buttonOperationExportTextFilePick.getFont().deriveFont(buttonOperationExportTextFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationExportTextFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationExportTextFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationExportTextFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationExportTextFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationExportTextFilePick.setIconTextGap(0);
                                            buttonOperationExportTextFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationExportTextFilePick.setActionCommand("exportTextFilePick");
                                            buttonOperationExportTextFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationExportTextFilePick.toolTipText"));
                                            buttonOperationExportTextFilePick.setName("exporttext");
                                            buttonOperationExportTextFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationExportTextFilePick, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer9 ----
                                            vSpacer9.setMinimumSize(new Dimension(10, 30));
                                            vSpacer9.setPreferredSize(new Dimension(20, 30));
                                            vSpacer9.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer9, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationPreservedDates ----
                                            labelOperationPreservedDates.setText(context.cfg.gs("Operations.labelOperationPreservedDates.text"));
                                            labelOperationPreservedDates.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationPreservedDates, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationPreserveDates ----
                                            checkBoxOperationPreserveDates.setName("preserveDates");
                                            checkBoxOperationPreserveDates.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationPreserveDates, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationExportItems ----
                                            labelOperationExportItems.setText(context.cfg.gs("Operations.labelOperationExportItems.text"));
                                            labelOperationExportItems.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationExportItems, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationExportItems ----
                                            textFieldOperationExportItems.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationExportItems.setName("exportItems");
                                            textFieldOperationExportItems.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationExportItems.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationExportItems.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationExportItems.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationExportItems, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationExportItemsFilePick ----
                                            buttonOperationExportItemsFilePick.setText("...");
                                            buttonOperationExportItemsFilePick.setFont(buttonOperationExportItemsFilePick.getFont().deriveFont(buttonOperationExportItemsFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationExportItemsFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationExportItemsFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationExportItemsFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationExportItemsFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationExportItemsFilePick.setIconTextGap(0);
                                            buttonOperationExportItemsFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationExportItemsFilePick.setActionCommand("exportItemsFilePick");
                                            buttonOperationExportItemsFilePick.setName("exportitems");
                                            buttonOperationExportItemsFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationExportItemsFilePick.toolTipText"));
                                            buttonOperationExportItemsFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationExportItemsFilePick, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer10 ----
                                            vSpacer10.setMinimumSize(new Dimension(10, 30));
                                            vSpacer10.setPreferredSize(new Dimension(20, 30));
                                            vSpacer10.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer10, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDecimalScale ----
                                            labelOperationDecimalScale.setText(context.cfg.gs("Operations.labelOperationDecimalScale.text"));
                                            labelOperationDecimalScale.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationDecimalScale, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDecimalScale ----
                                            checkBoxOperationDecimalScale.setName("decimalScale");
                                            checkBoxOperationDecimalScale.setToolTipText(context.cfg.gs("Operations.checkBoxOperationDecimalScale.toolTipText"));
                                            checkBoxOperationDecimalScale.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDecimalScale, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer11 ----
                                            vSpacer11.setMinimumSize(new Dimension(10, 30));
                                            vSpacer11.setPreferredSize(new Dimension(20, 30));
                                            vSpacer11.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer11, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDryRun ----
                                            labelOperationDryRun.setText(context.cfg.gs("Operations.labelOperation.DryRun.text"));
                                            labelOperationDryRun.setMinimumSize(new Dimension(5260, 16));
                                            panelCardPublisher.add(labelOperationDryRun, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDryRun ----
                                            checkBoxOperationDryRun.setName("dryRun");
                                            checkBoxOperationDryRun.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDryRun, new GridBagConstraints(5, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationHintKeys ----
                                            comboBoxOperationHintKeys.setPrototypeDisplayValue(context.cfg.gs("Operations.comboBoxOperationHintKeys.prototypeDisplayValue"));
                                            comboBoxOperationHintKeys.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Hint keys:",
                                                "Hint keys, only:"
                                            }));
                                            comboBoxOperationHintKeys.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationHintKeys.setName("keys");
                                            comboBoxOperationHintKeys.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationHintKeys, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHintKeys ----
                                            textFieldOperationHintKeys.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHintKeys.setName("hintKeys");
                                            textFieldOperationHintKeys.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHintKeys.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationHintKeys, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintKeysFilePick ----
                                            buttonOperationHintKeysFilePick.setText("...");
                                            buttonOperationHintKeysFilePick.setFont(buttonOperationHintKeysFilePick.getFont().deriveFont(buttonOperationHintKeysFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintKeysFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintKeysFilePick.setIconTextGap(0);
                                            buttonOperationHintKeysFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintKeysFilePick.setActionCommand("hintKeysFilePick");
                                            buttonOperationHintKeysFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationHintKeysFilePick.toolTipText"));
                                            buttonOperationHintKeysFilePick.setName("hintkeys");
                                            buttonOperationHintKeysFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationHintKeysFilePick, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer19 ----
                                            vSpacer19.setMinimumSize(new Dimension(10, 30));
                                            vSpacer19.setPreferredSize(new Dimension(20, 30));
                                            vSpacer19.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer19, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationNoBackfill ----
                                            labelOperationNoBackfill.setText(context.cfg.gs("Operations.labelOperation.NoBackfill.text"));
                                            labelOperationNoBackfill.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationNoBackfill, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationNoBackFill ----
                                            checkBoxOperationNoBackFill.setName("noBackFill");
                                            checkBoxOperationNoBackFill.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationNoBackFill, new GridBagConstraints(5, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationHintsAndServer ----
                                            comboBoxOperationHintsAndServer.setPrototypeDisplayValue(context.cfg.gs("Operations.comboBoxOperationHintsAndServer.prototypeDisplayValue"));
                                            comboBoxOperationHintsAndServer.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Hints:",
                                                "Hint Server:"
                                            }));
                                            comboBoxOperationHintsAndServer.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationHintsAndServer.setName("hints");
                                            comboBoxOperationHintsAndServer.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationHintsAndServer, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHints ----
                                            textFieldOperationHints.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHints.setName("hints");
                                            textFieldOperationHints.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHints.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHints.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHints.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationHints, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintsFilePick ----
                                            buttonOperationHintsFilePick.setText("...");
                                            buttonOperationHintsFilePick.setFont(buttonOperationHintsFilePick.getFont().deriveFont(buttonOperationHintsFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintsFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintsFilePick.setIconTextGap(0);
                                            buttonOperationHintsFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintsFilePick.setActionCommand("hintsFilePick");
                                            buttonOperationHintsFilePick.setToolTipText(context.cfg.gs("Operations.buttonOperationHintsFilePick.toolTipText"));
                                            buttonOperationHintsFilePick.setName("hints");
                                            buttonOperationHintsFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(buttonOperationHintsFilePick, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer18 ----
                                            vSpacer18.setMinimumSize(new Dimension(10, 30));
                                            vSpacer18.setPreferredSize(new Dimension(20, 30));
                                            vSpacer18.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer18, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationValidate ----
                                            labelOperationValidate.setText(context.cfg.gs("Operations.labelOperationValidate.text"));
                                            labelOperationValidate.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationValidate, new GridBagConstraints(4, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationValidate ----
                                            checkBoxOperationValidate.setName("validate");
                                            checkBoxOperationValidate.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationValidate, new GridBagConstraints(5, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationKeepGoing ----
                                            labelOperationKeepGoing.setText(context.cfg.gs("Operations.labelOperationKeepGoing.text"));
                                            labelOperationKeepGoing.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationKeepGoing, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationKeepGoing ----
                                            checkBoxOperationKeepGoing.setName("keepgoing");
                                            checkBoxOperationKeepGoing.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationKeepGoing, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer17 ----
                                            vSpacer17.setMinimumSize(new Dimension(10, 30));
                                            vSpacer17.setPreferredSize(new Dimension(20, 30));
                                            vSpacer17.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer17, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationQuitStatusServer ----
                                            labelOperationQuitStatusServer.setText(context.cfg.gs("Operations.labelOperationQuitStatusServer.text"));
                                            labelOperationQuitStatusServer.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationQuitStatusServer, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationQuitStatus ----
                                            checkBoxOperationQuitStatus.setName("quitstatusserver");
                                            checkBoxOperationQuitStatus.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationQuitStatus, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer16 ----
                                            vSpacer16.setMinimumSize(new Dimension(10, 30));
                                            vSpacer16.setPreferredSize(new Dimension(20, 30));
                                            vSpacer16.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer16, new GridBagConstraints(3, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDuplicates ----
                                            labelOperationDuplicates.setText(context.cfg.gs("Operations.labelOperationDuplicates.text"));
                                            labelOperationDuplicates.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationDuplicates, new GridBagConstraints(4, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDuplicates ----
                                            checkBoxOperationDuplicates.setName("duplicates");
                                            checkBoxOperationDuplicates.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDuplicates, new GridBagConstraints(5, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer15 ----
                                            vSpacer15.setMinimumSize(new Dimension(10, 30));
                                            vSpacer15.setPreferredSize(new Dimension(20, 30));
                                            vSpacer15.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer15, new GridBagConstraints(3, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationCrossCheck ----
                                            labelOperationCrossCheck.setText(context.cfg.gs("Operations.labelOperationCrossCheck.text"));
                                            labelOperationCrossCheck.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationCrossCheck, new GridBagConstraints(4, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 8, 4, 4), 0, 0));

                                            //---- checkBoxOperationCrossCheck ----
                                            checkBoxOperationCrossCheck.setName("crossCheck");
                                            checkBoxOperationCrossCheck.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationCrossCheck, new GridBagConstraints(5, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer14 ----
                                            vSpacer14.setMinimumSize(new Dimension(10, 30));
                                            vSpacer14.setPreferredSize(new Dimension(20, 30));
                                            vSpacer14.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer14, new GridBagConstraints(3, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationEmptyDirectories ----
                                            labelOperationEmptyDirectories.setText(context.cfg.gs("Operations.labelOperationEmptyDirectories.text"));
                                            labelOperationEmptyDirectories.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationEmptyDirectories, new GridBagConstraints(4, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationEmptyDirectories ----
                                            checkBoxOperationEmptyDirectories.setName("emptyDirectories");
                                            checkBoxOperationEmptyDirectories.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationEmptyDirectories, new GridBagConstraints(5, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationLogLevels ========
                                            {
                                                panelOperationLogLevels.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
                                            }
                                            panelCardPublisher.add(panelOperationLogLevels, new GridBagConstraints(1, 15, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer13 ----
                                            vSpacer13.setMinimumSize(new Dimension(10, 30));
                                            vSpacer13.setPreferredSize(new Dimension(20, 30));
                                            vSpacer13.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer13, new GridBagConstraints(3, 15, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIgnored ----
                                            labelOperationIgnored.setText(context.cfg.gs("Operations.labelOperationIgnored.text"));
                                            labelOperationIgnored.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationIgnored, new GridBagConstraints(4, 15, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationIgnored ----
                                            checkBoxOperationIgnored.setName("ignored");
                                            checkBoxOperationIgnored.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationIgnored, new GridBagConstraints(5, 15, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardPublisher, "publisher");

                                        //======== panelCardListener ========
                                        {
                                            panelCardListener.setName("listener");
                                            panelCardListener.setPreferredSize(new Dimension(824, 530));
                                            panelCardListener.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardListener.getLayout()).rowHeights = new int[] {0, 0, 28, 34, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardListener.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- vSpacer40 ----
                                            vSpacer40.setPreferredSize(new Dimension(10, 8));
                                            vSpacer40.setMinimumSize(new Dimension(12, 8));
                                            vSpacer40.setMaximumSize(new Dimension(32767, 8));
                                            panelCardListener.add(vSpacer40, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationTargets2 ----
                                            labelOperationTargets2.setText(context.cfg.gs("Operations.labelOperation.Targets2.text"));
                                            panelCardListener.add(labelOperationTargets2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationTargets2 ----
                                            textFieldOperationTargets2.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationTargets2.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationTargets2.setName("targets2");
                                            textFieldOperationTargets2.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationTargets2.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationTargets2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationTargets2, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationTargetsFilePick2 ----
                                            buttonOperationTargetsFilePick2.setText("...");
                                            buttonOperationTargetsFilePick2.setFont(buttonOperationTargetsFilePick2.getFont().deriveFont(buttonOperationTargetsFilePick2.getFont().getStyle() | Font.BOLD));
                                            buttonOperationTargetsFilePick2.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick2.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick2.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationTargetsFilePick2.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationTargetsFilePick2.setIconTextGap(0);
                                            buttonOperationTargetsFilePick2.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationTargetsFilePick2.setActionCommand("targetsFilePick");
                                            buttonOperationTargetsFilePick2.setToolTipText(context.cfg.gs("Navigator.buttonOperationTargetsFilePick2.toolTipText"));
                                            buttonOperationTargetsFilePick2.setName("targets2");
                                            buttonOperationTargetsFilePick2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationTargetsFilePick2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer32 ----
                                            vSpacer32.setMinimumSize(new Dimension(10, 30));
                                            vSpacer32.setPreferredSize(new Dimension(20, 30));
                                            vSpacer32.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer32, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationExcludeBox ========
                                            {
                                                panelOperationExcludeBox.setPreferredSize(new Dimension(240, 120));
                                                panelOperationExcludeBox.setMinimumSize(new Dimension(168, 120));
                                                panelOperationExcludeBox.setLayout(new BoxLayout(panelOperationExcludeBox, BoxLayout.Y_AXIS));

                                                //======== scrollPaneOperationExclude ========
                                                {
                                                    scrollPaneOperationExclude.setPreferredSize(new Dimension(52, 120));

                                                    //---- listOperationExclude ----
                                                    listOperationExclude.setName("exclude");
                                                    listOperationExclude.setVisibleRowCount(5);
                                                    listOperationExclude.setModel(new AbstractListModel<String>() {
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
                                                    scrollPaneOperationExclude.setViewportView(listOperationExclude);
                                                }
                                                panelOperationExcludeBox.add(scrollPaneOperationExclude);

                                                //======== panelOperationExcludeButtons ========
                                                {
                                                    panelOperationExcludeButtons.setPreferredSize(new Dimension(250, 28));
                                                    panelOperationExcludeButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                    //---- buttonOperationAddExclude ----
                                                    buttonOperationAddExclude.setText(context.cfg.gs("Navigator.buttonOperationAddExclude.text"));
                                                    buttonOperationAddExclude.setFont(buttonOperationAddExclude.getFont().deriveFont(buttonOperationAddExclude.getFont().getSize() - 2f));
                                                    buttonOperationAddExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationAddExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationAddExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationAddExclude.setMnemonic(context.cfg.gs("Navigator.buttonOperationAddExclude.mnemonic").charAt(0));
                                                    buttonOperationAddExclude.setToolTipText(context.cfg.gs("Navigator.buttonOperationAddExclude.toolTipText"));
                                                    buttonOperationAddExclude.setName("addexc");
                                                    buttonOperationAddExclude.addActionListener(e -> context.operationsUI.actionOperationAddRowClicked(e));
                                                    panelOperationExcludeButtons.add(buttonOperationAddExclude);

                                                    //---- buttonOperationRemoveExclude ----
                                                    buttonOperationRemoveExclude.setText(context.cfg.gs("Navigator.buttonOperationRemoveExclude.text"));
                                                    buttonOperationRemoveExclude.setFont(buttonOperationRemoveExclude.getFont().deriveFont(buttonOperationRemoveExclude.getFont().getSize() - 2f));
                                                    buttonOperationRemoveExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationRemoveExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveExclude.setMnemonic(context.cfg.gs("Navigator.buttonOperationRemoveExclude.mnemonic").charAt(0));
                                                    buttonOperationRemoveExclude.setToolTipText(context.cfg.gs("Navigator.buttonOperationRemoveExclude.toolTipText"));
                                                    buttonOperationRemoveExclude.setName("removeexc");
                                                    buttonOperationRemoveExclude.addActionListener(e -> context.operationsUI.actionOperationRemoveRowClicked(e));
                                                    panelOperationExcludeButtons.add(buttonOperationRemoveExclude);
                                                }
                                                panelOperationExcludeBox.add(panelOperationExcludeButtons);
                                            }
                                            panelCardListener.add(panelOperationExcludeBox, new GridBagConstraints(5, 1, 1, 4, 0.0, 0.0,
                                                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationExclude ----
                                            labelOperationExclude.setText(context.cfg.gs("Operations.labelOperation.Exclude.text"));
                                            labelOperationExclude.setMinimumSize(new Dimension(60, 16));
                                            panelCardListener.add(labelOperationExclude, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer8 ----
                                            vSpacer8.setMinimumSize(new Dimension(10, 30));
                                            vSpacer8.setPreferredSize(new Dimension(20, 30));
                                            vSpacer8.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer8, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationAuthorize ----
                                            labelOperationAuthorize.setText(context.cfg.gs("Operations.labelOperation.Authorize.text"));
                                            panelCardListener.add(labelOperationAuthorize, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- passwordFieldOperationsAuthorize ----
                                            passwordFieldOperationsAuthorize.setToolTipText(context.cfg.gs("Operations.passwordFieldOperationsAuthorize.toolTipText"));
                                            passwordFieldOperationsAuthorize.setName("authpassword");
                                            passwordFieldOperationsAuthorize.addActionListener(e -> context.operationsUI.genericAction(e));
                                            passwordFieldOperationsAuthorize.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            panelCardListener.add(passwordFieldOperationsAuthorize, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer12 ----
                                            vSpacer12.setMinimumSize(new Dimension(10, 30));
                                            vSpacer12.setPreferredSize(new Dimension(20, 30));
                                            vSpacer12.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer12, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationAuthKeys ----
                                            labelOperationAuthKeys.setText(context.cfg.gs("Operations.labelOperation.AuthKeys.text"));
                                            panelCardListener.add(labelOperationAuthKeys, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationAuthKeys ----
                                            textFieldOperationAuthKeys.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationAuthKeys.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationAuthKeys.setName("authkeys");
                                            textFieldOperationAuthKeys.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationAuthKeys.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationAuthKeys.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationAuthKeys, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationAuthKeysFilePick ----
                                            buttonOperationAuthKeysFilePick.setText("...");
                                            buttonOperationAuthKeysFilePick.setFont(buttonOperationAuthKeysFilePick.getFont().deriveFont(buttonOperationAuthKeysFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationAuthKeysFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationAuthKeysFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationAuthKeysFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationAuthKeysFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationAuthKeysFilePick.setIconTextGap(0);
                                            buttonOperationAuthKeysFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationAuthKeysFilePick.setActionCommand("authKeysFilePick");
                                            buttonOperationAuthKeysFilePick.setToolTipText(context.cfg.gs("Navigator.buttonOperationAuthKeysFilePick.toolTipText"));
                                            buttonOperationAuthKeysFilePick.setName("authkeys");
                                            buttonOperationAuthKeysFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationAuthKeysFilePick, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer20 ----
                                            vSpacer20.setMinimumSize(new Dimension(10, 30));
                                            vSpacer20.setPreferredSize(new Dimension(20, 30));
                                            vSpacer20.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer20, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationBlacklist ----
                                            labelOperationBlacklist.setText(context.cfg.gs("Operations.labelOperation.Blacklist.text"));
                                            panelCardListener.add(labelOperationBlacklist, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationBlacklist ----
                                            textFieldOperationBlacklist.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationBlacklist.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationBlacklist.setName("blacklist");
                                            textFieldOperationBlacklist.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationBlacklist.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationBlacklist.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationBlacklist, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationBlacklistFilePick ----
                                            buttonOperationBlacklistFilePick.setText("...");
                                            buttonOperationBlacklistFilePick.setFont(buttonOperationBlacklistFilePick.getFont().deriveFont(buttonOperationBlacklistFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationBlacklistFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationBlacklistFilePick.setIconTextGap(0);
                                            buttonOperationBlacklistFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationBlacklistFilePick.setActionCommand("blacklistFilePick");
                                            buttonOperationBlacklistFilePick.setToolTipText(context.cfg.gs("Navigator.buttonOperationBlacklistFilePick.toolTipText"));
                                            buttonOperationBlacklistFilePick.setName("blacklist");
                                            buttonOperationBlacklistFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationBlacklistFilePick, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer21 ----
                                            vSpacer21.setMinimumSize(new Dimension(10, 30));
                                            vSpacer21.setPreferredSize(new Dimension(20, 30));
                                            vSpacer21.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer21, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationOverwrite2 ----
                                            labelOperationOverwrite2.setText(context.cfg.gs("Operations.labelOperation.Overwrite2.text"));
                                            labelOperationOverwrite2.setMinimumSize(new Dimension(60, 16));
                                            panelCardListener.add(labelOperationOverwrite2, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationOverwrite2 ----
                                            checkBoxOperationOverwrite2.setName("overwrite2");
                                            checkBoxOperationOverwrite2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(checkBoxOperationOverwrite2, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIpWhitelist ----
                                            labelOperationIpWhitelist.setText(context.cfg.gs("Operations.labelOperation.IpWhitelist.text"));
                                            panelCardListener.add(labelOperationIpWhitelist, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationIpWhitelist ----
                                            textFieldOperationIpWhitelist.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationIpWhitelist.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationIpWhitelist.setName("ipwhitelist");
                                            textFieldOperationIpWhitelist.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationIpWhitelist.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationIpWhitelist.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationIpWhitelist, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationIpWhitelistFilePick ----
                                            buttonOperationIpWhitelistFilePick.setText("...");
                                            buttonOperationIpWhitelistFilePick.setFont(buttonOperationIpWhitelistFilePick.getFont().deriveFont(buttonOperationIpWhitelistFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationIpWhitelistFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationIpWhitelistFilePick.setIconTextGap(0);
                                            buttonOperationIpWhitelistFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationIpWhitelistFilePick.setActionCommand("ipWhitelistFilePick");
                                            buttonOperationIpWhitelistFilePick.setToolTipText(context.cfg.gs("Navigator.buttonOperationIpWhitelistFilePick.toolTipText"));
                                            buttonOperationIpWhitelistFilePick.setName("ipwhitelist");
                                            buttonOperationIpWhitelistFilePick.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationIpWhitelistFilePick, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer22 ----
                                            vSpacer22.setMinimumSize(new Dimension(10, 30));
                                            vSpacer22.setPreferredSize(new Dimension(20, 30));
                                            vSpacer22.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer22, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationPreservedDates2 ----
                                            labelOperationPreservedDates2.setText(context.cfg.gs("Operations.labelOperation.PreservedDates2.text"));
                                            labelOperationPreservedDates2.setMinimumSize(new Dimension(60, 16));
                                            panelCardListener.add(labelOperationPreservedDates2, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationPreserveDates2 ----
                                            checkBoxOperationPreserveDates2.setName("preserveDates2");
                                            checkBoxOperationPreserveDates2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(checkBoxOperationPreserveDates2, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer23 ----
                                            vSpacer23.setMinimumSize(new Dimension(10, 30));
                                            vSpacer23.setPreferredSize(new Dimension(20, 30));
                                            vSpacer23.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer23, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDecimalScale2 ----
                                            labelOperationDecimalScale2.setText(context.cfg.gs("Operations.labelOperation.DecimalScale2.text"));
                                            labelOperationDecimalScale2.setMinimumSize(new Dimension(60, 16));
                                            panelCardListener.add(labelOperationDecimalScale2, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDecimalScale2 ----
                                            checkBoxOperationDecimalScale2.setName("decimalScale2");
                                            checkBoxOperationDecimalScale2.setToolTipText(context.cfg.gs("Operations.checkBoxOperationDecimalScale2.toolTipText"));
                                            checkBoxOperationDecimalScale2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(checkBoxOperationDecimalScale2, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationHintKeys ----
                                            labelOperationHintKeys.setText(context.cfg.gs("Operations.labelOperationHintKeys.text"));
                                            panelCardListener.add(labelOperationHintKeys, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHintKeys2 ----
                                            textFieldOperationHintKeys2.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHintKeys2.setName("hintKeys2");
                                            textFieldOperationHintKeys2.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys2.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys2.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHintKeys2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationHintKeys2, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintKeysFilePick2 ----
                                            buttonOperationHintKeysFilePick2.setText("...");
                                            buttonOperationHintKeysFilePick2.setFont(buttonOperationHintKeysFilePick2.getFont().deriveFont(buttonOperationHintKeysFilePick2.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintKeysFilePick2.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick2.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick2.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick2.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintKeysFilePick2.setIconTextGap(0);
                                            buttonOperationHintKeysFilePick2.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintKeysFilePick2.setActionCommand("hintKeysFilePick");
                                            buttonOperationHintKeysFilePick2.setToolTipText(context.cfg.gs("Navigator.buttonOperationHintKeysFilePick2.toolTipText"));
                                            buttonOperationHintKeysFilePick2.setName("hintkeys2");
                                            buttonOperationHintKeysFilePick2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationHintKeysFilePick2, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer24 ----
                                            vSpacer24.setMinimumSize(new Dimension(10, 30));
                                            vSpacer24.setPreferredSize(new Dimension(20, 30));
                                            vSpacer24.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer24, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationHintsAndServer2 ----
                                            comboBoxOperationHintsAndServer2.setPrototypeDisplayValue(context.cfg.gs("Navigator.comboBoxOperationHintsAndServer2.prototypeDisplayValue"));
                                            comboBoxOperationHintsAndServer2.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Hints:",
                                                "Hint Server:"
                                            }));
                                            comboBoxOperationHintsAndServer2.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationHintsAndServer2.setName("hints");
                                            comboBoxOperationHintsAndServer2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(comboBoxOperationHintsAndServer2, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHints2 ----
                                            textFieldOperationHints2.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHints2.setName("hints2");
                                            textFieldOperationHints2.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHints2.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHints2.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHints2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(textFieldOperationHints2, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintsFilePick2 ----
                                            buttonOperationHintsFilePick2.setText("...");
                                            buttonOperationHintsFilePick2.setFont(buttonOperationHintsFilePick2.getFont().deriveFont(buttonOperationHintsFilePick2.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintsFilePick2.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick2.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick2.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick2.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintsFilePick2.setIconTextGap(0);
                                            buttonOperationHintsFilePick2.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintsFilePick2.setActionCommand("hintsFilePick");
                                            buttonOperationHintsFilePick2.setToolTipText(context.cfg.gs("Navigator.buttonOperationHintsFilePick2.toolTipText"));
                                            buttonOperationHintsFilePick2.setName("hints2");
                                            buttonOperationHintsFilePick2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(buttonOperationHintsFilePick2, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer25 ----
                                            vSpacer25.setMinimumSize(new Dimension(10, 30));
                                            vSpacer25.setPreferredSize(new Dimension(20, 30));
                                            vSpacer25.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer25, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationKeepGoing2 ----
                                            labelOperationKeepGoing2.setText(context.cfg.gs("Operations.labelOperation.KeepGoing2.text"));
                                            labelOperationKeepGoing2.setMinimumSize(new Dimension(60, 16));
                                            panelCardListener.add(labelOperationKeepGoing2, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationKeepGoing2 ----
                                            checkBoxOperationKeepGoing2.setName("keepgoing2");
                                            checkBoxOperationKeepGoing2.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardListener.add(checkBoxOperationKeepGoing2, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer26 ----
                                            vSpacer26.setMinimumSize(new Dimension(10, 30));
                                            vSpacer26.setPreferredSize(new Dimension(20, 30));
                                            vSpacer26.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer26, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer27 ----
                                            vSpacer27.setMinimumSize(new Dimension(10, 30));
                                            vSpacer27.setPreferredSize(new Dimension(20, 30));
                                            vSpacer27.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer27, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer28 ----
                                            vSpacer28.setMinimumSize(new Dimension(10, 30));
                                            vSpacer28.setPreferredSize(new Dimension(20, 30));
                                            vSpacer28.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer28, new GridBagConstraints(3, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer29 ----
                                            vSpacer29.setMinimumSize(new Dimension(10, 30));
                                            vSpacer29.setPreferredSize(new Dimension(20, 30));
                                            vSpacer29.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer29, new GridBagConstraints(3, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationLogLevels2 ========
                                            {
                                                panelOperationLogLevels2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
                                            }
                                            panelCardListener.add(panelOperationLogLevels2, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer30 ----
                                            vSpacer30.setMinimumSize(new Dimension(10, 30));
                                            vSpacer30.setPreferredSize(new Dimension(20, 30));
                                            vSpacer30.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer30, new GridBagConstraints(3, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer31 ----
                                            vSpacer31.setMinimumSize(new Dimension(10, 30));
                                            vSpacer31.setPreferredSize(new Dimension(20, 30));
                                            vSpacer31.setMaximumSize(new Dimension(20, 30));
                                            panelCardListener.add(vSpacer31, new GridBagConstraints(3, 15, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardListener, "listener");

                                        //======== panelCardHintServer ========
                                        {
                                            panelCardHintServer.setName("hintserver");
                                            panelCardHintServer.setPreferredSize(new Dimension(824, 530));
                                            panelCardHintServer.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardHintServer.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardHintServer.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- vSpacer41 ----
                                            vSpacer41.setMaximumSize(new Dimension(32767, 8));
                                            vSpacer41.setMinimumSize(new Dimension(12, 8));
                                            vSpacer41.setPreferredSize(new Dimension(10, 8));
                                            panelCardHintServer.add(vSpacer41, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationHintKeys2 ----
                                            labelOperationHintKeys2.setText(context.cfg.gs("Navigator.labelOperationHintKeys2.text"));
                                            panelCardHintServer.add(labelOperationHintKeys2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHintKeys3 ----
                                            textFieldOperationHintKeys3.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHintKeys3.setName("hintKeys3");
                                            textFieldOperationHintKeys3.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys3.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHintKeys3.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHintKeys3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(textFieldOperationHintKeys3, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintKeysFilePick3 ----
                                            buttonOperationHintKeysFilePick3.setText("...");
                                            buttonOperationHintKeysFilePick3.setFont(buttonOperationHintKeysFilePick3.getFont().deriveFont(buttonOperationHintKeysFilePick3.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintKeysFilePick3.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick3.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick3.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintKeysFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintKeysFilePick3.setIconTextGap(0);
                                            buttonOperationHintKeysFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintKeysFilePick3.setActionCommand("hintKeysFilePick");
                                            buttonOperationHintKeysFilePick3.setToolTipText(context.cfg.gs("Navigator.buttonOperationHintKeysFilePick3.toolTipText"));
                                            buttonOperationHintKeysFilePick3.setName("hintkeys3");
                                            buttonOperationHintKeysFilePick3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(buttonOperationHintKeysFilePick3, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer34 ----
                                            vSpacer34.setMinimumSize(new Dimension(10, 30));
                                            vSpacer34.setPreferredSize(new Dimension(20, 30));
                                            vSpacer34.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer34, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationHintKeyServer ----
                                            labelOperationHintKeyServer.setText(context.cfg.gs("Navigator.labelOperationHintKeyServer.text"));
                                            panelCardHintServer.add(labelOperationHintKeyServer, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHints3 ----
                                            textFieldOperationHints3.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHints3.setName("hints3");
                                            textFieldOperationHints3.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHints3.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHints3.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHints3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(textFieldOperationHints3, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintsFilePick3 ----
                                            buttonOperationHintsFilePick3.setText("...");
                                            buttonOperationHintsFilePick3.setFont(buttonOperationHintsFilePick3.getFont().deriveFont(buttonOperationHintsFilePick3.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintsFilePick3.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick3.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick3.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintsFilePick3.setIconTextGap(0);
                                            buttonOperationHintsFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintsFilePick3.setActionCommand("hintsFilePick");
                                            buttonOperationHintsFilePick3.setToolTipText(context.cfg.gs("Navigator.buttonOperationHintsFilePick3.toolTipText"));
                                            buttonOperationHintsFilePick3.setName("hints3");
                                            buttonOperationHintsFilePick3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(buttonOperationHintsFilePick3, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer35 ----
                                            vSpacer35.setMinimumSize(new Dimension(10, 30));
                                            vSpacer35.setPreferredSize(new Dimension(20, 30));
                                            vSpacer35.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer35, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationKeepGoing3 ----
                                            labelOperationKeepGoing3.setText(context.cfg.gs("Navigator.labelOperationKeepGoing3.text"));
                                            labelOperationKeepGoing3.setMinimumSize(new Dimension(60, 16));
                                            panelCardHintServer.add(labelOperationKeepGoing3, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationKeepGoing3 ----
                                            checkBoxOperationKeepGoing3.setName("keepgoing3");
                                            checkBoxOperationKeepGoing3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(checkBoxOperationKeepGoing3, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer36 ----
                                            vSpacer36.setMinimumSize(new Dimension(10, 30));
                                            vSpacer36.setPreferredSize(new Dimension(20, 30));
                                            vSpacer36.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer36, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer39 ----
                                            vSpacer39.setMinimumSize(new Dimension(10, 30));
                                            vSpacer39.setPreferredSize(new Dimension(20, 30));
                                            vSpacer39.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer39, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationBlacklist3 ----
                                            labelOperationBlacklist3.setText(context.cfg.gs("Navigator.labelOperationBlacklist3.text"));
                                            panelCardHintServer.add(labelOperationBlacklist3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationBlacklist3 ----
                                            textFieldOperationBlacklist3.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationBlacklist3.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationBlacklist3.setName("blacklist3");
                                            textFieldOperationBlacklist3.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationBlacklist3.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationBlacklist3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(textFieldOperationBlacklist3, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationBlacklistFilePick3 ----
                                            buttonOperationBlacklistFilePick3.setText("...");
                                            buttonOperationBlacklistFilePick3.setFont(buttonOperationBlacklistFilePick3.getFont().deriveFont(buttonOperationBlacklistFilePick3.getFont().getStyle() | Font.BOLD));
                                            buttonOperationBlacklistFilePick3.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick3.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick3.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationBlacklistFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationBlacklistFilePick3.setIconTextGap(0);
                                            buttonOperationBlacklistFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationBlacklistFilePick3.setActionCommand("blacklistFilePick");
                                            buttonOperationBlacklistFilePick3.setToolTipText(context.cfg.gs("Navigator.buttonOperationBlacklistFilePick3.toolTipText"));
                                            buttonOperationBlacklistFilePick3.setName("blacklist3");
                                            buttonOperationBlacklistFilePick3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(buttonOperationBlacklistFilePick3, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer37 ----
                                            vSpacer37.setMinimumSize(new Dimension(10, 30));
                                            vSpacer37.setPreferredSize(new Dimension(20, 30));
                                            vSpacer37.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer37, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIpWhitelist3 ----
                                            labelOperationIpWhitelist3.setText(context.cfg.gs("Navigator.labelOperationIpWhitelist3.text"));
                                            panelCardHintServer.add(labelOperationIpWhitelist3, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationIpWhitelist3 ----
                                            textFieldOperationIpWhitelist3.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationIpWhitelist3.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationIpWhitelist3.setName("ipwhitelist3");
                                            textFieldOperationIpWhitelist3.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationIpWhitelist3.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationIpWhitelist3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(textFieldOperationIpWhitelist3, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationIpWhitelistFilePick3 ----
                                            buttonOperationIpWhitelistFilePick3.setText("...");
                                            buttonOperationIpWhitelistFilePick3.setFont(buttonOperationIpWhitelistFilePick3.getFont().deriveFont(buttonOperationIpWhitelistFilePick3.getFont().getStyle() | Font.BOLD));
                                            buttonOperationIpWhitelistFilePick3.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick3.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick3.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationIpWhitelistFilePick3.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationIpWhitelistFilePick3.setIconTextGap(0);
                                            buttonOperationIpWhitelistFilePick3.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationIpWhitelistFilePick3.setActionCommand("ipWhitelistFilePick");
                                            buttonOperationIpWhitelistFilePick3.setToolTipText(context.cfg.gs("Navigator.buttonOperationIpWhitelistFilePick3.toolTipText"));
                                            buttonOperationIpWhitelistFilePick3.setName("ipwhitelist3");
                                            buttonOperationIpWhitelistFilePick3.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardHintServer.add(buttonOperationIpWhitelistFilePick3, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer38 ----
                                            vSpacer38.setMinimumSize(new Dimension(10, 30));
                                            vSpacer38.setPreferredSize(new Dimension(20, 30));
                                            vSpacer38.setMaximumSize(new Dimension(20, 30));
                                            panelCardHintServer.add(vSpacer38, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardHintServer, "hintserver");

                                        //======== panelCardTerminal ========
                                        {
                                            panelCardTerminal.setName("terminal");
                                            panelCardTerminal.setLayout(new BorderLayout());

                                            //---- labelOperationsTerminal ----
                                            labelOperationsTerminal.setText(context.cfg.gs("Navigator.labelOperationsTerminal.text"));
                                            labelOperationsTerminal.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCardTerminal.add(labelOperationsTerminal, BorderLayout.CENTER);
                                        }
                                        panelOperationCards.add(panelCardTerminal, "terminal");

                                        //======== panelCardQuit ========
                                        {
                                            panelCardQuit.setName("quit");
                                            panelCardQuit.setLayout(new BorderLayout());

                                            //---- labelOperationsQuitter ----
                                            labelOperationsQuitter.setText(context.cfg.gs("Navigator.labelOperationsQuitter.text"));
                                            labelOperationsQuitter.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCardQuit.add(labelOperationsQuitter, BorderLayout.CENTER);
                                        }
                                        panelOperationCards.add(panelCardQuit, "quitter");

                                        //======== panelCardQuitHints ========
                                        {
                                            panelCardQuitHints.setName("hintserver");
                                            panelCardQuitHints.setPreferredSize(new Dimension(824, 530));
                                            panelCardQuitHints.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardQuitHints.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardQuitHints.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- vSpacer42 ----
                                            vSpacer42.setMaximumSize(new Dimension(32767, 8));
                                            vSpacer42.setMinimumSize(new Dimension(12, 8));
                                            vSpacer42.setPreferredSize(new Dimension(10, 8));
                                            panelCardQuitHints.add(vSpacer42, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationHintKeyServer6 ----
                                            labelOperationHintKeyServer6.setText(context.cfg.gs("Navigator.labelOperationHintKeyServer6.text"));
                                            panelCardQuitHints.add(labelOperationHintKeyServer6, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHints6 ----
                                            textFieldOperationHints6.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHints6.setName("hints6");
                                            textFieldOperationHints6.setMaximumSize(new Dimension(240, 30));
                                            textFieldOperationHints6.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationHints6.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    context.operationsUI.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHints6.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardQuitHints.add(textFieldOperationHints6, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationHintsFilePick6 ----
                                            buttonOperationHintsFilePick6.setText("...");
                                            buttonOperationHintsFilePick6.setFont(buttonOperationHintsFilePick6.getFont().deriveFont(buttonOperationHintsFilePick6.getFont().getStyle() | Font.BOLD));
                                            buttonOperationHintsFilePick6.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick6.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick6.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationHintsFilePick6.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationHintsFilePick6.setIconTextGap(0);
                                            buttonOperationHintsFilePick6.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationHintsFilePick6.setActionCommand("hintsFilePick");
                                            buttonOperationHintsFilePick6.setToolTipText(context.cfg.gs("Navigator.buttonOperationHintsFilePick6.toolTipText"));
                                            buttonOperationHintsFilePick6.setName("hints6");
                                            buttonOperationHintsFilePick6.addActionListener(e -> context.operationsUI.genericAction(e));
                                            panelCardQuitHints.add(buttonOperationHintsFilePick6, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer43 ----
                                            vSpacer43.setMinimumSize(new Dimension(10, 30));
                                            vSpacer43.setPreferredSize(new Dimension(20, 30));
                                            vSpacer43.setMaximumSize(new Dimension(20, 30));
                                            panelCardQuitHints.add(vSpacer43, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardQuitHints, "hintsquitter");
                                    }
                                    scrollPaneOperationCards.setViewportView(panelOperationCards);
                                }
                                panelOperationOptions.add(scrollPaneOperationCards, BorderLayout.CENTER);
                            }
                            splitPaneOperationContent.setRightComponent(panelOperationOptions);
                        }
                        panelOperationTop.add(splitPaneOperationContent, BorderLayout.CENTER);

                        //======== panelOperationBottom ========
                        {
                            panelOperationBottom.setMinimumSize(new Dimension(0, 0));
                            panelOperationBottom.setLayout(new BorderLayout());
                            panelOperationBottom.add(labelOperationStatus, BorderLayout.CENTER);

                            //======== panelOperationBottomButtons ========
                            {
                                panelOperationBottomButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

                                //---- buttonOperationSave ----
                                buttonOperationSave.setText(context.cfg.gs("Z.save"));
                                buttonOperationSave.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                                panelOperationBottomButtons.add(buttonOperationSave);

                                //---- buttonOperationCancel ----
                                buttonOperationCancel.setText(context.cfg.gs("Z.cancel"));
                                buttonOperationCancel.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
                                panelOperationBottomButtons.add(buttonOperationCancel);
                            }
                            panelOperationBottom.add(panelOperationBottomButtons, BorderLayout.EAST);
                        }
                        panelOperationTop.add(panelOperationBottom, BorderLayout.SOUTH);
                    }
                    splitPaneOperation.setTopComponent(panelOperationTop);

                    //======== tabbedPaneOperationBottom ========
                    {
                        tabbedPaneOperationBottom.setTabPlacement(SwingConstants.BOTTOM);
                        tabbedPaneOperationBottom.setPreferredSize(new Dimension(1160, 90));
                        tabbedPaneOperationBottom.setFocusable(false);
                        tabbedPaneOperationBottom.setMinimumSize(new Dimension(0, 0));
                        tabbedPaneOperationBottom.setAutoscrolls(true);

                        //======== scrollPaneOperationLog ========
                        {
                            scrollPaneOperationLog.setFocusable(false);
                            scrollPaneOperationLog.setMinimumSize(new Dimension(0, 0));
                            scrollPaneOperationLog.setAutoscrolls(true);

                            //---- textAreaOperationLog ----
                            textAreaOperationLog.setEditable(false);
                            textAreaOperationLog.setTabSize(4);
                            textAreaOperationLog.setLineWrap(true);
                            textAreaOperationLog.setMinimumSize(new Dimension(0, 0));
                            textAreaOperationLog.setComponentPopupMenu(popupMenuOperationLog);
                            textAreaOperationLog.setVerifyInputWhenFocusTarget(false);
                            textAreaOperationLog.setFont(new Font("Courier 10 Pitch", Font.PLAIN, 12));
                            textAreaOperationLog.setWrapStyleWord(true);
                            scrollPaneOperationLog.setViewportView(textAreaOperationLog);
                        }
                        tabbedPaneOperationBottom.addTab(context.cfg.gs("Operations.scrollPaneLog.tab.title"), scrollPaneOperationLog);
                        tabbedPaneOperationBottom.setMnemonicAt(0, context.cfg.gs("Operations.scrollPaneLog.tab.mnemonic").charAt(0));
                    }
                    splitPaneOperation.setBottomComponent(tabbedPaneOperationBottom);
                }
                tabbedPaneMain.addTab(context.cfg.gs("Navigator.splitPane.Operations.tab.title"), splitPaneOperation);
                tabbedPaneMain.setMnemonicAt(1, context.cfg.gs("Navigator.splitPaneOperations.tab.mnemonic").charAt(0));

                //======== panelLibraries ========
                {
                    panelLibraries.setLayout(new BorderLayout());
                }
                tabbedPaneMain.addTab(context.cfg.gs("Navigator.splitPane.Libraries.tab.title"), panelLibraries);
                tabbedPaneMain.setMnemonicAt(2, context.cfg.gs("Navigator.splitPane.Libraries.tab.mnemonic").charAt(0));
            }
            panelMain.add(tabbedPaneMain);
        }
        contentPane.add(panelMain, BorderLayout.CENTER);

        //======== panelStatus ========
        {
            panelStatus.setLayout(new GridBagLayout());

            //---- labelStatusLeft ----
            labelStatusLeft.setText(context.cfg.gs("Navigator.label.StatusLeft.text"));
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
            labelStatusRight.setText(context.cfg.gs("Navigator.label.StatusRight.text"));
            labelStatusRight.setHorizontalAlignment(SwingConstants.RIGHT);
            panelStatus.add(labelStatusRight, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 8), 0, 0));
        }
        contentPane.add(panelStatus, BorderLayout.SOUTH);
        setSize(1025, 835);
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

        //======== popupMenuOperationLog ========
        {
            popupMenuOperationLog.setPreferredSize(new Dimension(180, 156));

            //---- popupMenuItemOperationFindNext ----
            popupMenuItemOperationFindNext.setText(context.cfg.gs("Navigator.popupMenuItemOperationFindNext.text"));
            popupMenuItemOperationFindNext.setMnemonic(context.cfg.gs("Navigator.popupMenuItemOperationFindNext.mnemonic").charAt(0));
            popupMenuItemOperationFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            popupMenuOperationLog.add(popupMenuItemOperationFindNext);

            //---- popupMenuItemOperationFind ----
            popupMenuItemOperationFind.setText(context.cfg.gs("Navigator.popupMenuItemOperationFind.text"));
            popupMenuItemOperationFind.setMnemonic(context.cfg.gs("Navigator.popupMenuItemOperationFind.mnemonic").charAt(0));
            popupMenuItemOperationFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
            popupMenuOperationLog.add(popupMenuItemOperationFind);
            popupMenuOperationLog.addSeparator();

            //---- popupMenuItemOperationTop ----
            popupMenuItemOperationTop.setText(context.cfg.gs("Operations.popupMenuItemOperationTop.text"));
            popupMenuItemOperationTop.setMnemonic(context.cfg.gs("Navigator.popupMenuItemOperationTop.mnemonic").charAt(0));
            popupMenuItemOperationTop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_DOWN_MASK));
            popupMenuOperationLog.add(popupMenuItemOperationTop);

            //---- popupMenuItemOperationBottom ----
            popupMenuItemOperationBottom.setText(context.cfg.gs("Navigator.popupMenuItemOperationBottom.text"));
            popupMenuItemOperationBottom.setMnemonic(context.cfg.gs("Navigator.popupMenuItemOperationBottom.mnemonic").charAt(0));
            popupMenuItemOperationBottom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_DOWN_MASK));
            popupMenuOperationLog.add(popupMenuItemOperationBottom);
            popupMenuOperationLog.addSeparator();

            //---- popupMenuItemOperationClear ----
            popupMenuItemOperationClear.setText(context.cfg.gs("Navigator.popupMenuItemOperationClear.text"));
            popupMenuItemOperationClear.setMnemonic(context.cfg.gs("Navigator.popupMenuItemOperationClear.mnemonic").charAt(0));
            popupMenuOperationLog.add(popupMenuItemOperationClear);
            popupMenuOperationLog.addSeparator();

            //---- popupCheckBoxMenuItemOperationWordWrap ----
            popupCheckBoxMenuItemOperationWordWrap.setText(context.cfg.gs("Navigator.popupCheckBoxMenuItemOperationWordWrap.text"));
            popupCheckBoxMenuItemOperationWordWrap.setMnemonic(context.cfg.gs("Navigator.popupCheckBoxMenuItemOperationWordWrap.mnemonic").charAt(0));
            popupCheckBoxMenuItemOperationWordWrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
            popupMenuOperationLog.add(popupCheckBoxMenuItemOperationWordWrap);
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
    public JMenuItem menuItemRenamer;
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
    public JSplitPane splitPaneOperation;
    public JPanel panelOperationTop;
    public JPanel panelOperationButtons;
    public JPanel panelTopOperationButtons;
    public JButton buttonNewOperation;
    public JButton buttonCopyOperation;
    public JButton buttonDeleteOperation;
    public JPanel hSpacerBeforeRun;
    public JButton buttonRunOperation;
    public JPanel hSpacerBeforeGenerate;
    public JButton buttonGenerateOperation;
    public JPanel panelOperationHelp;
    public JLabel labelOperationHelp;
    public JSplitPane splitPaneOperationContent;
    public JScrollPane scrollPaneOperationConfig;
    public JTable operationConfigItems;
    public JPanel panelOperationOptions;
    public JPanel panelOperationControls;
    public JPanel topOperationOptions;
    public JPanel vSpacer0;
    public JPanel panelOperationMode;
    public JPanel hSpacer3;
    public JLabel labelOperationMode;
    public JScrollPane scrollPaneOperationCards;
    public JPanel panelOperationCards;
    public JPanel panelCardGettingStarted;
    public JLabel labelOperationGettingStarted;
    public JPanel panelCardPublisher;
    public JPanel vSpacer3;
    public JLabel labelOperationNavigatorCheckbox;
    public JCheckBox checkBoxOperationNavigator;
    public JPanel vSpacer33;
    public JPanel panelOperationIncludeExcludeBox;
    public JScrollPane scrollPaneOperationIncludeExclude;
    public JList<String> listOperationIncludeExclude;
    public JPanel panelOperationIncludeExcludeButtons;
    public JButton buttonOperationAddIncludeExclude;
    public JButton buttonOperationRemoveIncludeExclude;
    public JLabel labelOperationIncludeExclude;
    public JPanel vSpacer4;
    public JLabel labelOperationTargets;
    public JTextField textFieldOperationTargets;
    public JButton buttonOperationTargetsFilePick;
    public JPanel vSpacer5;
    public JLabel labelOperationsMismatches;
    public JTextField textFieldOperationMismatches;
    public JButton buttonOperationMismatchesFilePick;
    public JPanel vSpacer6;
    public JComboBox<String> comboBoxOperationWhatsNew;
    public JTextField textFieldOperationWhatsNew;
    public JButton buttonOperationWhatsNewFilePick;
    public JPanel vSpacer7;
    public JLabel labelOperationOverwrite;
    public JCheckBox checkBoxOperationOverwrite;
    public JLabel labelOperationExportText;
    public JTextField textFieldOperationExportText;
    public JButton buttonOperationExportTextFilePick;
    public JPanel vSpacer9;
    public JLabel labelOperationPreservedDates;
    public JCheckBox checkBoxOperationPreserveDates;
    public JLabel labelOperationExportItems;
    public JTextField textFieldOperationExportItems;
    public JButton buttonOperationExportItemsFilePick;
    public JPanel vSpacer10;
    public JLabel labelOperationDecimalScale;
    public JCheckBox checkBoxOperationDecimalScale;
    public JPanel vSpacer11;
    public JLabel labelOperationDryRun;
    public JCheckBox checkBoxOperationDryRun;
    public JComboBox<String> comboBoxOperationHintKeys;
    public JTextField textFieldOperationHintKeys;
    public JButton buttonOperationHintKeysFilePick;
    public JPanel vSpacer19;
    public JLabel labelOperationNoBackfill;
    public JCheckBox checkBoxOperationNoBackFill;
    public JComboBox<String> comboBoxOperationHintsAndServer;
    public JTextField textFieldOperationHints;
    public JButton buttonOperationHintsFilePick;
    public JPanel vSpacer18;
    public JLabel labelOperationValidate;
    public JCheckBox checkBoxOperationValidate;
    public JLabel labelOperationKeepGoing;
    public JCheckBox checkBoxOperationKeepGoing;
    public JPanel vSpacer17;
    public JLabel labelOperationQuitStatusServer;
    public JCheckBox checkBoxOperationQuitStatus;
    public JPanel vSpacer16;
    public JLabel labelOperationDuplicates;
    public JCheckBox checkBoxOperationDuplicates;
    public JPanel vSpacer15;
    public JLabel labelOperationCrossCheck;
    public JCheckBox checkBoxOperationCrossCheck;
    public JPanel vSpacer14;
    public JLabel labelOperationEmptyDirectories;
    public JCheckBox checkBoxOperationEmptyDirectories;
    public JPanel panelOperationLogLevels;
    public JPanel vSpacer13;
    public JLabel labelOperationIgnored;
    public JCheckBox checkBoxOperationIgnored;
    public JPanel panelCardListener;
    public JPanel vSpacer40;
    public JLabel labelOperationTargets2;
    public JTextField textFieldOperationTargets2;
    public JButton buttonOperationTargetsFilePick2;
    public JPanel vSpacer32;
    public JPanel panelOperationExcludeBox;
    public JScrollPane scrollPaneOperationExclude;
    public JList<String> listOperationExclude;
    public JPanel panelOperationExcludeButtons;
    public JButton buttonOperationAddExclude;
    public JButton buttonOperationRemoveExclude;
    public JLabel labelOperationExclude;
    public JPanel vSpacer8;
    public JLabel labelOperationAuthorize;
    public JPasswordField passwordFieldOperationsAuthorize;
    public JPanel vSpacer12;
    public JLabel labelOperationAuthKeys;
    public JTextField textFieldOperationAuthKeys;
    public JButton buttonOperationAuthKeysFilePick;
    public JPanel vSpacer20;
    public JLabel labelOperationBlacklist;
    public JTextField textFieldOperationBlacklist;
    public JButton buttonOperationBlacklistFilePick;
    public JPanel vSpacer21;
    public JLabel labelOperationOverwrite2;
    public JCheckBox checkBoxOperationOverwrite2;
    public JLabel labelOperationIpWhitelist;
    public JTextField textFieldOperationIpWhitelist;
    public JButton buttonOperationIpWhitelistFilePick;
    public JPanel vSpacer22;
    public JLabel labelOperationPreservedDates2;
    public JCheckBox checkBoxOperationPreserveDates2;
    public JPanel vSpacer23;
    public JLabel labelOperationDecimalScale2;
    public JCheckBox checkBoxOperationDecimalScale2;
    public JLabel labelOperationHintKeys;
    public JTextField textFieldOperationHintKeys2;
    public JButton buttonOperationHintKeysFilePick2;
    public JPanel vSpacer24;
    public JComboBox<String> comboBoxOperationHintsAndServer2;
    public JTextField textFieldOperationHints2;
    public JButton buttonOperationHintsFilePick2;
    public JPanel vSpacer25;
    public JLabel labelOperationKeepGoing2;
    public JCheckBox checkBoxOperationKeepGoing2;
    public JPanel vSpacer26;
    public JPanel vSpacer27;
    public JPanel vSpacer28;
    public JPanel vSpacer29;
    public JPanel panelOperationLogLevels2;
    public JPanel vSpacer30;
    public JPanel vSpacer31;
    public JPanel panelCardHintServer;
    public JPanel vSpacer41;
    public JLabel labelOperationHintKeys2;
    public JTextField textFieldOperationHintKeys3;
    public JButton buttonOperationHintKeysFilePick3;
    public JPanel vSpacer34;
    public JLabel labelOperationHintKeyServer;
    public JTextField textFieldOperationHints3;
    public JButton buttonOperationHintsFilePick3;
    public JPanel vSpacer35;
    public JLabel labelOperationKeepGoing3;
    public JCheckBox checkBoxOperationKeepGoing3;
    public JPanel vSpacer36;
    public JPanel vSpacer39;
    public JLabel labelOperationBlacklist3;
    public JTextField textFieldOperationBlacklist3;
    public JButton buttonOperationBlacklistFilePick3;
    public JPanel vSpacer37;
    public JLabel labelOperationIpWhitelist3;
    public JTextField textFieldOperationIpWhitelist3;
    public JButton buttonOperationIpWhitelistFilePick3;
    public JPanel vSpacer38;
    public JPanel panelCardTerminal;
    public JLabel labelOperationsTerminal;
    public JPanel panelCardQuit;
    public JLabel labelOperationsQuitter;
    public JPanel panelCardQuitHints;
    public JPanel vSpacer42;
    public JLabel labelOperationHintKeyServer6;
    public JTextField textFieldOperationHints6;
    public JButton buttonOperationHintsFilePick6;
    public JPanel vSpacer43;
    public JPanel panelOperationBottom;
    public JLabel labelOperationStatus;
    public JPanel panelOperationBottomButtons;
    public JButton buttonOperationSave;
    public JButton buttonOperationCancel;
    public JTabbedPane tabbedPaneOperationBottom;
    public JScrollPane scrollPaneOperationLog;
    public JTextArea textAreaOperationLog;
    public JPanel panelLibraries;
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
    public JPopupMenu popupMenuOperationLog;
    public JMenuItem popupMenuItemOperationFindNext;
    public JMenuItem popupMenuItemOperationFind;
    public JMenuItem popupMenuItemOperationTop;
    public JMenuItem popupMenuItemOperationBottom;
    public JMenuItem popupMenuItemOperationClear;
    public JCheckBoxMenuItem popupCheckBoxMenuItemOperationWordWrap;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
