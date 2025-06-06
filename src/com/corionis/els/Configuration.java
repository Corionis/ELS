package com.corionis.els;

import com.corionis.els.gui.MainFrame;
import com.corionis.els.gui.Preferences;
import com.corionis.els.repository.Repository;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.sshd.common.util.io.IoUtils;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

/**
 * Configuration class
 * <p>
 * Contains all command-line options and other application-level configuration.
 */
public class Configuration
{
    public static final String APPLICATION_NAME = "ELS"; // for apple.awt.application.name
    public static final int BUILD_CHANGES_URL = 5;
    public static final int BUILD_DATE = 2;
    public static final int BUILD_ELS_DISTRO = 3;
    public static final int BUILD_FLAGS = 6;
    public static final int BUILD_NUMBER = 1;
    public static final int BUILD_UPDATER_DISTRO = 4;
    public static final int BUILD_VERSION_NAME = 0;
    public static final String ELS_ICON = "els-logo-98px";
    public static final String ELS_JAR = "ELS.jar";
    public static final int JOB_PROCESS = 8;
    public static final String LOGGER_NAME = "ELS Logger";
    public static final String NAVIGATOR_NAME = "ELS Navigator";
    public static final int NOT_REMOTE = 0;
    public static final int NOT_SET = -1;
    public static final String PROGRAM_NAME = "Corionis ELS : Entertainment Library Synchronizer";
    public static final int PUBLISHER_LISTENER = 4;
    public static final int PUBLISHER_MANUAL = 3;
    public static final int PUBLISH_REMOTE = 1;
    public static final int STATUS_SERVER = 6;
    public static final int STATUS_SERVER_FORCE_QUIT = 7;
    public static final int SUBSCRIBER_LISTENER = 2;
    public static final int SUBSCRIBER_LISTENER_FORCE_QUIT = 9;
    public static final int SUBSCRIBER_TERMINAL = 5;
    public static final String URL_PREFIX = "https://raw.githubusercontent.com/Corionis/ELS/Version-4.0.0/deploy"; // TODO MAINTENANCE+ Adjust as needed
    public static final int VERSION_SIZE = 6; // number of lines required in version.info
    public static final String[] availableLocales = {"en_US"}; // TODO EXTEND+ Add new locales here; Potentially refactor to include files from a locales directory
    private boolean defaultNavigator = false;
    private String authKeysFile = "";
    private String authorizedPassword = "";
    private String blacklist = "";
    private boolean checkForUpdate = false;
    private String consoleLevel = "Info";  // Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF
    private boolean consoleSet = false;
    private Context context;
    private int crossCheck = -1;
    private ResourceBundle currentBundle = null;
    private String debugLevel = "Info";
    private boolean debugSet = false;
    private int dryRun = -1;
    private int duplicateCheck = -1;
    private int emptyDirectoryCheck = -1;
    private String exportCollectionFilename = "";
    private String exportTextFilename = "";
    private int forceCollection = -1;
    private int forceTargets = -1;
    private String hintKeysFile = "";
    private int hintSkipMainProcess = -1;
    private String hintTrackerFilename = "";
    private String hintsDaemonFilename = "";
    private int ignoredReported = -1;
    private boolean installUpdate = false;
    private String iplist = "";
    private String jobName = "";
    private int keepGoing = -1;
    private String logFileFullPath = "";
    private String logFileName = "";
    private String logFilePath = "";
    private int logOverwrite = 1;
    private boolean loggerView = false;
    private double longScale = 1024L;
    private String marker = "";
    private String mismatchFilename = "";
    private int navigator = -1;
    private int noBackFill = -1;
    private int operation = NOT_SET;
    private String[] originalArgs;
    private int overrideHintHost = -1;
    private String overrideSubscriberHost = "";
    private int overwrite = -1;
    private int preserveDates = -1;
    private String publisherCollectionFilename = "";
    private String publisherLibrariesFileName = "";
    private int quitStatusServer = -1;
    private int quitSubscriberListener = -1;
    private String remoteArg = "";
    private int requestCollection = -1;
    private int requestTargets = -1;
    private ArrayList<String> selectedLibraryExcludes = new ArrayList<>();
    private ArrayList<String> selectedLibraryNames = new ArrayList<>();
    private int specificExclude = -1;
    private int specificLibrary = -1;
    private String subscriberCollectionFilename = "";
    private String subscriberLibrariesFileName = "";
    private int targetsEnabled = -1;
    private String targetsFilename = "";
    private boolean updateFailed = false;
    private boolean updateSuccessful = false;
    private int validation = -1;
    private int whatsNewAll = -1;
    private String whatsNewFilename = "";
    private String workingDirectory = "";
    private String workingDirectorySubscriber = "";

    /**
     * Constructor
     */
    public Configuration(Context ctxt)
    {
        context = ctxt;
        context.cfg = this;

        // attempt to load the language file based on local system, default en_US
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String filePart = lang + "_" + country;
        context.cfg.loadLocale(filePart);
        if (context.cfg.gs("Transfer.received.subscriber.commands").length() == 0)
        {
            context.cfg.loadLocale("-");
            System.out.println(context.cfg.gs("Process.local.locale.not.supported.loading.default"));
        }
        else
        {
            if (context.main != null)
                context.main.localeAbbrev = filePart;
        }
    }

    /**
     * Add an excluded publisher library name
     *
     * @param publisherLibraryName the publisher library name
     */
    public void addExcludedLibraryName(String publisherLibraryName)
    {
        this.selectedLibraryExcludes.add(publisherLibraryName);
    }

    /**
     * Add a publisher library name
     *
     * @param publisherLibraryName the publisher library name
     */
    public void addPublisherLibraryName(String publisherLibraryName)
    {
        this.selectedLibraryNames.add(publisherLibraryName);
    }

    /**
     * Return locale bundle
     *
     * @return bundle ResourceBundle
     */
    public ResourceBundle bundle()
    {
        return currentBundle;
    }

    /**
     * Clone Configuration
     *
     * @return Configuration Object
     */
    @Override
    public Object clone()
    {
        Configuration clone = new Configuration(context);
        clone.defaultNavigator = defaultNavigator;
        clone.authKeysFile = authKeysFile;
        clone.authorizedPassword = authorizedPassword;
        clone.blacklist = blacklist;
        clone.consoleLevel = consoleLevel;
        clone.consoleSet = consoleSet;
        clone.context = context;
        clone.crossCheck = crossCheck;
        clone.currentBundle = currentBundle;
        clone.debugLevel = debugLevel;
        clone.debugSet = debugSet;
        clone.dryRun = dryRun;
        clone.duplicateCheck = duplicateCheck;
        clone.emptyDirectoryCheck = emptyDirectoryCheck;
        clone.exportCollectionFilename = exportCollectionFilename;
        clone.exportTextFilename = exportTextFilename;
        clone.forceCollection = forceCollection;
        clone.forceTargets = forceTargets;
        clone.hintKeysFile = hintKeysFile;
        clone.hintSkipMainProcess = hintSkipMainProcess;
        clone.hintTrackerFilename = hintTrackerFilename;
        clone.hintsDaemonFilename = hintsDaemonFilename;
        clone.ignoredReported = ignoredReported;
        clone.iplist = iplist;
        clone.jobName = jobName;
        clone.keepGoing = keepGoing;
        clone.logFileFullPath = logFileFullPath;
        clone.logFileName = logFileName;
        clone.logFilePath = logFilePath;
        clone.loggerView = loggerView;
        ;
        clone.logOverwrite = logOverwrite;
        clone.longScale = longScale;
        clone.marker = marker;
        clone.mismatchFilename = mismatchFilename;
        clone.navigator = navigator;
        clone.noBackFill = noBackFill;
        clone.operation = operation;
        clone.originalArgs = originalArgs;
        clone.overrideHintHost = overrideHintHost;
        clone.overrideSubscriberHost = overrideSubscriberHost;
        clone.overwrite = overwrite;
        clone.preserveDates = preserveDates;
        clone.publisherCollectionFilename = publisherCollectionFilename;
        clone.publisherLibrariesFileName = publisherLibrariesFileName;
        clone.quitStatusServer = quitStatusServer;
        clone.quitSubscriberListener = quitSubscriberListener;
        clone.requestCollection = requestCollection;
        clone.requestTargets = requestTargets;
        clone.selectedLibraryExcludes = (ArrayList<String>) selectedLibraryExcludes.clone();
        clone.selectedLibraryNames = (ArrayList<String>) selectedLibraryNames.clone();
        clone.specificExclude = specificExclude;
        clone.specificLibrary = specificLibrary;
        clone.subscriberCollectionFilename = subscriberCollectionFilename;
        clone.subscriberLibrariesFileName = subscriberLibrariesFileName;
        clone.targetsEnabled = targetsEnabled;
        clone.targetsFilename = targetsFilename;
        clone.updateFailed = updateFailed;
        clone.updateSuccessful = updateSuccessful;
        clone.validation = validation;
        clone.whatsNewAll = whatsNewAll;
        clone.whatsNewFilename = whatsNewFilename;
        clone.workingDirectory = workingDirectory;
        clone.workingDirectorySubscriber = workingDirectorySubscriber;
        return clone;
    }

    /**
     * Configure the logging configuration
     * <br/>
     * Sets the logFullPath and logPath based on configuration.
     * This is executed BEFORE the logger is configured, so the Utils class cannot be used.
     */
    public void configureLoggerPath() throws Exception
    {
        // setup the absolute path for the log file before configuring logging
        if (getLogFileName() != null && getLogFileName().length() > 0)
        {
            String prefix = "";
            String relative = "";

            String lfn = getLogFileName();
            setLogFilePath("");

            // if relative path prepend with working directory
            if (!lfn.matches("^[a-zA-Z]:.*") &&
                    !lfn.startsWith("/") &&
                    !lfn.startsWith("\\"))
            {
                prefix = getWorkingDirectory() + System.getProperty("file.separator");

                String separator = "";
                if (lfn.contains("\\"))
                {
                    separator = "\\";
                }
                else if (lfn.contains("/"))
                {
                    separator = "/";
                }
                int i = (separator.length() > 0 ? lfn.lastIndexOf(separator) : -1);
                if (i >= 0)
                {
                    relative = lfn.substring(0, i + 1);
                    setLogFilePath(relative);
                }
            }
            setLogFileFullPath(prefix + lfn);
        }
        else
        {
            setLogFileName("ELS-Navigator.log");
            setLogFileFullPath(this.workingDirectory + System.getProperty("file.separator") + "output/ELS-Navigator.log");
        }
    }

    /**
     * Configure the working directory
     * <br/>
     * Sets the current working directory based on configuration.
     */
    public void configureWorkingDirectory() throws Exception
    {
        // set default working directory if not set with -C | --cfg
        if (this.workingDirectory == null || this.workingDirectory.length() == 0)
            setWorkingDirectory(getDefaultWorkingDirectory());

        // check & create working directory
        File wd = new File(getWorkingDirectory());
        if (!wd.exists())
            wd.mkdirs();
        else if (!wd.isDirectory())
            throw new MungeException(MessageFormat.format(context.cfg.gs("Cfg.configuration.directory.is.not.a.directory"), wd.getCanonicalPath()));

        // change to working directory
        setWorkingDirectory(wd.getCanonicalPath());
        System.setProperty("user.dir", getWorkingDirectory());
        setWorkingDirectorySubscriber(getWorkingDirectory());  // set local for default
    }

    /**
     * Disable Hint Tracking/Server
     * <p></p>
     * Use BEFORE main.setupHints();
     */
    public void disableHintTracking()
    {
        this.hintTrackerFilename = "";
        this.hintsDaemonFilename = "";
    }

    /**
     * Dump the configuration
     */
    public void dump()
    {
        Logger logger = LogManager.getLogger("applog");
        Marker SHORT = MarkerManager.getMarker("SHORT");

        String msg = "Arguments: ";
        for (int index = 0; index < originalArgs.length; ++index)
        {
            if (StringUtils.isAsciiPrintable(originalArgs[index])) // handle JDK arguments bug
                msg = msg + originalArgs[index] + " ";
        }
        logger.info(SHORT, msg);

        if (getAuthKeysFile().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.a.authentication.keys.filename"), getAuthKeysFile()));
        }
        if (getAuthorizedPassword().length() > 0)
        {
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.a.authorize.mode.password.has.been.specified"));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.b.no.back.fill"), noBackFill);
        if (blacklist.length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.b.blacklist.filename"), blacklist));
        }
        if (consoleSet)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.c.console.logging.level"), getConsoleLevel()));
        }
        if (workingDirectory.length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.c.configuration.directory"), getWorkingDirectory()));
        }
        if (debugSet)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.d.debug.logging.level"), getDebugLevel()));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.d.dry.run"), dryRun);
        if (getExportTextFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.e.export.text.filename"), getExportTextFilename()));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.e.empty.directories"), emptyDirectoryCheck);
        if (getLogFileName().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.choice.f.log.filename"), isLogOverwrite() ? 0 : 1,getLogFileName()));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.g.listener.keep.going"), keepGoing);
        if (isQuitSubscriberListener())
        {
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.g.subscriber.listener.force.quit.now"));
        }
        if (hintTrackerFilename != null && hintTrackerFilename.length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.h.hint.tracker"), getHintTrackerFilename()));
        }
        if (hintsDaemonFilename != null && hintsDaemonFilename.length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.h.hint.status.server"), getHintsDaemonFilename()));
        }
        if (getExportCollectionFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.i.export.collection.json.filename"), getExportCollectionFilename()));
        }
        if (getIpWhitelist().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.i.ip.whitelist.filename"), getIpWhitelist()));
        }
        if (getOperation() == JOB_PROCESS && getJobName().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.j.job"), getJobName()));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.j.override.hint.host"), overrideHintHost);
        if (getHintKeysFile().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.choice.k.hint.keys.filename"), isHintSkipMainProcess() ? 0 : 1,getHintKeysFile()));
        }
        if (!getSelectedLibraryNames().isEmpty())
        {
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.l.publisher.library.name.s"));
            for (String ln : getSelectedLibraryNames())
            {
                logger.info(SHORT, "          " + ln);
            }
        }
        if (!getExcludedLibraryNames().isEmpty())
        {
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.l.excluded.library.name.s"));
            for (String ln : getExcludedLibraryNames())
            {
                logger.info(SHORT, "          " + ln);
            }
        }
        if (getMismatchFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.m.mismatches.output.filename"), getMismatchFilename()));
        }
        if (marker.length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.marker"), marker));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.n.navigator"), navigator);
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.n.ignored.files.reported"), ignoredReported);
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.o.overwrite"), overwrite);
        if (!overrideSubscriberHost.isEmpty())
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.o.override.subscriber.host"), overrideSubscriberHost));
        }
        if (getPublisherLibrariesFileName().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.p.publisher.library.filename"), getPublisherLibrariesFileName()));
        }
        if (getPublisherCollectionFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.p.publisher.collection.filename"), getPublisherCollectionFilename()));
        }
        if (isQuitStatusServer())
        {
            if (getOperation() == STATUS_SERVER_FORCE_QUIT)
                logger.info(SHORT, context.cfg.gs("Cfg.cfg.q.status.server.force.quit.now"));
            else
                logger.info(SHORT, context.cfg.gs("Cfg.cfg.q.status.server.quit"));
        }
        if (!getRemoteType().equalsIgnoreCase("-"))
        {
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.r.remote.session.type") + getRemoteType() + " " + getRemoteTypeName(getOperation()));
        }
        if (getSubscriberLibrariesFileName().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.s.subscriber.library.filename"), getSubscriberLibrariesFileName()));
        }
        if (getSubscriberCollectionFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.s.subscriber.collection.filename"), getSubscriberCollectionFilename()));
        }
        if (isForceCollection() || getTargetsFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.choice.t.targets.filename"), (isForceTargets()) ? 0 : 1,getTargetsFilename()));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.u.duplicates"), duplicateCheck);
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.v.validate"), validation);
        if (getWhatsNewFilename().length() > 0)
        {
            logger.info(SHORT, MessageFormat.format(context.cfg.gs("Cfg.cfg.choice.w.what.s.new.output.filename"), isWhatsNewAll() ? 0 : 1) +
                    getWhatsNewFilename() + (isWhatsNewAll() ? context.cfg.gs("Cfg.show.all.items") : ""));
        }
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.x.cross.check"), crossCheck);
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.y.preserve.dates"), preserveDates);
        indicator(logger, SHORT, context.cfg.gs("Cfg.cfg.z.decimal.scale"), getLongScale() == 1024 ? -1 : 1);
        if (loggerView)
            logger.info(SHORT, context.cfg.gs("Cfg.cfg.logger.display.mode"));
    }

    /**
     * Generate the current Navigator command line
     * <p>
     * Some options are not used by Navigator.
     *
     * @return String command line
     */
    public String generateCurrentCommandline(String consoleLevel, String debugLevel, boolean overwriteLog, String log)
    {
        // generate-commandline
        String opts;
        String exec = context.cfg.getExecutablePath();
        String jar = "";

        Configuration cc = context.cfg;
        Preferences pr = context.preferences;
        boolean glo = context.preferences != null ? context.preferences.isGenerateLongOptions() : false;
        StringBuilder sb = new StringBuilder();

        String conf = (glo ? "--config \"" : "-C \"") + context.cfg.getWorkingDirectory() + "\"";

        // --- non-munging actions
        if (context.cfg.isInstallUpdate())
            sb.append(" " + "-Y");
        if (context.navigator != null)
            sb.append(" " + (glo ? "--navigator" : "-n"));

        if (cc.isDryRun() != cc.isDryRun())
            sb.append(" " + (glo ? "--dry-run" : "-D"));

        // --- hint keys
        if (!cc.isHintSkipMainProcess() && pr.getLastHintKeysOpenFile().length() > 0 && pr.isLastHintKeysIsOpen())
            sb.append(" " + (glo ? "--keys" : "-k") + " \"" +
                    Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pr.getLastHintKeysOpenFile() + "\""));
        if (cc.isHintSkipMainProcess() && pr.getLastHintKeysOpenFile().length() > 0 && pr.isLastHintKeysIsOpen())
            sb.append(" " + (glo ? "--keys-only" : "-K") + " \"" +
                    Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pr.getLastHintKeysOpenFile() + "\""));

        // --- hints & hint server
        if (pr.getLastHintTrackingOpenFile().length() > 0 && pr.isLastHintTrackingIsOpen())
        {
            String hf = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pr.getLastHintTrackingOpenFile());
            if (pr.isLastHintTrackingIsRemote())
            {
                if (pr.isLastOverrideHintHost())
                    sb.append(" " + (glo ? "--override-hints-host" : "-J"));
                sb.append(" " + (glo ? "--hint-server" : "-H") + " \"" + hf + "\"");
            }
            else
                sb.append(" " + (glo ? "--hints" : "-h") + " \"" + hf + "\"");
        }

        // --- Remote
        if (cc.isRemoteOperation())
            sb.append(" " + (glo ? "--remote" : "-r") + " P");

        // --- Publisher
        if (context.publisherRepo != null)
        {
            if (pr.getLastPublisherOpenFile().length() > 0 && pr.isLastPublisherIsOpen())
                sb.append(" " + (glo ? "--publisher-libraries" : (context.preferences.isLastPublisherIsWorkstation() ? "-p" : "-P")) + " \"" +
                        Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pr.getLastPublisherOpenFile()) + "\"");
        }

        // -- Subscriber
        if (context.subscriberRepo != null)
        {
            if (pr.getLastSubscriberOpenFile().length() > 0 && pr.isLastSubscriberIsOpen())
            {
                if (!pr.getLastOverrideSubscriber().isEmpty())
                    sb.append(" " + (glo ? "--override-host" : "-O ") + overrideSubscriberHost);
                sb.append(" " + (glo ? "--subscriber-libraries" : "-s") + " \"" +
                        Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pr.getLastSubscriberOpenFile()) + "\"");
            }
        }

        // --- options
        if (!cc.isBinaryScale() != !cc.isBinaryScale())
            sb.append(" " + (glo ? "--decimal-scale" : "-z"));
        if (cc.isQuitSubscriberListener() != cc.isQuitSubscriberListener())
            sb.append(" " + (glo ? "--listener-quit" : "-G"));
        if (cc.isKeepGoing() != cc.isKeepGoing())
            sb.append(" " + (glo ? "--listener-keep-going" : "-g"));
        if (cc.isPreserveDates() != cc.isPreserveDates())
            sb.append(" " + (glo ? "--preserve-dates" : "-y"));

        opts = sb.toString().trim();

        String overOpt = overwriteLog ? (glo ? "--log-overwrite" : "-F") : (glo ? "--log-file" : "-f");

        String cmd = exec + (jar.length() > 0 ? " -jar " + "\"" + jar + "\"" : "") +
                " " + conf + " " + opts + (glo ? " --console-level " : " -c ") + consoleLevel +
                (glo ? " --debug-level " : " -d ") + debugLevel + " " + overOpt + " \"" + log + "\"";
        return cmd;
    }

    /**
     * Gets Authentication Keys filename
     *
     * @return String filename
     */
    public String getAuthKeysFile()
    {
        return authKeysFile;
    }

    /**
     * Gets Authorized password
     *
     * @return the password required to access Authorized mode when using a ClientStty
     */
    public String getAuthorizedPassword()
    {
        return authorizedPassword;
    }

    /**
     * Get the blacklist filename, if defined
     *
     * @return Filename or empty string
     */
    public String getBlacklist()
    {
        return blacklist;
    }

    /**
     * Read the built-in build date
     *
     * @return Build changes URL, or unknown
     */
    public static String getBuildChangesUrl()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_CHANGES_URL)
            {
                text = lines.get(BUILD_CHANGES_URL);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Read the built-in build date
     *
     * @return Build stamp string, or unknown
     */
    public static String getBuildDate()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_DATE)
            {
                text = lines.get(BUILD_DATE);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Read the built-in ELS distribution name
     *
     * @return Build version string, or unknown
     */
    public static String getBuildElsDistro()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_ELS_DISTRO)
            {
                text = lines.get(BUILD_ELS_DISTRO);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Read built-in build flags
     *
     * @return Optional build flags or empty String
     */
    public static String getBuildFlags()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_FLAGS)
            {
                text = lines.get(BUILD_FLAGS);
            }
        }
        catch (Exception e)
        {
            text = "";
        }
        return text;
    }

    /**
     * Read the built-in build number
     *
     * @return Build number string, or unknown
     */
    public static String getBuildNumber()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_NUMBER)
            {
                text = lines.get(BUILD_NUMBER);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Read the built-in updater distribution name
     *
     * @return Build version string, or unknown
     */
    public static String getBuildUpdaterDistro()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() >= BUILD_UPDATER_DISTRO)
            {
                text = lines.get(BUILD_UPDATER_DISTRO);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Read the built-in simple version name
     *
     * @return Build version string, or unknown
     */
    public static String getBuildVersionName()
    {
        String text = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/corionis/els/resources/version.info");
            List<String> lines = IoUtils.readAllLines(url);
            if (lines.size() > BUILD_VERSION_NAME)
            {
                text = lines.get(BUILD_VERSION_NAME);
            }
        }
        catch (Exception e)
        {
            text = "unknown";
        }
        return text;
    }

    /**
     * Gets console level
     *
     * @return the console level
     */
    public String getConsoleLevel()
    {
        return consoleLevel;
    }

    /**
     * Get the original configured Context
     *
     * @return Context
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * Gets debug level
     *
     * @return the debug level
     */
    public String getDebugLevel()
    {
        return debugLevel;
    }

    /**
     * Gets the default working directory
     * <br/>
     * The default is: [user home]/.els
     *
     * @return String Default ELS working directory
     */
    public String getDefaultWorkingDirectory()
    {
        return System.getProperty("user.home") + System.getProperty("file.separator") + ".els";
    }

    /**
     * Get full path to ELS Jar including Jar file
     *
     * @return String Full path to ELS.jar
     */
    public String getElsJar()
    {
        String path = getElsJarPath() + System.getProperty("file.separator") + context.cfg.ELS_JAR;
        return path;
    }

    /**
     * Get path to ELS Jar file
     * <br/>
     * Works with ELS installed using ELS.jar and in development when executing with class files
     * and ELS.jar is in the development deploy directory.
     * <br/><br/>
     * This is the single source for the executable path for the ELS program.
     *
     * @return String Path to directory of ELS.jar
     */
    public String getElsJarPath()
    {
        String jarPath = "";
        try
        {
            jarPath = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            if (jarPath.endsWith(context.cfg.ELS_JAR))
            {
                jarPath = jarPath.substring(0, jarPath.length() - context.cfg.ELS_JAR.length());
            }
            else // hack for development environment to be consistent with the install environment
            {
                String dev = "out" + System.getProperty("file.separator") + "production" + System.getProperty("file.separator") + "ELS";
                if (jarPath.endsWith(dev))
                {
                    jarPath = jarPath.substring(0, jarPath.length() - dev.length());
                    jarPath += "mock/bin";
                }
            }
        }
        catch (Exception e)
        {
            // should never get an exception
        }
        return jarPath;
    }

    /**
     * Gets excluded library names
     *
     * @return the excluded library names collection
     */
    public ArrayList<String> getExcludedLibraryNames()
    {
        return selectedLibraryExcludes;
    }

    /**
     * Get the path to the executable that runs ELS.
     * <br/>
     * Depends on the operating system:<br/>
     * * Linux is rt/bin/java
     * * Windows is ELS-Navigator.exe
     * * macOS is an open to ELS-Navigator.app or ELS-Navigator.sh depending on the Preference
     *
     * @return
     */
    public String getExecutablePath()
    {
        String exePath = getInstalledPath() + System.getProperty("file.separator");
        if (Utils.isOsWindows())
            exePath += "ELS-Navigator.exe";
        else if (Utils.isOsMac())
        {
            if (context.preferences.isMacosLauncher())
                exePath = "open -F -W -n -a " + getInstalledPath() + "/ELS-Navigator.app --args";
            else
                exePath += "ELS-Navigator.sh";
        }
        else
            exePath += "ELS-Navigator.sh";

        return exePath;
    }

    /**
     * Gets the export collection filename
     *
     * @return the export filename
     */
    public String getExportCollectionFilename()
    {
        return exportCollectionFilename;
    }

    /**
     * Gets the export text filename
     *
     * @return exportTextFilename the export text filename
     */
    public String getExportTextFilename()
    {
        return exportTextFilename;
    }

    /**
     * Get the full path to the subscriber file
     * @param filename
     * @return
     */
    public String getFullPathSubscriber(String filename)
    {
        String path;
        if (isRelativePath(filename))
            path = getWorkingDirectorySubscriber() + context.subscriberRepo.getSeparator() + filename;
        else
            path = filename;
        if (path.matches("^[a-zA-Z]:.*"))
            path = "/" + path;
        return path;
    }

    /**
     * Get either the Hint Tracker or HintDaemon filename, whichever is defined
     *
     * @return String filename
     */
    public String getHintHandlerFilename()
    {
        if (getHintsDaemonFilename().length() > 0)
            return getHintsDaemonFilename();
        return getHintTrackerFilename();
    }

    /**
     * Gets Hint Keys filename
     *
     * @return String filename
     */
    public String getHintKeysFile()
    {
        return hintKeysFile;
    }

    /**
     * Get the Hint Status Tracker configuration filename
     *
     * @return String filename
     */
    public String getHintTrackerFilename()
    {
        return hintTrackerFilename;
    }

    /**
     * Gets Hint Status Server filename
     *
     * @return String filename
     */
    public String getHintsDaemonFilename()
    {
        return hintsDaemonFilename;
    }

    /**
     * Gets the path to the ELS icon
     *
     * @return String Fully-qualified path
     */
    public String getIconPath()
    {
        String path = "";
        String jarPath = getElsJarPath();
        String ext = (Utils.getOS().equalsIgnoreCase("Windows") ? ".ico" : ".png");
        path = jarPath + System.getProperty("file.separator") + ELS_ICON + ext;
        return path;
    }

    /**
     * Get the path to the software installation directory
     * <br/>
     * The software installation directory may be either combined or
     * separate from the runtime configuration directory where all the
     * supporting data files are stored.
     *
     * @return Path to software directory
     */
    public String getInstalledPath()
    {
        String path = Main.class.getResource("Main.class").toExternalForm();
        if (path.startsWith("jar")) // if a Jar then same directory
        {
            try
            {
                path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                path = FilenameUtils.getPath(path);
                if (path.endsWith("bin" + System.getProperty("file.separator")))
                {
                    path = path.substring(0, path.length() - 5);
                }
                if (path.endsWith(System.getProperty("file.separator")))
                {
                    path = path.substring(0, path.length() - 1);
                }
                path = System.getProperty("file.separator") + path;
            }
            catch (Exception e)
            {
            }
        }
        else
        {
            path = getWorkingDirectory(); // otherwise the working directory/bin
        }
        return path;
    }

    /**
     * Get IP address whitelist filename
     *
     * @return Filename or empty string
     */
    public String getIpWhitelist()
    {
        return iplist;
    }

    /**
     * Gets the job name
     *
     * @return the job name
     */
    public String getJobName()
    {
        return jobName;
    }

    /**
     * Gets the full path to the log file
     *
     * @return String Fully-qualified path to log file
     */
    public String getLogFileFullPath()
    {
        return logFileFullPath;
    }

    /**
     * Gets log filename
     *
     * @return the log filename
     */
    public String getLogFileName()
    {
        return logFileName;
    }

    /**
     * Gets any relative path to the log file
     *
     * @return String Relative path if any, otherwise an empty string
     */
    public String getLogFilePath()
    {
        return logFilePath;
    }

    /**
     * Gets the configured scale for formatting long values, 1024 or 1000
     *
     * @return long Scale for formatting
     */
    public double getLongScale()
    {
        return longScale;
    }

    /**
     * Gets mismatch filename
     *
     * @return the mismatch filename
     */
    public String getMismatchFilename()
    {
        return mismatchFilename;
    }

    /**
     * Get the Navigator name
     *
     * @return String
     */
    public String getNavigatorName()
    {
        return NAVIGATOR_NAME;
    }

    /**
     * Gets remote flag
     *
     * @return the remote flag
     */
    public int getOperation()
    {
        return this.operation;
    }

    /**
     * Gets the original command line as a String
     *
     * @return String command line
     */
    public String getOriginalCommandline()
    {
        String cmd = "";
        for (int index = 0; index < originalArgs.length; ++index)
        {
            if (StringUtils.isAsciiPrintable(originalArgs[index])) // handle JDK arguments bug
                cmd += originalArgs[index] + " ";
        }
        return cmd;
    }

    /**
     * Get override of subscriber host value
     *
     * @return String true if -O | --override-host is enabled for listen address, address:ip if enabled as custom, otherwise empty string
     */
    public String getOverrideSubscriberHost()
    {
        return overrideSubscriberHost;
    }

    /**
     * Gets PatternLayout for log4j2
     * <p>
     * Call this method AFTER setDebugLevel() has been called.
     *
     * @return the PatternLayout to use
     */
    public String getPattern()
    {
        String withMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m [%t]:%C.%M:%L%n";
        String withoutMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m%n";
        if (getDebugLevel().trim().equalsIgnoreCase("info"))
        {
            return withoutMethod;
        }
        return withMethod;
    }

    /**
     * Gets publisher import filename
     *
     * @return the publisher import filename
     */
    public String getPublisherCollectionFilename()
    {
        return publisherCollectionFilename;
    }

    /**
     * Get the publisher file name
     * <br/>
     * Either the Collection or Libraries file name is returned depending on which is defined.
     * To get one or the other specifically use the appropriate direct method.
     *
     * @return the publisher file name being used
     */
    public String getPublisherFilename()
    {
        if (getPublisherCollectionFilename() != null && getPublisherCollectionFilename().length() > 0)
            return getPublisherCollectionFilename();
        return getPublisherLibrariesFileName();
    }

    /**
     * Gets publisher configuration file name
     *
     * @return the publisher configuration file name
     */
    public String getPublisherLibrariesFileName()
    {
        return publisherLibrariesFileName;
    }

    /**
     * Gets remote type
     *
     * @return the remote type from the command line
     */
    public String getRemoteType()
    {
        return getRemoteType(this.operation);
    }

    /**
     * Gets remote type based on operation
     *
     * @return the remote type from the command line
     */
    public String getRemoteType(int operation)
    {
        // STATUS_SERVER 6, STATUS_SERVER_FORCE_QUIT 7 and SUBSCRIBER_SERVER_FORCE_QUIT 9 are not remote modes
        String op = "-";
        switch (operation)
        {
            case NOT_REMOTE: // 0
                op = "-";
                break;
            case PUBLISHER_LISTENER: // 4
                op = "L";
                break;
            case PUBLISHER_MANUAL: // 3
                op = "M";
                break;
            case PUBLISH_REMOTE: // 1
                op = "P";
                break;
            case SUBSCRIBER_LISTENER: // 2
                op = "S";
                break;
            case SUBSCRIBER_TERMINAL: // 5
                op = "T";
                break;
        }
        return op;
    }

    /**
     * Gets remote type long name based on operation
     *
     * @return the remote type long name from the command line
     */
    public String getRemoteTypeName(int operation)
    {
        String name = "-";
        switch (operation)
        {
            case NOT_REMOTE: // 0
                name = "Not remote";
                break;
            case PUBLISHER_LISTENER: // 4
                name = "Publisher listener";
                break;
            case PUBLISHER_MANUAL: // 3
                name = "Publisher manual";
                break;
            case PUBLISH_REMOTE: // 1
                name = "Publisher remote";
                break;
            case SUBSCRIBER_LISTENER: // 2
                name = "Subscriber listener";
                break;
            case SUBSCRIBER_TERMINAL: // 5
                name = "Subscriber terminal";
                break;
            case STATUS_SERVER: // 6
                name = "Status server";
                break;
            case STATUS_SERVER_FORCE_QUIT: // 7
                name = "Status server force quit";
                break;
            case JOB_PROCESS:
                name = "Job";
                break;
            case SUBSCRIBER_LISTENER_FORCE_QUIT: // 9
                name = "Subscriber listener force quit";
                break;
        }
        return name;
    }

    /**
     * Gets publisher library names
     *
     * @return the publisher library names collection
     */
    public ArrayList<String> getSelectedLibraryNames()
    {
        return selectedLibraryNames;
    }

    /**
     * Gets subscriber import filename
     *
     * @return the import filename
     */
    public String getSubscriberCollectionFilename()
    {
        return subscriberCollectionFilename;
    }

    /**
     * Get the subscriber file name
     * <br/>
     * Either the Collection or Libraries file name is returned depending on which is defined.
     * To get one or the other specifically use the appropriate direct method.
     *
     * @return the subscriber file name being used
     */
    public String getSubscriberFilename()
    {
        if (getSubscriberCollectionFilename() != null && getSubscriberCollectionFilename().length() > 0)
            return getSubscriberCollectionFilename();
        return getSubscriberLibrariesFileName();
    }

    /**
     * Gets subscriber configuration file name
     *
     * @return the subscriber configuration file name
     */
    public String getSubscriberLibrariesFileName()
    {
        return subscriberLibrariesFileName;
    }

    /**
     * Gets targets filename
     *
     * @return the targets filename
     */
    public String getTargetsFilename()
    {
        return targetsFilename;
    }

    /**
     * Gets the default URL prefix for updates if update.info file not found
     *
     * @return String Default URL prefix to deploy folder on GitHub
     */
    public String getUrlPrefix()
    {
        return URL_PREFIX;
    }

    /**
     * Gets whats new filename
     *
     * @return the whats new filename
     */
    public String getWhatsNewFilename()
    {
        return whatsNewFilename;
    }

    /**
     * Gets the working directory of ELS
     * <br/>
     * This is the root of the ELS directory.
     *
     * @return String Fully-qualified working directory
     */
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    /**
     * Get the (remote) subscriber working directory
     *
     * @return String Subscriber working directory
     */
    public String getWorkingDirectorySubscriber()
    {
        if (workingDirectorySubscriber.isEmpty())
            return workingDirectory;
        return workingDirectorySubscriber;
    }

    /**
     * Return locale bundle string
     *
     * @return String from built-in locale
     */
    public String gs(String key)
    {
        String value = "";
        try
        {
            if (currentBundle == null)
                value = key;
            else
                value = currentBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            value = key;
        }
        return value;
    }

    /**
     * Print a logger.info() line from a configuration indicator
     * <br/><br/>
     * An indicator is an int value representing a boolean: -1 = not set,
     * 0 = false, 1 = true
     *
     * @param logger    The logger singleton
     * @param SHORT     The SHORT format
     * @param message   The message to print
     * @param indicator The indicator
     */
    private void indicator(Logger logger, Marker SHORT, String message, int indicator)
    {
        if (indicator >= 0)
        {
            String value = indicator == 0 ? "false" : "true";
            logger.info(SHORT, message + value);
        }
    }

    /**
     * Is the scaling factor binary?
     *
     * @return true == 1024, false == 1000
     */
    public boolean isBinaryScale()
    {
        return (getLongScale() == 1024.0 ? true : false);
    }

    /**
     * Is this a -V check for update operation?
     *
     * @return true == check only, false == not a check for update operation
     */
    public boolean isCheckForUpdate()
    {
        return checkForUpdate;
    }

    /**
     * Is a duplicates cross-check enabled?
     *
     * @return true if enabled, else false
     */
    public boolean isCrossCheck()
    {
        return crossCheck == 1 ? true : false;
    }

    /**
     * Is the Navigator the default operation?
     *
     * @return true == run Navigator, false == Navigator is not default
     */
    public boolean isDefaultNavigator()
    {
        return defaultNavigator;
    }

    /**
     * Is dry run boolean
     *
     * @return the boolean
     */
    public boolean isDryRun()
    {
        return dryRun == 1 ? true : false;
    }

    /**
     * Are duplicates being checked?
     *
     * @return true if duplcates checking is enabled, else false
     */
    public boolean isDuplicateCheck()
    {
        return duplicateCheck == 1 ? true : false;
    }

    /**
     * Are empty directories being checked?
     *
     * @return true if empty directory checking is enabled, else false
     */
    public boolean isEmptyDirectoryCheck()
    {
        return emptyDirectoryCheck == 1 ? true : false;
    }

    /**
     * Is the current library one that has been excluded on the command line?
     *
     * @return isSelected true/false
     */
    public boolean isExcludedLibrary(String name)
    {
        for (String library : selectedLibraryExcludes)
        {
            if (library.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Is this a forced collection operation?
     *
     * @return true/false
     */
    public boolean isForceCollection()
    {
        return forceCollection == 1 ? true : false;
    }

    /**
     * Is this a forced targets operation?
     *
     * @return true/false
     */
    public boolean isForceTargets()
    {
        return forceTargets == 1 ? true : false;
    }

    /**
     * Is the Navigator GUI to be used, either logger view or Navigator?
     *
     * @return true if GUI is to be used
     */
    public boolean isGui()
    {
        return (loggerView || navigator == 1 ? true : false);
    }

    /**
     * Are only Hints being processed so skip the main munge process?
     *
     * @return true if hints skipping main process enabled
     */
    public boolean isHintSkipMainProcess()
    {
        return hintSkipMainProcess == 1 ? true : false;
    }

    /**
     * Is a Hint Status Tracker or Status Server being used?
     *
     * @return true if so
     */
    public boolean isHintTrackingEnabled()
    {
        return (hintTrackerFilename.length() > 0 || hintsDaemonFilename.length() > 0);
    }

    /**
     * Should ignored files be reported?
     *
     * @return true to report ignored files
     */
    public boolean isIgnoredReported()
    {
        return ignoredReported == 1 ? true : false;
    }

    /**
     * Is this an install updates then exit operation?
     *
     * @return true == install updates then exit
     */
    public boolean isInstallUpdate()
    {
        return installUpdate;
    }

    /**
     * For a Publisher a true keep going flag skips sending
     * the quit command to the subscriber when the operation is
     * complete. For a subscriber it skips ending with a fault
     * on an unexpected disconnect (EOL).
     *
     * @return true to keep going
     */
    public boolean isKeepGoing()
    {
        return keepGoing == 1 ? true : false;
    }

    /**
     * Is the log to be overwritten?
     *
     * @return true to overwrite
     */
    public boolean isLogOverwrite()
    {
        return logOverwrite == 1 ? true : false;
    }

    /**
     * Is ELS running in logger view?
     *
     * @return True if logger mode
     */
    public boolean isLoggerView()
    {
        return loggerView;
    }

    /**
     * Is this session the Navigator?
     *
     * @return true if a Navigator session
     */
    public boolean isNavigator()
    {
        return navigator == 1 ? true : false;
    }

    /**
     * Is the no back-fill option enabled so the default behavior of filling-in
     * original locations with new files is disabled?
     *
     * @return true if no back-fill is enabled
     */
    public boolean isNoBackFill()
    {
        return noBackFill == 1 ? true : false;
    }

    /**
     * Is the Hint Server host value overridden so the listen value should be used?
     *
     * @return True if the listen value is to be used
     */
    public boolean isOverrideHintsHost()
    {
        return overrideHintHost == 1 ? true : false;
    }

    /**
     * Gets overwrite mode
     *
     * @return true/false
     */
    public boolean isOverwrite()
    {
        return overwrite == 1 ? true : false;
    }

    /**
     * Are file dates to be preserved between publisher and subscriber?
     * <br/>
     * Note directory dates are not preserved.
     *
     * @return true if dates are to be preserved
     */
    public boolean isPreserveDates()
    {
        return preserveDates == 1 ? true : false;
    }

    /**
     * Is this a publish operation?
     *
     * @return true/false
     */
    public boolean isPublishOperation()
    {
        return (operation == NOT_REMOTE || operation == PUBLISH_REMOTE) ? true : false;
    }

    /**
     * Returns true if this publisher is in listener mode
     *
     * @return true/false
     */
    public boolean isPublisherListener()
    {
        return (getOperation() == PUBLISHER_LISTENER);
    }

    /**
     * Returns true if this publisher is in terminal mode
     *
     * @return true/false
     */
    public boolean isPublisherTerminal()
    {
        return (getOperation() == PUBLISHER_MANUAL);
    }

    /**
     * Should the current process command the Hint Status Server to quit?
     *
     * @return true if the command is to be sent
     */
    public boolean isQuitStatusServer()
    {
        return quitStatusServer == 1 ? true : false;
    }

    /**
     * Force the (remote) subscriber to quit, then end.
     *
     * @return true if this is to send Quit to subscriber, then end
     */
    public boolean isQuitSubscriberListener()
    {
        return quitSubscriberListener == 1 ? true : false;
    }

    /**
     * Is the path relative or absolute?
     *
     * @param path Path to check
     * @return true if a relative path, false if an absolute fully-qualified path
     */
    public boolean isRelativePath(String path)
    {
        // from Utils but here for use during initialization because the logger is not configured yet
        if (path.matches("^[a-zA-Z]:.*"))
            return false;
        if (path.startsWith("/") || path.startsWith("\\") || path.startsWith("|"))
            return false;
        return true;
    }

    /**
     * Is either a remote Subscriber or Hint Server active?
     *
     * @return True if active
     */
    public boolean isRemoteActive()
    {
        if (isRemoteStatusServer())
            return true;
        if (isRemoteSubscriber())
            return true;
        return false;
    }

    /**
     * Returns true if this is any type of remote session
     *
     * @return true/false
     */
    public boolean isRemoteOperation()
    {
        return (getOperation() != NOT_REMOTE && getOperation() != JOB_PROCESS);
    }

    /**
     * Returns true if this is a publisher process, automatically execute the process
     *
     * @return true/false
     */
    public boolean isRemotePublishOperation()
    {
        return (getOperation() == PUBLISH_REMOTE);
    }

    /**
     * Is this instance operating as a remote Hint Server?
     *
     * @return True if this is a remote Hint Server
     */
    public boolean isRemoteStatusServer()
    {
        if (context.hintsStty != null && context.hintsStty.isConnected())
            return true;
        return false;
    }

    /**
     * Is this instance operating as a remote Subscriber?
     *
     * @return True if a remote Subscriber
     */
    public boolean isRemoteSubscriber()
    {
        if (context.clientStty != null && context.clientStty.isConnected())
            return true;
        return false;
    }

    /**
     * Is this a request collection operation?
     *
     * @return true/false
     */
    public boolean isRequestCollection()
    {
        return requestCollection == 1 ? true : false;
    }

    /**
     * Is this a request targets operation?
     *
     * @return true/false
     */
    public boolean isRequestTargets()
    {
        return requestTargets == 1 ? true : false;
    }

    /**
     * Is the current library one that has been specified on the command line?
     *
     * @return isSelected true/false
     */
    public boolean isSelectedLibrary(String name)
    {
        for (String library : selectedLibraryNames)
        {
            if (library.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Is specific publisher library exclude flag set?
     */
    public boolean isSpecificExclude()
    {
        return this.specificExclude == 1 ? true : false;
    }

    /**
     * Is specific publisher library boolean.
     *
     * @return the boolean
     */
    public boolean isSpecificLibrary()
    {
        return specificLibrary == 1 ? true : false;
    }

    /**
     * Returns true if this is a hint status server
     *
     * @return true/false
     */
    public boolean isStatusServer()
    {
        return (getOperation() == STATUS_SERVER);
    }

    /**
     * Returns true if subscriber is in listener mode
     */
    public boolean isSubscriberListener()
    {
        return (getOperation() == SUBSCRIBER_LISTENER);
    }

    /**
     * Returns true if this subscriber is in terminal mode
     */
    public boolean isSubscriberTerminal()
    {
        return (getOperation() == SUBSCRIBER_TERMINAL);
    }

    /**
     * Have targets been enabled?
     */
    public boolean isTargetsEnabled()
    {
        return targetsEnabled == 1 ? true : false;
    }

    /**
     * Did the update file?
     */
    public boolean isUpdateFailed()
    {
        return updateFailed;
    }

    /**
     * Is this a restarted instance after being updated?
     */
    public boolean isUpdateSuccessful()
    {
        return updateSuccessful;
    }

    /**
     * Is the validation of collections and targets enabled?
     *
     * @return true if validation should be done
     */
    public boolean isValidation()
    {
        return validation == 1 ? true : false;
    }

    /**
     * Is What's New an all option?
     *
     * @return true/false
     */
    public boolean isWhatsNewAll()
    {
        return this.whatsNewAll == 1 ? true : false;
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
        loadLocale(filePart, context.cfg);
    }

    /**
     * Load a locale and set current locale bundle
     * <br/>
     * Requires the abbreviated language_country part of the locale filename, e.g. en_US.
     * It must be one of the built-in locale files. If not available en_US is used.
     *
     * @param filePart Locale file end
     * @param config   Configuration
     */
    public void loadLocale(String filePart, Configuration config)
    {
        // load the language file if available
        if (!Arrays.asList(Configuration.availableLocales).contains(filePart))
        {
            filePart = "en_US"; // default locale
        }
        config.setCurrentBundle(ResourceBundle.getBundle("com.corionis.els.locales.bundle_" + filePart));
    }

    /**
     * Make a full path to a publisher or subscriber library file
     *
     * @param repo Repository of desired file
     * @param filename The repository filename
     *
     * @return The full path to the file
     */
    public String makeFullPath(Repository repo, String filename)
    {
        String path = filename;
        if (Utils.isRelativePath(filename))
        {
            if (repo.getPurpose() == Repository.PUBLISHER)
                path = getWorkingDirectory();
            else if (repo.getPurpose() == Repository.SUBSCRIBER)
                path = getWorkingDirectorySubscriber();
            path = path + repo.getSeparator() + filename;
        }
        return path;
    }

    /**
     * Make a relative path based on the current working directory
     *
     * @param path
     * @return A path relative to the working directory, if possible
     */
    public String makeRelativePath(String path)
    {
        if (path != null && path.length() > 0)
        {
            path = Utils.makeRelativePath(getWorkingDirectory(), path);
            path = Utils.pipe(path);
            path = Utils.unpipe(path, "/");
        }
        else
            path = "";
        return path;
    }

    /**
     * Make a relative path based on the current working directory of the Subscriber
     *
     * @param path
     * @return A path relative to the working directory of Subscriber, if possible
     */
    public String makeRelativePathSubscriber(String path)
    {
        if (path != null && path.length() > 0)
            path = Utils.makeRelativePath(getWorkingDirectorySubscriber(), path);
        else
            path = "";
        return path;
    }

    /**
     * Parse command line
     * <p>
     * This populates the rest.
     *
     * @param args the args
     * @throws MungeException the els exception
     */
    public void parseCommandLine(String[] args) throws MungeException
    {
        // Reserved:
        //   M match dates
        //   R restrict Hint processing, i.e. do not execute
        //   U user authentication & authorization
        //   X execute Hints only. -X -R checks for Hints but does not execute
        //   Z verify connectivity (only)

        int index;
        originalArgs = args;
        for (index = 0; index < args.length; ++index)
        {
            switch (args[index])
            {
                case "-A":                                              // authentication keys for publisher/subscriber listeners
                case "--auth-keys":
                    if (index <= args.length - 2)
                    {
                        setAuthKeysFile(args[index + 1].trim());
                        verifyFileExistence(getAuthKeysFile());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.a.requires.an.els.authentication.keys.filename"));
                    }
                    break;
                case "-a":                                             // authorize mode password
                case "--authorize":
                    if (index <= args.length - 2)
                    {
                        setAuthorizedPassword(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.a.requires.a.password.value"));
                    }
                    break;
                case "-b":                                             // disable back-filling
                case "--no-back-fill":
                    setNoBackFill(true);
                    break;
                case "-B":                                             // blacklist
                case "--blacklist":
                    if (index <= args.length - 2)
                    {
                        setBlacklist(args[index + 1].trim());
                        verifyFileExistence(getBlacklist());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.b.requires.a.blacklist.filename"));
                    }
                    break;
                case "-c":                                             // console level
                case "--console-level":
                    if (index <= args.length - 2)
                    {
                        setConsoleLevel(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.c.requires.a.level.trace.debug.info.warn.error.fatal.or.off"));
                    }
                    break;
                case "-C":
                case "--config":
                    if (index <= args.length - 2)
                    {
                        // see configure()
                        setWorkingDirectory(args[index + 1].trim());
                        verifyFileExistence(getWorkingDirectory());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.c.requires.a.directory.path"));
                    }
                    break;
                case "-d":                                             // debug level
                case "--debug-level":
                    if (index <= args.length - 2)
                    {
                        setDebugLevel(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.d.requires.a.level.trace.debug.info.warn.error.fatal.or.off"));
                    }
                    break;
                case "-D":                                             // Dry run
                case "--dry-run":
                    setDryRun(true);
                    break;
                case "--dump-system":
                    System.out.println("ELS version " + getBuildVersionName() + ", " + getBuildDate() + System.getProperty("line.separator"));
                    System.getProperties().list(System.out);
                    System.exit(1);
                    break;
                case "-e":                                             // export publisher items to flat text file
                case "--export-text":
                    if (index <= args.length - 2)
                    {
                        setExportTextFilename(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.e.requires.an.export.path.output.filename"));
                    }
                    break;
                case "-E":                                             // publisher empty directory check
                case "--empty-directories":
                    setEmptyDirectoryCheck(true);
                    break;
                case "-f":                                             // log filename
                case "-F":
                case "--log-file":
                case "--log-overwrite":
                    if (getLogFileName().length() > 0)
                        throw new MungeException(context.cfg.gs("Cfg.error.f.and.f.cannot.be.used.at.the.same.time"));
                    if (args[index].equals("-F") || args[index].equals("--log-overwrite"))
                        setLogOverwrite(true);
                    if (index <= args.length - 2)
                    {
                        setLogFileName(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.f.requires.a.log.filename"));
                    }
                    break;
                case "-g":                                              // publisher and subscriber keep subscriber going
                case "--listener-keep-going":
                    setKeepGoing(true);
                    break;
                case "-G":                                              // tell listener to quit right now, then end
                case "--listener-quit":
                    setQuitSubscriberListener(true);
                    break;
                case "-h":                                              // hint status tracker
                case "--hints":
                    if (index <= args.length - 2)
                    {
                        setHintTrackerFilename(args[index + 1].trim());
                        verifyFileExistence(getHintTrackerFilename());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.h.requires.a.hint.status.tracker.repository.filename"));
                    }
                    break;
                case "-H":                                              // hint status server
                case "--hint-server":
                    if (index <= args.length - 2)
                    {
                        setHintsDaemonFilename(args[index + 1].trim());
                        verifyFileExistence(getHintsDaemonFilename());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.h.requires.a.hint.status.server.repository.filename"));
                    }
                    break;
                case "-i":                                             // export publisher items to collection file
                case "--export-items":
                    if (index <= args.length - 2)
                    {
                        setExportCollectionFilename(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.i.requires.a.collection.output.filename"));
                    }
                    break;
                case "-I":                                             // whitelist
                case "--ip-whitelist":
                    if (index <= args.length - 2)
                    {
                        setIplist(args[index + 1].trim());
                        verifyFileExistence(getIpWhitelist());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.i.requires.an.ip.whitelist.filename"));
                    }
                    break;
                case "-j":                                             // Job
                case "--job":
                    if (index <= args.length - 2)
                    {
                        setJobName(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.j.requires.a.job.name"));
                    }
                    break;
                case "-J":                                              // override Hint Server host (use listen)
                case "--override-hints-host":
                    setOverrideHintsHost(true);
                    break;
                case "-k":                                             // ELS keys file
                case "--keys":
                    if (index <= args.length - 2)
                    {
                        setHintKeysFile(args[index + 1].trim());
                        verifyFileExistence(getHintKeysFile());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.k.requires.an.els.hint.keys.filename"));
                    }
                    break;
                case "-K":                                             // ELS keys file and skip main process munge
                case "--keys-only":
                    if (index <= args.length - 2)
                    {
                        setHintKeysFile(args[index + 1].trim());
                        verifyFileExistence(getHintKeysFile());
                        ++index;
                        setHintSkipMainProcess(true);
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.k1.requires.an.els.hint.keys.filename"));
                    }
                    break;
                case "-l":                                             // publisher library to process
                case "--library":
                    if (index <= args.length - 2)
                    {
                        addPublisherLibraryName(args[index + 1].trim());
                        setSpecificLibrary(true);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.l.requires.a.publisher.library.name"));
                    }
                    break;
                case "-L":                                             // publisher library to exclude
                case "--exclude":
                    if (index <= args.length - 2)
                    {
                        addExcludedLibraryName(args[index + 1].trim());
                        setSpecificExclude(true);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.l.requires.a.publisher.library.name.to.exclude"));
                    }
                    break;
                case "--logger":
                    setLoggerView(true);
                    break;
                case "-m":                                             // Mismatch output filename
                case "--mismatches":
                    if (index <= args.length - 2)
                    {
                        setMismatchFilename(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.m.requires.a.mismatches.output.filename"));
                    }
                    break;
                case "--marker":                                        // marker
                    if (index <= args.length - 2)                       // ignore any value
                    {
                        marker = args[index + 1].trim();
                        ++index;
                    }
                    else
                        marker = context.cfg.gs("Cfg.marker.enabled");
                    break;
                case "-n":                                              // Navigator
                case "--navigator":
                    setNavigator(true);
                    break;
                case "-N":                                              // ignored files reported
                case "--ignored":
                    setIgnoredReported(true);
                    break;
                case "-o":                                              // overwrite
                case "--overwrite":
                    setOverwrite(true);
                    break;
                case "-O":                                              // override Subscriber host (use listen if no argument)
                case "--override-subscriber-host":
                    if (index <= args.length - 2 && !args[index + 1].startsWith("-"))
                    {
                        setOverrideSubscriberHost(args[index + 1].trim());
                        ++index;
                    }
                    else
                        setOverrideSubscriberHost("true");
                    break;
                case "-p":                                              // publisher JSON libraries file
                case "--publisher-libraries":
                    if (index <= args.length - 2)
                    {
                        setPublisherLibrariesFileName(args[index + 1].trim());
                        verifyFileExistence(getPublisherLibrariesFileName());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.p.requires.a.publisher.libraries.filename"));
                    }
                    break;
                case "-P":                                             // publisher JSON collection items file
                case "--publisher-collection":
                    if (index <= args.length - 2)
                    {
                        setPublisherCollectionFilename(args[index + 1].trim());
                        verifyFileExistence(getPublisherCollectionFilename());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.p.requires.a.publisher.collection.filename"));
                    }
                    break;
                case "-q":                                             // tell status server to quit when done
                case "--quit-status":
                    setQuitStatusServer(true);
                    break;
                case "-Q":                                             // tell status server to quit right now, then end
                case "--force-quit":
                    setQuitStatusServer(true);
                    break;
                case "-r":                                             // remote session
                case "--remote":
                    if (index <= args.length - 2)
                    {
                        remoteArg = args[index + 1].trim();
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.r.must.be.followed.by.p.l.m.s.t.case.insensitive"));
                    }
                    break;
                case "-s":                                             // subscriber JSON libraries file
                case "--subscriber-libraries":
                    if (index <= args.length - 2)
                    {
                        setForceCollection(false);
                        setRequestCollection(true);
                        setSubscriberLibrariesFileName(args[index + 1].trim());
                        verifyFileExistence(getSubscriberLibrariesFileName());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.s.requires.a.subscriber.libraries.filename"));
                    }
                    break;
                case "-S":                                             // subscriber JSON collection items file
                case "--subscriber-collection":
                    if (index <= args.length - 2)
                    {
                        setForceCollection(true);
                        setRequestCollection(false);
                        setSubscriberCollectionFilename(args[index + 1].trim());
                        verifyFileExistence(getSubscriberCollectionFilename());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.s.requires.an.subscriber.collection.filename"));
                    }
                    break;
                case "-t":                                             // targets filename
                case "--targets":
                    setTargetsEnabled(true);
                    setForceTargets(false);
                    setRequestTargets(true);
                    if (index <= args.length - 2 && !args[index + 1].startsWith("-"))
                    {
                        setTargetsFilename(args[index + 1].trim());
                        verifyFileExistence(getTargetsFilename());
                        ++index;
                    }
                    break;
                case "-T":                                             // targets filename - force to publisher
                case "--force-targets":
                    setTargetsEnabled(true);
                    setForceTargets(true);
                    setRequestTargets(false);
                    if (index <= args.length - 2 && !args[index + 1].startsWith("-"))
                    {
                        setTargetsFilename(args[index + 1].trim());
                        verifyFileExistence(getTargetsFilename());
                        ++index;
                    }
                    break;
                case "-u":                                             // publisher duplicate check
                case "--duplicates":
                    setDuplicateCheck(true);
                    break;
                case "--update-failed":
                    setUpdateFailed(true);
                    break;
                case "--update-successful":
                    setUpdateSuccessful();
                    break;
                case "-v":                                             // validation run
                case "--validate":
                    setValidation(true);
                    break;
                case "-V":                                              // check for update then exit
                case "--check-update":
                    setCheckForUpdate(true);
                    break;
                case "--version":                                       // version
                    System.out.println("");
                    System.out.println(PROGRAM_NAME + context.cfg.gs("Main.version") + getBuildVersionName() + ", " + getBuildDate());
                    System.out.println(context.cfg.gs("Cfg.see.the.els.wiki.on.github.for.documentation.at"));
                    System.out.println("  https://github.com/Corionis/ELS/wiki");
                    System.out.println("");
                    System.exit(1);
                    break;
                case "-w":                                             // What's New output filename
                case "--whatsnew":
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1].trim());
                        ++index;
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.w.requires.a.what.s.new.output.filename"));
                    }
                    break;
                case "-W":                                             // What's New output filename, set all option
                case "--whatsnew-all":
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1].trim());
                        ++index;
                        setWhatsNewAll(true);
                    }
                    else
                    {
                        throw new MungeException(context.cfg.gs("Cfg.error.w1.requires.a.what.s.new.output.filename"));
                    }
                    break;
                case "-x":                                             // cross-library duplicate check
                case "--cross-check":
                    setCrossCheck(true);
                    break;
                case "-y":                                             // preserve file dates
                case "--preserve-dates":
                    setPreserveDates(true);
                    break;
                case "-Y":                                             // install update then exit
                case "--install-update":
                    setInstallUpdate(true);
                    break;
                case "-z":                                             // scale long values with 1000 instead of 1024
                case "--decimal-scale":
                    setLongScale(false);
                    break;
                default:
                    if (StringUtils.isAsciiPrintable(args[index])) // handle JDK arguments bug
                    {
                        if (!args[index].endsWith(".exe") && !args[index].endsWith(".app"))
                        {
                            context.fault = true;
                            throw new MungeException(context.cfg.gs("Cfg.error.unknown.option") + args[index]);
                        }
                    }
            }
        }
    }

    /**
     * Set the Authentication Keys filename
     *
     * @param authKeysFile
     */
    public void setAuthKeysFile(String authKeysFile)
    {
        this.authKeysFile = authKeysFile;
    }

    /**
     * Sets Authorized password
     *
     * @param password the password required to access Authorized mode with a ClientStty
     */
    public void setAuthorizedPassword(String password)
    {
        this.authorizedPassword = password;
    }

    /**
     * Sets the blacklist path & filename
     *
     * @param blacklist Full or relative path to blacklist file
     */
    public void setBlacklist(String blacklist)
    {
        this.blacklist = blacklist;
    }

    /**
     * Set the flag indicating this is a Check for Update option
     *
     * @param checkForUpdate
     */
    public void setCheckForUpdate(boolean checkForUpdate)
    {
        this.checkForUpdate = checkForUpdate;
    }

    /**
     * Sets console level
     *
     * @param consoleLevel the console level
     */
    public void setConsoleLevel(String consoleLevel)
    {
        this.consoleLevel = consoleLevel;
        consoleSet = true;
    }

    /**
     * Sets duplicates cross-check
     *
     * @param crossCheck
     */
    public void setCrossCheck(boolean crossCheck)
    {
        this.crossCheck = crossCheck ? 1 : 0;
    }

    /**
     * Set the current resource bundle for translations
     *
     * @param bundle
     */
    public void setCurrentBundle(ResourceBundle bundle)
    {
        currentBundle = bundle;
    }

    /**
     * Sets debug level
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel)
    {
        this.debugLevel = debugLevel;
        debugSet = true;
    }

    /**
     * Set this instance as defaulting to Navigator mode
     *
     * @param defaultNavigator
     */
    public void setDefaultNavigator(boolean defaultNavigator)
    {
        this.defaultNavigator = defaultNavigator;
        this.setNavigator(true);
    }

    /**
     * Sets dry run
     *
     * @param dryRun true/false boolean
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun ? 1 : 0;
    }

    /**
     * Sets duplcates checking
     *
     * @param duplicateCheck
     */
    public void setDuplicateCheck(boolean duplicateCheck)
    {
        this.duplicateCheck = duplicateCheck ? 1 : 0;
    }

    /**
     * Sets empty directory checking
     *
     * @param emptyDirectoryCheck
     */
    public void setEmptyDirectoryCheck(boolean emptyDirectoryCheck)
    {
        this.emptyDirectoryCheck = emptyDirectoryCheck ? 1 : 0;
    }

    /**
     * Sets export collection filename
     *
     * @param exportCollectionFilename the export collection filename
     */
    public void setExportCollectionFilename(String exportCollectionFilename)
    {
        this.exportCollectionFilename = exportCollectionFilename;
    }

    /**
     * Sets the export text filename
     *
     * @param exportTextFilename the export text filename
     */
    public void setExportTextFilename(String exportTextFilename)
    {
        this.exportTextFilename = exportTextFilename;
    }

    /**
     * Set if this is a forced collection operation
     *
     * @param forceCollection true/false
     */
    public void setForceCollection(boolean forceCollection)
    {
        this.forceCollection = forceCollection ? 1 : 0;
    }

    /**
     * Set if this is a forced targets operation
     *
     * @param forceTargets true/false
     */
    public void setForceTargets(boolean forceTargets)
    {
        this.forceTargets = forceTargets ? 1 : 0;
    }

    /**
     * Set the Hint Keys filename
     *
     * @param hintKeysFile
     */
    public void setHintKeysFile(String hintKeysFile)
    {
        this.hintKeysFile = hintKeysFile;
    }

    /**
     * Sets if the hints option to skip the main munge process is enabled
     *
     * @param hintSkipMainProcess
     */
    public void setHintSkipMainProcess(boolean hintSkipMainProcess)
    {
        this.hintSkipMainProcess = hintSkipMainProcess ? 1 : 0;
    }

    /**
     * Set the Hint Status Tracker configuration filename
     *
     * @param hintTrackerFilename
     */
    public void setHintTrackerFilename(String hintTrackerFilename)
    {
        this.hintTrackerFilename = hintTrackerFilename;
    }

    /**
     * Sets the Hint Status Server filename
     *
     * @param hintsDaemonFilename
     */
    public void setHintsDaemonFilename(String hintsDaemonFilename)
    {
        this.hintsDaemonFilename = hintsDaemonFilename;
    }

    /**
     * Enable to report ignored files, default false
     *
     * @param ignoredReported True to report ignored files
     */
    public void setIgnoredReported(boolean ignoredReported)
    {
        this.ignoredReported = ignoredReported ? 1 : 0;
    }

    /**
     * Set the flag indicating this is an Install Update option
     * @param installUpdate
     */
    public void setInstallUpdate(boolean installUpdate)
    {
        this.installUpdate = installUpdate;
    }

    /**
     * Set the IP address whitelist filename
     *
     * @param iplist Full or relative path to file
     */
    public void setIplist(String iplist)
    {
        this.iplist = iplist;
    }

    /**
     * Sets the job name
     *
     * @param name the configuation name of the job
     */
    public void setJobName(String name)
    {
        this.jobName = name;
    }

    /**
     * For a Publisher the keep going flag skips sending
     * the quit command to the subscriber when the operation is
     * complete. For a subscriber it skips ending with a fault
     * on an unexpected disconnect (EOL).
     *
     * @param keepGoing
     */
    public void setKeepGoing(boolean keepGoing)
    {
        this.keepGoing = keepGoing ? 1 : 0;
    }

    /**
     * Set the full path to the log file
     *
     * @param logFileFullPath
     */
    public void setLogFileFullPath(String logFileFullPath)
    {
        this.logFileFullPath = logFileFullPath;
    }

    /**
     * Sets log filename
     *
     * @param logFileName the log filename
     */
    public void setLogFileName(String logFileName)
    {
        this.logFileName = logFileName;
    }

    /**
     * Set any relative path to the log file
     *
     * @param logFilePath
     */
    public void setLogFilePath(String logFilePath)
    {
        this.logFilePath = logFilePath;
    }

    /**
     * Sets if the log should be overwritten when the process starts
     *
     * @param logOverwrite
     */
    public void setLogOverwrite(boolean logOverwrite)
    {
        this.logOverwrite = logOverwrite ? 1 : 0;
    }

    /**
     * Set this instance as operating in logger mode
     *
     * @param loggerView
     */
    public void setLoggerView(boolean loggerView)
    {
        this.loggerView = loggerView;
    }

    /**
     * Sets the scale factor for formatting long values, 1024 or 1000
     *
     * @param binaryScale true = 1024, false = 1000
     */
    public void setLongScale(boolean binaryScale)
    {
        this.longScale = (binaryScale ? 1024.0 : 1000.0);
    }

    /**
     * Sets mismatch filename
     *
     * @param mismatchFilename the mismatch filename
     */
    public void setMismatchFilename(String mismatchFilename)
    {
        this.mismatchFilename = mismatchFilename;
    }

    /**
     * Set if this is a Navigator operation
     *
     * @param navigator
     */
    public void setNavigator(boolean navigator)
    {
        this.navigator = navigator ? 1 : 0;
    }

    /**
     * Sets the no back-fill option
     *
     * @param noBackFill
     */
    public void setNoBackFill(boolean noBackFill)
    {
        this.noBackFill = noBackFill ? 1 : 0;
    }

    /**
     * Sets the operation value based on the remote type
     *
     * @param remoteType the remote type, if empty string the command line argument is used
     */
    public void setOperation(String remoteType)
    {
        operation = NOT_SET;

        if (remoteType.isEmpty())
            remoteType = remoteArg; // use command line value

        if (!remoteType.isEmpty())
        {
            if (remoteType.equalsIgnoreCase("P")) // 1
                operation = PUBLISH_REMOTE;
            else if (remoteType.equalsIgnoreCase("S")) // 2
                operation = SUBSCRIBER_LISTENER;
            else if (remoteType.equalsIgnoreCase("M")) // 3
                operation = PUBLISHER_MANUAL;
            else if (remoteType.equalsIgnoreCase("L")) // 4
                operation = PUBLISHER_LISTENER;
            else if (remoteType.equalsIgnoreCase("T")) // 5
                operation = SUBSCRIBER_TERMINAL;
            else if (!remoteType.equals("-"))
                remoteType = "-";
        }

        if (getPublisherFilename().length() == 0 &&
                getSubscriberFilename().length() == 0 &&
                getHintsDaemonFilename().length() > 0)
        {
            operation = STATUS_SERVER; // 6
        }

        if (operation == NOT_SET)
        {
            if (!getJobName().isEmpty())
                operation = JOB_PROCESS; // 8
            if (isQuitStatusServer() && operation == NOT_SET)
                operation = STATUS_SERVER_FORCE_QUIT; // 7
            if (isQuitSubscriberListener() && operation == NOT_SET)
                operation = SUBSCRIBER_LISTENER_FORCE_QUIT; // 9
        }

        if (operation == NOT_SET)
            operation = NOT_REMOTE; // 0
    }

    /**
     * Set that this instance is overriding the Hint Server host to use the listen value instead
     *
     * @param overrideHintHost
     */
    public void setOverrideHintsHost(boolean overrideHintHost)
    {
        this.overrideHintHost = overrideHintHost ? 1 : 0;
    }

    /**
     * Set the this instance is overriding the Subscriber host to use the listen value instead
     *
     * @param overrideSubscriberHost
     */
    public void setOverrideSubscriberHost(String overrideSubscriberHost)
    {
        this.overrideSubscriberHost = overrideSubscriberHost;
    }

    /**
     * Sets file overwrite mode
     */
    public void setOverwrite(boolean sense)
    {
        overwrite = sense ? 1 : 0;
    }

    /**
     * Set whether file dates should be preserved between publisher and subscriber
     *
     * @param preserveDates
     */
    public void setPreserveDates(boolean preserveDates)
    {
        this.preserveDates = preserveDates ? 1 : 0;
    }

    /**
     * Sets publisher collection filename
     *
     * @param publisherCollectionFilename the publisher import filename
     */
    public void setPublisherCollectionFilename(String publisherCollectionFilename)
    {
        this.publisherCollectionFilename = publisherCollectionFilename;
    }

    /**
     * Sets publisher libraries file name
     *
     * @param publisherLibrariesFileName the publisher configuration file name
     */
    public void setPublisherLibrariesFileName(String publisherLibrariesFileName)
    {
        this.publisherLibrariesFileName = publisherLibrariesFileName;
    }

    /**
     * Sets whether this process should command the Hint Status Server to quit
     *
     * @param quitStatusServer
     */
    public void setQuitStatusServer(boolean quitStatusServer)
    {
        this.quitStatusServer = quitStatusServer ? 1 : 0;
    }

    /**
     * Sets the flag for the operation to force the subscriber to quit, then end
     *
     * @param quitSubscriberListener
     */
    public void setQuitSubscriberListener(boolean quitSubscriberListener)
    {
        this.quitSubscriberListener = quitSubscriberListener ? 1 : 0;
    }

    /**
     * Set if this is a request collection operation
     *
     * @param requestCollection true/false
     */
    public void setRequestCollection(boolean requestCollection)
    {
        this.requestCollection = requestCollection ? 1 : 0;
    }

    /**
     * Set if this is a request targets operation
     *
     * @param requestTargets true/false
     */
    public void setRequestTargets(boolean requestTargets)
    {
        this.requestTargets = requestTargets ? 1 : 0;
    }

    /**
     * Sets the list of included library names
     *
     * @param includedLibraries
     */
    public void setSelectedLibraryNames(ArrayList<String> includedLibraries)
    {
        selectedLibraryNames = includedLibraries;
    }

    /**
     * Sets specific publisher library exclude flag
     *
     * @param sense true/false
     */
    public void setSpecificExclude(boolean sense)
    {
        this.specificExclude = sense ? 1 : 0;
    }

    /**
     * Sets specific publisher library flag
     *
     * @param sense true/false
     */
    public void setSpecificLibrary(boolean sense)
    {
        this.specificLibrary = sense ? 1 : 0;
    }

    /**
     * Sets subscriber collection filename
     *
     * @param subscriberCollectionFilename the import filename
     */
    public void setSubscriberCollectionFilename(String subscriberCollectionFilename)
    {
        this.subscriberCollectionFilename = subscriberCollectionFilename;
    }

    /**
     * Sets subscriber libraries file name
     *
     * @param subscriberLibrariesFileName the subscriber configuration file name
     */
    public void setSubscriberLibrariesFileName(String subscriberLibrariesFileName)
    {
        this.subscriberLibrariesFileName = subscriberLibrariesFileName;
    }

    /**
     * Set targets enabled
     */
    public void setTargetsEnabled(boolean sense)
    {
        targetsEnabled = sense ? 1 : 0;
    }

    /**
     * Sets targets filename
     *
     * @param targetsFilename the targets filename
     */
    public void setTargetsFilename(String targetsFilename)
    {
        this.targetsFilename = targetsFilename;
    }

    /**
     * Set that an attempted update failed
     *
     * @param updateFailed
     */
    public void setUpdateFailed(boolean updateFailed)
    {
        this.updateFailed = updateFailed;
    }

    /**
     * Sets the updateSuccessful flag provided by the Updater
     */
    public void setUpdateSuccessful()
    {
        updateSuccessful = true;
    }

    /**
     * Sets the collection and targets validation option
     *
     * @param validation
     */
    public void setValidation(boolean validation)
    {
        this.validation = validation ? 1 : 0;
    }

    /**
     * Set What's New all option
     *
     * @param isWhatsNewAll true = all option
     */
    public void setWhatsNewAll(boolean isWhatsNewAll)
    {
        this.whatsNewAll = isWhatsNewAll ? 1 : 0;
    }

    /**
     * Sets whats new filename
     *
     * @param whatsNewFilename the whats new filename
     */
    public void setWhatsNewFilename(String whatsNewFilename)
    {
        this.whatsNewFilename = whatsNewFilename;
    }

    /**
     * Sets the working directory data member
     * <br/>
     * Does not change the working directory.
     *
     * @param workingDirectory
     */
    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Set the value of the (remote) subscriber working directory
     *
     * @param workingDirectorySubscriber
     */
    public void setWorkingDirectorySubscriber(String workingDirectorySubscriber)
    {
        this.workingDirectorySubscriber = workingDirectorySubscriber;
    }

    /**
     * Verify that a file exists and is readable
     *
     * @param file
     * @throws MungeException
     */
    private void verifyFileExistence(String file) throws MungeException
    {
        String filename;
        boolean isRelative = isRelativePath(file);
        if (isRelative)
        {
            String prefix = (context.cfg.getWorkingDirectory().length() > 0 ? context.cfg.getWorkingDirectory() + System.getProperty("file.separator") : "");
            filename = prefix + file;
        }
        else
            filename = file;

        File candidate = new File(filename);
        if (!candidate.exists() || !candidate.canRead())
            throw new MungeException(MessageFormat.format(context.cfg.gs("Cfg.file.not.found.or.not.readable"), filename));
    }

}
