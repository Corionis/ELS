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
import javax.swing.plaf.basic.BasicLabelUI;
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
            setBrowserTabs(-1);

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
            new SmartScroller(scrollPaneOperationLog);

            // Change default JOptionPanel button names based on the locale
            // TODO add tool tip text & mnemonic
            UIManager.put("OptionPane.cancelButtonText", guiContext.cfg.gs("Z.cancel"));
            // TODO add all the FileChooser buttons
            UIManager.put("FileChooser.openButtonText", guiContext.cfg.gs("Z.open"));
            UIManager.put("FileChooser.cancelButtonText", guiContext.cfg.gs("Z.cancel"));
            UIManager.put("OptionPane.noButtonText", guiContext.cfg.gs("Z.no"));
            UIManager.put("OptionPane.okButtonText", guiContext.cfg.gs("Z.ok"));
            UIManager.put("OptionPane.yesButtonText", guiContext.cfg.gs("Z.yes"));

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

    private boolean changesCheckAll()
    {
        boolean changes = false;
        if (guiContext.operations.checkForChanges())
            changes = true;
        else if (guiContext.navigator.dialogJunkRemover != null && guiContext.navigator.dialogJunkRemover.checkForChanges())
            changes = true;
        else if (guiContext.navigator.dialogRenamer != null && guiContext.navigator.dialogRenamer.checkForChanges())
            changes = true;
        else if (guiContext.navigator.dialogJobs != null && guiContext.navigator.dialogJobs.checkForChanges())
            changes = true;
        return changes;
    }

    private void changesGotoUnsaved()
    {
        boolean changes = false;
        if (guiContext.operations.checkForChanges())
        {
            tabbedPaneMain.setSelectedIndex(1);
            buttonOperationSave.requestFocus();
        }
        else if (guiContext.navigator.dialogJunkRemover != null && guiContext.navigator.dialogJunkRemover.checkForChanges())
        {
            guiContext.navigator.dialogJunkRemover.setVisible(true);
            guiContext.navigator.dialogJunkRemover.toFront();
            guiContext.navigator.dialogJunkRemover.requestFocus();
            guiContext.navigator.dialogJunkRemover.toFront();
            guiContext.navigator.dialogJunkRemover.requestFocus();
            guiContext.navigator.dialogJunkRemover.saveButton.requestFocus();
        }
        else if (guiContext.navigator.dialogRenamer != null && guiContext.navigator.dialogRenamer.checkForChanges())
        {
            guiContext.navigator.dialogRenamer.toFront();
            guiContext.navigator.dialogRenamer.requestFocus();
            guiContext.navigator.dialogRenamer.toFront();
            guiContext.navigator.dialogRenamer.requestFocus();
            guiContext.navigator.dialogRenamer.saveButton.requestFocus();
        }
        else if (guiContext.navigator.dialogJobs != null && guiContext.navigator.dialogJobs.checkForChanges())
        {
            guiContext.navigator.dialogJobs.toFront();
            guiContext.navigator.dialogJobs.requestFocus();
            guiContext.navigator.dialogJobs.toFront();
            guiContext.navigator.dialogJobs.requestFocus();
            guiContext.navigator.dialogJobs.saveButton.requestFocus();
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

    public void setBrowserTabs(int tabPlacementIndex)
    {
        int tabPlacement;
        if (tabPlacementIndex < 0)
        {
            tabPlacement = guiContext.preferences.getTabPlacement();
            tabPlacementIndex = guiContext.preferences.getTabPlacementIndex();
        }
        else
            tabPlacement = guiContext.preferences.getTabPlacement(tabPlacementIndex);

        tabbedPaneBrowserOne.setTabPlacement(tabPlacement);
        tabbedPaneBrowserTwo.setTabPlacement(tabPlacement);

        if (tabPlacementIndex > 1) // left or right, rotate
        {
            // change browser tabs orientation to vertical
            JLabel label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionOne.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserOne.setTabComponentAt(0, label);
            //
            label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemOne.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserOne.setTabComponentAt(1, label);

            label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionTwo.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserTwo.setTabComponentAt(0, label);
            //
            label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemTwo.tab.title"));
            label.setUI(new VerticalLabel(tabPlacementIndex == 3));
            tabbedPaneBrowserTwo.setTabComponentAt(1, label);
        }
        else // top or bottom
        {
            // change browser tabs orientation to vertical
            JLabel label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionOne.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserOne.setTabComponentAt(0, label);
            //
            label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemOne.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserOne.setTabComponentAt(1, label);

            label = new JLabel(guiContext.cfg.gs("Navigator.panel.CollectionTwo.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserTwo.setTabComponentAt(0, label);
            //
            label = new JLabel(guiContext.cfg.gs("Navigator.panel.SystemTwo.tab.title"));
            label.setUI(new BasicLabelUI());
            tabbedPaneBrowserTwo.setTabComponentAt(1, label);
        }
    }

    private void thisWindowClosing(WindowEvent e)
    {
        if (verifyClose())
            guiContext.navigator.stop();
    }

    public boolean verifyClose()
    {
        if (changesCheckAll())
        {
            int r = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                    guiContext.cfg.gs("MainFrame.unsaved.changes.are.you.sure"),
                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
            {
                changesGotoUnsaved();
                return false;
            }
        }

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
        menuItemOpenHintServer = new JMenuItem();
        menuItemSaveLayout = new JMenuItem();
        menuItemQuitTerminate = new JMenuItem();
        menuItemFileQuit = new JMenuItem();
        menuEdit = new JMenu();
        menuItemCopy = new JMenuItem();
        menuItemCut = new JMenuItem();
        menuItemPaste = new JMenuItem();
        menuItemDelete = new JMenuItem();
        menuItemFind = new JMenuItem();
        menuItemFindNext = new JMenuItem();
        menuItemNewFolder = new JMenuItem();
        menuItemRename = new JMenuItem();
        menuItemTouch = new JMenuItem();
        menuView = new JMenu();
        menuItemRefresh = new JMenuItem();
        menuItemProgress = new JMenuItem();
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
        menuItem1 = new JMenuItem();
        menuItemPlexGenerator = new JMenuItem();
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
        panelCardPublisher = new JPanel();
        labelOperationNavigatorCheckbox = new JLabel();
        checkBoxOperationNavigator = new JCheckBox();
        vSpacer3 = new JPanel(null);
        panelOperationIncludeExcludeBox = new JPanel();
        vSpacer8 = new JPanel(null);
        scrollPaneOperationIncludeExclude = new JScrollPane();
        listOperationIncludeExclude = new JList();
        panelOperationIncludeExcludeButtons = new JPanel();
        buttonOperationAddIncludeExclude = new JButton();
        buttonOperationRemoveIncludeExclude = new JButton();
        labelOperationIncludeExclude = new JLabel();
        labelOperationJob = new JLabel();
        textFieldOperationJob = new JTextField();
        buttonOperationJobFilePick = new JButton();
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
        labelOperationDryRun = new JLabel();
        checkBoxOperationDryRun = new JCheckBox();
        labelOperationExportText = new JLabel();
        textFieldOperationExportText = new JTextField();
        buttonOperationExportTextFilePick = new JButton();
        vSpacer9 = new JPanel(null);
        labelOperationNoBackfill = new JLabel();
        checkBoxOperationNoBackFill = new JCheckBox();
        labelOperationExportItems = new JLabel();
        textFieldOperationExportItems = new JTextField();
        buttonOperationExportItemsFilePick = new JButton();
        vSpacer10 = new JPanel(null);
        labelOperationOverwrite = new JLabel();
        checkBoxOperationOverwrite = new JCheckBox();
        vSpacer11 = new JPanel(null);
        labelOperationPreservedDates = new JLabel();
        checkBoxOperationPreserveDates = new JCheckBox();
        comboBoxOperationHintKeys = new JComboBox<>();
        textFieldOperationHintKeys = new JTextField();
        buttonOperationHintKeysFilePick = new JButton();
        vSpacer19 = new JPanel(null);
        labelOperationDecimalScale = new JLabel();
        checkBoxOperationDecimalScale = new JCheckBox();
        comboBoxOperationHintsAndServer = new JComboBox<>();
        textFieldOperationHints = new JTextField();
        buttonOperationHintsFilePick = new JButton();
        vSpacer18 = new JPanel(null);
        labelOperationValidate = new JLabel();
        checkBoxOperationValidate = new JCheckBox();
        labelOperationQuitStatusServer = new JLabel();
        checkBoxOperationQuitStatus = new JCheckBox();
        vSpacer17 = new JPanel(null);
        labelOperationKeepGoing = new JLabel();
        checkBoxOperationKeepGoing = new JCheckBox();
        vSpacer16 = new JPanel(null);
        labelOperationDuplicates = new JLabel();
        checkBoxOperationDuplicates = new JCheckBox();
        vSpacer15 = new JPanel(null);
        labelOperationCrossCheck = new JLabel();
        checkBoxOperationCrossCheck = new JCheckBox();
        comboBoxOperationLog = new JComboBox<>();
        textFieldOperationLog = new JTextField();
        buttonOperationLogFilePick = new JButton();
        vSpacer14 = new JPanel(null);
        labelOperationEmptyDirectories = new JLabel();
        checkBoxOperationEmptyDirectories = new JCheckBox();
        labelOperationLogLevels = new JLabel();
        panelOperationLogLevels = new JPanel();
        comboBoxOperationConsoleLevel = new JComboBox<>();
        comboBoxOperationDebugLevel = new JComboBox<>();
        vSpacer13 = new JPanel(null);
        labelOperationIgnored = new JLabel();
        checkBoxOperationIgnored = new JCheckBox();
        panelCardListener = new JPanel();
        label1 = new JLabel();
        panelCardQuit = new JPanel();
        label2 = new JLabel();
        panelCardTerminal = new JPanel();
        label3 = new JLabel();
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
        popupMenuItemNewFolder = new JMenuItem();
        popupMenuItemRename = new JMenuItem();
        popupMenuItemTouch = new JMenuItem();
        popupMenuItemCopy = new JMenuItem();
        popupMenuItemCut = new JMenuItem();
        popupMenuItemPaste = new JMenuItem();
        popupMenuItemDelete = new JMenuItem();
        popupMenuLog = new JPopupMenu();
        popupMenuItemTop = new JMenuItem();
        popupMenuItemClear = new JMenuItem();
        popupMenuItemBottom = new JMenuItem();
        popupMenuOperationLog = new JPopupMenu();
        popupMenuItemOperationTop = new JMenuItem();
        popupMenuItemOperationClear = new JMenuItem();
        popupMenuItemOperationBottom = new JMenuItem();

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
                menuItemQuitTerminate.setMnemonic(guiContext.cfg.gs("Navigator.menuItemQuitTerminate.mnemonic").charAt(0));
                menuItemQuitTerminate.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemQuitTerminate.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemQuitTerminate.setDisplayedMnemonicIndex(12);
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

                //---- menuItemFind ----
                menuItemFind.setText(guiContext.cfg.gs("Navigator.menu.Find.text"));
                menuItemFind.setMnemonic(guiContext.cfg.gs("Navigator.menu.Find.mnemonic").charAt(0));
                menuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
                menuItemFind.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFind.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemFind);

                //---- menuItemFindNext ----
                menuItemFindNext.setText(guiContext.cfg.gs("Navigator.menuItemFindNext.text"));
                menuItemFindNext.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFindNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
                menuItemFindNext.setMnemonic(guiContext.cfg.gs("Navigator.menuItemFindNext.mnemonic").charAt(0));
                menuItemFindNext.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemFindNext);
                menuEdit.addSeparator();

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

                //---- menuItemAutoRefresh ----
                menuItemAutoRefresh.setText(guiContext.cfg.gs("Navigator.menuItemAutoRefresh.text"));
                menuItemAutoRefresh.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemAutoRefresh.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAutoRefresh.setMnemonic(guiContext.cfg.gs("Navigator.menuItemAutoRefresh.mnemonic").charAt(0));
                menuView.add(menuItemAutoRefresh);

                //---- menuItemShowHidden ----
                menuItemShowHidden.setText(guiContext.cfg.gs("Navigator.menu.ShowHidden.text"));
                menuItemShowHidden.setMnemonic(guiContext.cfg.gs("Navigator.menu.ShowHidden.mnemonic").charAt(0));
                menuItemShowHidden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
                menuItemShowHidden.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemShowHidden.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemShowHidden.setDisplayedMnemonicIndex(5);
                menuView.add(menuItemShowHidden);

                //---- menuItemWordWrap ----
                menuItemWordWrap.setText(guiContext.cfg.gs("Navigator.menuItemWordWrap.text"));
                menuItemWordWrap.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemWordWrap.setMnemonic(guiContext.cfg.gs("Navigator.menuItemWordWrap.mnemonic").charAt(0));
                menuItemWordWrap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
                menuItemWordWrap.setHorizontalTextPosition(SwingConstants.LEFT);
                menuView.add(menuItemWordWrap);
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
                menuItemDuplicates.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemDuplicates);

                //---- menuItemEmptyFinder ----
                menuItemEmptyFinder.setText(guiContext.cfg.gs("Navigator.menuItemEmptyFinder.text"));
                menuItemEmptyFinder.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemEmptyFinder.setHorizontalTextPosition(SwingConstants.LEFT);
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

            //======== menuSystem ========
            {
                menuSystem.setText("System");
                menuSystem.setMnemonic(guiContext.cfg.gs("Navigator.menuSystem.mnemonic").charAt(0));

                //---- menuItemSettings ----
                menuItemSettings.setText(guiContext.cfg.gs("Navigator.menu.Settings.text"));
                menuItemSettings.setMnemonic(guiContext.cfg.gs("Navigator.menu.Settings.mnemonic").charAt(0));
                menuItemSettings.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSettings.setHorizontalAlignment(SwingConstants.LEFT);
                menuSystem.add(menuItemSettings);
                menuSystem.addSeparator();

                //---- menuItemAuthKeys ----
                menuItemAuthKeys.setText(guiContext.cfg.gs("Navigator.menuItemAuthKeys.text"));
                menuItemAuthKeys.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemAuthKeys.setMnemonic(guiContext.cfg.gs("Navigator.menuItemAuthKeys.mnemonic").charAt(0));
                menuItemAuthKeys.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAuthKeys.setEnabled(false);
                menuSystem.add(menuItemAuthKeys);

                //---- menuItemHintKeys ----
                menuItemHintKeys.setText(guiContext.cfg.gs("Navigator.menuItemHintKeys.text"));
                menuItemHintKeys.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemHintKeys.setMnemonic(guiContext.cfg.gs("Navigator.menuItemHintKeys.mnemonic").charAt(0));
                menuItemHintKeys.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemHintKeys.setEnabled(false);
                menuSystem.add(menuItemHintKeys);
                menuSystem.addSeparator();

                //---- menuItemBlacklist ----
                menuItemBlacklist.setText(guiContext.cfg.gs("Navigator.menuItemBlacklist.text"));
                menuItemBlacklist.setMnemonic(guiContext.cfg.gs("Navigator.menuItemBlacklist.mnemonic").charAt(0));
                menuItemBlacklist.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemBlacklist.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemBlacklist.setEnabled(false);
                menuSystem.add(menuItemBlacklist);

                //---- menuItemWhitelist ----
                menuItemWhitelist.setText(guiContext.cfg.gs("Navigator.menuItemWhitelist.text"));
                menuItemWhitelist.setEnabled(false);
                menuItemWhitelist.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemWhitelist.setMnemonic(guiContext.cfg.gs("Navigator.menuItemWhitelist.mnemonic").charAt(0));
                menuItemWhitelist.setHorizontalTextPosition(SwingConstants.LEFT);
                menuSystem.add(menuItemWhitelist);
            }
            menuBarMain.add(menuSystem);

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
                                panelHintTracking.setMaximumSize(new Dimension(136, 30));
                                panelHintTracking.setMinimumSize(new Dimension(136, 30));
                                panelHintTracking.setPreferredSize(new Dimension(136, 30));
                                panelHintTracking.setLayout(new BorderLayout());

                                //---- buttonHintTracking ----
                                buttonHintTracking.setText(guiContext.cfg.gs("Navigator.button.HintTracking.text"));
                                buttonHintTracking.setMnemonic(guiContext.cfg.gs("Navigator.button.HintTracking.mnemonic").charAt(0));
                                buttonHintTracking.setFocusable(false);
                                buttonHintTracking.setPreferredSize(new Dimension(124, 30));
                                buttonHintTracking.setMinimumSize(new Dimension(124, 30));
                                buttonHintTracking.setMaximumSize(new Dimension(124, 30));
                                buttonHintTracking.setIcon(new ImageIcon(getClass().getResource("/hint-green.png")));
                                buttonHintTracking.setActionCommand("hints");
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
                                tabbedPaneBrowserOne.setDisplayedMnemonicIndexAt(0, 11);

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
                                tabbedPaneBrowserOne.setDisplayedMnemonicIndexAt(1, Integer.parseInt(guiContext.cfg.gs("Navigator.panelSystemOne.tab.mnemonicIndex")));
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
                                tabbedPaneBrowserTwo.setDisplayedMnemonicIndexAt(0, Integer.parseInt(guiContext.cfg.gs("Navigator.panelCollectionTwo.tab.mnemonicIndex")));

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
                                tabbedPaneBrowserTwo.setDisplayedMnemonicIndexAt(1, Integer.parseInt(guiContext.cfg.gs("Navigator.panelSystemTwo.tab.mnemonicIndex")));
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
                            textAreaLog.setFont(new Font("Ubuntu Mono", Font.PLAIN, 14));
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

                //======== splitPaneOperation ========
                {
                    splitPaneOperation.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneOperation.setDividerLocation(650);
                    splitPaneOperation.setLastDividerLocation(450);

                    //======== panelOperationTop ========
                    {
                        panelOperationTop.setLayout(new BorderLayout());

                        //======== panelOperationButtons ========
                        {
                            panelOperationButtons.setMinimumSize(new Dimension(140, 38));
                            panelOperationButtons.setPreferredSize(new Dimension(614, 38));
                            panelOperationButtons.setLayout(new BorderLayout());

                            //======== panelTopOperationButtons ========
                            {
                                panelTopOperationButtons.setMinimumSize(new Dimension(140, 38));
                                panelTopOperationButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                                //---- buttonNewOperation ----
                                buttonNewOperation.setText(guiContext.cfg.gs("Operations.buttonNew.text"));
                                buttonNewOperation.setToolTipText(guiContext.cfg.gs("Operations.buttonNew.toolTipText"));
                                buttonNewOperation.setMnemonic(guiContext.cfg.gs("Operations.buttonNew.mnemonic").charAt(0));
                                panelTopOperationButtons.add(buttonNewOperation);

                                //---- buttonCopyOperation ----
                                buttonCopyOperation.setText(guiContext.cfg.gs("Operations.buttonCopy.text"));
                                buttonCopyOperation.setMnemonic(guiContext.cfg.gs("Operations.buttonCopy.mnemonic").charAt(0));
                                buttonCopyOperation.setToolTipText(guiContext.cfg.gs("Operations.buttonCopy.toolTipText"));
                                panelTopOperationButtons.add(buttonCopyOperation);

                                //---- buttonDeleteOperation ----
                                buttonDeleteOperation.setText(guiContext.cfg.gs("Operations.buttonDelete.text"));
                                buttonDeleteOperation.setMnemonic(guiContext.cfg.gs("Operations.buttonDelete.mnemonic").charAt(0));
                                buttonDeleteOperation.setToolTipText(guiContext.cfg.gs("Operations.buttonDelete.toolTipText"));
                                panelTopOperationButtons.add(buttonDeleteOperation);

                                //---- hSpacerBeforeRun ----
                                hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                                hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                                panelTopOperationButtons.add(hSpacerBeforeRun);

                                //---- buttonRunOperation ----
                                buttonRunOperation.setText(guiContext.cfg.gs("Operations.buttonRun.text"));
                                buttonRunOperation.setMnemonic(guiContext.cfg.gs("Operations.buttonRun.mnemonic").charAt(0));
                                buttonRunOperation.setToolTipText(guiContext.cfg.gs("Operations.buttonRun.toolTipText"));
                                panelTopOperationButtons.add(buttonRunOperation);

                                //---- hSpacerBeforeGenerate ----
                                hSpacerBeforeGenerate.setMinimumSize(new Dimension(22, 6));
                                hSpacerBeforeGenerate.setPreferredSize(new Dimension(22, 6));
                                panelTopOperationButtons.add(hSpacerBeforeGenerate);

                                //---- buttonGenerateOperation ----
                                buttonGenerateOperation.setText(guiContext.cfg.gs("Operations.buttonGenerate.text"));
                                buttonGenerateOperation.setMnemonic(guiContext.cfg.gs("Operations.buttonGenerate.mnemonic").charAt(0));
                                buttonGenerateOperation.setToolTipText(guiContext.cfg.gs("Operations.buttonGenerate.toolTipText"));
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
                                labelOperationHelp.setToolTipText(guiContext.cfg.gs("Operations.labelHelp.toolTipText"));
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
                            splitPaneOperationContent.setMinimumSize(new Dimension(140, 80));

                            //======== scrollPaneOperationConfig ========
                            {
                                scrollPaneOperationConfig.setMinimumSize(new Dimension(140, 16));
                                scrollPaneOperationConfig.setPreferredSize(new Dimension(142, 146));

                                //---- operationConfigItems ----
                                operationConfigItems.setPreferredSize(new Dimension(128, 54));
                                operationConfigItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                                operationConfigItems.setShowVerticalLines(false);
                                operationConfigItems.setFillsViewportHeight(true);
                                scrollPaneOperationConfig.setViewportView(operationConfigItems);
                            }
                            splitPaneOperationContent.setLeftComponent(scrollPaneOperationConfig);

                            //======== panelOperationOptions ========
                            {
                                panelOperationOptions.setMinimumSize(new Dimension(0, 78));
                                panelOperationOptions.setLayout(new BorderLayout());

                                //======== panelOperationControls ========
                                {
                                    panelOperationControls.setLayout(new BorderLayout());

                                    //======== topOperationOptions ========
                                    {
                                        topOperationOptions.setLayout(new BorderLayout());

                                        //---- vSpacer0 ----
                                        vSpacer0.setPreferredSize(new Dimension(10, 2));
                                        vSpacer0.setMinimumSize(new Dimension(10, 2));
                                        vSpacer0.setMaximumSize(new Dimension(10, 2));
                                        topOperationOptions.add(vSpacer0, BorderLayout.NORTH);

                                        //======== panelOperationMode ========
                                        {
                                            panelOperationMode.setLayout(new BoxLayout(panelOperationMode, BoxLayout.X_AXIS));

                                            //---- hSpacer3 ----
                                            hSpacer3.setPreferredSize(new Dimension(4, 10));
                                            hSpacer3.setMinimumSize(new Dimension(4, 12));
                                            hSpacer3.setMaximumSize(new Dimension(4, 32767));
                                            panelOperationMode.add(hSpacer3);

                                            //---- labelOperationMode ----
                                            labelOperationMode.setMaximumSize(new Dimension(800, 16));
                                            labelOperationMode.setFont(labelOperationMode.getFont().deriveFont(labelOperationMode.getFont().getSize() + 1f));
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

                                    //======== panelOperationCards ========
                                    {
                                        panelOperationCards.setLayout(new CardLayout());

                                        //======== panelCardPublisher ========
                                        {
                                            panelCardPublisher.setName("publisher");
                                            panelCardPublisher.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardPublisher.getLayout()).rowHeights = new int[] {0, 28, 34, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

                                            //---- labelOperationNavigatorCheckbox ----
                                            labelOperationNavigatorCheckbox.setText(guiContext.cfg.gs("Operations.labelOperationNavigatorCheckbox.text"));
                                            labelOperationNavigatorCheckbox.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationNavigatorCheckbox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationNavigator ----
                                            checkBoxOperationNavigator.setName("navigator");
                                            checkBoxOperationNavigator.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationNavigator, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer3 ----
                                            vSpacer3.setMinimumSize(new Dimension(10, 30));
                                            vSpacer3.setPreferredSize(new Dimension(20, 30));
                                            vSpacer3.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer3, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationIncludeExcludeBox ========
                                            {
                                                panelOperationIncludeExcludeBox.setLayout(new BoxLayout(panelOperationIncludeExcludeBox, BoxLayout.Y_AXIS));

                                                //---- vSpacer8 ----
                                                vSpacer8.setMinimumSize(new Dimension(12, 4));
                                                panelOperationIncludeExcludeBox.add(vSpacer8);

                                                //======== scrollPaneOperationIncludeExclude ========
                                                {

                                                    //---- listOperationIncludeExclude ----
                                                    listOperationIncludeExclude.setMinimumSize(new Dimension(60, 32));
                                                    listOperationIncludeExclude.setPreferredSize(new Dimension(240, 32));
                                                    listOperationIncludeExclude.setVisibleRowCount(5);
                                                    listOperationIncludeExclude.setMaximumSize(new Dimension(5280, 54));
                                                    scrollPaneOperationIncludeExclude.setViewportView(listOperationIncludeExclude);
                                                }
                                                panelOperationIncludeExcludeBox.add(scrollPaneOperationIncludeExclude);

                                                //======== panelOperationIncludeExcludeButtons ========
                                                {
                                                    panelOperationIncludeExcludeButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                                    //---- buttonOperationAddIncludeExclude ----
                                                    buttonOperationAddIncludeExclude.setText(guiContext.cfg.gs("Operations.buttonOperationAddIncludeExclude.text"));
                                                    buttonOperationAddIncludeExclude.setFont(buttonOperationAddIncludeExclude.getFont().deriveFont(buttonOperationAddIncludeExclude.getFont().getSize() - 2f));
                                                    buttonOperationAddIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationAddIncludeExclude.setMnemonic(guiContext.cfg.gs("Operations.buttonOperationAddIncludeExclude.mnemonic").charAt(0));
                                                    buttonOperationAddIncludeExclude.setToolTipText(guiContext.cfg.gs("Operations.buttonOperationAddIncludeExclude.toolTipText"));
                                                    buttonOperationAddIncludeExclude.addActionListener(e -> guiContext.operations.eventOperationAddRowClicked(e));
                                                    panelOperationIncludeExcludeButtons.add(buttonOperationAddIncludeExclude);

                                                    //---- buttonOperationRemoveIncludeExclude ----
                                                    buttonOperationRemoveIncludeExclude.setText(guiContext.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.text"));
                                                    buttonOperationRemoveIncludeExclude.setFont(buttonOperationRemoveIncludeExclude.getFont().deriveFont(buttonOperationRemoveIncludeExclude.getFont().getSize() - 2f));
                                                    buttonOperationRemoveIncludeExclude.setPreferredSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMinimumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMaximumSize(new Dimension(78, 24));
                                                    buttonOperationRemoveIncludeExclude.setMnemonic(guiContext.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.mnemonic").charAt(0));
                                                    buttonOperationRemoveIncludeExclude.setToolTipText(guiContext.cfg.gs("Operations.buttonOperationRemoveIncludeExclude.toolTipText"));
                                                    buttonOperationRemoveIncludeExclude.addActionListener(e -> guiContext.operations.eventOperationRemoveRowClicked(e));
                                                    panelOperationIncludeExcludeButtons.add(buttonOperationRemoveIncludeExclude);
                                                }
                                                panelOperationIncludeExcludeBox.add(panelOperationIncludeExcludeButtons);
                                            }
                                            panelCardPublisher.add(panelOperationIncludeExcludeBox, new GridBagConstraints(5, 0, 1, 4, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIncludeExclude ----
                                            labelOperationIncludeExclude.setText(guiContext.cfg.gs("Operations.labelOperationIncludeExclude.text"));
                                            labelOperationIncludeExclude.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationIncludeExclude, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationJob ----
                                            labelOperationJob.setText(guiContext.cfg.gs("Operations.labelOperationJob.text"));
                                            labelOperationJob.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationJob, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationJob ----
                                            textFieldOperationJob.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationJob.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationJob.setName("job");
                                            textFieldOperationJob.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationJob.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationJob, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationJobFilePick ----
                                            buttonOperationJobFilePick.setText("...");
                                            buttonOperationJobFilePick.setFont(buttonOperationJobFilePick.getFont().deriveFont(buttonOperationJobFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationJobFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationJobFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationJobFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationJobFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationJobFilePick.setIconTextGap(0);
                                            buttonOperationJobFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationJobFilePick.setActionCommand("jobFilePick");
                                            panelCardPublisher.add(buttonOperationJobFilePick, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer4 ----
                                            vSpacer4.setMinimumSize(new Dimension(10, 30));
                                            vSpacer4.setPreferredSize(new Dimension(20, 30));
                                            vSpacer4.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer4, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationTargets ----
                                            labelOperationTargets.setText(guiContext.cfg.gs("Operations.labelOperationTargets.text"));
                                            panelCardPublisher.add(labelOperationTargets, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationTargets ----
                                            textFieldOperationTargets.setPreferredSize(new Dimension(240, 30));
                                            textFieldOperationTargets.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationTargets.setName("targets");
                                            textFieldOperationTargets.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationTargets.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationTargets, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
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
                                            panelCardPublisher.add(buttonOperationTargetsFilePick, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer5 ----
                                            vSpacer5.setMinimumSize(new Dimension(10, 30));
                                            vSpacer5.setPreferredSize(new Dimension(20, 30));
                                            vSpacer5.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer5, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationsMismatches ----
                                            labelOperationsMismatches.setText(guiContext.cfg.gs("Operations.labelOperationsMismatches.text"));
                                            labelOperationsMismatches.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationsMismatches, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationMismatches ----
                                            textFieldOperationMismatches.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationMismatches.setName("mismatches");
                                            textFieldOperationMismatches.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationMismatches.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationMismatches, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
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
                                            panelCardPublisher.add(buttonOperationMismatchesFilePick, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer6 ----
                                            vSpacer6.setMinimumSize(new Dimension(10, 30));
                                            vSpacer6.setPreferredSize(new Dimension(20, 30));
                                            vSpacer6.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer6, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationWhatsNew ----
                                            comboBoxOperationWhatsNew.setPrototypeDisplayValue(guiContext.cfg.gs("Operations.comboBoxOperationWhatsNew.prototypeDisplayValue"));
                                            comboBoxOperationWhatsNew.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "What's New:",
                                                "What's New, all:"
                                            }));
                                            comboBoxOperationWhatsNew.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationWhatsNew.setName("whatsnew");
                                            comboBoxOperationWhatsNew.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationWhatsNew, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationWhatsNew ----
                                            textFieldOperationWhatsNew.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationWhatsNew.setName("whatsNew");
                                            textFieldOperationWhatsNew.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationWhatsNew.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationWhatsNew, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationWhatsNewFilePick ----
                                            buttonOperationWhatsNewFilePick.setText("...");
                                            buttonOperationWhatsNewFilePick.setFont(buttonOperationWhatsNewFilePick.getFont().deriveFont(buttonOperationWhatsNewFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationWhatsNewFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationWhatsNewFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationWhatsNewFilePick.setIconTextGap(0);
                                            buttonOperationWhatsNewFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationWhatsNewFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationWhatsNewFilePick, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer7 ----
                                            vSpacer7.setMinimumSize(new Dimension(10, 30));
                                            vSpacer7.setPreferredSize(new Dimension(20, 30));
                                            vSpacer7.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer7, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDryRun ----
                                            labelOperationDryRun.setText(guiContext.cfg.gs("Operations.labelOperationDryRun.text"));
                                            labelOperationDryRun.setMinimumSize(new Dimension(5260, 16));
                                            panelCardPublisher.add(labelOperationDryRun, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDryRun ----
                                            checkBoxOperationDryRun.setName("dryRun");
                                            checkBoxOperationDryRun.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDryRun, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationExportText ----
                                            labelOperationExportText.setText(guiContext.cfg.gs("Operations.labelOperationExportText.text"));
                                            labelOperationExportText.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationExportText, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationExportText ----
                                            textFieldOperationExportText.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationExportText.setName("exportText");
                                            textFieldOperationExportText.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationExportText.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationExportText, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
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
                                            buttonOperationExportTextFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationExportTextFilePick, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer9 ----
                                            vSpacer9.setMinimumSize(new Dimension(10, 30));
                                            vSpacer9.setPreferredSize(new Dimension(20, 30));
                                            vSpacer9.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer9, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationNoBackfill ----
                                            labelOperationNoBackfill.setText(guiContext.cfg.gs("Operations.labelOperationNoBackfill.text"));
                                            labelOperationNoBackfill.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationNoBackfill, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationNoBackFill ----
                                            checkBoxOperationNoBackFill.setName("noBackFill");
                                            checkBoxOperationNoBackFill.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationNoBackFill, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationExportItems ----
                                            labelOperationExportItems.setText(guiContext.cfg.gs("Operations.labelOperationExportItems.text"));
                                            labelOperationExportItems.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationExportItems, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationExportItems ----
                                            textFieldOperationExportItems.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationExportItems.setName("exportItems");
                                            textFieldOperationExportItems.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationExportItems.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationExportItems, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
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
                                            buttonOperationExportItemsFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationExportItemsFilePick, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer10 ----
                                            vSpacer10.setMinimumSize(new Dimension(10, 30));
                                            vSpacer10.setPreferredSize(new Dimension(20, 30));
                                            vSpacer10.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer10, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationOverwrite ----
                                            labelOperationOverwrite.setText(guiContext.cfg.gs("Operations.labelOperationOverwrite.text"));
                                            labelOperationOverwrite.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationOverwrite, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationOverwrite ----
                                            checkBoxOperationOverwrite.setName("overwrite");
                                            checkBoxOperationOverwrite.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationOverwrite, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer11 ----
                                            vSpacer11.setMinimumSize(new Dimension(10, 30));
                                            vSpacer11.setPreferredSize(new Dimension(20, 30));
                                            vSpacer11.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer11, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationPreservedDates ----
                                            labelOperationPreservedDates.setText(guiContext.cfg.gs("Operations.labelOperationPreservedDates.text"));
                                            labelOperationPreservedDates.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationPreservedDates, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationPreserveDates ----
                                            checkBoxOperationPreserveDates.setName("preserveDates");
                                            checkBoxOperationPreserveDates.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationPreserveDates, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationHintKeys ----
                                            comboBoxOperationHintKeys.setPrototypeDisplayValue(guiContext.cfg.gs("Operations.comboBoxOperationHintKeys.prototypeDisplayValue"));
                                            comboBoxOperationHintKeys.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Hint keys:",
                                                "Hint keys, only:"
                                            }));
                                            comboBoxOperationHintKeys.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationHintKeys.setName("keys");
                                            comboBoxOperationHintKeys.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationHintKeys, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHintKeys ----
                                            textFieldOperationHintKeys.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHintKeys.setName("hintKeys");
                                            textFieldOperationHintKeys.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHintKeys.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationHintKeys, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
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
                                            buttonOperationHintKeysFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationHintKeysFilePick, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer19 ----
                                            vSpacer19.setMinimumSize(new Dimension(10, 30));
                                            vSpacer19.setPreferredSize(new Dimension(20, 30));
                                            vSpacer19.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer19, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDecimalScale ----
                                            labelOperationDecimalScale.setText(guiContext.cfg.gs("Operations.labelOperationDecimalScale.text"));
                                            labelOperationDecimalScale.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationDecimalScale, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDecimalScale ----
                                            checkBoxOperationDecimalScale.setName("decimalScale");
                                            checkBoxOperationDecimalScale.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDecimalScale, new GridBagConstraints(5, 8, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationHintsAndServer ----
                                            comboBoxOperationHintsAndServer.setPrototypeDisplayValue(guiContext.cfg.gs("Operations.comboBoxOperationHintsAndServer.prototypeDisplayValue"));
                                            comboBoxOperationHintsAndServer.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Hints:",
                                                "Hint Server:"
                                            }));
                                            comboBoxOperationHintsAndServer.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationHintsAndServer.setName("hints");
                                            comboBoxOperationHintsAndServer.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationHintsAndServer, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationHints ----
                                            textFieldOperationHints.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationHints.setName("hints");
                                            textFieldOperationHints.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationHints.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationHints, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
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
                                            buttonOperationHintsFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationHintsFilePick, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer18 ----
                                            vSpacer18.setMinimumSize(new Dimension(10, 30));
                                            vSpacer18.setPreferredSize(new Dimension(20, 30));
                                            vSpacer18.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer18, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationValidate ----
                                            labelOperationValidate.setText(guiContext.cfg.gs("Operations.labelOperationValidate.text"));
                                            labelOperationValidate.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationValidate, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationValidate ----
                                            checkBoxOperationValidate.setName("validate");
                                            checkBoxOperationValidate.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationValidate, new GridBagConstraints(5, 9, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationQuitStatusServer ----
                                            labelOperationQuitStatusServer.setText(guiContext.cfg.gs("Operations.labelOperationQuitStatusServer.text"));
                                            labelOperationQuitStatusServer.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationQuitStatusServer, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationQuitStatus ----
                                            checkBoxOperationQuitStatus.setName("quitStatusServer");
                                            checkBoxOperationQuitStatus.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationQuitStatus, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer17 ----
                                            vSpacer17.setMinimumSize(new Dimension(10, 30));
                                            vSpacer17.setPreferredSize(new Dimension(20, 30));
                                            vSpacer17.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer17, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationKeepGoing ----
                                            labelOperationKeepGoing.setText(guiContext.cfg.gs("Operations.labelOperationKeepGoing.text"));
                                            labelOperationKeepGoing.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationKeepGoing, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationKeepGoing ----
                                            checkBoxOperationKeepGoing.setName("keepGoing");
                                            checkBoxOperationKeepGoing.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationKeepGoing, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer16 ----
                                            vSpacer16.setMinimumSize(new Dimension(10, 30));
                                            vSpacer16.setPreferredSize(new Dimension(20, 30));
                                            vSpacer16.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer16, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationDuplicates ----
                                            labelOperationDuplicates.setText(guiContext.cfg.gs("Operations.labelOperationDuplicates.text"));
                                            labelOperationDuplicates.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationDuplicates, new GridBagConstraints(4, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationDuplicates ----
                                            checkBoxOperationDuplicates.setName("duplicates");
                                            checkBoxOperationDuplicates.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationDuplicates, new GridBagConstraints(5, 11, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer15 ----
                                            vSpacer15.setMinimumSize(new Dimension(10, 30));
                                            vSpacer15.setPreferredSize(new Dimension(20, 30));
                                            vSpacer15.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer15, new GridBagConstraints(3, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationCrossCheck ----
                                            labelOperationCrossCheck.setText(guiContext.cfg.gs("Operations.labelOperationCrossCheck.text"));
                                            labelOperationCrossCheck.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationCrossCheck, new GridBagConstraints(4, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 8, 4, 4), 0, 0));

                                            //---- checkBoxOperationCrossCheck ----
                                            checkBoxOperationCrossCheck.setName("crossCheck");
                                            checkBoxOperationCrossCheck.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationCrossCheck, new GridBagConstraints(5, 12, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- comboBoxOperationLog ----
                                            comboBoxOperationLog.setPrototypeDisplayValue(guiContext.cfg.gs("Operations.comboBoxOperationLog.prototypeDisplayValue"));
                                            comboBoxOperationLog.setModel(new DefaultComboBoxModel<>(new String[] {
                                                "Log:",
                                                "Log, overwrite:"
                                            }));
                                            comboBoxOperationLog.setMinimumSize(new Dimension(60, 30));
                                            comboBoxOperationLog.setName("log");
                                            comboBoxOperationLog.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(comboBoxOperationLog, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- textFieldOperationLog ----
                                            textFieldOperationLog.setMinimumSize(new Dimension(60, 30));
                                            textFieldOperationLog.setName("log");
                                            textFieldOperationLog.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    guiContext.operations.genericTextFieldFocusLost(e);
                                                }
                                            });
                                            textFieldOperationLog.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(textFieldOperationLog, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- buttonOperationLogFilePick ----
                                            buttonOperationLogFilePick.setText("...");
                                            buttonOperationLogFilePick.setFont(buttonOperationLogFilePick.getFont().deriveFont(buttonOperationLogFilePick.getFont().getStyle() | Font.BOLD));
                                            buttonOperationLogFilePick.setMaximumSize(new Dimension(32, 24));
                                            buttonOperationLogFilePick.setMinimumSize(new Dimension(32, 24));
                                            buttonOperationLogFilePick.setPreferredSize(new Dimension(32, 24));
                                            buttonOperationLogFilePick.setVerticalTextPosition(SwingConstants.TOP);
                                            buttonOperationLogFilePick.setIconTextGap(0);
                                            buttonOperationLogFilePick.setHorizontalTextPosition(SwingConstants.LEADING);
                                            buttonOperationLogFilePick.setActionCommand("mismatchesFilePick");
                                            panelCardPublisher.add(buttonOperationLogFilePick, new GridBagConstraints(2, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer14 ----
                                            vSpacer14.setMinimumSize(new Dimension(10, 30));
                                            vSpacer14.setPreferredSize(new Dimension(20, 30));
                                            vSpacer14.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer14, new GridBagConstraints(3, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationEmptyDirectories ----
                                            labelOperationEmptyDirectories.setText(guiContext.cfg.gs("Operations.labelOperationEmptyDirectories.text"));
                                            labelOperationEmptyDirectories.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationEmptyDirectories, new GridBagConstraints(4, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationEmptyDirectories ----
                                            checkBoxOperationEmptyDirectories.setName("emptyDirectories");
                                            checkBoxOperationEmptyDirectories.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationEmptyDirectories, new GridBagConstraints(5, 13, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationLogLevels ----
                                            labelOperationLogLevels.setText(guiContext.cfg.gs("Operations.labelOperationLogLevels.text"));
                                            labelOperationLogLevels.setMinimumSize(new Dimension(60, 16));
                                            labelOperationLogLevels.setPreferredSize(new Dimension(138, 16));
                                            panelCardPublisher.add(labelOperationLogLevels, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //======== panelOperationLogLevels ========
                                            {
                                                panelOperationLogLevels.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));

                                                //---- comboBoxOperationConsoleLevel ----
                                                comboBoxOperationConsoleLevel.setModel(new DefaultComboBoxModel<>(new String[] {
                                                    "All",
                                                    "Trace",
                                                    "Debug",
                                                    "Info",
                                                    "Warn",
                                                    "Error",
                                                    "Fatal",
                                                    "Off"
                                                }));
                                                comboBoxOperationConsoleLevel.setSelectedIndex(3);
                                                comboBoxOperationConsoleLevel.setName("consolelevel");
                                                comboBoxOperationConsoleLevel.addActionListener(e -> guiContext.operations.genericAction(e));
                                                panelOperationLogLevels.add(comboBoxOperationConsoleLevel);

                                                //---- comboBoxOperationDebugLevel ----
                                                comboBoxOperationDebugLevel.setModel(new DefaultComboBoxModel<>(new String[] {
                                                    "All",
                                                    "Trace",
                                                    "Debug",
                                                    "Info",
                                                    "Warn",
                                                    "Error",
                                                    "Fatal",
                                                    "Off"
                                                }));
                                                comboBoxOperationDebugLevel.setSelectedIndex(2);
                                                comboBoxOperationDebugLevel.setName("debuglevel");
                                                comboBoxOperationDebugLevel.addActionListener(e -> guiContext.operations.genericAction(e));
                                                panelOperationLogLevels.add(comboBoxOperationDebugLevel);
                                            }
                                            panelCardPublisher.add(panelOperationLogLevels, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- vSpacer13 ----
                                            vSpacer13.setMinimumSize(new Dimension(10, 30));
                                            vSpacer13.setPreferredSize(new Dimension(20, 30));
                                            vSpacer13.setMaximumSize(new Dimension(20, 30));
                                            panelCardPublisher.add(vSpacer13, new GridBagConstraints(3, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- labelOperationIgnored ----
                                            labelOperationIgnored.setText(guiContext.cfg.gs("Operations.labelOperationIgnored.text"));
                                            labelOperationIgnored.setPreferredSize(new Dimension(138, 16));
                                            labelOperationIgnored.setMinimumSize(new Dimension(60, 16));
                                            panelCardPublisher.add(labelOperationIgnored, new GridBagConstraints(4, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));

                                            //---- checkBoxOperationIgnored ----
                                            checkBoxOperationIgnored.setName("ignored");
                                            checkBoxOperationIgnored.addActionListener(e -> guiContext.operations.genericAction(e));
                                            panelCardPublisher.add(checkBoxOperationIgnored, new GridBagConstraints(5, 14, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 4, 4), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardPublisher, "publisher");

                                        //======== panelCardListener ========
                                        {
                                            panelCardListener.setName("listener");
                                            panelCardListener.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardListener.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardListener.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- label1 ----
                                            label1.setText("Listener card");
                                            panelCardListener.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardListener, "listener");

                                        //======== panelCardQuit ========
                                        {
                                            panelCardQuit.setName("quit");
                                            panelCardQuit.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardQuit.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardQuit.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- label2 ----
                                            label2.setText("Quit card");
                                            panelCardQuit.add(label2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardQuit, "quit");

                                        //======== panelCardTerminal ========
                                        {
                                            panelCardTerminal.setName("terminal");
                                            panelCardTerminal.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelCardTerminal.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                            ((GridBagLayout)panelCardTerminal.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                            //---- label3 ----
                                            label3.setText("Terminal card");
                                            panelCardTerminal.add(label3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOperationCards.add(panelCardTerminal, "terminal");
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
                            panelOperationBottom.setLayout(new BorderLayout());
                            panelOperationBottom.add(labelOperationStatus, BorderLayout.CENTER);

                            //======== panelOperationBottomButtons ========
                            {
                                panelOperationBottomButtons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

                                //---- buttonOperationSave ----
                                buttonOperationSave.setText(guiContext.cfg.gs("Z.save"));
                                buttonOperationSave.setToolTipText(guiContext.cfg.gs("Z.save.toolTip.text"));
                                panelOperationBottomButtons.add(buttonOperationSave);

                                //---- buttonOperationCancel ----
                                buttonOperationCancel.setText(guiContext.cfg.gs("Z.cancel"));
                                buttonOperationCancel.setToolTipText(guiContext.cfg.gs("Z.cancel.changes.toolTipText"));
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
                            textAreaOperationLog.setFont(new Font("Ubuntu Mono", Font.PLAIN, 14));
                            scrollPaneOperationLog.setViewportView(textAreaOperationLog);
                        }
                        tabbedPaneOperationBottom.addTab(guiContext.cfg.gs("Operations.scrollPaneLog.tab.title"), scrollPaneOperationLog);
                        tabbedPaneOperationBottom.setMnemonicAt(0, guiContext.cfg.gs("Operations.scrollPaneLog.tab.mnemonic").charAt(0));
                    }
                    splitPaneOperation.setBottomComponent(tabbedPaneOperationBottom);
                }
                tabbedPaneMain.addTab(guiContext.cfg.gs("Navigator.splitPane.Operations.tab.title"), splitPaneOperation);
                tabbedPaneMain.setMnemonicAt(1, guiContext.cfg.gs("Navigator.splitPaneOperations.tab.mnemonic").charAt(0));

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
        setSize(1024, 835);
        setLocationRelativeTo(getOwner());

        //======== popupMenuBrowser ========
        {

            //---- popupMenuItemRefresh ----
            popupMenuItemRefresh.setText(guiContext.cfg.gs("Navigator.popupMenuItemRefresh.text"));
            popupMenuItemRefresh.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemRefresh.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuItemRefresh.setMnemonic(guiContext.cfg.gs("Navigator.popupMenuItemRefresh.mnemonic").charAt(0));
            popupMenuBrowser.add(popupMenuItemRefresh);
            popupMenuBrowser.addSeparator();

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

            //---- popupMenuItemTop ----
            popupMenuItemTop.setText(guiContext.cfg.gs("Navigator.popupMenuItemTop.text"));
            popupMenuItemTop.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemTop.setMnemonic(guiContext.cfg.gs("Navigator.popupMenuItemTop.mnemonic").charAt(0));
            popupMenuItemTop.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuLog.add(popupMenuItemTop);

            //---- popupMenuItemClear ----
            popupMenuItemClear.setText(guiContext.cfg.gs("Navigator.popupMenu.Clear.text"));
            popupMenuItemClear.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemClear.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Clear.mnemonic").charAt(0));
            popupMenuItemClear.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuLog.add(popupMenuItemClear);

            //---- popupMenuItemBottom ----
            popupMenuItemBottom.setText(guiContext.cfg.gs("Navigator.popupMenu.Bottom.text"));
            popupMenuItemBottom.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemBottom.setMnemonic(guiContext.cfg.gs("Navigator.popupMenu.Bottom.mnemonic").charAt(0));
            popupMenuItemBottom.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuLog.add(popupMenuItemBottom);
        }

        //======== popupMenuOperationLog ========
        {

            //---- popupMenuItemOperationTop ----
            popupMenuItemOperationTop.setText(guiContext.cfg.gs("Operations.popupMenuItemOperationTop.text"));
            popupMenuItemOperationTop.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemOperationTop.setMnemonic(guiContext.cfg.gs("Navigator.popupMenuItemOperationTop.mnemonic").charAt(0));
            popupMenuItemOperationTop.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuOperationLog.add(popupMenuItemOperationTop);

            //---- popupMenuItemOperationClear ----
            popupMenuItemOperationClear.setText(guiContext.cfg.gs("Navigator.popupMenuItemOperationClear.text"));
            popupMenuItemOperationClear.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemOperationClear.setMnemonic(guiContext.cfg.gs("Navigator.popupMenuItemOperationClear.mnemonic").charAt(0));
            popupMenuItemOperationClear.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuOperationLog.add(popupMenuItemOperationClear);

            //---- popupMenuItemOperationBottom ----
            popupMenuItemOperationBottom.setText(guiContext.cfg.gs("Navigator.popupMenuItemOperationBottom.text"));
            popupMenuItemOperationBottom.setHorizontalAlignment(SwingConstants.LEFT);
            popupMenuItemOperationBottom.setMnemonic(guiContext.cfg.gs("Navigator.popupMenuItemOperationBottom.mnemonic").charAt(0));
            popupMenuItemOperationBottom.setHorizontalTextPosition(SwingConstants.LEFT);
            popupMenuOperationLog.add(popupMenuItemOperationBottom);
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
    public JMenuItem menuItemCopy;
    public JMenuItem menuItemCut;
    public JMenuItem menuItemPaste;
    public JMenuItem menuItemDelete;
    public JMenuItem menuItemFind;
    public JMenuItem menuItemFindNext;
    public JMenuItem menuItemNewFolder;
    public JMenuItem menuItemRename;
    public JMenuItem menuItemTouch;
    public JMenu menuView;
    public JMenuItem menuItemRefresh;
    public JMenuItem menuItemProgress;
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
    public JMenuItem menuItem1;
    public JMenuItem menuItemPlexGenerator;
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
    public JPanel panelCardPublisher;
    public JLabel labelOperationNavigatorCheckbox;
    public JCheckBox checkBoxOperationNavigator;
    public JPanel vSpacer3;
    public JPanel panelOperationIncludeExcludeBox;
    public JPanel vSpacer8;
    public JScrollPane scrollPaneOperationIncludeExclude;
    public JList listOperationIncludeExclude;
    public JPanel panelOperationIncludeExcludeButtons;
    public JButton buttonOperationAddIncludeExclude;
    public JButton buttonOperationRemoveIncludeExclude;
    public JLabel labelOperationIncludeExclude;
    public JLabel labelOperationJob;
    public JTextField textFieldOperationJob;
    public JButton buttonOperationJobFilePick;
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
    public JLabel labelOperationDryRun;
    public JCheckBox checkBoxOperationDryRun;
    public JLabel labelOperationExportText;
    public JTextField textFieldOperationExportText;
    public JButton buttonOperationExportTextFilePick;
    public JPanel vSpacer9;
    public JLabel labelOperationNoBackfill;
    public JCheckBox checkBoxOperationNoBackFill;
    public JLabel labelOperationExportItems;
    public JTextField textFieldOperationExportItems;
    public JButton buttonOperationExportItemsFilePick;
    public JPanel vSpacer10;
    public JLabel labelOperationOverwrite;
    public JCheckBox checkBoxOperationOverwrite;
    public JPanel vSpacer11;
    public JLabel labelOperationPreservedDates;
    public JCheckBox checkBoxOperationPreserveDates;
    public JComboBox<String> comboBoxOperationHintKeys;
    public JTextField textFieldOperationHintKeys;
    public JButton buttonOperationHintKeysFilePick;
    public JPanel vSpacer19;
    public JLabel labelOperationDecimalScale;
    public JCheckBox checkBoxOperationDecimalScale;
    public JComboBox<String> comboBoxOperationHintsAndServer;
    public JTextField textFieldOperationHints;
    public JButton buttonOperationHintsFilePick;
    public JPanel vSpacer18;
    public JLabel labelOperationValidate;
    public JCheckBox checkBoxOperationValidate;
    public JLabel labelOperationQuitStatusServer;
    public JCheckBox checkBoxOperationQuitStatus;
    public JPanel vSpacer17;
    public JLabel labelOperationKeepGoing;
    public JCheckBox checkBoxOperationKeepGoing;
    public JPanel vSpacer16;
    public JLabel labelOperationDuplicates;
    public JCheckBox checkBoxOperationDuplicates;
    public JPanel vSpacer15;
    public JLabel labelOperationCrossCheck;
    public JCheckBox checkBoxOperationCrossCheck;
    public JComboBox<String> comboBoxOperationLog;
    public JTextField textFieldOperationLog;
    public JButton buttonOperationLogFilePick;
    public JPanel vSpacer14;
    public JLabel labelOperationEmptyDirectories;
    public JCheckBox checkBoxOperationEmptyDirectories;
    public JLabel labelOperationLogLevels;
    public JPanel panelOperationLogLevels;
    public JComboBox<String> comboBoxOperationConsoleLevel;
    public JComboBox<String> comboBoxOperationDebugLevel;
    public JPanel vSpacer13;
    public JLabel labelOperationIgnored;
    public JCheckBox checkBoxOperationIgnored;
    public JPanel panelCardListener;
    public JLabel label1;
    public JPanel panelCardQuit;
    public JLabel label2;
    public JPanel panelCardTerminal;
    public JLabel label3;
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
    public JMenuItem popupMenuItemNewFolder;
    public JMenuItem popupMenuItemRename;
    public JMenuItem popupMenuItemTouch;
    public JMenuItem popupMenuItemCopy;
    public JMenuItem popupMenuItemCut;
    public JMenuItem popupMenuItemPaste;
    public JMenuItem popupMenuItemDelete;
    public JPopupMenu popupMenuLog;
    public JMenuItem popupMenuItemTop;
    public JMenuItem popupMenuItemClear;
    public JMenuItem popupMenuItemBottom;
    public JPopupMenu popupMenuOperationLog;
    public JMenuItem popupMenuItemOperationTop;
    public JMenuItem popupMenuItemOperationClear;
    public JMenuItem popupMenuItemOperationBottom;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
