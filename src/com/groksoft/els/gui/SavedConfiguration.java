package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class SavedConfiguration
{
    GuiContext guiContext;
    Configuration cfg;
    Context context;

    private boolean dryRun;
    private boolean publishOperation;
    private String publisherCollectionFilename;
    private String publisherLibrariesFilename;
    private String remoteType = "-";
    private String subscriberCollectionFilename;
    private String subscriberLibrariesFilename;
    private Logger logger = LogManager.getLogger("applog");

    public SavedConfiguration(GuiContext guiContext, Configuration config, Context context)
    {
        this.guiContext = guiContext;
        this.cfg = config;
        this.context = context;
    }

    public void restore(Task task)
    {
        try
        {
            boolean pcfChanged = false;
            boolean plfChanged = false;
            boolean scfChanged = false;
            boolean slfChanged = false;

            cfg.setDryRun(dryRun);
            cfg.setRemoteType(remoteType);

            cfg.setPublishOperation(publishOperation);
            if (!cfg.getPublisherCollectionFilename().equals(publisherCollectionFilename))
            {
                cfg.setPublisherCollectionFilename(publisherCollectionFilename);
                pcfChanged = true;
            }
            if (!cfg.getPublisherLibrariesFileName().equals(publisherLibrariesFilename))
            {
                cfg.setPublisherLibrariesFileName(publisherLibrariesFilename);
                plfChanged = true;
            }
            if (!cfg.getSubscriberCollectionFilename().equals(subscriberCollectionFilename))
            {
                cfg.setSubscriberCollectionFilename(subscriberCollectionFilename);
                scfChanged = true;
            }
            if (!cfg.getSubscriberLibrariesFileName().equals(subscriberLibrariesFilename))
            {
                cfg.setSubscriberLibrariesFileName(subscriberLibrariesFilename);
                slfChanged = true;
            }

            if (cfg.getPublisherFilename() != null && cfg.getPublisherFilename().length() > 0 && (pcfChanged || plfChanged))
            {
                context.publisherRepo = new Repository(cfg, Repository.PUBLISHER);
                context.publisherRepo.read(cfg.getPublisherFilename(), true);
            }
            if (cfg.getSubscriberFilename() != null && cfg.getSubscriberFilename().length() > 0 && (scfChanged || slfChanged))
            {
                context.subscriberRepo = new Repository(cfg, Repository.SUBSCRIBER);
                context.subscriberRepo.read(cfg.getSubscriberFilename(), true);
            }

            if (cfg.isRemoteSession() && task.getSubscriberKey().length() > 0 &&
                    context.clientStty != null && context.clientStty.isConnected() &&
                    !context.clientStty.getTheirKey().equals(context.subscriberRepo.getLibraryData().libraries.key) &&
                    !task.getSubscriberKey().equals(Task.ANY_SERVER))
            {
                task.connectRemote(cfg, context, context.publisherRepo, context.subscriberRepo);
                Thread.sleep(2000); // wait for connection to be setup
            }
            else
            {
                // if not a remote session, and connected, disconnect
                if (!cfg.isRemoteSession() && context.clientStty != null && context.clientStty.isConnected())
                {
                    try
                    {
                        context.clientStty.send("bye", "Sending bye command");
                        Thread.sleep(500);
                        context.clientSftp.stopClient();
                        Thread.sleep(500);
                    }
                    catch (Exception e)
                    {
                    }
                }
            }

            if (guiContext != null)
            {
                guiContext.browser.refreshAll();
            }
        }
        catch (Exception e)
        {
            if (guiContext != null)
            {
                String msg = guiContext.cfg.gs("Z.exception") + e.getMessage() + "; " + Utils.getStackTrace(e);
                JOptionPane.showMessageDialog(guiContext.mainFrame, msg,
                        guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(e.getMessage() + " " + Utils.getStackTrace(e));
        }
    }

    public void save()
    {
        dryRun = cfg.isDryRun();
        remoteType = cfg.getRemoteType();
        publishOperation = cfg.isPublishOperation();
        publisherCollectionFilename = cfg.getPublisherCollectionFilename();
        publisherLibrariesFilename = cfg.getPublisherLibrariesFileName();
        subscriberCollectionFilename = cfg.getSubscriberCollectionFilename();
        subscriberLibrariesFilename = cfg.getSubscriberLibrariesFileName();
    }
}
