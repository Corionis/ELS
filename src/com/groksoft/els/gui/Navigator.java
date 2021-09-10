package com.groksoft.els.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Main;
import com.sun.java.swing.plaf.gtk.GTKLookAndFeel;
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
public class Navigator extends JFrame {
    private transient Logger logger = LogManager.getLogger("applog");
    Configuration cfg;
    Context context;
    Main els;
    LookAndFeel laf;
    int lafIndex = 0;
    ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");

    public Navigator(Main main, Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
        els = main;

        // TODO:
        //  Add Navigator configuration file that holds the LaF, position, size, options, etc.
        //  *
        //  * How to organize editing JSON server and targets files with N-libraries with N-sources each?

        try
        {
            laf = getLookAndFeel();
            UIManager.setLookAndFeel(laf);
            initComponents();

            // change browser tabs orientation to vertical
            JLabel label = new JLabel(bundle.getString("Navigator.panelCollectionLeft.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneNavigatorLeft.setTabComponentAt(0, label);

            label = new JLabel(bundle.getString("Navigator.panelSystemLeft.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneNavigatorLeft.setTabComponentAt(1, label);

            label = new JLabel(bundle.getString("Navigator.panelCollectionRight.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneNavigatorRight.setTabComponentAt(0, label);

            label = new JLabel(bundle.getString("Navigator.panelSystemRight.tab.title"));
            label.setUI(new VerticalLabelUI(false));
            tabbedPaneNavigatorRight.setTabComponentAt(1, label);

            // set defaults
        }
        catch( Exception ex )
        {
            els.setFault(true);
        }
    }

    private LookAndFeel getLookAndFeel()
    {
        switch (lafIndex)
        {
            // FlatLaf themes
            case 0:
                laf = new FlatDarculaLaf();
                break;
            case 1:
                laf = new FlatIntelliJLaf();
                break;
            case 2:
                laf = new FlatLightLaf();
                break;
            case 3:
                laf = new FlatDarkLaf();
                break;
            // Built-in themes
            case 4:
                laf = new GTKLookAndFeel();
                break;
            case 5:
                laf = new MetalLookAndFeel();
                break;
            case 6:
                laf = new NimbusLookAndFeel();
                break;
            case 7:
                //laf = new WindowsLookAndFeel(); // only on Windows
                break;
        }
        return laf;
    }

    private void menuItemFileQuitActionPerformed(ActionEvent e)
    {
        stop();
    }

    public int run() throws Exception
    {
        logger.info("Displaying ELS Navigator");
        setVisible(true);
        return 0;
    }

    private void SaveActionPerformed(ActionEvent e)
    {
        // something
    }

    private void stop()
    {
        Main.stopVerbiage();
        setVisible(false);
        dispose();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        stop();
    }

    public JSplitPane getSplitPaneBackup() {
        return splitPaneBackup;
    }

    public JPanel getPanelNavigatorTop() {
        return panelNavigatorTop;
    }

    //<editor-fold desc="Generated code (Fold)">
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
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
        menuItem1 = new JMenuItem();
        menuItemUuidGenerator = new JMenuItem();
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
        splitPaneNavigator = new JSplitPane();
        panelNavigatorTop = new JPanel();
        panelLocationAndButtons = new JPanel();
        toolBarNavigator = new JToolBar();
        buttonPlay = new JButton();
        buttonCopy = new JButton();
        buttonMove = new JButton();
        buttonRename = new JButton();
        buttonDelete = new JButton();
        panelLocation = new JPanel();
        panel1 = new JPanel();
        button1 = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        textFieldLocation = new JTextField();
        panelLocationRight = new JPanel();
        buttonBrowse = new JButton();
        buttonToggle = new JButton();
        splitPaneBrowsers = new JSplitPane();
        tabbedPaneNavigatorLeft = new JTabbedPane();
        panelCollectionLeft = new JPanel();
        scrollPaneCollectionTreeLeft = new JScrollPane();
        treeCollectionLeft = new JTree();
        scrollPaneCollectionListLeft = new JScrollPane();
        tableCollectionListLeft = new JTable();
        panelSystemLeft = new JPanel();
        scrollPaneSystemTree = new JScrollPane();
        treeSystemLeft = new JTree();
        scrollPaneSystemListLeft = new JScrollPane();
        tableSystemListLeft = new JTable();
        tabbedPaneNavigatorRight = new JTabbedPane();
        panelCollectionRight = new JPanel();
        scrollPaneCollectionTreeRight = new JScrollPane();
        treeCollectionRight = new JTree();
        scrollPaneCollectionListRight = new JScrollPane();
        tableCollectionListRight = new JTable();
        panelSystemRight = new JPanel();
        scrollPaneSystemTreeRight = new JScrollPane();
        treeSystemRight = new JTree();
        scrollPaneSystemListRight = new JScrollPane();
        tableSystemListRight = new JTable();
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
                menuFile.setText("File");
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
                menuItemFileQuit.setText("Quit");
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

                //---- menuItem1 ----
                menuItem1.setText(bundle.getString("Navigator.menuItem1.text"));
                menuTools.add(menuItem1);

                //---- menuItemUuidGenerator ----
                menuItemUuidGenerator.setText(bundle.getString("Navigator.menuItemUuidGenerator.text"));
                menuItemUuidGenerator.setMnemonic(bundle.getString("Navigator.menuItemUuidGenerator.mnemonic").charAt(0));
                menuTools.add(menuItemUuidGenerator);
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
            panelMain.setBorder (new javax. swing. border. CompoundBorder( new javax .swing .border .TitledBorder (
            new javax. swing. border. EmptyBorder( 0, 0, 0, 0) , "JF\u006frmDes\u0069gner \u0045valua\u0074ion"
            , javax. swing. border. TitledBorder. CENTER, javax. swing. border. TitledBorder. BOTTOM
            , new java .awt .Font ("D\u0069alog" ,java .awt .Font .BOLD ,12 )
            , java. awt. Color. red) ,panelMain. getBorder( )) ); panelMain. addPropertyChangeListener (
            new java. beans. PropertyChangeListener( ){ @Override public void propertyChange (java .beans .PropertyChangeEvent e
            ) {if ("\u0062order" .equals (e .getPropertyName () )) throw new RuntimeException( )
            ; }} );
            panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));

            //======== tabbedPaneMain ========
            {

                //======== splitPaneNavigator ========
                {
                    splitPaneNavigator.setDividerLocation(450);
                    splitPaneNavigator.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    splitPaneNavigator.setLastDividerLocation(450);

                    //======== panelNavigatorTop ========
                    {
                        panelNavigatorTop.setLayout(new BorderLayout());

                        //======== panelLocationAndButtons ========
                        {
                            panelLocationAndButtons.setLayout(new BorderLayout());

                            //======== toolBarNavigator ========
                            {
                                toolBarNavigator.setFloatable(false);
                                toolBarNavigator.setMargin(new Insets(0, 4, 0, 0));

                                //---- buttonPlay ----
                                buttonPlay.setText(bundle.getString("Navigator.buttonPlay.text"));
                                toolBarNavigator.add(buttonPlay);

                                //---- buttonCopy ----
                                buttonCopy.setText(bundle.getString("Navigator.buttonCopy.text"));
                                toolBarNavigator.add(buttonCopy);

                                //---- buttonMove ----
                                buttonMove.setText(bundle.getString("Navigator.buttonMove.text"));
                                toolBarNavigator.add(buttonMove);

                                //---- buttonRename ----
                                buttonRename.setText(bundle.getString("Navigator.buttonRename.text"));
                                toolBarNavigator.add(buttonRename);

                                //---- buttonDelete ----
                                buttonDelete.setText(bundle.getString("Navigator.buttonDelete.text"));
                                toolBarNavigator.add(buttonDelete);
                            }
                            panelLocationAndButtons.add(toolBarNavigator, BorderLayout.NORTH);

                            //======== panelLocation ========
                            {
                                panelLocation.setLayout(new BorderLayout());

                                //======== panel1 ========
                                {
                                    panel1.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panel1.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                                    ((GridBagLayout)panel1.getLayout()).rowHeights = new int[] {0, 0};
                                    ((GridBagLayout)panel1.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0, 1.0E-4};
                                    ((GridBagLayout)panel1.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                                    //---- button1 ----
                                    button1.setText("Home");
                                    panel1.add(button1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- button2 ----
                                    button2.setText("<");
                                    button2.setFont(new Font("Ubuntu", Font.PLAIN, 18));
                                    button2.setMaximumSize(new Dimension(36, 30));
                                    button2.setMinimumSize(new Dimension(36, 30));
                                    button2.setPreferredSize(new Dimension(36, 30));
                                    panel1.add(button2, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- button3 ----
                                    button3.setText(">");
                                    button3.setFont(new Font("Ubuntu", Font.PLAIN, 18));
                                    button3.setMaximumSize(new Dimension(36, 30));
                                    button3.setMinimumSize(new Dimension(36, 30));
                                    button3.setPreferredSize(new Dimension(36, 30));
                                    panel1.add(button3, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                panelLocation.add(panel1, BorderLayout.WEST);

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
                                    panelLocationRight.add(buttonBrowse, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));

                                    //---- buttonToggle ----
                                    buttonToggle.setText("Toggle");
                                    panelLocationRight.add(buttonToggle, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                panelLocation.add(panelLocationRight, BorderLayout.EAST);
                            }
                            panelLocationAndButtons.add(panelLocation, BorderLayout.SOUTH);
                        }
                        panelNavigatorTop.add(panelLocationAndButtons, BorderLayout.NORTH);

                        //======== splitPaneBrowsers ========
                        {
                            splitPaneBrowsers.setDividerLocation(506);
                            splitPaneBrowsers.setLastDividerLocation(516);

                            //======== tabbedPaneNavigatorLeft ========
                            {
                                tabbedPaneNavigatorLeft.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneNavigatorLeft.setPreferredSize(new Dimension(400, 427));

                                //======== panelCollectionLeft ========
                                {
                                    panelCollectionLeft.setLayout(new BorderLayout());

                                    //======== scrollPaneCollectionTreeLeft ========
                                    {
                                        scrollPaneCollectionTreeLeft.setPreferredSize(new Dimension(180, 362));
                                        scrollPaneCollectionTreeLeft.setViewportView(treeCollectionLeft);
                                    }
                                    panelCollectionLeft.add(scrollPaneCollectionTreeLeft, BorderLayout.WEST);

                                    //======== scrollPaneCollectionListLeft ========
                                    {

                                        //---- tableCollectionListLeft ----
                                        tableCollectionListLeft.setPreferredSize(new Dimension(200, 32));
                                        tableCollectionListLeft.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                        scrollPaneCollectionListLeft.setViewportView(tableCollectionListLeft);
                                    }
                                    panelCollectionLeft.add(scrollPaneCollectionListLeft, BorderLayout.CENTER);
                                }
                                tabbedPaneNavigatorLeft.addTab(bundle.getString("Navigator.panelCollectionLeft.tab.title"), panelCollectionLeft);

                                //======== panelSystemLeft ========
                                {
                                    panelSystemLeft.setLayout(new BorderLayout());

                                    //======== scrollPaneSystemTree ========
                                    {
                                        scrollPaneSystemTree.setPreferredSize(new Dimension(180, 362));
                                        scrollPaneSystemTree.setViewportView(treeSystemLeft);
                                    }
                                    panelSystemLeft.add(scrollPaneSystemTree, BorderLayout.WEST);

                                    //======== scrollPaneSystemListLeft ========
                                    {

                                        //---- tableSystemListLeft ----
                                        tableSystemListLeft.setPreferredSize(new Dimension(754, 32));
                                        tableSystemListLeft.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                        scrollPaneSystemListLeft.setViewportView(tableSystemListLeft);
                                    }
                                    panelSystemLeft.add(scrollPaneSystemListLeft, BorderLayout.CENTER);
                                }
                                tabbedPaneNavigatorLeft.addTab(bundle.getString("Navigator.panelSystemLeft.tab.title"), panelSystemLeft);
                            }
                            splitPaneBrowsers.setLeftComponent(tabbedPaneNavigatorLeft);

                            //======== tabbedPaneNavigatorRight ========
                            {
                                tabbedPaneNavigatorRight.setTabPlacement(SwingConstants.LEFT);
                                tabbedPaneNavigatorRight.setPreferredSize(new Dimension(400, 427));

                                //======== panelCollectionRight ========
                                {
                                    panelCollectionRight.setLayout(new BorderLayout());

                                    //======== scrollPaneCollectionTreeRight ========
                                    {
                                        scrollPaneCollectionTreeRight.setPreferredSize(new Dimension(180, 362));
                                        scrollPaneCollectionTreeRight.setViewportView(treeCollectionRight);
                                    }
                                    panelCollectionRight.add(scrollPaneCollectionTreeRight, BorderLayout.WEST);

                                    //======== scrollPaneCollectionListRight ========
                                    {

                                        //---- tableCollectionListRight ----
                                        tableCollectionListRight.setPreferredSize(new Dimension(754, 32));
                                        tableCollectionListRight.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                        scrollPaneCollectionListRight.setViewportView(tableCollectionListRight);
                                    }
                                    panelCollectionRight.add(scrollPaneCollectionListRight, BorderLayout.CENTER);
                                }
                                tabbedPaneNavigatorRight.addTab(bundle.getString("Navigator.panelCollectionRight.tab.title"), panelCollectionRight);

                                //======== panelSystemRight ========
                                {
                                    panelSystemRight.setLayout(new BorderLayout());

                                    //======== scrollPaneSystemTreeRight ========
                                    {
                                        scrollPaneSystemTreeRight.setPreferredSize(new Dimension(180, 362));
                                        scrollPaneSystemTreeRight.setViewportView(treeSystemRight);
                                    }
                                    panelSystemRight.add(scrollPaneSystemTreeRight, BorderLayout.WEST);

                                    //======== scrollPaneSystemListRight ========
                                    {

                                        //---- tableSystemListRight ----
                                        tableSystemListRight.setPreferredSize(new Dimension(200, 32));
                                        tableSystemListRight.setPreferredScrollableViewportSize(new Dimension(754, 400));
                                        scrollPaneSystemListRight.setViewportView(tableSystemListRight);
                                    }
                                    panelSystemRight.add(scrollPaneSystemListRight, BorderLayout.CENTER);
                                }
                                tabbedPaneNavigatorRight.addTab(bundle.getString("Navigator.panelSystemRight.tab.title"), panelSystemRight);
                            }
                            splitPaneBrowsers.setRightComponent(tabbedPaneNavigatorRight);
                        }
                        panelNavigatorTop.add(splitPaneBrowsers, BorderLayout.CENTER);
                    }
                    splitPaneNavigator.setTopComponent(panelNavigatorTop);

                    //======== tabbedPaneNavigatorBottom ========
                    {
                        tabbedPaneNavigatorBottom.setTabPlacement(SwingConstants.BOTTOM);
                        tabbedPaneNavigatorBottom.setPreferredSize(new Dimension(70, 170));

                        //======== scrollPaneFind ========
                        {
                            scrollPaneFind.setViewportView(listFind);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneFind.tab.title"), scrollPaneFind);

                        //======== scrollPaneContent ========
                        {
                            scrollPaneContent.setViewportView(textPaneContent);
                        }
                        tabbedPaneNavigatorBottom.addTab(bundle.getString("Navigator.scrollPaneContent.tab.title"), scrollPaneContent);
                    }
                    splitPaneNavigator.setBottomComponent(tabbedPaneNavigatorBottom);
                }
                tabbedPaneMain.addTab(bundle.getString("Navigator.splitPaneNavigator.tab.title"), splitPaneNavigator);

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
    // Generated using JFormDesigner Evaluation license - unknown
    protected JMenuBar menuBarMain;
    protected JMenu menuFile;
    protected JMenuItem menuItemNew;
    protected JMenuItem menuItemOpen;
    protected JMenuItem menuItemSave;
    protected JMenuItem menuItemSaveAll;
    protected JMenuItem menuItem8;
    protected JMenuItem menuItemFileQuit;
    protected JMenu menuEdit;
    protected JMenuItem menuItemCopy;
    protected JMenuItem menuItemCut;
    protected JMenuItem menuItemPaste;
    protected JMenuItem menuItemPreferences;
    protected JMenu menuBookmarks;
    protected JMenuItem menuItemShowAllBookmarks;
    protected JMenuItem menuItemAddBookmark;
    protected JMenu menuTools;
    protected JMenuItem menuItemExternalTools;
    protected JMenu menuRunSubMenu;
    protected JMenuItem menuItem1;
    protected JMenuItem menuItemUuidGenerator;
    protected JMenu menuWindows;
    protected JMenuItem menuItemMaximize;
    protected JMenuItem menuItemMinimize;
    protected JMenuItem menuItemFullScreen;
    protected JMenuItem menuItemSplitHorizontal;
    protected JMenuItem menuItemSplitVertical;
    protected JMenu menuHelp;
    protected JMenuItem menuItemDocumentation;
    protected JMenuItem menuItemAbout;
    protected JPanel panelMain;
    protected JTabbedPane tabbedPaneMain;
    protected JSplitPane splitPaneNavigator;
    protected JPanel panelNavigatorTop;
    protected JPanel panelLocationAndButtons;
    protected JToolBar toolBarNavigator;
    protected JButton buttonPlay;
    protected JButton buttonCopy;
    protected JButton buttonMove;
    protected JButton buttonRename;
    protected JButton buttonDelete;
    protected JPanel panelLocation;
    protected JPanel panel1;
    protected JButton button1;
    protected JButton button2;
    protected JButton button3;
    protected JTextField textFieldLocation;
    protected JPanel panelLocationRight;
    protected JButton buttonBrowse;
    protected JButton buttonToggle;
    protected JSplitPane splitPaneBrowsers;
    protected JTabbedPane tabbedPaneNavigatorLeft;
    protected JPanel panelCollectionLeft;
    protected JScrollPane scrollPaneCollectionTreeLeft;
    protected JTree treeCollectionLeft;
    protected JScrollPane scrollPaneCollectionListLeft;
    protected JTable tableCollectionListLeft;
    protected JPanel panelSystemLeft;
    protected JScrollPane scrollPaneSystemTree;
    protected JTree treeSystemLeft;
    protected JScrollPane scrollPaneSystemListLeft;
    protected JTable tableSystemListLeft;
    protected JTabbedPane tabbedPaneNavigatorRight;
    protected JPanel panelCollectionRight;
    protected JScrollPane scrollPaneCollectionTreeRight;
    protected JTree treeCollectionRight;
    protected JScrollPane scrollPaneCollectionListRight;
    protected JTable tableCollectionListRight;
    protected JPanel panelSystemRight;
    protected JScrollPane scrollPaneSystemTreeRight;
    protected JTree treeSystemRight;
    protected JScrollPane scrollPaneSystemListRight;
    protected JTable tableSystemListRight;
    protected JTabbedPane tabbedPaneNavigatorBottom;
    protected JScrollPane scrollPaneFind;
    protected JList listFind;
    protected JScrollPane scrollPaneContent;
    protected JTextPane textPaneContent;
    protected JSplitPane splitPaneBackup;
    protected JPanel panelProfiles;
    protected JPanel panelKeys;
    protected JPanel panelStatus;
    protected JLabel labelStatusLeft;
    protected JLabel labelStatusMiddle;
    protected JLabel labelStatusRight;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    //</editor-fold>

}
