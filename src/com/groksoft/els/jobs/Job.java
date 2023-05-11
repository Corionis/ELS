package com.groksoft.els.jobs;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
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
    public static String INTERNAL_NAME = "jobs"; // must be lowercase

    private String configName; // user-defined name for this instance
    private ArrayList<Task> tasks;

    transient Context context;
    transient Task currentTask = null;
    transient private boolean dataHasChanged = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private Task previousTask = null;
    transient private final boolean realOnly = false;
    transient private boolean stop = false;

    public Job(Context context, String name)
    {
        super(context);
        this.context = context;
        this.configName = name;
        this.tasks = new ArrayList<Task>();
        this.dataHasChanged = false;
    }

    public Job clone()
    {
        Job job = new Job(context, this.getConfigName());
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
        return context.cfg.gs("jobs.displayName");
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

    public boolean isCachedLastTask()
    {
        return true;
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
    public void processTool(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    @Override
    public void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task previousTask) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
        return null;
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
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
                    return new Job(context, "");
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
                        if (tmpJob.getConfigName().equals(jobName))
                        {
                            job = tmpJob;
                            break;
                        }
                    }
                }
            }
            if (job == null)
                throw new MungeException(jobName + context.cfg.gs("Z.not.found"));
        }
        return job;
    }

    /**
     * Process Job
     * <br/>
     * Used by Main() with the -j | --job command line option
     *
     * @param context
     * @throws Exception
     */
    public void process(Context context) throws Exception
    {
        processJob(context, this, context.cfg.isDryRun());
    }

    /**
     * Process Job on a SwingWorker thread
     * <br/>
     * Used by the Jobs GUI run button and Navigator Jobs menu
     *
     * @param context The Context
     * @param comp       The owning component
     * @param title      The title for any dialogs
     * @param job        The Job to run
     * @param isDryRun   True for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> process(Context context, Component comp, String title, Job job, boolean isDryRun)
    {
        // create a fresh dialog
        // TODO factors controlling whether to display the progress dialog may need adjusting
/*
        if (context.progress == null || !context.progress.isBeingUsed())
        {
            ActionListener cancel = new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    requestStop();
                }
            };
            context.progress = new Progress(context, comp, cancel, isDryRun);
            context.progress.display();
        }
        else
*/

        if (context.progress != null && context.progress.isBeingUsed())
        {
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (willDisconnect(context))
        {
            int reply = JOptionPane.showConfirmDialog(comp, context.cfg.gs("Job.this.job.contains.remote.subscriber"), title, JOptionPane.YES_NO_OPTION);
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
                    processJob(context, job, isDryRun);
                }
                catch (Exception e)
                {
                    String msg = context.cfg.gs("Z.exception") + e.getMessage() + "; " + Utils.getStackTrace(e);
                    logger.error(msg);
                    JOptionPane.showMessageDialog(context.mainFrame, msg,
                            context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);

                }
                return null;
            }
        };
        return worker;
    }

    private void processJob(Context context, Job job, boolean isDryRun) throws Exception
    {
        int result = 0;
        stop = false;

        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            logger.info(context.cfg.gs("Job.executing.job") + job.getConfigName() + ((isDryRun) ? context.cfg.gs("Z.dry.run") : ""));
            for (Task task : job.getTasks())
            {
                if (isRequestStop())
                    break;

                currentTask = task;

                // is the task a Job?
                if (currentTask.isJob())
                {
                    Job subJob = load(currentTask.getConfigName());

                    // run it
                    subJob.processJob(context, subJob, isDryRun);
                    if (subJob.previousTask != null)
                        previousTask = subJob.previousTask;

                    logger.info(context.cfg.gs("Job.continuing.job") + job.getConfigName() + ((isDryRun) ? context.cfg.gs("Z.dry.run") : ""));
                }
                else // regular task
                {
                    // publisher key (only) is used to indicate cached last task
                    if (previousTask != null && currentTask.isCachedLastTask(context) && currentTask.getPublisherKey().equalsIgnoreCase(Task.CACHEDLASTTASK))
                        currentTask.setPreviousTask(previousTask);

                    // run it
                    if (!currentTask.process(context, isDryRun))
                        requestStop();

                    if (currentTask.isCachedLastTask(context))
                        previousTask = currentTask;
                }
            }

            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            context.savedEnvironment.restore(currentTask);

            logger.info(context.cfg.gs("Job.completed.job") +
                    job.getConfigName()+ ((isDryRun) ? context.cfg.gs("Z.dry.run") : "") +
                    context.cfg.gs("Z.completed"));
        }
        else
            throw new MungeException(context.cfg.gs("JobsUI.job.has.no.tasks") + ": " + job.getConfigName());
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

    public boolean usesPublisher()
    {
        for (Task task : tasks)
        {
            if (task.getPublisherKey() != null && task.getPublisherKey().length() > 0 && !task.getPublisherKey().equals(Task.CACHEDLASTTASK))
                return true;
        }
        return false;
    }

    public boolean usesSubscriber()
    {
        for (Task task : tasks)
        {
            if (task.getSubscriberKey() != null && task.getSubscriberKey().length() > 0)
                return true;
        }
        return false;
    }

    public String validate(Configuration cfg)
    {
        Job job = this;
        String status = "";
        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            for (Task task : job.getTasks())
            {
                // if not a Job or using cached task
                if (!task.getInternalName().equals(getInternalName()) && !task.getPublisherKey().equals(Task.CACHEDLASTTASK))
                {
                    if (task.getPublisherKey().length() == 0 && task.getSubscriberKey().length() == 0)
                    {
                        status = cfg.gs("JobsUI.task.has.no.publisher.and.or.subscriber") + task.getConfigName();
                        break;
                    }
                    else if ((task.getOrigins() == null || task.getOrigins().size() == 0) && !task.getInternalName().equals("Operations"))
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

    private boolean willDisconnect(Context context)
    {
        if (context.cfg.isRemoteSession())
        {
            for (Task task : getTasks())
            {
                // publisher key (only) is used to indicate cached last task
                if (task.isSubscriberRemote() &&
                        !task.getPublisherKey().equals(Task.ANY_SERVER) &&
                        !task.getPublisherKey().equals(Task.CACHEDLASTTASK) &&
                        !context.subscriberRepo.getLibraryData().libraries.key.equals(task.getSubscriberKey()))
                    return true;
            }
        }
        return false;
    }

}
