package com.groksoft.els.tools.sleep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SleepTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Sleep";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user-specified name for this instance
    private String internalName = INTERNAL_NAME;
    private int sleep = 0;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private boolean dualRepositories = false; // used by GUI, always false for this tool
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private final boolean realOnly = false;
    // @formatter:on

    /**
     * Constructor when used from the command line
     *
     * @param context   Context
     */
    public SleepTool(Context context)
    {
        super(context);
        this.context = context;
        setDisplayName(getCfg().gs("Sleep.displayName"));
        this.dataHasChanged = false;
        this.originPathsAllowed = false;
    }

    public SleepTool clone()
    {
        assert context != null;
        SleepTool tool = new SleepTool(context);
        tool.setConfigName(getConfigName());
        tool.setDisplayName(getDisplayName());
        tool.setDataHasChanged();
        tool.setIncludeInToolsList(this.isIncludeInToolsList());
        tool.isDryRun = this.isDryRun;
        tool.setIsRemote(this.isRemote());
        tool.setSleepTime(this.getSleepTime());
        tool.setIncludeInToolsList(isIncludeInToolsList());
        return tool;
    }

    @Override
    public String getConfigName()
    {
        return configName;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    public int getSleepTime()
    {
        return this.sleep;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    @Override
    public boolean isCachedLastTask()
    {
        return false;
    }

    @Override
    public boolean isDualRepositories()
    {
        return dualRepositories;
    }

    @Override
    public boolean isRealOnly()
    {
        return false;
    }

    @Override
    public void processTool(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    /**
     * Process the tool with the metadata provided
     */
    @Override
    public void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception
    {
        logger.info(getDisplayName() + ", " + getConfigName() + ": " + sleep);
        Thread.sleep(sleep * 1000 * 60);
        logger.info(getConfigName() + context.cfg.gs("Z.completed"));
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
        return null;
    }

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param context     The context
     * @param publisherRepo  Publisher repo, or null
     * @param subscriberRepo Subscriber repo, or null
     * @param origins        List of origins to process
     * @param dryRun         Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        return null;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged;
    }

    public void reset()
    {

    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    public void setDataHasChanged(boolean state)
    {
        dataHasChanged = state;
    }

    public void setSleepTime(int sleep)
    {
        this.sleep = sleep;
    }

    public void write() throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(getCfg().gs("Z.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
