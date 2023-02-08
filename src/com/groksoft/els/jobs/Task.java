package com.groksoft.els.jobs;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.gui.GuiContext;
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

    transient private AbstractTool currentTool = null;
    transient private boolean dual = true;
    transient private GuiContext guiContext = null;
    transient private Task lastTask = null;
    transient private Repository pubRepo = null;
    transient private Repository subRepo = null;
    transient private boolean realOnly = false;
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

    public boolean connectRemote(Configuration config, Context context, Repository publisherRepo, Repository subscriberRepo) throws Exception
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
        context.clientStty = new ClientStty(config, context, false, true);
        if (!context.clientStty.connect(publisherRepo, subscriberRepo))
        {
            config.setRemoteType("-");
            if (guiContext != null)
            {
                JOptionPane.showMessageDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect") +
                                subscriberRepo.getLibraryData().libraries.host,
                        guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        // check for opening commands from Subscriber
        // *** might change cfg options for subscriber and targets that are handled below ***
        if (context.clientStty.checkBannerCommands())
        {
            logger.info(config.gs("Transfer.received.subscriber.commands") + (config.isRequestCollection() ? "RequestCollection " : "") + (config.isRequestTargets() ? "RequestTargets" : ""));
        }

        // close any existing SFTP connections
        if (didDisconnect)
        {
            context.clientSftp.stopClient();
        }

        // start the serveSftp client
        context.clientSftp = new ClientSftp(config, publisherRepo, subscriberRepo, true);
        if (!context.clientSftp.startClient())
        {
            config.setRemoteType("-");
            if (guiContext != null)
            {
                JOptionPane.showMessageDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                        guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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

    private Repository getRepo(Configuration cfg, Context context, String key, boolean forPublisher) throws Exception
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
                    repositories.loadList(cfg);

                    Repositories.Meta meta;
                    if (key.length() > 0)
                    {
                        meta = repositories.find(key);
                        if (meta != null)
                        {
                            repo = new Repository(cfg, forPublisher ? Repository.PUBLISHER : Repository.SUBSCRIBER);
                            repo.read(meta.path, true);
                        }
                        else
                            throw new MungeException(key + cfg.gs("Z.not.found"));
                    }
                }
            }
        }

        if (repo != null)
        {
            cfg.setPublisherCollectionFilename("");
            cfg.setSubscriberCollectionFilename("");
            if (forPublisher)
                cfg.setPublisherLibrariesFileName(repo.getJsonFilename());
            else
                cfg.setSubscriberLibrariesFileName(repo.getJsonFilename());
        }

        return repo;
    }

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

    public AbstractTool getTool()
    {
        return currentTool;
    }

    public boolean isCachedLastTask(Configuration config, Context ctxt)
    {
        AbstractTool tool;
        if (currentTool == null)
        {
            if (tools == null)
                this.tools = new Tools();
            tool = tools.makeTempTool(getInternalName(), config, ctxt);
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

    public boolean isOriginPathsAllowed(Configuration config, Context ctxt)
    {
        AbstractTool tool;
        if (currentTool == null)
        {
            if (tools == null)
                this.tools = new Tools();
            tool = tools.makeTempTool(getInternalName(), config, ctxt);
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
     * @param config The Configuration
     * @param ctxt   The Context
     * @param dryRun Boolean for a dry-run
     * @throws Exception
     */
    public boolean process(GuiContext guiContext, Configuration config, Context ctxt, boolean dryRun) throws Exception
    {
        this.guiContext = guiContext;
        if (logger == null)
            logger = LogManager.getLogger("applog");

        // get tool
        if (tools == null)
            this.tools = new Tools();

        currentTool = tools.loadTool(guiContext, config, ctxt, getInternalName(), getConfigName());
        if (currentTool != null)
        {
            if ((origins == null || origins.size() == 0) && !useCachedLastTask(config, ctxt) && currentTool.isOriginPathsAllowed())
            {
                if (guiContext != null)
                {
                    String msg = guiContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                    logger.info(msg);
                    JOptionPane.showMessageDialog(guiContext.mainFrame, msg, guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                    logger.info(config.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName());

                return false;
            }

            // get repos
            if (useCachedLastTask(config, ctxt))
            {
                pubRepo = lastTask.pubRepo;
                subRepo = lastTask.subRepo;
                setSubscriberRemote(lastTask.subscriberRemote);
            }
            else
            {
                pubRepo = getRepo(config, ctxt, getPublisherKey(), true);
                subRepo = getRepo(config, ctxt, getSubscriberKey(), false);
            }
            if (pubRepo == null && subRepo == null)
                throw new MungeException((config.gs("Task.no.repository.is.loaded")));

            if (!(currentTool instanceof OperationsTool))
            {
                type = (isSubscriberRemote() || getSubscriberKey().equals(Task.ANY_SERVER)) ? "P" : "-";
                config.setRemoteType(type);
                config.setPublishOperation(false);  // TODO Change when OperationsUI tool added

                if (isSubscriberRemote())
                {
                    Repository me = pubRepo;
                    if (me == null)
                        me = ctxt.publisherRepo;
                    if (!connectRemote(config, ctxt, me, subRepo))
                        return false;
                }
            }
            else
                ctxt.nestedProcesses = true;

            currentTool.processTool(guiContext, pubRepo, subRepo, origins, dryRun, useCachedLastTask(config, ctxt) ? lastTask : null);
        }
        else
            throw new MungeException(config.gs("Task.tool.not.found") + getInternalName() + ":" + getConfigName());
        return true;
    }

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param guiContext The GuiContext
     * @param dryRun     Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> process(GuiContext guiContext, AbstractTool tool, boolean dryRun) throws Exception
    {
        if (logger == null)
            logger = LogManager.getLogger("applog");

        if (tool != null)
        {
            this.guiContext = guiContext;
            currentTool = tool;

            if ((origins == null || origins.size() == 0) && !useCachedLastTask(guiContext.cfg, guiContext.context) && currentTool.isOriginPathsAllowed())
            {
                if (guiContext != null)
                {
                    String msg = guiContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName();
                    logger.info(msg);
                    JOptionPane.showMessageDialog(guiContext.mainFrame, msg, guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                    logger.info(guiContext.cfg.gs("JobsUI.task.has.no.origins") + currentTool.getDisplayName() + ", " + currentTool.getConfigName());
                return null;
            }

            // get repos
            if (useCachedLastTask(guiContext.cfg, guiContext.context))
            {
                pubRepo = lastTask.pubRepo;
                subRepo = lastTask.subRepo;
                setSubscriberRemote(lastTask.subscriberRemote);
            }
            else
            {
                pubRepo = getRepo(guiContext.cfg, guiContext.context, getPublisherKey(), true);
                subRepo = getRepo(guiContext.cfg, guiContext.context, getSubscriberKey(), false);
            }
            if (pubRepo == null && subRepo == null)
                throw new MungeException((guiContext.cfg.gs("Task.no.repository.is.loaded")));

            // No connection change. This method is only used by the tool's Run button, not by a Job.
            // So it uses whatever subscriber is currently loaded.

            return tool.processToolThread(guiContext, pubRepo, subRepo, origins, dryRun);
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

    public boolean useCachedLastTask(Configuration cfg, Context ctxt)
    {
        if (isCachedLastTask(cfg, ctxt) && lastTask != null && publisherKey.equalsIgnoreCase(CACHEDLASTTASK))
            return true;
        return false;
    }

}
