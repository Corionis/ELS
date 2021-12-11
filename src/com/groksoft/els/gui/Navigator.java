package com.groksoft.els.gui;

import com.groksoft.els.*;
import jdk.jfr.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class Navigator
{
    public static GuiContext guiContext;
    ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private transient Logger logger = LogManager.getLogger("applog");

    // QUESTION:
    //  1. How to organize editing JSON server and targets files with N-libraries with N-sources each?
    //      a. A tree control of JSON nodes and values with add/delete?

    // TODO:
    //  ! TEST Hints with spread-out files, e.g. TV Show in two locations.
    //  * Display Collection:
    //     * Whole tree
    //     * !-Z alphabetic
    //     * By-source
    //  * View, Wrap log lines (toggle)
    //  * Help, Controls simple dialog showing all the keys and mouse controls

    public Navigator(Main main, Configuration config, Context ctx)
    {
        guiContext = new GuiContext();
        guiContext.cfg = config;
        guiContext.context = ctx;
        guiContext.navigator = this;
        guiContext.preferences = new Preferences();
        guiContext.cfg.setLongScale(guiContext.preferences.isBinaryScale());
    }

    /**
     * Initialize everything for the GUI
     *
     * @return true if successful, false if a fault occurred
     */
    private boolean initialize()
    {
        // setup the needed tools
        guiContext.context.transfer = new Transfer(guiContext.cfg, guiContext.context);
        try
        {
            guiContext.context.transfer.initialize();
            guiContext.preferences.initialize();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
            return false;
        }

        // setup the GUI
        guiContext.form = new MainFrame(guiContext);
        if (!guiContext.context.fault)
        {
            // setup the Main Menu and primary tabs
            initializeMainMenu();
            guiContext.browser = new Browser(guiContext);

            // TODO Add Backup, Profiles and Keys creation here

        }

        // disable back-fill
        guiContext.cfg.setNoBackFill(true);

        guiContext.cfg.setPreserveDates(guiContext.preferences.isPreserveFileTime());

//        Thread.setDefaultUncaughtExceptionHandler( (thread, throwable) -> {
//            logger.error("GOT IT: " + Utils.getStackTrace(throwable));
//        });

/*
        PrintStream originalOut = System.out;
        System.setErr(new PrintStream(new OutputStream()
        {
            @Override
            public void write(int i) throws IOException
            {

            }
        }));
*/

        return !guiContext.context.fault;
    }

    private void initializeMainMenu()
    {
        // --- Main Menu ------------------------------------------
        //
        // -- File Menu

        //
        // -- Edit Menu
        //
        // New Folder
        guiContext.form.menuItemNewFolder.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.context.fault = false;
                boolean tooMany = false;
                JTree tree = null;
                NavTreeUserObject tuo = null;
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    tree = (JTree) object;
                }
                else if (object instanceof JTable)
                {
                    tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
                }
                assert (tree != null);

                TreePath[] paths = tree.getSelectionPaths();
                if (paths.length == 1)
                {
                    NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                    tuo = ntn.getUserObject();
                }
                else if (paths.length == 0)
                    return;
                else
                    tooMany = true;

                if (!tooMany)
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
                            int opt = JOptionPane.showOptionDialog(guiContext.form, "Select 1 of " + tuo.sources.length + " locations in library " + tuo.name + ":",
                                    guiContext.cfg.getNavigatorName(), JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, tuo.sources, tuo.sources[0]);
                            if (opt > -1)
                            {
                                path = tuo.sources[opt];
                            }
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(guiContext.form, "Cannot create new folder in current location", guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String reply = "";
                    if (path.length() > 0)
                    {
// TODO Check target is writable
                        reply = JOptionPane.showInputDialog(guiContext.form, "New folder for " + path + ": ", guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE);
                        if (reply != null && reply.length() > 0)
                        {
                            NavTreeUserObject createdTuo = null;
                            try
                            {
                                path = path + Utils.getSeparatorFromPath(path) + reply;
                                String msg = "Creating " + (tuo.isRemote ? "remote " : "") + "directory " + path;
                                guiContext.browser.printLog(msg);
// LEFTOFF Tree stops working to makeDirs fails ... WTF?????????????????????
                                if (!guiContext.context.transfer.makeDirs((tuo.isRemote ? path + Utils.getSeparatorFromPath(path) + "dummyfile.txt" : path), true, tuo.isRemote))
                                    throw new MungeException("fake");

                                // make tuo and add node
                                NavTreeNode createdNode = new NavTreeNode(guiContext, tree);
                                if (tuo.isRemote)
                                {
                                    createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(path, null),
                                            path, 0, LocalTime.now().toSecondOfDay(), true);
                                }
                                else
                                {
                                    createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(path, null), new File(path));
                                }
                                createdNode.setNavTreeUserObject(createdTuo);
                                createdNode.setVisible(true);
                                tuo.node.add(createdNode);
                            }
                            catch (Exception e)
                            {
                                guiContext.context.fault = false;
                                JOptionPane.showMessageDialog(guiContext.form, "Error creating " + (tuo.isRemote ? "remote " : "") + "directory " + path, guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);                                guiContext.context.fault = true;
                            }
                            guiContext.browser.refreshByObject(tree);
                            if (!guiContext.context.fault)
                            {
                                if (object instanceof JTree)
                                    tuo.node.selectMe();
                                else
                                {
                                    // update table & select relevant row
                                    tuo.node.loadTable();
                                    if (createdTuo != null)
                                    {
                                        int row = guiContext.browser.findRowIndex((JTable) object, createdTuo);
                                        if (row > -1)
                                        {
                                            ((JTable) object).setRowSelectionInterval(row, row);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Please select a single destination for a new folder", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }

            }
        });
        //
        // Rename
        guiContext.form.menuItemRename.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.context.fault = false;
                int[] rows = {0};
                boolean tooMany = false;
                JTree tree = null;
                NavTreeUserObject tuo = null;
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    tree = (JTree) object;
                    TreePath[] paths = tree.getSelectionPaths();
                    if (paths.length == 1)
                    {
                        NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                        tuo = ntn.getUserObject();
                    }
                    else if (paths.length == 0)
                        return;
                    else
                        tooMany = true;
                }
                else if (object instanceof JTable)
                {
                    tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
                    rows = ((JTable) object).getSelectedRows();
                    if (rows.length == 1)
                    {
                        tuo = (NavTreeUserObject) ((JTable) object).getValueAt(rows[0], 1);
                    }
                    else if (rows.length == 0)
                        return;
                    else
                        tooMany = true;
                }

                if (!tooMany)
                {
                    String name = "";
                    String path = "";
                    if (tuo.type == NavTreeUserObject.REAL)
                    {
                        name = tuo.name;
                        path = tuo.path;
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(guiContext.form, "Cannot rename current location", guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String reply = "";
                    if (path.length() > 0)
                    {

                        reply = JOptionPane.showInputDialog(guiContext.form, "Rename " + name + " to: ", guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE);
                        //
                        // TODO Rename object here
                        //
                        guiContext.browser.refreshByObject(tree);
                        if (object instanceof JTree)
                            tuo.node.selectMe();
                        else
                            ((JTable) object).setRowSelectionInterval(rows[0], rows[0]);
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Please select a single item to be renamed", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        // Copy
        guiContext.form.menuItemCopy.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    guiContext.context.fault = false;
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "copy");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        // Cut
        guiContext.form.menuItemCut.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    guiContext.context.fault = false;
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "cut");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        // Paste
        guiContext.form.menuItemPaste.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    guiContext.context.fault = false;
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "paste");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        // Delete
        guiContext.form.menuItemDelete.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.context.fault = false;
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    guiContext.browser.deleteSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    guiContext.browser.deleteSelected(sourceTable);
                }
            }
        });

        //
        // -- View Menu
        guiContext.form.menuItemRefresh.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.browser.refreshByObject(guiContext.browser.lastComponent);
            }
        });
        //
        guiContext.form.menuItemShowHidden.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.preferences.setHideHiddenFiles(!guiContext.preferences.isHideHiddenFiles());
                if (guiContext.preferences.isHideHiddenFiles())
                    guiContext.form.menuItemShowHidden.setSelected(false);
                else
                    guiContext.form.menuItemShowHidden.setSelected(true);

                guiContext.browser.refreshTree(guiContext.form.treeCollectionOne);
                guiContext.browser.refreshTree(guiContext.form.treeSystemOne);
                guiContext.browser.refreshTree(guiContext.form.treeCollectionTwo);
                guiContext.browser.refreshTree(guiContext.form.treeSystemTwo);
            }
        });
        // set initial state of checkbox
        if (guiContext.preferences.isHideHiddenFiles())
            guiContext.form.menuItemShowHidden.setSelected(false);
        else
            guiContext.form.menuItemShowHidden.setSelected(true);

        //
        // -- Bookmarks Menu

        //
        // -- Tools Menu

        //
        // -- Window Menu
        guiContext.form.menuItemMaximize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(guiContext.form.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });
        //
        guiContext.form.menuItemMinimize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setState(JFrame.ICONIFIED);
            }
        });
        //
        guiContext.form.menuItemRestore.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(JFrame.NORMAL);
            }
        });
        //
        guiContext.form.menuItemSplitHorizontal.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getHeight();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.VERTICAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });
        //
        guiContext.form.menuItemSplitVertical.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getWidth();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // -- Help Menu
        //
        guiContext.form.menuItemDocumentation.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS/wiki");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        //
        guiContext.form.menuItemGitHubProject.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public int run() throws Exception
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                logger.info("Initializing Navigator");
                if (initialize())
                {
                    logger.info("Displaying Navigator");
                    guiContext.form.setVisible(true);

                    guiContext.preferences.setBrowserBottomSize(guiContext.form.tabbedPaneNavigatorBottom.getHeight());

                    String os = Utils.getOS();
                    logger.debug("Detected local system as " + os);
                    guiContext.form.labelStatusMiddle.setText("Detected local system as " + os);
                }
                else
                {
                    stop();
                    guiContext.form = null; // failed
                }
            }
        });
        return 0;
    }

    public void stop()
    {
        // tell remote end to exit
        if (guiContext.context.clientStty != null)
        {
            String resp;
            try
            {
                resp = guiContext.context.clientStty.roundTrip("quit");
            }
            catch (Exception e)
            {
                resp = null;
            }
            if (resp != null && !resp.equalsIgnoreCase("End-Execution"))
            {
                logger.warn("Remote might not have quit");
            }
            else if (resp == null)
            {
                logger.warn("Remote is in an unknown state");
            }
        }

        // report stats and shutdown
        Main.stopVerbiage();
        if (guiContext.form != null)
        {
            guiContext.form.setVisible(false);
            guiContext.form.dispose();
        }

        // stop the program if something blew-up
        if (guiContext.context.fault)
        {
            System.exit(1);
        }
    }

}
