package com.groksoft.els.gui.libraries;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LibrariesUI
{
    public static final String INTERNAL_NAME = "libraries";
    private String currentLibraryName = "";
    private String displayName;
    private GuiContext guiContext;
    private Logger logger = LogManager.getLogger("applog");

    private LibrariesUI()
    {
        // hide default constructor
    }

    public LibrariesUI(GuiContext guiContext)
    {
        this.guiContext = guiContext;
        this.displayName = guiContext.cfg.gs("Operations.displayName");
    }

    public String getConfigName()
    {
        return currentLibraryName;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                (getSubsystem().length() > 0 ? getSubsystem() + System.getProperty("file.separator") : "") +
                getInternalName();
        return path;
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    public String getInternalName()
    {
        return INTERNAL_NAME;
    }

    public String getSubsystem()
    {
        return "";
    }


}
