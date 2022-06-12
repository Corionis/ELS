package com.groksoft.els.tools;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * AbstractTool class
 * <br/>
 * Match the inheritor class name and it's .displayName key in locales,
 * e.g. JunkRemoverTool class and JunkRemoverTool.displayName locale key.
 */
public abstract class AbstractTool implements Comparable, Serializable
{
    transient protected Configuration cfg;
    transient protected Context context;
    transient protected boolean includeInToolsList = true; // set by tool at runtime
    transient private boolean isRemote;
    transient private String displayName; // GUI i18n display name
    transient private boolean stop = false;

    /**
     * Constructor
     */
    public AbstractTool(Configuration config, Context ctxt)
    {
        this.cfg = config;
        this.context = ctxt;
    }

    public int compareTo(Object o)
    {
        return toString().compareTo(((JunkRemoverTool)o).toString());
    }

    public Configuration getCfg()
    {
        return cfg;
    }

    abstract public String getConfigName();

    public Context getContext()
    {
        return context;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                getSubsystem() + System.getProperty("file.separator") +
                getInternalName();
        return path;
    }

    abstract public String getDisplayName();

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    abstract public String getInternalName();

    abstract public String getSubsystem();

    abstract public boolean isDualRepositories();

    public boolean isIncludeInToolsList()
    {
        return includeInToolsList;
    }

    public boolean isRemote()
    {
        return isRemote;
    }

    public boolean isRequestStop()
    {
        return stop;
    }

    /**
     * Process the tool
     *
     * @param publisherRepo Repository of the publisher or null
     * @param subscriberRepo Repository of the subscriber or null
     * @param origins ArrayList<Origin> of starting locations
     * @param dryRun Boolean dryrun
     * @throws Exception
     */
    public abstract void processTool(Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun) throws Exception;

    /**
     * Process the tool on a SwingWorker thread
     *
     * @param publisherRepo Repository of the publisher or null
     * @param subscriberRepo Repository of the subscriber or null
     * @param origins ArrayList<Origin> of starting locations
     * @param dryRun Boolean dryrun
     * @return SwingWorker<Void, Void> of thread
     */
    public abstract SwingWorker<Void, Void> processToolThread(Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun);

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

    public void setContext(Configuration config, Context context)
    {
        this.cfg = config;
        this.context = context;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public void setIncludeInToolsList(boolean includeInToolsList)
    {
        this.includeInToolsList = includeInToolsList;
    }

    public void setIsRemote(boolean remote)
    {
        isRemote = remote;
    }

    @Override
    public String toString()
    {
        return getConfigName();
    }

}
