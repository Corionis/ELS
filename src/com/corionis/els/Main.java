package com.corionis.els;

import com.corionis.els.gui.Navigator;
import com.corionis.els.gui.Preferences;
import com.corionis.els.gui.util.GuiLogAppender;
import com.corionis.els.jobs.Job;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.hints.Hints;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.sftp.ServeSftp;
import com.corionis.els.stty.ClientStty;
import com.corionis.els.stty.ServeStty;
import com.corionis.els.stty.hintServer.Datastore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;

import static com.corionis.els.Configuration.*;

/**
 * ELS main program
 */
public class Main
{
    public Context context = new Context();
    public String localeAbbrev; // abbreviation of locale, e.g. en_US
    public Logger logger = null; // log4j2 logger singleton
    public Main main;
    public String operationName = ""; // secondaryInvocation name
    public Context previousContext = null; // the previous Context during a secondaryInvocation
    public boolean secondaryInvocation = false;
    public boolean secondaryNavigator = false;
    public Date stamp = new Date(); // runtime stamp for this invocation
    private GuiLogAppender appender = null;
    private boolean catchExceptions = true;
    private boolean isListening = false; // listener mode

    /**
     * Hide default constructor
     */
    private Main()
    {
    }

    /**
     * Main application command line constructor
     */
    public Main(String[] args)
    {
        main = this;
        this.context.main = this;
        this.secondaryInvocation = false;
        main.process(args);          // ELS Processor
    }

    /**
     * Main application Job for Operations task constructor
     */
    public Main(String[] args, Context context, String operationName)
    {
        main = this;
        this.context.main = this;
        this.secondaryInvocation = true;
        this.previousContext = context;
        this.operationName = operationName;
        main.process(args);          // ELS Processor
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        new Main(args);
    }

    public void checkEmptyArguments()
    {
        if (context.cfg.getPublisherFilename().length() == 0 && context.cfg.getSubscriberFilename().length() == 0 && !context.cfg.isStatusServer())
            context.cfg.setDefaultNavigator(true);

        if (context.cfg.isNavigator() && context.preferences.isUseLastPublisherSubscriber() &&
                (context.cfg.getOperation() == NOT_REMOTE || context.cfg.getOperation() == PUBLISH_REMOTE))
        {
            if (context.cfg.getPublisherFilename().length() == 0 && context.cfg.getSubscriberFilename().length() == 0)
            {
                if (context.preferences.isLastPublisherInUse())
                    context.cfg.setPublisherLibrariesFileName(context.preferences.getLastPublisherOpenFile());

                if (context.preferences.isLastSubscriberInUse())
                {
                    context.cfg.setSubscriberLibrariesFileName(context.preferences.getLastSubscriberOpenFile());
                    if (context.preferences.isLastSubscriberIsRemote() && context.cfg.getSubscriberFilename().length() > 0)
                    {
                        context.cfg.setRemoteType("P");
                    }
                }

                // hint keys
                if (context.preferences.isLastHintKeysInUse() && context.cfg.getHintKeysFile().length() == 0 &&
                        context.preferences.getLastHintKeysOpenFile().length() > 0)
                {
                    context.cfg.setHintKeysFile(context.preferences.getLastHintKeysOpenFile());

                    // hint tracking, must have hint keys
                    if (context.preferences.isLastHintTrackingInUse() && context.cfg.getHintHandlerFilename().length() == 0 &&
                            context.preferences.getLastHintTrackingOpenFile().length() > 0)
                    {
                        // hint daemon or tracker?
                        if (context.preferences.isLastHintTrackingIsRemote())
                            context.cfg.setHintsDaemonFilename(context.preferences.getLastHintTrackingOpenFile());
                        else
                            context.cfg.setHintTrackerFilename(context.preferences.getLastHintTrackingOpenFile());
                    }
                }
            }
        }
    }

    public GuiLogAppender getGuiLogAppender()
    {
        if (appender == null)
        {
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            AbstractConfiguration loggerContextConfiguration = (AbstractConfiguration) loggerContext.getConfiguration();
            LoggerConfig loggerConfig = loggerContextConfiguration.getLoggerConfig("applog");
            Map<String, Appender> appenders = loggerConfig.getAppenders();
            appender = (GuiLogAppender) appenders.get("GuiLogAppender");
        }
        return appender;
    }

    /**
     * Get the path as relative to the current working path if possible.
     * <br/>
     * If the path starts with the current working path it is shortened
     * to be relative to the current working path. Otherwise the path
     * is returned.
     *
     * @param path        Path to check for relativity to the current working path
     * @param osSeparator true = use local OS separator, otherwise Linux /
     * @return String Path possibly shortened to be relative
     */
    public String getWorkingDirectoryRelative(String path, boolean osSeparator)
    {
        if (path != null && path.length() > 0 && path.startsWith(context.cfg.getWorkingDirectory()))
        {
            if (path.length() >= context.cfg.getWorkingDirectory().length() + 1)
                path = path.substring(context.cfg.getWorkingDirectory().length() + 1);
            else
                path = "";
        }
        // normalize path separator
        if (path != null)
        {
            path = Utils.pipe(path);
            if (osSeparator)
            {
                if (Utils.getOS().toLowerCase().equals("windows"))
                    path = Utils.unpipe(path, "\\\\");
                else
                    path = Utils.unpipe(path, "/");
            }
            else
                path = Utils.unpipe(path, "/");
        }
        return path;
    }

    public boolean isStartupActive()
    {
        if (appender != null && appender.isStartupActive())
            return true;
        return false;
    }

    public String makeRelativeWorkingPath(String path)
    {
        if (path != null && path.length() > 0)
        {
            path = getWorkingDirectoryRelative(path, false);
            path = Utils.pipe(path);
            path = Utils.unpipe(path, "/");
        }
        else
            path = "";
        return path;
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
        Process process;

        context.cfg = new Configuration(context);

        try
        {
            MungeException cfgException = null;
            try
            {
                context.cfg.parseCommandLine(args);
            }
            catch (MungeException e)
            {
                cfgException = e; // configuration exception
            }

            // setup the working directory & logger - once
            if (!secondaryInvocation)
            {
                context.cfg.configure(); // configure working directory & log path
                if (context.cfg.isLogOverwrite()) // optionally delete any existing log
                {
                    File delLog = new File(context.cfg.getLogFileFullPath());
                    if (delLog.exists())
                        delLog.delete();
                }
                System.setProperty("logFilename", context.cfg.getLogFileFullPath());
                System.setProperty("consoleLevel", context.cfg.getConsoleLevel());
                System.setProperty("debugLevel", context.cfg.getDebugLevel());
                System.setProperty("pattern", context.cfg.getPattern());
                LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false); //context.navigator == null ? true : false);
                //LoggerContext loggerContext = (LoggerContext) LogManager.getContext(context.navigator == null ? true : false);
                loggerContext.reconfigure();
                appender = getGuiLogAppender();
                appender.setContext(context);
                loggerContext.updateLoggers();
            }
            else // carry-over selected previous Context values
            {
                context.cfg.setConsoleLevel(previousContext.cfg.getConsoleLevel());
                context.cfg.setDebugLevel(previousContext.cfg.getDebugLevel());
                context.cfg.setLogFileName(previousContext.cfg.getLogFileName());
                context.cfg.setLogFilePath(previousContext.cfg.getLogFilePath());
                context.cfg.setLogFileFullPath(previousContext.cfg.getLogFileFullPath());
                context.cfg.setLogOverwrite(previousContext.cfg.isLogOverwrite());
                context.cfg.setWorkingDirectory(previousContext.cfg.getWorkingDirectory());
            }

            // get the named logger
            logger = LogManager.getLogger("applog");
            context.trace = context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? true : false;

            context.preferences = new Preferences();
            Utils.readPreferences(context);
            context.preferences.setContext(context);

            // attempt to load the language Java started with, default en_US
            Locale locale = Locale.getDefault();
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            String filePart = lang + "_" + country;
            context.cfg.loadLocale(filePart);
            if (context.cfg.gs("Transfer.received.subscriber.commands").length() == 0)
            {
                logger.trace("local locale not supported, loading default");
                context.cfg.loadLocale("-");
            }
            else
                //logger.trace("loaded locale: " + filePart);
                localeAbbrev = filePart;

            // re-throw any configuration exception
            if (cfgException != null)
                throw cfgException;

            // use preferences for empty publisher/subscriber/hint server arguments for Navigator
            checkEmptyArguments();

            //
            // an execution of this program can only be configured as one of these operations
            //
            logger.info("+------------------------------------------");
            switch (context.cfg.getOperation())
            {
                // --- local execution, no -r|--remote option
                case NOT_REMOTE:
                    // handle -n|--navigator to display the Navigator
                    if (context.cfg.isNavigator())
                    {
                        logger.info("ELS: Local Navigator, version " + getBuildVersionName() + ", " + getBuildDate());
                        context.cfg.dump();

                        if (context.cfg.getPublisherFilename().length() > 0)
                        {
                            context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                        }

                        if (context.cfg.getSubscriberFilename().length() > 0)
                        {
                            context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                        }

                        // setup the hint status server if defined
                        setupHints(context.publisherRepo);

                        context.navigator = new Navigator(main, context);
                        if (!context.fault)
                        {
                            context.navigator.run();
                        }
                    }
                    else
                    {
                        logger.info("ELS: Local Publish, version " + getBuildVersionName() + ", " + getBuildDate());
                        context.cfg.dump();

                        context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                        if (!context.cfg.isValidation() && (context.cfg.getSubscriberFilename().length() > 0))
                        {
                            context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                        }
                        else if (context.cfg.isTargetsEnabled())
                        {
                            context.subscriberRepo = context.publisherRepo;
                        }

                        // setup the hint status server for local use if defined
                        setupHints(context.publisherRepo);

                        // the Process class handles the ELS process
                        process = new Process(context);
                        process.process();
                    }
                    break;

                // --- -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    logger.info("ELS: Publisher Listener, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start servers for -r T & clients for get command in stty.publisher.Daemon
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.publisherRepo);

                        // start serveStty server
                        sessionThreads = new ThreadGroup("publisher.listener");
                        context.serveStty = new ServeStty(sessionThreads, 10, context.cfg, context, true);
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

                // --- -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    logger.info("ELS: Publisher Terminal, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.publisherRepo);

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(context, true, true);
                        if (context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            context.clientStty.terminalSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungeException("Publisher manual console to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp client to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                        }
                    }
                    break;

                // --- -r P execute the operation process to remote subscriber -r S
                case PUBLISH_REMOTE:
                    // handle -n|--navigator to display the Navigator
                    if (context.cfg.isNavigator())
                        logger.info("ELS: Remote Navigator, version " + getBuildVersionName() + ", " + getBuildDate());
                    else
                        logger.info("ELS: Remote Publish, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.cfg.isNavigator() || (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized()))
                    {
                        // connect to the hint status server if defined
                        setupHints(context.publisherRepo);

                        // start the serveStty client for automation
                        context.clientStty = new ClientStty(context, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungeException("Remote subscriber " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Subscriber sftp to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                        }

                        // handle -n|--navigator to display the Navigator
                        if (context.cfg.isNavigator())
                        {
                            context.navigator = new Navigator(main, context);
                            if (!context.fault)
                            {
                                context.navigator.run();
                            }
                        }
                        else
                        {
                            // the Process class handles the ELS process
                            process = new Process(context);
                            process.process();
                        }
                    }
                    else
                    {
                        throw new MungeException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // --- -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    logger.info("ELS: Subscriber Listener, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (!context.cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t|-T required");

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start servers
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.subscriberRepo);

                        // start serveStty server
                        sessionThreads = new ThreadGroup("subscriber.listener");
                        context.serveStty = new ServeStty(sessionThreads, 10, context.cfg, context, true);
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

                // --- -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    logger.info("ELS: Subscriber Terminal, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.subscriberRepo);

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(context, true, true);
                        if (context.clientStty.connect(context.subscriberRepo, context.publisherRepo))
                        {
                            context.clientStty.terminalSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungeException("Subscriber terminal console to " + context.publisherRepo.getLibraryData().libraries.description + " failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context, context.subscriberRepo, context.publisherRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungeException("Publisher sftp to " + context.publisherRepo.getLibraryData().libraries.description + " failed to connect");
                        }

                        // start serveStty server
                        sessionThreads = new ThreadGroup("subscriber.terminal");
                        context.serveStty = new ServeStty(sessionThreads, 10, context.cfg, context, false);
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

                // --- -H|--hint-server stand-alone hint status server
                case STATUS_SERVER:
                    logger.info("ELS: Hint Status Server, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getHintKeysFile() == null || context.cfg.getHintKeysFile().length() == 0)
                        throw new MungeException("-H|--status-server requires a -k|-K hint keys file");

                    if (context.cfg.getHintsDaemonFilename() == null || context.cfg.getHintsDaemonFilename().length() == 0)
                        throw new MungeException("-H|--status-server requires Hint Server JSON file");

                    if (context.cfg.getPublisherFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -p|-P");

                    if (context.cfg.getSubscriberFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -s|-S");

                    if (context.cfg.isTargetsEnabled())
                        throw new MungeException("-H|--status-server does not use targets");

                    // Get Hint Keys
                    context.hintKeys = new HintKeys(context);
                    context.hintKeys.read(context.cfg.getHintKeysFile());

                    // Get the Hint Status Server repository
                    context.statusRepo = new Repository(context, Repository.HINT_SERVER);
                    context.statusRepo.read(context.cfg.getHintsDaemonFilename(), "Hint Status Server", true);

                    // Setup the Hint Status Server datastore, single instance
                    context.datastore = new Datastore(context);
                    context.datastore.initialize();

                    // start server
                    if (context.statusRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("hint.status.server");
                        context.serveStty = new ServeStty(sessionThreads, 10, context.cfg, context, true);
                        context.serveStty.startListening(context.statusRepo);
                        isListening = true;
                    }
                    else
                    {
                        throw new MungeException("Error initializing from hint status server JSON file");
                    }
                    break;

                // --- -Q|--force-quit the hint status server remotely
                case STATUS_SERVER_FORCE_QUIT:
                    logger.info("ELS: Hint Status Server Quit, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getHintHandlerFilename() == null || context.cfg.getHintHandlerFilename().length() == 0)
                        throw new MungeException("-Q|--force-quit requires a either -h|--hints or -H|--hint-server");

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE); // no need to validate for this

                    setupHints(context.publisherRepo);

                    // force the cfg setting & let this process end normally
                    // that will send the quit command to the hint status server
                    context.cfg.setQuitStatusServer(true);
                    break;

                // --- -G|--listener-quit the remote subscriber
                case SUBSCRIBER_LISTENER_FORCE_QUIT:
                    logger.info("ELS: Subscriber Listener Quit, version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getSubscriberFilename() == null || context.cfg.getSubscriberFilename().length() == 0)
                        throw new MungeException("-G|--listener-quit requires a -s|-S subscriber JSON file");

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE); // who we are
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE); // listener to quit

                    // start client
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client
                        context.clientStty = new ClientStty(context, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungeException("Remote subscriber " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                        }
                        try
                        {
                            main.context.clientStty.roundTrip("quit", "Sending remote quit command", 5000);
                            Thread.sleep(3000);
                        }
                        catch (Exception e)
                        {
                            // ignore any exception
                        }
                    }
                    break;

                // --- -j|--job to execute a Job
                case JOB_PROCESS:
                    logger.info("ELS: Job, version " + getBuildVersionName() + ", " + getBuildDate());

                    context.cfg.dump();

                    if (context.cfg.isNavigator())
                        throw new MungeException("-j|--job and -n|--navigator are not used together");

                    // optional arguments for support of Any Publisher/Subscriber
                    if (context.cfg.getPublisherFilename().length() > 0)
                    {
                        context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    }

                    if (context.cfg.getSubscriberFilename().length() > 0)
                    {
                        context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                    }

                    // setup the hint status server if defined
                    setupHints(context.publisherRepo);

                    context.transfer = new Transfer(context);
                    context.transfer.initialize();

                    // run the Job
                    Job tmpJob = new Job(context, "temp");
                    Job job = tmpJob.load(context.cfg.getJobName());
                    if (job == null)
                        throw new MungeException("Job \"" + context.cfg.getJobName() + "\" could not be loaded");
                    job.process(context);
                    break;

                default:
                    throw new MungeException("Unknown operation");
            }
        }
        catch (Exception e)
        {
            if (context.navigator == null) // if not running as an Operation
                context.fault = true;

            if (catchExceptions)
            {
                if (logger != null)
                {
                    logger.error(Utils.getStackTrace(e));
                }
                else
                {
                    System.out.println(Utils.getStackTrace(e));
                }

                if (context.cfg.isNavigator())
                {
                    Component centerOn = null;
                    if (context.mainFrame != null)
                        centerOn = context.mainFrame;
                    else
                    {
                        if (isStartupActive())
                            centerOn = appender.getStartup();
                    }
                    JOptionPane.showMessageDialog(centerOn, e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }

            isListening = false; // force stop
        }
        finally
        {
            // stop stuff
            if (!isListening && !context.cfg.isNavigator() && !context.nestedProcesses) // clients
            {
                // if a fault occurred tell any listener
                if (main.context.fault && main.context.clientStty != null && main.context.clientStty.isConnected())
                {
                    if (!main.context.timeout)
                    {
                        try
                        {
                            main.context.clientStty.roundTrip("fault", "Sending remote fault command (1)", 5000);
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
                    @Override
                    public void run()
                    {
                        try // also done in Connection.run()
                        {
                            logger.info(context.cfg.gs("Main.disconnecting"));

                            // optionally command status server to quit
                            if (main.context.statusStty != null)
                                main.context.statusStty.quitStatusServer(context);  // do before stopping the services

                            main.stopVerbiage();
                            main.stopServices(); // must be AFTER stopVerbiage()

                            // halt kills the remaining threads
                            if (main.context.fault)
                                logger.error("Exiting with error code");
                            if (!secondaryInvocation)
                            {
                                Runtime.getRuntime().halt(main.context.fault ? 1 : 0);
                            }
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            if (!secondaryInvocation)
                            {
                                Runtime.getRuntime().halt(1);
                            }
                        }
                    }
                });
                logger.trace("listener shutdown hook added");
            }
        }

        // is this a restarted Navigator instance after being updated?
        if (context.cfg.isNavigator() && (context.cfg.isUpdateSuccessful() || context.cfg.isUpdateFailed()))
        {
            try
            {
                // give the GUI time to come up
                Thread.sleep(4000);
            }
            catch (Exception e)
            {
            }
            String logFilename = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") +
                    "ELS_Updater" + System.getProperty("file.separator") + "ELS-Updater.log";
            String message = context.cfg.isUpdateSuccessful() ?
                    Configuration.PROGRAM_NAME + " " + context.cfg.gs("Navigator.updated") :
                    java.text.MessageFormat.format(context.cfg.gs("Navigator.update.failed"), logFilename);
            logger.info(message);
            Object[] opts = {context.cfg.gs("Z.ok")};
            JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update.status"),
                    JOptionPane.PLAIN_MESSAGE, context.cfg.isUpdateSuccessful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
        }

        if (main.context.fault)
        {
            logger.error("Exiting with error code");
            Runtime.getRuntime().halt(1);
        }
    } // process

    /**
     * Read either a publisher or subscriber repository
     *
     * @param context  The Context
     * @param purpose  Is this the PUBLISHER, SUBSCRIBER or HINT_SERVER
     * @param validate Validate repository against actual directories and files true/false
     * @return Repository object
     * @throws Exception
     */
    public Repository readRepo(Context context, int purpose, boolean validate) throws Exception
    {
        Repository repo = new Repository(context, purpose);
        if (purpose == Repository.PUBLISHER)
        {
            if (context.cfg.getPublisherLibrariesFileName().length() > 0 &&                     // both
                    context.cfg.getPublisherCollectionFilename().length() > 0)
            {
                throw new MungeException("Cannot use both -p and -P");
            }
            else if (context.cfg.getPublisherLibrariesFileName().length() == 0 &&               // neither
                    context.cfg.getPublisherCollectionFilename().length() == 0)
            {
                if (!context.cfg.isNavigator())
                {
                    if (context.cfg.isRemoteOperation())
                    {
                        throw new MungeException("A -p publisher library or -P collection file is required for -r P");
                    }
                    else
                    {
                        throw new MungeException("A -p publisher library or -P collection file is required, or the filename missing from -p or -P");
                    }
                }
                else
                    return null;
            }

            // get Publisher data
            repo.read(context.cfg.getPublisherFilename(), "Publisher", true);
        }
        else // is Repository.SUBSCRIBER
        {
            if (context.cfg.getSubscriberLibrariesFileName().length() > 0 &&                    // both
                    context.cfg.getSubscriberCollectionFilename().length() > 0)
            {
                throw new MungeException("Cannot use both -s and -S");
            }
            else if (context.cfg.getSubscriberLibrariesFileName().length() == 0 &&              // neither
                    context.cfg.getSubscriberCollectionFilename().length() == 0)
            {
                if (!context.cfg.isNavigator())
                {
                    if (context.cfg.isRemoteOperation())
                    {
                        throw new MungeException("A -s subscriber library or -S collection file is required for -r S");
                    }
                    else
                    {
                        if (context.cfg.isPublishOperation())
                        {
                            throw new MungeException("A -s subscriber library or -S collection file is required, or the filename missing for -s or -S");
                        }
                        return null;
                    }
                }
                else
                    return null;
            }

            // get Subscriber data
            repo.read(context.cfg.getSubscriberFilename(), "Subscriber", true);
        }

        // -v|--validate option
        if (validate && repo.isInitialized())
        {
            repo.validate();
        }

        return repo;
    }

    /**
     * Setup hint keys & tracking, connect to hint server if specified
     * <br/>
     * <br/>
     * Gets Hints Keys if specified. Hint Keys are required for Hint Tracking/Daemon.
     * Will connect to a Hint Server, if specified, or local Hint Tracker, if specified.
     * If none of those things are defined in the configuration this method simply returns.
     *
     * @param repo The Repository that is connecting to the tracker/server
     * @throws Exception Configuration and connection exceptions
     */
    public void setupHints(Repository repo) throws Exception
    {
        boolean keys = false;
        String msg = "";
        try
        {
            if (context.cfg.getHintKeysFile().length() > 0)
            {
                // Hints Keys
                keys = true;
                context.hintKeys = new HintKeys(context);
                msg = "Exception while reading Hint Keys: ";
                context.hintKeys.read(context.cfg.getHintKeysFile());
                context.hints = new Hints(context, context.hintKeys);
                if (context.cfg.isNavigator())
                    context.preferences.setLastHintKeysInUse(true);

                if (context.cfg.isUsingHintTracking())
                {
                    keys = false;
                    context.statusRepo = new Repository(context, Repository.HINT_SERVER);

                    // Remote Hint Status Server
                    if (context.cfg.getHintsDaemonFilename().length() > 0 && repo != null)
                    {
                        // exceptions handle by read()
                        catchExceptions = false;
                        if (context.cfg.isNavigator())
                            context.preferences.setLastHintTrackingInUse(false);

                        if (context.statusRepo.read(context.cfg.getHintsDaemonFilename(), "Hint Status Server", true))
                        {
                            catchExceptions = true;

                            // start the serveStty client connection the Hint Status Server
                            context.statusStty = new ClientStty(context, false, true);
                            if (!context.statusStty.connect(repo, context.statusRepo))
                            {
                                msg = "";
                                throw new MungeException("Hint Status Server: " + context.statusRepo.getLibraryData().libraries.description + " failed to connect");
                            }

                            String response = context.statusStty.receive("", 5000); // check the initial prompt
                            if (!response.startsWith("CMD"))
                            {
                                msg = "";
                                throw new MungeException("Bad initial response from Hint Status Server: " + context.statusRepo.getLibraryData().libraries.description);
                            }

                            if (context.cfg.isNavigator())
                                context.preferences.setLastHintTrackingInUse(true);
                        }
                        else
                        {
                            catchExceptions = true;
                            context.cfg.setHintsDaemonFilename("");
                        }
                    }
                    else // Local Hint Tracker
                    {
                        // exceptions handle by read()
                        catchExceptions = false;
                        if (context.statusRepo.read(context.cfg.getHintTrackerFilename(), "Hint Tracker", true))
                        {
                            // Setup the Hint Tracker datastore, single instance
                            context.datastore = new Datastore(context);
                            context.datastore.initialize();

                            if (context.cfg.isNavigator())
                                context.preferences.setLastHintTrackingInUse(true);
                        }
                        else
                        {
                            catchExceptions = true;
                            context.cfg.setHintTrackerFilename("");
                            if (context.cfg.isNavigator())
                                context.preferences.setLastHintTrackingInUse(false);
                        }
                    }
                }
                else
                {
                    if (context.cfg.isNavigator())
                        context.preferences.setLastHintTrackingInUse(false);
                }
            }
            else
            {
                // no Hint Keys, Daemon or Tracker
                if (context.cfg.isNavigator())
                {
                    context.preferences.setLastHintKeysInUse(false);
                    context.preferences.setLastHintTrackingInUse(false);
                }
            }
        }
        catch (Exception e)
        {
            if (catchExceptions)
            {
                logger.error(msg + " " + e.toString());

                context.cfg.setHintsDaemonFilename("");
                context.cfg.setHintTrackerFilename("");
                if (keys)
                    context.cfg.setHintKeysFile("");

                if (isStartupActive())
                {
                    if (msg.length() > 0)
                        msg += "<br/>" + e.toString();
                    else
                        msg = e.toString();

                    int opt = JOptionPane.showConfirmDialog(getGuiLogAppender().getStartup(),
                            "<html><body>" + msg + "<br/><br/>Continue?</body></html>",
                            context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION)
                    {
                        context.fault = false;
                        return;
                    }
                }
                catchExceptions = false;
            }
            throw new MungeException(msg);
        }
    }

    /**
     * Stop all service that are in use
     */
    public void stopServices()
    {
        logger.trace("stopServices()");

        try
        {
            // logout from any hint status server if not shutting it down
            if (context.statusStty != null)
            {
                if (!context.cfg.isQuitStatusServer() && context.statusStty.isConnected())
                {
                    context.statusStty.send("bye", "Sending bye command to remote Hint Status Server");
                    Thread.sleep(3000);
                }
                context.statusStty.disconnect();
                context.statusStty = null;
            }
            if (context.clientSftp != null)
            {
                logger.trace("  sftp client");
                context.clientSftp.stopClient();
                context.clientSftp = null;
                Thread.sleep(3000L);
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
        if (!main.context.cfg.getConsoleLevel().equalsIgnoreCase(context.cfg.getDebugLevel()))
            main.logger.info("log file has more details: " + context.cfg.getLogFileName());

        Date done = new Date();
        long millis = Math.abs(done.getTime() - stamp.getTime());
        main.logger.fatal("Runtime: " + Utils.getDuration(millis));

        if (!main.context.fault)
            main.logger.fatal("Process completed normally");
        else
            main.logger.fatal("Process failed");
    }

}
