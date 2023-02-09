package com.groksoft.els.gui;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class SavedEnvironment
{
    Context context;

    private boolean dryRun;
    private boolean publishOperation;
    private String publisherCollectionFilename;
    private String publisherLibrariesFilename;
    private String remoteType = "-";
    private String subscriberCollectionFilename;
    private String subscriberLibrariesFilename;
    private Logger logger = LogManager.getLogger("applog");

    public SavedEnvironment(Context context)
    {
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

            context.cfg.setDryRun(dryRun);
            context.cfg.setRemoteType(remoteType);

            context.cfg.setPublishOperation(publishOperation);
            if (!context.cfg.getPublisherCollectionFilename().equals(publisherCollectionFilename))
            {
                context.cfg.setPublisherCollectionFilename(publisherCollectionFilename);
                pcfChanged = true;
            }
            if (!context.cfg.getPublisherLibrariesFileName().equals(publisherLibrariesFilename))
            {
                context.cfg.setPublisherLibrariesFileName(publisherLibrariesFilename);
                plfChanged = true;
            }
            if (!context.cfg.getSubscriberCollectionFilename().equals(subscriberCollectionFilename))
            {
                context.cfg.setSubscriberCollectionFilename(subscriberCollectionFilename);
                scfChanged = true;
            }
            if (!context.cfg.getSubscriberLibrariesFileName().equals(subscriberLibrariesFilename))
            {
                context.cfg.setSubscriberLibrariesFileName(subscriberLibrariesFilename);
                slfChanged = true;
            }

            if (context.cfg.getPublisherFilename() != null && context.cfg.getPublisherFilename().length() > 0 && (pcfChanged || plfChanged))
            {
                context.publisherRepo = new Repository(context, Repository.PUBLISHER);
                context.publisherRepo.read(context.cfg.getPublisherFilename(), true);
            }
            if (context.cfg.getSubscriberFilename() != null && context.cfg.getSubscriberFilename().length() > 0 && (scfChanged || slfChanged))
            {
                context.subscriberRepo = new Repository(context, Repository.SUBSCRIBER);
                context.subscriberRepo.read(context.cfg.getSubscriberFilename(), true);
            }

            if (context.cfg.isRemoteSession() && task.getSubscriberKey().length() > 0 &&
                    context.clientStty != null && context.clientStty.isConnected() &&
                    !context.clientStty.getTheirKey().equals(context.subscriberRepo.getLibraryData().libraries.key) &&
                    !task.getSubscriberKey().equals(Task.ANY_SERVER))
            {
                task.connectRemote(context, context.publisherRepo, context.subscriberRepo);
                Thread.sleep(2000); // wait for connection to be setup
            }
            else
            {
                // if not a remote session, and connected, disconnect
                if (!context.cfg.isRemoteSession() && context.clientStty != null && context.clientStty.isConnected())
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

            if (context != null && context.browser != null)
            {
                context.browser.refreshAll();
            }
        }
        catch (Exception e)
        {
            if (context != null && context.browser != null)
            {
                String msg = context.cfg.gs("Z.exception") + e.getMessage() + "; " + Utils.getStackTrace(e);
                JOptionPane.showMessageDialog(context.mainFrame, msg,
                        context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(e.getMessage() + " " + Utils.getStackTrace(e));
        }
    }

    public void save()
    {
        dryRun = context.cfg.isDryRun();
        remoteType = context.cfg.getRemoteType();
        publishOperation = context.cfg.isPublishOperation();
        publisherCollectionFilename = context.cfg.getPublisherCollectionFilename();
        publisherLibrariesFilename = context.cfg.getPublisherLibrariesFileName();
        subscriberCollectionFilename = context.cfg.getSubscriberCollectionFilename();
        subscriberLibrariesFilename = context.cfg.getSubscriberLibrariesFileName();
    }
}
