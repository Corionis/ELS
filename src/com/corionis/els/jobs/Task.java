package com.corionis.els.jobs;

import com.corionis.els.*;
import com.corionis.els.repository.RepoMeta;
import com.corionis.els.repository.Repositories;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;

public class Task implements Comparable, Serializable
{
    // @formatter:off
    public static final String ANY_SERVER = "_ANY_SERVER_";
    public static final String CACHEDLASTTASK = "_CACHEDLASTTASK_";

    public String configName = null; // name of tool configuration
    public String internalName = null; // internal name of tool
    public String emailTool = "";
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
    transient public Context localContext = null;
    transient public Task previousTask = null;
    transient public String remoteType = null;
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

    /**
     * Task used directly with a tool
     *
     * @param currentTool
     */
    public Task(AbstractTool currentTool)
    {
        this.currentTool = currentTool;
        this.configName = currentTool.getConfigName();
        this.internalName = currentTool.getInternalName();
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
        task.setEmailTool(this.getEmailTool());
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
        task.remoteType = this.remoteType;
        return task;
    }

    @Override
    public int compareTo(Object o)
    {
        return this.getConfigName().compareTo(((Task) o).getConfigName());
    }

    public String getConfigName()
    {
        return configName;
    }

    public String getEmailTool()
    {
        return emailTool;
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

    private Repository getRepo(String key, String path, int purpose) throws Exception
    {
        boolean connect = true;
        boolean disconnect = true;
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

        // connect & login
        if (repo != null)
        {
            repo.setContext(localContext);

            switch (purpose)
            {
                case Repository.PUBLISHER:
                {
                    localContext.cfg.setPublisherLibrariesFileName(repo.getJsonFilename());
                    localContext.publisherRepo = repo;

                    if (isSamePublisher())
                    {
                        localContext.publisherUser = previousTask.localContext.publisherUser;
                        connect = false;
                    }

                    if (connect)
                    {
                        if ((localContext.publisherUser = repo.login()) == null)
                            throw new MungeException(localContext.cfg.gs("Z.publisher.login.failed"));
                    }

                    if (localContext.cfg.isGui() && localContext.mainFrame != null && localContext.mainFrame.isVisible())
                        localContext.navigator.displayConnection();
                    break;
                }
                case Repository.SUBSCRIBER:
                {
                    localContext.cfg.setSubscriberLibrariesFileName(repo.getJsonFilename());
                    localContext.subscriberRepo = repo;

                    // non-Operations task; Operation tasks run a new Main()
                    if (!currentTool.getInternalName().equalsIgnoreCase("Operations"))
                    {
                        // same Subscriber?
                        if (isSameSubscriber())
                        {
                            localContext.subscriberUser = previousTask.localContext.subscriberUser;
                            localContext.clientStty = previousTask.localContext.clientStty;
                            localContext.clientSftp = previousTask.localContext.clientSftp;
                            localContext.clientSftpMetadata =  previousTask.localContext.clientSftpMetadata;
                            if (isSubscriberRemote() && localContext.clientStty != null && localContext.clientStty.isConnected())
                            {
                                connect = false;
                                disconnect = false;
                            }
                        }

                        // disconnect needed?
                        if (disconnect)
                        {
                            if (previousTask != null && previousTask.localContext.clientStty != null && previousTask.localContext.clientStty.isConnected())
                            {
                                previousTask.localContext.clientStty.send("bye", "Sending bye command to Remote Subscriber");
                                previousTask.localContext.clientStty.disconnect();
                                previousTask.localContext.clientSftp.stopClient();
                                previousTask.localContext.clientStty = null;
                                previousTask.localContext.clientSftp = null;
                                previousTask.localContext.clientSftpMetadata = null;
                            }
                        }

                        // remote and connect needed?
                        if (isSubscriberRemote() && connect)
                        {
                            // start the serveStty client for automation; Throws exception if missing parameters
                            if (localContext.main.connectSubscriber(localContext, false, localContext.cfg.isGui()))
                            {
                                // give Subscriber a moment to be ready for login
                                try
                                {
                                    Thread.sleep(1000);
                                }
                                catch (InterruptedException e)
                                {
                                }

                                // login User if enabled
                                if ((localContext.subscriberUser = localContext.subscriberRepo.login(localContext.publisherRepo.getLibraries().key, true)) == null)
                                {
                                    localContext.clientStty.disconnect();
                                    throw new MungeException(MessageFormat.format(localContext.cfg.gs("Z.login.failed.from.to"), localContext.publisherUser.getName(),
                                            localContext.publisherRepo.getLibraries().description, localContext.subscriberRepo.getLibraries().description));
                                }

                                // start the serveSftp transfer client
                                localContext.clientSftp = new ClientSftp(localContext, localContext.publisherRepo, localContext.subscriberRepo, true);
                                if (!localContext.clientSftp.startClient("transfer"))
                                {
                                    throw new MungeException(MessageFormat.format(localContext.cfg.gs("Main.subscriber.sftp.transfer.to.failed.to.connect"),
                                            localContext.subscriberRepo.getLibraries().description));
                                }
                            }
                        }
                        else // Subscriber is local
                        {
                            if (isSameSubscriber())
                                localContext.subscriberUser = previousTask.localContext.subscriberUser;
                            else
                            {
                                // login User if enabled
                                if ((localContext.subscriberUser = localContext.subscriberRepo.login(localContext.publisherRepo.getLibraries().key, false)) == null)
                                {
                                    throw new MungeException(MessageFormat.format(localContext.cfg.gs("Z.login.failed.from.to"), localContext.publisherUser.getName(),
                                            localContext.publisherRepo.getLibraries().description, localContext.subscriberRepo.getLibraries().description));
                                }
                            }
                        }
                    }
                    else
                    {
                        // Subscriber Listener does not need a Publisher if Authentication keys are defined
                        if (currentTool.getInternalName().equalsIgnoreCase("Operations") &&
                                ((OperationsTool) currentTool).getOperation() == OperationsTool.Operations.SubscriberListener &&
                                (((OperationsTool) currentTool).getOptAuthKeys() == null || ((OperationsTool) currentTool).getOptAuthKeys().isEmpty()))
                        {
                            // otherwise login the Publisher to the Subscriber
                            if (localContext.preferences.isUsersEnabled())
                            {
                                if (localContext.publisherRepo == null)
                                    throw new MungeException(localContext.cfg.gs("Z.publisher.login.missing"));

                                if ((localContext.subscriberUser = localContext.subscriberRepo.login(localContext.publisherRepo.getLibraries().key, false)) == null)
                                    throw new MungeException(MessageFormat.format(localContext.cfg.gs("Z.login.failed.from.to"), localContext.publisherUser.getName(),
                                            localContext.publisherRepo.getLibraries().description, repo.getLibraries().description));
                            }
                        }
                    }

                    if (localContext.cfg.isGui() && localContext.mainFrame != null && localContext.mainFrame.isVisible())
                        localContext.navigator.displayConnection();
                    break;
                }
                case Repository.HINT_SERVER:
                {
                    // if not executing as an operation; Operation tasks run a new Main()
                    if (!currentTool.getInternalName().equalsIgnoreCase("Operations"))
                    {
                        localContext.cfg.setHintsDaemonFilename("");
                        localContext.cfg.setHintTrackerFilename("");
                        if (isHintsRemote())
                            localContext.cfg.setHintsDaemonFilename(repo.getJsonFilename());
                        else
                            localContext.cfg.setHintTrackerFilename(repo.getJsonFilename());
                        localContext.hintsRepo = repo;

                        if (isSameHints())
                        {
                            localContext.hintKeys = previousTask.localContext.hintKeys;
                            localContext.hintsHandler = previousTask.localContext.hintsHandler;
                            localContext.hintsStty = previousTask.localContext.hintsStty;
                            if (isHintsRemote() && localContext.hintsStty != null && localContext.hintsStty.isConnected())
                            {
                                connect = false;
                                disconnect = false;
                            }
                        }

                        if (disconnect)
                        {
                            if (previousTask != null && previousTask.localContext.hintsStty != null && previousTask.localContext.hintsStty.isConnected())
                            {
                                previousTask.localContext.hintsStty.send("bye", "Sending bye command to Remote Hint Server");
                                previousTask.localContext.hintsStty.disconnect();
                                previousTask.localContext.hintsStty = null;
                            }
                        }

                        if (isHintsRemote() && connect)
                        {
                            localContext.main.connectHints(localContext, localContext.hintsRepo);
                        }
                    }
                    break;
                }
            }
        }

        if (repo != null)
            repo.setContext(localContext);

        return repo;
    }

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

    private boolean isPreviousTaskSameConnection()
    {
        boolean result = false;

        if (previousTask != null)
        {
            if (previousTask.subscriberRemote &&
                previousTask.localContext.subscriberRepo.getLibraryData().libraries.key.equalsIgnoreCase(localContext.subscriberRepo.getLibraryData().libraries.key) &&
                previousTask.localContext.clientStty != null && previousTask.localContext.clientStty.isConnected())
            {
                result = true;
            }
        }

        return result;
    }

    private boolean isSameHints()
    {
        boolean result = false;
        if (previousTask != null)
        {
            if (localContext.hintsRepo != null && previousTask.localContext.hintsRepo != null)
            {
                if (localContext.hintsRepo.getLibraryData().libraries.key.equalsIgnoreCase(previousTask.localContext.hintsRepo.getLibraryData().libraries.key))
                    result = true;
            }
        }
        return result;
    }

    private boolean isSamePublisher()
    {
        boolean result = false;
        if (previousTask != null)
        {
            if (localContext.publisherRepo != null && previousTask.localContext.publisherRepo != null)
            {
                if (localContext.publisherRepo.getLibraryData().libraries.key.equalsIgnoreCase(previousTask.localContext.publisherRepo.getLibraryData().libraries.key))
                    result = true;
            }
        }
        return result;
    }

    private boolean isSameSubscriber()
    {
        boolean result = false;
        if (previousTask != null)
        {
            if (localContext.subscriberRepo != null && previousTask.localContext.subscriberRepo != null)
            {
                if (localContext.subscriberRepo.getLibraryData().libraries.key.equalsIgnoreCase(previousTask.localContext.subscriberRepo.getLibraryData().libraries.key))
                    result = true;
            }
        }
        return result;
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
        if (logger == null)
            logger = LogManager.getLogger("applog");

        context.taskDone = false;

        if (currentTool == null)
            currentTool = getTool();

        if (currentTool != null)
        {
            currentTool.setContext(localContext);

            if ((origins == null || origins.size() == 0) && !useCachedLastTask(localContext) && currentTool.isToolOriginsUsed())
            {
                String msg = localContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                logger.info(msg);
                if (localContext.navigator != null)
                    JOptionPane.showMessageDialog(localContext.mainFrame, msg, localContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // get repositories or paths
            if (useCachedLastTask(localContext)) // not an Operations tool
            {
                setPublisherKey(previousTask.getPublisherKey());
                if (previousTask.localContext != null)
                {
                    publisherPath = previousTask.publisherPath;
                    localContext.publisherRepo = previousTask.localContext.publisherRepo;
                    localContext.publisherUser = previousTask.localContext.publisherUser;
                    localContext.publisherUser.setContext(localContext);
                }
                else
                    publisherPath = "";

                remoteType = previousTask.remoteType;

                if (previousTask.localContext.subscriberRepo != null)
                {
                    subscriberPath = previousTask.subscriberPath;
                    localContext.subscriberRepo = previousTask.localContext.subscriberRepo;
                    localContext.subscriberUser = previousTask.localContext.subscriberUser;
                    localContext.subscriberUser.setContext(localContext);

                    setSubscriberKey(previousTask.getSubscriberKey());
                    setSubscriberOverride(previousTask.subscriberOverride);
                    setSubscriberRemote(previousTask.subscriberRemote);
                    if (subscriberRemote)
                        localContext.subscriberRepo = getRepo(getSubscriberKey(), getSubscriberPath(), Repository.SUBSCRIBER);
                }
                else
                    subscriberPath = "";

                setHintsKey(previousTask.getHintsKey());
                setHintsOverrideHost(previousTask.isHintsOverrideHost());
                setHintsRemote(previousTask.isHintsRemote());
                hintsPath = previousTask.hintsPath;

                setOrigins(previousTask.getOrigins());
            }
            else
            {
                localContext.publisherRepo = getRepo(getPublisherKey(), getPublisherPath(), Repository.PUBLISHER);
                if (localContext.publisherRepo == null && localContext.preferences.isUsersEnabled())
                {
                    localContext.publisherRepo = context.publisherRepo;
                    localContext.publisherUser = context.publisherUser;
                }
                if (localContext.publisherRepo != null)
                {
                    publisherPath = localContext.publisherRepo.getJsonFilename();
                    localContext.publisherUser = localContext.publisherRepo.getUser();
                    if (localContext.publisherUser != null)
                        localContext.publisherUser.setContext(localContext);
                }
                else if (getPublisherKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Publisher but none specified");

                localContext.cfg.setOverrideSubscriberHost(getSubscriberOverride());
                localContext.subscriberRepo = getRepo(getSubscriberKey(), getSubscriberPath(), Repository.SUBSCRIBER);
                if (localContext.subscriberRepo != null)
                {
                    subscriberPath = localContext.subscriberRepo.getJsonFilename();
                    localContext.subscriberUser = localContext.subscriberRepo.getUser();
                    if (localContext.subscriberUser != null)
                        localContext.subscriberUser.setContext(localContext);
                }
                else if (getSubscriberKey().equals(Task.ANY_SERVER))
                    throw new MungeException("\"Any Server\" defined for Subscriber but none specified");

                remoteType = (isSubscriberRemote() || (getSubscriberKey().equals(Task.ANY_SERVER) && localContext.cfg.isRemoteOperation())) ? "P" : "-";

                if (previousTask != null)
                    previousTask.remoteType = remoteType;

                if (getHintsKey() != null && !getHintsKey().isEmpty())
                {
                    localContext.cfg.setOverrideHintsHost(isHintsOverrideHost());
                    localContext.hintsRepo = getRepo(getHintsKey(), getHintsPath(), Repository.HINT_SERVER);
                    if (localContext.hintsRepo != null)
                        hintsPath = localContext.hintsRepo.getJsonFilename();
                }
                else
                {
                    localContext.cfg.setOverrideHintsHost(false);
                    setHintsPath("");
                    localContext.hintsRepo = null;
                }
            }

            if (localContext.publisherRepo != null)
            {
                Persistent.lastPublisherRepo = localContext.publisherRepo;
                Persistent.lastPublisherUser = localContext.publisherUser;
            }
            if (localContext.subscriberRepo != null)
            {
                Persistent.lastSubscriberRepo = localContext.subscriberRepo;
                Persistent.lastSubscriberUser = localContext.subscriberUser;
            }

            localContext.cfg.setOperation(remoteType);

            // setup Transfer if needed
            if ((currentTool.getInternalName().equalsIgnoreCase("JunkRemover") ||
                currentTool.getInternalName().equalsIgnoreCase("Renamer")) &&
                !isPreviousTaskSameConnection())
            {
                localContext.transfer = new Transfer(localContext);
                localContext.transfer.initialize(); // checks banner commands and sets subscriber working directory
            }
            else
            {
                if (previousTask != null && previousTask.localContext != null)
                    localContext.transfer = previousTask.localContext.transfer;
            }

            // set subscriber working directory
            String directory;
            if (isSubscriberRemote() && localContext.clientStty != null)
                directory = localContext.clientStty.getWorkingDirectoryRemote();
            else
                directory = localContext.cfg.getWorkingDirectory();
            localContext.cfg.setWorkingDirectorySubscriber(directory);

            // set the task for a Subscriber Listener so it does not exit in Connection.run()
            if (currentTool.getInternalName().equalsIgnoreCase("Operations") &&
                    ((OperationsTool) currentTool).isToolSubscriber())
            {
                localContext.task = this;
            }

            // run it
            currentTool.processTool(this);

            if (currentTool.isRequestStop())
                return false;

            context.fault = localContext.fault;
        }
        else
            throw new MungeException(localContext.cfg.gs("Task.tool.not.found") + getInternalName() + ": " + getConfigName());

        // wait for listener task to stop - to sequence the job
        if (currentTool.getInternalName().equalsIgnoreCase("Operations") &&
                ((OperationsTool) currentTool).isToolSubscriber())
        {
            logger.info(context.cfg.gs("Task.waiting.for.subscriber.listener.to.exit"));
            while (!localContext.taskDone)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        context.task = null;
        context.taskDone = false;

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
        this.localContext = (Context)context.clone();
    }

    public void setDryRun(boolean sense)
    {
        this.dryRun = sense;
    }

    public void setEmailTool(String emailTool)
    {
        this.emailTool = emailTool;
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
