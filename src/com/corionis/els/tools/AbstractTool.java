package com.corionis.els.tools;

import com.corionis.els.jobs.Task;
import com.corionis.els.Configuration;
import com.corionis.els.Context;
import com.corionis.els.Utils;

import java.io.Serializable;

/**
 * AbstractTool class
 * <br/>
 * Match the inheritor class name and it's .displayName key in locales,
 * e.g. JunkRemoverTool class and JunkRemoverTool.displayName locale key.
 */
public abstract class AbstractTool implements Comparable, Serializable
{
    transient protected Context context;
    transient protected boolean dataHasChanged = false;
    transient protected String displayName; // GUI i18n display name
    transient protected boolean isRemote;
    transient protected boolean stop = false;

    private AbstractTool()
    {
        // hide default constructor
    }

    /**
     * Constructor
     */
    public AbstractTool(Context context)
    {
        this.context = context;
        this.dataHasChanged = false;
    }

    abstract public Object clone();

    public int compareTo(Object o)
    {
        return toString().compareTo(o.toString());
    }

    public Configuration getCfg()
    {
        return context.cfg;
    }

    abstract public String getConfigName();

    public Context getContext()
    {
        return context;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") +
                (getSubsystem().length() > 0 ? getSubsystem() + System.getProperty("file.separator") : "") +
                getInternalName();
        return path;
    }

    abstract public String getDisplayName();

    public String getListName()
    {
        return getDisplayName() + ": " + getConfigName();
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    abstract public String getInternalName();

    abstract public String getSubsystem();

    abstract public boolean isDataChanged();

    public boolean isRemote()
    {
        return isRemote;
    }

    public boolean isRequestStop()
    {
        return stop;
    }

    /**
     * Does this tool use cached origins from a previous task?
     * <br/>Override to change.
     * @return boolean, default false
     */
    public boolean isToolCachedOrigins()
    {
        return false;
    }

    /**
     * Is this tool a Hint Server?
     * <br/>Override to change.
     * @return boolean, default false
     */
    public boolean isToolHintServer()
    {
        return false;
    }

    /**
     * Does this tool use origins?
     * <br/>Override to change.
     * @return boolean, default false
     */
    public boolean isToolOriginsUsed()
    {
        return false;
    }

    /**
     * Does this tool use publisher OR subscriber if enabled?
     * <br/>Override to change.
     * @return boolean, default false uses publisher AND subscriber if enabled
     */
    public boolean isToolPubOrSub()
    {
        return false;
    }

    /**
     * Does this tool use a publisher?
     * <br/>Override to change.
     * @return boolean, default true
     */
    public boolean isToolPublisher()
    {
        return true;
    }

    /**
     * Does this tool use a subscriber?
     * <br/>Override to change.
     * @return boolean, default true
     */
    public boolean isToolSubscriber()
    {
        return true;
    }

    /**
     * Process the tool
     *
     * @param task Current Task
     * @throws Exception
     */
    public abstract void processTool(Task task) throws Exception;

    /**
     * Request the tool to stop (optional)
     */
    public void requestStop()
    {
        this.stop = true;
    }

    public void resetStop()
    {
        this.stop = false;
    }

    abstract public void setConfigName(String configName);

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    public void setDataHasChanged(boolean state)
    {
        dataHasChanged = state;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setRemote(boolean remote)
    {
        isRemote = remote;
    }

    @Override
    public String toString()
    {
        return getConfigName();
    }

}
