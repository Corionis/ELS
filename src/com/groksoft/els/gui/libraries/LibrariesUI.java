package com.groksoft.els.gui.libraries;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
    IDEA
        + When adding a Target or Location use a dialog with a checkbox
          for each library and an All checkbox to select which libraries
          should have that storage space added to it.
 */

public class LibrariesUI
{
    private Context context;
    private String currentLibraryName = "";
    private String displayName;
    private Logger logger = LogManager.getLogger("applog");

    private LibrariesUI()
    {
        // hide default constructor
    }

    public LibrariesUI(Context context)
    {
        this.context = context;
        this.displayName = context.cfg.gs("Operations.displayName");
    }

    public String getConfigName()
    {
        return currentLibraryName;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "libraries";
        return path;
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    public String getSubsystem()
    {
        return "";
    }


}
