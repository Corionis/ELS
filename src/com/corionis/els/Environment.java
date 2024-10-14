package com.corionis.els;

import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.stty.ClientStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class Environment
{
    private Context context;
    private Logger logger = LogManager.getLogger("applog");

    private Environment()
    {
        // hide default constructor
    }

    public Environment(Context context)
    {
        this.context = (Context) context.clone();
    }

    public Configuration getConfiguration()
    {
        return context.cfg;
    }

    public Context getContext()
    {
        return context;
    }

/*
    public void restore()
    {
        Configuration restored = localContext.cfg.cloneRestore(savedCfg);
        // sanity checks
        if (restored != original)
            logger.info("Restored configuration does not match original configuration");
        if (restored.getContext() != localContext)
            logger.info("Restored Context does not match original Context");
    }
*/

    public boolean switchConnections() throws Exception
    {
        boolean pcfChanged = false;
        boolean plfChanged = false;
        boolean scfChanged = false;
        boolean slfChanged = false;
        boolean hjfChanged = false;

        // what's different from current configuration?
        if (context.cfg.getPublisherCollectionFilename() != null &&
            !context.cfg.getPublisherCollectionFilename().equals(this.context.cfg.getPublisherCollectionFilename()))
        {
            pcfChanged = true;
        }
        if (context.cfg.getPublisherLibrariesFileName() != null &&
            !context.cfg.getPublisherLibrariesFileName().equals(this.context.cfg.getPublisherLibrariesFileName()))
        {
            plfChanged = true;
        }
        if (context.cfg.getSubscriberCollectionFilename() != null &&
                !context.cfg.getSubscriberCollectionFilename().equals(this.context.cfg.getSubscriberCollectionFilename()))
        {
            scfChanged = true;
        }
        if (context.cfg.getSubscriberLibrariesFileName() != null &&
                !context.cfg.getSubscriberLibrariesFileName().equals(this.context.cfg.getSubscriberLibrariesFileName()))
        {
            slfChanged = true;
        }
        if (context.cfg.getHintHandlerFilename() != null &&
                !context.cfg.getHintHandlerFilename().equals(this.context.cfg.getHintHandlerFilename()))
        {
            hjfChanged = true;
        }

        // subscriber
        if (scfChanged || slfChanged)
        {
            setupSubscriber();
            Thread.sleep(3000); // wait for connection to be setup
        }

        // hintsHandler
        if (hjfChanged)
        {
            setupHintTracking();
        }

        // navigator
        if (pcfChanged || plfChanged || scfChanged || slfChanged)
        {
            setupNavigator(); // Navigator metaSftp connection
        }

        return true;
    }

    private boolean setupNavigator() throws Exception
    {
        if (context.cfg.isNavigator())
        {
            if (context.clientSftpMetadata != null && context.clientSftpMetadata.isConnected())
            {
                // start the serveSftp metadata client
                context.clientSftpMetadata = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, context.main.primaryExecution);
                if (!context.clientSftpMetadata.startClient("metadata"))
                {
                    if (context.navigator != null) // if GUI is up
                    {
                        JOptionPane.showMessageDialog(context.mainFrame,
                                context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                                context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean setupSubscriber() throws Exception
    {
        // close existing STTY connection if not same subscriber
        if (context.clientStty != null && context.clientStty.isConnected())
        {
            // compare repo and connection keys
            if (!context.clientStty.getMyKey().equals(context.publisherRepo.getLibraryData().libraries.key) ||
                !context.clientStty.getTheirKey().equals(context.subscriberRepo.getLibraryData().libraries.key))
            {
                try
                {
                    if (!context.timeout)
                    {
                        context.clientStty.send("bye", "Disconnect Subscriber: " +
                                context.clientStty.getTheirRepo().getLibraryData().libraries.description);
                        Thread.sleep(500);

                        context.clientStty.disconnect();

                        // close any existing SFTP connections
                        if (context.clientSftp != null && context.clientSftp.isConnected())
                        {
                            context.clientSftp.stopClient();
                        }
                        Thread.sleep(500);
                    }
                }
                catch (Exception e)
                {
                }
            }
        }

        // start the serveStty client for automation
        if (context.cfg.isRemoteOperation())
        {
            context.clientStty = new ClientStty(context, false, true, false); //localContext.main.primaryServers);
            if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
            {
                context.cfg.setOperation("-");
                if (context.navigator != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect") +
                                    context.subscriberRepo.getLibraryData().libraries.host,
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

            // start the serveSftp transfer client
            context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, context.main.primaryExecution);
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
        }
        else
        {
            context.cfg.setOperation("-");
            context.clientStty = null;
            context.clientSftp = null;
        }

        return true;
    }

    private boolean setupHintTracking() throws Exception
    {
        // close existing Hint Server STTY connection if not same server
        if (context.hintsStty != null && context.hintsStty.isConnected())
        {
            try
            {
                // compare repo and connection keys
                if (!context.hintsStty.getMyKey().equals(context.publisherRepo.getLibraryData().libraries.key) ||
                        !context.hintsStty.getTheirKey().equals(context.subscriberRepo.getLibraryData().libraries.key))
                {
                    context.clientStty.send("bye", "Disconnect Hint Server: " +
                            context.hintsStty.getMyRepo().getLibraryData().libraries.description);
                    Thread.sleep(500);
                    context.hintsStty.disconnect();
                    Thread.sleep(500);
                }
            }
            catch (Exception e)
            {
            }
        }

        context.main.setupHints(context.hintsRepo);

        return true;
    }

}
