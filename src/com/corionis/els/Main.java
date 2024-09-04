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

import com.corionis.els.tools.Tools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;

import static com.corionis.els.Configuration.*;

/**
 * ELS main program
 *
 * <p>ELS uses an embedded JRE from the OpenJDK project.<br/>
 * * https://openjdk.org/<br/>
 * * https://github.com/AdoptOpenJDK<br/>
 * * https://github.com/adoptium<br/>
 * * Current https://github.com/adoptium/temurin19-binaries/releases/tag/jdk-19.0.2%2B7<br/>
 */
public class Main
{
    public Context context;
    public String localeAbbrev; // abbreviation of locale, e.g. en_US
    public Logger logger = null; // log4j2 logger singleton
    public String operationName = ""; // secondary invocation name
    public Context previousContext = null; // the previous Context during a secondary invocation
    public boolean primaryExecution = true;
    public boolean secondaryNavigator = false;
    public Date stamp = new Date(); // runtime stamp for this invocation
    public String whatsRunning = "";

    private GuiLogAppender appender = null;
    private boolean catchExceptions = true;
    private boolean isListening = false; // listener mode
    private Job job = null;

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
        this.context = new Context();
        this.context.main = this;
        this.primaryExecution = true;
        this.previousContext = null;
        process(args);          // ELS Processor

        if (this.context.mainFrame == null && !this.context.cfg.isNavigator() && !this.isListening)
            System.exit(this.context.fault ? 1 : 0);
    }

    /**
     * Main application constructor for Jobs and Operations
     */
    public Main(String[] args, Context context, String operationName)
    {
        this.context = (Context) context.clone();
        this.context.main = this;
        this.primaryExecution = false;
        this.previousContext = context;
        this.operationName = operationName;
        process(args);          // ELS Processor
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
     * Check whether default values should be set from Preferences
     * <br/>
     * Rules: If this is a Navigator session; if no publisher and subscriber
     * have been specified; if "Use Last Pubisher/Subscriber" is enabled;
     * then set default arguments from the previous session.
     */
    private void checkEmptyArguments()
    {
        if (context.cfg.getPublisherFilename().length() == 0 &&
                context.cfg.getSubscriberFilename().length() == 0 &&
                context.cfg.getOperation() != JOB_PROCESS &&
                !context.cfg.isStatusServer())
            context.cfg.setDefaultNavigator(true);

        if (context.cfg.getOperation() != JOB_PROCESS && context.preferences.isUseLastPublisherSubscriber())
        {
            // operating as the Navigator desktop application?
            if (context.cfg.isNavigator() &&
                    (context.cfg.getOperation() == NOT_REMOTE || context.cfg.getOperation() == PUBLISH_REMOTE))
            {
                // no pub and sub?
                if (context.cfg.getPublisherFilename().length() == 0 && context.cfg.getSubscriberFilename().length() == 0)
                {
                    // use saved publisher?
                    if (context.preferences.isLastPublisherIsOpen() && context.preferences.getLastPublisherOpenPath().length() > 0)
                    {
                        if (context.preferences.isLastPublisherIsWorkstation())
                        {
                            context.cfg.setPublisherCollectionFilename("");
                            context.cfg.setPublisherLibrariesFileName(context.preferences.getLastPublisherOpenFile());
                        }
                        else
                        {
                            context.cfg.setPublisherCollectionFilename(context.preferences.getLastPublisherOpenFile());
                            context.cfg.setPublisherLibrariesFileName("");
                        }
                    }

                    // use saved subscriber?
                    if (context.preferences.isLastSubscriberIsOpen() && context.preferences.getLastSubscriberOpenFile().length() > 0)
                    {
                        context.cfg.setSubscriberLibrariesFileName(context.preferences.getLastSubscriberOpenFile());
                        if (context.preferences.isLastSubscriberIsRemote() && context.cfg.getSubscriberFilename().length() > 0)
                            context.cfg.setRemoteType("P");
                        context.cfg.setOverrideSubscriberHost(context.preferences.getLastOverrideSubscriber());
                    }

                    // use last hint keys?
                    if (context.preferences.isLastHintKeysIsOpen() &&
                            context.cfg.getHintKeysFile().length() == 0 &&
                            context.preferences.getLastHintKeysOpenFile().length() > 0)
                    {
                        context.cfg.setHintKeysFile(context.preferences.getLastHintKeysOpenFile());

                        // hint tracking, must have hint keys
                        if (context.preferences.isLastHintTrackingIsOpen() &&
                                context.cfg.getHintHandlerFilename().length() == 0 &&
                                context.preferences.getLastHintTrackingOpenFile().length() > 0)
                        {
                            // hint daemon or tracker?
                            if (context.preferences.isLastHintTrackingIsRemote())
                            {
                                context.cfg.setHintsDaemonFilename(context.preferences.getLastHintTrackingOpenFile());
                                context.cfg.setOverrideHintsHost(context.preferences.isLastOverrideHintHost());
                            }
                            else
                                context.cfg.setHintTrackerFilename(context.preferences.getLastHintTrackingOpenFile());
                        }
                    }
                }
            }
        }
    }

    /**
     * Check for the basic directory structure, create as needed
     * <p>
     * Call -after- localContext.cfg.configureWorkingDirectory
     */
    public void checkWorkingDirectories() throws Exception
    {
        String[] stdDirs = {"jobs", "libraries", "local", "system", "tools"};
        String working = context.cfg.getWorkingDirectory();
        for (int i = 0; i < stdDirs.length; ++i)
        {
            Path dir = Paths.get(working, stdDirs[i]);
            if (!Files.exists(dir))
                Files.createDirectories(dir);
        }

        String[] toolDirs = {"JunkRemover", "Operations", "Renamer", "Sleep"};
        for (int i = 0; i < toolDirs.length; ++i)
        {
            Path dir = Paths.get(working, "tools", toolDirs[i]);
            if (!Files.exists(dir))
                Files.createDirectories(dir);
        }
    }

    /**
     * Connect to remote Subscriber
     *
     * @return true if successful, otherwise false
     * @throws Exception Configuration and connection exceptions
     */
    public boolean connectSubscriber(boolean promptOnFailure) throws Exception
    {
        try
        {
            if (context.publisherRepo != null && context.subscriberRepo != null)
            {
                // start the clientStty
                context.clientStty = new ClientStty(context, false, true, false);
                if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                {
                    throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("Main.remote.subscriber.failed.to.connect"),
                            context.subscriberRepo.getLibraryData().libraries.description));
                }
            }
            else
            {
                throw new MungeException((context.publisherRepo == null) ? context.cfg.gs("Main.publisher.library.or.collection.file.is.required.for.remote.publish") :
                        ("Main.subscriber.library.or.collection.file.is.required.for.remote.publish"));
            }
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            logger.error(msg);

            context.clientStty = null;
            context.clientSftp = null;
            context.clientSftpMetadata = null;
            context.subscriberRepo = null;
            context.cfg.setOperation(NOT_REMOTE);
            context.cfg.setSubscriberCollectionFilename("");
            context.cfg.setSubscriberLibrariesFileName("");

            if (isStartupActive())
            {
                int opt = JOptionPane.showConfirmDialog(getGuiLogAppender().getStartup(),
                        "<html><body>" + msg + "<br/><br/>" + context.cfg.gs(("Main.continue")) + "</body></html>",
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                    context.fault = false;
                return false;
            }
        }
        return true;
    }

    /**
     * Decrypt a byte array to a string using provided key
     *
     * @param key       UUID key
     * @param encrypted Data to decrypt
     * @return String Decrypted texts
     */
    public synchronized String decrypt(String key, byte[] encrypted)
    {
        String output = "";
        try
        {
            // Create key and cipher
            key = key.replaceAll("-", "");
            if (key.length() > 16)
            {
                key = key.substring(0, 16);
            }
            //logger.trace("  decrypt with " + key + ", " + whatsRunning);  // todo comment out
            Key aesKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            // decrypt the text
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            output = new String(cipher.doFinal(encrypted));
            //logger.trace("  decrypted " + output.length() + " bytes" + ", " + whatsRunning);  // todo comment out
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return output;
    }

    /**
     * Encrypt a string using provided key
     *
     * @param key  UUID key
     * @param text Data to encrypt
     * @return byte[] of encrypted data
     */
    public synchronized byte[] encrypt(String key, String text)
    {
        byte[] encrypted = {};
        try
        {
            // Create key and cipher
            key = key.replaceAll("-", "");
            if (key.length() > 16)
            {
                key = key.substring(0, 16);
            }
            //logger.trace("  encrypt with " + key + ", " + text + ", " + whatsRunning); // todo comment out
            Key aesKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            // encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            //logger.trace("  encrypted " + text.getBytes().length + " bytes, " + whatsRunning);  // todo comment out
            encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return encrypted;
    }

    /**
     * Execute an external executable and monitor it's output and execution
     *
     * @param comp  Component performing action
     * @param cfg   The Configuration
     * @param parms Execution parameters
     * @return Success = true, else false
     */
    public boolean execExternalExe(Component comp, Configuration cfg, String[] parms)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");

        String cl = "";
        for (int i = 0; i < parms.length; ++i)
        {
            cl = cl + parms[i] + " ";
        }
        final String cmdline = cl;

        final int MAX_TRIES = 3;
        int tries = 0;
        while (tries < MAX_TRIES)
        {
            try
            {
                final java.lang.Process proc = Runtime.getRuntime().exec(parms);
                Thread thread = new Thread()
                {
                    public void run()
                    {
                        String line;
                        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                        try
                        {
                            while ((line = input.readLine()) != null)
                                logger.info(SIMPLE, line);
                            input.close();
                        }
                        catch (IOException e)
                        {
                            logger.error(cfg.gs("Z.process.failed") + cmdline + System.getProperty("line.separator") + Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(comp, cfg.gs("Z.exception") + Utils.getStackTrace(e), cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };

                // run it
                thread.start();
                int result = proc.waitFor();
                thread.join();
                if (result != 0)
                {
                    ++tries;
                    String message = cfg.gs("Z.process.failed") + result + ", : " + cmdline;
                    logger.error(message);
                    JOptionPane.showMessageDialog(comp, message, cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                else
                    return true;
            }
            catch (Exception e)
            {
                ++tries;
                logger.warn(cfg.gs("Z.exception") + parms[0] + ", #" + tries + ", " + e.getMessage());
                // give the OS a little more time
                try
                {
                    Thread.sleep(2000);
                }
                catch (Exception e1)
                {
                }
            }

            if (tries >= MAX_TRIES)
            {
                String message = cfg.gs("Z.process.failed") + cmdline;
                logger.error(message);
                JOptionPane.showMessageDialog(comp, message, cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    /**
     * Get the GuiLogAppender from log4j
     *
     * @return GuiLogAppender custom logger for Navigator
     */
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

    public boolean isListening()
    {
        return isListening;
    }

    /**
     * Is ELS just starting-up?
     *
     * @return true/false
     */
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
            path = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), path);
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
     */
    public void process(String[] args)
    {
        Process process;
        ThreadGroup sessionThreads = null;

        context.cfg = new Configuration(context);

        try
        {
            MungeException cfgException = null;

            try
            {
                context.cfg.parseCommandLine(args);
                if (primaryExecution)
                    context.cfg.configureWorkingDirectory();
                else
                    context.cfg.setWorkingDirectory(previousContext.cfg.getWorkingDirectory());
            }
            catch (MungeException e)
            {
                cfgException = e; // configuration exception
            }

            // setup the working directory & logger - once
            if (primaryExecution)
            {
                System.setProperty("jdk.lang.Process.launchMechanism", "POSIX_SPAWN");

                // must be set before any AWT classes are loaded
                if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
                {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                    System.setProperty("apple.awt.application.name", APPLICATION_NAME);
                    System.setProperty("apple.awt.application.appearance", "system");
                }

                // configure logger based on Configuration
                context.cfg.configureLoggerPath();
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

                LoggerContext loggerContext = (LoggerContext) LogManager.getContext(!primaryExecution ? true : false);
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

                LoggerContext loggerContext = (LoggerContext) LogManager.getContext(!primaryExecution ? true : false);
                appender = getGuiLogAppender();
                appender.setContext(context);
                loggerContext.updateLoggers();
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
                logger.warn("local locale not supported, loading default");
                context.cfg.loadLocale("-");
            }
            else
                //logger.trace("loaded locale: " + filePart);
                localeAbbrev = filePart;

            // use preferences for empty publisher/subscriber/hint server arguments for Navigator
            checkEmptyArguments();

            // re-throw any configuration exception
            if (cfgException != null)
                throw cfgException;

            // pre-create working directory structure
            checkWorkingDirectories();

            // logger mode is only for Jobs
            if (context.cfg.getOperation() != JOB_PROCESS)
                context.cfg.setLoggerView(false);

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
                        whatsRunning = "ELS: Local Navigator";
                        logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                        context.cfg.dump();

                        if (context.cfg.getPublisherFilename().length() > 0)
                        {
                            context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                        }

                        if (context.cfg.getSubscriberFilename().length() > 0)
                        {
                            context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                        }

                        context.tools = new Tools();
                        context.tools.loadAllTools(context, null);

                        // setup the hint status server if defined
                        setupHints(context.publisherRepo);

                        context.navigator = new Navigator(context);
                        if (!context.fault)
                        {
                            context.navigator.run(); // environment saved again in Navigator
                        }
                    }
                    else
                    {
                        whatsRunning = "ELS: Local Publish";;
                        logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                        context.cfg.dump();

                        if (context.cfg.getPublisherFilename().length() > 0)
                            context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                        else
                            throw new MungeException("A -p publisher library or -P collection file is required for Local Publish");

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
                        saveEnvironment();
                        process = new Process(context);
                        process.process();
                    }
                    break;

                // --- -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    whatsRunning = "ELS: Publisher Listener";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
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
                        context.serveStty = new ServeStty(sessionThreads, 100, context, true);
                        context.serveStty.startListening(context.publisherRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context, context.publisherRepo, context.subscriberRepo, true);
                        context.serveSftp.startServer();

                        saveEnvironment();
                    }
                    else
                    {
                        throw new MungeException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // --- -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    whatsRunning = "ELS: Publisher Terminal";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.publisherRepo);

                        // start the serveStty client interactively
                        if (connectSubscriber(false))
                        {
                            // start the serveSftp transfer client
                            context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                            if (!context.clientSftp.startClient("transfer"))
                            {
                                throw new MungeException("Publisher sftp transfer client to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                            }
                        }

                        saveEnvironment();
                    }
                    break;

                // --- -r P execute the backup process to remote subscriber -r S
                case PUBLISH_REMOTE:
                    // handle -n|--navigator to display the Navigator
                    if (context.cfg.isNavigator())
                        whatsRunning = "ELS: Remote Navigator";
                    else
                        whatsRunning = "ELS: Remote Publish";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.cfg.isNavigator() || (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized()))
                    {
                        // connect to the hint status server if defined
                        boolean commOk = true;
                        setupHints(context.publisherRepo);

                        // start the serveStty client for automation
                        if (connectSubscriber(true))
                        {
                            // start the serveSftp transfer client
                            context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                            if (!context.clientSftp.startClient("transfer"))
                            {
                                throw new MungeException("Subscriber sftp transfer to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                            }
                        }
                        else
                            commOk = false;

                        // handle -n|--navigator to display the Navigator
                        if (context.cfg.isNavigator())
                        {
                            context.tools = new Tools();
                            context.tools.loadAllTools(context, null);

                            if (commOk)
                            {
                                // start the serveSftp metadata client
                                context.clientSftpMetadata = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                                if (!context.clientSftpMetadata.startClient("metadata"))
                                {
                                    throw new MungeException("Subscriber sftp metadata to " + context.subscriberRepo.getLibraryData().libraries.description + " failed to connect");
                                }
                            }

                            context.navigator = new Navigator(context);
                            if (!context.fault)
                            {
                                saveEnvironment();
                                context.navigator.run();
                            }
                        }
                        else
                        {
                            if (commOk)
                            {
                                // the Process class handles the ELS process
                                saveEnvironment();
                                process = new Process(context);
                                process.process();
                            }
                        }
                    }
                    else
                    {
                        throw new MungeException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // --- -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    whatsRunning = "ELS: Subscriber Listener";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (!context.cfg.isTargetsEnabled())
                        throw new MungeException("Targets -t|-T required");

                    if (context.cfg.getPublisherFilename().length() > 0)
                    {
                        context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    }
                    else
                    {
                        if (context.cfg.getAuthKeysFile() == null || context.cfg.getAuthKeysFile().isEmpty())
                        {
                            throw new MungeException(context.cfg.gs(("Main.either.a.publisher.or.authentication.keys.file.is.required")));
                        }

                    }
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start servers
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.subscriberRepo);

                        // start serveStty server
                        sessionThreads = new ThreadGroup("subscriber.listener");
                        context.serveStty = new ServeStty(sessionThreads, 100, context, true);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context, context.subscriberRepo, context.publisherRepo, true);
                        context.serveSftp.startServer();

                        saveEnvironment();
                    }
                    else
                    {
                        throw new MungeException("Subscriber and publisher options are required for -r S");
                    }
                    break;

                // --- -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    whatsRunning = "ELS: Subscriber Terminal";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // connect to the hint status server if defined
                        setupHints(context.subscriberRepo);

                        // start the serveStty client interactively
                        if (connectSubscriber(false))
                        {
                            // start the serveSftp transfer client
                            context.clientSftp = new ClientSftp(context, context.subscriberRepo, context.publisherRepo, true);
                            if (!context.clientSftp.startClient("transfer"))
                            {
                                throw new MungeException("Publisher sftp transfer to " + context.publisherRepo.getLibraryData().libraries.description + " failed to connect");
                            }

                            // start serveStty server
                            sessionThreads = new ThreadGroup("subscriber.terminal");
                            context.serveStty = new ServeStty(sessionThreads, 100, context, false);
                            context.serveStty.startListening(context.subscriberRepo);
                            isListening = true;

                            // start serveSftp server
                            context.serveSftp = new ServeSftp(context, context.subscriberRepo, context.publisherRepo, false);
                            context.serveSftp.startServer();

                            saveEnvironment();
                        }
                    }
                    else
                    {
                        throw new MungeException("A subscriber -s or -S file and publisher -p or -P) is required for -r T");
                    }
                    break;

                // --- -H|--hint-server stand-alone hint status server
                case STATUS_SERVER:
                    whatsRunning = "ELS: Hint Status Server";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getHintKeysFile() == null || context.cfg.getHintKeysFile().length() == 0)
                        throw new MungeException("-H|--status-server requires a -k|-K hint keys file");

                    if (context.cfg.getHintsDaemonFilename() == null || context.cfg.getHintsDaemonFilename().length() == 0)
                        throw new MungeException("-H|--status-server requires Hint Server JSON file");

                    if (context.cfg.getAuthKeysFile() == null || context.cfg.getAuthKeysFile().length() == 0)
                        throw new MungeException("-H|--status-server requires a -A|--auth-keys file");

                    if (context.cfg.getPublisherFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -p|-P");

                    if (context.cfg.getSubscriberFilename().length() > 0)
                        throw new MungeException("-H|--status-server does not use -s|-S");

                    if (context.cfg.isTargetsEnabled())
                        throw new MungeException("-H|--status-server does not use targets");

                    // Get Hint Keys
                    context.hintKeys = new HintKeys(context);
                    context.hintKeys.read(context.cfg.getHintKeysFile());
                    context.hints = new Hints(context, context.hintKeys);

                    // Get the Hint Status Server repository
                    context.hintsRepo = new Repository(context, Repository.HINT_SERVER);
                    context.hintsRepo.read(context.cfg.getHintsDaemonFilename(), "Hint Status Server", true);

                    // Setup the Hint Status Server datastore, single instance
                    context.datastore = new Datastore(context);
                    boolean valid = context.datastore.initialize();

                    // start server
                    if (valid && context.hintsRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("hint.status.server");
                        context.serveStty = new ServeStty(sessionThreads, 100, context, true);
                        context.serveStty.startListening(context.hintsRepo);
                        isListening = true;
                        saveEnvironment();
                    }
                    else
                    {
                        throw new MungeException("Error initializing from hint status server JSON file");
                    }
                    break;

                // --- -Q|--force-quit the hint status server remotely
                case STATUS_SERVER_FORCE_QUIT:
                    whatsRunning = "ELS: Hint Status Server Quit";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getHintHandlerFilename() == null || context.cfg.getHintHandlerFilename().length() == 0)
                        throw new MungeException("-Q|--force-quit requires a either -h|--hints or -H|--hint-server");

                    if (context.cfg.getPublisherFilename() == null || context.cfg.getPublisherFilename().length() == 0)
                        throw new MungeException("-Q|--force-quit requires a -p|-P publisher to connect from");

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE); // no need to validate for this

                    setupHints(context.publisherRepo);

                    // force the cfg setting & let this process end normally
                    // that will send the quit command to the hint status server
                    context.cfg.setQuitStatusServer(true);

                    saveEnvironment();
                    break;

                // --- -G|--listener-quit the remote subscriber
                case SUBSCRIBER_LISTENER_FORCE_QUIT:
                    whatsRunning = "ELS: Subscriber Listener Quit";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    if (context.cfg.getSubscriberFilename() == null || context.cfg.getSubscriberFilename().length() == 0)
                        throw new MungeException("-G|--listener-quit requires a -s|-S subscriber JSON file");

                    context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.NO_VALIDATE); // who we are
                    context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE); // listener to quit

                    // start client
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client
                        if (connectSubscriber(false))
                        {
                            try
                            {
                                saveEnvironment();
                                context.clientStty.roundTrip("quit", "Sending remote quit command", 5000);
                                Thread.sleep(3000);
                            }
                            catch (Exception e)
                            {
                                // ignore any exception
                            }
                        }
                    }
                    break;

                // --- -j|--job to execute a Job
                case JOB_PROCESS:
                    whatsRunning = "ELS: Job";
                    logger.info(whatsRunning + ", version " + getBuildVersionName() + ", " + getBuildDate());
                    context.cfg.dump();

                    // optional arguments for support of Any Publisher/Subscriber
                    if (context.cfg.getPublisherFilename().length() > 0)
                    {
                        context.publisherRepo = readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                    }

                    if (context.cfg.getSubscriberFilename().length() > 0)
                    {
                        context.subscriberRepo = readRepo(context, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                    }

                    if (context.cfg.isLoggerView())
                    {
                        context.navigator = new Navigator(context);
                        if (!context.fault)
                        {
                            context.navigator.run(); // environment saved again in Navigator
                        }
                    }
                    else
                    {
                        context.tools = new Tools();
                        context.tools.loadAllTools(context, null);

                        // setup the hint status server if defined
                        setupHints(context.publisherRepo);

                        context.transfer = new Transfer(context);
                        context.transfer.initialize();

                        // run the Job
                        Job tmpJob = new Job(context, "temp");
                        job = tmpJob.load(context.cfg.getJobName());
                        if (job == null)
                            throw new MungeException("Job \"" + context.cfg.getJobName() + "\" could not be loaded");
                        whatsRunning += ", " + job.getConfigName();
                        saveEnvironment();
                        job.process(context);
                    }
                    break;

                default:
                    throw new MungeException("Unknown operation");
            }
        }
        catch (Exception e)
        {
            if (primaryExecution) // if not running as an Operation
                context.fault = true;

            if (catchExceptions)
            {
                if (logger != null)
                {
                    logger.error(Utils.getStackTrace(e));
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
            if (!isListening && !context.cfg.isNavigator()) // clients
            {
                // if a fault occurred tell any listener
                if (context.fault && context.clientStty != null && context.clientStty.isConnected())
                {
                    if (!context.timeout)
                    {
                        try
                        {
                            context.clientStty.roundTrip("fault", "Sending remote fault command (1)", 5000);
                        }
                        catch (Exception e)
                        {
                            // ignore any exception
                        }
                    }
                }

                // optionally command status server to quit
                if (context.hintsStty != null)
                    context.hintsStty.quitStatusServer(context);  // do before stopping the necessary services

                // stop any remaining services
                if (primaryExecution)
                {
                    stopServices();
                    stopVerbiage();
                }
                else
                    restoreEnvironment();
            }
            else if (!primaryExecution && !isListening && context.cfg.isNavigator())
            {
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            logger.info("NAVIGATOR SHUTDOWN HOOK");
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                        }
                    }
                });
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
                            if (isListening && context.cfg.getOperation() == JOB_PROCESS)
                            {
                                String msg = java.text.MessageFormat.format(context.cfg.gs("Job.completed.job"),
                                        job.getConfigName() + (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : ""));
                                logger.info(msg);
                            }
                            //logger.info(context.cfg.gs("Main.disconnecting"));

                            // optionally command status server to quit
                            if (context.hintsStty != null)
                                context.hintsStty.quitStatusServer(context);  // do before stopping the services

                            if (primaryExecution)
                            {
                                stopVerbiage();
                                stopServices(); // must be called AFTER stopVerbiage()
                            }
                            else
                                restoreEnvironment();

                            // halt kills the remaining threads
                            if (context.fault)
                                logger.error("Exiting with error code");
                            if (primaryExecution)
                            {
                                Runtime.getRuntime().halt(context.fault ? 1 : 0);
                            }
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            if (primaryExecution)
                            {
                                Runtime.getRuntime().halt(1);
                            }
                        }
                    }
                });
                logger.trace("listener shutdown hook added, " + whatsRunning);
            }
        }

        // is this a restarted Navigator instance after being updated?
        if (!context.fault)
        {
            if (context.cfg.isNavigator() && (context.cfg.isUpdateSuccessful() || context.cfg.isUpdateFailed()))
            {
                try
                {
                    // give the GUI time to come up
                    Thread.sleep(5000);
                }
                catch (Exception e)
                {
                }

                String logFilename = Utils.getTempUpdaterDirectory() + System.getProperty("file.separator") + "ELS-Updater.log";
                String message = context.cfg.isUpdateSuccessful() ?
                        Configuration.PROGRAM_NAME + " " + context.cfg.gs("Navigator.updated") :
                        java.text.MessageFormat.format(context.cfg.gs("Navigator.update.failed"), logFilename);
                logger.info(message);

                Object[] opts = {context.cfg.gs("Z.ok")};
                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update.status"),
                        JOptionPane.PLAIN_MESSAGE, context.cfg.isUpdateSuccessful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                        null, opts, opts[0]);
            }
        }

        if (primaryExecution && context.fault)
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
     * Read an encrypted data stream, return decrypted string
     *
     * @param in  DataInputStream to read, e.g. remote connection
     * @param key UUID key to decrypt the data stream
     * @return String read from stream; null if connection is closed
     */
    public String readStream(DataInputStream in, String key) throws Exception
    {
        byte[] buf = {};
        String input = "";
        while (true)
        {
            try
            {
                logger.trace("read() waiting ... " + whatsRunning);
                int count = in.readInt();

                logger.trace("  receiving " + count + " encrypted bytes, " + whatsRunning);
                int pos = 0;
                if (count > 0)
                {
                    buf = new byte[count];
                    int remaining = count;
                    while (true)
                    {
                        int readCount = in.read(buf, pos, remaining);
                        if (readCount < 0)
                            break;
                        pos += readCount;
                        remaining -= readCount;
                        if (pos == count)
                            break;
                    }
                    if (pos != count)
                    {
                        logger.warn("read counts do not match, expected " + count + ", received " + pos);
                    }
                }
                break;
            }
            catch (SocketTimeoutException e)
            {
                logger.error("read() timed-out");
                input = null;
                throw e;
            }
            catch (EOFException e)
            {
                logger.error("  read() EOF");
                input = null; // remote disconnected
                break;
            }
            catch (IOException e)
            {
                if (e.getMessage().toLowerCase().contains("connection reset"))
                    logger.warn("connection closed during read");
                input = null;
                throw e;
            }
        }
        if (buf.length > 0 && input != null)
            input = decrypt(key, buf);

        logger.trace("read done " + ((input != null) ? input.length() : "0") + " bytes, " + whatsRunning);
        return input;
    }

    public void restoreEnvironment()
    {
        try
        {
            context.environment.switchConnections();
        }
        catch (Exception e)
        {
            logger.error(context.cfg.gs("Z.exception") + System.getProperty("line.separator") + Utils.getStackTrace(e));
            if (context.cfg.isNavigator())
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.exception") + Utils.getStackTrace(e),
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save original environment
     */
    public void saveEnvironment()
    {
        if (primaryExecution && !secondaryNavigator)
        {
            if (context.environment != null)
                context.environment = null; // suggest clean-up

            context.environment = new Environment(context); // the initial environment only
        }
    }

    public void setListening(boolean listening)
    {
        isListening = listening;
    }

    /**
     * Setup hint keys & tracking, connect to hint server if specified
     * <br/><br/>
     * Hint Keys are required for Hint Tracking/Daemon.<br/><br/>
     * Will connect to a Hint Server, if specified, or local Hint Tracker, if specified.
     * If none of those things are defined in the configuration this method simply returns.
     *
     * @param repo The Repository that is connecting to the tracker/server
     * @throws Exception Configuration and connection exceptions
     */
    public void setupHints(Repository repo) throws Exception
    {
        String msg = "";
        try
        {
            if (context.cfg.isHintTrackingEnabled() || context.cfg.getHintKeysFile().length() > 0)
            {
                if (context.cfg.getHintKeysFile().length() > 0)
                {
                    // Hints Keys
                    context.hintKeys = new HintKeys(context);
                    msg = context.cfg.gs("Main.exception.while.reading.hint.keys");
                    context.hintKeys.read(context.cfg.getHintKeysFile());
                    context.hints = new Hints(context, context.hintKeys);
                    context.preferences.setLastHintKeysIsOpen(true);
                }
                else
                {
                    if (!context.cfg.isQuitStatusServer())
                        throw new MungeException(context.cfg.gs("Main.hint.keys.are.required.to.use.hint.tracking"));
                }

                if (context.cfg.isHintTrackingEnabled())
                {
                    context.hintsRepo = new Repository(context, Repository.HINT_SERVER);

                    // Remote Hint Status Server
                    if (context.cfg.getHintsDaemonFilename().length() > 0 && repo != null)
                    {
                        // exceptions handle by read()
                        catchExceptions = false;
                        msg = context.cfg.gs("Main.exception.while.reading.hint.server");

                        if (context.hintsRepo.read(context.cfg.getHintsDaemonFilename(), "Hint Status Server", true))
                        {
                            catchExceptions = true;

                            // start the hintsStty client connection to the Hint Status Server
                            context.hintsStty = new ClientStty(context, false, true, true); //primaryServers);
                            if (!context.hintsStty.connect(repo, context.hintsRepo))
                            {
                                msg = "";
                                throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("Main.hint.status.server.failed.to.connect"),
                                        context.hintsRepo.getLibraryData().libraries.description));
                            }

                            String response = context.hintsStty.receive("", 5000); // check the initial prompt
                            if (!response.startsWith("CMD"))
                            {
                                msg = "";
                                throw new MungeException(context.cfg.gs("Main.bad.initial.response.from.hint.status.server") +
                                        context.hintsRepo.getLibraryData().libraries.description);
                            }

                            context.preferences.setLastHintTrackingIsRemote(true);
                            context.preferences.setLastHintTrackingIsOpen(true);
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
                        msg = context.cfg.gs("Main.exception.while.reading.hint.tracker");

                        if (context.hintsRepo.read(context.cfg.getHintTrackerFilename(), "Hint Tracker", true))
                        {
                            // Setup the Hint Tracker datastore, single instance
                            catchExceptions = true;
                            context.datastore = new Datastore(context);
                            boolean valid = context.datastore.initialize();
                            if (!valid)
                            {
                                throw new MungeException(context.cfg.gs("Main.error.initializing.from.hint.status.server.json.file"));
                            }

                            context.preferences.setLastHintTrackingIsRemote(false);
                            context.preferences.setLastHintTrackingIsOpen(true);
                        }
                        else
                        {
                            catchExceptions = true;
                            context.cfg.setHintTrackerFilename("");
                        }
                    }
                }
            }
            else
            {
                context.preferences.setLastHintKeysIsOpen(false);
                context.preferences.setLastHintTrackingIsOpen(false);
            }
        }
        catch (Exception e)
        {
            if (catchExceptions)
            {
                msg = (msg.length() > 0 ? msg : "") + e.getMessage();
                logger.error(msg);

                //context.cfg.setHintKeysFile("");
                context.cfg.setHintsDaemonFilename("");
                context.cfg.setHintTrackerFilename("");
                context.hintsRepo = null;

                if (isStartupActive())
                {
                    int opt = JOptionPane.showConfirmDialog(getGuiLogAppender().getStartup(),
                            "<html><body>" + msg + "<br/><br/>" + context.cfg.gs(("Main.continue")) + "</body></html>",
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
        logger.trace("stopServices(), " + whatsRunning);

        try
        {
            // logout from any hint status server if not shutting it down
            if (context.hintsStty != null)
            {
                if (!context.cfg.isQuitStatusServer() && context.hintsStty.isConnected())
                {
                    context.hintsStty.send("bye", "Sending bye command to remote Hint Status Server");
                    Thread.sleep(3000);
                }
                context.hintsStty.disconnect();
                context.hintsStty = null;
            }
            if (context.clientSftp != null)
            {
                logger.trace("  sftp client");
                context.clientSftp.stopClient();
                context.clientSftp = null;
                Thread.sleep(3000L);
            }
            if (context.clientSftpMetadata != null)
            {
                logger.trace("  sftp client transfer");
                context.clientSftpMetadata.stopClient();
                context.clientSftpMetadata = null;
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
        isListening = false;
    }

    /**
     * Log completion statistics
     */
    public void stopVerbiage()
    {
        if (!context.cfg.getConsoleLevel().equalsIgnoreCase(context.cfg.getDebugLevel()))
            logger.info("log file has more details: " + context.cfg.getLogFileName());

        Date done = new Date();
        long millis = Math.abs(done.getTime() - stamp.getTime());
        logger.fatal("Runtime: " + Utils.getDuration(millis));

        if (!context.fault)
            logger.fatal("Process completed normally");
        else
            logger.fatal("Process failed");
    }

    /**
     * Write an encrypted string to output stream
     *
     * @param out     DataOutputStream to write
     * @param key     UUID key to encrypt the string
     * @param message String to encrypted and write
     */
    public void writeStream(DataOutputStream out, String key, String message) throws Exception
    {
        logger.trace("writing " + message.length() + " bytes, " + whatsRunning);
        byte[] buf = encrypt(key, message);

        logger.trace("  sending " + buf.length + " encrypted bytes, " + whatsRunning);
        //logger.trace("  writing size");
        out.writeInt(buf.length);

        //logger.trace("  flushing size");
        out.flush();

        //logger.trace("  writing data");
        out.write(buf);

        //logger.trace("  flushing data");
        out.flush();

        logger.trace("write done, " + whatsRunning);
    }
}
