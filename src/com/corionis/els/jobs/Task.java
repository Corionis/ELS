package com.corionis.els.jobs;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.repository.RepoMeta;
import com.corionis.els.repository.Repositories;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Task implements Comparable, Serializable
{
    // @formatter:off
    public static final String ANY_SERVER = "_ANY_SERVER_";
    public static final String CACHEDLASTTASK = "_CACHEDLASTTASK_";

    public String configName = null; // name of tool configuration
    public String internalName = null; // internal name of tool
    public String hintsKey = "";
    public boolean hintsOverrideHost = false;
    public String hintsPath = "";
    public boolean hintsRemote = false;
    public String publisherKey = "";
    public String publisherPath = "";
    public String subscriberKey = "";
    public String subscriberOverride = "";
    public String subscriberPath = "";
    public boolean subscriberRemote = false;

    public ArrayList<Origin> origins; // last serializable member

    transient public AbstractTool currentTool = null;
    transient public boolean dryRun = false; // set before calling process(task)
    transient public Repository hintsRepo = null;
    transient public Context localContext = null;
    transient public Context originalContext = null;
    transient public Task previousTask = null;
    transient public Repository publisherRepo = null;
    transient public String remoteType = null;
    transient public Repository subscriberRepo = null;
    transient private Logger logger = LogManager.getLogger("applog");
    // @formatter:on

    private Task()
    {
        // hide default constructor
    }
    /**
     * Task used in a Job
     *
     * @param internalName Internal name of the task
     * @param configName   User name for the task
     */
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

    @Override
    public Task clone()
    {
        Task task = new Task(this.getInternalName(), this.getConfigName());
        task.setContext(this.localContext);
        task.setHintsKey(this.getHintsKey());
        task.setHintsOverrideHost(this.isHintsOverrideHost());
        task.setHintsPath(this.getHintsPath());
        task.setHintsRemote(this.isHintsRemote());
        task.setPublisherKey(this.getPublisherKey());
        task.setPublisherPath(this.getPublisherPath());
        task.setSubscriberKey(this.getSubscriberKey());
        task.setSubscriberOverride(this.getSubscriberOverride());
        task.setSubscriberPath(this.getSubscriberPath());
        task.setSubscriberRemote(this.isSubscriberRemote());

        ArrayList<Origin> origins = new ArrayList<Origin>();
        for (Origin origin : this.getOrigins())
        {
            origins.add(origin.clone());
        }
        task.setOrigins(origins);

        task.currentTool = this.currentTool;
        task.dryRun = this.dryRun;
        task.previousTask = this.previousTask;
        task.publisherRepo = this.publisherRepo;
        task.remoteType = this.remoteType;
        task.subscriberRepo = this.subscriberRepo;
        return task;
    }

    @Override
    public int compareTo(Object o)
    {
        return this.getConfigName().compareTo(((Task) o).getConfigName());
    }

/*
    public boolean connectRemote(Context context, Repository publisherRepo, Repository subscriberRepo) throws Exception
    {
        boolean didDisconnect = false;

        // connect to the hint status server if defined
        context.main.setupHints(context.publisherRepo);

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
        context.clientStty = new ClientStty(context, false, true, false);
        if (!context.clientStty.connect(publisherRepo, subscriberRepo))
        {
            context.cfg.setOperation("-");
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
        if (context.clientStty.checkBannerCommands())
        {
            logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
        }

        String directory = context.clientStty.getWorkingDirectoryRemote();
        context.cfg.setWorkingDirectorySubscriber(directory);

        // close any existing SFTP connections
        if (didDisconnect)
        {
            context.clientSftp.stopClient();
        }

        // start the serveSftp transfer client
        context.clientSftp = new ClientSftp(context, publisherRepo, subscriberRepo, true);
        if (!context.clientSftp.startClient("transfer"))
        {
            context.cfg.setOperation("-");
            if (context.navigator != null)
            {
                JOptionPane.showMessageDialog(context.mainFrame,
                        context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        if (context.cfg.isNavigator())
        {
            // start the serveSftp metadata client
            context.clientSftpMetadata = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
            if (!context.clientSftpMetadata.startClient("metadata"))
            {
                if (context.navigator != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }

        return true;
    }
*/

    public String getConfigName()
    {
        return configName;
    }

    public String getHintsKey()
    {
        return hintsKey;
    }

    public String getHintsPath()
    {
        return hintsPath;
    }

    public Repository getHintsRepo()
    {
        return hintsRepo;
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

    public String getPublisherPath()
    {
        return publisherPath;
    }

    private Repository getRepo(String key, String path, int purpose) throws Exception
    {
        Repository repo = null;

        if (key != null && key.length() > 0)
        {
            // currently-loaded values
            if (key.equals(ANY_SERVER))
            {
                if (purpose == Repository.PUBLISHER)
                    repo = localContext.publisherRepo;
                else if (purpose == Repository.SUBSCRIBER)
                {
                    repo = localContext.subscriberRepo;
                    setSubscriberOverride(localContext.cfg.getOverrideSubscriberHost());
                    setSubscriberRemote(localContext.cfg.isRemoteSubscriber());
                }
                else // only used by Run button of Tools, never in a Job
                {
                    repo = localContext.hintsRepo;
                    setHintsOverrideHost(localContext.cfg.isOverrideHintsHost());
                    setHintsRemote(localContext.cfg.isRemoteStatusServer());
                }
            }
            else
            {
                // is already-loaded
                if (purpose == Repository.PUBLISHER && localContext.publisherRepo != null && localContext.publisherRepo.getLibraryData().libraries.key.equals(key))
                    repo = localContext.publisherRepo;
                else if (purpose == Repository.SUBSCRIBER && localContext.subscriberRepo != null && localContext.subscriberRepo.getLibraryData().libraries.key.equals(key))
                    repo = localContext.subscriberRepo;
                else if (purpose == Repository.HINT_SERVER && localContext.hintsRepo != null && localContext.hintsRepo.getLibraryData().libraries.key.equals(key))
                    repo = localContext.hintsRepo;
                else
                {
                    // load it; other parameters come from Job
                    Repositories repositories = new Repositories();
                    repositories.loadList(localContext);

                    RepoMeta repoMeta;
                    if (path.length() > 0)
                    {
                        repoMeta = repositories.findMetaPath(path);
                        if (repoMeta != null)
                        {
                            repo = new Repository(localContext, purpose);
                            repo.read(repoMeta.path, (purpose == Repository.PUBLISHER ? "Publisher" :
                                    (purpose == Repository.SUBSCRIBER ? "Subscriber" : "Hint Status Server")), false);
                        }
                        else
                            throw new MungeException(path + ", " + key + localContext.cfg.gs("Z.not.found"));
                    }
                }
            }
        }

        localContext.cfg.setPublisherCollectionFilename("");
        localContext.cfg.setSubscriberCollectionFilename("");

        if (repo != null)
        {
            switch (purpose)
            {
                case Repository.PUBLISHER:
                {
                    localContext.cfg.setPublisherLibrariesFileName(repo.getJsonFilename());
                    localContext.publisherRepo = repo;
                    break;
                }
                case Repository.SUBSCRIBER:
                {
                    localContext.cfg.setSubscriberLibrariesFileName(repo.getJsonFilename());
                    localContext.subscriberRepo = repo;
                    break;
                }
                case Repository.HINT_SERVER:
                {
                    localContext.cfg.setHintsDaemonFilename("");
                    localContext.cfg.setHintTrackerFilename("");
                    if (isHintsRemote())
                        localContext.cfg.setHintsDaemonFilename(repo.getJsonFilename());
                    else
                        localContext.cfg.setHintTrackerFilename(repo.getJsonFilename());
                    break;
                }
            }
        }

        return repo;
    }

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

/*
    public String getRepoPath(String key)
    {
        String path = "";
        Repositories repositories = new Repositories();
        repositories.loadList(localContext);

        RepoMeta repoMeta = null;
        if (key.length() > 0)
        {
            repoMeta = repositories.findMeta(key);
            if (repoMeta != null)
            {
                path = repoMeta.path;
            }
        }
        return path;
    }
*/

    public String getSubscriberOverride()
    {
        return subscriberOverride;
    }

    public String getSubscriberPath()
    {
        return subscriberPath;
    }

    public AbstractTool getTool() throws Exception
    {
        currentTool = localContext.tools.loadTool(localContext, getInternalName(), getConfigName());
        return currentTool;
    }

    public boolean isHintsOverrideHost()
    {
        return hintsOverrideHost;
    }

    public boolean isHintsRemote()
    {
        return hintsRemote;
    }

    public boolean isJob()
    {
        if (getInternalName().equalsIgnoreCase(Job.INTERNAL_NAME) && getOrigins().size() == 0)
            return true;
        return false;
    }

    public boolean isSubscriberRemote()
    {
        return subscriberRemote;
    }

    public boolean isToolCachedOrigins(Context context)
    {
        AbstractTool tool;
        tool = context.tools.makeTempTool(getInternalName(), context);
        return tool.isToolCachedOrigins();
    }

    /**
     * Process the task
     * <br/>
     * Used by a Job
     *
     * @param context The execution Context
     * @throws Exception
     */
    public boolean process(Context context) throws Exception
    {
        this.localContext = (Context)context.clone();

        if (logger == null)
            logger = LogManager.getLogger("applog");

        currentTool = getTool();
        if (currentTool != null)
        {
            if ((origins == null || origins.size() == 0) && !useCachedLastTask(localContext) && currentTool.isToolOriginsUsed())
            {
                if (localContext.navigator != null)
                {
                    String msg = localContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                    logger.info(msg);
                    JOptionPane.showMessageDialog(localContext.mainFrame, msg, localContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                    logger.info(localContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName());
                return false;
            }

            // get repositories or paths
            if (useCachedLastTask(localContext)) // not an Operations tool
            {
                setPublisherKey(previousTask.getPublisherKey());
                if (previousTask.publisherRepo != null)
                {
                    publisherPath = previousTask.publisherRepo.getJsonFilename();
                    publisherRepo = previousTask.publisherRepo;
                }
                else
                    publisherPath = "";

                remoteType = previousTask.remoteType;

                if (previousTask.subscriberRepo != null)
                {
                    subscriberPath = previousTask.subscriberRepo.getJsonFilename();
                    setSubscriberKey(previousTask.getSubscriberKey());
                    setSubscriberOverride(previousTask.subscriberOverride);
                    setSubscriberRemote(previousTask.subscriberRemote);
                }
                else
                    subscriberPath = "";
                subscriberRepo = previousTask.subscriberRepo;

                setHintsKey(previousTask.getHintsKey());
                setHintsOverrideHost(previousTask.isHintsOverrideHost());
                setHintsRemote(previousTask.isHintsRemote());
                hintsPath = previousTask.hintsPath;

                setOrigins(previousTask.getOrigins());
            }
            else
            {
                publisherRepo = getRepo(getPublisherKey(), getPublisherPath(), Repository.PUBLISHER);
                if (publisherRepo != null)
                    publisherPath = publisherRepo.getJsonFilename();
                else if (getPublisherKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Publisher but none specified");

                subscriberRepo = getRepo(getSubscriberKey(), getSubscriberPath(), Repository.SUBSCRIBER);
                if (subscriberRepo != null)
                    subscriberPath = subscriberRepo.getJsonFilename();
                else if (getSubscriberKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Subscriber but none specified");

                remoteType = (isSubscriberRemote() || (getSubscriberKey().equals(Task.ANY_SERVER) && localContext.cfg.isRemoteOperation())) ? "P" : "-";

                hintsRepo = getRepo(getHintsKey(), getHintsPath(), Repository.HINT_SERVER);
                if (hintsRepo != null)
                    hintsPath = hintsRepo.getJsonFilename();
                else if (getHintsKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Hint Status Server but none specified");
            }

            localContext.cfg.setOperation(remoteType);

            // run it
            currentTool.processTool(this);

            if (currentTool.isRequestStop())
                return false;

            context.fault = localContext.fault;
        }
        else
            throw new MungeException(localContext.cfg.gs("Task.tool.not.found") + getInternalName() + ":" + getConfigName());

        return true;
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

    public void setContext(Context context)
    {
        this.localContext = context;
    }

    public void setDryRun(boolean sense)
    {
        this.dryRun = sense;
    }

    public void setHintsKey(String hintsKey)
    {
        this.hintsKey = hintsKey;
    }

    public void setHintsOverrideHost(boolean hintsOverrideHost)
    {
        this.hintsOverrideHost = hintsOverrideHost;
    }

    public void setHintsPath(String hintsPath)
    {
        this.hintsPath = Utils.makeRelativePath(localContext.cfg.getWorkingDirectory(), hintsPath);
    }

    public void setHintsRemote(boolean isHintsRemote)
    {
        this.hintsRemote = isHintsRemote;
    }

    public void setHintsRepo(Repository hintsRepo)
    {
        this.hintsRepo = hintsRepo;
    }

    public void setInternalName(String internalName)
    {
        this.internalName = internalName;
    }

    public void setOrigins(ArrayList<Origin> origins)
    {
        this.origins = origins;
    }

    public void setPreviousTask(Task previousTask)
    {
        this.previousTask = previousTask;
    }

    public void setPublisherKey(String publisherKey)
    {
        this.publisherKey = publisherKey;
    }

    public void setPublisherPath(String publisherPath)
    {
        this.publisherPath = Utils.makeRelativePath(localContext.cfg.getWorkingDirectory(), publisherPath);
    }

    public void setSubscriberKey(String subscriberKey)
    {
        this.subscriberKey = subscriberKey;
    }

    public void setSubscriberOverride(String subscriberOverride)
    {
        this.subscriberOverride = subscriberOverride;
    }

    public void setSubscriberPath(String subscriberPath)
    {
        this.subscriberPath = Utils.makeRelativePath(localContext.cfg.getWorkingDirectory(), subscriberPath);
    }

    public void setSubscriberRemote(boolean isSubscriberRemote)
    {
        this.subscriberRemote = isSubscriberRemote;
    }

    public boolean useCachedLastTask(Context context)
    {
        // publisher key (only) is used to indicate last task
        if (isToolCachedOrigins(context) && previousTask != null && publisherKey.equalsIgnoreCase(CACHEDLASTTASK))
            return true;
        return false;
    }

}
