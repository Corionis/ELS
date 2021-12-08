package com.groksoft.els.gui;

import com.groksoft.els.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
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
        guiContext.fileSystemView = FileSystemView.getFileSystemView();
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
        guiContext.form.menuItemCopy.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "copy");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        //
        guiContext.form.menuItemCut.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "cut");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        //
        guiContext.form.menuItemPaste.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "paste");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        });
        //
        guiContext.form.menuItemDelete.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
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
