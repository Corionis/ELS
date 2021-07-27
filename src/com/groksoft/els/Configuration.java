package com.groksoft.els;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.ArrayList;

/**
 * Configuration
 * <p>
 * Contains all command-line options and any other application-level configuration.
 */
public class Configuration
{
    public static final int NOT_REMOTE = 0;
    public static final int PUBLISHER_LISTENER = 4;
    public static final int PUBLISHER_MANUAL = 3;
    public static final int REMOTE_PUBLISH = 1;
    public static final int RENAME_BOTH = 3;
    public static final int RENAME_DIRECTORIES = 2;
    public static final int RENAME_FILES = 1;
    public static final int RENAME_NONE = 0;
    public static final int SUBSCRIBER_LISTENER = 2;
    public static final int SUBSCRIBER_TERMINAL = 5;
    private final String PROGRAM_VERSION = "3.0.0";
    private final String PROGRAM_NAME = "ELS : Entertainment Library Synchronizer";
    private String authorizedPassword = "";
    private String consoleLevel = "debug";  // Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF
    private boolean crossCheck = false;
    private String debugLevel = "debug";
    private boolean dryRun = false;
    private boolean duplicateCheck = false;
    private String exportCollectionFilename = "";
    private String exportTextFilename = "";
    private boolean forceCollection = false;
    private boolean forceTargets = false;
    private String hintKeysFile = "";
    private boolean hintSkipMainProcess = false;
    private String logFilename = "";
    private boolean logOverwrite = false;
    private String mismatchFilename = "";
    private boolean noBackFill = false;
    private String[] originalArgs;
    private boolean overwrite = false;
    private boolean publishOperation = true;
    private String publisherCollectionFilename = "";
    private String publisherLibrariesFileName = "";
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
    public Configuration()
    {
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
        logger.info(SHORT, "  cfg: -n Renaming = " + Boolean.toString(isRenaming()));
        logger.info(SHORT, "  cfg: -o Overwrite = " + Boolean.toString(isOverwrite()));
        if (getPublisherLibrariesFileName().length() > 0)
        {
            logger.info(SHORT, "  cfg: -p Publisher Library filename = " + getPublisherLibrariesFileName());
        }
        if (getPublisherCollectionFilename().length() > 0)
        {
            logger.info(SHORT, "  cfg: -P Publisher Collection filename = " + getPublisherCollectionFilename());
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
     * Sets Authorized password
     *
     * @param password the password required to access Authorized mode with a ClientStty
     */
    public void setAuthorizedPassword(String password)
    {
        this.authorizedPassword = password;
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
     * Sets console level
     *
     * @param consoleLevel the console level
     */
    public void setConsoleLevel(String consoleLevel)
    {
        this.consoleLevel = consoleLevel;
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
     * Sets debug level
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel)
    {
        this.debugLevel = debugLevel;
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
     * Sets export collection filename
     *
     * @param exportCollectionFilename the export collection filename
     */
    public void setExportCollectionFilename(String exportCollectionFilename)
    {
        this.exportCollectionFilename = exportCollectionFilename;
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
     * Sets the export text filename
     *
     * @param exportTextFilename the export text filename
     */
    public void setExportTextFilename(String exportTextFilename)
    {
        this.exportTextFilename = exportTextFilename;
    }

    public String getHintKeysFile()
    {
        return hintKeysFile;
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
     * Sets log filename
     *
     * @param logFilename the log filename
     */
    public void setLogFilename(String logFilename)
    {
        this.logFilename = logFilename;
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
     * Sets mismatch filename
     *
     * @param mismatchFilename the mismatch filename
     */
    public void setMismatchFilename(String mismatchFilename)
    {
        this.mismatchFilename = mismatchFilename;
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
    public String getProgramVersionN()
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
     * Sets publisher collection filename
     *
     * @param publisherCollectionFilename the publisher import filename
     */
    public void setPublisherCollectionFilename(String publisherCollectionFilename)
    {
        this.publisherCollectionFilename = publisherCollectionFilename;
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
     * Sets publisher libraries file name
     *
     * @param publisherLibrariesFileName the publisher configuration file name
     */
    public void setPublisherLibrariesFileName(String publisherLibrariesFileName)
    {
        this.publisherLibrariesFileName = publisherLibrariesFileName;
    }

    /**
     * Gets remote flag
     *
     * @return the remote flag, 0 = none, 1 = publisher, 2 = subscriber, 3 = pub terminal, 4 = pub listener, 5 = sub terminal
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
     * Sets remote type
     *
     * @param type the remote type and remote flag
     */
    public void setRemoteType(String type) throws MungerException
    {
        if (!this.remoteType.equals("-"))
        {
            throw new MungerException("The -r option may only be used once");
        }
        this.remoteType = type;
        this.remoteFlag = NOT_REMOTE;
        if (type.equalsIgnoreCase("P"))
            this.remoteFlag = REMOTE_PUBLISH;
        else if (type.equalsIgnoreCase("S"))
            this.remoteFlag = SUBSCRIBER_LISTENER;
        else if (type.equalsIgnoreCase("M"))
            this.remoteFlag = PUBLISHER_MANUAL;
        else if (type.equalsIgnoreCase("L"))
            this.remoteFlag = PUBLISHER_LISTENER;
        else if (type.equalsIgnoreCase("T"))
            this.remoteFlag = SUBSCRIBER_TERMINAL;
        else
            throw new MungerException("Error: -r must be followed by B|L|P|S|T, case-insensitive");
    }

    /**
     * Set the type of renaming to perform
     */
    public int getRenamingType()
    {
        return this.renamingType;
    }

    /**
     * Set the type of renaming to perform
     */
    public void setRenamingType(String type) throws MungerException
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
                throw new MungerException("unknown -n | --rename type of rename; requires F | D | B");
        }
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
     * Sets subscriber collection filename
     *
     * @param subscriberCollectionFilename the import filename
     */
    public void setSubscriberCollectionFilename(String subscriberCollectionFilename)
    {
        this.subscriberCollectionFilename = subscriberCollectionFilename;
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
     * Sets subscriber libraries file name
     *
     * @param subscriberLibrariesFileName the subscriber configuration file name
     */
    public void setSubscriberLibrariesFileName(String subscriberLibrariesFileName)
    {
        this.subscriberLibrariesFileName = subscriberLibrariesFileName;
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
     * Sets targets filename
     *
     * @param targetsFilename the targets filename
     */
    public void setTargetsFilename(String targetsFilename)
    {
        this.targetsFilename = targetsFilename;
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
     * Sets whats new filename
     *
     * @param whatsNewFilename the whats new filename
     */
    public void setWhatsNewFilename(String whatsNewFilename)
    {
        this.whatsNewFilename = whatsNewFilename;
    }

    public boolean isCrossCheck()
    {
        return crossCheck;
    }

    public void setCrossCheck(boolean crossCheck)
    {
        this.crossCheck = crossCheck;
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

    /**
     * Sets dry run
     *
     * @param dryRun true/false boolean
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    public boolean isDuplicateCheck()
    {
        return duplicateCheck;
    }

    public void setDuplicateCheck(boolean duplicateCheck)
    {
        this.duplicateCheck = duplicateCheck;
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
     * Set if this is a "forced collection" operation
     *
     * @param forceCollection true/false
     */
    public void setForceCollection(boolean forceCollection)
    {
        this.forceCollection = forceCollection;
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
     * Set if this is a "forced targets" operation
     *
     * @param forceTargets true/false
     */
    public void setForceTargets(boolean forceTargets)
    {
        this.forceTargets = forceTargets;
    }

    public boolean isHintSkipMainProcess()
    {
        return hintSkipMainProcess;
    }

    public void setHintSkipMainProcess(boolean hintSkipMainProcess)
    {
        this.hintSkipMainProcess = hintSkipMainProcess;
    }

    public boolean isLogOverwrite()
    {
        return logOverwrite;
    }

    public void setLogOverwrite(boolean logOverwrite)
    {
        this.logOverwrite = logOverwrite;
    }

    public boolean isNoBackFill()
    {
        return noBackFill;
    }

    public void setNoBackFill(boolean noBackFill)
    {
        this.noBackFill = noBackFill;
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

    /**
     * Sets overwrite mode
     */
    public void setOverwrite(boolean sense)
    {
        overwrite = sense;
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
     * Set if this is a publish operation
     *
     * @param publishOperation true/false
     */
    public void setPublishOperation(boolean publishOperation)
    {
        this.publishOperation = publishOperation;
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
     * Returns true if this is a publisher process, automatically execute the process
     *
     * @return true/false
     */
    public boolean isRemotePublish()
    {
        return (getRemoteFlag() == REMOTE_PUBLISH);
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
     * Enable or disable performing renaming
     *
     * @param renaming true to enable
     */
    public void setRenaming(boolean renaming)
    {
        this.renaming = renaming;
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
     * Set if this is a "request collection" operation
     *
     * @param requestCollection true/false
     */
    public void setRequestCollection(boolean requestCollection)
    {
        this.requestCollection = requestCollection;
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
     * Set if this is a "request targets" operation
     *
     * @param requestTargets true/false
     */
    public void setRequestTargets(boolean requestTargets)
    {
        this.requestTargets = requestTargets;
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
     * Sets specific publisher library exclude flag
     *
     * @param sense true/false
     */
    public void setSpecificExclude(boolean sense)
    {
        this.specificExclude = sense; // v3.0.0
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
     * Sets specific publisher library flag
     *
     * @param sense true/false
     */
    public void setSpecificLibrary(boolean sense)
    {
        this.specificLibrary = sense;
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
     * Set targets enabled
     */
    public void setTargetsEnabled(boolean sense)
    {
        targetsEnabled = sense;
    }

    public boolean isValidation()
    {
        return validation;
    }

    public void setValidation(boolean validation)
    {
        this.validation = validation;
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
     * Set What's New "all" option
     *
     * @param isWhatsNewAll true = all option
     */
    public void setWhatsNewAll(boolean isWhatsNewAll)
    {
        this.whatsNewAll = isWhatsNewAll;
    }

    /**
     * Parse command line
     * <p>
     * This populates the rest.
     *
     * @param args the args
     * @throws MungerException the els exception
     */
    public void parseCommandLine(String[] args) throws MungerException
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
                        throw new MungerException("Error: -a requires a password value");
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
                        throw new MungerException("Error: -c requires a level, trace, debug, info, warn, error, fatal, or off");
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
                        throw new MungerException("Error: -d requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
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
                        throw new MungerException("Error: -e requires an export path output filename");
                    }
                    break;
                case "-f":                                             // log filename
                case "-F":
                case "--log-file":
                case "--log-overwrite":
                    if (getLogFilename().length() > 0)
                        throw new MungerException("Error: -f and -F cannot be used at the same time");
                    if (args[index].equals("-F") || args[index].equals("--log-overwrite"))
                        setLogOverwrite(true);
                    if (index <= args.length - 2)
                    {
                        setLogFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -f requires a log filename");
                    }
                    break;
                case "-h":
                case "--version":
                    System.out.println("");
                    System.out.println(PROGRAM_NAME + ", Version " + PROGRAM_VERSION);
                    System.out.println("See the ELS wiki on GitHub for documentation at:");
                    System.out.println("  https://github.com/GrokSoft/ELS/wiki");
                    System.out.println("");
                    System.exit(1);
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
                        throw new MungerException("Error: -i requires a collection output filename");
                    }
                    break;
                case "-k":                                             // ELS keys file
                case "--keys":
                    if (index <= args.length - 2)
                    {
                        hintKeysFile = args[index + 1];
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -k requires an ELS keys filename");
                    }
                    break;
                case "-K":                                             // ELS keys file and skip main process munge
                case "--keys-only":
                    if (index <= args.length - 2)
                    {
                        hintKeysFile = args[index + 1];
                        ++index;
                        setHintSkipMainProcess(true);
                    }
                    else
                    {
                        throw new MungerException("Error: -K requires an ELS keys filename");
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
                        throw new MungerException("Error: -l requires a publisher library name");
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
                        throw new MungerException("Error: -L requires a publisher library name to exclude");
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
                        throw new MungerException("Error: -m requires a mismatches output filename");
                    }
                    break;
                case "-n":                                             // perform renaming
                case "--rename":
                    setRenaming(true);
                    if (index <= args.length - 2)
                    {
                        setRenamingType(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -n requires the type F | D | B");
                    }
                    break;
                case "-o":
                case "--overwrite":
                    setOverwrite(true);
                    break;
                case "-p":                                             // publisher JSON libraries file
                case "--publisher-libraries":
                    if (index <= args.length - 2)
                    {
                        setPublisherLibrariesFileName(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -p requires a publisher libraries filename");
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
                        throw new MungerException("Error: -P requires a publisher collection filename");
                    }
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
                        throw new MungerException("Error: -r must be followed by P|L|M|S|T, case-insensitive");
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
                        throw new MungerException("Error: -s requires a subscriber libraries filename");
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
                        throw new MungerException("Error: -S requires an subscriber collection filename");
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
                case "-w":                                             // What's New output filename
                case "--whatsnew":
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -w requires a What's New output filename");
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
                        throw new MungerException("Error: -W requires a What's New output filename");
                    }
                    break;
                case "-x":                                             // cross-library duplicate check
                case "--cross-check":
                    setCrossCheck(true);
                    break;
                default:
                    throw new MungerException("Error: unknown option " + args[index]);
            }
        }
    }

}
