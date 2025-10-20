package com.corionis.els.tools.sleep;

import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class SleepTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Sleep";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user-specified name for this instance
    private String internalName = INTERNAL_NAME;
    private int sleep = 0;

    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
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
    }

    public SleepTool clone()
    {
        assert context != null;
        SleepTool tool = new SleepTool(context);
        tool.setConfigName(getConfigName());
        tool.setDisplayName(getDisplayName());
        tool.setDataHasChanged();
        tool.isDryRun = this.isDryRun;
        tool.setRemote(this.isRemote());
        tool.setSleepTime(this.getSleepTime());
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
    public void processTool(Task task) throws Exception
    {
        logger.info(context.cfg.gs("Z.launching") + getDisplayName() + ", " + getConfigName());
        logger.info("+------------------------------------------");
        logger.info(java.text.MessageFormat.format(context.cfg.gs("Sleep.sleeping.minutes"), sleep));
        Thread.sleep(sleep * 1000 * 60);
        logger.info(getConfigName() + context.cfg.gs("Z.completed"));
    }

    public boolean isDataChanged()
    {
        return dataHasChanged;
    }

    @Override
    public boolean isToolPublisher()
    {
        return false;
    }

    @Override
    public boolean isToolSubscriber()
    {
        return false;
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
