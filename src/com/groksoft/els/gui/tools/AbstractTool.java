package com.groksoft.els.gui.tools;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class AbstractTool implements Serializable
{
    private String configName; // user name for this instance
    private String internalName; // internal name

    transient private boolean isRemote;
    transient private boolean isSubscriber = false;

    transient protected Logger logger = LogManager.getLogger("applog");
    transient protected Configuration cfg;
    transient protected Context context;
    transient private String displayName; // GUI i18n display name
    transient private boolean dryRun = false;
    transient protected boolean includeInToolsList = true; // set by tool at runtime
    transient private boolean stop = false;

    /**
     * Constructor
     */
    public AbstractTool(Configuration config, Context ctxt, String internalId)
    {
        this.cfg = config;
        this.context = ctxt;
        this.internalName = internalId;
    }

    public Configuration getCfg()
    {
        return cfg;
    }

    public String getConfigName()
    {
        return configName;
    }

    public Context getContext()
    {
        return context;
    }

    public String getDisplayName()
    {
        return displayName;
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

    public String getInternalName()
    {
        return internalName;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public boolean isRemote()
    {
        return isRemote;
    }

    public boolean isSubscriber()
    {
        return isSubscriber;
    }

    public boolean isRequestStop()
    {
        return stop;
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
    public abstract void processTool() throws Exception;

    /**
     * Request the tool to stop (optional)
     */
    public void requestStop()
    {
        this.stop = true;
    }

    public void resetRequestStop()
    {
        this.stop = false;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
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

    public void setIsRemote(boolean remote)
    {
        isRemote = remote;
    }

    public void setIsSubscriber(boolean flag)
    {
        this.isSubscriber = flag;
    }

    @Override
    public String toString()
    {
        return getConfigName();
    }

}
