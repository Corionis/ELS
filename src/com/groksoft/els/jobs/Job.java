package com.groksoft.els.jobs;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.Progress;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Job extends AbstractTool implements Comparable, Serializable
{
    public static String INTERNAL_NAME = "jobs";

    private String configName; // user name for this instance
    private ArrayList<Task> tasks;

    transient Configuration cfg;
    transient Context context;
    transient Task currentTask = null;
    transient private boolean dataHasChanged = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private final boolean realOnly = false;
    transient private boolean stop = false;

    public Job(Configuration cfg, Context context, String name)
    {
        super(cfg, context);
        this.cfg = cfg;
        this.context = context;
        this.configName = name;
        this.tasks = new ArrayList<Task>();
        this.dataHasChanged = false;
    }

    public Job clone()
    {
        Job job = new Job(this.cfg, context, this.getConfigName());
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

    public String getDisplayName()
    {
        return cfg.gs("jobs.displayName");
    }

    public String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName()) + ".json";
        return path;
    }

    @Override
    public String getInternalName()
    {
        return INTERNAL_NAME;
    }

    @Override
    public String getSubsystem()
    {
        return ""; // jobs are not a subsystem
    }

    @Override
    public boolean isDualRepositories()
    {
        return false;
    }

    public String getListName()
    {
        return getDisplayName() + ": " + getConfigName();
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

    @Override
    public void processTool(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        // to satisfy AbstractTool, not used
        return null;
    }

    @Override
    public boolean isRealOnly()
    {
        return realOnly;
    }

    public Job load(String jobName) throws Exception
    {
        Job job = null;
        String path = getDirectoryPath();

        if (jobName == null || jobName.length() == 0)
            jobName = getConfigName();

        File jobDir = new File(path);
        if (jobDir.exists() && jobDir.isDirectory())
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new Job(cfg, context, "");
                }
            }
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Job.class, new objInstanceCreator());

            String json;
            File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    json = new String(Files.readAllBytes(Paths.get(entry.getPath())));
                    if (json != null && json.length() > 0)
                    {
                        Job tmpJob = builder.create().fromJson(json, Job.class);
                        if (tmpJob.getConfigName().equalsIgnoreCase(jobName))
                        {
                            job = tmpJob;
                            break;
                        }
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
    public void process(Configuration cfg, Context context) throws Exception
    {
        processJob(null, cfg, context, this, cfg.isDryRun());
    }

    /**
     * Process the job on a SwingWorker thread
     * <br/>
     * Used by the Jobs GUI and Navigator Jobs menu run
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
        // create a fresh dialog
        if (guiContext.progress == null || !guiContext.progress.isBeingUsed())
        {
            ActionListener cancel = new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    requestStop();
                }
            };
            guiContext.progress = new Progress(guiContext, comp, cancel, isDryRun);
        }
        else
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            return null;
        }

        guiContext.progress.display();

        if (willDisconnect(guiContext))
        {
            int reply = JOptionPane.showConfirmDialog(comp, guiContext.cfg.gs("Job.this.job.contains.remote.subscriber"), title, JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return null;
        }

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
        return worker;
    }

    public void processJob(GuiContext guiContext, Configuration cfg, Context context, Job job, boolean isDryRun) throws Exception
    {
        int result = 0;
        stop = false;

        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            if (guiContext != null)
                guiContext.browser.printLog(cfg.gs("Job.executing.job") + job.getConfigName() + ((isDryRun) ? cfg.gs("Z.dry.run") : ""));
            else
                logger.info(cfg.gs("Job.executing.job") + job.getConfigName() + ((isDryRun) ? cfg.gs("Z.dry.run") : ""));

            for (Task task : job.getTasks())
            {
                if (isRequestStop())
                    break;

                currentTask = task;
                if (currentTask.getInternalName().equals(getInternalName()))
                {
                    Job subJob = load(currentTask.getConfigName());
                    subJob.processJob(guiContext, cfg, context, subJob, isDryRun);

                    if (guiContext != null)
                        guiContext.browser.printLog(cfg.gs("Job.continuing.job") + job.getConfigName() + ((isDryRun) ? cfg.gs("Z.dry.run") : ""));
                    else
                        logger.info(cfg.gs("Job.continuing.job") + job.getConfigName() + ((isDryRun) ? cfg.gs("Z.dry.run") : ""));

                }
                else
                {
                    if (!currentTask.process(guiContext, cfg, context, isDryRun))
                        requestStop();
                }
            }

            context.main.savedConfiguration.restore(currentTask);
        }
        else
            throw new MungeException(cfg.gs("JobsUI.job.has.no.tasks") + ": " + job.getConfigName());
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

    public void setDataHasChanged(boolean state)
    {
        dataHasChanged = state;
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
                if (!task.getInternalName().equals(getInternalName())) // if not a Job
                {
                    if (task.getPublisherKey().length() == 0 && task.getSubscriberKey().length() == 0)
                    {
                        status = cfg.gs("JobsUI.task.has.no.publisher.and.or.subscriber") + task.getConfigName();
                        break;
                    }
                    else if (task.getOrigins() == null || task.getOrigins().size() == 0)
                    {
                        status = cfg.gs("JobsUI.task.has.no.origins") + task.getConfigName();
                        break;
                    }
                }
            }
        }
        else
        {
            status = cfg.gs("JobsUI.job.has.no.tasks");
        }
        return status;
    }

    private boolean willDisconnect(GuiContext guiContext)
    {
        if (guiContext.cfg.isRemoteSession())
        {
            for (Task task : getTasks())
            {
                if (task.isSubscriberRemote() && !guiContext.context.subscriberRepo.getLibraryData().libraries.key.equals(task.getSubscriberKey()))
                    return true;
            }
        }
        return false;
    }

}
