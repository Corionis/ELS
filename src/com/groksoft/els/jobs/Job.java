package com.groksoft.els.jobs;

import com.google.gson.Gson;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Job implements Serializable
{
    private String configName; // user name for this instance
    private ArrayList<Task> tasks;

    transient private boolean dataHasChanged = false;
    transient private boolean initialized = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private boolean stop = false;

    private Job()
    {
        // hide default constructor
    }

    public Job(String name)
    {
        this.configName = name;
        this.tasks = new ArrayList<Task>();
        this.dataHasChanged = true;
    }

    public Job clone()
    {
        Job job = new Job(this.getConfigName());
        ArrayList<Task> tasks = new ArrayList<Task>();
        for (Task task : this.getTasks())
        {
            tasks.add(task.clone());
        }
        job.setTasks(tasks);
        job.initialized = true;
        return job;
    }

    public String getConfigName()
    {
        return configName;
    }

    public static String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                "jobs";
        return path;
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    public ArrayList<Task> getTasks()
    {
        return tasks;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    public boolean isRequestStop()
    {
        return stop;
    }

    public static Job load(String jobName) throws Exception
    {
        Job job = null;
        String path = getDirectoryPath();

        try
        {
            File jobDir = new File(path);
            if (jobDir.exists() && jobDir.isDirectory())
            {
                String json;
                Gson gson = new Gson();
                File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, false);
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        json = new String(Files.readAllBytes(Paths.get(entry.getPath())));
                        Job tmpJob = gson.fromJson(json, Job.class);
                        if (tmpJob.getConfigName().equalsIgnoreCase(jobName))
                        {
//                            logger.info("Read Job file: " + entry.getName());
                            job = tmpJob;
                            job.initialized = true;
                            break;
                        }
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            throw new MungeException("Exception while reading job " + path + " trace: " + Utils.getStackTrace(ioe));
        }
        return job;
    }

    public int process(GuiContext guiContext) throws Exception
    {
        return process(guiContext.cfg, guiContext.context);
    }

    public int process(GuiContext guiContext, Job job) throws Exception
    {
        return process(guiContext.cfg, guiContext.context, this.configName);
    }

    public int process(Configuration cfg, Context context) throws Exception
    {
        return process(cfg, context, this);
    }

    public int process(Configuration cfg, Context context, String jobName) throws Exception
    {
        Job job;
        if (!this.getConfigName().equals(jobName) || !this.initialized)
            job = Job.load(jobName);
        else
            job = this;

        return process(cfg, context, job);
    }

    public int process(Configuration cfg, Context context, Job job) throws Exception
    {
        int result = 0;
        if (job.initialized)
        {
            logger.info("Executing job: " + job.getConfigName());
            for (Task task : job.getTasks())
            {
                if (isRequestStop())
                    break;
                task.process(cfg, context, cfg.isDryRun());
            }
        }
        else
            throw new MungeException("Attempt to process uninitialized job");

        return result;
    }

    public void requestStop()
    {
        this.stop = true;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    public void setTasks(ArrayList<Task> tasks)
    {
        this.tasks = tasks;
    }

    @Override
    public String toString()
    {
        return configName;
    }

}
