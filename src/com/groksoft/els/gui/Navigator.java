package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Main;
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
        logger.info("Initializing Navigator");
        guiContext.form = new MainFrame(els, this, cfg, context);
        if (!context.fault)
        {
            guiContext.browser = new Browser(this, cfg, context, guiContext);

        }
        return !context.fault;
    }

    public int run() throws Exception
    {
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
        return 0;
    }

    public void stop()
    {
        Main.stopVerbiage();
        if (guiContext.form != null)
        {
            guiContext.form.setVisible(false);
            guiContext.form.dispose();
        }
    }

    public class GuiContext
    {
        Browser browser;
        MainFrame form;
    }

}
