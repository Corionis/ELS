package com.corionis.els.jobs;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Environment;
import com.corionis.els.Utils;
import com.corionis.els.repository.RepoMeta;
import com.corionis.els.repository.Repositories;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.stty.ClientStty;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Task implements Comparable, Serializable
{
    public static final String ANY_SERVER = "_ANY_SERVER_";
    public static final String CACHEDLASTTASK = "_CACHEDLASTTASK_";
    public String configName = null; // name of tool configuration
    public String hintsKey = "";
    public boolean hintsOverrideHost = false;
    public String hintsPath = "";
    public boolean hintsRemote = false;
    public String internalName = null; // internal name of tool
    public ArrayList<Origin> origins; // last serializable member
    public String publisherKey = "";
    public String publisherPath = "";
    public String subscriberKey = "";
    public String subscriberOverride = "";
    public String subscriberPath = "";
    public boolean subscriberRemote = false;

    transient public Context localContext = null;
    transient public AbstractTool currentTool = null;
    transient public boolean dryRun = false; // set before calling process(task)
    transient public Environment environment = null;
    transient public Task previousTask = null;
    transient public Repository publisherRepo = null;
    transient public String remoteType = null;
    transient public Repository subscriberRepo = null;
    transient private Logger logger = LogManager.getLogger("applog");

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

        task.setContext(this.localContext);
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

    private Repository getRepo(Context context, String key, String path, boolean forPublisher) throws Exception
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

                    RepoMeta repoMeta;
                    if (path.length() > 0)
                    {
                        repoMeta = repositories.findMetaPath(path);
                        if (repoMeta != null)
                        {
                            repo = new Repository(context, forPublisher ? Repository.PUBLISHER : Repository.SUBSCRIBER);
                            repo.read(repoMeta.path, (forPublisher ? "Publisher" : "Subscriber"), false);
                        }
                        else
                            throw new MungeException(path + ", " + key + context.cfg.gs("Z.not.found"));
                    }
                }
            }
        }

        context.cfg.setPublisherCollectionFilename("");
        context.cfg.setSubscriberCollectionFilename("");

        if (repo != null)
        {
            if (forPublisher)
            {
                context.cfg.setPublisherLibrariesFileName(repo.getJsonFilename());
                context.publisherRepo = repo;
            }
            else
            {
                context.cfg.setSubscriberLibrariesFileName(repo.getJsonFilename());
                context.subscriberRepo = repo;
            }
        }
        else // no repo
        {
            if (forPublisher)
            {
                context.cfg.setPublisherLibrariesFileName("");
                context.publisherRepo = null;
            }
            else
            {
                context.cfg.setSubscriberLibrariesFileName("");
                context.subscriberRepo = null;
            }
        }

        return repo;
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

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

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
        // TODO handle this BEFORE COMMIT
        this.environment = new Environment(context);
        this.localContext = this.environment.getContext(); // use cloned Context
        this.dryRun = dryRun;

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
                setHintsKey(previousTask.getHintsKey());
                setHintsOverrideHost(previousTask.isHintsOverrideHost());
                setHintsRemote(previousTask.isHintsRemote());
                setPublisherKey(previousTask.getPublisherKey());
                setSubscriberKey(previousTask.getSubscriberKey());
                setHintsOverrideHost(previousTask.hintsOverrideHost);
                setSubscriberOverride(previousTask.subscriberOverride);
                setSubscriberRemote(previousTask.subscriberRemote);
                setOrigins(previousTask.getOrigins());
                hintsPath = previousTask.hintsPath;
                if (previousTask.publisherRepo != null)
                    publisherPath = previousTask.publisherRepo.getJsonFilename();
                else
                    publisherPath = "";
                publisherRepo = previousTask.publisherRepo;
                remoteType = previousTask.remoteType;
                if (previousTask.subscriberRepo != null)
                    subscriberPath = previousTask.subscriberRepo.getJsonFilename();
                else
                    subscriberPath = "";
                subscriberRepo = previousTask.subscriberRepo;
            }
            else
            {
                publisherRepo = getRepo(localContext, getPublisherKey(), getPublisherPath(), true);
                if (publisherRepo != null)
                    publisherPath = publisherRepo.getJsonFilename();
                else if (getPublisherKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Publisher but no Publisher specified");

                remoteType = (isSubscriberRemote() || (getSubscriberKey().equals(Task.ANY_SERVER) && localContext.cfg.isRemoteOperation())) ? "P" : "-";

                subscriberRepo = getRepo(localContext, getSubscriberKey(), getSubscriberPath(), false);
                if (subscriberRepo != null)
                    subscriberPath = subscriberRepo.getJsonFilename();
                else if (getSubscriberKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Subscriber but no Subscriber specified");

                if (!(currentTool instanceof OperationsTool))
                    this.environment.switchConnections();
            }

            localContext.cfg.setPublisherLibrariesFileName(publisherPath);
            localContext.cfg.setPublisherCollectionFilename("");

            localContext.cfg.setOperation(remoteType);

            localContext.cfg.setSubscriberLibrariesFileName(subscriberPath);
            localContext.cfg.setSubscriberCollectionFilename("");
            setSubscriberRemote(subscriberRemote);
            localContext.cfg.setOverrideSubscriberHost(getSubscriberOverride());

            localContext.cfg.setHintsDaemonFilename("");
            localContext.cfg.setHintTrackerFilename("");
            localContext.cfg.setOverrideHintsHost(isHintsOverrideHost());
            if (isHintsRemote())
                localContext.cfg.setHintsDaemonFilename(hintsPath);
            else
                localContext.cfg.setHintTrackerFilename(hintsPath);

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
