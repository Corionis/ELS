package com.groksoft.els.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

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
    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private GuiContext guiContext;
    private LookAndFeel laf;

    public MainFrame(GuiContext guiContext)
    {
        this.guiContext = guiContext;

        try
        {
            if (guiContext.preferences.getLafStyle() == 0)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            else
            {
                laf = getLookAndFeel();
                UIManager.setLookAndFeel(laf);
            }

            initComponents();

            // change browser tabs orientation to vertical
            JLabel label = new JLabel(bundle.getString("Navigator.panelCollectionOne.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneBrowserOne.setTabComponentAt(0, label);
            //
            label = new JLabel(bundle.getString("Navigator.panelSystemOne.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneBrowserOne.setTabComponentAt(1, label);

            label = new JLabel(bundle.getString("Navigator.panelCollectionTwo.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneBrowserTwo.setTabComponentAt(0, label);
            //
            label = new JLabel(bundle.getString("Navigator.panelSystemTwo.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneBrowserTwo.setTabComponentAt(1, label);

            // setup the right-side tables
            tableCollectionOne.setName("tableCollectionOne");
            tableCollectionOne.setAutoCreateRowSorter(true);
            tableCollectionOne.setShowGrid(false);
            tableCollectionOne.getTableHeader().setReorderingAllowed(false);
            tableCollectionOne.setRowSelectionAllowed(true);
            tableCollectionOne.setColumnSelectionAllowed(false);

            tableSystemOne.setName("tableSystemOne");
            tableSystemOne.setAutoCreateRowSorter(true);
            tableSystemOne.setShowGrid(false);
            tableSystemOne.getTableHeader().setReorderingAllowed(false);
            tableSystemOne.setRowSelectionAllowed(true);
            tableSystemOne.setColumnSelectionAllowed(false);

            tableCollectionTwo.setName("tableCollectionTwo");
            tableCollectionTwo.setAutoCreateRowSorter(true);
            tableCollectionTwo.setShowGrid(false);
            tableCollectionTwo.getTableHeader().setReorderingAllowed(false);
            tableCollectionTwo.setRowSelectionAllowed(true);
            tableCollectionTwo.setColumnSelectionAllowed(false);

            tableSystemTwo.setName("tableSystemTwo");
            tableSystemTwo.setAutoCreateRowSorter(true);
            tableSystemTwo.setShowGrid(false);
            tableSystemTwo.getTableHeader().setReorderingAllowed(false);
            tableSystemTwo.setRowSelectionAllowed(true);
            tableSystemTwo.setColumnSelectionAllowed(false);

            // set Back/Forward keys
            buttonBack.setMnemonic(KeyEvent.VK_LEFT);
            buttonForward.setMnemonic(KeyEvent.VK_RIGHT);

            // add smart scroll to the log
            new SmartScroller(scrollPaneLog);

            pack();
        }
        catch(Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            guiContext.context.fault = true;
        }
    }

    private LookAndFeel getLookAndFeel()
    {
        switch (guiContext.preferences.getLafStyle())
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
        guiContext.navigator.stop();
    }

    private void SaveActionPerformed(ActionEvent e)
    {
        // something
    }

    private void thisWindowClosing(WindowEvent e)
    {
        guiContext.navigator.stop();
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        menuBarMain = new JMenuBar();
        menuFile = new JMenu();
        menuItemNew = new JMenuItem();
        menuItemOpen = new JMenuItem();
        menuItemSave = new JMenuItem();
        menuItemSaveAll = new JMenuItem();
        menuItemSaveLayout = new JMenuItem();
        menuItemFileQuit = new JMenuItem();
        menuEdit = new JMenu();
        menuItemFind = new JMenuItem();
        menuItemCopy = new JMenuItem();
        menuItemCut = new JMenuItem();
        menuItemPaste = new JMenuItem();
        menuItemPreferences = new JMenuItem();
        menuView = new JMenu();
        menuItemRefresh = new JMenuItem();
        menuItemShowHidden = new JCheckBoxMenuItem();
        menuBookmarks = new JMenu();
        menuItemShowAllBookmarks = new JMenuItem();
        menuItemAddBookmark = new JMenuItem();
        menuTools = new JMenu();
        menuItemDuplicates = new JMenuItem();
        menuItemUuidGenerator = new JMenuItem();
        menuItemJunk = new JMenuItem();
        menuItemPlexGenerator = new JMenuItem();
        menuItemTouch = new JMenuItem();
        menuItemExternalTools = new JMenuItem();
        menuRunSubMenu = new JMenu();
        menuWindows = new JMenu();
        menuItemMaximize = new JMenuItem();
        menuItemMinimize = new JMenuItem();
        menuItemRestore = new JMenuItem();
        menuItemSplitHorizontal = new JMenuItem();
        menuItemSplitVertical = new JMenuItem();
        menuHelp = new JMenu();
        menuItemDocumentation = new JMenuItem();
        menuItemGitHubProject = new JMenuItem();
        menuItemAbout = new JMenuItem();
        panelMain = new JPanel();
        tabbedPaneMain = new JTabbedPane();
        splitPaneBrowser = new JSplitPane();
        panelBrowserTop = new JPanel();
        panelLocationAndButtons = new JPanel();
        toolBarBrowser = new JToolBar();
        buttonCreate = new JButton();
        buttonCopy = new JButton();
        buttonMove = new JButton();
        buttonRename = new JButton();
        buttonDelete = new JButton();
        panelLocation = new JPanel();
        panelLocationLeft = new JPanel();
        buttonBack = new JButton();
        buttonForward = new JButton();
        textFieldLocation = new JTextField();
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
        textAreaProperties = new JTextArea();
        splitPaneBackup = new JSplitPane();
        panelLibraries = new JPanel();
        panelStatus = new JPanel();
        labelStatusLeft = new JLabel();
        labelStatusMiddle = new JLabel();
        labelStatusRight = new JLabel();

        //======== this ========
        setMinimumSize(new Dimension(100, 100));
        setTitle("ELS Navigator");
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
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
                menuFile.setText(bundle.getString("Navigator.menuFile.text"));
                menuFile.setMnemonic(bundle.getString("Navigator.menuFile.mnemonic").charAt(0));

                //---- menuItemNew ----
                menuItemNew.setText(bundle.getString("Navigator.menuItemNew.text"));
                menuItemNew.setMnemonic(bundle.getString("Navigator.menuItemNew.mnemonic").charAt(0));
                menuItemNew.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemNew.setEnabled(false);
                menuItemNew.setHorizontalAlignment(SwingConstants.LEFT);
                menuFile.add(menuItemNew);

                //---- menuItemOpen ----
                menuItemOpen.setText(bundle.getString("Navigator.menuItemOpen.text"));
                menuItemOpen.setMnemonic(bundle.getString("Navigator.menuItemOpen.mnemonic").charAt(0));
                menuItemOpen.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemOpen.setEnabled(false);
                menuItemOpen.setHorizontalAlignment(SwingConstants.LEFT);
                menuFile.add(menuItemOpen);

                //---- menuItemSave ----
                menuItemSave.setText(bundle.getString("Navigator.menuItemSave.text"));
                menuItemSave.setMnemonic(bundle.getString("Navigator.menuItemSave.mnemonic").charAt(0));
                menuItemSave.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSave.setEnabled(false);
                menuItemSave.setHorizontalAlignment(SwingConstants.LEFT);
                menuFile.add(menuItemSave);

                //---- menuItemSaveAll ----
                menuItemSaveAll.setText(bundle.getString("Navigator.menuItemSaveAll.text"));
                menuItemSaveAll.setMnemonic(bundle.getString("Navigator.menuItemSaveAll.mnemonic").charAt(0));
                menuItemSaveAll.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSaveAll.setEnabled(false);
                menuItemSaveAll.setHorizontalAlignment(SwingConstants.LEFT);
                menuFile.add(menuItemSaveAll);
                menuFile.addSeparator();

                //---- menuItemSaveLayout ----
                menuItemSaveLayout.setText(bundle.getString("Navigator.menuItemSaveLayout.text"));
                menuItemSaveLayout.setMnemonic(bundle.getString("Navigator.menuItemSaveLayout.mnemonic_3").charAt(0));
                menuItemSaveLayout.setEnabled(false);
                menuItemSaveLayout.setHorizontalTextPosition(SwingConstants.LEFT);
                menuFile.add(menuItemSaveLayout);
                menuFile.addSeparator();

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(bundle.getString("Navigator.menuItemFileQuit.text"));
                menuItemFileQuit.setMnemonic(bundle.getString("Navigator.menuItemFileQuit.mnemonic").charAt(0));
                menuItemFileQuit.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemFileQuit.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFileQuit.addActionListener(e -> menuItemFileQuitActionPerformed(e));
                menuFile.add(menuItemFileQuit);
            }
            menuBarMain.add(menuFile);

            //======== menuEdit ========
            {
                menuEdit.setText(bundle.getString("Navigator.menuEdit.text"));
                menuEdit.setMnemonic(bundle.getString("Navigator.menuEdit.mnemonic").charAt(0));

                //---- menuItemFind ----
                menuItemFind.setText(bundle.getString("Navigator.menuItemFind.text"));
                menuItemFind.setMnemonic(bundle.getString("Navigator.menuItemFind.mnemonic").charAt(0));
                menuItemFind.setEnabled(false);
                menuItemFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
                menuItemFind.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemFind.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemFind);
                menuEdit.addSeparator();

                //---- menuItemCopy ----
                menuItemCopy.setText(bundle.getString("Navigator.menuItemCopy.text"));
                menuItemCopy.setMnemonic(bundle.getString("Navigator.menuItemCopy.mnemonic").charAt(0));
                menuItemCopy.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
                menuItemCopy.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(bundle.getString("Navigator.menuItemCut.text"));
                menuItemCut.setMnemonic(bundle.getString("Navigator.menuItemCut.mnemonic_2").charAt(0));
                menuItemCut.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK));
                menuItemCut.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(bundle.getString("Navigator.menuItemPaste.text"));
                menuItemPaste.setMnemonic(bundle.getString("Navigator.menuItemPaste.mnemonic_2").charAt(0));
                menuItemPaste.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
                menuItemPaste.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemPreferences ----
                menuItemPreferences.setText(bundle.getString("Navigator.menuItemPreferences.text"));
                menuItemPreferences.setMnemonic(bundle.getString("Navigator.menuItemPreferences.mnemonic_2").charAt(0));
                menuItemPreferences.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPreferences.setEnabled(false);
                menuItemPreferences.setHorizontalAlignment(SwingConstants.LEFT);
                menuEdit.add(menuItemPreferences);
            }
            menuBarMain.add(menuEdit);

            //======== menuView ========
            {
                menuView.setText(bundle.getString("Navigator.menuView.text"));
                menuView.setMnemonic(bundle.getString("Navigator.menuView.mnemonic").charAt(0));
                menuView.setSelectedIcon(null);

                //---- menuItemRefresh ----
                menuItemRefresh.setText(bundle.getString("Navigator.menuItemRefresh.text"));
                menuItemRefresh.setMnemonic(bundle.getString("Navigator.menuItemRefresh.mnemonic").charAt(0));
                menuItemRefresh.setEnabled(false);
                menuItemRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
                menuItemRefresh.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemRefresh.setHorizontalTextPosition(SwingConstants.LEFT);
                menuView.add(menuItemRefresh);
                menuView.addSeparator();

                //---- menuItemShowHidden ----
                menuItemShowHidden.setText(bundle.getString("Navigator.menuItemShowHidden.text"));
                menuItemShowHidden.setMnemonic(bundle.getString("Navigator.menuItemShowHidden.mnemonic").charAt(0));
                menuItemShowHidden.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK));
                menuItemShowHidden.setHorizontalAlignment(SwingConstants.LEFT);
                menuItemShowHidden.setHorizontalTextPosition(SwingConstants.LEFT);
                menuView.add(menuItemShowHidden);
            }
            menuBarMain.add(menuView);

            //======== menuBookmarks ========
            {
                menuBookmarks.setText(bundle.getString("Navigator.menuBookmarks.text"));
                menuBookmarks.setMnemonic(bundle.getString("Navigator.menuBookmarks.mnemonic").charAt(0));

                //---- menuItemShowAllBookmarks ----
                menuItemShowAllBookmarks.setText(bundle.getString("Navigator.menuItemShowAllBookmarks.text"));
                menuItemShowAllBookmarks.setMnemonic(bundle.getString("Navigator.menuItemShowAllBookmarks.mnemonic").charAt(0));
                menuItemShowAllBookmarks.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemShowAllBookmarks.setEnabled(false);
                menuItemShowAllBookmarks.setHorizontalAlignment(SwingConstants.LEFT);
                menuBookmarks.add(menuItemShowAllBookmarks);

                //---- menuItemAddBookmark ----
                menuItemAddBookmark.setText(bundle.getString("Navigator.menuItemAddBookmark.text"));
                menuItemAddBookmark.setMnemonic(bundle.getString("Navigator.menuItemAddBookmark.mnemonic").charAt(0));
                menuItemAddBookmark.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAddBookmark.setEnabled(false);
                menuItemAddBookmark.setHorizontalAlignment(SwingConstants.LEFT);
                menuBookmarks.add(menuItemAddBookmark);
                menuBookmarks.addSeparator();
            }
            menuBarMain.add(menuBookmarks);

            //======== menuTools ========
            {
                menuTools.setText(bundle.getString("Navigator.menuTools.text"));
                menuTools.setMnemonic(bundle.getString("Navigator.menuTools.mnemonic").charAt(0));

                //---- menuItemDuplicates ----
                menuItemDuplicates.setText(bundle.getString("Navigator.menuItemDuplicates.text"));
                menuItemDuplicates.setMnemonic(bundle.getString("Navigator.menuItemDuplicates.mnemonic").charAt(0));
                menuItemDuplicates.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDuplicates.setEnabled(false);
                menuItemDuplicates.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemDuplicates);

                //---- menuItemUuidGenerator ----
                menuItemUuidGenerator.setText(bundle.getString("Navigator.menuItemUuidGenerator.text"));
                menuItemUuidGenerator.setMnemonic(bundle.getString("Navigator.menuItemUuidGenerator.mnemonic_2").charAt(0));
                menuItemUuidGenerator.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemUuidGenerator.setEnabled(false);
                menuItemUuidGenerator.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemUuidGenerator);

                //---- menuItemJunk ----
                menuItemJunk.setText(bundle.getString("Navigator.menuItemJunk.text"));
                menuItemJunk.setMnemonic(bundle.getString("Navigator.menuItemJunk.mnemonic").charAt(0));
                menuItemJunk.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemJunk.setEnabled(false);
                menuItemJunk.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemJunk);

                //---- menuItemPlexGenerator ----
                menuItemPlexGenerator.setText(bundle.getString("Navigator.menuItemPlexGenerator.text"));
                menuItemPlexGenerator.setMnemonic(bundle.getString("Navigator.menuItemPlexGenerator.mnemonic").charAt(0));
                menuItemPlexGenerator.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPlexGenerator.setEnabled(false);
                menuItemPlexGenerator.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemPlexGenerator);

                //---- menuItemTouch ----
                menuItemTouch.setText(bundle.getString("Navigator.menuItemTouch.text"));
                menuItemTouch.setMnemonic(bundle.getString("Navigator.menuItemTouch.mnemonic").charAt(0));
                menuItemTouch.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemTouch.setEnabled(false);
                menuItemTouch.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemTouch);
                menuTools.addSeparator();

                //---- menuItemExternalTools ----
                menuItemExternalTools.setText(bundle.getString("Navigator.menuItemExternalTools.text"));
                menuItemExternalTools.setMnemonic(bundle.getString("Navigator.menuItemExternalTools.mnemonic_2").charAt(0));
                menuItemExternalTools.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemExternalTools.setEnabled(false);
                menuItemExternalTools.setHorizontalAlignment(SwingConstants.LEFT);
                menuTools.add(menuItemExternalTools);

                //======== menuRunSubMenu ========
                {
                    menuRunSubMenu.setText(bundle.getString("Navigator.menuRunSubMenu.text"));
                    menuRunSubMenu.setMnemonic(bundle.getString("Navigator.menuRunSubMenu.mnemonic").charAt(0));
                    menuRunSubMenu.setHorizontalTextPosition(SwingConstants.LEFT);
                    menuRunSubMenu.setEnabled(false);
                    menuRunSubMenu.setHorizontalAlignment(SwingConstants.LEFT);
                }
                menuTools.add(menuRunSubMenu);
            }
            menuBarMain.add(menuTools);

            //======== menuWindows ========
            {
                menuWindows.setText(bundle.getString("Navigator.menuWindows.text"));
                menuWindows.setMnemonic(bundle.getString("Navigator.menuWindows.mnemonic").charAt(0));

                //---- menuItemMaximize ----
                menuItemMaximize.setText(bundle.getString("Navigator.menuItemMaximize.text"));
                menuItemMaximize.setMnemonic(bundle.getString("Navigator.menuItemMaximize.mnemonic").charAt(0));
                menuItemMaximize.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemMaximize.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(bundle.getString("Navigator.menuItemMinimize.text"));
                menuItemMinimize.setMnemonic(bundle.getString("Navigator.menuItemMinimize.mnemonic_2").charAt(0));
                menuItemMinimize.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemMinimize.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemMinimize);

                //---- menuItemRestore ----
                menuItemRestore.setText(bundle.getString("Navigator.menuItemRestore.text"));
                menuItemRestore.setMnemonic(bundle.getString("Navigator.menuItemRestore.mnemonic_2").charAt(0));
                menuItemRestore.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemRestore.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemRestore);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(bundle.getString("Navigator.menuItemSplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(bundle.getString("Navigator.menuItemSplitHorizontal.mnemonic").charAt(0));
                menuItemSplitHorizontal.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSplitHorizontal.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(bundle.getString("Navigator.menuItemSplitVertical.text"));
                menuItemSplitVertical.setMnemonic(bundle.getString("Navigator.menuItemSplitVertical.mnemonic").charAt(0));
                menuItemSplitVertical.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSplitVertical.setHorizontalAlignment(SwingConstants.LEFT);
                menuWindows.add(menuItemSplitVertical);
            }
            menuBarMain.add(menuWindows);

            //======== menuHelp ========
            {
                menuHelp.setText(bundle.getString("Navigator.menuHelp.text"));
                menuHelp.setMnemonic(bundle.getString("Navigator.menuHelp.mnemonic").charAt(0));

                //---- menuItemDocumentation ----
                menuItemDocumentation.setText(bundle.getString("Navigator.menuItemDocumentation.text"));
                menuItemDocumentation.setMnemonic(bundle.getString("Navigator.menuItemDocumentation.mnemonic").charAt(0));
                menuItemDocumentation.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDocumentation.setHorizontalAlignment(SwingConstants.LEFT);
                menuHelp.add(menuItemDocumentation);

                //---- menuItemGitHubProject ----
                menuItemGitHubProject.setText(bundle.getString("Navigator.menuItemGitHubProject.text"));
                menuItemGitHubProject.setMnemonic(bundle.getString("Navigator.menuItemGitHubProject.mnemonic").charAt(0));
                menuItemGitHubProject.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemGitHubProject.setHorizontalAlignment(SwingConstants.LEFT);
                menuHelp.add(menuItemGitHubProject);
                menuHelp.addSeparator();

                //---- menuItemAbout ----
                menuItemAbout.setText(bundle.getString("Navigator.menuItemAbout.text"));
                menuItemAbout.setMnemonic(bundle.getString("Navigator.menuItemAbout.mnemonic").charAt(0));
                menuItemAbout.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAbout.setEnabled(false);
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
                    splitPaneBrowser.setLastDividerLocation(396);
                    splitPaneBrowser.setMinimumSize(new Dimension(0, 0));
                    splitPaneBrowser.setContinuousLayout(true);
                    splitPaneBrowser.setDividerLocation(396);

                    //======== panelBrowserTop ========
                    {
                        panelBrowserTop.setLayout(new BorderLayout());

                        //======== panelLocationAndButtons ========
                        {
                            panelLocationAndButtons.setLayout(new BorderLayout());

                            //======== toolBarBrowser ========
                            {
                                toolBarBrowser.setFloatable(false);
                                toolBarBrowser.setMargin(new Insets(0, 4, 0, 0));
                                toolBarBrowser.setRollover(true);
                                toolBarBrowser.setFocusable(false);

                                //---- buttonCreate ----
                                buttonCreate.setText(bundle.getString("Navigator.buttonCreate.text"));
                                buttonCreate.setToolTipText("Create directory");
                                buttonCreate.setEnabled(false);
                                toolBarBrowser.add(buttonCreate);
                                toolBarBrowser.addSeparator();

                                //---- buttonCopy ----
                                buttonCopy.setText(bundle.getString("Navigator.buttonCopy.text"));
                                buttonCopy.setToolTipText(bundle.getString("Navigator.buttonCopy.toolTipText"));
                                buttonCopy.setEnabled(false);
                                toolBarBrowser.add(buttonCopy);

                                //---- buttonMove ----
                                buttonMove.setText(bundle.getString("Navigator.buttonMove.text"));
                                buttonMove.setToolTipText(bundle.getString("Navigator.buttonMove.toolTipText"));
                                buttonMove.setEnabled(false);
                                toolBarBrowser.add(buttonMove);

                                //---- buttonRename ----
                                buttonRename.setText(bundle.getString("Navigator.buttonRename.text"));
                                buttonRename.setToolTipText(bundle.getString("Navigator.buttonRename.toolTipText"));
                                buttonRename.setEnabled(false);
                                toolBarBrowser.add(buttonRename);
                                toolBarBrowser.addSeparator();

                                //---- buttonDelete ----
                                buttonDelete.setText(bundle.getString("Navigator.buttonDelete.text"));
                                buttonDelete.setToolTipText(bundle.getString("Navigator.buttonDelete.toolTipText"));
                                buttonDelete.setEnabled(false);
                                toolBarBrowser.add(buttonDelete);
                            }
                            panelLocationAndButtons.add(toolBarBrowser, BorderLayout.NORTH);

                            //======== panelLocation ========
                            {
                                panelLocation.setLayout(new BorderLayout());

                                //======== panelLocationLeft ========
                                {
                                    panelLocationLeft.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelLocationLeft.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                                    ((GridBagLayout)panelLocationLeft.getLayout()).rowHeights = new int[] {0, 0};
                                    ((GridBagLayout)panelLocationLeft.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0, 1.0E-4};
                                    ((GridBagLayout)panelLocationLeft.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                                    //---- buttonBack ----
                                    buttonBack.setText("<html>&lt;</html>");
                                    buttonBack.setMaximumSize(new Dimension(36, 30));
                                    buttonBack.setMinimumSize(new Dimension(36, 30));
                                    buttonBack.setPreferredSize(new Dimension(36, 30));
                                    buttonBack.setToolTipText(bundle.getString("Navigator.buttonBack.toolTipText"));
                                    buttonBack.setActionCommand("navBack");
                                    buttonBack.setFocusable(false);
                                    panelLocationLeft.add(buttonBack, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonForward ----
                                    buttonForward.setText("<html>&gt;</html>");
                                    buttonForward.setMaximumSize(new Dimension(36, 30));
                                    buttonForward.setMinimumSize(new Dimension(36, 30));
                                    buttonForward.setPreferredSize(new Dimension(36, 30));
                                    buttonForward.setToolTipText(bundle.getString("Navigator.buttonForward.toolTipText"));
                                    buttonForward.setActionCommand("NavForward");
                                    buttonForward.setFocusable(false);
                                    panelLocationLeft.add(buttonForward, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                panelLocation.add(panelLocationLeft, BorderLayout.WEST);

                                //---- textFieldLocation ----
                                textFieldLocation.setPreferredSize(new Dimension(850, 30));
                                textFieldLocation.setHorizontalAlignment(SwingConstants.LEFT);
                                textFieldLocation.setEditable(false);
                                textFieldLocation.setToolTipText("Location");
                                textFieldLocation.setFocusable(false);
                                panelLocation.add(textFieldLocation, BorderLayout.CENTER);
                            }
                            panelLocationAndButtons.add(panelLocation, BorderLayout.SOUTH);
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
                                            tableCollectionOne.setCellSelectionEnabled(true);
                                            scrollPaneTableCollectionOne.setViewportView(tableCollectionOne);
                                        }
                                        splitPaneCollectionOne.setRightComponent(scrollPaneTableCollectionOne);
                                    }
                                    panelCollectionOne.add(splitPaneCollectionOne);
                                }
                                tabbedPaneBrowserOne.addTab(bundle.getString("Navigator.panelCollectionOne.tab.title"), panelCollectionOne);
                                tabbedPaneBrowserOne.setMnemonicAt(0, bundle.getString("Navigator.panelCollectionOne.tab.mnemonic").charAt(0));

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
                                            scrollPaneTableSystemOne.setViewportView(tableSystemOne);
                                        }
                                        splitPaneSystemOne.setRightComponent(scrollPaneTableSystemOne);
                                    }
                                    panelSystemOne.add(splitPaneSystemOne);
                                }
                                tabbedPaneBrowserOne.addTab(bundle.getString("Navigator.panelSystemOne.tab.title"), panelSystemOne);
                                tabbedPaneBrowserOne.setMnemonicAt(1, bundle.getString("Navigator.panelSystemOne.tab.mnemonic").charAt(0));
                            }
                            splitPaneTwoBrowsers.setLeftComponent(tabbedPaneBrowserOne);

                            //======== tabbedPaneBrowserTwo ========
                            {
                                tabbedPaneBrowserTwo.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserTwo.setPreferredSize(new Dimension(950, 427));
                                tabbedPaneBrowserTwo.setMinimumSize(new Dimension(0, 0));
                                tabbedPaneBrowserTwo.setAutoscrolls(true);
                                tabbedPaneBrowserTwo.setFocusable(false);

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
                                            scrollPaneTableCollectionTwo.setViewportView(tableCollectionTwo);
                                        }
                                        splitPaneCollectionTwo.setRightComponent(scrollPaneTableCollectionTwo);
                                    }
                                    panelCollectionTwo.add(splitPaneCollectionTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(bundle.getString("Navigator.panelCollectionTwo.tab.title"), panelCollectionTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(0, bundle.getString("Navigator.panelCollectionTwo.tab.mnemonic_2").charAt(0));

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
                                            scrollPaneTableSystemTwo.setViewportView(tableSystemTwo);
                                        }
                                        splitPaneSystemTwo.setRightComponent(scrollPaneTableSystemTwo);
                                    }
                                    panelSystemTwo.add(splitPaneSystemTwo);
                                }
                                tabbedPaneBrowserTwo.addTab(bundle.getString("Navigator.panelSystemTwo.tab.title"), panelSystemTwo);
                                tabbedPaneBrowserTwo.setMnemonicAt(1, bundle.getString("Navigator.panelSystemTwo.tab.mnemonic").charAt(0));
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
                            scrollPaneLog.setViewportView(textAreaLog);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneLog.tab.title"), scrollPaneLog);
                        tabbedPaneNavigatorBottom.setMnemonicAt(0, bundle.getString("Navigator.scrollPaneLog.tab.mnemonic").charAt(0));

                        //======== scrollPaneProperties ========
                        {
                            scrollPaneProperties.setFocusable(false);
                            scrollPaneProperties.setMinimumSize(new Dimension(0, 0));

                            //---- textAreaProperties ----
                            textAreaProperties.setEditable(false);
                            textAreaProperties.setTabSize(4);
                            textAreaProperties.setMinimumSize(new Dimension(0, 0));
                            scrollPaneProperties.setViewportView(textAreaProperties);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneProperties.tab.title"), scrollPaneProperties);
                        tabbedPaneNavigatorBottom.setMnemonicAt(1, bundle.getString("Navigator.scrollPaneProperties.tab.mnemonic").charAt(0));
                    }
                    splitPaneBrowser.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab("Browser", splitPaneBrowser);
                tabbedPaneMain.setMnemonicAt(0, bundle.getString("Navigator.splitPaneBrowser.tab.mnemonic").charAt(0));

                //======== splitPaneBackup ========
                {
                    splitPaneBackup.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBackup.setDividerLocation(450);
                    splitPaneBackup.setLastDividerLocation(450);
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.splitPaneBackup.tab.title"), splitPaneBackup);
                tabbedPaneMain.setMnemonicAt(1, bundle.getString("Navigator.splitPaneBackup.tab.mnemonic").charAt(0));

                //======== panelLibraries ========
                {
                    panelLibraries.setLayout(new BorderLayout());
                }
                tabbedPaneMain.addTab("Libraries", panelLibraries);
                tabbedPaneMain.setMnemonicAt(2, bundle.getString("Navigator.panelLibraries.tab.mnemonic").charAt(0));
            }
            panelMain.add(tabbedPaneMain);
        }
        contentPane.add(panelMain, BorderLayout.CENTER);

        //======== panelStatus ========
        {
            panelStatus.setLayout(new GridBagLayout());

            //---- labelStatusLeft ----
            labelStatusLeft.setText(bundle.getString("Navigator.labelStatusLeft.text"));
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
            labelStatusRight.setText(bundle.getString("Navigator.labelStatusRight.text"));
            labelStatusRight.setHorizontalAlignment(SwingConstants.RIGHT);
            panelStatus.add(labelStatusRight, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 4), 0, 0));
        }
        contentPane.add(panelStatus, BorderLayout.SOUTH);
        setSize(1024, 640);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JMenuBar menuBarMain;
    public JMenu menuFile;
    public JMenuItem menuItemNew;
    public JMenuItem menuItemOpen;
    public JMenuItem menuItemSave;
    public JMenuItem menuItemSaveAll;
    public JMenuItem menuItemSaveLayout;
    public JMenuItem menuItemFileQuit;
    public JMenu menuEdit;
    public JMenuItem menuItemFind;
    public JMenuItem menuItemCopy;
    public JMenuItem menuItemCut;
    public JMenuItem menuItemPaste;
    public JMenuItem menuItemPreferences;
    public JMenu menuView;
    public JMenuItem menuItemRefresh;
    public JCheckBoxMenuItem menuItemShowHidden;
    public JMenu menuBookmarks;
    public JMenuItem menuItemShowAllBookmarks;
    public JMenuItem menuItemAddBookmark;
    public JMenu menuTools;
    public JMenuItem menuItemDuplicates;
    public JMenuItem menuItemUuidGenerator;
    public JMenuItem menuItemJunk;
    public JMenuItem menuItemPlexGenerator;
    public JMenuItem menuItemTouch;
    public JMenuItem menuItemExternalTools;
    public JMenu menuRunSubMenu;
    public JMenu menuWindows;
    public JMenuItem menuItemMaximize;
    public JMenuItem menuItemMinimize;
    public JMenuItem menuItemRestore;
    public JMenuItem menuItemSplitHorizontal;
    public JMenuItem menuItemSplitVertical;
    public JMenu menuHelp;
    public JMenuItem menuItemDocumentation;
    public JMenuItem menuItemGitHubProject;
    public JMenuItem menuItemAbout;
    public JPanel panelMain;
    public JTabbedPane tabbedPaneMain;
    public JSplitPane splitPaneBrowser;
    public JPanel panelBrowserTop;
    public JPanel panelLocationAndButtons;
    public JToolBar toolBarBrowser;
    public JButton buttonCreate;
    public JButton buttonCopy;
    public JButton buttonMove;
    public JButton buttonRename;
    public JButton buttonDelete;
    public JPanel panelLocation;
    public JPanel panelLocationLeft;
    public JButton buttonBack;
    public JButton buttonForward;
    public JTextField textFieldLocation;
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
    public JTextArea textAreaProperties;
    public JSplitPane splitPaneBackup;
    public JPanel panelLibraries;
    public JPanel panelStatus;
    public JLabel labelStatusLeft;
    public JLabel labelStatusMiddle;
    public JLabel labelStatusRight;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
