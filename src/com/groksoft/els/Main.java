package com.groksoft.els;

import com.groksoft.els.gui.Navigator;
import com.groksoft.els.gui.Preferences;
import com.groksoft.els.gui.util.GuiLogAppender;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hintServer.Datastore;

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

import static com.groksoft.els.Configuration.*;

/**
 * ELS main program
 */
public class Main
{
    public Main main;
    public boolean navigatorSession = false;
    public Context context = new Context();
    private boolean isListening = false; // listener mode
    public String localeAbbrev; // abbreviation of locale, e.g. en_US
    public Logger logger = null; // log4j2 logger singleton
    public String operationName = ""; // secondaryInvocation name
    public Context previousContext = null; // the previous Context during a secondaryInvocation
    public Date stamp = new Date(); // runtime stamp for this invocation
    public boolean secondaryInvocation = false;
    public boolean secondaryNavigator = false;

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

    /**
     * Connect to or setup hint tracking, connect to hint server if specified
     * <br/>
     * Will connect to a Hint Server, if specified, or local Hint Tracker,
     * if specified, or Hint keys for creating basic Hint files, if specified.
     * If none of those things are defined in the configuration this method
     * simply returns.
     *
     * @param repo The Repository that is connecting to the tracker/server
     * @throws Exception Configuration and connection exceptions
     */
    public void connectHintServer(Repository repo) throws Exception
    {
        if (context.cfg.isUsingHintTracking() && repo != null)
        {
            context.statusRepo = new Repository(context, Repository.HINT_SERVER);
            if (context.cfg.getHintsDaemonFilename().length() > 0)
            {
                context.statusRepo.read(context.cfg.getHintsDaemonFilename(), true);

                // start the serveStty client to the hints status server
                context.statusStty = new ClientStty(context, false, true);
                if (!context.statusStty.connect(repo, context.statusRepo))
                {
                    throw new MungeException("Hint Status Server " + context.statusRepo.getLibraryData().libraries.description + " failed to connect");
                }
                String response = context.statusStty.receive("", 5000); // check the initial prompt
                if (!response.startsWith("CMD"))
                    throw new MungeException("Bad initial response from status server: " + context.statusRepo.getLibraryData().libraries.description);
            }
            else
            {
                context.statusRepo.read(context.cfg.getHintTrackerFilename(), true);

                // Setup the hint status store, single instance
                context.datastore = new Datastore(context);
                context.datastore.initialize();
            }
        }
        else
            // Validate ELS hints keys if specified
            if (context.cfg.getHintKeysFile().length() > 0) 
            {
                context.hintKeys = new HintKeys(context);
                context.hintKeys.read(context.cfg.getHintKeysFile());
            }
    }

    public GuiLogAppender getGuiLogAppender()
    {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        AbstractConfiguration loggerContextConfiguration = (AbstractConfiguration) loggerContext.getConfiguration();
        LoggerConfig loggerConfig = loggerContextConfiguration.getLoggerConfig("applog");
        Map<String, Appender> appenders = loggerConfig.getAppenders();
        GuiLogAppender appender = (GuiLogAppender) appenders.get("GuiLogAppender");
        return appender;
    }

    /**
     * Get the path as relative to the current working path if possible.
     * <br/>
     * If the path starts with the current working path it is shortened
     * to be relative to the current working path. Otherwise the path
     * is returned.
     * @param path Path to check for relativity to the current working path
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
                GuiLogAppender appender = getGuiLogAppender();
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

            // re-throw any configuration exception
            if (cfgException != null)
                throw cfgException;

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

            //
            // an execution of this program can only be configured as one of these operations
            //
            logger.info("+------------------------------------------");
            switch (context.cfg.getOperation())
            {
                // --- local execution, no -r|--remote option
                case NOT_REMOTE:
                    if (context.cfg.getPublisherFilename().length() == 0 && context.cfg.getSubscriberFilename().length() == 0)
                        context.cfg.defaultNavigator = true;

                    // handle -n|--navigator to display the Navigator
                    if (context.cfg.defaultNavigator || context.cfg.isNavigator())
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

                        if (context.cfg.defaultNavigator && !context.cfg.isNavigator())
                        {
                            context.cfg.setNavigator(true);
                            logger.warn("Publisher, subscriber and mode not defined. Defaulting to Navigator");
                        }

                        // setup the hint status server if defined
                        connectHintServer(context.publisherRepo);

                        context.navigator = new Navigator(main, context);
                        if (!context.fault)
                        {
                            context.navigator.run();
                            navigatorSession = true;
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
                        connectHintServer(context.publisherRepo);

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
                        connectHintServer(context.publisherRepo);

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
                        connectHintServer(context.publisherRepo);

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
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        connectHintServer(context.publisherRepo);

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
                                navigatorSession = true;
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
                        connectHintServer(context.subscriberRepo);

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
                        connectHintServer(context.subscriberRepo);

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

                    if (context.cfg.getPublisherFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -p|-P");

                    if (context.cfg.getSubscriberFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -s|-S");

                    if (context.cfg.isTargetsEnabled())
                        throw new MungeException("-H|--status-server does not use targets");

                    // Get the hint status server repo
                    context.statusRepo = new Repository(context, Repository.HINT_SERVER);
                    context.statusRepo.read(context.cfg.getHintsDaemonFilename(), true);

                    // Get ELS hints keys
                    context.hintKeys = new HintKeys(context);
                    context.hintKeys.read(context.cfg.getHintKeysFile());

                    // Setup the hint status store, single instance
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

                    connectHintServer(context.publisherRepo);

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
                    connectHintServer(context.publisherRepo);

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

            if (logger != null)
            {
                logger.error(Utils.getStackTrace(e));
            }
            else
            {
                System.out.println(Utils.getStackTrace(e));
            }

            if (context.cfg.isNavigator() || context.cfg.defaultNavigator)
            {
                Component centerOn = null;
                if (context.mainFrame != null)
                    centerOn = context.mainFrame;
                else
                {
                    GuiLogAppender appender = getGuiLogAppender();
                    if (appender.isStartupActive())
                        centerOn = appender.getStartup();
                }
                JOptionPane.showMessageDialog(centerOn, e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                            logger.info(context.cfg.gs("Main.disconnecting.and.shutting.down"));

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
                Thread.sleep(1500);
            }
            catch (Exception e)
            {}
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
     * @param context     The Context
     * @param purpose     Is this the PUBLISHER, SUBSCRIBER or HINT_SERVER
     * @param validate    Validate repository against actual directories and files true/false
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
                if (context.cfg.isRemoteSession())
                {
                    throw new MungeException("A -p publisher library or -P collection file is required for -r P");
                }
                else
                {
                    throw new MungeException("A -p publisher library or -P collection file is required, or the filename missing from -p or -P");
                }
            }

            // get Publisher data
            repo.read(context.cfg.getPublisherFilename(), true);
        }
        else
        {
            if (context.cfg.getSubscriberLibrariesFileName().length() > 0 &&                    // both
                    context.cfg.getSubscriberCollectionFilename().length() > 0)
            {
                throw new MungeException("Cannot use both -s and -S");
            }
            else if (context.cfg.getSubscriberLibrariesFileName().length() == 0 &&              // neither
                    context.cfg.getSubscriberCollectionFilename().length() == 0)
            {
                if (context.cfg.isRemoteSession())
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

            // get Subscriber data
            repo.read(context.cfg.getSubscriberFilename(), true);
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

        try
        {
            // logout from any hint status server if not shutting it down
            if (context.statusStty != null)
            {
                if (!context.cfg.isQuitStatusServer() && context.statusStty.isConnected())
                {
                    context.statusStty.send("bye", "Sending bye command to remote Hint Server");
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
