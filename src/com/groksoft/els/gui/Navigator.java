package com.groksoft.els.gui;

import com.groksoft.els.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ResourceBundle;

public class Navigator
{
    private transient Logger logger = LogManager.getLogger("applog");
    ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");

    private Configuration cfg;
    private Context context;
    private Main els;
    private GuiContext guiContext;

    // QUESTION:
    //  1. How to organize editing JSON server and targets files with N-libraries with N-sources each?
    //      a. A tree control of JSON nodes and values with add/delete?

    // TODO:
    //  ! TEST Hints with spread-out files, e.g. TV Show in two locations.
    //  * Add Navigator preferences class & file that holds the LaF, position, size, options, etc.
    //  * Display Collection:
    //     * Whole tree
    //     * !-Z alphabetic
    //     * By-source

    public Navigator(Main main, Configuration config, Context ctx)
    {
        els = main;
        cfg = config;
        context = ctx;
        guiContext = new GuiContext();
    }

    /**
     * Initialize everything for the GUI
     *
     * @return true if successful, false if a fault occurred
     */
    private boolean initialize()
    {
        context.transfer = new Transfer(cfg, context);
        try
        {
            context.transfer.initialize();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
            return false;
        }

        guiContext.form = new MainFrame(els, this, cfg, context);
        if (!context.fault)
        {
            guiContext.browser = new Browser(this, cfg, context, guiContext);

        }
        return !context.fault;
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
        if (context.clientStty != null)
        {
            String resp;
            try
            {
                resp = context.clientStty.roundTrip("quit");
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
        if (context.fault)
        {
            System.exit(1);
        }
    }

    public class GuiContext
    {
        Browser browser;
        MainFrame form;
    }

}
