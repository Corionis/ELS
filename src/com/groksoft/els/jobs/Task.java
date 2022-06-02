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

public class Task implements Serializable
{
    public static final String CURRENT_LOADED = "_CURRENT_LOADED_";

    private String configName; // name of tool configuration
    private String internalName; // internal name of tool
    private String publisherKey;
    private String subscriberKey;
    private ArrayList<Origin> origins;

    public Task(String internalName, String configName)
    {
        this.internalName = internalName;
        this.configName = configName;
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
            if (key.equals(CURRENT_LOADED))
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
                        repo.read(meta.path);
                    }
                    else
                        throw new MungeException((forPublisher ? "Publisher" : "Subscriber") + " repository " + key + " not found");
                }
            }
        }
        return repo;
    }

    public String getSubscriberKey()
    {
        return subscriberKey;
    }

    /**
     * Process the task
     *
     * @param config The Configuration
     * @param ctxt The Context
     * @param dryRun Boolean for a dry-run
     * @throws Exception
     */
    public void process(Configuration config, Context ctxt, boolean dryRun) throws Exception
    {
        // get tool
        Tools tools = new Tools();
        AbstractTool tool = tools.getTool(config, ctxt, getConfigName(), getInternalName());

        if (tool != null)
        {
            // get repos
            Repository pubRepo = getRepo(config, ctxt, getPublisherKey(), true);
            Repository subRepo = getRepo(config, ctxt, getSubscriberKey(), false);

            tool.processTool(pubRepo, subRepo, origins, dryRun);
        }
        else
            throw new MungeException("Tools not found " + getInternalName() + ":" + getConfigName());
    }

    /**
     * Process the task on a SwingWorker thread
     *
     * @param guiContext The GuiContext
     * @param dryRun Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> process(GuiContext guiContext, boolean dryRun, AbstractTool tool) throws Exception
    {
        if (tool != null)
        {
            // get repos
            Repository pubRepo = getRepo(guiContext.cfg, guiContext.context, getPublisherKey(), true);
            Repository subRepo = getRepo(guiContext.cfg, guiContext.context, getSubscriberKey(), false);

            return tool.processToolThread(pubRepo, subRepo, origins, dryRun);
        }
        return null;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
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
