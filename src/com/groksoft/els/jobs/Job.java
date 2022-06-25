package com.groksoft.els.jobs;

import com.google.gson.Gson;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Job implements Comparable, Serializable
{
    private String configName; // user name for this instance
    private ArrayList<Task> tasks;

    transient Task currentTask = null;
    transient private boolean dataHasChanged = false;
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
        return job;
    }

    @Override
    public int compareTo(Object o)
    {
        return getConfigName().compareTo(((Job) o).getConfigName());
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

    private boolean willDisconnect(Configuration cfg)
    {
        if (cfg.isRemoteSession())
        {
            for (Task task : getTasks())
            {
                if (task.isSubscriberRemote())
                    return true;
            }
        }
        return false;
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
                        job = tmpJob;
                        break;
                    }
                }
            }
        }
        return job;
    }

    /**
     * Process this Job
     * <br/>
     * Used by Main() with the -j | --job command line option
     *
     * @param cfg
     * @param context
     * @return
     * @throws Exception
     */
    public int process(GuiContext guiContext, Configuration cfg, Context context) throws Exception
    {
        return processJob(guiContext, cfg, context, this, cfg.isDryRun());
    }

    /**
     * Process the job on a SwingWorker thread
     * <br/>
     * Used by the Jobs GUI
     *
     * @param guiContext The GuiContext
     * @param comp       The owning component
     * @param title      The title for any dialogs
     * @param job        The Job to run
     * @param isDryRun   True for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> process(GuiContext guiContext, Component comp, String title, Job job, boolean isDryRun)
    {
        if (willDisconnect(guiContext.cfg))
        {
            int reply = JOptionPane.showConfirmDialog(comp,
                    guiContext.cfg.gs("This job contains remote subscribers. The existing remote subscriber connection will be unavailable while the job is running. Continue?"),
                    title, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                guiContext.browser.closeSubscriberPane();

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
                {
                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        try
                        {
                            processJob(guiContext, guiContext.cfg, guiContext.context, job, isDryRun);
                        }
                        catch (Exception e)
                        {
                            String msg = guiContext.cfg.gs("Z.exception") + e.getMessage() + "; " + Utils.getStackTrace(e);
                            guiContext.browser.printLog(msg, true);
                            JOptionPane.showMessageDialog(guiContext.mainFrame, msg,
                                    guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);

                        }
                        return null;
                    }
                };
                worker.execute();
                return worker;
            }
        }
        return null;
    }

    private int processJob(GuiContext guiContext, Configuration cfg, Context context, Job job, boolean isDryRun) throws Exception
    {
        int result = 0;
        stop = false;
        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            logger.info(guiContext.cfg.gs("Jobs.executing.job") + job.getConfigName());
            for (Task task : job.getTasks())
            {
                if (isRequestStop())
                    break;
                currentTask = task;
                currentTask.process(guiContext, cfg, context, isDryRun);
            }
        }
        else
            throw new MungeException(cfg.gs("JobsUI.job.has.no.tasks") + ": " + job.getConfigName());

        return result;
    }

    public void requestStop()
    {
        stop = true;
        if (currentTask != null)
            currentTask.requestStop();
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

    public String validate(Configuration cfg)
    {
        Job job = this;
        String status = "";
        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            for (Task task : job.getTasks())
            {
                if (task.getPublisherKey().length() == 0 && task.getSubscriberKey().length() == 0)
                {
                    status = cfg.gs("JobsUI.task.has.no.publisher.and.or.subscriber") + task.getConfigName();
                }
                else if (task.getOrigins() == null || task.getOrigins().size() == 0)
                {
                    status = cfg.gs("JobsUI.task.has.no.origins") + task.getConfigName();
                }
            }
        }
        else
        {
            status = cfg.gs("JobsUI.job.has.no.tasks");
        }
        return status;
    }

}
