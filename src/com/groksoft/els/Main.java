package com.groksoft.els;

import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hintServer.Datastore;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;
import java.util.Date;

import static com.groksoft.els.Configuration.*;

/**
 * ELS main program.
 */
public class Main
{
    private static Main els;
    public boolean isListening = false;
    boolean fault = false;
    private Configuration cfg;
    private Context context = new Context();
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
        els = new Main();
        els.process(args);          // ELS Processor
    } // main

    /**
     * Connect to or setup hint tracking, connect to hint server if specified
     *
     * @param repo The Repository that is connecting to the tracker/server
     * @throws Exception Configuration and connection exceptions
     */
    private void connectHintServer(Repository repo) throws Exception
    {
        if (cfg.isUsingHintTracker())
        {
            context.statusRepo = new Repository(cfg);
            context.statusRepo.read(cfg.getStatusTrackerFilename());

            if (cfg.isRemoteSession())
            {
                // start the serveStty client to the hints status server
                context.statusStty = new ClientStty(cfg, false, true);
                if (!context.statusStty.connect(repo, context.statusRepo))
                {
                    throw new MungeException("Hint Status Server failed to connect");
                }
                String response = context.statusStty.receive(); // check the initial prompt
                if (!response.startsWith("CMD"))
                    throw new MungeException("Bad initial response from status server: " + context.statusRepo.getLibraryData().libraries.description);
            }
            else
            {
                // Setup the hint status store, single instance
                context.datastore = new Datastore(cfg, context);
                context.datastore.initialize();
            }
        }
    }

    /**
     * Execute the process
     *
     * @param args the input arguments
     * @return Return status
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
            MungeException cfgException = null;
            try
            {
                cfg.parseCommandLine(args);
            }
            catch (MungeException e)
            {
                cfgException = e; // configuration exception
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

            if (cfgException != null) // re-throw any configuration exception
                throw cfgException;

            //
            // an execution of this program can only be configured as one of these
            //
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

                    // setup the hint status server for local use if defined
                    connectHintServer(context.publisherRepo);

                    // the Process class handles the ELS process
                    proc = new Process(cfg, context);
                    fault = proc.process();
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
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

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
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

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
                        fault = proc.process();
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
                        // connect to the hint status server if defined
                        connectHintServer(context.subscriberRepo);

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

                    // Setup the hint status store, single instance
                    context.datastore = new Datastore(cfg, context);
                    context.datastore.initialize();

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

                // handle -Q | --force-quit the hint status server remotely
                case STATUS_SERVER_FORCE_QUIT:
                    logger.info("ELS Quit Hint Status Server begin, version " + cfg.getProgramVersion());
                    cfg.dump();

                    if (cfg.getStatusTrackerFilename() == null || cfg.getStatusTrackerFilename().length() == 0)
                        throw new MungeException("-Q | --force-quit requires a -h | --hints hint server JSON file");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);

                    connectHintServer(context.publisherRepo);

                    // force the cfg setting & let this process end normally
                    // that will send the quit command to the hint status server
                    cfg.setQuitStatusServer(true);
                    break;

                default:
                    throw new MungeException("Unknown type of execution");
            }

        }
        catch (Exception e)
        {
            fault = true;
            if (logger != null)
            {
                logger.error(Utils.getStackTrace(e));
            }
            else
            {
                System.out.println(Utils.getStackTrace(e));
            }
            isListening = false; // force stop
            returnValue = 1;
        }
        finally
        {
            // stop stuff
            if (!isListening) // clients
            {
                // optionally command status server to quit
                if (context.statusStty != null)
                    fault = context.statusStty.quitStatusServer(context, fault);  // do before stopping the necessary services

                // stop any remaining services
                stopServices();

                if (!cfg.getConsoleLevel().equalsIgnoreCase(cfg.getDebugLevel()))
                    logger.info("Log file has more details: " + cfg.getLogFilename());

                Date done = new Date();
                long millis = Math.abs(done.getTime() - stamp.getTime());
                logger.fatal("Runtime: " + Utils.getDuration(millis));

                if (!fault)
                    logger.fatal("Process completed normally");
                else
                    logger.fatal("Process failed");
            }
            else // daemons
            {
                // this shutdown hook is triggered when all connections and
                // threads used by the daemon have been closed and stopped
                // See ServeStty.run()
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            // optionally command status server to quit
                            if (els.context.statusStty != null)
                                els.fault = els.context.statusStty.quitStatusServer(context, els.fault);  // do before stopping the necessary services

                            if (!els.cfg.getConsoleLevel().equalsIgnoreCase(els.cfg.getDebugLevel()))
                                logger.info("Log file has more details: " + els.cfg.getLogFilename());

                            Date done = new Date();
                            long millis = Math.abs(done.getTime() - stamp.getTime());
                            logger.fatal("Runtime: " + Utils.getDuration(millis));

                            if (!els.fault)
                                logger.fatal("Process completed normally");
                            else
                                logger.fatal("Process failed");

                            Thread.sleep(4000L);

                            // stop any remaining services
                            stopServices(); // has to be last
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                        }
                    }
                });
            }
        }
        return returnValue;
    } // process

    /**
     * Read either a publisher or subscriber repository
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
        if (context.statusStty != null)
        {
            if (!cfg.isQuitStatusServer() && context.statusStty.isConnected())
            {
                try
                {
                    context.statusStty.send("logout");
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                }
            }

            context.statusStty.disconnect();
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
