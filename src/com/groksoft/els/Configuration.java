package com.groksoft.els;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.sshd.common.util.io.IoUtils;

import java.net.URL;
import java.util.*;

/**
 * Configuration class.
 * <p>
 * Contains all command-line options and any other application-level configuration.
 */
public class Configuration
{
    public static final int NOT_REMOTE = 0;
    public static final int PUBLISHER_LISTENER = 4;
    public static final int PUBLISHER_MANUAL = 3;
    public static final int PUBLISH_REMOTE = 1;
    public static final int RENAME_BOTH = 3;
    public static final int RENAME_DIRECTORIES = 2;
    public static final int RENAME_FILES = 1;
    public static final int RENAME_NONE = 0;
    public static final int STATUS_SERVER = 6;
    public static final int STATUS_SERVER_FORCE_QUIT = 7;
    public static final int SUBSCRIBER_LISTENER = 2;
    public static final int SUBSCRIBER_TERMINAL = 5;
    private final String NAVIGATOR_NAME = "ELS Navigator";
    private final String PROGRAM_VERSION = "4.0.0";
    private final String PROGRAM_NAME = "ELS : Entertainment Library Synchronizer";
    // add new locales here
    public String[] availableLocales = {"en_US"}; // List of built-in locales; TODO: Update locales here
    private String authorizedPassword = "";
    private Context context;
    private String consoleLevel = "debug";  // Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF
    private boolean crossCheck = false;
    private ResourceBundle currentBundle = null;
    private String currentFilePart = "";
    private String debugLevel = "debug";
    private boolean dryRun = false;
    private boolean dumpSystem = false;
    private boolean duplicateCheck = false;
    private String exportCollectionFilename = "";
    private String exportTextFilename = "";
    private boolean forceCollection = false;
    private boolean forceTargets = false;
    private String hintKeysFile = "";
    private boolean hintSkipMainProcess = false;
    private String hintsDaemonFilename = "";
    private String logFilename = "";
    private boolean logOverwrite = false;
    private double longScale = 1024L;
    private String mismatchFilename = "";
    private boolean navigator = false;
    private boolean noBackFill = false;
    private String[] originalArgs;
    private boolean overwrite = false;
    private boolean preserveDates = false;
    private boolean publishOperation = true;
    private String publisherCollectionFilename = "";
    private String publisherLibrariesFileName = "";
    private boolean quitStatusServer = false;
    private int remoteFlag = NOT_REMOTE;
    private String remoteType = "-";
    private boolean renaming = false;
    private int renamingType = RENAME_NONE;
    private boolean requestCollection = false;
    private boolean requestTargets = false;
    private ArrayList<String> selectedLibraryExcludes = new ArrayList<>();
    private ArrayList<String> selectedLibraryNames = new ArrayList<>();
    private boolean specificExclude = false;
    private boolean specificLibrary = false;
    private String statusTrackerFilename = "";
    private String subscriberCollectionFilename = "";
    private String subscriberLibrariesFileName = "";
    private boolean targetsEnabled = false;
    private String targetsFilename = "";
    private boolean validation = false;
    private boolean whatsNewAll = false;
    private String whatsNewFilename = "";


    /**
     * Instantiates a new Configuration
     */
    public Configuration(Context ctxt)
    {
        context = ctxt;
    }

    /**
     * Add an excluded publisher library name
     *
     * @param publisherLibraryName the publisher library name
     */
    public void addExcludedLibraryName(String publisherLibraryName)
    {
        this.selectedLibraryExcludes.add(publisherLibraryName); // v3.0.0
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
     * Dump the configuration
     */
    public void dump()
    {
        Logger logger = LogManager.getLogger("applog");
        Marker SHORT = MarkerManager.getMarker("SHORT");

        String msg = "Arguments: ";
        for (int index = 0; index < originalArgs.length; ++index)
        {
            msg = msg + originalArgs[index] + " ";
        }
        logger.info(SHORT, msg);

        if (getAuthorizedPassword().length() > 0)
        {
            logger.info(SHORT, "  cfg: -a Authorize mode password has been specified");
        }
        logger.info(SHORT, "  cfg: -b No back fill = " + Boolean.toString(isNoBackFill()));
        logger.info(SHORT, "  cfg: -c Console logging level = " + getConsoleLevel());
        logger.info(SHORT, "  cfg: -d Debug logging level = " + getDebugLevel());
        logger.info(SHORT, "  cfg: -D Dry run = " + Boolean.toString(isDryRun()));
        if (getExportTextFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -e Export text filename = " + getExportTextFilename());
        }
        logger.info(SHORT, "  cfg: -f Log filename = " + getLogFilename());
        if (statusTrackerFilename != null && statusTrackerFilename.length() > 0)
        {
            logger.info(SHORT, "  cfg: -h Hint status server: " + getStatusTrackerFilename());
        }
        if (hintsDaemonFilename != null && hintsDaemonFilename.length() > 0)
        {
            logger.info(SHORT, "  cfg: -H Hints status server daemon: " + getHintsDaemonFilename());
        }
        if (getExportCollectionFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -i Export collection JSON filename = " + getExportCollectionFilename());
        }
        if (hintKeysFile != null && hintKeysFile.length() > 0)
        {
            logger.info(SHORT, "  cfg: " + (isHintSkipMainProcess() ? "-K" : "-k") + " Hint keys file: " + hintKeysFile + ((isHintSkipMainProcess()) ? ", Skip main process" : ""));
        }
        if (!getSelectedLibraryNames().isEmpty())
        {
            logger.info(SHORT, "  cfg: -l Publisher library name(s):");
            for (String ln : getSelectedLibraryNames())
            {
                logger.info(SHORT, "          " + ln);
            }
        }
        if (!getExcludedLibraryNames().isEmpty())
        {
            logger.info(SHORT, "  cfg: -L Excluded library name(s):"); // v3.0.0
            for (String ln : getExcludedLibraryNames())
            {
                logger.info(SHORT, "          " + ln);
            }
        }
        if (getMismatchFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -m Mismatches output filename = " + getMismatchFilename());
        }
        logger.info(SHORT, "  cfg: -n Navigator session = " + Boolean.toString(isNavigator()));
        logger.info(SHORT, "  cfg: -o Overwrite = " + Boolean.toString(isOverwrite()));
        if (getPublisherLibrariesFileName().length() > 0)
        {
            logger.info(SHORT, "  cfg: -p Publisher Library filename = " + getPublisherLibrariesFileName());
        }
        if (getPublisherCollectionFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -P Publisher Collection filename = " + getPublisherCollectionFilename());
        }
        if (isQuitStatusServer())
        {
            logger.info(SHORT, "  cfg: -q Status server QUIT");
        }
        logger.info(SHORT, "  cfg: -r Remote session type = " + getRemoteType());
        if (getSubscriberLibrariesFileName().length() > 0)
        {
            logger.info(SHORT, "  cfg: -s Subscriber Library filename = " + getSubscriberLibrariesFileName());
        }
        if (getSubscriberCollectionFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -S Subscriber Collection filename = " + getSubscriberCollectionFilename());
        }
        if (isForceCollection() || getTargetsFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -" + ((isForceTargets()) ? "T" : "t") + " Targets filename = " + getTargetsFilename());
        }
        logger.info(SHORT, "  cfg: -u Duplicates = " + Boolean.toString(isDuplicateCheck()));
        logger.info(SHORT, "  cfg: -v Validate = " + Boolean.toString(isValidation()));
        if (getWhatsNewFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -" + (whatsNewAll ? "W" : "w") + " What's New output filename = " + getWhatsNewFilename() + (whatsNewAll ? ", show all items" : ""));
        }
        logger.info(SHORT, "  cfg: -x Cross-check = " + Boolean.toString(isCrossCheck()));
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
     * Gets console level
     *
     * @return the console level
     */
    public String getConsoleLevel()
    {
        return consoleLevel;
    }

    public String getCurrentFilePart()
    {
        return currentFilePart;
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
     * Gets excluded library names
     *
     * @return the excluded library names collection
     */
    public ArrayList<String> getExcludedLibraryNames()
    {
        return selectedLibraryExcludes; // v3.0.0
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
     * Gets Hint Keys filename
     *
     * @return String filename
     */
    public String getHintKeysFile()
    {
        return hintKeysFile;
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
     * Gets log filename
     *
     * @return the log filename
     */
    public String getLogFilename()
    {
        return logFilename;
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
     * Gets Main version
     *
     * @return the Main version
     */
    public String getProgramVersion()
    {
        return PROGRAM_VERSION;
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
        if (getPublisherCollectionFilename().length() > 0)
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
     * Gets remote flag
     *
     * @return the remote flag, 0 = none, 1 = publisher, 2 = subscriber, 3 = pub terminal, 4 = pub listener, 5 = sub terminal, 6 = status server, 7 = force quit status server
     */
    public int getRemoteFlag()
    {
        return this.remoteFlag;
    }

    /**
     * Gets remote type
     *
     * @return the remote type from the command line
     */
    public String getRemoteType()
    {
        return this.remoteType;
    }

    /**
     * Set the type of renaming to perform
     */
    public int getRenamingType()
    {
        return this.renamingType;
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
     * Get the Hint Status Tracker configuration filename
     *
     * @return String filename
     */
    public String getStatusTrackerFilename()
    {
        return statusTrackerFilename;
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
        if (getSubscriberCollectionFilename().length() > 0)
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
     * Gets whats new filename
     *
     * @return the whats new filename
     */
    public String getWhatsNewFilename()
    {
        return whatsNewFilename;
    }

    /**
     * Return locale bundle string
     *
     * @return String from built-in locale
     */
    public String gs(String key)
    {
        return currentBundle.getString(key);
    }

    /**
     * Is a duplicates cross-check enabled?
     *
     * @return true if enabled, else false
     */
    public boolean isCrossCheck()
    {
        return crossCheck;
    }

    /**
     * Is dry run boolean
     *
     * @return the boolean
     */
    public boolean isDryRun()
    {
        return dryRun;
    }

    public boolean isDumpSystem()
    {
        return dumpSystem;
    }

    /**
     * Are duplicates being checked?
     *
     * @return true if duplcates checking is enabled, else false
     */
    public boolean isDuplicateCheck()
    {
        return duplicateCheck;
    }

    /**
     * Is the current library one that has been excluded on the command line?
     *
     * @return isSelected true/false
     */
    public boolean isExcludedLibrary(String name)
    {
        for (String library : selectedLibraryExcludes) // v3.0.0
        {
            if (library.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Is this a "forced collection" operation?
     *
     * @return true/false
     */
    public boolean isForceCollection()
    {
        return forceCollection;
    }

    /**
     * Is this a "forced targets" operation
     *
     * @return true/false
     */
    public boolean isForceTargets()
    {
        return forceTargets;
    }

    /**
     * Are only Hints being processed so skip the main munge process?
     *
     * @return true if hints skipping main process enabled
     */
    public boolean isHintSkipMainProcess()
    {
        return hintSkipMainProcess;
    }

    /**
     * Is the log to be overwritten?
     *
     * @return true to overwrite
     */
    public boolean isLogOverwrite()
    {
        return logOverwrite;
    }

    public boolean isNavigator()
    {
        return navigator;
    }

    /**
     * Is the no back-fill option enabled so the default behavior of filling-in
     * original locations with new files is disabled?
     *
     * @return true if no back-fill is enabled
     */
    public boolean isNoBackFill()
    {
        return noBackFill;
    }

    /**
     * Gets overwrite mode
     *
     * @return true/false
     */
    public boolean isOverwrite()
    {
        return overwrite == true;
    }

    public boolean isPreserveDates()
    {
        return preserveDates;
    }

    /**
     * Is this a publish operation?
     *
     * @return true/false
     */
    public boolean isPublishOperation()
    {
        return publishOperation;
    }

    /**
     * Returns true if this publisher is in listener mode
     *
     * @return true/false
     */
    public boolean isPublisherListener()
    {
        return (getRemoteFlag() == PUBLISHER_LISTENER);
    }

    /**
     * Returns true if this publisher is in terminal mode
     *
     * @return true/false
     */
    public boolean isPublisherTerminal()
    {
        return (getRemoteFlag() == PUBLISHER_MANUAL);
    }

    /**
     * Should the current process command the Hint Status Server to quit?
     *
     * @return true if the command is to be sent
     */
    public boolean isQuitStatusServer()
    {
        return quitStatusServer;
    }

    /**
     * Returns true if this is a publisher process, automatically execute the process
     *
     * @return true/false
     */
    public boolean isRemotePublish()
    {
        return (getRemoteFlag() == PUBLISH_REMOTE);
    }

    /**
     * Returns true if this is any type of remote session
     *
     * @return true/false
     */
    public boolean isRemoteSession()
    {
        return (this.remoteFlag != NOT_REMOTE);
    }

    /**
     * Returns true if the -n | --rename options is specified
     */
    public boolean isRenaming()
    {
        return renaming;
    }

    /**
     * Is this a "request collection" operation?
     *
     * @return true/false
     */
    public boolean isRequestCollection()
    {
        return requestCollection;
    }

    /**
     * Is this a "request targets" operation?
     *
     * @return true/false
     */
    public boolean isRequestTargets()
    {
        return requestTargets;
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
        return this.specificExclude; // v3.0.0
    }

    /**
     * Is specific publisher library boolean.
     *
     * @return the boolean
     */
    public boolean isSpecificLibrary()
    {
        return specificLibrary;
    }

    /**
     * Returns true if this is a hint status server
     *
     * @return true/false
     */
    public boolean isStatusServer()
    {
        return (getRemoteFlag() == STATUS_SERVER);
    }

    /**
     * Returns true if subscriber is in listener mode
     */
    public boolean isSubscriberListener()
    {
        return (getRemoteFlag() == SUBSCRIBER_LISTENER);
    }

    /**
     * Returns true if this subscriber is in terminal mode
     */
    public boolean isSubscriberTerminal()
    {
        return (getRemoteFlag() == SUBSCRIBER_TERMINAL);
    }

    /**
     * Have targets been enabled?
     */
    public boolean isTargetsEnabled()
    {
        return targetsEnabled;
    }

    /**
     * Is a Hint Status Tracker being used?
     *
     * @return true if so
     */
    public boolean isUsingHintTracker()
    {
        return (statusTrackerFilename.length() > 0);
    }

    /**
     * Is the validation of collections and targets enabled?
     *
     * @return true if validation should be done
     */
    public boolean isValidation()
    {
        return validation;
    }

    /**
     * Is What's New an "all" option?
     *
     * @return true/false
     */
    public boolean isWhatsNewAll()
    {
        return this.whatsNewAll;
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
        if (!currentFilePart.equals(filePart))
        {
            // load the language file if available
            if (!Arrays.asList(availableLocales).contains(filePart))
            {
                filePart = "en_US"; // default locale
            }
            currentFilePart = filePart;
            currentBundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle_" + currentFilePart);
        }
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
        int index;
        originalArgs = args;

        for (index = 0; index < args.length; ++index)
        {
            switch (args[index])
            {
                case "-a":                                             // authorize mode password
                case "--authorize":
                    if (index <= args.length - 2)
                    {
                        setAuthorizedPassword(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -a requires a password value");
                    }
                    break;
                case "-b":                                             // disable back-filling
                case "--no-back-fill":
                    setNoBackFill(true);
                    break;
                case "-c":                                             // console level
                case "--console-level":
                    if (index <= args.length - 2)
                    {
                        setConsoleLevel(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -c requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "-D":                                             // Dry run
                case "--dry-run":
                    setDryRun(true);
                    break;
                case "-d":                                             // debug level
                case "--debug-level":
                    if (index <= args.length - 2)
                    {
                        setDebugLevel(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -d requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "--dump-system":
                    setDumpSystem(true);
                    break;
                case "-e":                                             // export publisher items to flat text file
                case "--export-text":
                    if (index <= args.length - 2)
                    {
                        setExportTextFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungeException("Error: -e requires an export path output filename");
                    }
                    break;
                case "-f":                                             // log filename
                case "-F":
                case "--log-file":
                case "--log-overwrite":
                    if (getLogFilename().length() > 0)
                        throw new MungeException("Error: -f and -F cannot be used at the same time");
                    if (args[index].equals("-F") || args[index].equals("--log-overwrite"))
                        setLogOverwrite(true);
                    if (index <= args.length - 2)
                    {
                        setLogFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -f requires a log filename");
                    }
                    break;
                case "-h":                                              // hint status tracker, v3.1.0
                case "--hints":
                    if (index <= args.length - 2)
                    {
                        setStatusTrackerFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungeException("Error: -h requires a hint status server repository filename");
                    }
                    break;
                case "-H":                                              // hint status server, v3.1.0
                case "--hint-server":
                    this.remoteFlag = STATUS_SERVER;
                    if (index <= args.length - 2)
                    {
                        setHintsDaemonFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungeException("Error: -H requires a hint status server repository filename");
                    }
                    break;
                case "-i":                                             // export publisher items to collection file
                case "--export-items":
                    if (index <= args.length - 2)
                    {
                        setExportCollectionFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungeException("Error: -i requires a collection output filename");
                    }
                    break;
                case "-j":                                             // Job
                    // TODO Implement execution of a defined Job in batch/background mode
                    break;
                case "-k":                                             // ELS keys file
                case "--keys":
                    if (index <= args.length - 2)
                    {
                        setHintKeysFile(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -k requires an ELS keys filename");
                    }
                    break;
                case "-K":                                             // ELS keys file and skip main process munge
                case "--keys-only":
                    if (index <= args.length - 2)
                    {
                        setHintKeysFile(args[index + 1]);
                        ++index;
                        setHintSkipMainProcess(true);
                    }
                    else
                    {
                        throw new MungeException("Error: -K requires an ELS keys filename");
                    }
                    break;
                case "-l":                                             // publisher library to process
                case "--library":
                    if (index <= args.length - 2)
                    {
                        addPublisherLibraryName(args[index + 1]);
                        setSpecificLibrary(true);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -l requires a publisher library name");
                    }
                    break;
                case "-L":                                             // publisher library to exclude
                case "--exclude":
                    if (index <= args.length - 2)
                    {
                        addExcludedLibraryName(args[index + 1]); // v3.0.0
                        setSpecificExclude(true);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -L requires a publisher library name to exclude");
                    }
                    break;
                case "-m":                                             // Mismatch output filename
                case "--mismatches":
                    if (index <= args.length - 2)
                    {
                        setMismatchFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -m requires a mismatches output filename");
                    }
                    break;
                case "-n":                                              // Navigator
                case "--navigator":
                    setNavigator(true);
                    break;
                case "-o":                                              // overwrite
                case "--overwrite":
                    setOverwrite(true);
                    break;
                case "-p":                                              // publisher JSON libraries file
                case "--publisher-libraries":
                    if (index <= args.length - 2)
                    {
                        setPublisherLibrariesFileName(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -p requires a publisher libraries filename");
                    }
                    break;
                case "-P":                                             // publisher JSON collection items file
                case "--publisher-collection":
                    if (index <= args.length - 2)
                    {
                        setPublisherCollectionFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -P requires a publisher collection filename");
                    }
                    break;
                case "-q":                                             // tell status server to quit
                case "--quit-status":
                    setQuitStatusServer(true);
                    break;
                case "-Q":
                case "--force-quit":
                    setQuitStatusServer(true);
                    this.remoteFlag = STATUS_SERVER_FORCE_QUIT;
                    break;
                case "-r":                                             // remote session
                case "--remote":
                    if (index <= args.length - 2)
                    {
                        setRemoteType(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -r must be followed by P|L|M|S|T, case-insensitive");
                    }
                    break;
                case "-s":                                             // subscriber JSON libraries file
                case "--subscriber-libraries":
                    if (index <= args.length - 2)
                    {
                        setForceCollection(false);
                        setRequestCollection(true);
                        setSubscriberLibrariesFileName(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -s requires a subscriber libraries filename");
                    }
                    break;
                case "-S":                                             // subscriber JSON collection items file
                case "--subscriber-collection":
                    if (index <= args.length - 2)
                    {
                        setForceCollection(true);
                        setRequestCollection(false);
                        setSubscriberCollectionFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -S requires an subscriber collection filename");
                    }
                    break;
                case "-t":                                             // targets filename
                case "--targets":
                    setTargetsEnabled(true);
                    setForceTargets(false);
                    setRequestTargets(true);
                    if (index <= args.length - 2 && !args[index + 1].startsWith("-"))
                    {
                        setTargetsFilename(args[index + 1]);
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
                        setTargetsFilename(args[index + 1]);
                        ++index;
                    }
                    break;
                case "-u":                                             // publisher duplicate check
                case "--duplicates":
                    setDuplicateCheck(true);
                    break;
                case "-v":                                             // validation run
                case "--validate":
                    setValidation(true);
                    break;
                case "--version":                                       // version
                    System.out.println("");
                    System.out.println(PROGRAM_NAME + ", Version " + PROGRAM_VERSION + ", " + context.main.getBuildStamp());
                    System.out.println("See the ELS wiki on GitHub for documentation at:");
                    System.out.println("  https://github.com/GrokSoft/ELS/wiki");
                    System.out.println("");
                    System.exit(1);
                    break;
                case "-w":                                             // What's New output filename
                case "--whatsnew":
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungeException("Error: -w requires a What's New output filename");
                    }
                    break;
                case "-W":                                             // What's New output filename, set "all" option
                case "--whatsnew-all":
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1]);
                        ++index;
                        setWhatsNewAll(true);
                    }
                    else
                    {
                        throw new MungeException("Error: -W requires a What's New output filename");
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
                case "-z":                                             // scale long values with 1000 instead of 1024
                case "--decimal-scale":
                    setLongScale(false);
                    break;
                default:
                    throw new MungeException("Error: unknown option: " + args[index]);
            }
        }

        // attempt to load the language Java started with, default en_US
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        String filePart = lang + "_" + country;
        loadLocale(filePart);
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
     * Sets console level
     *
     * @param consoleLevel the console level
     */
    public void setConsoleLevel(String consoleLevel)
    {
        this.consoleLevel = consoleLevel;
    }

    /**
     * Sets duplicates cross-check
     *
     * @param crossCheck
     */
    public void setCrossCheck(boolean crossCheck)
    {
        this.crossCheck = crossCheck;
    }

    /**
     * Sets debug level
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel)
    {
        this.debugLevel = debugLevel;
    }

    /**
     * Sets dry run
     *
     * @param dryRun true/false boolean
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    public void setDumpSystem(boolean dumpSystem)
    {
        this.dumpSystem = dumpSystem;
    }

    /**
     * Sets duplcates checking
     *
     * @param duplicateCheck
     */
    public void setDuplicateCheck(boolean duplicateCheck)
    {
        this.duplicateCheck = duplicateCheck;
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
     * Set if this is a "forced collection" operation
     *
     * @param forceCollection true/false
     */
    public void setForceCollection(boolean forceCollection)
    {
        this.forceCollection = forceCollection;
    }

    /**
     * Set if this is a "forced targets" operation
     *
     * @param forceTargets true/false
     */
    public void setForceTargets(boolean forceTargets)
    {
        this.forceTargets = forceTargets;
    }

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
        this.hintSkipMainProcess = hintSkipMainProcess;
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
     * Sets log filename
     *
     * @param logFilename the log filename
     */
    public void setLogFilename(String logFilename)
    {
        this.logFilename = logFilename;
    }

    /**
     * Sets if the log should be overwritten when the process starts
     *
     * @param logOverwrite
     */
    public void setLogOverwrite(boolean logOverwrite)
    {
        this.logOverwrite = logOverwrite;
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

    public void setNavigator(boolean navigator)
    {
        this.navigator = navigator;
    }

    /**
     * Sets the no back-fill option
     *
     * @param noBackFill
     */
    public void setNoBackFill(boolean noBackFill)
    {
        this.noBackFill = noBackFill;
    }

    /**
     * Sets overwrite mode
     */
    public void setOverwrite(boolean sense)
    {
        overwrite = sense;
    }

    public void setPreserveDates(boolean preserveDates)
    {
        this.preserveDates = preserveDates;
    }

    /**
     * Set if this is a publish operation
     *
     * @param publishOperation true/false
     */
    public void setPublishOperation(boolean publishOperation)
    {
        this.publishOperation = publishOperation;
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
        this.quitStatusServer = quitStatusServer;
    }

    /**
     * Sets remote type
     *
     * @param type the remote type and remote flag
     */
    public void setRemoteType(String type) throws MungeException
    {
        this.remoteType = type;
        this.remoteFlag = NOT_REMOTE;
        if (type.equalsIgnoreCase("P"))
            this.remoteFlag = PUBLISH_REMOTE;
        else if (type.equalsIgnoreCase("S"))
            this.remoteFlag = SUBSCRIBER_LISTENER;
        else if (type.equalsIgnoreCase("M"))
            this.remoteFlag = PUBLISHER_MANUAL;
        else if (type.equalsIgnoreCase("L"))
            this.remoteFlag = PUBLISHER_LISTENER;
        else if (type.equalsIgnoreCase("T"))
            this.remoteFlag = SUBSCRIBER_TERMINAL;
        else if (!type.equals("-"))
            throw new MungeException("Error: -r must be followed by B|L|P|S|T, case-insensitive");
    }

    /**
     * Enable or disable performing renaming
     *
     * @param renaming true to enable
     */
    public void setRenaming(boolean renaming)
    {
        this.renaming = renaming;
    }

    /**
     * Set the type of renaming to perform
     */
    public void setRenamingType(String type) throws MungeException
    {
        switch (type.toLowerCase())
        {
            case "d":
                this.renamingType = RENAME_DIRECTORIES;
                break;
            case "f":
                this.renamingType = RENAME_FILES;
                break;
            case "b":
                this.renamingType = RENAME_BOTH;
                break;
            default:
                throw new MungeException("unknown -n | --rename type of rename; requires F | D | B");
        }
    }

    /**
     * Set if this is a "request collection" operation
     *
     * @param requestCollection true/false
     */
    public void setRequestCollection(boolean requestCollection)
    {
        this.requestCollection = requestCollection;
    }

    /**
     * Set if this is a "request targets" operation
     *
     * @param requestTargets true/false
     */
    public void setRequestTargets(boolean requestTargets)
    {
        this.requestTargets = requestTargets;
    }

    /**
     * Sets specific publisher library exclude flag
     *
     * @param sense true/false
     */
    public void setSpecificExclude(boolean sense)
    {
        this.specificExclude = sense; // v3.0.0
    }

    /**
     * Sets specific publisher library flag
     *
     * @param sense true/false
     */
    public void setSpecificLibrary(boolean sense)
    {
        this.specificLibrary = sense;
    }

    /**
     * Set the Hint Status Tracker configuration filename
     *
     * @param statusTrackerFilename
     */
    public void setStatusTrackerFilename(String statusTrackerFilename)
    {
        this.statusTrackerFilename = statusTrackerFilename;
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
        targetsEnabled = sense;
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
     * Sets the collection and targets validation option
     *
     * @param validation
     */
    public void setValidation(boolean validation)
    {
        this.validation = validation;
    }

    /**
     * Set What's New "all" option
     *
     * @param isWhatsNewAll true = all option
     */
    public void setWhatsNewAll(boolean isWhatsNewAll)
    {
        this.whatsNewAll = isWhatsNewAll;
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

}
