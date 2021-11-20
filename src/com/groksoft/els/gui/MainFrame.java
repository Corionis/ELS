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
 * Navigator graphical user interface.
 * <p><br/>
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

            Dimension dim = new Dimension();
            dim.height = 10;
            dim.width = splitPaneBrowser.getWidth();
            tabbedPaneNavigatorBottom.setMinimumSize(dim);

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
        menuItemFileQuit = new JMenuItem();
        menuEdit = new JMenu();
        menuItemCopy = new JMenuItem();
        menuItemCut = new JMenuItem();
        menuItemPaste = new JMenuItem();
        menuItemPreferences = new JMenuItem();
        menuBookmarks = new JMenu();
        menuItemShowAllBookmarks = new JMenuItem();
        menuItemAddBookmark = new JMenuItem();
        menuTools = new JMenu();
        menuItemPlexGenerator = new JMenuItem();
        menuItemDuplicates = new JMenuItem();
        menuItemUuidGenerator = new JMenuItem();
        menuItemJunk = new JMenuItem();
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
        panelProfiles = new JPanel();
        panelKeys = new JPanel();
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
                menuFile.add(menuItemNew);

                //---- menuItemOpen ----
                menuItemOpen.setText(bundle.getString("Navigator.menuItemOpen.text"));
                menuItemOpen.setMnemonic(bundle.getString("Navigator.menuItemOpen.mnemonic").charAt(0));
                menuItemOpen.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemOpen.setEnabled(false);
                menuFile.add(menuItemOpen);

                //---- menuItemSave ----
                menuItemSave.setText(bundle.getString("Navigator.menuItemSave.text"));
                menuItemSave.setMnemonic(bundle.getString("Navigator.menuItemSave.mnemonic").charAt(0));
                menuItemSave.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSave.setEnabled(false);
                menuFile.add(menuItemSave);

                //---- menuItemSaveAll ----
                menuItemSaveAll.setText(bundle.getString("Navigator.menuItemSaveAll.text"));
                menuItemSaveAll.setMnemonic(bundle.getString("Navigator.menuItemSaveAll.mnemonic").charAt(0));
                menuItemSaveAll.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemSaveAll.setEnabled(false);
                menuFile.add(menuItemSaveAll);
                menuFile.addSeparator();

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(bundle.getString("Navigator.menuItemFileQuit.text"));
                menuItemFileQuit.setMnemonic(bundle.getString("Navigator.menuItemFileQuit.mnemonic").charAt(0));
                menuItemFileQuit.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemFileQuit.addActionListener(e -> menuItemFileQuitActionPerformed(e));
                menuFile.add(menuItemFileQuit);
            }
            menuBarMain.add(menuFile);

            //======== menuEdit ========
            {
                menuEdit.setText(bundle.getString("Navigator.menuEdit.text"));
                menuEdit.setMnemonic(bundle.getString("Navigator.menuEdit.mnemonic").charAt(0));

                //---- menuItemCopy ----
                menuItemCopy.setText(bundle.getString("Navigator.menuItemCopy.text"));
                menuItemCopy.setMnemonic(bundle.getString("Navigator.menuItemCopy.mnemonic").charAt(0));
                menuItemCopy.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(bundle.getString("Navigator.menuItemCut.text"));
                menuItemCut.setMnemonic(bundle.getString("Navigator.menuItemCut.mnemonic_2").charAt(0));
                menuItemCut.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(bundle.getString("Navigator.menuItemPaste.text"));
                menuItemPaste.setMnemonic(bundle.getString("Navigator.menuItemPaste.mnemonic_2").charAt(0));
                menuItemPaste.setHorizontalTextPosition(SwingConstants.LEFT);
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemPreferences ----
                menuItemPreferences.setText(bundle.getString("Navigator.menuItemPreferences.text"));
                menuItemPreferences.setMnemonic(bundle.getString("Navigator.menuItemPreferences.mnemonic_2").charAt(0));
                menuItemPreferences.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPreferences.setEnabled(false);
                menuEdit.add(menuItemPreferences);
            }
            menuBarMain.add(menuEdit);

            //======== menuBookmarks ========
            {
                menuBookmarks.setText(bundle.getString("Navigator.menuBookmarks.text"));
                menuBookmarks.setMnemonic(bundle.getString("Navigator.menuBookmarks.mnemonic").charAt(0));

                //---- menuItemShowAllBookmarks ----
                menuItemShowAllBookmarks.setText(bundle.getString("Navigator.menuItemShowAllBookmarks.text"));
                menuItemShowAllBookmarks.setMnemonic(bundle.getString("Navigator.menuItemShowAllBookmarks.mnemonic").charAt(0));
                menuItemShowAllBookmarks.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemShowAllBookmarks.setEnabled(false);
                menuBookmarks.add(menuItemShowAllBookmarks);

                //---- menuItemAddBookmark ----
                menuItemAddBookmark.setText(bundle.getString("Navigator.menuItemAddBookmark.text"));
                menuItemAddBookmark.setMnemonic(bundle.getString("Navigator.menuItemAddBookmark.mnemonic").charAt(0));
                menuItemAddBookmark.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAddBookmark.setEnabled(false);
                menuBookmarks.add(menuItemAddBookmark);
                menuBookmarks.addSeparator();
            }
            menuBarMain.add(menuBookmarks);

            //======== menuTools ========
            {
                menuTools.setText(bundle.getString("Navigator.menuTools.text"));
                menuTools.setMnemonic(bundle.getString("Navigator.menuTools.mnemonic").charAt(0));

                //---- menuItemPlexGenerator ----
                menuItemPlexGenerator.setText(bundle.getString("Navigator.menuItemPlexGenerator.text"));
                menuItemPlexGenerator.setMnemonic(bundle.getString("Navigator.menuItemPlexGenerator.mnemonic_2").charAt(0));
                menuItemPlexGenerator.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemPlexGenerator.setEnabled(false);
                menuTools.add(menuItemPlexGenerator);

                //---- menuItemDuplicates ----
                menuItemDuplicates.setText(bundle.getString("Navigator.menuItemDuplicates.text"));
                menuItemDuplicates.setMnemonic(bundle.getString("Navigator.menuItemDuplicates.mnemonic").charAt(0));
                menuItemDuplicates.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemDuplicates.setEnabled(false);
                menuTools.add(menuItemDuplicates);

                //---- menuItemUuidGenerator ----
                menuItemUuidGenerator.setText(bundle.getString("Navigator.menuItemUuidGenerator.text"));
                menuItemUuidGenerator.setMnemonic(bundle.getString("Navigator.menuItemUuidGenerator.mnemonic_2").charAt(0));
                menuItemUuidGenerator.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemUuidGenerator.setEnabled(false);
                menuTools.add(menuItemUuidGenerator);

                //---- menuItemJunk ----
                menuItemJunk.setText(bundle.getString("Navigator.menuItemJunk.text"));
                menuItemJunk.setMnemonic(bundle.getString("Navigator.menuItemJunk.mnemonic").charAt(0));
                menuItemJunk.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemJunk.setEnabled(false);
                menuTools.add(menuItemJunk);

                //---- menuItemTouch ----
                menuItemTouch.setText(bundle.getString("Navigator.menuItemTouch.text"));
                menuItemTouch.setMnemonic(bundle.getString("Navigator.menuItemTouch.mnemonic").charAt(0));
                menuItemTouch.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemTouch.setEnabled(false);
                menuTools.add(menuItemTouch);
                menuTools.addSeparator();

                //---- menuItemExternalTools ----
                menuItemExternalTools.setText(bundle.getString("Navigator.menuItemExternalTools.text"));
                menuItemExternalTools.setMnemonic(bundle.getString("Navigator.menuItemExternalTools.mnemonic").charAt(0));
                menuItemExternalTools.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemExternalTools.setEnabled(false);
                menuTools.add(menuItemExternalTools);

                //======== menuRunSubMenu ========
                {
                    menuRunSubMenu.setText(bundle.getString("Navigator.menuRunSubMenu.text"));
                    menuRunSubMenu.setMnemonic(bundle.getString("Navigator.menuRunSubMenu.mnemonic").charAt(0));
                    menuRunSubMenu.setHorizontalTextPosition(SwingConstants.LEFT);
                    menuRunSubMenu.setEnabled(false);
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
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(bundle.getString("Navigator.menuItemMinimize.text"));
                menuItemMinimize.setMnemonic(bundle.getString("Navigator.menuItemMinimize.mnemonic_2").charAt(0));
                menuItemMinimize.setHorizontalTextPosition(SwingConstants.LEFT);
                menuWindows.add(menuItemMinimize);

                //---- menuItemRestore ----
                menuItemRestore.setText(bundle.getString("Navigator.menuItemRestore.text"));
                menuItemRestore.setMnemonic(bundle.getString("Navigator.menuItemRestore.mnemonic_2").charAt(0));
                menuItemRestore.setHorizontalTextPosition(SwingConstants.LEFT);
                menuWindows.add(menuItemRestore);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(bundle.getString("Navigator.menuItemSplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(bundle.getString("Navigator.menuItemSplitHorizontal.mnemonic").charAt(0));
                menuItemSplitHorizontal.setHorizontalTextPosition(SwingConstants.LEFT);
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(bundle.getString("Navigator.menuItemSplitVertical.text"));
                menuItemSplitVertical.setMnemonic(bundle.getString("Navigator.menuItemSplitVertical.mnemonic").charAt(0));
                menuItemSplitVertical.setHorizontalTextPosition(SwingConstants.LEFT);
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
                menuHelp.add(menuItemDocumentation);

                //---- menuItemGitHubProject ----
                menuItemGitHubProject.setText(bundle.getString("Navigator.menuItemGitHubProject.text"));
                menuItemGitHubProject.setMnemonic(bundle.getString("Navigator.menuItemGitHubProject.mnemonic").charAt(0));
                menuItemGitHubProject.setHorizontalTextPosition(SwingConstants.LEFT);
                menuHelp.add(menuItemGitHubProject);
                menuHelp.addSeparator();

                //---- menuItemAbout ----
                menuItemAbout.setText(bundle.getString("Navigator.menuItemAbout.text"));
                menuItemAbout.setMnemonic(bundle.getString("Navigator.menuItemAbout.mnemonic").charAt(0));
                menuItemAbout.setHorizontalTextPosition(SwingConstants.LEFT);
                menuItemAbout.setEnabled(false);
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

                //======== splitPaneBrowser ========
                {
                    splitPaneBrowser.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBrowser.setLastDividerLocation(396);
                    splitPaneBrowser.setMinimumSize(new Dimension(54, 0));
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
                                    buttonBack.setMnemonic(bundle.getString("Navigator.buttonBack.mnemonic").charAt(0));
                                    panelLocationLeft.add(buttonBack, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonForward ----
                                    buttonForward.setText("<html>&gt;</html>");
                                    buttonForward.setMaximumSize(new Dimension(36, 30));
                                    buttonForward.setMinimumSize(new Dimension(36, 30));
                                    buttonForward.setPreferredSize(new Dimension(36, 30));
                                    buttonForward.setToolTipText(bundle.getString("Navigator.buttonForward.toolTipText"));
                                    buttonForward.setMnemonic(bundle.getString("Navigator.buttonForward.mnemonic").charAt(0));
                                    buttonForward.setActionCommand("NavForward");
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
                            }
                            splitPaneTwoBrowsers.setLeftComponent(tabbedPaneBrowserOne);

                            //======== tabbedPaneBrowserTwo ========
                            {
                                tabbedPaneBrowserTwo.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserTwo.setPreferredSize(new Dimension(950, 427));
                                tabbedPaneBrowserTwo.setMinimumSize(new Dimension(0, 0));
                                tabbedPaneBrowserTwo.setAutoscrolls(true);

                                //======== panelCollectionTwo ========
                                {
                                    panelCollectionTwo.setMinimumSize(new Dimension(0, 0));
                                    panelCollectionTwo.setLayout(new BoxLayout(panelCollectionTwo, BoxLayout.X_AXIS));

                                    //======== splitPaneCollectionTwo ========
                                    {
                                        splitPaneCollectionTwo.setBorder(null);
                                        splitPaneCollectionTwo.setDividerLocation(150);
                                        splitPaneCollectionTwo.setResizeWeight(0.5);
                                        splitPaneCollectionTwo.setContinuousLayout(true);
                                        splitPaneCollectionTwo.setMinimumSize(new Dimension(0, 0));

                                        //======== scrollPaneTreeCollectionTwo ========
                                        {
                                            scrollPaneTreeCollectionTwo.setFocusable(false);
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
                                            scrollPaneTableCollectionTwo.setFocusable(false);
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
                            scrollPaneLog.setViewportView(textAreaLog);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneLog.tab.title"), scrollPaneLog);

                        //======== scrollPaneProperties ========
                        {
                            scrollPaneProperties.setFocusable(false);
                            scrollPaneProperties.setMinimumSize(new Dimension(0, 0));

                            //---- textAreaProperties ----
                            textAreaProperties.setEditable(false);
                            textAreaProperties.setTabSize(4);
                            scrollPaneProperties.setViewportView(textAreaProperties);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneProperties.tab.title"), scrollPaneProperties);
                    }
                    splitPaneBrowser.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab("Browser", splitPaneBrowser);

                //======== splitPaneBackup ========
                {
                    splitPaneBackup.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBackup.setDividerLocation(450);
                    splitPaneBackup.setLastDividerLocation(450);
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.splitPaneBackup.tab.title"), splitPaneBackup);

                //======== panelProfiles ========
                {
                    panelProfiles.setLayout(new BorderLayout());
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.panelProfiles.tab.title"), panelProfiles);

                //======== panelKeys ========
                {
                    panelKeys.setLayout(new BorderLayout());
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.panelKeys.tab.title"), panelKeys);
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
    public JMenuItem menuItemFileQuit;
    public JMenu menuEdit;
    public JMenuItem menuItemCopy;
    public JMenuItem menuItemCut;
    public JMenuItem menuItemPaste;
    public JMenuItem menuItemPreferences;
    public JMenu menuBookmarks;
    public JMenuItem menuItemShowAllBookmarks;
    public JMenuItem menuItemAddBookmark;
    public JMenu menuTools;
    public JMenuItem menuItemPlexGenerator;
    public JMenuItem menuItemDuplicates;
    public JMenuItem menuItemUuidGenerator;
    public JMenuItem menuItemJunk;
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
    public JPanel panelProfiles;
    public JPanel panelKeys;
    public JPanel panelStatus;
    public JLabel labelStatusLeft;
    public JLabel labelStatusMiddle;
    public JLabel labelStatusRight;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
