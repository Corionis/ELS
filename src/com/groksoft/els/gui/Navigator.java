package com.groksoft.els.gui;

import com.groksoft.els.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.filechooser.FileSystemView;
import java.util.ResourceBundle;

public class Navigator
{
    private transient Logger logger = LogManager.getLogger("applog");
    ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");

    public static GuiContext guiContext;

    // QUESTION:
    //  1. How to organize editing JSON server and targets files with N-libraries with N-sources each?
    //      a. A tree control of JSON nodes and values with add/delete?

    // TODO:
    //  ! TEST Hints with spread-out files, e.g. TV Show in two locations.
    //  * Display Collection:
    //     * Whole tree
    //     * !-Z alphabetic
    //     * By-source
    //  * Add subscriber command to Request Library, not collection; Use to prime Browser then use lazy loading;
    //    then skip subscriber-side commands for RequestCollection and RequestTargets

    public Navigator(Main main, Configuration config, Context ctx)
    {
        guiContext = new GuiContext();
        guiContext.cfg = config;
        guiContext.context = ctx;
        guiContext.els = main;
        guiContext.fileSystemView = FileSystemView.getFileSystemView();
        guiContext.navigator = this;
        guiContext.preferences = new Preferences();
    }

    /**
     * Initialize everything for the GUI
     *
     * @return true if successful, false if a fault occurred
     */
    private boolean initialize()
    {
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

        guiContext.form = new MainFrame(guiContext);
        if (!guiContext.context.fault)
        {
            guiContext.browser = new Browser(guiContext);
        }
        return !guiContext.context.fault;
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
