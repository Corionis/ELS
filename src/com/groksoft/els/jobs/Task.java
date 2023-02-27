package com.groksoft.els.jobs;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.repository.Repositories;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.operations.OperationsTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Task implements Comparable, Serializable
{
    private transient Logger logger = LogManager.getLogger("applog");
    public static final String ANY_SERVER = "_ANY_SERVER_";
    public static final String CACHEDLASTTASK = "_CACHEDLASTTASK_";

    private String configName; // name of tool configuration
    private String internalName; // internal name of tool
    private String publisherKey = "";
    private String subscriberKey = "";
    private boolean subscriberRemote = false;
    private ArrayList<Origin> origins;

    transient private Context context = null;
    transient private AbstractTool currentTool = null;
    transient private boolean dual = true;
    transient private Task lastTask = null;
    transient private Repository pubRepo;
    transient private boolean realOnly = false;
    transient private Repository subRepo;
    transient private Tools tools = null;
    transient private String type;

    public Task(String internalName, String configName)
    {
        this.internalName = internalName;
        this.configName = configName;
        this.origins = new ArrayList<Origin>();
    }

    public void addOrigins(ArrayList<Origin> origins)
    {
        this.origins.addAll(origins);
    }

    public Task clone()
    {
        Task task = new Task(this.getInternalName(), this.getConfigName());
        task.setPublisherKey(this.getPublisherKey());
        task.setSubscriberKey(this.getSubscriberKey());
        task.setDual(this.isDual());
        task.setRealOnly(this.isRealOnly());
        task.setSubscriberRemote(this.subscriberRemote);
        ArrayList<Origin> origins = new ArrayList<Origin>();
        for (Origin origin : this.getOrigins())
        {
            origins.add(origin.clone());
        }
        task.setOrigins(origins);
        return task;
    }

    @Override
    public int compareTo(Object o)
    {
        return this.getConfigName().compareTo(((Task) o).getConfigName());
    }

    public boolean connectRemote(Context context, Repository publisherRepo, Repository subscriberRepo) throws Exception
    {
        boolean didDisconnect = false;

        // connect to the hint status server if defined
        context.main.connectHintServer(context.publisherRepo);

        // already connected
        if (context.clientStty != null && context.clientStty.isConnected() && context.clientStty.getTheirKey().equals(subscriberKey))
            return true;

        // close any existing STTY connection
        if (context.clientStty != null && context.clientStty.isConnected())
        {
            try
            {
                didDisconnect = true;
                if (!context.timeout)
                {
                    context.clientStty.send("bye", "Disconnect previous session");
                    Thread.sleep(500);
                    context.clientStty.disconnect();
                    Thread.sleep(500);
                }
            }
            catch (Exception e)
            {
            }
        }

        // start the serveStty client for automation
        context.clientStty = new ClientStty(context, false, true);
        if (!context.clientStty.connect(publisherRepo, subscriberRepo))
        {
            context.cfg.setRemoteType("-");
            if (context.navigator != null)
            {
                JOptionPane.showMessageDialog(context.mainFrame,
                        context.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect") +
                                subscriberRepo.getLibraryData().libraries.host,
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        // check for opening commands from Subscriber
        // *** might change cfg options for subscriber and targets that are handled below ***
        if (context.clientStty.checkBannerCommands())
        {
            logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
        }

        // close any existing SFTP connections
        if (didDisconnect)
        {
            context.clientSftp.stopClient();
        }

        // start the serveSftp client
        context.clientSftp = new ClientSftp(context, publisherRepo, subscriberRepo, true);
        if (!context.clientSftp.startClient())
        {
            context.cfg.setRemoteType("-");
            if (context.navigator != null)
            {
                JOptionPane.showMessageDialog(context.mainFrame,
                        context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        return true;
    }

    public String getConfigName()
    {
        return configName;
    }

    public String getInternalName()
    {
        return internalName;
    }

    public ArrayList<Origin> getOrigins()
    {
        return origins;
    }

    public String getPublisherKey()
    {
        return publisherKey;
    }

    private Repository getRepo(Context context, String key, boolean forPublisher) throws Exception
    {
        Repository repo = null;

        if (key != null && key.length() > 0)
        {
            // any server
            if (key.equals(ANY_SERVER))
            {
                if (forPublisher)
                    repo = context.publisherRepo;
                else
                    repo = context.subscriberRepo;
            }
            else
            {
                // already-loaded
                if (forPublisher && context.publisherRepo != null && context.publisherRepo.getLibraryData().libraries.key.equals(key))
                    repo = context.publisherRepo;
                else if (context.subscriberRepo != null && context.subscriberRepo.getLibraryData().libraries.key.equals(key))
                    repo = context.subscriberRepo;
                else
                {
                    // load it
                    Repositories repositories = new Repositories();
                    repositories.loadList(context);

                    Repositories.Meta meta;
                    if (key.length() > 0)
                    {
                        meta = repositories.find(key);
                        if (meta != null)
                        {
                            repo = new Repository(context, forPublisher ? Repository.PUBLISHER : Repository.SUBSCRIBER);
                            repo.read(meta.path, true);
                        }
                        else
                            throw new MungeException(key + context.cfg.gs("Z.not.found"));
                    }
                }
            }
        }

        if (repo != null)
        {
            context.cfg.setPublisherCollectionFilename("");
            context.cfg.setSubscriberCollectionFilename("");
            if (forPublisher)
                context.cfg.setPublisherLibrariesFileName(repo.getJsonFilename());
            else
                context.cfg.setSubscriberLibrariesFileName(repo.getJsonFilename());
        }

        return repo;
    }

    private String getRepoPath(String key) throws Exception
    {
        String path = null;
        Repositories repositories = new Repositories();
        repositories.loadList(context);

        Repositories.Meta meta;
        if (key.length() > 0)
        {
            meta = repositories.find(key);
            if (meta != null)
            {
                path = meta.path;
            }
            else
                throw new MungeException(key + context.cfg.gs("Z.not.found"));
        }
        return path;
    }

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

    public AbstractTool getTool()
    {
        return currentTool;
    }

    public boolean isCachedLastTask(Context context)
    {
        AbstractTool tool;
        if (currentTool == null)
        {
            if (tools == null)
                this.tools = new Tools();
            tool = tools.makeTempTool(getInternalName(), context);
        }
        else
            tool = currentTool;
        return tool.isCachedLastTask();
    }

    public boolean isDual()
    {
        return dual;
    }

    public boolean isJob()
    {
        if (getInternalName().equalsIgnoreCase(Job.INTERNAL_NAME) && getOrigins().size() == 0)
            return true;
        return false;
    }

    public boolean isOriginPathsAllowed(Context context)
    {
        AbstractTool tool;
        if (currentTool == null)
        {
            if (tools == null)
                this.tools = new Tools();
            tool = tools.makeTempTool(getInternalName(), context);
        }
        else
            tool = currentTool;
        return tool.isOriginPathsAllowed();
    }

    public boolean isSubscriberRemote()
    {
        return subscriberRemote;
    }

    public boolean isRealOnly()
    {
        return realOnly;
    }

    /**
     * Process the task
     * <br/>
     * Used by a Job
     *
     * @param context   The Context
     * @param dryRun Boolean for a dry-run
     * @throws Exception
     */
    public boolean process(Context context, boolean dryRun) throws Exception
    {
        String pubPath = null;
        String subPath = null;

        this.context = context;
        if (tools == null)
            this.tools = new Tools();

        if (logger == null)
            logger = LogManager.getLogger("applog");

        currentTool = tools.loadTool(context, getInternalName(), getConfigName());
        if (currentTool != null)
        {
            if ((origins == null || origins.size() == 0) && !useCachedLastTask(context) && currentTool.isOriginPathsAllowed())
            {
                if (context.navigator != null)
                {
                    String msg = context.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                    logger.info(msg);
                    JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                    logger.info(context.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName());
                return false;
            }

            // get repositories or paths
            if (useCachedLastTask(context)) // not an Operations tool
            {
                pubRepo = lastTask.pubRepo;
                subRepo = lastTask.subRepo;
                setSubscriberRemote(lastTask.subscriberRemote);
            }
            else
            {
                if (currentTool instanceof OperationsTool)
                {
                    pubPath = getRepoPath(getPublisherKey());
                    subPath = getRepoPath(getSubscriberKey());
                    pubRepo = null;
                    subRepo = null;
                    context.nestedProcesses = true;
                }
                else // other tools
                {
                    pubRepo = getRepo(context, getPublisherKey(), true);
                    subRepo = getRepo(context, getSubscriberKey(), false);
                    type = (isSubscriberRemote() || getSubscriberKey().equals(Task.ANY_SERVER)) ? "P" : "-";

                    context.cfg.setRemoteType(type);
                    context.cfg.setPublishOperation(false);  // TODO Change when OperationsUI tool added

                    if (isSubscriberRemote())
                    {
                        Repository me = pubRepo;
                        if (me == null)
                            me = context.publisherRepo;
                        if (!connectRemote(context, me, subRepo))
                            return false;
                    }
                }
            }

            // sanity check then run it
            if (currentTool instanceof OperationsTool)
            {
                if (pubPath == null && subPath == null)
                    throw new MungeException(context.cfg.gs("Task.no.repository.is.defined"));

                currentTool.processTool(context, pubPath, subPath, dryRun);
            }
            else
            {
                if (pubRepo == null && subRepo == null)
                    throw new MungeException(context.cfg.gs("Task.no.repository.is.loaded"));

                currentTool.processTool(context, pubRepo, subRepo, origins, dryRun, useCachedLastTask(context) ? lastTask : null);
            }
        }
        else
            throw new MungeException(context.cfg.gs("Task.tool.not.found") + getInternalName() + ":" + getConfigName());

        return true;
    }

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param context The Context
     * @param dryRun     Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> process(Context context, AbstractTool tool, boolean dryRun) throws Exception
    {
        String pubPath = null;
        String subPath = null;
        SwingWorker<Void, Void> worker = null;

        this.context = context;
        currentTool = tool;

        if (logger == null)
            logger = LogManager.getLogger("applog");

        if (currentTool != null)
        {
            if ((origins == null || origins.size() == 0) && !useCachedLastTask(context) && currentTool.isOriginPathsAllowed())
            {
                if (context.navigator != null)
                {
                    String msg = context.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                    logger.info(msg);
                    JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                    logger.info(context.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName());
                return null;
            }

            // get repositories or paths
            if (useCachedLastTask(context)) // not an Operations tool
            {
                pubRepo = lastTask.pubRepo;
                subRepo = lastTask.subRepo;
                setSubscriberRemote(lastTask.subscriberRemote);
            }
            else
            {
                if (currentTool instanceof OperationsTool)
                {
                    pubPath = getRepoPath(getPublisherKey());
                    subPath = getRepoPath(getSubscriberKey());
                    pubRepo = null;
                    subRepo = null;
                    context.nestedProcesses = true;
                }
                else // other tools
                {
                    pubRepo = getRepo(context, getPublisherKey(), true);
                    subRepo = getRepo(context, getSubscriberKey(), false);
                }
            }

            // sanity check then run it
            if (currentTool instanceof OperationsTool)
            {
                if (pubPath == null && subPath == null)
                    throw new MungeException(context.cfg.gs("Task.no.repository.is.defined"));

                worker = currentTool.processToolThread(context, pubPath, subPath, dryRun);
            }
            else
            {
                if (pubRepo == null && subRepo == null)
                    throw new MungeException((context.cfg.gs("Task.no.repository.is.loaded")));

                worker = currentTool.processToolThread(context, pubRepo, subRepo, origins, dryRun);
            }

            return worker;
        }
        return null;
    }

    public void requestStop()
    {
        if (currentTool != null)
        {
            currentTool.requestStop();
        }
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setDual(boolean dual)
    {
        this.dual = dual;
    }

    public void setInternalName(String internalName)
    {
        this.internalName = internalName;
    }

    public void setOrigins(ArrayList<Origin> origins)
    {
        this.origins = origins;
    }

    public void setLastTask(Task lastTask)
    {
        this.lastTask = lastTask;
    }

    public void setPublisherKey(String publisherKey)
    {
        this.publisherKey = publisherKey;
    }

    public void setRealOnly(boolean realOnly)
    {
        this.realOnly = realOnly;
    }

    public void setSubscriberKey(String subscriberKey)
    {
        this.subscriberKey = subscriberKey;
    }

    public void setSubscriberRemote(boolean isSubscriberRemote)
    {
        this.subscriberRemote = isSubscriberRemote;
    }

    public boolean useCachedLastTask(Context context)
    {
        if (isCachedLastTask(context) && lastTask != null && publisherKey.equalsIgnoreCase(CACHEDLASTTASK))
            return true;
        return false;
    }

}
