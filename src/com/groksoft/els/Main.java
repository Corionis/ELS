package com.groksoft.els;

import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hints.Store;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.util.Date;

import static com.groksoft.els.Configuration.*;

/**
 * ELS main program
 */
public class Main
{
    public boolean isListening = false;
    Configuration cfg;
    Context context = new Context();
    private Logger logger = null;

    /**
     * Instantiates the Main application
     */
    public Main()
    {
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        Main els = new Main();
        els.process(args);          // ELS Processor
    } // main

    private void connectHintServer(Repository repo) throws Exception
    {
        if (cfg.isUsingHintServer())
        {
            context.statusRepo = new Repository(cfg);
            context.statusRepo.read(cfg.getStatusServerFilename());

            // start the serveStty client to the hints status server
            context.statusStty = new ClientStty(cfg, false, true);
            if (!context.statusStty.connect(repo, context.statusRepo))
            {
                throw new MungeException("Hint Status Server failed to connect");
            }
        }
    }

    /**
     * execute the process
     *
     * @param args the input arguments
     * @return
     */
    public int process(String[] args)
    {
        int returnValue = 0;
        ThreadGroup sessionThreads = null;
        cfg = new Configuration();
        Process proc;
        Date stamp = new Date();

        try
        {
            MungeException ce = null;
            try
            {
                cfg.parseCommandLine(args);
            }
            catch (MungeException e)
            {
                ce = e; // configuration exception
            }

            // setup the logger based on configuration and/or defaults
            if (cfg.getLogFilename().length() < 1)
                cfg.setLogFilename("els.log"); // make sure there's a filename
            if (cfg.isLogOverwrite()) // optionally delete any existing log
            {
                File aLog = new File(cfg.getLogFilename());
                aLog.delete();
            }
            System.setProperty("logFilename", cfg.getLogFilename());
            System.setProperty("consoleLevel", cfg.getConsoleLevel());
            System.setProperty("debugLevel", cfg.getDebugLevel());
            System.setProperty("pattern", cfg.getPattern());
            org.apache.logging.log4j.core.LoggerContext lctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            lctx.reconfigure();
            org.apache.logging.log4j.core.config.Configuration ccfg = lctx.getConfiguration();
            LoggerConfig lcfg = ccfg.getLoggerConfig("Console");
            lcfg.setLevel(Level.toLevel(cfg.getConsoleLevel()));
            lcfg = ccfg.getLoggerConfig("applog");
            lcfg.setLevel(Level.toLevel(cfg.getDebugLevel()));
            lctx.updateLoggers();

            // get the named logger
            logger = LogManager.getLogger("applog");

            if (ce != null) // re-throw any configuration exception
                throw ce;

            // TODO
            //   * Add "locations" to the example publisher and subscriber files.
            //   * Fix docs for -T | -T behavior changes.
            //   * Add Javadoc to all methods

            // an execution of this program can only be configured as one of these
            logger.info("+------------------------------------------");
            switch (cfg.getRemoteFlag())
            {
                // handle standard local execution, no -r option
                case NOT_REMOTE:
                    logger.info("ELS Local Process begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    if (!cfg.isValidation() &&
                            (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                                    cfg.getSubscriberCollectionFilename().length() > 0))
                    {
                        context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                    }
                    else if (cfg.isTargetsEnabled())
                    {
                        context.subscriberRepo = context.publisherRepo; // v3.00 for publisher ELS Hints
                    }

                    // the Process class handles the ELS process
                    proc = new Process(cfg, context);
                    returnValue = proc.process();
                    break;

                // handle -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    logger.info("ELS Publisher Listener begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start servers for -r T & clients for get command in stty.publisher.Daemon
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("PServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, true);
                        context.serveStty.startListening(context.publisherRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.publisherRepo, context.subscriberRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // handle -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    logger.info("ELS Publisher Manual Terminal begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true, true);
                        if (context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungeException("Publisher manual console failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp client failed to connect");
                        }
                    }
                    break;

                // handle -r P execute the automated process to remote subscriber -r S
                case PUBLISH_REMOTE:
                    logger.info("ELS Publish Process to Remote Subscriber begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

                        // start the serveStty client for automation
                        context.clientStty = new ClientStty(cfg, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungeException("Publisher remote failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp client failed to connect");
                        }

                        // the Process class handles the ELS process
                        proc = new Process(cfg, context);
                        returnValue = proc.process();
                    }
                    else
                    {
                        throw new MungeException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // handle -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    logger.info("ELS Subscriber Listener begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t | -T required");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start servers
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.subscriberRepo);

                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, true);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.subscriberRepo, context.publisherRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("Subscriber and publisher options are required for -r S");
                    }
                    break;

                // handle -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    logger.info("ELS Subscriber Manual Terminal begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t | -T required");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients & servers for -r L for get command
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true, true);
                        if (context.clientStty.connect(context.subscriberRepo, context.publisherRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungeException("Subscriber terminal console failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.subscriberRepo, context.publisherRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp client failed to connect");
                        }

                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, false);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.subscriberRepo, context.publisherRepo, false);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("A subscriber -s or -S file and publisher -p or -P) is required for -r T");
                    }
                    break;

                // handle -H | --hint-server stand-alone hints status server
                case STATUS_SERVER:
                    logger.info("ELS Hint Status Server begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    if (cfg.getHintKeysFile() == null || cfg.getHintKeysFile().length() == 0)
                        throw new MungeException("-H | --status-server requires a -k | -K hint keys file");

                    if (cfg.getPublisherLibrariesFileName().length() > 0 || cfg.getPublisherCollectionFilename().length() > 0)
                        throw new MungeException("-H | --status-server does not use -p | -P");

                    if (cfg.getSubscriberLibrariesFileName().length() > 0 || cfg.getSubscriberCollectionFilename().length() > 0)
                        throw new MungeException("-H | --status-server does not use -s | -S");

                    if (cfg.isTargetsEnabled())
                        throw new MungeException("-H | --status-server does not use targets");

                    // Get the hint status server repo
                    context.statusRepo = new Repository(cfg);
                    context.statusRepo.read(cfg.getHintsDaemonFilename());

                    // Get ELS hints keys if specified
                    context.hintKeys = new HintKeys(cfg, context);
                    context.hintKeys.read(cfg.getHintKeysFile());

                    // Setup the hint status store
                    context.hintStore = new Store(cfg, context);
                    context.hintStore.initialize();

                    // start server
                    if (context.statusRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, true);
                        context.serveStty.startListening(context.statusRepo);
                        isListening = true;
                    }
                    else
                    {
                        throw new MungeException("Error initializing from hint status server JSON file");
                    }
                    break;

                // handle -q | --quit-status shutdown remote stand-alone hints status server
                case STATUS_SHUTDOWN:
                    logger.info("ELS Hint Status Server shutdown begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    if (cfg.getSubscriberLibrariesFileName().length() > 0 || cfg.getSubscriberCollectionFilename().length() > 0)
                        throw new MungeException("-q | --quit-status does not use -s | -S");

                    if (cfg.isTargetsEnabled())
                        throw new MungeException("-q | --quit-status does not use targets");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    connectHintServer(context.publisherRepo);
                    if (context.statusStty != null)
                    {
                        logger.info("Sending command to shutdown hint status server to: " + context.statusRepo.getLibraryData().libraries.description);
                        context.statusStty.send("quit");
                        logger.fatal("Process completed normally");
                    }
                    else
                        logger.info("Could not connect to hint status server " + context.statusRepo.getLibraryData().libraries.description);
                    break;

                default:
                    throw new MungeException("Unknown type of execution");
            }

        }
        catch (Exception e)
        {
            if (logger != null)
            {
                logger.error(e.getMessage());
            }
            else
            {
                System.out.println(e.getMessage());
            }
            isListening = false; // force stop
            returnValue = 1;
        }
        finally
        {
            if (!isListening)
            {
                stopServices();
                Date done = new Date();
                long millis = Math.abs(done.getTime() - stamp.getTime());
                logger.fatal("Runtime: " + Utils.getDuration(millis));
            }
            else
            {
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            if (context.clientStty != null)
                            {
                                context.clientStty.disconnect();
                            }

                            Date done = new Date();
                            long millis = Math.abs(done.getTime() - stamp.getTime());
                            logger.fatal("Runtime: " + Utils.getDuration(millis));

                            logger.info("Stopping ELS services");
                            Thread.sleep(10000L);
                            stopServices();
                        }
                        catch (Exception e)
                        {
                            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
                        }
                    }
                });
            }
        }

        return returnValue;
    } // process

    /**
     * Read either publisher or subscriber repository
     *
     * @param cfg         Loaded configuration
     * @param isPublisher Is this the publisher? true/false
     * @param validate    Validate repository against actual directories and files true/false
     * @return Repository object
     * @throws Exception
     */
    private Repository readRepo(Configuration cfg, boolean isPublisher, boolean validate) throws Exception
    {
        Repository repo = new Repository(cfg);
        if (isPublisher)
        {
            if (cfg.getPublisherLibrariesFileName().length() > 0 &&                     // both
                    cfg.getPublisherCollectionFilename().length() > 0)
            {
                throw new MungeException("Cannot use both -p and -P");
            }
            else if (cfg.getPublisherLibrariesFileName().length() == 0 &&               // neither
                    cfg.getPublisherCollectionFilename().length() == 0)
            {
                if (cfg.isRemoteSession())
                {
                    throw new MungeException("A -p publisher library or -P collection file is required for -r P");
                }
                else
                {
                    throw new MungeException("A -p publisher library or -P collection file is required, or the filename missing from -p or -P");
                }
            }

            // get -p Publisher libraries
            if (cfg.getPublisherLibrariesFileName().length() > 0)
            {
                repo.read(cfg.getPublisherLibrariesFileName());

            }
            // get -P Publisher collection
            if (cfg.getPublisherCollectionFilename().length() > 0)
            {
                repo.read(cfg.getPublisherCollectionFilename());
            }
        }
        else
        {
            if (cfg.getSubscriberLibrariesFileName().length() > 0 &&                    // both
                    cfg.getSubscriberCollectionFilename().length() > 0)
            {
                throw new MungeException("Cannot use both -s and -S");
            }
            else if (cfg.getSubscriberLibrariesFileName().length() == 0 &&              // neither
                    cfg.getSubscriberCollectionFilename().length() == 0)
            {
                if (cfg.isRemoteSession())
                {
                    throw new MungeException("A -s subscriber library or -S collection file is required for -r S");
                }
                else
                {
                    if (cfg.isPublishOperation())
                    {
                        throw new MungeException("A -s subscriber library or -S collection file is required, or the filename missing for -s or -S");
                    }
                    return null;
                }
            }

            // get -s Subscriber libraries
            if (cfg.getSubscriberLibrariesFileName().length() > 0)
            {
                repo.read(cfg.getSubscriberLibrariesFileName());
            }

            // get -S Subscriber collection
            if (cfg.getSubscriberCollectionFilename().length() > 0)
            {
                repo.read(cfg.getSubscriberCollectionFilename());
            }
        }

        // -v | --validate option
        if (validate && repo.isInitialized())
        {
            repo.validate();
        }

        return repo;
    }

    /**
     * Stop all service that are in use
     */
    public void stopServices()
    {
        // logout from any hint status server if not shutting it down
        if (context.statusStty != null && cfg.getRemoteFlag() != STATUS_SHUTDOWN)
        {
            try
            {
                context.statusStty.send("logout");
            }
            catch (Exception e)
            {
                logger.info(Utils.getStackTrace(e));
            }
        }
        if (context.clientStty != null)
        {
            context.clientStty.disconnect();
        }
        if (context.serveStty != null)
        {
            context.serveStty.stopServer();
        }
        if (context.clientSftp != null)
        {
            context.clientSftp.stopClient();
        }
        if (context.serveSftp != null)
        {
            context.serveSftp.stopServer();
        }
    }

} // Main
