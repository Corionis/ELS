package com.groksoft.els.tools;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;

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
    transient protected Context context;
    transient protected String displayName; // GUI i18n display name
    transient protected boolean includeInToolsList = true; // set by tool at runtime
    transient private boolean isRemote;
    transient protected boolean originPathsAllowed = true; // set by tool at runtime
    transient private boolean stop = false;

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
    }

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

    abstract public ArrayList<String> getForwardPaths();

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    abstract public String getInternalName();

    abstract public String getSubsystem();

    abstract public boolean isCachedLastTask();

    abstract public boolean isDualRepositories();

    public boolean isIncludeInToolsList()
    {
        return includeInToolsList;
    }

    public boolean isOriginPathsAllowed()
    {
        return originPathsAllowed;
    }

    abstract public boolean isRealOnly();

    public boolean isRemote()
    {
        return isRemote;
    }

    public boolean isRequestStop()
    {
        return stop;
    }

    /**
     * Process an Operations tool
     *
     * @param context The runtime Context
     * @param publisherPath Repository of the publisher or null
     * @param subscriberPath Repository of the subscriber or null
     * @param dryRun Boolean dryrun
     * @throws Exception
     */
    public abstract void processTool(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception;

    /**
     * Process the tool
     *
     * @param context The runtime Context
     * @param publisherRepo Repository of the publisher or null
     * @param subscriberRepo Repository of the subscriber or null
     * @param origins ArrayList<Origin> of starting locations
     * @param dryRun Boolean dryrun
     * @throws Exception
     */
    public abstract void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception;

    /**
     * Process an Operations tool on a SwingWorker thread
     *
     * @param context The runtime Context
     * @param publisherPath Repository of the publisher or null
     * @param subscriberPath Repository of the subscriber or null
     * @param dryRun Boolean dryrun
     * @return SwingWorker<Void, Void> of thread
     */
    public abstract SwingWorker<Void, Void> processToolThread(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception;

    /**
     * Process the tool on a SwingWorker thread
     *
     * @param context The runtime Context
     * @param publisherRepo Repository of the publisher or null
     * @param subscriberRepo Repository of the subscriber or null
     * @param origins ArrayList<Origin> of starting locations
     * @param dryRun Boolean dryrun
     * @return SwingWorker<Void, Void> of thread
     */
    public abstract SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun);

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

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    abstract public void setForwardPaths(ArrayList<String> forwardPaths);

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
