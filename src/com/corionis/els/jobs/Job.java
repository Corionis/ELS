package com.corionis.els.jobs;

import com.corionis.els.*;
import com.corionis.els.gui.util.ArgumentTokenizer;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import com.corionis.els.tools.sleep.SleepTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Job extends AbstractTool
{
    public static String INTERNAL_NAME = "jobs"; // must be lowercase

    private String configName; // user-defined name for this instance
    private ArrayList<Task> tasks;

    transient Context context;
    transient Task currentTask = null;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private Task previousTask = null;
    transient private boolean stop = false;

    public Job(Context context, String name)
    {
        super(context);
        this.context = context;
        this.configName = name;
        this.tasks = new ArrayList<Task>();
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

    public String generateCommandline(boolean isDryRun)
    {
        // generate-commandline
        boolean glo = context.preferences != null ? context.preferences.isGenerateLongOptions() : false;
        String conf = (glo ? "--config \"" : "-C \"") + context.cfg.getWorkingDirectory() + "\"" + (context.cfg.isLoggerView() ? " --logger" : "");

        // add defined publisher and subscriber for ANY SERVER if used
        boolean setPub = false;
        boolean setSub = false;
        for (Task task : getTasks())
        {
            if (task.getPublisherKey().equals(Task.ANY_SERVER) && !setPub && context.publisherRepo != null)
            {
                conf += " -p \"" + context.publisherRepo.getJsonFilename() + "\"";
                setPub = true;
            }
            if (task.getSubscriberKey().equals(Task.ANY_SERVER) && !setSub && context.subscriberRepo != null)
            {
                conf += " -s \"" + context.subscriberRepo.getJsonFilename() + "\"";
                setSub = true;
            }
        }

        String cmd = conf + (isDryRun ? (glo ? " --dry-run" : " -D") : "") + (glo ? " --job \"" : " -j \"") + getConfigName() + "\"";
        return cmd;
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
    public boolean isToolCachedOrigins()
    {
        return true;
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

    @Override
    public void processTool(Task task) throws Exception
    {
        // to satisfy AbstractTool, not used
        logger.error("SHOULD NOT BE HERE");
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
            File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, true);
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
     * Used by the Jobs Run button and Jobs menu
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
        // check for other blocking processes
        if (context.navigator.isBlockingProcessRunning())
        {
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            return null;
        }

        // the SwingWorker is here instead of JobsUI so it may be used by the Jobs menu items
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                try
                {
                    String cmd = generateCommandline(isDryRun);
                    List<String> list = ArgumentTokenizer.tokenize(cmd);
                    String[] args = list.toArray(new String[0]);

                    logger.info(context.cfg.gs("Z.launching.job") + getConfigName());
                    Context localContext = (Context) context.clone();
                    Main main = new Main(args, localContext, getConfigName());
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

            @Override
            protected void done()
            {
                // everything should be ok at this point
            }
        };
        return worker;
    }

    private void processJob(Context context, Job job, boolean isDryRun) throws Exception
    {
        stop = false;

        if (job.getTasks() != null && job.getTasks().size() > 0)
        {
            logger.info(context.cfg.gs("Job.executing.job") + job.getConfigName() + ((isDryRun) ? context.cfg.gs("Z.dry.run") : ""));

            for (Task task : job.getTasks())
            {
                if (isRequestStop())
                    break;

                task.setContext(context);
                currentTask = task.clone();
                currentTask.setDryRun(isDryRun);

                // is the task a Job?
                if (currentTask.isJob())
                {
                    Job subJob = load(currentTask.getConfigName());

                    // run it
                    subJob.processJob(context, subJob, isDryRun);
                    if (subJob.previousTask != null)
                        previousTask = subJob.previousTask;

                    logger.info(context.cfg.gs("Job.job.complete") + currentTask.getConfigName() + ((isDryRun) ? context.cfg.gs("Z.dry.run") : ""));
                }
                else // regular task
                {
                    // publisher key (only) is used to indicate cached last task
                    if (previousTask != null && currentTask.isToolCachedOrigins(context) && currentTask.getPublisherKey().equalsIgnoreCase(Task.CACHEDLASTTASK))
                        currentTask.setPreviousTask(previousTask);

                    // run it
                    // the Operations Tool runs a new Main
                    if (!currentTask.process(context))
                        requestStop();

                    if (currentTask.isToolCachedOrigins(context))
                        previousTask = currentTask;
                }

                if (context.fault)
                    break;
            }

            if (context.mainFrame != null)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                context.mainFrame.labelStatusMiddle.setText(null);
            }
        }
        else
            throw new MungeException(context.cfg.gs("JobsUI.job.has.no.tasks") + job.getConfigName());
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

    public void setTasks(ArrayList<Task> tasks)
    {
        this.tasks = tasks;
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
                task.setContext(context);

                // if not a Job or using cached task
                if (!task.getInternalName().equals(Job.INTERNAL_NAME) &&
                        !task.getInternalName().equals(SleepTool.INTERNAL_NAME) &&
                        !task.getPublisherKey().equals(Task.CACHEDLASTTASK))
                {
                    boolean skip = false;
                    // if not certain Operations
                    if (task.getInternalName().equals(OperationsTool.INTERNAL_NAME))
                    {
                        try
                        {
                            OperationsTool tool = (OperationsTool) task.getTool();
                            if (tool.getOperation().equals(OperationsTool.Operations.StatusServer))
                                skip = true;
                        }
                        catch (Exception e)
                        {
                            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                            logger.error(msg);
                            JOptionPane.showMessageDialog(context.mainFrame, msg,
                                    context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (!skip)
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
        }
        else
        {
            status = cfg.gs("JobsUI.job.has.no.tasks") + job.getConfigName();
        }
        return status;
    }

}
