package com.groksoft.els.gui.tools;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class AbstractTool implements Serializable
{
    // @formatter:off
    private String internalName; // internal name
    private String configName; // user name for this instance
    private boolean dryRun = false;
    private String arguments = "";

    protected transient static Logger logger = LogManager.getLogger("applog");
    private transient Configuration cfg;
    private transient Context context;
    private transient String displayName; // GUI i18n display name
    private transient boolean stop = false;
    // @formatter:on

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
     * Replace arguments with GUI data
     *
     * @throws Exception
     */
    protected abstract void argsReplace() throws Exception;

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
            argsReplace();
            argsTest();
        }
        catch (Exception e)
        {
            result = e.getMessage();
        }
        return result;
    }

}
