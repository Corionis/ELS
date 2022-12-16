package com.groksoft.els;

import com.groksoft.els.gui.Navigator;
import com.groksoft.els.gui.SavedConfiguration;
import com.groksoft.els.jobs.Job;
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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.sshd.common.util.io.IoUtils;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.*;

import static com.groksoft.els.Configuration.*;

/**
 * ELS main program
 */
public class Main
{
    public static Main main;
    public final Context context = new Context();
    public Configuration cfg;
    public String currentFilePart;
    private boolean isListening = false;
    public Logger logger = null;
    public Date stamp = new Date();
    public SavedConfiguration savedConfiguration;
    public boolean trace;

    // add new locales here
    public String[] availableLocales = {"en_US"}; // Array of built-in locale names; TODO: Update locales here

    /*
        IDEA
            * Knob for "Show examples" then
                + Include, or not, files from examples directories
     */

    /**
     * Main application
     */
    public Main()
    {
        context.main = this;
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        main = new Main();
        main.process(args);          // ELS Processor
    }

    /**
     * Connect to or setup hint tracking, connect to hint server if specified
     *
     * @param repo The Repository that is connecting to the tracker/server
     * @throws Exception Configuration and connection exceptions
     */
    public void connectHintServer(Repository repo) throws Exception
    {
        if (cfg.isUsingHintTracker() && repo != null)
        {
            context.statusRepo = new Repository(cfg, Repository.HINT_SERVER);
            context.statusRepo.read(cfg.getStatusTrackerFilename(), true);

            if (cfg.isRemoteSession())
            {
                // start the serveStty client to the hints status server
                context.statusStty = new ClientStty(cfg, context, false, true);
                if (!context.statusStty.connect(repo, context.statusRepo))
                {
                    throw new MungeException("Hint Status Server failed to connect");
                }
                String response = context.statusStty.receive("", 1000); // check the initial prompt
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
        else
            // Validate ELS hints keys if specified
            if (cfg.getHintKeysFile().length() > 0) 
            {
                context.hintKeys = new HintKeys(cfg, context);
                context.hintKeys.read(cfg.getHintKeysFile());
            }
    }

    /**
     * Read the built-in build stamp
     *
     * @return Build stamp string
     */
    public String getBuildStamp()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("buildstamp.txt");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() > 0)
            {
                text += lines.get(0);
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        return text;
    }

    /**
     * Load a locale and set current locale bundle
     * <br/>
     * Requires the abbreviated language_country part of the locale filename, e.g. en_US.
     * It must be one of the built-in locale files. If not available en_US is used.
     *
     * @param filePart Locale file end
     */
    public void loadLocale(String filePart)
    {
        // load the language file if available
        if (!Arrays.asList(availableLocales).contains(filePart))
        {
            filePart = "en_US"; // default locale
        }
        cfg.setCurrentBundle(ResourceBundle.getBundle("com.groksoft.els.locales.bundle_" + filePart));
    }

    /**
     * Execute the process
     *
     * @param args the input arguments
     * @return Return status
     */
    public void process(String[] args)
    {
        ThreadGroup sessionThreads = null;
        Process proc;

        cfg = new Configuration(context);
        context.cfg = cfg;

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
            LoggerContext lctx = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);
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

            trace = cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? true : false;

            // Hack for viewing all system properties
            if (cfg.isDumpSystem())
            {
                System.out.println("\nDumping System Properties");
                System.getProperties().list(System.out);
                System.exit(1); // exit on dump of system properties
            }

            // attempt to load the language Java started with, default en_US
            Locale locale = Locale.getDefault();
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            String filePart = lang + "_" + country;
            loadLocale(filePart);
            if (cfg.gs("Transfer.received.subscriber.commands").length() == 0)
            {
                //logger.debug("Local locale not supported, loading default");
                loadLocale("-");
            }
            //else
                //logger.debug("Loaded locale: " + filePart);
            currentFilePart = filePart;

            Utils.setContext(context);

            //
            // an execution of this program can only be configured as one of these
            //
            logger.info("+------------------------------------------");
            boolean defaultNavigator = false;
            switch (cfg.getOperation())
            {
                // --- handle standard local execution, no -r|--remote option
                case NOT_REMOTE:
                    if (cfg.getPublisherFilename().length() == 0 && cfg.getSubscriberFilename().length() == 0)
                    {
                        cfg.setNavigator(true);
                        defaultNavigator = true;
                    }

                    // handle -n|--navigator to display the Navigator
                    if (cfg.isNavigator())
                    {
                        logger.info("ELS: Local Navigator, version " + cfg.getVersionStamp());
                        cfg.dump();

                        if (cfg.getPublisherFilename().length() > 0)
                        {
                            context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                        }

                        if (cfg.getSubscriberFilename().length() > 0)
                        {
                            context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                        }

                        if (defaultNavigator)
                            logger.warn("Publisher and subscriber not defined. Defaulting to the Navigator");

                        // setup the hint status server if defined
                        connectHintServer(context.publisherRepo);

                        context.navigator = new Navigator(this, cfg, context);
                        if (!context.fault)
                            context.navigator.run();
                    }
                    else
                    {
                        logger.info("ELS: Local Publish, version " + cfg.getVersionStamp());
                        cfg.dump();

                        context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                        if (!cfg.isValidation() && (cfg.getSubscriberFilename().length() > 0))
                        {
                            context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                        }
                        else if (cfg.isTargetsEnabled())
                        {
                            context.subscriberRepo = context.publisherRepo; 
                        }

                        // setup the hint status server for local use if defined
                        connectHintServer(context.publisherRepo);

                        // the Process class handles the ELS process
                        proc = new Process(cfg, context);
                        proc.process();
                    }
                    break;

                // --- handle -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    logger.info("ELS: Publisher Listener, version " + cfg.getVersionStamp());
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
                        context.serveSftp = new ServeSftp(context, context.publisherRepo, context.subscriberRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // --- handle -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    logger.info("ELS: Publisher Terminal, version " + cfg.getVersionStamp());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, context, true, true);
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
                        context.clientSftp = new ClientSftp(cfg, context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp client failed to connect");
                        }
                    }
                    break;

                // --- handle -r P execute the operations process to remote subscriber -r S
                case PUBLISH_REMOTE:
                    // handle -n|--navigator to display the Navigator
                    if (cfg.isNavigator())
                        logger.info("ELS: Remote Navigator, version " + cfg.getVersionStamp());
                    else
                        logger.info("ELS: Remote Publish, version " + cfg.getVersionStamp());

                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

                        // start the serveStty client for automation
                        context.clientStty = new ClientStty(cfg, context, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungeException("Remote subscriber failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(cfg, context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Subscriber sftp failed to connect");
                        }

                        // handle -n|--navigator to display the Navigator
                        if (cfg.isNavigator())
                        {
                            context.navigator = new Navigator(this, cfg, context);
                            if (!context.fault)
                                context.navigator.run();
                        }
                        else
                        {
                            // the Process class handles the ELS process
                            proc = new Process(cfg, context);
                            proc.process();
                        }
                    }
                    else
                    {
                        throw new MungeException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // --- handle -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    logger.info("ELS: Subscriber Listener, version " + cfg.getVersionStamp());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t|-T required");

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
                        context.serveSftp = new ServeSftp(context, context.subscriberRepo, context.publisherRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("Subscriber and publisher options are required for -r S");
                    }
                    break;

                // --- handle -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    logger.info("ELS: Subscriber Terminal, version " + cfg.getVersionStamp());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t|-T required");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients & servers for -r L for get command
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.subscriberRepo);

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, context, true, true);
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
                        context.clientSftp = new ClientSftp(cfg, context.subscriberRepo, context.publisherRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp failed to connect");
                        }

                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, false);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context, context.subscriberRepo, context.publisherRepo, false);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungeException("A subscriber -s or -S file and publisher -p or -P) is required for -r T");
                    }
                    break;

                // --- handle -H|--hint-server stand-alone hints status server
                case STATUS_SERVER:
                    logger.info("ELS: Hint Status Server, version " + cfg.getVersionStamp());
                    cfg.dump();

                    if (cfg.getHintKeysFile() == null || cfg.getHintKeysFile().length() == 0)
                        throw new MungeException("-H|--status-server requires a -k|-K hint keys file");

                    if (cfg.getPublisherFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -p|-P");

                    if (cfg.getSubscriberFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -s|-S");

                    if (cfg.isTargetsEnabled())
                        throw new MungeException("-H|--status-server does not use targets");

                    // Get the hint status server repo
                    context.statusRepo = new Repository(cfg, Repository.HINT_SERVER);
                    context.statusRepo.read(cfg.getHintsDaemonFilename(), true);

                    // Get ELS hints keys
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

                // --- handle -Q|--force-quit the hint status server remotely
                case STATUS_SERVER_FORCE_QUIT:
                    logger.info("ELS: Hint Status Server Quit, version " + cfg.getVersionStamp());
                    cfg.dump();

                    if (cfg.getStatusTrackerFilename() == null || cfg.getStatusTrackerFilename().length() == 0)
                        throw new MungeException("-Q|--force-quit requires a -h|--hints hint server JSON file");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE); // no need to validate for this

                    connectHintServer(context.publisherRepo);

                    // force the cfg setting & let this process end normally
                    // that will send the quit command to the hint status server
                    cfg.setQuitStatusServer(true);
                    break;

                // --- handle -G|--listener-quit the remote subscriber
                case SUBSCRIBER_SERVER_FORCE_QUIT:
                    logger.info("ELS: Subscriber Listener Quit, version " + cfg.getVersionStamp());
                    cfg.dump();

                    if (cfg.getSubscriberFilename() == null || cfg.getSubscriberFilename().length() == 0)
                        throw new MungeException("-G|--listener-quit requires a -s|-S subscriber JSON file");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE); // no need to validate for this
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start client
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client
                        context.clientStty = new ClientStty(cfg, context, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungeException("Remote subscriber failed to connect");
                        }
                        try
                        {
                            main.context.clientStty.roundTrip("quit", "Sending remote quit command", 1000);
                            Thread.sleep(1000);
                        }
                        catch (Exception e)
                        {
                            // ignore any exception
                        }
                    }
                    break;

                // --- handle -j|--job to execute a Job
                case JOB_PROCESS:
                    if (cfg.getRemoteType().equals("J"))
                        logger.info("ELS: Remote Job, version " + cfg.getVersionStamp());
                    else
                        logger.info("ELS: Local Job, version " + cfg.getVersionStamp());

                    cfg.dump();

                    if (cfg.isNavigator())
                        throw new MungeException("-j|--job and -n|--navigator are not used together");

                    if (cfg.getPublisherFilename().length() > 0)
                    {
                        context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    }

                    if (cfg.getSubscriberFilename().length() > 0)
                    {
                        context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                    }

                    // setup the hint status server if defined
                    connectHintServer(context.publisherRepo);

                    savedConfiguration = new SavedConfiguration(null, cfg, context);
                    context.transfer = new Transfer(cfg, context);
                    context.transfer.initialize();

                    // run the Job
                    Job tmpJob = new Job(cfg, context, "temp");
                    Job job = tmpJob.load(cfg.getJobName());
                    if (job == null)
                        throw new MungeException("Job \"" + cfg.getJobName() + "\" could not be loaded");
                    job.process(cfg, context);
                    break;

                default:
                    throw new MungeException("Unknown type of execution");
            }
        }
        catch (Exception e)
        {
            context.fault = true;
            if (logger != null)
            {
                logger.error(Utils.getStackTrace(e));
            }
            else
            {
                System.out.println(Utils.getStackTrace(e));
            }
            if (cfg.isNavigator())
                JOptionPane.showMessageDialog(null, e.getMessage(), cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            isListening = false; // force stop
        }
        finally
        {
            // stop stuff
            if (!isListening && !cfg.isNavigator()) // clients
            {
                // if a fault occurred tell any listener
                if (main.context.fault && main.context.clientStty != null && main.context.clientStty.isConnected())
                {
                    if (!main.context.timeout)
                    {
                        try
                        {
                            main.context.clientStty.roundTrip("fault", "Sending remote fault command (1)", 1000);
                        }
                        catch (Exception e)
                        {
                            // ignore any exception
                        }
                    }
                }

                // optionally command status server to quit
                if (context.statusStty != null)
                    context.statusStty.quitStatusServer(context);  // do before stopping the necessary services

                // stop any remaining services
                main.stopServices();
                main.stopVerbiage();
            }
            else if (isListening) // daemons
            {
                // this shutdown hook is triggered when all connections and
                // threads used by the daemon have been closed and stopped,
                // see ServeStty.run(). Also System.exit(0) triggers it and
                // is preferred over trying to determine which threads are
                // still alive and will block or hang
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            logger.trace("shutdown hook");

                            // optionally command status server to quit
                            if (main.context.statusStty != null)
                                main.context.statusStty.quitStatusServer(context);  // do before stopping the services

                            main.stopVerbiage();
                            main.stopServices(); // must be AFTER stopVerbiage()

                            // halt kills the remaining threads
                            if (main.context.fault)
                                logger.error("Exiting with error code");
                            Runtime.getRuntime().halt(main.context.fault ? 1 : 0);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            Runtime.getRuntime().halt(1);
                        }
                    }
                });
            }
        }

        if (main.context.fault)
        {
            logger.error("Exiting with error code");
            System.exit(1);
        }
    } // process

    /**
     * Read either a publisher or subscriber repository
     *
     * @param cfg         Loaded configuration
     * @param purpose     Is this the PUBLISHER, SUBSCRIBER or HINT_SERVER
     * @param validate    Validate repository against actual directories and files true/false
     * @return Repository object
     * @throws Exception
     */
    public Repository readRepo(Configuration cfg, int purpose, boolean validate) throws Exception
    {
        Repository repo = new Repository(cfg, purpose);
        if (purpose == Repository.PUBLISHER)
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

            // get Publisher data
            repo.read(cfg.getPublisherFilename(), true);
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

            // get Subscriber data
            repo.read(cfg.getSubscriberFilename(), true);
        }

        // -v|--validate option
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
        logger.trace("stopServices()");

        // logout from any hint status server if not shutting it down
        try
        {
            if (context.statusStty != null)
            {
                if (!cfg.isQuitStatusServer() && context.statusStty.isConnected())
                {
                    context.statusStty.send("bye", "Sending bye command to Hint Server");
                    Thread.sleep(1000);
                }
                context.statusStty.disconnect();
                context.statusStty = null;
            }
            if (context.clientSftp != null)
            {
                logger.trace("  sftp client");
                context.clientSftp.stopClient();
                context.clientSftp = null;
                Thread.sleep(2000L);
            }
            if (context.serveSftp != null)
            {
                logger.trace("  sftp server");
                context.serveSftp.stopServer();
                context.serveSftp = null;
            }
            if (context.clientStty != null)
            {
                logger.trace("  stty client");
                context.clientStty.disconnect();
                context.clientStty = null;
            }
            if (context.serveStty != null)
            {
                logger.trace("  stty server");
                context.serveStty.stopServer();
                context.serveStty = null;
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    /**
     * Log completion statistics
     */
    public void stopVerbiage()
    {
        if (!main.cfg.getConsoleLevel().equalsIgnoreCase(main.cfg.getDebugLevel()))
            main.logger.info("Log file has more details: " + main.cfg.getLogFilename());

        Date done = new Date();
        long millis = Math.abs(done.getTime() - main.stamp.getTime());
        main.logger.fatal("Runtime: " + Utils.getDuration(millis));

        if (!main.context.fault)
            main.logger.fatal("Process completed normally");
        else
            main.logger.fatal("Process failed");
    }

}
