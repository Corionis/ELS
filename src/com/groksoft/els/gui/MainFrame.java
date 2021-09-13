package com.groksoft.els.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Main;
import com.groksoft.els.Utils;
import com.sun.java.swing.plaf.gtk.GTKLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private Configuration cfg;
    private Context context;
    private Main els;
    private Navigator navigator;
    private LookAndFeel laf;

    private int lafStyle = 7;  // 0-7, see getLookAndFeel()

    public MainFrame(Main main, Navigator nav, Configuration config, Context ctx)
    {
        els = main;
        navigator = nav;
        cfg = config;
        context = ctx;

        try
        {
            laf = getLookAndFeel();
            UIManager.setLookAndFeel(laf);
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

            //pack();
        }
        catch(Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            context.fault = true;
        }
    }

    private LookAndFeel getLookAndFeel()
    {
        switch (lafStyle)
        {
            // Built-in themes
            case 0:
                laf = new MetalLookAndFeel();
                break;
            case 1:
                laf = new NimbusLookAndFeel();
                break;
            case 2:
                laf = new GTKLookAndFeel();
                break;
            case 3:
                laf = new WindowsLookAndFeel(); // only on Windows
                break;
            // FlatLaf themes
            case 4:
                laf = new FlatLightLaf();
                break;
            case 5:
                laf = new FlatDarkLaf();
                break;
            case 6:
                laf = new FlatIntelliJLaf();
                break;
            case 7:
            default:
                laf = new FlatDarculaLaf();
                break;
        }
        return laf;
    }

    private void menuItemFileQuitActionPerformed(ActionEvent e)
    {
        navigator.stop();
    }

    private void SaveActionPerformed(ActionEvent e)
    {
        // something
    }

    private void thisWindowClosing(WindowEvent e)
    {
        navigator.stop();
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
        menuItem8 = new JMenuItem();
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
        menuItemExternalTools = new JMenuItem();
        menuRunSubMenu = new JMenu();
        menuItemDuplicates = new JMenuItem();
        menuItemUuidGenerator = new JMenuItem();
        menuItemJunk = new JMenuItem();
        menuWindows = new JMenu();
        menuItemMaximize = new JMenuItem();
        menuItemMinimize = new JMenuItem();
        menuItemFullScreen = new JMenuItem();
        menuItemSplitHorizontal = new JMenuItem();
        menuItemSplitVertical = new JMenuItem();
        menuHelp = new JMenu();
        menuItemDocumentation = new JMenuItem();
        menuItemAbout = new JMenuItem();
        panelMain = new JPanel();
        tabbedPaneMain = new JTabbedPane();
        splitPaneBrowser = new JSplitPane();
        panelBrowserTop = new JPanel();
        panelLocationAndButtons = new JPanel();
        toolBarBrowser = new JToolBar();
        buttonPlay = new JButton();
        buttonCopy = new JButton();
        buttonMove = new JButton();
        buttonRename = new JButton();
        buttonDelete = new JButton();
        panelLocation = new JPanel();
        panelLocationLeft = new JPanel();
        buttonHome = new JButton();
        buttonBack = new JButton();
        buttonForward = new JButton();
        textFieldLocation = new JTextField();
        panelLocationRight = new JPanel();
        buttonBrowse = new JButton();
        buttonToggle = new JButton();
        splitPaneTwoBrowsers = new JSplitPane();
        tabbedPaneBrowserOne = new JTabbedPane();
        panelCollectionOne = new JPanel();
        splitPaneCollectionOne = new JSplitPane();
        scrollPaneCollectionTreeOne = new JScrollPane();
        treeCollectionOne = new JTree();
        scrollPaneCollectionListOne = new JScrollPane();
        tableCollectionListOne = new JTable();
        panelSystemOne = new JPanel();
        splitPaneSystemOne = new JSplitPane();
        scrollPaneSystemTreeOne = new JScrollPane();
        treeSystemOne = new JTree();
        scrollPaneSystemListOne = new JScrollPane();
        tableSystemListOne = new JTable();
        tabbedPaneBrowserTwo = new JTabbedPane();
        panelCollectionTwo = new JPanel();
        splitPaneCollectionTwo = new JSplitPane();
        scrollPaneCollectionTreeTwo = new JScrollPane();
        treeCollectionTwo = new JTree();
        scrollPaneCollectionListTwo = new JScrollPane();
        tableCollectionListTwo = new JTable();
        panelSystemTwo = new JPanel();
        splitPaneSystemTwo = new JSplitPane();
        scrollPaneSystemTreeTwo = new JScrollPane();
        treeSystemTwo = new JTree();
        scrollPaneSystemListTwo = new JScrollPane();
        tableSystemListTwo = new JTable();
        tabbedPaneNavigatorBottom = new JTabbedPane();
        scrollPaneFind = new JScrollPane();
        listFind = new JList();
        scrollPaneContent = new JScrollPane();
        textPaneContent = new JTextPane();
        splitPaneBackup = new JSplitPane();
        panelProfiles = new JPanel();
        panelKeys = new JPanel();
        panelStatus = new JPanel();
        labelStatusLeft = new JLabel();
        labelStatusMiddle = new JLabel();
        labelStatusRight = new JLabel();

        //======== this ========
        setMinimumSize(new Dimension(1024, 640));
        setTitle("ELS Navigator");
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
                menuFile.add(menuItemNew);

                //---- menuItemOpen ----
                menuItemOpen.setText(bundle.getString("Navigator.menuItemOpen.text"));
                menuItemOpen.setMnemonic(bundle.getString("Navigator.menuItemOpen.mnemonic").charAt(0));
                menuFile.add(menuItemOpen);

                //---- menuItemSave ----
                menuItemSave.setText(bundle.getString("Navigator.menuItemSave.text"));
                menuItemSave.setMnemonic(bundle.getString("Navigator.menuItemSave.mnemonic").charAt(0));
                menuFile.add(menuItemSave);

                //---- menuItemSaveAll ----
                menuItemSaveAll.setText(bundle.getString("Navigator.menuItemSaveAll.text"));
                menuItemSaveAll.setMnemonic(bundle.getString("Navigator.menuItemSaveAll.mnemonic").charAt(0));
                menuFile.add(menuItemSaveAll);
                menuFile.addSeparator();

                //---- menuItem8 ----
                menuItem8.setText(bundle.getString("Navigator.menuItem8.text"));
                menuItem8.setMnemonic(bundle.getString("Navigator.menuItem8.mnemonic").charAt(0));
                menuFile.add(menuItem8);
                menuFile.addSeparator();

                //---- menuItemFileQuit ----
                menuItemFileQuit.setText(bundle.getString("Navigator.menuItemFileQuit.text"));
                menuItemFileQuit.setMnemonic(bundle.getString("Navigator.menuItemFileQuit.mnemonic").charAt(0));
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
                menuEdit.add(menuItemCopy);

                //---- menuItemCut ----
                menuItemCut.setText(bundle.getString("Navigator.menuItemCut.text"));
                menuItemCut.setMnemonic(bundle.getString("Navigator.menuItemCut.mnemonic").charAt(0));
                menuEdit.add(menuItemCut);

                //---- menuItemPaste ----
                menuItemPaste.setText(bundle.getString("Navigator.menuItemPaste.text"));
                menuItemPaste.setMnemonic(bundle.getString("Navigator.menuItemPaste.mnemonic").charAt(0));
                menuEdit.add(menuItemPaste);
                menuEdit.addSeparator();

                //---- menuItemPreferences ----
                menuItemPreferences.setText(bundle.getString("Navigator.menuItemPreferences.text"));
                menuItemPreferences.setMnemonic(bundle.getString("Navigator.menuItemPreferences.mnemonic").charAt(0));
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
                menuBookmarks.add(menuItemShowAllBookmarks);

                //---- menuItemAddBookmark ----
                menuItemAddBookmark.setText(bundle.getString("Navigator.menuItemAddBookmark.text"));
                menuItemAddBookmark.setMnemonic(bundle.getString("Navigator.menuItemAddBookmark.mnemonic").charAt(0));
                menuBookmarks.add(menuItemAddBookmark);
                menuBookmarks.addSeparator();
            }
            menuBarMain.add(menuBookmarks);

            //======== menuTools ========
            {
                menuTools.setText(bundle.getString("Navigator.menuTools.text"));
                menuTools.setMnemonic(bundle.getString("Navigator.menuTools.mnemonic").charAt(0));

                //---- menuItemExternalTools ----
                menuItemExternalTools.setText(bundle.getString("Navigator.menuItemExternalTools.text"));
                menuItemExternalTools.setMnemonic(bundle.getString("Navigator.menuItemExternalTools.mnemonic").charAt(0));
                menuTools.add(menuItemExternalTools);

                //======== menuRunSubMenu ========
                {
                    menuRunSubMenu.setText(bundle.getString("Navigator.menuRunSubMenu.text"));
                    menuRunSubMenu.setMnemonic(bundle.getString("Navigator.menuRunSubMenu.mnemonic").charAt(0));
                }
                menuTools.add(menuRunSubMenu);
                menuTools.addSeparator();

                //---- menuItemDuplicates ----
                menuItemDuplicates.setText(bundle.getString("Navigator.menuItemDuplicates.text"));
                menuItemDuplicates.setMnemonic(bundle.getString("Navigator.menuItemDuplicates.mnemonic").charAt(0));
                menuTools.add(menuItemDuplicates);

                //---- menuItemUuidGenerator ----
                menuItemUuidGenerator.setText(bundle.getString("Navigator.menuItemUuidGenerator.text"));
                menuItemUuidGenerator.setMnemonic(bundle.getString("Navigator.menuItemUuidGenerator.mnemonic_2").charAt(0));
                menuTools.add(menuItemUuidGenerator);

                //---- menuItemJunk ----
                menuItemJunk.setText(bundle.getString("Navigator.menuItemJunk.text"));
                menuItemJunk.setMnemonic(bundle.getString("Navigator.menuItemJunk.mnemonic_2").charAt(0));
                menuTools.add(menuItemJunk);
            }
            menuBarMain.add(menuTools);

            //======== menuWindows ========
            {
                menuWindows.setText(bundle.getString("Navigator.menuWindows.text"));
                menuWindows.setMnemonic(bundle.getString("Navigator.menuWindows.mnemonic").charAt(0));

                //---- menuItemMaximize ----
                menuItemMaximize.setText(bundle.getString("Navigator.menuItemMaximize.text"));
                menuItemMaximize.setMnemonic(bundle.getString("Navigator.menuItemMaximize.mnemonic").charAt(0));
                menuWindows.add(menuItemMaximize);

                //---- menuItemMinimize ----
                menuItemMinimize.setText(bundle.getString("Navigator.menuItemMinimize.text"));
                menuItemMinimize.setMnemonic(bundle.getString("Navigator.menuItemMinimize.mnemonic").charAt(0));
                menuWindows.add(menuItemMinimize);

                //---- menuItemFullScreen ----
                menuItemFullScreen.setText(bundle.getString("Navigator.menuItemFullScreen.text"));
                menuItemFullScreen.setMnemonic(bundle.getString("Navigator.menuItemFullScreen.mnemonic").charAt(0));
                menuWindows.add(menuItemFullScreen);
                menuWindows.addSeparator();

                //---- menuItemSplitHorizontal ----
                menuItemSplitHorizontal.setText(bundle.getString("Navigator.menuItemSplitHorizontal.text"));
                menuItemSplitHorizontal.setMnemonic(bundle.getString("Navigator.menuItemSplitHorizontal.mnemonic").charAt(0));
                menuWindows.add(menuItemSplitHorizontal);

                //---- menuItemSplitVertical ----
                menuItemSplitVertical.setText(bundle.getString("Navigator.menuItemSplitVertical.text"));
                menuItemSplitVertical.setMnemonic(bundle.getString("Navigator.menuItemSplitVertical.mnemonic").charAt(0));
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
                menuHelp.add(menuItemDocumentation);

                //---- menuItemAbout ----
                menuItemAbout.setText(bundle.getString("Navigator.menuItemAbout.text"));
                menuItemAbout.setMnemonic(bundle.getString("Navigator.menuItemAbout.mnemonic").charAt(0));
                menuHelp.add(menuItemAbout);
            }
            menuBarMain.add(menuHelp);
        }
        setJMenuBar(menuBarMain);

        //======== panelMain ========
        {
            panelMain.setPreferredSize(new Dimension(1020, 560));
            panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));

            //======== tabbedPaneMain ========
            {

                //======== splitPaneBrowser ========
                {
                    splitPaneBrowser.setDividerLocation(450);
                    splitPaneBrowser.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneBrowser.setLastDividerLocation(450);

                    //======== panelBrowserTop ========
                    {
                        panelBrowserTop.setPreferredSize(new Dimension(1160, 390));
                        panelBrowserTop.setLayout(new BorderLayout());

                        //======== panelLocationAndButtons ========
                        {
                            panelLocationAndButtons.setLayout(new BorderLayout());

                            //======== toolBarBrowser ========
                            {
                                toolBarBrowser.setFloatable(false);
                                toolBarBrowser.setMargin(new Insets(0, 4, 0, 0));
                                toolBarBrowser.setRollover(true);

                                //---- buttonPlay ----
                                buttonPlay.setText(bundle.getString("Navigator.buttonPlay.text"));
                                buttonPlay.setToolTipText(bundle.getString("Navigator.buttonPlay.toolTipText"));
                                toolBarBrowser.add(buttonPlay);

                                //---- buttonCopy ----
                                buttonCopy.setText(bundle.getString("Navigator.buttonCopy.text"));
                                buttonCopy.setToolTipText(bundle.getString("Navigator.buttonCopy.toolTipText"));
                                toolBarBrowser.add(buttonCopy);

                                //---- buttonMove ----
                                buttonMove.setText(bundle.getString("Navigator.buttonMove.text"));
                                buttonMove.setToolTipText(bundle.getString("Navigator.buttonMove.toolTipText"));
                                toolBarBrowser.add(buttonMove);

                                //---- buttonRename ----
                                buttonRename.setText(bundle.getString("Navigator.buttonRename.text"));
                                buttonRename.setToolTipText(bundle.getString("Navigator.buttonRename.toolTipText"));
                                toolBarBrowser.add(buttonRename);

                                //---- buttonDelete ----
                                buttonDelete.setText(bundle.getString("Navigator.buttonDelete.text"));
                                buttonDelete.setToolTipText(bundle.getString("Navigator.buttonDelete.toolTipText"));
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

                                    //---- buttonHome ----
                                    buttonHome.setText("Home");
                                    buttonHome.setToolTipText(bundle.getString("Navigator.buttonHome.toolTipText"));
                                    panelLocationLeft.add(buttonHome, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonBack ----
                                    buttonBack.setText("<");
                                    buttonBack.setFont(new Font("Ubuntu", Font.PLAIN, 13));
                                    buttonBack.setMaximumSize(new Dimension(36, 30));
                                    buttonBack.setMinimumSize(new Dimension(36, 30));
                                    buttonBack.setPreferredSize(new Dimension(36, 30));
                                    buttonBack.setToolTipText(bundle.getString("Navigator.buttonBack.toolTipText"));
                                    panelLocationLeft.add(buttonBack, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonForward ----
                                    buttonForward.setText(">");
                                    buttonForward.setFont(new Font("Ubuntu", Font.PLAIN, 13));
                                    buttonForward.setMaximumSize(new Dimension(36, 30));
                                    buttonForward.setMinimumSize(new Dimension(36, 30));
                                    buttonForward.setPreferredSize(new Dimension(36, 30));
                                    buttonForward.setToolTipText(bundle.getString("Navigator.buttonForward.toolTipText"));
                                    panelLocationLeft.add(buttonForward, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                panelLocation.add(panelLocationLeft, BorderLayout.WEST);

                                //---- textFieldLocation ----
                                textFieldLocation.setText("RockPlex/home/plex/Plex/Media/Rock01/");
                                textFieldLocation.setPreferredSize(new Dimension(850, 30));
                                textFieldLocation.setHorizontalAlignment(SwingConstants.LEFT);
                                panelLocation.add(textFieldLocation, BorderLayout.CENTER);

                                //======== panelLocationRight ========
                                {
                                    panelLocationRight.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panelLocationRight.getLayout()).columnWidths = new int[] {0, 0, 0};
                                    ((GridBagLayout)panelLocationRight.getLayout()).rowHeights = new int[] {0, 0};
                                    ((GridBagLayout)panelLocationRight.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0E-4};
                                    ((GridBagLayout)panelLocationRight.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                                    //---- buttonBrowse ----
                                    buttonBrowse.setText("Browse ");
                                    buttonBrowse.setToolTipText(bundle.getString("Navigator.buttonBrowse.toolTipText"));
                                    panelLocationRight.add(buttonBrowse, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonToggle ----
                                    buttonToggle.setText("Toggle");
                                    buttonToggle.setToolTipText(bundle.getString("Navigator.buttonToggle.toolTipText"));
                                    panelLocationRight.add(buttonToggle, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                panelLocation.add(panelLocationRight, BorderLayout.EAST);
                            }
                            panelLocationAndButtons.add(panelLocation, BorderLayout.SOUTH);
                        }
                        panelBrowserTop.add(panelLocationAndButtons, BorderLayout.NORTH);

                        //======== splitPaneTwoBrowsers ========
                        {
                            splitPaneTwoBrowsers.setDividerLocation(506);
                            splitPaneTwoBrowsers.setLastDividerLocation(516);
                            splitPaneTwoBrowsers.setResizeWeight(0.5);
                            splitPaneTwoBrowsers.setPreferredSize(new Dimension(812, 390));

                            //======== tabbedPaneBrowserOne ========
                            {
                                tabbedPaneBrowserOne.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserOne.setPreferredSize(new Dimension(400, 427));
                                tabbedPaneBrowserOne.setFocusable(false);

                                //======== panelCollectionOne ========
                                {
                                    panelCollectionOne.setFocusable(false);
                                    panelCollectionOne.setLayout(new BorderLayout());

                                    //======== splitPaneCollectionOne ========
                                    {
                                        splitPaneCollectionOne.setDividerLocation(150);
                                        splitPaneCollectionOne.setFocusable(false);
                                        splitPaneCollectionOne.setBorder(null);

                                        //======== scrollPaneCollectionTreeOne ========
                                        {
                                            scrollPaneCollectionTreeOne.setPreferredSize(new Dimension(180, 362));

                                            //---- treeCollectionOne ----
                                            treeCollectionOne.setFocusable(false);
                                            treeCollectionOne.setPreferredSize(new Dimension(101, 380));
                                            scrollPaneCollectionTreeOne.setViewportView(treeCollectionOne);
                                        }
                                        splitPaneCollectionOne.setLeftComponent(scrollPaneCollectionTreeOne);

                                        //======== scrollPaneCollectionListOne ========
                                        {
                                            scrollPaneCollectionListOne.setPreferredSize(new Dimension(756, 380));

                                            //---- tableCollectionListOne ----
                                            tableCollectionListOne.setPreferredSize(new Dimension(200, 32));
                                            tableCollectionListOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableCollectionListOne.setFocusable(false);
                                            scrollPaneCollectionListOne.setViewportView(tableCollectionListOne);
                                        }
                                        splitPaneCollectionOne.setRightComponent(scrollPaneCollectionListOne);
                                    }
                                    panelCollectionOne.add(splitPaneCollectionOne, BorderLayout.NORTH);
                                }
                                tabbedPaneBrowserOne.addTab(bundle.getString("Navigator.panelCollectionOne.tab.title"), panelCollectionOne);

                                //======== panelSystemOne ========
                                {
                                    panelSystemOne.setFocusable(false);
                                    panelSystemOne.setLayout(new BorderLayout());

                                    //======== splitPaneSystemOne ========
                                    {
                                        splitPaneSystemOne.setBorder(null);
                                        splitPaneSystemOne.setDividerLocation(150);

                                        //======== scrollPaneSystemTreeOne ========
                                        {
                                            scrollPaneSystemTreeOne.setPreferredSize(new Dimension(180, 362));

                                            //---- treeSystemOne ----
                                            treeSystemOne.setFocusable(false);
                                            treeSystemOne.setPreferredSize(new Dimension(101, 380));
                                            scrollPaneSystemTreeOne.setViewportView(treeSystemOne);
                                        }
                                        splitPaneSystemOne.setLeftComponent(scrollPaneSystemTreeOne);

                                        //======== scrollPaneSystemListOne ========
                                        {
                                            scrollPaneSystemListOne.setPreferredSize(new Dimension(756, 380));

                                            //---- tableSystemListOne ----
                                            tableSystemListOne.setPreferredSize(new Dimension(754, 380));
                                            tableSystemListOne.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableSystemListOne.setFocusable(false);
                                            scrollPaneSystemListOne.setViewportView(tableSystemListOne);
                                        }
                                        splitPaneSystemOne.setRightComponent(scrollPaneSystemListOne);
                                    }
                                    panelSystemOne.add(splitPaneSystemOne, BorderLayout.NORTH);
                                }
                                tabbedPaneBrowserOne.addTab(bundle.getString("Navigator.panelSystemOne.tab.title"), panelSystemOne);
                            }
                            splitPaneTwoBrowsers.setLeftComponent(tabbedPaneBrowserOne);

                            //======== tabbedPaneBrowserTwo ========
                            {
                                tabbedPaneBrowserTwo.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneBrowserTwo.setPreferredSize(new Dimension(400, 380));
                                tabbedPaneBrowserTwo.setFocusable(false);

                                //======== panelCollectionTwo ========
                                {
                                    panelCollectionTwo.setLayout(new BorderLayout());

                                    //======== splitPaneCollectionTwo ========
                                    {
                                        splitPaneCollectionTwo.setBorder(null);
                                        splitPaneCollectionTwo.setDividerLocation(150);

                                        //======== scrollPaneCollectionTreeTwo ========
                                        {
                                            scrollPaneCollectionTreeTwo.setPreferredSize(new Dimension(180, 380));

                                            //---- treeCollectionTwo ----
                                            treeCollectionTwo.setFocusable(false);
                                            treeCollectionTwo.setRequestFocusEnabled(false);
                                            treeCollectionTwo.setPreferredSize(new Dimension(101, 380));
                                            scrollPaneCollectionTreeTwo.setViewportView(treeCollectionTwo);
                                        }
                                        splitPaneCollectionTwo.setLeftComponent(scrollPaneCollectionTreeTwo);

                                        //======== scrollPaneCollectionListTwo ========
                                        {
                                            scrollPaneCollectionListTwo.setPreferredSize(new Dimension(756, 380));

                                            //---- tableCollectionListTwo ----
                                            tableCollectionListTwo.setPreferredSize(new Dimension(754, 380));
                                            tableCollectionListTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableCollectionListTwo.setFocusable(false);
                                            scrollPaneCollectionListTwo.setViewportView(tableCollectionListTwo);
                                        }
                                        splitPaneCollectionTwo.setRightComponent(scrollPaneCollectionListTwo);
                                    }
                                    panelCollectionTwo.add(splitPaneCollectionTwo, BorderLayout.NORTH);
                                }
                                tabbedPaneBrowserTwo.addTab(bundle.getString("Navigator.panelCollectionTwo.tab.title"), panelCollectionTwo);

                                //======== panelSystemTwo ========
                                {
                                    panelSystemTwo.setPreferredSize(new Dimension(946, 380));
                                    panelSystemTwo.setLayout(new BorderLayout());

                                    //======== splitPaneSystemTwo ========
                                    {
                                        splitPaneSystemTwo.setBorder(null);
                                        splitPaneSystemTwo.setDividerLocation(150);

                                        //======== scrollPaneSystemTreeTwo ========
                                        {
                                            scrollPaneSystemTreeTwo.setPreferredSize(new Dimension(180, 362));

                                            //---- treeSystemTwo ----
                                            treeSystemTwo.setFocusable(false);
                                            scrollPaneSystemTreeTwo.setViewportView(treeSystemTwo);
                                        }
                                        splitPaneSystemTwo.setLeftComponent(scrollPaneSystemTreeTwo);

                                        //======== scrollPaneSystemListTwo ========
                                        {

                                            //---- tableSystemListTwo ----
                                            tableSystemListTwo.setPreferredSize(new Dimension(200, 32));
                                            tableSystemListTwo.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                            tableSystemListTwo.setFocusable(false);
                                            scrollPaneSystemListTwo.setViewportView(tableSystemListTwo);
                                        }
                                        splitPaneSystemTwo.setRightComponent(scrollPaneSystemListTwo);
                                    }
                                    panelSystemTwo.add(splitPaneSystemTwo, BorderLayout.NORTH);
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
                        tabbedPaneNavigatorBottom.setPreferredSize(new Dimension(70, 170));
                        tabbedPaneNavigatorBottom.setFocusable(false);

                        //======== scrollPaneFind ========
                        {
                            scrollPaneFind.setFocusable(false);

                            //---- listFind ----
                            listFind.setFocusable(false);
                            scrollPaneFind.setViewportView(listFind);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneFind.tab.title"), scrollPaneFind);

                        //======== scrollPaneContent ========
                        {
                            scrollPaneContent.setFocusable(false);

                            //---- textPaneContent ----
                            textPaneContent.setFocusable(false);
                            scrollPaneContent.setViewportView(textPaneContent);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneContent.tab.title"), scrollPaneContent);
                    }
                    splitPaneBrowser.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.splitPaneBrowser.tab.title"), splitPaneBrowser);

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
            labelStatusMiddle.setText(bundle.getString("Navigator.labelStatusMiddle.text"));
            labelStatusMiddle.setHorizontalAlignment(SwingConstants.CENTER);
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
        pack();
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
    public JMenuItem menuItem8;
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
    public JMenuItem menuItemExternalTools;
    public JMenu menuRunSubMenu;
    public JMenuItem menuItemDuplicates;
    public JMenuItem menuItemUuidGenerator;
    public JMenuItem menuItemJunk;
    public JMenu menuWindows;
    public JMenuItem menuItemMaximize;
    public JMenuItem menuItemMinimize;
    public JMenuItem menuItemFullScreen;
    public JMenuItem menuItemSplitHorizontal;
    public JMenuItem menuItemSplitVertical;
    public JMenu menuHelp;
    public JMenuItem menuItemDocumentation;
    public JMenuItem menuItemAbout;
    public JPanel panelMain;
    public JTabbedPane tabbedPaneMain;
    public JSplitPane splitPaneBrowser;
    public JPanel panelBrowserTop;
    public JPanel panelLocationAndButtons;
    public JToolBar toolBarBrowser;
    public JButton buttonPlay;
    public JButton buttonCopy;
    public JButton buttonMove;
    public JButton buttonRename;
    public JButton buttonDelete;
    public JPanel panelLocation;
    public JPanel panelLocationLeft;
    public JButton buttonHome;
    public JButton buttonBack;
    public JButton buttonForward;
    public JTextField textFieldLocation;
    public JPanel panelLocationRight;
    public JButton buttonBrowse;
    public JButton buttonToggle;
    public JSplitPane splitPaneTwoBrowsers;
    public JTabbedPane tabbedPaneBrowserOne;
    public JPanel panelCollectionOne;
    public JSplitPane splitPaneCollectionOne;
    public JScrollPane scrollPaneCollectionTreeOne;
    public JTree treeCollectionOne;
    public JScrollPane scrollPaneCollectionListOne;
    public JTable tableCollectionListOne;
    public JPanel panelSystemOne;
    public JSplitPane splitPaneSystemOne;
    public JScrollPane scrollPaneSystemTreeOne;
    public JTree treeSystemOne;
    public JScrollPane scrollPaneSystemListOne;
    public JTable tableSystemListOne;
    public JTabbedPane tabbedPaneBrowserTwo;
    public JPanel panelCollectionTwo;
    public JSplitPane splitPaneCollectionTwo;
    public JScrollPane scrollPaneCollectionTreeTwo;
    public JTree treeCollectionTwo;
    public JScrollPane scrollPaneCollectionListTwo;
    public JTable tableCollectionListTwo;
    public JPanel panelSystemTwo;
    public JSplitPane splitPaneSystemTwo;
    public JScrollPane scrollPaneSystemTreeTwo;
    public JTree treeSystemTwo;
    public JScrollPane scrollPaneSystemListTwo;
    public JTable tableSystemListTwo;
    public JTabbedPane tabbedPaneNavigatorBottom;
    public JScrollPane scrollPaneFind;
    public JList listFind;
    public JScrollPane scrollPaneContent;
    public JTextPane textPaneContent;
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
