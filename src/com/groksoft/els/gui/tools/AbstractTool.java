package com.groksoft.els.gui.tools;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class AbstractTool implements Serializable
{
    //@formatter:off

    // data members
    private String internalName; // internal name
    private String configName; // user name for this instance

    // runtime data
    transient private String arguments = "";
    transient protected static Logger logger = LogManager.getLogger("applog");
    transient private Configuration cfg;
    transient private Context context;
    transient private boolean dryRun = false;
    transient private String displayName; // GUI i18n display name
    transient private boolean includeInToolsList = true; // set by tool at runtime
    transient private boolean stop = false;

    //@formatter:on

    /**
     * Constructor
     */
    public AbstractTool(Configuration config, Context ctxt, String internalId)
    {
        this.cfg = config;
        this.context = ctxt;
        this.internalName = internalId;
    }

    /**
     * Parse arguments for this tool
     *
     * @throws Exception
     */
    protected abstract void argsParse() throws Exception;

    /**
     * Test values of arguments for sanity
     *
     * @throws Exception
     */
    protected abstract void argsTest() throws Exception;

    public String getArguments()
    {
        return arguments;
    }

    public Configuration getCfg()
    {
        return cfg;
    }

    public Context getContext()
    {
        return context;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getInternalName()
    {
        return internalName;
    }

    public String getConfigName()
    {
        return configName;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                "tools" + System.getProperty("file.separator") +
                getInternalName();
        return path;
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator")+
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public boolean isIncludeInToolsList()
    {
        return includeInToolsList;
    }

    /**
     * Process the tool with the arguments provided
     *
     * @throws Exception
     */
    public abstract boolean process() throws Exception;

    /**
     * Request the tool to stop (optional)
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("Requesting stop for: " + configName + ":" + internalName);
    }

    public void setArguments(String arguments)
    {
        this.arguments = arguments;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    public void setIncludeInToolsList(boolean includeInToolsList)
    {
        this.includeInToolsList = includeInToolsList;
    }

    public void setInternalName(String internalName)
    {
        this.internalName = internalName;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    @Override
    public String toString()
    {
        return getConfigName();
    }

    /**
     * Validate the settings for this tool
     *
     * @return An error message or an empty string if the settings are valid
     */
    public String validate()
    {
        String result = "";
        try
        {
            argsParse();
            argsTest();
        }
        catch (Exception e)
        {
            result = e.getMessage();
        }
        return result;
    }

}
