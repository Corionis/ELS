package com.groksoft.els.jobs;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.repository.Repositories;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;

import javax.swing.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Task implements Comparable, Serializable
{
    public static final String ANY_SERVER = "_ANY_SERVER_";

    private String configName; // name of tool configuration
    private String internalName; // internal name of tool
    private String publisherKey = "";
    private String subscriberKey = "";
    private ArrayList<Origin> origins;

    transient AbstractTool currentTool = null;
    transient boolean dual = true;

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

    private Repository getRepo(Configuration config, Context context, String key, boolean forPublisher) throws Exception
    {
        Repository repo = null;
        if (key != null && key.length() > 0)
        {
            if (key.equals(ANY_SERVER))
            {
                if (forPublisher)
                    repo = context.publisherRepo;
                else
                    repo = context.subscriberRepo;
            }
            else
            {
                Repositories repositories = new Repositories();
                repositories.loadList(config);

                Repositories.Meta meta;
                if (key.length() > 0)
                {
                    meta = repositories.find(key);
                    if (meta != null)
                    {
                        repo = new Repository(config, forPublisher ? Repository.PUBLISHER : Repository.SUBSCRIBER);
                        repo.read(meta.path, true);
                    }
                    else // QUESTION should this be handled in a non-fatal way?
                        throw new MungeException(key + config.gs("Z.not.found"));
                }
            }
        }
        return repo;
    }

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

    public boolean isDual()
    {
        return dual;
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
    public void process(GuiContext guiContext, Configuration config, Context ctxt, boolean dryRun) throws Exception
    {
        // get tool
        Tools tools = new Tools();
        currentTool = tools.loadTool(guiContext, config, ctxt, getInternalName(), getConfigName());
        if (currentTool != null)
        {
            // get repos
            Repository pubRepo = getRepo(config, ctxt, getPublisherKey(), true);
            Repository subRepo = getRepo(config, ctxt, getSubscriberKey(), false);
            if (pubRepo == null && subRepo == null)
                throw new MungeException((config.gs("Task.no.repository.is.loaded")));

            currentTool.processTool(guiContext, pubRepo, subRepo, origins, dryRun);
        }
        else
            throw new MungeException(config.gs("Task.tool.not.found") + getInternalName() + ":" + getConfigName());
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
        if (tool != null)
        {
            currentTool = tool;

            // get repos
            Repository pubRepo = getRepo(guiContext.cfg, guiContext.context, getPublisherKey(), true);
            Repository subRepo = getRepo(guiContext.cfg, guiContext.context, getSubscriberKey(), false);
            if (pubRepo == null && subRepo == null)
                throw new MungeException((guiContext.cfg.gs("Task.no.repository.is.loaded")));

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

    public void setPublisherKey(String publisherKey)
    {
        this.publisherKey = publisherKey;
    }

    public void setSubscriberKey(String subscriberKey)
    {
        this.subscriberKey = subscriberKey;
    }

}
